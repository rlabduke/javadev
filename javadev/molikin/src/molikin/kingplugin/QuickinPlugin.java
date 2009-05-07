// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.kingplugin;
import molikin.logic.*;
import molikin.*;

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
import java.net.*;
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
        if(pdbFilter.accept(f))        coordFile = Quickin.readPDB(f);
        else if(cifFilter.accept(f))   coordFile = Quickin.readCIF(f);
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
    buildFilter();
    if (kMain.getApplet() == null) {
      buildFileChooser();
    }
    kMain.getFileDropHandler().addFileDropListener(new CoordFileOpen("Make lots kinemage", Quickin.getLotsLogic(true)));
    BallAndStickLogic logic = Quickin.getLotsLogic(true);
    logic.doBackbone        = false;
    logic.doSidechains      = false;
    logic.doHydrogens       = false;
    kMain.getFileDropHandler().addFileDropListener(new CoordFileOpen("Make pseudo-backbone kinemage", logic));
    kMain.getFileDropHandler().addFileDropListener(new CoordFileOpen("Make ribbons kinemage", Quickin.getRibbonLogic()));
  }
  //}}}
  
  //{{{ buildFilter
  public void buildFilter() {
    allFilter = new SuffixFileFilter("PDB and mmCIF files");
    allFilter.addSuffix(".pdb");
    allFilter.addSuffix(".xyz");
    allFilter.addSuffix(".ent");
    allFilter.addSuffix(".cif");
    allFilter.addSuffix(".mmcif");
    allFilter.addSuffix(".pdb.gz");
    allFilter.addSuffix(".xyz.gz");
    allFilter.addSuffix(".ent.gz");
    allFilter.addSuffix(".cif.gz");
    allFilter.addSuffix(".mmcif.gz");
    pdbFilter = new SuffixFileFilter("Protein Data Bank (PDB) files");
    pdbFilter.addSuffix(".pdb");
    pdbFilter.addSuffix(".xyz");
    pdbFilter.addSuffix(".ent");
    pdbFilter.addSuffix(".pdb.gz");
    pdbFilter.addSuffix(".xyz.gz");
    pdbFilter.addSuffix(".ent.gz");
    cifFilter = new SuffixFileFilter("mmCIF files");
    cifFilter.addSuffix(".cif");
    cifFilter.addSuffix(".mmcif");
    cifFilter.addSuffix(".cif.gz");
    cifFilter.addSuffix(".mmcif.gz");
  }
  //}}}
  
  //{{{ buildFileChooser
  //##################################################################################################
  /** Constructs the Open file chooser */
  private void buildFileChooser()
  {
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
      buildKinemage(null, coordFile, Quickin.getLotsLogic());
      //logic.printKinemage(out, m, residues, pdbId, bbColor);
    }
  }

  
  public void onRibbons(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      RibbonLogic logic = Quickin.getRibbonLogic();
      logic.secondaryStructure    = coordFile.getSecondaryStructure();
      buildKinemage(null, coordFile, logic);
    }
  }
  
  public void onPseudo(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      BallAndStickLogic logic = Quickin.getLotsLogic();
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
      BallAndStickLogic logic = Quickin.getLotsLogic();
      logic.doHydrogens       = false;
      logic.colorBy           = BallAndStickLogic.COLOR_BY_RES_TYPE;
      buildKinemage(null, coordFile, logic);
    }
  }
  
  public void onRibbonLots(ActionEvent ev) {
    CoordinateFile coordFile = onOpenFile();
    if (coordFile != null) {
      Logic[] logicList = new Logic[2];
      logicList[0] = Quickin.getLotsLogic();
      logicList[1] = Quickin.getRibbonLogic();
      ((RibbonLogic)logicList[1]).secondaryStructure    = coordFile.getSecondaryStructure();
      buildKinemage(null, coordFile, logicList);
    }
  }
  //}}}
  
  //{{{ onOpenFile, readPDB/CIF
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
          if(cifFilter.accept(f))    return Quickin.readCIF(f);
          else                       return Quickin.readPDB(f);
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
  
  public CoordinateFile readPDB(InputStream f) throws IOException
  {
    PdbReader       pdbReader   = new PdbReader();
    CoordinateFile  coordFile   = pdbReader.read(f);
    return coordFile;
  }
  
  public CoordinateFile readCIF(InputStream f) throws IOException
  {
    CifReader       cifReader   = new CifReader();
    CoordinateFile  coordFile   = cifReader.read(f);
    return coordFile;
  }
  //}}}
  
  //{{{ loadFileFromCmdline
  /** Plugins that can work on files from the king cmdline should overwrite this function */
  public void loadFileFromCmdline(ArrayList<File> files, ArrayList<String> args) {
    for (File f : files) {
      try {
        CoordinateFile coordFile = null;
        if(pdbFilter.accept(f))        coordFile = Quickin.readPDB(f);
        else if(cifFilter.accept(f))   coordFile = Quickin.readCIF(f);
        if (coordFile != null) {
          Logic[] logicList = new Logic[2];
          logicList[0] = Quickin.getLotsLogic();
          logicList[1] = Quickin.getRibbonLogic();
          ((RibbonLogic)logicList[1]).secondaryStructure    = coordFile.getSecondaryStructure();
          Kinemage kin = null;
          if (args.contains("-merge")||args.contains("-m")) {
            kin = kMain.getKinemage();
          }
          buildKinemage(kin, coordFile, logicList);
        }
      } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }
  }
  //}}}
  
  //{{{ loadFromURL
  public void loadFromURL(URL url) {
    String fName = null;
    try
    {
      fName = url.getFile();
      
      URLConnection uconn = url.openConnection();
      uconn.setAllowUserInteraction(false);
      uconn.connect();
      int urlSize = uconn.getContentLength();
      //System.out.println(urlSize);
      SuffixFileFilter gzipFilter = new SuffixFileFilter("GZipped files");
      gzipFilter.addSuffix(".gz");

      CoordinateFile coordFile = null;
      if(pdbFilter.accept(fName))        coordFile = readPDB(uconn.getInputStream());
      else if(cifFilter.accept(fName))   coordFile = readCIF(uconn.getInputStream());
      int                           maxSize = 5000000;
      if (gzipFilter.accept(fName)) maxSize = 1000000;
      if (coordFile != null) {
        if (urlSize < maxSize) {
          Logic[] logicList = new Logic[2];
          logicList[0] = Quickin.getLotsLogic();
          logicList[1] = Quickin.getRibbonLogic();
          ((RibbonLogic)logicList[1]).secondaryStructure    = coordFile.getSecondaryStructure();
          buildKinemage(null, coordFile, logicList);
        } else {
          JOptionPane.showMessageDialog(kMain.getTopWindow(),
            "Your input file may have been truncated for display in this applet due to limited memory.  If you see less of the structure\nthan you expect, try increasing the amount of memory available for Java applets or download the standalone version of KiNG.",
            "Warning", JOptionPane.WARNING_MESSAGE);
          RibbonLogic ribbon = Quickin.getRibbonLogic();
          ribbon.secondaryStructure = coordFile.getSecondaryStructure();
          buildKinemage(null, coordFile, new Logic[] {ribbon}, 5);
        }
      }
      //loadStream(uconn.getInputStream(), uconn.getContentLength());
      // Execution halts here until ioException()
      // or loadingComplete() closes the dialog.
    }
    catch(IOException ex)
    { ex.printStackTrace(SoftLog.err);
      JOptionPane.showMessageDialog(kMain.getTopWindow(),
        "The file '"+fName+"'\ncould not be opened due to an exception:\n"+ex.getMessage(),
         "Error", JOptionPane.ERROR_MESSAGE); }
  }
  
  /** Like loadFile, but it takes an InputStream. */
  public void loadStream(InputStream in, int dataLen)
  {
    //mergeTarget = kin;
    //new KinfileLoader(in, this);
    
    //progBar.setMaximum(dataLen);
    //progBar.setValue(0);
    //
    //progDialog.pack();
    //progDialog.setLocationRelativeTo(kMain.getTopWindow());
    //progDialog.setVisible(true);
    // Execution halts here until ioException()
    // or loadingComplete() closes the dialog.
  }
  //}}}
  
  //{{{ buildKinemage
  //##############################################################################
  void buildKinemage(Kinemage appendTo, CoordinateFile coordFile, Logic logic)
  {
    buildKinemage(appendTo, coordFile, new Logic[] {logic});
  }
  
  void buildKinemage(Kinemage appendTo, CoordinateFile coordFile, Logic[] logiclist) {
    buildKinemage(appendTo, coordFile, logiclist, coordFile.getModels().size());
  }
  
  void buildKinemage(Kinemage appendTo, CoordinateFile coordFile, Logic[] logiclist, int numModels)
  {
    StreamTank kinData = new StreamTank();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
    
    out.println("@kinemage "+(kinNumber++));
    out.println("@onewidth");
    Quickin.printKinemage(out, coordFile, logiclist, numModels);
    
    coordFile = null; // hopefully to save memory?
    out.flush();
    kinData.close();
    kMain.getKinIO().loadStream(kinData.getInputStream(), kinData.size(), appendTo);
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
