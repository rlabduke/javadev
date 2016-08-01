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
* <code>SwapBox</code> is a wrapper component that acts
* as a placeholder in a complex layout. It provides any
* easy way to swap other components into and out of the
* same space, and provides methods for updating its
* containers. 
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May  6 15:24:44 EDT 2003
*/
public class SwapBox extends AlignBox
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    private Component   targetComp      = null;
    private boolean     doAutoPack      = false;
    private boolean     doAutoValidate  = true;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a box that will re-validate() its parents but not re-pack() them.
    * @param comp the target Component to be swapped in. May be null.
    */
    public SwapBox(Component comp)
    {
        super(BoxLayout.X_AXIS);
        this.setTarget(comp);
    }
//}}}

//{{{ get/setTarget
//##################################################################################################
    /** Gets the component. May be null. */
    public Component getTarget()
    { return targetComp; }
    
    /**
    * Sets the swap in / swap out component.
    * @param comp the target Component to be swapped in. May be null.
    */
    public void setTarget(Component comp)
    {
        if(targetComp != null) this.remove(targetComp);
        targetComp = comp;
        if(targetComp != null) this.add(targetComp);
        
        this.invalidate();
        if(doAutoValidate)  this.validateParent();
        if(doAutoPack)      this.packParent();
    }
//}}}

//{{{ get/set{AutoPack, AutoValidate}
//##################################################################################################
    public boolean getAutoPack()
    { return doAutoPack; }
    public void setAutoPack(boolean b)
    { doAutoPack = b; }
    
    public boolean getAutoValidate()
    { return doAutoValidate; }
    public void setAutoValidate(boolean b)
    { doAutoValidate = b; }
//}}}

//{{{ packParent, validateParent
//##################################################################################################
    /**
    * Causes the top-level parent of this component to be laid out again,
    * assuming some of its children have been invalidated.
    */
    public void validateParent()
    {
        // Find the top-level ancestor of this component,
        // and cause it to be laid out again.
        Container parent = this;
        while(parent.getParent() != null) parent = parent.getParent();
        parent.validate();
    }
    
    /**
    * Causes the window/dialog/etc that contains this component
    * to be resized and laid out again,
    * assuming some of its children have been invalidated.
    */
    public void packParent()
    {
        // Find the top-level ancestor of this component,
        // and cause it to be laid out again.
        Container parent = this;
        while((parent = parent.getParent()) != null)
        {
            if(parent instanceof Window)
            {
                ((Window)parent).pack();
                break;
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

