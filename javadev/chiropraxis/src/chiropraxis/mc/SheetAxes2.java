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
//}}}
/**
* <code>SheetAxes2</code> is a data structure for describing a local
* (residue specific) Cartesian coordinate system on a beta sheet,
* relative to the strand direction and sheet normal vectors.
* It works on a pair of planes, each defined using the central strand and one
* of its neighbors, instead of a single plane defined by the central strand and
* both its neighboring strands at once.
*
* <p>Known bugs/problems:
* <ul>
* <li>Need a better H-bond potential: this one lets the C and N interact well</li>
* <li>For cross-strand neighbors, along and across numbers don't correlate b/c
*   the parallel / antiparallel relationship should affect the sign of the angle!</li>
* </ul>
*
* <p>Copyright (C) 2007 by Daniel A. Keedy. All rights reserved.
*/
public class SheetAxes2 //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The unit vector sum of the "concavified" normal1, normal2. Kinda 
    * temporary -- i.e., not part of the coord system defined here. */
    public Triple normal;
    
    /** The pseudo-X axis, along the strand from N-term to C-term. */
    public Triple strand;
    
    /** The pseudo-Z axis. Equals strand x btwCentroids. */
    public Triple zAxis;
    
    /** The pseudo-Y axis, similar to the vector btw the two planes' centroids.
    * Equals zAxis x strand. */
    public Triple cross;
    
    /** Angular deviation of the CaCb vector from the across-strand concavity vector, 
    * in degrees. 
    * This angle indicates whether CaCb points toward the concave or convex side
    * of the sheet i.t.o. the plane-centroid-to-plane-centroid axis (i.e. the 
    * strands could be completely straight, but rotated relative to each other 
    * like a barrel). > 90: convex, < 90: concave. */
    public double cacb_acrossConcavity;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SheetAxes2(Triple normalSum, Triple btwCentroids, Tuple3 strandNtoC, Tuple3 fromCaToCb)
    {
        super();
        
        this.normal     = normalSum; // kinda temporary
        
        this.strand     = new Triple(strandNtoC).unit();
        this.zAxis      = new Triple().likeCross(strand, btwCentroids).unit();
        this.cross      = new Triple().likeCross(zAxis, strand).unit(); // similar to btwCentroids
        
        // Note: working in 2D pseudo-YZ plane from here on!
        
        double normalDotCross = cross.dot(normal);
        double normalDotZ     = zAxis.dot(normal);
        Triple normalInYZ     = new Triple(0, normalDotCross, normalDotZ);
        // ^^ the across-strand "concavity vector"
        
        double cacbDotCross   = cross.dot(fromCaToCb);
        double cacbDotZ       = zAxis.dot(fromCaToCb);
        Triple cacbInYZ       = new Triple(0, cacbDotCross, cacbDotZ);
        
        cacb_acrossConcavity = normalInYZ.angle(cacbInYZ);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

