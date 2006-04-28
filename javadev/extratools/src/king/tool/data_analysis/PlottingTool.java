// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;
import king.tool.util.KinUtil;

import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.Strings;


public class PlottingTool extends BasicTool {
    

//{{{ Variable definitions
//##################################################################################################
    ArrayList allPoints; //list of original values from selected file.
    TreeMap binnedPoints; //points split by bin (color) value.
    HashMap plottedPoints; //points split by value.
    JFileChooser filechooser;
    JComboBox color1;

    TablePane pane;
    JButton plotButton, replotButton, exportButton, filterButton, resetButton;
    JTextField xMultField, yMultField, zMultField;
    JTextField xFiltField, yFiltField, zFiltField;
    JTextField xFiltRange, yFiltRange, zFiltRange;
    JCheckBox clickColorBox;
    JComboBox[] comboBoxes;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PlottingTool(ToolBox tb)
    {
        super(tb);
        
	//buildGUI();
        //makeFileFilters();
	
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI() {
	
	dialog = new JDialog(kMain.getTopWindow(), "Data Plotter", false);
	if (allPoints == null) return;
	Iterator iter = allPoints.iterator();
	String[] values = (String[]) iter.next();
	int numColumns = values.length;

	pane = new TablePane();
	pane.newRow();

	JLabel infoLabel = new JLabel("Data Plotter has detected " + numColumns + " columns of data;  Row 1 shown below.");
	pane.add(infoLabel, numColumns, 1);
	pane.newRow();

	comboBoxes = new JComboBox[numColumns];
	
	String[] axes = {"", "x", "y", "z", "color"};
	for(int i = 0; i < numColumns; i++) {
	    JLabel exampleLabel = new JLabel(values[i]);
	    pane.add(exampleLabel);
	}
	color1 = new JComboBox(KPalette.getStandardMap().values().toArray());
	color1.setSelectedItem(KPalette.blue);
	//pane.add(color1, 2, 1);
	pane.add(color1);
	clickColorBox = new JCheckBox("Color on click");
	clickColorBox.setSelected(false);
	pane.add(clickColorBox);

	pane.newRow();
	for(int i = 0; i < numColumns; i++) {
	    JComboBox comboBox = new JComboBox(axes);
	    comboBoxes[i] = comboBox;
	    pane.add(comboBox);
	}
	plotButton = new JButton(new ReflectiveAction("Plot!", null, this, "onPlot"));
	pane.add(plotButton);
	
	replotButton = new JButton(new ReflectiveAction("Replot!", null, this, "onPlot"));
	replotButton.setEnabled(false);
	pane.add(replotButton);

	pane.newRow();
	JLabel xLabel = new JLabel("x mult=");
	xMultField = new JTextField("1", 5);
	JLabel yLabel = new JLabel("y mult=");
	yMultField = new JTextField("1", 5);
	JLabel zLabel = new JLabel("z mult=");
	zMultField = new JTextField("1", 5);
	
	pane.add(xLabel);
	pane.add(xMultField);
	
	pane.add(yLabel);
	pane.add(yMultField);
	
	pane.add(zLabel);
	pane.add(zMultField);

	exportButton = new JButton(new ReflectiveAction("Export!", null, this, "onExport"));
	pane.add(exportButton, 2, 1);

	JLabel xLab2 = new JLabel("keep x=");
	xFiltField = new JTextField("0", 5);
	xFiltRange = new JTextField("-1", 5);
	JLabel yLab2 = new JLabel("keep y=");
	yFiltField = new JTextField("0", 5);
	yFiltRange = new JTextField("-1", 5);
	JLabel zLab2 = new JLabel("keep z=");
	zFiltField = new JTextField("0", 5);
	zFiltRange = new JTextField("-1", 5);

	filterButton = new JButton(new ReflectiveAction("Filter!", null, this, "onFilter"));
	resetButton = new JButton(new ReflectiveAction("ResetFilt", null, this, "onReset"));

	pane.newRow();
	pane.add(xLab2);
	pane.add(xFiltField);
	pane.add(xFiltRange);
	pane.add(yLab2);
	pane.add(yFiltField);
	pane.add(yFiltRange);
	pane.add(zLab2);
	pane.add(zFiltField);
	pane.add(zFiltRange);
	pane.add(filterButton);
	pane.add(resetButton);
	
        dialog.addWindowListener(this);
	dialog.setContentPane(pane);

	
    }
//}}}

//{{{ start
//##################################################################################################
    public void start()
    {
        //if(kMain.getKinemage() == null) return;

        //try
        //{
	//buildGUI();
            //if(kMain.getApplet() != null)   openMapURL();
            //else                            openMapFile();
	    //dataMap = new HashMap();
	//listMap = new HashMap();
	//offPoints = new ArrayList();
	//System.out.println("Starting Plotter");
	allPoints = new ArrayList();
	binnedPoints = new TreeMap();
	plottedPoints = new HashMap();
	openFile();

	buildGUI();
	
	    
	show();
	    
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


//{{{ openOpenFile
//##################################################################################################
    public void openFile()
    {
        // Create file chooser on demand
        if(filechooser == null) makeFileChooser();
        
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
	{
	    try {
		File f = filechooser.getSelectedFile();
		if(f != null && f.exists()) {
		    //dialog.setTitle(f.getName());
		    BufferedReader reader = new BufferedReader(new FileReader(f));
		    String delimChoice = askDelimiter(f.getName());
		    if (delimChoice != null) {
			scanFile(reader, delimChoice);
		    }
		    //String fileChoice = askFileFormat(f.getName());
		    //System.out.println(fileChoice);
		    //String angleChoice = "none";
		    //if (fileChoice.equals("First")) {
		    //angleChoice = askAngleFormat(f.getName());
		    //}
		    //if ((fileChoice != null)&&(angleChoice != null)) {
		    //listMap = new HashMap();
		    //offPoints = new ArrayList();
		    //allPoints = new ArrayList();
		    //scanFile(reader);
			//System.out.println(allPoints.size());
			reader.close();
			//}

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
					      "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
	    }
	}
    }
    
//}}}

    //{{{ askFormats
    private String askDelimiter(String f) {
	Object[] choices = {"Comma (,)", "Semi-colon (;)", "Colon (:)", "Space"};
	String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(), 
		  "Please indicate the delimiter to use to parse the data.", 
							     "Choose", JOptionPane.PLAIN_MESSAGE, 
							     null, choices, "Comma (,)");
	if (choice.equals("Comma (,)")) {
	    return ",";
	} else if (choice.equals("Semi-colon (;)")) {
	    return ";";
	} else if (choice.equals("Colon (:)")) {
	    return ":";
	} else {
	    return " ";
	}
    }
    
    //}}}

//{{{ scanFile
//##################################################################################################
    /**
     * Does most of the work reading and analyzing the data files.
     **/
    private void scanFile(BufferedReader reader, String delimiter) {
	String line;
	try {
	    while((line = reader.readLine())!=null){
		line = line.trim();
		//System.out.println(line);
		//String[] values = line.split("\\s");
		//int numColumns = values.length;
		//System.out.println(numColumns);
		String[] values = Strings.explode(line, delimiter.charAt(0), false, true);
		allPoints.add(values);


	    }
	}
	catch (IOException ex) {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
        }
    }

    public void onPlot(ActionEvent ev) {
	replotButton.setEnabled(true);
	int numColumns = comboBoxes.length;
	int x = -1, y = -1, z = -1, color = -1;
	for (int i = 0; i < numColumns; i++) {
	    //System.out.print(comboBoxes[i].getSelectedItem());
	    String selectedVal = (String) comboBoxes[i].getSelectedItem();
	    //int x = -1, y = -1, z = -1, color = -1;
	    if (selectedVal.equals("x")) {
		x = i;
	    }
	    if (selectedVal.equals("y")) {
		y = i;
	    }
	    if (selectedVal.equals("z")) {
		z = i;
	    }
	    if (selectedVal.equals("color")) {
		color = i;
	    }
	    //createPoints(x, y, z, color);
	    //System.out.println(x + y + z);
	}
	if (ev.getSource().equals(plotButton)) {
	    createPoints(x, y, z, color);
	} else {
	    replotPoints(x, y, z, color);
	}
    }

    public void createPoints(int x, int y, int z, int color) {
	binnedPoints.clear();
	plottedPoints.clear();
	double minColor = 100000;
	double maxColor = -100000;
	Iterator iter = allPoints.iterator();
	if (color != -1) {
	    while (iter.hasNext()) {
		String[] value = (String[]) iter.next();
		//if (color != -1) {
		double dColor = Double.parseDouble(value[color]);
		if (minColor > dColor) {
		    minColor = dColor;
		}
		if (maxColor < dColor) {
		    maxColor = dColor;
		}
	    }
	}
	double perDiv = (maxColor-minColor)/10;
	//System.out.println(maxColor);
	if (color != -1) {
	    //double perDiv = (maxColor-minColor)/10;
	    for (int i = 0; i < 11; i++) {
		// I'm forced to round the bins because round-off error in calculation of bins causes nullpointerexceptions
		//  without rounding the bins (and calculations later).
		Double bin = new Double((double)Math.round((minColor + perDiv * i)*1000)/1000);
		KList list = new KList(null, bin.toString());
		list.flags |= KList.NOHILITE;
		list.addMaster(bin.toString());
		binnedPoints.put(bin, list);
		//System.out.println(bin);
	    }
	    /*
	    for (double d = minColor; d <= maxColor; d=d+perDiv) {
		System.out.println(d);
		Double bin = new Double((double)Math.round(d*1000)/1000);
		KList list = new KList(null, bin.toString());
		list.addMaster(bin.toString());
		binnedPoints.put(bin, list);
		System.out.println(bin);
		}*/
	} else {
	    binnedPoints.put(new Double(0), new KList());
	}
	
	iter = allPoints.iterator();
	BallPoint point;
	//double minColor = 100000;
	//double maxColor = -100000;
	while (iter.hasNext()) {
	    String[] value = (String[]) iter.next();
	    point = new BallPoint(null, value[0] + " " + value[1]);
	    plottedPoints.put(value, point);
	    point.setRadius((float)0.1);
	    if ((x != -1)&&(KinUtil.isNumeric(value[x]))) {
		point.setX(Double.parseDouble(value[x]));
	    } else {
		point.setX(0);
	    }
	    if ((y != -1)&&(KinUtil.isNumeric(value[y]))) {
		point.setY(Double.parseDouble(value[y]));
	    } else {
		point.setY(0);
	    }
	    if ((z != -1)&&(KinUtil.isNumeric(value[z]))) {
		point.setZ(Double.parseDouble(value[z]));
	    } else {
		point.setZ(0);
	    }
	    if (color != -1) {
		double colValue = Double.parseDouble(value[color]);
		double binVal = (Math.floor((colValue-minColor)/perDiv) * perDiv)+minColor;
		double binValRounded = ((double)Math.round(binVal*1000)/1000);
		//System.out.println(colValue);
		//System.out.println(binValRounded);
		KList list = (KList) binnedPoints.get(new Double(binValRounded));
		list.add(point);
		point.setOwner(list);
	    } else {
		KList list = (KList) binnedPoints.get(new Double(0));
		list.add(point);
		point.setOwner(list);
	    }
	    //binnedPoints.add(point);
	    /*
	    if (color != -1) {
		double dColor = Double.parseDouble(value[color]);
		if (minColor > dColor) {
		    minColor = dColor;
		}
		if (maxColor < dColor) {
		    maxColor = dColor;
		}
	    }*/
	}
	//System.out.println(minColor + " " + maxColor);
	plot();
    }

    public void plot() {
	Kinemage kin = kMain.getKinemage();
	kin.getMasterByName("Data Points");
	//Iterator iter = kin.iterator();
	//while (iter.hasNext()) {
	KGroup group = new KGroup(kin, "test");
	group.setAnimate(true);
	group.addMaster("Data Points");
	kin.add(group);
	KSubgroup subgroup = new KSubgroup(group, "test2");
	subgroup.setHasButton(false);
	group.add(subgroup);
	//KList list = new KList(subgroup, "test3");
	//KGroup group = (KGroup) iter.next();
	//if (group.hasMaster("Data Points")) {
	//KSubgroup subgroup = (KSubgroup) group.getChildAt(0);
	//KList list = (KList) subgroup.getChildAt(0);
	Collection lists = binnedPoints.values();
	Iterator iter = lists.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    list.setOwner(subgroup);
	    subgroup.add(list);
	    //point.setOwner(list);
	    //list.add(point);
	}
	//subgroup.add(list);
    	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
      
    }

    public void replotPoints(int x, int y, int z, int color) {
	// x, y, z, and color are array indexes.
	Iterator iter = allPoints.iterator();
	while (iter.hasNext()) {
	    String[] value = (String[]) iter.next();
	    KPoint point = (KPoint) plottedPoints.get(value);
	    if (x != -1) {
		point.setX(Double.parseDouble(value[x]));
		if (KinUtil.isNumeric(xMultField.getText())) {
		    point.setX(point.getX() * Double.parseDouble(xMultField.getText()));
		}
	    } else {
		point.setX(0);
	    }
	    if (y != -1) {
		point.setY(Double.parseDouble(value[y]));
		if (KinUtil.isNumeric(yMultField.getText())) {
		    point.setY(point.getY() * Double.parseDouble(yMultField.getText()));
		}
	    } else {
		point.setY(0);
	    }
	    if (z != -1) {
		point.setZ(Double.parseDouble(value[z]));
		if (KinUtil.isNumeric(zMultField.getText())) {
		    point.setZ(point.getZ() * Double.parseDouble(zMultField.getText()));
		}
	    } else {
		point.setZ(0);
	    }
	}
	kCanvas.repaint();
    }

    

    public void onRescale(ActionEvent ev) {
	Collection points = plottedPoints.values();
	Iterator iter = points.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    if (KinUtil.isNumeric(xMultField.getText()) && KinUtil.isNumeric(yMultField.getText()) && KinUtil.isNumeric(zMultField.getText())) {
		point.setDrawX(point.getDrawX() * Double.parseDouble(xMultField.getText()));
		point.setDrawY(point.getDrawY() * Double.parseDouble(yMultField.getText()));
		point.setDrawZ(point.getDrawZ() * Double.parseDouble(zMultField.getText()));
	    }
	}
	kCanvas.repaint();
    }

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);    
	if (p != null) {
	    if (clickColorBox.isSelected()) {
		p.setColor((KPaint)color1.getSelectedItem());
	    }
	}
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

