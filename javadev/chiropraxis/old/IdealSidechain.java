// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.sc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb.*;
import driftwood.r3.*;
//}}}
/**
* <code>IdealSidechain</code> is a class for working with
* ideal geometry side chains.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 14:08:45 EST 2003
*/
public class IdealSidechain //extends ... implements ...
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
    public IdealSidechain()
    {
    }
//}}}

//{{{ idealizeCB
//##################################################################################################
    /**
    * Given a heavy-atom backbone (N, CA, C)
    * this will reconstruct the C-beta and H-alpha(s)
    * if they already exist (are not null).
    * The existing sidechain will be rotated about the C-alpha
    * to bring it into the correct position.
    *
    * The reconstruction is fully generic and is not
    * adjusted for the type of residue under consideration.
    */
    public static void idealizeCB(AminoAcid aa)
    {
        Triple t1, t2;
        Builder build = new Builder();
        
        // Build an ideal C-beta and swing the side chain into place
        if(aa.CB != null)
        {
            // Construct ideal C-beta
            t1 = build.construct4(aa.N, aa.C, aa.CA, 1.536, 110.4, 123.1);
            t2 = build.construct4(aa.C, aa.N, aa.CA, 1.536, 110.6, -123.0);
            Triple idealCB = new Triple().likeMidpoint(t1, t2);
            
            // Construct rotation to align actual and ideal
            double theta = Triple.angle(idealCB, aa.CA, aa.CB);
            t1.likeNormal(idealCB, aa.CA, aa.CB).add(aa.CA);
            Transform xform = new Transform().likeRotation(aa.CA, t1, theta);
            
            // Apply the transformation
            for(Iterator iter = aa.sc.values().iterator(); iter.hasNext(); )
            { xform.transform((Atom)iter.next()); }
        }
        
        // Reconstruct alpha hydrogens
        if(aa.HA != null)
        {
            t1 = build.construct4(aa.N, aa.C, aa.CA, 1.100, 107.9, -118.3);
            t2 = build.construct4(aa.C, aa.N, aa.CA, 1.100, 108.1, 118.2);
            aa.HA.likeMidpoint(t1, t2).sub(aa.CA).unit().mult(1.100).add(aa.CA);
        }
        if(aa.HA1 != null)
        {
            t1 = build.construct4(aa.N, aa.C, aa.CA, 1.100, 109.3, -121.6);
            t2 = build.construct4(aa.C, aa.N, aa.CA, 1.100, 109.3, 121.6);
            aa.HA1.likeMidpoint(t1, t2).sub(aa.CA).unit().mult(1.100).add(aa.CA);
        }
        if(aa.HA2 != null)
        {
            t1 = build.construct4(aa.N, aa.C, aa.CA, 1.100, 109.3, 121.6);
            t2 = build.construct4(aa.C, aa.N, aa.CA, 1.100, 109.3, -121.6);
            aa.HA2.likeMidpoint(t1, t2).sub(aa.CA).unit().mult(1.100).add(aa.CA);
        }
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

