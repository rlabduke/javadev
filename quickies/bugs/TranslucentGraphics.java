// (jEdit options) :folding=explicit:collapseFolds=1:
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Random;
import javax.swing.*;
/**
* <code>TranslucentGraphics</code> shows that enabling anti-aliasing at any time
* enhances the speed at which translucent polygons are rendered on Linux, even
* if you later turn antialiasing back off!
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Nov 25 20:33:23 EST 2003
*/
public class TranslucentGraphics //extends ... implements ...
{
    JCheckBox useAA;
    
    /**
    * Allows for comparison in timing for filling polygons on
    * different machines and different platforms.
    *
    * G4 PowerBook (1.25 Ghz, 1 GB RAM, Panther 10.3.2, Java 1.4.2)
    *                               alpha=255           alpha=85
    *   Without antialiasing:       2700 ms             4100 ms
    *   With antialiasing   :       5300 ms             6500 ms
    *
    * Pentium 1000 MHz IBM T23? ThinkPad, 384 Mb RAM
    * S3 Savage/IX 1014, 32bpp @ 1400x1050, 8Mb RAM
    *                               alpha=255           alpha=85
    *   Windows 2000, Java 1.4.1:
    *       Without antialiasing:   1950 ms            32700 ms
    *       With antialiasing   :  11800 ms            16400 ms
    *   Windows 2000, Java 1.5.0-beta1:
    *       Without antialiasing:   1950 ms            37500 ms
    *       With antialiasing   :  11500 ms            16500 ms
    *   Red Hat Linux 9, Java 1.4.1:
    *       Without antialiasing:   1700 ms            49000 ms
    *       With antialiasing   :  15500 ms            26100 ms
    */
    class TimedPolys extends JComponent implements ActionListener
    {
        int         alpha;
        
        TimedPolys(int alpha)
        {
            super();
            this.alpha = alpha;
            this.setPreferredSize(new Dimension(400,400));
        }
        
        public void actionPerformed(ActionEvent ev) { this.repaint(); }
        
        protected void paintComponent(Graphics g)
        {
            if(useAA.isSelected())
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Dimension dim = this.getSize();
            g.setColor(Color.black);
            g.fillRect(0, 0, dim.width, dim.height);
            
            // Generate all polygons first to isolate that
            // from graphics performance.
            final int num = 10000;
            final Color[] colorset = {
                new Color(255, 0, 0, alpha),
                new Color(255, 128, 0, alpha),
                new Color(255, 255, 0, alpha),
                new Color(0, 255, 0, alpha),
                new Color(0, 255, 255, alpha),
                new Color(0, 0, 255, alpha),
                new Color(255, 0, 255, alpha)
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
            for(int i = 0; i < num; i++)
            {
                g.setColor(cs[i]);
                g.fillPolygon(xs[i], ys[i], 3);
            }
            time = System.currentTimeMillis() - time;
            System.err.println("Drew "+num+" triangles in "+time+" ms with alpha="+alpha+" and aa="+useAA.isSelected());
        }
    }

    public TranslucentGraphics()
    {
        useAA = new JCheckBox("Enable antialiasing");
        
        JTabbedPane tp = new JTabbedPane();
        TimedPolys opaque = new TimedPolys(255);
        useAA.addActionListener(opaque);
        tp.addTab("TimedPolys Opaque", opaque);
        TimedPolys translucent = new TimedPolys(85);
        useAA.addActionListener(translucent);
        tp.addTab("TimedPolys Translucent", translucent);
        
        JPanel cp = new JPanel(new BorderLayout());
        cp.add(tp, BorderLayout.CENTER);
        cp.add(useAA, BorderLayout.SOUTH);
        
        JFrame frame = new JFrame("TranslucentGraphics test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(cp);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        TranslucentGraphics mainprog = new TranslucentGraphics();
    }
}//class

