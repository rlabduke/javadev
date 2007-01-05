// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.gui.*;
//}}}
/**
* <code>SkylightPlugin</code> uses Skyligher to do lighting calculations for a bunch of kinemage spheres.
* It works for balls, too, but looks weird because of the highlights.
* This is intended to be used to give additional visual depth cues to CPK space-filling models.
* Note that it does obliterate existing point colors, and until we have a mechanism for writing
* out per-point unique (non-palette) colors, the effect can't be saved.
* It can, however, be recalculated once some atoms (eg H) are turned off.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Nov  2 21:23:01 EST 2006
*/
public class SkylightPlugin extends Plugin
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SkylightPlugin(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ getToolsMenuItem, toString
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onSkylight"));
    }
    
    public String toString()
    { return "Artificial skylighting"; }
//}}}
    
//{{{ onSkylight
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSkylight(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k == null) return;
        
        ArrayList balls = new ArrayList();
        for(RecursivePointIterator iter = new RecursivePointIterator(k, false, true); iter.hasNext(); )
        {
            KPoint p = iter.next();
            if(p instanceof BallPoint || p instanceof SpherePoint)
                balls.add(p);
        }
        double[] radii = new double[balls.size()];
        for(int i = 0; i < radii.length; i++)
        {
            KPoint p = (KPoint) balls.get(i);
            KList list = (KList) p.getParent();
            radii[i] = p.getRadius();
            if(radii[i] == 0) radii[i] = list.getRadius();
        }
        
        Collection transforms = new ArrayList();
        Triple zAxis = new Triple(0,0,1), yAxis = new Triple(0,1,0);
        Triple u = new Triple();
        Transform r = new Transform();
        // The idea is to create a dotball to represent all the positions of the "sun",
        // the set up rotations that will bring the "sun" into line with the +Z axis.
        // 16 is a bit nicer than 8.  4 is too few -- too hit and miss.
        // 32 is nicer still, but 64 doesn't gain you anything.
        // 32 and a 0.25 grid is twice as fast as 16 and 0.125 -- and looks as good.
        for(Iterator iter = Builder.makeDotSphere(1, 32).iterator(); iter.hasNext(); )
        {
            Tuple3 v = (Tuple3) iter.next();
            u.likeCross(zAxis, v);
            double angle = u.angle(v);
            Transform t = new Transform();
            if(!Double.isNaN(angle)) t.likeRotation(u, angle);
            transforms.add(t);
        }
    //System.out.println("@text\n"+transforms.size()+" projection directions");
        // This is the slow call that does all the work:
        Skylighter skylight = new Skylighter( (Tuple3[]) balls.toArray( new Tuple3[balls.size()] ),
            radii, (Transform[]) transforms.toArray( new Transform[transforms.size()] ));
        double[] lightness = skylight.getLighting();
        
    //System.out.println("@group {} animate\n@vectorlist {}");
    //for(int i = 0; i < lightness.length; i++)
    //    System.out.println("{} "+i+" "+(0.5*lightness[i]*lightness.length));
        
        for(int i = 0; i < lightness.length; i++)
        {
            KPoint p = (KPoint) balls.get(i);
            KList list = (KList) p.getParent();
            KPaint paint = list.getColor();
            //KPaint paint = KPalette.white; // useful to evaluating the effect during devel.
            // If sphere is hit by at least half the light rays, we consider it maximally exposed.
            // This prevents over-lightening of projecting bits.
            // minLight defines a minimum level of true ambient illumination coming from the observer.
            // We tried a sqrt function on lightness too,
            // but now that other things work right it looks weird.
            double minLight = 0.3; // stronger than this and it looks weird
            double light = minLight + (1-minLight)*Math.min(1, 2*lightness[i]);
            Color c = blendColors(Color.black, 1 - light,
                (Color) paint.getBlackExemplar(), light, 255);
            float[] black = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            p.setColor(KPaint.createLightweightHSV("foo", 360*black[0], 100*black[1], 100*black[2], 360*black[0], 100*black[1], 100*black[2]));
        }
        
        //k.setModified(true); -- can't save it yet anyway!
        kMain.notifyChange(KingMain.EM_EDIT_FINE);
    }
//}}}

//{{{ blendColors
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
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

