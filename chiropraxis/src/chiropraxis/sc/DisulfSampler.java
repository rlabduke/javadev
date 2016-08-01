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
import driftwood.util.Strings;
//}}}
/**
* <code>DisulfSampler</code> accepts a list of chi angles and figures
* of merit for a disulfide, and translates them into PDB-format coordinates
* for one amino acid type.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan  6 15:33:28 EST 2004
*/
public class DisulfSampler extends RotamerSampler // implements ...
{
//{{{ Constants
    String      aaType       = "CIS";
    
    /** Higher indices into this string are LESS remote */
    public static final String REMOTENESS = "HZEDGBA ";
//}}}

//{{{ Variable definitions
//##############################################################################
    File        inFile       = null;
    Residue     res1         = null; // our "template" residues
    Residue     res2         = null; // our "template" residues
    int         nAngles      = 5;
    String      chisSupplied = "1,2,3";
    boolean     plotChis     = false;
    String      group        = null;
    String      color        = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DisulfSampler()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ setChi, areParentAndChild
//##############################################################################
    /**
    * Rotates appropriate atoms in the model disulfide coordinates to set the 
    * given dihedral, then returns the resulting ModelState.
    * This method is modeled after SidechainAngles2.setAngle.
    */
    public ModelState setChi(int chi, Residue res1, Residue res2, ModelState state, double endAngle) throws AtomException
    {
        ArrayList allAtoms = new ArrayList<Atom>();
        for (Iterator iter = res1.getAtoms().iterator(); iter.hasNext(); )
            allAtoms.add((Atom)iter.next());
        for (Iterator iter = res2.getAtoms().iterator(); iter.hasNext(); )
            allAtoms.add((Atom)iter.next());
        
        AtomState a1 = null, a2 = null, a3 = null, a4 = null;
        //{{{ ... set coordinates for four atoms comprising dihedral
        if (chi == 1)
        {
            a1 = state.get(res1.getAtom(" N  "));
            a2 = state.get(res1.getAtom(" CA "));
            a3 = state.get(res1.getAtom(" CB "));
            a4 = state.get(res1.getAtom(" SG "));
        }
        if (chi == 2)
        {
            a1 = state.get(res1.getAtom(" CA "));
            a2 = state.get(res1.getAtom(" CB "));
            a3 = state.get(res1.getAtom(" SG "));
            a4 = state.get(res2.getAtom(" SG "));
        }
        if (chi == 3)
        {
            a1 = state.get(res1.getAtom(" CB "));
            a2 = state.get(res1.getAtom(" SG "));
            a3 = state.get(res2.getAtom(" SG "));
            a4 = state.get(res2.getAtom(" CB "));
        }
        if (chi == 4)
        {
            a1 = state.get(res1.getAtom(" SG "));
            a2 = state.get(res2.getAtom(" SG "));
            a3 = state.get(res2.getAtom(" CB "));
            a4 = state.get(res2.getAtom(" CA "));
        }
        if (chi == 5)
        {
            a1 = state.get(res2.getAtom(" SG "));
            a2 = state.get(res2.getAtom(" CB "));
            a3 = state.get(res2.getAtom(" CA "));
            a4 = state.get(res2.getAtom(" N  "));
        }
        //}}}
        
        double startAngle = Triple.dihedral(a1, a2, a3, a4);
        double dTheta = endAngle - startAngle;
        Transform rot = new Transform();
        rot.likeRotation(a2, a3, dTheta);
        
        ModelState ms = new ModelState(state);
        for(Iterator iter = allAtoms.iterator(); iter.hasNext(); )
        {
            Atom atom = (Atom)iter.next();
            a1 = state.get(atom);
            a2 = (AtomState)a1.clone();
            if( areParentAndChild(a3.getAtom(), atom, a3.getAtom().getResidue(), atom.getResidue()) )
            {
                //System.err.println("Rotating \t"+atom+" for chi"+chi);
                //Triple a2old = new Triple(a2);
                rot.transform(a2);
                //System.err.println(atom+" moved "+Triple.distance(a2, a2old)+" for chi"+chi);
                ms.add(a2);
            }
            //else System.err.println("Not rotating \t"+atom+" for chi"+chi);
        }
        
        return ms;
    }
    
