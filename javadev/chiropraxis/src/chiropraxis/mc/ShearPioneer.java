// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.Strings;
import chiropraxis.rotarama.*;
//}}}
/**
* <code>ShearPioneer</code> explores the effects of different shear motions 
* in real space and Ramachandran space, focusing on helical structure.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu Dec. 9, 2010
*/
public class ShearPioneer //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df  = new DecimalFormat("0");
    DecimalFormat df2 = new DecimalFormat("0.#");
    DecimalFormat df3 = new DecimalFormat("#");
    DecimalFormat df4 = new DecimalFormat("#.##");
    DecimalFormat df5 = new DecimalFormat("0.0");
    String STREAKS = "phi/psi streaks kin";
    String STRUCTS = "local structures kin";
    String GOOD_COLOR = "greentint";
    String BAD_COLOR = "hotpink";
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean  verbose       = false;
    
    /** Kinemage style to output: either phi/psi streaks (STREAKS, default)
    * or actual local structures (STRUCTS). */
    String   outputMode    = STREAKS;
    
    /** If true and output mode is STRUCTS, print in PDB instead of kin format. */
    boolean  pdbOut        = false;
    
    String   filename      = null;   // mututally
    boolean  useIdealHelix = false;  // exclusive
    
    /** Residue number i for i to i+4 shears (or i to i+3 backrubs when inherited). */
    int      resnum        = Integer.MAX_VALUE;
    
    /** Extent of deviation (in degrees) for phi and psi relative to ideal
    * helix resource PDB file.  NaN means the original phi,psi will be used. */
    double   phipsiRange   = Double.NaN;
    
    /** Spacing of deviation (in degrees) for phi and psi relative to ideal 
    * helix resource PDB file. */
    double   phipsiSpacing = 1.0;
    
    /** For detecting Rama outliers for mobile residues. */
    Ramachandran  rama = null;
    
    /** Maximum tau deviation for all mobile residues (5.5 in BRDEE paper). */
    double   maxTauDev     = 5.5;
    
    /** Maximum primary shear rotation angle in each direction. */
    double   maxTheta      = 15.0;
    
    /** Spacing in degrees between sample points on main shear trajectory. */
    double   thetaSpacing  = 1.0;
    
    /** Fraction of rotations that would most closely restore positions of 
    * flanking oxygens. */
    double   epsilon       = 0.7;
    
    /** If both are NaN, iterate through epsilon values from min to max, ignoring 
    * the default epsilon value.  Alternative to non-NaN initial phi,psi range. */
    double   minEpsilon    = Double.NaN;
    double   maxEpsilon    = Double.NaN;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ShearPioneer()
    {
        super();
        
        try { rama = Ramachandran.getInstance(); }
        catch(IOException ex) {}
    }
//}}}

//{{{ processModel
//##############################################################################
    /** 
    * Sets up to do a series of moves, with varying initial phi,psi and/or 
    * epsilon restoration values for the first and last oxygens.
    */
    void processModel(Model model)
    {
        System.out.println("@kinemage {"+resnum+"-"+(resnum+3)+" "
            +(outputMode == STREAKS ? "streaks" : "structs")+"}");
        
        Residue res = findResidue(model);
        ModelState state = model.getState();
        if(!Double.isNaN(phipsiRange))
        {
            try
            {
                // Try a grid of near-alpha helices in ideal geomtry
                double origPhi = AminoAcid.getPhi(model, res, state); // was hard-coded as -60
                double origPsi = AminoAcid.getPsi(model, res, state); // was hard-coded as -40
                double minPhi = origPhi - phipsiRange;
                double maxPhi = origPhi + phipsiRange;
                double minPsi = origPsi - phipsiRange;
                double maxPsi = origPsi + phipsiRange;
                boolean reverse = true;
                for(double initPhi = minPhi; initPhi <= maxPhi; initPhi += phipsiSpacing) 
                {
                    reverse = !reverse;
                    for(double initPsi = minPsi; initPsi <= maxPsi; initPsi += phipsiSpacing)
                    {
                        // Reorder grid traversal to produce a pleasant snake-like pattern
                        double trueInitPsi = initPsi;
                        if(reverse) trueInitPsi = maxPsi - (initPsi - minPsi);
                        
                        // Alter state then do shears (or backrubs when inherited)
                        ModelState initState = initalizePhiPsi(model, state, initPhi, trueInitPsi);
                        String label = " ("+df.format(initPhi)+","+df.format(trueInitPsi)+")";
                        doMoveSeries(model, initState, res, label);
                    }
                }
            }
            catch(AtomException ex)
            { System.err.println("D'oh!  Can't compute phi/psi for "+res); }
            catch(ResidueException ex)
            { System.err.println("D'oh!  Can't compute phi/psi for "+res); }
        }
        else if(!Double.isNaN(minEpsilon) && !Double.isNaN(maxEpsilon))
        {
            // Try a range of epsilon values on a single input model
            for(double e = minEpsilon; e <= maxEpsilon; e += 0.1)
            {
                epsilon = e; // global
                doMoveSeries(model, state, res);
            }
        }
        else
        {
            // Just use the single input model
            System.out.println("@group {"+resnum+"-"+(resnum+3)
                +" ep="+df2.format(epsilon)+"} animate dominant");
            doMoveSeries(model, state, res);
        }
    }
