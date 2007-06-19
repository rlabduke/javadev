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
              pdbMap.put(zEntry.getName().substring(7, 11), zEntry);
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
  public void setCurrentPdb(String name) {
    if ((currentPdb == null) || (!currentPdb.getIdCode().equals(name))) {
      currentPdb = readPdb(name);
      //System.out.println("Setting currentPdb to: " + name);
    }
  }
  //}}}
  
  //{{{ getFragment
  public Model getFragment(String modNum, int startRes, int length) {
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
      Collection modResidues = firstMod.getResidues();
      Iterator iter = modResidues.iterator();
      while ((fragModel.getResidues().size() <= length + 2)&&(iter.hasNext())) {
        Residue res = (Residue) iter.next();
        int resNum = res.getSequenceInteger();
        if ((resNum >= startRes)&&(resNum <= startRes + length + 2)) {
          try {
            fragModel.add(res);
            res.cloneStates(res, firstState, fragState);
            String past80 = "  " + currentPdb.getIdCode() + Integer.toString(startRes);
            addPast80Info(res, past80, fragState);
          } catch (AtomException ae) {
            System.err.println("Error occurred during cloning of atom in PdbLibraryReader.");
          } catch (ResidueException re) {
            System.err.println("Error occurred during adding of residue in PdbLibraryReader.");
          }
        }
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
      firstRes.remove(firstRes.getAtom(" N  "));
      Residue lastRes = (Residue) residues.lastItem();
      lastRes.remove(lastRes.getAtom(" O  "));
      lastRes.remove(lastRes.getAtom(" C  "));
    } catch (AtomException ae) {
      System.err.println("Error while trimming fragment.");
    }
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
  
}
