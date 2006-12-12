// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.painters;
import king.core.*;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*; // for GLUT
//}}}
/**
* <code>JoglEngine3D</code> has not yet been documented.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Dec 12 09:21:44 EST 2006
*/
public class JoglEngine3D extends Engine
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    protected GL        gl;
    protected GLU       glu;
    protected GLUT      glut;
    protected float[]   clearColor;
    
    protected Triple[]  icosVerts;
    protected int[][]   icosFaces;
    
    protected int       ballDL; // display lists for rendering balls
//}}}

//{{{ Constructor(s)
//##############################################################################
    public JoglEngine3D()
    {
        super();
        
        // From OpenGL Programming Guide, Ch. 2, pg. 56
        // Basic unit icosahedron for approximating a sphere
        final double x = 0.525731112119133606;
        final double z = 0.850650808352039932;
        this.icosVerts = new Triple[] {
            new Triple(-x,0,z), new Triple(x,0,z), new Triple(-x,0,-z), new Triple(x,0,-z),
            new Triple(0,z,x), new Triple(0,z,-x), new Triple(0,-z,x), new Triple(0,-z,-x),
            new Triple(z,x,0), new Triple(-z,x,0), new Triple(z,-x,0), new Triple(-z,-x,0)
        };
        this.icosFaces = new int[][] {
            {0,4,1}, {0,9,4}, {9,5,4}, {4,5,8}, {4,8,1},
            {8,10,1}, {8,3,10}, {5,3,8}, {5,2,3}, {2,7,3},
            {7,10,3}, {7,6,10}, {7,11,6}, {11,0,6}, {0,1,6},
            {6,1,10}, {9,0,11}, {9,11,2}, {9,2,5}, {7,2,11}
        };
    }
//}}}

//{{{ render
//##################################################################################################
    /**
    * Transforms the given Transformable and renders it to a graphics context.
    * @param xformable      the Transformable that will be transformed and rendered
    * @param view           a KView representing the current rotation/zoom/clip
    * @param bounds         the bounds of the area to render to.
    */
    public void render(AGE xformable, KView view, Rectangle bounds, GL gl)
    {
        // init GL
        this.gl     = gl;
        this.glu    = new GLU();
        this.glut   = new GLUT();
        
        // I don't think display lists hold from one render pass to another
        this.ballDL = 0;
        
        if(whiteBackground) clearColor = new float[] {1, 1, 1, 1};
        else                clearColor = new float[] {0, 0, 0, 1};
        
        // This is necessary for antialiasing, but also for transparent objects.
        gl.glEnable(gl.GL_BLEND);
        gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);

        // Antialiasing for points and lines.
        // Polygons have to be antialiased with full-scene AA.
        gl.glEnable(gl.GL_POINT_SMOOTH);
        gl.glEnable(gl.GL_LINE_SMOOTH);
        

        // Lighting for spheres, ribbons, etc
        // Direction is relative to transformed coordinate space in which observer is defined.
        // We don't want the light rotating with the model!
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        // Directional light source, such that rays from given position shine thru the origin.
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] {0.5f, 0.5f, 0.5f, 1}, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, new float[] {4, 4, 4, 1}, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, new float[] {10, 10, 10, 10}, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[] {-(float)lightingVector.getX(), -(float)lightingVector.getY(), -(float)lightingVector.getZ(), 0}, 0);
        //gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, new float[] {0.2f, 0.2f, 0.2f, 1}, 0);
        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, new float[] {0, 0, 0, 1}, 0);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_FALSE);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE); // will be needed for ribbons!
        // Base material
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, new float[] {1,0,0,1}, 0);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, new float[] {1,0,0,1}, 0);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, new float[] {1,1,1,1}, 0);
        gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, 127f);
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE); // seems not to work?
        gl.glEnable(GL.GL_COLOR_MATERIAL);
        gl.glPopMatrix();

        // Projection and model-view matrix, and fog
        setupTransforms(view, bounds);

        // Clear background
        gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        
        for(KList list : KIterator.visibleLists(xformable))
        {
            String type = list.getType();
            if(KList.VECTOR.equals(type))           doVectorList(list);
            else if(KList.DOT.equals(type))         doDotList(list);
            else if(KList.RIBBON.equals(type))      doDotList(list);
            else if(KList.BALL.equals(type))        doBallList(list);
            else if(KList.SPHERE.equals(type))      doBallList(list);
            else if(KList.TRIANGLE.equals(type))    doDotList(list);
            else if(KList.MARK.equals(type))        doDotList(list);
            else if(KList.LABEL.equals(type))       doDotList(list);
            else if(KList.RING.equals(type))        doDotList(list);
            else if(KList.ARROW.equals(type))       doDotList(list);
            else                                    doDotList(list);
        }
        
        // clean up GL
        if(ballDL != 0) gl.glDeleteLists(ballDL, 1);
    }    