    protected boolean areParentAndChild(Atom parent, Atom child, Residue parentRes, Residue childRes)
    {
        // See if putative 'child' atom is further than putative 'parent' atom 
        // along the length of the disulfide residue-wise (defined as 1->2)
        int cn = childRes.getSequenceInteger();
        int pn = parentRes.getSequenceInteger();
        if (cn > pn)   return true;
        
        // If in same residue, see which one is further along based on atom name
        // and which residue it is
        String p = parent.getName();
        String c = child.getName();
        int pi = REMOTENESS.indexOf(p.charAt(2));
        int ci = REMOTENESS.indexOf(c.charAt(2));
        
        return
         ( (pi > ci && (cn == 1 && pn == 1))                      // parent closer AND on 1st res of SS
        || (pi < ci && (cn == 2 && pn == 2))                      // parent farther AND on 2nd res of SS
        || (pi == ci && pn == cn &&                               // OR same res AND child is an H of parent
           (p.charAt(3) == ' ' || p.charAt(3) == c.charAt(3)) && 
            p.charAt(1) != 'H' && c.charAt(1) == 'H') );
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, NumberFormatException
    {
        // Check arguments
        if(inFile == null)
            throw new IllegalArgumentException("Not enough command line arguments");
        //if (nAngles != 3 && nAngles != 4 && nAngles != 5)
        //    System.err.println("-chis=# must be 3, 4, or 5!");
        
        // Obtain template residues
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(this.getClass().getResourceAsStream("disulf.pdb"));
        Model           model       = coordFile.getFirstModel();
        ModelState      modelState  = model.getState();
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue)iter.next();
            if (res1 == null)        res1 = r;
            else if (res2 == null)   res2 = r;
            else System.err.println("Oops, found more than two residues in the reference disulfide coordinates!");
        }
        if(res1 == null || res2 == null)
            throw new IllegalArgumentException("Couldn't find enough residues in the the reference disulfide coordinates...");
        
        // Read data from list file
        LineNumberReader in = new LineNumberReader(new FileReader(inFile));
        ArrayList data = new ArrayList();
        String s;
        int nFields = -1;
        while((s = in.readLine()) != null)
        {
            if(s.startsWith("#")) continue;
            
            String[] parts = Strings.explode(s, ':');
            if(nFields < 0) nFields = parts.length;
            else if(nFields != parts.length) throw new IllegalArgumentException("Data fields are of different lengths");
            
            double[] vals = new double[7];//[nFields];
            for(int i = 0; i < 7; i++)
                vals[i] = Double.NaN;
            if (chisSupplied.equals("1,2,3"))
            {
                for(int i = 0; i < nFields-2; i++)
                    vals[i] = Double.parseDouble(parts[i]);
                if (Double.isNaN(vals[3]))   vals[3] = 290; // placeholder chi2'
                if (Double.isNaN(vals[4]))   vals[4] = 300; // placeholder chi1'
                vals[5] = Double.parseDouble(parts[nFields-2]);
                vals[6] = Double.parseDouble(parts[nFields-1]);
                //System.err.println("main: "+vals[5]+"\t check: "+vals[6]);
            }
            else if (chisSupplied.equals("2,3,2") || chisSupplied.equals("2,3,2p"))
            {
                vals[0] = 300;                              // placeholder chi1
                for(int i = 0; i < nFields-2; i++)
                    vals[i+1] = Double.parseDouble(parts[i]);
                if (Double.isNaN(vals[4]))   vals[4] = 300; // placeholder chi1'
                vals[5] = Double.parseDouble(parts[nFields-2]);
                vals[6] = Double.parseDouble(parts[nFields-1]);
                //System.err.println("main: "+vals[5]+"\t check: "+vals[6]);
            }
            data.add(vals);
        }
        in.close();
        
