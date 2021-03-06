// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.edmap;
import king.*;
import king.core.*;
import king.points.*;

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
* <code>EDMapPlotter</code> creates an isosurface
* by instantiating VectorPoints directly.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar  4 14:35:25 EST 2003
*/
public class EDMapPlotter implements EdgePlotter
{
//{{{ Constants
    DecimalFormat df2 = driftwood.util.Strings.usDecimalFormat("0.##");
//}}}

//{{{ Variable definitions
//##################################################################################################
    KList           list;
    VectorPoint     prevV;
    TrianglePoint   prevT;
    String          level;
    Object          mode;
    boolean         unpickable;
    float           alphavalue;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @param mode is one of the MarchingCubes.MODE_xxx constants.
    */
    public EDMapPlotter(boolean pickable, Object mode)
    {
        list    = null;
        prevV   = null;
        prevT   = null;
        level   = null;
        unpickable = !pickable;
        alphavalue = (float)0.25;
        
        if(mode != MarchingCubes.MODE_MESH && mode != MarchingCubes.MODE_TRIANGLE)
            throw new IllegalArgumentException("Illegal MarchingCubes MODE constant: "+mode);
        
        this.mode = mode;
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
        list = new KList((mode == MarchingCubes.MODE_MESH ? KList.VECTOR : KList.TRIANGLE));
        list.setName("ED map @ "+level);
        list.setWidth(1);
        if(mode == MarchingCubes.MODE_TRIANGLE)
            list.setAlpha((int)(alphavalue * 255));
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
        prevV = null;
        prevT = null;
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
        KPoint p;
        if(mode == MarchingCubes.MODE_MESH)
        {
            if(lineto)  p = new VectorPoint(level, prevV);
            else        p = new VectorPoint(level, null);
            prevV = (VectorPoint)p;
        }
        else//mode == MarchingCubes.MODE_TRIANGLE
        {
            if(lineto)  p = new TrianglePoint(level, prevT);
            else        p = new TrianglePoint(level, null);
            prevT = (TrianglePoint)p;
        }
        
        p.setX(x);
        p.setY(y);
        p.setZ(z);
        p.setUnpickable(unpickable);
        
        list.add(p);
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

//{{{ get, setAlphaValue
public float getAlphaValue() {
  return alphavalue;
}

public void setAlphaValue(float val) {
  alphavalue = val;
}
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

