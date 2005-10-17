// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;
import molikin.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.data.*;
import driftwood.gui.*;
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
    boolean inputIsCIF = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Test()
    {
        super();
    }
//}}}

//{{{ showGUI
//##############################################################################
    PrintWriter outWriter = null;
    JDialog frame = null;
    MainGuiPane guiPane = null;

    void showGUI(CoordinateFile cfile, PrintWriter out)
    {
        outWriter = out;
        guiPane = new MainGuiPane(cfile);
        guiPane.right().addCell(new JButton(new ReflectiveAction("Make kinemage", null, this, "onMakeKinemage")));
        
        JFrame parent = new JFrame();
        frame = new JDialog(parent, "Choose residues to display", true); // modal
        //frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(guiPane);
        frame.pack();
        frame.setVisible(true);
        parent.dispose();
    }
    
    public void onMakeKinemage(ActionEvent ev)
    {
        frame.dispose();
        outWriter.println("@kinemage");
        outWriter.println("@onewidth");
        for(Iterator iter = guiPane.getAllPanes().iterator(); iter.hasNext(); )
        {
            DrawingPane p = (DrawingPane) iter.next();
            p.printKinemage(outWriter);
        }
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
        time = System.currentTimeMillis() - time;
        System.err.println("Loading PDB:            "+time+" ms");
        System.err.println("Mem. usage:             "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())+" bytes");
        
        PrintWriter out = new PrintWriter(kinOut);
        //doModel(coordFile, out);
        showGUI(coordFile, out);
        out.flush();
        System.err.println("Mem. usage:             "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())+" bytes");
    }

    public void doCIF(Reader cifIn, Writer kinOut) throws IOException
    {
        long            time        = System.currentTimeMillis();
        CifReader       cifReader   = new CifReader();
        CoordinateFile  coordFile   = cifReader.read(cifIn);
        time = System.currentTimeMillis() - time;
        System.err.println("Loading mmCIF:          "+time+" ms");
        System.err.println("Mem. usage:             "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())+" bytes");
        
        PrintWriter out = new PrintWriter(kinOut);
        //doModel(coordFile, out);
        showGUI(coordFile, out);
        out.flush();
        System.err.println("Mem. usage:             "+memdf.format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())+" bytes");
    }
//}}}

//{{{ doModel
//##############################################################################
    public void doModel(CoordinateFile coordFile, PrintWriter out)
    {
        Model model = coordFile.getFirstModel();
        
        // Test of GUI for residue selections
        Set allResidues = new CheapSet(model.getResidues());
        Set selectedResidues = allResidues;
        //Set selectedResidues = showGUI(coordFile);
        //System.err.println("# residues selected:    "+selectedResidues.size()+" res");
        
        /* For testing ResClassifier * /
        ResClassifier rc = new ResClassifier(model.getResidues());
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            System.err.println(r+"    "+rc.classify(r));
        }
        /* For testing ResClassifier */
        
        long time;
        time = System.currentTimeMillis();
    ResClassifier resC = new ResClassifier(model.getResidues());
        time = System.currentTimeMillis() - time;
        System.err.println("Classifying residues:   "+time+" ms");

        time = System.currentTimeMillis();
    Collection atomStates = Util.extractOrderedStatesByName(model);
    AtomClassifier atomC = new AtomClassifier(atomStates, resC);
        time = System.currentTimeMillis() - time;
        System.err.println("Classifying atoms:      "+time+" ms");

        time = System.currentTimeMillis();
    AtomGraph graph = new AtomGraph(atomStates);
        time = System.currentTimeMillis() - time;
        System.err.println("Spatial binning:        "+time+" ms");

        time = System.currentTimeMillis();
    Collection bonds = graph.getBonds();
        time = System.currentTimeMillis() - time;
        System.err.println("Building bond network:  "+time+" ms");

        time = System.currentTimeMillis();
        StickPrinter sp = new StickPrinter(out);
        BallPrinter bp = new BallPrinter(out);
        out.println("@kinemage");
        out.println("@onewidth");
        out.println("@group {macromol}");
        out.println("@subgroup {backbone}");
        out.println("@vectorlist {heavy} color= white");
        sp.printSticks(bonds, atomC.bbHeavy, atomC.bbHeavy);
        out.println("@vectorlist {H} color= gray master= {Hs}");
        sp.printSticks(bonds, atomC.bbHydro, atomC.bbHeavy);
        out.println("@subgroup {sidechain}");
        out.println("@vectorlist {heavy} color= cyan"); // includes disulfides, for now
        sp.printSticks(bonds, atomC.scHeavy, atomC.bioHeavy, selectedResidues, allResidues); // to scHeavy if we want stubs to ribbon instead
        out.println("@vectorlist {H} color= gray master= {Hs}");
        sp.printSticks(bonds, atomC.scHydro, atomC.bioHeavy, selectedResidues, allResidues); // makes sure Gly 2HA connects to bb
        if(atomC.hetHeavy.size() > 0)
        {
            out.println("@subgroup {hets}");
            out.println("@vectorlist {heavy} color= pink");
            sp.printSticks(bonds, atomC.hetHeavy, atomC.hetHeavy);
            out.println("@vectorlist {H} color= gray master= {Hs}");
            sp.printSticks(bonds, atomC.hetHydro, atomC.hetHeavy);
            out.println("@vectorlist {connect} color= pinktint");
            sp.printSticks(bonds, atomC.hetHeavy, atomC.bioHeavy);
        }
        if(atomC.ion.size() > 0)
        {
            out.println("@subgroup {ions}");
            out.println("@spherelist {heavy} color= magenta radius= 0.5");
            bp.printBalls(atomC.ion);
        }
        if(atomC.watHeavy.size() > 0)
        {
            out.println("@subgroup {water}");
            out.println("@balllist {heavy} color= bluetint radius= 0.15");
            bp.printBalls(atomC.watHeavy);
            out.println("@vectorlist {H} color= gray master= {Hs}");
            sp.printSticks(bonds, atomC.watHydro, atomC.watHeavy);
        }
        
        /* Disulfides are different:
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
        printBonds(out, ssBonds,   "SS", "yellow");*/
        
        
        time = System.currentTimeMillis() - time;
        System.err.println("Drawing bonds:          "+time+" ms");
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

