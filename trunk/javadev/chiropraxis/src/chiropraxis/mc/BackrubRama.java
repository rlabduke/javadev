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
//}}}
/**
* <code>BackrubRama</code> performs a series of backrubs on designated 
* residues in a provided PDB structure and outputs the i-1 and i+1 phi,psi 
* values in kinemage format for Ramachandran visualization.
*
* <p>Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Feb. 11, 2007.
*/
public class BackrubRama //extends ... implements ...
{
//{{{ Constants
    PrintStream   out = System.out;
    DecimalFormat df  = new DecimalFormat("###.#");
    DecimalFormat df2 = new DecimalFormat("#");
//}}}

//{{{ Variable definitions
//##############################################################################
    String   filename      = null;
    boolean  verbose       = false;
    boolean  doStructKin   = false;
    String   group         = null;
    int      resnum        = Integer.MAX_VALUE;
    /** Initial and final phi(i-1),psi(i-1),phi(i),psi(i),phi(i+1),psi(i-1). */
    double[] initPhiPsi    = null;
    double[] finalPhiPsi   = null;
    String   alphaOrBeta   = "alpha";
    double   closeCutoff   = Double.NaN; // automatically turned on if finalPhiPsi != null
    boolean  noCloseCutoff = false;      // ... unless this option is provided
    /** Maximum tau deviation for i+/-1 residues (5.5 in BRDEE paper). */
    double   maxTauDev     = 5.5;
    /** Maximum primary backrub rotation angle in each direction. */
    double   maxTheta13    = 15.0;
    /** Spacing between sample points on theta13 trajectory. */
    double   thetaSpacing  = 1.0;
    /** Fraction of rotation that would most closely restore position of O(i). */
    double   epsilon       = 0.7;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BackrubRama()
    {
        super();
    }
//}}}

//{{{ doBackrubs
//##############################################################################
    /** 
    * Do backrubs for all desired residues in the model and store phi,psi values.
    */
    void doBackrubs(Model model)
    {
        if (!doStructKin && initPhiPsi != null)
        {
            out.println("@group {init phi,psi} dominant master= {all}");
            
            out.println("@balllist {init i-1 phi,psi} radius= 2.5 color= greentint master= {i-1}");
            out.println("{init i-1 phi,psi} "+df.format(initPhiPsi[0])+" "
                                             +df.format(initPhiPsi[1])+" 0");
            
            out.println("@balllist {init i+1 phi,psi} radius= 2.5 color= peachtint master= {i+1}");
            if (initPhiPsi.length == 4)
                out.println("{init i+1 phi,psi} "+df.format(initPhiPsi[2])+" "
                                                 +df.format(initPhiPsi[3])+" 0");
            else if (initPhiPsi.length == 6)
                out.println("{init i+1 phi,psi} "+df.format(initPhiPsi[4])+" "
                                                 +df.format(initPhiPsi[5])+" 0");
        }
        if (!doStructKin && finalPhiPsi != null)
        {
            out.println("@group {final phi,psi} dominant master= {all}");
            
            out.println("@balllist {final i-1 phi,psi} radius= 2.5 color= green master= {i-1}");
            out.println("{final i-1 phi,psi} "+df.format(finalPhiPsi[0])+" "
                                              +df.format(finalPhiPsi[1])+" 0");
            
            out.println("@balllist {final i+1 phi,psi} radius= 2.5 color= peach master= {i+1}");
            out.println("{final i+1 phi,psi} "+df.format(finalPhiPsi[2])+" "
                                              +df.format(finalPhiPsi[3])+" 0");
        }
        
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if (finalPhiPsi != null)  epsilon = 0.0;
            if (res.getSequenceInteger() == resnum)
            {
                if (finalPhiPsi != null)
                {
                    // Final "target" phi,psi provided => try range of epsilons to hit it
                    for (epsilon = 0; epsilon <= 1.0; epsilon += 0.1)
                    {
                        ModelState state = model.getState();
                        double[] phipsi = backrubSeries(model, state, res);
                        //System.err.println("*** phipsi = "+phipsi);
                        if (!doStructKin && phipsi != null)  printStreaks(res, phipsi);
                    }
                }
                else
                {
                    // We're not shooting for a final "target" phi,psi
                    ModelState state = model.getState();
                    double[] phipsi = backrubSeries(model, state, res);
                    
                    if (!doStructKin && phipsi != null)  printStreaks(res, phipsi);
                }
            }
        } //for each residue
    }
