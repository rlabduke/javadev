// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;

import java.io.*;
import java.net.URL;
import java.text.*;
import java.util.*;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
//}}}
/**
* <code>RotamerPCA</code> performs principal component analysis on observed
* sidechain conformations within rotamer clusters to find correlated modes
* of motion.
*
* Could be used for more principled (pun intended) rotamer sampling in 
* dihedral space.
*
* With some changes, could be extended to rotamer sampling in Cartesian space,
* or other Cartesian ensemble analysis.
*/
public class RotamerPCA //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("##0.000");
//}}}

//{{{ Variable definitions
//##############################################################################
    
    // INPUT
    
    /** Chi values of raw empirical observations w/o rotamers */
    File  rawFile;
    
    /** Chi values of hills grid points w/ rotamers.  
    *   Used to assign raw points to rotamers. */
    File  hillsFile;
    
    /** Chi values of hill modes (i.e. peaks) w/ rotamers */
    File  peaksFile;
    
    
    // OPTIONS
    
    boolean  verbose  = false;
    
    /** Boundaries for each dimension (min1, max1, min2, max2, ..) */
    int[]  wrapBounds;
    
    /** Number of chis (dimensions), not including any clusterID at the end */
    int  nChis  = -1;
    
    /** Points farther than this distance from the closest hills grid point 
    *   will NOT be included in further analysis for that rotamer.  The idea
    *   is to avoid problems associated with nearby minor clusters that aren't
    *   quite considered their own hill and therefore get "drawn in" to another 
    *   hill, thereby skewing analysis at that hill. */
    double  eventHorizon  = 30;
    
    // INTERNAL
    
    /** Chi values of raw data, pointing to rotamers.  Map:double[]->int */
    HashMap<double[],Integer>  data;
    
    /** Chi values of hills grid points, pointing to rotamers.  Map:double[]->int */
    HashMap<double[],Integer>  hills;
    
    /** Rotamers, pointing to chi values of hill modes.  Map:int->double[] */
    HashMap<Integer,double[]>  peaks;
    
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotamerPCA()
    {
        super();
    }
//}}}

//{{{ loadData
//##############################################################################
    /**
    * Loads hills grid points text & raw data points text files.
    * Assigns each raw data point to a hills rotamer.
    * Based off code from chiropraxis.sc.RotamerSampler.Main().
    */
    public void loadData() throws IOException
    {
        // HILLS -- the colored points from hills kins (+ rotamers)
        LineNumberReader hillsIn = new LineNumberReader(new FileReader(hillsFile));
        hills = new HashMap<double[],Integer>();
        String s;
        while((s = hillsIn.readLine()) != null)
        {
            if(s.startsWith("#")) continue;
            // format: "50.0 80.0 1.0"  <- last column is rotamer
            String[] parts = Strings.explode(s,' ');
            if(parts.length == 1) parts = Strings.explode(s,',');
            if(parts.length == 1) parts = Strings.explode(s,':');
            if(nChis < 0)
                nChis = parts.length-1;
            else if(parts.length-1 != nChis)
                throw new IllegalArgumentException("Data fields are of different lengths");
            double[] vals = new double[nChis]; // does *not* include rotamer
            for(int i = 0; i < nChis; i++)
                vals[i] = Double.parseDouble(parts[i]);
            int rotamer = (int)Double.parseDouble(parts[parts.length-1]);
            hills.put(vals, rotamer);
        }
        hillsIn.close();
        // sanity check on wrap dimensions (1st place we can do this)
        if(wrapBounds != null && wrapBounds.length != 2*nChis)
        {
            System.err.println("Wrap bounds given for "+wrapBounds.length/2+
                " dims -- doesn't match "+nChis+" chis in data! .. ignoring wrap bounds!");
            wrapBounds = null;
        }
                
        // RAW DATA -- empirical observations
        LineNumberReader rawIn = new LineNumberReader(new FileReader(rawFile));
        data = new HashMap<double[],Integer>();
        while((s = rawIn.readLine()) != null)
        {
            if(s.startsWith("resname")) continue;
            // format: "16pk A 14 LEU,-50.176,169.212,NULL,NULL,-120.669,-15.612,13.300,LEU"
            String[] parts = Strings.explode(s,',');
            if(parts.length == 1) parts = Strings.explode(s,':');
            if(parts[nChis].equals("NULL"))
            {
                System.err.println("Not enough chis in "+rawFile+" for line '"+s+"' .. skipping");
                continue;
            }
            double[] vals = new double[nChis]; // does *not* include rotamer
            for(int i = 0; i < nChis; i++)
                vals[i] = Double.parseDouble(parts[i+1]);
            int rotamer = nearestRotamer(vals, hills);
            if(rotamer != Integer.MAX_VALUE) // i.e. not beyond "event horizon"
                data.put(vals, rotamer);
        }
        rawIn.close();
        
        // PEAKS -- from Silk hillmodes
        LineNumberReader peaksIn = new LineNumberReader(new FileReader(peaksFile));
        peaks = new HashMap<Integer, double[]>();
        while((s = peaksIn.readLine()) != null)
        {
            if(s.startsWith("#")) continue;
            // format: "1,65.0,85.0,0.04130813357918459"  <- first column is rotamer
            String[] parts = Strings.explode(s,' ');
            if(parts.length == 1) parts = Strings.explode(s,',');
            if(parts.length == 1) parts = Strings.explode(s,':');
            if(parts.length-2 != nChis)
                throw new IllegalArgumentException("Data fields are of different lengths");
            double[] vals = new double[nChis]; // does *not* include rotamer
            for(int i = 0; i < nChis; i++)
                vals[i] = Double.parseDouble(parts[i+1]);
            int rotamer = (int)Double.parseDouble(parts[0]);
            peaks.put(rotamer, vals);
        }
        peaksIn.close();
    }
