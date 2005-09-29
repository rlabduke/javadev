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
//import driftwood.*;
//}}}
/**
* <code>BondClassifier</code> generates standard sets of bonds used in kinemage
* drawing based on the divisions in AtomClassifier: bonds to/from H vs. bonds
* between heavy atoms; bonds within backbone vs. bonds to/from sidechains;
* and bonds within biopolymers vs. bonds to/from hets.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Sep 29 16:39:05 EDT 2005
*/
public class BondClassifier //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    public Collection bbBonds;
    public Collection bbhyBonds;
    public Collection bbscBonds;    // might be supplanted by stubs to ribbons
    public Collection scBonds;
    public Collection schyBonds;
    
    public Collection htBonds;
    public Collection hthyBonds;
    public Collection htbioBonds;   // connections to biopolymers
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BondClassifier(Collection allBonds, AtomClassifier ac)
    {
        super();
        this.bbBonds        = Util.selectBondsBetween(allBonds, ac.bbHeavy, ac.bbHeavy);
        this.bbhyBonds      = Util.selectBondsBetween(allBonds, ac.bbHeavy, ac.bbHydro);
        this.bbscBonds      = Util.selectBondsBetween(allBonds, ac.bbHeavy, ac.scHeavy);
        this.scBonds        = Util.selectBondsBetween(allBonds, ac.scHeavy, ac.scHeavy);
        this.schyBonds      = Util.selectBondsBetween(allBonds, ac.scHeavy, ac.scHydro);
        
        this.htBonds        = Util.selectBondsBetween(allBonds, ac.hetHeavy, ac.hetHeavy);
        this.hthyBonds      = Util.selectBondsBetween(allBonds, ac.hetHeavy, ac.hetHydro);
        this.htbioBonds     = Util.selectBondsBetween(allBonds, ac.hetHeavy, ac.bbHeavy);
        this.htbioBonds.addAll(Util.selectBondsBetween(allBonds, ac.hetHeavy, ac.scHeavy));
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

