// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;
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
//}}}
/**
* <code>MovePointTool</code> allows the user to click &amp; drag
* to move points around.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Feb 28 15:17:36 EST 2003
*/
public class MovePointTool extends BasicTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KPoint          draggedPoint = null;
    KPoint[]        allPoints = null;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public MovePointTool(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ xx_drag() functions
//##################################################################################################
    /** Override this function for (left-button) drags */
    public void drag(int dx, int dy, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(v != null && allPoints != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            for(int k = 0; k < allPoints.length; k++)
            {
                float[] offset = v.translateRotated(dx, -dy, 0, Math.min(dim.width, dim.height));
                
                // Check to make sure this isn't just a SpherePoint disk:
                if(allPoints[k] instanceof ProxyPoint) continue;
                
                allPoints[k].setOrigX(allPoints[k].getOrigX() + offset[0]);
                allPoints[k].setOrigY(allPoints[k].getOrigY() + offset[1]);
                allPoints[k].setOrigZ(allPoints[k].getOrigZ() + offset[2]);
            }

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
        else super.drag(dx, dy, ev);
    }

    /** Override this function for middle-button/control drags */
    public void c_drag(int dx, int dy, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(v != null && draggedPoint != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            float[] offset = v.translateRotated(dx, -dy, 0, Math.min(dim.width, dim.height));
            draggedPoint.setOrigX(draggedPoint.getOrigX() + offset[0]);
            draggedPoint.setOrigY(draggedPoint.getOrigY() + offset[1]);
            draggedPoint.setOrigZ(draggedPoint.getOrigZ() + offset[2]);

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
        else super.c_drag(dx, dy, ev);
    }
//}}}

//{{{ xx_wheel() functions
//##################################################################################################
    /** Override this function for mouse wheel motion */
    public void wheel(int rotation, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(v != null && draggedPoint != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            for(int k = 0; k < allPoints.length; k++)
            {
                float[] offset = v.translateRotated(0, 0, 6*rotation, Math.min(dim.width, dim.height));
                allPoints[k].setOrigX(allPoints[k].getOrigX() + offset[0]);
                allPoints[k].setOrigY(allPoints[k].getOrigY() + offset[1]);
                allPoints[k].setOrigZ(allPoints[k].getOrigZ() + offset[2]);
            }

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
    }

    /** Override this function for mouse wheel motion with control down */
    public void c_wheel(int rotation, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(v != null && draggedPoint != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            float[] offset = v.translateRotated(0, 0, 6*rotation, Math.min(dim.width, dim.height));
            draggedPoint.setOrigX(draggedPoint.getOrigX() + offset[0]);
            draggedPoint.setOrigY(draggedPoint.getOrigY() + offset[1]);
            draggedPoint.setOrigZ(draggedPoint.getOrigZ() + offset[2]);

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
    }
//}}}

//{{{ Mouse click listners
//##################################################################################################
    public void mousePressed(MouseEvent ev)
    {
        super.mousePressed(ev);
        if(kMain.getKinemage() != null)
            draggedPoint = kCanvas.getEngine().pickPoint(ev.getX(), ev.getY(), services.doSuperpick.isSelected());
        else draggedPoint = null;
        // Otherwise, we just create a nonsensical warning message about stereo picking
        
        if(draggedPoint == null)
            allPoints = null;
        else
        {
            // The 0.5 allows for a little roundoff error,
            // both in the kinemage itself and our floating point numbers.
            Collection all = kCanvas.getEngine().pickAll3D(
                draggedPoint.getDrawX(), draggedPoint.getDrawY(), draggedPoint.getDrawZ(),
                services.doSuperpick.isSelected(), 0.5);
            allPoints = (KPoint[])all.toArray( new KPoint[all.size()] );
        }
    }
    
    public void mouseReleased(MouseEvent ev)
    {
        // Let's keep the point around so we can Z-translate too
        //draggedPoint = null;
    }
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#movept-tool"; }
    
    public String toString() { return "Move points"; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

