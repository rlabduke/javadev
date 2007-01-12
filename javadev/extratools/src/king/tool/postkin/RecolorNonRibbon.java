// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.tool.util.*;

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
    HashMap<Integer, ArrayList<KPoint>> structMap;
    //HashSet clickedLists;

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
      //clickedLists = new HashSet();
      
      //undoStack = new LinkedList();
      //buildGUI();
    }
//}}}

//{{{ preChangeAnalyses
  /**
  * Reinitializes the class and splits the structure in separate lists, based on residue number.
  **/
  public void preChangeAnalyses(KPoint p) {
    newGroup();
    splitStructure(p);
  }
//}}}
  
//{{{ splitStructure
//##################################################################################################
  /**
  * Splits the parent list of a point into separate lists, based on residue number.
  **/
  public void splitStructure(KPoint p) {
    String pointID = p.getName().trim();
    KList parentList = (KList) p.getParent();
    //clickedLists.add(parentList);
    //KSubgroup parentGroup = (KSubgroup) parentList.getParent();
    KIterator<KPoint> points = KIterator.allPoints(parentList);
    for (KPoint point : points) {
    //Iterator iter = parentList.iterator();
    //KPoint point;
    //ArrayList<KPoint> listofPoints = new ArrayList<KPoint>();
    //while (iter.hasNext()) {
	    //point = (KPoint) iter.next();
	    //String master = getOldMaster(list);
	    Integer resNumber = new Integer(KinUtil.getResNumber(point.getName()));
	    ArrayList<KPoint> listofPoints = structMap.get(resNumber);
	    if (listofPoints == null) {
        listofPoints = new ArrayList<KPoint>();
	    } 
	    listofPoints.add(point);
	    structMap.put(resNumber, listofPoints);
    }
    //Set keySet = structMap.keySet();
    TreeSet<Integer> keys = new TreeSet<Integer>(structMap.keySet());
    lowResNum = (Integer) keys.first();
    highResNum = (Integer) keys.last();
  }
//}}}

//{{{ contains
//    public boolean contains(KPoint p) {
//	KList parentList = (KList) p.getParent();
//	Integer resNum = new Integer(KinUtil.getResNumber(p.getName()));
//	return (structMap.containsKey(resNum)&&clickedLists.contains(parentList));
//    }
//}}}	

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
        ArrayList<KPoint> listofPoints = structMap.get(hashKey);
        for (KPoint point : listofPoints) {
        //Iterator iter = listofLists.iterator();
        //while (iter.hasNext()) {
          //KPoint point = (KPoint) iter.next();
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

//{{{ highlightAA
  /**
  * For coloring particular amino acids.  
  **/
  public void highlightAA(KPoint p, String aaName, KPaint color, boolean colorPrior) {
    HashSet<Integer> aaNums = new HashSet<Integer>();
    KList parentList = (KList) p.getParent();
    KIterator<KPoint> points = KIterator.allPoints(parentList);
    for (KPoint point : points) {
    //Iterator iter = parentList.iterator();
    //while (iter.hasNext()) {
	    //KPoint point = (KPoint) iter.next();
	    String name = point.getName();
	    if (name.indexOf(aaName) != -1) {
        point.setColor(color);
        Integer resNum = new Integer(KinUtil.getResNumber(point.getName()));
        aaNums.add(resNum);
	    }
    }
    if (colorPrior) {
	    Iterator iter = aaNums.iterator();
	    while (iter.hasNext()) {
        int priorResNum = ((Integer) iter.next()).intValue() - 1;
        ArrayList<KPoint> listofPoints = structMap.get(new Integer(priorResNum));
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
//}}}
  
//{{{ newGroup
//######################################################################################################
    /**
     * Resets the tool by creating new hashmaps for the various list storing maps. This is for coloring
     * ribbons in different KGroups, or in new kinemages.
     **/
    public void newGroup() {
//	clickedLists = new HashSet();
	structMap = new HashMap<Integer, ArrayList<KPoint>>();
    }
//}}}

//{{{ onTable
  /**
  * Used for determining the amino acid sequence of a clicked-on structure.
  **/
  public String onTable() {
    TreeSet<Integer> keys = new TreeSet<Integer>(structMap.keySet());
    String aaText = "";
    int j = 0;
    for (int i = 1; i <= highResNum.intValue(); i++) {
	    if (j == 5) {
        aaText = aaText.concat(" ");
        j = 0;
	    }
	    j++;
	    if (keys.contains(new Integer(i))) {
        ArrayList<KPoint> list = structMap.get(new Integer(i));
        KPoint point = list.get(0);
        String aa = KinUtil.getResName(point);
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

