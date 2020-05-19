// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

import java.util.*;
import java.time.*;
//}}}
/**
* <code>Times</code> is a utility class for manipulating and formatting
* Date/time objects in a variety of ways.
*
* <p>Copyright (C) 2020 by Vincent B Chen. All rights reserved.
* <br>Begun on Mon May 18 15:00:38 EDT 2020 @833 /Internet Time/
*/
public class Times //extends ... implements ...
{
  
//{{{ Constructor
    /**
  Basic constructor for Times
  */
  public Times()
  {
  } 
//}}}

//{{{ getCurrentTimeString
  public static String getCurrentTimeString() {
    Instant timestamp = Instant.now();
    LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
    return String.format("%s %d %d at %d:%d", ldt.getMonth(), ldt.getDayOfMonth(),
                  ldt.getYear(), ldt.getHour(), ldt.getMinute());
  }

//}}}



  
}//class