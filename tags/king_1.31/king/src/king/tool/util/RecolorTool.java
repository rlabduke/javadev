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
 * <code>RecolorTool</code> was created to make it easier to color
 * sections of kinemages.  
 *
 * <p>Copyright (C) 2004 by Vincent B. Chen. All rights reserved.
 * <br>Begun on Sept 15 2004
 **/

public class RecolorTool extends BasicTool implements ActionListener {

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################

    // sortedKin is hashmap with masters as keys, arraylists as values
    // ribbonMap is hashmap with Klists as keys, arraylists as values
    //HashMap sortedKin, ribbonMap;
    // sortbyNum is hashmap with Integer resNum as keys, arraylists as values
    //HashMap sortbyNum;
    //HashMap sortedKin;
    HashMap structMap;
    HashSet clickedLists;

    JRadioButton chooseColor, colorAll;
    JRadioButton colorRegion, hippieMode;
    TablePane pane;
    JComboBox   color1;
    JTextField lowNumField;
    JTextField highNumField;

//}}}

//{{{ Constructor(s)
//##############################################################################
    public RecolorTool(ToolBox tb)
    {
        super(tb);
	newGroup();
	clickedLists = new HashSet();

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

	//colorOne = new JRadioButton("Color One", false);
	colorRegion = new JRadioButton("Color Region", true);
	colorAll = new JRadioButton("Color All", false);
	//colorOne.addActionListener(this);
	colorRegion.addActionListener(this);
	colorAll.addActionListener(this);
	ButtonGroup colorRange = new ButtonGroup();
	//colorRange.add(colorOne);
	colorRange.add(colorRegion);
	colorRange.add(colorAll);

	chooseColor = new JRadioButton("Choose Color", true);
	//checkSort = new JRadioButton("Check Sort", false);
	//colorRegion = new JRadioButton("Color Region", false);
	hippieMode = new JRadioButton("Hippie Mode", false);
	//chooseColor.addActionListener(this);
	//checkSort.addActionListener(this);
	hippieMode.addActionListener(this);
	ButtonGroup colorType = new ButtonGroup();
	colorType.add(chooseColor);
	//colorType.add(checkSort);
	//colorType.add(colorRegion);
	colorType.add(hippieMode);

	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);

        pane = new TablePane();
	pane.newRow();
        pane.add(color1);
	pane.add(chooseColor);
	//pane.add(colorRegion);
	pane.add(hippieMode);
	// removing checksort from panel so people can't accidentally use it.
	//pane.add(checkSort);
        //pane.newRow();

	pane.newRow();
	//pane.add(colorOne);
	pane.add(colorRegion);
	pane.add(colorAll);

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
	    Integer resNumber = new Integer(getResNumber(p));
	    if (!structMap.containsKey(resNumber)||!clickedLists.contains(parentList)) {
		newGroup();
		splitStructure(p);
	    }

	    Kinemage k = kMain.getKinemage();
	    if(k != null) k.setModified(true);
	    //}
	    //}
	    if (colorAll.isSelected()) {
		highlightAll(p);
	    } else {
		highlightRange(p);
	    }
	}
    }
//})}

// event functions
//###############################################################################################
    /**
     * Event handler for when action performed.
     */
    public void actionPerformed(ActionEvent ev) {
	if (colorAll.isSelected()||colorRegion.isSelected()) {
	    //hippieMode.setEnabled(false);
	    //checkSort.setEnabled(false);
	    //chooseColor.setSelected(true);
	} else {
	    //hippieMode.setEnabled(true);
	    //checkSort.setEnabled(true);	
	}
	if (hippieMode.isSelected()) {
	    //colorRegion.setEnabled(false);
	    //colorAll.setEnabled(false);
	    //colorOne.setSelected(true);
	} else {
	    //colorRegion.setEnabled(true);
	    //colorAll.setEnabled(true);
	}

        kCanvas.repaint();
    }

    /**
     * Event handler for when tool window closed.
     */
    public void windowClosing(WindowEvent ev) {
	newGroup();
	parent.activateDefaultTool(); 
    }

//{{{ splitStructure
//##################################################################################################
    /**
     * 
     **/
    
    public void splitStructure(KPoint p) {

	String pointID = p.getName().trim();
	KList parentList = (KList) p.getOwner();
	//KSubgroup parentGroup = (KSubgroup) parentList.getOwner();
	Iterator iter = parentList.iterator();
	KPoint point;
	ArrayList listofLists = new ArrayList();

	while (iter.hasNext()) {
	    point = (KPoint) iter.next();
	    //String master = getOldMaster(list);
	    Integer resNumber = new Integer(getResNumber(point));
	    listofLists = (ArrayList) structMap.get(resNumber);
	    if (listofLists == null) {
		listofLists = new ArrayList();
	    } 
	    listofLists.add(point);
	    structMap.put(resNumber, listofLists);
	}
    }
//}}}

