// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fftoys;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
//}}}
/**
* <code>RealPanel</code> displays the real-space image and
* allows the user to draw on the canvas.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb 10 10:03:19 EST 2004
*/
public class RealPanel extends JPanel implements MouseListener, MouseMotionListener, ChangeListener
{
//{{{ Constants
//}}}

//{{{ CLASS: RealImagePanel
//##############################################################################
    class RealImagePanel extends JComponent
    {
        protected void paintComponent(Graphics g)
        {
            Dimension size = this.getSize();
            //g.setColor(Color.black);
            //g.fillRect(0, 0, size.width, size.height);
            BufferedImage img = dataman.getRealImage();
            img = gainImage(img);
            g.drawImage(
                img,
                (size.width - img.getWidth())/2,
                (size.height - img.getHeight())/2,
                this);
        }
    }
//}}}

//{{{ CLASS: NamedImage
//##############################################################################
    static class NamedImage
    {
        String          name;
        BufferedImage   img;
        
        public NamedImage(String n, BufferedImage i)
        {
            this.name   = n;
            this.img    = i;
        }
        
        public String toString()
        {
            return name;
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    FFToysApplet    applet;
    DataManager     dataman;
    RealImagePanel  imgPane;
    JSlider         gainSlider;
    JComboBox       imageChooser;
    int             lastXCoord, lastYCoord;
    int             pencilSize = 7;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RealPanel(FFToysApplet app, DataManager dm)
    {
        super();
        this.applet     = app;
        this.dataman    = dm;
        
        imgPane = new RealImagePanel();
        imgPane.addMouseListener(this);
        imgPane.addMouseMotionListener(this);
        
        gainSlider = new JSlider(5, 255, 255);
        gainSlider.setInverted(true);
        gainSlider.addChangeListener(this);
        
        imageChooser = new JComboBox();
        imageChooser.addActionListener(new ReflectiveAction(null, null, this, "onImageChoosen"));
        
        JButton btnPencil = new JButton(new ReflectiveAction("Change pencil size", null, this, "onPencilSize"));
        btnPencil.setToolTipText("Changes the width of the drawing tool");
        JButton btnWipe = new JButton(new ReflectiveAction("Wipe all", null, this, "onWipeAll"));
        btnWipe.setToolTipText("Erases the drawing area so you can start over");
        JButton btnPatterson = new JButton(new ReflectiveAction("Make Patterson", null, this, "onPatterson"));
        btnPatterson.setToolTipText("Creates a Patterson map in real space");
        JButton btnLattice = new JButton(new ReflectiveAction("Xtal lattice", null, this, "onLattice"));
        btnLattice.setToolTipText("Creates a crystal lattice from your current \"unit cell\"");
        TablePane tp = new TablePane();
        tp.addCell(imageChooser).newRow();
        tp.addCell(btnPencil).newRow();
        tp.addCell(btnWipe).newRow();
        tp.addCell(btnLattice).newRow();
        tp.addCell(btnPatterson).newRow();
        
        this.setLayout(new BorderLayout());
        this.add(imgPane, BorderLayout.CENTER);
        this.add(gainSlider, BorderLayout.SOUTH);
        this.add(tp, BorderLayout.EAST);
        this.validate();
        
        //this.addChoosableImage("Starting image", dm.getRealImage());
    }
//}}}

//{{{ Mouse motion listeners
//##################################################################################################
    public void mouseDragged(MouseEvent ev)
    {
        Point where = ev.getPoint();
        Dimension size = imgPane.getSize();
        BufferedImage img = dataman.getRealImage();
        
        where.x -= (size.width - img.getWidth())/2;
        where.y -= (size.height - img.getHeight())/2;
        
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(pencilSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(lastXCoord, lastYCoord, where.x, where.y);
        g.dispose();
        dataman.setRealImage(img);
        
        imgPane.repaint();

        lastXCoord = where.x;
        lastYCoord = where.y;
    }
    
    public void mouseMoved(MouseEvent ev)
    {}
//}}}

//{{{ Mouse click listners
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {}
    public void mouseEntered(MouseEvent ev)
    {}
    public void mouseExited(MouseEvent ev)
    {}
    public void mousePressed(MouseEvent ev)
    {
        // Track where the mouse started moving
        Point where = ev.getPoint();
        Dimension size = imgPane.getSize();
        BufferedImage img = dataman.getRealImage();
        
        where.x -= (size.width - img.getWidth())/2;
        where.y -= (size.height - img.getHeight())/2;
        
        lastXCoord  = where.x;
        lastYCoord  = where.y;
        
        // Make sure our gain slider is all the way down
        if(gainSlider.getValue() != 255) gainSlider.setValue(255);
        
        // Allows us to draw a dot by clicking:
        this.mouseDragged(ev);
    }
    public void mouseReleased(MouseEvent ev)
    {}
//}}}

//{{{ stateChanged, gainImage
//##############################################################################
    public void stateChanged(ChangeEvent ev)
    {
        imgPane.repaint();
    }
    
    BufferedImage gainImage(BufferedImage in)
    {
        int max = gainSlider.getValue();
        if(max == 255) return in;
        
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < in.getWidth(); i++)
        {
            for(int j = 0; j < in.getHeight(); j++)
            {
                int val = in.getRGB(i, j);
                int red = (val>>>16) & 0xff;
                int grn = (val>>>8)  & 0xff;
                int blu = (val)      & 0xff;
                red = Math.min(255, (255 * red) / max);
                grn = Math.min(255, (255 * grn) / max);
                blu = Math.min(255, (255 * blu) / max);
                val = 0xff000000 + (red<<16) + (grn<<8) + (blu);
                out.setRGB(i, j, val);
            }
        }
        
        return out;
    }
//}}}

//{{{ onWipeAll
//##############################################################################
    // target of reflection
    public void onWipeAll(ActionEvent ev)
    {
        BufferedImage img = dataman.getRealImage();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        dataman.setRealImage(img);
        
        imgPane.repaint();
    }
//}}}

//{{{ onPatterson
//##############################################################################
    // target of reflection
    public void onPatterson(ActionEvent ev)
    {
        dataman.convertToPatterson();
        imgPane.repaint();
    }
//}}}

//{{{ onLattice
//##############################################################################
    // target of reflection
    public void onLattice(ActionEvent ev)
    {
        String in = (String)JOptionPane.showInputDialog(this,
            "How many repeats along each axis?",
            "Create crystal lattice", JOptionPane.PLAIN_MESSAGE,
            null, null, "8");
            
        if(in == null) return;
        
        try
        {
            int val = Integer.parseInt(in);
            
            BufferedImage src = dataman.getRealImage();
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.black);
            g.fillRect(0, 0, w, h);
            
            for(int i = 0; i < val; i++)
            {
                for(int j = 0; j < val; j++)
                {
                    g.drawImage(src, i*(w/val), j*(h/val), w/val, h/val, this);
                }
            }
            //g.dispose();
            
            dataman.setRealImage(dst);
            imgPane.repaint();
        }
        catch(NumberFormatException ex)
        {}
    }
//}}}

//{{{ onPencilSize
//##############################################################################
    // target of reflection
    public void onPencilSize(ActionEvent ev)
    {
        String in = (String)JOptionPane.showInputDialog(this,
            "Enter pencil diameter in pixels:",
            "Change pencil size", JOptionPane.PLAIN_MESSAGE,
            null, null, Integer.toString(pencilSize));
            
        if(in == null) return;
        
        try { pencilSize = Integer.parseInt(in); }
        catch(NumberFormatException ex) {}
    }
//}}}

//{{{ addChoosableImage, onImageChoosen
//##############################################################################
    public void addChoosableImage(String name, BufferedImage img)
    {
        // Make a copy, in case this one gets overwritten
        BufferedImage copy = DataManager.copyImage(img);
        imageChooser.addItem(new NamedImage(name, copy));
    }
    
    // target of reflection
    public void onImageChoosen(ActionEvent ev)
    {
        NamedImage ni = (NamedImage) imageChooser.getSelectedItem();
        // Make a copy, in case this one gets overwritten
        BufferedImage copy = DataManager.copyImage(ni.img);
        dataman.setRealImage(copy);
        imgPane.repaint();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

