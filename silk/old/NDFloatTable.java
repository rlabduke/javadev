// (jEdit options) :folding=explicit:collapseFolds=1:
package boundrotamers;
//import java.awt.*;
import java.io.*;
import java.util.*;
//import javax.swing.*;

/**
 * <code>NDFloatTable</code> implements an n-dimensional lookup table of floating-point values.
 * The table covers a "rectangular" range; i.e., each dimension <i>d</i> is confined to some interval [min<sub>d</sub>, max<sub>d</sub>].
 * Each dimension may optionally be "wrapped" such that min<sub>d</sub> and max<sub>d</sub> are two ways of referring to the same thing.
 * On each dimension the table is subdivided into nbins<sub>d</sub> "bins" or sample points.
 * Thus, it is each n-dimensional bin that has a single floating-point value.
 * There is no explicit "memory" of the input points; their identity merges with one (or more) bins.
 * The basic behavior is to simply add an input point to the bin it falls into, but various smoothing functions are available to distribute the point over multiple bins.
 * The class is intended for making histograms and (particularly) density traces.
 *
 * <p>Using the Gaussian and cosine functions for density traces has some hang-ups, particularly if one varies the mask radii.
 * See the README file associated with the <code>boundrotamers</code> project for all the details.
 *
 * <p>The various <i>n</i>-dimensional entities (points/vectors, bins, etc.) are represented as primitive arrays of length <i>n</i>.
 *
 * <p><b>Note that no checking of points or indices is performed!</b>
 * If you are not certain that a point or set of bin indices falls within the table, use <code>contains()</code> to check.
 * Submission of invalid values will result in unpredictable behavior and/or exceptions being thrown.
 *
 *<!-- {{{ GPL, start date, copyright  -->
<br><pre>
/----------------------------------------------------------------------\
| This program is free software; you can redistribute it and/or modify |
| it under the terms of the GNU General Public License as published by |
| the Free Software Foundation; either version 2 of the License, or    |
| (at your option) any later version.                                  |
|                                                                      |
| This program is distributed in the hope that it will be useful,      |
| but WITHOUT ANY WARRANTY; without even the implied warranty of       |
| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        |
| GNU General Public License for more details.                         |
\----------------------------------------------------------------------/
</pre>
 *
 * <p>Begun on Wed Mar 20 16:47:59 EST 2002
 * <br>Copyright (C) 2002 by Ian W. Davis
 * <br>Richardson laboratory, Duke University: <a href="http://kinemage.biochem.duke.edu">kinemage.biochem.duke.edu</a>
 *
<!-- }}} -->
 */
public class NDFloatTable //extends ... implements ...
{
//{{{ Static fields
    /**
    * The number of mask radii that tallyGaussian() explores out to.
    * Note that at one mask radius, the value has fallen to 10% of its maximum (by definition).
    * But at two mask radii, the value is only 0.01% of its maximum.
    * At two-and-a-half, it's less than a millionth; at three, less than a billionth.
    */
    static final float GSN_REACH = 2.5f;
//}}}

//{{{ Variables
//##################################################################################################
    // The name of this table, for later reference
    String ourName;
    // Number of dimensions the table exists in
    int nDim;
    // Minimum and maximum boundaries for each of n dimensions
    float[] minVal, maxVal;
    // Number of subdivisions on each of n dimensions
    int[] nBins;
    // Whether each dimension should be wrapped around
    boolean[] doWrap;
    // Number of real points that have been tallied
    int realcount;

    // The actual storage space
    float[] lookupTable;

    // Width of each bin. Used often, nice to precalculate.
    float[] wBin;

