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
//import driftwood.*;
//}}}
/**
* <code>DataManager</code> handles real- and reciprocal-space data,
* as well as their image renderings.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb 10 10:03:19 EST 2004
*/
public class DataManager //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    // These are all computed on demand. A null value
    // means the information was stale and needs to be regenerated.
    double[]        realData    = null;
    BufferedImage   realImg     = null;
    double[]        recipData   = null;
    BufferedImage   recipImg    = null;
    
    ComplexDouble2DFFT fft = null;
    int xlen = 0, ylen = 0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DataManager(BufferedImage realspaceImage)
    {
        super();
        setRealImage(realspaceImage);
    }
//}}}

//{{{ getRealImage
//##############################################################################
    public BufferedImage getRealImage()
    {
        if(realImg != null) return realImg;
        
        // No image, so we need to recalculate from the data.
        // Getting real data relies on image not existing yet!
        double[] data = getRealData();
        realImg = new BufferedImage(xlen, ylen, BufferedImage.TYPE_INT_ARGB);
        
        // Convert complex data to magnitude and phase angle
        double[] mag = new double[xlen*ylen];
        //double[] phi = new double[xlen*ylen];
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index       = (i*ylen + j);
                double re       = data[2*index];
                double im       = data[2*index+1];
                mag[index]      = Math.sqrt(re*re + im*im);
                //phi[index]      = Math.atan2(im, re);
                //if(phi[index] < 0.0)
                //    phi[index] += 2*Math.PI;
            }
        }
        
        // Find max magnitude
        double max = Double.NEGATIVE_INFINITY;
        for(int idx = 0; idx < xlen*ylen; idx++)
                max = Math.max(max, mag[idx]);

        // Write image
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index   = (i*ylen + j);
                int m       = (int)Math.min(255, Math.round(255.0 * mag[index] / max));
                realImg.setRGB(i, j, 0xff000000 + (m << 16) + (m << 8) + m);
            }
        }
        
        return realImg;
    }
//}}}

//{{{ getRealData
//##############################################################################
    private double[] getRealData()
    {
        if(realData != null) return realData;
        else if(recipData != null) // we can get it from a FFT
        {
            realData = (double[])recipData.clone();
            fft.transform(realData);
            return realData;
        }
        else if(realImg != null) // convert image to data
        {
            realData = new double[xlen * ylen * 2];
            for(int i = 0; i < xlen; i++)
            {
                for(int j = 0; j < ylen; j++)
                {
                    int index           = (i*ylen + j) * 2;
                    int color           = realImg.getRGB(i, j);
                    // mag ranges from 0 to 3*255
                    double mag          = ((color>>>16)&0xff) + ((color>>>8)&0xff) + (color&0xff);
                    // Brightness becomes real component, phase angle is zero.
                    realData[index]     = mag;
                    realData[index+1]   = 0.0;
                }
            }
            return realData;
        }
        else throw new Error("Both recipData and realImg are null; can't generate realData");
    }
//}}}

//{{{ getRecipData
//##############################################################################
    private double[] getRecipData()
    {
        if(recipData != null) return recipData;
        else if(realData == null && realImg == null)
            throw new Error("Both realData and realImg are null; can't generate recipData");
        else // we can get it from a FFT
        {
            recipData = (double[]) getRealData().clone();
            fft.backtransform(recipData);
            return recipData;
        }
    }
//}}}

