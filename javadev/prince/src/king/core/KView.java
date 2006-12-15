// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.io.*;
//import java.text.*;
import java.util.*;
//}}}
/**
* <code>KView</code> holds a rotation matrix that specifies the current view or some saved view.
* Views are thread-safe, although most King components are not.
* This allows e.g. an auto-rocker to update the view in the background even if the event thread is repainting the window.
*
* <p>Zooms and clipping are specified such that a zoom factor of 1.0 means the object will fill the available
* display space without extending outside it, <em>regardless of its orientation</em>.
* A clipping value of 1.0 means that at a zoom of 1.0, no part of the object will be clipped.
*
* <p>Note that changing the rotation, zoom, etc. does NOT trigger a repaint of the graphics window --
* that is the responsibility of the caller.
*
* <p>Begun on Thu May 23 21:08:29 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class KView implements Serializable
{
//{{{ Static fields
    static final int N_UPDATES_ALLOWED = 100;
//}}}

//{{{ Variable definitions
//##################################################################################################
    /**
    * When serializing a view, the kinemage is excluded as it would contribute
    * a great deal of bulk to the serialization, when it might not be warranted.
    * If Kinemage itself ever becomes Serializable (which I doubt),
    * this might be the general pattern: parents are marked transient, and
    * upon de-serialization parents are responsible for calling setParent() on children.
    */
    transient protected Kinemage parent = null;
    
    // The label used on the View menu
    protected String ID = "Unnamed view";
    
    // The rotation matrix
    public float[][] xform  = { {1f, 0f, 0f}, {0f, 1f, 0f}, {0f, 0f, 1f} };
    // Coordinates (in real-space) of the center of rotation
    protected float[] center = null;
    
    /** The zooming factor */
    protected float zoom = 0f;
    /** The span factor, an alternate representation of zoom. */
    protected float span = 0f;
    /** The clipping factor */
    protected float clip = 1f;

    /** The elements of the transformation matrix, in real-space. Call compile() before using them! */
    public float R11 = 1f, R12 = 0f, R13 = 0f, R21 = 0f, R22 = 1f, R23 = 0f, R31 = 0f, R32 = 0f, R33 = 1f;
    /** The center of rotation, in real-space. Call compile() before using them! */
    public float cx = 0f, cy = 0f, cz = 0f;
    
    /** 0-based indices of which viewing axes to use when this view is activated. */
    protected int[] viewingAxes = null; // null -> don't change axes when selected
    
    // A counter for when this matrix needs to be 'cleaned'
    protected int nUpdates = 0;
//}}}

//{{{ Constructors, clone
//##################################################################################################
    /** Used by the serialization mechanism. */
    private KView() { super(); }
    
    /**
    * Constructor
    */
    public KView(Kinemage kin)
    {
        parent = kin;
    }

    /** Duplicates this object */
    public synchronized KView clone()
    {
        KView ret = new KView(parent);
        ret.ID = ID;
        ret.xform = (float[][])xform.clone();
        if(center == null) ret.center = getCenter();
        else               ret.center = (float[])center.clone();
        
        ret.zoom = zoom;
        ret.span = span;
        ret.clip = clip;
        
        ret.compile();
        return ret;
    }
//}}}

//{{{ get/setName, toString
//##################################################################################################
    /** Gets the name of this element */
    public String getName()
    { return ID; }
    /** Sets the name of this element */
    public synchronized void setName(String nm)
    { ID = nm; }
    /** Gets the name of this element (same as <code>getName()</code>*/
    public String toString()
    { return getName(); }
//}}}

