// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.sc;

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
* <code>RotamerDef</code> is for storing the various details associated
* with a single rotameric conformation (for a particular type of side-chain).
* It exists primarily as a data structure for use by SidechainAngles.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May 15 12:58:31 EDT 2003
*/
public class RotamerDef //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    public String       rotamerName;
    public String       frequency = null; // allows for things like "<1%"
    public double[]     chiAngles;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public RotamerDef()
    {
    }
//}}}

//{{{ toString
//##################################################################################################
    public String toString()
    {
        if(frequency != null)
            return rotamerName+" - "+frequency;
        else
            return rotamerName;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

