// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;
import king.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;

import javax.swing.Timer; // not java.util.Timer
//}}}
/**
* <code>SelfUpdatePlugin</code> allows the user to download the newest KiNG
* version and install it from within the KiNG program itself.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat May 22 11:53:49 EDT 2004
*/
public class SelfUpdatePlugin extends Plugin
{
//{{{ Constants
    static final String UPDATE_ANYWAY = 
        "It appears that either you already have the latest version of KiNG,\n"+
        "or that you are not connected to the network at this time.\n"+
        "\n"+
        "Do you still want to try updating KiNG?";
        
    static final String ARE_YOU_SURE =
        "This plugin will download the latest version of KiNG\n"+
        "that is publicly available on the Kinemage website.\n"+
        "It will then be installed over top of your current KiNG\n"+
        "version, completely replacing it.\n"+
        "\n"+
        "This action cannot be undone. Furthermore, there is always\n"+
        "a small chance it may not perform correctly, potentially\n"+
        "rendering KiNG unusable and forcing you to reinstall manually.\n"+
        "Are you sure you want to continue?";
        
    static final String UPDATE_FAILED =
        "Due to circumstances beyond our control, the update failed.\n"+
        "The most likely source of error is that you don't have permission\n"+
        "to overwrite the KiNG installation, or that the network is down.\n"+
        "More details about the error are available under Help | Error Log.\n"+
        "\n"+
        "Your copy of KiNG is probably OK, but it COULD have been damaged --\n"+
        "if it acts strangely or refuses to start, you may have to reinstall.\n"+
        "You can get a new copy of KiNG from http://kinemage.biochem.duke.edu.\n"+
        "\n"+
        "We are very sorry this happened. If you believe this is the result of\n"+
        "a bug in KiNG or in this plugin, please report it to the author/maintainter,\n"+
        "whose email address is listed in the user manual.";
        
    static final String ABORT_OK =
        "The update has been aborted.\n"+
        "No changes have been made to KiNG.";
        
