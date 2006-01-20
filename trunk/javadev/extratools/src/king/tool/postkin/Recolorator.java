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

    //public void clickHandler(KPoint p);

    abstract public boolean contains(KPoint p);

    abstract public void preChangeAnalyses(KPoint p);

    //public void highlightRange(KPoint p);
    //abstract public int numofResidues();
    abstract public String onTable();

    abstract public void highlightAll(KPoint p, KPaint[] colors);

    abstract public void highlightRange(int firstNum, int secondNum, KPaint[] color);

    abstract public void highlightAA(KPoint p, String aaName, KPaint color, boolean colorPrior);

    //{{{ getResNumber
//###################################################################################################
    /**
     * Helper function to get the residue number of parentList.  It gets the first KPoint in the KList, 
     * and extracts the residue number from the name.  EXTREMELY dependent on the format of the name of the KPoint.
     **/
    
    public int getResNumber(KPoint point) {
	return KinUtil.getResNumber(point);
	/*
	String name = point.getName().trim();
	String[] uncleanParsed = name.split(" ");
	String[] parsed = new String[uncleanParsed.length];
        int i2 = 0;
	// To clean out the empty strings from the split name.
	
	for (int i = 0; i < uncleanParsed.length; i++) {
	    String unclean = uncleanParsed[i];
	    if ((!unclean.equals(""))&&(!unclean.equals(" "))) {
		parsed[i2] = unclean;
		i2++;
	    }
	}
	// another pass to see if there are any AAName + int in name.
	if (parsed[1].length() > 3) {
	    String parseValue = parsed[1].substring(3);
	    if (isNumeric(parseValue)) {
		//System.out.print(parseValue + " ");
		return Integer.parseInt(parseValue);
	    }
	}
	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (isNumeric(parseValue)) {
		if (Integer.parseInt(parseValue)>0) {
		    return Integer.parseInt(parseValue);
		}
	    }
	}

	return -1;
	*/
    }

    public int getResNumber(KList parentList) {
	Iterator pointIter = parentList.iterator();
	KPoint firstPoint = (KPoint) pointIter.next();
	return KinUtil.getResNumber(firstPoint);
    }

    public String getResName(KPoint point) {
	return KinUtil.getResName(point);
	/*
	String name = point.getName().trim();
	String[] uncleanParsed = name.split(" ");
	String[] parsed = new String[uncleanParsed.length];
        int i2 = 0;
	String returnString = "";
	// To clean out the empty strings from the split name.
	
	for (int i = 0; i < uncleanParsed.length; i++) {
	    String unclean = uncleanParsed[i];
	    if ((!unclean.equals(""))&&(!unclean.equals(" "))) {
		parsed[i2] = unclean;
		i2++;
	    }
	}
	return parsed[1];
	//while ((returnString.equals("")&&
	*/
    }

    public int numofResidues() {
	return highResNum.intValue()-lowResNum.intValue()+1;
    }
    
    public boolean isNumeric(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    public void setPointColors(KList list, KPaint color) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    point.setColor(color);
	}
    }

    
}
