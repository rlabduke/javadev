// (jEdit options) :folding=explicit:collapseFolds=1:
package silk;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.util.Strings;
/**
* <code>NDimTable</code> implements an n-dimensional lookup table of real-number values.
* The table covers a "rectangular" range; i.e., each dimension <i>d</i>
* is confined to some interval [min<sub>d</sub>, max<sub>d</sub>].
* Each dimension may optionally be "wrapped" such that min<sub>d</sub> and max<sub>d</sub>
* are two ways of referring to the same thing.
* On each dimension the table is subdivided into nbins<sub>d</sub> "bins" or sample points.
* Thus, it is each n-dimensional bin that has a single floating-point value.
* There is no explicit "memory" of the input points; their identity merges with one (or more) bins.
* <p>The basic behavior is to simply add an input point to the bin it falls into,
* but various smoothing functions are available to distribute the point over multiple bins.
* The class is intended for making histograms and (particularly) density traces.
* The various <i>n</i>-dimensional entities (points/vectors, bins, etc.)
* are represented as primitive arrays of length <i>n</i>.
* <p>Using the Gaussian and cosine functions for density traces has some hang-ups,
* particularly if one varies the mask radii.
* See the README file associated with the <code>boundrotamers</code> project for all the details.
* <p><b>Note that no checking of points or indices is performed!</b>
* If you are not certain that a point or set of bin indices falls within the table, use <code>contains()</code> to check.
* Submission of invalid values will result in unpredictable behavior and/or exceptions being thrown.
*
* <p>Begun on Wed Mar 20 16:47:59 EST 2002
* <br>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
*/
abstract public class NDimTable //extends ... implements ...
{
//{{{ Static fields
    /**
    * The number of mask radii that tallyGaussian() explores out to.
    * Note that at one mask radius, the value has fallen to 10% of its maximum (by definition).
    * But at two mask radii, the value is only 0.01% of its maximum.
    * At two-and-a-half, it's less than a millionth; at three, less than a billionth.
    */
    static final double GSN_REACH = 2.5;
//}}}

//{{{ Variables
//##################################################################################################
    /** The name of this table, for later reference */
    String          ourName;
    /** Number of dimensions the table exists in */
    int             nDim;
    /** Minimum and maximum boundaries for each of n dimensions */
    double[]        minVal, maxVal;
    /** Number of subdivisions on each of n dimensions */
    int[]           nBins;
    /** Whether each dimension should be wrapped around */
    boolean[]       doWrap;
    /** Sum of weights of real points that have been tallied (usually == number of points) */
    double          realcount;
    /** The actual storage space */
    double[]        lookupTable;
    /** Width of each bin. Used often, nice to precalculate. */
    double[]        wBin;

    /**
    * Common variables used by tallyGaussian(), tallyCosine(), and their helper functions.
    * 'tgf' stood for Tally Gaussian Fast
    */
    double      tgf_a, tgf_b, tgf_b_2, tgf_mask_2;
    int[]       tgf_start, tgf_end, tgf_curr;       // must be created with new
    double[]    tgf_pt;                             // only a reference to an existing point
    double[]    tgf_bin_ctr;                        // must be created with new

    /** Reusable variables for valueAt() */
    int[]       va_home;        // the bin this point falls into
    double[]    va_home_ctr;    // center of the above bin (nearest)
    int[]       va_neighbor;    // 'diagonal' neighbor (most distant)
    int[]       va_current;     // bin being examined - some mix of elements from home[] and neighbor[]
    double[]    va_contrib;     // fractional contribution of neighbor[] to value at pt[]
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new <i>n</i>-dimensional lookup table of floating point numbers.
    *
    * @param name    the name of this table, for later identification
    * @param ndim    <i>n</i>, the number of dimensions
    * @param min     the minimum ranges of each dimension
    * @param max     the maximum ranges of each dimension
    * @param bins    the number of bins in each dimension
    * @param wrap    whether or not to wrap in each dimension
    */
    public NDimTable(String name, int ndim, double[] min, double[] max, int[] bins, boolean[] wrap)
    {
        ourName = name;
        nDim = ndim;

        // Make copies of all input data
        minVal  = new double[nDim];     System.arraycopy(min, 0, minVal, 0, nDim);
        maxVal  = new double[nDim];     System.arraycopy(max, 0, maxVal, 0, nDim);
        nBins   = new int[nDim];        System.arraycopy(bins, 0, nBins, 0, nDim);
        doWrap  = new boolean[nDim];    System.arraycopy(wrap, 0, doWrap, 0, nDim);

        // Calculate the bin widths.
        wBin = new double[nDim];
        for(int i = 0; i < nDim; i++) wBin[i] = (maxVal[i] - minVal[i]) / nBins[i];

        // Allocate storage for the tgfXXX variables
        tgf_start   = new int[nDim];
        tgf_end     = new int[nDim];
        tgf_curr    = new int[nDim];
        tgf_bin_ctr = new double[nDim];
        
        // Allocate storage for the vaXXX variables
        va_home     = new int[nDim];
        va_home_ctr = new double[nDim];
        va_neighbor = new int[nDim];
        va_current  = new int[nDim];
        va_contrib  = new double[nDim];
    }
    
