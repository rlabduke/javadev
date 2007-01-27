// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;
import king.points.*;
import king.tool.util.*;
import driftwood.gui.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.text.DecimalFormat;
//}}}

public class FramerTool extends BasicTool {

//{{{ Constants   
    static final DecimalFormat df = new DecimalFormat("0.000");
    //static final DecimalFormat intf = new DecimalFormat("0");
//}}}

//{{{ Variable definitions
//###############################################################
    TreeMap caMap;
    TreeMap oxyMap;
    //TreeMap cMap;
    //TreeMap nitMap;
    TreeMap bfactMap;
    TreeMap resultsMap; // index is "n" and the element is a map of results
    HashSet includedPoints;

    JButton exportButton, doAllButton;
    JFileChooser filechooser;
    JTextField lowNumField, highNumField;
    TablePane pane;

//}}}

    public FramerTool(ToolBox tb) {
	super(tb);
    }

    public void start() {
	//if (kMain.getKinemage() == null) return;
	buildGUI();
	caMap = new TreeMap();
	oxyMap = new TreeMap();
	//cMap = new TreeMap();
	//nitMap = new TreeMap();
	bfactMap = new TreeMap();
	includedPoints = new HashSet();
	resultsMap = new TreeMap();
	show();
    }  

    public void reset() {
	caMap.clear();
	oxyMap.clear();
	bfactMap.clear();
	includedPoints.clear();
	resultsMap.clear();
    }

