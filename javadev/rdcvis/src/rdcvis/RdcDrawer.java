// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package rdcvis;
import king.*;
import king.points.*;
import king.core.*;

import driftwood.r3.*;
import Jama.*;
import java.util.*;
import java.text.*;
//}}}

/**
* <code>RdcDrawer</code> is for drawing the actual RDC curves.
* 
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* <br>Begun Tue Nov 27 15:57:02 EST 2007
**/
public class RdcDrawer {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.00");
  //}}}
  
  //{{{ Variables
  double daxial, drhombic, xisquared, etasquared, lambdasquared, zetasquared, deltacrit;
  double dzz, dyy, dxx;
  Matrix eigMatrix;
  //}}}
  
  //{{{ Constructor
  public RdcDrawer(Matrix saupeDiag, Matrix eigV) {
    calcVariables(saupeDiag, eigV);
  }
  //}}}
  
  //{{{ calcVariables
  public void calcVariables(Matrix saupeDiag, Matrix eigV) {
    dzz = saupeDiag.get(0, 0);
    dyy = saupeDiag.get(2, 2);
    dxx = saupeDiag.get(1, 1);
    daxial = dzz / 2;
    drhombic = (dxx - dyy) / 3.0;
    deltacrit = dxx / daxial;
    eigMatrix = new Matrix(3, 3);
    eigMatrix.set(0, 0, eigV.get(0, 1));
    eigMatrix.set(1, 0, eigV.get(1, 1));
    eigMatrix.set(2, 0, eigV.get(2, 1));
    eigMatrix.set(0, 1, eigV.get(0, 2));
    eigMatrix.set(1, 1, eigV.get(1, 2));
    eigMatrix.set(2, 1, eigV.get(2, 2));
    eigMatrix.set(0, 2, eigV.get(0, 0));
    eigMatrix.set(1, 2, eigV.get(1, 0));
    eigMatrix.set(2, 2, eigV.get(2, 0));
    eigV.print(1, 3);
    eigMatrix.print(1, 3);
    //eigMatrix = eigMatrix.inverse();
    //System.out.println(daxial + " " + drhombic + " " + deltacrit);
  }
  //}}}
  
  //{{{ createKin
  //public void createKin() {
  //  Kinemage kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
  //  KGroup group = new KGroup("RDC Points");
  //  KGroup sub = new KGroup("Sub");
  //  sub.setHasButton(false);
  //  KList list = new KList(KList.VECTOR, "points");
  //  
  //  kin.add(group);
  //  group.add(sub);
  //  sub.add(list);
  //  
  //  createPoints(list);
  //  kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));
  //
  //}
  //}}}
  
  //{{{ draw
  public void drawAllCurves(Tuple3 center, KList list) {
    //VectorPoint origin = new VectorPoint("0 0 0", null);
    //origin.setXYZ(0, 0, 0);
    //list.add(origin);
    for (int i = (int)((-1 - 3/2 * drhombic/daxial) * 100); i <= 200; i = i + 5) { // avoids round off errors, I think.
      //System.out.println((double) i / 100.0);
      //System.out.println(i);
      double dn = i * daxial / 100.0;
      drawCurve(dn, center, 0, list);
    }
  }
  //}}}
  
