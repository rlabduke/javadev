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
* <code>SheetBuilder</code> has not yet been documented.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class SheetBuilder //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SheetBuilder()
    {
        super();
    }
//}}}

//{{{ processModel
//##############################################################################
    void processModel(String modelName, Model model, ModelState state)
    {
        // Create a set of Peptides and connect them up
        Collection peptides = createPeptides(model, state);
        connectPeptides(peptides);
        findHBonds(peptides, state);
        
        // Try to identify sheet based on H-bonding pattern
        assignSecStruct(peptides);
        // all Peptide data has now been filled in!
        
        // Map each residue to a beta-sheet plane
        // and a normal to that plane, if possible.
        // Returns a Map<Residue, Triple>
        Map normals = calcSheetNormals(peptides, model, state);
        
        // Flesh the normals out into a local coordinate system
        // and measure the Ca-Cb's angle to the normal.
        Map angles = measureSheetAngles(peptides, normals, state);
        
        //System.out.println("@text");
        //printAlongStrandNeighborAngles(System.out, peptides, angles);
        printCrossStrandNeighborAngles(System.out, modelName, peptides, angles);
        //System.out.println("@kinemage 1");
        //sketchHbonds(System.out, peptides, state);
        //sketchNormals(System.out, normals, state);
        //sketchLocalAxes(System.out, angles, state);
        //sketchPlanes(System.out, angles, state);
    }
//}}}

//{{{ createPeptides
//##############################################################################
    /**
    * Given a model and a state, create Peptide objects for all the "complete"
    * peptides in the model.
    * These fields will be filled: nRes, cRes, midpoint
    */
    Collection createPeptides(Model model, ModelState state)
    {
        ArrayList peptides = new ArrayList();
        Residue prev = null;
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if(! AminoAcid.isAminoAcid(res)) continue;
            
            try
            {
                Peptide pep = new Peptide(prev, res, state); // prev could be null
                // If prev is null, no distance check.
                if(prev == null) peptides.add(pep);
                // If we have two residues, make sure they're connected,
                // or else do two separate half-peptides.
                else
                {
                    AtomState pepC = state.get(prev.getAtom(" C  "));
                    AtomState pepN = state.get(res.getAtom(" N  "));
                    if(pepC.sqDistance(pepN) < 4.0) // within 2 A of each other
                        peptides.add(pep);
                    else
                    {
                        peptides.add(new Peptide(prev, null, state));
                        peptides.add(new Peptide(null, res,  state));
                    }
                }
            }
            catch(AtomException ex) // missing atoms? try halves.
            {
                try { peptides.add(new Peptide(prev, null, state)); }
                catch(AtomException ex2) {}
                try { peptides.add(new Peptide(null, res,  state)); }
                catch(AtomException ex2) {}
            }
            prev = res;
        }//for all residues
        
        // Add last residue as a half-peptide
        try { peptides.add(new Peptide(prev, null, state)); }
        catch(AtomException ex) {}
        
        return peptides;
    }
//}}}

//{{{ connectPeptides
//##############################################################################
    /**
    * Given an ordered collection of peptides, connect them on the criteria that
    * successive peptides must share a common residue between them.
    * These fields will be filled: prev, next, chain, index.
    * Chain and index will be indexed starting from 1, not 0.
    */
    void connectPeptides(Collection peptides)
    {
        Peptide prev = null;
        int chain = 0;
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pept = (Peptide) iter.next();
            if(prev != null && prev.nRes != null && pept.cRes != null && prev.nRes == pept.cRes)
            {
                // Chain is continuous
                prev.next = pept;
                pept.prev = prev;
                pept.chain = prev.chain;
                pept.index = prev.index+1;
            }
            else
            {
                // Chain is broken
                pept.chain = ++chain;
                pept.index = 1;
            }
            prev = pept;
        }
    }
//}}}

