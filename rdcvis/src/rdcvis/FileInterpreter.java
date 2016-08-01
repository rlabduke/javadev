// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package rdcvis;

import king.core.*;
import driftwood.moldb2.*;
import driftwood.util.*;
import driftwood.r3.*;
import Jama.*;

import java.util.*;
//}}}

public class FileInterpreter {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  CoordinateFile pdb;
  MagneticResonanceFile mrf;
  
  RdcDrawer2           drawer;
  RdcSolver           solver;
  TreeMap             currentRdcs;
  HashMap             transAtomMap;


  //}}}
  
  //{{{ Constructors
  public FileInterpreter(CoordinateFile pdbfile, MagneticResonanceFile mrfile) {
    pdb = pdbfile;
    mrf = mrfile;
    transAtomMap = new HashMap();
    transAtomMap.put("HN", "H");
  }
  //}}}
  
  //{{{ solveRdcsSingleModel
  public void solveRdcsSingleModel(String rdcType, String modelId) {

    currentRdcs = mrf.getRdcMapforType(rdcType);
    if (currentRdcs != null) {
      ArrayList atomVects = new ArrayList();
      ArrayList rdcValues = new ArrayList();
      Iterator models = (pdb.getModels()).iterator();
      while (models.hasNext()) {
        Model mod = (Model) models.next();
        if (mod.getName().equals(modelId)) {
          ModelState state = mod.getState();
          HashMap lists = makeLists(mod, state, currentRdcs, rdcType);
          ArrayList modAtoms = (ArrayList) lists.keySet().iterator().next();
          ArrayList modRdcs = (ArrayList) lists.get(modAtoms);
          atomVects.addAll(modAtoms);
          rdcValues.addAll(modRdcs);
        }
      }
      solver = makeSolver(atomVects, rdcValues);
      if (solver != null) {
        drawer = new RdcDrawer2(solver.getSaupeDiagonalized(), solver.getSaupeEigenvectors());
      }
    } else {
      System.err.println("Unknown RDC type entered!");
    }
  }
  //}}}
  
  //{{{ solveRdcsEnsemble
  public void solveRdcsEnsemble(String rdcType) {
    currentRdcs = mrf.getRdcMapforType(rdcType);
    if (currentRdcs != null) {
      ArrayList atomVects = new ArrayList();
      ArrayList rdcValues = new ArrayList();
      Iterator models = (pdb.getModels()).iterator();
      while (models.hasNext()) {
        //System.out.print(".");
        Model mod = (Model) models.next();
        ModelState state = mod.getState();
        HashMap lists = makeLists(mod, state, currentRdcs, rdcType);
        ArrayList modAtoms = (ArrayList) lists.keySet().iterator().next();
        ArrayList modRdcs = (ArrayList) lists.get(modAtoms);
        atomVects.addAll(modAtoms);
        rdcValues.addAll(modRdcs);
      }
      solver = makeSolver(atomVects, rdcValues);
      if (solver != null) {
        drawer = new RdcDrawer2(solver.getSaupeDiagonalized(), solver.getSaupeEigenvectors());
      }
    } else {
      System.err.println("Unknown RDC type entered!");
    }
  }
  //}}}
  
  //{{{ makeLists
  public HashMap makeLists(Model mod, ModelState state, TreeMap rdcs, String rdcType) {
    Iterator residues = mod.getResidues().iterator();
    ArrayList atomVects = new ArrayList();
    ArrayList rdcValues = new ArrayList();

    while (residues.hasNext()) {
      Residue res = (Residue) residues.next();
      String seq = res.getSequenceNumber().trim();
      if (rdcs.containsKey(seq)) {      
        DipolarRestraint dr = (DipolarRestraint) rdcs.get(seq);
        String[] atoms = parseAtomNames(rdcType, dr);
        Atom from = null;
        Atom to = null;
        if (dr.isInOneResidue()) {
          //System.out.println(atoms[0]);
          //System.out.println(atoms[1]);
          from = res.getAtom(atoms[0]);
          to = res.getAtom(atoms[1]);
        } else {
          
          from = res.getAtom(pdbifyAtomName(dr.getFromName())); // the current res should correspond to the 'from' res in the DipolarRestraint (see MagResFile
          Residue toRes = res.getNext(mod);
          if (!toRes.getSequenceNumber().trim().equals(dr.getToNum())) {
            toRes = res.getPrev(mod);
          }
          //String toCNIT = res.getCNIT().replaceFirst(res.getSequenceNumber(), Strings.justifyRight(dr.getToNum(), 4));
          //System.out.println("|"+res.getCNIT()+"|");
          //System.out.println("|"+toCNIT+"|");
          //Residue origResTest = mod.getResidue(res.getCNIT());
          //System.out.println(origResTest+" from "+res.getCNIT());
          //Residue toRes = mod.getResidue(toCNIT);
          //System.out.println(toRes+" from "+toCNIT);
          if (toRes != null && toRes.getSequenceNumber().trim().equals(dr.getToNum())) {
            to = toRes.getAtom(pdbifyAtomName(dr.getToName()));
          } else {
            System.err.println("Residue "+ dr.getToNum() +" not found!");
          }
        }
        //System.out.println(from+" "+to);
        try {
          AtomState fromState = state.get(from);
          AtomState toState = state.get(to);
          //DipolarRestraint dr = (DipolarRestraint) rdcs.get(seq);
          Triple vect = new Triple().likeVector(fromState, toState).unit();
          //System.out.println(vect);
          atomVects.add(vect);
          rdcValues.add(rdcs.get(seq));
          //System.out.println(seq + " " + dr.getValues()[0]);
        } catch (AtomException ae) {
          System.out.println(ae + " thrown, either "+atoms[0]+" or "+atoms[1]+" is missing from residues "+dr.getFromNum()+" or "+dr.getToNum());
        }
      }
    }
    HashMap atomsToRdcs = new HashMap();
    atomsToRdcs.put(atomVects, rdcValues);
    return atomsToRdcs;
  }
  //}}}
  
