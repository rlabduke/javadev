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

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

