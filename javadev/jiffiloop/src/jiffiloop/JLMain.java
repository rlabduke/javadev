// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package jiffiloop;

//import king.*;
//import king.core.*;
//import king.points.*;
//import king.io.*;
import molikin.logic.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.*;
import driftwood.data.*;
//import king.tool.util.*;
//import king.tool.postkin.*;
import driftwood.util.SoftLog;
import driftwood.moldb2.*;
import driftwood.mysql.*;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.*;
import java.util.zip.*;
import java.sql.*;

//}}}

/**
* <code>JiffiLoop</code> is based off a plugin to make it easy to fill gaps in protein structures.  
* It combines functionality originally made for FramerTool, LoopTool, and the docking tools.
* It scans through a protein structure kin for gaps, analyzes the framing peptides of that gap,
* searches through my database of loops for matches, finds kins of those matches, and superimposes
* them in the kinemage.  
* Unfortunately it requires both my database (in zip format) of loop frame information, and a directory of
* kinemages, both of which are quite large, and would be difficult to distribute...
*
* <p>Copyright (C) 2007 by Vincent Chen. All rights reserved.
* 
*/
public class JLMain {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  //TreeMap<Integer, KPoint> caMap;
  //TreeMap<Integer, KPoint> coMap;
  //TreeMap<Integer, Integer> gapMap;
  //HashMap<ArrayList<Double>, ArrayList<Triple>> gapFrameAtomsMap;
  static HashMap<Integer, Integer> simulatedGaps;
  //HashMap<ProteinGap, ArrayList<String>> filledMap; // gap (oneNum, nNum, frame) -> list of strings (pdbname length startResNum)
  PdbLibraryReader libReader;
  static File fragLibrary = null;
  static File pdbLibrary = null;
  static int matchDiff;
  static boolean ntermsup = false;
  static boolean useStems = false;
  static boolean renumber = true;
  boolean keepSequence = false;
  int fragmentLimit = 100;
  //JFileChooser        filechooser     = null;
  //ProgressDialog progDiag;
  //KGroup group;
  //}}}
  
  //{{{ main
  //###############################################################
  public static void main(String[] args) {
    JLMain main = new JLMain();
    simulatedGaps = new HashMap<Integer, Integer>();
    try {
      ArrayList<String> argList = main.parseArgs(args);
      File pdbFile = new File(argList.get(0));
      File outKinFile = new File(argList.get(1)+".kin");
      File outPrefix = new File(argList.get(1));
	    //}
      //System.out.println(pdbFile);
	    main.runAnalysis(new File(pdbFile.getAbsolutePath()), new File(outKinFile.getAbsolutePath()), outPrefix.getAbsolutePath());
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      System.err.println();
      main.showHelp(true);
      System.exit(0);
    }
  }
  //}}}
  
  //{{{ showHelp, showChanges, getVersion
  //##############################################################################
  /**
  * Parse the command-line options for this program.
  * @param args the command-line options, as received by main()
  * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
  *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
  */
  // Display help information
  void showHelp(boolean showAll)
  {
    if(showAll)
    {
      InputStream is = getClass().getResourceAsStream("jiffiloop.help");
      if(is == null)
        System.err.println("\n*** Unable to locate help information in 'jiffiloop.help' ***\n");
      else
      {
        try { streamcopy(is, System.out); }
        catch(IOException ex) { ex.printStackTrace(); }
      }
    }
    System.err.println("jiffiloop.JLMain " + getVersion("version")+" "+getVersion("buildnum"));
    System.err.println("Copyright (C) 2007-2009 by Vincent B. Chen. All rights reserved.");
  }
  
  // Display changes information
  void showChanges(boolean showAll)
  {
    if(showAll)
    {
      InputStream is = getClass().getResourceAsStream("jiffiloop.changes");
      if(is == null)
        System.err.println("\n*** Unable to locate changes information in 'jiffiloop.changes' ***\n");
      else
      {
        try { streamcopy(is, System.out); }
        catch(IOException ex) { ex.printStackTrace(); }
      }
    }
    System.err.print("jiffiloop.JLMain " + getVersion("version")+" "+getVersion("buildnum"));
    System.err.println();
    System.err.println("Copyright (C) 2007-2009 by Vincent B. Chen. All rights reserved.");
  }
  
  // Copies src to dst until we hit EOF
  void streamcopy(InputStream src, OutputStream dst) throws IOException
  {
    byte[] buffer = new byte[2048];
    int len;
    while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
  }
  
