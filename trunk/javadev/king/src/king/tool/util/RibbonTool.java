// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;

import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import driftwood.gui.*;
import javax.swing.*;
import javax.swing.event.*;
//}}}
/**
 * <code>RibbonTool</code> was created in response to numerous requests for the ability to color individual ribbons 
 * of a kinemage quickly and easily.  It supports coloring single ribbons, multiple ribbons of a type, and 
 * regions of ribbons in a Prekin-created ribbon kinemage.  
 *
 * <p>Copyright (C) 2004 by Vincent B. Chen. All rights reserved.
 * <br>Begun on Apr 21 2004
 **/


public class RibbonTool extends BasicTool implements ActionListener {

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################

    // sortedKin is hashmap with masters as keys, arraylists as values
    // ribbonMap is hashmap with Klists as keys, arraylists as values
    HashMap sortedKin, ribbonMap;
    // sortbyNum is hashmap with Integer resNum as keys, arraylists as values
    HashMap sortbyNum;

    JRadioButton colorOne, colorAll;
    JRadioButton chooseColor, checkSort, colorRegion, hippieMode;
    TablePane pane;
    JComboBox   color1;
    JTextField lowNumField;
    JTextField highNumField;

//}}}

//{{{ Constructor(s)
//##############################################################################
    public RibbonTool(ToolBox tb)
    {
        super(tb);
	newGroup();

        //undoStack = new LinkedList();
	buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        
        //dialog = new JDialog(kMain.getTopWindow(),"Ribbons", false);
	color1 = new JComboBox(KPalette.getStandardMap().values().toArray());
	color1.setSelectedItem(KPalette.blue);
        color1.addActionListener(this);

	colorOne = new JRadioButton("Color One", true);
	colorAll = new JRadioButton("Color All", false);
	ButtonGroup colorRange = new ButtonGroup();
	colorRange.add(colorOne);
	colorRange.add(colorAll);

	chooseColor = new JRadioButton("Choose Color", true);
	checkSort = new JRadioButton("Check Sort", false);
	colorRegion = new JRadioButton("Color Region", false);
	hippieMode = new JRadioButton("Hippie Mode", false);
	ButtonGroup colorType = new ButtonGroup();
	colorType.add(chooseColor);
	colorType.add(checkSort);
	colorType.add(colorRegion);
	colorType.add(hippieMode);

	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);

        pane = new TablePane();
	pane.newRow();
        pane.add(color1);
        //pane.newRow();
	pane.add(colorOne);
	pane.add(colorAll);
	pane.newRow();
	pane.add(chooseColor);
	pane.add(colorRegion);
	pane.add(hippieMode);
	pane.add(checkSort);
	pane.newRow();
	pane.add(lowNumField);
	pane.add(highNumField);

    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if (p != null) {
	    KList parentList = (KList) p.getOwner();
	    if (!ribbonMap.containsKey(parentList)) {
		newGroup();
		sortStructure(p);
		sortbyNumber(p);
	    }
	    //debug();
	    if (colorAll.isSelected()) {
		highlightAll(p);
	    } else if (checkSort.isSelected()) {
		checkSort(p);
	    } else if (colorRegion.isSelected()) {
		highlightRange(p);
	    } else if (hippieMode.isSelected()) {
		highlightHippie(p);
	    } else {
		highlight(p);
	    }
	    Kinemage k = kMain.getKinemage();
	    if(k != null) k.setModified(true);
	}

    }
//})}

// event functions
//###############################################################################################
    /**
     * Event handler for when action performed.
     */
    public void actionPerformed(ActionEvent ev) {

        kCanvas.repaint();
    }

    /**
     * Event handler for when tool window closed.
     */
    public void windowClosing(WindowEvent ev) {
	newGroup();
	parent.activateDefaultTool(); 
    }

//{{{ sortStructure
//##################################################################################################
    /**
     * Initializer function that handles the sorting of a prekin-generated ribbon kinemage.  It sorts
     * based on the second master of each KList in the kinemage.  This function needs to be called
     * before any of the highlighting functions can be called.
     **/
    
    public void sortStructure(KPoint p) {

	String pointID = p.getName().trim();
	KList parentList = (KList) p.getOwner();
	KSubgroup parentGroup = (KSubgroup) parentList.getOwner();
	Iterator iter = parentGroup.iterator();
	KList list;
	ArrayList listofLists = new ArrayList();
	//Kinemage kin = kMain.getKinemage();
	//KGroup group = new KGroup(kin, "sorted");

	while (iter.hasNext()) {
	    list = (KList) iter.next();
	    String master = getOldMaster(list);
	    listofLists = (ArrayList) sortedKin.get(master);
	    if (listofLists == null) {
		listofLists = new ArrayList();
	    } 
	    listofLists.add(list);
	    sortedKin.put(master, listofLists);
	}
	Set keys = sortedKin.keySet();
	iter = keys.iterator();
	while (iter.hasNext()) {
	    String master = (String) iter.next();
	    listofLists = (ArrayList) sortedKin.get(master);
	    splitStructure(listofLists);
	    //KSubgroup sub = new KSubgroup(group, master);
	    //Iterator listIter = listofLists.iterator();
	    //while (listIter.hasNext()) {
	    //	list = (KList) listIter.next();
	    //	sub.add(list);
	    //}
	    //group.add(sub);
	}	
	//kin.add(group);
	//initiated = true;
    }
