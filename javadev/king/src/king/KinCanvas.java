// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
//import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.*;
//}}}
/**
* <code>KinCanvas</code> is the display surface for the 3-D model.
*
* <p>Begun on Mon Apr 22 17:19:48 EDT 2002
* <br>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
*/
public class KinCanvas extends JComponent implements TransformSignalSubscriber, ChangeListener, Printable
{
//{{{ Static fields
    static final double LOG_2 = Math.log(2.0);
    static final int SLIDER_SCALE = 16;
//}}}

//{{{ Variables
//##################################################################################################
    KingMain kMain = null;
    Engine engine = null;
    ToolBox toolbox = null;

    DefaultBoundedRangeModel zoommodel = null;
    DefaultBoundedRangeModel clipmodel = null;
    Image logo = null;
    
    int renderQuality = Engine.QUALITY_GOOD;
    boolean writeFPS;
//}}}
    
//{{{ Constructor
//##################################################################################################
    /**
    * Creates a new drawing surface that displays the given set of visible lists.
    *
    * @param kmain the program instance that owns this canvas
    */
    public KinCanvas(KingMain kmain)
    {
        super();
        kMain = kmain;
        
        // Set default graphics mode for OS X
        // This partly compensates for broken graphics primitives
        // on Mac implementations through at least 1.4.1...
        try
        {
            String os = System.getProperty("os.name").toLowerCase();
            if(os.indexOf("mac") != -1 || os.indexOf("apple") != -1)
                renderQuality = Engine.QUALITY_BETTER;
        }
        catch(SecurityException ex) { SoftLog.err.println(ex.getMessage()); }
        
        // Determine the screen size:
        Rectangle screenBounds =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        int screenSize = (3*Math.min(screenBounds.width, screenBounds.height)) / 4;
        
        Props props = kMain.getPrefs();
        Dimension canvasSize = new Dimension(screenSize, screenSize);
        Dimension minCanvasSize = new Dimension(200, 200);
        if(props.hasProperty("graphicsWidth" ) && props.getInt("graphicsWidth")  >= minCanvasSize.width)
            canvasSize.width  = props.getInt("graphicsWidth");
        if(props.hasProperty("graphicsHeight") && props.getInt("graphicsHeight") >= minCanvasSize.height)
            canvasSize.height = props.getInt("graphicsHeight");
        writeFPS = props.getBoolean("writeFPS", false);
        
        // This will be overriden in KingMain if we're a
        // webpage-embedded applet
        setPreferredSize(canvasSize);
        setMinimumSize(minCanvasSize);
        setOpaque(true);
        
        zoommodel = new DefaultBoundedRangeModel(0, 0, -3*SLIDER_SCALE, 7*SLIDER_SCALE);
        zoommodel.addChangeListener(this);
        clipmodel = new DefaultBoundedRangeModel(200, 0, 0, 800);
        clipmodel.addChangeListener(this);
        
        engine = new Engine();
        engine.updatePrefs(kMain.prefs); // set font sizes etc
        logo = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("images/king-logo.gif"));
        
        // We do this to enable mouse wheel in Java 1.4 without
        // causing a NoClassDefFoundError in Java 1.3.
        // For ToolBoxMW to not be loaded automatically,
        // there must not be any "hard" references to it from
        // main(). Thus, we use reflection instead.
        // A similar approach is used for XML.        
        if(System.getProperty("java.version").compareTo("1.4") >= 0)
        {
            try
            {
                // What I'd like to do:
                //ToolBoxMW toolboxmw = new ToolBoxMW(kMain, this);
                //addMouseWheelListener(toolboxmw);
                //toolbox = toolboxmw;
                
                // Create a ToolBoxMW instance
                Class mwClass = Class.forName("king.ToolBoxMW");
                Constructor mwConstr = mwClass.getConstructor(new Class[] { KingMain.class, KinCanvas.class });
                toolbox = (ToolBox)mwConstr.newInstance(new Object[] { kMain, this });
                
                // Call addMouseWheelListener on this KinCanvas.
                Class kcClass = this.getClass();
                Method addWheelListener = kcClass.getMethod("addMouseWheelListener",
                    new Class[] { Class.forName("java.awt.event.MouseWheelListener") });
                addWheelListener.invoke(this, new Object[] {toolbox});
            }
            catch(Throwable t)
            {
                t.printStackTrace(SoftLog.err);
                toolbox = new ToolBox(kMain, this);
            }
        }
        else
        {
            toolbox = new ToolBox(kMain, this);
        }
        addMouseListener(toolbox);
        addMouseMotionListener(toolbox);
        
