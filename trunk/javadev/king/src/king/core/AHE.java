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
public interface AHE extends TransformSignalSubscriber
{
    /** Gets the name of this element */
    public String getName();
    /** Sets the name of this element */
    public void setName(String nm);
    /** Determines the owner (parent) of this element */
    public AGE getOwner();
    /** Establishes the owner (parent) of this element */
    public void setOwner(AGE owner);
    /** Retrieves the Kinemage object holding this element, or null if none. */
    public Kinemage getKinemage();
    /** Indicates whether this element will paint itself, given the chance */
    public boolean isOn();
    /** Sets the painting status of this element */
    public void setOn(boolean paint);
    /** Indicates whether this element will actually be painted (i.e., is it and all its parents on?) */
    public boolean isVisible();
    /** Returns true iff this element is On, it's owner is On, it's owner's owner is On, and so on */
    public boolean isTotallyOn();
    /**
    * Gets a bounding box for the current model.
    * @param bound the first 6 elements get set to { minX, minY, minZ, maxX, maxY, maxZ }.
    * Should be called with { +inf, +inf, +inf, -inf, -inf, -inf }
    */
    public void calcBoundingBox(float[] bound);
    /**
    * Gets the square of the radius of this model from the specified center.
    * @param center an array with the x, y,  and z coordinates of the center
    * @return the square of the radius of this element, centered at center
    */
    public float calcRadiusSq(float[] center);
}//class

