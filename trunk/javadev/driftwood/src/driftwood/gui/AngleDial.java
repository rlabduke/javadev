// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
import java.awt.geom.*;
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
* <code>AngleDial</code> is a GUI component that looks like
* a round dial, enabling the user to select an angle.
* The function is similar to that provided by a slider but
* allows the range to wrap around.
*
* <b>Mouse wheel functionality has been removed to preserve
* compatibility with Java 1.3.</b> If you want this functionality,
* remove comments marked 'XXX-JAVA13'
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May  1 15:18:30 EDT 2003
*/
public class AngleDial extends JComponent implements MouseListener, MouseMotionListener //XXX-JAVA13//, MouseWheelListener
{
//{{{ Constants
    static final Dimension      DIAL_DIM    = new Dimension(92,92);
    static final DecimalFormat  df1         = new DecimalFormat("0.0");
    static final double         TWO_PI      = 2.0 * Math.PI;

    static final Color          backColor   = new Color(0xFFFFFF);
    static final Color          activeBack  = new Color(0xFFFFCC);
    static final Color          offBack     = new Color(0xCCCCCC);
    static final Color          ghostColor  = new Color(0xBBBBBB);
    static final Color          currColor   = new Color(0x0000CC);
    static final Color          activeCurr  = new Color(0xFF6600);
    static final Color          foreColor   = new Color(0x000000);
    static final Color          offFore     = new Color(0x999999);
//}}}

//{{{ INTERFACE: Formatter
//##################################################################################################
    /** Allows a user-specified component to decide how the angle is rendered. */
    public static interface Formatter
    {
        /** Returns a String representation of a given angle (in radians) */
        public String formatAngle(double angle);
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    /**
    * Variables that describe the geometry of the dial
    * within our allotted display space.
    * These are updated every time we paint().
    * They are used by the mouse handlers.
    */
    int size, top, left, cx, cy;
    
    /** The font we use for painting */
    Font        font;
    FontMetrics metrics = null;
    
    /** Cached Shape objects we use for painting */
    Line2D.Double       line2d      = new Line2D.Double();
    Ellipse2D.Double    ellipse2d   = new Ellipse2D.Double();
    BasicStroke         pen1        = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    BasicStroke         pen2        = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    Rectangle           plusBtn     = new Rectangle();
    Rectangle           minusBtn    = new Rectangle();
    
    /** The optional element that formats angle values */
    Formatter   formatter = null;
    
    /** If true, paint the original angle on the dial too */
    boolean     paintOrigAngle = true;
    
    /**
    * The actual values that are tracked by this component.
    * NO ONE should modify these except set(Orig)Radians()!
    */
    double currAngleValue, origAngleValue;
    
    /** Number of mouse movement pixels equal to one full rotation */
    int mouseSensitivity = 3600; // one pixel == 0.1 degree
    
    /** The amount the angle changes in response to arrow keys, etc */
    double stepSize = TWO_PI / 360.0; // one degree
    
    /** True if user is dragging the mouse */
    boolean isUpdating = false;
    
    /** State of the system when the user started dragging the mouse */
    double dragStartAngle = 0;              // == getRadians() when drag started
    int dragStartX = 0, dragStartY = 0;     // x,y where mouse touched down
    double dragAngleOffset = 0;             // atan2 of where mouse touched down
    
    /** List of listeners for ChangeEvents */
    Collection changeListeners = new ArrayList();
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public AngleDial()
    {
        this(0, 0);
    }
    
    /**
    * Constructor
    * @param start the starting value for this dial, in radians
    * @param orig  the original/bookmark value for this dial, in radians
    */
    public AngleDial(double start, double orig)
    {
        super();
        
        this.currAngleValue = start;
        this.origAngleValue = orig;
        
        this.setPreferredSize(DIAL_DIM);
        this.setMinimumSize(DIAL_DIM);
        this.setOpaque(false); // we're not (logically) rectangular
        
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        //XXX-JAVA13//this.addMouseWheelListener(this);
        
        this.font = new Font("SansSerif", Font.PLAIN, 18);
    }
//}}}

//{{{ paintComponent
//##################################################################################################
    /** Paints our component to the screen */
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g); // this does nothing b/c we have no UI delegate
        Graphics2D g2 = (Graphics2D)g;
        
        Dimension   dim     = this.getSize();
        g.setFont(font);
        if(metrics == null) metrics = g.getFontMetrics();
        
        size    = Math.min(dim.width, dim.height) - 1;
        left    = (dim.width  - size)   / 2;
        top     = (dim.height - size)   / 2;
        cx      = dim.width / 2;
        cy      = dim.height/ 2;
        
        //SoftLog.err.println("dim="+dim+"; size="+size+"; top="+top+"; left="+left+"; cx="+cx+"; cy="+cy);
        final boolean isOff = !this.isEnabled();
        
