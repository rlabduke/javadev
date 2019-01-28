// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

import java.util.regex.*;
//}}}

//}}}
/**
* <code>NumberUtils</code> is a utility class for manipulating and formatting
* Number objects in a variety of ways.
*
* <br>Begun on Mon Jan 28 15:26:01 EST 2019 @893 /Internet Time/
*/

public class NumberUtils {
  
  static Pattern intPattern = Pattern.compile("[0-9]+");

  public NumberUtils() {}
  
  public static boolean isInteger(String s) {
    //try {
	  //  Integer.parseInt(s);
	  //  return true;
    //} catch (NumberFormatException e) {
	  //  return false;
    //} catch (NullPointerException e) {
	  //  return false;
    //}
    Matcher matcher = intPattern.matcher(s);
    return matcher.matches();
  }
  
  public static boolean isNumeric(String s) {
    try {
	    Double.parseDouble(s);
	    return true;
    } catch (NumberFormatException e) {
	    return false;
    }
  }
  
}