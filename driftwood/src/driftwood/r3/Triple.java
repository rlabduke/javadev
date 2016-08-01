// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.r3;

//import java.awt.*;
import java.io.*;
//import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import gnu.regexp.*;
//}}}
/**
* <code>Triple</code> represents either a point in 3-space or a vector in 3-space.
* This will no doubt confuse some people, but it works for me.
* Functions that take Triples as parameters should make it clear when they are
* interpretted as vectors and when they are interpretted as points.
*
* <p>Triples are <em>mutable</em>, so their values may change over time.
* In particular, all of the mathematical operations (add, sub, scale, cross, etc.) alter
* the coordinates of the calling object (but not those of the parameters).
* Most functions that return a Triple merely return <em>this</em> Triple, but thus
* allow for convenient "chaining" of function calls.
*
* <p>Warning! Some functions that take parameters (Tuple3, etc) are NOT safe to call
* with <code>this</code> as a parameter. Those that are safe are marked as such.
*
* <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep  9 09:33:59 EDT 2002
*/
public class Triple implements MutableTuple3, Serializable
{
//{{{ Variable definitions
//##################################################################################################
    public double x;
    public double y;
    public double z;
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
    public Triple(Tuple3 t)
    {
        x = t.getX();
        y = t.getY();
        z = t.getZ();
    }
    
