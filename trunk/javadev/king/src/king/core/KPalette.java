// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
//}}}
/**
* <code>KPalette</code> organizes the canonical Mage colors.
* This class is an incompatible, 3rd-generation replacement
* for ColorManager.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 25 15:37:14 EDT 2003
*/
public class KPalette //extends ... implements ...
{
//{{{ Pens
    /** Scaling factors for width at different depth-cue levels */
    public static final float[] widthScale = new float[KPaint.COLOR_LEVELS];
    
    /** A zero-width pen for objects that should be filled instead. */
    public static final BasicStroke     pen0 = new BasicStroke(0, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    /** A one pixel thick pen for ordinary drawing. */
    public static final BasicStroke     pen1 = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    /** The set of various pens for drawing depth-cued lines in different widths. */
    public static final BasicStroke[][] pens = new BasicStroke[7][KPaint.COLOR_LEVELS];
    
    /** The set of line widths used for basic drawing, adjusted by widthScale. */
    public static final int[][] lineWidths = new int[7][KPaint.COLOR_LEVELS];
    
    /**
    * This version of line widths emulates MAGE.
    * Line width multipliers scale linearly from 0.5 to 2.0.
    * It's not realistic with respect to the laws of optical perspective,
    * but it is more pronounced than my old version.
    */
    static
    {
        for(int i = 0; i < KPaint.COLOR_LEVELS; i++)
        {
            double a = i / (KPaint.COLOR_LEVELS-1.0);
            widthScale[i] = (float)(a*2.0 + (1-a)*0.5);
        }
        
        // No depth cueing by width for really thin lines
        for(int j = 0; j < KPaint.COLOR_LEVELS; j++)
        {
            lineWidths[0][j]    = 1;
            pens[0][j]          = pen1;
        }
        
        // All other line thicknesses get depth cueing
        for(int i = 1; i < 7; i++)
        {
            for(int j = 0; j < KPaint.COLOR_LEVELS; j++)
            {
                lineWidths[i][j]    = Math.max(1, (int)((i+1)*widthScale[j] + 0.5));
                pens[i][j]          = new BasicStroke((i+1)*widthScale[j], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            }
        }
    }

    /** Jane feels this doesn't give a strong enough effect * /
    
    // I don't remember what this code is calculating anymore.
    // I think it has to do with perspective, so that lines
    // shrink in the background as though they were cylinders
    // according to the normal rules of perspective.
    static
    {
        double half = (KPaint.COLOR_LEVELS-1.0) / 2.0;
        double quot = 3.0 * half;
        for(int i = 0; i < KPaint.COLOR_LEVELS; i++)
        {
            widthScale[i] = (float)(1.0 / (1.0 - (i-half)/quot));
        }
        
        for(int i = 0; i < 7; i++)
        {
            for(int j = 0; j < KPaint.COLOR_LEVELS; j++)
            {
                pens[i][j] = new BasicStroke((i+1)*widthScale[j], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            }
        }
    }
    /** Jane feels this doesn't give a strong enough effect */
//}}}

//{{{ Colors
//##################################################################################################
    // Create standard entries
    //                                                          name        hue     bSat    wSat    bVal    wVal
    //public static final KPaint defaultColor = KPaint.createHSV("default",   0,      0,      0,      100,    0);
    public static final KPaint red          = KPaint.createHSV("red",       0,      100,    100,    100,    80);
    public static final KPaint orange       = KPaint.createHSV("orange",    20,     100,    100,    100,    90);
    //public static final KPaint rust         = KPaint.createHSV("rust",      20,     100,    100,    100,    90);
    public static final KPaint gold         = KPaint.createHSV("gold",      40,     100,    100,    100,    90);
    public static final KPaint yellow       = KPaint.createHSV("yellow",    60,     100,    100,    100,    90);
    public static final KPaint lime         = KPaint.createHSV("lime",      80,     100,    100,    100,    85);
    public static final KPaint green        = KPaint.createHSV("green",     120,    80,     90,     100,    75);
    public static final KPaint sea          = KPaint.createHSV("sea",       150,    100,    100,    100,    85);
    //public static final KPaint seagreen     = KPaint.createHSV("seagreen",  150,    100,    100,    100,    85);
    public static final KPaint cyan         = KPaint.createHSV("cyan",      180,    100,    85,     85,     80);
    public static final KPaint sky          = KPaint.createHSV("sky",       210,    75,     80,     95,     90);
    //public static final KPaint skyblue      = KPaint.createHSV("skyblue",   210,    75,     80,     95,     90);
    public static final KPaint blue         = KPaint.createHSV("blue",      240,    70,     80,     100,    100);
    public static final KPaint purple       = KPaint.createHSV("purple",    275,    75,     100,    100,    85);
    public static final KPaint magenta      = KPaint.createHSV("magenta",   300,    95,     100,    100,    90);
    public static final KPaint hotpink      = KPaint.createHSV("hotpink",   335,    100,    100,    100,    90);
    public static final KPaint pink         = KPaint.createHSV("pink",      350,    55,     75,     100,    90);
    public static final KPaint peach        = KPaint.createHSV("peach",     25,     75,     75,     100,    90);
    public static final KPaint lilac        = KPaint.createHSV("lilac",     275,    55,     75,     100,    80);
    public static final KPaint pinktint     = KPaint.createHSV("pinktint",  340,    30,     100,    100,    55);
    public static final KPaint peachtint    = KPaint.createHSV("peachtint", 25,     50,     100,    100,    60);
    public static final KPaint yellowtint   = KPaint.createHSV("yellowtint",60,     50,     100,    100,    75);
    //public static final KPaint paleyellow   = KPaint.createHSV("paleyellow",60,     50,     100,    100,    75);
    public static final KPaint greentint    = KPaint.createHSV("greentint", 135,    40,     100,    100,    35);
    public static final KPaint bluetint     = KPaint.createHSV("bluetint",  220,    40,     100,    100,    50);
    public static final KPaint lilactint    = KPaint.createHSV("lilactint", 275,    35,     100,    100,    45);
    public static final KPaint white        = KPaint.createHSV("white",     0,      0,      0,      100,    0);
    public static final KPaint gray         = KPaint.createHSV("gray",      0,      0,      0,      50,     40);
    //public static final KPaint grey         = KPaint.createHSV("grey",      0,      0,      0,      50,     40);
    public static final KPaint brown        = KPaint.createHSV("brown",     20,     45,     45,     75,     55);
    public static final KPaint deadwhite    = KPaint.createSolid("deadwhite", Color.white);
    public static final KPaint deadblack    = KPaint.createSolid("deadblack", Color.black);
    //public static final KPaint black        = KPaint.createSolid("black", Color.black);
    public static final KPaint invisible    = KPaint.createInvisible("invisible");
    
    public static final KPaint defaultColor = white;
//}}}

//{{{ Color map
//##################################################################################################
    private static final Map stdColorMap;   // unmodifiable once created
    private static final Map fullColorMap;  // unmodifiable once created
    
    static
    {
        Map map = new UberMap();
        map.put(red.toString(), red);
        map.put(pink.toString(), pink);
        map.put(pinktint.toString(), pinktint);
        map.put(orange.toString(), orange);
        map.put(peach.toString(), peach);
        map.put(peachtint.toString(), peachtint);
        map.put(gold.toString(), gold);
        map.put(yellow.toString(), yellow);
        map.put(yellowtint.toString(), yellowtint);
        map.put(lime.toString(), lime);
        map.put(green.toString(), green);
        map.put(greentint.toString(), greentint);
        map.put(sea.toString(), sea);
        map.put(cyan.toString(), cyan);
        map.put(sky.toString(), sky);
        map.put(blue.toString(), blue);
        map.put(bluetint.toString(), bluetint);
        map.put(purple.toString(), purple);
        map.put(lilac.toString(), lilac);
        map.put(lilactint.toString(), lilactint);
        map.put(magenta.toString(), magenta);
        map.put(hotpink.toString(), hotpink);
        map.put(white.toString(), white);
        map.put(gray.toString(), gray);
        map.put(brown.toString(), brown);
        map.put(deadwhite.toString(), deadwhite);
        map.put(deadblack.toString(), deadblack);
        map.put(invisible.toString(), invisible);
        stdColorMap = Collections.unmodifiableMap(map);
        
        map = new UberMap(map);
        map.put("default", defaultColor);
        map.put("rust", orange);
        map.put("seagreen", sea);
        map.put("skyblue", sky);
        map.put("paleyellow", yellowtint);
        map.put("grey", gray);
        map.put("black", deadblack);
        fullColorMap = Collections.unmodifiableMap(map);
    }
//}}}

//{{{ Aspect table
//##################################################################################################
    private static final KPaint[] aspectTable;
    
    static
    {
        aspectTable = new KPaint[256];
        Arrays.fill(aspectTable, null);
        
        addAspect('A', red);
        addAspect('B', orange);
        addAspect('C', gold);
        addAspect('D', yellow);
        addAspect('E', lime);
        addAspect('F', green);
        addAspect('G', sea);
        addAspect('H', cyan);
        addAspect('I', sky);
        addAspect('J', blue);
        addAspect('K', purple);
        addAspect('L', magenta);
        addAspect('M', hotpink);
        addAspect('N', pink);
        addAspect('O', lilac);
        addAspect('P', peach);
        addAspect('Q', peachtint);
        addAspect('R', yellowtint);
        addAspect('S', greentint);
        addAspect('T', bluetint);
        addAspect('U', lilactint);
        addAspect('V', pinktint);
        addAspect('W', white);
        addAspect('X', gray);
        addAspect('Y', brown);
        addAspect('Z', invisible);
    }
    
    static private void addAspect(char key, KPaint color)
    {
        int upper = Character.toUpperCase(key);
        int lower = Character.toLowerCase(key);
        aspectTable[upper] = aspectTable[lower] = color;
    }
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Not instantiable
    */
    private KPalette()
    {
    }
//}}}

//{{{ forName, forAspect, getStandard/FullMap
//##################################################################################################
    /** Returns the named KPaint or null if none is known. */
    static public KPaint forName(String name)
    { return (KPaint)fullColorMap.get(name); }
    
    /** Returns the KPaint for the given aspect, or null if bad aspect */
    static public KPaint forAspect(char aspect)
    {
        if(aspect < 0 || aspect > 255) return null;
        else return aspectTable[aspect];
    }
    
    /**
    * Returns a Map&lt;String, KPaint&gt; of the predefined Mage colors.
    * This map includes one entry for each unique color, using its preferred name.
    */
    static public Map getStandardMap()
    { return stdColorMap; }
    
    /**
    * Returns a Map&lt;String, KPaint&gt; of the predefined Mage colors.
    * This map includes extra entries for alternate spellings and
    * older, deprecated color names.
    */
    static public Map getFullMap()
    { return fullColorMap; }
//}}}

//{{{ setContrast
//##################################################################################################
    /**
    * Adjusts the contrast for the entire palette.
    * A contrast of less than 1.0 is flat, and greater than 1.0 is exagerated.
    */
    static public void setContrast(double alpha)
    {
        for(Iterator iter = getFullMap().values().iterator(); iter.hasNext(); )
        {
            KPaint p = (KPaint) iter.next();
            p.setContrast(alpha);
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

