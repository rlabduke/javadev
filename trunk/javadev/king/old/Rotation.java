// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

//import java.awt.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import gnu.regexp.*;
import driftwood.r3.*;
//}}}
/**
 * <code>Rotation</code> is a matrix rotation about some point in 3-space.
 * It supports an option basis component, allowing the user to work in some
 * arbitrary but convenient rotated coordinate system.
 * They are (relatively) expensive to allocate, but easy to re-initialize,
 * thus suggesting reuse of one Rotation object for many rotation tasks when time is short.
 *
 * <p>The following information on rotations matrices was found at
 * <a href="http://www.makegames.com/3drotation/">http://www.makegames.com/3drotation/</a>
<pre>
R is special orthogonal (I'll trust their math):
  R R' = I    (R times its transpose is identity)
  det R = 1   (determinant of R equals 1)

"A more helpful set of properties is provided by Michael E. Pique in Graphics Gems (Glassner, Academic Press, 1990): 
1. R is normalized: the squares of the elements in any row or column sum to 1. 
2. R is orthogonal: the dot product of any pair of rows or any pair of columns is 0. 
3. The rows of R represent the coordinates in the original space of unit vectors along the coordinate axes of the rotated space. (Figure 1). 
4. The columns of R represent the coordinates in the rotated space of unit vectors along the axes of the original space."

One can use the rule 3 and cross products for building a "good" rotation matrix (sort of),
but without using what they call a World Up vector,
since we're in a molecule and that doesn't mean anything.
</pre>
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Mon Sep  9 11:55:50 EDT 2002
*/
public class Rotation //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    double[] basis = {1,0,0,0,1,0,0,0,1};       // basis matrix
    double[] xform = {1,0,0,0,1,0,0,0,1};       // rotation matrix
    double[] total = new double[9];             // composite matrix
    double[] tmp   = new double[9];             // temporary matrix for calcs
    Triple center = new Triple();               // center point
    boolean dirty = true;                       // changed since last composited?
    
    // Temporary variables for basis calculations
    Triple pri  = new Triple();
    Triple sec  = new Triple();
    Triple tert = new Triple();
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates an identity transform about the origin.
    */
    public Rotation()
    {}

    /**
    * Creates an identity transform about a specified point.
    * @param t the desired center of rotation, in standard (non-rotated) coordinates
    */
    public Rotation(Triple t)
    { center.like(t); }
//}}}

//{{{ get/set Center/Matrix/Basis
//##################################################################################################
    /** Gets the current origin of rotation, in natural/non-rotated coordinates. */
    public Triple getCenter()
    { return new Triple(center); }
    /** Sets the current origin of rotation, in natural/non-rotated coordinates. */
    public void setCenter(Triple c)
    { center.like(c); }
    /**
    * Sets the current origin of rotation to be c itself, so that if c changes in the future,
    * so will the center of rotation. Use this function with care!
    */
    public void setMobileCenter(Triple c)
    { center = c; }
    
    /** Gets the current rotation matrix */
    public double[] getMatrix()
    { return (double[])xform.clone(); }
    /** Sets the current rotation matrix. No checking is done to ensure well-formedness. */
    public void setMatrix(double[] m)
    //{ xform = (double[])m.clone(); dirty = true; }
    { System.arraycopy(m, 0, xform, 0, 9); dirty = true; }
    /** Resets the rotation matrix to identity. */
    public void resetMatrix()
    {
        xform[0] = 1; xform[1] = 0; xform[2] = 0;
        xform[3] = 0; xform[4] = 1; xform[5] = 0;
        xform[6] = 0; xform[7] = 0; xform[8] = 1;
        dirty = true;
    }
    
    /** Gets the current basis matrix, which defines the coordinate system the rotation takes place in. */
    public double[] getBasis()
    { return (double[])basis.clone(); }
    /** Sets the current rotation matrix. No checking is done to ensure well-formedness. */
    public void setBasis(double[] b)
    { basis = (double[])b.clone(); dirty = true; }
//}}}

//{{{ mmult(), transpose()
//##################################################################################################
    // Multiplies two 3x3 matrices
    double[] mmult(double[] a, double[] b, double[] result)
    {
        result[0] = a[0]*b[0] + a[1]*b[3] + a[2]*b[6];
        result[1] = a[0]*b[1] + a[1]*b[4] + a[2]*b[7];
        result[2] = a[0]*b[2] + a[1]*b[5] + a[2]*b[8];
        result[3] = a[3]*b[0] + a[4]*b[3] + a[5]*b[6];
        result[4] = a[3]*b[1] + a[4]*b[4] + a[5]*b[7];
        result[5] = a[3]*b[2] + a[4]*b[5] + a[5]*b[8];
        result[6] = a[6]*b[0] + a[7]*b[3] + a[8]*b[6];
        result[7] = a[6]*b[1] + a[7]*b[4] + a[8]*b[7];
        result[8] = a[6]*b[2] + a[7]*b[5] + a[8]*b[8];
        return result;
    }
    
    // Constructs the transpose of a 3x3 matrix
    double[] transpose(double[] a, double[] result)
    {
        result[0] = a[0];
        result[1] = a[3];
        result[2] = a[6];
        result[3] = a[1];
        result[4] = a[4];
        result[5] = a[7];
        result[6] = a[2];
        result[7] = a[5];
        result[8] = a[8];
        return result;
    }