//}}}

//{{{ findResidue 
//##############################################################################
    /**
    * Finds the user's requested residue (based on residue number)
    * and does some simple checks before proceeding.
    */
    Residue findResidue(Model model)
    {
        Residue res = null;
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            if(r.getSequenceInteger() == resnum) { res = r; break; }
        }
        
        // Need at least 1 preceding residue and 4 subsequent residues
        // to measure phi,psi for all 4 shear residues
        if(res == null)
        {
            System.err.println("D'oh!  Can't find residue # "+resnum);
            System.exit(0);
        }
        if(res.getPrev(model) == null)
        {
            System.err.println("D'oh!  Need a residue preceding "+res);
            System.exit(0);
        }
        Residue r = res;
        for(int i = 0; i < 4; i++)
        {
            r = r.getNext(model);
            if(r == null)
            {
                System.err.println("D'oh!  Need 4 residues following "+res);
                System.exit(0);
            }
        }
        return res;
    }
//}}}

//{{{ initalizePhiPsi 
//##############################################################################
    /**
    * Directly alters the given ModelState by setting phi/psi to the given values
    * for every residue (except maybe the ends).
    */
    ModelState initalizePhiPsi(Model model, ModelState state, double initPhi, double initPsi)
    {
        ModelState initState = new ModelState(state);
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            //initState = setPhiOrPsi(res, model,     state, "phi", initPhi);
            initState = setPhiOrPsi(res, model, initState, "phi", initPhi);
            initState = setPhiOrPsi(res, model, initState, "psi", initPsi);
        }
        
        return initState;
    }
//}}}

