// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.Strings;
import chiropraxis.rotarama.Ramachandran;
//}}}
/**
* <code>BackrubPioneer</code> is the backrub companion to <code>ShearPioneer</code>.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Wed Dec. 15, 2010
*/
public class BackrubPioneer extends ShearPioneer
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BackrubPioneer()
    {
        super();
        
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
    }
//}}}

//{{{ findResidue 
//##############################################################################
    /**
    * Finds the user's requested residue (based on residue number)
    * and does some simple checks before proceeding.
    */
    Residue findResidue(Model model)
    {
        Residue res = null;
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            if(r.getSequenceInteger() == resnum) { res = r; break; }
        }
        
        // Need at least 1 preceding residue and 3 subsequent residues
        // to measure phi,psi for all 3 backrub residues
        if(res == null)
        {
            System.err.println("D'oh!  Can't find residue # "+resnum);
            System.exit(0);
        }
        if(res.getPrev(model) == null)
        {
            System.err.println("D'oh!  Need a residue preceding "+res);
            System.exit(0);
        }
        Residue r = res;
        for(int i = 0; i < 3; i++)
        {
            r = r.getNext(model);
            if(r == null)
            {
                System.err.println("D'oh!  Need 4 residues following "+res);
                System.exit(0);
            }
        }
        return res;
    }
//}}}

//{{{ doMoveSeries 
//##############################################################################
    /**
    * Performs a series of backrubs of varying magnitudes, and prints out 
    * some type of kinemage (Rama streaks or local backbone structures).
    */
    void doMoveSeries(Model model, ModelState state, Residue res, String label)
    {
        System.out.println("@group {brb"+resnum+"-"+(resnum+2)+label
            +" ep"+df5.format(epsilon)+"} animate dominant");
        
        Residue res1 = res;
        Residue res2 = res1.getNext(model);
        Residue res3 = res2.getNext(model);
        if(res1 == null || res2 == null || res3 == null)
        {
            System.err.println("Oops, missing residues somewhere in here: "+resnum+"-"+(resnum+2));
            System.exit(0);
        }
        ArrayList<Residue> residues = new ArrayList<Residue>();
        residues.add(res1);
        residues.add(res2);
        residues.add(res3);
        Residue[] resArray = residues.toArray(new Residue[residues.size()]);
        
        ArrayList<BackrubbedRegion> movedRegions = new ArrayList<BackrubbedRegion>();
        for(double theta = -1.0 * maxTheta; theta <= maxTheta; theta += thetaSpacing)
        {
            try
            {
                // Primary backrub
                ModelState newState = CaRotation.makeConformation(residues, state, theta, false);
                
                // Counter-rotations to restore O1 and O2
                double pepRot1 = calcPepRot(state, newState, res1, res2, theta);
                double pepRot2 = calcPepRot(state, newState, res2, res3, theta);
                double[]  thetas     = new double[]  { pepRot1, pepRot2 };
                boolean[] idealizeSC = new boolean[] { false, false, false };
                newState = CaShear.twistPeptides(resArray, newState, thetas, idealizeSC);
                
                // Store data for output
                double[] rots = new double[3];
                rots[0] = theta;
                rots[1] = pepRot1;
                rots[2] = pepRot2;
                double[] phipsi = new double[4];
                phipsi[0] = AminoAcid.getPhi(model, res1, newState);
                phipsi[1] = AminoAcid.getPsi(model, res1, newState);
                phipsi[2] = AminoAcid.getPhi(model, res3, newState);
                phipsi[3] = AminoAcid.getPsi(model, res3, newState);
                boolean badTau = 
                    AminoAcid.getTauDeviation(res1, newState) > maxTauDev ||
                    AminoAcid.getTauDeviation(res2, newState) > maxTauDev ||
                    AminoAcid.getTauDeviation(res3, newState) > maxTauDev;
                boolean ramaOut = 
                    rama.isOutlier(model, res1, state) ||
                    rama.isOutlier(model, res2, state) ||
                    rama.isOutlier(model, res3, state);
                BackrubbedRegion s = new BackrubbedRegion(resArray, newState, rots, phipsi, badTau, ramaOut);
                movedRegions.add(s);
            }
            catch(AtomException ex)
            { System.err.println("Can't perform "+df.format(theta)+" degree backrub!"); }
            catch(ResidueException ex)
            { System.err.println("Can't get phi/psi after "+df.format(theta)+" degree backrub!"); }
        }
        
        // Kinemage
        if(outputMode == STREAKS)
        {
            printStreaks(movedRegions);
        }
        else if(outputMode == STRUCTS)
        {
            printStructs(movedRegions, model);
        }
        else System.err.println("Cannot compute!  Bad output mode: "+outputMode);
    }
