// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package rdcvis;
//import king.*;
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
  DecimalFormat df2 = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  double sxx, syy, szz;
  Matrix saupeD, matV; // saupeD is 3x1, matV is 3x3
  //}}}
  
  //{{{ Constructor
  public RdcDrawer2(Matrix saupeDiag, Matrix eigV) {
    Matrix newV = sortEig(saupeDiag, eigV);
    //saupeDiag.print(3, 3);
    //eigV.print(3, 3);
    matV = rightHandCoord(newV);
    calcVariables();
  }
  //}}}
  
  //{{{ drawCurve
  /** 
   *  Handles the actual calculation and drawing of the RDC curves.  Note that for some reason, this
   *  calculation code assumes the radius of the sphere is 1, and then shrinks the curves down 
   *  to the proper radius.  When I tried using the radius in the xyz coordinate calculation,
   *  some curves wouldnt' be properly divided up into neg and pos lists.
   **/
  public void drawCurve(double rdcVal, Tuple3 center, Triple modelVect, double r, int numPoints, double backcalcRdc, KList list, String text, double error) {
    drawCurve(rdcVal, center, modelVect, r, numPoints, backcalcRdc, list, text, error, null);
  }
  
  public void drawCurve(double rdcVal, Tuple3 center, Triple modelVect, double r, int numPoints, double backcalcRdc, KList list, String text, double error, KPaint color) {
    VectorPoint old = null;
    VectorPoint negOld = null;
    ArrayList<KPoint> posPoints = new ArrayList<KPoint>();
    ArrayList<KPoint> negPoints = new ArrayList<KPoint>();
    list.setColor(KPalette.hotpink);
    //BallPoint testPoint = new BallPoint("testpoint");
    //testPoint.setXYZ(modelVect.getX()+center.getX(), modelVect.getY()+center.getY(), modelVect.getZ()+center.getZ());
    //list.add(testPoint);
    boolean rdcIsOut = false;
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
        rdcIsOut = true;
        //System.out.println("RDC value ("+rdcVal+") not in range ("+sxx+"-"+szz+")!");
        //a = Double.NaN;
        //return;
      }
      if (!Double.isNaN(a)&&!Double.isNaN(x)&&!Double.isNaN(z)) {
        Matrix changeBase = new Matrix(3, 1);
        changeBase.set(0, 0, x);
        changeBase.set(1, 0, y);
        changeBase.set(2, 0, z);
        Matrix adjFrame = matV.times(changeBase);
        adjFrame.timesEquals(r);
        VectorPoint point = new VectorPoint("Pos " + text, old);
        point.setXYZ(adjFrame.get(0, 0)+center.getX(), adjFrame.get(1, 0)+center.getY(), adjFrame.get(2, 0)+center.getZ());
        if (color == null) {
          if (modelVect != null) {
            double dist = modelVect.distance(new Triple(adjFrame.get(0, 0), adjFrame.get(1, 0), adjFrame.get(2, 0)));
            if (dist < 0.1) list.setColor(KPalette.green);
            else if ((dist < 0.5)&&(!list.getColor().equals(KPalette.green))) list.setColor(KPalette.orange);
          }// else {
          if (Math.abs(rdcVal - backcalcRdc) < error) point.setColor(KPalette.greentint);
          else if (Math.abs(rdcVal - backcalcRdc) < 2*error) point.setColor(KPalette.orange);
          else point.setColor(KPalette.hotpink);
          //}
        } else {
          point.setColor(color);
        }
        posPoints.add(point);
        old = point;
        VectorPoint negPoint = new VectorPoint("Neg " + text, negOld);
        negPoint.setXYZ(-adjFrame.get(0, 0)+center.getX(), -adjFrame.get(1, 0)+center.getY(), -adjFrame.get(2, 0)+center.getZ());      
        if (color == null) {
          if (modelVect != null) {
            double dist = modelVect.distance(new Triple(-adjFrame.get(0, 0), -adjFrame.get(1, 0), -adjFrame.get(2, 0)));
            if (dist < 0.1) list.setColor(KPalette.green);
            else if ((dist < 0.5)&&(!list.getColor().equals(KPalette.green))) list.setColor(KPalette.orange);
            //else negPoint.setColor(KPalette.hotpink);
          }// else {
          if (Math.abs(rdcVal - backcalcRdc) < error) negPoint.setColor(KPalette.greentint);
          else if (Math.abs(rdcVal - backcalcRdc) < 2*error) negPoint.setColor(KPalette.orange);
          else negPoint.setColor(KPalette.hotpink);
          //}
        } else {
          negPoint.setColor(color);
        }
        negPoints.add(negPoint);
        negOld = negPoint;
      }
    }
    if (rdcIsOut) {
      System.out.println(text+" not drawn");
      System.out.println("RDC value ("+rdcVal+") not in range ("+sxx+" -> "+szz+")!");
      rdcIsOut = false;
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
  
  //{{{ analyzeVector
  /** @return double containing info about how well a vector matches a curve */
  public double analyzeVector(double rdcVal, Tuple3 center, Triple modelVect, double r, int numPoints) {
    VectorPoint old = null;
    VectorPoint negOld = null;
    ArrayList<KPoint> posPoints = new ArrayList<KPoint>();
    ArrayList<KPoint> negPoints = new ArrayList<KPoint>();
    boolean rdcIsOut = false;
    //need to catch special cases where rdcVal == sxx or szz
    double minDist = Double.MAX_VALUE;
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
        rdcIsOut = true;
        //System.out.println("RDC value not in range!");
      }
      if (!Double.isNaN(a)&&!Double.isNaN(x)&&!Double.isNaN(z)) {
        Matrix changeBase = new Matrix(3, 1);
        changeBase.set(0, 0, x);
        changeBase.set(1, 0, y);
        changeBase.set(2, 0, z);
        Matrix adjFrame = matV.times(changeBase);
        adjFrame.timesEquals(r);
        Triple point = new Triple();
        point.setXYZ(adjFrame.get(0, 0)+center.getX(), adjFrame.get(1, 0)+center.getY(), adjFrame.get(2, 0)+center.getZ());      
        if (modelVect != null) {
          double dist = modelVect.distance(new Triple(adjFrame.get(0, 0), adjFrame.get(1, 0), adjFrame.get(2, 0)));
          //System.out.print(df2.format(modelVect.getX()+center.getX())+","+df2.format(modelVect.getY()+center.getY())+","+df2.format(modelVect.getZ()+center.getZ())+":");
          //System.out.println(df2.format(adjFrame.get(0, 0)+center.getX())+","+ df2.format(adjFrame.get(1, 0)+center.getY())+","+ df2.format(adjFrame.get(2, 0)+center.getZ())+" pos "+dist + "->" + minDist);
          if (dist < minDist) minDist = dist;
        }
        
        Triple negPoint = new Triple();
        negPoint.setXYZ(-adjFrame.get(0, 0)+center.getX(), -adjFrame.get(1, 0)+center.getY(), -adjFrame.get(2, 0)+center.getZ());      
        if (modelVect != null) {
          double dist = modelVect.distance(new Triple(-adjFrame.get(0, 0), -adjFrame.get(1, 0), -adjFrame.get(2, 0)));
          //System.out.print(df2.format(modelVect.getX()+center.getX())+","+df2.format(modelVect.getY()+center.getY())+","+df2.format(modelVect.getZ()+center.getZ())+":");
          //System.out.println(df2.format(-adjFrame.get(0, 0)+center.getX())+","+ df2.format(-adjFrame.get(1, 0)+center.getY())+","+ df2.format(-adjFrame.get(2, 0)+center.getZ())+" neg "+dist + "->" + minDist);
          //System.out.print((modelVect.getX()+center.getX())+","+(modelVect.getY()+center.getY())+","+(modelVect.getZ()+center.getZ())+":");
          //System.out.println((-adjFrame.get(0, 0)+center.getX())+","+ (-adjFrame.get(1, 0)+center.getY())+","+ (-adjFrame.get(2, 0)+center.getZ())+" neg "+dist + "->" + minDist);
          if (dist < minDist) minDist = dist;
        }
      }
    }
    if (rdcIsOut) {
      System.out.println("RDC value ("+rdcVal+") not in range ("+sxx+" -> "+szz+")!");
      rdcIsOut = false;
      return -1.0;
    }
    if (minDist != Double.MAX_VALUE) {
      return minDist;
    }
    return -1.0;
  }
  //}}}
  
  //{{{ drawSurface
  public void drawSurface(double rdcVal, Tuple3 center, KList list) {
    //for (double xVal = -1; xVal <= 1; xVal = xVal + 0.05) {
    //  for (double yVal = -1; yVal <= 1; yVal = yVal + 0.05) {
    //for (double radius = 0; radius <=1.1; radius = radius + 0.05) {
    //  for (double xVal = -radius*5; xVal <= radius*5; xVal = xVal+0.01) {
    //    double yTemp = Math.sqrt(radius-xVal*xVal);
    //    for (int i = -1; i <= 1; i = i + 2) {
    //      double yVal = yTemp*i;
    for (double radius = 0; radius <=1.1; radius = radius + 0.025) {
      for (double tau = 0; tau <= 2 * Math.PI; tau += 2 * Math.PI / (10+radius*200)) {
        double xVal = shake(radius*Math.sin(tau));
        double yVal = shake(radius*Math.cos(tau));
          double zVal = Math.sqrt(-sxx/szz*xVal*xVal-syy/szz*yVal*yVal+rdcVal/szz);
          if (!Double.isNaN(zVal)) {
            Matrix changeBase = new Matrix(3, 1);
            changeBase.set(0, 0, xVal);
            changeBase.set(1, 0, yVal);
            changeBase.set(2, 0, zVal);
            Matrix adjFrame = matV.times(changeBase);
            //adjFrame.timesEquals(r); //adjust radius
            double x = adjFrame.get(0, 0);
            double y = adjFrame.get(1, 0);
            double z = adjFrame.get(2, 0);
            BallPoint point = new BallPoint("surface "+df.format(xVal)+","+df.format(yVal)+","+df.format(zVal));
            point.setRadius((float)0.01);
            point.setXYZ(x+center.getX(), y+center.getY(), z+center.getZ());
            list.add(point);
            BallPoint negPoint = new BallPoint("-surface "+df.format(xVal)+","+df.format(yVal)+","+df.format(zVal));
            negPoint.setRadius((float)0.01);
            negPoint.setXYZ(-x+center.getX(), -y+center.getY(), -z+center.getZ());
            list.add(negPoint);
          }
        //}
      }
    }
    BallPoint inSphere = new BallPoint("internuclear sphere");
    inSphere.setRadius((float)1);
    inSphere.setXYZ(center.getX(),center.getY(),center.getZ());
    inSphere.setColor(KPalette.sky);
    list.add(inSphere);
  }
  //}}}
  
  //{{{ shake
  public double shake(double d) {
    //double shakeFactor = d/10;
    double randFactor = (2*Math.random()-1)*0.01;
    return d + randFactor;
  }
  //}}}
  
  //{{{ drawTriangleSurface
  /* not currently working, draws triangles across wrong axis 090122*/
  public void drawTriangleSurface(double rdcVal, Tuple3 center, KList list) {
    //for (double xVal = -1; xVal <= 1; xVal = xVal + 0.05) {
    //  for (double yVal = -1; yVal <= 1; yVal = yVal + 0.05) {
    //for (double radius = 0; radius <=1.1; radius = radius + 0.05) {
    //  for (double xVal = -radius*5; xVal <= radius*5; xVal = xVal+0.01) {
    //    double yTemp = Math.sqrt(radius-xVal*xVal);
    //    for (int i = -1; i <= 1; i = i + 2) {
    //      double yVal = yTemp*i;
    TrianglePoint prev = null;
    boolean radIn = true;
    for (double radius = 0; radius <=1.1; radius = radius + 0.1) {
      for (double tau = 0; tau <= 2 * Math.PI; tau += 2 * Math.PI / (10+radius*20)) {
        double xVal;
        double yVal;
        if (radIn) {
          xVal = radius*Math.sin(tau);
          yVal = radius*Math.cos(tau);
        } else {
          xVal = (radius+0.1)*Math.sin(tau);
          yVal = (radius+0.1)*Math.cos(tau);
        }
        double zVal = Math.sqrt(-sxx/szz*xVal*xVal-syy/szz*yVal*yVal+rdcVal/szz);
        if (!Double.isNaN(zVal)) {
          Matrix changeBase = new Matrix(3, 1);
          changeBase.set(0, 0, xVal);
          changeBase.set(1, 0, yVal);
          changeBase.set(2, 0, zVal);
          Matrix adjFrame = matV.times(changeBase);
          //adjFrame.timesEquals(r); //adjust radius
          double x = adjFrame.get(0, 0);
          double y = adjFrame.get(1, 0);
          double z = adjFrame.get(2, 0);
          TrianglePoint point = new TrianglePoint("surface "+df.format(xVal)+","+df.format(yVal)+","+df.format(zVal), prev);
          point.setRadius((float)0.01);
          point.setXYZ(x+center.getX(), y+center.getY(), z+center.getZ());
          list.add(point);
          //BallPoint negPoint = new BallPoint("-surface "+df.format(xVal)+","+df.format(yVal)+","+df.format(zVal));
          //negPoint.setRadius((float)0.01);
          //negPoint.setXYZ(-x+center.getX(), -y+center.getY(), -z+center.getZ());
          //list.add(negPoint);
          radIn = !radIn;
          prev = point;
        }
      }
    }
    //BallPoint inSphere = new BallPoint("internuclear sphere");
    //inSphere.setRadius((float)1);
    //inSphere.setXYZ(center.getX(),center.getY(),center.getZ());
    //inSphere.setColor(KPalette.sky);
    //list.add(inSphere);
  }
  //}}}
  
  //{{{ calcVariables
  public void calcVariables() {
    sxx = saupeD.get(0, 0);
    syy = saupeD.get(1, 0);
    szz = saupeD.get(2, 0);
    //System.out.println("Sxx: "+sxx+" Syy: "+syy+" Szz: "+ szz);
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
      //col.print(4, 4);
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
    for (double d = sxx; d <= szz; d=d+1) {
      drawCurve(d, center, null, r, numPoints, backcalcRdc, list, df.format(d), 2);
    }
  }
  //}}}
}
