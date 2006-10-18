// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.logic;
import molikin.*;
import molikin.crayons.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>RibbonLogic</code> handles a usual set of options and logic for doing
* ribbon drawings of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class RibbonLogic
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter     out = null;
    RibbonPrinter   rp  = null;
    
    public boolean              doProtein, doNucleic;
    public boolean              doUntwistRibbons, doDnaStyle;
    public SecondaryStructure   secondaryStructure = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RibbonLogic()
    {
        super();
    }
//}}}

//{{{ printKinemage
//##############################################################################
    /** Emits the kinemage (text) representation as selected by the user */
    public void printKinemage(PrintWriter out, Model m, Set residues, String bbColor)
    {
        this.out = out;
        this.rp = new RibbonPrinter(out);
        if(secondaryStructure != null)
            rp.setCrayon(new ProteinSecStructCrayon(secondaryStructure));
        
        if(doProtein)   printProtein(m, residues, bbColor);
        if(doNucleic)   printNucAcid(m, residues, bbColor);
        
        this.out.flush();
        this.out = null;
        this.rp = null;
    }
//}}}

//{{{ printProtein
//##############################################################################
    void printProtein(Model model, Set selectedRes, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        ModelState      state   = model.getState();
        
        Ribbons ribbons = new Ribbons();
        Collection contigs = ribbons.getProteinContigs(selectedRes, state, resC);
        for(Iterator iter = contigs.iterator(); iter.hasNext(); )
        {
            Collection contig = (Collection) iter.next();
            if(contig.size() < 2) continue; // too small to use!
            GuidePoint[] guides = ribbons.makeProteinGuidepoints(contig, state);
            if(doUntwistRibbons) ribbons.untwistRibbon(guides);
            
            //rp.printGuidepoints(guides);
            
            if(secondaryStructure != null)
            {
                rp.printFancyRibbon(guides, secondaryStructure, 2, 2.2,
                    "color= red master= {protein} master= {ribbon} master= {alpha}",
                    "color= lime master= {protein} master= {ribbon} master= {beta}",
                    "width= 4 color= "+bbColor+" master= {protein} master= {ribbon} master= {coil}");
            }
            else
            {
                out.println("@ribbonlist {protein ribbon} color= "+bbColor+" master= {protein} master= {ribbon}");
                rp.printFlatRibbon(guides, 4, true);
                //out.println("@vectorlist {protein ribbon} color= "+bbColor+" master= {protein} master= {ribbon}");
                //rp.printFiveLine(guides, 4, true);
            }
        }
    }
//}}}

//{{{ printNucAcid
//##############################################################################
    void printNucAcid(Model model, Set selectedRes, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        ModelState      state   = model.getState();
        
        Ribbons ribbons = new Ribbons();
        Collection contigs = ribbons.getNucleicAcidContigs(selectedRes, state, resC);
        for(Iterator iter = contigs.iterator(); iter.hasNext(); )
        {
            Collection contig = (Collection) iter.next();
            //Residue[] res = (Residue[]) contig.toArray(new Residue[contig.size()]);
            //System.err.println(res[0]+" --> "+res[res.length-1]);
            if(contig.size() < 2) continue; // too small to use!
            GuidePoint[] guides = ribbons.makeNucleicAcidGuidepoints(contig, state);
            // Makes very little difference for nucleic acid, but occasionally does.
            if(doUntwistRibbons) ribbons.untwistRibbon(guides);
            if(doDnaStyle) ribbons.swapEdgeAndFace(guides);
            
            //rp.printGuidepoints(guides);
            
            if(secondaryStructure != null)
            {
                rp.printFancyRibbon(guides, secondaryStructure, 3.0, 3.0,
                    "color= red master= {nucleic acid} master= {ribbon} master= {RNA helix?}",
                    "color= lime master= {nucleic acid} master= {ribbon} master= {A-form}",
                    "width= 4 color= "+bbColor+" master= {nucleic acid} master= {ribbon} master= {coil}");
            }
            else
            {
                out.println("@ribbonlist {nucleic acid ribbon} color= "+bbColor+" master= {nucleic acid} master= {ribbon}");
                rp.printFlatRibbon(guides, 4, true);
                //out.println("@vectorlist {nucleic acid ribbon} color= "+bbColor+" master= {nucleic acid} master= {ribbon}");
                //rp.printFiveLine(guides, 4, true);
            }
            
            //out.println("@ribbonlist {nucleic acid ribbon} color= "+bbColor+" master= {nucleic acid} master= {ribbon}");
            //rp.printFlatRibbon(guides, 4, true);
            //
            //out.println("@vectorlist {nucleic acid ribbon edges} color= deadblack master= {nucleic acid} master= {ribbon}");
            //RibbonCrayon c = rp.getCrayon();
            //rp.setCrayon(ConstCrayon.NONE);
            //rp.printTwoLine(guides, 4, true);
            //rp.setCrayon(c);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

