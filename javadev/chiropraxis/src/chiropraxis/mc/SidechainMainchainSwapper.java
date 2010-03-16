// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import chiropraxis.mc.SubImpose;
import molikin.logic.BallAndStickLogic;
//}}}
/**
* <code>SidechainMainchainSwapper</code> finds places where two otherwise 
* similar models deviate, the sidechain of one performing a similar structural 
* role as the mainchain of the other.
* 
* TODO: try all rotamers to get best possible scmc+mcsc rmsd
*       use SpatialBin somehow to make method more generic wrt aa type
*       separate out a "SidechainMainchainSwap" class -- goulash!
* 
* <p>Begun on Mon Mar  1 2010
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
*/
public class SidechainMainchainSwapper //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df  = new DecimalFormat("0.0###");
    DecimalFormat df2 = new DecimalFormat("0.0");
    double[] sieves = new double[] { 1.00, 0.95, 0.90, 0.85, 0.80, 0.75 };
    double mcRadius = Double.POSITIVE_INFINITY; //15;
    double scRadius = 5;
    static final String MC_PREV = "mainchain N-ward";
    static final String MC_NEXT = "mainchain C-ward";
    static final String SC      = "sidechain";
//}}}

//{{{ Variable definitions
//##############################################################################
    String structIn1 = null, structIn2 = null; // straight from cmdline
    String title1 = null, title2 = null; // prettied up
    BallAndStickLogic bsl = null;
    Model m1min = null; // globally accessible; min of swapped, not regular
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SidechainMainchainSwapper()
    {
        super();
    }
//}}}

//{{{ compareModels
//##############################################################################
    public void compareModels(Model m1, Model m2, Alignment align)
    {
        System.err.println(
            "mdl1\t"+(title1.length() > 7 ? "\t" : "")+
            "mdl2\t"+(title2.length() > 7 ? "\t" : "")+
            "rmsdglb\tres1\tres2\trmsdnrm\trmsdswp\tswapped");
        
        globalKinemage(m2, align, "peach", "sky");
        
        for(int i = 0; i < align.b.length; i++)
        {
            Residue r1 = (Residue) align.a[i];
            Residue r2 = (Residue) align.b[i];
            if(r1 != null && r2 != null)
            {
                if(r1.getName().equals("ARG")
                || r1.getName().equals("LYS") || r1.getName().equals("MET")
                || r1.getName().equals("GLN") || r1.getName().equals("GLU")
                || r1.getName().equals("ASN") || r1.getName().equals("ASP"))
                {
                    double rmsd = compareResidues(r1, r2, m1, m2, align);
                    if(!Double.isNaN(rmsd))
                    {
                        // local kinemage using the global Calpha superposition that 
                        // produced the lowest local sidechain-mainchain-swapped rmsd, 
                        // which must also be less than the lowest non-swapped rmsd
                        localSwapKinemage(r1, m1min, "white", "cyan", rmsd);
                    }
                }
            }
        }//residue
    }
//}}}

//{{{ compareResidues
//##############################################################################
    public double compareResidues(Residue r1, Residue r2, Model m1, Model m2, Alignment align)
    {
        double rmsdGlbMin = Double.POSITIVE_INFINITY;
        double rmsdLocNormMin = Double.POSITIVE_INFINITY; // regular mc-mc, mc-mc, sc-sc pairing
        double rmsdLocSwapMin = Double.POSITIVE_INFINITY; // 1 of 2 possible swapped pairings
        m1min = null;
        
        try
        {
            for(double sieve : sieves)
            {
                Model m1new = (Model) m1.clone(); // retain original coordinates
                
                // global Calpha superposition
                double rmsdGlb = globalSup(m1new, m2, align, sieve);
                if(rmsdGlb < rmsdGlbMin) rmsdGlbMin = rmsdGlb;
                
                // local Calpha co-centering, then local rmsds of sc/mc in various combinations
                double[] rmsdLocs = localCoCenter(r1, r2, m1new, m2);
                double rmsdLocNorm = rmsdLocs[0];
                double rmsdLocSwap = Math.min(rmsdLocs[1], rmsdLocs[2]);
                if(rmsdLocNorm < rmsdLocNormMin) rmsdLocNormMin = rmsdLocNorm;
                if(rmsdLocSwap < rmsdLocSwapMin)
                {
                    rmsdLocSwapMin = rmsdLocSwap;
                    m1min = m1new;
                }
            }//sieve
        }
        catch(ParseException ex) {}
        //{ if(verbose) System.err.println("parsing error w/ global sup"); }
        catch(AtomException ex) {}
        //{ if(verbose) System.err.println("atom error w/ global sup OR local rmsd"); }
        
        boolean swapped = (rmsdLocSwapMin < rmsdLocNormMin);
        
        System.err.println(title1+"\t"+title2+"\t"+df.format(rmsdGlbMin)
            +"\t"+r1.nickname()+"\t"+r2.nickname()+"\t"+df.format(rmsdLocNormMin)
            +"\t"+df.format(rmsdLocSwapMin)+"\t"+(swapped ? "YES!" : "no"));
        
        if(swapped) return rmsdLocSwapMin;
        return Double.NaN;
    }
