// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fftoys;
import jnt.FFT.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
//}}}
/**
* <code>FFTDrawingApplet</code> allows the user to draw on a canvas, and then
* calculates the 2-D Fourier transform of the image.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb  9 10:31:13 EST 2004
*/
public class FFTDrawingApplet extends JApplet implements MouseListener, MouseMotionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    ImagePanel      imgPane;
    BufferedImage   workingImage;
    int             lastXCoord, lastYCoord;
    boolean         backward = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FFTDrawingApplet()
    {
        super();
    }
//}}}

//{{{ init
//##############################################################################
    public void init()
    {
        // Build the GUI
        imgPane = new ImagePanel(this);
        imgPane.addMouseListener(this);
        imgPane.addMouseMotionListener(this);
        
        JButton btnFFT          = new JButton(new ReflectiveAction("Compute FFT", null, this, "onComputeFFT"));
        JButton btnWipePhases   = new JButton(new ReflectiveAction("Wipe phases", null, this, "onWipePhases"));
        JButton btnWipeAll      = new JButton(new ReflectiveAction("Wipe all", null, this, "onWipeAll"));
        
        TablePane tp = new TablePane();
        tp.addCell(btnFFT).newRow();
        tp.addCell(btnWipePhases).newRow();
        tp.addCell(btnWipeAll).newRow();
        
        Container cp = new JPanel(new BorderLayout());
        cp.add(imgPane, BorderLayout.CENTER);
        cp.add(tp, BorderLayout.EAST);
        cp.add(imgPane.gain, BorderLayout.SOUTH);
        this.setContentPane(cp);
        this.validate();
    }
//}}}

//{{{ onXXX, onComputeFFT
//##############################################################################
    // target of reflection
    public void onXXX(ActionEvent ev)
    {
        System.err.println("Something hasn't been implemented yet...");
    }

    // target of reflection
    public void onComputeFFT(ActionEvent ev)
    {
        if(workingImage == null) return;
        workingImage = transformImage(workingImage);
        imgPane.repaint();
    }

    // target of reflection
    public void onWipePhases(ActionEvent ev)
    {
        filterImage(workingImage, 0xff00ffff);
        imgPane.repaint();
    }

    // target of reflection
    public void onWipeAll(ActionEvent ev)
    {
        filterImage(workingImage, 0xff0000ff);
        imgPane.repaint();
    }
//}}}

//{{{ getImageForPainting
//##############################################################################
    public BufferedImage getImageForPainting(Component c)
    {
        Dimension size = c.getSize();
        if(workingImage == null)
            workingImage = makeTestImage2();
        if(workingImage.getWidth() != size.width || workingImage.getHeight() != size.height)
        {
            BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.createGraphics();
            g.setColor(Color.black);
            g.fillRect(0, 0, size.width, size.height);
            g.drawImage(workingImage, 0, 0, this);
            //g.dispose();
            workingImage = img;
        }
        
        return workingImage;
    }
//}}}

//{{{ Mouse motion listeners
//##################################################################################################
    public void mouseDragged(MouseEvent ev)
    {
        Point where = ev.getPoint();
        
        Graphics2D g = workingImage.createGraphics();
        g.setColor(Color.green);
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(lastXCoord, lastYCoord, where.x, where.y);
        g.dispose();
        
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
        Point where = ev.getPoint();
        lastXCoord  = where.x;
        lastYCoord  = where.y;
    }
    public void mouseReleased(MouseEvent ev)
    {}
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ filterImage
//##############################################################################
    /**
    * Performs a bitwise AND on every pixel in the image.
    */
    public void filterImage(BufferedImage img, int andMask)
    {
        for(int i = 0; i < img.getWidth(); i++)
        {
            for(int j = 0; j < img.getHeight(); j++)
            {
                img.setRGB(i, j, img.getRGB(i,j) & andMask);
            }
        }
    }
//}}}

//{{{ imageToData
//##############################################################################
    /**
    * The X index of the image is data rows, the Y index is data columns,
    * which isn't quite intuitive. Tough luck.
    */
    double[] imageToData(BufferedImage img)
    {
        int xlen = img.getWidth();
        int ylen = img.getHeight();
        
        double[] data = new double[xlen * ylen * 2];
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index       = (i*ylen + j) * 2;
                double mag      = ((img.getRGB(i, j) >>>  8) & 0xFF) / 255.0;
                double phi      = ((img.getRGB(i, j) >>> 16) & 0xFF) / 256.0 * (2*Math.PI);
                data[index]     = mag * Math.cos(phi);
                data[index+1]   = mag * Math.sin(phi);
            }
        }
        
        return data;
    }
//}}}

