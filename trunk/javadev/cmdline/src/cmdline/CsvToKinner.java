// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package/Imports
//##################################################################################################
package cmdline;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import driftwood.r3.*;
import driftwood.util.Strings;
//}}}

/**
* <code>CsvToKinner</code> takes in a delimited text file and outputs
* either a kinemage that plots the values in the n columns specified by 
* the user in 3D space or writes those n columns back out in csv format.
* It can also concatenate two csv files row-by-row, yielding 2*n columns.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
*/
public class CsvToKinner
{ 

//{{{ Constants
//##################################################################################################
    final String AA_NAMES_NO_ALA_GLY = "arg lys met glu gln asp asn ile leu his trp tyr phe pro thr val ser cys";
      // no "ala" or "gly"
//}}}

//{{{ Variable Definitions
//##################################################################################################
    int                 numDims;
    String              csvFilename;
    File                csv;
    String              csv2filename;  // for row-by-row concatenation of designated
    File                csv2;          //   columns from two different files
    int[]               cols;          // column ids/idxs to look for in csv for n-D data
    int[]               ptidcols;      // column ids/idxs to look for in csv for pointids
    String[]            labels;        // one for each column 
    ArrayList<double[]> pts;           // size() is # rows in csv file
    ArrayList<String>   ptids;         // size() is # rows in csv file
    double[]            maxes;         // for axis placement purposes
    double[]            mins;          // for axis placement purposes
    double[]            avgs;
    String              delimiter;     // ":", ",", etc.
    boolean             kinHeading;
    boolean[]           wrap360;
    boolean[]           wrap180;
    boolean             scaleZ;
    int                 scalingFactorZ = Integer.MAX_VALUE;
    boolean             noKin;
    ArrayList<String[]> noKinPts;
    boolean             noFrame;
    String              groupName;
    ArrayList<String>   rotaBalls;
    boolean             altFrame;
    String              color;
    ArrayList<String>   masters;
    boolean             verbose;
    
//}}}

//{{{ Constructor
//##################################################################################################
	public CsvToKinner()
	{
        // Initialize defaults
        numDims         = Integer.MAX_VALUE;
        csv             = null;
        csvFilename     = null;
        csv2            = null;
        csv2filename    = null;
        ptidcols        = null;
        ptids           = null;
        labels          = null;
        wrap360         = null;
        wrap180         = null;
        delimiter       = ":";
        kinHeading      = false;
        scaleZ          = false;
        scalingFactorZ  = 10;
        noKin           = false;
        noFrame         = false;
        groupName       = "";
        rotaBalls       = null;
        altFrame        = false;
        color           = "white";
        masters         = null;
        verbose         = false;
    }
//}}}

//{{{ doChecks
//##################################################################################################
	void doChecks()
    {
        if (verbose) System.err.println("doing checks...");
        
        if (numDims == Integer.MAX_VALUE)
        {
            System.err.println("Need to provide -cols=#[,#,#,...]!");
            System.exit(0);
        }
        
        if (csv2 != null && csv2filename != null)
        {
            if (!noKin)
            {
                System.err.println("Setting -nokin option b/c doing row-by-row "
                    +"concatenation of two files");
                noKin = true;
            }
            return; // no need to do other checks
        }
        
        // Labels array
        if (!noKin)
        {
            if (labels != null) // provided by user
            {
                // Make sure there are enough labels for the # columns requested
                if (labels.length < cols.length)
                {
                    System.err.println("Not enough labels for the # columns requested!"
                        +" ... setting extra(s) to ???");
                    String[] newLabels = new String[cols.length];
                    for (int i = 0; i < cols.length; i ++)
                    {
                        if (i < labels.length)  newLabels[i] = labels[i];
                        else                    newLabels[i] = "???";
                    }
                    labels = newLabels;
                }
            }
            else // default labels: 'X', 'Y', 'Z', ...
            {
                labels = new String[numDims];
                char xChar = 'X';
                for (int i = 0; i < numDims; i ++)
                    labels[i] = "" + (int) xChar + i;
            }
        }
        
        // Wrap360/180 arrays
        if (wrap360 != null) // provided by user
        {
            if (wrap360.length < numDims)
            {
                System.err.println("Not enough wrap360 designations for the # columns requested!"
                    +" ... setting extra "+(numDims-wrap360.length)+" to false");
                boolean[] newWrap360 = new boolean[numDims];
                for (int i = 0; i < wrap360.length; i++)        newWrap360[i] = wrap360[i];
                for (int i = wrap360.length; i < numDims; i++)  newWrap360[i] = false;
                wrap360 = newWrap360;
            }
            // else if (wrap360.length >= numDims) that's fine
        }
        else // default: all false
        {
            wrap360 = new boolean[numDims];
            for (int i = 0; i < numDims; i ++)
                wrap360[i] = false;
            wrap180 = new boolean[numDims];
            for (int i = 0; i < numDims; i ++)
                wrap180[i] = false;
        }
        if (wrap180 != null) // provided by user
        {
            if (wrap180.length < numDims)
            {
                System.err.println("Not enough wrap180 designations for the # columns requested!"
                    +" ... setting extra "+(numDims-wrap180.length)+" to false");
                boolean[] newWrap180 = new boolean[numDims];
                for (int i = 0; i < wrap180.length; i++)        newWrap180[i] = wrap180[i];
                for (int i = wrap180.length; i < numDims; i++)  newWrap180[i] = false;
                wrap180 = newWrap180;
            }
            // else if (wrap180.length >= numDims) that's fine
        }
        else // default: all false
        {
            wrap180 = new boolean[numDims];
            for (int i = 0; i < numDims; i ++)
                wrap180[i] = false;
            wrap180 = new boolean[numDims];
            for (int i = 0; i < numDims; i ++)
                wrap180[i] = false;
        }
        
        // Make sure there is a file provided
        if (csv == null)
        {
            System.err.println("Didn't specify an input csv file!");
            System.exit(0);
        }
        
        // Make sure user didn't try to do -scalez with numDims != 3
        // (scaleZ option assumes & requires x,y,z coordinate system)
        if (scaleZ && numDims != 3)
        {
            System.err.println("Can't do -scalez if # coordinates is not 3!");
            System.err.println("Disregarding -scalez ...");
            scaleZ = false;
        }
    }
//}}}


//{{{ readInput
//##################################################################################################
	private void readInput()
	{
		if (verbose) System.err.println("reading input (for kin)...");
        
        // Mission: Fill points with n-dimensional arrays from cols[0], cols[1], ...
        // Goal   : Make kin
        try
        {
            pts = new ArrayList<double[]>();
            for (double[] array : pts)
                array = new double[cols.length];
            Scanner fileScan = new Scanner(csv);
            boolean firstLine = true;
            int count = 0;
            while (fileScan.hasNextLine())
            {
                String line = fileScan.nextLine();
                String currToken = "a desired column";
                int    currCol   = Integer.MAX_VALUE;
                try
                {
                    if (ptidcols != null)
                    {
                        // Get data for pointid from this line
                        String ptid = "";
                        for (int p = 0; p < ptidcols.length; p ++)
                        {
                            // Skip to jth column in this particular line
                            int ptidcol = ptidcols[p];
                            Scanner lineScan = new Scanner(line);
                            lineScan.useDelimiter(delimiter);
                            for (int j = 0; j < ptidcol; j++)
                                lineScan.next();
                            ptid += " "+lineScan.next();
                        }
                        // Put this multi-D point's pointid into our ptids ArrayList
                        ptids.add(ptid);
                    }
                    
                    // Make a multi-D "point" for this line
                    double[] thisPoint = new double[cols.length];
                    for (int c = 0; c < cols.length; c ++)
                    {
                        // Skip to jth column in this particular line
                        int col = cols[c];
                        Scanner lineScan = new Scanner(line);
                        lineScan.useDelimiter(delimiter);
                        for (int j = 0; j < col; j++)
                            lineScan.next();
                        currToken = lineScan.next();
                        currCol   = c;
                        thisPoint[c] = Double.parseDouble(currToken);
                    }
                    // Put this multi-D "point" into our ArrayList
                    pts.add(thisPoint);
                }
                catch (java.util.NoSuchElementException nsee)
                {
                    System.err.println("Couldn't find indicated columns ...try using different delimiter instead of "+delimiter+" ?\n");
                    System.exit(0);
                }
                catch (NumberFormatException nfe)
                {
                    if (count == 0)
                        System.err.println("Couldn't parse \""+currToken+"\" in column "+cols[currCol]+" as double in first line, so skipping:\n  \""+line+"\"");
                    else
                        System.err.println("Couldn't parse \""+currToken+"\" in column "+cols[currCol]+" as double");
                }
                count ++;
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read through csv input file...");   
        }
	}
//}}}

//{{{ getMaxes, getMins
//##################################################################################################
	private void getMaxes()
	{
        // Find xMax, yMax, and zMax
        double[] maxesArray = new double[numDims];
        for (int xi = 0; xi < numDims; xi ++)
        {
            double xiMax = Double.NEGATIVE_INFINITY;
            for (double[] pt : pts)
            {
                if (pt[xi] > xiMax)    xiMax = pt[xi];
            }
            
            // Make sure the axes (which these are used for) will match the wrapping of the data
            if (wrap360[xi]) //&& xi < 2)
            {
                xiMax = wrap360(xiMax);
            }
            if (wrap180[xi]) //&& xi < 2)
            {
                xiMax = wrap180(xiMax);
            }
            
            maxesArray[xi] = xiMax;
        }
        
        maxes = maxesArray;
    }

    private void getMins()
	{
        // Find xMin, yMin, and zMin
        double[] minsArray = new double[numDims];
        for (int xi = 0; xi < numDims; xi ++)
        {
            double xiMin = Double.POSITIVE_INFINITY;
            for (double[] pt : pts)
            {
                if (pt[xi] < xiMin)    xiMin = pt[xi];
            }
            
            // Make sure the axes (which these are used for) will match the wrapping of the data
            if (wrap360[xi]) //&& xi < 2)
            {
                xiMin = wrap360(xiMin);
            }
            if (wrap180[xi]) //&& xi < 2)
            {
                xiMin = wrap180(xiMin);
            }
            
            minsArray[xi] = xiMin;
        }
        
        mins = minsArray;
    }
//}}}

//{{{ getAvgs
//##################################################################################################
	private void getAvgs()
	{
        double[] avgsArray = new double[numDims];
        for (int xi = 0; xi < numDims; xi ++)
        {
            double sum = 0;
            int count = 0;
            for (int i = 0; i < pts.size(); i ++)
            {
                double[] pt = pts.get(i);
                sum += pt[xi];   // was += pt[2]; ... ???
                count ++;
            }
            
            double avg = sum / count;
            avgsArray[xi] = avg;
        }
        
        avgs = avgsArray;
    }
//}}}

//{{{ wrap360, wrap180
//##################################################################################################
	private double wrap360(double angle)
    {
        //if (verbose) System.err.println("wrapping "+angle+" to 0->360");
        angle = angle % 360;
        if(angle < 0) return angle + 360;
        else return angle;
    }

    private double wrap180(double angle)
    {
        // Anything over 180 gets 360 subtracted from it
        // e.g. 210 --> 210 - 360 --> -150
        //if (verbose) System.err.println("wrapping "+angle+" to 0->180");
        if(angle > 180) return angle - 360;
        else return angle;
    }
//}}}

//{{{ scale, inverseScale
//##################################################################################################
	private double scale(double value, int xi)
    {
        double valueScaled = value + (value - avgs[xi]) * scalingFactorZ;
        return valueScaled;
    }
    
    private double inverseScale(double scaledTarget, int xi)
    {
        // Tells you what non-scaled # would give a certain scaled #
        // If you enter this return value into scale method, you'll get scaledTarget
        return (scaledTarget+(avgs[xi]*scalingFactorZ)) / (1+scalingFactorZ);
    }
//}}}

//{{{ getAndPrintDesiredColumns
//##################################################################################################
	private void getAndPrintDesiredColumns()
	{
        // Not doing a kin, just spitting certain columns of data back out
        if (verbose)
        {
            System.err.print("col idxs: ");
            for (int i = 0; i < cols.length-1; i++) System.err.print(cols[i]+",");
            System.err.println(cols[cols.length-1]);
        }
        
        // Get data from input csv
        try
        {
            for (String[] noKinArray : noKinPts)
                noKinArray = new String[cols.length];
            Scanner fileScan = new Scanner(csv);
            boolean firstLine = true;
            while (fileScan.hasNextLine())
            {
                String line = fileScan.nextLine();
                if (!firstLine)
                {
                    // Make a multi-D set of Strings for this line
                    String[] thisNoKinPoint = new String[cols.length];
                    try
                    {
                        for (int c = 0; c < cols.length; c ++)
                        {
                            // Skip to ith column
                            Scanner lineScan_c = new Scanner(line);
                            lineScan_c.useDelimiter(delimiter);
                            for (int j = 0; j < cols[c]; j++)
                                lineScan_c.next();
                            thisNoKinPoint[c] = lineScan_c.next();
                        }
                    }
                    catch (java.util.NoSuchElementException nsee)
                    {
                        System.err.println("Couldn't find indicated columns...");
                        System.err.println("Try using different delimiter instead of "+delimiter+" ?");
                        System.err.println();
                        System.exit(0);
                    }
                    
                    // Put this multi-D set of Strings into our ArrayList
                    noKinPts.add(thisNoKinPoint);
                }
                else if (verbose) System.err.println("skipping first line: \""+line+"\"");
                firstLine = false;
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Can't read through csv input file...");   
        }
        
        // Print data to stdout
        for (String[] noKinPt : noKinPts) // each row in csv
        {
            for (int c = 0; c < cols.length-1; c ++)
                System.out.print(noKinPt[c]+delimiter);
            System.out.println(noKinPt[cols.length-1]);
        }
        
        // We're done!
        System.exit(0);
    }
//}}}

//{{{ concatFilesByRow
//##################################################################################################
	private void concatFilesByRow()
	{
        if (verbose)
        {
            System.err.print("col idxs: ");
            for (int i = 0; i < cols.length-1; i++) System.err.print(cols[i]+",");
            System.err.println(cols[cols.length-1]);
        }
        
        char[] delim = delimiter.toCharArray();
        try
        {
            LineNumberReader lnr1 = new LineNumberReader(new FileReader(csv ));
            LineNumberReader lnr2 = new LineNumberReader(new FileReader(csv2));
            String s1, s2;
            while((s1 = lnr1.readLine()) != null && (s2 = lnr2.readLine()) != null)
            {
                try
                {
                    String[] parts1 = Strings.explode(s1, delim[0]);
                    String[] parts2 = Strings.explode(s2, delim[0]);
                    if (parts1.length == 1) System.err.println(
                        "Can't find '"+delim[0]+"' in line of csv #1 ... try different delimiter?");
                    if (parts2.length == 1) System.err.println(
                        "Can't find '"+delim[0]+"' in line of csv #2 ... try different delimiter?");
                    
                    String data = "";
                    for (int c = 0; c < cols.length; c++)  data += parts1[cols[c]]+delimiter;
                    for (int c = 0; c < cols.length; c++)  data += parts2[cols[c]]+delimiter;
                    data = data.substring(0,data.length()-1);
                    
                    System.out.println(data);
                }
                catch(IndexOutOfBoundsException ex)
                { System.err.println("Error reading from csv file #1 or #2, line "+lnr1.getLineNumber()); }
                catch(NumberFormatException ex)
                { System.err.println("Error reading from csv file #2 or #2, line "+lnr1.getLineNumber()); }
            }
        }
        catch(FileNotFoundException fnfe)
        { System.err.println("Cannot find either csv file #1 or #2"); }
        catch(IOException ioe)
        { System.err.println("I/O error reading from either csv file #1 or #2"); }
        
        // We're done!
        System.exit(0);
    }
//}}}


//{{{ printOutput
//##################################################################################################
	private void printOutput()
	{
		final DecimalFormat df = new DecimalFormat("###.#");
        
        // Heading stuff
        printHeading();
        
        // Group containing data
        printDataGroup();
        
        // Frame and labels
        if (!noFrame)
        {
            if (altFrame)
            {
                printAltFrame(df);
                printAltLabels(df);
            }
            else
                printDavesFrameAndLabels();
        }
        
        // Dotlist for common-atom value for each rotamer
        if (rotaBalls != null)
            printRotaBalls();
    }
//}}}

//{{{ printHeading
//##################################################################################################
	private void printHeading()
	{
        // @kin, if desired
        if (kinHeading) System.out.println("@kin {"+csvFilename+"}");
        
        // @flat, if appropriate
        if (numDims == 2) System.out.println("@flat");
        
        // Masters
        System.out.println("@master {data}");
        if (rotaBalls != null) System.out.println("@master {rota centers}");

        // Multi-D kin heading stuff (@dimension...) for sc's with >=3 chis
        if (numDims > 3)
        {
            
            System.out.print("@dimension ");
            for (int d = 0; d < numDims; d ++)
                System.out.print("{"+labels[d]+"} ");
            System.out.println();
            
            System.out.print("@dimminmax ");
            for (int d = 0; d < numDims; d ++)
            {
                if (wrap360[d])         System.out.print("0 360 ");
                else if (wrap180[d])    System.out.print("-180 180 ");
                else                    System.out.print(mins[d]+" "+maxes[d]+" ");
            }
            System.out.println();
        }
    }
//}}}

//{{{ printDavesFrameAndLabels
//##################################################################################################
	private void printDavesFrameAndLabels()
	{
        System.out.println("@subgroup {Dave's frame} dominant master= {frame}");
        System.out.println("@vectorlist {frame} color= white");
        System.out.println("P  0.000 0.000 0.000");
        System.out.println(" 5.000 0.000 0.000");
        System.out.println("P  35.000 0.000 0.000");
        System.out.println(" 40.000 0.000 0.000");
        System.out.println("P  80.000 0.000 0.000");
        System.out.println(" 160.000 0.000 0.000");
        System.out.println("P  0.000 0.000 0.000");
        System.out.println(" 0.000 5.000 0.000");
        System.out.println("P  0.000 35.000 0.000");
        System.out.println(" 0.000 40.000 0.000");
        System.out.println("P  0.000 80.000 0.000");
        System.out.println(" 0.000 160.000 0.000");
        System.out.println("P  0.000 0.000 0.000");
        System.out.println(" 0.000 0.000 5.000");
        System.out.println("P  0.000 0.000 35.000");
        System.out.println(" 0.000 0.000 40.000");
        System.out.println("P  0.000 0.000 80.000");
        System.out.println(" 0.000 0.000 160.000");
        System.out.println("P  200.000 0.000 0.000");
        System.out.println(" 280.000 0.000 0.000");
        System.out.println("P  320.000 0.000 0.000");
        System.out.println(" 360.000 0.000 0.000");
        System.out.println("P  0.000 200.000 0.000");
        System.out.println(" 0.000 280.000 0.000");
        System.out.println("P  0.000 320.000 0.000");
        System.out.println(" 0.000 360.000 0.000");
        System.out.println("P  0.000 0.000 200.000");
        System.out.println(" 0.000 0.000 280.000");
        System.out.println("P  0.000 0.000 320.000");
        System.out.println(" 0.000 0.000 360.000");
        System.out.println("@subgroup {Dave's labels} dominant master= {labels}");
        System.out.println("@labellist {XYZ} color= white");
        System.out.println("{X} 20.000 -5.000 -5.000");
        System.out.println("{X} 380.000 -5.000 -5.000");
        System.out.println("{Y} -5.000 20.000 -5.000");
        System.out.println("{Y} -5.000 380.000 -5.000");
        System.out.println("{Z} -5.000 -5.000 20.000");
        System.out.println("{Z} -5.000 -5.000 380.000");
        System.out.println("@labellist {mtp} color= green");
        System.out.println("{p} 60.000 0.000 0.000");
        System.out.println("{t} 180.000 0.000 0.000");
        System.out.println("{m} 300.000 0.000 0.000");
        System.out.println("{p} 0.000 60.000 0.000");
        System.out.println("{t} 0.000 180.000 0.000");
        System.out.println("{m} 0.000 300.000 0.000");
        System.out.println("{p} 0.000 0.000 60.000");
        System.out.println("{t} 0.000 0.000 180.000");
        System.out.println("{m} 0.000 0.000 300.000");
    }
//}}}

//{{{ printAltFrame
//##################################################################################################
	private void printAltFrame(DecimalFormat df)
	{
        if (verbose) System.err.println("adding alt frame...");
        
        System.out.println("@subgroup {frame} dominant master= {frame} ");
        System.out.println("@vectorlist {frame} color= white ");
        
        // Now, axis generation is general so that only the columns the user
        // desires to be wrapped (but not necessarily all of the columns the user
        // desires to be in the output kin or csv!) are wrapped
        for (int i = 0; i < numDims; i ++)
        {
            // Special case for Z axis if numDims = 3 and -scaleZ=# flag used
            // (First implemented b/c Z axis was bond angles while X+Y axes were
            // dihedrals --> different visual scales desirable)
            if (i == 2 && numDims == 3 && scaleZ)
            {
                // Assume other two axes (X, Y) are in XY plane where Z = 0
                // Want bottom of Z axis to be below that plane or at least in it
                // Likewise, want top of Z axis to be above that plane or at least in it
                // (This is just for visual effect)
                if (scale(mins[2], 2) > 0)
                    System.out.println("{min "+df.format(inverseScale(0,2))+"}P   0.000 0.000 0");
                else // "normal" way
                    System.out.println("{min "+mins[2]+"}P   0.000 0.000 "+scale(mins[2], 2));
                
                if (scale(maxes[2], 2) < 0)
                    System.out.println("{max "+df.format(inverseScale(0,2))+"}   0.000 0.000 0");
                else // "normal" way
                    System.out.println("{max "+maxes[2]+"}   0.000 0.000 "+scale(maxes[2], 2));
                
            }
            else
            {
                if (wrap360[i])
                {
                    System.out.print("{min 0  }P ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" 0 ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{max 360} ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" 360 ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                }
                else if (wrap180[i])
                {
                    System.out.print("{min -180}P ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" -180 ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{max 180} ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" 180 ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                }
                else // don't use angle-like "180/360" axes; instead, do it based on the data
                {
                    System.out.print("{min "+mins[i]+"}P ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" "+mins[i]+" ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{max "+df.format(maxes[i])+"} ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" "+df.format(maxes[i])+" ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                }
            }
        }
    }
//}}}

//{{{ printAltLabels
//##################################################################################################
	private void printAltLabels(DecimalFormat df)
	{
        if (verbose) System.err.println("adding alt labels...");
        
        System.out.println("@subgroup {labels} dominant master= {labels} ");
        System.out.println("@labellist {XYZ} color= white ");
        
        // Now, axis generation is general so that only the columns the user
        // desires to be wrapped (but not necessarily all of the columns the user
        // desires to be in the output kin or csv!) are wrapped
        for (int i = 0; i < numDims; i ++)
        {
            // Special case for Z axis if numDims = 3 and -scaleZ=# flag used
            // (First implemented b/c Z axis was bond angles while X+Y axes were
            // dihedrals --> different visual scales desirable)
            if (i == 2 && numDims == 3 && scaleZ)
            {
                // Assume other two axes (X, Y) are in XY plane where Z = 0
                // Want bottom of Z axis to be below that plane or at least in it
                // Likewise, want top of Z axis to be above that plane or at least in it
                // (This is just for visual effect)
                //if (scale(mins[2], 2) > 0)
                //    System.out.println("{"+labels[2]+" "+df.format(inverseScale(0,2))+"} 0 0 0");
                //else
                //    System.out.println("{"+labels[2]+" "+df.format(mins[2] )+"} "+"0 0 "+scale(mins[2], 2));
                
                if (scale(maxes[2], 2) < 0)
                    System.out.println("{"+labels[2]+" "+df.format(inverseScale(0,2))+"} 0 0 0");
                else
                    System.out.println("{"+labels[2]+" "+df.format(maxes[2])+"} "+"0 0 "+scale(maxes[2], 2));
            }
            else
            {
                if (wrap360[i])
                {
                    //System.out.print("{"+labels[i]+" 0  }P ");
                    //for (int c = 0; c < i; c ++)
                    //    System.out.print(" 0.000 ");
                    //System.out.print(" 0 ");
                    //for (int c = i+1; c < numDims; c ++)
                    //    System.out.print(" 0.000 ");
                    //System.out.println();
                    
                    System.out.print("{"+labels[i]+" 360} ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" 360 ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                }
                else if (wrap180[i])
                {
                    //System.out.print("{"+labels[i]+" -180}P ");
                    //for (int c = 0; c < i; c ++)
                    //    System.out.print(" 0.000 ");
                    //System.out.print(" -180 ");
                    //for (int c = i+1; c < numDims; c ++)
                    //    System.out.print(" 0.000 ");
                    //System.out.println();
                    
                    System.out.print("{"+labels[i]+" 180} ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" 180 ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                }
                else
                {
                    //System.out.print("{"+labels[i]+" "+mins[i] +"} ");
                    //for (int c = 0; c < i; c ++)
                    //    System.out.print(" 0.000 ");
                    //System.out.print(" "+mins[i]+" ");
                    //for (int c = i+1; c < numDims; c ++)
                    //    System.out.print(" 0.000 ");
                    //System.out.println();
                    
                    System.out.print("{"+labels[i]+" "+df.format(maxes[i]) +"} ");
                    for (int c = 0; c < i; c ++)          System.out.print(" 0.000 ");
                    System.out.print(" "+df.format(maxes[i])+" ");
                    for (int c = i+1; c < numDims; c ++)  System.out.print(" 0.000 ");
                    System.out.println();
                }
            }
        }
    }
//}}}

//{{{ printDataGroup
//##################################################################################################
	private void printDataGroup()
	{
        
        if (!groupName.equals("")) System.out.print("@group {"+groupName);
        else
        {
            System.out.print("@group {"+groupName+" ");
            for (int i = 0; i < cols.length; i ++)
            {
                if (i < cols.length - 1) System.out.print("col"+cols[i]+", ");
                else                     System.out.print("col"+cols[i]);
            }
        }
        System.out.print("} animate ");
        if (masters != null)  for (String master : masters)  System.out.print("master= {"+master+"} ");
        if (numDims > 3)  System.out.print("dimension="+numDims+" "); // select???
        System.out.println();
        
        // Subgroup & dotlist
        System.out.println("@subgroup {data} master= {data} dominant ");
        System.out.println("@dotlist {data} color= "+color);
        for (int i = 0; i < pts.size(); i ++)
        {
            double[] pt = pts.get(i);
            
            // Point ids
            if (ptids != null) // use indicated point id
                System.out.print("{"+ptids.get(i)+"} ");                               
            else               // use arbitrary 'point#' for point id
            {
                System.out.print("{");
                for (int c = 0; c < cols.length; c ++)
                    System.out.print(pt[c]+" ");
                System.out.print("} ");
            }
            
            // Coordinates
            for (int c = 0; c < cols.length; c ++)
            {
                if (c == 2 && scaleZ) // Treat Z differently than X and Y
                {
                    // For scaling Z to X- & Y-like dimensions, if desired
                    // First added for Karplus's PGD bb geometry stuff, to get tau
                    // angles on same scale as 0to360 (or -180to180) phi & psi.
                    pt[c] = scale(pt[c], 2);
                    System.out.print(pt[c]+" ");
                }
                else
                {
                    if (wrap360[c])        System.out.print(wrap360(pt[c])+" ");
                    else if (wrap180[c])   System.out.print(wrap180(pt[c])+" ");
                    else                   System.out.print(pt[c]+" ");
                }
            }
            System.out.println();
        }
    }
//}}}

//{{{ printRotaBalls
//##################################################################################################
	private void printRotaBalls()
	{
        ArrayList<String> lines = getRotaBalls();
        int dim = 0;
        for (String line : lines)
        {
            if (line.indexOf("@balllist") == 0)
            {
                // format: "@balllist {rota common-atom ball(s)} radius= 3.0 color= green"
                System.out.println(line);
            }
            else
            {
                // format: "{pp 62 80} 62 80"
                System.out.print( line.substring(0, line.indexOf("}")+1)+" " );
                Scanner s = new Scanner( line.substring(line.indexOf("}")+1) );
                while (s.hasNext())
                {
                    double coord = s.nextDouble();
                    if      (wrap360[dim])  System.out.print(wrap360(coord)+" ");
                    else if (wrap180[dim])  System.out.print(wrap180(coord)+" ");
                    else                    System.out.print(coord         +" ");
                    dim ++;
                }
                System.out.println();
                dim = 0;
            }
        }
    }
//}}}

//{{{ getRotaBalls
//##################################################################################################
	private ArrayList<String> getRotaBalls()
	{
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("@balllist {rota common-atom ball(s)} radius= 3.0 color= green master= {rota centers}");
        
        for (int r = 0; r < rotaBalls.size(); r ++)
        {
            String resRota = rotaBalls.get(r);
            String res = resRota.substring(0,3).toLowerCase();
            String rota = resRota.substring(3).toLowerCase();
            boolean doAll = false;
            
            if (res.equals("leu"))
            {
                if (rota.equals("all")) doAll = true;
                if (rota.equals("pp")  || doAll) lines.add("{pp   62 80} 62 80");
                if (rota.equals("tp")  || doAll) lines.add("{tp   -177 65} -177 65");
                if (rota.equals("tt")  || doAll) lines.add("{tt   -172 145} -172 145");
                if (rota.equals("mp")  || doAll) lines.add("{mp   -85 65} -85 65");
                if (rota.equals("mt")  || doAll) lines.add("{mt   -65 175} -65 175");
                if (rota.equals("pt?") || doAll) lines.add("{pt?   60? 180?} 60 180");   // approx.
                if (rota.equals("tm?") || doAll) lines.add("{tm?   180? 300?} 180 300"); // approx.
                if (rota.equals("mm?") || doAll) lines.add("{mm?   300? 300?} 300 300"); // approx.
                // mm is in Penultimate, but not in chiropraxis, so it's left out here
                doAll = false;
            }
	    if (res.equals("arg"))
            {
                if (rota.equals("all")) doAll = true;
                
                if (rota.equals("ptp85")    || doAll) lines.add("{ptp85   62 180 65 85} 62 180 65 85");
                if (rota.equals("ptp180")   || doAll) lines.add("{ptp180   62 180 65 -175} 62 180 65 -175");
                
                if (rota.equals("ptt85")    || doAll) lines.add("{ptt85   62 180 180 85} 62 180 180 85");
                if (rota.equals("ptt180")   || doAll) lines.add("{ptt180   62 180 180 180} 62 180 180 180");
                if (rota.equals("ptt-85")   || doAll) lines.add("{ptt-85   62 180 180 -85} 62 180 180 -85");
                
                if (rota.equals("ptm180")   || doAll) lines.add("{ptm180   62 180 -65 175} 62 180 -65 175");
                if (rota.equals("ptm-85")   || doAll) lines.add("{ptm-85   62 180 -65 -85} 62 180 -65 -85");
                
                if (rota.equals("tpp85")    || doAll) lines.add("{tpp85   -177 65 65 85} -177 65 65 85");
                if (rota.equals("tpp180")   || doAll) lines.add("{tpp180   -177 65 65 -175} -177 65 65 -175");
                
                if (rota.equals("tpt85")    || doAll) lines.add("{tpt85   -177 65 180 85} -177 65 180 85");
                if (rota.equals("tpt180")   || doAll) lines.add("{tpt180   -177 65 180 180} -177 65 180 180");
                
                if (rota.equals("ttp85")    || doAll) lines.add("{ttp85   -177 180 65 85} -177 180 65 85");
                if (rota.equals("ttp180")   || doAll) lines.add("{ttp180   -177 180 65 -175} -177 180 65 -175");
                if (rota.equals("ttp-105")  || doAll) lines.add("{ttp-105   -177 180 65 105} -177 180 65 -105");
                
                if (rota.equals("ttt85")    || doAll) lines.add("{ttt85   -177 180 180 85} -177 180 180 85");
                if (rota.equals("ttt180")   || doAll) lines.add("{ttt180   -177 180 180 180} -177 180 180 180");
                if (rota.equals("ttt-85")   || doAll) lines.add("{ttt-85   -177 180 180 -85} -177 180 180 -85");
                
                if (rota.equals("ttm105")   || doAll) lines.add("{ttm105   -177 180 -65 105} -177 180 -65 105");
                if (rota.equals("ttm180")   || doAll) lines.add("{ttm180   -177 180 -65 175} -177 180 -65 175");
                if (rota.equals("ttm-85")   || doAll) lines.add("{ttm-85   -177 180 -65 -85} -177 180 -65 -85");
                
                if (rota.equals("mtp85")    || doAll) lines.add("{mtp85   -67 180 65 85} -67 180 65 85");
                if (rota.equals("mtp180")   || doAll) lines.add("{mtp180   -67 180 65 -175} -67 180 65 -175");
                if (rota.equals("mtp-105")  || doAll) lines.add("{mtp-105   -67 180 65 -105} -67 180 65 -105");
                
                if (rota.equals("mtt85")    || doAll) lines.add("{mtt85   -67 180 180 85} -67 180 180 85");
                if (rota.equals("mtt180")   || doAll) lines.add("{mtt180   -67 180 180 180} -67 180 180 180");
                if (rota.equals("mtt-85")   || doAll) lines.add("{mtt-85   -67 180 180 -85} -67 180 180 -85");
                
                if (rota.equals("mtm105")   || doAll) lines.add("{mtm105   -67 180 -65 105} -67 180 -65 105");
                if (rota.equals("mtm180")   || doAll) lines.add("{mtm180   -67 180 -65 175} -67 180 -65 175");
                if (rota.equals("mtm-85")   || doAll) lines.add("{mtm-85   -67 180 -65 -85} -67 180 -65 -85");
                
                if (rota.equals("mmt85")    || doAll) lines.add("{mmt85   -62 -68 180 85} -62 -68 180 85");
                if (rota.equals("mmt180")   || doAll) lines.add("{mmt180   -62 -68 180 180} -62 -68 180 180");
                if (rota.equals("mmt-85")   || doAll) lines.add("{mmt-85   -62 -68 180 -85} -62 -68 180 -85");
                
                if (rota.equals("mmm180")   || doAll) lines.add("{mmm180   -62 -68 -65 175} -62 -68 -65 175");
                if (rota.equals("mmm-85")   || doAll) lines.add("{mmm-85   -62 -68 -65 -85} -62 -68 -65 -85");
                
                if (rota.equals("ppp_?")  || doAll) lines.add("{ppp_?   60? 60? 60? ??} 60 60 60 0"); // approx.
                if (rota.equals("ppt_?")  || doAll) lines.add("{ppt_?   60? 60? 180? ??} 60 60 180 0"); // approx.
                if (rota.equals("ppm_?")  || doAll) lines.add("{ppm_?   60? 60? 300? ??} 60 60 300 0"); // approx.
                if (rota.equals("pmp_?")  || doAll) lines.add("{pmp_?   60? 300? 60? ??} 60 300 60 0"); // approx.
                if (rota.equals("pmt_?")  || doAll) lines.add("{pmt_?   60? 300? 180? ??} 60 300 60 0"); // approx.
                if (rota.equals("pmm_?")  || doAll) lines.add("{pmm_?   60? 300? 300? ??} 60 300 300 0"); // approx.
                if (rota.equals("tpm_?")  || doAll) lines.add("{tpm_?   180? 60? 300? ??} 180 60 300 0"); // approx.
                if (rota.equals("tmp_?")  || doAll) lines.add("{tmp_?   180? 300? 60? ??} 180 300 60 0"); // approx.
                if (rota.equals("tmt_?")  || doAll) lines.add("{tmt_?   180? 300? 180? ??} 180 300 180 0"); // approx.
                if (rota.equals("tmm_?")  || doAll) lines.add("{tmm_?   180? 300? 300? ??} 180 300 300 0"); // approx.
                if (rota.equals("mpp_?")  || doAll) lines.add("{mpp_?   300? 60? 60? ??} 300 60 60 0"); // approx.
                if (rota.equals("mpt_?")  || doAll) lines.add("{mpt_?   300? 60? 180? ??} 300 60 180 0"); // approx.
                if (rota.equals("mpm_?")  || doAll) lines.add("{mpm_?   300? 60? 300? ??} 300 60 300 0"); // approx.
                if (rota.equals("mmp_?")  || doAll) lines.add("{mmp_?   300? 300? 60? ??} 300 300 60 0"); // approx.
                
                doAll = false;
            }
            // more res types here...
        }
        
        return lines;
    }
//}}}


//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
    	doChecks();
        
        // If not doing kin (last command in this case)
        if (noKin)
        {
            if (csv2 != null && csv2filename != null) concatFilesByRow();
            else                                      getAndPrintDesiredColumns();
        }
        
        // If doing kin
        readInput();
        getMaxes();
        getMins();
        getAvgs();
        if (verbose) for (int i = 0; i < maxes.length; i++)
            System.err.println("coord"+i+"  min, max, avg, wrap360, wrap180:  ("
                +mins[i]+", "+maxes[i]+", "+avgs[i]+", "+wrap360[i]+", "+wrap180[i]+")");
        
        // Rework mins if nec. so axes actually touch each other in kin
        for (int i = 0; i < mins.length; i++)  if (mins[i] > 0)  mins[i] = 0;
        
        printOutput();
	}
    
    public static void main(String[] args)
	{
        CsvToKinner mainprog = new CsvToKinner();
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
            InputStream is = getClass().getResourceAsStream("CsvToKinner.help");
            if(is == null) System.err.println(
                "\n*** Unable to locate help information in 'DihedralPlotter.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.CsvToKinner");
        System.err.println("Copyright (C) 2007 by Daniel A. Keedy. All rights reserved.");
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
        if(csv == null && csvFilename == null)
        {
            csvFilename = arg;
            csv  = new File(arg);
        }
        else if (csv2 == null && csv2filename == null)
        {
            csv2filename = arg;
            csv2  = new File(arg);
        }
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
            else if(flag.equals("-columns") || flag.equals("-cols"))
            {
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    int numCols = 0;
                    ArrayList<Integer> colsAL = new ArrayList<Integer>();
                    while (s.hasNext())
                    {
                        colsAL.add(Integer.parseInt(s.next()));
                        numCols ++;
                    }
                    cols = new int[colsAL.size()];
                    for (int j = 0; j < colsAL.size(); j ++)
                        cols[j] = colsAL.get(j);
                    numDims = colsAL.size();
                }
                else 
                {
                    System.err.println("Need n comma-separated #s with this flag: "
                        +"-columns=3,5,8 or -columns=0,3,4,5,8,67");
                    System.err.println("Default: 0,1,2");
                    System.err.println("Indicates x,y,z,... data for kinemage");
                }
            }
            else if(flag.equals("-delimiter") || flag.equals("-delim"))
            {
                if (param != null)
                {
                    delimiter = param;
                }
                else 
                {
                    System.err.println("Need a delimiter with this flag: "
                        +"-delimiter=[text] where text is : or ; or ...");
                    System.err.println("Default is :");
                }
            }
            else if(flag.equals("-kinheading") || flag.equals("-kinhead"))
            {
                kinHeading = true;
            }
            else if(flag.equals("-labels"))
            {
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    int numLabels = 0;
                    ArrayList<String> labelsAL = new ArrayList<String>();
                    while (s.hasNext() && numLabels < cols.length)
                    {
                        labelsAL.add(s.next());
                        numLabels ++;
                    }
                    labels = new String[labelsAL.size()];
                    for (int j = 0; j < labelsAL.size(); j ++)
                        labels[j] = labelsAL.get(j);
                }
                else
                {
                    System.err.println("Need comma-sep'd labels with this flag: "
                        +"-labels=[word],[word],... where word is X, phi, etc.");
                    System.err.println("Default: X,Y,Z");
                }
            }
            else if(flag.equals("-wrap360"))
            {
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    ArrayList<String> wrap360AL = new ArrayList<String>();
                    while (s.hasNext())
                    {
                        String elem = s.next();
                        try
                        {
                            int colToWrap360 = 999;
                            colToWrap360 = Integer.parseInt(elem);
                            if (colToWrap360 != 999) // i.e. a valid column specifier is given
                                wrap360AL.add("true");
                        }
                        catch (NumberFormatException nfe)
                        {
                            System.err.println("Couldn't parse '"+elem+"' as an integer");
                        }
                    }
                    wrap360 = new boolean[wrap360AL.size()];
                    for (int i = 0; i < wrap360AL.size(); i++)
                    {
                        if (wrap360AL.get(i).equals("true"))  wrap360[i] = true;
                        else                                  wrap360[i] = false;
                    }
                }
                else
                {
                    System.err.println("Need comma-sep'd labels with this flag: "
                        +"-wrap360=0,2,4 or -wrap360=0 for example");
                }
            }
            else if(flag.equals("-wrap180"))
            {
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    ArrayList<String> wrap180AL = new ArrayList<String>();
                    while (s.hasNext())
                    {
                        String elem = s.next();
                        try
                        {
                            int colToWrap180 = 999;
                            colToWrap180 = Integer.parseInt(elem);
                            if (colToWrap180 != 999) // i.e. a valid column specifier is given
                                wrap180AL.add("true");
                        }
                        catch (NumberFormatException nfe)
                        {
                            System.err.println("Couldn't parse '"+elem+"' as an integer");
                        }
                    }
                    wrap180 = new boolean[wrap180AL.size()];
                    for (int i = 0; i < wrap180AL.size(); i++)
                    {
                        if (wrap180AL.get(i).equals("true"))  wrap180[i] = true;
                        else                                  wrap180[i] = false;
                    }
                }
                else
                {
                    System.err.println("Need comma-sep'd labels with this flag: "
                        +"-wrap180=0,2,4 or -wrap180=0 for example");
                }
            }
            else if(flag.equals("-scalez"))
            {
                scaleZ = true;
                if (param != null)
                    scalingFactorZ = Integer.parseInt(param);
            }
            else if(flag.equals("-nokin"))
            {
                noKin = true;
                noKinPts = new ArrayList<String[]>();
            }
            else if(flag.equals("-kin"))
            {
                noKin = false;
                noKinPts = null;
            }
            else if(flag.equals("-noframe"))
            {
                noFrame = true;
            }
            else if(flag.equals("-groupname") || flag.equals("-group"))
            {
                if (param != null)
                    groupName = param;
                else
                    System.out.println("Need a word for the kinemage group's name with this flag!");
            }
            else if(flag.equals("-rotaballs"))
            {
                rotaBalls = new ArrayList<String>();
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    while (s.hasNext())
                    {
                        String elem = s.next();
                        String possAaName = (elem.substring(0,3)).toLowerCase();
                        if (AA_NAMES_NO_ALA_GLY.indexOf(possAaName) != -1)
                            rotaBalls.add(elem);
                        else
                        {
                            System.err.println("Couldn't understand parameter '"+elem+"'");
                            System.err.println("Expected an amino acid name + rotamer,");
                            System.err.println("  e.g. leutp");
                        }
                    }
                }
                else
                {
                    System.err.println("Need comma-sep'd labels with this flag: "
                        +"-rotaballs=leutp,leupp or -rotaballs=leuall for example");
                }
            }
            else if(flag.equals("-pointidcols"))
            {
                ArrayList<Integer> ptidcolsAL = new ArrayList<Integer>(); // temporary
                ptids = new ArrayList<String>();
                if (param != null)
                {
                    Scanner s = new Scanner(param).useDelimiter(",");
                    while (s.hasNext())
                    {
                        String elem = s.next();
                        int ptidcol = 999;
                        ptidcol = Integer.parseInt(elem);
                        if (ptidcol != 999) // i.e. a valid column specifier is given
                            ptidcolsAL.add(ptidcol);
                        else
                        {
                            System.err.println("Couldn't understand parameter '"+elem+"'");
                            System.err.println("Expected an integer");
                        }
                    }
                    ptidcols = new int[ptidcolsAL.size()]; // permanent ptidcol container
                    for (int j = 0; j < ptidcolsAL.size(); j++)
                        ptidcols[j] = ptidcolsAL.get(j);
                    
                }
                else
                {
                    System.err.println("Need comma-sep'd integers with this flag: "
                        +"-pointidcols=0 or -pointidcols=0,1 for example");
                }
            }
            else if(flag.equals("-altframe"))
            {
                altFrame = true;
            }
            else if(flag.equals("-color"))
            {
                color = param;
            }
            else if(flag.equals("-master"))
            {
                masters = new ArrayList<String>();
                String[] ms = Strings.explode(param, ',');
                for (String m : ms)  masters.add(m);
            }
            else if(flag.equals("-dummy-option"))
            {
                // Do something...
            }
            else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
        }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Non-number argument to "+flag+": '"+param+"'"); }
    }
//}}}

} //class
