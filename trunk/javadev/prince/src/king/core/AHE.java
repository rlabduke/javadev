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
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Oct  2 10:50:32 EDT 2002
*/
public interface AHE extends Transformable
{
    /** "Everything" has changed (if you're not sure which one(s) to use, try this) */
    public static final int CHANGE_EVERYTHING           = ~0;

    /** A group/subgroup/list has been added to / removed from the kinemage */
    public static final int CHANGE_TREE_CONTENTS        = (1<<0);
    /** Some property (name, dominant, etc) of a group/subgroup/list has changed */
    public static final int CHANGE_TREE_PROPERTIES      = (1<<1);
    /** A group/subgroup/list has switched on or off (shown / hidden) */
    public static final int CHANGE_TREE_ON_OFF          = (1<<2);
    /** The set of masters controlling a group/subgroup/list has changed */
    public static final int CHANGE_TREE_MASTERS         = (1<<3);
    /** Something / everything has changed about a group/subgroup/list */
    public static final int CHANGE_TREE                 = 0x000000ff;
    
    /** A point has been added to / removed from the kinemage */
    public static final int CHANGE_POINT_CONTENTS       = (1<<8);
    /** Some property (name, dominant, etc) of a point has changed */
    public static final int CHANGE_POINT_PROPERTIES     = (1<<9);
    /** A point has switched on or off (shown / hidden) */
    public static final int CHANGE_POINT_ON_OFF         = (1<<10);
    /** The set of masters controlling a point has changed */
    public static final int CHANGE_POINT_MASTERS        = (1<<11);
    /** Something / everything has changed about a point */
    public static final int CHANGE_POINT                = 0x0000ff00;

    /** List properties have changed */
    public static final int CHANGE_LIST_PROPERTIES      = CHANGE_TREE_PROPERTIES | CHANGE_POINT_PROPERTIES;



    /** Gets the name of this element */
    public String getName();
    /** Sets the name of this element */
    public void setName(String nm);
    /** Determines the owner (parent) of this element */
    public AGE getParent();
    /** Establishes the owner (parent) of this element */
    public void setParent(AGE owner);
    /** Retrieves the Kinemage object at the top of this hierarchy, or null if none. */
    public Kinemage getKinemage();
    /**
    * Returns the number of steps up the hierarchy to reach the kinemage.
    * 0 means this object is the kinemage, 1 means it's a group, 2 means a subgroup, etc.
    */
    public int getDepth();


    /** Indicates whether this element will paint itself, given the chance */
    public boolean isOn();
    /** Sets the painting status of this element */
    public void setOn(boolean paint);
    /** Indicates whether this element will actually be painted (ie, whether it and all its parents are "on") */
    public boolean isVisible();
    /**
    * Propagates a signal up the kinemage hierarchy that something has been changed.
    * It is rare for client code to call this function -- it's usually called
    * automatically when some property is changed.
    */
    public void fireKinChanged(int eventFlags);


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