//}}}

//{{{ rotateX/Y/Z()
//##################################################################################################
    /**
    * Rotates about the axis defined as 'x' by the basis of this tranform.
    * @param t the amount of rotation, in radians
    */
    public void rotateX(double t)
    {
        double sin, cos;
        sin = Math.sin(t);
        cos = Math.cos(t);
        tmp[0] = 1;
        tmp[1] = 0;
        tmp[2] = 0;
        tmp[3] = 0;
        tmp[4] = cos;
        tmp[5] = -sin;
        tmp[6] = 0;
        tmp[7] = sin;
        tmp[8] = cos;
        mmult(tmp, xform, total);
        // swap xform and total (avoid allocation of new arrays)
        double[] swap = xform;
        xform = total;
        total = swap;
        dirty = true;
    }

    /**
    * Rotates about the axis defined as 'y' by the basis of this tranform.
    * @param t the amount of rotation, in radians
    */
    public void rotateY(double t)
    {
        double sin, cos;
        sin = Math.sin(t);
        cos = Math.cos(t);
        tmp[0] = cos;
        tmp[1] = 0;
        tmp[2] = sin;
        tmp[3] = 0;
        tmp[4] = 1;
        tmp[5] = 0;
        tmp[6] = -sin;
        tmp[7] = 0;
        tmp[8] = cos;
        mmult(tmp, xform, total);
        // swap xform and total (avoid allocation of new arrays)
        double[] swap = xform;
        xform = total;
        total = swap;
        dirty = true;
    }

    /**
    * Rotates about the axis defined as 'z' by the basis of this tranform.
    * @param t the amount of rotation, in radians
    */
    public void rotateZ(double t)
    {
        double sin, cos;
        sin = Math.sin(t);
        cos = Math.cos(t);
        tmp[0] = cos;
        tmp[1] = -sin;
        tmp[2] = 0;
        tmp[3] = sin;
        tmp[4] = cos;
        tmp[5] = 0;
        tmp[6] = 0;
        tmp[7] = 0;
        tmp[8] = 1;
        mmult(tmp, xform, total);
        // swap xform and total (avoid allocation of new arrays)
        double[] swap = xform;
        xform = total;
        total = swap;
        dirty = true;
    }
//}}}

//{{{ composite()
//##################################################################################################
    /** This function combines basis and transform matrices for faster transforms later. */
    void composite()
    {
        transpose(basis, total);  // B'
        mmult(total, xform, tmp); // B'*A
        mmult(tmp, basis, total); // (B'*A) * B
        
        dirty = false;
    }
//}}}

//{{{ tranform()
//##################################################################################################
    /**
    * Transforms a <code>Triple</code> from one coordinate system to another.
    * The coordinates of the Triple are altered by this function, and the
    * Triple itself is returned.
    */
    public Triple transform(Triple t)
    {
        if(dirty) composite();
        
        double x0, y0, z0;
        x0 = t.x - center.x;
        y0 = t.y - center.y;
        z0 = t.z - center.z;
        
        t.x = total[0]*x0 + total[1]*y0 + total[2]*z0 + center.x;
        t.y = total[3]*x0 + total[4]*y0 + total[5]*z0 + center.y;
        t.z = total[6]*x0 + total[7]*y0 + total[8]*z0 + center.z;
        
        return t;
    }

    /**
    * Transforms a <code>KPoint</code> from one coordinate system to another.
    * The raw coordinates of the KPoint are altered by this function.
    */
    public void transform(KPoint p)
    {
        if(dirty) composite();
        
        double x0, y0, z0;
        x0 = p.x0 - center.x;
        y0 = p.y0 - center.y;
        z0 = p.z0 - center.z;
        
        p.x0 = (float)(total[0]*x0 + total[1]*y0 + total[2]*z0 + center.x);
        p.y0 = (float)(total[3]*x0 + total[4]*y0 + total[5]*z0 + center.y);
        p.z0 = (float)(total[6]*x0 + total[7]*y0 + total[8]*z0 + center.z);
    }
//}}}

