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
* <code>BallAndStickLogic</code> handles a usual set of options and logic
* for doing for (ball and) stick drawings of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class BallAndStickLogic implements Logic
{
//{{{ Constants
    public static final Object COLOR_BY_MC_SC       = "backbone / sidechain";
    public static final Object COLOR_BY_RES_TYPE    = "residue type";
    public static final Object COLOR_BY_ELEMENT     = "element";
    public static final Object COLOR_BY_B_FACTOR    = "B factor";
    public static final Object COLOR_BY_OCCUPANCY   = "occupancy";
    static final DecimalFormat df = new DecimalFormat("00");
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter     out = null;
    StickPrinter    sp  = null;
    BallPrinter     bp  = null;
    
    public boolean  doProtein, doNucleic, doHets, doIons, doWater;
    public boolean  doPseudoBB, doBackbone, doSidechains, doHydrogens, doDisulfides;
    public boolean  doBallsOnCarbon, doBallsOnAtoms;
    public Object   colorBy = COLOR_BY_MC_SC;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BallAndStickLogic()
    {
        super();
    }
//}}}

//{{{ printKinemage
//##############################################################################
    /** Emits the kinemage (text) representation as selected by the user */
    public void printKinemage(PrintWriter out, Model m, Set residues, String bbColor) {
      printKinemage(out, m, residues, "", bbColor);
    }

    public void printKinemage(PrintWriter out, Model m, Set residues, String pdbId, String bbColor)
    {
        this.out = out;
        this.sp = new StickPrinter(out);
        this.bp = new BallPrinter(out);
        
        if(colorBy == COLOR_BY_MC_SC)
        {
            sp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new DisulfideCrayon()));
            bp.setCrayon(new AltConfCrayon());
        }
        else if(colorBy == COLOR_BY_RES_TYPE)
        {
            sp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new ResTypeCrayon()));
            bp.setCrayon(new AltConfCrayon());
        }
        else if(colorBy == COLOR_BY_ELEMENT)
        {
            sp.setHalfBonds(true);
            sp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new HalfBondElementCrayon()));
            bp.setCrayon(new AltConfCrayon());
        }
        else if(colorBy == COLOR_BY_B_FACTOR)
        {
            sp.setHalfBonds(true);
            sp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new BfactorCrayon()));
            bp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new BfactorCrayon()));
        }
        else if(colorBy == COLOR_BY_OCCUPANCY)
        {
            sp.setHalfBonds(true);
            sp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new OccupancyCrayon()));
            bp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new OccupancyCrayon()));
        }
        else throw new UnsupportedOperationException();

        if(doProtein)  printProtein(m, residues, pdbId, bbColor);
        if(doNucleic)  printNucAcid(m, residues, pdbId, bbColor);
        if(doHets)     printHets(m, residues, pdbId);
        if(doIons)     printIons(m, residues, pdbId);
        if(doWater)    printWaters(m, residues, pdbId);
        
        this.out.flush();
        this.out = null;
        this.sp = null;
        this.bp = null;
    }
//}}}

