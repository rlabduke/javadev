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
//}}}
/**
* <code>BgKinRunner</code> runs some command that produces a kinemage file
* in the background and updates the current kinemage with it when it finishes.
* It's designed to, e.g. run Probe and find dots between
* some modified portion of the model and the original PDB.
*
* <p>Each instance of this class creates a background thread.
* Therefore, a single instance should be re-used as much as possible in order to conserve resources.
* It is also a good idea to call {@link #terminate()} before discarding the object,
* so that the useless thread is cleaned up rather than continuing to consume resources.
* On the other hand, it may be a good idea to use one instance per
* target program so that fast programs aren't held up by slow ones.
*
* The output of the target command is expected to be a legal kinemage
* containing exactly one group. The command will receive a fragment
* of a PDB file consisting of only the "molten" atoms on standard input,
* and can be given the following placeholders on the command line too:
<ul>
<li><b>{pdbfile}</b> the full path and file name for the "base" PDB file</li>
<li><b>{molten}</b> a comma-separated list of residue numbers for molten residues</li>
<li><b>{center}</b> the center of view for the current kinemage</li>
</ul>
*
* <p>Be careful -- stray instances of this class will prevent
* garbage collection of their target kinemages!
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun 23 12:16:40 EDT 2003
*/
public class BgKinRunner implements Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain                kMain;

    /** The target kinemage that we want to put the dots into. */
    Kinemage                kin;
    
    /** The group that holds the dots from the most recent time we ran the command. */
    volatile Kinemage       newKin              = null;
    
    /** The group that holds the dots from the previous time we ran the command. */
    KGroup                  oldGroup            = null;
    
    /** The drop-box for residues to be plotted. */
    volatile Collection     dropboxResidues     = null;
    
    /** The drop-box for the state to be plotted. */
    volatile ModelState     dropboxState        = null;
    
    /** The reference PDB file that we want to contrast with. */
    volatile File           dropboxPdbFile      = null;
    
    /** The command string with placeholders ({pdbfile}, {molten}, etc.) intact. */
    volatile String         cmdTemplate         = null;
    
    /** True iff the drop-box has been filled and not emptied. */
    volatile boolean        dropboxFull         = false;
    
    /** If true, the background thread will terminate and this object will become useless. */
    volatile boolean        backgroundTerminate = false;
    
    /** Controls how much error logging goes on. Set at create time from KingPrefs. */
    final boolean           dumpCmdLine, dumpStdErr, dumpStdOut;
    
    /** Controls how long background jobs can live, in msec. */
    final int               helperTimeout;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public BgKinRunner(KingMain kmain, Kinemage kin, String cmd)
    {
        if(kmain == null || kin == null || cmd == null)
            throw new NullPointerException("Null parameters are not allowed.");
        
        this.kMain          = kmain;
        this.kin            = kin;
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
        while(!backgroundTerminate)
        {
            while(dropboxFull)
            {
                Collection  residues;
                ModelState  state;
                File        pdbfile;
                String      cmdtemp;
                synchronized(this)
                {
                    residues    = dropboxResidues;
                    state       = dropboxState;
                    pdbfile     = dropboxPdbFile;
                    cmdtemp     = cmdTemplate;
                    dropboxFull = false;
                    // it may get refilled while the command is running
                }
                
                // runCommand() shouldn't hold a lock b/c users
                // may want to submit an update request.
                try { runCommand(residues, state, pdbfile, cmdtemp); }
                catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
            }//while dropboxFull
            
            // update the kinemage from the GUI thread
            SwingUtilities.invokeLater(new ReflectiveRunnable(this, "updateKinemage"));
            
            // we have to own the lock in order to wait()
            synchronized(this)
            {
                // we will be notify()'d when state changes
                try { this.wait(); }
                catch(InterruptedException ex) {}
            }
        }
    }
//}}}

