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
 * <code>RecolorRibbon</code> implements Recolorator to allow recoloring of
 * kinemage sections that are ribbons.
 *
 * <p>Copyright (C) 2005 by Vincent B. Chen. All rights reserved.
 * <br>Begun on Feb 20 2005
 **/


public class RecolorRibbon extends Recolorator //implements ActionListener 
{

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################
    HashMap<Integer, ArrayList<ArrayList>> structMap;
    //HashSet clickedLists;

    // sortedKin is hashmap with masters as keys, arraylists as values
    // ribbonMap is hashmap with Klists as keys, arraylists as values
    HashMap<String, ArrayList<KList>> sortedKin;
    HashMap<KList, ArrayList<KList>> ribbonMap;
    
    //Integer lowResNum, highResNum;

    String[] aaNames = {"gly", "ala", "ser", "thr", "cys", "val", "leu", "ile", "met", "pro", "phe", "tyr", "trp", "asp", "glu", "asn", "gln", "his", "lys", "arg"};
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RecolorRibbon()
    {
	newGroup();
    }
//}}}

//{{{ preChangeAnalyses
  /**
  * Splits up a ribbon structure, derived from a clicked point, into Klists
  * which all correspond to one residue.
  **/
  public void preChangeAnalyses(KPoint p) {
    KList parentList = (KList) p.getParent();
    if (!ribbonMap.containsKey(parentList)) {
	    newGroup();
	    if (hasRibbonMasters(parentList)) {
        sortStructure(p);
	    }
	    sortbyNumber(p);
    }
    //sortStructure(p);
    //sortbyNumber(p);
  }
//}}}
  
  //{{{ newGroup
//######################################################################################################
    /**
     * Resets the tool by creating new hashmaps for the various list storing maps. This is for coloring
     * ribbons in different KGroups, or in new kinemages.
     **/
    public void newGroup() {
	//clickedLists = new HashSet();
	structMap = new HashMap<Integer, ArrayList<ArrayList>>();
	ribbonMap = new HashMap<KList, ArrayList<KList>>();
	sortedKin = new HashMap<String, ArrayList<KList>>();
    }
//}}}
    
//{{{ sortStructure
//##################################################################################################
    /**
     * Initializer function that handles the sorting of a prekin-generated ribbon kinemage.  It sorts
     * based on the second master of each KList in the kinemage.  This function needs to be called
     * before any of the highlighting functions can be called.
     **/
    