  //{{{ drawCurve
  public void drawCurve(double dn, Tuple3 center, double backcalcRdc, KList list) {
    xisquared = (2 * daxial - dn) / (3 * daxial - (3/2) * drhombic);
    etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    lambdasquared = (dn + daxial + (3 / 2) * drhombic) / (3 * drhombic);
    zetasquared = (dn + daxial + (3 / 2) * drhombic) / (3 * daxial + (3 / 2) * drhombic);
    //System.out.println("xisquared: "+xisquared);
    //System.out.println("etasquared: "+etasquared);
    //System.out.println("lambdasquared: "+lambdasquared);
    //System.out.println("zetasquared: "+zetasquared);
    VectorPoint old = null;
    VectorPoint negOld = null;
    ArrayList<KPoint> posPoints = new ArrayList<KPoint>();
    ArrayList<KPoint> negPoints = new ArrayList<KPoint>();
    for (double tau = - Math.PI; tau <= Math.PI; tau += Math.PI / 60) {
      //System.out.println(tau);
      VectorPoint point = new VectorPoint("Point " + df.format(dn), old);
      Matrix vectSaupeSpace = new Matrix (3, 1); // 3 rows, 1 column
      vectSaupeSpace.set(0, 0, calcX(dn, tau));
      vectSaupeSpace.set(1, 0, calcY(dn, tau));
      vectSaupeSpace.set(2, 0, calcZ(dn, tau));
      Matrix vectPdbSpace = eigMatrix.times(vectSaupeSpace);
      //point.setXYZ(calcX(dn, tau)+center.getX(), calcY(dn, tau)+center.getY(), calcZ(dn, tau)+center.getZ());
      point.setXYZ(vectPdbSpace.get(0, 0)+center.getX(), vectPdbSpace.get(1, 0)+center.getY(), vectPdbSpace.get(2, 0)+center.getZ());      
      //if (dn > 0) point.setColor(KPalette.green);
      if (Math.abs(dn - backcalcRdc) < 0.25) point.setColor(KPalette.green);
      else if (Math.abs(dn - backcalcRdc) < 0.5) point.setColor(KPalette.yellow);
      else point.setColor(KPalette.red);
      //list.add(origin);
      //list.add(point);
      posPoints.add(point);
      old = point;
      VectorPoint negPoint = new VectorPoint("Neg Point " + df.format(dn), negOld);
      Matrix negSaupeSpace = vectSaupeSpace.copy();
      negSaupeSpace.set(2, 0, -calcZ(dn, tau));
      Matrix negZPdbSpace = eigMatrix.times(negSaupeSpace);
      negSaupeSpace = vectSaupeSpace.copy();
      negSaupeSpace.set(1, 0, -calcY(dn, tau));
      Matrix negYPdbSpace = eigMatrix.times(negSaupeSpace);
      if (dn / daxial > deltacrit) {
        //negPoint.setXYZ(calcX(dn, tau)+center.getX(), calcY(dn, tau)+center.getY(), -calcZ(dn, tau)+center.getZ());
        negPoint.setXYZ(negZPdbSpace.get(0, 0)+center.getX(), negZPdbSpace.get(1, 0)+center.getY(), negZPdbSpace.get(2, 0)+center.getZ());
      } else {
        negPoint.setXYZ(negYPdbSpace.get(0, 0)+center.getX(), negYPdbSpace.get(1, 0)+center.getY(), negYPdbSpace.get(2, 0)+center.getZ());
      }
      //if (dn > 0) negPoint.setColor(KPalette.green);
      if (Math.abs(dn - backcalcRdc) < 0.25) negPoint.setColor(KPalette.green);
      else if (Math.abs(dn - backcalcRdc) < 0.5) negPoint.setColor(KPalette.yellow);
      else negPoint.setColor(KPalette.red);
      //list.add(negPoint);
      negPoints.add(negPoint);
      negOld = negPoint;
    }
    for (KPoint posPt : posPoints) {
      // need to be smarter about dealing with possible nans
      if (!pointHasNans(posPt)) {
        list.add(posPt);
      } else {
        //System.out.println(posPt);
      }
    }
    for (KPoint negPt : negPoints) {
      if (!pointHasNans(negPt)) {
        list.add(negPt);
      } else {
        //System.out.println(negPt);
      }
    }
  }
  //}}}
  
  //{{{ checkPoint
  /** apparently this math is stupid because it results in sqrt of neg numbers sometimes **/
  public boolean pointHasNans(Tuple3 pt) {
    double x = pt.getX();
    double y = pt.getY();
    double z = pt.getZ();
    return (Double.isNaN(x)||Double.isNaN(y)||Double.isNaN(z));
  }
  //}}}

  //{{{ calcX
  public double calcX(double dn, double tau) {
    //double xisquared = (2 * daxial - dn) / (3 * daxial - (3/2) * drhombic);
    //double etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    if (dn / daxial > deltacrit) {
      return Math.sqrt(xisquared) * Math.cos(tau);
    } else {
      return Math.sqrt(lambdasquared) * Math.cos(tau);
    }
  }
  //}}}
  
  //{{{ calcY
  public double calcY(double dn, double tau) {
    //double etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    if (dn / daxial > deltacrit) {
      return Math.sqrt(etasquared) * Math.sin(tau);
    } else {
      return Math.sqrt(1 - lambdasquared * Math.pow(Math.cos(tau),2) - zetasquared * Math.pow(Math.sin(tau),2));
    }
  }
  //}}}
  
  //{{{ calcZ
  /** only returns positive value **/
  public double calcZ(double dn, double tau) {
    //double xisquared = (2 * daxial - dn) / (3 * daxial - (3/2) * drhombic);
    //double etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    if (dn / daxial > deltacrit) {
      return Math.sqrt(1 - xisquared * Math.pow(Math.cos(tau),2) - etasquared * Math.pow(Math.sin(tau),2));
    } else {
      return Math.sqrt(zetasquared) * Math.sin(tau);
    }
  }
  //}}}
  
}
