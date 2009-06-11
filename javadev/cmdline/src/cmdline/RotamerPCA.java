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
    DecimalFormat df = new DecimalFormat("#.##");
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean  verbose       = false;
    
    /** Boundaries for each dimension (min1, max1, min2, max2, ..) */
    int[]    wrapBounds    = null;
    /** Number of chis (dimensions), not including any clusterID at the end */
    int      nChis         = -1;
    
    /** Chi values of raw empirical observations w/o cluster IDs */
    File     rawFile       = null;
    HashMap  rawData       = null;
    /** Chi values of hills grid points w/ cluster IDs. Used for assigning 
    cluster IDs to raw emprical observations */
    File     hillsFile     = null;
    HashMap  hillsData     = null;
    
    /** One per rotamer cluster; raw chi values */
    HashMap  dataMatrices  = null;
    /** One per rotamer cluster; chi diffs from cluster peak. Each diff  
    * matrix is the equivalent of the A matrix of SVD's A = USV^T */
    HashMap  diffMatrices  = null;
    
    /** ??????? */
    HashMap  avgPrincComps = null;
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
    /** Loads hills grid points text & raw data points text files. Assigns each
    *   raw data point to a cluster ID from the set of hills clusters. */
    public void loadData() throws IOException
    {
        // Based off code from chiropraxis.sc.RotamerSampler.Main()
        
        // (1) Hills data points (i.e. the colored points from hills kins)
        //     Also yields set of cluster IDs
        LineNumberReader hillsIn = new LineNumberReader(new FileReader(hillsFile));
        hillsData = new HashMap<double[], Integer>();
        String s;
        while((s = hillsIn.readLine()) != null)
        {
            if(s.startsWith("#")) continue;
            
            // format: "50.0 80.0 1.0" - last column is hills cluster ID
            String[] parts = Strings.explode(s,' ');
            if(nChis < 0) nChis = parts.length-1;
            else if(parts.length-1 != nChis) throw new IllegalArgumentException("Data fields are of different lengths");
            double[] vals = new double[nChis]; // does *not* include clusterID
            for(int i = 0; i < nChis; i++) vals[i] = Double.parseDouble(parts[i]);
            
            int clusterID = (int) Double.parseDouble(parts[parts.length-1]);
            hillsData.put(vals, clusterID);
        }
        hillsIn.close();
        
        // Sanity check on wrap dimensions (1st place we can do this)
        if(wrapBounds != null && wrapBounds.length != 2*nChis)
        {
            System.err.println("wrap bounds given for "+wrapBounds.length/2+" dims; "+
                "doesn't match "+nChis+" chis in data!");
            System.err.println(".. ignoring wrap bounds!");
            wrapBounds = null;
        }
                
        // (2) Raw data points (i.e. empirical observations),
        LineNumberReader rawIn = new LineNumberReader(new FileReader(rawFile));
        rawData = new HashMap<double[], Integer>();
        while((s = rawIn.readLine()) != null)
        {
            if(s.startsWith("resname")) continue;
            
            // format: "16pk A 14 LEU:-50.176:169.212:NULL:NULL:-120.669:-15.612:13.300:LEU"
            String[] parts = Strings.explode(s,':');
            if(parts[nChis].equals("NULL"))
            {
                System.err.println("not enough chis in "+rawFile+" for line '"+s+"' .. skipping");
                continue;
            }
            double[] vals = new double[nChis];
            for(int i = 0; i < nChis; i++) vals[i] = Double.parseDouble(parts[i+1]);
            
            //System.err.println("read data point: "+str(vals));
            
            int clusterID = nearestCluster(vals);
            if(!(clusterID < 0)) rawData.put(vals, clusterID);
            else if(verbose) System.err.print(
                "can't assign cluster for raw data point "+str(vals)+" .. skipping");
        }
        rawIn.close();
    }
//}}}

