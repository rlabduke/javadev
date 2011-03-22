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
* <code>BackboneMoveFinder</code> makes some measurements on a whole bunch of MODELs
* in hopes of finding places that move like a backrub, shear, peptide flip, or smear.
*
* This variant works on pairs of models, doing local superpositions and then
* calculating an RMSD (is there a stable "base" structure?) and an angle of
* rotation for the move type in question.
*
* It's based on Ian's <code>BackrubFinder2</code> class.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu Jan 13 2011
*/
public class BackboneMoveFinder //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("0.0##");
    static final String BACKRUB = "Backrubs";
    static final String SHEAR   = "Shears";
    static final String PEPFLIP = "Peptide flips";
    static final String SMEAR   = "Diagonal smears";
//}}}

//{{{ Variable definitions
//##############################################################################
    double  rmsdLimit            =  0.10; // Angstroms
    double  backrubAngleLimit    = 10.0;  // degrees
    double  shearAngleLimit      =  5.0;  // degrees
    double  shearDistanceLimit   =  0.30; // Angstroms (0.33A <- 5deg shear in ideal helix)
    String  moveType             = BACKRUB;
    boolean pairwise             = true;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BackboneMoveFinder()
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

//{{{ getCbetas
//##############################################################################
    AtomState[][] getCbetas(Collection models)
    {
        AtomState[][] cbetas = new AtomState[models.size()][0];
        int i = 0;
        for(Iterator mi = models.iterator(); mi.hasNext(); i++)
        {
            Model m = (Model) mi.next();
            ModelState state = m.getState();
            ArrayList cbs = new ArrayList();
            for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
            {
                Residue r = (Residue) ri.next();
                Atom a = r.getAtom(" CB ");
                if(a == null) a = r.getAtom("2HA ");
                if(a == null) continue;
                try { cbs.add( state.get(a) ); }
                catch(AtomException ex) { ex.printStackTrace(); }
            }
            cbetas[i] = (AtomState[]) cbs.toArray(cbetas[i]);
        }
        return cbetas;
    }
//}}}

//{{{ getOxygens
//##############################################################################
    AtomState[][] getOxygens(Collection models)
    {
        AtomState[][] oxygens = new AtomState[models.size()][0];
        int i = 0;
        for(Iterator mi = models.iterator(); mi.hasNext(); i++)
        {
            Model m = (Model) mi.next();
            ModelState state = m.getState();
            ArrayList os = new ArrayList();
            for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
            {
                Residue r = (Residue) ri.next();
                Atom a = r.getAtom(" O  ");
                if(a == null) continue;
                try { os.add( state.get(a) ); }
                catch(AtomException ex) { ex.printStackTrace(); }
            }
            oxygens[i] = (AtomState[]) os.toArray(oxygens[i]);
        }
        return oxygens;
    }
//}}}

//{{{ printBackrubPairs
//##############################################################################
    /**
    * Does pairwise analysis of backrubs for all models vs. all models at each window.
    */
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

//{{{ printShearPairs
//##############################################################################
    /**
    * Does pairwise analysis of shears for all models vs. all models at each window.
    */
    void printShearPairs(PrintStream out, AtomState[][] cas, AtomState[][] os)
    {
        //out.println("residue,ref_model,mobile_model,Ca_rmsd(1256),shear_angle(3243p),shear_angle(4354p),shear_angle_sum");
        out.println("residue,ref_model,mobile_model,Ca_rmsd(1256),shear_angle(O3Ca3Ca4O3p),O3_dist");
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
                    
                    /*
                    // Look at 3243' & 4354', i.e. dihedrals to direct neighbors
                    // Abs(sum) should be substantial if both shear angles are in same direction
                    Triple ca2 = new Triple().likeMidpoint(r[1], m[1]);
                    Triple ca3r = cas[i][k],   ca3m = new Triple(cas[j][k]);
                    T.transform(ca3m);
                    Triple ca4r = cas[i][k+1], ca4m = new Triple(cas[j][k+1]);
                    T.transform(ca4m);
                    Triple ca5 = new Triple().likeMidpoint(r[3], m[3]);
                    double shearAngle3243p = Triple.dihedral(ca3r, ca2, ca4r, ca3m);
                    double shearAngle4354p = Triple.dihedral(ca4r, ca3r, ca5, ca4m);
                    double shearAngleSum = shearAngle3243p + shearAngle4354p;
                    */
                    
                    Triple ca3r = cas[i][k];
                    Triple ca4r = cas[i][k+1];
                    Triple  o3r = os[i][k], o3m = new Triple(os[j][k]);
                    T.transform(o3m);
                    double shearAngle    = Triple.dihedral(o3r, ca3r, ca4r, o3m);
                    double shearDistance = Triple.distance(o3r, o3m);
                    
                    if(rmsd <= rmsdLimit && Math.abs(shearAngle) < shearAngleLimit && shearDistance > shearDistanceLimit)
                        out.println(cas[i][k].getResidue()+","+(i+1)+","+(j+1)+","+df.format(rmsd)+","+
                            df.format(shearAngle)+","+df.format(shearDistance));
                }
            }
        }
    }
