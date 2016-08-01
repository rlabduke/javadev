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
//}}}
/**
* <code>VariableRegions</code> searches through the alternate conformation loops
* of a crystal structure, outputting the Ca-Ca distance and phi,psi for each 
* residue between the hinge points.
*
* It can also do the same for two provided PDB files, e.g. from the Donald lab's
* BD backbone-DEE protein design algorithm (the '-hinges' option is on by default
* in this mode).
*
* Alternative output modes: kinemage (default) or text/csv
*
* <p>Copyright (C) 2007 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Mon Feb. 11, 2007.
*/
public class VariableRegions //extends ... implements ...
{
//{{{ Constants
    PrintStream   out = System.out;
    DecimalFormat df  = new DecimalFormat("#.###");
    DecimalFormat df2 = new DecimalFormat("#.#");
    DecimalFormat df3 = new DecimalFormat("###.#");
//}}}

//{{{ Variable definitions
//##############################################################################
    String        filename1     = null;
    String        filename2     = null;
    String        label1        = null;
    String        label2        = null;
    String        delim         = ",";
    boolean       verbose       = false;
    boolean       doKin         = true;
    boolean       absVal        = false;
    boolean       allRes        = false;
    boolean       hinges        = true;
    double        dCaMin        = 0.1;
    double        dCaScale      = Double.NaN; // for text and kin
    double        dPhiPsiScale  = Double.NaN; // for kin only
    double[]      maxAbsMvmts      = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public VariableRegions()
    {
        super();
    }
//}}}

//{{{ CLASS: SimpleResAligner
//##############################################################################
    static class SimpleResAligner implements Alignment.Scorer
    {
        // High is good, low is bad.
        public double score(Object a, Object b)
        {
            Residue r = (Residue) a;
            Residue s = (Residue) b;
            if(r == null || s == null)
                return -1;  // gap
            else if(r.getName().equals(s.getName()))
                return 2;   // match
            else
                return 0;   // mismatch
        }
        
        public double open_gap(Object a) { return extend_gap(a); }
        public double extend_gap(Object a) { return score(a, null); }
    }
//}}}

//{{{ searchOneModel
//##############################################################################
    /** 
    * For evaluating variability in alternate conformation regions within a 
    * single PDB file. 
    */
    void searchOneModel(Model model)
    {
        if(verbose) System.err.println("Looking for variable regions in "+filename1);
        
        ModelState state1 = model.getState("A");
        ModelState state2 = model.getState("B");
        if(state2 != null)
        {
            // Get residues that move
            TreeMap<Residue, double[]> moved = new TreeMap<Residue, double[]>();
            TreeMap<Residue, Triple>   ca2s  = new TreeMap<Residue, Triple>();
            for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
            {
                Residue res = (Residue) iter.next();
                if(res != null)
                {
                    Residue prev = res.getPrev(model);
                    Residue next = res.getNext(model);
                    try
                    {
                        Atom ca = res.getAtom(" CA ");
                        AtomState ca1 = state1.get(ca);
                        AtomState ca2 = state2.get(ca);
                        double caTravel = Triple.distance(ca1, ca2);
                        double dPhi = Double.NaN, dPsi = Double.NaN;
                        if(prev != null && next != null)
                        {
                            double phi1 = calcPhi(prev, res, state1);
                            double phi2 = calcPhi(prev, res, state2);
                            double psi1 = calcPsi(res, next, state1);
                            double psi2 = calcPsi(res, next, state2);
                            dPhi = angleDiff(phi1, phi2);
                            dPsi = angleDiff(psi1, psi2);
                        }
                        if(hinges || (!Double.isNaN(dPhi) && dPhi != 0) || (!Double.isNaN(dPsi) && dPsi != 0) || caTravel != 0)
                        {
                            // This residue moved, or we ultimately want just "hinge" residues and will 
                            // excise the ones in between in the next step
                            double[] movements = new double[3];
                            movements[0] = dPhi; movements[1] = dPsi; movements[2] = caTravel;
                            moved.put(res, movements);
                            ca2s.put(res, ca2);
                        }
                    }
                    catch (AtomException ae) { }
                }
            } //for each residue
            
            if(hinges)
            {
                // Changes the contents of the 'moved' Residue->movement mapping so that 
                // only Ca-Ca arrows will be drawn for residues in the midst of "hinged: loops & 
                // only dphi,psi fans will be drawn for residues on the ends of those loops.
                doHinges(model, moved);
            }
            
            printOneModelResults(model, state1, moved, ca2s);
        }
        else System.err.println("No altB ModelState for Model "+model+"...");
    }
//}}}

//{{{ printOneModelResults
//##############################################################################
    void printOneModelResults(Model model, ModelState state1, TreeMap<Residue,double[]> moved, TreeMap<Residue,Triple> ca2s)
    {
        // Find maxima of d(phi), d(psi) and d(Ca) for normalization purposes
        maxAbsMvmts = new double[3];
        maxAbsMvmts[0] = 0;
        maxAbsMvmts[1] = 0;
        maxAbsMvmts[2] = 0;
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if(res != null && moves(res, moved))
            {
                // Something moved
                double[] mvmts = moved.get(res);
                if(Math.abs(mvmts[0]) > maxAbsMvmts[0])  maxAbsMvmts[0] = Math.abs(mvmts[0]); // d(phi)
                if(Math.abs(mvmts[1]) > maxAbsMvmts[1])  maxAbsMvmts[1] = Math.abs(mvmts[1]); // d(psi)
                if(Math.abs(mvmts[2]) > maxAbsMvmts[2])  maxAbsMvmts[2] = Math.abs(mvmts[2]); // d(Ca)
            }
        }
        if(verbose)
        {
            System.err.println("Max d(phi): "+df2.format(maxAbsMvmts[0]));
            System.err.println("Max d(phi): "+df2.format(maxAbsMvmts[1]));
            System.err.println("Max d(Ca) : "+df2.format(maxAbsMvmts[2]));
        }
        
