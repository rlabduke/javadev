// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.points.*;

import java.util.*;
import driftwood.r3.*;
import driftwood.moldb2.*;
import java.text.*;
//}}}

public class ConnectivityFinder {
  
  //{{{ Constants
  static DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variable definitions
  //##############################################################################
  HashMap adjacencyMap;
  HashSet mobilePoints;
  KingMain kMain;
  boolean keepAll;
  //}}}	
	
  //{{{ Constructor
  /**
  Basic constructor for ConnectivityFinder
  */
  public ConnectivityFinder(KingMain kMain) {
    this.kMain = kMain;
    keepAll = false;
  }
  //}}}
  
  //{{{ buildAdjacencyList
  public void buildAdjacencyList() {
    Kinemage kin = kMain.getKinemage();
    if (kin != null) kin.setModified(true);
    buildAdjacencyList(kin, false);
  }
  
  public void buildAdjacencyList(boolean useAllLists) {
    //adjacencyMap = new HashMap();
    buildAdjacencyList(kMain.getKinemage(), useAllLists);
  }
  
  private void buildAdjacencyList(AGE target, boolean useAllLists) {
    adjacencyMap = new HashMap();
  //  if (target instanceof KList) {
	//    //KList list = (KList) target;
	//    if (target.isOn()||useAllLists) {
  //      Iterator iter = target.iterator();
  //      while (iter.hasNext()) {
  //        KPoint pt = (KPoint) iter.next();
  //        if (pt instanceof VectorPoint) {
  //          VectorPoint currPoint = (VectorPoint) pt;
  //          
  //          if ((!currPoint.isBreak())&&((currPoint.isOn())||useAllLists)) {
  //            VectorPoint prevPoint = (VectorPoint) currPoint.getPrev();
  //            addPoints(prevPoint, currPoint);
  //            addPoints(currPoint, prevPoint);
  //          }
  //        }
  //      }
	//    }
  //  } else {
	//    Iterator iter = target.iterator();
	//    //HashMap adjMap = new HashMap;
	//    while (iter.hasNext()) {
  //      AGE next = (AGE) iter.next();
  //      if (next.isOn()||useAllLists) {
  //        buildAdjacencyList(next, useAllLists);
  //      }
	//    }
	//    //return adjMap;
  //  }
  buildAdjacencyMap(target, useAllLists, adjacencyMap);
  }
  //}}}
  
  //{{{ buildAdjacencyMap
  public static void buildAdjacencyMap(AGE target, boolean useAllLists, HashMap adj) {
    //HashMap adj = new HashMap();
    if (target instanceof KList) {
	    //KList list = (KList) target;
	    if (target.isOn()||useAllLists) {
        Iterator iter = target.iterator();
        while (iter.hasNext()) {
          KPoint pt = (KPoint) iter.next();
          if (pt instanceof VectorPoint) {
            VectorPoint currPoint = (VectorPoint) pt;
            
            if ((!currPoint.isBreak())&&((currPoint.isOn())||useAllLists)) {
              VectorPoint prevPoint = (VectorPoint) currPoint.getPrev();
              addPoints(prevPoint, currPoint, adj);
              addPoints(currPoint, prevPoint, adj);
            }
          }
        }
	    }
    } else {
	    Iterator iter = target.iterator();
	    //HashMap adjMap = new HashMap;
	    while (iter.hasNext()) {
        AGE next = (AGE) iter.next();
        if (next.isOn()||useAllLists) {
          buildAdjacencyMap(next, useAllLists, adj);
        }
	    }
	    //return adjMap;
    }
  }
  //}}}
  
  //{{{ addPoints
  private void addPoints(VectorPoint prev, VectorPoint curr) {
    //if (adjacencyMap.containsKey(prev)) {
	  //  HashSet prevSet = (HashSet) adjacencyMap.get(prev);
	  //  prevSet.add(curr);
    //} else {
	  //  HashSet prevSet = new HashSet();
	  //  prevSet.add(curr);
	  //  adjacencyMap.put(prev, prevSet);
    //}
    addPoints(prev, curr, adjacencyMap);
  }
  
  private static void addPoints(VectorPoint prev, VectorPoint curr, HashMap adjMap) {
    if (adjMap.containsKey(prev)) {
	    HashSet prevSet = (HashSet) adjMap.get(prev);
	    prevSet.add(curr);
    } else {
	    HashSet prevSet = new HashSet();
	    prevSet.add(curr);
	    adjMap.put(prev, prevSet);
    }
  }
  //}}}
	
  //{{{ setKeepAll
  public void setKeepAll(boolean val) {
    keepAll = val;
  }
  
  public boolean getKeepAll() {
    return keepAll;
  }
  //}}}
  
