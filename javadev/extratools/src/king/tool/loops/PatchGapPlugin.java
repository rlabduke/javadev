// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;
import king.points.*;
import driftwood.gui.*;
import driftwood.r3.*;
import king.tool.util.*;
import king.tool.postkin.*;
import driftwood.util.SoftLog;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.*;
import java.util.zip.*;
//}}}

/**
* <code>PatchGapPlugin</code> is a plugin to make it easy to fill gaps in protein structures.  
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
public class PatchGapPlugin extends Plugin {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  TreeMap<Integer, KPoint> caMap;
  TreeMap<Integer, KPoint> coMap;
  TreeMap<Integer, Integer> gapMap;
  HashMap<ArrayList<Double>, ArrayList<String>> filledMap; // gap (oneNum, nNum, frame) -> list of info that matches
  JFileChooser        filechooser     = null;
  KGroup group;
  //}}}
  
  //{{{ Constructors
  public PatchGapPlugin(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ initialize
  public void initialize() {
    caMap = new TreeMap<Integer, KPoint>();
    coMap = new TreeMap<Integer, KPoint>();
    gapMap = new TreeMap<Integer, Integer>();
    filledMap = new HashMap<ArrayList<Double>, ArrayList<String>>();
    Kinemage kin = kMain.getKinemage();
    group = new KGroup("loops");
    //newSub = new KGroup("sub");
    kin.add(group);
    //group.add(newSub);
    makeFileChooser();
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
  
  //{{{ analyzeSequence
  public void analyzeSequence() {
    Kinemage kin = kMain.getKinemage();
    KIterator<KPoint> points = KIterator.allPoints(kin);
    for (KPoint pt : points) {
      String atomName = KinUtil.getAtomName(pt);
      //System.out.println(atomName);
      if (atomName.equals("ca")) {
        Integer resNum = new Integer(KinUtil.getResNumber(pt));
        caMap.put(resNum, pt);
      }
      if (atomName.equals("o")) {
        Integer resNum = new Integer(KinUtil.getResNumber(pt));
        coMap.put(resNum, pt);
      }
    }
    //System.out.println(coMap.size());
    //System.out.println(caMap.size());
  }
  //}}}
  
  //{{{ findGaps
  public void findGaps() {
    int oldresNum = 100000;
    for (Integer i : caMap.keySet()) {
      int resNum = i.intValue();
      if (resNum - 1 > oldresNum) {
        gapMap.put(new Integer(oldresNum), new Integer(resNum));
      }
      oldresNum = resNum;
    }
  }
  //}}}
  
  //{{{ getGapFrames
  public ArrayList<ArrayList<Double>> getGapFrames() {
    ArrayList<ArrayList<Double>> gapFrames = new ArrayList<ArrayList<Double>>();
    for (Integer oneNum : gapMap.keySet()) {
      Integer nNum = gapMap.get(oneNum);
      Integer zeroNum = new Integer(oneNum.intValue() - 1);
      Integer n1Num = new Integer(nNum.intValue() + 1);
      if ((caMap.containsKey(zeroNum))&&(caMap.containsKey(oneNum))&&
      (caMap.containsKey(nNum))&&(caMap.containsKey(n1Num))&&
      (coMap.containsKey(zeroNum))&&(coMap.containsKey(nNum))) {
        ArrayList<Double> frame = Framer.calphaAnalyzeList(caMap.get(zeroNum), caMap.get(oneNum), caMap.get(nNum), caMap.get(n1Num), coMap.get(zeroNum), coMap.get(nNum));
        frame.add(0, new Double(oneNum.doubleValue()));
        frame.add(1, new Double(nNum.doubleValue()));
        gapFrames.add(frame);
        //System.out.print(oneNum + " " + nNum + " ");
        for (double d : frame) {
          System.out.print(df.format(d) + " ");
        }
        System.out.println();
      }
    }
    return gapFrames;
  }
  //}}}
  
  //{{{ makeFileChooser
  //##################################################################################################
  void makeFileChooser()
  {
    
    // Make accessory for file chooser
    TablePane acc = new TablePane();
    
    // Make actual file chooser -- will throw an exception if we're running as an Applet
    filechooser = new JFileChooser();
    String currdir = System.getProperty("user.dir");
    if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
    
    filechooser.setAccessory(acc);
  }
  //}}}
  
  //{{{ getLoopDataList
  public ArrayList<File> getLoopDataList() {
    //if(filechooser == null) makeFileChooser();
    filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    filechooser.setDialogTitle("Pick loop frame data directory");
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow())) {
	    //try {
        File f = filechooser.getSelectedFile();
        File[] datFiles = f.listFiles();
        ArrayList<File> dataList = new ArrayList<File>();
        for (File dat : datFiles) {
          if (dat.getName().endsWith(".zip")) {
            dataList.add(dat);
          }
        }
        return dataList;
      //}
    }
    return null;
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
          JOptionPane.showMessageDialog(kMain.getTopWindow(),
          "An I/O error occurred while loading the file:\n"+ie.getMessage(),
          "Sorry!", JOptionPane.ERROR_MESSAGE);
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
        if ((line[i] >= frame.get(i) + 25)||(line[i] <= frame.get(i) - 25)) {
          inRange = false;
        }
      }
    //}
    return inRange;
  }
  //}}}
  
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
  
  //{{{ scanLoopKins
  public void scanLoopKins(ArrayList<File> loopKins) {
    //ArrayList<File> loopKins = findLoopKins();
    for (ArrayList<Double> gap : filledMap.keySet()) {
      KGroup newSub = new KGroup(gap.get(0).intValue() + "-" + gap.get(1).intValue());
      group.add(newSub);
      ArrayList<String> listofFiller = filledMap.get(gap);
      System.out.println(listofFiller.size());
      for (int ind = 0; ((ind < 100000)&&(ind < listofFiller.size())); ind++) {
        String info = listofFiller.get(ind);
        String[] splitInfo = info.split(" ");
        String pdbName = splitInfo[0]; // should be pdbname
        int length = Integer.parseInt(splitInfo[1]);
        int startRes = Integer.parseInt(splitInfo[2]);
        TreeSet<Integer> keepSet = new TreeSet<Integer>();
        for (int i = startRes; i <= startRes + length + 2; i++) {
          keepSet.add(new Integer(i));
        }
        //for (int i = 1; i <= 1000; i++) {
        //  keepSet.add(new Integer(i));
        //}
        //for (int i = startRes + length + 1; i <= 1000; i++) {
        //  keepSet.add(new Integer(i));
        //}
        for (File kinFile : loopKins) {
          if (kinFile.getName().indexOf(pdbName) != -1) {
            ArrayList<KList> loops = openKin(kinFile, keepSet);
            KList mc = loops.get(0);
            SuperPoser poser = new SuperPoser(getGapTupleArray(gap), getListTupleArray(mc));
            Transform t = poser.superpos();
            for (KList loop : loops) {
              loop.setHasButton(false);
              transform(loop, t);
              newSub.add(loop);
            }
          }
        }
      }
    }
  }
  //}}}
  
  //{{{ openKin
  public ArrayList<KList> openKin(File f, TreeSet<Integer> keepSet) {
    kMain.getKinIO().loadFile(f, null);
    KIterator<KPoint> points = KIterator.allPoints(kMain.getKinemage());
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
    Kinemage kin = kMain.getKinemage();
    removeExtraAtoms(kin, keepSet);
    KIterator<KList> lists = KIterator.allLists(kin.getChildren().get(0));
    try {
      ArrayList<KList> listofLists = new ArrayList<KList>();
      for (KList list : lists) {
        listofLists.add((KList) list.clone());
      }
        //KList clone = (KList) lists.next().clone();
      kMain.getStable().closeCurrent();
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
  
  //{{{ transform
  public void transform(KList list, Transform t) {
    KIterator<KPoint> points = KIterator.allPoints(list);
    for (KPoint pt : points) {
      Triple proxy = new Triple();
      proxy.setXYZ(pt.getX(), pt.getY(), pt.getZ());
      t.transform(proxy);
      pt.setX(proxy.getX());
      pt.setY(proxy.getY());
      pt.setZ(proxy.getZ());
    }
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
  
  //{{{ savePdb
  /**
  * First sorts the loop group by the PDB structures the loops came from, doing subgroups separately.
  * Then it saves a pdb file for each subgroup, putting the loops into different models.  
  **/
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
  //}}}
  
  //{{{ getToolsMenuItem
  public JMenuItem getToolsMenuItem() {
    JMenu menu = new JMenu("Fill Model Gaps");
    menu.add(new JMenuItem(new ReflectiveAction("Analyze Gaps", null, this, "onAnalyzeCurrent")));
    menu.add(new JMenuItem(new ReflectiveAction("Export Loops", null, this, "onExport")));
    return menu;
  }
  //}}}
  
  /** Returns the URL of a web page explaining use of this tool */
  public URL getHelpURL()
  {
    URL     url     = getClass().getResource("/extratools/tools-manual.html");
    String  anchor  = getHelpAnchor();
    if(url != null && anchor != null)
    {
      try { url = new URL(url, anchor); }
      catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
      return url;
    }
    else return null;
  }
  
  public String getHelpAnchor()
  { return "#fillgap-tool"; }
  
}//class
