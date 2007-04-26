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
//}}}

//{{{ Variable definitions
//##############################################################################
    Set allItems;   // _table_name.item_name
    Set allTables;  // table_name
    Map tables;     // String -> CifTableModel
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CifFile(InputStream in) throws IOException, java.text.ParseException
    {
        super();
        this.tables     = new HashMap();
        this.allItems   = new HashSet();
        this.allTables  = new HashSet();
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
        Pattern tableColumnPattern = Pattern.compile("_([^.]+)\\.(.+)");
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(in));
        StarFile starFile = new StarReader().parse(lnr);
        
        for(Iterator bi = starFile.getDataBlockNames().iterator(); bi.hasNext(); )
        {
            DataBlock block = (DataBlock) starFile.getDataBlock((String)bi.next());
            //System.out.println("BLOCK: "+block);
            //System.out.println("  "+block.getSaveFrames().size()+" save frames");
            //System.out.println("  "+block.getItemNames().size()+" items");
            Set itemNames = block.getItemNames();
            for(Iterator ii = itemNames.iterator(); ii.hasNext(); )
            {
                String itemName = (String)ii.next();
                List item = (List) block.getItem(itemName);
                Matcher m = tableColumnPattern.matcher(itemName);
                if(!m.matches()) { System.err.println("Regex failed on "+itemName); continue; }
                String tableName = m.group(1), columnName = m.group(2);
                //System.out.println("    "+tableName+" / "+columnName+" : "+item.size());
                this.allItems.add(itemName);
                this.allTables.add(tableName);
                
                CifTableModel tModel = (CifTableModel) tables.get(tableName);
                if(tModel == null)
                {
                    tModel = new CifTableModel(tableName);
                    tables.put(tableName, tModel);
                }
                tModel.addItem(columnName, item);
            }
        }
    }
//}}}

//{{{ getTable/Tables, hasItem/Table
//##############################################################################
    /** Retrieves a table model for the given CIF table name (without leading underscore), or null if none */
    public CifTableModel getTable(String tableName)
    { return (CifTableModel) this.tables.get(tableName); }
    
    /** Returns a Collection(CifTableModel) of all tables in this file. */
    public Collection getTables()
    { return Collections.unmodifiableCollection(this.tables.values()); }
    
    /** Does this data set have the given "_table_name.item_name" pair? */
    public boolean hasItem(String itemName)
    { return this.allItems.contains(itemName); }

    /** Does this data set have the given "table_name" ? */
    public boolean hasTable(String tableName)
    { return this.allTables.contains(tableName); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

