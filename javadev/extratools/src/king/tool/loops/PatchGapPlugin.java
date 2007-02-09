// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;
import king.points.*;
import driftwood.gui.*;
import driftwood.r3.*;
import king.tool.util.*;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.*;
import java.util.zip.*;
//}}}

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
              for (ArrayList<Double> gapFrame : gapFrames) {
                if (scanLine(split, gapFrame)) {
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
  public boolean scanLine(String[] line, ArrayList<Double> frame) {
    String[] split = line[0].split(" "); // pdbname length
    boolean inRange = true;
    //while (inRange) {
      if ((frame.get(1) - frame.get(0)) != Double.parseDouble(split[1])) {
        inRange = false;
      }
      if ((Double.parseDouble(line[2]) >= frame.get(2) + 1)||(Double.parseDouble(line[2]) <= frame.get(2) - 1)) {
        inRange = false;
      }
      for (int i = 3; i < frame.size() && inRange; i++) {
        if ((Double.parseDouble(line[i]) >= frame.get(i) + 25)||(Double.parseDouble(line[i]) <= frame.get(i) - 25)) {
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
          if (kin.getName().endsWith(".kin")) {
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
      KGroup newSub = new KGroup(gap.get(0).intValue() + "->" + gap.get(1).intValue());
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
      if ((!keepSet.contains(new Integer(resNum)))){//||(!chainID.equals(ptChain))) {
          points.remove();
      } else if ((keepSet.contains(new Integer(resNum)))&&(!keepSet.contains(new Integer(resNum-1)))) {
        if (pt instanceof VectorPoint) {
          VectorPoint vpoint = (VectorPoint) pt;
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
    Tuple3[] tuples = new Tuple3[6];
    tuples[0] = listcoMap.get(new Integer(zeroNum));
    tuples[1] = listcaMap.get(new Integer(zeroNum));
    tuples[2] = listcaMap.get(new Integer(oneNum));
    tuples[3] = listcaMap.get(new Integer(nNum));
    tuples[4] = listcaMap.get(new Integer(n1Num));
    tuples[5] = listcoMap.get(new Integer(nNum));
    return tuples;
  }
  //}}}
  
  //{{{ getGapTupleArray
  public Tuple3[] getGapTupleArray(ArrayList<Double> gap) {
    int oneNum = gap.get(0).intValue();
    int nNum = gap.get(1).intValue();
    Tuple3[] tuples = new Tuple3[6];
    tuples[0] = coMap.get(new Integer(oneNum - 1));
    tuples[1] = caMap.get(new Integer(oneNum - 1));
    tuples[2] = caMap.get(new Integer(oneNum));
    tuples[3] = caMap.get(new Integer(nNum));
    tuples[4] = caMap.get(new Integer(nNum + 1));
    tuples[5] = coMap.get(new Integer(nNum));
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
  
  //{{{ getToolsMenuItem
  public JMenuItem getToolsMenuItem() {
    JMenuItem menu = new JMenuItem(new ReflectiveAction("Analyze Gaps", null, this, "onAnalyzeCurrent"));
    return menu;
  }
  //}}}
  
}//class
