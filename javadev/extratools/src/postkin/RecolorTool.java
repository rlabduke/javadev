// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.text.*;
import driftwood.gui.*;
import driftwood.moldb2.AminoAcid;
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
    //HashMap structMap;
    //HashSet clickedLists;
    HashMap coloratorMap;//AGE as key, colorator as value
    Recolorator colorator;

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
    JCheckBoxMenuItem clickMode;

    JTextPane textPane = null;
    SimpleAttributeSet sas;

//}}}


//{{{ Constructor(s)
//##############################################################################
    public RecolorTool(ToolBox tb)
    {
        super(tb);
	//coloratorMap = new HashMap();
	//colorator = new RecolorNonRibbon();
	//newGroup();
	//clickedLists = new HashSet();

        //undoStack = new LinkedList();
	//buildGUI();
    }
//}}}


//{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {
        
        dialog = new JDialog(kMain.getTopWindow(),"Recolor", false);
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
	
	dialog.setContentPane(pane);

	JMenuBar menubar = new JMenuBar();
	JMenu menu;
	JMenuItem item;
	

	menu = new JMenu("Options");
	menubar.add(menu);
	item = new JMenuItem(new ReflectiveAction("Create Table", null, this, "onTable"));
	menu.add(item);
	clickMode = new JCheckBoxMenuItem("Click Mode On", true);
	menu.add(clickMode);

	dialog.setJMenuBar(menubar);

    }
//}}}

    public void start() {
	if (kMain.getKinemage() == null) return;
	buildGUI();
	coloratorMap = new HashMap();
	//colorator = new RecolorNonRibbon();
	show();
    }

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if ((p != null)&&(clickMode.getState())) {
	    //colorator = new RecolorNonRibbon();
	    AGE coloratorKey = (KList) p.getOwner();
	    //KList parentList = (KList) p.getOwner();
	    if (coloratorKey.hasMaster("ribbon")) {
		coloratorKey = (AGE) coloratorKey.getOwner();
	    }
	    if (coloratorMap.containsKey(coloratorKey)) {
		colorator = (Recolorator) coloratorMap.get(coloratorKey);
	    } else {
		if (coloratorKey instanceof KSubgroup) {
		    colorator = new RecolorRibbon();
		    coloratorMap.put(coloratorKey, colorator);
		} else {
		    colorator = new RecolorNonRibbon();
		    coloratorMap.put(coloratorKey, colorator);
		}
	    }
	    if (!colorator.contains(p)) {
		colorator.preChangeAnalyses(p);
	    }
	    Kinemage k = kMain.getKinemage();
	    if(k != null) k.setModified(true);
	    if (colorAll.isSelected()) {
		int numRes = colorator.numofResidues();
		//String tableText = textPane.getText();
		colorator.highlightAll(p, createColorArray(numRes));
		if (textPane != null) {
		    String tableText = textPane.getText();
		    recolorTable(0, tableText.length(), createColorArray(numRes));
		}
	    } else if (colorAA.isSelected()) {
		colorator.highlightAA(p, (String)aaBox.getSelectedItem(), (KPaint) color1.getSelectedItem(), colorPrior.isSelected());
		if (textPane != null) {
		    String aa = (String) aaBox.getSelectedItem();
		    aa = AminoAcid.translate(aa);
		    String tableText = textPane.getText();
		    //System.out.println(tableText);
		    //int i = 0;
		    int aaIndex = 0;
		    while (aaIndex >= 0) {
			aaIndex = tableText.indexOf(aa, aaIndex + 1);
			if (aaIndex >=0) {
			    //System.out.println(aaIndex);
			    //System.out.println(aaIndex + (int)Math.floor(aaIndex/5));
			    //i = aaIndex + 1;
			    recolorTable(aaIndex, 1, (KPaint) color1.getSelectedItem());
			}
		    }
		

		}
	    } else {
		numberHandler(p);
		if (!highNumField.getText().equals("")) {
		    int firstNum = Integer.parseInt(lowNumField.getText());
		    int secondNum = Integer.parseInt(highNumField.getText());
		    if (firstNum > secondNum) {
			int temp = secondNum;
			secondNum = firstNum;
			firstNum = temp;
		    }
		    KPaint[] colors = createColorArray(secondNum-firstNum+1);
		    colorator.highlightRange(firstNum, secondNum, colors);
		    if (textPane != null) {
			int correctedFirst = firstNum + (int)Math.floor(firstNum/5) - 1;
			int correctedSec = secondNum + (int)Math.floor(secondNum/5);
			//recolorTable(correctedFirst, correctedSec - correctedFirst, createColorArray(correctedSec-correctedFirst));
			recolorTable(correctedFirst, correctedSec - correctedFirst, colors);
		    }
		}
		
	    }
	   
	}
    }
