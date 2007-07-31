// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package/Imports
//##################################################################################################
package cmdline;

import java.text.DecimalFormat;
import molikin.*;
import molikin.logic.*;
import driftwood.data.*;
import driftwood.r3.*;
import driftwood.moldb2.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
//}}}


// I made this class by modifying Vince's original PdbSuperimposer.java. -- DK

public class MultiPdbSuperimposer
{ 
//{{{ Constants
//##################################################################################################
	String AA_NAMES = "GLY,ALA,VAL,LEU,ILE,PRO,PHE,TYR,TRP,SER,THR,CYS,MET,MSE,LYS,HIS,ARG,ASP,"+
		"ASN,GLN,GLU";
	final DecimalFormat df = new DecimalFormat("0.###");
//}}}

//{{{ Variable Definitions
//##################################################################################################
    
    String outPrefixAbsPath;
	String inPdbListFileAbsPath;
    boolean kinoption;
	boolean dobinkin;
    boolean doCZballs;
	boolean doguannormals;
  	int quasipolarBins;
    String bbAtoms;
    
    File kinOut;
    ArrayList<PrintWriter> binkinPrintWriters;
	ArrayList<PrintWriter> guankinPrintWriters;
    
    CoordinateFile cleanFile;
    String pdbcode;
	String chain;
    int resno;
    String altconf;
    String cnit;
    PdbReader reader;
	
//}}}

//{{{ main
//##################################################################################################
	public static void main(String[] args)
	{
		MultiPdbSuperimposer imposer = new MultiPdbSuperimposer();
		imposer.parseArgs(args);
		imposer.build();
	}
//}}}

//{{{ Constructor
//##################################################################################################
	public MultiPdbSuperimposer()
	{
		// Set defaults
        kinoption       = true;
        dobinkin        = false;
        doCZballs       = false;
        doguannormals   = false;
        quasipolarBins  = 10;
        String bbAtoms = "N,CA,C";
	}
//}}}

//{{{ parseArgs, interpretArg, interpretFlag
//##################################################################################################
	private void parseArgs(String[] args)
	{
		// Parse arguments from command line or passed from MySqlLiaison
		
		if (args.length < 2) 
        {
			System.out.println("This function needs at least 2 arguments: (1) a .csv file"+ 
				"containing a list of pdb names, chain ids, and residue numbers; and "+
                "(2) an output prefix with no extension!");
            System.exit(0);
		}
		
        for (int i = 0; i < args.length; i ++)
        {
            
            if (!args[i].startsWith("-"))
            {
                // Probably a .csv input filename or .kin output filename
                interpretArg(args[i]);
            }
            
            else 
            {
                // Probably a flag; may have a param after the = sign
                String flag, param;
                int eq = args[i].indexOf('=');
                if(eq != -1)
                {
                    flag    = args[i].substring(0, eq);
                    param   = args[i].substring(eq+1);
                }
                else
                {
                    flag    = args[i];
                    param   = null;
                }
                
                try { interpretFlag(flag, param); }
                catch(NullPointerException ex)
                { throw new IllegalArgumentException("'"+args[i]
                    +"' expects to be followed by a parameter"); }
            }
        }
        
        prepOutFiles();
    }
    
    private void interpretArg(String arg)
    {
        if (arg.indexOf(".csv") > 0)
        {
            File inPdbList = new File(arg);
            inPdbListFileAbsPath = inPdbList.getAbsolutePath();
        }
        else if (arg.indexOf(".") < 0)
        {
            String outPrefix = arg;
            File outPdbFile = new File(outPrefix+".pdb");
            String outPdbFileAbsPath = outPdbFile.getAbsolutePath();
            int outPdbFileAbsPathLength = outPdbFileAbsPath.length();
            outPrefixAbsPath = outPdbFileAbsPath.substring(0, outPdbFileAbsPathLength-4);
            // This should give 
            //     /home/keedy/...../OUTPREFIX 
            // with no .pdb or .kin on the end
            System.out.println("outPrefixAbsPath:     "+outPrefixAbsPath);
        }
	}
    
