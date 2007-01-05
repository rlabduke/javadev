// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;

import king.core.*;

import driftwood.r3.*;
import java.util.*;
import java.text.DecimalFormat;

//}}}
/**
* 
* 
* 
*
* <p>Copyright (C) 2004 Vincent Chen. All rights reserved.
* <br>Begun on Apr 09 2004.
*/

public class RNAPlanePlotter {

//{{{ Constants
    DecimalFormat df2 = new DecimalFormat("0.00");
//}}}

//{{{ Variable definitions
//##################################################################################################
    KList       list;
    //CrystalVertexSource map;
    //RNAPolygonTracker  polyTracker;
    PlaneFinder basePlane;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public RNAPlanePlotter() {

	list = new KList(KList.TRIANGLE);
        list.setName("Plain");
        list.setWidth(5);
	//list.setColor(KPalette.deadwhite);

	//this.map = map;
	
	//polyTracker = new RNAPolygonTracker();
	//basePlane = new PlaneFinder();
    }
//}}}

    public void getPlane(KPoint p) {
	KList parentList = (KList) p.getOwner();
	list.setColor(parentList.getColor());
	Iterator iter = parentList.iterator();
	ArrayList data = new ArrayList();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    data.add(point);
	}
	basePlane = new PlaneFinder();
	basePlane.fitPlane(data);
	//basePlane.drawNormal();
	basePlane.drawPlane(3);
	this.list = basePlane.getList();
    }

    public void getBase(KPoint p) {
    }

    public RNATriple getNormal() {
	return basePlane.getNormal();
    }

    public RNATriple getAnchor() {
	return basePlane.getAnchor();
    }

    public RNATriple getPointPlaneIntersect(KPoint p) {
	double dist = basePlane.calcPointPlaneDistance(p);
	RNATriple newNormal = basePlane.getNormal().mult(-dist);
	return newNormal.add(p);
    }

    public KList getList() {
	//System.out.println("list called");
	return list;
    }

    public void debug() {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    System.out.println(point);
	}
    }
}


