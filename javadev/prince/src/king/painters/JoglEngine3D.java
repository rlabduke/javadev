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
* <code>JoglEngine3D</code> is a kinemage renderer that uses "real" OpenGL
* calls to render a 3-dimensional image of the scene.
* It does not support all possible options in the kinemage format, so it
* should only be used in cases where it has to be (e.g. a CAVE setting).
*
* <p>Features that are not currently supported:
* <ul>
* <li>Some types of points (arrows, markers, rings -- rendered as dots)</li>
* <li>Depth cueing by line width</li>
* <li>Translucency</li>
* <li>Instanced lists (instance=)</li>
* <li>Per-group or per-list transformations</li>
* <li></li>
* <li>... other things I've forgotten ...</li>
* </ul>
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
    /** Location of the screen center in some arbitrary world frame, measured in pixels (1/72" for most displays) */
    public Triple       screenCenterPos     = new Triple(0, 0, 0);
    /** Unit vector defining the screen normal; points TOWARD the observer */
    public Triple       screenNormalVec     = new Triple(0, 0, 1);
    /** Unit vector defining the screen / world "up"; needs not be orthogonal to screen normal but should be close */
    public Triple       screenUpVec         = new Triple(0, 1, 0);
    
    /** If set, clipping volume always starts very close to the eye, instead of around the screen */
    public boolean      caveClipping = false;

    protected GL        gl;
    protected GLU       glu;
    protected GLUT      glut;
    protected float[]   clearColor;
    protected int       currFont = GLUT.BITMAP_HELVETICA_12;
    
    protected Triple[]  icosVerts;
    protected int[][]   icosFaces;
    
    protected int       ballDL = 0; // display lists for rendering balls
//}}}

//{{{ Constructor(s), cleanup
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
    
    /** Please call this before discarding the Engine object to release OpenGL resources! */
    public void cleanup(GL gl)
    {
        // clean up display lists
        if(ballDL != 0)
        {
            gl.glDeleteLists(ballDL, 1);
            ballDL = 0;
        }
    }
//}}}

//{{{ render
//##################################################################################################
    /** Renders with the observer at (0, 0, perspDist) -- perfect for viewing the default screen. */
    public void render(AGE xformable, KView view, Rectangle bounds, GL gl)
    { render(xformable, view, bounds, gl, new Triple(0, 0, perspDist)); }

    /**
    * Transforms the given Transformable and renders it to a graphics context.
    * @param xformable      the Transformable that will be transformed and rendered
    * @param view           a KView representing the current rotation/zoom/clip
    * @param bounds         the bounds of the area to render to.
    * @param gl             the OpenGL graphics context
    * @param eyePosition    location of the observer's eyeball in the arbitrary world frame,
    *   measured in pixels (1/72" for most displays).  Interacts with screen position/orientation.
    */
    public void render(AGE xformable, KView view, Rectangle bounds, GL gl, Tuple3 eyePosition)
    {
        // init GL
        this.gl     = gl;
        this.glu    = new GLU();
        this.glut   = new GLUT();
        
        // Display lists DO hold from one render pass to another
        //this.ballDL = 0;
        
        if(whiteBackground) clearColor = new float[] {1, 1, 1, 1};
        else                clearColor = new float[] {0, 0, 0, 1};
        
        // This is necessary for antialiasing, but also for transparent objects.
        gl.glEnable(gl.GL_BLEND);
        gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);

        // Antialiasing for points and lines.
        // Polygons have to be antialiased with full-scene AA.
        gl.glEnable(gl.GL_POINT_SMOOTH);
        gl.glEnable(gl.GL_LINE_SMOOTH);
        
        // Projection and model-view matrix, and fog
        Transform R = setupTransforms(view, bounds, eyePosition);

        // Lighting for spheres, ribbons, etc
        setupLighting(R);

        // Clear background
        // This must happen AFTER the viewport is set in setupTransforms
        // Actually, for some reason this clears the whole screen, not just my viewport
        gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        
        for(KList list : KIterator.visibleLists(xformable))
        {
            String type = list.getType();
            if(KList.VECTOR.equals(type))           doVectorList(list);
            else if(KList.DOT.equals(type))         doDotList(list);
            else if(KList.RIBBON.equals(type))      doTriangleList(list, true);
            else if(KList.BALL.equals(type))        doBallList(list);
            else if(KList.SPHERE.equals(type))      doBallList(list);
            else if(KList.TRIANGLE.equals(type))    doTriangleList(list, false);
            else if(KList.MARK.equals(type))        doDotList(list);
            else if(KList.LABEL.equals(type))       doLabelList(list);
            else if(KList.RING.equals(type))        doDotList(list);
            else if(KList.ARROW.equals(type))       doDotList(list);
            else                                    doDotList(list);
        }
        
        // Cleaning up and re-allocating display lists all the time
        // slows the app down a LOT after a while.
        // I can't tell if it's Java memory churn or OpenGL, but the effect is big.
        // Now this is handled in the cleanup() function.
        //
        // clean up display lists
        //if(ballDL != 0) gl.glDeleteLists(ballDL, 1);
    }    