        // Paint the background
        g2.setStroke(pen1);
        if(isOff)           g.setColor(offBack);
        else if(isUpdating) g.setColor(activeBack);
        else                g.setColor(backColor);
        ellipse2d.setFrame(left, top, size, size);
        g2.fill(ellipse2d);
        
        // Paint the old-value marker
        g2.setStroke(pen2);
        double origAngle = this.getOrigRadians();
        if(isOff)   g.setColor(offFore);
        else        g.setColor(ghostColor);
        line2d.setLine(cx, cy,
            cx+(int)(0.5*size*Math.cos(origAngle)),
            cy-(int)(0.5*size*Math.sin(origAngle)));
        g2.draw(line2d);
        
        // Print the current value, centered and either above or below center
        double currAngle = this.getRadians();
        String msg  = (formatter == null ? defaultFormatAngle(currAngle) : formatter.formatAngle(currAngle));
        int textx   = cx - metrics.stringWidth(msg)/2;
        int texty   = (currAngle > Math.PI ? cy-4 : cy+2+metrics.getMaxAscent());
        if(isOff)   g.setColor(offFore);
        else        g.setColor(foreColor);
        g.drawString(msg, textx, texty);
        
        // Paint the original value under the sweep line
        if(getPaintOrigAngle())
        {
            msg  = (formatter == null ? defaultFormatAngle(origAngle) : formatter.formatAngle(origAngle));
            textx   = cx - metrics.stringWidth(msg)/2;
            texty   = (currAngle > Math.PI ? cy+2+metrics.getMaxAscent() : cy-4);
            if(isOff)   g.setColor(offFore);
            else        g.setColor(ghostColor);
            g.drawString(msg, textx, texty);
        }
        
        // Paint the new-value marker
        g2.setStroke(pen2);
        if(isOff)           g.setColor(offFore);
        else if(isUpdating) g.setColor(activeCurr);
        else                g.setColor(currColor);
        line2d.setLine(cx, cy,
            cx+(int)(0.5*size*Math.cos(currAngle)),
            cy-(int)(0.5*size*Math.sin(currAngle)));
        g2.draw(line2d);
        
        // Paint the outer ring
        g2.setStroke(pen1);
        if(isOff)   g.setColor(offFore);
        else        g.setColor(foreColor);
        g2.draw(ellipse2d);
        
        // Position the + and - buttons
        int btnSize = (int)(0.146 * size) - 2;
        plusBtn.setBounds(left, top+size-btnSize, btnSize, btnSize);
        minusBtn.setBounds(left+size-btnSize, top+size-btnSize, btnSize, btnSize);
        
        // Paint the + and - buttons
        g2.setStroke(pen1);
        if(isOff)           g.setColor(offBack);
        else                g.setColor(backColor);
        g2.fill(plusBtn);
        g2.fill(minusBtn);
        
        if(isOff)   g.setColor(offFore);
        else        g.setColor(foreColor);
        g2.draw(plusBtn);
        g2.draw(minusBtn);
        
        g2.setStroke(pen2);
        line2d.setLine(plusBtn.x+3, plusBtn.y+plusBtn.height/2.0,
            plusBtn.x+plusBtn.width-3, plusBtn.y+plusBtn.height/2.0);
        g2.draw(line2d);
        line2d.setLine(plusBtn.x+plusBtn.width/2.0, plusBtn.y+3,
            plusBtn.x+plusBtn.width/2.0, plusBtn.y+plusBtn.height-3);
        g2.draw(line2d);
        line2d.setLine(minusBtn.x+3, minusBtn.y+minusBtn.height/2.0,
            minusBtn.x+minusBtn.width-3, minusBtn.y+minusBtn.height/2.0);
        g2.draw(line2d);
    }
//}}}

//{{{ defaultFormatAngle
//##################################################################################################
    /** Writes the angle in degrees on (-180,180] with one digit after the decimal */
    protected String defaultFormatAngle(double angle)
    {
        angle = Math.toDegrees(angle);
        if(angle > 180.0) angle -= 360.0;
        return df1.format(angle);
    }
//}}}

//{{{ mouseClicked, mousePressed, mouseReleased
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        // Plus button
        if(plusBtn.contains(ev.getPoint()))
        {
            setDegrees(getDegrees() + 360.0 / mouseSensitivity);
        }
        else if(minusBtn.contains(ev.getPoint()))
        {
            setDegrees(getDegrees() - 360.0 / mouseSensitivity);
        }
        // Reset to original value on double click
        else if(this.isEnabled() && ev.getClickCount() == 2)
        {
            setRadians(getOrigRadians());
        }
    }

    public void mouseEntered(MouseEvent ev)
    {}
    public void mouseExited(MouseEvent ev)
    {}
    
    public void mousePressed(MouseEvent ev)
    {
        isUpdating      = true;
        dragStartAngle  = getRadians();
        dragStartX      = ev.getX();
        dragStartY      = ev.getY();
        dragAngleOffset = Math.atan2(cy-dragStartY, dragStartX-cx);
    }
    
    public void mouseReleased(MouseEvent ev)
    {
        isUpdating      = false;
        if(this.isEnabled() && getRadians() != dragStartAngle)
            fireStateChanged();
    }
