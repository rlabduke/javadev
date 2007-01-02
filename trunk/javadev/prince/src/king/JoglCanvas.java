// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.painters.JoglPainter;

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

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
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
* <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Jun  5 15:47:31 EDT 2004
*/
public class JoglCanvas extends JPanel implements GLEventListener, Transformable, MouseListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain    kMain;
    Engine2D    engine;
    ToolBox     toolbox;
    GLCanvas    canvas;
    Dimension   glSize = new Dimension();
    
    // Variables for doing text with a Graphics2D then overlaying it
    //WritableRaster      raster      = null;
    //BufferedImage       overlayImg  = null;
    //byte[]              overlayData = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public JoglCanvas(KingMain kMain, Engine2D engine, ToolBox toolbox)
    {
        super(new BorderLayout());
        
        this.kMain = kMain;
        this.engine = engine;
        this.toolbox = toolbox;
        
        // Java 1.4+ only! - adds support for Drag & Drop to the canvas
        new FileDropHandler(kMain, this);
        
        // Create and listen to an OpenGL canvas
        GLCapabilities capabilities = new GLCapabilities();
        capabilities.setDoubleBuffered(true); // usually enabled by default, but to be safe...
        
        int fsaaNumSamples = kMain.getPrefs().getInt("joglNumSamples");
        capabilities.setSampleBuffers(fsaaNumSamples > 1); // enables/disables full-scene antialiasing (FSAA)
        capabilities.setNumSamples(fsaaNumSamples); // sets number of samples for FSAA (default is 2)

        //canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);
        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this); // calls display(), reshape(), etc.
        canvas.addMouseListener(this); // cursor related; see this.mouseEntered().
        toolbox.listenTo(canvas);
        this.add(canvas, BorderLayout.CENTER);

    }
//}}}

//{{{ init, display, reshape, displayChanged
//##############################################################################
    public void init(GLAutoDrawable drawable)
    {}
    
    public void display(GLAutoDrawable drawable)
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
            
            long timestamp = System.currentTimeMillis();
            KView view = kMain.getView();
            Rectangle bounds = new Rectangle(this.glSize);
            KinCanvas.syncToKin(engine, kin);
            engine.render(this, view, bounds, painter);
            if(toolbox != null) toolbox.overpaintCanvas(painter);
            timestamp = System.currentTimeMillis() - timestamp;
            if(kMain.getCanvas().writeFPS)
                SoftLog.err.println(timestamp+" ms ("+(timestamp > 0 ? Long.toString(1000/timestamp) : ">1000")
                    +" FPS) - "+engine.getNumberPainted()+" objects painted");
        }
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        GL gl = drawable.getGL();
        //GLU glu = drawable.getGLU();
        GLU glu = new GLU();
        
        this.glSize.setSize(width, height);
        gl.glViewport(0, 0, width, height); // left, right, width, height
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, width, -height, 0.0); // left, right, bottom, top
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChnaged, boolean deviceChanged)
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

//{{{ doTransform
//##################################################################################################
    public void doTransform(Engine engine, Transform xform)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        kin.doTransform(engine, xform);
        if(toolbox != null) toolbox.doTransform(engine, xform);
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

//{{{ Mouse listeners (for cursor)
//##################################################################################################
    public void mouseEntered(MouseEvent ev)
    {
        // This is the only thing that removes the <-|-> cursor from the split pane.
        // Forces update of cursor for top level native peer (i.e. window, not GL canvas):
        this.setCursor( Cursor.getDefaultCursor() );
        //canvas.setCursor( Cursor.getDefaultCursor() ); // not needed
    }
    
    public void mouseExited(MouseEvent ev)      {}
    public void mousePressed(MouseEvent ev)     {}
    public void mouseReleased(MouseEvent ev)    {}
    public void mouseClicked(MouseEvent ev)     {}
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

