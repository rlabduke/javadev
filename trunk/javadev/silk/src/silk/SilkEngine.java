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
        
        // Unroll bounds into min and max
        double[] min = new double[options.nDim];
        double[] max = new double[options.nDim];
        for(int i = 0; i < options.nDim; i++)
        {
            min[i] = options.bounds[2*i];
            max[i] = options.bounds[2*i + 1];
        }
        
        // Create a table of appropriate size
        NDimTable densityTrace = new NDimTable(options.title,
            options.nDim, min, max,
            options.gridsamples, options.wrap);
        
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
        
        // Apply post-smoothing operations
        if(options.postop == SilkOptions.POSTOP_NONE)           {}
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
            densityTrace.scale(-0.0019872 * 298);                    // k_Boltzmann in kcal/mol.K   *   temperature in K
        }
        if(options.scale != 1.0) densityTrace.scale(options.scale);
        
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
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            densityTrace.tallyGaussian(sample.coords, halfwidth, sample.weight);
        }
        densityTrace.normalize();
    }
    
    /** Creates a cosine trace using a fixed mask width */
    void traceCosineFixed(NDimTable densityTrace, Collection dataSamples, double halfwidth)
    {
        DataSample sample;
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            densityTrace.tallyCosine(sample.coords, halfwidth, sample.weight);
        }
        densityTrace.normalize();
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
        NDimTable trace2 = new NDimTable(densityTrace.getName(),
            densityTrace.getDimensions(), densityTrace.getMinBounds(), densityTrace.getMaxBounds(),
            densityTrace.getBins(), densityTrace.getWrap());

        // Do second (variable-width) processing step
        DataSample  sample;
        double      density, halfwidth;
        if(options.operation == SilkOptions.OP_GAUSSIAN)
        {
            for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
            {
                sample      = (DataSample)iter.next();
                density     = densityTrace.valueAt(sample.coords);
                halfwidth   = options.ddhalfwidth / Math.pow(density, options.lambda/options.nDim);
                trace2.tallyGaussian(sample.coords, halfwidth, sample.weight);
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
            }
        }
        else throw new IllegalArgumentException("Illegal operation for two-pass smoothing: "+options.operation);

        trace2.normalize();
        return trace2;
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
                    val = sample.coords[i] - min;
                    if(val < 0) val = span + (val % span);
                    else        val = (val % span);
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