    /**
    * Creates a NDimTable without initializing any of its fields.
    * For use by functions that reconstruct a table from some stored form.
    */
    protected NDimTable()
    {}
//}}}

//{{{ wrapbin, bin2long, bin2long_limit
//##################################################################################################
    // A utility function for subclasses.
    // In Java, (-a) % b == -(a % b), unlike a real modulo function.
    // Wraps an imaginary bin number on a given dimension.
    int wrapbin(int bin, int dim)
    {
        // BUG!! This produces [1,nBins] for negative numbers and [0,nBins-1] for positive numbers.
        //  if(doWrap[dim]) return ( bin < 0 ? nBins[dim] + (bin % nBins[dim]) : bin % nBins[dim] );
        // Fixed 11 Jan 2006; results before this may be (very slightly) wrong.
        
        if(doWrap[dim])
        {
            bin = bin % nBins[dim];
            if(bin < 0) return bin + nBins[dim];
            else return bin;
        }
        else return bin;
    }

    // Takes a set of bin numbers and produces a linear offset into an full-size table.
    // *** THIS MAY NOT BE A REAL, USABLE OFFSET INTO lookupTable ***
    // If no bin can be found after wrapping is applied, returns -1.
    long bin2long(int[] where)
    {
        long which = 0;
        int bin, i;
        for(i = 0; i < nDim-1; i++)
        {
            bin = wrapbin(where[i], i);
            if(bin < 0 || bin >= nBins[i]) return -1;
            which += bin;
            which *= nBins[i+1];
        }
        bin = wrapbin(where[i], i);
        if(bin < 0 || bin >= nBins[i]) return -1;
        which += bin;
        return which;
    }

    // Takes a set of bin numbers and produces a linear offset into an full-size table.
    // *** THIS MAY NOT BE A REAL, USABLE OFFSET INTO lookupTable ***
    // If no bin can be found after wrapping is applied, the edge of the table is used.
    long bin2long_limit(int[] where)
    {
        long which = 0;
        int bin, i;
        for(i = 0; i < nDim-1; i++)
        {
            bin = Math.min(nBins[i]-1, Math.max(wrapbin(where[i], i), 0));
            which += bin;
            which *= nBins[i+1];
        }
        bin = Math.min(nBins[i]-1, Math.max(wrapbin(where[i], i), 0));
        which += bin;
        return which;
    }
//}}}

//{{{ [abstract] index2bin, bin2index, set/addValueAt, valueAt, valueAtLimit
//##################################################################################################
    /**
    * Takes an index into lookupTable and regenerates a set of bin numbers.
    * @param where      the first nDim slots are overwritten with bin numbers.
    */
    abstract void index2bin(int idx, int[] where);
    
    /**
    * Takes a set of bin indices and converts it into an offset into lookupTable,
    * or returns -1 if no such bin exists in the table.
    * In general, this bin is <b>NOT SAFE FOR WRITING INTO</b>
    * unless it already has a non-zero value.
    */
    abstract int bin2index(int[] where);
    
    /**
    * Sets the number of data points in a given bin.
    * You (probably) shouldn't ever be using this.
    *
    * @param where the bin to affect
    * @param value the value to place in the bin
    */
    abstract public void setValueAt(int[] where, double value);

    /**
    * Adds to the number of data points in a given bin.
    * You (probably) shouldn't ever be using this.
    *
    * @param where the bin to affect
    * @param value the value to add to the bin
    */
    abstract public void addValueAt(int[] where, double value);

    /**
    * Retrieves the number of data points in a given bin.
    * If no bin can be found after wrapping is applied, an exception is thrown.
    *
    * @param where the bin to look in
    * @return the number of data points in the bin
    * @throws IndexOutOfBoundsException if no such bin exists in the table
    */
    abstract public double valueAt(int[] where);

