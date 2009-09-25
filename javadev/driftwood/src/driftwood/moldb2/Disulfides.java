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
* <code>Disulfides</code> represents a generic set of disulfide bonds.
* It was basically copied from IWD's SecondaryStructure class.
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Wed Sep 2 2009
*/
abstract public class Disulfides //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ CLASS: NoDisulfides
//##############################################################################
    /** A dummy implementation that always returns NOT_IN_DISULFIDE. */
    public static class NoDisulfides extends Disulfides
    {
        /** A dummy implementation that always returns NOT_IN_DISULFIDE. */
        public Object classify(Residue r)
        { return Disulfide.NOT_IN_DISULFIDE; }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Holds Disulfide objects */
    private Collection disulfides = new ArrayList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Disulfides()
    {
        super();
    }
//}}}

//{{{ add, get(All), classify
//##############################################################################
    protected void add(Disulfide d)
    {
        disulfides.add(d);
        d.disulfideIndex = disulfides.size();
    }
    
    /** @return a Disulfide object denoting a type of disulfide bond (intra-chain or 
    * inter-chain) or null if the residue doesn't participate in a disulfide bond. */
    public Disulfide get(Residue res)
    {
        for(Iterator iter = disulfides.iterator(); iter.hasNext(); )
        {
            Disulfide dslf = (Disulfide) iter.next();
            if(dslf.contains(res)) return dslf;
        }
        return null; // no entry for that residue
    }
    
    /** @return an unmodifiable view of the Disulfides in this structure. */
    public Collection getAll()
    {
        return Collections.unmodifiableCollection(disulfides);
    }
    
    /** @return one of the disulfide category constants defined by this class, 
    * or null if the residue doesn't participate in a disulfide bond. */
    public Object classify(Residue res)
    {
        Disulfide dslf = get(res);
        if(dslf != null) return dslf.getType();
        else return null;
    }
//}}}

//{{{ isIntraChain, isInterChain
//##############################################################################
    public boolean isInDisulfide(Residue r)
    { return !Disulfide.NOT_IN_DISULFIDE.equals(classify(r)); }
    
    public boolean isIntraChain(Residue r)
    { return Disulfide.INTRA_CHAIN.equals(classify(r)); }

    public boolean isInterChain(Residue r)
    { return Disulfide.INTER_CHAIN.equals(classify(r)); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

