// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>ExpSlider</code> provides interpretation of JSlider values
* as floating-point numbers on an exponential scale, such that
* a fixed amount of slider movement represents a doubleing
* or halving of the floating-point value.
*
* <p>For a slider on the interval [A, B] where A and B are real,
* with N steps separting them, then for a slider with integer
* value s on [0, N] the "exponential" value v is:
*
* <p>v = A*exp[ s/Q ], for s < N
*<br>v = A*exp[ s/Q ] = B, for s = N
*
* <p>This implies Q = N / ln(B/A).
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jun 27 09:38:30 EDT 2003
*/
public class ExpSlider extends JSlider // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    private double minVal, maxVal;
    private int steps;
    private double q;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a horizontal JSlider from 0 to steps with an initial value of steps/2.
    * Values will be interpretted as being between minVal and maxVal, inclusive.
    * All other settings are standard for a new JSlider.
    */
    public ExpSlider(double minVal, double maxVal, int steps)
    {
        super(0, steps, steps/2);
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.steps  = steps;
        
        this.q = steps / Math.log(maxVal / minVal);
    }
//}}}

//{{{ getDouble, setDouble
//##################################################################################################
    /** Returns the interpretted value of this slider. */
    public double getDouble()
    {
        return getDouble(this.getValue());
    }
    
    protected double getDouble(int s)
    {
        if(s == 0)          return minVal;
        if(s == steps)      return maxVal;
        
        return (minVal * Math.exp( s / q ));
    }
    
    /** Sets this slider to the value nearest to the specified value. */
    public void setDouble(double t)
    {
        // Make sure we're in range.
        if(t < minVal)      t = minVal;
        else if(t > maxVal) t = maxVal;
        
        int s = (int)Math.round(q * Math.log( t / minVal ));
        if(s < 0)           s = 0;
        else if(s > steps)  s = steps;
        
        this.setValue(s);
    }
//}}}

//{{{ setLabels
//##################################################################################################
    /**
    * Creates the specified number of labels using the provided formatter.
    * 1 label will be displayed at steps/2.
    * More than 1 label will hit both endpoints and the rest even spaced in between.
    * Label painting will be automatically enabled.
    */
    public void setLabels(int numLabels, NumberFormat nf)
    {
        Hashtable labels = new Hashtable();
        if(numLabels < 2)
            labels.put( new Integer(steps/2), new JLabel(nf.format(getDouble(steps/2))) );
        else
        {
            for(double i = 0; i < numLabels; i++)
            {
                int pos = (int)Math.round( i/(numLabels-1) * steps );
                labels.put( new Integer(pos), new JLabel(nf.format(getDouble(pos))) );
            }
        }
        
        setLabelTable(labels);
        setPaintLabels(true);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

