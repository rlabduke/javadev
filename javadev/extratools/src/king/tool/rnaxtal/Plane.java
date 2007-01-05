// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
import king.*;
import king.core.*;


public class Plane {
    
    //{{{ Constants
    //DecimalFormat df2 = new DecimalFormat("0.00");    
    //}}}

//{{{ Variable definitions
//##################################################################################################
    RNATriple anchor;
    RNATriple normal;
    double a, b, c, d;
    //KList list;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */

    public Plane(RNATriple anch, RNATriple norm) {
	anchor = anch;
	normal = norm;
	a = normal.getX();
	b = normal.getY();
	c = normal.getZ();
	d = - a * anchor.getX() - b * anchor.getY() - c * anchor.getZ();
    }


    public boolean isBelow(KPoint p) {
	if (a * p.getX() + b * p.getY() + c * p.getZ() + d < 0) {
	    return true;
	} else {
	    return false;
	}
    }
}
    