  //{{{ mobilityFinder (tuples)
  public static HashSet mobilityFinder(Tuple3 first, Tuple3 second, HashMap adjMap, HashMap atomStates) {
    Set keys = adjMap.keySet();
    Iterator iter = keys.iterator();
    HashMap colors = new HashMap(); // point -> color
    HashMap pointStateMap = new HashMap(); // abstractPoint -> Atom
    //adjMap = abstractPoint -> {abstractPoints....}
    //atomStates = String (formatted triple) -> Triple
    //System.out.println(adjMap);
    HashSet mobile = new HashSet();
    AbstractPoint firstPoint = null;
    AbstractPoint secondPoint = null;
    while (iter.hasNext()) {
      AbstractPoint key = (AbstractPoint) iter.next();
      Triple keyTrip = new Triple(key);
      //keyTrip.format(df);
      //System.out.println(keyTrip.format(df));
      //System.out.println(new Triple(second).format(df));
      if ((keyTrip.format(df)).equals((new Triple(first)).format(df))) firstPoint = key;
      if ((keyTrip.format(df)).equals((new Triple(second)).format(df))) secondPoint = key;
      colors.put(key, KPalette.white);
      pointStateMap.put(key, ((AtomState)atomStates.get(keyTrip.format(df))).getAtom());
    }
    colors.put(secondPoint, KPalette.gray);
    colors.put(firstPoint, KPalette.deadblack);
    LinkedList queue = new LinkedList();
    queue.addFirst(secondPoint);
    mobile.add(pointStateMap.get(secondPoint));
    while (!queue.isEmpty()) {
      AbstractPoint point = (AbstractPoint) queue.getFirst();
      queue.removeFirst();
      HashSet adjSet = (HashSet) adjMap.get(point);
      //System.out.println(point);
      //System.out.println(adjSet);
      Iterator adjIter = adjSet.iterator();
      while (adjIter.hasNext()) {
        AbstractPoint adjPoint = (AbstractPoint) adjIter.next();
        if (colors.get(adjPoint).equals(KPalette.white)) {
          colors.put(adjPoint, KPalette.gray);
          mobile.add(pointStateMap.get(adjPoint));
          queue.addLast(adjPoint);
        }
      }
      colors.put(point, KPalette.deadblack);
    }
    
    return mobile;
  }
  //}}}
  
  //{{{ mobilityFinder (two points)
  /**
  * Finds mobility based on orientation of second in relation to the first.
  * Used for KinFudger to be able to move pieces of the same structure.
  *
  **/
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
  
  //{{{ mobilityFinder (one point)
  /** finds mobility based on only one point, so everything connected to that
  * point is mobile.
  **/
  public HashSet mobilityFinder(AbstractPoint first) {
    Set keys = adjacencyMap.keySet();
    Iterator iter = keys.iterator();
    HashMap colors = new HashMap();
    mobilePoints = new HashSet();
    while (iter.hasNext()) {
	    Object key = iter.next();
	    colors.put(key, KPalette.white);
    }
    colors.put(first, KPalette.gray);
    //colors.put(first, KPalette.deadblack);
    LinkedList queue = new LinkedList();
    queue.addFirst(first);
    mobilePoints.add(clonePoint(first));
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
    return mobilePoints;
  }
  //}}}
  
  //{{{ clonePoint
  private Object clonePoint(AbstractPoint point) {
    VectorPoint pointClone = new VectorPoint(point.getName(), null);
    pointClone.setX((float) point.getX());
    pointClone.setY((float) point.getY());
    pointClone.setZ((float) point.getZ());
    return pointClone;
  }
  //}}}
  
  //{{{ pathFinder
  public ArrayList pathFinder(AbstractPoint first, AbstractPoint second) {
    HashSet adjPoints = (HashSet) adjacencyMap.get(first);
    Iterator iter = adjPoints.iterator();
    HashMap branchLists = new HashMap();
    LinkedList queue = new LinkedList();
    while (iter.hasNext()) {
	    AbstractPoint point = (AbstractPoint) iter.next();
	    ArrayList list = new ArrayList();
	    if (!first.equals(point)) {
        list.add(first);
	    }
	    list.add(point);
	    branchLists.put(point, list);
	    queue.addLast(point);
    }
    HashMap colors = new HashMap();
    Set keys = adjacencyMap.keySet();
    Iterator keysIter = keys.iterator();
    while (keysIter.hasNext()) {
	    Object key = keysIter.next();
	    colors.put(key, KPalette.white);
    }
    colors.put(first, KPalette.deadblack);
    while (!queue.isEmpty()) {
	    AbstractPoint point = (AbstractPoint) queue.getFirst();
	    ArrayList branch = (ArrayList) branchLists.get(point);
	    queue.removeFirst();
	    HashSet adjSet = (HashSet) adjacencyMap.get(point);
	    Iterator adjIter = adjSet.iterator();
	    while (adjIter.hasNext()) {
        AbstractPoint adjPoint = (AbstractPoint) adjIter.next();
        if ((((HashSet) adjacencyMap.get(adjPoint)).size() == 1)&&(!keepAll)) {
          // to eliminate all 1 atom branches (O's, H's, etc)
          colors.put(adjPoint, KPalette.deadblack); 
        }
        if (colors.get(adjPoint).equals(KPalette.white)) {
          branch.add(adjPoint);
          branchLists.put(adjPoint, branch);
          colors.put(adjPoint, KPalette.gray);
          //mobilePoints.add(clonePoint(adjPoint));
          queue.addLast(adjPoint);
        }
        if (adjPoint.equals(second)) {
          return branch;
          //queue.clear();
        }
	    }
	    colors.put(point, KPalette.deadblack);
    }
    return null;
  }
  //}}}

}//}}}
//class

