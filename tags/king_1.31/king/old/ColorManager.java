// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
 * <code>ColorManager</code> encodes flexible color palettes for kinemage rendering.
 * This class replaces MagePaints, but not in a compatible way.
 * It allows for kinemages defining new colors, and re-defining standard colors.
 *
 * <p>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Sep 18 08:54:44 EDT 2002
*/
public class ColorManager //extends ... implements ...
{
//{{{ Constants & pens
    public static final int COLOR_LEVELS = 5;
    public static final int SHADE_LEVELS = 16;
    
    static final double     AMBIENT_COEFF   = 0.4;
    static final double     DIFFUSE_COEFF   = 0.6;
    
    public static final BasicStroke     pen0 = new BasicStroke(0, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static final BasicStroke     pen1 = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static final BasicStroke[][] pens = new BasicStroke[7][5];
    static
    {
        for(int i = 0; i < 7; i++)
        {
            for(int j = 0; j < 5; j++)
            {
                pens[i][j] = new BasicStroke((i+1)*KPoint.widthScale[j], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            }
        }
    }
//}}}

//{{{ Standard color names
    static final java.util.List STD_COLOR_NAMES = new ArrayList();
    static {
        STD_COLOR_NAMES.add("default");
        STD_COLOR_NAMES.add("red");
        STD_COLOR_NAMES.add("pink");
        STD_COLOR_NAMES.add("pinktint");
        STD_COLOR_NAMES.add("orange");
        STD_COLOR_NAMES.add("peach");
        STD_COLOR_NAMES.add("peachtint");
        STD_COLOR_NAMES.add("gold");
        STD_COLOR_NAMES.add("yellow");
        STD_COLOR_NAMES.add("yellowtint");
        STD_COLOR_NAMES.add("lime");
        STD_COLOR_NAMES.add("green");
        STD_COLOR_NAMES.add("greentint");
        STD_COLOR_NAMES.add("sea");
        STD_COLOR_NAMES.add("cyan");
        STD_COLOR_NAMES.add("sky");
        STD_COLOR_NAMES.add("blue");
        STD_COLOR_NAMES.add("bluetint");
        STD_COLOR_NAMES.add("purple");
        STD_COLOR_NAMES.add("lilac");
        STD_COLOR_NAMES.add("lilactint");
        STD_COLOR_NAMES.add("magenta");
        STD_COLOR_NAMES.add("hotpink");
        STD_COLOR_NAMES.add("white");
        STD_COLOR_NAMES.add("gray");
        STD_COLOR_NAMES.add("brown");
        STD_COLOR_NAMES.add("deadwhite");
        STD_COLOR_NAMES.add("deadblack");
        STD_COLOR_NAMES.add("invisible");
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    Palette         black;
    Palette         blackMono;
    Palette         white;
    Palette         whiteMono;
    HashMap         nametable;
    HashMap         indextable;
    int[]           aspecttable;
    java.util.List  newColorNames;
    ArrayList       aliasedColors;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ColorManager()
    {
        nametable       = new HashMap(51);
        indextable      = new HashMap(51);
        newColorNames   = new LinkedList();
        aliasedColors   = new ArrayList();
        makeKingPalettes();
    }
//}}}

//{{{ getIndex/Name, hasColor, getIndexForAspect
//##################################################################################################
    /** Retrieves the index of the named color, or -1 if that color is unknown. */
    public int getIndex(String name)
    {
        Integer colorkey = (Integer)nametable.get(name);
        if(colorkey == null) return -1;
        else return colorkey.intValue();
    }
    
    /** Returns the name associated with this index, or null if none is registered. */
    public String getName(int colorkey)
    { return (String)indextable.get(new Integer(colorkey)); }
    
    /** Returns true if the named color is known to this palette */
    public boolean hasColor(String name)
    { return nametable.containsKey(name); }
    
    /** Translates an aspect into a color index */
    public int getIndexForAspect(char aspect)
    {
        int a = aspect;
        if(a >= aspecttable.length) return 0;
        else return aspecttable[a];
    }
//}}}

//{{{ getWhite/Black(Mono)Palette
//##################################################################################################
    /** Gets the palette of colors used for painting on the white background */
    public Palette getWhitePalette()
    { return white; }
    /** Gets the palette of colors used for painting on the black background */
    public Palette getBlackPalette()
    { return black; }
    /** Gets the palette of colors used for painting on the white background, in grayscale */
    public Palette getWhiteMonoPalette()
    { return whiteMono; }
    /** Gets the palette of colors used for painting on the black background, in grayscale */
    public Palette getBlackMonoPalette()
    { return blackMono; }
//}}}

//{{{ makeKingPalettes
//##################################################################################################
    /** Creates a default palette with a black background, and one with a white background. */
    void makeKingPalettes()
    {
        /* Code to create the color-cone kinemage * /
        DecimalFormat df = new DecimalFormat("0.0###");
        System.out.println("@vectorlist {100% saturation} color= gray");
        for(int i = 0; i <=  360; i+=5) 
        { System.out.println("{\"} "+df.format(100*Math.sin(Math.toRadians(i)))+" "+df.format(100*Math.cos(Math.toRadians(i)))+" 100.0"); }
        System.out.println("@vectorlist {100% saturation} color= gray");
        for(int i = 0; i <=  360; i+=5) 
        { System.out.println("{\"} "+df.format(75*Math.sin(Math.toRadians(i)))+" "+df.format(75*Math.cos(Math.toRadians(i)))+" 75.0"); }
        System.out.println("@vectorlist {100% saturation} color= gray");
        for(int i = 0; i <=  360; i+=5) 
        { System.out.println("{\"} "+df.format(50*Math.sin(Math.toRadians(i)))+" "+df.format(50*Math.cos(Math.toRadians(i)))+" 50.0"); }
        System.out.println("@vectorlist {100% saturation} color= gray");
        for(int i = 0; i <=  360; i+=5) 
        { System.out.println("{\"} "+df.format(25*Math.sin(Math.toRadians(i)))+" "+df.format(25*Math.cos(Math.toRadians(i)))+" 25.0"); }
        System.out.println("@vectorlist {75% saturation} color= gray");
        for(int i = 0; i <=  360; i+=5) 
        { System.out.println("{\"} "+df.format(75*Math.sin(Math.toRadians(i)))+" "+df.format(75*Math.cos(Math.toRadians(i)))+" 100.0"); }
        System.out.println("@vectorlist {50% saturation} color= gray");
        for(int i = 0; i <=  360; i+=5) 
        { System.out.println("{\"} "+df.format(50*Math.sin(Math.toRadians(i)))+" "+df.format(50*Math.cos(Math.toRadians(i)))+" 100.0"); }
        System.out.println("@vectorlist {25% saturation} color= gray");
        for(int i = 0; i <=  360; i+=5) 
        { System.out.println("{\"} "+df.format(25*Math.sin(Math.toRadians(i)))+" "+df.format(25*Math.cos(Math.toRadians(i)))+" 100.0"); }
        /* Code to create the color-cone kinemage */
        
        black           = new Palette(Color.black);
        blackMono       = new Palette(Color.black);
        white           = new Palette(Color.white);
        whiteMono       = new Palette(Color.white);
        
        // Create the entry for "invisible"
        Integer colorkey = new Integer(-1);
        nametable.put( "invisible",    colorkey);
        indextable.put( colorkey,     "invisible");
        // Notice there are no real color palette entries for invisible...
        
        // Create standard King entries
        //              name        hue     bSat    wSat    bVal    wVal
        createHSVcolor("default",   0,      0,      0,      100,    0);
        createHSVcolor("red",       0,      100,    100,    100,    80);
        createHSVcolor("orange",    20,     100,    100,    100,    90);
        createHSVcolor("rust",      20,     100,    100,    100,    90);
        createHSVcolor("gold",      40,     100,    100,    100,    90);
        createHSVcolor("yellow",    60,     100,    100,    100,    90);
        createHSVcolor("lime",      80,     100,    100,    100,    85);
        createHSVcolor("green",     120,    80,     90,     100,    75);
        createHSVcolor("sea",       150,    100,    100,    100,    85);
        createHSVcolor("seagreen",  150,    100,    100,    100,    85);
        createHSVcolor("cyan",      180,    100,    85,     85,     80);
        createHSVcolor("sky",       210,    75,     80,     95,     90);
        createHSVcolor("skyblue",   210,    75,     80,     95,     90);
        createHSVcolor("blue",      240,    70,     80,     100,    100);
        createHSVcolor("purple",    275,    75,     100,    100,    85);
        createHSVcolor("magenta",   300,    95,     100,    100,    90);
        createHSVcolor("hotpink",   335,    100,    100,    100,    90);
        createHSVcolor("pink",      350,    55,     75,     100,    90);
        createHSVcolor("peach",     25,     75,     75,     100,    90);
        createHSVcolor("lilac",     275,    55,     75,     100,    80);
        createHSVcolor("pinktint",  340,    30,     100,    100,    55);
        createHSVcolor("peachtint", 25,     50,     100,    100,    60);
        createHSVcolor("yellowtint",60,     50,     100,    100,    75);
        createHSVcolor("paleyellow",60,     50,     100,    100,    75);
        createHSVcolor("greentint", 135,    40,     100,    100,    35);
        createHSVcolor("bluetint",  220,    40,     100,    100,    50);
        createHSVcolor("lilactint", 275,    35,     100,    100,    45);
        createHSVcolor("white",     0,      0,      0,      100,    0);
        createHSVcolor("gray",      0,      0,      0,      50,     40);
        createHSVcolor("grey",      0,      0,      0,      50,     40);
        createHSVcolor("brown",     20,     45,     45,     75,     55);
        
        Color[][] solidwhite = uniformColorArray(Color.white);
        Color[][] solidblack = uniformColorArray(Color.black);
        
        addEntry("deadwhite", solidwhite, solidwhite);
        addEntry("deadblack", solidblack, solidblack);
        addEntry("black"    , solidblack, solidblack);
        
        aspecttable = new int[256];
        Arrays.fill(aspecttable, 0);
        addAspect('A', "red");
        addAspect('B', "orange");
        addAspect('C', "gold");
        addAspect('D', "yellow");
        addAspect('E', "lime");
        addAspect('F', "green");
        addAspect('G', "sea");
        addAspect('H', "cyan");
        addAspect('I', "sky");
        addAspect('J', "blue");
        addAspect('K', "purple");
        addAspect('L', "magenta");
        addAspect('M', "hotpink");
        addAspect('N', "pink");
        addAspect('O', "lilac");
        addAspect('P', "peach");
        addAspect('Q', "peachtint");
        addAspect('R', "yellowtint");
        addAspect('S', "greentint");
        addAspect('T', "bluetint");
        addAspect('U', "lilactint");
        addAspect('V', "pinktint");
        addAspect('W', "white");
        addAspect('X', "gray");
        addAspect('Y', "brown");
        addAspect('Z', "invisible");
    }
    
    void addAspect(char key, String color)
    {
        int index = getIndex(color);
        int upper = Character.toUpperCase(key);
        int lower = Character.toLowerCase(key);
        aspecttable[upper] = aspecttable[lower] = index;
    }
//}}}

//{{{ addEntry
//##################################################################################################
    /**
    * Adds color entries to both white and black palettes.
    */
    void addEntry(String name, Color[][] forBlack, Color[][] forWhite)
    {
        int i;
        Integer colorkey;
        if(nametable.containsKey(name))
        {
            colorkey = (Integer)nametable.get(name);
            i        = colorkey.intValue();
        }
        else
        {
            i        = black.size();
            colorkey = new Integer(i);
            nametable.put(  name,       colorkey);
            indextable.put( colorkey,   name);
        }
        
        black.putEntry(forBlack, i);
        white.putEntry(forWhite, i);
        
        blackMono.putEntry( createAsMonochrome(forBlack), i );
        whiteMono.putEntry( createAsMonochrome(forWhite), i );
    }
//}}}

//{{{ add/createHSVcolor, getHSB
//##################################################################################################
    /**
    * Creates a new color definition based on hue (0-360), saturation (0-100),
    * and relative value (0-100; usually 75-100); and adds it to a list
    * of colors known to be re-defined in this kinemage.
    */
    public void addHSVcolor(String name, float hue, float blackSat, float whiteSat, float blackVal, float whiteVal)
    {
        createHSVcolor(name, hue, blackSat, whiteSat, blackVal, whiteVal);
        newColorNames.add(name);
    }

    /**
    * Creates a new color definition based on hue (0-360), saturation (0-100),
    * and relative value (0-100; usually 75-100); WITHOUT marking it as a new color.
    */
    void createHSVcolor(String name, float hue, float blackSat, float whiteSat, float blackVal, float whiteVal)
    {
        Color[] bcolors = new Color[5];
        Color[] wcolors = new Color[5];

        // Cone of colors display
        //DecimalFormat df = new DecimalFormat("0.0###");
        //System.out.println("{"+name+"} "+name+" "+df.format(Math.sin(Math.toRadians(hue))*blackSat*blackVal/100f)+" "+df.format(Math.cos(Math.toRadians(hue))*blackSat*blackVal/100f)+" "+df.format(blackVal));
        //System.out.println("{"+name+"} "+name+" "+df.format(Math.sin(Math.toRadians(hue))*whiteSat*whiteVal/100f)+" "+df.format(Math.cos(Math.toRadians(hue))*whiteSat*whiteVal/100f)+" "+df.format(whiteVal));

        hue /= 360f;
        blackSat /= 100f;
        whiteSat /= 100f;
        blackVal /= 100f;
        whiteVal /= 100f;
        
        // value decreases going back
        bcolors[4] = getHSB(hue, blackSat, 1.00f*blackVal); //front
        bcolors[3] = getHSB(hue, blackSat, 0.84f*blackVal);
        bcolors[2] = getHSB(hue, blackSat, 0.68f*blackVal);
        bcolors[1] = getHSB(hue, blackSat, 0.52f*blackVal);
        bcolors[0] = getHSB(hue, blackSat, 0.36f*blackVal); //back
        
        // value increases, saturation decreases going back
        wcolors[4] = getHSB(hue, 1.00f*whiteSat, Math.min(1f, 0.00f+whiteVal*1.00f)); //front
        wcolors[3] = getHSB(hue, 0.84f*whiteSat, Math.min(1f, 0.15f+whiteVal*0.85f));
        wcolors[2] = getHSB(hue, 0.68f*whiteSat, Math.min(1f, 0.30f+whiteVal*0.70f));
        wcolors[1] = getHSB(hue, 0.52f*whiteSat, Math.min(1f, 0.45f+whiteVal*0.55f));
        wcolors[0] = getHSB(hue, 0.36f*whiteSat, Math.min(1f, 0.60f+whiteVal*0.40f)); //back
        
        Color[][] bshades = createBlackShades(bcolors);
        Color[][] wshades = createWhiteShades(wcolors);
        addEntry(name, bshades, wshades);
    }
    
    Color getHSB(float hue, float sat, float val)
    {
        int rgb = Color.HSBtoRGB(hue, sat, val);
        //int r   = (rgb >> 16) & 0xff;
        //int g   = (rgb >>  8) & 0xff;
        //int b   = (rgb      ) & 0xff;
        //System.out.println("HSB ("+hue+" "+sat+" "+val+")   RGB ("+r+" "+g+" "+b+")");
        return new Color(rgb);
    }
//}}}

//{{{ uniformColorArray, blendColors
//##################################################################################################
    /** Makes a 2-D array of colors, all with the same value */
    Color[][] uniformColorArray(Color c)
    {
        Color[][] shades = new Color[SHADE_LEVELS][COLOR_LEVELS];
        for(int i = 0; i < SHADE_LEVELS; i++)
        {
            for(int j = 0; j < COLOR_LEVELS; j++)
            {
                shades[i][j] = c;
            }
        }
        return shades;
    }
    
    /** Returns a blend of two colors, weighted by the given coefficients */
    Color blendColors(Color c1, double w1, Color c2, double w2)
    {
        int red     = (int)(c1.getRed()*w1 + c2.getRed()*w2);
        int green   = (int)(c1.getGreen()*w1 + c2.getGreen()*w2);
        int blue    = (int)(c1.getBlue()*w1 + c2.getBlue()*w2);
        
        if(red < 0) red = 0;
        else if(red > 255) red = 255;
        if(green < 0) green = 0;
        else if(green > 255) green = 255;
        if(blue < 0) blue = 0;
        else if(blue > 255) blue = 255;
        
        return new Color(red, green, blue);
    }
//}}}

//{{{ createBlackShades
//##################################################################################################
    /**
    * Takes an array of depth-cued colors for a black background,
    * and returns a 2-D array of the same colors with a simple
    * lighting model (diffuse and ambient) applied:
    *
    * <p>C = Fa*Lc*Oc + Fd*Lc*Oc*(On . -Ln)
    *
    * <p>where C is the resulting color, Fa and Fd are the ambient
    * and diffuse lighting coefficients, Lc and Oc are color curves
    * for the light source and the object, and Ln and On are normals
    * for the light source and the object.
    *
    * The resulting array is indexed as color[dot][depth],
    * where dot is the cosine of the angle between the lighting
    * and object normal vectors on the interval [0,1) multiplied by SHADE_LEVELS;
    * and depth is a depth cue from 0 (far away) to COLOR_LEVELS-1 (near).
    */
    Color[][] createBlackShades(Color[] depthcued)
    {
        Color[][] shades = new Color[SHADE_LEVELS][COLOR_LEVELS];
        Color[] background = new Color[] {Color.black, Color.black, Color.black, Color.black, Color.black};
        
        double weight;
        for(int i = 0; i < SHADE_LEVELS; i++)
        {
            weight = AMBIENT_COEFF + (DIFFUSE_COEFF*(i+1))/(SHADE_LEVELS);
            shades[i][4] = blendColors(depthcued[4], weight, background[4], (1-weight));
            shades[i][3] = blendColors(depthcued[3], weight, background[3], (1-weight));
            shades[i][2] = blendColors(depthcued[2], weight, background[2], (1-weight));
            shades[i][1] = blendColors(depthcued[1], weight, background[1], (1-weight));
            shades[i][0] = blendColors(depthcued[0], weight, background[0], (1-weight));
        }
        
        return shades;
    }
//}}}

//{{{ createWhiteShades
//##################################################################################################
    /**
    * Takes an array of depth-cued colors for a white background,
    * and returns a 2-D array of the same colors with a simple
    * lighting model (diffuse and ambient) applied:
    *
    * <p>C = Fa*Lc*Oc + Fd*Lc*Oc*(On . -Ln)
    *
    * <p>where C is the resulting color, Fa and Fd are the ambient
    * and diffuse lighting coefficients, Lc and Oc are color curves
    * for the light source and the object, and Ln and On are normals
    * for the light source and the object.
    *
    * The resulting array is indexed as color[dot][depth],
    * where dot is the cosine of the angle between the lighting
    * and object normal vectors on the interval [0,1) multiplied by SHADE_LEVELS;
    * and depth is a depth cue from 0 (far away) to COLOR_LEVELS-1 (near).
    */
    Color[][] createWhiteShades(Color[] depthcued)
    {
        Color[][] shades = new Color[SHADE_LEVELS][COLOR_LEVELS];
        // Instead of blending toward black, we're going to blend
        // toward black depthcued on a white background:
        Color[] background = new Color[] {
            getHSB(0f, 0f, 0.00f),
            getHSB(0f, 0f, 0.15f),
            getHSB(0f, 0f, 0.30f),
            getHSB(0f, 0f, 0.45f),
            getHSB(0f, 0f, 0.60f),
        };
        
        double weight;
        for(int i = 0; i < SHADE_LEVELS; i++)
        {
            weight = AMBIENT_COEFF + (DIFFUSE_COEFF*(i+1))/(SHADE_LEVELS);
            shades[i][4] = blendColors(depthcued[4], weight, background[4], (1-weight));
            shades[i][3] = blendColors(depthcued[3], weight, background[3], (1-weight));
            shades[i][2] = blendColors(depthcued[2], weight, background[2], (1-weight));
            shades[i][1] = blendColors(depthcued[1], weight, background[1], (1-weight));
            shades[i][0] = blendColors(depthcued[0], weight, background[0], (1-weight));
        }
        
        return shades;
    }
//}}}

//{{{ createAsMonochrome
//##################################################################################################
    /**
    * Duplicates an array of colors while translating each one into monochrome.
    * The formula used was taken from the POV-Ray documentation:
    * <code>gray value = Red*29.7% + Green*58.9% + Blue*11.4%</code>.
    * Presumably this roughly matches the response of B&amp;W film,
    * based on some articles I've read elsewhere.
    */
    public Color[][] createAsMonochrome(Color[][] src)
    {
        Color[][] targ = new Color[src.length][];
        for(int i = 0; i < src.length; i++)
        {
            targ[i] = new Color[ src[i].length ];
            for(int j = 0; j < src[i].length; j++)
            {
                Color sc = src[i][j];
                float gray = (0.297f*sc.getRed() + 0.589f*sc.getGreen() + 0.114f*sc.getBlue()) / 255f;
                if(gray < 0) gray = 0;
                if(gray > 1) gray = 1;
                targ[i][j] = new Color(gray, gray, gray);
            }
        }
        return targ;
    }
//}}}

//{{{ aliasColor
//##################################################################################################
    /**
    * Creating "aliases" for existing colors allows simple implementation of colorsets.
    */
    public void aliasColor(String newcolor, String existingcolor)
    {
        int i = getIndex(existingcolor);
        if(i < 0) i = 0;
        
        Color[][] bcolors = black.getEntry(i);
        Color[][] wcolors = white.getEntry(i);
        addEntry(newcolor, bcolors, wcolors);
        newColorNames.add(newcolor);
        aliasedColors.add(newcolor+" "+existingcolor);
    }
    
    public Collection getAliases()
    { return Collections.unmodifiableCollection(aliasedColors); }
//}}}

//{{{ getUniqueColorNames
//##################################################################################################
    /**
    * Returns a list of all the preferred names of unique Mage colors
    * plus the names of any new colors that have been defined.
    */
    public java.util.List getUniqueColorNames()
    {
        ArrayList allnames = new ArrayList();
        allnames.addAll(STD_COLOR_NAMES);
        allnames.addAll(newColorNames);
        return allnames;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ INNER CLASS: Palette
//##################################################################################################
    /** A simple class for storing colors */
    public static class Palette
    {
        Color       background;
        ArrayList   foreground;
        
        /** Constructor */
        public Palette(Color bg)
        {
            background = bg;
            foreground = new ArrayList(51);
        }
        
        /** Gets the background color for this palette */
        public Color getBackground()
        { return background; }
        
        /** Returns the number of color entries in this palette */
        public int size()
        { return foreground.size(); }
        
        /**
        * Gets an array of Colors for the specified index.
        * @throws IndexOutOfBoundsException if (colorkey &gt;= size() || colorkey &lt; 0)
        */
        public Color[][] getEntry(int colorkey)
        { return (Color[][])foreground.get(colorkey); }
        
        /** Gets a set of COLOR_LEVELS colors with full lighting */
        public Color[] getColors(int colorkey)
        { return getEntry(colorkey)[SHADE_LEVELS-1]; }
        
        /** Gets a set of COLOR_LEVELS colors with specified lighting */
        public Color[] getShadedColors(int colorkey, double dotprod)
        {
            if(dotprod < 0) dotprod = -dotprod;
            int dot = (int)(dotprod*SHADE_LEVELS);
            if(dot >= SHADE_LEVELS) dot = SHADE_LEVELS-1;
            return getEntry(colorkey)[dot];
        }
        
        /**
        * Enters an array of Colors at the specified index, replacing the old entry if necessary.
        * If the index is greater than the number of entries or less than 0,
        * the colors will be assigned the next available index.
        * @return the actual index assigned to this set of Colors
        */
        public int putEntry(Color[][] colors, int colorkey)
        {
            int size = foreground.size();
            if(colorkey >= 0 && colorkey < size)
            {
                foreground.set(colorkey, colors.clone());
                return colorkey;
            }
            else
            {
                foreground.add(colors.clone());
                return size;
            }
        }
    }
//}}}
}//class

