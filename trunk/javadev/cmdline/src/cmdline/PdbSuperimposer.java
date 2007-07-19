// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.r3.*;
import driftwood.moldb2.*;
import java.io.*;
import java.util.*;
//}}}
/**
* PdbSuperimposer is for generating fragment libraries for Resolve.  It uses the 
* Ca, C, and O atoms of either the first residue or last residue to generate the
* libraries.  
*/
public class PdbSuperimposer {

  //{{{ main
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("This function takes 2 arguments, you need a source pdb and an output pdb!");
    } else {
      File pdbIn = new File(args[0]);
      File pdbOut = new File(args[1]);
      PdbSuperimposer imposer = new PdbSuperimposer(pdbIn.getAbsolutePath(), pdbOut.getAbsolutePath());
    }
  }
  //}}}
  
  //{{{ Constructor
  public PdbSuperimposer(String inPdb, String outFile) {
    //System.out.println(inPdb + " " + outFile);
    try {
	    System.out.println("reading in file");
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
      File pdb = new File(inPdb);
	    CoordinateFile coordFile = reader.read(pdb);
	    System.out.println("file has been read");
      CoordinateFile cleanFile = superimposeModels(coordFile);
	    //System.out.println("builder built");
	    writePdbFile(cleanFile, outFile);
    }
    catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
    }
  }
  //}}}
  
  //{{{ superimposeModels
  public CoordinateFile superimposeModels(CoordinateFile coordFile) {
    Builder built = new Builder();
    AtomState modCA = null;
    AtomState modC = null;
    AtomState modO = null;
    CoordinateFile cleanFile = new CoordinateFile();
    Triple refCA = new Triple(0, 0, 0);
    Triple refC = new Triple(1.5, 0, 0);
    Triple refO = new Triple(2.15, 1, 0);
    Iterator models = (coordFile.getModels()).iterator();
    while (models.hasNext()) {
      Model mod = (Model) models.next();
      if (PdbSuperimposer.isBackboneComplete(mod)) {
        cleanFile.add(mod);
        //System.out.println("model written");
        ModelState modState = mod.getState();
        Residue modRes = getFirstResidue(mod);
        //System.out.println(modState + " " +  modRes);
        try {
          modCA = modState.get(modRes.getAtom(" CA "));
          //System.out.println(modCA);
          modC = modState.get(modRes.getAtom(" C  "));
          modO = modState.get(modRes.getAtom(" O  "));
          //System.out.println(refCA.toString() + refC.toString() + refO.toString());
          Transform dock3point = built.dock3on3(refCA, refC, refO, modCA, modC, modO);
          transformModel(mod, dock3point);
        } catch (AtomException ae) {
          System.out.println("a mod atom wasn't found");
        }
      }
    }
    return cleanFile;
  }
  //}}}
  
  //{{{ getResidue functions
  private Residue getLastResidue(Model mod) {
    Iterator residues = (mod.getResidues()).iterator();
    while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    if (res.getSequenceInteger() == 3) {
        return res;
	    }
    }
    return null;
  }
  
  private Residue getFirstResidue(Model mod) {
    Iterator residues = (mod.getResidues()).iterator();
    while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    if (res.getSequenceInteger() == 1) {
        return res;
	    }
    }
    return null;
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
  
  //{{{ isBackboneComplete(Model)
  /** 
  * checks to see if a Model has residues with the right atoms (N, CA, C, O) 
  * and to see if the model has only 3 residues.  
  */
  public static boolean isBackboneComplete(Model mod) {
    ModelState modState = mod.getState();
    Iterator residues = (mod.getResidues()).iterator();
    //boolean complete = true;
    Residue tempRes = null;
    int resCount = 0;
    while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    if (!PdbSuperimposer.isBackboneComplete(res)) {
        try { 
          AtomState notComp = modState.get(res.getAtom(" N  "));
          System.out.println(notComp);
          //return false;
        } catch (AtomException ae) {
        }
        return false;
	    }
	    resCount++;
	    tempRes = res;
    }
    if (resCount < 3) {
	    try {
        AtomState modCA = modState.get(tempRes.getAtom(" CA "));
        System.out.println("Model " + modCA + " has " + resCount + " residues");
	    } catch (AtomException ae) {
	    }
	    return false;
    }
    return true;
  }
  //}}}
  
  //{{{ isBackboneComplete(Residue)
  /** tries to check if a residue has the correct number and type of atoms */
  public static boolean isBackboneComplete(Residue res) {
    Iterator atoms = (res.getAtoms()).iterator();
    int atomTotal = 0x00000000;
    while (atoms.hasNext()) {
	    Atom at = (Atom) atoms.next();
	    String atomName = at.getName();
	    if (atomName.equals(" N  ")) atomTotal = atomTotal + 0x00000001;
	    if (atomName.equals(" CA ")) atomTotal = atomTotal + 0x00000002;
	    if (atomName.equals(" C  ")) atomTotal = atomTotal + 0x00000004;
	    if (atomName.equals(" O  ")) atomTotal = atomTotal + 0x00000008;
    }
    if (atomTotal == 15) {
	    return true;
    } else {
	    System.out.println("Residue: " + res + " not complete");
	    return false;
    }
  }
  //}}}
  
  //{{{ writePdbFile
  public void writePdbFile(CoordinateFile coodFile, String outFile) {
    //File pdbOut = new File("C:/docs/labwork/modeling/terwilliger/tripepTop500v2/tripepTop500_filtered-endmdl-ntermsup.pdb");
    File pdbOut = new File(outFile);
    try {
	    PdbWriter writer = new PdbWriter(pdbOut);
	    writer.writeCoordinateFile(coodFile);
	    writer.close();
    } catch (IOException e) {
	    System.out.println("problem when writing file");
    }
  }
  //}}}
  
}

    
