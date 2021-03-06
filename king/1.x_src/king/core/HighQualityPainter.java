// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
import java.awt.geom.*;
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
* <code>HighQualityPainter</code> paints kinemage graphics using the new-style
* Shape calls from a java.awt.Graphics2D object.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 21 19:20:49 EDT 2004
*/
public class HighQualityPainter extends StandardPainter
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Line2D.Double    line1      = new Line2D.Double();
    Ellipse2D.Double ellipse1   = new Ellipse2D.Double();
    GeneralPath      path1      = new GeneralPath();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public HighQualityPainter(boolean forceAntialiasing)
    {
        super(forceAntialiasing);
    }
//}}}

//{{{ paintBall
//##################################################################################################
    public void paintBall(Paint paint, double x, double y, double z, double r, boolean showHighlight)
    {
        if(r < 0.5) r = 0.5; // make sure balls don't disappear
        double d = 2.0*r;
        
        // one disk
        g.setPaint(paint);
        g.setStroke(KPalette.pen0);
        ellipse1.setFrame((x-r), (y-r), d, d);
        if(REALLY_PAINT) g.fill(ellipse1);

        // Black rim, causes ~10% speed penalty but useful for visualization!
        try
        {
            Color c = (Color) paint;
            if(d >= 10 && c.getAlpha() == 255)
            {
                g.setPaint(KPaint.black); // wants to merge with the background
                g.setStroke(KPalette.pen1);
                if(REALLY_PAINT) g.draw(ellipse1);
            }
        }
        catch(ClassCastException ex)
        {
            ex.printStackTrace();
            System.err.println("HighQualityPainter: tried painting with non-Color type of Paint");
        }
        
        // highlight
        if(showHighlight)
        {
            double off = 0.5 * r;
            d = 0.3*r;
            g.setPaint(Color.white); // wants to be bright white no matter what
            g.setStroke(KPalette.pen0);
            ellipse1.setFrame((x-off), (y-off), d, d);
            if(REALLY_PAINT) g.fill(ellipse1);
        }
    }
//}}}

//{{{ paintDot
//##################################################################################################
    public void paintDot(Paint paint, double x, double y, double z, int width)
    {
        int off = width/2;
        g.setPaint(paint);
        g.setStroke(KPalette.pen0);
        ellipse1.setFrame((x-off), (y-off), width, width);
        if(REALLY_PAINT) g.fill(ellipse1);
    }
//}}}

//{{{ paintSphereDisk
//##################################################################################################
    public void paintSphereDisk(Paint paint, double x, double y, double z, double r)
    {
        if(r < 0.5) r = 0.5; // make sure balls don't disappear
        double d = 2.0*r;
        
        // one disk
        g.setPaint(paint);
        g.setStroke(KPalette.pen0);
        ellipse1.setFrame((x-r), (y-r), d, d);
        if(REALLY_PAINT) g.fill(ellipse1);
    }
//}}}

//{{{ paintTriangle
//##################################################################################################
    public void paintTriangle(Paint paint,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3)
    {
        g.setPaint(paint);
        g.setStroke(KPalette.pen1);

        path1.reset();
        path1.moveTo((float)x1, (float)y1);
        path1.lineTo((float)x2, (float)y2);
        path1.lineTo((float)x3, (float)y3);
        path1.closePath();
        if(REALLY_PAINT)
        {
            g.fill(path1);
            g.draw(path1);   // closes up the hairline cracks between triangles (?)
        }
    }
//}}}

//{{{ paintVector
//##################################################################################################
    public void paintVector(Paint paint, int width, int widthCue,
        double x1, double y1, double z1,
        double x2, double y2, double z2)
    {
        g.setPaint(paint);
        g.setStroke(KPalette.pens[width-1][widthCue]);
        line1.setLine(x1, y1, x2, y2);
        if(REALLY_PAINT) g.draw(line1);
    }
//}}}

//{{{ drawOval
//##################################################################################################
    public void drawOval(Paint paint, double x, double y, double z, double width, double height)
    {
        g.setPaint(paint);
        g.setStroke(KPalette.pen1);
        ellipse1.setFrame((x - width/2), (y - height/2), width, height);
        if(REALLY_PAINT) g.draw(ellipse1);
    }

    public void drawOval(Paint paint, int linewidth, int widthCue, double x, double y, double z, double width, double height)
    {
        g.setPaint(paint);
        g.setStroke(KPalette.pens[linewidth-1][widthCue]);
        ellipse1.setFrame((x - width/2), (y - height/2), width, height);
        if(REALLY_PAINT) g.draw(ellipse1);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

