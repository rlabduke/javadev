// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;

import java.util.List;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
//}}}
/**
* <code>RamaPdfWriter</code> uses the iText PDF libraries to produce a PDF
* plot of the six Richardson Ramachandran plots.
*
* <p>When run as an application, it just copies its template file to standard out.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Mar  5 11:23:43 EST 2004
*/
public class RamaPdfWriter //extends ... implements ...
{
//{{{ Constants
    /** Width of the extra border around the plot Graphics */
    static final float PLOT_EXTRA   = 60f;
    /** Scale factor to make plots fit on the page */
    static final float PLOT_SCALE   = 165.3f / 360; // smaller plots now
    /*static final float PLOT_SCALE = 215.5f / 360;*/
    /** Offset for positioning plots */
    static final float PLOT_OFFSET  = PLOT_SCALE * PLOT_EXTRA;
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RamaPdfWriter()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ createRamaPDF
//##############################################################################
    // Post-multiplied transformation matrix:
    // [ x ]   [ a  b  0 ]   [ x' ]   [ x'/q ]
    // [ y ] * [ c  d  0 ] = [ y' ] = [ y'/q ]
    // [ 1 ]   [ e  f  1 ]   [ q  ]   [   1  ]
    // Top, left, botttom, and right already include margins.
    // Document coordinate system has bottom left corner as (0, 0)
    // However, template coordinate systems have TOP left as (0, 0)
    // Rama plot lower left corners:
    // (108.3, 558.7)   (351.2, 558.7)
    // (108.3, 357.9)   (351.2, 357.9)
    // (108.3, 156.0)   (351.2, 156.0)
    // These were determined by hand by DAK in Illustrator; not sure how  
    // IWD got the old versions below (presumably by a similar strategy).
    /*
    // (62.5, 480.5)   (345.0, 480.5)
    // (62.5, 221.5)   (345.0, 221.5)
    */
    /**
    * Takes a bunch of analyzed models and plots Rama plots for all of them.
    * @param analyses a Map&lt; Collection&lt;Ramalyze.RamaEval&gt;, String &gt;
    *   that maps each collection of analyzed residues to a label.
    *   All of them together are rendered as "All models"
    * @param structName a label identifying this structure, or null for none.
    * @param out a destination for the PDF file.
    */
    public void createRamaPDF(Map analyses, String structName, OutputStream out) throws IOException
    {
        try
        {
            URL templateFile = this.getClass().getResource("rama8000/rama6-template.pdf");
            /*URL templateFile = this.getClass().getResource("rama5200/rama4-template.pdf");*/
            PdfReader pdfReader = new PdfReader(templateFile);
            
            Document    doc = new Document(PageSize.LETTER);
            PdfWriter   pdf = PdfWriter.getInstance(doc, out);
            doc.addCreator(this.getClass().getName()+" by Ian W. Davis");
            // add header and footer now, before opening document
            doc.open();
            
            PdfContentByte  content     = pdf.getDirectContent();
            PdfTemplate     template    = pdf.getImportedPage(pdfReader, 1);
            
            doModelByModel(analyses, structName, doc, content, template);
            // TODO: doResidueByResidue()
            
            // Closing the document writes everything to file
            doc.close();
        }
        catch(DocumentException ex)
        { throw new IOException("Got DocumentException when trying to generate PDF."); }
    }
//}}}

//{{{ doModelByModel
//##############################################################################
    /**
    * Produces the all-models and model-by-model pages.
    * @param analyses   a Map&lt; Collection&lt;Ramalyze.RamaEval&gt;, String &gt;
    *   that maps each collection of analyzed residues to a label.
    *   All of them together are rendered as "All models"
    * @param structName a label identifying this structure, or null for none.
    * @param doc        the document being generated
    * @param content    the content of the document being generated
    * @param template   the six-square Rama page template object
    */
    void doModelByModel(Map analyses, String structName, Document doc,
        PdfContentByte content, PdfTemplate template) throws DocumentException
    {
        int i = 0;
        PdfTemplate[] generalTemplates  = new PdfTemplate[analyses.size()];
        PdfTemplate[] ilevalTemplates   = new PdfTemplate[analyses.size()];
        PdfTemplate[] preproTemplates   = new PdfTemplate[analyses.size()];
        PdfTemplate[] glycineTemplates  = new PdfTemplate[analyses.size()];
        PdfTemplate[] transproTemplates = new PdfTemplate[analyses.size()];
        PdfTemplate[] cisproTemplates   = new PdfTemplate[analyses.size()];
        /*PdfTemplate[] prolineTemplates  = new PdfTemplate[analyses.size()];*/
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); i++)
        {
            Collection analysis  = (Collection) iter.next();
            generalTemplates[i]  = makeAnalysisTemplate(content, Ramalyze.RamaEval.GENERAL, analysis);
            ilevalTemplates[i]   = makeAnalysisTemplate(content, Ramalyze.RamaEval.ILEVAL, analysis);
            preproTemplates[i]   = makeAnalysisTemplate(content, Ramalyze.RamaEval.PREPRO, analysis);
            glycineTemplates[i]  = makeAnalysisTemplate(content, Ramalyze.RamaEval.GLYCINE, analysis);
            transproTemplates[i] = makeAnalysisTemplate(content, Ramalyze.RamaEval.TRANSPRO, analysis);
            cisproTemplates[i]   = makeAnalysisTemplate(content, Ramalyze.RamaEval.CISPRO, analysis);
            /*prolineTemplates[i] = makeAnalysisTemplate(content, Ramalyze.RamaEval.PROLINE, analysis);*/
        }
        
        // Do all plots superimposed
        if(analyses.size() > 1)
        {
            doc.newPage();
            content.addTemplate(template, 0, 0);
            for(i = 0; i < analyses.size(); i++)
            {
                content.addTemplate(generalTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    108.3f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
                content.addTemplate(preproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    108.3f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
                content.addTemplate(transproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    108.3f-PLOT_OFFSET, 156.0f-PLOT_OFFSET);
                content.addTemplate(ilevalTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    351.2f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
                content.addTemplate(glycineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    351.2f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
                content.addTemplate(cisproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    351.2f-PLOT_OFFSET, 156.0f-PLOT_OFFSET);
                /*content.addTemplate(generalTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    62.5f-PLOT_OFFSET, 480.5f-PLOT_OFFSET);
                content.addTemplate(glycineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    345.0f-PLOT_OFFSET, 480.5f-PLOT_OFFSET);
                content.addTemplate(prolineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    62.5f-PLOT_OFFSET, 221.5f-PLOT_OFFSET);
                content.addTemplate(preproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                    345.0f-PLOT_OFFSET, 221.5f-PLOT_OFFSET);*/
            }
            
            // Aggregate statistics for all residues analyzed
            ArrayList allRes = new ArrayList();
            for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); )
                allRes.addAll((Collection) iter.next());
            //PdfTemplate stats = plotStatistics(content, 540, 144, allRes);
            /*PdfTemplate stats = plotStatistics(content, 540, 130, allRes);*/
            PdfTemplate stats = plotStatistics(content, 540, 80, allRes);
            //float scale = Math.min(540/stats.getWidth(), 144/stats.getHeight());
            /*float scale = Math.min(540/stats.getWidth(), 130/stats.getHeight());*/
            float scale = Math.min(540/stats.getWidth(), 80/stats.getHeight());
            if(scale > 1) scale = 1;
            //content.addTemplate(stats, scale, 0, 0, scale, 36, 36);
            content.addTemplate(stats, scale, 0, 0, scale, 50, 50);
            
            if(structName != null)  addPageTitle(structName+", all models", content);
            else                    addPageTitle("All models", content);
        }
        
        // Do each model individually
        i = 0;
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); i++)
        {
            Collection analysis = (Collection) iter.next();
            String label = (String) analyses.get(analysis);
            doc.newPage();
            content.addTemplate(template, 0, 0);
            content.addTemplate(generalTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                108.3f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
            content.addTemplate(preproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                108.3f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
            content.addTemplate(transproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                108.3f-PLOT_OFFSET, 156.0f-PLOT_OFFSET);
            content.addTemplate(ilevalTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                351.2f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
            content.addTemplate(glycineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                351.2f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
            content.addTemplate(cisproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                351.2f-PLOT_OFFSET, 156.0f-PLOT_OFFSET);
            /*content.addTemplate(generalTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                62.5f-PLOT_OFFSET, 480.5f-PLOT_OFFSET);
            content.addTemplate(glycineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                345.0f-PLOT_OFFSET, 480.5f-PLOT_OFFSET);
            content.addTemplate(prolineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                62.5f-PLOT_OFFSET, 221.5f-PLOT_OFFSET);
            content.addTemplate(preproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                345.0f-PLOT_OFFSET, 221.5f-PLOT_OFFSET);*/
            //PdfTemplate stats = plotStatistics(content, 540, 144, analysis);
            /*PdfTemplate stats = plotStatistics(content, 540, 130, analysis);*/
            PdfTemplate stats = plotStatistics(content, 80, 100, analysis);
            //float scale = Math.min(540/stats.getWidth(), 144/stats.getHeight());
            /*float scale = Math.min(540/stats.getWidth(), 130/stats.getHeight());*/
            float scale = Math.min(540/stats.getWidth(), 80/stats.getHeight());
            if(scale > 1) scale = 1;
            //content.addTemplate(stats, scale, 0, 0, scale, 36, 36);
            content.addTemplate(stats, scale, 0, 0, scale, 50, 50);
            
            if(structName != null)  addPageTitle(structName+", model "+label, content);
            else                    addPageTitle("Model "+label, content);
        }
    }
//}}}

//{{{ makeAnalysisTemplate
//##############################################################################
    PdfTemplate makeAnalysisTemplate(PdfContentByte content, String evalType, Collection analysis)
    {
        PdfTemplate canvas = content.createTemplate(360+2*PLOT_EXTRA, 360+2*PLOT_EXTRA);
        Graphics2D g2 = canvas.createGraphics(canvas.getWidth(), canvas.getHeight());
        g2.translate(180+PLOT_EXTRA, 180+PLOT_EXTRA); // now we can use angles from -180 to +180 naturally
        
        Color c;
        if(Ramalyze.RamaEval.GENERAL.equals(evalType))  c = new Color(0xcc00cc); // purple
        else if(Ramalyze.RamaEval.ILEVAL.equals(evalType))  c = new Color(0xff0000); // red
        else if(Ramalyze.RamaEval.PREPRO.equals(evalType))  c = new Color(0x3366cc); // blue
        else if(Ramalyze.RamaEval.GLYCINE.equals(evalType))  c = new Color(0x00cc66); // green
        else if(Ramalyze.RamaEval.TRANSPRO.equals(evalType))  c = new Color(0xcc6600); // orange
        else if(Ramalyze.RamaEval.CISPRO.equals(evalType))  c = new Color(0x999900); // gold/yellow
        /*else if(Ramalyze.RamaEval.PROLINE.equals(evalType))  c = new Color(0xcc6600);*/
        else c = new Color(0x000000); // shouldn't ever happen
        
        this.plotAnalysis(g2, c, evalType, analysis);
        g2.dispose();
        return canvas;
    }
//}}}

//{{{ plotAnalysis
//##############################################################################
    /**
    * Plots the results of a Ramachandran analysis for one model onto
    * any arbitrary Graphics object.
    * We assume that user space for the Graphics is typical for screens, images, etc
    * where the top left corner is (0, 0).
    * angle space for the Ramachandran plot: a Residue with (phi, psi) = (-60, -40)
    * will be plotted at (-60, +40) on the Graphics.
    * @param g the Graphics to draw on
    * @param outColor the color to draw outliers in
    * @param evalType one of the Ramalyze.RamaEval type constants specifying 
    *   which type of residues should appear on this plot. One of 
    *   GENERAL, ILEVAL, PREPRO, GLYCINE, TRANSPRO, or CISPRO.
    * @param analysis a Collection of Ramalyze.RamaEval objects to be plotted.
    */
    void plotAnalysis(Graphics g, Color outColor, String evalType, Collection analysis)
    {
        Color fontColor     = new Color(0x000000);
        Color normalColor   = new Color(0x333333);
        g.setFont(new java.awt.Font("Serif", java.awt.Font.PLAIN, 10));
        
        for(Iterator iter = analysis.iterator(); iter.hasNext(); )
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter.next();
            if(! evalType.equals(eval.type)) continue;
            
            if(eval.score == Ramalyze.RamaEval.OUTLIER)
            {
                g.setColor(outColor);
                g.drawOval((int)eval.phi-3, ((int) -eval.psi)-3, 6, 6);
                g.setColor(fontColor);
                g.drawString(eval.name, (int)eval.phi+5, ((int) -eval.psi)+3);
            }
            else
            {
                g.setColor(normalColor);
                g.drawOval((int)eval.phi-2, ((int) -eval.psi)-2, 4, 4); 
            }
        }
    }
//}}}

//{{{ plotStatistics
//##############################################################################
    /**
    * Draws textual statistics about the results of a Ramachandran analysis
    * for any collection of RamaEvals.
    * @param content the PdfContentByte object to create a template from.
    * @param width the available width to draw in.
    * @param height the available height to draw in.
    * @param analysis a Collection of Ramalyze.RamaEval objects to be plotted.
    * @return a PdfTemplate of arbitrary size. The caller is responsible for scaling
    * it to fit into a width x height area.
    */
    PdfTemplate plotStatistics(PdfContentByte content, float width, float height, Collection analysis)
    {
        // Calculate the statistics
        int total = 0, favored = 0, allowed = 0, outlier = 0;
        for(Iterator iter = analysis.iterator(); iter.hasNext(); )
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter.next();
            if(eval.score == Ramalyze.RamaEval.FAVORED) { total++; favored++; }
            else if(eval.score == Ramalyze.RamaEval.ALLOWED) { total++; allowed++; }
            else if(eval.score == Ramalyze.RamaEval.OUTLIER) { total++; outlier++; }
        }
        
        // Write out a collection of the strings we want to print.
        DecimalFormat df = new DecimalFormat("0.0");
        ArrayList strings = new ArrayList();
        strings.add(df.format((100.0*favored)/total)+"% ("+favored+"/"+total+") of all residues were in favored (98%) regions.");
        strings.add(df.format((100.0*(favored+allowed))/total)+"% ("+(favored+allowed)+"/"+total+") of all residues were in allowed (>99.8%) regions.");
        strings.add(""); // blank line
        if(outlier == 0) strings.add("There were no outliers.");
        else
        {
            strings.add("There were "+outlier+" outliers (phi, psi):");
            for(Iterator iter = analysis.iterator(); iter.hasNext(); )
            {
                Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter.next();
                if(eval.score == Ramalyze.RamaEval.OUTLIER)
                    strings.add("    "+eval.name+" ("+df.format(eval.phi)+", "+df.format(eval.psi)+")");
            }
        }

        java.awt.Font font = new java.awt.Font("Serif", java.awt.Font.PLAIN, 10);
        
        return layoutColumns(content, width, height, font, strings);
    }
//}}}

//{{{ layoutColumns
//##############################################################################
    /**
    * Draws a series of text strings in either a 1- or 2-column layout.
    * @param content    the PdfContentByte object to create a template from.
    * @param width      the available width to draw in.
    * @param height     the available height to draw in.
    * @param font       the font to do the drawing in.
    * @param strings    a Collection of Strings to draw, one per line.
    * @return a PdfTemplate of arbitrary size. The caller is responsible for scaling
    * it to fit into a width x height area.
    */
    PdfTemplate layoutColumns(PdfContentByte content, float width, float height,
        java.awt.Font font, Collection stringList)
    {
        ArrayList strings = new ArrayList(stringList); // we need these as a List later
        
        // Calculate how big all these strings will be, and whether we should use 1 column or 2.
        // This template won't actually be used; it's just for FontMetrics info.
        PdfTemplate canvas = content.createTemplate(1, 1);
        Graphics2D g2 = canvas.createGraphics(canvas.getWidth(), canvas.getHeight());
        FontMetrics metrics = g2.getFontMetrics(font);
        int lineheight = metrics.getMaxAscent() + metrics.getMaxDescent() + 2;
        int height1 = strings.size() * lineheight;
        int height2 = (strings.size()+1)/2 * lineheight;
        int width1 = 0;
        for(Iterator iter = strings.iterator(); iter.hasNext(); )
            width1 = Math.max(width1, metrics.stringWidth((String)iter.next()));
        int widthL = 0;
        List halfL = strings.subList(0, strings.size()/2);
        for(Iterator iter = halfL.iterator(); iter.hasNext(); )
            widthL = Math.max(widthL, metrics.stringWidth((String)iter.next()));
        int widthR = 0;
        List halfR = strings.subList(strings.size()/2, strings.size());
        for(Iterator iter = halfR.iterator(); iter.hasNext(); )
            widthR = Math.max(widthR, metrics.stringWidth((String)iter.next()));
        int width2spacer = (int)Math.round(0.2 * (widthL+widthR));
        int width2 = widthL + widthR + width2spacer;
        g2.dispose();
        
        if(Math.min(width/width1, height/height1) > Math.min(width/width2, height/height2))
        // One column layout
        {
            canvas = content.createTemplate(width1, height1);
            g2 = canvas.createGraphics(canvas.getWidth(), canvas.getHeight());
            g2.setFont(font);
            int y = metrics.getMaxAscent();
            for(Iterator iter = strings.iterator(); iter.hasNext(); y += lineheight)
                g2.drawString((String)iter.next(), 0, y);
        }
        // Two column layout
        else
        {
            canvas = content.createTemplate(width2, height2);
            g2 = canvas.createGraphics(canvas.getWidth(), canvas.getHeight());
            g2.setFont(font);
            int y = metrics.getMaxAscent();
            for(Iterator iter = halfL.iterator(); iter.hasNext(); y += lineheight)
                g2.drawString((String)iter.next(), 0, y);
            y = metrics.getMaxAscent();
            for(Iterator iter = halfR.iterator(); iter.hasNext(); y += lineheight)
                g2.drawString((String)iter.next(), widthL+width2spacer, y);
        }
        
        g2.dispose();
        return canvas;
    }
//}}}

//{{{ addPageTitle
//##############################################################################
    /**
    * Centers the given string as a title on the current page.
    */
    void addPageTitle(String title, PdfContentByte content)
    {
        Document doc = content.getPdfDocument();
        PdfTemplate canvas = content.createTemplate(doc.getPageSize().getWidth(), doc.getPageSize().getHeight());
        Graphics2D g2 = canvas.createGraphics(canvas.getWidth(), canvas.getHeight());
        
        java.awt.Font font = new java.awt.Font("Serif", java.awt.Font.PLAIN, 10);
        FontMetrics metrics = g2.getFontMetrics(font);
        g2.setFont(font);
        g2.setColor(Color.black);
        int width = metrics.stringWidth(title);
        int height = metrics.getMaxAscent() + metrics.getMaxDescent();
        int x = ((int)canvas.getWidth() - width) / 2;
        /*int y = 72 + height/2;*/
        int y = 46 + height/2;
        g2.drawString(title, x, y);
        
        g2.dispose();
        content.addTemplate(canvas, 0, 0);
        
        return;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        try
        {
            // Otherwise, this will fail in a server environment!
            System.setProperty("java.awt.headless", "true");
            
            // Write out our template file to standard out
            URL templateFile = this.getClass().getResource("rama/rama6-template.pdf");
            /*URL templateFile = this.getClass().getResource("rama/rama4-template.pdf");*/
            PdfReader pdfReader = new PdfReader(templateFile);
    
            Document    doc = new Document(PageSize.LETTER);
            PdfWriter   pdf = PdfWriter.getInstance(doc, System.out);
            doc.addCreator(this.getClass().getName()+" by Ian W. Davis");
            doc.open();
            
            PdfContentByte content = pdf.getDirectContent();
            for(int i = 1; i <= pdfReader.getNumberOfPages(); i++)
            {
                doc.newPage();
                PdfTemplate template = pdf.getImportedPage(pdfReader, i);
                // Top, left, botttom, and right already include margins.
                // Coordinate system has bottom left corner as (0, 0)
                content.addTemplate(template, 0, 0);
            }
            
            // Closing the document writes everything to file
            doc.close();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }

    public static void main(String[] args)
    {
        RamaPdfWriter mainprog = new RamaPdfWriter();
        mainprog.Main();
    }
//}}}
}//class

