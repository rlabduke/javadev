// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.painters.*;

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

import java.util.List;
//}}}
/**
* <code>KinCanvas</code> is the display surface for the 3-D model.
*
* <p>Begun on Mon Apr 22 17:19:48 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class KinCanvas extends JComponent implements KMessage.Subscriber, Transformable, ChangeListener, Printable
{
//{{{ Static fields
    static final double LOG_2 = Math.log(2.0);
    static final int SLIDER_SCALE = 16;
    
    public static final int QUALITY_GOOD        = 0;
    public static final int QUALITY_BETTER      = 1;
    public static final int QUALITY_BEST        = 2;
    public static final int QUALITY_JOGL        = 10;
//}}}

//{{{ Variables
//##################################################################################################
    KingMain            kMain           = null;
    Engine2D            engine          = null;
    ToolBox             toolbox         = null;
    
    StandardPainter     goodPainter     = new StandardPainter(false);
    StandardPainter     betterPainter   = new StandardPainter(true);
    HighQualityPainter  bestPainter     = new HighQualityPainter(true);
    Component           joglCanvas      = null;
    ReflectiveAction    joglAction      = null;

    DefaultBoundedRangeModel zoommodel  = null;
    DefaultBoundedRangeModel clipmodel  = null;
    Image logo = null;
    
    int renderQuality = QUALITY_GOOD;
    boolean writeFPS;
    
    String currViewKey;
    String currAspectKey;
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
        this.currViewKey = this.getClass().getName()+"@"+System.identityHashCode(this)+".current_view";
        this.currAspectKey = this.getClass().getName()+"@"+System.identityHashCode(this)+".current_aspect";
        
        // Set default graphics mode for OS X
        // This partly compensates for broken graphics primitives
        // on Mac implementations through at least 1.4.1...
        try
        {
            String os = System.getProperty("os.name").toLowerCase();
            if(os.indexOf("mac") != -1 || os.indexOf("apple") != -1)
                renderQuality = QUALITY_BETTER;
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
        
        engine = new Engine2D();
        updatePrefs(engine, props);
        logo = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("images/king-logo.gif"));
        
        toolbox = new ToolBox(kMain, this);
        toolbox.listenTo(this);
        
        // If we do this here, everything JOGL needs is already created. I think.
        // It seems to work, at least, which isn't true if this code comes earlier!
        if(kMain.getPrefs().getBoolean("joglByDefault"))
        {
            try
            {
                //SoftLog.err.println("Trying to init OpenGL...");
                //this.setQuality(QUALITY_JOGL); -- generates an error dialog
                this.loadJOGL();
                this.renderQuality = QUALITY_JOGL;
            }
            catch(Throwable t) {}//{ t.printStackTrace(SoftLog.err); }
        }
        
        // Java 1.4+ only! - adds support for Drag & Drop to the canvas
        new FileDropHandler(kMain, this);

        kMain.subscribe(this);
    }
//}}}
    
//{{{ updatePrefs, syncToKin
//##################################################################################################
    void updatePrefs(Engine2D engine, Props prefs)
    {
        engine.setPickingRadius( prefs.getDouble("pickingRadius") );
        engine.useObjPicking    = prefs.getBoolean("pickObjects");
        engine.stereoRotation   = (float)Math.toRadians(prefs.getDouble("stereoAngle"));
        engine.bigFont          = new Font("SansSerif", Font.PLAIN, prefs.getInt("fontSizeBig"));
        engine.smallFont        = new Font("SansSerif", Font.PLAIN, prefs.getInt("fontSizeSmall"));
    }
    
    /** Takes needed display settings from the kinemage */
    public static void syncToKin(Engine engine, Kinemage kin)
    {
        engine.usePerspective   = kin.atPerspective;
        engine.cueThickness     = ! kin.atOnewidth;
        engine.thinLines        = kin.atThinline;
        engine.whiteBackground  = kin.atWhitebackground;
        engine.colorByList      = kin.atListcolordominant;
    }
//}}}

