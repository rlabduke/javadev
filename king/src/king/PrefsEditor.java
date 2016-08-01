// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.data.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>PrefsEditor</code> provides a GUI for editing preferences.
* Because the list of loaded plugins could change at any time,
* instances of this class should only be used once and then discarded.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Dec 12 13:32:25 EST 2002
*/
public class PrefsEditor //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    
    JDialog dialog;
    JTabbedPane tabPane;
    JTextField fontMagnification, fontSizeSmall, fontSizeBig;
    JTextField stereoAngle;
    JCheckBox joglByDefault, textOpenOnStart, textAllowEdits, textEmptyKinsClosed;
    JCheckBox treeConfirmDelete, treeConfirmMerge;
    JCheckBox checkNewVersion;
    JCheckBox minimizableTools;
    Map pluginMenuMap; // maps plugin class name to a JComboBox
    JButton btnDone, btnDefaults, btnSave;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PrefsEditor(KingMain kmain)
    {
        kMain = kmain;
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI()
    {
        btnDone     = new JButton(new ReflectiveAction("Close", null, this, "onDone"));
        btnDefaults = new JButton(new ReflectiveAction("Reset to defaults", null, this, "onDefaults"));
        btnSave     = new JButton(new ReflectiveAction("Save to disk", null, this, "onSave"));
        if(!kMain.isTrusted()) btnSave.setEnabled(false);
        
        Component generalPane = buildGeneralPane();
        Component pluginPane = buildPluginsPane();
        
        tabPane = new JTabbedPane();
        tabPane.addTab("General", generalPane);
        tabPane.addTab("Plugins", pluginPane);
        
        TablePane2 content = new TablePane2();
        content.hfill(true).vfill(true).addCell(tabPane, 2, 1);
        content.newRow();
        content.center().memorize();
        content.addCell(btnDefaults).addCell(btnSave);
        content.newRow();
        content.addCell(btnDone, 2, 1);
        
        dialog = new JDialog(kMain.getTopWindow(), "Configure KiNG", true);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setContentPane(content);
    }
//}}}

//{{{ buildGeneralPane
//##################################################################################################
    private Component buildGeneralPane()
    {
        fontMagnification   = new JTextField(4);
        fontSizeSmall       = new JTextField(4);
        fontSizeBig         = new JTextField(4);
        stereoAngle         = new JTextField(4);
        
        joglByDefault       = new JCheckBox("Start in OpenGL mode");
        textOpenOnStart     = new JCheckBox("Open text window on startup");
        textAllowEdits      = new JCheckBox("Text window starts off editable");
        textEmptyKinsClosed = new JCheckBox("Empty text window if all kins closed");
        treeConfirmDelete   = new JCheckBox("Ask before deleting groups");
        treeConfirmMerge    = new JCheckBox("Ask before merging groups");
        checkNewVersion     = new JCheckBox("Check for new version online");
        minimizableTools    = new JCheckBox("Use minimizable tool windows");
        
        TablePane2 innerPane = new TablePane2();
        innerPane.addCell(new JLabel("Menu font magnification (requires restart)")).addCell(fontMagnification).newRow();
        innerPane.addCell(new JLabel("Font size (normal)")).addCell(fontSizeSmall).newRow();
        innerPane.addCell(new JLabel("Font size (large)")).addCell(fontSizeBig).newRow();
        innerPane.addCell(new JLabel("Stereo angle (- cross, + wall)")).addCell(stereoAngle).newRow();
        innerPane.addCell(joglByDefault, 2, 1).newRow();
        innerPane.addCell(textOpenOnStart, 2, 1).newRow();
        innerPane.addCell(textAllowEdits, 2, 1).newRow();
        innerPane.addCell(textEmptyKinsClosed, 2, 1).newRow();
        innerPane.addCell(treeConfirmDelete, 2, 1).newRow();
        innerPane.addCell(treeConfirmMerge, 2, 1).newRow();
        innerPane.addCell(checkNewVersion, 2, 1).newRow();
        innerPane.addCell(minimizableTools, 2, 1).newRow();
        
        return innerPane;
    }
//}}}

//{{{ buildPluginsPane
//##################################################################################################
    private Component buildPluginsPane()
    {
        ToolBox toolbox = kMain.getCanvas().getToolBox();
        Collection plugins = toolbox.getPluginList();
        
        // Make a list of all unique submenu names
        UberSet menuNames = new UberSet();
        menuNames.add(ToolBox.MENU_NONE);
        menuNames.add(ToolBox.MENU_MAIN);
        menuNames.add(ToolBox.MENU_IMPORT);
        menuNames.add(ToolBox.MENU_EXPORT);
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
            menuNames.add(toolbox.getPluginMenuName((Plugin)iter.next()));
        Object[] items = menuNames.toArray();
        
        // Add GUI components for all plugins, one per row
        pluginMenuMap = new HashMap();
        TablePane2 content = new TablePane2();
        content.addCell(new JLabel("Choose a menu for each tool, or type in a new menu name."), 3, 1).newRow();
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            Plugin p = (Plugin) iter.next();
            JComboBox combo = new JComboBox(items);
            combo.setSelectedItem(toolbox.getPluginMenuName(p));
            combo.setEditable(true);
            content.addCell(new JLabel(p.toString()));
            content.addCell(content.strut(10,0));
            content.hfill(true).addCell(combo);
            content.newRow();
            pluginMenuMap.put(p.getClass().getName(), combo);
        }
        
        JScrollPane scroll = new JScrollPane(content);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // Keep the list from being too tall in the dialog
        Dimension dim = scroll.getPreferredSize();
        dim.height = 100;
        scroll.setPreferredSize(dim);
        return scroll;
    }
