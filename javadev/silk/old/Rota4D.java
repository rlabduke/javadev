// (jEdit options) :folding=explicit:collapseFolds=1:
package boundrotamers;
//import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
//import javax.swing.*;

/**
 * <code>Rota4D</code> is a workbench for reading in angle data from tab files and manipulating it.
 * I keep changing what it does, so it's more a research platform than a real program moving toward a final form.
 *
<!--{{{ options -->
 * <p>Command-line options:
 * <ul>
 * <li><code>-h</code>, <code>-help</code>
 *   <br>Displays short message describing usage</li>
 * <li><code>-version</code>
 *   <br>Displays version and copyright information</li>
 * <li><code>-center0</code>
 *   <br>Plots angles on range from -180 to +180, instead of 0 to 360.<li>
 * <li><code>-fields=</code><i>field1</i>,<i>field2</i>,<i>field3</i>,<i>field4</i>,<i>fieldB</i>
 *   <br>Reads angles from fields 1, 2, 3, and 4 in a tab file. Filters data by requiring field B < some number specified with <code>-blt</code>
 * <li><code>-blt=</code><i>float_max_B</i>
 *   <br>Sets filter level for B factors (defaults to 30)</li>
 * <li><code>-name=</code><i>"Some description of this table"</i>
 *   <br>Provides a name for the saved data table</li>
 * <li><code>-output=</code><i>foobar</i>
 *   <br>Names output foobar.kin and foobar.ndft</li>
 * <li><code>-interval=</code><i>float_bin_width</i>
 *   <br>Sets the width of bins, i.e., spacing of sample points. Default is 10.0.</li>
 * <li><code>-mask=</code><i>const</i>,<i>k</i>
 *   <br>Sets constant and variable masking parameters.</li>
 * <li><code>-contour=</code><i>const_level_1</i>,<i>const_level_2</i>,<i>...</i>:<i>dd_level_1</i>,<i>dd_level_2</i>,<i>...</i>
 *   <br>Contours the plot at the specified levels. Each of the two lists may have 0 or more entries.
 *   Levels are specified as percentages of data enclosed, e.g. -contour:99.9,99,98,95,90.</li>
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
 * <p>Begun as SmoothHist2D on Thu Mar 21 10:18:34 EST 2002
 * <br>Became Rota2D on Wed Jul 10 15:46:51 EDT 2002
 * <br>Became Rota3D on Wed Jul 10 16:27:24 EDT 2002 
 * <br>Became Rota4D on Thu Jul 11 15:36:12 EDT 2002 
 * <br>Copyright (C) 2002 by Ian W. Davis
 * <br>Richardson laboratory, Duke University: <a href="http://kinemage.biochem.duke.edu">kinemage.biochem.duke.edu</a>
 *
<!--}}}-->
 */
public class Rota4D implements Version //extends ... implements ...
{
    public static void main(String[] args) { new Rota4D().Main(args); }

//{{{ Variables
//##################################################################################################
    // Data storage
    NDFloatTable currTable = null, prevTable = null;

    // 0-centered values
    float[] min0 = {-180.0f, -180.0f, -180.0f, -180.0f};
    float[] max0 = {180.0f, 180.0f, 180.0f, 180.0f};
    // 180-centered values
    float[] min180 = {0.0f, 0.0f, 0.0f, 0.0f};
    float[] max180 = {360.0f, 360.0f, 360.0f, 360.0f};
    // values actually used
    float[] min = min180;
    float[] max = max180;
    float[] tmin = null, tmax = null;
    // binning/sampling interval
    int[] bins = null;
    float binwidth = 10.0f;
    // wrap-around flag
    boolean[] wrapstar = {true, true, true, true};

    // All the points in our tab files
    ArrayList allData = null;