        if(doKin) out.println("@group {var-reg} dominant");
        else       out.println("label:model:chain:res_type:res_num:dPhi:dPsi:dCa");
        
        // Output (kin or text) for residues that move
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if(verbose) System.err.println("seeing if "+res+" moved...");
            if(res != null && moves(res, moved))
            {
                // Something moved
                double[] mvmts = moved.get(res);
                double dPhi     = mvmts[0];
                double dPsi     = mvmts[1];
                double caTravel = mvmts[2];
                if(doKin)
                {
                    if(dPhi == Double.POSITIVE_INFINITY && dPsi == Double.POSITIVE_INFINITY)
                    {
                        // Draw arrows, not phi/psi fans 
                        doKinForRes(res, state1, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, new Triple(ca2s.get(res)));
                    }
                    else if(caTravel == Double.POSITIVE_INFINITY) 
                    {
                        // Draw phi/psi fans, not arrows
                        doKinForRes(res, state1, dPhi, dPsi, null);
                    }
                    else
                    {
                        // (default)
                        doKinForRes(res, state1, dPhi, dPsi, new Triple(ca2s.get(res)));
                    }
                }
                else if( allRes || (!Double.isNaN(dPhi) && !Double.isNaN(dPsi) 
                && (dPhi != 0 || dPsi != 0 || caTravel != 0)) )
                {
                    // Either something changed and is therefore worth printing 
                    // or we want to print stats for all residues regardless of 
                    // whether anything changed.
                    out.print(label1+delim+model+delim+res.getChain()+delim+
                        res.getName()+delim+res.getSequenceInteger()+delim);
                    if(!Double.isNaN(dPhi))     out.print(df.format(dPhi)+delim);
                    else                         out.print("__?__"+delim);
                    if(!Double.isNaN(dPsi))     out.print(df.format(dPsi)+delim);
                    else                         out.print("__?__"+delim);
                    if(!Double.isNaN(caTravel)) out.println(df.format(caTravel));
                    else                         out.print("__?__");
                }
            }
        } //for each residue
    }
//}}}

