// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.xtal;
import king.*;
import king.core.*;

//import javax.swing.*;
//import java.awt.event.*;
//import java.awt.*;
import java.util.*;
import javax.swing.event.*;

import driftwood.gui.*;
//import driftwood.r3.*;
//}}}




public class HyperRotParser implements ListSelectionListener {

    //{{{ Constants
   

    //}}}

//{{{ Variable definitions
//##################################################################################################

    HashMap hypMap = null; // name of config-->ArrayList of bondRots
    String[] hypNames;
    //JList hypList;

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public HyperRotParser(String input) {
	hypMap = new HashMap();
	findLinks(new String(input));
    }


//}}}

    private void buildGUI() {


    }

    public void findLinks(String text) {
	//System.out.println(text);
	ArrayList hypList = new ArrayList();
	int startHyp = 0;
	int endHyp = 0;
	while (startHyp >= 0) {
	    startHyp = text.indexOf("*{");
	    endHyp = text.indexOf("}*");
	    if (startHyp >= 0) {
		String hypText = text.substring(startHyp + 2, endHyp);
		if (isHyper(hypText)) {
		    hypList.add(hypText);
		}
		text = text.substring(endHyp + 2);
	    }
	}
	Iterator iter = hypList.iterator();
	hypNames = new String[hypList.size()];
	int i = 0;
	while (iter.hasNext()) {
	    String hypText = (String) iter.next();
	    String hypName = hypText.substring(0, hypText.indexOf(" "));
	    String rots = hypText.substring(hypText.indexOf("rot="));
	    hypNames[i] = hypName;
	    i++;
	    hypMap.put(hypName, extractRotInfo(rots));
	}
    }

    public ArrayList extractRotInfo(String text) {
	String rotName = null;
	int startName = 0;
	int endName = -1;
	double rotAng = -1;
	ArrayList bondRots = new ArrayList();
	
	while (startName >= 0) {
	    startName = text.indexOf("rot={");
	    if (startName >= 0) {
		endName = text.indexOf("}");
		rotName = text.substring(startName + 5, endName);
		//System.out.println(rotName);

		text = text.substring(endName + 1).trim();
		//System.out.println("rest: " + text);
		int endAngSp = text.indexOf(" ");
		int endAngNL = text.indexOf("\n");
		int endAng;
		if (endAngSp < 0) endAng = endAngNL;
		else if (endAngNL < 0) endAng = endAngSp;
		else if (endAngSp < endAngNL) endAng = endAngSp;
		else endAng = endAngNL;
		//System.out.println("endAng " + endAng);
		if (endAng >= 0) {
		    rotAng = Double.parseDouble(text.substring(0, endAng));
		    text = text.substring(endAng + 1);
		} else {
		    rotAng = Double.parseDouble(text);
		}
		BondRot store = new BondRot(1, rotName, rotAng);
		bondRots.add(store);

	    }
	}
	return bondRots;
    }
    /*
    public String extractRotName(String text) {
	String rotName = null;
	int startName = text.indexOf("rot={");
	if (startName > 0) {
	    int endName = text.indexOf("}");
	    rotName = text.substring(startName + 5, endName);
	    System.out.println(rotName);
	}
	return rotName;
	}*/

    public String[] getHypList() {
	return hypNames;
    }

    public ArrayList getRotList(String rotName) {
	return (ArrayList) hypMap.get(rotName);
    }

    private boolean isHyper(String text) {
	return (text.indexOf("rot=")>0);
    }

    public void valueChanged(ListSelectionEvent ev) {

    }


//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    
    public String toString() { return "HyperRotParser"; }
//}}}




    public void debug() {

	/*
	BondRot[] bondRots = new BondRot[dialMap.size()];
	Iterator iter = (dialMap.values()).iterator();
	for (int i = 0; i < dialMap.size(); i++) {
	    bondRots[i] = (BondRot) iter.next();
	}
	rotList = new JList(bondRots);
        rotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rotList.addListSelectionListener(this);
	*/
    }
	    

}//class
