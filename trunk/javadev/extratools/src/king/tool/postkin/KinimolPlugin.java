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
//}}}

public class KinimolPlugin extends Plugin {
  
  //{{{ Constants
  //}}}

  //{{{ Variables
  //}}}
  
  //{{{ Constructor
  public KinimolPlugin(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ savePdb
  public void savePdb(File f) {
    Iterator groups = kMain.getKinemage().iterator();
    while (groups.hasNext()) {
      KGroup group = (KGroup) groups.next();
      if (group.getName().equals("loops")) {
        Iterator subs = group.iterator();
        while (subs.hasNext()) {
          KGroup sub = (KGroup) subs.next();
          HashMap<String, KGroup> groupMap = new HashMap<String, KGroup>();
          KIterator<KList> lists = KIterator.allLists(sub);
          for (KList list : lists) {
            KPoint pt = list.getChildren().get(0);
            String pdbName = KinUtil.getPdbName(pt.getName());
            if (pdbName != null) {
              if (groupMap.containsKey(pdbName)) {
                KGroup pdbGroup = groupMap.get(pdbName);
                pdbGroup.add(list);
              } else {
                KGroup pdbGroup = new KGroup("");
                pdbGroup.add(list);
                groupMap.put(pdbName, pdbGroup);
              }
            }
          }
          File pdbout = new File(f, sub.getName() + ".pdb");
          if( !pdbout.exists() ||
            JOptionPane.showConfirmDialog(kMain.getTopWindow(),
          "The file " + pdbout.toString() + " exists -- do you want to overwrite it?",
          "Overwrite file?", JOptionPane.YES_NO_OPTION)
          == JOptionPane.YES_OPTION )
          {
            try {
              Writer w = new FileWriter(pdbout);
              PrintWriter out = new PrintWriter(new BufferedWriter(w));
              int i = 1;
              for (String pdbName : groupMap.keySet()) {
                out.println("MODEL     " + Kinimol.formatStrings(Integer.toString(i), 4));
                out.print(Kinimol.convertGrouptoPdb(groupMap.get(pdbName), pdbName));
                out.println("ENDMDL");
                i++;
              }
              out.flush();
              w.close();
            } catch (IOException ex) {
              System.out.println(ex);
              JOptionPane.showMessageDialog(kMain.getTopWindow(),
              "An error occurred while saving the file.",
              "Sorry!", JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      }
    }
  }
  
  //}}}
  
  //{{{ onConvert
  public void onConvert(ActionEvent ev) {
    JFileChooser saveChooser = new JFileChooser();
    saveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    String currdir = System.getProperty("user.dir");
    if(currdir != null) {
	    saveChooser.setCurrentDirectory(new File(currdir));
    }
    if (saveChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow())) {
	    File f = saveChooser.getSelectedFile();
      savePdb(f);
      
    }
    
  }
  //}}}
  
  //{{{ getToolsMenuItem
  public JMenuItem getToolsMenuItem() {
    JMenuItem menu = new JMenuItem(new ReflectiveAction("Kin -> PDB", null, this, "onConvert"));
    return menu;
  }
  //}}}
  
}
