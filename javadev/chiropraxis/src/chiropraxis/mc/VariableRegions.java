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
//}}}
/**
* <code>VariableRegions</code> searches through the alternate conformation loops
* of a crystal structure, outputting the Ca-Ca distance and phi,psi for each 
* residue between the hinge points.
* It can also do the same for two provided PDB files, e.g. from the Donald lab's
* BD backbone-DEE protein design algorithm.
*
* <p>Copyright (C) 2007 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Feb. 11, 2007.
*/
public class VariableRegions //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String        filename1 = null;
    String        filename2 = null;
    boolean       verbose   = false;
    boolean       absVal    = false;
    boolean       allRes    = false;
    PrintStream   out       = System.out;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public VariableRegions()
    {
        super();
    }
//}}}

//{{{ searchModel
//##############################################################################
    void searchModel(String label, Model model)
    {
        if (verbose) System.err.println("Looking for regions that vary in "+filename1);
        DecimalFormat df = new DecimalFormat("#.###");
        ModelState state1 = model.getState("A");
        ModelState state2 = model.getState("B");
        if (state2 != null)
        {
            for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
            {
                Residue res = (Residue) iter.next();
                if (res != null)
                {
                    Residue prev = res.getPrev(model);
                    Residue next = res.getNext(model);
                    try
                    {
                        Atom ca = res.getAtom(" CA ");
                        AtomState ca1 = state1.get(ca);
                        AtomState ca2 = state2.get(ca);
                        double caTravel = Triple.distance(ca1, ca2);
                        double dPhi = Double.NaN, dPsi = Double.NaN;
                        if (prev != null && next != null)
                        {
                            double phi1 = calcPhi(prev, res, state1);
                            double phi2 = calcPhi(prev, res, state2);
                            double psi1 = calcPsi(res, next, state1);
                            double psi2 = calcPsi(res, next, state2);
                            dPhi = (absVal ? Math.abs(phi2-phi1) : phi2-phi1);
                            dPsi = (absVal ? Math.abs(psi2-psi1) : psi2-psi1);
                        }
                        if ( allRes || (!Double.isNaN(dPhi) && !Double.isNaN(dPsi) 
                        && (dPhi != 0 || dPsi != 0 || caTravel != 0)) )
                        {
                            // Either something changed and is therefore worth printing 
                            // or we want to print stats for all residues regardless of 
                            // whether anything changed.
                            out.print(label+":"+model+":"+res.getChain()+":"+
                                res.getName()+":"+res.getSequenceInteger()+":");
                            if (!Double.isNaN(dPhi)) out.print(df.format(dPhi)+":");
                            else                     out.print("__?__:");
                            if (!Double.isNaN(dPsi)) out.print(df.format(dPsi)+":");
                            else                     out.print("__?__:");
                            out.println(df.format(caTravel));
                        }
                    }
                    catch (AtomException ae) { }
                }
            } //for each residue
        }
        else System.err.println("No altB ModelState for Model "+model+"...");
    }
//}}}

//{{{ searchModels
//##############################################################################
    void searchModels(Model model1, Model model2, String label1, String label2)
    {
        if (verbose) System.err.println("Looking for regions that vary between "
            +filename1+" and "+filename2+"...");
        
        DecimalFormat df = new DecimalFormat("#.###");
        ModelState state1 = model1.getState();
        ModelState state2 = model2.getState();
        
        // Align residues by sequence
        // For now we just take all residues as they appear in the file,
        // without regard to chain IDs, etc.
        Alignment align = Alignment.needlemanWunsch(model1.getResidues().toArray(), model2.getResidues().toArray(), new SimpleResAligner());
        if (verbose)
        {
            System.err.println("Residue alignments:");
            for (int i = 0; i < align.a.length; i++)
                System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            System.err.println();
        }
        
        for(int i = 0, len = align.a.length; i < len; i++)
        {
            if (align.a[i] == null || align.b[i] == null) continue;
            Residue res1 = (Residue) align.a[i];
            Residue res2 = (Residue) align.b[i];
            if (!res1.getName().equals(res2.getName())) continue; // sequence mismatch
            if (verbose) System.err.println("Comparing "+res1+" to "+res2+"...");
            
            Residue prev1 = res1.getPrev(model1);
            Residue next1 = res1.getNext(model1);
            Residue prev2 = res2.getPrev(model2);
            Residue next2 = res2.getNext(model2);
            
            try
            {
                Atom calpha1 = res1.getAtom(" CA ");
                Atom calpha2 = res2.getAtom(" CA ");
                AtomState ca1 = state1.get(calpha1);
                AtomState ca2 = state2.get(calpha2);
                double caTravel = Triple.distance(ca1, ca2);
                
                double phi1 = Double.NaN, psi1 = Double.NaN;
                if (prev1 != null && next1 != null)
                {
                    phi1 = calcPhi(prev1, res1, state1);
                    psi1 = calcPsi(res1, next1, state1);
                }
                double phi2 = Double.NaN, psi2 = Double.NaN;
                if (prev2 != null && next2 != null)
                {
                    phi2 = calcPhi(prev2, res2, state2);
                    psi2 = calcPsi(res2, next2, state2);
                }
                double dPhi = Double.NaN, dPsi = Double.NaN;
                if(!Double.isNaN(phi1) && !Double.isNaN(psi1)
                && !Double.isNaN(phi2) && !Double.isNaN(psi2))
                {
                    dPhi = (absVal ? Math.abs(phi2-phi1) : phi2-phi1);
                    dPsi = (absVal ? Math.abs(psi2-psi1) : psi2-psi1);
                }
                
                if ( allRes || (!Double.isNaN(dPhi) && !Double.isNaN(dPsi) 
                && (dPhi != 0 || dPsi != 0 || caTravel != 0)) )
                {
                    // Either something changed and is therefore worth printing 
                    // or we want to print stats for all residues regardless of 
                    // whether anything changed.
                    out.print(label1+":"+label2+":"+model1+":"+model2+":"
                        +res1.getChain()+":"+res2.getChain()+":"
                        +res1.getName()+":"+res2.getName()+":"
                        +res1.getSequenceInteger()+":"+res2.getSequenceInteger()+":");
                    if (!Double.isNaN(dPhi)) out.print(df.format(dPhi)+":");
                    else                     out.print("__?__:");
                    if (!Double.isNaN(dPsi)) out.print(df.format(dPsi)+":");
                    else                     out.print("__?__:");
                    out.println(df.format(caTravel));
                }
            }
            catch (AtomException ae) { }
            
        } //for each residue pair in alignment
    }
