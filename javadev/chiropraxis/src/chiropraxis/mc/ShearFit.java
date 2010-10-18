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
import chiropraxis.rotarama.*;
//}}}
/**
* <code>ShearFit</code> applies shear moves and Backrub-like hinges to minize
* Ca-RMSD between two four-residue segments, e.g. alternate conformations, 
* preferably at least somewhat helical.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Sep 21 2010
*/
public class ShearFit //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df  = new DecimalFormat("0.000");
    DecimalFormat df2 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##############################################################################
    Builder  builder;
    
    Model               model   = null;
    ModelState          state1  = null;
    ModelState          state2  = null;
    ArrayList<Residue>  res     = null;
    AtomState[]         atoms1  = null;
    AtomState[]         atoms2  = null;
    String          whichAtoms  = "ca+o";
    
    Ramachandran    rama       = null;
    TauByPhiPsi     tauscorer  = null;
    
    double   bestShear     = 0;
    double   bestBackrub1  = 0;
    double   bestBackrub2  = 0;
    double   bestPepRot1   = 0;
    double   bestPepRot2   = 0;
    double   bestPepRot3   = 0;
    
    boolean         idealizeSC  = false;
    boolean         verbose     = false;
    String          delim       = ",";
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ShearFit()
    {
        super();
        builder = new Builder();
        
        // force loading of data tables that will be used later
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
        try { tauscorer = TauByPhiPsi.getInstance(); }
        catch(IOException ex) {}
    }
//}}}

//{{{ initData
//##############################################################################
    void initData(Model m, Residue r1, Residue r2, Residue r3, Residue r4, String alt1, String alt2, String wa, boolean v, String d)
    {
        verbose = v;
        delim   = d;
        
        // Prep alt conf states
        model = m;
        state1 = (ModelState) model.getStates().get(alt1);
        state2 = (ModelState) model.getStates().get(alt2);
        
        res = new ArrayList<Residue>();
        res.add(r1);
        res.add(r2);
        res.add(r3);
        res.add(r4);
        
        // Make arrays of atoms for RMSD calculations (default: C-alphas + 2 central C=O oxygens)
        whichAtoms = wa;
        atoms1 = loadAtomStates(state1, new Residue[] {r1, r2, r3, r4}); // will be overwritten
        atoms2 = loadAtomStates(state2, new Residue[] {r1, r2, r3, r4});
        if(atoms1.length != atoms2.length)
            throw new IllegalArgumentException("Selections must have same number of atoms");
    }
//}}}

//{{{ loadAtomStates
//##############################################################################
    AtomState[] loadAtomStates(ModelState state, Residue[] residues)
    {
        if(state == null)
            throw new IllegalArgumentException("Must supply ModelState");
        if(!whichAtoms.equals("ca") && !whichAtoms.equals("ca+o"))
            throw new IllegalArgumentException("Unrecognized atom selection: "+whichAtoms);
        
        // Identify atoms
        ArrayList atoms = new ArrayList();
        for(int i = 0; i < residues.length; i++)
        {
            Residue r = residues[i];
            atoms.add(r.getAtom(" CA "));
            /*if(whichAtoms.equals("ca+o") && (i == 1 || i == 2))
                atoms.add(r.getAtom(" O  ")); // add central 2 C=O oxygens*/
            if(whichAtoms.equals("ca+o") && (i == 0 || i == 1 || i == 2))
                atoms.add(r.getAtom(" O  ")); // add central 3 C=O oxygens
        }
        
        // Extract their states
        ArrayList states = new ArrayList();
        for(int i = 0; i < atoms.size(); i++)
        {
            try
            {
                Atom a = (Atom) atoms.get(i);
                if(a != null)  states.add(state.get(a));
            }
            catch(AtomException ex) { ex.printStackTrace(); } // should never happen
        }
        
        // Make them into an array
        AtomState[] ret = (AtomState[])states.toArray(new AtomState[states.size()]);
        return ret;
    }