//}}}

//{{{ nearestRotamer
//##############################################################################
    /**
    * Assigns a rotamer to the given raw empirical observation 
    * based on the hills grid point to which it is closest.
    */
    public int nearestRotamer(double[] vals, HashMap<double[],Integer> hills) throws IOException
    {
        //System.err.println("finding closest hills point to "+Strings.arrayInParens(vals));
        
        double[] closest = null;
        double minDist = Double.POSITIVE_INFINITY;
        for(Iterator iter = hills.keySet().iterator(); iter.hasNext(); )
        {
            double[] other = (double[]) iter.next();
            if(verbose && other.length != vals.length) System.err.println(
                "ERROR: non-matching # chis in nearestCluster()!!");
            
            double[] dists = new double[other.length]; // one dist per chi; best wrap vs. no wrap
            for(int j = 0; j < other.length; j++)
                dists[j] = Math.abs(displacement(vals[j], other[j], j));
            
            double sqrd = 0;
            for(int d = 0; d < dists.length; d++) sqrd += Math.pow(dists[d], 2);
            double dist = Math.sqrt(sqrd);
            if(dist < minDist)
            {
                closest = other;
                minDist = dist;
            }
        }
        
        //System.err.println("hills point closest to "+Strings.arrayInParens(vals)
        //    +" is "+Strings.arrayInParens(closest)+" .. dist: "+df.format(minDist));
        
        if(minDist > eventHorizon)
        {
            if(verbose) System.err.println("Can't assign rotamer for "+Strings.arrayInParens(vals)
                +" b/c "+df.format(minDist)+" > "+eventHorizon+" degrees from nearest hills grid point");
            return Integer.MAX_VALUE;
        }
        
        int rotamer = (Integer) hills.get(closest);
        return rotamer;
    }
//}}}

//{{{ displacement
//##############################################################################
    /**
    * Returns signed (could be + or -) displacement with smallest absolute 
    * value from val to ref in just one dimension (i.e. one chi). 
    * Handles wrapping if -wrap=.. flag provided by user (0-360 if not)
    * @param val  chi value of empirical observation point in question
    * @param ref  chi value of reference point, e.g. peak or hills point
    * @param j    index of dimension of val & ref (typically chi-1)
    */
    public double displacement(double val, double ref, int j)
    {
        // Set wrapping if neither provided nor already defined
        if(wrapBounds == null)
        {
            wrapBounds = new int[2*nChis];
            for(int i = 0; i < nChis; i++)
            {
                wrapBounds[2*i]   =   0;
                wrapBounds[2*i+1] = 360;
            }
            if(verbose)
            {
                String s = "Using default -wrapbounds=";
                for(int i = 0; i < nChis; i++) s += wrapBounds[2*i]+","+wrapBounds[2*i+1]+",";
                System.err.println(s.substring(0,s.length()-1));
            }
        }
        
        double min = wrapBounds[2*j];
        double max = wrapBounds[2*j+1];
        double range = max - min;
        
        double valRange = val % range;
        if(valRange < min) valRange += range; // to fall within e.g. 0-360
        
        double valBelow = valRange - range; // e.g. 2 -> -358
        double valAbove = valRange + range; // e.g. 2 ->  362
        
        double disRange = valRange - ref; // e.g.    2 - 180 = -178  <- winner b/c abs(-178) lowest
        double disBelow = valBelow - ref; // e.g. -358 - 180 = -538
        double disAbove = valRange - ref; // e.g.  362 - 180 =  182
        
        double dis = disRange;
        if(Math.abs(disBelow) < Math.abs(dis)) dis = disBelow;
        if(Math.abs(disAbove) < Math.abs(dis)) dis = disAbove;
        
        //System.err.println(val+" is "+(dis>0?"+":"")+df.format(dis)+" away from "+ref+" in chi"+(j+1));
        return dis;
    }