//}}}

//{{{ sortbyNumber
//####################################################################################################
    /**
     * Helper function to sort p's parent by residue number. Stores in Hashmap with residue number as key, and
     * arraylist of KLists as value.
     **/
    private void sortbyNumber(KPoint p) {
	String pointID = p.getName().trim();
	KList parentList = (KList) p.getOwner();
	KSubgroup parentGroup = (KSubgroup) parentList.getOwner();
	Iterator iter = parentGroup.iterator();
	KList list;
	ArrayList listofLists = new ArrayList();
	
	while (iter.hasNext()) {
	    list = (KList) iter.next();
	    Integer resNum = getResNumber(list);
	    if (sortbyNum.containsKey(resNum)) {
		listofLists = (ArrayList) sortbyNum.get(resNum);
		listofLists.add(list);
	    } else {
		listofLists = new ArrayList();
		listofLists.add(list);
		sortbyNum.put(resNum, listofLists);
	    }
	}
    }
//}}}    

//{{{ splitStructure
//###################################################################################################
    /**
     * Helper function to sort srcLists into separate ribbons.  Stores in Hashmap with KList as key, and an 
     * arraylist of KLists (all of one ribbon) as values.
     **/
    private void splitStructure(ArrayList srcLists) {

	Iterator iter = srcLists.iterator();
	ArrayList listofLists = new ArrayList();
	Integer oldresNum = new Integer(-100);


	// gets each KList, gets first point of Klist, checks whether within one of previous list,
	// then sorts appropriately
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    Integer resNum = getResNumber(list);
	    if ((resNum.equals(oldresNum))||(resNum.equals(new Integer(oldresNum.intValue() + 1)))) {
		listofLists.add(list);
		ribbonMap.put(list, listofLists);
		oldresNum = resNum;
	    } else {
		listofLists = new ArrayList();
		listofLists.add(list);
		ribbonMap.put(list, listofLists);
		oldresNum = resNum;
	    }
	}
    }
//}}}

//{{{ getResNumber
//###################################################################################################
    /**
     * Helper function to get the residue number of parentList.  It gets the first KPoint in the KList, 
     * and extracts the residue number from the name.  EXTREMELY dependent on the format of the name of the KPoint.
     **/
    private Integer getResNumber(KList parentList) {
	Iterator pointIter = parentList.iterator();
	KPoint firstPoint = (KPoint) pointIter.next();
	String pointID = firstPoint.getName().trim();
	return Integer.valueOf(pointID.substring(pointID.lastIndexOf(" ") + 1));
    }
//}}}

    //public boolean initiated() {
    //	return initiated;
    //}


//{{{ highlightRange
//###################################################################################################
    /**
     * For highlighting a range of a ribbon kinemage. The first time this function is called, it stores p
     * as the starting point, and the second time it's called, it colors the region between the stored p
     * and the current p.  
     *
     **/
    public void highlightRange(KPoint p) {
	if (!highNumField.getText().equals("")) {
	    lowNumField.setText("");
	    highNumField.setText("");
	}
	KList parentList = (KList) p.getOwner();
	Integer resNum = getResNumber(parentList);
	if(lowNumField.getText().equals("")) {
	    lowNumField.setText(resNum.toString());
	} else if (highNumField.getText().equals("")) {
	    highNumField.setText(resNum.toString());
	}
	if (!highNumField.getText().equals("")) {
	    int firstNum = Integer.parseInt(lowNumField.getText());
	    int secondNum = Integer.parseInt(highNumField.getText());
	    if (firstNum > secondNum) {
		int temp = secondNum;
		secondNum = firstNum;
		firstNum = temp;
	    }
	    for (int i = firstNum; i <= secondNum; i++) {
		Integer hashKey = new Integer(i);
		if (sortbyNum.containsKey(hashKey)) {
		    ArrayList listofLists = (ArrayList) sortbyNum.get(hashKey);
		    Iterator iter = listofLists.iterator();
		    while (iter.hasNext()) {
			KList list = (KList) iter.next();
			list.setColor((KPaint) color1.getSelectedItem());
		    }
		}
	    }
	    //lowNumField.setText("");
	    //highNumField.setText("");
	}
    }
//}}}