//}}}

//{{{ interrelateAltConfs
//##############################################################################
    /**
    * Tries a shear then backrubs then peptide rotations
    * one or more times successively in order to 
    * relate the two ModelStates stored in this class
    * at the four residues stored in this class.
    */
    ModelState interrelateAltConfs(int numTrials, double maxTheta)
    {
        ModelState bestState = null;
        
        if(verbose) System.err.print("Original         ");
        double origRmsd = calcRmsd(atoms1, atoms2);
        if(verbose) System.err.println(df.format(origRmsd)+"  0  0,0  0,0,0");
        else System.out.print(delim+df.format(origRmsd));
        
        if(verbose) System.err.print("Brub+Peps        ");
        bestState = runTrials(numTrials, maxTheta, false, true, true);
        
        if(verbose) System.err.print("Shear+Peps       ");
        bestState = runTrials(numTrials, maxTheta, true, false, true);
        
        if(verbose) System.err.print("Shear+Brub       ");
        bestState = runTrials(numTrials, maxTheta, true, true, false);
        
        if(verbose) System.err.print("Shear+Brub+Peps  ");
        bestState = runTrials(numTrials, maxTheta, true, true, true);
        
        if(verbose) System.err.println();
        return bestState;
    }
//}}}

//{{{ runTrials
//##############################################################################
    ModelState runTrials(int numTrials, double maxTheta, boolean doShear, boolean doBackrubs, boolean doPepRots)
    {
        // Reset stats
        ModelState bestState = null;
        double bestRmsd = Double.POSITIVE_INFINITY;
        bestShear    = 0;
        bestBackrub1 = 0;
        bestBackrub2 = 0;
        bestPepRot1  = 0;
        bestPepRot2  = 0;
        bestPepRot3  = 0;
        
        // Start trials
        ModelState trialState = state1.createCollapsed(); // only 1st trial -- changed after that
        for(int i = 0; i < numTrials; i++)
        {
            if(doShear)
            {
                trialState = findBestShear(trialState, maxTheta);
            }
            if(doBackrubs)
            {
                trialState = findBestBackrub(trialState, maxTheta, "first");
                trialState = findBestBackrub(trialState, maxTheta, "second");
            }
            if(doPepRots)
            {
                trialState = findBestPeptideRotation(trialState, maxTheta, "first");
                trialState = findBestPeptideRotation(trialState, maxTheta, "second");
                trialState = findBestPeptideRotation(trialState, maxTheta, "third");
            }
            
            atoms1 = loadAtomStates(trialState, (Residue[]) res.toArray(new Residue[res.size()]));
            double rmsd = calcRmsd(atoms1, atoms2);
            if(bestState == null || rmsd < bestRmsd)
            {
                bestState = trialState;
                bestRmsd = rmsd;
            }
        }
        
        // Report results 
        if(verbose) System.err.println(
            df.format(bestRmsd)+"  "+
            df2.format(bestShear)+"  "+
            df2.format(bestBackrub1)+delim+df2.format(bestBackrub2)+"  "+
            df2.format(bestPepRot1)+delim+df2.format(bestPepRot2)+delim+df2.format(bestPepRot3));
        //else System.out.print(delim+
        //    df.format(bestRmsd)+delim+
        //    df.format(bestShear)+delim+
        //    df.format(bestBackrub1)+delim+df.format(bestBackrub2)+delim+
        //    df.format(bestPepRot1)+delim+df.format(bestPepRot2)+delim+df.format(bestPepRot3));
        else System.out.print(delim+df.format(bestRmsd));
        return bestState;
    }
//}}}