  public void sortStructure(KPoint p) {
    String pointID = p.getName().trim();
    KList parentList = (KList) p.getParent();
    KGroup parentGroup = (KGroup) parentList.getParent();
    KIterator<KList> lists = KIterator.allLists(parentGroup);
    for (KList list : lists) {
    //Iterator iter = parentGroup.iterator();
    //KList list;
    //ArrayList listofLists = new ArrayList();
    //while (iter.hasNext()) {
	    //list = (KList) iter.next();
	    String master = getRibbonMaster(list);
	    ArrayList<KList> listofLists = sortedKin.get(master);
	    if (listofLists == null) {
        listofLists = new ArrayList<KList>();
	    } 
	    listofLists.add(list);
	    sortedKin.put(master, listofLists);
    }
    for (ArrayList<KList> sortList : sortedKin.values()) {
      splitStructure(sortList);
    }
    /* pre 2.0
    Set keys = sortedKin.keySet();
    iter = keys.iterator();
    while (iter.hasNext()) {
	    String master = (String) iter.next();
	    //listofLists = (ArrayList) sortedKin.get(master);
	    splitStructure((ArrayList) sortedKin.get(master));
    }	*/
  }
  //}}}

//{{{ splitStructure
//###################################################################################################
    /**
     * Helper function to sort srcLists into separate ribbons.  Stores in Hashmap with KList as key, and an 
     * arraylist of KLists (all of one ribbon) as values.
     **/
  private void splitStructure(ArrayList<KList> srcLists) {
    //Iterator iter = srcLists.iterator();
    //ArrayList listofLists = new ArrayList();
    Integer oldresNum = new Integer(-100);
    // gets each KList, gets first point of Klist, checks whether within one of previous list,
    // then sorts appropriately
    ArrayList<KList> listofLists = null;
    for (KList list : srcLists) {
    //while (iter.hasNext()) {
	    //KList list = (KList) iter.next();
      KPoint point = list.getChildren().get(0);
	    Integer resNum = new Integer(KinUtil.getResNumber(point));
	    if ((resNum.equals(oldresNum))||(resNum.equals(new Integer(oldresNum.intValue() + 1)))) {
        listofLists.add(list);
        ribbonMap.put(list, listofLists);
        oldresNum = resNum;
	    } else {
        listofLists = new ArrayList<KList>();
        listofLists.add(list);
        ribbonMap.put(list, listofLists);
        oldresNum = resNum;
	    }
    }
  }
  //}}}
  
//{{{ sortbyNumber
//####################################################################################################
    /**
     * Helper function to sort p's parent by residue number. Stores in Hashmap with residue number as key, and
     * arraylist of KLists as value.  
     **/
  private void sortbyNumber(KPoint p) {
    String pointID = p.getName().trim();
    KList parentList = (KList) p.getParent();
    AGE parentGroup = (AGE) parentList.getParent();
    KIterator<KList> lists = KIterator.allLists(parentGroup);
    //Iterator iter = parentGroup.iterator();
    //KList list;
    //ArrayList listofLists = new ArrayList();
    for (KList list : lists) {
      ArrayList<ArrayList<KPoint>> residueLists = splitLists(list); 
      for (ArrayList splitList : residueLists) { // at this point, each splitlist should only have points from one res.
	    //Iterator residueIter = residueLists.iterator();
	    //while (residueIter.hasNext()) {
        //ArrayList splitList = (ArrayList) residueIter.next();
        //Iterator listIter = splitList.iterator();
        KPoint point = (KPoint) splitList.get(0);
        if (!point.isUnpickable()) { //to not color over black outlines on ribbons
          Integer resNum = new Integer(KinUtil.getResNumber(point.getName()));
          //System.out.println(resNum);
          ArrayList<ArrayList> listofLists = null;
          if (structMap.containsKey(resNum)) {
            listofLists = structMap.get(resNum);
            listofLists.add(splitList);
          } else {
            listofLists = new ArrayList<ArrayList>();
            listofLists.add(splitList);
            structMap.put(resNum, listofLists);
          }
        }
	    }
    }
    TreeSet<Integer> keys = new TreeSet<Integer>(structMap.keySet());
    lowResNum = (Integer) keys.first();
    highResNum = (Integer) keys.last();
  }
//}}}    

//{{{ splitLists
  /**
  *  Splits up KLists that contain more than one residue into separate arraylists.
  **/
  public ArrayList<ArrayList<KPoint>> splitLists(KList bigList) {
    Iterator iter = bigList.iterator();
    int oldResNum = -100;
    //KList list = new KList(bigList.getType());
    ArrayList<KPoint> list = new ArrayList<KPoint>();
    ArrayList<ArrayList<KPoint>> residueList = new ArrayList<ArrayList<KPoint>>();
    while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    //oldResNum = getResNumber(point);
	    if (KinUtil.getResNumber(point.getName()) != oldResNum) {
        //list = new KList(bigList.getType());
        list = new ArrayList<KPoint>();
        list.add(point);
        oldResNum = KinUtil.getResNumber(point.getName());
        residueList.add(list);
	    } else {
        list.add(point);
	    }
    }
    return residueList;
  }
//}}}

//{{{ contains
//    public boolean contains(KPoint p) {
      /**
        I used to keep track of which parts of a structure had been clicked,
        but King seems to be fast enough now that this isn't necessary (for ribbons).
      **/
	//KList parentList = (KList) p.getParent();
	//Integer resNum = new Integer(KinUtil.getResNumber(p));
	//return (structMap.containsKey(resNum)&&clickedLists.contains(parentList));
//  return false;
//    }
//}}}

//{{{ highlightRange
  /**
  * Changes the colors of the points in a range of residue numbers, based on the KPaints
  * in colors.  
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
          ArrayList list= (ArrayList) iter.next();
          setPointColors(list, (KPaint) colors[index]);
        }
        index++;
	    }
    }
  }
//}}}  

//{{{ highlightAll
  /**
  * Colors a whole structure, based on the KPaints in colors.
  **/
    public void highlightAll(KPoint p, KPaint[] colors) {
    //System.out.println(lowResNum + " " + highResNum);
    highlightRange(lowResNum.intValue(), highResNum.intValue(), colors);
  }
