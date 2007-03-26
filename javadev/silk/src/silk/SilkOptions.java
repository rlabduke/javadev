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
* <code>SilkOptions</code> is a simple container for the various
* processing options that Silk supports.
* It also contains methods for validating the settings.
*
* Note that column indices in the input data start from 1, not 0.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Apr 16 15:42:39 EDT 2003
*/
public class SilkOptions //extends ... implements ...
{
//{{{ Constants
    public static final String      OP_HISTOGRAM        = "histogram";
    public static final String      OP_GAUSSIAN         = "Gaussian density trace";
    public static final String      OP_COSINE           = "cosine density trace";
    public static final String      POSTOP_NONE         = "no-op (counts-per-unit)";
    public static final String      POSTOP_COUNTS       = "counts-per-bin";
    public static final String      POSTOP_LN           = "natural-log";
    public static final String      POSTOP_0TO1         = "zero-to-one";
    public static final String      POSTOP_FRACTION     = "convert-to-fraction";
    public static final String      POSTOP_PROB         = "probability (bins-sum-to-one)";
    public static final String      POSTOP_ENERGY       = "energy (-kT ln p_i @ 298K)";
    public static final String      OUTPUT_VALUE_LAST   = "text (value last)";
    public static final String      OUTPUT_VALUE_FIRST  = "text (value first)";
    public static final String      OUTPUT_KINEMAGE     = "kinemage";
    public static final String      OUTPUT_NDFT         = "binary (NDFT)";
    public static final int         V_STANDARD          = 0;
    public static final int         V_QUIET             = -10;
    public static final int         V_VERBOSE           = 10;
//}}}

//{{{ Variable definitions
//##################################################################################################
    // BAYESIAN STATS (PRIOR)
    /** If set, it will be used as the prior for this distribution (default null) */
    public SilkOptions  prior       = null;
    /** The weighting factor that will be applied to counts from the prior (default 1.0 = equal weights, higher = stronger prior) */
    public double       priorWeight = 1.0;
    
    // INPUT
    /** Collection of DataSample objects;  if left as null we try to load from a stream. */
    public Collection   data        = null;
    /** Stream to try loading data from if not null (defaults to null). */
    public Reader       dataSource  = null;
    /** Number of dimensions (required) */
    public int          nDim        = 0;
    /** Column to take data labels from (defaults to none) */
    public int          label       = 0;
    /** Columns to take data coordinates from (defaults to 1, 2, 3, ...) */
    public int[]        coords      = null;
    /** Column to take the per-sample weight from (defaults to none )*/
    public int          weight      = 0;
    /** Bounds of data space: min1, max1, min2, max2, ... (required) */
    public double[]     bounds      = null;
    /** Whether or not the bounds are wrapped (defaults to false) */
    public boolean[]    wrap        = null;
    /** Cropping for input data, in the space defined by bounds. (defaults to none) */
    public double[]     crop        = null;
    /** Character to use as a separator when parsing the input file (defaults to space) */
    public char         inSep       = ' ';
    /** Whether to use sparse or dense data storage. Defaults to sparse (true). */
    public boolean      sparse      = true;
    
    // SMOOTHING
    /** Which smoothing operation to carry out on the input data */
    public String       operation   = OP_HISTOGRAM;
    /** The halfwidth to use for Gaussian or cosine smoothing (required for Gaussian, cosine) */
    public double       halfwidth   = 0;
    /**
    * Effective per-dimension scaling factors for Gaussian/cosine mask.
    * Inverse scaling factors for input coords on input; as-is for scaling on output.
    */
    public double[]     aniso       = null;
    /** The size of each bin (either this or gridsamples is required) */
    public double[]     gridsize    = null;
    /** The number of bins in each dimension (either this or gridsize is required) */
    public int[]        gridsamples = null;
    /** Whether or not to use two-pass density-dependent smoothing (defaults to false) */
    public boolean      twopass     = false;
    /** The halfwidth to use for second-pass Gaussian or cosine smoothing (required for Gaussian, cosine) */
    public double       ddhalfwidth = 0;
    /** The parameter lambda to use in density-dependent smoothing (defaults to 0.5) */
    public double       lambda      = 0.5;
    
    // POST-PROCESSING
    /** Which post-processing operation to apply to the final data (defaults to none) */
    public String       postop      = POSTOP_NONE;
    /** Scale factor to apply to output data (defaults to 1.0) */
    public double       scale       = 1.0;
    /** After postop and scale, should we squash low values, climb hills, and label the peaks? (defaults to false) */
    public boolean      hillClimb = false;
    /** For hill climbing, squash values less than this down to zero. (defaults to 0 -- no squash) */
    public double       hillSquash = 0.0;
    
