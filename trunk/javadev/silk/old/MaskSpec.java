package boundrotamers;
//import java.awt.*;
//import java.io.*;
//import java.util.*;
//import javax.swing.*;

/**
 * <code>MaskSpec</code> encapsulates the parameters for a density-dependent mask width-determining function.
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
 * <p>Begun on Thu Mar 28 17:27:11 EST 2002
 * <br>Copyright (C) 2002 by Ian W. Davis
 * <br>Richardson laboratory, Duke University: <a href="http://kinemage.biochem.duke.edu">kinemage.biochem.duke.edu</a>
 *
 */
public class MaskSpec //extends ... implements ...
{
//##################################################################################################
    boolean isConstant = false;
    float   constValue = 0f;
    
    float maskScale = 1f, power = 0.5f;
//##################################################################################################
    /**
    * Specifies a new mask and its properties.
    *
    */
    public MaskSpec(float binwidth, int ndim, float k)
    {
        // Ignore the following -- now outdated
        //-----
        // densityScale: expectation is that each table will be on
        // same scale as a histogram; each tallied point contributes
        // binwidth^n units of area/volume/etc.
        //densityScale = (float)Math.pow(binwidth, ndim);
        //-----
        maskScale = k;
        power = 0.5f / ndim;
    }

    /**
    * Specified a new mask of constant radius.
    *
    * @param width the constant radius of this mask
    */
    public MaskSpec(float width)
    {
        isConstant = true;
        constValue = width;
    }

//##################################################################################################
    /**
    * Returns a mask width based on a given density.
    *
    * @param density data density at the point in question
    * @return a mask width at the current location
    */
    float getMask(float density)
    {
        if(isConstant) return constValue;
        else
        {
            //float mask = maskScale / (float)Math.pow((density/densityScale), power);
            float mask = maskScale / (float)Math.pow((density), power);
            return mask;
        }
    }

//##################################################################################################
    // Convenience function for debugging
    void echo(String s) { System.err.println(s); }

}//class
