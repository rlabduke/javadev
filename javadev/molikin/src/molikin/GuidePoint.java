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

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

