// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.xtal;
import king.*;
import king.core.*;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import javax.swing.event.*;

import driftwood.gui.*;
import driftwood.r3.*;
//}}}




public class SuiteRotationTool extends BasicTool implements ChangeListener, ListSelectionListener {

    //{{{ Constants

    //}}}

//{{{ Variable definitions
//##################################################################################################
    AngleDial rotDial;
    TablePane pane;
    KPoint firstPoint = null;
    KPoint secondPoint = null;
    KPoint thirdPoint = null;
    RNATriple firstTrip = null;
    RNATriple secondTrip = null;
    RNATriple thirdTrip = null;
    //Transform rotate;
    PolygonFinder polyFind;
    KList list;
    HashMap origMap;
    HashSet origSet;
    HashMap pointMap;
    HashMap axisMap;
    //ArrayList origCoords;
    //ArrayList points;
    Iterator listIter;
    //JLabel label1;
    //HashMap dialMap;
    //KinfileRotParser  parser;
    JList rotList;

    BondRot oldRot = null;
    boolean valueChanging = false;
    

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */

    public SuiteRotationTool(ToolBox tb) {
	super(tb);
	list = new KList();
	polyFind = new PolygonFinder();
	//origCoords = new ArrayList();
	//points = new ArrayList();
	origMap = new HashMap();
	origSet = new HashSet();
	pointMap = new HashMap();
	//rotate = new Transform();
	//dialMap = new HashMap();
	axisMap = new HashMap();
	buildGUI();

    }

//}}}

    private void buildGUI() {
	rotDial = new AngleDial();
	rotDial.addChangeListener(this);
	//label1 = new JLabel();

	pane = new TablePane();
	//pane.addCell(rotDial);
       	//pane.newRow();
	//pane.addCell(label1);
    }

