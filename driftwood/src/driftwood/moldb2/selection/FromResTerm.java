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
import driftwood.r3.*;
//}}}
/**
* <code>FromResTerm</code> handles "fromres (SELECTION)" statements.
* This expands as set of atoms (SELECTION) to include all the atoms in those residues.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class FromResTerm extends Selection
{
//{{{ Constants
    static final private DecimalFormat df = new DecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##############################################################################
    Set         includedRes;
    Selection   childTerm;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FromResTerm(Selection target)
    {
        super();
        this.childTerm = target;
    }
//}}}

//{{{ init, selectImpl, toString
//##############################################################################
    public void init(Collection atomStates, CoordinateFile coordFile)
    {
        super.init(atomStates, coordFile);
        this.childTerm.init(atomStates, coordFile);
        // childTerm now fully initialized, doing selections should be safe
        this.includedRes = childTerm.selectResidues(atomStates);
    }
    
    /**
    * Returns true iff the given AtomState should belong to this selection.
    */
    protected boolean selectImpl(AtomState as)
    {
        return this.includedRes.contains( as.getResidue() );
    }
    
    public String toString()
    { return "fromres ("+childTerm+")"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

