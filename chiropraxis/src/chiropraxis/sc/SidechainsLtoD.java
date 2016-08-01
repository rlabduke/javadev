// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.sc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>SidechainsLtoD</code> is a class for converting a PDB file of all 
* "normal" L-amino acids into D-amino acids.
* It's structured after chiropraxis.sc.SidechainIdealizer.
*
* <p>Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Dec 15 2008
*/
public class SidechainsLtoD //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    boolean              verbose;
    String               filename;  // of PDB we want to change from L to D
    Model                model;
    ArrayList<Residue>   residues;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public SidechainsLtoD()
    {
        // nothing to see here...
    }
//}}}

//{{{ loadPdb
//##################################################################################################
    ModelState loadPdb() throws IOException
    {
        File             file  = new File(filename);
        FileInputStream  fis   = new FileInputStream(file);
        PdbReader        pdbr  = new PdbReader();
        CoordinateFile   cf    = pdbr.read(fis);
                        
                   model = cf.getFirstModel();
        ModelState state = model.getState();
        
        residues = new ArrayList<Residue>();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            residues.add(r);
        }
        
        return state;        
    }
//}}}

//{{{ changeSidechainLtoD
//##################################################################################################
    /**
    * For given residue, changes sidechain from L -> D and returns a new 
    * ModelState reflecting this change.
    *
    * You can think of this procedure as simply pointing CA-CB in the direction 
    * of CA-HA and vice versa.  However, what we're really doing is modeled
    * after Ian's chiropraxis.sc.SidechainIdealizer.idealizeCD(), which uses 
    * N, CA, C to construct an ideal CB.
    * 
    * Then we'll transform the rest of the sidechain accordingly, maintaining 
    * original bond lengths and angles (except for N-CA-CB and C-CA-CB).
    */
    public ModelState changeSidechainLtoD(Residue res, ModelState orig) throws AtomException
    {
        if (res == null)
        {
            System.err.println("Cannot recognize residue "+res+"!");
            return orig;
        }
        
        Triple t1, t2, ideal = new Triple();
        Builder build = new Builder();
        ModelState modState = new ModelState(orig);
        
        // These will trigger AtomExceptions if res is missing an Atom
        // because it will try to retrieve the state of null.
        AtomState aaN   = orig.get( res.getAtom(" N  ") );
        AtomState aaCA  = orig.get( res.getAtom(" CA ") );
        AtomState aaC   = orig.get( res.getAtom(" C  ") );
        
        // Build an ideal D-amino acid C-beta and swing the side chain into place
        Atom cBeta = res.getAtom(" CB ");
        if(cBeta != null)
        {
            // (1) Construct ideal C-beta for D-amino acid
            // Changed dihedrals to match where HA is instead of CB.
            // Used Ian's HA values from idealizeCB(); also confirmed 
            // by visual inspection in KiNG
            t1 = build.construct4(aaN, aaC, aaCA, 1.536, 110.4, -118.3); // 123.1);
            t2 = build.construct4(aaC, aaN, aaCA, 1.536, 110.6,  118.2); //-123.0);
            ideal.likeMidpoint(t1, t2);
            
            // Construct rotation to align actual and ideal
            AtomState aaCB = orig.get(cBeta);
            double theta = Triple.angle(ideal, aaCA, aaCB);
            //SoftLog.err.println("Angle of correction: "+theta);
            t1.likeNormal(ideal, aaCA, aaCB).add(aaCA);
            Transform xform = new Transform().likeRotation(aaCA, t1, theta);
            
            // Apply the transformation
            for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
            {
                Atom        atom    = (Atom)iter.next();
                String      name    = atom.getName();
                
                // Transform everything that's not mainchain
                if( !( name.equals(" N  ") || name.equals(" H  ")
                    || name.equals(" CA ") || name.equals(" HA ")
                    || name.equals("1HA ") || name.equals("2HA ")
                    || name.equals(" HA2") || name.equals(" HA3")
                    || name.equals(" C  ") || name.equals(" O  ")) )
                {
                    // Clone the original state, move it, and insert it into our model
                    AtomState   s1      = orig.get(atom);
                    AtomState   s2      = (AtomState)s1.clone();
                    xform.transform(s1, s2);
                    modState.add(s2);
                }//if atom is not mainchain
            }//for each atom in the residue
            
            // (2) Construct second rotation around chi1 to address fact that 
            // new D-amino acid sidechain now has eclipsed chi1
            // This involves symmetrizing chi1 (i.e. x1 for L -> -x1 for D), 
            // while the rest of the sidechain dihedrals are left untouched.
            String r = res.getName();      Atom xGamma = res.getAtom(" CG ");
            if      (r == "GLY" || r == "ALA")  xGamma = null;
            else if (r == "ILE" || r == "VAL")  xGamma = res.getAtom(" CG1");
            else if (r == "SER")                xGamma = res.getAtom(" OG ");
            else if (r == "THR")                xGamma = res.getAtom(" OG1");
            else if (r == "CYS")                xGamma = res.getAtom(" SG ");
            if (xGamma != null) // only makes sense for residue types w/ defined chi1
            {
                AtomState  aaXG    = orig.get(xGamma);
                AtomState newXG    = modState.get(xGamma);
                double origChi1 = Triple.dihedral(aaN, aaCA, aaCB,  aaXG );
                double currChi1 = Triple.dihedral(aaN, aaCA, ideal, newXG);
                double goalChi1 = -1.0 * origChi1;
                Transform xform2 = new Transform().likeRotation(aaCA, ideal, goalChi1-currChi1);
                
                // Apply the transformation
                for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
                {
                    Atom        atom    = (Atom)iter.next();
                    String      name    = atom.getName();
                    
                    // Transform everything that's not mainchain or CB
                    if( !( name.equals(" N  ") || name.equals(" H  ")
                        || name.equals(" CA ") || name.equals(" HA ")
                        || name.equals("1HA ") || name.equals("2HA ")
                        || name.equals(" HA2") || name.equals(" HA3")
                        || name.equals(" C  ") || name.equals(" O  ")
                        || name.equals(" CB ")                      ) )
                    {
                        // Clone the original state, move it, and insert it into our model
                        // Use add+overwrite method b/c we don't wanna leave old, eclipsed 
                        // sidechain atoms in place in addition to new, non-eclipsed ones
                        AtomState   s3      = modState.get(atom);
                        AtomState   s4      = (AtomState)s3.clone();
                        xform2.transform(s3, s4);
                        modState.addOverwrite(s4); // note overwrite
                    }//if atom is not mainchain
                }//for each atom in the residue
            }//if chi1 defined
        }//rebuilt C-beta + sidechain
        
        // Reconstruct alpha hydrogens
        // These are easier -- just compute the position and make it so!
        Atom hAlpha = res.getAtom(" HA ");
        if(hAlpha != null)
        {
            AtomState s1 = orig.get(hAlpha);
            AtomState s2 = (AtomState)s1.clone();
            // Changed dihedrals to match where CB is instead of HA.
            // Used Ian's CB values from idealizeCB()
            t1 = build.construct4(aaN, aaC, aaCA, 1.100, 107.9,  123.1);  //-118.3);
            t2 = build.construct4(aaC, aaN, aaCA, 1.100, 108.1, -123.0);  // 118.2);
            s2.likeMidpoint(t1, t2).sub(aaCA).unit().mult(1.100).add(aaCA);
            modState.add(s2);
        }
        
        // Now for glycine, and then we're done
        // I (keedy) reversed naming convention of two HA's for L -> D change.
        // (Who knows if that's "proper", but hey, it made sense to me...)
        hAlpha = res.getAtom("1HA ");
        if (hAlpha == null) res.getAtom(" HA2");
        if(hAlpha != null)
        {
            AtomState s1 = orig.get(hAlpha);
            AtomState s2 = (AtomState)s1.clone();
            t1 = build.construct4(aaN, aaC, aaCA, 1.100, 109.3,  121.6);  //-121.6);
            t2 = build.construct4(aaC, aaN, aaCA, 1.100, 109.3, -121.6);  // 121.6);
            s2.likeMidpoint(t1, t2).sub(aaCA).unit().mult(1.100).add(aaCA);
            modState.add(s2);
        }
        hAlpha = res.getAtom("2HA ");
        if (hAlpha == null) res.getAtom(" HA3");
        if(hAlpha != null)
        {
            AtomState s1 = orig.get(hAlpha);
            AtomState s2 = (AtomState)s1.clone();
            t1 = build.construct4(aaN, aaC, aaCA, 1.100, 109.3, -121.6);  // 121.6);
            t2 = build.construct4(aaC, aaN, aaCA, 1.100, 109.3,  121.6);  //-121.6);
            s2.likeMidpoint(t1, t2).sub(aaCA).unit().mult(1.100).add(aaCA);
            modState.add(s2);
        }
        
        return modState;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if (verbose) System.err.println("Starting Main method...");
        
        try
        {
            ModelState state = loadPdb(); // could throw exception
            
            // Change L -> D for each sidechain, continuously updating 'state'
            for (int i = 0; i < residues.size(); i++)
            {
                Residue res = (Residue) residues.get(i);
                try
                { state = changeSidechainLtoD(res, state); } // could throw exception
                catch (AtomException ae)
                { System.err.println("Atom exception for residue "+res); }
            }
            
            // Output new PDB
            PdbWriter pdbWriter = new PdbWriter(System.out);
            pdbWriter.writeResidues(residues, state);
            pdbWriter.close();
        }
        catch (IOException ioe)
        {
            System.err.println("I/O exception in reading PDB file "+filename+" ... exiting!");
            System.exit(0);
        }
    }

    public static void main(String[] args)
    {
        SidechainsLtoD mainprog = new SidechainsLtoD();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}

//{{{ parseArguments, showHelp
//##############################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        String  arg, flag, param;
        boolean interpFlags = true;
        
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") || !interpFlags || arg.equals("-"))
            {
                // This is probably a filename or something
                interpretArg(arg);
            }
            else if(arg.equals("--"))
            {
                // Stop treating things as flags once we find --
                interpFlags = false;
            }
            else
            {
                // This is a flag. It may have a param after the = sign
                int eq = arg.indexOf('=');
                if(eq != -1)
                {
                    flag    = arg.substring(0, eq);
                    param   = arg.substring(eq+1);
                }
                else
                {
                    flag    = arg;
                    param   = null;
                }
                
                try { interpretFlag(flag, param); }
                catch(NullPointerException ex)
                { throw new IllegalArgumentException("'"+arg
                    +"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("SidechainsLtoD.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SidechainsLtoD.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.sc.SidechainsLtoD");
        System.err.println("Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ interpretArg, interpretFlag
//##############################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
        if (filename == null) filename = arg;
        else System.out.println("Didn't need "+arg+"; already have file "+filename);
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

