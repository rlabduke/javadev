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
* <code>CosHelper</code> prints sin/cos values as 15 bit fixed decimals 
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb  1 16:04:42 EST 2005
*/
public class CosHelper //extends ... implements ...
{
    public static void main(String[] args)
    {
        System.out.print("cos_degrees = { ");
        for(int i = 0; i <= 20; i++)
        {
            if(i != 0) System.out.print(", ");
            System.out.print(  (int)Math.round(Math.cos(Math.toRadians(i)) * (1 << 15))  );
        }
        System.out.println(" }; ");
        
        System.out.print("sin_degrees = { ");
        for(int i = 0; i <= 20; i++)
        {
            if(i != 0) System.out.print(", ");
            System.out.print(  (int)Math.round(Math.sin(Math.toRadians(i)) * (1 << 15))  );
        }
        System.out.println(" }; ");

    }
}//class

