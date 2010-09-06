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
      
      String modelNames = "";
      TreeMap deltaRdcByRes = new TreeMap();
      TreeMap minDistByRes = new TreeMap();
      
      /** This code assumes that all models will have same RDCs and bond vectors as the first model. */
      Model firstMod = pdb.getFirstModel();
      Iterator rezes = firstMod.getResidues().iterator();
      while (rezes.hasNext()) {
        Residue orig = (Residue) rezes.next();
        deltaRdcByRes.put(orig.getCNIT(), "");
        minDistByRes.put(orig.getCNIT(), "");
      }
      
      Iterator models = (pdb.getModels()).iterator();
      while (models.hasNext()) {

        //System.out.print(".");
        Model mod = (Model) models.next();
        modelNames = modelNames + mod.getName()+":";
        //System.out.println("Model "+mod.getName());
        System.out.println("model:ResID:expRDC:backcalcRDC:absdeltaRDC:minDist");
        
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
            String seq = orig.getSequenceNumber().trim();
            double rdcVal = fi.getRdcValue(seq);
            double backcalcRdc = fi.getBackcalcRdc(rdcVect);
            if ((!Double.isNaN(rdcVal))&&(!Double.isNaN(backcalcRdc))) {
              double dist = analyzeResidue(origin, rdcVect, orig, fi);
              System.out.println(mod.getName()+":"+orig +":"+ df.format(rdcVal)+":"+df.format(backcalcRdc)+":"+df.format(Math.abs(rdcVal-backcalcRdc))+":"+df.format(dist));
              String deltaOut = (String) deltaRdcByRes.get(orig.getCNIT());
              //System.err.println(orig+" "+deltaOut);
              String distOut = (String) minDistByRes.get(orig.getCNIT());
              deltaRdcByRes.put(orig.getCNIT(), deltaOut+":"+df.format(Math.abs(rdcVal-backcalcRdc)));
              minDistByRes.put(orig.getCNIT(), distOut+":"+df.format(dist));
            }
          } else {
          }
        }
      }
      System.out.println("Absolute(delta RDCs)");
      System.out.println("res"+":"+modelNames);
      printMap(deltaRdcByRes, firstMod);
      System.out.println("Min distance");
      System.out.println("res"+":"+modelNames);
      printMap(minDistByRes, firstMod);
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
      //System.out.println(rdcVect);
      rdcVect = rdcVect.mult(Triple.distance(fromState, toState));
      //System.out.println(rdcVect);
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
  public double analyzeResidue(Tuple3 p, Triple rdcVect, Residue orig, FileInterpreter fi) {
    String seq = orig.getSequenceNumber().trim();
    double rdcVal = fi.getRdcValue(seq);
    double backcalcRdc = fi.getBackcalcRdc(rdcVect);
    //double rdcError = fi.getRdcError(seq);
    //if (Double.isNaN(rdcError)) rdcError = 1;
    if ((!Double.isNaN(rdcVal))&&(!Double.isNaN(backcalcRdc))) {
      double radius = rdcVect.distance(new Triple(0, 0, 0));
      //String text = "res= "+seq+" rdc= "+df.format(rdcVal)+"+/-"+df.format(rdcError)+" backcalc= "+df.format(backcalcRdc);
      double dist = fi.getDrawer().analyzeVector(rdcVal, p, rdcVect, radius, 60);
      return dist;
    } else {
      //System.out.println("this residue does not appear to have an rdc");
    }
    return -1;
  }
  //}}}
  
  //{{{ printMap
  public void printMap(TreeMap resMap, Model firstMod) {
    Iterator rezes = firstMod.getResidues().iterator();
    while (rezes.hasNext()) {
      Residue orig = (Residue) rezes.next();
      String value = (String) resMap.get(orig.getCNIT());
      if (value != "") {
        System.out.println(orig+value);
      }
    }
  }
  //}}}

  
  
}
