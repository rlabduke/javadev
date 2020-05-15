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
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.form.*;
import org.apache.pdfbox.util.*;
import de.rototor.pdfbox.graphics2d.*;
//}}}
/**
* <code>PdfExport</code> uses the iText library to export the current graphics
* as a (vector) PDF file. The exact image is preserved, including
* the font on this platform and the rendering quality -- fully WYSIWYG.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep 29 09:33:14 EDT 2003
*/
public class PdfExport extends Plugin implements PropertyChangeListener, Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JFileChooser        chooser;
    SuffixFileFilter    pdfFilter, pdftFilter;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PdfExport(ToolBox tb)
    {
        super(tb);
        buildChooser();
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        pdfFilter = new SuffixFileFilter("Portable Document Format (PDF)");
        pdfFilter.addSuffix(".pdf");
        pdftFilter = new SuffixFileFilter("PDF with transparent background");
        pdftFilter.addSuffix(".pdf");
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(pdfFilter);
        chooser.addChoosableFileFilter(pdftFilter);
        chooser.setFileFilter(pdfFilter);
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        chooser.addPropertyChangeListener(this);
    }
//}}}

//{{{ exportPDF
//##############################################################################
    static public void exportPDF(KinCanvas kCanvas, File outfile)
        throws IOException
    { exportPDF(kCanvas, false, outfile); }
    
    static public void exportPDF(KinCanvas kCanvas, boolean transparentBackground, File outfile)
        throws IOException
    {
      exportPDF(kCanvas, transparentBackground, outfile, kCanvas.getCanvasSize());
    }
    
    static public void exportPDF(KinCanvas kCanvas, boolean transparentBackground, File outfile, Dimension dim)
        throws IOException
    {
        PDDocument    doc = new PDDocument();//PageSize.LETTER, 72, 72, 72, 72); // 1" margins
        PDPage        page = new PDPage();
        PDDocumentInformation pdd = doc.getDocumentInformation();
        pdd.setCreator("KiNG by the Richardsons Lab");
        
        doc.addPage(page);
        
        // Drawing code
        float w = (float)dim.getWidth();
        float h = (float)dim.getHeight();
        PdfBoxGraphics2D pbGraphics2D = new PdfBoxGraphics2D(doc, w, h);
        if(transparentBackground)
            kCanvas.getEngine().setTransparentBackground();
        kCanvas.paintCanvas(pbGraphics2D, dim, KinCanvas.QUALITY_BEST);
        pbGraphics2D.dispose();
        
        // Get the XForm, calculate scaling factor and position.
        PDFormXObject xform = pbGraphics2D.getXFormObject();
        
        float page_width = page.getCropBox().getWidth();
        float page_height = page.getCropBox().getHeight();
        
        float scale = (float)Math.min(page_width/w, page_height/h);
        
        float x_centered = ( page_width - w * scale ) / 2 + page.getCropBox().getLowerLeftX();
        float y_centered = ( page_height - h * scale ) / 2 + page.getCropBox().getLowerLeftY();
        
        // Build a matrix to scale and place the form
        Matrix matrix = new Matrix();

        matrix.translate(x_centered, y_centered);
        matrix.scale(scale, scale);
        
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
        contentStream.transform(matrix);
        
        //Now finally draw the form.
        contentStream.drawForm(xform);
		
        contentStream.close();
        
        doc.save(outfile);

        doc.close();
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
            File f = chooser.getSelectedFile();
            if(!pdfFilter.accept(f) &&
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file has the wrong extension. Append '.pdf' to the name?",
            "Fix extension?", JOptionPane.YES_NO_OPTION))
            {
                f = new File(f+".pdf");
            }

                
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    exportPDF(kMain.getCanvas(), pdftFilter.equals(chooser.getFileFilter()), f);
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file:\n"+ex.getMessage(),
                        "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
                System.setProperty("user.dir", f.getAbsolutePath());
            }
        }
    }
//}}}

//{{{ propertyChange, run
//##################################################################################################
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
        String fmt = "pdf";
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

//{{{ getToolsMenuItem, getHelpMenuItem, toString, onExport, isAppletSafe
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString()+"...", null, this, "onExport"));
    }

    public JMenuItem getHelpMenuItem()
    { return null; }
    
    public String toString()
    { return "PDF document"; }

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

