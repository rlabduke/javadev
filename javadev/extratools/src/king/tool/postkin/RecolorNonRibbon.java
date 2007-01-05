// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;

import java.util.*;
import driftwood.moldb2.AminoAcid;

//}}}
/**
 * <code>RecolorNonRibbon</code> implements Recolorator to allow recoloring of
 * kinemage sections that aren't ribbons (C-alphas, lots).
 *
 * <p>Copyright (C) 2005 by Vincent B. Chen. All rights reserved.
 * <br>Begun on Feb 20 2005
 **/

public class RecolorNonRibbon extends Recolorator //implements ActionListener 
{

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################

    // sortedKin is hashmap with masters as keys, arraylists as values
    // ribbonMap is hashmap with Klists as keys, arraylists as values
    //HashMap sortedKin, ribbonMap;
    // sortbyNum is hashmap with Integer resNum as keys, arraylists as values
    //HashMap sortbyNum;
    //HashMap sortedKin;
    HashMap structMap;
    HashSet clickedLists;


    String[] aaNames = {"gly", "ala", "ser", "thr", "cys", "val", "leu", "ile", "met", "pro", "phe", "tyr", "trp", "asp", "glu", "asn", "gln", "his", "lys", "arg"};
    //JComboBox  aaBox;
    //JCheckBox  colorPrior;

    //JTextPane textPane = null;
    //SimpleAttributeSet sas;

//}}}

//{{{ Constructor(s)
//##############################################################################
    public RecolorNonRibbon()
    {
        //super(tb);
	newGroup();
	clickedLists = new HashSet();

        //undoStack = new LinkedList();
	//buildGUI();
    }
//}}}


//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    /*
    public void clickHandler(KPoint p)
    {
        //super.click(x, y, p, ev);
	if (p != null) {
	    KList parentList = (KList) p.getParent();
	    Integer resNumber = new Integer(getResNumber(p));
	    if (!structMap.containsKey(resNumber))//||!clickedLists.contains(parentList)) {
		{
		newGroup();
		splitStructure(p);
	    }

	    //Kinemage k = kMain.getKinemage();
	    //if(k != null) k.setModified(true);
	    //}
	    //}
	    //if (colorAll.isSelected()) {
	    //highlightAll(p);
		//} else if (colorAA.isSelected()) {
		//highlightAA(p);
		//} else {
		//highlightRange(p);
		//}
	}
	}*/
//})}

    public String onTable() {
	TreeSet keys = new TreeSet(structMap.keySet());

	String aaText = "";
	int j = 0;
	for (int i = 1; i <= highResNum.intValue(); i++) {
	    if (j == 5) {
		aaText = aaText.concat(" ");
		j = 0;
	    }
	    j++;
	    if (keys.contains(new Integer(i))) {
		ArrayList list = (ArrayList) structMap.get(new Integer(i));
		KPoint point = (KPoint) list.get(0);
		String aa = getResName(point);
		if (aa.length()==4) {
		    aa = aa.substring(1);
		}
		aaText = aaText.concat(AminoAcid.translate(aa));
	    } else {
		aaText = aaText.concat("-");
	    }
	}
	return aaText;
    }

    public void preChangeAnalyses(KPoint p) {
	newGroup();
	splitStructure(p);
    }

//{{{ splitStructure
//##################################################################################################
    /**
     * 
     **/
    
    public void splitStructure(KPoint p) {

	String pointID = p.getName().trim();
	KList parentList = (KList) p.getParent();
	clickedLists.add(parentList);
	//KSubgroup parentGroup = (KSubgroup) parentList.getParent();
	Iterator iter = parentList.iterator();
	KPoint point;
	ArrayList listofPoints = new ArrayList();

	while (iter.hasNext()) {
	    point = (KPoint) iter.next();
	    //String master = getOldMaster(list);
	    Integer resNumber = new Integer(getResNumber(point));
	    listofPoints = (ArrayList) structMap.get(resNumber);
	    if (listofPoints == null) {
		listofPoints = new ArrayList();
	    } 
	    listofPoints.add(point);
	    structMap.put(resNumber, listofPoints);
	}
	//Set keySet = structMap.keySet();
	TreeSet keys = new TreeSet(structMap.keySet());
	lowResNum = (Integer) keys.first();
	highResNum = (Integer) keys.last();
	
    }
//}}}

    public boolean contains(KPoint p) {
	KList parentList = (KList) p.getParent();
	Integer resNum = new Integer(getResNumber(p));
	return (structMap.containsKey(resNum)&&clickedLists.contains(parentList));
    }
	



//{{{ highlightRange
//###################################################################################################
    /**
     * For highlighting a range of a kinemage. The first time this function is called, it stores p
     * as the starting point, and the second time it's called, it colors the region between the stored p
     * and the current p.  
     *
     **/

    public void highlightRange(int firstNum, int secondNum, KPaint[] colors) {
	int index = 0;
	for (int i = firstNum; i <= secondNum; i++) {
	    if (index >= colors.length) {
		index = 0;
	    }
	    Integer hashKey = new Integer(i);
	    
	    if (structMap.containsKey(hashKey)) {
		ArrayList listofLists = (ArrayList) structMap.get(hashKey);
		Iterator iter = listofLists.iterator();
		
		while (iter.hasNext()) {
		    KPoint point = (KPoint) iter.next();
		    point.setColor((KPaint) colors[index]);
		}
		index++;
	    }
	}
    }

//}}}

//{{{ highlightAll
//#######################################################################################################
    /**
     * For coloring all of a KList.  
     **/
    
    public void highlightAll(KPoint p, KPaint[] colors) {
    	//KList parentList = (KList) p.getParent();
    	//Iterator iter = parentList.iterator();
    	highlightRange(lowResNum.intValue(), highResNum.intValue(), colors);
    }
    
//}}}

    public void highlightAA(KPoint p, String aaName, KPaint color, boolean colorPrior) {
	KList parentList = (KList) p.getParent();
	Iterator iter = parentList.iterator();
	HashSet aaNums = new HashSet();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    String name = point.getName();
	    if (name.indexOf(aaName) != -1) {
		point.setColor(color);
		Integer resNum = new Integer(getResNumber(point));
		aaNums.add(resNum);
	    }
	}
	if (colorPrior) {
	    iter = aaNums.iterator();
	    while (iter.hasNext()) {
		int priorResNum = ((Integer) iter.next()).intValue() - 1;
		ArrayList listofPoints = (ArrayList) structMap.get(new Integer(priorResNum));
		if (listofPoints != null) {
		    Iterator listIter = listofPoints.iterator();
		    while (listIter.hasNext()) {
			KPoint point = (KPoint) listIter.next();
			if (point != null) {
			    point.setColor(KPalette.green);
			}
		    }
		}
	    }
	}
    }

//{{{ newGroup
//######################################################################################################
    /**
     * Resets the tool by creating new hashmaps for the various list storing maps. This is for coloring
     * ribbons in different KGroups, or in new kinemages.
     **/
    public void newGroup() {
	clickedLists = new HashSet();
	structMap = new HashMap();
    }
//}}}

//{{{ debug
//######################################################################################################
    /**
     * debugger function.
     **/
    public void debug() {
	Set keys = structMap.keySet();
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    String master = (String) iter.next();
	    ArrayList listofLists = (ArrayList) structMap.get(master);
	    System.out.print(master + ": ");
	    System.out.print(listofLists.size() + "; ");
	}
	System.out.println("");
    }
//}}}

}//class

