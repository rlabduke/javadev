// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;
import driftwood.moldb2.*;
import driftwood.r3.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//}}}
/**
* <code>DisulfideParameterer</code> reads a PDB and outputs geometrical stats 
* about disulfides, including phis, psis, chis, and VBC's loop parameters.
* 
* Initial disulfide assignments are made outside this class.  For example, 
* PdbReader can get them from SSBOND records and store them in CoordinateFile 
* in a Disulfides object.
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Wed Sep 2 2009
*/
public class DisulfideParameterer //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean  verbose = false;
    File     file;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DisulfideParameterer()
    {
        super();
    }
//}}}

//{{{ analyzeDisulfide
//##############################################################################
    /** Finds the specified disulfide in the specified model and prints some 
    * geometrical stats about it. */
    public void analyzeDisulfide(Disulfide disulfide, CoordinateFile structure, Model model)
    {
        // Find residues on ends
        Residue initRes = null; // not necessarily in
        Residue endRes  = null; //   sequence order
        for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
        {
            Residue res = (Residue) rItr.next();
            {
                if(res.getChain().equals(disulfide.getInitChainId())
                && res.getSequenceInteger() == disulfide.getInitSeqNum()
                && res.getInsertionCode().equals(disulfide.getInitICode()))
                { initRes = res; }
                else if(res.getChain().equals(disulfide.getEndChainId())
                && res.getSequenceInteger() == disulfide.getEndSeqNum()
                && res.getInsertionCode().equals(disulfide.getEndICode()))
                { endRes = res; }
            }
        }
        if(initRes == null)
        {
            System.err.println("*** Couldn't find res1 CNIT '"+disulfide.getInitChainId()
                +disulfide.getInitSeqNum()+disulfide.getInitICode()+"CYS'!");
            //System.out.print("__?__,"+endRes);
            //for(int i = 0; i < 17; i++) System.out.print(",__?__");
            //System.out.println();
            return;
        }
        if(endRes == null)
        {
            System.err.println("*** Couldn't find res2 CNIT '"+disulfide.getEndChainId()
                +disulfide.getEndSeqNum()+disulfide.getEndICode()+"CYS'!");
            //System.out.print(initRes+",__?__");
            //for(int i = 0; i < 17; i++) System.out.print(",__?__");
            //System.out.println();
            return;
        }
        if(verbose) System.err.println("Found "+initRes+" :: "+endRes);
        
        // Output parameters
        double[] angles = calcDisulfAngles(model, initRes, endRes);
        double[] params = calcDisulfParams(model, initRes, endRes);
        int seqDif = calcSeqDif(model, initRes, endRes);
        DecimalFormat df = new DecimalFormat("0.000");
        
        System.out.print(initRes+","+endRes+
            ","+(seqDif == Integer.MAX_VALUE ? "__?__" : seqDif));
        for(int i = 0; i < angles.length; i++) System.out.print(
            ","+(Double.isNaN(angles[i]) ? "__?__" : df.format(angles[i])));
        for(int i = 0; i < params.length; i++) System.out.print(
            ","+(Double.isNaN(params[i]) ? "__?__" : df.format(params[i])));
        System.out.println();
    }
//}}}

//{{{ calcDisulfAngles
//##############################################################################
    /** Calculates phi, psi, chi1, chi2, chi3, chi2', chi1', phi', and psi' for
    * the given pair of residues comprising a disulfide. 
    * Ideally I would like to have put this functionality in Dangle, but the 
    * problem is that Dangle deals with fixed i to i+n measurements the same way 
    * across the structure, whereas for disulfides long-range i to i+n measurements 
    * must be made individually or "ad hoc," so this probably works better. */
    public double[] calcDisulfAngles(Model m, Residue r1, Residue r2)
    {
        Residue res0  = r1.getPrev(m);
        Residue res1  = r1;
        Residue res2  = r1.getNext(m);
        
        Residue resN0 = r2.getPrev(m);
        Residue resN  = r2;
        Residue resN1 = r2.getNext(m);
        
        ModelState ms = m.getState();
        try
        {
            Triple c0   = new Triple(ms.get(res0.getAtom(" C  ")));
            Triple n1   = new Triple(ms.get(res1.getAtom(" N  ")));
            Triple ca1  = new Triple(ms.get(res1.getAtom(" CA ")));
            Triple c1   = new Triple(ms.get(res1.getAtom(" C  ")));
            Triple n2   = new Triple(ms.get(res2.getAtom(" N  ")));
            
            Triple cb1  = new Triple(ms.get(res1.getAtom(" CB ")));
            Triple sg1  = new Triple(ms.get(res1.getAtom(" SG ")));
            Triple sgN  = new Triple(ms.get(resN.getAtom(" SG ")));
            Triple cbN  = new Triple(ms.get(resN.getAtom(" CB ")));
            
            Triple cN0  = new Triple(ms.get(resN0.getAtom(" C  ")));
            Triple nN   = new Triple(ms.get(resN.getAtom(" N  ")));
            Triple caN  = new Triple(ms.get(resN.getAtom(" CA ")));
            Triple cN   = new Triple(ms.get(resN.getAtom(" C  ")));
            Triple nN1  = new Triple(ms.get(resN1.getAtom(" N  ")));
            
            double[] angles = new double[9];
            for(int i = 0; i < angles.length; i++) angles[i] = Double.NaN;
            angles[0] = Triple.dihedral(c0, n1, ca1, c1);    // phi
            angles[1] = Triple.dihedral(n1, ca1, c1, n2);    // psi
            angles[2] = Triple.dihedral(n1, ca1, cb1, sg1);    // chi1
            angles[3] = Triple.dihedral(ca1, cb1, sg1, sgN);   // chi2
            angles[4] = Triple.dihedral(cb1, sg1, sgN, cbN);   // chi3
            angles[5] = Triple.dihedral(sg1, sgN, cbN, caN);   // chi2'
            angles[6] = Triple.dihedral(sgN, cbN, caN, nN);    // chi1'
            angles[7] = Triple.dihedral(cN0, nN, caN, cN);   // phi'
            angles[8] = Triple.dihedral(nN, caN, cN, nN1);   // psi'
            return angles;
        }
        catch(AtomException ex)
        { System.err.println("*** Error calculating 7 parameters for "+r1+" :: "+r2+"!"); }
        return null;
    }
