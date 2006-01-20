// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;

import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import driftwood.gui.*;



public class PlottingTool extends BasicTool {
    

//{{{ Variable definitions
//##################################################################################################
    ArrayList allPoints;
    TreeMap drawnPoints;
    JFileChooser filechooser;

    TablePane pane;
    JButton plotButton;
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
	pane.newRow();
	for(int i = 0; i < numColumns; i++) {
	    JComboBox comboBox = new JComboBox(axes);
	    comboBoxes[i] = comboBox;
	    pane.add(comboBox);
	}
	plotButton = new JButton(new ReflectiveAction("Plot!", null, this, "onPlot"));
	pane.add(plotButton);
	


	
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
	allPoints = new ArrayList();
	drawnPoints = new TreeMap();
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
			scanFile(reader);
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

//{{{ scanFile
//##################################################################################################
    /**
     * Does most of the work reading and analyzing the data files.
     **/
    private void scanFile(BufferedReader reader) {
	String line;
	try {
	    while((line = reader.readLine())!=null){
		line = line.trim();
		String[] values = line.split("\\s");
		//int numColumns = values.length;
		//System.out.println(numColumns);
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
	createPoints(x, y, z, color);
    }

    public void createPoints(int x, int y, int z, int color) {
	drawnPoints.clear();
	double minColor = 100000;
	double maxColor = -100000;
	Iterator iter = allPoints.iterator();
	while (iter.hasNext()) {
	    String[] value = (String[]) iter.next();
	    if (color != -1) {
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
		list.addMaster(bin.toString());
		drawnPoints.put(bin, list);
		//System.out.println(bin);
	    }
	    /*
	    for (double d = minColor; d <= maxColor; d=d+perDiv) {
		System.out.println(d);
		Double bin = new Double((double)Math.round(d*1000)/1000);
		KList list = new KList(null, bin.toString());
		list.addMaster(bin.toString());
		drawnPoints.put(bin, list);
		System.out.println(bin);
		}*/
	} else {
	    drawnPoints.put(new Double(0), new KList());
	}
	
	iter = allPoints.iterator();
	BallPoint point;
	//double minColor = 100000;
	//double maxColor = -100000;
	while (iter.hasNext()) {
	    String[] value = (String[]) iter.next();
	    point = new BallPoint(null, value[0]);
	    point.setRadius(2);
	    if (x != -1) {
		point.setX(Double.parseDouble(value[x]));
	    } else {
		point.setX(0);
	    }
	    if (y != -1) {
		point.setY(Double.parseDouble(value[y]));
	    } else {
		point.setY(0);
	    }
	    if (z != -1) {
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
		KList list = (KList) drawnPoints.get(new Double(binValRounded));
		list.add(point);
		point.setOwner(list);
	    } else {
		KList list = (KList) drawnPoints.get(new Double(0));
		list.add(point);
		point.setOwner(list);
	    }
	    //drawnPoints.add(point);
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
	Collection lists = drawnPoints.values();
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
