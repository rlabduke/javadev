// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;

import king.*;
import king.core.*;

import java.util.*;


public class PointSorter //extends ... implements ...
{
//{{{ Constants
    static final int SORTBY_X = 0;
    static final int SORTBY_Y = 1;
    static final int SORTBY_Z = 2;
//}}}

//{{{ Variable definitions
//##################################################################################################
    List  allPoints;
    int        coordToSort;
    
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */

    public PointSorter(List list, int coord) {
	allPoints = list;
	coordToSort = coord;
    }

//}}}

/**
 * Implements a modified version of counting sort from Intro to Algorithms book.
 * Obviously only works on data sets with integer sorting values.  
 **/
    public Collection sortPhiPsi() {
	//Iterator iter = allPoints.iterator();
	int[] counts = new int[361];
	for (int i = 0; i < counts.length; i++) {
	    counts[i] = 0;
	}
	for (int i = 0; i < allPoints.size(); i++) {
	    KPoint point = (KPoint) allPoints.get(i);
	    int value = (int) (getCoord(coordToSort, point)+180);
	    counts[value] = counts[value]+1;
	}
	for (int i = 1; i < counts.length; i++) {
	    counts[i] = counts[i] + counts[i-1];
	}
// for some stupid reason java doesn't allow me to use the initialcapacity constructor of arraylist.
	ArrayList sortedList = new ArrayList(allPoints);
	//sortedList.ensureCapacity(30947);
	for (int i = allPoints.size()-1; i >= 0; i--) {
	    //System.out.print(".");
	    KPoint point = (KPoint) allPoints.get(i);
	    //System.out.print(getCoord(coordToSort, point));
	    int value = (int) (getCoord(coordToSort, point)+180);
	    //System.out.print(sortedList.size());
	    sortedList.set(counts[value]-1, point);
	    counts[value] = counts[value] - 1;
	}
	return sortedList;
    }

    public double getCoord(int index, KPoint point) {
	if (index == SORTBY_X) {
	    return point.getX();
	} else if (index == SORTBY_Y) {
	    return point.getY();
	} else if (index == SORTBY_Z) {
	    return point.getZ();
	}
	return 0;
    }
}
