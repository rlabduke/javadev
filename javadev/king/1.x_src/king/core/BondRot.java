// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;
import king.*;

//import java.awt.*;
//import java.awt.event.*;
//import java.io.*;
//import java.net.*;
//import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.gui.*;
//import driftwood.util.SoftLog;
//}}}
/**
 * <code>BondRot</code> is an object to represent a BondRot for doing bond rotations like in Mage.
 * 
 * <p>Copyright (C) 2004 Vincent B. Chen. All rights reserved.
 * <br>Begun in June 2004
 **/
public class BondRot {

//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    int bondRotNum = -1;
    ArrayList bondLists = null;
    boolean isOpen = false;
    String name = null;
    double origAng = 0;
    double currAng = 0;
    HashMap colorMap = null;

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public BondRot(int rotNum) {
	bondLists = new ArrayList();
	bondRotNum = rotNum;
	isOpen = true;
	colorMap = new HashMap();
    }
//}}}

    /**
     * Constructor
     **/
    public BondRot(int rotNum, String nm, double angle) {
	bondLists = new ArrayList();
	bondRotNum = rotNum;
	name = nm;
	origAng = angle;
	currAng = angle;
	isOpen = true;
	colorMap = new HashMap();
    }

//{{{ Methods
//##################################################################################################

//{{{ Add
//##################################################################################################
    /**
     * Adds a KList to this bondrot.
     **/
    public void add(KList list) {
	bondLists.add(list);
	colorMap.put(list, list.getColor());
    }
//{{{

    public void setOpen(boolean status) {
	isOpen = status;
    }

    public void setColor(KPaint color) {
	Iterator iter = bondLists.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    list.setColor(color);
	}
    }

    public void setAxisColor(KPaint color) {
	Iterator iter = bondLists.iterator();
	KList list = (KList) iter.next();
	list.setColor(color);
    }

    public void setCurrentAngle(double ang) {
	currAng = ang;
    }

    public void restoreOrigColor() {
	Iterator iter = bondLists.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    KPaint color = (KPaint) colorMap.get(list);
	    list.setColor(color);
	}
    }

    public Iterator iterator() {
	return bondLists.iterator();
    }

    public KList getAxisList() {
	return (KList) bondLists.get(0);
    }

    public double getOrigAngle() {
	return origAng;
    }
    
    public double getCurrentAngle() {
	return currAng;
    }

    public String getName() {
	return name;
    }

    public boolean isOpen() {
	return isOpen;
    }

    public String toString() {
	//return ("BondRot " + bondRotNum + ", Contains: " + bondLists.size() + " lists");
	return name;
    }

    public boolean equals(Object obj) {
	if (obj == null || !(obj instanceof BondRot)) return false;
	BondRot rot = (BondRot) obj;
	if (name.equals(rot.getName())) return true;
	else return false;
    }

    public int hashCode() {
	return name.hashCode();
    }

    
}
