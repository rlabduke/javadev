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
import driftwood.util.SoftLog;
//}}}
/**
* <code>CrystalVertexSource</code> manages vertex information built
* from a crystallographic electron density map file.
* This is an abstract base class that supports
* X-PLOR/CNS and O (DSN6) formats via concrete subclasses.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb 10 14:34:41 EST 2003
*/
abstract public class CrystalVertexSource implements VertexLocator, VertexEvaluator
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    public  int                 aSteps;                     // number of subdivisions along 'a' axis
    public  int                 aMin;                       // minimum index on 'a' axis, inclusive
    public  int                 aMax;                       // maximum index on 'a' axis, INCLUSIVE
    public  int                 aCount;                     // aMax - aMin + 1
    public  int                 bSteps;
    public  int                 bMin;
    public  int                 bMax;
    public  int                 bCount;
    public  int                 cSteps;
    public  int                 cMin;
    public  int                 cMax;
    public  int                 cCount;
    public  double              aLength;                    // length of unit cell along 'a' axis, in Angstroms
    public  double              bLength;
    public  double              cLength;
    public  double              alpha;                      // angle between 'b' & 'c', in degrees
    public  double              beta;                       // angle between 'a' & 'c', in degrees
    public  double              gamma;                      // angle between 'a' & 'b', in degrees
    public  double              mean        = 0;            // average density value
    public  double              sigma       = 1;            // standard deviation
    
    double              tax, tbx, tby,              // Transformation matrix for cryst. --> Cart.
                        tcx, tcy, tcz;
//}}}

//{{{ Constructor (init)
//##################################################################################################
    /**
    * Initializes a new VertexSource based on a crystallographic map file.
    * Subclass constructors should call this method.
    * @param readData if false, only header info will be read
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    void init(boolean readData) throws IOException
    {
        readHeader();
        if(readData)
        {
            readData();
        }
        initTransformMatrix();
    }
//}}}

//{{{ readHeader
//##################################################################################################
    /**
    * Decodes information from the map header.
    * In particular, this method should fill in all the unit cell parameters.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    abstract void readHeader() throws IOException;
//}}}

//{{{ readData
//##################################################################################################
    /**
    * Decodes the body of a map file (i.e. density values for all grid points) and stores it in
    * memory. Assumes readHeader has already been called.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    abstract void readData() throws IOException;
//}}}

//{{{ hasData
//##################################################################################################
    /**
    * Returns true iff density data was read in at the time of creation.
    */
    abstract public boolean hasData();
//}}}

