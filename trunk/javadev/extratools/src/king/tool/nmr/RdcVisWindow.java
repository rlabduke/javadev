// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.core.*;

import driftwood.gui.*;
import driftwood.util.*;
import driftwood.r3.*;
import driftwood.moldb2.*;
import Jama.*;
import king.tool.util.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import chiropraxis.kingtools.*;
//}}}

/**
* <code>RdcVisWindow</code> makes the gui for RdcVisTool.
* 
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* <br>Begun Tue Nov 27 19:43:16 EST 2007
**/

public class RdcVisWindow implements ActionListener, WindowListener {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  KingMain            kMain;
  KinCanvas           kCanvas;
  ModelManager2       modelman;
  MagneticResonanceFile mrf;
  RdcDrawer2           drawer;
  TreeMap             currentRdcs;
  HashMap             transAtomMap;
  RdcSolver           solver;
  
  JDialog             dialog;
  
  //GUI
  TablePane2          pane;
  JComboBox           rdcBox;
  //}}}
  
  //{{{ Constructor
  public RdcVisWindow(KingMain kMain, MagneticResonanceFile mrfile, ModelManager2 mm) {
    this.kMain = kMain;
    this.kCanvas = kMain.getCanvas();
    this.modelman = mm;
    this.mrf = mrfile;
    transAtomMap = new HashMap();
    transAtomMap.put("HN", "H");
    //this.ctrRes = target;
    //this.anchorList = new KList(KList.BALL);
    //anchorList.setColor( KPalette.peach );
    buildGUI();
    
    // force loading of data tables that will be used later
    //try { rama = Ramachandran.getInstance(); }
    //catch(IOException ex) {}
    //try { tauscorer = TauByPhiPsi.getInstance(); }
    //catch(IOException ex) {}
    
    //Model model = modelman.getModel();
    //ModelState state = modelman.getMoltenState();
    //this.anchor1 = ctrRes.getPrev(model);
    //this.anchor2 = ctrRes.getNext(model);
    //if(anchor1 != null && anchor2 != null)
    //{
    //  markAnchor(anchor1, state);
    //  markAnchor(anchor2, state);
    //  //updateLabels(); -- done by stateChanged()
    //  
    //  // May also throw IAEx:
    //  Collection residues = CaRotation.makeMobileGroup(modelman.getModel(), anchor1, anchor2);
    //  modelman.registerTool(this, residues);
    //  stateChanged(null);
    //}
    //else
    //{
    //  dialog.dispose();
    //  throw new IllegalArgumentException("Can't find neighbor residues for "+target);
    //}
    
  }
  //}}}
  
  //{{{ buildGUI
  public void buildGUI() {
    Object[] rdcTypes = mrf.getRdcTypeSet().toArray();
    rdcBox = new JComboBox(rdcTypes);
    rdcBox.setSelectedIndex(0);
    rdcBox.addActionListener(this);
    
    pane = new TablePane2();
    pane.newRow();
    pane.add(rdcBox);
    dialog = new JDialog(kMain.getTopWindow(), "RDC Viewer", false);
    //dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(this);
    dialog.setContentPane(pane);
    //dialog.setJMenuBar(menubar);
    dialog.pack();
    dialog.setVisible(true);
  }
  //}}}
  
  // ev may be null!
  public void actionPerformed(ActionEvent ev)
  {
    System.out.println("rdcBox changed to :" + rdcBox.getSelectedItem());
    solveRdcs((String) rdcBox.getSelectedItem());
    //modelman.requestStateRefresh(); // will call this.updateModelState()
    //updateLabels();
    //kCanvas.repaint();
  }
  
