// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ package, imports
package king;
import king.core.*;
import king.io.KinfileParser;

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
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class UIMenus implements KMessage.Subscriber
{
    static final DecimalFormat df = new DecimalFormat("###,###,##0");

//{{{ INNER CLASS: ViewAction
//##################################################################################################
    class ViewAction extends AbstractAction
    {
        KView view;
        
        public ViewAction(int i, KView v)
        {
            super(i+" "+v.getName());
            this.view = v;
        }
        
        public void actionPerformed(ActionEvent ev)
        {
            kMain.setView(view);
        }
    }
//}}}

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
    
    // Elements of menus that get rebuilt frequently
    JMenu oldViewMenu = null;
    JMenu fileMenu, toolsMenu;
//}}}
    
//{{{ Constructor, getMenuBar
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
        // Temporary fix for Java 6 bug # 6570445:  JFileChooser in unsigned applet
        catch(java.lang.ExceptionInInitializerError ex)
        {
            if(!(ex.getCause() instanceof java.security.AccessControlException))
                throw ex;
        }
        // Subsequent attempts to create JFileChooser cause NoClassDefFound
        catch(java.lang.NoClassDefFoundError ex) {}

        finder = new PointFinder(kMain);
        viewEditor = new ViewEditor(kMain);

        buildMenus();
        
        kMain.subscribe(this);
    }

    /** Returns the menu bar */
    public JMenuBar getMenuBar() { return menubar; }
//}}}
    
//{{{ buildMenus
//##################################################################################################
    // Construct all the menus and their actions for the menubar.
    void buildMenus()
    {
        menubar = new JMenuBar();
        JMenu menu, submenu;
        JMenuItem item;
        JCheckBoxMenuItem cbitem;
        KinCanvas kCanvas;
        
        // File menu
        fileMenu = menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menubar.add(menu);
        rebuildFileMenu();
        
        //{{{ Edit menu
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menubar.add(menu);
        item = new JMenuItem(new ReflectiveAction("Find point...", null, this, "onEditFind"));
        item.setMnemonic(KeyEvent.VK_F);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, kMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Find next", null, this, "onEditFindNext"));
        item.setMnemonic(KeyEvent.VK_N);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, kMain.MENU_ACCEL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Kinemage properties...", null, this, "onEditKinProps"));
        item.setMnemonic(KeyEvent.VK_K);
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, kMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Edit text...", null, this, "onEditText"));
        item.setMnemonic(KeyEvent.VK_T);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, kMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Edit hierarchy...", null, this, "onEditHierarchy"));
        item.setMnemonic(KeyEvent.VK_H);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, kMain.MENU_ACCEL_MASK));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Configure KiNG...", null, this, "onEditConfigure"));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);
        //}}}
        
        // Views menu
        rebuildViewsMenu();
        
        // Display menu
        displayMenu = new UIDisplayMenu(kMain);
        menubar.add(displayMenu.getMenu());
        
        // Tools menu
        toolsMenu = menu = new JMenu("Tools");
        menu.setMnemonic(KeyEvent.VK_T);
        menubar.add(menu);
        rebuildToolsMenu();
        
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
                    tb.addPluginsToHelpMenu(submenu);
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
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, kMain.MENU_ACCEL_MASK)); // 0 => no modifiers
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("About KiNG...", null, this, "onHelpAbout"));
        item.setMnemonic(KeyEvent.VK_A);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, kMain.MENU_ACCEL_MASK)); // 0 => no modifiers
        menu.add(item);
        //}}}
    }
//}}}

