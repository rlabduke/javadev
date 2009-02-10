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
import java.net.URL;
//}}}
/**
* <code>QuickinPlugin</code> is a tool for quickly generating kinemages of
* a particular CoordinateFile in KiNG.
*
* <p>Copyright (C) 2009 by Vincent B. Chen. All rights reserved.
* <br>Begun on Tue Feb 9 13:37:31 EST 2009
**/
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
    logic.colorBy           = BallAndStickLogic.COLOR_BY_MC_SC;
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
  
  public void onPseudo(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    BallAndStickLogic logic = new BallAndStickLogic();
    logic.doProtein         = true;
    logic.doNucleic         = true;
    logic.doHets            = true;
    logic.doIons            = false;
    logic.doWater           = false;
    logic.doPseudoBB        = true;
    logic.doBackbone        = false;
    logic.doSidechains      = false;
    logic.doHydrogens       = false;
    logic.doDisulfides      = true;
    logic.doBallsOnCarbon   = false;
    logic.doBallsOnAtoms    = false;
    logic.colorBy           = BallAndStickLogic.COLOR_BY_MC_SC;
    buildKinemage(null, coordFile, logic);
  }
  
  public void onResidue(ActionEvent ev) {
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
    logic.doHydrogens       = false;
    logic.doDisulfides      = true;
    logic.doBallsOnCarbon   = false;
    logic.doBallsOnAtoms    = false;
    logic.colorBy           = BallAndStickLogic.COLOR_BY_RES_TYPE;
    buildKinemage(null, coordFile, logic);
  }
  
  public void onRibbonLots(ActionEvent ev) {
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
    logic.colorBy           = BallAndStickLogic.COLOR_BY_MC_SC;
    buildKinemage(null, coordFile, logic);
    Kinemage current = kMain.getKinemage();
    RibbonLogic ribbLogic = new RibbonLogic();
    ribbLogic.secondaryStructure    = coordFile.getSecondaryStructure();
    ribbLogic.doProtein             = true;
    ribbLogic.doNucleic             = true;
    ribbLogic.doUntwistRibbons      = true;
    ribbLogic.doDnaStyle            = false;
    ribbLogic.colorBy               = RibbonLogic.COLOR_BY_RAINBOW;
    buildKinemage(current, coordFile, ribbLogic);
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
    return "To quick kin...";
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
    item = new JMenuItem(new ReflectiveAction("Pseudo-backbone", null, this, "onPseudo"));
    menu.add(item);
    item = new JMenuItem(new ReflectiveAction("Separate res", null, this, "onResidue"));
    menu.add(item);
    item = new JMenuItem(new ReflectiveAction("Lots+Ribbons", null, this, "onRibbonLots"));
    menu.add(item);
    return menu;
  }
  //}}}

  //{{{ getHelpURL, getHelpAnchor
  //##################################################################################################
  /** Returns the URL of a web page explaining use of this tool */
  public URL getHelpURL()
  {
    return null; // until we document this...
    
    /*URL     url     = getClass().getResource("/extratools/tools-manual.html");
    String  anchor  = getHelpAnchor();
    if(url != null && anchor != null)
    {
      try { url = new URL(url, anchor); }
      catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
      return url;
    }
    else return null;*/
  }
  
  /**
  * Returns an anchor marking a place within <code>king-manual.html</code>
  * that is the help for this plugin. This is called by the default
  * implementation of <code>getHelpURL()</code>. 
  * If you override that function, you can safely ignore this one.
  * @return for example, "#edmap-plugin" (or null)
  */
  public String getHelpAnchor()
  { return null; }
  //}}}
  
}
