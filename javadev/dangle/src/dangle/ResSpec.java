// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package dangle;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.Triple;
//}}}
/**
* <code>ResSpec</code> takes strings like "_ZN" and matches
* them to residue names like " ZN".
* Regular expressions can be specified by enclosing the regex in slashes (/.../).
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 15 11:18:34 EST 2007
*/
public class ResSpec //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String      origResName; // just used for a pretty toString()
    int         resOffset;
    Matcher     regexName;
    boolean     requireCis;
    boolean     require2prime;
    boolean     requireDeoxy;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * @param requireCis if true, requires that the peptide bond preceding the residue
    * be cis rather than trans.  If false, doesn't care whether cis or trans.
    */
    public ResSpec(int resOffset, boolean requireCis, boolean require2prime, boolean requireDeoxy, String resName)
    {
        super();
        this.resOffset      = resOffset;
        this.requireCis     = requireCis;
        this.require2prime  = require2prime;
        this.requireDeoxy   = requireDeoxy;
        this.origResName    = resName;
        
        resName = resName.replace('_', ' ');
        
        if(resName.startsWith("/") && resName.endsWith("/"))
            this.regexName = Pattern.compile(resName.substring(1,resName.length()-1)).matcher("");
        else
            this.regexName = Pattern.compile(Pattern.quote(resName)).matcher("");
    }
//}}}

//{{{ isMatch
//##############################################################################
    public boolean isMatch(Model model, ModelState state, Residue curr)
    {
        curr = getRes(model, state, curr, this.resOffset);
        if(curr == null) return false;

        if(requireCis)
        {
            Residue prev = getRes(model, state, curr, -1);
            if(prev == null) return false;
            try
            {
                AtomState prevCa = state.get(prev.getAtom(" CA "));
                AtomState prevC  = state.get(prev.getAtom(" C  "));
                AtomState currN  = state.get(curr.getAtom(" N  "));
                AtomState currCa = state.get(curr.getAtom(" CA "));
                double omega = Triple.dihedral(prevCa, prevC, currN, currCa);
                if(omega < -90 || omega > 90) return false;
            }
            catch(AtomException ex)
            { return false; }
        }
        
        if(require2prime)
        {
            System.err.println(
                "RNA 2'-pucker-specific bond lengths & angles not yet implemented!");
            return false;
        }
        
        if(requireDeoxy)
        {
            System.err.println("DNA-specific bond lengths & angles not yet implemented!");
            return false;
        }
        
        return regexName.reset(curr.getName()).matches();
    }
//}}}

//{{{ getRes
//##############################################################################
    private Residue getRes(Model model, ModelState state, Residue curr, int resOff)
    {
        Residue res = curr;
        for(int i = 0, end_i = Math.abs(resOff); i < end_i; i++)
        {
            Residue old = res;
            if(resOff > 0) res = curr.getNext(model); // forward search
            else res = curr.getPrev(model); // backward search
            if(res == null) return null;
            
            if(resOff > 0)  { if(!checkConnection(model, state, old, res)) return null; }
            else            { if(!checkConnection(model, state, res, old)) return null; }
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
        return "for "
            + (requireCis ? "cis " : "")
            + origResName;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

