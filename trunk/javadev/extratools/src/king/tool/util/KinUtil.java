// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;

import king.*;
import king.core.*;

import java.util.*;

import driftwood.moldb2.AminoAcid;
import driftwood.util.Strings;

public class KinUtil {


    public KinUtil() {
    }

    
    public static String getFirstGroupName(Kinemage kin) {
	Iterator kinIter = kin.iterator();
	//while (kinIter.hasNext()) {
	KGroup group = (KGroup) kinIter.next();
	return group.getName();
    }


    // similar to the other getResNumber, but this one is to keep
    // insertion code info.
    public static String getResNumString(KPoint point) {
	String name = point.getName().trim();
	String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
	// another pass to see if there are any AAName + int in name.
	if (parsed.length > 1) {
	    if (parsed[1].length() > 3) {
		String parseValue = parsed[1].substring(3);
		if (isInteger(parseValue)) {
		    //System.out.print(parseValue + " ");
		    return parseValue;
		}
	    }
	}
	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (isInteger(parseValue)) {
		//if (Integer.parseInt(parseValue)>0) { // resnumbers can be neg!?
		    return parseValue;
		    //}
	    }
	}
	
	// for insertions (1a, 1b, 1c, etc).
	//System.out.println(parsed[3]);
	//System.out.println(parsed[3].length());
	for (int i = 0; i < parsed.length; i++) {
	    if (parsed[i] != null) {
		String parseValue = parsed[i].substring(0, parsed[i].length()-1);
		//System.out.println(parseValue + ", " + i);
		if (isInteger(parseValue)) {
		    //if (Integer.parseInt(parseValue)>0) {
			return parsed[i];
			//}
		}
	    }
	}
	//if (isNumeric(parsed[3].substring(0, parsed[3].length()-1))) {
	//    return Integer.parseInt(parsed[3].substring(0, parsed[3].length()-1));
	//}


	return "";
	
    }

    public static String getLastString(String name) {
	String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
	return parsed[parsed.length - 1];
    }

    public static int getResNumber(KPoint point) {
	String name = point.getName().trim();
	return getResNumber(name);
    }


//{{{ getResNumber
//###################################################################################################
    /**
     * Helper function to get the residue number of parentList.  It gets the first KPoint in the KList, 
     * and extracts the residue number from the name.  EXTREMELY dependent on the format of the name of the KPoint.
     **/
    
    public static int getResNumber(String name) {
	//String name = point.getName().trim();
	String[] uncleanParsed = name.split(" ");
	//String[] parsed = new String[uncleanParsed.length];
	String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
        int i2 = 0;
	// To clean out the empty strings from the split name.
	
	//for (int i = 0; i < uncleanParsed.length; i++) {
	//    String unclean = uncleanParsed[i];
	//    if ((!unclean.equals(""))&&(!unclean.equals(" "))) {
	//		parsed[i2] = unclean;
	//	i2++;
	//    }
	//}
	// another pass to see if there are any AAName + int in name.
	if (parsed.length > 1) {
	    if (parsed[1].length() > 3) {
		String parseValue = parsed[1].substring(3);
		if (isInteger(parseValue)) {
		    //System.out.print(parseValue + " ");
		    return Integer.parseInt(parseValue);
		}
	    }
	}
	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (isInteger(parseValue)) {
		if (Integer.parseInt(parseValue)>0) {
		    return Integer.parseInt(parseValue);
		}
	    }
	}
	
	// for insertions (1a, 1b, 1c, etc).
	//System.out.println(parsed[3]);
	//System.out.println(parsed[3].length());
	for (int i = 0; i < parsed.length; i++) {
	    if (parsed[i] != null) {
		String parseValue = parsed[i].substring(0, parsed[i].length()-1);
		//System.out.println(parseValue + ", " + i);
		if (isInteger(parseValue)) {
		    if (Integer.parseInt(parseValue)>0) {
			return Integer.parseInt(parseValue);
		    }
		}
	    }
	}
	//if (isNumeric(parsed[3].substring(0, parsed[3].length()-1))) {
	//    return Integer.parseInt(parsed[3].substring(0, parsed[3].length()-1));
	//}


	return -1;
    }

    public static String getChainID(KPoint point) {
	String name = point.getName().trim();
	return getChainID(name);
    }

    public static String getChainID(String name) {
	String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
	if (parsed.length > 2) {
	    if (parsed[2].length() == 1) {
		return parsed[2];
	    }
	}
	return " ";
    }

    public static boolean isInteger(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	} catch (NullPointerException e) {
	    return false;
	}
    }

    public static boolean isNumeric(String s) {
	try {
	    Double.parseDouble(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    public static String getAtomName(KPoint point) {
	String name = point.getName().trim();
	return getAtomName(name);
    }

    public static String getAtomName(String name) {
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
	return parsed[0];
    }

    public static String getResName(KPoint point) {
	String name = point.getName().trim();
	return getResName(name);
    }

    // quick and dirty way of getting residue name.
    public static String getResName(String name) {
	//String name = point.getName().trim();
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

    }
    
    //more robust way of getting residue name, but limited to real AAs.
    public static String getResAA(String name) {
	String[] uncleanParsed = name.split(" ");
	String[] parsed = new String[uncleanParsed.length];
        int i2 = 0;
	// To clean out the empty strings from the split name.
	
	for (int i = 0; i < uncleanParsed.length; i++) {
	    String unclean = uncleanParsed[i];
	    //System.out.println(unclean);
	    if ((!unclean.equals(""))&&(!unclean.equals(" "))&&(unclean != null)) {
		parsed[i2] = unclean;
		i2++;
	    }
	    //if (unclean
	}
	for (int i = 0; i < i2; i++) {
	    String parseValue = parsed[i];
	    if (parseValue == null) {
		System.out.println(name + ", " + i + ", " + parsed.length);
	    }
	    //System.out.println(parseValue);
	    //System.out.println(parseValue + ", " + i);
	    if (parseValue.length()==3){
		if (AminoAcid.isAminoAcid(parseValue)) {
		    return parseValue;
		}
	    }
	}
	return "UNK";
    }

    public static double getBvalue(KPoint point) {
	String name = point.getName().trim();
	return getBvalue(name);
    }

    public static double getBvalue(String name) {
	//String[] uncleanParsed = name.split(" ");
	//String[] parsed = new String[uncleanParsed.length];
	String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    if (parseValue.charAt(0) == 'B') {
		String bVal = parseValue.substring(1);
		return Double.parseDouble(bVal);
	    }
	}
	return 0;
    }
}
