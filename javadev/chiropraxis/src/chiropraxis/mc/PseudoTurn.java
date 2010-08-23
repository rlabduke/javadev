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
* <code>PseudoTurn</code> is a pseudo-turn (i.e. Asx turn), 
* defined as three residues in-plane, with the first Asn/Asp 
* residue's sidechain OD1/2 H-bonded to the fourth residue's NH.
*
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Aug 10 2010
*/
public class PseudoTurn extends Turn
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    //public Residue r0;         <-- sc of r1 fills this role
    //public double phi0, psi0;  <-- n/a for pseudo-turns
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * @param r1 the Asn/Asp residue with its sidechain OD1 oxygen serving the 
    * role of the mainchain CO oxygen in a normal tight turn
    @ @param r3 the fourth of four residue in the tight turn, with its mainchain
    * NH hydrogen in an H-bond to the first residue
    */
    public PseudoTurn(Model model, ModelState state, Residue r1, Residue r2, Residue r3) throws AtomException, ResidueException
    {
        this.model = model;
        this.state = state;
        this.r1    = r1;
        this.r2    = r2;
        this.r3    = r3;
        calcHbondEnergies();
        calcDihedral();
        calcPhiPsis();
        calcHighB();
        decideType();
    }
//}}}

//{{{ calcHbondEnergies
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
    protected void calcHbondEnergies() throws AtomException, ResidueException
    {
        Residue r4 = r3.getNext(model);
        if(r4 == null) throw new ResidueException(
            "Need residue after "+r3+"to complete turn!");
        
        this.hbEnergy1to3 = calcHbondEnergy(r1, r3);
        this.hbEnergy1to4 = calcHbondEnergy(r1, r4);
    }
//}}}

//{{{ calcHbondEnergy
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
    protected double calcHbondEnergy(Residue r, Residue s) throws AtomException
    {
        Atom c = r.getAtom(" CG ");
        Atom o = r.getAtom(" OD1");
        Atom n = s.getAtom(" N  ");
        Atom h = s.getAtom(" H  ");
        if(r.getName().equals("ASP"))
        {
            // We guessed OD1 before, but OD2 could be closer for Asp
            // (Not a problem for Asn)
            Atom oAlt = r.getAtom(" OD2");
            double dist    = Triple.distance(state.get(o)   , state.get(h));
            double distAlt = Triple.distance(state.get(oAlt), state.get(h));
            if(distAlt < dist) o = oAlt;
        }
        AtomState cs = state.get(c);
        AtomState os = state.get(o);
        AtomState ns = state.get(n);
        AtomState hs = state.get(h);
        double rON = os.distance(ns);
        double rCH = cs.distance(hs);
        double rOH = os.distance(hs);
        double rCN = cs.distance(ns);
        return 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
    }
//}}}

//{{{ calcDihedral
//##############################################################################
    /**
    * Calculates a dihedral from the four "joint" atoms comprising the turn.
    * In the case of pseudo-turns, they are the first Asx residue's ND2 atom
    * and three C-alphas, from the Asx and its two subsequent neighbors.
    */
    protected void calcDihedral() throws AtomException
    {
        Atom c1  = r1.getAtom(" CG ");
        Atom ca1 = r1.getAtom(" CA ");
        Atom ca2 = r2.getAtom(" CA ");
        Atom ca3 = r3.getAtom(" CA ");
        if(c1 == null || ca1 == null || ca2 == null || ca3 == null)
            throw new AtomException("Can't find Asx CG and/or 3 C-alphas!");
        AtomState c1s  = state.get(c1);
        AtomState ca1s = state.get(ca1);
        AtomState ca2s = state.get(ca2);
        AtomState ca3s = state.get(ca3);
        dihedral = Triple.dihedral(c1s, ca1s, ca2s, ca3s);
    }
//}}}

//{{{ calcPhiPsis
//##############################################################################
    /**
    * Calculates phi,psi dihedrals for the residues comprising this turn.
    */
    protected void calcPhiPsis() throws AtomException, ResidueException
    {
        phi1 = AminoAcid.getPhi(model, r1, state);
        psi1 = AminoAcid.getPsi(model, r1, state);
        phi2 = AminoAcid.getPhi(model, r2, state);
        psi2 = AminoAcid.getPsi(model, r2, state);
        phi3 = AminoAcid.getPhi(model, r3, state);
        psi3 = AminoAcid.getPsi(model, r3, state);
    }
//}}}

//{{{ calcHighB
//##############################################################################
    /**
    * Simply stores the highest atomic B-factor for the three residues 
    * comprising the turn.
    */
    protected void calcHighB() throws AtomException
    {
        highB = Double.NEGATIVE_INFINITY;
        Residue[] residues = new Residue[] {r1, r2, r3};
        for(Residue r : residues)
        {
            for(Iterator iter = r.getAtoms().iterator(); iter.hasNext(); )
            {
                Atom a = (Atom) iter.next();
                AtomState as = state.get(a);
                if(as.getTempFactor() > highB)
                    highB = as.getTempFactor();
            }
        }
    }
//}}}

//{{{ decideType
//##############################################################################
    /**
    * Prepares atoms to geometrically check turn "type".
    */
    protected void decideType() throws AtomException
    {
        // Atom names here are kept analogous to tight turns -- 
        // they don't reflect actual numbering for pseudo-turns!
        
        // C-alphas for plane
        Atom ca0 = r1.getAtom(r1.getName().equals("ASN") ? " ND2" : " OD2");
        Atom ca1 = r1.getAtom(" CA ");
        Atom ca2 = r2.getAtom(" CA ");
        Atom ca3 = r3.getAtom(" CA ");
        if(ca0 == null || ca1 == null || ca2 == null || ca3 == null)
            throw new AtomException("Can't find all atoms needed to determine type!");
        AtomState ca0s = state.get(ca0);
        AtomState ca1s = state.get(ca1);
        AtomState ca2s = state.get(ca2);
        AtomState ca3s = state.get(ca3);
        Collection points = new ArrayList();
        points.add(ca0s);
        points.add(ca1s);
        points.add(ca2s);
        points.add(ca3s);
        Triple check = new Triple().likeNormal(ca0s, ca1s, ca2s);
        
        // Carbonyls
        Atom c0  = r1.getAtom(" CG ");
        Atom o0  = r1.getAtom(" OD1");
        Atom c1  = r1.getAtom(" C  ");
        Atom o1  = r1.getAtom(" O  ");
        if(c0 == null || o0  == null || c1  == null || o1 == null)
            throw new AtomException("Can't find all atoms needed to determine type!");
        AtomState c0s  = state.get(c0);
        AtomState o0s  = state.get(o0);
        AtomState c1s  = state.get(c1);
        AtomState o1s  = state.get(o1);
        // Swap OD1/OD2 for Asp if we guessed wrong earlier
        if(r1.getName().equals("ASP"))
        {
            Atom o0Alt = r1.getAtom(" OD2");
            if(o0Alt == null) return;
            AtomState o0sAlt = state.get(o0Alt);
            double dist    = Triple.distance(o0s   , ca3s);
            double distAlt = Triple.distance(o0sAlt, ca3s);
            if(distAlt < dist) o0s = o0sAlt;
        }
        Triple co0 = new Triple().likeVector(c0s, o0s);
        Triple co1 = new Triple().likeVector(c1s, o1s);
        
        pickType(co0, co1, points, check); // parent method in Turn
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "type "+type+" pseudo-turn from "+r1+" to "+r3;
    }
//}}}
}//class