//{{{ rebuildFileMenu
//##################################################################################################
    public void rebuildFileMenu()
    {
        JMenuItem item;
        JMenu menu = fileMenu;
        menu.removeAll();
        
        boolean trusted = kMain.isTrusted();
        if(!kMain.isApplet())
        {
            item = new JMenuItem(new ReflectiveAction("New KiNG window", null, this, "onFileNewKing"));
            item.setMnemonic(KeyEvent.VK_N);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, kMain.MENU_ACCEL_MASK));
            menu.add(item);
            menu.addSeparator();
        }
        if(trusted)
        {
            item = new JMenuItem(new ReflectiveAction("Open file...", null, this, "onFileOpen"));
            item.setMnemonic(KeyEvent.VK_O);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, kMain.MENU_ACCEL_MASK));
            menu.add(item);
        }
        if(kMain.isApplet())
        {
            item = new JMenuItem(new ReflectiveAction("Open URL...", null, this, "onFileOpenURL"));
            if(!trusted) item.setMnemonic(KeyEvent.VK_O);
            if(!trusted) item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, kMain.MENU_ACCEL_MASK));
            menu.add(item);
        }
        if(trusted)
        {
            item = new JMenuItem(new ReflectiveAction("Append file...", null, this, "onFileMerge"));
            item.setMnemonic(KeyEvent.VK_A);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, kMain.MENU_ACCEL_MASK));
            menu.add(item);
        }
        if(kMain.isApplet())
        {
            item = new JMenuItem(new ReflectiveAction("Append URL...", null, this, "onFileMergeURL"));
            if(!trusted) item.setMnemonic(KeyEvent.VK_A);
            if(!trusted) item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, kMain.MENU_ACCEL_MASK));
            menu.add(item);
        }
        if(trusted)
        {
            KinCanvas kCanvas = kMain.getCanvas();
            if(kCanvas != null)
            {
                ToolBox tb = kCanvas.getToolBox();
                if(tb != null)
                {
                    JMenu importMenu = new JMenu("Import");
                    importMenu.setMnemonic(KeyEvent.VK_I);
                    tb.addPluginsToSpecialMenu(ToolBox.MENU_IMPORT, importMenu);
                    if(importMenu.getItemCount() > 0)
                    {
                        menu.add(importMenu);
                    }
                }
            }
        }
        item = new JMenuItem(new ReflectiveAction("Close", null, this, "onFileClose"));
        item.setMnemonic(KeyEvent.VK_C);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, kMain.MENU_ACCEL_MASK));
        menu.add(item);
        item = new JMenuItem(new ReflectiveAction("Close all", null, this, "onFileCloseAll"));
        item.setMnemonic(KeyEvent.VK_L);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, kMain.MENU_ACCEL_MASK | KeyEvent.SHIFT_MASK));
        menu.add(item);
        menu.addSeparator();
        if(trusted)
        {
            item = new JMenuItem(new ReflectiveAction("Save as...", null, this, "onFileSaveAs"));
            item.setMnemonic(KeyEvent.VK_S);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, kMain.MENU_ACCEL_MASK));
            menu.add(item);
        }
        if(kMain.isApplet() && kMain.getApplet().getParameter("kinfileSaveHandler") != null)
        {
            item = new JMenuItem(new ReflectiveAction("Network save...", null, this, "onFileSaveAsURL"));
            if(!trusted) item.setMnemonic(KeyEvent.VK_S);
            if(!trusted) item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, kMain.MENU_ACCEL_MASK));
            menu.add(item);
        }
        if(trusted)
        {
            KinCanvas kCanvas = kMain.getCanvas();
            if(kCanvas != null)
            {
                ToolBox tb = kCanvas.getToolBox();
                if(tb != null)
                {
                    JMenu exportMenu = new JMenu("Export");
                    exportMenu.setMnemonic(KeyEvent.VK_E);
                    tb.addPluginsToSpecialMenu(ToolBox.MENU_EXPORT, exportMenu);
                    if(exportMenu.getItemCount() > 0)
                    {
                        menu.add(exportMenu);
                    }
                }
            }
        }
        // This might throw a SecurityException, if the user denies us permission...
        item = new JMenuItem(new ReflectiveAction("Print...", null, this, "onFilePrint"));
        item.setMnemonic(KeyEvent.VK_P);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, kMain.MENU_ACCEL_MASK));
        menu.add(item);
        if(!kMain.isApplet())
        {
            menu.addSeparator();
            item = new JMenuItem(new ReflectiveAction("Exit", null, this, "onFileExit"));
            item.setMnemonic(KeyEvent.VK_X);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, kMain.MENU_ACCEL_MASK));
            menu.add(item);
        }
    }
//}}}

