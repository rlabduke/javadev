// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.backrub;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb.*;
import driftwood.r3.*;
import chiropraxis.sc.*;
import chiropraxis.rotarama.*;
//}}}
/**
* <code>Backrub</code> performs a simple kind of Chiropraxis
* on a region of protein backbone that includes just two mobile
* peptides.
*
* <p>The C-alpha at position i is allowed to move, but those
* at i+1 and i-1 are fixed in place and fixed in orientation
* to the rest of the backbone, though their sidechains can move.
* The major rotation is the one around the (i-1)--(i+1) axis,
* which is the most important parameter from a user's point of view.
* The minor rotations occur about the (i-1)--(i) and (i)--(i+1) axes,
* which allows the rigid peptides to relieve deviation in tau
* that was caused by the first (major) rotation without respect to
* geometry at the pivot points (i.e. the i-1 and i+1 C-alphas)
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Mar 19 14:11:04 EST 2003
*/
public class Backrub //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    // Residues 1 (i-1), 2 (i), and 3 (i+1)
    AminoAcid orig1,   orig2,   orig3;     // original coords
    AminoAcid first1,  first2,  first3;    // coords after major rotation
    AminoAcid second1, second2, second3;   // coords after peptide rotations
    
    // Atoms sets for transformation: peptides 1 (i-1 to i) and 2 (i to i+1)
    Atom[] peptide1, peptide2, bothPeptides;
    
    Ramachandran rama;
//}}}

//{{{ INNER CLASS: Constraints
//##################################################################################################
    /**
    * A set of constraints that govern how the backrub process
    * is performed.
    * The xxxStart fields specify the minimum rotation angle,
    * the xxxEnd fields specify the maximum rotation angle,
    * and the xxxStep fields specify the granularity of sampling.
    * The xxxBest fields are overwritten by optimizeConformation().
    * All angles are in degrees.
    */
    public static class Constraints
    {
        public double majorAngle    = 0.0;
        public double minor1Start   = -180.0;
        public double minor1End     = 180.0;
        public double minor1Step    = 1.0;
        public double minor2Start   = -180.0;
        public double minor2End     = 180.0;
        public double minor2Step    = 1.0;
        
        public double minor1Best    = 0.0;
        public double minor2Best    = 0.0;
    }
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Backrub(Residue res)
    {
        if(res == null || res.getPrev() == null || res.getNext() == null)
            throw new IllegalArgumentException("Residue must have upstream and downstream neighbors");
        
        try { this.rama = Ramachandran.getInstance(); }
        catch(IOException ex) { this.rama = null; }
        
        // Contain original atoms from res
        orig1 = new AminoAcid(res.getPrev());
        orig2 = new AminoAcid(res);
        orig3 = new AminoAcid(res.getNext());
        
        // Clones for holding major rotation
        first1 = (AminoAcid)orig1.clone();
        first2 = (AminoAcid)orig2.clone();
        first3 = (AminoAcid)orig3.clone();
        bothPeptides = makeFullArray(first1, first2, first3);
        
        // Clones for holding minor rotations
        second1 = (AminoAcid)first1.clone();
        second2 = (AminoAcid)first2.clone();
        second3 = (AminoAcid)first3.clone();
        peptide1 = makeArray1(second1, second2);
        peptide2 = makeArray2(second2, second3);
    }
//}}}

