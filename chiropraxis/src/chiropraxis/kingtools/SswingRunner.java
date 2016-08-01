// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
import king.*;
import king.core.*;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.util.*;


import driftwood.data.*;
import driftwood.util.ReflectiveRunnable;

//}}}
/**
* <code>SswingRunner</code> runs SSWING as a background job and then creates
* a UI to potentially modify the result, using a SidechainSswing window.
*
* <p>Each instance of this class creates a background thread, which dies off
* when SSWING is finished running.
*
* <p>This class is based on BgKinRunnner, so the structure is a little weird
* because this task is a little different. Copy and paste was the quick answer though...
*
* <p>Copyright (C) 2004 by XXX. All rights reserved.
* <br>Begun on Mon Jun 23 12:16:40 EDT 2004
*/
public class SswingRunner implements Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain                kMain;
    ModelManager2           modelman;
    Residue                 targetRes;

    /** The command string. Not really a template; already in final form. */
    volatile String         cmdTemplate         = null;
    
    /** Controls how much error logging goes on. Set at create time from KingPrefs. */
    final boolean           dumpCmdLine, dumpStdErr, dumpStdOut;
    
    /** Controls how long background jobs can live, in msec. */
    final int               helperTimeout;

    /* sidechain chi angles */
    double[]                angles = new double[5];
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public SswingRunner(KingMain kmain, ModelManager2 modelman, Residue targetRes, String cmd)
    {
        if(kmain == null || modelman == null || targetRes == null || cmd == null)
            throw new NullPointerException("Null parameters are not allowed.");
        
        this.kMain          = kmain;
        this.modelman       = modelman;
        this.targetRes      = targetRes;
        this.cmdTemplate    = cmd;
        
        dumpCmdLine         = kMain.getPrefs().getBoolean("showHelperCommand");
        dumpStdErr          = kMain.getPrefs().getBoolean("showHelperErrors");
        dumpStdOut          = kMain.getPrefs().getBoolean("showHelperOutput");
        helperTimeout       = kMain.getPrefs().getInt("helperTimeout") * 1000;
        
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }
//}}}

//{{{ run
//##################################################################################################
    public void run()
    {
        try { runCommand(cmdTemplate); }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        
        // update the UI from the GUI thread
        SwingUtilities.invokeLater(new ReflectiveRunnable(this, "cmdFinished"));
    }
//}}}

//{{{ runCommand
//##################################################################################################
    /** This is where the background thread does its work. */
    void runCommand(String cmdLine) throws IOException
    {
        // This class doesn't need or support placeholders

        if(dumpCmdLine)
            SoftLog.err.println(cmdLine); // print cmd line for debugging
        String[] cmdTokens = Strings.tokenizeCommandLine(cmdLine);

        // waiting for sswing process to finish
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(cmdTokens);
            proc.waitFor();
        }
        catch (IOException e1) { System.out.println("can't create process:" +e1); }
        catch (InterruptedException ex) { System.out.println("process can't wait:" +ex); }
        
        // initialize the chi angles
        for (int i=0; i<angles.length; i++) {
            angles[i] = 180.0;
        }
        
        /* parse sswing outpput file   */
        BufferedReader br = new BufferedReader(new FileReader("forKingsswingOutput.txt"));
        String s;
        int i=0;
        while((s = br.readLine()) != null)
        {
            try
            {
                StringTokenizer st = new StringTokenizer(s, ":");
                while (st.hasMoreTokens())
                {
                    if(i<4)
                    {
                        angles[i] = Double.valueOf(st.nextToken()).doubleValue();
                        i++;
                    }
                    else st.nextToken();
                }
            }
            catch(IndexOutOfBoundsException ex) {}
        }//while more lines
        br.close();
    }
    
    // Copies src to dst until we hit EOF
    private static final void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ cmdFinished
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Gets called when the run is done. Now we're in the Swing (not "Sswing" !) GUI thread */
    public void cmdFinished()
    {
        try
        {
            SidechainSswing refitWindow = new SidechainSswing(kMain.getTopWindow(), targetRes, modelman);
            refitWindow.setAllAngles(angles);
            
            /* show sswing output file (detail) */
            SswingResultFrame sswingResult = new SswingResultFrame("sswingOutput.txt", refitWindow);
            sswingResult.showFrame();
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ findProgram
//##################################################################################################
    /**
    * Attempts to find the given program name in the same directory as the king.jar file.
    * In this case, the entire path will be quoted to protect any whitespace inside.
    * If not found, it assumes the program is in the PATH.
    * Automatically appends ".exe" if we appear to be running under Windows.
    */
    static public String findProgram(KingMain kMain, String basename)
    {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.indexOf("windows") != -1)
            basename = basename+".exe";
        
        // We search the directory holding the king.jar file
        // for 'probe' or 'probe.exe'; if not found, we just use 'probe'.
        File progFile = new File(kMain.getPrefs().jarFileDirectory, basename);
        if(progFile.exists())
        {
            // Full path might have spaces in it (Win, Mac)
            try { basename = "'"+progFile.getCanonicalPath()+"'"; }
            catch(Throwable t) { t.printStackTrace(SoftLog.err); }
        }
        
        return basename;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

