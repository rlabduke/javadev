// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

import java.awt.Paint;
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
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class MarkerPoint extends AbstractPoint // implements ...
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
    * @param label the pointID of this point
    */
    public MarkerPoint(String label)
    {
        super(label);
    }
    
    /** Quick way to follow a point -- creates an unpickable marker named "marker" */
    public MarkerPoint(KPoint p, KPaint color, int style_mask)
    {
        super("marker");
        setColor(color);
        setStyle(style_mask);
        setUnpickable(true);
        x0 = (float)p.getX();
        y0 = (float)p.getY();
        z0 = (float)p.getZ();
    }
//}}}

//{{{ get/setStyle
//##################################################################################################
    public int getStyle() { return style; }
    public void setStyle(int s) { style = s; fireKinChanged(CHANGE_POINT_PROPERTIES); }
//}}}

//{{{ paint2D
//##################################################################################################
    public void paint2D(Engine2D engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
        Paint paint = maincolor.getPaint(engine.backgroundMode, engine.colorCue);
        int width = engine.markerSize;
        // Point style dominants over list style
        int paintStyle = this.getStyle();
        if(paintStyle == 0 && parent != null) paintStyle = parent.getStyle();
        
        engine.painter.paintMarker(paint, x, y, z, width, paintStyle);
    }
//}}}
}//class
