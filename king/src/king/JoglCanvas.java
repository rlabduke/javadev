// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.painters.JoglPainter;
import king.painters.JoglRenderer;
import king.painters.StandardPainter;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.util.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.awt.*;
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
* <p>In the future, it would be nice to use the com.sun.opengl.util.j2d.Overlap
* class that appeared in JOGL 1.1.0, but for now we're using the hack method
* above to not screw people up who have JOGL 1.0.0 installed.
* (Especially since 1.1.0 is still in the Release Candidate stage.)
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
    KingMain        kMain;
    Engine2D        engine;
    ToolBox         toolbox;
    GLCanvas        canvas;
    JoglRenderer    renderer;
    Dimension       glSize = new Dimension();
    
    // Variables for doing text with a Graphics2D then overlaying it
    WritableRaster      raster      = null;
    BufferedImage       overlayImg  = null;
    ByteBuffer          overlayData = null;
    Image               logo        = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public JoglCanvas(KingMain kMain, Engine2D engine, ToolBox toolbox)
    {
        super(new BorderLayout());
        
        this.kMain = kMain;
        this.engine = engine;
        this.toolbox = toolbox;
        
        // Not guaranteed to load fully before returning -- gives blank screen.
        this.logo = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("images/king-logo.2021.png"));
        // Loads fully before returning:
        this.logo = new ImageIcon(this.getClass().getResource("images/king-logo.2021.png")).getImage();
        
        // Java 1.4+ only! - adds support for Drag & Drop to the canvas
        kMain.getFileDropHandler().handleDropsFor(this);
        
        // Create and listen to an OpenGL canvas
        GLProfile profile = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDoubleBuffered(true); // usually enabled by default, but to be safe...
        capabilities.setDepthBits(24); // hardware depth buffer for VBO renderer
        
        int fsaaNumSamples = kMain.getPrefs().getInt("joglNumSamples");
        capabilities.setSampleBuffers(fsaaNumSamples > 1); // enables/disables full-scene antialiasing (FSAA)
        capabilities.setNumSamples(fsaaNumSamples); // sets number of samples for FSAA (default is 2)

        //canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);
        canvas = new GLCanvas(capabilities);

        canvas.addGLEventListener(this); // calls display(), reshape(), etc.
        canvas.addMouseListener(this); // cursor related; see this.mouseEntered().
        toolbox.listenTo(canvas);
        this.add(canvas, BorderLayout.CENTER);

        //float[] result = canvas.getCurrentSurfaceScale(new float[2]);
        //System.out.println("currsurfscale: "+Arrays.toString(result));
        //canvas.setSurfaceScale(new float[] {4.0f, 4.0f}); 
        //result = canvas.getNativeSurfaceScale(new float[2]);
        //System.out.println("nativesurfscale: "+Arrays.toString(result));

    }
//}}}

