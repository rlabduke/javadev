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
HashMap colorListMap;
KGroup colorGroup = null;
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
	colorListMap = new HashMap();
	scanForColors(kin);
	splitKin(kin);
	kin.add(colorGroup);
	kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
}
//}}}

//{{{ scanForColors
public void scanForColors(AGE target) {
	if (target instanceof KList) {
		KList list = (KList) target;
		System.out.println("scanning " + list.getName());
		AGE owner = (AGE) list.getOwner();
		if (owner.getName().equals("color")) {
			String name = list.getName();
			if (!colorListMap.containsKey(name)) {
				colorListMap.put(name, list);
			}
		}
	} else {
		Iterator iter = target.iterator();
		while (iter.hasNext()) {
			AGE targ = (AGE) iter.next();
			scanForColors(targ);
		}
	}
}
//}}}

//{{{ splitKin
public void splitKin(AGE target) {
	if (target instanceof KList) {
		KList list = (KList) target;
		System.out.println("splitting " + list.getName());
		if ((list.getType().equals(KList.BALL))&&(!list.hasMaster("color"))) {
			ListIterator iter = target.iterator();
			while (iter.hasNext()) {
				KPoint pt = (KPoint) iter.next();
				String paintName = pt.getDrawingColor().toString();
				if (colorListMap.containsKey(paintName)) {
					KList colorList = (KList)colorListMap.get(paintName);
					colorList.add(pt);
					pt.setOwner(colorList);
				} else {
					if (colorGroup == null) {
						colorGroup = new KGroup(kMain.getKinemage(), "new splits");
						//kMain.getKinemage().add(colorGroup);
					}
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