//{{{ onFilter
    
    public void onFilter(ActionEvent ev) {
	double x, xrange, y, yrange, z, zrange;
	if ((KinUtil.isNumeric(xFiltField.getText()))&&(KinUtil.isNumeric(xFiltRange.getText()))&& 
	    (KinUtil.isNumeric(yFiltField.getText()))&&(KinUtil.isNumeric(yFiltRange.getText()))&& 
	    (KinUtil.isNumeric(zFiltField.getText()))&&(KinUtil.isNumeric(zFiltRange.getText()))) {
		
	    x = Double.parseDouble(xFiltField.getText());
	    xrange = Double.parseDouble(xFiltRange.getText());
	    y = Double.parseDouble(yFiltField.getText());
	    yrange = Double.parseDouble(yFiltRange.getText());
	    z = Double.parseDouble(zFiltField.getText());
	    zrange = Double.parseDouble(zFiltRange.getText());
	    filterCoord(x, xrange, y, yrange, z, zrange);
	} else {
	    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
					  JOptionPane.ERROR_MESSAGE);
	    
	}

    }

///}}}

//{{{ filterCoord

    public void filterCoord(double x, double xrange, double y, double yrange, double z, double zrange) {
	double lowX = x - xrange;
	double highX = x + xrange;
	double lowY = y - yrange;
	double highY = y + yrange;
	double lowZ = z - zrange;
	double highZ = z + zrange;
	
	Set keys = plottedPoints.keySet();
	HashMap newPlottedPoints = new HashMap();
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    String[] key = (String[]) iter.next();
	    KPoint point = (KPoint) plottedPoints.get(key);
	    double xCoord = point.getX();
	    double yCoord = point.getY();
	    double zCoord = point.getZ();
	    if (xrange != -1) {
		if ((xCoord >= lowX)&&(xCoord <= highX)) {
		    newPlottedPoints.put(key, point);
		} else {
		    point.setColor(KPalette.invisible);
		}
	    }
	    if (yrange != -1) {
		if ((yCoord >= lowY)&&(yCoord <= highY)) {
		    newPlottedPoints.put(key, point);
		} else {
		    point.setColor(KPalette.invisible);
		}
	    }
	    if (zrange != -1) {
		if ((zCoord >= lowZ)&&(zCoord <= highZ)) {
		    newPlottedPoints.put(key, point);
		} else {
		    point.setColor(KPalette.invisible);
		}
	    }
	}
	//plottedPoints = newPlottedPoints;
	kCanvas.repaint();

    }

