// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>Paintable</code> is a graphics object that can be
* rendered directly in some Component, without further
* transformations, etc. It may be an inherently 2-D object
* or a 2-D projection of a 3-D object, but its coordinates
* are in screen-space and relative to some specific
* PaintRegion (i.e. a canvas).
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 27 10:19:24 EST 2003
*/
public interface Paintable //extends ... implements ...
{
    /**
    * Produces a lower-quality, higher-speed rendering of
    * this paintable. If no such rendering is possible,
    * it should produce the same results as paintStandard()
    */
    public void paintFast(Graphics2D g, Engine engine);
    
    /**
    * Renders this Paintable to the specified graphics surface,
    * using the display settings from engine.
    */
    public void paintStandard(Graphics2D g, Engine engine);
    
    /**
    * Produces a higher-quality, lower-speed rendering of
    * this paintable. If no such rendering is possible,
    * it should produce the same results as paintStandard()
    */
    public void paintHighQuality(Graphics2D g, Engine engine);
    
    /**
    * Returns a KPoint representing the point picked
    * if a mouse click at (x,y) would pick this Paintable;
    * otherwise, returns null.
    */
    public KPoint pickPoint(int x, int y);
}//class

