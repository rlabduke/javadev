// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;

import king.core.*;
import king.points.*;
import king.painters.*;
import driftwood.r3.*;
import driftwood.util.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
//}}}
/**
* <code>Slave</code> renders a particular view, coordinated by Commands from a Master.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec 15 08:03:06 EST 2006
*/
public class Slave implements GLEventListener
{
//{{{ Constants
//}}}

//{{{ CLASS: CommandRunner
//##############################################################################
    class CommandRunner implements Runnable
    {
        Command cmd;
        
        public CommandRunner(Command cmd)
        { this.cmd = cmd; }
        
        public void run()
        { cmd.doCommand(Slave.this); }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    Props props;
    ObjectLink<String,Command> link;
    
    Kinemage        kin = null;
    KView           view = null;
    Triple          leftEyePos;
    Triple          rightEyePos;
    boolean         wantStereo;
    
    JFrame          frame   = null;
    GLCanvas        canvas  = null;
    JoglEngine3D    engine  = null;
    Dimension       glSize  = new Dimension();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Slave()
    {
        super();
        Props defaultProps = new Props();
        try { defaultProps.load( getClass().getResourceAsStream("default.props") ); }
        catch(IOException ex) { ex.printStackTrace(); }
        this.props = new Props(defaultProps);
    }
//}}}

//{{{ initGraphics
//##############################################################################
    void initGraphics()
    {
        // Set up parameters for rendering
        GLCapabilities capabilities = new GLCapabilities();
        capabilities.setDoubleBuffered(true); // usually enabled by default, but to be safe...
        int fsaaNumSamples = props.getInt("slave.fsaa_samples");
        capabilities.setSampleBuffers(fsaaNumSamples > 1); // enables/disables full-scene antialiasing (FSAA)
        capabilities.setNumSamples(fsaaNumSamples); // sets number of samples for FSAA (default is 2)
        // Trying to set stereo when your graphics card doesn't support it
        // leads to a segfault (OS X) or a GLException (Linux).
        capabilities.setStereo(wantStereo);

        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this); // calls display(), reshape(), etc.
        engine = new JoglEngine3D();
        engine.usePerspective = true;
        engine.setFont(18);
        engine.caveClipping = props.getBoolean("is_cave");
        engine.screenCenterPos  = getTriple(props, "slave.screen_center_px");
        engine.screenNormalVec  = getTriple(props, "slave.screen_normal_vec");
        engine.screenUpVec      = getTriple(props, "slave.screen_up_vec");
        
        // Figure out which display device to create the window on.
        int whichScreen = props.getInt("slave.which_screen");
        int whichConfig = props.getInt("slave.which_subscreen");
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        if(0 <= whichScreen && whichScreen < gds.length)
            gd = gds[whichScreen];
        // A GraphicsConfiguration is a sub-section of a physical display, like a virtual desktop
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        GraphicsConfiguration[] gcs = gd.getConfigurations();
        if(0 <= whichConfig && whichConfig < gcs.length)
            gc = gcs[whichConfig];
        
        frame = new JFrame(this.getClass().getName(), gc);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas);
        frame.setUndecorated(props.getBoolean("slave.full_screen"));
        frame.pack();
        frame.show();
        
        if(props.getBoolean("slave.hide_mouse"))
        {
            // Only way to hide the mouse cursor in Java -- make it transparent.
            int[] pixels = new int[16 * 16];
            Image image = Toolkit.getDefaultToolkit().createImage(new java.awt.image.MemoryImageSource(16, 16, pixels, 0, 16));
            Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisiblecursor");
            frame.setCursor(transparentCursor);
        }
        
        if(props.getBoolean("slave.full_screen"))
        {
            // Puts the window in full screen mode.  Seems to work OK with JOGL.
            // However, true full screen exclusive mode doesn't work on the Mac
            // for anything but the primary display.
            // However, on the primary display, it's needed to cover the Dock
            // and menu bar.
            if(props.getBoolean("slave.full_screen_exclusive"))
                gd.setFullScreenWindow(frame); // should be done after becoming visible
            else
            {
                frame.setBounds(gc.getBounds());
                frame.setAlwaysOnTop(true);
            }
        }
    }
//}}}

