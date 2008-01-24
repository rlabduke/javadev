// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fragmentfiller;

import driftwood.moldb2.*;
import driftwood.r3.*;
import java.util.*;
import java.text.*;
//}}}

public class ProteinStem {
  
  //{{{ Constants
  public static final int  N_TERM   = 1;
  public static final int  C_TERM   = 2;
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  Residue zeroRes;
  Residue oneRes;
  Residue twoRes;
  ArrayList<AtomState> states;
  ArrayList<Double> pairStats;
  String sourceModName;
  String sourceChain;
  int stemType;
  //int oneResNum;
  //int nResNum;
  //}}}
  
  //{{{ Constructors
  public ProteinStem(Model mod, String chain, Residue zr, Residue or, Residue tr, int st) {
    sourceModName = mod.getName();
    sourceChain = chain;
    ModelState modState = mod.getState();
    zeroRes = zr;
    oneRes = or;
    twoRes = tr;
    stemType = st;
    try {
      AtomState ca0 = modState.get(zeroRes.getAtom(" CA "));
      AtomState ca1 = modState.get(oneRes.getAtom(" CA "));
      AtomState ca2 = modState.get(twoRes.getAtom(" CA "));
      AtomState co0 = modState.get(zeroRes.getAtom(" O  "));
      AtomState co1 = modState.get(oneRes.getAtom(" O  "));
      states = new ArrayList<AtomState>();
      states.add(ca0);
      states.add(ca1);
      states.add(ca2);
      states.add(co0);
      states.add(co1);
      pairStats = pairAnalyze(co0, ca0, ca1, ca2, co1);
      System.out.print(sourceModName + " " + sourceChain + " ");
      for (Double doub : pairStats) {
        double d = doub.doubleValue();
        System.out.print(df.format(d) + " ");
      }
      System.out.println();
    } catch (AtomException ae) {
      System.err.println("Problem with atom " + ae.getMessage());
    }
  }
  //}}}
  
  //{{{ pairAnalyze
  public ArrayList<Double> pairAnalyze(Triple coFirst, Triple caFirst, Triple caTwo, Triple caThree, Triple coTwo) {
    double[] params = new double[3];
    params[0] = Triple.angle(caFirst, caTwo, caThree);
    params[1] = Triple.dihedral(coFirst, caFirst, caTwo, caThree);
    params[2] = Triple.dihedral(caFirst, caTwo, caThree, coTwo);
    ArrayList<Double> list = new ArrayList<Double>();
    for (double d : params) {
      list.add(new Double(d));
    }
    return list;
  }
  //}}}
  
  //{{{ get functions
  public ArrayList<Double> getParameters() {
    return pairStats;
  }
  
  public ArrayList<AtomState> getAtomStates() {
    return states;
  }
  
  public int getOneNum() {
    return oneRes.getSequenceInteger();
  }
  
  public String getSourceString() {
    return sourceModName + sourceChain;
  }
  
  public int getStemType() {
    return stemType;
  }
  
  public Tuple3[] getTupleArray() {
    Tuple3[] tuples = new Tuple3[3];
    //tuples[0] = coMap.get(new Integer(oneNum - 1));
    tuples[0] = states.get(0);
    tuples[1] = states.get(1);
    tuples[2] = states.get(2);
    //tuples[3] = states.get(3);
    //tuples[5] = coMap.get(new Integer(nNum));
    return tuples;
  }
  
  //public Tuple3[] getNtermTuples() {
  //  Tuple3[] tuples = new Tuple3[3];
  //  //tuples[0] = coMap.get(new Integer(oneNum - 1));
  //  tuples[0] = states.get(0);
  //  tuples[1] = states.get(4);
  //  tuples[2] = states.get(1);
  //  //tuples[3] = states.get(3);
  //  //tuples[5] = coMap.get(new Integer(nNum));
  //  return tuples;
  //}
  //}}}
  
  //{{{ equals, hashcode
  public boolean equals(Object o)
  {
    if(! (o instanceof ProteinStem)) return false;
    else
    {
      ProteinStem p = (ProteinStem)o;
      if(pairStats.equals(p.getParameters()) && states.equals(p.getAtomStates())) return true;
      else return false;
    }
  }
  
  public int hashCode() {
    return (pairStats.hashCode() ^ states.hashCode());
  }
  //}}}
  
}
