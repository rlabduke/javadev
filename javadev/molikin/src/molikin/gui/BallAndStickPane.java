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
* <code>BallAndStickPane</code> is the UI "page" for (ball and) stick drawings
* of macromolecular structures.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  5 10:58:41 EDT 2005
*/
public class BallAndStickPane extends TablePane2
{
//{{{ Constants
    // pinktint is not used b/c that's used for connections to hets
    static final String[] BACKBONE_COLORS = { "white", "yellowtint", "peachtint", "greentint", "bluetint", "lilactint" };
//}}}

//{{{ Variable definitions
//##############################################################################
    CoordinateFile  coordFile;
    String          idCode;
    
    PrintWriter     out = null;
    StickPrinter    sp  = null;
    BallPrinter     bp  = null;
    
    SelectorPane    selector;
    JCheckBox       cbProtein, cbNucleic, cbHets, cbIons, cbWater;
    JCheckBox       cbPseudoBB, cbBackbone, cbSidechains, cbHydrogens;
    JCheckBox       cbBallsOnCarbon, cbBallsOnAtoms;
    JComboBox       cmColorBy;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BallAndStickPane(CoordinateFile cfile)
    {
        super();
        this.coordFile  = cfile;
        
        if(coordFile.getIdCode() != null)       this.idCode = coordFile.getIdCode();
        else if(coordFile.getFile() != null)    this.idCode = coordFile.getFile().getName();
        else                                    this.idCode = "macromol";
        
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
        cbWater     = new JCheckBox("water", true);
        
        cbPseudoBB      = new JCheckBox("C-alpha trace", true);
            cbPseudoBB.setEnabled(false);
        cbBackbone      = new JCheckBox("backbone", true);
        cbSidechains    = new JCheckBox("sidechain", true);
        cbHydrogens     = new JCheckBox("hydrogens", true);
        
        cbBallsOnCarbon     = new JCheckBox("balls on C atoms too", false);
        cbBallsOnAtoms      = new JCheckBox("balls on N, O, P, etc.", false);
        
        cmColorBy   = new JComboBox(new String[] {"backbone / sidechain", "element (half bonds)", "B factor", "occupancy"});
            cmColorBy.setEnabled(false);
        
        this.hfill(true).vfill(true).addCell(selector, 2, 1).newRow();
        this.weights(1,0).memorize();
        this.addCell(this.strut(0,6)).newRow();
        this.startSubtable(2,1);
            this.weights(0,0).memorize();
            this.addCell(new JLabel("Color by")).addCell(cmColorBy);
        this.endSubtable();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbProtein).addCell(cbPseudoBB).newRow();
        this.addCell(cbNucleic).addCell(cbBackbone).newRow();
        this.addCell(cbHets).addCell(cbSidechains).newRow();
        this.addCell(cbIons).addCell(cbHydrogens).newRow();
        this.addCell(cbWater).skip().newRow();
        this.addCell(this.strut(0,6)).newRow();
        this.addCell(cbBallsOnAtoms).addCell(cbBallsOnCarbon).newRow();
        
        this.setBorder( BorderFactory.createTitledBorder(null, "Ball & stick") );
    }
//}}}

