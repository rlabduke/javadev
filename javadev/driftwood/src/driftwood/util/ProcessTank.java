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
    * @return true if the process exited on its own; false if it was killed forcibly.
    */
    public boolean fillTank(Process process, long timeout, boolean killRunaway) throws IOException
    {
        long enterTime, exitTime, lastReadTime, lastExitCheckTime;
        int outReadCount = 0, errReadCount = 0, exitCheckCount = 0;
        
        InputStream outStream = process.getInputStream();
        InputStream errStream = process.getErrorStream();
        int outAvail, errAvail;
        byte[] buffer = new byte[32*1024];
        boolean finished = false;
        boolean returnCode = true; // assume normal termination
        enterTime = lastReadTime = lastExitCheckTime = System.currentTimeMillis();
        
        while(!finished)
        {
            // Try reading from stdout (without blocking)
            outAvail = outStream.available();
            if(outAvail > 0)
            {
                //System.err.println("Reading!");
                outAvail = outStream.read(buffer, 0, Math.min(buffer.length, outAvail));
                outTank.write(buffer, 0, outAvail);
                outReadCount++;
            }
            
            // Try reading from stderr (without blocking)
            errAvail = errStream.available();
            if(errAvail > 0)
            {
                errAvail = errStream.read(buffer, 0, Math.min(buffer.length, errAvail));
                errTank.write(buffer, 0, errAvail);
                errReadCount++;
            }
            
            // If no data was read in this pass, try checking the exit code of the process.
            // This will throw an exception if the process is still running.
            // Thus we check the exit code a maximum of once every 50 ms, to avoid swamping the system.
            long currTime = System.currentTimeMillis();
            long sinceLastExitCheck = currTime - lastExitCheckTime;
            if(outAvail <= 0 && errAvail <= 0)
            {
                if(sinceLastExitCheck >= 50)
                {
                    try
                    {
                        lastExitCheckTime = currTime;
                        exitCheckCount++;
                        process.exitValue(); // may throw ITSEx
                        finished = true;
                    }
                    catch(IllegalThreadStateException ex)
                    {
                        // Check for timeout condition -- timeout set, data read, and enough time elapsed
                        long sinceLastRead = currTime - lastReadTime;
                        if(timeout > 0 && sinceLastRead > timeout)
                        {
                            finished = true;
                            if(killRunaway) process.destroy();
                            returnCode = false; // abnormal termination
                            break;
                        }
                    }//try-catch(check exit code)
                }
                else
                {
                    // No data read this pass, but we checked the exit code recently.
                    // Sleep for a while and try again later.
                    //System.err.println("Sleeping...");
                    try { Thread.sleep(50); }
                    catch(InterruptedException ex) {}
                }
            }//if(no data read this time)
            else
            { lastReadTime = currTime; }
            
            // Play nice and let other threads run.
            // This may matter if the process isn't running
            // asynchronously alongside the Java code.
            Thread.yield();
        }//while(not finished)
        
        exitTime = System.currentTimeMillis();
        //System.err.println("exit("+exitTime+") - enter("+enterTime+") = "+(exitTime-enterTime)+" ms ellapsed");
        //System.err.println("stdout reads: "+outReadCount+"    stderr reads: "+errReadCount+"    exit checks: "+exitCheckCount);
        
        return returnCode;
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