  public String getVersion(String type) {
    String version = "";
    InputStream is = getClass().getResourceAsStream(type+".props");
    if(is == null) {
      System.err.println("\n*** Unable to locate changes information in '"+type+".props' ***\n");
    } else { 
      try {
        BufferedReader read = new BufferedReader(new InputStreamReader(is));
        String line = read.readLine();
        while (line != null) {
          if (!line.startsWith("#")) version = line;
          line = read.readLine();
        }
        read.close();
      } catch (IOException ex) { ex.printStackTrace(); }
    }
    return version;
  }
  //}}}
  
  //{{{ parseArgs
  public ArrayList parseArgs(String[] args) throws IllegalArgumentException {
    //Pattern p = Pattern.compile("^[0-9]*-[0-9]*$");
    ArrayList<String> argList = new ArrayList<String>();
    String arg;
    if (args.length < 2)
      throw new IllegalArgumentException("Not enough arguments: you must have an input pdb and an output prefix!");
    for (int i = 0; i < args.length; i++) {
      arg = args[i];
      // this is an option
      if(arg.startsWith("-")) {
        if(arg.equals("-h") || arg.equals("-help")) {
          showHelp(true);
          System.exit(0);
        } else if(arg.equals("-libloc") || arg.equals("-librarylocation")) {
          if (i+1 < args.length) {
            fragLibrary = new File(args[i+1]);
            if (!fragLibrary.canRead()) {
              throw new IllegalArgumentException("Invalid input for fragment library location");
            }
            i++;
          } else {
            throw new IllegalArgumentException("No argument given for fragment library location");
          }
        } else if(arg.equals("-pdbloc") || arg.equals("-pdblocation")) {
          if (i+1 < args.length) {
            pdbLibrary = new File(args[i+1]);
            if (!pdbLibrary.canRead()) {
              throw new IllegalArgumentException("Invalid input for pdb library location");
            }
            i++;
          } else {
            throw new IllegalArgumentException("No argument given for pdb library location");
          }
        } else if (arg.equals("-nomatchsize")) {
          if (i+1 < args.length) {
            if (isInteger(args[i+1])) {
              matchDiff = Integer.parseInt(args[i+1]);
              i++;
            }
          } else {
            throw new IllegalArgumentException("No integer given for -nomatchsize");
          }
        } else if (arg.equals("-fragments")) {
          if (i+1 < args.length) {
            if (isInteger(args[i+1])) {
              fragmentLimit = Integer.parseInt(args[i+1]);
              i++;
            }
          } else {
            throw new IllegalArgumentException("No integer given for -fragments");
          }
        } else if (arg.equals("-ntermsup")) {
          ntermsup = true;
        } else if (arg.equals("-stems")||arg.equals("-stem")) {
          useStems = true;
        } else if (arg.equals("-norenumber")) {
          renumber = false;
        } else if (arg.equals("-sequence")) {
          keepSequence = true;
        } else {
          throw new IllegalArgumentException("*** Unrecognized option: "+arg);
        }
      } else {
        if (arg.matches("[0-9]*-[0-9]*")) {
          String[] gapBounds = arg.split("-");
          int first = Integer.parseInt(gapBounds[0]);
          int sec = Integer.parseInt(gapBounds[1]);
          if (first > sec) {
            simulatedGaps.put(sec, first);
          } else if (sec > first) {
            simulatedGaps.put(first, sec);
          } else {
            throw new IllegalArgumentException("You are trying to simulate a gap of size zero!");
          }
        } else {
          System.out.println(arg);
          argList.add(arg);
        }
      }
    }
    return argList;
  }
  //}}}
  
  //{{{ setDefaults
  public void setDefaults() {
    String labworkPath = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").indexOf("labwork") + 7);
    //System.out.println(labworkPath);
    if (fragLibrary == null) {
      fragLibrary = new File(labworkPath + "/loopwork/fragfiller/");
    }
    //String labworkPath = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").indexOf("labwork") + 7);
    //System.out.println(labworkPath);
    if (pdbLibrary == null) {
      pdbLibrary = new File(labworkPath + "/loopwork/fragfiller/pdblibrary");
    }
    if (matchDiff < 0) {
      matchDiff = 0;
    }
  }
  //}}}

  //{{{ Constructors
  public JLMain() {
  }
  //}}}
  
