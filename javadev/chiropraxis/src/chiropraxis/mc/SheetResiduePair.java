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
* <code>SheetResiduePair</code> is a pair of residues across from each other
* in beta sheet.
*
* Really, <code>BetaRes1</code> should extend this... but I wrote that before
* this, so I'm not inclined to go back and clean up the inheritance post facto.
*
* <p>Copyright (C) 2012 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Nov 6 2012
*/
public class SheetResiduePair //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Name of the structure this sheet is in */
    public String modelName;
    /** Model this sheet is in */
    public Model model;
    /** First and second residues (order is arbitrary) */
    public Residue res1, res2;
    
    /** Whether or not the strands are parallel to each other */
    boolean parallel;
    /** Narrow or wide for anti-parallel, null for parallel */
    String type;
    
    /** Number of fully beta residues (i.e. both peptides of which the residue
    * is a part must be beta according to SheetBuilder) on each end of res1 & 
    * res2 residues */
    public int res1NumBetaResN, res1NumBetaResC, res2NumBetaResN, res2NumBetaResC;
    
    /** Whether or not each strand is an edge strand */
    public boolean res1Edge, res2Edge;
    
    /** Distance from Ca(i,res1)-Ca(i,res2) */
    public double cacaDist;
    
    /** Angle from Cb(i,res1)-Ca(i,res1)-Ca(i,res2) */
    public double cbcacaAngle;
    
    /** Beta "twist" of 4 Calphas N-ward of 2 central residues (inclusive) */
    public double nwardTwist;
    /** Beta "twist" of 4 Calphas C-ward of 2 central residues (inclusive) */
    public double cwardTwist;
    
    /** Beta "twist" of 2 Calphas N-ward of residue 1 (inclusive)
    * and 2 Calphas C-ward of residue 2 (inclusive) */
    public double nwardCrossTwist;
    /** Beta "twist" of 2 Calphas C-ward of residue 1 (inclusive)
    * and 2 Calphas N-ward of residue 2 (inclusive) */
    public double cwardCrossTwist;
    
    /** "Fray" of strands: how much they're pulling apart, as viewed from Ca(res1)->Ca(res2) */
    public double fray;
    
    /** "Tilt": angle between Ca(res1)(i-1,i,i+1)-Ca(res2)(i-1,i,i+1):  /...\  or \.../ */
    public double tilt;
    
    /** Residue 1 strand simple angle from Ca(i-1)-Ca(i)-Ca(i+1) */
    public double res1CaAngle;
    /** Residue 2 strand simple angle from Ca(i-1)-Ca(i)-Ca(i+1) */
    public double res2CaAngle;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SheetResiduePair(Residue res1, Residue res2, boolean parallel, String type, Collection peptides, Model model, String modelName)
    {
        super();
        
        this.modelName = modelName;
        this.model     = model;
        this.res1      = res1;
        this.res2      = res2;
        
        this.parallel = parallel;
        this.type     = type;
        
        this.res1NumBetaResN = numNward(res1, model, peptides);
        this.res1NumBetaResC = numCward(res1, model, peptides);
        this.res2NumBetaResN = numNward(res2, model, peptides);
        this.res2NumBetaResC = numCward(res2, model, peptides);
        
        this.res1Edge = isEdgeStrand(res1, peptides);
        this.res2Edge = isEdgeStrand(res2, peptides);
        
        this.calcGeometry();
    }
//}}}

