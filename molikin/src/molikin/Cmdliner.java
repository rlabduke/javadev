// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;
import molikin.logic.*;
import molikin.kingplugin.*;

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
* <code>Cmdliner</code> is for generating kins quickly on the cmdline.
*
* <p>Copyright (C) 2009 by Vincent B. Chen. All rights reserved.
* <br>Begun on Fri Feb 20 18:15:54 EST 2009
**/
public class Cmdliner {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  int                     kinNumber = 1;
  SuffixFileFilter        pdbFilter, cifFilter, allFilter;
  ArrayList<File>         filesToOpen;
  ArrayList<Logic>        logics;
  int                     numModels = -1;
  //}}}
  
  //{{{ Constructors
  public Cmdliner() {
    buildFileChooser();
  }
  //}}}
  
  //{{{ buildFileChooser
  //##################################################################################################
  /** Constructs the Open file chooser */
  private void buildFileChooser()
  {
    allFilter = CoordinateFile.getCoordFileFilter();
    pdbFilter = CoordinateFile.getPdbFileFilter();
    cifFilter = CoordinateFile.getCifFileFilter();
  }
  //}}}
  
  //{{{ onOpenFile, readPDB/CIF
  //##############################################################################
  // This method is the target of reflection -- DO NOT CHANGE ITS NAME
  public void onOpenFiles()
  {
    for (File f : filesToOpen) {
      try {
        CoordinateFile coords = null;
        if(cifFilter.accept(f))    coords = Quickin.readCIF(f);
        else                       coords = Quickin.readPDB(f);
        if (coords != null) {
          Logic[] logicList;
          if ((logics != null) && logics.size() > 0) {
            logicList = new Logic[logics.size()];
            int i = 0;
            for (Logic l : logics) {
              if (l instanceof RibbonLogic) 
                ((RibbonLogic)l).secondaryStructure    = coords.getSecondaryStructure();
              logicList[i] = l;
              i++;
            }
          } else {
            logicList = new Logic[2];
            logicList[0] = Quickin.getLotsLogic();
            logicList[1] = Quickin.getRibbonLogic();
            ((RibbonLogic)logicList[1]).secondaryStructure    = coords.getSecondaryStructure();
          }
          buildKinemage(coords, logicList);
        }
      }
      catch(IOException ex)
      {
        System.err.println("An I/O error occurred while loading the file:\n"+ex.getMessage());
        ex.printStackTrace(SoftLog.err);
      }
    }
  }
  //}}}
  
  //{{{ buildKinemage
  //##############################################################################
  void buildKinemage(CoordinateFile coordFile, Logic logic)
  {
    buildKinemage(coordFile, new Logic[] {logic});
  }
  
  void buildKinemage(CoordinateFile coordFile, Logic[] logiclist)
  {
    System.out.println("@kinemage "+(kinNumber++));
    System.out.println("@onewidth");
    PrintWriter out = new PrintWriter(System.out);
    if (numModels == -1)   Quickin.printKinemage(out, coordFile, logiclist);
    else                   Quickin.printKinemage(out, coordFile, logiclist, numModels);
    out.flush();
    out.close();
    coordFile = null;
  }
  //}}}
  
  public static void main(String[] args) { new Cmdliner().Main(args); }

  public void Main(String[] args) {
    parseArguments(args);
    onOpenFiles();
  }
  
  //{{{ parseArguments
  //##################################################################################################
  // Interpret command-line arguments
  void parseArguments(String[] args)
  {
    filesToOpen = new ArrayList<File>();
    logics = new ArrayList<Logic>();
    
    String arg;
    for(int i = 0; i < args.length; i++)
    {
      arg = args[i];
      // this is an option
      if(arg.startsWith("-"))
      {
        if(arg.equals("-h") || arg.equals("-help")) {
          SoftLog.err.println("Help not available. Sorry!");
          System.exit(0);
        } else if(arg.equals("-l") || arg.equals("-lots")) {
          logics.add(Quickin.getLotsLogic());
        } else if(arg.equals("-r") || arg.equals("-ribbons")) {
          logics.add(Quickin.getRibbonLogic());
        } else if(arg.equals("-single")) {
          numModels = 1;
        } else {
          System.err.println("*** Unrecognized option: "+arg);
        }
      }
      // this is a file, etc.
      else
      {
        filesToOpen.add(new File(arg));
      }
    }
  }
  //}}}

}
