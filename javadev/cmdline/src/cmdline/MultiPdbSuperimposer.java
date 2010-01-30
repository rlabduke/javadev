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
    boolean useFlipPdbs;
    boolean useOrigAndFlipPdbs;
    
    boolean preppedOutFiles;
    File kinOut;
    ArrayList<PrintWriter> binkinPrintWriters;
	ArrayList<PrintWriter> guankinPrintWriters;
    
    CoordinateFile cleanFile;
    CoordinateFile cleanFileFlip;
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
        kinoption          = true;
        dobinkin           = false;
        doCZballs          = false;
        doguannormals      = false;
        quasipolarBins     = 10;
        this.bbAtoms       = "CA-N-C";
        preppedOutFiles    = false;
        useFlipPdbs        = false;
        useOrigAndFlipPdbs = false;
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
            System.out.println("out prefix:\t"+outPrefixAbsPath);
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
        else if (flag.equals("-argbins"))
        {
            kinoption = false;
            dobinkin = true;
            doCZballs = true;
            doguannormals = true;
        }
        else if (flag.equals("-bbatoms"))
        {
            if (param.equals("CA-N-C") || param.equals("CA-C-N") || param.equals("CA-C-O") ||
                param.equals("CZ-NH1-NH2") || param.equals("CG-CB-CD"))
                this.bbAtoms = param;
            else
                System.out.println("Expected CA-N-C, CA-C-N, CA-C-O, CZ-NH1-NH2, or CG-CB-CD after -bbatoms=");
        }
        else if (flag.equals("-flip") || flag.equals("-flipped"))
        {
            useFlipPdbs = true;
        }
        else if (flag.equals("-preandpostflip") || flag.equals("-origandflip"))
        {
            useOrigAndFlipPdbs = true;
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
        
        else if (dobinkin) // either this or kinoption
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
        
        if (doguannormals)
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
        
        preppedOutFiles = true;
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
            cleanFileFlip = new CoordinateFile();
			
            System.out.println("PDB list:\t"+inPdbListFileAbsPath);
            
            Scanner pdbidScanner = new Scanner(new File(inPdbListFileAbsPath));
			  // for getting pdbcode to put in @group name
			
			Scanner inListScanner = new Scanner(new File(inPdbListFileAbsPath));
			
			int count = 0;
			String line = "";
			while (inListScanner.hasNextLine())   // for each pdb + resno combination
			{
				  line = inListScanner.nextLine();
				  Scanner lineScanner = new Scanner(line);
				  pdbcode = lineScanner.next();        //System.out.println("pdbcode: "+pdbcode);
				  String temp = lineScanner.next();
                  boolean hasChain;
                  try {
                      // In case there's no chain id for this one
                      int some_integer = Integer.parseInt(temp);
                      hasChain = false; 
                  }
                  catch (NumberFormatException nfe) {
                      hasChain = true;
                  }
                  if (hasChain) {
                      // It's the chain id, not the resno
                      chain = temp;
                      resno = Integer.parseInt(lineScanner.next());
				  }
                  else { // if (!hasChain)
                      chain = "A";
                      resno = Integer.parseInt(temp);
                  }
                  if (lineScanner.hasNext()) {
					  // i.e. altconf is not the null string ""
					  altconf = lineScanner.next();
				  }
				  else altconf = "no_alt_conf";
				  lineScanner.close();
				  
                  // 'true' or 'false' in method calls below means use flipped or not flipped coords
                  // This entire pdb (all models and residues)
				  //CoordinateFile thisPdbsCoordFile = getCoordFile();
				  CoordinateFile thisPdbsCoordFile     = null;
                  CoordinateFile thisPdbsFlipCoordFile = null;
                  if (useOrigAndFlipPdbs)
                  {
                      thisPdbsCoordFile     = getCoordFile(false);
                      thisPdbsFlipCoordFile = getCoordFile(true);
                      
                      // Trims model and adds any single residues that match resno + chain + pdbcode
                      // criteria to cleanFile in the form of single-residue models
                      overlayResidues(thisPdbsCoordFile    , built, false);
                      overlayResidues(thisPdbsFlipCoordFile, built, true);
                  }
                  else if (useFlipPdbs)
                  {
                      thisPdbsFlipCoordFile = getCoordFile(true);
                      
                      // Trims model and adds any single residues that match resno + chain + pdbcode
                      // criteria to cleanFile in the form of single-residue models
                      overlayResidues(thisPdbsFlipCoordFile, built, true);
                  }
                  else
                  {
                      thisPdbsCoordFile = getCoordFile(false);
                      
                      // Trims model and adds any single residues that match resno + chain + pdbcode
                      // criteria to cleanFile in the form of single-residue models
                      overlayResidues(thisPdbsCoordFile    , built, false);
                  }
                  
				  
				  count ++;
				  if (count == 100) 
				  // 100 worked for chi1-4_anyrot_begs.specif_ch.noalts.sql; 250, 500 did not
				  {
					  if (!preppedOutFiles)
                          prepOutFiles();
                      
                      if (kinoption)	  writeToKin(pdbidScanner);
					  else if (dobinkin) 
                      {
                          if (useOrigAndFlipPdbs)  
                          {
                              writeToBinKins(true);
                              writeToBinKins(false);
                          }
                          else if (useFlipPdbs)
                              writeToBinKins(true);
                          else
                              writeToBinKins(false);
                      }
					  
				  	  count = 0;
				  	  cleanFile = new CoordinateFile();
				  }
				  
			} // done looking through pdb files in list
			inListScanner.close();
			
			if (!preppedOutFiles)   prepOutFiles();
            if (kinoption)		    writeToKin(pdbidScanner);
			else if (dobinkin)
			{
				//writeToBinKins();
                if (useOrigAndFlipPdbs)  
                {
                    writeToBinKins(true);
                    writeToBinKins(false);
                }
                else if (useFlipPdbs)
                    writeToBinKins(true);
                else
                    writeToBinKins(false);
                
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
  private CoordinateFile getCoordFile(boolean flipThisTime)
  {
	  String inputfilenamepdb = "/home/keedy/PDBs/neo500copy/"+pdbcode+".pdb";
      boolean inNeo500 = true;
      
      CoordinateFile coordFile = new CoordinateFile();	// what we will return
	  reader = new PdbReader();
	  reader.setUseSegID(false);
	  
	  // Try my ~/PDBs directory for input pdb file in ####H.pdb format (v3.0)
      //if (useFlipPdbs)
      if (flipThisTime)
          inputfilenamepdb = "/home/keedy/PDBs/"+pdbcode+"/"+pdbcode+"Hflip.pdb";
      else
          inputfilenamepdb = "/home/keedy/PDBs/"+pdbcode+"/"+pdbcode+"H.pdb";
      try
      {
          FileInputStream pdbFileInputStream = new FileInputStream(inputfilenamepdb);
          coordFile = reader.read( (InputStream) pdbFileInputStream);
          
          pdbFileInputStream.close();
      }
      catch (IOException e1)
      {
          // Next, try ~/PDBs/neo500copy directory (v3.0)
          // Files in .pdb.gz format --> use a gzipInputStream to make CoordinateFile
          try
          {
              GZIPInputStream gzipInputStream = new GZIPInputStream(
                  new FileInputStream(inputfilenamepdb+".gz"));
              coordFile = reader.read( (InputStream) gzipInputStream);
              
              gzipInputStream.close();
          }
          catch (IOException e2)
          {
              // Next, try my ~/PDBs directory for input pdb file in pdb####H.ent format (v2.2)
              inputfilenamepdb = "/home/keedy/PDBs/"+pdbcode+"/pdb"+pdbcode+"H.ent";
              try
              {
                  FileInputStream pdbFileInputStream = new FileInputStream(inputfilenamepdb);
                  coordFile = reader.read( (InputStream) pdbFileInputStream);
                  
                  pdbFileInputStream.close();
              }
              catch (IOException e3)
              {
                  System.out.println("Can't open ####.pdb.gz, ####H.pdb, or pdb####H.ent file");
              }
          }
      }
      
	  return coordFile;
  }
//}}}

//{{{ overlayResidues
//##################################################################################################
  private void overlayResidues(CoordinateFile thisPdbsCoordFile, Builder built, boolean flip)
  {
	  Iterator models = (thisPdbsCoordFile.getModels()).iterator();
	  
	  AtomState modN   = null;
	  AtomState modCA  = null;
	  AtomState modC   = null;
	  AtomState modO   = null;
	  AtomState modCZ  = null;
      AtomState modNH1 = null;
      AtomState modNH2 = null;
      AtomState modCG  = null;
      AtomState modCB  = null;
      AtomState modCD  = null;
      
      Triple refN   = new Triple(-0.35, -1.46, 0);
	  Triple refCA  = new Triple(0, 0, 0);
	  Triple refC   = new Triple(1.5, 0, 0);
	  Triple refO   = new Triple(2.15, 1, 0);
	  Triple refCZ  = new Triple(0, 0, 0);
      Triple refNH1 = new Triple(0, 1.326, 0);
      Triple refNH2 = new Triple(1.326*Math.cos(30/360), -1.326*Math.sin(30/360), 0);
      Triple refCG  = new Triple(0, 0, 0);
      Triple refCB  = new Triple(0, 1.52, 0);
      Triple refCD  = new Triple(1.52*Math.cos(21.3/360), 1.52*Math.sin(21.3/360), 0);
      
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
			  
              //System.out.println(desiredRes.toString());
              
			  if (bbAtoms.equals("CA-N-C"))
              {
                  modCA = modOneResState.get(desiredRes.getAtom(" CA "));
                  modN  = modOneResState.get(desiredRes.getAtom(" N  "));
                  modC  = modOneResState.get(desiredRes.getAtom(" C  "));
                  
                  Transform dock3pointCANC = built.dock3on3(refCA, refN, refC, modCA, modN, modC);
			      transformModel(modOneRes, dock3pointCANC);
              }
              if (bbAtoms.equals("CA-C-N"))
              {
                  modCA = modOneResState.get(desiredRes.getAtom(" CA "));
                  modC  = modOneResState.get(desiredRes.getAtom(" C  "));
                  modN  = modOneResState.get(desiredRes.getAtom(" N  "));
                  
                  Transform dock3pointCACN = built.dock3on3(refCA, refC, refN, modCA, modC, modN);
			      transformModel(modOneRes, dock3pointCACN);
              }
              if (bbAtoms.equals("CA-C-O"))
              {
                  modCA = modOneResState.get(desiredRes.getAtom(" CA "));
                  modC  = modOneResState.get(desiredRes.getAtom(" C  "));
                  modO  = modOneResState.get(desiredRes.getAtom(" O  "));
                  
                  Transform dock3pointCACO = built.dock3on3(refCA, refC, refO, modC, modCA, modO);
			      transformModel(modOneRes, dock3pointCACO);
              }
              if (bbAtoms.equals("CZ-NH1-NH2"))
              {
                  modCZ  = modOneResState.get(desiredRes.getAtom(" CZ "));
                  modNH1 = modOneResState.get(desiredRes.getAtom(" NH1"));
                  modNH2 = modOneResState.get(desiredRes.getAtom(" NH2"));
                  
                  Transform dock3pointCZNH1NH2 = built.dock3on3(refCZ, refNH1, refNH2, modCZ, modNH1, modNH2);
			      transformModel(modOneRes, dock3pointCZNH1NH2);
              }
              if (bbAtoms.equals("CG-CB-CD"))
              {
                  modCG  = modOneResState.get(desiredRes.getAtom(" CG "));
                  modCB  = modOneResState.get(desiredRes.getAtom(" CB "));
                  modCD  = modOneResState.get(desiredRes.getAtom(" CD "));
                  
                  Transform dock3pointCGCBCD = built.dock3on3(refCG, refCB, refCD, modCG, modCB, modCD);
			      transformModel(modOneRes, dock3pointCGCBCD);
              }
		  }
		  catch (AtomException ae) 
		  {
			  System.out.println("a mod atom wasn't found");
		  }
		  
		  if (flip)
              cleanFileFlip.add(modOneRes);	// Done with this model
          else
              cleanFile.add(modOneRes);	    // Done with this model
		  
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
		  if (AA_NAMES.indexOf(type) < 0 && AA_NAMES.indexOf("A"+type) < 0) 
		  {
			  // To allow use to find a one-Residue Model even if type = 
              // AARG instead of ARG, e.g
              isProtein = false;
		  }
		  
          //System.out.println("currChain: '"+currChain+"'");
          
          if (seqInt == resno && currChain.equals(chain) && isProtein) 
		  {
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
	  
      modBeingTrimmed.setName(pdbcode+" Arg"+resno);
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
            bsl.doMainchain = true;
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
  public void writeToBinKins(boolean flip) 
  {
	  int bin = 0;
	  
	  BallAndStickLogic bsl = new BallAndStickLogic();
	  bsl.doProtein = true;
	  bsl.doMainchain = true;
	  bsl.doSidechains = true;
	  bsl.doHydrogens = true;
	  bsl.colorBy = BallAndStickLogic.COLOR_BY_MC_SC;
	  
	  Collection mods;
      if (flip)
          mods = cleanFileFlip.getModels();
	  else
          mods = cleanFile.getModels();
      
      Iterator iter = mods.iterator();
	  while (iter.hasNext())
	  {
		  Model mod = (Model) iter.next();   // this should contain only one residue
		  
          
          //System.out.println("Model name: \t"+mod.toString());
          //Iterator it = (mod.getResidues()).iterator();
          //while (it.hasNext())
          //{
          //    Residue r = (Residue) it.next();
          //    System.out.println("Residue name: \t"+r.toString());
          //}
          
          
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

//{{{ makeGuanNormalKins, getTriple, getTripleForAltConf, hasChain, normalImpl
//##################################################################################################
  public void makeGuanNormalKins()
  {
	  try
	  {
		  for (int b = 1; b <= quasipolarBins; b ++)
		  {
			  // Prep for output
			  File guanNormalFile = new File(outPrefixAbsPath+"_bin"+b+"guans.kin");
			  Writer w = new FileWriter(guanNormalFile, true);   
			  PrintWriter guanNormalPrintWriter = new PrintWriter(new BufferedWriter(w));
			  guanNormalPrintWriter.println("@vectorlist {bin"+b+" guan normals} "+
                  "color= hotpink master= {guan normals}");
			  
			  // Prep input
			  File f = new File(outPrefixAbsPath+"_bin"+b+".kin");
			  FileInputStream fis = new FileInputStream(f);
			  InputStreamReader isr = new InputStreamReader(fis);
			  LineNumberReader lnr = new LineNumberReader(isr);
			  
              String line2;
              while ((line2 = lnr.readLine()) != null && 
                  line2.indexOf("@vectorlist {protein sc}") < 0)
              {
                  // Scan to first residue to avoid getting "hybrids" 
                  // (NH1 from one residue, NH2 from previous one written)
              }
              
			  String line;
			  String czLine = "";
              //  String nh1Line = "";
              //  String nh2Line = "";
			  Triple CZ = null;
			  Triple NH1 = null;
			  Triple NH2 = null;
			  while ((line = lnr.readLine()) != null)
			  {
				  // Must get Triples for CZ, NH1, NH2
                  // Accept only alt conf a (if a and b exist)
				  if (line.indexOf(" cz ") > 0 && line.indexOf("barg") < 0 && line.indexOf("}P") > 0)
                  {
                      // "}P" starts a polyline in kinemages
                      CZ = getTriple(line);
				  	  czLine = line;
				  }
				  else if (line.indexOf(" nh1") > 0 && line.indexOf("barg") < 0)
                  {
					  NH1 = getTriple(line);
                      //nh1Line = line;
				  }
                  else if (line.indexOf(" nh2") > 0 && line.indexOf("barg") < 0)
                  {
					  NH2 = getTriple(line);
                      //nh2Line = line;
				  }
				  
                  if (CZ != null && NH1 != null && NH2 != null)
				  {
					  // FOR TESTING
                      //guanNormalPrintWriter.println("@balllist {bin"+b+" guan normals} "+
                      //    "color= red master= {guan normals}");
					  //guanNormalPrintWriter.println(nh1Line);
                      //guanNormalPrintWriter.println(nh2Line);
                      //guanNormalPrintWriter.println("@vectorlist {bin"+b+" guan normals} "+
                      //    "color= hotpink master= {guan normals}");
                      
                      // Print result
					  guanNormalPrintWriter.println(czLine);
					  guanNormalPrintWriter.println(normalImpl(CZ, NH1, NH2, czLine));
					  
					  // Reset CZ, NH1, NH2 for next residue in _bin(b).kin
					  czLine = null;
                      //  nh1Line = null;
                      //  nh2Line = null;
					  CZ = null;
					  NH1 = null;
			  		  NH2 = null;
                      
                      while ((line2 = lnr.readLine()) != null && 
                          line2.indexOf("@vectorlist {protein sc}") < 0)
                      {
                          // Scan to next residue to avoid getting "hybrids" 
                          // (NH1 from one residue, NH2 from previous one written)
                      }
                      
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
	  // line is in kin format
	  
	  if (line.substring(5,6).equals("a") || line.substring(5,6).equals("b"))
          return getTripleForAltConf(line); // e.g. "aarg" instead of " arg"
      
      Scanner s = new Scanner(line);
	  s.next();	    // skip {pdbid
	  s.next();	    // skip atom name
	  s.next();	    // skip residue type name
	  if (hasChain(line))
          s.next();	// skip chain
	  s.next();	    // skip resno
	  s.next();	    // skip B##.##}L or P
	  
      return new Triple(s.nextDouble(), s.nextDouble(), s.nextDouble());
  }

  public Triple getTripleForAltConf(String line)
  {
      //e.g.:
      //{ n  aarg a 247  B44.37}P -0.338 -1.411 0 
      //{ ca aarg a 247  B43.52}L 'aa' 0 0 0
      
      String coords;
      if (line.indexOf("}P") >= 0)
      {
          coords = line.substring(line.indexOf("}P"));
      }
      else //if (line.indexOf("}L") >= 0), or if there's no L there (opt'l in kin format)
      {
          if (line.substring(5,6).equals("a"))
              coords = line.substring(line.indexOf("'aa'"));
          else // if (line.substring(5,6).equals("b"))
              coords = line.substring(line.indexOf("'bb'"));
      }
      
      Scanner s = new Scanner(coords);
      s.next(); // skip 'aa' or 'bb'
      return new Triple(s.nextDouble(), s.nextDouble(), s.nextDouble());
  }

  public boolean hasChain(String line)
  {
      Scanner s2 = new Scanner(line);
	  s2.next();	// skip {pdbid
      s2.next();	// skip atom name
	  s2.next();	// skip residue type name
      
      // See if there's no chain and the next entry is a resno ('try')
      // or if the chain is there ('catch')
      boolean hasChain;
      try
      {
          String temp = s2.next();
          int some_integer = Integer.parseInt(temp);
          hasChain = false;
      }
      catch (NumberFormatException nfe)
      {
          hasChain = true;
      }
      
      return hasChain;
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
      //double mag = Math.sqrt(Math.pow(normal.getX(), 2) + 
      //                       Math.pow(normal.getY(), 2) + 
      //                       Math.pow(normal.getZ(), 2) );
      //Triple normalScaled = new Triple(normal.getX() / mag,
      //                                 normal.getY() / mag,
      //                                 normal.getZ() / mag );
      Triple normalScaled = normal.mult( 1/normal.mag() );
      
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
			  
			  // Delete the temp/intermediate files used for assembling this bin of 
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
		  // Write this bin's vectorlists to _bins.kin
		  
          // A temporary fix -- if #bins > 1, I don't know how to get the right pdbid
          // to each pointid
		  Scanner pdbidScanner = null;
          if (quasipolarBins == 1)
               pdbidScanner = new Scanner(new File(inPdbListFileAbsPath));
		  
		  LineNumberReader lnrProt = new LineNumberReader(new InputStreamReader(new 
			  FileInputStream(new File(outPrefixAbsPath+"_bin"+b+".kin"))));
		  
		  String line, lineToPrint, pdbidLine, pdbid, currResNo, prevResNo;
		  pdbid = "some pdb";//"";
		  prevResNo = "";
		  
		  while ((line = lnrProt.readLine()) != null)
		  {
			  if (line.indexOf("@") >= 0)
			  {
				  // It's @group, @vectorlist, @master, etc.
				  out.write(line);
				  out.newLine();
			  }
			  else
			  {
				  // It's an atom or point description line
				  currResNo = line.substring(11, 15);
				  if (! currResNo.equals(prevResNo))
				  {
					  // Update prevResNo and get new pdbid
					  prevResNo = currResNo;
					  if (quasipolarBins == 1 && pdbidScanner != null)
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
		  // Write this bin's balllist to _bins.kin
		  
          // A temporary fix -- if #bins > 1, I don't know how to get the right pdbid
          // to each pointid
		  Scanner pdbidScanner = null;
          if (quasipolarBins == 1)
               pdbidScanner = new Scanner(new File(inPdbListFileAbsPath));
		  
		  LineNumberReader lnrBalls = new LineNumberReader(new InputStreamReader(new 
			  FileInputStream(new File(outPrefixAbsPath+"_bin"+b+"CZballs.kin"))));
		  
		  String line, lineToPrint, pdbidLine, pdbid, currResNo, prevResNo;
		  pdbid = "some pdb";//"";
		  prevResNo = "";
		  
		  while ((line = lnrBalls.readLine()) != null)
		  {
			  if (line.indexOf("@") >= 0)
			  {
				  // It's @balllist
				  out.write(line);
				  out.newLine();
			  }
			  else
			  {
				  // It's an single ball description line
				  currResNo = line.substring(11, 15);
				  if (! currResNo.equals(prevResNo))
				  {
					  // update prevResNo and get new pdbid
					  prevResNo = currResNo;
					  if (quasipolarBins == 1 && pdbidScanner != null)
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
		  // Write this bin's guans vectorlist to _bins.kin
		  
		  // A temporary fix -- if #bins > 1, I don't know how to get the right pdbid
          // to each pointid
		  Scanner pdbidScanner = null;
          if (quasipolarBins == 1)
               pdbidScanner = new Scanner(new File(inPdbListFileAbsPath));
		  
		  LineNumberReader lnrBalls = new LineNumberReader(new InputStreamReader(new 
			  FileInputStream(new File(outPrefixAbsPath+"_bin"+b+"guans.kin"))));
		  
		  String line, lineToPrint, pdbidLine, pdbid, currResNo, prevResNo;
		  pdbid = "";
		  prevResNo = "";
		  
		  while ((line = lnrBalls.readLine()) != null)
		  {
			  if (line.indexOf("@") >= 0)
			  {
				  // It's a guans @vectorlist
				  out.write(line);
				  out.newLine();
			  }
			  else
			  {
				  // It's an single ball description line
				  currResNo = line.substring(11, 15);
				  if (! currResNo.equals(prevResNo))
				  {
					  // Update prevResNo and get new pdbid
					  prevResNo = currResNo;
					  if (quasipolarBins == 1 && pdbidScanner != null)
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