//}}}

//{{{ setupTransforms
//##################################################################################################
    protected void setupTransforms(KView view, Rectangle bounds)
    {
        double width, height, size, xOff, yOff;
        width   = bounds.getWidth();
        height  = bounds.getHeight();
        size    = Math.min(width, height);
        xOff    = bounds.getX() + width/2.0;
        yOff    = bounds.getY() + height/2.0;
        this.viewClipScaling = size/2.0;
        
        // Get information from the current view
        double cx, cy, cz, R11, R12, R13, R21, R22, R23, R31, R32, R33;
        synchronized(view)
        {
            view.compile();
            zoom3D      = size / view.getSpan();
            cx          = view.cx;
            cy          = view.cy;
            cz          = view.cz;
            R11         = view.R11;
            R12         = view.R12;
            R13         = view.R13;
            R21         = view.R21;
            R22         = view.R22;
            R23         = view.R23;
            R31         = view.R31;
            R32         = view.R32;
            R33         = view.R33;
            viewClipFront = view.getClip();
            viewClipBack = -viewClipFront;
        }
        
        // Viewport is independent of anything else -- it just specifies
        // where within the window (pixel coords) the image will be drawn.
        gl.glViewport(bounds.x, bounds.y, bounds.width, bounds.height);

        // Projection matrix
        double near     = perspDist - viewClipScaling*viewClipFront;
        double far      = perspDist - viewClipScaling*viewClipBack;
        double right    = (width/2) * (near / perspDist);
        double left     = -right;
        double top      = (height/2) * (near / perspDist);
        double bottom   = -top;
        gl.glMatrixMode(GL.GL_PROJECTION);  
        gl.glLoadIdentity();
        // l,r,b,t apply at distance "near" and are X,Y coords
        // near and far are (positive) distances from the camera for glFrustum
        // but are Z coords for glOrtho (although they're then negated!).
        if(usePerspective)  gl.glFrustum(left, right, bottom, top, near, far);
        else                gl.glOrtho(left, right, bottom, top, near, far);

        // Model-view matrix
        // Transforms are applied to the points in the REVERSE of the order specified.
        // i.e. glMultMatrixd() is a post-multiply operation.
        // It is VERY IMPORTANT that we leave the model-view matrix as the active one!
        gl.glMatrixMode(GL.GL_MODELVIEW);  
        gl.glLoadIdentity();
        gl.glTranslated(0, 0, -perspDist); // move camera back to viewing position
        // rotate: down the first col, then down the second col, etc.
        gl.glMultMatrixd(new double[] {R11, R21, R31, 0, R12, R22, R32, 0, R13, R23, R33, 0,   0, 0, 0, 1}, 0);
        gl.glScaled(zoom3D, zoom3D, zoom3D); // scale
        gl.glTranslated(-cx, -cy, -cz); // center
        
        // Fog
        gl.glEnable(GL.GL_FOG);
        gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
        gl.glFogf(GL.GL_FOG_START, (float) near);
        double farFog = (far - 0.36*near) / (1 - 0.36);
        gl.glFogf(GL.GL_FOG_END, (float) (farFog));
        gl.glFogfv(GL.GL_FOG_COLOR, clearColor, 0);
    }
//}}}

//{{{ doVectorList
//##############################################################################
    protected void doVectorList(KList list)
    {
        KPaint listColor = list.getColor();
        setPaint(listColor);
        // The +0.5 makes it closer to other KiNG rendering modes
        gl.glLineWidth(list.getWidth()+0.5f);
        gl.glBegin(GL.GL_LINE_STRIP);
        for(KPoint p : list.getChildren())
        {
            if(p.isBreak())
            {
                gl.glEnd();
                gl.glBegin(GL.GL_LINE_STRIP);
            }
            gl.glVertex3d(p.getX(), p.getY(), p.getZ());
        }
        gl.glEnd();
    }
