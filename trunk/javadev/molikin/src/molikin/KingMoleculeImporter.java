// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
import king.*;
//}}}
/**
* <code>KingMoleculeImporter</code> allows users to "open" PDB and mmCIF files
* from KiNG directly, rather than going thru Prekin.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed May 11 09:16:35 EDT 2005
*/
public class KingMoleculeImporter extends king.Plugin
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    SuffixFileFilter        pdbFilter, cifFilter, allFilter;
    JFileChooser            openChooser;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KingMoleculeImporter(ToolBox tb)
    {
        super(tb);
        buildFileChooser();
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

//{{{ onOpenFile
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
                    Test test = new Test();
                    Reader in = new FileReader(f);
                    StreamTank kinData = new StreamTank();
                    Writer out = new OutputStreamWriter(kinData);
                    if(pdbFilter.accept(f))         test.doPDB(in, out);
                    else if(cifFilter.accept(f))    test.doCIF(in, out);
                    else throw new IOException("Can't identify file type");
                    
                    out.flush();
                    kinData.close();
                    kMain.getKinIO().loadStream(kinData.getInputStream(), kinData.size(), null);
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

