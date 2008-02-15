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
* <code>StemFiller</code> is for filling gaps in pdb files using the stem-oriented
* fragment database.
*
* <p>Copyright (C) 2007 by Vincent Chen. All rights reserved.
* 
*/
public class StemFiller implements Filler {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  //TreeMap<Integer, KPoint> caMap;
  //TreeMap<Integer, KPoint> coMap;
  //TreeMap<Integer, Integer> gapMap;
  //HashMap<ArrayList<Double>, ArrayList<Triple>> gapFrameAtomsMap;
  //static HashMap<Integer, Integer> simulatedGaps;
  HashMap<ProteinStem, ArrayList<String>> filledMap; // gap (oneNum, nNum, frame) -> list of strings (pdbname length startResNum)
  //PdbLibraryReader libReader;
  static int matchDiff;
  //static boolean ntermsup = false;
  //JFileChooser        filechooser     = null;
  //ProgressDialog progDiag;
  //KGroup group;
  //}}}
    
  //{{{ Constructors
  public StemFiller(Map<String, ArrayList<ProteinStem>> stems) {
    filledMap = new HashMap<ProteinStem, ArrayList<String>>();
    //gapFrameAtomsMap = new HashMap<ArrayList<Double>, ArrayList<Triple>>();
    //System.out.println(pdbFile.toString());
    //PdbFileAnalyzer analyzer = new PdbFileAnalyzer(pdbFile);
    //for (Integer start : simulatedStems.keySet()) {
    //  Integer end = simulatedGaps.get(start);
    //  analyzer.simulateGap(start.intValue(), end.intValue());
    //}
    //Map<String, ArrayList<ProteinGap>> gaps = analyzer.getGaps();
    ArrayList<ProteinStem> allStems = new ArrayList<ProteinStem>();
    for (ArrayList list : stems.values()) {
      allStems.addAll(list);
    }
    for (ProteinStem stem : allStems) {
      filledMap.put(stem, new ArrayList<String>());
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
    //ArrayList<File> frameDataFiles = getFrameDataList();
    //searchFragmentDB(allGaps);
    //scanLoopData(frameDataFiles, allGaps);
    //for (ArrayList matchedInfo : filledMap.values()) {
    //  System.out.println("# of matches: " + matchedInfo.size());
    //}
    //for (ProteinGap gap : filledMap.keySet()) {
    //  ArrayList matchedInfo = filledMap.get(gap);
    //  System.out.println(gap.getSourceString() + " had " + matchedInfo.size() + " matches");
    //}
    //readPdbLibrary();
    //CoordinateFile[] pdbOut = getFragments();
    //writePdbs(pdbOut, outPrefix);
    //writeKin(analyzer.getCoordFile(), pdbOut, outKinFile);
  }
  //}}}
    
  //{{{ searchDB
  public void searchDB(int matchDiff) {
    DatabaseManager dm = new DatabaseManager();
    //dm.connectToDatabase("//spiral.research.duhs.duke.edu/qDBrDB");
    dm.connectToDatabase("//quality.biochem.duke.edu/vbc3");
    for (ProteinStem stem : filledMap.keySet()) {
      ArrayList<Double> stemParams = stem.getParameters();
      //int gapLength = gap.getSize();
      String sqlSelect = "SELECT pdb_id, chain_id, frag_length, start_res_num FROM parameters5200 ";
      if (matchDiff==0) {
        sqlSelect = sqlSelect.concat("WHERE frag_length = "+Integer.toString(4)+" \n");
      } else {
        //sqlSelect = sqlSelect.concat("WHERE (frag_length <= "+Integer.toString(gapLength+matchDiff));
        //sqlSelect = sqlSelect.concat(" AND frag_length >= "+Integer.toString(gapLength-matchDiff)+") \n");
        sqlSelect = sqlSelect.concat("WHERE frag_length = "+Integer.toString(matchDiff)+" \n");
      }
      double startAng = stemParams.get(0);
      double endAng = stemParams.get(1);
      double startDih = stemParams.get(2);
      if (stem.getStemType() == ProteinStem.N_TERM) {
        sqlSelect = sqlSelect.concat(createWhereQuery(startAng, "start_pair_angle", 5) + " \n");
        sqlSelect = sqlSelect.concat(createWhereQuery(endAng, "sp_n_dihedral", 5) + " \n");
        sqlSelect = sqlSelect.concat(createWhereQuery(startDih, "sp_c_dihedral", 5) + "\n");
      } else {
        sqlSelect = sqlSelect.concat(createWhereQuery(startAng, "end_pair_angle", 5) + " \n");
        sqlSelect = sqlSelect.concat(createWhereQuery(endAng, "ep_n_dihedral", 5) + " \n");
        sqlSelect = sqlSelect.concat(createWhereQuery(startDih, "ep_c_dihedral", 5) + " \n");
      }
      sqlSelect = sqlSelect.concat("AND max_B_factor <= 35;");
      System.out.println(sqlSelect);
      ArrayList<String> listofMatches = filledMap.get(stem);
      dm.select(sqlSelect);
      while (dm.next()) {
        //System.out.println(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" "+dm.getString(4));
        listofMatches.add(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" "+dm.getString(4));
      }
    }
  }
  //}}}
  
  //{{{ getMatchesInfo
  public String getMatchesInfo() {
    String info = "";
    for (ProteinStem stem : filledMap.keySet()) {
      ArrayList matchedInfo = filledMap.get(stem);
      info = info.concat(stem.getSourceString() + " had " + matchedInfo.size() + " matches\n");
    }
    return info;
  }
  //}}}
  
  //{{{ createWhereQuery
  public String createWhereQuery(double frameVal, String colName, int sigma) {
    if (frameVal > 180 - sigma) {
      return "AND ("+colName+" >= "+Double.toString(frameVal-sigma)+" OR "+colName+" <= "+Double.toString(-360+sigma+frameVal)+")";
    } else if (frameVal < -180 + sigma) {
      return "AND ("+colName+" <= "+Double.toString(frameVal+sigma)+" OR "+colName+" >= "+Double.toString(frameVal+360-sigma)+")";
    } else {
      return "AND ("+colName+" <= "+Double.toString(frameVal+sigma)+" AND "+colName+" >= "+Double.toString(frameVal-sigma)+")";
    }
  }
  //}}}
  
  //{{{ scanLoopData
  public void scanLoopData(ArrayList<File> datFiles, ArrayList<ProteinStem> stems) {
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
              for (ProteinStem stem : stems) {
                ArrayList<Double> stemFrame = stem.getParameters();
                int size = 5;
                if (scanLine(lineArray, size, stemFrame)) {
                  ArrayList<String> listofMatches = filledMap.get(stemFrame);
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
  
  //{{{ getFragments
  public CoordinateFile[] getFragments(PdbLibraryReader libReader, boolean ntermsup) {
    CoordinateFile[] fragPdbOut = new CoordinateFile[filledMap.keySet().size()];
    int i = 0;
    for (ProteinStem stem : filledMap.keySet()) {
      fragPdbOut[i] = new CoordinateFile();
      System.out.println(stem.getSourceString() + "." + stem.getOneNum());
      fragPdbOut[i].setIdCode(stem.getSourceString() + "." + stem.getOneNum());
      ArrayList<String> listofFiller = filledMap.get(stem);
      //ArrayList<Triple> stemFrameStates = stemFrameAtomsMap.get(stem);
      System.out.println(listofFiller.size());
      for (int ind = 0; ((ind < 20000)&&(ind < listofFiller.size())); ind++) {
        String info = listofFiller.get(ind);
        String[] splitInfo = info.split(" ");
        String pdbName = splitInfo[0]; // should be pdbname
        String chain = splitInfo[1];
        int length = Integer.parseInt(splitInfo[2]);
        int startRes = Integer.parseInt(splitInfo[3]);
        libReader.setCurrentPdb(pdbName, chain);
        Model frag = libReader.getFragment(Integer.toString(ind), chain, startRes, length); //set of residues
        if (frag != null) {
          //SuperPoser poser = null;
          try {
            Transform t = null;
            if (!ntermsup) {
              SuperPoser poser;
              if (stem.getStemType() == ProteinStem.N_TERM) {
                poser = new SuperPoser(stem.getTupleArray(), libReader.getStemNtermAtoms(frag));
              } else {
                poser = new SuperPoser(stem.getTupleArray(), libReader.getStemCtermAtoms(frag));
              }
              t = poser.superpos();
            } else {
              Builder builder = new Builder();
              if (stem.getStemType() == ProteinStem.N_TERM) {
                Tuple3[] stemArray = stem.getTupleArray();
                Tuple3[] fragArray = libReader.getStemNtermAtoms(frag);
                t = builder.dock3on3(
                stemArray[2], stemArray[1], stemArray[0],
                fragArray[2], fragArray[1], fragArray[0]);
              } else {
                Tuple3[] stemArray = stem.getTupleArray();
                Tuple3[] fragArray = libReader.getStemCtermAtoms(frag);
                t = builder.dock3on3(
                stemArray[0], stemArray[1], stemArray[2],
                fragArray[0], fragArray[1], fragArray[2]);
              }
            }
            //System.out.println(poser.calcRMSD(t));
            transform(frag, t);
            fragPdbOut[i].add(frag);
            if (Math.IEEEremainder((double) ind, 100.0) == 0) {
              System.out.println("Opened: " + ind);
            }
          } catch (AtomException ae) {
            System.err.println("Problem with atom " + ae.getMessage() + " in pdb " + pdbName);
          } catch (NoSuchElementException nsee) {
            System.err.println("Problem with residue "+chain+" "+startRes+" in pdb "+pdbName);
          }
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
  
  //{{{ getTupleArray
  public Tuple3[] getTupleArray(ArrayList<Triple> stemFrameStates) {
    //int oneNum = stem.get(0).intValue();
    //int nNum = stem.get(1).intValue();
    Tuple3[] tuples = new Tuple3[4];
    //tuples[0] = coMap.get(new Integer(oneNum - 1));
    tuples[0] = stemFrameStates.get(0);
    tuples[1] = stemFrameStates.get(1);
    tuples[2] = stemFrameStates.get(2);
    tuples[3] = stemFrameStates.get(3);
    //tuples[5] = coMap.get(new Integer(nNum));
    return tuples;
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
