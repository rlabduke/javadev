// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.r3;

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
* <code>Transform</code> describes an affine transformation
* in three dimensions using homogenous coordinates, such that
* any number of rotations, translations, scalings, shears, etc.
* may be concatenated into a single matrix operation.
*
* <p>The resulting matrix M allows transformation of a column
* vector x like this: Mx = x'
*
* <p>In full, we have:
<pre>
[ r11  r12  r13  t14 ] [ x ]    [ x' ]
[ r21  r22  r23  t24 ] [ y ]    [ y' ]
[ r31  r32  r33  t34 ] [ z ] == [ z' ]
[  0    0    0    1  ] [ 1 ]    [ 1  ]
</pre>
* <br>where the r## are elements that cause rotation and
* the t## are elements that cause translation.
*
* <p>The above example shows transformation of a point;
* points are represented as [ x y z 1 ]. Vectors, on the
* other hand, are represented as [ x y z 0 ]. Thus, vectors
* are rotated by such a transformation but are not translated,
* because their meaning is not tied to a specific origin point.
*
* <p>Most functions return <code>this</code>, to facilitate chaining.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 10 13:03:17 EST 2003
*/
public class Transform //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** The elements of the transformation matrix */
    protected double m11 = 1, m12 = 0, m13 = 0, m14 = 0,
                     m21 = 0, m22 = 1, m23 = 0, m24 = 0,
                     m31 = 0, m32 = 0, m33 = 1, m34 = 0;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructs a new identity transform.
    */
    public Transform()
    {
    }
//}}}

//{{{ like
//##################################################################################################
    /** Makes this Transform into a copy of t */
    public Transform like(Transform t)
    {
        m11 = t.m11;
        m12 = t.m12;
        m13 = t.m13;
        m14 = t.m14;
        m21 = t.m21;
        m22 = t.m22;
        m23 = t.m23;
        m24 = t.m24;
        m31 = t.m31;
        m32 = t.m32;
        m33 = t.m33;
        m34 = t.m34;
        return this;
    }
//}}}

//{{{ likeTranslation
//##################################################################################################
    /** Makes this Transform into a translation by (x,y,z) */
    public Transform likeTranslation(double x, double y, double z)
    {
        m11 = 1;
        m12 = 0;
        m13 = 0;
        m14 = x;
        m21 = 0;
        m22 = 1;
        m23 = 0;
        m24 = y;
        m31 = 0;
        m32 = 0;
        m33 = 1;
        m34 = z;
        return this;
    }

    /** Makes this Transform into a translation by t */
    public Transform likeTranslation(Tuple3 t)
    {
        return likeTranslation(t.getX(), t.getY(), t.getZ());
    }
//}}}

//{{{ likeRotation
//##################################################################################################
    /**
    * Makes this Transform into a rotation at the origin
    * by theta degrees around the axis defined by axisVector
    */
    public Transform likeRotation(Tuple3 axisVector, double theta)
    {
        Triple axis = new Triple().like(axisVector).unit();
        double a, b, c, radians, sin, cos, one_minus_cos;
        a = axis.x;
        b = axis.y;
        c = axis.z;
        radians = Math.toRadians(theta);
        sin = Math.sin(radians);
        cos = Math.cos(radians);
        one_minus_cos = 1.0 - cos;
        
        // This math was worked out VERY tediously by hand.
        // Keys to eliminating extra variables are:
        // 1. sum of squares of row/column = 1
        // 2. cross prod of any two rows/cols is || to the third one
        // 3. B' x B = I (many entries are 0)
        // 3'. dot prod of any two rows/cols is 0 (equiv to note 3)
        m11 = a*a*one_minus_cos +   cos;
        m12 = a*b*one_minus_cos - c*sin;
        m13 = a*c*one_minus_cos + b*sin;
        m14 = 0;
        m21 = a*b*one_minus_cos + c*sin;
        m22 = b*b*one_minus_cos +   cos;
        m23 = b*c*one_minus_cos - a*sin;
        m24 = 0;
        m31 = a*c*one_minus_cos - b*sin;
        m32 = b*c*one_minus_cos + a*sin;
        m33 = c*c*one_minus_cos +   cos;
        m34 = 0;
        
        return this;
    }
    
    /**
    * Makes this Transform into a rotation centered at axisFrom
    * by theta degrees around the axis defined between axisFrom and axisTo.
    */
    public Transform likeRotation(Tuple3 axisFrom, Tuple3 axisTo, double theta)
    {
        Triple axis = new Triple().likeVector(axisFrom, axisTo).unit();
        double a, b, c, x, y, z, radians, sin, cos, one_minus_cos;
        a = axis.x;
        b = axis.y;
        c = axis.z;
        x = axisFrom.getX();
        y = axisFrom.getY();
        z = axisFrom.getZ();
        radians = Math.toRadians(theta);
        sin = Math.sin(radians);
        cos = Math.cos(radians);
        one_minus_cos = 1.0 - cos;
        
        // This math was worked out VERY tediously by hand.
        // Keys to eliminating extra variables are:
        // 1. sum of squares of row/column = 1
        // 2. cross prod of any two rows/cols is || to the third one
        // 3. B' x B = I (many entries are 0)
        // 3'. dot prod of any two rows/cols is 0 (equiv to note 3)
        m11 = a*a*one_minus_cos +   cos;
        m12 = a*b*one_minus_cos - c*sin;
        m13 = a*c*one_minus_cos + b*sin;
        m14 = x - (m11*x + m12*y + m13*z);
        m21 = a*b*one_minus_cos + c*sin;
        m22 = b*b*one_minus_cos +   cos;
        m23 = b*c*one_minus_cos - a*sin;
        m24 = y - (m21*x + m22*y + m23*z);
        m31 = a*c*one_minus_cos - b*sin;
        m32 = b*c*one_minus_cos + a*sin;
        m33 = c*c*one_minus_cos +   cos;
        m34 = z - (m31*x + m32*y + m33*z);
        
        return this;
    }
//}}}

//{{{ likeMatrix
//##################################################################################################
    /** Makes this Transform into the specified matrix */
    public Transform likeMatrix(
        double m11, double m12, double m13,
        double m21, double m22, double m23,
        double m31, double m32, double m33)
    {
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m14 = 0;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m24 = 0;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        this.m34 = 0;
        return this;
    }
//}}}

//{{{ likeScale
//##################################################################################################
    /** Makes this Transform into a uniform scaling (enlarge/shrink) by a factor f */
    public Transform likeScale(double f)
    {
        m11 = f;
        m12 = 0;
        m13 = 0;
        m14 = 0;
        m21 = 0;
        m22 = f;
        m23 = 0;
        m24 = 0;
        m31 = 0;
        m32 = 0;
        m33 = f;
        m34 = 0;
        return this;
    }
    
    /** Makes this Transform into a non-uniform scaling (enlarge/shrink) */
    public Transform likeScale(double x, double y, double z)
    {
        m11 = x;
        m12 = 0;
        m13 = 0;
        m14 = 0;
        m21 = 0;
        m22 = y;
        m23 = 0;
        m24 = 0;
        m31 = 0;
        m32 = 0;
        m33 = z;
        m34 = 0;
        return this;
    }
//}}}

//{{{ premult, append
//##################################################################################################
    /** Assigns this matrix the value of t*this */
    public Transform premult(Transform t)
    {
        return append(t);
    }
    
    /**
    * Concatenates t onto this transform so that it happens "after"
    * all the currently included transformations.
    * Equivalent to premult(t)
    */
    public Transform append(Transform t)
    {
        double r11, r12, r13, r14,
               r21, r22, r23, r24,
               r31, r32, r33, r34;

        r11 = t.m11*m11 + t.m12*m21 + t.m13*m31;
        r12 = t.m11*m12 + t.m12*m22 + t.m13*m32;
        r13 = t.m11*m13 + t.m12*m23 + t.m13*m33;
        r14 = t.m11*m14 + t.m12*m24 + t.m13*m34 + t.m14;
        r21 = t.m21*m11 + t.m22*m21 + t.m23*m31;
        r22 = t.m21*m12 + t.m22*m22 + t.m23*m32;
        r23 = t.m21*m13 + t.m22*m23 + t.m23*m33;
        r24 = t.m21*m14 + t.m22*m24 + t.m23*m34 + t.m24;
        r31 = t.m31*m11 + t.m32*m21 + t.m33*m31;
        r32 = t.m31*m12 + t.m32*m22 + t.m33*m32;
        r33 = t.m31*m13 + t.m32*m23 + t.m33*m33;
        r34 = t.m31*m14 + t.m32*m24 + t.m33*m34 + t.m34;
        
        m11 = r11; m12 = r12; m13 = r13; m14 = r14;
        m21 = r21; m22 = r22; m23 = r23; m24 = r24;
        m31 = r31; m32 = r32; m33 = r33; m34 = r34;
        
        return this;
    }
//}}}

//{{{ postmult, prepend
//##################################################################################################
    /** Assigns this matrix the value of this*t */
    public Transform postmult(Transform t)
    {
        return prepend(t);
    }
    
    /**
    * Concatenates t onto this transform so that it happens "before"
    * all the currently included transformations.
    * Equivalent to postmult(t)
    */
    public Transform prepend(Transform t)
    {
        double r11, r12, r13, r14,
               r21, r22, r23, r24,
               r31, r32, r33, r34;

        r11 = m11*t.m11 + m12*t.m21 + m13*t.m31;
        r12 = m11*t.m12 + m12*t.m22 + m13*t.m32;
        r13 = m11*t.m13 + m12*t.m23 + m13*t.m33;
        r14 = m11*t.m14 + m12*t.m24 + m13*t.m34 + m14;
        r21 = m21*t.m11 + m22*t.m21 + m23*t.m31;
        r22 = m21*t.m12 + m22*t.m22 + m23*t.m32;
        r23 = m21*t.m13 + m22*t.m23 + m23*t.m33;
        r24 = m21*t.m14 + m22*t.m24 + m23*t.m34 + m24;
        r31 = m31*t.m11 + m32*t.m21 + m33*t.m31;
        r32 = m31*t.m12 + m32*t.m22 + m33*t.m32;
        r33 = m31*t.m13 + m32*t.m23 + m33*t.m33;
        r34 = m31*t.m14 + m32*t.m24 + m33*t.m34 + m34;
        
        m11 = r11; m12 = r12; m13 = r13; m14 = r14;
        m21 = r21; m22 = r22; m23 = r23; m24 = r24;
        m31 = r31; m32 = r32; m33 = r33; m34 = r34;
        
        return this;
    }
//}}}

//{{{ transform, transformVector
//##################################################################################################
    /** Transforms the given MutableTuple3 in place, treating it like a point */
    public void transform(MutableTuple3 p)
    {
        transform(p, p);
    }
    
    /** Transforms the given Tuple3 and places the result in pOut, treating it like a point */
    public void transform(Tuple3 pIn, MutableTuple3 pOut)
    {
        double x0, y0, z0;
        x0 = pIn.getX();
        y0 = pIn.getY();
        z0 = pIn.getZ();
        pOut.setX(m11*x0 + m12*y0 + m13*z0 + m14);
        pOut.setY(m21*x0 + m22*y0 + m23*z0 + m24);
        pOut.setZ(m31*x0 + m32*y0 + m33*z0 + m34);
    }

    /** Transforms the given MutableTuple3 in place, treating it like a vector */
    public void transformVector(MutableTuple3 v)
    {
        transformVector(v, v);
    }
    
    /** Transforms the given Tuple3 and places the result in vOut, treating it like a vector */
    public void transformVector(Tuple3 vIn, MutableTuple3 vOut)
    {
        double x0, y0, z0;
        x0 = vIn.getX();
        y0 = vIn.getY();
        z0 = vIn.getZ();
        vOut.setX(m11*x0 + m12*y0 + m13*z0);
        vOut.setY(m21*x0 + m22*y0 + m23*z0);
        vOut.setZ(m31*x0 + m32*y0 + m33*z0);
    }
//}}}

//{{{ orthonormalize
//##################################################################################################
    /**
    * Adjusts rotation components of the matrix to ensure that
    * geometry is preserved -- ie, that there are no shear/warp
    * aspects to the transformation.
    * See http://www.makegames.com/3drotation/
    *
    * R is special orthogonal (I'll trust their math):
    *   R R' = I    (R times its transpose is identity)
    *   det R = 1   (determinant of R equals 1)
    *
    * "A more helpful set of properties is provided by Michael E. Pique in Graphics Gems (Glassner, Academic Press, 1990): 
    * 1. R is normalized: the squares of the elements in any row or column sum to 1. 
    * 2. R is orthogonal: the dot product of any pair of rows or any pair of columns is 0. 
    * 3. The rows of R represent the coordinates in the original space of unit vectors along the coordinate axes of the rotated space. (Figure 1). 
    * 4. The columns of R represent the coordinates in the rotated space of unit vectors along the axes of the original space."
    *
    * Here I follow the procedure described above for building a "good" rotation matrix (sort of),
    * but without using what they call a World Up vector, since we're in a molecule & that doesn't mean anything.
    */
    public Transform orthonormalize()
    {
        // As per (3), create a vector for each row of the matrix
        Triple xAxis = new Triple(m11, m12, m13);
        Triple yAxis = new Triple(m21, m22, m23);
        Triple zAxis = new Triple(m31, m32, m33);
        
        // Normalize Z
        zAxis.unit();
        
        // Let X = Y x Z
        xAxis.likeCross(yAxis, zAxis);
        xAxis.unit();
        
        // Let Y = Z x X
        yAxis.likeCross(zAxis, xAxis);
        yAxis.unit();
        
        m11 = xAxis.x; m12 = xAxis.y; m13 = xAxis.z;
        m21 = yAxis.x; m22 = yAxis.y; m23 = yAxis.z;
        m31 = zAxis.x; m32 = zAxis.y; m33 = zAxis.z;
        
        return this;
    }
//}}}

//{{{ hashCode, equals
//##################################################################################################
    /** Two transforms are equal if all their corresponding matrix entries are equal */
    public boolean equals(Object o)
    {
        if(! (o instanceof Transform)) return false;
        Transform t = (Transform)o;
        return (m11 == t.m11 && m12 == t.m12 && m13 == t.m13 && m14 == t.m14
             && m21 == t.m21 && m22 == t.m22 && m23 == t.m23 && m24 == t.m24
             && m31 == t.m31 && m32 == t.m32 && m33 == t.m33 && m34 == t.m34);
    }
    
    /** Based on Triple.hashCode() */
    public int hashCode()
    {
        long hash = Double.doubleToLongBits(m11)
                  ^ Double.doubleToLongBits(m12)
                  ^ Double.doubleToLongBits(m13)
                  ^ Double.doubleToLongBits(m14)
                  ^ Double.doubleToLongBits(m21)
                  ^ Double.doubleToLongBits(m22)
                  ^ Double.doubleToLongBits(m23)
                  ^ Double.doubleToLongBits(m24)
                  ^ Double.doubleToLongBits(m31)
                  ^ Double.doubleToLongBits(m32)
                  ^ Double.doubleToLongBits(m33)
                  ^ Double.doubleToLongBits(m34);
        return (int)(hash ^ (hash >>> 32));
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