//}}}

//{{{ calcPhi, calcPsi
//##############################################################################
    double calcPhi(Residue prev, Residue res, ModelState state)
    {
        try
        {
            Atom prevC = prev.getAtom(" C  ");
            Atom n     = res.getAtom(" N  ");
            Atom ca    = res.getAtom(" CA ");
            Atom c     = res.getAtom(" C  ");
            
            AtomState prevCState = state.get(prevC);
            AtomState nState     = state.get(n);
            AtomState caState    = state.get(ca);
            AtomState cState     = state.get(c);
            
            return Triple.dihedral(prevCState, nState, cState, caState);
        }
        catch (AtomException ae) { return Double.NaN; }
    }

    double calcPsi(Residue res, Residue next, ModelState state)
    {
        try
        {
            Atom n     = res.getAtom(" N  ");
            Atom ca    = res.getAtom(" CA ");
            Atom c     = res.getAtom(" C  ");
            Atom nextN = next.getAtom(" N  ");
            
            AtomState nState     = state.get(n);
            AtomState caState    = state.get(ca);
            AtomState cState     = state.get(c);
            AtomState nextNState = state.get(nextN);
            
            return Triple.dihedral(nState, cState, caState, nextNState);
        }
        catch (AtomException ae) { return Double.NaN; }
    }
//}}}

//{{{ CLASS: SimpleResAligner
//##############################################################################
    static class SimpleResAligner implements Alignment.Scorer
    {
        // High is good, low is bad.
        public int score(Object a, Object b)
        {
            Residue r = (Residue) a;
            Residue s = (Residue) b;
            if(r == null || s == null)
                return -1;  // gap
            else if(r.getName().equals(s.getName()))
                return 2;   // match
            else
                return 0;   // mismatch
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
        if (filename1 == null && filename2 == null)
        {
            System.err.println("Need at least one filename!");
            System.exit(0);
        }
        try
        {
            if (filename1 != null && filename2 == null)
            {
                // Looking for alt conf loops in one structure
                PdbReader reader = new PdbReader();
                File f = new File(filename1);
                CoordinateFile cf = reader.read(f);
                out.println("label:model:chain:res_type:res_num:dPhi:dPsi:Ca-Ca");
                for(Iterator models = cf.getModels().iterator(); models.hasNext(); )
                {
                    Model m = (Model) models.next();
                    String label = f.toString();
                    if(cf.getIdCode() != null) label = cf.getIdCode();
                    searchModel(label, m);
                }
            }
            else if (filename1 != null && filename2 != null)
            {
                // Looking for regions that vary between two structures
                PdbReader reader = new PdbReader();
                File f1 = new File(filename1);
                File f2 = new File(filename2);
                CoordinateFile cf1 = reader.read(f1);
                CoordinateFile cf2 = reader.read(f2);
                Model m1 = cf1.getFirstModel();
                Model m2 = cf2.getFirstModel();
                String label1 = f1.toString();
                String label2 = f2.toString();
                out.println("label1:label2:model1:model2:chain1:chain2:res_type1:res_type2:res_num1:res_num2:dPhi:dPsi:Ca-Ca");
                searchModels(m1, m2, label1, label2);
            }
        }
        catch (IOException ioe) { System.err.println("Trouble parsing files!"); }
    }

    public static void main(String[] args)
    {
        VariableRegions mainprog = new VariableRegions();
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
            InputStream is = getClass().getResourceAsStream("VariableRegions.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'VariableRegions.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.VariableRegions");
        System.err.println("Copyright (C) 2007 by Daniel A. Keedy. All rights reserved.");
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
        if (filename1 == null)         filename1 = arg;
        else if (filename2 == null)    filename2 = arg;
        else throw new IllegalArgumentException("Only need 1 or 2 files!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-absval") || flag.equals("-abs"))
        {
            absVal = true;
        }
        else if(flag.equals("-allres") || flag.equals("-all"))
        {
            allRes = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

