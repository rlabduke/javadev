// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
//import java.io.*;
//import java.text.*;
//import java.util.*;
//import javax.swing.*;
//}}}
/**
* <code>MarkerPoint</code> is a "screen-oriented displayable" that can take on many different looks.
* It servers as a marker for picked points and may come in list form, e.g. for graphs.
*
* <p>Begun on Sun Jun 23 15:33:28 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class MarkerPoint extends KPoint // implements ...
{
//{{{ Static fields
    public static final int CROSS_S  = 0x00000001;
    public static final int CROSS_M  = 0x00000002;
    public static final int CROSS_L  = 0x00000004;
    public static final int CROSS_2  = 0x00000008;
    
    public static final int X_S      = 0x00000010;
    public static final int X_M      = 0x00000020;
    public static final int X_L      = 0x00000040;
    public static final int X_2      = 0x00000080;
    
    public static final int SQUARE_S = 0x00000100;
    public static final int SQUARE_M = 0x00000200;
    public static final int SQUARE_L = 0x00000400;
    
    public static final int BOX_S    = 0x00001000;
    public static final int BOX_M    = 0x00002000;
    public static final int BOX_L    = 0x00004000;
    
    public static final int RING_S   = 0x00010000;
    public static final int RING_M   = 0x00020000;
    public static final int RING_L   = 0x00040000;

    public static final int DISC_S   = 0x00100000;
    public static final int DISC_M   = 0x00200000;
    public static final int DISC_L   = 0x00400000;
//}}}

//{{{ Variable definitions
//##################################################################################################
    int style = 0;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new data point displayed as some type of marker.
    *
    * @param list the list that contains this point
    * @param label the pointID of this point
    */
    public MarkerPoint(KList list, String label)
    {
        super(list, label);
    }
    
    /** Quick way to follow a point -- creates an unpickable marker named "marker" */
    public MarkerPoint(KPoint p, KPaint color, int style_mask)
    {
        super(null, "marker");
        setColor(color);
        setStyle(style_mask);
        setUnpickable(true);
        x0 = p.x0;
        y0 = p.y0;
        z0 = p.z0;
    }
//}}}

//{{{ get/setStyle()
//##################################################################################################
    /** Retrieves the style code for this marker */
    public int getStyle() { return style; }
    /** Sets the style for this marker. Use a Boolean OR of fields defined in this class. */
    public void setStyle(int s) { style = s; }
//}}}

//{{{ paintStandard
//##################################################################################################
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paintStandard(Graphics2D g, Engine engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
        Paint[] colors = maincolor.getPaints(engine.backgroundMode);
        
        int cx = (int)x, cy = (int)y;
        int width = engine.markerSize;
        int one = width, two = 2*width, three = 3*width, four = 4*width, five = 5*width,
            six = 6*width, seven = 7*width, ten = 10*width, eleven = 11*width;
        
        // Point style dominants over list style
        int paintStyle = 0;
        if(this.getStyle() != 0)                            paintStyle = this.getStyle();
        else if(parent != null && parent.getStyle() != 0)   paintStyle = parent.getStyle();
        
        // Large discs and boxes
        g.setPaint( colors[(engine.colorCue>1?engine.colorCue-2:0)] );
        if((paintStyle & BOX_L) != 0) g.fillRect(cx-five, cy-five, eleven, eleven);
        else if((paintStyle & DISC_L) != 0) g.fillOval(cx-five, cy-five, eleven, eleven);

        // Medium discs and boxes
        g.setPaint( colors[(engine.colorCue>0?engine.colorCue-1:0)] );
        if((paintStyle & BOX_M) != 0) g.fillRect(cx-three, cy-three, seven, seven);
        else if((paintStyle & DISC_M) != 0) g.fillOval(cx-three, cy-three, seven, seven);

        // Everything else
        //g.setPaint( colors[2] );
        g.setPaint( colors[engine.colorCue] );
        // Small discs and boxes
        if((paintStyle & BOX_S) != 0) g.fillRect(cx-one, cy-one, three, three);
        else if((paintStyle & DISC_S) != 0) g.fillOval(cx-one, cy-one, three, three);
        // Crosses
        if((paintStyle & CROSS_S) != 0) { g.drawLine(cx, cy-one, cx, cy+one); g.drawLine(cx-one, cy, cx+one, cy); }
        if((paintStyle & CROSS_M) != 0) { g.drawLine(cx, cy-three, cx, cy+three); g.drawLine(cx-three, cy, cx+three, cy); }
        if((paintStyle & CROSS_L) != 0) { g.drawLine(cx, cy-five, cx, cy+five); g.drawLine(cx-five, cy, cx+five, cy); }
        if((paintStyle & CROSS_2) != 0)
        {
            g.drawLine(cx-one, cy-five, cx-one, cy+five); g.drawLine(cx+one, cy-five, cx+one, cy+five);
            g.drawLine(cx-five, cy-one, cx+five, cy-one); g.drawLine(cx-five, cy+one, cx+five, cy+one);
        }
        // X's
        if((paintStyle & X_S) != 0) { g.drawLine(cx-one, cy-one, cx+one, cy+one); g.drawLine(cx-one, cy+one, cx+one, cy-one); }
        if((paintStyle & X_M) != 0) { g.drawLine(cx-three, cy-three, cx+three, cy+three); g.drawLine(cx-three, cy+three, cx+three, cy-three); }
        if((paintStyle & X_L) != 0) { g.drawLine(cx-five, cy-five, cx+five, cy+five); g.drawLine(cx-five, cy+five, cx+five, cy-five); }
        if((paintStyle & X_2) != 0)
        {
            g.drawLine(cx-four, cy-five, cx+five, cy+four); g.drawLine(cx-five, cy-four, cx+four, cy+five);
            g.drawLine(cx-four, cy+five, cx+five, cy-four); g.drawLine(cx-five, cy+four, cx+four, cy-five);
        }
        // Squares
        if((paintStyle & SQUARE_S) != 0) g.drawRect(cx-one, cy-one, two, two);
        if((paintStyle & SQUARE_M) != 0) g.drawRect(cx-three, cy-three, six, six);
        if((paintStyle & SQUARE_L) != 0) g.drawRect(cx-five, cy-five, ten, ten);
        // Circles
        if((paintStyle & RING_S) != 0) g.drawOval(cx-one, cy-one, two, two);
        if((paintStyle & RING_M) != 0) g.drawOval(cx-three, cy-three, six, six);
        if((paintStyle & RING_L) != 0) g.drawOval(cx-five, cy-five, ten, ten);
    }
//}}}
}//class
