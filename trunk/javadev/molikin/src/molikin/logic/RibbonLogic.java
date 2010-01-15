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
public class RibbonLogic implements Logic
{
//{{{ Constants
    public static final Object COLOR_BY_SEC_STRUCT  = "secondary structure";
    public static final Object COLOR_BY_RAINBOW     = "N -> C / 5' -> 3'";
    //public static final Object COLOR_BY_RES_TYPE    = "residue type";
    //public static final Object COLOR_BY_B_FACTOR    = "B factor";
    //public static final Object COLOR_BY_OCCUPANCY   = "occupancy";
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter     out = null;
    RibbonPrinter   rp  = null;
    
    public boolean  doProtein, doNucleic;
    public boolean  doUntwistRibbons, doDnaStyle;
    public boolean  doPlainCoils = false;
    public Object   colorBy = COLOR_BY_SEC_STRUCT;
    
    public SecondaryStructure secondaryStructure = null;
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
        
        // coloring set-up has to wait until contigs are calculated
        
        String chainID = "_";
        if(residues.size() > 0)
            chainID = ((Residue) residues.iterator().next()).getChain().trim();
        if(chainID.equals("")) chainID = "_";
        
        if(doProtein)   printProtein(m, residues, chainID, bbColor);
        if(doNucleic)   printNucAcid(m, residues, chainID, bbColor);
        
        this.out.flush();
        this.out = null;
        this.rp = null;
    }
    
    // to satisfy the interface
    public void printKinemage(PrintWriter out, Model m, Set residues, String pdbId, String bbColor) {
      printKinemage(out, m, residues, bbColor);
    }
//}}}

//{{{ setUpColoring
//##############################################################################
    private void setUpColoring(Collection contigs)
    {
        if(colorBy == COLOR_BY_SEC_STRUCT)
            rp.setCrayon(ConstCrayon.NONE);
        else if(colorBy == COLOR_BY_RAINBOW)
            rp.setCrayon(ResColorMapCrayon.newRainbow(contigs));
        else throw new UnsupportedOperationException();
    }
//}}}

//{{{ printProtein
//##############################################################################
    void printProtein(Model model, Set selectedRes, String chainID, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        AtomClassifier  atomC   = data.getAtomClassifier();
        ResClassifier   resC    = data.getResClassifier();
        ModelState      state   = model.getState();
        if (atomC.mcNotCa==0) secondaryStructure = new SecondaryStructure.AllCoil(); //to fix bug where CA only 
        //structure has helix classifications. See PDB 1hr3
        
        Ribbons ribbons = new Ribbons();
        Collection contigs = ribbons.getProteinContigs(selectedRes, state, resC);
        setUpColoring(contigs);
        rp.setRnaPointIDs(false);
        
        if(contigs.size() > 0 && secondaryStructure != null)
        {
            if(bbColor.equals("white"))
            {
                out.println("@colorset {alph"+chainID+"} red");
                out.println("@colorset {beta"+chainID+"} lime");
                out.println("@colorset {coil"+chainID+"} white");
            }
            else
            {
                out.println("@colorset {alph"+chainID+"} "+bbColor);
                out.println("@colorset {beta"+chainID+"} "+bbColor);
                out.println("@colorset {coil"+chainID+"} "+bbColor);
            }
        }
        
        int i = 0;
        for(Iterator iter = contigs.iterator(); iter.hasNext(); i++)
        {
            Collection contig = (Collection) iter.next();
            if(contig.size() < 2) continue; // too small to use!
            GuidePoint[] guides = ribbons.makeProteinGuidepoints(contig, state);
            //System.out.println("prot: "+contig.size()+" "+guides.length);
            if(doUntwistRibbons) ribbons.untwistRibbon(guides);
            
            //rp.printGuidepoints(guides);
            
            if(secondaryStructure != null)
            {
              if (doPlainCoils) {
                rp.printFancyRibbon(guides, secondaryStructure, 2, 2.2,
                    "color= {alph"+chainID+"} master= {protein} master= {ribbon} master= {alpha}",
                    "color= {beta"+chainID+"} master= {protein} master= {ribbon} master= {beta}",
                    "width= 4 color= {coil"+chainID+"} master= {protein} master= {ribbon} master= {coil}");
              } else {
                rp.printFancyRibbon(guides, secondaryStructure, 2, 2.2,
                    "color= {alph"+chainID+"} master= {protein} master= {ribbon} master= {alpha}",
                    "color= {beta"+chainID+"} master= {protein} master= {ribbon} master= {beta}",
                    "width= 4 fore color= {coil"+chainID+"} master= {protein} master= {ribbon} master= {coil}",
                    "width= 6 rear color= deadblack master= {protein} master= {ribbon} master= {coil}");
              }
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
    void printNucAcid(Model model, Set selectedRes, String chainID, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        ModelState      state   = model.getState();
        
        Ribbons ribbons = new Ribbons();
        Collection contigs = ribbons.getNucleicAcidContigs(selectedRes, state, resC);
        setUpColoring(contigs);
        rp.setRnaPointIDs(true);
        
        if(contigs.size() > 0 && secondaryStructure != null)
        {
            if(bbColor.equals("white"))
            {
                out.println("@colorset {nucl"+chainID+"} lime");
                out.println("@colorset {ncoi"+chainID+"} white");
            }
            else
            {
                out.println("@colorset {nucl"+chainID+"} "+bbColor);
                out.println("@colorset {ncoi"+chainID+"} "+bbColor);
            }
        }
        
        for(Iterator iter = contigs.iterator(); iter.hasNext(); )
        {
            Collection contig = (Collection) iter.next();
            //Residue[] res = (Residue[]) contig.toArray(new Residue[contig.size()]);
            //System.err.println(res[0]+" --> "+res[res.length-1]);
            if(contig.size() < 2) continue; // too small to use!
            GuidePoint[] guides = ribbons.makeNucleicAcidGuidepoints(contig, state);
            //System.out.println("nuc: "+contig.size()+" "+guides.length);
            // Makes very little difference for nucleic acid, but occasionally does.
            if(doUntwistRibbons) ribbons.untwistRibbon(guides);
            if(doDnaStyle) ribbons.swapEdgeAndFace(guides);
            
            //rp.printGuidepoints(guides);
            
            if(secondaryStructure != null)
            {
                rp.printFancyRibbon(guides, secondaryStructure, 3.0, 3.0,
                    "color= {nucl"+chainID+"} master= {nucleic acid} master= {ribbon} master= {RNA helix?}",
                    "color= {nucl"+chainID+"} master= {nucleic acid} master= {ribbon} master= {A-form}",
                    "width= 4 color= {ncoi"+chainID+"} master= {nucleic acid} master= {ribbon} master= {coil}");
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

