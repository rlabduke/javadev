package boundrotamers;
//import java.awt.*;
import java.io.*;
import java.text.*;
//import javax.swing.*;

/**
 * <code>RamaStat</code> calculates statistics for a tab file by comparing it to an NDFloatTable.
 *
 * <p>Command-line options (see code for more):
 * <ul>
 * <li><code>-h</code>, <code>-help</code>
 *   <br>Displays short message describing usage</li>
 * <li><code>-version</code>
 *   <br>Displays version and copyright information</li>
 * </ul>
 *
<br><pre>
/----------------------------------------------------------------------\
| This program is free software; you can redistribute it and/or modify |
| it under the terms of the GNU General Public License as published by |
| the Free Software Foundation; either version 2 of the License, or    |
| (at your option) any later version.                                  |
|                                                                      |
| This program is distributed in the hope that it will be useful,      |
| but WITHOUT ANY WARRANTY; without even the implied warranty of       |
| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        |
| GNU General Public License for more details.                         |
\----------------------------------------------------------------------/
</pre>
 *
 * <p>Begun on Thu Apr 25 11:25:36 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis
 * <br>Richardson laboratory, Duke University: <a href="http://kinemage.biochem.duke.edu">kinemage.biochem.duke.edu</a>
 *
 */
public class RamaStat //extends ... implements ...
{
    public static void main(String[] args) { new RamaStat(args).Main(); }

//##################################################################################################
    // Variables
    // go
    // here
    String ndftFile = "general.ndft";
    int fieldPhi = 0;
    int fieldPsi = 1;
    int fieldB   = 2;
    int fieldRes = 3;
    float minB = 0f, maxB = 30f, minRes = 0f, maxRes = 2.0f, level = 0f;

//##################################################################################################
    /**
    * Constructor
    */
    public RamaStat(String[] args)
    {
        parseArguments(args);
    }

//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        // Load the table
        NDFloatTable table = null;
        try {
            DataInputStream dis;
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(ndftFile)));
            table = new NDFloatTable(dis);
            dis.close();
        } catch(IOException ex) {
            echo("Error loading table of Ramachandran data!");
            System.exit(1);
        }
        
        // Start reading tab file
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        String s;
        String[] fields;
        float[] angle = new float[2];
        float B, res;
        float nPoints = 0f, nOK = 0f, nInside = 0f;
        DecimalFormat df = new DecimalFormat("##0.000");
        
        echo("");
        echo("Collecting statistics on residues with phi and psi present and:");
        echo("    "+minB+" <= B-factor < "+maxB);
        echo("    "+minRes+" < resolution <= "+maxRes);
        echo("    Table value at (phi,psi) >= "+level);
        
        try {
            while( (s = in.readLine()) != null )
            {
                try {
                    fields = StringManip.explode(s, ':');
                    angle[0] = Float.parseFloat(fields[fieldPhi]);
                    angle[1] = Float.parseFloat(fields[fieldPsi]);
                    B        = Float.parseFloat(fields[fieldB]);
                    res      = Float.parseFloat(fields[fieldRes]);
                    
                    nPoints++;
                    if(B >= minB && B < maxB && res > minRes && res <= maxRes)
                    {
                        nOK++;
                        if(table.valueAt(angle) >= level) nInside++;
                    }                    
                }//try
                catch(NumberFormatException ex) {}
            }//while
        }//try
        catch(IOException ex) { echo("Aborted due to I/O error."); }
        
        echo(df.format((nOK*100f/nPoints))+"% of valid points passed resolution and B-factor criteria ("+(int)nOK+" out of "+(int)nPoints+")");
        echo(df.format((nInside*100f/nOK))+"% of those were inside the specified contour level ("+(int)nInside+" out of "+(int)nOK+")");
    }

//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.out.println(s); } // like Unix 'echo'
    void echon(String s) { System.out.print(s); }  // like Unix 'echo -n'

//##################################################################################################
    // Interpret command-line arguments
    void parseArguments(String[] args)
    {
        String arg;
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            // this is an option
            if(arg.startsWith("-"))
            {
                try {
                    if(arg.equals("-h") || arg.equals("-help")) {
                        echo("Help not available. Sorry!");
                        System.exit(0);
                    } else if(arg.equals("-version")) {
                        echo("RamaStat, version "+Version.VERSION+"\nCopyright (C) 2002 by Ian W. Davis");
                        System.exit(0);
                    } else if(arg.startsWith("-field")) {
                        int[] fields = StringManip.explodeInts(arg.substring(arg.indexOf('=')+1), " ,;:");
                        fieldPhi = fields[0];
                        fieldPsi = fields[1];
                        fieldB   = fields[2];
                        fieldRes = fields[3];
                    } else if(arg.startsWith("-B")) {
                        float[] fields = StringManip.explodeFloats(arg.substring(arg.indexOf('=')+1), " ,;:");
                        minB = fields[0];
                        maxB = fields[1];
                    } else if(arg.startsWith("-res")) {
                        float[] fields = StringManip.explodeFloats(arg.substring(arg.indexOf('=')+1), " ,;:");
                        minRes = fields[0];
                        maxRes = fields[1];
                    } else if(arg.startsWith("-table")) {
                        ndftFile = arg.substring(arg.indexOf('=')+1);
                    } else if(arg.startsWith("-level")) {
                        level = Float.parseFloat(arg.substring(arg.indexOf('=')+1));
                    } else {
                        echo("*** Unrecognized option: "+arg);
                    }
                }
                catch(IndexOutOfBoundsException ex)
                    { echo("*** Bad option '"+arg+"'. Use -help for details."); }
                catch(NumberFormatException ex)
                    { echo("*** Bad option '"+arg+"'. Use -help for details."); }
            }
            // this is a file, etc.
            else
            {
            }
        }
    }

}//class
