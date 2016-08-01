// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cifless;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.star.*;
//}}}
/**
* <code>CifFile</code> holds all the tables in the mmCIF file for a molecule.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jul 24 09:53:32 EDT 2006
*/
public class CifFile //extends ... implements ...
{
//{{{ Constants
    static final Pattern tableColumnPattern = Pattern.compile("_([^.]+)\\.(.+)");
//}}}

//{{{ CLASS: Block
//##############################################################################
    /** A data block or a save frame */
    public static class Block
    {
        String                      simpleName;
        Map<String, CifTableModel>  tables          = new UberMap();
        Map<String, Block>          subBlocks       = new UberMap();
        
        public Block(String name)
        {
            this.simpleName = name;
        }
        
        public String getName()
        { return simpleName; }
        
        public String toString()
        { return simpleName; }
        
        public Map<String, CifTableModel> getTables()
        { return tables; }
        
        public Map<String, Block> getSubBlocks()
        { return subBlocks; }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    Block globalBlock;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CifFile(InputStream in) throws IOException, java.text.ParseException
    {
        super();
        loadCifTables(in);
    }
//}}}

//{{{ loadCifTables
//##############################################################################
    /**
    * Given a stream to a CIF file, returns a map of Strings (table names)
    * to CifTableModels.
    */
    private void loadCifTables(InputStream in) throws IOException, java.text.ParseException
    {
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(in));
        StarFile starFile = new StarReader().parse(lnr);
        
        this.globalBlock = loadBlock(starFile.getGlobalBlock());
        Set<String> dataNames = starFile.getDataBlockNames();
        for(String dataName : dataNames)
        {
            DataBlock data = starFile.getDataBlock(dataName);
            this.globalBlock.subBlocks.put(data.getName(), loadBlock(data));
        }
    }
//}}}

//{{{ loadBlock
//##############################################################################
    private Block loadBlock(DataCell cell)
    {
        Block block = new Block(cell.getName());
        
        Set<String> itemNames = cell.getItemNames();
        for(String itemName : itemNames)
        {
            List item = (List) cell.getItem(itemName);
            Matcher m = tableColumnPattern.matcher(itemName);
            if(!m.matches()) { System.err.println("Regex failed on "+itemName); continue; }
            String tableName = m.group(1), columnName = m.group(2);
            //System.out.println("    "+tableName+" / "+columnName+" : "+item.size());
            
            CifTableModel tModel = block.tables.get(tableName);
            if(tModel == null)
            {
                tModel = new CifTableModel(tableName);
                block.tables.put(tableName, tModel);
            }
            try { tModel.addItem(columnName, item); }
            catch(IllegalArgumentException ex) { ex.printStackTrace(); }
        }
        
        if(cell instanceof DataBlock)
        {
            DataBlock data = (DataBlock) cell;
            Collection<DataCell> saveFrames = data.getSaveFrames();
            for(DataCell saveFrame : saveFrames)
                block.subBlocks.put(saveFrame.getName(), loadBlock(saveFrame));
        }
        
        return block;
    }
//}}}

//{{{ getTopBlock
//##############################################################################
    /** Retrieves the Block at the top of this CIF file's hierarchy. */
    public Block getTopBlock()
    { return this.globalBlock; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