//{{{ init, display, reshape, displayChanged
//##############################################################################
    public void init(GLAutoDrawable drawable)
    {}
    
    public void display(GLAutoDrawable drawable)
    {
        GL gl = drawable.getGL();
        
        int[] canDoStereo = new int[1];
        gl.glGetIntegerv(GL.GL_STEREO, canDoStereo, 0);
        if(wantStereo && canDoStereo[0] == GL.GL_TRUE)
        {
            // Trying this if it's not supported would probably throw a GLException
            gl.glDrawBuffer(GL.GL_BACK_RIGHT);
            engine.render(kin, view, new Rectangle(glSize), gl, rightEyePos);
            gl.glDrawBuffer(GL.GL_BACK_LEFT);
            engine.render(kin, view, new Rectangle(glSize), gl, leftEyePos);
        }
        else
        {
            Triple eyePos = new Triple().likeMidpoint(leftEyePos, rightEyePos);
            engine.render(kin, view, new Rectangle(glSize), gl, eyePos);
        }
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        this.glSize.setSize(width, height);
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
    {}
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
        
        KList list = new KList(KList.TRIANGLE);
        list.setOn(false);
        list.setColor(KPalette.gray);
        g.add(list);
        TrianglePoint prevPt = null;
        for(double y = -1; y <= 1.001; y += 0.02)
        {
            double r = 1 - Math.abs(y);
            double theta = (2.0 * Math.PI * y);
            TrianglePoint pt = new TrianglePoint("", prevPt);
            pt.setXYZ(r * Math.cos(theta), y-0.1, r * Math.sin(theta));
            list.add(pt);
            prevPt = pt;
            pt = new TrianglePoint("", prevPt);
            pt.setXYZ(r * Math.cos(theta), y+0.1, r * Math.sin(theta));
            list.add(pt);
            prevPt = pt;
        }
        
        list = new KList(KList.LABEL);
        //list.setOn(false);
        list.setColor(KPalette.deadwhite);
        g.add(list);
        LabelPoint l1 = new LabelPoint("X-axis");
        l1.setXYZ(0.5, 0, 0);
        l1.setColor(KPalette.pinktint);
        list.add(l1);
        LabelPoint l2 = new LabelPoint("Y-axis");
        l2.setXYZ(0, 0.5, 0);
        l2.setColor(KPalette.greentint);
        list.add(l2);
        LabelPoint l3 = new LabelPoint("Z-axis");
        l3.setXYZ(0, 0, 0.5);
        l3.setColor(KPalette.bluetint);
        list.add(l3);
        
        return k;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ getTriple
//##############################################################################
    public static Triple getTriple(Props p, String name) throws NumberFormatException
    {
        String s = p.getString(name);
        double[] d = Strings.explodeDoubles(s, ' ');
        if(d.length < 3)
            throw new NumberFormatException("Not enough numbers in '"+name+"'");
        return new Triple(d[0], d[1], d[2]);
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        try
        {
            String  host    = props.getString("master.host");
            int     port    = props.getInt("master.port");
                    link    = new ObjectLink<String,Command>(host, port);
            System.out.println("Connected to master at "+host+":"+port);
            
            this.kin = createKinemage();
            // These are default values that will be quickly overwritten.
            this.view = new KView(kin);
            this.leftEyePos = getTriple(props, "master.observer_left_eye_px");
            this.rightEyePos = getTriple(props, "master.observer_right_eye_px");
            this.wantStereo = props.getBoolean("slave.use_stereo");
            
            initGraphics();
            
            while(true)
            {
                Command cmd = link.getBlocking();
                SwingUtilities.invokeLater( new CommandRunner(cmd) );
            }
            
        }
        catch(Exception ex) { ex.printStackTrace(); }
        finally { if(link != null) link.disconnect(); }
    }

    public static void main(String[] args)
    {
        Slave mainprog = new Slave();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}

//{{{ parseArguments, showHelp
//##############################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        String  arg, flag, param;
        boolean interpFlags = true;
        
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") || !interpFlags || arg.equals("-"))
            {
                // This is probably a filename or something
                interpretArg(arg);
            }
            else if(arg.equals("--"))
            {
                // Stop treating things as flags once we find --
                interpFlags = false;
            }
            else
            {
                // This is a flag. It may have a param after the = sign
                int eq = arg.indexOf('=');
                if(eq != -1)
                {
                    flag    = arg.substring(0, eq);
                    param   = arg.substring(eq+1);
                }
                else
                {
                    flag    = arg;
                    param   = null;
                }
                
                try { interpretFlag(flag, param); }
                catch(NullPointerException ex)
                { throw new IllegalArgumentException("'"+arg
                    +"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("Slave.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Slave.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("king.dive.Slave");
        System.err.println("Copyright (C) 2006 by Ian W. Davis. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ interpretArg, interpretFlag
//##############################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
        try { props.load(new FileInputStream(arg)); }
        catch(IOException ex) { throw new IllegalArgumentException("Can't read properties from file '"+arg+"'"); }
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

