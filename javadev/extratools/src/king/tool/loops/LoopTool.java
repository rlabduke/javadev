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
    JButton delButton, keepButton;
    JTextField lowNumField, highNumField;
    TablePane pane;



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

	keepButton = new JButton(new ReflectiveAction("Keep!", null, this, "onKeep"));
	delButton = new JButton(new ReflectiveAction("Delete rest!", null, this, "onDelete"));

	pane = new TablePane();
	pane.newRow();
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(keepButton);
	pane.add(delButton);

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
