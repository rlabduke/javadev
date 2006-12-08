// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

import java.awt.*;
import java.awt.geom.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>DotPoint</code> represents a contact dot or other small point.
* These dots are not depth-cued by size, though the size can be set at time of creation.
* "Dots" are actually square, so a radius between 1 and 3 is probably desireable.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 12:57:57 EDT 2002
*/
public class DotPoint extends AbstractPoint // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new data point representing a "dot".
    *
    * @param label the pointID of this point
    */
    public DotPoint(String label)
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
        
        int width = calcLineWidth(engine);
        engine.painter.paintDot(paint, x, y, z, width);
    }
//}}}

//{{{ calcLineWidth
//##################################################################################################
    // Default way of finding the right line width to use, given the settings in the engine
    // Dots never do depth cueing by width, because big square dots look crappy.
    protected int calcLineWidth(Engine engine)
    {
        if(engine.thinLines)    return 1;
        else if(parent != null) return parent.getWidth();
        else                    return 2;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

