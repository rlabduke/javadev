// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package absigner;

import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.text.*;

import driftwood.r3.*;
import driftwood.moldb2.*;
//}}}

public class Absigner {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  Parameters params = null;
  //}}}
  
  //{{{ main
  //###############################################################
  public static void main(String[] args) {
    ArrayList<String> argList = parseArgs(args);
    if (argList.size() < 1) {
	    System.out.println("Not enough arguments: you must have an input pdb!");
    } else {
      File pdbFile = new File(argList.get(0));
	    Absigner absigner = new Absigner(new File(pdbFile.getAbsolutePath()));
    }
  }
  //}}}
  
  //{{{ Constructor
  public Absigner(File f) {
    String name = f.getName();
    System.out.println(name);
    CoordinateFile pdb = null;
    if (name.endsWith(".pdb")||name.endsWith(".pdb.gz")) {
      pdb = readFile(f);
    }
    params = new Parameters();
    analyzePdb(1, pdb);
    analyzePdb(2, pdb);
  }
  //}}}
  
  //{{{ parseArgs
  public static ArrayList parseArgs(String[] args) {
    //Pattern p = Pattern.compile("^[0-9]*-[0-9]*$");
    ArrayList<String> argList = new ArrayList<String>();
    String arg;
    for (int i = 0; i < args.length; i++) {
      arg = args[i];
      // this is an option
      if(arg.startsWith("-")) {
        if(arg.equals("-h") || arg.equals("-help")) {
          System.err.println("Help not available. Sorry!");
          System.exit(0);
        } else {
          System.err.println("*** Unrecognized option: "+arg);
        }
      } else {
        System.out.println(arg);
        argList.add(arg);
      }
    }
    return argList;
  }
  //}}}
  
  //{{{ readFile
  public CoordinateFile readFile(File f) {
    try {
	    //System.out.println("reading in file");
      InputStream input = new FileInputStream(f);
      LineNumberReader    lnr;
      
      // Test for GZIPped files
      input = new BufferedInputStream(input);
      input.mark(10);
      if(input.read() == 31 && input.read() == 139)
      {
        // We've found the gzip magic numbers...
        input.reset();
        input = new GZIPInputStream(input);
      }
      else input.reset();
      
      lnr = new LineNumberReader(new InputStreamReader(input));
      
	    PdbReader reader = new PdbReader();
	    reader.setUseSegID(false);
      //File pdb = new File(f);
	    CoordinateFile coordFile = reader.read(lnr);
	    System.out.println(coordFile.getIdCode()+" has been read");
      lnr.close();
      return coordFile;
    }
    catch (IOException e) {
	    System.err.println("IO Exception thrown " + e.getMessage());
    }
    return null;
  }
  //}}}
  
  //{{{ analyzePdb
  public void analyzePdb(int fraglength, CoordinateFile pdb) {
    //String libParams = "";
    //CoordinateFile pdb = readFile(f);
    if (pdb == null) {
      System.err.println("Somehow a file wasn't readable");
    } else {
      Model first = pdb.getFirstModel();
      Set<String> chains = first.getChainIDs();
      for (String cid : chains) {
        System.out.println("Chain -" + cid + "-");
        Set<Residue> residues = first.getChain(cid);
        fragalyze(pdb.getIdCode(), first, residues, fraglength);
      }
    }
    //return libParams+"\n";
  }
  //}}}

