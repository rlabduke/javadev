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

public class MultiMADSuperimposer
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
    
    boolean preppedOutFiles;
    File kinOut;
    ArrayList<PrintWriter> binkinPrintWriters;
	ArrayList<PrintWriter> guankinPrintWriters;
    
    String pdbid;
	String chain;
    String restype;
    int resno;
    String altconf;
	String atomname;
    String element;
//}}}

//{{{ main
//##################################################################################################
	public static void main(String[] args)
	{
		MultiMADSuperimposer imposer = new MultiMADSuperimposer();
		imposer.parseArgs(args);
		imposer.build();
	}
//}}}

//{{{ Constructor
//##################################################################################################
	public MultiMADSuperimposer()
	{
		// Set defaults
        kinoption       = true;
        dobinkin        = false;
        doCZballs       = false;
        doguannormals   = false;
        quasipolarBins  = 10;
        this.bbAtoms = "CA-N-C";
        preppedOutFiles = false;
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
                param.equals("CZ-NH1-NH2"))
                this.bbAtoms = param;
            else
                System.out.println("Expected CA-N-C, CA-C-N, CA-C-O, or CZ-NH1-NH2 after -bbatoms=");
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
			System.out.println("inPdbListFileAbsPath: "+inPdbListFileAbsPath);
            
            Scanner inListScanner = new Scanner(new File(inPdbListFileAbsPath));
            ArrayList<String> lines  = new ArrayList<String>();
            ArrayList<Triple> newCoords = new ArrayList<Triple>();
            boolean firstLine = true;
            String prevResSig = "";
			while (inListScanner.hasNextLine())   // for each atom in query results
			{
			    String line = inListScanner.nextLine();
				
                
                System.out.println("line = "+line);
                
                
                Scanner lineScanner = new Scanner(line).useDelimiter(";");
				pdbid  = lineScanner.next();
				chain    = lineScanner.next();
				restype  = lineScanner.next();
                resno    = Integer.parseInt(lineScanner.next());
                altconf  = lineScanner.next();
				atomname = lineScanner.next();
                lineScanner.close();
				
                String resSig = pdbid+chain+restype+resno;
                if (firstLine)
                {
                    prevResSig = resSig;
                    firstLine = false;
                }
                if (!resSig.equals(prevResSig)) // if entering next residue
                {
                    // Overlay completed "residue"
                    newCoords = overlayResidue(lines);
                    
                    // Make a model that BallAndStickLogic will use
                    Model mod = new Model(pdbid+" "+chain+" "+restype+" "+resno);
                    
                    // Write that "residue" to proper kin
                    if (!preppedOutFiles)   prepOutFiles();
                    if (kinoption)          writeToKin(lines, newCoords, mod);
                    //else if (dobinkin)      writeToBinKin();
                    
                    lines  = new ArrayList<String>();
                }
                
                prevResSig = resSig;
                lines.add(line);
                
			} // done looking through atoms in query results
			inListScanner.close();
			
			//if (dobinkin)
			//{
			//	if (doCZballs)		makeCzBallKins();
			//	if (doguannormals)	makeGuanNormalKins();
			//	assembleBinKin();
			//}
		}
        catch (IOException e)
        {
            System.err.println("IO Exception thrown " + e.getMessage());
        }
	}
//}}}