//}}}
    
//{{{ highlightAA
  /**
  * Colors all the points corresponding to a particular amino acid name.
  **/
  public void highlightAA(KPoint p, String aaName, KPaint color, boolean colorPrior) {
    KList parentList = (KList) p.getParent();
    KGroup parentSub = (KGroup) parentList.getParent();
    HashSet<Integer> aaNums = new HashSet<Integer>();
    KIterator<KList> lists = KIterator.allLists(parentSub);
    for (KList list : lists) {
    //Iterator iter = parentSub.iterator();
    //while (iter.hasNext()) {
	    //KList list = (KList) iter.next();
	    ArrayList<ArrayList<KPoint>> resLists = splitLists(list);
      for (ArrayList splitList : resLists) {
	    //Iterator resIter = resLists.iterator();
	    //while (resIter.hasNext()) {
        //ArrayList splitList = (ArrayList) resIter.next();
        if (containsAAName(splitList, aaName)) {
          setPointColors(splitList, color);
          Integer resNum = new Integer(KinUtil.getResNumber((KPoint)splitList.get(0)));
          aaNums.add(resNum);
        }
	    }
    }
    if (colorPrior) {
	    Iterator iter = aaNums.iterator();
	    while (iter.hasNext()) {
        int priorResNum = ((Integer) iter.next()).intValue() - 1;
        ArrayList listofLists = (ArrayList) structMap.get(new Integer(priorResNum));
        if (listofLists != null) {
          Iterator listIter = listofLists.iterator();
          while (listIter.hasNext()) {
            ArrayList list = (ArrayList) listIter.next();
            if (list != null) {
              setPointColors(list, KPalette.green);
            }
          }
        }
	    }
    }
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
        ArrayList alist = (ArrayList) structMap.get(new Integer(i));
        ArrayList list = (ArrayList) alist.get(0);
        Iterator iter = list.iterator();
        KPoint point = (KPoint) iter.next();
        //String pointID = point.getName().trim();
        //String aa = pointID.substring(4, 7);
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

//{{{ getRibbonMaster
//######################################################################################################
    /**
     * Helper function that (hopefully) gets the "ribbon master" (alpha, beta, coil) from list.  Depends on
     * prekin giving the ribbon master as the second master of the list.
     **/
  private String getRibbonMaster(KList list) {
    Collection<String> masters = list.getMasters();
    for (String mast : masters) {
      if (mast.equals("alpha")||mast.equals("beta")||mast.equals("coil")) {
        return mast;
	    }
    }
    /* Pre 2.0
    Iterator masIter = list.masterIterator();
    while (masIter.hasNext()) {
	    String oldMaster = (String) masIter.next();
	    if (oldMaster.equals("alpha")||oldMaster.equals("beta")||oldMaster.equals("coil")) {
        return oldMaster;
	    }
    }*/
    return "";
  }
//}}}

//{{{ containsAAName
  /**
  * Checks to see whether the name of the first point in a list (and presumably all the points in that list) 
  * contains the given aaName.
  **/
  private boolean containsAAName(ArrayList list, String aaName) {
	Iterator iter = list.iterator();
	KPoint point = (KPoint) iter.next();
	String name = point.getName();
	return (name.indexOf(aaName) != -1);
    }
//}}}
    
//{{{ hasRibbonMasters
  /**
  * Determines whether a KList has masters corresponding to ribbon masters (alpha, beta, or coil).
  **/
  private boolean hasRibbonMasters(KList list) {
    Collection<String> masters = list.getMasters();
    for (String mast : masters) {
      if (mast.equals("alpha")||mast.equals("beta")||mast.equals("coil")) {
        return true;
	    }
    }
    /* pre 2.0
    Iterator masIter = list.masterIterator();
    if (!(masIter == null)) {
	    while (masIter.hasNext()) {
        String oldMaster = (String) masIter.next();
        if ((oldMaster.equals("coil"))||(oldMaster.equals("beta"))||(oldMaster.equals("alpha"))) {
          return true;
        } 
	    }
    }*/
    return false;
  }
//}}}

}//class
