// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>RibbonPrinter</code> prints various ribbon representations.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan 24 13:18:01 EST 2006
*/
public class RibbonPrinter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df = driftwood.util.Strings.usDecimalFormat("0.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter out;
    RibbonCrayon crayon = molikin.crayons.ConstCrayon.NONE;
    
    Triple tmp = new Triple();

    // From PKININIT.c (ribwidalpha, ribwidbeta, ribwidcoil)
    double widthAlpha   = 2.0;
    double widthBeta    = 2.2;
    double widthCoil    = 1.0;
    double widthDefault = 2.0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RibbonPrinter(PrintWriter out)
    {
        super();
        this.out = out;
    }
//}}}

//{{{ printGuidepoints
//##############################################################################
    /**
    * For debugging purposes, displays several of the geometric constructions
    * used in building ribbons. Several kinemage lists are generated.
    */
    public void printGuidepoints(GuidePoint[] guides)
    {
        Triple end = new Triple();

        out.println("@balllist {guide points} color= red radius= 0.20 master= {guidepts}");
        for(int i = 0; i < guides.length; i++)
            out.println("{guide point off="+df.format(guides[i].offsetFactor)+" wid="+df.format(guides[i].widthFactor)+"} "+guides[i].xyz.format(df));

        out.println("@vectorlist {c vectors} color= green master= {guidepts}");
        for(int i = 0; i < guides.length; i++)
        {
            end.likeSum(guides[i].xyz, guides[i].cvec);
            out.println("{c}P "+guides[i].xyz.format(df)+" {\"}L "+end.format(df));
        }

        out.println("@vectorlist {d vectors} color= blue master= {guidepts}");
        for(int i = 0; i < guides.length; i++)
        {
            end.likeSum(guides[i].xyz, guides[i].dvec);
            out.println("{d}P "+guides[i].xyz.format(df)+" {\"}L "+end.format(df));
        }

        out.flush();
    }
//}}}

//{{{ printOne/Two/Three/FiveLine
//##############################################################################
    public void printOneLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, 0, 99); }
    
    public void printTwoLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, -1, 2); }
    
    public void printThreeLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, -1, 1); }
    
    public void printFiveLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, -1, 0.5); }
//}}}

