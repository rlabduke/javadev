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

    HashMap             dataMap; // clashscore -> BallPoint
    TablePane           pane;
    JComboBox           color1;
    JTextField          lowNumField;
    JTextField          highNumField;
    JButton             colorButton;

    //double maxX, maxY, maxZ = -100000;
    //double minX, minY, minZ = 100000;
    //double xspan, yspan, zspan, xInc, yInc, zInc;
    HashMap             listMap; // axisValue -> KList
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
	
	pane = new TablePane();
	pane.newRow();
	pane.add(color1);
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(colorButton);
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
	    openFile();

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

	show();
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
	    /*
	    Kinemage kin = kMain.getKinemage();
	    KGroup group = new KGroup(kin, "Data Points");
	    kin.add(group);
	    kin.setModified(true);
	    
	    KSubgroup subgroup = new KSubgroup(group, "Data Points");
	    subgroup.setHasButton(false);
	    group.add(subgroup);
	    
	    KList list = new KList(subgroup, "Points");
	    list.flags |= KList.NOHILITE;
	    list.setType("BALL");
	    subgroup.add(list);
	    */
	    while((line = reader.readLine())!=null){
		
		if (line.charAt(0)=='#') {
		    System.out.println(line);
		    if (line.indexOf("#ROT")>-1) analyzeRotLine(line);
		} else {
		    line.trim();
		    StringTokenizer spaceToks = new StringTokenizer(line, " ");
		    Double clashValue = Double.valueOf(spaceToks.nextToken());
		    float clashFloat = clashValue.floatValue();
		    double x = Double.parseDouble(spaceToks.nextToken());
		    double y = Double.parseDouble(spaceToks.nextToken());
		    double z = 0;
		    if (spaceToks.hasMoreTokens()) {
			z = Double.parseDouble(spaceToks.nextToken());
		    }
		    //trackHighLows(x, y, z); // for determining what planes to split into different lists.
		    BallPoint point = new BallPoint(null, clashValue.toString());
		    if (clashFloat>0) {
			point.setRadius(clashFloat);
			point.setColor(KPalette.green);
		    } else {
			point.setRadius(-clashFloat/10);
			//point.setRadius(1);
			if (-clashFloat>15) {
			    point.setColor(KPalette.red);
			} else if (-clashFloat>5) {
			    point.setColor(KPalette.gold);
			} else {
			    point.setColor(KPalette.yellow);
			}
		    }
		    point.setOrigX(x);
		    point.setOrigY(y);
		    point.setOrigZ(z);
		    sortByValue(y, point);
		    //dataMap.put(clashValue, point);
		    //list.add(point);

		    // for recoloring by clash value
		    Integer clashInt = new Integer((int)Math.floor(clashValue.doubleValue()));
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
	    list.setType("BALL");
	    list.setHasButton(false);
	    subgroup.add(list);
	    list.setOwner(subgroup);
	}
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }

//{{{ highlightRange
//################################################################################################
    /**
     * Colors all points with value between firstNum and secondNum
     */
    private void highlightRange(int firstNum, int secondNum) {
	for (int i = firstNum; i < secondNum; i++) {
	    if (dataMap.containsKey(new Integer(i))) {
		//System.out.println("coloring " + i);
		ArrayList clashPoints = (ArrayList) dataMap.get(new Integer(i));
		Iterator iter = clashPoints.iterator();
		while (iter.hasNext()) {
		    BallPoint point = (BallPoint) iter.next();
		    point.setColor((KPaint) color1.getSelectedItem());
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
