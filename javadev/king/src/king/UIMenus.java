// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ package, imports
package king;
import king.core.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>UIMenus</code> contains all the menus and their corresponding
* Action objects for various actions the user can take.
*
* <p>Begun on Sat Apr 27 20:34:46 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class UIMenus //extends ... implements ...
{
    static final DecimalFormat df = new DecimalFormat("###,###,##0");
    public static final int MENU_ACCEL_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    JMenuBar menubar;
    
    UIDisplayMenu   displayMenu;
    
    // Tool-like objects
    JFileChooser fileChooser = null;
    SuffixFileFilter fileFilter;
    PointFinder finder;
    ViewEditor viewEditor;
    PrefsEditor prefsEditor;
    
    // Timers, etc for auto-xxx functions
    javax.swing.Timer autoAnimTimer = null;
    
    // Elements of menus that get rebuilt frequently
    JMenu oldViewMenu = null;
    JMenu oldAnimMenu = null;
    JCheckBoxMenuItem autoAnimMenuItem = null;
//}}}
    
//{{{ Constructor, getMenuBar()
//##################################################################################################
    public UIMenus(KingMain kmain)
    {
        kMain = kmain;
        
        // Will throw an exception if we're running as an Applet
        try
        {
            fileFilter = new SuffixFileFilter("Kinemage files (*.kin)");
            fileFilter.addSuffix(".kin");
            fileFilter.addSuffix(".kip");
            fileFilter.addSuffix(".kip1");
            fileFilter.addSuffix(".kip2");
            fileFilter.addSuffix(".kip3");
            fileFilter.addSuffix(".kip4");
            fileFilter.addSuffix(".kip5");
            fileFilter.addSuffix(".kip6");
            fileFilter.addSuffix(".kip7");
            fileFilter.addSuffix(".kip8");
            fileFilter.addSuffix(".kip9");
            
            fileChooser = new JFileChooser();
            String currdir = System.getProperty("user.dir");
            if(currdir != null) fileChooser.setCurrentDirectory(new File(currdir));
            fileChooser.addChoosableFileFilter(fileFilter);
            fileChooser.setFileFilter(fileFilter);
        }
        catch(SecurityException ex) {}

        finder = new PointFinder(kMain);
        viewEditor = new ViewEditor(kMain);
        prefsEditor = new PrefsEditor(kMain);
        
        autoAnimTimer = new javax.swing.Timer(kMain.prefs.getInt("autoAnimateDelay"), new ReflectiveAction(null, null, this, "onAnimForward"));
        autoAnimTimer.setRepeats(true);
        autoAnimTimer.setCoalesce(true);

        buildMenus();
    }

    /** Returns the menu bar */
    public JMenuBar getMenuBar() { return menubar; }
//}}}
    
//{{{ buildMenus()
//##################################################################################################
    // Construct all the menus and their actions for the menubar.
    void buildMenus()
    {
        menubar = new JMenuBar();
        JMenu menu, submenu;
        JMenuItem item;
        JCheckBoxMenuItem cbitem;
        KinCanvas kCanvas;
        
        //{{{ File menu
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menubar.add(menu);
        if(kMain.getApplet() == null) // => not in an applet
        {
            item = new JMenuItem(new ReflectiveAction("New KiNG window", null, this, "onFileNewKing"));
            item.setMnemonic(KeyEvent.VK_N);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_ACCEL_MASK));
            menu.add(item);
            menu.addSeparator();
            item = new JMenuItem(new ReflectiveAction("Open...", null, this, "onFileOpen"));
            item.setMnemonic(KeyEvent.VK_O);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_ACCEL_MASK));
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Append...", null, this, "onFileMerge"));
            item.setMnemonic(KeyEvent.VK_A);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, MENU_ACCEL_MASK));
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Close", null, this, "onFileClose"));
            item.setMnemonic(KeyEvent.VK_C);
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Close all", null, this, "onFileCloseAll"));
            item.setMnemonic(KeyEvent.VK_L);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, MENU_ACCEL_MASK));
            menu.add(item);
            menu.addSeparator();
            item = new JMenuItem(new ReflectiveAction("Save as...", null, this, "onFileSaveAs"));
            item.setMnemonic(KeyEvent.VK_S);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_ACCEL_MASK));
            menu.add(item);
            submenu = new JMenu("Export");
            submenu.setMnemonic(KeyEvent.VK_E);
            menu.add(submenu);
                item = new JMenuItem(new ReflectiveAction("As JPEG or PNG...", null, this, "onFileWriteImage"));
                item.setMnemonic(KeyEvent.VK_J);
                //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_ACCEL_MASK));
                submenu.add(item);
                item = new JMenuItem(new ReflectiveAction("As PDF document...", null, this, "onFileWritePDF"));
                item.setMnemonic(KeyEvent.VK_P);
                //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_ACCEL_MASK));
                submenu.add(item);
                item = new JMenuItem(new ReflectiveAction("As Kin-XML...", null, this, "onFileWriteXML"));
                item.setMnemonic(KeyEvent.VK_X);
                //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_ACCEL_MASK));
                submenu.add(item);
                item = new JMenuItem(new ReflectiveAction("As shown to VRML 2...", null, this, "onFileWriteVRML"));
                item.setMnemonic(KeyEvent.VK_V);
                //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_ACCEL_MASK));
                submenu.add(item);
        }
        // This might throw a SecurityException, if the user denies us permission...
        item = new JMenuItem(new ReflectiveAction("Print...", null, this, "onFilePrint"));
        item.setMnemonic(KeyEvent.VK_P);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, MENU_ACCEL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Exit", null, this, "onFileExit"));
        item.setMnemonic(KeyEvent.VK_X);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_ACCEL_MASK));
        menu.add(item);
        //}}}
        
        //{{{ Edit menu
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menubar.add(menu);
        item = new JMenuItem(new ReflectiveAction("Find point...", null, this, "onEditFind"));
        item.setMnemonic(KeyEvent.VK_F);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Find next", null, this, "onEditFindNext"));
        item.setMnemonic(KeyEvent.VK_N);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, MENU_ACCEL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Kinemage properties...", null, this, "onEditKinProps"));
        item.setMnemonic(KeyEvent.VK_K);
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Edit text...", null, this, "onEditText"));
        item.setMnemonic(KeyEvent.VK_T);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Edit hierarchy...", null, this, "onEditHierarchy"));
        item.setMnemonic(KeyEvent.VK_H);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, MENU_ACCEL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Configure KiNG...", null, this, "onEditConfigure"));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);
        //}}}
        
        // Views menu
        rebuildViewsMenu(null);
        
        // Animations menu
        rebuildAnimationsMenu(null);
        
        // Display menu
        displayMenu = new UIDisplayMenu(kMain);
        menubar.add(displayMenu.getMenu());
        
        //{{{ Tools menu
        menu = new JMenu("Tools");
        menu.setMnemonic(KeyEvent.VK_T);
        menubar.add(menu);
        cbitem = kMain.getCanvas().getToolBox().services.doFlatland;
        cbitem.setMnemonic(KeyEvent.VK_L);
        cbitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
        menu.add(cbitem);
        cbitem = kMain.getCanvas().getToolBox().services.doXYZ;
        cbitem.setMnemonic(KeyEvent.VK_X);
        //cbitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, MENU_ACCEL_MASK));
        menu.add(cbitem);
        cbitem = kMain.getCanvas().getToolBox().services.doMeasureAll;
        cbitem.setMnemonic(KeyEvent.VK_M);
        cbitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
        menu.add(cbitem);
        cbitem = kMain.getCanvas().getToolBox().services.doSuperpick;
        cbitem.setMnemonic(KeyEvent.VK_S);
        menu.add(cbitem);
        kCanvas = kMain.getCanvas();
        if(kCanvas != null)
        {
            ToolBox tb = kCanvas.getToolBox();
            if(tb != null)
            {
                menu.addSeparator();
                tb.addToolsToToolsMenu(menu);
                menu.addSeparator();
                tb.addPluginsToToolsMenu(menu);
            }
        }
        //}}}
        
        //{{{ Help menu
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menubar.add(menu);
        item = new JMenuItem(new ReflectiveAction("User manual...", null, this, "onHelpManual"));
        item.setMnemonic(KeyEvent.VK_M);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)); // 0 => no modifiers
        menu.add(item);
        submenu = new JMenu("Tools");
        submenu.setMnemonic(KeyEvent.VK_T);
        menu.add(submenu);
            kCanvas = kMain.getCanvas();
            if(kCanvas != null)
            {
                ToolBox tb = kCanvas.getToolBox();
                if(tb != null)
                {
                    tb.addToolsToHelpMenu(submenu);
                    submenu.addSeparator();
                    tb.addPluginsToHelpMenu(submenu);
                }
            }
        item = new JMenuItem(new ReflectiveAction("Keyboard shortcuts...", null, this, "onHelpKeyboardShortcuts"));
        item.setMnemonic(KeyEvent.VK_S);
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)); // 0 => no modifiers
        menu.add(item);
        submenu = new JMenu("Built-in kinemages");
        submenu.setMnemonic(KeyEvent.VK_K);
        menu.add(submenu);
            item = new JMenuItem(new ReflectiveAction("Internal palette", null, this, "onHelpKinPalette"));
            item.setMnemonic(KeyEvent.VK_P);
            submenu.add(item);
            item = new JMenuItem(new ReflectiveAction("Color cone", null, this, "onHelpKinCone"));
            item.setMnemonic(KeyEvent.VK_C);
            submenu.add(item);
            item = new JMenuItem(new ReflectiveAction("Falling teddy bear", null, this, "onHelpKinBear"));
            item.setMnemonic(KeyEvent.VK_B);
            submenu.add(item);
        item = new JMenuItem(new ReflectiveAction("Error log...", null, this, "onHelpLog"));
        item.setMnemonic(KeyEvent.VK_E);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MENU_ACCEL_MASK)); // 0 => no modifiers
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("About KiNG...", null, this, "onHelpAbout"));
        item.setMnemonic(KeyEvent.VK_A);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, MENU_ACCEL_MASK)); // 0 => no modifiers
        menu.add(item);
        //}}}
    }