//{{{ findBestShear
//##############################################################################
    /**
    * Finds best shear in terms of C-alpha + C=O RMSD 
    * that doesn't violate Ramachandran and tau considerations
    */
    ModelState findBestShear(ModelState trialState, double maxTheta)
    {
        ModelState bestState = trialState.createCollapsed();
        ModelState newState = null;
        double bestRmsd = Double.POSITIVE_INFINITY;
        double bestTheta = 0; // addition to global best theta
        for(double theta = -1 * maxTheta; theta < maxTheta; theta += 0.1)
        {
            try
            {
                // Do the shear
                ArrayList residues = res;
                newState = CaShear.makeConformation(residues, trialState, theta, idealizeSC);
                
                // Decide whether to keep
                atoms1 = loadAtomStates(newState, new Residue[] 
                    {res.get(0), res.get(1), res.get(2), res.get(3)});
                double rmsd = calcRmsd(atoms1, atoms2);
                if(geometryIsValid(newState) && rmsd < bestRmsd)
                {
                    bestRmsd = rmsd;
                    bestTheta = theta;
                    bestState = newState;
                }
            }
            catch(AtomException ex) {}
        }
        
        //if(verbose) System.err.print("    best shear        "
        //    +(bestTheta > 0 ? " +" : " ")+df.format(bestTheta));
        bestShear += bestTheta;
        //if(verbose) System.err.println(" -> "+df.format(bestShear));
        return bestState;
    }
//}}}

//{{{ findBestBackrub
//##############################################################################
    /**
    * Finds best backrub (singular) in terms of C-alpha + C=O RMSD 
    * that doesn't violate Ramachandran and tau considerations
    */
    ModelState findBestBackrub(ModelState trialState, double maxTheta, String which)
    {
        ModelState bestState = trialState.createCollapsed();
        ModelState newState = null;
        double bestRmsd = Double.POSITIVE_INFINITY;
        double bestTheta = 0; // addition to global best theta
        for(double theta = -1 * maxTheta; theta < maxTheta; theta += 0.1)
        {
            try
            {
                // Do the backrub
                ArrayList residues = new ArrayList();
                if(which.equals("first"))         // residues 1-2-3
                {
                    residues.add(res.get(0));
                    residues.add(res.get(1));
                    residues.add(res.get(2));
                }
                else //if(which.equals("second")) // residues 2-3-4
                {
                    residues.add(res.get(1));
                    residues.add(res.get(2));
                    residues.add(res.get(3));
                }
                newState = CaRotation.makeConformation(residues, trialState, theta, idealizeSC);
                
                // Decide whether to keep
                atoms1 = loadAtomStates(newState, new Residue[] 
                    {res.get(0), res.get(1), res.get(2), res.get(3)});
                double rmsd = calcRmsd(atoms1, atoms2);
                if(geometryIsValid(newState) && rmsd < bestRmsd)
                {
                    bestRmsd = rmsd;
                    bestTheta = theta;
                    bestState = newState;
                }
            }
            catch(AtomException ex) {}
        }
        
        //if(verbose) System.err.print("    best backrub "
        //    +(which.equals("first") ? "(1-3)" : "(2-4)")
        //    +(bestTheta > 0 ? " +" : " ")+df.format(bestTheta)+" -> ");
        if(which.equals("first"))
        {
            bestBackrub1 += bestTheta;
            //if(verbose) System.err.println(df.format(bestBackrub1));
        }
        else //if(which.equals("second"))
        {
            bestBackrub2 += bestTheta;
            //if(verbose) System.err.println(df.format(bestBackrub2));
        }
        
        return bestState;
    }
//}}}

