// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;

import driftwood.r3.*;
import Jama.*;
//}}}
/**
* <code>RdcSolver</code> does the heavy lifting for the RdcVisTool, primarily
* solving for the Saupe matrix as described in Losonczi et al (1999) and taught to me
* by Tony Yan in the Donald lab.  
* 
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* <br>Begun Wed Nov 07 2007
**/
public class RdcSolver {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  Matrix vectors;
  Matrix rdcs;
  Matrix saupe;
  //}}}
  
  //{{{ Constructor
  public RdcSolver(Matrix vects, Matrix rs) {
    //Matrix test = new Matrix(3,3,5.5);
    //test.print(3, 2);
    //rdcs.print(1, 0);
    vectors = vects.copy();
    //printBigMatrix(vectors,8);
    rdcs = rs.copy();
    Matrix vectMat = calculateVectorMatrix();
    vectMat.print(1, 4);
    Matrix pInv = pseudoInverse(vectMat, 0.00000001);
    Matrix saupeVect = pInv.times(rdcs.transpose());
    printBigMatrix(pInv, 8);
    saupe = new Matrix(3,3);
    saupe.set(0, 0, saupeVect.get(0, 0));
    saupe.set(0, 1, saupeVect.get(1, 0));
    saupe.set(1, 0, saupeVect.get(1, 0));
    saupe.set(0, 2, saupeVect.get(2, 0));
    saupe.set(2, 0, saupeVect.get(2, 0));
    saupe.set(1, 1, saupeVect.get(3, 0));
    saupe.set(1, 2, saupeVect.get(4, 0));
    saupe.set(2, 1, saupeVect.get(4, 0));
    saupe.set(2, 2, - saupeVect.get(0, 0) - saupeVect.get(3, 0));
    saupeVect.print(4, 4);
    saupe.print(4, 4);
    diagonalize(saupe).print(4, 4);
    //Matrix test = vectMat.times(pInv);
    //test.print(4,4);
    //test();
  }
  //}}}
  
  //{{{ calculateVectorMatrix
  public Matrix calculateVectorMatrix() {
    Matrix vectMat = new Matrix(vectors.getColumnDimension(), 5);
    for (int i = 0; i < vectors.getColumnDimension(); i++) {
      double x = vectors.get(0, i);
      double y = vectors.get(1, i);
      double z = vectors.get(2, i);
      //System.out.println(x+", "+y+", "+z);
      vectMat.set(i, 0, x*x-z*z);
      vectMat.set(i, 1, 2*x*y);
      vectMat.set(i, 2, 2*x*z);
      vectMat.set(i, 3, y*y-z*z);
      vectMat.set(i, 4, 2*y*z);
    }
    return vectMat;
    //SingularValueDecomposition svd = vectMat.svd();
    //double[] singVals = svd.getSingularValues();
    //for (double d : singVals) {
    //  System.out.print(d + " ");
    //}
  }
  //}}}

  //{{{ pseudoInverse
  public Matrix pseudoInverse(Matrix p, double eps) {
    SingularValueDecomposition svd = p.svd();
    Matrix u = svd.getU();
    Matrix v = svd.getV();
    Matrix s = svd.getS();
    Matrix sPinv = s.copy();
    int minDim = Math.min(s.getColumnDimension(), s.getRowDimension());
    for (int i = 0; i < minDim; i++) {
      double value = s.get(i, i);
      if (value < eps) sPinv.set(i, i, 0);
      else sPinv.set(i, i, 1/value);
    }
    return (v.times(sPinv.transpose())).times(u.transpose());
  }
  //}}}
  
  //{{{ diagonalize
  public Matrix diagonalize(Matrix m) {
    EigenvalueDecomposition evd = m.eig();
    return evd.getD();
  }
  //}}}
  
  //{{{ getSaupeMatrix
  public Matrix getSaupeMatrix() {
    return saupe;
  }
  //}}}
  
  //{{{ getSaupeDiagonalized
  public Matrix getSaupeDiagonalized() {
    return diagonalize(saupe);
  }
  //}}}
  
  //{{{ getSaupeEigenvectors
  public Matrix getSaupeEigenvectors() {
    return saupe.eig().getV();
  }
  //}}}
  
  //{{{ backCalculateRdc
  public double backCalculateRdc(Tuple3 vector) {
    Matrix vect = new Matrix(3, 1);
    vect.set(0, 0, vector.getX());
    vect.set(1, 0, vector.getY());
    vect.set(2, 0, vector.getZ());
    Matrix ans = vect.transpose().times(saupe).times(vect);
    //ans.print(1, 3);
    return ans.get(0, 0);
  }
  //}}}
  
  //{{{ printBigMatrix
  public void printBigMatrix(Matrix big, int numCol) {
    int maxCol = big.getColumnDimension();
    int j = 0;
    int i = 0;
    int lasti = -1;
    for (i = 0; i < maxCol; i++) {
      if (j == numCol - 1) {
        System.out.println("Columns "+(i-numCol+1)+" through "+i);
        Matrix subMat = big.getMatrix(0,big.getRowDimension()-1,i-numCol+1,i);
        subMat.print(1,4);
        lasti = i;
        j = -1;
      }
      j++;
    }
    System.out.println("Columns "+(lasti+1)+" through "+(maxCol-1));
    Matrix subMat = big.getMatrix(0,big.getRowDimension()-1,lasti+1,maxCol - 1);
    subMat.print(1,4);
  }
  //}}}  
  
  //{{{ test
  public void test() {
    Matrix testM = new Matrix(5, 4);
    testM.set(0, 0, 1);
    testM.set(0, 3, 2);
    testM.set(1, 2, 3);
    testM.set(3, 1, 4);
    testM.set(4, 0, 5);
    testM.print(1, 0);
    SingularValueDecomposition svd = testM.svd();
    Matrix u = svd.getU();
    Matrix v = svd.getV();
    Matrix s = svd.getS();
    u.print(1, 3);
    s.print(1, 3);
    v.print(1, 3);
    testM.times(pseudoInverse(testM, 0.00001)).times(testM).print(1, 3);
  }
  //}}}
}
