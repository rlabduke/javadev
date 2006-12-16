// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import driftwood.gui.*;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * <code>ColorGrouperTool</code> was created to make it easier to group
 * multidimensional points.  
 *
 * <p>Copyright (C) 2004 by Vincent B. Chen. All rights reserved.
 * <br>Begun on Dec 05 2006.
 **/
 
 public class ColorGrouperPlugin extends Plugin {
	 
	 //{{{ Constants
	 
	 //}}}

	 //{{{ Variables
	 HashMap colorSetMap;
	 //KGroup colorGroup = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ColorGrouperPlugin(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ onGroupByColor
public void onGroupByColor(ActionEvent e) {
	Kinemage kin = kMain.getKinemage();
	colorSetMap = new HashMap();
	//Set existingColors = colorSetMap.keySet();
	splitKin(kin);
	KGroup splitGroup = scanForSplitGroup(kin);
	if (splitGroup != null) {
		splitGroup.setOwner(null);
		kMain.getKinemage().remove(splitGroup);
	}
	//clearColorLists(kin);
	populateColorLists(kin);
	//kin.add(colorGroup);
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
}
//}}}

//{{{ clearColorLists
public void clearColorLists(AGE target) {
	if (target instanceof KList) {
		KList list = (KList) target;
		System.out.println("scanning " + list.getName());
		AGE owner = (AGE) list.getOwner();
		if (owner.getName().equals("color")) {
			//list.clear();
			String name = list.getName();
			if (KPalette.getStandardMap().containsKey(name)) {
				list.clear();
			}
			//if (!colorSetMap.containsKey(name)) {
			//	colorSetMap.put(name, new HashSet());
			//}
		}
	} else {
		Iterator iter = target.iterator();
		while (iter.hasNext()) {
			AGE targ = (AGE) iter.next();
			clearColorLists(targ);
		}
	}
}
//}}}

//{{{ scanForSplitGroup
public KGroup scanForSplitGroup(Kinemage kin) {
	Iterator groups = kin.iterator();
	while (groups.hasNext()) {
		KGroup group = (KGroup) groups.next();
		if (group.getName().equals("new splits")) {
			//colorGroup = group;
			return group;
		}
	}
	return null;
}
//}}}

//{{{ splitKin
public void splitKin(AGE target) {
	if (target instanceof KList) {
		KList list = (KList) target;
		if (list.getType().equals(KList.BALL)) {
			System.out.println("splitting " + list.getName());
			ListIterator iter = target.iterator();
			while (iter.hasNext()) {
				KPoint pt = (KPoint) iter.next();
				String paintName = pt.getDrawingColor().toString();
				if (colorSetMap.containsKey(paintName)) {
					HashSet colorSet = (HashSet)colorSetMap.get(paintName);
					colorSet.add(pt);
					//pt.setOwner(colorList);
				} else {
					HashSet colorSet = new HashSet();
					colorSet.add(pt);
					colorSetMap.put(paintName, colorSet);
					
					//if (colorGroup == null) {
						//colorGroup = new KGroup(kMain.getKinemage(), "new splits");
						//kMain.getKinemage().add(colorGroup);
					//}
					/*
					KSubgroup sub = new KSubgroup(colorGroup, "color");
					sub.setHasButton(false);
					colorGroup.add(sub);
					KList newList = new KList(sub, paintName);
					KList oldList = (KList) pt.getOwner();
					newList.flags |= oldList.flags;
					newList.addMaster("color");
					sub.add(newList);
					newList.setDimension(pt.getAllCoords().length);
					newList.setType(KList.BALL);
					newList.add(pt);
					pt.setOwner(newList);
					colorListMap.put(paintName, newList);
					*/
				}
			}
		}
	} else {
		Iterator iter = target.iterator();
		while (iter.hasNext()) {
			AGE targ = (AGE) iter.next();
			splitKin(targ);
		}
	}
}
//}}}

//{{{ populateColorLists()
public void populateColorLists(Kinemage kin) {
	KGroup colorGroup = new KGroup(kin, "new splits");
	kin.add(colorGroup);
	Iterator keys = colorSetMap.keySet().iterator();
	while (keys.hasNext()) {
		String color = (String) keys.next();
		HashSet points = (HashSet) colorSetMap.get(color);
		KSubgroup sub = new KSubgroup(colorGroup, "color");
		sub.setHasButton(false);
		colorGroup.add(sub);
		KList newList = new KList(sub, color);
		newList.addMaster("color");
		sub.add(newList);
		newList.setType(KList.BALL);
		Iterator pointsIter = points.iterator();
		while (pointsIter.hasNext()) {
			KPoint pt = (KPoint) pointsIter.next();
			KList oldList = (KList) pt.getOwner();
			newList.flags |= oldList.flags;
			newList.setDimension(pt.getAllCoords().length);
			newList.add(pt);
			pt.setOwner(newList);
		}
	}
}
	
//}}}

//{{{ getToolsMenuItem
public JMenuItem getToolsMenuItem() {
	return new JMenuItem(new ReflectiveAction("Group by color", null, this, "onGroupByColor"));
}
//}}}

//{{{ toString
public String toString() {
	return "Group By Color";
}
//}}}

 }
