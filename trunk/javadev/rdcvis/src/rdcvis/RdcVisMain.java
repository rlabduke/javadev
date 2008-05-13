// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package rdcvis;


//import molikin.logic.*;
//import driftwood.gui.*;
import driftwood.r3.*;
//import driftwood.util.*;
//import driftwood.data.*;
//import driftwood.util.SoftLog;
import driftwood.moldb2.*;
import king.core.*;
import king.io.*;
//import driftwood.mysql.*;
//
//import java.net.*;
import java.util.*;
import java.io.*;
//import javax.swing.*;
//import java.awt.event.*;
import java.text.*;
//import java.util.zip.*;
//import java.sql.*;

//}}}

/**
* <code>RdcVisMain</code> is based off the RDCVisTool in King for generating
* RDC curves for viewing.
*
* <p>Copyright (C) 2007 by Vincent Chen. All rights reserved.
* 
*/
public class RdcVisMain {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
   static String versionNumber = "1.00.080513";
  
  FileInterpreter fi;
  CoordinateFile pdb;
  
  KGroup group = null;
  KGroup subgroup = null;
  KGroup subError = null;
  boolean drawErrors = false;
  boolean ensembleTensor = true;
  static ArrayList<String> rdcTypes = null;
  //}}}
  
  //{{{ main
  //###############################################################
  public void Main(ArrayList<String> argList) {
    //simulatedGaps = new HashMap<Integer, Integer>();
    rdcTypes = new ArrayList<String>();
    //ArrayList<String> argList = parseArgs(args);
    if (argList.size() < 2) {
	    System.out.println("Not enough arguments: you must have an input pdb and an input mr file!");
    } else if (rdcTypes.size() == 0) {
      System.out.println("No RDCs specified: you must put it in the format of atom1-atom2.");
    } else {
      //args[0]
	    //File[] inputs = new File[args.length];
	    //for (int i = 0; i < args.length; i++) {
      //File pdbFile = new File(System.getProperty("user.dir") + "/" + args[0]);
      //File outKinFile = new File(System.getProperty("user.dir") + "/" + args[1]);
      File pdbFile = new File(argList.get(0));
      File outKinFile = new File(argList.get(0).substring(0, argList.get(0).length()-3)+"kin");
      File mrFile = new File(argList.get(1));
	    //}
      //System.out.println(pdbFile);
	    //RdcVisMain main = new RdcVisMain(new File(pdbFile.getAbsolutePath()), new File(outKinFile.getAbsolutePath()), new File(mrFile.getAbsolutePath()));
      pdb = readPdb(new File(pdbFile.getAbsolutePath()));
      MagneticResonanceFile mr = readMR(new File(mrFile.getAbsolutePath()));
      fi = new FileInterpreter(pdb, mr);
      for (String rdcName : rdcTypes) {
        fi.solveRdcsEnsemble(rdcName);
        
        writeKin(new File(outKinFile.getAbsolutePath()), rdcName);
      }
    }
  }
  
  public static void main(String[] args) {
    RdcVisMain mainprog = new RdcVisMain();
    ArrayList<String> argList = mainprog.parseArgs(args);
    mainprog.Main(argList);
    
  }
  //}}}
  
  //{{{ parseArgs
  public ArrayList parseArgs(String[] args) {
    //Pattern p = Pattern.compile("^[0-9]*-[0-9]*$");
    ArrayList<String> argList = new ArrayList<String>();
    String arg;
    for (int i = 0; i < args.length; i++) {
      arg = args[i];
      // this is an option
      if(arg.startsWith("-")) {
        if(arg.equals("-h") || arg.equals("-help")) {
          showHelp(true);
          System.exit(0);
        //} else if(arg.equals("-libloc") || arg.equals("-librarylocation")) {
        //  if (i+1 < args.length) {
        //    fragLibrary = new File(args[i+1]);
        //    if (!fragLibrary.canRead()) {
        //      System.err.println("Invalid input for fragment library location");
        //      System.exit(0);
        //    }
        //    i++;
        //  } else {
        //    System.err.println("No argument given for fragment library location");
        //    System.exit(0);
        //  }
        } else if (arg.equals("-ensembletensor")) {
          ensembleTensor = true;
        } else if (arg.equals("-modeltensor")) {
          ensembleTensor = false;
        } else {
          System.err.println("*** Unrecognized option: "+arg);
        }
      } else {
        if (arg.matches("[A-Za-z0-9]*-[A-Za-z0-9]*")) {
          rdcTypes.add(arg);
        } else {
          System.out.println(arg);
          argList.add(arg);
        }
      }
    }
    return argList;
  }
  //}}}
  