//}}}

//{{{ globalSup
//##############################################################################
    public double globalSup(Model m1, Model m2, Alignment align, double sieve) throws AtomException, ParseException
    {
        ModelState s1 = m1.getState();
        ModelState s2 = m2.getState(); 
        
        
        AtomState[][] atoms = SubImpose.getAtomsForSelection(
            m1.getResidues(), s1, m2.getResidues(), s2, "atom_CA_", null, align);
        if(atoms[0].length < 3) throw new IllegalArgumentException(
            "Can't superimpose on less than 3 atoms!");
        
        SuperPoser superpos = new SuperPoser(atoms[1], atoms[0]);
        Transform R = new Transform(); // identity, defaults to no superposition
        R = superpos.superpos();
        
        // Lesk sieve
        int lenAtomsUsed = atoms[0].length;
        int len = (int) Math.round( sieve * atoms[0].length );
        if(len < 3) System.err.println(
            "WARNING: too few atoms for Lesk's sieve at "+df.format(sieve));
        else
        {
            lenAtomsUsed = len;
            SubImpose.sortByLeskSieve(atoms[0], atoms[1]);
            superpos.reset(atoms[1], 0, atoms[0], 0, len); // only use the len best
            R = superpos.superpos();
        }
        
        // Transform model 1 so transformed coords will be used in the future.
        for(Iterator iter = Model.extractOrderedStatesByName(m1).iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            R.transform(as);
        }
        
        return superpos.calcRMSD(R);
    }
//}}}

//{{{ globalKinemage
//##############################################################################
    public void globalKinemage(Model m, Alignment align, String mcColor, String scColor)
    {
        System.out.println("@kinemage {"+title1+"."+title2+"}");
        System.out.println("@master {sc ?= mc}");
        System.out.println("@group {"+title2+"} dominant");
        Set residues = new TreeSet<Residue>();
        for(Iterator iter = m.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if(res != null) residues.add(res);
        }
        bsl.width = 3;
        bsl.scColor = scColor;
        bsl.printKinemage(new PrintWriter(System.out), m, residues, title2, mcColor);
    }
//}}}

//{{{ localSwapKinemage
//##############################################################################
    public void localSwapKinemage(Residue r, Model m, String mcColor, String scColor, double rmsd)
    {
        System.out.println("@group {"+r.nickname()+"  "+df2.format(rmsd)
            +"} dominant animate master= {"+title1+"} master= {"+r.nickname()+"}");
        bsl.scColor = scColor;
        bsl.width = -1; // i.e. use default width
        ModelState s = m.getState();
        
        // mainchain
        Set residues = new TreeSet<Residue>();
        for(Iterator iter = m.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            try
            { if(res != null && residuesAreClose(res, r, m, s, mcRadius, false))  residues.add(res); }
            catch(AtomException ex)
            { ex.printStackTrace(); }
        }
        bsl.doSidechains = false;
        bsl.doMainchain  = true;
        bsl.printKinemage(new PrintWriter(System.out), m, residues, title1, mcColor);
        
        // sidechains
        residues = new TreeSet<Residue>();
        for(Iterator iter = m.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            try
            { if(res != null && residuesAreClose(res, r, m, s, scRadius, true))  residues.add(res); }
            catch(AtomException ex)
            { ex.printStackTrace(); }
        }
        bsl.doSidechains = true;
        bsl.doMainchain  = false;
        bsl.printKinemage(new PrintWriter(System.out), m, residues, title1, mcColor);
        
        // site of interest
        System.out.println("@balllist {sc ?= mc} radius= 0.55 color= green master= {sc ?= mc}");
        try
        {
            AtomState ca = s.get(r.getAtom(" CA "));
            System.out.println("{"+r+" sc ?= mc} "+ca.getX()+" "+ca.getY()+" "+ca.getZ());
        }
        catch(AtomException ex)
        { System.err.println("error drawing sc ?= mc ball for "+r); }
    }
