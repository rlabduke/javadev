// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.io.KinfileParser;
import king.tool.export.PdfExport;

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
import javax.swing.plaf.metal.MetalLookAndFeel;
import driftwood.util.*;
import driftwood.isosurface.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>KingMain</code> is the control center of the King program.
*
* <p>Begun on Mon Apr 22 17:18:36 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis
*/
public class KingMain implements WindowListener
{
    static
    {
        // This allows JMenus to overlap the JOGL canvas, which stopped
        // happening automatically with the release of Java 1.5.
        // This should happen once, before any KingMains are created.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
    }
    
    public static void main(String[] args) { new KingMain(args).Main(); }
    
//{{{ Variables
//##################################################################################################
    // Used for counting # of clones still alive, so we don't
    // call System.exit() prematurely!
    static int instanceCount = 0;
    public static /*final*/ int MENU_ACCEL_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    KingPrefs           prefs           = null;
    KinStable           kinStable       = null;
    KinfileIO           kinIO           = null;
    FileDropHandler     dropHandler     = null;
    KinCanvas           kinCanvas       = null;
    UIMenus             uiMenus         = null;
    UIText              uiText          = null;
    KinTree             kinTree         = null;
    MainWindow          mainWin         = null;
    ContentPane         contentPane     = null;
    JApplet             theApplet       = null;
    boolean             isAppletFlat    = true;
    
    ArrayList<File>     filesToOpen     = null;
    ArrayList<File>     kinFilesToOpen  = null;
    ArrayList<File>     pdbFilesToOpen  = null;
    boolean             doMerge         = true;
    ArrayList<String>   pluginArgs      = null;
    
    boolean             doViewExport    = false;
    
    Set<KMessage.Subscriber> subscribers = new LinkedHashSet<KMessage.Subscriber>();
//}}}
    
//{{{ Constructors
//##################################################################################################
    /** Simple constructor for embedded apps */
    public KingMain() { this(new String[] {}); }
    
