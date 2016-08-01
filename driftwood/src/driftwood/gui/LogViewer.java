// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>LogViewer</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Sep 18 08:42:19 EDT 2003
*/
public class LogViewer extends JDialog
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    SoftLog     log;
    JTextArea   text;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public LogViewer(Frame owner, String title, SoftLog sl)
    {
        super(owner, title, true);
        init(sl);
        setLocationRelativeTo(owner);
        this.setVisible(true);
    }
    
    public LogViewer(Dialog owner, String title, SoftLog sl)
    {
        super(owner, title, true);
        init(sl);
        setLocationRelativeTo(owner);
        this.setVisible(true);
    }
    
    private void init(SoftLog sl)
    {
        log = sl;
        buildGUI();
        
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.pack();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {
        TablePane cp = new TablePane();
        this.setContentPane(cp);
        
        text = new JTextArea(log.getString(), 25, 60);
        text.setLineWrap(false);        // we don't want exceptions to be hard to read
        new TextCutCopyPasteMenu(text); // provide std commands on right click
        
        JButton clear   = new JButton(new ReflectiveAction("Clear", null, this, "onClear"));
        JButton save    = new JButton(new ReflectiveAction("Save to disk...", null, this, "onSave"));
        
        JScrollPane scroll = new JScrollPane(text);
        cp.hfill(true).vfill(true);
        cp.add(scroll,2,1);
        cp.newRow();
        cp.hfill(false).vfill(false).weights(0,0);
        cp.left().add(clear);
        cp.right().add(save);
        
        ActionMap am = this.getRootPane().getActionMap();
        InputMap  im = this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        am.put("close-window", new ReflectiveAction("", null, this, "onClose"));
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W , Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "close-window" );
    }
//}}}

//{{{ onClose, onClear, onSave
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClose(ActionEvent ev)
    {
        this.dispose();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClear(ActionEvent ev)
    {
        text.setText(null);
        log.clear();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSave(ActionEvent ev)
    {
        JFileChooser saveChooser = new JFileChooser();
        File f = new File("errorlog.txt");
        saveChooser.setSelectedFile(f);
        if(JFileChooser.APPROVE_OPTION == saveChooser.showSaveDialog(this))
        {
            f = saveChooser.getSelectedFile();
            try
            {
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
                pw.println(text.getText());
                pw.close();
            }
            catch(IOException ex)
            {
                ex.printStackTrace(SoftLog.err);
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

