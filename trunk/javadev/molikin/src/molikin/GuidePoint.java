// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>GuidePoint</code> is a spline guide point for constructing ribbons.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 23 13:10:50 EST 2006
*/
public class GuidePoint //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Final coordinates for this guide point, after any correction for local curvature */
    public Triple xyz = new Triple();
    /** Unit vector normal to the local plane of the ribbon */
    public Triple cvec = new Triple();
    /** Unit vector in the local plane of the ribbon, perpendicular to its overall direction */
    public Triple dvec = new Triple();
    /**
    * Offset factor needed to pull spline through guidepoints.
    * Already applied; does not depend on neighbors (unlike width).
    */
    public double offsetFactor = 0;
    /**
    * Suggested ribbon width modifier, from 0 (skinniest) to 1 (fattest).
    * This is based on low/high curvature of 3+ residues in a row.
    */
    public double widthFactor = 0;
    /**
    * Residues immediately before and after this guidepoint (never null).
    * Usually different residues for proteins and for nucleic acids, except at chain ends.
    * For nucleic acids, prevRes is the residue centered on the guide point and nextRes is the one after.
    * (Protein guides fall between residues; nucleic guides fall in middle of a residue.)
    */
    public Residue prevRes, nextRes;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public GuidePoint()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