//{{{ init, dispose, display, reshape, displayChanged
//##############################################################################
    public void init(GLAutoDrawable drawable)
    {
        GL2 gl = (GL2)drawable.getGL();
        renderer = new JoglRenderer();
        renderer.init(gl);
    }

    public void dispose(GLAutoDrawable drawable)
    {
        GL2 gl = (GL2)drawable.getGL();
        if(renderer != null) renderer.dispose(gl);
    }
    
    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = (GL2)drawable.getGL();
        Kinemage kin = kMain.getKinemage();

        Dimension kCanvasDim = kMain.getCanvas().getPreferredSize();
        Dimension glCanvasDim = glSize;
        if (!kCanvasDim.equals(glCanvasDim)) {
          float heightScale = (float)kCanvasDim.getHeight()/(float)glCanvasDim.getHeight();
          float widthScale = (float)kCanvasDim.getWidth()/(float)glCanvasDim.getWidth();
          canvas.setSurfaceScale(new float[] {widthScale, heightScale});
        }

        if(kin == null)
        {
            // Blank screen
            //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            //gl.glClear(GL.GL_COLOR_BUFFER_BIT);

            // KiNG logo and new version availability
            // Use logical pixel dimensions so the logo appears at the correct
            // visual size on HiDPI displays, then scale to the physical framebuffer.
            Dimension logicalDim = kCanvasDim;
            Graphics2D g2 = setupOverlay(logicalDim);
            gl.glRasterPos2d(0, -glSize.height); // position at bottom-left in physical coords
            g2.setColor(Color.black);
            g2.fillRect(0, 0, logicalDim.width, logicalDim.height);
            if(logo != null) g2.drawImage(logo, (logicalDim.width-logo.getWidth(this))/2, (logicalDim.height-logo.getHeight(this))/2, this);
            if(kMain.getPrefs().newerVersionAvailable())
                announceNewVersion(g2);
            g2.dispose();
            // Scale from logical pixels to physical framebuffer pixels
            float zoomX = (float)glSize.width  / (float)logicalDim.width;
            float zoomY = (float)glSize.height / (float)logicalDim.height;
            gl.glPixelZoom(zoomX, zoomY);
            gl.glDrawPixels(logicalDim.width, logicalDim.height,
                GL.GL_RGBA, GL.GL_UNSIGNED_BYTE , getOverlayBytes());
            gl.glPixelZoom(1.0f, 1.0f); // reset
        }
        else
        {
            long timestamp = System.currentTimeMillis();
            KView view = kMain.getView();
            Rectangle bounds = new Rectangle(this.glSize);
            kMain.getCanvas().syncToKin(engine, kin);

            if(renderer != null && renderer.isReady())
            {
                // Set engine state normally done in engine.render()
                // Use logical dims so getCanvasSize() matches mouse events
                engine.setCanvasSize(kCanvasDim.width, kCanvasDim.height);
                engine.whiteBackground = kin.atWhitebackground;
                engine.backgroundMode = engine.whiteBackground ? KPaint.WHITE_COLOR : KPaint.BLACK_COLOR;
                engine.markerSize = (engine.bigMarkers ? 2 : 1);
                engine.labelFont = (engine.bigLabels ? engine.bigFont : engine.smallFont);

                // VBO-based rendering path
                renderer.render(kin, view, bounds, gl, engine);

                // CPU transform for picking support.
                // Use logical pixel dims so pick coords match mouse events.
                engine.transform(this, view, new Rectangle(kCanvasDim));

                // Toolbox overpaint (point IDs, distances, auger circle, etc.)
                // Render to a transparent overlay at logical pixel dims using
                // Java2D (StandardPainter) for proper font scaling on HiDPI,
                // then composite onto the GL framebuffer with glPixelZoom.
                if(toolbox != null)
                {
                    Dimension logicalDim = kCanvasDim;
                    Graphics2D g2 = setupOverlay(logicalDim);
                    StandardPainter overlayPainter = new StandardPainter(true);
                    overlayPainter.setGraphics(g2);
                    toolbox.overpaintCanvas(overlayPainter);
                    g2.dispose();

                    gl.glDisable(GL.GL_DEPTH_TEST);
                    gl.glEnable(GL.GL_BLEND);
                    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                    gl.glRasterPos2d(0, -glSize.height);
                    float zoomX = (float)glSize.width  / (float)logicalDim.width;
                    float zoomY = (float)glSize.height / (float)logicalDim.height;
                    gl.glPixelZoom(zoomX, zoomY);
                    gl.glDrawPixels(logicalDim.width, logicalDim.height,
                        GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getOverlayBytes());
                    gl.glPixelZoom(1.0f, 1.0f);
                }
            }
            else
            {
                // Fallback: original immediate-mode path
                JoglPainter painter = new JoglPainter(drawable);
                engine.render(this, view, bounds, painter);
                if(toolbox != null) toolbox.overpaintCanvas(painter);
            }

            timestamp = System.currentTimeMillis() - timestamp;
            if(kMain.getCanvas().writeFPS)
                SoftLog.err.println(timestamp+" ms ("+(timestamp > 0 ? Long.toString(1000/timestamp) : ">1000")
                    +" FPS) - "+engine.getNumberPainted()+" objects painted");
        }

    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        GL2 gl = (GL2)drawable.getGL();
        //GLU glu = drawable.getGLU();
        GLU glu = new GLU();
        
        //System.out.println("reshape width: "+width+" height: "+height);
        //Dimension dim = kMain.getCanvas().getPreferredSize();
        //this.glSize.setSize(dim.getWidth(), dim.getHeight());

        this.glSize.setSize(width, height);
        gl.glViewport(0, 0, width, height); // left, right, width, height
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, width, -height, 0.0); // left, right, bottom, top
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
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
    Graphics2D setupOverlay()
    { return setupOverlay(glSize); }

    Graphics2D setupOverlay(Dimension size)
    {
        if(overlayImg == null || overlayImg.getWidth() != size.width || overlayImg.getHeight() != size.height)
        {
            overlayImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
            int[] data = ((DataBufferInt)overlayImg.getRaster().getDataBuffer()).getData();
            overlayData = ByteBuffer.allocate(4 * data.length);
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
        t.translate(0, size.height);
        t.scale(1.0, -1.0);
        g.transform(t);

        return g;
    }
    
    ByteBuffer getOverlayBytes()
    {
        overlayData.clear();
        int i = 0;
        int[] data = ((DataBufferInt)overlayImg.getRaster().getDataBuffer()).getData();
        while(i < data.length)
        {
            int d = data[i];
            //if(d == 0)
            //{
            //    overlayData[j] = overlayData[j+1] = overlayData[j+2] = overlayData[j+3] = 0;
            //}
            //else // pack into RGBA order from ARGB ints
            //{
                overlayData.put((byte)((d>>16) & 0xff));
                overlayData.put((byte)((d>> 8) & 0xff));
                overlayData.put((byte)((d    ) & 0xff));
                overlayData.put((byte)((d>>24) & 0xff));
            //}
            i+=1;
        }
        overlayData.rewind(); // or else clients try to read from where we stopped writing
        return overlayData;
    }
//}}}

//{{{ announceNewVersion
//##################################################################################################
    void announceNewVersion(Graphics2D g2)
    {
        String msg = "A new version of KiNG is now available";
        Dimension d = this.glSize;
        Font font = new Font("SansSerif", Font.BOLD, 16);
        g2.setFont(font);
        g2.setColor(Color.white);
        FontMetrics metrics = g2.getFontMetrics();
        Rectangle2D r = metrics.getStringBounds(msg, g2);
        g2.drawString(msg, (d.width - (int)r.getWidth())/2, (d.height - (int)r.getHeight())/2 + 210);
    }
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