//{{{ makeFullArray
//##################################################################################################
    Atom[] makeFullArray(AminoAcid m1, AminoAcid ctr, AminoAcid p1)
    {
        ArrayList atoms = new ArrayList();
        //atoms.add(m1.N);
        //atoms.add(m1.CA);
        atoms.add(m1.C);
        atoms.add(m1.O);
        //if(m1.H != null) atoms.add(m1.H);
        atoms.add(ctr.N);
        atoms.add(ctr.CA);
        atoms.add(ctr.C);
        atoms.add(ctr.O);
        if(ctr.H != null) atoms.add(ctr.H);
        // Transform the side chain along the major rotation
        atoms.addAll(ctr.sc.values());
        atoms.add(p1.N);
        atoms.add(p1.CA);
        //atoms.add(p1.C);
        //atoms.add(p1.O);
        if(p1.H != null) atoms.add(p1.H);
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
//}}}

//{{{ makeArray1, makeArray2
//##################################################################################################
    Atom[] makeArray1(AminoAcid m1, AminoAcid ctr)
    {
        ArrayList atoms = new ArrayList();
        atoms.add(m1.C);
        atoms.add(m1.O);
        atoms.add(ctr.N);
        if(ctr.H != null) atoms.add(ctr.H);
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
    
    Atom[] makeArray2(AminoAcid ctr, AminoAcid p1)
    {
        ArrayList atoms = new ArrayList();
        atoms.add(ctr.C);
        atoms.add(ctr.O);
        atoms.add(p1.N);
        if(p1.H != null) atoms.add(p1.H);
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
//}}}

//{{{ likenBackbone, resetMajor, resetMinor
//##################################################################################################
    /** Goes through and calls like(src) for all atoms in dest. */
    void likenBackbone(AminoAcid src, AminoAcid dest)
    {
        dest.N.like(src.N);
        dest.CA.like(src.CA);
        dest.C.like(src.C);
        dest.O.like(src.O);
        
        if(src.H != null)   dest.H.like(src.H);
        if(src.HA != null)  dest.HA.like(src.HA);
        if(src.HA1 != null) dest.HA1.like(src.HA1);
        if(src.HA2 != null) dest.HA2.like(src.HA2);
        //if(src.CB != null)  dest.CB.like(src.CB);
        
        String atomName;
        Atom srcAtom, destAtom;
        for(Iterator iter = src.sc.keySet().iterator(); iter.hasNext(); )
        {
            atomName    = (String)iter.next();
            srcAtom     = (Atom)src.sc.get(atomName);
            destAtom    = (Atom)dest.sc.get(atomName);
            destAtom.like(srcAtom);
        }
    }
    
    /** Resets the atom positions in first to those of orig */
    void resetMajor()
    {
        likenBackbone(orig1, first1);
        likenBackbone(orig2, first2);
        likenBackbone(orig3, first3);
    }
    
    /** Resets the atom positions in second to those of first */
    void resetMinor()
    {
        likenBackbone(first1, second1);
        likenBackbone(first2, second2);
        likenBackbone(first3, second3);
    }
//}}}

//{{{ transformAtoms
//##################################################################################################
    /** Applies a Transform to all the Atoms in the given array */
    void transformAtoms(Transform t, Atom[] atoms)
    {
        for(int i = 0; i < atoms.length; i++)
        {
            t.transform(atoms[i]);
        }
    }
//}}}

//{{{ makeConformation
//##################################################################################################
    /**
    * Generates some AminoAcids with cloned atoms representing the given transformation.
    * @return a Collection of AminoAcids representing the modified structure
    */
    public Collection makeConformation(double majorAngle, double minorAngle1, double minorAngle2)
    {
        AminoAcid ret1, ret2, ret3;
        ret1 = (AminoAcid)orig1.clone();
        ret2 = (AminoAcid)orig2.clone();
        ret3 = (AminoAcid)orig3.clone();
        
        Atom[] arrFull, arr1, arr2;
        arrFull = makeFullArray(ret1, ret2, ret3);
        arr1    = makeArray1(ret1, ret2);
        arr2    = makeArray2(ret2, ret3);
        
        Transform majorRot = new Transform().likeRotation(ret1.CA, ret3.CA, majorAngle);
        transformAtoms(majorRot, arrFull);
        
        Transform minorRot1 = new Transform().likeRotation(ret1.CA, ret2.CA, minorAngle1);
        transformAtoms(minorRot1, arr1);
        
        Transform minorRot2 = new Transform().likeRotation(ret2.CA, ret3.CA, minorAngle2);
        transformAtoms(minorRot2, arr2);
        
        IdealSidechain.idealizeCB(ret1);
        IdealSidechain.idealizeCB(ret2);
        IdealSidechain.idealizeCB(ret3);
        
        return Arrays.asList(new AminoAcid[] { ret1, ret2, ret3 });
    }
//}}}

//{{{ optimizeConformation
//##################################################################################################
    /**
    * Tries different conformations of the peptides, given a major angle.
    * On return, constr.minor{1,2}Best have been filled in with values
    * that minimize tau deviation and (hopefully) keep the chain out of
    * disallowed regions of the Ramachandran plot
    * @param constr the limits on rotation
    */
    public void optimizeConformation(Constraints constr)
    {
        // Establish the major rotation
        resetMajor();
        Transform majorRot = new Transform().likeRotation(first1.CA, first3.CA, constr.majorAngle);
        transformAtoms(majorRot, bothPeptides);
        
        // Variables for the minor rotations
        double minor1, minor2;
        double minor1Best = 0.0, minor2Best = 0.0;
        double tauDev, bestTauDev = Double.POSITIVE_INFINITY;
        boolean isRamaOK, bestRamaOK = false;
        
        Transform minor1Rot, minor2Init, minor2Step;
        minor1Rot   = new Transform();
        minor2Init  = new Transform();
        minor2Step  = new Transform();
        
        // Loop over specified possibilities for angles
        for(minor1 = constr.minor1Start; minor1 <= constr.minor1End; minor1 += constr.minor1Step)
        {
            resetMinor();
            minor1Rot.likeRotation(second1.CA, second2.CA, minor1);
            transformAtoms(minor1Rot, peptide1);
            
            minor2Init.likeRotation(second2.CA, second3.CA, constr.minor2Start);
            transformAtoms(minor2Init, peptide2);
            minor2Step.likeRotation(second2.CA, second3.CA, constr.minor2Step);
            
            for(minor2 = constr.minor2Start; minor2 <= constr.minor2End; minor2 += constr.minor2Step)
            {
                // IF we were a Rama outlier and now are not,
                // OR IF our Rama status hasn't changed but our taus have improved,
                // THEN record this as the best conformation yet.
                isRamaOK = isRamaOK(second1, second2, second3);
                tauDev = getRmsTauDev(second1, second2, second3);
                if((!bestRamaOK && isRamaOK)
                || (bestRamaOK == isRamaOK && bestTauDev > tauDev))
                {
                    minor1Best  = minor1;
                    minor2Best = minor2;
                    bestRamaOK  = isRamaOK;
                    bestTauDev  = tauDev;
                }
                
                // Rotate the second peptide by another step
                transformAtoms(minor2Step, peptide2);
            }
        }
        
        // Record the optimal angles for the caller
        constr.minor1Best = minor1Best;
        constr.minor2Best = minor2Best;
    }
//}}}

//{{{ isRamaOK
//##################################################################################################
    /**
    * Returns true iff all three backbone segments are not
    * Ramachandran outliers.
    */
    public boolean isRamaOK(AminoAcid m1, AminoAcid ctr, AminoAcid p1)
    {
        if(rama == null) return true;
        else return !(rama.isOutlier(m1) || rama.isOutlier(ctr) || rama.isOutlier(p1));
    }
    
    /**
    * Returns true iff all backbone segments are not
    * Ramachandran outliers.
    */
    public boolean isRamaOK(Collection aaBackbones)
    {
        if(rama == null) return true;
        else
        {
            AminoAcid mc;
            for(Iterator iter = aaBackbones.iterator(); iter.hasNext(); )
            {
                mc = (AminoAcid)iter.next();
                if(rama.isOutlier(mc)) return false;
            }
            return true;
        }
    }
//}}}

//{{{ getTauDev(), getRmsTauDev()
//##################################################################################################
    /**
    * Returns the deviation of tau from ideal for the given conformation.
    * Values for ideal angles and standard deviations from the mean
    * are taken from Engh and Huber (1991).
    */
    public double getTauDev(AminoAcid aa)
    {
        String type = aa.getResidue().getType();
        if(type.equals("GLY"))      return aa.getTau() - 112.5;
        else if(type.equals("PRO")) return aa.getTau() - 111.8;
        else                        return aa.getTau() - 111.2;
    }
    
    
    /**
    * Returns the root-mean-square deviation of tau from ideal
    * for the three possible taus (in degrees).
    */
    public double getRmsTauDev(AminoAcid m1, AminoAcid ctr, AminoAcid p1)
    {
        double dev, rmsd = 0;

        dev = getTauDev(m1);
        rmsd += dev*dev;
        dev = getTauDev(ctr);
        rmsd += dev*dev;
        dev = getTauDev(p1);
        rmsd += dev*dev;

        return Math.sqrt(rmsd);
    }
//}}}

//{{{ getWorstTauDev()
//##################################################################################################
    /**
    * Returns the absolute value of the largest deviation from ideal
    * for the three possible taus (in degrees).
    * Values for ideal angles and standard deviations from the mean
    * are taken from Engh and Huber (1991).
    */
    public double getWorstTauDev(AminoAcid m1, AminoAcid ctr, AminoAcid p1)
    {
        //TODO: report in sigma?
        // stddev(18aa) = 2.8
        // stddev(Gly ) = 2.9
        // stddev(Pro ) = 2.5
        double dev, worstDev = 0;
        
        dev = Math.abs(getTauDev(m1));
        if(dev > worstDev) worstDev = dev;
        dev = Math.abs(getTauDev(ctr));
        if(dev > worstDev) worstDev = dev;
        dev = Math.abs(getTauDev(p1));
        if(dev > worstDev) worstDev = dev;

        return worstDev;
    }

    /**
    * Returns the absolute value of the largest deviation from ideal
    * for all the taus (in degrees).
    * Values for ideal angles and standard deviations from the mean
    * are taken from Engh and Huber (1991).
    */
    public double getWorstTauDev(Collection aaBackbones)
    {
        double dev, worstDev = 0;
        for(Iterator iter = aaBackbones.iterator(); iter.hasNext(); )
        {
            dev = Math.abs(getTauDev((AminoAcid)iter.next()));
            if(dev > worstDev) worstDev = dev;
        }
        
        return worstDev;
    }
//}}}

//{{{ updateModel
//##################################################################################################
    /**
    * Copies the coordinates from the supplied AminoAcids
    * into the original atoms of the model.
    */
    public void updateModel(Collection aaBackbones)
    {
        if(aaBackbones.size() != 3)
            throw new IllegalArgumentException("Must supply exactly 3 AminoAcid objects");
        
        Iterator iter = aaBackbones.iterator();
        AminoAcid m1, ctr, p1;
        m1  = (AminoAcid)iter.next();
        ctr = (AminoAcid)iter.next();
        p1  = (AminoAcid)iter.next();
        
        likenBackbone(m1,   orig1);
        likenBackbone(ctr,  orig2);
        likenBackbone(p1,   orig3);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

