// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Crayon</code> interfaces are used (1) to customize rendering of
* kinemage elements (color, size, masters, etc) and (2) to filter out
* things that should not be rendered at all (via shouldPrint()).
*
* <p>Given a Crayon object, one calls a forXXX() function, then reads out the
* result or results from the getter functions.  After another forXXX() call,
* those results will be overwritten with new ones.  This is to avoid many
* unnecessary object allocations.
*
* <p>Most users will only need to check shouldPrint() and getKinString();
* the individual attribute getters exist for the use of compositing Crayons.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 10 07:27:10 EDT 2006
*/
public interface Crayon //extends ... implements ...
{
    /**
    * Returns true if the object should be part of the kinemage,
    * false if it should be skipped.
    */
    public boolean shouldPrint();
    
    /**
    * Customizes the rendering of a kinemage point by returning a string
    * that includes color, width/radius, pointmasters, aspects, etc --
    * all the attributes a Crayon can carry, in kinemage format.
    * @return       a valid kinemage string, or "" for nothing.
    *   Null is NOT a valid return value and will cause problems.
    *   Leading/trailing spaces are not necessary.
    */
    public String getKinString();
    
    /** Returns a valid kinemage color, or null for none */
    public String getColor();
    
    /** Returns one or more pointmaster flags, without quote marks, or null for none */
    public String getPointmasters();
    
    /** Returns one or more aspect flags, without parentheses, or null for none */
    public String getAspects();
    
    /** Returns a line width between 1 and 7, or 0 for default */
    public int getWidth();

    /** Returns a ball or sphere radius, or 0 for default */
    public double getRadius();
    
    /** Returns true if the point should be marked unpickable (U) */
    public boolean getUnpickable();
}//class