//{{{ dataToImage
//##############################################################################
    /**
    * Unpacks data from a complex, double-precision FFT into a BufferedImage.
    * The minimum and maximum magnitudes of the complex values will correspond
    * to the minimum (0) and maximum (255) values in the green channel of the image.
    * The phase angle will be similarly encoded in the red channel (0 to 2pi).
    */
    BufferedImage dataToImage(double[] data, int xlen, int ylen)
    {
        BufferedImage img = new BufferedImage(xlen, ylen, BufferedImage.TYPE_INT_ARGB);
        
        // Convert complex data to magnitude and phase angle
        double[] mag = new double[xlen*ylen];
        double[] phi = new double[xlen*ylen];
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index       = (i*ylen + j);
                double re       = data[2*index];
                double im       = data[2*index+1];
                mag[index]      = Math.sqrt(re*re + im*im);
                phi[index]      = Math.atan2(im, re);
                if(phi[index] < 0.0)
                    phi[index] += 2*Math.PI;
            }
        }
        
        // Find min and max magnitude
        double max = Double.NEGATIVE_INFINITY;
        for(int idx = 0; idx < xlen*ylen; idx++)
                max = Math.max(max, mag[idx]);
        //System.err.println("max="+max);
        // min should always be zero!

        // Write image, putting low hkl's in center
        int xhalf = (int)Math.ceil(xlen/2.0);
        int yhalf = (int)Math.ceil(ylen/2.0);
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index   = (((i+xhalf)%xlen)*ylen + ((j+yhalf)%ylen));
                //Lots of error: int red     = (int)Math.floor(256.0 * phi[index] / (2*Math.PI));
                int red     = (int)Math.round(256.0 * phi[index] / (2*Math.PI));
                //Additional error: int green   = (int)Math.floor(256.0 * mag[index] / max);
                int green   = (int)Math.round(255.0 * mag[index] / max);
                if(red > 255)   red -= 256;
                if(green > 255) green = 255;
                img.setRGB(i, j, 0xff000000 + (red << 16) + (green << 8));
                //Magnitude only: img.setRGB(i, j, 0xff000000 + (green << 8));
            }
        }
        /*for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index   = (i*ylen + j);
                //Lots of error: int red     = (int)Math.floor(256.0 * phi[index] / (2*Math.PI));
                int red     = (int)Math.round(256.0 * phi[index] / (2*Math.PI));
                //Additional error: int green   = (int)Math.floor(256.0 * mag[index] / max);
                int green   = (int)Math.round(255.0 * mag[index] / max);
                if(red > 255)   red -= 256;
                if(green > 255) green = 255;
                img.setRGB(i, j, 0xff000000 + (red << 16) + (green << 8));
                //Magnitude only: img.setRGB(i, j, 0xff000000 + (green << 8));
            }
        }*/
        
        return img;
    }
//}}}

//{{{ transformImage
//##############################################################################
    /**
    * Fourier transforms an image and returns a new image of the same dimensions.
    */
    public BufferedImage transformImage(BufferedImage in)
    {
        double[] data = imageToData(in);
        ComplexDouble2DFFT fft = new ComplexDouble2DFFT(in.getWidth(), in.getHeight());
        
        // Alternate directions
        if(backward)
            fft.backtransform(data);
        else
            fft.transform(data); // runs in place
        backward = !backward;
        
        return dataToImage(data, in.getWidth(), in.getHeight());
    }
//}}}

//{{{ makeTestImage{1, 2}
//##############################################################################
    BufferedImage makeTestImage1()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < img.getWidth(); i++)
        {
            for(int j = 0; j < img.getHeight(); j++)
            {
                if(i%2 == 0 && j%2 == 0)
                    img.setRGB(i, j, 0xff00ff00);
                else
                    img.setRGB(i, j, 0xff000000);
            }
        }
        return img;
    }

    BufferedImage makeTestImage2()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < img.getWidth(); i++)
        {
            for(int j = 0; j < img.getHeight(); j++)
            {
                if(i%6 == 0 && j%11 == 0)
                    img.setRGB(i, j, 0xff00ff00);
                else
                    img.setRGB(i, j, 0xff000000);
            }
        }
        return img;
    }
//}}}

//{{{ makeTestImage{3, 4}
//##############################################################################
    BufferedImage makeTestImage3()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < img.getWidth(); i++)
        {
            for(int j = 0; j < img.getHeight(); j++)
            {
                if(i%32 == 0 && j%8 == 0)
                    img.setRGB(i, j, 0xff00ff00);
                else if(i%8 == 0 && j%4 == 0)
                    img.setRGB(i, j, 0xff007f00);
                else
                    img.setRGB(i, j, 0xff000000);
            }
        }
        return img;
    }
    
    BufferedImage makeTestImage4()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, 400, 400);
        g.setColor(Color.green);
        g.fillOval( 50,  50, 100, 100);
        g.fillOval(250,  50, 100, 100);
        g.fillOval( 50, 250, 100, 100);
        g.fillOval(250, 250, 100, 100);
        g.dispose();
        return img;
    }
//}}}
}//class

