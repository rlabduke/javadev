//{{{ Package, imports
package king.tool.rnaxtal;
import king.*;
import king.core.*;

import java.util.*;
import driftwood.isosurface.*;
import driftwood.r3.*;
//}}}
/**
 * <code>PolygonFinder</code> takes in a KList of points and
 * sorts the KList into polyhedra, based on the connectedness 
 * of the points.  It stores the polyhedra in a hashset, allowing the
 * polyhedra to be efficiently retrieved and used (e.g. for highlighting a 
 * polyhedra on the screen).
 *
 * <p>Copyright (C) 2003 by Vincent Chen. All rights reserved.
 * 
 **/

public class PolygonFinder {

//{{{ Variable Definitions
//################################################

    HashMap hMap, polyMap;
    boolean initialized = false;
//}}}

//{{{ Constructors
//###############################################

    /**
     * Constructor
     **/

    public PolygonFinder() {
	hMap = new HashMap();
	polyMap = new HashMap();
    }
//}}}

//{{{ Methods
//#################################################

//{{{ initiateMap
//#################################################
/**
 * This method takes in a KList containing vectorpoints and
 * sorts the points into a hashmap, where the keys are the 
 * points themselves, and the values are hashsets containing
 * all the points connected to that particular key.
 *
 * <p> For each vectorpoint in the KList, there are four possibilities
 * depending on whether or not the point has been encountered by the algorithm, 
 * and whether or not it is a break.  This program takes advantage of the fact
 * that in HashSets, many keys can map to the same value.
 *
 * @param trackedOrg   the KList to be sorted.
 **/
    public void initiateMap(KList trackedOrg) {
	Iterator iter;
	VectorPoint listPoint;
	HashSet priorSet, breakSet;
	KList trackedList;

	priorSet = new HashSet();
	breakSet = new HashSet();
	System.out.println("Map Initiation started");
	trackedList = cloneList(trackedOrg);
	//System.out.println(listDebugger(trackedList));
	iter = trackedList.iterator();
	for ( ; iter.hasNext(); ) {
	    listPoint = (VectorPoint) iter.next();
	    if(hMap.containsKey(listPoint)) { //old point
		if (listPoint.isBreak()) {                       
		    // old point and break:  retrieve the mapping of the point.
		    breakSet = (HashSet) hMap.get(listPoint);
		} else {                                         
		    // old point and not break:  retrieve mapping of point, add
		    // all points of the current set, and update mappings for all
		    // points of the old set.
		    priorSet = (HashSet) hMap.get(listPoint);
		    priorSet.addAll(breakSet);
		    for (Iterator breakIter = breakSet.iterator(); breakIter.hasNext(); ) {
			VectorPoint setPoint = (VectorPoint) breakIter.next();
			hMap.put(setPoint, priorSet);
		    }
		    breakSet = priorSet;
		    //hMap.put(listPoint, priorSet);
		}
	    } else { // new point
		if(listPoint.isBreak()) {                        
		    // new point and break:  make a new hashset with the point, and
		    // put it in the map.
		    breakSet = new HashSet();
		    breakSet.add(listPoint);
		    hMap.put(listPoint, breakSet);
		} else {                                         
		    // new point and not break:  add the point to the current set, and
		    // put it in the map.
		    breakSet.add(listPoint);
		    hMap.put(listPoint, breakSet);
		}
	    }
	}
	//System.out.println("Set: " + hMap.values());
	finishMap(trackedList);
    }
//}}}

    public void initiateMap(KList trackedOrg, VectorPoint break1, VectorPoint break2) {
	Iterator iter;
	VectorPoint listPoint;
	HashSet priorSet, breakSet;
	KList trackedList;

	priorSet = new HashSet();
	breakSet = new HashSet();
	System.out.println("Map Initiation started");
	trackedList = cloneList(trackedOrg);
	//System.out.println(listDebugger(trackedList));
	iter = trackedList.iterator();
	for ( ; iter.hasNext(); ) {
	    listPoint = (VectorPoint) iter.next();
	    if(hMap.containsKey(listPoint)) { //old point
		if (listPoint.isBreak()) {                       
		    // old point and break:  retrieve the mapping of the point.
		    breakSet = (HashSet) hMap.get(listPoint);
		} else {                                         
		    // old point and not break:  retrieve mapping of point, add
		    // all points of the current set, and update mappings for all
		    // points of the old set.
		    priorSet = (HashSet) hMap.get(listPoint);
		    priorSet.addAll(breakSet);
		    for (Iterator breakIter = breakSet.iterator(); breakIter.hasNext(); ) {
			VectorPoint setPoint = (VectorPoint) breakIter.next();
			hMap.put(setPoint, priorSet);
		    }
		    breakSet = priorSet;
		    //hMap.put(listPoint, priorSet);
		}
	    } else { // new point
		if(listPoint.isBreak()||listPoint.equals(break1)||listPoint.equals(break2)) {                        
		    // new point and break:  make a new hashset with the point, and
		    // put it in the map.
		    breakSet = new HashSet();
		    breakSet.add(listPoint);
		    hMap.put(listPoint, breakSet);
		} else {                                         
		    // new point and not break:  add the point to the current set, and
		    // put it in the map.
		    breakSet.add(listPoint);
		    hMap.put(listPoint, breakSet);
		}
	    }
	}
	//System.out.println("Set: " + hMap.values());
	finishMap(trackedList);
    }
//}}}

//{{{ cloneList, clonePoint
//##########################################################
/**
 * Functions to create a clone of a KList or a VectorPoint.
 *
 * <p>Used because King used to overwrite the points in kin files
 * when the files were saved, instead of only adding the list of 
 * selected polyhedra to the end of the file.
 *
 * <p>Probably unnecessary as of 07 Jan 04, but aren't hurting anything.
 **/
    private KList cloneList(KList trackedList) {
	KList listClone = new KList(trackedList.getType());
	Iterator iter;
	VectorPoint listPoint, pointClone, prev;
	
	prev = null;
	iter = trackedList.iterator();
	for ( ; iter.hasNext(); ) {
	    listPoint = (VectorPoint) iter.next();
	    if (listPoint.isBreak()) {
		pointClone = clonePoint(listPoint, listClone, null);
	    } else {
		pointClone = clonePoint(listPoint, listClone, prev);
	    }
	    listClone.add(pointClone);
	    prev = pointClone;
	}
	return listClone;
    }