    /**
    * Retrieves the number of data points in a given bin.
    * If no bin can be found after wrapping is applied, the edge of the table is used.
    *
    * @param where the bin to look in
    * @return the number of data points in the bin
    */
    abstract public double valueAtLimit(int[] where);
//}}}

//{{{ interpolated valueAt
//##################################################################################################
    /**
    * Estimates the value of the density trace at a given position, using linear interpolation.
    *
    * This algorithm is too complex to describe fully, but it basically consults the 2<sup>n</sup> bins
    * nearest in space to the input point, and weights their contributions according to their distances.
    *
    * <p>To get a feeling for how it works, work out a linear interpolation in one dimension
    * ( a...x.......b -- what is x in terms of a and b? )(trivial)
    * and then in two dimensions (still fairly easy -- do one dimension first, then interpolate between those results).
    *
    * <p>In dimensions very near the edge of the table, no interpolation is performed in that dimension
    * (it would be impossible to do so, anyway, because there's no 2nd value to interpolate out <i>to</i>).
    *
    * @param pt the point at which the value should be estimated
    * @return the approximate value of the density trace at the specified point
    */
    public double valueAt(double[] pt)
    {
        double value = 0.0; // The value this function is calculating!

        int dim, bin; // loop counters for dimensions and bins
        int bin_stop = 1 << nDim; // max val of the bin counter

        whereIs(pt, va_home);           // the bin this point falls into
        centerOf(va_home, va_home_ctr); // center of the above bin (nearest)
        double coeff;                   // product of a mix of elements from va_contrib[] and (1-va_contrib[])

        // Initialize va_neighbor[] and va_contrib[]
        for(dim = 0; dim < nDim; dim++)
        {
            // getValueAtLimit() makes sure we stay inside the table limits when finding va_neighbors.
            // If we would step outside the table, use the va_home bin instead -- thus there is effectively
            // no interpolation in the specified dimension.
            if(pt[dim] < va_home_ctr[dim]) {
                va_neighbor[dim] = va_home[dim]-1;
            } else {
                va_neighbor[dim] = va_home[dim]+1;
            }
            va_contrib[dim] = Math.abs( (pt[dim]-va_home_ctr[dim])/wBin[dim] ); // always on [0.0, 0.5]
        }

        // Loop over all bins
        // bin is used as a bit mask, with one bit per dimension
        // 0 means va_home, 1 means va_neighbor -- this way all 2^n va_neighbor bins are evaluated
        // The limit is a 30-D table, but a 2x2x...x2 table in 30-D would occupy > 4GB !!!
        for(bin = 0; bin < bin_stop; bin++)
        {
            coeff = 1.0; // reset coefficient

            // Loop over all dimensions, checking the appropriate bit in bin
            for(dim = 0; dim < nDim; dim++)
            {
                // Bit is off -- elements are drawn from va_home[] and (1-va_contrib[])
                if((bin & (1 << dim)) == 0)
                {
                    va_current[dim] = va_home[dim];
                    coeff *= 1.0 - va_contrib[dim];
                }
                // Bit is on -- elements are drawn from va_neighbor[] and va_contrib[]
                else
                {
                    va_current[dim] = va_neighbor[dim];
                    coeff *= va_contrib[dim];
                }
            }

            value += coeff * valueAtLimit(va_current); // calc. va_contribution of va_currently selected bin
        }

        return value;
    }
//}}}

//{{{ maxValue, totalCount, realCount
//##################################################################################################
    /**
    * Retrieves the greatest number of points found in any one bin.
    *
    * @return the maximum value in the table.
    */
    public double maxValue()
    {
        double max = lookupTable[0];
        for(int i = 1; i < lookupTable.length; i++)
        {
            if(lookupTable[i] > max) max = lookupTable[i];
        }
        return max;
    }

    /**
    * Retrieves the number of points in all bins.
    * @deprecated Use totalCount instead.
    */
    public double dumbTotalCount()
    {
        double count = 0.0;
        for(int i = 0; i < lookupTable.length; i++) count += lookupTable[i];
        return count;
    }
    
    /** Retrieves the number of points in all bins. */
    public double totalCount()
    {
        return recursiveCount(0, lookupTable.length-1);
    }
    
    /**
    * Does a recursive sum from start to end (inclusive).
    * This hopefully avoids the loss of accuracy that comes with
    * adding a small number to a big number.
    */
    private double recursiveCount(int start, int end)
    {
        if(start == end)
        {
            return lookupTable[start];
        }
        else
        {
            int middle = (start+end) / 2;
            return recursiveCount(start, middle) + recursiveCount(middle+1, end);
        }
    }

    /**
    * Returns the sum of weights of points tallied by this table,
    * which is usually the number of points tallyied since the last zero().
    */
    public double realCount()
    {
        return realcount;
    }
//}}}

//{{{ contains
//##################################################################################################
    /**
    * Checks if a given <i>n</i>-D point falls into the range covered by this table or can be wrapped to that range.
    *
    * @param pt the point to check
    * @return <code>true</code> if the points falls into the range, <code>false</code> otherwise
    */
    public boolean contains(double[] pt)
    {
        for(int i = 0; i < nDim; i++)
            { if( !doWrap[i] && ( pt[i] < minVal[i] || pt[i] > maxVal[i] )) return false; }
        return true;
    }

    /**
    * Checks if a given <i>n</i>-D bin exists in this table or can be wrapped to a bin that exists in this table.
    *
    * @param bin the bin to check
    * @return <code>true</code> if the bin exists, <code>false</code> otherwise
    */
    public boolean contains(int[] bin)
    {
        for(int i = 0; i < nDim; i++)
            { if( !doWrap[i] && ( bin[i] < 0 || bin[i] >= nBins[i] )) return false; }
        return true;
    }
//}}}

//{{{ whereIs, centerOf
//##################################################################################################
    /**
    * Returns the bin indices for a given point.
    * A point outside the range will return an imaginary bin outside the limits of the table.
    *
    * @param pt the point to find
    * @return the bin indices
    */
    public int[] whereIs(double[] pt)
    {
        int[] where = new int[nDim];
        for(int i = 0; i < nDim; i++)
        {
            // min() is necessary for the case where pt[i] == max[i]
            where[i] = Math.min( (int)Math.floor((pt[i]-minVal[i])/wBin[i]),
                                 nBins[i] - 1);
        }
        return where;
    }

