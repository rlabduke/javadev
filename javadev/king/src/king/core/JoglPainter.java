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

import net.java.games.jogl.*;
import net.java.games.jogl.util.GLUT;
//}}}
/**
* <code>JoglPainter</code> is a hardware-accelerated Painter that uses
* the JOGL Java bindings for OpenGL.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Jun  5 16:15:26 EDT 2004
*/
public class JoglPainter implements Painter
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    GL          gl;
    GLU         glu;
    GLUT        glut;
    Rectangle   clipRgn     = new Rectangle();
    int         currFont    = GLUT.BITMAP_HELVETICA_12;
    int[]       xPoints     = new int[6];
    int[]       yPoints     = new int[6];
    float[]     circle4, circle8, circle16, circle32;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public JoglPainter(GLDrawable drawable)
    {
        super();
        gl = drawable.getGL();
        glu = drawable.getGLU();
        glut = new GLUT();
        
        // This is necessary for antialiasing, but also for transparent objects.
        gl.glEnable(gl.GL_BLEND);
        gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);

        // Antialiasing for points and lines.
        // Almost everything we draw is done as polygons, so this doesn't help.
        //gl.glEnable(gl.GL_POINT_SMOOTH);
        //gl.glEnable(gl.GL_LINE_SMOOTH);

        // Antialiasing doesn't work for polygons (easily).
        // Best bet is to render multiple times with slight offset to accum. buffer.
        // See notes in JoglCanvas.
        // THIS CODE DOESN"T WORK:
        //gl.glBlendFunc(gl.GL_SRC_ALPHA_SATURATE, gl.GL_ONE);
        //gl.glEnable(gl.GL_POLYGON_SMOOTH);
        //gl.glDisable(gl.GL_DEPTH_TEST);
        
        circle4     = makeCircle(4);
        circle8     = makeCircle(8);
        circle16    = makeCircle(16);
        circle32    = makeCircle(32);
    }
//}}}

//{{{ makeCircle, fillOval, drawOval
//##############################################################################
    float[] makeCircle(int nVertices)
    {
        float[] c = new float[2*nVertices];
        for(int i = 0; i < nVertices; i++)
        {
            double angle = 2 * Math.PI * (double)i / (double)nVertices;
            // The /2 is a correction for using diameter instead of radius in fillOval()
            c[2*i]      = (float)(Math.cos(angle) / 2);
            c[2*i+1]    = (float)(Math.sin(angle) / 2);
        }
        return c;
    }
    
    void fillOval(double x, double y, double width, double height)
    {
        float[] circle;
        double diam = (width > height ? width : height);
        if(diam <= 3)       circle = circle4;
        else if(diam <= 8)  circle = circle8;
        else if(diam <= 16) circle = circle16;
        else                circle = circle32;
        
        double cx = x + width/2;
        double cy = y + height/2;
        gl.glBegin(gl.GL_POLYGON);
        for(int i = 0; i < circle.length; i+=2)
        {
            gl.glVertex2i((int)(cx + width*circle[i]), -(int)(cy + height*circle[i+1]));
        }
        gl.glEnd();
    }
    
    void drawOval(double x, double y, double width, double height)
    {
        float[] circle;
        double diam = (width > height ? width : height);
        if(diam <= 3)       circle = circle4;
        else if(diam <= 8)  circle = circle8;
        else if(diam <= 16) circle = circle16;
        else                circle = circle32;
        
        double cx = x + width/2;
        double cy = y + height/2;
        gl.glBegin(gl.GL_LINE_LOOP);
        for(int i = 0; i < circle.length; i+=2)
        {
            gl.glVertex2i((int)(cx + width*circle[i]), -(int)(cy + height*circle[i+1]));
        }
        gl.glEnd();
    }
//}}}

