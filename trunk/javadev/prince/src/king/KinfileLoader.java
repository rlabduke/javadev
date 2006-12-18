// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.io.*;

//import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.util.*;
//}}}
/**
* <code>KinfileLoader</code> is a system for loading
* kinemages in a background thread. It messages a
* Listener with the results of its actions.
* This is the level where auto-detection of gzipped
* kinemages takes place.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Apr 11 10:07:14 EDT 2003
*/
public class KinfileLoader implements ActionListener
{
//{{{ Constants
//}}}

//{{{ INTERFACE: Listener
//##################################################################################################
    /**
    * <code>Listener</code> is an interface for monitoring
    * the progress of a kinemage that's being loaded in the
    * background. All these methods are called from the
    * Swing event-handling thread.
    */
    public interface Listener //extends ... implements ...
    {
        /**
        * Messaged periodically as the parser reads the file.
        */
        public void updateProgress(long charsRead);
        
        /**
        * Messaged if anything is thrown during the loading process.
        * This generally means loadingComplete() won't be called.
        */
        public void loadingException(Throwable t);
        
        /**
        * Messaged if and when loading finished successfully.
        */
        public void loadingComplete(KinfileParser parser);
    }//class
//}}}

//{{{ Variable definitions
//##################################################################################################
    InputStream         input;
    Listener     listener;
    Thread              thread;
    javax.swing.Timer   timer;
    
    KinfileParser       parser;
    Throwable           thrown  = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a loader with the given input and listener,
    * starts the job in the background, and returns as soon
    * as the job has begun.
    */
    public KinfileLoader(InputStream input, Listener listener)
    {
        this.input      = input;
        this.listener   = listener;
        
        parser  = new KinfileParser();
        timer   = new javax.swing.Timer(1000, this); // update every 1000ms
        
        thread  = new Thread(new ReflectiveRunnable(this, "backgroundWorker"));
        thread.setDaemon(true);
        
        timer.start();
        thread.start();
    }
//}}}

//{{{ backgroundWorker
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Should not be called directly. Does loading in the background. */
    public void backgroundWorker()
    {
        try
        {
            LineNumberReader    lnr;
            
            // Test for GZIPped files
            input = new BufferedInputStream(input);
            input.mark(10);
            if(input.read() == 31 && input.read() == 139)
            {
                // We've found the gzip magic numbers...
                input.reset();
                input = new GZIPInputStream(input);
            }
            else input.reset();
            
            lnr = new LineNumberReader(new InputStreamReader(input));
            parser.parse(lnr);
            
            SwingUtilities.invokeLater(new ReflectiveRunnable(this, "successCallback"));
            lnr.close();
        }
        catch(Throwable t)
        {
            thrown = t;
            SwingUtilities.invokeLater(new ReflectiveRunnable(this, "errorCallback"));
        }
    }
//}}}

//{{{ actionPerformed
//##################################################################################################
    /** Messaged when the progress needs to be updated */
    public void actionPerformed(ActionEvent ev)
    {
        listener.updateProgress(parser.getCharsRead());
    }
//}}}

//{{{ successCallback, errorCallback
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Should not be called directly. Notifies listener that kinemage has been loaded. */
    public void successCallback()
    {
        timer.stop();
        listener.loadingComplete(parser);
        parser = null;
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /** Should not be called directly. Notifies listener of I/O or other error. */
    public void errorCallback()
    {
        timer.stop();
        listener.loadingException(thrown);
        parser = null;
        thrown = null;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

