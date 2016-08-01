// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.bondrot;
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

    JList rotJList;
    JList hypJList;

    JTextField angField;

    HyperRotParser hyptyp;
    BondRotHandler handler;

    BondRot oldRot = null;
    //BondRot hyperRot = null;
    boolean valueChanging = false;
    ArrayList highlightedLists = null;

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
	rotJList = new JList();
	hypJList = new JList();

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
	//buildGUI();
	Collection brColl = kMain.getKinemage().getBondRots();
	if (brColl.size() > 0) {
	    handler = new BondRotHandler(brColl);
	    //kMain.getTextWindow().addHypertextListener(this);
	    hyptyp = new HyperRotParser(kMain.getTextWindow().getText());
	    //hypJList.setListData(hyptyp.getHypList());
	    DefaultListModel hypModel = new DefaultListModel();
	    String[] stringArray = hyptyp.getHypList();
	    //System.out.println(stringArray.length);
	    for (int i = 0; i<stringArray.length; i++) {
		hypModel.addElement(stringArray[i]);
	    }
	    hypJList.setModel(hypModel);
	    
	    //rotJList.setListData(new BondRot[0]);
	    //rotJList.setListData(handler.getBondRotArray());
	    DefaultListModel rotModel = new DefaultListModel();
	    BondRot[] bondRotArray = handler.getBondRotArray();
	    //System.out.println(bondRotArray.length);
	    for (int i = 0; i<bondRotArray.length; i++) {
		rotModel.addElement(bondRotArray[i]);
	    }
	    rotJList.setModel(rotModel);
	    rotJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    
	    highlightedLists = new ArrayList();

	    //SET ROTATION
	    show();
	    rotDial.addChangeListener(this);
	    rotJList.addListSelectionListener(this);
	    hypJList.addListSelectionListener(this);
	    kMain.getTextWindow().addHypertextListener(this);
	} else {
	    
	}


	kCanvas.repaint();
	//debug();
    }

    public void reset() {
	stop();
	if (kMain.getKinemage()!=null) {
	    start();
	}
    }

    public void stop() {
	oldRot = null;
	rotDial.removeChangeListener(this);
	rotJList.removeListSelectionListener(this);
	DefaultListModel model = (DefaultListModel) rotJList.getModel();
	model.clear();
	hypJList.removeListSelectionListener(this);
	model = (DefaultListModel) hypJList.getModel();
	model.clear();
	kMain.getTextWindow().removeHypertextListener(this);
	hide();
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

	    handler.doRotation(oldRot, rotDial.getDegrees());
	    kCanvas.repaint();
	}
    }

    public void valueChanged(ListSelectionEvent ev) {
	valueChanging = true;
	if (oldRot != null) {
	    oldRot.restoreOrigColor();
	    //oldRot.setColor(KPalette.white);
	    /*
	    Iterator iter = highlightedLists.iterator();
	    while (iter.hasNext()) {
		KList list = (KList) iter.next();
		KSubgroup subgroup = (KSubgroup) list.getOwner();
		//subgroup.remove(list);
		list.setOwner(null);
	    }
	    */

	    handler.updateCoords(oldRot);
	    oldRot.setCurrentAngle(rotDial.getDegrees());
	    //System.out.println("old rot: " + oldRot);
	    //oldRot = null;
	}
	JList hitList = (JList) ev.getSource();
	//System.out.println("Value Changed");
	if (hitList.equals(rotJList)) {
	    
	    oldRot = (BondRot) rotJList.getSelectedValue();
	    if (oldRot != null) {
		
		oldRot.setColor(KPalette.green);
		oldRot.setAxisColor(KPalette.red);
		/*
		Iterator iter = oldRot.iterator();
		KList list = (KList) iter.next();
		KList clonedList = (KList) list.clone(true);
		clonedList.setWidth(5);
		clonedList.setColor(KPalette.red);
		KSubgroup subgroup = (KSubgroup) list.getOwner();
		subgroup.add(clonedList);
		clonedList.setOwner(subgroup);
		highlightedLists.add(clonedList);
		while (iter.hasNext()) {
		    list = (KList) iter.next();
		    clonedList = (KList) list.clone(true);
		    clonedList.setWidth(5);
		    clonedList.setColor(KPalette.green);
		    subgroup = (KSubgroup) list.getOwner();
		    subgroup.add(clonedList);
		    clonedList.setOwner(subgroup);
		    highlightedLists.add(clonedList);
		}
		*/
		rotDial.setOrigDegrees(oldRot.getOrigAngle());
		rotDial.setDegrees(oldRot.getCurrentAngle());
		
		//System.out.println("RotList hit");
		kCanvas.repaint();
	    }
	} else if (hitList.equals(hypJList)) {
	    rotJList.clearSelection();
	    rotDial.setDegrees(0);
	    rotDial.setOrigDegrees(0);
	    String hypName = (String) hypJList.getSelectedValue();
	    ArrayList rots = hyptyp.getRotList(hypName);
	    Iterator iter = rots.iterator();
	    while (iter.hasNext()) {
		BondRot bRot = (BondRot) iter.next();		
		if (handler.getBondRot(bRot.getName())!= null) {
		    handler.doRotation(handler.getBondRot(bRot.getName()), bRot.getCurrentAngle());
		    handler.updateCoords(handler.getBondRot(bRot.getName()));
		    handler.getBondRot(bRot.getName()).setCurrentAngle(bRot.getCurrentAngle());
		}
	    }
	    
	    kCanvas.repaint();
	}
	valueChanging = false;
    }

    public void actionPerformed(ActionEvent ev) {
	//System.out.println("Hello");
	valueChanging = true;
	double ang = Double.parseDouble(angField.getText());
	handler.doRotation(oldRot, ang);
	handler.updateCoords(oldRot);
	oldRot.setCurrentAngle(ang);
	rotDial.setDegrees(oldRot.getCurrentAngle());
	angField.setText("");
	valueChanging = false;
	kCanvas.repaint();
    }

    public void mageHypertextHit(String link) {
	ArrayList list = hyptyp.extractRotInfo(link);
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    BondRot bRot = (BondRot) iter.next();
	    handler.doRotation(handler.getBondRot(bRot.getName()), bRot.getCurrentAngle());
	    handler.updateCoords(handler.getBondRot(bRot.getName()));
	    
	    kCanvas.repaint();
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
	//Iterator iter = origSet.iterator();
	//while (iter.hasNext()) {
	//    RNATriple temp = (RNATriple) iter.next();
	//    System.out.println(temp);
	//}
    }

    public void debug() {
	/*
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
	*/
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