  //{{{ runAnalysis
  public void runAnalysis(File pdbFile, File outKinFile, String outPrefix) {
    setDefaults();
    //filledMap = new HashMap<ProteinGap, ArrayList<String>>();
    //gapFrameAtomsMap = new HashMap<ArrayList<Double>, ArrayList<Triple>>();
    //System.out.println(pdbFile.toString());
    PdbFileAnalyzer analyzer = new PdbFileAnalyzer(pdbFile);
    analyzer.analyzePdb();
    for (Integer start : simulatedGaps.keySet()) {
      Integer end = simulatedGaps.get(start);
      analyzer.simulateGap(start.intValue(), end.intValue(), useStems);
    }
    libReader = new PdbLibraryReader(pdbLibrary, renumber, !keepSequence);
    CoordinateFile[] pdbOut;
    Map<String, ArrayList<ProteinGap>> gaps = analyzer.getGaps(); // stems need gaps!
    if (!useStems) {
      FragFiller fragFill = new FragFiller(gaps);
      ArrayList<ProteinGap> allGaps = new ArrayList<ProteinGap>();
      fragFill.searchDB(matchDiff);
      /* for searching loop data not using a database.  probably doesn't work with neo5200 params.
      scanLoopData(frameDataFiles, allGaps);
      for (ArrayList matchedInfo : filledMap.values()) {
        System.out.println("# of matches: " + matchedInfo.size());
      }
      */
      System.out.println(fragFill.getMatchesInfo());
      pdbOut = fragFill.getFragments(libReader, ntermsup, fragmentLimit);
    } else {
      Map<String, ArrayList<ProteinStem>> stems = analyzer.getStems();
      StemFiller stemFill = new StemFiller(stems);
      ArrayList<ProteinStem> allStems = new ArrayList<ProteinStem>();
      stemFill.searchDB(matchDiff);
      System.out.println(stemFill.getMatchesInfo());
      pdbOut = stemFill.getFragments(libReader, ntermsup, fragmentLimit);
    }
    writePdbs(pdbOut, outPrefix);
    writeKin(analyzer.getCoordFile(), pdbOut, outKinFile);
  }
  //}}}
  
  //{{{ getFrameDataList
  public ArrayList<File> getFrameDataList() {
    System.out.println(fragLibrary);
    File[] datFiles = fragLibrary.listFiles();
    ArrayList<File> dataList = new ArrayList<File>();
    for (File dat : datFiles) {
      if (dat.getName().endsWith(".zip")) {
        dataList.add(dat);
        System.out.println(dat);
      }
    }
    return dataList;
  //}
  }
        
  //}}}
  
  //{{{ readPdbLibrary
  //public void readPdbLibrary() {
  //
  //  libReader = new PdbLibraryReader(pdbLibrary);
  //
  //}
  //}}}
  
  //{{{ writePdbs
  public void writePdbs(CoordinateFile[] pdbs, String prefix) {
    for (CoordinateFile pdb : pdbs) {
      try {
        File outFile = new File(prefix + pdb.getIdCode() + ".pdb");
        PdbWriter writer = new PdbWriter(outFile);
        writer.writeCoordinateFile(pdb);
        writer.close();
      } catch (IOException ex) {
        System.err.println("An error occurred while saving a pdb." + ex);
      }
    }
  }
  //}}}
  
  //{{{ writeKin
  public void writeKin(CoordinateFile inputPdb, CoordinateFile[] pdbs, File kinout) {
    //File pdbout = new File(f, sub.getName() + ".pdb");
    try {
      Writer w = new FileWriter(kinout);
      PrintWriter out = new PrintWriter(new BufferedWriter(w));
      out.println("@kinemage");
      BallAndStickLogic bsl = new BallAndStickLogic();
      bsl.doProtein = true;
      bsl.doBackbone = true;
      bsl.doSidechains = true;
      bsl.doHydrogens = true;
      bsl.colorBy = BallAndStickLogic.COLOR_BY_MC_SC;
      Iterator iter = inputPdb.getModels().iterator();
      while (iter.hasNext()) {
        Model mod = (Model) iter.next();
        out.println("@group {"+inputPdb.getIdCode()+" "+mod.getName()+"} dominant master= {input pdb}");
        bsl.printKinemage(out, mod, new UberSet(mod.getResidues()), inputPdb.getIdCode(), "bluetint");
      }
      for (CoordinateFile pdb : pdbs) {
        Iterator models = pdb.getModels().iterator();
        while (models.hasNext()) {
          Model mod = (Model) models.next();
          out.println("@group {"+pdb.getIdCode()+" "+mod.getName()+"} dominant animate master= {all models}");
          bsl.printKinemage(out, mod, new UberSet(mod.getResidues()), inputPdb.getIdCode(), "white");
        }
      }
      out.flush();
      w.close();
    } catch (IOException ex) {
        System.err.println("An error occurred while saving the kin." + ex);
    }
  }
  //}}}
  
  //{{{ isInteger
  public static boolean isInteger(String s) {
    try {
	    Integer.parseInt(s);
	    return true;
    } catch (NumberFormatException e) {
	    return false;
    } catch (NullPointerException e) {
	    return false;
    }
  }
  //}}}
  
}//class