    // Common variables used by tallyGaussian(), tallyCosine(), and their helper functions.
    // 'tgf' stood for Tally Gaussian Fast
    float tgf_a, tgf_b, tgf_b_2, tgf_mask_2;
    int[] tgf_start, tgf_end, tgf_curr;  // must be created with new
    float[] tgf_pt;                      // only a reference to an existing point
//}}}

//{{{ Constructors
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
    public NDFloatTable(String name, int ndim, float[] min, float[] max, int[] bins, boolean[] wrap)
    {
        ourName = name;
        nDim = ndim;

        // Make copies of all input data
        minVal = new float[nDim];      System.arraycopy(min, 0, minVal, 0, nDim);
        maxVal = new float[nDim];      System.arraycopy(max, 0, maxVal, 0, nDim);
        nBins = new int[nDim];         System.arraycopy(bins, 0, nBins, 0, nDim);
        doWrap = new boolean[nDim];    System.arraycopy(wrap, 0, doWrap, 0, nDim);

        // Calculate the bin widths.
        wBin = new float[nDim];
        for(int i = 0; i < nDim; i++) wBin[i] = (maxVal[i] - minVal[i]) / nBins[i];

        // Allocate storage for the tgfXXX variables
        tgf_start = new int[nDim];
        tgf_end   = new int[nDim];
        tgf_curr  = new int[nDim];

        // Calculate number of entries in lookupTable
        int n_entries = 1;
        for(int i = 0; i < nDim; i++) n_entries *= nBins[i];

        // Allocate table and initialize it
        lookupTable = new float[n_entries];
        zero();
    }
//}}}

//{{{ bin <==> index functions, wrapping
//##################################################################################################
    // In Java, (-a) % b == -(a % b), unlike a real modulo function.
    // Wraps an imaginary bin number on a given dimension.
    int wrapbin(int bin, int dim)
    {
        if(doWrap[dim]) return ( bin < 0 ? nBins[dim] + bin % nBins[dim] : bin % nBins[dim] );
        else            return bin;
    }

