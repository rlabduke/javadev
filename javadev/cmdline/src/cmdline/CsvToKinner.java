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
* a kinemage that plots the values in the 3 columns specified by the
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
    ArrayList<Triple> pts;
    String delimiter;           // ":", ",", etc.
    boolean kinHeading;
//}}}

//{{{ main
//##################################################################################################
	public static void main(String[] args)
	{
		CsvToKinner ctk = new CsvToKinner();
		ctk.parseArgs(args);
        ctk.readInput();
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
        pts = new ArrayList<Triple>();
        String delimiter = ":";
        
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
                        int col0 = Integer.parseInt(s.next());
                        int col1 = Integer.parseInt(s.next());
                        int col2 = Integer.parseInt(s.next());
                        cols[0] = col0;
                        cols[1] = col1;
                        cols[2] = col2;
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
    }
    
    // Display help information
    void showHelp()
    {
        
        System.out.println("Usage: java -cp cmdline.CsvToKinner [flags] [filename]" );
        System.out.println(                                                         );
        System.out.println("Flags:"                                                 );
        System.out.println("-COLumns=#,#,#    uses designated columns for "         );
        System.out.println("                     points in kinemage"                );
        System.out.println("                  default: plot columns 0,1,2 in kin"   );
        System.out.println("-DELIMiter=[word] sets delimiter for reading csv input" );
        System.out.println("                  default: colon" );
        System.out.println("-KINHEADing       prints @kin header in output"         );
        System.out.println(                                                         );
        System.out.println("We assume row 1 is headings."                           );
        System.out.println("Rows and columns measured 0 to n."                      );
    }
//}}}

//{{{ readInput
//##################################################################################################
	private void readInput()
	{
		// Mission: Fill points with Triples from cols[0], cols[1], and cols[2]
        try
        {
            Scanner fileScan = new Scanner(csv);
            boolean firstLine = true;
            double pt0, pt1, pt2;
            while (fileScan.hasNextLine())
            {
                String line = fileScan.nextLine();
                if (!firstLine)
                {
                    // Skip to first column
                    Scanner lineScan0 = new Scanner(line);
                    lineScan0.useDelimiter(delimiter);
                    for (int i = 0; i < cols[0]; i++)
                        lineScan0.next();
                    pt0 = Double.parseDouble(lineScan0.next());
                    
                    // Skip to second column
                    Scanner lineScan1 = (new Scanner(line)).useDelimiter(delimiter);
                    for (int i = 0; i < cols[1]; i++)
                        lineScan1.next();
                    pt1 = Double.parseDouble(lineScan1.next());                
                    
                    // Skip to third column
                    Scanner lineScan2 = (new Scanner(line)).useDelimiter(delimiter);
                    for (int i = 0; i < cols[2]; i++)
                        lineScan2.next();
                    pt2 = Double.parseDouble(lineScan2.next());
                    
                    pts.add(new Triple(pt0, pt1, pt2));
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

//{{{ printOutput
//##################################################################################################
	private void printOutput()
	{
		if (kinHeading)
            System.out.println("@kin {"+csvFilename+"}");
        System.out.println("@group {col"+cols[0]+", col"+cols[1]+", col"+cols[2]+
            "} animate dominant");
        System.out.println("@dotlist {nuthin'}");
        for (int i = 0; i < pts.size(); i ++)
        {
            Triple pt = pts.get(i);
            System.out.println("{point"+i+"} "+pt.getX()+" "+pt.getY()+" "+pt.getZ());
        }
	}
//}}}

} //class