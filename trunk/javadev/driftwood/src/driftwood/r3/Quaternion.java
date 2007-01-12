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
* <code>Quaternion</code> implements quaternions for describing 3-D rotations.
* This implementation is intentionally bare-bones; more functions will be
* added as needed.
*
* <p>Implementation is based on the public domain code in <code>quat.c</code>
* from the VRPN project at UNC.
* Said code is based on Warren Robinett's adapted version of Ken
* Shoemake's code, as seen in Shoemake's 1985 SIGGRAPH paper.
* Some of the code assumes a right-handed coordinate system
* (as does the rest of this package), but I don't know what all that affects.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jan 12 16:26:02 EST 2007
*/
public class Quaternion implements MutableTuple3
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    protected double x; // imaginary (I)
    protected double y; // imaginary (J)
    protected double z; // imaginary (K)
    protected double w; // real / scalar
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Constructs the identity quaternion, (0 0 0 1). */
    public Quaternion()
    { this(0, 0, 0, 1); }
    
    /** Constructs a new quaternion with imaginary components x, y, z and real component w. */
    public Quaternion(double x, double y, double z, double w)
    {
        super();
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
//}}}

//{{{ MutableTuple3: getX/Y/Z, setX/Y/Z
//##################################################################################################
    /** Returns the first element of this tuple */
    public double getX() { return x; }
    /** Returns the second element of this tuple */
    public double getY() { return y; }
    /** Returns the third element of this tuple */
    public double getZ() { return z; }
    /** Returns the fourth element of this tuple */
    public double getW() { return w; }
    
    /** Assigns a value to the first element of this tuple */
    public void setX(double x0) { x = x0; }
    /** Assigns a value to the second element of this tuple */
    public void setY(double y0) { y = y0; }
    /** Assigns a value to the third element of this tuple */
    public void setZ(double z0) { z = z0; }
    /** Assigns a value to the fourth element of this tuple */
    public void setW(double w0) { w = w0; }
    
    /** Assigns a value to three elements of this tuple (w is unaffected). */
    public void setXYZ(double x0, double y0, double z0)
    {
        x = x0;
        y = y0;
        z = z0;
    }

    /** Assigns a value to all elements of this tuple. */
    public void setXYZW(double x0, double y0, double z0, double w0)
    {
        x = x0;
        y = y0;
        z = z0;
        w = w0;
    }
//}}}

//{{{ likeRotation
//##############################################################################
    /**
    * Makes a quaternion from the given transform, assuming it represents a rotation.
    * No warranty if you apply this to a transform that's not a rotation!
    */
    public Quaternion likeRotation(Transform t)
    {
        // q_from_row_matrix()
        
        double trace = t.m11 + t.m22 + t.m33;
        if(trace > 0.0)
        {
            double s = Math.sqrt(trace + 1.0);
            this.w = s * 0.5;
            s = 0.5 / s; // now s = 1 / sqrt(trace+1)
            
            this.x = (t.m23 - t.m32) * s;
            this.y = (t.m31 - t.m13) * s;
            this.z = (t.m12 - t.m21) * s;
        } 
        else 
        {
            int i = 1, j = 2, k = 3;
            if(t.m22 > t.m11)       { i = 2; j = 3; k = 1; }
            if(t.m33 > t.get(i,i))  { i = 3; j = 1; k = 2; }
            
            double s = Math.sqrt( (t.get(i,i) - (t.get(j,j)+t.get(k,k))) + 1.0 );
            
            double[] destQuat = new double[4]; // {dummy, x,  y, z}
            
            destQuat[i] = s * 0.5;
            s = 0.5 / s;
            destQuat[j] = (t.get(i,j) + t.get(j,i)) * s;
            destQuat[k] = (t.get(i,k) + t.get(k,i)) * s;
            
            this.x = destQuat[1];
            this.y = destQuat[2];
            this.z = destQuat[3];
            this.w = (t.get(j,k) - t.get(k,j)) * s;
        }
        
        return this;
    }
//}}}

//{{{ isNaN, equals, hashCode, toString
//##################################################################################################
    /** Returns <code>true</code> iff one or more component is Not-A-Number */
    public boolean isNaN()
    { return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isNaN(w); }
    
    /**
    * Obeys the general contract of Object.equals().
    * Two quaternions are equal if their x, y, z, and w coordinates are equal.
    * Two quaternions producing equivalent rotations may not be considered
    * equal, as they can have different coordinates.
    */
    public boolean equals(Object o)
    {
        if(! (o instanceof Quaternion)) return false;
        else
        {
            Quaternion q = (Quaternion) o;
            return (x == q.x && y == q.y && z == q.z && w == q.w);
        }
    }
    
    /**
    * Obeys the general contract of Object.hashCode().
    * Based on Colt's HashFunctions.java.
    */
    public int hashCode()
    {
        // I stole this from Colt:
        //   this avoids excessive hashCollisions
        //   in the case values are of the form (1.0, 2.0, 3.0, ...)
        int b1 = Float.floatToIntBits((float)x*663608941.737f);
        int b2 = Float.floatToIntBits((float)y*663608941.737f);
        int b3 = Float.floatToIntBits((float)z*663608941.737f);
        int b4 = Float.floatToIntBits((float)w*663608941.737f);
        // The rotation of bits is my own idea
        return (b1 ^ (b2<<11 | b2>>>21) ^ (b3<<22 | b3>>>10) ^ b4);
    }
    
    /**
    * Prints (x, y, z, w)
    */
    public String toString()
    {
        return "("+x+", "+y+", "+z+", "+w+")";
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

