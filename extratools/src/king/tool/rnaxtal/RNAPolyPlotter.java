// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
import king.*;
import king.core.*;
import king.points.*;

import java.util.*;
import driftwood.isosurface.*;
//}}}
/**
* <code>RNAMapPlotter</code> isn't really a plotter, but
* is named as such to be consistent in RNAMapWindow.
* 
* It is intended to use RNAPhosphateFinder and RNAPolyTracker
* to create a KList of selected polyhedra and their max points.
*
* <p>Copyright (C) 2004 Vincent Chen. All rights reserved.
* <br>Begun on Jan 06 2004.
*/
public class RNAPolyPlotter
{
//{{{ Constants

//}}}

//{{{ Variable definitions
//##################################################################################################
    KList       list;
    CrystalVertexSource map;
    PolygonFinder  polyTracker;
    MarkerPoint phPoint;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public RNAPolyPlotter(CrystalVertexSource map) {

	list = new KList(KList.VECTOR);
        list.setName("Picked Polys");
        list.setWidth(5);

	this.map = map;
	
	polyTracker = new PolygonFinder();

    }
//}}}

//{{{ polyTrack
//################################################
/**
 * This initializes the polytracker, and uses it to add poly lists to
 * the list of stored selected polyhedra.
 * 
 * <p> It also uses polyAnalyze to find the high point within the polyhedra.
 * The program still needs to be modified to allow more than one KList be 
 * initialized. 
 *
 * @param p      VectorPoint to be used to find its polyhedra.
 **/
    public void polyTrack(VectorPoint p) {

	if (!polyTracker.isInitialized()) {
	    //still need to make program check whether polytracker initiated for particular KList
	    polyTracker.initiateMap((KList) p.getParent());
	}
	KList polyList = polyTracker.getPolyhedra(p);
	MarkerPoint phos = polyAnalyze(polyList);
	addAll(list, polyList);

	list.add(phos); // Gives classcastexception if add markerpoint before combining lists.

	//onTracked(polyList);
    }
//}}}


//{{{ polyAnalyze
//################################################
/**
 * This uses the phosphate finder to return a markerpoint showing the
 * highest value of the map within the outerlimits of the input list.
 *
 * @param polyList   The list to be analyzed.
 **/
    public MarkerPoint polyAnalyze(KList polyList) {
	RNAPhosphateFinder phinder = new RNAPhosphateFinder(map);
	//old find map max code
	//phinder.findLimits(polyList);
	//phinder.findIndexLimits();
	//phinder.findMax();
	//System.out.println(phinder);
	//double[] xyz = phinder.highPoint();
	RNATriple centroid = phinder.findCentroid(polyList);
	phPoint = new MarkerPoint("phosphate");
	//phPoint.setX(xyz[0]);
	//phPoint.setY(xyz[1]);
	//phPoint.setZ(xyz[2]);
	phPoint.setX(centroid.getX());
	phPoint.setY(centroid.getY());
	phPoint.setZ(centroid.getZ());
	phPoint.setStyle(MarkerPoint.BOX_L);
	phPoint.setColor(KPalette.green);
	phPoint.setWidth(5);
	phPoint.setName("phosphake: x=" + phPoint.getX() + ", y=" + phPoint.getY() + ", z=" + phPoint.getZ());
	phPoint.setOn(true);
	return phPoint;
    }
//}}}
    
    /** Retrieves the KList containing all selected polyhedra.*/
    public KList getList()
    { return list; }
//}}}

    public VectorPoint getPhosphate() {
	VectorPoint phos = new VectorPoint("phosphate", null);
	phos.setX(phPoint.getX());
	phos.setY(phPoint.getY());
	phos.setZ(phPoint.getZ());
	return phos;
    }

    public void addPoint(RNATriple p) {
	VectorPoint phos = getPhosphate();
	list.add(phos);
	VectorPoint intersect = new VectorPoint("intersect", phos);
	intersect.setX(p.getX());
	intersect.setY(p.getY());
	intersect.setZ(p.getZ());
	intersect.setColor(KPalette.purple);
	System.out.println("Intersect: " + intersect.getX() + ", " + intersect.getY() + ", " + intersect.getZ());
	list.add(intersect);
    }

// addAll
//###################################################
/*
 * Helper function that combines two KLists into one.
 *
 * @param startList  the first list, gets stuff added to it.
 * @param lastList   the second list, stuff added from it.
 */
    private KList addAll(KList startList, KList lastList) {
	Iterator iter;
	VectorPoint listPoint;

	iter = lastList.iterator();
	for ( ; iter.hasNext(); ) {
	    listPoint = (VectorPoint) iter.next();
	    //listPoint.setColor(KPalette.gold);
	    //listPoint.setWidth(5);
	    listPoint.setParent(startList);
	    startList.add(listPoint);
	}
	return startList;

    }



//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