    public void whereIs(double[] pt, int[] where)
    {
        for(int i = 0; i < nDim; i++)
        {
            // min() is necessary for the case where pt[i] == max[i]
            where[i] = Math.min( (int)Math.floor((pt[i]-minVal[i])/wBin[i]),
                                 nBins[i] - 1);
        }
    }

    /**
    * Returns a point that represents the "center" of a specified bin.
    * Imaginary bins outside the table are OK; their centers will fall outside the bounds of the table, though.
    *
    * @param where the bin indices
    * @return the center of that bin
    */
    public double[] centerOf(int[] where)
    {
        double[] pt = new double[nDim];
        for(int i = 0; i < nDim; i++)
        {
            pt[i] = minVal[i] + wBin[i]*(where[i]+0.5);
        }
        return pt;
    }

    public void centerOf(int[] where, double[] pt)
    {
        for(int i = 0; i < nDim; i++)
        {
            pt[i] = minVal[i] + wBin[i]*(where[i]+0.5);
        }
    }
//}}}

//{{{ distanceSquared, tallySimple
//##################################################################################################
    /**
    * Returns the distance between two points, squared.
    *
    * @param pt1 an array of coordinates representing a point in n-space.
    * @param pt2 ditto
    *
    * @return the sum of the squares of the differences of as many values as are present in both points.
    */
    public static double distanceSquared(double[] pt1, double[] pt2)
    {
        int dim = Math.min(pt1.length, pt2.length);
        double diff, dist_2 = 0.0;
        for(int i = 0; i < dim; i++)
        {
            diff    = pt1[i] - pt2[i];
            dist_2 += diff*diff;
        }
        return dist_2;
    }

    /**
    * Increments the counter for the bin that this point falls into.
    * Used for making histograms.
    *
    * @param pt     the point to tally
    * @param weight the relative weight of this data point. Defaults to 1.0.
    */
    public void tallySimple(double[] pt, double weight)
    {
        realcount += weight;
        whereIs(pt, va_current);
        //addValueAt(va_current, 1); the old code added 1.0 regardless of the weight...
        addValueAt(va_current, weight);
    }
    
    public void tallySimple(double[] pt)
    { tallySimple(pt, 1.0); }
//}}}

//{{{ tallyGaussian
//##################################################################################################
    /**
    * Calculates the contribution of this point to neighboring bins using a Gaussian mask.
    * The function is scaled such that regardless of the mask radius, it encloses a total
    * area/volume/etc. of exactly one. Probably. Unless my math's off. Which it might be.
    *
    * <p>The formula: {1/[b*sqrt(pi)]^n}*e^[ -(x/b)^2 ]
    *
    * @param pt the point to tally
    * @param halfwidth the mask half width at half height, <i>b*sqrt(-ln(0.5))</i>
    * @param weight the relative weight of this data point. Defaults to 1.0.
    * @param reach the maximum number of half-widths at which to evaluate the function.
    *   Defaults to 4.5, which means only values less than 1e-6 of the max are discarded.
    */
    public void tallyGaussian(double[] pt, double halfwidth, double weight, double reach)
    {
        realcount += weight;
        tgf_pt = pt; // uses the tgfXXX public variables!

        // determine the coefficients
        tgf_b   = halfwidth / Math.sqrt( -Math.log(0.5) );
        tgf_b_2 = tgf_b*tgf_b;
        tgf_a   = weight / Math.pow(tgf_b * Math.sqrt(Math.PI), nDim);

        // set up the limits of exploration
        double range    = reach * halfwidth;
        tgf_mask_2      = range * range;
        for(int i = 0; i < nDim; i++)
        {
            // round() is the best choice, b/c the half-bin marks correspond to the point
            // where the function will be evaluated anyway...
            // Bin numbers may be negative, or too large. It will be taken care of later.
            tgf_start[i] = (int)Math.round((pt[i]-minVal[i]-range)/wBin[i]);
            tgf_end[i]   = (int)Math.round((pt[i]-minVal[i]+range)/wBin[i]);
            tgf_curr[i]  = tgf_start[i];
        }

        gsnRecursiveLoop(0);
    }
    
