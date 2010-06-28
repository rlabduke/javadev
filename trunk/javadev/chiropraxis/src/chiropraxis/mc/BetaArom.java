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
* <code>BetaArom</code> defines an aromatic residue in beta sheet and the 
* opposite residue on the adjacent strand (in the direction of the aromatic's CO).
* The aromatic is "hovering" over the opposite residue, which may be causing the 
* aromatic to backrub forward or backward if it's a Gly or something else, respectively.
* The strands can be either anti-parallel or (less common?) parallel.
* SheetBuilder makes an array of these if given the right flag (-betaarom).
* The class derives from DAK's Helix, which derives from IWD's Peptide.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class BetaArom //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Name of the structure this sheet is in */
    public String modelName;
    /** Model this sheet is in */
    public Model model;
    /** The aromatic residue which may be backrubbed */
    public Residue aromRes;
    /** The residue on the opposite strand which may be causing the aromatic 
    * to backrub */
    public Residue oppRes;
    
    /** Whether or not opposite strand is parallel to aromatic strand */
    boolean parallel;
    /** Chi1 & chi2 of arom(i) */
    public double chi1, chi2;
    
    /** Max B-factor for heavy atoms in aromatic residue (mc+sc) */
    public double aromMaxB;
    /** Max B-factor for heavy atoms in opposite residue (mc+sc) */
    public double oppMaxB;
    
    /** Number of fully beta residues (i.e. both peptides of which the residue
    * is a part must be beta according to SheetBuilder) on each end of arom & 
    * opp residues */
    public int aromNumBetaResN, aromNumBetaResC, oppNumBetaResN, oppNumBetaResC;
    
    /** If the aromatic strand is an edge strand */
    public boolean aromEdge;
    /** If the opposite strand (in the direction of the aromatic's CO) is an edge strand */
    public boolean oppEdge;
    
    /** Distance from Ca(i,arom)-Ca(i,opp) */
    public double cacaDist;
    
    /** Angle from Cb(i,arom)-Ca(i,arom)-Ca(i,opp) */
    public double cbcacaAngle;
    
    /** Beta "twist" of 4 Calphas N-ward of 2 central residues (inclusive) */
    public double nwardTwist;
    /** Beta "twist" of 4 Calphas C-ward of 2 central residues (inclusive) */
    public double cwardTwist;
    
    /** Beta "twist" of 2 Calphas N-ward of aromatic (inclusive)
    * and 2 Calphas C-ward of opposite (inclusive) */
    public double nwardCrossTwist;
    /** Beta "twist" of 2 Calphas C-ward of aromatic (inclusive)
    * and 2 Calphas N-ward of opposite (inclusive) */
    public double cwardCrossTwist;
    
    /** "Fray" of strands: how much they're pulling apart, as viewed from Ca(arom)->Ca(opp) */
    public double fray;
    
    /** "Tilt": angle between Ca(arom)(i-1,i,i+1)-Ca(opp)(i-1,i,i+1):  /...\  or \.../ */
    public double tilt;
    
    /** Aromatic strand simple angle from Ca(i-1)-Ca(i)-Ca(i+1) */
    public double aromCaAngle;
    /** Opposite strand simple angle from Ca(i-1)-Ca(i)-Ca(i+1) */
    public double oppCaAngle;
    
    /** Phi,psi of arom(i-1,i,i+1) */
    public double aromPrevPhi, aromPrevPsi, aromPhi, aromPsi, aromNextPhi, aromNextPsi;
    
    /** Tau (N-Ca-C) angle of aromatic, aromatic i-1, & aromatic i+1 */
    public double aromPrevTau, aromTau, aromNextTau;
    
    /** Angle from Ca(i,arom)-Cb(i,arom)-Cg(i,arom) */
    public double cgcbcaAngle;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BetaArom(Residue aromRes, Residue oppRes, boolean parallel, Collection peptides, Model model, String modelName)
    {
        super();
        
        this.modelName = modelName;
        this.model     = model;
        this.aromRes   = aromRes;
        this.oppRes    = oppRes;
        
        this.parallel = parallel;
        
        this.aromMaxB = maxB(aromRes, model);
        this.oppMaxB  = maxB(oppRes , model);
        
        this.aromNumBetaResN = numNward(aromRes, model, peptides);
        this.aromNumBetaResC = numCward(aromRes, model, peptides);
        this.oppNumBetaResN  = numNward(oppRes , model, peptides);
        this.oppNumBetaResC  = numCward(oppRes , model, peptides);
        
        this.aromEdge = isEdgeStrand(aromRes, peptides);
        this.oppEdge  = isEdgeStrand(oppRes , peptides);
    }
//}}}

//{{{ maxB
//##############################################################################
    double maxB(Residue res, Model model)
    {
        double maxB = Double.NEGATIVE_INFINITY;
        ModelState state = model.getState();
        for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
        {
            try
            {
                Atom a = (Atom) iter.next();
                AtomState as = state.get(a);
                double b = as.getTempFactor();
                if(b > maxB) maxB = b;
            }
            catch(AtomException ex)
            { System.err.println("Error getting max B-factor for "+res); }
        }
        return maxB;
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
        if(nPep.isBeta && cPep.isBeta)  return true;
        return false;
    }
//}}}

//{{{ isEdgeStrand
//##############################################################################
    /**
    * Determines whether or not the given beta residue is on an edge strand.
    * A backbone-backbone H-bond by either the exposed CO or NH 
    * is basically enough to say it's not a true edge locally, 
    * even though the H-bond recipient may itself not be a "strand".
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
        if(nPep.hbondO != null || cPep.hbondN != null) return false;
        return true; // no bb-bb H-bond at all
    }
//}}}

//{{{ calcGeometry
//##############################################################################
    public void calcGeometry() throws AtomException
    {
        ModelState state = model.getState();
        
        // Aromatic's Ca(i-1), Ca(i), Ca(i+1), and Cb(i)
        ArrayList<AtomState> aromCoords = new ArrayList<AtomState>();
        aromCoords.add(state.get(aromRes.getPrev(model).getAtom(" CA ")));
        aromCoords.add(state.get(aromRes.getAtom(" CA ")));
        aromCoords.add(state.get(aromRes.getNext(model).getAtom(" CA ")));
        aromCoords.add(state.get(aromRes.getAtom(" CB ")));
        
        // Opposite residue's Ca(i-1), Ca(i), Ca(i+1), and Cb(i)
        ArrayList<AtomState> oppCoords = new ArrayList<AtomState>();
        oppCoords.add(state.get(oppRes.getPrev(model).getAtom(" CA ")));
        oppCoords.add(state.get(oppRes.getAtom(" CA ")));
        oppCoords.add(state.get(oppRes.getNext(model).getAtom(" CA ")));
        if(oppRes.getName().equals("GLY")) oppCoords.add(null);
        else oppCoords.add(state.get(oppRes.getAtom(" CB ")));
        
        // Distance from Ca(i,arom)-Ca(i,opp)
        Triple caArom = state.get(aromRes.getAtom(" CA "));
        Triple caOpp  = state.get(oppRes.getAtom(" CA "));
        cacaDist = Triple.distance(caArom, caOpp);
        
        // Ed's angle between Cb(arom)-Ca(arom)-Ca(opp)
        Triple cbArom = state.get(aromRes.getAtom(" CB "));
        cbcacaAngle = Triple.angle(cbArom, caArom, caOpp);
        
        // Angle between Ca(i+1,arom)-Ca(i,arom)-Ca(i+1,arom)
        Triple caAromPlus1  = state.get(aromRes.getNext(model).getAtom(" CA "));
        Triple caAromMinus1 = state.get(aromRes.getPrev(model).getAtom(" CA "));
        aromCaAngle = Triple.angle(caAromPlus1, caArom, caAromMinus1);
        
        // Angle between Ca(i+1,opp)-Ca(i,opp)-Ca(i+1,opp)
        Triple caOppPlus1  = state.get(oppRes.getNext(model).getAtom(" CA "));
        Triple caOppMinus1 = state.get(oppRes.getPrev(model).getAtom(" CA "));
        oppCaAngle = Triple.angle(caOppPlus1, caOpp, caOppMinus1);
        
        // Initially assume anti-parallel for following dihedrals ...
        
        // Beta "twist" of 4 Calphas N-ward of 2 central residues (inclusive)
        // Dihedral from Ca(i-1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i+1,opp)
        nwardTwist = Triple.dihedral(caAromMinus1, caArom, caOpp, caOppPlus1);
        // Beta "twist" of 4 Calphas C-ward of 2 central residues (inclusive)
        // Dihedral from Ca(i+1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i-1,opp)
        cwardTwist = Triple.dihedral(caAromPlus1, caArom, caOpp, caOppMinus1);
        // Beta "twist" of 2 Calphas N-ward of aromatic (inclusive)
        // and 2 Calphas C-ward of opposite (inclusive)
        // Dihedral from Ca(i-1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i-1,opp)
        nwardCrossTwist = Triple.dihedral(caAromMinus1, caArom, caOpp, caOppMinus1);
        // Beta "twist" of 2 Calphas C-ward of aromatic (inclusive)
        // and 2 Calphas N-ward of opposite (inclusive)
        // Dihedral from Ca(i+1,arom)-Ca(i,arom)-Ca(i,opp)-Ca(i+1,opp)
        cwardCrossTwist = Triple.dihedral(caAromPlus1, caArom, caOpp, caOppPlus1);
                                  
        // ... but swap variable values if opposite strand reversed
        if(parallel)
        {
            double justasec;
            justasec = nwardTwist; nwardTwist = nwardCrossTwist; nwardCrossTwist = justasec;
            justasec = cwardTwist; cwardTwist = cwardCrossTwist; cwardCrossTwist = justasec;
        }
        
        // "Fray" of strands: how much they're pulling apart, as viewed from Ca(arom)->Ca(opp).
        // If the first term is high, the strand "pull apart" at one of the ends
        // However, some of that is because of different pleating (Ca-Ca-Ca angle)
        // in the two strands, so the second, correction term subtracts that out.
        // In other words, it's how much the strands would be pulling apart relative 
        // to each other if their CaCaCa angles were dead equal.
        fray = Math.abs(cwardTwist - nwardTwist) - Math.abs(aromCaAngle - oppCaAngle);
        
        // "Tilt": angle between Ca(arom)(i-1,i,i+1) & Ca(opp)(i-1,i,i+1) planes,
        // i.e.   /...\   or   \.../   
        Triple normArom = new Triple().likeNormal(caAromMinus1, caArom, caAromPlus1);
        Triple normOpp  = new Triple().likeNormal(caOppMinus1, caOpp, caOppPlus1);
        tilt = normArom.angle(normOpp);
        
        // Phi,psi of arom(i-1,i,i+1)
        Triple cAromPrevPrev  = state.get(aromRes.getPrev(model).getPrev(model).getAtom(" C  "));
        Triple nAromPrev      = state.get(aromRes.getPrev(model).getAtom(" N  "));
        Triple caAromPrev     = state.get(aromRes.getPrev(model).getAtom(" CA "));
        Triple cAromPrev      = state.get(aromRes.getPrev(model).getAtom(" C  "));
        Triple nArom          = state.get(aromRes.getAtom(" N  "));
        Triple cArom          = state.get(aromRes.getAtom(" C  "));
        Triple nAromNext      = state.get(aromRes.getNext(model).getAtom(" N  "));
        Triple caAromNext     = state.get(aromRes.getNext(model).getAtom(" CA "));
        Triple cAromNext      = state.get(aromRes.getNext(model).getAtom(" C  "));
        Triple nAromNextNext  = state.get(aromRes.getNext(model).getNext(model).getAtom(" N  "));
        aromPrevPhi = Triple.dihedral(cAromPrevPrev, nAromPrev , caAromPrev, cAromPrev    );
        aromPrevPsi = Triple.dihedral(nAromPrev    , caAromPrev, cAromPrev , nArom        );
        aromPhi     = Triple.dihedral(cAromPrev    , nArom     , caArom    , cArom        );
        aromPsi     = Triple.dihedral(nArom        , caArom    , cArom     , nAromNext    );
        aromNextPhi = Triple.dihedral(cArom        , nAromNext , caAromNext, cAromNext    );
        aromNextPsi = Triple.dihedral(nAromNext    , caAromNext, cAromNext , nAromNextNext);
        
        // Chi1,chi2 of aromatic residue
        Triple cdArom = state.get(aromRes.getAtom(" CD1"));
        Triple cgArom = state.get(aromRes.getAtom(" CG "));
        chi1 = Triple.dihedral(nArom , caArom, cbArom, cgArom);
        chi2 = Triple.dihedral(caArom, cbArom, cgArom, cdArom);
        
        // Tau angle of aromatic residue
        aromPrevTau = Triple.angle(nAromPrev, caAromPrev, cAromPrev);
        aromTau     = Triple.angle(nArom,     caArom,     cArom    );
        aromNextTau = Triple.angle(nAromNext, caAromNext, cAromNext);
        
        // Angle between Cg(i,arom)-Cb(i,arom)-Ca(i,arom)
        cgcbcaAngle = Triple.angle(cgArom, cbArom, caArom);
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "aromatic "+aromRes+" across from "+oppRes+" from "+modelName;
    }
//}}}
}//class

