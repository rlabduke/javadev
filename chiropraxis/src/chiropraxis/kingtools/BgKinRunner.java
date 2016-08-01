// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
import king.*;
import king.core.*;
import king.io.*;

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
import driftwood.r3.*;
import driftwood.util.*;
import driftwood.util.ReflectiveRunnable;
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
<li><b>{viewcenter}</b> the center of view for the current kinemage</li>
<li><b>{bbcenter}</b> the center of the bounding box for all molten atoms</li>
<li><b>{bbradius}</b> distance from the {bbcenter} that captures all molten atoms plus a 5A buffer</li>
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
    DecimalFormat df = new DecimalFormat("0.0##");
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
            //long outsideLoop = System.currentTimeMillis();
            while(dropboxFull)
            {
                //long insideLoop = System.currentTimeMillis();
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
                //System.err.println("This cycle:     "+(System.currentTimeMillis() - insideLoop)+" ms");
            }//while dropboxFull

            // update the kinemage from the GUI thread
            // this takes 1 - 200 ms on the Mac (yes, really -- but why?)
            //long endOfLoop = System.currentTimeMillis();
            SwingUtilities.invokeLater(new ReflectiveRunnable(this, "updateKinemage"));
            //long afterInvoke = System.currentTimeMillis();
            //System.err.println("invokeLater():  "+(afterInvoke-endOfLoop)+" ms");

            // we have to own the lock in order to wait()
            synchronized(this)
            {
                //long afterSync = System.currentTimeMillis();
                //System.err.println("synchronize:    "+(afterSync-afterInvoke)+" ms");
                //System.err.println("run() loop:     "+(afterSync-outsideLoop)+" ms");
                //System.err.println("----------------------------------------");
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
        long time;

        // Make the command line in its final form.
        // This is very fast (1-2 ms).
        // Build replacement strings for placeholders
        StringBuffer resCommas = new StringBuffer();
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue res = (Residue)iter.next();
            if(resCommas.length() > 0) resCommas.append(",");
            resCommas.append(res.getSequenceNumber());
        }
        // can't leave it empty, and there is no "none" keyword
        if(resCommas.length() == 0) resCommas.append("not all"); // == none (?)
        float[] ctr = kMain.getView().getCenter();
        String viewCtr = ctr[0]+", "+ctr[1]+", "+ctr[2];
        Triple[] bbox = getBoundingBox(residues, state);
        // Bounding box may be NaN if residues is empty
        if(bbox[0].isNaN()) bbox[0].setXYZ(0,0,0);
        if(bbox[1].isNaN()) bbox[1].setXYZ(0,0,0);
        double radius = bbox[1].mag() + 5.0;
        // Splice in parameters and parse out command line
        String[] cmdKeys = {
            "pdbfile",
            "molten",
            "viewcenter",
            "bbcenter",
            "bbradius"
        };
        String[] cmdParams = {
            pdbfile.getCanonicalPath(),
            resCommas.toString(),
            viewCtr,
            bbox[0].format(df, ", "),
            df.format(radius)
        };
        // MessageFormat doesn't work well here so we roll our own:
        String cmdLine = Strings.expandVariables(cmdTplt, cmdKeys, cmdParams);
        if(dumpCmdLine)
            SoftLog.err.println(cmdLine); // print cmd line for debugging
        String[] cmdTokens = Strings.tokenizeCommandLine(cmdLine);
        //for(int i = 0; i < cmdTokens.length; i++) SoftLog.err.println("  #"+cmdTokens[i]+"#");
    //System.err.println("time "+cmdLine+" < dummy.pdb > dummy.out");

        // Create the PDB file fragment to be feed in on stdin.
        // This also very fast (~10 ms).
        // Build up the PDB fragment in a memory buffer
        // This decreases latency and may avoid deadlock...
        StreamTank pdbFrag = new StreamTank();
        PdbWriter writer = new PdbWriter(pdbFrag);
        writer.writeResidues(residues, state);
        // Don't run the cmd if we're not goint to use the results:
        if(dropboxFull) return;


    //time = System.currentTimeMillis();
        // Launch command and feed it the PDB file fragment
        // This is usually the lion's share of run time (>50%)
        Process proc = Runtime.getRuntime().exec(cmdTokens);
    //System.err.println("Starting proc:      "+(System.currentTimeMillis()-time)+" ms");
    //time = System.currentTimeMillis();
        // Send PDB fragment to stdin (buffering doesn't help speed)
        //OutputStream stdin = new BufferedOutputStream(proc.getOutputStream(), 20000);
        //pdbFrag.writeTo(stdin);
        pdbFrag.writeTo(proc.getOutputStream());
        // send EOF to let command know we're done
        //stdin.close();
        proc.getOutputStream().close();
    //System.err.println("Feeding stdout:     "+(System.currentTimeMillis()-time)+" ms with "+pdbFrag.size()+" bytes");
    //time = System.currentTimeMillis();
        // Wait for command to finish and collect its output on stdout.
        // Buffer output of cmd to decrease latency and chance of deadlock?
        ProcessTank tank = new ProcessTank();
        if(!tank.fillTank(proc, helperTimeout))
            SoftLog.err.println("*** Forced termination of background process '"+cmdTokens[0]+"'!");
    //System.err.println("Harvesting stdin:   "+(System.currentTimeMillis()-time)+" ms");
        // Don't bother with parsing if we're not goint to use the results:
        if(dropboxFull) return;


    //time = System.currentTimeMillis();
        // Try to interpret what it sends back
        // This is also fairly slow (~100-200 ms)
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
    //System.err.println("Parsing kin:        "+(System.currentTimeMillis()-time)+" ms");
    }

    // Copies src to dst until we hit EOF
    private static final void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ getBoundingBox