//}}}

//{{{ setupLighting
//##############################################################################
    protected void setupLighting(Transform R)
    {
        // Direction is relative to transformed coordinate space in which observer is defined.
        // We don't want the light rotating with the model!
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        // If we've rotated the world for a nonstandard screen position,
        // we need to move the light as well.
        gl.glMultMatrixd(new double[] {
            R.get(1,1), R.get(2,1), R.get(3,1), R.get(4,1),
            R.get(1,2), R.get(2,2), R.get(3,2), R.get(4,2),
            R.get(1,3), R.get(2,3), R.get(3,3), R.get(4,3),
            R.get(1,4), R.get(2,4), R.get(3,4), R.get(4,4)
            }, 0);
        // Lighting is enabled only for specific objects, because lines and points
        // only are affected by ambient lighting components.
        //gl.glEnable(GL.GL_LIGHTING);
        // Correctly scaled normals are essential for lighting!
        gl.glEnable(GL.GL_NORMALIZE);
        gl.glEnable(GL.GL_LIGHT0);
        
        float I = 1.0f;        // overal intensity
        float a = 0.3f * I;     // ambient
        float d = 0.8f * I;     // diffuse
        float s = 0.0f * I;     // specular (doesn't seem to work?)
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] {a, a, a, 1}, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, new float[] {d, d, d, 1}, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, new float[] {s, s, s, 1}, 0);

        // Directional light source; vector indicates light direction.
        Triple lv = this.lightingVector;
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION,
            new float[] {-(float)lv.getX(), -(float)lv.getY(), -(float)lv.getZ(), 0}, 0);
        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, new float[] {0, 0, 0, 1}, 0); // default is ???
        gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_FALSE); // supposedly more efficient
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE); // will be needed for ribbons!
        
        // Base material
        gl.glEnable(GL.GL_COLOR_MATERIAL); // color will be taken from color statements
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE); // must be enabled to work!
        //gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, new float[] {1,0,0,1}, 0);
        //gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, new float[] {1,0,0,1}, 0);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, new float[] {1,1,1,1}, 0);
        gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, 80f);
        gl.glPopMatrix();
    }
//}}}

//{{{ NOTES ON THE COORDINATE SYSTEM
/*##############################################################################
KiNG uses a right-handed coordinate system, like Mage.
If +X is right and +Y is up, then +Z is toward you and -Z is into the screen.
OpenGL also uses a right-handed coordinate system (thankfully).

The result of all the usual transformations in KiNG is to leave
the center of view at the origin, scaled so that 1 unit = 1 pixel. 
The observer is on the +Z axis, about 2000 pixels (~27") away from the origin.

In OpenGL, the observer is always at the origin, and the view is always down
the -Z axis (same direction as KiNG expects, actually, just translated in Z).
To simulate any other arrangement, the model should be transformed appropriately.
(Transforming the projection matrix instead is possible but causes artifacts.)

Scheme for rendering in the DiVE:
(do these in reverse order as the "first" (last applied) components of the modelview matrix)
    center on the observer
    rotate the screen to the front
        one Triple per edge
        can use these to calculate top,bottom,left,right for glFrustum()
        each screen defines a coordinate frame (right, up, front vectors)
        these make a rotation matrix; default screen gives identity matrix
        either this matrix or its transpose (=inverse) is our desired rotation
            desired rotation matrix is:
            [ rightX  rightY  rightZ ]
            [    upX     upY     upZ ]
            [ frontX  frontY  frontZ ]

- if observer is null, observer = (0, 0, perspDist)
- default screen is at (0,0,0) with up (0,1,0), right (1,0,0), and front (0,0,1)
}}}*/