//{{{ getResNumber
//###################################################################################################
    /**
     * Helper function to get the residue number of parentList.  It gets the first KPoint in the KList, 
     * and extracts the residue number from the name.  EXTREMELY dependent on the format of the name of the KPoint.
     **/
    
    private int getResNumber(KPoint point) {
	String name = point.getName().trim();
	String[] uncleanParsed = name.split(" ");
	String[] parsed = new String[uncleanParsed.length];
        int i2 = 0;
	// To clean out the empty strings from the split name.
	
	for (int i = 0; i < uncleanParsed.length; i++) {
	    String unclean = uncleanParsed[i];
	    if ((!unclean.equals(""))&&(!unclean.equals(" "))) {
		parsed[i2] = unclean;
		i2++;
	    }
	}
	// another pass to see if there are any AAName + int in name.
	if (parsed[1].length() > 3) {
	    String parseValue = parsed[1].substring(3);
	    if (isNumeric(parseValue)) {
		//System.out.print(parseValue + " ");
		return Integer.parseInt(parseValue);
	    }
	}
	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (isNumeric(parseValue)) {
		if (Integer.parseInt(parseValue)>0) {
		    return Integer.parseInt(parseValue);
		}
	    }
	}

	return -1;
    }

    private int getResNumber(KList parentList) {
	Iterator pointIter = parentList.iterator();
	KPoint firstPoint = (KPoint) pointIter.next();
	return getResNumber(firstPoint);
    }

    public boolean isNumeric(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }


//}}}

//{{{ highlightRange
//###################################################################################################
    /**
     * For highlighting a range of a kinemage. The first time this function is called, it stores p
     * as the starting point, and the second time it's called, it colors the region between the stored p
     * and the current p.  
     *
     **/
    public void highlightRange(KPoint p) {
	if (!highNumField.getText().equals("")) {
	    lowNumField.setText("");
	    highNumField.setText("");
	}
	//KList parentList = (KList) p.getOwner();
	Integer resNum = new Integer(getResNumber(p));
	if(lowNumField.getText().equals("")) {
	    lowNumField.setText(resNum.toString());
	} else if (highNumField.getText().equals("")) {
	    highNumField.setText(resNum.toString());
	}
	if (!highNumField.getText().equals("")) {
	    //System.out.println("coloring");
	    int firstNum = Integer.parseInt(lowNumField.getText());
	    int secondNum = Integer.parseInt(highNumField.getText());
	    if (firstNum > secondNum) {
		int temp = secondNum;
		secondNum = firstNum;
		firstNum = temp;
	    }
	    int index = 0;
	    Object colors[] = KPalette.getStandardMap().values().toArray();
	    for (int i = firstNum; i <= secondNum; i++) {
		if (index > (colors.length - 6)) {
		    index = 0;
		}
		Integer hashKey = new Integer(i);
		//System.out.println(sortbyNum.size());
		if (structMap.containsKey(hashKey)) {
		    ArrayList listofLists = (ArrayList) structMap.get(hashKey);
		    Iterator iter = listofLists.iterator();
		    //int index = 0;
		    //Object colors[] = KPalette.getStandardMap().values().toArray();
		    while (iter.hasNext()) {
			//if (index > (colors.length - 6)) {
			//    index = 0;
		       	//}
			//System.out.print("coloring");
			KPoint point = (KPoint) iter.next();
			if (hippieMode.isSelected()) {
			    point.setColor((KPaint) colors[index]);
			    //index++;
			} else {
			    point.setColor((KPaint) color1.getSelectedItem());
			}
		    }
		    index++;
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
     * For coloring all of a KList.  
     **/
    
    public void highlightAll(KPoint p) {
	KList parentList = (KList) p.getOwner();
	Iterator iter = parentList.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    point.setColor((KPaint) color1.getSelectedItem());
	}

    }
    
//}}}

//{{{ highlight
//#######################################################################################################
    /**
     * For coloring one particular ribbon (one stretch of alpha, beta, or coil ribbon).
     **/
    /*
    public void highlight(KPoint p) {
	KList parentList = (KList) p.getOwner();
	ArrayList listofLists = (ArrayList) ribbonMap.get(parentList);
	Iterator iter = listofLists.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    list.setColor((KPaint) color1.getSelectedItem());
	}
    }
    */
//}}}    

//{{{ newGroup
//######################################################################################################
    /**
     * Resets the tool by creating new hashmaps for the various list storing maps. This is for coloring
     * ribbons in different KGroups, or in new kinemages.
     **/
    public void newGroup() {
	//System.out.println("new group");
	//sortedKin = new HashMap();
	//ribbonMap = new HashMap();
	//sortbyNum = new HashMap();
	//initiated = false;
	structMap = new HashMap();
    }
//}}}

//{{{ debug
//######################################################################################################
    /**
     * debugger function.
     **/
    public void debug() {
	Set keys = structMap.keySet();
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    String master = (String) iter.next();
	    ArrayList listofLists = (ArrayList) structMap.get(master);
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
    { return "#recolor-tool"; }

    public String toString() { return "Recolor Tool"; }
//}}}
}//class

