// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2.selection;

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
* <code>SecondaryStructureTerm</code> handles "alpha" or "beta" secondary 
* structure selections.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Mar 15 2011
*/
public class SecondaryStructureTerm extends Selection
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String secStrucType;
    CoordinateFile coordFile;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SecondaryStructureTerm(String secStrucType)
    {
        super();
        this.secStrucType = secStrucType;
    }
//}}}

//{{{ init, selectImpl, toString
//##############################################################################
    //public void init(Collection atomStates)
    //{
    //    //super.init(atomStates); <-- don't do this!
    //    // For this type of Selection, wait for init(CoordinateFile)
    //    // so SecondaryStructure is accessible
    //}
    
    public void init(Collection atomStates, CoordinateFile coordFile)
    {
        super.init(atomStates, coordFile);
        this.coordFile = coordFile;
    }
    
    /**
    * Returns true iff the given AtomState should belong to this selection.
    */
    protected boolean selectImpl(AtomState as)
    {
        SecondaryStructure secStruc = coordFile.getSecondaryStructure();
        if(secStrucType.equals("alpha") && secStruc.isHelix(as.getResidue()))
            return true;
        if(secStrucType.equals("beta") && secStruc.isStrand(as.getResidue()))
            return true;
        return false;
    }
    
    public String toString()
    { return secStrucType; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