//}}}

//{{{ rebuildViewsMenu
//##################################################################################################
    /**
    * Creates a new Views menu from the specified iterator of KingView objects
    * @param viewiter Iterator of KingViews, or null for an empty menu
    */
    public void rebuildViewsMenu(Iterator viewiter)
    {
        JMenu menu = new JMenu("Views");
        menu.setMnemonic(KeyEvent.VK_V);
        JMenuItem item;
        JRadioButtonMenuItem ritem;
        ButtonGroup rgroup = new ButtonGroup();

        if(viewiter != null)
        {
            item = new JMenuItem(new ReflectiveAction("Save current view", null, this, "onViewSave"));
            item.setMnemonic(KeyEvent.VK_S);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, MENU_ACCEL_MASK));
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Edit saved views...", null, this, "onViewEdit"));
            item.setMnemonic(KeyEvent.VK_E);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, MENU_ACCEL_MASK | KeyEvent.SHIFT_MASK));
            menu.add(item);
            menu.addSeparator();
            
            JMenu currMenu = menu;
            for(int i = 1; viewiter.hasNext(); i++)
            {
                // Every 25 views, chain them into a new menu
                if(i != 1 && i % 25 == 1)
                {
                    JMenu newMenu = new JMenu("More views");
                    currMenu.add(newMenu);
                    currMenu = newMenu;
                }
                KingView view = (KingView)viewiter.next();
                ritem = new JRadioButtonMenuItem(new ReflectiveAction((i+" "+view.toString()), null, view, "selectedFromMenu"));
                rgroup.add(ritem);
                currMenu.add(ritem);
            }
        }
        else
        {
            item = new JMenuItem("No views available");
            item.setEnabled(false);
            menu.add(item);
        }
        
        if(oldViewMenu != null)
        {
            int viewIndex = menubar.getComponentIndex(oldViewMenu);
            menubar.remove(oldViewMenu);
            menubar.add(menu, viewIndex);
        }
        else
        {
            menubar.add(menu);
        }
        
        // This step is ESSENTIAL for the menu to appear & keep working!
        menubar.revalidate();
        
        oldViewMenu = menu;
    }
