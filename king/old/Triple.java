// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

//import java.awt.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import gnu.regexp.*;
//}}}
/**
 * <code>Triple</code> represents either a point in 3-space or a vector in 3-space.
 * Triples are <em>mutable</em>, so their values may change over time.
 * In particular, all of the mathematical operations (add, sub, scale, cross, etc.) alter
 * the coordinates of the calling object (but not those of the parameters).
 * To avoid this, use the duplicate (dup) function. For example: <code>c = a.dup().add(b)<code>.
 * Most functions that return a Triple merely return <em>this</em> Triple, but thus
 * allow for convenient "chaining" of function calls.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Mon Sep  9 09:33:59 EDT 2002
*/
public class Triple //extends ... implements ...
{
//{{{ Variable definitions
//##################################################################################################
    double x, y, z;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Creates a triple with the specified x, y, and z coordinates. */
    public Triple(double x0, double y0, double z0)
    {
        x = x0;
        y = y0;
        z = z0;
    }
    
    /** Creates a triple with the the same x, y, and z coordinates as t */
    public Triple(Triple t)
    {
        x = t.x;
        y = t.y;
        z = t.z;
    }
    
    /** Creates a triple at the origin. */
    public Triple()
    {
        x = y = z = 0.0;
    }
//}}}

//{{{ dup(), like(), set()
//##################################################################################################
    /** Duplicates (clones) this Triple as a new object. */
    public Triple dup()
    {
        return new Triple(x, y, z);
    }
    
    /** Assigns the coordinates of t to this Triple. A null value leaves coordinates unchanged. */
    public Triple like(Triple t)
    {
        if(t != null)
        {
            x = t.x;
            y = t.y;
            z = t.z;
        }
        return this;
    }

    /** Assigns the specified x, y, and z coordinates to this Triple. */
    public Triple set(double x0, double y0, double z0)
    {
        x = x0;
        y = y0;
        z = z0;
        return this;
    }
//}}}

//{{{ getX/Y/Z()
//##################################################################################################
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
//}}}

//{{{ dot(), cross()
//##################################################################################################
    /**
    * Returns the dot product of this triple and t.
    * The dot product of A and B, A.B, is equal to |A||B|cos(theta),
    * where theta is the angle between vectors from the origin to A and B.
    */
    public double dot(Triple t)
    {
        // Dot product: d . e = d e cos(a) = dxex + dyey + dzez
        return x*t.x + y*t.y + z*t.z;
    }
    
    /**
    * Returns the cross product of this triple and t.
    * The cross product of A and B, AxB, is orthogonal to the plane defined by vectors
    * from the origin to A and B. Its direction (sign) is given by the right-hand rule.
    */
    public Triple cross(Triple t)
    {
        // Cross product: a x b = (aybz-azby, -axbz+azbx, axby-aybx)
        double x0 = y*t.z - z*t.y;
        double y0 = z*t.x - x*t.z;
        double z0 = x*t.y - y*t.x;
        x = x0;
        y = y0;
        z = z0;
        return this;
    }
//}}}

//{{{ mag(), unit(), scale()
//##################################################################################################
    /** Returns the maginitude of a vector from the origin to this point. */
    public double mag()
    {
        return Math.sqrt(x*x + y*y + z*z);
    }
    
    /** Returns a unit vector with the same directionality as this one */
    public Triple unit()
    {
        double mag = Math.sqrt(x*x + y*y + z*z);
        x /= mag;
        y /= mag;
        z /= mag;
        return this;
    }
    
    /** Scales this Triple by k. If k < 1, this vector is shortened; if k > 1, this vector is lengthened. */
    public Triple scale(double k)
    {
        x *= k;
        y *= k;
        z *= k;
        return this;
    }
//}}}

//{{{ add(), sub(), neg(), distance(), sqDistance()
//##################################################################################################
    /** Adds t to this Triple. */
    public Triple add(Triple t)
    {
        x += t.x;
        y += t.y;
        z += t.z;
        return this;
    }
    
