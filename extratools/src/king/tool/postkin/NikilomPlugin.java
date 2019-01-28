// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.tool.util.*;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>NikilomPlugin</code> builds a PDB file from a kinemage.
* It builds on Vince's Kinimol, but tries to be more generic & reliable.
*
* <p>Copyright (C) 2012 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Fri Jan 6 2011
*/
public class NikilomPlugin extends Plugin {
  
//{{{ Constants
//##############################################################################
//}}}

//{{{ Variables
//##############################################################################
//}}}
  
//{{{ Constructor
//##############################################################################
    public NikilomPlugin(ToolBox tb)
    {
        super(tb);
    }
//}}}
  
//{{{ savePdb
//##############################################################################
    public void savePdb(File f)
    {
      try
      {
        FileWriter w = new FileWriter(f, false); // to not append (?)
        PrintWriter out = new PrintWriter(new BufferedWriter(w));
        
        out.println(getPdbText());
        
        out.flush();
        w.close();
      }
      catch(IOException ex)
      {
        System.out.println(ex);
        JOptionPane.showMessageDialog(kMain.getTopWindow(),
          "An error occurred while saving the file.",
          "Sorry!", JOptionPane.ERROR_MESSAGE);
      }
    }
//}}}

//{{{ getPdbText
  public String getPdbText() {
    String pdbText = "";
    Iterator groups = kMain.getKinemage().iterator();
    int groupCount = 0;
    while(groups.hasNext())
    {
      KGroup group = (KGroup) groups.next();
      if(group.isOn())
      {
        groupCount++;

        pdbText = pdbText.concat("MODEL     "+Strings.forceRight(""+groupCount, 4)+"\n");
        pdbText = pdbText.concat(Kinimol.convertGrouptoPdb(group, group.getName()));
        pdbText = pdbText.concat("ENDMDL\n");
       
      }
    }
    return pdbText;
  }
//}}}
  
//{{{ onConvert
//##############################################################################
    public void onConvert(ActionEvent ev)
    {
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String currdir = System.getProperty("user.dir");
        if(currdir != null)
        {
            saveChooser.setCurrentDirectory(new File(currdir));
        }
        if(saveChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow()))
        {
            File f = saveChooser.getSelectedFile();
            if(!f.exists() ||
            JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "The file "+f.toString()+" exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
                savePdb(f);
            }
        }
    }
//}}}
  
//{{{ getToolsMenuItem, toString
//##############################################################################
    public JMenuItem getToolsMenuItem()
    {
        JMenuItem menu = new JMenuItem(new ReflectiveAction("Kin -> PDB 2.0", null, this, "onConvert"));
        return menu;
    }
    
    public String toString()
    {
        return "Kin -> PDB 2.0";
    }
//}}}
}//class

