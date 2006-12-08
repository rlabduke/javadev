// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

import java.awt.*;
//import java.io.*;
//import java.text.*;
//import java.util.*;
//import javax.swing.*;
//}}}
/**
* <code>LabelPoint</code> implements a floating label at some point in space.
*
* <p>Begun on Mon Jun 24 21:09:57 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class LabelPoint extends AbstractPoint // implements ...
{
//{{{ Static fields
    public static final int LEFT   = 0;
    public static final int CENTER = 0x00800000;
    public static final int RIGHT  = 0x00400000;
    public static final int HALIGN_MASK = ~(CENTER | RIGHT); 
//}}}

//{{{ Variable definitions
//##################################################################################################
    int minx = 0, miny = 0, maxx = 0, maxy = 0;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new data point representing a label.
    *
    * @param label the pointID of this point
    */
    public LabelPoint(String label)
    {
        super(label);
    }
//}}}

//{{{ paint2D
//##################################################################################################
    public void paint2D(Engine2D engine)
    {
        KPaint maincolor = getDrawingColor(engine);
        if(maincolor.isInvisible()) return;
        Paint paint = maincolor.getPaint(engine.backgroundMode, engine.colorCue);
        
        int width, ascent, descent;
        width   = engine.painter.getLabelWidth(this.toString());
        ascent  = engine.painter.getLabelAscent(this.toString());
        descent = engine.painter.getLabelDescent(this.toString());
        
        maxy = (int)y + descent;    // screen coords: big y is down
        miny = (int)y - ascent;     // "
        
             if((multi & CENTER) != 0) { minx = (int)x - width/2; }
        else if((multi & RIGHT)  != 0) { minx = (int)x - width;   }
        else                           { minx = (int)x;           }
        maxx = minx + width;
        
        engine.painter.paintLabel(paint, this.toString(), minx, y, z);
    }
//}}}

//{{{ alignment functions
//##################################################################################################
    public void setHorizontalAlignment(int align)
    {
        multi = (multi & HALIGN_MASK) | align;
    }
//}}}

//{{{ isPickedBy
//##################################################################################################
    public KPoint isPickedBy(float xx, float yy, float radius, boolean objPick)
    {
        // Labels should always act in "object pick" mode
        //if(objPick && minx <= xx && xx <= maxx && miny <= yy && yy <= maxy)
        if(minx <= xx && xx <= maxx && miny <= yy && yy <= maxy)
            return this;
        else
            return super.isPickedBy(xx, yy, radius, objPick);        
    }
//}}}
}//class
