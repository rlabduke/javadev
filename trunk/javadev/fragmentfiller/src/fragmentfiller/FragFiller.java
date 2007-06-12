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
  HashMap<ArrayList<Double>, ArrayList<Triple>> gapFrameAtomsMap;
  HashMap<ArrayList<Double>, ArrayList<String>> filledMap; // gap (oneNum, nNum, frame) -> list of info that matches
  PdbLibraryReader libReader;
  static File fragLibrary = null;
  static File pdbLibrary = null;
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
  }
  
  //{{{ Constructors
  public FragFiller(File pdbFile, File outKinFile, String outPrefix) {
    setDefaults();
    filledMap = new HashMap<ArrayList<Double>, ArrayList<String>>();
    gapFrameAtomsMap = new HashMap<ArrayList<Double>, ArrayList<Triple>>();
    //System.out.println(pdbFile.toString());
    PdbFileAnalyzer analyzer = new PdbFileAnalyzer(pdbFile);
    Map<String, ArrayList> gapMap = analyzer.getGapAtoms();
    ArrayList<ArrayList<Double>> gapFrames = new ArrayList<ArrayList<Double>>();
    for (String name : gapMap.keySet()) {
      String[] nameParts = Strings.explode(name, ",".charAt(0)); // Model,Chain,seq#ofres1,seq#ofresN
      ArrayList gapAtomStates = gapMap.get(name);
      ArrayList<Double> frame = getGapFrame(gapAtomStates);
      frame.add(0, Double.valueOf(nameParts[2]));
      frame.add(1, Double.valueOf(nameParts[3]));
      gapFrames.add(frame);
      gapFrameAtomsMap.put(frame, gapAtomStates);
    }
    ArrayList<File> frameDataFiles = getFrameDataList();
    scanLoopData(frameDataFiles, gapFrames);
    for (ArrayList matchedInfo : filledMap.values()) {
      System.out.println("# of matches: " + matchedInfo.size());
    }
    readPdbLibrary();
    CoordinateFile[] pdbOut = getFragments();
    writePdbs(pdbOut, outPrefix);
    writeKin(pdbOut, outKinFile);
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
  
  //{{{ scanLoopData
  public void scanLoopData(ArrayList<File> datFiles, ArrayList<ArrayList<Double>> gapFrames) {
    for (ArrayList<Double> gapFrame : gapFrames) {
      filledMap.put(gapFrame, new ArrayList<String>());
    }
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
              for (ArrayList<Double> gapFrame : gapFrames) {
                if (scanLine(lineArray, gapFrame)) {
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
  public boolean scanLine(double[] line, ArrayList<Double> frame) {
    //String[] split = stringLine[0].split(" "); // pdbname length
    boolean inRange = true;
    //while (inRange) {
      //if ((frame.get(1) - frame.get(0)) != Double.parseDouble(split[1])) {
      //  inRange = false;
      //}
      if ((frame.get(1) - frame.get(0)) != line[1]) {
        inRange = false;
      }
      if ((line[2] >= frame.get(2) + 1)||(line[2] <= frame.get(2) - 1)) {
        inRange = false;
      }
      for (int i = 3; i < frame.size() && inRange; i++) {
        inRange = checkAngle(frame.get(i), line[i]);
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
    for (ArrayList<Double> gap : filledMap.keySet()) {
      fragPdbOut[i] = new CoordinateFile();
      fragPdbOut[i].setIdCode(gap.get(0).intValue() + "-" + gap.get(1).intValue());
      ArrayList<String> listofFiller = filledMap.get(gap);
      ArrayList<Triple> gapFrameStates = gapFrameAtomsMap.get(gap);
      System.out.println(listofFiller.size());
      for (int ind = 0; ((ind < 100000)&&(ind < listofFiller.size())); ind++) {
        String info = listofFiller.get(ind);
        String[] splitInfo = info.split(" ");
        String pdbName = splitInfo[0]; // should be pdbname
        int length = Integer.parseInt(splitInfo[1]);
        int startRes = Integer.parseInt(splitInfo[2]);
        libReader.setCurrentPdb(pdbName);
        Model frag = libReader.getFragment(Integer.toString(ind), startRes, length); //set of residues
        SuperPoser poser = new SuperPoser(getGapTupleArray(gapFrameStates), libReader.getFragmentEndpointAtoms(frag));
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
  public void writeKin(CoordinateFile[] pdbs, File kinout) {
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
  
  /*
  //{{{ initialize
  public void initialize() {
    caMap = new TreeMap<Integer, KPoint>();
    coMap = new TreeMap<Integer, KPoint>();
    gapMap = new TreeMap<Integer, Integer>();
    filledMap = new HashMap<ArrayList<Double>, ArrayList<String>>();
    progDiag = new ProgressDialog(kMain.getTopWindow(), "Filling gaps...", true);
    //Kinemage kin = kMain.getKinemage();
    //group = new KGroup("loops");
    //newSub = new KGroup("sub");
    //kin.add(group);
    //group.add(newSub);
    //makeFileChooser();
  }
  //}}}
  
  //{{{ onAnalyzeCurrent
  public void onAnalyzeCurrent(ActionEvent ev) {
    initialize();
    analyzeSequence();
    findGaps();
    ArrayList<ArrayList<Double>> gapFrames = getGapFrames();
    ArrayList<File> datFiles = getLoopDataList();
    ArrayList<File> loopKins = findLoopKins();
    if (datFiles != null) {
      long startTime = System.currentTimeMillis();
      scanLoopData(datFiles, gapFrames);
      long endTime = System.currentTimeMillis();
      System.out.println((endTime - startTime)/1000 + " seconds to scan dat files");
    }
    scanLoopKins(loopKins);

  }
  //}}}
  

  */
  /*
  //{{{ findLoopKins
  public ArrayList<File> findLoopKins() {
    filechooser.setDialogTitle("Pick source kins data directory");
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow())) {
	    //try {
        File f = filechooser.getSelectedFile();
        File[] kinFiles = f.listFiles();
        ArrayList<File> kinList = new ArrayList<File>();
        for (File kin : kinFiles) {
          if ((kin.getName().endsWith(".kin"))||(kin.getName().endsWith(".kin.gz"))) {
            kinList.add(kin);
          }
        }
        return kinList;
      //}
    }
    return null;
  }
  //}}}
  

  
  //{{{ openKinFile
  public KinfileParser openKinFile(File f) {
    //System.out.println("Opening " + f.getName());
    KinfileParser parser = new KinfileParser();
    try {
      FileInputStream fileIS = new FileInputStream(f);
      LineNumberReader    lnr;
      
      //parser = new KinfileParser();
      // Test for GZIPped files
      InputStream input = new BufferedInputStream(fileIS);
      input.mark(10);
      if(input.read() == 31 && input.read() == 139)
      {
        // We've found the gzip magic numbers...
        input.reset();
        input = new GZIPInputStream(input);
      }
      else input.reset();
      
      lnr = new LineNumberReader(new InputStreamReader(input));
      parser.parse(lnr);
      lnr.close();
      fileIS.close();
      //return parser;
    } catch (IOException ioe) {
      System.out.println(ioe);
      JOptionPane.showMessageDialog(kMain.getTopWindow(),
      "An error occurred while opening the file: " + f.getName(), "Sorry!", JOptionPane.ERROR_MESSAGE);
    } 
    return parser;
  }
  //}}}
  
  //{{{ getLoopFromKin
  public ArrayList<KList> getLoopFromKin(KinfileParser parser, TreeSet<Integer> keepSet) {
    //kMain.getKinIO().loadFile(f, null);
    ArrayList<Kinemage> kins = new ArrayList(parser.getKinemages());
    Kinemage kin = kins.get(0);
    KIterator<KPoint> points = KIterator.allPoints(kin);
    for (KPoint pt : points) {
      int resNum = KinUtil.getResNumber(pt);
      //String ptChain = KinUtil.getChainID(pt).toLowerCase();
      if ((!keepSet.contains(new Integer(resNum)))||(!(pt instanceof VectorPoint))){//||(!chainID.equals(ptChain))) {
          points.remove();
      } else {
        String name = pt.getName();
        pt.setName(name + keepSet.first().toString()); //temp fix to pdb export putting loops from same pdb together
      }
      if ((keepSet.contains(new Integer(resNum)))&&(!keepSet.contains(new Integer(resNum-1)))) {
        if (pt instanceof VectorPoint) {
          VectorPoint vpoint = (VectorPoint) pt;
          //String name = vpoint.getName();
          //System.out.println(name);
          //System.out.print(keepSet.first().toString());
          //vpoint.setName(name + keepSet.first().toString()); //temp fix to pdb export putting loops from same pdb together
          KPoint prev = vpoint.getPrev();
          if (prev instanceof KPoint) {
            if (!keepSet.contains(new Integer(KinUtil.getResNumber(prev)))) {
              vpoint.setPrev(null);
            }
          }
        }
      }
    }
    //Kinemage kin = kMain.getKinemage();
    removeExtraAtoms(kin, keepSet);
    KIterator<KList> lists = KIterator.allLists(kin.getChildren().get(0));
    try {
      ArrayList<KList> listofLists = new ArrayList<KList>();
      for (KList list : lists) {
        listofLists.add((KList) list.clone());
      }
        //KList clone = (KList) lists.next().clone();
      //kMain.getStable().closeCurrent();
      kMain.getTextWindow().setText("");
      return listofLists;
      //kMain.getStable().changeCurrentKinemage(1);
      //newSub.add(clone);
    } catch (CloneNotSupportedException e) {
    }
    //kMain.getStable().changeCurrentKinemage(2);
    //kMain.getStable().closeCurrent();
    return null;
  }
  //}}}
  
  //{{{ removeExtraAtoms
    public void removeExtraAtoms(Kinemage kin, TreeSet<Integer> keepSet) {
    KIterator<KPoint> points = KIterator.allPoints(kin);
    int first = keepSet.first().intValue();
    int last = keepSet.last().intValue();
    for (KPoint pt : points) {
      int resNum = KinUtil.getResNumber(pt);
      String atomName = KinUtil.getAtomName(pt);
      if ((atomName.equals("n"))&&(resNum == first)) points.remove();
      if ((atomName.equals("h"))&&(resNum == first)) points.remove();
      if ((atomName.equals("ca"))&&(resNum == first)) pt.setPrev(null);
      if ((atomName.equals("c"))&&(resNum == last)) points.remove();
      if ((atomName.equals("o"))&&(resNum == last)) points.remove();
    }
  }
  //}}}  

  //{{{ getListTupleArray
  public Tuple3[] getListTupleArray(KList list) {
    TreeMap<Integer, KPoint> listcaMap = new TreeMap<Integer, KPoint>();
    TreeMap<Integer, KPoint> listcoMap = new TreeMap<Integer, KPoint>();
    KIterator<KPoint> points = KIterator.allPoints(list);
    for (KPoint pt : points) {
      String atomName = KinUtil.getAtomName(pt);
      //System.out.println(atomName);
      if (atomName.equals("ca")) {
        Integer resNum = new Integer(KinUtil.getResNumber(pt));
        listcaMap.put(resNum, pt);
      }
      if (atomName.equals("o")) {
        Integer resNum = new Integer(KinUtil.getResNumber(pt));
        listcoMap.put(resNum, pt);
      }
    }
    int zeroNum = listcaMap.firstKey().intValue();
    int oneNum = zeroNum + 1;
    int n1Num = listcaMap.lastKey().intValue();
    int nNum = n1Num - 1;
    Tuple3[] tuples = new Tuple3[4];
    //tuples[0] = listcoMap.get(new Integer(zeroNum));
    tuples[0] = listcaMap.get(new Integer(zeroNum));
    tuples[1] = listcaMap.get(new Integer(oneNum));
    tuples[2] = listcaMap.get(new Integer(nNum));
    tuples[3] = listcaMap.get(new Integer(n1Num));
    //tuples[5] = listcoMap.get(new Integer(nNum));
    return tuples;
  }
  //}}}
  
  //{{{ getGapTupleArray
  public Tuple3[] getGapTupleArray(ArrayList<Double> gap) {
    int oneNum = gap.get(0).intValue();
    int nNum = gap.get(1).intValue();
    Tuple3[] tuples = new Tuple3[4];
    //tuples[0] = coMap.get(new Integer(oneNum - 1));
    tuples[0] = caMap.get(new Integer(oneNum - 1));
    tuples[1] = caMap.get(new Integer(oneNum));
    tuples[2] = caMap.get(new Integer(nNum));
    tuples[3] = caMap.get(new Integer(nNum + 1));
    //tuples[5] = coMap.get(new Integer(nNum));
    return tuples;
  }
  //}}}
  

  
  //{{{ onExport
  public void onExport(ActionEvent ev) {
    JFileChooser saveChooser = new JFileChooser();
    saveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    saveChooser.setDialogTitle("Pick pdb output directory");
    String currdir = System.getProperty("user.dir");
    if(currdir != null) {
	    saveChooser.setCurrentDirectory(new File(currdir));
    }
    if (saveChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow())) {
	    File f = saveChooser.getSelectedFile();
      savePdb(f);
    }
  }
  //}}}
  */
  //{{{ savePdb
  /**
  * First sorts the loop group by the PDB structures the loops came from, doing subgroups separately.
  * Then it saves a pdb file for each subgroup, putting the loops into different models.  
  **/
  /*
  public void savePdb(File f) {
    Iterator groups = kMain.getKinemage().iterator();
    while (groups.hasNext()) {
      KGroup tempGroup = (KGroup) groups.next();
      if (tempGroup.getName().equals("loops")) {
        group = tempGroup;
      }
    }
    Iterator subs = group.iterator();
    while (subs.hasNext()) {
      KGroup sub = (KGroup) subs.next();
      HashMap<String, KGroup> groupMap = new HashMap<String, KGroup>();
      KIterator<KList> lists = KIterator.allLists(sub);
      for (KList list : lists) {
        KPoint pt = list.getChildren().get(0);
        String pdbName = KinUtil.getPdbName(pt.getName());
        //System.out.println(pdbName);
        if (pdbName != null) {
          if (groupMap.containsKey(pdbName)) {
            KGroup pdbGroup = groupMap.get(pdbName);
            pdbGroup.add(list);
          } else {
            KGroup pdbGroup = new KGroup("");
            pdbGroup.add(list);
            groupMap.put(pdbName, pdbGroup);
          }
        }
      }
      File pdbout = new File(f, sub.getName() + ".pdb");
      if( !pdbout.exists() ||
        JOptionPane.showConfirmDialog(kMain.getTopWindow(),
      "The file " + pdbout.toString() + " exists -- do you want to overwrite it?",
      "Overwrite file?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION )
      {
        try {
          Writer w = new FileWriter(pdbout);
          PrintWriter out = new PrintWriter(new BufferedWriter(w));
          int i = 1;
          for (String pdbName : groupMap.keySet()) {
            //KIterator<KPoint> pts = KIterator.allPoints(groupMap.get(pdbName));
            //int counter = 0;
            //for (KPoint pt : pts) {
            //  counter++;
            //}
            //System.out.println(counter);
            out.println("MODEL     " + Kinimol.formatStrings(Integer.toString(i), 4));
            out.print(Kinimol.convertGrouptoPdb(groupMap.get(pdbName), pdbName));
            out.println("ENDMDL");
            i++;
          }
          out.flush();
          w.close();
        } catch (IOException ex) {
          System.out.println(ex);
          JOptionPane.showMessageDialog(kMain.getTopWindow(),
          "An error occurred while saving the file.", "Sorry!", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }
  */
  //}}}
  
}//class
