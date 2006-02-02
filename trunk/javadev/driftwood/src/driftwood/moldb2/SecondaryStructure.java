// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

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
* <code>SecondaryStructure</code> represents a generic secondary structure
* assignment for a bunch of residues.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb  2 09:43:51 EST 2006
*/
abstract public class SecondaryStructure //extends ... implements ...
{
//{{{ Constants
    // Secondary structure types
    public static final Object COIL             = "Unnamed structure (coil)";
    public static final Object HELIX            = "Generic helix";
    // Listed in PDB file format guide:
    //public static final Object HELIX_RH_AL      = "Right-handed alpha helix";   // DSSP "H"
    //public static final Object HELIX_RH_OM      = "Right-handed omega helix";
    //public static final Object HELIX_RH_PI      = "Right-handed pi helix";      // DSSP "I"
    //public static final Object HELIX_RH_GA      = "Right-handed gamma helix";
    //public static final Object HELIX_RH_310     = "Right-handed 3_10 helix";    // DSSP "G"
    //public static final Object HELIX_LH_AL      = "Left-handed alpha helix";
    //public static final Object HELIX_LH_OM      = "Left-handed omega helix";
    //public static final Object HELIX_LH_GA      = "Left-handed gamma helix";
    //public static final Object HELIX_27_RIBBON  = "27 ribbon/helix";            // what's this?
    //public static final Object HELIX_POLYPRO    = "Poly-proline helix";
    public static final Object STRAND           = "Generic extended strand";      // DSSP "E"
    public static final Object TURN             = "Generic turn";
    // Listed in mmCIF dictionary under struct_conf_type:
    //public static final Object TURN_TY1         = "Type 1 turn";
    //public static final Object TURN_TY1P        = "Type 1' turn";
    //public static final Object TURN_TY2         = "Type 2 turn";
    //public static final Object TURN_TY2P        = "Type 2' turn";
//}}}

//{{{ CLASS: AllCoil
//##############################################################################
    /** A dummy implementation that always returns COIL. */
    public static class AllCoil extends SecondaryStructure
    {
        /** A dummy implementation that always returns COIL. */
        public Object classify(Residue r)
        { return COIL; }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SecondaryStructure()
    {
        super();
    }
//}}}

    /** Returns one of the structure category constants defined by this class. */
    abstract public Object classify(Residue r);

//{{{ isHelix/Strand/Turn/Coil
//##############################################################################
    // This will have to become more complicated if we allow different types of helix
    public boolean isHelix(Residue r)
    { return HELIX.equals(classify(r)); }

    public boolean isStrand(Residue r)
    { return STRAND.equals(classify(r)); }

    public boolean isTurn(Residue r)
    { return TURN.equals(classify(r)); }

    public boolean isCoil(Residue r)
    { return COIL.equals(classify(r)); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

