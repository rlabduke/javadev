// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import driftwood.gui.*;
import java.text.DecimalFormat;

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
    static final DecimalFormat df = new DecimalFormat("0.000");
    static final DecimalFormat intf = new DecimalFormat("0");
//}}}

//{{{ Variable definitions
//##################################################################################################
    JFileChooser        filechooser     = null;
    JDialog             urlchooser      = null;
    JList               urlList         = null;
    JTextField          urlField        = null;
    boolean             urlChooserOK    = false;

    //HashMap             dataMap; // probescore -> arraylist of BallPoints
    TablePane           pane;
    //JDialog             dialog;
    JComboBox           color1;
    JTextField          lowNumField;
    JTextField          highNumField;
    JTextField          scalingField;
    JCheckBoxMenuItem   logScaleBox;
    JButton             colorButton, setOnButton, setOffButton;
    JCheckBox           plotChangeBox;

    //double maxX, maxY, maxZ = -100000;
    //double minX, minY, minZ = 100000;
    //double xspan, yspan, zspan, xInc, yInc, zInc;
    HashMap             listMap; // axisValue -> KList
    //HashSet             offPoints;
    ArrayList offPoints;
    double minSpan = 100000000;
    double minSpanIncr, minSpanLow, minSpanHigh;
    ArrayList xKlists, yKlists, zKlists;
    ArrayList allPoints;

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public AutoBondRotDataTool(ToolBox tb)
    {
        super(tb);
        
	//buildGUI();
        //makeFileFilters();
	
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI() {

	dialog = new JDialog(kMain.getTopWindow(), "Data Tool", false);

	color1 = new JComboBox(KPalette.getStandardMap().values().toArray());
	color1.setSelectedItem(KPalette.red);
        color1.addActionListener(this);

	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);
	scalingField = new JTextField("5", 5);
	
	logScaleBox = new JCheckBoxMenuItem("Plot by log scale", false);

	colorButton = new JButton(new ReflectiveAction("Color!", null, this, "onHighlightRange"));
	//colorButton.setActionCommand("color");
	//colorButton.addActionListener(this);

	setOnButton = new JButton(new ReflectiveAction("Turn On", null, this, "onTurnOn"));
	//setOnButton.setActionCommand("setOn");
	//setOnButton.addActionListener(this);

	setOffButton = new JButton(new ReflectiveAction("Turn Off", null, this, "onTurnOff"));
	//setOffButton.setActionCommand("setOff");
	//setOffButton.addActionListener(this);

	plotChangeBox = new JCheckBox("Toggle plotting", true);
	plotChangeBox.addActionListener(this);

	pane = new TablePane();
	pane.newRow();
	pane.add(color1);
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(colorButton);
	//pane.add(setDefaultButton);
	pane.newRow();
	pane.add(setOnButton);
	pane.add(setOffButton);
	pane.add(plotChangeBox);
	pane.add(scalingField);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(this);
	dialog.setContentPane(pane);
	
	JMenuBar menubar = new JMenuBar();
	JMenu menu;
	JMenuItem item;


	menu = new JMenu("Options");
	menubar.add(menu);
	item = new JMenuItem(new ReflectiveAction("Open File", null, this, "onOpenFile"));
	menu.add(item);
	menu.add(logScaleBox);
	item = new JMenuItem(new ReflectiveAction("Smooth Data", null, this, "onSmoothing"));
	menu.add(item);
	item = new JMenuItem(new ReflectiveAction("Take Difference", null, this, "onDifference"));
	menu.add(item);
	item = new JMenuItem(new ReflectiveAction("Export", null, this, "onExport"));
	menu.add(item);
	item = new JMenuItem(new ReflectiveAction("Restore Default", null, this, "onSetDefault"));
	menu.add(item);
	item = new JMenuItem(new ReflectiveAction("Score in Last Col", null, this, "onFixScore"));
	menu.add(item);	
	menu.insertSeparator(4);

	dialog.setJMenuBar(menubar);
	
    }
//}}}

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

        //try
        //{
	buildGUI();
            //if(kMain.getApplet() != null)   openMapURL();
            //else                            openMapFile();
	    //dataMap = new HashMap();
	listMap = new HashMap();
	offPoints = new ArrayList();
	allPoints = new ArrayList();
	//openFile();
	    
	
	    
	show();
	    
    }
//}}}

