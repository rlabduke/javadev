// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
import king.core.*;
import king.points.*;
import king.tool.util.*;
import king.tool.postkin.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>PdbExport</code> exports PDB files from kinemage files.  Obviously this is 
* extremely dependent on the point ID format of all the points.  This is taken pretty
* much straight from the KinFudger tool.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Sep 29 09:33:14 EDT 2003
*/
public class PdbExport extends Plugin implements PropertyChangeListener, Runnable
{
//{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  static final DecimalFormat df2 = new DecimalFormat("0.00");
//}}}

//{{{ Variable definitions
//##############################################################################
    JFileChooser        chooser;
    SuffixFileFilter    pdbFilter;
    //HashMap adjacencyMap;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PdbExport(ToolBox tb)
    {
        super(tb);
        buildChooser();
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        pdbFilter = new SuffixFileFilter("PDB Format");
        pdbFilter.addSuffix(".pdb");
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(pdbFilter);
        chooser.setFileFilter(pdbFilter);
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        chooser.addPropertyChangeListener(this);
    }
//}}}

  //{{{ buildAdjacencyList
  public HashMap buildAdjacencyList(AGE groupElement) {
    HashMap adjacencyMap = new HashMap();
    //Kinemage kin = kMain.getKinemage();
    
    KIterator<KPoint> iter = KIterator.allPoints(groupElement);
    for (KPoint point : iter) {
      //System.out.println(point.getName());
      if (point.getName().length()>11){
        if (point instanceof VectorPoint) {
          VectorPoint currPoint = (VectorPoint) point;
          if ((!currPoint.isBreak())/*&&(currPoint.isOn())*/) {
            VectorPoint prevPoint = (VectorPoint) currPoint.getPrev();
            addPoints(prevPoint, currPoint, adjacencyMap);
            addPoints(currPoint, prevPoint, adjacencyMap);
          }
        }
      }
    }
    return adjacencyMap;
  }
  //}}}

  //{{{ addPoints
  private void addPoints(VectorPoint prev, VectorPoint curr, HashMap adjacencyMap) {
    if (adjacencyMap.containsKey(prev)) {
	    HashSet prevSet = (HashSet) adjacencyMap.get(prev);
	    prevSet.add(curr);
    } else {
	    HashSet prevSet = new HashSet();
	    prevSet.add(curr);
	    adjacencyMap.put(prev, prevSet);
    }
  }
  //}}}
  
//{{{ exportPDB
//##############################################################################
    public void exportPDB(KinCanvas kCanvas, File outfile)
        throws IOException
    { 
      Writer w = new FileWriter(outfile);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    Kinemage kin = kMain.getKinemage();
	    KIterator<KGroup> kgroupIter = KIterator.visibleGroups(kin);
	    while (kgroupIter.hasNext()) {
	      KGroup group = kgroupIter.next();
	      if (group.isDeepestGroup()) {
	        out.print(getPdbText(group));
	      }
	    }
	    out.flush();
	    w.close();
    }
//}}}

//{{{ getPdbText
  public String getPdbText(AGE groupElement) {
    StringBuilder pdbBuilder = new StringBuilder();
    HashMap adjacencyMap = buildAdjacencyList(groupElement);
    //Set keys = adjacencyMap.keySet();
    int i = 1;
    PointComparator pc = new PointComparator();
    TreeSet keyTree = new TreeSet(pc);
    keyTree.addAll(adjacencyMap.keySet());
    Iterator iter = keyTree.iterator();
    while (iter.hasNext()) {
      AbstractPoint point = (AbstractPoint) iter.next();
      //System.out.println(point + " POINT ON:" + pointActuallyOn(point));
      //if (pointActuallyOn(point)) {
      if (pointActuallyOn(point)) {
        //System.out.println(point);
        //System.out.println(KinPointIdParser.getResNumber(point.getName().toUpperCase()));
        pdbBuilder.append("ATOM  ");
        pdbBuilder.append(formatStrings(String.valueOf(i), 5) + " ");
        //out.print(point.getName().toUpperCase().substring(0, 8) + "  " + point.getName().toUpperCase().substring(8) + "     ");
        String atomName = PointComparator.getAtomName(point.getName().toUpperCase());
        if (atomName.equals("UNK ")) {
          System.err.println(point+" had an unknown atom name, is this expected?");
        }
        pdbBuilder.append(PointComparator.getAtomName(point.getName().toUpperCase()));
        pdbBuilder.append(KinPointIdParser.getAltConf(point.getName().toUpperCase()));
        pdbBuilder.append(KinPointIdParser.getResName(point.getName().toUpperCase()) + " ");
        pdbBuilder.append(KinPointIdParser.getChainID(point.getName()).toUpperCase());
        pdbBuilder.append(formatStrings(String.valueOf(KinPointIdParser.getResNumber(point.getName().toUpperCase())), 4) + "    ");
        pdbBuilder.append(formatStrings(df.format(point.getX()), 8));
        pdbBuilder.append(formatStrings(df.format(point.getY()), 8));
        pdbBuilder.append(formatStrings(df.format(point.getZ()), 8));
        pdbBuilder.append(formatStrings(df2.format(KinPointIdParser.getOccupancy(point)), 6));
        pdbBuilder.append(formatStrings(df2.format(KinPointIdParser.getBvalue(point.getName().toUpperCase())), 6)+"\n");
        i++;
      }
    }
    return pdbBuilder.toString();
  }
//}}}

