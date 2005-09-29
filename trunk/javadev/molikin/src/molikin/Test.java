// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Test</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon May  9 08:54:43 EDT 2005
*/
public class Test //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat memdf = new DecimalFormat("##0.0E0");
//}}}

//{{{ Variable definitions
//##############################################################################
    // Collections of AtomStates:
    Set allH        = new CheapSet(new IdentityHashFunction());
    Set hetH        = new CheapSet(new IdentityHashFunction());
    Set waterH      = new CheapSet(new IdentityHashFunction());
    Set otherH      = new CheapSet(new IdentityHashFunction());
    Set nonHetH     = new CheapSet(new IdentityHashFunction());
    Set mcH         = new CheapSet(new IdentityHashFunction());
    Set scH         = new CheapSet(new IdentityHashFunction());
    
    Set allNonH     = new CheapSet(new IdentityHashFunction());
    Set hetNonH     = new CheapSet(new IdentityHashFunction());
    Set waterNonH   = new CheapSet(new IdentityHashFunction());
    Set otherNonH   = new CheapSet(new IdentityHashFunction());
    Set nonHetNonH  = new CheapSet(new IdentityHashFunction());
    Set mcNonH      = new CheapSet(new IdentityHashFunction());
    Set scNonH      = new CheapSet(new IdentityHashFunction());
    
    boolean inputIsCIF = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Test()
    {
        super();
    }
//}}}

