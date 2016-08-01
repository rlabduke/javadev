// (jEdit options) :folding=explicit:collapseFolds=1:
import java.awt.*;
import java.awt.image.*;
import java.util.Random;
import javax.swing.*;
/**
* <code>MacGraphics</code> demonstrates some ways in which
* the java.awt.Graphics line and polygon primitives do not behave
* the same on Mac OS X as they do on Windows and Linux.
* None of these are noticeable when antialiasing is enabled,
* although this causes a major performance hit.
*
* The program accepts one switch, -aa, which enables antialiasing.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Nov 25 20:33:23 EST 2003
*/
public class MacGraphics //extends ... implements ...
{
    /**
    * Demonstrates a problem with Graphics.drawLine() for slightly
    * off-vertical, off-horizontal, or off-diagonal lines.
    * This has been FIXED in Panther (OS X 10.3.1 / Java 1.4.1).
    * It used to be (10.2.x) that many parts of a one-pixel line at a slight
    * angle were drawn two pixels thick unless antialiasing was enabled.
    * MAC                   WINDOWS/LINUX
    * -----------          ----------
    *     -----------               ------------
    * In fact, the Mac version now looks BETTER when antialiasing is on;
    * weird gray blocks appear in the bottom half for Windows and Linux (only with AA on).
    */
    class ZebraLines extends JComponent //{{{
    {
        ZebraLines()
        {
            super();
            this.setPreferredSize(new Dimension(400,400));
        }
        protected void paintComponent(Graphics g)
        {
            if(useAA)
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Dimension dim = this.getSize();
            g.setColor(Color.black);
            g.fillRect(0, 0, dim.width, dim.height);
            g.setColor(Color.white);
            for(int x = 0; x < dim.width; x+=2)
            {
                g.drawLine(x, 0, x, dim.width/2);
                g.drawLine(x, dim.width/2, x+1, dim.width);
            }
        }
    }//}}}

    /**
    * Demonstrates a problem with Graphics.fillPolygon().
    * Despite Sun's talk of the pen hanging down and right
    * of the pixel location (see Graphics javadoc),
    * it appears that drawPolygon() and fillPolygon() were
    * intended to be a complimentary pair, like stroke and fill in Illustrator.
    * Thus Apple's Java draws polygons that are one pixel too wide
    * going to the right and down.
    * This is actually fairly hard to correct for -- you can't just
    * subtract one pixel from all the coordinates, because that just moves the shape.
    *
    * This is STILL BROKEN in Panther (10.3.1 / Java 1.4.1)
    *
    * These also identify a new bug where the polygons are drawn twice as wide
    * and half as bright, in contrast to the goals stated in the Graphics2D javadoc.
    * See below.
    */
    class ZebraPolys extends JComponent //{{{
    {
        ZebraPolys()
        {
            super();
            this.setPreferredSize(new Dimension(400,400));
        }
        protected void paintComponent(Graphics g)
        {
            if(useAA)
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Dimension dim = this.getSize();
            g.setColor(Color.black);
            g.fillRect(0, 0, dim.width, dim.height);
            g.setColor(Color.white);
            for(int x = 0; x < dim.width; x+=2)
            {
                // TOP HALF
                // Apple: solid white b/c polys are too wide
                //  With antialiasing on, solid gray (twice as wide/half as bright)
                // Windows/Linux: zebra stripe (one pixel wide)
                g.fillPolygon(new int[] {x, x, x+1, x+1},
                    new int[] {0, dim.width/2, dim.width/2, 0},
                    4);
                // BOTTOM HALF
                // Apple: zebra stripe (one pixel wide)
                // Windows/Linux: solid black b/c polys have zero width
                g.fillPolygon(new int[] {x, x, x, x},
                    new int[] {dim.width/2, dim.width, dim.width, dim.width/2},
                    4);
            }
        }
    }//}}}