//{{{ findHBonds
//##############################################################################
    /**
    * Maps out all the inter-peptide H-bonds based on the criteria defined in
    * W. Kabsch and C. Sander (1983) Biopolymers, 22:2577.
    * The basic idea is that the H-bond is accepted if
    * E = 0.42*0.20*332*(1/rON + 1/rCH - 1/rOH - 1/rCN) is less than -0.5 kcal/mol.
    * Atom-atom distances are in Angstroms and E is in kcal/mol.
    * Ideal alignment allows distances up to 5.2 A (O to N);
    * ideal distance allows angles up to 63 degrees.
    * Be careful -- it will try to pick up i to {i, i+1, i+2} "H-bonds".
    * Only the strongest H-bond for each N to an unbonded O is kept.
    * These fields will be filled: hbondN, hbondO.
    */
    void findHBonds(Collection peptides, ModelState state)
    {
        Peptide[] pep = (Peptide[]) peptides.toArray(new Peptide[peptides.size()]);
        // Do carbon/oxygen lookup just once
        AtomState[] carbon = new AtomState[pep.length];
        AtomState[] oxygen = new AtomState[pep.length];
        for(int i = 0; i < pep.length; i++)
        {
            if(pep[i].cRes != null) try {
                carbon[i] = state.get(pep[i].cRes.getAtom(" C  "));
                oxygen[i] = state.get(pep[i].cRes.getAtom(" O  "));
            } catch(AtomException ex) {} // left as null
        }
        // For each N/H, look for bonded C/O
        final double maxNOdist2 = 5.3*5.3;
        for(int i = 0; i < pep.length; i++)
        {
            if(pep[i].nRes != null) try
            {
                AtomState nitrogen = state.get(pep[i].nRes.getAtom(" N  "));
                AtomState hydrogen = state.get(pep[i].nRes.getAtom(" H  "));
                Peptide bestBond = null;
                double bestBondE = -0.5;
                for(int j = 0; j < pep.length; j++)
                {
                    if(i == j) continue; // no intra-peptide H-bonds
                    if(pep[i].chain == pep[j].chain && Math.abs(pep[i].index - pep[j].index) <= 2)
                        continue; // no i to {i, i+1, i+2} H-bonds!
                    if(carbon[j] == null || oxygen[j] == null) continue;
                    if(nitrogen.sqDistance(oxygen[j]) > maxNOdist2) continue;
                    
                    double rON = oxygen[j].distance(nitrogen);
                    double rCH = carbon[j].distance(hydrogen);
                    double rOH = oxygen[j].distance(hydrogen);
                    double rCN = carbon[j].distance(nitrogen);
                    double energy = 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
                    if(energy < bestBondE && pep[j].hbondO == null)
                    {
                        bestBond = pep[j];
                        bestBondE = energy;
                    }
                }//for all possible partners
                if(bestBond != null)
                {
                    pep[i].hbondN = bestBond;
                    bestBond.hbondO = pep[i];
                }
            }
            catch(AtomException ex) {} // no connections then
        }//for each peptide N
    }
//}}}

//{{{ assignSecStruct
//##############################################################################
    /**
    * Given a collection of Peptides, we attempt to flag some of them as being
    * part of a beta sheet.
    * <p>If they're antiparallel and beta, and the nitrogen of peptide n is
    * H-bonded to the oxygen of peptide m, then one of the following is true: <ul>
    * <li>n+1's O is H-bonded to m-1's N</li>
    * <li>n-1's O is H-bonded to m+1's N</li>
    * </ul>
    * <p>If they're parallel and beta, and the nitrogen of peptide n is
    * H-bonded to the oxygen of peptide m, then one of the following is true: <ul>
    * <li>n+1's O is H-bonded to m+1's N</li>
    * <li>n-1's O is H-bonded to m-1's N</li>
    * </ul>
    * However, it must ALSO be true that |n-m| is greater than 5 OR that
    * n and m are in different chains to avoid picking up helices here.
    * These fields will be filled: isBeta, isParallelN, isParallelO
    */
    void assignSecStruct(Collection peptides)
    {
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pepN = (Peptide) iter.next();
            Peptide pepM = pepN.hbondN;
            if(pepM != null)
            {
                // Antiparallel?
                if(pepN.next != null && pepM.prev != null && pepN.next.hbondO == pepM.prev)
                {
                    pepN.isBeta = pepM.isBeta = true;
                    pepN.isParallelN = pepM.isParallelO = false;
                }
                else if(pepN.prev != null && pepM.next != null && pepN.prev.hbondO == pepM.next)
                {
                    pepN.isBeta = pepM.isBeta = true;
                    pepN.isParallelN = pepM.isParallelO = false;
                }
                // Parallel?
                else if(pepN.chain != pepM.chain || Math.abs(pepN.index - pepM.index) > 5)
                {
                    if(pepN.next != null && pepM.next != null && pepN.next.hbondO == pepM.next)
                    {
                        pepN.isBeta = pepM.isBeta = true;
                        pepN.isParallelN = pepM.isParallelO = true;
                    }
                    else if(pepN.prev != null && pepM.prev != null && pepN.prev.hbondO == pepM.prev)
                    {
                        pepN.isBeta = pepM.isBeta = true;
                        pepN.isParallelN = pepM.isParallelO = true;
                    }
                }
            }
        }
    }
//}}}