        // Arrow keys do Y-rotation (for Bryan)
        ActionMap am = this.getActionMap();
        InputMap  im = this.getInputMap(JComponent.WHEN_FOCUSED);
        // This version doesn't work, for unknown reasons.
        //JComponent contentPane = kMain.getContentPane();
        //ActionMap am = contentPane.getActionMap();
        //InputMap im = contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Action arrowUp    = new ReflectiveAction("", null, toolbox, "onArrowUp" );
        Action arrowDown  = new ReflectiveAction("", null, toolbox, "onArrowDown" );
        Action arrowLeft  = new ReflectiveAction("", null, toolbox, "onArrowLeft" );
        Action arrowRight = new ReflectiveAction("", null, toolbox, "onArrowRight");
        
        // Register listeners for arrows with all combinations of Shift and Ctrl
        am.put("arrow-up",  arrowUp );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP , 0), "arrow-up" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP , KeyEvent.SHIFT_MASK), "arrow-up" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP , KeyEvent.CTRL_MASK), "arrow-up" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP , KeyEvent.SHIFT_MASK|KeyEvent.CTRL_MASK), "arrow-up" );
        am.put("arrow-down",  arrowDown );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN , 0), "arrow-down" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN , KeyEvent.SHIFT_MASK), "arrow-down" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN , KeyEvent.CTRL_MASK), "arrow-down" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN , KeyEvent.SHIFT_MASK|KeyEvent.CTRL_MASK), "arrow-down" );
        am.put("arrow-left",  arrowLeft );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT , 0), "arrow-left" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT , KeyEvent.SHIFT_MASK), "arrow-left" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT , KeyEvent.CTRL_MASK), "arrow-left" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT , KeyEvent.SHIFT_MASK|KeyEvent.CTRL_MASK), "arrow-left" );
        am.put("arrow-right",  arrowRight );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT , 0), "arrow-right" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT , KeyEvent.SHIFT_MASK), "arrow-right" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT , KeyEvent.CTRL_MASK), "arrow-right" );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT , KeyEvent.SHIFT_MASK|KeyEvent.CTRL_MASK), "arrow-right" );
    }
//}}}
    
//{{{ notifyChange
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    void notifyChange(int event_mask)
    {
        // Notify children
        if(engine != null)
        {
            if((event_mask & KingMain.EM_PREFS) != 0)
                engine.updatePrefs(kMain.prefs);
            if((event_mask & (KingMain.EM_CLOSE|KingMain.EM_CLOSEALL)) != 0)
                engine.flushZBuffer(); // prevents memory leaks
        }
        if(toolbox != null) toolbox.notifyChange(event_mask);

        // Take care of yourself
        if(event_mask != 0) this.repaint();
        
        KingView view = kMain.getView();
        if(view != null && (event_mask & (KingMain.EM_NEWVIEW | KingMain.EM_SWITCH)) != 0)
        {
            double viewspan = view.getSpan();
            double kinspan  = kMain.getKinemage().getSpan();
            
            zoommodel.setValue((int)Math.round((double)SLIDER_SCALE * Math.log(kinspan/viewspan) / LOG_2));
            clipmodel.setValue((int)(view.getClip() * 200.0));
        }
    }
//}}}

//{{{ painting
//##################################################################################################
    /** Override of JPanel.paintComponent -- wrapper for paintCanvas. */
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        paintCanvas(g2, renderQuality);
    }

    /** Does the rendering, with Engine.render() doing most of the real work. */
    public void paintCanvas(Graphics2D g2, int quality)
    {
        /* A quick query for analyzing graphics performance
        java.awt.image.VolatileImage vi = createVolatileImage(getWidth(), getHeight());
        if(vi != null)
        {
            ImageCapabilities ic = vi.getCapabilities();
            SoftLog.err.println("isAccelerated = "+ic.isAccelerated()+"; isTrueVolatile = "+ic.isTrueVolatile());
        }*/
        
        Dimension dim = getSize();
        Kinemage kin = kMain.getKinemage();
        if(kin == null)
        {
            g2.setColor(Color.black);
            g2.fillRect(0, 0, dim.width, dim.height);
            if(logo != null) g2.drawImage(logo, (dim.width-logo.getWidth(this))/2, (dim.height-logo.getHeight(this))/2, this);
            if(kMain.getPrefs().newerVersionAvailable())
                announceNewVersion(g2);
        }
        else
        {
            KingView view = kin.getCurrentView();
            Rectangle bounds = new Rectangle(dim);
            if(kin.currAspect == null) engine.activeAspect = 0;
            else engine.activeAspect = kin.currAspect.getIndex().intValue();
            long timestamp = System.currentTimeMillis();
            engine.render(this, view, bounds, g2, quality);
            timestamp = System.currentTimeMillis() - timestamp;
            if(writeFPS)
                SoftLog.err.println(timestamp+" ms ("+(timestamp > 0 ? Long.toString(1000/timestamp) : ">1000")+" FPS)");
            if(toolbox != null) toolbox.overpaintCanvas(g2);
        }
    }
