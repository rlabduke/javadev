// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
//}}}
/**
* <code>TablePane</code> is a second-generation tool for
* simplifying the task of hand-coding a GUI.
* It is closely modeled on HTML tables, which are a very
* flexible tool, particularly when nested.
* The underlying framework uses GridBagLayout.
*
* <p>Nesting is simplified by the <code>startSubtable</code> and
* <code>endSubtable</code> functions.
* When a subtable is started, a new TablePane object is created.
* The current TablePane is then connected to the new TablePane,
* and its methods "pass through" to the subtable until
* <code>endSubtable</code> is called.
* Subtables do not inherit the layout properties of their parents.
*
* <p>This class is geared toward relatively static layouts,
* and may not be as useful in designing layouts that change frequently.
*
* <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Dec 18 14:25:54 EST 2002
*/
public class TablePane extends JPanel // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    TablePane           subtable;
    GridBagLayout       layout;
    GridBagConstraints  gbc;
    LinkedList          gbcStack;
    int[]               maxGridX;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * General purpose constructor.
    * The new table has the following defaults:
    * <ul>
    * <li>Horizontal and vertical weights are 1.0</li>
    * <li>Components do not stretch to fill space</li>
    * <li>Components align to the left (horiz.) and middle (vert.)</li>
    * <li>Each cell has external padding of 2 pixels on all sides, and no insets (internal padding)</li>
    * </ul>
    */
    public TablePane()
    {
        super();
        
        subtable = null;
        layout = new GridBagLayout();
        this.setLayout(layout);
        
        gbc         = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.NONE;
        gbc.insets  = new Insets(2,2,2,2);
        gbc.ipadx   = 0;
        gbc.ipady   = 0;
        
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        maxGridX = new int[20];
        Arrays.fill(maxGridX, 0);
        
        gbcStack = new LinkedList();
    }
//}}}

//{{{ startSubtable, endSubtable, isSubtable, getCurrent
//##################################################################################################
    /**
    * Starts a new subtable, and adds it to the current table.
    * Calls to this table will "pass through" to the subtable
    * until <code>endSubtable</code> is called. Subtables may
    * be nested arbitrarily deep.
    *
    * <p>The subtable inherits the layout properties of the current
    * table, but changes to them will not be reflected in the parent.
    */
    public void startSubtable()
    {
        startSubtable(1, 1);
    }

    public void startSubtable(int gridwidth, int gridheight)
    {
        if(subtable != null) subtable.startSubtable(gridwidth, gridheight);
        else
        {
            TablePane tp = new TablePane();
            this.add(tp, gridwidth, gridheight);
            subtable = tp;
        }
    }
    
    /**
    * Ends the mostly recently started subtable.
    * @throws UnsupportedOperationException if there are no active subtables 
    */
    public void endSubtable()
    {
        if(subtable == null)                throw new UnsupportedOperationException("No subtable exists");
        else if(subtable.subtable == null)  subtable = null;
        else                                subtable.endSubtable();
    }
    
    /**
    * Returns <code>true</code> if <code>startSubtable</code>
    * has been called more times than <code>endSubtable</code>;
    * that is, if function calls on this object affect a subtable
    * rather than this object itself.
    */
    public boolean isSubtable()
    { return (subtable != null); }
    
    /**
    * Returns the current TablePane that will be affected
    * by function calls on this object (i.e., the active
    * subtable). If there are no subtables, returns <code>this</code>.
    */
    public TablePane getCurrent()
    {
        if(subtable != null) return subtable.getCurrent();
        else return this;
    }
//}}}