//{{{ calcSheetNormals
//##############################################################################
    /**
    * Returns a Map&lt;Residue, Triple&gt; that maps each Residue in model
    * that falls in a "reasonable" part of the beta sheet to a Triple
    * representing the normal vector of the beta sheet at that Residue's C-alpha.
    * The normal is the normal of a plane least-squares fit through
    * six nearby peptide centers: the ones before and after this residue in
    * the strand, and their two (each) H-bonding partners, all of which
    * must be present and classified as being in beta sheet.
    */
    Map calcSheetNormals(Collection peptides, Model model, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map normals = new HashMap();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Peptide cPep = (Peptide) cPeptides.get(res);
            Peptide nPep = (Peptide) nPeptides.get(res);
            if(cPep != null && cPep.hbondN != null && cPep.hbondO != null
            && nPep != null && nPep.hbondN != null && nPep.hbondO != null
            && cPep.isBeta && cPep.hbondN.isBeta && cPep.hbondO.isBeta
            && nPep.isBeta && nPep.hbondN.isBeta && nPep.hbondO.isBeta)
            {
                Collection guidePts = new ArrayList();
                guidePts.add(cPep.hbondN.midpoint);
                guidePts.add(cPep.midpoint);
                guidePts.add(cPep.hbondO.midpoint);
                guidePts.add(nPep.hbondN.midpoint);
                guidePts.add(nPep.midpoint);
                guidePts.add(nPep.hbondO.midpoint);
                LsqPlane lsqPlane = new LsqPlane(guidePts);
                Triple normal = new Triple(lsqPlane.getNormal());
                normals.put(res, normal);
                // Try to make it point the same way as Ca-Cb
                try
                {
                    AtomState ca = state.get(res.getAtom(" CA "));
                    AtomState cb = state.get(res.getAtom(" CB "));
                    Triple cacb = new Triple(cb).sub(ca);
                    if(cacb.dot(normal) < 0) normal.neg();
                }
                catch(AtomException ex) {} // oh well (e.g. Gly)
            }
        }
        return normals;
    }
//}}}

//{{{ measureSheetAngles
//##############################################################################
    /** Returns a map of Residues to SheetAxes */
    Map measureSheetAngles(Collection peptides, Map normals, ModelState state)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }
        
        Map angles = new HashMap();
        for(Iterator iter = normals.keySet().iterator(); iter.hasNext(); )
        {
            Residue res     = (Residue) iter.next();
            Triple  normal  = (Triple) normals.get(res);
            Peptide cPep    = (Peptide) cPeptides.get(res);
            Peptide nPep    = (Peptide) nPeptides.get(res);
            if(cPep == null || nPep == null) continue;
            Triple  n2c     = new Triple(cPep.midpoint).sub(nPep.midpoint);
            try
            {
                AtomState   ca      = state.get(res.getAtom(" CA "));
                AtomState   cb      = state.get(res.getAtom(" CB "));
                Triple      cacb    = new Triple(cb).sub(ca);
                SheetAxes   axes    = new SheetAxes(normal, n2c, cacb);
                angles.put(res, axes);
            }
            catch(AtomException ex) {}
        }
        return angles;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ sketchHbonds
//##############################################################################
    void sketchHbonds(PrintStream out, Collection peptides, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {peptides & hbonds}");
        out.println("@balllist {peptides} radius= 0.1 color= green");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.isBeta)
                out.println("{"+pep+"} r=0.3 "+pep.midpoint.format(df));
            else
                out.println("{"+pep+"} "+pep.midpoint.format(df));
        }
        
        out.println("@vectorlist {N hbonds} color= sky");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.hbondN != null)
            {
                try
                {
                    AtomState h = state.get(pep.nRes.getAtom(" H  "));
                    AtomState o = state.get(pep.hbondN.cRes.getAtom(" O  "));
                    out.println("{"+pep+"}P "+h.format(df));
                    out.println("{"+pep.hbondN+"} "+o.format(df));
                }
                catch(AtomException ex) {}
            }
        }
        
        out.println("@vectorlist {O hbonds} color= red");
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.hbondO != null)
            {
                try
                {
                    AtomState o = state.get(pep.cRes.getAtom(" O  "));
                    AtomState h = state.get(pep.hbondO.nRes.getAtom(" H  "));
                    out.println("{"+pep+"}P "+o.format(df));
                    out.println("{"+pep.hbondO+"} "+h.format(df));
                }
                catch(AtomException ex) {}
            }
        }
    }
//}}}

