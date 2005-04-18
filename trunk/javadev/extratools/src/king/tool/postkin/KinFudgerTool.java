// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;

import driftwood.r3.*;
import driftwood.gui.*;

import javax.swing.*;
import java.util.*;
import java.awt.event.*;

public class KinFudgerTool extends BasicTool {

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################
    HashMap adjacencyMap;
    HashSet mobilePoints;

    JRadioButton fudgeDistance, fudgeAngle;

    AbstractPoint firstClick, secondClick;

//}}}


//{{{ Constructor(s)
//##############################################################################
    public KinFudgerTool(ToolBox tb) {
	super(tb);
    }
//}}}


//{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {
        
        dialog = new JDialog(kMain.getTopWindow(),"Recolor", false);

	fudgeDistance = new JRadioButton("Adjust Distance", true);
	fudgeAngle = new JRadioButton("Adjust Angle", false);

	ButtonGroup fudgeGroup = new ButtonGroup();
	fudgeGroup.add(fudgeDistance);
	fudgeGroup.add(fudgeAngle);
	
	TablePane pane = new TablePane();
	pane.newRow();
	pane.add(fudgeDistance);
	pane.add(fudgeAngle);

	dialog.setContentPane(pane);

    }


    public void start() {
	if (kMain.getKinemage() == null) return;
	adjacencyMap = new HashMap();
	//buildAdjacencyList();
	buildGUI();
	show();
    }

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if (p != null) {
	    if (fudgeDistance.isSelected()) {
		if (firstClick != null) {
		    buildAdjacencyList();
		    String ans = askInput("distance?");
		    if (ans != null) {
		    double dist = Double.parseDouble(ans);
			System.out.println("starting to find mobiles");
			mobilityFinder(firstClick,(AbstractPoint) p);
			System.out.println("finished finding mobiles");
			translatePoints(firstClick, (AbstractPoint) p, dist);
			System.out.println("finished");
		    }
		    firstClick = null;
		} else {
		    firstClick = (AbstractPoint) p;
		}
	    }
	    if (fudgeAngle.isSelected()) {
		if (firstClick !=null) {
		    if (secondClick != null) {
			buildAdjacencyList();
			String ans = askInput("angle?");
			//System.out.println(ans);
			if (ans != null) {
			    double angle = Double.parseDouble(ans);
			    System.out.println("starting to find mobiles");
			    mobilityFinder(secondClick,(AbstractPoint) p);
			    System.out.println("finished finding mobiles");
			    rotatePoints(firstClick, secondClick, (AbstractPoint) p, angle);
			    System.out.println("finished");
			}
			firstClick = null;
			secondClick = null;
		    } else {
			secondClick = (AbstractPoint) p;
		    }
		} else {
		    firstClick = (AbstractPoint) p;
		}
	    }
	}
	

    }

    private String askInput(String f) {
	String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(), "What is your desired " + f);
	return choice;
    }

    


    public void buildAdjacencyList() {
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    Iterator groupIters = group.iterator();
	    while (groupIters.hasNext()) {
		KSubgroup sub = (KSubgroup) groupIters.next();
		Iterator subIters = sub.iterator();
		while (subIters.hasNext()) {
		    KList list = (KList) subIters.next();
		    Iterator listIter = list.iterator();
		    while (listIter.hasNext()) {
			Object next = listIter.next();
			if (next instanceof VectorPoint) {
			    VectorPoint currPoint = (VectorPoint) next;
			
			    if (!currPoint.isBreak()) {
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

    public void mobilityFinder(AbstractPoint first, AbstractPoint second) {
	Set keys = adjacencyMap.keySet();
	Iterator iter = keys.iterator();
	HashMap colors = new HashMap();
	mobilePoints = new HashSet();
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

    private Object clonePoint(AbstractPoint point) {
	VectorPoint pointClone = new VectorPoint(null, point.getName(), null);
	pointClone.setX((float) point.getX());
	pointClone.setY((float) point.getY());
	pointClone.setZ((float) point.getZ());
	return pointClone;
    }

    public void translatePoints(AbstractPoint first, AbstractPoint second, double idealDist) {
	System.out.println("translate");
	double realDist = (new Triple(first)).distance(second);
	Triple origVector = new Triple(second.getX() - first.getX(), second.getY() - first.getY(), second.getZ() - first.getZ());
	origVector = origVector.mult(idealDist/realDist).add(first).sub(second);
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    Iterator groupIters = group.iterator();
	    while (groupIters.hasNext()) {
		KSubgroup sub = (KSubgroup) groupIters.next();
		Iterator subIters = sub.iterator();
		while (subIters.hasNext()) {
		    KList list = (KList) subIters.next();
		    Iterator listIter = list.iterator();
		    while (listIter.hasNext()) {
			AbstractPoint point = (AbstractPoint) listIter.next();
			if (mobilePoints.contains(point)) {
			    //System.out.println("Moving: " + point);
			    point.setX(point.getX() + origVector.getX());
			    point.setY(point.getY() + origVector.getY());
			    point.setZ(point.getZ() + origVector.getZ());  
			}
			
		    }
		}
	    }
	}
	kCanvas.repaint();

    }

    public void rotatePoints(AbstractPoint first, AbstractPoint second, AbstractPoint third, double idealAngle) {
	double currAngle = Triple.angle(first, second, third);
	System.out.println(currAngle + ", " + idealAngle);
	Triple vectA = new Triple(first.getX()-second.getX(), first.getY()-second.getY(), first.getZ()-second.getZ());
	Triple vectB = new Triple(third.getX()-second.getX(), third.getY()-second.getY(), third.getZ()-second.getZ());
	Triple normal = vectA.cross(vectB);
	VectorPoint ppoint = new VectorPoint(null, "axis", null);
	ppoint.setXYZ(second.getX(), second.getY(), second.getZ());
	VectorPoint vpoint = new VectorPoint(null, "test", ppoint);
	vpoint.setX(normal.getX()+second.getX());
	vpoint.setY(normal.getY()+second.getY());
	vpoint.setZ(normal.getZ()+second.getZ());
	//drawDebug(ppoint, vpoint);
	Transform rotate = new Transform();
	rotate = rotate.likeRotation(ppoint, vpoint, idealAngle - currAngle);
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    Iterator groupIters = group.iterator();
	    while (groupIters.hasNext()) {
		KSubgroup sub = (KSubgroup) groupIters.next();
		Iterator subIters = sub.iterator();
		while (subIters.hasNext()) {
		    KList list = (KList) subIters.next();
		    Iterator listIter = list.iterator();
		    while (listIter.hasNext()) {
			AbstractPoint point = (AbstractPoint) listIter.next();
			if (mobilePoints.contains(point)) {
			    rotate.transform(point); 
			}
			
		    }
		}
	    }
	}
	kCanvas.repaint();
    }

    private void drawDebug(AbstractPoint prev, VectorPoint point) {
	Kinemage kin = kMain.getKinemage();
	KGroup group = new KGroup(kin, "test");
	kin.add(group);
	KSubgroup sub = new KSubgroup(group, "test");
	group.add(sub);
	KList list = new KList(sub, "list");
	sub.add(list);
	list.add(prev);
	list.add(point);
    }
	

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    //protected Container getToolPanel()
    //{ return dialog; }

    public String getHelpAnchor()
    { return null; }

    public String toString() { return "Fudge Kins"; }
//}}}
}//class	    
