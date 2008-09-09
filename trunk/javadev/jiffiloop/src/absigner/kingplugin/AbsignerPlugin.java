// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package absigner.kingplugin;

import javax.swing.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import absigner.*;
import king.*;
import king.core.*;
import driftwood.gui.*;
import driftwood.util.*;
import driftwood.moldb2.*;
import molikin.*;
//import driftwood.data.*;

//import driftwood.util.*;
//}}}

public class AbsignerPlugin extends king.Plugin {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  Absigner ab = null;
  //}}}

  //{{{ Constructor
  public AbsignerPlugin(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ onOpenFile, doPDB/CIF
  //##############################################################################
  // This method is the target of reflection -- DO NOT CHANGE ITS NAME
  public void onOpenFile(ActionEvent ev) {
    
    String currdir = System.getProperty("user.dir");
    JFileChooser openChooser = new JFileChooser();
    if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
    // Open the new file
    if(JFileChooser.APPROVE_OPTION == openChooser.showOpenDialog(kMain.getTopWindow())) {
      //try {
        File f = openChooser.getSelectedFile();
        if(f != null && f.exists()) {
          ab = new Absigner(f);
          buildKinemage(null);
          exploreParameters();
          //recolorKinemage(kMain.getKinemage());
        }
      //}
      //catch(IOException ex) {
      //  JOptionPane.showMessageDialog(kMain.getTopWindow(),
      //  "An I/O error occurred while loading the file:\n"+ex.getMessage(),
      //  "Sorry!", JOptionPane.ERROR_MESSAGE);
      //  ex.printStackTrace(SoftLog.err);
      //}
    }
  }
  //}}}

  //{{{ buildKinemage
  public void buildKinemage(Kinemage appendTo) {
    StreamTank kinData = new StreamTank();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
    
    //out.println("@kinemage 1");
    //out.println("@onewidth");
    ab.printKinemage(ab.getPdb(), out);
    
    //out.flush();
    kinData.close();
    kMain.getKinIO().loadStream(kinData.getInputStream(), kinData.size(), appendTo);
  }
  //}}}
  
  //{{{ recolorGroup
  public void recolorGroup(KGroup group, double[] multiplier) {
    PrekinIDer ider = new PrekinIDer();
    HashMap<String, Integer> pointMap = new HashMap<String, Integer>();
    int[] paramSizes = {2};
    ab.recalculate(paramSizes, multiplier);
    HashMap<Residue, Integer> alphaMap = ab.getAlphaMap();
    HashMap<Residue, Integer> betaMap = ab.getBetaMap();
    CoordinateFile pdb = ab.getPdb();
    Model first = pdb.getFirstModel();
    ModelState state = first.getState();
    Set<String> chains = first.getChainIDs();
    for (String cid : chains) {
      Set<Residue> residues = first.getChain(cid);
      for (Residue res : residues) {
        Iterator atoms = res.getAtoms().iterator();
        while (atoms.hasNext()) {
          try {
            Atom at = (Atom) atoms.next();
            AtomState atst = state.get(at);
            String pointID = ider.identifyAtom(atst);
            // I'm assuming in the following line that alpha and beta params are exclusive of each other.
            pointMap.put(pointID, new Integer(alphaMap.get(res).intValue() + betaMap.get(res).intValue()));
            if (betaMap.get(res).intValue() != 0) {
              //System.out.println(new Integer(betaMap.get(res).intValue()));
            }
          } catch (AtomException ae) {
            //shouldn't ever happen...i think
          }
        }
      }
    }
    KIterator<KPoint> points = KIterator.allPoints(group);
    for (KPoint pt : points) {
      if (pointMap.containsKey(pt.getName())) {
        int alphaValue = pointMap.get(pt.getName()).intValue();
        if (alphaValue == 0) {
          pt.setColor(KPalette.white);
        } else if (alphaValue == 1) {
          pt.setColor(KPalette.yellowtint);
        } else if (alphaValue == 2) {
          pt.setColor(KPalette.greentint);
        } else if (alphaValue >= 3) {
          pt.setColor(KPalette.green);
        } else if (alphaValue == -1) {
          pt.setColor(KPalette.pinktint);
        } else if (alphaValue == -2) {
          pt.setColor(KPalette.pink);
        } else if (alphaValue <= -3) {
          pt.setColor(KPalette.hotpink);
        }
      } else {
        System.err.println("Point Map does not have key for " + pt);
      }
    }
  }
  //}}}
  
  //{{{ exploreParameters
  public void exploreParameters() {
    Kinemage kin = kMain.getKinemage();
    KGroup group = (KGroup) kin.getChildren().get(0);
    for (int i = 1; i <= 30; i++) {
      group.setAnimate(true);
      KGroup gClone = group.clone(true);
      double[] mult = new double[] {i, i, i, i, i, i};
      recolorGroup(gClone, mult);
      kin.add(gClone);
    }
  }
  //}}}
  
  //{{{ toString, getToolsMenuItem
  //##################################################################################################
  public String toString()
  {
    return "ABsigner";
  }
  
  /**
  * Creates a new JMenuItem to be displayed in the Tools menu,
  * which will allow the user to access function(s) associated
  * with this Plugin.
  *
  * Only one JMenuItem may be returned, but it could be a JMenu
  * that contained several functionalities under it.
  *
  * The Plugin may return null to indicate that it has no
  * associated menu item.
  */
  public JMenuItem getToolsMenuItem()
  {
    JMenuItem item = new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onOpenFile"));
    //JMenuItem item = new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onTest"));
    return item;
  }
  //}}}
  
  //{{{ onTest
  public void onTest(ActionEvent ev) {
    checkAngleInRange(150, 160, 15, true);
    checkAngleInRange(150, 160, 10, true);
    checkAngleInRange(150, 150, 0, true);
    checkAngleInRange(170, -170, 20, true);
    checkAngleInRange(-170, 170, 20, true);
    checkAngleInRange(-180, 180, 0, true);
    checkAngleInRange(180, -180, 0, true);
    checkAngleInRange(0, 5, 10, true);
    checkAngleInRange(0, -5, 10, true);
    checkAngleInRange(150, 151, 0, false);
    checkAngleInRange(170, -170, 10, false);
    checkAngleInRange(-170, 170, 10, false);
    System.out.println("Test complete");
  }
  
  private void checkAngleInRange(double val, double param, double sd, boolean result) {
    if (Parameters.angleInRange(val, param, sd) != result) {
      System.out.println("ERROR: "+Double.toString(val)+" +/- "+Double.toString(sd)+" not "+result+" of "+Double.toString(param));
    }
  }
  //}}}
  
}

