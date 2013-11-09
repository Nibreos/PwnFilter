/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Handles keeping a cache of data that we need during Async event handling.
 * We can't get this data in our Async method, as Bukkit API calls are not threadsafe.
 *
 * Making this a singleton, since it will be needed for lookups across multiple threads
 * and we don't want to have to pass around a reference to it.
 *
 * The DataCache runs in the main bukkit thread every second, pulling the required
 * information.
 */

public class DataCache {

    public final static int runEveryTicks = 20;

    private static DataCache _instance = null;

    // Permissions we are interested in caching
    protected Set<String> permSet = new TreeSet<String>();

    //private
    private final Plugin plugin;
    private int taskId;
    private ConcurrentHashMap<Player,String> playerName;
    private ConcurrentHashMap<Player,String> playerWorld;
    private ConcurrentHashMap<Player,HashSet<String>> playerPermissions;
    private ArrayList<Player> queuedPlayerList = new ArrayList<Player>();
    private Player[] onlinePlayers = {};

    //TODO: Add a "registration" system for interesting permissions, etc.
    // so that plugins can add/remove things they want cached.
    private DataCache(Plugin plugin) {
        playerName = new ConcurrentHashMap<Player,String>();
        playerWorld = new ConcurrentHashMap<Player,String>();
        playerPermissions = new ConcurrentHashMap<Player,HashSet<String>>();
        this.plugin = plugin;
    }

    // This method is for the owning plugin (PwnFilter) to initialize the cache.
    public static DataCache getInstance(Plugin p) {
        if ( _instance == null ) {
            _instance = new DataCache(p);
            return _instance;
        } else return _instance;
    }

    // This method is for other classes to call to query the cache.
    public static DataCache getInstance() throws IllegalStateException {
        if ( _instance == null ) {
            throw new IllegalStateException("DataCache accessed before initialized!");
        } else return _instance;
    }

    public Player[] getOnlinePlayers() {
        return onlinePlayers;
    }

    private void cachePlayerPermissions(Player p) {
        HashSet<String> playerPerms = new HashSet<String>();

        for (String perm : permSet) {
            if (p.hasPermission(perm)) {
                playerPerms.add(perm);
            }
        }

        playerPermissions.put(p,playerPerms);
    }

    public synchronized void addPermission(String permission) {
        permSet.add(permission);
    }

    public synchronized void addPermissions(List<Permission> permissions) {
        for (Permission p : permissions ) {
            permSet.add(p.getName());
        }
    }

    public synchronized void addPermissions(Set<String> permissions) {
        permSet.addAll(permissions);
    }


    public boolean hasPermission(Player p, String s) {
        HashSet<String> perms = playerPermissions.get(p);
        return perms != null && perms.contains(s);
    }

    public boolean hasPermission(Player p, Permission perm) {
        HashSet<String> perms = playerPermissions.get(p);
        return perms != null && perms.contains(perm.getName());
    }

    public String getPlayerWorld(Player p) {
        return playerWorld.get(p);
    }

    public String getPlayerName(Player p) {
        return playerName.get(p);
    }

    private synchronized void updateCache() {
        /*
        I think I want to process an average of 1 player per tick.
          Every time this method is called, it will check to see if there are players on the
          onlinePlayerList[].  If so, it will process them.  If not, it will
          grab the list of online players, and add it to the list.
         */
        if (queuedPlayerList.size() < 1) {
            onlinePlayers = Bukkit.getOnlinePlayers();

            if (onlinePlayers.length > 0) {
                queuedPlayerList.addAll(Arrays.asList(onlinePlayers));
            }
            // Clear out stale data
            for (Player p : playerName.keySet() ) {
                if (!Arrays.asList(onlinePlayers).contains(p)) {
                    playerName.remove(p);
                    playerWorld.remove(p);
                    playerPermissions.remove(p);
                }
            }
        }
        // Update the cache
        for (int i= 0 ; i < runEveryTicks ; i++) {
            if (queuedPlayerList.size() < 1) break;
            Player player = queuedPlayerList.remove(0);
            playerName.put(player,player.getName());
            playerWorld.put(player,player.getWorld().getName());
            cachePlayerPermissions(player);

        }
    }


    public void start() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                updateCache();
            }
        },0,DataCache.runEveryTicks);
    }

    public void stop() {
        Bukkit.getScheduler().cancelTask(taskId);
        taskId = 0;
    }

    public void finalize() throws Throwable {
        if ( taskId != 0 ) {
            stop();
        }
        super.finalize();
    }

    public void dumpCache(Logger l) {
        l.finest("PwnFilter Data Cache Contents:");
        l.finest("Task Id: " + taskId);
        l.finest("Online Players: " + Bukkit.getOnlinePlayers().length);
        l.finest("Total Names: " + playerName.size() + " Worlds: " + playerWorld.size() + " Perms: " + playerPermissions.size());
        StringBuilder sb = new StringBuilder();
        for (Player p : queuedPlayerList ){
            sb.append(p.toString());
            sb.append(" ");
        }
        l.finest(sb.toString());
        l.finest("-----PlayerCache ------");
        for (Player p : playerName.keySet()) {
            l.finest("Player ID: " + p + " Name: " + playerName.get(p) + " World: " + playerWorld.get(p));
            StringBuilder s = new StringBuilder();
            sb.append("PermissionsSet : ");
            HashSet<String> perms = playerPermissions.get(p);
            for (String perm : perms) {
                s.append(perm);
                s.append(" ");
            }
            l.finest(s.toString());
        }
    }

}