//{{{ askFormats
    private String askFileFormat(String f) {
	Object[] choices = {"First", "Last"};
	String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(), 
		  "Is the data in the first or last column?", 
							     "Choose", JOptionPane.PLAIN_MESSAGE, 
							     null, choices, "First");
	return choice;
    }
    
    private String askAngleFormat(String f) {
	Object[] choices = {"phi, psi, angle", "phi, angle, psi", "angle, phi, psi"};
	String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(),
							     "How is the data formatted?",
							     "Choose", JOptionPane.PLAIN_MESSAGE,
							     null, choices, "phi, angle, psi");
	return choice;
    } //}}}

//{{{ openOpenFile
//##################################################################################################
    public void onOpenFile(ActionEvent ev)
    {
        // Create file chooser on demand
        if(filechooser == null) makeFileChooser();
        
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
	{
	    try {
		File f = filechooser.getSelectedFile();
		if(f != null && f.exists()) {
		    dialog.setTitle(f.getName());
		    BufferedReader reader = new BufferedReader(new FileReader(f));
		    String fileChoice = askFileFormat(f.getName());
		    //System.out.println(fileChoice);
		    String angleChoice = "none";
		    if (fileChoice.equals("First")) {
			angleChoice = askAngleFormat(f.getName());
		    }
		    if ((fileChoice != null)&&(angleChoice != null)) {
			listMap = new HashMap();
			offPoints = new ArrayList();
			allPoints = new ArrayList();
			scanFile(reader, fileChoice, angleChoice);
		    }

		    kCanvas.repaint(); // otherwise we get partial-redraw artifacts
		}
	    } 
	    
	    catch(IOException ex) { // includes MalformedURLException 
		JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
	    } catch(IllegalArgumentException ex) {
		JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
	    }
	}
    }
    
//}}}

//{{{ scanFile
//##################################################################################################
    /**
     * Does most of the work reading and analyzing the data files.
     **/
    private void scanFile(BufferedReader reader, String fileChoice, String angleChoice) {
	String line;
	try {
	    while((line = reader.readLine())!=null){
		
		if (line.charAt(0)=='#') {
		    System.out.println(line);
		    //if (line.indexOf("#ROT")>-1) analyzeRotLine(line);
		} else {
		    line = line.trim();
		    //System.out.println(line);
		    /*
		    StringTokenizer spaceToks = new StringTokenizer(line, " ");
		    int numToks = spaceToks.countTokens();
		    double x, y, clashValue;
		    double temp1 = 1000, temp2 = 1000, temp3 = 1000;
		    double temp4 = Double.NaN;
		    double z = Double.NaN;
		    if (numToks > 2) {
			temp1 = Double.parseDouble(spaceToks.nextToken());
			temp2 = Double.parseDouble(spaceToks.nextToken());
			temp3 = Double.parseDouble(spaceToks.nextToken());
			if (spaceToks.hasMoreTokens()) {
			    temp4 = Double.parseDouble(spaceToks.nextToken());
			}
			}*/
		    //System.out.println(line);
		    String[] values = line.split("\\s");
		    //System.out.print(values.length + " ");
		    //if (values.length>2) {
		    //System.out.println(values[0]);
		    //}
		    int numValues = values.length;
		    double x, y, clashValue;
		    double temp1 = 1000, temp2 = 1000, temp3 = 1000;
		    double temp4 = Double.NaN;
		    double z = Double.NaN;
		    if (numValues > 2) {
			temp1 = Double.parseDouble(values[0]);
			temp2 = Double.parseDouble(values[1]);
			temp3 = Double.parseDouble(values[2]);
		    }
		    if (numValues > 3) {
			temp4 = Double.parseDouble(values[3]);
		    }

		    if (fileChoice.equals("Last")) {
			x = temp1;
			y = temp2;
			if (!Double.isNaN(temp4)) {
			    clashValue = temp4;
			    z = temp3;
			} else {
			    clashValue = temp3;
			}
		    } else {
			clashValue = temp1;
			if (Double.isNaN(temp4)) {
			    x = temp2;
			    y = temp3;
			} else {
			    if (angleChoice.equals("phi, psi, angle")) {
				x = temp2;
				y = temp3;
				z = temp4;
			    } else if (angleChoice.equals("phi, angle, psi")) {
				x = temp2;
				z = temp3;
				y = temp4;
			    } else {
				z = temp2;
				x = temp3;
				y = temp4;
			    }
			}
		    }
		    BallPoint point = new BallPoint(null, Double.toString(clashValue));
		    allPoints.add(point);

		    if (clashValue>0) {
			point.setRadius((float)clashValue);
			//point.setColor(KPalette.green);
		    } else {
			point.setRadius(-(float)clashValue/10);
			//point.setRadius(1);
		    }
		    point.setX(x);
		    point.setY(y);
		    //point.setZ(clashValue*100);
		    if (!Double.isNaN(z)) {
			sortByValue(z, point);
		    } else {
			sortByValue(0, point);
		    }
		    //System.out.println(allPoints.size());
		}
	    }
	    //System.out.println(allPoints.size());
	    plotByScore(true, 5);
	    //System.out.println(allPoints.size());
	    setDefaultColors();
	    reSortKlists();
	    //System.out.println(allPoints.size());
	    //kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
        }
    }