//{{{ printProtein
//##############################################################################
    void printProtein(Model model, Set selectedRes, String pdbId, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet proteinRes = new CheapSet(selectedRes);
        proteinRes.retainAll(resC.proteinRes);
        if(proteinRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        Collection      bonds   = data.getCovalentGraph().getBonds();
        
        String identifier = " m"+df.format(Integer.parseInt(data.getModelId()))+"_"+pdbId.toLowerCase();
        
        if(doPseudoBB||atomC.bbNotCa==0)
        {
            String off = ((doBackbone&&atomC.bbNotCa!=0) ? " off" : "");
            out.println("@vectorlist {protein ca} color= "+bbColor+" master= {protein} master= {Calphas}"+off);
            PseudoBackbone pseudoBB = data.getPseudoBackbone();
            sp.printSticks(pseudoBB.getProteinBonds(), null, null, proteinRes, proteinRes, identifier);
        }
        if(doBackbone)
        {
            if(atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {protein bb} color= "+bbColor+" master= {protein} master= {backbone}");
                sp.printSticks(bonds, atomC.bbHeavy, atomC.bbHeavy, proteinRes, proteinRes, identifier);
                if(doHydrogens && atomC.bbHydro.size() > 0)
                {
                    out.println("@vectorlist {protein bbH} color= gray master= {protein} master= {backbone} master= {Hs}");
                    sp.printSticks(bonds, atomC.bbHydro, atomC.bbHeavy, proteinRes, proteinRes, identifier);
                }
                if(doBallsOnAtoms)
                {
                    printAtomBalls(atomC.bbHeavy, proteinRes,
                        (doBallsOnCarbon ? bbColor : null),
                        "master= {protein} master= {backbone}");
                }
            }
        }
        if(doSidechains)
        {
            if(atomC.scHeavy.size() > 0)
            {
                out.println("@vectorlist {protein sc} color= cyan master= {protein} master= {sidechains}");
                // to scHeavy instead of bioHeavy if we want to end at CB and add stubs to ribbon instead
                // to resC.proteinRes allows disulfides to cross over chains
                sp.printSticks(bonds, atomC.scHeavy, atomC.bioHeavy, proteinRes, resC.proteinRes, identifier);
                if(doHydrogens && atomC.scHydro.size() > 0)
                {
                    out.println("@vectorlist {protein scH} color= gray master= {protein} master= {sidechains} master= {Hs}");
                    // makes sure Gly 2HA connects to bb
                    sp.printSticks(bonds, atomC.scHydro, atomC.bioHeavy, proteinRes, proteinRes, identifier);
                }
                if(doBallsOnAtoms)
                {
                    printAtomBalls(atomC.scHeavy, proteinRes,
                        (doBallsOnCarbon ? "cyan" : null),
                        "master= {protein} master= {sidechains}");
                }
            }
        }
        if(doDisulfides)
        {
            Set ssRes = Util.selectDisulfideResidues(bonds);
            ssRes.retainAll(proteinRes);
            if(atomC.scHeavy.size() > 0 && ssRes.size() > 0)
            {
                out.println("@vectorlist {protein ss} color= yellow master= {protein} master= {disulfides}");
                sp.printSticks(bonds, atomC.scHeavy, atomC.bioHeavy, ssRes, null, identifier);
                if(doHydrogens && atomC.scHydro.size() > 0)
                {
                    out.println("@vectorlist {protein ssH} color= gray master= {protein} master= {disulfides} master= {Hs}");
                    sp.printSticks(bonds, atomC.scHydro, atomC.bioHeavy, ssRes, ssRes, identifier);
                }
                if(doBallsOnAtoms)
                {
                    printAtomBalls(atomC.scHeavy, ssRes,
                        (doBallsOnCarbon ? "cyan" : null),
                        "master= {protein} master= {disulfides}");
                }
            }
        }
    }
//}}}

//{{{ printNucAcid
//##############################################################################
    void printNucAcid(Model model, Set selectedRes, String pdbId, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet nucAcidRes = new CheapSet(selectedRes);
        nucAcidRes.retainAll(resC.nucAcidRes);
        if(nucAcidRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        Collection      bonds   = data.getCovalentGraph().getBonds();
        
        // Needs an extra space for some reason.
        String identifier = " m"+df.format(Integer.parseInt(data.getModelId()))+"_"+pdbId.toLowerCase();
        
        if(doPseudoBB)
        {
            String off = (doBackbone ? " off" : "");
            out.println("@vectorlist {nuc. acid pseudobb} color= "+bbColor+" master= {nucleic acid} master= {pseudo-bb}"+off);
            PseudoBackbone pseudoBB = data.getPseudoBackbone();
            sp.printSticks(pseudoBB.getNucAcidBonds(), null, null, nucAcidRes, nucAcidRes, identifier);
        }
        if(doBackbone)
        {
            if(atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {nuc. acid bb} color= "+bbColor+" master= {nucleic acid} master= {backbone}");
                sp.printSticks(bonds, atomC.bbHeavy, atomC.bbHeavy, nucAcidRes, nucAcidRes, identifier);
                if(doHydrogens && atomC.bbHydro.size() > 0)
                {
                    out.println("@vectorlist {nuc. acid bbH} color= gray master= {nucleic acid} master= {backbone} master= {Hs}");
                    sp.printSticks(bonds, atomC.bbHydro, atomC.bbHeavy, nucAcidRes, nucAcidRes, identifier);
                }
                if(doBallsOnAtoms)
                {
                    printAtomBalls(atomC.bbHeavy, nucAcidRes,
                        (doBallsOnCarbon ? bbColor : null),
                        "master= {nucleic acid} master= {backbone}");
                }
            }
        }
        if(doSidechains)
        {
            if(atomC.scHeavy.size() > 0)
            {
                out.println("@vectorlist {nuc. acid sc} color= cyan master= {nucleic acid} master= {sidechains}");
                // to scHeavy if we want stubs to ribbon instead
                sp.printSticks(bonds, atomC.scHeavy, atomC.bioHeavy, nucAcidRes, nucAcidRes, identifier);
                if(doHydrogens && atomC.scHydro.size() > 0)
                {
                    out.println("@vectorlist {nuc. acid scH} color= gray master= {nucleic acid} master= {sidechains} master= {Hs}");
                    sp.printSticks(bonds, atomC.scHydro, atomC.scHeavy, nucAcidRes, nucAcidRes, identifier);
                }
                if(doBallsOnAtoms)
                {
                    printAtomBalls(atomC.scHeavy, nucAcidRes,
                        (doBallsOnCarbon ? "cyan" : null),
                        "master= {nucleic acid} master= {sidechains}");
                }
            }
        }
    }
//}}}

//{{{ printHets
//##############################################################################
    void printHets(Model model, Set selectedRes, String pdbId)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet hetRes = new CheapSet(resC.ohetRes);
        hetRes.addAll(resC.unknownRes);
        hetRes.retainAll(selectedRes);
        if(hetRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        Collection      bonds   = data.getCovalentGraph().getBonds();
        
        String identifier = " m"+df.format(Integer.parseInt(data.getModelId()))+"_"+pdbId.toLowerCase();
        
        // First, the hets themselves.
        if(atomC.hetHeavy.size() == 0) return;
        out.println("@vectorlist {het} color= pink master= {hets}");
        sp.printSticks(bonds, atomC.hetHeavy, atomC.hetHeavy, hetRes, hetRes, identifier);

        if(doBallsOnAtoms)
        {
            printAtomBalls(atomC.hetHeavy, hetRes,
                (doBallsOnCarbon ? "pink" : null),
                "master= {hets}");
        }
        
        if(doHydrogens && atomC.hetHydro.size() > 0)
        {
            out.println("@vectorlist {hetH} color= gray master= {hets} master= {Hs}");
            sp.printSticks(bonds, atomC.hetHydro, atomC.hetHeavy, hetRes, hetRes, identifier);
        }
        // Now, the connections to protein.
        if(doProtein)
        {
            CheapSet proteinRes = new CheapSet(selectedRes);
            proteinRes.retainAll(resC.proteinRes);

            if(proteinRes.size() > 0 && doBackbone && atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {het - protein bb} color= pinktint master= {hets} master= {protein} master= {backbone}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.bbHeavy, hetRes, proteinRes, identifier);
            }
            if(proteinRes.size() > 0 && doSidechains && atomC.scHeavy.size() > 0)
            {
                out.println("@vectorlist {het - protein sc} color= pinktint master= {hets} master= {protein} master= {sidechains}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.scHeavy, hetRes, proteinRes, identifier);
            }
        }
        // Finally, the connections to nucleic acid.
        if(doNucleic)
        {
            CheapSet nucAcidRes = new CheapSet(selectedRes);
            nucAcidRes.retainAll(resC.nucAcidRes);

            if(nucAcidRes.size() > 0 && doBackbone && atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {het - nuc. acid bb} color= pinktint master= {hets} master= {nucleic acid} master= {backbone}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.bbHeavy, hetRes, nucAcidRes, identifier);
            }
            if(nucAcidRes.size() > 0 && doSidechains && atomC.scHeavy.size() > 0)
            {
                out.println("@vectorlist {het - nuc. acid sc} color= pinktint master= {hets} master= {nucleic acid} master= {sidechains}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.scHeavy, hetRes, nucAcidRes, identifier);
            }
        }
    }
//}}}

//{{{ printIons
//##############################################################################
    void printIons(Model model, Set selectedRes, String pdbId)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet ionRes = new CheapSet(selectedRes);
        ionRes.retainAll(resC.ionRes);
        if(ionRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        if(atomC.ion.size() == 0) return;
        
        String identifier = " m"+df.format(Integer.parseInt(data.getModelId()))+"_"+pdbId.toLowerCase();
        
        // 0.5 is the Prekin default metal radius
        out.println("@spherelist {ions} color= gray radius= 0.5 master= {ions}");
        bp.printBalls(atomC.ion, ionRes, identifier);
    }
//}}}

//{{{ printWaters
//##############################################################################
    void printWaters(Model model, Set selectedRes, String pdbId)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet waterRes = new CheapSet(selectedRes);
        waterRes.retainAll(resC.waterRes);
        if(waterRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        if(atomC.watHeavy.size() == 0) return;
        
        String identifier = " m"+df.format(Integer.parseInt(data.getModelId()))+"_"+pdbId.toLowerCase();
        
        out.println("@balllist {waters} color= peachtint radius= 0.15 master= {waters}");
        bp.printBalls(atomC.watHeavy, waterRes, identifier);

        if(doHydrogens && atomC.watHydro.size() > 0)
        {
            Collection      bonds   = data.getCovalentGraph().getBonds();
            out.println("@vectorlist {waterH} color= gray master= {waters} master= {Hs}");
            sp.printSticks(bonds, atomC.watHydro, atomC.watHeavy, waterRes, waterRes, identifier);
        }
    }
//}}}

//{{{ printAtomBalls
//##############################################################################
    /** null for carbonColor means leave off carbon balls */
    void printAtomBalls(Collection atomStates, Set residues, String carbonColor, String masters)
    {
        // First, sort the AtomStates by element
        Map elementsToAtoms = new HashMap();
        for(Iterator iter = atomStates.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            String elem = as.getElement();
            // Remove carbon if no color has been specified; always remove H and Q
            if(elem.equals("H") || elem.equals("Q") || (carbonColor == null && elem.equals("C")))
                continue;
            if(!residues.contains(as.getResidue())) continue;
            Collection atoms = (Collection) elementsToAtoms.get(elem);
            if(atoms == null)
            {
                atoms = new ArrayList();
                elementsToAtoms.put(elem, atoms);
            }
            atoms.add(as);
        }
        
        // Now print one balllist per element
        for(Iterator iter = elementsToAtoms.keySet().iterator(); iter.hasNext(); )
        {
            String element = (String) iter.next();
            Collection atoms = (Collection) elementsToAtoms.get(element);
            String color = Util.getElementColor(element);
            // Matching carbon color to backbone, sidechain, etc. doesn't really work, visually.
            //if(carbonColor != null && element.equals("C")) color = carbonColor;
            out.println("@balllist {"+element+" balls} color= "+color+" radius= 0.2 master= {"+element+"} "+masters);
            bp.printBalls(atoms);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

