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
* <code>FoldingBox</code> is an IndentBox which
* removes or inserts a component based on the state of a button.
*
* This allows for GUI elements that expand, tree-like,
* when e.g. a checkbox is hit.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May  6 15:24:44 EDT 2003
*/
public class FoldingBox extends IndentBox implements ChangeListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    ButtonModel     btnModel;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public FoldingBox(ButtonModel model, Component comp)
    {
        super(comp);
        this.btnModel   = model;
        btnModel.addChangeListener(this);
        
        Component target = this.getTarget();
        if(target != null)
            target.setVisible(btnModel.isSelected());
    }

    public FoldingBox(AbstractButton btn, Component comp)
    { this(btn.getModel(), comp); }
//}}}

//{{{ stateChanged
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        Component target = this.getTarget();
        if(target != null)
            target.setVisible(btnModel.isSelected());
        
        // Mark this component and its children as needing to be laid out again
        this.invalidate();
        
        if(getAutoValidate())   validateParent();
        if(getAutoPack())       packParent();
    }
//}}}

//{{{ setTarget
//##################################################################################################
    public void setTarget(Component comp)
    {
        // could be (is) called by superclass constructor!
        // thus btnModel hasn't been initialized yet...
        if(comp != null && btnModel != null)
            comp.setVisible(btnModel.isSelected());
        super.setTarget(comp);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