//{{{ setPhiOrPsi
//##############################################################################
    /**
    * Directly modifies the given ModelState by setting phi or psi to the given value
    * for the given residue.
    */
    ModelState setPhiOrPsi(Residue res, Model model, ModelState state, String phiOrPsi, double endAngle)
    {
        Residue prev = res.getPrev(model);
        Residue next = res.getNext(model);
        if(prev == null && phiOrPsi.equals("phi"))
        {
            if(verbose) System.err.println("Setting "+phiOrPsi+" for "+res+" ... ERROR: no previous residue!");
            return state;
        }
        else if(next == null && phiOrPsi.equals("psi"))
        {
            if(verbose) System.err.println("Setting "+phiOrPsi+" for "+res+" ... ERROR: no next residue!");
            return state;
        }
        
        ArrayList allAtoms = new ArrayList<Atom>();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue currRes = (Residue) iter.next();
            if(currRes != null)
                for(Iterator iter2 = currRes.getAtoms().iterator(); iter2.hasNext(); )
                    allAtoms.add((Atom)iter2.next());
        }
        
        AtomState a1 = null, a2 = null, a3 = null, a4 = null;
        try
        {
            if(phiOrPsi.equals("phi"))
            {
                a1 = state.get(prev.getAtom(" C  "));
                a2 = state.get( res.getAtom(" N  "));
                a3 = state.get( res.getAtom(" CA "));
                a4 = state.get( res.getAtom(" C  "));
            }
            else if(phiOrPsi.equals("psi"))
            {
                a1 = state.get( res.getAtom(" N  "));
                a2 = state.get( res.getAtom(" CA "));
                a3 = state.get( res.getAtom(" C  "));
                a4 = state.get(next.getAtom(" N  "));
            }
            
            double startAngle = Triple.dihedral(a1, a2, a3, a4);
            double dTheta = endAngle - startAngle;
            Transform rot = new Transform();
            rot.likeRotation(a2, a3, dTheta);
            
            ModelState newState = new ModelState(state);
            for(Iterator iter = allAtoms.iterator(); iter.hasNext(); )
            {
                Atom atom = (Atom) iter.next();
                a1 = state.get(atom);
                a2 = (AtomState) a1.clone();
                
                int targResnum = res.getSequenceInteger();
                int thisResnum = atom.getResidue().getSequenceInteger();
                
                             // 0    5    10   15   20   25
                String order = " H  , N  , CA , HA , C  , O  ";
                int idx = order.indexOf(atom.getName());
                
                if((thisResnum > targResnum)
                || (thisResnum == targResnum && phiOrPsi.equals("phi") && idx >= 10)
                || (thisResnum == targResnum && phiOrPsi.equals("psi") && idx >= 20))
                {
                    // This atom should be affected by the current phi or psi rotation
                    rot.transform(a2);
                    newState.add(a2);
                }
            }
            
            if(verbose) System.err.println("Setting "+phiOrPsi+" for "+res+" ... done");
            return newState;
        }
        catch(AtomException ex)
        {
            if(verbose) System.err.println("Setting "+phiOrPsi+" for "+res+" ... ERROR: missing necessary atoms!");
            return state;
        }
    }
//}}}

//{{{ doMoveSeries 
//##############################################################################
    void doMoveSeries(Model model, ModelState state, Residue res)
    { doMoveSeries(model, state, res, ""); }
    
    /**
    * Performs a series of shears of varying magnitudes, and prints out 
    * some type of kinemage (Rama streaks or local backbone structures).
    */
    void doMoveSeries(Model model, ModelState state, Residue res, String label)
    {
        System.out.println("@group {shr"+resnum+"-"+(resnum+3)+label
            +" ep"+df5.format(epsilon)+"} animate dominant");
        
        Residue res1 = res;
        Residue res2 = res1.getNext(model);
        Residue res3 = res2.getNext(model);
        Residue res4 = res3.getNext(model);
        if(res1 == null || res2 == null || res3 == null || res4 == null)
        {
            System.err.println("Oops, missing residues somewhere in here: "+resnum+"-"+(resnum+3));
            System.exit(0);
        }
        ArrayList<Residue> residues = new ArrayList<Residue>();
        residues.add(res1);
        residues.add(res2);
        residues.add(res3);
        residues.add(res4);
        Residue[] resArray = residues.toArray(new Residue[residues.size()]);
        
        ArrayList<ShearedRegion> movedRegions = new ArrayList<ShearedRegion>();
        for(double theta = -1.0 * maxTheta; theta <= maxTheta; theta += thetaSpacing)
        {
            try
            {
                // Primary shear
                ModelState newState = CaShear.makeConformation(residues, state, theta, false);
                
                // Counter-rotations to restore O1 and O3
                double pepRot1 = calcPepRot(state, newState, res1, res2, theta);
                double pepRot3 = calcPepRot(state, newState, res3, res4, theta);
                double[]  thetas     = new double[]  { pepRot1, 0, pepRot3 };
                boolean[] idealizeSC = new boolean[] { false, false, false, false };
                newState = CaShear.twistPeptides(resArray, newState, thetas, idealizeSC);
                
                // Store data for output
                double[] rots = new double[3];
                rots[0] = theta;
                rots[1] = pepRot1;
                rots[2] = pepRot3;
                double[] phipsi = new double[4];
                phipsi[0] = AminoAcid.getPhi(model, res1, newState);
                phipsi[1] = AminoAcid.getPsi(model, res1, newState);
                phipsi[2] = AminoAcid.getPhi(model, res4, newState);
                phipsi[3] = AminoAcid.getPsi(model, res4, newState);
                boolean badTau = 
                    AminoAcid.getTauDeviation(res1, newState) > maxTauDev ||
                    AminoAcid.getTauDeviation(res2, newState) > maxTauDev ||
                    AminoAcid.getTauDeviation(res3, newState) > maxTauDev ||
                    AminoAcid.getTauDeviation(res4, newState) > maxTauDev;
                boolean ramaOut = 
                    rama.isOutlier(model, res1, state) ||
                    rama.isOutlier(model, res2, state) ||
                    rama.isOutlier(model, res3, state) ||
                    rama.isOutlier(model, res4, state);
                ShearedRegion s = new ShearedRegion(resArray, newState, rots, phipsi, badTau, ramaOut);
                movedRegions.add(s);
            }
            catch(AtomException ex)
            { System.err.println("Can't perform "+df.format(theta)+" degree shear!"); }
            catch(ResidueException ex)
            { System.err.println("Can't get phi/psi after "+df.format(theta)+" degree shear!"); }
        }
        
        // Kinemage
        if(outputMode == STREAKS)
        {
            printStreaks(movedRegions);
        }
        else if(outputMode == STRUCTS)
        {
            printStructs(movedRegions, model);
        }
        else System.err.println("Cannot compute!  Bad output mode: "+outputMode);
    }
