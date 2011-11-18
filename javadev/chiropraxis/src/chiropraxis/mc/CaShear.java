// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.Strings;
import driftwood.util.SoftLog;
import chiropraxis.sc.*;
//}}}
/**
* <code>CaShear</code> is like IWD's CaRotation, but it 
* "shears" atoms between two C-alphas instead of rotating them.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Fri Jul 30 2010
*/
public class CaShear extends CaRotation
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
    public CaShear()
    {
        super();
    }
//}}}

//{{{ makeConformation
//##################################################################################################
    /**
    * Rotates a collection of residues (as from
    * {@link #makeMobileGroup(Model, Residue, Residue)})
    * by theta degrees around the axis through the
    * C-alphas of the terminal residues.
    * @throws AtomException if either one of the terminal residues
    *   is missing a C-alpha, or if the state is missing a state definition
    *   for any of the mobile atoms.
    */
    public static ModelState makeConformation(Collection residues, ModelState state, double theta1, boolean idealizeSC) throws AtomException
    {
        if(residues.size() != 4)
            throw new AtomException("Need 4 residues for makeConformation(..)!");
        
        // Find first, second, third, and fourth residue in the collection
        Residue res1 = null, res2 = null, res3 = null, res4 = null;
        Iterator iter = residues.iterator();
        while(iter.hasNext())
        {
            Residue curr = (Residue)iter.next();
            if     (res1 == null)  res1 = curr;
            else if(res2 == null)  res2 = curr;
            else if(res3 == null)  res3 = curr;
            else if(res4 == null)  res4 = curr;
        }
        if(res1 == null || res2 == null || res3 == null || res4 == null)
            throw new AtomException("First, second, third, or fourth residue is missing!");
        
        // Find atoms to rotate around
        Atom ca1, ca2, ca3, ca4;
        ca1 = res1.getAtom(" CA ");
        ca2 = res2.getAtom(" CA ");
        ca3 = res3.getAtom(" CA ");
        ca4 = res4.getAtom(" CA ");
        if(ca1 == null || ca2 == null || ca3 == null || ca4 == null)
            throw new AtomException("C-alpha is missing from "+res1+", "+res2+", "+res3+", or "+res4);
        Atom c2, o2;
        c2 = res2.getAtom(" C  ");
        o2 = res2.getAtom(" O  ");
        if(c2 == null || o2 == null)
            throw new AtomException("Carbonyl is missing from "+res2);
        
        // Split up atoms into three groups, one per peptide
        Atom[] atoms = getMobileAtoms(residues);
        ArrayList p1 = new ArrayList();
        ArrayList p2 = new ArrayList();
        ArrayList p3 = new ArrayList();
        for(int i = 0; i < atoms.length; i++)
        {
            Atom a = atoms[i];
            Residue r = a.getResidue();
            if(r.equals(res1)) p1.add(a);
            else if(r.equals(res2))
            {
                if(a.getName().equals(" C  ") || a.getName().equals(" O  ")) p2.add(a);
                else p1.add(a);
            }
            else if(r.equals(res3))
            {
                if(a.getName().equals(" N  ") || a.getName().equals(" H  ")) p2.add(a);
                else p3.add(a);
            }
            else p3.add(a);
        }
        Atom[] pep1 = (Atom[]) p1.toArray(new Atom[p1.size()]);
        Atom[] pep2 = (Atom[]) p2.toArray(new Atom[p2.size()]);
        Atom[] pep3 = (Atom[]) p3.toArray(new Atom[p3.size()]);
        
        // Define first rotation axis, before anything has moved:
        // first CA  ->  normal to plane of first, second, and third CAs
        Triple normal123 = new Triple().likeNormal(state.get(ca1), state.get(ca2), state.get(ca3));
        normal123.add(state.get(ca1));
        
        // Do first rotation
        Transform   rot1  = new Transform().likeRotation(state.get(ca1), normal123, theta1);
        ModelState  rv1   = transformAtoms(rot1, pep1, state);
        
        // Define second rotation axis, now that (first and) second CAs have moved:
        // fourth CA  ->  normal to plane of second, third, and fourth CAs
        Triple normal234 = new Triple().likeNormal(rv1.get(ca2), rv1.get(ca3), rv1.get(ca4));
        normal234.add(rv1.get(ca4));
        
        // Find second rotation that best preserves original distance between second and third CAs
        // Try up to 1.5x the first rotation in 0.1 degree increments
        double distOrig     = Triple.distance(state.get(ca2), state.get(ca3));
        double distBest     = Double.POSITIVE_INFINITY;
        double distDiffBest = Double.POSITIVE_INFINITY;
        double theta2 = 0;
        double max = Math.min(Math.abs(theta1), Math.abs(Math.abs(theta1)-360));
        //for(double t = -2*max; t < 2*max; t += 0.1)
        for(double t = -1.5*max; t < 1.5*max; t += 0.1)
        {
            // Try a second rotation
            Transform   rot2  = new Transform().likeRotation(rv1.get(ca4), normal234, t);
            ModelState  rv2   = transformAtoms(rot2, pep3, rv1);
            
            // See how close second and third CAs are
            double distCurr = Triple.distance(rv2.get(ca2), rv2.get(ca3));
            double distDiff = Math.abs(distCurr - distOrig);
            if(distDiff < distDiffBest)
            {
                distBest     = distCurr;
                distDiffBest = distDiff;
                theta2 = t;
            }
        }
        
        // Do best second rotation
        Transform   rot2  = new Transform().likeRotation(rv1.get(ca4), normal234, theta2);
        ModelState  rv2   = transformAtoms(rot2, pep3, rv1);
        
        // "Plug in" middle peptide
        AtomState[] ca23orig = new AtomState[] {state.get(ca2), state.get(ca3)};
        AtomState[] ca23movd = new AtomState[] {rv2.get(ca2),   rv2.get(ca3)};
        SuperPoser  poser  = new SuperPoser(ca23movd, ca23orig);
        Transform   sup    = poser.superpos();
        ModelState  rv3    = transformAtoms(sup, pep2, rv2);
        
        ModelState rv4 = rv3;
        /*{{{ Aborted manual middle peptide rotation
        // I thought about manually rotating the middle peptide 
        // to make sure its C=O is as near as possible to its original direction,
        // but in practice SuperPoser.superpos() seems to do just fine with 2 points.
        // Middle peptide rotations will probably be tried anyway, 
        // either manually in KiNG or systematically in some automated refit program.
        Triple origCO = state.get(o2).sub(state.get(c2));
        double angleBest = Double.POSITIVE_INFINITY;
        double theta3 = 0;
        for(double t = -15.0; t < 15.0; t += 0.1)
        {
            // Try a middle peptide rotation
            Transform   rot3  = new Transform().likeRotation(rv3.get(ca2), rv3.get(ca3), t);
            ModelState  rv4   = transformAtoms(rot3, pep2, rv3);
            
            // See how close middle C=O direction is to its original direction
            Triple movdCO = rv4.get(o2).sub(rv4.get(c2));
            double angleCurr = origCO.angle(movdCO);
            if(angleCurr < angleBest)
            {
                angleBest = angleCurr;
                theta3 = t;
            }
        }
        
        // Do best middle peptide rotation
        theta3 = Math.round(theta3 * 10) / 10; // so we stay at 0.0 for no shear
        Transform   rot3  = new Transform().likeRotation(rv3.get(ca2), rv3.get(ca3), theta3);
        ModelState  rv4   = transformAtoms(rot3, pep2, rv3);
        }}}*/
        
        // Fix the sidechains
        if(idealizeSC && sidechainIdealizer != null)
        {
            rv4 = sidechainIdealizer.idealizeSidechain(res1, rv4);
            rv4 = sidechainIdealizer.idealizeSidechain(res2, rv4);
            rv4 = sidechainIdealizer.idealizeSidechain(res3, rv4);
            rv4 = sidechainIdealizer.idealizeSidechain(res4, rv4);
        }
        
        return rv4.createCollapsed(state);
    }
//}}}

//{{{ getMobileAtoms
//##################################################################################################
    static Atom[] getMobileAtoms(Collection residues)
    {
        ArrayList   atoms   = new ArrayList();
        Iterator    iter    = residues.iterator();
        Residue     r       = (Residue)iter.next();
        
        // Add the first residue, C and O only
        for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
        {
            Atom    a   = (Atom)ai.next();
            String  an  = a.getName();
            if(an.equals(" C  ") || an.equals(" O  "))
                atoms.add(a);
        }
        
        // Add all atoms for the rest of residues except the last
        r = (Residue)iter.next();
        while(iter.hasNext())
        {
            for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom    a   = (Atom)ai.next();
                atoms.add(a);
            }
            r = (Residue)iter.next();
        }
        
        // Add last residue, N and H only
        for(Iterator ai = r.getAtoms().iterator(); ai.hasNext(); )
        {
            Atom    a   = (Atom)ai.next();
            String  an  = a.getName();
            if(an.equals(" N  ") || an.equals(" H  "))
                atoms.add(a);
        }
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