    public void start() {
	Iterator bondRotIter = kMain.getKinIO().getBondRotIterator();
	//BondRot bonds = (BondRot) bondRotIter.next();
	//System.out.println(bonds);
	ArrayList bondRotList = new ArrayList();
	while (bondRotIter.hasNext()) {

	    BondRot bonds = (BondRot) bondRotIter.next();
	    bondRotList.add(bonds);
	    //dial = new AngleDial();
	    //dial.addChangeListener(this);
	    //JLabel label = new JLabel();
	    //pane.addCell(label);
	    //pane.addCell(dial);
	    //pane.addCell(rotList);
	    //pane.newRow();
	    //dialMap.put(dial, bonds);

	    ArrayList origList = new ArrayList();
	    ArrayList ptsList = new ArrayList();
	    
	    origMap.put(bonds, origList);
	    pointMap.put(bonds, ptsList);

	    listIter = bonds.iterator();
	    KList axis = (KList) listIter.next();
	    //calcRotation(axis);
	    //axisMap.put(dial, axis);
	    //axis.setColor(KPalette.red);
	    storeCoords(axis, ptsList, origList);
	    while (listIter.hasNext()) {
		KList bondList = (KList) listIter.next();
		bondList.setColor(KPalette.white);
		//System.out.println("list color set");
		storeCoords(bondList, ptsList, origList);
	    }
	    //label.setText(bonds.getName());
	    //dial.setOrigDegrees(bonds.getAngle());
	    //dial.setDegrees(bonds.getAngle());
	}
	// at this point i have 2 hashmaps: 1 with original coordinates of points in an RNATriple,
	// and 1 with "current" coordinates of points in a KPoint.  
	BondRot[] bondRots = new BondRot[bondRotList.size()];
	Iterator iter = bondRotList.iterator();
	for (int i = 0; i < bondRotList.size(); i++) {
	    bondRots[i] = (BondRot) iter.next();
	}
	rotList = new JList(bondRots);
        rotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rotList.addListSelectionListener(this);
	//debug();
	pane.addCell(rotList);
	pane.addCell(rotDial);
	
	//SET ROTATION
	show();
    }


    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if(p instanceof VectorPoint) {
	    /*
	    if (firstPoint == null) {
		firstPoint = p;
		firstTrip = new RNATriple(p);
	    } else if (secondPoint == null) {
		secondPoint = p;
		secondTrip = new RNATriple(p);
		//calcRotation();
	    } else {
		thirdPoint = p;
		thirdTrip = new RNATriple(p);
		list = (KList) thirdPoint.getOwner();
		storeCoords(list);
	    }
	    */
	
	}
    }

    //private void calcRotation() {
    //	rotate = new Transform();
	//rotate = rotate.likeRotation(firstTrip, secondTrip, rotDial.getDegrees());
    //}

    private void calcRotation(KList axis) {
	//rotate = new Transform();
	Iterator iter = axis.iterator();
	firstTrip = new RNATriple((KPoint) iter.next());
	//System.out.println(firstPoint);
	secondTrip = new RNATriple((KPoint) iter.next());
	//System.out.println(secondPoint);
	//rotate = rotate.likeRotation(firstTrip, secondTrip, rotDial.getDegrees());
    }

    private void doRotation(BondRot rot) {
	calcRotation(rot.getAxisList());
	Transform rotate = new Transform();
	rotate = rotate.likeRotation(firstTrip, secondTrip, rotDial.getDegrees() - rotDial.getOrigDegrees());
	Iterator origIter = ((ArrayList) origMap.get(rot)).iterator();
	Iterator pointIter = ((ArrayList) pointMap.get(rot)).iterator();
	while (origIter.hasNext()) {
	    KPoint point = (KPoint) pointIter.next();
	    RNATriple origTrip = (RNATriple) origIter.next();
	    RNATriple trip = (RNATriple) origTrip.clone();
	    rotate.transform(trip);
	    point.setOrigX(trip.getX());
	    point.setOrigY(trip.getY());
	    point.setOrigZ(trip.getZ());
	    //trip = origTrip;
	}
    }

    private void doRotation(AngleDial dial) {
	/*
	//list = (KList) thirdPoint.getOwner();
	Transform rotate = new Transform();

	//KList axis = (KList) axisMap.get(dial);
	//Iterator axisIter = axis.iterator();
	//firstTrip = new RNATriple((KPoint) axisIter.next());
	//System.out.println(firstPoint);
	//secondTrip = new RNATriple((KPoint) axisIter.next());
	
	//rotate = rotate.likeRotation(firstTrip, secondTrip, dial.getDegrees() - dial.getOrigDegrees());
	//BondRot bonds = (BondRot) dialMap.get(dial);
	//bonds.setColor(KPalette.blue);
	
	//while (listIter.hasNext()) {
	//doRotation((KList) listIter.next());
	    //}
	//rotate = rotate.likeRotation(firstTrip, secondTrip, rotDial.getDegrees());
	//storeCoords(list);
	//doRotation(list);

	//RNATriple origTrip = (RNATriple) thirdTrip.clone();
	//rotate.transform(thirdTrip);
	//thirdPoint.setOrigX(thirdTrip.getX());
	//thirdPoint.setOrigY(thirdTrip.getY());
	//thirdPoint.setOrigZ(thirdTrip.getZ());
	//thirdTrip = origTrip;
	//System.out.println(thirdPoint + ", org x: " + thirdPoint.getOrigX());
	//System.out.println(thirdPoint + ", x: " + thirdPoint.getX());
	//list.add(thirdPoint);
	
	Iterator iter = ((ArrayList) origMap.get(bonds)).iterator();
	Iterator pointIter = ((ArrayList) pointMap.get(bonds)).iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) pointIter.next();
	    RNATriple origTrip = (RNATriple) iter.next();
	    RNATriple trip = (RNATriple) origTrip.clone();
	    rotate.transform(trip);
	    point.setOrigX(trip.getX());
	    point.setOrigY(trip.getY());
	    point.setOrigZ(trip.getZ());
	    //trip = origTrip;
	}
	*/
    }

    private void updateCoords(BondRot rot) {
	//need to update origMap with new triples of points...
	ArrayList origList = (ArrayList) origMap.get(rot);
	ArrayList ptsList = (ArrayList) pointMap.get(rot);
	//origList.clear();
	
	Iterator pointIter = ptsList.iterator();
	Iterator origIter = origList.iterator();

	while (pointIter.hasNext()) {
	    KPoint p = (KPoint) pointIter.next();
	    RNATriple trip = (RNATriple) origIter.next();
	    trip.setXYZ(p.getOrigX(), p.getOrigY(), p.getOrigZ());
	    //RNATriple trip = new RNATriple(p);
	    //origList.add(trip);
	}
	
    }
    

    private void storeCoords(KList list, ArrayList ptsList, ArrayList origList) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint p = (KPoint) iter.next();
	    ptsList.add(p);
	    RNATriple trip = new RNATriple(p);
	    if (origSet.contains(trip)) {
		Iterator origIter = origSet.iterator();
		while (origIter.hasNext()) {
		    RNATriple temp = (RNATriple) origIter.next();
		    if (temp.equals(trip)) {
			trip = temp;
		    }
		}
	    }
	    origList.add(trip);
	    origSet.add(trip);
		
	}
	//System.out.print("points size: " + ptsList.size());
	//System.out.println(" origCoords size: " + origList.size());
    }

    //need to add old points to rotation too.
    /*
    private void doRotation(KList list) {
	Iterator iter = origCoords.iterator();
	Iterator pointIter = points.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) pointIter.next();
	    RNATriple origTrip = (RNATriple) iter.next();
	    RNATriple trip = (RNATriple) origTrip.clone();
	    rotate.transform(trip);
	    point.setOrigX(trip.getX());
	    point.setOrigY(trip.getY());
	    point.setOrigZ(trip.getZ());
	    //trip = origTrip;
	}
	}*/

    public void onOpenKin(ActionEvent ev) {

    }

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return pane; }
    
    public String getHelpAnchor()
    { return "#suite-tool"; }
    
    public String toString() { return "Suite Rotation"; }
