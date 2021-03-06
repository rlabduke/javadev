package king.tutorial;

import king.core.*;
import king.points.*;
import king.painters.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
/**
* <code>JoglTumblingObject</code> creates a decorative spiral ornament and sets it rotating.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec  8 13:27:16 EST 2006
*/
public class JoglTumblingObject extends JFrame implements ActionListener, GLEventListener
{
    final boolean       use3d   = true;
    Kinemage            kin     = null;
    KView               view    = null;
    Engine2D            engine2 = null;
    JoglEngine3D        engine3 = null;
    GLCanvas            canvas  = null;
    Dimension           glSize  = new Dimension();
    Timer               timer   = null;

    static public void main(String[] args) { new JoglTumblingObject(); }
    
    public JoglTumblingObject()
    {
        super("JoglTumblingObject");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        kin = createKinemage();
        view = new KView(kin);
        view.setSpan(1.2f * view.getSpan());
        engine2 = new Engine2D();
        engine2.usePerspective = true;
        engine3 = new JoglEngine3D();
        engine3.usePerspective = true;
        
        // Create and listen to an OpenGL canvas
        GLCapabilities capabilities = new GLCapabilities();
        capabilities.setDoubleBuffered(true); // usually enabled by default, but to be safe...
        int fsaaNumSamples = 4;
        capabilities.setSampleBuffers(fsaaNumSamples > 1); // enables/disables full-scene antialiasing (FSAA)
        capabilities.setNumSamples(fsaaNumSamples); // sets number of samples for FSAA (default is 2)
        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this); // calls display(), reshape(), etc.
        canvas.setPreferredSize(new Dimension(400,400));
        this.getContentPane().add(canvas);
        
        this.pack();
        this.show();

        timer = new Timer(1000 / 30, this);
        timer.start();
    }
    
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

    public void actionPerformed(ActionEvent ev)
    {
        view.rotateY( (float) Math.toRadians(1.0) );
        canvas.repaint();
    }
    
    public void init(GLAutoDrawable drawable)
    {}
    
    public void display(GLAutoDrawable drawable)
    {
        if(use3d)
        {
            engine3.render(kin, view, new Rectangle(this.glSize), drawable.getGL());
        }
        else
        {
            JoglPainter painter = new JoglPainter(drawable);
            engine2.render(kin, view, new Rectangle(this.glSize), painter);
        }
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        this.glSize.setSize(width, height);
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
    {}
}//class