//{{{ num(N/C)ward, isBeta
//##############################################################################
    /**
    * Determines number of residue N-ward in sequence of <code>res</code>
    * that are in the same beta strand.
    */
    int numNward(Residue res, Model model, Collection peptides)
    {
        int numN = 0;
        boolean endOfStrand = false;
        Residue currRes = res;
        while(!endOfStrand)
        {
            if(currRes.getPrev(model) != null)
            {
                currRes = currRes.getPrev(model);
                if(!isBeta(currRes, model, peptides))  endOfStrand = true;
                else                                   numN += 1;
            }
            else endOfStrand = true;
        }
        return numN;
    }

    /**
    * Determines number of residue C-ward in sequence of <code>res</code>
    * that are in the same beta strand.
    */
    int numCward(Residue res, Model model, Collection peptides)
    {
        int numC = 0;
        boolean endOfStrand = false;
        Residue currRes = res;
        while(!endOfStrand)
        {
            if(currRes.getNext(model) != null)
            {
                currRes = currRes.getNext(model);
                if(!isBeta(currRes, model, peptides))  endOfStrand = true;
                else                                   numC += 1;
            }
            else endOfStrand = true;
        }
        return numC;
    }

    /**
    * Decides <code>res</code> is beta if both the peptides containing it are beta.
    */
    boolean isBeta(Residue res, Model model, Collection peptides)
    {
        Peptide nPep = null; // peptide N-ward to this residue; also contains this residue's N-H
        Peptide cPep = null; // peptide C-ward to this residue; also contains this residue's C=O
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(res.equals(pep.nRes))  nPep = pep;
            if(res.equals(pep.cRes))  cPep = pep;
        }
        if(nPep == null || cPep == null) return false;
        if(nPep.isBeta && cPep.isBeta) return true;
        return false;
    }
//}}}

//{{{ isEdgeStrand
//##############################################################################
    /**
    * Determines whether or not the given beta residue is on an edge strand.
    * An edge strand is defined here as having one side or the other fully exposed,
    * i.e. if both the NH and the CO lack H-bonds.
    * By this definition, a strand can be classified non-edge if it H-bonds
    * to something other than a strand, like a water or other ligand.
    */
    boolean isEdgeStrand(Residue res, Collection peptides)
    {
        Peptide nPep = null; // peptide N-ward to this residue; also contains this residue's N-H
        Peptide cPep = null; // peptide C-ward to this residue; also contains this residue's C=O
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(res.equals(pep.nRes))  nPep = pep;
            if(res.equals(pep.cRes))  cPep = pep;
        }
        
        /*if(nPep.hbondO != null || cPep.hbondN != null) return false;*/
        boolean narrowSideHbonded = false, wideSideHbonded = false;
        if(nPep.hbondN != null || cPep.hbondO != null)  narrowSideHbonded = true;
        if(nPep.hbondO != null || cPep.hbondN != null)  wideSideHbonded = true;
        if(!narrowSideHbonded || !wideSideHbonded)
            return true; // this strand is exposed on at least one its two sides
        return false; // this strand is at least partially protected on both sides
    }
//}}}

