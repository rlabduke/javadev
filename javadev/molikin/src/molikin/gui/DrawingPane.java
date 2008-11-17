// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;

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
* <code>DrawingPane</code> is the common interface for
* UI elements for kinemage construction.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Oct 17 14:07:49 EDT 2005
*/
public interface DrawingPane //extends ... implements ...
{
    /** To identify this pane */
    public String toString();
    
    /** Emits the kinemage (text) representation as selected by the user */
    public void printKinemage(PrintWriter out, Model m, String chainID, String idCode, String bbColor);
    
    /** As a Collection of Model objects. */
    public Collection getSelectedModels();
    
    /** As a Collection of Strings representing chain IDs. */
    public Collection getSelectedChains();
}//class

