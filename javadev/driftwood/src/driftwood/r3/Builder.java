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
* <code>Builder</code> is a utility class for doing geometrical
* constructions, like Mage's construct4 operation.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 13:46:26 EST 2003
*/
public class Builder //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Triple x1, x2;      // working triples
    Transform rot1;     // working rotation
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Builder()
    {
        x1      = new Triple();
        x2      = new Triple();
        rot1    = new Transform();
    }
//}}}

//{{{ construct4
//##################################################################################################
    /**
    * Given three points A, B, and C,
    * construct a line segment from C to D
    * of a given length,
    * at a given angle to BC,
    * and with a given dihedral angle to ABC.
    * @param ang the angle BCD in degrees, between 0 and 180
    * @param dihe the angle ABCD in degrees
    * @return the endpoint of the new line segment
    */
    public Triple construct4(Tuple3 a, Tuple3 b, Tuple3 c, double len, double ang, double dihe)
    {
        Triple d = new Triple().likeVector(c, b);
        d.unit().mult(len);
        
        // Not robust to a/b/c colinear
        // Doesn't matter since that makes dihe undef.
        x1.likeVector(b, a);
        x2.likeVector(b, c);
        x1.cross(x2);
        
        rot1.likeRotation(x1, ang);
        rot1.transformVector(d);
        
        rot1.likeRotation(x2, dihe);
        rot1.transformVector(d);
        
        return d.add(c);
    }
//}}}

//{{{ dock3on3
//##################################################################################################
    /**
    * Creates a transform that, if applied to the mobile object,
    * would superimpose the three specified points onto
    * three points in the reference object.
    * The primary point is perfectly superimposed,
    * the secondary point determines orientation (an axis),
    * and the tertiary point determines rotation about the axis.
    */
    public Transform dock3on3(Tuple3 ref1, Tuple3 ref2, Tuple3 ref3, Tuple3 mob1, Tuple3 mob2, Tuple3 mob3)
    {
        Transform dock = new Transform();
        
        // Translate to ref1
        x1.like(ref1).sub(mob1);
        rot1.likeTranslation(x1);
        dock.append(rot1);

        // Calc angle and do rotation
        dock.transform(mob2, x1);
        double angle = Triple.angle(ref2, ref1, x1);
        x2.likeNormal(ref2, ref1, x1).add(ref1);
        rot1.likeRotation(ref1, x2, angle);
        dock.append(rot1);
        
        // Calc dihedral and do rotation
        dock.transform(mob2, x1);
        dock.transform(mob3, x2);
        double dihedral = Triple.dihedral(ref3, ref1, x1, x2);
        rot1.likeRotation(ref1, x1, -dihedral);
        dock.append(rot1);
        
        return dock;        
    }
//}}}