//{{{ mmult, normalize, transpose
//##################################################################################################
    // Multiplies two 3x3 matrices
    static protected float[][] mmult(float[][] A, float[][] B)
    {
        float[][] R = new float[3][3];
        int i, j, k;
        float r;
        
        for(i = 0; i < 3; i++)
        {// row loop
            for(j = 0; j < 3; j++)
            {// column loop
                r = 0f;
                // index loop
                for(k = 0; k < 3; k++) r += A[i][k] * B[k][j];
                R[i][j] = r;
            }// column loop
        }// row loop
        
        return R;
    }
    
    // Multiplies a 3x3 matrix with a 3x1 vector
    static protected float[] mmult(float[][] A, float[] X)
    {
        float[] R = new float[3];
        int i, j;
        float r;
        
        for(i = 0; i < 3; i++)
        {// row loop
            r = 0f;
            // index loop
            for(j = 0; j < 3; j++) r += A[i][j] * X[j];
            R[i] = r;
        }
        
        return R;
    }
    
    /**
    * Adjusts matrix values for self-consistency -- http://www.makegames.com/3drotation/
    *
    * R is special orthogonal (I'll trust their math):
    *   R R' = I    (R times its transpose is identity)
    *   det R = 1   (determinant of R equals 1)
    *
    * "A more helpful set of properties is provided by Michael E. Pique in Graphics Gems (Glassner, Academic Press, 1990): 
    * 1. R is normalized: the squares of the elements in any row or column sum to 1. 
    * 2. R is orthogonal: the dot product of any pair of rows or any pair of columns is 0. 
    * 3. The rows of R represent the coordinates in the original space of unit vectors along the coordinate axes of the rotated space. (Figure 1). 
    * 4. The columns of R represent the coordinates in the rotated space of unit vectors along the axes of the original space."
    *
    * Here I follow the procedure described above for building a "good" rotation matrix (sort of),
    * but without using what they call a World Up vector, since we're in a molecule & that doesn't mean anything.
    * I call the rows X*, Y*, and Z*, as per (3) above
    */
    static protected void normalize(float[][] A)
    {
        float mag;
        
        // Don't change 3rd row (Z*) except to normalize it (magnitude --> 1)
        mag = (float)Math.sqrt(A[2][0]*A[2][0] + A[2][1]*A[2][1] + A[2][2]*A[2][2]);
        A[2][0] = A[2][0]/mag; A[2][1] = A[2][1]/mag; A[2][2] = A[2][2]/mag;
        
        // Normalize 2nd row (Y*), then take cross product to get the first ( Y* x Z* = X* )
        // X* will be normalized b/c Y* and Z* are.
        // <a1,a2,a3> x <b1,b2,b3> = <a2b3-a3b2, -a1b3+a3b1, a1b2-a2b1>
        mag = (float)Math.sqrt(A[1][0]*A[1][0] + A[1][1]*A[1][1] + A[1][2]*A[1][2]);
        A[1][0] = A[1][0]/mag; A[1][1] = A[1][1]/mag; A[1][2] = A[1][2]/mag;
        A[0][0] = A[1][1]*A[2][2] - A[1][2]*A[2][1];
        A[0][1] = A[1][2]*A[2][0] - A[1][0]*A[2][2];
        A[0][2] = A[1][0]*A[2][1] - A[1][1]*A[2][0];
        
        // Now let Y* = Z* x X* so it's perpendicular to both!
        // <a1,a2,a3> x <b1,b2,b3> = <a2b3-a3b2, -a1b3+a3b1, a1b2-a2b1>
        A[1][0] = A[2][1]*A[0][2] - A[2][2]*A[0][1];
        A[1][1] = A[2][2]*A[0][0] - A[2][0]*A[0][2];
        A[1][2] = A[2][0]*A[0][1] - A[2][1]*A[0][0];
    }
    
    // Constructs the transpose of a 3x3 matrix
    static protected float[][] transpose(float[][] A)
    {
        float[][] R = new float[3][3];
        int i, j;
        for(i = 0; i < 3; i++)
        {
            for(j = 0; j < 3; j++) R[i][j] = A[j][i];
        }
        return R;
    }
    
//}}} Matrix math routines

//{{{ compile
//##################################################################################################
    /**
    * Updates this view's public fields to reflect the current internal state.
    */
    public synchronized void compile()
    {
        R11 = xform[0][0];// * zoom;
        R12 = xform[0][1];// * zoom;
        R13 = xform[0][2];// * zoom;
        R21 = xform[1][0];// * zoom;
        R22 = xform[1][1];// * zoom;
        R23 = xform[1][2];// * zoom;
        R31 = xform[2][0];// * zoom;
        R32 = xform[2][1];// * zoom;
        R33 = xform[2][2];// * zoom;
        
        if(center == null) getCenter();
        cx = center[0]; cy = center[1]; cz = center[2];
    }
    
//}}}

//{{{ rotateX/Y/Z
//##################################################################################################
    /**
    * Rotates about the axis defined as 'x' by the basis of this tranform.
    * @param t the amount of rotation, in radians
    */
    public synchronized void rotateX(float t)
    {
        float sin, cos;
        sin = (float)Math.sin(t);
        cos = (float)Math.cos(t);
        float[][] rot = { {1f, 0f, 0f}, {0f, cos, -sin}, {0f, sin, cos} };
        xform = mmult(rot, xform);
        if(++nUpdates % N_UPDATES_ALLOWED == 0) { nUpdates = 0; normalize(xform); }
    }

    /**
    * Rotates about the axis defined as 'y' by the basis of this tranform.
    * @param t the amount of rotation, in radians
    */
    public synchronized void rotateY(float t)
    {
        float sin, cos;
        sin = (float)Math.sin(t);
        cos = (float)Math.cos(t);
        float[][] rot = { {cos, 0f, sin}, {0f, 1f, 0f}, {-sin, 0f, cos} };
        xform = mmult(rot, xform);
        if(++nUpdates % N_UPDATES_ALLOWED == 0) { nUpdates = 0; normalize(xform); }
    }

    /**
    * Rotates about the axis defined as 'z' by the basis of this tranform.
    * @param t the amount of rotation, in radians
    */
    public synchronized void rotateZ(float t)
    {
        float sin, cos;
        sin = (float)Math.sin(t);
        cos = (float)Math.cos(t);
        float[][] rot = { {cos, -sin, 0f}, {sin, cos, 0f}, {0f, 0f, 1f} };
        xform = mmult(rot, xform);
        if(++nUpdates % N_UPDATES_ALLOWED == 0) { nUpdates = 0; normalize(xform); }
    }
//}}}

