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
//import driftwood.*;
//}}}
/**
* <code>SpatialBin</code> sorts points into a 3-D grid of rectangular bins
* to facilitate rapid discovery of spatial neighbors.
* Thus, when searching for a point within a distance D of a point,
* one only needs to look in bins within D (plus a margin) of that point.
* The grid is kept sparse by storing point lists in a hashtable,
* indexed on the three integer bin indices.
* This class does not guarantee the set property, so attempting to remove a
* point that has been added multiple times may have unpredictable results.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 30 09:38:47 EST 2004
*/
public class SpatialBin //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /**
    * A Map&gt;Tuple3, ArrayList&gt;Tuple3&lt;$lt; that maps integer bin indices
    * to Collections of points with Cartesian coordinates.
    */
    Map             grid = new HashMap();
    
    /** An overwriteable index for lookups. Don't use for storage or it will be overwritten! */
    Triple          lookupIndex = new Triple();
    
    /** To make sure we can treat our search point as a Triple, not just a Tuple3. */
    Triple          searchProxy = new Triple();
    
    /** The sizes of the bins in x, y, and z. */
    final double    xWidth, yWidth, zWidth;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a new spatial binning system with a grid of the given granularity.
    * As a general rule, grid size should be about the same as the distance
    * you want to use for a typical search.
    * Alternately, choose a grid small enough that on average a small percentage of
    * your data falls into any one bin, but large enough that points don't have
    * bins all to themselves, which is wasteful and inefficient to search.
    * @param width the dimension along one axis of a cubic grid cell
    */
    public SpatialBin(double width)
    {
        super();
        this.xWidth = width;
        this.yWidth = width;
        this.zWidth = width;
    }
//}}}

//{{{ getIndex
//##############################################################################
    /** Writes the integer indices of pt into index. */
    protected void getIndex(Tuple3 pt, MutableTuple3 index)
    {
        index.setX(Math.round(pt.getX()/xWidth));
        index.setY(Math.round(pt.getY()/yWidth));
        index.setZ(Math.round(pt.getZ()/zWidth));
    }
//}}}

//{{{ add, addAll
//##############################################################################
    /** Adds the given point to the appropriate bin. */
    public void add(Tuple3 pt)
    {
        getIndex(pt, lookupIndex);
        ArrayList bin = (ArrayList) grid.get(lookupIndex);
        if(bin == null)
        {
            bin = new ArrayList();
            grid.put(new Triple(lookupIndex), bin);
        }
        bin.add(pt);
    }
    
    /** Adds all the Tuple3s in this collection to their appropriate bins. */
    public void addAll(Collection c)
    {
        for(Iterator iter = c.iterator(); iter.hasNext(); )
        {
            Tuple3 pt = (Tuple3) iter.next();
            add(pt);
        }
    }
//}}}

//{{{ findSphere
//##############################################################################
    /**
    * Finds all the points in this collection whose distance to the search point
    * is less than or equal to the specified radius.
    * @return a Collection of all the points on or within the sphere
    */
    public Collection findSphere(Tuple3 search, double radius)
    {
        ArrayList found = new ArrayList();
        findSphere(search, radius, found);
        return found;
    }
    
    /**
    * Finds all the points in this collection whose distance to the search point
    * is less than or equal to the specified radius,
    * and adds them to the specified Collection.
    */
    public void findSphere(Tuple3 search, double radius, Collection found)
    {
        searchProxy.like(search);
        double r2 = radius * radius; // saves doing a sqrt() operation
        
        // We search over a rectangular selection of grid cells.
        // This is minimally inefficient for grid sizes that are
        // well-matched to the searched data set.
        int minx = (int)Math.round((search.getX()-radius)/xWidth);
        int miny = (int)Math.round((search.getY()-radius)/yWidth);
        int minz = (int)Math.round((search.getZ()-radius)/zWidth);
        int maxx = (int)Math.round((search.getX()+radius)/xWidth);
        int maxy = (int)Math.round((search.getY()+radius)/yWidth);
        int maxz = (int)Math.round((search.getZ()+radius)/zWidth);

        // An unusual syntax for nested loops, but it reduces indentation
        for(int i = minx; i <= maxx; i++)
        for(int j = miny; j <= maxy; j++)
        for(int k = minz; k <= maxz; k++)
        {
            lookupIndex.setXYZ(i, j, k);
            ArrayList bin = (ArrayList) grid.get(lookupIndex);
            if(bin != null)
            {
                // Using get() is substantially (>25%) faster than creating iterators
                //for(Iterator iter = bin.iterator(); iter.hasNext(); )
                for(int l = 0, end_l = bin.size(); l < end_l; l++)
                {
                    //Tuple3 hit = (Tuple3) iter.next();
                    Tuple3 hit = (Tuple3) bin.get(l);
                    if(searchProxy.sqDistance(hit) <= r2)
                        found.add(hit);
                }
            }
        } // end triple for loop
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ main (for testing)
//##############################################################################
    /*static public void main(String[] args)
    {
        SpatialBin bin = new SpatialBin(5);
        for(int i = 0; i < 1000; i++)
        {
            Triple p = new Triple(Math.random(), Math.random(), Math.random()).mult(100);
            bin.add(p);
        }
        for(int i = 0; i < 10; i++)
        {
            Triple p = new Triple(Math.random(), Math.random(), Math.random()).mult(100);
            double r = Math.random() * 10;
            Collection c = bin.findSphere(p, r);
            System.out.println("Within "+r+" of "+p+":");
            System.out.println(c);
            System.out.println();
            System.out.println();
        }
    }*/
//}}}
}//class

