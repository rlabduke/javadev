// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fragmentfiller;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import driftwood.moldb2.*;
import driftwood.data.*;
import driftwood.r3.*;
//}}}

public class PdbLibraryReader {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  HashMap<String, ZipEntry> pdbMap;
  CoordinateFile currentPdb;
  String currentChain;
  ZipFile zip;
  //}}}
  
  //{{{ Constructor
  public PdbLibraryReader(File location) {
    File[] pdbFiles = location.listFiles();
    pdbMap = new HashMap<String, ZipEntry>();
    for (File f : pdbFiles) {
      if (f.getName().endsWith(".zip")) {
        try {
          System.out.println("Opening file: " + f.getName());
          zip = new ZipFile(f);
          Enumeration entries= zip.entries();
          while (entries.hasMoreElements()) {
            ZipEntry zEntry = (ZipEntry) entries.nextElement();
            if (zEntry.getName().indexOf(".pdb") > -1) {
              //System.out.println("Scanning: " + zEntry.getName());
              //LineNumberReader reader = new LineNumberReader(new InputStreamReader(zip.getInputStream(zEntry)));
              //System.out.println(zEntry.getName().substring(7, 11));
              String[] splitInfo = zEntry.getName().split("/");
              String pdbFileName = splitInfo[splitInfo.length - 1];
              //System.out.println(pdbFileName + ":"+ pdbFileName.substring(0,5));
              pdbMap.put(pdbFileName.substring(0, 5).toLowerCase(), zEntry);
            }
          }
          
        } catch (IOException ie) {
          System.err.println("An I/O error occurred while loading the file:\n"+ie.getMessage());
        }
      }
    }
  }
  //}}}
  
  //{{{ readPdb
  public CoordinateFile readPdb(String name) {
    //if (!pdbMap.containsKey(name)) {
    //  System.out.println(name);
    //}
    try {
      LineNumberReader pdb = new LineNumberReader(new InputStreamReader(zip.getInputStream(pdbMap.get(name))));
      PdbReader reader = new PdbReader();
      CoordinateFile pdbFile = reader.read(pdb);
      return pdbFile;
    } catch (IOException ie) {
	    System.err.println("Problem when reading pdb file: " + name);
    }
    return null;
  }
  //}}}
  
  //{{{ setCurrentPdb
  public void setCurrentPdb(String name, String chain) {
    if (((currentPdb == null)||(currentChain == null)) || ((!currentPdb.getIdCode().equals(name))||(currentChain!=chain))) {
      currentPdb = readPdb(name.toLowerCase()+chain.toLowerCase());
      currentChain = chain;
      //System.out.println("Setting currentPdb to: " + name);
    }
  }
  //}}}
  
  //{{{ getFragment
  public Model getFragment(String modNum, String chain, int startRes, int length) {
    //CoordinateFile pdbFile = readPdb(pdbName);
    //UberSet fragRes = new UberSet();
    Model fragModel = new Model(modNum);
    ModelState fragState = new ModelState();
    TreeMap stateMap = new TreeMap();
    stateMap.put(" ", fragState);
    fragModel.setStates(stateMap);
    if (currentPdb != null) {
      Model firstMod = currentPdb.getFirstModel();
      ModelState firstState = firstMod.getState();
      Collection modResidues = firstMod.getChain(chain);
      //System.out.println("Currently set to " + currentPdb.getIdCode() + " with chain:" + chain);
      if (modResidues != null) {
        Iterator iter = modResidues.iterator();
        while ((fragModel.getResidues().size() <= length + 2)&&(iter.hasNext())) {
          Residue res = (Residue) iter.next();
          int resNum = res.getSequenceInteger();
          if ((resNum >= startRes)&&(resNum <= startRes + length + 2)&&(res.getInsertionCode().equals(" "))) {
            try {
              if (isResidueComplete(res)) {
                fragModel.add(res);
                res.cloneStates(res, firstState, fragState);
                String past80 = "  " + currentPdb.getIdCode() + Integer.toString(startRes) + chain;
                addPast80Info(res, past80, fragState);
              } else {
                System.err.println("fragment from "+currentPdb.getIdCode()+" "+String.valueOf(resNum)+" discarded due to incomplete residues");
                return null;
              }
            } catch (AtomException ae) {
              System.err.println("Error occurred during cloning of atom in PdbLibraryReader.");
            } catch (ResidueException re) {
              System.err.println("Error occurred during adding of residue in PdbLibraryReader.");
            }
          }
        }
      } else {
        System.err.println("This shouldn't happen, but a pdb in the library does not have a chain the database says it should!");
      }
    } else {
      System.err.println("No pdb set in PdbLibraryReader!");
    }
    trimFragment(fragModel);
    return fragModel;
  }
  //}}}
  
