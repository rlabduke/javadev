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
* <code>PlotFaces</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Feb  8 08:51:09 EST 2003
*/
public class PlotFaces //extends ... implements ...
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
    public PlotFaces()
    {
        marchingCubes = createLookupTable();
    }
//}}}

//{{{ createLookupTable
//##################################################################################################
    int[][] createLookupTable()
    {
        int[][] marchingCubes = new int[256][];
        // The 15 exemplars
        // Contents are edge indexes, and -1 means a break
        marchingCubes[0] = new int[] {};
        marchingCubes[1] = new int[] {0,8,3};
        marchingCubes[3] = new int[] {9,8,1,3};
        marchingCubes[5] = new int[] {0,8,3,-1,2,10,1};
        marchingCubes[65] = new int[] {0,8,3,-1,10,6,5};
        marchingCubes[50] = new int[] {0,8,1,7,5};
        marchingCubes[67] = new int[] {9,8,1,3,-1,10,6,5};
        marchingCubes[74] = new int[] {3,2,11,-1,10,6,5,-1,0,1,9};
        marchingCubes[51] = new int[] {1,3,5,7};
        marchingCubes[177] = new int[] {9,5,0,6,3,11};
        marchingCubes[105] = new int[] {0,8,2,11,-1,9,4,10,6};
        marchingCubes[113] = new int[] {3,7,0,10,9,-1,7,10,6};
        marchingCubes[58] = new int[] {3,2,11,-1,0,8,1,7,5};
        marchingCubes[165] = new int[] {0,8,3,-1,6,7,11,-1,1,2,10,-1,4,5,9};
        marchingCubes[178] = new int[] {8,0,11,5,6,-1,0,1,5};
        
        for(int i = 0; i < 256; i++)
        {
            if(countBits(i) <= 4 && marchingCubes[i] == null)
                marchingCubes[i] = findRotated(i, marchingCubes);
            //else catch it below.
        }
        
        for(int i = 0; i < 256; i++)
        {
            if(marchingCubes[i] == null)
                marchingCubes[i] = marchingCubes[i ^ 0xff];
            if(marchingCubes[i] == null)
                throw new Error("Couldn't generate edges for "+i);
            
            if(i < 128)
            {
                System.err.print("marchingCubes["+i+"] = new int[] {");
                for(int j = 0; j < marchingCubes[i].length; j++)
                {
                    if(j > 0) System.err.print(",");
                    System.err.print(marchingCubes[i][j]);
                }
                System.err.println("};");
            }
        }
        
        return marchingCubes;
    }
//}}}

//{{{ findRotated
//##################################################################################################
    int[] findRotated(int origIndex, int[][] exemplars)
    {
        // Unpack origIndex into an array of 0 / 1 values
        // These will be "rotated" to give new indices
        int[] rotatedVertices = new int[8];
        for(int i = 0; i < 8; i++) rotatedVertices[i] = ((origIndex & (1<<i)) == 0 ? 0 : 1);
        
        // Create array of edge indices. These will be rotated in the same manner,
        // allowing us to match a nominal edge (key) to a real edge (value)
        int[] rotatedEdges = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        
        // Create our rotation "matrices". The element currently numbered [key] is remapped to [value].
        int[][] vertRot = {
            {4,0,3,7,5,1,2,6},  // rotation +90 Y
            {3,2,6,7,0,1,5,4},  // rotation +90 X
            {1,2,3,0,5,6,7,4}   // rotation +90 Z
        };
        int[][] edgeRot = {
            {8,3,11,7,9,1,10,5,4,0,2,6},    // rotation +90 Y
            {2,10,6,11,0,9,4,8,3,1,5,7},    // rotation +90 X
            {1,2,3,0,5,6,7,4,9,10,11,8}     // rotation +90 Z
        };
        
        int newIndex = 0;
        do {
            // Make a random rotation
            int which = (int)(3*Math.random());
            rotatedVertices = rotate(rotatedVertices, vertRot[which]);
            rotatedEdges    = rotate(rotatedEdges, edgeRot[which]);
            // Pack the vertices into newIndex
            newIndex = 0;
            for(int i = 0; i < 8; i++) newIndex += (rotatedVertices[i] == 0 ? 0 : (1<<i));
        } while(exemplars[newIndex] == null);
        //System.err.println("Index "+origIndex+" is like index "+newIndex);
        
        // Figure out how the known exemplar edges match to our edges.
        int[] exemplar = exemplars[newIndex];
        int[] newEdges = new int[ exemplar.length ];
        for(int i = 0; i < exemplar.length; i++)
        {
            if(exemplar[i] == -1)
                newEdges[i] = -1;
            else
                newEdges[i] = rotatedEdges[ exemplar[i] ];
        }
        
        return newEdges;
    }
    
    int[] rotate(int[] src, int[] guide)
    {
        int[] dst = new int[src.length];
        for(int i = 0; i < src.length; i++)
        {
            dst[ guide[i] ] = src[i];
        }
        return dst;
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
        System.out.println("@trianglelist {faces} color= cyan alpha= 0.35");
        for(int i = 0; i < e.length; i++)
        {
            if(e[i] == -1) // start a new list
                System.out.println("@trianglelist {faces} color= cyan alpha= 0.35");
            else
            {
                System.out.print("{x} ");
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
            plotMask(mask);
            plotEdges(marchingCubes[mask]);
        }
    }

    public static void main(String[] args)
    {
        PlotFaces mainprog = new PlotFaces();
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
        InputStream is = getClass().getResourceAsStream("/rc/PlotFaces.props");
        try { props.load(is); } catch(IOException ex) {ex.printStackTrace();}
        // User properties from home dir -- may be present
        File userfile = new File(System.getProperty("user.home")+System.getProperty("file.separator")+".PlotFaces.props");
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
        File localfile = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+"PlotFaces.props");
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
            InputStream is = getClass().getResourceAsStream("/rc/PlotFaces.help");
            if(is == null) System.err.println("\n*** Unable to locate help information in '/rc/PlotFaces.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        //System.err.println("cubes-marching.PlotFaces, version "+Version.VERSION+" (build "+Version.BUILD+")");
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

