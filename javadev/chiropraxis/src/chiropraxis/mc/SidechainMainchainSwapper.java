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
* similar models may deviate, the sidechain of one performing a similar 
* structural role as the mainchain of the other.
* 
* TODO:   if "normal" scsc+mcmc rmsd < "swap" scmc+mcsc rmsd, don't even bother outputting (at least in kin)
*         try all rotamers to get best possible scmc+mcsc rmsd
*         use SpatialBin somehow - could make method more generic wrt aa type
*         ...
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
                || r1.getName().equals("GLN") || r1.getName().equals("GLU")
                || r1.getName().equals("ASN") || r1.getName().equals("ASP"))
                {
                    String rn1 = r1.getName().trim().toLowerCase()
                        +r1.getChain().replace(" ", "_")+r1.getSequenceNumber().trim();
                    String rn2 = r2.getName().trim().toLowerCase()
                        +r2.getChain().replace(" ", "_")+r2.getSequenceNumber().trim();
                    double rmsd = compareResidues(r1, r2, m1, m2, rn1, rn2, align);
                    if(!Double.isNaN(rmsd))
                    {
                        // local kinemage using the global Calpha superposition that 
                        // produced the lowest local sidechain-mainchain-swapped rmsd 
                        // (assuming that is less than the lowest non-swapped rmsd)
                        localKinemage(r1, m1min, "white", "cyan", rmsd, rn1);
                    }
                }
            }
        }//residue
    }
//}}}

//{{{ compareResidues
//##############################################################################
    public double compareResidues(Residue r1, Residue r2, Model m1, Model m2, String rn1, String rn2, Alignment align)
    {
        double rmsdGlbMin = Double.POSITIVE_INFINITY;
        double rmsdLocNormMin = Double.POSITIVE_INFINITY; // regular mc-mc, mc-mc, sc-sc pairing
        double rmsdLocSwapMin = Double.POSITIVE_INFINITY; // 1 of 2 possible swapped pairings
        m1min = null;
        
        for(double sieve : sieves)
        {
            try
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
            }
            catch(ParseException ex)
            { System.err.println("parsing error with global sup"); }
            catch(AtomException ex)
            { System.err.println("atom error with global sup, or local co-ctr or rmsd"); }
        }//sieve
        
        boolean swapped = (rmsdLocSwapMin < rmsdLocNormMin);
        
        System.err.println(title1+"\t"+title2+"\t"+df.format(rmsdGlbMin)
            +"\t"+rn1+"\t"+rn2+"\t"+df.format(rmsdLocNormMin)+"\t"
            +df.format(rmsdLocSwapMin)+"\t"+(swapped ? "YES!" : "no"));
        
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
    * where x_y means x atoms from the first (mobile) residue will be compared 
    * to y atoms from the second (reference) residue.
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
        
        // normal
        // mc(i-1)_mc(i-1)  mc(i+1)_mc(i+1)  sc_sc
        rmsds[0] += calcLocalRmsd(r1, r2, m1, m2, MC_PREV, MC_PREV);
        rmsds[0] += calcLocalRmsd(r1, r2, m1, m2, MC_NEXT, MC_NEXT);
        //rmsds[0] += calcLocalRmsd(r1, r2, m1, m2, SC     , SC     );
        //rmsds[0] /= 3.0;
        rmsds[0] /= 2.0;
        
        // entering a sc-mc swap "bubble"
        // mc(i-1)_mc(i-1)  mc(i+1)_sc       sc_mc(i+1)
        rmsds[1] += calcLocalRmsd(r1, r2, m1, m2, MC_PREV, MC_PREV);
        rmsds[1] += calcLocalRmsd(r1, r2, m1, m2, MC_NEXT, SC     );
        rmsds[1] += calcLocalRmsd(r1, r2, m1, m2, SC     , MC_NEXT);
        rmsds[1] /= 3.0;
        
        // exiting a sc-mc swap "bubble"
        // mc(i-1)_sc       mc(i+1)_mc(i+1)  sc_mc(i-1)
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
        // NB: "r1" and "r2" here are different from "r1" and "r2" in localCoCenter()
        
        // NB: I stopped caring about sc flips (e.g. NH1 vs. NH2 for Arg) - DAK 100307
        
        // get correct atoms
        AtomState[] atoms1 = getLocalAtoms(r1, m1, sel1);
        AtomState[] atoms2 = getLocalAtoms(r2, m2, sel2);
        if(atoms1 == null || atoms2 == null)
        {
            System.err.println("not enough atoms for co-ctr then rmsd of "+r1+" sc onto "+r2+" mc");
            return Double.NaN;
        }
        
        // rmsd w/o any relative rotation
        double rmsd = 0;
        for(int i = 0; i < atoms2.length; i++)
            rmsd += Triple.sqDistance(atoms2[i], atoms1[i]);
        rmsd = Math.sqrt(rmsd);
        return rmsd;
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