    /**
    * Allows for comparison in timing for filling polygons on <!-- {{{ -->
    * different machines and different platforms.
    * Times are for 10,000 random polygons, averaged over 5 trials.
    *
    * G4 PowerBook (1.25 Ghz, 1 GB RAM, Panther 10.3.1, Java 1.4.1)
    *   Without antialiasing:       3100 ms
    *   With antialiasing*  :       5600 ms (80% slowdown)
    * (* Using -Dapple.awt.antialiasing=on; similar for -aa)
    *
    * Pentium 1000 MHz IBM T23? ThinkPad, 384 Mb RAM, Java 1.4.1
    * S3 Savage/IX 1014, 32bpp @ 1400x1050, 8Mb RAM
    *   Windows 2000:
    *       Without antialiasing:   4750 ms
    *       With antialiasing   :  11850 ms
    *   Red Hat Linux 9:
    *       Without antialiasing:   3200 ms
    *       With antialiasing   :  32500 ms
    *
    * Testing with desktop Linux and Mac systems agreed that the
    * performance hit for antialiasing is about 80% under OS X,
    * and about 10-fold (1000%!) under Linux.
    *
    * So maybe Apple's performance in Java graphics isn't the issue for my molecular
    * modeling program, which renders more slowly on my new Mac than on the old IBM.
    *
    * I'd still like to see better speed for graphics primitives, given that
    * the video subsystem on the PowerBook blows the ThinkPad out of the water.
    * For comparison, the desktop Linux system I tested (results not shown)
    * has a nice nVidia card that's a year or two old, and it completed this
    * test in just 1150 ms (though it took 10,000 ms for antialiased, but that's a Linux issue).
    * I don't know whether it's even reasonable to hope for that from a notebook,
    * but then my notebook ran this test just as fast as a G4 desktop...
    */
    class TimedPolys extends JComponent
    {
        TimedPolys()
        {
            super();
            this.setPreferredSize(new Dimension(400,400));
        }
        protected void paintComponent(Graphics g)
        {
            if(useAA)
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Dimension dim = this.getSize();
            g.setColor(Color.black);
            g.fillRect(0, 0, dim.width, dim.height);
            
            // Generate all polygons first to isolate that
            // from graphics performance.
            final int num = 10000;
            final Color[] colorset = {Color.red, Color.orange, Color.yellow,
                Color.green, Color.cyan, Color.blue, Color.magenta};
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
            for(int i = 0; i < num; i++)
            {
                g.setColor(cs[i]);
                g.fillPolygon(xs[i], ys[i], 3);
            }
            time = System.currentTimeMillis() - time;
            System.err.println("Drew "+num+" triangles in "+time+" ms.");
        }
    }//}}}

    /**
    * Uses filled, 6-sided polygons to emulate thick lines.
    * Not submitted to Apple.
    */
    class TimedLines extends JComponent //{{{
    {
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        TimedLines()
        {
            super();
            this.setPreferredSize(new Dimension(400,400));
        }
        protected void paintComponent(Graphics g)
        {
            if(useAA)
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Dimension dim = this.getSize();
            g.setColor(Color.black);
            g.fillRect(0, 0, dim.width, dim.height);
            
            // Generate all polygons first to isolate that
            // from graphics performance.
            final int num = 10000;
            final Color[] colorset = {Color.red, Color.orange, Color.yellow,
                Color.green, Color.cyan, Color.blue, Color.magenta};
            int[][] xs = new int[num][2];
            int[][] ys = new int[num][2];
            Color[] cs = new Color[num];
            Random rand = new Random();
            for(int i = 0; i < num; i++)
            {
                xs[i][0] = rand.nextInt(dim.width);
                xs[i][1] = rand.nextInt(dim.width);
                ys[i][0] = rand.nextInt(dim.height);
                ys[i][1] = rand.nextInt(dim.height);
                cs[i]    = colorset[ rand.nextInt(colorset.length) ];
            }
            
            // Time how long it takes to draw them all
            long time = System.currentTimeMillis();
            for(int i = 0; i < num; i++)
            {
                g.setColor(cs[i]);
                prettyLine((Graphics2D)g, xs[i][0], ys[i][0], xs[i][1], ys[i][1], 2);
            }
            time = System.currentTimeMillis() - time;
            System.err.println("Drew "+num+" lines in "+time+" ms.");
        }
        void prettyLine(Graphics2D g, int x1, int y1, int x2, int y2, int width) //{{{
        {
            int s, e, abs_x2_x1, abs_y2_y1;
            s = -width / 2; // Start offset
            e = s + width;  // End offset
            abs_x2_x1 = Math.abs(x2 - x1);
            abs_y2_y1 = Math.abs(y2 - y1);
            
            // horizontal --
            if( abs_x2_x1 > abs_y2_y1 )
            {
                // left to right
                if( x1 < x2 )
                {
                    xPoints[0] = x1  ; xPoints[1] = x1+s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2-s; xPoints[5] = x2;
                    yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
                }
                // right to left
                else
                {
                    xPoints[0] = x1  ; xPoints[1] = x1-s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2+s; xPoints[5] = x2;
                    yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
                }
            }
            // vertical |
            else
            {
                // top to bottom
                if( y1 < y2 )
                {
                    xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                    yPoints[0] = y1  ; yPoints[1] = y1+s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2-s; yPoints[5] = y2;
                }
                // bottom to top
                else
                {
                    xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                    yPoints[0] = y1  ; yPoints[1] = y1-s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2+s; yPoints[5] = y2;
                }
            }
            
            g.fillPolygon(xPoints, yPoints, 6);
        }//}}}
    }//}}}