    // Takes a set of bin numbers and produces a linear offset into lookuptable.
    // If no bin can be found after wrapping is applied, returns -1.
    int bin2index(int[] where)
    {
        int which = 0, bin, i;
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

    // Takes a set of bin numbers and produces a linear offset into lookuptable.
    // If no bin can be found after wrapping is applied, the edge of the table is used.
    int bin2index_limit(int[] where)
    {
        int which = 0, bin, i;
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

    // Takes an index into lookuptable and regenerates a set of bin numbers
    int[] index2bin(int which)
    {
        int[] where = new int[nDim];
        for(int i = nDim-1; i >= 0; i--)
        {
            where[i] = which % nBins[i];
            which -= where[i];
            which /= nBins[i];
        }
        return where;
    }
//}}}

//{{{ counting, get/set value
//##################################################################################################
    /**
    * Sets the number of data points in a given bin.
    * You (probably) shouldn't ever be using this.
    *
    * @param where the bin to affect
    * @param value the value to place in the bin
    */
    public void setValueAt(int[] where, float value)
    {
        lookupTable[bin2index(where)] = value;
    }

    /**
    * Retrieves the number of data points in a given bin.
    *
    * @param where the bin to look in
    * @return the number of data points in the bin
    */
    public float valueAt(int[] where)
    {
        return lookupTable[bin2index(where)];
    }

    /**
    * Retrieves the greatest number of points found in any one bin.
    *
    * @return the maximum value in the table.
    */
    public float maxValue()
    {
        float max = lookupTable[0];
        for(int i = 1; i < lookupTable.length; i++)
        {
            if(lookupTable[i] > max) max = lookupTable[i];
        }
        return max;
    }

    /**
    * Retrieves the number of points in all bins.
    *
    * @return the sum across all bins
    */
    public float totalCount()
    {
        float count = 0.0f;
        for(int i = 0; i < lookupTable.length; i++) count += lookupTable[i];
        return count;
    }

    /**
    * Returns the number of points tallied by this table.
    * @return number of times tallyXXX() has been called since the last zero()
    */
    public int realCount()
    {
        return realcount;
    }
//}}}

//{{{ interpolated valueAt()
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
    public float valueAt(float[] pt)
    {
        float value = 0.0f; // The value this function is calculating!

        int dim, bin; // loop counters for dimensions and bins
        int bin_stop = 1 << nDim; // max val of the bin counter

        int[] home = whereIs(pt);          // the bin this point falls into
        float[] home_ctr = centerOf(home); // center of the above bin (nearest)
        int[] neighbor  = new int[nDim];   // 'diagonal' neighbor (most distant)
        int[] current   = new int[nDim];   // bin being examined - some mix of elements from home[] and neighbor[]
        float[] contrib = new float[nDim]; // fractional contribution of neighbor[] to value at pt[]
        float coeff;                       // product of a mix of elements from contrib[] and (1-contrib[])

        // Initialize neighbor[] and contrib[]
        for(dim = 0; dim < nDim; dim++)
        {
            // Use min() and max() to make sure we stay inside the table limits when finding neighbors.
            // If we would step outside the table, use the home bin instead -- thus there is effectively
            // no interpolation in the specified dimension.
            //
            // Now this is done later, implicitly, by bin2index_limit()
            //
            if(pt[dim] < home_ctr[dim]) {
                neighbor[dim] = home[dim]-1;
            } else {
                neighbor[dim] = home[dim]+1;
            }
            contrib[dim] = (float)Math.abs( (pt[dim]-home_ctr[dim])/wBin[dim] ); // always on [0.0, 0.5]
        }

        // Loop over all bins
        // bin is used as a bit mask, with one bit per dimension
        // 0 means home, 1 means neighbor -- this way all 2^n neighbor bins are evaluated
        // The limit is a 30-D table, but a 2x2x...x2 table in 30-D would occupy > 4GB !!!
        for(bin = 0; bin < bin_stop; bin++)
        {
            coeff = 1.0f; // reset coefficient

            // Loop over all dimensions, checking the appropriate bit in bin
            for(dim = 0; dim < nDim; dim++)
            {
                // Bit is off -- elements are drawn from home[] and (1-contrib[])
                if((bin & (1 << dim)) == 0)
                {
                    current[dim] = home[dim];
                    coeff *= 1.0f - contrib[dim];
                }
                // Bit is on -- elements are drawn from neighbor[] and contrib[]
                else
                {
                    current[dim] = neighbor[dim];
                    coeff *= contrib[dim];
                }
            }

            value += coeff * lookupTable[bin2index_limit(current)]; // calc. contribution of currently selected bin
        }

        return value;
    }
//}}}

//{{{ contains()
//##################################################################################################
    /**
    * Checks if a given <i>n</i>-D point falls into the range covered by this table or can be wrapped to that range.
    *
    * @param pt the point to check
    * @return <code>true</code> if the points falls into the range, <code>false</code> otherwise
    */
    public boolean contains(float[] pt)
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

//{{{ whereIs(), centerOf()
//##################################################################################################
    /**
    * Returns the bin indices for a given point.
    * A point outside the range will return an imaginary bin outside the limits of the table.
    *
    * @param pt the point to find
    * @return the bin indices
    */
    public int[] whereIs(float[] pt)
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

    /**
    * Returns a point that represents the "center" of a specified bin.
    * Imaginary bins outside the table are OK; their centers will fall outside the bounds of the table, though.
    *
    * @param where the bin indices
    * @return the center of that bin
    */
    public float[] centerOf(int[] where)
    {
        float[] pt = new float[nDim];
        for(int i = 0; i < nDim; i++)
        {
            pt[i] = minVal[i] + wBin[i]*((float)where[i]+0.5f);
        }
        return pt;
    }
//}}}

//{{{ distance calculation, simple tally
//##################################################################################################
    /**
    * Returns the distance between two points, squared.
    *
    * @param pt1 an array of coordinates representing a point in n-space.
    * @param pt2 ditto
    *
    * @return the sum of the squares of the differences of as many values as are present in both points.
    */
    public static float distanceSquared(float[] pt1, float[] pt2)
    {
        int dim = Math.min(pt1.length, pt2.length);
        float diff, dist_2 = 0.0f;
        for(int i = 0; i < dim; i++)
        {
            diff    = pt1[i] - pt2[i];
            dist_2 += diff*diff;
        }
        return dist_2;
    }

//##################################################################################################
    /**
    * Increments the counter for the bin that this point falls into.
    *
    * @param pt The point to tally
    */
    public void tallySimple(float[] pt)
    {
        realcount++;
        lookupTable[bin2index(whereIs(pt))]++;
    }
//}}}

//{{{ Gaussians
//##################################################################################################
    /**
    * Calculates the contribution of this point to neighboring bins using a Gaussian mask.
    * The function is scaled such that regardless of the mask radius, it encloses a total
    * area/volume/etc. of exactly one. Probably. Unless my math's off. Which it might be.
    *
    * <p>The formula: {1/[b*sqrt(pi)]^n}*e^[ -(x/b)^2 ]
    *
    * @param pt the point to tally
    * @param mask the mask radius
    */
    public void tallyGaussian(float[] pt, float mask)
    {
        tgf_b = mask / 1.5174f; // sqrt[ -ln .10 ] = 1.5147
        tgf_a = 1.0f / (float)Math.pow(tgf_b * 1.7725, nDim); // sqrt(pi) = 1.7725
        tallyGaussian(pt, mask, tgf_a);
    }

//##################################################################################################
    /**
    * Calculates the contribution of this point to neighboring bins using a Gaussian mask.
    *
    * <p>The general form is e^[ -(x/b)<sup>2</sup> ], where <i>b</i> is calculated such that
    * the function falls to 10% of its maximum value at a distance of <i>mask</i> from the center.
    *
    * <p>The maximum value will be <i>scale</i>, which is usually equal to 1.
    *
    * @param pt the point to tally
    * @param mask the mask radius
    * @param scale the maximum height of the function (1 is typical)
    */
    public void tallyGaussian(float[] pt, float mask, float scale)
    {
        realcount++;

        // makes use of the tgfXXX public variables!
        tgf_pt = pt;

        // determine the coefficients
        tgf_b = mask / 1.5174f; // sqrt[ -ln .10 ] = 1.5147
        tgf_b_2 = tgf_b*tgf_b;
        tgf_a = scale;

        // Other numbers we need
        float range = GSN_REACH*mask;

        // set up the limits of exploration
        for(int i = 0; i < nDim; i++)
        {
            // round() is the best choice, b/c the half-bin marks correspond to the point
            // where the function will be evaluated anyway...
            // Bin numbers may be negative, or too large. It will be taken care of later.
            tgf_start[i] = Math.round((pt[i]-minVal[i]-range)/wBin[i]);
            tgf_end[i]   = Math.round((pt[i]-minVal[i]+range)/wBin[i]);
            tgf_curr[i]  = tgf_start[i];
        }

        tgfRecursiveLoop(0);
    }

    // Use recursion to loop over an unknown number of dimensions
    void tgfRecursiveLoop(int depth)
    {
        // Set up a loop and go down to the next level
        if(depth < nDim)
        {
            for(tgf_curr[depth] = tgf_start[depth]; tgf_curr[depth] <= tgf_end[depth]; tgf_curr[depth]++)
            {
                tgfRecursiveLoop(depth+1);
            }
        }
        // We're at the bottom -- calculate the contribution to our current position
        else
        {
            // Make sure this is a real bin before we get going...
            int i = bin2index(tgf_curr);
            if(i != -1)
            {
                // It's a real bin, or at least it wraps to a real bin.
                // Calculate a center for it.
                float[] bin_center = centerOf(tgf_curr);

                float dist_2 = distanceSquared(bin_center, tgf_pt);
                float gaussian = tgf_a * (float)Math.exp( -dist_2 / tgf_b_2 );
                lookupTable[i] += gaussian;
            }
        }
    }
//}}}

//{{{ Cosines
//##################################################################################################
    /**
    * Calculates the contribution of this point to neighboring bins using a cosine mask.
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
    * @param mask the mask radius, <i>a</i>
    */
    public void tallyCosine(float[] pt, float mask)
    {
        float scale = 1.0f / (float)Math.pow(mask, nDim);
        tallyCosine(pt, mask, scale);
    }

//##################################################################################################
    /**
    * Calculates the contribution of this point to neighboring bins using a cosine mask.
    * This mask is very similar to the Gaussian but does actually reach zero at a finite distance.
    *
    * @param pt the point to tally
    * @param mask the mask radius (where the function falls to zero)
    * @param scale multiplier for the whole function (1 is typical)
    */
    public void tallyCosine(float[] pt, float mask, float scale)
    {
        realcount++;

        // hijacks the tgfXXX public variables!
        tgf_pt = pt;

        // determine the coefficients
        tgf_b = mask / (float)Math.PI; // we want to sample from -pi to +pi
        tgf_a = scale;
        tgf_mask_2 = mask * mask;

        // set up the limits of exploration
        for(int i = 0; i < nDim; i++)
        {
            // round() is the best choice, b/c the half-bin marks correspond to the point
            // where the function will be evaluated anyway...
            // Bin numbers may be negative, or too large. It will be taken care of later.
            tgf_start[i] = Math.round((pt[i]-minVal[i]-mask)/wBin[i]);
            tgf_end[i]   = Math.round((pt[i]-minVal[i]+mask)/wBin[i]);
            tgf_curr[i]  = tgf_start[i];
        }

        cosRecursiveLoop(0);
    }

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
            int i = bin2index(tgf_curr);
            if(i != -1)
            {
                // It's a real bin, or at least it wraps to a real bin.
                // Calculate a center for it.
                float[] bin_center = centerOf(tgf_curr);

                float dist = distanceSquared(bin_center, tgf_pt);
                // Doing this prevents us from inadvertently
                // including negative values in our sum.
                if(dist < tgf_mask_2)
                {
                    dist = (float)Math.sqrt(dist);
                    //float cosine = Math.max(0, tgf_a * (float)Math.cos( dist/tgf_b ));
                    // Function varies from 0 to 2, as in published Rama/C-beta paper
                    float cosine = tgf_a * (float)(Math.cos(dist/tgf_b) + 1.0);
                    lookupTable[i] += cosine;
                }
            }
        }
    }
//}}}

//{{{ zero, scale, normalize, standardize, transformLog, fractionLessThan
//##################################################################################################
    /**
    * Zeros out every bin. Good for starting over ;)
    */
    public void zero()
    {
        realcount = 0;
        for(int i = 0; i < lookupTable.length; i++) lookupTable[i] = 0.0f;
    }