//{{{ findBestPeptideRotation
//##############################################################################
    /**
    * Finds best peptide rotation in terms of C-alpha + C=O RMSD 
    * that doesn't violate Ramachandran and tau considerations
    */
    ModelState findBestPeptideRotation(ModelState trialState, double maxTheta, String which)
    {
        ModelState bestState = trialState.createCollapsed();
        ModelState newState = null;
        double bestRmsd /*bestDist*/ = Double.POSITIVE_INFINITY;
        double bestTheta = 0; // addition to global best theta
        for(double theta = -1 * maxTheta; theta < maxTheta; theta += 0.1)
        {
            try
            {
                // Do the peptide rotation
                Atom o = null;
                Residue[] residues = new Residue[2];
                if(which.equals("first"))        // residues 1-2
                {
                    residues[0] = res.get(0);
                    residues[1] = res.get(1);
                    o = res.get(0).getAtom(" O  ");
                }
                else if(which.equals("second"))  // residues 2-3
                {
                    residues[0] = res.get(1);
                    residues[1] = res.get(2);
                    o = res.get(1).getAtom(" O  ");
                }
                else //if(which.equals("third")) // residues 3-4
                {
                    residues[0] = res.get(2);
                    residues[1] = res.get(3);
                    o = res.get(2).getAtom(" O  ");
                }
                double[] thetas = new double[] {theta};
                boolean[] idealizeSCs = new boolean[] {idealizeSC, idealizeSC};
                newState = CaRotation.twistPeptides(residues, trialState, thetas, idealizeSCs);
                
                // Decide whether to keep
                /*AtomState o1 = newState.get(o);
                AtomState o2 =   state2.get(o);
                double dist = o1.distance(o2);
                if(geometryIsValid(newState) && dist < bestDist)*/
                atoms1 = loadAtomStates(newState, new Residue[] 
                    {res.get(0), res.get(1), res.get(2), res.get(3)});
                double rmsd = calcRmsd(atoms1, atoms2);
                if(geometryIsValid(newState) && rmsd < bestRmsd)
                {
                    /*bestDist = dist;*/
                    bestRmsd = rmsd;
                    bestTheta = theta;
                    bestState = newState;
                }
            }
            catch(AtomException ex) {}
        }
        
        //if(verbose) System.err.print("    best pep rot "
        //    +(which.equals("first") ? "(1-2)" : (which.equals("second") ? "(2-3)" : "(3-4)"))
        //    +(bestTheta > 0 ? " +" : " ")+df.format(bestTheta)+" -> ");
        if(which.equals("first"))
        {
            bestPepRot1 += bestTheta;
            //if(verbose) System.err.println(df.format(bestPepRot1));
        }
        else if(which.equals("second"))
        {
            bestPepRot2 += bestTheta;
            //if(verbose) System.err.println(df.format(bestPepRot2));
        }
        else //if(which.equals("third"))
        {
            bestPepRot3 += bestTheta;
            //if(verbose) System.err.println(df.format(bestPepRot3));
        }
        return bestState;
    }
//}}}

//{{{ geometryIsValid
//##############################################################################
    /** Checks for Ramachandran or tau problems */
    boolean geometryIsValid(ModelState state)
    {
        if(rama.isOutlier(model, res.get(0), state)
        || rama.isOutlier(model, res.get(1), state)
        || rama.isOutlier(model, res.get(2), state)
        || rama.isOutlier(model, res.get(3), state)) return false;
        
        try
        {        
            // Allow 5.5 degree tau deviations like in the BRDEE paper,
            // not just 3.0 like in the BACKRUB tool in KiNG
            if(Math.abs(AminoAcid.getTauDeviation(res.get(0), state)) >= 5.5
            || Math.abs(AminoAcid.getTauDeviation(res.get(1), state)) >= 5.5 
            || Math.abs(AminoAcid.getTauDeviation(res.get(2), state)) >= 5.5
            || Math.abs(AminoAcid.getTauDeviation(res.get(3), state)) >= 5.5) return false;
        }
        catch(AtomException ex) { return false; }
        
        return true;
    }
//}}}

//{{{ calcRmsd
//##############################################################################
    double calcRmsd(Triple[] t1, Triple[] t2)
    {
        double r = 0;
        for(int i = 0; i < t1.length; i++)
            r += t1[i].sqDistance(t2[i]);
        r = Math.sqrt(r / t1.length);
        return r;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

