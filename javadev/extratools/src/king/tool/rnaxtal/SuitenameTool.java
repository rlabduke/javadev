// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
import king.*;
import king.core.*;
import king.points.*;

import java.io.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.gui.*;
import driftwood.util.*;
import driftwood.moldb2.*;
//}}}
/**

Tue Dec 14 17:51:18 EST 2009 
*/
public class SuitenameTool extends BasicTool {
  
  //{{{ Constants
  //}}}

  //{{{ Variables
  JFileChooser filechooser;
  KList suiteList;
  //}}}

  //{{{ Constructor
  /**
  * Constructor
  */
  public SuitenameTool(ToolBox tb)
  {
    super(tb);
    
  }
  //}}}
  
  //{{{ makeFileChooser
  //##################################################################################################
  void makeFileChooser()
  {
    
    // Make accessory for file chooser
    //TablePane acc = new TablePane();
    
    // Make actual file chooser -- will throw an exception if we're running as an Applet
    filechooser = new JFileChooser();
    String currdir = System.getProperty("user.dir");
    if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
    
    //filechooser.setAccessory(acc);
    
  }
  //}}}
  
  //{{{ openFile
  public SuitenameReader openFile() {
    // Create file chooser on demand
    if(filechooser == null) makeFileChooser();
    
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
    {
	    try {
        File f = filechooser.getSelectedFile();
        SuitenameReader snr = new SuitenameReader();
        snr.readFile(f);
        return snr;
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
  
  //{{{ openPdbFile
  public CoordinateFile openPdbFile() {
    // Create file chooser on demand
    if(filechooser == null) makeFileChooser();
    
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
    {
	    try {
        File f = filechooser.getSelectedFile();
        PdbReader read = new PdbReader();
        CoordinateFile pdb = read.read(f);
        return pdb;
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
  
  //{{{ start
  public void start() {
    Kinemage kin = kMain.getKinemage();
    if (kin == null) return;
    setupKinStuff(kin);
    SuitenameReader snr = openFile();
    CoordinateFile pdb = openPdbFile();
    Iterator models = (pdb.getModels()).iterator();
    while (models.hasNext()) {
      Model mod = (Model) models.next();
      ModelState state = mod.getState();
      Iterator residues = mod.getResidues().iterator();
      while (residues.hasNext()) {
        Residue res = (Residue) residues.next();
        //System.out.println(res.getCNIT());
        String name = snr.getConformerName(res.getCNIT());
        if ((name != null) && name.equals("!!")) {
          //System.out.println("bang!="+res.getCNIT());
          Atom oxy = res.getAtom(" O3'");
          if (oxy == null) oxy = res.getAtom(" O3*");
          Atom car = res.getAtom(" C5'");
          if (car == null) car = res.getAtom(" C5*");
          Atom phos = res.getAtom(" P  ");
          if ((oxy != null) && (car != null) && (phos != null)) {
            try {
              AtomState oxyState = state.get(oxy);
              AtomState carState = state.get(car);
              AtomState phosState = state.get(phos);
              drawOutlier(phosState, carState, oxyState);
            } catch (AtomException ae) {
              ae.printStackTrace(SoftLog.err);
            }
          }
        }
      }
    }
  }
  //}}}
  
  //{{{ setupKinStuff
  public void setupKinStuff(Kinemage kin) {
    KGroup group = new KGroup("suite outliers");
    group.setDominant(true);
    KGroup sub = new KGroup("suite outliers sub");
    group.add(sub);
    suiteList = new KList(KList.VECTOR, "suite outliers");
    suiteList.setWidth(5);
    suiteList.setColor(KPalette.green);
    sub.add(suiteList);
    kin.add(group);
  }
  //}}}
  
  //{{{ drawOutlier
  public void drawOutlier(Tuple3 p1, Tuple3 p2, Tuple3 p3) {
    VectorPoint v1 = new VectorPoint("suite outlier", null);
    v1.setXYZ(p1.getX(), p1.getY(), p1.getZ());
    VectorPoint v2 = new VectorPoint("suite outlier", v1);
    v2.setXYZ(p2.getX(), p2.getY(), p2.getZ());
    VectorPoint v3 = new VectorPoint("suite outlier", v2);
    v3.setXYZ(p3.getX(), p3.getY(), p3.getZ());
    suiteList.add(v1);
    suiteList.add(v2);
    suiteList.add(v3);
  }
  //}}}
  
  //{{{ getToolPanel, getHelpAnchor, toString
  //##################################################################################################
  /** Returns a component with controls and options for this tool */
  protected Container getToolPanel()
  { return null; }
  
  ///** Returns the URL of a web page explaining use of this tool */
  //public URL getHelpURL()
  //{
  //  URL     url     = getClass().getResource("/extratools/tools-manual.html");
  //  String  anchor  = getHelpAnchor();
  //  if(url != null && anchor != null)
  //  {
  //    try { url = new URL(url, anchor); }
  //    catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
  //    return url;
  //  }
  //  else return null;
  //}
  //
  //public String getHelpAnchor()
  //{ return "#rdcvis-tool"; }
  
  public String toString() { return "Suitename Tool"; }    
  //}}}
}