//{{{ searchTwoModels
//##############################################################################
    /** 
    * For evaluating variability in corresponding regions between two related 
    * PDB files, e.g. from the flexible backbone DEE ("BD") algorithm. 
    */
    void searchTwoModels(Model model1, Model model2)
    {
        if(verbose) System.err.println("Looking for variable regions between "+filename1+" and "+filename2+"...");
        
        // Align residues by sequence
        // For now we just take all residues as they appear in the file, without regard to chain IDs, etc.
        Alignment align = Alignment.needlemanWunsch(model1.getResidues().toArray(), model2.getResidues().toArray(), new SimpleResAligner());
        if(verbose)
        {
            System.err.println("Residue alignments:");
            for(int i = 0; i < align.a.length; i++)
                System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            System.err.println();
        }
        
        ModelState state1 = model1.getState();
        ModelState state2 = model2.getState();
        // Get residues that move
        TreeMap<Residue, double[]> moved = new TreeMap<Residue, double[]>();
        TreeMap<Residue, Triple>   ca2s  = new TreeMap<Residue, Triple>();
        for(int i = 0, len = align.a.length; i < len; i++)
        {
            if(align.a[i] == null || align.b[i] == null) continue;
            Residue res1 = (Residue) align.a[i];
            Residue res2 = (Residue) align.b[i];
            if(!res1.getName().equals(res2.getName())) continue; // sequence mismatch
            if(verbose) System.err.println("Comparing "+res1+" to "+res2+"...");
            
            Residue prev1 = res1.getPrev(model1);
            Residue next1 = res1.getNext(model1);
            Residue prev2 = res2.getPrev(model2);
            Residue next2 = res2.getNext(model2);
            
            try
            {
                Atom calpha1 = res1.getAtom(" CA ");
                Atom calpha2 = res2.getAtom(" CA ");
                AtomState ca1 = state1.get(calpha1);
                AtomState ca2 = state2.get(calpha2);
                double caTravel = Triple.distance(ca1, ca2);
                if(verbose)  System.err.println("Dist ("+ca1.getX()+","+ca1.getY()+","+ca1.getZ()
                    +") to ("+ca2.getX()+","+ca2.getY()+","+ca2.getZ()+") = "+caTravel+" (caTravel)");
                
                double phi1 = Double.NaN, psi1 = Double.NaN;
                if(prev1 != null && next1 != null)
                {
                    phi1 = calcPhi(prev1, res1, state1);
                    psi1 = calcPsi(res1, next1, state1);
                }
                double phi2 = Double.NaN, psi2 = Double.NaN;
                if(prev2 != null && next2 != null)
                {
                    phi2 = calcPhi(prev2, res2, state2);
                    psi2 = calcPsi(res2, next2, state2);
                }
                double dPhi = Double.NaN, dPsi = Double.NaN;
                if(!Double.isNaN(phi1) && !Double.isNaN(psi1)
                && !Double.isNaN(phi2) && !Double.isNaN(psi2))
                {
                    dPhi = angleDiff(phi1, phi2);
                    dPsi = angleDiff(psi1, psi2);
                }
                if(hinges || !Double.isNaN(dPhi) || !Double.isNaN(dPsi) || caTravel != 0)
                {
                    // This residue moved, or we ultimately want just "hinge" residues 
                    // and will excise the ones in between in the next step.
                    double[] movements = new double[3];
                    movements[0] = dPhi; movements[1] = dPsi; movements[2] = caTravel;
                    moved.put(res1, movements);
                    ca2s.put(res1, ca2);
                }
            }
            catch (AtomException ae) { }
        } //for each residue pair in alignment
        
        if(hinges)
        {
            // Changes the contents of the 'moved' Residue->movement mapping so that 
            // only Ca-Ca arrows will be drawn for residues in the midst of "hinged: loops & 
            // only dphi,psi fans will be drawn for residues on the ends of those loops.
            doHinges(model1, moved);
        }
        
        printTwoModelsResults(model1, model2, state1, moved, ca2s, align);
    }
//}}}