//}}}

//{{{ mouseDragged, mouseWheelMoved
//##################################################################################################
    public void mouseDragged(MouseEvent ev)
    {
        if(this.isEnabled())
        {
            // RMB/Shift does O-style pseudo dials
            if(SwingUtilities.isRightMouseButton(ev)  || ev.isShiftDown())
            {
                int pixelsMoved = (ev.getX() - dragStartX) - (ev.getY() - dragStartY);
                setRadians(dragStartAngle + (TWO_PI*pixelsMoved)/mouseSensitivity);
            }
            // Normal control just follows the mouse pointer
            else
            {
                int dx, dy;
                dx = ev.getX() - cx;
                dy = ev.getY() - cy;
                setRadians(dragStartAngle + Math.atan2(-dy, dx) - dragAngleOffset);
            }
        }
    }
    
    public void mouseMoved(MouseEvent ev)
    {}

    /* XXX-JAVA13 * /
    public void mouseWheelMoved(MouseWheelEvent ev)
    {
        setRadians(getRadians() - stepSize*ev.getWheelRotation());
    }
    /* XXX-JAVA13 */
//}}}

//{{{ add/removeChangeListener, fireStateChanged
//##################################################################################################
    public void addChangeListener(ChangeListener l)
    {
        changeListeners.add(l);
    }
    
    public void removeChangeListener(ChangeListener l)
    {
        changeListeners.remove(l);
    }
    
    /** Notifies all listeners and repaints this component */
    protected void fireStateChanged()
    {
        ChangeEvent ev = new ChangeEvent(this);
        for(Iterator iter = changeListeners.iterator(); iter.hasNext(); )
        {
            ((ChangeListener)iter.next()).stateChanged(ev);
        }
        this.repaint();
    }
//}}}

//{{{ get/set{Radians, Degrees, OrigRadians, OrigDegrees}
//##################################################################################################
    /** Returns the currently selected angle in radians, between 0 and 2*pi */
    public double getRadians()
    { return currAngleValue; }
    public void setRadians(double v)
    {
        v = v % TWO_PI;
        if(v < 0) v += TWO_PI;
        
        if(currAngleValue != v)
        {
            currAngleValue = v;
            fireStateChanged();
        }
    }
    
    /** Returns the currently selected angle in degrees, between 0 and 360 */
    public double getDegrees()
    { return Math.toDegrees(this.getRadians()); }
    public void setDegrees(double v)
    { this.setRadians(Math.toRadians(v)); }

    /** Returns the original angle in radians, between 0 and 2*pi */
    public double getOrigRadians()
    { return origAngleValue; }
    public void setOrigRadians(double v)
    {
        v = v % TWO_PI;
        if(v < 0) v += TWO_PI;
        
        if(origAngleValue != v)
        {
            origAngleValue = v;
            fireStateChanged();
        }
    }
    
    /** Returns the original angle in degrees, between 0 and 360 */
    public double getOrigDegrees()
    { return Math.toDegrees(this.getOrigRadians()); }
    public void setOrigDegrees(double v)
    { this.setOrigRadians(Math.toRadians(v)); }
//}}}

//{{{ get/set{Sensitivity, StepSize, PaintOrigAngle}
//##################################################################################################
    public int getSensitivity()
    { return mouseSensitivity; }
    /**
    * Sets the number of pixels worth of mouse movement
    * that registers as a full rotation of the dial.
    * The default is 3600, one-tenth degree per pixel.
    */
    public void setSensitivity(int s)
    { mouseSensitivity = s; }
    
    public double getStepSize()
    { return stepSize; }
    /**
    * Sets the size of one step, in radians.
    * A step is the amount the value changes in
    * response to the mouse wheel or arrow keys.
    * Default is Math.PI/180.0, one degree.
    */
    public void setStepSize(double s)
    { stepSize = s; }
    
    public boolean getPaintOrigAngle()
    { return paintOrigAngle; }
    /**
    * Sets whether the original angle
    * should be painted on the dial.
    */
    public void setPaintOrigAngle(boolean b)
    { paintOrigAngle = b; this.repaint(); }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ isFocusable, getValueIsAdjusting
//##################################################################################################
    // Has been replaced with isFocusable() in 1.4+
    public boolean isFocusTraversable()
    { return true; }
    public boolean isFocusable()
    { return true; }
    
    public boolean getValueIsAdjusting()
    { return isUpdating; }
//}}}
}//class