//}}}

//{{{ calcPepRot
//##############################################################################
    /** 
    * Calculates the best peptide rotation for restoring the C=O position of 
    * the given peptide, less than or equal to the given theta, and returns 
    * that rotation angle multiplied by epsilon.
    */
    double calcPepRot(ModelState oldState, ModelState newState, Residue r1, Residue r2, double theta)
    {
        if(epsilon == 0) return 0;
        
        try
        {
            Atom calpha1 = r1.getAtom(" CA ");
            Atom calpha2 = r2.getAtom(" CA ");
            Atom oxygen  = r1.getAtom(" O  ");
            
            AtomState newCa1 = newState.get(calpha1); // for axis
            AtomState newCa2 = newState.get(calpha2); // for axis
            AtomState newO   = newState.get(oxygen);  // will get moved
            AtomState oldO   = oldState.get(oxygen);  // for reference only
            
            double bestRot = 0;
            double bestDist = Double.POSITIVE_INFINITY;
            
            double min = -1.5 * Math.abs(theta);
            double max =  1.5 * Math.abs(theta);
            for(double rot = min; rot < max; rot += 0.1)
            {
                Triple rotO = rotate(newCa1, newCa2, rot, new Triple(newO));
                double dist = Triple.distance(rotO, oldO);
                if(dist < bestDist)
                {
                    bestDist = dist;
                    bestRot = rot;
                }
            }
            
            return bestRot * epsilon;
        }
        catch(AtomException ex)
        {
            System.err.println("Error calculating peptide rotation for "+r1+" to "+r2);
            return Double.NaN;
        }
    }
//}}}

//{{{ rotate
//##############################################################################
    /**
    * Rotates the last point by the given angle (in degrees) 
    * around an axis defined by the first two points.
    */
    Triple rotate(Triple axisTail, Triple axisHead, double y, Triple coords)
    {
        Triple axis = new Triple().likeVector(axisTail, axisHead); // br: prevCa, nextCa
        Transform rot = new Transform();
        rot = rot.likeRotation(axis, y);
        
        Triple axisTailToCoords = new Triple().likeVector(axisTail, coords);
        rot.transform(axisTailToCoords);
        Triple newCoords = new Triple().likeSum(axisTail, axisTailToCoords);
        
        return newCoords;
    }
//}}}