    /**
    * Uses filled, 6-sided polygons to emulate thick lines.
    * Also uses a back buffer VolatileImage instead of assuming Swing's doing it.
    * Not submitted to Apple. This doesn't make things any faster.
    */
    class VolatileLines extends JComponent //{{{
    {
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        VolatileImage buffer = null;
        
        VolatileLines()
        {
            super();
            this.setPreferredSize(new Dimension(400,400));
        }
        
        void createBuffer()
        {
            if(buffer != null)
            {
                buffer.flush();
                buffer = null;
            }
            buffer = this.createVolatileImage(this.getWidth(), this.getHeight());
            ImageCapabilities ic = buffer.getCapabilities();
            System.err.println("isAccelerated = "+ic.isAccelerated()+"; isTrueVolatile = "+ic.isTrueVolatile());
        }
        
        protected void paintComponent(Graphics g)
        {
            Dimension dim = this.getSize();
            g.setColor(Color.black);
            g.fillRect(0, 0, dim.width, dim.height);
            
            if(buffer == null || buffer.getWidth() != this.getWidth() || buffer.getHeight() != this.getHeight())
                createBuffer();
            
            do {
                int valCode = buffer.validate( this.getGraphicsConfiguration() );
                if(valCode == VolatileImage.IMAGE_RESTORED)
                    {} // Don't care
                else if(valCode == VolatileImage.IMAGE_INCOMPATIBLE)
                    createBuffer();
                
                Graphics2D gBuf = buffer.createGraphics();
                if(useAA)
                    gBuf.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
                // Generate all polygons first to isolate that
                // from graphics performance.
                final int num = 10000;
                final Color[] colorset = {Color.red, Color.orange, Color.yellow,
                    Color.green, Color.cyan, Color.blue, Color.magenta};
                int[][] xs = new int[num][2];
                int[][] ys = new int[num][2];
                Color[] cs = new Color[num];
                Random rand = new Random();
                for(int i = 0; i < num; i++)
                {
                    xs[i][0] = rand.nextInt(dim.width);
                    xs[i][1] = rand.nextInt(dim.width);
                    ys[i][0] = rand.nextInt(dim.height);
                    ys[i][1] = rand.nextInt(dim.height);
                    cs[i]    = colorset[ rand.nextInt(colorset.length) ];
                }
                
                // Time how long it takes to draw them all
                long time = System.currentTimeMillis();
                for(int i = 0; i < num; i++)
                {
                    gBuf.setColor(cs[i]);
                    prettyLine(gBuf, xs[i][0], ys[i][0], xs[i][1], ys[i][1], 2);
                }
                time = System.currentTimeMillis() - time;
                System.err.println("Drew "+num+" lines in "+time+" ms.");
                
                g.drawImage(buffer, 0, 0, this);
            } while(buffer.contentsLost());
        }
        
        void prettyLine(Graphics2D g, int x1, int y1, int x2, int y2, int width) //{{{
        {
            int s, e, abs_x2_x1, abs_y2_y1;
            s = -width / 2; // Start offset
            e = s + width;  // End offset
            abs_x2_x1 = Math.abs(x2 - x1);
            abs_y2_y1 = Math.abs(y2 - y1);
            
            // horizontal --
            if( abs_x2_x1 > abs_y2_y1 )
            {
                // left to right
                if( x1 < x2 )
                {
                    xPoints[0] = x1  ; xPoints[1] = x1+s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2-s; xPoints[5] = x2;
                    yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
                }
                // right to left
                else
                {
                    xPoints[0] = x1  ; xPoints[1] = x1-s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2+s; xPoints[5] = x2;
                    yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
                }
            }
            // vertical |
            else
            {
                // top to bottom
                if( y1 < y2 )
                {
                    xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                    yPoints[0] = y1  ; yPoints[1] = y1+s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2-s; yPoints[5] = y2;
                }
                // bottom to top
                else
                {
                    xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                    yPoints[0] = y1  ; yPoints[1] = y1-s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2+s; yPoints[5] = y2;
                }
            }
            
            g.fillPolygon(xPoints, yPoints, 6);
        }//}}}
    }//}}}

    public MacGraphics()
    {
        JFrame frame = new JFrame("MacGraphics test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTabbedPane cp = new JTabbedPane();
        frame.setContentPane(cp);
        
        cp.addTab("ZebraLines", new ZebraLines());
        cp.addTab("ZebraPolys", new ZebraPolys());
        cp.addTab("TimedPolys", new TimedPolys());
        cp.addTab("TimedLines", new TimedLines());
        cp.addTab("VolatileLines", new VolatileLines());
        
        frame.pack();
        frame.setVisible(true);
    }

    static boolean useAA = false;
    public static void main(String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("-aa")) useAA = true;
        }
        
        MacGraphics mainprog = new MacGraphics();
    }
}//class

