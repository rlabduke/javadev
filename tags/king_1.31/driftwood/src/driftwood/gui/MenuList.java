// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
//import driftwood.*;
//}}}
/**
* <code>MenuList</code> is a simple system for handling lists
* of items in a menu where the user must choose a single item.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon May 26 13:21:04 EDT 2003
*/
abstract public class MenuList implements ActionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    private Map     radioToItemMap;
    private JMenu   theMenu;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @param items          the items to choose from (should probably have the Set property)
    * @param defaultItem    the item to be selected initially. Defaults to none (null).
    * @param menu           the menu to build the list in.
    */
    public MenuList(Collection items, Object defaultItem, JMenu menu)
    {
        super();
        theMenu                         = menu;
        radioToItemMap                  = new HashMap();
        
        ButtonGroup             group   = new ButtonGroup();
        JRadioButtonMenuItem    radio;
        
        Object item;
        for(Iterator iter = items.iterator(); iter.hasNext(); )
        {
            item = iter.next();
            radio = new JRadioButtonMenuItem(item.toString());
            group.add(radio);
            if(item.equals(defaultItem)) radio.setSelected(true);
            theMenu.add(radio);
            radio.addActionListener(this);
            
            radioToItemMap.put(radio, item);
        }
    }
    
    public MenuList(Collection items, Object defaultItem)
    { this(items, defaultItem, new JMenu()); }
    
    public MenuList(Collection items)
    { this(items, null); }
//}}}

//{{{ getMenu, actionPerformed
//##################################################################################################
    public JMenu getMenu()
    { return theMenu; }
    
    public void actionPerformed(ActionEvent ev)
    {
        Object item = radioToItemMap.get(ev.getSource());
        itemSelected(item);
    }
//}}}

//{{{ itemSelected
//##################################################################################################
    /**
    * This function will be called when an item is selected from
    * the menu of radio buttons.
    * Subclasses should implement this to create custom functionality.
    */
    abstract protected void itemSelected(Object item);
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

