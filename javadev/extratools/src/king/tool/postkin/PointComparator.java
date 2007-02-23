// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.points.*;
import king.tool.util.*;

import driftwood.moldb2.*;
import driftwood.util.*;
//import chiropraxis.kingtools.*;
import java.util.*;
import java.io.*;
//}}}
/**
* <code>PointComparator</code> is a comparator used to compare AbstractPoints and determine
* which would come first, as dictated by PDB format.  This is used in the KinFudgerTool
* and was originally made to export PDBs from the small peptides used for Ramachandran plots.
* 
* Its sorting function is based on the idea of Radix sort, which sorts based on the least
* significant digit first.  In this case, the points should be sorted by atom name first,
* then residue name, and then residue number.  Naturally, this comparator is extremely dependent 
* on the format of the pointID; slight differences in the ID will make this code cry like a little 
* schoolgirl.
*
* <p>Copyright (C) 2005 by Vincent Chen. All rights reserved.
* 
*/
public class PointComparator implements Comparator {
  

  //{{{ Constants

  //}}}
  
  //{{{ Variable definitions
  //##############################################################################    
  //String[] atomNames = {" N  ", " CA ", " C  ", " O  ", " CB ", " CG ", " CD ", " CE ", " NZ "};
  Props     scProps;
  ArrayList heteroAtoms;
  ArrayList hydrogens;
  static ArrayList allAtoms;
  //}}}
  
  //{{{ Constructor(s)
  //##################################################################################################
  /**
  * Constructor
  */
  public PointComparator() {
    
    heteroAtoms = new ArrayList();
    hydrogens = new ArrayList();
    extractAAs();
    heteroAtoms.addAll(hydrogens);
    allAtoms = heteroAtoms;
    //System.out.println(allAtoms);
  }
  //}}}
  
  //{{{ compare
  //##################################################################################################
  /**
  * Compares two objects, and if they are abstract points, returns a value according to 
  * specification given for Comparator.
  */
  public int compare(Object o1, Object o2) {
    long value = 0;
    if ((o1 instanceof AbstractPoint) && (o2 instanceof AbstractPoint)) {
	    AbstractPoint point1 = (AbstractPoint) o1;
	    AbstractPoint point2 = (AbstractPoint) o2;
	    String p1name = point1.getName().toUpperCase();
	    String p2name = point2.getName().toUpperCase();
	    //value = Integer.parseInt(p1name.substring(8)) - Integer.parseInt(p2name.substring(8));
	    String atom1 = p1name.substring(0, 4);
	    String atom2 = p2name.substring(0, 4);
	    //System.out.println(atom1 + ", " + atom2);
	    //int atomPosition = allAtoms.indexOf(atom1) - allAtoms.indexOf(atom2);
	    int atomPosition = getAtomNamePosition(p1name) - getAtomNamePosition(p2name);
	    value = atomPosition * 10;
      
	    
	    value = value + KinUtil.getResAA(p1name).compareTo(KinUtil.getResAA(p2name)) * 1000;
      
	    
	    value = value + (KinUtil.getResNumber(p1name) - KinUtil.getResNumber(p2name)) * 1000000;
	    //return value;
	    if (value == 0) {
        //System.out.println(p1name + ", " + p2name + ": " + value);
	    }
    }
    //return value;
    if (value < 0) {
	    return -1;
    } else if (value == 0) {
	    return 0;
    } else {
	    return 1;
    }
  }
  //}}}
    
