// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cifless;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.star.*;
//}}}
/**
* <code>CifDictionary</code> handles reading in and interpretting mmCIF dictionaries,
* particularly parent-child relationships between columns (ie, primary keys vs foreign keys).
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jul 21 13:44:03 EDT 2006
*/
public class CifDictionary //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Map parentage; // child -> parent
    Map offspring; // parent -> Collection( children )
    Map groupings; // cat. group -> Collection( groups )
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CifDictionary()
    {
        super();
        try
        {
            StarFile dom = loadDOM(this.getClass().getResourceAsStream("mmcif_pdbx.dic"));
            this.parentage = getParentChildLinks(dom);
            this.offspring = getChildrenForParents(parentage);
            this.groupings = getCategoryGroups(dom);
            // DOM is garbage collected b/c it's big and we don't need it all
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.err.println("*** Unable to load CIF dictionary"); 
        }
    }
//}}}

//{{{ loadDOM
//##############################################################################
    private StarFile loadDOM(InputStream in) throws IOException, java.text.ParseException
    {
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(in));
        StarFile starFile = new StarReader().parse(lnr);
        return starFile;
    }
//}}}
    
//{{{ getParentChildLinks
//##############################################################################
    /** Returns a map of children (String) to parents (String). */
    private Map getParentChildLinks(StarFile starFile)
    {
        Map childrenToParents = new HashMap();
        for(Iterator bi = starFile.getDataBlockNames().iterator(); bi.hasNext(); )
        {
            DataBlock block = (DataBlock) starFile.getDataBlock((String)bi.next());
            //System.out.println("BLOCK: "+block);
            //System.out.println("  "+block.getSaveFrames().size()+" save frames");
            //System.out.println("  "+block.getItemNames().size()+" items");
            //Set itemNames = block.getItemNames();
            //for(Iterator ii = itemNames.iterator(); ii.hasNext(); )
            //{
            //    String itemName = (String)ii.next();
            //    List item = (List) block.getItem(itemName);
            //    System.out.println("    "+itemName+" : "+item.size());
            //}
            for(Iterator si = block.getSaveFrames().iterator(); si.hasNext(); )
            {
                DataCell saveFrame = (DataCell) si.next();
                List children = saveFrame.getItem("_item_linked.child_name");
                if(children.size() == 0) continue;
                List parents = saveFrame.getItem("_item_linked.parent_name");
                Iterator pi = parents.iterator();
                for(Iterator ci = children.iterator(); ci.hasNext(); )
                {
                    String child = (String) ci.next();
                    String parent = saveFrame.getName();
                    if(pi.hasNext())  parent = (String) pi.next();
                    //if(childrenToParents.containsKey(child)) System.out.println("Already have parent for "+child);
                    childrenToParents.put(child, parent);
                    //System.out.println(child+" --> "+parent);
                }
            }
        }
        return childrenToParents;
    }
//}}}

//{{{ getCategoryGroups
//##############################################################################
    /** Returns a map of table groups (String) to tables (Collection(String)). */
    private Map getCategoryGroups(StarFile starFile)
    {
        Map groupsToTables = new TreeMap();
        for(Iterator bi = starFile.getDataBlockNames().iterator(); bi.hasNext(); )
        {
            DataBlock block = (DataBlock) starFile.getDataBlock((String)bi.next());
            for(Iterator si = block.getSaveFrames().iterator(); si.hasNext(); )
            {
                DataCell saveFrame = (DataCell) si.next();
                String saveName = saveFrame.getName();
                List groups = saveFrame.getItem("_category_group.id");
                for(Iterator gi = groups.iterator(); gi.hasNext(); )
                {
                    String group = (String) gi.next();
                    Collection tables = (Collection) groupsToTables.get(group);
                    if(tables == null)
                    {
                        tables = new TreeSet();
                        groupsToTables.put(group, tables);
                    }
                    tables.add(saveName);
                }
            }
        }
        return groupsToTables;
    }
//}}}

//{{{ getChildrenForParents
//##############################################################################
    private Map getChildrenForParents(Map childrenToParents)
    {
        Map parentsToChildren = new HashMap();
        for(Iterator iter = childrenToParents.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry e = (Map.Entry) iter.next();
            String child = (String) e.getKey();
            String parent = (String) e.getValue();
            Collection children = (Collection) parentsToChildren.get(parent);
            if(children == null)
            {
                children = new ArrayList();
                parentsToChildren.put(parent, children);
            }
            children.add(child);
        }
        return parentsToChildren;
    }
//}}}

//{{{ getParent, getChildren, getGroupings
//##############################################################################
    /**
    * Returns the parent of a CIF item or null if none;
    * that is, it returns the table and primary key referenced by this foreign key.
    */
    public String getParent(String cifItem)
    { return (String) parentage.get(cifItem); }

    /**
    * Returns the parent of a CIF item or null if none;
    * that is, it returns the table and primary key referenced by this foreign key.
    */
    public Collection getChildren(String cifItem)
    { return (List) offspring.get(cifItem); }
    
    /**
    * Returns a map of category group names ("atom_group") to a Collection
    * of category names ("atom_site", ...)
    */
    public Map getGroupings()
    { return Collections.unmodifiableMap(this.groupings); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

