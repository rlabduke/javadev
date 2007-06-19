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
public class FragFiller {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  //TreeMap<Integer, KPoint> caMap;
  //TreeMap<Integer, KPoint> coMap;
  //TreeMap<Integer, Integer> gapMap;
  //HashMap<ArrayList<Double>, ArrayList<Triple>> gapFrameAtomsMap;
  HashMap<ProteinGap, ArrayList<String>> filledMap; // gap (oneNum, nNum, frame) -> list of strings (pdbname length startResNum)
  PdbLibraryReader libReader;
  static File fragLibrary = null;
  static File pdbLibrary = null;
  static int matchDiff;
  //JFileChooser        filechooser     = null;
  //ProgressDialog progDiag;
  //KGroup group;
  //}}}
  
  //{{{ main
  //###############################################################
  public static void main(String[] args) {
    ArrayList<String> argList = parseArgs(args);
    if (argList.size() < 3) {
	    System.out.println("Not enough arguments: you must have an input pdb, output kin, and an output prefix!");
    } else {
      //args[0]
	    //File[] inputs = new File[args.length];
	    //for (int i = 0; i < args.length; i++) {
      //File pdbFile = new File(System.getProperty("user.dir") + "/" + args[0]);
      //File outKinFile = new File(System.getProperty("user.dir") + "/" + args[1]);
      File pdbFile = new File(argList.get(0));
      File outKinFile = new File(argList.get(1));
      File outPrefix = new File(argList.get(2));
	    //}
      //System.out.println(pdbFile);
	    FragFiller filler = new FragFiller(new File(pdbFile.getAbsolutePath()), new File(outKinFile.getAbsolutePath()), outPrefix.getAbsolutePath());
    }
  }
  //}}}
  
