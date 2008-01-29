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
* <code>SheetAxes3</code> is a data structure for describing a local
* (residue specific) Cartesian coordinate system on a beta sheet,
* relative to the strand direction and sheet normal vectors.
* It works on a pair of planes, but (difference from SheetAxes2) it deals with
* along- instead of across-strand planes.
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
public class SheetAxes3 //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The unit vector sum of the "concavified" normal1, normal2. Kinda 
    * temporary -- i.e., not part of the coord system defined here. */
    public Triple normal;
    
    /** The pseudo-X axis, roughly along the strand from N-term to C-term. */
    public Triple strand;
    
    /** The pseudo-Y axis, roughly cross-strand. */
    public Triple cross;
    
    /** The pseudo-Z axis. Equals along x cross. Its up-or-down direction is 
    * irrelevant since CaCb and the normals' sum will be defined in the same 
    * coord system, i.e. relative to zAxis, so comparisons btw them are valid. */
    public Triple zAxis;
    
    /** Angular deviation of the CaCb vector from the along-strand concavity vector, 
    * in degrees. 
    * This angle indicates whether CaCb points toward the concave or convex side
    * of the sheet i.t.o. the Nward-plane-centroid-to-Cward-plane-centroid axis 
    * (i.e. the strands could be perfectly lined up from a profile view but could
    * themselves "bend").
    * > 90: convex, < 90: concave. */
    public double cacb_alongConcavity;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SheetAxes3(Triple normalSum, Triple btwCentroids, Tuple3 closeToZAxis, Tuple3 fromCaToCb)
    {
        super();
        
        this.normal    = normalSum; // kinda temporary
        
        this.strand     = new Triple(btwCentroids).unit();
        this.cross     = new Triple().likeCross(closeToZAxis, strand).unit();
        this.zAxis     = new Triple().likeCross(strand, cross).unit(); // similar to closeToZAxis
        
        // Note: working in 2D pseudo-XZ plane from here on!
        
        double normalDotStrand = strand.dot(normal);
        double normalDotZ      = zAxis.dot(normal);
        Triple normalInXZ      = new Triple(normalDotStrand, 0, normalDotZ);
        // ^^ the along-strand "concavity vector"
        
        double cacbDotStrand   = strand.dot(fromCaToCb);
        double cacbDotZ        = zAxis.dot(fromCaToCb);
        Triple cacbInXZ        = new Triple(cacbDotStrand, 0, cacbDotZ);
        
        cacb_alongConcavity = normalInXZ.angle(cacbInXZ);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

