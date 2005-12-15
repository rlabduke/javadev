// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;

import java.util.*;
import driftwood.r3.*;

public class Framer {

//{{{ Constants
    
//}}}

//{{{ Variable definitions
//######################################################
    
    

    public Framer() {
	
    }


    public static double[] calphaAnalyze(KPoint ca0, KPoint ca1, KPoint caN, KPoint caN1, KPoint co1, KPoint coN) {
	Triple tripca0 = new Triple(ca0);
	Triple tripca1 = new Triple(ca1);
	Triple tripcaN = new Triple(caN);
	Triple tripcaN1 = new Triple(caN1);
	Triple tripco1 = new Triple(co1);
	Triple tripcoN = new Triple(coN);
	//System.out.println(tripca0.distance(ca1));
	System.out.println(tripca1.distance(caN));
	//System.out.println(tripcaN.distance(caN1));
	System.out.println(Triple.angle(tripca0, tripca1, tripcaN));
	System.out.println(Triple.angle(tripca1, tripcaN, tripcaN1));
	System.out.println(Triple.dihedral(tripco1, tripca0, tripca1, tripcaN));
	System.out.println(Triple.dihedral(tripca0, tripca1, tripcaN, tripcaN1));
	System.out.println(Triple.dihedral(tripca1, tripcaN, tripcaN1, tripcoN));
	double[] params = new double[6];
	params[0] = tripca1.distance(caN);
	params[1] = Triple.angle(tripca0, tripca1, tripcaN);
	params[2] = Triple.angle(tripca1, tripcaN, tripcaN1);
	params[3] = Triple.dihedral(tripco1, tripca0, tripca1, tripcaN);
	params[4] = Triple.dihedral(tripca0, tripca1, tripcaN, tripcaN1);
	params[5] = Triple.dihedral(tripca1, tripcaN, tripcaN1, tripcoN);
	return params;
    }
}
	//Triple tripca1 = new Triple(ca1);