//{{{ add, getMaxGridX, setMaxGridX, newRow, replace
//##################################################################################################
    /**
    * Adds the specified component at the end of the current row.
    */
    public Component add(Component c)
    {
        return add(c, 1, 1);
    }
    
    /**
    * Adds the specified component at the end of the current row.
    * It will occupy gridwidth columns and gridheight rows.
    */
    public Component add(Component c, int gridwidth, int gridheight)
    {
        if(subtable != null)
        {
            return subtable.add(c, gridwidth, gridheight);
        }
        else
        {
            gbc.gridwidth   = Math.max(1, gridwidth);
            gbc.gridheight  = Math.max(1, gridheight);
            
            gbc.gridx = getMaxGridX(gbc.gridy);
            for(int i = 0; i < gridheight; i++) setMaxGridX( gbc.gridy+i, gbc.gridx+gridwidth);
            
            return addWithConstraints(c, gbc);
        }
    }
    
    protected Component addWithConstraints(Component c, GridBagConstraints constr)
    {
        layout.setConstraints(c, constr);
        return super.add(c);
    }
    
    /** Sets the first available x coord for the given y coord */
    private void setMaxGridX(int y, int maxX)
    {
        if(y >= maxGridX.length)
        {
            int[] newMaxGridX = new int[ Math.max(y+1, maxGridX.length*2) ];
            Arrays.fill(newMaxGridX, 0);
            System.arraycopy(maxGridX, 0, newMaxGridX, 0, maxGridX.length);
            maxGridX = newMaxGridX;
        }
        
        maxGridX[y] = maxX;
    }
    
    /** Sets the first available x coord for the given y coord */
    private int getMaxGridX(int y)
    {
        if(y < maxGridX.length) return maxGridX[y];
        else return 0;
    }
    
    /**
    * Starts a new row in the table.
    * All subsequently added components will be on the new row.
    */
    public void newRow()
    {
        if(subtable != null) subtable.newRow();
        else gbc.gridy += 1;
    }
    
    /**
    * Replaces <code>oldComp</code> with <code>newComp</code>,
    * assuming <code>oldComp</code> is found in a TablePane.
    * Use <code>null</code> for <code>newComp</code> to
    * remove <code>oldComp</code>.
    *
    * @return <code>true</code> on success, <code>false</code> on error
    */
    public static boolean replace(Component oldComp, Component newComp)
    {
        if(oldComp == null) return false;
        
        Container cont = oldComp.getParent();
        if(cont == null | !(cont instanceof TablePane)) return false;
        
        TablePane tp = (TablePane)cont;
        GridBagConstraints constr = tp.layout.getConstraints(oldComp);
        tp.remove(oldComp);
        
        if(newComp == null) return true;
        
        tp.addWithConstraints(newComp, constr);
        return true;
    }
//}}}

//{{{ save, restore
//##################################################################################################
    /**
    * Pushes the current alignment, insets, etc. onto a stack.
    * @return <code>this</code> (for chaining)
    */
    public TablePane save()
    {
        if(subtable != null) subtable.save();
        else gbcStack.addLast(gbc.clone());
        return this;
    }
    
    /**
    * Restores alignment, insets, etc. saved with <code>save()</code>.
    * @return <code>this</code> (for chaining)
    * @throws NoSuchElementException if nothing has been saved on the stack.
    */
    public TablePane restore()
    {
        if(subtable != null) subtable.restore();
        else gbc = (GridBagConstraints)gbcStack.removeLast();
        return this;
    }
//}}}

//{{{ weights, insets, pad
//##################################################################################################
    /**
    * Sets the weighting assigned to components,
    * which determines where extra space is allocated
    * during layout.
    * @return <code>this</code> (for chaining)
    */
    public TablePane weights(double wx, double wy)
    {
        if(subtable != null) subtable.weights(wx, wy);
        else
        {
            gbc.weightx = wx;
            gbc.weighty = wy;
        }
        return this;
    }
    
    /**
    * Sets the insets (external padding, empty space)
    * that surround components.
    * @return <code>this</code> (for chaining)
    */
    public TablePane insets(int top, int left, int bottom, int right)
    {
        if(subtable != null) subtable.insets(top, left, bottom, right);
        else
        {
            gbc.insets = new Insets(top, left, bottom, right);
        }
        return this;
    }
    
    /** Sets the insets symmetrically. */
    public TablePane insets(int tlbr)
    {
        return insets(tlbr, tlbr, tlbr, tlbr);
    }
    
    /**
    * Sets the padding, that is, the amount of space
    * that will be guaranteed for a component in
    * addition to its <code>minimumSize</code>.
    * @return <code>this</code> (for chaining)
    */
    public TablePane pad(int px, int py)
    {
        if(subtable != null) subtable.pad(px, py);
        else
        {
            gbc.ipadx = px;
            gbc.ipady = py;
        }
        return this;
    }
    
    /** Sets the padding symmetrically. */
    public TablePane pad(int pxy)
    {
        return pad(pxy, pxy);
    }
//}}}