//{{{ deliverMessage
//##################################################################################################
    public void deliverMessage(KMessage msg)
    {
        // Notify children
        Kinemage kin = kMain.getKinemage();
        if(engine != null)
        {
            if(msg.testProg(KMessage.PREFS_CHANGED))
                updatePrefs(engine, kMain.getPrefs());
            if(msg.testProg(KMessage.KIN_CLOSED | KMessage.ALL_CLOSED))
                engine.flushZBuffer(); // prevents memory leaks
            if(kin != null && msg.testProg(KMessage.KIN_SWITCHED))
                syncToKin(engine, kin);
        }
        //if(toolbox != null) toolbox.notifyChange(event_mask);

        // Take care of yourself
        this.repaint();
        
        KView view = kMain.getView();
        if(view != null && msg.testProg(KMessage.KIN_SWITCHED | KMessage.VIEW_SELECTED))
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
        paintCanvas(g2, this.getSize(), renderQuality);
    }

    /** Does the rendering, with Engine.render() doing most of the real work. */
    public void paintCanvas(Graphics2D g2, Dimension dim, int quality)
    {
        /* A quick query for analyzing graphics performance
        java.awt.image.VolatileImage vi = createVolatileImage(getWidth(), getHeight());
        if(vi != null)
        {
            ImageCapabilities ic = vi.getCapabilities();
            SoftLog.err.println("isAccelerated = "+ic.isAccelerated()+"; isTrueVolatile = "+ic.isTrueVolatile());
        }*/
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null)
        {
            g2.setColor(Color.black);
            g2.fillRect(0, 0, dim.width, dim.height);
            if(logo != null) g2.drawImage(logo, (dim.width-logo.getWidth(this))/2, (dim.height-logo.getHeight(this))/2, this);
            if(kMain.getPrefs().newerVersionAvailable())
                announceNewVersion(g2);
        }
        // This is not the usual way in which the JOGL canvas is redrawn,
        // and in fact, it may NEVER get called any more because the actual
        // KinCanvas object is never displayed while the JOGL canvas is.
        else if(quality >= QUALITY_JOGL && joglAction != null)
            joglAction.actionPerformed(null);
        else
        {
            Painter painter = null;
            if(quality == QUALITY_BETTER)
            {
                betterPainter.setGraphics(g2);
                painter = betterPainter;
            }
            else if(quality == QUALITY_BEST)
            {
                bestPainter.setGraphics(g2);
                painter = bestPainter;
            }
            else //(quality == QUALITY_GOOD)
            {
                goodPainter.setGraphics(g2);
                painter = goodPainter;
            }
            
            long timestamp = System.currentTimeMillis();
            KView view = getCurrentView();
            Rectangle bounds = new Rectangle(dim);
            syncToKin(engine, kin);
            engine.render(this, view, bounds, painter);
            if(toolbox != null) toolbox.overpaintCanvas(painter);
            timestamp = System.currentTimeMillis() - timestamp;
            if(writeFPS)
                SoftLog.err.println(timestamp+" ms ("+(timestamp > 0 ? Long.toString(1000/timestamp) : ">1000")
                    +" FPS) - "+engine.getNumberPainted()+" objects painted");
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
        g2.drawString(msg, (d.width - (int)r.getWidth())/2, (d.height - (int)r.getHeight())/2 + 170);
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
        Dimension dim = getCanvasSize();

        Kinemage kin = kMain.getKinemage();
        if(kin == null || pageindex > 0) return NO_SUCH_PAGE; 
        else
        {
            KView view = getCurrentView();
            
            // Scale the paper to match the graphics window:
            double scale = Math.min( (double)(format.getImageableWidth() / dim.width),
                                     (double)(format.getImageableHeight()/dim.height) );
            g2.scale(scale, scale);
            g2.setClip((int)(format.getImageableX()/scale), (int)(format.getImageableY()/scale), dim.width, dim.height);
            
            Rectangle bounds = new Rectangle(dim);
            bounds.setLocation((int)(format.getImageableX()/scale), (int)(format.getImageableY()/scale));
            syncToKin(engine, kin);
            bestPainter.setGraphics(g2);
            engine.render(this, view, bounds, bestPainter);
            if(toolbox != null) toolbox.overpaintCanvas(bestPainter);
            
            return PAGE_EXISTS;
        }
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
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        kin.doTransform(engine, xform);
        if(toolbox != null) toolbox.doTransform(engine, xform);
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
    
    /** The size of the drawing surface (!= this.getSize() in OpenGL mode) */
    public Dimension getCanvasSize()
    { return engine.getCanvasSize(); };
    
    /** Returns the drawing engine this canvas uses for rendering. */
    public Engine2D getEngine() { return engine; }
    
    /** Returns the toolbox used for interacting with this canvas. */
    public ToolBox getToolBox() { return toolbox; }
    
    /** Sets the rendering quality to one of the QUALITY_XXX constants */
    public void setQuality(int q)
    {
        renderQuality = q;
        
        // JOGL canvas is reusable in some situations but not others:
        // eg it crashes on FC2 with Java 1.5. For some reason, we must set the
        // graphics component here -- it doesn't work in paintComponent()!
        if(q == QUALITY_JOGL)// && joglCanvas == null)
        {
            try { loadJOGL(); }
            catch(Throwable t)
            {
                t.printStackTrace(SoftLog.err);
                joglCanvas = null;
                joglAction = null;
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Unable to initialize OpenGL graphics.\nSee user manual for details on enabling this feature.",
                    "No OpenGL", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(q < QUALITY_JOGL)
            kMain.getContentPane().setGraphicsComponent(this);
    }
//}}}

//{{{ get/setCurrentView/Aspect
//##################################################################################################
    /**
    * Returns this canvas' current view of the current kinemage, or null
    * if there is no current kinemage.
    */
    public KView getCurrentView()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return null;
        
        KView view = (KView) kin.metadata.get(currViewKey);
        if(view == null)
        {
            List<KView> views = kin.getViewList();
            if(views.size() < 1)
                kin.addView( new KView(kin) );
            view = views.get(0);
            setCurrentView(view);
        }
        return view;
    }
    
    /**
    * Assigns a new view object as the current view
    * and notifies all listeners.
    * Actually, the view object gets cloned so future
    * modifications don't change the original.
    */
    public void setCurrentView(KView view)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        kin.metadata.put(currViewKey, view.clone());
        kMain.publish(new KMessage(this, KMessage.VIEW_SELECTED));
    }
    
    /** Returns the currently active aspect for this view, or null if none. */
    public Aspect getCurrentAspect()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return null;
        
        return (Aspect) kin.metadata.get(currAspectKey);
    }

    /**
    * Assigns a new aspect object as the current one
    * (null is also OK) and notifies all listeners.
    */
    public void setCurrentAspect(Aspect aspect)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        kin.metadata.put(currAspectKey, aspect);
        kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
    }
    
