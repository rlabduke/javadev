// (jEdit options) :folding=explicit:collapseFolds=1:
package cmdline.fragment;

import driftwood.moldb2.*;
import driftwood.r3.*;

import java.util.*;
import java.io.*;
import java.text.*;

public class FragmentLibraryCreator {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  ArrayList<File> pdbs;
  //}}}

  //{{{ main
  public static void main(String[] args) {
    long startTime = System.currentTimeMillis();
    if (args.length < 2) {
	    System.out.println("Not enough arguments, you need one directory and outfile!");
    } else if (args.length > 2) {
      System.out.println("Too many arguments, you need one directory and outfile!");
    } else {
      File pdbsDirectory = new File(args[0]);
      pdbsDirectory = pdbsDirectory.getAbsoluteFile();
      File[] files = pdbsDirectory.listFiles();
      //System.out.println(inputs[i]);
    
	    //File resAnalysisDirectory = new File(args[1]);
      //File[] resAnalysisFiles = resAnalysisDirectory.listFiles();
	    FragmentLibraryCreator filterer = new FragmentLibraryCreator(files);
      String lib = filterer.createLibrary(2);
      //System.out.println(lib);
      File saveFile = new File(args[1]);
      saveFile = saveFile.getAbsoluteFile();
      System.out.println(saveFile.getAbsolutePath());
      //System.out.println(saveFile.getCanonicalPath());
      filterer.write(saveFile, lib);
    }
    
    long endTime = System.currentTimeMillis();
    System.out.println((endTime - startTime)/1000 + " seconds to generate library");
  }
  //}}}
  
  //{{{ Constructor
  public FragmentLibraryCreator(File[] files) {
    pdbs = new ArrayList<File>();
    for (File f : files) {
      String name = f.getName();
      System.out.println(name);
      if (name.endsWith(".pdb")) {
        pdbs.add(f);
      }
    }
  }
  //}}}
  
