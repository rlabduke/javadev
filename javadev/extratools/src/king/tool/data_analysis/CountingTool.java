// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;
import king.points.*;
import king.io.*;
import king.tool.util.KinUtil;

import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
import driftwood.r3.*;
//}}}

public class CountingTool extends BasicTool {
  
  //{{{ Variable definitions
  //##################################################################################################
  JTextField searchField;
  JCheckBox doOnlyActiveBox;
  JList countList;
  DefaultListModel listModel;
  //}}}
  
  //{{{ Constructor(s)
  //##################################################################################################
  /**
  * Constructor
  */
  public CountingTool(ToolBox tb)
  {
    super(tb);
    buildGUI();
  }
  //}}}

  //{{{ buildGUI
  //##############################################################################
  private void buildGUI() {
    searchField = new JTextField(10);
    doOnlyActiveBox = new JCheckBox("Count only active");
    listModel = new DefaultListModel();;
    countList = new JList(listModel);
    JScrollPane scroll = new JScrollPane(countList);
    
    TablePane2 pane = new TablePane2();
    pane.newRow();
    pane.hfill(true);
    pane.add(searchField, 2, 1);
    pane.add(new JButton(new ReflectiveAction("Count", null, this, "onCount")));
    pane.newRow();
    pane.hfill(true).vfill(true);
    pane.add(scroll, 2, 2);
    pane.add(doOnlyActiveBox);
    pane.newRow();
    pane.add(new JButton(new ReflectiveAction("Clear", null, this, "onClear")));
    
    dialog = new JDialog(kMain.getTopWindow(), "Counting Tool", false);
    //dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(this);
    dialog.setContentPane(pane);
    //dialog.setJMenuBar(menubar);
    dialog.pack();
	}
	//}}}
	
	//{{{ onCount
	public void onCount(ActionEvent ev) {
    int count = 0;
    String searchString = searchField.getText();
    System.out.println(searchString);
    KIterator<KPoint> iter;
    if (doOnlyActiveBox.isSelected()) {
      iter = KIterator.visiblePoints(kMain.getKinemage());
    } else {
      iter = KIterator.allPoints(kMain.getKinemage());
    }
    for (KPoint p : iter) {
      if (p.getName().indexOf(searchString)!=-1) {
        count++;
      }
    }
    listModel.addElement(searchString+":"+Integer.toString(count));
	}
	//}}}
	
	//{{{ onClear
	public void onClear(ActionEvent ev) {
	  listModel.clear();
	}
	//}}}

	//{{{ getToolPanel, getHelpURL/Anchor, toString
	//##################################################################################################
	/** Returns a component with controls and options for this tool */
	protected Container getToolPanel()
	{ return dialog; }
	
	/** Returns the URL of a web page explaining use of this tool */
	public URL getHelpURL()
	{
	  URL     url     = getClass().getResource("/extratools/tools-manual.html");
	  String  anchor  = getHelpAnchor();
	  if(url != null && anchor != null)
	  {
	    try { url = new URL(url, anchor); }
	    catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
	    return url;
	  }
	  else return null;
	}
	
	/**
	* Returns an anchor marking a place within <code>king-manual.html</code>
	* that is the help for this tool. This is called by the default
	* implementation of <code>getHelpURL()</code>. 
	* If you override that function, you can safely ignore this one.
	* @return for example, "#navigate-tool" (or null)
	*/
	public String getHelpAnchor()
	{ return null; }
	
	public String toString() { return "Counting Tool"; }
	//}}}
}

