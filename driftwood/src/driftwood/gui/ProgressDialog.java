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
//import driftwood.*;

import javax.swing.Timer;
//}}}
/**
* <code>ProgressDialog</code> implements a simple modal dialog with a progress bar.
* Its show, dispose, and update methods can be safely called from any thread.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jan 18 09:16:57 EST 2007
*/
public class ProgressDialog implements ActionListener, Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JDialog             progDialog;
    JProgressBar        progBar;
    Timer               timer;
                         
    volatile int        taskComplete = 0;
    volatile int        taskTotal = 0;
    volatile boolean    hide = false;
    volatile boolean    canceled = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ProgressDialog(Component top, String title, boolean allowCancel)
    {
        super();
        
        // Find the Frame or Dialog that is the ultimate parent of the component
        while(true)
        {
            if(top == null)
            {
                // I think it's OK to pass null in as the dialog parent...
                break;
            }
            else if(top instanceof Frame || top instanceof Dialog) break;
            else top = top.getParent();
        }
        
        // Build the progress display dialog...
        progBar = new JProgressBar();
        progBar.setIndeterminate(true);
        progBar.setStringPainted(false); // shows % complete

        // If this isn't included, progDialog doesn't paint (bug in Java 1.4.0) (reported)
        JLabel labelNote = new JLabel(title);
        //labelNote.setHorizontalAlignment(JLabel.CENTER);
        
        JButton cancelBtn = new JButton(new ReflectiveAction("Cancel", null, this, "onRequestCancel"));
        
        TablePane2 cp = new TablePane2().insets(6).center().memorize();
        cp.addCell(labelNote).newRow();
        cp.hfill(true).addCell(progBar).newRow();
        if(allowCancel)
            cp.addCell(cancelBtn);

        if(top instanceof Dialog)
            progDialog = new JDialog((Dialog) top, "", true); // true => modal
        else
            progDialog = new JDialog((Frame) top, "", true); // true => modal
        progDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progDialog.setContentPane(cp);
        progDialog.pack();
        progDialog.setLocationRelativeTo(top);
        
        timer = new Timer(250, this); // updates every quarter second
    }
//}}}

//{{{ actionPerformed, run, onRequestCancel
//##############################################################################
    public void actionPerformed(ActionEvent ev)
    {
        synchronized(this)
        {
            if(progBar.getMaximum() != taskTotal)
                progBar.setMaximum(taskTotal);
            if(progBar.getValue() != taskComplete)
                progBar.setValue(taskComplete);
            if(taskTotal > 0 && progBar.isIndeterminate())
            {
                progBar.setIndeterminate(false);
                progBar.setStringPainted(true); // shows % complete
            }
        }
        
        // Can't show the dialog in response to a timer event or else
        // the timer gets suspended.  I don't really understand why.
        // So we do show() separately, and hide() here.
        if(hide)
        {
            timer.stop();
            hide = false;
            //progDialog.setVisible(false);
            progDialog.dispose();
        }
    }
    
    public void run()
    {
        if(!progDialog.isVisible())
        {
            timer.start();
            progDialog.pack();
            progDialog.setVisible(true); // blocks here until done
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRequestCancel(ActionEvent ev)
    {
        this.canceled = true;
    }
//}}}

//{{{ show, dispose, update, isCanceled
//##############################################################################
    /** Makes the dialog visible.  Call from any thread; does not block; does not take effect immediately. */
    synchronized public void show()
    {
        SwingUtilities.invokeLater(this);
    }
    
    /** Closes the dialog.  Call from any thread; does not block; does not take effect immediately. */
    synchronized public void dispose()
    {
        this.hide = true;
    }
    
    /**
    * Updates the progress bar.  Pass total = 0 to set "indeterminate" mode.
    * Call from any thread; does not block; does not take effect immediately.
    */
    synchronized public void update(int complete, int total)
    {
        this.taskComplete   = complete;
        this.taskTotal      = total;
    }
    
    /**
    * Returns true if the user has asked to cancel this action.
    * The dialog must still be closed manually.
    * Call from any thread; does not block; does not take effect immediately.
    */
    synchronized public boolean isCanceled()
    {
        return this.canceled;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