  //{{{ addPast80Info
  public void addPast80Info(Residue res, String past80, ModelState modState) {
    Collection atoms = res.getAtoms();
    Iterator iter = atoms.iterator();
    while (iter.hasNext()) {
      try {
        Atom at = (Atom) iter.next();
        AtomState atState = modState.get(at);
        atState.setPast80(past80);
      } catch (AtomException ae) {
        System.err.println("Error occurred while adding extra info in PdbLibraryReader.");
      } 
    }
  }
  //}}}
  
  //{{{ trimFragment
  /** to remove the extra atoms from the ends of the fragments **/
  public void trimFragment(Model fragment) {
    try {
      UberSet residues = new UberSet(fragment.getResidues());
      Residue firstRes = (Residue) residues.firstItem();
      Atom firstN = firstRes.getAtom(" N  ");
      if (firstN != null) firstRes.remove(firstN);
      else throw new AtomException("trouble trimming model "+fragment.toString()+" res "+firstRes.getSequenceNumber());
      Residue lastRes = (Residue) residues.lastItem();
      Atom lastO = lastRes.getAtom(" O  ");
      Atom lastC = lastRes.getAtom(" C  ");
      if (lastO != null) lastRes.remove(lastO);
      else throw new AtomException("trouble trimming model "+fragment.toString()+" res "+firstRes.getSequenceNumber());
      if (lastC != null) lastRes.remove(lastC);
      else throw new AtomException("trouble trimming model "+fragment.toString()+" res "+firstRes.getSequenceNumber());
    } catch (AtomException ae) {
      System.err.println(ae);
    }
  }
  //}}}
  
  //{{{ checkResidue
  public boolean isResidueComplete(Residue res) {
    if (res == null) return false;
    return ((res.getAtom(" N  ")!=null)&&
            (res.getAtom(" CA ")!=null)&&
            (res.getAtom(" C  ")!=null)&&
            (res.getAtom(" O  ")!=null));
  }
  //}}}
  
  //{{{ getFragmentEndpointAtoms
  public Tuple3[] getFragmentEndpointAtoms(Model fragment) {
    //UberSet fragRes = (UberSet) residues;
    UberSet fragRes = new UberSet(fragment.getResidues());
    Tuple3[] endAtomStates = new Tuple3[4];
    if (currentPdb != null) {
      //Model firstMod = currentPdb.getFirstModel();
      ModelState modState = fragment.getState();
      Residue zeroRes = (Residue) fragRes.firstItem();
      Residue oneRes = (Residue) fragRes.itemAfter(zeroRes);
      Residue n1Res = (Residue) fragRes.lastItem();
      Residue nRes = (Residue) fragRes.itemBefore(n1Res);
      //System.out.println(zeroRes.getSequenceInteger() + " " + oneRes.getSequenceInteger() + " " + nRes.getSequenceInteger() + " " + n1Res.getSequenceInteger());
      try {
        endAtomStates[0] = modState.get(zeroRes.getAtom(" CA "));
        endAtomStates[1] =modState.get(oneRes.getAtom(" CA "));
        endAtomStates[2] =modState.get(nRes.getAtom(" CA "));
        endAtomStates[3] =modState.get(n1Res.getAtom(" CA "));
      } catch (AtomException ae) {
        System.err.println("Problem with atom " + ae.getMessage() + " in pdb " + currentPdb.toString());
      }
    } else {
      System.err.println("No pdb set in PdbLibraryReader!");
    }
    return endAtomStates;
  }
  //}}}
  
