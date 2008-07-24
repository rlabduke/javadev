// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package absigner;

import java.util.*;
//}}}

public class Parameters {
  
  //{{{ Constants
  
  //}}}
  
  //{{{ Variables
  ArrayList<Parameter> alphaParams;
  ArrayList<Parameter> alphaSD;
  ArrayList<Parameter> betaParams;
  ArrayList<Parameter> betaSD;
  //}}}
  
  //{{{ class: Parameter
  class Parameter {
    
    double[] param;
    
    public Parameter(double dist, double start_ang, double end_ang, double start_dihed, double mid_dihed, double end_dihed) {
      param = new double[] {dist, start_ang, end_ang, start_dihed, mid_dihed, end_dihed};
    }
    
    public double[] get() {
      return param;
    }
  }
  //}}}

  //{{{ Constructor
  public Parameters() {
    alphaParams = new ArrayList<Parameter>();
    alphaSD = new ArrayList<Parameter>();
    Parameter alpha1 = new Parameter(   3.788, 91.2, 92.2, -74, 51, 125);
    Parameter alpha1sd = new Parameter( 0.1  ,  1  ,  1  ,   1,  1,   1);
    Parameter alpha2 = new Parameter(5.4, 66, 62, -37, 113, 160.5);
    Parameter alpha2sd = new Parameter(0.1, 1, 1, 1, 1, 1);
    alphaParams.add(alpha1);
    alphaParams.add(alpha2);
    alphaSD.add(alpha1sd);
    alphaSD.add(alpha2sd);
    Parameter beta2 = new Parameter(    6.357,   161.6,   167,   -124.6,   12.8,  -31.9   );
    Parameter beta2sd = new Parameter(  0.1,       1,       1,      1,      1,      1     );
    betaParams.add(beta2);
    betaSD.add(beta2sd);
  }
  //}}}

  //{{{ getAlphaSize
  public double[] getAlphaSize(int size) {
    if (size <= alphaParams.size()) {
      Parameter param = alphaParams.get(size-1);
      return param.get();
    } else {
      System.err.println("No data on alpha fragments of that size available!");
      return null;
    }
  }
  //}}}
  
  //{{{ getAlphaSD
  public double[] getAlphaSD(int size) {
    if (size <= alphaSD.size()) {
      Parameter param = alphaSD.get(size-1);
      return param.get();
    } else {
      System.err.println("No data on alpha standard dev of that size available!");
      return null;
    }
  }
  //}}}
  
  //{{{ getBetaSize
  public double[] getBetaSize(int size) {
    if (size <= betaParams.size()) {
      Parameter param = betaParams.get(size-1);
      return param.get();
    } else {
      System.err.println("No data on beta fragments of that size available!");
      return null;
    }
  }
  //}}}
  
  //{{{ getBetaSD
  public double[] getBetaSD(int size) {
    if (size <= betaSD.size()) {
      Parameter param = betaSD.get(size-1);
      return param.get();
    } else {
      System.err.println("No data on beta standard dev of that size available!");
      return null;
    }
  }
  //}}}
  
  //{{{ inRange
  public static boolean inRange(double[] currFrag, double[] ssParams, double[] paramsSD) {
    if ((currFrag[0] > ssParams[0] + paramsSD[0]) || (currFrag[0] < ssParams[0] - paramsSD[0])) {
      return false;
    }
    for (int i = 1; i < ssParams.length; i++) {
      if (angleInRange(currFrag[i], ssParams[i], paramsSD[i])) {
        return false;
      }
    }
    return true;
  }
  //}}}
  
  //{{{ angleInRange
  public static boolean angleInRange(double value, double param, double sd) {
    if (param > 180 - sd) {
      if (!((value >= param - sd) || (value <= -360 + param + sd))) {
        return false;
      }
    } else if (param < -180 + sd) {
      if (!((value <= param + sd) || (value >= 360 + param - sd))) {
        return false;
      }
    } else {
      if ((value > param + sd) || (value < param + sd)) {
        return false;
      }
    }
    return true;
  }
  //}}}
}