    // Flags and options
    String tableName = "Angle correlation plot";
    float bLessThan = 30f;
    int ang1FieldNumber = 0;
    int ang2FieldNumber = 1;
    int ang3FieldNumber = 2;
    int ang4FieldNumber = 3;
    int    bFieldNumber = 4;
    String outputPrefix = "output";
    String[] inputTabFiles = null;
    float mask_const = 10f, mask_k = 16f;
    float[] contourConst = { },
            contourDD    = { };
//}}}

//{{{ Main()
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main(String[] args)
    {
        parseArguments(args);

        if(inputTabFiles == null)
        {
            echo("*** No input files specified!");
            System.exit(1);
        }

        // Figure number of bins
        bins = new int[4];
        bins[0] = (int)((max[0] - min[0])/binwidth);
        bins[1] = (int)((max[1] - min[1])/binwidth);
        bins[2] = (int)((max[2] - min[2])/binwidth);
        bins[3] = (int)((max[3] - min[3])/binwidth);

        // Adjust table boundaries for compatibility with kinNDcont
        // We're going to wrap anyway, so this shouldn't matter.
        tmin = (float[])min.clone();
        tmax = (float[])max.clone();
        tmin[0] -= binwidth/2f; tmin[1] -= binwidth/2f; tmin[2] -= binwidth/2f; tmin[3] -= binwidth/2f;
        tmax[0] -= binwidth/2f; tmax[1] -= binwidth/2f; tmax[2] -= binwidth/2f; tmax[3] -= binwidth/2f;
        
        // Load all data points
        allData = new ArrayList(20000);
        readDataPoints(bLessThan); // B < 30
        echo("# "+allData.size()+" data elements for "+outputPrefix);

        // Process data for constant mask
        MaskSpec msConst  = new MaskSpec(mask_const);
        currTable = traceDensity(msConst);
        sortDataPoints(currTable);
        writeKinContours(contourConst, currTable, outputPrefix);

        prevTable = currTable;
        MaskSpec msVar = new MaskSpec(binwidth, 4, mask_k);
        currTable = traceDensityDD(prevTable, msVar);
        sortDataPoints(currTable);

        /* Convert from density values to fraction enclosed, then save */
        float[] allDataSamples = new float[allData.size()];
        for(int i = 0; i < allDataSamples.length; i++) allDataSamples[i] = ((TabEntry)allData.get(i)).density;
        currTable.fractionLessThan(allDataSamples);
        /* Convert from density values to fraction enclosed, then save */
        
        writeKinContours(contourDD, currTable, outputPrefix);

        // Save our smoothed table to a file
        currTable.setName(tableName+" [v"+VERSION+"]");
        try {
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputPrefix+".data")));
            currTable.writeText(ps);
            ps.close();
        } catch(IOException ex) { echo("*** Error saving table!"); }
        try {
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputPrefix+".ndft")));
            currTable.writeBinary(dos);
            dos.close();
        } catch(IOException ex) { echo("*** Error saving table!"); }
    }
//}}}

//{{{ readDataPoints()
//##################################################################################################
    void readDataPoints(float cutoff)
    {
        String s;
        String[] fields;
        TabEntry te;

        //loop over files
        for(int i = 0; i < inputTabFiles.length; i++)
        {
            try {
                BufferedReader datafile = new BufferedReader(new FileReader(inputTabFiles[i]));

                //for each line
                while( (s = datafile.readLine()) != null )
                {
                    // The modern way of extracting fields from a colon-separated line
                    fields = StringManip.explode(s, ':');

                    try {
                        // Attempt to parse the values in fields 6 (chi 1) and 7 (chi 2) (numbering fields from 0)
                        te = new TabEntry();
                        te.ang = new float[4];
                        te.ang[0] = Float.parseFloat(fields[ang1FieldNumber]);
                        te.ang[1] = Float.parseFloat(fields[ang2FieldNumber]);
                        te.ang[2] = Float.parseFloat(fields[ang3FieldNumber]);
                        te.ang[3] = Float.parseFloat(fields[ang4FieldNumber]);

                        te.Bfactor = Float.parseFloat(fields[bFieldNumber]); // side chain B, as it stands now

                        // Make chi's lie in the range [0,+360]
                        if(te.ang[0] < min[0]) te.ang[0] += 360.0f;
                        if(te.ang[1] < min[1]) te.ang[1] += 360.0f;
                        if(te.ang[2] < min[2]) te.ang[2] += 360.0f;
                        if(te.ang[3] < min[3]) te.ang[3] += 360.0f;

                        // Identify where this data point came from
                        te.name = fields[0]+":"+fields[1]+":"+fields[2]+":"+fields[3];

                        if(te.Bfactor < cutoff)
                        {
                            allData.add(te);
                        }
                        
                    } catch(NumberFormatException ex) {}
                }//for each line

                datafile.close();

            } catch(IOException ex) {
                echo("*** Problems reading the file '"+inputTabFiles[i]+"'.");
                System.exit(1);
            }
        }//loop over files
        echo(""); // for the point-counting line
    }
//}}}

//{{{ traceDensity[DD]
//##################################################################################################
    // Do a simple density trace
    NDFloatTable traceDensity(MaskSpec ms)
    {
        int sz = allData.size();
        TabEntry te;
        NDFloatTable ft = new NDFloatTable("traceDensity", 4, tmin, tmax, bins, wrapstar);

        for(int i = 0; i < sz; i++)
        {
            te = (TabEntry)allData.get(i);
            //ft.tallyGaussian(te.ang, ms.getMask(0));
            ft.tallyCosine(te.ang, ms.getMask(0));
            if((i % 100) == 0) echon("#");
        }
        echo("");

        ft.normalize();

        return ft;
    }

    // Do a density-dependent density trace
    NDFloatTable traceDensityDD(NDFloatTable density, MaskSpec ms)
    {
        int sz = allData.size();
        TabEntry te;
        NDFloatTable ft = new NDFloatTable("traceDensityDD", 4, tmin, tmax, bins, wrapstar);
        float mask;

        for(int i = 0; i < sz; i++)
        {
            te = (TabEntry)allData.get(i);
            //ft.tallyGaussian(te.ang, ms.getMask(density.valueAt(te.ang)));
            mask = ms.getMask(density.valueAt(te.ang));
            //echo("m: "+mask);
            //echo("d: "+density.valueAt(te.ang));
            ft.tallyCosine(te.ang, mask);
            if((i % 100) == 0) echon("#");
        }
        echo("");

        ft.normalize();

        return ft;
    }
