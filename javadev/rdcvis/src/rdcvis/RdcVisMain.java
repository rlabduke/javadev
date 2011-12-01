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
import java.util.zip.*;
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
  static String versionNumber = "1.04.100708";
  
  //FileInterpreter fi;
  //CoordinateFile pdb;
  
  KGroup group = null;
  KGroup subgroup = null;
  KGroup subError = null;
  KGroup subSurface = null;
  boolean drawErrors = false;
  boolean ensembleTensor = true;
  boolean drawSurface = false;
  boolean textOutput = false;
  boolean drawCurveSphere = false;
  static ArrayList<String> rdcTypes = null;
  //}}}
  
  //{{{ main
  //###############################################################
  public void Main(ArrayList<String> argList) {
    if (argList.size() < 2) {
	    System.out.println("Not enough arguments: you must have an input pdb and an input mr file!");
    } else if (rdcTypes.size() == 0) {
      System.out.println("No RDCs specified: you must put it in the format of atom1-atom2.");
    } else if (textOutput) {
      File pdbFile = new File(argList.get(0));
      File mrFile = new File(argList.get(1));
      CoordinateFile pdb = readPdb(new File(pdbFile.getAbsolutePath()));
      MagneticResonanceFile mr = readMR(new File(mrFile.getAbsolutePath()));
      FileInterpreter fi = new FileInterpreter(pdb, mr);
      RdcAnalyzer analyzer = new RdcAnalyzer();
      analyzer.analyzeCoordFile(fi, rdcTypes, ensembleTensor);
    } else {
      File pdbFile = new File(argList.get(0));
      String outKinName = argList.get(0).substring(0, argList.get(0).length()-4);
      if (ensembleTensor) {
        outKinName = outKinName+"rdcvis-enstensor.kin";
      } else {
        outKinName = outKinName+"rdcvis-modeltensor.kin";
      }
      File outKinFile = new File(outKinName);
      File mrFile = new File(argList.get(1));
      Collection<Kinemage> inputKins = null;
      if (argList.size() > 2) {
        File inputKinFile = new File(argList.get(2));
        inputKins = readKinemage(inputKinFile);
      }
	    CoordinateFile pdb = readPdb(new File(pdbFile.getAbsolutePath()));
      MagneticResonanceFile mr = readMR(new File(mrFile.getAbsolutePath()));
      FileInterpreter fi = new FileInterpreter(pdb, mr);
      Kinemage rdcKin = createKin(fi);
      ArrayList<Kinemage> kins = new ArrayList<Kinemage>();
      if (inputKins != null) {
        for (Kinemage inKin : inputKins) {
          kins.add(mergeKins(inKin, rdcKin));
        }
      } else {
        kins.add(rdcKin);
      }
      writeKin(kins, new File(outKinFile.getAbsolutePath()));
    }
  }
  
  public static void main(String[] args) {
    RdcVisMain mainprog = new RdcVisMain();
    //rdcTypes = new ArrayList<String>();
    ArrayList<String> argList = mainprog.parseArgs(args);
    mainprog.Main(argList);
    
  }
  //}}}
  
  //{{{ mergeKins
  /** for merging groups of kins (ie RDC groups into their corresponding model groups in
  a multi-model multikin. **/
  public Kinemage mergeKins(Kinemage toKin, Kinemage fromKin) {
    ArrayList<KGroup> toGroups = toKin.getChildren();
    ArrayList<KGroup> fromGroups = fromKin.getChildren();
    for (int i = 0; i < toGroups.size(); i++) {
      if (i < fromGroups.size()) {
        KGroup toGrp = toGroups.get(i);
        KGroup fromGrp = fromGroups.get(i);
        for (AGE subs : fromGrp.getChildren()) {
          toGrp.add(subs);
        }
      }
    }
    return toKin;
  }
  //}}}
  
  //{{{ readKinemage
  /** from KiNG's KinfileLoader. **/
  public Collection<Kinemage> readKinemage(File f) {
    try {
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
      KinfileParser parser = new KinfileParser();
      parser.parse(lnr);
      
      
      lnr.close();
      
      return parser.getKinemages();
    } catch (IOException ie) {
      System.err.println("Problem when reading kinemage\n" + ie.getMessage());
    }
    return null;
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
        } else if (arg.equals("-surface")) {
          drawSurface = true;
        } else if (arg.equals("-text")) {
          textOutput = true;
        } else if (arg.equals("-curvesphere")) {
          drawCurveSphere = true;
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
    rdcTypes = new ArrayList<String>();
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
    System.err.println("An error occurred while loading the mr file");
    return null;
  }
  //}}}
  
  //{{{ createKin
  public Kinemage createKin(FileInterpreter fi) {
    Kinemage kin = new Kinemage();
    CoordinateFile pdb = fi.getPdb();
    for (String rdcName : rdcTypes) {
      if (ensembleTensor) {
        fi.solveRdcsEnsemble(rdcName);
      }
      //String[] atoms = fi.parseAtomNames(rdcName);
      Iterator models = (pdb.getModels()).iterator();
      while (models.hasNext()) {
        //System.out.print(".mod.");
        Model mod = (Model) models.next();
        ModelState state = mod.getState();
        if (!ensembleTensor) {
          fi.solveRdcsSingleModel(rdcName, mod.toString());
        }
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
        if (drawSurface) {
          subSurface = new KGroup("surfaces");
          subSurface.setHasButton(true);
          group.add(subSurface);
          subSurface.addMaster("RDC surfaces");
        }
        Iterator iter = mod.getResidues().iterator();
        while (iter.hasNext()) {
          Residue[] tofrom = fi.getFromToResidue(mod, (Residue) iter.next());
          if (tofrom != null && tofrom[0] != null && tofrom[1] != null) {          
            //System.out.println("tofrom: "+tofrom[0]);
            DipolarRestraint dr = fi.getRdc(tofrom[0]);
            if (dr != null) {
              //System.out.println("dr = "+dr.toString());
              String[] atoms = fi.parseAtomNames(rdcName, dr);
              //System.out.println(atoms[0]+atoms[1]);
              Triple rdcVect = RdcAnalyzer.getResidueRdcVect(state, tofrom[0], tofrom[1], atoms);
              AtomState origin = RdcAnalyzer.getOriginAtom(state, tofrom[0], atoms);
              //System.out.println(origin);
              if ((rdcVect != null)&&(origin != null)) {
                //System.out.println(orig);
                drawCurve(kin, origin, rdcVect, tofrom[0], fi);
                if (drawSurface) {
                  drawSurface(kin, origin, tofrom[0], fi);
                }
              } else {
              }
            } else {
              System.err.println("Residue: "+tofrom[0]+" doesn't have an RDC");
            }
            //JOptionPane.showMessageDialog(kMain.getTopWindow(),
            //"Sorry, the atoms needed for this RDC do not seem to be in this residue.",
            //"Selected RDC atoms not found",
            //JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    }
    return kin;
  }
  //}}}
  
  //{{{ writeKin
  public void writeKin(ArrayList<Kinemage> kins, File outKinFile) {
    //Kinemage kin = new Kinemage();
    //Iterator models = (pdb.getModels()).iterator();
    //String[] atoms = fi.parseAtomNames(rdcName);
    //while (models.hasNext()) {
    //  //System.out.print(".");
    //  Model mod = (Model) models.next();
    //  ModelState state = mod.getState();
    //  group = new KGroup("RDCs "+mod.toString());
    //  group.addMaster("Curves");
    //  group.setDominant(true);
    //  group.setAnimate(true);
    //  kin.add(group);
    //  subgroup = new KGroup("sub");
    //  subgroup.setHasButton(true);
    //  group.add(subgroup);
    //  if (drawErrors) {
    //    subError = new KGroup("suberrorbars");
    //    subError.setHasButton(true);
    //    group.add(subError);
    //  }
    //  Iterator iter = mod.getResidues().iterator();
    //  while (iter.hasNext()) {
    //    Residue orig = (Residue) iter.next();
    //    Triple rdcVect = getResidueRdcVect(state, orig, atoms);
    //    AtomState origin = getOriginAtom(state, orig, atoms);
    //    if ((rdcVect != null)&&(origin != null)) {
    //      drawCurve(kin, origin, rdcVect, orig);
    //    } else {
    //      //JOptionPane.showMessageDialog(kMain.getTopWindow(),
    //      //"Sorry, the atoms needed for this RDC do not seem to be in this residue.",
    //      //"Selected RDC atoms not found",
    //      //JOptionPane.ERROR_MESSAGE);
    //    }
    //  }
    //}
    //ArrayList<Kinemage> kins = new ArrayList<Kinemage>();
    //kins.add(kin);
    KinfileWriter writer = new KinfileWriter();
    try {
      writer.save(new PrintWriter(new BufferedWriter(new FileWriter(outKinFile))), "RDC visualization text", kins);
    } catch (IOException ex) {
      System.err.println("An error occurred while saving the kin." + ex);
    }
  }
  //}}}
  
  //{{{ addRdc
  public void addRdc(String rdc) {
    rdcTypes.add(rdc);
  }
  //}}}
  
  //{{{ set functions
  public void setDrawErrors(boolean value) {
    drawErrors = value;
  }
  
  public void setDrawSurfaces(boolean value) {
    drawSurface = value;
  }
  
  public void setEnsembleTensor(boolean value) {
    ensembleTensor = value;
  }
  
  public void setDrawCurveSphere(boolean value) {
    drawCurveSphere = value;
  }
  //}}}
  
  //{{{ drawCurve
  public void drawCurve(Kinemage kin, Tuple3 p, Triple rdcVect, Residue orig, FileInterpreter fi) {
    String seq = orig.getSequenceNumber().trim();
    double rdcVal = fi.getRdcValue(seq);
    //System.out.println(rdcVal);
    //System.out.println((rdcVal != Double.NaN));
    double backcalcRdc = fi.getBackcalcRdc(rdcVect);
    //System.out.println(orig+" backcalc "+backcalcRdc);
    double rdcError = fi.getRdcError(seq);
    if (Double.isNaN(rdcError)) rdcError = 1;
    if ((!Double.isNaN(rdcVal))&&(!Double.isNaN(backcalcRdc))) {
      double radius = rdcVect.distance(new Triple(0, 0, 0));
      KList list = new KList(KList.VECTOR, "RDCs");
      list.addMaster("RDCs");
      if (Math.abs(rdcVal - backcalcRdc) < rdcError)      list.addMaster("Good RDC match");
      else if (Math.abs(rdcVal - backcalcRdc) < 2*rdcError) list.addMaster("Mild RDC match");
      else                                         list.addMaster("Bad RDC match");
      list.setWidth(4);
      subgroup.add(list);
      String text = "res= "+seq+" rdc= "+df.format(rdcVal)+"+/-"+df.format(rdcError)+" backcalc= "+df.format(backcalcRdc);
      //System.out.println(text);
      //fi.getDrawer().drawCurve(rdcVal, p, backcalcRdc, list);
      if (drawCurveSphere) {
        fi.getDrawer().drawAll(p, radius, 60, backcalcRdc, list);
      } else {
        fi.getDrawer().drawCurve(rdcVal, p, rdcVect, radius, 60, backcalcRdc, list, text, rdcError);
        //fi.getDrawer().drawCurve(rdcVal-0.5, p, 1, 60, backcalcRdc, list);
        //fi.getDrawer().drawCurve(rdcVal+0.5, p, 1, 60, backcalcRdc, list);
        //fi.getDrawer().drawCurve(backcalcRdc, p, 1, 60, backcalcRdc, list);
        //fi.getDrawer().drawAll(p, 1, 60, backcalcRdc, list);
        if (drawErrors) {
          KList errorBars = new KList(KList.VECTOR, "Error Bars");
          list.addMaster("RDC error bars");
          for (String master : list.getMasters()) {
            errorBars.addMaster(master);
          }
          errorBars.setWidth(2);
          subError.add(errorBars);
          fi.getDrawer().drawCurve(rdcVal - rdcError*2, p, rdcVect, radius, 60, backcalcRdc, errorBars, "-2x error bar", rdcError);
          fi.getDrawer().drawCurve(rdcVal + rdcError*2, p, rdcVect, radius, 60, backcalcRdc, errorBars, "+2x error bar", rdcError);
        }
      }
    } else {
      //System.out.println("this residue does not appear to have an rdc");
    }
  }
  //}}}
  
  //{{{ drawSurface
  public void drawSurface(Kinemage kin, Tuple3 p, Residue orig, FileInterpreter fi) {
    String seq = orig.getSequenceNumber().trim();
    double rdcVal = fi.getRdcValue(seq);
    if (!Double.isNaN(rdcVal)) {
      KList list = new KList(KList.BALL, "surfaces");
      list.setNoHighlight(true);
      list.setAlpha(100);
      subSurface.add(list);
      fi.getDrawer().drawSurface(rdcVal, p, list);
      //KList tlist = new KList(KList.TRIANGLE, "surfaces2");
      //tlist.setAlpha(100);
      //subgroup.add(tlist);
      //fi.getDrawer().drawTriangleSurface(rdcVal, p, tlist);
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