//{{{ calcGeometry
//##############################################################################
    public void calcGeometry()
    {
        // Prepare residues and atoms -- no states (coordinates) yet
        Atom res1Ca = res1.getAtom(" CA ");
        Atom res1Cb = res1.getAtom(" CB ");
        Atom res2Ca = res2.getAtom(" CA ");
        Residue res1Next = res1.getNext(model);  
        Residue res1Prev = res1.getPrev(model);  
        Residue res2Next = res2.getNext(model);  
        Residue res2Prev = res2.getPrev(model);  
        Atom res1NextCa = null; if(res1Next != null) res1NextCa = res1Next.getAtom(" CA ");
        Atom res1PrevCa = null; if(res1Prev != null) res1PrevCa = res1Prev.getAtom(" CA ");
        Atom res2NextCa = null; if(res2Next != null) res2NextCa = res2Next.getAtom(" CA ");
        Atom res2PrevCa = null; if(res2Prev != null) res2PrevCa = res2Prev.getAtom(" CA ");
        
        ModelState state = model.getState();
        
        // Distance: Ca(i,res1)-Ca(i,res2)
        try { cacaDist = Triple.distance(state.get(res1Ca),
                                         state.get(res2Ca)); }
        catch(AtomException ex) {}
        
        // Ed's angle: Cb(res1)-Ca(res1)-Ca(res2)
        try { cbcacaAngle = Triple.angle(state.get(res1Cb),
                                         state.get(res1Ca),
                                         state.get(res2Ca)); }
        catch(AtomException ex) {}
        
        // Angle: Ca(i+1,res1)-Ca(i,res1)-Ca(i+1,res1)
        try { res1CaAngle = Triple.angle(state.get(res1NextCa),
                                         state.get(res1Ca),
                                         state.get(res1PrevCa)); }
        catch(AtomException ex) {}
        
        // Angle: Ca(i+1,res2)-Ca(i,res2)-Ca(i+1,res2)
        try { res2CaAngle = Triple.angle(state.get(res2NextCa),
                                         state.get(res2Ca),
                                         state.get(res2PrevCa)); }
        catch(AtomException ex) {}
        
        // Initially assume anti-parallel for following dihedrals ...
        
        // Beta "twist" of 4 Calphas N-ward of 2 central residues (inclusive)
        // Dihedral from Ca(i-1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i+1,res2)
        try { nwardTwist = Triple.dihedral(state.get(res1PrevCa),
                                           state.get(res1Ca),
                                           state.get(res2Ca),
                                           state.get(res2NextCa)); }
        catch(AtomException ex) {}
        // Beta "twist" of 4 Calphas C-ward of 2 central residues (inclusive)
        // Dihedral from Ca(i+1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i-1,res2)
        try { cwardTwist = Triple.dihedral(state.get(res1NextCa), 
                                           state.get(res1Ca), 
                                           state.get(res2Ca),
                                           state.get(res2PrevCa)); }
        catch(AtomException ex) {}
        // Beta "twist" of 2 Calphas N-ward of residue 1 (inclusive)
        // and 2 Calphas C-ward of residue 2 (inclusive)
        // Dihedral from Ca(i-1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i-1,res2)
        try { nwardCrossTwist = Triple.dihedral(state.get(res1PrevCa),
                                                state.get(res1Ca),
                                                state.get(res2Ca),
                                                state.get(res2PrevCa)); }
        catch(AtomException ex) {}
        // Beta "twist" of 2 Calphas C-ward of residue 1 (inclusive)
        // and 2 Calphas N-ward of residue 2 (inclusive)
        // Dihedral from Ca(i+1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i+1,res2)
        try { cwardCrossTwist = Triple.dihedral(state.get(res1NextCa),
                                                state.get(res1Ca),
                                                state.get(res2Ca),
                                                state.get(res2NextCa)); }
        catch(AtomException ex) {}
        
        // ... but swap variable values if residue 2 strand reversed
        if(parallel)
        {
            double justasec;
            justasec = nwardTwist; nwardTwist = nwardCrossTwist; nwardCrossTwist = justasec;
            justasec = cwardTwist; cwardTwist = cwardCrossTwist; cwardCrossTwist = justasec;
        }
        
        // "Fray" of strands: how much they're pulling apart, as viewed from Ca(res1)->Ca(res2).
        // If the first term is high, the strand "pull apart" at one of the ends
        // However, some of that is because of different pleating (Ca-Ca-Ca angle)
        // in the two strands, so the second, correction term subtracts that out.
        // In other words, it's how much the strands would be pulling apart relative 
        // to each other if their CaCaCa angles were dead equal.
        if(!Double.isNaN(cwardTwist) && !Double.isNaN(nwardTwist) 
        && !Double.isNaN(res1CaAngle) && !Double.isNaN(res2CaAngle))
        {
            fray = Math.abs(cwardTwist - nwardTwist) - Math.abs(res1CaAngle - res2CaAngle);
        }
        
        // "Tilt": angle between Ca(res1)(i-1,i,i+1) & Ca(res2)(i-1,i,i+1) planes,
        // i.e.   /...\   or   \.../   
        try
        {
            Triple normRes1 = new Triple().likeNormal(state.get(res1PrevCa),
                                                      state.get(res1Ca),
                                                      state.get(res1NextCa));
            Triple normRes2 = new Triple().likeNormal(state.get(res2PrevCa),
                                                      state.get(res2Ca),
                                                      state.get(res2NextCa));
            tilt = normRes1.angle(normRes2);
        }
        catch(AtomException ex) {}
    }
//}}}
        