//{{{ printTwoModelsResults
//##############################################################################
    void printTwoModelsResults(Model model1, Model model2, ModelState state1, TreeMap<Residue,double[]> moved, TreeMap<Residue,Triple> ca2s, Alignment align)
    {
        // Find maxima of d(phi), d(psi) and d(Ca) for normalization purposes
        maxAbsMvmts = new double[3];
        maxAbsMvmts[0] = 0;
        maxAbsMvmts[1] = 0;
        maxAbsMvmts[2] = 0;
        for(int i = 0, len = align.a.length; i < len; i++)
        {
            if(align.a[i] == null || align.b[i] == null) continue;
            Residue res1 = (Residue) align.a[i];
            Residue res2 = (Residue) align.b[i];
            if(res1 != null && moves(res1, moved)) // DO want moves method here, not caMovesEnough (that was determined earlier)
            {
                // Something moved ENOUGH
                double[] mvmts = moved.get(res1);
                if(Math.abs(mvmts[0]) > maxAbsMvmts[0])  maxAbsMvmts[0] = Math.abs(mvmts[0]); // d(phi)
                if(Math.abs(mvmts[1]) > maxAbsMvmts[1])  maxAbsMvmts[1] = Math.abs(mvmts[1]); // d(psi)
                if(Math.abs(mvmts[2]) > maxAbsMvmts[2])  maxAbsMvmts[2] = Math.abs(mvmts[2]); // d(Ca)
            }
        }
        if(verbose)
        {
            System.err.println("Max d(phi): "+df2.format(maxAbsMvmts[0]));
            System.err.println("Max d(phi): "+df2.format(maxAbsMvmts[1]));
            System.err.println("Max d(Ca) : "+df2.format(maxAbsMvmts[2]));
        }
        
        if(doKin) out.println("@group {var-reg} dominant");
        else       out.println("label1:label2:model1:model2:chain1:chain2:res_type1:res_type2:res_num1:res_num2:dPhi:dPsi:dCa");
        
        // Output (kin or text) for residues that move
        for(int i = 0, len = align.a.length; i < len; i++)
        {
            if(align.a[i] == null || align.b[i] == null) continue;
            Residue res1 = (Residue) align.a[i];
            Residue res2 = (Residue) align.b[i];
            if(res1 != null && moves(res1, moved)) // DO want moves method here, not caMovesEnough (that was determined earlier)
            {
                // Something moved ENOUGH
                double[] mvmts = moved.get(res1);
                double dPhi     = mvmts[0];
                double dPsi     = mvmts[1];
                double caTravel = mvmts[2];
                if(doKin)
                {
                    if(dPhi == Double.POSITIVE_INFINITY && dPsi == Double.POSITIVE_INFINITY)
                    {
                        // Draw arrows, not phi/psi fans 
                        doKinForRes(res1, state1, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, new Triple(ca2s.get(res1)));
                    }
                    else if(caTravel == Double.POSITIVE_INFINITY) 
                    {
                        // Draw phi/psi fans, not arrows
                        doKinForRes(res1, state1, dPhi, dPsi, null);
                    }
                    else
                    {
                        // (default)
                        doKinForRes(res1, state1, dPhi, dPsi, new Triple(ca2s.get(res1)));
                    }
                }
                else if( allRes || (!Double.isNaN(dPhi) && !Double.isNaN(dPsi) 
                && (dPhi != 0 || dPsi != 0 || caTravel != 0)) )
                {
                    // Either something changed and is therefore worth printing 
                    // or we want to print stats for all residues regardless of 
                    // whether anything changed.
                    out.print(label1+delim+label2+delim+model1+delim+model2+delim
                        +res1.getChain()+delim+res2.getChain()+delim
                        +res1.getName()+delim+res2.getName()+delim
                        +res1.getSequenceInteger()+delim+res2.getSequenceInteger()+delim);
                    if(!Double.isNaN(dPhi))     out.print(df.format(dPhi)+delim);
                    else                         out.print("__?__"+delim);
                    if(!Double.isNaN(dPsi))     out.print(df.format(dPsi)+delim);
                    else                         out.print("__?__"+delim);
                    if(!Double.isNaN(caTravel)) out.println(df.format(caTravel));
                    else                         out.print("__?__");
                }
            }
        } //for each residue pair in alignment
    }
//}}}