  public boolean pointActuallyOn(AbstractPoint point) {
    AHE element = point;
    while (element.getDepth() > 0) {
      if (!element.isOn()) return false;
      element = element.getParent();
    }
    return true;
  }

//{{{ formatString
  public String formatStrings(String value, int numSpaces) {
    while (value.length() < numSpaces) {
	    value = " " + value;
    }
    return value;
    //if (coord < 0) {
      //    return (df.format(coord));
    //}
    //return " " + (df.format(coord));
  }
  //}}}

//{{{ askExport
//##############################################################################
    public void askExport()
    {
        // Auto-generate a file name
        propertyChange(null);
        
        // Show the Save dialog
        String currdir = System.getProperty("user.dir");
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(kMain.getTopWindow()))
        {
            File f = chooser.getSelectedFile();
            if(!pdbFilter.accept(f) &&
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file has the wrong extension. Append '.pdb' to the name?",
            "Fix extension?", JOptionPane.YES_NO_OPTION))
            {
                f = new File(f+".pdb");
            }

                
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    exportPDB(kMain.getCanvas(), f);
                    JOptionPane.showMessageDialog(kMain.getTopWindow(), "PDB export completed!");
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file:\n"+ex.getMessage(),
                        "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
                System.setProperty("user.dir", f.getAbsolutePath());
            }
        }
    }
//}}}

//{{{ propertyChange, run
//##################################################################################################
    public void propertyChange(PropertyChangeEvent ev)
    {
        if(ev == null
        || JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(ev.getPropertyName())
        || JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(ev.getPropertyName()))
        {
            // Has to be done "asynchronously" or file name will be corrupted
            SwingUtilities.invokeLater(this);
        }
    }
    
    public void run()
    {
        String fmt = "pdb";
        // Autogenerate an output name.
        for(int i = 1; i < 1000; i++)
        {
            File f = new File(chooser.getCurrentDirectory(), "kingsnap"+i+"."+fmt);
            if(!f.exists())
            {
                chooser.setSelectedFile(f);
                break;
            }
        }
    }
//}}}

//{{{ getToolsMenuItem, getHelpMenuItem, toString, onExport, isAppletSafe
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
      JMenu exportMenu = new JMenu("PDB file...");
      exportMenu.add(new JMenuItem(new ReflectiveAction("From visible groups", null, this, "onExport")));
      return exportMenu;
    }

    public JMenuItem getHelpMenuItem()
    { return null; }
    
    public String toString()
    { return "PDB file..."; }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExport(ActionEvent ev)
    { this.askExport(); }

    static public boolean isAppletSafe()
    { return false; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

