// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk;

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
* <code>KinfilePlotter</code> does simple plotting tasks that
* kin2Dcont can't do.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Apr 16 15:42:40 EDT 2003
*/
public class KinfilePlotter //extends ... implements ...
{
//{{{ Constants
    static final String[] colors = {"pink", "peach", "yellow", "sea", "sky", "lilac"};
//}}}

//{{{ Variable definitions
//##################################################################################################
    DecimalFormat df = new DecimalFormat("0.#####");
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public KinfilePlotter()
    {
    }
//}}}

//{{{ plot1D
//##################################################################################################
    /** Writes a kinemage dot for every sample */
    public void plot1D(OutputStream out, Collection dataSamples)
    {
        PrintStream ps = new PrintStream(out);
        ps.println("@group {data} dominant animate");
        ps.println("@subgroup {1-D data}");
        ps.println("@dotlist {"+dataSamples.size()+" pts}");
        DataSample sample;
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            ps.println("{"+sample.label+"}"+sample.color+" "+df.format(sample.coords[0])+" 0 0");
        }
        ps.flush();
    }
//}}}

//{{{ plot2D
//##################################################################################################
    /** Writes a kinemage dot for every sample */
    public void plot2D(OutputStream out, Collection dataSamples)
    {
        PrintStream ps = new PrintStream(out);
        ps.println("@group {data} dominant animate");
        ps.println("@subgroup {2-D data}");
        ps.println("@dotlist {"+dataSamples.size()+" pts}");
        DataSample sample;
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            ps.println("{"+sample.label+"}"+sample.color+" "+df.format(sample.coords[0])+" "+df.format(sample.coords[1])+" 0");
        }
        ps.flush();
    }
//}}}

//{{{ plot3D
//##################################################################################################
    /** Writes a kinemage dot for every sample */
    public void plot3D(OutputStream out, Collection dataSamples)
    {
        PrintStream ps = new PrintStream(out);
        ps.println("@group {data} dominant animate");
        ps.println("@subgroup {3-D data}");
        ps.println("@dotlist {"+dataSamples.size()+" pts}");
        DataSample sample;
        for(Iterator iter = dataSamples.iterator(); iter.hasNext(); )
        {
            sample = (DataSample)iter.next();
            ps.println("{"+sample.label+"}"+sample.color+" "+df.format(sample.coords[0])+" "+df.format(sample.coords[1])+" "+df.format(sample.coords[2]));
        }
        ps.flush();
    }
//}}}

//{{{ contour1D
//##################################################################################################
    void contour1D(OutputStream out, double[] levels, NDimTable table)
    {
        if(levels == null || levels.length < 1) return;
        
        PrintStream ps = new PrintStream(out);
        ps.println("@group {contours}");
        ps.println("@subgroup {height trace} dominant");
        
        int i;
        double val, level;
        double[] j = new double[1];
        boolean toggle;

        double min = table.getMinBounds()[0];
        double max = table.getMaxBounds()[0];
        double binwidth = (max-min) / table.getBins()[0];
        
        // trace of data points
        ps.println("@vectorlist {height trace} color= gray width= 1");
        for(j[0] = min; j[0] <= max; j[0] += binwidth)
        {
            val = table.valueAt(j);
            ps.println("{} "+df.format(j[0])+" "+val+" 0");
        }
        
        // bars and tics
        for(i = 0; i < levels.length; i++)
        {
            level = levels[i];
            ps.println("@subgroup {"+df.format(level)+"} dominant");
            ps.println("@vectorlist {bar: "+level+"} master= {bars} width= 1 color= "+colors[i%colors.length]);
            ps.println("{}P "+df.format(min)+" "+df.format(level)+" 0");
            ps.println("{} "+df.format(max)+" "+df.format(level)+" 0");
            
            toggle = false;
            ps.println("@vectorlist {tic: "+level+"} master= {tics} width= 1 color= "+colors[i%colors.length]);
            for(j[0] = min; j[0] <= max; j[0] += binwidth/10f)
            {
                val = table.valueAt(j);
                if((val >= level) != toggle)
                {
                    toggle = (val >= level);
                    ps.println("{}P "+df.format(j[0])+" 0 0");
                    ps.println("{} "+df.format(j[0])+" "+df.format(level)+" 0");
                }
            }
        }//for(each level)
        
        ps.flush();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

