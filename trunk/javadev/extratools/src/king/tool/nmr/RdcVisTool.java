// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.core.*;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.text.*;

import driftwood.gui.*;
import driftwood.r3.*;
import Jama.*;
import driftwood.moldb2.*;
import driftwood.util.*;
import chiropraxis.kingtools.*;
import king.tool.util.*;

//}}}

/**
* <code>RdcVisTool</code> is intended as a tool to visualize RDC data on proteins.
* 
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* <br>Begun Wed Nov 07 14:17:42 EST 2007
**/
public class RdcVisTool extends ModelingTool {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  JFileChooser filechooser;
  RdcDrawer drawer;
  RdcVisWindow rdcWin;
  TreeMap currentRdcs;
  KGroup group = null;
  KGroup subgroup = null;
  KGroup subError = null;
  //}}}
  
  //{{{ Constructors
  public RdcVisTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ start
  public void start() {
    //super.start();

    // Bring up model manager
    modelman.onShowDialog(null);
    MagneticResonanceFile mrf = openFile();
    //Set rdcTypes = mrf.getRdcTypeSet();
    //analyzeFile(mrf);
    rdcWin = new RdcVisWindow(kMain, mrf, modelman);
    // Helpful hint for users:
    this.services.setID("Ctrl-click, option-click, or middle-click a residue to see RDC curves");
  }
  //}}}
  
  //{{{ makeFileChooser
  //##################################################################################################
  void makeFileChooser()
  {
    
    // Make accessory for file chooser
    TablePane acc = new TablePane();
    
    // Make actual file chooser -- will throw an exception if we're running as an Applet
    filechooser = new JFileChooser();
    String currdir = System.getProperty("user.dir");
    if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
    
    filechooser.setAccessory(acc);
    
  }
  //}}}

  //{{{ openFile
  public MagneticResonanceFile openFile() {
    // Create file chooser on demand
    if(filechooser == null) makeFileChooser();
    
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
    {
	    try {
        File f = filechooser.getSelectedFile();
        NMRRestraintsReader nrr = new NMRRestraintsReader();
        nrr.scanFile(f);
        MagneticResonanceFile mrf = nrr.analyzeFileContents();
        return mrf;
        //analyzeFile(mrf);
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(kMain.getTopWindow(),
        "An I/O error occurred while loading the file:\n"+ex.getMessage(),
        "Sorry!", JOptionPane.ERROR_MESSAGE);
        //ex.printStackTrace(SoftLog.err);
      }
    }
    return null;
  }
  //}}}
  
  //{{{ analyzeFile
  //public void analyzeFile(MagneticResonanceFile mrf) {
  //  //Set rdcTypes = mrf.getRdcTypeSet();
  //  //Iterator iter = rdcTypes.iterator();
  //  //while (iter.hasNext()) {
  //  //  System.out.println(iter.next());
  //  //}
  //  currentRdcs = mrf.getRdcMapforType("N-HN");
  //  Iterator keys = currentRdcs.keySet().iterator();
  //  //while (keys.hasNext()) {
  //  //  System.out.print(keys.next() + " ");
  //  //}
  //  Model       model   = modelman.getModel();
  //  ModelState  state   = modelman.getFrozenState();
  //  RdcSolver solver = solveRdcs(model, state, currentRdcs);
  //  drawer = new RdcDrawer(solver.getSaupeDiagonalized());
  //  //Matrix saupeDiagonal = solver.getSaupeDiagonalized();
  //}
  //}}}
  
  //{{{ c_click
  //##############################################################################
  /** Override this function for middle-button/control clicks */
  public void c_click(int x, int y, KPoint p, MouseEvent ev)
  {
    if(p != null)
    {
      //if(modelman.isMolten())
      //{
      //  JOptionPane.showMessageDialog(kMain.getTopWindow(),
      //  "Please release all mobile groups and then try again.",
      //  "Cannot show curves",
      //  JOptionPane.ERROR_MESSAGE);
      //  return;
      //}
      //
      Model       model   = modelman.getModel();
      ModelState  state   = modelman.getFrozenState();
      //Residue     orig    = this.getResidueNearest(model, state,
      //                           p.getX(), p.getY(), p.getZ());
      Iterator iter = model.getResidues().iterator();
      while (iter.hasNext()) {
        Residue orig = (Residue) iter.next();
        Triple rdcVect = getResidueRdcVect(state, orig);
        AtomState origin = getOriginAtom(state, orig);
        if ((rdcVect != null)&&(origin != null)) {
          drawCurve(origin, rdcVect, orig);
        } else {
          //JOptionPane.showMessageDialog(kMain.getTopWindow(),
          //"Sorry, the atoms needed for this RDC do not seem to be in this residue.",
          //"Selected RDC atoms not found",
          //JOptionPane.ERROR_MESSAGE);
        }
      }
      //RdcSolver solver = solveRdcs(model, state);
      //if(orig != null)
      //  showRdcCurve(model, orig, state);
    }
  }
  //}}}
  
  //{{{ getResidueRdcVect
  /** returns RdcVect for orig residue based on what is selected in rdcWin **/
  public Triple getResidueRdcVect(ModelState state, Residue orig) {
    String atoms[] = rdcWin.parseAtomNames();
    Atom from = orig.getAtom(atoms[0]);
    Atom to = orig.getAtom(atoms[1]);
    try {
      AtomState fromState = state.get(from);
      AtomState toState = state.get(to);
      Triple rdcVect = new Triple().likeVector(fromState, toState).unit();
      return rdcVect;
    } catch (AtomException ae) {
    }
    return null;
  }
  //}}}
  
