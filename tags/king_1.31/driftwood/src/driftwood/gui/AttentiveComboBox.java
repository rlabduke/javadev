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
//import driftwood.*;
//}}}
/**
* <code>AttentiveComboBox</code> sends an ActionEvent when it loses the input
* focus iff its selection has changed since it gained focus, or since the
* last ActionEvent, whichever is more recent.
* Of course, this only matters if this particular combo box is editable,
* so we setEditable(true) in all of the constructors.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 26 15:13:33 EST 2004
*/
public class AttentiveComboBox extends JComboBox implements FocusListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String lastContents;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AttentiveComboBox()
    {
        super();
        init();
    }
        
    public AttentiveComboBox(ComboBoxModel aModel)
    {
        super(aModel);
        init();
    }
        
    public AttentiveComboBox(Object[] items)
    {
        super(items);
        init();
    }
        
    public AttentiveComboBox(Vector items)
    {
        super(items);
        init();
    }
        
    public AttentiveComboBox(Collection items)
    {
        super(items.toArray());
        init();
    }
        
    private void init()
    {
        this.addActionListener(this);
        
        this.setEditable(true);
        ComboBoxEditor cbe = this.getEditor();
        cbe.getEditorComponent().addFocusListener(this);
        this.lastContents = this.getText();
    }
//}}}

//{{{ actionPerformed, focusGained, focusLost
//##############################################################################
    public void actionPerformed(ActionEvent ev)
    {
        super.actionPerformed(ev);
        this.lastContents = this.getText();
    }
    
    public void focusGained(FocusEvent ev)
    {
        this.lastContents = this.getText();
    }
    
    public void focusLost(FocusEvent ev)
    {
        if((this.lastContents == null && this.getText() != null)
        || (this.lastContents != null && !this.lastContents.equals(this.getText())))
        {
            fireActionEvent();
        }
    }
//}}}

//{{{ getText
//##############################################################################
    /**
    * Returns the current item as displayed in the editable combo box,
    * even if that item is not a part of the current list.
    * Returns null if the box is not editable or no data is present.
    */
    public String getText()
    {
        ComboBoxEditor cbe = this.getEditor();
        if(cbe == null) return null;
        Object item = cbe.getItem();
        if(item == null) return null;
        return item.toString();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

