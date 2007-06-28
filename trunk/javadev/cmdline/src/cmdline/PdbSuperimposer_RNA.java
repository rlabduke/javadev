// (jEdit options) :folding=explicit:collapseFolds=1:
// modified by Bob and Jeff 6/28/07
// presented as is
package cmdline;

import driftwood.r3.*;
import driftwood.moldb2.*;
import java.io.*;
import java.util.*;

public class PdbSuperimposer_RNA {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("This function takes 2 arguments, you need a source pdb and an output pdb!");
    } else {
      File pdbIn = new File(args[0]);
      File pdbOut = new File(args[1]);
      PdbSuperimposer_RNA imposer = new PdbSuperimposer_RNA(pdbIn.getAbsolutePath(), pdbOut.getAbsolutePath());
    }
  }
  
  public PdbSuperimposer_RNA(String inPdb, String outFile) {
    //System.out.println(inPdb + " " + outFile);
    Builder built = new Builder();
    try {
	    AtomState modP = null;
	    AtomState modO3 = null;
	    AtomState modO5 = null;
	    System.out.println("reading in file");
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
	    //File pdb = new File("C:/docs/labwork/modeling/terwilliger/tripepTop500v2/tripepTop500_filtered-endmdl.pdb");
	    //File pdb = new File("C:/docs/labwork/modeling/terwilleger/tripepTop500_pdblib2/test1.pdb");
      File pdb = new File(inPdb);
	    CoordinateFile coodFile = reader.read(pdb);
	    CoordinateFile cleanFile = new CoordinateFile();
	    System.out.println("file has been read");
	    Triple refP = new Triple(0, 0, 0);
	    Triple refO3 = new Triple(1.6, 0, 0);
	    Triple refO5 = new Triple(0, 1.6, 0);
	    Iterator models = (coodFile.getModels()).iterator();
	    while (models.hasNext()) {
        Model mod = (Model) models.next();
        //if (PdbSuperimposer_RNA.isBackboneComplete(mod)) {
          cleanFile.add(mod);
          //System.out.println("model written");
          ModelState modState = mod.getState();
          Residue modRes = getFirstResidue(mod);
	  Residue mod2Res = getSecondResidue(mod);
          //System.out.println(modState + " " +  modRes);
          try {
            modP = modState.get(mod2Res.getAtom(" P  "));
            //System.out.println(modP);
            modO3 = modState.get(modRes.getAtom(" O3*"));
            modO5 = modState.get(mod2Res.getAtom(" O5*"));
            //System.out.println(refP.toString() + refO3.toString() + refO5.toString());
            Transform dock3point = built.dock3on3(refP, refO5, refO3, modP, modO5, modO3);
            transformModel(mod, dock3point);
            //Iterator atoms = modRes
            //dock3point.transform(>>>>>>);
          } catch (AtomException ae) {
            System.out.println("a mod atom wasn't found");
          }
        //}
	    }
	    //System.out.println("builder built");
	    writePdbFile(cleanFile, outFile);
    }
    catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
    }
  }
  
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
	    if (res.getSequenceInteger() == 2) {
        return res;
	    }
    }
    return null;
  }
  
  private Residue getSecondResidue(Model mod) {
    Iterator residues = (mod.getResidues()).iterator();
    while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    if (res.getSequenceInteger() == 3) {
        return res;
	    }
    }
    return null;
  }

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
  
  public static boolean isBackboneComplete(Model mod) {
    ModelState modState = mod.getState();
    Iterator residues = (mod.getResidues()).iterator();
    //boolean complete = true;
    Residue tempRes = null;
    int resCount = 0;
    while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    if (!PdbSuperimposer_RNA.isBackboneComplete(res)) {
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
        AtomState modP = modState.get(tempRes.getAtom(" CA "));
        System.out.println("Model " + modP + " has " + resCount + " residues");
	    } catch (AtomException ae) {
	    }
	    return false;
    }
    return true;
  }
  
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
  
}

    