    public void tallyGaussian(double[] pt, double halfwidth, double weight)
    { tallyGaussian(pt, halfwidth, weight, 4.5); }
    public void tallyGaussian(double[] pt, double halfwidth)
    { tallyGaussian(pt, halfwidth, 1.0); }
//}}}

//{{{ gsnRecursiveLoop
//##################################################################################################
    // Use recursion to loop over an unknown number of dimensions
    void gsnRecursiveLoop(int depth)
    {
        // Set up a loop and go down to the next level
        if(depth < nDim)
        {
            for(tgf_curr[depth] = tgf_start[depth]; tgf_curr[depth] <= tgf_end[depth]; tgf_curr[depth]++)
            {
                gsnRecursiveLoop(depth+1);
            }
        }
        // We're at the bottom -- calculate the contribution to our current position
        else
        {
            // Make sure this is a real bin before we get going...
            if(contains(tgf_curr))
            {
                // It's a real bin, or at least it wraps to a real bin.
                // Calculate a center for it.
                centerOf(tgf_curr, tgf_bin_ctr);

                // Doing this means we cut off the mask uniformly
                // at the specified reach
                double dist_2 = distanceSquared(tgf_bin_ctr, tgf_pt);
                if(dist_2 < tgf_mask_2)
                {
                    double gaussian = tgf_a * Math.exp( -dist_2 / tgf_b_2 );
                    addValueAt(tgf_curr, gaussian);
                }
            }
        }
    }
//}}}

//{{{ tallyCosine
//##################################################################################################
    /**
    * Calculates the contribution of this point to neighboring bins using a cosine mask.
    * This mask is very similar to the Gaussian but does actually reach zero at a finite distance.
    * The function is scaled such that regardless of the mask radius, it encloses a constant
    * total area/volume/etc.
    *
    * <p>The formula: (1/a^n)*f(n)*{ [cos(pi*x/a)+1] / 2 }
    * <br>where f(n) is an unknown function that corrects the constant (but arbitrary) total area/volume/etc.
    * to be exactly 1.0, and is dependent only on the number of dimensions the table covers.
    *
    * <p<table border=1>
    * <tr><td><b>n</b></td><td><b>f(n)</b></td></tr>
    * <tr><td>1</td><td>1 / 2 = 0.5</td></tr>
    * <tr><td>2</td><td>1 / (pi - 4/pi) = 0.535230730883</td></tr>
    * <tr><td>3</td><td>1 / [ 4*pi/3 - 8/(pi^2) ] = 0.29601381267</td></tr>
    * <tr><td>4</td><td>???</td></tr>
    * </table>
    *
    * @param pt the point to tally
    * @param halfwidth the mask half width at half height, <i>a/2</i>
    * @param weight the relative weight of this data point. Defaults to 1.0.
    */
    public void tallyCosine(double[] pt, double halfwidth, double weight)
    {
        realcount += weight;
        tgf_pt = pt; // hijacks the tgfXXX public variables!

        // determine the coefficients
        double mask = 2.0*halfwidth;
        tgf_b       = mask / Math.PI; // we want to sample from -pi to +pi
        tgf_a       = weight / Math.pow(mask, nDim);
        tgf_mask_2  = mask * mask;

        // set up the limits of exploration
        for(int i = 0; i < nDim; i++)
        {
            // round() is the best choice, b/c the half-bin marks correspond to the point
            // where the function will be evaluated anyway...
            // Bin numbers may be negative, or too large. It will be taken care of later.
            tgf_start[i] = (int)Math.round((pt[i]-minVal[i]-mask)/wBin[i]);
            tgf_end[i]   = (int)Math.round((pt[i]-minVal[i]+mask)/wBin[i]);
            tgf_curr[i]  = tgf_start[i];
        }

        cosRecursiveLoop(0);
    }

    public void tallyCosine(double[] pt, double halfwidth)
    { tallyCosine(pt, halfwidth, 1.0); }
//}}}

//{{{ cosRecursiveLoop
//##################################################################################################
    // Use recursion to loop over an unknown number of dimensions
    void cosRecursiveLoop(int depth)
    {
        // Set up a loop and go down to the next level
        if(depth < nDim)
        {
            for(tgf_curr[depth] = tgf_start[depth]; tgf_curr[depth] <= tgf_end[depth]; tgf_curr[depth]++)
            {
                cosRecursiveLoop(depth+1);
            }
        }
        // We're at the bottom -- calculate the contribution to our current position
        else
        {
            // Make sure this is a real bin before we get going...
            if(contains(tgf_curr))
            {
                // It's a real bin, or at least it wraps to a real bin.
                // Calculate a center for it.
                //double[] bin_center = centerOf(tgf_curr);
                centerOf(tgf_curr, tgf_bin_ctr);

                // Doing this prevents us from inadvertently
                // including negative values in our sum.
                //double dist = distanceSquared(bin_center, tgf_pt);
                double dist = distanceSquared(tgf_bin_ctr, tgf_pt);
                if(dist < tgf_mask_2)
                {
                    dist = Math.sqrt(dist);
                    // Function varies from 0 to 2, as in published Rama/C-beta paper
                    double cosine = tgf_a * (Math.cos(dist/tgf_b) + 1.0);
                    addValueAt(tgf_curr, cosine);
                }
            }
        }
    }
//}}}

//{{{ classifyByHills, recurseHills, hillsRecurseMaxNeighbor, assignDataToHills
//##############################################################################
    int[] hillTable = null; // holds the labels, since we can't climb in place!
    HashMap<Integer,double[]> hillModes = null; // peak ID -> peak coords
    HashMap<Integer,ArrayList<DataSample>> hillAssign = null;  // peak ID -> list of data samples
    /**
    * Traverses the table, transforming each positive (non-zero) value into
    * a negative integer label, such that each point is labeled according to
    * which hill it belongs to.
    * <p>In an N-dimensional space, each of the 3^N - 1 neighbor points
    * (ie, diagonal neighbors are included) is queried, and the largest
    * positive value is followed recursively, until the top is reached.
    * @param data - same as SilkOptions.data: raw, discrete, unfiltered input.
    */
    public void classifyByHills(boolean findHillModes, Collection data)
    {
        tgf_b = 1.0; // used for next unused label
        tgf_pt = new double[nDim]; // could be null, or someone else's var!
        
        hillTable = new int[lookupTable.length];
        double[] origTable = new double[lookupTable.length];
        if(findHillModes)
            hillModes = new HashMap<Integer,double[]>();
        
        for(int i = 0; i < lookupTable.length; i++)
            recurseHills(i);
        
        for(int i = 0; i < lookupTable.length; i++)
            origTable[i] = lookupTable[i]; // [store density values]
        for(int i = 0; i < lookupTable.length; i++)
            lookupTable[i] = hillTable[i]; // lookup = hill IDs
        
        if(data != null)
            assignDataToHills(data);
        
        for(int i = 0; i < lookupTable.length; i++)
            lookupTable[i] = origTable[i]; // lookup = density values
        // Basis for ^, changed by DAK 6 July 2009:
        // "Make climbHills return a List of peaks
        //    This is a bit hard, because you probably want the peak heights too.
        //    Which are gone when you overwrite them with peak labels.
        //    Maybe classifyByHills() shouldn't alter the table after all?" - IWD
        
        
        origTable = null;
        hillTable = null;
    }
    
