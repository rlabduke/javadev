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
//import driftwood.*;
//}}}
/**
* <code>KPaint</code> encapsulates all the data about a single
* named Mage color, like "red" or "lilactint" or "invisible".
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 25 14:37:56 EDT 2003
*/
public class KPaint //extends ... implements ...
{
//{{{ Constants
    /** The number of different depth-cueing levels available */
    public static final int COLOR_LEVELS = 16;
    
    /** The number of different ribbon-shading levels (angles) available */
    public static final int SHADE_LEVELS = 16;
    
    static final double     AMBIENT_COEFF   = 0.4;
    static final double     DIFFUSE_COEFF   = 0.6;
    
    public static final int        BLACK_COLOR     = 0;
    public static final int        WHITE_COLOR     = 1;
    public static final int        BLACK_MONO      = 2;
    public static final int        WHITE_MONO      = 3;
    public static final int        N_BACKGROUNDS   = 4;
//}}}

//{{{ Variable definitions
//##################################################################################################
    String          name;
    KPaint          aliasOf;
    Paint[][][]     paints;
    boolean         isInvisible;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor -- use create___() functions instead
    */
    private KPaint()
    {
    }
//}}}

//{{{ createHSV, getHSB
//##################################################################################################
    /**
    * Creates a new color definition based on hue (0-360), saturation (0-100),
    * and relative value (0-100; usually 75-100).
    */
    static public KPaint createHSV(String name, float hue, float blackSat, float whiteSat, float blackVal, float whiteVal)
    {
        if(name == null)
            throw new NullPointerException("Must give paint a name");
        
        KPaint p        = new KPaint();
        p.name          = name;
        p.aliasOf       = null;
        p.paints        = new Paint[N_BACKGROUNDS][][];
        p.isInvisible   = false;
        
        hue         /= 360f;
        blackSat    /= 100f;
        whiteSat    /= 100f;
        blackVal    /= 100f;
        whiteVal    /= 100f;
        
        // value decreases going back
        Color[] bcolors = new Color[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
        {
            bcolors[i] = getHSB(hue, blackSat,
                ( 0.36f + 0.64f*i/(COLOR_LEVELS-1) )*blackVal );
        }
        
        // value increases, saturation decreases going back
        Color[] wcolors = new Color[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
        {
            wcolors[i] = getHSB(hue,
                ( 0.36f + 0.64f*i/(COLOR_LEVELS-1) )*whiteSat,
                Math.min(1f, 1f + (whiteVal-1f)*( 0.40f + 0.60f*i/(COLOR_LEVELS-1) )) ); 
        }
        
        p.paints[BLACK_COLOR]   = makeBlackShades(bcolors);
        p.paints[WHITE_COLOR]   = makeWhiteShades(wcolors);
        p.paints[BLACK_MONO]    = makeMonochrome((Color[][])p.paints[BLACK_COLOR]);
        p.paints[WHITE_MONO]    = makeMonochrome((Color[][])p.paints[WHITE_COLOR]);
        
        return p;
    }
    
    static private Color getHSB(float hue, float sat, float val)
    { return new Color(Color.HSBtoRGB(hue, sat, val)); }
//}}}

//{{{ makeBlackShades
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
    private static Color[][] makeBlackShades(Color[] depthcued)
    {
        Color[][] shades = new Color[SHADE_LEVELS][COLOR_LEVELS];
        Color[] background = new Color[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
            background[i] = Color.black;
        
        double weight;
        for(int i = 0; i < SHADE_LEVELS; i++)
        {
            weight = AMBIENT_COEFF + (DIFFUSE_COEFF*(i+1))/(SHADE_LEVELS);
            for(int j = 0; j < COLOR_LEVELS; j++)
                shades[i][j] = blendColors(depthcued[j], weight, background[j], (1-weight));
        }
        
        return shades;
    }
//}}}

//{{{ makeWhiteShades
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
    private static Color[][] makeWhiteShades(Color[] depthcued)
    {
        Color[][] shades = new Color[SHADE_LEVELS][COLOR_LEVELS];
        // Instead of blending toward black, we're going to blend
        // toward black depthcued on a white background:
        Color[] background = new Color[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
            background[i] = getHSB(0f, 0f, 0.60f*i / (COLOR_LEVELS-1));
        
        double weight;
        for(int i = 0; i < SHADE_LEVELS; i++)
        {
            weight = AMBIENT_COEFF + (DIFFUSE_COEFF*(i+1))/(SHADE_LEVELS);
            for(int j = 0; j < COLOR_LEVELS; j++)
                shades[i][j] = blendColors(depthcued[j], weight, background[j], (1-weight));
        }
        
        return shades;
    }
//}}}

//{{{ blendColors, makeMonochrome
//##################################################################################################
    /** Returns a blend of two colors, weighted by the given coefficients */
    private static Color blendColors(Color c1, double w1, Color c2, double w2)
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
        // Experimental: transparency
        //return new Color(red/255f, green/255f, blue/255f, 0.5f);
    }
    
    /**
    * Duplicates an array of colors while translating each one into monochrome.
    * The formula used was taken from the POV-Ray documentation:
    * <code>gray value = Red*29.7% + Green*58.9% + Blue*11.4%</code>.
    * Presumably this roughly matches the response of B&amp;W film,
    * based on some articles I've read elsewhere.
    */
    private static Color[][] makeMonochrome(Color[][] src)
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

//{{{ createSolid
//##################################################################################################
    /**
    * Creates a paint that is the same regardless of shading, depth-cueing, etc.
    */
    static public KPaint createSolid(String name, Paint solid)
    {
        if(name == null)
            throw new NullPointerException("Must give paint a name");
        if(solid == null)
            throw new NullPointerException("Must provide a valid Paint");
        
        KPaint p        = new KPaint();
        p.name          = name;
        p.aliasOf       = null;
        p.paints        = new Paint[N_BACKGROUNDS][SHADE_LEVELS][COLOR_LEVELS];
        p.isInvisible   = false;
        
        for(int i = 0; i < N_BACKGROUNDS; i++)
            for(int j = 0; j < SHADE_LEVELS; j++)
                for(int k = 0; k < COLOR_LEVELS; k++)
                    p.paints[i][j][k] = solid;
        
        return p;
    }
//}}}

//{{{ createAlias, createInvisible
//##################################################################################################
    /**
    * Creates a new color that is simply an alias to some existing color.
    */
    static public KPaint createAlias(String name, KPaint target)
    {
        if(name == null)
            throw new NullPointerException("Must give paint a name");
        if(target == null)
            throw new NullPointerException("Must give paint alias an existing paint to reference");
        
        KPaint p        = new KPaint();
        p.name          = name;
        p.aliasOf       = target;
        p.paints        = target.paints;
        p.isInvisible   = target.isInvisible;
        
        return p;
    }
    
    /**
    * Create a color marked as invisible that is very
    * distinctive if actually painted.
    */
    static public KPaint createInvisible(String name)
    {
        Paint invis     = new GradientPaint(0, 0, Color.red, 11, 3, Color.green, true);
        KPaint p        = createSolid(name, invis);
        p.isInvisible   = true;
        return p;
    }
//}}}

//{{{ getPaint(s)
//##################################################################################################
    /**
    * Returns the correct Paint object to use for rendering.
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    * @param dotprod the dot product of the surface normal with the lighting vector.
    * @param depth the depth cue number, from 0 (far away) to COLOR_LEVELS-1 (near by).
    */
    public Paint getPaint(int backgroundMode, double dotprod, int depth)
    {
        if(dotprod < 0) dotprod = -dotprod;
        int dot = (int)(dotprod*SHADE_LEVELS);
        if(dot >= SHADE_LEVELS) dot = SHADE_LEVELS-1;
        
        return paints[backgroundMode][dot][depth];
    }
    
    /**
    * Returns the correct Paint object to use for rendering,
    * assuming direct lighting of the surface.
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    * @param depth the depth cue number, from 0 (far away) to COLOR_LEVELS-1 (near by).
    */
    public Paint getPaint(int backgroundMode, int depth)
    {
        return paints[backgroundMode][SHADE_LEVELS-1][depth];
    }
    
    /**
    * Returns the set of depth-cued Paint objects to use for rendering,
    * indexed from 0 (far away) to COLOR_LEVELS-1 (near by).
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    * @param dotprod the dot product of the surface normal with the lighting vector.
    */
    public Paint[] getPaints(int backgroundMode, double dotprod)
    {
        if(dotprod < 0) dotprod = -dotprod;
        int dot = (int)(dotprod*SHADE_LEVELS);
        if(dot >= SHADE_LEVELS) dot = SHADE_LEVELS-1;
        
        return paints[backgroundMode][dot];
    }
    
    /**
    * Returns the set of depth-cued Paint objects to use for rendering,
    * indexed from 0 (far away) to COLOR_LEVELS-1 (near by),
    * assuming direct lighting of the surface.
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    */
    public Paint[] getPaints(int backgroundMode)
    {
        return paints[backgroundMode][SHADE_LEVELS-1];
    }
//}}}

//{{{ get{Black, White}Exemplar
//##################################################################################################
    /**
    * Returns the most typical paint for this named color.
    * If this Paint is an instance of Color, the hue, saturation, and value
    * can be fed back into createHSV to recreate this color (probably).
    * Remember to convert from the [0,1] scale to the [0,360] / [0,100] scale!
    */
    public Paint getBlackExemplar()
    { return paints[BLACK_COLOR][SHADE_LEVELS-1][COLOR_LEVELS-1]; }

    /**
    * Returns the most typical paint for this named color.
    * If this Paint is an instance of Color, the hue, saturation, and value
    * can be fed back into createHSV to recreate this color (probably).
    * Remember to convert from the [0,1] scale to the [0,360] / [0,100] scale!
    */
    public Paint getWhiteExemplar()
    { return paints[WHITE_COLOR][SHADE_LEVELS-1][COLOR_LEVELS-1]; }
//}}}

//{{{ isInvisible, isAlias, getAlias, toString
//##################################################################################################
    /** Returns true iff objects of this color should not be rendered. */
    public boolean isInvisible()
    { return isInvisible; }
    
    /** Returns true iff this KPaint is just another name for some canonical KPaint. */
    public boolean isAlias()
    { return aliasOf != null; }
    
    /**
    * Returns the KPaint that this object is an alias of.
    * @throws UnsupportedOperationException if this object is not an alias.
    */
    public KPaint getAlias()
    {
        if(aliasOf != null) return aliasOf;
        else throw new UnsupportedOperationException(this+" is not an alias of some other KPaint");
    }
    
    /** Returns the name this KPaint was created with. Will never be null. */
    public String toString()
    { return name; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

