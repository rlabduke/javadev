// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.isosurface;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>LowResolutionVertexSource</code> decreases the number of (visible) sample
* points in an electron density map by half, one third, etc. along each dimension.
* This is a crude way to make a coarser mesh.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Apr 11 13:51:35 EDT 2005
*/
public class LowResolutionVertexSource implements VertexEvaluator, VertexLocator
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    final CrystalVertexSource map;
    final int divisor;
    final double[] cell = new double[3];
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Create a new, lower-resolution vertex source from an existing one.
    * @param divisor    the sampling interval to use along each dimension,
    *   typically 2 or 3.
    */
    public LowResolutionVertexSource(CrystalVertexSource map, int divisor)
    {
        super();
        this.map = map;
        this.divisor = divisor;
    }
//}}}

//{{{ evaluateVertex
//##################################################################################################
    /**
    * Returns a scalar value <i>v</i> for legal vertex. If the indices are
    * meaningful but no value is available, this function should return
    * NaN rather than throwing an exception. An IndexOutOfBoundsException
    * should be thrown in cases where the indices are not meaningful, which
    * usually corresponds to conditions where VertexLocator.locateVertex()
    * would also throw an exception.
    * @param i the x index of the vertex to be evaluated
    * @param j the y index of the vertex to be evaluated
    * @param k the z index of the vertex to be evaluated
    * @return the value at the specified vertex,
    *   or NaN if the value can't be calculated for a legal vertex
    * @throws IndexOutOfBoundsException if no such vertex exists
    */
    public double evaluateVertex(int i, int j, int k)
    {
        return map.evaluateVertex(i*divisor, j*divisor, k*divisor);
    }
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
    public void locateVertex(int i, int j, int k, double[] xyz)
    {
        map.locateVertex(i*divisor, j*divisor, k*divisor, xyz);
    }
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
    public void findVertexForPoint(double x, double y, double z, int[] ijk)
    {
        // Load cell with x,y,z expressed in unit cell coordinates
        map.findVertexForPoint(x, y, z, this.cell);
        // Convert to nearest whole number after dividing to coarser grid
        ijk[0] = (int) Math.round(cell[0] / divisor);
        ijk[1] = (int) Math.round(cell[1] / divisor);
        ijk[2] = (int) Math.round(cell[2] / divisor);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

