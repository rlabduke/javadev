// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;

import king.*;
import king.core.*;

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
    HashMap structMap;
    HashSet clickedLists;

    // sortedKin is hashmap with masters as keys, arraylists as values
    // ribbonMap is hashmap with Klists as keys, arraylists as values
    HashMap sortedKin, ribbonMap;

    //Integer lowResNum, highResNum;

    String[] aaNames = {"gly", "ala", "ser", "thr", "cys", "val", "leu", "ile", "met", "pro", "phe", "tyr", "trp", "asp", "glu", "asn", "gln", "his", "lys", "arg"};


//{{{ Constructor(s)
//##############################################################################
    public RecolorRibbon()
    {
        //super(tb);
	newGroup();
	clickedLists = new HashSet();

        //undoStack = new LinkedList();
	//buildGUI();
    }
//}}}

    public void preChangeAnalyses(KPoint p) {
	KList parentList = (KList) p.getOwner();
	if (!ribbonMap.containsKey(parentList)) {
	    newGroup();
	    if (hasRibbonMasters(parentList)) {
		sortStructure(p);
	    } else {
		//setRibbonMode(false);
		//colorRegion.setSelected(true);
	    }
	    //System.out.println("sorting by number");
	    sortbyNumber(p);
	}
	//sortStructure(p);
	//sortbyNumber(p);
    }
    
//{{{ sortStructure
//##################################################################################################
    /**
     * Initializer function that handles the sorting of a prekin-generated ribbon kinemage.  It sorts
     * based on the second master of each KList in the kinemage.  This function needs to be called
     * before any of the highlighting functions can be called.
     **/
    
    public void sortStructure(KPoint p) {

	String pointID = p.getName().trim();
	KList parentList = (KList) p.getOwner();
	KSubgroup parentGroup = (KSubgroup) parentList.getOwner();
	Iterator iter = parentGroup.iterator();
	KList list;
	ArrayList listofLists = new ArrayList();
	//Kinemage kin = kMain.getKinemage();
	//KGroup group = new KGroup(kin, "sorted");

	while (iter.hasNext()) {
	    list = (KList) iter.next();
	    String master = getOldMaster(list);
	    listofLists = (ArrayList) sortedKin.get(master);
	    if (listofLists == null) {
		listofLists = new ArrayList();
	    } 
	    listofLists.add(list);
	    sortedKin.put(master, listofLists);
	}
	Set keys = sortedKin.keySet();
	iter = keys.iterator();
	while (iter.hasNext()) {
	    String master = (String) iter.next();
	    listofLists = (ArrayList) sortedKin.get(master);
	    splitStructure(listofLists);
	    //KSubgroup sub = new KSubgroup(group, master);
	    //Iterator listIter = listofLists.iterator();
	    //while (listIter.hasNext()) {
	    //	list = (KList) listIter.next();
	    //	sub.add(list);
	    //}
	    //group.add(sub);
	}	
	//kin.add(group);
	//initiated = true;
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
	KList parentList = (KList) p.getOwner();
	AGE parentGroup = (AGE) parentList.getOwner();
	Iterator iter = parentGroup.iterator();
	KList list;
	ArrayList listofLists = new ArrayList();
	
	while (iter.hasNext()) {
	    list = (KList) iter.next();
	    Iterator listIter = list.iterator();
	    KPoint point = (KPoint) listIter.next();
	    if (!point.isUnpickable()) { //to not color over black outlines on ribbons
		Integer resNum = new Integer(getResNumber(list));
		if (structMap.containsKey(resNum)) {
		    listofLists = (ArrayList) structMap.get(resNum);
		    listofLists.add(list);
		} else {
		    listofLists = new ArrayList();
		    listofLists.add(list);
		    structMap.put(resNum, listofLists);
		}
	    }
	}
	TreeSet keys = new TreeSet(structMap.keySet());
	lowResNum = (Integer) keys.first();
	highResNum = (Integer) keys.last();
    }
//}}}    

//{{{ splitStructure
//###################################################################################################
    /**
     * Helper function to sort srcLists into separate ribbons.  Stores in Hashmap with KList as key, and an 
     * arraylist of KLists (all of one ribbon) as values.
     **/
    private void splitStructure(ArrayList srcLists) {

	Iterator iter = srcLists.iterator();
	ArrayList listofLists = new ArrayList();
	Integer oldresNum = new Integer(-100);


	// gets each KList, gets first point of Klist, checks whether within one of previous list,
	// then sorts appropriately
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    Integer resNum = new Integer(getResNumber(list));
	    if ((resNum.equals(oldresNum))||(resNum.equals(new Integer(oldresNum.intValue() + 1)))) {
		listofLists.add(list);
		ribbonMap.put(list, listofLists);
		oldresNum = resNum;
	    } else {
		listofLists = new ArrayList();
		listofLists.add(list);
		ribbonMap.put(list, listofLists);
		oldresNum = resNum;
	    }
	}
    }
