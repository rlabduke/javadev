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
* <code>TurnFinder</code> finds tight turns or pseudo-turns in a PDB file
* and prints simple information about them.
*
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Aug 10 2010
*/
public class TurnFinder //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("0.0##");
//}}}

//{{{ Variable definitions
//##############################################################################
    ArrayList inputList = new ArrayList();
    boolean pseudo = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public TurnFinder()
    {
        super();
    }
//}}}

//{{{ findTightTurns
//##############################################################################
    /**
    * Finds and stores tight turns, defined as in {@link TightTurn}.
    */
    public Collection findTightTurns(Model model, ModelState state)
    {
        Collection turns = new ArrayList();
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            // Make sure all four residues are present
            Residue r0 = (Residue) iter.next();  if(r0 == null) continue;
            Residue r1 = r0.getNext(model);      if(r1 == null) continue;
            Residue r2 = r1.getNext(model);      if(r2 == null) continue;
            Residue r3 = r2.getNext(model);      if(r3 == null) continue;
            
            try
            {
                Turn turn = new TightTurn(model, state, r0, r1, r2, r3);
                if(turn.hbEnergy0to3 < -0.5 && turn.hbEnergy0to4 >= -0.5 && Math.abs(turn.dihedral) < 60)
                {
                    // NB: If an H-bond from 1 to 4 exists, that's 
                    // basically a helix N-cap, not a beta turn!
                    turns.add(turn);
                    System.err.println("Added "+turn);
                }
                else turn = null; // to avoid memory leak (?)
            }
            catch(AtomException ex) {} // missing atoms, or no H-bond
            catch(ResidueException ex) {}
        }
        
        return turns;
    }
//}}}

//{{{ findPseudoTurns
//##############################################################################
    /**
    * Finds and stores pseudo-turns, defined as in {@link PseudoTurn}.
    */
    public Collection findPseudoTurns(Model model, ModelState state)
    {
        Collection turns = new ArrayList();
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            // Make sure all three residues are present
            Residue r1 = (Residue) iter.next();  if(r1 == null) continue;
            Residue r2 = r1.getNext(model);      if(r2 == null) continue;
            Residue r3 = r2.getNext(model);      if(r3 == null) continue;
            if(!r1.getName().equals("ASN") && !r1.getName().equals("ASP")) continue;
            
            try
            {
                Turn turn = new PseudoTurn(model, state, r1, r2, r3);
                if(turn.hbEnergy0to3 < -0.5 && turn.hbEnergy0to4 >= -0.5 && Math.abs(turn.dihedral) < 60)
                {
                    // NB: If an H-bond from 1 to 4 exists, that's 
                    // basically a helix N-cap, not a beta turn!
                    turns.add(turn);
                    System.err.println("Added "+turn);
                }
                else turn = null; // to avoid memory leak (?)
            }
            catch(AtomException ex) {} // missing atoms, or no H-bond
            catch(ResidueException ex) {}
        }
        
        return turns;
    }
//}}}

//{{{ printTightTurns
//##############################################################################
    public void printTightTurns(Collection turns, Model model, ModelState state, String fileName)
    {
        System.err.println("Found "+turns.size()+" tight turns in "+fileName);
        for(Iterator ti = turns.iterator(); ti.hasNext(); )
        {
            TightTurn t = (TightTurn) ti.next();
            System.out.println(
                fileName+":"+
                t.r0.getChain()+":"+t.r0.getSequenceNumber().trim()+":"+t.r0.getInsertionCode()+":"+t.r0.getName()+":"+
                t.r1.getChain()+":"+t.r1.getSequenceNumber().trim()+":"+t.r1.getInsertionCode()+":"+t.r1.getName()+":"+
                t.r2.getChain()+":"+t.r2.getSequenceNumber().trim()+":"+t.r2.getInsertionCode()+":"+t.r2.getName()+":"+
                t.r3.getChain()+":"+t.r3.getSequenceNumber().trim()+":"+t.r3.getInsertionCode()+":"+t.r3.getName()+":"+
                AminoAcid.isCisPeptide(model, t.r0, state)+":"+
                AminoAcid.isCisPeptide(model, t.r1, state)+":"+
                AminoAcid.isCisPeptide(model, t.r2, state)+":"+
                AminoAcid.isCisPeptide(model, t.r3, state)+":"+
                t.type+":"+
                df.format(t.hbEnergy0to3)+":"+
                df.format(t.hbEnergy0to4)+":"+
                df.format(t.dihedral)+":"+
                df.format(t.phi0)+":"+
                df.format(t.psi0)+":"+
                df.format(t.phi1)+":"+
                df.format(t.psi1)+":"+
                df.format(t.phi2)+":"+
                df.format(t.psi2)+":"+
                df.format(t.phi3)+":"+
                df.format(t.psi3)+":"+
                df.format(t.highB)
            );
        }
    }
//}}}

//{{{ printPseudoTurns
//##############################################################################
    public void printPseudoTurns(Collection turns, Model model, ModelState state, String fileName)
    {
        System.err.println("Found "+turns.size()+" pseudo-turns in "+fileName);
        for(Iterator ti = turns.iterator(); ti.hasNext(); )
        {
            PseudoTurn t = (PseudoTurn) ti.next();
            System.out.println(
                fileName+":"+
                t.r1.getChain()+":"+t.r1.getSequenceNumber().trim()+":"+t.r1.getInsertionCode()+":"+t.r1.getName()+":"+
                t.r2.getChain()+":"+t.r2.getSequenceNumber().trim()+":"+t.r2.getInsertionCode()+":"+t.r2.getName()+":"+
                t.r3.getChain()+":"+t.r3.getSequenceNumber().trim()+":"+t.r3.getInsertionCode()+":"+t.r3.getName()+":"+
                AminoAcid.isCisPeptide(model, t.r1, state)+":"+
                AminoAcid.isCisPeptide(model, t.r2, state)+":"+
                AminoAcid.isCisPeptide(model, t.r3, state)+":"+
                t.type+":"+
                df.format(t.hbEnergy0to3)+":"+
                df.format(t.hbEnergy0to4)+":"+
                df.format(t.dihedral)+":"+
                df.format(t.phi1)+":"+
                df.format(t.psi1)+":"+
                df.format(t.phi2)+":"+
                df.format(t.psi2)+":"+
                df.format(t.phi3)+":"+
                df.format(t.psi3)+":"+
                df.format(t.highB)
            );
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
        PdbReader reader = new PdbReader();
        for(Iterator iter = inputList.iterator(); iter.hasNext(); )
        {
            String fileName = (String) iter.next();
            try
            {
                CoordinateFile cf = reader.read(new File(fileName));
                Model model = cf.getFirstModel();
                ModelState state = model.getState();
                
                if(pseudo) // pseudo-turns (sc/mc-swapped)
                {
                    Collection turns = findPseudoTurns(model, state);
                    printPseudoTurns(turns, model, state, fileName);
                }
                else // regular tight turns
                {
                    Collection turns = findTightTurns(model, state);
                    printTightTurns(turns, model, state, fileName);
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
        TurnFinder mainprog = new TurnFinder();
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
            InputStream is = getClass().getResourceAsStream("TurnFinder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'TurnFinder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.TurnFinder");
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
        else if(flag.equals("-pseudo"))
        {
            pseudo = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

