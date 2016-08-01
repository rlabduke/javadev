// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package boundrotamers;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;

import util.isosurface.*;
//}}}
/**
* <code>NdftVertexSource</code> acts as a VertexLocator and a
* VertexEvaluator for an NDFloat Table.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb 10 10:13:29 EST 2003
*/
public class NdftVertexSource implements VertexLocator, VertexEvaluator
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    NDFloatTable ndft;
    
    int[] bin = {0,0,0};
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public NdftVertexSource(NDFloatTable ndft)
    {
        if(ndft.getDimensions() != 3) throw new IllegalArgumentException("NDFT must have exactly 3 dimensions");
        this.ndft = ndft;
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
        bin[0] = i;
        bin[1] = j;
        bin[2] = k;
        float[] ctr = ndft.centerOf(bin);
        
        xyz[0] = ctr[0];
        xyz[1] = ctr[1];
        xyz[2] = ctr[2];
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
        throw new UnsupportedOperationException("findVertexForPoint() not supported by this implementation");
    }
//}}}

//{{{ evaluateVertex
//##################################################################################################
    /**
    * Returns a scalar value <i>v</i> for legal vertex.
    * @param i the x index of the vertex to be evaluated
    * @param j the y index of the vertex to be evaluated
    * @param k the z index of the vertex to be evaluated
    * @return the value at the specified vertex
    * @throws IndexOutOfBoundsException if no such vertex exists
    */
    public double evaluateVertex(int i, int j, int k)
    {
        bin[0] = i;
        bin[1] = j;
        bin[2] = k;
        return ndft.valueAt(bin);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

