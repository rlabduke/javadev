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

    JRadioButton chooseColor, colorAll, colorAA;
    JRadioButton colorRegion, hippieMode, spectralMode;
    JButton colorButton;
    TablePane pane;
    JComboBox   color1;
    JTextField lowNumField;
    JTextField highNumField;
    Integer lowResNum, highResNum;

    String[] aaNames = {"gly", "ala", "ser", "thr", "cys", "val", "leu", "ile", "met", "pro", "phe", "tyr", "trp", "asp", "glu", "asn", "gln", "his", "lys", "arg"};
    JComboBox  aaBox;
    JCheckBox  colorPrior;

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

	aaBox = new JComboBox(aaNames);
	aaBox.setSelectedItem("pro");
	aaBox.addActionListener(this);

	//colorOne = new JRadioButton("Color One", false);
	colorRegion = new JRadioButton("Color Region", true);
	colorAll = new JRadioButton("Color All", false);
	colorAA = new JRadioButton("Color AAs", false);

	colorPrior = new JCheckBox("Color prior aa", false);
	colorPrior.setEnabled(false);
	//colorOne.addActionListener(this);
	colorRegion.addActionListener(this);
	colorAll.addActionListener(this);
	colorAA.addActionListener(this);
	ButtonGroup colorRange = new ButtonGroup();
	//colorRange.add(colorOne);
	colorRange.add(colorRegion);
	colorRange.add(colorAll);
	colorRange.add(colorAA);

	chooseColor = new JRadioButton("Choose Color", true);
	//checkSort = new JRadioButton("Check Sort", false);
	//colorRegion = new JRadioButton("Color Region", false);
	hippieMode = new JRadioButton("Hippie Mode", false);
	spectralMode = new JRadioButton("Spectral Mode", false);
	//chooseColor.addActionListener(this);
	//checkSort.addActionListener(this);
	hippieMode.addActionListener(this);
	ButtonGroup colorType = new ButtonGroup();
	colorType.add(chooseColor);
	//colorType.add(checkSort);
	//colorType.add(colorRegion);
	colorType.add(hippieMode);
	colorType.add(spectralMode);

	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);

	colorButton = new JButton("Color!");
	colorButton.setActionCommand("color");
	colorButton.addActionListener(this);

        pane = new TablePane();
	pane.newRow();
        pane.add(color1);
	pane.add(chooseColor);
	//pane.add(colorRegion);
	pane.add(hippieMode);
	pane.add(spectralMode);
	// removing checksort from panel so people can't accidentally use it.
	//pane.add(checkSort);
        //pane.newRow();

	pane.newRow();
	//pane.add(colorOne);
	pane.add(colorRegion);
	pane.add(colorAll);
	pane.add(colorAA);
	pane.add(aaBox);

	pane.newRow();
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(colorButton);
	pane.add(colorPrior);

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
	    } else if (colorAA.isSelected()) {
		highlightAA(p);
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
	if (colorAA.isSelected()) {
	    colorPrior.setEnabled(true);
	} else {
	    colorPrior.setEnabled(false);
	    colorPrior.setSelected(false);
	}
	if ("color".equals(ev.getActionCommand())) {
	    if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))) {
		int firstNum = Integer.parseInt(lowNumField.getText());
		int secondNum = Integer.parseInt(highNumField.getText());
		if (firstNum > secondNum) {
		    int temp = secondNum;
		    secondNum = firstNum;
		    firstNum = temp;
		}

		highlightRange(firstNum, secondNum);
	    } else {
		JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
	    }
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
	//Set keySet = structMap.keySet();
	TreeSet keys = new TreeSet(structMap.keySet());
	lowResNum = (Integer) keys.first();
	highResNum = (Integer) keys.last();
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

	    highlightRange(firstNum, secondNum);
	}
    }

    public void highlightRange(int firstNum, int secondNum) {
	int index = 0;
	//Object colors[] = KPalette.getStandardMap().values().toArray();
	Object colors[] = createColorArray(secondNum-firstNum+1);
	for (int i = firstNum; i <= secondNum; i++) {
	    //Object colors[] = createColorArray(secondNum-firstNum);
	    if (index >= colors.length) {
		index = 0;
	    }
	    Integer hashKey = new Integer(i);
	    
	    if (structMap.containsKey(hashKey)) {
		ArrayList listofLists = (ArrayList) structMap.get(hashKey);
		Iterator iter = listofLists.iterator();
		
		while (iter.hasNext()) {
		    
		    KPoint point = (KPoint) iter.next();
		    //if (hippieMode.isSelected()) {
		    //   point.setColor((KPaint) colors[index]);
		    //} else {
		    //    point.setColor((KPaint) color1.getSelectedItem());
		    //}
		    if (chooseColor.isSelected()) {
			point.setColor((KPaint) color1.getSelectedItem());
		    } else {
			point.setColor((KPaint) colors[index]);
		    }
		}
		index++;
	    }
	}
	//lowNumField.setText("");
	//highNumField.setText("");
    }