//}}}

//{{{ doPca
//##############################################################################
    /**
    * Finds principal components for each rotamer.
    */
    public void doPca()
    {
        TreeSet rotamers = new TreeSet<Integer>();
        for(Iterator iter = data.keySet().iterator(); iter.hasNext(); )
        {
            double[] vals = (double[])iter.next();
            int rotamer = (Integer)data.get(vals);
            rotamers.add(rotamer);
        }
        
        for(Iterator rItr = rotamers.iterator(); rItr.hasNext(); )
        {
            int rotamer = (Integer)rItr.next();
            
            double[][] xArray = buildX(rotamer);
            //Matrix x = new Matrix(xArray);
            //System.err.print("Input X matrix:");
            //x.print(new PrintWriter(System.err, true), new DecimalFormat("#.####"), 1);
            
            averageDisplacements(rotamer, xArray);
            
            //// Do SVD:   X = USV^T   (S = sigma, V^T = V_transpose)
            //SingularValueDecomposition svd = new SingularValueDecomposition(x);
            //Matrix u = svd.getU(); // columns = principal componenents
            //Matrix s = svd.getS(); // diagonal elements = singular values (weights)
            //System.err.print("SVD output U Matrix:");
            //u.print(new PrintWriter(System.err, true), new DecimalFormat("#.####"), 1);
            //System.err.print("SVD output S matrix:");
            //s.print(new PrintWriter(System.err, true), new DecimalFormat("#.####"), 1);
            //
            //System.err.println("MORE CODE HERE!!!");
        }
    }
//}}}

//{{{ buildX
//##############################################################################
    /**
    * Portrays one rotamer's data points in the guise of an N x M (rows x columns) 2-D array
    */
    public double[][] buildX(int rotamer) 
    {
        //if(verbose) System.err.println("Building X matrix for rotamer "+rotamer);
        
        // Get all data points assigned to this rotamer
        ArrayList<double[]> inRot = new ArrayList<double[]>();
        for(Iterator dItr = data.keySet().iterator(); dItr.hasNext(); )
        {
            double[] vals = (double[])dItr.next();
            int r = (Integer)data.get(vals);
            if(r == rotamer)  inRot.add(vals);
        }
        
        int m = inRot.size(); // data points assigned to this rotamer
        int n = nChis;        // dimensions i.e. chis
        
        if(verbose) System.err.print("Rotamer "+rotamer+": "+inRot.size()+" points");
        
        // Build X array
        double[][] xArray = new double[n][m];
        for(int i = 0; i < m; i++)
        {
            double[] vals = inRot.get(i);
            for(int j = 0; j < n; j++)
                xArray[j][i] = vals[j]; // j ~ n = rows = chis;  i ~ m = cols = points
        }
        
        // Subtract peak (determined by Silk hill-climbing a priori)
        double[] peak = peaks.get(rotamer);
        for(int i = 0; i < m; i++)
            for(int j = 0; j < n; j++)
                xArray[j][i] = displacement(xArray[j][i], peak[j], j); // *not* abs val - wanna know directionality
        
        if(verbose) System.err.println(", peak: "+Strings.arrayInParens(peak));
        
        return xArray;
    }
//}}}