    /** Creates a triple at the origin. */
    public Triple()
    {
        x = y = z = 0.0;
    }
//}}}

//{{{ like
//##################################################################################################
    /** Assigns the coordinates of t to this Triple. A null value leaves coordinates unchanged. */
    public Triple like(Tuple3 t)
    {
        if(t != null)
        {
            x = t.getX();
            y = t.getY();
            z = t.getZ();
        }
        return this;
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
    
    /** Assigns a value to the first element of this tuple */
    public void setX(double x0) { x = x0; }
    /** Assigns a value to the second element of this tuple */
    public void setY(double y0) { y = y0; }
    /** Assigns a value to the third element of this tuple */
    public void setZ(double z0) { z = z0; }
    
    /** Assigns a value to all the elements of this tuple */
    public void setXYZ(double x0, double y0, double z0)
    {
        x = x0;
        y = y0;
        z = z0;
    }
//}}}

//{{{ dot, cross, likeCross
//##################################################################################################
    /**
    * Returns the vector dot product of this triple and v.
    * The dot product of A and B, A.B, is equal to |A||B|cos(theta),
    * where theta is the angle between vectors from the origin to A and B.
    */
    public double dot(Tuple3 v)
    {
        // Dot product: d . e = d e cos(a) = dxex + dyey + dzez
        return x*v.getX() + y*v.getY() + z*v.getZ();
    }
    
    /**
    * Assigns the vector cross product of this triple and v to this.
    * The cross product of A and B, AxB, is orthogonal to the plane defined by vectors
    * from the origin to A and B. Its direction (sign) is given by the right-hand rule.
    */
    public Triple cross(Tuple3 v)
    {
        // Cross product: a x b = (aybz-azby, -axbz+azbx, axby-aybx)
        double x0 = y*v.getZ() - z*v.getY();
        double y0 = z*v.getX() - x*v.getZ();
        double z0 = x*v.getY() - y*v.getX();
        x = x0;
        y = y0;
        z = z0;
        return this;
    }
    
    /**
    * Assigns the vector cross product of v1 and v2 to this: this = v1 x v2
    * The cross product of A and B, AxB, is orthogonal to the plane defined by vectors
    * from the origin to A and B. Its direction (sign) is given by the right-hand rule.
    * Safe to execute on <code>this</code>.
    */
    public Triple likeCross(Tuple3 v1, Tuple3 v2)
    {
        // Cross product: a x b = (aybz-azby, -axbz+azbx, axby-aybx)
        double x0 = v1.getY()*v2.getZ() - v1.getZ()*v2.getY();
        double y0 = v1.getZ()*v2.getX() - v1.getX()*v2.getZ();
        double z0 = v1.getX()*v2.getY() - v1.getY()*v2.getX();
        x = x0;
        y = y0;
        z = z0;
        return this;
    }
//}}}

//{{{ mag, mag2, unit
//##################################################################################################
    /** Returns the maginitude of a vector from the origin to this point. */
    public double mag()
    {
        return Math.sqrt(x*x + y*y + z*z);
    }
    
    /**
    * Returns the squared maginitude of a vector from the origin to this point.
    * This is equivalent to the dot product of the vector with itself.
    */
    public double mag2()
    {
        return x*x + y*y + z*z;
    }
    
    /**
    * Returns the maginitude of a vector from the origin to this point.
    * Safe to execute on <code>this</code>.
    */
    static public double mag(Tuple3 t)
    {
        double x0 = t.getX();
        double y0 = t.getY();
        double z0 = t.getZ();
        return Math.sqrt(x0*x0 + y0*y0 + z0*z0);
    }
    
    /**
    * Makes this vector one unit in length (magnitude) with the same directionality.
    * Returns (0, 0, 0) if this Triple is already (0, 0, 0).
    */
    public Triple unit()
    {
        double mag = this.mag();
        if(mag != 0.0)
        {
            x /= mag;
            y /= mag;
            z /= mag;
        }
        return this;
    }
//}}}

//{{{ mult, div, likeProd, likeQuot
//##################################################################################################
    /** Multiplies (scales) this Triple by k. If k < 1, this vector is shortened; if k > 1, this vector is lengthened. */
    public Triple mult(double k)
    {
        x *= k;
        y *= k;
        z *= k;
        return this;
    }
    
    /** Divides this Triple by k. */
    public Triple div(double k)
    {
        x /= k;
        y /= k;
        z /= k;
        return this;
    }
    
    /**
    * Makes this vector a scaled version of v: this = k*v = [k*vx, k*vy, k*vz].
    * Safe to execute on <code>this</code>.
    */
    public Triple likeProd(double k, Tuple3 v)
    {
        x = k*v.getX();
        y = k*v.getY();
        z = k*v.getZ();
        return this;
    }
    
    /**
    * Makes this vector a scaled version of v: this = v/k = [vx/k, vy/k, vz/k].
    * Safe to execute on <code>this</code>.
    */
    public Triple likeQuot(double k, Tuple3 v)
    {
        x = v.getX()/k;
        y = v.getY()/k;
        z = v.getZ()/k;
        return this;
    }
//}}}

//{{{ add, likeSum, sub, likeDiff, neg
//##################################################################################################
    /**
    * Adds t to this Triple.
    * Safe to execute on <code>this</code>.
    */
    public Triple add(Tuple3 t)
    {
        x += t.getX();
        y += t.getY();
        z += t.getZ();
        return this;
    }
    
    /**
    * Assigns the sum of t1 and t2 to this Triple.
    * Safe to execute on <code>this</code>.
    */
    public Triple likeSum(Tuple3 t1, Tuple3 t2)
    {
        x = t1.getX() + t2.getX();
        y = t1.getY() + t2.getY();
        z = t1.getZ() + t2.getZ();
        return this;
    }
    
    /**
    * Subtracts t from this Triple.
    * Safe to execute on <code>this</code>.
    */
    public Triple sub(Tuple3 t)
    {
        x -= t.getX();
        y -= t.getY();
        z -= t.getZ();
        return this;
    }
    
    /**
    * Assigns the difference of t1 and t2 to this Triple: this = t1 - t2
    * Safe to execute on <code>this</code>.
    */
    public Triple likeDiff(Tuple3 t1, Tuple3 t2)
    {
        x = t1.getX() - t2.getX();
        y = t1.getY() - t2.getY();
        z = t1.getZ() - t2.getZ();
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
//}}}
    
//{{{ addMult
//##################################################################################################
    /**
    * Adds k*t to this Triple.
    * Safe to execute on <code>this</code>.
    */
    public Triple addMult(double k, Tuple3 t)
    {
        x += k * t.getX();
        y += k * t.getY();
        z += k * t.getZ();
        return this;
    }
//}}}

//{{{ distance, sqDistance
//##################################################################################################
    /**
    * Returns the distance between this triple and t.
    * Safe to execute on <code>this</code>.
    */
    public double distance(Tuple3 t)
    {
        double dx, dy, dz;
        dx = x - t.getX();
        dy = y - t.getY();
        dz = z - t.getZ();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
    * Returns the distance between s and t.
    * Safe to execute on <code>this</code>.
    */
    public static double distance(Tuple3 s, Tuple3 t)
    {
        double dx, dy, dz;
        dx = s.getX() - t.getX();
        dy = s.getY() - t.getY();
        dz = s.getZ() - t.getZ();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
    * Returns the square of the distance between this triple and t. Faster than distance().
    * Safe to execute on <code>this</code>.
    */
    public double sqDistance(Tuple3 t)
    {
        double dx, dy, dz;
        dx = x - t.getX();
        dy = y - t.getY();
        dz = z - t.getZ();
        return (dx*dx + dy*dy + dz*dz);
    }

    /**
    * Returns the square of the distance between s and t. Faster than distance().
    * Safe to execute on <code>this</code>.
    */
    public static double sqDistance(Tuple3 s, Tuple3 t)
    {
        double dx, dy, dz;
        dx = s.getX() - t.getX();
        dy = s.getY() - t.getY();
        dz = s.getZ() - t.getZ();
        return (dx*dx + dy*dy + dz*dz);
    }
//}}}

//{{{ likeMidpoint, angle
//##################################################################################################
    /**
    * Assigns this point as half way between these triples
    * Safe to execute on <code>this</code>.
    */
    public Triple likeMidpoint(Tuple3 t1, Tuple3 t2)
    {
        x = (t1.getX() + t2.getX()) / 2.0;
        y = (t1.getY() + t2.getY()) / 2.0;
        z = (t1.getZ() + t2.getZ()) / 2.0;
        return this;
    }

    /**
    * Finds the angle in degrees between two vectors (with tails at the origin)
    * Safe to execute on <code>this</code>.
    */
    public double angle(Tuple3 t)
    {
        // acos returns NaN sometimes when we're
        // too close to an angle of 0 or 180 (if |dot/mag| > 1)
        double dot = this.dot(t);
        double ret = Math.toDegrees(Math.acos(  dot / (this.mag() * mag(t))  ));
        if(Double.isNaN(ret)) ret = (dot>=0.0 ? 0.0 : 180.0);
        return ret;
    }
    
    /**
    * Returns the angle ABC, in degrees, from 0 to 180.
    * Safe to execute on <code>this</code>.
    */
    public static double angle(Tuple3 a, Tuple3 b, Tuple3 c)
    {
        // Too expensive!
        //Triple ba = new Triple().likeDiff(a, b), bc = new Triple().likeDiff(c, b);
        //return Math.toDegrees(Math.acos(  ba.dot(bc) / (ba.mag() * bc.mag())  ));
        
        // Dot product: d . e = d e cos(a) = dxex + dyey + dzez
        double dot, mag;
        double dx, dy, dz;
        double ex, ey, ez;
        dx = a.getX() - b.getX(); dy = a.getY() - b.getY(); dz = a.getZ() - b.getZ();
        ex = c.getX() - b.getX(); ey = c.getY() - b.getY(); ez = c.getZ() - b.getZ();
        dot = dx*ex + dy*ey + dz*ez;
        mag = Math.sqrt((dx*dx + dy*dy + dz*dz) * (ex*ex + ey*ey + ez*ez));
        
        // acos returns NaN sometimes when we're
        // too close to an angle of 0 or 180 (if |dot/mag| > 1)
        double ret = Math.toDegrees(Math.acos( dot / mag ));
        if(Double.isNaN(ret)) ret = (dot>=0.0 ? 0.0 : 180.0);
        return ret;
    }
//}}}

//{{{ dihedral
//##################################################################################################
    /**
    * Returns the dihedral ABCD, in degrees, from -180 to 180.
    * Safe to execute on <code>this</code>.
    */
    public static double dihedral(Tuple3 a, Tuple3 b, Tuple3 c, Tuple3 d)
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
        /* This is too expensive in terms of creating objects, as it creates
           3 new Triples, but it *is* easy to understand the code.
           
        Triple e, f, g, u, v;
        e = new Triple().likeDiff(b, a);
        f = new Triple().likeDiff(c, b);
        g = new Triple().likeDiff(d, c);
        u = e.cross(f); // overwrite 'e' b/c we don't need it anymore
        v = f.cross(g); // overwrite 'f' b/c we don't need it anymore
        
        double dihedral = u.angle(v);
        
        // Solve handedness problem:
        if(u.angle(g) > 90.0) dihedral = -dihedral;
        
        return dihedral;
        */
        
        //e = new Triple().likeDiff(b, a);
        double ex, ey, ez;
        ex = b.getX() - a.getX();     ey = b.getY() - a.getY();     ez = b.getZ() - a.getZ();
        //f = new Triple().likeDiff(c, b);
        double fx, fy, fz;
        fx = c.getX() - b.getX();     fy = c.getY() - b.getY();     fz = c.getZ() - b.getZ();
        //g = new Triple().likeDiff(d, c);
        double gx, gy, gz;
        gx = d.getX() - c.getX();     gy = d.getY() - c.getY();     gz = d.getZ() - c.getZ();
        
        // Cross product: a x b = (aybz-azby, -axbz+azbx, axby-aybx)
        //u = e.cross(f);
        double ux, uy, uz;
        ux = ey*fz - ez*fy;
        uy = ez*fx - ex*fz;
        uz = ex*fy - ey*fx;
        //v = f.cross(g);
        double vx, vy, vz;
        vx = fy*gz - fz*gy;
        vy = fz*gx - fx*gz;
        vz = fx*gy - fy*gx;
        
        // Dot product: d . e = d e cos(a) = dxex + dyey + dzez
        //double dihedral = u.angle(v);
        double dihedral, dot, mag;
        dot = ux*vx + uy*vy + uz*vz;
        mag = Math.sqrt((ux*ux + uy*uy + uz*uz) * (vx*vx + vy*vy + vz*vz));
        dihedral = Math.toDegrees(Math.acos( dot / mag));
        
        if(Double.isNaN(dihedral))
        {
            // acos returns NaN sometimes when we're
            // too close to an angle of 0 or 180 (if |dot/mag| > 1)
            dihedral = (dot>=0.0 ? 0.0 : 180.0);
        }
        else
        {
            // Solve handedness problem:
            //if(u.angle(g) > 90.0) dihedral = -dihedral;
            dot = ux*gx + uy*gy + uz*gz;
            mag = Math.sqrt((ux*ux + uy*uy + uz*uz) * (gx*gx + gy*gy + gz*gz));
            if(Math.toDegrees(Math.acos( dot / mag)) > 90.0) dihedral = -dihedral;
        }
        
        return dihedral;
    }
//}}}

//{{{ likeNormal, likeVector
//##################################################################################################
    /**
    * Assigns this as the vector from one point to another.
    * Equivalent to likeDiff(ptTo, ptFrom)
    * Safe to execute on <code>this</code>.
    */
    public Triple likeVector(Tuple3 ptFrom, Tuple3 ptTo)
    {
        x = ptTo.getX() - ptFrom.getX();
        y = ptTo.getY() - ptFrom.getY();
        z = ptTo.getZ() - ptFrom.getZ();
        return this;
    }
    /**
    * Constructs a unit normal vector (relative to the origin) to plane ABC,
    * following the right-hand rule.
    * If a, b, and c are colinear, it produces a random vector orthogonal to the line.
    */
    public Triple likeNormal(Tuple3 a, Tuple3 b, Tuple3 c)
    {
        double ax, ay, az, bx, by, bz, cx, cy, cz;
        ax = a.getX(); ay = a.getY(); az = a.getZ();
        bx = b.getX(); by = b.getY(); bz = b.getZ();
        cx = c.getX(); cy = c.getY(); cz = c.getZ();
        
        // Convert points to vectors
        cx -= bx; cy -= by; cz -= bz;
        bx -= ax; by -= ay; bz -= az;
        
        // Do cross product
        this.x = by*cz - bz*cy;
        this.y = bz*cx - bx*cz;
        this.z = bx*cy - by*cx;
        this.unit();
        
        // Look for a zero vector -- happens if a,b,c are colinear
        if(Math.abs(this.mag2() - 1.0) > 1e-10)
        {
            //System.err.println("driftwood.r3.Triple.likeNormal(): a/b/c nearly colinear");
            this.setXYZ(bx, by, bz);
            this.likeOrthogonal(this);
        }
        
        return this;
    }
//}}}

//{{{ likeProjection
//##################################################################################################
    /**
    * Assigns this as the projection of the first point
    * onto the vector from the second point to the third point.
    * Safe to execute on <code>this</code>.
    */
    public Triple likeProjection(Tuple3 ptDrop, Tuple3 ptFrom, Tuple3 ptTo)
    {
        // Draw appropriate vectors
        Triple start_stop = new Triple().likeVector(ptFrom, ptTo);
        Triple start_drop = new Triple().likeVector(ptFrom, ptDrop);
        
        // Get distance from the start point to the intersection point 
        // of the start->stop line and the perpendicular line
        double dist_start_corner = start_stop.dot(start_drop) / start_stop.mag();
        
        // Move along the start->stop line by that amount to the "corner"
        Triple start_corner = new Triple(start_stop).unit().mult(dist_start_corner);
        Triple corner = new Triple().likeSum(ptFrom, start_corner);
        
        // Return modified version of this object
        this.setXYZ(corner.getX(), corner.getY(), corner.getZ());
        return this;
        
        //{{{ Alternate, parametric method via Chris "'Topher" Williams:
        /*
        Wanna know b2, projection of b1 onto vector from a1 to a2.
        
        a1 = ptFrom
        a2 = ptTo
        A  = [a2x-a1x     , a2y-a1y     , a2z-a1z]
        
        b1 = ptDrop
        b2 = [a1x+Ax*t    , a1y+Ay*t    , a1z+Az*t]
        B  = [a1x+Ax*t-b1x, a1y+Ay*t-b1y, a1z+Az*t-b1z]
        
        Set A dot B = 0, then solve for t, the parametric variable.
        
        Then, b2 = a1+A*t
        
        Directly copied 'n' pasted code:
        
        def PerpToLine(a1, a2, b1):
          #Find the slope of line A in each direction, A is in vector notation
          A = [a1[0]-a2[0], a1[1]-a2[1], a1[2]-a2[2]]
         
          #Solve the parametric equations . . .
          t = (A[0]*(b1[0]-a1[0]) + A[1]*(b1[1]-a1[1]) + A[2]*(b1[2]-a1[2])) / ((A[0]**2)+(A[1]**2)+(A[2]**2))
         
          # . . . and use the result to find the new point on the line
          b2 = [a1[0]+A[0]*t, a1[1]+A[1]*t, a1[2]+A[2]*t]
         
          #Find the distance to that point
          #distance = math.sqrt((b1[0]-b2[0])**2+(b1[1]-b2[1])**2+(b1[2]-b2[2])**2)
          
          return b2
        
        FYI!
        */
        //}}}
    }
//}}}

//{{{ likeOrthogonal + test
//##################################################################################################
    /**
    * Makes this vector a unit vector orthogonal to the given vector,
    * although the orientation is pseudo-random.
    *
    * <p>FWIW, likeOrthogonal(v) == likeOrthogonal(v.neg())
    *
    * Safe to execute on <code>this</code>.
    */
    public Triple likeOrthogonal(Tuple3 v)
    {
        double a = v.getX();
        double b = v.getY();
        double c = v.getZ();
        double aa = a*a;
        double bb = b*b;
        double cc = c*c;
        
        if(aa >= bb && aa >= cc) // a is biggest
        {
            if(bb >= cc) // b is second biggest but maybe 0
            {
                this.z = 0;
                this.y = Math.sqrt( 1.0 / (1.0+bb/aa) );
                this.x = -b / a * this.y;
            }
            else // c is second biggest but maybe 0
            {
                this.y = 0;
                this.z = Math.sqrt( 1.0 / (1.0+cc/aa) );
                this.x = -c / a * this.z;
            }
        }
        else if(bb >= aa && bb >= cc) // b is biggest
        {
            if(aa >= cc) // a is second biggest but maybe 0
            {
                this.z = 0;
                this.x = Math.sqrt( 1.0 / (1.0+aa/bb) );
                this.y = -a / b * this.x;
            }
            else // c is second biggest but maybe 0
            {
                this.x = 0;
                this.z = Math.sqrt( 1.0 / (1.0+cc/bb) );
                this.y = -c / b * this.z;
            }
        }
        else //(cc >= aa && cc >= bb) // c is biggest
        {
            if(aa >= bb) // a is second biggest but maybe 0
            {
                this.y = 0;
                this.x = Math.sqrt( 1.0 / (1.0+aa/cc) );
                this.z = -a / c * this.x;
            }
            else // b is second biggest but maybe 0
            {
                this.x = 0;
                this.y = Math.sqrt( 1.0 / (1.0+bb/cc) );
                this.z = -b / c * this.y;
            }
        }
        return this;
    }
    
    /* For testing likeOrthogonal() * /
    public static void main(String[] args)
    {
        Triple u = new Triple(), v = new Triple();
        double maxdelta = 0;
        for(int i = 0; i < 100000; i++)
        {
            u.setXYZ(Math.random(), Math.random(), Math.random());
            u.mult(1000*Math.random());
            v.likeOrthogonal(u);
            double angle = u.angle(v);
            double delta = Math.abs(angle-90);
            maxdelta = Math.max(maxdelta, delta);
            if(delta > 1e-10)
                System.err.println(angle+": u="+u+"; v="+v);
        }
        System.err.println("Maximum deviation from orthogonality = "+maxdelta+" degrees.");
    }
    /* For testing likeOrthogonal() */
//}}}

//{{{ isNaN, equals, hashCode, toString
//##################################################################################################
    /** Returns <code>true</code> iff one or more component is Not-A-Number */
    public boolean isNaN()
    { return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z); }
    
    /**
    * Obeys the general contract of Object.equals().
    * Two Tuple3's are equal if their x, y, and z coordinates are equal.
    */
    public boolean equals(Object o)
    {
        if(! (o instanceof Tuple3)) return false;
        else
        {
            Tuple3 t = (Tuple3)o;
            if(x == t.getX() && y == t.getY() && z == t.getZ()) return true;
            else return false;
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
        // The rotation of bits is my own idea
        return (b1 ^ (b2<<11 | b2>>>21) ^ (b3<<22 | b3>>>10));
    }
    
    /**
    * Prints (x, y, z)
    */
    public String toString()
    {
        return "("+x+", "+y+", "+z+")";
    }
//}}}

//{{{ format
//##################################################################################################
    /**
    * Formats the triple using the supplied DecimalFormat object,
    * with the string <code>sep</code> between X and Y, and between Y and Z.
    */
    public String format(DecimalFormat df, String sep)
    {
        return df.format(this.getX())
            + sep
            + df.format(this.getY())
            + sep
            + df.format(this.getZ());
    }
    
    /**
    * Formats the triple using the supplied DecimalFormat
    * with a single space as the separator.
    */
    public String format(DecimalFormat df)
    {
        return this.format(df, " ");
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