  //{{{ fragalyze
  public void fragalyze(String pdbId, Model mod, Set<Residue> residues, int size) {
    ArrayList<Residue> currFrag = new ArrayList();
    HashMap<Residue, Integer> alphaMap = new HashMap<Residue, Integer>();
    for (Residue res : residues) {
      alphaMap.put(res, new Integer(0));
    }
    //double maxBfactor = 0;
    //String params = "";
    for (Residue res : residues) {
      //System.out.println(res.getCNIT());
      //if (!res.getName().equals("HOH")) System.out.println(res.getCNIT() + " " + currFrag.size());
      //if (!res.getName().equals("PHE")) testForAlts(res, mod);
      if (currFrag.size() != size + 3) {
        if (isBackboneComplete(res, mod)/*&&(res.getInsertionCode().equals(" "))*/) {
          currFrag.add(res);
        } else {
          //maxBfactor = 0;
          currFrag.clear(); // so none of the fragments have incomplete residues.
        }
      }
      if (currFrag.size() == size + 3) {
        double[] currParams = parameterize(mod, currFrag, size);
        if (inAlphaRange(currParams, params.getAlphaSize(size), params.getAlphaSD(size))) {
          for (Residue alphaLike : currFrag) {
            System.out.println(alphaLike.toString()+" is alpha-like");
            Integer count = alphaMap.get(alphaLike);
            alphaMap.put(alphaLike, new Integer(count.intValue() + 1));
          }
        }
        currFrag.remove(0);
      }
    }
  }
  //}}}
  
  //{{{ isBackboneComplete(Residue)
  /** tries to check if a residue has the correct number and type of atoms */
  public static boolean isBackboneComplete(Residue res, Model mod) {
    ModelState modState = mod.getState(); // n.b. that this is just the default state (ie. "A" or " ")
    Iterator atoms = (res.getAtoms()).iterator();
    int atomTotal = 0x00000000;
    while (atoms.hasNext()) {
	    Atom at = (Atom) atoms.next();
      if (!modState.hasState(at)) return false;
	    String atomName = at.getName();
	    if (atomName.equals(" N  ")) atomTotal = atomTotal + 0x00000001;
	    if (atomName.equals(" CA ")) atomTotal = atomTotal + 0x00000002;
	    if (atomName.equals(" C  ")) atomTotal = atomTotal + 0x00000004;
	    if (atomName.equals(" O  ")) atomTotal = atomTotal + 0x00000008;

      //if (res.getName().equals("PHE")) System.out.println(atomTotal);
    }
    //if (!res.getName().equals("HOH")) System.out.println(atomTotal);    
    if (atomTotal == 15) {
	    return true;
    } else {
	    //System.out.println("Residue: " + res + " not complete");
	    return false;
    }
  }
  //}}}
  
  //{{{ parameterize
  public double[] parameterize(Model mod, ArrayList<Residue> frag, int size) {
    Residue zeroRes = frag.get(0);
    Residue oneRes = frag.get(1);
    Residue twoRes = frag.get(2);
    Residue mRes = frag.get(frag.size()-3);
    Residue n1Res = frag.get(frag.size()-1);
    Residue nRes = frag.get(frag.size()-2);
    ModelState modState = mod.getState();
    //String params = "";
    double[] parameters = null;
    try {
      AtomState ca0 = modState.get(zeroRes.getAtom(" CA "));
      AtomState ca1 = modState.get(oneRes.getAtom(" CA "));
      AtomState caN = modState.get(nRes.getAtom(" CA "));
      AtomState caN1 = modState.get(n1Res.getAtom(" CA "));
      AtomState co0 = modState.get(zeroRes.getAtom(" O  "));
      AtomState coN = modState.get(nRes.getAtom(" O  "));
      parameters = frameAnalyze(ca0, ca1, caN, caN1, co0, coN);
      //return parameters;
      //params = params.concat(String.valueOf(size)+":"+zeroRes.getSequenceNumber().trim()+":");
      for (double d : parameters) { System.out.print(df.format(d)+":"); }
      System.out.println();
    } catch (AtomException ae) {
      System.err.println("Problem with atom " + ae.getMessage());
    }
    return parameters;
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
  
  //{{{ inAlphaRange
  public boolean inAlphaRange(double[] currFrag, double[] ssParams, double[] paramsSD) {
    boolean inRange = true;
    for (int i = 0; i < ssParams.length; i++) {
      if ((currFrag[i] > ssParams[i] + paramsSD[i]) || (currFrag[i] < ssParams[i] - paramsSD[i])) {
        inRange = false;
      }
    }
    return inRange;
  }
  //}}}
  
}