  //{{{ analyzeFile
  public void solveRdcs(String rdcType) {

    currentRdcs = mrf.getRdcMapforType(rdcType);
    if (currentRdcs != null) {
      Iterator keys = currentRdcs.keySet().iterator();
      //while (keys.hasNext()) {
        //  System.out.print(keys.next() + " ");
      //}
      Model       model   = modelman.getModel();
      ModelState  state   = modelman.getFrozenState();
      solver = solveRdcs(model, state, currentRdcs, rdcType);
      if (solver != null) {
        drawer = new RdcDrawer2(solver.getSaupeDiagonalized(), solver.getSaupeEigenvectors());
      }
      //Matrix saupeDiagonal = solver.getSaupeDiagonalized();
    } else {
      JOptionPane.showMessageDialog(pane, "You must select an RDC type!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }
  //}}}
  
  //{{{ solveRdcs
  public RdcSolver solveRdcs(Model mod, ModelState state, TreeMap rdcs, String rdcType) {
    Iterator residues = mod.getResidues().iterator();
    ArrayList atomVects = new ArrayList();
    ArrayList rdcValues = new ArrayList();
    while (residues.hasNext()) {
      Residue res = (Residue) residues.next();
      String seq = res.getSequenceNumber().trim();
      //System.out.println(seq);
      if (rdcs.containsKey(seq)) {
        String[] atoms = parseAtomNames(rdcType);
        System.out.println(atoms[0]);
        System.out.println(atoms[1]);
        Atom from = res.getAtom(atoms[0]);
        Atom to = res.getAtom(atoms[1]);
        try {
          AtomState fromState = state.get(from);
          AtomState toState = state.get(to);
          DipolarRestraint dr = (DipolarRestraint) rdcs.get(seq);
          Triple vect = new Triple().likeVector(fromState, toState).unit();
          //System.out.println(vect);
          atomVects.add(vect);
          rdcValues.add(rdcs.get(seq));
          System.out.println(seq + " " + dr.getValues()[0]);
        } catch (AtomException ae) {
          System.out.println(ae + " thrown, atom is missing");
        }
      }
    }
    // make matrices
    if (atomVects.size() != rdcValues.size()) {
      System.out.println("atomVects and rdcValues not same size, must be same for SVD calc");
    } else if (atomVects.size() < 5) {
      System.out.println("not enough rdcs or vectors for SVD calc, must be at least 5");
    } else {
      Matrix atomMatrix = new Matrix(3, atomVects.size());
      Matrix rdcMatrix = new Matrix(1, atomVects.size());
      for (int i = 0; i < atomVects.size(); i++) {
        Triple vect = (Triple) atomVects.get(i);
        DipolarRestraint dr = (DipolarRestraint) rdcValues.get(i);
        atomMatrix.set(0, i, vect.getX());
        atomMatrix.set(1, i, vect.getY());
        atomMatrix.set(2, i, vect.getZ());
        rdcMatrix.set(0, i, dr.getValues()[0]);
      }
      return new RdcSolver(atomMatrix, rdcMatrix);
    }
    return null;
  }
  //}}}
  
  //{{{ parseAtomNames
  public String[] parseAtomNames() {
    return parseAtomNames((String) rdcBox.getSelectedItem());
  }
  
  public String[] parseAtomNames(String rdcType) {
    String[] atoms = Strings.explode(rdcType, '-');
    for (int i = 0; i < atoms.length; i++) {
      String atom = atoms[i];
      if (transAtomMap.containsKey(atom)) {
        atom = (String) transAtomMap.get(atom);
      }
      atoms[i] = pdbifyAtomName(atom);
    }
    return atoms;
  }
  //}}}
  
  //{{{ pdbifyAtomName
  public static String pdbifyAtomName(String atom) {
    if ((atom.length() == 1)||(atom.length() == 2)) {
      return Strings.justifyCenter(atom, 4);
    } else if (atom.length() == 3) {
      if (KinUtil.isNumeric(atom.substring(0,1))) {
        return Strings.justifyLeft(atom, 4);
      } else {
        return Strings.justifyRight(atom, 4);
      }
    } else {
      return Strings.justifyLeft(atom, 4);
    }
  }
  //}}}
  
  //{{{ get functions
  public RdcDrawer2 getDrawer() {
    return drawer;
  }
  
  public double getRdcValue(String seqNum) {
    System.out.println(seqNum);
    DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seqNum);
    System.out.println(dr);
    if (dr != null) {
      double rdcVal = dr.getValues()[0];
      return rdcVal;
    }
    return Double.NaN;
  }
  
  public double getBackcalcRdc(Triple vector) {
    return solver.backCalculateRdc(vector);
  }
  //}}}
  
  //{{{ Dialog window listeners
  //##################################################################################################
  public void windowActivated(WindowEvent ev)   {}
  public void windowClosed(WindowEvent ev)      {}
  public void windowClosing(WindowEvent ev)     {/* onReleaseResidues(null); */}
  public void windowDeactivated(WindowEvent ev) {}
  public void windowDeiconified(WindowEvent ev) {}
  public void windowIconified(WindowEvent ev)   {}
  public void windowOpened(WindowEvent ev)      {}
  //}}}
  
}
