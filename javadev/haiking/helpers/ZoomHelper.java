// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
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
* <code>ZoomHelper</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Mar  2 09:31:22 EST 2005
*/
public class ZoomHelper //extends ... implements ...
{
    public static void main(String[] args)
    {
        final double zoomBits = 26;
        final double stepsToDouble = 3;
        System.out.print("    static final int[] ZOOM_FACTORS = { ");
        for(int i = 1; i <= zoomBits*stepsToDouble; i++)
        {
            if(i != 1) System.out.print(", ");
            System.out.print(  (int)Math.round(Math.pow(2.0, i/stepsToDouble))  );
        }
        System.out.println(" }; ");
    }
}//class

