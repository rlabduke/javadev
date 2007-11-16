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
* <code>ResRangeTerm</code> handles "INT-INT" statements.
* This expands as set of atoms (SELECTION) to include all the atoms in those residues.
* 
* This class evolved (directed evolution?) from FromResTerm under the supervision of 
* dak (started 10/22/07)
* 
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class ResRangeTerm extends Selection
{
//{{{ Constants
    static final private DecimalFormat df = new DecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##############################################################################
    Set         includedRes;
    Selection   childTerm;
    int         resnum1;
    int         resnum2;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ResRangeTerm(Selection target, int rn1, int rn2)
    {
        super();
        this.childTerm = target;
        // In all cases I've tried, target is null, but I'm leaving this 
        // reference to it here anyway since Ian had it coded in...
        //System.out.println("Selection target is null: "+(target == null));
        resnum1 = rn1;
        resnum2 = rn2;
    }
//}}}

//{{{ init, selectImpl, toString
//##############################################################################
    public void init(Collection atomStates)
    {
        super.init(atomStates);
        // I commented out the rest of the lines in this method to try and avoid
        // getting errors at runtime. -- dak
        //this.childTerm.init(atomStates);
        // childTerm now fully initialized, doing selections should be safe
        //this.includedRes = childTerm.selectResidues(atomStates);
    }
    
    /**
    * Returns true iff the given AtomState should belong to this selection.
    */
    protected boolean selectImpl(AtomState as)
    {
        int resnum = ( (Residue)as.getResidue() ).getSequenceInteger();
        if (resnum >= resnum1 && resnum <= resnum2)
        {
            //System.out.println("resnum "+resnum+" is btw "+resnum1+" and "+resnum2);
            return true;
        }
        //else
        //    System.out.println("resnum "+resnum+" is NOT btw "+resnum1+" and "+resnum2+" inclusive");
        // I added this fall-back/default return statement and commented out 
        // the old one. -- dak
        return false;
        //return this.includedRes.contains( as.getResidue() );
    }
    
    public String toString()
    { return "resrange ("+resnum1+"-"+resnum2+")"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

