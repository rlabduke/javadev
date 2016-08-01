//{{{ Package, imports
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
//}}}
/**
 * <code>RNAPhosphateFinder</code> takes in a ED map and a KList
 * and finds the highest ED value within the limits of the KList.
 * 
 * <p>It uses a brute force algorithm, basically going through 
 * every value within the limits of the KList, and storing the highest
 * value.  This works for a "phosphate" which typically has maybe 100 values, 
 * but obviously has issues with large polyhedra.  Yeah, big issues. 
 *
 * <p>Copyright (C) 2003 by Vincent Chen. All rights reserved.
 * 
 **/


public class RNAPhosphateFinder {

//{{{ Variable Definitions
//################################################

    CrystalVertexSource map;
    double minX = 10000, minY = 10000, minZ = 10000;
    double maxX, maxY, maxZ;
    double highValue = 0;
    int[] min = new int[3];
    int[] max = new int[3];
    int[] highPoint = new int[3];
    double[] highPointXYZ = new double[3];
//}}}

//{{{ Constructors
//#################################################

    public RNAPhosphateFinder() {
    }

    public RNAPhosphateFinder(CrystalVertexSource map) {
	
	this.map = map;
	//kMain = parent.kMain;
	//kCanvas = parent.kCanvas;
    }
//}}}

//{{{ Methods
//###################################################

//{{{ findLimits
//###################################################
/**
 * This method takes in a KList and iterates through it, 
 * to find the outer limits of the points in the list.
 **/
    public void findLimits(KList trackedList) {
	Iterator iter;
	KPoint listPoint;

	iter = trackedList.iterator();
	for ( ; iter.hasNext(); ) {
	    double origX, origY, origZ;

	    listPoint = (KPoint) iter.next();
	    origX = listPoint.getX();
	    origY = listPoint.getY();
	    origZ = listPoint.getZ();
	    if (origX > maxX) {
		maxX = origX;
	    }
	    if (origY > maxY) {
		maxY = origY;
	    }
	    if (origZ > maxZ) {
		maxZ = origZ;
	    }
	    if (origX < minX) {
		minX = origX;
	    }
	    if (origY < minY) {
		minY = origY;
	    }
	    if (origZ < minZ) {
		minZ = origZ;
	    }
	}
	//System.out.println("mins: " + minX +", " + minY + ", " + minZ);
	//System.out.println("maxs: " + maxX +", " + maxY + ", " + maxZ);
    }

//{{{ findIndexLimits
//###################################################
/**
 * This method just finds the map indicies from the King indicies. 
 **/
    public void findIndexLimits() {
	map.findVertexForPoint(minX, minY, minZ, min);
	map.findVertexForPoint(maxX, maxY, maxZ, max);
	//System.out.println("Min: " + min[0] + ", " + min[1] + ", " + min[2]);
	//System.out.println("Max: " + max[0] + ", " + max[1] + ", " + max[2]);
    }
//}}}

//{{{ findMax
//##################################################
/**
 * This method steps through every index combination within the limits
 * of the polyhedra to find the max value point.
 **/
    public void findMapMax() {
	int[] totalxyz = new int[3];
	double value;
	
	totalxyz[0] = max[0] - min[0];
	totalxyz[1] = max[1] - min[1];
	totalxyz[2] = max[2] - min[2];
	for (int i = 0; i <= totalxyz[0]; i++) {
	    for (int j = 0; j <= totalxyz[1]; j++) {
		for (int k = 0; k <= totalxyz[2]; k++) {
		    //System.out.println("Index: " + (min[0] + i) + " " + (min[1] + j) + " " + (min[2] + k));
		    // Hmmm, I think I have to adjust indexes so origin is at 0.
		    value = map.getValue(min[0] + i - map.aMin, min[1] + j - map.bMin, min[2] + k - map.cMin);
		    //System.out.println(value);
		    if (value > highValue) {
			highValue = value;
			highPoint[0] = min[0] + i;
			highPoint[1] = min[1] + j;
			highPoint[2] = min[2] + k;
		    }
		}
	    }
	}
	map.locateVertex(highPoint[0], highPoint[1], highPoint[2], highPointXYZ);
    } 
//}}}

    public RNATriple findCentroid(KList trackedList) {
	Iterator iter = trackedList.iterator();
	//ArrayList dataPoints = new ArrayList();
	RNATriple centroid = new RNATriple();
	int size = 0;
	while (iter.hasNext()) {
	    KPoint t = (KPoint) iter.next();
	    centroid.add(t);
	    size++;
	}
	centroid.mult(1.0 / size);
	return centroid;
    }

//{{{ highPoint
//###################################################
/**
 * Returns a clone of the double containing the high point.
 **/
    public double[] highMapPoint() {
	double[] hpClone = new double[3];
	hpClone[0] = highPointXYZ[0];
	hpClone[1] = highPointXYZ[1];
	hpClone[2] = highPointXYZ[2];
	return hpClone;
    }
//}}}

//{{{ toString
//##################################################
/** Used for debugging mostly. **/
    public String toString() {
	String returnString;
	returnString = "HighValue = " + highValue + "\n";
	returnString = returnString + 
	    "x=" + highPoint[0] + ", " +
	    "y=" + highPoint[1] + ", " +
	    "z=" + highPoint[2];
	return returnString;
    }
//}}}

}