//{{{ setID, setMatrix, setZoom, get/setSpan
//##################################################################################################
    /**
    * Sets the label used to identify this view.
    * Identical to <code>setName()</code>.
    * @param name the name of this view
    */
    public synchronized void setID(String name) { setName(name); }
    
    /**
    * Sets the matrix used by this view
    * @param matrix a 3x3 matrix, matrix[row][col]
    */
    public synchronized void setMatrix(float[][] matrix) { xform = (float[][])matrix.clone(); }
    
    /**
    * Sets the current zoom factor.
    * The zoom factor is defined as the ratio of the span of the kinemage
    * to the span of this view, such that values less than 1 appear to be
    * distant, and values greater than 1 appear to be zoomed in.
    *
    * <p>Calling this function will overwrite the previous value for span.
    *
    * @param z the desired zoom
    */
    public synchronized void setZoom(float z)
    {
        zoom = z;
        span = 0f;
        //span = getSpan(); // so span will be calc'd immediately with current kin size
    }
    
    /**
    * Gets the span of this view,
    * i.e., the desired width of the graphics area in model units.
    */
    public synchronized float getSpan()
    {
        if(span <= 0f)
        {
            if(zoom <= 0f) zoom = 1.0f;
            span = parent.getSpan() / zoom;
        }
        
        return span;
    }
    
    /**
    * Set the span of this view, which (if no zoom has been set or zoom is <= 0)
    * will set the zoom such that an object s units across fits on screen.
    */
    public synchronized void setSpan(float s) { span = s; }
//}}}

//{{{ get/setClip, get/setCenter
//##################################################################################################
    /**
    * Returns the current clip factor.
    * @return the current clip
    */
    public float getClip() { return clip; }
    
    /**
    * Sets the current clip factor.
    * @param c the desired clip
    */
    public synchronized void setClip(float c) { clip = c; }

    /** Returns the coordinates of the current center as a float[] = {x,y,z} */
    public synchronized float[] getCenter()
    {
        if(center == null)
        {
            if(parent != null) center = parent.getCenter();
            else
            {
                float[] dummy = { 0f, 0f, 0f };
                center = dummy;
            }
        }
        
        return (float[])center.clone();
    }
    
    /**
    * Set the center point for this transform.
    * @param x the x coordinate
    * @param y the y coordinate
    * @param z the z coordinate
    */
    public synchronized void setCenter(float x, float y, float z)
    {
        if(center == null) center = new float[3];
        center[0] = x;
        center[1] = y;
        center[2] = z;
    }
//}}}

//{{{ translateRotated, viewTranslateRotated
//##################################################################################################
    /**
    * Given a pixel offset in rotated and scaled coordinates, calculate an offset in "real" coordinates.
    * If x and y are taken from screen coordinates, remember to invert y!
    */
    public synchronized float[] translateRotated(int x, int y, int z, int screenSize)
    {
        float cx, cy, cz, correctedZoom;
        correctedZoom = (float)screenSize / getSpan(); //getZoom() * (float)screenSize / parent.getSpan();
        cx = (float)x / correctedZoom;
        cy = (float)y / correctedZoom;
        cz = (float)z / correctedZoom;
        
        float[] tcoord = { cx, cy, cz };
        float[] coord = mmult(transpose(xform), tcoord);
        return coord;
    }

    /**
    * Given a pixel offset in rotated and scaled coordinates, calculate a new center point.
    * If x and y are taken from screen coordinates, remember to invert y!
    */
    public synchronized void viewTranslateRotated(int x, int y, int z, int screenSize)
    {
        float[] coord = translateRotated(x, y, z, screenSize);
        float[] orig  = getCenter();
        setCenter(orig[0]-coord[0], orig[1]-coord[1], orig[2]-coord[2]);
    }
//}}}

//{{{ get/set/activateViewingAxes
//##################################################################################################
    /**
    * Stores a set of 0-based indices for the coordinates axes that should be
    * placed into X, Y, Z when this view is activated.
    * Indices are stored as an int[3], or null (meaning no change).
    * This does NOT automatically update the coordinates in the kinemage,
    * even if called on the currently active view.
    * Coordinates ARE automatically updated when this view is next chosen from the menu.
    */
    public void setViewingAxes(int[] indices)
    {
        if(indices == null) this.viewingAxes = null;
        else this.viewingAxes = (int[]) indices.clone();
    }
    
    /** Returns the value set by setViewingAxes() (may be null) */
    public int[] getViewingAxes() { return this.viewingAxes; }
    
    /**
    * Copies the high-dimensional coordinates at the specified indices
    * into all point's (untransformed) X, Y, and Z fields.
    * If a index is out of range (0-based), it is ignored and the value is not changed.
    */
    public void activateViewingAxes(int xIndex, int yIndex, int zIndex)
    {
        for(KList list : KIterator.allLists(parent))
        {
            // We will miss points with extra coordinates if
            // list.dimension wasn't set...
            if(list.getDimension() <= 3) continue;
            for(KPoint pt : list)
                pt.useCoordsXYZ(xIndex, yIndex, zIndex);
        }
        parent.fireKinChanged(AHE.CHANGE_POINT_COORDINATES);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class