  //{{{ makeSolver
  public RdcSolver makeSolver(ArrayList atomVects, ArrayList rdcValues) {
    // make matrices
    if (atomVects.size() != rdcValues.size()) {
      System.out.println("atomVects and rdcValues not same size, must be same for SVD calc");
    } else if (atomVects.size() < 5) {
      System.out.println("not enough rdcs or vectors for SVD calc, must be at least 5");
    } else {
      Matrix atomMatrix = new Matrix(3, atomVects.size());
      Matrix rdcMatrix = new Matrix(1, atomVects.size());
      for (int i = 0; i < atomVects.size(); i++) {
        Triple vect = (Triple) atomVects.get(i);
        DipolarRestraint dr = (DipolarRestraint) rdcValues.get(i);
        atomMatrix.set(0, i, vect.getX());
        atomMatrix.set(1, i, vect.getY());
        atomMatrix.set(2, i, vect.getZ());
        rdcMatrix.set(0, i, dr.getValues()[0]);
      }
      return new RdcSolver(atomMatrix, rdcMatrix);
    }
    return null;
  }
  //}}}
  
  //{{{ parseAtomNames
  /** 
  * Parses atom names out of RDC type selection.  
  * If one of the atoms has an H, it will switch the order so
  * that atom is returned second, so vector points toward H.
  **/
  public String[] parseAtomNames(String rdcType, DipolarRestraint dr) {
    if (dr.isInOneResidue()) {
      String[] atoms = Strings.explode(rdcType, '-');
      if (atoms[0].indexOf("H") > -1) {
        String temp = atoms[0];
        atoms[0] = atoms[1];
        atoms[1] = temp;
      }
      for (int i = 0; i < atoms.length; i++) {
        String atom = atoms[i];
        if (transAtomMap.containsKey(atom)) {
          atom = (String) transAtomMap.get(atom);
        }
        atoms[i] = pdbifyAtomName(atom);
      }
      return atoms;
    } else {
      String[] atoms = new String[2];
      atoms[0] = pdbifyAtomName(dr.getFromName());
      atoms[1] = pdbifyAtomName(dr.getToName());
      return atoms;
    }
  }
  //}}}
  
  //{{{ pdbifyAtomName
  public static String pdbifyAtomName(String atom) {
    if ((atom.length() == 1)||(atom.length() == 2)) {
      return Strings.justifyCenter(atom, 4);
    } else if (atom.length() == 3) {
      if (isNumeric(atom.substring(0,1))) {
        return Strings.justifyLeft(atom, 4);
      } else {
        return Strings.justifyRight(atom, 4);
      }
    } else {
      return Strings.justifyLeft(atom, 4);
    }
  }
  //}}}
  
  //{{{ get functions
  public RdcDrawer2 getDrawer() {
    return drawer;
  }
  
  public DipolarRestraint getRdc(Residue res) {
    DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(res.getSequenceNumber().trim());
    return dr;
  }
  
  public double getRdcValue(String seqNum) {
    //System.out.println(seqNum);
    DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seqNum);
    if (dr != null) {
      //System.out.println(dr);
      double rdcVal = dr.getValues()[0];
      return rdcVal;
    }
    return Double.NaN;
  }
  
  public double getRdcError(String seqNum) {
    DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seqNum);
    if (dr != null) {
      double error = dr.getValues()[1];
      if (error == 0.0) return 1;
      return error;
    }
    return Double.NaN;
  }
  
  // hmm, this function should return the same value as input...this seems odd
  public String getRdcFromNum(String seqNum) {
    DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seqNum);
    if (dr != null) {
      return dr.getFromNum();
    }
    return null;
  }
  
  public String getRdcToNum(String seqNum) {
    DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seqNum);
    if (dr != null) {
      return dr.getToNum();
    }
    return null;
  }
  
  public double getBackcalcRdc(Triple vector) {
    return solver.backCalculateRdc(vector);
  }
  
  public CoordinateFile getPdb() {
    return pdb;
  }
  
  /** expects that one of the solve functions have been run already */
  public Residue[] getFromToResidue(Model mod, Residue res) {
    String seq = res.getSequenceNumber().trim();
    Residue[] rezzes = new Residue[2];
    rezzes[0] = res;
    if (currentRdcs.containsKey(seq)) {      
      DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seq);
      if (dr.isInOneResidue()) {
        rezzes[1] = res;
      } else {
        rezzes[1] = res.getNext(mod);
        if (!rezzes[1].getSequenceNumber().trim().equals(dr.getToNum())) {
          rezzes[1] = res.getPrev(mod);
        }
        if (!rezzes[1].getSequenceNumber().trim().equals(dr.getToNum())) return null;
      }
    } else {
      return null;
    }
    return rezzes;
  }
  //}}}
  
  //{{{ isNumeric
  public static boolean isNumeric(String s) {
    try {
	    Double.parseDouble(s);
	    return true;
    } catch (NumberFormatException e) {
	    return false;
    }
  }
  //}}}
  
}
