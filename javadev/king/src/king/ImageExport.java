// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.Engine;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.imageio.*; // J2SE 1.4+
import javax.imageio.stream.*; // J2SE 1.4+
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>ImageExport</code> allows the current graphics to be quickly
* exported as an image. The exact image is preserved, including
* the font on this platform and the rendering quality -- fully WYSIWYG.
* Only works in Java 1.4 or later.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep 29 09:33:14 EDT 2003
*/
public class ImageExport implements PropertyChangeListener, Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JFileChooser        chooser;
    SuffixFileFilter    jpgFilter, pngFilter;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ImageExport()
    {
        super();
        buildChooser();
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        pngFilter = new SuffixFileFilter("Portable Network Graphics (PNG)");
        pngFilter.addSuffix(".png");
        jpgFilter = new SuffixFileFilter("Joint Photographic Experts Group (JPEG)");
        jpgFilter.addSuffix(".jpg");
        jpgFilter.addSuffix(".jpe");
        jpgFilter.addSuffix(".jpeg");
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(pngFilter);
        chooser.addChoosableFileFilter(jpgFilter);
        chooser.setFileFilter(jpgFilter);
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        chooser.addPropertyChangeListener(this);
    }
//}}}

//{{{ exportImage
//##############################################################################
    static public void exportImage(KinCanvas kCanvas, String format, File outfile)
        throws IOException
    {
        Dimension       dim = kCanvas.getSize();
        BufferedImage   img;
        if(format.equals("png"))
            img = new BufferedImage(dim.width, dim.height,
            BufferedImage.TYPE_INT_ARGB); // needed so we can get transparency in output
        else
            img = new BufferedImage(dim.width, dim.height,
            BufferedImage.TYPE_INT_BGR); // this avoids color problems with JPEG and gives smaller files (?)
        Graphics2D      g2  = img.createGraphics();
        
        kCanvas.paintCanvas(g2, Engine.QUALITY_BEST);
        
        // This ensures we get high-quality JPEGs
        if(format.equals("jpg"))
            writeAtHighestQuality(img, format, outfile);
        else
            ImageIO.write(img, format, outfile);
    }
//}}}

//{{{ writeAtHighestQuality
//##############################################################################
    /**
    * Saves the image in the specified format with the
    * highest quality encoding available.
    * A lot of this code was borrowed from a Java Tech Tip.
    */
    static void writeAtHighestQuality(BufferedImage img, String format, File outfile)
        throws IOException
    {
        ImageWriter bestWriter = null;
        ImageWriteParam bestWriteParam = null;
        float bestQual = 0;
        
        for(Iterator iter = ImageIO.getImageWritersByFormatName(format); iter.hasNext(); )
        {
            ImageWriter iw = (ImageWriter) iter.next();
            //System.err.println("New "+format+" writer ["+iw.getClass().getName()+"]:");
            ImageWriteParam iwp = iw.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            
            float best = 0;
            float values[] = iwp.getCompressionQualityValues();
            for(int i = 0; i < values.length; i++)
            {
                //System.err.println("  "+values[i]);
                best = Math.max(best, values[i]);
            }
            
            if(best > bestQual)
            {
                iwp.setCompressionQuality(best);
                bestWriter      = iw;
                bestWriteParam  = iwp;
                bestQual        = best;
            }
        }

        if(bestWriter == null) return;
        
        FileImageOutputStream output = new FileImageOutputStream(outfile);
        bestWriter.setOutput(output);
        IIOImage image = new IIOImage(img, null, null);
        bestWriter.write(null, image, bestWriteParam);
    }
//}}}

//{{{ askExport
//##############################################################################
    public void askExport(KingMain kMain)
    {
        // Auto-generate a file name
        propertyChange(null);
        
        // Show the Save dialog
        if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(kMain.getTopWindow()))
        {
            File f = chooser.getSelectedFile();
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    String fmt;
                    if(pngFilter.equals(chooser.getFileFilter()))           fmt = "png";
                    else if(jpgFilter.equals(chooser.getFileFilter()))      fmt = "jpg";
                    else if(pngFilter.accept(f))                            fmt = "png";
                    else if(jpgFilter.accept(f))                            fmt = "jpg";
                    else fmt = "jpg";
                    
                    exportImage(kMain.getCanvas(), fmt, f);
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An I/O error occurred while saving the file:\n"+ex.getMessage(),
                        "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
            }
        }
    }
//}}}

//{{{ propertyChange, run
//##################################################################################################
    public void propertyChange(PropertyChangeEvent ev)
    {
        if(ev == null || JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(ev.getPropertyName()))
        {
            // Has to be done "asynchronously" or file name will be corrupted
            SwingUtilities.invokeLater(this);
        }
        // Example from other KiNG code:
        //if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(ev.getPropertyName()))
        //...
    }
    
    public void run()
    {
        String fmt = null;
        if(pngFilter.equals(chooser.getFileFilter()))           fmt = "png";
        else if(jpgFilter.equals(chooser.getFileFilter()))      fmt = "jpg";
        
        if(fmt != null)
        {
            // Autogenerate an output name.
            for(int i = 1; i < 1000; i++)
            {
                File f = new File("kingsnap"+i+"."+fmt);
                if(!f.exists())
                {
                    chooser.setSelectedFile(f);
                    break;
                }
            }
        }
    }
//}}}

//{{{ diagnostics
//##############################################################################
    /** Writes diagnostic info about javax.imageio to SoftLog.err */
    static public void diagnostics()
    {
        SoftLog.err.println("Can write the following formats:");
        String[] types = ImageIO.getWriterMIMETypes();
        for(int i = 0; i < types.length; i++)
            SoftLog.err.println("  "+i+":\t"+types[i]);
        
        for(int i = 0; i < types.length; i++)
        {
            SoftLog.err.println("Writers for "+types[i]+":");
            for(Iterator iter = ImageIO.getImageWritersByMIMEType(types[i]); iter.hasNext(); )
                SoftLog.err.println("  "+iter.next());
        }
        
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

