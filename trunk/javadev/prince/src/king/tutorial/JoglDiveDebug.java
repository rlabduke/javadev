// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tutorial;

import king.core.*;
import king.points.*;
import king.painters.*;
import driftwood.r3.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
//}}}
/**
* <code>JoglDiveDebug</code> is used for testing out rendering methods for
* the Duke DiVE (6-sided VR cave).
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec  8 13:27:16 EST 2006
*/
public class JoglDiveDebug extends JFrame implements ActionListener
{
//{{{ CLASS: Screen
//##############################################################################
    public class Screen implements GLEventListener
    {
        public GLCanvas         canvas  = null;
        public JoglEngine3D     engine  = null;
        public Dimension        glSize  = new Dimension();
        
        public Screen(GLCapabilities capabilities)
        {
            canvas = new GLCanvas(capabilities);
            canvas.addGLEventListener(this); // calls display(), reshape(), etc.
            engine = new JoglEngine3D();
            engine.usePerspective = true;
            engine.clippingMultiplier = 100;
        }
        
        public void init(GLAutoDrawable drawable)
        {}
        
        public void display(GLAutoDrawable drawable)
        {
            GL gl = drawable.getGL();
            engine.render(kin, view, new Rectangle(glSize), gl, eyePos);
        }
        
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
        {
            this.glSize.setSize(width, height);
        }
        
        public void displayChanged(GLAutoDrawable drawable, boolean modeChnaged, boolean deviceChanged)
        {}
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    Kinemage            kin     = null;
    KView               view    = null;
    Triple              eyePos  = new Triple(0, 0, 0);
    Screen[]            screens = null;
    Timer               timer   = null;
//}}}

//{{{ main, Constructor(s)
//##############################################################################
    static public void main(String[] args) { new JoglDiveDebug(); }
    
    public JoglDiveDebug()
    {
        super("JoglDiveDebug");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setUndecorated(true); // no window border or controls
        
        kin = createKinemage();
        view = new KView(kin);
        view.setSpan(1.2f * view.getSpan());
        
        createScreens();
        Container cp = new Panel();
        cp.setLayout(null);
        for(Screen s : screens)
            if(s != null) cp.add(s.canvas);
        this.setContentPane(cp);

        this.pack();
        this.show();
        
        // Only way to hide the mouse cursor in Java -- make it transparent.
        int[] pixels = new int[16 * 16];
        Image image = Toolkit.getDefaultToolkit().createImage(new java.awt.image.MemoryImageSource(16, 16, pixels, 0, 16));
        Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisiblecursor");
        this.setCursor(transparentCursor);
        
        // Puts the window in full screen mode.  Seems to work OK with JOGL.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        gd.setFullScreenWindow(this); // should be done after becoming visible

        timer = new Timer(1000 / 30, this);
        timer.start();
    }
//}}}
    
//{{{ createScreens
//##############################################################################
    void createScreens()
    {
        // Set up parameters for rendering
        GLCapabilities capabilities = new GLCapabilities();
        capabilities.setDoubleBuffered(true); // usually enabled by default, but to be safe...
        int fsaaNumSamples = 4;
        capabilities.setSampleBuffers(fsaaNumSamples > 1); // enables/disables full-scene antialiasing (FSAA)
        capabilities.setNumSamples(fsaaNumSamples); // sets number of samples for FSAA (default is 2)

        // Allocate screens[]
        this.screens = new Screen[6];
        Screen s;
        GLCanvas c;
        JoglEngine3D e;
        final int size = 400;
        
        // center / straight ahead
        s = screens[0] = new Screen(capabilities);
        e = s.engine;
        e.screenCenterPos = new Triple(0, 0, -size/2.0);
        e.screenNormalVec = new Triple(0, 0, 1);
        c = s.canvas;
        c.setBounds(1*size, 1*size, size, size);

        // right
        s = screens[1] = new Screen(capabilities);
        e = s.engine;
        e.screenCenterPos = new Triple(size/2.0, 0, 0);
        e.screenNormalVec = new Triple(-1, 0, 0);
        c = s.canvas;
        c.setBounds(2*size, 1*size, size, size);

        // left
        s = screens[2] = new Screen(capabilities);
        e = s.engine;
        e.screenCenterPos = new Triple(-size/2.0, 0, 0);
        e.screenNormalVec = new Triple(1, 0, 0);
        c = s.canvas;
        c.setBounds(0*size, 1*size, size, size);

        // top
        s = screens[3] = new Screen(capabilities);
        e = s.engine;
        e.screenCenterPos = new Triple(0, size/2.0, 0);
        e.screenNormalVec = new Triple(0, -1, 0);
        e.screenUpVec     = new Triple(0, 0, 1); // can't be || to normal
        c = s.canvas;
        c.setBounds(1*size, 0*size, size, size);

        // bottom
        s = screens[4] = new Screen(capabilities);
        e = s.engine;
        e.screenCenterPos = new Triple(0, -size/2.0, 0);
        e.screenNormalVec = new Triple(0, 1, 0);
        e.screenUpVec     = new Triple(0, 0, -1); // can't be || to normal
        c = s.canvas;
        c.setBounds(1*size, 2*size, size, size);

        // back
        s = screens[5] = new Screen(capabilities);
        e = s.engine;
        e.screenCenterPos = new Triple(0, 0, size/2.0);
        e.screenNormalVec = new Triple(0, 0, -1);
        e.screenUpVec     = new Triple(0, 1, 0);
        c = s.canvas;
        c.setBounds(3*size, 1*size, size, size);
    }
//}}}

//{{{ createKinemage
//##############################################################################
    Kinemage createKinemage()
    {
        Kinemage k = new Kinemage();
        KGroup g = new KGroup();
        k.add(g);
        
        KPaint[] colors = { KPalette.red, KPalette.green, KPalette.gold,
            KPaint.createLightweightHSV("silver", 240, 3, 90, 240, 3, 10) };
        
        for(int c = 0; c < colors.length; c++)
        {
            KList list = new KList(KList.BALL);
            //list.setOn(false);
            list.setColor(colors[c]);
            list.setRadius(0.1f);
            g.add(list);
            double offset = (2.0 * Math.PI * c) / colors.length;
            for(double y = -1; y <= 1.001; y += list.getRadius())
            {
                double r = 1 - Math.abs(y);
                double theta = (2.0 * Math.PI * y) + offset;
                BallPoint pt = new BallPoint("");
                pt.setXYZ(r * Math.cos(theta), y, r * Math.sin(theta));
                list.add(pt);
            }
        }
        
        for(int c = 0; c < colors.length; c++)
        {
            KList list = new KList(KList.VECTOR);
            //list.setOn(false);
            list.setColor(colors[c]);
            list.setWidth(4);
            g.add(list);
            double offset = (2.0 * Math.PI * c) / colors.length;
            VectorPoint prevPt = null;
            for(double y = -1; y <= 1.001; y += 0.02)
            {
                double r = 1 - Math.abs(y);
                double theta = (2.0 * Math.PI * y) + offset;
                VectorPoint pt = new VectorPoint("", prevPt);
                pt.setXYZ(r * Math.cos(theta), y, r * Math.sin(theta));
                list.add(pt);
                prevPt = pt;
            }
        }
        
        return k;
    }
//}}}

//{{{ actionPerformed
//##############################################################################
    public void actionPerformed(ActionEvent ev)
    {
        view.rotateY( (float) Math.toRadians(1.0) );
        for(Screen s : screens)
            if(s != null) s.canvas.repaint();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

