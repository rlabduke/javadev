package king.tool.rnaxtal;
import king.*;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.isosurface.*;
import driftwood.r3.*;


/**
 * Started 6 November 2003 by Vincent Chen
 **/

public class RNALineTracker {//implements TransformSignalSubscriber  {

    //Variable Definitions
    
    KingMain    kMain;
    KinCanvas   kCanvas;
    ToolBox     parent;

    Stack storedVertices;
    KList trackedList;

    // Constructors

    public RNALineTracker(ToolBox parent) {

	this.parent     = parent;
        kMain           = parent.kMain;
        kCanvas         = parent.kCanvas;

	//parent.sigTransform.subscribe(this);

	storedVertices = new Stack();
	trackedList = new KList();
	trackedList.setName("test list");
	trackedList.setType(KList.VECTOR);
	//trackedList.setColor(KPalette.gold);
	//trackedList.setWidth(7);

    }


    // Methods

    public void startTracking(VectorPoint startPoint) {
	KList ownerList, tempTrackList;
	ListIterator iter;
	VectorPoint listPoint;

	ownerList = (KList) startPoint.getOwner();
	tempTrackList = new KList();
	iter = ownerList.iterator();
	for ( ; iter.hasNext(); ) {
	    listPoint = (VectorPoint) iter.next();
	    if (listPoint.isBreak()) {
		if (contains(tempTrackList, startPoint)) {
		    trackedList = addAll(trackedList, tempTrackList);
		}
		tempTrackList.clear();
	    }
	    tempTrackList.add(listPoint);
	    //System.out.print(listPoint.getDrawingColor().toString());
	}
	if (contains(tempTrackList, startPoint)) { // to check the last group of the kinemage
	    trackedList = addAll(trackedList, tempTrackList);
	}

	onTracked();
    }

    public boolean contains(KList ownerList, VectorPoint startPoint) {
	Iterator iter;
	VectorPoint listPoint;
	
	iter = ownerList.iterator();
	for ( ; iter.hasNext();) {
	    listPoint = (VectorPoint) iter.next();
	    if (listPoint.equals(startPoint)) {
		return true;
	    }
	}
	return false;
    }
	

    public void onTracked() {

	Kinemage kin = kMain.getKinemage();
	KSubgroup subGroup;
	KList oldList, newList;

	KGroup group = findGroup(kin, "tracker Group");
	if (group == null) {
	    group = new KGroup(kin, "tracker Group");
	    kin.add(group);
	    subGroup = new KSubgroup(group, "tracker Subgroup");
	    subGroup.setHasButton(false);
	    group.add(subGroup);
	    trackedList.setOwner(subGroup);
	    subGroup.add(trackedList);
	} else {
	    subGroup = findSubgroup(group, "tracker Subgroup");
	    oldList = findKList(subGroup, "test list");
	    subGroup.remove(oldList);
	    newList = addAll(oldList, trackedList);
	    newList.setOwner(subGroup);
	    subGroup.add(newList);
	}

	System.out.println("");
	System.out.println("");
	System.out.println("tl: " + listDebugger(trackedList));

	//trackedList.setOwner(subgroup);
	//subgroup.add(trackedList);
     

	//subgroup.add(trackedList);
	//setID("tracking is going?");
	//System.out.println("on tracking...");
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	//kCanvas.repaint();
    }

    private KGroup findGroup(Kinemage kin, String groupName) {
	Iterator iter = kin.iterator();
	KGroup kinGroup;

	for ( ; iter.hasNext(); ) {
	    kinGroup = (KGroup) iter.next();
	    if (kinGroup.getName().equals(groupName)) {
		return kinGroup;
	    }
	}
	return null;
    }
    
    private KSubgroup findSubgroup(KGroup group, String subgroupName) {
	Iterator iter = group.iterator();
	KSubgroup kinSubgroup;

	for ( ; iter.hasNext(); ) {
	    kinSubgroup = (KSubgroup) iter.next();
	    if (kinSubgroup.getName().equals(subgroupName)) {
		return kinSubgroup;
	    }
	}
	return null;
    }

    private KList findKList(KSubgroup subGroup, String listName) {
	Iterator iter = subGroup.iterator();
	KList sgList;

	for ( ; iter.hasNext(); ) {
	    sgList = (KList) iter.next();
	    if (sgList.getName().equals(listName)) {
		return sgList;
	    }
	}
	return null;
    }

    private String listDebugger(KList dList) {
	Iterator iter;
	VectorPoint listPoint;
	String totalString = "";
	
	iter = dList.iterator();
	for ( ; iter.hasNext(); ) {
	    listPoint = (VectorPoint) iter.next();
	    totalString = totalString + listPoint.toString() + ", ";
	}
	return totalString;
    }

    private KList addAll(KList startList, KList lastList) {
	Iterator iter;
	VectorPoint listPoint;

	iter = lastList.iterator();
	for ( ; iter.hasNext(); ) {
	    listPoint = (VectorPoint) iter.next();
	    listPoint.setColor(KPalette.gold);
	    listPoint.setWidth(5);
	    startList.add(listPoint);
	}
	return startList;

    }
    /**
    public void signalTransform(Engine engine, Transform xform)
    {
        //KList list;
        //if(centerChanged()) updateMesh();
	//System.out.println("signal transformed");
        
        //list = trackedList;
        if(trackedList != null)
        {
            //System.out.println(list.toString());
	    trackedList.setColor(KPalette.gold);
            trackedList.signalTransform(engine, xform);
        }
        
        
        //SoftLog.err.println("Painted maps.");
    }
    **/

}


    /**
    private void breakTester(VectorPoint startPoint) {
       
       if(startPoint.isBreak()) {
          trackedList.add(startPoint);
	    for (int i = 0; storedVertices.size(); i++;) {
	    trackedList.add((VectorPoint) storedVertices.pop());
		}
	    breakTester((VectorPoint) iter.next());
	    } else {
	storedVertices.push(startPoint); // adds the point to the top of the stack
	    breakTester((VectorPoint) startPoint.getPrev());
	    }
	}
    **/
    

    

