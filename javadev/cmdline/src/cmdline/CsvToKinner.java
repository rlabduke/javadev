// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package/Imports
//##################################################################################################
package cmdline;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import driftwood.r3.*;
//}}}

/**
* <code>CsvToKinner</code> takes in a delimited text file and outputs
* either a kinemage that plots the values in the n columns specified by 
* the user in 3D space or writes those n columns back out in csv format.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* 
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
    int numDims;    
    String csvFilename;
    File csv;
    int[] cols;                 // column ids/indices to look for in csv for n-D data
    int[] ptidcols;             // column ids/indices to look for in csv for pointids
    String[] labels;            // one for each column 
    ArrayList<double[]> pts;    // size() is # rows in csv file
    ArrayList<String> ptids;    // size() is # rows in csv file
    double[] maxes;             // for axis placement purposes
    double[] mins;              // for axis placement purposes
    double[] avgs;
    String delimiter;           // ":", ",", etc.
    boolean kinHeading;
    boolean[] wrap360;
    boolean[] wrap180;
    boolean scaleZ;
    int scalingFactorZ;
    boolean noKin;
    ArrayList<String[]> noKinPts;
    boolean noFrame;
    String groupName;
    ArrayList<String> rotaBalls;
    boolean altFrame;
    String color;
    
//}}}

//{{{ main
//##################################################################################################
	public static void main(String[] args)
	{
		CsvToKinner ctk = new CsvToKinner();
		
        // Must get number of dimensions first to avoid arrayIndexOutOfBoundsErrors, etc. below
        // Use specified columns to do so
        ctk.getNumDims(args);
        
        ctk.setUpArrays();
        ctk.parseArgs(args);
        ctk.doChecks();
        
        // If not doing kin (last command in this case)
        ctk.getAndPrintDesiredColumns();
        
        // If doing kin
        ctk.readInput();
        ctk.getMaxes();
        ctk.getMins();
        ctk.getAvgs();
        ctk.printOutput();
	}
//}}}

//{{{ Constructor
//##################################################################################################
	public CsvToKinner()
	{
        // Initialize defaults
        numDims = 3;
        csv = null;
        csvFilename = "no_filename_given";
        String delimiter = ":";
        kinHeading = false;
        scaleZ = false;
        scalingFactorZ = 10;
        noKin = false;
        noFrame = false;
        groupName = "";
        altFrame = false;
        color = "white";
    }
//}}}

//{{{ getNumDims
//##################################################################################################
	public void getNumDims(String[] args)
	{
		String  arg, flag, param;
        for (int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") )
            {
                // This is probably a filename or something
                //csvFilename = arg;
                //csv = new File(arg);
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
                
                // This flag will give us # dimensions
                if(flag.equals("-columns") || flag.equals("-cols"))
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
                        System.out.println("Need n comma-separated #s with this flag: "
                            +"-columns=3,5,8 or -columns=0,3,4,5,8,67");
                        System.out.println("Default: 0,1,2");
                        System.out.println("Indicates x,y,z,... data for kinemage");
                    }
                }
            }
        }
    }
//}}}

//{{{ setUpArrays
//##################################################################################################
	public void setUpArrays()
	{
        //cols = new int[numDims];
        //for (int i = 0; i < numDims; i ++)
        //    cols[i] = i;
        
        labels = new String[numDims];
        char xChar = 'X';
        for (int i = 0; i < numDims; i ++)
            labels[i] = "" + (int) xChar + i;  // should give 'X', 'Y', 'Z', ...
        
        wrap360 = new boolean[numDims];
        for (int i = 0; i < numDims; i ++)
            wrap360[i] = false;
        wrap180 = new boolean[numDims];
        for (int i = 0; i < numDims; i ++)
            wrap180[i] = false;
        
        rotaBalls = null;
        ptidcols = null;
        ptids = null;
    }
//}}}

//{{{ parseArgs
//##################################################################################################
	public void parseArgs(String[] args)
	{
		String  arg, flag, param;
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") )
            {
                // This is probably a filename or something
                csvFilename = arg;
                csv = new File(arg);
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
                
                // Look thru options for this flag
                if(flag.equals("-help") || flag.equals("-h"))
                {
                    showHelp(true);
                    System.exit(0);
                }
                else if(flag.equals("-delimiter") || flag.equals("-delim"))
                {
                    if (param != null)
                    {
                        delimiter = param;
                    }
                    else 
                    {
                        System.out.println("Need a delimiter with this flag: "
                            +"-delimiter=[text] where text is : or ; or ...");
                        System.out.println("Default is :");
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
                        numDims = labelsAL.size();
                    }
                    else
                    {
                        System.out.println("Need comma-sep'd labels with this flag: "
                            +"-labels=[word],[word],... where word is X, phi, etc.");
                        System.out.println("Default: X,Y,Z");
                    }
                }
                else if(flag.equals("-wrap360"))
                {
                    if (param != null)
                    {
                        Scanner s = new Scanner(param).useDelimiter(",");
                        while (s.hasNext())
                        {
                            String elem = s.next();
                            int colToWrap360 = 999;
                            colToWrap360 = Integer.parseInt(elem);
                            if (colToWrap360 != 999) // i.e. a valid column specifier is given
                                wrap360[colToWrap360] = true;
                            else
                            {
                                System.out.println("Couldn't understand parameter '"+elem+"'");
                                System.out.println("Expected an integer");
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Need comma-sep'd labels with this flag: "
                            +"-wrap360=0,2,4 or -wrap360=0 for example");
                    }
                }
                else if(flag.equals("-wrap180"))
                {
                    if (param != null)
                    {
                        Scanner s = new Scanner(param).useDelimiter(",");
                        while (s.hasNext())
                        {
                            String elem = s.next();
                            int colToWrap180 = 999;
                            colToWrap180 = Integer.parseInt(elem);
                            if (colToWrap180 != 999) // i.e. a valid column specifier is given
                                wrap180[colToWrap180] = true;
                            else
                            {
                                System.out.println("Couldn't understand parameter '"+elem+"'");
                                System.out.println("Expected an integer");
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Need comma-sep'd labels with this flag: "
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
                                System.out.println("Couldn't understand parameter '"+elem+"'");
                                System.out.println("Expected an amino acid name + rotamer,");
                                System.out.println("  e.g. leutp");
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Need comma-sep'd labels with this flag: "
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
                                System.out.println("Couldn't understand parameter '"+elem+"'");
                                System.out.println("Expected an integer");
                            }
                        }
                        
                        ptidcols = new int[ptidcolsAL.size()]; // permanent ptidcol container
                        for (int j = 0; j < ptidcolsAL.size(); j++)
                            ptidcols[j] = ptidcolsAL.get(j);
                        
                    }
                    else
                    {
                        System.out.println("Need comma-sep'd integers with this flag: "
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
                else if(flag.equals("-dummy-option"))
                {
                    // Do something...
                }
                else
                {
                    if (!flag.equals("-columns") && !flag.equals("-cols"))
                        System.out.println("Couldn't understand flag: "+flag);   
                }
            }
        }//for(each arg in args)
    }
//}}}

//{{{ doChecks
//##################################################################################################
	void doChecks()
    {
        // Make sure there is a file provided
        if (csv == null)
        {
            System.out.println("Didn't specify an input csv file!");
            System.exit(0);
        }
        
        if (!noKin)
            // Make sure there are enough labels for the # columns requested
            if (labels.length < cols.length)
            {
                System.out.println("Not enough labels for the # columns requested!");
                String[] labelsNew = new String[cols.length];
                for (int i = 0; i < cols.length; i ++)
                {
                    if (i < labels.length)
                        labelsNew[i] = labels[i];
                    else
                        labelsNew[i] = "???";
                }
            }
        
        // Make sure user didn't try to do -scalez with numDims != 3
        // (scaleZ option assumes & requires x,y,z coordinate system)
        if (scaleZ && numDims != 3)
        {
            System.out.println("Can't to -scalez if # coordinates is not 3!");
            System.out.println("Disregarding -scalez ...");
            scaleZ = false;
        }
    }
//}}}

//{{{ showHelp
//##################################################################################################
	// Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("CsvToKinner.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'CsvToKinner.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.CsvToKinner");
        System.err.println("Copyright (C) 2007 by Daniel Keedy. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ readInput
//##################################################################################################
	private void readInput()
	{
		// Mission: Fill points with n-dimensional arrays from cols[0], cols[1], ...
        try
        {
            pts = new ArrayList<double[]>();
            for (double[] array : pts)
                array = new double[cols.length];
            Scanner fileScan = new Scanner(csv);
            boolean firstLine = true;
            while (fileScan.hasNextLine())
            {
                String line = fileScan.nextLine();
                //System.err.println(line);
                if (!firstLine)
                {
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
                            thisPoint[c] = Double.parseDouble(lineScan.next());
                        }
                        // Put this multi-D "point" into our ArrayList
                        pts.add(thisPoint);
                    }
                    catch (java.util.NoSuchElementException nsee)
                    {
                        System.out.println("Couldn't find indicated columns...");
                        System.out.println("Try using different delimiter instead of "+delimiter+"?");
                        System.exit(0);
                    }
                    
                }
                
                firstLine = false;
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.println("Can't read through csv input file...");   
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
            double xiMax = -999999999;
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
            double xiMin = 999999999;
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
        
        angle = angle % 360;
        if(angle < 0) return angle + 360;
        else return angle;
    }

    private double wrap180(double angle)
    {
        // Anything over 180 gets 360 subtracted from it
        // e.g. 210 --> 210 - 360 --> -150
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
        //for (int col : cols)
        //    System.out.println("col: "+col);
        
        if (noKin)
        {
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
                            System.out.println("Couldn't find indicated columns...");
                            System.out.println("Try using different delimiter?");
                            System.exit(0);
                        }
                        
                        // Put this multi-D set of Strings into our ArrayList
                        noKinPts.add(thisNoKinPoint);
                    }
                    
                    firstLine = false;
                }
            }
            catch (FileNotFoundException fnfe)
            {
                System.out.println("Can't read through csv input file...");   
            }
            
            
            // Print data to screen
            for (String[] noKinPt : noKinPts) // each row in csv
            {
                for (int c = 0; c < cols.length; c ++)
                    System.out.print(noKinPt[c] + delimiter);
                System.out.println();
            }
        
        // If not doing a kin, we're done!
        System.exit(0);
        
        }
    }
//}}}

//{{{ printOutput
//##################################################################################################
	private void printOutput()
	{
		final DecimalFormat df = new DecimalFormat("###.#");
        
        // Heading stuff
        printHeading();
        
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
        
        // Group containing data
        printDataGroup();
        
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
        if (kinHeading)
            System.out.println("@kin {"+csvFilename+"}");
        
        // Masters
        System.out.println("@master {data}");
        if (rotaBalls != null)
	    System.out.println("@master {rota centers}");

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
        System.out.println("@group {frame} dominant");
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
        System.out.println("@group {frame} dominant ");
        System.out.println("@vectorlist {frame} color= white");
        
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
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" 0 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{max 360} ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" 360 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                }
                else if (wrap180[i])
                {
                    System.out.print("{min -180}P ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" -180 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{max 180} ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" 180 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                }
                else // don't use angle-like "180/360" axes; instead, do it based on the data
                {
                    System.out.print("{min "+mins[i]+"}P ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" "+mins[i]+" ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{max "+maxes[i]+"} ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" "+maxes[i]+" ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
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
        System.out.println("@group {labels} dominant");
        System.out.println("@labellist {XYZ} color= white");
        
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
                    System.out.println("{"+labels[2]+" "+df.format(inverseScale(0,2))+"} 0 0 0");
                else
                    System.out.println("{"+labels[2]+" "+df.format(mins[2] )+"} "+"0 0 "+scale(mins[2], 2));
                
                if (scale(maxes[2], 2) < 0)
                    System.out.println("{"+labels[2]+" "+df.format(inverseScale(0,2))+"} 0 0 0");
                else
                    System.out.println("{"+labels[2]+" "+df.format(maxes[2])+"} "+"0 0 "+scale(maxes[2], 2));
            }
            else
            {
                if (wrap360[i])
                {
                    System.out.print("{"+labels[i]+" 0  }P ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" 0 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{"+labels[i]+" 360} ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" 360 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                }
                else if (wrap180[i])
                {
                    System.out.print("{"+labels[i]+" -180}P ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" -180 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{"+labels[i]+" 180} ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" 180 ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                }
                else
                {
                    System.out.print("{"+labels[i]+" "+mins[i] +"} ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" "+mins[i]+" ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
                    System.out.println();
                    
                    System.out.print("{"+labels[i]+" "+maxes[i] +"} ");
                    for (int c = 0; c < i; c ++)
                        System.out.print(" 0.000 ");
                    System.out.print(" "+maxes[i]+" ");
                    for (int c = i+1; c < numDims; c ++)
                        System.out.print(" 0.000 ");
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
        
        if (!groupName.equals(""))
            System.out.print("@group {"+groupName);
        else
        {
            System.out.print("@group {"+groupName+" ");
            for (int i = 0; i < cols.length; i ++)
            {
                if (i < cols.length - 1) System.out.print("col"+cols[i]+", ");
                else                     System.out.print("col"+cols[i]);
            }
        }
        System.out.print("} animate dominant ");
        if (numDims > 3)
            System.out.print("dimension="+numDims+" ");   // select???
        System.out.println();
        
        // Dotlist
        System.out.println("@dotlist {data} master= {data} color= "+color);
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
                if (c == 2) // Treat Z differently than X and Y
                {
                    // For scaling Z to X- & Y-like dimensions, if desired
                    // First added for Karplus's PGD bb geometry stuff, to get tau
                    // angles on same scale as 0to360 (or -180to180) phi & psi.
                    if (scaleZ)
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

} //class