//}}}

//{{{ printStreaks
//##############################################################################
    /** 
    * Prints two streaks of phi/psi points, one for each end of a backrub series, 
    * in kinemage format.
    */
    void printStreaks(ArrayList movedRegions)
    {
        System.out.println("@balllist {backrub i} radius= 0.3 master= {backrub i}");
        for(int i = 0; i < movedRegions.size(); i++)
        {
            BackrubbedRegion s = (BackrubbedRegion) movedRegions.get(i);
            System.out.println("{i "
                +s.res1.getName().toLowerCase().trim()+s.res1.getSequenceInteger()
                +" "+df.format(s.theta)+","+df.format(s.pepRot1)+","+df.format(s.pepRot2)
                +" ep="+df2.format(epsilon)+" ("+df.format(s.phi1)+","+df.format(s.psi1)+")}"
                +(s.badTau || s.ramaOut ? BAD_COLOR : GOOD_COLOR)+" "
                +df4.format(s.phi1)+" "+df4.format(s.psi1)); // actual coordinates
        }
        
        System.out.println("@balllist {backrub i+2} radius= 0.3 master= {backrub i+2}");
        for(int i = 0; i < movedRegions.size(); i++)
        {
            BackrubbedRegion s = (BackrubbedRegion) movedRegions.get(i);
            System.out.println("{i+3 "
                +s.res3.getName().toLowerCase().trim()+s.res3.getSequenceInteger()
                +" "+df.format(s.theta)+","+df.format(s.pepRot1)+","+df.format(s.pepRot2)
                +" ep="+df2.format(epsilon)+" ("+df.format(s.phi3)+","+df.format(s.psi3)+")}"
                +(s.badTau || s.ramaOut ? BAD_COLOR : GOOD_COLOR)+" "
                +df4.format(s.phi3)+" "+df4.format(s.psi3)); // actual coordinates
        }
    }
//}}}

//{{{ printStructs
//##############################################################################
    /** 
    * Prints a series of local backbone structures resulting from backrubs of varying
    * magnitudes in kinemage format.
    */
    void printStructs(ArrayList movedRegions, Model model)
    {
        for(int i = 0; i < movedRegions.size(); i++)
        {
            BackrubbedRegion s = (BackrubbedRegion) movedRegions.get(i);
            
            if(pdbOut)
            {
                PdbWriter writer = new PdbWriter(System.out);
                System.out.println("MODEL       "+((i+1) < 10 ? " " : "")+(i+1));
                writer.writeResidues(model.getResidues(), s.state);
            }
            else
            {
                Atom ca1 = s.res1.getAtom(" CA ");
                Atom c1  = s.res1.getAtom(" C  ");
                Atom o1  = s.res1.getAtom(" O  ");
                Atom n2  = s.res2.getAtom(" N  ");
                Atom ca2 = s.res2.getAtom(" CA ");
                Atom c2  = s.res2.getAtom(" C  ");
                Atom o2  = s.res2.getAtom(" O  ");
                Atom n3  = s.res3.getAtom(" N  ");
                Atom ca3 = s.res3.getAtom(" CA ");
                try
                {
                    AtomState ca1s = s.state.get(ca1);
                    AtomState c1s  = s.state.get(c1);
                    AtomState o1s  = s.state.get(o1);
                    AtomState n2s  = s.state.get(n2);
                    AtomState ca2s = s.state.get(ca2);
                    AtomState c2s  = s.state.get(c2);
                    AtomState o2s  = s.state.get(o2);
                    AtomState n3s  = s.state.get(n3);
                    AtomState ca3s = s.state.get(ca3);
                    
                    System.out.println("@vectorlist {"+s.toString()
                        +" "+df.format(s.theta)+","+df.format(s.pepRot1)+","+df.format(s.pepRot2)
                        +"} width= 2 color= "+(s.badTau || s.ramaOut ? BAD_COLOR : GOOD_COLOR));
                    printAtomCoords(ca1s, s.res1+" 'CA'", true);
                    printAtomCoords(c1s,  s.res1+" 'C'" , false);
                    printAtomCoords(o1s,  s.res1+" 'O'" , false);
                    printAtomCoords(c1s,  s.res1+" 'C'" , true);
                    printAtomCoords(n2s,  s.res2+" 'N'" , false);
                    printAtomCoords(ca2s, s.res2+" 'CA'", false);
                    printAtomCoords(c2s,  s.res2+" 'C'" , false);
                    printAtomCoords(o2s,  s.res2+" 'O'" , false);
                    printAtomCoords(c2s,  s.res2+" 'C'" , true);
                    printAtomCoords(n3s,  s.res3+" 'N'" , false);
                    printAtomCoords(ca3s, s.res3+" 'CA'", false);
                }
                catch(AtomException ex)
                {
                    System.err.println("Error printing structures for "+s);
                }
            }
        }
    }
