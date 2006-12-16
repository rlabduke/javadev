package cmdline;

import driftwood.r3.*;
import driftwood.moldb2.*;
import java.io.*;
import java.util.*;

public class LibraryFilterer {

    public static void main(String[] args) {
	//LibraryFilterer filterer = new LibraryFilterer();
	long startTime = System.currentTimeMillis();
	if (args.length == 0) {
	    System.out.println("No pdb files were specified!");
	} else {
	    File[] inputs = new File[args.length];
	    for (int i = 0; i < args.length; i++) {
		inputs[i] = new File(System.getProperty("user.dir") + "/" + args[i]);
		System.out.println(inputs[i]);
	    }
	    File outFile = new File(System.getProperty("user.dir") + "/" + args[0] + ".rmsd.pdb");
	    LibraryFilterer filterer = new LibraryFilterer(inputs, outFile);
	}

	long endTime = System.currentTimeMillis();
	System.out.println((endTime - startTime)/1000 + " seconds to generate library");
    }
    
    public LibraryFilterer(File[] files, File outFile) {
	CoordinateFile cleanFile = new CoordinateFile();
	for (int i = 0; i < files.length; i++) {
	    System.out.println("Reading new file");
	    filterFileByRMSD(files[i], cleanFile);
	}
	writePdbFile(cleanFile, outFile);

    }

    public void filterFileByDist(File pdb, CoordinateFile cleanFile) {
	try {
	    PdbReader reader = new PdbReader();
	    //File pdb = new File("C:/docs/labwork/modeling/terwilleger/tripepTop500_pdblib2/tripepTop500_1superNterm.pdb");
	    CoordinateFile coodFile = reader.read(pdb);
	    //CoordinateFile cleanFile = new CoordinateFile();
	    Iterator models = (coodFile.getModels()).iterator();
	    while (models.hasNext()) {
		Model mod = (Model) models.next();
		ModelState modState = mod.getState();
		Residue thirdRes = getThirdResidue(mod);
		try {
		    AtomState modC = modState.get(thirdRes.getAtom(" C  "));
		    Iterator cleanIter = (cleanFile.getModels()).iterator();
		    double dist = 1;
		    while ((cleanIter.hasNext())&&(dist > 0.25)) {
			Model cleanMod = (Model) cleanIter.next();
			AtomState cleanC = getThirdCarb(cleanMod);
			dist = modC.distance(cleanC);
		    }
		    if (dist > 0.25) {
			cleanFile.add(mod);
			System.out.print(".");
		    }
		} catch (AtomException ae) {
		    System.out.println("a mod atom wasn't found");
		}
		
	    }
	    //writePdbFile(cleanFile);
	}
	catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
	}
    }

    public void filterFileByRMSD(File pdb, CoordinateFile cleanFile) {
	try {
	    PdbReader reader = new PdbReader();
	    //File pdb = new File("C:/docs/labwork/modeling/terwilleger/tripepTop500_pdblib2/tripepTop500_1superNterm.pdb");
	    CoordinateFile coodFile = reader.read(pdb);
	    //CoordinateFile cleanFile = new CoordinateFile();
	    Iterator models = (coodFile.getModels()).iterator();
	    while (models.hasNext()) {
		Model mod = (Model) models.next();
		//ModelState modState = mod.getState();
		//Residue thirdRes = getThirdResidue(mod);
		//try {
		//    AtomState modC = modState.get(thirdRes.getAtom(" C  "));
		Iterator cleanIter = (cleanFile.getModels()).iterator();
		    //    double dist = 1;
		double rmsd = 1;
		while ((cleanIter.hasNext())&&(rmsd > 0.55)) {
		    Model cleanMod = (Model) cleanIter.next();
			//AtomState cleanC = getThirdCarb(cleanMod);
		    rmsd = calcBackboneRMSD(mod, cleanMod);
			//dist = modC.distance(cleanC);
		}
		if (rmsd > 0.55) {
		    cleanFile.add(mod);
		    System.out.print(".");
		}
		    //} catch (AtomException ae) {
	    //    System.out.println("a mod atom wasn't found");
	    //	}
		
	    }
	    //writePdbFile(cleanFile);
	}
	catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
	}
    }

    public double calcBackboneRMSD(Model ref, Model mod) {
	//get residues, get atoms, use all except CB
	ModelState refState = ref.getState();
	ModelState modState = mod.getState();
	ArrayList refAtoms = getBackboneAtoms(ref);
	ArrayList modAtoms = getBackboneAtoms(mod);
	if (refAtoms.size()==modAtoms.size()) {
	    double sum = 0;
	    for (int i = 0; i < refAtoms.size(); i++) {
		try {
		    AtomState refpoint = refState.get((Atom)refAtoms.get(i));
		    AtomState modpoint = modState.get((Atom)modAtoms.get(i));
		    sum = sum + Math.sqrt((Math.pow((refpoint.getX() - modpoint.getX()), 2) + Math.pow((refpoint.getY() - modpoint.getY()), 2) + Math.pow((refpoint.getZ() - modpoint.getZ()), 2)));
		} catch (AtomException ae) {
		    System.err.println("Somehow an Atomstate wasn't found.");
		}
	    }
	    double rmsd = Math.sqrt(sum/refAtoms.size());
	    return rmsd;
	}
	return -1;
    }

    public ArrayList getBackboneAtoms(Model ref) {
	Iterator refResidues = (ref.getResidues()).iterator();
	ArrayList refAtoms = new ArrayList();
	while (refResidues.hasNext()) {
	    Residue refRes = (Residue) refResidues.next();
	    if (refRes.getSequenceInteger() < 4) {
		Iterator atoms = (refRes.getAtoms()).iterator();
		while (atoms.hasNext()) {
		    Atom at = (Atom) atoms.next();
		    if (!at.getName().equals(" CB ")) {
			refAtoms.add(at);
		    }
		}
	    }
	}
	return refAtoms;
    }
    
    private Residue getThirdResidue(Model mod) {
	Iterator residues = (mod.getResidues()).iterator();
	while (residues.hasNext()) {
	    Residue res = (Residue) residues.next();
	    if (res.getSequenceInteger() == 3) {
		return res;
	    }
	}
	return null;
    }

    private AtomState getThirdCarb(Model mod) {
	ModelState modState = mod.getState();
	Residue thirdRes = getThirdResidue(mod);
	try {
	    return modState.get(thirdRes.getAtom(" C  "));
	} catch (AtomException ae) {
	    System.out.println("a clean third atom wasn't found");
	}
	return null;  //shouldn't ever happen, cause all clean 
	              //have already had C's found.
    }

    public void writePdbFile(CoordinateFile coodFile, File outFile) {
	//File pdbOut = new File("C:/docs/labwork/modeling/terwilliger/tripepTop500_pdblib2/tripepTop500_rmsdfilter.pdb");
	try {
	    PdbWriter writer = new PdbWriter(outFile);
	    writer.writeCoordinateFile(coodFile);
	    writer.close();
	} catch (IOException e) {
	    System.out.println("problem when writing file");
	}
    }
}
