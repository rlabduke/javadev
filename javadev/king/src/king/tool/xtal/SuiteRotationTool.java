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




public class SuiteRotationTool extends BasicTool implements ChangeListener, ListSelectionListener, ActionListener, MageHypertextListener {

    //{{{ Constants

    //}}}

//{{{ Variable definitions
//##################################################################################################
    AngleDial rotDial;
    TablePane pane;

    RNATriple firstTrip = null;
    RNATriple secondTrip = null;

    HashMap origMap;
    HashSet origSet;
    HashMap pointMap;

    HashMap rotMap;

    JList rotJList;
    JList hypJList;

    JTextField angField;

    HyperRotParser hyptyp;

    BondRot oldRot = null;
    BondRot hyperRot = null;
    boolean valueChanging = false;
    

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */

    public SuiteRotationTool(ToolBox tb) {
	super(tb);
	buildGUI();

    }

//}}}

    private void buildGUI() {
	rotDial = new AngleDial();
	//rotDial.addChangeListener(this);
	rotJList = new JList();
        //rotJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //rotJList.addListSelectionListener(this);
	//label1 = new JLabel();
	hypJList = new JList();
	//hypJList.addListSelectionListener(this);
	angField = new JTextField("", 5);
	angField.addActionListener(this);

	pane = new TablePane();
	pane.addCell(rotJList);
	pane.addCell(rotDial);
	pane.addCell(hypJList);
	pane.newRow();
	pane.skip();
	pane.addCell(angField);

    }

    public void start() {

	origMap = new HashMap();
	origSet = new HashSet();
	pointMap = new HashMap();
	rotMap = new HashMap();

	Collection brColl = kMain.getKinemage().getBondRots();
	kMain.getTextWindow().addHypertextListener(this);
	hyptyp = new HyperRotParser(kMain.getTextWindow().getText());
	hypJList.setListData(hyptyp.getHypList());
	Iterator bondRotIter = brColl.iterator();
	Iterator listIter;
	ArrayList bondRotList = new ArrayList();
	while (bondRotIter.hasNext()) {

	    BondRot bonds = (BondRot) bondRotIter.next();
	    rotMap.put(bonds.getName(), bonds);
	    bondRotList.add(bonds);

	    ArrayList origList = new ArrayList();
	    ArrayList ptsList = new ArrayList();
	    
	    origMap.put(bonds, origList);
	    pointMap.put(bonds, ptsList);

	    listIter = bonds.iterator();
	    //KList axis = (KList) listIter.next();
	    //storeCoords(axis, ptsList, origList);
	    while (listIter.hasNext()) {
		KList bondList = (KList) listIter.next();
		bondList.setColor(KPalette.white);
		//System.out.println("list color set");
		storeCoords(bondList, ptsList, origList);
	    }

	}
	debugSet();
	// at this point i have 2 hashmaps: 1 with original coordinates of points in an RNATriple,
	// and 1 with "current" coordinates of points in a KPoint.  
	BondRot[] bondRots = new BondRot[bondRotList.size()];
	Iterator iter = bondRotList.iterator();
	for (int i = 0; i < bondRotList.size(); i++) {
	    bondRots[i] = (BondRot) iter.next();
	}
	rotJList.setListData(bondRots);
        rotJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //rotJList.addListSelectionListener(this);
	//debug();
	//pane.addCell(rotJList);
	//pane.addCell(rotDial);

	//SET ROTATION
	show();
	rotDial.addChangeListener(this);
        rotJList.addListSelectionListener(this);
	hypJList.addListSelectionListener(this);
	kCanvas.repaint();
	//debug();
    }

    public void stop() {
	rotDial.removeChangeListener(this);
	rotJList.removeListSelectionListener(this);
	kMain.getTextWindow().removeHypertextListener(this);
	hide();
    }

    private void setAxisVars(KList axis) {
	Iterator iter = axis.iterator();
	firstTrip = new RNATriple((KPoint) iter.next());
	secondTrip = new RNATriple((KPoint) iter.next());

    }

