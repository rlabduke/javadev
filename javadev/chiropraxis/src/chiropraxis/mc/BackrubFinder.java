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
* <code>BackrubFinder</code> makes some measurements on a whole bunch of MODELs
* in hopes of finding places that move in Backrub-like ways.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Dec  5 14:27:25 EST 2005
*/
public class BackrubFinder //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("0.0##");
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BackrubFinder()
    {
        super();
    }
//}}}

//{{{ getCalphas
//##############################################################################
    AtomState[][] getCalphas(Collection models)
    {
        AtomState[][] calphas = new AtomState[models.size()][0];
        int i = 0;
        for(Iterator mi = models.iterator(); mi.hasNext(); i++)
        {
            Model m = (Model) mi.next();
            ModelState state = m.getState();
            ArrayList cas = new ArrayList();
            for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
            {
                Residue r = (Residue) ri.next();
                Atom a = r.getAtom(" CA ");
                if(a == null) continue;
                try { cas.add( state.get(a) ); }
                catch(AtomException ex) { ex.printStackTrace(); }
            }
            calphas[i] = (AtomState[]) cas.toArray(calphas[i]);
        }
        return calphas;
    }
//}}}

//{{{ printStats
//##############################################################################
    void printStats(PrintStream out, AtomState[][] cas)
    {
        //out.println("Residue,dist13,sd,dist24,sd,dist35,sd,dist14,sd,dist25,sd,dist15,sd,theta124,sd,theta245,sd,phi1245,len,sd");
        out.println("Residue,dist24,sd,theta124,sd,theta245,sd,phi1245,len,sd,phi1243,len,sd,phi3245,len,sd");
        int numModels   = cas.length;       if(numModels == 0)  return;
        int numRes      = cas[0].length;    if(numRes == 0)     return;
        
        double[] dist13     = new double[numModels];
        double[] dist24     = new double[numModels];
        double[] dist35     = new double[numModels];
        double[] dist14     = new double[numModels];
        double[] dist25     = new double[numModels];
        double[] dist15     = new double[numModels];
        double[] theta124   = new double[numModels];
        double[] theta245   = new double[numModels];
        double[] phi1245    = new double[numModels];
        double[] phi1243    = new double[numModels];
        double[] phi3245    = new double[numModels];
        for(int i = 2; i < numRes - 2; i++)
        {
            for(int j = 0; j < numModels; j++)
            {
                AtomState c1 = cas[j][i-2];
                AtomState c2 = cas[j][i-1];
                AtomState c3 = cas[j][i  ];
                AtomState c4 = cas[j][i+1];
                AtomState c5 = cas[j][i+2];
                dist13[j] = c1.distance(c3);
                dist24[j] = c2.distance(c4);
                dist35[j] = c3.distance(c5);
                dist14[j] = c1.distance(c4);
                dist25[j] = c2.distance(c5);
                dist15[j] = c1.distance(c5);
                theta124[j] = Triple.angle(c1, c2, c4);
                theta245[j] = Triple.angle(c2, c4, c5);
                phi1245[j] = Triple.dihedral(c1, c2, c4, c5);
                phi1243[j] = Triple.dihedral(c1, c2, c4, c3);
                phi3245[j] = Triple.dihedral(c3, c2, c4, c5);
            }
            out.print("\""+cas[0][i].getResidue());
            //out.print("\",\""+df.format(mean(dist13))+"\",\""+df.format(stddev(dist13)));
            out.print("\",\""+df.format(mean(dist24))+"\",\""+df.format(stddev(dist24)));
            //out.print("\",\""+df.format(mean(dist35))+"\",\""+df.format(stddev(dist35)));
            //out.print("\",\""+df.format(mean(dist14))+"\",\""+df.format(stddev(dist14)));
            //out.print("\",\""+df.format(mean(dist25))+"\",\""+df.format(stddev(dist25)));
            //out.print("\",\""+df.format(mean(dist15))+"\",\""+df.format(stddev(dist15)));
            out.print("\",\""+df.format(mean(theta124))+"\",\""+df.format(stddev(theta124)));
            out.print("\",\""+df.format(mean(theta245))+"\",\""+df.format(stddev(theta245)));
            out.print("\",\""+df.format(circ_mean(phi1245))+"\",\""+df.format(circ_len(phi1245))+"\",\""+df.format(circ_stddev(phi1245)));
            out.print("\",\""+df.format(circ_mean(phi1243))+"\",\""+df.format(circ_len(phi1243))+"\",\""+df.format(circ_stddev(phi1243)));
            out.print("\",\""+df.format(circ_mean(phi3245))+"\",\""+df.format(circ_len(phi3245))+"\",\""+df.format(circ_stddev(phi3245)));
            out.println("\"");
        }
    }
//}}}

//{{{ mean, stddev
//##############################################################################
    double mean(double[] x)
    {
        double sum = 0;
        for(int i = 0; i < x.length; i++) sum += x[i];
        return sum / x.length;
    }

    double stddev(double[] x)
    {
        double mean = mean(x);
        double sum2 = 0;
        for(int i = 0; i < x.length; i++)
        {
            double dev = mean - x[i];
            sum2 += dev*dev;
        }
        return Math.sqrt(sum2 / x.length);
    }
//}}}

//{{{ circ_mean, circ_len, circ_stddev
//##############################################################################
    /** Given an array of angles (in degrees), computes the ANGLE of the vector average (in degrees). */
    double circ_mean(double[] t)
    {
        double sx = 0, sy = 0;
        for(int i = 0; i < t.length; i++)
        {
            sx += Math.cos( Math.toRadians(t[i]) );
            sy += Math.sin( Math.toRadians(t[i]) );
        }
        return Math.toDegrees( Math.atan2(sy/t.length, sx/t.length) );
    }

    /** Given an array of angles (in degrees), computes the LENGTH of the vector average (0.0 - 1.0). */
    double circ_len(double[] t)
    {
        double sx = 0, sy = 0;
        for(int i = 0; i < t.length; i++)
        {
            sx += Math.cos( Math.toRadians(t[i]) );
            sy += Math.sin( Math.toRadians(t[i]) );
        }
        sx /= t.length;
        sy /= t.length;
        return Math.sqrt(sx*sx + sy*sy);
    }

    /** Given an array of angles (in degrees), computes the angular standard deviation (in degrees). */
    double circ_stddev(double[] t)
    {
        double mean = circ_mean(t);
        double a, sa = 0;
        for(int i = 0; i < t.length; i++)
        {
            a = Math.abs(mean - t[i]) % 360.0;
            if(a > 180.0) a = 360.0 - a;
            sa += a*a;
        }
        return Math.sqrt(sa / t.length);
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
        PdbReader pdbReader = new PdbReader();
        CoordinateFile cfile = pdbReader.read(System.in);
        AtomState[][] cas = getCalphas(cfile.getModels());
        printStats(System.out, cas);
    }

    public static void main(String[] args)
    {
        BackrubFinder mainprog = new BackrubFinder();
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
            InputStream is = getClass().getResourceAsStream("BackrubFinder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'BackrubFinder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.BackrubFinder");
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
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

