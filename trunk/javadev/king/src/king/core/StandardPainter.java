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
* <code>StandardPainter</code> paints kinemage graphics using standard calls
* on a java.awt.Graphics object. Must be initialized with setGraphics()
* before use!
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 21 19:11:16 EDT 2004
*/
public class StandardPainter implements Painter
{
//{{{ Constants
    /*
    * Useful for benchmarking. Setting to false keeps us from drawing to screen.
    * This should optimize away at compile time, like #ifdef in C.
    *
    * On my G4 PowerBook, transforming and all the paint calcs account for
    * only about 10% of the total time required to render one frame.
    */
    protected static final boolean REALLY_PAINT = true;
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean         forceAA;
    Graphics2D      g           = null;
    Font            font        = null;
    FontMetrics     metrics     = null;
    Rectangle       clipRgn     = new Rectangle(0,0,1,1);
    int[]           xPoints     = new int[6];
    int[]           yPoints     = new int[6];
//}}}

//{{{ Constructor(s)
//##############################################################################
    public StandardPainter(boolean forceAntialiasing)
    {
        super();
        this.forceAA = forceAntialiasing;
    }
//}}}

//{{{ paintBall
//##################################################################################################
    public void paintBall(Paint paint, double x, double y, double z, double r, boolean showHighlight)
    {
        int d = (int)(2.0*r + 0.5);
        if(d < 2) d = 2; // make sure balls don't disappear
        
        // one disk
        g.setPaint(paint);
        if(REALLY_PAINT) g.fillOval((int)(x-r), (int)(y-r), d, d);

        // Black rim, causes ~10% speed penalty but useful for visualization!
        try
        {
            Color c = (Color) paint;
            if(d >= 10 && c.getAlpha() == 255)
            {
                g.setPaint(KPaint.black); // wants to merge with the background
                if(REALLY_PAINT) g.drawOval((int)(x-r), (int)(y-r), d, d);
            }
        }
        catch(ClassCastException ex)
        {
            ex.printStackTrace();
            System.err.println("StandardPainter: tried painting with non-Color type of Paint");
        }
        
        // highlight
        if(showHighlight)
        {
            double off = 0.5 * r;
            d = (int)(0.3*r)+1;
            g.setPaint(Color.white); // wants to be bright white no matter what
            if(REALLY_PAINT) g.fillOval((int)(x-off), (int)(y-off), d, d);
        }
    }
//}}}

//{{{ paintDot
//##################################################################################################
    public void paintDot(Paint paint, double x, double y, double z, int width)
    {
        int off = width/2;
        g.setPaint(paint);
        if(REALLY_PAINT) g.fillRect((int)(x-off), (int)(y-off), width, width);
    }
//}}}

//{{{ paintLabel
//##################################################################################################
    public void paintLabel(Paint paint, String label, double x, double y, double z)
    {
        g.setPaint(paint);
        g.setFont(font);
        if(REALLY_PAINT) g.drawString(label, (int)x, (int)y);
    }
//}}}

//{{{ paintMarker
//##################################################################################################
    public void paintMarker(Paint paint, double x, double y, double z, int width, int paintStyle)
    {
        int cx = (int)x, cy = (int)y;
        int one = width, two = 2*width, three = 3*width, four = 4*width, five = 5*width,
            six = 6*width, seven = 7*width, ten = 10*width, eleven = 11*width;
        g.setPaint(paint);
        
        if(REALLY_PAINT)
        {
            // Large discs and boxes
            if((paintStyle & MarkerPoint.BOX_L) != 0) g.fillRect(cx-five, cy-five, eleven, eleven);
            else if((paintStyle & MarkerPoint.DISC_L) != 0) g.fillOval(cx-five, cy-five, eleven, eleven);
            // Medium discs and boxes
            if((paintStyle & MarkerPoint.BOX_M) != 0) g.fillRect(cx-three, cy-three, seven, seven);
            else if((paintStyle & MarkerPoint.DISC_M) != 0) g.fillOval(cx-three, cy-three, seven, seven);
            // Small discs and boxes
            if((paintStyle & MarkerPoint.BOX_S) != 0) g.fillRect(cx-one, cy-one, three, three);
            else if((paintStyle & MarkerPoint.DISC_S) != 0) g.fillOval(cx-one, cy-one, three, three);
            // Crosses
            if((paintStyle & MarkerPoint.CROSS_S) != 0) { g.drawLine(cx, cy-one, cx, cy+one); g.drawLine(cx-one, cy, cx+one, cy); }
            if((paintStyle & MarkerPoint.CROSS_M) != 0) { g.drawLine(cx, cy-three, cx, cy+three); g.drawLine(cx-three, cy, cx+three, cy); }
            if((paintStyle & MarkerPoint.CROSS_L) != 0) { g.drawLine(cx, cy-five, cx, cy+five); g.drawLine(cx-five, cy, cx+five, cy); }
            if((paintStyle & MarkerPoint.CROSS_2) != 0)
            {
                g.drawLine(cx-one, cy-five, cx-one, cy+five); g.drawLine(cx+one, cy-five, cx+one, cy+five);
                g.drawLine(cx-five, cy-one, cx+five, cy-one); g.drawLine(cx-five, cy+one, cx+five, cy+one);
            }
            // X's
            if((paintStyle & MarkerPoint.X_S) != 0) { g.drawLine(cx-one, cy-one, cx+one, cy+one); g.drawLine(cx-one, cy+one, cx+one, cy-one); }
            if((paintStyle & MarkerPoint.X_M) != 0) { g.drawLine(cx-three, cy-three, cx+three, cy+three); g.drawLine(cx-three, cy+three, cx+three, cy-three); }
            if((paintStyle & MarkerPoint.X_L) != 0) { g.drawLine(cx-five, cy-five, cx+five, cy+five); g.drawLine(cx-five, cy+five, cx+five, cy-five); }
            if((paintStyle & MarkerPoint.X_2) != 0)
            {
                g.drawLine(cx-four, cy-five, cx+five, cy+four); g.drawLine(cx-five, cy-four, cx+four, cy+five);
                g.drawLine(cx-four, cy+five, cx+five, cy-four); g.drawLine(cx-five, cy+four, cx+four, cy-five);
            }
            // Squares
            if((paintStyle & MarkerPoint.SQUARE_S) != 0) g.drawRect(cx-one, cy-one, two, two);
            if((paintStyle & MarkerPoint.SQUARE_M) != 0) g.drawRect(cx-three, cy-three, six, six);
            if((paintStyle & MarkerPoint.SQUARE_L) != 0) g.drawRect(cx-five, cy-five, ten, ten);
            // Circles
            if((paintStyle & MarkerPoint.RING_S) != 0) g.drawOval(cx-one, cy-one, two, two);
            if((paintStyle & MarkerPoint.RING_M) != 0) g.drawOval(cx-three, cy-three, six, six);
            if((paintStyle & MarkerPoint.RING_L) != 0) g.drawOval(cx-five, cy-five, ten, ten);
        }
    }
//}}}

//{{{ paintSphereDisk
//##################################################################################################
    public void paintSphereDisk(Paint paint, double x, double y, double z, double r)
    {
        int d = (int)(2.0*r + 0.5);
        if(d < 2) d = 2; // make sure balls don't disappear
        
        // one disk
        g.setPaint(paint);
        if(REALLY_PAINT) g.fillOval((int)(x-r), (int)(y-r), d, d);
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
        xPoints[0] = (int)x1;           yPoints[0] = (int)y1;
        xPoints[1] = (int)x2;           yPoints[1] = (int)y2;
        xPoints[2] = (int)x3;           yPoints[2] = (int)y3;
        if(REALLY_PAINT) g.fillPolygon(xPoints, yPoints, 3);
    }
//}}}

//{{{ paintVector
//##################################################################################################
    public void paintVector(Paint paint, int width, int widthCue,
        double x1, double y1, double z1,
        double x2, double y2, double z2)
    {
        g.setPaint(paint);
        if(REALLY_PAINT) prettyLine((int)x1, (int)y1, (int)x2, (int)y2, KPalette.lineWidths[width-1][widthCue]);
    }
//}}}

//{{{ prettyLine
//##################################################################################################
    /** Draws a thick line with nice ends, using fillPolygon(). Slightly slower (30-35%) than fastLine(). */
    void prettyLine(int x1, int y1, int x2, int y2, int width)
    {
        int s, e, abs_x2_x1, abs_y2_y1;
        s = -width / 2; // Start offset
        e = s + width;  // End offset
        abs_x2_x1 = Math.abs(x2 - x1);
        abs_y2_y1 = Math.abs(y2 - y1);
        
        // horizontal --
        if( abs_x2_x1 > abs_y2_y1 )
        {
            // left to right
            if( x1 < x2 )
            {
                xPoints[0] = x1  ; xPoints[1] = x1+s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2-s; xPoints[5] = x2;
                yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
            }
            // right to left
            else
            {
                xPoints[0] = x1  ; xPoints[1] = x1-s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2+s; xPoints[5] = x2;
                yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
            }
        }
        // vertical |
        else
        {
            // top to bottom
            if( y1 < y2 )
            {
                xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                yPoints[0] = y1  ; yPoints[1] = y1+s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2-s; yPoints[5] = y2;
            }
            // bottom to top
            else
            {
                xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                yPoints[0] = y1  ; yPoints[1] = y1-s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2+s; yPoints[5] = y2;
            }
        }
        
        g.fillPolygon(xPoints, yPoints, 6);
    }
//}}}

//{{{ fastLine
//##################################################################################################
    /** Draws a thick line using multiple calls to drawLine(). Not as robust as prettyLine(), but slightly faster. */
    void fastLine(int x1, int y1, int x2, int y2, int width)
    {
        g.drawLine(x1, y1, x2, y2);
        
        // Then, draw more lines until we get to the approximate thickness.
        // This idea is borrowed from JavaMage, and possibly regular Mage, too.
        // The plan is to step along x if it's a mostly vertical line, along y if it's mostly horizontal
        int start, end, i;
        start = -width/2;
        end = start + width;
        
        // step along y
        if( Math.abs(x2-x1) > Math.abs(y2-y1) )
        {
            for(i = start; i < 0; i++) g.drawLine(x1, y1+i, x2, y2+i); 
            for(i = 1; i < end; i++)   g.drawLine(x1, y1+i, x2, y2+i); 
        }
        // step along x
        else
        {
            for(i = start; i < 0; i++) g.drawLine(x1+i, y1, x2+i, y2); 
            for(i = 1; i < end; i++)   g.drawLine(x1+i, y1, x2+i, y2); 
        }
    }
//}}}

//{{{ drawOval
//##################################################################################################
    public void drawOval(Paint paint, double x, double y, double z, double width, double height)
    {
        // one disk
        g.setPaint(paint);
        if(REALLY_PAINT) g.drawOval((int)(x - width/2), (int)(y - height/2), (int)width, (int)height);
    }

