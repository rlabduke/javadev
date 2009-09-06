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

//{{{ contains
//##############################################################################
    public boolean contains(Residue r)
    {
        if(!r.getName().equals("CYS")) return false;
        if(!initChainId.equals(r.getChain())
        && ! endChainId.equals(r.getChain())) return false;
        int seqNum = r.getSequenceInteger();
        if(seqNum != initSeqNum && seqNum != endSeqNum) return false;
        String iCode = r.getInsertionCode();
        if(seqNum == initSeqNum && iCode.compareTo(initICode) < 0) return false;
        if(seqNum == endSeqNum  && iCode.compareTo(endICode)  > 0) return false;
        return true;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