//{{{ drawLine, fillRect, drawRect
//##############################################################################
    void drawLine(int x1, int y1, int x2, int y2)
    {
        gl.glBegin(gl.GL_LINES);
        gl.glVertex2i(x1, -y1);
        gl.glVertex2i(x2, -y2);
        gl.glEnd();
    }
    
    void fillRect(int x, int y, int width, int height)
    {
        gl.glRecti(x, -y, x+width, -(y+height));
    }
    
    void drawRect(int x, int y, int width, int height)
    {
        gl.glBegin(gl.GL_LINE_LOOP);
        gl.glVertex2i(x, -y);
        gl.glVertex2i(x, -(y+height));
        gl.glVertex2i(x+width, -(y+height));
        gl.glVertex2i(x+width, -y);
        gl.glEnd();
    }
//}}}

//{{{ paintBall
//##################################################################################################
    public void paintBall(Paint paint, double x, double y, double z, double r, boolean showHighlight)
    {
        int d = (int)(2.0*r + 0.5);
        if(d < 2) d = 2; // make sure balls don't disappear
        
        // one disk
        setPaint(paint);
        fillOval(x-r, y-r, d, d);

        // Black rim, causes ~10% speed penalty but useful for visualization!
        try
        {
            Color c = (Color) paint;
            if(d >= 10 && c.getAlpha() == 255)
            {
                setPaint(Color.black);
                drawOval(x-r, y-r, d, d);
            }
        }
        catch(ClassCastException ex)
        {
            ex.printStackTrace();
            System.err.println("JoglPainter: tried painting with non-Color type of Paint");
        }
        
        // highlight
        if(showHighlight)
        {
            double off = 0.5 * r;
            d = (int)(0.3*r)+1;
            setPaint(Color.white);
            fillOval(x-off, y-off, d, d);
        }
    }
//}}}

//{{{ paintDot
//##################################################################################################
    public void paintDot(Paint paint, double x, double y, double z, int width)
    {
        double off = width/2;
        setPaint(paint);
        if(width == 1)
            fillRect((int)(x-off), (int)(y-off), width, width);
        else
        {
            width += 1; // not big enough otherwise
            fillOval(x-off, y-off, width, width);
        }
    }
//}}}

//{{{ paintLabel
//##################################################################################################
    public void paintLabel(Paint paint, String label, double x, double y, double z)
    {
        setPaint(paint);
        gl.glRasterPos2d(x, -y);
        glut.glutBitmapString(gl, currFont, label);
    }
//}}}

