// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
//package quickies;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>ArrayLookup</code> is intended to test the relative speed
* of several different methods of looking up items in an array.
* <p>The results:
* <ul>
* <li>Direct array access is by far the fastest. It may be up to
*   40 times faster than using a Collection (and at least 6 times).
* <li>All the Collections are within a factor of 3 or so.</li>
*   ArrayList is the fastest, and LinkedHashMap is the slowest.
* <li>You pay a 2.3 times penalty (130%) for arbitrary String indices
*   as opposed to integer indices.</li>
*   Note that in this example, there is strict equality of the
*   search key and the found key. This speeds up String.equals().
* <li>You pay a 2.9 times penalty (190%) for using LinkedHashMap
*   instead of ArrayList. You pay a 25% penalty for adding the
*   linked list capability to a basic HashMap.</li>
* <li>For painting 100,000 points, using a HashMap to lookup color
*   would contribute about 10 msec to the render time.</li>
* <li>Tests done with Sun's Java 1.4.2-beta.</li>
* </ul>
* <p>
<pre>
WITH 10x LOOP UNROLLING
======================
For class [Ljava.lang.String;, 83886080 accesses took 79 ms.
For class java.util.ArrayList, 83886080 accesses took 3355 ms.
For class java.util.HashMap, 83886080 accesses took 7881 ms.
For class java.util.LinkedHashMap, 83886080 accesses took 9666 ms.

For class [Ljava.lang.String;, 83886080 accesses took 79 ms.
For class java.util.ArrayList, 83886080 accesses took 3367 ms.
For class java.util.HashMap, 83886080 accesses took 7867 ms.
For class java.util.LinkedHashMap, 83886080 accesses took 9648 ms.

For class [Ljava.lang.String;, 83886080 accesses took 81 ms.
For class java.util.ArrayList, 83886080 accesses took 3358 ms.
For class java.util.HashMap, 83886080 accesses took 7875 ms.
For class java.util.LinkedHashMap, 83886080 accesses took 9642 ms.


WITHOUT LOOP UNROLLING
======================
For class [Ljava.lang.String;, 67108864 accesses took 509 ms.
For class java.util.ArrayList, 67108864 accesses took 3026 ms.
For class java.util.HashMap, 67108864 accesses took 6817 ms.
For class java.util.LinkedHashMap, 67108864 accesses took 8285 ms.

For class [Ljava.lang.String;, 67108864 accesses took 495 ms.
For class java.util.ArrayList, 67108864 accesses took 3024 ms.
For class java.util.HashMap, 67108864 accesses took 6827 ms.
For class java.util.LinkedHashMap, 67108864 accesses took 8286 ms.

For class [Ljava.lang.String;, 67108864 accesses took 495 ms.
For class java.util.ArrayList, 67108864 accesses took 3009 ms.
For class java.util.HashMap, 67108864 accesses took 6796 ms.
For class java.util.LinkedHashMap, 67108864 accesses took 8214 ms.

For class [Ljava.lang.String;, 67108864 accesses took 501 ms.
For class java.util.ArrayList, 67108864 accesses took 2970 ms.
For class java.util.HashMap, 67108864 accesses took 6838 ms.
For class java.util.LinkedHashMap, 67108864 accesses took 8236 ms.
</pre>
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 25 09:48:19 EDT 2003
*/
public class ArrayLookup //extends ... implements ...
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
    public ArrayLookup()
    {
        super();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        int i;
        long time;
        Object o;
        int index = 2;
        String key = "green";
        final int reps = 1 << 23;
        
        String[]    array   = new String[] { "red", "yellow", "green", "blue", "turquoise", "indigo", "cerulean", "aqua", "cornflower", "navy", "azure" };
        List        list    = new ArrayList();
        Map         map     = new HashMap();
        Map         lmap    = new LinkedHashMap();
        
        for(i = 0; i < array.length; i++)
        {
            list.add(array[i]);
            map.put(array[i], array[i]);
            lmap.put(array[i], array[i]);
        }
        
        System.out.println("Starting access tests with "+(reps*10)+" access attempts.");
        
        // Loops have been unrolled to de-emphasize overhead from looping.
        time = System.currentTimeMillis();
        for(i = 0; i < reps; i++)
        {
            o = array[index];
            o = array[index];
            o = array[index];
            o = array[index];
            o = array[index];
            o = array[index];
            o = array[index];
            o = array[index];
            o = array[index];
            o = array[index];
        }
        time = System.currentTimeMillis() - time;
        System.out.println("For "+array.getClass()+", "+(reps*10)+" accesses took "+time+" ms.");
        
        time = System.currentTimeMillis();
        for(i = 0; i < reps; i++)
        {
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
            o = list.get(index);
        }
        time = System.currentTimeMillis() - time;
        System.out.println("For "+list.getClass()+", "+(reps*10)+" accesses took "+time+" ms.");
        
        time = System.currentTimeMillis();
        for(i = 0; i < reps; i++)
        {
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
            o = map.get(key);
        }
        time = System.currentTimeMillis() - time;
        System.out.println("For "+map.getClass()+", "+(reps*10)+" accesses took "+time+" ms.");
        
        time = System.currentTimeMillis();
        for(i = 0; i < reps; i++)
        {
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
            o = lmap.get(key);
        }
        time = System.currentTimeMillis() - time;
        System.out.println("For "+lmap.getClass()+", "+(reps*10)+" accesses took "+time+" ms.");
    }

    public static void main(String[] args)
    {
        ArrayLookup mainprog = new ArrayLookup();
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

//{{{ parseArguments, showHelp
//##################################################################################################
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
                { throw new IllegalArgumentException("'"+arg+"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("ArrayLookup.help");
            if(is == null) System.err.println("\n*** Unable to locate help information in 'ArrayLookup.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("quickies.ArrayLookup");
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

//{{{ interpretArg, interpretFlag
//##################################################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

