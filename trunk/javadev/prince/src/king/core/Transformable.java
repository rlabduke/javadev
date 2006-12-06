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
* <code>Transformable</code> objects can participate in rendering and picking
* operations by having their original coordinates transformed into the
* drawing coordinate space.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 27 10:32:24 EST 2003
*/
public interface Transformable //extends ... implements ...
{
    /**
    * A call to this method indicates the object
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void doTransform(Engine engine, Transform xform);
}//class