    private void doRotation(BondRot rot) {
	setAxisVars(rot.getAxisList());
	Transform rotate = new Transform();
	//System.out.print(rotDial.getDegrees()-rot.getCurrentAngle() + ", ");
	double degs = rotDial.getDegrees() - rot.getCurrentAngle();
	if (degs < 0) degs = degs + 360;
	System.out.println("transform deg: " + degs);
	rotate = rotate.likeRotation(firstTrip, secondTrip, degs);
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

    //private void doRotation() {
    //	doRotation(oldRot);
    //}

    private void doNoDialRotation(BondRot rot, double angle) {
	setAxisVars(rot.getAxisList());
	Transform rotate = new Transform();
	double degs = angle - rot.getCurrentAngle();
	if (degs < 0) degs = degs + 360;
	System.out.println("transform deg: " + degs);
	rotate = rotate.likeRotation(firstTrip, secondTrip, degs);
	//rotate = rotate.orthonormalize();
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

    public void setAngle(String rotname, double angle) {
	BondRot hyperRot = (BondRot) rotMap.get(rotname);
	if (angle<0) angle = angle + 360;
	if (hyperRot != null) {
	    //double oldAngle = rot.getCurrentAngle();
	    //oldRot = rot;
	    if (hyperRot.getCurrentAngle()!=angle) {
		//rotJList.setSelectedValue(oldRot, false);
		//System.out.println("Setting angle: " + hyperRot + " from " + hyperRot.getCurrentAngle() + " to " + angle + " degrees");
		//rotDial.setDegrees(angle);
	    
		doNoDialRotation(hyperRot, angle);
		updateCoords(hyperRot);
		hyperRot.setCurrentAngle(angle);
		//pause();
	    }
	    kCanvas.repaint();
	    
	}
	//pause();
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
	//System.out.println("updating coords");
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

	if (!valueChanging) {
	    //BondRot rot = (BondRot) rotJList.getSelectedValue();
	    doRotation(oldRot);
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
	    //System.out.println("old rot: " + oldRot);
	    oldRot = null;
	}
	JList hitList = (JList) ev.getSource();
	//System.out.println("Value Changed");
	if (hitList.equals(rotJList)) {
	    //if (oldRot != null) {
	    //	oldRot.setColor(KPalette.white);
	    //	updateCoords(oldRot);
	    //	oldRot.setCurrentAngle(rotDial.getDegrees());
	    //}
	    oldRot = (BondRot) rotJList.getSelectedValue();
	    oldRot.setColor(KPalette.green);
	    oldRot.setAxisColor(KPalette.red);
	    //System.out.println(oldRot);
	    //System.out.println("oldRot orig " + oldRot.getOrigAngle());
	    //System.out.println("oldRot current " + oldRot.getCurrentAngle());
	    rotDial.setOrigDegrees(oldRot.getOrigAngle());
	    rotDial.setDegrees(oldRot.getCurrentAngle());
	    //oldRot.setColor(KPalette.white);
	    System.out.println("RotList hit");
	    kCanvas.repaint();
	} else if (hitList.equals(hypJList)) {
	    //rotJList.clearSelection();
	    String hypName = (String) hypJList.getSelectedValue();
	    ArrayList rots = hyptyp.getRotList(hypName);
	    Iterator iter = rots.iterator();
	    while (iter.hasNext()) {
		BondRot bRot = (BondRot) iter.next();
		//System.out.println("hyp bondRot: " + bRot + ", " + bRot.getCurrentAngle());
		//rotJList.setSelectedValue(rotMap.get(bRot.getName()), false);
		setAngle(bRot.getName(), bRot.getCurrentAngle());
		//kCanvas.repaint();
		//pause();
	    }
	    
	    kCanvas.repaint();
	}
	valueChanging = false;
    }

    public void actionPerformed(ActionEvent ev) {
	//System.out.println("Hello");
	double ang = Double.parseDouble(angField.getText());
	setAngle(((BondRot) rotJList.getSelectedValue()).getName(), ang);
	angField.setText("");
    }

    public void mageHypertextHit(String link) {
	ArrayList list = hyptyp.extractRotInfo(link);
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    BondRot bRot = (BondRot) iter.next();
	    //System.out.println("hyp bondRot: " + bRot + ", " + bRot.getCurrentAngle());
	    //rotJList.setSelectedValue(rotMap.get(bRot.getName()), false);
	    setAngle(bRot.getName(), bRot.getCurrentAngle());
	    //kCanvas.repaint();
	    //pause();
	}
    }

    private void pause() {
	long startTime = System.currentTimeMillis();
	long endTime = 0;
	System.out.println("Pause on");
	//endTime = startTime;
	while (endTime < startTime + 1000) {
	    endTime = System.currentTimeMillis();
	}
	System.out.println("Pause off");
    }

    public void debugSet() {
	Iterator iter = origSet.iterator();
	while (iter.hasNext()) {
	    RNATriple temp = (RNATriple) iter.next();
	    System.out.println(temp);
	}
    }

    public void debug() {

	setAngle("-1 delta", 147.0);
	//doRotation();
	setAngle("-1 c-c4*-o4*-c", -2.25);
	//doRotation();
	setAngle("-1 o-c3*-c2*-o", -38.4);
	//doRotation();
	setAngle("-1 c-o4*-c1*-c", -23.7);
	//doRotation();
	setAngle("-1 epsilon", -100);
	//doRotation();
	setAngle("-1 zeta", 85);
	//doRotation();
	setAngle("alpha", 65);
	//doRotation();
	setAngle("beta", 180);
	//doRotation();
	setAngle("gamma", 55);
	//doRotation();
	setAngle("delta", 84.3);
	//doRotation();
	setAngle("c-c3*-c2*-c", 36.9);
	//doRotation();
	setAngle("c-c2*-c1*-o", -26.2);
	//doRotation();

	/*
	BondRot[] bondRots = new BondRot[dialMap.size()];
	Iterator iter = (dialMap.values()).iterator();
	for (int i = 0; i < dialMap.size(); i++) {
	    bondRots[i] = (BondRot) iter.next();
	}
	rotJList = new JList(bondRots);
        rotJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rotJList.addListSelectionListener(this);
	*/
    }
	    

}//class