//{{{ highlightAll
//#######################################################################################################
    /**
     * For coloring all ribbons of a certain type (alpha, beta, coil).  
     **/
    public void highlightAll(KPoint p) {
	KList parentList = (KList) p.getOwner();
	String master = getOldMaster(parentList);
	ArrayList listofLists = (ArrayList) sortedKin.get(master);
	Iterator iter = listofLists.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    list.setColor((KPaint) color1.getSelectedItem());
	}
    }
//}}}

//{{{ highlight
//#######################################################################################################
    /**
     * For coloring one particular ribbon (one stretch of alpha, beta, or coil ribbon).
     **/
    public void highlight(KPoint p) {
	KList parentList = (KList) p.getOwner();
	ArrayList listofLists = (ArrayList) ribbonMap.get(parentList);
	Iterator iter = listofLists.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    list.setColor((KPaint) color1.getSelectedItem());
	}
    }
//}}}    

//{{{ highlightHippie
//#######################################################################################################
    /**
     * For coloring one particular ribbon (one stretch of alpha, beta, or coil ribbon) with "hippie" colors.
     **/
    public void highlightHippie(KPoint p) {
	KList parentList = (KList) p.getOwner();
	ArrayList listofLists = (ArrayList) ribbonMap.get(parentList);
	Iterator iter = listofLists.iterator();
	int index = 0;
	Object colors[] = KPalette.getStandardMap().values().toArray();
	while (iter.hasNext()) {
	    if (index > (colors.length - 6)) {
		index = 0;
	    }
	    KList list = (KList) iter.next();
	    list.setColor((KPaint) colors[index]);
	    index++;
	}
    }
//}}}

// checkSort
//################################################################################################
    /**
     * For checking the natural sorting of a kinemage.  It colors each section of ribbon with a different color.
     * If the kinemage is sorted nicely, the colors will correspond to each section of ribbon exactly.  If not
     * sorted nicely, then each section of ribbon will have multiple different colors.  
     * Currently this function has nice patriotic (if you're american) colors.
     **/
    public void checkSort(KPoint p) {
	String pointID = p.getName().trim();
	KList parentList = (KList) p.getOwner();
	KSubgroup parentGroup = (KSubgroup) parentList.getOwner();
	Iterator iter = parentGroup.iterator();
	// Assumes last part of point ID is the residue number
	Integer resNum = Integer.valueOf(pointID.substring(pointID.lastIndexOf(" ") + 1));
	int i = 0;
	KList list;
	String oldMaster = "nothing";
	Kinemage kin = kMain.getKinemage();

	while (iter.hasNext()) {
	    list = (KList) iter.next();
	    if (list.hasMaster(oldMaster)) {
		//list.addMaster(oldMaster + Integer.toString(i));
		if (i == 0) {
		    list.setColor(KPalette.blue);
		} else if (i == 1) {
		    list.setColor(KPalette.red);
		} else {
		    list.setColor(KPalette.white);
		}
		//sub.add(list);
	    } else {
		//if (sub != null) group.add(sub);
		oldMaster = getOldMaster(list);
		//sub = new KSubgroup(group, oldMaster);
		if (i <= 1) {
		    i++;
		} else {
		    i = 0;
		}

		if (i == 0) {
		    list.setColor(KPalette.blue);
		} else if (i == 1) {
		    list.setColor(KPalette.red);
		} else {
		    list.setColor(KPalette.white);
		}

	    }
	}

    }
//}}}

//{{{ getOldMaster
//######################################################################################################
    /**
     * Helper function that (hopefully) gets the "ribbon master" (alpha, beta, coil) from list.  Depends on
     * prekin giving the ribbon master as the second master of the list.
     **/
    private String getOldMaster(KList list) {
	Iterator masIter = list.masterIterator();
	String oldMaster = (String) masIter.next();
	//while (masIter.hasNext()) {

	//if (oldMaster == "ribbon") {

	    oldMaster = (String) masIter.next();

	    //}

	return oldMaster;
    }
//}}}

//{{{ newGroup
//######################################################################################################
    /**
     * Resets the ribbon tool by creating new hashmaps for the various list storing maps. This is for coloring
     * ribbons in different KGroups, or in new kinemages.
     **/
    public void newGroup() {
	//System.out.println("new group");
	sortedKin = new HashMap();
	ribbonMap = new HashMap();
	sortbyNum = new HashMap();
	//initiated = false;
    }
//}}}

//{{{ debug
//######################################################################################################
    /**
     * debugger function.
     **/
    public void debug() {
	Set keys = sortedKin.keySet();
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    String master = (String) iter.next();
	    ArrayList listofLists = (ArrayList) sortedKin.get(master);
	    System.out.print(master + ": ");
	    System.out.print(listofLists.size() + "; ");
	}
	System.out.println("");
    }
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return pane; }

    public String getHelpAnchor()
    { return "#ribbon-tool"; }

    public String toString() { return "Ribbons"; }
//}}}
}//class

