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
* <code>BackboneMoveFinder2</code> makes some measurements on a whole bunch of MODELs
* in hopes of finding places that move like a backrub or shear.
*
* It's based on Ian's <code>BackrubFinder2</code> class.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu Jan 13 2011
*/
public class BackboneMoveFinder2 //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("0.0##");
    static final String BACKRUB = "backrub";
    static final String SHEAR   = "shear";
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose           = false;
    double  rmsdLimit         =  0.05; // Angstroms
    double  backrubAngleLimit = 20.0; // degrees
    double  shearAngleLimit   =  4.0; // degrees
    double  shearDistLimit    =  0.3; // Angstroms
    /*double  shearProjLimit    =  0.0; // Angstroms*/
    String  moveType          = BACKRUB;
    boolean pairwise          = true;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BackboneMoveFinder2()
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

//{{{ printBackrubPairs
//##############################################################################
    void printBackrubPairs(PrintStream out, AtomState[][] cas)
    {
        out.println("residue,ref_model,mobile_model,Ca_rmsd(1245),backrub_angle");
        int numModels = cas.length; if(numModels == 0) return;
        
        for(int k = 2; k < cas[0].length - 2; k++)
        {
            for(int i = 0; i < numModels; i++)
            {
                for(int j = i+1; j < numModels; j++)
                {
                    // Reference, Mobile, Weights; don't use center Ca
                    Triple[] r = {new Triple(cas[i][k-2]), new Triple(cas[i][k-1]), new Triple(cas[i][k+1]), new Triple(cas[i][k+2])};
                    Triple[] m = {new Triple(cas[j][k-2]), new Triple(cas[j][k-1]), new Triple(cas[j][k+1]), new Triple(cas[j][k+2])};
                    SuperPoser p = new SuperPoser(r, m);
                    Transform T = p.superpos();
                    double rmsd = p.calcRMSD(T);
                    
                    for(int n = 0; n < m.length; n++)
                        T.transform(m[n]);
                    Triple ca2 = new Triple().likeMidpoint(r[1], m[1]);
                    Triple ca3r = cas[i][k], ca3m = new Triple(cas[j][k]);
                    T.transform(ca3m);
                    Triple ca4 = new Triple().likeMidpoint(r[2], m[2]);
                    double backrubAngle = Triple.dihedral(ca3r, ca2, ca4, ca3m);
                    
                    if(rmsd <= rmsdLimit && Math.abs(backrubAngle) >= backrubAngleLimit)
                        out.println(cas[i][k].getResidue()+","+(i+1)+","+(j+1)+","+df.format(rmsd)+","+df.format(backrubAngle));
                }
            }
        }//per window
    }
//}}}

