// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;
import king.*;
import king.core.*;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import driftwood.gui.*;

//}}}
/**
 * AutoBondRotDataTool is a tool that reads in the output files of a probe
 * autobondrot run.  It plots out the data as balls with radii dependent on
 * the probe score.  It also allows the balls to be recolored according to 
 * their score.  The tool should be able to handle 2D and 3D data.  
 **/
public class AutoBondRotDataTool extends BasicTool implements ActionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    JFileChooser        filechooser     = null;
    JDialog             urlchooser      = null;
    JList               urlList         = null;
    JTextField          urlField        = null;
    boolean             urlChooserOK    = false;

    HashMap             dataMap; // probescore -> arraylist of BallPoints
    TablePane           pane;
    JComboBox           color1;
    JTextField          lowNumField;
    JTextField          highNumField;
    JButton             colorButton, setDefaultButton, setOnButton, setOffButton;
    JCheckBox           plotChangeBox;

    //double maxX, maxY, maxZ = -100000;
    //double minX, minY, minZ = 100000;
    //double xspan, yspan, zspan, xInc, yInc, zInc;
    HashMap             listMap; // axisValue -> KList
    HashSet             offPoints;
    double minSpan = 100000000;
    double minSpanIncr, minSpanLow, minSpanHigh;
    ArrayList xKlists, yKlists, zKlists;

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public AutoBondRotDataTool(ToolBox tb)
    {
        super(tb);
        
	buildGUI();
        //makeFileFilters();
	
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI() {

	color1 = new JComboBox(KPalette.getStandardMap().values().toArray());
	color1.setSelectedItem(KPalette.red);
        color1.addActionListener(this);

	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);
	
	colorButton = new JButton("Color!");
	colorButton.setActionCommand("color");
	colorButton.addActionListener(this);
	
	setDefaultButton = new JButton("Restore Default");
	setDefaultButton.setActionCommand("set defaults");
	setDefaultButton.addActionListener(this);

	setOnButton = new JButton("Turn On");
	setOnButton.setActionCommand("setOn");
	setOnButton.addActionListener(this);

	setOffButton = new JButton("Turn Off");
	setOffButton.setActionCommand("setOff");
	setOffButton.addActionListener(this);

	plotChangeBox = new JCheckBox("Toggle plotting", true);
	plotChangeBox.addActionListener(this);

	pane = new TablePane();
	pane.newRow();
	pane.add(color1);
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(colorButton);
	pane.newRow();
	pane.add(setDefaultButton);
	pane.add(setOnButton);
	pane.add(setOffButton);
	pane.add(plotChangeBox);
    }

//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
	
        // Make accessory for file chooser
        TablePane acc = new TablePane();

        // Make actual file chooser -- will throw an exception if we're running as an Applet
        filechooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        
        filechooser.setAccessory(acc);
        //filechooser.addPropertyChangeListener(this);
        //filechooser.addChoosableFileFilter(fastaFilter);
        //filechooser.setFileFilter(fastaFilter);
    }
//}}}

//{{{ start
//##################################################################################################
    public void start()
    {
        if(kMain.getKinemage() == null) return;

        try
        {
            //if(kMain.getApplet() != null)   openMapURL();
            //else                            openMapFile();
	    dataMap = new HashMap();
	    listMap = new HashMap();
	    offPoints = new HashSet();
	    openFile();
	    show();
        }
        catch(IOException ex) // includes MalformedURLException
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
        }
        catch(IllegalArgumentException ex)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
        }

	//show();
	//buildGUI();
	//dialog.pack();
	//dialog.setLocationRelativeTo(kMain.getTopWindow());
        //dialog.setVisible(true);
    }
//}}}


//{{{ openMapFile
//##################################################################################################
    void openFile() throws IOException
    {
        // Create file chooser on demand
        if(filechooser == null) makeFileChooser();
        
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
        {
            File f = filechooser.getSelectedFile();
            if(f != null && f.exists())
            {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		scanFile(reader);
		//buildGUI();
		//show();
		//dialog.pack();
		//dialog.setLocationRelativeTo(kMain.getTopWindow());
		//dialog.setVisible(true);
                kCanvas.repaint(); // otherwise we get partial-redraw artifacts
            }
        }
    }
