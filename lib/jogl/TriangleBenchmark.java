import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.event.*; 
import java.util.Random;
import net.java.games.jogl.*;

// import javax.swing.*;

/** This appears to be approximately 100x faster than the equivalent Java code for drawing. */

public class TriangleBenchmark extends Frame implements GLEventListener, MouseListener
{
    
    public static final Dimension PREFERRED_FRAME_SIZE = new Dimension (400,400);
    
    GLCanvas canvas;
    
    public TriangleBenchmark() {
        // init Frame
        super ("Simple 2D with JOGL");
        System.out.println ("constructor");
        // get a GLCanvas
        GLCapabilities capabilities = new GLCapabilities();
        canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);
        // add a GLEventListener, which will get called when the
        // canvas is resized or needs a repaint
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        // now add the canvas to the Frame.  Note we use BorderLayout.CENTER
        // to make the canvas stretch to fill the container (ie, the frame)
        add (canvas, BorderLayout.CENTER);
    }
    
    /** We'd like to be 600x400, please.
     */
    public Dimension getPreferredSize () {
        return PREFERRED_FRAME_SIZE;
    }
    
    /** main just creates and shows a TriangleBenchmark Frame
     */
    public static void main (String[] args) {
        Frame f = new TriangleBenchmark();
        f.pack();
        f.setVisible(true);
    }
    
    public void mouseClicked(MouseEvent ev)
    {
        System.out.println("mouseClicked() in thread "+Thread.currentThread());
        canvas.repaint();
    }

    public void mouseEntered(MouseEvent ev)
    {}
    public void mouseExited(MouseEvent ev)
    {}
    public void mousePressed(MouseEvent ev)
    {}
    public void mouseReleased(MouseEvent ev)
    {}
    
    /*
     * METHODS DEFINED BY GLEventListener
     */
    
    /** Called by drawable to initiate drawing 
     */
    public void display (GLDrawable drawable) {
        System.out.println ("display() in thread "+Thread.currentThread());
        GL gl = drawable.getGL();
        GLU glu = drawable.getGLU(); // is this needed?
        paintComponent(gl);
    }
    
    protected void paintComponent(GL gl)
    {
        gl.glClearColor( 1.0f, 1.0f, 1.0f, 1.0f ); //white 
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        
        // Antialiasing - doesn't work!
        //gl.glEnable(gl.GL_BLEND);
        //gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
        //gl.glBlendFunc(gl.GL_SRC_ALPHA_SATURATE, gl.GL_ONE);
        //gl.glEnable(gl.GL_POINT_SMOOTH);
        //gl.glEnable(gl.GL_LINE_SMOOTH);
        //gl.glEnable(gl.GL_POLYGON_SMOOTH);
        
        Dimension dim = PREFERRED_FRAME_SIZE;
        
        // Generate all polygons first to isolate that
        // from graphics performance.
        final int num = 10000;
        final Color[] colorset = {
            new Color(255, 0, 0),
            new Color(255, 128, 0),
            new Color(255, 255, 0),
            new Color(0, 255, 0),
            new Color(0, 255, 255),
            new Color(0, 0, 255),
            new Color(255, 0, 255)
        };
        int[][] xs = new int[num][3];
        int[][] ys = new int[num][3];
        Color[] cs = new Color[num];
        Random rand = new Random();
        for(int i = 0; i < num; i++)
        {
            xs[i][0] = rand.nextInt(dim.width);
            xs[i][1] = rand.nextInt(dim.width);
            xs[i][2] = rand.nextInt(dim.width);
            ys[i][0] = rand.nextInt(dim.height);
            ys[i][1] = rand.nextInt(dim.height);
            ys[i][2] = rand.nextInt(dim.height);
            cs[i]    = colorset[ rand.nextInt(colorset.length) ];
        }
        
        // Time how long it takes to draw them all
        long time = System.currentTimeMillis();
        //gl.glBegin(GL.GL_TRIANGLES); not the slightest bit faster!
        for(int i = 0; i < num; i++)
        {
            //g.setColor(cs[i]);
            gl.glColor3f(cs[i].getRed()/255f, cs[i].getGreen()/255f, cs[i].getBlue()/255f);

            //g.fillPolygon(xs[i], ys[i], 3);
            gl.glBegin(GL.GL_POLYGON);
            gl.glVertex2i(xs[i][0], ys[i][0]);
            gl.glVertex2i(xs[i][1], ys[i][1]);
            gl.glVertex2i(xs[i][2], ys[i][2]);
            gl.glEnd();
        }
        //gl.glEnd();
        time = System.currentTimeMillis() - time;
        System.err.println("Drew "+num+" triangles in "+time+" ms");
    }

    /** Called by drawable to indicate mode or device has changed
     */
    public void displayChanged (GLDrawable drawable,
                                boolean modeChanged,
                                boolean deviceChanged) {
        System.out.println ("displayChanged()");
    }
    
    /** Called after OpenGL is init'ed
     */
    public void init (GLDrawable drawable) {
        System.out.println ("init()");
        
        GL gl = drawable.getGL(); 
        // set erase color
        gl.glClearColor( 1.0f, 1.0f, 1.0f, 1.0f ); //white 
        // set drawing color and point size
        gl.glColor3f( 0.0f, 0.0f, 0.0f ); 
        gl.glPointSize(4.0f); //a 'dot' is 4 by 4 pixels 
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized
     */
    public void reshape (GLDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        System.out.println ("reshape()");
        GL gl = drawable.getGL(); 
        GLU glu = drawable.getGLU(); 
        gl.glViewport( 0, 0, width, height ); 
        gl.glMatrixMode( GL.GL_PROJECTION );  
        gl.glLoadIdentity(); 
        glu.gluOrtho2D( 0.0, 400.0, 0.0, 400.0); 
    }
}
