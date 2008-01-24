// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fragmentfiller;

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
* <code>FragFiller</code> is based off a plugin to make it easy to fill gaps in protein structures.  
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
public class FMain {
  
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
  //JFileChooser        filechooser     = null;
  //ProgressDialog progDiag;
  //KGroup group;
  //}}}
  
  //{{{ main
  //###############################################################
  public static void main(String[] args) {
    simulatedGaps = new HashMap<Integer, Integer>();
    ArrayList<String> argList = parseArgs(args);
    if (argList.size() < 2) {
	    System.out.println("Not enough arguments: you must have an input pdb and an output prefix!");
    } else {
      //args[0]
	    //File[] inputs = new File[args.length];
	    //for (int i = 0; i < args.length; i++) {
      //File pdbFile = new File(System.getProperty("user.dir") + "/" + args[0]);
      //File outKinFile = new File(System.getProperty("user.dir") + "/" + args[1]);
      File pdbFile = new File(argList.get(0));
      File outKinFile = new File(argList.get(1)+".kin");
      File outPrefix = new File(argList.get(1));
	    //}
      //System.out.println(pdbFile);
	    FMain main = new FMain(new File(pdbFile.getAbsolutePath()), new File(outKinFile.getAbsolutePath()), outPrefix.getAbsolutePath());
    }
  }
  //}}}
  
  //{{{ parseArgs
  public static ArrayList parseArgs(String[] args) {
    //Pattern p = Pattern.compile("^[0-9]*-[0-9]*$");
    ArrayList<String> argList = new ArrayList<String>();
    String arg;
    for (int i = 0; i < args.length; i++) {
      arg = args[i];
      // this is an option
      if(arg.startsWith("-")) {
        if(arg.equals("-h") || arg.equals("-help")) {
          System.err.println("Help not available. Sorry!");
          System.exit(0);
        } else if(arg.equals("-libloc") || arg.equals("-librarylocation")) {
          if (i+1 < args.length) {
            fragLibrary = new File(args[i+1]);
            if (!fragLibrary.canRead()) {
              System.err.println("Invalid input for fragment library location");
              System.exit(0);
            }
            i++;
          } else {
            System.err.println("No argument given for fragment library location");
            System.exit(0);
          }
        } else if(arg.equals("-pdbloc") || arg.equals("-pdblocation")) {
          if (i+1 < args.length) {
            pdbLibrary = new File(args[i+1]);
            if (!pdbLibrary.canRead()) {
              System.err.println("Invalid input for pdb library location");
              System.exit(0);
            }
            i++;
          } else {
            System.err.println("No argument given for pdb library location");
            System.exit(0);
          }
        } else if (arg.equals("-nomatchsize")) {
          if (i+1 < args.length) {
            if (isInteger(args[i+1])) {
              matchDiff = Integer.parseInt(args[i+1]);
              i++;
            }
          } else {
            System.err.println("No integer given for -nomatchsize");
            System.exit(0);
          }
        } else if (arg.equals("-ntermsup")) {
          ntermsup = true;
        } else {
          System.err.println("*** Unrecognized option: "+arg);
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
            System.err.println("You are trying to simulate a gap of size zero!");
            System.exit(0);
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
  public FMain(File pdbFile, File outKinFile, String outPrefix) {
    setDefaults();
    //filledMap = new HashMap<ProteinGap, ArrayList<String>>();
    //gapFrameAtomsMap = new HashMap<ArrayList<Double>, ArrayList<Triple>>();
    //System.out.println(pdbFile.toString());
    PdbFileAnalyzer analyzer = new PdbFileAnalyzer(pdbFile);
    for (Integer start : simulatedGaps.keySet()) {
      Integer end = simulatedGaps.get(start);
      analyzer.simulateGap(start.intValue(), end.intValue());
    }
    Map<String, ArrayList<ProteinGap>> gaps = analyzer.getGaps();
    FragFiller fragFill = new FragFiller(gaps);
    ArrayList<ProteinGap> allGaps = new ArrayList<ProteinGap>();
    //for (ArrayList list : gaps.values()) {
    //  allGaps.addAll(list);
    //}
    //for (ProteinGap gap : allGaps) {
    //  filledMap.put(gap, new ArrayList<String>());
    //}
    //Map<String, ArrayList> gapMap = analyzer.getGapAtoms();
    //ArrayList<ArrayList<Double>> gapFrames = new ArrayList<ArrayList<Double>>();
    //for (String name : gapMap.keySet()) {
    //  String[] nameParts = Strings.explode(name, ",".charAt(0)); // Model,Chain,seq#ofres1,seq#ofresN
    //  ArrayList gapAtomStates = gapMap.get(name);
    //  ArrayList<Double> frame = getGapFrame(gapAtomStates);
    //  frame.add(0, Double.valueOf(nameParts[2]));
    //  frame.add(1, Double.valueOf(nameParts[3]));
    //  gapFrames.add(frame);
    //  gapFrameAtomsMap.put(frame, gapAtomStates);
    //  filledMap.put(frame, new ArrayList<String>());
    //}
    //ArrayList<File> frameDataFiles = getFrameDataList();
    fragFill.searchDB(matchDiff);
    //scanLoopData(frameDataFiles, allGaps);
    //for (ArrayList matchedInfo : filledMap.values()) {
    //  System.out.println("# of matches: " + matchedInfo.size());
    //}
    System.out.println(fragFill.getMatchesInfo());
    readPdbLibrary();
    CoordinateFile[] pdbOut = fragFill.getFragments(libReader);
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
  public void readPdbLibrary() {

    libReader = new PdbLibraryReader(pdbLibrary);

  }
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
        bsl.printKinemage(out, mod, new UberSet(mod.getResidues()), "bluetint");
      }
      for (CoordinateFile pdb : pdbs) {
        Iterator models = pdb.getModels().iterator();
        while (models.hasNext()) {
          Model mod = (Model) models.next();
          out.println("@group {"+pdb.getIdCode()+" "+mod.getName()+"} dominant animate master= {all models}");
          bsl.printKinemage(out, mod, new UberSet(mod.getResidues()), "white");
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
