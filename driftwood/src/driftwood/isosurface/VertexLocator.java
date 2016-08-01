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
* A <code>VertexLocator</code> is responsible for translating the indices of a vertex,
* <i>&lt;i, j, k&gt;</i>, to a location in 3-space, <i>&lt;x, y, z&gt;</i>.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Feb  9 17:19:20 EST 2003
*/
public interface VertexLocator //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ locateVertex
//##################################################################################################
    /**
    * Translates the indices of a vertex, <i>&lt;i, j, k&gt;</i>,
    * to a location in 3-space, <i>&lt;x, y, z&gt;</i>.
    * @param i      the x index of the vertex to be found
    * @param j      the y index of the vertex to be found
    * @param k      the z index of the vertex to be found
    * @param xyz    a double[3] to be filled with the coordinates of the vertex
    * @throws IndexOutOfBoundsException if no such vertex exists
    */
    public void locateVertex(int i, int j, int k, double[] xyz);
//}}}

//{{{ findVertexForPoint
//##################################################################################################
    /**
    * Returns the indices of the vertex "nearest" some point &lt;x,y,z&gt; (optional operation).
    * The meaning of "nearest" is up to the implementation, but typically this operation is
    * the inverse of locateVertex() for all points; i.e.,  xyz == locateVertex(findVertexForPoint(xyz)).
    * @param x      the x coordinate in the Cartesian system
    * @param y      the x coordinate in the Cartesian system
    * @param z      the x coordinate in the Cartesian system
    * @param ijk    an int[3] to be filled with the indices of the vertex
    * @throws IndexOutOfBoundsException if no suitable vertex exists
    * @throws UnsupportedOperationException if this operation is not supported by the implementor
    */
    public void findVertexForPoint(double x, double y, double z, int[] ijk);
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