//{{{ rebuildViewsMenu
//##################################################################################################
    /**
    * Creates a new Views menu
    */
    public void rebuildViewsMenu()
    {
        JMenu menu = new JMenu("Views");
        menu.setMnemonic(KeyEvent.VK_V);
        JMenuItem item;
        JRadioButtonMenuItem ritem;
        ButtonGroup rgroup = new ButtonGroup();
        
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            item = new JMenuItem(new ReflectiveAction("Save current view", null, this, "onViewSave"));
            item.setMnemonic(KeyEvent.VK_S);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, kMain.MENU_ACCEL_MASK));
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Edit saved views...", null, this, "onViewEdit"));
            item.setMnemonic(KeyEvent.VK_E);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, kMain.MENU_ACCEL_MASK | KeyEvent.SHIFT_MASK));
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Choose viewing axes...", null, this, "onViewChooseAxes"));
            item.setMnemonic(KeyEvent.VK_C);
            //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, kMain.MENU_ACCEL_MASK | KeyEvent.SHIFT_MASK));
            menu.add(item);
            item = new JMenuItem(new ReflectiveAction("Parallel coordinates", null, this, "onViewParallelCoords"));
            item.setMnemonic(KeyEvent.VK_P);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0));
            menu.add(item);
            menu.addSeparator();
            
            JMenu currMenu = menu;
            Iterator<KView> viewiter = kin.getViewList().iterator();
            for(int i = 1; viewiter.hasNext(); i++)
            {
                // Every 25 views, chain them into a new menu
                if(i != 1 && i % 25 == 1)
                {
                    JMenu newMenu = new JMenu("More views");
                    currMenu.add(newMenu);
                    currMenu = newMenu;
                }
                KView view = viewiter.next();
                ritem = new JRadioButtonMenuItem(new ViewAction(i, view));
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

//{{{ rebuildToolsMenu
//##################################################################################################
    public void rebuildToolsMenu()
    {
        JMenuItem item;
        JCheckBoxMenuItem cbitem;
        
        JMenu menu = toolsMenu;
        menu.removeAll();
        cbitem = kMain.getCanvas().getToolBox().services.doFlatland;
        cbitem.setMnemonic(KeyEvent.VK_L);
        cbitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
        menu.add(cbitem);
        cbitem = kMain.getCanvas().getToolBox().services.doXYZ;
        cbitem.setMnemonic(KeyEvent.VK_X);
        //cbitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, kMain.MENU_ACCEL_MASK));
        menu.add(cbitem);
        cbitem = kMain.getCanvas().getToolBox().services.doMeasureAll;
        cbitem.setMnemonic(KeyEvent.VK_M);
        cbitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
        menu.add(cbitem);
        cbitem = kMain.getCanvas().getToolBox().services.doObjectPick;
        cbitem.setMnemonic(KeyEvent.VK_O);
        menu.add(cbitem);
        cbitem = kMain.getCanvas().getToolBox().services.doSuperpick;
        cbitem.setMnemonic(KeyEvent.VK_S);
        menu.add(cbitem);
        KinCanvas kCanvas = kMain.getCanvas();
        if(kCanvas != null)
        {
            ToolBox tb = kCanvas.getToolBox();
            if(tb != null)
            {
                menu.addSeparator();
                tb.addPluginsToToolsMenu(menu);
            }
        }
        menu.addSeparator();
        item = new JMenuItem(new ReflectiveAction("Customize Tools menu...", null, this, "onEditConfigurePlugins"));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);
    }
//}}}

