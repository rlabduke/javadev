// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
//package quickies;

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
 * <code>MemAlloc</code> has not yet been documented.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Tue Nov 12 13:09:29 EST 2002
*/
public class MemAlloc //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public MemAlloc()
    {
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ main() and Main()
//##################################################################################################
    public static void main(String[] args)
    {
        MemAlloc mainprog = new MemAlloc();
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

    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        int i, size = 1000000;
        long mem;
        float each;
        Runtime rt = Runtime.getRuntime();
        Object[] objs = new Object[size];
        
        mem = rt.totalMemory() - rt.freeMemory();
        for(i = 0; i < size; i++) objs[i] = new byte[0];
        mem = rt.totalMemory() - rt.freeMemory() - mem;
        
        each = (float)mem / size;
        System.out.println("Each object used "+each+" bytes.");        
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
        InputStream is = getClass().getResourceAsStream("/rc/MemAlloc.props");
        try { props.load(is); } catch(IOException ex) {ex.printStackTrace();}
        // User properties from home dir -- may be present
        File userfile = new File(System.getProperty("user.home")+System.getProperty("file.separator")+".MemAlloc.props");
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
        File localfile = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+"MemAlloc.props");
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
            InputStream is = getClass().getResourceAsStream("/rc/MemAlloc.help");
            if(is == null) System.err.println("\n*** Unable to locate help information in '/rc/MemAlloc.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        //System.err.println("quickies.MemAlloc, version "+Version.VERSION+" (build "+Version.BUILD+")");
        System.err.println("Copyright (C) 2002 by Ian W. Davis. All rights reserved.");
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