//}}}

//{{{ backrubSeries
//##############################################################################
    /** 
    * Rotate a dipeptide by a series of primary and secondary backrub angles and
    * returns the phi,psi values traced out.
    */
    double[] backrubSeries(Model model, ModelState state, Residue res)
    {
        if (doStructKin)  out.println("@group {e="+df.format(epsilon)+" y13=["+
            -1*maxTheta13+","+maxTheta13+"] "+res+"} dominant animate master= {all}");
        
        ArrayList<Double> phipsi = new ArrayList<Double>();
        try
        {
            Residue prev = res.getPrev(model);
            Residue next = res.getNext(model);
            
            if (initPhiPsi != null)
            {
                state = setPhiPsi(prev, model, state, "phi", initPhiPsi[0]);
                state = setPhiPsi(prev, model, state, "psi", initPhiPsi[1]);
                if (initPhiPsi.length == 6)
                {
                    state = setPhiPsi( res, model, state, "phi", initPhiPsi[2]);
                    state = setPhiPsi( res, model, state, "psi", initPhiPsi[3]);
                    state = setPhiPsi(next, model, state, "phi", initPhiPsi[4]);
                    state = setPhiPsi(next, model, state, "psi", initPhiPsi[5]);
                }
                else if (initPhiPsi.length == 4)
                {
                    state = setPhiPsi(next, model, state, "phi", initPhiPsi[2]);
                    state = setPhiPsi(next, model, state, "psi", initPhiPsi[3]);
                }
            }
            
            Atom prevCaAtom = prev.getAtom(" CA ");
            Atom nextCaAtom = next.getAtom(" CA ");
            AtomState prevCa = state.get(prevCaAtom);
            AtomState nextCa = state.get(nextCaAtom);
            double origPrevTau = Triple.angle(state.get(prev.getAtom(" N  ")),
                                              state.get(prev.getAtom(" CA ")),
                                              state.get(prev.getAtom(" C  ")));
            double origThisTau = Triple.angle(state.get( res.getAtom(" N  ")),
                                              state.get( res.getAtom(" CA ")),
                                              state.get( res.getAtom(" C  ")));
            double origNextTau = Triple.angle(state.get(next.getAtom(" N  ")),
                                              state.get(next.getAtom(" CA ")),
                                              state.get(next.getAtom(" C  ")));
            for (double y = -1.0*maxTheta13; y <= maxTheta13; y += thetaSpacing)
            {
                if (verbose) System.err.println("Primary rot: "+df.format(y));
                
                // Prepare relevant atoms
                // i-2: C               i  : C
                // i-1: N, Ca, C        i+1: N, Ca, C
                // i  : N               i+2: N
                Residue prevPrev = prev.getPrev(model);
                Residue nextNext = next.getNext(model);
                
                Triple likePrevPrevC = new Triple(state.get(prevPrev.getAtom(" C  ")));
                Triple likePrevN     = new Triple(state.get(    prev.getAtom(" N  ")));
                Triple likePrevCa    = new Triple(state.get(    prev.getAtom(" CA ")));
                Triple likePrevC     = new Triple(state.get(    prev.getAtom(" C  ")));
                Triple likePrevO     = new Triple(state.get(    prev.getAtom(" O  ")));
                Triple likeThisN     = new Triple(state.get(     res.getAtom(" N  ")));
                Triple likeThisCa    = new Triple(state.get(     res.getAtom(" CA ")));
                Triple likeThisC     = new Triple(state.get(     res.getAtom(" C  ")));
                Triple likeThisO     = new Triple(state.get(     res.getAtom(" O  ")));
                Triple likeNextN     = new Triple(state.get(    next.getAtom(" N  ")));
                Triple likeNextCa    = new Triple(state.get(    next.getAtom(" CA ")));
                Triple likeNextC     = new Triple(state.get(    next.getAtom(" C  ")));
                Triple likeNextNextN = new Triple(state.get(nextNext.getAtom(" N  ")));
                
                // Primary rotation of relevant atoms
                likePrevC     = rotate(prevCa, nextCa, y, likePrevC); 
                likePrevO     = rotate(prevCa, nextCa, y, likePrevO); 
                likeThisN     = rotate(prevCa, nextCa, y, likeThisN); 
                likeThisCa    = rotate(prevCa, nextCa, y, likeThisCa);
                likeThisC     = rotate(prevCa, nextCa, y, likeThisC); 
                likeThisO     = rotate(prevCa, nextCa, y, likeThisO); 
                likeNextN     = rotate(prevCa, nextCa, y, likeNextN);
                
                // Secondary counter-rotations of relevant atoms
                Triple origPrevO = state.get(prev.getAtom(" O  "));
                Triple origThisO = state.get( res.getAtom(" O  "));
                double y12 = 0, y23 = 0;
                if (epsilon != 0.0)
                {
                    // i-1 to i
                    double dist = Double.POSITIVE_INFINITY;
                    for (double i = 0; Math.abs(i) <= Math.abs(y); i += thetaSpacing)  
                    {
                        double y12test = (y > 0 ? -1.0*i : i); // want *counter* rotation
                        Triple rotatedO = rotate(prevCa, likeThisCa, y12test, new Triple(likePrevO));
                        if (Triple.distance(rotatedO, origPrevO) < dist)  
                        {
                            dist = Triple.distance(rotatedO, origPrevO);
                            y12 = y12test;
                        }
                    }
                    //y12 = calcCounterRot(prevCa, likeThisCa, state.get(prev.getAtom(" O  ")), likePrevO, y);
                    y12 *= epsilon;
                    if (verbose) System.err.println("Secondary rot i-1: "+df.format(y12)+
                        " (dist = "+dist+")");
                    likePrevC     = rotate(prevCa, likeThisCa, y12, likePrevC);
                    likePrevO     = rotate(prevCa, likeThisCa, y12, likePrevO);
                    likeThisN     = rotate(prevCa, likeThisCa, y12, likeThisN);
                    
                    // i to i+1
                    dist = Double.POSITIVE_INFINITY;
                    for (double i = 0; Math.abs(i) <= Math.abs(y); i += thetaSpacing)  
                    {
                        double y23test = (y > 0 ? -1.0*i : i); // want *counter* rotation
                        Triple rotatedO = rotate(likeThisCa, nextCa, y23test, new Triple(likeThisO));
                        if (Triple.distance(rotatedO, origThisO) < dist)
                        {
                            dist = Triple.distance(rotatedO, origThisO);
                            y23 = y23test;
                        }
                    }
                    //y23 = calcCounterRot(likeThisCa, nextCa, state.get(res.getAtom(" O  ")), likeThisO, y);
                    y23 *= epsilon;
                    if (verbose) System.err.println("Secondary rot i+1: "+df.format(y23)+
                        " (dist = "+dist+")");
                    likeThisC     = rotate(likeThisCa, nextCa, y23, likeThisC); 
                    likeThisO     = rotate(likeThisCa, nextCa, y23, likeThisO); 
                    likeNextN     = rotate(likeThisCa, nextCa, y23, likeNextN);
                }
                
                double prevTau = Triple.angle(likePrevN, likePrevCa, likePrevC);
                double thisTau = Triple.angle(likeThisN, likeThisCa, likeThisC);
                double nextTau = Triple.angle(likeNextN, likeNextCa, likeNextC);
                if (doStructKin)
                {
                    // Print structural kinemage for this residue
                    if (verbose) System.err.println("Testing tau for br("+df.format(y)+", "
                        +df.format(y12)+", "+df.format(y23)+") for '"+prev+"' and '"+next+"' ...");
                    if (tauInRange(prevTau, origPrevTau) && tauInRange(nextTau, origNextTau))
                    {
                        String sign = (y > 0 ? "+" : "");
                        out.println("@vectorlist {"+res+" br: "+sign+y+","+y12+","+y23+
                            "} width= 2 color= peachtint");
                        
                        printAtomCoords(likePrevN,  prev+"   ' N  '", true);
                        printAtomCoords(likePrevCa, prev+"   ' CA '", false);
                        printAtomCoords(likePrevC,  prev+"   ' C  '", false);
                        printAtomCoords(likePrevO,  prev+"   ' O  '", false);
                        printAtomCoords(likePrevC,  prev+"   ' C  '", true);
                        printAtomCoords(likeThisN,  res +"   ' N  '", false);
                        printAtomCoords(likeThisCa, res +"   ' CA '", false);
                        printAtomCoords(likeThisC,  res +"   ' C  '", false);
                        printAtomCoords(likeThisO,  res +"   ' O  '", false);
                        printAtomCoords(likeThisC,  res +"   ' C  '", true);
                        printAtomCoords(likeNextN,  next+"   ' N  '", false);
                        printAtomCoords(likeNextCa, next+"   ' CA '", false);
                        printAtomCoords(likeNextC,  next+"   ' C  '", false);
                    }
                }
                else
                {
                    // Calculate and store new i+/-1 phi,psi and relevant backrub angles
                    if (verbose) System.err.println("Testing tau for br("+df.format(y)+", "
                        +df.format(y12)+", "+df.format(y23)+") for '"+prev+"' and '"+next+"' ...");
                    if (tauInRange(prevTau, origPrevTau) && tauInRange(thisTau, origThisTau) && tauInRange(nextTau, origNextTau))
                    {
                        // i-1
                        double prevPhi = Triple.dihedral(likePrevPrevC, likePrevN, likePrevCa, likePrevC);    
                        double prevPsi = Triple.dihedral(likePrevN, likePrevCa, likePrevC, likeThisN);    
                        phipsi.add(prevPhi); 
                        phipsi.add(prevPsi);
                        
                        // i+1
                        double nextPhi = Triple.dihedral(likeThisC, likeNextN, likeNextCa, likeNextC);
                        double nextPsi = Triple.dihedral(likeNextN, likeNextCa, likeNextC, likeNextNextN);
                        phipsi.add(nextPhi); 
                        phipsi.add(nextPsi);
                        
                        // y13,12,23
                        phipsi.add(y  );
                        phipsi.add(y12);
                        phipsi.add(y23);
                    }
                }
            }
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble with backrub series for "+res);
        }
        
        if (!doStructKin)
        {
            double[] phipsiArray = new double[phipsi.size()];
            for (int i = 0; i < phipsi.size(); i ++)  phipsiArray[i] = phipsi.get(i);
            if (finalPhiPsi == null || noCloseCutoff || (closeEnoughToFinal(phipsiArray)))
                return phipsiArray;
            else   return null;
        }
        return null;
    }