    /**
    * Constructor for application
    */
    public KingMain(String[] args)
    {
      pluginArgs = new ArrayList<String>();
        // This prevents number formatting problems when writing kins in
        // e.g. Germany. Kludgy, but KiNG isn't internationalized anyway.
        // Ideally, this will go away one day.
        try { Locale.setDefault(Locale.US); }
        catch(SecurityException ex) { SoftLog.err.println("Can't change to US locale; numbers may be garbled on kinemage write."); }
        
        prefs = new KingPrefs(false);
        if(prefs.getBoolean("checkNewVersion"))
        {
            // "Timeout" after 2.000 seconds
            try { prefs.checkVersion(new URL("http://kinemage.biochem.duke.edu/downloads/software/king/king2.version.props"), 2000); }
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
      pluginArgs = new ArrayList<String>();
        prefs           = new KingPrefs(true);
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

//{{{ createComponents
//##################################################################################################
    public void createComponents() { createComponents(true, true); }

    /**
    * Creates all the major components of a running KiNG instance:
    * KinStable, ContentPane, KinfileIO, KinCanvas, UIMenus, UIText, KinTree.
    * Call this after the constructor but before trying to assemble the overall GUI.
    */
    public void createComponents(boolean useButtons, boolean useSliders)
    {
        kinStable   = new KinStable(this);
        contentPane = new ContentPane(this);    // doesn't create GUI yet
        kinIO       = new KinfileIO(this);      // progress dlg. references main window
        dropHandler = new FileDropHandler(this);
        kinCanvas   = new KinCanvas(this);
        uiMenus     = new UIMenus(this);
        uiText      = new UIText(this);
        kinTree     = new KinTree(this);
        
        contentPane.buildGUI(useButtons, useSliders);
    }
//}}}

//{{{ shutdown
//##################################################################################################
    /**
    * Initiates shutdown, albeit in a crude way. Called by Kinglet.stop() and the window close listeners.
    */
    public void shutdown()
    {
        if(mainWin != null) mainWin.shutdown();
        this.publish(new KMessage(this, KMessage.KING_SHUTDOWN));
        
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
        
        // CTRL-x shortcuts are still useful in a Mac browser.
        // There's no good option for Windows / Linux broswers though.
        if(isApplet()) MENU_ACCEL_MASK = Event.CTRL_MASK;
        
        // Start in a reasonable directory if launched by double-click / drag-n-drop
        try {
            if(System.getProperty("user.dir").equals("/Applications"))
                System.setProperty("user.dir", System.getProperty("user.home"));
            //System.err.println("Current dir: "+System.getProperty("user.dir"));
        } catch(Exception ex) {}//{ ex.printStackTrace(); }
        
        // It's just too hard to change this after we've already started up!
        float magnification = prefs.getFloat("fontMagnification");
        if(magnification != 1)
        {
            MetalLookAndFeel.setCurrentTheme(new MagnifiedTheme(magnification));
            // This forces initialization of the LAF, which keeps Java 1.5
            // from replacing our theme with their "Ocean" theme.
            try { UIManager.setLookAndFeel( UIManager.getLookAndFeel() ); }
            catch(Exception ex) { ex.printStackTrace(); }
        }
        
        if(!SoftLog.replaceSystemStreams())
            if (theApplet == null) SoftLog.err.println("Unable to subvert System.err; some exception traces may be lost.");
        
        if(theApplet == null || !isAppletFlat)
            mainWin = new MainWindow(this); // doesn't create GUI yet, but other dlgs may depend on this one (?)
        createComponents(); // actually creates most of the stuff KiNG uses
        
        if(doViewExport) {
        } else if(theApplet == null || !isAppletFlat)
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
        
        // Mac OS X only! - adds support for Drag & Drop to Dock icon and for program launch
        try {
            //MacDropTarget.bindTo(this);
            Class macDropClass = Class.forName("king.MacDropTarget");
            Method bindTo = macDropClass.getMethod("bindTo", new Class[] {KingMain.class});
            bindTo.invoke(null, new Object[] {this});
        } catch(Throwable t) {}

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

      PdfExport pdfExportPlugin = null;

      if((filesToOpen != null && filesToOpen.size() > 0)||(pdbFilesToOpen != null && pdbFilesToOpen.size() > 0)||(kinFilesToOpen != null && kinFilesToOpen.size() > 0))
        {
          Kinemage kin = null;
          if(doMerge && kinFilesToOpen.size() > 1)
            kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
          for(File f : kinFilesToOpen) 
            kinIO.loadFile(f, kin);
          
          if(kin != null) this.getStable().append(Arrays.asList(new Kinemage[] {kin}));
          
          Collection plugins = kinCanvas.toolbox.getPluginList();
          Iterator iter = plugins.iterator();
          if (pdbFilesToOpen != null && pdbFilesToOpen.size() > 0) {
            while (iter.hasNext()) {
              Plugin plug = (Plugin) iter.next();
              plug.loadFileFromCmdline(pdbFilesToOpen, pluginArgs);
            }
          }
          // the reason I scan through plugins twice is so pdb files get processed
          // first in order to make sure a kin is available before anything else.
          iter = plugins.iterator();
          while (iter.hasNext()) {
            Plugin plug = (Plugin) iter.next();
            if (plug instanceof PdfExport) {
              pdfExportPlugin = (PdfExport) plug;
            }
            plug.loadFileFromCmdline(filesToOpen, pluginArgs);
          }
        }
        
        this.publish(new KMessage(this, KMessage.KING_STARTUP));

        if (doViewExport) {
          if (kinFilesToOpen.size() == 1 && pdbFilesToOpen.size() == 0) {
            //this.getStable().changeCurrentKinemage(1);
            String kinFileName = kinFilesToOpen.get(0).getName();
            String kinFileNameNoExtension = kinFileName;
            if(kinFileNameNoExtension != null && kinFileNameNoExtension.contains(".")) kinFileNameNoExtension.substring(0, kinFileNameNoExtension.lastIndexOf('.'));
            Kinemage viewKin = this.getStable().getKinemage();
            ArrayList<KView> views = new ArrayList(viewKin.getViewList());
            for (int view_count = 0; view_count < views.size(); view_count++) {
              KView view = views.get(view_count);
              this.setView(view);
              try {
                File viewPdf = null;
                if (!doMerge) {
                  String fixedViewName = view.getSafeFileName();
                  viewPdf = new File(fixedViewName+".pdf");
                  int i = 0;
                  while(viewPdf.exists()) {
                    viewPdf = new File(fixedViewName+Integer.toString(i)+".pdf");
                    i++;
                  }
                } else {
                  viewPdf = new File(kinFileNameNoExtension+"Views.pdf");
                }
                String currentTime = Times.getCurrentTimeString();
                pdfExportPlugin.exportPDF(this.getCanvas(), false, viewPdf, new Dimension(1024, 1024), viewKin.getName()+" | View #"+(view_count+1)+": "+view, kinFileName+" | "+currentTime);
              } catch (IOException ex){
                System.out.println (ex.toString());
              }
            }
            System.exit(0);
          } else {
            System.err.println("Only one kinemage file may have views exported to images at a time");
            System.exit(0);
          }
        }
    }
//}}}

//{{{ appletLoadFiles, getAppletKinURL
//##################################################################################################
    /**
    * File loading in event dispatch thread
    */
    public void appletLoadFiles()
    {
        try
        {
            URL kinURL = getAppletKinURL();
            if(kinURL != null) this.getKinIO().loadURL(kinURL, null);
            URL[] kinList = getAppletKinURLList();
            if(kinList != null) this.getKinIO().loadURLs(kinList);
        }
        catch(MalformedURLException ex)
        { SoftLog.err.println("<PARAM> kinSource specified an unresolvable URL."); }
        
        try {
          URL pdbURL = getAppletPdbURL();
          if (pdbURL != null) {
            Collection plugins = kinCanvas.toolbox.getPluginList();
            Iterator iter = plugins.iterator();
            while (iter.hasNext()) {
              Plugin plug = (Plugin) iter.next();
              try {
                plug.loadFromURL(pdbURL);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        } catch(MalformedURLException ex)
        { SoftLog.err.println("<PARAM> pdbSource specified an unresolvable URL."); }

        this.publish(new KMessage(this, KMessage.KING_STARTUP));
    }
    
    /** Returns the URL of the primary kinemage this applet was invoked to show, or null for none. */
    public URL getAppletKinURL() throws MalformedURLException
    {
        // Try multiple names for this parameter
        String kinsrc = theApplet.getParameter("kinSource");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kinFile");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kinURL");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kinemage");
        if(kinsrc == null) kinsrc = theApplet.getParameter("kin");
        
        if(kinsrc != null) return new URL(theApplet.getDocumentBase(), kinsrc);
        else return null;
    }
    
    public URL[] getAppletKinURLList() throws MalformedURLException {
      String kinBase = theApplet.getParameter("kinfileBase");
      String kins = theApplet.getParameter("kinfileList");
      if(kins != null && kinBase != null)
      {
        String[] kinlist = Strings.explode(kins, ' ');
        URL[] urllist = new URL[kinlist.length];
        for (int i = 0; i < kinlist.length; i++) {
          urllist[i] = new URL(theApplet.getDocumentBase(), kinBase+"/"+kinlist[i]);
        }
        return urllist;
      }
      return null;
    }
    
    public URL getAppletPdbURL() throws MalformedURLException {
      String pdbsrc = theApplet.getParameter("pdbSource");
      if(pdbsrc == null) pdbsrc = theApplet.getParameter("pdbFile");
      if(pdbsrc == null) pdbsrc = theApplet.getParameter("pdbURL");
      if(pdbsrc == null) pdbsrc = theApplet.getParameter("pdb");
      
      if(pdbsrc != null) return new URL(theApplet.getDocumentBase(), pdbsrc);
      else return null;
    }
//}}}

//{{{ publish, (un)subscribe
//##################################################################################################
    /** Sign up to receive event messages from KiNG */
    public void subscribe(KMessage.Subscriber listener)
    { subscribers.add(listener); }
    
    /** Stop receiving event messages from KiNG */
    public void unsubscribe(KMessage.Subscriber listener)
    { subscribers.remove(listener); }
    
    /**
    * Distribute the message to all current subscribers.
    * This method runs synchronously, in the current Thread:
    * it does not return until all subscribers have reacted.
    */
    public void publish(KMessage msg)
    {
        // I hate to do this for the overhead (which is in reality probably quite small),
        // but it's necessary to avoid ConcurrentModificationExceptions if someone
        // tries to unsubscribe as a result of receiving an event (like EDMapPlugin does).
        KMessage.Subscriber[] s = subscribers.toArray(new KMessage.Subscriber[subscribers.size()]);
        for(KMessage.Subscriber subscriber : s)
            subscriber.deliverMessage(msg);
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
    
    /** Returns the drag-n-drop handler for files (never null) */
    public FileDropHandler getFileDropHandler() { return dropHandler; }
    
    /** Returns the active drawing canvas (never null) */
    public KinCanvas getCanvas() { return kinCanvas; }
    
    /** Returns the collection of UI actions and menus that manage user input (may be null) */
    public UIMenus getMenus() { return uiMenus; }
    
    /** Returns the text storage/edit/display system (never null) */
    public UIText getTextWindow() { return uiText; }
    
    /** Returns the tree controller (may be null) */
    public KinTree getKinTree() { return kinTree; }
    
    /** Returns the preferences storage object */
    public KingPrefs getPrefs() { return prefs; }

    /** Convenience function for getStable().getKinemage() (may be null) */
    public Kinemage getKinemage()
    {
        return kinStable.getKinemage();
    }

    /** Convenience function for getCanvas().getCurrentView() (may be null) */
    public KView getView()
    {
        if(kinCanvas == null) return null;
        else return kinCanvas.getCurrentView();
    }
    /** Convenience for getCanvas().setCurrentView() */
    public void setView(KView view)
    { if(kinCanvas != null) kinCanvas.setCurrentView(view); }
    
    /** Returns the applet this was spawned from (may be null) */
    public JApplet getApplet() { return theApplet; }
    
    /**
    * Returns true if KiNG is running as an applet.
    * However, it could be a *trusted* applet, so you are probably
    * are more interested in isTrusted() instead.
    */
    public boolean isApplet()
    { return getApplet() != null; }
    
    /**
    * Returns true if this code is allowed to access the filesystem,
    * open arbitrary URLs, etc -- all the things (unsigned) applets
    * can't do and ordinary Java desktop applications can.
    */
    public boolean isTrusted()
    {
        // need a better implementation later
        //return isApplet();
        
        // I'm not sure whether signed applets get AllPermission or not.
        // I know that applications do with Sun's Java 1.5.x
        SecurityManager sm = System.getSecurityManager();
        if(sm == null) return true;
        try { sm.checkPermission(new java.security.AllPermission()); }
        catch(SecurityException ex) { return false; }
        return true;
    }
//}}}
    
//{{{ parseArguments
//##################################################################################################
    // Interpret command-line arguments
    void parseArguments(String[] args)
    {
        SuffixFileFilter pdbFilter = CoordinateFile.getCoordFileFilter();
        SuffixFileFilter kinFilter = Kinemage.getKinFileFilter();
        
        filesToOpen = new ArrayList<File>();
        kinFilesToOpen = new ArrayList<File>();
        pdbFilesToOpen = new ArrayList<File>();
        
        String arg;
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            // this is an option
            if(arg.startsWith("-"))
            {
                if(arg.equals("-h") || arg.equals("-help")) {
                  //SoftLog.err.println("Possible arguments for KiNG:");
                  showHelp();
                    System.exit(0);
                } else if(arg.equals("-version")) {
                    SoftLog.err.println("KingMain, version "+getPrefs().getString("version")+"\nCopyright (C) 2002-2011 by Ian W. Davis");
                    System.exit(0);
                } else if(arg.equals("-m") || arg.equals("-merge")) {
                    doMerge = true;
                    pluginArgs.add(arg);
                } else if(arg.equals("-s") || arg.equals("-single")) {
                    doMerge = false;
                } else if(arg.equals("-phenix")) {
                  pluginArgs.add(arg);
                } else if(arg.equals("-exportviews")) {
                  doViewExport = true;
                } else {
                    SoftLog.err.println("*** Unrecognized option: "+arg);
                }
            }
            // this is a file, etc.
            else
            {
                if (pdbFilter.accept(arg)) 
                  pdbFilesToOpen.add(new File(arg));
                else if (kinFilter.accept(arg))
                  kinFilesToOpen.add(new File(arg));
                else 
                  filesToOpen.add(new File(arg));
            }
        }
    }
//}}}

  //{{{ showHelp
  /**
  * Parse the command-line options for this program.
  * @param args the command-line options, as received by main()
  * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
  *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
  */
  // Display help information
  void showHelp()
  {
    InputStream is = getClass().getResourceAsStream("king.help");
    if(is == null)
      System.err.println("\n*** Unable to locate help information in 'king.help' ***\n");
    else
    {
      try { streamcopy(is, System.out); }
      catch(IOException ex) { ex.printStackTrace(); }
    }
    System.err.println("Copyright (C) 2002-2011 by IWD, VBC, DAK. All rights reserved.");
  }
  
  // Copies src to dst until we hit EOF
  void streamcopy(InputStream src, OutputStream dst) throws IOException
  {
    byte[] buffer = new byte[2048];
    int len;
    while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
  }
  //}}}

//{{{ Window events
//##################################################################################################
    public void windowActivated(WindowEvent ev)   {}
    public void windowClosed(WindowEvent ev)      {}
    public void windowClosing(WindowEvent ev)
    {
        if(uiMenus != null)     uiMenus.onFileExit(null);
        else                    shutdown();
    }
    public void windowDeactivated(WindowEvent ev) {}
    public void windowDeiconified(WindowEvent ev) {}
    public void windowIconified(WindowEvent ev)   {}
    public void windowOpened(WindowEvent ev)      {}
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class
