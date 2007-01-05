// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports

package king.tool.rnaxtal;
import king.core.*;
import driftwood.r3.*;

public class RNATriple implements MutableTuple3 {

//{{{ Constants
    
//}}}

//{{{ Variable definitions
//##################################################################################################
    public double x;
    public double y;
    public double z;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Creates a triple with the specified x, y, and z coordinates. */
    public RNATriple(double x0, double y0, double z0)
    {
        x = x0;
        y = y0;
        z = z0;
    }

    /** Creates a triple at the origin. */
    public RNATriple()
    {
        x = y = z = 0.0;
    }

    public RNATriple(KPoint p) {
	x = p.getX();
	y = p.getY();
	z = p.getZ();
    }
//}}}

    public RNATriple add(KPoint t)
    {
        x += t.getX();
        y += t.getY();
        z += t.getZ();
        return this;
    }

    /** Multiplies (scales) this Triple by k. If k < 1, this vector is shortened; if k > 1, this vector is lengthened. */
    public RNATriple mult(double k)
    {
        x *= k;
        y *= k;
        z *= k;
        return this;
    }

    /**
    * Makes this vector one unit in length (magnitude) with the same directionality.
    * Returns (0, 0, 0) if this Triple is already (0, 0, 0).
    */
    public RNATriple unit()
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

    /** Returns the maginitude of a vector from the origin to this point. */
    public double mag()
    {
        return Math.sqrt(x*x + y*y + z*z);
    }

    /**
    * Obeys the general contract of Object.equals().
    * Two Tuple3's are equal if their x, y, and z coordinates are equal.
    */
    public boolean equals(Object o)
    {
        if(! (o instanceof RNATriple)) return false;
        else
        {
            RNATriple t = (RNATriple)o;
            if(x == t.getX() && y == t.getY() && z == t.getZ()) return true;
            else return false;
        }
    }

    public boolean equalCoords(Tuple3 tup) {
	if (x == tup.getX() && y ==  tup.getY() && z == tup.getZ()) return true;
	else return false;
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

    public Object clone() {
	RNATriple c = new RNATriple(x, y, z);
	return c;
    }
}