//{{{ nearestCluster
//##############################################################################
    /** Assigns a cluster ID to the given raw empirical observation based on 
    *   the hills grid point to which it is closest. */
    public int nearestCluster(double[] vals) throws IOException
    {
        //System.err.println("finding closest hills point to "+str(vals));
        
        double[] closest = null;
        double minDist = Double.POSITIVE_INFINITY;
        for(Iterator iter = hillsData.keySet().iterator(); iter.hasNext(); )
        {
            double[] other = (double[]) iter.next();
            if(verbose) if(other.length != vals.length) System.err.println(
                "ERROR: non-matching # chis in nearestCluster()!!");
            
            double[] dists = new double[other.length]; // one dist per chi; best wrap vs. no wrap
            for(int j = 0; j < other.length; j++)
                dists[j] = Math.abs(distWithWrap(vals[j], other[j], j));
            
            double sqrd = 0;
            for(int d = 0; d < dists.length; d++) sqrd += Math.pow(dists[d], 2);
            double dist = Math.sqrt(sqrd);
            if(dist < minDist)
            {
                closest = other;
                minDist = dist;
            }
        }
        
        //System.err.println("hills point closest to "+str(vals)+" is "+str(closest)
        //    +" .. dist: "+df.format(minDist));
        
        int id = (Integer) hillsData.get(closest); // clusterID
        return id;
    }
//}}}

//{{{ fillMatrices
//##############################################################################
    /** Maps each hills cluster ID to a matrix containing all its raw data 
    *   points. Note: mapping from ID -> data, not data -> ID. */
    public void fillMatrices() throws IOException
    {
        dataMatrices = new HashMap<Integer, Matrix>();
        diffMatrices = new HashMap<Integer, Matrix>();
        
        TreeSet clusterIDs = new TreeSet<Integer>();
        for(Iterator iter = rawData.keySet().iterator(); iter.hasNext(); )
        {
            double[] vals = (double[]) iter.next();
            int clusterID = (Integer) rawData.get(vals);
            clusterIDs.add(clusterID);
        }
        
        for(Iterator iter = clusterIDs.iterator(); iter.hasNext(); )
        {
            int clusterID = (Integer) iter.next();
            
            // Get all raw data points in this cluster
            ArrayList dataInCluster = new ArrayList<double[]>();
            for(Iterator rdi = rawData.keySet().iterator(); rdi.hasNext(); )
            {
                double[] vals = (double[]) rdi.next();
                int clusterNum = (Integer) rawData.get(vals);
                if(clusterNum == clusterID) dataInCluster.add(vals); // in right cluster
            }
            
            // Make a 2D array for this cluster.
            // Must be row = sample, col = chi rather than row = chi, col = sample
            // because for SVD to work, m (# rows) must be >= n (# cols), and for 
            // rotamer distributions there should always be more samples than chis.
            System.err.println("# chis     ="+nChis);
            System.err.println("# data pts ="+ dataInCluster.size());
            //double[][] dataArray = new double[nChis][dataInCluster.size()];
            double[][] dataArray = new double[dataInCluster.size()][nChis];
            for(int i = 0; i < dataInCluster.size(); i++) // row
            {
                double[] vals = (double[]) dataInCluster.get(i);
                for(int j = 0; j < vals.length; j++) // col
                    //dataArray[j][i] = vals[j];
                    dataArray[i][j] = vals[j];
            }
            
            // Substract cluster peak
            double[][] diffArray = subtractPeak(dataArray, clusterID);
            
            // Convert those 2D arrays to matrices
            Matrix dataMatrix = new Matrix(dataArray);
            Matrix diffMatrix = new Matrix(diffArray);
            dataMatrices.put(clusterID, dataMatrix);
            diffMatrices.put(clusterID, diffMatrix);
            
            //System.err.print("cluster "+clusterID+" data matrix:");
            //dataMatrix.print(new DecimalFormat("#.#"), 1);
            //System.err.print("cluster "+clusterID+" diff matrix:");
            //diffMatrix.print(new DecimalFormat("#.#"), 1);
            
        }//for each cluster
    }
//}}}

//{{{ subtractPeak
//##############################################################################
    /** Subtracts the cluster peak (from hillmodes) from raw data points in 
    * that cluster. It's up to the caller to provide data in d that matches
    * the integer clusterID of the peak to be subtracted in id! */
    public double[][] subtractPeak(double[][] d, int id)
    {
        // Get peak
        double[] peak = null;
        for(Iterator iter = hillsData.keySet().iterator(); iter.hasNext(); )
        {
            double[] somePeak = (double[]) iter.next();
            int someClusterID = (Integer) hillsData.get(somePeak);
            if(someClusterID == id)  peak = somePeak;
        }
        if(peak == null)
        {
            System.err.println("Can't find peak values for cluster ID "+id+"!");
            return null;
        }
        if(verbose) System.err.println("cluster "+id+" data: "
            +d.length+" rows, "+d[0].length+" cols, peak "+str(peak));
        
        // Subtract it from all raw data points in this cluster
        double[][] ret = new double[d.length][d[0].length];
        for(int i = 0; i < d.length; i++)
        {
            for(int j = 0; j < d[i].length; j++)
                //ret[i][j] = distWithWrap(d[i][j], peak[i], i);
                ret[i][j] = distWithWrap(d[i][j], peak[j], j); // *not* abs val - wanna know directionality
            
            //System.err.println("point "+str(d[i])+" is dist "+str(ret[i])
            //    +" from peak "+str(peak)+(wrapBounds != null ? " w/ wrap" : ""));
        }
        
        return ret;
    }