//}}}

//{{{ analyzeRotLine
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
//}}}

//{{{ sortByValue
//need to set owners of the Klists.
    private void sortByValue(double value, KPoint point) {
	if (listMap.containsKey(new Double(value))) {
	    KList list = (KList) listMap.get(new Double(value));
	    list.add(point);
	    point.setParent(list);
	} else {
	    KList list = new KList(KList.BALL);
	    point.setParent(list);
	    list.add(point);
	    list.setName(Double.toString(value));
	    listMap.put(new Double(value), list);
	}

    }
//}}}

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
	    list.setParent(subgroup);
	}
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }
//}}}

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
//}}}
    
//{{{ highlightRange
//################################################################################################
    /**
     * Colors all points with value between firstNum and secondNum
     */
    public void highlightRange(double firstNum, double secondNum, KPaint color) {
	if (firstNum > secondNum) {
	    double temp = secondNum;
	    secondNum = firstNum;
	    firstNum = temp;
	}
	Iterator iter = allPoints.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    double clashValue = Double.parseDouble(point.getName());
	    if ((clashValue<= secondNum)&&(clashValue>= firstNum)) {
		point.setColor(color);
	    }
	}
	kCanvas.repaint();

    }
//}}}    

//{{{ onSetDefault

    public void onSetDefault(ActionEvent ev) {
	setDefaultColors();
    }
//}}}
    
//{{{ onTurnOff
    public void onTurnOff(ActionEvent ev) {
	if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))) {
	    double firstNum = Double.parseDouble(lowNumField.getText());
	    double secondNum = Double.parseDouble(highNumField.getText());
	    if (firstNum > secondNum) {
		double temp = secondNum;
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
	    // for fixing a bug where points that are .equals() are in different lists.
	    HashSet offPointSet = new HashSet(offPoints); 
	    iter = allPoints.iterator();
	    int i = 0;
	    while (iter.hasNext()) {
		KPoint point = (KPoint) iter.next();
		double clashValue = Double.parseDouble(point.getName());
		if ((clashValue<firstNum)||(clashValue>secondNum)) {
		    if (!offPointSet.contains(point)) {
			KList owner = (KList) point.getParent();
			owner.add(point);
		    }
		} else {
		    offPoints.add(point);
		    offPointSet.add(point);
		    i++;
		}
	    }
	    //System.out.println(offPoints.size()+", " + i);
	} else {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(), "You have to put numbers in the text boxes!", "Error",
					  JOptionPane.ERROR_MESSAGE);
	}
	kCanvas.repaint();

    }
//}}}
    
//{{{ onTurnOn
    public void onTurnOn(ActionEvent ev) {
	if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))) {
	    double firstNum = Double.parseDouble(lowNumField.getText());
	    double secondNum = Double.parseDouble(highNumField.getText());

	    if (firstNum > secondNum) {
		double temp = secondNum;
		secondNum = firstNum;
		firstNum = temp;
	    } 

	    for (int i = offPoints.size() - 1; i > -1; i--) {
		KPoint point = (KPoint) offPoints.get(i);
		double clashValue = Double.parseDouble(point.getName());
		if ((clashValue>=firstNum)&&(clashValue<=secondNum)) {
		    KList owner = (KList) point.getParent();
		    owner.add(point);
		    offPoints.remove(i);
		}
	    }
	    //System.out.println(offPoints.size() + ", " + i);
	} else {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(), "You have to put numbers in the text boxes!", "Error",
					  JOptionPane.ERROR_MESSAGE);
	}
	kCanvas.repaint();
	
    }
//}}}

