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
* <br>Begun Wed Nov 07 14:17:42 EST 2007
**/
public class RdcVisTool extends ModelingTool {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  JFileChooser filechooser;
  RdcDrawer drawer;
  TreeMap currentRdcs;
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
    read();
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
  public void openFile() {
    // Create file chooser on demand
    if(filechooser == null) makeFileChooser();
    
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
    {
	    try {
        File f = filechooser.getSelectedFile();
        NMRRestraintsReader nrr = new NMRRestraintsReader();
        nrr.scanFile(f);
        MagneticResonanceFile mrf = nrr.analyzeFileContents();
        analyzeFile(mrf);
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(kMain.getTopWindow(),
        "An I/O error occurred while loading the file:\n"+ex.getMessage(),
        "Sorry!", JOptionPane.ERROR_MESSAGE);
        //ex.printStackTrace(SoftLog.err);
      }
    }
  }
  //}}}
  
  //{{{ read
  public void read() {
    openFile();
  }
  //}}}
  
  //{{{ analyzeFile
  public void analyzeFile(MagneticResonanceFile mrf) {
    Set rdcTypes = mrf.getRdcTypeSet();
    Iterator iter = rdcTypes.iterator();
    while (iter.hasNext()) {
      System.out.println(iter.next());
    }
    currentRdcs = mrf.getRdcMapforType("N-HN");
    Iterator keys = currentRdcs.keySet().iterator();
    //while (keys.hasNext()) {
    //  System.out.print(keys.next() + " ");
    //}
    Model       model   = modelman.getModel();
    ModelState  state   = modelman.getFrozenState();
    RdcSolver solver = solveRdcs(model, state, currentRdcs);
    drawer = new RdcDrawer(solver.getSaupeDiagonalized());
    //Matrix saupeDiagonal = solver.getSaupeDiagonalized();
  }
  //}}}
  
  //{{{ c_click
  //##############################################################################
  /** Override this function for middle-button/control clicks */
  public void c_click(int x, int y, KPoint p, MouseEvent ev)
  {
    if(p != null)
    {
      drawCurve(p);
      //if(modelman.isMolten())
      //{
      //  JOptionPane.showMessageDialog(kMain.getTopWindow(),
      //  "Please release all mobile groups and then try again.",
      //  "Cannot show curves",
      //  JOptionPane.ERROR_MESSAGE);
      //  return;
      //}
      //
      //Model       model   = modelman.getModel();
      //ModelState  state   = modelman.getFrozenState();
      //Residue     orig    = this.getResidueNearest(model, state,
      //                           p.getX(), p.getY(), p.getZ());
      //RdcSolver solver = solveRdcs(model, state);
      //if(orig != null)
      //  showRdcCurve(model, orig, state);
    }
  }
  //}}}
  
  //{{{ drawCurve
  public void drawCurve(KPoint p) {
    Kinemage kin = kMain.getKinemage();
    //if(kin == null) return null;
    KGroup group = new KGroup("group");
    group.setAnimate(true);
    group.addMaster("Curves");
    kin.add(group);
    KGroup subgroup = new KGroup("sub");
    subgroup.setHasButton(false);
    group.add(subgroup);
    KList list = new KList(KList.VECTOR, "Points");
    subgroup.add(list);
    String seq = String.valueOf(KinUtil.getResNumber(p));
    DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seq);
    double rdcVal = dr.getValues()[0];
    System.out.println("res num= "+seq+" rdc val= "+df.format(rdcVal));
    drawer.drawCurve(rdcVal, p, list);
  }
  //}}}
  
  //{{{ solveRdcs
  public RdcSolver solveRdcs(Model mod, ModelState state, TreeMap rdcs) {
    Iterator residues = mod.getResidues().iterator();
    ArrayList nhVects = new ArrayList();
    ArrayList rdcValues = new ArrayList();
    while (residues.hasNext()) {
      Residue res = (Residue) residues.next();
      String seq = res.getSequenceNumber().trim();
      //System.out.println(seq);
      if (rdcs.containsKey(seq)) {
        Atom from = res.getAtom(" N  ");
        Atom to = res.getAtom(" H  ");
        try {
          AtomState fromState = state.get(from);
          AtomState toState = state.get(to);
          DipolarRestraint dr = (DipolarRestraint) rdcs.get(seq);
          Triple vect = new Triple().likeVector(fromState, toState).unit();
          //System.out.println(vect);
          nhVects.add(vect);
          rdcValues.add(rdcs.get(seq));
          System.out.println(seq + " " + dr.getValues()[0]);
        } catch (AtomException ae) {
          System.out.println(ae + " thrown, atom is missing");
        }
      }
    }
    // make matrices
    if (nhVects.size() != rdcValues.size()) {
      System.out.println("nhVects and rdcValues not same size, must be same for SVD calc");
    } else {
      Matrix nhMatrix = new Matrix(3, nhVects.size());
      Matrix rdcMatrix = new Matrix(1, nhVects.size());
      for (int i = 0; i < nhVects.size(); i++) {
        Triple vect = (Triple) nhVects.get(i);
        DipolarRestraint dr = (DipolarRestraint) rdcValues.get(i);
        nhMatrix.set(0, i, vect.getX());
        nhMatrix.set(1, i, vect.getY());
        nhMatrix.set(2, i, vect.getZ());
        rdcMatrix.set(0, i, dr.getValues()[0]);
      }
      return new RdcSolver(nhMatrix, rdcMatrix);
    }
    return null;
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