//}}}

//{{{ printStreaks
//##############################################################################
    /** 
    * Prints phi,psi points traced out by a backrub in kinemage format.
    */
    void printStreaks(Residue res, double[] phipsi)
    {
        if (verbose) System.err.println("phipsi.length = "+phipsi.length);
        
        out.println("@group {"+(group == null ? "e="+df.format(epsilon)+" "+res+" streaks" : group)
            +"} dominant animate master= {all}");
        
        // i-1 ("prev")
        out.println("@subgroup {"+res+" i-1 streak} master= {i-1}");
        out.println("@balllist {"+res+" i-1 streak} radius= 0.3");
        for (int i = 0; i < phipsi.length; i += 7)
        {
            double prevPhi = phipsi[i  ];
            double prevPsi = phipsi[i+1];
            double nextPhi = phipsi[i+2];
            double nextPsi = phipsi[i+3];
            int y13 = (int) phipsi[i+4];
            int y12 = (int) phipsi[i+5];
            int y23 = (int) phipsi[i+6];
            
            out.print("{"+res+"   i-1   phi,psi=("
                +df.format(prevPhi)+", "+df.format(prevPsi)+", "
                +df.format(nextPhi)+", "+df.format(nextPsi)
                +")   y13,y12,y23=("+df2.format(y13)+" "+df2.format(y12)+" "+df2.format(y23)
                +")   e="+df.format(epsilon)+"} ");
            out.println(df.format(prevPhi)+" " +df.format(prevPsi)+" 0.000");
        }
        
        // i+1 ("next")
        out.println("@subgroup {"+res+" i+1 streak} master= {i+1}");
        out.println("@balllist {"+res+" i+1 streak} radius= 0.3");
        for (int i = 0; i < phipsi.length; i += 7)
        {
            double prevPhi = phipsi[i  ];
            double prevPsi = phipsi[i+1];
            double nextPhi = phipsi[i+2];
            double nextPsi = phipsi[i+3];
            int y13 = (int) phipsi[i+4];
            int y12 = (int) phipsi[i+5];
            int y23 = (int) phipsi[i+6];
            
            out.print("{"+res+"   i+1   phi,psi=("
                +df.format(prevPhi)+", "+df.format(prevPsi)+", "
                +df.format(nextPhi)+", "+df.format(nextPsi)
                +")   y13,y12,y23=("+df2.format(y13)+" "+df2.format(y12)+" "+df2.format(y23)
                +")   e="+df.format(epsilon)+"} ");
            out.println(df.format(nextPhi)+" " +df.format(nextPsi)+" 0.000");
        }
    }