    /**
    * Scales every bin. Useful for correcting the effects of tallyXXX().
    *
    * @param f the scaling factor to multiply by
    */
    public void scale(float f)
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
        float f = 1;
        for(int i = 0; i < nDim; i++) f *= wBin[i];
        scale( (float)realcount / f / totalCount() );
    }
    
    /**
    * Scales all the values of the table such that the greatest value in the table is equal to some specified value (e.g., 1.0).
    *
    * @param max the top end of the scale the table should lie on.
    */
    public void standardize(float max)
    {
        scale( max / maxValue() );
    }

    /**
    * Transforms every value <i>v</i> in the table on a log scale.
    * The mapping <i>v</v> --&gt; <i>v'</i> is as follows:
    * <p>v' = ln(v+1)
    * <p>The 'plus one' keeps values of zero equal to zero.
    */
    public void transformLog()
    {
        for(int i = 0; i < lookupTable.length; i++) lookupTable[i] = (float)Math.log(lookupTable[i] + 1.0f);
    }
    
    /**
    * Transforms the table so each (new) value reflects the fraction of entries in
    * samples that is less than the (old) value stored in the table.
    * Afterward all table values will range between 1.0 (higher density than any entry in samples)
    * and 0.0 (lower density than any entry in samples).
    * @param samples an array of density values to use for comparison. Will be sorted by this function.
    */
    public void fractionLessThan(float[] samples)
    {
        double n, size = samples.length;
        Arrays.sort(samples); // so we can do binary searches
        
        for(int i = 0; i < lookupTable.length; i++)
        {
            n = Arrays.binarySearch(samples, lookupTable[i]);
            // weird optimization was producing negative zeros in output
            //if(n < 0) n = -(n+1); // calculate insertion point
            if(n < 0) n = Math.abs(n+1);
            lookupTable[i] = (float)(n / size);
        }
    }