//{{{ reporter -- the dummy action
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
    public void onFileOpenURL(ActionEvent ev)
    {
        KinfileIO io = kMain.getKinIO();
        if(kMain.isApplet()) io.askLoadURL(null);
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
        else
            io.askLoadFile(kin);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileMergeURL(ActionEvent ev)
    {
        KinfileIO io = kMain.getKinIO();
        Kinemage kin = kMain.getKinemage();
        if(kin == null)
        {
            kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
            if(kMain.isApplet() && io.askLoadURL(kin))
                kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));
            // This way we don't create an empty if append is canceled
        }
        else if(kMain.isApplet())
            io.askLoadURL(kin);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileClose(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null && k.isModified())
        {
            int confirm = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                "This kinemage has been modified.\nDo you want to save it before closing?",
                "Save before closing?", JOptionPane.YES_NO_CANCEL_OPTION);
            if(confirm == JOptionPane.CANCEL_OPTION) return;
            else if(confirm == JOptionPane.YES_OPTION) onFileSaveAs(ev);
        }
        
        kMain.getStable().closeCurrent();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileCloseAll(ActionEvent ev)
    {
        boolean modified = false;
        for(Kinemage k : kMain.getStable().getKins())
            if(k.isModified()) modified = true;

        if(modified)
        {
            int confirm = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                "One or more open kinemages have been modified.\nDo you want to save them before closing?",
                "Save before closing?", JOptionPane.YES_NO_CANCEL_OPTION);
            if(confirm == JOptionPane.CANCEL_OPTION) return;
            else if(confirm == JOptionPane.YES_OPTION) onFileSaveAs(ev);
        }

        kMain.getStable().closeAll();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileSaveAs(ActionEvent ev)
    {
        KinfileIO io = kMain.getKinIO();
        int numKins = kMain.getStable().getKins().size();
        if(numKins > 1)
        {
            JRadioButton btnBoth = new JRadioButton((numKins == 2 ? "Save both in one file" : "Save all "+numKins+" in one file"), true);
            JRadioButton btnCurr = new JRadioButton("Save only the currently selected kinemage", false);
            ButtonGroup btnGroup = new ButtonGroup();
            btnGroup.add(btnBoth);
            btnGroup.add(btnCurr);
            
            int result = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                new Object[] {
                    "There are currently "+numKins+" open kinemages.",
                    "What do you want to do?",
                    btnBoth,
                    btnCurr
                },
                "Saving multiple kinemages",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if(result == JOptionPane.NO_OPTION || result == JOptionPane.CANCEL_OPTION) {}
            else if(btnBoth.isSelected())   io.askSaveFile();
            else if(btnCurr.isSelected())   io.askSaveFile(kMain.getKinemage());
        }
        else io.askSaveFile();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFileSaveAsURL(ActionEvent ev)
    {
        KinfileIO io = kMain.getKinIO();
        if(kMain.isApplet()) io.askSaveURL();
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
        onFileCloseAll(ev); // checks for modifications and prompts to save
        if(kMain.getStable().getKins().size() == 0) kMain.shutdown();
        // else we must have pressed Cancel
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
        PrefsEditor prefsEditor = new PrefsEditor(kMain);
        prefsEditor.edit();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditConfigurePlugins(ActionEvent ev)
    {
        PrefsEditor prefsEditor = new PrefsEditor(kMain);
        prefsEditor.editPlugins();
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
        KView view = kMain.getView();
        if(view == null) return;
        view = (KView)view.clone();
        view.setName(viewname);
        
        // User should choose to save axes positions or not (?)
        // If the data is high-D data, views only really make sense
        // in the context of the dimensions that were being viewed!
        //view.setViewingAxes(null);
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.addView(view);
        kin.setModified(true);
        rebuildViewsMenu();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onViewEdit(ActionEvent ev)
    {
        viewEditor.editViews();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onViewChooseAxes(ActionEvent ev)
    {
        new AxisChooser(kMain, kMain.getKinemage());
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onViewParallelCoords(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        String key = ParaParams.class.getName()+".instance";
        ParaParams params = (ParaParams) kin.metadata.get(key);
        if(params == null)
        {
            params = new ParaParams(kMain, kin);
            kin.metadata.put(key, params);
        }
        params.swap();
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
        KingPrefs prefs = kMain.getPrefs();
        
        Runtime runtime = Runtime.getRuntime();
        int i, total0 = 0, free0 = 0, total1 = 0, free1 = 0, used; // in kilobytes
        
        msgs.add(new JLabel("KiNG (Kinetic Image, Next Generation)"));
        msgs.add(new JLabel("Version "+kMain.getPrefs().getString("version")));
        msgs.add(new JLabel("Build "+kMain.getPrefs().getString("buildnum")));
        try {
            if(prefs.jarFileDirectory != null)
                msgs.add(new JLabel("Installed in "+prefs.jarFileDirectory.getCanonicalPath()));
        } catch(IOException ex) {}
        msgs.add(new JLabel(" "));
        msgs.add(new JLabel("Installed Plugins:"));
        Enumeration propNames = prefs.propertyNames();
        while (propNames.hasMoreElements()) {
          String propName = (String) propNames.nextElement();
          if (propName.matches(".* version")) {
            String jarName = propName.substring(0, propName.lastIndexOf(" "));
            String buildNum = prefs.getProperty(jarName+" buildnum");
            msgs.add(new JLabel(propName+": "+prefs.getProperty(propName)+"."+buildNum));
          }
        }
        msgs.add(new JLabel(" "));
        msgs.add(new JLabel("Created in the Richardson lab at Duke University"));
        msgs.add(new JLabel("http://kinemage.biochem.duke.edu"));
        msgs.add(new JLabel(" "));
        msgs.add(new JLabel("Copyright (C) 2002-2009 Ian W. Davis and Vincent B. Chen"));
        msgs.add(new JLabel("All rights reserved."));

        msgs.add(new JLabel(" "));
        msgs.add(new JLabel("Using Java "+System.getProperty("java.version", "(unknown version)")));
        try {
            msgs.add(new JLabel(System.getProperty("java.home", "(path not found)")));
        } catch(SecurityException ex) {}
        // We're not listing the versions of iText, JOGL, etc. here ...
        //msgs.add(new JLabel("Using gnu.regexp "+gnu.regexp.RE.version()));

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

//{{{ deliverMessage
//##################################################################################################
    static final long REDO_MENUS_P = KMessage.KIN_SWITCHED | KMessage.KIN_CLOSED | KMessage.ALL_CLOSED;
    static final int REDO_MENUS_K = AHE.CHANGE_VIEWS_LIST | AHE.CHANGE_MASTERS_LIST;
    public void deliverMessage(KMessage msg)
    {
        if(msg.testProg(REDO_MENUS_P) || msg.testKin(REDO_MENUS_K))
        {
            rebuildViewsMenu();
            displayMenu.rebuildAspectsMenu();
            finder.clearSearch();
        }
        if(msg.testProg(KMessage.PREFS_CHANGED))
        {
            // Plugin placement may have changed
            rebuildFileMenu();
            rebuildToolsMenu();
        }
    }
//}}}
}//class