//{{{ hfill, vfill, etc.
//##################################################################################################
    /**
    * Determines whether components expand to fill
    * the horizontal space allocated to them.
    * @return <code>this</code> (for chaining)
    */
    public TablePane hfill(boolean h)
    {
        if(subtable != null) subtable.hfill(h);
        else fill(h, isVfill());
        return this;
    }
    
    /**
    * Determines whether components expand to fill
    * the vertical space allocated to them.
    * @return <code>this</code> (for chaining)
    */
    public TablePane vfill(boolean v)
    {
        if(subtable != null) subtable.vfill(v);
        else fill(isHfill(), v);
        return this;
    }
    
    private void fill(boolean h, boolean v)
    {
        if(h)
        {
            if(v) gbc.fill = GridBagConstraints.BOTH;
            else  gbc.fill = GridBagConstraints.HORIZONTAL;
        }
        else
        {
            if(v) gbc.fill = GridBagConstraints.VERTICAL;
            else  gbc.fill = GridBagConstraints.NONE;
        }
    }
    
    private boolean isHfill()
    {
        return (gbc.fill == GridBagConstraints.HORIZONTAL || gbc.fill == GridBagConstraints.BOTH);
    }
    
    private boolean isVfill()
    {
        return (gbc.fill == GridBagConstraints.VERTICAL || gbc.fill == GridBagConstraints.BOTH);
    }
//}}}

//{{{ left, center, right, top, middle, bottom, etc.
//##################################################################################################
    /** Sets horizontal alignment to left. */
    public TablePane left()
    {
        if(subtable != null) subtable.left();
        else align(0, getValignment());
        return this;
    }
    
    /** Sets horizontal alignment to center. */
    public TablePane center()
    {
        if(subtable != null) subtable.center();
        else align(1, getValignment());
        return this;
    }
    
    /** Sets horizontal alignment to right. */
    public TablePane right()
    {
        if(subtable != null) subtable.right();
        else align(2, getValignment());
        return this;
    }
    
    /** Sets vertical alignment to top. */
    public TablePane top()
    {
        if(subtable != null) subtable.top();
        else align(getHalignment(), 0);
        return this;
    }
    
    /** Sets vertical alignment to middle. */
    public TablePane middle()
    {
        if(subtable != null) subtable.middle();
        else align(getHalignment(), 1);
        return this;
    }
    
    /** Sets vertical alignment to bottom. */
    public TablePane bottom()
    {
        if(subtable != null) subtable.bottom();
        else align(getHalignment(), 2);
        return this;
    }
    
    private void align(int h, int v)
    {
        if(h == 2)//right
        {
            if(v == 2)      gbc.anchor = GridBagConstraints.SOUTHEAST;
            else if(v == 1) gbc.anchor = GridBagConstraints.EAST;
            else            gbc.anchor = GridBagConstraints.NORTHEAST;
        }
        else if(h == 1)//center
        {
            if(v == 2)      gbc.anchor = GridBagConstraints.SOUTH;
            else if(v == 1) gbc.anchor = GridBagConstraints.CENTER;
            else            gbc.anchor = GridBagConstraints.NORTH;
        }
        else//left
        {
            if(v == 2)      gbc.anchor = GridBagConstraints.SOUTHWEST;
            else if(v == 1) gbc.anchor = GridBagConstraints.WEST;
            else            gbc.anchor = GridBagConstraints.NORTHWEST;
        }
    }    
    
    private int getHalignment()
    {
        if(gbc.anchor == GridBagConstraints.SOUTHEAST || gbc.anchor == GridBagConstraints.EAST  || gbc.anchor == GridBagConstraints.NORTHEAST)
            return 2;
        else if(gbc.anchor == GridBagConstraints.SOUTH || gbc.anchor == GridBagConstraints.CENTER || gbc.anchor == GridBagConstraints.NORTH)
            return 1;
        else
            return 0;
    }

    private int getValignment()
    {
        if(gbc.anchor == GridBagConstraints.SOUTHWEST || gbc.anchor == GridBagConstraints.SOUTH  || gbc.anchor == GridBagConstraints.SOUTHEAST)
            return 2;
        else if(gbc.anchor == GridBagConstraints.WEST || gbc.anchor == GridBagConstraints.CENTER || gbc.anchor == GridBagConstraints.EAST)
            return 1;
        else
            return 0;
    }
//}}}

//{{{ strut
//##################################################################################################
    /**
    * This is a convenience method for Box.createRigidArea().
    */
    public static Component strut(int w, int h)
    {
        return Box.createRigidArea(new Dimension(w, h));
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