  //{{{ extractAAs
  //##################################################################################################
  /**
  * Loads atom connect info from sc-connect.props.
  *
  */
  public void extractAAs() {
    // Load side chain connectivity database
    scProps = new Props();
    try
    {
      InputStream is = getClass().getResourceAsStream("sc-connect.props");
      if(is != null)
      {
        scProps.load(is);
        is.close();
      }
      else SoftLog.err.println("Couldn't find sc-connect.props");
    }
    catch(IOException ex)
    { ex.printStackTrace(SoftLog.err); }
    //scProps.list(System.out);
    
    Set scs = scProps.keySet();
    Iterator iter = (new TreeSet(scs)).iterator();
    while (iter.hasNext()) {
	    String key = (String) iter.next();
	    if (key.indexOf(".hy") > 0) {
        buildAALists(hydrogens, scProps.getString(key, ""), "hy");
	    } else {
        buildAALists(heteroAtoms, scProps.getString(key, ""), "hetero");
	    }
	    //System.out.println(key);
    }
    
  }
  //}}}
    
  //{{{ buildAALists
  //##################################################################################################
  /**
  * Figures out the atom info from the property file.  I use it to create two arraylists, one for
  * hydrogens, and one for everything else.  Then, I combine the arraylists into an all atom lists.
  * The lists should only have each atom type once.  
  */
  public void buildAALists(ArrayList list, String connectivity, String type) {
    String          token = "";
    boolean  nextToken = false;
    StringTokenizer tokenizer   = new StringTokenizer(connectivity, ",;", true);
    
    if (type.equals("hy")) {
	    while(tokenizer.hasMoreTokens()) {
        token = tokenizer.nextToken();
        if (nextToken) {
          if (!(list.contains(token))) {
            //System.out.println(token);
            list.add(token);
          }
          nextToken = false;
        }
        if (token.equals(",")) {
          nextToken = true;
        }
        
	    }
    } else {
	    while (tokenizer.hasMoreTokens()) {
        token = tokenizer.nextToken();
        if (!(token.equals(",")||(token.equals(";")))) {
          if (!(list.contains(token))) {
            //System.out.println(token);
            list.add(token);
          }
        }
	    }
    }
    //System.out.println(list);
  }
  //}}}
  
  //{{{ getAtomNamePosition
  /**
  * Uses sc.props to figure out the order of atoms.  Returns the index of the allAtoms array.
  **/
  public int getAtomNamePosition(String name) {
    /* don't have to figure out the spacing myself...sc props has it all in there already!
    for (int i = allAtoms.size() - 1; i >= 0; i--) {
	    String atom = ((String) allAtoms.get(i)).trim();
	    if (atom.length() == 1) {
        atom = " " + atom + " ";
	    } else if (atom.length() == 2) {
        atom = " " + atom + " ";
	    } else if (atom.length() == 3) {
        if (KinUtil.isNumeric(atom.substring(0, 1))) {
          atom = atom + " ";
        } else {
          atom = " " + atom;
        }
	    }
	    //System.out.print(atom + ",");
	    if (name.indexOf(atom)>-1) {
        //System.out.print(atom + ",");
        return i;
	    }
    }
    return -1;
    */
    for (int i = allAtoms.size() - 1; i >= 0; i--) {
      String atom = (String) allAtoms.get(i);
      if (name.indexOf(atom) > -1) return i;
    }
    return -1;
  }
  //}}}

  //{{{ getAtomName
  public static String getAtomName(String name) {
    /*
    for (int i = allAtoms.size() - 1; i >= 0; i--) {
      String atom = ((String) allAtoms.get(i)).trim();
      if (atom.length() == 1) {
        atom = " " + atom + " ";
      } else if (atom.length() == 2) {
        atom = " " + atom + " ";
      } else if (atom.length() == 3) {
        if (KinUtil.isNumeric(atom.substring(0, 1))) {
          atom = atom + " ";
        } else {
          atom = " " + atom;
        }
      }
      //System.out.print(atom + ",");
      if (name.indexOf(atom)>-1) {
        //System.out.print(atom + ",");
        if (atom.length() == 3) {
          return atom + " ";
        } else {
          return atom;
        }
      }
    }
    return "UNK ";
    */
    for (int i = allAtoms.size() - 1; i >= 0; i--) {
      String atom = (String) allAtoms.get(i);
      if (name.indexOf(atom) > -1) return atom;
    }
    return "UNK ";
  }
//}}}    
    
}