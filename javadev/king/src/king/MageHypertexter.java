// (jEdit options) :folding=explicit:collapseFolds=1:
package king;
import king.core.*;

//import java.util.StringTokenizer;
import java.util.*;

public class MageHypertexter implements MageHypertextListener
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;

//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public MageHypertexter(KingMain kmain) {
	this.kMain = kmain;
    }

    public void mageHypertextHit(String link) {
	//link = link.toLowerCase();
	StringTokenizer commaToks = new StringTokenizer(link, ",");
	while (commaToks.hasMoreTokens()) {
	    String origToken = commaToks.nextToken().trim();
	    if (origToken.indexOf("{")>-1) {
		//String nextToken = token;
		while (origToken.indexOf("}")<0) {
		    String nextToken = commaToks.nextToken();
		    origToken = origToken + "," + nextToken;
		}
	    }
	    String token = origToken.toLowerCase();
	    if (token.indexOf("alloff")>-1) {
		doAllOffToken();
	    }
	    if ((token.indexOf("kinemage")>-1) || (token.indexOf("kin"))>-1) {
		doKinToken(token);
	    }
	    if ((token.indexOf("view")>-1) || (token.indexOf("v=")>-1)) {
		doViewToken(token);
	    }
	    if ((token.indexOf("master=")>-1) || (token.indexOf("m=")>-1)) {
		doMasterToken(origToken);
	    } else {
		//System.out.println(token);
	    }
	}
	//if ((link.indexOf("kinemage")>-1) || (link.indexOf("kin"))>-1) {
	    
	
    }

    public void doKinToken(String token) {
	int firstSpace = token.indexOf(" ") + 1;
	int secSpace = token.indexOf(" ", firstSpace);
	String kinNum;
	if (secSpace > -1) {
	    kinNum = token.substring(firstSpace, secSpace);
	} else {
	    kinNum = token.substring(firstSpace);
	}
	//System.out.println("." + token.substring(firstSpace, secSpace) + ".");
	kMain.getStable().changeCurrentKinemage(kinNum);
    }
    
    public void doViewToken(String token) {
	int firstSpace;
	if (token.indexOf("view ")>-1) {
	    firstSpace = token.indexOf("view ") + 5;
	} else {
	    firstSpace = token.indexOf("v=") + 2;
	}
	String viewNumString = token.substring(firstSpace);
	int viewNum = Integer.valueOf(viewNumString).intValue();
	Kinemage kin = kMain.getStable().getKinemage();
	KingView view = (KingView)kin.getViewList().get(viewNum - 1);
	kin.notifyViewSelected(view);
    }

    public void doMasterToken(String token) {
	int openBrack = token.indexOf("{") + 1;
	int endBrack = token.indexOf("}");
	String masterName = token.substring(openBrack, endBrack);
	String masterAlive = token.substring(endBrack+2);
	boolean mAlive = false;
	if (masterAlive.equals("on")) {
	    mAlive = true;
	}
	//System.out.println(masterName + " " + masterAlive);
	Kinemage kin = kMain.getKinemage();
	kin.getMasterByName(masterName).setOn(mAlive);
    }

    public void doAllOffToken() {
	Kinemage kin = kMain.getKinemage();
	Collection masters = kin.masterList();
	Iterator iter = masters.iterator();
	while (iter.hasNext()) {
	    MasterGroup master = (MasterGroup) iter.next();
	    master.setOn(false);
	}
    }

}//class

