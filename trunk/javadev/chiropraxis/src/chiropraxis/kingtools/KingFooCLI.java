// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;

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
import driftwood.util.*;
//}}}
/**
* <code>KingFooCLI</code> is the command-line interface to KingFoo.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Apr 25 09:34:51 EDT 2005
*/
public class KingFooCLI //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    InputStream pdbInput        = null;
    boolean     removeHets      = true;
    Triple      fooOrigen       = null;
    double      fooRadius       = 1.0;
    double      gridSpacing     = 0.4;
    int         steps           = 10;
    int         freeSteps       = 3;
    boolean     removeWetFoos   = true;
    double      wetFooRadius    = 3.0;
    boolean     makeDotSurface  = true;
    double      dotDensity      = 16.0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KingFooCLI()
    {
        super();
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
        Model m = new PdbReader().read(pdbInput).getFirstModel();
        Collection atoms = new LinkedList(m.getState().createCollapsed().getLocalStateMap().values());
        if(removeHets) for(Iterator iter = atoms.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(as.isHet()) iter.remove();
        }
        
        long time = System.currentTimeMillis();
        KingFoo kf = new KingFoo(atoms, fooRadius, fooRadius/1.0);
        kf.placeFoosFCC(fooOrigen, gridSpacing, steps, freeSteps);
        time = System.currentTimeMillis() - time;
        System.err.println(kf.getFoos().size()+" foos were placed successfully in "+time+" ms");
        
        if(removeWetFoos)
        {
            time = System.currentTimeMillis();
            kf.removeWetFoos(wetFooRadius);
            time = System.currentTimeMillis() - time;
            System.err.println(kf.getFoos().size()+" dry foos remaining after "+time+" ms");
        }
        
        DecimalFormat df = new DecimalFormat("0.0###");
        //System.out.println("@kinemage 1");
        System.out.println("@group {foo cavities}");
        System.out.println("@subgroup {foo cavities}");
        System.out.println("@balllist {foo balls} radius= "+df.format(fooRadius)+" color= pink off nohighlight alpha= 1.0");
        for(Iterator iter = kf.getFoos().iterator(); iter.hasNext(); )
            System.out.println("{x} "+((Triple)iter.next()).format(df));
        
        if(makeDotSurface)
        {
            time = System.currentTimeMillis();
            Collection dotSurface = kf.surfaceFoos(dotDensity);
            time = System.currentTimeMillis() - time;
            System.err.println(dotSurface.size()+" dots placed in "+time+" ms");
            
            System.out.println("@dotlist {foo dots} color= gray width= 1");
            for(Iterator iter = dotSurface.iterator(); iter.hasNext(); )
                System.out.println("{x} "+((Triple)iter.next()).format(df));
        }
    }

    public static void main(String[] args)
    {
        KingFooCLI mainprog = new KingFooCLI();
        try
        {
            mainprog.parseArguments(args);
            if(mainprog.fooOrigen == null)
                throw new IllegalArgumentException("Must specify -origen for foo.");
            if(mainprog.pdbInput == null)
                mainprog.pdbInput = System.in;
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
            InputStream is = getClass().getResourceAsStream("KingFooCLI.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'KingFooCLI.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.kingtools.KingFooCLI");
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
        if(pdbInput == null)
        {
            try { pdbInput = new BufferedInputStream(new FileInputStream(arg)); }
            catch(IOException ex) { throw new IllegalArgumentException("IOEx: "+ex.getMessage()); }
        }
        else
            throw new IllegalArgumentException("Extra argument: "+arg);
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-origen"))
        {
            String[] sXYZ = param.split("\\s*[, ]\\s*");
            try { fooOrigen = new Triple(Double.parseDouble(sXYZ[0]), Double.parseDouble(sXYZ[1]), Double.parseDouble(sXYZ[2])); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException("Correct usage: -ORIGEN=#.#,#.#,#.#"); }
        }
        else if(flag.equals("-steps"))
        {
            try { steps = Integer.parseInt(param); }
            catch(NumberFormatException ex) { throw new IllegalArgumentException("Correct usage: -STEPS=#"); }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

