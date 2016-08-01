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
* A <code>VertexEvaluator</code> is responsible for delivering the scalar value <i>v</i>
* for any legal vertex <i>&lt;i, j, k&gt;</i>.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Feb  9 17:19:20 EST 2003
*/
public interface VertexEvaluator //extends ... implements ...
{
//{{{ Constants
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
    public double evaluateVertex(int i, int j, int k);
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