    // OUTPUT
    /** A short comment to associate with the output data (defaults to none) */
    public String       title       = "";
    /** The number of significant digits to keep in the output (defaults to all) */
    public int          sigdig      = 0;
    /** What type of output should be produced */
    public String       outputMode  = OUTPUT_VALUE_LAST;
    /** Where the output should be directed (defaults to stdout) */
    public OutputStream outputSink  = null;
    /** The charcter to use as a separator in text output (defaults to space) */
    public char         outSep      = ' ';
    /** The levels at which to contour a 1-D plot (null for none) */
    public double[]     contours    = null;
    /** How detailed output should be. Higher values mean more verbose. */
    public int          verbosity   = V_STANDARD;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public SilkOptions()
    {
    }
//}}}

//{{{ fillInMissing
//##################################################################################################
    /**
    * Fills in missing values with their defaults
    * and expands arrays that were specified "shorthand."
    * If gridsize is set, it will be used to set gridsamples
    * and then will be set to null.
    *
    * @throws IllegalArgumentException
    *   if either nDim or bounds has not yet been specified, or
    *   if both gridsize and gridsamples have not yet been specified, or
    *   if there's a size mismatch between some array and nDim.
    */
    public void fillInMissing()
    {
        // Dimensions and bounds
        if(nDim <= 0)       throw new IllegalArgumentException("nDim must be specified");
        if(bounds == null)  throw new IllegalArgumentException("bounds must be specified");
        else if(bounds.length != 2*nDim)
            throw new IllegalArgumentException("Length mismatch: bounds and nDim");
        if(crop == null)    crop = (double[]) bounds.clone();
        else if(crop.length != 2*nDim)
            throw new IllegalArgumentException("Length mismatch: crop and nDim");
        
        for(int i = 0; i < nDim; i++)
        {
            if(bounds[2*i] >= bounds[2*i + 1])
                throw new IllegalArgumentException("max bound < min bound in dimension "+i);
            if(crop[2*i] >= crop[2*i + 1])
                throw new IllegalArgumentException("max bound < min bound in dimension "+i);
        }
        
        // Coordinates
        if(coords == null)
        {
            coords = new int[nDim];
            for(int i = 0; i < nDim; i++) coords[i] = i+1;
        }
        else if(coords.length != nDim)
            throw new IllegalArgumentException("Length mismatch: coords and nDim");
        
        // Wrapping
        if(wrap == null)
        {
            wrap = new boolean[nDim];
            for(int i = 0; i < nDim; i++) wrap[i] = false;
        }
        else if(wrap.length == 1)
        {
            boolean doWrap = wrap[0];
            wrap = new boolean[nDim];
            for(int i = 0; i < nDim; i++) wrap[i] = doWrap;
        }
        else if(wrap.length != nDim)
            throw new IllegalArgumentException("Length mismatch: wrap and nDim");
        
        // Aniso scaling of data coordinates
        if(aniso == null)
        {
            aniso = new double[nDim];
            for(int i = 0; i < nDim; i++) aniso[i] = 1.0;
        }
        
        // Gridsize
        if(gridsize != null)
        {
            if(gridsize.length == 1)
            {
                double gSize = gridsize[0];
                gridsamples = new int[nDim];
                for(int i = 0; i < nDim; i++) gridsamples[i] = (int)((bounds[2*i + 1] - bounds[2*i])/gSize + 0.5);
            }
            else if(bounds.length != nDim)
            { throw new IllegalArgumentException("Length mismatch: gridsize and nDim"); }
            else
            {
                gridsamples = new int[nDim];
                for(int i = 0; i < nDim; i++) gridsamples[i] = (int)((bounds[2*i + 1] - bounds[2*i])/gridsize[i] + 0.5);
            }
            
            gridsize = null;
        }
        
        // Gridsamples
        if(gridsamples == null) throw new IllegalArgumentException("Either gridsize or gridsamples must be specified");
        else if(gridsamples.length == 1)
        {
            int gSamp = gridsamples[0];
            gridsamples = new int[nDim];
            for(int i = 0; i < nDim; i++) gridsamples[i] = gSamp;
        }
        else if(gridsamples.length != nDim)
            throw new IllegalArgumentException("Length mismatch: gridsamples and nDim");
        
        
        // Half-width
        if(operation == OP_GAUSSIAN || operation == OP_COSINE)
        {
            if(halfwidth == 0)
                throw new IllegalArgumentException("halfwidth must be specified");
            if(twopass && ddhalfwidth == 0)
                throw new IllegalArgumentException("ddhalfwidth must be specified");
        }
        
        // Output
        if(outputSink == null) outputSink = new BufferedOutputStream(System.out);
        if(outputMode == OUTPUT_NDFT) sparse = false; // must use dense table to write NDFT
        
        // Bayesian
        if(prior != null) sparse = false; // addPrior() doesn't work for sparse tables right now
        
        // Data input
        if(data == null)
        {
            if(dataSource == null)
                throw new IllegalArgumentException("Either data or dataSource must be specified");
            try
            {
                TabDataLoader       loader  = new TabDataLoader(this);
                LineNumberReader    in      = new LineNumberReader(dataSource);
                data = loader.parseReader(in);
            }
            catch(IOException ex)
            { throw new IllegalArgumentException("I/O error while loading from dataSource", ex); }
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

