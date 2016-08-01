// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>ResidueException</code> is thrown when a Residue cannot be found,
* or when the appropriate resource for a Residue cannot be located.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  9 16:42:31 EDT 2003
*/
public class ResidueException extends Exception
{
    /**
    * Constructor
    */
    public ResidueException(String msg)
    {
        super(msg);
    }
}//class

