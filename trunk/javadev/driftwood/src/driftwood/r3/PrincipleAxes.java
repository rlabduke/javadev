// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.r3;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.*;
import Jama.*;
//}}}
/**
* <code>PrincipleAxes</code> uses Principle Components Analysis
* and the JAMA matrix libraries to calculate the principle components of
* a set of 3-D points.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Apr 26 15:13:42 EDT 2004
*/
public class PrincipleAxes //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ CLASS: Sortable
//##############################################################################
    /** Used for sorting eigenvectors. */
    static private class Sortable implements Comparable
    {
        final public double value;
        final public int index;
        
        public Sortable(double value, int index)
        {
            this.value  = value;
            this.index = index;
        }
        
        // Sorts largest first, not smallest
        public int compareTo(Object o)
        {
            Sortable that = (Sortable) o;
            if(this.value < that.value)         return 1;
            else if(this.value > that.value)    return -1;
            else                                return 0;
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Principle component vectors in columns; greatest in col 0 */
    Matrix principleComponents;
    /** Weights of the components, sorted in the same order */
    double[] pcaEigenvalues;
    /** Transpose/inverse (same thing) of principleComponents */
    Transform pcaTransform;
    /** The minimum and maximum (x,y,z) values for input data in transformed coordinates */
    Triple boxMin, boxMax;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Compute the principle axes of a collection of Tuple3s.
    */
    public PrincipleAxes(Collection data)
    {
        super();
        
        // Do principle components analysis
        Matrix features = packTuples(data);
        Matrix cov = covariance(features);
        EigenvalueDecomposition eig = cov.eig();
        this.pcaEigenvalues = new double[3];
        this.principleComponents = sortEigenvectors(eig, pcaEigenvalues);
        
        // Form rotation matrix for going from Cartesian coordinates
        // to coordinates with the PCA as a basis set
        this.pcaTransform = new Transform().likeMatrix(
             principleComponents.get(0,0), principleComponents.get(1,0), principleComponents.get(2,0),
             principleComponents.get(0,1), principleComponents.get(1,1), principleComponents.get(2,1),
             principleComponents.get(0,2), principleComponents.get(1,2), principleComponents.get(2,2)
        );
        
        // Calculate bounding box for rotated points
        this.boxMin = new Triple( Double.MAX_VALUE,  Double.MAX_VALUE,  Double.MAX_VALUE);
        this.boxMax = new Triple(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
        Triple work = new Triple();
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            Tuple3 t = (Tuple3) iter.next();
            pcaTransform.transform(t, work);
            boxMin.setX(Math.min(boxMin.getX(), work.getX()));
            boxMin.setY(Math.min(boxMin.getY(), work.getY()));
            boxMin.setZ(Math.min(boxMin.getZ(), work.getZ()));
            boxMax.setX(Math.max(boxMax.getX(), work.getX()));
            boxMax.setY(Math.max(boxMax.getY(), work.getY()));
            boxMax.setZ(Math.max(boxMax.getZ(), work.getZ()));
        }
    }
//}}}

//{{{ getTransform, getAxes, getLengths
//##############################################################################
    /**
    * Will transform coordinates from the input space to PCA space.
    * This is a pure rotation, with no scaling, translation, or shearing.
    */
    public Transform getTransform()
    {
        return new Transform().like(pcaTransform);
    }
    
    /** Returns the 3 principle components as normalized vectors, with the major one first. */
    public Tuple3[] getAxes()
    {
        Triple[] out = new Triple[3];
        out[0] = new Triple(principleComponents.get(0,0), principleComponents.get(1,0), principleComponents.get(2,0));
        out[1] = new Triple(principleComponents.get(0,1), principleComponents.get(1,1), principleComponents.get(2,1));
        out[2] = new Triple(principleComponents.get(0,2), principleComponents.get(1,2), principleComponents.get(2,2));
        return out;
    }
    
    /** Returns the relative lengths of the components (that is, the eigenvalues), sorted in descending order. */
    public double[] getLengths()
    {
        return (double[]) pcaEigenvalues.clone();
    }
//}}}

//{{{ getKinCenter, getKinSpan
//##############################################################################
    /** Returns the center of the PCA bounding box in Cartesian coords. */
    public Tuple3 getKinCenter()
    {
        // Compute box center and backtranslate to Cartesian coords
        Matrix center = new Matrix(3,1);
        center.set(0, 0, (boxMax.getX()+boxMin.getX())/2);
        center.set(1, 0, (boxMax.getY()+boxMin.getY())/2);
        center.set(2, 0, (boxMax.getZ()+boxMin.getZ())/2);
        center = principleComponents.times(center);
        return new Triple(center.get(0,0), center.get(1,0), center.get(2,0));
    }
    
    /** Returns the (Cartesian space) length of the longest side of the (PCA space) bounding box. */
    public double getKinSpan()
    { return boxMax.getX() - boxMin.getX(); }
//}}}

//{{{ getKinView
//##############################################################################
    /**
    * Returns a preformatted kinemage view that will ensure all the points
    * are visible and that their least significant principle component
    * is aligned with the viewing axis.
    */
    public String getKinView(String viewID, String viewLabel)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        StringBuffer out = new StringBuffer();
        out.append("@").append(viewID).append("viewid {").append(viewLabel).append("}\n");
        
        // Compute span and z-slab
        double span = 1.05 * getKinSpan(); // largest side
        double zslab = 200; // as deep as it is wide
        out.append("@").append(viewID).append("span ").append(df.format(span)).append("\n");
        out.append("@").append(viewID).append("zslab ").append(df.format(zslab)).append("\n");
        
        // Compute box center and backtranslate to Cartesian coords
        Tuple3 center = getKinCenter();
        out.append("@").append(viewID).append("center ").append(df.format(center.getX()));
        out.append(" ").append(df.format(center.getY()));
        out.append(" ").append(df.format(center.getZ())).append("\n");
        
        // Write out transpose of transformation matrix (== original PCA matrix)
        out.append("@").append(viewID).append("matrix");
        for(int r = 0; r < 3; r++)
            for(int c = 0; c < 3; c++)
                out.append(" ").append(df.format(principleComponents.get(r,c)));
        out.append("\n");
        
        return out.toString();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ packTuples
//##############################################################################
    /**
    * Takes a Collection of Tuple3s and packs them into the rows of
    * a Matrix, suitable for using with covariance().
    */
    static Matrix packTuples(Collection data)
    {
        int rows = data.size();
        Matrix out = new Matrix(rows, 3);
        int r = 0;
        for(Iterator iter = data.iterator(); iter.hasNext(); r++)
        {
            Tuple3 t = (Tuple3) iter.next();
            out.set(r, 0, t.getX());
            out.set(r, 1, t.getY());
            out.set(r, 2, t.getZ());
        }
        return out;
    }
//}}}

//{{{ covariance
//##############################################################################
    /**
    * Given a matrix where the features (data points) are stored as rows,
    * calculate the covariance matrix.
    * The covariance matrix is square, and equal in dimension to the number of
    * columns in the input data.
    */
    static Matrix covariance(Matrix data)
    {
        int rows = data.getRowDimension(), cols = data.getColumnDimension();
        
        // Calculate the mean and subtract it from each element
        double mean[] = new double[cols];
        for(int r = 0; r < rows; r++)           // sum over rows and cols
            for(int c = 0; c < cols; c++)
                mean[c] += data.get(r, c);
        for(int c = 0; c < cols; c++)           // divide by number of rows
            mean[c] /= rows;
        Matrix adjusted = new Matrix(rows, cols);
        for(int r = 0; r < rows; r++)           // subtract it out
            for(int c = 0; c < cols; c++)
                adjusted.set(r, c, data.get(r, c) - mean[c]);
        
        // The mean of adjusted in each column is now zero
        
        // Calculate the covariance matrix
        Matrix cov = adjusted.transpose().times(adjusted);
        cov.timesEquals(1.0 / rows);
        
        return cov;
    }
//}}}

//{{{ sortEigenvectors
//##############################################################################
    /**
    * Given an eigenvalue decomposition with real eigenvalues, sort the
    * eigenvectors by the ascending order of their eigenvalues.
    * <p>I'm pretty sure the Jama always sorts the eigenvectors for symmetric
    * matrices anyway, but that's from a quick reading of the source code
    * and not from the javadocs.
    * (Jama sorts in ascending order, though.)
    * @param eigenvals      will be overwritten with sorted eigenvalues
    * @return a sorted version of the EigenvalueDecomposition matrix V,
    *   with the most significant principle component in the first column
    *   and the least significant one in the last column.
    */
    static Matrix sortEigenvectors(EigenvalueDecomposition eig, double[] eigenvals)
    {
        Matrix d = eig.getD();
        Matrix v = eig.getV();
        int rows = v.getRowDimension(), cols = v.getColumnDimension();
        
        Sortable[] sort = new Sortable[cols];
        for(int c = 0; c < cols; c++)
            sort[c] = new Sortable(d.get(c, c), c);
        Arrays.sort(sort);
        
        Matrix v2 = new Matrix(rows, cols);
        for(int c = 0; c < cols; c++)
        {
            eigenvals[c] = sort[c].value;
            for(int r = 0; r < rows; r++)
                v2.set(r, c, v.get(r, sort[c].index));
        }
        
        return v2;
    }
//}}}

//{{{ main - for testing
//##############################################################################
    /*public static void main(String[] args)
    {
        try
        {
            ArrayList data = new ArrayList();
            LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
            String s;
            while((s = in.readLine()) != null)
            {
                try
                {
                    String[] line = Strings.explode(s, ' ');
                    Triple t = new Triple(
                        Double.parseDouble(line[0]),
                        Double.parseDouble(line[1]),
                        Double.parseDouble(line[2])
                    );
                    data.add(t);
                }
                catch(NumberFormatException ex) { System.err.println(ex.getMessage()); }
                catch(IndexOutOfBoundsException ex) { System.err.println(ex.getMessage()); }
            }
            
            //Matrix features = packTuples(data);
            //Matrix cov = covariance(features);
            
            //EigenvalueDecomposition eig = cov.eig();
            //eig.getD().print(20, 8);
            //eig.getV().print(20, 8);
            
            //Matrix pca = sortEigenvectors(eig);
            //pca.print(20, 8);
            //pca.transpose().times(pca).print(20, 8);
            PrincipleAxes axes = new PrincipleAxes(data);
            System.out.println( axes.getKinView("", "PCA view") );
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }*/
//}}}
}//class