//{{{ paintMarker
//##################################################################################################
    public void paintMarker(Paint paint, double x, double y, double z, int width, int paintStyle)
    {
        int cx = (int)x, cy = (int)y;
        int one = width, two = 2*width, three = 3*width, four = 4*width, five = 5*width,
            six = 6*width, seven = 7*width, ten = 10*width, eleven = 11*width;
        this.setPaint(paint);
        
        // Large discs and boxes
        if((paintStyle & MarkerPoint.BOX_L) != 0) this.fillRect(cx-five, cy-five, eleven, eleven);
        else if((paintStyle & MarkerPoint.DISC_L) != 0) this.fillOval(cx-five, cy-five, eleven, eleven);
        // Medium discs and boxes
        if((paintStyle & MarkerPoint.BOX_M) != 0) this.fillRect(cx-three, cy-three, seven, seven);
        else if((paintStyle & MarkerPoint.DISC_M) != 0) this.fillOval(cx-three, cy-three, seven, seven);
        // Small discs and boxes
        if((paintStyle & MarkerPoint.BOX_S) != 0) this.fillRect(cx-one, cy-one, three, three);
        else if((paintStyle & MarkerPoint.DISC_S) != 0) this.fillOval(cx-one, cy-one, three, three);
        // Crosses
        if((paintStyle & MarkerPoint.CROSS_S) != 0) { this.drawLine(cx, cy-one, cx, cy+one); this.drawLine(cx-one, cy, cx+one, cy); }
        if((paintStyle & MarkerPoint.CROSS_M) != 0) { this.drawLine(cx, cy-three, cx, cy+three); this.drawLine(cx-three, cy, cx+three, cy); }
        if((paintStyle & MarkerPoint.CROSS_L) != 0) { this.drawLine(cx, cy-five, cx, cy+five); this.drawLine(cx-five, cy, cx+five, cy); }
        if((paintStyle & MarkerPoint.CROSS_2) != 0)
        {
            this.drawLine(cx-one, cy-five, cx-one, cy+five); this.drawLine(cx+one, cy-five, cx+one, cy+five);
            this.drawLine(cx-five, cy-one, cx+five, cy-one); this.drawLine(cx-five, cy+one, cx+five, cy+one);
        }
        // X's
        if((paintStyle & MarkerPoint.X_S) != 0) { this.drawLine(cx-one, cy-one, cx+one, cy+one); this.drawLine(cx-one, cy+one, cx+one, cy-one); }
        if((paintStyle & MarkerPoint.X_M) != 0) { this.drawLine(cx-three, cy-three, cx+three, cy+three); this.drawLine(cx-three, cy+three, cx+three, cy-three); }
        if((paintStyle & MarkerPoint.X_L) != 0) { this.drawLine(cx-five, cy-five, cx+five, cy+five); this.drawLine(cx-five, cy+five, cx+five, cy-five); }
        if((paintStyle & MarkerPoint.X_2) != 0)
        {
            this.drawLine(cx-four, cy-five, cx+five, cy+four); this.drawLine(cx-five, cy-four, cx+four, cy+five);
            this.drawLine(cx-four, cy+five, cx+five, cy-four); this.drawLine(cx-five, cy+four, cx+four, cy-five);
        }
        // Squares
        if((paintStyle & MarkerPoint.SQUARE_S) != 0) this.drawRect(cx-one, cy-one, two, two);
        if((paintStyle & MarkerPoint.SQUARE_M) != 0) this.drawRect(cx-three, cy-three, six, six);
        if((paintStyle & MarkerPoint.SQUARE_L) != 0) this.drawRect(cx-five, cy-five, ten, ten);
        // Circles
        if((paintStyle & MarkerPoint.RING_S) != 0) this.drawOval(cx-one, cy-one, two, two);
        if((paintStyle & MarkerPoint.RING_M) != 0) this.drawOval(cx-three, cy-three, six, six);
        if((paintStyle & MarkerPoint.RING_L) != 0) this.drawOval(cx-five, cy-five, ten, ten);
    }
//}}}

//{{{ paintSphereDisk
//##################################################################################################
    public void paintSphereDisk(Paint paint, double x, double y, double z, double r)
    {
        int d = (int)(2.0*r + 0.5);
        if(d < 2) d = 2; // make sure balls don't disappear
        
        // one disk
        setPaint(paint);
        fillOval(x-r, y-r, d, d);
    }
//}}}

//{{{ paintTriangle
//##################################################################################################
    public void paintTriangle(Paint paint,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3)
    {
        setPaint(paint);
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glVertex2i((int)x1, -(int)y1);
        gl.glVertex2i((int)x2, -(int)y2);
        gl.glVertex2i((int)x3, -(int)y3);
        gl.glEnd();
    }
//}}}

//{{{ paintVector
//##################################################################################################
    public void paintVector(Paint paint, int width, int widthCue,
        double x1, double y1, double z1,
        double x2, double y2, double z2)
    {
        setPaint(paint);
        prettyLine((int)x1, (int)y1, (int)x2, (int)y2, KPalette.lineWidths[width-1][widthCue]);
    }
//}}}

