// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;
import king.*;
import king.core.*;
import king.points.*;

import java.util.*;
//import driftwood.isosurface.*;
import Jama.*;
//import Jama.Matrix;
import driftwood.r3.*;
import java.text.DecimalFormat;

//}}}
/**
* 
* 
* 
*
* <p>Copyright (C) 2004 Vincent Chen. All rights reserved.
* <br>Begun on Apr 09 2004.
*/
public class PlaneFinder
{
//{{{ Constants
    DecimalFormat df2 = new DecimalFormat("0.00");    
//}}}

//{{{ Variable definitions
//##################################################################################################
    RNATriple anchor = new RNATriple();
    RNATriple normal = new RNATriple();
    KList list;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */

    public PlaneFinder() {
        list = new KList(KList.TRIANGLE);
	list.setName("Plain");
	list.setWidth(5);
    }
    //}}}

    public RNATriple getNormal()
    { return (RNATriple) normal.clone(); }

    public RNATriple getAnchor()
    { return (RNATriple) anchor.clone(); }
    
//{{{ fitPlane
//##############################################################################
    /**
    * Finds a least-squares plane through all the data points.
    * You can retrieve the plane definition with {@link #getNormal()}
    * and {@link #getAnchor()}.
    * @param data a Collection of Tuple3 objects
    */
    public void fitPlane(Collection data)
    {
        anchor = calcCentroid(data);
        Matrix m = buildMatrix(data, anchor);
        
        SingularValueDecomposition svd = new SingularValueDecomposition(m);
        Matrix v = svd.getV(); // last column corresponds to min singular value
        
        normal = new RNATriple(
            v.get( 0, v.getColumnDimension()-1 ),
            v.get( 1, v.getColumnDimension()-1 ),
            v.get( 2, v.getColumnDimension()-1 )
        );
        normal.unit();  // makes vector mag 1
    }
//}}}

//{{{ calcCentroid, buildMatrix
//##############################################################################
    /** Calculates the centroid of a collection of Tuple3s. */
    RNATriple calcCentroid(Collection data)
    {
        RNATriple centroid = new RNATriple();
        for(Iterator iter = data.iterator(); iter.hasNext(); )
        {
            KPoint t = (KPoint) iter.next();
	    //System.out.println(t.getX());
            centroid.add(t);
        }
	//System.out.println(centroid);
        centroid.mult(1.0 / data.size());
	//System.out.println(centroid);
        return centroid;
    }

    public double calcPointPlaneDistance(KPoint p) {
	double d = - normal.getX() * anchor.getX() - normal.getY() * anchor.getY() - normal.getZ() * anchor.getZ();
	double dist = normal.getX() * p.getX() + normal.getY() * p.getY() + normal.getZ() * p.getZ() + d;
	return dist;
    }
    
    /** Builds the matrix M */
    Matrix buildMatrix(Collection data, RNATriple centroid)
    {
        Matrix matrix = new Matrix(data.size(), 3);
        int i = 0;
        for(Iterator iter = data.iterator(); iter.hasNext(); i++)
        {
            KPoint t = (KPoint) iter.next();
            matrix.set(i, 0, t.getX() - centroid.getX());
            matrix.set(i, 1, t.getY() - centroid.getY());
            matrix.set(i, 2, t.getZ() - centroid.getZ());
        }
        return matrix;
    }
//}}}

    public void drawNormal() {
	//RNATriple anchor = basePlane.getAnchor();
	VectorPoint centroid = new VectorPoint("centroid", null);
	//centroid.setXYZ(anchor.getX(), anchor.getY(), anchor.getZ());
	centroid.setX(anchor.getX());
	centroid.setY(anchor.getY());
	centroid.setZ(anchor.getZ());
	//centroid.setStyle(MarkerPoint.BOX_L);
	//centroid.setColor(KPalette.red);
	centroid.setName("centroid: x=" + df2.format(centroid.getX()) + ", y=" + df2.format(centroid.getY()) + ", z=" + df2.format(centroid.getZ()));
	centroid.setOn(true);
	list.add(centroid);

	//RNATriple norm = basePlane.getNormal();
	VectorPoint norm = new VectorPoint("normal", centroid);
	//normal.setXYZ(norm.getX(), norm.getY(), norm.getZ());
	norm.setX(anchor.getX() + normal.getX());
	norm.setY(anchor.getY() + normal.getY());
	norm.setZ(anchor.getZ() + normal.getZ());
	//normal.setStyle(MarkerPoint.BOX_L);
	//normal.setColor(KPalette.deadwhite);
	norm.setName("normal: x=" + df2.format(norm.getX()) + ", y=" + df2.format(norm.getY()) + ", z=" + df2.format(norm.getZ()));
	norm.setOn(true);
	list.add(norm);
    }

    public void drawPlane() {
	TrianglePoint pointA = calcPlane(2, 2, null);
	TrianglePoint pointB = calcPlane(2, -2, pointA);
	TrianglePoint pointC = calcPlane(-2, 2, pointB);
	TrianglePoint pointD = calcPlane(-2, -2, pointC);
    }

    public void drawPlane(int size) {
	TrianglePoint pointA = calcPlane(size, size, null);
	TrianglePoint pointB = calcPlane(size, -size, pointA);
	TrianglePoint pointC = calcPlane(-size, size, pointB);
	TrianglePoint pointD = calcPlane(-size, -size, pointC);
    }

    private TrianglePoint calcPlane(double first, double second, TrianglePoint prev) {
	//RNATriple normal = basePlane.getNormal();
	//RNATriple anchor = basePlane.getAnchor();
	double x, y, z;
	//y = 1; z = 1;
	double highCoord = normal.getX();
	//double lowCoordA = normal.getY();
	//double lowCoordB = normal.getZ();
	//System.out.println(normal.getX());
	//System.out.println(normal.getY());
	//System.out.println(normal.getZ());

	x = -(normal.getY() * first + normal.getZ() * second) / normal.getX();
	y = first;
	z = second;
	if (StrictMath.abs(normal.getY()) >= StrictMath.abs(highCoord)) {
	    //lowCoordA = highCoord;
	    highCoord = normal.getY();
	    y = -(normal.getX() * first + normal.getZ() * second) / normal.getY();
	    x = first;
	    z = second;
	}
	if (StrictMath.abs(normal.getZ()) >= StrictMath.abs(highCoord)) {
	    //lowCoordB = highCoord;
	    highCoord = normal.getZ();
	    z = -(normal.getX() * first + normal.getY() * second) / normal.getZ();
	    x = first;
	    y = second;
	}
	//x = -(lowCoordA * y + lowCoordB * z) / highCoord;
	TrianglePoint plane = new TrianglePoint("plane", prev);
	plane.setX(anchor.getX() + x);
	plane.setY(anchor.getY() + y);
	plane.setZ(anchor.getZ() + z);
	plane.setOn(true);
	list.add(plane);
	return plane;
    }

    public KList getList() {
	return list;
    }

    public Plane getPlane() {
	return new Plane(getAnchor(), getNormal());
    }

}//class
