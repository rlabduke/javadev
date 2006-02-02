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
//}}}
/**
* <code>ProteinSecStructCrayon</code> colors ribbons by secondary structure.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb  2 14:14:06 EST 2006
*/
public class ProteinSecStructCrayon implements RibbonCrayon
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    SecondaryStructure secStruct;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ProteinSecStructCrayon(SecondaryStructure secStruct)
    {
        super();
        this.secStruct = secStruct;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
    public String colorRibbon(GuidePoint start, GuidePoint end, int interval, int nIntervals)
    {
        Residue r = (interval <= nIntervals/2 ? start.nextRes : end.prevRes);
             if(secStruct.isHelix(r))   return "width4 red";
        else if(secStruct.isStrand(r))  return "width4 green";
        else if(secStruct.isTurn(r))    return "sky";
        else if(secStruct.isCoil(r))    return "white";
        else                            return "magenta";
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

