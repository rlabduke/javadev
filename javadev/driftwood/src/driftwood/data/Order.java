// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.data;

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
* <code>Order</code> contains static utility functions for dealing with the
* order of elements in arrays.
*
* Unlike the <code>sort()</code> functions in {@link java.util.Arrays},
* no special precautions are taken to deal with NaN's and +0 vs -0.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Apr 27 10:37:46 EDT 2004
*/
public class Order //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Order()
    {
        super();
    }
//}}}

//{{{ sort(double), permute(double)
//##############################################################################
    /**
    * Sorts the array <code>x</code> in place, in ascending order, while
    * simultaneously generating a <i>permutation vector</i> <code>p</code>,
    * such <code>p</code> can be used with the <code>permute()</code> functions
    * to sort other arrays into the same order.
    * In fact, <code>x</code> will have been permuted such that the element that
    * is now at index <code>i</code> used to be at index <code>p[i]</code>.
    * This is useful for things like multi-column sorts.
    * <p>The sort is stable and has a worst-case running time of O(n ln n).
    * @param x      the values to sort
    * @return       the permutation vector <code>p</code>.
    */
    static public int[] sort(double[] x)
    {
        int[] p = new int[x.length];
        for(int i = 0; i < p.length; i++) p[i] = i;
        
        if(x.length < 16) insertionSort(x, p, 0, x.length);
        else mergeSort((double[])x.clone(), x, (int[])p.clone(), p, 0, x.length);
        
        return p;
    }
    
    /**
    * Given a permutation vector <code>p</code>, re-order the input array <code>x</code>
    * such that the element that was at <code>p[i]</code> is moved to <code>i</code>.
    * This rearrangement cannot be done in place.
    */
    static public double[] permute(double[] x, int[] p)
    {
        double[] y = new double[x.length];
        for(int i = 0; i < x.length; i++) y[i] = x[ p[i] ];
        return y;
    }
//}}}

//{{{ mergeSort(double, int)
//##############################################################################
    /**
    * Sorts the specified subsection of x in place while simultaneously
    * permuting the elements of p in the same way.
    * Upon entry, xi and xo (and pi and po) must have identical contents.
    * During the course of the algorithm, the contents of BOTH may be changed.
    * <p>The sort is stable and has a worst-case running time of O(n ln n).
    * @param xi     the values to sort (input)
    * @param xo     the sorted values (output)
    * @param pi     the auxillary array to permute (input)
    * @param po     the permuted auxillary array (output)
    * @param start  the starting index for the sort, inclusive
    * @param end    the ending index for the sort, exclusive
    */
    static private void mergeSort(double[] xi, double[] xo, int[] pi, int[] po, int start, int end)
    {
        int i, j, len = end - start;
        // Use insertion sort on short sub-arrays
        if(len <= 8)
        {
            // For each input element from start to end
            for(i = start; i < end; i++)
            {
                final double xkey = xi[i]; // saves array lookups
                // Shuffle greater-or-equal output values to the right
                for(j = i-1; j >= start && xkey < xo[j]; j--)
                {
                    xo[j+1] = xo[j];
                    po[j+1] = po[j];
                }
                // Copy this input element to its place in the output
                xo[j+1] = xkey;
                po[j+1] = pi[i];
            }
        }
        // Use merge sort on longer arrays. We trickle all the way to the bottom
        // with xi and xo being identical. On return, we can merge from xi into xo.
        else
        {
            int mid = (start+end)/2;
            mergeSort(xo, xi, po, pi, start, mid);
            mergeSort(xo, xi, po, pi, mid, end);
            
            // If they're already merged, just copy them into the output array
            if(xi[mid-1] < xi[mid])
            {
                System.arraycopy(xi, start, xo, start, len);
                System.arraycopy(pi, start, po, start, len);
            }
            // Otherwise, merge element-by-element
            else
            {
                i = start; j = mid;
                for(int cnt = start; cnt < end; cnt++)
                {
                    if(i == mid || (j < end && xi[j] < xi[i]))
                    {
                        xo[cnt] = xi[j];
                        po[cnt] = pi[j];
                        j++;
                    }
                    else
                    {
                        xo[cnt] = xi[i];
                        po[cnt] = pi[i];
                        i++;
                    }
                }
            }
        }
    }
//}}}

//{{{ insertionSort(double, int)
//##############################################################################
    /**
    * Sorts the specified subsection of x in place while simultaneously
    * permuting the elements of p in the same way.
    * <p>The sort is stable and has a worst-case running time of O(n^2).
    * @param x      the values to sort
    * @param p      the auxillary array to permute
    * @param start  the starting index for the sort, inclusive
    * @param end    the ending index for the sort, exclusive
    */
    static public void insertionSort(double[] x, int[] p, int start, int end)
    {
        double xkey;
        int i, j, pkey;
        // For each element after the first one
        for(i = start+1; i < end; i++)
        {
            xkey = x[i];
            pkey = p[i];
            for(j = i-1; j >= start && xkey < x[j]; j--)
            {
                x[j+1] = x[j];
                p[j+1] = p[j];
            }
            x[j+1] = xkey;
            p[j+1] = pkey;
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ swap
//##############################################################################
    /** Swaps x[i] and x[j] in place */
    static public void swap(int[] x, int i, int j)
    {
        int tmp = x[i];
        x[i] = x[j];
        x[j] = tmp;
    }

    /** Swaps x[i] and x[j] in place */
    static public void swap(double[] x, int i, int j)
    {
        double tmp = x[i];
        x[i] = x[j];
        x[j] = tmp;
    }
//}}}

//{{{ main -- for testing
//##############################################################################
    /*static public void main(String[] args)
    {
        double[] test = new double[(int)Math.floor(Math.random()*10000)];
        System.out.println("Testing with "+test.length+" random values between 0 and 1");
        for(int i = 0; i < test.length; i++) test[i] = Math.random();
        
        double[] test1 = (double[])test.clone();
        double[] test2 = (double[])test.clone();
        double[] test3 = (double[])test.clone();
        
        Arrays.sort(test); // our gold standard
        
        int[] p = Order.sort(test1);
        if(Arrays.equals(test, test1))  System.out.println("    Order.sort() works correctly.");
        else                            System.out.println("*** Order.sort() is broken!");
        
        Order.insertionSort(test2, new int[test2.length], 0, test2.length);
        if(Arrays.equals(test, test2))  System.out.println("    Order.insertionSort() works correctly.");
        else                            System.out.println("*** Order.insertionSort() is broken!");
        
        test3 = Order.permute(test3, p);
        if(Arrays.equals(test, test3))  System.out.println("    Order.permute() works correctly.");
        else                            System.out.println("*** Order.permute() is broken!");
    }*/
//}}}
}//class