//{{{ printStreaks
//##############################################################################
    /** 
    * Prints two streaks of phi/psi points, one for each end of a shear series, 
    * in kinemage format.
    */
    void printStreaks(ArrayList movedRegions)
    {
        System.out.println("@balllist {shear i} radius= 0.3 master= {shear i}");
        for(int i = 0; i < movedRegions.size(); i++)
        {
            ShearedRegion s = (ShearedRegion) movedRegions.get(i);
            System.out.println("{i "
                +s.res1.getName().toLowerCase().trim()+s.res1.getSequenceInteger()
                +" "+df.format(s.theta)+","+df.format(s.pepRot1)+","+df.format(s.pepRot3)
                +" ep="+df2.format(epsilon)+" ("+df.format(s.phi1)+","+df.format(s.psi1)+")}"
                +(s.badTau || s.ramaOut ? BAD_COLOR : GOOD_COLOR)+" "
                +df4.format(s.phi1)+" "+df4.format(s.psi1)); // actual coordinates
        }
        
        System.out.println("@balllist {shear i+3} radius= 0.3 master= {shear i+3}");
        for(int i = 0; i < movedRegions.size(); i++)
        {
            ShearedRegion s = (ShearedRegion) movedRegions.get(i);
            System.out.println("{i+3 "
                +s.res4.getName().toLowerCase().trim()+s.res4.getSequenceInteger()
                +" "+df.format(s.theta)+","+df.format(s.pepRot1)+","+df.format(s.pepRot3)
                +" ep="+df2.format(epsilon)+" ("+df.format(s.phi4)+","+df.format(s.psi4)+")}"
                +(s.badTau || s.ramaOut ? BAD_COLOR : GOOD_COLOR)+" "
                +df4.format(s.phi4)+" "+df4.format(s.psi4)); // actual coordinates
        }
    }
//}}}

//{{{ printStructs
//##############################################################################
    /** 
    * Prints a series of local backbone structures resulting from shears of varying
    * magnitudes in kinemage format.
    */
    void printStructs(ArrayList movedRegions, Model model)
    {
        for(int i = 0; i < movedRegions.size(); i++)
        {
            ShearedRegion s = (ShearedRegion) movedRegions.get(i);
            
            if(pdbOut)
            {
                PdbWriter writer = new PdbWriter(System.out);
                System.out.println("MODEL       "+((i+1) < 10 ? " " : "")+(i+1));
                writer.writeResidues(model.getResidues(), s.state);
            }
            else
            {
                Atom ca1 = s.res1.getAtom(" CA ");
                Atom c1  = s.res1.getAtom(" C  ");
                Atom o1  = s.res1.getAtom(" O  ");
                Atom n2  = s.res2.getAtom(" N  ");
                Atom ca2 = s.res2.getAtom(" CA ");
                Atom c2  = s.res2.getAtom(" C  ");
                Atom o2  = s.res2.getAtom(" O  ");
                Atom n3  = s.res3.getAtom(" N  ");
                Atom ca3 = s.res3.getAtom(" CA ");
                Atom c3  = s.res3.getAtom(" C  ");
                Atom o3  = s.res3.getAtom(" O  ");
                Atom n4  = s.res4.getAtom(" N  ");
                Atom ca4 = s.res4.getAtom(" CA ");
                try
                {
                    AtomState ca1s = s.state.get(ca1);
                    AtomState c1s  = s.state.get(c1);
                    AtomState o1s  = s.state.get(o1);
                    AtomState n2s  = s.state.get(n2);
                    AtomState ca2s = s.state.get(ca2);
                    AtomState c2s  = s.state.get(c2);
                    AtomState o2s  = s.state.get(o2);
                    AtomState n3s  = s.state.get(n3);
                    AtomState ca3s = s.state.get(ca3);
                    AtomState c3s  = s.state.get(c3);
                    AtomState o3s  = s.state.get(o3);
                    AtomState n4s  = s.state.get(n4);
                    AtomState ca4s = s.state.get(ca4);
                    
                    System.out.println("@vectorlist {"+s.toString()
                        +" "+df.format(s.theta)+","+df.format(s.pepRot1)+","+df.format(s.pepRot3)
                        +"} width= 2 color= "+(s.badTau || s.ramaOut ? BAD_COLOR : GOOD_COLOR));
                    printAtomCoords(ca1s, s.res1+" 'CA'", true);
                    printAtomCoords(c1s,  s.res1+" 'C'" , false);
                    printAtomCoords(o1s,  s.res1+" 'O'" , false);
                    printAtomCoords(c1s,  s.res1+" 'C'" , true);
                    printAtomCoords(n2s,  s.res2+" 'N'" , false);
                    printAtomCoords(ca2s, s.res2+" 'CA'", false);
                    printAtomCoords(c2s,  s.res2+" 'C'" , false);
                    printAtomCoords(o2s,  s.res2+" 'O'" , false);
                    printAtomCoords(c2s,  s.res2+" 'C'" , true);
                    printAtomCoords(n3s,  s.res3+" 'N'" , false);
                    printAtomCoords(ca3s, s.res3+" 'CA'", false);
                    printAtomCoords(c3s,  s.res3+" 'C'" , false);
                    printAtomCoords(o3s,  s.res3+" 'O'" , false);
                    printAtomCoords(c3s,  s.res3+" 'C'" , true);
                    printAtomCoords(n4s,  s.res4+" 'N'" , false);
                    printAtomCoords(ca4s, s.res4+" 'CA'", false);
                }
                catch(AtomException ex)
                {
                    System.err.println("Error printing structures for "+s);
                }
            }
        }
    }