//{{{ calcGeometry [OLD]
//##############################################################################
//    public void calcGeometry()
//    {
//        ModelState state = model.getState();
//        
//        // Distance from Ca(i,res1)-Ca(i,res2)
//        Triple caRes1 = state.get(res1.getAtom(" CA "));
//        Triple caRes2 = state.get(res2.getAtom(" CA "));
//        cacaDist = Triple.distance(caRes1, caRes2);
//        
//        // Ed's angle between Cb(res1)-Ca(res1)-Ca(res2)
//        if(!res1.getName().equals("GLY"))
//        {
//            Triple cbRes1 = state.get(res1.getAtom(" CB "));
//            cbcacaAngle = Triple.angle(cbRes1, caRes1, caRes2);
//        }
//        
//        // Angle between Ca(i+1,res1)-Ca(i,res1)-Ca(i+1,res1)
//        Triple caRes1Plus1  = state.get(res1.getNext(model).getAtom(" CA "));
//        Triple caRes1Minus1 = state.get(res1.getPrev(model).getAtom(" CA "));
//        res1CaAngle = Triple.angle(caRes1Plus1, caRes1, caRes1Minus1);
//        
//        // Angle between Ca(i+1,res2)-Ca(i,res2)-Ca(i+1,res2)
//        Triple caRes2Plus1  = state.get(res2.getNext(model).getAtom(" CA "));
//        Triple caRes2Minus1 = state.get(res2.getPrev(model).getAtom(" CA "));
//        res2CaAngle = Triple.angle(caRes2Plus1, caRes2, caRes2Minus1);
//        
//        // Initially assume anti-parallel for following dihedrals ...
//        
//        // Beta "twist" of 4 Calphas N-ward of 2 central residues (inclusive)
//        // Dihedral from Ca(i-1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i+1,res2)
//        nwardTwist = Triple.dihedral(caRes1Minus1, caRes1, caRes2, caRes2Plus1);
//        // Beta "twist" of 4 Calphas C-ward of 2 central residues (inclusive)
//        // Dihedral from Ca(i+1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i-1,res2)
//        cwardTwist = Triple.dihedral(caRes1Plus1, caRes1, caRes2, caRes2Minus1);
//        // Beta "twist" of 2 Calphas N-ward of residue 1 (inclusive)
//        // and 2 Calphas C-ward of residue 2 (inclusive)
//        // Dihedral from Ca(i-1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i-1,res2)
//        nwardCrossTwist = Triple.dihedral(caRes1Minus1, caRes1, caRes2, caRes2Minus1);
//        // Beta "twist" of 2 Calphas C-ward of residue 1 (inclusive)
//        // and 2 Calphas N-ward of residue 2 (inclusive)
//        // Dihedral from Ca(i+1,res1)-Ca(i,res1)-Ca(i,res2)-Ca(i+1,res2)
//        cwardCrossTwist = Triple.dihedral(caRes1Plus1, caRes1, caRes2, caRes2Plus1);
//                                  
//        // ... but swap variable values if residue 2 strand reversed
//        if(parallel)
//        {
//            double justasec;
//            justasec = nwardTwist; nwardTwist = nwardCrossTwist; nwardCrossTwist = justasec;
//            justasec = cwardTwist; cwardTwist = cwardCrossTwist; cwardCrossTwist = justasec;
//        }
//        
//        // "Fray" of strands: how much they're pulling apart, as viewed from Ca(res1)->Ca(res2).
//        // If the first term is high, the strand "pull apart" at one of the ends
//        // However, some of that is because of different pleating (Ca-Ca-Ca angle)
//        // in the two strands, so the second, correction term subtracts that out.
//        // In other words, it's how much the strands would be pulling apart relative 
//        // to each other if their CaCaCa angles were dead equal.
//        fray = Math.abs(cwardTwist - nwardTwist) - Math.abs(res1CaAngle - res2CaAngle);
//        
//        // "Tilt": angle between Ca(res1)(i-1,i,i+1) & Ca(res2)(i-1,i,i+1) planes,
//        // i.e.   /...\   or   \.../   
//        Triple normRes1 = new Triple().likeNormal(caRes1Minus1, caRes1, caRes1Plus1);
//        Triple normRes2 = new Triple().likeNormal(caRes2Minus1, caRes2, caRes2Plus1);
//        tilt = normRes1.angle(normRes2);
//    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        String s = res1.toString();
        s += (parallel ? " parallel" : " anti-parallel");
        s += " ("+type+")"+" to "+res2+" in "+modelName;
        return s;
    }
//}}}
}//class

