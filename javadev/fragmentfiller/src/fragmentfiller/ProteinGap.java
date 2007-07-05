// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fragmentfiller;

import driftwood.moldb2.*;
import driftwood.r3.*;
import java.util.*;
import java.text.*;
//}}}

public class ProteinGap {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  Residue zeroRes;
  Residue oneRes;
  Residue nRes;
  Residue n1Res;
  ArrayList<AtomState> states;
  ArrayList<Double> frame;
  String sourceModName;
  String sourceChain;
  //int oneResNum;
  //int nResNum;
  //}}}
  
  //{{{ Constructors
  public ProteinGap(Model mod, String chain, Residue zr, Residue or, Residue nr, Residue n1r) {
    sourceModName = mod.getName();
    sourceChain = chain;
    ModelState modState = mod.getState();
    zeroRes = zr;
    oneRes = or;
    nRes = nr;
    n1Res = n1r;
    try {
      AtomState ca0 = modState.get(zeroRes.getAtom(" CA "));
      AtomState ca1 = modState.get(oneRes.getAtom(" CA "));
      AtomState caN = modState.get(nRes.getAtom(" CA "));
      AtomState caN1 = modState.get(n1Res.getAtom(" CA "));
      AtomState co0 = modState.get(zeroRes.getAtom(" O  "));
      AtomState coN = modState.get(nRes.getAtom(" O  "));
      states = new ArrayList<AtomState>();
      states.add(ca0);
      states.add(ca1);
      states.add(caN);
      states.add(caN1);
      states.add(co0);
      states.add(coN);
      frame = Framer.calphaAnalyzeList(ca0, ca1, caN, caN1, co0, coN);
      System.out.print(sourceModName + " " + sourceChain + " ");
      for (double d : frame) {
        System.out.print(df.format(d) + " ");
      }
      System.out.println();
    } catch (AtomException ae) {
      System.err.println("Problem with atom " + ae.getMessage());
    }
  }
  //}}}
  
  //{{{ get functions
  public ArrayList<Double> getParameters() {
    return frame;
  }
  
  public ArrayList<AtomState> getAtomStates() {
    return states;
  }
  
  public int getSize() {
    return nRes.getSequenceInteger() - oneRes.getSequenceInteger();
  }
  
  public int getOneNum() {
    return oneRes.getSequenceInteger();
  }

  public int getNNum() {
    return nRes.getSequenceInteger();
  }
  
  public String getSourceString() {
    return sourceModName + sourceChain;
  }
  
  public Tuple3[] getTupleArray() {
    Tuple3[] tuples = new Tuple3[4];
    //tuples[0] = coMap.get(new Integer(oneNum - 1));
    tuples[0] = states.get(0);
    tuples[1] = states.get(1);
    tuples[2] = states.get(2);
    tuples[3] = states.get(3);
    //tuples[5] = coMap.get(new Integer(nNum));
    return tuples;
  }
  //}}}
  
  //{{{ equals, hashcode
  public boolean equals(Object o)
  {
    if(! (o instanceof ProteinGap)) return false;
    else
    {
      ProteinGap p = (ProteinGap)o;
      if(frame.equals(p.getParameters()) && states.equals(p.getAtomStates())) return true;
      else return false;
    }
  }
  
  public int hashCode() {
    return (frame.hashCode() ^ states.hashCode());
  }
  //}}}
  
}
