// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
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
* <code>Painter</code> is a standard interface for classes that are capable of
* rendering transformed KPoints as 2-D images (usually on the screen).
*
* <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 21 19:11:16 EDT 2004
*/
public interface Painter //extends ... implements ...
{
    public void paintBall(Paint paint, double x, double y, double z, double r, boolean showHighlight);
    public void paintDot(Paint paint, double x, double y, double z, int width);
    public void paintLabel(Paint paint, String label, double x, double y, double z);
    public void paintMarker(Paint paint, double x, double y, double z, int width, int paintStyle);
    public void paintSphereDisk(Paint paint, double x, double y, double z, double r);
    public void paintTriangle(Paint paint,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3);
    public void paintVector(Paint paint, int width, int widthCue,
        double x1, double y1, double z1,
        double x2, double y2, double z2);
    
    // Used by the auger tool for its targeting circle
    /** x,y,z is CENTER, not edge like it is for Java AWT functions */
    public void drawOval(Paint paint, double x, double y, double z, double width, double height);
    public void drawOval(Paint paint, int linewidth, int widthCue, double x, double y, double z, double width, double height);
        
    public void setFont(Font f);
    public int getLabelWidth(String s);
    public int getLabelAscent(String s);
    public int getLabelDescent(String s);
        
    public void setViewport(int x, int y, int width, int height);
    public void clearCanvas(Color color); // setViewport() must be called first!
}//class

