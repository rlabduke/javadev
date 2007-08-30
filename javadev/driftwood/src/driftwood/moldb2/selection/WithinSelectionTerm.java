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
* <code>WithinSelectionTerm</code> handles "within DIST of (SELECTION)" statements.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Aug 29 13:33:28 PDT 2007
*/
public class WithinSelectionTerm extends Selection
{
//{{{ Constants
    static final private DecimalFormat df = new DecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##############################################################################
    double      distance;
    Selection   childTerm;
    SpatialBin  spatialBin;
    Collection  foundPts;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public WithinSelectionTerm(double distance, Selection target)
    {
        super();
        this.distance = distance;
        this.childTerm = target;
        this.foundPts = new ArrayList();
    }
//}}}

//{{{ init, selectImpl, toString
//##############################################################################
    public void init(Collection atomStates)
    {
        super.init(atomStates);
        this.childTerm.init(atomStates);
        // childTerm now fully initialized, doing selections should be safe
        this.spatialBin = new SpatialBin(3.0); // taken from Molikin; a good size for atoms
        spatialBin.addAll( childTerm.selectAtomStates(atomStates) );
    }
    
    /**
    * Returns true iff the given AtomState should belong to this selection.
    */
    protected boolean selectImpl(AtomState as)
    {
        this.foundPts.clear();
        this.spatialBin.findSphere(as, this.distance, this.foundPts);
        return !foundPts.isEmpty();
    }
    
    public String toString()
    { return "within "+df.format(distance)+" of ("+childTerm+")"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