//}}}

//{{{ sort, fractions, etc.
//##################################################################################################
    // Calculates the density for each data point and puts them in ascending order...
    void sortDataPoints(NDFloatTable table)
    {
        TabEntry te;
        for(Iterator iter = allData.iterator(); iter.hasNext(); )
        {
            te = (TabEntry)iter.next();
            te.density = table.valueAt(te.ang);
        }
        Collections.sort(allData);
    }
    
    // Calculate the highest level that will still include at least the specified fraction of points (0 <= level <= 1)
    float getLevel(float frac)
    {
        int index = (int)( (1f-frac) * (allData.size()-1) );
        return ((TabEntry)(allData.get(index))).density;
    }
    
//##################################################################################################
    // Count the fraction of points that fall within a given contour
    float frac_ge_level(NDFloatTable ft, float level)
    {
        int cnt = 0;
        TabEntry te;

        for(Iterator iter = allData.iterator(); iter.hasNext(); )
        {
            te = (TabEntry)iter.next();
            if(ft.valueAt(te.ang) >= level) cnt++;
        }

        return (float)cnt / (float)allData.size();
    }

//}}}

//{{{ writeKinContours
//##################################################################################################
    void writeKinContours(float[] levels, NDFloatTable table, String lvlName)
    {
        if(levels.length < 1) return;
        for(int i = 0; i < levels.length; i++)
        {
            float level = 1f - levels[i]/100f;
            //float level = getLevel(levels[i]/100f);
            echo("# Actual fraction: "+frac_ge_level(table, level)+"\n"+lvlName+i+" = "+level);
        }
    }
//}}}

//{{{ echo
//##################################################################################################
    // Convenience function for debugging
    void echo(String s) { System.err.println(s); }
    void echon(String s) { System.err.print(s); }
    
    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ parseArguments()
//##################################################################################################
    // Interpret command-line arguments
    void parseArguments(String[] args)
    {
        Vector files = new Vector();

        // iterate thru all command line params
        for(int i = 0; i < args.length; i++)
        {
            // consumed by another option
            if(args[i] == null) {}
            // this is an option
            else if(args[i].startsWith("-"))
            {
                try {
                    if(args[i].equals("-h") || args[i].equals("-help")) {
                        echo("Rota4D: Does 3-D angle correlation plots & generates cosine-smoothed\ndensity traces from them.");
                        echo("Try reading the JavaDoc documentation or the README file.");
                        System.exit(0);
                    } else if(args[i].equals("-center0")) {
                        min = min0;
                        max = max0;
                    } else if(args[i].startsWith("-name=")) {
                        tableName = args[i].substring(args[i].indexOf('=')+1);
                    } else if(args[i].startsWith("-output=")) {
                        outputPrefix = args[i].substring(args[i].indexOf('=')+1);
                    } else if(args[i].startsWith("-fields=")) {
                        int[] fields = StringManip.explodeInts(args[i].substring(args[i].indexOf('=')+1), " ,;:");
                        ang1FieldNumber = fields[0];
                        ang2FieldNumber = fields[1];
                        ang3FieldNumber = fields[2];
                        ang4FieldNumber = fields[3];
                           bFieldNumber = fields[4];
                    } else if(args[i].startsWith("-mask=")) {
                        float[] masks = StringManip.explodeFloats(args[i].substring(args[i].indexOf('=')+1), " ,;:");
                        mask_const = masks[0];
                        mask_k     = masks[1];
                    } else if(args[i].startsWith("-contour=")) {
                        String conts = args[i].substring(args[i].indexOf('=')+1);
                        contourConst = StringManip.explodeFloats( conts.substring(0, conts.indexOf(':')), " ,");
                        contourDD    = StringManip.explodeFloats( conts.substring(conts.indexOf(':')+1), " ,");
                    } else if(args[i].startsWith("-blt=")) {
                        String s = args[i].substring(args[i].indexOf('=')+1);
                        bLessThan = Float.parseFloat(s);
                    } else if(args[i].startsWith("-interval=")) {
                        String s = args[i].substring(args[i].indexOf('=')+1);
                        binwidth = Float.parseFloat(s);
                    } else if(args[i].equals("-version")) {
                        echo("Rota4D, version "+VERSION+"\nCopyright (C) 2002 by Ian W. Davis");
                        System.exit(0);
                    } else {
                        echo("*** Unrecognized option: "+args[i]);
                    }
                }//try
                catch(IndexOutOfBoundsException ex)
                    { echo("*** Bad option '"+args[i]+"'. Use -help for details."); }
                catch(NumberFormatException ex)
                    { echo("*** Bad option '"+args[i]+"'. Use -help for details."); }
            }
            // this is a file, etc.
            else
            {
                files.add(args[i]);
            }
        }

        // copy file names to an array
        int sz = files.size();
        if(sz > 0)
        {
            inputTabFiles = new String[sz];
            for(int i = 0; i < sz; i++) inputTabFiles[i] = (String)files.elementAt(i);
        }
    }
//}}}
}//class
