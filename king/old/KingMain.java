// (jEdit options) :folding=explicit:collapseFolds=1:
package king;

import java.awt.*;
import java.awt.event.*;
//import java.awt.geom.*;
import java.io.*;
import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import driftwood.string.Props;

/**
 * <code>KingMain</code> is the control center of the King program.
 *
 * <p>Begun on Mon Apr 22 17:18:36 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis
 */
public class KingMain implements WindowListener
{
    public static void main(String[] args) { new KingMain(args).Main(); }
    
    // Used for counting # of clones still alive, so we don't
    // call System.exit() prematurely!
    static int instanceCount = 0;

//{{{ Event masks
    /** Event mask: current kinemage has been switched */
    public static final int EM_SWITCH     = 0x00000001;
    /** Event mask: all kinemages have been closed */
    public static final int EM_CLOSEALL   = 0x00000002;
    /** Event mask: new view was chosen from the Views menu */
    public static final int EM_NEWVIEW    = 0x00000004;
    /** Event mask: large-scale editing was performed (group/list level) */
    public static final int EM_EDIT_GROSS = 0x00000008;
    /** Event mask: small-scale editing was performed (point level) */
    public static final int EM_EDIT_FINE  = 0x00000010;
    /** Event mask: buttons were turned on/off, or points became (in)visible for some other reason (master, animate) */
    public static final int EM_ON_OFF     = 0x00000020;
    /** Event mask: display mode changed */
    public static final int EM_DISPLAY    = 0x00000040;
    /** Event mask: current kinemage has been closed */
    public static final int EM_CLOSE      = 0x00000080;
    /** Event mask: preferences were updated */
    public static final int EM_PREFS      = 0x00000100;
//}}}

//{{{ Variables
//##################################################################################################
    KingPrefs           prefs       = null;
    KinStable           kinStable   = null;
    KinReader           kinReader   = null;
    KinfileIO           kinIO       = null;
    KinCanvas           kinCanvas   = null;
    UIMenus             uiMenus     = null;
    UIText              uiText      = null;
    KinTree             kinTree     = null;
    MainWindow          mainWin     = null;
    JApplet             theApplet   = null;
    
    ArrayList           filesToOpen = null;
    boolean             doMerge     = true;
//}}}
    
//{{{ Constructors
//##################################################################################################
    /**
    * Constructor for application
    */
    public KingMain(String[] args)
    {
        prefs = new KingPrefs();
        parseArguments(args);
        instanceCount++;
    }

    /**
    * Constructor for applet
    */
    public KingMain(JApplet plet)
    {
        prefs = new KingPrefs();
        theApplet = plet;
        instanceCount++;
    }
//}}}

//{{{ shutdown
//##################################################################################################
    /**
    * Initiates shutdown, albeit in a crude way. Called by Kinglet.stop() and the window close listeners.
    */
    public void shutdown()
    {
        if(uiText != null) uiText.shutdown();
        if(mainWin != null) mainWin.shutdown();
        
        instanceCount--;
        if(instanceCount <= 0)
        {
            try { System.exit(0); } catch(SecurityException ex) {}
        }
    }
//}}}

//{{{ Main()
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        // It's just too hard to change this after we've already started up!
        float magnification = prefs.getFloat("fontMagnification");
        if(magnification != 1)
            javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new MagnifiedTheme(magnification));
        
        kinStable = new KinStable(this);
        mainWin   = new MainWindow(this); // doesn't create GUI yet
        kinReader = new KinReader(this);  // progress dlg. references main window
        kinIO     = new KinfileIO(this);  // progress dlg. references main window
        kinCanvas = new KinCanvas(this);
        uiMenus   = new UIMenus(this);
        uiText    = new UIText(this);
        kinTree   = new KinTree(this);

        mainWin.buildGUI();
        mainWin.addWindowListener(this);
        mainWin.pack();
        mainWin.setVisible(true);
        if(prefs.getBoolean("textOpenOnStart"))     uiText.cascadeBehind(mainWin);

        SwingUtilities.invokeLater(new ReflectiveRunnable(this, "loadFiles"));
    }
//}}}

//{{{ loadFiles()
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void loadFiles()
    {
        if(filesToOpen == null) return;
        
        ArrayList urls = new ArrayList();
        File file;
        for(Iterator iter = filesToOpen.iterator(); iter.hasNext(); )
        {
            try {
                file = (File)iter.next();
                if(file.exists()) urls.add( file.toURL() );
            }
            catch(MalformedURLException ex) {}
            catch(SecurityException ex)     {}
        }
        
        if(urls.size() > 0)
        {
            if(doMerge && urls.size() > 1)
            {
                Kinemage k = new Kinemage(this, "Merged kins");
                kinReader.open(urls, k);
                ArrayList l = new ArrayList();
                l.add(k);
                kinStable.append(l);
            }
            else kinReader.open(urls, null);
        }
    }