    //{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {
	dialog = new JDialog(kMain.getTopWindow(), "Loops", false);
	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);

	exportButton = new JButton(new ReflectiveAction("Export!", null, this, "onExport"));

	doAllButton = new JButton(new ReflectiveAction("Analyze All Files", null, this, "onDoAll"));

	pane = new TablePane();
	pane.newRow();
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(exportButton);
	pane.add(doAllButton);

	dialog.addWindowListener(this);
	dialog.setContentPane(pane);
    }
    //}}}

    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
	super.click(x, y, p, ev);
	if (p != null) {
	    KList parent = (KList) p.getParent();
	    //while !(parent instanceof Kinemage) {
	    //	parent = parent.getParent();
	    //}
	    //TreeMap caMap = new TreeMap();
	    //TreeMap oxyMap = new TreeMap();
	    if (!includedPoints.contains(p)) {
		splitKin(parent, caMap, oxyMap);
	    }
	    if (KinUtil.isNumeric(lowNumField.getText())&&KinUtil.isNumeric(highNumField.getText())) {
		/*
		int numPep = Integer.parseInt(lowNumField.getText());
		int resNum = KinUtil.getResNumber(p);
		KPoint ca0 = (KPoint) caMap.get(new Integer(resNum - 1));
		KPoint ca1 = (KPoint) caMap.get(new Integer(resNum));
		KPoint caN = (KPoint) caMap.get(new Integer(resNum + numPep));
		KPoint caN1 = (KPoint) caMap.get(new Integer(resNum + numPep + 1));
		KPoint co1 = (KPoint) oxyMap.get(new Integer(resNum));
		KPoint coN = (KPoint) oxyMap.get(new Integer(resNum + numPep));
		//System.out.println(ca0);
		Framer.calphaAnalyze(ca0, ca1, caN, caN1, co1, coN);
		*/
		int lowNum = Integer.parseInt(lowNumField.getText());
		int highNum = Integer.parseInt(highNumField.getText());
		for (int i = lowNum; i <= highNum; i++) {
		    doKin(i);
		}
	    } else {
	    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
						  JOptionPane.ERROR_MESSAGE);
	    }
	    
	}
    }

    public void doKin(int numPep) {
	//int numPep = Integer.parseInt(lowNumField.getText());
	Set keys = caMap.keySet();
	Iterator iter = keys.iterator();
	HashMap oneNResults = new HashMap(); // index is residue "0", and element is ArrayList of the calculations
	while (iter.hasNext()) {
	    Integer lowInteger = (Integer) iter.next();
	    int lowNum = lowInteger.intValue();
	    Integer highNum = new Integer(lowNum + numPep + 2);
	    if (isContinuous(lowInteger, highNum)) {
		KPoint ca0 = (KPoint) caMap.get(new Integer(lowNum));
		KPoint ca1 = (KPoint) caMap.get(new Integer(lowNum + 1));
		KPoint caN = (KPoint) caMap.get(new Integer(lowNum + numPep + 1));
		KPoint caN1 = (KPoint) caMap.get(new Integer(lowNum + numPep + 2));
		KPoint co0 = (KPoint) oxyMap.get(new Integer(lowNum));
		KPoint coN = (KPoint) oxyMap.get(new Integer(lowNum + numPep + 1));
		//System.out.print(lowNum + " ");
		ArrayList results = Framer.calphaAnalyzeList(ca0, ca1, caN, caN1, co0, coN);

		//B-factor
	        Double bfact = (Double) bfactMap.get(new Integer(lowNum));
		//System.out.println(bfact);
		results.add(bfact);
		//System.out.println(KinUtil.getResNumber(ca0.getName()));
		//oneNResults.ensureCapacity(KinUtil.getResNumber(ca0.getName()));
		//oneNResults.add(KinUtil.getResNumber(ca0.getName()), results);
		oneNResults.put(new Integer(KinUtil.getResNumber(ca0.getName())), results);
		//plotValues(results[0], results[1], results[2], ca0.getName());
		//plotValues(results[3], results[4], results[5], ca0.getName(), ca0.getColor());
	    }
	}
	resultsMap.put(new Integer(numPep), oneNResults);
    }

    public void plotValues(double x, double y, double z, String ptID, KPaint color) {
	Kinemage kin = kMain.getKinemage();
	KGroup group = new KGroup("group");
	group.setAnimate(true);
	group.addMaster("Data Points");
	kin.add(group);
	KGroup subgroup = new KGroup("sub");
	subgroup.setHasButton(false);
	group.add(subgroup);
	KList list = new KList(KList.BALL, "Points");
	BallPoint point = new BallPoint(ptID);
	point.setX(x);
	point.setY(y);
	point.setZ(z);
	point.setColor(color);
	list.add(point);
	subgroup.add(list);
	list.setParent(subgroup);
    }

    
    public boolean isContinuous(Integer lowNum, Integer highNum) {
	for (int i = lowNum.intValue(); i <= highNum.intValue(); i++) {
	    if (!oxyMap.containsKey(new Integer(i))||!caMap.containsKey(new Integer(i))) {
		return false;
	    }
	}
	return true;
    }

    // calculates maximum Bfactor for a given Ca using the ca, c, o, n+1, and ca+1, because framer
    //   is dealing with peptides.
    public void splitKin(AGE target, TreeMap caMap, TreeMap oxyMap) {
	//TreeMap caMap = new HashMap();
	//TreeMap oxyMap = new HashMap();
	if (target instanceof Kinemage) {
	}
	if ((target instanceof KList)&&(target.isOn())) {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		includedPoints.add(pt);
		int resNum = KinUtil.getResNumber(pt);
		//System.out.println("resNum = " + resNum);
		String atomName = KinUtil.getAtomName(pt).toLowerCase();
		double bVal = KinUtil.getBvalue(pt);
		//System.out.print(resNum + " " + bVal + ",");
		double maxBval = -10000;
		if (atomName.equals("ca")) {
		    caMap.put(new Integer(resNum), pt);

		    if (bfactMap.containsKey(new Integer(resNum))) maxBval = ((Double) bfactMap.get(new Integer(resNum))).doubleValue();
		    if (bVal >= maxBval) bfactMap.put(new Integer(resNum), new Double(bVal));
		    if (bfactMap.containsKey(new Integer(resNum-1))) maxBval = ((Double) bfactMap.get(new Integer(resNum-1))).doubleValue();
		    if (bVal >= maxBval) bfactMap.put(new Integer(resNum-1), new Double(bVal));
		}
		if (atomName.equals("o")) {
		    oxyMap.put(new Integer(resNum), pt);
		    if (bfactMap.containsKey(new Integer(resNum))) maxBval = ((Double) bfactMap.get(new Integer(resNum))).doubleValue();
		    if (bVal >= maxBval) bfactMap.put(new Integer(resNum), new Double(bVal));
		}
		if (atomName.equals("n")) {
		    if (bfactMap.containsKey(new Integer(resNum-1))) maxBval = ((Double) bfactMap.get(new Integer(resNum-1))).doubleValue();
		    if (bVal >= maxBval) bfactMap.put(new Integer(resNum-1), new Double(bVal));
		    //nitMap.put(new Integer(resNum), pt));
		}
		if (atomName.equals("c")) {
		    if (bfactMap.containsKey(new Integer(resNum))) maxBval = ((Double) bfactMap.get(new Integer(resNum))).doubleValue();
		    if (bVal >= maxBval) bfactMap.put(new Integer(resNum), new Double(bVal));
		    //cMap.put(new Integer(resNum), pt);
		}
	    }
	} else {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		splitKin((AGE) iter.next(), caMap, oxyMap);
	    }
	}
    }
	
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

//{{{ onDoAll
    public void onDoAll(ActionEvent ev) {

	if (filechooser == null) makeFileChooser();
	
	if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow())) {
	    //try {
	    File f = filechooser.getSelectedFile();
	    System.out.println(f.getPath() + " : " + f.getName() + " : " + f.getParent());
	    File[] allFiles = f.getParentFile().listFiles();
	    //for (int i = 0; i < allFiles.length; i++) {
	    JFileChooser saveChooser = new JFileChooser();
	    String currdir = System.getProperty("user.dir");
	    if(currdir != null) {
		saveChooser.setCurrentDirectory(new File(currdir));
	    }
	    if (saveChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow())) {
		File saveFile = saveChooser.getSelectedFile();
		if( !f.exists() ||
		    JOptionPane.showConfirmDialog(kMain.getTopWindow(),
						  "This file exists -- do you want to overwrite it?",
						  "Overwrite file?", JOptionPane.YES_NO_OPTION)
		    == JOptionPane.YES_OPTION ) 
		{    
		    doAll(allFiles, saveFile);
		}
	    }
	}
    }
    
