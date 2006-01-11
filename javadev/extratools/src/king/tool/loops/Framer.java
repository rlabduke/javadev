// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;

import java.util.*;
import java.text.DecimalFormat;
//import java.awt.*;
import driftwood.r3.*;

public class Framer {

//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.000");
//}}}

//{{{ Variable definitions
//######################################################
    
    

    public Framer() {
	
    }


    public static double[] calphaAnalyze(KPoint ca0, KPoint ca1, KPoint caN, KPoint caN1, KPoint co0, KPoint coN) {
	Triple tripca0 = new Triple(ca0);
	Triple tripca1 = new Triple(ca1);
	Triple tripcaN = new Triple(caN);
	Triple tripcaN1 = new Triple(caN1);
	Triple tripco0 = new Triple(co0);
	Triple tripcoN = new Triple(coN);
	//System.out.println(tripca0.distance(ca1));
	//System.out.println(tripca1.distance(caN));
	//System.out.println(tripcaN.distance(caN1));
	//System.out.println(Triple.angle(tripca0, tripca1, tripcaN));
	//System.out.println(Triple.angle(tripca1, tripcaN, tripcaN1));
	//System.out.println(Triple.dihedral(tripco1, tripca0, tripca1, tripcaN));
	//System.out.println(Triple.dihedral(tripca0, tripca1, tripcaN, tripcaN1));
	//System.out.println(Triple.dihedral(tripca1, tripcaN, tripcaN1, tripcoN));
	double[] params = new double[6];
	params[0] = tripca1.distance(caN);
	params[1] = Triple.angle(tripca0, tripca1, tripcaN);
	params[2] = Triple.angle(tripca1, tripcaN, tripcaN1);
	params[3] = Triple.dihedral(tripco0, tripca0, tripca1, tripcaN);
	params[4] = Triple.dihedral(tripca0, tripca1, tripcaN, tripcaN1);
	params[5] = Triple.dihedral(tripca1, tripcaN, tripcaN1, tripcoN);
	System.out.print(df.format(params[0]) + " ");
	System.out.print(df.format(params[1]) + " ");
	System.out.print(df.format(params[2]) + " ");
	System.out.print(df.format(params[3]) + " ");
	System.out.print(df.format(params[4]) + " ");	
	System.out.println(df.format(params[5]));
	return params;
    }

    public static ArrayList calphaAnalyzeList(KPoint ca0, KPoint ca1, KPoint caN, KPoint caN1, KPoint co0, KPoint coN) {
	double[] results = calphaAnalyze(ca0, ca1, caN, caN1, co0, coN);
	ArrayList list = new ArrayList();
	for (int i = 0; i < results.length; i++) {
	    list.add(new Double(results[i]));
	}
	return list;
    }

}
	//Triple tripca1 = new Triple(ca1);