//}}}

//{{{ notifyChange
//##################################################################################################
    /**
    * Notifies all existing sub-components of a change to the state of the program.
    * Only notifies components directly owned by KingMain;
    * for instance KinCanvas is reponsible for propagating this to Engine.
    * @param event_mask the bitwise OR of the flag(s) representing the change
    */    
    public void notifyChange(int event_mask)
    {
        if(kinStable != null) kinStable.notifyChange(event_mask);
        if(mainWin   != null) mainWin.notifyChange(event_mask);
        if(kinReader != null) kinReader.notifyChange(event_mask);
        if(kinCanvas != null) kinCanvas.notifyChange(event_mask);
        if(uiMenus   != null) uiMenus.notifyChange(event_mask);
        if(kinTree   != null) kinTree.notifyChange(event_mask);
    }
//}}}

//{{{ getXXX functions
//##################################################################################################
    /** Returns the data model that holds all data for this session (never null) */
    public KinStable getStable() { return kinStable; }
    
    /** Returns the kinemage reader (never null) */
    public KinReader getReader() { return kinReader; }
    
    /** Returns the kinemage reader/writer (never null) */
    public KinfileIO getKinIO() { return kinIO; }
    
    /** Returns the active drawing canvas (may be null) */
    public KinCanvas getCanvas() { return kinCanvas; }
    
    /** Returns the main window (may be null) */
    public MainWindow getMainWindow() { return mainWin; }
    
    /** Returns the applet this was spawned from (may be null) */
    public JApplet getApplet() { return theApplet; }
    
    /** Returns the collection of UI actions and menus that manage user input (may be null) */
    public UIMenus getMenus() { return uiMenus; }
    
    /** Returns the text storage/edit/display system (never null) */
    public UIText getTextWindow() { return uiText; }
    
    /** Returns the tree controller (may be null) */
    public KinTree getKinTree() { return kinTree; }
    
    /** Returns the preferences storage object */
    public KingPrefs getPrefs() { return prefs; }
    
    /** Convenience function for getStable().getKinemage().getTreeModel() (may be null) */
    public DefaultTreeModel getTreeModel()
    {
        Kinemage kin = kinStable.getKinemage();
        if(kin == null) return null;
        return kin.getTreeModel();
    }

    /** Convenience function for getStable().getKinemage() (may be null) */
    public Kinemage getKinemage()
    {
        return kinStable.getKinemage();
    }

    /** Convenience function for getStable().getKinemage().getCurrentView() (may be null) */
    public KingView getView()
    {
        Kinemage kin = kinStable.getKinemage();
        if(kin == null) return null;
        return kin.getCurrentView();
    }
//}}}
    
//{{{ parseArguments()
//##################################################################################################
    // Interpret command-line arguments
    void parseArguments(String[] args)
    {
        filesToOpen = new ArrayList();
        
        String arg;
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            // this is an option
            if(arg.startsWith("-"))
            {
                if(arg.equals("-h") || arg.equals("-help")) {
                    echo("Help not available. Sorry!");
                    System.exit(0);
                } else if(arg.equals("-version")) {
                    echo("KingMain, version "+getPrefs().getString("version")+"\nCopyright (C) 2002-2003 by Ian W. Davis");
                    System.exit(0);
                } else if(arg.equals("-m") || arg.equals("-merge")) {
                    doMerge = true;
                } else if(arg.equals("-s") || arg.equals("-single")) {
                    doMerge = false;
                } else {
                    echo("*** Unrecognized option: "+arg);
                }
            }
            // this is a file, etc.
            else
            {
                filesToOpen.add(new File(arg));
            }
        }
    }
//}}}

//{{{ Window events
//##################################################################################################
    public void windowActivated(WindowEvent ev)   {}
    public void windowClosed(WindowEvent ev)      {}
    public void windowClosing(WindowEvent ev)     { shutdown(); }
    public void windowDeactivated(WindowEvent ev) {}
    public void windowDeiconified(WindowEvent ev) {}
    public void windowIconified(WindowEvent ev)   {}
    public void windowOpened(WindowEvent ev)      {}
//}}}

//{{{ echo
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}
}//class