//}}}

    public boolean contains(KPoint p) {
	KList parentList = (KList) p.getOwner();
	Integer resNum = new Integer(getResNumber(p));
	return (structMap.containsKey(resNum)&&clickedLists.contains(parentList));
    }


    public void highlightRange(int firstNum, int secondNum, KPaint[] colors) {
	int index = 0;
	//Object colors[] = KPalette.getStandardMap().values().toArray();
	//Object colors[] = createColorArray(secondNum-firstNum+1);
	for (int i = firstNum; i <= secondNum; i++) {
	    //Object colors[] = createColorArray(secondNum-firstNum);
	    if (index >= colors.length) {
		index = 0;
	    }
	    Integer hashKey = new Integer(i);
	    
	    if (structMap.containsKey(hashKey)) {
		ArrayList listofLists = (ArrayList) structMap.get(hashKey);
		Iterator iter = listofLists.iterator();
		
		while (iter.hasNext()) {
		    
		    KList list= (KList) iter.next();
		    //if (hippieMode.isSelected()) {
		    //   point.setColor((KPaint) colors[index]);
		    //} else {
		    //    point.setColor((KPaint) color1.getSelectedItem());
		    //}
		    //if (chooseColor) {
		    //	point.setColor(color);
		    //} else {
			setPointColors(list, (KPaint) colors[index]);
			//}
		}
		index++;
	    }
	}
	//lowNumField.setText("");
	//highNumField.setText("");
    }

    public void highlightAll(KPoint p, KPaint[] colors) {
        highlightRange(lowResNum.intValue(), highResNum.intValue(), colors);
    }

    
    public void highlightAA(KPoint p, String aaName, KPaint color, boolean colorPrior) {
	KList parentList = (KList) p.getOwner();
	KSubgroup parentSub = (KSubgroup) parentList.getOwner();
	HashSet aaNums = new HashSet();
	Iterator iter = parentSub.iterator();
	while (iter.hasNext()) {
	    KList list = (KList) iter.next();
	    if (containsAAName(list, aaName)) {
		setPointColors(list, color);
		Integer resNum = new Integer(getResNumber(list));
		aaNums.add(resNum);
	    }
	}
	if (colorPrior) {
	    iter = aaNums.iterator();
	    while (iter.hasNext()) {
		int priorResNum = ((Integer) iter.next()).intValue() - 1;
		ArrayList listofLists = (ArrayList) structMap.get(new Integer(priorResNum));
		if (listofLists != null) {
		    Iterator listIter = listofLists.iterator();
		    while (listIter.hasNext()) {
			KList list = (KList) listIter.next();
			if (list != null) {
			    setPointColors(list, KPalette.green);
			}
		    }
		}
	    }
	}
    }
	    
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
		ArrayList alist = (ArrayList) structMap.get(new Integer(i));
		KList list = (KList) alist.get(0);
		Iterator iter = list.iterator();
		KPoint point = (KPoint) iter.next();
		//String pointID = point.getName().trim();
		//String aa = pointID.substring(4, 7);
		String aa = getResName(point);
		aaText = aaText.concat(AminoAcid.translate(aa));
	    } else {
		aaText = aaText.concat("-");
	    }
	}
	return aaText;
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
	ribbonMap = new HashMap();
	sortedKin = new HashMap();
    }
//}}}

//{{{ getOldMaster
//######################################################################################################
    /**
     * Helper function that (hopefully) gets the "ribbon master" (alpha, beta, coil) from list.  Depends on
     * prekin giving the ribbon master as the second master of the list.
     **/
    private String getOldMaster(KList list) {
	Iterator masIter = list.masterIterator();
	String oldMaster = (String) masIter.next();
	//while (masIter.hasNext()) {

	//if (oldMaster.equals("ribbon")) {

		oldMaster = (String) masIter.next();

		//}
		//}

	return oldMaster;
    }
//}}}

    private boolean containsAAName(KList list, String aaName) {
	Iterator iter = list.iterator();
	KPoint point = (KPoint) iter.next();
	String name = point.getName();
	return (name.indexOf(aaName) != -1);
    }

    private boolean hasRibbonMasters(KList list) {
	Iterator masIter = list.masterIterator();
	if (!(masIter == null)) {
	    while (masIter.hasNext()) {
		String oldMaster = (String) masIter.next();
		if ((oldMaster.equals("coil"))||(oldMaster.equals("beta"))||(oldMaster.equals("alpha"))) {
		    return true;
		} 
	    }
	}
	return false;
    }

    //public int numofResidues() {
    //	return highResNum.intValue()-lowResNum.intValue()+1;
    //}

}
