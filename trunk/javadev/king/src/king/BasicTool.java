// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
 * <code>BasicTool</code> implements the most common manipulation functions.
 * It is intended to serve as a basis for the construction of new tools, which should override
 * one or more of the xx_click(), xx_drag(), and xx_wheel() functions.
 * Note that some of these functions have default activities;
 * descendents may want to replace one or more with null functions.
 *
 * <p>Begun on Fri Jun 21 09:30:40 EDT 2002
 * <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class BasicTool extends Plugin implements MouseListener, MouseMotionListener, Transformable, WindowListener
{
//{{{ Static fields
    static final Object MODE_UNDECIDED  = new Object();
    static final Object MODE_VERTICAL   = new Object();
    static final Object MODE_HORIZONTAL = new Object();
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected int lastXCoord = 0, lastYCoord = 0;
    protected int pressXCoord = 0, pressYCoord = 0;
    protected boolean isNearTop = false, isNearBottom = false;
    protected Object mouseDragMode = MODE_UNDECIDED;
    
    protected JDialog       dialog          = null;
    private   boolean       hasBeenCentered = false;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public BasicTool(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ start/stop/reset functions
//##################################################################################################
    public void start()
    {
        show();
    }
    
    public void stop()
    {
        hide();
    }
    
    public void reset()
    {}
//}}}

//{{{ initDialog, show, hide
//##################################################################################################
    protected void initDialog()
    {
        Container content = this.getToolPanel();
        if(content == null) return;
        
        dialog = new JDialog(kMain.getTopWindow(), this.toString(), false); // false => not modal
        //dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(this); // to make the window close button work
        dialog.setContentPane(content);
        //dialog.invalidate();
        //dialog.validate();
        dialog.pack();
    }
    
    protected void show()
    {
        if(dialog == null) initDialog();
        if(dialog == null) return;
        
        if(!dialog.isVisible())
        {
            dialog.pack();
            Container w = kMain.getContentContainer();
            if(w != null)
            {
                Point p = w.getLocation();
                Dimension dimDlg = dialog.getSize();
                Dimension dimWin = w.getSize();
                p.x += dimWin.width - (dimDlg.width / 2) ;
                p.y += (dimWin.height - dimDlg.height) / 2;
                dialog.setLocation(p);
            }
            dialog.setVisible(true);
        }
        else
        {
            dialog.toFront();
        }
    }
    
    protected void hide()
    {
        if(dialog != null) dialog.setVisible(false);
    }
//}}}

//{{{ xx_click functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        services.pick(p);
        
        if(p != null && p.getComment() != null)
            clickActionHandler(p.getComment());
    }
    
    /** Override this function for right-button/shift clicks */
    public void s_click(int x, int y, KPoint p, MouseEvent ev)
    {
        services.centerOnPoint(p);
    }
    
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    { click(x, y, p, ev); }
    /** Override this function for shift-control clicks */
    public void sc_click(int x, int y, KPoint p, MouseEvent ev)
    { click(x, y, p, ev); }
//}}}

//{{{ xx_drag functions
//##################################################################################################
    /** Override this function for (left-button) drags */
    public void drag(int dx, int dy, MouseEvent ev)
    {
        if(services.doFlatland.isSelected())
        {
            if(isNearTop)   services.ztranslate(dx);
            else            services.translate(dx, dy);
        }
        else
        {
            if(isNearTop)   services.pinwheel(dx);
            else            services.rotate(dx, dy);
        }
    }
    
    /** Override this function for right-button/shift drags */
    public void s_drag(int dx, int dy, MouseEvent ev)
    {
        if(mouseDragMode == MODE_VERTICAL)          services.adjustZoom(dy);
        else if(mouseDragMode == MODE_HORIZONTAL)   services.adjustClipping(dx);
    }
    
    /** Override this function for middle-button/control drags */
    public void c_drag(int dx, int dy, MouseEvent ev)
    {
        if(isNearTop)   services.ztranslate(dx);
        else            services.translate(dx, dy);
    }

    /** Override this function for shift-control drags */
    public void sc_drag(int dx, int dy, MouseEvent ev)
    {
        // Like normal rotation, but can only rotate about Y axis
        if(isNearTop)   services.pinwheel(dx);
        else            services.rotate(dx, 0);
    }
//}}}

//{{{ xx_wheel functions
//##################################################################################################
    /** Override this function for mouse wheel motion */
    public void wheel(int rotation, MouseEvent ev)
    {
        services.adjustZoom(rotation*18f);
    }
    /** Override this function for mouse wheel motion with shift down */
    public void s_wheel(int rotation, MouseEvent ev)
    {
        services.adjustClipping(-rotation*18f);
    }
    /** Override this function for mouse wheel motion with control down */
    public void c_wheel(int rotation, MouseEvent ev)
    { wheel(rotation, ev); }
    /** Override this function for mouse wheel motion with shift AND control down */
    public void sc_wheel(int rotation, MouseEvent ev)
    { s_wheel(rotation, ev); }
//}}}

//{{{ clickActionHandler
//##################################################################################################
    /**
    * This function gets called by BasicTool.click() iff the currently selected
    * point has a point comment associated with it. Subclasses that wish to
    * implement custom actions (which match <code>/^[a-zA-Z]+:.+/</code> )
    * stored in point comments can override this method.
    * @param comment    the point comment for the picked point
    * @return true if the action was handled, false if not
    * @since 1.34
    */
    protected boolean clickActionHandler(String comment)
    {
        if(comment.startsWith("http:"))
        {
            try
            {
                URL url;
                // Saying "http:foo/bar.html" allows us to use relative URLs in applets
                if(!comment.startsWith("http://") && kMain.getApplet() != null)
                    url = new URL(kMain.getApplet().getDocumentBase(), comment.substring(5));
                else url = new URL(comment);
                new HTMLHelp(kMain, url).show();
                return true;
            }
            catch(MalformedURLException ex)
            {
                SoftLog.err.println("Bad HTTP URL in point comment: "+comment);
                return false;
            }
        }
        else return false;
    }
//}}}

//{{{ Mouse motion listeners
//##################################################################################################
    /**
    * This function is compatible with Java 1.3, so
    * we can define keyboard equivalents, etc.
    */
    public void mouseWheelMoved(MouseEvent ev, int rotation)
    {
        boolean isShift, isCtrl;
        isShift = ev.isShiftDown();
        isCtrl  = ev.isControlDown();
        
        if(isShift && isCtrl)   sc_wheel(rotation, ev);
        else if(isCtrl)         c_wheel(rotation, ev);
        else if(isShift)        s_wheel(rotation, ev);
        else                    wheel(rotation, ev);
    }
    
    public void mouseDragged(MouseEvent ev)
    {
        Dimension dim   = kCanvas.getCanvasSize();
        Point where     = ev.getPoint();
        int dx, dy;
        dx = where.x - lastXCoord;
        dy = where.y - lastYCoord;
        
        // Force a (strong?) committment to either horizonal
        // or vertical motion before we take action
        if(mouseDragMode == MODE_UNDECIDED)
        {
            int tdx = Math.abs(where.x - pressXCoord);
            int tdy = Math.abs(where.y - pressYCoord);
            if(tdx/2 >= tdy+1)      mouseDragMode = MODE_HORIZONTAL;
            else if(tdy/2 >= tdx+1) mouseDragMode = MODE_VERTICAL;
        }
        
        boolean isShift, isCtrl, isShiftCtrl;
        isShift     = SwingUtilities.isRightMouseButton(ev)  || ev.isShiftDown();
        isCtrl      = SwingUtilities.isMiddleMouseButton(ev) || ev.isControlDown();
        isShiftCtrl = (isShift && isCtrl) || (SwingUtilities.isLeftMouseButton(ev) && SwingUtilities.isRightMouseButton(ev));
            
        if(isShiftCtrl)     sc_drag(dx, dy, ev);
        else if(isCtrl)     c_drag(dx, dy, ev);
        else if(isShift)    s_drag(dx, dy, ev);
        else                drag(dx, dy, ev);
        
        lastXCoord = where.x;
        lastYCoord = where.y;
    }
    
    public void mouseMoved(MouseEvent ev)
    {}
//}}}

//{{{ Mouse click listners
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        // Handle picking, etc. here
        int x = ev.getX(), y = ev.getY();
        KPoint p = null;
        if(kMain.getKinemage() != null)
            p = kCanvas.getEngine().pickPoint(x, y, services.doSuperpick.isSelected());
        // otherwise, we just create a nonsensical warning message about stereo picking
        
        /* Mouse debugging * /
        // Java:    RMB == Meta, MMB == Alt
        // Apple:   RMB == Control (not in Java?) (2 btn but not 3 btn mouse?)
        if(SwingUtilities.isLeftMouseButton(ev))    SoftLog.err.print("Left ");
        if(SwingUtilities.isMiddleMouseButton(ev))  SoftLog.err.print("Middle ");
        if(SwingUtilities.isRightMouseButton(ev))   SoftLog.err.print("Right ");
        if(ev.isShiftDown())                        SoftLog.err.print("Shift ");
        if(ev.isControlDown())                      SoftLog.err.print("Control ");
        if(ev.isMetaDown())                         SoftLog.err.print("Meta ");
        if(ev.isAltDown())                          SoftLog.err.print("Alt ");
        SoftLog.err.println(Integer.toBinaryString(ev.getModifiers()));
        /* Mouse debugging */
        
        boolean isShift, isCtrl;
        isShift = SwingUtilities.isRightMouseButton(ev)  || ev.isShiftDown();
        isCtrl  = SwingUtilities.isMiddleMouseButton(ev) || ev.isControlDown();
        
        // This "if" statement is to correct for a Java 1.5 issue where 
        // pressing mouse and releasing counts as a click as well, so
        // using the right mouse to zoom would also recenter if you released
        // over a point.  See java bug # 5039416.
        if(mouseDragMode.equals(MODE_UNDECIDED))
        {
            if(isShift && isCtrl)   sc_click(x, y, p, ev);
            else if(isCtrl)         c_click(x, y, p, ev);
            else if(isShift)        s_click(x, y, p, ev);
            else                    click(x, y, p, ev);
        }
    }

    public void mouseEntered(MouseEvent ev)
    {}
    public void mouseExited(MouseEvent ev)
    {}
    
    public void mousePressed(MouseEvent ev)
    {
        Dimension dim = kCanvas.getCanvasSize();
        
        isNearBottom = isNearTop = false;
        if(ev.getY() < (dim.height / 6)) isNearTop = true;
        else if(ev.getY() > ((dim.height * 5) / 6)) isNearBottom = true;

        Point where     = ev.getPoint();
        pressXCoord     = lastXCoord = where.x;
        pressYCoord     = lastYCoord = where.y;
        mouseDragMode   = MODE_UNDECIDED;
    }
    
    public void mouseReleased(MouseEvent ev)
    {}