//}}}

//{{{ residuesAreClose
//##############################################################################
    /**
    * Returns true if any atom in the first residue is within 
    * the specified distance of any atom in the second residue.
    * May be restricted to distances between sidechain atoms.
    */
    public boolean residuesAreClose(Residue r1, Residue r2, Model m, ModelState s, double distance, boolean onlySidechains) throws AtomException
    {
        for(Iterator iter1 = r1.getAtoms().iterator(); iter1.hasNext(); )
        {
            AtomState a1 = s.get( (Atom) iter1.next() );
            if(onlySidechains && isMainchain(a1.getName())) continue;
            for(Iterator iter2 = r2.getAtoms().iterator(); iter2.hasNext(); )
            {
                AtomState a2 = s.get( (Atom) iter2.next() );
                if(onlySidechains && isMainchain(a2.getName())) continue;
                if(a1.distance(a2) < distance) return true;
            }
        }
        return false;
    }

    private boolean isMainchain(String atomName)
    {
        if(atomName.equals(" N  ") || atomName.equals(" CA ")
        || atomName.equals(" C  ") || atomName.equals(" O  ")) return true;
        return false;
    }
//}}}

//{{{ localCoCenter
//##############################################################################
    /**
    * There are 6 possible rmsd calculations at a Calpha junction due to its 3 
    * outgoing units: mc(i-1), mc(i+1), sc.
    * However, 3 of these pair mainchain units going in opposite directions,
    * which implies bad enough global superposition that a sidechain-mainchain
    * swap might not be meaningful, so here we return only the remaining 3:<ul>
    * <li>mc(i-1)_mc(i-1)  mc(i+1)_mc(i+1)  sc_sc</li>
    * <li>mc(i-1)_mc(i-1)  mc(i+1)_sc       sc_mc(i+1)</li>
    * <li>mc(i-1)_sc       mc(i+1)_mc(i+1)  sc_mc(i-1)</li>
    * where x_y means atoms x from the first (mobile) residue will be compared 
    * to atoms y from the second (reference) residue.
    */
    public double[] localCoCenter(Residue r1, Residue r2, Model m1, Model m2) throws AtomException
    {
        // translate mobile CA (1) to reference CA (2) ("co-center")
        ModelState s1 = m1.getState();
        ModelState s2 = m2.getState(); 
        AtomState ca1 = s1.get(r1.getAtom(" CA "));
        AtomState ca2 = s2.get(r2.getAtom(" CA "));
        Triple xlate = new Triple(ca2).sub(ca1);
        for(Iterator iter = Model.extractOrderedStatesByName(m1).iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            as.add(xlate);
        }
        
        // local rmsds
        double[] rmsds = new double[] { 0, 0, 0 };
        
        // normal:  mc(i-1)_mc(i-1)  mc(i+1)_mc(i+1)  sc_sc
        rmsds[0] += calcLocalRmsd(r1, r2, m1, m2, MC_PREV, MC_PREV);
        rmsds[0] += calcLocalRmsd(r1, r2, m1, m2, MC_NEXT, MC_NEXT);
        //rmsds[0] += calcLocalRmsd(r1, r2, m1, m2, SC     , SC     );
        //rmsds[0] /= 3.0;
        rmsds[0] /= 2.0;
        
        // entering a sc-mc swap "bubble": mc(i-1)_mc(i-1)  mc(i+1)_sc  sc_mc(i+1)
        rmsds[1] += calcLocalRmsd(r1, r2, m1, m2, MC_PREV, MC_PREV);
        rmsds[1] += calcLocalRmsd(r1, r2, m1, m2, MC_NEXT, SC     );
        rmsds[1] += calcLocalRmsd(r1, r2, m1, m2, SC     , MC_NEXT);
        rmsds[1] /= 3.0;
        
        // exiting a sc-mc swap "bubble": mc(i-1)_sc mc(i+1)_mc(i+1)  sc_mc(i-1)
        rmsds[2] += calcLocalRmsd(r1, r2, m1, m2, MC_PREV, SC     );
        rmsds[2] += calcLocalRmsd(r1, r2, m1, m2, MC_NEXT, MC_NEXT);
        rmsds[2] += calcLocalRmsd(r1, r2, m1, m2, SC     , MC_PREV);
        rmsds[2] /= 3.0;
        
        return rmsds;
    }
