package boundrotamers;
//import java.awt.*;
//import java.io.*;
//import java.util.*;
//import javax.swing.*;

/**
 * <code>MageColors</code> contains all of the standard Mage colors as string constants.
 *
<br><pre>
/----------------------------------------------------------------------\
| This program is free software; you can redistribute it and/or modify |
| it under the terms of the GNU General Public License as published by |
| the Free Software Foundation; either version 2 of the License, or    |
| (at your option) any later version.                                  |
|                                                                      |
| This program is distributed in the hope that it will be useful,      |
| but WITHOUT ANY WARRANTY; without even the implied warranty of       |
| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        |
| GNU General Public License for more details.                         |
\----------------------------------------------------------------------/
</pre>
 *
 * <p>Begun on Thu Apr  4 10:07:30 EST 2002
 * <br>Copyright (C) 2002 by Ian W. Davis
 * <br>Richardson laboratory, Duke University: <a href="http://kinemage.biochem.duke.edu">kinemage.biochem.duke.edu</a>
 *
 */
public class MageColors //extends ... implements ...
{
//##################################################################################################

    // Left out peach (P) because I can't find a good category to put it in!!

    public static final String[] str = {"red", /*"orange",*/ "gold", "yellow", "lime", "green", "sea", "cyan",
        "sky", "blue", "purple", "lilac", "magenta", "hotpink", "pink", "pinktint", "peachtint", "yellowtint",
        "greentint", "bluetint", "lilactint", "deadwhite", "white", "gray", "deadblack", "brown", "invisible"};

    public static final char[]   chr = {'A', /*'B',*/ 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'O', 'L', 'M', 'N', 'V', 'Q', 'R',
         'S', 'T', 'U', ' ', 'W', 'X', ' ', 'Y', 'Z'};

    public static final int BOLD    = 0,
                            SOFT    = 15,
                            NEUTRAL = 21;
//##################################################################################################
    /**
    * Returns a bold color.
    */
    public static String bold(int which) { return str[BOLD+(which % (SOFT-BOLD))]; }

    /**
    * Returns a soft color.
    */
    public static String soft(int which) { return str[SOFT+(which % (NEUTRAL-SOFT))]; }

    /**
    * Returns a neutral color.
    */
    public static String neutral(int which) { return str[NEUTRAL+(which % (str.length-NEUTRAL))]; }
}//class