  //{{{ readFile
  public CoordinateFile readFile(File f) {
    try {
	    //System.out.println("reading in file");
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
      //File pdb = new File(f);
	    CoordinateFile coordFile = reader.read(f);
	    System.out.println(coordFile.getIdCode()+" has been read");
      return coordFile;
    }
    catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
    }
    return null;
  }
  //}}}
    
  //{{{ createLibrary
  /** only uses first model of each pdb file **/
  public String createLibrary(int fraglength) {
    String libParams = "";
    for (File f : pdbs) {
      CoordinateFile pdb = readFile(f);
      if (pdb == null) {
        System.err.println("Somehow a file wasn't readable");
      } else {
        Model first = pdb.getFirstModel();
        Set<String> chains = first.getChainIDs();
        for (String cid : chains) {
          System.out.println("Chain -" + cid + "-");
          Set<Residue> residues = first.getChain(cid);
          if (libParams.equals(""))  libParams = fragalyze(pdb.getIdCode(), first, residues, fraglength);
          else                       libParams = libParams+"\n"+fragalyze(pdb.getIdCode(), first, residues, fraglength);
          //System.out.println(libParams);
        }
      }
      libParams = libParams.trim(); // to get rid of spaces in between pdb file parameters
    }
    return libParams;
  }
  //}}}

  //{{{ fragalyze
  public String fragalyze(String pdbId, Model mod, Set<Residue> residues, int size) {
    ArrayList<Residue> currFrag = new ArrayList();
    //double maxBfactor = 0;
    String params = "";
    for (Residue res : residues) {
      //System.out.println(res.getCNIT());
      if (currFrag.size() != size + 3) {
        if (isBackboneComplete(res)&&(res.getInsertionCode().equals(" "))) {
          currFrag.add(res);
        } else {
          //maxBfactor = 0;
          currFrag.clear(); // so none of the fragments have incomplete residues.
        }
      }
      if (currFrag.size() == size + 3) {
        String currParams = parameterize(mod, currFrag, size);
        currParams = pdbId+":"+res.getChain()+":"+currParams;
        //System.out.println(params);
        if (params.equals(""))  params = currParams;
        else                    params = params+"\n"+currParams;
        currFrag.remove(0);
      }
    }
    return params;
  }
  //}}}
  
  //{{{ parameterize
  public String parameterize(Model mod, ArrayList<Residue> frag, int size) {
    Residue zeroRes = frag.get(0);
    Residue oneRes = frag.get(1);
    Residue twoRes = frag.get(2);
    Residue mRes = frag.get(frag.size()-3);
    Residue n1Res = frag.get(frag.size()-1);
    Residue nRes = frag.get(frag.size()-2);
    ModelState modState = mod.getState();
    String params = "";
    try {
      AtomState ca0 = modState.get(zeroRes.getAtom(" CA "));
      AtomState ca1 = modState.get(oneRes.getAtom(" CA "));
      AtomState caN = modState.get(nRes.getAtom(" CA "));
      AtomState caN1 = modState.get(n1Res.getAtom(" CA "));
      AtomState co0 = modState.get(zeroRes.getAtom(" O  "));
      AtomState coN = modState.get(nRes.getAtom(" O  "));
      double[] parameters = frameAnalyze(ca0, ca1, caN, caN1, co0, coN);
      //return parameters;
      params = params.concat(String.valueOf(size)+":"+zeroRes.getSequenceNumber().trim()+":");
      for (double d : parameters) { params = params.concat(df.format(d)+":"); }
      AtomState ca2 = modState.get(twoRes.getAtom(" CA "));
      AtomState co1 = modState.get(oneRes.getAtom(" O  "));
      AtomState caM = modState.get(mRes.getAtom(" CA "));
      AtomState coM = modState.get(mRes.getAtom(" O  "));
      double[] firstPair = pairAnalyze(co0, ca0, ca1, ca2, co1);
      for (double d : firstPair) { params = params.concat(df.format(d)+":"); }
      double[] lastPair = pairAnalyze(coM, caM, caN, caN1, coN);
      for (double d : lastPair) { params = params.concat(df.format(d)+":"); }
      params = params.concat(df.format(getMaxBackboneBfactor(mod, frag)));
    } catch (AtomException ae) {
      System.err.println("Problem with atom " + ae.getMessage());
    }
    return params;
  }
  //}}}
  
  //{{{ pairAnalyze
  public double[] pairAnalyze(Triple coFirst, Triple caFirst, Triple caTwo, Triple caThree, Triple coTwo) {
    double[] params = new double[3];
    params[0] = Triple.angle(caFirst, caTwo, caThree);
    params[1] = Triple.dihedral(coFirst, caFirst, caTwo, caThree);
    params[2] = Triple.dihedral(caFirst, caTwo, caThree, coTwo);
    return params;
  }
  //}}}
  
  //{{{ frameAnalyze
  public double[] frameAnalyze(Triple tripca0, Triple tripca1, Triple tripcaN, Triple tripcaN1, Triple tripco0, Triple tripcoN) {
    double[] params = new double[6];
    params[0] = tripca1.distance(tripcaN);
    params[1] = Triple.angle(tripca0, tripca1, tripcaN);
    params[2] = Triple.angle(tripca1, tripcaN, tripcaN1);
    params[3] = Triple.dihedral(tripco0, tripca0, tripca1, tripcaN);
    params[4] = Triple.dihedral(tripca0, tripca1, tripcaN, tripcaN1);
    params[5] = Triple.dihedral(tripca1, tripcaN, tripcaN1, tripcoN);
    //System.out.print(df.format(params[0]) + " ");
    //System.out.print(df.format(params[1]) + " ");
    //System.out.print(df.format(params[2]) + " ");
    //System.out.print(df.format(params[3]) + " ");
    //System.out.print(df.format(params[4]) + " ");	
    //System.out.println(df.format(params[5]));
    return params;
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
	    //System.out.println("Residue: " + res + " not complete");
	    return false;
    }
  }
  //}}}
  
  //{{{ getMaxBackboneBfactor
  public double getMaxBackboneBfactor(Model mod, ArrayList<Residue> residues) {
    double maxBfact = 0;
    for (Residue res : residues) {
      maxBfact = Math.max(maxBfact, getMaxBackboneBfactor(mod, res));
    }
    return maxBfact;
  }
  //}}}
  
  //{{{ getMaxBackboneBfactor
  public double getMaxBackboneBfactor(Model mod, Residue res) {
    ModelState modState = mod.getState();
    try {
      AtomState n  = modState.get(res.getAtom(" N  "));
      AtomState ca = modState.get(res.getAtom(" CA "));
      AtomState c  = modState.get(res.getAtom(" C  "));
      AtomState o  = modState.get(res.getAtom(" O  "));
      return Math.max(n.getTempFactor(), Math.max(ca.getTempFactor(), Math.max(c.getTempFactor(), o.getTempFactor())));
    } catch (AtomException ae) {
      System.err.println("Problem with atom " + ae.getMessage());
    }
    return Double.NaN;
  }
  //}}}
  
  //{{{ write
  public void write(File saveFile, String text) {
    try {
      System.out.println(saveFile.getCanonicalPath());
      if (!saveFile.exists()) {
        saveFile.createNewFile();
      }
      Writer w = new FileWriter(saveFile);
      PrintWriter out = new PrintWriter(new BufferedWriter(w));
      out.print(text);
      out.flush();
      w.close();
    } catch (IOException ie) {
      System.out.println("Error when writing lib file!");
      ie.printStackTrace();
    }
  }
  //}}}
  
}
