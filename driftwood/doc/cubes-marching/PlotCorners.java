// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
//package cubes-marching;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>PlotCorners</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Feb  8 08:51:09 EST 2003
*/
public class PlotCorners //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
int[][] marchingCubes;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PlotCorners()
    {
        marchingCubes = createLookupTable();
    }
//}}}

//{{{ createLookupTable
//##################################################################################################
    int[][] createLookupTable()
    {
        int[][] marchingCubes = new int[256][];
        marchingCubes[0] = new int[] {};
        marchingCubes[1] = new int[] {0,3,8,0};
        marchingCubes[2] = new int[] {1,0,9};
        marchingCubes[3] = new int[] {1,3,8,9};
        marchingCubes[4] = new int[] {1,2};
        marchingCubes[5] = new int[] {1,2,-1,0,3,8,0};
        marchingCubes[6] = new int[] {2,0,9};
        marchingCubes[7] = new int[] {9,8,10,-1,2,3,8};
        marchingCubes[8] = new int[] {2,3,11};
        marchingCubes[9] = new int[] {11,8,0,2};
        marchingCubes[10] = new int[] {2,3,11,-1,1,0,9};
        marchingCubes[11] = new int[] {9,8,11,9,-1,1,2};
        marchingCubes[12] = new int[] {1,3,11};
        marchingCubes[13] = new int[] {10,8,11,-1,1,0,8};
        marchingCubes[14] = new int[] {0,3,11,9,0};
        marchingCubes[15] = new int[] {9,8,11};
        marchingCubes[16] = new int[] {4,8,7};
        marchingCubes[17] = new int[] {7,3,0,4};
        marchingCubes[18] = new int[] {1,0,9,-1,4,8,7};
        marchingCubes[19] = new int[] {1,3,7,1,-1,4,9};
        marchingCubes[20] = new int[] {1,2,-1,4,8,7};
        marchingCubes[21] = new int[] {1,2,-1,7,3,0,4};
        marchingCubes[22] = new int[] {2,0,9,-1,4,8,7};
        marchingCubes[23] = new int[] {2,3,7,9,2,7,-1,4,9};
        marchingCubes[24] = new int[] {2,3,11,-1,4,8,7};
        marchingCubes[25] = new int[] {2,0,4,2,-1,7,11};
        marchingCubes[26] = new int[] {1,0,9,-1,11,3,2,-1,7,8,4};
        marchingCubes[27] = new int[] {1,2,4,9,-1,7,11};
        marchingCubes[28] = new int[] {1,3,11,-1,4,8,7};
        marchingCubes[29] = new int[] {11,7,0,10,7,-1,4,0,1};
        marchingCubes[30] = new int[] {0,3,11,9,0,-1,4,8,7};
        marchingCubes[32] = new int[] {4,9};
        marchingCubes[33] = new int[] {0,3,8,0,-1,4,9};
        marchingCubes[34] = new int[] {1,0,4};
        marchingCubes[35] = new int[] {1,3,5,-1,4,8,3};
        marchingCubes[36] = new int[] {1,2,-1,4,9};
        marchingCubes[37] = new int[] {1,2,-1,4,9,-1,0,3,8,0};
        marchingCubes[38] = new int[] {0,2,4,0};
        marchingCubes[39] = new int[] {2,4,8,3,2};
        marchingCubes[40] = new int[] {2,3,11,-1,4,9};
        marchingCubes[41] = new int[] {2,0,8,11,-1,4,9};
        marchingCubes[42] = new int[] {2,3,11,-1,1,0,4};
        marchingCubes[43] = new int[] {4,8,11,-1,1,2,5,8,2};
        marchingCubes[44] = new int[] {1,3,11,-1,4,9};
        marchingCubes[45] = new int[] {11,8,10,-1,1,0,8,-1,4,9};
        marchingCubes[46] = new int[] {11,3,0,4,10,3,4};
        marchingCubes[48] = new int[] {7,8,9};
        marchingCubes[49] = new int[] {7,3,5,-1,9,0,3};
        marchingCubes[50] = new int[] {0,1,7,8,0};
        marchingCubes[51] = new int[] {1,3,7};
        marchingCubes[52] = new int[] {1,2,-1,7,8,9};
        marchingCubes[53] = new int[] {7,3,5,-1,9,0,3,-1,1,2};
        marchingCubes[54] = new int[] {2,0,8,7,10,0,7};
        marchingCubes[56] = new int[] {7,8,9,-1,2,3,11};
        marchingCubes[57] = new int[] {11,7,9,2,7,-1,9,0,2};
        marchingCubes[58] = new int[] {0,1,7,8,0,-1,2,3,11};
        marchingCubes[60] = new int[] {7,8,9,-1,1,3,11};
        marchingCubes[64] = new int[] {};
        marchingCubes[65] = new int[] {0,3,8,0};
        marchingCubes[66] = new int[] {1,0,9};
        marchingCubes[67] = new int[] {1,3,8,9};
        marchingCubes[68] = new int[] {1,2};
        marchingCubes[69] = new int[] {1,2,-1,0,3,8,0};
        marchingCubes[70] = new int[] {2,0,6,-1,0,9};
        marchingCubes[71] = new int[] {2,3,8,9,6,3,9};
        marchingCubes[72] = new int[] {2,3,11};
        marchingCubes[73] = new int[] {2,0,8,11};
        marchingCubes[74] = new int[] {1,0,9,-1,2,3,11};
        marchingCubes[75] = new int[] {8,9,11,8,-1,1,2};
        marchingCubes[76] = new int[] {1,3,5,-1,3,11};
        marchingCubes[77] = new int[] {11,8,0,1,6,8,1};
        marchingCubes[78] = new int[] {9,0,3,11,9};
        marchingCubes[80] = new int[] {4,8,7};
        marchingCubes[81] = new int[] {4,0,3,7};
        marchingCubes[82] = new int[] {1,0,9,-1,4,8,7};
        marchingCubes[83] = new int[] {1,3,7,1,-1,4,9};
        marchingCubes[84] = new int[] {1,2,-1,4,8,7};
        marchingCubes[85] = new int[] {1,2,-1,4,0,3,7};
        marchingCubes[86] = new int[] {2,0,6,-1,0,9,-1,4,8,7};
        marchingCubes[88] = new int[] {2,3,11,-1,4,8,7};
        marchingCubes[89] = new int[] {0,2,4,0,-1,7,11};
        marchingCubes[90] = new int[] {4,8,7,-1,2,3,11,-1,1,0,9};
        marchingCubes[92] = new int[] {1,3,5,-1,3,11,-1,4,8,7};
        marchingCubes[96] = new int[] {4,9};
        marchingCubes[97] = new int[] {4,9,-1,0,3,8,0};
        marchingCubes[98] = new int[] {4,0,6,-1,0,1};
        marchingCubes[99] = new int[] {1,3,8,4,10,3,4};
        marchingCubes[100] = new int[] {1,2,4,9};
        marchingCubes[101] = new int[] {1,2,4,9,-1,0,3,8,0};
        marchingCubes[102] = new int[] {2,0,4};
        marchingCubes[104] = new int[] {4,9,-1,2,3,11};
        marchingCubes[105] = new int[] {4,9,-1,2,0,8,11};
        marchingCubes[106] = new int[] {4,0,6,-1,0,1,-1,2,3,11};
        marchingCubes[108] = new int[] {9,4,11,1,4,-1,11,3,1};
        marchingCubes[112] = new int[] {9,8,10,-1,7,8};
        marchingCubes[113] = new int[] {9,0,3,7,10,0,7,10};
        marchingCubes[114] = new int[] {1,0,8,7,1};
        marchingCubes[116] = new int[] {9,8,7,-1,2,1,8,6,1};
        marchingCubes[120] = new int[] {2,3,11,-1,9,8,10,-1,7,8};
        marchingCubes[128] = new int[] {7,11};
        marchingCubes[129] = new int[] {7,11,-1,0,3,8,0};
        marchingCubes[130] = new int[] {7,11,-1,1,0,9};
        marchingCubes[131] = new int[] {1,3,8,9,-1,7,11};
        marchingCubes[132] = new int[] {7,11,-1,1,2};
        marchingCubes[133] = new int[] {7,11,-1,1,2,-1,0,3,8,0};
        marchingCubes[134] = new int[] {2,0,9,-1,11,7};
        marchingCubes[136] = new int[] {2,3,7};
        marchingCubes[137] = new int[] {2,0,6,-1,7,8,0};
        marchingCubes[138] = new int[] {2,3,7,-1,1,0,9};
        marchingCubes[140] = new int[] {1,3,7,1};
        marchingCubes[144] = new int[] {4,8,11};
        marchingCubes[145] = new int[] {4,0,6,-1,0,3,11};
        marchingCubes[146] = new int[] {4,8,11,-1,1,0,9};
        marchingCubes[148] = new int[] {4,8,11,-1,1,2};
        marchingCubes[152] = new int[] {2,3,8,4,2};
        marchingCubes[160] = new int[] {7,11,-1,4,9};
        marchingCubes[161] = new int[] {7,11,-1,4,9,-1,0,3,8,0};
        marchingCubes[162] = new int[] {1,0,4,-1,7,11};
        marchingCubes[164] = new int[] {1,2,-1,4,9,-1,7,11};
        marchingCubes[168] = new int[] {2,3,7,-1,4,9};
        marchingCubes[176] = new int[] {8,9,11,8};
        marchingCubes[192] = new int[] {7,11};
        marchingCubes[193] = new int[] {7,11,-1,0,3,8,0};
        marchingCubes[194] = new int[] {7,11,-1,1,0,9};
        marchingCubes[196] = new int[] {2,1,7,11};
        marchingCubes[200] = new int[] {5,3,7,-1,2,3};
        marchingCubes[208] = new int[] {10,8,11,-1,4,8};
        marchingCubes[224] = new int[] {4,9,11,7};
        
        for(int i = 0; i < 256; i++)
        {
            if(marchingCubes[i] == null)
                marchingCubes[i] = marchingCubes[i ^ 0xff];
        }
        
        return marchingCubes;
    }