  //{{{ getOriginAtom
  public AtomState getOriginAtom(ModelState state, Residue orig) {
    String atoms[] = rdcWin.parseAtomNames();
    Atom origin;
    if (atoms[0].indexOf("H") > -1) {
      origin = orig.getAtom(atoms[1]);
    } else {
      origin = orig.getAtom(atoms[0]);
    }
    try {
      AtomState originState = state.get(origin);
      return originState;
    } catch (AtomException ae) {
    }
    return null;
  }
  //}}}
  
  //{{{ drawCurve
  public void drawCurve(Tuple3 p, Triple rdcVect, Residue orig) {
    Kinemage kin = kMain.getKinemage();
    //if(kin == null) return null;
    if (group == null) {
      group = new KGroup("RDCs");
      group.addMaster("Curves");
      group.setDominant(true);
      kin.add(group);
    }
    if (subgroup == null) {
      subgroup = new KGroup("sub");
      subgroup.setHasButton(true);
      group.add(subgroup);
    }
    if (subError == null) {
      subError = new KGroup("suberrorbars");
      subError.setHasButton(true);
      group.add(subError);
    }
    String seq = orig.getSequenceNumber().trim();
    double rdcVal = rdcWin.getRdcValue(seq);
    //System.out.println(rdcVal);
    //System.out.println((rdcVal != Double.NaN));
    double backcalcRdc = rdcWin.getBackcalcRdc(rdcVect);
    if ((!Double.isNaN(rdcVal))&&(!Double.isNaN(backcalcRdc))) {
      KList list = new KList(KList.VECTOR, "RDCs");
      KList errorBars = new KList(KList.VECTOR, "Error Bars");
      list.setWidth(4);
      errorBars.setWidth(2);
      subgroup.add(list);
      subError.add(errorBars);
      //System.out.println(seq);
      //String seq = String.valueOf(KinUtil.getResNumber(p));
      //DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seq);
      String text = "res= "+seq+" rdc= "+df.format(rdcVal)+" backcalc= "+df.format(backcalcRdc);
      System.out.println(text);
      //rdcWin.getDrawer().drawCurve(rdcVal, p, backcalcRdc, list);
      if (rdcWin.drawErrorsIsSelected()) {
        rdcWin.getDrawer().drawCurve(rdcVal - 2, p, 1, 60, backcalcRdc, errorBars, "-2 error bar");
        rdcWin.getDrawer().drawCurve(rdcVal + 2, p, 1, 60, backcalcRdc, errorBars, "+2 error bar");
      }
      rdcWin.getDrawer().drawCurve(rdcVal, p, 1, 60, backcalcRdc, list, text);
      //rdcWin.getDrawer().drawCurve(rdcVal-0.5, p, 1, 60, backcalcRdc, list);
      //rdcWin.getDrawer().drawCurve(rdcVal+0.5, p, 1, 60, backcalcRdc, list);
      //rdcWin.getDrawer().drawCurve(backcalcRdc, p, 1, 60, backcalcRdc, list);
      //rdcWin.getDrawer().drawAll(p, 1, 60, backcalcRdc, list);
    } else {
      System.out.println("this residue does not appear to have an rdc");
    }
  }
  //}}}
  

  
  //{{{ showRdcCurve
  //##############################################################################
  void showRdcCurve(Model model, Residue orig, ModelState origState)
  {
    //try
    //{
      //String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(),
      //    "Mutate this sidechain to what?",
      //    "Choose mutation",
      //    JOptionPane.QUESTION_MESSAGE, null,
      //    resChoices, orig.getName());
      //String choice = makeOptionPane(orig);
      //if((choice == null)||(choice.equals(JOptionPane.UNINITIALIZED_VALUE))) return; // user canceled operation
      
      // Create the mutated sidechain
      //ModelState newState = new ModelState(origState);
      //Residue newRes = idealizer.makeIdealResidue(orig.getChain(),
      //orig.getSegment(),
      //orig.getSequenceNumber(),
      //orig.getInsertionCode(),
      //choice, newState, usePdbv3);
      
      // Align it on the old backbone
      //newState = idealizer.dockResidue(newRes, newState, orig, origState);
      
      
      // Create the mutated model
      //Model newModel = (Model) model.clone();
      //newModel.replace(orig, newRes);
      //
      //// Remove any unnecessary AtomStates from the model
      //newState = newState.createForModel(newModel);
      //
      //// Insert the mutated model into the model manager
      //modelman.replaceModelAndState(newModel, newState);
      //
      //// Make a note in the headers
      //modelman.addUserMod("Mutated "+orig+" to "+newRes);
      //
      //// Set it up for rotation
      //try {
      //  new SidechainRotator(kMain, newRes, modelman);
      //      } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
            
    //}
    //catch(ResidueException ex)
    //{
    //  ex.printStackTrace(SoftLog.err);
    //}
    //catch(AtomException ex)
    //{
    //  ex.printStackTrace(SoftLog.err);
    //}
  }
  //}}}
  
  //{{{ getToolPanel, getHelpAnchor, toString
  //##################################################################################################
  /** Returns a component with controls and options for this tool */
  protected Container getToolPanel()
  { return null; }
  
  /** Returns the URL of a web page explaining use of this tool */
  public URL getHelpURL()
  {
    URL     url     = getClass().getResource("/extratools/tools-manual.html");
    String  anchor  = getHelpAnchor();
    if(url != null && anchor != null)
    {
      try { url = new URL(url, anchor); }
      catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
      return url;
    }
    else return null;
  }
  
  public String getHelpAnchor()
  { return "#rdcvis-tool"; }
  
  public String toString() { return "RDC Vis Tool"; }    
  //}}}
  
}