//}}}

//{{{ scanFile
//##################################################################################################
    /**
     * Does most of the work reading and analyzing the data files.
     **/
    private void scanFile(BufferedReader reader) {
	String line;
	try {
	    while((line = reader.readLine())!=null){
		
		if (line.charAt(0)=='#') {
		    System.out.println(line);
		    if (line.indexOf("#ROT")>-1) analyzeRotLine(line);
		} else {
		    line.trim();
		    StringTokenizer spaceToks = new StringTokenizer(line, " ");
		    //double x = Double.parseDouble(spaceToks.nextToken());
		    //double y = Double.parseDouble(spaceToks.nextToken());
		    double clashValue = Double.parseDouble(spaceToks.nextToken());
		    //float clashFloat = clashValue.floatValue();
		    double x = Double.parseDouble(spaceToks.nextToken());
		    double y = Double.parseDouble(spaceToks.nextToken());
		    double z = Double.NaN;
		    if (spaceToks.hasMoreTokens()) {
			// Temporarily switching y and z stupidly.
			z = y;
			y = Double.parseDouble(spaceToks.nextToken());
		    }
		    //trackHighLows(x, y, z); // for determining what planes to split into different lists.
		    BallPoint point = new BallPoint(null, Double.toString(clashValue));
		    if (clashValue>0) {
			point.setRadius((float)clashValue);
			//point.setColor(KPalette.green);
		    } else {
			point.setRadius(-(float)clashValue/10);
			//point.setRadius(1);
		    }
		    point.setOrigX(x);
		    point.setOrigY(y);
		    //point.setOrigZ(clashValue*100);
		    if (!Double.isNaN(z)) {
			sortByValue(z, point);
		    } else {
			sortByValue(y, point);
		    }

		    // for recoloring by clash value
		    Integer clashInt = new Integer((int)Math.floor(clashValue));
		    if (dataMap.containsKey(clashInt)) {
			ArrayList clashPoints = (ArrayList) dataMap.get(clashInt);
			clashPoints.add(point);
		    } else {
			ArrayList clashPoints = new ArrayList();
			clashPoints.add(point);
			dataMap.put(clashInt, clashPoints);
		    }
		    //reSortKlists();

		}
	    }
	    plotByScore(true);
	    setDefaultColors();
	    reSortKlists();
	    //kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
        }
    }
//}}}

    /*
    private void trackHighLows(int x, int y, int z) {
	if (x > maxX) maxX = x;
	if (x < minX) minX = x;
	if (x > maxY) maxY = y;
	if (x < minY) minY = y;	
	if (x > maxZ) maxZ = z;
	if (x < minZ) minZ = z;	
	}*/

    // analyzes the comment lines in the files; function is not used at the moment.
    private void analyzeRotLine(String line) {
	System.out.println(line.substring(line.indexOf("from ")+5, line.indexOf(" to "))+ ".");
	System.out.println(line.substring(line.indexOf("to ") + 3, line.indexOf(" by "))+ ".");
	System.out.println(line.substring(line.indexOf("by ") + 3) + ".");

	double from = Double.parseDouble(line.substring(line.indexOf("from ")+5, line.indexOf(" to ")));
	double toValue = Double.parseDouble(line.substring(line.indexOf("to ") + 3, line.indexOf(" by ")));
	double increment = Double.parseDouble(line.substring(line.indexOf("by ") + 3));
	if (toValue - from < minSpan) {
	    minSpanLow = from;
	    minSpanHigh = toValue;
	    minSpan = toValue - from;
	    minSpanIncr = increment;
	}
	
	
    }

    //need to set owners of the Klists.
    private void sortByValue(double value, KPoint point) {
	if (listMap.containsKey(new Double(value))) {
	    KList list = (KList) listMap.get(new Double(value));
	    list.add(point);
	    point.setOwner(list);
	} else {
	    KList list = new KList();
	    list.setType(KList.BALL);
	    point.setOwner(list);
	    listMap.put(new Double(value), list);
	}

    }


