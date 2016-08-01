// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.points;
import king.core.*;

import java.awt.*;
import java.awt.geom.*;
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
* <code>ArrowPoint</code> represents the endpoint of a line
* and has an arrowhead on it.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Apr 26 16:46:09 EDT 2002
*/
public class ArrowPoint extends VectorPoint // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new data point representing one end of a line.
    *
    * @param label the pointID of this point
    * @param start where this line is drawn from, or null if it's the starting point
    */
    public ArrowPoint(String label, VectorPoint start)
    {
        super(label, start);
    }
//}}}

//{{{ paintStandard2
//##################################################################################################
    /**
    * This function exists solely for the convenience of ArrowPoints;
    * a good JIT will optimize it away for VectorPoints.
    * Coordinates are already transformed, perspective corrected, and clipped by Z planes.
    * They have NOT been clipped to the drawing area yet.
    */
    protected void paintStandard2(Engine2D engine, Paint paint, double fromX, double fromY, double fromZ, double toX, double toY, double toZ)
    {
        int lineWidth = calcLineWidth(engine);
        
        // Arrow tines are faked at the real "to" endpoint OR at the edge of the screen.
        // Each tine is a vector with a component perpedicular to the arrow body and a component parallel.
        // The parallel is foreshortened by the dot product of the body with <0,0,1>;
        // the perpendicular is unchanged, which keeps the arrow "facing" the screen as much as possible.
        // Perspective effects are ignored b/c the arrowheads are small.
        double tinePerp = 10, tinePar = 20; // in pixels
        if(parent != null)
        {
            // Yes, these calculations are not dirt cheap.
            // But no sensible kinemage has more than a few hundred arrows in it, tops.
            // So the total cost for repeating this calc for every arrow in the list is small.
            double angle = Math.toRadians( parent.getAngle() );
            double radius = parent.getRadius();
            tinePerp = (float)(radius * Math.sin(angle) * engine.zoom3D);
            tinePar  = (float)(radius * Math.cos(angle) * engine.zoom3D);
        }
        
        // Unit vector from arrow head toward arrow tail
        engine.work1.setXYZ( fromX-toX, fromY-toY, fromZ-toZ );
        if(engine.work1.mag2() < 1e-10) engine.work1.setXYZ(0,0,1);
        else engine.work1.unit();
        // Z vector and dot product (for foreshortening of tines)
        engine.work2.setXYZ(0,0,1);
        tinePar *= 1 - Math.abs( engine.work2.dot(engine.work1) );
        // Unit vector from arrow head toward arrow tail, in the plane of the screen!
        engine.work1.setXYZ( fromX-toX, fromY-toY, 0 );
        if(engine.work1.mag2() < 1e-10) engine.work1.setXYZ(1,0,0);
        else engine.work1.unit();

        // "To" ends of the line (where arrow head is drawn) must be clipped to
        // the edges of the screen so we can see outbound arrows when zoomed in.
        // This code ignores Z, which is why we calc'd foreshortening first.
        // We use the Cohen-Sutherland algorithm for clipping a line to a box.
        // Int flags represent being out of bounds on each of four sides:
        final int LEFT = 1, RIGHT = 2, BOTTOM = 4, TOP = 8;
        final double xmin = engine.pickingRect.x, ymin = engine.pickingRect.y;
        final double xmax = xmin+engine.pickingRect.width, ymax = ymin+engine.pickingRect.height;
        int toOutcode = 0, fromOutcode = 0;
        if(toX < xmin) toOutcode |= LEFT;
        else if(toX > xmax) toOutcode |= RIGHT;
        if(toY < ymin) toOutcode |= TOP;
        else if(toY > ymax) toOutcode |= BOTTOM;
        if(fromX < xmin) fromOutcode |= LEFT;
        else if(fromX > xmax) fromOutcode |= RIGHT;
        if(fromY < ymin) fromOutcode |= TOP;
        else if(fromY > ymax) fromOutcode |= BOTTOM;
        
        // If outcode is zero, the point is inside the clipping region.
        // If the AND of the outcodes is nonzero, thw whole line is outside the clipping region.
        if(toOutcode != 0 && (toOutcode & fromOutcode) == 0)
        {
            //paint = Color.red;
            if(toX < xmin)
            {
                toY = fromY + (toY-fromY)*(xmin-fromX)/(toX-fromX);
                toX = xmin;
            }
            else if(toX > xmax)
            {
                toY = fromY + (toY-fromY)*(xmax-fromX)/(toX-fromX);
                toX = xmax;
            }
            // Even though we've corrected the side-to-side clipping,
            // the top-to-bottom clipping may also need work
            // (e.g. if we're projecting out thru a corner of the canvas)
            if(toY < ymin)
            {
                toX = fromX + (toX-fromX)*(ymin-fromY)/(toY-fromY);
                toY = ymin;
            }
            else if(toY > ymax)
            {
                toX = fromX + (toX-fromX)*(ymax-fromY)/(toY-fromY);
                toY = ymax;
            }
        }
        
        // (-y,x) and (y,-x) are orthogonal to (x,y)
        // x and y offsets each have components from perpedicular and parallel.
        double dx, dy;
        dx = tinePar*engine.work1.getX() - tinePerp*engine.work1.getY();
        dy = tinePar*engine.work1.getY() + tinePerp*engine.work1.getX();
        engine.painter.paintVector(paint, lineWidth, engine.widthCue,
            toX, toY, toZ, toX+dx, toY+dy, toZ);
        dx = tinePar*engine.work1.getX() + tinePerp*engine.work1.getY();
        dy = tinePar*engine.work1.getY() - tinePerp*engine.work1.getX();
        engine.painter.paintVector(paint, lineWidth, engine.widthCue,
            toX, toY, toZ, toX+dx, toY+dy, toZ);
        
        // Main arrow body
        engine.painter.paintVector(paint, lineWidth, engine.widthCue,
            fromX, fromY, fromZ, toX, toY, toZ);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

