// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

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
* <code>OrderedSet</code> is simply the union of the
* Set and List interfaces, which are not mutually exclusive.
* For instance, LinkedHashSet should implement this interface.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 16 18:51:18 EDT 2003
*/
public interface OrderedSet extends List, Set
{
}//class