//{{{ prettyLine
//##################################################################################################
    /** Draws a thick line with nice ends, using fillPolygon(). Slightly slower (30-35%) than fastLine(). */
    void prettyLine(int x1, int y1, int x2, int y2, int width)
    {
        int s, e, abs_x2_x1, abs_y2_y1;
        s = -width / 2; // Start offset
        e = s + width;  // End offset
        abs_x2_x1 = Math.abs(x2 - x1);
        abs_y2_y1 = Math.abs(y2 - y1);
        
        // horizontal --
        if( abs_x2_x1 > abs_y2_y1 )
        {
            // left to right
            if( x1 < x2 )
            {
                xPoints[0] = x1  ; xPoints[1] = x1+s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2-s; xPoints[5] = x2;
                yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
            }
            // right to left
            else
            {
                xPoints[0] = x1  ; xPoints[1] = x1-s; xPoints[2] = x1;   xPoints[3] = x2;   xPoints[4] = x2+s; xPoints[5] = x2;
                yPoints[0] = y1+s; yPoints[1] = y1;   yPoints[2] = y1+e; yPoints[3] = y2+e; yPoints[4] = y2;   yPoints[5] = y2+s;
            }
        }
        // vertical |
        else
        {
            // top to bottom
            if( y1 < y2 )
            {
                xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                yPoints[0] = y1  ; yPoints[1] = y1+s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2-s; yPoints[5] = y2;
            }
            // bottom to top
            else
            {
                xPoints[0] = x1+s; xPoints[1] = x1;   xPoints[2] = x1+e; xPoints[3] = x2+e; xPoints[4] = x2;   xPoints[5] = x2+s;
                yPoints[0] = y1  ; yPoints[1] = y1-s; yPoints[2] = y1;   yPoints[3] = y2;   yPoints[4] = y2+s; yPoints[5] = y2;
            }
        }
        
        gl.glBegin(GL.GL_POLYGON);
        gl.glVertex2i((int)xPoints[0], -(int)yPoints[0]);
        gl.glVertex2i((int)xPoints[1], -(int)yPoints[1]);
        gl.glVertex2i((int)xPoints[2], -(int)yPoints[2]);
        gl.glVertex2i((int)xPoints[3], -(int)yPoints[3]);
        gl.glVertex2i((int)xPoints[4], -(int)yPoints[4]);
        gl.glVertex2i((int)xPoints[5], -(int)yPoints[5]);
        gl.glEnd();
    }
//}}}

//{{{ setFont, getLabelWidth/Ascent/Descent
//##############################################################################
    public void setFont(Font f)
    {
        int sz = f.getSize();
        if(sz <= 10)        currFont = GLUT.BITMAP_HELVETICA_10;
        else if(sz <= 14)   currFont = GLUT.BITMAP_HELVETICA_12;
        else                currFont = GLUT.BITMAP_HELVETICA_18;
    }
    
    public int getLabelWidth(String s)
    { return glut.glutBitmapLength(currFont, s); }
    public int getLabelAscent(String s)
    {
        if(currFont == GLUT.BITMAP_HELVETICA_10)        return 10;
        else if(currFont == GLUT.BITMAP_HELVETICA_12)   return 12;
        else if(currFont == GLUT.BITMAP_HELVETICA_18)   return 18;
        else                                            return 1;
    }
    public int getLabelDescent(String s)
    { return getLabelAscent(s)/4; }
//}}}

//{{{ setPaint, setViewport, clearCanvas
//##############################################################################
    void setPaint(Paint p)
    {
        try
        {
            Color c = (Color) p;
            gl.glColor4f( c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f);
        }
        catch(ClassCastException ex)
        {
            ex.printStackTrace();
            System.err.println("JoglPainter: tried painting with non-Color type of Paint");
        }
    }
    
    public void setViewport(int x, int y, int width, int height)
    {
        clipRgn.setBounds(x, y, width, height);
        
        gl.glMatrixMode(GL.GL_PROJECTION);  
        gl.glLoadIdentity(); 
        //glu.gluOrtho2D(0.0, width, -height, 0.0); 
        glu.gluOrtho2D(x, x+width, y-height, y); 
        gl.glViewport(x, y, width, height); 
    }
    
    public void clearCanvas(Color c)
    {
        gl.glClearColor(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