//}}}

//{{{ printPepFlipPairs [not yet implemented]
//##############################################################################
    /**
    * Does pairwise analysis of peptide flips for all models vs. all models at each window.
    */
    void printPepFlipPairs(PrintStream out, AtomState[][] cas)
    {
        System.err.println("NOT YET IMPLEMENTED");
        System.exit(0);
    }
//}}}

//{{{ printSmearPairs [not yet implemented]
//##############################################################################
    /**
    * Does pairwise analysis of diagonal smears for all models vs. all models at each window.
    */
    void printSmearPairs(PrintStream out, AtomState[][] cas)
    {
        System.err.println("NOT YET IMPLEMENTED");
        System.exit(0);
    }
//}}}

//{{{ printBackrubStats
//##############################################################################
    /**
    * Does statistical analysis of backrubs across the set of models at each window.
    */
    void printBackrubStats(PrintStream out, AtomState[][] cas)
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

//{{{ printShearStats [not yet implemented]
//##############################################################################
    /**
    * Does statistical analysis of shears across the set of models at each window.
    */
    void printShearStats(PrintStream out, AtomState[][] cas, AtomState[][] os)
    {
        System.err.println("NOT YET IMPLEMENTED");
        System.exit(0);
    }
//}}}

//{{{ printPepFlipStats [not yet implemented]
//##############################################################################
    /**
    * Does statistical analysis of peptide flips across the set of models at each window.
    */
    void printPepFlipStats(PrintStream out, AtomState[][] cas)
    {
        System.err.println("NOT YET IMPLEMENTED");
        System.exit(0);
    }
//}}}

//{{{ printSmearStats [not yet implemented]
//##############################################################################
    /**
    * Does statistical analysis of diagonal smears across the set of models at each window.
    */
    void printSmearStats(PrintStream out, AtomState[][] cas)
    {
        System.err.println("NOT YET IMPLEMENTED");
        System.exit(0);
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
        System.err.println("Friendly reminder: Be sure you used \"command < file.pdb\" for input!");
        System.err.println("Mode: "+moveType+" "+(pairwise ? 
            "(all-against-all pairwise model comparisons)" : "(full-ensemble per-residue stats)"));
        
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
            AtomState[][] os = getOxygens(cfile.getModels());
            if(pairwise) printShearPairs(System.out, cas, os);
            else         printShearStats(System.out, cas, os);
        }
        else if(moveType == PEPFLIP)
        {
            if(pairwise) printPepFlipPairs(System.out, cas);
            else         printPepFlipStats(System.out, cas);
        }
        else if(moveType == SMEAR)
        {
            if(pairwise) printSmearPairs(System.out, cas);
            else         printSmearStats(System.out, cas);
        }
    }

    public static void main(String[] args)
    {
        BackboneMoveFinder mainprog = new BackboneMoveFinder();
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
            InputStream is = getClass().getResourceAsStream("BackboneMoveFinder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'BackboneMoveFinder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.BackboneMoveFinder");
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
        else if(flag.equals("-backrub") || flag.equals("-backrubs"))
        {
            moveType = BACKRUB;
        }
        else if(flag.equals("-shear") || flag.equals("-shears"))
        {
            moveType = SHEAR;
        }
        else if(flag.equals("-flip") || flag.equals("-flips"))
        {
            moveType = PEPFLIP;
        }
        else if(flag.equals("-smear") || flag.equals("-smears"))
        {
            moveType = SMEAR;
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
        else if(flag.equals("-shearangle"))
        {
            shearAngleLimit = Double.parseDouble(param);
        }
        else if(flag.equals("-sheardistance") || flag.equals("-sheardist"))
        {
            shearDistanceLimit = Double.parseDouble(param);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