    private double recurseHills(int idx)
    {
        double val = lookupTable[idx], label = hillTable[idx];
        if(val == 0)    return 0;       // should only happen directly from classifyByHills()
        if(label != 0)  return label;   // already converted to a label
        
        // Does a neighbor have a higher value than this cell?
        // yes: call recurseHills() on that cell, and use that label
        // no: give this cell a new label, and return it
        //
        // This is recursive with shared (instance) variables, BUT
        // we get all the goody out of them before this function itself recurses,
        // so it *should* all be OK...
        index2bin(idx, va_current);     // best bin so far: this one
        tgf_a = val;                    // best value so far: this one
        System.arraycopy(va_current, 0, tgf_curr, 0, nDim); // find current bin, then go one left and one right:
        for(int i = 0; i < nDim; i++)
        {
            tgf_start[i] = tgf_curr[i]-1;   // min bound for loop
            tgf_end[i]   = tgf_curr[i]+1;   // max bound for loop
            tgf_curr[i]  = tgf_start[i];    // curr loop indices: start at start
        }
        hillsRecurseMaxNeighbor(0);
        
        int nextIdx = bin2index(va_current);
        if(nextIdx == idx) // we're the highest value: we've reached the hill top
        {
            label = tgf_b++; // next label is more positive than this one: +1, +2, +3, ...
            centerOf(va_current, tgf_pt);
            if(hillModes != null)
            {
                double[] hillMode = new double[nDim+1];
                for(int i = 0; i < nDim; i++)
                    hillMode[i] = tgf_pt[i];
                hillMode[nDim] = lookupTable[idx];
                hillModes.put((int)label, hillMode);
            }
        }
        else // further up and further in: try the next highest point
            label = recurseHills(nextIdx);
        hillTable[idx] = (int) label;
        return label;
    }
    
    // Use recursion to loop over an unknown number of dimensions.
    // Uses tgf_start/end/curr for looping; va_current and tgf_a for best found so far.
    private void hillsRecurseMaxNeighbor(int depth)
    {
        // Set up a loop and go down to the next level
        if(depth < nDim)
        {
            for(tgf_curr[depth] = tgf_start[depth]; tgf_curr[depth] <= tgf_end[depth]; tgf_curr[depth]++)
                hillsRecurseMaxNeighbor(depth+1);
        }
        // We're at the bottom -- is this cell better than the current best?
        else
        {
            // Make sure this is a real bin before we get going...
            if(contains(tgf_curr))
            {
                // It's a real bin, or at least it wraps to a real bin.
                double val = valueAt(tgf_curr);
                // Have we done better? Using >= means tied neighbors will drift toward higher indices...
                if(val >= tgf_a)
                {
                    tgf_a = val;
                    System.arraycopy(tgf_curr, 0, va_current, 0, nDim);
                }
            }
        }
    }

