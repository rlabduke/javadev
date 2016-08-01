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
    static final String ANY_RESNAME_PRINT = "";
    static final String ANY_RESNAME_REGEX = "/[_A-Za-z0-9]{3}/";
//}}}

//{{{ Variable definitions
//##############################################################################
    String      origResName;   // just used for a pretty toString()
    int         resOffset;
    Matcher     regexName;
    boolean     requireCis;    // cis peptide instead of trans
    boolean     requireDeoxy;  // DNA
    boolean     requireOxy;    // RNA
    boolean     require2p;     // ribose pucker
    boolean     requireDisulf; // in a disulfide bond
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Full constructor will all 'require' variables specified.
    * @param requireCis if true, requires that the peptide bond preceding the residue
    *        be cis rather than trans.  If false, doesn't care whether cis or trans.
    * @param requireDeoxy if true, requires that the C2' is detectable but the O2'
    *        is not (i.e. DNA).  If false, doesn't care whether RNA or DNA.
    * @param requireOxy if true, requires that the C2'-O2' bond is detectable (i.e. RNA).
    *        If false, doesn't care whether RNA or DNA.
    * @param require2p if true, requires that the ribose pucker is the rarer 2' endo
    *        rather than the more common 3' endo.  If false, doesn't care whether 2' or 3'.
    * @param requireDisulf if true, requires that the residue is in a disulfide bond.
    *        If false, doesn't care whether in a disulfide bond or not.
    */
    public ResSpec(int resOffset, boolean requireCis, boolean requireDeoxy, boolean requireOxy, boolean require2p, boolean requireDisulf, String resName)
    {
        super();
        this.resOffset     = resOffset;
        this.requireCis    = requireCis;
        this.requireDeoxy  = requireDeoxy;
        this.requireOxy    = requireOxy;
        this.require2p     = require2p;
        this.requireDisulf = requireDisulf;
        setResNames(resName);
    }

    /**
    * In-between constructor.  Reverts to defaults for everything (0 residue offset, 
    * no geometry requirements) except for residue name.
    */
    public ResSpec(String resName)
    {
        super();
        this.resOffset     = 0;
        this.requireCis    = false;
        this.requireDeoxy  = false;
        this.requireOxy    = false;
        this.require2p     = false;
        this.requireDisulf = false;
        setResNames(resName);
    }

    /**
    * Absolute bare-minimum constructor.  Reverts to defaults for everything: 
    * 0 residue offset, no geometry requirements, and any 3-letter residue name.
    */
    public ResSpec()
    {
        super();
        this.resOffset     = 0;
        this.requireCis    = false;
        this.requireDeoxy  = false;
        this.requireOxy    = false;
        this.require2p     = false;
        this.requireDisulf = false;
        setResNames(ANY_RESNAME_REGEX);
    }
//}}}

//{{{ setResNames
//##############################################################################
    public void setResNames(String resName)
    {
        // Purely aesthetic
        if(resName.equals(ANY_RESNAME_REGEX))
            this.origResName = ANY_RESNAME_PRINT;
        else
            this.origResName = resName;
        
        // Name for use by code for matching
        resName = resName.replace('_', ' ');
        if(resName.startsWith("/") && resName.endsWith("/"))
            this.regexName = Pattern.compile(resName.substring(1,resName.length()-1)).matcher("");
        else
            this.regexName = Pattern.compile(Pattern.quote(resName)).matcher("");
    }
//}}}

