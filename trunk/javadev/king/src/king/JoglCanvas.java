// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.util.*;

import net.java.games.jogl.*;
//}}}
/**
* <code>JoglCanvas</code> is a wrapper for a Painter that uses
* the OpenGL libraries for hardware-accelerated 2D rendering
* via the JOGL Java library.
*
* <p>Despite dire warnings about mixing heavyweight and lightweight components,
* it doesn't appear to be a problem unless a lightweight component were
* supposed to paint over top of this one...
*
* <p>Painting with a Graphics2D *is* possible via a mostly-transparent
* image with its own Graphics2D.
* However, Java prefers ARGB-int graphics and OpenGL requires RGBA-byte graphics.
* For some reason, it's MUCH faster to let Java draw on the ARGB graphics and
* then map the bytes into an array ourselves than it is to draw directly on a
* BufferedImage backed by a byte array.
* The two perform at roughly comparable speeds (30 - 35 ms) if nothing is drawn.
* However, the speeds are ~50 ms vs 1-2 SECONDS if even one text string is drawn.
* <p>This mode of doing the canvas overpaint has been replaced by one that uses
* the JoglPainter directly (which then uses the GLUT font functions for text).
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Jun  5 15:47:31 EDT 2004
*/
public class JoglCanvas extends JPanel implements GLEventListener, TransformSignalSubscriber
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain    kMain;
    Engine      engine;
    ToolBox     toolbox;
    GLCanvas    canvas;
    boolean     writeFPS;
    Dimension   glSize = new Dimension();
    
    // Variables for doing text with a Graphics2D then overlaying it
    //WritableRaster      raster      = null;
    //BufferedImage       overlayImg  = null;
    //byte[]              overlayData = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public JoglCanvas(KingMain kMain, Engine engine, ToolBox toolbox)
    {
        super(new BorderLayout());
        
        this.kMain = kMain;
        this.engine = engine;
        this.toolbox = toolbox;
        
        // Create and listen to an OpenGL canvas
        GLCapabilities capabilities = new GLCapabilities();
        canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);
        canvas.addGLEventListener(this); // calls display(), reshape(), etc.
        toolbox.listenTo(canvas);
        this.add(canvas, BorderLayout.CENTER);
    }
//}}}

//{{{ init, display, reshape, displayChanged
//##############################################################################
    public void init(GLDrawable drawable)
    {}
    
    public void display(GLDrawable drawable)
    {
        GL gl = drawable.getGL();
        Kinemage kin = kMain.getKinemage();
        
        if(kin == null)
        {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        }
        else
        {
            JoglPainter painter = new JoglPainter(drawable);
            
            KingView view = kin.getCurrentView();
            Rectangle bounds = new Rectangle(this.glSize);
            if(kin.currAspect == null) engine.activeAspect = 0;
            else engine.activeAspect = kin.currAspect.getIndex().intValue();
            long timestamp = System.currentTimeMillis();
            engine.render(this, view, bounds, painter);
            timestamp = System.currentTimeMillis() - timestamp;
            if(writeFPS)
                SoftLog.err.println(timestamp+" ms ("+(timestamp > 0 ? Long.toString(1000/timestamp) : ">1000")
                    +" FPS) - "+engine.getNumberPainted()+" objects painted");
            
            if(toolbox != null)
            {
                //timestamp = System.currentTimeMillis();
                //Graphics2D g2 = setupOverlay();
                //toolbox.overpaintCanvas(g2); // This is the actual slow step, probably because of the data model.
                //gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
                //gl.glEnable(gl.GL_BLEND);
                //gl.glRasterPos2i(0, -glSize.height);
                //gl.glDrawPixels(glSize.width, glSize.height, gl.GL_RGBA, gl.GL_UNSIGNED_BYTE, getOverlayBytes());
                toolbox.overpaintCanvas(painter);
                //timestamp = System.currentTimeMillis() - timestamp;
                //if(writeFPS)
                //    SoftLog.err.println(" + "+timestamp+" ms for overpainting");
            }
        }
    }
    
    public void reshape(GLDrawable drawable, int x, int y, int width, int height)
    {
        GL gl = drawable.getGL();
        GLU glu = drawable.getGLU();
        
        this.glSize.setSize(width, height);
        gl.glViewport(0, 0, width, height); // left, right, width, height
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, width, -height, 0.0); // left, right, bottom, top
    }
    
    public void displayChanged(GLDrawable drawable, boolean modeChnaged, boolean deviceChanged)
    {}
