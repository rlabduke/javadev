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
* <code>AltConfBackrubFinder</code> searches through the alternate conformations
* of a crystal structure, looking for residues that appear to undergo
* a Backrub-like conformational change.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Apr 29 11:08:49 EDT 2004
*/
public class AltConfBackrubFinder //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Collection      inputFiles;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AltConfBackrubFinder()
    {
        super();
        inputFiles = new ArrayList();
    }
//}}}

//{{{ searchModel
//##############################################################################
    void searchModel(PrintStream out, String label, Model model)
    {
        DecimalFormat df = new DecimalFormat("0.0####");
        final double maxCaShift = 0.01; // less than 2% more examples at 0.1 A allowance
        Collection stateC = model.getStates();
        ModelState[] states = (ModelState[]) stateC.toArray(new ModelState[stateC.size()]);
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Residue prev = res.getPrev(model);
            Residue next = res.getNext(model);
            if(prev == null || next == null) continue;
            Atom thisCb = res.getAtom(" CB ");
            Atom prevCa = prev.getAtom(" CA ");
            Atom nextCa = next.getAtom(" CA ");
            // vectors between atom pairs for max Cb move
            Triple maxCaCa = new Triple(1,0,0), maxCbCb = new Triple(1,0,0);
            double maxCbTravel = 0;
            // Test between all pairs of states
            for(int i = 0; i < states.length; i++)
            {
                for(int j = i+1; j < states.length; j++)
                {
                    try
                    {
                        AtomState prevCa1 = states[i].get(prevCa);
                        AtomState prevCa2 = states[j].get(prevCa);
                        AtomState nextCa1 = states[i].get(nextCa);
                        AtomState nextCa2 = states[j].get(nextCa);
                        // If Ca's move too far, skip this one.
                        if(prevCa1.distance(prevCa2) > maxCaShift || nextCa1.distance(nextCa2) > maxCaShift) continue;
                        // Otherwise, test the Cb distance
                        AtomState thisCb1 = states[i].get(thisCb);
                        AtomState thisCb2 = states[j].get(thisCb);
                        double cbdist = thisCb1.distance(thisCb2);
                        if(cbdist > maxCbTravel)
                        {
                            maxCbTravel = cbdist;
                            Triple prevCaMid = new Triple().likeMidpoint(prevCa1, prevCa2);
                            Triple nextCaMid = new Triple().likeMidpoint(nextCa1, nextCa2);
                            maxCaCa.likeVector(prevCaMid, nextCaMid);
                            maxCbCb.likeVector(thisCb1, thisCb2);
                        }
                    }
                    catch(AtomException ex) {}
                }//for j states
            }//for i states
            if(maxCbTravel > 0)
                out.println(label+":"+model+":"+res.getCNIT()+":"+df.format(maxCbTravel)+":"+df.format(maxCaCa.angle(maxCbCb)));
        }//for each residue
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        PdbReader reader = new PdbReader();
        for(Iterator files = inputFiles.iterator(); files.hasNext(); )
        {
            File f = (File) files.next();
            try
            {
                CoordinateFile cf = reader.read(f);
                for(Iterator models = cf.getModels().iterator(); models.hasNext(); )
                {
                    Model m = (Model) models.next();
                    String label = f.toString();
                    if(cf.getIdCode() != null) label = cf.getIdCode();
                    searchModel(System.out, label, m);
                }
            }
            catch(IOException ex)
            { System.err.println("IOException when processing "+f); }
        }
    }

    public static void main(String[] args)
    {
        AltConfBackrubFinder mainprog = new AltConfBackrubFinder();
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
            InputStream is = getClass().getResourceAsStream("AltConfBackrubFinder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AltConfBackrubFinder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AltConfBackrubFinder");
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
        // Handle files, etc. here
        File f = new File(arg);
        if(f.isFile()) inputFiles.add(f);
        else throw new IllegalArgumentException("'"+arg+"' is not a valid file name.");
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

