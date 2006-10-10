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
            out.println("{guide point} "+guides[i].xyz.format(df));

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

//{{{ printUniformFiveLine
//##############################################################################
    /**
    * For debugging purposes, displays a 5-skein ribbon of uniform width.
    * Only points are generated; the client is responsible for writing "@vectorlist ...".
    */
    public void printUniformFiveLine(GuidePoint[] guides, int nIntervals)
    {
        int         len     = guides.length;
        NRUBS       nrubs   = new NRUBS();
        Triple      tmp     = new Triple();
        Triple[]    pts     = new Triple[len];
        for(int i = 0; i < len; i++) pts[i] = new Triple();
        
        final double halfWidth = 1.0;
        for(double strand = -1; strand <= 1; strand+=0.5)
        {
            for(int i = 0; i < len; i++)
                pts[i].like(guides[i].xyz).addMult(strand*halfWidth, guides[i].dvec);
            Tuple3[] spline = nrubs.spline(pts, nIntervals);
            for(int i = 0; i < spline.length; i++)
            {
                int startGuide = (i/nIntervals) + 1;
                crayon.forRibbon(guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
                // For this to make sense, we have to be able to restart line if there's a break
                //if(!crayon.shouldPrint()) continue;
                tmp.like(spline[i]); // because Tuple3 doesn't have a format() method
                out.println("{}"+(i==0 ? "P " : "")+crayon.getKinString()+" "+tmp.format(df));
            }
        }

        out.flush();
    }
//}}}

//{{{ get/setCrayon
//##############################################################################
    /** The RibbonCrayon used for coloring these sections. */
    public RibbonCrayon getCrayon()
    { return this.crayon; }
    /** The RibbonCrayon used for coloring these sections. */
    public void setCrayon(RibbonCrayon c)
    { this.crayon = c; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