    public void drawOval(Paint paint, int linewidth, int widthCue, double x, double y, double z, double width, double height)
    {
        g.setPaint(paint);
        int startx = (int)(x -  width/2.0 - linewidth/2.0);
        int starty = (int)(y - height/2.0 - linewidth/2.0);
        int diamx  = (int)( width + linewidth);
        int diamy  = (int)(height + linewidth);
        if(REALLY_PAINT)
        {
            for(int i = 0; i < linewidth; i++)
            {
                g.drawOval(startx, starty, diamx, diamy);
                startx += 1;
                starty += 1;
                diamx  -= 2;
                diamy  -= 2;
            }
        }
    }
//}}}

//{{{ setGraphics, setFont, getLabelWidth/Ascent/Descent
//##############################################################################
    /** Sets the Graphics object this painter will use for painting. */
    public void setGraphics(Graphics2D g2)
    {
        this.g = g2;
        if(forceAA)
            this.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if(font != null)
            this.metrics = g.getFontMetrics(font);
    }
    
    public void setFont(Font f)
    {
        this.font = f;
        if(g != null)
            this.metrics = g.getFontMetrics(f);
    }
    
    public int getLabelWidth(String s)
    { return metrics.stringWidth(s); }
    public int getLabelAscent(String s)
    { return metrics.getAscent(); }
    public int getLabelDescent(String s)
    { return metrics.getDescent(); }
//}}}

//{{{ setViewport, clearCanvas
//##############################################################################
    public void setViewport(int x, int y, int width, int height)
    {
        clipRgn.setBounds(x, y, width, height);
        g.setClip(clipRgn);
    }
    
    public void clearCanvas(Color color)
    {
        g.setColor(color);
        if(REALLY_PAINT) g.fillRect(clipRgn.x, clipRgn.y, clipRgn.width, clipRgn.height);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