//##################################################################################################
    /** (center, halfwidths) of bounding box for all mobile atoms */
    Triple[] getBoundingBox(Collection residues, ModelState state)
    {
        ArrayList atomStates = new ArrayList(20*residues.size());
        int cnt = 0;
        for(Iterator ri = residues.iterator(); ri.hasNext(); )
        {
            Residue r = (Residue) ri.next();
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom a = (Atom) ai.next();
                try { atomStates.add( state.get(a) ); cnt++; }
                catch(AtomException ex) {}
            }
        }
        Builder builder = new Builder();
        Triple[] bbox = builder.makeBoundingBox(atomStates);
        bbox[0].add(bbox[1]).div(2);
        bbox[1].sub(bbox[0]);
        return bbox;
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
                newGroup.setParent(kin);      // have to make sure we know who our parent is

                // append kinemage creates all the masters we need
                if(oldGroup == null)    kin.appendKinemage(newKin);
                else                    kin.replace(oldGroup, newGroup);
                oldGroup = newGroup;
                newKin = null;
            }
            // Probe may produce a kinemage with an empty @group,
            // but this gets pruned when read in by KiNG,
            // so not even an empty group is present.
            else if(oldGroup != null)
            {
                oldGroup.clear();
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

        // check if running under a Phenix environment for Probe
        if(basename == "probe")
        {
          String curPath = System.getenv("PATH");
          String[] temp = curPath.split(":");
          String phenixPath = "";
          for(int i=0; i<temp.length; i++)
          {
            boolean contains = temp[i].contains("phenix");
            if(contains)
            {
              phenixPath = temp[i];
            }
          }
          if(phenixPath.length() > 0)
          {
            String file = phenixPath+"/phenix.probe";
            File phenixFile = new File(file);
            if(phenixFile.exists())
            {
              basename = "phenix.probe";
            }
          }
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
            "{viewcenter} is the current center of view: x, y, z",
            "{bbcenter} is the center of the bounding box for molten atoms: x, y, z",
            "{bbradius} is the 'radius' of the bounding box for molten atoms: x, y, z",
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

