// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>KinfileIO</code> is a uniform front-end for normal
* reading and writing of kinemage files. It handles tracking
* of file name and current directory, file name mangling, etc.
*
* It also allows files to be opened in a background thread
* while a progress bar is displayed, thus enhancing the user experience.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Apr 10 14:38:35 EDT 2003
*/
public class KinfileIO implements KinLoadListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain            kMain;
    JFileChooser        fileOpenChooser     = null;     // stays null if we're in an Applet
    JFileChooser        fileSaveChooser     = null;     // stays null if we're in an Applet
    File                lastOpenedFile      = null;
    File                lastSavedFile       = null;

    JDialog             progDialog          = null;
    JProgressBar        progBar             = null;
    
    // These are necessary to make the callback mechanism work,
    // but we have to be very careful of memory leaks!
    String              fName               = null;
    Kinemage            mergeTarget         = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public KinfileIO(KingMain kmain)
    {
        kMain = kmain;
        initChooser();
        initProgressDialog();
    }
//}}}

//{{{ initChooser
//##################################################################################################
    private void initChooser()
    {
        // Will throw an exception if we're running as an Applet
        try
        {
            SuffixFileFilter fileFilter = new SuffixFileFilter("Kinemage files (*.kin)");
            // New suffixes:
            fileFilter.addSuffix(".kin");
            fileFilter.addSuffix(".kin.gz");    // compressed with gzip
            fileFilter.addSuffix(".kinz");      // compressed with gzip
            // Old suffixes:
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
            
            fileOpenChooser = new JFileChooser();
            fileOpenChooser.addChoosableFileFilter(fileFilter);
            fileOpenChooser.setFileFilter(fileFilter);

            fileSaveChooser = new JFileChooser();
            fileSaveChooser.addChoosableFileFilter(fileFilter);
            fileSaveChooser.setFileFilter(fileFilter);

            String currdir = System.getProperty("user.dir");
            if(currdir != null)
            {
                fileOpenChooser.setCurrentDirectory(new File(currdir));
                fileSaveChooser.setCurrentDirectory(new File(currdir));
            }
        }
        catch(SecurityException ex) {}
    }
//}}}

//{{{ initProgressDialog
//##################################################################################################
    private void initProgressDialog()
    {
        // Build the progress display dialog...
        progBar = new JProgressBar();
        progBar.setStringPainted(true); // shows % complete
        progBar.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        // If this isn't included, progDialog doesn't paint (bug in Java 1.4.0) (reported)
        JLabel labelNote = new JLabel("Loading kinemage(s)...");
        labelNote.setHorizontalAlignment(JLabel.CENTER);
        labelNote.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        progDialog = new JDialog(kMain.getTopWindow(), "", true); // true => modal
        progDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progDialog.getContentPane().add(progBar, BorderLayout.SOUTH);
        progDialog.getContentPane().add(labelNote, BorderLayout.NORTH);
    }
//}}}

//{{{ notifyChange
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    void notifyChange(int event_mask)
    {
    }
//}}}

//{{{ askLoadFile, loadFile
//##################################################################################################
    /**
    * Asks the user to choose a kinemage file to load.
    * All the kinemages it contains will be added to the stable
    * or appended to the specified kinemage.
    *
    * This function MUST be called from the event-dispatch thread.
    * This function will not return until all the kinemages have
    * been loaded.
    *
    * @param kin the kinemage into which the opened kinemage(s)
    *   should be merged, or null for them to stand alone.
    * @return true iff the user choose to open a file
    */
    public boolean askLoadFile(Kinemage kin)
    {
        if(fileOpenChooser == null) return false;
        
        if(fileOpenChooser.APPROVE_OPTION == fileOpenChooser.showOpenDialog(kMain.getTopWindow()))
        {
            File f = fileOpenChooser.getSelectedFile();
            loadFile(f, kin);
            return true;
        }
        else return false;
    }
    
    /** Like askLoadFile, but it doesn't ask */
    public void loadFile(File f, Kinemage kin)
    {
        try
        {
            fName           = f.getName();
            mergeTarget     = kin;
            new KinfileLoader(kMain, new FileInputStream(f), this);
            
            // Only reset the filenames if we do a true open
            if(kin == null)
            {
                lastOpenedFile  = f;
                lastSavedFile   = null;
            }
            
            progBar.setMaximum((int)f.length());
            progBar.setValue(0);
            
            progDialog.pack();
            progDialog.setLocationRelativeTo(kMain.getTopWindow());
            progDialog.setVisible(true);
            // Execution halts here until ioException()
            // or loadingComplete() closes the dialog.
        }
        catch(IOException ex)
        { loadingException(ex); }
    }
//}}}