///}}}

    public void onReset(ActionEvent ev) {
	xFiltField.setText("0");
	yFiltField.setText("0");
	zFiltField.setText("0");
	xFiltRange.setText("-1");
	yFiltRange.setText("-1");
	zFiltRange.setText("-1");
    }

//{{{ saveDataFile

    public void saveDataFile(File f) {
	try {
	    Writer w = new FileWriter(f);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    //addAllDataPoints();
	    out.println("@kinemage 0");
	    //out.println("@group {" + f.getName() + "} dimension=7 wrap=360 select");
	    //out.println("@balllist {" + f.getName() + "} nohilite");
	    //Iterator iter = allPoints.iterator();
	    String[] zeroVal = (String[]) allPoints.get(0);
	    out.println("@group {" + f.getName() + "} dimension=" + (zeroVal.length - 1) + " wrap=360 select");
	    out.println("@balllist {" + f.getName() + "} nohilite");
	    //int length = value.length;
	    Iterator iter = allPoints.iterator();
	    while (iter.hasNext()) {
		String[] value = (String[]) iter.next();
		out.print("{" + value[0] + "} ");
		for (int i = 1; i < value.length; i++) {
		    out.print(value[i]);
		    if (i != value.length - 1) {
			out.print(", ");
		    }
		}
		out.println("");
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

//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return null; }

    public Container getToolPanel()
    { return pane; }
    
    public String toString()
    { return "Data Plotter"; }
//}}}

}
//}}}