//{{{ onDifference
    public void onDifference(ActionEvent ev) {
	/*
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	allPoints.clear();
	long startTime = System.currentTimeMillis();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		if (group.hasMaster("Data Points")) {
		    KSubgroup subgroup = (KSubgroup) group.getChildAt(0);
		    
		    KList list = (KList) subgroup.getChildAt(0);
		    //if (list.isOn()) {
		    Iterator points = list.iterator();
		    while (points.hasNext()) {
			KPoint point = (KPoint) points.next();
			allPoints.add(point);
		    }
		    //}
		}
	    }
	}

	PointSorter ps = new PointSorter(allPoints, PointSorter.SORTBY_Y);
	allPoints = (ArrayList) ps.sortPhiPsi();
        ps = new PointSorter(allPoints, PointSorter.SORTBY_X);
	allPoints = (ArrayList) ps.sortPhiPsi();
	*/
	long startTime = System.currentTimeMillis();
	addAllDataPoints();
	for (int i = 0; i < allPoints.size(); i++) {
	    KPoint point = (KPoint) allPoints.get(i);
	    if ((i+1) < allPoints.size()) {
		KPoint nextPoint = (KPoint) allPoints.get(i+1);
		double score = Double.parseDouble(point.getName());
		double npScore = Double.parseDouble(nextPoint.getName());
		if ((point.getX()==nextPoint.getX())&&(point.getY()==nextPoint.getY())&&(score!=npScore)) {
		    //double z = point.getZ();
		    //double npz = nextPoint.getZ();
		    point.setZ((score-npScore)*5);
		    nextPoint.setZ((score-npScore)*5);
		    point.setRadius((float)1);
		    point.setRadius((float)1);
		}
	    }
	}
	long endTime = System.currentTimeMillis();
	
	System.out.println("Total Time to diff: " + ((endTime-startTime)/1000) + " seconds");
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	
    }
//}}}

//{{{ addAllDataPoints

    private void addAllDataPoints() {
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	allPoints.clear();
	//long startTime = System.currentTimeMillis();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		if (group.hasMaster("Data Points")) {
		    KSubgroup subgroup = (KSubgroup) group.getChildAt(0);
		    
		    KList list = (KList) subgroup.getChildAt(0);
		    //if (list.isOn()) {
		    Iterator points = list.iterator();
		    while (points.hasNext()) {
			KPoint point = (KPoint) points.next();
			allPoints.add(point);
		    }
		    //}
		}
	    }
	}

	PointSorter ps = new PointSorter(allPoints, PointSorter.SORTBY_Y);
	allPoints = (ArrayList) ps.sortPhiPsi();
        ps = new PointSorter(allPoints, PointSorter.SORTBY_X);
	allPoints = (ArrayList) ps.sortPhiPsi();

    }
//}}}

//{{{ onExport

    public void onExport(ActionEvent ev) {
	//addAllDataPoints();
	JFileChooser saveChooser = new JFileChooser();
	String currdir = System.getProperty("user.dir");
	if(currdir != null) {
	    saveChooser.setCurrentDirectory(new File(currdir));
	}
	if (saveChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow())) {
	    File f = saveChooser.getSelectedFile();
	    if( !f.exists() ||
                JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                    "This file exists -- do you want to overwrite it?",
                    "Overwrite file?", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION )
            {
                saveDataFile(f);
            }
	}

    }
//}}}

//{{{ saveDataFile

    public void saveDataFile(File f) {
	try {
	    Writer w = new FileWriter(f);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    addAllDataPoints();
	    Iterator iter = allPoints.iterator();
	    while (iter.hasNext()) {
		AbstractPoint point = (AbstractPoint) iter.next();
		out.print(df.format(point.getZ()));
		out.print(" ");
		out.print(intf.format(point.getX()));
		out.print(" ");
		out.println(intf.format(point.getY()));


	    }
	    out.flush();
	    w.close();

	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An error occurred while saving the file.",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
        }
    }
//}}}