//{{{ sketchNormals
//##############################################################################
    void sketchNormals(PrintStream out, Map normals, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {normals & planes}");
        out.println("@vectorlist {peptides} color= magenta");
        for(Iterator iter = normals.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Triple normal = (Triple) normals.get(res);
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(normal).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{normal: "+tip.format(df)+"} "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ sketchLocalAxes
//##############################################################################
    void sketchLocalAxes(PrintStream out, Map angles, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {local axes}");
        out.println("@vectorlist {axes} color= brown");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(axes.strand).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{strand}red "+tip.format(df));
                tip.like(axes.cross).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{cross}green "+tip.format(df));
                tip.like(axes.normal).add(ca);
                out.println("{"+res+"}P "+ca.format(df));
                out.println("{normal}blue "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
        out.println("@labellist {angles} color= peach");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple(axes.strand).add(ca);
                out.println("{strand: "+df.format(axes.angleAlong)+"}red "+tip.format(df));
                tip.like(axes.cross).add(ca);
                out.println("{cross: "+df.format(axes.angleAcross)+"}green "+tip.format(df));
                tip.like(axes.normal).add(ca);
                out.println("{normal: "+df.format(axes.angleNormal)+"}blue "+tip.format(df));
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ sketchPlanes
//##############################################################################
    void sketchPlanes(PrintStream out, Map angles, ModelState state)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        Transform xform = new Transform();
        // It's important that we visit the residues in order.
        ArrayList residues = new ArrayList(angles.keySet());
        Collections.sort(residues);
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.println("@group {"+res.getCNIT()+"} animate dominant");
            out.println("@vectorlist {axes} color= brown");
            try
            {
                AtomState ca = state.get(res.getAtom(" CA "));
                Triple tip = new Triple();
                for(int i = 0; i < 360; i+=5)
                {
                    tip.like(axes.strand).mult(5);
                    xform.likeRotation(axes.normal, i);
                    xform.transformVector(tip);
                    tip.add(ca);
                    out.println("{"+res+"}P "+ca.format(df));
                    out.println("{plane} "+tip.format(df));
                }
            }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
    }
//}}}

//{{{ printAlongStrandNeighborAngles
//##############################################################################
    void printAlongStrandNeighborAngles(PrintStream out, Collection peptides, Map angles)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }

        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("residue:normal:across:along:next-neighbor?:normal:across:along");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.print(res.getCNIT()+":"+df.format(axes.angleNormal)+":"+df.format(axes.angleAcross)+":"+df.format(axes.angleAlong));
            Peptide pep = (Peptide) cPeptides.get(res);
            SheetAxes next = (SheetAxes) angles.get(pep.nRes);
            if(pep.nRes !=  null && next != null)
                out.print(":"+pep.nRes.getCNIT()+":"+df.format(next.angleNormal)+":"+df.format(next.angleAcross)+":"+df.format(next.angleAlong));
            out.println();
        }
    }
//}}}

//{{{ printCrossStrandNeighborAngles
//##############################################################################
    void printCrossStrandNeighborAngles(PrintStream out, String prefix, Collection peptides, Map angles)
    {
        // Allow mapping from residues to the peptides that hold them.
        Map cPeptides = new HashMap(), nPeptides = new HashMap();
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide p = (Peptide) iter.next();
            if(p.cRes != null) cPeptides.put(p.cRes, p);
            if(p.nRes != null) nPeptides.put(p.nRes, p);
        }

        DecimalFormat df = new DecimalFormat("0.0###");
        //out.println("residue:normal:across:along:acrossN-neighbor?:normal:across:along");
        for(Iterator iter = angles.keySet().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            SheetAxes axes = (SheetAxes) angles.get(res);
            out.print(prefix+":"+res.getCNIT()+":"+df.format(axes.angleNormal)+":"+df.format(axes.angleAcross)+":"+df.format(axes.angleAlong));
            Peptide pep = (Peptide) nPeptides.get(res);
            if(pep.hbondN != null) // has a cross-strand neighbor
            {
                Residue nextRes;
                if(pep.isParallelN) nextRes = pep.hbondN.nRes;
                else                nextRes = pep.hbondN.cRes;
                SheetAxes next = (SheetAxes) angles.get(nextRes);
                if(nextRes !=  null && next != null)
                    out.print(":"+nextRes.getCNIT()+":"+df.format(next.angleNormal)+":"+df.format(next.angleAcross)+":"+df.format(next.angleAlong));
            }
            out.println();
        }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        // Load model group from PDB files
        PdbReader pdbReader = new PdbReader();
        ModelGroup mg = pdbReader.read(System.in);
        
        Model m = mg.getFirstModel();
        ModelState state = m.getState();
        processModel(mg.getIdCode(), m, state);
    }

    public static void main(String[] args)
    {
        SheetBuilder mainprog = new SheetBuilder();
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
            InputStream is = getClass().getResourceAsStream("SheetBuilder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SheetBuilder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.SheetBuilder");
        System.err.println("Copyright (C) 2004 by Ian W. Davis. All rights reserved.");
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