//}}}
	
    public void doAll(File[] allFiles, File saveFile) {
	try {
	    //Integer numPep = Integer.valueOf(lowNumField.getText());
	    Writer w = new FileWriter(saveFile);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    for (int i = 0; i < allFiles.length; i++) {
		File pdbFile = allFiles[i];
		if (pdbFile.getName().indexOf(".kin") > -1) {
		    reset();
		    kMain.getKinIO().loadFile(pdbFile, null);
		    Kinemage kin = kMain.getKinemage();
		    kin.getMasterByName("mainchain").setOn(true);
		    kin.getMasterByName("Calphas").setOn(false);
		    kin.getMasterByName("rotamer outlie").setOn(false);
		    AGE kage = (KGroup) kin.getChildren().get(0);
		    
		    while (!(kage instanceof KList)) {
			kage = (AGE) kage.getChildren().get(0);
		    }
		    //KGroup group = (KGroup) kin.getChildAt(0);
		    //KSubgroup sub = (KSubgroup) group.getChildAt(0);
		    KList list = (KList) kage;
		    if (list.getName().equals("mc")) {
			splitKin(list, caMap, oxyMap);
			if (KinUtil.isNumeric(lowNumField.getText())&&KinUtil.isNumeric(highNumField.getText())) {
			    int lowNum = Integer.parseInt(lowNumField.getText());
			    int highNum = Integer.parseInt(highNumField.getText());
			    for (int numPep = lowNum; numPep <= highNum; numPep++) {
				doKin(numPep);
				Iterator diffNiter = resultsMap.values().iterator(); // values are HashMaps of results for a given n.
				while (diffNiter.hasNext()) {
				    HashMap oneN = (HashMap) diffNiter.next();
				    TreeSet keys = new TreeSet(oneN.keySet()); // keys are residue number of ca0, the first residue for calc of results
				    Iterator keysIter = keys.iterator();
				    //out.print(pdbFile.getName().substring(0,4) + ",");
				    while (keysIter.hasNext()) {
					//out.print(pdbFile.getName().substring(0, 4) + " ");
					out.print(pdbFile.getName().substring(0,4) + " " + numPep + ",");
					Integer n = (Integer) keysIter.next();
					ArrayList results = (ArrayList) oneN.get(n);
					out.print(n);
					Iterator resIter = results.iterator();
					while (resIter.hasNext()) {
					    Double value = (Double) resIter.next();
					    out.print(",");
					    out.print(df.format(value.doubleValue()));
					    //out.print(" ");
					}
					out.println("");
				    }
				}
				resultsMap.clear();
			    }
			}
		
		    } else {
			System.out.println("list not detected in " + pdbFile);
		    }
		
		    kMain.getTextWindow().setText("");
		    kMain.getStable().closeCurrent();
		}
	    }
	    out.flush();
	    w.close();
	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
					  "An error occurred while saving the file.",
					  "Sorry!", JOptionPane.ERROR_MESSAGE);
	}
    }
    
//{{{ saveDataFile

    public void saveDataFile(File f) {
	try {
	    Writer w = new FileWriter(f);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    //addAllDataPoints();
	    Kinemage kin = kMain.getKinemage();
	    String kinName = kin.getName().substring(0,4);
	    Iterator diffNiter = resultsMap.values().iterator(); // values are HashMaps of results for a given n.
	    while (diffNiter.hasNext()) {
		HashMap oneN = (HashMap) diffNiter.next();
		TreeSet keys = new TreeSet(oneN.keySet()); // keys are residue number of ca0, the first residue for calc of results
		Iterator keysIter = keys.iterator();
		while (keysIter.hasNext()) {
		    Integer n = (Integer) keysIter.next();
		    ArrayList results = (ArrayList) oneN.get(n);
		    out.print(kinName + ",");
		    out.print(n);
		    Iterator resIter = results.iterator();
		    while (resIter.hasNext()) {
			Double value = (Double) resIter.next();
			//System.out.println(n + " " + value);
			out.print(",");
			out.print(df.format(value.doubleValue()));
			//out.print(" ");
		    }
		    out.println("");
		    /*
		//ArrayList list = (ArrayList) iter.next();
		//ListIterator listIter = list.listIterator();
		while (listIter.hasNext()) {
		    int n = listIter.nextIndex();
		    ArrayList results = (ArrayList) listIter.next();
		    out.print(n);
		    //out.print(" ");
		    Iterator resIter = results.iterator();
		    while (resIter.hasNext()) {
			Double value = (Double) resIter.next();
			out.print(" ");
			out.print(value);
			//out.print(" ");
			}*/
		    //AbstractPoint point = (AbstractPoint) iter.next();
		    //out.print(df.format(point.getZ()));
		    //out.print(" ");
		    //out.print(intf.format(point.getX()));
		    //out.print(" ");
		    //out.println(intf.format(point.getY()));
		}
	    

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

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return dialog; }

    public String toString() { return "Framer Tool"; }    

    //}}}
    
}//class