//}}}

//{{{ doSVD
//##############################################################################
    /** From reduce() method found here:
    * http://sujitpal.blogspot.com/2008/10/ir-math-in-java-cluster-visualization.html */
    public void doSVD()
    {
        avgPrincComps = new HashMap<Integer,double[]>();
        
        for(Iterator iter = diffMatrices.keySet().iterator(); iter.hasNext(); )
        {
            int clusterID = (Integer) iter.next();
            Matrix diffMatrix = (Matrix) diffMatrices.get(clusterID);
            SingularValueDecomposition svd = new SingularValueDecomposition(diffMatrix);
            Matrix u = svd.getU(); // columns = principal componenents
            Matrix s = svd.getS();
            Matrix v = svd.getV();
            
            System.err.print("SVD U matrix for cluster "+clusterID+":");
            u.print(new PrintWriter(System.err,true), new DecimalFormat("#.##"), 1);
            System.err.print("SVD S matrix for cluster "+clusterID+":");
            s.print(new PrintWriter(System.err,true), new DecimalFormat("#.##"), 1);
            System.err.print("SVD V matrix for cluster "+clusterID+":");
            v.print(new PrintWriter(System.err,true), new DecimalFormat("#.##"), 1);
            
            // HONESTLY, DON'T UNDERSTAND PCA WELL ENOUGH TO KNOW WHAT THIS IS TRYING TO DO!
            /*
            // We know that the diagonal of S is ordered, so we can take the
            // first 3 cols from V, for use in plot2d and plot3d
            Matrix vRed = v.getMatrix(0, v.getRowDimension() - 1, 0, 2);
            for (int i = 0; i < v.getRowDimension(); i++)
            {
                System.err.println("cluster"+clusterID+" PC "+i+1+":  "+
                    Math.abs(vRed.get(i, 0))+" "+Math.abs(vRed.get(i, 1)));
            }
            */
            
            
            //WRONG!!!
            double[][] uArray = u.getArray();
            double[] avgPC = new double[uArray[0].length]; // #chis
            for(int j = 0; j < avgPC.length; j++)  avgPC[j] = 0.0;
            for(int i = 0; i < uArray.length; i++)  for(int j = 0; j < uArray[i].length; j++)
                avgPC[j] += uArray[i][j];
            for(int j = 0; j < avgPC.length; j++)  avgPC[j] /= uArray.length; // to get avg
            avgPrincComps.put(clusterID, avgPC);
            System.err.println("cluster "+clusterID+" avg U: "+str(avgPC));
            //WRONG!!!
        }
    }
//}}}