//}}}

//{{{ calcLocalRmsd
//##############################################################################
    public double calcLocalRmsd(Residue r1, Residue r2, Model m1, Model m2, String sel1, String sel2) throws AtomException
    {
        // get correct atoms
        Triple[] atoms1 = null, atoms2 = null, atoms1flip = null, atoms2flip = null;
        atoms1 = getLocalAtoms(r1, m1, sel1, false);
        atoms2 = getLocalAtoms(r2, m2, sel2, false);
        if(sel1.equals(SC) && isFlippable(r1)) atoms1flip = getLocalAtoms(r1, m1, sel1, true);
        if(sel2.equals(SC) && isFlippable(r2)) atoms2flip = getLocalAtoms(r2, m2, sel2, true);
        if(atoms1 == null || atoms2 == null)
        {
            System.err.println("not enough atoms for co-center then rmsd of "+r1+" onto "+r2);
            return Double.NaN;
        }
        
        // rmsds w/o any relative rotation; flip (F) or no flip (n)
        double rmsd_nn = Double.POSITIVE_INFINITY;
        double rmsd_Fn = Double.POSITIVE_INFINITY;
        double rmsd_nF = Double.POSITIVE_INFINITY;
        double rmsd_FF = Double.POSITIVE_INFINITY;
        rmsd_nn = rmsd(atoms1, atoms2);
        if(atoms1flip != null) rmsd_Fn = rmsd(atoms1flip, atoms2);
        if(atoms2flip != null) rmsd_nF = rmsd(atoms1, atoms2flip);
        if(atoms1flip != null
        && atoms2flip != null) rmsd_FF = rmsd(atoms1flip, atoms2flip);
        return Math.min(rmsd_nn, 
               Math.min(rmsd_Fn, 
               Math.min(rmsd_nF, rmsd_FF))); // exhale...
    }

    private boolean isFlippable(Residue r)
    {
        if(r.getName().equals("ARG")
        || r.getName().equals("GLN")
        || r.getName().equals("GLU")
        || r.getName().equals("ASN")
        || r.getName().equals("ASP"))
            return true;
        return false;
    }

    private double rmsd(Triple[] atoms1, Triple[] atoms2)
    {
        if(atoms1.length != atoms2.length) return Double.NaN;
        double rmsd = 0;
        for(int i = 0; i < atoms2.length; i++)
            rmsd += Triple.sqDistance(atoms2[i], atoms1[i]);
        rmsd = Math.sqrt(rmsd);
        return rmsd;
    }
//}}}