//{{{ setupTransforms
//##################################################################################################
    /** Returns the screen positioning transform (needed for light positioning) */
    protected Transform setupTransforms(KView view, Rectangle bounds, Tuple3 eyePosition)
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

        /*{{{ The original code (which works for the standard observer, standard screen)
        // Projection matrix
        // Goal is to transform visible points into the cube from (-1,-1,-1) to (+1,+1,+1)
        // Anything outside that cube after applying this matrix gets clipped!
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
        // Can do other view translation/rotation here, but it screws up the lighting!
        // Do it at the "end" of the model-view sequence instead (i.e. first calls)
        }}}*/
        
        /*{{{ The Syzygy way -- this sucks -- too hard to understand
        double[] matrix = new double[16];
        gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, matrix, 0);
        printMatrix("Standard", matrix);
        // This one works, but is weird (normal, clipping)
        //matrix = ar_frustumMatrix(new Triple(0, 0, -perspDist), new Triple(0,0,-1), new Triple(0,1,0), new Triple(0,0,0),
        //    width/2, height/2, near, -viewClipScaling*viewClipBack);
        // Equivalent to the one above:
        //matrix = ar_frustumMatrix(new Triple(0, 0, 0), new Triple(0,0,-1), new Triple(0,1,0), new Triple(0, 0, perspDist),
        //    width/2, height/2, near, -viewClipScaling*viewClipBack);
        matrix = ar_frustumMatrix(new Triple(0, 0, 0), new Triple(-1,0,0), new Triple(0,1,0), new Triple(perspDist, 0, 0),
            width/2, height/2, near, -viewClipScaling*viewClipBack);
        printMatrix("Syzygy", matrix);
        gl.glLoadMatrixd(matrix, 0);
        }}}*/

        // Projection matrix
        // Goal is to transform visible points into the cube from (-1,-1,-1) to (+1,+1,+1)
        // Anything outside that cube after applying this matrix gets clipped!
        gl.glMatrixMode(GL.GL_PROJECTION);  
        gl.glLoadIdentity();
        // Calculate the unit vectors that form a local coordinate frame for the screen.
        // We want the rotation that will bring them to match the default screen,
        // because OpenGl always has the observer at the origin looking down -Z.
        // These are the screen normal (z), right (x), and up (y) vectors.
        Triple zHat = new Triple(this.screenNormalVec).unit();
        // Results will be nonsensical if screen normal is pointed away from the observer.
        // That should mean s/he shouldn't be able to see it anyway, but just in case,
        // we'll render on the back side of the screen.
        Triple scrToEye = new Triple().likeVector(screenCenterPos, eyePosition);
        if(scrToEye.dot(zHat) < 0) zHat.neg();
        // Build orthogonal up and right vectors via cross products
        Triple xHat = new Triple(this.screenUpVec).cross(zHat).unit();
        Triple yHat = new Triple(zHat).cross(xHat);
        // Check for yourself: R*xHat = (1,0,0); R*yHat = (0,1,0); R*zHat = (0,0,1)
        Transform R = new Transform().likeMatrix(
            xHat.getX(), xHat.getY(), xHat.getZ(),
            yHat.getX(), yHat.getY(), yHat.getZ(),
            zHat.getX(), zHat.getY(), zHat.getZ());
    //System.err.println(R.transform(xHat, new Triple()));
    //System.err.println(R.transform(yHat, new Triple()));
    //System.err.println(R.transform(zHat, new Triple()));
        // Rotation should be centered around the observer
        Triple negEyePos = new Triple(eyePosition).neg();
        R.prepend( new Transform().likeTranslation(negEyePos) );
    //System.err.println(R);
        // Find position of screen center in the new scheme.
        // Screen Z < 0 and screen is normal to Z axis, but may be shifted to side or up/down.
        Triple scrCtr = (Triple) R.transform(this.screenCenterPos, new Triple());
        final double minClipDist = size/20;
        double scrDistZ     = (-scrCtr.getZ());
        double near         = Math.max(scrDistZ - viewClipScaling*viewClipFront, minClipDist);
        double far          = Math.max(scrDistZ - viewClipScaling*viewClipBack, 2*minClipDist);
        double right        = (scrCtr.getX() + (width  / 2)) * (near / scrDistZ);
        double left         = (scrCtr.getX() - (width  / 2)) * (near / scrDistZ);
        double top          = (scrCtr.getY() + (height / 2)) * (near / scrDistZ);
        double bottom       = (scrCtr.getY() - (height / 2)) * (near / scrDistZ);
        if(caveClipping)
        {
            // Slide the clipping planes forward to very near the observer.
            // Can't be AT the observer because transform is undef.
            double offset = near - minClipDist;
            near -= offset;
            far  -= offset;
        }
    //System.err.println("scrCtr = "+scrCtr);
    //System.err.println("top = "+top+"; bottom = "+bottom);
    //System.err.println("left = "+left+"; right = "+right);
    //System.err.println("near = "+near+"; far = "+far);
        // l,r,b,t apply at distance "near" and are X,Y coords
        // l,r,b,t apply at distance "near" and are X,Y coords
        // near and far are (positive) distances from the camera for glFrustum
        // but are Z coords for glOrtho (although they're then negated!).
        if(usePerspective)  gl.glFrustum(left, right, bottom, top, near, far);
        else                gl.glOrtho(left, right, bottom, top, near, far);
        // Can do other view translation/rotation here, but it screws up the lighting!
        // Do it at the "end" of the model-view sequence instead (i.e. first calls)
    //double[] matrix = new double[16];
    //gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, matrix, 0);
    //printMatrix("Standard", matrix);
        
        // Model-view matrix
        // Transforms are applied to the points in the REVERSE of the order specified.
        // i.e. glMultMatrixd() is a post-multiply operation.
        // It is VERY IMPORTANT that we leave the model-view matrix as the active one!
        gl.glMatrixMode(GL.GL_MODELVIEW);  
        gl.glLoadIdentity();
        // Move the world like we moved the screen, so we see what was behind it.
        // Remember OpenGL lists elements top-to-bottom, then left-to-right.
        gl.glMultMatrixd(new double[] {
            R.get(1,1), R.get(2,1), R.get(3,1), R.get(4,1),
            R.get(1,2), R.get(2,2), R.get(3,2), R.get(4,2),
            R.get(1,3), R.get(2,3), R.get(3,3), R.get(4,3),
            R.get(1,4), R.get(2,4), R.get(3,4), R.get(4,4)
            }, 0);
        //gl.glTranslated(0, 0, -perspDist); // move camera back to viewing position
        // rotate: down the first col, then down the second col, etc.
        gl.glMultMatrixd(new double[] {
            R11, R21, R31, 0,
            R12, R22, R32, 0,
            R13, R23, R33, 0,
            0,   0,   0,   1
            }, 0);
        gl.glScaled(zoom3D, zoom3D, zoom3D); // scale
        gl.glTranslated(-cx, -cy, -cz); // center
        
        // Fog
        gl.glEnable(GL.GL_FOG);
        gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
        gl.glFogf(GL.GL_FOG_START, (float) near);
        double farFog = (far - 0.36*near) / (1 - 0.36);
        gl.glFogf(GL.GL_FOG_END, (float) (farFog));
        gl.glFogfv(GL.GL_FOG_COLOR, clearColor, 0);
        
        return R;
    }
    
    private void printMatrix(String label, double[] m)
    {
        System.err.println(label);
        System.err.printf("%10g %10g %10g %10g \n", m[0], m[4], m[ 8], m[12]);
        System.err.printf("%10g %10g %10g %10g \n", m[1], m[5], m[ 9], m[13]);
        System.err.printf("%10g %10g %10g %10g \n", m[2], m[6], m[10], m[14]);
        System.err.printf("%10g %10g %10g %10g \n", m[3], m[7], m[11], m[15]);
    }