//{{{ reSortKlists
//###################################################################################################
    /**
     * Sorts keys and adds KLists to the kinemage, each in separate groups and animatable.
     **/
    private void reSortKlists() {
	Kinemage kin = kMain.getKinemage();
	//KGroup group = new KGroup(kin, "Data Points");
	//kin.add(group);
	kin.setModified(true);
	kin.getMasterByName("Data Points");
	
	Set origKeys = listMap.keySet();
	TreeSet keys = new TreeSet(origKeys);
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    Double key = (Double) iter.next();
	    KGroup group = new KGroup(kin, key.toString());
	    group.setAnimate(true);
	    group.addMaster("Data Points");
	    kin.add(group);
	    KSubgroup subgroup = new KSubgroup(group, key.toString());
	    subgroup.setHasButton(false);
	    group.add(subgroup);
	
	    //KList list = new KList(subgroup, "Points");
	    
	    KList list = (KList) listMap.get(key);
	    list.flags |= KList.NOHILITE;
	    //list.setType("BALL");
	    list.setHasButton(false);
	    subgroup.add(list);
	    list.setOwner(subgroup);
	}
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }

//{{{ setDefaultColors
//################################################################################################
    public void setDefaultColors() {
	highlightRange(0, 180, KPalette.green);
	highlightRange(-35, -100, KPalette.red);
	highlightRange(-25, -35, KPalette.orange);
	highlightRange(-15, -25, KPalette.peach);
	highlightRange(-5, -15, KPalette.gold);
	highlightRange(0, -5, KPalette.yellow);
    }

//{{{ highlightRange
//################################################################################################
    /**
     * Colors all points with value between firstNum and secondNum
     */
    public void highlightRange(int firstNum, int secondNum, KPaint color) {
	if (firstNum > secondNum) {
	    int temp = secondNum;
	    secondNum = firstNum;
	    firstNum = temp;
	}
	for (int i = firstNum; i < secondNum; i++) {
	    if (dataMap.containsKey(new Integer(i))) {
		//System.out.println("coloring " + i);
		ArrayList clashPoints = (ArrayList) dataMap.get(new Integer(i));
		Iterator iter = clashPoints.iterator();
		while (iter.hasNext()) {
		    BallPoint point = (BallPoint) iter.next();
		    //point.setColor((KPaint) color1.getSelectedItem());
		    point.setColor(color);
		}
	    }
	}
    }