//{{{ printBackrubStats
//##############################################################################
    void printBackrubStats(PrintStream out, AtomState[][] cas)
    {
        out.println("residue,dist24,dist24sd,theta124,theta124sd,theta245,theta245sd,phi1245,phi1245len,phi1245sd,"
            +"phi1243,phi1243len,phi1243sd,phi3245,phi3245len,phi3245sd");
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

//{{{ printShearPairs
//##############################################################################
    void printShearPairs(PrintStream out, AtomState[][] cas)
    {
        out.println("residue,ref_model,mobile_model,Ca_rmsd(1256),shear_angle,shear_dist");
        int numModels = cas.length; if(numModels == 0) return;
        
        for(int k = 2; k < cas[0].length - 3; k++) 
        {
            for(int i = 0; i < numModels; i++)
            {
                for(int j = i+1; j < numModels; j++)
                {
                    // Intuitive numbering:   1   2   3   4   5   6
                    // k-relative numbering:  k-2 k-1 k   k+1 k+2 k+3
                    // Don't use center 2 C-alphas:   ^   ^
                    Triple[] r = {new Triple(cas[i][k-2]), new Triple(cas[i][k-1]), new Triple(cas[i][k+2]), new Triple(cas[i][k+3])};
                    Triple[] m = {new Triple(cas[j][k-2]), new Triple(cas[j][k-1]), new Triple(cas[j][k+2]), new Triple(cas[j][k+3])};
                    SuperPoser p = new SuperPoser(r, m);
                    Transform T = p.superpos();
                    double rmsd = p.calcRMSD(T);
                    for(int n = 0; n < m.length; n++)
                        T.transform(m[n]);
                    
                    // Break down into the two in-plane rotations used in the KiNG shear tool
                    // (Ca3 rotates in Ca2-3-4 plane, Ca4 rotates in Ca3-4-5 plane); 
                    // report on the maximum deviation from planarity for those two dihedrals
                    Triple ca3r = cas[i][k], ca3m = new Triple(cas[j][k]);
                    Triple ca4r = cas[i][k+1], ca4m = new Triple(cas[j][k+1]);
                    T.transform(ca3m);
                    T.transform(ca4m);
                    Triple ca2 = new Triple().likeMidpoint(r[1], m[1]);
                    Triple ca3 = new Triple().likeMidpoint(ca3r, ca3m);
                    Triple ca4 = new Triple().likeMidpoint(ca4r, ca4m);
                    Triple ca5 = new Triple().likeMidpoint(r[2], m[2]);
                    double shearAngle3 = Triple.dihedral(ca3r, ca2, ca4, ca3m);
                    double shearAngle4 = Triple.dihedral(ca4r, ca3, ca5, ca4m);
                    double shearAngle = shearAngle3;
                    if(Math.abs(shearAngle4) > Math.abs(shearAngle3))
                        shearAngle = shearAngle4;
                    
                    // Also make sure both central Ca's actually moved significantly
                    double shearDist3 = Triple.distance(ca3r, ca3m);
                    double shearDist4 = Triple.distance(ca4r, ca4m);
                    double shearDist = Math.min(shearDist3, shearDist4);
                    
                    if(rmsd <= rmsdLimit && Math.abs(shearAngle) < shearAngleLimit && shearDist > shearDistLimit)
                        out.println(cas[i][k].getResidue()+","+(i+1)+","+(j+1)+","+df.format(rmsd)
                            +","+df.format(shearAngle)+","+df.format(shearDist));
                    
                    /* Doesn't work: picks up large but non-parallel motions, e.g. at hairpins
                    // Project 3->3' + 4->4' onto 3---->4
                    Triple ca3r = cas[i][k], ca3m = new Triple(cas[j][k]);
                    Triple ca4r = cas[i][k+1], ca4m = new Triple(cas[j][k+1]);
                    T.transform(ca3m);
                    T.transform(ca4m);
                    Triple ca3 = new Triple().likeVector(ca3r, ca3m);
                    Triple ca4 = new Triple().likeVector(ca4r, ca4m);
                    Triple ca34 = new Triple().likeVector(ca3r, ca4r);
                    double shearProj = (ca3.add(ca4)).dot(ca34) / ca34.mag();
                    
                    if(rmsd <= rmsdLimit && shearProj > shearProjLimit)
                        out.println(cas[i][k].getResidue()+","+(i+1)+","+(j+1)+","+df.format(rmsd)+","+df.format(shearProj));*/
                }
            }
        }
    }
//}}}

//{{{ printShearStats
//##############################################################################
    void printShearStats(PrintStream out, AtomState[][] cas)
    {
        out.println("residue,dist25,dist25sd,dist16,dist16sd,theta126,theta126sd,theta256,theta256sd,phi1256,phi1256len,phi1256sd,"
            +"dist24,dist24sd,dist35,dist35sd,theta234,theta234sd,theta345,theta345sd,phi3254,phi3254len,phi3254sd");
        int numModels   = cas.length;       if(numModels == 0)  return;
        int numRes      = cas[0].length;    if(numRes == 0)     return;
        
        // Geometry that should have low variation
        double[] dist25     = new double[numModels];
        double[] dist16     = new double[numModels];
        double[] theta126   = new double[numModels];
        double[] theta256   = new double[numModels];
        double[] phi1256    = new double[numModels];
        
        // Geometry that should have higher variation
        double[] dist24     = new double[numModels];
        double[] dist35     = new double[numModels];
        double[] theta234   = new double[numModels];
        double[] theta345   = new double[numModels];
        double[] phi3254    = new double[numModels]; // ???
        
        for(int i = 2; i < numRes - 3; i++)
        {
            for(int j = 0; j < numModels; j++)
            {
                AtomState c1 = cas[j][i-2];
                AtomState c2 = cas[j][i-1];
                AtomState c3 = cas[j][i  ];
                AtomState c4 = cas[j][i+1];
                AtomState c5 = cas[j][i+2];
                AtomState c6 = cas[j][i+3];
                
                dist25[j] = c2.distance(c5);
                dist16[j] = c1.distance(c6);
                theta126[j] = Triple.angle(c1, c2, c6);
                theta256[j] = Triple.angle(c2, c5, c6);
                phi1256[j] = Triple.dihedral(c1, c2, c5, c6);
                
                dist24[j] = c2.distance(c4);
                dist35[j] = c3.distance(c5);
                theta234[j] = Triple.angle(c2, c3, c4);
                theta345[j] = Triple.angle(c3, c4, c5);
                phi3254[j] = Triple.dihedral(c3, c2, c5, c4);
            }
            
            out.print(cas[0][i].getResidue());
            
            out.print(","+df.format(mean(dist25))+","+df.format(stddev(dist25)));
            out.print(","+df.format(mean(dist16))+","+df.format(stddev(dist16)));
            out.print(","+df.format(mean(theta126))+","+df.format(stddev(theta126)));
            out.print(","+df.format(mean(theta256))+","+df.format(stddev(theta256)));
            out.print(","+df.format(circ_mean(phi1256))+","+df.format(circ_len(phi1256))+","+df.format(circ_stddev(phi1256)));
            
            out.print(","+df.format(mean(dist24))+","+df.format(stddev(dist24)));
            out.print(","+df.format(mean(dist35))+","+df.format(stddev(dist35)));
            out.print(","+df.format(mean(theta234))+","+df.format(stddev(theta234)));
            out.print(","+df.format(mean(theta345))+","+df.format(stddev(theta345)));
            out.print(","+df.format(circ_mean(phi3254))+","+df.format(circ_len(phi3254))+","+df.format(circ_stddev(phi3254)));
            
            out.println();
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
        if(verbose) System.err.println("Mode: "+moveType+" "+(pairwise ? "pairs" : "stats"));
        
        PdbReader pdbReader = new PdbReader();
        CoordinateFile cfile = pdbReader.read(System.in);
        
        AtomState[][] cas = getCalphas(cfile.getModels());
        
        if(moveType == BACKRUB)
        {
            if(pairwise) printBackrubPairs(System.out, cas);
            else         printBackrubStats(System.out, cas);
        }
        else if(moveType == SHEAR)
        {
            if(pairwise) printShearPairs(System.out, cas);
            else         printShearStats(System.out, cas);
        }
    }

    public static void main(String[] args)
    {
        BackboneMoveFinder2 mainprog = new BackboneMoveFinder2();
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
            InputStream is = getClass().getResourceAsStream("BackboneMoveFinder2.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'BackboneMoveFinder2.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.BackboneMoveFinder2");
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
    }
    
    void interpretFlag(String flag, String param)
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
        else if(flag.equals("-backrub") || flag.equals("-backrubs"))
        {
            moveType = BACKRUB;
        }
        else if(flag.equals("-shear") || flag.equals("-shears"))
        {
            moveType = SHEAR;
        }
        else if(flag.equals("-pairs"))
        {
            pairwise = true;
        }
        else if(flag.equals("-stats"))
        {
            pairwise = false;
        }
        else if(flag.equals("-rmsd"))
        {
            rmsdLimit = Double.parseDouble(param);
        }
        else if(flag.equals("-backrubangle"))
        {
            backrubAngleLimit = Double.parseDouble(param);
        }
        /*else if(flag.equals("-shearproj"))
        {
            shearProjLimit = Double.parseDouble(param);
        }*/
        else if(flag.equals("-shearangle"))
        {
            shearAngleLimit = Double.parseDouble(param);
        }
        else if(flag.equals("-sheardistance") || flag.equals("-sheardist"))
        {
            shearDistLimit = Double.parseDouble(param);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