  //{{{ parseArgs
  public static ArrayList parseArgs(String[] args) {
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
        } else if(arg.equals("-nomatchsize")) {
          if (i+1 < args.length) {
            if (isInteger(args[i+1])) {
              matchDiff = Integer.parseInt(args[i+1]);
              i++;
            }
          } else {
            System.err.println("No integer given for -nomatchsize");
            System.exit(0);
          }
        } else {
          System.err.println("*** Unrecognized option: "+arg);
        }
      } else {
        argList.add(arg);
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
  public FragFiller(File pdbFile, File outKinFile, String outPrefix) {
    setDefaults();
    filledMap = new HashMap<ProteinGap, ArrayList<String>>();
    //gapFrameAtomsMap = new HashMap<ArrayList<Double>, ArrayList<Triple>>();
    //System.out.println(pdbFile.toString());
    PdbFileAnalyzer analyzer = new PdbFileAnalyzer(pdbFile);
    Map<String, ArrayList<ProteinGap>> gaps = analyzer.getGaps();
    ArrayList<ProteinGap> allGaps = new ArrayList<ProteinGap>();
    for (ArrayList list : gaps.values()) {
      allGaps.addAll(list);
    }
    for (ProteinGap gap : allGaps) {
      filledMap.put(gap, new ArrayList<String>());
    }
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
    ArrayList<File> frameDataFiles = getFrameDataList();
    searchFragmentDB(allGaps);
    //scanLoopData(frameDataFiles, allGaps);
    for (ArrayList matchedInfo : filledMap.values()) {
      System.out.println("# of matches: " + matchedInfo.size());
    }
    readPdbLibrary();
    CoordinateFile[] pdbOut = getFragments();
    writePdbs(pdbOut, outPrefix);
    writeKin(analyzer.getCoordFile(), pdbOut, outKinFile);
  }
  //}}}
  
  //{{{ getGapFrames
  public ArrayList<Double> getGapFrame(ArrayList<Triple> gapAtomStates) {
    ArrayList<ArrayList<Double>> gapFrames = new ArrayList<ArrayList<Double>>();
    Triple ca0 = gapAtomStates.get(0);
    Triple ca1 = gapAtomStates.get(1);
    Triple caN = gapAtomStates.get(2);
    Triple caN1 = gapAtomStates.get(3);
    Triple co0 = gapAtomStates.get(4);
    Triple coN = gapAtomStates.get(5);
    ArrayList<Double> frame = Framer.calphaAnalyzeList(ca0, ca1, caN, caN1, co0, coN);
    //frame.add(0, new Double(oneNum.doubleValue()));
    //frame.add(1, new Double(nNum.doubleValue()));
    //System.out.print(oneNum + " " + nNum + " ");
    for (double d : frame) {
      System.out.print(df.format(d) + " ");
    }
    System.out.println();
    return frame;
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
  
  //{{{ searchFragmentDB
  public void searchFragmentDB(ArrayList<ProteinGap> gaps) {
    DatabaseManager dm = new DatabaseManager();
    dm.connectToDatabase("//spiral.research.duhs.duke.edu/qDBrDB");
    for (ProteinGap gap : gaps) {
      ArrayList<Double> gapFrame = gap.getParameters();
      int gapLength = gap.getSize();
      String sqlSelect = "SELECT pdb_id, frag_length, start_res_num FROM prot_frag_params ";
      if (matchDiff==0) {
        sqlSelect = sqlSelect.concat("WHERE frag_length = "+Integer.toString(gapLength)+" \n");
      } else {
        //sqlSelect = sqlSelect.concat("WHERE (frag_length <= "+Integer.toString(gapLength+matchDiff));
        //sqlSelect = sqlSelect.concat(" AND frag_length >= "+Integer.toString(gapLength-matchDiff)+") \n");
        sqlSelect = sqlSelect.concat("WHERE frag_length = "+Integer.toString(matchDiff)+" \n");
      }
      double dist = gapFrame.get(0);
      sqlSelect = sqlSelect.concat("AND (distance <= "+df.format(gapFrame.get(0)+1)+" AND distance >= "+df.format(gapFrame.get(0)-1));
      sqlSelect = sqlSelect.concat(") \n");
      double startAng = gapFrame.get(1);
      sqlSelect = sqlSelect.concat(createWhereQuery(startAng, "start_angle") + " \n");
      double endAng = gapFrame.get(2);
      sqlSelect = sqlSelect.concat(createWhereQuery(endAng, "end_angle") + " \n");
      double startDih = gapFrame.get(3);
      sqlSelect = sqlSelect.concat(createWhereQuery(startDih, "start_dihedral") + " \n");
      double middleDih = gapFrame.get(4);
      sqlSelect = sqlSelect.concat(createWhereQuery(middleDih, "middle_dihedral") + " \n");
      double endDih = gapFrame.get(5);
      sqlSelect = sqlSelect.concat(createWhereQuery(endDih, "end_dihedral") + ";");
      System.out.println(sqlSelect);
      ArrayList<String> listofMatches = filledMap.get(gap);
      dm.select(sqlSelect);
      while (dm.next()) {
        //System.out.println(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3));
        listofMatches.add(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3));
      }
    }
  }
  //}}}
  
  //{{{ createWhereQuery
  public String createWhereQuery(double frameVal, String colName) {
    if (frameVal > 180 - 25) {
      return "AND ("+colName+" >= "+Double.toString(frameVal-25)+" OR "+colName+" <= "+Double.toString(-360+25+frameVal)+")";
    } else if (frameVal < -180 + 25) {
      return "AND ("+colName+" <= "+Double.toString(frameVal+25)+" OR "+colName+"' >= "+Double.toString(frameVal+360-25)+")";
    } else {
      return "AND ("+colName+" <= "+Double.toString(frameVal+25)+" AND "+colName+" >= "+Double.toString(frameVal-25)+")";
    }
  }
  //}}}
  
  //{{{ scanLoopData
  public void scanLoopData(ArrayList<File> datFiles, ArrayList<ProteinGap> gaps) {
    //for (ArrayList<Double> gapFrame : gapFrames) {
    //  filledMap.put(gapFrame, new ArrayList<String>());
    //}
    for (File f : datFiles) {
      if(f != null && f.exists()) {
        try {
          System.out.println("Opening file: " + f.getName());
          ZipFile zip = new ZipFile(f);
          Enumeration entries= zip.entries();
          while (entries.hasMoreElements()) {
            ZipEntry zEntry = (ZipEntry) entries.nextElement();
            System.out.println("Scanning: " + zEntry.getName());
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(zip.getInputStream(zEntry)));
            //BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            while ((line = reader.readLine()) != null) {
              //System.out.println(line);
              String[] split = line.split(",");
              String[] firstSplit = split[0].split(" ");
              double lineArray[] = new double[8];
              lineArray[1] = Double.parseDouble(firstSplit[1]);
              for (int i = 2; i < 8; i++) {
                lineArray[i] = Double.parseDouble(split[i]);
              }
              for (ProteinGap gap : gaps) {
                ArrayList<Double> gapFrame = gap.getParameters();
                int size = gap.getSize();
                if (scanLine(lineArray, size, gapFrame)) {
                  ArrayList<String> listofMatches = filledMap.get(gapFrame);
                  listofMatches.add(split[0] + " " + split[1]); // should result in pdbname length startResNum
                }
              }
            }
          }
        } catch (IOException ie) {
          System.err.println("An I/O error occurred while loading the file:\n"+ie.getMessage());
        }
      }
    }
  }
  //}}}
  
  //{{{ scanLine
  public boolean scanLine(double[] line, int size, ArrayList<Double> frame) {
    //String[] split = stringLine[0].split(" "); // pdbname length
    boolean inRange = true;
    //while (inRange) {
      //if ((frame.get(1) - frame.get(0)) != Double.parseDouble(split[1])) {
      //  inRange = false;
      //}
      if (size != line[1]) {
        inRange = false;
      }
      if ((line[2] >= frame.get(0) + 1)||(line[2] <= frame.get(0) - 1)) {
        inRange = false;
      }
      for (int i = 3; i < frame.size() && inRange; i++) {
        inRange = checkAngle(frame.get(i - 2), line[i]);
        //if ((line[i] >= frame.get(i) + 25)||(line[i] <= frame.get(i) - 25)) {
        //  inRange = false;
        //}
      }
    //}
    return inRange;
  }
  //}}}
  
  //{{{ checkAngle 
  /** for checking if a value is within a range, taking into account angle wrapping **/
  public boolean checkAngle(double frameVal, double lineVal) {
    if (frameVal > 180 - 25) {
      //if ((lineVal >= frameVal - 25)||(lineVal <= -360 + 25 + frameVal)) {
      //  System.out.print("Frame: " + frameVal);
      //  System.out.println(" Line: " + lineVal);
      //}
      return ((lineVal >= frameVal - 25)||(lineVal <= -360 + 25 + frameVal));
    } else if (frameVal < -180 + 25) {
      return ((lineVal <= frameVal + 25)||(lineVal >= frameVal + 360 - 25));
    } else {
      return ((lineVal <= frameVal + 25)&&(lineVal >= frameVal - 25));
    }
  }
  //}}}
  
  //{{{ readPdbLibrary
  public void readPdbLibrary() {

    libReader = new PdbLibraryReader(pdbLibrary);
    /*
    File[] datFiles = f.listFiles();
    ArrayList<File> dataList = new ArrayList<File>();
    for (File dat : datFiles) {
      if (dat.getName().endsWith(".zip")) {
        dataList.add(dat);
        System.out.println(dat);
      }
    }
    return dataList;
    */
  }
  //}}}
  
  //{{{ getFragments
  public CoordinateFile[] getFragments() {
    CoordinateFile[] fragPdbOut = new CoordinateFile[filledMap.keySet().size()];
    int i = 0;
    for (ProteinGap gap : filledMap.keySet()) {
      fragPdbOut[i] = new CoordinateFile();
      fragPdbOut[i].setIdCode(gap.getOneNum() + "-" + gap.getNNum());
      ArrayList<String> listofFiller = filledMap.get(gap);
      //ArrayList<Triple> gapFrameStates = gapFrameAtomsMap.get(gap);
      System.out.println(listofFiller.size());
      for (int ind = 0; ((ind < 100000)&&(ind < listofFiller.size())); ind++) {
        String info = listofFiller.get(ind);
        String[] splitInfo = info.split(" ");
        String pdbName = splitInfo[0]; // should be pdbname
        int length = Integer.parseInt(splitInfo[1]);
        int startRes = Integer.parseInt(splitInfo[2]);
        libReader.setCurrentPdb(pdbName);
        Model frag = libReader.getFragment(Integer.toString(ind), startRes, length); //set of residues
        SuperPoser poser = new SuperPoser(gap.getTupleArray(), libReader.getFragmentEndpointAtoms(frag));
        Transform t = poser.superpos();
        //System.out.println(poser.calcRMSD(t));
        transform(frag, t);
        fragPdbOut[i].add(frag);
        if (Math.IEEEremainder((double) ind, 100.0) == 0) {
          System.out.println("Opened: " + ind);
        }
      }
      i++;
    }
    return fragPdbOut;
  }
  //}}}
  
  //{{{ transform
  public void transform(Model frag, Transform t) {
    //KIterator<KPoint> points = KIterator.allPoints(list);
    ModelState fragState = frag.getState();
    Iterator resIter = frag.getResidues().iterator();
    while (resIter.hasNext()) {
      Residue res = (Residue) resIter.next();
      Iterator atomIter = res.getAtoms().iterator();
      while (atomIter.hasNext()) {
        Atom at = (Atom) atomIter.next();
        try {
          AtomState atState = fragState.get(at);
          Triple proxy = new Triple();
          proxy.setXYZ(atState.getX(), atState.getY(), atState.getZ());
          t.transform(proxy);
          atState.setX(proxy.getX());
          atState.setY(proxy.getY());
          atState.setZ(proxy.getZ());
        } catch (AtomException ae) {
          System.err.println("Problem with atom " + ae.getMessage() + " in fragment filler");
        }
      }
    }
  }
  //}}}
  
  //{{{ getGapTupleArray
  public Tuple3[] getGapTupleArray(ArrayList<Triple> gapFrameStates) {
    //int oneNum = gap.get(0).intValue();
    //int nNum = gap.get(1).intValue();
    Tuple3[] tuples = new Tuple3[4];
    //tuples[0] = coMap.get(new Integer(oneNum - 1));
    tuples[0] = gapFrameStates.get(0);
    tuples[1] = gapFrameStates.get(1);
    tuples[2] = gapFrameStates.get(2);
    tuples[3] = gapFrameStates.get(3);
    //tuples[5] = coMap.get(new Integer(nNum));
    return tuples;
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
        out.println("@group {"+inputPdb.getIdCode()+" "+mod.getName()+"} dominant master= {all models} master= {input pdb}");
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
