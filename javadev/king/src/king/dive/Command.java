// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;

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
* <code>Command</code> is the interface for commands
* that are sent from the Master to the Slaves.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec 15 11:39:22 EST 2006
*/
public interface Command extends Serializable
{
    public void doCommand(Slave slave);
}//class

