// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;
import king.tool.util.*;
import driftwood.gui.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class FramerTool extends BasicTool {

//{{{ Variable definitions
//###############################################################
    TreeMap caMap;
    TreeMap oxyMap;
    TreeMap resultsMap; // index is "n" and the element is a map of results
    HashSet includedPoints;

    JButton exportButton;
    JTextField numField;
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
	includedPoints = new HashSet();
	resultsMap = new TreeMap();
	show();
    }  

    //{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {
	dialog = new JDialog(kMain.getTopWindow(), "Loops", false);
	numField = new JTextField("", 5);

	exportButton = new JButton(new ReflectiveAction("Export!", null, this, "onExport"));
	
	pane = new TablePane();
	pane.newRow();
	pane.add(numField);
	pane.add(exportButton);

	dialog.addWindowListener(this);
	dialog.setContentPane(pane);
    }
    //}}}

    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
	super.click(x, y, p, ev);
	if (p != null) {
	    KList parent = (KList) p.getOwner();
	    //while !(parent instanceof Kinemage) {
	    //	parent = parent.getOwner();
	    //}
	    //TreeMap caMap = new TreeMap();
	    //TreeMap oxyMap = new TreeMap();
	    if (!includedPoints.contains(p)) {
		splitKin(parent, caMap, oxyMap);
	    }
	    if (KinUtil.isNumeric(numField.getText())) {
		/*
		int numPep = Integer.parseInt(numField.getText());
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
		doKin();
	    } else {
	    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
						  JOptionPane.ERROR_MESSAGE);
	    }
	    
	}
    }

    public void doKin() {
	int numPep = Integer.parseInt(numField.getText());
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
		KPoint co1 = (KPoint) oxyMap.get(new Integer(lowNum + 1));
		KPoint coN = (KPoint) oxyMap.get(new Integer(lowNum + numPep + 1));
		System.out.println(lowNum);
		ArrayList results = Framer.calphaAnalyzeList(ca0, ca1, caN, caN1, co1, coN);
		System.out.println(KinUtil.getResNumber(ca0.getName()));
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
	KGroup group = new KGroup(kin, "group");
	group.setAnimate(true);
	group.addMaster("Data Points");
	kin.add(group);
	KSubgroup subgroup = new KSubgroup(group, "sub");
	subgroup.setHasButton(false);
	group.add(subgroup);
	KList list = new KList(null, "Points");
	BallPoint point = new BallPoint(list, ptID);
	point.setX(x);
	point.setY(y);
	point.setZ(z);
	point.setColor(color);
	list.add(point);
	subgroup.add(list);
	list.setOwner(subgroup);
    }

    
    public boolean isContinuous(Integer lowNum, Integer highNum) {
	for (int i = lowNum.intValue(); i <= highNum.intValue(); i++) {
	    if (!oxyMap.containsKey(new Integer(i))||!caMap.containsKey(new Integer(i))) {
		return false;
	    }
	}
	return true;
    }

    public void splitKin(AGE target, TreeMap caMap, TreeMap oxyMap) {
	//TreeMap caMap = new HashMap();
	//TreeMap oxyMap = new HashMap();
	if (target instanceof Kinemage) {
	}
	if ((target instanceof KList)&&(target.isOn())) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		includedPoints.add(pt);
		int resNum = KinUtil.getResNumber(pt);
		String atomName = KinUtil.getAtomName(pt).toLowerCase();
		if (atomName.equals("ca")) {
		    caMap.put(new Integer(resNum), pt);
		}
		if (atomName.equals("o")) {
		    oxyMap.put(new Integer(resNum), pt);
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

//{{{ saveDataFile

    public void saveDataFile(File f) {
	try {
	    Writer w = new FileWriter(f);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    //addAllDataPoints();
	    Iterator diffNiter = resultsMap.values().iterator(); // values are HashMaps of results for a given n.
	    while (diffNiter.hasNext()) {
		HashMap oneN = (HashMap) diffNiter.next();
		TreeSet keys = new TreeSet(oneN.keySet()); // keys are residue number of ca0, the first residue for calc of results
		Iterator keysIter = keys.iterator();
		while (keysIter.hasNext()) {
		    Integer n = (Integer) keysIter.next();
		    ArrayList results = (ArrayList) oneN.get(n);
		    out.print(n);
		    Iterator resIter = results.iterator();
		    while (resIter.hasNext()) {
			Double value = (Double) resIter.next();
			out.print(" ");
			out.print(value);
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


}
