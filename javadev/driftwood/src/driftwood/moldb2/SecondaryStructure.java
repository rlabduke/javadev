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

//{{{ CLASS: Range
//##############################################################################
    /** Describes a start-end range for a helix, sheet, or turn */
    public static class Range
    {
        int     rangeIndex = 0; 
        Object  type = COIL; 
        String  chainId;
        public int     initSeqNum, endSeqNum; // added public (ARK Spring2010)
        String  initICode = " ", endICode = " ";
        public int	sense = 0; // 0 if first strand, 1 if parallel, -1 if anti-parallel (ARK Spring2010), is this used? 
        public int 	strand = 1; // starts at 1 for each strand within a sheet and increases by one  (ARK Spring2010) 
        public String	sheetID = " "; 		// (ARK Spring2010)
        public boolean 	flipped = false;  	// (ARK Spring2010)
        public Range	previous = null;  	// (ARK Spring2010)
        public Range	next = null; 	   	// (ARK Spring2010)
        public Range    duplicateOf = null; 	// (ARK Spring2010)
        
        public boolean contains(Residue r)
        {
            if(!chainId.equals(r.getChain())) return false;
            int seqNum = r.getSequenceInteger();
            if(seqNum < initSeqNum || seqNum > endSeqNum) return false;
            String iCode = r.getInsertionCode();
            if(seqNum == initSeqNum && iCode.compareTo(initICode) < 0) return false;
            if(seqNum == endSeqNum  && iCode.compareTo(endICode)  > 0) return false;
            return true;
        }
        
        /// could change vars from public and add more accessor methods (ARK Spring2010)
        public Object getType()
        { return type; }
        
        public int getIndex()
        { return rangeIndex; }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    public Collection ranges = new ArrayList(); // changed private to public, (ARK Spring2010)
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SecondaryStructure()
    {
        super();
    }
//}}}

//{{{ add/getRange, classify
//##############################################################################
    protected void addRange(Range r)
    {
    	ranges.add(r);
        r.rangeIndex = ranges.size();
    }
    
    /** Returns a Range object denoting a secondary structure element (helix, strand, etc) or null for none. */
    public Range getRange(Residue res)
    {
        for(Iterator iter = ranges.iterator(); iter.hasNext(); )
        {
            Range rng = (Range) iter.next();
            if(rng.duplicateOf!=null) continue; // (ARK Spring2010)
	    if(rng.contains(res)) return rng;
        }
        return null; // no entry for that residue
    }

    /** Returns one of the structure category constants defined by this class. */
    public Object classify(Residue res)
    {
        Range rng = getRange(res);
        if(rng != null) return rng.getType();
        else return COIL;
    }
//}}}

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

//{{{ consolidateSheets  
    public void consolidateSheets() // (ARK Spring2010)
    {
    	Hashtable uniqueStrands = new Hashtable();
        
    	// For each ribbon, record it's predecessor in sheet, and check for duplicates
        ///this is repeated later in ribbon printer, with strands vector!! fix?!?!?
    	for(Iterator iter = ranges.iterator(); iter.hasNext(); ) 
        {
            Range rng = (Range) iter.next();  
            if(!rng.type.equals(STRAND)) continue; // not a sheet
            
            // get unique representation for strand, test if it's been found before
            Integer key = new Integer(rng.initSeqNum*1000 + (int)rng.chainId.charAt(0)); ////sufficient to make it unique?!?!?
            if(!uniqueStrands.containsKey(key)) uniqueStrands.put(key, rng);
	    else rng.duplicateOf = (Range)uniqueStrands.get(key);
            
            // now find this strand's previous and next strand
            for(Iterator iter2 = ranges.iterator(); iter2.hasNext(); ){
	    	Range rng2 = (Range) iter2.next();  
	    	if(!rng2.type.equals(STRAND)) continue;
	    	if(rng2.sheetID.equals(rng.sheetID) && rng2.strand == rng.strand-1){
            		    rng.previous = rng2;
            		    rng2.next = rng;
            		    ///break;
            	}  
            }
        }
        
        // now go through and reassign previous/next fields
        for(Iterator iter = ranges.iterator(); iter.hasNext(); ) 
        {
                Range rng = (Range) iter.next();  
   	        if(!rng.type.equals(STRAND)) continue; // not a sheet
		if(rng.duplicateOf==null && rng.next!=null && rng.next.duplicateOf!=null)
			rng.next.duplicateOf.previous = rng;	
		if(rng.duplicateOf==null && rng.previous!= null && rng.previous.duplicateOf!=null)
			rng.previous = rng.previous.duplicateOf;	
        }
        
    }

//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

