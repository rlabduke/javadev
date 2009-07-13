// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package rdcvis;

import java.util.*;

//}}}

/**
* <code>MagneticResonanceFile</code> is an object representing an NMR restraints (.mr) files.  It probably
* only works with .mr files from certain programs.
* 
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* <br>Begun on Fri Nov 09 13:57:53 EST 2007
**/

public class MagneticResonanceFile {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  HashMap dipolarCouplings;
  //}}}
  
  //{{{ Constructor
  public MagneticResonanceFile() {
    dipolarCouplings = new HashMap();
  }
  //}}}
  
  //{{{ addDipolarCoupling
  /** This function adds both the name and the reversed name of an RDC to the map
      to try to get around if a user specifies an unexpected name (N-HN vs HN-N) **/
  public void addDipolarCoupling(DipolarRestraint dr) {
    String drName = processName(dr);
    addCouplingToMap(dr, drName);
    String drRevName = processReverseName(dr);
    addCouplingToMap(dr, drRevName);
  }
  
  private void addCouplingToMap(DipolarRestraint dr, String drName) {
    //System.out.println(drName);
    if (dipolarCouplings.containsKey(drName)) {
      TreeMap couplingMap = (TreeMap) dipolarCouplings.get(drName);
      String drNum = dr.getFromNum().trim();
      if (couplingMap.containsKey(drNum)) {
        System.out.println("The dipolar restraint "+drName+" map already has an entry for "+drNum);
      } else {
        couplingMap.put(drNum, dr);
      }
    } else {
      TreeMap couplingMap = new TreeMap();
      couplingMap.put(dr.getFromNum().trim(), dr);
      dipolarCouplings.put(drName, couplingMap);
    }
  }
  //}}}
  
  //{{{ getRdcTypeSet
  public Set getRdcTypeSet() {
    return dipolarCouplings.keySet();
  }
  //}}}
  
  //{{{ getRdcMapforType
  public TreeMap getRdcMapforType(String name) {
    if (dipolarCouplings.containsKey(name)) {
      return (TreeMap) dipolarCouplings.get(name);
    }
    return null;
  }
  //}}}
  
  //{{{ processName
  public String processName(DipolarRestraint dr) {
    String fName = dr.getFromName().trim();
    String tName = dr.getToName().trim();
    if (fName.endsWith("#")) fName = fName.substring(0, fName.length()-1);
    if (tName.endsWith("#")) tName = tName.substring(0, tName.length()-1);
    return fName+"-"+tName;
  }
  //}}}
  
  //{{{ processReverseName
  public String processReverseName(DipolarRestraint dr) {
    String fName = dr.getFromName().trim();
    String tName = dr.getToName().trim();
    if (fName.endsWith("#")) fName = fName.substring(0, fName.length()-1);
    if (tName.endsWith("#")) tName = tName.substring(0, tName.length()-1);
    return tName+"-"+fName;
  }
  //}}}
  
}
