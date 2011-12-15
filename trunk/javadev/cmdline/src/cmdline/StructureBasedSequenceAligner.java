// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

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
* <code>StructureBasedSequenceAligner</code> reports residue correspondences
* based on CA-CA distances in pre-superimposed structures.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Apr 18 2011
*/
public class StructureBasedSequenceAligner //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("0.##");
//}}}

//{{{ CLASS: StructureBasedResAligner
//##############################################################################
    public static class StructureBasedResAligner implements Alignment.Scorer
    {
        double min = -1; // same as in 
        double max =  4; // SimpleResAligner
        
        // High is good, low is bad.
        public double score(Object a, Object b)
        {
            AtomState a1 = (AtomState) a;
            AtomState a2 = (AtomState) b;
            double dist = a1.distance(a2);
            
            // Start at max score and decrease linearly with distance,
            // but truncate at min score
            double score = max - dist;
            return Math.max(score, min);
        }
        
        public double open_gap(Object a) { return -8; }
        public double extend_gap(Object a) { return -2; }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    String structIn1 = null, structIn2 = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public StructureBasedSequenceAligner()
    {
        super();
    }
//}}}

//{{{ getCalphas
//##############################################################################
    /**
    * Get all C-alpha atoms in the given 
    */
    public AtomState[] getCalphas(Model m, ModelState s)
    {
        // Find the C-alphas
        ArrayList cAlphas = new ArrayList();
        for(Iterator rIter = m.getResidues().iterator(); rIter.hasNext(); )
        {
            Residue r = (Residue) rIter.next();
            for(Iterator aIter = r.getAtoms().iterator(); aIter.hasNext(); )
            {
                Atom a = (Atom) aIter.next();
                if(a.getName().equals(" CA "))
                {
                    try
                    {
                        AtomState as = s.get(a);
                        cAlphas.add(as);
                        break; // done with this residue
                    }
                    catch(AtomException ex)
                    {
                        System.err.println("Can't get CA "+a+" from "+r+" in model "+m);
                    }
                }
            }
            
        }
        
        // Put them into an array and return
        AtomState[] ca = new AtomState[cAlphas.size()];
        for(int i = 0; i < cAlphas.size(); i++)
            ca[i] = (AtomState) cAlphas.get(i);
        return ca;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, ParseException
    {
        // Read in structures, get arrays of atoms.
        if(structIn1 == null || structIn2 == null)
            throw new IllegalArgumentException("must provide two structures");
        PdbReader pdbReader = new PdbReader();
        File file1 = new File(structIn1);
        File file2 = new File(structIn2);
        CoordinateFile coord1 = pdbReader.read(file1);
        CoordinateFile coord2 = pdbReader.read(file2);
        Model m1 = coord1.getFirstModel();
        Model m2 = coord2.getFirstModel();
        AtomState[] atoms1 = getCalphas(m1, m1.getState());
        AtomState[] atoms2 = getCalphas(m2, m2.getState());
        
        // Align atoms by structure
        Alignment.Scorer scorer = new StructureBasedResAligner();
        Alignment align = Alignment.needlemanWunsch(atoms1, atoms2, scorer);
        if(verbose) System.err.println("Residue alignments:");
        for(int i = 0; i < align.a.length; i++)
        {
            AtomState as1 = (AtomState) align.a[i];
            AtomState as2 = (AtomState) align.b[i];
            Residue r1 = (as1 == null ? null : as1.getAtom().getResidue());
            Residue r2 = (as2 == null ? null : as2.getAtom().getResidue());
            double dist = (as1 == null || as2 == null ? Double.NaN : as1.distance(as2));
            double score = (as1 == null || as2 == null ? Double.NaN : scorer.score(as1, as2));
            
            if(verbose) System.err.println("  "+r1+" <==> "+r2+"  "+df.format(dist)+"  "+df.format(score));
            
            String output1 = file1.getName();
            if(r1 == null) output1 += ",NULL,NULL,NULL,NULL";
            else output1 += ","+r1.getChain()+","+r1.getSequenceInteger()+","
                +r1.getInsertionCode().trim()+","+r1.getName();
            String output2 = file2.getName();
            if(r2 == null) output2 += ",NULL,NULL,NULL,NULL";
            else output2 += ","+r2.getChain()+","+r2.getSequenceInteger()+","
                +r2.getInsertionCode().trim()+","+r2.getName();
            String output3 = (as1 == null || as2 == null ? "NULL" : df.format(dist));
            System.out.println(output1+","+output2+","+output3);
        }
    }

    public static void main(String[] args)
    {
        StructureBasedSequenceAligner mainprog = new StructureBasedSequenceAligner();
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
            System.exit(2);
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
            InputStream is = getClass().getResourceAsStream("StructureBasedSequenceAligner.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'StructureBasedSequenceAligner.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.StructureBasedSequenceAligner");
        System.err.println("Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.");
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
        if(structIn1 == null)       structIn1 = arg;
        else if(structIn2 == null)  structIn2 = arg;
        else throw new IllegalArgumentException("too many arguments!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-v"))
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

