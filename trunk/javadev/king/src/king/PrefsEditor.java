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
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
 * <code>PrefsEditor</code> provides a GUI for editing preferenes.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
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
    JTextField fontMagnification, fontSizeSmall, fontSizeBig;
    JTextField stereoAngle;
    JCheckBox textOpenOnStart;
    JCheckBox treeConfirmDelete, treeConfirmMerge;
    JCheckBox checkNewVersion;
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
        
        dialog = new JDialog(kMain.getTopWindow(), "Configure KiNG", true);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        
        fontMagnification   = new JTextField(4);
        fontSizeSmall       = new JTextField(4);
        fontSizeBig         = new JTextField(4);
        stereoAngle         = new JTextField(4);
        
        textOpenOnStart     = new JCheckBox("Open text window on startup");
        treeConfirmDelete   = new JCheckBox("Ask before deleting groups");
        treeConfirmMerge    = new JCheckBox("Ask before merging groups");
        checkNewVersion     = new JCheckBox("Check for new version online");
        
        btnDone     = new JButton(new ReflectiveAction("Close", null, this, "onDone"));
        btnDefaults = new JButton(new ReflectiveAction("Reset to defaults", null, this, "onDefaults"));
        btnSave     = new JButton(new ReflectiveAction("Save to disk", null, this, "onSave"));
        if(kMain.getApplet() != null) btnSave.setEnabled(false);
        
        GridBagPanel innerPane = new GridBagPanel();
        innerPane.gbc.anchor = GridBagConstraints.WEST;
        innerPane.gbc.weightx = innerPane.gbc.weighty = 1f;
        innerPane.gbc.insets = new Insets(2,2,2,2); //TLBR
        
        innerPane.add(new JLabel("Menu font magnification (requires restart)"), 0, 0);
        innerPane.add(fontMagnification, 1, 0);
        innerPane.add(new JLabel("Font size (normal)"), 0, 1);
        innerPane.add(fontSizeSmall, 1, 1);
        innerPane.add(new JLabel("Font size (large)"), 0, 2);
        innerPane.add(fontSizeBig, 1, 2);
        innerPane.add(new JLabel("Stereo angle (- cross, + wall)"), 0, 3);
        innerPane.add(stereoAngle, 1, 3);
        
        innerPane.span(2,1).add(textOpenOnStart,   0, 4);
        innerPane.span(2,1).add(treeConfirmDelete, 0, 5);
        innerPane.span(2,1).add(treeConfirmMerge,  0, 6);
        innerPane.span(2,1).add(checkNewVersion,   0, 7);
        
        GridBagPanel content = new GridBagPanel();
        content.gbc.insets = new Insets(4,4,4,4); //TLBR
        content.gbc.weightx = content.gbc.weighty = 1f;
        content.span(2,1).add(innerPane, 0, 0);
        content.add(btnDefaults, 0, 1);
        content.add(btnSave, 1, 1);
        content.center().span(2,1).add(btnDone, 0, 2);
        
        dialog.setContentPane(content);
    }
//}}}

//{{{ edit
//##################################################################################################
    public void edit()
    {
        toGUI();
        dialog.pack();
        dialog.setLocationRelativeTo(kMain.getTopWindow());
        dialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
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
        textOpenOnStart.setSelected(p.getBoolean("textOpenOnStart"));
        treeConfirmDelete.setSelected(p.getBoolean("treeConfirmDelete"));
        treeConfirmMerge.setSelected(p.getBoolean("treeConfirmMerge"));
        checkNewVersion.setSelected(p.getBoolean("checkNewVersion"));
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
        
        p.setProperty("textOpenOnStart", new Boolean(textOpenOnStart.isSelected()).toString());
        p.setProperty("treeConfirmDelete", new Boolean(treeConfirmDelete.isSelected()).toString());
        p.setProperty("treeConfirmMerge",  new Boolean(treeConfirmMerge.isSelected()).toString());
        p.setProperty("checkNewVersion",  new Boolean(checkNewVersion.isSelected()).toString());
    }
//}}}

//{{{ onDone, onSave, onDefaults
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDone(ActionEvent ev)
    {
        fromGUI();
        dialog.setVisible(false);
        kMain.notifyChange(KingMain.EM_PREFS);
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
        kMain.prefs.loadFromJar();
        toGUI();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

