// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;
import molikin.logic.*;
import molikin.gui.*;

import driftwood.moldb2.*;
import driftwood.util.*;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
//}}}
/**
* <code>Quickin</code> is intended to be a central static function storage object
* for <code>QuickinPlugin</code> and <code>Cmdliner</code>.
*
* <p>Copyright (C) 2009 by Vincent B. Chen. All rights reserved.
* <br>Begun on Tue Feb 9 13:37:31 EST 2009
**/
public class Quickin {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  //}}}
  
  //{{{ Constructors
  public Quickin() {
  }
  //}}}
  
  //{{{ getLogics
  static public BallAndStickLogic getLotsLogic() {
    return getLotsLogic(false);
  }
  
  static public BallAndStickLogic getLotsLogic(boolean doVirtualBB) {
    BallAndStickLogic logic = new BallAndStickLogic();
    logic.doProtein         = true;
    logic.doNucleic         = true;
    logic.doHets            = true;
    logic.doMetals          = true;
    logic.doWater           = false;
    logic.doVirtualBB       = doVirtualBB;
    logic.doMainchain       = true;
    logic.doSidechains      = true;
    logic.doHydrogens       = true;
    logic.doDisulfides      = true;
    logic.doBallsOnCarbon   = false;
    logic.doBallsOnAtoms    = false;
    logic.colorBy           = BallAndStickLogic.COLOR_BY_MC_SC;
    return logic;
  }
  
  static public RibbonLogic getRibbonLogic() {
    RibbonLogic logic = new RibbonLogic();
    logic.doProtein             = true;
    logic.doNucleic             = true;
    logic.doUntwistRibbons      = true;
    logic.doDnaStyle            = false;
    logic.colorBy               = RibbonLogic.COLOR_BY_RAINBOW;
    return logic;
  }
  //}}}
  
  //{{{ readPDB/CIF
  //##############################################################################
  static public CoordinateFile readPDB(File f) throws IOException
  {
    PdbReader       pdbReader   = new PdbReader();
    CoordinateFile  coordFile   = pdbReader.read(f);
    return coordFile;
  }
  
  static public CoordinateFile readCIF(File f) throws IOException
  {
    CifReader       cifReader   = new CifReader();
    CoordinateFile  coordFile   = cifReader.read(f);
    return coordFile;
  }
  //}}}
  
  //{{{ printKinemage
  //##############################################################################
  /** Emits the kinemage (text) representation as selected by the user */
  static public void printKinemage(PrintWriter out, CoordinateFile coordFile, Logic[] logiclist) {
    printKinemage(out, coordFile, logiclist, coordFile.getModels().size());
  }
  
  static public void printKinemage(PrintWriter out, CoordinateFile coordFile, Logic[] logiclist, int numModels)
  {
    String idCode = "macromol";
    if(coordFile.getIdCode() != null)       idCode = coordFile.getIdCode();
    else if(coordFile.getFile() != null)    idCode = coordFile.getFile().getName();
    
    Collection models = coordFile.getModels();
    boolean groupByModel = (models.size() > 1)&&(numModels > 1);
    //Collection chains = this.getSelectedChains();
    
    int modelCount = 0;
    for(Iterator mi = models.iterator(); (mi.hasNext()&&modelCount<numModels); modelCount++)
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
}