  //{{{ setDefaults
  public void setDefaults() {
    //String labworkPath = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").indexOf("labwork") + 7);
    ////System.out.println(labworkPath);
    //if (fragLibrary == null) {
    //  fragLibrary = new File(labworkPath + "/loopwork/fragfiller/");
    //}
    ////String labworkPath = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").indexOf("labwork") + 7);
    ////System.out.println(labworkPath);
    //if (pdbLibrary == null) {
    //  pdbLibrary = new File(labworkPath + "/loopwork/fragfiller/pdblibrary");
    //}
    //if (matchDiff < 0) {
    //  matchDiff = 0;
    //}
  }
  //}}}

  //{{{ Constructors
  public RdcVisMain() {
    //setDefaults();
    
    //readPdbLibrary();
    //pdb = readPdb(pdbFile);
    //MagneticResonanceFile mr = readMR(mrFile);
    //fi = new FileInterpreter(pdb, mr);
    //for (String rdcName : rdcTypes) {
    //  fi.solveRdcsEnsemble(rdcName);
    //  
    //  writeKin(outKinFile, rdcName);
    //}
  }
  //}}}
  
  //{{{ readPdb
  public CoordinateFile readPdb(File pdb) {
    CoordinateFile pdbFile;
    PdbReader reader = new PdbReader();
    try {
      pdbFile = reader.read(pdb);
	    pdbFile.setIdCode(pdb.getName());
	    return pdbFile;
    } catch (IOException ie) {
	    System.err.println("Problem when reading pdb file");
    }
    return null;
  }
  //}}}
  
  //{{{ readMR
  public MagneticResonanceFile readMR(File f) {
    try {
      NMRRestraintsReader nrr = new NMRRestraintsReader();
      nrr.scanFile(f);
      MagneticResonanceFile mrf = nrr.analyzeFileContents();
      return mrf;
      //analyzeFile(mrf);
    } catch (IOException ex) {
      System.err.println("An I/O error occurred while loading the file:\n"+ex.getMessage());
      //ex.printStackTrace(SoftLog.err);
    }
    return null;
  }
  //}}}
  
  //{{{ writeKin
  public void writeKin(File outKinFile, String rdcName) {
    Kinemage kin = new Kinemage();
    Iterator models = (pdb.getModels()).iterator();
    String[] atoms = fi.parseAtomNames(rdcName);
    while (models.hasNext()) {
      //System.out.print(".");
      Model mod = (Model) models.next();
      ModelState state = mod.getState();
      group = new KGroup("RDCs "+mod.toString());
      group.addMaster("Curves");
      group.setDominant(true);
      group.setAnimate(true);
      kin.add(group);
      subgroup = new KGroup("sub");
      subgroup.setHasButton(true);
      group.add(subgroup);
      if (drawErrors) {
        subError = new KGroup("suberrorbars");
        subError.setHasButton(true);
        group.add(subError);
      }
      Iterator iter = mod.getResidues().iterator();
      while (iter.hasNext()) {
        Residue orig = (Residue) iter.next();
        Triple rdcVect = getResidueRdcVect(state, orig, atoms);
        AtomState origin = getOriginAtom(state, orig, atoms);
        if ((rdcVect != null)&&(origin != null)) {
          drawCurve(kin, origin, rdcVect, orig);
        } else {
          //JOptionPane.showMessageDialog(kMain.getTopWindow(),
          //"Sorry, the atoms needed for this RDC do not seem to be in this residue.",
          //"Selected RDC atoms not found",
          //JOptionPane.ERROR_MESSAGE);
        }
      }
    }
    ArrayList<Kinemage> kins = new ArrayList<Kinemage>();
    kins.add(kin);
    KinfileWriter writer = new KinfileWriter();
    try {
      writer.save(new PrintWriter(new BufferedWriter(new FileWriter(outKinFile))), "RDC visualization text", kins);
    } catch (IOException ex) {
      System.err.println("An error occurred while saving the kin." + ex);
    }
  }
  //}}}
  
  //{{{ getResidueRdcVect
  /** returns RdcVect for orig residue based on what is selected in fi **/
  public Triple getResidueRdcVect(ModelState state, Residue orig, String[] atoms) {
    Atom from = orig.getAtom(atoms[0]);
    Atom to = orig.getAtom(atoms[1]);
    try {
      AtomState fromState = state.get(from);
      AtomState toState = state.get(to);
      Triple rdcVect = new Triple().likeVector(fromState, toState).unit();
      return rdcVect;
    } catch (AtomException ae) {
    }
    return null;
  }
  //}}}
  