//}}}

//{{{ ar_frustumMatrix [NOT USED]
//##############################################################################
    /**
    * I believe that all units are in the transformed space,
    * which makes them pixels in this case.
    * On a standard display, 72 pixels = 1 inch.
    *
    * This really only cares about the angle between the screenNormal and the
    * screenCenter-eyePosition vector.  Putting the screen at a nominal location
    * of (1,0,0) vs. (0,0,1) doesn't actually rotate the object to view from
    * a different side as one might expect.
    *
    * @param screenCenter   (what it sounds like)
    * @param screenNormal   unit vector, e.g. (0,0,1), points AWAY from viewer
    * @param screenUp       unit vector, e.g. (0,1,0)
    * @param eyePosition    (what it sounds like)
    * @param halfWidth      of the screen surface
    * @param halfHeight     of the screen surface
    * @param nearClip       positive distance from eyeball to front clipping plane
    * @param farClip        positive distance from screen to back clipping plane
    *
    * Taken from Syzygy (BSD license), /src/math/arMath.cpp
    *
    * The derivation of this matrix (as produced by glFrustum()) is also listed
    * in Appendix G of the OpenGL Programming Guide!
    */
    protected double[] ar_frustumMatrix(
        Triple screenCenter, Triple screenNormal, Triple screenUp, Triple eyePosition, 
        double halfWidth,  double halfHeight, double nearClip,  double farClip)
    {
        /// copypaste start
        if(screenNormal.mag() <= 0)
            return null; // error
        final Triple zHat = new Triple(screenNormal).unit();
        final Triple xHat = new Triple(zHat).cross(screenUp).unit();
        final Triple yHat = new Triple(xHat).cross(zHat);
        /// copypaste end
        
        final Triple rightEdge  = new Triple(screenCenter).addMult( halfWidth, xHat);
        final Triple leftEdge   = new Triple(screenCenter).addMult(-halfWidth, xHat);
        final Triple topEdge    = new Triple(screenCenter).addMult( halfHeight, yHat);
        final Triple botEdge    = new Triple(screenCenter).addMult(-halfHeight, yHat);
        
        // double zEye = (eyePosition - headPosition) % zHat; // '%' = dot product
        double screenDistance = new Triple(screenCenter).sub(eyePosition).dot(zHat);
        if (screenDistance == 0)
            return null; // error
        
        final double nearFrust  = nearClip;
        final double distScale  = nearFrust / screenDistance;
        final double rightFrust = distScale*(new Triple(rightEdge).sub(eyePosition).dot(xHat));
        final double leftFrust  = distScale*(new Triple(leftEdge ).sub(eyePosition).dot(xHat));
        final double topFrust   = distScale*(new Triple(topEdge  ).sub(eyePosition).dot(yHat));
        final double botFrust   = distScale*(new Triple(botEdge  ).sub(eyePosition).dot(yHat));
        final double farFrust   = screenDistance + farClip;
        
        if (rightFrust == leftFrust || topFrust == botFrust || nearFrust == farFrust)
            return null; // error
        
        // this is necessary because g++ 2.96 is messed up.
        //double funnyElement = (nearFrust+farFrust)/(nearFrust-farFrust);
        //double[] result = new double[] {
        //(2*nearFrust)/(rightFrust-leftFrust),   0,                                  (rightFrust+leftFrust)/(rightFrust-leftFrust),  0,
        //0,                                      (2*nearFrust)/(topFrust-botFrust),  (topFrust+botFrust)/(topFrust-botFrust),        0,
        //0,                                      0,                                  funnyElement,                                   2*nearFrust*farFrust/(nearFrust-farFrust),
        //0,                                      0,                                  -1,                                             0 };
        
        // OpenGL order is transposed vs. what any normal person would do.
        // Coordinate system handedness change may require inverting sign on off-diagonal elements of third row and third column (?)
        double[] result = new double[] {
        (2*nearFrust)/(rightFrust-leftFrust),           0,                                          0,                                          0,
        0,                                              (2*nearFrust)/(topFrust-botFrust),          0,                                          0,
        (rightFrust+leftFrust)/(rightFrust-leftFrust),  (topFrust+botFrust)/(topFrust-botFrust),    (nearFrust+farFrust)/(nearFrust-farFrust),  -1,
        0,                                              0,                                          2*nearFrust*farFrust/(nearFrust-farFrust),  0 };
        return result;
    }