//}}}

//{{{ CLASS: BackrubbedRegion
//##############################################################################
    /**
    * Embodies a local backrubbed structure and several useful aspects of its geometry.
    */
    public class BackrubbedRegion //extends ... implements ...
    {
        protected Residue res1, res2, res3;
        protected ModelState state;
        protected double theta, pepRot1, pepRot2;
        protected double phi1, psi1, phi3, psi3;
        protected boolean badTau;
        protected boolean ramaOut;
        
        public BackrubbedRegion(Residue[] r, ModelState s, double[] rots, double[] pp, boolean bt, boolean ro)
        {
            super();
            
            res1 = r[0];
            res2 = r[1];
            res3 = r[2];
            state = s;
            
            theta   = rots[0];
            pepRot1 = rots[1];
            pepRot2 = rots[2];
            
            phi1 = pp[0];
            psi1 = pp[1];
            phi3 = pp[2];
            psi3 = pp[3];
            
            badTau = bt;
            ramaOut = ro;
        }
        
        public String toString()
        {
            return res1.getName().toLowerCase().trim()+res1.getSequenceInteger()+"-"+
                   res3.getName().toLowerCase().trim()+res3.getSequenceInteger();
        }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if(filename == null)
        {
            if(useIdealHelix)
            {
                if(verbose) System.err.println("Using ideal helix");
            }
            else
            {
                System.err.println("Using ideal helix (no input file provided)");
                useIdealHelix = true;
            }
        }
        /*if(filename != null && !Double.isNaN(phipsiRange))
        {
            System.err.println("Grid of initial phi,psi requires using ideal helix (-alpha)!");
            System.exit(0);
        }*/
        if(!Double.isNaN(minEpsilon) && !Double.isNaN(maxEpsilon) && !Double.isNaN(phipsiRange))
        {
            System.err.println("Can't use -epsilon=#,# AND -phipsirange=#, silly goose!");
            System.exit(0);
        }
        if(resnum == Integer.MAX_VALUE)
        {
            System.err.println("Need a residue number!  Use -res=i (for i to i+2 backrub)");
            System.exit(0);
        }
        if(phipsiRange > 15)
        {
            System.err.println("Init phi,psi range of "+df3.format(phipsiRange)+" too big!  Using 15");
            phipsiRange = 15;
        }
        if(phipsiRange > 0 && outputMode == STRUCTS && !pdbOut)
        {
            System.err.println("Warning: Exact kin coords w/ altered init phi/psi are not"
                +" meaningful w.r.t. original coords!");
        }
        
        try
        {
            PdbReader reader = new PdbReader();
            CoordinateFile cf = null;
            if(filename != null)
                cf = reader.read(new File(filename));
            else
                cf = reader.read(this.getClass().getResourceAsStream("idealpolyala12-alpha.pdb"));
            
            Model m = cf.getFirstModel();
            processModel(m);
        }
        catch(IOException ioe) { System.err.println("Trouble parsing file!"); }
    }

    public static void main(String[] args)
    {
        BackrubPioneer mainprog = new BackrubPioneer();
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
            InputStream is = getClass().getResourceAsStream("BackrubPioneer.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'BackrubPioneer.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.BackrubPioneer");
        System.err.println("Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.");
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

