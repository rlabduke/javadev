// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.docking;
import king.*;
import king.core.*;

import java.util.*;

import java.awt.event.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.gui.*;

public class DockConnTool extends DockLsqTool {

//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    //TablePane       toolpane;
    //JRadioButton    btnReference, btnMobile;
    //JButton         btnDock;
    JCheckBox keepRefBox;
    HashSet mobilePoints;
    HashMap adjacencyMap;
    AbstractPoint firstClick, secondClick;
    LinkedList refList = new LinkedList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DockConnTool(ToolBox tb)
    {
        super(tb);

        addGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void addGUI()
    {
	//super.buildGUI();
        //btnDock = new JButton(new ReflectiveAction("Dock mobile on reference", null, this, "onDock"));
	keepRefBox = new JCheckBox("Keep reference points", true);
	toolpane.newRow();
	toolpane.add(keepRefBox, 3, 1);
    }

    //}

    public void start() {
	if (kMain.getKinemage() == null) return;
	adjacencyMap = new HashMap();
	//buildAdjacencyList();

	show();
    }


//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        //super.click(x, y, p, ev);
        services.pick(p);
        
        if(p != null && p.getComment() != null)
            clickActionHandler(p.getComment());

        if(p != null) {
	    if (firstClick != null) {
		buildAdjacencyList(false);
		ArrayList list = pathFinder(firstClick, (AbstractPoint) p);
		//System.out.println(list.size());
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
		    AbstractPoint point = (AbstractPoint) iter.next();
		    //Triple t = new Triple(point);
		    if (btnReference.isSelected()) {
			pkReference.add(point.getName(), point);
		    } else if (btnMobile.isSelected()) {
			pkMobile.add(point.getName(), point);
		    }
		    else {
			JOptionPane.showMessageDialog(kMain.getTopWindow(),
						      "Either 'Reference' or 'Mobile' should be selected.",
						      "Error", JOptionPane.ERROR_MESSAGE);
		    }
		    firstClick = null;
		}
	    } else {
		firstClick = (AbstractPoint) p;
	    }
	    
	    /*	
            Triple t = new Triple(p.getOrigX(), p.getOrigY(), p.getOrigZ());
            if(btnReference.isSelected())
            {
                pkReference.add(p.getName(), t);
            }
            else if(btnMobile.isSelected()) 
            {
                pkMobile.add(p.getName(), t);
            }
            else
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Either 'Reference' or 'Mobile' should be selected.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
	    */
        }
    }

    public void buildAdjacencyList(boolean useAllLists) {
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
			if (list.isOn()||useAllLists) {
			    Iterator listIter = list.iterator();
			    while (listIter.hasNext()) {
				Object next = listIter.next();
				if (next instanceof VectorPoint) {
				    VectorPoint currPoint = (VectorPoint) next;
				    
				    if ((!currPoint.isBreak())&&((currPoint.isOn())||useAllLists)) {
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
	    //System.out.println(adjSet.size());

	    //if (adjSet.size() = 1) {
		
	    Iterator adjIter = adjSet.iterator();
	    while (adjIter.hasNext()) {
		AbstractPoint adjPoint = (AbstractPoint) adjIter.next();
		if (((HashSet) adjacencyMap.get(adjPoint)).size() == 1) {
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

    private Object clonePoint(AbstractPoint point) {
	VectorPoint pointClone = new VectorPoint(null, point.getName(), null);
	pointClone.setX((float) point.getX());
	pointClone.setY((float) point.getY());
	pointClone.setZ((float) point.getZ());
	return pointClone;
    }

//{{{ onDock
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDock(ActionEvent ev)
    {
	buildAdjacencyList(true);
	mobilityFinder((AbstractPoint)pkMobile.tupleList.get(0));
        Tuple3[] ref = (Tuple3[])pkReference.tupleList.toArray(new Tuple3[pkReference.tupleList.size()]);
        Tuple3[] mob = (Tuple3[])pkMobile.tupleList.toArray(new Tuple3[pkMobile.tupleList.size()]);
        double[] w = new double[ref.length];
        Arrays.fill(w, 1.0);
        
        SuperPoser poser = new SuperPoser(ref, mob);
        Transform t = poser.superpos(w);
        
        Kinemage kin = kMain.getKinemage();
        if(!t.isNaN() && kin != null)
        {
            transform(kin, t);
            kin.setModified(true);
        }
        
        // Swap which button is selected
        if(btnReference.isSelected())   btnMobile.setSelected(true);
        else                            btnReference.setSelected(true);

        if(!keepRefBox.isSelected()) {
	    pkReference.clear();
	    btnMobile.setSelected(true);
	}
        pkMobile.clear();
        kCanvas.repaint();
    }

//{{{ transformAllVisible
//##############################################################################
    private void transform(AGE target, Transform t)
    {
        //if(!target.isOn()) return;
        
        if(target instanceof KList)
        {
            Triple proxy = new Triple();
            for(Iterator iter = target.iterator(); iter.hasNext(); )
            {
                KPoint pt = (KPoint)iter.next();
                if(mobilePoints.contains(pt))
                {
                    proxy.setXYZ(pt.getOrigX(), pt.getOrigY(), pt.getOrigZ());
                    t.transform(proxy);
                    pt.setOrigX(proxy.getX());
                    pt.setOrigY(proxy.getY());
                    pt.setOrigZ(proxy.getZ());
                }
            }
        }
        else
        {
            for(Iterator iter = target.iterator(); iter.hasNext(); )
                transform((AGE)iter.next(), t);
        }
    }
//}}}

    public String toString() { return "Dock by picking"; }

    public String getHelpAnchor() { return null; }

}
