// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import driftwood.util.*;
//}}}
/**
* <code>KingPrefs</code> holds information about the preferrences of
* the user running this instance of KiNG.
* For documentation of the available options, see resource/king/king_prefs.
*
* <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jun 21 09:10:40 EDT 2002
*/
public class KingPrefs extends Props // implements ...
{
//{{{ Constants
//##################################################################################################
    static final String PROPS_HEADER =
        "#\n"+
        "# This file contains your customized settings for running KiNG.\n"+
        "# Place this in your home directory, or in an applet's\n"+
        "# <PARAM name='king_prefs' value='url/path/to/king.prefs'> tag.\n"+
        "# KiNG must be restarted for changes to take effect.\n"+
        "#\n"+
        "# For more information on all the possible properties that can be set,\n"+
        "# including some which cannot be accessed from the GUI,\n"+
        "# see the sample .king_prefs in the doc/ folder of your KiNG distribution.\n"+
        "#"; // store() adds a newline for us
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** Whether a newer version of KiNG is present on the server. */
    volatile boolean newerVersionAvail = false;
    volatile URL remoteVersionURL = null;

    /** The local dir where the main jar file resides, or null for none. */
    public File jarFileDirectory;
    /** The image icon to display on the corner of the window, in the task bar, etc. */
    public Image windowIcon;
    /** The icon for animating in the reverse direction */
    public Icon stepBackIcon;
    /** The icon for animating in the forward direction */
    public Icon stepForwardIcon;
    /** The icon for move-up type actions */
    public Icon moveUpIcon;
    /** The icon for move-down type actions */
    public Icon moveDownIcon;
    /** The icon for add/new actions (paper+) */
    public Icon addIcon;
    /** The icon for cut actions (scissors) */
    public Icon cutIcon;
    /** The icon for copy actions (two pages) */
    public Icon copyIcon;
    /** The icon for paste actions (clipboard) */
    public Icon pasteIcon;
    /** The icon for delete actions (a trash can) */
    public Icon deleteIcon;
    /** The icon for getting help */
    public Icon helpIcon;
    /** The icon for returning to a previous HTML page */
    public Icon htmlBackIcon;
    /** The icon for returning to the initial HTML page */
    public Icon htmlHomeIcon;
    
    /** Icons for rotations, coded by Axis and Plus/Minus */
    public Icon rotXpIcon, rotXmIcon, rotYpIcon, rotYmIcon, rotZpIcon, rotZmIcon;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public KingPrefs()
    {
        super();
        
        // Self-awareness
        jarFileDirectory = locateJarFile();
        
        // Default settings
        Props defaults = new Props();
        loadFromJar(defaults);
        this.setDefaults(defaults);
        
        // User settings
        loadFromFile();
        
        // Resources
        // Icon that's displayed in the corner of the main window
        windowIcon          = new ImageIcon(getClass().getResource("images/kingicon20.png")).getImage();
        
        stepBackIcon        = new ImageIcon(getClass().getResource("images/StepBack16.gif"));
        stepForwardIcon     = new ImageIcon(getClass().getResource("images/StepForward16.gif"));
        moveUpIcon          = new ImageIcon(getClass().getResource("images/Up16.gif"));
        moveDownIcon        = new ImageIcon(getClass().getResource("images/Down16.gif"));
        addIcon             = new ImageIcon(getClass().getResource("images/Add16.gif"));
        cutIcon             = new ImageIcon(getClass().getResource("images/Cut16.gif"));
        copyIcon            = new ImageIcon(getClass().getResource("images/Copy16.gif"));
        pasteIcon           = new ImageIcon(getClass().getResource("images/Paste16.gif"));
        deleteIcon          = new ImageIcon(getClass().getResource("images/Delete16.gif"));
        helpIcon            = new ImageIcon(getClass().getResource("images/Help16.gif"));
        htmlBackIcon        = new ImageIcon(getClass().getResource("images/Back24.gif"));
        htmlHomeIcon        = new ImageIcon(getClass().getResource("images/Home24.gif"));

        rotXpIcon           = new ImageIcon(getClass().getResource("images/rotxp.png"));
        rotXmIcon           = new ImageIcon(getClass().getResource("images/rotxm.png"));
        rotYpIcon           = new ImageIcon(getClass().getResource("images/rotyp.png"));
        rotYmIcon           = new ImageIcon(getClass().getResource("images/rotym.png"));
        rotZpIcon           = new ImageIcon(getClass().getResource("images/rotzp.png"));
        rotZmIcon           = new ImageIcon(getClass().getResource("images/rotzm.png"));
    }
//}}}

//{{{ locateJarFile
//##################################################################################################
    /**
    * Calculates the current location in the local filesystem
    * of the JAR file that holds king/version.props
    * (presumbably, king.jar).
    * @return null if the JAR cannot be located or is not local
    */
    File locateJarFile()
    {
        URL url = this.getClass().getResource("version.props");
        
        File f = null;
        try
        {
            f = Strings.jarUrlToFile(url);
            if(!f.isDirectory()) f = f.getParentFile();
        }
        catch(IllegalArgumentException ex)  { ex.printStackTrace(SoftLog.err); }
        catch(IOException ex)               { ex.printStackTrace(SoftLog.err); }
        catch(SecurityException ex)         { ex.printStackTrace(SoftLog.err); }
        
        return f;
    }
//}}}

//{{{ loadFromJar, loadFromFile, loadFromURL
//##################################################################################################
    /** Returns true on success, false on failure */
    boolean loadFromJar(Properties loadInto)
    {
        try
        {
            // Defaults from JAR file:
            ClassLoader loader = this.getClass().getClassLoader();
            Enumeration urls = loader.getResources("king/king_prefs"); // no leading slash
            while(urls.hasMoreElements())
            {
                try
                {
                    InputStream is = ((URL)urls.nextElement()).openStream();
                    loadInto.load(is);
                    is.close();
                }
                catch(IOException ex)
                { SoftLog.err.println("Preferences loading error: "+ex.getMessage()); }
            }
            
            // Old, single file code:
            //InputStream is = this.getClass().getResourceAsStream("king_prefs");
            //loadInto.load(is);
            //is.close();
            
            InputStream is = this.getClass().getResourceAsStream("version.props");
            loadInto.load(is);
            is.close();
            is = this.getClass().getResourceAsStream("buildnum.props");
            loadInto.load(is);
            is.close();
        }
        catch(NullPointerException ex) { ex.printStackTrace(SoftLog.err); return false; }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); return false; }
        
        return true;
    }

    /** Returns true on success, false on failure */
    public boolean loadFromFile()
    {
        try
        {
            // Home directory: .king_prefs
            File propfile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".king_prefs");
            if(propfile.exists())
            {
                InputStream is = new BufferedInputStream(new FileInputStream(propfile));
                this.load(is);
                is.close();
                SoftLog.err.println("Loaded preferences from "+propfile.toString());
            }
        }
        catch(NullPointerException ex) { ex.printStackTrace(SoftLog.err); return false; }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); return false; }
        catch(SecurityException ex) { return false; }
        
        return true;
    }

    /** Returns true on success, false on failure */
    public boolean loadFromURL(URL url)
    {
        try
        {
            // Defaults from JAR file:
            InputStream is = url.openStream();
            this.load(is);
            is.close();
        }
        catch(NullPointerException ex) { ex.printStackTrace(SoftLog.err); return false; }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); return false; }
        catch(SecurityException ex) { ex.printStackTrace(SoftLog.err); return false; }
        
        return true;
    }

