// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import driftwood.gui.*;

public class LoopTool extends BasicTool {

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################
    HashSet keptSet;
    JButton delButton, keepButton, removeButton;
    JTextField lowNumField, highNumField;
    TablePane pane;
    JList keptList;
    DefaultListModel listModel;


    public LoopTool(ToolBox tb) {
	super(tb);
	//buildGUI();
    }
    
//{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {

	dialog = new JDialog(kMain.getTopWindow(), "Loops", false);
	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);
	listModel = new DefaultListModel();
	keptList = new JList(listModel);

	keepButton = new JButton(new ReflectiveAction("Keep!", null, this, "onKeep"));
	delButton = new JButton(new ReflectiveAction("Delete rest!", null, this, "onDelete"));
	removeButton = new JButton(new ReflectiveAction("Remove last", null, this, "onRemove"));

	pane = new TablePane();
	pane.newRow();
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(keepButton);
	pane.add(delButton);
	pane.newRow().save().hfill(true).vfill(true);
	pane.add(new JScrollPane(keptList), 4, 1);
	pane.newRow().restore();
	pane.add(removeButton, 2, 1);

	dialog.setContentPane(pane);
    }

//}}}

    public void start() {
	if (kMain.getKinemage() == null) return;
	buildGUI();
	keptSet = new HashSet();
	//colorator = new RecolorNonRibbon();
	show();
    }   


    public void onKeep(ActionEvent ev) {
	if (KinUtil.isNumeric(lowNumField.getText())&&(KinUtil.isNumeric(highNumField.getText()))) {
	    int firstNum = Integer.parseInt(lowNumField.getText());
	    int secondNum = Integer.parseInt(highNumField.getText());
	    if (firstNum > secondNum) {
		int temp = secondNum;
		secondNum = firstNum;
		firstNum = temp;
	    }
	    for (int i = firstNum; i <= secondNum; i++) {
		keptSet.add(new Integer(i));
	    }
	    listModel.addElement(Integer.toString(firstNum) + " to " + Integer.toString(secondNum));
	} else {
	    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
						  JOptionPane.ERROR_MESSAGE);
	}
    }

    public void onDelete(ActionEvent ev) {
	//currently assuming kins formatted as lots.
	delete(kMain.getKinemage());
	kCanvas.repaint();
    }

    public void onRemove(ActionEvent ev) {
	//if(kept.size() > 0) tupleList.remove(tupleList.size()-1);
	if(listModel.size() > 0) {
	    String last = (String) listModel.get(listModel.size()-1);
	    listModel.remove(listModel.size()-1);
	    
	    String[] limits = last.split(" to ");
	    int firstNum = Integer.parseInt(limits[0]);
	    int secondNum = Integer.parseInt(limits[1]);
	    for (int i = firstNum; i <= secondNum; i++) {
		keptSet.remove(new Integer(i));
	    }
	}
	//if(markList.children.size() > 0) markList.children.remove(markList.children.size()-1);
	
    }

    private void delete(AGE target) {
	if (target instanceof KList) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		int resNum = KinUtil.getResNumber(pt);
		if (!keptSet.contains(new Integer(resNum))) {
		    iter.remove();
		} else if ((keptSet.contains(new Integer(resNum)))&&(!keptSet.contains(new Integer(resNum-1)))) {
		    if (pt instanceof VectorPoint) {
			VectorPoint vpoint = (VectorPoint) pt;
			KPoint prev = vpoint.getPrev();
			if (prev instanceof KPoint) {
			    if (!keptSet.contains(new Integer(KinUtil.getResNumber(prev)))) {
				vpoint.setPrev(null);
			    }
			}
		    }
		}
	    
		
		    
	    }
	} else {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		delete((AGE)iter.next());
	    }
	}
    }

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return dialog; }

    public String toString() { return "Loop Tool"; }    


}