  //{{{ getOriginAtom
  public AtomState getOriginAtom(ModelState state, Residue orig, String[] atoms) {
    Atom origin;
    if (atoms[0].indexOf("H") > -1) {
      origin = orig.getAtom(atoms[1]);
    } else {
      origin = orig.getAtom(atoms[0]);
    }
    try {
      AtomState originState = state.get(origin);
      return originState;
    } catch (AtomException ae) {
    }
    return null;
  }
  //}}}
  
  //{{{ drawCurve
  public void drawCurve(Kinemage kin, Tuple3 p, Triple rdcVect, Residue orig) {
    //if(kin == null) return null;
    //if (group == null) {
    //  group = new KGroup("RDCs");
    //  group.addMaster("Curves");
    //  group.setDominant(true);
    //  kin.add(group);
    //}
    //if (subgroup == null) {
    //  subgroup = new KGroup("sub");
    //  subgroup.setHasButton(true);
    //  group.add(subgroup);
    //}
    //if (subError == null) {
    //  subError = new KGroup("suberrorbars");
    //  subError.setHasButton(true);
    //  group.add(subError);
    //}
    String seq = orig.getSequenceNumber().trim();
    double rdcVal = fi.getRdcValue(seq);
    //System.out.println(rdcVal);
    //System.out.println((rdcVal != Double.NaN));
    double backcalcRdc = fi.getBackcalcRdc(rdcVect);
    if ((!Double.isNaN(rdcVal))&&(!Double.isNaN(backcalcRdc))) {
      KList list = new KList(KList.VECTOR, "RDCs");
      KList errorBars = new KList(KList.VECTOR, "Error Bars");
      list.setWidth(4);
      errorBars.setWidth(2);
      subgroup.add(list);
      subError.add(errorBars);
      //System.out.println(seq);
      //String seq = String.valueOf(KinUtil.getResNumber(p));
      //DipolarRestraint dr = (DipolarRestraint) currentRdcs.get(seq);
      String text = "res= "+seq+" rdc= "+df.format(rdcVal)+" backcalc= "+df.format(backcalcRdc);
      System.out.println(text);
      //fi.getDrawer().drawCurve(rdcVal, p, backcalcRdc, list);
      if (drawErrors) {
        fi.getDrawer().drawCurve(rdcVal - 2, p, 1, 60, backcalcRdc, errorBars, "-2 error bar");
        fi.getDrawer().drawCurve(rdcVal + 2, p, 1, 60, backcalcRdc, errorBars, "+2 error bar");
      }
      fi.getDrawer().drawCurve(rdcVal, p, 1, 60, backcalcRdc, list, text);
      //fi.getDrawer().drawCurve(rdcVal-0.5, p, 1, 60, backcalcRdc, list);
      //fi.getDrawer().drawCurve(rdcVal+0.5, p, 1, 60, backcalcRdc, list);
      //fi.getDrawer().drawCurve(backcalcRdc, p, 1, 60, backcalcRdc, list);
      //fi.getDrawer().drawAll(p, 1, 60, backcalcRdc, list);
    } else {
      System.out.println("this residue does not appear to have an rdc");
    }
  }
  //}}}
  
  //{{{ showHelp, showChanges, getVersion
  //##############################################################################
  /**
  * Parse the command-line options for this program.
  * @param args the command-line options, as received by main()
  * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
  *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
  */
  // Display help information
  void showHelp(boolean showAll)
  {
    if(showAll)
    {
      InputStream is = getClass().getResourceAsStream("RdcVis.help");
      if(is == null)
        System.err.println("\n*** Unable to locate help information in 'RdcVis.help' ***\n");
      else
      {
        try { streamcopy(is, System.out); }
        catch(IOException ex) { ex.printStackTrace(); }
      }
    }
    System.err.println();
    System.err.println("RdcVis version " + versionNumber);
    System.err.println("Copyright (C) 2008 by Vincent B. Chen. All rights reserved.");
  }
  
  // Display changes information
  void showChanges(boolean showAll)
  {
    if(showAll)
    {
      InputStream is = getClass().getResourceAsStream("RdcVis.changes");
      if(is == null)
        System.err.println("\n*** Unable to locate changes information in 'RdcVis.changes' ***\n");
      else
      {
        try { streamcopy(is, System.out); }
        catch(IOException ex) { ex.printStackTrace(); }
      }
    }
    System.err.print("RdcVis version " + versionNumber);
    System.err.println();
    System.err.println("Copyright (C) 2008 by Vincent B. Chen. All rights reserved.");
  }
  
  // Copies src to dst until we hit EOF
  void streamcopy(InputStream src, OutputStream dst) throws IOException
  {
    byte[] buffer = new byte[2048];
    int len;
    while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
  }
  //}}}
  
}//class
