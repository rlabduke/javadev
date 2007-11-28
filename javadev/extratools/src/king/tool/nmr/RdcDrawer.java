// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.points.*;
import king.core.*;

import Jama.*;
import java.util.*;
import java.text.*;
//}}}

/**
* <code>RdcDrawer</code> is for drawing the actual RDC curves.
* 
* <br>Begun Tue Nov 27 15:57:02 EST 2007
**/
public class RdcDrawer {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.00");
  //}}}
  
  //{{{ Variables
  double daxial, drhombic, xisquared, etasquared, lambdasquared, zetasquared, deltacrit;
  double dzz, dyy, dxx;
  //}}}
  
  //{{{ Constructor
  public RdcDrawer(Matrix saupeDiag) {
    calcVariables(saupeDiag);
  }
  //}}}
  
    
  //{{{ calcVariables
  public void calcVariables(Matrix saupeDiag) {
    dzz = saupeDiag.get(0, 0);
    dyy = saupeDiag.get(2, 2);
    dxx = saupeDiag.get(1, 1);
    daxial = dzz / 2;
    drhombic = (dxx - dyy) / 3.0;
    deltacrit = dxx / daxial;
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
  public void drawAllCurves(KPoint center, KList list) {
    //VectorPoint origin = new VectorPoint("0 0 0", null);
    //origin.setXYZ(0, 0, 0);
    //list.add(origin);
    for (int i = (int)((-1 - 3/2 * drhombic/daxial) * 100); i <= 200; i = i + 5) { // avoids round off errors, I think.
      //System.out.println((double) i / 100.0);
      //System.out.println(i);
      double dn = i * daxial / 100.0;
      drawCurve(dn, center, list);
    }
  }
  //}}}
  
  //{{{ drawCurve
  public void drawCurve(double dn, KPoint center, KList list) {
    xisquared = (2 * daxial - dn) / (3 * daxial - (3/2) * drhombic);
    etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    lambdasquared = (dn + daxial + (3 / 2) * drhombic) / (3 * drhombic);
    zetasquared = (dn + daxial + (3 / 2) * drhombic) / (3 * daxial + (3 / 2) * drhombic);
    //System.out.println(xisquared);
    //System.out.println(etasquared);
    VectorPoint old = null;
    VectorPoint negOld = null;
    ArrayList<KPoint> posPoints = new ArrayList<KPoint>();
    ArrayList<KPoint> negPoints = new ArrayList<KPoint>();
    for (double tau = - Math.PI; tau <= Math.PI; tau += Math.PI / 60) {
      //System.out.println(tau);
      VectorPoint point = new VectorPoint("Point " + df.format(dn), old);
      point.setXYZ(calcX(dn, tau)+center.getX(), calcY(dn, tau)+center.getY(), calcZ(dn, tau)+center.getZ());
      if (dn > 0) point.setColor(KPalette.green);
      //list.add(origin);
      //list.add(point);
      posPoints.add(point);
      old = point;
      VectorPoint negPoint = new VectorPoint("Neg Point " + df.format(dn), negOld);
      if (dn / daxial > deltacrit) {
        negPoint.setXYZ(calcX(dn, tau)+center.getX(), calcY(dn, tau)+center.getY(), -calcZ(dn, tau)+center.getZ());
      } else {
        negPoint.setXYZ(calcX(dn, tau)+center.getX(), -calcY(dn, tau)+center.getY(), calcZ(dn, tau)+center.getZ());
      }
      if (dn > 0) negPoint.setColor(KPalette.green);
      //list.add(negPoint);
      negPoints.add(negPoint);
      negOld = negPoint;
    }
    for (KPoint posPt : posPoints) {
      list.add(posPt);
    }
    for (KPoint negPt : negPoints) {
      list.add(negPt);
    }
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
