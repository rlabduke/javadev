// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
import driftwood.util.*;

import Jama.*;
//}}}
/**
* <code>LsqPlane</code> performs a least-squares fit of a plane
* through some number of {@link driftwood.r3.Tuple3}s.
* The resulting plane is defined in terms of an anchor point and a unit normal.
*
* <p>I would have put this in <code>driftwood.r3</code> except
* that it depends on the singular value decomposition code from Jama,
* and I don't want driftwood to have outside dependencies.
*
* <p>From Ask Dr. Math, I learned that the orthogonal least-squares regression
* plane passes through the centroid of the data (x0,y0,z0) and its normal vector
* is the singular vector (found the columns of in V) corresponding to the
* smallest singular value (in S) for the singular value decomposition (SVD) of
* matrix M, which factors M such that M = U S V' (where ' means transpose).
*
<p><pre>
M = [  x1-x0  y1-y0  z1-z0  ]
    [  x2-x0  y2-y0  z2-z0  ]
    [   ...    ...    ...   ]
    [  xN-x0  yN-y0  zN-z0  ]
</pre>
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Mar 17 15:31:07 EST 2004
*/
public class LsqPlane //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Triple anchor = new Triple();
    Triple normal = new Triple();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public LsqPlane()
    {
        super();
    }
//}}}

//{{{ getNormal, getAnchor
//##############################################################################
    /**
    * Returns the unit normal of the plane calculated during the last fitting.
    */
    public Tuple3 getNormal()
    { return normal; }
    
    /**
    * Returns the anchor point for the plane calculated during the last fitting.
    * This is equal to the centroid of all the data points used.
    */
    public Tuple3 getAnchor()
    { return anchor; }
//}}}

//{{{ fitPlane
//##############################################################################
    /**
    * Finds a least-squares plane through all the data points.
    * You can retrieve the plane definition with {@link #getNormal()}
    * and {@link #getAnchor()}.
    * @param data a Collection of Tuple3 objects
    */
    public void fitPlane(Collection data)
    {
        anchor = calcCentroid(data);
        Matrix m = buildMatrix(data, anchor);
        
        SingularValueDecomposition svd = m.svd();
        Matrix v = svd.getV(); // last column corresponds to min singular value
        
        normal = new Triple(
            v.get( 0, v.getColumnDimension()-1 ),
            v.get( 1, v.getColumnDimension()-1 ),
            v.get( 2, v.getColumnDimension()-1 )
        );
        normal.unit();
    }
//}}}

//{{{ calcCentroid, buildMatrix
//##############################################################################
    /** Calculates the centroid of a collection of Tuple3s. */
    Triple calcCentroid(Collection data)
    {
        Triple centroid = new Triple();
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            Tuple3 t = (Tuple3) iter.next();
            centroid.add(t);
        }
        centroid.mult(1.0 / data.size());
        return centroid;
    }
    
    /** Builds the matrix M */
    Matrix buildMatrix(Collection data, Triple centroid)
    {
        Matrix matrix = new Matrix(data.size(), 3);
        int i = 0;
        for(Iterator iter = data.iterator(); iter.hasNext(); i++)
        {
            Tuple3 t = (Tuple3) iter.next();
            matrix.set(i, 0, t.getX() - centroid.getX());
            matrix.set(i, 1, t.getY() - centroid.getY());
            matrix.set(i, 2, t.getZ() - centroid.getZ());
        }
        return matrix;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        ArrayList data = new ArrayList();
        LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
        String s;
        while((s = in.readLine()) != null)
        {
            try
            {
                String[] line = Strings.explode(s, ' ');
                Triple t = new Triple(
                    Double.parseDouble(line[0]),
                    Double.parseDouble(line[1]),
                    Double.parseDouble(line[2])
                );
                data.add(t);
            }
            catch(NumberFormatException ex) { System.err.println(ex.getMessage()); }
            catch(IndexOutOfBoundsException ex) { System.err.println(ex.getMessage()); }
        }
        
        this.fitPlane(data);
        System.out.println("Anchor: "+this.getAnchor());
        System.out.println("Normal: "+this.getNormal());
        Triple plus = new Triple(this.getAnchor()).add(this.getNormal());
        System.out.println("Ar+Nm : "+plus);
    }

    public static void main(String[] args)
    {
        LsqPlane mainprog = new LsqPlane();
        try
        {
            mainprog.Main();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
//}}}
}//class

