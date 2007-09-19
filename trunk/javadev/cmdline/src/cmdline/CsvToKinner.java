// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package/Imports
//##################################################################################################
package cmdline;

import java.io.*;
import java.util.*;
import driftwood.r3.*;
//}}}

/**
* <code>CsvToKinner</code> takes in a delimited text file and outputs
* a kinemage that plots the values in the n columns specified by the
* user in 3D space.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* 
*/
public class CsvToKinner
{ 
//{{{ Variable Definitions
//##################################################################################################
    String csvFilename;
    File csv;
    int[] cols;
    String[] labels;            // one for each column 
    ArrayList<double[]> pts;
    double max;                 // for axis placement purposes
    double min;                 // for axis placement purposes
    String delimiter;           // ":", ",", etc.
    boolean kinHeading;
    boolean wrap360;
    boolean wrap180;
    boolean scaleZ;
    int scalingFactorZ;
//}}}

//{{{ main
//##################################################################################################
	public static void main(String[] args)
	{
		CsvToKinner ctk = new CsvToKinner();
		ctk.parseArgs(args);
        ctk.readInput();
        ctk.getMaxMin();
        ctk.printOutput();
	}
//}}}

//{{{ Constructor
//##################################################################################################
	public CsvToKinner()
	{
        csv = null;
        csvFilename = "no_filename_given";
        cols = new int[3];
        cols[0] = 0;
        cols[1] = 1;
        cols[2] = 2;
        labels = new String[3];
        labels[0] = "X";
        labels[1] = "Y";
        labels[2] = "Z";
        String delimiter = ":";
        kinHeading = false;
        wrap360 = false;
        wrap180 = false;
        scaleZ = false;
        scalingFactorZ = 10;
        
        // Nothing more to see here.  Move along...
    }
//}}}

//{{{ parseArgs, showHelp
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
                else if(flag.equals("-columns") || flag.equals("-col"))
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
                    }
                    else 
                    {
                        System.out.println("Need three comma-separated #s with this flag: "
                            +"-columns=3,5,8");
                        System.out.println("Default: 0,1,2");
                        System.out.println("Indicates x,y,z data for kinemage");
                    }
                }
                else if(flag.equals("-delimiter") || flag.equals("-delim"))
                {
                    if (param != null)
                    {
                        if (param.equals("colon")) delimiter = ":";
                        if (param.equals("comma")) delimiter = ":";
                        if (param.equals("tab"))   delimiter = "\t";
                        if (param.equals("space")) delimiter = " ";
                    }
                    else 
                    {
                        System.out.println("Need a delimiter with this flag: "
                            +"-delimiter=[word] where word is colon, comma, tab,"
                            +"or space (current options...)");
                        System.out.println("Default: colon");
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
                        System.out.println("Need comma-sep'd labels with this flag: "
                            +"-labels=[word],[word],... where word is X, phi, etc.");
                        System.out.println("Default: X,Y,Z");
                    }
                }
                else if(flag.equals("-data0to360") || flag.equals("-0to360") || 
                        flag.equals("-0-360") || flag.equals("wrap360"))
                {
                    wrap360 = true;
                }
                else if(flag.equals("-data0to180") || flag.equals("-0to180") || 
                        flag.equals("-0-180") || flag.equals("wrap180"))
                {
                    wrap180 = true;
                }
                else if(flag.equals("-scalez"))
                {
                    scaleZ = true;
                    if (param != null)
                        scalingFactorZ = Integer.parseInt(param);
                }
                else if(flag.equals("-dummy-option"))
                {
                    // Do something...
                }
                else
                {
                    System.out.println("Couldn't understand flag: "+flag);   
                }
            }
        }//for(each arg in args)
        
        // Make sure there is a file provided
        if (csv == null)
        {
            System.out.println("Didn't specify an input csv file!");
            System.exit(0);
        }
        
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
    }
    
    // Display help information
    void showHelp()
    {
        
        System.out.println("Usage: java -cp cmdline.CsvToKinner [flags] [filename]"            );
        System.out.println(                                                                    );
        System.out.println("Flags:"                                                            );
        System.out.println("-COLumns=#,#,#,... uses designated columns for points in kinemage" );
        System.out.println("                   default: plot columns 0,1,2 in kin"             );
        System.out.println("                   if less than 3 columns, filled in with zeros"   );
        System.out.println("-DELIMiter=[word]  sets delimiter for reading csv input"           );
        System.out.println("                   default: colon"                                 );
        System.out.println("-KINHEADing        prints @kin header in output"                   );
        System.out.println(                                                                    );
        System.out.println("-data0to180 || -0to180 || -0-180 || wrap180"                       );
        System.out.println("                   wraps data and axes to -180 --> 180"            );
        System.out.println("-data0to360 || -0to360 || -0-360 || wrap360"                       );
        System.out.println("                   wraps data and axes to -360 --> 360"            );
        System.out.println("-scalez=#          subtracts the average Z from each Z point, then");
        System.out.println("                      (opt'l) multiplies by the provided int"      );
        System.out.println(                                                                    );
        System.out.println(                                                                    );
        System.out.println(                                                                    );
        System.out.println("We assume row 0 is headings."                                      );
        System.out.println("Rows and columns measured 0 to n."                                 );
        System.out.println(                                                                    );
    }