//}}}

//{{{ storeToFile
//##################################################################################################
    /** Returns true on success, false on failure */
    public boolean storeToFile()
    {
        this.minimizeDifferences(); // store only changes from the defaults
        try
        {
            // Home directory: .king_prefs
            File propfile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".king_prefs");
            PrintStream os = new PrintStream(new BufferedOutputStream(new FileOutputStream(propfile)));
            os.println(PROPS_HEADER);
            this.store(os, "");
            os.close();
            SoftLog.err.println("Stored preferences to   "+propfile.toString());
        }
        catch(NullPointerException ex) { ex.printStackTrace(SoftLog.err); return false; }
        catch(IOException ex) { ex.printStackTrace(SoftLog.err); return false; }
        catch(SecurityException ex) { return false; }
        
        return true;
    }
//}}}

//{{{ checkVersion, newerVersionAvailable
//##################################################################################################
    /** Retrieves a properties file from the given URL and compares version numbers. */
    public void checkVersion(URL propsURL, long timeoutMillis)
    {
        remoteVersionURL = propsURL;
        Thread checkThread = new Thread(new ReflectiveRunnable(this, "checkVersionBackground"));
        checkThread.run();
        // If the URL hasn't connected in time, we leave it running and return to our business.
        try { checkThread.join(timeoutMillis); }
        catch(InterruptedException ex) {}
    }
    
    /** Do not call this directly! A target of reflection. */
    public void checkVersionBackground()
    {
        if(remoteVersionURL == null) return;
        
        Props remote = new Props();
        try
        {
            InputStream is = remoteVersionURL.openStream();
            remote.load(is);
            is.close();
        }
        //catch(NullPointerException ex) { ex.printStackTrace(SoftLog.err); return; }
        //catch(IOException ex) { ex.printStackTrace(SoftLog.err); return; }
        //catch(SecurityException ex) { ex.printStackTrace(SoftLog.err); return; }
        catch(Exception ex)
        {
            SoftLog.err.println("Unable to check for new version: "+ex.getMessage());
            SoftLog.err.println("You can disable version checking under Edit | Configure.");
            return;
        }
        
        String ourVer = this.getString("version");
        String newVer = remote.getString("version", null);
        if(newVer != null && Strings.compareVersions(ourVer, newVer) < 0)
            this.newerVersionAvail = true; // volatile
    }
    
    public boolean newerVersionAvailable()
    { return newerVersionAvail; }
//}}}

//{{{ empty
//##################################################################################################
//}}}
}//class
