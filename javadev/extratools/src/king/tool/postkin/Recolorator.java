// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.tool.util.KinUtil;

import java.util.*;

//}}}
/**
 * <code>Recolorator</code> is an abstract class to allow RecolorTool
 * to be able to deal with the different types of kinemages (Ribbons vs
 * everything else).
 *
 * <p>Copyright (C) 2005 by Vincent B. Chen. All rights reserved.
 * <br>Begun on Feb 20 2005
 **/


public abstract class Recolorator {

    Integer lowResNum, highResNum;

    //abstract public boolean contains(KPoint p);

    abstract public void preChangeAnalyses(KPoint p);

    abstract public String onTable();

    abstract public void highlightAll(KPoint p, KPaint[] colors);

    abstract public void highlightRange(int firstNum, int secondNum, KPaint[] color);

    abstract public void highlightAA(KPoint p, String aaName, KPaint color, boolean colorPrior);

    public int numofResidues() {
	return highResNum.intValue()-lowResNum.intValue()+1;
    }

    public void setPointColors(Collection list, KPaint color) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
      if (!point.isUnpickable()) {
        point.setColor(color);
      }
	}
    }
    
}