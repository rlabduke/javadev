// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.util.*;
//}}}
/**
* <code>ProbePlotter</code> runs Probe in the background and updates the kinemage when it finishes.
* It's designed to find dots between some modified portion of the model and the original PDB.
*
* <p>Each instance of this class creates a background thread.
* Therefore, a single instance should be re-used as much as possible in order to conserve resources.
* It is also a good idea to call {@link #terminate()} before discarding the object,
* so that the useless thread is cleaned up rather than continuing to consume resources.
*
* <p>Be careful -- stray instances of this class will prevent
* garbage collection of their target kinemages!
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun 23 12:16:40 EDT 2003
*/
public class ProbePlotter implements Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain                kMain;

    /** The target kinemage that we want to put the dots into. */
    Kinemage                kin;
    
    /** True iff we want dots to water displayed. */
    volatile boolean        showWaterDots       = false;
    
    /** The group that holds the dots from the most recent time we ran Probe. */
    volatile Kinemage       newDotKin           = null;
    
    /** The group that holds the dots from the previous time we ran Probe. */
    KGroup                  oldDots             = null;
    
    /** The drop-box for residues to be plotted. */
    volatile Collection     dropboxResidues     = null;
    
    /** The drop-box for the state to be plotted. */
    volatile ModelState     dropboxState        = null;
    
    /** The reference PDB file that we want to contrast with. */
    volatile File           dropboxPdbFile      = null;
    
    /** True iff the drop-box has been filled and not emptied. */
    volatile boolean        dropboxFull         = false;
    
    /** If true, the background thread will terminate and this object will become useless. */
    volatile boolean        backgroundTerminate = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ProbePlotter(KingMain kmain, Kinemage kin)
    {
        if(kmain == null || kin == null)
            throw new NullPointerException("Null parameters are not allowed.");
        
        this.kMain      = kmain;
        this.kin        = kin;
        
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
                synchronized(this)
                {
                    residues    = dropboxResidues;
                    state       = dropboxState;
                    pdbfile     = dropboxPdbFile;
                    dropboxFull = false;
                    // it may get refilled while Probe is running
                }
                // runProbe() shouldn't hold a lock b/c users
                // may want to submit a Probe request.
                runProbe(residues, state, pdbfile);
            }
            
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

//{{{ runProbe
//##################################################################################################
    /** This is where the background thread does its work. */
    void runProbe(Collection residues, ModelState state, File pdbfile)
    {
        StringBuffer resNumbers = new StringBuffer();
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue res = (Residue)iter.next();
            if(resNumbers.length() > 0) resNumbers.append(",");
            resNumbers.append(res.getSequenceNumber());
        }
        
        // We search the directory holding the king.jar file
        // for 'probe' or 'probe.exe'; if not found, we just use 'probe'.
        File probeExe = new File(kMain.prefs.jarFileDirectory, "probe");
        if(!probeExe.exists())
            probeExe = new File(kMain.prefs.jarFileDirectory, "probe.exe");
        String probeName = "probe";
        if(probeExe.exists())
        {
            try { probeName = probeExe.getCanonicalPath(); }
            catch(Throwable t) { t.printStackTrace(SoftLog.err); }
        }
        
        // -drop is very important, or else the unselected atoms from file1
        // (waters, the residues we're excluding because they're in file2)
        // will interfere with (obstruct) the dots between file1 and file2 atoms.
        String probeCmd = probeName+" -quiet -drop -mc -both -stdbonds '(file1 "
            +(showWaterDots ? "" : "not water ")
            +"not("+resNumbers+")),file2' 'file2' "+pdbfile.getAbsolutePath()+" -";
        String[] cmdTokens = Strings.tokenizeCommandLine(probeCmd);
        
        try
        {
            // Build up the PDB fragment in a memory buffer
            // This decreases latency and may avoid deadlock...
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdbWriter writer = new PdbWriter(baos);
            writer.writeResidues(residues, state);
            // Launch Probe and feed it the PDB file fragment
            Process probe = Runtime.getRuntime().exec(cmdTokens);
            baos.writeTo(probe.getOutputStream());
            // send EOF to let Probe know we're done
            probe.getOutputStream().close();
        
            // Buffer output to decrease latency and chance of deadlock?
            ProcessTank tank = new ProcessTank();
            if(!tank.fillTank(probe, 5000)) // 5 sec timeout
                SoftLog.err.println("*** Abnormal (forced) termination of PROBE process!");
            
            // Try to interpret what it sends back
            KinfileParser parser = new KinfileParser(kMain);
            parser.parse(new LineNumberReader(new InputStreamReader(tank.getStdout())));
            Collection kins = parser.getKinemages();
            if(kins.size() > 0) newDotKin = (Kinemage)kins.iterator().next();
            else                newDotKin = null;
        }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ updateKinemage
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Gets called when the kinemage needs to be updated with Probe dots */
    public void updateKinemage()
    {
        if(newDotKin != null)
        {
            KGroup newDots = (KGroup)newDotKin.iterator().next();
            newDots.setDominant(true);  // we don't need to see 1-->2, 2-->1
            newDots.setOwner(kin);      // have to make sure we know who our parent is
            
            // append kinemage creates all the masters we need
            if(oldDots == null)     kin.appendKinemage(newDotKin);
            else                    kin.replace(oldDots, newDots);
            oldDots = newDots;
            newDotKin = null;
            
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
        }
    }
//}}}

//{{{ requestProbe, terminate, getKinemage
//##################################################################################################
    /**
    * Fills the dropbox with a request for the background thread.
    * @throws IllegalThreadStateException if terminate() has ever been called on this object.
    */
    public synchronized void requestProbe(Collection residues, ModelState state, File pdbfile)
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

//{{{ get/set{DotsToWater}
//##################################################################################################
    public boolean getDotsToWater()
    { return showWaterDots; }
    
    public void setDotsToWater(boolean b)
    { showWaterDots = b; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

