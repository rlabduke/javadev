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
//import driftwood.*;
//}}}
/**
* <code>ImagePanel</code> just pulls the FFT image over and paints it.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb  9 10:40:35 EST 2004
*/
public class ImagePanel extends JComponent implements ChangeListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    FFTDrawingApplet    imageSource;
    JSlider             gain;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ImagePanel(FFTDrawingApplet imgSrc)
    {
        super();
        this.imageSource = imgSrc;
        gain = new JSlider(1, 255, 255);
        gain.setInverted(true);
        gain.addChangeListener(this);
    }
//}}}

//{{{ paintComponent
//##################################################################################################
    /** Override of JPanel.paintComponent. */
    protected void paintComponent(Graphics g)
    {
        Dimension size = this.getSize();
        
        Graphics2D g2 = (Graphics2D)g;
        BufferedImage img = imageSource.getImageForPainting(this);
        img = calcGain(img);
        g2.drawImage(img, 0, 0, this);
    }
//}}}
    
//{{{ calcGain
//##############################################################################
    BufferedImage calcGain(BufferedImage in)
    {
        int max = gain.getValue();
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
    
//{{{ stateChanged
//##############################################################################
    public void stateChanged(ChangeEvent ev)
    {
        this.repaint();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

