// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.core.*;
import king.points.*;

import driftwood.r3.*;
import java.util.*;
import Jama.*;
import java.text.*;

//}}}

/**
* <code>RdcDrawer2</code> is for drawing the actual RDC curves.
* This version is based on Tony Yan's code from the Donald Lab.
* 
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* <br>Begun Tue Nov 27 15:57:02 EST 2007
**/

public class RdcDrawer2 {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.00");
  //}}}
  
  //{{{ Variables
  double sxx, syy, szz;
  Matrix saupeD, matV; // saupeD is 3x1, matV is 3x3
  //}}}
  
  //{{{ Constructor
  public RdcDrawer2(Matrix saupeDiag, Matrix eigV) {
    Matrix newV = sortEig(saupeDiag, eigV);
    matV = rightHandCoord(newV);
    calcVariables();
  }
  //}}}
  
  //{{{ drawCurve
  public void drawCurve(double rdcVal, Tuple3 center, double r, int numPoints, double backcalcRdc, KList list) {
    VectorPoint old = null;
    VectorPoint negOld = null;
    ArrayList<KPoint> posPoints = new ArrayList<KPoint>();
    ArrayList<KPoint> negPoints = new ArrayList<KPoint>();
    //need to catch special cases where rdcVal == sxx or szz
    for (double tau = 0; tau <= 2 * Math.PI; tau += 2 * Math.PI / numPoints) {
      double a=Double.NaN, b=Double.NaN, y=Double.NaN, z=Double.NaN, x=Double.NaN;
      if ((sxx < rdcVal) && (rdcVal < syy)) {
        a = Math.sqrt((rdcVal-sxx)/(syy-sxx));
        b = Math.sqrt((rdcVal-sxx)/(szz-sxx));
        y = a * Math.cos(tau);
        z = b * Math.sin(tau);
        x = Math.sqrt(1 - Math.pow(y, 2) - Math.pow(z, 2));
      } else if ((syy<=rdcVal) && (rdcVal < szz)) {
        a = Math.sqrt((szz-rdcVal)/(szz-sxx));
        b = Math.sqrt((szz-rdcVal)/(szz-syy));
        x = a * Math.cos(tau);
        y = b * Math.sin(tau);
        z = Math.sqrt(1 - Math.pow(x, 2) - Math.pow(y, 2));
      } else {
        System.out.println("RDC value not in range!");
        //a = Double.NaN;
        //return;
      }
      if (!Double.isNaN(a)) {
        Matrix changeBase = new Matrix(3, 1);
        changeBase.set(0, 0, x);
        changeBase.set(1, 0, y);
        changeBase.set(2, 0, z);
        Matrix adjFrame = matV.times(changeBase);
        adjFrame.timesEquals(r);
        VectorPoint point = new VectorPoint("Point " + df.format(rdcVal), old);
        point.setXYZ(adjFrame.get(0, 0)+center.getX(), adjFrame.get(1, 0)+center.getY(), adjFrame.get(2, 0)+center.getZ());      
        if (Math.abs(rdcVal - backcalcRdc) < 0.25) point.setColor(KPalette.green);
        else if (Math.abs(rdcVal - backcalcRdc) < 0.5) point.setColor(KPalette.yellow);
        else point.setColor(KPalette.red);
        posPoints.add(point);
        old = point;
        VectorPoint negPoint = new VectorPoint("Neg Point " + df.format(rdcVal), negOld);
        negPoint.setXYZ(-adjFrame.get(0, 0)+center.getX(), -adjFrame.get(1, 0)+center.getY(), -adjFrame.get(2, 0)+center.getZ());      
        if (Math.abs(rdcVal - backcalcRdc) < 0.25) negPoint.setColor(KPalette.green);
        else if (Math.abs(rdcVal - backcalcRdc) < 0.5) negPoint.setColor(KPalette.yellow);
        else negPoint.setColor(KPalette.red);
        negPoints.add(negPoint);
        negOld = negPoint;
      }
    }
    for (KPoint posPt : posPoints) {
      // need to be smarter about dealing with possible nans
      //if (!pointHasNans(posPt)) {
        list.add(posPt);
      //} else {
        //System.out.println(posPt);
      //}
    }
    for (KPoint negPt : negPoints) {
      //if (!pointHasNans(negPt)) {
        list.add(negPt);
      //} else {
        //System.out.println(negPt);
      //}
    }
  }
  //}}}
  
  //{{{ calcVariables
  public void calcVariables() {
    sxx = saupeD.get(0, 0);
    syy = saupeD.get(1, 0);
    szz = saupeD.get(2, 0);
    System.out.println("Sxx: "+sxx+" Syy: "+syy+" Szz: "+ szz);
    double scale = Math.max(Math.max(Math.abs(sxx), Math.abs(syy)), Math.abs(szz));
    Triple xAxis = new Triple(matV.get(0, 0), matV.get(1, 0), matV.get(2, 0));
    Triple yAxis = new Triple(matV.get(0, 1), matV.get(1, 1), matV.get(2, 1));
    Triple zAxis = new Triple(matV.get(0, 2), matV.get(1, 2), matV.get(2, 2));
  }
  //}}}
  
  //{{{ sortEig
  public Matrix sortEig(Matrix eigD, Matrix eigV) {
    saupeD = new Matrix(3, 1);
    Matrix newV = new Matrix(3, 3);
    Double d0 = new Double(eigD.get(0, 0));
    Double d1 = new Double(eigD.get(1, 1));
    Double d2 = new Double(eigD.get(2, 2));
    TreeMap<Double, Matrix> sorter = new TreeMap<Double, Matrix>();
    sorter.put(d0, eigV.getMatrix(0, 2, 0, 0));
    sorter.put(d1, eigV.getMatrix(0, 2, 1, 1));
    sorter.put(d2, eigV.getMatrix(0, 2, 2, 2));
    int i = 0;
    for (Double d : sorter.keySet()) {
      Matrix col = sorter.get(d);
      col.print(4, 4);
      saupeD.set(i, 0, d.doubleValue());
      newV.set(0, i, col.get(0, 0));
      newV.set(1, i, col.get(1, 0));
      newV.set(2, i, col.get(2, 0));
      i++;
    }
    return newV;
  }
  //}}}
  
  //{{{ rightHandCoord
  public Matrix rightHandCoord(Matrix eigV) {
    Triple firstCol = new Triple(eigV.get(0, 0), eigV.get(1, 0), eigV.get(2, 0));
    Triple secCol = new Triple(eigV.get(0, 1), eigV.get(1, 1), eigV.get(2, 1));
    Triple thirdCol = new Triple(eigV.get(0, 2), eigV.get(1, 2), eigV.get(2, 2));
    if (firstCol.dot((secCol.cross(thirdCol))) < 0) {
      eigV.set(0, 2, -eigV.get(0, 2));
      eigV.set(1, 2, -eigV.get(1, 2));
      eigV.set(2, 2, -eigV.get(2, 2));
    }
    return eigV;
  }
  //}}}     
      
  //{{{ drawAll
  public void drawAll(Tuple3 center, double r, int numPoints, double backcalcRdc, KList list) {
    for (double d = sxx; d <= szz; d=d+0.25) {
      drawCurve(d, center, r, numPoints, backcalcRdc, list);
    }
  }
  //}}}
}
