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
public class FragFiller implements Filler {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  static final DecimalFormat df2 = new DecimalFormat("00");
  //}}}
  
  //{{{ Variables
  //TreeMap<Integer, KPoint> caMap;
  //TreeMap<Integer, KPoint> coMap;
  //TreeMap<Integer, Integer> gapMap;
  //HashMap<ArrayList<Double>, ArrayList<Triple>> gapFrameAtomsMap;
  //static HashMap<Integer, Integer> simulatedGaps;
  HashMap<ProteinGap, ArrayList<String>> filledMap; // gap (oneNum, nNum, frame) -> list of strings (pdbname length startResNum)
  //ArrayList
  //PdbLibraryReader libReader;
  //static int matchDiff;
  //static boolean ntermsup = false;
  //static boolean tighterParams = false;
  //JFileChooser        filechooser     = null;
  //ProgressDialog progDiag;
  //KGroup group;
  //}}}
    
  //{{{ Constructors
  public FragFiller(Map<String, ArrayList<ProteinGap>> gaps) {
    filledMap = new HashMap<ProteinGap, ArrayList<String>>();
    //gapFrameAtomsMap = new HashMap<ArrayList<Double>, ArrayList<Triple>>();
    //System.out.println(pdbFile.toString());
    //PdbFileAnalyzer analyzer = new PdbFileAnalyzer(pdbFile);
    //for (Integer start : simulatedGaps.keySet()) {
    //  Integer end = simulatedGaps.get(start);
    //  analyzer.simulateGap(start.intValue(), end.intValue());
    //}
    //Map<String, ArrayList<ProteinGap>> gaps = analyzer.getGaps();
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
  //public void searchDB(int matchDiff) {
  //  searchDB(matchDiff, 1, 25, 25, 25, 25, 25);
  //}
  
  public void searchDB(int matchDiff, double distRange, double nAngle, double cAngle, double nDihed, double dDihed, double cDihed) {
    DatabaseManager dm = new DatabaseManager();
    //dm.connectToDatabase("//spiral.research.duhs.duke.edu/qDBrDB");
    dm.connectToDatabase("//quality.biochem.duke.edu:1352/jiffiloop");
    for (ProteinGap gap : filledMap.keySet()) {
      ArrayList<Double> gapFrame = gap.getParameters();
      int gapLength = gap.getSize();
      String sqlSelect;
      if (gapLength > 15) {
        System.err.println("Gap of length "+gapLength+" detected, skipping...");
        filledMap.remove(gap);
      } else {
        if (matchDiff==0) {
          //sqlSelect = sqlSelect.concat("WHERE frag_length = "+Integer.toString(gapLength)+" \n");
          sqlSelect = "SELECT pdb_id, chain_id, frag_length, start_res_num FROM parameters5200_f"+df2.format(gapLength)+" \n";
        } else {
          //sqlSelect = sqlSelect.concat("WHERE (frag_length <= "+Integer.toString(gapLength+matchDiff));
          //sqlSelect = sqlSelect.concat(" AND frag_length >= "+Integer.toString(gapLength-matchDiff)+") \n");
          sqlSelect = "SELECT pdb_id, chain_id, frag_length, start_res_num FROM parameters5200_f"+df2.format(matchDiff)+" \n";
        }
        double dist = gapFrame.get(0);
        sqlSelect = sqlSelect.concat("WHERE (distance <= "+df.format(gapFrame.get(0)+distRange)+" AND distance >= "+df.format(gapFrame.get(0)-distRange));
        sqlSelect = sqlSelect.concat(") \n");
        double startAng = gapFrame.get(1);
        sqlSelect = sqlSelect.concat(createWhereAngle(startAng, "start_angle", nAngle) + " \n");
        double endAng = gapFrame.get(2);
        sqlSelect = sqlSelect.concat(createWhereAngle(endAng, "end_angle", cAngle) + " \n");
        double startDih = gapFrame.get(3);
        sqlSelect = sqlSelect.concat(createWhereDihedral(startDih, "start_dihedral", nDihed) + " \n");
        double middleDih = gapFrame.get(4);
        sqlSelect = sqlSelect.concat(createWhereDihedral(middleDih, "middle_dihedral", dDihed) + " \n");
        double endDih = gapFrame.get(5);
        sqlSelect = sqlSelect.concat(createWhereDihedral(endDih, "end_dihedral", cDihed) + " \n");
        sqlSelect = sqlSelect.concat("AND max_B_factor <= 35;");
        System.out.println(sqlSelect);
        ArrayList<String> listofMatches = filledMap.get(gap);
        dm.select(sqlSelect);
        while (dm.next()) {
          //System.out.println(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" "+dm.getString(4));
          listofMatches.add(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" "+dm.getString(4));
        }
      }
    }
  }
  //}}}
  
  //{{{ getMatchesInfo
  public String getMatchesInfo() {
    String info = "";
    for (ProteinGap gap : filledMap.keySet()) {
      ArrayList matchedInfo = filledMap.get(gap);
      info = info.concat(gap.getSourceString()+" "+gap.getResidueRange() + " had " + matchedInfo.size() + " matches\n");
    }
    return info;
  }
  //}}}
  
  //{{{ createWhereDihedral
  public String createWhereDihedral(double frameVal, String colName, double dihedral) {
    if (frameVal > 180 - dihedral) {
      return "AND ("+colName+" >= "+Double.toString(frameVal-dihedral)+" OR "+colName+" <= "+Double.toString(-360+dihedral+frameVal)+")";
    } else if (frameVal < -180 + dihedral) {
      return "AND ("+colName+" <= "+Double.toString(frameVal+dihedral)+" OR "+colName+" >= "+Double.toString(frameVal+360-dihedral)+")";
    } else {
      return "AND ("+colName+" <= "+Double.toString(frameVal+dihedral)+" AND "+colName+" >= "+Double.toString(frameVal-dihedral)+")";
    }
  }
  //}}}
  
  //{{{ createWhereAngle
  public String createWhereAngle(double frameVal, String colName, double angle) {
    /** since the 0-180 angles don't wrap, it should be ok to just use +- 25, even close to 0 or 180 */
    //if (frameVal > 180 - 25) {
    //  return "AND ("+colName+" >= "+Double.toString(frameVal-25)+" OR "+colName+" <= "+Double.toString(-180+25+frameVal)+")";
    //} else if (frameVal < 0 + 25) {
    //  return "AND ("+colName+" <= "+Double.toString(frameVal+25)+" OR "+colName+" >= "+Double.toString(frameVal+180-25)+")";
    //} else {
      return "AND ("+colName+" <= "+Double.toString(frameVal+angle)+" AND "+colName+" >= "+Double.toString(frameVal-angle)+")";
    //}
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
  
  //{{{ getFragments
  //public CoordinateFile[] getFragments(PdbLibraryReader libReader, boolean ntermsup) {
  //  return getFragments(libReader, ntermsup, 100);
  //}
  
  public CoordinateFile[] getFragments(PdbLibraryReader libReader, boolean ntermsup, int limit) {
    CoordinateFile[] fragPdbOut = new CoordinateFile[filledMap.keySet().size()];
    int i = 0;
    for (ProteinGap gap : filledMap.keySet()) {
      fragPdbOut[i] = new CoordinateFile();
      //System.out.println(gap.getSourceString() + "." + gap.getOneNum() + "-" + gap.getNNum());
      fragPdbOut[i].setIdCode(gap.getSourceString() + "." + gap.getOneNum() + "-" + gap.getNNum());
      ArrayList<String> listofFiller = filledMap.get(gap);
      //ArrayList<Triple> gapFrameStates = gapFrameAtomsMap.get(gap);
      //System.out.println(listofFiller.size());
      for (int ind = 0; ((ind < limit)&&(ind < listofFiller.size())); ind++) {
        String info = listofFiller.get(ind);
        String[] splitInfo = info.split(" ");
        String pdbName = splitInfo[0]; // should be pdbname
        String chain = splitInfo[1];
        int length = Integer.parseInt(splitInfo[2]);
        int startRes = Integer.parseInt(splitInfo[3]);
        //System.out.println(pdbName);
        //fragPdbOut[i].setIdCode(pdbName+chain+splitInfo[3]+"-"+Integer.toString(startRes+length));
        libReader.setCurrentPdb(pdbName, chain);
        Model frag = libReader.getFragment(Integer.toString(ind), chain, startRes, length, gap.getOneNum()-1); //set of residues
        if (frag != null) {
          //SuperPoser poser = null;
          try {
            Transform t = null;
            if (!ntermsup) {
              SuperPoser poser = new SuperPoser(gap.getTupleArray(), libReader.getFragmentEndpointAtoms(frag));
              t = poser.superpos();
            } else {
              Builder builder = new Builder();
              Tuple3[] gapArray = gap.getNtermTuples();
              Tuple3[] fragArray = libReader.getFragmentNtermAtoms(frag);
              t = builder.dock3on3(
              gapArray[2], gapArray[0], gapArray[1],
              fragArray[2], fragArray[0], fragArray[1]);
            }
            //System.out.println(poser.calcRMSD(t));
            transform(frag, t);
            fragPdbOut[i].add(frag);
            if (Math.IEEEremainder((double) ind, 100.0) == 0) {
              System.out.println("Opened: " + ind);
            }
          } catch (AtomException ae) {
            System.err.println("Problem with atom " + ae.getMessage() + " in pdb " + pdbName);
            ae.printStackTrace(System.err);
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
  public Tuple3[] getTupleArray(ArrayList<Triple> gapFrameStates) {
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
