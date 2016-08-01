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
* <code>SheetAxes</code> is a data structure for describing a local
* (residue specific) Cartesian coordinate system on a beta sheet,
* relative to the strand direction and sheet normal vectors.
*
* <p>Known bugs/problems:
* <ul>
* <li>Need a better H-bond potential: this one lets the C and N interact well</li>
* <li>For cross-strand neighbors, along and across numbers don't correlate b/c
*   the parallel / antiparallel relationship should affect the sign of the angle!</li>
* </ul>
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Mar 31 17:38:10 EST 2004
*/
public class SheetAxes //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The pseudo-X axis, along the strand from N-term to C-term. Equals cross x normal. */
    public Triple strand;
    /** The pseudo-Y axis, across the strands. Equals normal x CaCb. */
    public Triple cross;
    /** The pseudo-Z axis, the least-squares-fit sheet normal. */
    public Triple normal;
    /** Projections of the Ca-&gt;Cb vector onto the axes (dot product). */
    double dotStrand, dotCross, dotNormal;
    /** Angular deviation of the Ca-Cb vector from the sheet normal, in degrees. */
    public double angleAlong, angleAcross, angleNormal;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SheetAxes(Tuple3 sheetNormal, Tuple3 strandNtoC, Tuple3 fromCaToCb)
    {
        super();
        this.normal     = new Triple(sheetNormal).unit();
        this.cross      = new Triple().likeCross(normal, strandNtoC).unit();
        this.strand     = new Triple().likeCross(cross, normal).unit(); // similar to strandNtoC
        
        dotStrand   = strand.dot(fromCaToCb);
        dotCross    = cross.dot(fromCaToCb);
        dotNormal   = normal.dot(fromCaToCb);
        
        angleNormal = normal.angle(fromCaToCb);
        angleAlong = Math.toDegrees(Math.atan2(dotStrand, dotNormal));
        angleAcross = Math.toDegrees(Math.atan2(dotCross, dotNormal));
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