//}}}

//{{{ stateChanged
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        KView view = kMain.getView();
        if(view == null) return;
        
        if(ev.getSource() == zoommodel)
        {
            double kinspan = kMain.getKinemage().getSpan();
            double newspan = kinspan / Math.pow(2, (double)zoommodel.getValue() / (double)SLIDER_SCALE);
            view.setSpan((float)newspan);
            kMain.publish(new KMessage(this, KMessage.VIEW_MOVED));
        }
        else if(ev.getSource() == clipmodel)
        {
            double newclip = (double)clipmodel.getValue() / 200.0;
            view.setClip((float)newclip);
            kMain.publish(new KMessage(this, KMessage.VIEW_MOVED));
        }
    }
//}}}

//{{{ isFocusTraversable
//##################################################################################################
    // Has been replaced with isFocusable() in 1.4+
    public boolean isFocusTraversable()
    { return true; }
    public boolean isFocusable()
    { return true; }
//}}}

//{{{ repaint, loadJOGL
//##################################################################################################
    /** To ensure OpenGL painting is done even when the canvas is hidden. */
    public void repaint()
    {
        super.repaint();
        if(renderQuality >= QUALITY_JOGL && joglAction != null)
        {
            // Is this check really needed?
            if(kMain.getContentPane().getGraphicsComponent() != joglCanvas)
                kMain.getContentPane().setGraphicsComponent(joglCanvas);
            
            joglAction.actionPerformed(null);
        }
    }
    
    // lazily loads the JOGL Painter just before we need it
    private void loadJOGL() throws Throwable
    {
        // Try to create a JOGL painter, via reflection
        Class joglClass = Class.forName("king.JoglCanvas");
        Constructor joglConstr = joglClass.getConstructor(new Class[] { KingMain.class, Engine2D.class, ToolBox.class });
        joglCanvas = (Component)joglConstr.newInstance(new Object[] { kMain, engine, toolbox });
        joglAction = new ReflectiveAction(null, null, joglCanvas, "requestRepaint");
    }
//}}}

//{{{ empty
//##################################################################################################
//}}}
}//class