//}}}

    private Object[] createColorArray(int numRes) {
	if (hippieMode.isSelected()) {
	    Object[] allColors = KPalette.getStandardMap().values().toArray();
	    Object[] hippieColors = new Object[allColors.length - 6];
	    for (int i = 0; i < hippieColors.length; i++) {
		hippieColors[i] = allColors[i];
	    }
	    return hippieColors;
	} else {
	    Object[] colors = {KPalette.red, KPalette.orange, KPalette.gold, KPalette.yellow, KPalette.lime, KPalette.green, KPalette.sea, KPalette.cyan, KPalette.blue, KPalette.lilac, KPalette.purple};
	    Object[] spectralColors = new Object[numRes];
	    if (numRes > 11) {
		for (int i = 0; i < numRes; i++) {
		    int colorNum = (int) Math.round(i/((double)numRes/10));
		    //if (colorNum == 10) colorNum = 0;
		    //System.out.println((int) Math.round(i/((double)numRes/9)));
		    spectralColors[i] = colors[colorNum];
		}
		return spectralColors;
	    } else {
		return colors;
	    }
	}
    }
	

//{{{ highlightAll
//#######################################################################################################
    /**
     * For coloring all of a KList.  
     **/
    
    public void highlightAll(KPoint p) {
	KList parentList = (KList) p.getOwner();
	Iterator iter = parentList.iterator();
	highlightRange(lowResNum.intValue(), highResNum.intValue());
	//int index = 0;
	//Object colors[] = KPalette.getStandardMap().values().toArray();
	/*
	while (iter.hasNext()) {
	    //if (index > (colors.length - 6)) {
	    //	index = 0;
	    //}
	    KPoint point = (KPoint) iter.next();
	    //if (hippieMode.isSelected()) {
	    //	point.setColor((KPaint) colors[index]);
	    //	index++;
	    //} else {
		point.setColor((KPaint) color1.getSelectedItem());
		//}
	}
	*/

    }
    
//}}}

    public void highlightAA(KPoint p) {
	KList parentList = (KList) p.getOwner();
	Iterator iter = parentList.iterator();
	HashSet aaNums = new HashSet();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    String name = point.getName();
	    if (name.indexOf((String)aaBox.getSelectedItem()) != -1) {
		point.setColor((KPaint) color1.getSelectedItem());
		Integer resNum = new Integer(getResNumber(point));
		aaNums.add(resNum);
	    }
	}
	if (colorPrior.isSelected()) {
	    iter = aaNums.iterator();
	    while (iter.hasNext()) {
		int priorResNum = ((Integer) iter.next()).intValue() - 1;
		ArrayList listofPoints = (ArrayList) structMap.get(new Integer(priorResNum));
		if (listofPoints != null) {
		    Iterator listIter = listofPoints.iterator();
		    while (listIter.hasNext()) {
			KPoint point = (KPoint) listIter.next();
			if (point != null) {
			    point.setColor(KPalette.green);
			}
		    }
		}
	    }
	}
    }

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

