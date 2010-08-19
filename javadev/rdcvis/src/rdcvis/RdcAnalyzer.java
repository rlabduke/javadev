// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package rdcvis;


//import molikin.logic.*;
//import driftwood.gui.*;
import driftwood.r3.*;
//import driftwood.util.*;
//import driftwood.data.*;
//import driftwood.util.SoftLog;
import driftwood.moldb2.*;
//import driftwood.mysql.*;
//
//import java.net.*;
import java.util.*;
import java.io.*;
//import javax.swing.*;
//import java.awt.event.*;
import java.text.*;
import java.util.zip.*;
//import java.sql.*;

//}}}
/**
* <code>RdcAnalyzer</code> is for analyzing how good a given RDC matches its 
* modeled vector.
*
* <p>Copyright (C) 2007 by Vincent Chen. All rights reserved.
* 
*/
public class RdcAnalyzer {
  
  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  //}}}

  //{{{ Variables
  //}}}

  //{{{ Constructors
  public RdcAnalyzer() {
    
  }
  //}}}

  //{{{ analyzeCoordFile
  public void analyzeCoordFile(FileInterpreter fi, ArrayList<String> rdcTypes, boolean ensembleTensor) {
    CoordinateFile pdb = fi.getPdb();
    for (String rdcName : rdcTypes) {
      if (ensembleTensor) {
        fi.solveRdcsEnsemble(rdcName);
      }
      String[] atoms = fi.parseAtomNames(rdcName);
      Iterator models = (pdb.getModels()).iterator();
      while (models.hasNext()) {

        //System.out.print(".");
        Model mod = (Model) models.next();
        System.out.println("Model "+mod.getName());
        System.out.println("ResID:expRDC:backcalcRDC:minDist");
        
        ModelState state = mod.getState();
        if (!ensembleTensor) {
          fi.solveRdcsSingleModel(rdcName, mod.toString());
        }
        Iterator iter = mod.getResidues().iterator();
        while (iter.hasNext()) {
          Residue orig = (Residue) iter.next();
          Triple rdcVect = getResidueRdcVect(state, orig, atoms);
          AtomState origin = getOriginAtom(state, orig, atoms);
          if ((rdcVect != null)&&(origin != null)) {
            analyzeResidue(origin, rdcVect, orig, fi);
          } else {
          }
        }
      }
    }
  }
  //}}}
  
  //{{{ getResidueRdcVect
  /** returns RdcVect for orig residue based on what is selected in fi **/
  public static Triple getResidueRdcVect(ModelState state, Residue orig, String[] atoms) {
    Atom from = orig.getAtom(atoms[0]);
    Atom to = orig.getAtom(atoms[1]);
    try {
      AtomState fromState = state.get(from);
      AtomState toState = state.get(to);
      Triple rdcVect = new Triple().likeVector(fromState, toState).unit();
      return rdcVect;
    } catch (AtomException ae) {
    }
    return null;
  }
  //}}}
  
  //{{{ getOriginAtom
  public static AtomState getOriginAtom(ModelState state, Residue orig, String[] atoms) {
    Atom origin;
    if (atoms[0].indexOf("H") > -1) {
      origin = orig.getAtom(atoms[1]);
    } else {
      origin = orig.getAtom(atoms[0]);
    }
    try {
      AtomState originState = state.get(origin);
      return originState;
    } catch (AtomException ae) {
    }
    return null;
  }
  //}}}
  
  //{{{ analyzeResidue
  public void analyzeResidue(Tuple3 p, Triple rdcVect, Residue orig, FileInterpreter fi) {
    String seq = orig.getSequenceNumber().trim();
    double rdcVal = fi.getRdcValue(seq);
    //System.out.println(rdcVal);
    //System.out.println((rdcVal != Double.NaN));
    double backcalcRdc = fi.getBackcalcRdc(rdcVect);
    double rdcError = fi.getRdcError(seq);
    if (Double.isNaN(rdcError)) rdcError = 1;
    if ((!Double.isNaN(rdcVal))&&(!Double.isNaN(backcalcRdc))) {
      double radius = rdcVect.distance(new Triple(0, 0, 0));
      String text = "res= "+seq+" rdc= "+df.format(rdcVal)+"+/-"+df.format(rdcError)+" backcalc= "+df.format(backcalcRdc);
      double dist = fi.getDrawer().analyzeVector(rdcVal, p, rdcVect, radius, 60);
      System.out.println(orig +":"+ df.format(rdcVal)+":"+df.format(backcalcRdc)+":"+df.format(dist));
    } else {
      //System.out.println("this residue does not appear to have an rdc");
    }
  }
  //}}}
  
  
  
  
}