//{{{ getRecipImage
//##############################################################################
    /**
    * This isn't quite fair, because it returns amplitudes (F's) rather than intensities.
    * But the intensities are on such a wide ranging scale it makes visualization difficult.
    */
    public BufferedImage getRecipImage()
    {
        if(recipImg != null) return recipImg;
        else
        {
            double[] data = getRecipData();
            recipImg = new BufferedImage(xlen, ylen, BufferedImage.TYPE_INT_ARGB);
            
            // Convert complex data to magnitudes
            double[] mag = new double[xlen*ylen];
            for(int i = 0; i < xlen; i++)
            {
                for(int j = 0; j < ylen; j++)
                {
                    int index       = (i*ylen + j);
                    double re       = data[2*index];
                    double im       = data[2*index+1];
                    mag[index]      = Math.sqrt(re*re + im*im);
                    //Intensity: inten[index] = (re*re + im*im);
                }
            }
            
            // Find max magnitude
            double max = Double.NEGATIVE_INFINITY;
            for(int idx = 0; idx < xlen*ylen; idx++)
                    max = Math.max(max, mag[idx]);
    
            // Write image, putting low hkl's in center
            int xhalf = (xlen+1)/2;
            int yhalf = (ylen+1)/2;
            for(int i = 0; i < xlen; i++)
            {
                for(int j = 0; j < ylen; j++)
                {
                    int index   = (((i+xhalf)%xlen)*ylen + ((j+yhalf)%ylen));
                    int m       = (int)Math.min(255, Math.round(255.0 * mag[index] / max));
                    recipImg.setRGB(i, j, 0xff000000 + (m << 16) + (m << 8) + m);
                }
            }
            
            return recipImg;
        }
    }
//}}}

//{{{ setRealImage
//##############################################################################
    public void setRealImage(BufferedImage img)
    {
        this.realImg = img;
        xlen = img.getWidth();
        ylen = img.getHeight();
        fft = new ComplexDouble2DFFT(xlen, ylen);
        
        realData    = null;
        recipData   = null;
        recipImg    = null;
    }
//}}}

//{{{ setResolution
//##############################################################################
    /**
    * Diminishes the (crystallographic) resolution of the reciprocal space image
    * by zeroing out some of the high hkl's.
    * @param pixels the radius in pixels around the center in which data will be preserved.
    */
    public void setResolution(double pixels)
    {
        getRecipData(); // ensures it exists, too
        
        realData    = null;
        realImg     = null;
        recipImg    = null;
        
        // Zero out high hkl combos
        int xhalf = (xlen+1)/2;
        int yhalf = (ylen+1)/2;
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index   = 2 * (i*ylen + j);
                int h       = (i < xhalf ? i : i-xlen);
                int k       = (j < yhalf ? j : j-ylen);
                if(h*h + k*k > pixels*pixels)
                {
                    recipData[index]    = 0;
                    recipData[index+1]  = 0;
                }
            }
        }
    }
//}}}

//{{{ convertToPatterson
//##############################################################################
    /**
    * Makes the reciprocal space data into intensities (squared amplitudes)
    * and wipes out the phase information. When transformed to real space,
    * the result will be a Patterson map.
    */
    public void convertToPatterson()
    {
        getRecipData(); // ensures it exists, too
        
        realData    = null;
        realImg     = null;
        recipImg    = null;
        
        for(int i = 0; i < recipData.length; i+=2)
        {
            double re = recipData[i];
            double im = recipData[i+1];
            recipData[i] = re*re + im*im; // square of magnitude
            recipData[i+1] = 0;
        }
    }
//}}}

//{{{ copyImage
//##############################################################################
    /**
    * @throws IllegalArgumentException if img can't be properly loaded.
    */
    static public BufferedImage copyImage(Image img)
    {
        // Prettier version using ImageIcon, and knowing ImageObservers can be null
        img = new ImageIcon(img).getImage(); // ImageIcon constructor forces loading to complete
        BufferedImage copy = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return copy;
        
        /* My ugly version cobbled together by hand.
        *
        // We have to try to make sure that the image is loaded.
        Component c = new Container();
        MediaTracker mt = new MediaTracker(c);
        mt.addImage(img, 0);
        while(true)
        {
            try { mt.waitForAll(); break; }
            catch(InterruptedException ex) {}
        }
        if(mt.isErrorAny())
            throw new IllegalArgumentException("Image could not be loaded");
        
        // Now we can actually copy it.
        BufferedImage copy = new BufferedImage(img.getWidth(c), img.getHeight(c), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(img, 0, 0, c);
        g.dispose();
        return copy;
        */
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

