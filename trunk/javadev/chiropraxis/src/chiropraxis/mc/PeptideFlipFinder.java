// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

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
//}}}
/**
* <code>PeptideFlipFinder</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan  3 15:44:04 EST 2005
*/
public class PeptideFlipFinder //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    ArrayList inputList = new ArrayList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PeptideFlipFinder()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ findPeptideFlips
//##############################################################################
    /**
    * Finds peptide flips defined as alternate conformations / states, and returns those residues.
    * A flip has C--O vectors offset by at least 90 degrees.
    */
    public Collection findPeptideFlips(Model model, ModelState[] states)
    {
        Collection flips = new ArrayList();
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            Atom c = r.getAtom(" C  ");
            Atom o = r.getAtom(" O  ");
            if(c == null || o == null) continue;
            
            // Calculate O--C vector for each state
            ArrayList vectors = new ArrayList();
            for(int i = 0; i < states.length; i++)
            {
                // In A/B cases, _ will probably be missing states!
                try { vectors.add(new Triple().like( states[i].get(o) ).sub( states[i].get(c) )); }
                catch(AtomException ex) {}
            }
            
            // Compare all pairs of vectors in this residue
            COMPARE:
            for(int i = 0; i < vectors.size(); i++)
            {
                for(int j = i+1; j < vectors.size(); j++)
                {
                    if(((Triple)vectors.get(i)).angle((Triple)vectors.get(j)) >= 90.0)
                    {
                        flips.add(r);
                        break COMPARE;
                    }
                }
            }
        }
        
        return flips;
    }
//}}}

//{{{ findPeptideFlips2
//##############################################################################
    /**
    * Finds peptide flips defined as alternate conformations / states, and returns those residues.
    * A flip has oxygens rotated at least 90 degrees around the Ca--Ca vector.
    */
    public Collection findPeptideFlips2(Model model, ModelState[] states)
    {
        Collection flips = new ArrayList();
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            Residue r2 = r.getNext(model);
            if(r2 == null) continue;
            
            // Average pos of first CA
            Triple ca1 = new Triple();
            int caCount = 0;
            for(int i = 0; i < states.length; i++)
            {
                try { ca1.add( states[i].get(r.getAtom(" CA ")) ); caCount++; }
                catch(AtomException ex) {}
            }
            if(caCount == 0) continue;
            ca1.div(caCount);
            
            // Average pos of second CA
            Triple ca2 = new Triple();
            caCount = 0;
            for(int i = 0; i < states.length; i++)
            {
                try { ca2.add( states[i].get(r2.getAtom(" CA ")) ); caCount++; }
                catch(AtomException ex) {}
            }
            if(caCount == 0) continue;
            ca2.div(caCount);
            
            // Find O's for each state
            ArrayList ohs = new ArrayList();
            for(int i = 0; i < states.length; i++)
            {
                // In A/B cases, _ will probably be missing states!
                try { ohs.add( states[i].get(r.getAtom(" O  ")) ); }
                catch(AtomException ex) {}
            }
            
            // Compare all pairs of O's in this residue around Ca--Ca axis
            COMPARE:
            for(int i = 0; i < ohs.size(); i++)
            {
                for(int j = i+1; j < ohs.size(); j++)
                {
                    Tuple3 oi = (Tuple3) ohs.get(i);
                    Tuple3 oj = (Tuple3) ohs.get(j);
                    if(Math.abs(Triple.dihedral(oi, ca1, ca2, oj)) >= 90.0)
                    {
                        flips.add(r);
                        break COMPARE;
                    }
                }
            }
        }
        
        return flips;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        PdbReader reader = new PdbReader();
        for(Iterator iter = inputList.iterator(); iter.hasNext(); )
        {
            String fileName = (String) iter.next();
            try
            {
                ModelGroup group = reader.read(new File(fileName));
                Model model = group.getFirstModel();
                Collection stateClcn = model.getStates();
                ModelState[] states = (ModelState[]) stateClcn.toArray(new ModelState[stateClcn.size()]);
                
                Collection flipRes = findPeptideFlips(model, states);
                for(Iterator fi = flipRes.iterator(); fi.hasNext(); )
                {
                    Residue r = (Residue) fi.next();
                    System.out.println(fileName+" : "+r);
                }
            }
            catch(IOException ex)
            {
                ex.printStackTrace();
                System.err.println("Error reading "+fileName);
            }
        }
    }

    public static void main(String[] args)
    {
        PeptideFlipFinder mainprog = new PeptideFlipFinder();
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
            InputStream is = getClass().getResourceAsStream("PeptideFlipFinder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'PeptideFlipFinder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.PeptideFlipFinder");
        System.err.println("Copyright (C) 2005 by Ian W. Davis. All rights reserved.");
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
        inputList.add(arg);
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

