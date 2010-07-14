// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package jiffiloop;

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
  HashMap<Model, HashMap<String, ArrayList>> masterGapMap; // Model -> (chain -> ListofGaps)
  HashMap<Model, HashMap<String, ArrayList>> masterStemMap; // Model -> (chain -> ListofStems).
  //}}}
  
  //{{{ Constructor
  public PdbFileAnalyzer(File pdb) {
    masterGapMap = new HashMap<Model, HashMap<String, ArrayList>>();
    masterStemMap = new HashMap<Model, HashMap<String, ArrayList>>();
    PdbReader reader = new PdbReader();
    try {
      pdbFile = reader.read(pdb);
	    pdbFile.setIdCode(pdb.getName());
    } catch (IOException ie) {
      System.err.println("Problem when reading pdb file");
    }
    //test();
  }
  //}}}
  
  //{{{ clear
  public void clear() {
    masterGapMap = new HashMap<Model, HashMap<String, ArrayList>>();
    masterStemMap = new HashMap<Model, HashMap<String, ArrayList>>();
  }
  //}}}
  
  //{{{ analyzePdb
  public void analyzePdb() {
    //PdbReader reader = new PdbReader();
    //try {
    //  pdbFile = reader.read(pdb);
	  //  pdbFile.setIdCode(pdb.getName());
	    Iterator models = (pdbFile.getModels()).iterator();
	    while (models.hasNext()) {
        //System.out.print(".");
        Model mod = (Model) models.next();
        masterGapMap.put(mod, analyzeModelforGaps(mod));
        masterStemMap.put(mod, analyzeModelforStems(mod));
	    }
    //} catch (IOException ie) {
	  //  System.err.println("Problem when reading pdb file");
    //}
  }
  //}}}
  
  //{{{ analyzeModel
  public HashMap<String, ArrayList> analyzeModelforGaps(Model mod) {
    //ModelState modState = mod.getState();
    HashMap<String, ArrayList> chainGapMap = new HashMap<String, ArrayList>();
    Set<String> chainSet = mod.getChainIDs();
    for (String chain : chainSet) {
      Set<Residue> residues = mod.getChain(chain);
      chainGapMap.put(chain, findGaps(mod, chain, residues));
    }
    return chainGapMap;
  }
  
  public HashMap<String, ArrayList> analyzeModelforStems(Model mod) {
    //ModelState modState = mod.getState();
    HashMap<String, ArrayList> chainStemMap = new HashMap<String, ArrayList>();
    Set<String> chainSet = mod.getChainIDs();
    for (String chain : chainSet) {
      Set<Residue> residues = mod.getChain(chain);
      chainStemMap.put(chain, findStems(mod, chain, residues));
    }
    return chainStemMap;
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
  
  //{{{ findStems
  public ArrayList<ProteinStem> findStems(Model mod, String chainId, Set<Residue> chainofRes) {
    int prevSeq = 100000;
    UberSet uberChainSet = new UberSet(chainofRes);
    ArrayList<ProteinStem> stems = new ArrayList<ProteinStem>();
    for (Residue res : chainofRes) {
      int seqNum = res.getSequenceInteger();
      if (seqNum > prevSeq + 1) {
        ArrayList<Residue> paramRes = new ArrayList<Residue>();
        try {
          Residue twoRes = (Residue) uberChainSet.itemBefore(res);
          Residue oneRes = (Residue) uberChainSet.itemBefore(twoRes);
          Residue zeroRes = (Residue) uberChainSet.itemBefore(oneRes);
          while (!containsCaO(zeroRes)&&!containsCaO(oneRes)&&!containsCa(twoRes)) {
            twoRes = (Residue) uberChainSet.itemBefore(twoRes);
            oneRes = (Residue) uberChainSet.itemBefore(twoRes);
            zeroRes = (Residue) uberChainSet.itemBefore(oneRes);
          }
          ProteinStem n_stem = new ProteinStem(mod, chainId, zeroRes, oneRes, twoRes, ProteinStem.N_TERM);
          stems.add(n_stem);
        } catch (NoSuchElementException nsee) {
          System.out.println("No nterm stem possible for " + res.toString());
        }
        try {
          Residue nRes = res;
          Residue n1Res = (Residue) uberChainSet.itemAfter(nRes);
          Residue n2Res = (Residue) uberChainSet.itemAfter(n1Res);
          while (!containsCaO(nRes)&&!containsCaO(n1Res)&&!containsCa(n2Res)) {
            nRes = (Residue) uberChainSet.itemAfter(nRes);
            n1Res = (Residue) uberChainSet.itemAfter(nRes);
            n2Res = (Residue) uberChainSet.itemAfter(n1Res);
          }
          ProteinStem c_stem = new ProteinStem(mod, chainId, nRes, n1Res, n2Res, ProteinStem.C_TERM);
          stems.add(c_stem);
        } catch (NoSuchElementException nsee) {
          System.out.println("No cterm stem possible for " + res.toString());
        }
      }
      prevSeq = seqNum;
    }
    return stems;
  }
  //}}}
  
  //{{{ getStems
  public Map<String, ArrayList<ProteinStem>> getStems() {
    TreeMap<String, ArrayList<ProteinStem>> statesMap = new TreeMap<String, ArrayList<ProteinStem>>();
    for (Model mod : masterStemMap.keySet()) {
      //ModelState modState = mod.getState();
      Map<String, ArrayList> chainGapMap = (Map<String, ArrayList>) masterStemMap.get(mod);
      for (String chain : chainGapMap.keySet()) {
        ArrayList<ProteinStem> stems = chainGapMap.get(chain);
        statesMap.put(mod.getName()+","+chain, stems);
      }
    }
    return statesMap;
  }
  //}}}
  
  //{{{ findGaps
  //public Map<Integer, ArrayList> findGaps(ModelState modState, Set<Residue> chainofRes) {
  public ArrayList<ProteinGap> findGaps(Model mod, String chainId, Set<Residue> chainofRes) {
    int prevSeq = 100000;
    UberSet uberChainSet = new UberSet(chainofRes);
    //TreeMap<Integer, ArrayList> gapMap = new TreeMap<Integer, ArrayList>();
    ArrayList<ProteinGap> gaps = new ArrayList<ProteinGap>();
    for (Residue res : chainofRes) {
      int seqNum = res.getSequenceInteger();
      if (hasProteinBB(res)) { // hopefully get around bug when waters in chain
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
          ProteinGap gap = new ProteinGap(mod, chainId, zeroRes, oneRes, nRes, n1Res);
          //paramRes.add(zeroRes);
          //paramRes.add(oneRes);
          //paramRes.add(nRes);
          //paramRes.add(n1Res);
          //gapMap.put(new Integer(prevSeq), paramRes);
          gaps.add(gap);
        }
      }
      prevSeq = seqNum;
    }
    return gaps;
  }
  //}}}
  
  //{{{ simulateGap
  public void simulateGap(int oneResNum, int nResNum, boolean doStems) {
    Iterator models = (pdbFile.getModels()).iterator();
    while (models.hasNext()) {
      //System.out.print(".");
      Model mod = (Model) models.next();
      //HashMap<String, ArrayList> chainGapMap = new HashMap<String, ArrayList>();
      HashMap<String, ArrayList> chainMap = masterGapMap.get(mod);
      if (chainMap == null) {
        chainMap = new HashMap<String, ArrayList>();
        masterGapMap.put(mod, chainMap);
      }
      HashMap<String, ArrayList> stemChainMap = masterStemMap.get(mod);
      if (stemChainMap == null) {
        stemChainMap = new HashMap<String, ArrayList>();
        masterStemMap.put(mod, stemChainMap);
      }
      Set<String> chainSet = mod.getChainIDs();
      for (String chain : chainSet) {
        //ModelState modState = mod.getState();
        ArrayList<ProteinGap> gaps = chainMap.get(chain);
        if (gaps == null) {
          gaps = new ArrayList<ProteinGap>();
          chainMap.put(chain, gaps);
        }
        ArrayList<ProteinStem> stems = stemChainMap.get(chain);
        if (stems == null) {
          stems = new ArrayList<ProteinStem>();
          stemChainMap.put(chain, stems);
        }
        Set<Residue> residues = mod.getChain(chain);
        Residue minusRes = null;
        Residue zeroRes = null;
        Residue oneRes = null;
        Residue nRes = null;
        Residue n1Res = null;
        Residue n1StemRes = null;
        Residue n2Res = null;
        for (Residue res : residues) {
          int seqNum = res.getSequenceInteger();
          if ((seqNum == oneResNum-2)&&(containsCaO(res))) minusRes = res;
          if ((seqNum == oneResNum-1)&&(containsCaO(res))) zeroRes = res;
          if ((seqNum == oneResNum)&&(containsCa(res))) oneRes = res;
          if ((seqNum == nResNum)&&(containsCaO(res))) nRes = res;
          if ((seqNum == nResNum+1)&&(containsCa(res))) n1Res = res;
          if ((seqNum == nResNum+1)&&(containsCaO(res))) n1StemRes = res;
          if ((seqNum == nResNum+2)&&(containsCa(res))) n2Res = res;
        }
        if ((zeroRes!=null)&&(oneRes!=null)&&(nRes!=null)&&(n1Res!=null)) {
          gaps.add(new ProteinGap(mod, chain, zeroRes, oneRes, nRes, n1Res));
        }
        if (doStems) {
          if ((minusRes!=null)&&(zeroRes!=null)&&(oneRes!=null)) {
            stems.add(new ProteinStem(mod, chain, minusRes, zeroRes, oneRes, ProteinStem.N_TERM));
          }
          if ((nRes!=null)&&(n1StemRes!=null)&&(n2Res!=null)) {
            stems.add(new ProteinStem(mod, chain, nRes, n1StemRes, n2Res, ProteinStem.C_TERM));
          }
        }
      }
    }
  }
  //}}}
  
  //{{{ getGaps
  public Map<String, ArrayList<ProteinGap>> getGaps() {
    TreeMap<String, ArrayList<ProteinGap>> statesMap = new TreeMap<String, ArrayList<ProteinGap>>();
    for (Model mod : masterGapMap.keySet()) {
      //ModelState modState = mod.getState();
      Map<String, ArrayList> chainGapMap = (Map<String, ArrayList>) masterGapMap.get(mod);
      for (String chain : chainGapMap.keySet()) {
        ArrayList<ProteinGap> gaps = chainGapMap.get(chain);
        statesMap.put(mod.getName()+","+chain, gaps);
      }
    }
    return statesMap;
  }
  /*
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
  }*/
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
  
  //{{{ hasProteinBB
  public boolean hasProteinBB(Residue res) {
    if (res == null) return false;
    return (res.getAtom(" CA ")!=null)&&(res.getAtom(" O  ")!=null)&&(res.getAtom(" N  ")!=null)&&(res.getAtom(" C  ")!=null);
  }
  //}}}
  
  //{{{ getCoordFile
  public CoordinateFile getCoordFile() {
    return pdbFile;
  }
  //}}}
  
  //{{{ test
  public void test() {
    Iterator iter = masterGapMap.values().iterator();
    while (iter.hasNext()) {
      Map<String, ArrayList> chainGapMap = (Map<String, ArrayList>) iter.next();
      for (String chain : chainGapMap.keySet()) {
        System.out.println(chain);
        ArrayList<ProteinGap> gapMap = chainGapMap.get(chain);
        //for (Integer startGap : gapMap.keySet()) {
        //  ArrayList<Residue> list = gapMap.get(startGap);
        //  Residue endRes = list.get(2);
        //  System.out.println(startGap + " -> " + endRes.getSequenceInteger());
        //}
      }
    }
  }
  //}}}
  
}
