// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;
import king.tool.util.*;
import driftwood.gui.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FramerTool extends BasicTool {

//{{{ Variable definitions
//###############################################################
    TreeMap caMap;
    TreeMap oxyMap;

    JTextField numField;
    TablePane pane;

//}}}

    public FramerTool(ToolBox tb) {
	super(tb);
    }

    public void start() {
	//if (kMain.getKinemage() == null) return;
	buildGUI();
	caMap = new TreeMap();
	oxyMap = new TreeMap();
	show();
    }  

    //{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {
	dialog = new JDialog(kMain.getTopWindow(), "Loops", false);
	numField = new JTextField("", 5);
	
	pane = new TablePane();
	pane.newRow();
	pane.add(numField);

	dialog.addWindowListener(this);
	dialog.setContentPane(pane);
    }
    //}}}

    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
	super.click(x, y, p, ev);
	if (p != null) {
	    KList parent = (KList) p.getOwner();
	    //while !(parent instanceof Kinemage) {
	    //	parent = parent.getOwner();
	    //}
	    //TreeMap caMap = new TreeMap();
	    //TreeMap oxyMap = new TreeMap();
	    if (caMap.isEmpty()) {
		splitKin(parent, caMap, oxyMap);
	    }
	    if (KinUtil.isNumeric(numField.getText())) {
		int numPep = Integer.parseInt(numField.getText());
		int resNum = KinUtil.getResNumber(p);
		KPoint ca0 = (KPoint) caMap.get(new Integer(resNum - 1));
		KPoint ca1 = (KPoint) caMap.get(new Integer(resNum));
		KPoint caN = (KPoint) caMap.get(new Integer(resNum + numPep));
		KPoint caN1 = (KPoint) caMap.get(new Integer(resNum + numPep + 1));
		KPoint co1 = (KPoint) oxyMap.get(new Integer(resNum));
		KPoint coN = (KPoint) oxyMap.get(new Integer(resNum + numPep));
		//System.out.println(ca0);
		Framer.calphaAnalyze(ca0, ca1, caN, caN1, co1, coN);
	    } else {
	    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
						  JOptionPane.ERROR_MESSAGE);
	    }
	    
	}
    }

    public void splitKin(AGE target, TreeMap caMap, TreeMap oxyMap) {
	//TreeMap caMap = new HashMap();
	//TreeMap oxyMap = new HashMap();
	if (target instanceof Kinemage) {
	}
	if ((target instanceof KList)&&(target.isOn())) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		int resNum = KinUtil.getResNumber(pt);
		String atomName = KinUtil.getAtomName(pt).toLowerCase();
		if (atomName.equals("ca")) {
		    caMap.put(new Integer(resNum), pt);
		}
		if (atomName.equals("o")) {
		    oxyMap.put(new Integer(resNum), pt);
		}    
	    }
	} else {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		splitKin((AGE) iter.next(), caMap, oxyMap);
	    }
	}
    }
	


//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return dialog; }

    public String toString() { return "Framer Tool"; }    


}