//}}}

//{{{ rebuildAnimationsMenu
//##################################################################################################
    /**
    * Creates a new Animations menu from the specified iterator of AnimationGroup objects
    * @param animiter Iterator of AnimationGroups, or null for an empty menu
    */
    public void rebuildAnimationsMenu(Iterator animiter)
    {
        JMenu menu = new JMenu("Animations");
        menu.setMnemonic(KeyEvent.VK_A);
        JMenuItem item;
        JRadioButtonMenuItem ritem;
        ButtonGroup rgroup = new ButtonGroup();

        if(animiter != null)
        {
            item = new JMenuItem(new ReflectiveAction("Step forward", null, this, "onAnimForward"));
            item.setMnemonic(KeyEvent.VK_F);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0)); // 0 => no modifiers
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Step backward", null, this, "onAnimBackward"));
            item.setMnemonic(KeyEvent.VK_B);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_MASK));
            menu.add(item);
            autoAnimMenuItem = new JCheckBoxMenuItem(new ReflectiveAction("Auto-animate", null, this, "onAnimAutoStartStop"));
            autoAnimMenuItem.setMnemonic(KeyEvent.VK_A);
            autoAnimMenuItem.setSelected(autoAnimTimer.isRunning());
            menu.add(autoAnimMenuItem);
            menu.addSeparator();
            
            for(int i = 1; animiter.hasNext(); i++)
            {
                AnimationGroup anim = (AnimationGroup)animiter.next();
                ritem = new JRadioButtonMenuItem(new ReflectiveAction((i+" "+anim.toString()), null, anim, "selectedFromMenu"));
                rgroup.add(ritem);
                menu.add(ritem);
            }
        }
        else
        {
            item = new JMenuItem("No animations available");
            // do this or there's a big memory leak (why?)
            // probably links back to parent menu, which
            // linked to AnimationGroups in the kinemage
            autoAnimMenuItem = null;
            item.setEnabled(false);
            menu.add(item);
        }
        
        if(oldAnimMenu != null)
        {
            int animIndex = menubar.getComponentIndex(oldAnimMenu);
            menubar.remove(oldAnimMenu);
            menubar.add(menu, animIndex);
        }
        else
        {
            menubar.add(menu);
        }
        
        // This step is ESSENTIAL for the menu to appear & keep working!
        menubar.revalidate();
        
        oldAnimMenu = menu;
    }
