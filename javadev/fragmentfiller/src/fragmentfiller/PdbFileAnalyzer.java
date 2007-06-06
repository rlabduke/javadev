// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fragmentfiller;

import java.util.*;
import java.io.*;
import driftwood.moldb2.*;
import driftwood.data.*;
//}}}

public class PdbFileAnalyzer {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  CoordinateFile pdbFile;
  HashMap<Model, Map> masterGapMap; // Model -> (chain -> (startGapInt -> listofResidues))
  //}}}
  
  //{{{ Constructor
  public PdbFileAnalyzer(File pdbFile) {
    masterGapMap = new HashMap<Model, Map>();
    readPdb(pdbFile);
    test();
  }
  //}}}
  
  //{{{ readPdb
  public void readPdb(File pdb) {
    PdbReader reader = new PdbReader();
    try {
      pdbFile = reader.read(pdb);
	    //System.out.println(coordFile.getIdCode());
	    Iterator models = (pdbFile.getModels()).iterator();
	    while (models.hasNext()) {
        //System.out.print(".");
        Model mod = (Model) models.next();
        masterGapMap.put(mod, analyzeModel(mod));
	    }
    } catch (IOException ie) {
	    System.err.println("Problem when reading pdb file");
    }
  }
  //}}}
  
  //{{{ analyzeModel
  public HashMap<String, Map> analyzeModel(Model mod) {
    ModelState modState = mod.getState();
    HashMap<String, Map> chainGapMap = new HashMap<String, Map>();
    Set<String> chainSet = mod.getChainIDs();
    for (String chain : chainSet) {
      Set<Residue> residues = mod.getChain(chain);
      chainGapMap.put(chain, findGaps(residues));
    }
    return chainGapMap;
  }
  //}}}
  
  //{{{ removeBadRes
  /* not needed currently */
  public Set<Residue> removeBadRes(Set<Residue> chainofRes) {
    UberSet cleanChain = new UberSet();
    for (Residue res : chainofRes) {
      if (containsCaO(res)) {
        cleanChain.add(res);
      }
    }
    return cleanChain;
  }
  //}}}
  
  //{{{ findGaps
  public Map<Integer, ArrayList> findGaps(Set<Residue> chainofRes) {
    int prevSeq = 100000;
    UberSet uberChainSet = new UberSet(chainofRes);
    TreeMap<Integer, ArrayList> gapMap = new TreeMap<Integer, ArrayList>();
    for (Residue res : chainofRes) {
      
      int seqNum = res.getSequenceInteger();
      if (seqNum > prevSeq + 1) {
        ArrayList<Residue> paramRes = new ArrayList<Residue>();
        Residue oneRes = (Residue) uberChainSet.itemBefore(res);
        Residue zeroRes = (Residue) uberChainSet.itemBefore(oneRes);
        while (!containsCaO(zeroRes)&&!containsCa(oneRes)) {
          oneRes = (Residue) uberChainSet.itemBefore(oneRes);
          zeroRes = (Residue) uberChainSet.itemBefore(oneRes);
        }
        Residue nRes = res;
        Residue n1Res = (Residue) uberChainSet.itemAfter(nRes);
        while (!containsCaO(nRes)&&!containsCa(n1Res)) {
          nRes = (Residue) uberChainSet.itemAfter(nRes);
          n1Res = (Residue) uberChainSet.itemAfter(nRes);
        }
        paramRes.add(zeroRes);
        paramRes.add(oneRes);
        paramRes.add(nRes);
        paramRes.add(n1Res);
        gapMap.put(new Integer(prevSeq), paramRes);
      }
      prevSeq = seqNum;
    }
    return gapMap;
  }
  //}}}
  
  //{{{ getGapAtoms
  public Map<String, ArrayList> getGapAtoms() {
    TreeMap<String, ArrayList> statesMap = new TreeMap<String, ArrayList>();
    for (Model mod : masterGapMap.keySet()) {
      ModelState modState = mod.getState();
      Map<String, Map> chainGapMap = (Map<String, Map>) masterGapMap.get(mod);
      for (String chain : chainGapMap.keySet()) {
        Map<Integer, ArrayList> gapMap = chainGapMap.get(chain);
        for (Integer startGap : gapMap.keySet()) {
          ArrayList<Residue> resList = gapMap.get(startGap);
          Residue zeroRes = resList.get(0);
          Residue oneRes = resList.get(1);
          Residue nRes = resList.get(2);
          Residue n1Res = resList.get(3);
          ArrayList<AtomState> states = new ArrayList<AtomState>();
          try {
            states.add(modState.get(zeroRes.getAtom(" CA ")));
            states.add(modState.get(oneRes.getAtom(" CA ")));
            states.add(modState.get(nRes.getAtom(" CA ")));
            states.add(modState.get(n1Res.getAtom(" CA ")));
            states.add(modState.get(zeroRes.getAtom(" O  ")));
            states.add(modState.get(nRes.getAtom(" O  ")));
            statesMap.put(mod.getName() + "," + chain + "," + oneRes.getSequenceInteger() + "," + nRes.getSequenceInteger(), states);
          } catch (AtomException ae) {
            System.err.println("Problem with atom " + ae.getMessage() + " in model " + mod.toString());
          }
        }
      }
    }
    return statesMap;
  }
  //}}}
  
  //{{{ checkCaO
  public boolean containsCaO(Residue res) {
    if (res == null) return false;
    return (res.getAtom(" CA ")!=null)&&(res.getAtom(" O  ")!=null);
  }
  //}}}
  
  //{{{ checkCa
  public boolean containsCa(Residue res) {
    if (res == null) return false;
    return (res.getAtom(" CA ")!=null);
  }
  //}}}
  
  //{{{ test
  public void test() {
    Iterator iter = masterGapMap.values().iterator();
    while (iter.hasNext()) {
      Map<String, Map> chainGapMap = (Map<String, Map>) iter.next();
      for (String chain : chainGapMap.keySet()) {
        System.out.println(chain);
        Map<Integer, ArrayList> gapMap = chainGapMap.get(chain);
        for (Integer startGap : gapMap.keySet()) {
          ArrayList<Residue> list = gapMap.get(startGap);
          Residue endRes = list.get(2);
          System.out.println(startGap + " -> " + endRes.getSequenceInteger());
        }
      }
    }
  }
  //}}}
  
}