//{{{ Andrew's explantion of checkTriangle
//##################################################################################################
/*<pre>
What the code does:
  When Engine.pickPoint() is called, if KPoint p is an instance of
  TrianglePoint, first check to see if all 3 points of the triangle
  can be found from p.  I.e. p.from and p.from.from are non-null.
  If we can get all three points of the triangle, then run
  checkTriangle().  If checkTriangle() says the point is inside the
  triangle, print the TriangleList's name out to stdout.

  This is a basic implementation with none of the preprocessing I
  mentioned before.  It's possible to save two operations in
  signedArea2() by computing and storing (bX-aX) and (bY-aY) for each
  edge of the triangle upon loading a file.  (see the code)

The formulas:
  See the comments in the code for the formulas.  Briefly, the
  signed area of a triangle can be computed by a 3x3 determinant.
  For triangle ABC in the plane and a given point p, we can determine
  if p is inside ABC by computing the signed area of triangles
  ABp, BCp, and CAp.  If all three signed areas are the same sign, then
  the point lies inside the triangle.  If any of the signed areas are
  equal to zero, then the point also lies inside the triangle --
  actually it lies on an edge of ABC.  Otherwise the point lies outside
  the triangle.

The underlying concept:
  This is the part I mentioned was a bit more involved.  I'll try to
  give an intuitive picture of the background info instead of going into
  too many details.  I'm not sure how much of what I write below you already
  know, so please bear with me if I say anything old hat.  Signed area comes
  from a relationship between the area of a region R bounded by a (piecewise
  smooth simple) closed curve C and a line integral along C.

  Think of a line integral is a generalization of the definite integral
  you encounter in calculus to multiple variables.  So instead of taking
  the integral of a function $y = f(x)$ defined in the interval [a,b],
  you take the integral of a function $z = f(x,y)$ which is defined on a
  curve C.  Visualize as follows: Suppose I am in R^3 and I am moving in
  the x-y plane.  My movement is dictated by travelling along curve C,
  which lives in the x-y plane.  As I move along C, I calculate f(x,y)
  for each point.  By doing this I've created a 2-dimensional "upright
  region."  The line integral is just the area of this region.  To calculate
  this you can do something analogous to calculating the definite integral.
  In the definite integral you break up the "area under the curve" into
  rectangular strips.  I.e. you break up [a,b] into small intervals (the
  width of the rectangular strip) and then choose some number w in each
  interval to calculate the function value f(w) (the height of the
  rectangular strip).  Take the area of each rectangular strip, and then
  sum them to get the definite integral.  With the line integral, you
  break up the curve C into small intervals or rather, small arcs, since
  it's a curve and do the same thing.

  Ok -- back to signed area.  This relationship is derived from Green's
  Theorem, which is a connection between a line integral for two continous
  functions defined along curve C and a surface integral for these two
  functions.  When I say defined along curve C, what I actually mean is
  that these two functions exist in a space which contains region R, but
  I solve them along curve C.  In addition, I travel along curve C in
  a particular direction, e.g. so that the region R is always on my left.
  Surface integral is also a generalization of the definite integral.
  Line integrals are solved along curves, surface integrals are solved
  along surfaces, so it's actually a double integral.  Green's Theorem
  says that instead of solving a line integral for these two functions,
  I can solve a surface integral which will give me the same result, and
  vice versa.

  Point being the following -- I can use Green's Theorem twice.  Once
  so that one function is 0, and the second function is x.  And once so
  that one function is -y, and the second function is 0.  By combining
  the two formulas I get from Green's Theorem, I get a formula for the
  area bounded by that closed curve, which is a pretty cool result.
  This is the signed area.  The sign comes from whether the region R
  ends up being on my left or right as I walk along the curve.  So
  from here you can probably see where this leads...

  For us the region R is triangle ABC, and the curve C are the edges.
  I want to figure out if a point p is inside ABC.  Let's walk along
  each of the edges in a particular direction.  Say we go from A -> B -> C.
  Then I can define A -> B -> p, B -> C -> p, and C -> A -> p as new
  triangles, and each one now has a defined walk along their boundary.
  As I walk along the boundary of a new triangle, take note of where
  the interior of the new triangle actually is -- if it's to my right or
  to my left.  If for a particular new triangle the region is to my
  left, but for the other new triangle the region is to my right, then
  I know the point p is outside of triangle ABC.  So in conclusion, just
  compute the signed areas of each new triangle.  By computing the
  signed area you have, in effect, "walked along a triangle" and seen
  whether the interior of a triangle is to your right or left as you
  walk.  And to conclude the signed area of a triangle can be
  represented as a determinant. :)

  Anyways, enough of my babbling.  I left out all of the formulas, but
  these can be found in a Calculus text or online somewhere like Mathworld.
  Green's Theorem is a specific case Stokes Theorem, and a lot of these
  sorts of things are very useful in physics and advanced calculus type
  things... complex analysis and the like.  I think I could have been
  less wordy, and perhaps there are simpler ways to describe what I've
  written above, but hopefully you've gotten a sense of where it all
  comes from.

-----

Oh...  I just realized this formulation is a lot easier.

 - take tangent vector at point p
 - draw second vector _perpendicular_ to tangent vector from point p
   towards region R.
 - cross product (== sign)

I have a tendency to make things too complicated on the first pass. :)
</pre>*/
//}}}

