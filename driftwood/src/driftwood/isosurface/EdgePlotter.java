// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.isosurface;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* An <code>EdgePlotter</code> receives a series of points in 3-space from
* an isosurface generating algorithm and outputs in an isosurface in some graphics format.
*
* <p>In the case of the wireframe mesh mode, the points constitute a polyline,
* and the lineto flag indicates breaks in the polyline.
* The first point in any polyline does not result in any drawing;
* it's just an anchor referenced by the second point.
*
* <p>In the case of the triangle strip surface mode, the points constitute a strip of triangles,
* and the lineto flag indicates the end of the old strip and the beginning of a new one.
* In a triangle strip, a triangle is drawn among the first three points;
* then points 2, 3, and 4; then points 3, 4, and 5; and so on.
* The first two points in any strip therefore do not result in any drawing;
* they're just anchors referenced by the third and fourth points.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Feb  9 19:38:16 EST 2003
*
* @see MarchingCubes
*/
public interface EdgePlotter //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ startIsosurface
//##################################################################################################
    /**
    * Called before the isosurface generator starts plotting anything.
    * Gives this plotter a chance to initialize any needed data structures, streams, etc.
    * @param level the level for which an isosurface will be generated
    */
    public void startIsosurface(double level);
//}}}

//{{{ startCell
//##################################################################################################
    /**
    * Called before the isosurface generator starts each cell.
    * @param i the minimum x index of the current cell
    * @param j the minimum y index of the current cell
    * @param k the minimum z index of the current cell
    */
    public void startCell(int i, int j, int k);
//}}}

//{{{ plotEdge
//##################################################################################################
    /**
    * Called for each edge in the isosurface mesh.
    * @param x      the x coordinate of the current point
    * @param y      the y coordinate of the current point
    * @param z      the z coordinate of the current point
    * @param lineto if true, a line should be drawn from the last point to this one.
    *               If false, the pen should move to this point without drawing.
    *               Guaranteed to be true for the first point in a new cell.
    */
    public void plotEdge(double x, double y, double z, boolean lineto);
//}}}

//{{{ endCell
//##################################################################################################
    /**
    * Called after the isosurface generator finishes each cell.
    * @param i the minimum x index of the current cell
    * @param j the minimum y index of the current cell
    * @param k the minimum z index of the current cell
    */
    public void endCell(int i, int j, int k);
//}}}

//{{{ endIsosurface
//##################################################################################################
    /**
    * Called after the isosurface generator finishes plotting everything.
    * Gives this plotter a chance to release memory, close streams, etc.
    * @param level the level for which an isosurface will be generated
    */
    public void endIsosurface(double level);
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