//}}}

//{{{ rotate, tauInRange, closeEnoughToFinal
//##############################################################################
    /** Implement a rotation of the last point by the given angle (in degrees)
    * around an axis defined by the first two points. */
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

    /** Tells if a given tau is acceptable by either (1) being within ideal +/- 
    * the maximum allowable deviation or (2) starting outside (1) but not getting 
    * any worse. */
    boolean tauInRange(double currTau, double origTau)
    {
        if (currTau > (111-maxTauDev) && currTau < (111+maxTauDev))
        {
            if (verbose) System.err.println("   Tau ("+df.format(currTau)+") in ideal range");
            return true;
        }
        else if (currTau > (111+maxTauDev) && currTau < origTau)
        {
            if (verbose) System.err.println("   Tau ("+df.format(currTau)+") above ideal range"
                +" but < orig ("+df.format(origTau)+")");
            return true;
        }
        else if (currTau < (111-maxTauDev) && currTau > origTau)   
        {
            if (verbose) System.err.println("   Tau ("+df.format(currTau)+") below ideal range"
                +" but > orig ("+df.format(origTau)+")");
            return true;
        }
        else
        {
            if (verbose) System.err.println("   Tau ("+df.format(currTau)+") unacceptable (orig: "+
                df.format(origTau)+")");
            return false;
        }
    }

    /** Decides whether or not a given backrub trajectory lands close enough to
    * the desired "target" conformation in phi,psi space. It must come close to 
    * both the target i-1 and i+1 phi,psi. If a final "target" conformation is
    * not supplied, this will always return true by default. */
    public boolean closeEnoughToFinal(double[] phipsi)
    {
        if (finalPhiPsi == null)  return true;
        
        double finalPrevPhi = finalPhiPsi[0];
        double finalPrevPsi = finalPhiPsi[1];
        double finalNextPhi = finalPhiPsi[2];
        double finalNextPsi = finalPhiPsi[3];
        
        double prevClosest = Double.POSITIVE_INFINITY;
        double nextClosest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < phipsi.length; i += 7)
        {
            double prevPhi = phipsi[i  ];
            double prevPsi = phipsi[i+1];
            double nextPhi = phipsi[i+2];
            double nextPsi = phipsi[i+3];
            
            // TODO: MAKE THIS HANDLE DIHEDRAL WRAPPING!
            double prevDist = Math.sqrt( Math.pow(prevPhi-finalPrevPhi,2) +
                                         Math.pow(prevPsi-finalPrevPsi,2) );
            double nextDist = Math.sqrt( Math.pow(nextPhi-finalNextPhi,2) +
                                         Math.pow(nextPsi-finalNextPsi,2) );
            
            if (prevDist < prevClosest && nextDist < nextClosest)
            {
                prevClosest = prevDist;
                nextClosest = nextDist;
            }
        }
        
        if (verbose) System.err.println("prevClosest = "+prevClosest);
        if (verbose) System.err.println("nextClosest = "+nextClosest);
        
        if (verbose)
        {
            if (prevClosest < closeCutoff && nextClosest < closeCutoff)
                System.err.println("Close enough: "+prevClosest+" & "+nextClosest);
            else
                System.err.println("*** Not close enough: "+prevClosest+" & "+nextClosest);
        }
        if (prevClosest < closeCutoff && nextClosest < closeCutoff)  return true;
        return false;
    }
