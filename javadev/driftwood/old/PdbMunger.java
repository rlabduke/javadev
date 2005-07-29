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
* <code>PdbMunger</code> reads a PDB file on stdin,
* creates an internal data structure,
* and writes it back out to stdout.
*
* <p>This provides a way to judge how badly or how well
* the information is preserved.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jun 27 13:27:54 EDT 2003
*/
public class PdbMunger //extends ... implements ...
{
    public static void main(String[] args) throws IOException
    {
        PdbReader   reader  = new PdbReader();
        CoordinateFile cf   = reader.read(System.in);
        PdbWriter   writer  = new PdbWriter(System.out);
        writer.writeCoordinateFile(cf, new HashMap());
    }
}//class