    // Assigns each data sample (from raw input) to nearest hill based on hills grid.
    // For each hill, makes list of data samples mapped to it.
    private void assignDataToHills(Collection data)
    {
        hillAssign = new HashMap<Integer,ArrayList<DataSample>>();
        for(Iterator dItr = data.iterator(); dItr.hasNext(); )
        {
            DataSample dataSample = (DataSample) dItr.next();
            
            // Hill nearest to this data sample
            /*// Old method: interpolate hill IDs of surrounding grid points, 
            // then round off to get hill ID assignment.
            // Doesn't work so well for samples near boundaries!
            int hillId = (int) Math.round(valueAt(dataSample.coords));*/
            // New method: find nearest(?) grid point with whereIs(), 
            // then get hill ID of that point.  Much better!
            int hillId = (int) Math.round(valueAt(whereIs(dataSample.coords)));
            
            // Store
            ArrayList<DataSample> hillData = (hillAssign.containsKey(hillId) ? 
                hillAssign.get(hillId) : new ArrayList<DataSample>());
            hillData.add(dataSample);
            hillAssign.put(hillId, hillData);
        }
        
    }
//}}}

//{{{ zero, squash, scale, normalize, standardize
//##################################################################################################
    /**
    * Zeros out every bin. Good for starting over ;)
    */
    public void zero()
    {
        realcount = 0;
        for(int i = 0; i < lookupTable.length; i++) lookupTable[i] = 0.0;
    }
    
    /** Sets bins to zero that have values STRICTLY LESS THAN the cutoff. */
    public void squash(double cutoff)
    {
        for(int i = 0; i < lookupTable.length; i++)
            if(lookupTable[i] < cutoff) lookupTable[i] = 0.0;
    }

    /**
    * Scales every bin. Useful for correcting the effects of tallyXXX().
    *
    * @param f the scaling factor to multiply by
    */
    public void scale(double f)
    {
        for(int i = 0; i < lookupTable.length; i++) lookupTable[i] *= f;
    }

    /**
    * "Normalizes" the table such that the sum of all bins is equal to the number of points tallied
    * divided by the product of the bin width in each dimension.
    * In other words, it ensures each tallied point contributed (approximately) 1 unit of area/volume/etc to the trace.
    * This is never a problem with tallySimple(), but is inherent in tallyGaussian() and tallyCosine().
    */
    public void normalize()
    {
        double f = 1;
        for(int i = 0; i < nDim; i++) f *= wBin[i];
        scale( realCount() / f / totalCount() );
    }
    
    /**
    * Scales all the values of the table such that the greatest value in the table is equal to some specified value (e.g., 1.0).
    *
    * @param max the top end of the scale the table should lie on.
    */
    public void standardize(double max)
    {
        scale( max / maxValue() );
    }
//}}}

//{{{ addPrior
//##################################################################################################
    /**
    * For each bin in this table, finds the count in the equivalent place in another table
    * and adds some fraction of thatCount.
    * Used for blending distributions in Bayesian statistics.
    * As it is effectively a tallying, it updates realcount as well.
    *
    * <p><b>Right now, this works for dense tables only, not sparse!</b>
    *
    * @param wThat      1.0 means equal "influence" for the prior and this,
    *   regardless of actual counts (same effective sample size).
    *   Larger values emphasize the prior more; smaller values, less.
    */
    public void addPrior(double wThat, NDimTable that)
    {
        wThat *= this.totalCount() / that.totalCount();
        int[] bin = new int[nDim];
        double[] pt = new double[nDim];
        for(int i = 0; i < lookupTable.length; i++)
        {
            index2bin(i, bin);
            centerOf(bin, pt);
            lookupTable[i] += wThat*that.valueAt(pt);
        }
        this.realcount *= (1.0 + wThat);
    }
//}}}

//{{{ transformLog, transformTrueNaturalLog, fractionLessThan
//##################################################################################################
    /**
    * Transforms every value <i>v</i> in the table on a log scale.
    * The mapping <i>v</i> --&gt; <i>v'</i> is as follows:
    * <p>v' = ln(v+1)
    * <p>The 'plus one' keeps values of zero equal to zero.
    */
    public void transformLog()
    {
        for(int i = 0; i < lookupTable.length; i++) lookupTable[i] = Math.log(lookupTable[i] + 1.0);
    }
    
    /**
    * Transforms every value <i>v</i> in the table on a log scale.
    * The mapping <i>v</i> --&gt; <i>v'</i> is as follows:
    * <p>v' = ln(v)
    * <p>Note that if <i>v</i> is zero, <i>v'</i> will be negative infinity!
    */
    public void transformTrueNaturalLog()
    {
        for(int i = 0; i < lookupTable.length; i++) lookupTable[i] = Math.log(lookupTable[i]);
    }
    
    /**
    * Transforms the table so each (new) value reflects the fraction of entries in
    * samples that is less than the (old) value stored in the table.
    * Afterward all table values will range between 1.0 (higher density than any entry in samples)
    * and 0.0 (lower density than any entry in samples).
    * @param samples an array of density values to use for comparison. Will be sorted by this function.
    */
    public void fractionLessThan(double[] samples)
    {
        double n, size = samples.length;
        Arrays.sort(samples); // so we can do binary searches
        
        for(int i = 0; i < lookupTable.length; i++)
        {
            n = Arrays.binarySearch(samples, lookupTable[i]);
            // weird optimization was producing negative zeros in output
            //if(n < 0) n = -(n+1); // calculate insertion point
            if(n < 0) n = Math.abs(n+1);
            lookupTable[i] = (n / size);
        }
    }
//}}}

//{{{ writeText
//##################################################################################################
    /** Writes out data with full precision and values as last field on line. */
    public void writeText(OutputStream out)
    { writeText(out, null, false); }

