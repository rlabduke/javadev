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
* <code>ReciprocalPanel</code> displays the reciprocal-space image.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb 10 10:03:19 EST 2004
*/
public class ReciprocalPanel extends JPanel implements ChangeListener
{
//{{{ Constants
//}}}

//{{{ CLASS: ReciprocalImagePanel
//##############################################################################
    class ReciprocalImagePanel extends JComponent
    {
        protected void paintComponent(Graphics g)
        {
            Dimension size = this.getSize();
            //g.setColor(Color.black);
            //g.fillRect(0, 0, size.width, size.height);
            BufferedImage img = dataman.getRecipImage();
            img = gainImage(img);
            g.drawImage(
                img,
                (size.width - img.getWidth())/2,
                (size.height - img.getHeight())/2,
                this);
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    FFToysApplet    applet;
    DataManager     dataman;
    ReciprocalImagePanel  imgPane;
    JSlider         gainSlider;
    int             lastXCoord, lastYCoord;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ReciprocalPanel(FFToysApplet app, DataManager dm)
    {
        super();
        this.applet     = app;
        this.dataman    = dm;
        
        imgPane = new ReciprocalImagePanel();
        
        gainSlider = new JSlider(5, 255, 255);
        gainSlider.setInverted(true);
        gainSlider.addChangeListener(this);
        
        JButton btnResol = new JButton(new ReflectiveAction("Set resolution", null, this, "onSetResol"));
        btnResol.setToolTipText("Wipes out high-resolution data at user-selected radius");
        TablePane tp = new TablePane();
        tp.addCell(btnResol).newRow();

        this.setLayout(new BorderLayout());
        this.add(imgPane, BorderLayout.CENTER);
        this.add(gainSlider, BorderLayout.SOUTH);
        this.add(tp, BorderLayout.EAST);
        this.validate();
    }
//}}}

//{{{ stateChanged
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

//{{{ onSetResol
//##############################################################################
    // target of reflection
    public void onSetResol(ActionEvent ev)
    {
        BufferedImage img = dataman.getRecipImage();
        int w = img.getWidth()/2, h = img.getHeight()/2;
        
        String in = JOptionPane.showInputDialog(this,
            "Select a resolution between 0 and "+(int)Math.sqrt(w*w+h*h)+",\n"
            +"higher values give better resolution.",
            "Set resolution", JOptionPane.PLAIN_MESSAGE);
            
        if(in == null) return;
        
        try
        {
            double val = Double.parseDouble(in);
            dataman.setResolution(val);
            imgPane.repaint();
        }
        catch(NumberFormatException ex)
        {}
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

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