//{{{ averageDisplacements
//##############################################################################
    /** Returns <code>nChis</code> displacement vectors for the specified rotamer */
    public void averageDisplacements(int rotamer, double[][] x)
    {
        double[] peak = peaks.get(rotamer);
        
        // Which way do data samples in aggregate "point"?
        ArrayList<double[]> dirVects = new ArrayList<double[]>();
        double[] prevDirVect = null;
        for(int d = 0; d < nChis; d++)
        {
            double[] dirVect = new double[nChis];
            for(int i = 0; i < nChis; i++) dirVect[i] = 0;
            
            int samples = x[0].length;
            int used = 0;
            for(int j = 0; j < samples; j++)
            {
                double[] sample = new double[nChis];
                for(int i = 0; i < nChis; i++) sample[i] = x[i][j];
                
                if(prevDirVect == null)
                {
                    for(int i = 0; i < nChis; i++)  dirVect[i] += sample[i]; // sum up
                    used++;
                }
                else
                {
                    // ignore samples contributing strongly to the previous average displacement
                    // so the next one will be in a significantly different direction
                    if(!aligned(sample, prevDirVect))
                    {
                        for(int i = 0; i < nChis; i++)  dirVect[i] += sample[i]; // sum up
                        used++;
                    }
                }
            }
            double frac = (1.0*used) / (1.0*samples);
            System.err.println("Using "+used+"/"+samples+" = "+frac+" samples");
            for(int i = 0; i < nChis; i++) dirVect[i] = dirVect[i] / used; // take avg
            dirVects.add(dirVect);
            prevDirVect = dirVect;
            
            System.out.println("@group {rot"+rotamer+" chi"+(d+1)+" "+frac+"} dominant animate");
            System.out.println("@balllist {peak} radius= 2.0 color= pink master= {peak}");
            System.out.println(Strings.kinPt(peak, false, "peak"));
            
            double[] tip = new double[nChis];
            for(int i = 0; i < nChis; i++) tip[i] = peak[i] + dirVect[i];
            System.out.println("@arrowlist {dir vect} width= 3 color= hotpink master= {dir vect}");
            System.out.println(Strings.kinPt(peak, true , "peak"     ));
            System.out.println(Strings.kinPt(tip , false, "direction"));
        }
    }
//}}}

//{{{ aligned
//##############################################################################
    /** ?????? */
    public boolean aligned(double[] a, double[] b)
    {
        // ???
        //final double dpCutoff = Math.cos(0.52); // ~30 deg - mark as aligned FEWER points
        final double dpCutoff = Math.cos(0.78); // ~45 deg
        //final double dpCutoff = Math.cos(1.05); // ~60 deg - mark as aligned MORE points
        // ???
        
        // Normalize to unit vectors
        double aMag = 0;
        double bMag = 0;
        for(int i = 0; i < nChis; i++) aMag += a[i]*a[i];
        for(int i = 0; i < nChis; i++) bMag += b[i]*b[i];
        aMag = Math.sqrt(aMag);
        bMag = Math.sqrt(bMag);
        if(aMag != 0.0) for(int i = 0; i < nChis; i++) a[i] /= aMag;
        if(bMag != 0.0) for(int i = 0; i < nChis; i++) b[i] /= bMag;
        
        // Dot product
        double dp = 0;
        for(int i = 0; i < nChis; i++) dp += a[i] * b[i];
        if(dp > dpCutoff) return false;
        return true;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(rawFile == null || hillsFile == null || peaksFile == null)
        {
            showHelp(true);
            return;
        }
        try
        {
            loadData();
            doPca();
        }
        catch (IOException ioe)
        { System.err.println("*** Can't find 1 or more of the input files!"); }
        System.err.println(".. done");
    }

    public static void main(String[] args)
    {
        RotamerPCA mainprog = new RotamerPCA();
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
            System.err.println();
            System.err.println("*** Error in execution: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("RotamerPCA.help");
            if(is == null)
                System.err.println("\n*** Usage: java RotamerPCA  srcdata.csv  hills.spc  peaks.spc ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.RotamerPCA");
        System.err.println("Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.");
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
        if     (rawFile   == null)  rawFile   = new File(arg);
        else if(hillsFile == null)  hillsFile = new File(arg);
        else if(peaksFile == null)  peaksFile = new File(arg);
        else throw new IllegalArgumentException("Too many file names: "+arg);
    }
    
    void interpretFlag(String flag, String param)
    {
        try
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
            else if(flag.equals("-wrap") || flag.equals("-wrapbounds"))
            {
                String[] parts = Strings.explode(param,',');
                if(parts.length % 2 != 0)
                {
                    System.err.println("Odd # of -wrap parameters; should be even!");
                    return;
                }
                if(parts.length > 8)
                {
                    System.err.println("Too many -wrap parameters; up to 8 allowed!");
                    return;
                }
                wrapBounds = new int[parts.length];
                try
                {
                    for(int i = 0; i < parts.length; i++)
                        wrapBounds[i] = Integer.parseInt(parts[i]);
                }
                catch (NumberFormatException nfe) 
                { System.err.println("Can't parse -wrap="+param+" as integers!"); }
            }
            else if(flag.equals("-dummy_option"))
            {
                // handle option here
            }
            else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
        }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Non-number argument to "+flag+": '"+param+"'"); }
    }
//}}}
}//class

