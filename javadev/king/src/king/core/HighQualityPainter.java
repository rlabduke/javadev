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
    public HighQualityPainter()
    {
        super();
    }
//}}}

//{{{ paintBall
//##################################################################################################
    public void paintBall(Graphics2D g, Paint paint, double x, double y, double z, double r, boolean showHighlight)
    {
        int d = (int)(2.0*r + 0.5);
        if(d < 2) d = 2; // make sure balls don't disappear
        
        // one disk
        g.setPaint(paint);
        g.setStroke(KPalette.pen0);
        ellipse1.setFrame((x-r), (y-r), d, d);
        g.fill(ellipse1);

        // highlight
        if(showHighlight)
        {
            double off = 0.5 * r;
            d = (int)(0.3*r)+1;
            g.setPaint(Color.white);
            g.setStroke(KPalette.pen0);
            ellipse1.setFrame((x-off), (y-off), d, d);
            g.fill(ellipse1);
        }
    }
//}}}

//{{{ paintDot
//##################################################################################################
    public void paintDot(Graphics2D g, Paint paint, double x, double y, double z, int width)
    {
        int off = width/2;
        g.setPaint(paint);
        g.setStroke(KPalette.pen0);
        ellipse1.setFrame((x-off), (y-off), width, width);
        g.fill(ellipse1);
    }
//}}}

//{{{ paintTriangle
//##################################################################################################
    public void paintTriangle(Graphics2D g, Paint paint,
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
        g.fill(path1);
        g.draw(path1);   // closes up the hairline cracks between triangles (?)
    }
//}}}

//{{{ paintVector
//##################################################################################################
    public void paintVector(Graphics2D g, Paint paint, int width, int widthCue,
        double x1, double y1, double z1,
        double x2, double y2, double z2)
    {
        g.setPaint(paint);
        g.setStroke(KPalette.pens[width-1][widthCue]);
        line1.setLine(x1, y1, x2, y2);
        g.draw(line1);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