//{{{ require___
//##############################################################################
    /** @return this, for chaining */
    public ResSpec requireCis()
    {
        this.requireCis = true;
        return this;
    }

    /** @return this, for chaining */
    public ResSpec requireDeoxy()
    {
        this.requireDeoxy = true;
        return this;
    }

    /** @return this, for chaining */
    public ResSpec requireOxy()
    {
        this.requireOxy = true;
        return this;
    }

    /** @return this, for chaining */
    public ResSpec require2p()
    {
        this.require2p = true;
        return this;
    }

    /** @return this, for chaining */
    public ResSpec requireDisulf()
    {
        this.requireDisulf = true;
        return this;
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
            catch(AtomException ex) { return false; }
        }
        
        /*{{{ old DNA/RNA code
        if(requireDeoxy) // DNA
        {
            // Some modified RNA bases have other atoms in place of the O2':
            String[] o2options = new String[] {" O2'", "SE2'", ____};
            try
            { AtomState c2 = state.get(curr.getAtom(" C2'")); }
            catch(AtomException ex)
            { return false; } // may just be disordered DNA, but can't be *sure* it's actually DNA
            
            for(String o2option : o2options)
            {
                try
                {
                    AtomState o2 = state.get(curr.getAtom(o2option));
                    return false; // only happens if O2' (or derivative) present, meaning *not* DNA
                }
                catch(AtomException ex) {} // O2' (or derivative) absent, meaning *likely* DNA - carry on!
            }
        }
        
        if(requireOxy) // RNA
        {
            boolean c2o2bonded = false;
            // Some modified RNA bases have other atoms in place of the O2':
            String[] o2options = new String[] {" O2'", "SE2'", ____};
            for(String o2option : o2options)
            {
                try
                {
                    AtomState c2 = state.get(curr.getAtom(" C2'"));
                    AtomState o2 = state.get(curr.getAtom(o2option));
                    double c2o2dist = Triple.distance(c2, o2);
                    if(!Double.isNaN(c2o2dist) && c2o2dist < 2.0)
                    {
                        c2o2bonded = true; // C2'-O2' bond *does* exist, so *is* RNA
                        break;
                    }
                }
                catch(AtomException ex) {}
            }
            if(!c2o2bonded) return false; // C2'-O2' bond does *not* exist, so *not* RNA
        }
        }}}*/
        
        if(requireOxy || requireDeoxy) // RNA or DNA
        {
            AtomSpec c2spec = new AtomSpec(0, "_C2*");
            AtomState c2 = c2spec.get(model, state, curr);
            if(c2 == null)    // may just be disordered, but can't be *sure* it's 
                return false; // actually DNA or RNA rather than something else 
            
            // Some modified RNA bases have other atoms in place of the O2':
            AtomSpec[] o2specs = new AtomSpec[] {
                new AtomSpec(0, " O2*"),
                new AtomSpec(0, "SE2*"), // e.g. 1YLS
                new AtomSpec(0, "_F__"), // e.g. 3DD2
            };
            
            boolean c2o2bonded = false;
            for(AtomSpec o2spec : o2specs)
            {
                AtomState o2 = o2spec.get(model, state, curr);
                if(o2 == null) // can't find this O2' or substitute, so
                    continue;  // can't confirm existence of C2-O2' bond
                double c2o2dist = Triple.distance(c2, o2);
                if(!Double.isNaN(c2o2dist) && c2o2dist < 2.0)
                {
                    c2o2bonded = true;
                    break;
                }
            }
            
            if(requireOxy   && !c2o2bonded) return false; // want RNA but this isn't
            if(requireDeoxy &&  c2o2bonded) return false; // want DNA but this isn't
        }
        
        if(require2p) // ribose pucker
        {
            System.err.println(
                "RNA 2'-pucker-specific bond lengths & angles not yet implemented!");
            return false;
        }
        
        if(requireDisulf)
        {
            // Attempt to find disulfide partner geometrically here?
            Disulfides disulfs = model.getDisulfides();
            if(disulfs == null) return false;
            Disulfide disulf = disulfs.get(curr);
            if(disulf == null) return false;
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
        return "for"
            + (requireCis ? " cis" : "")
            + (requireDeoxy ? " deoxy" : "")
            + (requireOxy ? " oxy" : "")
            + (require2p ? " 2'" : "")
            + (requireDisulf ? " disulf" : "")
            + (origResName.equals(this.ANY_RESNAME_PRINT) ? "" : " "+origResName);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