//}}}

//{{{ reporter() -- the dummy action
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void reporter(ActionEvent ev)
    {
        JOptionPane.showMessageDialog(kMain.getTopWindow(), "This feature has not been implemented yet.", "Sorry!", JOptionPane.INFORMATION_MESSAGE);
    }
//}}}

//{{{ onFileXXX handlers
//##################################################################################################
//### "File" functions #############################################################################
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileNewKing(ActionEvent ev)
    {
        new KingMain(new String[] {}).Main();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileOpen(ActionEvent ev)
    {
        KinfileIO io = kMain.getKinIO();
        io.askLoadFile(null);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileMerge(ActionEvent ev)
    {
        KinfileIO io = kMain.getKinIO();
        Kinemage kin = kMain.getKinemage();
        if(kin == null)
        {
            kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
            if(io.askLoadFile(kin))
                kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));
            // This way we don't create an empty if append is canceled
        }
        else io.askLoadFile(kin);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileClose(ActionEvent ev)
    {
        kMain.getStable().closeCurrent();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileCloseAll(ActionEvent ev)
    {
        kMain.getStable().closeAll();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileSaveAs(ActionEvent ev)
    {
        KinfileIO io = kMain.getKinIO();
        io.askSaveFile();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileWriteXML(ActionEvent ev)
    {
        //reporter(ev);
        if(fileChooser.showSaveDialog(kMain.getTopWindow()) == fileChooser.APPROVE_OPTION)
        {
            File f = fileChooser.getSelectedFile();
            if( !f.exists() ||
                JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                    "This file exists -- do you want to overwrite it?",
                    "Overwrite file?", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION )
            {
                try
                {
                    // We don't want any hard references to XML stuff so we
                    // can load those JARs lazily, especially for applets
                    // over the network...
                    //==> XknWriter xw = new XknWriter(kMain);
                    Class xknClass = Class.forName("king.XknWriter");
                    Constructor xknConstr = xknClass.getConstructor(new Class[] {KingMain.class});
                    Object xw = xknConstr.newInstance(new Object[] {kMain});
                    
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                    
                    //==> xw.save(os);
                    Method xknSave = xknClass.getMethod("save", new Class[] {OutputStream.class});
                    xknSave.invoke(xw, new Object[] {os});
                    
                    os.close();
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file.",
                        "Sorry!",
                        JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
                catch(Throwable t)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "Your version of Java appears to be missing the needed XML libraries. File was not written.",
                        "Sorry!",
                        JOptionPane.ERROR_MESSAGE);
                    t.printStackTrace(SoftLog.err);
                }
            }
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileWriteVRML(ActionEvent ev)
    {
        if(fileChooser.showSaveDialog(kMain.getTopWindow()) == fileChooser.APPROVE_OPTION)
        {
            File f = fileChooser.getSelectedFile();
            if( !f.exists() ||
                JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                    "This file exists -- do you want to overwrite it?",
                    "Overwrite file?", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION )
            {
                try
                {
                    Writer w = new BufferedWriter(new FileWriter(f));
                    Vrml97Writer vrml = new Vrml97Writer(kMain);
                    
                    vrml.save(w);
                    w.close();
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file.",
                        "Sorry!",
                        JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
            }
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileWriteImage(ActionEvent ev)
    {
        // This needs to be done via reflection because
        // image writing is only supported in Java 1.4+
        //      ImageExport imgex = new ImageExport();
        //      imgex.askExport(kMain);
        try
        {
            Class       imgexClass  = Class.forName("king.ImageExport");
            Constructor imgexConstr = imgexClass.getConstructor(new Class[] {});
            Object      imgex       = imgexConstr.newInstance(new Object[] {});
            
            Method      imgexExport = imgexClass.getMethod("askExport", new Class[] {KingMain.class});
            imgexExport.invoke(imgex, new Object[] {kMain});
        }
        catch(Throwable t)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "Writing images requires a newer version of Java (1.4 or later).",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            t.printStackTrace(SoftLog.err);
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileWritePDF(ActionEvent ev)
    {
        // This needs to be done via reflection because
        // PDF writing resides in an optional external library.
        try
        {
            Class       pdfexClass  = Class.forName("king.PdfExport");
            Constructor pdfexConstr = pdfexClass.getConstructor(new Class[] {});
            Object      pdfex       = pdfexConstr.newInstance(new Object[] {});
            
            Method      pdfexExport = pdfexClass.getMethod("askExport", new Class[] {KingMain.class});
            pdfexExport.invoke(pdfex, new Object[] {kMain});
        }
        catch(Throwable t)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "Writing images requires the iText library.\n"
                +"Download it from http://www.lowagie.com/iText/.\n"
                +"Call it itext.jar and place it with king.jar.",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            t.printStackTrace(SoftLog.err);
        }
    }
    
    // This might throw a SecurityException, if the user denies us permission...
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFilePrint(ActionEvent ev)
    {
        try
        {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(kMain.getCanvas());
            if(job.printDialog()) job.print();
        }
        catch(Exception ex) { ex.printStackTrace(SoftLog.err); }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileExit(ActionEvent ev)
    {
        kMain.shutdown();
    }
//}}}

//{{{ onEditXXX handlers
//##################################################################################################
//### "Edit" functions #############################################################################
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditFind(ActionEvent ev)
    {
        finder.show();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditFindNext(ActionEvent ev)
    {
        // If we can't find a next point, offer to search again.
        if( finder.findNext() == false )
            onEditFind(ev);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditText(ActionEvent ev)
    {
        UIText win = kMain.getTextWindow();
        if(win != null) win.onPopupButton(null);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditKinProps(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        GroupEditor groupEd = new GroupEditor(kMain, kMain.getTopWindow());
        groupEd.editKinemage(kin);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditHierarchy(ActionEvent ev)
    {
        KinTree win = kMain.getKinTree();
        if(win != null) win.show();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditConfigure(ActionEvent ev)
    {
        prefsEditor.edit();
    }
//}}}

//{{{ onViewXXX handlers
//##################################################################################################
//### "Views" functions ############################################################################
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onViewSave(ActionEvent ev)
    {
        String viewname = JOptionPane.showInputDialog(kMain.getTopWindow(),
            "Name for this view:",
            "Save view",
            JOptionPane.PLAIN_MESSAGE);
        if(viewname == null) return;
        KingView view = kMain.getView();
        if(view == null) return;
        view = (KingView)view.clone();
        view.setID(viewname);
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.addView(view);
        rebuildViewsMenu(kin.getViewIterator());
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onViewEdit(ActionEvent ev)
    {
        viewEditor.editViews();
    }
//}}}

//{{{ onAnimXXX handlers
//##################################################################################################
//### "Animation" functions ########################################################################
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnimForward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null)
        {
            AnimationGroup a = k.getCurrentAnimation();
            if(a != null) a.forward();
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnimBackward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null)
        {
            AnimationGroup a = k.getCurrentAnimation();
            if(a != null) a.backward();
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnimAutoStartStop(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null)
        {
            AnimationGroup a = k.getCurrentAnimation();
            if(a != null)
            {
                if(autoAnimMenuItem.isSelected())   autoAnimTimer.start();
                else                                autoAnimTimer.stop();
            }
        }
    }
//}}}

//{{{ onHelpXXX handlers
//##################################################################################################
//### "Help" functions ############################################################################
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelpManual(ActionEvent ev)
    {
        URL start = getClass().getResource("html/king-manual.html");
        if(start != null) new HTMLHelp(kMain, start).show();
        else SoftLog.err.println("Couldn't find the specified resource!");
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelpKeyboardShortcuts(ActionEvent ev)
    {
        URL start = getClass().getResource("html/kbd-shortcuts.html");
        if(start != null) new HTMLHelp(kMain, start).show();
        else SoftLog.err.println("Couldn't find the specified resource!");
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelpKinPalette(ActionEvent ev)
    {
        URL palkin = getClass().getResource("kins/pal5.kin");
        if(palkin != null) kMain.getKinIO().loadURL(palkin, null);
        else SoftLog.err.println("Couldn't find the specified resource!");
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelpKinCone(ActionEvent ev)
    {
        URL kin = getClass().getResource("kins/cone.kin");
        if(kin != null) kMain.getKinIO().loadURL(kin, null);
        else SoftLog.err.println("Couldn't find the specified resource!");
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelpKinBear(ActionEvent ev)
    {
        URL kin = getClass().getResource("kins/fallingbear.kin");
        if(kin != null) kMain.getKinIO().loadURL(kin, null);
        else SoftLog.err.println("Couldn't find the specified resource!");
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelpLog(ActionEvent ev)
    {
        new LogViewer(kMain.getTopWindow(), "KiNG error log", SoftLog.err);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onHelpAbout(ActionEvent ev)
    {
        ArrayList msgs = new ArrayList();
        
        Runtime runtime = Runtime.getRuntime();
        int i, total0 = 0, free0 = 0, total1 = 0, free1 = 0, used; // in kilobytes
        
        msgs.add(new JLabel("KiNG (Kinetic Image, Next Generation)"));
        msgs.add(new JLabel("Version "+kMain.getPrefs().getString("version")));
        msgs.add(new JLabel("Build "+kMain.getPrefs().getString("buildnum")));
        msgs.add(new JLabel("Copyright (C) 2002-2004 Ian W. Davis"));
        msgs.add(new JLabel("All rights reserved."));

        msgs.add(new JLabel(" "));
        msgs.add(new JLabel("Using Java "+System.getProperty("java.version", "(unknown version)")));
        try {
            msgs.add(new JLabel(System.getProperty("java.home", "(path not found)")));
        } catch(SecurityException ex) {}
        msgs.add(new JLabel("Using gnu.regexp "+gnu.regexp.RE.version()));

        // Take up to 10 tries at garbage collection
        for(i = 0; i < 10; i++)
        {
            total1 = (int)(runtime.totalMemory() >> 10);
            free1  = (int)(runtime.freeMemory() >> 10);
            if(total1 == total0 && free1 == free0) break;
            else
            {
                System.gc();
                //try { Thread.sleep(500); } catch(InterruptedException ex) {}
                total0 = total1;
                free0  = free1;
            }
        }
        used = total1 - free1;
        
        msgs.add(new JLabel(" "));
        //msgs.add(new JLabel("Garbage collection freed "+df.format(free2-free1)+"kb"));
        JProgressBar mem = new JProgressBar(0, total1);
        mem.setStringPainted(true);
        mem.setString(df.format(used)+"kb / "+df.format(total1)+"kb");
        mem.setValue(used);
        msgs.add(mem);
        
        JOptionPane.showMessageDialog(kMain.getTopWindow(), msgs.toArray(), "About KiNG", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /** I use this from JDB for tracking down memory leaks */
    public static String showMem()
    {
        Runtime runtime = Runtime.getRuntime();
        int i, total0 = 0, free0 = 0, total1 = 0, free1 = 0, used; // in kilobytes
        // Take up to 10 tries at garbage collection
        for(i = 0; i < 10; i++)
        {
            total1 = (int)(runtime.totalMemory() >> 10);
            free1  = (int)(runtime.freeMemory() >> 10);
            if(total1 == total0 && free1 == free0) break;
            else
            {
                System.gc();
                //try { Thread.sleep(500); } catch(InterruptedException ex) {}
                total0 = total1;
                free0  = free1;
            }
        }
        used = total1 - free1;
        return df.format(used)+"kb / "+df.format(total1)+"kb";
    }
//}}}

//{{{ notifyChange
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    static final int REDO_MENUS = KingMain.EM_SWITCH | KingMain.EM_CLOSE | KingMain.EM_CLOSEALL;
    
    void notifyChange(int event_mask)
    {
        // Take care of yourself
        if((event_mask & REDO_MENUS) != 0)
        {
            Kinemage kin = kMain.getKinemage();
            if(kin != null)
            {
                rebuildViewsMenu(kin.getViewIterator());
                rebuildAnimationsMenu(kin.getAnimationIterator());
            }
            else
            {
                rebuildViewsMenu(null);
                rebuildAnimationsMenu(null);
            }
            displayMenu.rebuildAspectsMenu();
            finder.clearSearch();
        }
        
        // Notify children
    }
//}}}
}//class