//}}}

//{{{ calcDisulfParams
//##############################################################################
    /** Calculates seven loop/fragment parameters that relate the two residues
    * flanking a disulfide in 3D space.  Re-implements some jiffiloop code. */
    public double[] calcDisulfParams(Model m, Residue r1, Residue r2)
    {
        // Setup taken from jiffiloop.ProteinGap.  Residue first in sequence 
        // is analogous to VBC's residues 0 & 1 (before gap in sequence).
        Residue res0  = r1.getPrev(m);
        Residue res1  = r1;
        Residue resN  = r2;
        Residue resN1 = r2.getNext(m);
        
        ModelState ms = m.getState();
        try
        {
            Triple ca0  = new Triple(ms.get(res0.getAtom(" CA ")));
            Triple ca1  = new Triple(ms.get(res1.getAtom(" CA ")));
            Triple caN  = new Triple(ms.get(resN.getAtom(" CA ")));
            Triple caN1 = new Triple(ms.get(resN1.getAtom(" CA ")));
            Triple co0  = new Triple(ms.get(res0.getAtom(" O  ")));
            Triple coN  = new Triple(ms.get(resN.getAtom(" O  ")));
            Triple cb1  = new Triple(ms.get(res1.getAtom(" CB ")));
            Triple cbN  = new Triple(ms.get(resN.getAtom(" CB ")));
            
            // Parameter calculation taken from jiffiloop.Framer.
            double[] params = new double[7];
            for(int i = 0; i < params.length; i++) params[i] = Double.NaN;
            params[0] = cb1.distance(cbN); // only one VBC doesn't use
            params[1] = ca1.distance(caN);
            params[2] = Triple.angle(ca0, ca1, caN);
            params[3] = Triple.angle(ca1, caN, caN1);
            params[4] = Triple.dihedral(co0, ca0, ca1, caN);
            params[5] = Triple.dihedral(ca0, ca1, caN, caN1);
            params[6] = Triple.dihedral(ca1, caN, caN1, coN);
            return params;
        }
        catch(AtomException ex)
        { System.err.println("*** Error calculating 7 parameters for "+r1+" :: "+r2+"!"); }
        return null;
    }
//}}}

//{{{ calcSeqDif
//##############################################################################
    /** Calculates signed sequence separation of two residues. 
    * Takes insertion codes and chain IDs into account by using Model 
    * connectivity instead of simple difference in integer residue numbers. */
    public int calcSeqDif(Model m, Residue r1, Residue r2)
    {
        // Residue.getNext(Model) takes chain IDs into account anyway, but 
        // why waste time iterating for inter-chain SSs if we don't need to?
        if(!r1.getChain().equals(r2.getChain())) return Integer.MAX_VALUE;
        
        // return r2.getSequenceInteger()-r1.getSequenceInteger();
        // This ^ would be the easy way, but...
        
        // Try C-ward
        Residue r = r1;
        int seqDif = 0;
        while((r = r.getNext(m)) != null)
        {
            seqDif++;
            if(r.getCNIT().equals(r2.getCNIT())) return seqDif;
        }
        // Try N-ward
        r = r1;
        seqDif = 0;
        while((r = r.getPrev(m)) != null)
        {
            seqDif--;
            if(r.getCNIT().equals(r2.getCNIT())) return seqDif;
        }
        // Something's wrong...
        return Integer.MAX_VALUE;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        PdbReader reader = new PdbReader();
        CoordinateFile structure = reader.read(file);
        
        System.out.println("res1,res2,seqdif,"
            +"phi,psi,chi1,chi2,chi3,chi2p,chi1p,phip,psip,"
            +"cb1--cbN,ca1--caN,ca0-ca1-caN,ca1-caN-caN1,"
            +"co0-ca0-ca1-caN,ca0-ca1-caN-caN1,ca1-caN-caN1-coN");
        
        Disulfides disulfides = structure.getDisulfides();
        for(Iterator dItr = disulfides.getDisulfides().iterator(); dItr.hasNext(); )
        {
            Disulfide disulfide = (Disulfide) dItr.next();
            for(Iterator mItr = structure.getModels().iterator(); mItr.hasNext(); )
            {
                Model model = (Model) mItr.next();
                analyzeDisulfide(disulfide, structure, model);
            }
        }
    }

    public static void main(String[] args)
    {
        DisulfideParameterer mainprog = new DisulfideParameterer();
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
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println("*** Error in execution: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("DisulfideParameterer.help");
            if(is == null)
            {
                System.err.println("\n*** Usage: java DisulfideParameterer in.pdb ***\n");
            }
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.DisulfideParameterer");
        System.err.println("Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.");
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
        if(file == null) file = new File(arg);
        else System.err.println("*** Too many files provided! ***");
    }
    
    void interpretFlag(String flag, String param)
    {
        try
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
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Non-number argument to "+flag+": '"+param+"'"); }
    }
//}}}
}//class

