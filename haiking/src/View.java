// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;
//}}}
/**
* <code>View</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb  1 16:16:45 EST 2005
*/
public class View //extends ... implements ...
{
//{{{ Constants
// Cosine and sine of 0, 1, ... 20 degree angles, multiplied by 1<<15 (1<<ROT_BITS).
static final int[] cos_degrees = {
    32768, 32763, 32748, 32723, 32688, 32643, 32588, 32524, 32449, 32365, 32270,
    32166, 32052, 31928, 31795, 31651, 31499, 31336, 31164, 30983, 30792
};
static final int[] sin_degrees = {
    0, 572, 1144, 1715, 2286, 2856, 3425, 3993, 4560, 5126, 5690, 6252, 6813,
    7371, 7927, 8481, 9032, 9580, 10126, 10668, 11207
};

// Divide the native coords by one of these numbers to get pixel coords.
// They're spaced at HALF powers of two, from 2^0 to 2^26 inclusive.
// Bit-shift produces full power-of-two steps, but that's too coarse.
/*static final int[] ZOOM_FACTORS = {
    1, 2, 3, 4, 6, 8, 11, 16, 23, 32, 45, 64, 91, 128, 181, 256, 362, 512, 724,
    1024, 1448, 2048, 2896, 4096, 5793, 8192, 11585, 16384, 23170, 32768, 46341,
    65536, 92682, 131072, 185364, 262144, 370728, 524288, 741455, 1048576,
    1482910, 2097152, 2965821, 4194304, 5931642, 8388608, 11863283, 16777216,
    23726566, 33554432, 47453133, 67108864
};*/

// Divide the native coords by one of these numbers to get pixel coords.
// They're spaced at ONE-THIRD powers of two, from 2^0 to 2^26 inclusive.
// (Duplicates created by roundoff of small ints were removed.)
// Bit-shift produces full power-of-two steps, but that's too coarse.
static final int[] ZOOM_FACTORS = {
    1, 2, 3, 4, 5, 6, 8, 10, 13, 16, 20, 25, 32, 40, 51, 64, 81, 102, 128, 161,
    203, 256, 323, 406, 512, 645, 813, 1024, 1290, 1625, 2048, 2580, 3251, 4096,
    5161, 6502, 8192, 10321, 13004, 16384, 20643, 26008, 32768, 41285, 52016,
    65536, 82570, 104032, 131072, 165140, 208064, 262144, 330281, 416128,
    524288, 660561, 832255, 1048576, 1321123, 1664511, 2097152, 2642246,
    3329021, 4194304, 5284492, 6658043, 8388608, 10568984, 13316085, 16777216,
    21137968, 26632170, 33554432, 42275935, 53264341, 67108864
};

static final int ROT_BITS = 15;     // decimal bits in rot matrix
static final int SCALE_MAX = 26;    // max # of bits to downshift orig coords
static final int PERSP_DIST = 500;  // perspective distance, in pixels
static final int MAX_COORD = 1<<14; // max x,y,z value that can be rotated w/out overflow
static final int MIN_COORD = -MAX_COORD;
//}}}

//{{{ Variable definitions
//##############################################################################
    // Rotation matrix (ROT_BITS decimal bits)
    int r11=1<<ROT_BITS, r12=0, r13=0,
        r21=0, r22=1<<ROT_BITS, r23=0,
        r31=0, r32=0, r33=1<<ROT_BITS;
    // Center (untransformed coords)
    int cx = 0, cy = 0, cz = 0;
    // Scale: number of bits to right-shift orig coords (0 to ZOOM_FACTORS.length-1)
    private int scaleIndex = ZOOM_FACTORS.length-1;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public View()
    {
        super();
    }
//}}}

//{{{ centerAndScale
//##############################################################################
    /**
    * First transformation step: maps x0,y0,z0 to x1,y1,z1.
    * Centers the coords and scales them down to units of pixels.
    * Needs to be repeated only when center or zoom changes.
    * @param radius2    the squared radius around 0,0,0 within which to keep pts
    * @return the point starting a chain along prevDrawable of points to be
    *   rotated and painted.
    */
    public KPoint centerAndScale(KPoint p, int radius2)
    {
        KPoint firstDrawable = null;
        boolean transformThis = false, transformNext = false;
        int scale = getScaleDivisor();
        while(p != null)
        {
            // Center and rescale point in pixel units
            p.x1 = (p.x0 - cx) / scale;
            p.y1 = (p.y0 - cy) / scale;
            p.z1 = (p.z0 - cz) / scale;
            
            // If we're not in the sphere enclosing the viewable box,
            // we'll never be visible no matter how we're rotated.
            // Lines are judged by their midpoint.
            int type = p.getType();
            if(!p.isOn()) // line will always be off if line is, b/c of list structure
            {
                transformThis = transformNext = false;
            }
            else if(type <= KPoint.TYPE_VECTOR_NODRAW)
            {
                transformThis = transformNext;
                transformNext = false;
            }
            else if(type <= KPoint.TYPE_VECTOR_DRAW2)
            {
                KPoint q = p.prev; // not null b/c this is a draw pt
                int midx = (p.x1+q.x1) >> 1;
                int midy = (p.y1+q.y1) >> 1;
                int midz = (p.z1+q.z1) >> 1;
                //boolean dist = (radius2 >= midx*midx + midy*midy + midz*midz);
                //transformThis = dist | transformNext;
                //transformNext = dist;
                
                // If one pt on the polyline is xformed, the rest of the line must be,
                // so that future calculations of midpoint Z will be correct.
                // This means really, really long polylines are inefficient for rendering...
                transformNext = transformThis = transformNext | (radius2 >= midx*midx + midy*midy + midz*midz);

                // To avoid overflow artifacts during rotation of remote pts on the polyline
                     if(p.x1 < MIN_COORD) p.x1 = MIN_COORD;
                else if(p.x1 > MAX_COORD) p.x1 = MAX_COORD;
                     if(p.y1 < MIN_COORD) p.y1 = MIN_COORD;
                else if(p.y1 > MAX_COORD) p.y1 = MAX_COORD;
                     if(p.z1 < MIN_COORD) p.z1 = MIN_COORD;
                else if(p.z1 > MAX_COORD) p.z1 = MAX_COORD;

            }
            else
            {
                transformThis = (radius2 >= p.x1*p.x1 + p.y1*p.y1 + p.z1*p.z1);
            }
            
            if(transformThis)
            {
                p.prevDrawable = firstDrawable;
                firstDrawable = p;
            }
            
            p = p.prev;
        }
        
        return firstDrawable;
    }
//}}}
    
//{{{ rotateAndRecenter
//##############################################################################
    /**
    * Second transformation step: maps x1,y1,z1 to x2,y2,z2.
    * Rotates the coordinates and shifts them to be usable as drawing coords.
    * Does not include perspective effects.
    * Needs to be repeated any time the view changes.
    * Input coords should have less than 15 significant bits or overflow will occur.
    */
    public void rotateAndRecenter(KPoint p, int canvasHalfWidth, int canvasHalfHeight)
    {
        while(p != null)
        {
            // Y has to be inverted in order to match drawing coord system
            p.x2 =  ((r11*p.x1 + r12*p.y1 + r13*p.z1) >> ROT_BITS) + canvasHalfWidth;
            p.y2 = -((r21*p.x1 + r22*p.y1 + r23*p.z1) >> ROT_BITS) + canvasHalfHeight;
            p.z2 =  ((r31*p.x1 + r32*p.y1 + r33*p.z1) >> ROT_BITS);
            
            p = p.prevDrawable;
        }
    }
//}}}

//{{{ rotateAndRecenterPersp
//##############################################################################
    /**
    * Second transformation step: maps x1,y1,z1 to x2,y2,z2.
    * Rotates the coordinates and shifts them to be usable as drawing coords.
    * Includes perspective effects.
    * Needs to be repeated any time the view changes.
    * Input coords should have less than 15 significant bits or overflow will occur.
    */
    public void rotateAndRecenterPersp(KPoint p, int canvasHalfWidth, int canvasHalfHeight)
    {
        while(p != null)
        {
            // Y has to be inverted in order to match drawing coord system
            p.x2 =  ((r11*p.x1 + r12*p.y1 + r13*p.z1) >> ROT_BITS);
            p.y2 = -((r21*p.x1 + r22*p.y1 + r23*p.z1) >> ROT_BITS);
            p.z2 =  ((r31*p.x1 + r32*p.y1 + r33*p.z1) >> ROT_BITS);
            
            // Perspective is currently ~20% of our compute cost...
            int div = PERSP_DIST - p.z2;
            p.x2 = (PERSP_DIST * p.x2) / div;
            p.y2 = (PERSP_DIST * p.y2) / div;
            
            p.x2 += canvasHalfWidth;
            p.y2 += canvasHalfHeight;
            
            p = p.prevDrawable;
        }
    }
//}}}

//{{{ rotate
//##############################################################################
    /**
    * Modifies the rotation matrix to rotated the current view around the
    * given axis by the specified number of degrees (up to some limit).
    * @param axis   1 for x, 2 for y, 3 for z
    */
    public void rotate(int axis, int degrees)
    {
        int absdeg = (degrees > 0 ? degrees : -degrees);
        if(absdeg >= cos_degrees.length) absdeg = cos_degrees.length - 1;
        int cos = cos_degrees[absdeg];
        int sin = (degrees > 0 ? sin_degrees[absdeg] : -sin_degrees[absdeg]);
        
        // Premultiplied matrix (ROT_BITS decimal bits)
        int m11=1<<ROT_BITS, m12=0, m13=0,
            m21=0, m22=1<<ROT_BITS, m23=0,
            m31=0, m32=0, m33=1<<ROT_BITS;
        switch(axis)
        {
            case 1:
                m22 = cos; m23 = -sin;
                m32 = sin; m33 = cos;
                break;
            case 2:
                m11 = cos; m13 = sin;
                m31 = -sin; m33 = cos;
                break;
            case 3:
                m11 = cos; m12 = -sin;
                m21 = sin; m22 = cos;
                break;
        }
        
        // Construct temporary rotation matrix elements
        int t11 = (m11*r11 + m12*r21 + m13*r31) >> ROT_BITS;
        int t12 = (m11*r12 + m12*r22 + m13*r32) >> ROT_BITS;
        int t13 = (m11*r13 + m12*r23 + m13*r33) >> ROT_BITS;
        int t21 = (m21*r11 + m22*r21 + m23*r31) >> ROT_BITS;
        int t22 = (m21*r12 + m22*r22 + m23*r32) >> ROT_BITS;
        int t23 = (m21*r13 + m22*r23 + m23*r33) >> ROT_BITS;
        int t31 = (m31*r11 + m32*r21 + m33*r31) >> ROT_BITS;
        int t32 = (m31*r12 + m32*r22 + m33*r32) >> ROT_BITS;
        int t33 = (m31*r13 + m32*r23 + m33*r33) >> ROT_BITS;
        
        r11 = t11; r12 = t12; r13 = t13;
        r21 = t21; r22 = t22; r23 = t23;
        r31 = t31; r32 = t32; r33 = t33;
    }
//}}}

//{{{ translate
//##############################################################################
    /** Translates the view center by a certain dx, dy, dz. */
    public void translate(int dx, int dy, int dz)
    {
        // We have to use longs to avoid overflow while retaining precision.
        long scale = getScaleDivisor();
        long xx = ((r11*dx + r21*dy + r31*dz)*scale) >> ROT_BITS;
        long yy = ((r12*dx + r22*dy + r32*dz)*scale) >> ROT_BITS;
        long zz = ((r13*dx + r23*dy + r33*dz)*scale) >> ROT_BITS;
        cx -= (int)xx;
        cy -= (int)yy;
        cz -= (int)zz;
    }
//}}}

//{{{ get/setScale, getScaleDivisor
//##############################################################################
    public int getScale()
    { return this.scaleIndex; }

    /** Sets the scale factor as a number of bits; 0 is max zoom. */
    public void setScale(int s)
    {
        if(s < 0)                           scaleIndex = 0;
        else if(s >= ZOOM_FACTORS.length)   scaleIndex = ZOOM_FACTORS.length;
        else                                scaleIndex = s;
    }
    
    /** Returns the factor by which to divide coordinates to scale them for this view. */
    public int getScaleDivisor()
    { return ZOOM_FACTORS[this.scaleIndex]; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

