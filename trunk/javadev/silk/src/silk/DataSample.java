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
* <code>DataSample</code> is a simple container for holding
* data points as they're read out of a file.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Apr 16 15:42:40 EDT 2003
*/
public class DataSample //extends ... implements ...
{
    public String       label   = "";
    public double[]     coords  = null;
    public double       weight  = 1.0;
    
    public DataSample() {}
    public DataSample(DataSample that)
    {
        this.label  = that.label;
        this.coords = that.coords;
        this.weight = that.weight;
    }
}//class

