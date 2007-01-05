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
	 HashMap<String, HashSet<KPoint>> colorSetMap;
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
	colorSetMap = new HashMap<String, HashSet<KPoint>>();
	//Set existingColors = colorSetMap.keySet();
	splitKin(kin);
	KGroup splitGroup = scanForSplitGroup(kin);
	if (splitGroup != null) {
		kMain.getKinemage().remove(splitGroup);
	}
	//clearColorLists(kin);
	populateColorLists(kin);
	//kin.add(colorGroup);
	//kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
}
//}}}

//{{{ clearColorLists
/* seems to not be used anymore
public void clearColorLists(AGE target) {
	if (target instanceof KList) {
		KList list = (KList) target;
		System.out.println("scanning " + list.getName());
		AGE owner = (AGE) list.getParent();
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
}*/
//}}}

//{{{ scanForSplitGroup
/** 
 * Scans to see if this kin has been color split already by checking 
 * the names of the groups for a group named new split.
 **/
public KGroup scanForSplitGroup(Kinemage kin) {
	Iterator<KGroup> groups = kin.iterator();
	while (groups.hasNext()) {
		KGroup group = groups.next();
		if (group.getName().equals("new splits")) {
			//colorGroup = group;
			return group;
		}
	}
	return null;
}
//}}}

//{{{ splitKin
/** 
 * Separates ballpoints into multiple hashsets, depending on color name.  
 **/
public void splitKin(Kinemage kin) {
	KIterator<KList> listIter = KIterator.allLists(kin);
  for (KList list : listIter) {
		if (list.getType().equals(KList.BALL)) {
			System.out.println("splitting " + list.getName());
			//Iterator<KPoint> iter = list.iterator();
      for (KPoint pt : list) {
				String paintName = pt.getDrawingColor().toString();
				if (colorSetMap.containsKey(paintName)) {
					HashSet<KPoint> colorSet = (HashSet)colorSetMap.get(paintName);
					colorSet.add(pt);
					//pt.setParent(colorList);
				} else {
					HashSet<KPoint> colorSet = new HashSet();
					colorSet.add(pt);
					colorSetMap.put(paintName, colorSet);
				}
			}
		}
	}
}
//}}}

//{{{ populateColorLists()
/**
 * Moves the points from old lists to new color-split lists.
 * Old "feature" where points were copied (instead of moved) 
 * from old lists to new lists might not work anymore!
 * Needs new way of copying flags from old lists.
 **/
public void populateColorLists(Kinemage kin) {
	KGroup colorGroup = new KGroup("new splits");
	kin.add(colorGroup);
	//Iterator<String> keys = colorSetMap.keySet().iterator();
  for (String color : colorSetMap.keySet()) {
		HashSet<KPoint> points = colorSetMap.get(color);
		KGroup sub = new KGroup("color");
		colorGroup.add(sub);
		sub.setHasButton(false);
		//colorGroup.add(sub);
		KList newList = new KList(KList.BALL, color);
		newList.addMaster("color");
		sub.add(newList);
		Iterator<KPoint> pointsIter = points.iterator();
		while (pointsIter.hasNext()) {
			KPoint pt = pointsIter.next();
			KList oldList = (KList) pt.getParent();
			//newList.flags |= oldList.flags;
			newList.setDimension(pt.getAllCoords().length);
			newList.add(pt);
			//pt.setParent(newList);
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
