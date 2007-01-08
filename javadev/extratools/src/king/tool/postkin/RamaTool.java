// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.points.*;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;

import driftwood.gui.*;
import driftwood.r3.*;
//}}}

public class RamaTool extends BasicTool {

//{{{ Variable definitions
//##############################################################################
    TreeMap pointMap;
    HashMap lengthMap;
    TablePane pane;


//{{{ Constructor(s)
//##############################################################################
    public RamaTool(ToolBox tb) {
	super(tb);
	buildGUI();
    }
//}}}

    protected void buildGUI() {
	JButton calcButton = new JButton(new ReflectiveAction("Calculate", null, this, "onCalc"));
	pane = new TablePane();
	pane.newRow();
	pane.add(calcButton);
    }

    public void onCalc(ActionEvent ev) {
	//analyzeKin();
	System.out.println(pointMap.size());
	//plotRama();
    }

    public void start() {
	if (kMain.getKinemage() == null) return;
	//adjacencyMap = new HashMap();
	//buildAdjacencyList();
	//pointMap = new HashMap();

	show();
    }

    public void click(int x, int y, KPoint p, MouseEvent ev) {
	super.click(x, y, p, ev);
	if (p != null) {
	    analyzeKin(p);
	    plotRama();
	}
    }

    public VectorPoint calcRama(AbstractPoint c0, AbstractPoint n1, AbstractPoint ca1, AbstractPoint c1, AbstractPoint n2, VectorPoint prev) {
	double phi = Triple.dihedral(c0, n1, ca1, c1);
	double psi = Triple.dihedral(n1, ca1, c1, n2);
	VectorPoint point = new VectorPoint(n1.getName(), prev);
	point.setX(phi);
	point.setY(psi);
	int resNum = PointComparator.getResNumber(n1.getName());
	int transNum = ((Integer)lengthMap.get(new Integer(resNum))).intValue();
	point.setZ(transNum);
	return point;
    }

    public void plotRama() {
	Kinemage kin = kMain.getKinemage();
	//KGroup group = new KGroup(kin, "Data Points");
	//kin.add(group);
	kin.setModified(true);
	kin.getMasterByName("Rama Points");
	
	//Set origKeys = listMap.keySet();
	//TreeSet keys = new TreeSet(origKeys);
	//Iterator iter = keys.iterator();
	//while (iter.hasNext()) {
	//Double key = (Double) iter.next();
	    KGroup group = new KGroup("group");
	    group.setAnimate(true);
	    group.addMaster("Data Points");
	    kin.add(group);
	    KGroup subgroup = new KGroup("sub");
	    subgroup.setHasButton(false);
	    group.add(subgroup);
	    KList list = new KList(KList.VECTOR, "Points");
	    
	    TreeSet keys = new TreeSet(pointMap.keySet());
	    Iterator iter = keys.iterator();
	    VectorPoint prev = null;
	    while (iter.hasNext()) {
	        Integer value = (Integer) iter.next();
		int intValue = value.intValue();
		Integer prevInt = new Integer(intValue - 1);
		Integer nextInt = new Integer(intValue + 1);
		if ((keys.contains(prevInt))&&(keys.contains(nextInt))) {
		    //System.out.println(prevInt + ", " + nextInt);
		    ArrayList prevList = (ArrayList) pointMap.get(prevInt);
		    ArrayList currList = (ArrayList) pointMap.get(value);
		    ArrayList nextList = (ArrayList) pointMap.get(nextInt);
		    VectorPoint c0 = getPoint(prevList, "c ");
		    System.out.println(c0);
		    VectorPoint n1 = getPoint(currList, "n ");
		    VectorPoint ca1 = getPoint(currList, "ca");
		    VectorPoint c1 = getPoint(currList, "c ");
		    VectorPoint n2 = getPoint(nextList, "n ");
		    VectorPoint ramaPoint = calcRama(c0, n1, ca1, c1, n2, prev);
		    //System.out.println(ramaPoint);
		    ramaPoint.setParent(list);
		    list.add(ramaPoint);
		    prev = ramaPoint;
		    
		}
	    }
	    //KList list = new KList(subgroup, "Points");
	    
	    //KList list = (KList) listMap.get(key);
	    //list.flags |= KList.NOHILITE;
	    //list.setType("BALL");
	    list.setHasButton(false);
	    subgroup.add(list);
	    list.setParent(subgroup);
    
	//kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }

    public VectorPoint getPoint(ArrayList list, String atomName) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    VectorPoint point = (VectorPoint) iter.next();
	    String pointAtom = point.getName().substring(0, 4);
	    if (pointAtom.indexOf(atomName) > -1) {
		return point;
	    }
	}
	return null;
    }

    public void analyzeKin(KPoint p) {
	ConnectivityFinder cf = new ConnectivityFinder(kMain);
	cf.buildAdjacencyList();
	HashSet mobilePoints = cf.mobilityFinder(null, (AbstractPoint) p, false);
	Iterator iter = mobilePoints.iterator();
	pointMap = new TreeMap();
	while (iter.hasNext()) {
	    VectorPoint point = (VectorPoint) iter.next();
	    Integer resNumber = new Integer(PointComparator.getResNumber(point.getName()));
	    if (pointMap.containsKey(resNumber)) {
		ArrayList list = (ArrayList) pointMap.get(resNumber);
		if (!list.contains(point)) {
		    list.add(point);
		}
	    } else {
		ArrayList list = new ArrayList();
		list.add(point);
		pointMap.put(resNumber, list);
	    }
	}
	lengthMap = new HashMap();
	Integer first = (Integer) pointMap.firstKey();
	Integer last = (Integer) pointMap.lastKey();
	int length = last.intValue() - first.intValue() + 1;
	Set keys = pointMap.keySet();
	iter = keys.iterator();
	while (iter.hasNext()) {
	    Integer key = (Integer) iter.next();
	    Integer transKey = new Integer((key.intValue() - first.intValue() + 1)*5);
	    lengthMap.put(key,transKey);
	
	//lengthMap.put(first, new Double(1));
	//iter.next(); //first already in lengthMap
	//lengthMap.put(iter.next(), new Double(2));
	//if (length - 4 > 0) {
	//    while (iter.hasNext()) {
		
	//lengthMap.put(
	//if (length < 4) {
	}    
			   
    }
	/*
	Kinemage kin = kMain.getKinemage();
	if (kin != null) kin.setModified(true);
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		Iterator groupIters = group.iterator();
		while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    if (sub.isOn()) {
			while (subIters.hasNext()) {
			    KList list = (KList) subIters.next();
			    if (list.isOn()) {
				Iterator listIter = list.iterator();
				while (listIter.hasNext()) {
				    Object next = listIter.next();
				    if (next instanceof VectorPoint) {
				        addPoints((VectorPoint)next);
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	*/
    
    
    public void addPoints(VectorPoint point) {
	Integer resNumber = new Integer(PointComparator.getResNumber(point.getName()));
	if (pointMap.containsKey(resNumber)) {
	    ArrayList list = (ArrayList) pointMap.get(resNumber);
	    if (!list.contains(point)) {
		list.add(point);
	    }
	} else {
	    ArrayList list = new ArrayList();
	    list.add(point);
	    pointMap.put(resNumber, list);
	}
    }
	
//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return pane; }

    public String getHelpAnchor()
    { return null; }

    public String toString() { return "Rama Tool"; }
//}}}
}//class