//{{{ plotByScore

    private void plotByScore(boolean plotStat, double scaleFactor) {
	if (plotStat) {
	    //Collection values = dataMap.values();
	    Iterator iter = allPoints.iterator();
	    while (iter.hasNext()) {
		//System.out.print("1");
		//ArrayList value = (ArrayList) iter.next();
		//Iterator points = value.iterator();
		//while (points.hasNext()) {
		KPoint point = (KPoint) iter.next();
		if (logScaleBox.getState()) {
		    point.setZ(Math.log(Double.parseDouble(point.getName())+1)*scaleFactor);
		} else {
		    point.setZ(Double.parseDouble(point.getName())*scaleFactor);
		}
		if (scaleFactor>=100) {
		    point.setRadius((float)((Double.parseDouble(point.getName())*scaleFactor/70)));
		} else {
		    point.setRadius((float)(Double.parseDouble(point.getName())/10));
		}
		if (point.getRadius() > 3) {
		    point.setRadius((float)3);
		}
		    //}
	    }
	} else {
	    Iterator iter = allPoints.iterator();
	    while (iter.hasNext()) {
		KPoint point = (KPoint) iter.next();
		KList list = (KList) point.getParent();
		point.setZ(Double.parseDouble(list.getName()));
	    }
	}
    }
//}}}

//{{{ onHighlightRange

    public void onHighlightRange(ActionEvent ev) {
	if (isNumeric(lowNumField.getText())&&(isNumeric(highNumField.getText()))) {
	    double firstNum = Double.parseDouble(lowNumField.getText());
	    double secondNum = Double.parseDouble(highNumField.getText());
	    highlightRange(firstNum, secondNum, (KPaint) color1.getSelectedItem());
	} else {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(), "You have to put numbers in the text boxes!", "Error",
					  JOptionPane.ERROR_MESSAGE);
	}
	kCanvas.repaint();
    }
//}}}
    
//{{{ onFixScore

    public void onFixScore(ActionEvent ev) {
	//Collection values = dataMap.values();
	//Iterator iter = values.iterator();
	
	listMap.clear();
	//dataMap.clear();
	Iterator iter = allPoints.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    double temp = point.getX();
	    double clashValue = point.getY();
	    point.setX(Double.parseDouble(point.getName()));
	    point.setName(Double.toString(clashValue));
	    point.setY(temp);
	    point.setRadius((float)clashValue);
	    //makeDataMap(point, 100);
	    sortByValue(0, point);
	}
	
	plotByScore(true, 5);
	setDefaultColors();
	reSortKlists();
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }
     //}}}

//{{{ onSmoothing
          
    public void onSmoothing(ActionEvent ev) {
	/*
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	long startTime = System.currentTimeMillis();
	allPoints.clear();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if ((group.hasMaster("Data Points"))&&(group.isOn())) {
		KSubgroup subgroup = (KSubgroup) group.getChildAt(0);
		KList list = (KList) subgroup.getChildAt(0);
		Iterator points = list.iterator();
		while (points.hasNext()) {
		    KPoint point = (KPoint) points.next();
		    allPoints.add(point);
		}
	    }
	}

	PointSorter ps = new PointSorter(allPoints, PointSorter.SORTBY_Y);
	allPoints = (ArrayList) ps.sortPhiPsi();
        ps = new PointSorter(allPoints, PointSorter.SORTBY_X);
	allPoints = (ArrayList) ps.sortPhiPsi();
	*/
	long startTime = System.currentTimeMillis();
	addAllDataPoints();
	for (int i = 0; i < allPoints.size(); i++) {
	    KPoint point = (KPoint) allPoints.get(i);
	    if ((i+1) < allPoints.size()) {
		KPoint nextPoint = (KPoint) allPoints.get(i+1);
		if ((point.getX()==nextPoint.getX())&&(point.getY()==nextPoint.getY())&&(point.getZ()!=nextPoint.getZ())) {
		    double z = point.getZ();
		    double npz = nextPoint.getZ();
		    point.setZ(z+npz);
		    nextPoint.setZ(z+npz);
		}
	    }

	    //newList.add(point);
	}
	long endTime = System.currentTimeMillis();
	
	System.out.println("Total Time to smooth: " + ((endTime-startTime)/1000) + " seconds");
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	
    }
//}}}

//{{{ event functions
//###############################################################################################
    /**
     * Event handler for when action performed.
     */
    public void actionPerformed(ActionEvent ev) {
	if (plotChangeBox.isSelected()) {
	    plotByScore(true, Double.parseDouble(scalingField.getText()));
	    //dataMap.clear();
	    
	    //System.out.println("selected");
	} else {
	    plotByScore(false, 5);
	    //System.out.println("not selected");
	}
	kCanvas.repaint();
    }
//}}}
    
    public boolean isNumeric(String s) {
	try {
	    Double.parseDouble(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return null; }

    public Container getToolPanel()
    { return pane; }
    
    public String toString()
    { return "AutoBondRot Data"; }
//}}}
}
//}}}