//}}}

//{{{ setPhiPsi, printAtomCoords
//##############################################################################
    /** Returns a new ModelState with the correct phi,psi for the given residue. */
    public ModelState setPhiPsi(Residue res, Model model, ModelState state, String phiOrPsi, double endAngle) throws AtomException
    {
        Residue prev     =  res.getPrev(model);
        Residue next     =  res.getNext(model);
        //Residue prevPrev = null;
        //Residue nextNext = null;
        //if (phiOrPsi.equals("phi"))  nextNext = next.getNext(model);
        //if (phiOrPsi.equals("psi"))  prevPrev = prev.getPrev(model);
        //
        //ArrayList allAtoms = new ArrayList<Atom>();
        //if (prevPrev != null)  for (Iterator iter = prevPrev.getAtoms().iterator(); iter.hasNext(); )
        //    allAtoms.add((Atom)iter.next());
        //for (Iterator iter = prev.getAtoms().iterator(); iter.hasNext(); )
        //    allAtoms.add((Atom)iter.next());
        //for (Iterator iter =  res.getAtoms().iterator(); iter.hasNext(); )
        //    allAtoms.add((Atom)iter.next());
        //for (Iterator iter = next.getAtoms().iterator(); iter.hasNext(); )
        //    allAtoms.add((Atom)iter.next());
        //if (nextNext != null)  for (Iterator iter = nextNext.getAtoms().iterator(); iter.hasNext(); )
        //    allAtoms.add((Atom)iter.next());
        ArrayList allAtoms = new ArrayList<Atom>();
        for (Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue currRes = (Residue)iter.next();
            if (currRes != null)  for (Iterator iter2 = currRes.getAtoms().iterator(); iter2.hasNext(); )
                allAtoms.add((Atom)iter2.next());
        }
        
        AtomState a1 = null, a2 = null, a3 = null, a4 = null;
        if (phiOrPsi.equals("phi"))
        {
            a1 = state.get(prev.getAtom(" C  "));
            a2 = state.get( res.getAtom(" N  "));
            a3 = state.get( res.getAtom(" CA "));
            a4 = state.get( res.getAtom(" C  "));
        }
        else if (phiOrPsi.equals("psi"))
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
        
        ModelState ms = new ModelState(state);
        int resnum = res.getSequenceInteger();
        for(Iterator iter = allAtoms.iterator(); iter.hasNext(); )
        {
            Atom atom = (Atom)iter.next();
            a1 = state.get(atom);
            a2 = (AtomState)a1.clone();
            
            int thisResnum = atom.getResidue().getSequenceInteger();
            //              0    5    10   15   20   25
            String order = " H  , N  , CA , HA , C  , O  ";
            int idx = order.indexOf(atom.getName());
            
            if ((thisResnum == resnum && phiOrPsi.equals("phi") && idx >= 10)
             || (thisResnum == resnum && phiOrPsi.equals("psi") && idx >= 20)
             || (thisResnum > resnum))
            {
                if (verbose) System.err.println("Rotating "+atom+" for "+phiOrPsi+" on "+res);
                rot.transform(a2);
                ms.add(a2);
            }
        }
        
        return ms;
    }

    /** Prints a point's coordinates in kinemage form with the given point ID. */
    void printAtomCoords(Triple coords, String label, boolean p)
    {
        out.print("{"+label+"}");
        if (p)  out.print("P ");
        else    out.print("  ");
        out.println(df.format(coords.getX())+","+df.format(coords.getY())+","+df.format(coords.getZ()));
    }
