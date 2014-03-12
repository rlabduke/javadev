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
* <code>Disulfides</code> represents a disulfide bond from a protein structure.
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu Sep 3 2009
*/
public class Disulfide //extends ... implements ...
{
//{{{ Constants
    // Disulfide types
    public static final Object NOT_IN_DISULFIDE = "No SS bond detected";
    public static final Object INTRA_CHAIN = "SS bond within same chain";
    public static final Object INTER_CHAIN = "SS bond between two different chains";
    // more?...
//}}}

//{{{ Variable definitions
//##############################################################################
    int     disulfideIndex = 0;
    Object  type = NOT_IN_DISULFIDE;
    String  initChainId, endChainId;
    int     initSeqNum, endSeqNum;
    String  initICode = " ", endICode = " ";
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Disulfide()
    {
        super();
    }
//}}}

//{{{ get(_) functions
//##############################################################################
    public int getIndex()
    { return disulfideIndex; }
    
    public Object getType()
    { return type; }
    
    public String getInitChainId()
    { return initChainId; }
    public String getEndChainId()
    { return endChainId; }
    
    public int getInitSeqNum()
    { return initSeqNum; }
    public int getEndSeqNum()
    { return endSeqNum; }
    
    public String getInitICode()
    { return initICode; }
    public String getEndICode()
    { return endICode; }
//}}}

//{{{ toString
public String toString() {
  return (String)type+" between "+ initChainId+initICode+initSeqNum+" and "+endChainId+endICode+endSeqNum;
}
//}}}

//{{{ contains, matches(Init,End)
//##############################################################################
    /**
    * @return true if the provided Residue matches either CNIT 
    * in this Disulfide, or false otherwise.
    */
    public boolean contains(Residue res)
    {
        if(matchesInit(res) || matchesEnd(res)) return true;
        return false;
    }

    /**
    * Decides the provided Residue matches the first (from the PDB header, usually 
    * first in sequence) residue in this Disulfide based purely on CNIT.
    */
    public boolean matchesInit(Residue res)
    {
      //if (res.getName().equals("CYS")) {
      //  System.out.println("chains: |"+initChainId+"|"+res.getChain()+"|");
      //  System.out.println((res.getSequenceInteger() != initSeqNum)+ " " + (res.getInsertionCode().compareTo(initICode) < 0));
      //}
        if(!res.getName().equals("CYS")) return false; // T
        if(!initChainId.equals(res.getChain())) return false; // C
        if(res.getSequenceInteger() != initSeqNum) return false; // N
        if(res.getInsertionCode().compareTo(initICode) < 0) return false; // I
        return true; // must be a match!        
    }

    /**
    * Decides the provided Residue matches the second (from the PDB header, usually 
    * second in sequence) residue in this Disulfide based purely on CNIT.
    */
    public boolean matchesEnd(Residue res)
    {
        if(!res.getName().equals("CYS")) return false; // T
        if(!endChainId.equals(res.getChain())) return false; // C
        if(res.getSequenceInteger() != endSeqNum) return false; // N
        if(res.getInsertionCode().compareTo(endICode) > 0) return false; // I
        return true; // must be a match!   
    }
//}}}

//{{{ otherEnd
//##############################################################################
    /**
    * @return the Residue at the other end of this Disulfide from the provided
    * Residue, or null if the other end doesn't exist (?) or the provided 
    * Residue isn't actually in this Disulfide.
    */ 
    public Residue otherEnd(Model model, Residue res)
    {
        if(!this.contains(res)) return null; // residue not even in this disulfide
        
        if(matchesInit(res)) // find end
        {
            for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
            {
                Residue curr = (Residue) rItr.next();
                if(matchesEnd(curr)) return curr;
            }
        }
        else if(matchesEnd(res)) // find init
        {
            for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
            {
                Residue curr = (Residue) rItr.next();
                if(matchesInit(curr)) return curr;
            }
        }
        return null; // should never happen (unless code changes elsewhere...)
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

