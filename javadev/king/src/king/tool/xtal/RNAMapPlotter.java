// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.xtal;
import king.*;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.isosurface.*;
//}}}
/**
* <code>RNAMapPlotter</code> creates an isosurface
* by instantiating VectorPoints directly.
*
* Attempting to use EDMap programs as a starting point for RNA ED
* analysis. VBC 27 Oct 2003.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar  4 14:35:25 EST 2003
*/
public class RNAMapPlotter implements EdgePlotter
{
//{{{ Constants
    DecimalFormat df2 = new DecimalFormat("0.00");
//}}}

//{{{ Variable definitions
//##################################################################################################
    KList       list;
    VectorPoint prev;
    String      level;
    boolean     unpickable;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public RNAMapPlotter(boolean pickable)
    {
        list    = null;
        prev    = null;
        level   = null;
        unpickable = !pickable;
    }
//}}}

//{{{ startIsosurface
//##################################################################################################
    /**
    * Called before the isosurface generator starts plotting anything.
    * Gives this plotter a chance to initialize any needed data structures, streams, etc.
    * @param lvl the level for which an isosurface will be generated
    */
    public void startIsosurface(double lvl)
    {
        level = df2.format(lvl);
        list = new KList();
        list.setName("RNA map @ "+level);
        list.setType(KList.VECTOR);
        list.setWidth(2);
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
        prev = null;
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
        VectorPoint p;
	//lineto = true;
        if(lineto)  p = new VectorPoint(list, level, prev);
        else        p = new VectorPoint(list, level, null);
        
        p.setOrigX(x);
        p.setOrigY(y);
        p.setOrigZ(z);
	
        p.setName("x = "+ df2.format(x) + " y = " + df2.format(y) + " z = " + df2.format(z));
        
        list.add(p);
        prev = p;
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
    }
//}}}

//{{{ getList, freeList
//##################################################################################################
    /** Releases the last surface generated */
    public void freeList()
    { list = null; }
    
    /** Retrieves the last surface generated (could be null)*/
    public KList getList()
    { return list; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

