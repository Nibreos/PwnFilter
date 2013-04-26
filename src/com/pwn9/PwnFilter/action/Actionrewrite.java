package com.pwn9.PwnFilter.action;

import com.pwn9.PwnFilter.FilterState;
import com.pwn9.PwnFilter.PwnFilter;
import com.pwn9.PwnFilter.util.ColoredString;

/**
 * Rewrite the string by replacing the matched text with the provided string.
 */
public class Actionrewrite implements Action {
    // messageString is what we will use to replace any matched text.
    String messageString;

    public void init(String s)
    {
        messageString = s.replaceAll("&([0-9a-fk-or])", "\u00A7$1");
    }

    public boolean execute(final PwnFilter plugin, final FilterState state) {
        ColoredString cs = state.message;
        cs.replaceText(state.pattern, messageString);
        return true;
    }
}