//}}}

//{{{ doVectorList
//##############################################################################
    protected void doVectorList(KList list)
    {
        KPaint currColor = list.getColor();
        if(currColor.isInvisible()) return;
        setPaint(currColor);
        int currWidth = list.getWidth();
        // The +0.5 makes it closer to other KiNG rendering modes (?)
        gl.glLineWidth(currWidth+0.5f);
        gl.glBegin(GL.GL_LINE_STRIP);
        for(KPoint p : list.getChildren())
        {
            if(p.isBreak())
            {
                gl.glEnd();
                gl.glBegin(GL.GL_LINE_STRIP);
            }
            
            KPaint ptColor = p.getDrawingColor(this);
            if(ptColor.isInvisible()) continue;
            // Supposedly it's good to minimize the number of color changes
            if(ptColor != currColor)
            {
                setPaint(ptColor);
                currColor = ptColor;
            }
            
            int ptWidth = calcLineWidth(p, list);
            if(ptWidth != currWidth)
            {
                gl.glLineWidth(ptWidth+0.5f);
                currWidth = ptWidth;
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
        KPaint currColor = list.getColor();
        if(currColor.isInvisible()) return;
        setPaint(currColor);
        int currWidth = list.getWidth();
        // The +0.5 makes it closer to other KiNG rendering modes (?)
        gl.glLineWidth(currWidth+0.5f);
        gl.glBegin(GL.GL_POINTS);
        for(KPoint p : list.getChildren())
        {
            KPaint ptColor = p.getDrawingColor(this);
            if(ptColor.isInvisible()) continue;
            // Supposedly it's good to minimize the number of color changes
            if(ptColor != currColor)
            {
                setPaint(ptColor);
                currColor = ptColor;
            }
            
            int ptWidth = calcLineWidth(p, list);
            if(ptWidth != currWidth)
            {
                gl.glLineWidth(ptWidth+0.5f);
                currWidth = ptWidth;
            }
            
            gl.glVertex3d(p.getX(), p.getY(), p.getZ());
        }
        gl.glEnd();
    }
//}}}

//{{{ doBallList
//##############################################################################
    protected void doBallList(KList list)
    {
        // Radius cutoffs in pixels for different levels of detail
        final double r1 = 4.0 / zoom3D, r2 = 12.0 / zoom3D, r3 = 128.0 / zoom3D;
        
        KPaint currColor = list.getColor();
        if(currColor.isInvisible()) return;
        setPaint(currColor);
        double listRadius = list.getRadius();
        for(KPoint p : list.getChildren())
        {
            KPaint ptColor = p.getDrawingColor(this);
            if(ptColor.isInvisible()) continue;
            // Supposedly it's good to minimize the number of color changes
            if(ptColor != currColor)
            {
                setPaint(ptColor);
                currColor = ptColor;
            }
            
            double radius = p.getRadius();
            if(radius == 0) radius = listRadius;
            
            gl.glPushMatrix();
            gl.glTranslated(p.getX(), p.getY(), p.getZ());
            gl.glScaled(radius, radius, radius);
            if(radius <= r1)        drawSphere(0);
            else if(radius <= r2)   drawSphere(1);
            else if(radius <= r3)   drawSphere(2);
            else                    drawSphere(3);
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
                    gl.glEnable(GL.GL_LIGHTING); // lines and points aren't lit
                    gl.glBegin(GL.GL_TRIANGLES);
                    for(int[] face : icosFaces)
                        drawSphereFace(icosVerts[face[0]], icosVerts[face[1]], icosVerts[face[2]], i);
                    gl.glEnd();
                    gl.glDisable(GL.GL_LIGHTING);
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
            gl.glEnable(GL.GL_LIGHTING); // lines and points aren't lit
            gl.glBegin(GL.GL_TRIANGLES);
            for(int[] face : icosFaces)
                drawSphereFace(icosVerts[face[0]], icosVerts[face[1]], icosVerts[face[2]], depth);
            gl.glEnd();
            gl.glDisable(GL.GL_LIGHTING);
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

//{{{ doLabelList
//##############################################################################
    protected void doLabelList(KList list)
    {
        KPaint currColor = list.getColor();
        if(currColor.isInvisible()) return;
        setPaint(currColor);
        for(KPoint p : list.getChildren())
        {
            KPaint ptColor = p.getDrawingColor(this);
            if(ptColor.isInvisible()) continue;
            // Supposedly it's good to minimize the number of color changes
            if(ptColor != currColor)
            {
                setPaint(ptColor);
                currColor = ptColor;
            }
            
            gl.glRasterPos3d(p.getX(), p.getY(), p.getZ());
            glut.glutBitmapString(currFont, p.getName());
        }
    }
//}}}

//{{{ doTriangleList
//##############################################################################
    protected void doTriangleList(KList list, boolean ribbonNormals)
    {
        KPaint currColor = list.getColor();
        if(currColor.isInvisible()) return;
        setPaint(currColor);
        gl.glEnable(GL.GL_LIGHTING); // lines and points aren't lit
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        KPoint from = null, fromfrom = null;
        boolean flipNormal = false; // to get consistent normals -- not really needed
        for(KPoint p : list.getChildren())
        {
            if(p.isBreak())
            {
                gl.glEnd();
                from = fromfrom = null;
                flipNormal = false;
                gl.glBegin(GL.GL_TRIANGLE_STRIP);
            }
            
            KPaint ptColor = p.getDrawingColor(this);
            if(ptColor.isInvisible()) continue;
            // Supposedly it's good to minimize the number of color changes
            if(ptColor != currColor)
            {
                setPaint(ptColor);
                currColor = ptColor;
            }
            
            if(from != null && fromfrom != null)
            {
                work2.likeVector(fromfrom, from);
                work1.likeVector(from, p);
                work1.cross(work2);
                // not normalized, but has to be re-normalized by OpenGL anyway
                if(flipNormal)
                {
                    if(!ribbonNormals) gl.glNormal3d(-work1.getX(), -work1.getY(), -work1.getZ());
                    // for ribbons, just use the previous normal on every other triangle 
                }
                else gl.glNormal3d(work1.getX(), work1.getY(), work1.getZ());
            }
            
            gl.glVertex3d(p.getX(), p.getY(), p.getZ());
            
            fromfrom = from;
            from = p;
            flipNormal = !flipNormal;
        }
        gl.glEnd();
        gl.glDisable(GL.GL_LIGHTING);
    }
//}}}

//{{{ setPaint, calcLineWidth
//##############################################################################
    protected void setPaint(KPaint p)
    {
        try
        {
            Color c = (Color) (whiteBackground ? p.getWhiteExemplar() : p.getBlackExemplar());
            gl.glColor4f( c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f );
        }
        catch(ClassCastException ex)
        {
            ex.printStackTrace();
            System.err.println("JoglPainter: tried painting with non-Color type of Paint");
        }
    }
    
    protected int calcLineWidth(KPoint point, KList parent)
    {
        int wid = point.getWidth();
        if(this.thinLines)      return 1;
        else if(wid > 0)        return wid;
        else if(parent != null) return parent.getWidth();
        else                    return 2;
    }

//}}}

//{{{ setFont, getLabelWidth/Ascent/Descent
//##############################################################################
    public void setFont(Font f)
    { setFont(f.getSize()); }
    
    public void setFont(int sz)
    {
        if(sz <= 10)        currFont = GLUT.BITMAP_HELVETICA_10;
        else if(sz <= 14)   currFont = GLUT.BITMAP_HELVETICA_12;
        else                currFont = GLUT.BITMAP_HELVETICA_18;
    }
    
    protected int getLabelWidth(String s)
    { return glut.glutBitmapLength(currFont, s); }
    
    protected int getLabelAscent(String s)
    {
        if(currFont == GLUT.BITMAP_HELVETICA_10)        return 10;
        else if(currFont == GLUT.BITMAP_HELVETICA_12)   return 12;
        else if(currFont == GLUT.BITMAP_HELVETICA_18)   return 18;
        else                                            return 1;
    }
    
    protected int getLabelDescent(String s)
    { return getLabelAscent(s)/4; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

