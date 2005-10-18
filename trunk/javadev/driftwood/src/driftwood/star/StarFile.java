// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.star;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
//}}}
/**
* <code>StarFile</code> is the top of the Document Object Model (DOM)
* representing the contents and structure of a STAR file.
* This DOM can model all of the syntax of STAR (Self-defining Text Archive
* and Retrieval) files as described in the 1994 paper by Hall and Spadaccini
* (J Chem Inf Comput Sci, 34:505), <b>except for nested loop_ objects.</b>
* Simple (one-level) loops are fully supported.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed May 19 11:54:37 EDT 2004
*/
public class StarFile //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    DataCell    globalBlock;    // never null
    UberMap     dataBlocks;     // may be empty
//}}}

//{{{ Constructor(s)
//##############################################################################
    public StarFile()
    {
        super();
        this.globalBlock = new DataCell("GLOBAL");
        this.dataBlocks = new UberMap();
    }
//}}}

//{{{ getGlobalBlock, getDataBlockNames, getDataBlock
//##############################################################################
    /** Returns the global_ block for this StarFile, which may be empty. */
    public DataCell getGlobalBlock()
    { return globalBlock; }
    
    /** Returns the set of data block names in this file. */
    public Set getDataBlockNames()
    { return Collections.unmodifiableSet(dataBlocks.keySet()); }
    
    /** Returns the named data block, or null if unknown. */
    public DataBlock getDataBlock(String blockName)
    { return (DataBlock) dataBlocks.get(blockName); }
//}}}

//{{{ addDataBlock, removeDataBlock
//##############################################################################
    /**
    * Adds the data block, displacing any former data blocks of the same name
    * (as returned by toString).
    * Returns the previous block of the same name, or null if none.
    */
    public DataBlock addDataBlock(DataBlock block)
    { return (DataBlock) dataBlocks.put(block.toString(), block); }
    
    /** Removes the named data block if it's part of this file. */
    public DataBlock removeDataBlock(String blockName)
    { return (DataBlock) dataBlocks.remove(blockName); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