//}}}

//{{{ printAtomCoords
//##############################################################################
    /**
    * Prints a point's coordinates in kinemage form with the given point ID.
    */
    void printAtomCoords(Triple coords, String label, boolean p)
    {
        System.out.print("{"+label+"}");
        if(p) System.out.print("P ");
        else  System.out.print("  ");
        System.out.println(
            df4.format(coords.getX())+","+
            df4.format(coords.getY())+","+
            df4.format(coords.getZ()));
    }
//}}}

//{{{ CLASS: ShearedRegion
//##############################################################################
    /**
    * Embodies a local sheared structure and several useful aspects of its geometry.
    */
    public class ShearedRegion //extends ... implements ...
    {
        protected Residue res1, res2, res3, res4;
        protected ModelState state;
        protected double theta, pepRot1, pepRot3;
        protected double phi1, psi1, phi4, psi4;
        protected boolean badTau;
        protected boolean ramaOut;
        
        public ShearedRegion(Residue[] r, ModelState s, double[] rots, double[] pp, boolean bt, boolean ro)
        {
            super();
            
            res1 = r[0];
            res2 = r[1];
            res3 = r[2];
            res4 = r[3];
            state = s;
            
            theta   = rots[0];
            pepRot1 = rots[1];
            pepRot3 = rots[2];
            
            phi1 = pp[0];
            psi1 = pp[1];
            phi4 = pp[2];
            psi4 = pp[3];
            
            badTau = bt;
            ramaOut = ro;
        }
        
        public String toString()
        {
            return res1.getName().toLowerCase().trim()+res1.getSequenceInteger()+"-"+
                   res4.getName().toLowerCase().trim()+res4.getSequenceInteger();
        }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if(filename == null)
        {
            if(useIdealHelix)
            {
                if(verbose) System.err.println("Using ideal helix");
            }
            else
            {
                System.err.println("Using ideal helix (no input file provided)");
                useIdealHelix = true;
            }
        }
        /*if(filename != null && !Double.isNaN(phipsiRange))
        {
            System.err.println("Grid of initial phi,psi requires using ideal helix (-alpha)!");
            System.exit(0);
        }*/
        if(!Double.isNaN(minEpsilon) && !Double.isNaN(maxEpsilon) && !Double.isNaN(phipsiRange))
        {
            System.err.println("Can't use -epsilon=#,# AND -phipsirange=#, silly goose!");
            System.exit(0);
        }
        if(resnum == Integer.MAX_VALUE)
        {
            System.err.println("Need a residue number!  Use -res=i (for i to i+4 shear)");
            System.exit(0);
        }
        if(phipsiRange > 15)
        {
            System.err.println("Init phi,psi range of "+df3.format(phipsiRange)+" too big!  Using 15");
            phipsiRange = 15;
        }
        if(phipsiRange > 0 && outputMode == STRUCTS && !pdbOut)
        {
            System.err.println("Warning: Exact kin coords w/ altered init phi/psi are not"
                +" meaningful w.r.t. original coords!");
        }
        
        try
        {
            PdbReader reader = new PdbReader();
            CoordinateFile cf = null;
            if(filename != null)
                cf = reader.read(new File(filename));
            else
                cf = reader.read(this.getClass().getResourceAsStream("idealpolyala12-alpha.pdb"));
            
            Model m = cf.getFirstModel();
            processModel(m);
        }
        catch(IOException ioe) { System.err.println("Trouble parsing file!"); }
    }

    public static void main(String[] args)
    {
        ShearPioneer mainprog = new ShearPioneer();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}

