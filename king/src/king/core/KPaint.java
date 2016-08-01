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
import driftwood.r3.*; // (ARK Spring2010)
//}}}
/**
* <code>KPaint</code> encapsulates all the data about a single
* named Mage color, like "red" or "lilactint" or "invisible".
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 25 14:37:56 EDT 2003
*/
abstract public class KPaint //extends ... implements ...
{
//{{{ Constants
    /** The nominal "black" color to be used as a background, etc. */
    public static final Color black = new Color(0.00f, 0.00f, 0.00f);
    /** The nominal "white" color to be used as a background, etc. */
    public static final Color white = new Color(1.00f, 1.00f, 1.00f);
    //public static final Color white = new Color(0.95f, 0.95f, 0.95f);

    /** The number of different depth-cueing levels available */
    public static final int COLOR_LEVELS = 16;
    
    /**
    * The minimum value multiplier on a black background.
    * Smaller numbers mean we fade closer to black before clipping.
    */
    static final float      BVAL            = 0.36f;
    
    /** The minimum value multiplier on a white background. */
    static final float      WVAL            = 0.40f;
    /** The minimum saturation multiplier on a white background. */
    static final float      WSAT            = 0.36f;
    
    /** Shading parameters for ribbons / triangles */
    static final double     AMBIENT_COEFF   = 0.4;
    static final double     DIFFUSE_COEFF   = 0.6;
    