//{{{ runCommand
//##################################################################################################
    /** This is where the background thread does its work. */
    void runCommand(Collection residues, ModelState state, File pdbfile, String cmdTplt)
        throws IOException
    {
        // Build replacement strings for placeholders
        StringBuffer resCommas = new StringBuffer();
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue res = (Residue)iter.next();
            if(resCommas.length() > 0) resCommas.append(",");
            resCommas.append(res.getSequenceNumber());
        }
        float[] ctr = kin.getCurrentView().getCenter();
        String viewCtr = ctr[0]+", "+ctr[1]+", "+ctr[2];
        
        // Splice in parameters and parse out command line
        String[] cmdKeys = {
            "pdbfile",
            "molten",
            "center"
        };
        String[] cmdParams = {
            pdbfile.getCanonicalPath(),
            resCommas.toString(),
            viewCtr
        };
        // MessageFormat doesn't work well here so we roll our own:
        String cmdLine = Strings.expandVariables(cmdTplt, cmdKeys, cmdParams);
        if(dumpCmdLine)
            SoftLog.err.println(cmdLine); // print cmd line for debugging
        String[] cmdTokens = Strings.tokenizeCommandLine(cmdLine);
        //for(int i = 0; i < cmdTokens.length; i++) SoftLog.err.println("  #"+cmdTokens[i]+"#");
        
        // Build up the PDB fragment in a memory buffer
        // This decreases latency and may avoid deadlock...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdbWriter writer = new PdbWriter(baos);
        writer.writeResidues(residues, state);
        // Launch command and feed it the PDB file fragment
        Process proc = Runtime.getRuntime().exec(cmdTokens);
        baos.writeTo(proc.getOutputStream());
        // send EOF to let command know we're done
        proc.getOutputStream().close();
    
        // Buffer output to decrease latency and chance of deadlock?
        ProcessTank tank = new ProcessTank();
        if(!tank.fillTank(proc, helperTimeout))
            SoftLog.err.println("*** Abnormal (forced) termination of background process '"+cmdTokens[0]+"'!");
        
        // Try to interpret what it sends back
        KinfileParser parser = new KinfileParser();
        parser.parse(new LineNumberReader(new InputStreamReader(tank.getStdout())));
        Collection kins = parser.getKinemages();
        if(kins.size() > 0)
        {
            newKin = (Kinemage)kins.iterator().next();
            if(dumpStdErr) streamcopy(tank.getStderr(), SoftLog.err);
        }
        else
        {
            newKin = null;
            SoftLog.err.println("*** No kinemage produced by background process '"+cmdTokens[0]+"'");
            SoftLog.err.println(cmdLine);
            SoftLog.err.println();
            streamcopy(tank.getStderr(), SoftLog.err);
            if(dumpStdOut)
            {
                SoftLog.err.println();
                streamcopy(tank.getStdout(), SoftLog.err);
            }
        }
    }
    
    // Copies src to dst until we hit EOF
    private static final void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ updateKinemage
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Gets called when the kinemage needs to be updated with Probe dots */
    public void updateKinemage()
    {
        if(newKin != null)
        {
            Iterator iter = newKin.iterator();
            if(iter.hasNext())
            {
                KGroup newGroup = (KGroup)iter.next();
                //newGroup.setDominant(true);  // we don't need to see 1-->2, 2-->1
                newGroup.setOwner(kin);      // have to make sure we know who our parent is
                
                // append kinemage creates all the masters we need
                if(oldGroup == null)    kin.appendKinemage(newKin);
                else                    kin.replace(oldGroup, newGroup);
                oldGroup = newGroup;
                newKin = null;
                
                kMain.notifyChange(KingMain.EM_EDIT_GROSS);
            }
        }
    }
//}}}

//{{{ requestRun, terminate, getKinemage
//##################################################################################################
    /**
    * Fills the dropbox with a request for the background thread.
    * @throws IllegalThreadStateException if terminate() has ever been called on this object.
    */
    public synchronized void requestRun(Collection residues, ModelState state, File pdbfile)
    {
        if(backgroundTerminate)
            throw new IllegalThreadStateException("terminate() was called; worker thread is dead");
        
        this.dropboxResidues    = residues;
        this.dropboxState       = state;
        this.dropboxPdbFile     = pdbfile;
        this.dropboxFull        = true;
        this.notifyAll();
    }
    
    /** Kills off the background thread. Call after you're done with this object. */
    public synchronized void terminate()
    {
        backgroundTerminate = true;
        this.notifyAll();
    }
    
    /** Returns the kinemage this plotter was created with (not null) */
    public Kinemage getKinemage()
    { return kin; }
//}}}

//{{{ findProgram
//##################################################################################################
    /**
    * Attempts to find the given program name in the same directory as the king.jar file.
    * In this case, the entire path will be quoted to protect any whitespace inside.
    * If not found, it assumes the program is in the PATH.
    * Automatically appends ".exe" if we appear to be running under Windows.
    */
    public String findProgram(String basename)
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

//{{{ get/set/editCommand
//##################################################################################################
    /** Returns the command line that was supplied at create time or since modified. */
    public String getCommand()
    { return cmdTemplate; }
    
    /**
    * Gives a new value for the command to be launched.
    * Does not automatically re-run the background program.
    */
    public synchronized void setCommand(String cmd)
    {
        cmdTemplate = cmd;
    }
    
    /**
    * Allows the user to edit the command via a Swing dialog box.
    * @return true if the user changed the command line
    */
    public boolean editCommand(Component dlgParent)
    {
        String cmd = this.getCommand();
        Object[] msg = {
            "{pdbfile} is the full path to the PDB file",
            "{molten} is a list of molten residues: 1,2,3",
            "{center} is the current center of view: x, y, z",
        };
        
        Object input = JOptionPane.showInputDialog(dlgParent,
            msg, "Edit command line", JOptionPane.PLAIN_MESSAGE,
            null, null, cmd);
        
        if(input != null && !cmd.equals(input.toString()))
        {
            this.setCommand(input.toString());
            return true;
        }
        else return false;
    }
//}}}

//{{{ setLastGroupOn
//##################################################################################################
    /** Convenience for calling setOn() on the last KGroup generated by this command runner. */
    public void setLastGroupOn(boolean b)
    {
        if(oldGroup != null)
            oldGroup.setOn(b);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

