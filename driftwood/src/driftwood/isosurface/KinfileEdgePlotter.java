// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.isosurface;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>KinfileEdgePlotter</code> writes mesh isosurface edges to
* a text-format kinemage file.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb 10 09:56:18 EST 2003
*/
public class KinfileEdgePlotter implements EdgePlotter
{
//{{{ Constants
    static DecimalFormat df4    = driftwood.util.Strings.usDecimalFormat("0.####");
    static DecimalFormat dfe4   = driftwood.util.Strings.usDecimalFormat("0.####E0");
//}}}

//{{{ Variable definitions
//##################################################################################################
    PrintStream out;
    
    // Attributes that can be get/set:
    String  color   = "cyan";
    int     width   = 1;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public KinfileEdgePlotter(OutputStream os)
    {
        out = new PrintStream(os);
    }
//}}}

//{{{ startIsosurface
//##################################################################################################
    /**
    * Called before the isosurface generator starts plotting anything.
    * Gives this plotter a chance to initialize any needed data structures, streams, etc.
    * @param level the level for which an isosurface will be generated
    */
    public void startIsosurface(double level)
    {
        out.println("@group {mesh @ "+format(level)+"} dominant");
        out.println("@vectorlist {x} color= "+color+" width= "+width);
    }
//}}}

//{{{ startCell
//##################################################################################################
    /**
    * Called before the isosurface generator starts each cell.
    * @param i the minimum x index of the current cell
    * @param j the minimum y index of the current cell
    * @param k the minimum z index of the current cell
    */
    public void startCell(int i, int j, int k)
    {
    }
//}}}

//{{{ plotEdge
//##################################################################################################
    /**
    * Called for each edge in the isosurface mesh.
    * @param x      the x coordinate of the current point
    * @param y      the y coordinate of the current point
    * @param z      the z coordinate of the current point
    * @param lineto if true, a line should be drawn from the last point to this one.
    *               if false, the pen should move to this point without drawing.
    */
    public void plotEdge(double x, double y, double z, boolean lineto)
    {
        if(lineto) out.println("{x} " +format(x)+" "+format(y)+" "+format(z));
        else       out.println("{x}P "+format(x)+" "+format(y)+" "+format(z));
    }
//}}}

//{{{ endCell
//##################################################################################################
    /**
    * Called after the isosurface generator finishes each cell.
    * @param i the minimum x index of the current cell
    * @param j the minimum y index of the current cell
    * @param k the minimum z index of the current cell
    */
    public void endCell(int i, int j, int k)
    {
    }
//}}}

//{{{ endIsosurface
//##################################################################################################
    /**
    * Called after the isosurface generator finishes plotting everything.
    * Gives this plotter a chance to release memory, close streams, etc.
    * @param level the level for which an isosurface will be generated
    */
    public void endIsosurface(double level)
    {
        out.flush();
    }
//}}}

//{{{ format
//##################################################################################################
    String format(double d)
    {
        return df4.format(d);
        //if(d < 1.0) return dfe4.format(d);
        //else        return df4.format(d);
    }
//}}}

//{{{ get/setColor, get/setWidth
//##################################################################################################
    public String getColor()
    { return color; }
    public void setColor(String color)
    { this.color = color; }
    
    public int getWidth()
    { return width; }
    public void setWidth(int w)
    {
        if(w < 1)       w = 1;
        else if(w > 7)  w = 7;
        width = w;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

