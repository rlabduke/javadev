// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;

import king.*;
import king.core.*;

import java.util.*;
import java.util.regex.*;

import driftwood.moldb2.AminoAcid;
import driftwood.util.*;
//}}}

public class KinPointIdParser {

  static Pattern chainResPattern = Pattern.compile("[A-Z][0-9]{4}");

  //{{{ Constructor
  public KinPointIdParser() {
  }
  //}}}
    
  //{{{ getFirstGroupName
  public static String getFirstGroupName(Kinemage kin) {
    Iterator kinIter = kin.iterator();
    //while (kinIter.hasNext()) {
      KGroup group = (KGroup) kinIter.next();
      return group.getName();
  }
  //}}}

  //{{{ getResNumString
  // similar to the other getResNumber, but this one is to keep
  // insertion code info.
  public static String getResNumString(KPoint point) {
    String name = point.getName().trim();
    String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
    // another pass to see if there are any AAName + int in name.
    if (parsed.length > 1) {
	    if (parsed[1].length() > 3) {
        String parseValue = parsed[1].substring(3);
        if (NumberUtils.isInteger(parseValue)) {
          //System.out.print(parseValue + " ");
          return parseValue;
        }
	    }
    }
    // one pass to see if there are any straight up ints in the name
    for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (NumberUtils.isInteger(parseValue)) {
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
        if (NumberUtils.isInteger(parseValue)) {
          //if (Integer.parseInt(parseValue)>0) {
            return parsed[i];
          //}
        }
	    }
    }
    return "";
  }
  //}}}
    
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
  * 
  **/
  
  public static int getResNumber(String name) {
    //String name = point.getName().trim();
    String[] uncleanParsed = name.trim().split(" ");
    //String[] parsed = new String[uncleanParsed.length];
    String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
    int parsedInt;
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
        if (NumberUtils.isInteger(parseValue)) {
          //System.out.print(parseValue + " ");
          return Integer.parseInt(parseValue);
        }
	    }
    }
    // one pass to see if there are any straight up ints in the name
    for (int i = parsed.length - 1; i > -1; i--) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (NumberUtils.isInteger(parseValue)) {
        parsedInt = Integer.parseInt(parseValue);
        if (parsedInt>0) {
          return parsedInt;
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
        if (NumberUtils.isInteger(parseValue)) {
          parsedInt = Integer.parseInt(parseValue);
          if (parsedInt>0) {
            return parsedInt;
          }
        }
	    }
    }
    // for chain-resnumber runons (only happens in large files (e.g. ribosome)
    for (int i = 0; i < parsed.length; i++) {
      Matcher matcher = chainResPattern.matcher(parsed[i]);
      if ((parsed[i] != null)&&(matcher.matches())) {
        String parseValue = parsed[i].substring(1, parsed[i].length());
        return Integer.parseInt(parseValue);
      }
    }
    //if (isNumeric(parsed[3].substring(0, parsed[3].length()-1))) {
      //    return Integer.parseInt(parsed[3].substring(0, parsed[3].length()-1));
    //}
    
    
    return -1;
  }
  //}}}
  
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

  
  public static String getAtomName(KPoint point) {
    String name = point.getName().trim();
    return getAtomName(name);
  }
  
  public static String getAtomName(String name) {
    //String[] uncleanParsed = name.split(" ");
    //String[] parsed = new String[uncleanParsed.length];
    //int i2 = 0;
    //// To clean out the empty strings from the split name.
    //
    //for (int i = 0; i < uncleanParsed.length; i++) {
	  //  String unclean = uncleanParsed[i];
	  //  if ((!unclean.equals(""))&&(!unclean.equals(" "))) {
    //    parsed[i2] = unclean;
    //    i2++;
	  //  }
    //}
    String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
    if (parsed[0].matches("\\([0-9]*\\).*")) return parsed[1];
    return parsed[0];
  }
  
  public static String getResName(KPoint point) {
    String name = point.getName().trim();
    return getResName(name);
  }
  
  // quick and dirty way of getting residue name.
  public static String getResName(String name) {
    String resname = AminoAcid.getAAName(name.substring(0,10));
    if (resname.trim().length() == 4) {
      resname = resname.substring(1);
    }
    if (!resname.equals("UNK")) {
      return resname;
    }

    String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
    resname = parsed[1];
    resname = String.format("%3s", resname);
    return resname;
    //while ((returnString.equals("")&&
    
  }
  
  //more robust way of getting residue name, but limited to real AAs.
  public static String getResAA(String name) {
    // substring is to prevent silly bug where the wrong amino acid type  
    // the pdbID that is sometimes in the pointIDs.
    String aaname = AminoAcid.getAAName(name.substring(0,10));
    if (aaname.trim().length() == 4) {
      return aaname.substring(1);
    }
    return aaname.trim();
  }
  
  public static String getAltConf(String name) {
    String aaname = AminoAcid.getAAName(name.substring(0, 10));
    if (aaname.trim().length() == 4) {
      return aaname.substring(0, 1);
    }
    else return " ";
  }
  
  public static double getOccupancy(KPoint point) {
    String name = point.getName().trim();
    String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
    for (String s : parsed) {
      if (s.matches("[0-9]+[.][0-9]+")) {
        return Double.parseDouble(s);
      }
      if (s.matches("[0-9]+[.][0-9]+B[0-9]+[.][0-9]+")) {
        String[] bsplit = Strings.explode(s, "B".charAt(0), false, true);
        return Double.parseDouble(bsplit[0]);
      }
    }
    return 1;
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
	    if (parseValue.matches("B[0-9]+[.][0-9]+")) {
        String bVal = parseValue.substring(1);
        return Double.parseDouble(bVal);
	    }
	    if (parseValue.matches("[0-9]+[.][0-9]+B[0-9]+[.][0-9]+")) {
        String[] bsplit = Strings.explode(parseValue, "B".charAt(0), false, true);
        return Double.parseDouble(bsplit[1]);
      }
    }
    return 0;
  }
  
  //{{{ getPdbName
  public static String getPdbName(String name) {
    String[] parsed = Strings.explode(name, " ".charAt(0), false, true);
    String lastVal = parsed[parsed.length - 1];
    if ((lastVal.length() > 3)&&(lastVal.charAt(0) != 'B')) {
      return lastVal;
    }
    return null;
  }
  //}}}
  
}