//{{{ printKinemage
//##############################################################################
    /** Emits the kinemage (text) representation as selected by the user */
    public void printKinemage(PrintWriter out)
    {
        this.out = out;
        this.sp = new StickPrinter(out);
        this.bp = new BallPrinter(out);
        
        Collection models = selector.getSelectedModels();
        boolean groupByModel = (models.size() > 1);
        Collection chains = selector.getSelectedChains();
        
        for(Iterator mi = models.iterator(); mi.hasNext(); )
        {
            Model m = (Model) mi.next();
            if(groupByModel) out.println("@group {"+idCode+" "+m+"} dominant animate master= {all models}");
            
            int chainNum = 0;
            for(Iterator ci = chains.iterator(); ci.hasNext(); chainNum++)
            {
                String chainID = (String) ci.next();
                Collection chainRes = m.getChain(chainID);
                if(chainRes == null) continue;

                Set residues = selector.getSelectedResidues(chainRes);
                if(residues.size() == 0) continue;
                
                if(groupByModel)    out.println("@subgroup {chain"+chainID+"} dominant master= {chain"+chainID+"}");
                else                out.println("@group {"+idCode+" "+chainID+"} dominant");
                
                String bbColor = BACKBONE_COLORS[ chainNum % BACKBONE_COLORS.length ];
                if(cbProtein.isSelected())  printProtein(m, residues, bbColor);
                if(cbNucleic.isSelected())  printNucAcid(m, residues, bbColor);
                if(cbHets.isSelected())     printHets(m, residues);
                if(cbIons.isSelected())     printIons(m, residues);
                if(cbWater.isSelected())    printWaters(m, residues);
            }
        }
        
        this.out.flush();
        this.out = null;
        this.sp = null;
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
        
        if(cbPseudoBB.isSelected())
        {
            // Print C-alpha trace
        }
        if(cbBackbone.isSelected())
        {
            if(atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {protein bb} color= "+bbColor+" master= {protein} master= {backbone}");
                sp.printSticks(bonds, atomC.bbHeavy, atomC.bbHeavy, proteinRes, proteinRes);
                if(cbHydrogens.isSelected() && atomC.bbHydro.size() > 0)
                {
                    out.println("@vectorlist {protein bbH} color= gray master= {protein} master= {backbone} master= {Hs}");
                    sp.printSticks(bonds, atomC.bbHydro, atomC.bbHeavy, proteinRes, proteinRes);
                }
                if(cbBallsOnAtoms.isSelected())
                {
                    printAtomBalls(atomC.bbHeavy, proteinRes,
                        (cbBallsOnCarbon.isSelected() ? bbColor : null),
                        "master= {protein} master= {backbone}");
                }
            }
        }
        if(cbSidechains.isSelected())
        {
            if(atomC.scHeavy.size() > 0)
            {
                // includes disulfides, for now
                out.println("@vectorlist {protein sc} color= cyan master= {protein} master= {sidechains}");
                // to scHeavy if we want stubs to ribbon instead
                sp.printSticks(bonds, atomC.scHeavy, atomC.bioHeavy, proteinRes, proteinRes);
                if(cbHydrogens.isSelected() && atomC.scHydro.size() > 0)
                {
                    out.println("@vectorlist {protein scH} color= gray master= {protein} master= {sidechains} master= {Hs}");
                    // makes sure Gly 2HA connects to bb
                    sp.printSticks(bonds, atomC.scHydro, atomC.bioHeavy, proteinRes, proteinRes);
                }
                if(cbBallsOnAtoms.isSelected())
                {
                    printAtomBalls(atomC.scHeavy, proteinRes,
                        (cbBallsOnCarbon.isSelected() ? "cyan" : null),
                        "master= {protein} master= {sidechains}");
                }
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
        
        CheapSet nucAcidRes = new CheapSet(selectedRes);
        nucAcidRes.retainAll(resC.nucAcidRes);
        if(nucAcidRes.size() == 0) return;
        
        AtomClassifier  atomC   = data.getAtomClassifier();
        Collection      bonds   = data.getCovalentGraph().getBonds();
        
        if(cbPseudoBB.isSelected())
        {
            // Print pseudobackbone trace
        }
        if(cbBackbone.isSelected())
        {
            if(atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {nuc. acid bb} color= "+bbColor+" master= {nucleic acid} master= {backbone}");
                sp.printSticks(bonds, atomC.bbHeavy, atomC.bbHeavy, nucAcidRes, nucAcidRes);
                if(cbHydrogens.isSelected() && atomC.bbHydro.size() > 0)
                {
                    out.println("@vectorlist {nuc. acid bbH} color= gray master= {nucleic acid} master= {backbone} master= {Hs}");
                    sp.printSticks(bonds, atomC.bbHydro, atomC.bbHeavy, nucAcidRes, nucAcidRes);
                }
                if(cbBallsOnAtoms.isSelected())
                {
                    printAtomBalls(atomC.bbHeavy, nucAcidRes,
                        (cbBallsOnCarbon.isSelected() ? bbColor : null),
                        "master= {nucleic acid} master= {backbone}");
                }
            }
        }
        if(cbSidechains.isSelected())
        {
            if(atomC.scHeavy.size() > 0)
            {
                out.println("@vectorlist {nuc. acid sc} color= cyan master= {nucleic acid} master= {sidechains}");
                // to scHeavy if we want stubs to ribbon instead
                sp.printSticks(bonds, atomC.scHeavy, atomC.bioHeavy, nucAcidRes, nucAcidRes);
                if(cbHydrogens.isSelected() && atomC.scHydro.size() > 0)
                {
                    out.println("@vectorlist {nuc. acid scH} color= gray master= {nucleic acid} master= {sidechains} master= {Hs}");
                    sp.printSticks(bonds, atomC.scHydro, atomC.scHeavy, nucAcidRes, nucAcidRes);
                }
                if(cbBallsOnAtoms.isSelected())
                {
                    printAtomBalls(atomC.scHeavy, nucAcidRes,
                        (cbBallsOnCarbon.isSelected() ? "cyan" : null),
                        "master= {nucleic acid} master= {sidechains}");
                }
            }
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
        
        // First, the hets themselves.
        if(atomC.hetHeavy.size() == 0) return;
        out.println("@vectorlist {het} color= pink master= {hets}");
        sp.printSticks(bonds, atomC.hetHeavy, atomC.hetHeavy, hetRes, hetRes);

        if(cbBallsOnAtoms.isSelected())
        {
            printAtomBalls(atomC.hetHeavy, hetRes,
                (cbBallsOnCarbon.isSelected() ? "pink" : null),
                "master= {hets}");
        }
        
        if(cbHydrogens.isSelected() && atomC.hetHydro.size() > 0)
        {
            out.println("@vectorlist {hetH} color= gray master= {hets} master= {Hs}");
            sp.printSticks(bonds, atomC.hetHydro, atomC.hetHeavy, hetRes, hetRes);
        }
        // Now, the connections to protein.
        if(cbProtein.isSelected())
        {
            CheapSet proteinRes = new CheapSet(selectedRes);
            proteinRes.retainAll(resC.proteinRes);

            if(proteinRes.size() > 0 && cbBackbone.isSelected() && atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {het - protein bb} color= pinktint master= {hets} master= {protein} master= {backbone}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.bbHeavy, hetRes, proteinRes);
            }
            if(proteinRes.size() > 0 && cbSidechains.isSelected() && atomC.scHeavy.size() > 0)
            {
                out.println("@vectorlist {het - protein sc} color= pinktint master= {hets} master= {protein} master= {sidechains}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.scHeavy, hetRes, proteinRes);
            }
        }
        // Finally, the connections to nucleic acid.
        if(cbNucleic.isSelected())
        {
            CheapSet nucAcidRes = new CheapSet(selectedRes);
            nucAcidRes.retainAll(resC.nucAcidRes);

            if(nucAcidRes.size() > 0 && cbBackbone.isSelected() && atomC.bbHeavy.size() > 0)
            {
                out.println("@vectorlist {het - nuc. acid bb} color= pinktint master= {hets} master= {nucleic acid} master= {backbone}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.bbHeavy, hetRes, nucAcidRes);
            }
            if(nucAcidRes.size() > 0 && cbSidechains.isSelected() && atomC.scHeavy.size() > 0)
            {
                out.println("@vectorlist {het - nuc. acid sc} color= pinktint master= {hets} master= {nucleic acid} master= {sidechains}");
                sp.printSticks(bonds, atomC.hetHeavy, atomC.scHeavy, hetRes, nucAcidRes);
            }
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
        
        // 0.5 is the Prekin default metal radius
        out.println("@spherelist {ions} color= gray radius= 0.5 master= {ions}");
        bp.printBalls(atomC.ion, ionRes);
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
        if(atomC.watHeavy.size() == 0) return;
        
        out.println("@balllist {waters} color= peach radius= 0.2 master= {waters}");
        bp.printBalls(atomC.watHeavy, waterRes);

        if(cbHydrogens.isSelected() && atomC.watHydro.size() > 0)
        {
            Collection      bonds   = data.getCovalentGraph().getBonds();
            out.println("@vectorlist {waterH} color= gray master= {waters} master= {Hs}");
            sp.printSticks(bonds, atomC.watHydro, atomC.watHeavy, waterRes, waterRes);
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
            if(Util.isH(as) || Util.isQ(as))        continue;
            if(!residues.contains(as.getResidue())) continue;
            String element = Util.getElement(as);
            Collection atoms = (Collection) elementsToAtoms.get(element);
            if(atoms == null)
            {
                atoms = new ArrayList();
                elementsToAtoms.put(element, atoms);
            }
            atoms.add(as);
        }
        
        // Remove carbon if no color has been specified
        if(carbonColor == null) elementsToAtoms.remove("C");
        
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