  //{{{ getStemNtermAtoms
  public Tuple3[] getStemNtermAtoms(Model fragment) {
    //UberSet fragRes = (UberSet) residues;
    UberSet fragRes = new UberSet(fragment.getResidues());
    Tuple3[] endAtomStates = new Tuple3[3];
    if (currentPdb != null) {
      //Model firstMod = currentPdb.getFirstModel();
      ModelState modState = fragment.getState();
      try {
        Residue zeroRes = (Residue) fragRes.firstItem();
        Residue oneRes = (Residue) fragRes.itemAfter(zeroRes);
        Residue twoRes = (Residue) fragRes.itemAfter(oneRes);
        //Residue n1Res = (Residue) fragRes.lastItem();
        //Residue nRes = (Residue) fragRes.itemBefore(n1Res);
        //System.out.println(zeroRes.getSequenceInteger() + " " + oneRes.getSequenceInteger() + " " + nRes.getSequenceInteger() + " " + n1Res.getSequenceInteger());
        
        endAtomStates[0] = modState.get(zeroRes.getAtom(" CA "));
        endAtomStates[1] = modState.get(oneRes.getAtom(" CA "));
        endAtomStates[2] = modState.get(twoRes.getAtom(" CA "));
        //endAtomStates[2] =modState.get(nRes.getAtom(" CA "));
        //endAtomStates[3] =modState.get(n1Res.getAtom(" CA "));
      } catch (AtomException ae) {
        System.err.println("Problem with atom " + ae.getMessage() + " in pdb " + currentPdb.toString());
      } catch (NoSuchElementException nsee) {
        System.err.println("Problem with residue "+fragRes.firstItem().toString()+" in pdb "+currentPdb.toString());
      }
    } else {
      System.err.println("No pdb set in PdbLibraryReader!");
    }
    return endAtomStates;
  }
  //}}}
  
  //{{{ getStemCtermAtoms
  public Tuple3[] getStemCtermAtoms(Model fragment) {
    //UberSet fragRes = (UberSet) residues;
    UberSet fragRes = new UberSet(fragment.getResidues());
    Tuple3[] endAtomStates = new Tuple3[3];
    if (currentPdb != null) {
      //Model firstMod = currentPdb.getFirstModel();
      ModelState modState = fragment.getState();
      try {
        Residue n1Res = (Residue) fragRes.lastItem();
        Residue nRes = (Residue) fragRes.itemBefore(n1Res);
        Residue mRes = (Residue) fragRes.itemBefore(nRes);
        //Residue n1Res = (Residue) fragRes.lastItem();
        //Residue nRes = (Residue) fragRes.itemBefore(n1Res);
        //System.out.println(zeroRes.getSequenceInteger() + " " + oneRes.getSequenceInteger() + " " + nRes.getSequenceInteger() + " " + n1Res.getSequenceInteger());
        endAtomStates[0] = modState.get(mRes.getAtom(" CA "));
        endAtomStates[1] = modState.get(nRes.getAtom(" CA "));
        endAtomStates[2] = modState.get(n1Res.getAtom(" CA "));
        //endAtomStates[2] =modState.get(nRes.getAtom(" CA "));
        //endAtomStates[3] =modState.get(n1Res.getAtom(" CA "));
      } catch (AtomException ae) {
        System.err.println("Problem with atom " + ae.getMessage() + " in pdb " + currentPdb.toString());
      } catch (NoSuchElementException nsee) {
        System.err.println("Problem with residue "+fragRes.lastItem().toString()+" in pdb "+currentPdb.toString());
      }
    } else {
      System.err.println("No pdb set in PdbLibraryReader!");
    }
    return endAtomStates;
  }
  //}}}
  
  //{{{ getFragmentNtermAtoms
  public Tuple3[] getFragmentNtermAtoms(Model fragment) {
    UberSet fragRes = new UberSet(fragment.getResidues());
    Tuple3[] endAtomStates = new Tuple3[3];
    if (currentPdb != null) {
      //Model firstMod = currentPdb.getFirstModel();
      ModelState modState = fragment.getState();
      Residue zeroRes = (Residue) fragRes.firstItem();
      Residue oneRes = (Residue) fragRes.itemAfter(zeroRes);
      //Residue n1Res = (Residue) fragRes.lastItem();
      //Residue nRes = (Residue) fragRes.itemBefore(n1Res);
      //System.out.println(zeroRes.getSequenceInteger() + " " + oneRes.getSequenceInteger() + " " + nRes.getSequenceInteger() + " " + n1Res.getSequenceInteger());
      try {
        endAtomStates[0] = modState.get(zeroRes.getAtom(" CA "));
        endAtomStates[1] =modState.get(zeroRes.getAtom(" O  "));
        endAtomStates[2] =modState.get(oneRes.getAtom(" CA "));
        //endAtomStates[3] =modState.get(n1Res.getAtom(" CA "));
      } catch (AtomException ae) {
        System.err.println("Problem with atom " + ae.getMessage() + " in pdb " + currentPdb.toString());
      }
    } else {
      System.err.println("No pdb set in PdbLibraryReader!");
    }
    return endAtomStates;
  }
  //}}}
  
}
