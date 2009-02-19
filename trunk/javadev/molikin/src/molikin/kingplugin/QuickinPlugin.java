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
public class QuickinPlugin extends king.Plugin {
  
  //{{{ Constants
  //}}}
  
  //{{{ CLASS: CoordFileOpen
  //##############################################################################
  private class CoordFileOpen implements FileDropHandler.Listener
  {
    String menuText;
    Logic logic;
    
    public CoordFileOpen(String text, Logic log) {
      menuText = text;
      logic = log;
    }
    
    public String toString()
    { return menuText; }
    
    public boolean canHandleDroppedFile(File file)
    {
      return pdbFilter.accept(file) || cifFilter.accept(file);
    }
    
    public void handleDroppedFile(File f)
    {
      try
      {
        CoordinateFile coordFile = null;
        if(pdbFilter.accept(f))        coordFile = readPDB(f);
        else if(cifFilter.accept(f))   coordFile = readCIF(f);
        if (logic instanceof RibbonLogic) {
          ((RibbonLogic)logic).secondaryStructure = coordFile.getSecondaryStructure();
        }
        buildKinemage(null, coordFile, logic);
      }
      catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }
  }
  //}}}
  
  //{{{ Variables
  int                     kinNumber = 1;
  SuffixFileFilter        pdbFilter, cifFilter, allFilter;
  JFileChooser            openChooser;
  //}}}
  
  //{{{ Constructors
  public QuickinPlugin(ToolBox tb) {
    super(tb);
    buildFileChooser();
    kMain.getFileDropHandler().addFileDropListener(new CoordFileOpen("Make lots kinemage", getLotsLogic()));
    BallAndStickLogic logic = getLotsLogic();
    logic.doPseudoBB        = true;
    logic.doBackbone        = false;
    logic.doSidechains      = false;
    logic.doHydrogens       = false;
    kMain.getFileDropHandler().addFileDropListener(new CoordFileOpen("Make pseudo-backbone kinemage", logic));
    kMain.getFileDropHandler().addFileDropListener(new CoordFileOpen("Make ribbons kinemage", getRibbonLogic()));
  }
  //}}}
  
  //{{{ buildFileChooser
  //##################################################################################################
  /** Constructs the Open file chooser */
  private void buildFileChooser()
  {
    allFilter = new SuffixFileFilter("PDB and mmCIF files");
    allFilter.addSuffix(".pdb");
    allFilter.addSuffix(".xyz");
    allFilter.addSuffix(".ent");
    allFilter.addSuffix(".cif");
    allFilter.addSuffix(".mmcif");
    pdbFilter = new SuffixFileFilter("Protein Data Bank (PDB) files");
    pdbFilter.addSuffix(".pdb");
    pdbFilter.addSuffix(".xyz");
    pdbFilter.addSuffix(".ent");
    cifFilter = new SuffixFileFilter("mmCIF files");
    cifFilter.addSuffix(".cif");
    cifFilter.addSuffix(".mmcif");
    
    String currdir = System.getProperty("user.dir");
    
    openChooser = new JFileChooser();
    openChooser.addChoosableFileFilter(allFilter);
    openChooser.addChoosableFileFilter(pdbFilter);
    openChooser.addChoosableFileFilter(cifFilter);
    openChooser.setFileFilter(allFilter);
    if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
  }
  //}}}
  
  //{{{ on___
  public void onLots(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      buildKinemage(null, coordFile, getLotsLogic());
      //logic.printKinemage(out, m, residues, pdbId, bbColor);
    }
  }

  
  public void onRibbons(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      RibbonLogic logic = getRibbonLogic();
      logic.secondaryStructure    = coordFile.getSecondaryStructure();
      buildKinemage(null, coordFile, logic);
    }
  }
  
  public void onPseudo(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      BallAndStickLogic logic = getLotsLogic();
      logic.doPseudoBB        = true;
      logic.doBackbone        = false;
      logic.doSidechains      = false;
      logic.doHydrogens       = false;
      buildKinemage(null, coordFile, logic);
    }
  }
  
  public void onResidue(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      BallAndStickLogic logic = getLotsLogic();
      logic.doHydrogens       = false;
      logic.colorBy           = BallAndStickLogic.COLOR_BY_RES_TYPE;
      buildKinemage(null, coordFile, logic);
    }
  }
  
  public void onRibbonLots(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      Logic[] logicList = new Logic[2];
      logicList[0] = getLotsLogic();
      logicList[1] = getRibbonLogic();
      ((RibbonLogic)logicList[1]).secondaryStructure    = coordFile.getSecondaryStructure();
      buildKinemage(null, coordFile, logicList);
    }
  }
  //}}}
  
  //{{{ getLogics
  public BallAndStickLogic getLotsLogic() {
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
    return logic;
  }
  
  public RibbonLogic getRibbonLogic() {
    RibbonLogic logic = new RibbonLogic();
    logic.doProtein             = true;
    logic.doNucleic             = true;
    logic.doUntwistRibbons      = true;
    logic.doDnaStyle            = false;
    logic.colorBy               = RibbonLogic.COLOR_BY_RAINBOW;
    return logic;
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
  
  //{{{ loadFileFromCmdline
  /** Plugins that can work on files from the king cmdline should overwrite this function */
  public void loadFileFromCmdline(ArrayList<File> args) {
    for (File f : args) {
      try {
        CoordinateFile coordFile = null;
        if(pdbFilter.accept(f))        coordFile = readPDB(f);
        else if(cifFilter.accept(f))   coordFile = readCIF(f);
        if (coordFile != null) {
          Logic[] logicList = new Logic[2];
          logicList[0] = getLotsLogic();
          logicList[1] = getRibbonLogic();
          ((RibbonLogic)logicList[1]).secondaryStructure    = coordFile.getSecondaryStructure();
          buildKinemage(null, coordFile, logicList);
        }
      } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }
  }
  //}}}
  
  //{{{ buildKinemage
  //##############################################################################
  void buildKinemage(Kinemage appendTo, CoordinateFile coordFile, Logic logic)
  {
    buildKinemage(appendTo, coordFile, new Logic[] {logic});
  }
  
  void buildKinemage(Kinemage appendTo, CoordinateFile coordFile, Logic[] logiclist)
  {
    StreamTank kinData = new StreamTank();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
    
    out.println("@kinemage "+(kinNumber++));
    out.println("@onewidth");
    printKinemage(out, coordFile, logiclist);
    
    out.flush();
    kinData.close();
    kMain.getKinIO().loadStream(kinData.getInputStream(), kinData.size(), appendTo);
  }
  //}}}
  
  //{{{ printKinemage
  //##############################################################################
  /** Emits the kinemage (text) representation as selected by the user */
  public void printKinemage(PrintWriter out, CoordinateFile coordFile, Logic[] logiclist)
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
          for (Logic logic : logiclist)
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