    static final String UPDATE_OK =
        "The update appears to have succeeded.\n"+
        "Cross your fingers and restart KiNG\n"+
        "for changes to take effect.";
//}}}

//{{{ Variable definitions
//##############################################################################
    Timer               progressTimer;
    JProgressBar        progressBar;
    JDialog             dialog;
    volatile int        totalSize = 1, downloadedSize = 0;
    volatile boolean    abortFlag = false;
    volatile Throwable  backgroundError = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SelfUpdatePlugin(ToolBox tb)
    {
        super(tb);
        progressTimer = new Timer(1000, new ReflectiveAction(null, null, this, "onProgressTimer"));
        progressTimer.setCoalesce(true);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        JButton btnCancel = new JButton(new ReflectiveAction("Cancel", null, this, "onDownloadCancel"));
        TablePane2 cp = new TablePane2();
        cp.insets(4).memorize();
        cp.addCell(new JLabel("Downloading new version of KiNG from")).newRow();
        cp.addCell(new JLabel("http://kinemage.biochem.duke.edu ...")).newRow();
        cp.hfill(true).addCell(progressBar).newRow();
        cp.center().addCell(btnCancel);
        dialog = new JDialog(kMain.getTopWindow(), true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setContentPane(cp);
    }
//}}}

//{{{ toString, isAppletSafe, getToolsMenuItem, getHelpAnchor
//##############################################################################
    public String toString()
    { return "Update KiNG"; }
    
    public static boolean isAppletSafe()
    { return false; }
    
    public JMenuItem getToolsMenuItem()
    {
        JMenuItem item = new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onBeginUpdate"));
        return item;
    }
    
    public String getHelpAnchor()
    { return null; }
//}}}

//{{{ onProgressTimer, onDownloadCancel
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onProgressTimer(ActionEvent ev)
    {
        progressBar.setValue((100*downloadedSize)/totalSize);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDownloadCancel(ActionEvent ev)
    {
        abortFlag = true;
    }
//}}}

//{{{ onBeginUpdate
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onBeginUpdate(ActionEvent ev)
    {
        // Check with user before starting
        if(! kMain.getPrefs().newerVersionAvailable()
        && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(kMain.getTopWindow(), UPDATE_ANYWAY, "Update anyway?", JOptionPane.YES_NO_OPTION))
            return;
        
        if(JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(kMain.getTopWindow(), ARE_YOU_SURE, "Update KiNG?", JOptionPane.YES_NO_OPTION))
            return;
        
        this.downloadedSize     = 0;
        this.abortFlag          = false;
        this.backgroundError    = null;
        this.progressBar.setValue(0);
        
        Thread backgroundJob = new Thread(new ReflectiveRunnable(this, "downloadFile"));
        backgroundJob.start();
        
        this.progressTimer.start();
        this.dialog.pack();
        this.dialog.setLocationRelativeTo(kMain.getTopWindow());
        this.dialog.setVisible(true);
        // execution halts here until the dialog is closed
    }
//}}}

//{{{ downloadFile
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void downloadFile()
    {
        try
        {
            URL updateURL = new URL("http://kinemage.biochem.duke.edu/ftpsite/pub/software/king/.current.java");
            URLConnection urlConn = updateURL.openConnection();
            this.totalSize = urlConn.getContentLength();
            this.downloadedSize = 0;
            
            InputStream is = urlConn.getInputStream();
            File tmpFile = File.createTempFile("kingupdate", null);
            tmpFile.deleteOnExit();
            OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile));
            //streamcopy(is, os);
            byte[] buffer = new byte[2048];
            int len;
            while((len = is.read(buffer)) != -1 && !abortFlag)
            {
                os.write(buffer, 0, len);
                this.downloadedSize += len;
            }
            os.close();
            is.close();
            
            if(!abortFlag)
            {
                ZipFile f = new ZipFile(tmpFile);
                installZipFile(f);
                f.close();
                abortFlag = false; // just in case there was a sync. problem
            }
        }
        catch(Throwable t)
        {
            this.backgroundError = t;
        }
        
        SwingUtilities.invokeLater(new ReflectiveRunnable(this, "onFinishUpdate"));
    }
//}}}

//{{{ installZipFile, streamcopy
//##############################################################################
    /**
    * Unpacks the ZIP file into the directory where king.jar is currently located,
    * after stripping off the initial king-x.xx/ path
    */
    private void installZipFile(ZipFile zipfile) throws IOException
    {
        File dest = kMain.getPrefs().jarFileDirectory;
        if(!dest.exists() || !dest.isDirectory() || !dest.canWrite())
            throw new IOException("Unable to unpack downloaded ZIP into "+dest+"; check permissions/ownership?");
        
        Enumeration entries = zipfile.entries();
        while(entries.hasMoreElements())
        {
            ZipEntry e = (ZipEntry) entries.nextElement();
            // Clip off king-x.xx/ prefix
            String name = e.getName();
            int i = name.indexOf("/");
            if(i != -1) name = name.substring(i);
            if(name.equals("")) continue;
            // Create directory or write file
            File f = new File(dest, name).getCanonicalFile();
            if(e.isDirectory())
            {
                f.mkdirs();
            }
            else
            {
                InputStream is = zipfile.getInputStream(e);
                OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                streamcopy(is, os);
                os.close();
                is.close();
            }
        }
    }
    
    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ onFinishUpdate
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onFinishUpdate()
    {
        this.progressTimer.stop();
        this.dialog.setVisible(false);
        
        if(backgroundError != null)
        {
            backgroundError.printStackTrace(SoftLog.err);
            JOptionPane.showMessageDialog(kMain.getTopWindow(), UPDATE_FAILED, "Update failed", JOptionPane.ERROR_MESSAGE);
        }
        else if(abortFlag)
        {
            // Don't really need to do anything.
            JOptionPane.showMessageDialog(kMain.getTopWindow(), ABORT_OK, "Update aborted", JOptionPane.INFORMATION_MESSAGE);
        }
        else
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(), UPDATE_OK, "Update succeeded", JOptionPane.INFORMATION_MESSAGE);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