//{{{ loadURL
//##################################################################################################
    /** Like loadFile, but it takes a URL */
    public void loadURL(URL url, Kinemage kin)
    {
        try
        {
            fName           = url.getFile();
            mergeTarget     = kin;
            
            URLConnection uconn = url.openConnection();
            uconn.setAllowUserInteraction(false);
            uconn.connect();
            new KinfileLoader(kMain, uconn.getInputStream(), this);
            
            progBar.setMaximum(uconn.getContentLength());
            progBar.setValue(0);
            
            progDialog.pack();
            progDialog.setLocationRelativeTo(kMain.getTopWindow());
            progDialog.setVisible(true);
            // Execution halts here until ioException()
            // or loadingComplete() closes the dialog.
        }
        catch(IOException ex)
        { loadingException(ex); }
    }
//}}}

//{{{ updateProgress
//##################################################################################################
    /**
    * Messaged periodically as the parser reads the file.
    */
    public void updateProgress(long charsRead)
    { progBar.setValue((int)charsRead); }
//}}}

//{{{ loadingException
//##################################################################################################
    /**
    * Messaged if anything is thrown during the loading process.
    * This generally means loadingComplete() won't be called.
    */
    public void loadingException(Throwable t)
    {
        progDialog.setVisible(false);
        
        t.printStackTrace(SoftLog.err);
        JOptionPane.showMessageDialog(kMain.getTopWindow(),
            "The file '"+fName+"'\ncould not be opened due to an exception:\n"+t.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
        
        // Avoid memory leaks!
        mergeTarget = null;
    }
//}}}

//{{{ loadingComplete
//##################################################################################################
    /**
    * Messaged if and when loading finished successfully.
    */
    public void loadingComplete(KinfileParser parser)
    {
        for(Iterator kinIter = parser.getKinemages().iterator(); kinIter.hasNext(); )
        { ((Kinemage)kinIter.next()).initAll(); }
        
        if(mergeTarget == null)
        {
            kMain.getStable().append(parser.getKinemages());
        }
        else
        {
            for(Iterator kinIter = parser.getKinemages().iterator(); kinIter.hasNext(); )
            { mergeTarget.appendKinemage((Kinemage)kinIter.next()); }
            kMain.notifyChange(KingMain.EM_SWITCH | KingMain.EM_NEWVIEW | KingMain.EM_EDIT_GROSS);
        }
        
        if(kMain.getTextWindow() != null)
        { kMain.getTextWindow().appendText(parser.getText()); }
        
        // Avoid memory leaks!
        mergeTarget = null;

        // Last step: close the dialog and let execution continue in the calling thread
        progDialog.setVisible(false);

    }
//}}}

//{{{ askSaveFile, saveFile
//##################################################################################################
    /**
    * Asks the user to choose a file where all open kinemages will be written.
    *
    * This function MUST be called from the event-dispatch thread.
    * This function will not return until all the kinemages have
    * been saved.
    */
    public void askSaveFile()
    {
        if(fileSaveChooser == null) return;
        
        setMangledName();
        if(fileSaveChooser.APPROVE_OPTION == fileSaveChooser.showSaveDialog(kMain.getTopWindow()))
        {
            File f = fileSaveChooser.getSelectedFile();
            if( !f.exists() ||
                JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                    "This file exists -- do you want to overwrite it?",
                    "Overwrite file?", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION )
            {
                saveFile(f);
            }
        }
    }
    
    /** Like askSaveFile, but doesn't ask */
    public void saveFile(File f)
    {
        try
        {
            Writer w = new FileWriter(f);
            KinWriter kw = new KinWriter();
            kw.save(w,
                kMain.getTextWindow().getText(),
                kMain.getStable().children);
            lastSavedFile = f;
            w.close();
            
            for(Iterator iter = kMain.getStable().iterator(); iter.hasNext(); )
            {
                Kinemage k = (Kinemage) iter.next();
                k.setModified(false);
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace(SoftLog.err);
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An error occurred while saving the file.",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
        }
    }
//}}}

//{{{ setMangledName
//##################################################################################################
    /**
    * Sets the filename in the fileSaveChooser to reflect the standard versioning scheme:
    * foo.kin, foo.1.kin, foo.2.kin, ... , foo.9.kin, foo.a.kin, ... , foo.z.kin
    */
    void setMangledName()
    {
        if(fileSaveChooser == null)     return;
        
        File f;
        if(lastSavedFile != null)       f = lastSavedFile;
        else if(lastOpenedFile != null) f = lastOpenedFile;
        else                            return;
        
        String name = f.getName();
        if(name.endsWith(".kin"))
        {
            if(name.length() > 6 && name.charAt(name.length()-6) == '.')
            {
                String  prefix  = name.substring(0, name.length()-6);
                char    version = name.charAt(name.length()-5);
                if('0' <= version && version < '9')         name = prefix+"."+(++version)+".kin";
                else if(version == '9')                     name = prefix+".a.kin";
                else if('a' <= version && version < 'z')    name = prefix+"."+(++version)+".kin";
                else                                        name = prefix+"."+version+".1.kin";
            }
            else
            {
                String prefix = name.substring(0, name.length()-4);
                name = prefix+".1.kin";
            }
        }
        
        f = new File(f.getParent(), name);
        fileSaveChooser.setCurrentDirectory(f);
        fileSaveChooser.setSelectedFile(f);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

