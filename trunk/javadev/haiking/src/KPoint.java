// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;
import java.util.Hashtable;
//}}}
/**
* <code>KPoint</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb  1 16:25:40 EST 2005
*/
public class KPoint //extends ... implements ...
{
//{{{ Constants
    // All point IDs are stored here (labels only)
    public static final Hashtable pointIDs = new Hashtable();

    // Parameters for bit-packing info into this.multi
    static final int TYPE_SHIFT         = 0;
    static final int TYPE_MASK          = (1<<3) - 1;
    static final int COLOR_SHIFT        = 3;
    static final int COLOR_MASK         = (1<<5) - 1;
    static final int RAD_SHIFT          = (8-3);
    static final int RAD_MASK           = 0x0007fff8;
    
    // Codes to identify point type
    static final int TYPE_VECTOR_NODRAW = 0;
    static final int TYPE_VECTOR_DRAW1  = 1;
    static final int TYPE_VECTOR_DRAW2  = 2;
    static final int TYPE_DOT_SMALL     = 3;
    static final int TYPE_DOT_MEDIUM    = 4;
    static final int TYPE_DOT_LARGE     = 5;
    static final int TYPE_BALL          = 6;
    static final int TYPE_LABEL         = 7;
    
    // Masks for hiding certain point types
    static final int MASK_VECTOR_NODRAW = 1 << TYPE_VECTOR_NODRAW;
    static final int MASK_VECTOR_DRAW1  = 1 << TYPE_VECTOR_DRAW1 ;
    static final int MASK_VECTOR_DRAW2  = 1 << TYPE_VECTOR_DRAW2 ;
    static final int MASK_DOT_SMALL     = 1 << TYPE_DOT_SMALL    ;
    static final int MASK_DOT_MEDIUM    = 1 << TYPE_DOT_MEDIUM   ;
    static final int MASK_DOT_LARGE     = 1 << TYPE_DOT_LARGE    ;
    static final int MASK_BALL          = 1 << TYPE_BALL         ;
    static final int MASK_LABEL         = 1 << TYPE_LABEL        ;
//}}}

//{{{ Variable definitions
//##############################################################################
    int x0 = 0, y0 = 0, z0 = 0;     // original coords, use full range
    int x1 = 0, y1 = 0, z1 = 0;     // centered and scaled-down (pixels)
    int x2 = 0, y2 = 0, z2 = 0;     // rotated and re-centered (drawing coords)
    KPoint prev = null;             // for linked list of pts in KList
    KPoint prevDrawable = null;     // for linked list of pts to xform and draw
    KPoint zchain = null;           // for linked list in z-buffer
    
    // Used for various things:
    // Bits 0-2:    point type
    // Bits 3-7:    color
    // Bits 8-23:   radius (mult by 8 to get same scale as pt coords)
    private int multi = 0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KPoint()
    { this(0, 0, 0, 0); }
    
    public KPoint(int x, int y, int z)
    { this(x, y, z, 0); }

    public KPoint(int x, int y, int z, int multi)
    {
        super();
        this.x0 = x;
        this.y0 = y;
        this.z0 = z;
        this.multi = multi;
    }
//}}}

//{{{ getType/Color/Radius
//##############################################################################
    public int getType()
    { return (multi >> TYPE_SHIFT) & TYPE_MASK; }
    
    public int getColor()
    { return (multi >> COLOR_SHIFT) & COLOR_MASK; }
    
    public int getRadius()
    { return (multi >> RAD_SHIFT) & RAD_MASK; }
//}}}

//{{{ getDrawingZ, get/setPointID
//##############################################################################
    /** The z-depth at which the point wants to be drawn. */
    public int getDrawingZ()
    {
        int type = this.getType();
        if(type <= KPoint.TYPE_VECTOR_NODRAW)       return -1000000; // never inside the clip planes
        else if(type <= KPoint.TYPE_VECTOR_DRAW2)   return (this.z2+prev.z2) >> 1;
        else                                        return this.z2;
    }
    
    public String getPointID()
    {
        String s = (String) pointIDs.get(this);
        if(s == null)   return "";
        else            return s;
    }
    
    public void setPointID(String s)
    { pointIDs.put(this, s); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

