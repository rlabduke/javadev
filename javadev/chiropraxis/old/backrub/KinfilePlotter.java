// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.backrub;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb.*;
//}}}
/**
* <code>KinfilePlotter</code> implements Plotter for kinemage files.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 18 14:05:37 EST 2003
*/
public class KinfilePlotter
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("0.#####");
//}}}

//{{{ Variable definitions
//##################################################################################################
    PrintStream     out;
    String          plotName        = "backbone";
    String          mainColor       = "yellowtint";
    String          sideColor       = "green";
    String          hyColor         = "gray";
    int             plotWidth       = 4;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public KinfilePlotter(OutputStream out, boolean append)
    {
        this.out = new PrintStream(out);
        if(!append)
        {
            this.out.println("@kinemage 1");
            this.out.println("@group {x}");
        }
    }
//}}}

//{{{ plotBackbone
//##################################################################################################
    /**
    * Creates a plot of the given protein backbone residues
    * @param aaBackbones a Collection of AminoAcid objects to plot sequentially,
    *   ordered from N to C
    */
    public void plotBackbone(Collection aaBackbones)
    {
        Atom a;
        AminoAcid mc;
        DecimalFormat df = new DecimalFormat("0.####");
        out.println("@vectorlist {"+plotName+"} color= "+mainColor+" width= "+plotWidth);
        
        for(Iterator iter = aaBackbones.iterator(); iter.hasNext(); )
        {
            mc = (AminoAcid)iter.next();
            a = mc.N; out.println("{"+a.getID()+"} "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            if(mc.H != null)
            {
                a = mc.H; out.println("{"+a.getID()+"}"+hyColor+" "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
                a = mc.N; out.println("{"+a.getID()+"}P "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            }
            
            a = mc.CA; out.println("{"+a.getID()+"} "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            if(mc.CB != null)
            {
                a = mc.CB; out.println("{"+a.getID()+"}"+sideColor+" "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
                a = mc.CA; out.println("{"+a.getID()+"}P "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            }
            if(mc.HA != null)
            {
                a = mc.HA; out.println("{"+a.getID()+"}"+hyColor+" "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
                a = mc.CA; out.println("{"+a.getID()+"}P "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            }
            if(mc.HA1 != null)
            {
                a = mc.HA1; out.println("{"+a.getID()+"}"+hyColor+" "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
                a = mc.CA;  out.println("{"+a.getID()+"}P "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            }
            if(mc.HA2 != null)
            {
                a = mc.HA2; out.println("{"+a.getID()+"}"+hyColor+" "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
                a = mc.CA;  out.println("{"+a.getID()+"}P "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            }
            
            a = mc.C; out.println("{"+a.getID()+"} "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            a = mc.O; out.println("{"+a.getID()+"} "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
            a = mc.C; out.println("{"+a.getID()+"}P "+df.format(a.getX())+" "+df.format(a.getY())+" "+df.format(a.getZ()));
        }
        
        out.flush();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