//{{{ doHinges
//##############################################################################
    /** Alters linked residue-movement data so that only residues in the midst
    * of "hinged" regions of movement are considered to move. Everything else
    * outside of those regions is treated as static */
    TreeMap<Residue, double[]> doHinges(Model model, TreeMap<Residue, double[]> moved)
    {
        // Get first residue in model
        Residue firstRes = null;
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue testRes = (Residue) iter.next();
            if(firstRes == null)                                                  firstRes = testRes;
            else if(testRes.getSequenceInteger() < firstRes.getSequenceInteger()) firstRes = testRes;
        }
        if(verbose) System.err.println("Found first residue: "+firstRes);
        
        // Get stretches of residues that move; keep only ends ("hinge" residues)
        Residue res = firstRes;
        ArrayList<Residue> stretch = new ArrayList<Residue>();
        boolean endOfChain = false;
        while (!endOfChain)
        {
            if(res.getNext(model) != null)
            {
                Residue temp = res.getNext(model);   res = temp;
                
                // Decide whether to treat as move or not
                boolean treatAsMove = false;
                //if(!Double.isNaN(dCaMin) && caMovesEnough(res, moved))   treatAsMove = true;
                if(caMovesEnough(res, moved))   treatAsMove = true;
                else if(moves(res, moved))      treatAsMove = true;
                
                if(treatAsMove) stretch.add(res);
                else if(stretch.size() > 0)
                {
                    // End of stretch of residues that move (enough) => treat first and last as having 
                    // moved only phi/psi and residues in btw as having moved only Ca; reset stretch
                    if(verbose) System.err.println(
                        "Variable region from '"+stretch.get(0)+"' to '"+stretch.get(stretch.size()-1)+"'");
                    if(stretch.size() > 2)
                    {
                        double[] oldMvmts = moved.get(stretch.get(0));
                        double[] newMvmts = new double[3];
                        newMvmts[0] = oldMvmts[0];
                        newMvmts[1] = oldMvmts[1];
                        newMvmts[2] = Double.POSITIVE_INFINITY;
                        moved.put(stretch.get(0), newMvmts);
                        for(int i = 1; i < stretch.size()-1; i ++)
                        {
                            oldMvmts = moved.get(stretch.get(i));
                            newMvmts = new double[3];
                            newMvmts[0] = Double.POSITIVE_INFINITY;
                            newMvmts[1] = Double.POSITIVE_INFINITY;
                            newMvmts[2] = oldMvmts[2];
                            moved.put(stretch.get(i), newMvmts);
                        }
                        oldMvmts = moved.get(stretch.get(stretch.size()-1));
                        newMvmts = new double[3];
                        newMvmts[0] = oldMvmts[0];
                        newMvmts[1] = oldMvmts[1];
                        newMvmts[2] = Double.POSITIVE_INFINITY;
                        moved.put(stretch.get(stretch.size()-1), newMvmts);
                    }
                    stretch = new ArrayList<Residue>();
                }
                // else if nothing yet in this stretch (size() == 0) and res doesn't move, do nothing
            }
            else endOfChain = true;
        }
        // If have stretch leading up to end of chain, treat all but first as not having moved
        if(stretch.size() > 0)
            for(int i = 1; i < stretch.size(); i ++)
            {
                double[] newMvmts = new double[3];
                newMvmts[0] = 0;   newMvmts[1] = 0;   newMvmts[2] = 0;
                moved.put(stretch.get(i), newMvmts);
            }
        return moved;
    }
//}}}

//{{{ moves, caMovesEnough
//##############################################################################
    boolean moves(Residue res, TreeMap<Residue, double[]> moved)
    {
        DecimalFormat df = new DecimalFormat("#.#");
        try
        {
            double[] mvmts = moved.get(res);
            if(!Double.isNaN(mvmts[0]) && !Double.isNaN(mvmts[1]))
                if(mvmts[0] != 0 || mvmts[1] != 0 || mvmts[2] != 0)
                {
                    if(verbose) System.err.println("moves(): "+res+" mvmts = ("+
                        df.format(mvmts[0])+","+df.format(mvmts[1])+","+df.format(mvmts[2])+") => MOVES");
                    return true;
                }
            else
            {
                if(verbose) System.err.println("moves(): "+res+" mvmts = ("+
                    df.format(mvmts[0])+","+df.format(mvmts[1])+","+df.format(mvmts[2])+") ...");
                return false;
            }
        }
        catch (NullPointerException npe) { }
        return false;
    }

    boolean caMovesEnough(Residue res, TreeMap<Residue, double[]> moved)
    {
        DecimalFormat df = new DecimalFormat("#.###");
        try
        {
            double[] mvmts = moved.get(res);
            if(!Double.isNaN(mvmts[2]) && mvmts[2] > dCaMin)
            {
                if(verbose) System.err.println("caMovesEnough(): "+res+"\tCa-Ca = "+
                    df.format(mvmts[2])+" > "+df.format(dCaMin)+") => MOVES");
                return true;
            }
            else
            {
                if(verbose) System.err.println("caMovesEnough(): "+res+"\tCa-Ca = "+
                    df.format(mvmts[2])+" < "+df.format(dCaMin)+") ...");
                return false;
            }
        }
        catch (NullPointerException npe) { }
        return false;
    }