//}}}

//{{{ writeBinary(), constructor-from-binary-file
//##################################################################################################
    /**
    * Saves the contents of the table to a file for re-use later.
    *
    * First, the name is written out as a C-style string (one byte per character, null terminated).
    * Then nDim is written, then all of minVal, all of maxVal, all of nBins, and all of doWrap. Next is realcount.
    * Finally, all of lookupTable is written out. In theory then, any other
    * application can also read and use this data. Note that high bytes are
    * written first. Some machines and platforms may need to take this into
    * account.
    *
    * @param out the stream to write to.
    *
    * @throws IOException if an IO error occurs.
    */
    public void writeBinary(DataOutputStream out) throws IOException
    {
        out.writeBytes(ourName);                                                // length bytes
        out.writeByte(0);                                                       // 1 byte

        out.writeInt(nDim);                                                     // 4 bytes

        int i;
        for(i = 0; i < nDim; i++) out.writeFloat(minVal[i]);                    // ndim*4 bytes
        for(i = 0; i < nDim; i++) out.writeFloat(maxVal[i]);                    // ndim*4 bytes
        for(i = 0; i < nDim; i++) out.writeInt(nBins[i]);                       // ndim*4 bytes
        for(i = 0; i < nDim; i++) out.writeBoolean(doWrap[i]);                  // ndim*1 bytes

        out.writeInt(realcount);                                                // 4 bytes

        for(i = 0; i < lookupTable.length; i++) out.writeFloat(lookupTable[i]); // nvals*4 bytes
    }

    /**
    * Recreates an <i>n</i>-dimensional lookup table of floating point numbers from a file.
    * See <code>writeBinary()</code> for details of the format.
    *
    * @param in The stream to read from
    *
    * @throws IOException if there is a problem reading from the stream
    */
    public NDFloatTable(DataInputStream in) throws IOException
    {
        // Extract our name
        StringBuffer sb = new StringBuffer();
        char c;
        while( (c = (char)in.readUnsignedByte()) != 0 ) sb.append(c);
        ourName = sb.toString();

        // How many dimensions?
        nDim = in.readInt();

        // Allocate storage for minVal, maxVal, nBins, doWrap
        minVal  = new   float[nDim];
        maxVal  = new   float[nDim];
        nBins   = new     int[nDim];
        doWrap  = new boolean[nDim];

        // Read them in
        int i;
        for(i = 0; i < nDim; i++) minVal[i]  = in.readFloat();
        for(i = 0; i < nDim; i++) maxVal[i]  = in.readFloat();
        for(i = 0; i < nDim; i++) nBins[i] = in.readInt();
        for(i = 0; i < nDim; i++) doWrap[i] = in.readBoolean();

        // Get the number of real points
        realcount = in.readInt();

        // Calculate the bin widths.
        wBin = new float[nDim];
        for(i = 0; i < nDim; i++) wBin[i] = (maxVal[i] - minVal[i]) / nBins[i];

        // Allocate storage for the tgfXXX variables
        tgf_start = new int[nDim];
        tgf_end   = new int[nDim];
        tgf_curr  = new int[nDim];

        // Calculate number of entries in lookupTable
        int n_entries = 1;
        for(i = 0; i < nDim; i++) n_entries *= nBins[i];

        // Allocate table and initialize it
        lookupTable = new float[n_entries];
        realcount = 0;
        for(i = 0; i < lookupTable.length; i++) lookupTable[i] = in.readFloat();
    }
