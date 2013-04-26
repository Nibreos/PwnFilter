package com.pwn9.PwnFilter.action;

import com.pwn9.PwnFilter.FilterState;
import com.pwn9.PwnFilter.PwnFilter;
import com.pwn9.PwnFilter.util.ColoredString;

import java.util.Random;

/**
 * Replace the matched text with a random selection from a | seperated list of text.
 */
public class Actionrandrep implements Action {
    // toRand is a String array of options to chose from for replacement.
    String[] toRand;

    public void init(String s)
    {
        toRand = s.split("\\|");

    }

    public boolean execute(final PwnFilter plugin, final FilterState state ) {
        Random random = new Random();
        int randomInt = random.nextInt(toRand.length);
        ColoredString cs = state.message;
        cs.replaceText(state.pattern,toRand[randomInt]);
        return true;
    }
}
