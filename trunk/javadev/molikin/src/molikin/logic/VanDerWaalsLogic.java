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
* <code>VanDerWaalsLogic</code> handles a usual set of options and logic
* for VDW drawings of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class VanDerWaalsLogic
{
//{{{ Constants
    static final DecimalFormat df2 = driftwood.util.Strings.usDecimalFormat("0.0#");
    public static final Object COLOR_BY_ELEMENT     = "element";
    public static final Object COLOR_BY_RES_TYPE    = "residue type";
    public static final Object COLOR_BY_B_FACTOR    = "B factor";
    public static final Object COLOR_BY_OCCUPANCY   = "occupancy";
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter     out = null;
    BallPrinter     bp  = null;
    
    public boolean  doProtein, doNucleic, doHets, doIons, doWater;
    public boolean  doBackbone, doSidechains, doHydrogens, doUseSpheres;
    public Object   colorBy = COLOR_BY_ELEMENT;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public VanDerWaalsLogic()
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
        this.bp = new BallPrinter(out);
        bp.setCrayon(new AltConfCrayon());
        
        if(colorBy == COLOR_BY_ELEMENT)
            bp.setCrayon(new AltConfCrayon());
        else if(colorBy == COLOR_BY_RES_TYPE)
            bp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new ResTypeCrayon()));
        else if(colorBy == COLOR_BY_B_FACTOR)
            bp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new BfactorCrayon()));
        else if(colorBy == COLOR_BY_OCCUPANCY)
            bp.setCrayon(new CompositeCrayon().add(new AltConfCrayon()).add(new OccupancyCrayon()));
        else throw new UnsupportedOperationException();

        if(doProtein)  printProtein(m, residues, bbColor);
        if(doNucleic)  printNucAcid(m, residues, bbColor);
        if(doHets)     printHets(m, residues);
        if(doIons)     printIons(m, residues);
        if(doWater)    printWaters(m, residues);
        
        this.out.flush();
        this.out = null;
        this.bp = null;
    }
//}}}

//{{{ printProtein
//##############################################################################
    void printProtein(Model model, Set selectedRes, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet proteinRes = new CheapSet(selectedRes);
        proteinRes.retainAll(resC.proteinRes);
        if(proteinRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        Collection      bonds   = data.getCovalentGraph().getBonds();
        
        if(doBackbone && atomC.bbHeavy.size() > 0)
        {
            printAtomBalls(atomC.bbHeavy, proteinRes, "master= {protein} master= {backbone}");
            if(doHydrogens && atomC.bbHydro.size() > 0)
                printAtomBalls(atomC.bbHydro, proteinRes, "master= {protein} master= {backbone}");
        }
        if(doSidechains && atomC.scHeavy.size() > 0)
        {
            printAtomBalls(atomC.scHeavy, proteinRes, "master= {protein} master= {sidechains}");
            if(doHydrogens && atomC.scHydro.size() > 0)
                printAtomBalls(atomC.scHydro, proteinRes, "master= {protein} master= {sidechains}");
        }
    }
//}}}

//{{{ printNucAcid
//##############################################################################
    void printNucAcid(Model model, Set selectedRes, String bbColor)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet nucAcidRes = new CheapSet(selectedRes);
        nucAcidRes.retainAll(resC.nucAcidRes);
        if(nucAcidRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        Collection      bonds   = data.getCovalentGraph().getBonds();
        
        if(doBackbone && atomC.bbHeavy.size() > 0)
        {
            printAtomBalls(atomC.bbHeavy, nucAcidRes, "master= {nucleic acid} master= {backbone}");
            if(doHydrogens && atomC.bbHydro.size() > 0)
                printAtomBalls(atomC.bbHydro, nucAcidRes, "master= {nucleic acid} master= {backbone}");
        }
        if(doSidechains && atomC.scHeavy.size() > 0)
        {
            printAtomBalls(atomC.scHeavy, nucAcidRes, "master= {nucleic acid} master= {sidechains}");
            if(doHydrogens && atomC.scHydro.size() > 0)
                printAtomBalls(atomC.scHydro, nucAcidRes, "master= {nucleic acid} master= {sidechains}");
        }
    }
//}}}

//{{{ printHets
//##############################################################################
    void printHets(Model model, Set selectedRes)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet hetRes = new CheapSet(resC.ohetRes);
        hetRes.addAll(resC.unknownRes);
        hetRes.retainAll(selectedRes);
        if(hetRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        Collection      bonds   = data.getCovalentGraph().getBonds();
        
        if(atomC.hetHeavy.size() > 0)
        {
            printAtomBalls(atomC.hetHeavy, hetRes, "master= {hets}");
        }
        if(doHydrogens && atomC.hetHydro.size() > 0)
        {
            printAtomBalls(atomC.hetHydro, hetRes, "master= {hets}");
        }
    }
//}}}

//{{{ printIons
//##############################################################################
    void printIons(Model model, Set selectedRes)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet ionRes = new CheapSet(selectedRes);
        ionRes.retainAll(resC.ionRes);
        if(ionRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        if(atomC.ion.size() == 0) return;
        
        printAtomBalls(atomC.ion, ionRes, "master= {ions}");
    }
//}}}

//{{{ printWaters
//##############################################################################
    void printWaters(Model model, Set selectedRes)
    {
        DataCache       data    = DataCache.getDataFor(model);
        ResClassifier   resC    = data.getResClassifier();
        
        CheapSet waterRes = new CheapSet(selectedRes);
        waterRes.retainAll(resC.waterRes);
        if(waterRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        
        if(atomC.watHeavy.size() > 0)
        {
            printAtomBalls(atomC.watHeavy, waterRes, "master= {waters}");
        }
        if(doHydrogens && atomC.watHydro.size() > 0)
        {
            printAtomBalls(atomC.watHydro, waterRes, "master= {waters}");
        }
    }
//}}}

//{{{ printAtomBalls
//##############################################################################
    void printAtomBalls(Collection atomStates, Set residues, String masters)
    {
        // First, sort the AtomStates by element
        Map elementsToAtoms = new HashMap();
        for(Iterator iter = atomStates.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(!residues.contains(as.getResidue())) continue;
            String element = as.getElement();
            Collection atoms = (Collection) elementsToAtoms.get(element);
            if(atoms == null)
            {
                atoms = new ArrayList();
                elementsToAtoms.put(element, atoms);
            }
            atoms.add(as);
        }
        
        // Now print one balllist or spherelist per element
        for(Iterator iter = elementsToAtoms.keySet().iterator(); iter.hasNext(); )
        {
            String element = (String) iter.next();
            Collection atoms = (Collection) elementsToAtoms.get(element);
            String color = Util.getElementColor(element);
            double radius = Util.getVdwRadius(element);
            if(doUseSpheres)    out.print("@spherelist");
            else                out.print("@balllist");
            out.println(" {"+element+" vdW} color= "+color+" radius= "+df2.format(radius)+" master= {"+element+"} master= {vdW} "+masters);
            bp.printBalls(atoms);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

