// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
//import java.awt.geom.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import driftwood.util.*;
import driftwood.isosurface.*;
import driftwood.gui.*;
//}}}
/**
* <code>KingMain</code> is the control center of the King program.
*
* <p>Begun on Mon Apr 22 17:18:36 EDT 2002
* <br>Copyright (C) 2002-2004 by Ian W. Davis
*/
public class KingMain implements WindowListener, KinemageSignalSubscriber
{
    public static void main(String[] args) { new KingMain(args).Main(); }
    
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
    // Used for counting # of clones still alive, so we don't
    // call System.exit() prematurely!
    static int instanceCount = 0;

    KingPrefs           prefs           = null;
    KinStable           kinStable       = null;
    KinfileIO           kinIO           = null;
    KinCanvas           kinCanvas       = null;
    UIMenus             uiMenus         = null;
    UIText              uiText          = null;
    KinTree             kinTree         = null;
    MainWindow          mainWin         = null;
    ContentPane         contentPane     = null;
    JApplet             theApplet       = null;
    boolean             isAppletFlat    = true;
    
    ArrayList           filesToOpen     = null;
    boolean             doMerge         = true;
//}}}
    
//{{{ Constructors
//##################################################################################################
    /**
    * Constructor for application
    */
    public KingMain(String[] args)
    {
        prefs = new KingPrefs();
        if(prefs.getBoolean("checkNewVersion"))
        {
            // "Timeout" after 2.000 seconds
            try { prefs.checkVersion(new URL("http://kinemage.biochem.duke.edu/software/king.version.props"), 2000); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
        }
        parseArguments(args);
        instanceCount++;
    }

    /**
    * Constructor for applet
    */
    public KingMain(JApplet plet, boolean isFlat)
    {
        prefs           = new KingPrefs();
        theApplet       = plet;
        isAppletFlat    = isFlat;

        // Load custom config from URL
        String king_prefs = theApplet.getParameter("king_prefs");
        if(king_prefs != null) try
        {
            URL prefsURL = new URL(theApplet.getDocumentBase(), king_prefs);
            prefs.loadFromURL(prefsURL);
        }
        catch(MalformedURLException ex)
        { SoftLog.err.println("<PARAM> king_prefs specified an unresolvable URL."); }

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
        if(uiText != null)      uiText.shutdown();
        if(mainWin != null)     mainWin.shutdown();
        if(kinCanvas != null)   kinCanvas.shutdown();
        
        instanceCount--;
        if(instanceCount <= 0 && theApplet == null)
        {
            try { System.exit(0); } catch(Throwable t) {} //catch(SecurityException ex) {}
        }
    }
//}}}

//{{{ Main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        // This compensates for incorrect drawing primitives on Mac OS X
        //try { System.setProperty("apple.awt.antialiasing", "on"); }
        //catch(SecurityException ex) { SoftLog.err.println("Not allowed to activate antialiasing."); }
        // No effect -- must be done using -D from Java cmd line
        
        // It's just too hard to change this after we've already started up!
        float magnification = prefs.getFloat("fontMagnification");
        if(magnification != 1)
            javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new MagnifiedTheme(magnification));
        
        if(!SoftLog.replaceSystemStreams())
            SoftLog.err.println("Unable to subvert System.err; some exception traces may be lost.");
        
        kinStable   = new KinStable(this);
        if(theApplet == null || !isAppletFlat)
            mainWin = new MainWindow(this);     // doesn't create GUI yet, but other dlgs depend on this one
        contentPane = new ContentPane(this);    // doesn't create GUI yet
        kinIO       = new KinfileIO(this);      // progress dlg. references main window
        kinCanvas   = new KinCanvas(this);
        uiMenus     = new UIMenus(this);
        uiText      = new UIText(this);
        kinTree     = new KinTree(this);

        contentPane.buildGUI();
        if(theApplet == null || !isAppletFlat)
        {
            mainWin.setContentPane(contentPane);
            mainWin.setJMenuBar(uiMenus.getMenuBar());
            mainWin.addWindowListener(this);
            mainWin.pack();
            mainWin.setVisible(true);
            if(prefs.getBoolean("textOpenOnStart"))
                uiText.cascadeBehind(mainWin);
        }
        else
        {
            kinCanvas.setPreferredSize(null);   // so we don't crowd off other components
            kinCanvas.setMinimumSize(null);
            theApplet.setContentPane(contentPane);
            theApplet.setJMenuBar(uiMenus.getMenuBar());
            theApplet.validate();
            // make sure text window gets opened as needed
        }
        
        // Drag & Drop support for opening kinemages
        if(theApplet == null)
        {
            // Mac OS X only! - adds support for Drag & Drop to Dock icon and for program launch
            try {
                //MacDropTarget.bindTo(this);
                Class macDropClass = Class.forName("king.MacDropTarget");
                Method bindTo = macDropClass.getMethod("bindTo", new Class[] {KingMain.class});
                bindTo.invoke(null, new Object[] {this});
            } catch(Throwable t) {}
            // Java 1.4+ only! - adds support for Drag & Drop to KinCanvas
            try {
                //new FileDropHandler(this, kinCanvas);
                Class dropClass = Class.forName("king.FileDropHandler");
                Constructor dropConstr = dropClass.getConstructor(new Class[] { KingMain.class, KinCanvas.class });
                dropConstr.newInstance(new Object[] { this, kinCanvas });
            } catch(Throwable t) {}
        }

        if(theApplet == null)
            SwingUtilities.invokeLater(new ReflectiveRunnable(this, "loadFiles"));
        else
            SwingUtilities.invokeLater(new ReflectiveRunnable(this, "appletLoadFiles"));
    }