//}}}

//{{{ doDotList
//##############################################################################
    protected void doDotList(KList list)
    {
        KPaint listColor = list.getColor();
        setPaint(listColor);
        // The +0.5 makes it closer to other KiNG rendering modes
        gl.glPointSize(list.getWidth()+0.5f);
        gl.glBegin(GL.GL_POINTS);
        for(KPoint p : list.getChildren())
        {
            gl.glVertex3d(p.getX(), p.getY(), p.getZ());
        }
        gl.glEnd();
    }
//}}}

//{{{ doBallList
//##############################################################################
    protected void doBallList(KList list)
    {
        KPaint listColor = list.getColor();
        setPaint(listColor);
        double radius = list.getRadius();
        for(KPoint p : list.getChildren())
        {
            gl.glPushMatrix();
            gl.glTranslated(p.getX(), p.getY(), p.getZ());
            gl.glScaled(radius, radius, radius);
            drawSphere(2);
            gl.glPopMatrix();
        }
    }
//}}}

//{{{ drawSphere, drawSphereFace
//##############################################################################
    /** Depth = 0 means 20 triangles, 1 is 80, 2 is 320, and 3 is 1280.  You shouldn't need more than that! */
    protected void drawSphere(int depth)
    {
        final int maxDepth = 3;
        // try to save sphere primitives as display lists
        if(ballDL == 0)
        {
            ballDL = gl.glGenLists(maxDepth+1);
            if(ballDL != 0)
            {
                for(int i = 0; i <= maxDepth; i++)
                {
                    gl.glNewList(ballDL+i, GL.GL_COMPILE);
                    gl.glBegin(GL.GL_TRIANGLES);
                    for(int[] face : icosFaces)
                        drawSphereFace(icosVerts[face[0]], icosVerts[face[1]], icosVerts[face[2]], i);
                    gl.glEnd();
                    gl.glEndList();
                }
            }
        }
        // use a display list if one is available
        if(ballDL != 0 && depth <= maxDepth)
            gl.glCallList(ballDL+depth);
        // else special case: render it the slow way
        else
        {
            gl.glBegin(GL.GL_TRIANGLES);
            for(int[] face : icosFaces)
                drawSphereFace(icosVerts[face[0]], icosVerts[face[1]], icosVerts[face[2]], depth);
            gl.glEnd();
        }
    }
    
    /** Draws the triangle if depth == 0, else subdivides depth times. */
    protected void drawSphereFace(Triple v1, Triple v2, Triple v3, int depth)
    {
        // From OpenGL Programming Guide, Ch. 2, pg. 56
        if(depth == 0)
        {
            // For a unit sphere, the normals are the same as the points!
            gl.glNormal3d(v1.getX(), v1.getY(), v1.getZ());
            gl.glVertex3d(v1.getX(), v1.getY(), v1.getZ());
            gl.glNormal3d(v2.getX(), v2.getY(), v2.getZ());
            gl.glVertex3d(v2.getX(), v2.getY(), v2.getZ());
            gl.glNormal3d(v3.getX(), v3.getY(), v3.getZ());
            gl.glVertex3d(v3.getX(), v3.getY(), v3.getZ());
        }
        else
        {
            Triple v12 = new Triple().likeMidpoint(v1, v2).unit();
            Triple v23 = new Triple().likeMidpoint(v2, v3).unit();
            Triple v31 = new Triple().likeMidpoint(v3, v1).unit();
            depth -= 1;
            drawSphereFace(v1, v12, v31, depth);
            drawSphereFace(v2, v23, v12, depth);
            drawSphereFace(v3, v31, v23, depth);
            drawSphereFace(v12, v23, v31, depth);
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ setPaint
//##############################################################################
    void setPaint(KPaint p)
    {
        try
        {
            Color c = (Color) (whiteBackground ? p.getWhiteExemplar() : p.getBlackExemplar());
            gl.glColor4f( c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f);
        }
        catch(ClassCastException ex)
        {
            ex.printStackTrace();
            System.err.println("JoglPainter: tried painting with non-Color type of Paint");
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

