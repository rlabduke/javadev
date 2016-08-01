// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.backrub;

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
* <code>BackbonePlotter</code> is a generic interaface to anything that could
* produce kinemage-like output of backbone conformations.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 18 14:05:37 EST 2003
*/
public interface BackbonePlotter //extends ... implements ...
{
    /**
    * Creates a plot of the given protein backbone residues
    * @param aaBackbones a Collection of AABackbone objects to plot sequentially,
    *   ordered from N to C
    * @param plot the plotter to direct the output to
    */
    public void plotBackbone(Collection aaBackbones);
}//class

