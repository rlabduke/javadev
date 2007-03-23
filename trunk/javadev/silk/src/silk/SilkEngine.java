// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk;

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
* <code>SilkEngine</code> contains the core processing functions.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Apr 16 15:42:40 EDT 2003
*/
public class SilkEngine //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public SilkEngine()
    {
    }
//}}}

//{{{ processData
//##################################################################################################
    /**
    * Accepts a Collection of DataSample's and a set of SilkOption's
    * and produces a final NDimTable that contains the smoothed data.
    */
    public NDimTable processData(Collection dataSamples, SilkOptions options)
    {
        // Check to make sure all needed options are in place
        options.fillInMissing();
        
        // Pre-wrap data, do anisotropic coordinate scaling, crop out unwanted points
        dataSamples = cleanData(dataSamples, options);
        
        // Unroll bounds into min and max
        double[] min = new double[options.nDim], minOrig = new double[options.nDim];
        double[] max = new double[options.nDim], maxOrig = new double[options.nDim];
        for(int i = 0; i < options.nDim; i++)
        {
            minOrig[i]  = options.bounds[2*i];
            maxOrig[i]  = options.bounds[2*i + 1];
            min[i]      = options.bounds[2*i] / options.aniso[i];
            max[i]      = options.bounds[2*i + 1] / options.aniso[i];
        }
        
        // Create a table of appropriate size
        NDimTable densityTrace;
        if(options.sparse)
        {
            densityTrace = new NDimTable_Sparse(options.title,
                options.nDim, min, max,
                options.gridsamples, options.wrap,
                dataSamples.size()); // starting size: one bin per data sample
        }
        else
        {
            densityTrace = new NDimTable_Dense(options.title,
                options.nDim, min, max,
                options.gridsamples, options.wrap);
        }
        
        // Do first (fixed-width) processing step
        if(options.operation == SilkOptions.OP_HISTOGRAM)
            traceHist(densityTrace, dataSamples);
        else if(options.operation == SilkOptions.OP_GAUSSIAN)
            traceGaussianFixed(densityTrace, dataSamples, options.halfwidth);
        else if(options.operation == SilkOptions.OP_COSINE)
            traceCosineFixed(densityTrace, dataSamples, options.halfwidth);
        else throw new IllegalArgumentException("Unknown operation: "+options.operation);
        
        // Do second (density-dependent) processing step
        if(options.twopass)
            densityTrace = processDensityDependent(densityTrace, dataSamples, options);
        
        // Reset table bounds in case of anisotropic smoothing masks. If aniso == 1, no effect.
        densityTrace.resetBounds(minOrig, maxOrig);
        // Also reset data coords, as they're needed for convert-to-fraction
        anisoUnscaleData(dataSamples, options);
        
        // Apply post-smoothing operations
        if(options.postop == SilkOptions.POSTOP_NONE)           {} // already had normalize() called
        else if(options.postop == SilkOptions.POSTOP_COUNTS)    densityTrace.scale( densityTrace.realCount() / densityTrace.totalCount() );
        else if(options.postop == SilkOptions.POSTOP_LN)        densityTrace.transformLog();
        else if(options.postop == SilkOptions.POSTOP_0TO1)      densityTrace.standardize(1.0);
        else if(options.postop == SilkOptions.POSTOP_FRACTION)  // convert to fraction excluded
        {
            double[]    densityValues   = new double[ dataSamples.size() ];
            Iterator    iter            = dataSamples.iterator();
            for(int i = 0; i < dataSamples.size(); i++)
            { densityValues[i] = densityTrace.valueAt(((DataSample)iter.next()).coords); }
            densityTrace.fractionLessThan(densityValues);
        }
        else if(options.postop == SilkOptions.POSTOP_ENERGY)
        {
            // Convert to energy in kcal/mol, with an arbitrary zero point.
            // The probability of state i, p_i, is related to its energy E_i
            // by a Boltzmann exponential:
            //      p_i = exp[ -E_i / (kB * T) ] / Q
            // where the partition function Q = sum(exp[ -E_j / kT ]) over all j.
            // For calculating E_i from p_i, Q can be ignored because it just
            // determines where the zero-point of the energy scale is set:
            //      E_i = -kT ln(p_i) - kT ln Q
            densityTrace.scale( 1.0 / densityTrace.totalCount() );  // convert to probability
            densityTrace.transformTrueNaturalLog();                 // 0 prob. -> -inf
            densityTrace.scale(-0.0019872 * 298);                   // k_Boltzmann in kcal/mol.K   *   temperature in K
        }
        
        if(options.scale != 1.0) densityTrace.scale(options.scale);
        
        if(options.hillClimb)
        {
            if(options.hillSquash > 0) densityTrace.squash(options.hillSquash); // squash(0) has no effect
            densityTrace.classifyByHills(); // label remaining values by hill climbing
        }
        
        return densityTrace;
    }
//}}}

//{{{ traceHist, trace{Gaussian, Cosine}Fixed
//##################################################################################################
    /** Creates a histogram trace */
    void traceHist(NDimTable densityTrace, Collection dataSamples)
    {
        DataSample sample;
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            densityTrace.tallySimple(sample.coords, sample.weight);
        }
        densityTrace.normalize(); // shouldn't be necessary for a histogram
    }
    
    /** Creates a Gaussian trace using a fixed mask width */
    void traceGaussianFixed(NDimTable densityTrace, Collection dataSamples, double halfwidth)
    {
        DataSample sample;
        int i = 0;
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            densityTrace.tallyGaussian(sample.coords, halfwidth, sample.weight);
            if(++i % 100 == 0) System.err.print("\r  "+i+" points have been tallied"); System.err.flush();
        }
        densityTrace.normalize();
        System.err.println("\r  "+i+" points have been tallied");
    }
    
    /** Creates a cosine trace using a fixed mask width */
    void traceCosineFixed(NDimTable densityTrace, Collection dataSamples, double halfwidth)
    {
        DataSample sample;
        int i = 0;
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            densityTrace.tallyCosine(sample.coords, halfwidth, sample.weight);
            if(++i % 100 == 0) System.err.print("\r  "+i+" points have been tallied"); System.err.flush();
        }
        densityTrace.normalize();
        System.err.println("\r  "+i+" points have been tallied");
    }
