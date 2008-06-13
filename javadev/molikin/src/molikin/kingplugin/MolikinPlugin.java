// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.kingplugin;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.util.*;
import king.*;
//}}}
/**
* <code>MolikinPlugin</code> allows users to "open" PDB and mmCIF files
* from KiNG directly, rather than going thru Prekin.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Nov  9 13:54:31 EST 2005
*/
public class MolikinPlugin extends king.Plugin
{
//{{{ Constants
//}}}

//{{{ CLASS: CoordFileOpen
//##############################################################################
    private class CoordFileOpen implements FileDropHandler.Listener
    {
        public String toString()
        { return "Open the file in Molikin"; }
        
        public boolean canHandleDroppedFile(File file)
        {
            return pdbFilter.accept(file) || cifFilter.accept(file);
        }
        
        public void handleDroppedFile(File f)
        {
            try
            {
                if(pdbFilter.accept(f))         doPDB(f);
                else if(cifFilter.accept(f))    doCIF(f);
            }
            catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    SuffixFileFilter        pdbFilter, cifFilter, allFilter;
    JFileChooser            openChooser;
    JFrame                  frame;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MolikinPlugin(ToolBox tb)
    {
        super(tb);
        buildFileChooser();
        kMain.getFileDropHandler().addFileDropListener(new CoordFileOpen());
    }
//}}}

//{{{ buildFileChooser
//##################################################################################################
    /** Constructs the Open file chooser */
    private void buildFileChooser()
    {
        allFilter = new SuffixFileFilter("PDB and mmCIF files");
        allFilter.addSuffix(".pdb");
        allFilter.addSuffix(".xyz");
        allFilter.addSuffix(".ent");
        allFilter.addSuffix(".cif");
        allFilter.addSuffix(".mmcif");
        pdbFilter = new SuffixFileFilter("Protein Data Bank (PDB) files");
        pdbFilter.addSuffix(".pdb");
        pdbFilter.addSuffix(".xyz");
        pdbFilter.addSuffix(".ent");
        cifFilter = new SuffixFileFilter("mmCIF files");
        cifFilter.addSuffix(".cif");
        cifFilter.addSuffix(".mmcif");
        
        String currdir = System.getProperty("user.dir");

        openChooser = new JFileChooser();
        openChooser.addChoosableFileFilter(allFilter);
        openChooser.addChoosableFileFilter(pdbFilter);
        openChooser.addChoosableFileFilter(cifFilter);
        openChooser.setFileFilter(allFilter);
        if(currdir != null) openChooser.setCurrentDirectory(new File(currdir));
    }
//}}}

//{{{ onOpenFile, doPDB/CIF
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenFile(ActionEvent ev)
    {
        // Open the new file
        if(JFileChooser.APPROVE_OPTION == openChooser.showOpenDialog(kMain.getTopWindow()))
        {
            try
            {
                File f = openChooser.getSelectedFile();
                if(f != null && f.exists())
                {
                    //if(pdbFilter.accept(f))         doPDB(f);
                    if(cifFilter.accept(f))    doCIF(f);
                    else                       doPDB(f);
                    //else throw new IOException("Can't identify file type");
                }
            }
            catch(IOException ex)
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace(SoftLog.err);
            }
        }
    }
    
    void doPDB(File f) throws IOException
    {
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(f);
        new MolikinWindow(this.parent, coordFile);
    }

    void doCIF(File f) throws IOException
    {
        CifReader       cifReader   = new CifReader();
        CoordinateFile  coordFile   = cifReader.read(f);
        new MolikinWindow(this.parent, coordFile);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ toString, getToolsMenuItem
//##################################################################################################
    public String toString()
    {
        return "Molecules (PDB, mmCIF)";
    }
    
    /**
    * Creates a new JMenuItem to be displayed in the Tools menu,
    * which will allow the user to access function(s) associated
    * with this Plugin.
    *
    * Only one JMenuItem may be returned, but it could be a JMenu
    * that contained several functionalities under it.
    *
    * The Plugin may return null to indicate that it has no
    * associated menu item.
    */
    public JMenuItem getToolsMenuItem()
    {
        JMenuItem item = new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onOpenFile"));
        return item;
    }
//}}}

//{{{ getHelpURL, getHelpAnchor
//##################################################################################################
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        return null; // until we document this...
        
        /*URL     url     = getClass().getResource("/extratools/tools-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else return null;*/
    }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this plugin. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#edmap-plugin" (or null)
    */
    public String getHelpAnchor()
    { return null; }
//}}}
}//class