//{{{ signedArea2, checkTriangle
//##################################################################################################
    /**
    * Signed area for a (planar) triangle, or rather signed area for
    * parallelogram defined by directed edges ab and bc.  For signed
    * area of triangle, multiply the returned value by 0.5.
    *
    * The signed area allows us to determine whether or not c is
    * left, right, or on the directed edge ab if the determinant
<pre>
|a.x b.x c.x|
|a.y b.y c.y|
| 1   1   1 |
</pre>
    * is positive, negative, or zero, respectively.
    * <p>Code courtesy of Andrew Ban, 20 May 2004.
    */
    public static double signedArea2(double aX, double aY, double bX, double bY, double cX, double cY)
    { return ( (bX-aX)*(cY-aY) - (bY-aY)*(cX-aX) ); }
    
    /**
    * Tests whether the point (x,y) lies in/on the triangle ABC or not.
    * Checks signed area for triangle formed by two vertices of
    * Triangle ABC and a given point (x,y).
    * If all 3 signedArea2 tests are of one sign, or one of the tests
    * is zero (indicating that point p sits on one of the edges of
    * ABC), return true.  Else return false.
    * <p>Code courtesy of Andrew Ban, 20 May 2004.
    */
    public static boolean checkTriangle(double x, double y, double aX, double aY, double bX, double bY, double cX, double cY)
    {
        // walk AB -> BC -> CA
        double ABp, BCp, CAp;
        ABp = signedArea2(aX, aY, bX, bY, x, y);
        if (ABp == 0)
            return true;
        
        BCp = signedArea2(bX, bY, cX, cY, x, y);
        if (BCp == 0)
            return true;
        else if ( (ABp < 0) != (BCp < 0) )
            return false;
        
        CAp = signedArea2(cX, cY, aX, aY, x, y);
        if (CAp == 0)
            return true;
        else if ( (BCp < 0) != (CAp < 0) )
            return false;
        
        return true;
    }
//}}}

//{{{ makeDotSphere
//##############################################################################
// Ported to Java by Ian W. Davis, 2004-Dec-8
/* from probe.2.8.011009 and hacked for mage.6.28.030402 by dcr  */
/* author: J. Michael Word (port from dcr and mez fortran code)  */
/* date written: 2/20/96                                         */
/* purpose: generate points on a sphere                          */

/* NOTICE: This is free software and the source code is freely   */
/* available. You are free to redistribute or modify under the   */
/* conditions that (1) this notice is not removed or modified    */
/* in any way and (2) any modified versions of the program are   */
/* also available for free.                                      */
/*               ** Absolutely no Warranty **                    */
/* Copyright (C) 1999 J. Michael Word                            */

    /**
    * Creates a set of ~evenly distributed points on the surface of a sphere.
    * 
    * @param radius     the radius of the sphere
    * @param density    dot density in dots per square unit of sphere surface
    * @return a Collection of Triples representing dots on the sphere surface,
    *   with the sphere centered on the origin.
    */
    public Collection makeDotSphere(double radius, double density) 
    {
        // overestimate of the number of dots
        final double sizefact = 1.0;
        int estNumDots = (int)Math.floor(4.0 * Math.PI * density * sizefact * (radius * radius));
        
        Collection points = new ArrayList(estNumDots);
        
        // These are all just magic numbers to me...
        final double offset = 0.2;
        boolean odd = true;
        double ang = 5.0 * Math.PI / 360.0; // 2.5 degrees as radians?
        double cosang = Math.cos(ang);
        double sinang = Math.sin(ang);
        
        // More interpretable: number of dots along latitude, longitude (?)
        int nequator = (int)Math.floor(Math.sqrt(estNumDots * Math.PI));
        int nvert = nequator / 2;
        
        for (int j = 0; j <= nvert; j++) 
        {
            double phi  = (Math.PI * j) / nvert;
            double z0   = Math.cos(phi) * radius;
            double xy0  = Math.sin(phi) * radius;
            int nhoriz  = (int)Math.floor(nequator * Math.sin(phi));
            if(nhoriz < 1) nhoriz = 1;
            
            for (int k = 0; k < nhoriz; k++) 
            {
                double theta;
                if(odd) theta = (2.0 * Math.PI * k + offset)/nhoriz;
                else    theta = (2.0 * Math.PI * k         )/nhoriz;
                double x0 = Math.cos(theta) * xy0;
                double y0 = Math.sin(theta) * xy0;
                
                Triple t = new Triple(x0, y0*cosang - z0*sinang, y0*sinang + z0*cosang);
                points.add(t);
            }
            odd = !odd;
        }
        
        return points;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

