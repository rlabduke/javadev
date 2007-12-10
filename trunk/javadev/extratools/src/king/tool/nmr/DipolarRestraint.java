// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;

//}}}

/**
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* Began Wed Nov 07 16:21:00 EST 2007
**/
public class DipolarRestraint implements NmrRestraint {

  //{{{ Constants
  //}}}
  
  //{{{ Variables
  double[] values;
  String fromName;
  String fromNum;
  String toName;
  String toNum;
  //}}}
  
  //{{{ Constructor
  public DipolarRestraint(String fName, String fNum, String tName, String tNum, double[] vals) {
    if (fName != null && fNum != null && tName != null && tNum != null && vals != null) {
      fromName = fName;
      fromNum = fNum.trim();
      toName = tName;
      toNum = tNum.trim();
      values = vals;
    } else {
      System.out.println("Missing value in constructing a dipolar restraint");
    }
  }
  //}}}
  
  //{{{ get functions
  public double[] getValues() {
    return values;
  }
  
  public String getFromName() {
    return fromName;
  }
  
  public String getFromNum() {
    return fromNum;
  }

  public String getToName() {
    return toName;
  }
  
  public String getToNum() {
    return toNum;
  }
  //}}}
  
  //{{{ toString
  public String toString() {
    String outString = "RDC restraint " + fromNum + " " + fromName + ":" + toNum +" " + toName;
    for (double d : values) {
      outString = outString.concat(" " + Double.valueOf(d));
    }
    return outString;
  }
  //}}}
}