//{{{ makeBasis()
//##################################################################################################
    /**
    * Makes a set of basis vectors to e.g. support bond rotations.
    * <code>primary</code> is a vector from the origin, describing the x-axis for rotations.
    * <code>secondary</code> describes the direction for the y-axis,
    * leaving the z-axis orthogonal to the other two. This uses rule (3) above.
    */
    public void makeBasis(Triple primary, Triple secondary)
    {
        pri.like(primary).unit();
        tert.like(pri).cross(secondary).unit();
        sec.like(tert).cross(pri);
        
        basis[0] = pri.getX();
        basis[1] = pri.getY();
        basis[2] = pri.getZ();
        basis[3] = sec.getX();
        basis[4] = sec.getY();
        basis[5] = sec.getZ();
        basis[6] = tert.getX();
        basis[7] = tert.getY();
        basis[8] = tert.getZ();
        dirty = true;
    }
    
    /**
    * Makes a set of basis vectors to e.g. support bond rotations.
    * <code>primary</code> and <code>secondary</code> describe vector endpoints
    * for the x- and y-axis, respectively, and <code>origin</code> describes
    * the origin of those vectors.
    */
    public void makeBasis(Triple primary, Triple secondary, Triple origin)
    {
        pri.like(primary).sub(origin).unit();
        sec.like(secondary).sub(origin);
        tert.like(pri).cross(sec).unit();
        sec.like(tert).cross(pri);
        
        basis[0] = pri.getX();
        basis[1] = pri.getY();
        basis[2] = pri.getZ();
        basis[3] = sec.getX();
        basis[4] = sec.getY();
        basis[5] = sec.getZ();
        basis[6] = tert.getX();
        basis[7] = tert.getY();
        basis[8] = tert.getZ();
        dirty = true;
    }
//}}}

//{{{ construct4()
//##################################################################################################
    /**
    * Imitates the construct4 function in Mage.
    * Given three points, it constructs a fourth at the specified distance from the third,
    * with the specified angle from the second to the third to the fourth,
    * and the specified dihedral from 1st to 2nd to 3rd to 4th.
    *
    * Angles are specified in degrees.
    *
    * <p>Let the first three points picked, in order, be A, B, and C.
    * Define a basis such that all three points lie in the x-y plane, with B-->C pointing
    * along the x axis and (B-->C)x(B-->A) defining the z axis.
    * [That is, do <code>makeBasis(C, A, B)</code>]
    * Let C be the origin.
    * Given a distance D, an angle E, and a dihedral F, construct a vector
    * in the basis-space from (0,0,0) to (0,D,0).
    * Rotate it (90 - E) degrees about the z axis, then F degrees about the x axis.
    * Transform out of the basis into standard coordinates, and return the corresponding Triple.
    */
    public Triple construct4(double dist, double ang, double dihe)
    {
        double sin, cos;
        double x = 0, y = dist, z = 0;
        Triple t = new Triple();
        ang      = Math.toRadians(90 - ang);
        dihe     = Math.toRadians(dihe);
        
        // "Angle" rotation (z axis)
        sin = Math.sin(ang);
        cos = Math.cos(ang);
        t.x = cos*x - sin*y;
        t.y = sin*x + cos*y;
        t.z = z;
        
        // "Dihedral" rotation (x axis)
        sin = Math.sin(dihe);
        cos = Math.cos(dihe);
        x = t.x;
        y = cos*t.y - sin*t.z;
        z = sin*t.y + cos*t.z;
        
        // Get out of basis space!
        transpose(basis, tmp);
        t.x = tmp[0]*x + tmp[1]*y + tmp[2]*z + center.x;
        t.y = tmp[3]*x + tmp[4]*y + tmp[5]*z + center.y;
        t.z = tmp[6]*x + tmp[7]*y + tmp[8]*z + center.z;
        
        return t;
    }
//}}}

//{{{ echoAll()
//##################################################################################################
    public void echoAll(PrintStream out)
    {
        if(dirty) composite();
        
        DecimalFormat df = new DecimalFormat(" 0.000;-0.000");
        out.println("xform: <"+df.format(xform[0])+","+df.format(xform[1])+","+df.format(xform[2])+">");
        out.println("     : <"+df.format(xform[3])+","+df.format(xform[4])+","+df.format(xform[5])+">");
        out.println("     : <"+df.format(xform[6])+","+df.format(xform[7])+","+df.format(xform[8])+">");
        out.println("basis: <"+df.format(basis[0])+","+df.format(basis[1])+","+df.format(basis[2])+">");
        out.println("     : <"+df.format(basis[3])+","+df.format(basis[4])+","+df.format(basis[5])+">");
        out.println("     : <"+df.format(basis[6])+","+df.format(basis[7])+","+df.format(basis[8])+">");
        out.println("total: <"+df.format(total[0])+","+df.format(total[1])+","+df.format(total[2])+">");
        out.println("     : <"+df.format(total[3])+","+df.format(total[4])+","+df.format(total[5])+">");
        out.println("     : <"+df.format(total[6])+","+df.format(total[7])+","+df.format(total[8])+">");
    }
//}}}

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}
}//class

