// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb;

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
* <code>TryReadPdb</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 18 09:36:32 EST 2003
*/
public class TryReadPdb //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public TryReadPdb()
    {
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Main() and main()
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main(String[] args)
    {
        PDBFile pdbFile = new PDBFile();
        try { pdbFile.read(args[0]); }
        catch(IOException ex) {ex.printStackTrace();}
    }

    public static void main(String[] args)
    {
        TryReadPdb mainprog = new TryReadPdb();
        mainprog.Main(args);
    }
//}}}
}//class

