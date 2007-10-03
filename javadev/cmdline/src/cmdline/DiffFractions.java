// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports

import java.io.*;
import java.util.*;
//}}}
/**
* Used <code>SilkCmdLine</code> as template...
* 
* DAK 10/1/2007
*/
public class DiffFractions //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    File preCootFile;
    File postCootFile;
    //File outFile;
    HashMap<double[], Double> preCootMap;
    HashMap<double[], Double> postCootMap;
    HashMap<double[], Double> diffMap;
    int numDims;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public DiffFractions()
    {
        preCootMap  = new HashMap<double[], Double>();
        postCootMap = new HashMap<double[], Double>();
        diffMap     = new HashMap<double[], Double>();
        numDims     = 3;
    }
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main(String[] args)
    {
        // Set up files from args
        preCootFile  = new File(args[0]);
        postCootFile = new File(args[1]);
        if (args[2].indexOf("-ndim=") >= 0)
            numDims = Integer.parseInt(args[2].substring(6));
        
        System.out.println("# numDims = "+numDims);
        
        // Set up ArrayLists of pre- and post-Coot, process data, and print output
        readData("pre");
        readData("post");
        getDiffs();
        doOutput();
    }

    public static void main(String[] args)
    {
        DiffFractions df = new DiffFractions();
        df.Main(args);
    }
//}}}

//{{{ readData
//##################################################################################################
    void readData(String preOrPost)
    {
        File inFile   = preCootFile;
        HashMap inMap = preCootMap;
        if (preOrPost.equals("post"))
        {
            inFile = postCootFile;
            inMap  = postCootMap;
        }
        
        try
        {
            Scanner s = new Scanner(inFile);
            while (s.hasNextLine())
            {
                // Set defaults
                double[] thisPointsArray = new double[numDims];
                for (int i = 0; i < numDims; i ++)
                    thisPointsArray[i] = 999;
                double frac = 999;
                
                // Scan and get data
                String line = s.nextLine();
                if (line.indexOf("#") < 0)
                {
                    Scanner lineScanner = new Scanner(line);
                    for (int i = 0; i < numDims; i ++)
                        if (lineScanner.hasNext())
                            thisPointsArray[i] = lineScanner.nextDouble();
                    if (lineScanner.hasNext())
                        frac = lineScanner.nextDouble();
                    
                    boolean gotEverything = true;
                    if (frac == 999)    gotEverything = false;
                    for (int i = 0; i < numDims; i ++)
                        if (thisPointsArray[i] == 999)  gotEverything = false;
                    if (gotEverything)
                        inMap.put(thisPointsArray, frac);
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.println(preOrPost + " file not found...");
        }
    }
//}}}

//{{{ getDiffs
//##################################################################################################
    void getDiffs()
    {
        // Get diff in data fraction for each point in 3D space
        // Have to go through both hashes (pre- and post-) b/c some residues are in some 
        // but not the other.  In  those cases, still subtract post- from pre-Coot (e.g. 
        // there before but not afterwards --> positive contour peak in output)
        // This will replicate some entries in the diffMap but they should be identical 
        // both times, so not a problem -- just inefficient
        
        Iterator iter = (preCootMap.keySet()).iterator();
        while (iter.hasNext())
        {
            double[] thisKey = (double[]) iter.next();
            double preCootFrac  = preCootMap.get(thisKey);
            double postCootFrac = 0;
            if (postCootMap.containsKey(thisKey))
                postCootFrac = postCootMap.get(thisKey);
            double diffFrac = preCootFrac - postCootFrac;
            diffMap.put(thisKey, diffFrac);
        }
        
        iter = (postCootMap.keySet()).iterator();
        while (iter.hasNext())
        {
            double[] thisKey = (double[]) iter.next();
            double postCootFrac = postCootMap.get(thisKey);
            double preCootFrac  = 0;
            if (preCootMap.containsKey(thisKey))
                preCootFrac  = preCootMap.get(thisKey);
            double diffFrac = preCootFrac - postCootFrac;
            diffMap.put(thisKey, diffFrac);
        }
    }
//}}}

//{{{ doOutput
//##################################################################################################
    void doOutput()
    {
        Iterator iter = (diffMap.keySet()).iterator();
        while (iter.hasNext())
        {
            double[] thisKey = (double[]) iter.next();
            double diffFrac  = diffMap.get(thisKey);
            for (int i = 0; i < numDims; i ++)
                System.out.print(thisKey[i]+":");
            System.out.print(diffMap.get(thisKey));    // should be frac from readData(___)
            System.out.println();
        }
    }
//}}}
}//class

