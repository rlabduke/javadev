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

public class PointComparator implements Comparator {

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################    
    //String[] atomNames = {" N  ", " CA ", " C  ", " O  ", " CB ", " CG ", " CD ", " CE ", " NZ "};
    Props     scProps;
//}}}


    public PointComparator() {
	extractAAs();
    }

    public int compare(Object o1, Object o2) {
	int value = 0;
	if ((o1 instanceof AbstractPoint) && (o2 instanceof AbstractPoint)) {
	    AbstractPoint point1 = (AbstractPoint) o1;
	    AbstractPoint point2 = (AbstractPoint) o2;
	    String p1name = point1.getName().toUpperCase().trim();
	    String p2name = point2.getName().toUpperCase().trim();
	    //value = Integer.parseInt(p1name.substring(8)) - Integer.parseInt(p2name.substring(8));
	    double calc = point1.getX()-point2.getX()+point1.getY()-point2.getY()+point1.getZ()-point2.getZ();
	    if (calc == 0) {
		value = 0;
	    } else {
		value = (int)(calc/Math.abs(calc));
	    }
	    value = value + getResAA(p1name).compareTo(getResAA(p2name)) * 1000;
	
	    
	    value = value + (getResNumber(p1name) - getResNumber(p2name)) * 100000;
	    //return value;
	}
	return value;
    }


    public int getResNumber(String name) {
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
	    if (isNumeric(parseValue)) {
		//System.out.print(parseValue + " ");
		return Integer.parseInt(parseValue);
	    }
	}
	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < parsed.length; i++) {
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

    public String getResAA(String name) {
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
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (parseValue.length()==3){
		if (AminoAcid.isAminoAcid(parseValue)) {
		    return parseValue;
		}
	    }
	}
	return "UNK";
    }

    public boolean isNumeric(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

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

	Set scs = scProps.keySet();
	Iterator iter = scs.iterator();
	while (iter.hasNext()) {
	    String key = (String) iter.next();
	    System.out.println(key);
	}
    }

}