//}}}

//{{{ calcCounterRot (UNUSED)
//##############################################################################
    /** Find the proper rotation angle around an axis formed by the first two 
    * points to return the given oxygen epsilon (0 to 1) of the way to its 
    * original position. */
    double calcCounterRot(Triple tail, Triple head, Triple origO, Triple currO, double primAngle)
    {
        // 1) Project currO onto tail->head to get center of rotation circle
        Triple tailToHead  = new Triple().likeVector(tail, head);
        Triple tailToCurrO = new Triple().likeVector(tail, currO);
        double scalarProjOntoAxis = (tailToCurrO.mag()) * Math.cos((tailToHead.angle(tailToCurrO)));
        if (scalarProjOntoAxis < 0) scalarProjOntoAxis *= -1;
        Triple tailToCenter = tailToHead.unit().mult(scalarProjOntoAxis);
        Triple center = new Triple(tail).add(tailToCenter);
        
        Triple tailCopy = new Triple(tail);
        
        // 2) Subtract center from all points (make it the origin)
        tail   = new Triple().likeVector(center, tail);
        head   = new Triple().likeVector(center, head);
        origO  = new Triple().likeVector(center, origO);
        currO  = new Triple().likeVector(center, currO);
        center = new Triple().likeVector(center, center); // now at (0,0,0)
        
        // 3) Project origO onto plane of circle (project onto normal and subtract)
        Triple normal = head.unit(); // same vector as Triple.normal(3 pts in circle)
        double scalarProjOntoNormal = (origO.mag()) * Math.cos((normal.angle(origO)));
        if (scalarProjOntoNormal < 0) scalarProjOntoNormal *= -1;
        System.err.println("scalarProjOntoNormal = "+scalarProjOntoNormal);
        normal.mult(scalarProjOntoNormal);
        Triple origOproj = new Triple(origO).sub(normal);
        
        printAtomCoords(new Triple().likeSum(tailCopy, origOproj), "origOproj", false);
        
        // 4) Calculate rotation angle and modify by epsilon for desired result
        double angle = -1.0 * epsilon * Triple.angle(currO, center, origOproj);
        System.err.println("angle b4 mod = "+angle);
        if (Math.abs(angle) > Math.abs(primAngle))
        {
            // Counter-rotation bigger than primary rotation -- don't want that, 
            // so reduce counter-rotation magnitude to that of primary rotation 
            // but preserve relative signs
            double sign = ( (angle * primAngle > 0) ? 1.0 : -1.0);
            angle = sign * primAngle;
        }
        return angle;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if (filename == null && finalPhiPsi == null)
        {
            System.err.println("Need a filename or -to=#,#,#,#!");
            System.exit(0);
        }
        if (filename == null && finalPhiPsi != null)
        {
            resnum = 3; // b/c using resource PDB file
        }
        if (resnum == Integer.MAX_VALUE)
        {
            System.err.println("Need a residue number! (use -res=#)");
            System.exit(0);
        }
        if (finalPhiPsi != null && Double.isNaN(closeCutoff) && !noCloseCutoff)
        {
            closeCutoff = 30.0;
            System.err.println("No -close=# cutoff for approaching target "+
                "was provided => using "+df2.format(closeCutoff)+" degrees");
        }
        
        try
        {
            PdbReader reader = new PdbReader();
            CoordinateFile cf = null;
            if (filename != null)
            {
                File f = new File(filename);
                cf = reader.read(f);
            }
            else
            {
                if (alphaOrBeta.equals("alpha"))
                    cf = reader.read(this.getClass().getResourceAsStream("ideal-alpha.pdb"));
                else if (alphaOrBeta.equals("beta"))
                    cf = reader.read(this.getClass().getResourceAsStream("ideal-beta.pdb"));
            }
            
            Iterator models = cf.getModels().iterator(); models.hasNext();
            Model m = (Model) models.next();
            doBackrubs(m);
        }
        catch (IOException ioe) { System.err.println("Trouble parsing file!"); }
    }

    public static void main(String[] args)
    {
        BackrubRama mainprog = new BackrubRama();
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
            InputStream is = getClass().getResourceAsStream("BackrubRama.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'BackrubRama.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.BackrubRama");
        System.err.println("Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.");
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
        if (filename == null)   filename = arg;
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
        else if(flag.equals("-structurekin") || flag.equals("-struct"))
        {
            doStructKin = true;
        }
        else if(flag.equals("-res"))
        {
            try
            {
                resnum = Integer.parseInt(param);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Can't format "+param+" as an integer for resnum!");
            }
        }
        else if(flag.equals("-theta13") || flag.equals("-theta"))
        {
            try
            {
                maxTheta13 = Double.parseDouble(param);
            }
            catch (NumberFormatException nfe) 
            {
                System.err.println("Can't format "+param+" as a double for theta13!");  
            }
        }
        else if(flag.equals("-epsilon") || flag.equals("-e"))
        {
            try
            {
                epsilon = Double.parseDouble(param);
            }
            catch (NumberFormatException nfe) 
            {
                System.err.println("Can't format "+param+" as a double for epsilon!");  
            }
        }
        else if (flag.equals("-maxtaudev"))
        {
            try
            {
                maxTauDev = Double.parseDouble(param);
            }
            catch (NumberFormatException nfe) 
            {
                System.err.println("Can't format "+param+" as a double for maxTauDev!");  
            }
        }
        else if (flag.equals("-from"))
        {
            try
            {
                String[] parts = Strings.explode(param, ',', false, true);
                if (parts.length == 4 || parts.length == 6)
                {
                    initPhiPsi = new double[parts.length];
                    for (int i = 0; i < parts.length; i++)
                        initPhiPsi[i] = Double.parseDouble(parts[i]);
                }
                else System.err.println("-from=#,#,#,# should have exactly 4 "+
                    "(phi/psi i-1,i+1) or 6 (phi/psi i-1,i,i+1) values");
            }
            catch (NumberFormatException nfe) 
            {
                System.err.println("Can't format "+param+" as doubles for initPhiPsi!");  
            }
        }
        else if (flag.equals("-to"))
        {
            try
            {
                String[] parts = Strings.explode(param, ',', false, true);
                if (parts.length == 4)
                {
                    finalPhiPsi = new double[4];
                    for (int i = 0; i < parts.length; i++)
                        finalPhiPsi[i] = Double.parseDouble(parts[i]);
                }
                else System.err.println(
                    "-to=#,#,#,# should have exactly 4 values (phi/psi i-1,i+1)");
            }
            catch (NumberFormatException nfe) 
            {
                System.err.println("Can't format "+param+" as doubles for finalPhiPsi!");  
            }
        }
        else if (flag.equals("-alpha"))
        {
            alphaOrBeta = "alpha";
        }
        else if (flag.equals("-beta"))
        {
            alphaOrBeta = "beta";
        }
        else if (flag.equals("-close"))
        {
            try
            {
                closeCutoff = Double.parseDouble(param);
            }
            catch (NumberFormatException nfe) 
            {
                System.err.println("Can't format "+param+" as a double for closeCutoff!");  
            }
        }
        else if (flag.equals("-noclose"))
        {
            noCloseCutoff = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

