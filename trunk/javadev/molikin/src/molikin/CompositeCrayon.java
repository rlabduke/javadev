// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>CompositeCrayon</code> strings together the output of several
* AtomCrayons or BondCrayons
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Nov 10 11:01:19 EST 2005
*/
public class CompositeCrayon //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
AtomCrayon[]    atomCrayons     = {};
BondCrayon[]    bondCrayons     = {};
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CompositeCrayon()
    {
        super();
    }
//}}}

//{{{ addAtomCrayon, colorAtom
//##############################################################################
    public void addAtomCrayon(AtomCrayon crayon)
    {
        int len = atomCrayons.length;
        AtomCrayon[] moreCrayons = new AtomCrayon[ len+1 ];
        System.arraycopy(atomCrayons, 0, moreCrayons, 0, len);
        moreCrayons[ len ] = crayon;
        atomCrayons = moreCrayons;
    }
    
    public String colorAtom(AtomState as)
    {
        int len = atomCrayons.length;
        if(len == 0) return "";
        else if(len == 1) return atomCrayons[0].colorAtom(as);
        else
        {
            StringBuffer buf = new StringBuffer();
            for(int i = 0; i < len; i++)
            {
                String out = atomCrayons[i].colorAtom(as);
                if(out.length() > 0)
                {
                    if(buf.length() > 0) buf.append(" ");
                    buf.append(out);
                }
            }
            return buf.toString();
        }
    }
//}}}

//{{{ addBondCrayon, colorBond
//##############################################################################
    public void addBondCrayon(BondCrayon crayon)
    {
        int len = bondCrayons.length;
        BondCrayon[] moreCrayons = new BondCrayon[ len+1 ];
        System.arraycopy(bondCrayons, 0, moreCrayons, 0, len);
        moreCrayons[ len ] = crayon;
        bondCrayons = moreCrayons;
    }
    
    public String colorBond(AtomState from, AtomState toward)
    {
        int len = bondCrayons.length;
        if(len == 0) return "";
        else if(len == 1) return bondCrayons[0].colorBond(from, toward);
        else
        {
            StringBuffer buf = new StringBuffer();
            for(int i = 0; i < len; i++)
            {
                String out = bondCrayons[i].colorBond(from, toward);
                if(out.length() > 0)
                {
                    if(buf.length() > 0) buf.append(" ");
                    buf.append(out);
                }
            }
            return buf.toString();
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

