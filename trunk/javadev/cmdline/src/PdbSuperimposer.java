package cmdline;

import driftwood.r3.*;
import driftwood.moldb2.*;
import java.io.*;
import java.util.*;

public class PdbSuperimposer {

    public static void main(String[] args) {
	PdbSuperimposer imposer = new PdbSuperimposer();
    }

    public PdbSuperimposer() {
	Builder built = new Builder();
	try {
	    AtomState modCA = null;
	    AtomState modC = null;
	    AtomState modO = null;
	    System.out.println("reading in file");
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
	    File pdb = new File("C:/docs/labwork/modeling/terwilleger/tripepTop500_pdblib2/tripepTop500_4endmdl.pdb");
	    //File pdb = new File("C:/docs/labwork/modeling/terwilleger/tripepTop500_pdblib2/test1.pdb");
	    CoordinateFile coodFile = reader.read(pdb);
	    CoordinateFile cleanFile = new CoordinateFile();
	    System.out.println("file has been read");
	    /*
	    Model first = coodFile.getFirstModel();
	    cleanFile.add(first);
	    ModelState firstState = first.getState();
	    Residue refRes = getLastResidue(first);
	    try {
		refCA = firstState.get(refRes.getAtom(" CA "));
		refC = firstState.get(refRes.getAtom(" C  "));
		refO = firstState.get(refRes.getAtom(" O  "));
		System.out.println(refCA.toString() + refC.toString() + refO.toString());
	    } catch (AtomException ae) {
		System.out.println("a ref state wasn't found");
	    }
	    */
	    Triple refCA = new Triple(0, 0, 0);
	    Triple refC = new Triple(1.5, 0, 0);
	    Triple refO = new Triple(2.15, 1, 0);
	    Iterator models = (coodFile.getModels()).iterator();
	    while (models.hasNext()) {
		Model mod = (Model) models.next();
		if (PdbSuperimposer.isBackboneComplete(mod)) {
		    cleanFile.add(mod);
		    //System.out.println("model written");
		    ModelState modState = mod.getState();
		    Residue modRes = getFirstResidue(mod);
		    try {
			modCA = modState.get(modRes.getAtom(" CA "));
			modC = modState.get(modRes.getAtom(" C  "));
			modO = modState.get(modRes.getAtom(" O  "));
			//System.out.println(refCA.toString() + refC.toString() + refO.toString());
			Transform dock3point = built.dock3on3(refCA, refC, refO, modCA, modC, modO);
			transformModel(mod, dock3point);
			//Iterator atoms = modRes
			//dock3point.transform(>>>>>>);
		    } catch (AtomException ae) {
			System.out.println("a mod atom wasn't found");
		    }
		}
	    }
	    //System.out.println("builder built");
	    writePdbFile(cleanFile);
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
	    if (res.getSequenceInteger() == 1) {
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
	while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    if (!PdbSuperimposer.isBackboneComplete(res)) return false;
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
	    System.out.println("Residue: " + res.getName() + " not complete");
	    return false;
	}
		
    }

    public void writePdbFile(CoordinateFile coodFile) {
	File pdbOut = new File("C:/docs/labwork/modeling/terwilleger/tripepTop500_pdblib2/tripepTop500_4superNterm.pdb");
	try {
	    PdbWriter writer = new PdbWriter(pdbOut);
	    writer.writeCoordinateFile(coodFile);
	    writer.close();
	} catch (IOException e) {
	    System.out.println("problem when writing file");
	}
    }

}

    
