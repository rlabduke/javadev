// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.tool.util.*;
import driftwood.r3.*;
import driftwood.gui.*;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class GeometryPlugin extends Plugin { 

//{{{ Constants   
    static final DecimalFormat df = new DecimalFormat("0.000");
    //static final DecimalFormat intf = new DecimalFormat("0");
//}}}

//{{{ Variable definitions
//###############################################################
    TreeMap caMap;
    TreeMap oxyMap;
    TreeMap carbMap;
    TreeMap nitMap;
    HashMap pepLength;
    HashMap pepSD;
    HashMap proLength;
    HashMap proSD;
    HashMap glyLength;
    HashMap glySD;
    HashMap pepAng;
    HashMap pepAngSD;
    HashMap proAng, proAngSD;
    HashMap glyAng, glyAngSD;
    KList geomList;

//}}}

    public GeometryPlugin(ToolBox tb) {
	super(tb);
    }
    
    public void onStart(ActionEvent e) {
	caMap = new TreeMap();
	oxyMap = new TreeMap();
	nitMap = new TreeMap();
	carbMap = new TreeMap();
	pepLength = new HashMap();
	pepLength.put("nca", new Double(1.459));
	pepLength.put("cac", new Double(1.525));
	pepLength.put("co", new Double(1.229));
	pepLength.put("cn", new Double(1.336));
	pepSD = new HashMap();
	pepSD.put("nca", new Double(0.020));
	pepSD.put("cac", new Double(0.026));
	pepSD.put("co", new Double(0.019));
	pepSD.put("cn", new Double(0.023));
	proLength = new HashMap();
	proLength.put("nca", new Double(1.468));
	proLength.put("cac", new Double(1.524));
	proLength.put("co", new Double(1.228));
	proLength.put("cn", new Double(1.338));
	proSD = new HashMap();
	proSD.put("nca", new Double(0.017));
	proSD.put("cac", new Double(0.020));
	proSD.put("co", new Double(0.020));
	proSD.put("cn", new Double(0.019));
	glyLength = new HashMap();
	glyLength.put("nca", new Double(1.456));
	glyLength.put("cac", new Double(1.514));
	glyLength.put("co", new Double(1.232));
	glyLength.put("cn", new Double(1.326));
	glySD = new HashMap();
	glySD.put("nca", new Double(0.015));
	glySD.put("cac", new Double(0.016));
	glySD.put("co", new Double(0.016));
	glySD.put("cn", new Double(0.018));
	pepAng = new HashMap();
	pepAng.put("ncac", new Double(111));
	pepAng.put("cacn", new Double(117.2));
	pepAng.put("caco", new Double(120.1));
	pepAng.put("ocn", new Double(122.7));
	pepAng.put("cnca", new Double(121.7));
	pepAngSD = new HashMap();
	pepAngSD.put("ncac", new Double(2.7));
	pepAngSD.put("cacn", new Double(2.2));
	pepAngSD.put("caco", new Double(2.1));
	pepAngSD.put("ocn", new Double(1.6));
	pepAngSD.put("cnca", new Double(2.5));

	proAng = new HashMap();
	proAng.put("ncac", new Double(112.1));
	proAng.put("cacn", new Double(117.1));
	proAng.put("caco", new Double(120.2));
	proAng.put("ocn", new Double(121.1));
	proAng.put("cnca", new Double(119.3)); //trans pro only, dunno how to handle cis yet.
	proAngSD = new HashMap();
	proAngSD.put("ncac", new Double(26));
	proAngSD.put("cacn", new Double(28));
	proAngSD.put("caco", new Double(24));
	proAngSD.put("ocn", new Double(19));
	proAngSD.put("cnca", new Double(15));

	glyAng = new HashMap();
	glyAng.put("ncac", new Double(113.1));
	glyAng.put("cacn", new Double(116.2));
	glyAng.put("caco", new Double(120.6));
	glyAng.put("ocn", new Double(123.2));
	glyAng.put("cnca", new Double(122.3));
	glyAngSD = new HashMap();
	glyAngSD.put("ncac", new Double(25));
	glyAngSD.put("cacn", new Double(20));
	glyAngSD.put("caco", new Double(18));
	glyAngSD.put("ocn", new Double(17));
	glyAngSD.put("cnca", new Double(21));

	KGroup geomGroup = new KGroup(kMain.getKinemage(), "Geometry");
	kMain.getKinemage().add(geomGroup);
	KSubgroup sub = new KSubgroup(geomGroup, "geom");
	geomGroup.add(sub);
	geomList = new KList(sub, "geomlist");
	sub.add(geomList);
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	splitKin(kMain.getKinemage());
	analyze();
    }

    public void splitKin(AGE target) {

	if ((target instanceof KList)&&(target.isOn())) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		if (pt.isOn()) {
		    int resNum = KinUtil.getResNumber(pt);
		    String atomName = KinUtil.getAtomName(pt).toLowerCase();
		    String resName = KinUtil.getResName(pt).toLowerCase();
		    if (atomName.equals("ca")) {
			caMap.put(new Integer(resNum), pt);
		    }
		    if (atomName.equals("o")) {
			oxyMap.put(new Integer(resNum), pt);
		    }
		    if (atomName.equals("n")) {
			nitMap.put(new Integer(resNum), pt);
		    }
		    if (atomName.equals("c")) {
			carbMap.put(new Integer(resNum), pt);
		    }
		}
	    }
	    //System.out.println(caMap.size());
	    //System.out.println(oxyMap.size());
	    //System.out.println(nitMap.size());
	    //System.out.println(carbMap.size());
	} else if (!(target instanceof KList)) {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		AGE targ = (AGE) iter.next();
		if (targ.isOn()) {
		    splitKin(targ);
		}
	    }
	}
    }


    public void analyze() {
	TreeSet keys = new TreeSet(caMap.keySet());
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    Integer key = (Integer) iter.next();
	    Integer nextKey = new Integer(key.intValue() + 1);
	    calcDist(key, (KPoint) nitMap.get(key), (KPoint) caMap.get(key));
	    calcDist(key, (KPoint) caMap.get(key), (KPoint) carbMap.get(key));
	    calcDist(key, (KPoint) carbMap.get(key), (KPoint) oxyMap.get(key));
	    if (keys.contains(nextKey)) {
		calcDist(key, (KPoint) carbMap.get(key), (KPoint) nitMap.get(nextKey));
	    }
	    calcAngle(key, (KPoint) nitMap.get(key), (KPoint) caMap.get(key), (KPoint) carbMap.get(key));
	    calcAngle(key, (KPoint) caMap.get(key), (KPoint) carbMap.get(key), (KPoint) oxyMap.get(key));
	    if (keys.contains(nextKey)) {
		calcAngle(key, (KPoint)caMap.get(key), (KPoint)carbMap.get(key), (KPoint)nitMap.get(nextKey));
		calcAngle(key, (KPoint)oxyMap.get(key), (KPoint)carbMap.get(key), (KPoint)nitMap.get(nextKey));
		calcAngle(key, (KPoint)carbMap.get(key), (KPoint)nitMap.get(nextKey), (KPoint)caMap.get(nextKey));
	    }
	}
    }

    public void calcDist(Integer key, KPoint pt1, KPoint pt2) {
	//System.out.println(pt1 + " " + pt2);
	if ((pt1 != null)&&(pt2 != null)) {
	    String atom1 = KinUtil.getAtomName(pt1).toLowerCase();
	    String atom2 = KinUtil.getAtomName(pt2).toLowerCase();
	    String res1 = KinUtil.getResName(pt1).toLowerCase();
	    double idealdist;
	    double sd;
	    if (res1.equals("pro")) {
		idealdist = ((Double)proLength.get(atom1 + atom2)).doubleValue();
		sd = ((Double)proSD.get(atom1 + atom2)).doubleValue();
	    } else if (res1.equals("gly")) {
		idealdist = ((Double)glyLength.get(atom1 + atom2)).doubleValue();
		sd = ((Double)glySD.get(atom1 + atom2)).doubleValue();
	    } else {
		idealdist = ((Double)pepLength.get(atom1 + atom2)).doubleValue();
		sd = ((Double)pepSD.get(atom1 + atom2)).doubleValue();
	    }
	    
	    Triple trip1 = new Triple(pt1);
	    Triple trip2 = new Triple(pt2);
	    double dist = trip1.distance(trip2);
	    if ((dist <= idealdist - 4 * sd)||(dist >= idealdist + 4 * sd)) {
		System.out.print("res " + key + " " + atom1 + "-" + atom2 + " ");
		System.out.print("Distance " + df.format(dist) + " ");
		System.out.println(df.format((dist - idealdist)/sd) + " sigma off");
		drawBall("res "+key+" "+atom1+"-"+atom2, trip1, trip2, dist, idealdist);
	    }
	}
    }

    public void calcAngle(Integer key, KPoint pt1, KPoint pt2, KPoint pt3) {
	if ((pt1 != null)&&(pt2 != null)&&(pt3 != null)) {
	    String atom1 = KinUtil.getAtomName(pt1).toLowerCase();
	    String atom2 = KinUtil.getAtomName(pt2).toLowerCase();
	    String atom3 = KinUtil.getAtomName(pt3).toLowerCase();
	    String res1 = KinUtil.getResName(pt1).toLowerCase();
	    double idealAng;
	    double sd;
	    if (res1.equals("pro")) {
		idealAng = ((Double)proAng.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)proAngSD.get(atom1 + atom2 + atom3)).doubleValue();
	    } else if (res1.equals("gly")) {
		idealAng = ((Double)glyAng.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)glyAngSD.get(atom1 + atom2 + atom3)).doubleValue();
	    } else {
		idealAng = ((Double)pepAng.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)pepAngSD.get(atom1 + atom2 + atom3)).doubleValue();
	    }
	    Triple trip1 = new Triple(pt1);
	    Triple trip2 = new Triple(pt2);
	    Triple trip3 = new Triple(pt3);
	    double ang = Triple.angle(trip1, trip2, trip3);
	    if ((ang <= idealAng - 4 * sd) || (ang >= idealAng + 4 * sd)) {
		System.out.print("res " + key + " " + atom1 + "-" + atom2 + "-" + atom3 + " ");
		System.out.print("angle " + df.format(ang) + " ");
		System.out.println(df.format((ang - idealAng)/sd) + " sigma off");
		drawLine("res "+key+" "+atom1+"-"+atom2+"-"+atom3, trip1, trip2, trip3, ang, idealAng);
	    }
	}
    }

    public void drawBall(String name, Triple trp1, Triple trp2, double dist, double idealdist) {
	double distdiff = dist - idealdist;
	KPaint color = KPalette.blue;
	if (distdiff < 0) {
	    color = KPalette.red;
	    distdiff = - distdiff;
	}
	BallPoint point = new BallPoint(geomList, name);
	geomList.add(point);
	point.setColor(color);
	point.setRadius((float)distdiff);
	Triple origVector = new Triple().likeVector(trp1, trp2);
	origVector = origVector.mult(idealdist/dist).add(trp1);
	point.setX(origVector.getX());
	point.setY(origVector.getY());
	point.setZ(origVector.getZ());
    }

    public void drawLine(String name, Triple trp1, Triple trp2, Triple trp3, double ang, double idealang) {
	//Triple vect1 = new Triple().likeVector(trp2, trp1);
	//Triple vect2 = new Triple().likeVector(trp2, trp3);
	//Triple normal = vect1.cross(vect2);
	Triple normal = new Triple().likeNormal(trp1, trp2, trp3);
	VectorPoint testNorm = new VectorPoint(geomList, "testNorm", null);
	//geomList.add(testNorm);
	testNorm.setXYZ(trp2.getX(), trp2.getY(), trp2.getZ());
	VectorPoint norm = new VectorPoint(geomList, "norm", testNorm);
	//geomList.add(norm);
	norm.setXYZ(normal.getX() + trp2.getX(), normal.getY() + trp2.getY(), normal.getZ() + trp2.getZ());
	Transform rotate = new Transform();
	//rotate = rotate.likeRotation(normal, idealang - ang);
	rotate = rotate.likeRotation(testNorm, norm, ang - idealang);
	VectorPoint prev = new VectorPoint(geomList, name, null);
	geomList.add(prev);
	prev.setX(trp2.getX());
	prev.setY(trp2.getY());
	prev.setZ(trp2.getZ());
	VectorPoint point = new VectorPoint(geomList, name, prev);
	point.setColor(KPalette.pink);
	geomList.add(point);
	point.setX(trp3.getX());
	point.setY(trp3.getY());
	point.setZ(trp3.getZ());
	rotate.transform(point);
	
    }

    public JMenuItem getToolsMenuItem() {
	return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onStart"));
    }
    
    public String toString() {
	return "Analyze Geometry";
    }
}
