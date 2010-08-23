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
    public static ModelState makeConformation(Collection residues, ModelState state, double theta12, boolean idealizeSC) throws AtomException
    {
        if(residues.size() != 4)
            throw new AtomException("Need 4 residues for makeConformation(..)!");
        
        // Find first, second, third, and fourth residue in the collection
        Residue first = null, second = null, third = null, fourth = null;
        Iterator iter = residues.iterator();
        while(iter.hasNext())
        {
            Residue curr = (Residue)iter.next();
            if(first == null)        first  = curr;
            else if(second == null)  second = curr;
            else if(third == null)   third  = curr;
            else if(fourth == null)  fourth = curr;
        }
        if(first == null || second == null || third == null || fourth == null)
            throw new AtomException("First, second, third, or fourth residue is missing!");
        
        // Find atoms to rotate around
        Atom firstCA, secondCA, thirdCA, fourthCA;
        firstCA   = first.getAtom(" CA ");
        secondCA  = second.getAtom(" CA ");
        thirdCA   = third.getAtom(" CA ");
        fourthCA  = fourth.getAtom(" CA ");
        if(firstCA == null || secondCA == null || thirdCA == null || fourthCA == null)
            throw new AtomException("C-alpha is missing from "+first+", "+second+", "+third+", or "+fourth);
        
        // Define rotation axes:
        // first CA  -> normal to plane of first,  second, and third  CAs
        // fourth CA -> normal to plane of second, third,  and fourth CAs
        Triple normal123 = new Triple().likeNormal(state.get(firstCA), state.get(secondCA), state.get(thirdCA));
        normal123.add(state.get(firstCA));
        Triple normal234 = new Triple().likeNormal(state.get(secondCA), state.get(thirdCA), state.get(fourthCA));
        normal234.add(state.get(fourthCA));
        
        // Split up atoms
        Atom[] atoms = getMobileAtoms(residues);
        ArrayList a12 = new ArrayList();
        ArrayList a34 = new ArrayList();
        for(int i = 0; i < atoms.length; i++)
        {
            Atom a = atoms[i];
            Residue r = a.getResidue();
            if(r.equals(first) || r.equals(second)) a12.add(a);
            else a34.add(a);
        }
        Atom[] atoms12 = (Atom[]) a12.toArray(new Atom[a12.size()]);
        Atom[] atoms34 = (Atom[]) a34.toArray(new Atom[a34.size()]);
        
        // Do first rotation
        Transform   rot12  = new Transform().likeRotation(state.get(firstCA), normal123, theta12);
        ModelState  rv12   = transformAtoms(rot12, atoms12, state);
        
        // Find second rotation that best preserves original distance between second and third CAs 
        double distOrig = Triple.distance(state.get(secondCA), state.get(thirdCA));
        double distDiffMin = Double.POSITIVE_INFINITY;
        double theta34 = 0;
        double max = Math.min(Math.abs(theta12), Math.abs(Math.abs(theta12)-360));
        for(double t = -2*max; t < 2*max; t += 0.1)
        {
            // Try a second rotation
            Transform   rot34  = new Transform().likeRotation(state.get(fourthCA), normal234, t);
            ModelState  rv34   = transformAtoms(rot34, atoms34, rv12);
            
            // See how close second and third CAs are
            double dist = Triple.distance(rv34.get(secondCA), rv34.get(thirdCA));
            double distDiff = Math.abs(dist - distOrig);
            if(distDiff < distDiffMin)
            {
                distDiffMin = distDiff;
                theta34 = t;
            }
        }
        //System.err.println("orig theta12: "+theta12);
        //System.err.println("best theta34: "+theta34+"\n");
        
        // Do best second rotation
        Transform   rot34  = new Transform().likeRotation(state.get(firstCA), normal234, theta34);
        ModelState  rv34   = transformAtoms(rot34, atoms34, rv12);
        
        // Fix the sidechains
        if(idealizeSC && sidechainIdealizer != null)
        {
            rv34 = sidechainIdealizer.idealizeSidechain(first, rv34);
            rv34 = sidechainIdealizer.idealizeSidechain(fourth, rv34);
        }
        
        return rv34.createCollapsed(state);
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

