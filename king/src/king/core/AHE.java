// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
 * <code>AHE</code> (Abstract Hierarchy Element) is the basis for all points, lists, groups, etc.
 *
 * <p>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Oct  2 10:50:32 EDT 2002
*/
abstract public class AHE implements TransformSignalSubscriber
{
//{{{ Variable definitions
//##################################################################################################
    String name = "";
//}}}

//{{{ get/setName, toString, get/setOwner
//##################################################################################################
    /** Gets the name of this element */
    public String getName()
    { return name; }
    /** Sets the name of this element */
    public void setName(String nm)
    { name = nm; }

    /** Gets the name of this element (same as <code>getName()</code>*/
    public String toString()
    { return getName(); }

    /** Determines the owner (parent) of this element */
    abstract public AGE getOwner();
    /** Establishes the owner (parent) of this element */
    abstract public void setOwner(AGE owner);
//}}}

//{{{ getKinemage
//##################################################################################################
    /** Retrieves the Kinemage object holding this element, or null if none. */
    public Kinemage getKinemage()
    {
        AGE owner = getOwner();
        if(owner == null)   return null;
        else                return owner.getKinemage();
    }
//}}}

//{{{ is/setOn(), isVisible(), isTotallyOn
//##################################################################################################
    /** Indicates whether this element will paint itself, given the chance */
    abstract public boolean isOn();
    /** Sets the painting status of this element */
    abstract public void setOn(boolean paint);
    /** Indicates whether this element will actually be painted (i.e., is it and all its parents on?) */
    public boolean isVisible()
    { return (getOwner().isVisible() && isOn()); }
    /** Returns true iff this element is On, it's owner is On, it's owner's owner is On, and so on */
    public boolean isTotallyOn()
    {
        if(this.getOwner() == null) return this.isOn();
        else return (this.isOn() && this.getOwner().isTotallyOn());
    }
//}}}

//{{{ calcBoundingBox() and calcRadiusSq()
//##################################################################################################
    /**
    * Gets a bounding box for the current model.
    * @param bound the first 6 elements get set to { minX, minY, minZ, maxX, maxY, maxZ }.
    * Should be called with { +inf, +inf, +inf, -inf, -inf, -inf }
    */
    abstract public void calcBoundingBox(float[] bound);
    
    /**
    * Gets the square of the radius of this model from the specified center.
    * @param center an array with the x, y,  and z coordinates of the center
    * @return the square of the radius of this element, centered at center
    */
    abstract public float calcRadiusSq(float[] center);
//}}}
    
//{{{ preTransform(), viewTransform()
//##################################################################################################
    /** Wipes out any previous transforms, preparing for a fresh start. */
    public void preTransform() {}
    
    /**
    * Transforms this object into screen space (pixel) coordinates, including (x,y) offset and perspective.
    * Also does depth buffering.
    * @param engine the engine holding the depth buffer and transform matrix
    */
    public void viewTransform(Engine engine) {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