//{{{ segregateAtomStates
//##############################################################################
    void segregateAtomStates(Collection states)
    {
        for(Iterator iter = states.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(Util.isH(as))
            {
                allH.add(as);
                if(as.isHet())
                {
                    hetH.add(as);
                    if(Util.isWater(as))        waterH.add(as);
                    else                        otherH.add(as);
                }
                else // non het
                {
                    nonHetH.add(as);
                    if(Util.isMainchain(as))    mcH.add(as);
                    else                        scH.add(as);
                }
            }
            else // heavy atom
            {
                allNonH.add(as);
                if(as.isHet())
                {
                    hetNonH.add(as);
                    if(Util.isWater(as))        waterNonH.add(as);
                    else                        otherNonH.add(as);
                }
                else // non het
                {
                    nonHetNonH.add(as);
                    if(Util.isMainchain(as))    mcNonH.add(as);
                    else                        scNonH.add(as);
                }
            }
        }// for each atom state
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
    public void Main() throws IOException
    {
        if(inputIsCIF)  doCIF(new InputStreamReader(System.in), new OutputStreamWriter(System.out));
        else            doPDB(new InputStreamReader(System.in), new OutputStreamWriter(System.out));
    }

    public static void main(String[] args)
    {
        Test mainprog = new Test();
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

//{{{ doPDB, doCIF
//##############################################################################
    public void doPDB(Reader pdbIn, Writer kinOut) throws IOException
    {
        long            time        = System.currentTimeMillis();
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(pdbIn);
        Model           model       = coordFile.getFirstModel();
        time = System.currentTimeMillis() - time;
        System.err.println("Loading PDB:         "+time+" ms");
        System.err.println("Mem. usage:          "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        
        PrintWriter out = new PrintWriter(kinOut);
        doModel(model, out);
        out.flush();
        System.err.println("Mem. usage:          "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
    }

    public void doCIF(Reader cifIn, Writer kinOut) throws IOException
    {
        long            time        = System.currentTimeMillis();
        CifReader       cifReader   = new CifReader();
        CoordinateFile  coordFile   = cifReader.read(cifIn);
        Model           model       = coordFile.getFirstModel();
        time = System.currentTimeMillis() - time;
        System.err.println("Loading mmCIF:       "+time+" ms");
        System.err.println("Mem. usage:          "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        
        PrintWriter out = new PrintWriter(kinOut);
        doModel(model, out);
        out.flush();
        System.err.println("Mem. usage:          "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
    }
//}}}

//{{{ doModel
//##############################################################################
    public void doModel(Model model, PrintWriter out)
    {
        /* For testing ResClassifier
        ResClassifier rc = new ResClassifier(model.getResidues());
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            System.err.println(r+"    "+rc.classify(r));
        }*/
        
        long time;
        time = System.currentTimeMillis();
        Collection atomStates = Util.extractOrderedStatesByName(model);
        AtomGraph graph = new AtomGraph(atomStates);
        time = System.currentTimeMillis() - time;
        System.err.println("Initializing graph:  "+time+" ms");

        time = System.currentTimeMillis();
        Collection bonds = graph.getCovalentBonds();
        time = System.currentTimeMillis() - time;
        System.err.println("Determining network: "+time+" ms");

        time = System.currentTimeMillis();
        segregateAtomStates(atomStates);
        time = System.currentTimeMillis() - time;
        System.err.println("Classifying atoms:   "+time+" ms");

        time = System.currentTimeMillis();
        out.println("@kinemage");
        out.println("@onewidth");
        Collection someBonds;
        someBonds = Util.selectBondsBetween(bonds, mcNonH, mcNonH);
        printBonds(out, someBonds, "mc", "white");
        someBonds = Util.selectBondsBetween(bonds, mcH, nonHetNonH);
        printBonds(out, someBonds, "mcH", "gray");
        
        // Disulfides are different:
        someBonds = Util.selectBondsBetween(bonds, scNonH, nonHetNonH);
        Collection ssBonds = new TreeSet();
        for(Iterator iter = someBonds.iterator(); iter.hasNext(); )
        {
            Bond b = (Bond) iter.next();
            if(Util.isDisulfide(b))
            {
                iter.remove();
                ssBonds.add(b);
            }
        }
        printBonds(out, someBonds, "sc", "cyan");
        printBonds(out, ssBonds,   "SS", "yellow");
            
        someBonds = Util.selectBondsBetween(bonds, scH, nonHetNonH);
        printBonds(out, someBonds, "scH", "gray");
        someBonds = Util.selectBondsBetween(bonds, hetNonH, allNonH);
        printBonds(out, someBonds, "het", "pink");
        someBonds = Util.selectBondsBetween(bonds, hetH, hetNonH);
        printBonds(out, someBonds, "hetH", "gray");
        // "Free" het atoms (metals, etc)
        Set freeAtoms = new CheapSet(new IdentityHashFunction());
        freeAtoms.addAll(graph.getCovalentUnbonded());
        //for(Iterator iter = waterNonH.iterator(); iter.hasNext(); ) freeAtoms.remove(iter.next());
        freeAtoms.removeAll(waterNonH);
        printAtoms(out, freeAtoms, "unbonded", "lime");
        // Waters are also special:
        printAtoms(out, waterNonH, "water", "peachtint");
        someBonds = Util.selectBondsBetween(bonds, waterH, waterNonH);
        printBonds(out, someBonds, "waterH", "gray");
        time = System.currentTimeMillis() - time;
        System.err.println("Drawing bonds:       "+time+" ms");
    }
//}}}

//{{{ printBonds
//##############################################################################
    static void printBonds(PrintWriter out, Collection bonds, String title, String color)
    {
        if(bonds.size() == 0) return;
        
        DecimalFormat df = new DecimalFormat("0.000");
        out.println("@vectorlist {"+title+"} color= "+color);
        Bond last = new Bond(null, -1, null, -1);
        for(Iterator iter = bonds.iterator(); iter.hasNext(); )
        {
            Bond curr = (Bond) iter.next();
            if(curr.iLow != last.iHigh)
                out.print("{"+curr.lower.getAtom()+"}P "+curr.lower.format(df)+" ");
            out.println("{"+curr.higher.getAtom()+"}L "+curr.higher.format(df));
            last = curr;
        }
    }
//}}}

//{{{ printAtoms
//##############################################################################
    static void printAtoms(PrintWriter out, Collection atomStates, String title, String color)
    {
        if(atomStates.size() == 0) return;
        
        DecimalFormat df = new DecimalFormat("0.000");
        out.println("@balllist {"+title+"} radius= 0.15 color= "+color);
        for(Iterator iter = atomStates.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            out.println("{"+as.getAtom()+"}L "+as.format(df));
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
            InputStream is = getClass().getResourceAsStream("Test.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Test.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("molikin.Test");
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
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-cif") || flag.equals("mmcif")) inputIsCIF = true;
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