//{{{ togglePointStatus
//###############################################################################################
    /**
     * turns on/off all points with value between firstNum and secondNum
     **/
    public void togglePointStatus(int firstNum, int secondNum, boolean status) {
	if (firstNum > secondNum) {
	    int temp = secondNum;
	    secondNum = firstNum;
	    firstNum = temp;
	}
	for (int i = firstNum; i < secondNum; i++) {
	    if (dataMap.containsKey(new Integer(i))) {
		//System.out.println("coloring " + i);
		ArrayList clashPoints = (ArrayList) dataMap.get(new Integer(i));
		Iterator iter = clashPoints.iterator();
		while (iter.hasNext()) {
		    BallPoint point = (BallPoint) iter.next();
		    //point.setColor((KPaint) color1.getSelectedItem());
		    //point.setColor(color);
		    point.setOn(status);
		}
	    }
	}
    }

    public void removePoints(int firstNum, int secondNum) {
	if (firstNum > secondNum) {
	    int temp = secondNum;
	    secondNum = firstNum;
	    firstNum = temp;
	}
	Collection lists = listMap.values();
	Iterator iter = lists.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    list.clear();
	    //onPoints.clear();
	}
	Set keys = dataMap.keySet();
	iter = keys.iterator();
	while (iter.hasNext()) {
	    int key = ((Integer) iter.next()).intValue();
	    if (dataMap.containsKey(new Integer(key))) {
		ArrayList points = (ArrayList) dataMap.get(new Integer(key));
		Iterator iter2 = points.iterator();
		while (iter2.hasNext()) {
		    BallPoint point = (BallPoint) iter2.next();
		    if ((key<firstNum)||(key>secondNum)) {
			if (!offPoints.contains(point)) {
			    KList owner = (KList) point.getOwner();
			    owner.add(point);
			}
		    } else {
			offPoints.add(point);
		    }
		}
	    }
	}
    }

    public void addPoints(int firstNum, int secondNum) {
	if (firstNum > secondNum) {
	    int temp = secondNum;
	    secondNum = firstNum;
	    firstNum = temp;
	} 
	for (int i = firstNum; i <= secondNum; i++) {
	    if (dataMap.containsKey(new Integer(i))) {
		//System.out.println("coloring " + i);
		ArrayList clashPoints = (ArrayList) dataMap.get(new Integer(i));
		Iterator iter = clashPoints.iterator();
		while (iter.hasNext()) {
		    BallPoint point = (BallPoint) iter.next();
		    //point.setColor((KPaint) color1.getSelectedItem());
		    //point.setColor(color);
		    if (offPoints.contains(point)) {
			KList owner = (KList) point.getOwner();
			owner.add(point);
			offPoints.remove(point);
		    }
		}
	    }
	}
    }

    private void plotByScore(boolean plotStat) {
	if (plotStat) {
	    Collection values = dataMap.values();
	    Iterator iter = values.iterator();
	    while (iter.hasNext()) {
		ArrayList value = (ArrayList) iter.next();
		Iterator points = value.iterator();
		while (points.hasNext()) {
		    KPoint point = (KPoint) points.next();
		    point.setOrigZ(Double.parseDouble(point.getName())*5);
		}
	    }
	} else {
	    Set keys = listMap.keySet();
	    Iterator iter = keys.iterator();
	    while (iter.hasNext()) {
		Double key = (Double) iter.next();
		KList list = (KList) listMap.get(key);
		Iterator points = list.iterator();
		while (points.hasNext()) {
		    KPoint point = (KPoint) points.next();
		    point.setOrigZ(key.doubleValue());
		}
	    }
	}
    }

// event functions
//###############################################################################################
    /**
     * Event handler for when action performed.
     */
    public void actionPerformed(ActionEvent ev) {
	if ("color".equals(ev.getActionCommand())) {
	    if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))) {
		int firstNum = Integer.parseInt(lowNumField.getText());
		int secondNum = Integer.parseInt(highNumField.getText());
		//if (firstNum > secondNum) {
		//    int temp = secondNum;
		//    secondNum = firstNum;
		//    firstNum = temp;
		//}

		highlightRange(firstNum, secondNum, (KPaint) color1.getSelectedItem());
	    } else {
		JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
	    }
	} else if ("set defaults".equals(ev.getActionCommand())) {
	    setDefaultColors();
	} else if ("setOn".equals(ev.getActionCommand())) {
	    if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))) {
		int firstNum = Integer.parseInt(lowNumField.getText());
		int secondNum = Integer.parseInt(highNumField.getText());
		//togglePointStatus(firstNum, secondNum, true);
		addPoints(firstNum, secondNum);
	    } else {
		JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
	    }
	} else if ("setOff".equals(ev.getActionCommand())) {
	    if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))) {
		int firstNum = Integer.parseInt(lowNumField.getText());
		int secondNum = Integer.parseInt(highNumField.getText());
		//togglePointStatus(firstNum, secondNum, false);
		removePoints(firstNum, secondNum);
	    } else {
		JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
					      JOptionPane.ERROR_MESSAGE);
	    }
	}
	if (plotChangeBox.isSelected()) {
	    plotByScore(true);
	    //System.out.println("selected");
	} else {
	    plotByScore(false);
	    //System.out.println("not selected");
	}
	kCanvas.repaint();
    }

    public boolean isNumeric(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#draw-tool"; }

    public Container getToolPanel()
    { return pane; }
    
    public String toString()
    { return "AutoBondRot Data"; }
//}}}
}