//{{{ localKinemage
//##############################################################################
    public void localKinemage(Residue r, Model m, String mcColor, String scColor, double rmsd, String rn)
    {
        System.out.println("@group {"+rn+"  "+df2.format(rmsd)
            +"} dominant animate master= {"+title1+"} master= {"+rn+"}");
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

//{{{ getLocalAtoms
//##############################################################################
    public AtomState[] getLocalAtoms(Residue r, Model m, String sel) throws AtomException
    {
        ModelState s = m.getState();
        AtomState[] atoms = null;
        if(sel.equals(SC))
        {
            if(r.getName().equals("ARG")) atoms = new AtomState[] {
                s.get(r.getAtom(" CA ")),
                s.get(r.getAtom(" CB ")),
                s.get(r.getAtom(" CG ")),
                s.get(r.getAtom(" CD ")),
                s.get(r.getAtom(" NE ")),
                s.get(r.getAtom(" CZ ")) };
            else if(r.getName().equals("GLN")) atoms = new AtomState[] {
                s.get(r.getAtom(" CA ")),
                s.get(r.getAtom(" CB ")),
                s.get(r.getAtom(" CG ")),
                s.get(r.getAtom(" CD ")) };
            else if(r.getName().equals("GLU")) atoms = new AtomState[] {
                s.get(r.getAtom(" CA ")),
                s.get(r.getAtom(" CB ")),
                s.get(r.getAtom(" CG ")),
                s.get(r.getAtom(" CD ")) };
            else if(r.getName().equals("ASN")) atoms = new AtomState[] {
                s.get(r.getAtom(" CA ")),
                s.get(r.getAtom(" CB ")),
                s.get(r.getAtom(" CG ")) };
            else if(r.getName().equals("ASP")) atoms = new AtomState[] {
                s.get(r.getAtom(" CA ")),
                s.get(r.getAtom(" CB ")),
                s.get(r.getAtom(" CG ")) };
        }
        else if(sel.equals(MC_PREV))
        {
            Residue minus0 = r, minus1 = null, minus2 = null;
            minus1 = minus0.getPrev(m);
            if(minus1 == null) return null;
            if(minus0.getName().equals("ARG"))
            {
                minus2 = minus1.getPrev(m);
                if(minus2 == null) return null;
            }
            if(minus0.getName().equals("ARG")) atoms = new AtomState[] {
                s.get(minus0.getAtom(" CA ")),
                s.get(minus0.getAtom(" N  ")),
                s.get(minus1.getAtom(" C  ")),
                s.get(minus1.getAtom(" CA ")),
                s.get(minus1.getAtom(" N  ")),
                s.get(minus2.getAtom(" C  ")) };
            else if(minus0.getName().equals("GLN") || minus0.getName().equals("GLU")) atoms = new AtomState[] {
                s.get(minus0.getAtom(" CA ")),
                s.get(minus0.getAtom(" N  ")),
                s.get(minus1.getAtom(" C  ")),
                s.get(minus1.getAtom(" CA ")) };
            else if(minus0.getName().equals("ASN") || minus0.getName().equals("ASP")) atoms = new AtomState[] {
                s.get(minus0.getAtom(" CA ")),
                s.get(minus0.getAtom(" N  ")),
                s.get(minus1.getAtom(" C  ")) };
        }
        else if(sel.equals(MC_NEXT))
        {
            Residue plus0 = r, plus1 = null, plus2 = null;
            plus1 = plus0.getNext(m);
            if(plus1 == null) return null;
            if(plus0.getName().equals("ARG"))
            {
                plus2 = plus1.getNext(m);
                if(plus2 == null) return null;
            }
            if(plus0.getName().equals("ARG")) atoms = new AtomState[] {
                s.get(plus0.getAtom(" CA ")),
                s.get(plus0.getAtom(" C  ")),
                s.get(plus1.getAtom(" N  ")),
                s.get(plus1.getAtom(" CA ")),
                s.get(plus1.getAtom(" C  ")),
                s.get(plus2.getAtom(" N  ")) };
            else if(plus0.getName().equals("GLN") || plus0.getName().equals("GLU")) atoms = new AtomState[] {
                s.get(plus0.getAtom(" CA ")),
                s.get(plus0.getAtom(" C  ")),
                s.get(plus1.getAtom(" N  ")),
                s.get(plus1.getAtom(" CA ")) };
            else if(plus0.getName().equals("ASN") || plus0.getName().equals("ASP")) atoms = new AtomState[] {
                s.get(plus0.getAtom(" CA ")),
                s.get(plus0.getAtom(" C  ")),
                s.get(plus1.getAtom(" N  ")) };
        }
        else
        {
            System.err.println("unrecognized atom selection: "+sel);
            return null;
        }
        return atoms;
    }
//}}}
/*
//{{{ getSidechainAtoms [OLD]
//##############################################################################
    public AtomState[] getSidechainAtoms(Residue r, ModelState s, boolean flipSc) throws AtomException
    {
        AtomState[] atoms = null;
        if(r.getName().equals("ARG")) atoms = new AtomState[] {
            s.get(r.getAtom(" CA ")),
            s.get(r.getAtom(" CB ")),
            s.get(r.getAtom(" CG ")),
            s.get(r.getAtom(" CD ")),
            s.get(r.getAtom(" NE ")),
            s.get(r.getAtom(" CZ ")),
            s.get(r.getAtom(" NH1")),
            s.get(r.getAtom(" NH2")) };
        else if(r.getName().equals("GLN")) atoms = new AtomState[] {
            s.get(r.getAtom(" CA ")),
            s.get(r.getAtom(" CB ")),
            s.get(r.getAtom(" CG ")),
            s.get(r.getAtom(" CD ")),
            s.get(r.getAtom(flipSc ? " OE1" : " NE2")) };
        else if(r.getName().equals("GLU")) atoms = new AtomState[] {
            s.get(r.getAtom(" CA ")),
            s.get(r.getAtom(" CB ")),
            s.get(r.getAtom(" CG ")),
            s.get(r.getAtom(" CD ")),
            s.get(r.getAtom(flipSc ? " OE1" : " OE2")) };
        else if(r.getName().equals("ASN")) atoms = new AtomState[] {
            s.get(r.getAtom(" CA ")),
            s.get(r.getAtom(" CB ")),
            s.get(r.getAtom(" CG ")),
            s.get(r.getAtom(" OD1")),
            s.get(r.getAtom(" ND2")) };
        else if(r.getName().equals("ASP")) atoms = new AtomState[] {
            s.get(r.getAtom(" CA ")),
            s.get(r.getAtom(" CB ")),
            s.get(r.getAtom(" CG ")),
            s.get(r.getAtom(" OD1")),
            s.get(r.getAtom(" OD2")) };
        
        if(flipSc && !r.getName().equals("GLN") && !r.getName().equals("GLU"))
        {
            // only one of the flippable atoms will be used for Gln/Glu
            AtomState temp = atoms[atoms.length-1];        // remember last atom
            atoms[atoms.length-1] = atoms[atoms.length-2]; // move second-to-last atom
            atoms[atoms.length-2] = temp;                  // reinstate last atom
        }
        
        return atoms;
    }
//}}}

//{{{ getMainchainAtoms [OLD]
//##############################################################################
    public AtomState[] getMainchainAtoms(Residue minus0, Model m, ModelState s) throws AtomException
    {
        Residue minus1 = null, minus2 = null;
        minus1 = minus0.getPrev(m);
        if(minus1 == null) return null;
        if(minus0.getName().equals("ARG"))
        {
            minus2 = minus1.getPrev(m);
            if(minus2 == null) return null;
        }
        
        AtomState[] atoms = null;
        if(minus0.getName().equals("ARG")) atoms = new AtomState[] {
            s.get(minus0.getAtom(" CA ")),
            s.get(minus0.getAtom(" N  ")),
            s.get(minus1.getAtom(" C  ")),
            s.get(minus1.getAtom(" CA ")),
            s.get(minus1.getAtom(" N  ")),
            s.get(minus2.getAtom(" C  ")),
            s.get(minus2.getAtom(" O  ")),
            s.get(minus2.getAtom(" CA ")) };
        else if(minus0.getName().equals("GLN") || minus0.getName().equals("GLU")) atoms = new AtomState[] {
            s.get(minus0.getAtom(" CA ")),
            s.get(minus0.getAtom(" N  ")),
            s.get(minus1.getAtom(" C  ")),
            s.get(minus1.getAtom(" CA ")),
            s.get(minus1.getAtom(" N  ")) };
        else if(minus0.getName().equals("ASN") || minus0.getName().equals("ASP")) atoms = new AtomState[] {
            s.get(minus0.getAtom(" CA ")),
            s.get(minus0.getAtom(" N  ")),
            s.get(minus1.getAtom(" C  ")),
            s.get(minus1.getAtom(" O  ")),
            s.get(minus1.getAtom(" CA ")) };
        
        return atoms;
    }
//}}}
*/
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