    private VectorPoint clonePoint(VectorPoint pointOrg, KList owner, VectorPoint start) {
	VectorPoint pointClone;
	
	pointClone = new VectorPoint(owner, pointOrg.getName(), start);
	pointClone.setOrigX((float) pointOrg.getOrigX());
	pointClone.setOrigY((float) pointOrg.getOrigY());
	pointClone.setOrigZ((float) pointOrg.getOrigZ());
	return pointClone;
    }
//}}}

//((( finishMap
//###############################################################
/**
 * This function is the second half of the polyhedra sorting program.
 * After sorting the points into hashsets, this function runs through the KList
 * again and creates KLists containing the points in each polyhedra, in order as
 * given in the KList.  These KLists are stored in a HashMap, with points as keys,
 * to allow efficient access.
 * 
 * @param trackedList   the KList to be sorted.
 **/
    private void finishMap(KList trackedList) {
	HashSet shapeSet = new HashSet();
	KList shapeList = new KList(trackedList.getType());
	VectorPoint listPoint;
	Iterator iter;
	HashSet polyVertSet = new HashSet(hMap.values()); // Hashset of the vertex-containing Hashsets.

	//HashSet vertSet // variable to hold each 
	System.out.println("Number of polys: " + polyVertSet.size());
	//hashMapDebug(hMap.values());
	
	Iterator setIter = polyVertSet.iterator();
	for ( ; setIter.hasNext(); ) {
	    shapeSet = (HashSet) setIter.next(); 
	    shapeList = new KList(trackedList.getType()); // make new KList
	    polyMap.put(shapeSet, shapeList); // Store KList with the vertex-containing hashsets as keys.
	}

	System.out.println("Finish it!");
	iter = trackedList.iterator();
	for ( ; iter.hasNext(); ) {

	    listPoint = (VectorPoint) iter.next();
	    shapeSet = (HashSet) hMap.get(listPoint); // Use point to get the Hashset it's in.
	    shapeList = (KList) polyMap.get(shapeSet); // Use Hashset to look up KList for the point.
	    shapeList.add(listPoint); // Add point to KList.
	    listPoint.setParent(shapeList);
	}
	initialized = true;
	System.out.println("Polyhedra stored");
    }
//)))

//((( getPolyhedra
//###############################################################
/**
 * This function is called with a KPoint and returns the KList of 
 * the polyhedra that contains that point.
 * 
 * @param listPoint   the point to use to get the polyhedra KList.
 **/
    public KList getPolyhedra(KPoint listPoint) {
	
	Iterator iter;
	VectorPoint changePoint;
	HashSet shapeSet = (HashSet) hMap.get((VectorPoint) listPoint);
	KList polyList = (KList) polyMap.get(shapeSet);
	System.out.println("Polyhedra gotten");
	return polyList;
    }
//}}}

//{{{ isInitialized
//###########################################
/**
 * Returns whether this program has been initialized.
 * N.B. This does not necessarily mean that the program has
 * been initialized for the point been tested.
 **/
    public boolean isInitialized() {
	return initialized;
    }
//}}}

//{{{ Debuggers
//#############################################
/**
 * Debugging programs used by VBC
 **/
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
    
    private void hashMapDebug(Collection c) {
	Iterator iter = c.iterator();
	System.out.println("hMap value sizes:");
	for ( ; iter.hasNext(); ) {
	    HashSet values = (HashSet) iter.next();
	    System.out.print(values.size() + ", ");
	}
    }
//}}}
}
