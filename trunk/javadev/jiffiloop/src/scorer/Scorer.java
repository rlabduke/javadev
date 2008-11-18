// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package scorer;

import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.text.*;

import driftwood.r3.*;
import driftwood.moldb2.*;
import driftwood.data.*;
import driftwood.mysql.*;
import molikin.logic.*;
import jiffiloop.*;
//}}}

public class Scorer {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  PdbFileAnalyzer analyzer;
  DatabaseManager dm;
  //}}}
  
  //{{{ main
  //###############################################################
  public static void main(String[] args) {
    ArrayList<String> argList = parseArgs(args);
    if (argList.size() < 1) {
	    System.out.println("Not enough arguments: you must have an input pdb!");
    } else {
      File argFile = new File(argList.get(0));
      if (argFile.isDirectory()) {
        File[] subFiles = argFile.listFiles();
        for (File subFile : subFiles) {
          if (subFile.isFile()) {
            Scorer scorer = new Scorer(new File(subFile.getAbsolutePath()));
          }
        }
      } else {
        Scorer scorer = new Scorer(new File(argFile.getAbsolutePath()));
      }
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
        } else {
          System.err.println("*** Unrecognized option: "+arg);
        }
      } else {
        if (arg.matches("[0-9]*-[0-9]*")) {
        } else {
          System.out.println(arg);
          argList.add(arg);
        }
      }
    }
    return argList;
  }
  //}}}

  //{{{ Constructors
  public Scorer(File f) {
    String name = f.getName();
    //System.out.println(name);
    analyzer = new PdbFileAnalyzer(f);
    createAllGaps(5);
    Map<String, ArrayList<ProteinGap>> gaps = analyzer.getGaps();
    dm = new DatabaseManager();
    //dm.connectToDatabase("//spiral.research.duhs.duke.edu/qDBrDB");
    dm.connectToDatabase("//quality.biochem.duke.edu/vbc3");
    for (String gapString : gaps.keySet()) {
      ArrayList<ProteinGap> gapList = gaps.get(gapString);
      System.out.print(name+","+gapString+","+gapList.size());
      int score = countDb(gapList);
      System.out.println(","+score);
    }
    //System.out.println();
    //System.out.println("Score is: " + score);
    dm.close();
  }
  //}}}
  
  //{{{ createAllGaps
  public void createAllGaps(int size) {
    CoordinateFile pdb = analyzer.getCoordFile();
    Iterator models = (pdb.getModels()).iterator();
    while (models.hasNext()) {
      Model mod = (Model) models.next();
      Set<String> chainSet = mod.getChainIDs();
      for (String chain : chainSet) {
        Set<Residue> residues = mod.getChain(chain);
        for (Residue res : residues) {
          int seqNum = res.getSequenceInteger();
          analyzer.simulateGap(seqNum, size+seqNum, false);
        }
      }
    }
  }
  //}}}

  //{{{ countDb
  public int countDb(Collection<ProteinGap> gapList) {
    int score = 0;
    for (ProteinGap gap : gapList) {
      ArrayList<Double> gapFrame = gap.getParameters();
      int gapLength = gap.getSize();
      String sqlSelect = "SELECT count(*) FROM parameters5200 ";
      sqlSelect = sqlSelect.concat("WHERE frag_length = "+Integer.toString(gapLength)+" \n");
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
      sqlSelect = sqlSelect.concat(createWhereQuery(endDih, "end_dihedral") + " \n");
      sqlSelect = sqlSelect.concat("AND max_B_factor <= 35;");
      //System.out.println(sqlSelect);
      //ArrayList<String> listofMatches = filledMap.get(gap);
      System.err.print(".");
      dm.select(sqlSelect);
      while (dm.next()) {
        String count = dm.getString(1);
        score = score + Integer.parseInt(count);
        System.out.print(","+count);
        //System.out.println("count= "+dm.getString(1));
        //listofMatches.add(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" "+dm.getString(4));
      }
    }
    System.err.println();
    return score;
  }
  //}}}

  //{{{ createWhereQuery
  public String createWhereQuery(double frameVal, String colName) {
    if (frameVal > 180 - 25) {
      return "AND ("+colName+" >= "+Double.toString(frameVal-25)+" OR "+colName+" <= "+Double.toString(-360+25+frameVal)+")";
    } else if (frameVal < -180 + 25) {
      return "AND ("+colName+" <= "+Double.toString(frameVal+25)+" OR "+colName+" >= "+Double.toString(frameVal+360-25)+")";
    } else {
      return "AND ("+colName+" <= "+Double.toString(frameVal+25)+" AND "+colName+" >= "+Double.toString(frameVal-25)+")";
    }
  }
  //}}}

  //{{{ readFile
  public CoordinateFile readFile(File f) {
    try {
	    //System.out.println("reading in file");
      InputStream input = new FileInputStream(f);
      LineNumberReader    lnr;
      
      // Test for GZIPped files
      input = new BufferedInputStream(input);
      input.mark(10);
      if(input.read() == 31 && input.read() == 139)
      {
        // We've found the gzip magic numbers...
        input.reset();
        input = new GZIPInputStream(input);
      }
      else input.reset();
      
      lnr = new LineNumberReader(new InputStreamReader(input));
      
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
      //File pdb = new File(f);
	    CoordinateFile coordFile = reader.read(lnr);
	    System.out.println(coordFile.getIdCode()+" has been read");
      lnr.close();
      return coordFile;
    }
    catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
    }
    return null;
  }
  //}}}

}