//{{{ printNLineImpl
//##############################################################################
    /**
    * Displays an N-skein ribbon of uniform or variable width.
    * Only points are generated; the client is responsible for writing "@vectorlist ...".
    */
    public void printNLineImpl(GuidePoint[] guides, int nIntervals, boolean variableWidth, double strandStart, double strandStride)
    {
        int         len     = guides.length;
        NRUBS       nrubs   = new NRUBS();
        Triple[]    pts     = new Triple[len];
        for(int i = 0; i < len; i++) pts[i] = new Triple();
        
        for(double strand = strandStart; strand <= 1; strand+=strandStride)
        {
            for(int i = 0; i < len; i++)
            {
                double ribwid = (guides[i].offsetFactor > 0 ? widthAlpha : widthBeta);
                double halfWidth = 0.5 * (widthCoil + guides[i].widthFactor*(ribwid - widthCoil));
                if(!variableWidth) halfWidth = widthDefault / 2.0;
                pts[i].like(guides[i].xyz).addMult(strand*halfWidth, guides[i].dvec);
            }
            Tuple3[] spline = nrubs.spline(pts, nIntervals);
            boolean isBreak = true;
            for(int i = 0; i < spline.length; i++)
            {
                int startGuide = (i/nIntervals) + 1;
                crayon.forRibbon(spline[i], guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
                // For this to make sense, we have to be able to restart line if there's a break
                if(!crayon.shouldPrint()) { isBreak = true; continue; }
                tmp.like(spline[i]); // because Tuple3 doesn't have a format() method
                out.println("{}"+(isBreak ? "P " : "")+crayon.getKinString()+" "+tmp.format(df));
                isBreak = false;
            }
        }

        out.flush();
    }
//}}}

//{{{ printFlatRibbon
//##############################################################################
    /**
    * Displays a triangulated ribbon of uniform or variable width.
    * Only points are generated; the client is responsible for writing "@ribbonlist ...".
    */
    public void printFlatRibbon(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    {
        int         len     = guides.length;
        NRUBS       nrubs   = new NRUBS();
        Triple[]    pts1    = new Triple[len];
        Triple[]    pts2    = new Triple[len];
        for(int i = 0; i < len; i++)
        {
            pts1[i] = new Triple();
            pts2[i] = new Triple();
        }
        
        for(int i = 0; i < len; i++)
        {
            double ribwid = (guides[i].offsetFactor > 0 ? widthAlpha : widthBeta);
            double halfWidth = 0.5 * (widthCoil + guides[i].widthFactor*(ribwid - widthCoil));
            if(!variableWidth) halfWidth = widthDefault / 2.0;
            pts1[i].like(guides[i].xyz).addMult( halfWidth, guides[i].dvec);
            pts2[i].like(guides[i].xyz).addMult(-halfWidth, guides[i].dvec);
        }
        Tuple3[] spline1 = nrubs.spline(pts1, nIntervals);
        Tuple3[] spline2 = nrubs.spline(pts2, nIntervals);
        boolean isBreak = true;
        for(int i = 0; i < spline1.length; i++)
        {
            int startGuide = (i/nIntervals) + 1;
            tmp.likeMidpoint(spline1[i], spline2[i]);
            crayon.forRibbon(tmp, guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
            // For this to make sense, we have to be able to restart line if there's a break
            if(!crayon.shouldPrint()) { isBreak = true; continue; }
            tmp.like(spline1[i]); // because Tuple3 doesn't have a format() method
            out.println("{}"+(isBreak ? "X " : "")+crayon.getKinString()+" "+tmp.format(df));
            tmp.like(spline2[i]);
            out.println("{}"+crayon.getKinString()+" "+tmp.format(df));
            isBreak = false;
        }

        out.flush();
    }
//}}}

//{{{ CLASS: RibbonElement
//##############################################################################
    static class RibbonElement
    {
        int start = 0, end = 0;
        Object type = SecondaryStructure.COIL;
        SecondaryStructure.Range range = null;
        
        public RibbonElement() {}
        
        public RibbonElement(RibbonElement that)
        { this.like(that); }
        
        public RibbonElement(SecondaryStructure.Range range)
        {
            this.range = range;
            if(range == null) type = SecondaryStructure.COIL;
            else type = range.getType();
            if(type == SecondaryStructure.TURN) type = SecondaryStructure.COIL;
        }
        
        public boolean sameSSE(RibbonElement that)
        { return (this.type == that.type && this.range == that.range); }
        
        public void like(RibbonElement that)
        {
            this.start  = that.start;
            this.end    = that.end;
            this.type   = that.type;
            this.range  = that.range;
        }
    }
//}}}

//{{{ printFancyRibbon, printFancy
//##############################################################################
    /**
    * Displays a triangulated ribbon of with arrowheads, etc.
    * Several lists are generated, but with additional parameters specified below.
    */
    public void printFancyRibbon(GuidePoint[] guides, SecondaryStructure secStruct,
        double widthAlpha, double widthBeta, String listAlpha, String listBeta, String listCoil)
    {
        // Data allocation, splining {{{
        final int   nIntervals = 4;
        int         len     = guides.length;
        NRUBS       nrubs   = new NRUBS();
        
        // Seven stands of guidepoints: coil, alpha, beta, (beta arrowheads, see below)
        double[] halfWidths = {0, -widthAlpha/2, widthAlpha/2, -widthBeta/2, widthBeta/2, -widthBeta, widthBeta};
        Triple[][] guidepts = new Triple[halfWidths.length][guides.length];
        for(int i = 0; i < guidepts.length; i++)
            for(int j = 0; j < guidepts[i].length; j++)
                guidepts[i][j] = new Triple(guides[j].xyz).addMult(halfWidths[i], guides[j].dvec);
        // Seven strands of interpolated points
        Tuple3[][] splinepts = new Tuple3[halfWidths.length][];
        for(int i = 0; i < halfWidths.length; i++)
            splinepts[i] = nrubs.spline(guidepts[i], nIntervals);
        // I'm pretty sure this isn't needed now:
        // Arrow heads -- can't just spline some offset guidepoints
        // or there's a small break between the arrow body and arrow head.
        //splinepts[halfWidths.length  ] = new Triple[ splinepts[0].length ];
        //splinepts[halfWidths.length+1] = new Triple[ splinepts[0].length ];
        //for(int i = 0; i < splinepts[0].length; i++)
        //{
        //    splinepts[halfWidths.length  ][i] = new Triple().likeVector(splinepts[4][i], splinepts[3][i]).div(2).add(splinepts[3][i]);
        //    splinepts[halfWidths.length+1][i] = new Triple().likeVector(splinepts[3][i], splinepts[4][i]).div(2).add(splinepts[4][i]);
        //}
        // Data allocation, splining }}}
        
        // Discovery of ribbon elements: ribbons, ropes, and arrows {{{
        ArrayList ribbonElements = new ArrayList();
        RibbonElement ribElement = new RibbonElement();
        ribbonElements.add(ribElement);
        ribElement.type = null;
        
        for(int i = 0; i < guides.length-3; i++)
        {
            GuidePoint g1 = guides[i+1], g2 = guides[i+2];
            // These two won't really be ribbon elements; we're just reusing the class for convenience.
            RibbonElement currSS = new RibbonElement(secStruct.getRange(g1.nextRes));
            RibbonElement nextSS = new RibbonElement(secStruct.getRange(g2.nextRes));
            // Otherwise, we get one unit of coil before alpha or beta at start
            if(ribElement.type == null) ribElement.like(currSS);
            
            if(!ribElement.sameSSE(currSS)) // helix / sheet starting
            {
                if(currSS.type == SecondaryStructure.HELIX
                || currSS.type == SecondaryStructure.STRAND)
                {
                    ribElement.end = nIntervals*i + 1;
                    ribbonElements.add(ribElement = new RibbonElement(currSS));
                    // Every helix / sheet starts from coil; see below
                    ribElement.start = nIntervals*i + 1;
                }
            }
            if(!ribElement.sameSSE(nextSS)) // helix / sheet ending
            {
                if(currSS.type == SecondaryStructure.HELIX
                || currSS.type == SecondaryStructure.STRAND)
                {
                    int end = nIntervals*i + 0;
                    if(currSS.type == SecondaryStructure.STRAND) end += nIntervals - 1;
                    ribElement.end = end;
                    ribbonElements.add(ribElement = new RibbonElement());
                    // Every helix / sheet flows into coil
                    ribElement.type = SecondaryStructure.COIL;
                    ribElement.start = end;
                }
            }
        }
        ribElement.end = splinepts[0].length-1;
        // Discovery of ribbon elements: ribbons, ropes, and arrows }}}
        
        for(Iterator iter = ribbonElements.iterator(); iter.hasNext(); )
        {
            ribElement = (RibbonElement) iter.next();
            //System.err.println(ribElement.type+"    ["+ribElement.start+", "+ribElement.end+"]");
            if(ribElement.type == SecondaryStructure.HELIX) //{{{
            {
                out.println("@ribbonlist {fancy helix} "+listAlpha);
                for(int i = ribElement.start; i < ribElement.end; i++)
                {
                    printFancy(guides, splinepts[1], i);
                    printFancy(guides, splinepts[2], i);
                }
                printFancy(guides, splinepts[0], ribElement.end); // angled tip at end of helix
                out.println("@vectorlist {fancy helix edges} width= 1 "+listAlpha+" color= deadblack");
                // black edge, left side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end; i++)
                    printFancy(guides, splinepts[1], i);
                printFancy(guides, splinepts[0], ribElement.end);
                // black edge, right side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end; i++)
                    printFancy(guides, splinepts[2], i);
                printFancy(guides, splinepts[0], ribElement.end);
            } //}}}
            else if(ribElement.type == SecondaryStructure.STRAND) //{{{
            {
                out.println("@ribbonlist {fancy sheet} "+listBeta);
                for(int i = ribElement.start; i < ribElement.end-1; i++)
                {
                    printFancy(guides, splinepts[3], i);
                    printFancy(guides, splinepts[4], i);
                }
                // Ending *exactly* like this is critical to avoiding a break
                // between the arrow body and arrow head!
                printFancy(guides, splinepts[5], ribElement.end-2);
                printFancy(guides, splinepts[6], ribElement.end-2);
                printFancy(guides, splinepts[0], ribElement.end);
                out.println("@vectorlist {fancy sheet edges} width= 1 "+listBeta+" color= deadblack");
                // black edge, left side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end-1; i++)
                    printFancy(guides, splinepts[3], i);
                printFancy(guides, splinepts[5], ribElement.end-2);
                printFancy(guides, splinepts[0], ribElement.end);
                // black edge, right side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end-1; i++)
                    printFancy(guides, splinepts[4], i);
                printFancy(guides, splinepts[6], ribElement.end-2);
                printFancy(guides, splinepts[0], ribElement.end);
            } //}}}
            else // COIL {{{
            {
                out.println("@vectorlist {fancy coil} "+listCoil);
                for(int i = ribElement.start; i <= ribElement.end; i++)
                    printFancy(guides, splinepts[0], i);
            } //}}}
        }

        out.flush();
    }
    
    private void printFancy(GuidePoint[] guides, Tuple3[] splines, int i)
    { printFancy(guides, splines, i, false); }
    
    private void printFancy(GuidePoint[] guides, Tuple3[] splines, int i, boolean lineBreak)
    {
        int nIntervals = (splines.length - 1) / (guides.length - 3);
        int startGuide = (i / nIntervals) + 1;
        tmp.like(splines[i]); // because Tuple3 doesn't have a format() command
        out.print("{}");
        if(lineBreak) out.print("P ");
        crayon.forRibbon(splines[i], guides[startGuide], guides[startGuide+1], i % nIntervals, nIntervals);
        out.print(crayon.getKinString());
        out.print(" ");
        out.println(tmp.format(df));
    }
//}}}

//{{{ get/setCrayon, setWidth
//##############################################################################
    /** The RibbonCrayon used for coloring these sections. */
    public RibbonCrayon getCrayon()
    { return this.crayon; }
    /** The RibbonCrayon used for coloring these sections. */
    public void setCrayon(RibbonCrayon c)
    { this.crayon = c; }
    
    /** Sets the width for constant-width ribbons.  Default is 2.0. */
    public void setWidth(double w)
    { this.widthDefault = w; }
    /**
    * Sets the width for variable-width ribbons.
    * Default is (2.0, 2.2, 1.0) for protein and (3.0, 3.0, 3.0) for nucleic acids.
    */
    public void setWidth(double alpha, double beta, double coil)
    {
        this.widthAlpha = alpha;
        this.widthBeta  = beta;
        this.widthCoil  = coil;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