//}}}

//{{{ readInput
//##################################################################################################
	private void readInput()
	{
		// Mission: Fill points with n-dimensional arrays from cols[0], cols[1], and cols[2]
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
                    for (int c = 0; c < cols.length; c ++)
                    {
                        // Skip to ith column
                        Scanner lineScan_c = new Scanner(line);
                        lineScan_c.useDelimiter(delimiter);
                        for (int j = 0; j < cols[c]; j++)
                            lineScan_c.next();
                        thisPoint[c] = Double.parseDouble(lineScan_c.next());
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

//{{{ getMaxMin
//##################################################################################################
	private void getMaxMin()
	{
        max = -999999999;
        min = 999999999;
        for (double[] pt : pts)
        {
            for (int i = 0; i < pt.length; i ++)
            {
                if (pt[i] < min)    min = pt[i];
                if (pt[i] > max)    max = pt[i];
            }
        }
        
        // Make sure the axes (which these are used for) will match the wrapping of the data
        if (wrap360)
        {
            min = wrap360(min);
            max = wrap360(max);
        }
        if (wrap180)
        {
            min = wrap180(min);
            max = wrap180(max);
        }
    }
//}}}

//{{{ wrap360, wrap180
//##################################################################################################
	private double wrap360(double angle)
    {
        if(wrap360)
        {
            angle = angle % 360;
            if(angle < 0) return angle + 360;
            else return angle;
        }
        else return angle;
    }

    private double wrap180(double angle)
    {
        if(wrap180)
        {
            // Anything over 180 gets 360 subtracted from it
            // e.g. 210 --> 210 - 360 --> -150
            if(angle > 180) return angle - 360;
            else return angle;
        }
        else return angle;
    }
//}}}

//{{{ getAvgZ
//##################################################################################################
	private double getAvgZ()
	{
        double sum = 0;
        int count = 0;
        for (int i = 0; i < pts.size(); i ++)
        {
            double[] pt = pts.get(i);
            sum += pt[2];
            count ++;
        }
        
        return sum / count;
    }
//}}}

//{{{ printOutput
//##################################################################################################
	private void printOutput()
	{
		// Heading, if desired
        if (kinHeading)
            System.out.println("@kin {"+csvFilename+"}");
        
        // Views and scaling
        System.out.println("@group {frame} dominant ");
        System.out.println("@vectorlist {frame} color= white");
        System.out.println("P "+min+" 0.000 0.000");
        System.out.println("  "+max+" 0.000 0.000");
        System.out.println("P  0.000 "+min+" 0.000");
        System.out.println("   0.000 "+max+" 0.000");
        System.out.println("P  0.000 0.000 "+min);
        System.out.println("   0.000 0.000 "+max);
        System.out.println("@labellist {XYZ} color= white");
        System.out.println("{"+labels[0]+"="+min+"} "+min+" 0 0");  //380.000 -5.000 -5.000");
        System.out.println("{"+labels[0]+"="+max+"} "+max+" 0 0");  //380.000 -5.000 -5.000");
        System.out.println("{"+labels[1]+"="+min+"} 0 "+min+" 0");  //-5.000 380.000 -5.000");
        System.out.println("{"+labels[1]+"="+max+"} 0 "+max+" 0");  //-5.000 380.000 -5.000");
        System.out.println("{"+labels[2]+"="+min+"} 0 0 "+min);     //-5.000 -5.000 380.000");
        System.out.println("{"+labels[2]+"="+max+"} 0 0 "+max);     //-5.000 -5.000 380.000");
        
        // Group
        System.out.print("@group {");
        for (int i = 0; i < cols.length; i ++)
        {
            if (i < cols.length - 1) System.out.print("col"+cols[i]+", ");
            else                   System.out.print("col"+cols[i]);
        }
        System.out.println("} animate dominant");
        
        // Get avg Z for scaling Z to X and Y, if desired
        double avgZ = 0;
        if (scaleZ)
            avgZ = getAvgZ();
        
        // Dotlist
        System.out.println("@dotlist {nuthin'}");
        for (int i = 0; i < pts.size(); i ++)
        {
            double[] pt = pts.get(i);
            System.out.print("{point"+i+"} ");
            for (int c = 0; c < cols.length; c ++)
            {
                // For scaling Z to X and Y, if desired
                // First added for Karplus's PGD bb geometry stuff, to get tau
                // angles on same scale as 0to360 (or -180to180) phi & psi.
                if (scaleZ && c == 2)
                    pt[c] = (pt[c] - avgZ) * scalingFactorZ;
                
                if (wrap360)        System.out.print(wrap360(pt[c])+" ");
                else if (wrap180)   System.out.print(wrap180(pt[c])+" ");
                else                System.out.print(pt[c]+" ");
            }
            System.out.println();
        }
	}
//}}}

} //class