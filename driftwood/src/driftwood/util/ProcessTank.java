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
* <code>ProcessTank</code> is intended to make reading data from
* the standard output and standard error of external processes
* easier and more robust. The idea is for ProcessTank to accumulate
* all the output over the course of the process, storing it in
* internal buffers. After the process has finished, the Java code
* can read and process data at its leisure, without worrying about
* deadlock caused by full buffers.
*
* In the future, this class may also include a runaway process monitor
* that can kill off external processes that don't expire within a given
* window...
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 16 18:51:18 EDT 2003
*/
public class ProcessTank //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Process     process;
    StreamTank  outTank;
    StreamTank  errTank;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor.
    * @param comingle   if true, stdout and stderr will be "interleaved" into
    *   the same holding tank. The time granularity of this operation is not
    *   well defined, and may not produce the expected results (reliably).
    *   Defaults to false.
    */
    public ProcessTank(boolean comingle)
    {
        outTank = new StreamTank();
        
        if(comingle)
            errTank = outTank;
        else
            errTank = new StreamTank();
    }

    public ProcessTank()
    {
        this(false);
    }
//}}}

//{{{ fillTank
//##################################################################################################
    /**
    * Allows the process to run until it has no more data available
    * and the process has terminated (or a timeout has expired).
    * Output is collected and stored in byte buffers which can later
    * be retrieved.
    *
    * @param process    the Process object from which to fill the tank
    * @param timeout    the timeout for the process, in milliseconds.
    *   The process will be given at least timeout millis to terminate normally.
    *   The countdown timer is reset everytime the program produces output.
    *   Defaults to 0 (no timeout criterion will be applied).
    * @param killRunaway    if a timeout was specified, should a Process
    *   that has timed out be forcibly terminated (destroy()ed)? Defaults to true.
    */
    public boolean fillTank(Process process, long timeout, boolean killRunaway) throws IOException
    {
        if(process == null)
            throw new IllegalArgumentException("Must provide a non-null process");
        
        InputStream outStream = process.getInputStream();
        InputStream errStream = process.getErrorStream();
        int outAvail, errAvail;
        byte[] buffer = new byte[2048];
        boolean finished = false;
        long timestamp = System.currentTimeMillis();
        
        while(!finished)
        {
            // Try reading from stdout (without blocking)
            outAvail = outStream.available();
            if(outAvail > 0)
            {
                outAvail = outStream.read(buffer, 0, Math.min(buffer.length, outAvail));
                outTank.write(buffer, 0, outAvail);
            }
            
            // Try reading from stderr (without blocking)
            errAvail = errStream.available();
            if(errAvail > 0)
            {
                errAvail = errStream.read(buffer, 0, Math.min(buffer.length, errAvail));
                errTank.write(buffer, 0, errAvail);
            }
            
            // If no data was read in this pass,
            // try checking the exit code of the process.
            // This will throw an exception if it's still running.
            if(outAvail <= 0 && errAvail <= 0)
            {
                try
                {
                    process.exitValue(); // may throw ITSEx
                    finished = true;
                }
                catch(IllegalThreadStateException ex)
                {
                    // Check for timeout condition -- timeout set, data read, and enough time elapsed
                    if(timeout > 0 && System.currentTimeMillis() - timestamp > timeout)
                    {
                        finished = true;
                        if(killRunaway) process.destroy();
                        return false; // abnormal termination
                    }
                }//try-catch(check exit code)
            }//if(no data read this time)
            else
            { timestamp = System.currentTimeMillis(); }
            
            // Play nice and let other threads run.
            // This may matter if the process isn't running
            // asynchronously alongside the Java code.
            Thread.yield();
        }//while(not finished)
        
        return true; // normal termination
    }
//}}}

//{{{ fillTank (defaults)
//##################################################################################################
    public boolean fillTank(Process process, long timeout) throws IOException
    { return fillTank(process, timeout, true); }

    public boolean fillTank(Process process) throws IOException
    { return fillTank(process, 0L); }
//}}}

//{{{ getStdout, getStderr
//##################################################################################################
    /** Returns a stream for reading from this process's stdout */
    public InputStream getStdout()
    {
        outTank.close();
        return outTank.getInputStream();
    }
    
    /** Returns a stream for reading from this process's stderr */
    public InputStream getStderr()
    {
        errTank.close();
        return errTank.getInputStream();
    }
//}}}

//{{{ stdoutSize, stderrSize
//##################################################################################################
    /** Returns the number of bytes generated by this process */
    public int stdoutSize()
    { return outTank.size(); }

    /** Returns the number of bytes generated by this process */
    public int stderrSize()
    { return errTank.size(); }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