    /** Subtracts t from this Triple. */
    public Triple sub(Triple t)
    {
        x -= t.x;
        y -= t.y;
        z -= t.z;
        return this;
    }
    
    /** Negates this triple */
    public Triple neg()
    {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }
    
    /** Returns the distance between this triple and t. */
    public double distance(Triple t)
    {
        double dx, dy, dz;
        dx = x - t.x;
        dy = y - t.y;
        dz = z - t.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Returns the square of the distance between this triple and t. Faster than distance(). */
    public double sqDistance(Triple t)
    {
        double dx, dy, dz;
        dx = x - t.x;
        dy = y - t.y;
        dz = z - t.z;
        return (dx*dx + dy*dy + dz*dz);
    }
//}}}

//{{{ midpoint(), angle(), dihedral()
//##################################################################################################
    /** Creates a point half way between these triples */
    public static Triple midpoint(Triple t1, Triple t2)
    {
        return new Triple( (t1.x+t2.x)/2.0, (t1.y+t2.y)/2.0, (t1.z+t2.z)/2.0 );
    }

    /** Finds the angle in degrees between two vectors (with tails at the origin) */
    public double angle(Triple t)
    {
        return Math.toDegrees(Math.acos(  this.dot(t) / (this.mag() * t.mag())  ));
    }
    
    /** Returns the angle ABC, in degrees. */
    public static double angle(Triple a, Triple b, Triple c)
    {
        Triple ba = a.dup().sub(b), bc = c.dup().sub(b);
        return Math.toDegrees(Math.acos(  ba.dot(bc) / (ba.mag() * bc.mag())  ));
    }
    
    /**
    * Returns the dihedral ABCD, in degrees.
    * This method is not particularly efficient, as it creates
    * 5 new objects, but it *is* easy to understand the code.
    */
    public static double dihedral(Triple a, Triple b, Triple c, Triple d)
    {
        /* Schematic for naming points (uppercase) and vectors (lowercase).
           Shown is a dihedral of 0 degress, with imaginary vectors u and v.
           ABCD are all in a plane; u, v, and f are in a plane orthogonal to it.
        
            ^        ^
          v :        : u
            :        :
            C<-------B
         g /    f    /\
          /            \ e
        \/              \
        D                A
        
        */
        Triple e, f, g, u, v;
        e = b.dup().sub(a);
        f = c.dup().sub(b);
        g = d.dup().sub(c);
        u = e.dup().cross(f);
        v = f.dup().cross(g);
        
        double dihedral = u.angle(v);
        
        // Solve handedness problem:
        if(u.angle(g) > 90.0) dihedral = -dihedral;
        
        return dihedral;
    }
//}}}

//{{{ normalToPlane()
//##################################################################################################
    /**
    * Constructs a unit normal vector (relative to the origin) to plane ABC,
    * following the right-hand rule.
    */
    public static Triple normalToPlane(Triple a, Triple b, Triple c)
    {
        Triple one = b.dup().sub(a);
        Triple two = c.dup().sub(b);
        one.cross(two);
        one.unit();
        return one;
    }
//}}}

//{{{ equals(), hashCode()
//##################################################################################################
    /**
    * Obeys the general contract of Object.equals().
    * Two triples are equal if their x, y, and z coordinates are equal.
    */
    public boolean equals(Object o)
    {
        if(! (o instanceof Triple)) return false;
        else
        {
            Triple t = (Triple)o;
            if(x == t.x && y == t.y && z == t.z) return true;
            else return false;
        }
    }
    
    /**
    * Obeys the general contract of Object.hashCode().
    * Based on Double.hashCode().
    */
    public int hashCode()
    {
        long xb = Double.doubleToLongBits(x);
        long yb = Double.doubleToLongBits(y);
        long zb = Double.doubleToLongBits(z);
        long bits = xb ^ yb ^ zb;
        return (int)(bits ^ (bits >>> 32));
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}
}//class

