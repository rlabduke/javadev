// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;
import molikin.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.data.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>VanDerWaalsPane</code> is the UI "page" for VDW drawings
* of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class VanDerWaalsPane extends TablePane2 implements DrawingPane
{
//{{{ Constants
    static final DecimalFormat df2 = new DecimalFormat("0.0#");
//}}}

//{{{ Variable definitions
//##############################################################################
    CoordinateFile  coordFile;
    String          title;
    
    PrintWriter     out = null;
    BallPrinter     bp  = null;
    
    SelectorPane    selector;
    JCheckBox       cbProtein, cbNucleic, cbHets, cbIons, cbWater;
    JCheckBox       cbBackbone, cbSidechains, cbHydrogens, cbUseSpheres;
    JComboBox       cmColorBy;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public VanDerWaalsPane(CoordinateFile cfile, String title)
    {
        super();
        this.coordFile  = cfile;
        this.title      = title;
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        selector = new SelectorPane(coordFile);
        
        cbProtein   = new JCheckBox("protein", true);
        cbNucleic   = new JCheckBox("nucleic acids", true);
        cbHets      = new JCheckBox("hets (non-water)", true);
        cbIons      = new JCheckBox("metals/ions", true);
        cbWater     = new JCheckBox("water", false);
        
        cbBackbone      = new JCheckBox("backbone", true);
        cbSidechains    = new JCheckBox("sidechain", true);
        cbHydrogens     = new JCheckBox("hydrogens", true);
        
        cbUseSpheres    = new JCheckBox("use spheres?", false);
        
        cmColorBy   = new JComboBox(new String[] {"element", "B factor", "occupancy"});
            cmColorBy.setEnabled(false);
        
        this.hfill(true).vfill(true).addCell(selector, 2, 1).newRow();
        this.weights(1,0).memorize();
        this.addCell(this.strut(0,6)).newRow();
        this.startSubtable(2,1);
            this.weights(0,0).memorize();
            this.addCell(new JLabel("Color by")).addCell(cmColorBy);
        this.endSubtable();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbProtein).addCell(cbBackbone).newRow();
        this.addCell(cbNucleic).addCell(cbSidechains).newRow();
        this.addCell(cbHets).addCell(cbHydrogens).newRow();
        this.addCell(cbIons).skip().newRow();
        this.addCell(cbWater).addCell(cbUseSpheres).newRow();
        
        this.setBorder( BorderFactory.createTitledBorder(null, "van der Waals") );
    }
//}}}

//{{{ toString, getSelectedModels, getSelectedChains
//##############################################################################
    public String toString()
    { return this.title; }
    
    /** As a Collection of Model objects. */
    public Collection getSelectedModels()
    { return selector.getSelectedModels(); }
    
    /** As a Collection of Strings representing chain IDs. */
    public Collection getSelectedChains()
    { return selector.getSelectedChains(); }
//}}}

//{{{ printKinemage
//##############################################################################
    /** Emits the kinemage (text) representation as selected by the user */
    public void printKinemage(PrintWriter out, Model m, String chainID, String bbColor)
    {
        Collection models = selector.getSelectedModels();
        if(!models.contains(m)) return;
        
        Collection chains = selector.getSelectedChains();
        if(!chains.contains(chainID)) return;
        
        Collection chainRes = m.getChain(chainID);
        if(chainRes == null) return;
        
        Set residues = selector.getSelectedResidues(chainRes);
        if(residues.size() == 0) return;
        
        this.out = out;
        this.bp = new BallPrinter(out);
        bp.setCrayon(new AltConfCrayon());
        
        if(cbProtein.isSelected())  printProtein(m, residues, bbColor);
        if(cbNucleic.isSelected())  printNucAcid(m, residues, bbColor);
        if(cbHets.isSelected())     printHets(m, residues);
        if(cbIons.isSelected())     printIons(m, residues);
        if(cbWater.isSelected())    printWaters(m, residues);
        
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
        
        if(cbBackbone.isSelected() && atomC.bbHeavy.size() > 0)
        {
            printAtomBalls(atomC.bbHeavy, proteinRes, "master= {protein} master= {backbone}");
            if(cbHydrogens.isSelected() && atomC.bbHydro.size() > 0)
                printAtomBalls(atomC.bbHydro, proteinRes, "master= {protein} master= {backbone}");
        }
        if(cbSidechains.isSelected() && atomC.scHeavy.size() > 0)
        {
            printAtomBalls(atomC.scHeavy, proteinRes, "master= {protein} master= {sidechains}");
            if(cbHydrogens.isSelected() && atomC.scHydro.size() > 0)
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
        
        if(cbBackbone.isSelected() && atomC.bbHeavy.size() > 0)
        {
            printAtomBalls(atomC.bbHeavy, nucAcidRes, "master= {nucleic acid} master= {backbone}");
            if(cbHydrogens.isSelected() && atomC.bbHydro.size() > 0)
                printAtomBalls(atomC.bbHydro, nucAcidRes, "master= {nucleic acid} master= {backbone}");
        }
        if(cbSidechains.isSelected() && atomC.scHeavy.size() > 0)
        {
            printAtomBalls(atomC.scHeavy, nucAcidRes, "master= {nucleic acid} master= {sidechains}");
            if(cbHydrogens.isSelected() && atomC.scHydro.size() > 0)
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
        if(cbHydrogens.isSelected() && atomC.hetHydro.size() > 0)
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
        if(cbHydrogens.isSelected() && atomC.watHydro.size() > 0)
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
            if(cbUseSpheres.isSelected())   out.print("@spherelist");
            else                            out.print("@balllist");
            out.println(" {"+element+" vdW} color= "+color+" radius= "+df2.format(radius)+" master= {"+element+"} master= {vdW} "+masters);
            bp.printBalls(atoms);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

