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
    Parameter alpha1sd = new Parameter( 0.2  ,  5  ,  5  ,   5,  4,   5);
    Parameter alpha2 = new Parameter(5.4, 66, 62, -37, 113, 160.5);
    Parameter alpha2sd = new Parameter(0.4, 5, 5, 6, 6, 6);
    alphaParams.add(alpha1);
    alphaParams.add(alpha2);
    alphaSD.add(alpha1sd);
    alphaSD.add(alpha2sd);
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
  
  
}
