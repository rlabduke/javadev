// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.moldb2.*;
import java.util.*;
import java.io.*;

//}}}

public class SequenceAnalyzer {
    
  //{{{ main
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("This function takes 1 argument, you need a source pdb!");
    } else {
      File pdbIn = new File(args[0]);
      //File pdbOut = new File(args[1]);
      SequenceAnalyzer analyzer = new SequenceAnalyzer(pdbIn.getAbsolutePath());
    }
  }
  //}}}
  
  //{{{ Constructor
  public SequenceAnalyzer(String inPdb) {
    try {
	    System.out.println("reading in file");
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
      File pdb = new File(inPdb);
	    CoordinateFile coordFile = reader.read(pdb);
	    System.out.println("file has been read");
      analyzeModels(coordFile);
      analyzeCombinations(coordFile, 2);
      analyzeCombinations(coordFile, 3);
	    //System.out.println("builder built");
	    //writePdbFile(cleanFile, outFile);
    }
    catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
    }
  }
  //}}}
  
  //{{{ analyzeModels
  public void analyzeModels(CoordinateFile coordFile) {
    HashMap[] resCounts = new HashMap[20];
    Iterator models = coordFile.getModels().iterator();
    while (models.hasNext()) {
      Model mod = (Model) models.next();
      ArrayList residues = new ArrayList(mod.getResidues());
      for (int i = 0; i < residues.size(); i++) {
        Residue res = (Residue) residues.get(i);
        String resName = res.getName();
        if (resCounts[i] == null) {
          System.out.println("making new hashmap for size " + i);
          resCounts[i] = new HashMap<String, Integer>();
        }
        HashMap map = resCounts[i];
        if (map.containsKey(resName)) {
          Integer count = (Integer) map.get(resName);
          map.put(resName, new Integer(count.intValue() + 1));
        } else {
          map.put(resName, new Integer(1));
        }
      }
    }
    outputCounts(resCounts);
  }
  //}}}
  
  //{{{ analyzeCombinations
  public void analyzeCombinations(CoordinateFile coordFile, int numCombos) {
    HashMap[] resCounts = new HashMap[20];
    Iterator models = coordFile.getModels().iterator();
    while (models.hasNext()) {
      Model mod = (Model) models.next();
      ArrayList residues = new ArrayList(mod.getResidues());
      for (int i = 0; i + numCombos <= residues.size(); i++) {
        String resName = getMultResNames(residues, i, numCombos);
        if (resCounts[i] == null) {
          System.out.println("making new hashmap for size " + i);
          resCounts[i] = new HashMap<String, Integer>();
        }
        HashMap map = resCounts[i];
        if (map.containsKey(resName)) {
          Integer count = (Integer) map.get(resName);
          map.put(resName, new Integer(count.intValue() + 1));
        } else {
          map.put(resName, new Integer(1));
        }
      }
    }
    outputCounts(resCounts);
  }
  //}}}
  
  //{{{ getMultipleResNames
  public String getMultResNames(ArrayList residues, int index, int numCombos) {
    String names = "";
    for (int i = 0; i < numCombos; i++) {
      Residue res = (Residue) residues.get(index+i);
      names = names + res.getName();
    }
    return names;
  }
  //}}}
  
  //{{{ outputCounts
  public void outputCounts(HashMap[] resCounts) {
    for (int i = 0; i < resCounts.length; i++) {
      if (resCounts[i] != null) {
        HashMap<String, Integer> map = resCounts[i];
        System.out.println("position " + (i + 1));
        Iterator keys = map.keySet().iterator();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          System.out.println(key + " " + (Integer) map.get(key));
        }
      }
    }
  }
  //}}}
}
