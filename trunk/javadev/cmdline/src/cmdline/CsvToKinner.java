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
//{{{ Variable Definitions
//##################################################################################################
    int numDims;    
    String csvFilename;
    File csv;
    int[] cols;                 // column ids/indices to look for in csv
    String[] labels;            // one for each column 
    ArrayList<double[]> pts;    // size() is # rows in csv file
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
        ctk.getAndPrintDesiredColumns();    // last command if not making a kin...
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
                if(flag.equals("-columns") || flag.equals("-col"))
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
        cols = new int[numDims];
        for (int i = 0; i < numDims; i ++)
            cols[i] = i;
        
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
                    showHelp();
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
                else if(flag.equals("-dummy-option"))
                {
                    // Do something...
                }
                else
                {
                    if (!flag.equals("-columns"))
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
        
        // Make sure there are enough labels for the # columns requested
        if (labels.length < cols.length && !noKin)
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
	void showHelp()
    {
        // Display help information
        System.out.println(                                                                    );
        System.out.println("cmdline.CsvToKinner DAK 2007"                                      );
        System.out.println("Makes a kin or outputs desired columns in csv format from data in" );
        System.out.println("  a comma-separated value (csv) input file"                        );
        System.out.println(                                                                    );
        System.out.println("Usage: java -cp cmdline.CsvToKinner [flags] [filename]"            );
        System.out.println(                                                                    );
        System.out.println("Flags:"                                                            );
        System.out.println("-COLumns=#,#,#,... uses designated columns for points in kinemage" );
        System.out.println("                   default: plot columns 0,1,2 in kin"             );
        System.out.println("                   if less than 3 columns, filled in with zeros"   );
        System.out.println("-DELIMiter=[text]  sets delimiter for reading csv input"           );
        System.out.println("                   default is :"                                   );
        System.out.println("-KINHEADing        prints @kin header in output"                   );
        System.out.println("-WRAP180=#,#,#,... or -WRAP360=#,#,#,..."                          );
        System.out.println("                   wraps data and axes -180 --> 180 or 0 --> 360"  );
        System.out.println("                   sets max & min for these axes to angle-like #s" );
        System.out.println("                      (e.g. 0-360 inst. of 12-344 dep'ing on data" );
        System.out.println("                   doesn't work for z axis right now..."           );
        System.out.println("-SCALEZ=#          subtracts the average Z from each Z point, then");
        System.out.println("                      (opt'l) multiplies by the provided int"      );
        System.out.println("                   assumes # dimension/columns is 3"               );
        System.out.println("-NOFRAME           outputs kin without axes and labels"            );
        System.out.println("-GROUPname         sets name of the group in the kin format output");
        System.out.println("-NOKIN             outputs columns indicated in csv (not kin) form");
        System.out.println(                                                                    );
        System.out.println("We assume row 0 is headings."                                      );
        System.out.println("Rows and columns measured 0 to n."                                 );
        System.out.println("Values must be parseable as doubles unless using -nokin."          );
        System.out.println(                                                                    );
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
            double pt0, pt1, pt2;
            while (fileScan.hasNextLine())
            {
                String line = fileScan.nextLine();
                if (!firstLine)
                {
                    // Make a multi-D "point" for this line
                    double[] thisPoint = new double[cols.length];
                    try
                    {
                        for (int c = 0; c < cols.length; c ++)
                        {
                            // Skip to jth column in this particular line
                            Scanner lineScan_c = new Scanner(line);
                            lineScan_c.useDelimiter(delimiter);
                            for (int j = 0; j < c; j++)
                                lineScan_c.next();
                            thisPoint[c] = Double.parseDouble(lineScan_c.next());
                        }
                    }
                    catch (java.util.NoSuchElementException nsee)
                    {
                        System.out.println("Couldn't find indicated columns...");
                        System.out.println("Try using different delimiter?");
                        System.exit(0);
                    }
                    
                    // Put this multi-D "point" into our ArrayList
                    pts.add(thisPoint);
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

//{{{ scale
//##################################################################################################
	private double scale(double value, int xi)
    {
        double valueScaled = value + (value - avgs[xi]) * scalingFactorZ;
        return valueScaled;
    }
//}}}

//{{{ getAndPrintDesiredColumns
//##################################################################################################
	private void getAndPrintDesiredColumns()
	{
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
        
        // Heading, if desired
        if (kinHeading)
            System.out.println("@kin {"+csvFilename+"}");
        
        // Frame and labels
        if (!noFrame)
        {
            printFrame(df);
            printLabels(df);
        }
        
        // Group
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
        System.out.println("} animate dominant");
        
        // Dotlist
        System.out.println("@dotlist {nuthin'}");
        for (int i = 0; i < pts.size(); i ++)
        {
            double[] pt = pts.get(i);
            System.out.print("{point"+i+" ");
            for (int c = 0; c < cols.length; c ++)
                System.out.print(pt[c]+" ");
            System.out.print("} ");
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
                    
                    
                    
                    // BELOW NEEDS TO CHANGE
                    
                    
                    
                    if (wrap360[c])        System.out.print(wrap360(pt[c])+" ");
                    else if (wrap180[c])   System.out.print(wrap180(pt[c])+" ");
                    else                   System.out.print(pt[c]+" ");
                }
            }
            System.out.println();
        }
    }
//}}}

//{{{ printFrame
//##################################################################################################
	private void printFrame(DecimalFormat df)
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
            if (i == 2 && scaleZ)
            {
                System.out.println("{min "+mins[2]+"}P   0.000 0.000 "+scale(mins[2], 2));
                System.out.println("{max "+maxes[2]+"}   0.000 0.000 "+scale(maxes[2], 2));
                continue; // skip to next column/dimension
            }
            
            if (wrap360[i])
            {
                System.out.print("{min 0  }P ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" 0 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
                
                System.out.print("{max 360} ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" 360 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
            }
            else if (wrap180[i])
            {
                System.out.print("{min -180}P ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" -180 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
                
                System.out.print("{max 180} ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" 180 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
            }
            else // don't use angle-like "180/360" axes; instead, do it based on the data
            {
                System.out.print("{min "+mins[i]+"}P ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" "+mins[i]+" ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
                
                System.out.print("{max "+maxes[i]+"} ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" "+maxes[i]+" ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
            }
        }
    }
//}}}

//{{{ printLabels
//##################################################################################################
	private void printLabels(DecimalFormat df)
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
            if (i == 2 && scaleZ)
            {
                System.out.println("{"+labels[2]+" "+df.format(mins[2] )+"} "+"0 0 "+scale(mins[2], 2));
                System.out.println("{"+labels[2]+" "+df.format(maxes[2])+"} "+"0 0 "+scale(maxes[2], 2));
            }
            
            if (wrap360[i])
            {
                System.out.print("{"+labels[i]+" 0  }P ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" 0 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
                
                System.out.print("{"+labels[i]+" 360} ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.print(" 360 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
            }
            else if (wrap180[i])
            {
                System.out.print("{"+labels[i]+" -180}P ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.println(" -180 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
                
                System.out.println("{"+labels[i]+" 180} ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.println(" 180 ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
            }
            else
            {
                System.out.print("{"+labels[i]+" "+mins[i] +"} ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.println(" "+mins[i]+" ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
                
                System.out.print("{"+labels[i]+" "+maxes[i] +"} ");
                for (int c = 0; c < i; c ++)
                    System.out.print(" 0.000 ");
                System.out.println(" "+maxes[i]+" ");
                for (int c = i; c < numDims; c ++)
                    System.out.print(" 0.000 ");
                System.out.println();
            }
        }
    }
//}}}

} //class