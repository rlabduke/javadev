// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.kingplugin;
import molikin.logic.*;

import king.*;
import king.core.*;
import molikin.gui.*;
import driftwood.moldb2.*;
import driftwood.util.*;
import driftwood.gui.*;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
//}}}

public class QuickinPlugin extends MolikinPlugin {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  int                     kinNumber = 1;

  //}}}
  
  //{{{ Constructors
  public QuickinPlugin(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ on___
  public void onLots(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    BallAndStickLogic logic = new BallAndStickLogic();
    logic.doProtein         = true;
    logic.doNucleic         = true;
    logic.doHets            = true;
    logic.doIons            = false;
    logic.doWater           = false;
    logic.doPseudoBB        = false;
    logic.doBackbone        = true;
    logic.doSidechains      = true;
    logic.doHydrogens       = true;
    logic.doDisulfides      = true;
    logic.doBallsOnCarbon   = false;
    logic.doBallsOnAtoms    = false;
    logic.colorBy           = "backbone / sidechain";
    buildKinemage(null, coordFile, logic);
    //logic.printKinemage(out, m, residues, pdbId, bbColor);
  }
  
  public void onRibbons(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    RibbonLogic logic = new RibbonLogic();
    logic.secondaryStructure    = coordFile.getSecondaryStructure();
    logic.doProtein             = true;
    logic.doNucleic             = true;
    logic.doUntwistRibbons      = true;
    logic.doDnaStyle            = false;
    logic.colorBy               = RibbonLogic.COLOR_BY_RAINBOW;
    buildKinemage(null, coordFile, logic);
  }
  //}}}
  
  //{{{ onOpenFile, doPDB/CIF
  //##############################################################################
  // This method is the target of reflection -- DO NOT CHANGE ITS NAME
  public CoordinateFile onOpenFile()
  {
    // Open the new file
    if(JFileChooser.APPROVE_OPTION == openChooser.showOpenDialog(kMain.getTopWindow()))
    {
      try
      {
        File f = openChooser.getSelectedFile();
        if(f != null && f.exists())
        {
          //if(pdbFilter.accept(f))         doPDB(f);
          if(cifFilter.accept(f))    return readCIF(f);
          else                       return readPDB(f);
          //else throw new IOException("Can't identify file type");
        }
      }
      catch(IOException ex)
      {
        JOptionPane.showMessageDialog(kMain.getTopWindow(),
        "An I/O error occurred while loading the file:\n"+ex.getMessage(),
        "Sorry!", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace(SoftLog.err);
      }
    }
    return null;
  }
  
  public CoordinateFile readPDB(File f) throws IOException
  {
    PdbReader       pdbReader   = new PdbReader();
    CoordinateFile  coordFile   = pdbReader.read(f);
    return coordFile;
  }
  
  public CoordinateFile readCIF(File f) throws IOException
  {
    CifReader       cifReader   = new CifReader();
    CoordinateFile  coordFile   = cifReader.read(f);
    return coordFile;
  }
  //}}}
  
  //{{{ buildKinemage
  //##############################################################################
  void buildKinemage(Kinemage appendTo, CoordinateFile coordFile, Logic logic)
  {
    StreamTank kinData = new StreamTank();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
    
    out.println("@kinemage "+(kinNumber++));
    out.println("@onewidth");
    printKinemage(out, coordFile, logic);
    
    out.flush();
    kinData.close();
    kMain.getKinIO().loadStream(kinData.getInputStream(), kinData.size(), appendTo);
  }
  //}}}
  
  //{{{ printKinemage
  //##############################################################################
  /** Emits the kinemage (text) representation as selected by the user */
  public void printKinemage(PrintWriter out, CoordinateFile coordFile, Logic logic)
  {
    String idCode = "macromol";
    if(coordFile.getIdCode() != null)       idCode = coordFile.getIdCode();
    else if(coordFile.getFile() != null)    idCode = coordFile.getFile().getName();
    
    Collection models = coordFile.getModels();
    boolean groupByModel = (models.size() > 1);
    //Collection chains = this.getSelectedChains();
    
    int modelCount = 0;
    for(Iterator mi = models.iterator(); mi.hasNext(); modelCount++)
    {
      Model m = (Model) mi.next();
      if(groupByModel) out.println("@group {"+idCode+" "+m+"} dominant animate master= {all models}");
      
      Collection chains = m.getChainIDs();
      int chainCount = 0;
      for(Iterator ci = chains.iterator(); ci.hasNext(); chainCount++)
      {
        String chainID = (String) ci.next();
        if (m.getChain(chainID)!=null) {
          
          if(groupByModel)    out.println("@subgroup {chain"+chainID+"} dominant master= {chain"+chainID+"}");
          else                out.println("@group {"+idCode+" "+chainID+"} dominant");
          
          
          String bbColor = MainGuiPane.BACKBONE_COLORS[ (groupByModel ? modelCount : chainCount) % MainGuiPane.BACKBONE_COLORS.length];
          logic.printKinemage(out, m, m.getChain(chainID), idCode, bbColor);
        }
      }
    }
    
    out.flush();
  }
  //}}}
  
  //{{{ toString, getToolsMenuItem
  //##################################################################################################
  public String toString()
  {
    return "Quick kinemages";
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
    JMenu menu = new JMenu(this.toString());
    JMenuItem item = new JMenuItem(new ReflectiveAction("Lots (MC-SC-H)", null, this, "onLots"));
    menu.add(item);
    item = new JMenuItem(new ReflectiveAction("Ribbons", null, this, "onRibbons"));
    menu.add(item);
    return menu;
  }
  //}}}

}