//}}}

//{{{ calcPhi, calcPsi, angleDiff
//##############################################################################
    double calcPhi(Residue prev, Residue res, ModelState state)
    {
        try
        {
            Atom prevC = prev.getAtom(" C  ");
            Atom n     = res.getAtom(" N  ");
            Atom ca    = res.getAtom(" CA ");
            Atom c     = res.getAtom(" C  ");
            
            AtomState prevCState = state.get(prevC);
            AtomState nState     = state.get(n);
            AtomState caState    = state.get(ca);
            AtomState cState     = state.get(c);
            
            return Triple.dihedral(prevCState, nState, cState, caState);
        }
        catch (AtomException ae) { return Double.NaN; }
    }

    double calcPsi(Residue res, Residue next, ModelState state)
    {
        try
        {
            Atom n     = res.getAtom(" N  ");
            Atom ca    = res.getAtom(" CA ");
            Atom c     = res.getAtom(" C  ");
            Atom nextN = next.getAtom(" N  ");
            
            AtomState nState     = state.get(n);
            AtomState caState    = state.get(ca);
            AtomState cState     = state.get(c);
            AtomState nextNState = state.get(nextN);
            
            return Triple.dihedral(nState, cState, caState, nextNState);
        }
        catch (AtomException ae) { return Double.NaN; }
    }

    /** Measures smallest difference between two angles, considering -180 to 180 
    * wrapping. */
    double angleDiff(double init, double fin)
    {
        double diffNoWrap = fin       - init;       // e.g.  170 - -170 = 340
        double diffWrap1  = (fin-360) - init;       // e.g. -190 - -170 = -20
        double diffWrap2  = fin       - (init-360); // e.g.  170 - -530 = 700
        double diffWrap3  = (fin-360) - (init-360); // e.g. -190 - -530 = 340
        
        double min = diffNoWrap;
        if(Math.abs(diffWrap1) < Math.abs(min))  min = diffWrap1;
        if(Math.abs(diffWrap2) < Math.abs(min))  min = diffWrap2;
        if(Math.abs(diffWrap3) < Math.abs(min))  min = diffWrap3;
        
        double diff = (absVal ? Math.abs(min) : min);
        return diff;
    }
//}}}