//})}

    public void numberHandler(KPoint p) {
	if (!highNumField.getText().equals("")) {
	    lowNumField.setText("");
	    highNumField.setText("");
	}
	//KList parentList = (KList) p.getOwner();
	Integer resNum = new Integer(colorator.getResNumber(p));
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

	    //highlightRange(firstNum, secondNum);
	}
    }

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
	    if (colorator == null) {
		JOptionPane.showMessageDialog(pane, "Please click on a point in the structure you want to color before using the number boxes.", "Error",
					      JOptionPane.ERROR_MESSAGE);
	    } else {
		if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))&&(textPane == null)) {
		    int firstNum = Integer.parseInt(lowNumField.getText());
		    int secondNum = Integer.parseInt(highNumField.getText());
		    if (firstNum > secondNum) {
			int temp = secondNum;
			secondNum = firstNum;
			firstNum = temp;
		    }
		    //int numRes = ((RecolorNonRibbon)colorator).numofResidues();
		    colorator.highlightRange(firstNum, secondNum, createColorArray(secondNum-firstNum+1));
		} else if (textPane != null) {
		    if (textPane.getSelectionEnd()>0) {
			int firstNum = textPane.getSelectionStart();
			int adjFirstNum = firstNum - Math.round(firstNum/6) + 1;
			int secondNum = textPane.getSelectionEnd();
			int adjSecondNum = secondNum - Math.round(secondNum/6);
			//StyledDocument doc = textPane.getStyledDocument();
			//StyleConstants.setForeground(sas, (Color) ((KPaint)color1.getSelectedItem()).getWhiteExemplar());
			//doc.setCharacterAttributes(textPane.getSelectionStart(), textPane.getSelectionEnd()-textPane.getSelectionStart(),sas, true);
			//recolorTable(firstNum, secondNum-firstNum, (KPaint)color1.getSelectedItem());
			recolorTable(firstNum, secondNum-firstNum, createColorArray(secondNum-firstNum+1));
			colorator.highlightRange(adjFirstNum, adjSecondNum, createColorArray(secondNum-firstNum+1));
		    }
		} else {
		    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
						  JOptionPane.ERROR_MESSAGE);
		}
	    }
	}

        kCanvas.repaint();
    }

    public void recolorTable(int offset, int length, KPaint[] colors) {
	int index = 0;
	String tableText = textPane.getText();
	for (int i = offset; i < length + offset; i++) {
	    if (index >= colors.length) {
		index = 0;
	    }
	    //Integer hashKey = new Integer(i);
	    if ((tableText.charAt(i)!='-')&&(tableText.charAt(i)!=' ')) {
		recolorTable(i, 1, colors[index]);
		index++;
	    }
	    //index++;
	}
	    
    }

    public void recolorTable(int offset, int length, KPaint color) {
	StyledDocument doc = textPane.getStyledDocument();
	StyleConstants.setForeground(sas, (Color) color.getWhiteExemplar());
	doc.setCharacterAttributes(offset, length, sas, true);

    }
	

    /**
     * Event handler for when tool window closed.
     */
    public void windowClosing(WindowEvent ev) {
	//newGroup();
	parent.activateDefaultTool(); 
    }

    public void onTable(ActionEvent ev) {
	textPane = new JTextPane();
	Font monospaced = new Font("monospaced", Font.PLAIN, 12);
	StyledDocument doc = textPane.getStyledDocument();
	JScrollPane scrollPane = new JScrollPane(textPane);
	scrollPane.setPreferredSize(new Dimension(510, 200));
	pane.newRow();
	pane.add(scrollPane, 4, 1);
	dialog.pack();
	sas = new SimpleAttributeSet();
	StyleConstants.setFontFamily(sas, "monospaced");
	StyleConstants.setFontSize(sas, 14);
	try {
	    doc.insertString(doc.getLength(), colorator.onTable(), sas);
	} catch (BadLocationException ble) {
	}
    }

    public boolean isNumeric(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    private KPaint[] createColorArray(int numRes) {
	if (chooseColor.isSelected()) {
	    KPaint[] chosenColor = new KPaint[1];
	    chosenColor[0] = (KPaint) color1.getSelectedItem();
	    return chosenColor;
	} else if (hippieMode.isSelected()) {
	    Object[] allColors = KPalette.getStandardMap().values().toArray();
	    KPaint[] hippieColors = new KPaint[allColors.length - 6];
	    for (int i = 0; i < hippieColors.length; i++) {
		hippieColors[i] = (KPaint) allColors[i];
	    }
	    return hippieColors;
	} else {
	    // KPaint[] colors = {KPalette.red, KPalette.orange, KPalette.gold, KPalette.yellow, KPalette.lime, KPalette.green, KPalette.sea, KPalette.cyan, KPalette.blue, KPalette.lilac, KPalette.purple};
	    //KPaint[] colors = {KPalette.purple, KPalette.lilac, KPalette.blue, KPalette.cyan, KPalette.sea, KPalette.green, KPalette.lime, KPalette.yellow, KPalette.gold, KPalette.orange, KPalette.red};
	    KPaint[] colors = {KPalette.blue, KPalette.sky, KPalette.cyan, KPalette.sea, KPalette.green, KPalette.lime, KPalette.yellow, KPalette.gold, KPalette.orange, KPalette.red};
	    KPaint[] spectralColors = new KPaint[numRes];
	    double meanValue = (double)numRes/10;
	    int floor = (int) Math.floor(meanValue);
	    int colorIndex = 0;
	    if (numRes > 10) {
		for (int i = 0; i < numRes; i++) {
		    if (i > floor) {
			colorIndex++;
			floor = (int) Math.floor(((colorIndex + 1) * meanValue));
		    }
		    spectralColors[i] = colors[colorIndex];		    
		    //int colorNum = (int) Math.round(i/((double)numRes/9));
		    //if (colorNum == 10) colorNum = 0;
		    //System.out.println((int) Math.round(i/((double)numRes/9)));
		    //spectralColors[i] = colors[colorNum];
		}
		return spectralColors;
	    } else {
		return colors;
	    }
	}
    }


//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return dialog; }

    public String getHelpAnchor()
    { return "#recolor-tool"; }

    public String toString() { return "Recolor Kins"; }
//}}}
}//class
