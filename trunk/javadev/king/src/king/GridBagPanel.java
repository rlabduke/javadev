// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
//import java.io.*;
//import java.net.*;
//import java.text.*;
//import java.util.*;
import javax.swing.*;
//import gnu.regexp.*;
//}}}
/**
 * <code>GridBagPanel</code> is just a JPanel with a GridBagLayout and some convenience functions.
 * To set default constraints, access the <code>gbc</code> member variable.
 *
 * <p><em>Functions</em> that set alignment properties, etc. take effect only until the next add() is called.
 * They return a reference to this same GridBagPanel so function calls can be "chained", like
 * java.lang.StringBuffer allows.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <p>Begun on Tue Jul  2 09:08:06 EDT 2002
*/
public class GridBagPanel extends JPanel // implements ...
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    GridBagLayout gbl;
    
    /** These are the default constraints for all objects added to the panel */
    public GridBagConstraints gbc;
    
    /** These are the derived constraints for the next component to be added */
    GridBagConstraints derived;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new panel and installs a GridBagLayout on it.
    */
    public GridBagPanel()
    {
        super();
        gbl = new GridBagLayout();
        gbc = new GridBagConstraints();
        derived = null;
        setLayout(gbl);
    }
//}}}

//{{{ add(...) functions
//##################################################################################################
    public void add(Component comp, int x, int y)
    {
        if(derived == null) rederive();
        if(x == -1) derived.gridx = GridBagConstraints.RELATIVE;
        else        derived.gridx = x;
        if(y == -1) derived.gridy = GridBagConstraints.RELATIVE;
        else        derived.gridy = y;
        gbl.setConstraints(comp, derived);
        this.add(comp);
        derived = null;
    }

    public void add(Component comp, int x, int y, int w, int h)
    {
        if(derived == null) rederive();
        span(w, h).add(comp, x, y);
    }

    public void add(Component comp, int x, int y, int w, int h, int fill, int anchor)
    {
        if(derived == null) rederive();
        derived.fill = fill;
        derived.anchor = anchor;
        span(w, h).add(comp, x, y);
    }

    public void add(Component comp, int x, int y, int w, int h, int fill, int anchor, double wx, double wy)
    {
        if(derived == null) rederive();
        derived.fill = fill;
        derived.anchor = anchor;
        span(w, h).weight(wx, wy).add(comp, x, y);
    }
//}}}

//{{{ rederive, span, weight, insets
//##################################################################################################
    /** Resets derived properties to be the same as defaults. */
    void rederive()
    { derived = (GridBagConstraints)gbc.clone(); }
    
    /** Sets gridwidth and gridheight for the next component added */
    public GridBagPanel span(int x, int y)
    {
        if(derived == null) rederive();
        derived.gridwidth   = x;
        derived.gridheight  = y;
        return this;
    }
    
    /** Sets weightx and weighty for the next component added */
    public GridBagPanel weight(double x, double y)
    {
        if(derived == null) rederive();
        derived.weightx = x;
        derived.weighty = y;
        return this;
    }
    
    /** Sets the insets for the next component added */
    public GridBagPanel insets(int top, int left, int bottom, int right)
    {
        if(derived == null) rederive();
        derived.insets = new Insets(top, left, bottom, right);
        return this;
    }
//}}}

//{{{ fillNone, fillH, fillV, fillBoth
//##################################################################################################
    public GridBagPanel fillNone()
    {
        if(derived == null) rederive();
        derived.fill = GridBagConstraints.NONE;
        return this;
    }
    
    public GridBagPanel fillH()
    {
        if(derived == null) rederive();
        derived.fill = GridBagConstraints.HORIZONTAL;
        return this;
    }
    
    public GridBagPanel fillV()
    {
        if(derived == null) rederive();
        derived.fill = GridBagConstraints.VERTICAL;
        return this;
    }
    
    public GridBagPanel fillBoth()
    {
        if(derived == null) rederive();
        derived.fill = GridBagConstraints.BOTH;
        return this;
    }
//}}}

//{{{ anchor functions (center, north, northwest, etc.)
//##################################################################################################
    public GridBagPanel center()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.CENTER;
        return this;
    }
    
    public GridBagPanel north()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.NORTH;
        return this;
    }
    
    public GridBagPanel northeast()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.NORTHEAST;
        return this;
    }
    
    public GridBagPanel east()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.EAST;
        return this;
    }
    
    public GridBagPanel southeast()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.SOUTHEAST;
        return this;
    }
    
    public GridBagPanel south()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.SOUTH;
        return this;
    }
    
    public GridBagPanel southwest()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.SOUTHWEST;
        return this;
    }
    
    public GridBagPanel west()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.WEST;
        return this;
    }
    
    public GridBagPanel northwest()
    {
        if(derived == null) rederive();
        derived.anchor = GridBagConstraints.NORTHWEST;
        return this;
    }
//}}}

//{{{ empty
//##################################################################################################
//}}}
}//class
