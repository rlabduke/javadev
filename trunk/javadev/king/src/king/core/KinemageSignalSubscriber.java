// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>KinemageSignalSubscriber</code> allows objects to
* receive signals from a KinemageSignal, which reflects
* state changes in the kinemage.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 27 10:32:24 EST 2003
*/
public interface KinemageSignalSubscriber //extends ... implements ...
{
    /**
    * The hierarchical, logical structure of the kinemage has changed.
    * This includes addition/deletion of groups, subgroups, lists,
    * masters, and views (but not points).
    * Also included are changes to the animate, 2animate, dominant,
    * and nobutton properties.
    */
    public static final int STRUCTURE       = 1<<0;
    
    /**
    * The 3-D visual manifestation of the kinemage has changed.
    * This includes addition/deletion of points.
    * Also included are rotations, selection of a new viewpoint,
    * selection of a new aspect, and changes to point or list display properties.
    */
    public static final int APPEARANCE      = 1<<1;
    
    /**
    * A call to this method indicates that the specified
    * kinemage has changed somehow.
    *
    * <p>This method will be called in response to KinemageSignal.signalKinemage().
    *
    * @param kinemage   the Kinemage object that has changed
    * @param bitmask    a set of flags describing which properties have changed.
    */
    public void signalKinemage(Kinemage kinemage, int bitmask);
}//class