//}}}

//{{{ loadFiles
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void loadFiles()
    {
        if(filesToOpen == null || filesToOpen.size() <= 0) return;
        
        Kinemage kin = null;
        if(doMerge && filesToOpen.size() > 1) kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
        
        KinfileIO io = this.getKinIO();
        for(Iterator iter = filesToOpen.iterator(); iter.hasNext(); )
        { io.loadFile((File)iter.next(), kin); }
    
        if(kin != null) this.getStable().append(Arrays.asList(new Kinemage[] {kin}));
    }
//}}}

//{{{ appletLoadFiles
//##################################################################################################
    /**
    * File loading in event dispatch thread
    */
    public void appletLoadFiles()
    {
        KinCanvas kCanvas = this.getCanvas();
        ToolBox toolbox;
        if(kCanvas == null) toolbox = null;
        else toolbox = kCanvas.getToolBox();


        // Try multiple names for this parameter
        String kinsrc = theApplet.getParameter("kinSource");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kinFile");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kinURL");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kinemage");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kin");
        
        if(kinsrc != null)
        {
            try
            {
                URL kinURL = new URL(theApplet.getDocumentBase(), kinsrc);
                this.getKinIO().loadURL(kinURL, null);
            }
            catch(MalformedURLException ex)
            { SoftLog.err.println("<PARAM> kinSource specified an unresolvable URL."); }
        }

        // Try multiple names for this parameter
        boolean isOmap = false;
        String mapsrc = theApplet.getParameter("xmap");
        if(mapsrc == null) { mapsrc = theApplet.getParameter("omap"); isOmap = true; }
        
        if(mapsrc != null && toolbox != null)
        {
            try
            {
                URL mapURL = new URL(theApplet.getDocumentBase(), mapsrc);
                
                CrystalVertexSource map;
                if(isOmap)
                { map = new OMapVertexSource(mapURL.openStream()); }
                else
                { map = new XplorVertexSource(mapURL.openStream()); }
                
                new EDMapWindow(toolbox, map, mapURL.getFile());
            }
            catch(MalformedURLException ex)
            { SoftLog.err.println("<PARAM> xmap/omap specified an unresolvable URL."); }
            catch(IOException ex)
            {
                JOptionPane.showMessageDialog(this.getTopWindow(),
                    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace(SoftLog.err);
            }
            catch(IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(this.getTopWindow(),
                    "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace(SoftLog.err);
            }
        }
    }
//}}}

//{{{ signalKinemage, notifyChange
//##################################################################################################
    /**
    * A call to this method indicates that the specified
    * kinemage has changed somehow.
    *
    * <p>This method will be called in response to KinemageSignal.signalKinemage().
    *
    * @param kinemage   the Kinemage object that has changed
    * @param bitmask    a set of flags describing which properties have changed.
    */
    public void signalKinemage(Kinemage kinemage, int bitmask)
    {
        int evmask = 0;
        if((bitmask & APPEARANCE) != 0)
            evmask |= EM_ON_OFF | EM_DISPLAY | EM_NEWVIEW | EM_EDIT_FINE;
            //evmask |= EM_ON_OFF | EM_DISPLAY | EM_EDIT_FINE;
        if((bitmask & STRUCTURE) != 0)
            evmask |= EM_SWITCH; // force menus to be remade
        
        if(evmask != 0) this.notifyChange(evmask);
    }
    
    /**
    * Notifies all existing sub-components of a change to the state of the program.
    * Only notifies components directly owned by KingMain;
    * for instance KinCanvas is reponsible for propagating this to Engine.
    * @param event_mask the bitwise OR of the flag(s) representing the change
    */    
    public void notifyChange(int event_mask)
    {
        if(kinStable    != null) kinStable.notifyChange(event_mask);
        if(contentPane  != null) contentPane.notifyChange(event_mask);
        if(kinIO        != null) kinIO.notifyChange(event_mask);
        if(kinCanvas    != null) kinCanvas.notifyChange(event_mask);
        if(uiMenus      != null) uiMenus.notifyChange(event_mask);
        if(kinTree      != null) kinTree.notifyChange(event_mask);
    }
//}}}

//{{{ getXXX functions
//##################################################################################################
    /** Returns the object holding our content: either a JFrame or a JApplet. Never null. */
    public Container getContentContainer() { return (mainWin == null ? (Container)theApplet : (Container)mainWin); }
    
    /** Returns the ContentPane object that holds all the GUI elements. Never null. */
    public ContentPane getContentPane() { return contentPane; }
    
    /** Returns the top-level window, if there is one; null otherwise. */
    public Frame getTopWindow() { return mainWin; }
    
    /** Returns the data model that holds all data for this session (never null) */
    public KinStable getStable() { return kinStable; }
    
    /** Returns the kinemage reader/writer (never null) */
    public KinfileIO getKinIO() { return kinIO; }
    
    /** Returns the active drawing canvas (never null) */
    public KinCanvas getCanvas() { return kinCanvas; }
    
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
    
//{{{ parseArguments
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
                    SoftLog.err.println("Help not available. Sorry!");
                    System.exit(0);
                } else if(arg.equals("-version")) {
                    SoftLog.err.println("KingMain, version "+getPrefs().getString("version")+"\nCopyright (C) 2002-2003 by Ian W. Davis");
                    System.exit(0);
                } else if(arg.equals("-m") || arg.equals("-merge")) {
                    doMerge = true;
                } else if(arg.equals("-s") || arg.equals("-single")) {
                    doMerge = false;
                } else {
                    SoftLog.err.println("*** Unrecognized option: "+arg);
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
}//class