//}}}

//{{{ Window listeners
//##################################################################################################
    public void windowActivated(WindowEvent ev)   {}
    public void windowClosed(WindowEvent ev)      {}
    public void windowClosing(WindowEvent ev)     { parent.activateDefaultTool(); }
    public void windowDeactivated(WindowEvent ev) {}
    public void windowDeiconified(WindowEvent ev) {}
    public void windowIconified(WindowEvent ev)   {}
    public void windowOpened(WindowEvent ev)      {}
//}}}

//{{{ onArrowUp/Down/Right/Left
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowUp(ActionEvent ev)
    {
        // fake event, except for modifiers
        MouseEvent mev = new MouseEvent(kCanvas, 0, 0, ev.getModifiers(), 0, 0, 0, false);
        mouseWheelMoved(mev, -1);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowDown(ActionEvent ev)
    {
        // fake event, except for modifiers
        MouseEvent mev = new MouseEvent(kCanvas, 0, 0, ev.getModifiers(), 0, 0, 0, false);
        mouseWheelMoved(mev, 1);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowRight(ActionEvent ev)
    {
        KView v = kMain.getView();
        if(v == null) return;
        v.rotateY((float)(Math.PI/180.0) * 2f);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowLeft(ActionEvent ev)
    {
        KView v = kMain.getView();
        if(v == null) return;
        v.rotateY((float)(Math.PI/180.0) * -2f);
    }
//}}}
    
//{{{ doTransform
//##################################################################################################
    /**
    * A call to this method indicates the subscriber
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * <p>This method will be called in response to TransformSignal.doTransform().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void doTransform(Engine engine, Transform xform)
    {
    }
//}}}
    
//{{{ overpaintCanvas
//##################################################################################################
    /**
    * Called by KinCanvas after all kinemage painting is complete,
    * this gives the tools a chance to write additional info
    * (e.g., point IDs) to the graphics area.
    * @param painter    the Painter that can paint on the current canvas
    */
    public void overpaintCanvas(Painter painter)
    {
    }
//}}}

//{{{ getToolsMenuItem, onToolActivate, getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /**
    * Creates a new JRadioButtonMenuItem to be displayed in the Tools menu,
    * which will allow the user to access function(s) associated
    * with this Tool.
    *
    * A tool that wants to return null here should probably be a Plugin instead.
    * @return a JRadioButtonMenuItem (not just a regular JMenuItem)
    */
    public JMenuItem getToolsMenuItem()
    {
        JRadioButtonMenuItem btn = new JRadioButtonMenuItem(
            new ReflectiveAction(this.toString(), null, this, "onToolActivate"));
        btn.setSelected(false);
        return btn;
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onToolActivate(ActionEvent ev)
    {
        parent.toolActivated(this);
    }

    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return null; }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this tool. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#navigate-tool" (or null)
    */
    public String getHelpAnchor()
    { return "#navigate-tool"; }
    
    public String toString() { return "Navigate"; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class