//{{{ overlayResidue, getAtomCoords, transformModel
//##################################################################################################
  private ArrayList<Triple> overlayResidue(ArrayList<String> lines)
  {
	  ArrayList<Triple> newCoords = new ArrayList<Triple>();
      // transformed coordinates that we will return
      
      Builder built = new Builder();
      
      Triple modN   = null;
	  Triple modCA  = null;
	  Triple modC   = null;
	  Triple modO   = null;
	  Triple modCZ  = null;
      Triple modNH1  = null;
      Triple modNH2  = null;
      
      Triple refN   = new Triple(-0.35, -1.46, 0);
	  Triple refCA  = new Triple(0, 0, 0);
	  Triple refC   = new Triple(1.5, 0, 0);
	  Triple refO   = new Triple(2.15, 1, 0);
	  Triple refCZ  = new Triple(0, 0, 0);
      Triple refNH1 = new Triple(0, 1.326, 0);
      Triple refNH2 = new Triple(1.326*Math.cos(30/360), -1.326*Math.sin(30/360), 0);
      
      if (bbAtoms.equals("CA-N-C"))
      {
          modCA = getAtomCoords(" CA ", lines);
          modN  = getAtomCoords(" N  ", lines);
          modC  = getAtomCoords(" C  ", lines);
          
          Transform dock3pointCANC = built.dock3on3(refCA, refN, refC, modCA, modN, modC);
          newCoords = transformResidue(lines, dock3pointCANC);
      }
      if (bbAtoms.equals("CA-C-N"))
      {
          modCA = getAtomCoords(" CA ", lines);
          modC  = getAtomCoords(" C  ", lines);
          modN  = getAtomCoords(" N  ", lines);
          
          Transform dock3pointCACN = built.dock3on3(refCA, refC, refN, modCA, modC, modN);
          newCoords = transformResidue(lines, dock3pointCACN);
      }
      if (bbAtoms.equals("CA-C-O"))
      {
          modCA = getAtomCoords(" CA ", lines);
          modC  = getAtomCoords(" C  ", lines);
          modO  = getAtomCoords(" O  ", lines);
          
          Transform dock3pointCACO = built.dock3on3(refCA, refC, refO, modC, modCA, modO);
          newCoords = transformResidue(lines, dock3pointCACO);
      }
      if (bbAtoms.equals("CZ-NH1-NH2"))
      {
          modCZ  = getAtomCoords(" CZ ", lines);
          modNH1 = getAtomCoords(" NH1", lines);
          modNH2 = getAtomCoords(" NH2", lines);
          
          Transform dock3pointCZNH1NH2 = built.dock3on3(refCZ, refNH1, refNH2, modCZ, modNH1, modNH2);
          newCoords = transformResidue(lines, dock3pointCZNH1NH2);
      }
      
      return newCoords;
  }

  private Triple getAtomCoords(String atomName, ArrayList<String> lines)
  {
      // Format of each entry in lines: 16pk;A;ARG;22;;N;2.121;26.334;12.636;N
      
      boolean atomFound = false;
      
      
      System.out.println("lines.size() = "+lines.size());
      
      
      for (String line : lines)
      {
          if (!atomFound)
          {
              Scanner s = new Scanner(line).useDelimiter(";");
              for (int i = 1; i <= 5; i ++)
                  s.next();
              String thisAtomsName = s.next();
              double thisX = s.nextDouble();
              double thisY = s.nextDouble();
              double thisZ = s.nextDouble();
              
              if (thisAtomsName.length() == 1)   thisAtomsName = " "+thisAtomsName+"  ";
              if (thisAtomsName.length() == 2)   thisAtomsName = " "+thisAtomsName+" ";
              if (thisAtomsName.length() == 3)   thisAtomsName = " "+thisAtomsName;
              
              
              System.out.println("thisAtomsName: &"+thisAtomsName+"&,\tatomName: &"+atomName+"&");
              
              
              if (thisAtomsName.equals(atomName))
                  return new Triple(thisX, thisY, thisZ);
          }
      }
      
      return null;
  }

  private ArrayList<Triple> transformResidue(ArrayList<String> lines, Transform trans) 
  {
	  ArrayList<Triple> coords = new ArrayList<Triple>();
      for (String line : lines)
      {
          Scanner s = new Scanner(line).useDelimiter(";");
          for (int i = 1; i <= 5; i ++)
              s.next();
          String thisAtomsName = s.next();
          double x = s.nextDouble();
          double y = s.nextDouble();
          double z = s.nextDouble();
          coords.add(new Triple(x, y, z));
      }
      
      ArrayList<Triple> newCoords = new ArrayList<Triple>();
      
      for (Triple atomCoords : coords) 
      {
          Triple newAtomCoords = (Triple) trans.transform(atomCoords);
          newCoords.add(newAtomCoords);
      }
      
      return newCoords;
  }
//}}}

