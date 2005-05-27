// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;

import driftwood.moldb2.*;
import driftwood.util.*;
//import chiropraxis.kingtools.*;
import java.util.*;
import java.io.*;
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
	    //double calc = point1.getX()-point2.getX()+point1.getY()-point2.getY()+point1.getZ()-point2.getZ();
	    //if (calc == 0) {
	    //	value = 0;
	    //} else {
	    //	value = ((int)(calc/Math.abs(calc)));
	    //}

	    //int atomPosition = allAtoms.indexOf(atom1) - allAtoms.indexOf(atom2);
	    int atomPosition = getAtomNamePosition(p1name) - getAtomNamePosition(p2name);
	    value = atomPosition * 10;

	    
	    value = value + getResAA(p1name).compareTo(getResAA(p2name)) * 1000;
	
	    
	    value = value + (getResNumber(p1name) - getResNumber(p2name)) * 1000000;
	    //return value;
	    if (value == 0) {
		System.out.println(p1name + ", " + p2name + ": " + value);
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


//{{{ get functions
//##################################################################################################
    /**
     * Functions that extract various info from pointIDs.
     */
    public static int getResNumber(String name) {
	//String name = point.getName().trim();
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
	// another pass to see if there are any AAName + int in name.
	if (parsed[1].length() > 3) {
	    String parseValue = parsed[1].substring(3);
	    String potentialAA = parsed[1].substring(0, 3);
	    if (AminoAcid.isAminoAcid(potentialAA)) {
		if (isNumeric(parseValue)) {
		    //System.out.print(parseValue + " ");
		    return Integer.parseInt(parseValue);
		}
	    }
	}
	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < i2; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (isNumeric(parseValue)) {
		if (Integer.parseInt(parseValue)>0) {
		    return Integer.parseInt(parseValue);
		}
	    }
	}

	return -1;
    }

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

    public static boolean isNumeric(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

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

    public int getAtomNamePosition(String name) {
	for (int i = allAtoms.size() - 1; i >= 0; i--) {
	    String atom = ((String) allAtoms.get(i)).trim();
	    if (atom.length() == 1) {
		atom = " " + atom + " ";
	    } else if (atom.length() == 2) {
		atom = " " + atom + " ";
	    } else if (atom.length() == 3) {
		if (isNumeric(atom.substring(0, 1))) {
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
    }

    public static String getAtomName(String name) {
	for (int i = allAtoms.size() - 1; i >= 0; i--) {
	    String atom = ((String) allAtoms.get(i)).trim();
	    if (atom.length() == 1) {
		atom = " " + atom + " ";
	    } else if (atom.length() == 2) {
		atom = " " + atom + " ";
	    } else if (atom.length() == 3) {
		if (isNumeric(atom.substring(0, 1))) {
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
    }
}