//}}}

//{{{ countBits
//##################################################################################################
    /** Counts the number of bits set in the low byte */
    int countBits(int mask)
    {
        int i, result = 0;
        for(i = 0; i < 8; i++)
        {
            if((mask & (1 << i)) != 0) result++;
        }
        return result;
    }
//}}}

//{{{ plotMask
//##################################################################################################
    /** Plots one ball for each bit set in mask */
    void plotMask(int mask)
    {
        System.out.println("@group {"+mask+"} dominant animate");
        System.out.println("@balllist {"+mask+"} color= red radius= 3.0");
        if((mask & 0x01) != 0) System.out.println("{v0} 0 0 0");
        if((mask & 0x02) != 0) System.out.println("{v1} 100 0 0");
        if((mask & 0x04) != 0) System.out.println("{v2} 100 100 0");
        if((mask & 0x08) != 0) System.out.println("{v3} 0 100 0");
        if((mask & 0x10) != 0) System.out.println("{v4} 0 0 100");
        if((mask & 0x20) != 0) System.out.println("{v5} 100 0 100");
        if((mask & 0x40) != 0) System.out.println("{v6} 100 100 100");
        if((mask & 0x80) != 0) System.out.println("{v7} 0 100 100");
    }
//}}}

//{{{ plotEdges
//##################################################################################################
    void plotEdges(int[] e)
    {
        System.out.println("@vectorlist {edges} color= cyan");
        boolean drawline = false;
        for(int i = 0; i < e.length; i++)
        {
            if(e[i] == -1) drawline = false;
            else
            {
                if(drawline)    System.out.print("{x} ");
                else          { System.out.print("{x}P "); drawline = true; }
                switch(e[i])
                {
                    case 0: System.out.println("50 0 0"); break;
                    case 1: System.out.println("100 50 0"); break;
                    case 2: System.out.println("50 100 0"); break;
                    case 3: System.out.println("0 50 0"); break;
                    case 4: System.out.println("50 0 100"); break;
                    case 5: System.out.println("100 50 100"); break;
                    case 6: System.out.println("50 100 100"); break;
                    case 7: System.out.println("0 50 100"); break;
                    case 8: System.out.println("0 0 50"); break;
                    case 9: System.out.println("100 0 50"); break;
                    case 10: System.out.println("100 100 50"); break;
                    case 11: System.out.println("0 100 50"); break;
                }
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Main() and main()
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        int bitcount;
        
        System.out.println("@kinemage 1");
        for(int mask = 0; mask < 256; mask++)
        {
            bitcount = countBits(mask);
            if(bitcount < 4 || (bitcount == 4 && mask < (mask ^ 0xff)))
            {
                plotMask(mask);
                plotEdges(marchingCubes[mask]);
                //System.err.println("marchingCubes["+mask+"] = new int[] {};");
            }
        }
    }

    public static void main(String[] args)
    {
        PlotCorners mainprog = new PlotCorners();
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
    }
//}}}

//{{{ Argument parsing, etc.
//##################################################################################################
    /**
    * Parse the command-line options for this program, separating out parameters from flags.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        /* Load properties from file * /
        // System properties from JAR file -- must be present
        Props props = new Props();
        InputStream is = getClass().getResourceAsStream("/rc/PlotCorners.props");
        try { props.load(is); } catch(IOException ex) {ex.printStackTrace();}
        // User properties from home dir -- may be present
        File userfile = new File(System.getProperty("user.home")+System.getProperty("file.separator")+".PlotCorners.props");
        // Load user properties from file, if found
        if(userfile.exists())
        {
            try {
                is = new FileInputStream(userfile);
                props.load(is);
                System.err.println("Found user properties in '"+userfile+"'");
            } catch(IOException ex) {ex.printStackTrace();}
        }
        // User properties from current dir -- may be present
        File localfile = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+"PlotCorners.props");
        // Load user properties from file, if found
        if(localfile.exists())
        {
            try {
                is = new FileInputStream(localfile);
                props.load(is);
                System.err.println("Found user properties in '"+localfile+"'");
            } catch(IOException ex) {ex.printStackTrace();}
        }
        /* Load properties from file */
        
        
        // Parse argument array into a linked list,
        // converting "-flag=param" to "-flag" "param"
        LinkedList arglist = new LinkedList();
        
        int iEquals;
        boolean interpFlags = true;
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("--"))
            {
                interpFlags = false;
                arglist.add(args[i]);
            }
            else if(args[i].startsWith("-") && interpFlags)
            {
                iEquals = args[i].indexOf('=');
                if(iEquals != -1)
                {
                    arglist.add( args[i].substring(0,iEquals) );
                    arglist.add( args[i].substring(iEquals+1) );
                }
                else arglist.add(args[i]);
            }
            else arglist.add(args[i]);
        }
        
        ListIterator iter = arglist.listIterator();
        try
        {
            //interpretArguments(iter, props);
            interpretArguments(iter);
        }
        catch(NoSuchElementException ex)
        {
            throw new IllegalArgumentException("'"+iter.previous()+"' expected an argument");
        }
    }

    // Get an integer from the command line argument list
    int getInt(Iterator iter)
    {
        String param = iter.next().toString();
        try { return Integer.parseInt(param); }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("'"+param+"' is not an integer"); }
    }
    
    // Get a double from the command line argument list
    double getDouble(Iterator iter)
    {
        String param = iter.next().toString();
        try { return Double.parseDouble(param); }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("'"+param+"' is not a real number"); }
    }

    // Get a reader for a file name, dash (-) for stdin
    static private LineNumberReader SYSTEM_IN_READER = null;
    LineNumberReader getReader(String filename)
    {
        LineNumberReader r = null;
        if(filename.equals("-"))
        {
            if(SYSTEM_IN_READER == null) SYSTEM_IN_READER = new LineNumberReader(new InputStreamReader(System.in));
            r = SYSTEM_IN_READER;
        }
        else
        {
            try { r = new LineNumberReader(new FileReader(filename)); }
            catch(IOException ex)
            { throw new IllegalArgumentException("'"+filename+"' is not a readable file or stream."); }
        }
        return r;
    }

    // Get a writer for a file name, dash (-) for stdout
    static private PrintWriter SYSTEM_OUT_WRITER = null;
    PrintWriter getWriter(String filename)
    {
        PrintWriter w = null;
        if(filename.equals("-"))
        {
            if(SYSTEM_OUT_WRITER == null) SYSTEM_OUT_WRITER = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
            w = SYSTEM_OUT_WRITER;
        }
        else
        {
            try { w = new PrintWriter(new BufferedWriter(new FileWriter(filename))); }
            catch(IOException ex)
            { throw new IllegalArgumentException("'"+filename+"' is not a writable file or stream."); }
        }
        return w;
    }

    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("/rc/PlotCorners.help");
            if(is == null) System.err.println("\n*** Unable to locate help information in '/rc/PlotCorners.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        //System.err.println("cubes-marching.PlotCorners, version "+Version.VERSION+" (build "+Version.BUILD+")");
        System.err.println("Copyright (C) 2003 by Ian W. Davis. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ Argument interpretation
//##################################################################################################
    //void interpretArguments(Iterator iter, Props props)
    void interpretArguments(Iterator iter)
    {
        String arg;
        boolean interpFlags = true;
        ArrayList files = new ArrayList();
        
        while(iter.hasNext())
        {
            arg = iter.next().toString();
            if(!arg.startsWith("-") || !interpFlags || arg.equals("-"))
            {
                files.add(arg);
            }
            else if (arg.equals("--")) interpFlags = false;
            else if(arg.equals("-help") || arg.equals("-h")) {
                showHelp(true);
                System.exit(0);
            } else if(arg.equals("-dummy_option")) {
                // handle option here
            } else {
                throw new IllegalArgumentException("'"+arg+"' is not recognized as a valid argument.");
            }
        }
        
        // Post-processing, adding things to props, etc.
    }
//}}}
}//class