    private void interpretFlag(String flag, String param)
    {
        if(flag.equals("-kin"))
        {
            kinoption = true;
            dobinkin = false;
        }
        else if(flag.equals("-binkin"))
        {
            dobinkin = true;
            kinoption = false;
        }
        else if (flag.equals("-numbins"))
        {
            try { this.quasipolarBins = Integer.parseInt(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException("Expected -numbins=#"); }
        }
        else if (flag.equals("-argczballs"))
        {
            doCZballs = true;
        }
        else if (flag.equals("-argguannormals"))
        {
            doguannormals = true;
        }
        else if (flag.equals("-bbAtoms"))
        {
            if (param.equals("N,CA,C") || param.equals("CA,C,O"))
                this.bbAtoms = param;
            else
                System.out.println("Expected N,CA,C or CA,C,O after -bbAtoms=");
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}

//{{{ prepOutFiles
//##################################################################################################
	private void prepOutFiles()
	{
		if (kinoption)
        {
            kinOut = new File(outPrefixAbsPath+".kin");
            try
            {
                Writer w = new FileWriter(kinOut);
                PrintWriter out = new PrintWriter(new BufferedWriter(w));
                out.println("@kinemage");
                out.println("@master {all residues}");
                out.flush();
                w.close();
            } 
            catch (IOException ex) 
            {
                System.err.println("An error occurred while writing '@kinemage'." + ex);
            }
        }
        
        else if (dobinkin)
        {
            binkinPrintWriters = new ArrayList<PrintWriter>();
            
            try
            {
                for (int b = 1; b <= quasipolarBins; b ++)
                {
                    File binkinFile = new File(outPrefixAbsPath+"_bin"+b+".kin");
                    Writer w = new FileWriter(binkinFile, true);   
                    // the second argument is a boolean telling whether to append to the file or not
                    PrintWriter out = new PrintWriter(new BufferedWriter(w));
                    
                    double binDihdMin = (b - 1) * (360 / quasipolarBins);
                    double binDihdMax = (b    ) * (360 / quasipolarBins);
                    int min = (int) Math.round(binDihdMin);
                    int max = (int) Math.round(binDihdMax);
                    
                    if (b == 1)
                        out.println("@group {bin"+b+" "+min+"-"+max+"} animate on "+
                            "dominant master= {all bins}");
                        else
                            out.println("@group {bin"+b+" "+min+"-"+max+"} animate off "+
                            "dominant master= {all bins}");
                    
                    binkinPrintWriters.add(out);
                }
            }
            catch (IOException ex) 
            {
              System.err.println("An error occurred while writing the _bin(b).kin." + ex);
            }
        }
        
        else if (doguannormals)
        {
            guankinPrintWriters = new ArrayList<PrintWriter>();
            
            try
            {
                for (int b = 1; b <= quasipolarBins; b ++)
                {
                    File guankinFile = new File(outPrefixAbsPath+"_bin"+b+"guans.kin");
                    Writer w = new FileWriter(guankinFile, true);   
                      // the second argument is a boolean telling whether to append to the file
                      // or not
                    PrintWriter out = new PrintWriter(new BufferedWriter(w));
                    
                    out.println("@vectorlist {bin"+b+" guan normals} animate on "+
                        "color= hotpink master= {guan normals}");
                    
                    guankinPrintWriters.add(out);
                }
            }
            catch (IOException ex) 
            {
                System.err.println("An error occurred while writing the _bin(b)guans.kin." + ex);
            }
        }
        
	}
//}}}

//{{{ build
//##################################################################################################
	private void build()
	{
		Builder built = new Builder();
		try
		{
			cleanFile = new CoordinateFile();
			
            System.out.println("inPdbListFileAbsPath: "+inPdbListFileAbsPath);
            
            Scanner pdbidScanner = new Scanner(new File(inPdbListFileAbsPath));
			  // for getting pdbcode to put in @group name
			
			Scanner inListScanner = new Scanner(new File(inPdbListFileAbsPath));
			
			int count = 0;
			String line = "";
			while (inListScanner.hasNextLine())   // for each pdb + resno combination
			{
				  line = inListScanner.nextLine();
				  Scanner lineScanner = new Scanner(line);
				  pdbcode = lineScanner.next();
				  chain = lineScanner.next();
				  resno = Integer.parseInt(lineScanner.next());
				  if (lineScanner.hasNext())
				  {
					  // i.e. altconf is not the null string ""
					  altconf = lineScanner.next();
				  }
				  else altconf = "no_alt_conf";
				  lineScanner.close();
				  
				  //System.out.println("Want\tres "+resno+", chain "+chain+", pdb "+pdbcode);
				  
				  String inputfilenamepdb = "/home/keedy/PDBs/neo500copy/"+pdbcode+".pdb";
				  
				  // This entire pdb (all models and residues)
				  CoordinateFile thisPdbsCoordFile = getCoordFile(inputfilenamepdb);
				  
				  // Trims model and adds any single residues that match resno + chain + pdbcode
				  // criteria to cleanFile in the form of single-residue models
                  overlayResidues(thisPdbsCoordFile, built);
				  
				  count ++;
				  if (count == 100) 
				  // 100 worked for chi1-4_anyrot_begs.specif_ch.noalts.sql; 250, 500 did not
				  {
					  if (kinoption)	  writeToKin(pdbidScanner);
					  if (dobinkin)		  writeToBinKins();
					  
				  	  count = 0;
				  	  cleanFile = new CoordinateFile();
				  }
				  
			} // done looking through pdb files in list
			inListScanner.close();
			
			if (kinoption)		writeToKin(pdbidScanner);
			if (dobinkin)
			{
				writeToBinKins();
				if (doCZballs)		makeCzBallKins();
				if (doguannormals)	makeGuanNormalKins();
				assembleBinKin();
			}
		}
        catch (IOException e)
        {
            System.err.println("IO Exception thrown " + e.getMessage());
        }
	}
//}}}

//{{{ getCoordFile
//##################################################################################################
  private CoordinateFile getCoordFile(String inputfilenamepdb)
  {
	  CoordinateFile coordFile = new CoordinateFile();	// what we will return
	  reader = new PdbReader();
	  reader.setUseSegID(false);
	  
	  // Files were brought into the neo500copy directory in .pdb.gz format, so 
	  // we want to get a gzipInputStream from those to make the CoordinateFile
	  
	  try
	  {
		  GZIPInputStream gzipInputStream = new GZIPInputStream(
			  new FileInputStream(inputfilenamepdb+".gz"));
		  coordFile = reader.read( (InputStream) gzipInputStream);
		  
		  gzipInputStream.close();
	  }
	  catch (IOException e)
	  {
		  System.out.println("Can't open .pdb.gz file");
	  }
	  
	  return coordFile;
  }
//}}}

//{{{ overlayResidues
//##################################################################################################
  private void overlayResidues(CoordinateFile thisPdbsCoordFile, Builder built)
  {
	  Iterator models = (thisPdbsCoordFile.getModels()).iterator();
	  
	  AtomState modN = null;
	  AtomState modCA = null;
	  AtomState modC = null;
	  //AtomState modO = null;
	  
	  Triple refCA = new Triple(0, 0, 0);
	  Triple refN = new Triple(-0.35, -1.46, 0);
	  Triple refC = new Triple(1.5, 0, 0);
	  //Triple refO = new Triple(2.15, 1, 0);
	  
	  // Iterate thru models in *this* pdb file
	  while (models.hasNext())
	  {
		  // Make a clone so we can delete Residues from it with no danger of deleting actual
		  // Residues from the original Model
		  Model mod = (Model) models.next();
          Model modClone = (Model) mod.clone();	
          Model modOneRes = new Model("");
          
		  try
		  {
			             modOneRes      = trimModel(modClone, chain, resno);
			  ModelState modOneResState = modOneRes.getState();
              Residue    desiredRes     = modOneRes.getResidue(cnit);
			  
			  modCA = modOneResState.get(desiredRes.getAtom(" CA "));
			  modN  = modOneResState.get(desiredRes.getAtom(" N  "));
			  modC  = modOneResState.get(desiredRes.getAtom(" C  "));
			  //modO = modState.get(desiredRes.getAtom(" O  "));
			  
			  Transform dock3pointNCAC = built.dock3on3(refCA, refN, refC, modCA, modN, modC);
			  transformModel(modOneRes, dock3pointNCAC);
		  }
		  catch (AtomException ae) 
		  {
			  System.out.println("a mod atom wasn't found");
		  }
		  
		  cleanFile.add(modOneRes);	// Done with this model
		  
	  } // Done iterating through models in this pdb file
  }
//}}}

//{{{ trimModel
//##################################################################################################
  private Model trimModel(Model modBeingTrimmed, String chain, int resno)
  {
	  Residue desiredRes = null;
	  
      // Make AL of all Residues in this Model 
	  ArrayList<Residue> residues = new ArrayList<Residue>();
	  Iterator unmodifiableResidues = (modBeingTrimmed.getResidues()).iterator();
	  while (unmodifiableResidues.hasNext())
	  {
		  Residue toAdd = (Residue) unmodifiableResidues.next();
		  residues.add(toAdd);
	  }
	  
      // Look thru all Residues; delete undesired ones
	  for (Residue res : residues)
	  {
		  String type = res.getName();
		  int seqInt = res.getSequenceInteger();
		  String currChain = res.getChain();
		  boolean isProtein = true;
		  if (AA_NAMES.indexOf(type) < 0) 
		  {
			  isProtein = false;
		  }
		  
          if (seqInt == resno && currChain.equals(chain) && isProtein) 
		  {
			  //System.out.println("Found\tres "+seqInt+", chain "+currChain+", pdb "+pdbcode+"\n");
			  
			  desiredRes = res;
			  cnit = res.getCNIT();
		  }
		  else 
		  {
			  try 
			  {
				  modBeingTrimmed.remove(res);
			  }
			  catch (ResidueException re) 
			  {
				  System.out.println("Can't remove this residue");
			  }
		  }
	  }
	  
	  return modBeingTrimmed; // Should now contain one Residue
  }
//}}}

//{{{ transformModel
//##################################################################################################
  private void transformModel(Model mod, Transform trans) 
  {
	  ModelState modState = mod.getState();
	  Iterator residues = (mod.getResidues()).iterator();
	  while (residues.hasNext()) 
	  {
		  Residue res = (Residue) residues.next();
		  Iterator atoms = (res.getAtoms()).iterator();
		  while (atoms.hasNext()) 
		  {
			  Atom at = (Atom) atoms.next();
			  try 
			  {
				  AtomState atState = modState.get(at);
				  trans.transform(atState);
			  } 
			  catch (AtomException ae) 
			  {
				  System.out.println("atom state not found");
			  }
		  }
	  }
  }
//}}}

//{{{ writeToKin
//##################################################################################################
  public void writeToKin(Scanner pdbidScanner)
  {
	  try
	  {
            Writer w = new FileWriter(kinOut, true);   
            // The second argument is a boolean telling whether to append to the file or not
            PrintWriter out = new PrintWriter(new BufferedWriter(w));
            
            BallAndStickLogic bsl = new BallAndStickLogic();
            bsl.doProtein = true;
            bsl.doBackbone = true;
            bsl.doSidechains = true;
            bsl.doHydrogens = true;
            bsl.colorBy = BallAndStickLogic.COLOR_BY_MC_SC;
            
            Collection mods = cleanFile.getModels();
            Iterator iter = mods.iterator();
            
            while (iter.hasNext())
            {
                  // assume mods.iterator() and pdbidScanner are synchronized in terms of pdbid
                  String thisModelsPdbCode = "unk_";
                  if (pdbidScanner.hasNextLine())
                  {
                      String line = pdbidScanner.nextLine();
                      thisModelsPdbCode = (new Scanner(line)).next();
                  }
                  
                  Model mod = (Model) iter.next();
                  Collection oneresCollection = mod.getResidues(); 
                    // (this should contain only one residue)
                  Residue oneres = (Residue) oneresCollection.iterator().next();
                  
                  String groupname = thisModelsPdbCode+" "+oneres.getName()+
                    " "+oneres.getSequenceNumber();
                    // format: "1amu ARG 211"
                  
                  out.println("@group {"+groupname+"} animate on dominant master= {all residues}");
                  bsl.printKinemage(out, mod, new UberSet(mod.getResidues()), "bluetint");
            }
            
            out.flush();
            w.close();
	   }
	   catch (IOException ex) 
	   {
           System.err.println("An error occurred while writing the kin." + ex);
	   }
  }
//}}}

//{{{ writeToBinKins
//##################################################################################################
  public void writeToBinKins() 
  {
	  int bin = 0;
	  
	  BallAndStickLogic bsl = new BallAndStickLogic();
	  bsl.doProtein = true;
	  bsl.doBackbone = true;
	  bsl.doSidechains = true;
	  bsl.doHydrogens = true;
	  bsl.colorBy = BallAndStickLogic.COLOR_BY_MC_SC;
	  
	  Collection mods = cleanFile.getModels();
	  Iterator iter = mods.iterator();
	  while (iter.hasNext())
	  {
		  Model mod = (Model) iter.next();   // this should contain only one residue
		  
		  bin = whichBin(mod);   // refer to bins as 1,2,..., not 0,1,...
		  
		  if (bin < 999)
		  {
			  // write to appropriate binkin
			  
			  bsl.printKinemage(binkinPrintWriters.get(bin-1), mod, 
				new UberSet(mod.getResidues()), "bluetint");
			  // binkinPrintWriters.get(bin) tells which PrintWriter to use and, 
			  // therefore, which of the #(quasipolarBins) binkins to write to
			  // (used to be "out")
		  }
	  }
 	  
	  // DO NOT "flush" all (quasipolarBins) PrintWriters -- it totally messes up each _bin(b).kin
	  // by repeating each group like 2-3 times.  Who knows why...
	  // (This may've been b/c of the use of the append option in the PrintWriter constructors and
	  // not have anything to do with flushing, as it turns out)
  }
//}}}

//{{{ whichBin, foundN_CA_CB_CZ, binImpl
//##################################################################################################
  public int whichBin(Model mod)
  {
	  // get N,CA,CB,CZ coords
	  AtomState Nstate = null;
	  AtomState CAstate = null;
	  AtomState CBstate = null;
	  AtomState CZstate = null;
	  ArrayList<AtomState> states = new ArrayList<AtomState>();
	  states.add(Nstate);
	  states.add(CAstate);
	  states.add(CBstate);
	  states.add(CZstate);
	  
	  ModelState modState = mod.getState();
	  
	  Residue res = (Residue) mod.getResidues().iterator().next();
	  Iterator iter = res.getAtoms().iterator();
	  while (iter.hasNext() && ! foundN_CA_CB_CZ(states))
	  {
		  Atom currAtom = (Atom) iter.next();
		  
		  try
		  {
			  AtomState currState = modState.get(currAtom);
			  
			  if (currAtom.getName().equals(" N  "))
				  states.set(0, currState);
			  if (currAtom.getName().equals(" CA "))
				  states.set(1, currState);
			  if (currAtom.getName().equals(" CB "))
				  states.set(2, currState);
			  if (currAtom.getName().equals(" CZ "))
				  states.set(3, currState);
			  //else
				  //System.out.println("This atom not N, CA, CB, or CZ");
		  }
		  catch (AtomException ae) 
		  {
			  System.out.println("Problem getting AtomState for Atom "+
				  currAtom.getName());
		  }
	  }
	  
	  // implement bin decision
	  if (! foundN_CA_CB_CZ(states))
	  {
	  	  System.out.println("Couldn't find all of N, CA, CB, or CZ in "+
              "residue "+res.getName()+" "+res.getSequenceInteger());
          return 999;
	  }
	  else
          return binImpl(states);
  }

  public boolean foundN_CA_CB_CZ(ArrayList<AtomState> states)
  {
	  if ((states.get(0) != null) &&
	      (states.get(1) != null) &&
      	  (states.get(2) != null) &&
	      (states.get(3) != null)  )
		  return true;
	  else
	  	  return false;
  }

  public int binImpl(ArrayList<AtomState> states)
  {
	  AtomState N = states.get(0);
	  AtomState CA = states.get(1);
	  AtomState CB = states.get(2);
	  AtomState CZ = states.get(3);
	  
	  // calc N_CA_CB_CZ dihd; comes as -180 to 180 so we add 180 to make it 0 to 360
	  double quasiDihedral = Triple.dihedral(N, CA, CB, CZ) + 180;
	  
	  // place in appropriate bin
	  for (int b = 1; b <= quasipolarBins; b ++)
	  {
		  double binDihdMin = (b - 1) * (360 / quasipolarBins);
		  double binDihdMax = (b    ) * (360 / quasipolarBins);
		  
		  if (quasiDihedral > binDihdMin && quasiDihedral < binDihdMax)
		  {
			  return b;
		  }
	  }
	  
	  return 999;
  }
//}}}

//{{{ makeCzBallKins
//##################################################################################################
  public void makeCzBallKins()
  {
	  // Look through each binkin and, for each line that contains "cz", add it to a
	  // corresponding _bin(b)CZballs.kin under a @balllist
	  
	  try
	  {
		  for (int b = 1; b <= quasipolarBins; b ++)
		  {
			  // prep for output
			  File czBallFile = new File(outPrefixAbsPath+"_bin"+b+"CZballs.kin");
			  Writer w = new FileWriter(czBallFile, true);   
			  PrintWriter czBallPrintWriter = new PrintWriter(new BufferedWriter(w));
			  czBallPrintWriter.println("@balllist {CZ balls} color= yellow radius= 0.05 "+
				  "master= {CZ balls}");
			  
			  // prep input
			  File f = new File(outPrefixAbsPath+"_bin"+b+".kin");
			  FileInputStream fis = new FileInputStream(f);
			  InputStreamReader isr = new InputStreamReader(fis);
			  LineNumberReader lnr = new LineNumberReader(isr);
			  
			  String line;
			  while ((line = lnr.readLine()) != null)
			  {
				  if (line.indexOf(" cz ") > 0 && line.indexOf("}L") > 0)
				  {
					  czBallPrintWriter.println(line);
				  }
			  }
			  lnr.close();
			  isr.close();
			  fis.close();
			  czBallPrintWriter.close();
		  }
	  }
	  catch (IOException ex) 
	  {
		  System.err.println("An error occurred while adding CZ balls" + ex);
	  }
  }
//}}}

//{{{ makeGuanNormalKins, getTriple, normalImpl
//##################################################################################################
  public void makeGuanNormalKins()
  {
	  try
	  {
		  for (int b = 1; b <= quasipolarBins; b ++)
		  {
			  // prep for output
			  File guanNormalFile = new File(outPrefixAbsPath+"_bin"+b+"guans.kin");
			  Writer w = new FileWriter(guanNormalFile, true);   
			  PrintWriter guanNormalPrintWriter = new PrintWriter(new BufferedWriter(w));
			  guanNormalPrintWriter.println("@vectorlist {bin"+b+" guan normals} "+
				  "color= hotpink master= {guan normals}");
			  
			  // prep input
			  File f = new File(outPrefixAbsPath+"_bin"+b+".kin");
			  FileInputStream fis = new FileInputStream(f);
			  InputStreamReader isr = new InputStreamReader(fis);
			  LineNumberReader lnr = new LineNumberReader(isr);
			  
			  String line;
			  String czLine = "";
			  Triple CZ = null;
			  Triple NH1 = null;
			  Triple NH2 = null;
			  while ((line = lnr.readLine()) != null)
			  {
				  // must get Triples for CZ, NH1, NH2
				  
				  Scanner s = new Scanner(line);
				  
				  if (line.indexOf(" cz ") > 0 && line.indexOf("}P") > 0) 
				  {
					  // "}P" starts a polyline in kinemages
					  CZ = getTriple(line);
				  	  czLine = line;
				  }
				  if (line.indexOf(" nh1") > 0)
					  NH1 = getTriple(line);
				  if (line.indexOf(" nh2") > 0)
					  NH2 = getTriple(line);
				  
				  if (CZ != null && NH1 != null && NH2 != null)
				  {
					  // print result
					  guanNormalPrintWriter.println(czLine);
					  guanNormalPrintWriter.println(
						  normalImpl(CZ, NH1, NH2, czLine));
					  
					  // reset CZ, NH1, NH2 for next residue in _bin(b).kin
					  czLine = null;
					  CZ = null;
					  NH1 = null;
			  		  NH2 = null;
				  }
			  }
			  lnr.close();
			  isr.close();
			  fis.close();
			  guanNormalPrintWriter.close();
		  }
	  }
	  catch (IOException ex) 
	  {
		  System.err.println("An error occurred while adding guanidinium normals" + ex);
	  }
  }

  public Triple getTriple(String line)
  {
	  // line in kin format
	  
	  Scanner s = new Scanner(line);
	  s.next();	// skip {pdbid
	  s.next();	// skip atom name
	  s.next();	// skip residue type name
	  s.next();	// skip chain
	  s.next();	// skip resno
	  s.next();	// skip B##.##}L or P
	  
	  return new Triple(s.nextDouble(), s.nextDouble(), s.nextDouble());
  }

  public String normalImpl(Triple CZ, Triple NH1, Triple NH2, String czLine)
  {
	  // Calc normal vector
	  Triple CZ_NH1 = new Triple(NH1.getX() - CZ.getX(),
				     NH1.getY() - CZ.getY(),
				     NH1.getZ() - CZ.getZ());
	  Triple CZ_NH2 = new Triple(NH2.getX() - CZ.getX(),
				     NH2.getY() - CZ.getY(),
				     NH2.getZ() - CZ.getZ());
	  Triple normal = CZ_NH1.cross(CZ_NH2);
      
      // Scale normal
      double mag = Math.sqrt(Math.pow(normal.getX(), 2) + 
                             Math.pow(normal.getY(), 2) + 
                             Math.pow(normal.getZ(), 2) );
      Triple normalScaled = new Triple(normal.getX() / mag,
                                       normal.getY() / mag,
                                       normal.getZ() / mag );
      // Move back so tail is on atom 2; now have guanine normal (gn) vector
	  Triple gn = new Triple(
		  CZ.getX() + normalScaled.getX(),
		  CZ.getY() + normalScaled.getY(),
		  CZ.getZ() + normalScaled.getZ());
	  
	  String line2 = "{norm"+czLine.substring(5, czLine.indexOf("}")+1)  +"L "+
		  df.format(gn.getX())+" "+
		  df.format(gn.getY())+" "+
		  df.format(gn.getZ());
	  
	  return line2;
  }
//}}}

//{{{ assembleBinKin, writeProt, writeBalls, writeGuans
//##################################################################################################
  public void assembleBinKin() 
  {
	  try
	  {
		  // Open _bins.kin writer
		  // Delete the _bins.kin if it already exists so we don't just append to it...
		  // (I think I was having this problem earlier)
		  if ((new File(outPrefixAbsPath+"_bins.kin")).exists())
			  (new File(outPrefixAbsPath+"_bins.kin")).delete();
		  BufferedWriter out = new BufferedWriter(new FileWriter(
			  outPrefixAbsPath+"_bins.kin", true));
		  
		  out.write("@kinemage");
		  out.newLine();
		  out.write("@master {all bins}");
		  out.newLine();
		  out.write("@master {CZ balls}");
		  out.newLine();
		  out.write("@master {guan normals}");
		  out.newLine();
		  
		  for (int b = 1; b <= quasipolarBins; b ++)
		  {
			  writeProt(b, out);
			  writeBalls(b, out);
			  writeGuans(b, out);
			  
			  // delete the temp/intermediate files used for assembling this bin of 
			  // the binkin
			  (new File(outPrefixAbsPath+"_bin"+b+".kin")).delete();
			  (new File(outPrefixAbsPath+"_bin"+b+"CZballs.kin")).delete();
			  (new File(outPrefixAbsPath+"_bin"+b+"guans.kin")).delete();
		  }
		  
		  out.close();
	  }
	  catch (IOException ex) 
	  {
		  System.err.println("An error occurred while writing "+outPrefixAbsPath+
			  "_bins.kin " + ex);
	  }
  }

  public void writeProt(int b, BufferedWriter out) 
  {
	try
	{
		  // write this bin's vectorlists to _bins.kin
		  
		  Scanner pdbidScanner = new Scanner(new File(outPrefixAbsPath+".csv"));
		  
		  LineNumberReader lnrProt = new LineNumberReader(new InputStreamReader(new 
			  FileInputStream(new File(outPrefixAbsPath+"_bin"+b+".kin"))));
		  
		  String line, lineToPrint, pdbidLine, pdbid, currResNo, prevResNo;
		  pdbid = "";
		  prevResNo = "";
		  
		  while ((line = lnrProt.readLine()) != null)
		  {
			  if (line.indexOf("@") >= 0)
			  {
				  // it's @group, @vectorlist, @master, etc.
				  out.write(line);
				  out.newLine();
			  }
			  else
			  {
				  // it's an atom or point description line
				  currResNo = line.substring(11, 15);
				  if (! currResNo.equals(prevResNo))
				  {
					  // update prevResNo and get new pdbid
					  prevResNo = currResNo;
					  if (pdbidScanner.hasNextLine())
					  {
						  pdbidLine = pdbidScanner.nextLine();
						  pdbid = (new Scanner(pdbidLine)).next();
					  }
				  }
				  
				  lineToPrint = line.substring(0,1)+pdbid+" "+line.substring(1);
				  out.write(lineToPrint);
				  out.newLine();
			  }
		  }
		  out.flush();
		  lnrProt.close();
	  }
	  catch (IOException ex) 
	  {
		  System.err.println("An error occurred while writing "+outPrefixAbsPath+
			  "_bins.kin" + ex);
	  }
  }

  public void writeBalls(int b, BufferedWriter out) 
  {
	  try
	  {
		  // write this bin's balllist to _bins.kin
		  
		  Scanner pdbidScanner = new Scanner(new File(outPrefixAbsPath+".csv"));
		  
		  LineNumberReader lnrBalls = new LineNumberReader(new InputStreamReader(new 
			  FileInputStream(new File(outPrefixAbsPath+"_bin"+b+"CZballs.kin"))));
		  
		  String line, lineToPrint, pdbidLine, pdbid, currResNo, prevResNo;
		  pdbid = "";
		  prevResNo = "";
		  
		  while ((line = lnrBalls.readLine()) != null)
		  {
			  if (line.indexOf("@") >= 0)
			  {
				  // it's @balllist
				  out.write(line);
				  out.newLine();
			  }
			  else
			  {
				  // it's an single ball description line
				  currResNo = line.substring(11, 15);
				  if (! currResNo.equals(prevResNo))
				  {
					  // update prevResNo and get new pdbid
					  prevResNo = currResNo;
					  if (pdbidScanner.hasNextLine())
					  {
						  pdbidLine = pdbidScanner.nextLine();
						  pdbid = (new Scanner(pdbidLine)).next();
					  }
				  }
				  
				  lineToPrint = line.substring(0,1)+pdbid+" "+line.substring(1);
				  out.write(lineToPrint);
				  out.newLine();
			  }
		  }
		  out.flush();
		  lnrBalls.close();
	  }
	  catch (IOException ex) 
	  {
		  System.err.println("An error occurred while writing "+outPrefixAbsPath+
			  "_bins.kin" + ex);
	  }
  }

  public void writeGuans(int b, BufferedWriter out) 
  {
	  try
	  {
		  // write this bin's guans vectorlist to _bins.kin
		  
		  Scanner pdbidScanner = new Scanner(new File(outPrefixAbsPath+".csv"));
		  
		  LineNumberReader lnrBalls = new LineNumberReader(new InputStreamReader(new 
			  FileInputStream(new File(outPrefixAbsPath+"_bin"+b+"guans.kin"))));
		  
		  String line, lineToPrint, pdbidLine, pdbid, currResNo, prevResNo;
		  pdbid = "";
		  prevResNo = "";
		  
		  while ((line = lnrBalls.readLine()) != null)
		  {
			  if (line.indexOf("@") >= 0)
			  {
				  // it's a guans @vectorlist
				  out.write(line);
				  out.newLine();
			  }
			  else
			  {
				  // it's an single ball description line
				  currResNo = line.substring(11, 15);
				  if (! currResNo.equals(prevResNo))
				  {
					  // update prevResNo and get new pdbid
					  prevResNo = currResNo;
					  if (pdbidScanner.hasNextLine())
					  {
						  pdbidLine = pdbidScanner.nextLine();
						  pdbid = (new Scanner(pdbidLine)).next();
					  }
				  }
				  
				  lineToPrint = line.substring(0,1)+pdbid+" "+line.substring(1);
				  out.write(lineToPrint);
				  out.newLine();
			  }
		  }
		  out.flush();
		  lnrBalls.close();
	  }
	  catch (IOException ex) 
	  {
		  System.err.println("An error occurred while writing "+outPrefixAbsPath+
			  "_bins.kin" + ex);
	  }
  }
//}}}

//{{{ javaGunzip [unused]
//##################################################################################################
  private File javaGunzip(String inputfilenamepdb)
  {
	  // This is an implementation of gunzip in Java that I found at 
	  // http://www.devshed.com/c/a/Java/GZIPping-with-Java/3/
	  
	  // Open the gzip file
	  GZIPInputStream gzipInputStream;
	  try 
	  {
		  gzipInputStream = new GZIPInputStream(new FileInputStream(inputfilenamepdb+".gz"));
		  
		  // Open the output file
		  String outFilename = inputfilenamepdb;
		  OutputStream out;
		  try 
		  {
			  out = new FileOutputStream(outFilename);
			  
			  // Transfer bytes from the compressed file to the output file
			  byte[] buf = new byte[1024];
			  int len;
			  while ((len = gzipInputStream.read(buf)) > 0) 
			  {
				  out.write(buf, 0, len);
			  }
			  
			  // Close the file and stream
			  gzipInputStream.close();
			  out.close();
			  
			  return new File(outFilename);
		  } 
		  catch (IOException e) 
		  {
			  System.out.println("Can't file for gunzipped output");
		  }
	  } 
	  catch (IOException e) 
	  {
		  System.out.println("Can't open .pdb.gz file");
	  }
	  
	  return null;
  }
//}}}

} // end class
