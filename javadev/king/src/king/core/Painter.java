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
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 21 19:11:16 EDT 2004
*/
public interface Painter //extends ... implements ...
{
    public void paintBall(Graphics2D g, Paint paint, double x, double y, double z, double r, boolean showHighlight);
    public void paintDot(Graphics2D g, Paint paint, double x, double y, double z, int width);
    public void paintLabel(Graphics2D g, Paint paint, Font font, FontMetrics metrics,
        String label, double x, double y, double z);
    public void paintMarker(Graphics2D g, Paint paint, double x, double y, double z, int width, int paintStyle);
    public void paintTriangle(Graphics2D g, Paint paint,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3);
    public void paintVector(Graphics2D g, Paint paint, int width, int widthCue,
        double x1, double y1, double z1,
        double x2, double y2, double z2);
}//class