//}}}

//{{{ processDensityDependent
//##################################################################################################
    /**
    * Accepts a Collection of DataSample's and a set of SilkOption's
    * and produces a final NDimTable that contains the smoothed data.
    */
    public NDimTable processDensityDependent(NDimTable densityTrace, Collection dataSamples, SilkOptions options)
    {
        // Create a second table of the same size as the first
        NDimTable trace2;
        if(options.sparse)
        {
            trace2 = new NDimTable_Sparse(densityTrace.getName(),
                densityTrace.getDimensions(), densityTrace.getMinBounds(), densityTrace.getMaxBounds(),
                densityTrace.getBins(), densityTrace.getWrap(),
                dataSamples.size()); // starting size: one bin per data sample
        }
        else
        {
            trace2 = new NDimTable_Dense(densityTrace.getName(),
                densityTrace.getDimensions(), densityTrace.getMinBounds(), densityTrace.getMaxBounds(),
                densityTrace.getBins(), densityTrace.getWrap());
        }

        // Do second (variable-width) processing step
        DataSample  sample;
        int i = 0;
        double      density, halfwidth;
        if(options.operation == SilkOptions.OP_GAUSSIAN)
        {
            for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
            {
                sample      = (DataSample)iter.next();
                density     = densityTrace.valueAt(sample.coords);
                halfwidth   = options.ddhalfwidth / Math.pow(density, options.lambda/options.nDim);
                trace2.tallyGaussian(sample.coords, halfwidth, sample.weight);
                if(++i % 100 == 0) System.err.print("\r  "+i+" points have been tallied"); System.err.flush();
            }
        }
        else if(options.operation == SilkOptions.OP_COSINE)
        {
            for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
            {
                sample      = (DataSample)iter.next();
                density     = densityTrace.valueAt(sample.coords);
                halfwidth   = options.ddhalfwidth / Math.pow(density, options.lambda/options.nDim);
                trace2.tallyCosine(sample.coords, halfwidth, sample.weight);
                if(++i % 100 == 0) System.err.print("\r  "+i+" points have been tallied"); System.err.flush();
                //if(++i % 1 == 0) System.err.print("\r  "+i+" points have been tallied"); System.err.flush();
            }
        }
        else throw new IllegalArgumentException("Illegal operation for two-pass smoothing: "+options.operation);

        trace2.normalize();
        System.err.println("\r  "+i+" points have been tallied");
        return trace2;
    }
//}}}

//{{{ cleanData, aniso(Un)ScaleData, cropData
//##################################################################################################
    /** Crops data points, wraps coordinates, applies aniso scaling. MAY modify input data. */
    public Collection cleanData(Collection dataSamples, SilkOptions options)
    {
        wrapData(dataSamples, options);
        dataSamples = cropData(dataSamples, options);
        anisoScaleData(dataSamples, options);
        return dataSamples;
    }
    
    public void anisoScaleData(Collection dataSamples, SilkOptions options)
    {
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            DataSample sample = (DataSample) iter.next();
            for(int i = 0; i < options.nDim; i++) sample.coords[i] /= options.aniso[i];
        }
    }
    
    public void anisoUnscaleData(Collection dataSamples, SilkOptions options)
    {
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            DataSample sample = (DataSample) iter.next();
            for(int i = 0; i < options.nDim; i++) sample.coords[i] *= options.aniso[i];
        }
    }
    
    public Collection cropData(Collection dataSamples, SilkOptions options)
    {
        ArrayList croppedSamples = new ArrayList();
        samples: for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            DataSample sample = (DataSample) iter.next();
            for(int i = 0; i < options.nDim; i++)
            {
                if(sample.coords[i] < options.crop[2*i]
                || sample.coords[i] > options.crop[2*i + 1]) continue samples;
            }
            croppedSamples.add(sample);
        }
        return croppedSamples;
    }
//}}}

//{{{ wrapData
//##################################################################################################
    /**
    * Modifies the coordinates of each sample to fall within
    * the specified bounds of the given options (but only if
    * wrapping is enabled for the relevant dimension)
    */
    public void wrapData(Collection dataSamples, SilkOptions options)
    {
        DataSample sample;
        double min, max, span, val;
        
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            for(int i = 0; i < options.nDim; i++)
            {
                if(options.wrap[i])
                {
                    min = options.bounds[2*i];
                    max = options.bounds[2*i + 1];
                    span = max - min;
                    // In Java, (-a) % b == -(a % b), unlike a real modulo function.
                    // BUG!! Old code produces [1,span] for negative numbers and [0,span-1] for positive numbers.
                    //      val = sample.coords[i] - min;
                    //      if(val < 0) val = span + (val % span);
                    //      else        val = (val % span);
                    // Fixed 20 Jan 2006; results before this may be (very slightly) wrong.
                    val = (sample.coords[i] - min) % span;
                    if(val < 0) val += span;
                    sample.coords[i] = val + min;
                }
            }//for(each dimension)
        }//for(each data point)
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

