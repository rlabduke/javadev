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
    static final DecimalFormat df = new DecimalFormat("0.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter out;
    RibbonCrayon crayon = molikin.crayons.ConstCrayon.NONE;

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
        Triple      tmp     = new Triple();
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
                crayon.forRibbon(guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
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
        Triple      tmp     = new Triple();
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
            crayon.forRibbon(guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
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