//{{{ doKin
//##############################################################################
    /** Prints data @dotlist and principal component @vectorlist for each cluster */
    public void doKin()
    {
        String[] cols = new String[] {
            "red", "orange", "gold", "yellow", "lime", "green", "sea", "cyan", 
            "sky", "blue", "purple", "magenta", "hotpink", "pink", "peach", "lilac", 
            "pinktint", "peachtint", "yellowtint", "greentint", "bluetint", 
            "lilactint", "white", "gray", "brown", "deadwhite", "deadblack" };
        
        System.out.println("@master {data}");
        System.out.println("@master {prncpl cmpnts}");
        
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for(Iterator iter = diffMatrices.keySet().iterator(); iter.hasNext(); )
            ids.add( (Integer) iter.next() );
        Collections.sort(ids);
        
        for(int x = 0; x < ids.size(); x++)
        {
            int id = ids.get(x);
            String col = cols[id-1 % cols.length];
            System.out.println("@group {cluster "+id+"} animate collapsable");
            
            double[][] data = ( (Matrix)dataMatrices.get(id) ).getArray();
            double[][] diff = ( (Matrix)diffMatrices.get(id) ).getArray();
            
            System.out.println("@dotlist {data} color= "+col+" alpha= 0.5 width= 2 master= {data}");
            for(int i = 0; i < data.length; i++)
            {
                // one data point from this cluster
                System.out.println("{point "+str(data[i])+"} "+str2(data[i]));
            }
            
            System.out.println("@arrowlist {-> nearest peak} color= "+col
                +" alpha= 0.2 radius= 2.0 master= {-> nearest peak}");
            double[] peak = null;
            for(int i = 0; i < diff.length; i++)
            {
                if(peak == null)
                {
                    peak = new double[diff[i].length];
                    for(int j = 0; j < diff[i].length; j++)  peak[j] = data[i][j] - diff[i][j];
                }
                System.out.println("{point "+str(data[i])+" pc??}P "+str2(peak));
                System.out.println("{\"} "+str2(data[i]));
            }
            
            
            
            //WRONG!!!
            System.out.println("@arrowlist {PC??} color= "+col
                +" alpha= 1.0 width= 3 radius= 2.0 master= {PCs}");
            double[] avgPC = (double[]) avgPrincComps.get(id);
            double[] peakPlusAvgPC = new double[avgPC.length];
            for(int i = 0; i < peakPlusAvgPC.length; i++)
                peakPlusAvgPC[i] = peak[i] + 2000*avgPC[i];
            System.out.println("{cluster "+id+" PC??}P "+str2(peak));
            System.out.println("{\"} "+str2(peakPlusAvgPC));
            //WRONG!!!
            
            
            
        }//per rotamer cluster
    }
//}}}

//{{{ distWithWrap, str[2]
//##############################################################################
    /** Returns signed (could be + or -) distance with smallest absolute value 
    * from val to ref in just one dimension (i.e. one chi). 
    * Handles wrapping if -wrap=.. flag provided by user
    * @param val chi value of empirical observation point in question
    * @param ref chi value of reference point, e.g. peak or hills point
    * @param j   the index of the dimension of val & ref (typically chi+1)
    */
    public double distWithWrap(double val, double ref, int j)
    {
        double d0 = val - ref; // 1 - 359 = -358  (just OK)
        
        double dw = Double.POSITIVE_INFINITY;
        if(wrapBounds != null)
        {
            double val_min = val + (wrapBounds[j+1] - wrapBounds[j]); // 1 ->  361
            double dw_min  = val_min - ref;                        // useful:  361 - 359 =    2
            
            double val_max = val - (wrapBounds[j+1] - wrapBounds[j]); // 1 -> -361
            double dw_max  = val_max - ref;                   // not so much: -361 - 359 = -720
            
            dw = (Math.abs(dw_min) < Math.abs(dw_max) ? dw_min : dw_max);
        }
        
        double d = (Math.abs(d0) < Math.abs(dw) ? d0 : dw);
        return d;
    }
    
    /** Makes a comma+space-delimited, intra-parentheses string 
    * out of an array of unknown dimension */
    public String str(double[] a)
    {
        String s = "(";
        for(int i = 0; i < a.length-1; i++)  s += df.format(a[i]) + ", ";
        s += df.format(a[a.length-1])+")";
        return s;
    }
    
    /** Makes a space-delimited, parentheses-free string 
    * out of an array of unknown dimension */
    public String str2(double[] a)
    {
        String s = "";
        for(int i = 0; i < a.length-1; i++)  s += df.format(a[i]) + " ";
        s += df.format(a[a.length-1]);
        return s;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(rawFile == null || hillsFile == null)
        {
            System.err.println("*** Need both (1) raw srcdata .csv and (2) .hillmodes as input!");
            System.err.println("*** Quitting...");
            System.exit(0);
        }
        
        try
        {
            loadData();
            fillMatrices();
            doSVD();
            doKin();
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
                System.err.println("\n*** Usage: java RotamerPCA srcdata.cdf whichhill.snglspc ***\n");
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
        if(rawFile == null)        rawFile   = new File(arg);
        else if(hillsFile == null) hillsFile = new File(arg);
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
                { for(int i = 0; i < parts.length; i++) wrapBounds[i] = Integer.parseInt(parts[i]); }
                catch (NumberFormatException nfe) 
                { System.err.println("Can't parse -wrap="+param+" as integers!"); }
            }
            else if(flag.equals("-verbose") || flag.equals("-v"))
            {
                verbose = true;
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