//}}}

//{{{ writeText()
//##################################################################################################
    /**
    * Writes out a human-readable version of the data in this table.
    * Format is self-documenting; lines begining with a hash (#) are comments
    * @param ps the stream to write to
    */
    public void writeText(PrintStream ps)
    {
        int i;

        ps.println("# Table name/description");
        ps.println("\""+ourName+"\"");
        
        ps.println("# Number of dimensions");
        ps.println(nDim);
        
        ps.println("# For each dimension, 1 to "+nDim+": lower_bound  upper_bound  number_of_bins  wrapping");
        for(i = 0; i < nDim; i++)
        {
            ps.print(minVal[i]);
            ps.print(" ");
            ps.print(maxVal[i]);
            ps.print(" ");
            ps.print(nBins[i]);
            ps.print(" ");
            ps.print(doWrap[i]);
            ps.println();
        }

        ps.println("# List of table values. For each dimension, define step_size as");
        ps.println("#     (upper_bound - lower_bound) / number_of_bins ");
        ps.println("#");
        ps.println("# In each dimension, sampling starts at lower_bound + (step_size/2),");
        ps.println("# continues at intervals of step_size, and does not exceed upper_bound.");
        ps.println("# There are thus number_of_bins samples along that dimension.");
        ps.println("#");
        ps.println("# Index for dimension 1 increases most slowly, index for dimension "+nDim);
        ps.println("# increases most quickly. Line breaks are arbitrary (for readability).");
        ps.println("#");
        ps.println("# More details from http://kinemage.biochem.duke.edu");
        for(i = 0; i < lookupTable.length; )
        {
            ps.print(lookupTable[i]);
            if( ++i % 6 == 0 ) ps.println();
            else ps.print(" ");
        }
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
    public float[] getMinBounds()
    {
        float[] retval = new float[nDim];
        System.arraycopy(minVal, 0, retval, 0, nDim);
        return retval;
    }

    /**
    * Gets the maximum bounds for this table.
    * @return maximum bounds for this table
    */
    public float[] getMaxBounds()
    {
        float[] retval = new float[nDim];
        System.arraycopy(maxVal, 0, retval, 0, nDim);
        return retval;
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

//{{{ echo
//##################################################################################################
    // Convenience function for debugging
    void echo(String s) { System.err.println(s); }
//}}}
}//class
