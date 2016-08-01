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
* <code>AttentiveTextField</code> sends an ActionEvent when it loses the input
* focus iff its contents have changed since it gained focus, or since the
* last ActionEvent, whichever is more recent.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 26 15:13:33 EST 2004
*/
public class AttentiveTextField extends JTextField implements ActionListener, FocusListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String lastContents;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AttentiveTextField()
    { this(null, 0); }
    public AttentiveTextField(int columns)
    { this(null, columns); }
    public AttentiveTextField(String contents)
    { this(contents, 0); }
    
    public AttentiveTextField(String contents, int columns)
    {
        super(contents, columns);
        this.lastContents = this.getText();
        this.addActionListener(this);
        this.addFocusListener(this);
    }
//}}}

//{{{ actionPerformed, focusGained, focusLost
//##############################################################################
    public void actionPerformed(ActionEvent ev)
    {
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
            fireActionPerformed();
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

