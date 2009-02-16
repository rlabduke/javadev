// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
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
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep 29 09:33:14 EDT 2003
*/
public class ImageExport extends Plugin implements PropertyChangeListener, Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JFileChooser        chooser;
    SuffixFileFilter    jpgFilter, pngFilter, pngtFilter;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ImageExport(ToolBox tb)
    {
        super(tb);
        buildChooser();
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        jpgFilter = new SuffixFileFilter("Joint Photographic Experts Group (JPEG)");
        jpgFilter.addSuffix(".jpg");
        jpgFilter.addSuffix(".jpe");
        jpgFilter.addSuffix(".jpeg");
        pngFilter = new SuffixFileFilter("Portable Network Graphics (PNG)");
        pngFilter.addSuffix(".png");
        pngtFilter = new SuffixFileFilter("PNG with transparent background");
        pngtFilter.addSuffix(".png");
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(jpgFilter);
        chooser.addChoosableFileFilter(pngFilter);
        chooser.addChoosableFileFilter(pngtFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(jpgFilter);
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        chooser.addPropertyChangeListener(this);
    }
//}}}

//{{{ exportImage
//##############################################################################
    static public void exportImage(KinCanvas kCanvas, String format, File outfile)
        throws IOException
    { exportImage(kCanvas, format, false, 1, outfile); }

    static public void exportImage(KinCanvas kCanvas, String format, boolean transparentBackground, int resol, File outfile)
        throws IOException
    {
        Dimension       dim = kCanvas.getCanvasSize();
        BufferedImage   img;
        if(format.equals("png"))
            img = new BufferedImage(resol*dim.width, resol*dim.height,
            BufferedImage.TYPE_INT_ARGB); // needed so we can get transparency in output
        else
            img = new BufferedImage(resol*dim.width, resol*dim.height,
            BufferedImage.TYPE_INT_BGR); // this avoids color problems with JPEG and gives smaller files (?)
        
        Graphics2D g2 = img.createGraphics();
        g2.scale(resol, resol);
        if(transparentBackground)
            kCanvas.getEngine().setTransparentBackground();
        kCanvas.paintCanvas(g2, dim, KinCanvas.QUALITY_BEST);
        
        /* Easy enough to do in Keynote that it's not worth including here!
        // Tasteful border - 1px, medium gray
        if(!transparentBackground)
        {
            g2.setColor(new Color(0.5f, 0.5f, 0.5f, 1.0f));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRect(0, 0, dim.width, dim.height);
        }
        */
        
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
        // These two lines copied from javax.imageio.ImageIO.write()
        // in hopes of writing out complete JPEGs under Windows
        output.flush();
        bestWriter.dispose();
        output.close();
    }
//}}}

//{{{ askExport
//##############################################################################
    public void askExport()
    {
        // Auto-generate a file name
        propertyChange(null);
        
        // Show the Save dialog
        String currdir = System.getProperty("user.dir");
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(kMain.getTopWindow()))
        {
            String fmt = getFormat();

            javax.swing.filechooser.FileFilter filter = chooser.getFileFilter();
            File f = chooser.getSelectedFile();
            if(!filter.accept(f) &&
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file has the wrong extension. Append '."+fmt+"' to the name?",
            "Fix extension?", JOptionPane.YES_NO_OPTION))
            {
                f = new File(f+"."+fmt);
            }

                
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try { exportImage(kMain.getCanvas(), fmt, pngtFilter.equals(filter), kMain.getPrefs().getInt("imageExportMultiplier"), f); }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An I/O error occurred while saving the file:\n"+ex.getMessage(),
                        "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
                System.setProperty("user.dir", f.getAbsolutePath());
            }
        }
    }
//}}}

//{{{ getFormat, propertyChange, run
//##################################################################################################
    String getFormat()
    {
        String fmt;
        javax.swing.filechooser.FileFilter filter = chooser.getFileFilter();
        if(pngFilter.equals(filter))        fmt = "png";
        else if(pngtFilter.equals(filter))  fmt = "png";
        else if(jpgFilter.equals(filter))   fmt = "jpg";
        else                                fmt = "jpg"; // shouldn't happen
        return fmt;
    }
    
    public void propertyChange(PropertyChangeEvent ev)
    {
        if(ev == null
        || JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(ev.getPropertyName())
        || JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(ev.getPropertyName()))
        {
            // Has to be done "asynchronously" or file name will be corrupted
            SwingUtilities.invokeLater(this);
        }
    }
    
    public void run()
    {
        String fmt = getFormat();
        // Autogenerate an output name.
        for(int i = 1; i < 1000; i++)
        {
            File f = new File(chooser.getCurrentDirectory(), "kingsnap"+i+"."+fmt);
            if(!f.exists())
            {
                chooser.setSelectedFile(f);
                break;
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

//{{{ getToolsMenuItem, getHelpMenuItem, toString, onExport, isAppletSafe
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString()+"...", null, this, "onExport"));
    }

    public JMenuItem getHelpMenuItem()
    { return null; }
    
    public String toString()
    { return "Image file (JPG, PNG)"; }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExport(ActionEvent ev)
    { this.askExport(); }

    static public boolean isAppletSafe()
    { return false; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