//{{{ initTransformMatrix
//##################################################################################################
    /**
    * Uses the unit cell parameters to construct a matrix for transitioning from
    * crystallographic coordinates (indices into data grid) into Cartesian coordinates.
    *
    * <p>Suppose I have a unit cell with sides of length A, B, and C; and angles between them
    * alpha, beta, and gamma; where the axes are analogous to X, Y, and Z in a right handed system;
    * and alpha is the angle between axis a and axis c, beta from a to c,
    * and gamma from a to b.
    *
    * <p>Define vectors a, b, and c as the unit cell axes such that they form a basis in R3,
    * and they define a frame that shares a common origin with the Cartesian system.
    * Given two column vectors u = [ux uy uz] and v = [vx vy vz], let u represent a point
    * in crystallographic coordinates and v, the same point in Cartesian coordinates.
    *
    * I want the 3x3 matrix T such that T*u = v. There are now two (or more) assumptions we
    * could make about the correspondances between the axes.
    *
    * I will assume that a points in the direction of x, that b lies in the x-y plane,
    * and that c falls where it may. This gives a matrix T like this:
<p><pre>
    [ ax bx cx ]
T = [  0 by cy ]
    [  0  0 cz ]
</pre>
    * ax is the x component of a in the Cartesian system, etc., such that a = (ax,0,0),
    * bx = (bx,by,0) and c = (cx,cy,cz) when they are expressed as vectors in Cartesian
    * coodinate space.
    *
    * <p>The coefficients here can be derived from the dot products and the fact that
    * by > 0 (b points in roughly the direction of the y axis) and
    * cz > 0 (c points in roughly the direction of the z axis).
    *
    * <p>Thus:
    * <br>ax = A
    * <br>bx = [A*B*cos(gamma)] / ax = B*cos(gamma)
    * <br>by = pos_sqrt[B*B - bx*bx] = B*pos_sqrt[1-cos(gamma)*cos(gamma)] = B*sin(gamma)
    * <br>cx = [A*C*cos(beta)] / ax = C*cos(beta)
    * <br>cy = [B*C*cos(alpha) - bx*cx] / by = C*[cos(alpha) - cos(beta)*cos(gamma)] / sin(gamma)]
    * <br>cz = pos_sqrt[C*C - cx*cx - cy*cy]
    */
    void initTransformMatrix()
    {
        double cosAlpha     = Math.cos(Math.toRadians(alpha)),
               cosBeta      = Math.cos(Math.toRadians(beta)),
               cosGamma     = Math.cos(Math.toRadians(gamma)),
               sinGamma     = Math.sin(Math.toRadians(gamma));
        
        tax = aLength;
        tbx = bLength * cosGamma;
        //tby = bLength * Math.sqrt(1.0 - cosGamma*cosGamma);
        tby = bLength * sinGamma;
        tcx = cLength * cosBeta;
        //tcy = cLength * (cosAlpha - cosBeta*cosGamma) / Math.sqrt(1.0 - cosGamma*cosGamma);
        tcy = cLength * (cosAlpha - cosBeta*cosGamma) / sinGamma;
        tcz = Math.sqrt(cLength*cLength - tcx*tcx - tcy*tcy);
        
        // In the case of orthogonal axes, remove roundoff error
        if(Math.abs(tbx) < 1e-12) tbx = 0.0;
        if(Math.abs(tcx) < 1e-12) tcx = 0.0;
        if(Math.abs(tcy) < 1e-12) tcy = 0.0;
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
    * @throws IndexOutOfBoundsException if no such vertex exists.
    *   This implementation never throws IndexOutOfBoundsException.
    */
    public double evaluateVertex(int i, int j, int k)
    {
        // move to zero-based indices
        i -= aMin; j -= bMin; k -= cMin;
        
        // wrap to the range 0 ... Steps
        // a mod b == (a<0 ? b+a%b : a%b)
        i = (i < 0 ? aSteps + i%aSteps : i%aSteps);
        j = (j < 0 ? bSteps + j%bSteps : j%bSteps);
        k = (k < 0 ? cSteps + k%cSteps : k%cSteps);
        
        try
        {
        // return a legal value if one can be had, NaN otherwise
        if(i >= aCount || j >= bCount || k >= cCount) return Double.NaN;
            //throw new IndexOutOfBoundsException("Can't find <"+i+","+j+","+k+"> without sym ops");
        else return getValue(i, j, k);
        }
        catch(ArrayIndexOutOfBoundsException ex)
        {
            SoftLog.err.println(ex.getMessage());
            SoftLog.err.println("    (i,j,k)="+i+" "+j+" "+k+"; count="+aCount+" "+bCount+" "+cCount+"; steps="+aSteps+" "+bSteps+" "+cSteps);
            return Double.NaN;
        }
    }
//}}}

//{{{ getValue, evaluateAtPoint
//##################################################################################################
    /**
    * Returns the value at the specified grid point,
    * where the indexes i, j, and k have been adjusted
    * to start from 0 (i.e. i==0 means aMin, j==1 means bMin+1, etc.)
    * No wrapping, etc. is done by this function -- you probably want evaluateVertex() instead.
    */
    abstract public double getValue(int i, int j, int k);
    
    /**
    * Linearly estimates the value at a specific set of Cartesian coordinates.
    * Return value may be NaN if no data is available in this region.
    */
    public double evaluateAtPoint(double x, double y, double z)
    {
        double[] ijk = new double[3];
        findVertexForPoint(x, y, z, ijk);
        int lowI = (int)Math.floor(ijk[0]);
        int lowJ = (int)Math.floor(ijk[1]);
        int lowK = (int)Math.floor(ijk[2]);
        double aI = ijk[0] - lowI;
        double aJ = ijk[1] - lowJ;
        double aK = ijk[2] - lowK;
        
        return
            (1-aI)*(1-aJ)*(1-aK)*evaluateVertex(lowI, lowJ, lowK) +
            (1-aI)*(1-aJ)*(aK)*evaluateVertex(lowI, lowJ, lowK+1) +
            (1-aI)*(aJ)*(1-aK)*evaluateVertex(lowI, lowJ+1, lowK) +
            (1-aI)*(aJ)*(aK)*evaluateVertex(lowI, lowJ+1, lowK+1) +
            (aI)*(1-aJ)*(1-aK)*evaluateVertex(lowI+1, lowJ, lowK) +
            (aI)*(1-aJ)*(aK)*evaluateVertex(lowI+1, lowJ, lowK+1) +
            (aI)*(aJ)*(1-aK)*evaluateVertex(lowI+1, lowJ+1, lowK) +
            (aI)*(aJ)*(aK)*evaluateVertex(lowI+1, lowJ+1, lowK+1);
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
        /* simple case for orthogonal axes * /
        xyz[0] = i * aStepLength;
        xyz[1] = j * bStepLength;
        xyz[2] = k * cStepLength;
        /* simple case for orthogonal axes */
        
        /* non-orthogonal axes: a matches x */
        double a, b, c;
        a = (double)i / (double)aSteps;
        b = (double)j / (double)bSteps;
        c = (double)k / (double)cSteps;
        xyz[0] = tax*a + tbx*b + tcx*c;
        xyz[1] = tby*b + tcy*c;
        xyz[2] = tcz*c;
        /* non-orthogonal axes: a matches x */
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
        // Do Gaussian elimination to solve for i, j, and k
        double a, b, c;
        c = (z) / tcz;
        b = (y - tcy*c) / tby;
        a = (x - tcx*c - tbx*b) / tax;
        ijk[0] = (int)Math.round(a * aSteps);
        ijk[1] = (int)Math.round(b * bSteps);
        ijk[2] = (int)Math.round(c * cSteps);
    }

    /**
    * Returns the fractional indices of the pseudo-vertex at some point &lt;x,y,z&gt;.
    * The numbers don't correspond to a real (integer indexed) vertex, but can be used
    * to choose such indices for e.g. linear interpolation.
    * @param x      the x coordinate in the Cartesian system
    * @param y      the x coordinate in the Cartesian system
    * @param z      the x coordinate in the Cartesian system
    * @param ijk    a double[3] to be filled with the indices of the pseudo-vertex
    */
    public void findVertexForPoint(double x, double y, double z, double[] ijk)
    {
        // Do Gaussian elimination to solve for i, j, and k
        double a, b, c;
        c = (z) / tcz;
        b = (y - tcy*c) / tby;
        a = (x - tcx*c - tbx*b) / tax;
        ijk[0] = (a * aSteps);
        ijk[1] = (b * bSteps);
        ijk[2] = (c * cSteps);
    }
//}}}

//{{{ isComplete, isOrthogonal
//##################################################################################################
    /** Returns true iff a value can be calculated for every &lt;i,j,k&gt; */
    public boolean isComplete()
    {
        return (aCount >= aSteps && bCount >= bSteps && cCount >= cSteps);
    }
    
    /** Returns true iff the unit cell axes are orthogonal to the Cartesian ones */
    public boolean isOrthogonal()
    {
        return (alpha == 90.0 && beta == 90.0 && gamma == 90.0);
    }
//}}}

//{{{ kinUnitCell
//##################################################################################################
    /** Returns a kinemage format trace of a, b, and c */
    public String kinUnitCell()
    {
        DecimalFormat df = new DecimalFormat("0.###");
        return "@vectorlist {unit cell axes} color= gray\n"+
        "{a}P 0 0 0 {a} "+df.format(tax)+" 0 0\n"+
        "{b}P 0 0 0 {b} "+df.format(tbx)+" "+df.format(tby)+" 0\n"+
        "{c}P 0 0 0 {c} "+df.format(tcx)+" "+df.format(tcy)+" "+df.format(tcz);
    }
//}}}

//{{{ toString()
//##################################################################################################
    public String toString()
    {
        return
        "A: "+aMin+" -> "+aMax+" ("+aCount+" samples); "+aLength+" A in "+aSteps+" samples\n"+
        "B: "+bMin+" -> "+bMax+" ("+bCount+" samples); "+bLength+" A in "+bSteps+" samples\n"+
        "C: "+cMin+" -> "+cMax+" ("+cCount+" samples); "+cLength+" A in "+cSteps+" samples\n"+
        "Is complete? "+isComplete()+"; Is orthogonal? "+isOrthogonal()+"\n"+
        "alpha = "+alpha+"; beta = "+beta+"; gamma = "+gamma+"\n"+
        "|a| = |<"+tax+", 0, 0>| = "+Math.sqrt(tax*tax)+"\n"+
        "|b| = |<"+tbx+", "+tby+", 0>| = "+Math.sqrt(tbx*tbx + tby*tby)+"\n"+
        "|c| = |<"+tcx+", "+tcy+", "+tcz+">| = "+Math.sqrt(tcx*tcx + tcy*tcy + tcz*tcz)+"\n"+
        "mean = "+mean+"; sigma = "+sigma;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

