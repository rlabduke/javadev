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
    public boolean              doUntwistRibbons;
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
            
            //out.println("@vectorlist {protein ribbon5} color= "+bbColor+" master= {protein} master= {ribbon}");
            //rp.printFiveLine(guides, 4, true);
            
            out.println("@ribbonlist {protein ribbon} color= "+bbColor+" master= {protein} master= {ribbon}");
            rp.printFlatRibbon(guides, 4, true);
            
            out.println("@vectorlist {protein ribbon edges} color= deadblack master= {protein} master= {ribbon}");
            RibbonCrayon c = rp.getCrayon();
            rp.setCrayon(ConstCrayon.NONE);
            rp.printTwoLine(guides, 4, true);
            rp.setCrayon(c);
        }
    }
//}}}

//{{{ printNucAcid
//##############################################################################
    void printNucAcid(Model model, Set selectedRes, String bbColor)
    {
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