    /**
    * Writes out a human-readable version of the data in this table.
    * Format is self-documenting; lines begining with a hash (#) are comments
    * @param out            the stream to write to
    * @param df             the formatting object to use in formatting output, or null for none.
    * @param valuesFirst    whether the value should come before or after the coords
    */
    public void writeText(OutputStream out, DecimalFormat df, boolean valuesFirst)
    {
        int         i, j;
        int[]       binIndices  = new int[nDim];
        double[]    binCoords   = new double[nDim];
        PrintStream ps          = new PrintStream(out);

        ps.println("# Table name/description: \""+ourName+"\"");
        ps.println("# Number of dimensions: "+nDim);
        ps.println("# For each dimension, 1 to "+nDim+": lower_bound  upper_bound  number_of_bins  wrapping");
        for(i = 0; i < nDim; i++)
        { ps.println("#   x"+(i+1)+": "+minVal[i]+" "+maxVal[i]+" "+nBins[i]+" "+doWrap[i]); }
        
        if(valuesFirst) ps.println("# List of table coordinates and values. (Value is first number on each line.)");
        else            ps.println("# List of table coordinates and values. (Value is last number on each line.)");
        if(df == null)
        {
            for(i = 0; i < lookupTable.length; i++)
            {
                if(lookupTable[i] == 0) continue; // only print non-zeros; zeros may be undef bins
                index2bin(i, binIndices);
                centerOf(binIndices, binCoords);
                if(valuesFirst)
                {
                    ps.print(lookupTable[i]);
                    for(j = 0; j < nDim; j++) { ps.print(" "); ps.print(binCoords[j]); }
                    ps.println();
                }
                else
                {
                    for(j = 0; j < nDim; j++) { ps.print(binCoords[j]); ps.print(" "); }
                    ps.println(lookupTable[i]);
                }
            }
        }
        else
        {
            for(i = 0; i < lookupTable.length; i++)
            {
                if(lookupTable[i] == 0) continue; // only print non-zeros; zeros may be undef bins
                index2bin(i, binIndices);
                centerOf(binIndices, binCoords);
                if(valuesFirst)
                {
                    ps.print(df.format(lookupTable[i]));
                    for(j = 0; j < nDim; j++) { ps.print(" "); ps.print(df.format(binCoords[j])); }
                    ps.println();
                }
                else
                {
                    for(j = 0; j < nDim; j++) { ps.print(df.format(binCoords[j])); ps.print(" "); }
                    ps.println(df.format(lookupTable[i]));
                }
            }
        }
        ps.flush();
    }
//}}}

//{{{ get/set functions
//##################################################################################################
    /**
    * Gets the identifier string associated with this table.
    * @return the name of the table
    */
    public String getName() { return ourName; }

    /**
    * Sets the identifier string associated with this table.
    * param name the name of the table
    */
    public void setName(String name) { ourName = name; }

    /**
    * Gets the number of dimensions for this table.
    * @return number of dimensions in the table
    */
    public int getDimensions() { return nDim; }

    /**
    * Gets the minimum bounds for this table.
    * @return minimum bounds for this table
    */
    public double[] getMinBounds()
    {
        double[] retval = new double[nDim];
        System.arraycopy(minVal, 0, retval, 0, nDim);
        return retval;
    }

    /**
    * Gets the maximum bounds for this table.
    * @return maximum bounds for this table
    */
    public double[] getMaxBounds()
    {
        double[] retval = new double[nDim];
        System.arraycopy(maxVal, 0, retval, 0, nDim);
        return retval;
    }
    
    /**
    * This is NOT for normal use, but if you're clever you can use it to
    * simulate having anisotropic (ellipsoidal) kernels for the cosine or
    * Gaussian smoothing functions. (It has no effect for histograms.)
    * Because the effect is to change how a given bin is translated to
    * data-space coordinates, you can start with half-size bounds in one
    * dimension, process the data with that dimension halved, and then expand
    * the bounds back to normal.  End result: twice as much smoothing in that
    * dimension.
    */
    protected void resetBounds(double[] min, double[] max)
    {
        System.arraycopy(min, 0, minVal, 0, nDim);
        System.arraycopy(max, 0, maxVal, 0, nDim);
        for(int i = 0; i < nDim; i++) wBin[i] = (maxVal[i] - minVal[i]) / nBins[i];
    }

    /**
    * Gets the number of bins for this table.
    * @return number of bins for this table
    */
    public int[] getBins()
    {
        int[] retval = new int[nDim];
        System.arraycopy(nBins, 0, retval, 0, nDim);
        return retval;
    }

    /**
    * Gets the wrapping status for this table.
    * @return number of bins for this table
    */
    public boolean[] getWrap()
    {
        boolean[] retval = new boolean[nDim];
        System.arraycopy(doWrap, 0, retval, 0, nDim);
        return retval;
    }
//}}}
}//class
