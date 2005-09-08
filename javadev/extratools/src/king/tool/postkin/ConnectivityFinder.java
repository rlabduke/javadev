// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;

import java.util.*;

public class ConnectivityFinder {

//{{{ Variable definitions
//##############################################################################
    HashMap adjacencyMap;
    HashSet mobilePoints;
    KingMain kMain;

//}}}	
	
//{{{ Constructor
    /**
       Basic constructor for ConnectivityFinder
    */
    public ConnectivityFinder(KingMain kMain) {
	this.kMain = kMain;
    }
//}}}

//{{{ buildAdjacencyList
    public void buildAdjacencyList() {
	adjacencyMap = new HashMap();
	Kinemage kin = kMain.getKinemage();
	if (kin != null) kin.setModified(true);
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		Iterator groupIters = group.iterator();
		while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    if (sub.isOn()) {
		    while (subIters.hasNext()) {
			KList list = (KList) subIters.next();
			if (list.isOn()) {
			    Iterator listIter = list.iterator();
			    while (listIter.hasNext()) {
				Object next = listIter.next();
				if (next instanceof VectorPoint) {
				    VectorPoint currPoint = (VectorPoint) next;
				    
				    if ((!currPoint.isBreak())&&(currPoint.isOn())) {
					VectorPoint prevPoint = (VectorPoint) currPoint.getPrev();
					addPoints(prevPoint, currPoint);
					addPoints(currPoint, prevPoint);
				    }
				}
			    }
			}
		    }
		    }
		}
	    }
	}
    }
//}}}	
	
//{{{ addPoints
    private void addPoints(VectorPoint prev, VectorPoint curr) {
	if (adjacencyMap.containsKey(prev)) {
	    HashSet prevSet = (HashSet) adjacencyMap.get(prev);
	    prevSet.add(curr);
	} else {
	    HashSet prevSet = new HashSet();
	    prevSet.add(curr);
	    adjacencyMap.put(prev, prevSet);
	}
    }
//}}}
	
//{{{ mobilityFinder
    public HashSet mobilityFinder(AbstractPoint first, AbstractPoint second, boolean onePoint) {

	    
	Set keys = adjacencyMap.keySet();
	Iterator iter = keys.iterator();
	HashMap colors = new HashMap();
	mobilePoints = new HashSet();
	if (onePoint) {
	    mobilePoints.add(clonePoint(second));
	    HashSet set = (HashSet) adjacencyMap.get(second);
	    iter = set.iterator();
	    while (iter.hasNext()) {
		AbstractPoint adjPoint = (AbstractPoint) iter.next();
		HashSet adjSet = (HashSet) adjacencyMap.get(adjPoint);
		if (adjSet.size() == 1) {
		    mobilePoints.add(clonePoint(adjPoint));
		}
	    }
	
	} else {
	    while (iter.hasNext()) {
		Object key = iter.next();
		colors.put(key, KPalette.white);
	    }
	    colors.put(second, KPalette.gray);
	    colors.put(first, KPalette.deadblack);
	    LinkedList queue = new LinkedList();
	    queue.addFirst(second);
	    mobilePoints.add(clonePoint(second));
	    while (!queue.isEmpty()) {
		AbstractPoint point = (AbstractPoint) queue.getFirst();
		queue.removeFirst();
		HashSet adjSet = (HashSet) adjacencyMap.get(point);
		Iterator adjIter = adjSet.iterator();
		while (adjIter.hasNext()) {
		    AbstractPoint adjPoint = (AbstractPoint) adjIter.next();
		    if (colors.get(adjPoint).equals(KPalette.white)) {
			colors.put(adjPoint, KPalette.gray);
			mobilePoints.add(clonePoint(adjPoint));
			queue.addLast(adjPoint);
		    }
		}
		colors.put(point, KPalette.deadblack);
	    }
	}
	return mobilePoints;
    }
//}}}

//{{{ clonePoint
    private Object clonePoint(AbstractPoint point) {
	VectorPoint pointClone = new VectorPoint(null, point.getName(), null);
	pointClone.setX((float) point.getX());
	pointClone.setY((float) point.getY());
	pointClone.setZ((float) point.getZ());
	return pointClone;
    }
//}}}

}//}}}
//class

