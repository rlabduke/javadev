// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
import king.core.*;
import king.*;
import bsh.*;
import bsh.util.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>BeanKing</code> makes the Beanshell scripting environment accessible from within KiNG.
*
* TODO:
*   Fancier GUI with Help info and builtin scripts
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jul  1 15:23:55 EDT 2004
*/
public class BeanKing extends Plugin
{
//{{{ Constants
//}}}

//{{{ CLASS: TextAreaOutputStream
//##############################################################################
    /** For directing output to the JTextArea */
    static class TextAreaOutputStream extends OutputStream
    {
        JTextArea       textarea;
        StringBuffer    buf;
        
        public TextAreaOutputStream(JTextArea textarea)
        {
            this.textarea = textarea;
            this.buf        = new StringBuffer();
        }
        
        public void write(int b)
        {
            buf.append((char)b);
        }
        
        public void flush()
        {
            textarea.append(buf.toString());
            textarea.setCaretPosition(textarea.getText().length());
            buf = new StringBuffer();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    Interpreter         interp;
    JFrame              frame;
    JTextArea           outText;
    JTextField          cmdLine;
    LinkedList          cmdHistory;
    ListIterator        historyIter = null;
    PrintStream         outerr;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BeanKing(ToolBox tb)
    {
        super(tb);
        this.cmdHistory = new LinkedList();
        
        // JConsole + Interpretter.run() is not safe for Swing b/c of threads
        buildGUI();
        outerr = new PrintStream(new TextAreaOutputStream(outText), true);
        
        interp = new Interpreter(new StringReader(""), outerr, outerr, false);
        try
        {
            interp.set("parent", parent);
            interp.set("kMain", kMain);
            interp.set("kCanvas", kCanvas);
            interp.set("services", services);
            
            Reader cmds = new InputStreamReader(this.getClass().getResourceAsStream("/commands.bsh"));
            interp.eval(cmds);
        }
        catch(EvalError e)
        { e.printStackTrace(outerr); }
    }
//}}}

//{{{ buildGUI
//##############################################################################
    void buildGUI()
    {
        outText = new JTextArea(25, 60);
        outText.setLineWrap(false);
        outText.setEditable(false);
        outText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        new TextCutCopyPasteMenu(outText);
        JScrollPane outScroll = new JScrollPane(outText);
        
        cmdLine = new JTextField(50);
        cmdLine.addActionListener(new ReflectiveAction("doCmd", null, this, "onDoCmd"));
        new TextCutCopyPasteMenu(cmdLine);
        
        // Up and down arrows for command history
        ActionMap am = cmdLine.getActionMap();
        InputMap  im = cmdLine.getInputMap(JComponent.WHEN_FOCUSED);
        Action arrowUp    = new ReflectiveAction("", null, this, "onArrowUp" );
        Action arrowDown  = new ReflectiveAction("", null, this, "onArrowDown" );
        am.put("arrow-up",  arrowUp );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP , 0), "arrow-up" );
        am.put("arrow-down",  arrowDown );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN , 0), "arrow-down" );
        
        TablePane2 cp = new TablePane2();
        cp.hfill(true).vfill(true).addCell(outScroll,2,1);
        cp.newRow();
        cp.weights(0,0).addCell(new JLabel("bsh %"));
        cp.hfill(true).weights(1,0).addCell(cmdLine);
        
        frame = new JFrame(this.toString());
        frame.setContentPane(cp);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
    }
//}}}

//{{{ onDoCmd, onArrowUp/Down
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDoCmd(ActionEvent ev)
    {
        try
        {
            long time = System.currentTimeMillis();
            interp.eval(cmdLine.getText());
            cmdHistory.addFirst(cmdLine.getText());
            historyIter = null;
            cmdLine.setText("");
            time = System.currentTimeMillis() - time;
            if(time > 4000) Toolkit.getDefaultToolkit().beep();
        }
        catch(EvalError e)
        { e.printStackTrace(this.outerr); }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowUp(ActionEvent ev)
    {
        if(historyIter == null)
            historyIter = cmdHistory.listIterator();
        if(historyIter.hasNext())
            cmdLine.setText((String) historyIter.next());
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onArrowDown(ActionEvent ev)
    {
        if(historyIter != null && historyIter.hasPrevious())
            cmdLine.setText((String) historyIter.previous());
        else
            cmdLine.setText("");
    }
//}}}

//{{{ toString, getToolsMenuItem, onShow
//##############################################################################
    public String toString()
    { return "Beanshell console"; }
    
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onShow"));
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShow(ActionEvent ev)
    {
        frame.setVisible(true);
        cmdLine.requestFocus();
    }
//}}}

//{{{ getHelpURL
//##############################################################################
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL url = null;
        try { url = new URL("http://www.beanshell.org/"); }
        catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
        return url;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