//{{{ getLocalAtoms
//##############################################################################
    public Triple[] getLocalAtoms(Residue r0, Model m, String sel, boolean flip) throws AtomException
    {
        ModelState s = m.getState();
        Triple[] atoms = null;
        
        //{{{ sc
        if(sel.equals(SC))
        {
            if(r0.getName().equals("ARG")) atoms = new Triple[] {
                new Triple(s.get(r0.getAtom(" CA "))),
                new Triple(s.get(r0.getAtom(" CB "))),
                new Triple(s.get(r0.getAtom(" CG "))),
                new Triple(s.get(r0.getAtom(" CD "))),
                new Triple(s.get(r0.getAtom(" NE "))),
                new Triple(s.get(r0.getAtom(" CZ "))),
                new Triple(s.get(r0.getAtom(" NH1"))),
                new Triple(s.get(r0.getAtom(" NH2"))) };
            else if(r0.getName().equals("LYS")) atoms = new Triple[] {
                new Triple(s.get(r0.getAtom(" CA "))),
                new Triple(s.get(r0.getAtom(" CB "))),
                new Triple(s.get(r0.getAtom(" CG "))),
                new Triple(s.get(r0.getAtom(" CD "))),
                new Triple(s.get(r0.getAtom(" CE "))),
                new Triple(s.get(r0.getAtom(" NZ "))) };
            else if(r0.getName().equals("MET")) atoms = new Triple[] {
                new Triple(s.get(r0.getAtom(" CA "))),
                new Triple(s.get(r0.getAtom(" CB "))),
                new Triple(s.get(r0.getAtom(" CG "))),
                new Triple(s.get(r0.getAtom(" SD "))),
                new Triple(s.get(r0.getAtom(" CE "))) };
            else if(r0.getName().equals("GLN")) atoms = new Triple[] {
                new Triple(s.get(r0.getAtom(" CA "))),
                new Triple(s.get(r0.getAtom(" CB "))),
                new Triple(s.get(r0.getAtom(" CG "))),
                new Triple(s.get(r0.getAtom(" CD "))),
                new Triple(s.get(r0.getAtom(" OE1"))),
                new Triple(s.get(r0.getAtom(" NE2"))) };
            else if(r0.getName().equals("GLU")) atoms = new Triple[] {
                new Triple(s.get(r0.getAtom(" CA "))),
                new Triple(s.get(r0.getAtom(" CB "))),
                new Triple(s.get(r0.getAtom(" CG "))),
                new Triple(s.get(r0.getAtom(" CD "))),
                new Triple(s.get(r0.getAtom(" OE1"))),
                new Triple(s.get(r0.getAtom(" OE2"))) };
            else if(r0.getName().equals("ASN")) atoms = new Triple[] {
                new Triple(s.get(r0.getAtom(" CA "))),
                new Triple(s.get(r0.getAtom(" CB "))),
                new Triple(s.get(r0.getAtom(" CG "))),
                new Triple(s.get(r0.getAtom(" OD1"))),
                new Triple(s.get(r0.getAtom(" ND2"))) };
            else if(r0.getName().equals("ASP")) atoms = new Triple[] {
                new Triple(s.get(r0.getAtom(" CA "))),
                new Triple(s.get(r0.getAtom(" CB "))),
                new Triple(s.get(r0.getAtom(" CG "))),
                new Triple(s.get(r0.getAtom(" OD1"))),
                new Triple(s.get(r0.getAtom(" OD2"))) };
            
            if(flip)
            {
                Triple[] atomsFlip = new Triple[atoms.length];
                for(int i = 0; i < atoms.length-2; i++) atomsFlip[i] = atoms[i];
                atomsFlip[atoms.length-2] = atoms[atoms.length-1]; // "flip" end
                atomsFlip[atoms.length-1] = atoms[atoms.length-2]; // of sidechain
                atoms = atomsFlip;
            }
        }
        //}}}
        //{{{ mc-
        else if(sel.equals(MC_PREV))
        {
            Residue r1 = null, r2 = null;
            r1 = r0.getPrev(m);
            if(r1 == null) return null;
            if(r0.getName().equals("ARG") || r0.getName().equals("LYS"))
            {
                r2 = r1.getPrev(m);
                if(r2 == null) return null;
            }
            if(r0.getName().equals("ARG"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    new Triple(s.get(r2.getAtom(" C  "))),
                    new Triple(s.get(r2.getAtom(" O  "))),
                    new Triple(s.get(r2.getAtom(" CA "))) };
            }
            else if(r0.getName().equals("LYS"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    new Triple(s.get(r2.getAtom(" C  "))) };
            }
            else if(r0.getName().equals("MET"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" N  "))) };
            }
            else if(r0.getName().equals("GLN") || r0.getName().equals("GLU"))
            {
                // branch point is Calpha, so compromise on a superposition point
                Triple midCbetaHalpha = new Triple().likeMidpoint(
                    new Triple(s.get(r1.getAtom(r1.getName().equals("GLY") ? " HA3" : " CB "))),
                    new Triple(s.get(r1.getAtom(r1.getName().equals("GLY") ? " HA2" : " HA "))));
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    midCbetaHalpha };
            }
            else if(r0.getName().equals("ASN") || r0.getName().equals("ASP"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" O  "))),
                    new Triple(s.get(r1.getAtom(" CA "))) };
            }
        }
        //}}}
        //{{{ mc+
        else if(sel.equals(MC_NEXT))
        {
            Residue r1 = null, r2 = null;
            r1 = r0.getNext(m);
            if(r1 == null) return null;
            if(r0.getName().equals("ARG") || r0.getName().equals("LYS"))
            {
                r2 = r1.getNext(m);
                if(r2 == null) return null;
            }
            if(r0.getName().equals("ARG"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    new Triple(s.get(r2.getAtom(" N  "))),
                    new Triple(s.get(r2.getAtom(r1.getName().equals("PRO") ? " CD " : " H  "))),
                    new Triple(s.get(r2.getAtom(" CA "))) };
            }
            else if(r0.getName().equals("LYS"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    new Triple(s.get(r2.getAtom(" N  "))) };
            }
            else if(r0.getName().equals("MET"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" C  "))) };
            }
            else if(r0.getName().equals("GLN") || r0.getName().equals("GLU"))
            {
                // branch point is Calpha, so compromise on a superposition point
                Triple midCbetaHalpha = new Triple().likeMidpoint(
                    new Triple(s.get(r1.getAtom(r1.getName().equals("GLY") ? " HA3" : " CB "))),
                    new Triple(s.get(r1.getAtom(r1.getName().equals("GLY") ? " HA2" : " HA "))));
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(" CA "))),
                    new Triple(s.get(r1.getAtom(" C  "))),
                    midCbetaHalpha };
            }
            else if(r0.getName().equals("ASN") || r0.getName().equals("ASP"))
            {
                atoms = new Triple[] {
                    new Triple(s.get(r0.getAtom(" CA "))),
                    new Triple(s.get(r0.getAtom(" C  "))),
                    new Triple(s.get(r1.getAtom(" N  "))),
                    new Triple(s.get(r1.getAtom(r1.getName().equals("PRO") ? " CD " : " H  "))),
                    new Triple(s.get(r1.getAtom(" CA "))) };
            }
        }
        //}}}
        //{{{ else
        else
        {
            System.err.println("unrecognized atom selection: "+sel);
            return null;
        }
        //}}}
        
        return atoms;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(structIn1 == null || structIn2 == null)
            throw new IllegalArgumentException("must provide two structures");
        PdbReader pdbReader = new PdbReader();
        File file1 = new File(structIn1);
        File file2 = new File(structIn2);
        title1 = file1.getName().replace(".pdb", "");
        title2 = file2.getName().replace(".pdb", "");
        title1 = title1.substring(0, Math.min(title1.length(), 10));
        title2 = title2.substring(0, Math.min(title2.length(), 10));
        CoordinateFile coord1 = pdbReader.read(file1);
        CoordinateFile coord2 = pdbReader.read(file2);
        Model m1 = coord1.getFirstModel();
        Model m2 = coord2.getFirstModel();
        m1.getState().setName(title1);
        m2.getState().setName(title2);
        Collection chains1 = SubImpose.getChains(m1);
        Collection chains2 = SubImpose.getChains(m2);
        Alignment align = Alignment.alignChains(chains1, chains2, 
            new Alignment.NeedlemanWunsch(), new SubImpose.SimpleNonWaterResAligner());
        
        bsl = new BallAndStickLogic();
        bsl.doProtein    = true;
        bsl.doMainchain  = true;
        bsl.doSidechains = true;
        bsl.doHydrogens  = true;
        
        compareModels(m1, m2, align);
    }
    
    public static void main(String[] args)
    {
        SidechainMainchainSwapper mainprog = new SidechainMainchainSwapper();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(Exception ex)
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
            InputStream is = getClass().getResourceAsStream("SidechainMainchainSwapper.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SidechainMainchainSwapper.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.SidechainMainchainSwapper");
        System.err.println("Copyright (C) 2010 by Daniel Keedy. All rights reserved.");
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
        if(structIn1 == null)       structIn1 = arg;
        else if(structIn2 == null)  structIn2 = arg;
        else throw new IllegalArgumentException("too many arguments!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class
