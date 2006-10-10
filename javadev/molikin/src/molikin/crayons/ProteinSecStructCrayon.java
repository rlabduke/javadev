// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.crayons;
import molikin.*;

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
public class ProteinSecStructCrayon extends AbstractCrayon implements RibbonCrayon
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    SecondaryStructure secStruct;
    int width = 0;
    String color = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ProteinSecStructCrayon(SecondaryStructure secStruct)
    {
        super();
        this.secStruct = secStruct;
    }
//}}}

//{{{ colorRibbon
//##############################################################################
    public void forRibbon(GuidePoint start, GuidePoint end, int interval, int nIntervals)
    {
        Residue r = (interval <= nIntervals/2 ? start.nextRes : end.prevRes);
             if(secStruct.isHelix(r))   { width = 4; color = "red"; }
        else if(secStruct.isStrand(r))  { width = 4; color = "green"; }
        else if(secStruct.isTurn(r))    { width = 0; color = "sky"; }
        else if(secStruct.isCoil(r))    { width = 0; color = "white"; }
        else                            { width = 0; color = "magenta"; }
    }
//}}}

//{{{ getColor, getWidth
//##############################################################################
    public String getColor()
    { return color; }
    
    public int getWidth()
    { return width; }
//}}}
}//class

