package king.tool.xtal;

import king.*;
import king.core.*;

import java.util.*;
import driftwood.r3.*;
import java.text.*;

public class BondRotHandler {
    
    
    DecimalFormat df2 = new DecimalFormat("0.00");
    
    HashMap origMap;
    HashSet origSet;
    HashMap pointMap;

    HashMap rotMap;

    BondRot[] bondRotArray = null;

    public BondRotHandler(Collection brColl) {
	origMap = new HashMap();
	origSet = new HashSet();
	pointMap = new HashMap();
	rotMap = new HashMap();
	
	Iterator bondRotIter = brColl.iterator();
	Iterator listIter;
	int i = 0;
	bondRotArray = new BondRot[brColl.size()];
	//ArrayList bondRotList = new ArrayList();
	while (bondRotIter.hasNext()) {

	    BondRot bonds = (BondRot) bondRotIter.next();
	    //System.out.println(bonds);
	    bondRotArray[i] = bonds;
	    i++;
	    rotMap.put(bonds.getName(), bonds);
	    
	    ArrayList origList = new ArrayList();
	    ArrayList ptsList = new ArrayList();
	    
	    origMap.put(bonds, origList);
	    pointMap.put(bonds, ptsList);
	    
	    listIter = bonds.iterator();
	    while (listIter.hasNext()) {
		KList bondList = (KList) listIter.next();
		bondList.setColor(KPalette.white);
		storeCoords(bondList, ptsList, origList);
	    }
	    //origSet.clear();
	}

	//BondRot[] bondRots = new BondRot[brColl.size()];
	//Iterator iter = bondRotList.iterator();
	//for (int i = 0; i < bondRotList.size(); i++) {
	//    bondRots[i] = (BondRot) iter.next();
	//}
    }

    private void storeCoords(KList list, ArrayList ptsList, ArrayList origList) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint p = (KPoint) iter.next();
	    ptsList.add(p);
	    RNATriple trip = new RNATriple(p);
	    if (origSet.contains(trip)) {
		Iterator origIter = origSet.iterator();
		while (origIter.hasNext()) {
		    RNATriple temp = (RNATriple) origIter.next();
		    if (temp.equals(trip)) {
			trip = temp;
		    }
		}
	    }
	    origList.add(trip);
	    origSet.add(trip);
		
	}
	//System.out.print("points size: " + ptsList.size());
	//System.out.println(" origCoords size: " + origList.size());
    }

    public void doRotation(BondRot rot, double angle) {
	if (rot != null) {
	    KList axis = rot.getAxisList();
	    Iterator axisIter = axis.iterator();
	    RNATriple firstTrip = new RNATriple((KPoint) axisIter.next());
	    RNATriple secondTrip = new RNATriple((KPoint) axisIter.next());
	    
	    // calc transform
	    Transform rotate = new Transform();
	    rotate = rotate.likeRotation(firstTrip, secondTrip, angle - rot.getCurrentAngle());
	    // apply transform
	    Iterator origIter = ((ArrayList) origMap.get(rot)).iterator();
	    Iterator pointIter = ((ArrayList) pointMap.get(rot)).iterator();
	    //System.out.println(rot + " being set");
	    while (pointIter.hasNext()) {
		KPoint point = (KPoint) pointIter.next();
		//RNATriple origTrip = (RNATriple) origIter.next();
		//RNATriple trip = (RNATriple) origTrip.clone();
		RNATriple trip = new RNATriple(point);
		rotate.transform(trip);
		//System.out.print(point + " " + df2.format(point.getOrigX()) + ", " + df2.format(point.getOrigY()) + ", " + df2.format(point.getOrigZ()) + " set to: ");
		point.setOrigX(trip.getX());
		point.setOrigY(trip.getY());
		point.setOrigZ(trip.getZ());
		//System.out.println(df2.format(point.getOrigX()) + ", " + df2.format(point.getOrigY()) + ", " + df2.format(point.getOrigZ()));
		//rot.setCurrentAngle(angle);
		
		//trip = origTrip;
		//origTrip.setXYZ(trip.getX(), trip.getY(), trip.getZ());
	    }
	    rot.setCurrentAngle(angle);
	}
	//rot.setCurrentAngle(angle);

    }

    public void updateCoords(BondRot rot) {
	//need to update origMap with new triples of points...
	ArrayList origList = (ArrayList) origMap.get(rot);
	ArrayList ptsList = (ArrayList) pointMap.get(rot);
	//origList.clear();
	
	Iterator pointIter = ptsList.iterator();
	Iterator origIter = origList.iterator();

	while (pointIter.hasNext()) {
	    KPoint p = (KPoint) pointIter.next();
	    RNATriple trip = (RNATriple) origIter.next();
	    trip.setXYZ(p.getOrigX(), p.getOrigY(), p.getOrigZ());
	    //RNATriple trip = new RNATriple(p);
	    //origList.add(trip);
	}
	//System.out.println("updating coords");
    }

    public BondRot getBondRot(String name) {
	return ((BondRot) rotMap.get(name));
    }

    public BondRot[] getBondRotArray() {
	return bondRotArray;
    }

}
