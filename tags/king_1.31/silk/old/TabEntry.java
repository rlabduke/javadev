package boundrotamers;
//import java.awt.*;
//import java.io.*;
//import java.util.*;
//import javax.swing.*;

/**
 * <code>TabEntry</code> is a wrapper class for the data we parse out of tab files.
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
 * <p>Begun on Mon Apr  8 13:11:16 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis
 * <br>Richardson laboratory, Duke University: <a href="http://kinemage.biochem.duke.edu">kinemage.biochem.duke.edu</a>
 *
 */
public class TabEntry implements Cloneable, Comparable //extends ... implements ...
{
    public String name = null; //deprecated!
    public String chain = null, number = null, type = null;
    public float[] ang = null;
    public float Bfactor = 0f;
    public float density = 0f;

    public Object clone()
    {
        TabEntry te = new TabEntry();
        te.name = this.name;
        te.chain = this.chain;
        te.number = this.number;
        te.type = this.type;
        te.Bfactor = this.Bfactor;
        te.ang = (float[])this.ang.clone();
        
        return te;
    }
    
    /**
    * The natural ordering of tab entries sorts them by the density they find themselves in, lowest to highest.
    * Note: this class has a natural ordering that is inconsistent with equals().
    */
    public int compareTo(Object ob)
    {
        float obden = ((TabEntry)ob).density;
        if(obden == density) return 0;
        else if(obden > density) return -1;
        else return 1;
    }

}//class