//}}}

//{{{ getPreferred/MinimumSize, requestRepaint
//##############################################################################
    public Dimension getPreferredSize()
    {
        return kMain.getCanvas().getPreferredSize();
    }
    
    public Dimension getMinimumSize()
    {
        return kMain.getCanvas().getMinimumSize();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void requestRepaint(ActionEvent ev)
    {
        canvas.repaint();
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

//{{{ SLOW - setupOverlay, getOverlayBytes
//##############################################################################
    /*
    Graphics2D setupOverlay()
    {
        if(overlayImg == null || overlayImg.getWidth() != glSize.width || overlayImg.getHeight() != glSize.height)
        {
            // Magic spells from the "Jumping into JOGL" article
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, glSize.width, glSize.height, 4, null);
            ComponentColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8,8,8,8},
                true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            overlayImg = new BufferedImage(colorModel, raster, false, null);
        }
        
        Graphics2D g = overlayImg.createGraphics();
        
        // Wipe out all data currently in image with invisible black.
        // The for loop is MUCH faster -- maybe 10x or more.
        //g.setColor(new Color(0,0,0,0));
        //g.fillRect(0, 0, glSize.width, glSize.height);
        byte[] overlayData = ((DataBufferByte)raster.getDataBuffer()).getData();
        for(int i = 0; i < overlayData.length; i++) overlayData[i] = 0;
        
        // Compensate for OpenGL Y-axis running the other way 'round
        AffineTransform t = new AffineTransform();
        t.translate(0, glSize.height);
        t.scale(1.0, -1.0);
        g.transform(t);
        
        return g;
    }
    
    byte[] getOverlayBytes()
    {
        return ((DataBufferByte)raster.getDataBuffer()).getData();
    }
    */
//}}}

//{{{ FAST - setupOverlay, getOverlayBytes
//##############################################################################
    /*
    Graphics2D setupOverlay()
    {
        if(overlayImg == null || overlayImg.getWidth() != glSize.width || overlayImg.getHeight() != glSize.height)
        {
            overlayImg = new BufferedImage(glSize.width, glSize.height, BufferedImage.TYPE_INT_ARGB);
            int[] data = ((DataBufferInt)overlayImg.getRaster().getDataBuffer()).getData();
            overlayData = new byte[4 * data.length];
        }
        
        Graphics2D g = overlayImg.createGraphics();
        
        // Wipe out all data currently in image with invisible black.
        // The for loop is MUCH faster -- maybe 10x or more.
        //g.setColor(new Color(0,0,0,0));
        //g.fillRect(0, 0, glSize.width, glSize.height);
        int[] data = ((DataBufferInt)overlayImg.getRaster().getDataBuffer()).getData();
        for(int i = 0; i < data.length; i++) data[i] = 0;
        
        // Compensate for OpenGL Y-axis running the other way 'round
        AffineTransform t = new AffineTransform();
        t.translate(0, glSize.height);
        t.scale(1.0, -1.0);
        g.transform(t);
        
        return g;
    }
    
    byte[] getOverlayBytes()
    {
        int i = 0, j = 0;
        int[] data = ((DataBufferInt)overlayImg.getRaster().getDataBuffer()).getData();
        while(i < data.length)
        {
            int d = data[i];
            if(d == 0)
            {
                overlayData[j] = overlayData[j+1] = overlayData[j+2] = overlayData[j+3] = 0;
            }
            else // pack into RGBA order from ARGB ints
            {
                overlayData[j]   = (byte)((d>>16) & 0xff);
                overlayData[j+1] = (byte)((d>> 8) & 0xff);
                overlayData[j+2] = (byte)((d    ) & 0xff);
                overlayData[j+3] = (byte)((d>>24) & 0xff);
            }
            
            i+=1;
            j+=4;
        }
        return overlayData;
    }
    */
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

