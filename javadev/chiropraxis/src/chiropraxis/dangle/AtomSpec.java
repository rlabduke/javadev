// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.dangle;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>AtomSpec</code> takes strings like "_CA_" and matches
* them to atom names like " CA ".
* For nucleic acids, the symbols * and ' are treated as equivalent.
* Regular expressions can be specified by enclosing the regex in slashes (/.../).
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 11:18:34 EST 2007
*/
public class AtomSpec extends XyzSpec
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    int resOffset;
    Matcher regexName;
    String starName, primeName;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AtomSpec(int resOffset, String atomName)
    {
        super();
        this.resOffset  = resOffset;
        
        atomName = atomName.replace('_', ' ');
        this.primeName  = atomName.replace('*', '\'');
        this.starName   = primeName.replace('\'', '*');
        
        if(atomName.startsWith("/") && atomName.endsWith("/"))
        {
            this.regexName  = Pattern.compile(atomName.substring(1,atomName.length()-1)).matcher("");
            this.primeName  = atomName;
            this.starName   = atomName;
        }
        else
        {
            this.regexName  = null;
            this.primeName  = atomName.replace('*', '\'');
            this.starName   = primeName.replace('\'', '*');
        }
    }
//}}}

//{{{ get
//##############################################################################
    /**
    * @return the AtomState specified by this spec, or null if it can't be found.
    */
    public AtomState get(Model model, ModelState state, Residue curr)
    {
        Residue res = getRes(model, state, curr);
        if(res == null) return null;
        
        Atom atom = null;
        if(regexName == null)
        {
            atom = res.getAtom(starName);
            if(atom == null) atom = res.getAtom(primeName);
        }
        else
        {
            for(Iterator iter = res.getAtoms().iterator(); iter.hasNext() && atom == null; )
            {
                Atom a = (Atom) iter.next();
                if(regexName.reset(a.getName()).matches())
                    atom = a;
            }
        }
        if(atom == null) return null;
        try { return state.get(atom); }
        catch(AtomException ex) { return null; }
    }
//}}}

//{{{ getAll
//##############################################################################
    /**
    * @return all the AtomStates specified by this spec, or the empty set.
    *   This only makes sense really if the spec is a regular expression.
    */
    public Collection getAll(Model model, ModelState state, Residue curr)
    {
        Residue res = getRes(model, state, curr);
        if(res == null) return Collections.emptySet();
        
        if(regexName == null)
        {
            Atom atom = res.getAtom(starName);
            if(atom == null) atom = res.getAtom(primeName);
            if(atom == null) return Collections.emptySet();
            try { return Collections.singleton(state.get(atom)); }
            catch(AtomException ex) { return Collections.emptySet(); }
        }
        else
        {
            ArrayList all = new ArrayList();
            for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
            {
                Atom a = (Atom) iter.next();
                if(regexName.reset(a.getName()).matches())
                {
                    try { all.add(state.get(a)); }
                    catch(AtomException ex) {}
                }
            }
            return all;
        }
    }
//}}}

//{{{ getRes
//##############################################################################
    private Residue getRes(Model model, ModelState state, Residue curr)
    {
        Residue res = curr;
        for(int i = 0, end_i = Math.abs(resOffset); i < end_i; i++)
        {
            Residue old = res;
            if(resOffset > 0) res = curr.getNext(model); // forward search
            else res = curr.getPrev(model); // backward search
            if(res == null) return null;
            
            if(resOffset > 0)   { if(!checkConnection(model, state, old, res)) return null; }
            else                { if(!checkConnection(model, state, res, old)) return null; }
        }
        return res;
    }
//}}}

//{{{ checkConnection
//##############################################################################
    private boolean checkConnection(Model model, ModelState state, Residue first, Residue second)
    {
        Atom c = null, n = null, o = null, p = null;
        c = first.getAtom(" C  ");
        n = second.getAtom(" N  ");
        if(c != null && n != null)
        {
            try
            {
                AtomState a1 = state.get(c);
                AtomState a2 = state.get(n);
                return (a1.sqDistance(a2) <= 4.00); // from Molikin
            }
            catch(AtomException ex) { return false; }
        }
        o = first.getAtom(" O3*");
        if(o == null) o = first.getAtom(" O3'");
        p = second.getAtom(" P  ");
        if(o != null && p != null)
        {
            try
            {
                AtomState a1 = state.get(o);
                AtomState a2 = state.get(p);
                return (a1.sqDistance(a2) <= 4.84); // from Molikin
            }
            catch(AtomException ex) { return false; }
        }
        return false;
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        String res = "i";
        if(resOffset > 0)       res += "+" + resOffset;
        else if(resOffset < 0)  res += resOffset;
        return res+" "+starName.replace(' ', '_');
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