        // Determine figure of merit
        double maxWeight = 0;
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            double[] vals = (double[])iter.next();
            maxWeight = Math.max(maxWeight, vals[5]);//[nFields-2]);//[nAngles]);
        }
        //System.err.println("maxWeight: "+maxWeight);
        
        // Output
        if (plotChis)
        {
            // Plot chis in kin format instead of creating rotamers PDB
            DecimalFormat df = new DecimalFormat("###.#");
            if (group == null) group = "disulf samp chis";
            if (color == null) color = "yellow";
            if (chisSupplied.equals("1,2,3"))
                System.out.println("@1axischoice 1 2 3");
            if (chisSupplied.equals("2,3,2") || chisSupplied.equals("2,3,2p"))
                System.out.println("@1axischoice 2 3 4");
            System.out.print("@dimensions ");
            for (int i = 1; i <= nAngles; i ++) System.out.print(" {chi"+i+"}");
            System.out.println();
            System.out.print("@dimminmax ");
            for (int i = 1; i <= nAngles; i ++) System.out.print(" 0 360");
            System.out.println();
            System.out.println("@group {"+group+"} dominant");
            System.out.println("@balllist {"+group+"} radius= 4 color= "+color+" dimension= "+nAngles);
            for(Iterator iter = data.iterator(); iter.hasNext(); )
            {
                // Point ID
                double[] vals = (double[])iter.next();
                System.out.print("{");
                for (int i = 0; i < nAngles; i ++)   System.out.print(df.format(vals[i])+", ");
                System.out.print("} ");
                
                // Actual x,y,z coordinates
                for (int i = 0; i < nAngles; i ++)   System.out.print(df.format(vals[i])+" ");
                System.out.println();
            }
        }
        else
        {
            // Create conformers
            PdbWriter pdbWriter = new PdbWriter(System.out);
            pdbWriter.setRenumberAtoms(true);
            int i = 1;
            for(Iterator iter = data.iterator(); iter.hasNext(); i++)
            {
                try
                {
                    // Set dihedrals
                    Residue tempRes1 = new Residue(res1, " ", "", Integer.toString(1), " ", res1.getName());
                    Residue tempRes2 = new Residue(res2, " ", "", Integer.toString(2), " ", res2.getName());
                    ModelState tempState = tempRes1.cloneStates(res1, modelState, new ModelState(modelState));
                    tempState            = tempRes2.cloneStates(res2, modelState, tempState);
                    
                    double[] vals = (double[])iter.next();
                    tempState     = setChi(1, tempRes1, tempRes2, new ModelState(tempState), vals[0]);
                    tempState     = setChi(2, tempRes1, tempRes2, new ModelState(tempState), vals[1]);
                    tempState     = setChi(3, tempRes1, tempRes2, new ModelState(tempState), vals[2]);
                    tempState     = setChi(4, tempRes1, tempRes2, new ModelState(tempState), vals[3]);
                    tempState     = setChi(5, tempRes1, tempRes2, new ModelState(tempState), vals[4]);
                    
                    // NOT WORKING RIGHT!
                    // Set figures of merit ("pseudo-occupancies") for atoms within residues
                    //for(Iterator ai = tempRes1.getAtoms().iterator(); ai.hasNext(); )
                    //{
                    //    Atom a = (Atom)ai.next();
                    //    double occ = 999.0 * vals[nFields-2]/maxWeight;
                    //    if(occ >= 1000.0) throw new Error("Logical error in occupancy weighting scheme");
                    //    tempState.get(a).setOccupancy(occ);
                    //}
                    //for(Iterator ai = tempRes2.getAtoms().iterator(); ai.hasNext(); )
                    //{
                    //    Atom a = (Atom)ai.next();
                    //    double occ = 999.0 * vals[nFields-2]/maxWeight;
                    //    if(occ >= 1000.0) throw new Error("Logical error in occupancy weighting scheme");
                    //    tempState.get(a).setOccupancy(occ);
                    //}
                    
                    Collection tempResidues = new ArrayList<Residue>();
                    tempResidues.add(tempRes1);
                    tempResidues.add(tempRes2);
                    
                    // Write out coordinates
                    if (i > 0 && i < 10)          System.out.println("MODEL        "+i);
                    else if (i >= 10 && i < 100)  System.out.println("MODEL       " +i);
                    else if (i >= 100)            System.out.println("MODEL      "  +i);
                    pdbWriter.writeResidues(tempResidues, tempState);
                    System.out.println("ENDMDL");
                }
                catch(AtomException ex) { ex.printStackTrace(); }
            }
            System.out.flush();
            pdbWriter.close();
        }
    }

    public static void main(String[] args)
    {
        DisulfSampler mainprog = new DisulfSampler();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
        catch(Throwable ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println("*** Error: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("DisulfSampler.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'DisulfSampler.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.sc.DisulfSampler");
        System.err.println("Copyright (C) 2004 by Ian W. Davis. All rights reserved.");
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
        if(inFile == null) inFile = new File(arg);
        else throw new IllegalArgumentException("Too many command line arguments");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-plotchis"))
        {
            plotChis= true;
        }
        else if(flag.equals("-group"))
        {
            group = param;
        }
        else if(flag.equals("-color"))
        {
            color = param;
        }
        else if(flag.equals("-chis"))
        {
            chisSupplied = param;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