//}}}
    
//{{{ announceNewVersion
//##################################################################################################
    void announceNewVersion(Graphics2D g2)
    {
        String msg = "A new version of KiNG is now available";
        Dimension d = this.getSize();
        Font font = new Font("SansSerif", Font.BOLD, 16);
        g2.setFont(font);
        g2.setColor(Color.white);
        FontMetrics metrics = g2.getFontMetrics();
        Rectangle2D r = metrics.getStringBounds(msg, g2);
        g2.drawString(msg, (d.width - (int)r.getWidth())/2, (d.height - (int)r.getHeight())/2 + 110);
    }
//}}}

//{{{ printing
//##################################################################################################
    /**
    * Printer callback -- calls KingRenderingEngine to do the real work.
    * This code was copied directly from paintComponent
    */
    public int print(Graphics g, PageFormat format, int pageindex)
    {
        Graphics2D g2 = (Graphics2D)g;
        Dimension dim = getSize();

        Kinemage kin = kMain.getKinemage();
        if(kin == null || pageindex > 0) return NO_SUCH_PAGE; 
        else
        {
            KingView view = kin.getCurrentView();
            
            // Scale the paper to match the graphics window:
            double scale = Math.min( (double)(format.getImageableWidth() / dim.width),
                                     (double)(format.getImageableHeight()/dim.height) );
            g2.scale(scale, scale);
            g2.setClip((int)(format.getImageableX()/scale), (int)(format.getImageableY()/scale), dim.width, dim.height);
            
            Rectangle bounds = new Rectangle(dim);
            bounds.setLocation((int)(format.getImageableX()/scale), (int)(format.getImageableY()/scale));
            
            if(kin.currAspect == null) engine.activeAspect = 0;
            else engine.activeAspect = kin.currAspect.getIndex().intValue();
            
            engine.render(this, view, bounds, g2, Engine.QUALITY_BEST);
            if(toolbox != null) toolbox.overpaintCanvas(g2);
            
            return PAGE_EXISTS;
        }
    }
//}}}

//{{{ signalTransform
//##################################################################################################
    /**
    * A call to this method indicates the subscriber
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * <p>This method will be called in response to TransformSignal.signalTransform().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void signalTransform(Engine engine, Transform xform)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        kin.signalTransform(engine, xform);
        if(toolbox != null) toolbox.signalTransform(engine, xform);
    }
//}}}
    
//{{{ get/set functions
//##################################################################################################
    public BoundedRangeModel getZoomModel() { return zoommodel; }
    public BoundedRangeModel getClipModel() { return clipmodel; }
    
    /** Returns a button that arms the pick-center function */
    public Component getPickcenterButton() { return toolbox.services.doPickcenter; }
    
    /** Returns a button that shows markers */
    public Component getMarkersButton() { return toolbox.services.doMarkers; }
    
    /** Returns the drawing engine this canvas uses for rendering. */
    public Engine getEngine() { return engine; }
    
    /** Returns the toolbox used for interacting with this canvas. */
    public ToolBox getToolBox() { return toolbox; }
    
    /** Sets the rendering quality to one of the Engine.QUALITY_XXX constants */
    public void setQuality(int q)
    { renderQuality = q; }
//}}}

//{{{ stateChanged
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        KingView view = kMain.getView();
        if(view == null) return;
        
        if(ev.getSource() == zoommodel)
        {
            double kinspan = kMain.getKinemage().getSpan();
            double newspan = kinspan / Math.pow(2, (double)zoommodel.getValue() / (double)SLIDER_SCALE);
            view.setSpan((float)newspan);
            this.repaint();
        }
        else if(ev.getSource() == clipmodel)
        {
            double newclip = (double)clipmodel.getValue() / 200.0;
            view.setClip((float)newclip);
            this.repaint();
        }
    }
//}}}

//{{{ isFocusTraversable()
//##################################################################################################
    // Has been replaced with isFocusable() in 1.4+
    public boolean isFocusTraversable()
    { return true; }
    public boolean isFocusable()
    { return true; }
//}}}

//{{{ empty
//##################################################################################################
//}}}
}//class