//{{{ doKinForRes
//##############################################################################
    void doKinForRes(Residue r1, ModelState s1, double dPhi, double dPsi)
    { doKinForRes(r1, s1, dPhi, dPsi, null); }

    void doKinForRes(Residue r1, ModelState s1, Triple ca2)
    { doKinForRes(r1, s1, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, ca2); }

    /** 
    * Uses vector "fans" for d(phi,psi).
    */
    void doKinForRes(Residue r1, ModelState s1, double dPhi, double dPsi, Triple ca2)
    {
        try
        {
            AtomState n1  = s1.get(r1.getAtom(" N  "));
            AtomState c1  = s1.get(r1.getAtom(" C  "));
            AtomState ca1 = s1.get(r1.getAtom(" CA "));
            
            // d(phi)
            if(!Double.isNaN(dPhi) && dPhi != 0 && dPhi != Double.POSITIVE_INFINITY)
            {
                Triple axis  = new Triple().likeVector(n1, ca1);
                for(int i = 0; i < 3; i ++)
                {
                    Transform rotate = new Transform();
                    double rotAngle = (dPhi / maxAbsMvmts[0]) * dPhiPsiScale;
                    // e.g. curr is 10 and max is 20 but scaling to 30 =>
                    // (10/20) = 0.5 => 0.5*30 = 15
                    // yields half of max possible fan size for these settings
                    rotAngle *= (0.5*i); // for "fading"/"fan" effect
                    rotate = rotate.likeRotation(axis, rotAngle);
                    // OLD: rotate = rotate.likeRotation(axis, (dPhi*dPhiPsiScale) * (1.00-0.33*i));
                    
                    Triple midpt = new Triple().likeMidpoint(n1, ca1);
                    Triple fan = new Triple().likeNormal(n1, ca1, c1);
                    rotate.transform(fan);
                    Triple fanAtMidpt = fan.mult(1.0).add(midpt);
                    
                    out.print("@vectorlist {"+r1+" d(phi)} master= {d(phi) norm'd->"+df2.format(dPhiPsiScale)+"} color= ");
                    out.print(dPhi > 0 ? "{red}" : "{blue}");
                    out.println(" width= "+(i+1));
                    out.println("{"+r1+" d(phi) = "+(dPhi>0?"+":"")+df3.format(dPhi)+" degrees "+label1+" => "+label2+"}P "
                        +df.format(midpt.getX())+" "
                        +df.format(midpt.getY())+" "
                        +df.format(midpt.getZ()));
                    out.println("{"+r1+" d(phi) = "+(dPhi>0?"+":"")+df3.format(dPhi)+" degrees "+label1+" => "+label2+"}  "
                        +df.format(fanAtMidpt.getX())+" "
                        +df.format(fanAtMidpt.getY())+" "
                        +df.format(fanAtMidpt.getZ()));
                }
            }
            
            // d(psi)
            if(!Double.isNaN(dPsi) && dPsi != 0 && dPsi != Double.POSITIVE_INFINITY)
            {
                Triple axis  = new Triple().likeVector(ca1, c1);
                for(int i = 0; i < 3; i ++)
                {
                    Transform rotate = new Transform();
                    double rotAngle = (dPsi / maxAbsMvmts[1]) * dPhiPsiScale;
                    rotAngle *= (0.5*i);
                    rotate = rotate.likeRotation(axis, rotAngle);
                    // OLD: rotate = rotate.likeRotation(axis, (dPsi*dPhiPsiScale) * (1.00-0.33*i));
                    
                    Triple midpt = new Triple().likeMidpoint(ca1, c1);
                    Triple fan = new Triple().likeNormal(c1, n1, ca1);
                    rotate.transform(fan);
                    Triple fanAtMidpt = fan.mult(1.0).add(midpt);
                    
                    out.print("@vectorlist {"+r1+" d(psi)} master= {d(psi) norm'd->"+df2.format(dPhiPsiScale)+"} color= ");
                    out.print(dPsi > 0 ? "{red}" : "{blue}");
                    out.println(" width= "+(i+1));
                    out.println("{"+r1+" d(psi) = "+(dPsi>0?"+":"")+df3.format(dPsi)+" degrees "+label1+" => "+label2+"}P "
                        +df.format(midpt.getX())+" "
                        +df.format(midpt.getY())+" "
                        +df.format(midpt.getZ()));
                    out.println("{"+r1+" d(psi) = "+(dPsi>0?"+":"")+df3.format(dPsi)+" degrees "+label1+" => "+label2+"}  "
                        +df.format(fanAtMidpt.getX())+" "
                        +df.format(fanAtMidpt.getY())+" "
                        +df.format(fanAtMidpt.getZ()));
                }
            }
            
            // d(Ca)
            if(ca2 != null)
            {
                double caTravel = Triple.distance(ca1, ca2);
                double scaledMag = (caTravel / maxAbsMvmts[2]) * dCaScale;
                Triple ca1ca2 = new Triple().likeVector(ca1, ca2).unit().mult(scaledMag);
                // OLD: Triple ca1ca2 = new Triple().likeVector(ca1, ca2).mult(dCaScale);
                
                if( !(ca1ca2.getX() == 0 && ca1ca2.getY() == 0 && ca1ca2.getZ() == 0) )
                {
                    Triple tip = new Triple().likeSum(ca1, ca1ca2);
                    
                    out.println("@arrowlist {"+r1+" d(Ca)} master= {d(Ca) norm'd->"+df2.format(dCaScale)+"} color= {green}");
                    out.println("{"+r1+" d(Ca) = "+df3.format(caTravel)+" Angstroms "+label1+" => "+label2+"}P "
                        +df.format(ca1.getX())+" "
                        +df.format(ca1.getY())+" "
                        +df.format(ca1.getZ()));
                    out.println("{"+r1+" d(Ca) = "+df3.format(caTravel)+" Angstroms "+label1+" => "+label2+"}  "
                        +df.format(tip.getX())+" "
                        +df.format(tip.getY())+" "
                        +df.format(tip.getZ()));
                }
            }
        }
        catch (AtomException ae) { System.err.println("Couldn't do kin for '"+r1+"'.."); }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if(filename1 == null && filename2 == null)
        {
            System.err.println("Need at least one filename!");
            System.exit(0);
        }
        
        // Parameters
        if(doKin)
        {
            if(!hinges)  dCaMin = 0.0;
            if(filename1 != null)
            {
                if(filename2 == null)
                {
                    if(Double.isNaN(dCaScale))      dCaScale     = 5;
                    if(Double.isNaN(dPhiPsiScale))  dPhiPsiScale = 1;
                }
                else //if(filename2 != null)
                {
                    if(Double.isNaN(dCaScale))      dCaScale     = 10;
                    if(Double.isNaN(dPhiPsiScale))  dPhiPsiScale = 500;
                }
            }
            
            System.err.println("Kin parameters:");
            System.err.println("d(phi,psi) scale: "+
                (Double.isNaN(dPhiPsiScale) ? "1 (default)" : df2.format(dPhiPsiScale)) );
            System.err.println("d(Ca)      scale: "+
                (Double.isNaN(dCaScale)     ? "1 (default)" : df2.format(dCaScale))     );
            System.err.println("d(Ca)      min  : "+df2.format(dCaMin));
        }
        
        // Main program
        try
        {
            // Looking for alt conf loops in one structure
            if(filename1 != null && filename2 == null)
            {
                PdbReader reader = new PdbReader();
                File f = new File(filename1);
                CoordinateFile cf = reader.read(f);
                for(Iterator models = cf.getModels().iterator(); models.hasNext(); )
                {
                    Model m = (Model) models.next();
                    label1 = f.toString()+" altA";
                    label2 = label1+" altB";
                    //if(cf.getIdCode() != null) label = cf.getIdCode();
                    searchOneModel(m);
                }
            }
            // Looking for regions that vary between two structures
            else if(filename1 != null && filename2 != null)
            {
                PdbReader reader = new PdbReader();
                File f1 = new File(filename1);
                File f2 = new File(filename2);
                CoordinateFile cf1 = reader.read(f1);
                CoordinateFile cf2 = reader.read(f2);
                Model m1 = cf1.getFirstModel();
                Model m2 = cf2.getFirstModel();
                label1 = f1.toString();
                label2 = f2.toString();
                searchTwoModels(m1, m2);
            }
        }
        catch (IOException ioe) { System.err.println("Trouble parsing files!"); }
    }

    public static void main(String[] args)
    {
        VariableRegions mainprog = new VariableRegions();
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
            InputStream is = getClass().getResourceAsStream("VariableRegions.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'VariableRegions.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.VariableRegions");
        System.err.println("Copyright (C) 2007 by Daniel A. Keedy. All rights reserved.");
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
        if(filename1 == null)         filename1 = arg;
        else if(filename2 == null)    filename2 = arg;
        else throw new IllegalArgumentException("Only need 1 or 2 files!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-kin"))
        {
            doKin = true;
        }
        else if(flag.equals("-nokin") || flag.equals("-csv"))
        {
            doKin = false;
        }
        else if(flag.equals("-delim"))
        {
            delim = param;
        }
        else if(flag.equals("-absval") || flag.equals("-abs"))
        {
            absVal = true;
        }
        else if(flag.equals("-allres") || flag.equals("-all"))
        {
            allRes = true;
        }
        else if(flag.equals("-hinges"))
        {
            hinges = true;
        }
        else if(flag.equals("-nohinges"))
        {
            hinges = false;
        }
        else if(flag.equals("-dcamin"))
        {
            try { dCaMin = Double.parseDouble(param); }
            catch (NumberFormatException nfe) { System.err.println("Can't parse "+param+" as a double!"); };
        }
        else if(flag.equals("-dcascale"))
        {
            try { dCaScale = Double.parseDouble(param); }
            catch (NumberFormatException nfe) { System.err.println("Can't parse "+param+" as a double!"); };
        }
        else if(flag.equals("-dphipsiscale"))
        {
            try { dPhiPsiScale = Double.parseDouble(param); }
            catch (NumberFormatException nfe) { System.err.println("Can't parse "+param+" as a double!"); };
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

