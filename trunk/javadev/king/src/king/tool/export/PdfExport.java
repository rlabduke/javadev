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

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
//}}}
/**
* <code>PdfExport</code> uses the iText library to export the current graphics
* as a (vector) PDF file. The exact image is preserved, including
* the font on this platform and the rendering quality -- fully WYSIWYG.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep 29 09:33:14 EDT 2003
*/
public class PdfExport extends Plugin implements PropertyChangeListener, Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JFileChooser        chooser;
    SuffixFileFilter    pdfFilter;
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
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(pdfFilter);
        chooser.setFileFilter(pdfFilter);
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        chooser.addPropertyChangeListener(this);
    }
//}}}

//{{{ exportPDF
//##############################################################################
    static public void exportPDF(KinCanvas kCanvas, File outfile)
        throws IOException, DocumentException
    {
        Dimension   dim = kCanvas.getCanvasSize();
        Document    doc = new Document(PageSize.LETTER, 72, 72, 72, 72); // 1" margins
        PdfWriter   pdf = PdfWriter.getInstance(doc, new FileOutputStream(outfile));
        doc.addCreator("KiNG by Ian W. Davis");
        // add header and footer now, before opening document
        doc.open();
        
        // Drawing code goes here. We use a template to simplify scaling/placement.
        PdfContentByte  content     = pdf.getDirectContent();
        PdfTemplate     template    = content.createTemplate((float)dim.getWidth(), (float)dim.getHeight());
        Graphics2D      g2          = template.createGraphics((float)dim.getWidth(), (float)dim.getHeight());
        kCanvas.paintCanvas(g2, dim, KinCanvas.QUALITY_BEST);
        g2.dispose();
        
        // Post-multiplied transformation matrix:
        // [ x ]   [ a  b  0 ]   [ x' ]   [ x'/q ]
        // [ y ] * [ c  d  0 ] = [ y' ] = [ y'/q ]
        // [ 1 ]   [ e  f  1 ]   [ q  ]   [   1  ]
        // Top, left, botttom, and right already include margins.
        // Coordinate system has bottom left corner as (0, 0)
        double w = doc.right() - doc.left();
        double h = doc.top() - doc.bottom();
        float scale = (float)Math.min(w/dim.getWidth(), h/dim.getHeight());
        // Place image at top left corner of page, respecting margins
        content.addTemplate(template, scale, 0f, 0f, scale,
            doc.left(),
            (float)(doc.top() - scale*dim.getHeight()));
        
        // Closing the document writes everything to file
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
                    exportPDF(kMain.getCanvas(), f);
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file:\n"+ex.getMessage(),
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
        String fmt = "pdf";
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

