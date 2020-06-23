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

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.form.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.util.*;
import de.rototor.pdfbox.graphics2d.*;
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
      URL templateFile = this.getClass().getResource("rama8000/rama6-template.pdf");
      /*URL templateFile = this.getClass().getResource("rama5200/rama4-template.pdf");*/
      PDDocument ramaTemplate = PDDocument.load(templateFile.openStream());
      PDPage template = (PDPage) ramaTemplate.getDocumentCatalog().getPages().get(0);
      
      PDDocument    doc = new PDDocument();
      
      PDDocumentInformation pdd = doc.getDocumentInformation();
      pdd.setCreator(this.getClass().getName()+" by the Richardsons Lab");
      // add header and footer now, before opening document
      
      PDPage importedPage = doc.importPage(template);
      if (analyses.size() > 1) {
        for (int i = 0; i < analyses.size(); i++) {
          importedPage = doc.importPage(template);
        }
      }

      System.out.println(doc.getPages().getCount());
            
      doModelByModel(analyses, structName, doc);
      System.out.println("done models");
      // TODO: doResidueByResidue()

      doc.save(out);
      // Closing the document writes everything to file
      
      ramaTemplate.close();

      doc.close();
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
    * @param template   the six-square Rama page template object
    */
    void doModelByModel(Map analyses, String structName, PDDocument doc) throws IOException
    {
        int i = 0;
        PDFormXObject[] generalTemplates  = new PDFormXObject[analyses.size()];
        PDFormXObject[] ilevalTemplates   = new PDFormXObject[analyses.size()];
        PDFormXObject[] preproTemplates   = new PDFormXObject[analyses.size()];
        PDFormXObject[] glycineTemplates  = new PDFormXObject[analyses.size()];
        PDFormXObject[] transproTemplates = new PDFormXObject[analyses.size()];
        PDFormXObject[] cisproTemplates   = new PDFormXObject[analyses.size()];
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); i++)
        {
            Collection analysis  = (Collection) iter.next();
            generalTemplates[i]  = makeAnalysisTemplate(doc, Ramalyze.RamaEval.GENERAL, analysis);
            ilevalTemplates[i]   = makeAnalysisTemplate(doc, Ramalyze.RamaEval.ILEVAL, analysis);
            preproTemplates[i]   = makeAnalysisTemplate(doc, Ramalyze.RamaEval.PREPRO, analysis);
            glycineTemplates[i]  = makeAnalysisTemplate(doc, Ramalyze.RamaEval.GLYCINE, analysis);
            transproTemplates[i] = makeAnalysisTemplate(doc, Ramalyze.RamaEval.TRANSPRO, analysis);
            cisproTemplates[i]   = makeAnalysisTemplate(doc, Ramalyze.RamaEval.CISPRO, analysis);
            /*prolineTemplates[i] = makeAnalysisTemplate(content, Ramalyze.RamaEval.PROLINE, analysis);*/
        }
        
        // Do all plots superimposed
        if(analyses.size() > 1)
        {
          PDPage allPlotsPage = doc.getPage(0);
            PDPageContentStream contentStream = new PDPageContentStream(doc, allPlotsPage, PDPageContentStream.AppendMode.APPEND, false);
            for(i = 0; i < analyses.size(); i++)
            {
              addPlotToPage(contentStream, generalTemplates[i], PLOT_SCALE, PLOT_SCALE, 
                108.3f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
              addPlotToPage(contentStream, preproTemplates[i], PLOT_SCALE, PLOT_SCALE,
                    108.3f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
              addPlotToPage(contentStream, transproTemplates[i], PLOT_SCALE, PLOT_SCALE,
                    108.3f-PLOT_OFFSET, 156.0f-PLOT_OFFSET);
              addPlotToPage(contentStream, ilevalTemplates[i], PLOT_SCALE, PLOT_SCALE,
                    351.2f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
              addPlotToPage(contentStream, glycineTemplates[i], PLOT_SCALE, PLOT_SCALE,
                    351.2f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
              addPlotToPage(contentStream, cisproTemplates[i], PLOT_SCALE, PLOT_SCALE,
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
            contentStream.close();
            //PdfTemplate stats = plotStatistics(content, 540, 144, allRes);
            /*PdfTemplate stats = plotStatistics(content, 540, 130, allRes);*/
//            PdfTemplate stats = plotStatistics(content, 540, 80, allRes);
            //float scale = Math.min(540/stats.getWidth(), 144/stats.getHeight());
            /*float scale = Math.min(540/stats.getWidth(), 130/stats.getHeight());*/
//            float scale = Math.min(540/stats.getWidth(), 80/stats.getHeight());
//            if(scale > 1) scale = 1;
            //content.addTemplate(stats, scale, 0, 0, scale, 36, 36);
//            content.addTemplate(stats, scale, 0, 0, scale, 50, 50);
//            
            if(structName != null)  addPageTitle(structName+", all models", doc, allPlotsPage);
            else                    addPageTitle("All models", doc, allPlotsPage);

        }
        
        // Do each model individually
        i = 0;
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); i++)
        {
            Collection analysis = (Collection) iter.next();
            String label = (String) analyses.get(analysis);
            int pageIndex = i;
            if (analyses.size() > 1) {
              pageIndex = i+1;
            }
            PDPage modelPage = doc.getPage(pageIndex);

            PDPageContentStream contentStream = new PDPageContentStream(doc, modelPage, PDPageContentStream.AppendMode.APPEND, false);
            
            addPlotToPage(contentStream, generalTemplates[i], PLOT_SCALE, PLOT_SCALE,
                108.3f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
            addPlotToPage(contentStream, preproTemplates[i], PLOT_SCALE, PLOT_SCALE,
                108.3f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
            addPlotToPage(contentStream, transproTemplates[i], PLOT_SCALE, PLOT_SCALE,
                108.3f-PLOT_OFFSET, 156.0f-PLOT_OFFSET);
            addPlotToPage(contentStream, ilevalTemplates[i], PLOT_SCALE, PLOT_SCALE,
                351.2f-PLOT_OFFSET, 558.7f-PLOT_OFFSET);
            addPlotToPage(contentStream, glycineTemplates[i], PLOT_SCALE, PLOT_SCALE,
                351.2f-PLOT_OFFSET, 357.9f-PLOT_OFFSET);
            addPlotToPage(contentStream, cisproTemplates[i], PLOT_SCALE, PLOT_SCALE,
                351.2f-PLOT_OFFSET, 156.0f-PLOT_OFFSET);
            
            contentStream.close();
            /*content.addTemplate(generalTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                62.5f-PLOT_OFFSET, 480.5f-PLOT_OFFSET);
            content.addTemplate(glycineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                345.0f-PLOT_OFFSET, 480.5f-PLOT_OFFSET);
            content.addTemplate(prolineTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                62.5f-PLOT_OFFSET, 221.5f-PLOT_OFFSET);
            content.addTemplate(preproTemplates[i], PLOT_SCALE, 0, 0, PLOT_SCALE,
                345.0f-PLOT_OFFSET, 221.5f-PLOT_OFFSET);*/
            ArrayList statStrings = makeStatisticsStrings(analysis);
            writeStatsToPage(doc, modelPage, statStrings);
            /*PdfTemplate stats = plotStatistics(content, 540, 130, analysis);*/
//            PdfTemplate stats = plotStatistics(content, 80, 100, analysis);
            //float scale = Math.min(540/stats.getWidth(), 144/stats.getHeight());
            /*float scale = Math.min(540/stats.getWidth(), 130/stats.getHeight());*/
//            float scale = Math.min(540/stats.getWidth(), 80/stats.getHeight());
//            if(scale > 1) scale = 1;
            //content.addTemplate(stats, scale, 0, 0, scale, 36, 36);
//            content.addTemplate(stats, scale, 0, 0, scale, 50, 50);
            
            if(structName != null)  addPageTitle(structName+", model "+label, doc, modelPage);
            else                    addPageTitle("Model "+label, doc, modelPage);
        }
    }
//}}}

//{{{ makeAnalysisTemplate
//##############################################################################
    PDFormXObject makeAnalysisTemplate(PDDocument doc, String evalType, Collection analysis) throws IOException
    {
        PdfBoxGraphics2D g2 = new PdfBoxGraphics2D(doc, 360+2*PLOT_EXTRA, 360+2*PLOT_EXTRA);
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
        return g2.getXFormObject();
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


//{{{ makeStatisticsStrings
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
    ArrayList makeStatisticsStrings(Collection analysis)
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
        String overloadedInfo = "";
        if (outlier>64) {
          overloadedInfo = "This list is truncated, visit MolProbity for full list.";
        }
        ArrayList strings = new ArrayList();
        strings.add(df.format((100.0*favored)/total)+"% ("+favored+"/"+total+") of all residues were in favored (98%) regions.");
        strings.add(df.format((100.0*(favored+allowed))/total)+"% ("+(favored+allowed)+"/"+total+") of all residues were in allowed (>99.8%) regions.");
        strings.add(overloadedInfo); // blank line

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
        
        return strings;
    }
//}}}

//{{{ writeStatsToPage
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
    void writeStatsToPage(PDDocument doc, PDPage page, Collection stringList) throws IOException
    {
        ArrayList strings = new ArrayList(stringList); // we need these as a List later
        
        PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
        
        float lowerLeftX = page.getCropBox().getLowerLeftX();
        float lowerLeftY = page.getCropBox().getLowerLeftY();
        float[] columnXs = new float[]{lowerLeftX+30, lowerLeftX+120, lowerLeftX+210, lowerLeftX+300, lowerLeftX+390, lowerLeftX+480};
        float[] footerYs = new float[]{lowerLeftY+130, lowerLeftY+98, lowerLeftY+130, lowerLeftY+130, lowerLeftY+130, lowerLeftY+130};
        
        contentStream.setFont(PDType1Font.TIMES_ROMAN, 6);
        contentStream.setLeading(8);
        contentStream.beginText();
        
        int stringOutCount = 0;
        int columnCount = 0;
        
        contentStream.newLineAtOffset(columnXs[0], footerYs[0]);
        
        Iterator textIter = strings.iterator();
        while (textIter.hasNext()) {
          
          if (stringOutCount == 12 || (columnCount == 1 && stringOutCount == 8)) {
            columnCount++;
            if (columnCount < 6) {
              contentStream.endText();
              contentStream.beginText();
              contentStream.newLineAtOffset(columnXs[columnCount], footerYs[columnCount]);
            } else {
              break;
            }
            stringOutCount = 0;
          }
            
          String textLine = (String) textIter.next();
          contentStream.showText(textLine);
          contentStream.newLine();
          stringOutCount++;
        }
        contentStream.endText();
        contentStream.close();
    }
//}}}

//{{{ addPageTitle
//##############################################################################
    /**
    * Centers the given string as a title on the current page.
    */
    void addPageTitle(String title, PDDocument doc, PDPage page) throws IOException
    {
        float page_width = page.getArtBox().getWidth();
        float page_height = page.getArtBox().getHeight();     
        PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
        
        PDFont font = PDType1Font.TIMES_ROMAN;
        int fontSize = 10;
        float titleWidth = font.getStringWidth(title) / 1000 * fontSize;
        float titleHeight = font.getHeight(title.charAt(0)) / 1000 * fontSize; //only accounts for height of the first char due to limitations in pdfbox
        
        float x_centered = ( page_width - titleWidth ) / 2 + page.getArtBox().getLowerLeftX();
        float y = page.getCropBox().getUpperRightY() - (50 + titleHeight/2);
        contentStream.setFont(font, fontSize);
        contentStream.beginText();
        contentStream.newLineAtOffset(x_centered, y);
        contentStream.showText(title);
        contentStream.endText();
        contentStream.close();
        
    }
//}}}


//{{{ addPlotToPage

  public void addPlotToPage(PDPageContentStream contentStream, PDFormXObject plotXform, float xScale, float yScale, float xOffset, float yOffset) throws IOException {
    Matrix matrix = new Matrix();
    matrix.translate(xOffset, yOffset);
    matrix.scale(xScale, yScale);
    contentStream.transform(matrix);
    contentStream.drawForm(plotXform);
    matrix = new Matrix();
    matrix.scale(1/xScale, 1/yScale);
    matrix.translate(-xOffset, -yOffset);
    contentStream.transform(matrix);
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
            PDDocument pdfReader = PDDocument.load(templateFile.openStream());
            
            // Closing the document writes everything to file
            pdfReader.save(System.out);
            pdfReader.close();
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