//{{{ writeToKin
//##################################################################################################
  public void writeToKin(ArrayList<String> lines, ArrayList<Triple> newCoords, Model mod)
  {
	  // Format of each entry in lines: 16pk;A;ARG;22;;N;2.121;26.334;12.636;N
      
      try
	  {
          System.out.println("*** Writing to kin ***");
          
          Writer w = new FileWriter(kinOut, true);   // 'true' b/c appending to file
          PrintWriter out = new PrintWriter(new BufferedWriter(w));
          
          ModelState ms = new ModelState();
          ms.createForModel(mod);
          Residue res = null;
          boolean madeResidue = false;
          for (int i = 0; i < lines.size(); i ++)
          {
              // Assume i in lines corresponds to i in newCoords
              String line = lines.get(i);
              Scanner lineScanner = new Scanner(line).useDelimiter(";");
              pdbid    = lineScanner.next();
              chain    = lineScanner.next();
              restype  = lineScanner.next();
              resno    = Integer.parseInt(lineScanner.next());
              altconf  = lineScanner.next();
              atomname = lineScanner.next();
              element  = lineScanner.next();
              lineScanner.close();
              
              Triple atomCoords = newCoords.get(i);
              double x = atomCoords.getX();
              double y = atomCoords.getY();
              double z = atomCoords.getZ();
              
              if (atomname.length() == 1)     atomname = " "+atomname+"  ";
              if (atomname.length() == 2)     atomname = " "+atomname+" ";
              if (atomname.length() == 3)     atomname = " "+atomname;
              
              if (!madeResidue)
              {
                  madeResidue = true;
                  res = new Residue(chain, "", ""+resno, " ", restype);
              }
              
              Atom a = new Atom(atomname, element, false);
              AtomState as = new AtomState(a, ""+i);
              as.setXYZ(x, y, z);
              try { 
                  ms.add(as); }
              catch (AtomException ae) { 
                  System.out.println("Trouble adding AtomState to ModelState"); }
              try {
                  res.add(a); 
                  // TESTING
                  System.out.println("   *Just added Atom "+a.toString()+" to Residue "+res.toString());
                  System.out.println("   *Now, Residue.getAtom("+atomname+") = "+res.getAtom(atomname));
                  System.out.println("   *But, Residue.getAtom( CZ ) = "+res.getAtom(" CZ "));
              }
              catch (AtomException ae) { 
                  System.out.println("Trouble adding Atom to Residue"); }
              
              
              // TESTING
              System.out.println("   Atom: \t"+a.toString());
              System.out.println("   AtomState: \t"+as.toString());
              System.out.println("   ModelState.hasState(a)?: "+ms.hasState(a));
          }
          
          try {
              mod.add(res); 
          
              // TESTING
              System.out.println("Residue: \t"+res.toString());
              System.out.println("Model.contains(res)?: "+mod.contains(res));
          
          }
          catch (ResidueException re) { 
          System.out.println("Trouble adding Residue to Model"); }
          
          
          // TESTING
          Iterator iter = (new UberSet(mod.getResidues())).iterator();
          while (iter.hasNext())
          {
              Residue r2 = ( (Residue) iter.next() );
              System.out.println("(new UberSet(mod.getResidues())).iterator().next() = "+
                  r2.toString());
              Iterator iter2 = r2.getAtoms().iterator();
              while (iter2.hasNext())
              {
                  Atom a2 = (Atom) iter2.next();
                  System.out.println("      Atom      iterator.next(): \t"+a2.toString());
                  try{
                      AtomState as2 = ms.get(a2);
                      System.out.println("      AtomState iterator.next(): \t"+as2.toString());
                  } catch (AtomException ae) { System.out.println("oops..."); }
              }
          }
          
          
          String groupname = pdbid+" "+chain+" "+restype+" "+resno;
            // format: "1amu A ARG 211"
          out.println("@group {"+groupname+"} animate on dominant master= {all residues}");
          
          BallAndStickLogic bsl = new BallAndStickLogic();
          bsl.doProtein = true;
          bsl.doBackbone = true;
          bsl.doSidechains = true;
          bsl.doHydrogens = true;
          bsl.colorBy = BallAndStickLogic.COLOR_BY_MC_SC;
	      bsl.printKinemage(out, mod, new UberSet(mod.getResidues()), "bluetint");
          
          
          out.println("Making sure out is working...");
          
          
          out.flush();
          w.close();
	   }
	   catch (IOException ex) 
	   {
           System.err.println("An error occurred while writing the kin." + ex);
	   }
  }
//}}}

} // end class
