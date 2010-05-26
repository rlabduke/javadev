// (jEdit options) :folding=explicit:collapseFolds=1:
package cmdline.fragment;

import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.mysql.*;

import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.text.*;

/**
* FragmentLibraryCreator takes in a directory of files and generates a fragment library parameter data set.
* It excludes fragments with altconfs, insertion codes, or residues with incomplete or odd backbones.
**/

public class FragmentLibraryCreator {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  ArrayList<File> pdbs;
  CoordinateFile currentPdb;
  static int lowSize;
  static int highSize;
  static boolean qualityFilter = true;
  static boolean excludeClash = true;
  DatabaseManager dm;
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
        } else if (arg.equals("-n") || arg.equals("-noqualityfilter")) {
          qualityFilter = false;
        } else if (arg.equals("-c") || arg.equals("-excludeclashes")) {
          excludeClash = false;
        } else {
          System.err.println("*** Unrecognized option: "+arg);
        }
      } else {
        if (arg.matches("[0-9]*-[0-9]*")) {
          String[] gapBounds = arg.split("-");
          int first = Integer.parseInt(gapBounds[0]);
          int sec = Integer.parseInt(gapBounds[1]);
          lowSize = Math.min(first, sec);
          highSize = Math.max(first, sec);
          if (lowSize <= 0) {
            System.err.println("Can't generate library <= 0 in size!");
            System.exit(0);
          }
          argList.add(arg);
        } else {
          System.out.println(arg);
          argList.add(arg);
        }
      }
    }
    return argList;
  }
  //}}}
  
  //{{{ main
  public static void main(String[] args) {
    ArrayList<String> argList = parseArgs(args);
    long startTime = System.currentTimeMillis();
    if (argList.size() < 3) {
	    System.out.println("Not enough arguments, you need a size range, a directory, and an outfile prefix, in that order!  Use -n for no quality filtering, and -c to exclude fragments with clashes.");
    } else {
      //int size = Integer.parseInt(args[0]);
      File pdbsDirectory = new File(argList.get(1));
      pdbsDirectory = pdbsDirectory.getAbsoluteFile();
      File[] files = pdbsDirectory.listFiles();
      //for (File f : files) {
      //  String name = f.getName();
      //  if (name.endsWith(".pdb")) {}
      LibraryWriter[] writers = new LibraryWriter[highSize+1];
      for (int size = lowSize; size <= highSize; size++) {
        String outPrefix = argList.get(2);
        File saveFile = new File(argList.get(2)+size+".txt");
        saveFile = saveFile.getAbsoluteFile();
        LibraryWriter writer = new LibraryWriter(saveFile);
        System.out.println(saveFile.getAbsolutePath());
        writers[size] = writer;
      }
      FragmentLibraryCreator filterer = new FragmentLibraryCreator(files);
      Iterator iter = filterer.iterator();
      while (iter.hasNext()) {
        File f = (File) iter.next();
        System.out.println(f.toString());
        filterer.setPdb(f);
        for (int size = lowSize; size <= highSize; size++) {
          LibraryWriter writer = writers[size];
          String lib = filterer.createLibrary(size);
          //System.out.println(lib.length());
          if (lib.length() > 1) { // avoids possible lib where none of structure generated fragments
            writer.write(lib);
          }
          //filterer.write(saveFile, lib);
        }
      }
      for (LibraryWriter writer : writers) {
        if (writer != null) {
          writer.close();
        }
      }
      filterer.closeDatabase();
    }
    
    long endTime = System.currentTimeMillis();
    System.out.println((endTime - startTime)/1000 + " seconds to generate library");
  }
  //}}}
  
  //{{{ Constructor
  public FragmentLibraryCreator(File[] files) {
    dm = new DatabaseManager();
    //dm.connectToDatabase("//spiral.research.duhs.duke.edu/qDBrDB");
    dm.connectToDatabase("//quality.biochem.duke.edu:1352/jiffiloop");
    
    pdbs = new ArrayList<File>();
    for (File f : files) {
      String name = f.getName();
      System.out.println(name);
      if (name.endsWith(".pdb")||name.endsWith(".pdb.gz")) {
        pdbs.add(f);
      }
    }
    Collections.sort(pdbs);
  }
  //}}}

  //{{{ iterator
  public Iterator iterator() {
    return pdbs.iterator();
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
  
  //{{{ setPdb
  public void setPdb(File f) {
    currentPdb = readFile(f);
  }
  //}}}
    
  //{{{ createLibrary (depreciated)
  /** only uses first model of each pdb file **/
  //public String createLibrary(int fraglength) {
  //  String libParams = "";
  //  for (File f : pdbs) {
  //    CoordinateFile pdb = readFile(f);
  //    if (pdb == null) {
  //      System.err.println("Somehow a file wasn't readable");
  //    } else {
  //      Model first = pdb.getFirstModel();
  //      Set<String> chains = first.getChainIDs();
  //      for (String cid : chains) {
  //        System.out.println("Chain -" + cid + "-");
  //        Set<Residue> residues = first.getChain(cid);
  //        if (libParams.equals(""))  libParams = fragalyze(pdb.getIdCode(), first, residues, fraglength);
  //        else                       libParams = libParams+"\n"+fragalyze(pdb.getIdCode(), first, residues, fraglength);
  //        //System.out.println(libParams);
  //      }
  //    }
  //    libParams = libParams.trim(); // to get rid of spaces in between pdb file parameters
  //  }
  //  return libParams;
  //}
  //}}}
  
  //{{{ createLibrary(fraglength)
  public String createLibrary(int fraglength) {
    String libParams = "";
    //CoordinateFile pdb = readFile(f);
    if (currentPdb == null) {
      System.err.println("Somehow a file wasn't readable");
    } else {
      Model first = currentPdb.getFirstModel();
      Set<String> chains = first.getChainIDs();
      for (String cid : chains) {
        System.out.println("Chain -" + cid + "-");
        Set<Residue> residues = first.getChain(cid);
        if (libParams.equals(""))  libParams = fragalyze(currentPdb.getIdCode(), first, residues, fraglength);
        else                       libParams = libParams+"\n"+fragalyze(currentPdb.getIdCode(), first, residues, fraglength);
        //System.out.println(libParams);
      }
    }
    libParams = libParams.trim(); // to get rid of spaces in between currentPdb file parameters from water chains, etc
    return libParams+"\n";
  }
  //}}}

  //{{{ fragalyze
  public String fragalyze(String pdbId, Model mod, Set<Residue> residues, int size) {
    ArrayList<Residue> currFrag = new ArrayList();
    //double maxBfactor = 0;
    String params = "";
    for (Residue res : residues) {
      //System.out.println(res.getCNIT());
      //if (!res.getName().equals("HOH")) System.out.println(res.getCNIT() + " " + currFrag.size());
      //if (!res.getName().equals("PHE")) testForAlts(res, mod);
      if (currFrag.size() != size + 3) {
        if (isBackboneComplete(res, mod)&&(res.getInsertionCode().equals(" "))&&(!isOutlier(pdbId, res))) {
          if (currFrag.size() > 0) {
            if (!isBonded(currFrag.get(currFrag.size()-1), res, mod)) {
              currFrag.clear();
            }
          }
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
  
  //{{{ isOutlier
  /** 
  *   Checks if there are any quality outliers for a residue, using our mysql database 
  *   (currently quality) which has a table containing the molprobity residue analysis
  *   data for the top 5200
  **/
  public boolean isOutlier(String pdbId, Residue res) {
    if (!qualityFilter) { return false; }

    String sqlSelect = "SELECT worst_clash, CB_dev, rama_eval, num_length_out, num_angle_out FROM top5200_mp_residue \n";
    sqlSelect = sqlSelect.concat("WHERE pdbid = '"+pdbId.toLowerCase()+"' ");
    sqlSelect = sqlSelect.concat("AND chain = '"+res.getChain()+"' ");
    sqlSelect = sqlSelect.concat("AND resnum = '"+res.getSequenceNumber()+"' ");
    sqlSelect = sqlSelect.concat("AND inscode = '"+res.getInsertionCode()+"' ");
    sqlSelect = sqlSelect.concat("AND restype = '"+res.getName()+"'");
    sqlSelect = sqlSelect.concat("\n");
    //System.out.println(sqlSelect);
    dm.select(sqlSelect);
    while (dm.next()) {
      //System.out.println(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" "+dm.getString(4));
      //if (!dm.getString(1).equals("0.000")||(!dm.getString(2).equals("0.000"))||(dm.getString(3).equals("OUTLIER"))||(!dm.getString(4).equals("0"))||(!dm.getString(5).equals("0"))) System.out.println(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" "+dm.getString(4)+" "+dm.getString(5));
      if (excludeClash) {
        if (!dm.getString(1).equals("0.000")) return true; // clash not zero (always zero unless greater than 0.4
      }
      if (!dm.getString(2).equals("0.000")) return true; // cbetadev not zero (always zero unless greater than 0.25
      if (dm.getString(3).equals("OUTLIER")) return true; // rama outlier
      if (!dm.getString(4).equals("0")) return true;      // bond outliers greater than zero?
      if (!dm.getString(5).equals("0")) return true;      // angle outliers greater than zero?
    }
    return false;
  }
  //}}}
  
  //{{{ isBonded
  public boolean isBonded(Residue prev, Residue curr, Model mod) {
    ModelState modState = mod.getState();
    try {
      AtomState prevC   = modState.get(prev.getAtom(" C  "));
      AtomState n       = modState.get(curr.getAtom(" N  "));
      if(prevC.sqDistance(n) > 2.56) return false;
      return true;
    } catch (AtomException ae) {
      System.err.println("An incomplete residue got passed to isBonded:" + ae.toString());
    }
    return false;
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
  
  //{{{ testforalts
  private static boolean testForAlts(Residue res, Model mod) {
    Map<String, ModelState> modStates = mod.getStates();
    Iterator atoms = (res.getAtoms()).iterator();
    //int atomTotal = 0x00000000;
    while (atoms.hasNext()) {
      Atom at = (Atom) atoms.next();
      try {
        for (ModelState modState : modStates.values()) {
          if (modState.hasState(at)) {
            AtomState atState = modState.get(at);
            if (!atState.getAltConf().equals(" ")) {
              System.out.println(atState);
            }
          }
        }
      } catch (AtomException ae) {
        System.err.println("Problem with atom " + ae.getMessage());
      }
    }
    return false;
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
  //public void write(File saveFile, String text) {
  //  try {
  //    System.out.println(saveFile.getCanonicalPath());
  //    if (!saveFile.exists()) {
  //      saveFile.createNewFile();
  //    }
  //    Writer w = new FileWriter(saveFile);
  //    PrintWriter out = new PrintWriter(new BufferedWriter(w));
  //    out.print(text);
  //    out.flush();
  //    w.close();
  //  } catch (IOException ie) {
  //    System.out.println("Error when writing lib file!");
  //    ie.printStackTrace();
  //  }
  //}
  //}}}
  
  //{{{ closeDatabase
  public void closeDatabase() {
    dm.close();
  }
  //}}}
  
}
