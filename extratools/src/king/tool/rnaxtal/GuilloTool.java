// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
import king.*;
import king.core.*;
import king.points.*;

import java.util.*;
import java.awt.event.*;

public class GuilloTool extends BasicTool {

//{{{ Variable definitions
//##################################################################################################
    ArrayList selectedPoints;
    PlaneFinder neckFinder;
    KList list;

//}}}


//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public GuilloTool(ToolBox tb)
    {
        super(tb);
	selectedPoints = new ArrayList();
	list = new KList(KList.TRIANGLE);
	list.setName("Plain");
	list.setWidth(5);
	//list.setColor(KPalette.sky);
    }
//}}}

    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if(p instanceof VectorPoint) {
	    if (selectedPoints.size() > 2) {
		calcDirection(p);
		selectedPoints.clear();
	    } else {
		selectedPoints.add(p);
		calcPlane();
	    }
	}
    }
    

    public void calcPlane() {
	if (selectedPoints.size() > 2) {
	    neckFinder = new PlaneFinder();
	    neckFinder.fitPlane(selectedPoints);
	    //neckFinder.drawNormal();
	    neckFinder.drawPlane(2);
	    this.list = neckFinder.getList();
	    list.setColor(KPalette.sky);

	    Kinemage kin = kMain.getKinemage();
	    KGroup group = new KGroup("Neck cutoff");
	    group.setParent(kin);
	    kin.add(group);
	    
	    KGroup subgroup = new KGroup("Neck cutoff");
	    subgroup.setHasButton(false);
	    subgroup.setParent(group);
	    group.add(subgroup);

	    list.setParent(subgroup);
	    subgroup.add(list);

	    //selectedPoints.clear();
	    
	    //kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
	}
    }

    public void calcDirection(KPoint p) {
	Plane neckPlane = neckFinder.getPlane();
	PolygonFinder polyTracker = new PolygonFinder();
	KList parentList = (KList) p.getParent();
	polyTracker.initiateMap(parentList);
	//polyTracker.finishMap(parentList);
	KList polyList = polyTracker.getPolyhedra(p);
	KList baseList = new KList(KList.VECTOR);
	baseList.setName("base");
	baseList.setWidth(5);
	//baseList.setColor(KPalette.green);
	Iterator iter = polyList.iterator();
	KPoint basePoint = null;
	KPoint point = null;
	while (iter.hasNext()) {
	    point = (KPoint) iter.next();
	    if (neckPlane.isBelow(p)) {
		if (neckPlane.isBelow(point)) {
		    KPoint prev = point.getPrev();
		    if (!(prev == null)) {
			if (!neckPlane.isBelow(prev)) {
			    point.setPrev(null);
			}
		    }
		    //point.setColor(KPalette.green);
		    point.setParent(baseList);
		    baseList.add(point);
		    if (point.equals(p)) {
			basePoint = point;
		    }
		    
		}
	    } else {
		if (!neckPlane.isBelow(point)) {
		    KPoint prev = point.getPrev();
		    if (!(prev == null)) {
			if (neckPlane.isBelow(prev)) {
			    point.setPrev(null);
			}
		    }
		    //point.setColor(KPalette.green);
		    point.setParent(baseList);
		    baseList.add(point);
		    if (point.equals(p)) {
			basePoint = point;
		    }
		}
	    }
	}
	
	PolygonFinder baseTracker = new PolygonFinder();
	baseTracker.initiateMap(baseList);
	//KPoint basePoint = p.clone();
	//basePoint.setParent(baseList);
	System.out.println(basePoint);
	KList base = baseTracker.getPolyhedra(basePoint);
	base.setName("base list");
	base.setWidth(5);
	base.setColor(KPalette.green);
	base.setOn(true);

	Kinemage kin = kMain.getKinemage();
	KGroup group = new KGroup("base");
	kin.add(group);
	//kin.metadata.put("base group", group);
	
	KGroup subgroup = new KGroup("base");
	subgroup.setHasButton(false);
	group.add(subgroup);
	//kin.metadata.put("base subgroup", subgroup);

	//kin.metadata.put("base list", base);
	subgroup.add(base);
	
	//selectedPoints.clear();
	//kin.signal.signalKinemage(kin, KinemageSignal.STRUCTURE);

	
    }	

    public void getPlane() {

	//System.out.println(normal.toString() + "; "  + anchor.toString());
    }


    public void clear() {
	selectedPoints.clear();
    }

    public String toString() {return "Limit Bases";}
}