//}}}

    public void stateChanged(ChangeEvent ev) {

	//System.out.println(rotDial.getDegrees());
	//Set dialSet = dialMap.keySet();
	//Iterator dialIter = dialSet.iterator();
	//while (dialIter.hasNext()) {
	//AngleDial dial = (AngleDial) dialIter.next();
	if (!valueChanging) {
	    BondRot rot = (BondRot) rotList.getSelectedValue();
	    doRotation(rot);
	    //System.out.println("State Changed");
	    //}
	    kCanvas.repaint();
	}
    }

    public void valueChanged(ListSelectionEvent ev) {
	valueChanging = true;
	if (oldRot != null) {
	    oldRot.setColor(KPalette.white);
	    updateCoords(oldRot);
	    oldRot.setCurrentAngle(rotDial.getDegrees());
	}
	oldRot = (BondRot) rotList.getSelectedValue();
	oldRot.setColor(KPalette.green);
	oldRot.setAxisColor(KPalette.red);
	rotDial.setOrigDegrees(oldRot.getOrigAngle());
	rotDial.setDegrees(oldRot.getCurrentAngle());
	//oldRot.setColor(KPalette.white);
	//System.out.println("Value Changed");
	kCanvas.repaint();
	valueChanging = false;
    }

    public void debug() {
	/*
	BondRot[] bondRots = new BondRot[dialMap.size()];
	Iterator iter = (dialMap.values()).iterator();
	for (int i = 0; i < dialMap.size(); i++) {
	    bondRots[i] = (BondRot) iter.next();
	}
	rotList = new JList(bondRots);
        rotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rotList.addListSelectionListener(this);
	*/
    }
	    

}//class
