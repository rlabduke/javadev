// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.moldb2.*;
import driftwood.r3.*;
import java.io.*;
import java.util.*;
//}}}

/**
* FragmentRotator is intended to generate a set of slightly rotated fragments
* so I can see how slight changes in alignment affect the RDC RMSDs that we are getting
* for our fragments.
*/
public class FragmentRotator {

  //{{{ Constants
  //}}}
  
  //{{{ Variables
  //}}}
  
  //{{{ main
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("This function takes 2 arguments, you need a source pdb and an output pdb!");
    } else {
      File pdbIn = new File(args[0]);
      File pdbOut = new File(args[1]);

      FragmentRotator fr = new FragmentRotator(new File(pdbIn.getAbsolutePath()), new File(pdbOut.getAbsolutePath()));
    }
  }
  //}}}
   
  //{{{ Constructors
  public FragmentRotator(File pdbIn, File fileOut) {
    try {
	    System.out.println("reading in file");
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
	    CoordinateFile coordFile = reader.read(pdbIn);
	    System.out.println("file has been read");
      //CoordinateFile cleanFile = superimposeModels(coordFile);
	    //System.out.println("builder built");
	    //writePdbFile(cleanFile, outFile);
      CoordinateFile outPdb = rotateModels(coordFile);
      writePdbFile(outPdb, fileOut);
    }
    catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
    }
  }
  //}}}
  
  //{{{ rotateModels
  public CoordinateFile rotateModels(CoordinateFile coords) {
    CoordinateFile rotatedModels = new CoordinateFile();
    Iterator iter = (coords.getModels()).iterator();
    int i = 1;
    while (iter.hasNext()) {
      Model mod = (Model) iter.next();
      ModelState modstate = mod.getState();
      Transform rotation = new Transform();
      try {
        for (double d = -90; d < 90; d = d + 0.1) {
          //Model modClone = (Model) mod.clone();
          Model modClone = deepClone(mod, Integer.toString(i));
          i++;
          ModelState cloneState = modClone.getState(); 
          Residue oneRes = getOneResidue(modClone);
          Residue nRes = getNResidue(modClone);
          Atom oneCa = getCa(oneRes);
          Atom nCa = getCa(nRes);
          rotation.likeRotation(cloneState.get(oneCa), cloneState.get(nCa), d);
          transformModel(modClone, rotation);
          rotatedModels.add(modClone);
        }
      } catch (AtomException ae) {
        System.out.println("Somehow a Ca got lost in one of the models");
      }
    }
    return rotatedModels;
  }
  //}}}
  
  //{{{ deepClone
  public Model deepClone(Model mod, String name) {
    //Model clone = new Model("test" + name);
    Model clone = (Model) mod.clone();
    clone.setName(name);
    ModelState cloneState = clone.getState();
    //clone.setStates(cloneState);
    try {
      Iterator iter = mod.getResidues().iterator();
      while (iter.hasNext()) {
        Residue res = (Residue) iter.next();
        Residue resClone = new Residue(res, res.getChain(), res.getSegment(), res.getSequenceNumber(), res.getInsertionCode(), res.getName());
        resClone.cloneStates(res, mod.getState(), cloneState);
        clone.replace(res, resClone);
      }
    } catch (ResidueException ae) {
      System.out.println("Error when cloning: " + mod);
    } catch (AtomException ae) {
      System.out.println("AtomException when cloning: " + mod);
    }
    return clone;
  }
  //}}}
  
  //{{{ transformModel
  private void transformModel(Model mod, Transform trans) {
    ModelState modState = mod.getState();
    Iterator residues = (mod.getResidues()).iterator();
    while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    Iterator atoms = (res.getAtoms()).iterator();
	    while (atoms.hasNext()) {
        Atom at = (Atom) atoms.next();
        try {
          AtomState atState = modState.get(at);
          trans.transform(atState);
        } catch (AtomException ae) {
          System.out.println("atom state not found");
        }
	    }
      
    }
  }
  //}}}
  
  public Residue getOneResidue(Model mod) {
    Iterator iter = (mod.getResidues()).iterator();
    Residue zeroRes = null;
    while (iter.hasNext()) {
      Residue res = (Residue) iter.next();
      if (containsCa(res)) {
        if (zeroRes != null) return res;
        else zeroRes = res;
      }
    }
    return null;
  }
  
  public Residue getNResidue(Model mod) {
    Iterator iter = (mod.getResidues()).iterator();
    Residue prevRes = null;
    Residue res = null;
    while (iter.hasNext()) {
      //prevRes = res;
      Residue temp = (Residue) iter.next();
      if (containsCa(temp)) {
        prevRes = res;
        res = temp;
      }
    }
    return prevRes;
  }
  
  //{{{ containsCa
  /** checks to see if a residue has a CA */
  public static boolean containsCa(Residue res) {
    Iterator atoms = (res.getAtoms()).iterator();
    while (atoms.hasNext()) {
	    Atom at = (Atom) atoms.next();
	    String atomName = at.getName();
	    if (atomName.equals(" CA ")) return true;
    }                                                          
    return false;
  }
  //}}}
  
  //{{{ getCa
  public static Atom getCa(Residue res) {
    Iterator atoms = (res.getAtoms()).iterator();
    while (atoms.hasNext()) {
	    Atom at = (Atom) atoms.next();
	    String atomName = at.getName();
	    if (atomName.equals(" CA ")) return at;
    }                                                          
    return null;
  }
  //}}}
  
  //{{{ writePdbFile
  public void writePdbFile(CoordinateFile coordFile, File pdbOut) {
    //File pdbOut = new File("C:/docs/labwork/modeling/terwilliger/tripepTop500v2/tripepTop500_filtered-endmdl-ntermsup.pdb");
    //File pdbOut = new File(outFile);
    try {
	    PdbWriter writer = new PdbWriter(pdbOut);
	    writer.writeCoordinateFile(coordFile);
	    writer.close();
    } catch (IOException e) {
	    System.out.println("problem when writing file");
    }
  }
  //}}}
  
}
  