//{{{ parseArguments, showHelp
//##############################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        String  arg, flag, param;
        boolean interpFlags = true;
        
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") || !interpFlags || arg.equals("-"))
            {
                // This is probably a filename or something
                interpretArg(arg);
            }
            else if(arg.equals("--"))
            {
                // Stop treating things as flags once we find --
                interpFlags = false;
            }
            else
            {
                // This is a flag. It may have a param after the = sign
                int eq = arg.indexOf('=');
                if(eq != -1)
                {
                    flag    = arg.substring(0, eq);
                    param   = arg.substring(eq+1);
                }
                else
                {
                    flag    = arg;
                    param   = null;
                }
                
                try { interpretFlag(flag, param); }
                catch(NullPointerException ex)
                { throw new IllegalArgumentException("'"+arg
                    +"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("ShearPioneer.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'ShearPioneer.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.ShearPioneer");
        System.err.println("Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ interpretArg, interpretFlag
//##############################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
        if(filename == null)  filename = arg;
        else throw new IllegalArgumentException("Only need 1 file!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-structs") || flag.equals("-struct"))
        {
            outputMode = STRUCTS;
        }
        else if(flag.equals("-streaks") || flag.equals("-streak"))
        {
            outputMode = STREAKS;
        }
        else if(flag.equals("-res"))
        {
            try
            {
                resnum = Integer.parseInt(param);
            }
            catch(NumberFormatException nfe)
            { System.err.println("Can't format "+param+" as an integer for resnum!"); }
        }
        else if(flag.equals("-theta"))
        {
            try
            {
                maxTheta = Double.parseDouble(param);
            }
            catch(NumberFormatException nfe) 
            { System.err.println("Can't format "+param+" as a double for maxTheta!"); }
        }
        else if(flag.equals("-epsilon") || flag.equals("-e"))
        {
            try
            {
                String[] parts = Strings.explode(param, ',');
                if(parts.length > 2)
                {
                    System.err.println("Sorry, should be -epsilon=# or -epsilon=#,#");
                }
                else if(parts.length == 2)
                {
                    minEpsilon = Double.parseDouble(parts[0]);
                    maxEpsilon = Double.parseDouble(parts[1]);
                }
                else
                {
                    epsilon = Double.parseDouble(param);
                }
            }
            catch(NumberFormatException nfe)
            { System.err.println("Can't format "+param+" as (a) double(s) for epsilon!"); }
        }
        else if(flag.equals("-maxtaudev"))
        {
            try
            {
                maxTauDev = Double.parseDouble(param);
            }
            catch(NumberFormatException nfe)
            { System.err.println("Can't format "+param+" as a double for maxTauDev!"); }
        }
        else if(flag.equals("-phipsirange") || flag.equals("-initrange"))
        {
            try
            {
                phipsiRange = Double.parseDouble(param);
            }
            catch(NumberFormatException nfe) 
            { System.err.println("Can't format "+param+" as doubles for phipsiRange!"); }
        }
        else if(flag.equals("-alpha"))
        {
            useIdealHelix = true;
        }
        else if(flag.equals("-pdbout"))
        {
            pdbOut = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

