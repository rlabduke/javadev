// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

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
* <code>SoftLog</code> provides a wrapper for any output stream
* that caches what's written in a memory-sensitive fashion,
* so that e.g. error messages can be reviewed later but won't
* cause an OutOfMemoryException if there are a lot of them.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Sep 17 11:32:02 EDT 2003
*/
public class SoftLog extends PrintStream
{
//{{{ Constants
    /** A SoftLog that goes to System.out */
    static public final SoftLog out = SoftLog.create(System.out);
    /** A SoftLog that goes to System.err */
    static public final SoftLog err = SoftLog.create(System.err);
//}}}

//{{{ Variable definitions
//##############################################################################
    OutputStreamTee     splitter;
    SoftOutputStream    backlog;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Use this instead of the constructor. */
    static public SoftLog create(OutputStream out)
    {
        SoftOutputStream    backlog     = new SoftOutputStream();
        OutputStreamTee     splitter    = new OutputStreamTee(out, backlog);
        return new SoftLog(splitter, backlog);
    }
    
    private SoftLog(OutputStreamTee s, SoftOutputStream b)
    {
        super(s);
        splitter    = s;
        backlog     = b;
    }
//}}}

//{{{ getString, clear, replaceSystemStreams
//##############################################################################
    /** Retrieves cached content as a String. */
    public String getString()
    { return backlog.getString(); }
    
    /** Clears cached content. */
    public void clear()
    { backlog.clear(); }
    
    /**
    * Replaces System.out and System.err with
    * SoftLog.out and SoftLog.err, respectively.
    * @return true if successful, false if a SecurityException prevented it.
    */
    static public boolean replaceSystemStreams()
    {
        try
        {
            System.setOut(out);
            System.setErr(err);
            return true;
        }
        catch(SecurityException ex)
        {
            return false;
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