    public static final int        BLACK_COLOR     = 0;
    public static final int        WHITE_COLOR     = 1;
    public static final int        BLACK_MONO      = 2;
    public static final int        WHITE_MONO      = 3;
    public static final int        N_BACKGROUNDS   = 4;
//}}}

//{{{ Constants for ribbon shading
    /** Background colors for ribbon shading calcs. */
    static final Color[][] SHADE_BACKGROUNDS = new Color[N_BACKGROUNDS][COLOR_LEVELS];
    static
    {
        Color[] background = new Color[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
            background[i] = black;
        SHADE_BACKGROUNDS[BLACK_COLOR]  = background;
        SHADE_BACKGROUNDS[BLACK_MONO]   = background;
        
        // Instead of blending toward black, we're going to blend
        // toward black depthcued on a white background:
        // XXX: needs to be updated to use KPaint.white ...
        float[] values = interpolate(1-WVAL, 0, COLOR_LEVELS);
        background = new Color[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
            background[i] = new Color(Color.HSBtoRGB(0f, 0f, values[i]));
        SHADE_BACKGROUNDS[WHITE_COLOR]  = background;
        SHADE_BACKGROUNDS[WHITE_MONO]   = background;
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    String          name;
    KPaint          aliasOf;
    boolean         isInvisible;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor -- use create___() functions instead
    */
    KPaint()
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
        
        HeavyKPaint p   = new HeavyKPaint();
        p.name          = name;
        p.aliasOf       = null;
        p.paints        = new Color[N_BACKGROUNDS][];
        p.paintsBackup  = p.paints;
        p.isInvisible   = false;
        
        hue         /= 360f;
        blackSat    /= 100f;
        whiteSat    /= 100f;
        blackVal    /= 100f;
        whiteVal    /= 100f;
        
        // value decreases going back
        Color[] bcolors = new Color[COLOR_LEVELS];
        float[] bvalues = interpolate(BVAL, 1, COLOR_LEVELS);
        for(int i = 0; i < COLOR_LEVELS; i++)
            bcolors[i] = getHSB(hue, blackSat, bvalues[i]*blackVal);
        
        // value increases, saturation decreases going back
        Color[] wcolors = new Color[COLOR_LEVELS];
        float[] wsats = interpolate(WSAT, 1, COLOR_LEVELS);
        // Low end is a blend between specified value and pure white value
        float[] wvalues = interpolate( (WVAL*whiteVal + (1f-WVAL)*1f) , whiteVal, COLOR_LEVELS);
        for(int i = 0; i < COLOR_LEVELS; i++)
            wcolors[i] = getHSB(hue, wsats[i]*whiteSat, wvalues[i]);
        
        p.paints[BLACK_COLOR]   = bcolors;
        p.paints[WHITE_COLOR]   = wcolors;
        p.paints[BLACK_MONO]    = makeMonochrome(p.paints[BLACK_COLOR]);
        p.paints[WHITE_MONO]    = makeMonochrome(p.paints[WHITE_COLOR]);
        
        return p;
    }
    
    static private Color getHSB(float hue, float sat, float val)
    { return new Color(Color.HSBtoRGB(hue, sat, val)); }
//}}}

//{{{ createLightweightHSV
//##################################################################################################
    /**
    * Creates a new color definition based on hue (0-360), saturation (0-100),
    * and relative value (0-100; usually 75-100).
    */
    static public KPaint createLightweightHSV(String name, float bHue, float bSat, float bVal, float wHue, float wSat, float wVal)
    {
        if(name == null)
            throw new NullPointerException("Must give paint a name");
        
        KPaint p = new LightKPaint(bHue / 360f, bSat / 100f, bVal / 100f,
            wHue / 360f, wSat / 100f, wVal / 100f);
        
        p.name          = name;
        p.aliasOf       = null;
        p.isInvisible   = false;
        
        return p;
    }
//}}}

//{{{ createSolid
//##################################################################################################
    /**
    * Creates a paint that is the same regardless of shading, depth-cueing, etc.
    */
    static public KPaint createSolid(String name, Color solid)
    {
        if(name == null)
            throw new NullPointerException("Must give paint a name");
        if(solid == null)
            throw new NullPointerException("Must provide a valid Color");
        
        HeavyKPaint p   = new HeavyKPaint();
        p.name          = name;
        p.aliasOf       = null;
        p.paints        = new Color[N_BACKGROUNDS][COLOR_LEVELS];
        p.paintsBackup  = p.paints;
        p.isInvisible   = false;
        
        for(int i = 0; i < N_BACKGROUNDS; i++)
                for(int j = 0; j < COLOR_LEVELS; j++)
                    p.paints[i][j] = solid;
        
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
        
        HeavyKPaint t   = (HeavyKPaint) target;
        HeavyKPaint p   = new HeavyKPaint();
        p.name          = name;
        p.aliasOf       = t;
        p.paints        = t.paints;
        p.paintsBackup  = p.paints;
        p.isInvisible   = t.isInvisible;
        
        return p;
    }
    
    /**
    * Create a color marked as invisible that is very
    * distinctive if actually painted.
    */
    static public KPaint createInvisible(String name)
    {
        // Wasn't worth saying Paint instead of Color for this one trick
        //Paint invis     = new GradientPaint(0, 0, Color.red, 11, 3, Color.green, true);
        Color invis     = new Color(51, 51, 51);
        KPaint p        = createSolid(name, invis);
        p.isInvisible   = true;
        return p;
    }
//}}}

//{{{ getPaint(s)
//##################################################################################################
    /**
    * Returns the correct Color object to use for rendering.
    * Based on the value of dotprod, a simple
    * lighting model (diffuse and ambient) is applied:
    *
    * <p>C = Fa*Lc*Oc + Fd*Lc*Oc*(On . -Ln)
    *
    * <p>where C is the resulting color, Fa and Fd are the ambient
    * and diffuse lighting coefficients, Lc and Oc are color curves
    * for the light source and the object, and Ln and On are normals
    * for the light source and the object.
    *
    * These color objects are calculated on the fly rather than being cached.
    *
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    * @param dotprod the (normalized) dot product of the surface normal with the lighting vector.
    * @param depth the depth cue number, from 0 (far away) to COLOR_LEVELS-1 (near by).
    * @param alpha the transparency, from 0 (transparent) to 255 (opaque).
    */
    public Color getPaint(int backgroundMode, double dotprod, int depth, int alpha)
    {
        Color basePaint = getPaint(backgroundMode, depth);
        
        if(dotprod < 0) dotprod = -dotprod;
        if(dotprod > 1) dotprod = 1;
        
        if(dotprod == 1 && alpha == 255)
            return basePaint;

        try
        {
            double weight = AMBIENT_COEFF + (DIFFUSE_COEFF*dotprod);
            return blendColors(
                basePaint, weight,
                SHADE_BACKGROUNDS[backgroundMode][depth], (1-weight),
                alpha);
        }
        catch(ClassCastException ex)
        {
            // Cast will only fail for the Invisible color, which
            // uses a gradient paint. BUT we should never be calling
            // this method if we already know our paint is invisible!
            return basePaint;
        }
    }
    
    /**
    * Returns the correct offset Color object to use for rendering.
    * HSV offset is based on the first parameter (a Triple).
    *
    * @param HSV offset X = hue offset, Y = saturation offset, Z = value (brightness) offset.
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    * @param dotprod the (normalized) dot product of the surface normal with the lighting vector.
    * @param depth the depth cue number, from 0 (far away) to COLOR_LEVELS-1 (near by).
    * @param alpha the transparency, from 0 (transparent) to 255 (opaque).
    */
    public Color getOffsetPaint(Triple HSVoffset, int backgroundMode, double dotprod, int depth, int alpha) // (ARK Spring2010)
    {
	Color paintColor = (Color) this.getPaint(backgroundMode, dotprod, depth, alpha);
	float hueDif = (float)HSVoffset.getX(); 
	float satDif = (float)HSVoffset.getY(); 
	float valDif = (float)HSVoffset.getZ(); 
	float[] hsbvals = paintColor.RGBtoHSB(paintColor.getRed(),paintColor.getGreen(),paintColor.getBlue(),null);
	
	// To calculate the new color, must do some some scaling:
	//
	// RGBtoHSB() returns: 
	// hsbvals[0] = hue (0-1)
	// hsbvals[1] = saturation (0-1)
	// hsbvals[2] = brightness (0-1)
	//
	// createHSV() takes: 
	// hue (0-360)
	// blackSat, whiteSat (0-100)
	// blackVal, whiteVal (0-100; usually 75-100)

	hsbvals[0]*=360;
	hsbvals[0]=hsbvals[0]<(360-hueDif) ? hsbvals[0]+hueDif : hsbvals[0]-(360-hueDif);
	hsbvals[1]*=100;
	hsbvals[1]*=((double)satDif/100);
	//hsbvals[1]=hsbvals[1]<(100-satDif) ? hsbvals[1]+satDif : hsbvals[1]-(100-satDif);
	// **As such, this doesn't allow user to explore the full range of sat or val,
	// **since their spectrum is linear rather than circular like hue's,
	// **so we can only decrease the value but have continuous movement in doing so 
	hsbvals[2]=hsbvals[2]*25+75;
	hsbvals[2]*=((double)valDif/100);
	
	return this.createHSV("backColor",hsbvals[0],hsbvals[1],hsbvals[1],hsbvals[2],hsbvals[2]).getPaint(backgroundMode, dotprod, depth, alpha);
    }
    
    
    /**
    * Returns the correct Color object to use for rendering,
    * assuming direct lighting of the surface and no transparency.
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    * @param depth the depth cue number, from 0 (far away) to COLOR_LEVELS-1 (near by).
    */
    abstract public Color getPaint(int backgroundMode, int depth);
    
    /**
    * Returns the set of depth-cued Color objects to use for rendering,
    * indexed from 0 (far away) to COLOR_LEVELS-1 (near by),
    * assuming direct lighting of the surface and no transparency.
    * @param backgroundMode one of BLACK_COLOR, WHITE_COLOR, BLACK_MONO, or WHITE_MONO.
    */
    abstract public Color[] getPaints(int backgroundMode);
//}}}

//{{{ blendColors, makeMonochrome
//##################################################################################################
    /**
    * Returns a blend of two colors, weighted by the given coefficients.
    * Alpha ranges from 0 (transparent) to 255 (opaque) and is not taken from either c1 or c2.
    */
    private static Color blendColors(Color c1, double w1, Color c2, double w2, int alpha)
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
        
        return new Color(red, green, blue, alpha);
    }
    
    /**
    * Duplicates an array of colors while translating each one into monochrome.
    * The formula used was taken from the POV-Ray documentation:
    * <code>gray value = Red*29.7% + Green*58.9% + Blue*11.4%</code>.
    * Presumably this roughly matches the response of B&amp;W film,
    * based on some articles I've read elsewhere.
    * <p>See also http://www.poynton.com/notes/colour_and_gamma/GammaFAQ.html,
    * which offers this equation: Y(709) = 0.2126*R + 0.7152*G + 0.0722*B.
    * However, using it directly here would probably be out of context...
    */
    private static Color[] makeMonochrome(Color[] src)
    {
        Color[] targ = new Color[src.length];
        for(int i = 0; i < src.length; i++)
            targ[i] = makeMonochrome(src[i]);
        return targ;
    }
    
    /**
    * Translates a color into monochrome.
    * The formula used was taken from the POV-Ray documentation:
    * <code>gray value = Red*29.7% + Green*58.9% + Blue*11.4%</code>.
    * Presumably this roughly matches the response of B&amp;W film,
    * based on some articles I've read elsewhere.
    * <p>See also http://www.poynton.com/notes/colour_and_gamma/GammaFAQ.html,
    * which offers this equation: Y(709) = 0.2126*R + 0.7152*G + 0.0722*B.
    * However, using it directly here would probably be out of context...
    */
    static Color makeMonochrome(Color sc)
    {
        float gray = (0.297f*sc.getRed() + 0.589f*sc.getGreen() + 0.114f*sc.getBlue()) / 255f;
        if(gray < 0) gray = 0;
        if(gray > 1) gray = 1;
        return new Color(gray, gray, gray, sc.getAlpha()/255f);
    }
//}}}

//{{{ interpolate
//##################################################################################################
    /** Blends linearly from start to end, with steps-2 intervening steps. */
    static float[] interpolate(double start, double end, int steps)
    {
        float[] result = new float[steps];
        for(int i = 0; i < steps; i++)
        {
            double a = (double)i / (double)(steps-1);
            result[i] = (float)((1-a)*start + a*end);
        }
        return result;
    }
//}}}

//{{{ get{Black, White}Exemplar
//##################################################################################################
    /**
    * Returns the most typical paint for this named color.
    * The hue, saturation, and value
    * can be fed back into createHSV to recreate this color (probably).
    * Remember to convert from the [0,1] scale to the [0,360] / [0,100] scale!
    */
    public Color getBlackExemplar()
    { return getPaint(BLACK_COLOR, COLOR_LEVELS-1); }

    /**
    * Returns the most typical paint for this named color.
    * The hue, saturation, and value
    * can be fed back into createHSV to recreate this color (probably).
    * Remember to convert from the [0,1] scale to the [0,360] / [0,100] scale!
    */
    public Color getWhiteExemplar()
    { return getPaint(WHITE_COLOR, COLOR_LEVELS-1); }
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

//{{{ setContrast
//##################################################################################################
    /**
    * Set the relative contrast of the color palette.
    * A contrast of less than 1.0 is flat, and greater than 1.0 is exagerated.
    * <p>Adjustment is done as linear interpolation/extrapolation to a mid-level gray.
    * See http://www.sgi.com/misc/grafica/interp/ or
    * P. Haeberli and D. Voorhies. Image Processing by Linear Interpolation and
    * Extrapolation. IRIS Universe Magazine No. 28, Silicon Graphics, Aug, 1994.
    * 
    * Only implemented for HeavyKPaint.
    */
    public void setContrast(double alpha) {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

/** A lightweight paint class for user-defined colors. */
class LightKPaint extends KPaint //{{{
{
    float bHue, bSat, bVal;
    float wHue, wSat, wVal;
    
    public LightKPaint(float bHue, float bSat, float bVal, float wHue, float wSat, float wVal)
    {
        super();
        this.bHue = bHue;
        this.wHue = wHue;
        this.bSat = bSat;
        this.wSat = wSat;
        this.bVal = bVal;
        this.wVal = wVal;
    }
    
    public Color getPaint(int backgroundMode, int depth)
    {
        double modHue, modSat, modVal;
        Color color;
        // Interpolation coefficient determined by depth
        double d = (double)depth / (double)(COLOR_LEVELS-1);
        
        if(backgroundMode == WHITE_COLOR || backgroundMode == WHITE_MONO)
        {
            modHue = wHue;
            // Saturation interpolated from sat*WSAT at the back to sat*1.0 at the front
            modSat = wSat * ((1-d)*WSAT + (d)*1);
            // Value interpolated from a blend of val and pure white at the back
            // to plain old val at the front.
            modVal = (1-d)*(WVAL*wVal + (1f-WVAL)*1f) + (d)*wVal;
        }
        else // BLACK_COLOR || BLACK_MONO
        {
            modHue = bHue;
            // Saturation is unchanged
            modSat = bSat;
            // Value interpolated from val*BVAL at the back to val*1.0 at the front
            modVal = bVal * ((1-d)*BVAL + (d)*1);
        }
        
        color = new Color(Color.HSBtoRGB((float) modHue, (float) modSat, (float) modVal));
        if(backgroundMode == WHITE_MONO || backgroundMode == BLACK_MONO)
            color = makeMonochrome(color);
        
        return color;
    }
    
    public Color[] getPaints(int backgroundMode)
    {
        Color[] paints = new Color[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
            paints[i] = getPaint(backgroundMode, i);
        return paints;
    }
}//}}}

/** A memory-intensive (~2500 bytes) paint class for built-in colors. */
class HeavyKPaint extends KPaint //{{{
{
    Color[][]       paints;
    Color[][]       paintsBackup; // Usually == to paints. See setContrast().

    public Color getPaint(int backgroundMode, int depth)
    {
        return paints[backgroundMode][depth];
    }
    
    public Color[] getPaints(int backgroundMode)
    {
        return paints[backgroundMode];
    }
    
    public void setContrast(double alpha)
    {
        this.paints = new Color[N_BACKGROUNDS][COLOR_LEVELS];
        final double midgray = 0.5;
        final double one_minus_alpha = 1.0 - alpha;
        
        for(int i = 0; i < N_BACKGROUNDS; i++)
        {
            for(int j = 0; j < COLOR_LEVELS; j++)
            {
                Color p = paintsBackup[i][j];
                float r = (float)(alpha * p.getRed()/255.0   + one_minus_alpha * midgray);
                r = Math.max(Math.min(1, r), 0);
                float g = (float)(alpha * p.getGreen()/255.0 + one_minus_alpha * midgray);
                g = Math.max(Math.min(1, g), 0);
                float b = (float)(alpha * p.getBlue()/255.0  + one_minus_alpha * midgray);
                b = Math.max(Math.min(1, b), 0);
                paints[i][j] = new Color(r, g, b);
            }
        }
    }
}//}}}