//}}}

//{{{ edit, editPlugins
//##################################################################################################
    public void edit()
    {
        toGUI();
        dialog.pack();
        dialog.setLocationRelativeTo(kMain.getTopWindow());
        dialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
    }
    
    public void editPlugins()
    {
        int index = tabPane.indexOfTab("Plugins");
        if(index != -1)
            tabPane.setSelectedIndex(index);
        this.edit();
    }
//}}}

//{{{ toGUI, fromGUI
//##################################################################################################
    private void toGUI()
    {
        KingPrefs p = kMain.prefs;
        if(p == null) return;
        
        fontMagnification.setText(p.getString("fontMagnification"));
        fontSizeSmall.setText(p.getString("fontSizeSmall"));
        fontSizeBig.setText(p.getString("fontSizeBig"));
        stereoAngle.setText(p.getString("stereoAngle"));
        joglByDefault.setSelected(p.getBoolean("joglByDefault"));
        textOpenOnStart.setSelected(p.getBoolean("textOpenOnStart"));
        textAllowEdits.setSelected(p.getBoolean("textDefaultAllowEdits"));
        textEmptyKinsClosed.setSelected(p.getBoolean("textEmptyIfAllKinsClosed"));
        treeConfirmDelete.setSelected(p.getBoolean("treeConfirmDelete"));
        treeConfirmMerge.setSelected(p.getBoolean("treeConfirmMerge"));
        checkNewVersion.setSelected(p.getBoolean("checkNewVersion"));
        minimizableTools.setSelected(p.getBoolean("minimizableTools"));
    }
    
    private void fromGUI()
    {
        KingPrefs p = kMain.prefs;
        if(p == null) return;
        
        try {
            Float f = new Float(fontMagnification.getText().trim());
            p.setProperty("fontMagnification", f.toString());
        } catch(NumberFormatException ex) {}
        
        try {
            Integer i = new Integer(fontSizeSmall.getText().trim());
            p.setProperty("fontSizeSmall", i.toString());
        } catch(NumberFormatException ex) {}

        try {
            Integer i = new Integer(fontSizeBig.getText().trim());
            p.setProperty("fontSizeBig", i.toString());
        } catch(NumberFormatException ex) {}
        
        try {
            Float f = new Float(stereoAngle.getText().trim());
            p.setProperty("stereoAngle", f.toString());
        } catch(NumberFormatException ex) {}
        
        p.setProperty("joglByDefault", new Boolean(joglByDefault.isSelected()).toString());
        p.setProperty("textOpenOnStart", new Boolean(textOpenOnStart.isSelected()).toString());
        p.setProperty("textDefaultAllowEdits", new Boolean(textAllowEdits.isSelected()).toString());
        p.setProperty("textEmptyIfAllKinsClosed", new Boolean(textEmptyKinsClosed.isSelected()).toString());
        p.setProperty("treeConfirmDelete", new Boolean(treeConfirmDelete.isSelected()).toString());
        p.setProperty("treeConfirmMerge",  new Boolean(treeConfirmMerge.isSelected()).toString());
        p.setProperty("checkNewVersion",  new Boolean(checkNewVersion.isSelected()).toString());
        p.setProperty("minimizableTools", new Boolean(minimizableTools.isSelected()).toString());
        
        for(Iterator iter = pluginMenuMap.keySet().iterator(); iter.hasNext(); )
        {
            String pluginName = (String) iter.next();
            JComboBox combo = (JComboBox) pluginMenuMap.get(pluginName);
            String menuName = (String) combo.getSelectedItem();
            p.setProperty(pluginName+".menuName", menuName);
        }
    }
//}}}

//{{{ onDone, onSave, onDefaults
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDone(ActionEvent ev)
    {
        fromGUI();
        dialog.setVisible(false);
        kMain.publish(new KMessage(this, KMessage.PREFS_CHANGED));
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSave(ActionEvent ev)
    {
        fromGUI();
        try { kMain.prefs.storeToFile(); }
        catch(SecurityException ex) { ex.printStackTrace(SoftLog.err); }
        toGUI();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDefaults(ActionEvent ev)
    {
        KingPrefs prefs = kMain.getPrefs();
        prefs.clear();
        toGUI();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

