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
//}}}
/**
* <code>Turn</code> is either a tight turn or a pseudo-turn.
*
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Aug 10 2010
*/
public abstract class Turn //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    public Model model;
    public ModelState state;
    public Residue r1, r2, r3;
    public String type; // I, I', II, II'
    public double hbEnergy1to3, hbEnergy1to4; // from Kabsch & Sander equation
    public double dihedral; // Ca(x4) or Ca(x3)+N
    public double phi1, psi1, phi2, psi2, phi3, psi3;
    public double highB;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Turn()
    {
        super();
    }
//}}}

//{{{ pickType
//##############################################################################
    /**
    * Sets turn "type" based on CO directionality relative to the plane.
    * See AnaTax chapter on tight turns for details.
    */
    protected void pickType(Triple co0, Triple co1, Collection points, Triple check)
    {
        // We don't necessarily know which way the normal to the four C-alphas 
        // points. So, we use a "sanity check" normal which follows the right-hand 
        // rule and thus points "down" if the turn is viewed with the connection at 
        // the top and the sequence going left to right.
        LsqPlane plane = new LsqPlane(points);
        Triple normal = (Triple) plane.getNormal();
        if(check.angle(normal) > 90) normal.mult(-1);
        
        // From AnaTax, Tight Turns:
        //
        // The first oxygen points nearly 90deg down from the center of the plane 
        // in type I, nearly 90deg up in type I', slightly down in type II, and slightly
        // up in type II'.
        //
        // In types I and II' the second carbonyl oxygen points approximately 90deg down
        // from the plane, while in types II and I' it points approximately 90deg up.
        //
        // The position of the second carbonyl oxygen, then, distinguishes between 
        // types I and II (or I' and II'), while the position of the first carbonyl 
        // oxygen distinguishes types I vs II' (or II vs I'). For either distinction 
        // intermediate cases should be rare, because they lie in a strongly 
        // prohibited region of the phi,psi map. 
        
        if     (co0.angle(normal) < 90 && co1.angle(normal) < 90) type = "I";   // down , down
        else if(co0.angle(normal) > 90 && co1.angle(normal) > 90) type = "Ip";  // up   , up
        else if(co0.angle(normal) < 90 && co1.angle(normal) > 90) type = "II";  // ~down, up
        else if(co0.angle(normal) > 90 && co1.angle(normal) < 90) type = "IIp"; // ~up  , down
        else type = "unknown";
    }
//}}}

//{{{ calcHbondEnergies [abstract]
//##############################################################################
    /**
    * Calculates C=O .. H-N H-bond "energies" based on the criteria defined in
    * W. Kabsch and C. Sander (1983) Biopolymers, 22:2577.
    * The basic idea is that the H-bond is accepted if
    * E = 0.42*0.20*332*(1/rON + 1/rCH - 1/rOH - 1/rCN) is less than -0.5 kcal/mol.
    * Atom-atom distances are in Angstroms and E is in kcal/mol.
    * Ideal alignment allows distances up to 5.2 A (O to N);
    * ideal distance allows angles up to 63 degrees.
    */
    abstract void calcHbondEnergies() throws AtomException, ResidueException;
//}}}
    
//{{{ calcHbondEnergy [abstract]
//##############################################################################
    /**
    * Calculates C=O .. H-N H-bond "energy" based on the criteria defined in
    * W. Kabsch and C. Sander (1983) Biopolymers, 22:2577.
    * The basic idea is that the H-bond is accepted if
    * E = 0.42*0.20*332*(1/rON + 1/rCH - 1/rOH - 1/rCN) is less than -0.5 kcal/mol.
    * Atom-atom distances are in Angstroms and E is in kcal/mol.
    * Ideal alignment allows distances up to 5.2 A (O to N);
    * ideal distance allows angles up to 63 degrees.
    * @param r the Residue containing the C=O
    * @param s the Residue containing the N-H
    */
    abstract double calcHbondEnergy(Residue r, Residue s) throws AtomException;
//}}}

//{{{ calcDihedral [abstract]
//##############################################################################
    /**
    * Calculates a dihedral from the four "joint" atoms comprising this turn.
    */
    abstract void calcDihedral() throws AtomException;
//}}}

//{{{ calcPhiPsis [abstract]
//##############################################################################
    /**
    * Calculates phi,psi dihedrals for the residues comprising this turn.
    */
    abstract void calcPhiPsis() throws AtomException, ResidueException;
//}}}

//{{{ calcHighB [abstract]
//##############################################################################
    /**
    * Simply stores the highest atomic B-factor for the four residues 
    * (or three, in the case of pseudo-turns) comprising the turn.
    */
    abstract void calcHighB() throws AtomException;
//}}}

//{{{ decideType [abstract]
//##############################################################################
    /**
    * Prepares atoms to geometrically check turn "type".
    */
    abstract void decideType() throws AtomException;
//}}}

//{{{ toString [abstract]
//##############################################################################
    /**
    * Returns a simple string representation of this turn.
    */
    public abstract String toString();
//}}}
}//class

