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
* <code>HelixBuilder</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class HelixBuilder //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    String filename;
    String list;                 // list of filenames if want more than one
    ArrayList<String> filenames; // from list
    ArrayList<Helix> helices;
    boolean doNcaps;
    boolean doKin;
    boolean doPrint;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public HelixBuilder()
    {
        super();
        filename  = null;
        list      = null;
        filenames = null;
        helices   = new ArrayList<Helix>();
        doNcaps   = false;
        doKin     = false;
        doPrint   = true;
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
        getPsiPhis(peptides, state);
        
        // Try to identify *helix* based on H-bonding pattern
        assignSecStruct(peptides); // all Peptide data has now been filled in!
        addHelices(peptides, model);
        findAxes(model, state);
        if (doNcaps) findNcaps(model, state);
        
        if (doKin)
        {
            System.out.println("@kinemage {"+filename+" helices}");
            sketchHbonds(System.out, peptides, state);
            sketchNcaps(System.out, state);
            sketchAxes(System.out);
        }
        //printHelicalPeptides(System.out, peptides);
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

//{{{ getPsiPhis
//##############################################################################
    /**
    * For each peptide (Ca i-1, C i-1, N i, Ca i), set the psi for the N-most
    * residue and the phi for the C-most residue.
    * This will be used to assign secondary structure below (phi,psi in helical
    * range).
    */
    void getPsiPhis(Collection peptides, ModelState state)
    {
        Peptide[] pep = (Peptide[]) peptides.toArray(new Peptide[peptides.size()]);
        for (int i = 0; i < pep.length; i++)
        {
            if (pep[i].nRes != null && pep[i].cRes != null) try
            {
                // Get psiN
                AtomState N_iminus1  = state.get(pep[i].nRes.getAtom(" N  "));
                AtomState Ca_iminus1 = state.get(pep[i].nRes.getAtom(" CA "));
                AtomState C_iminus1  = state.get(pep[i].nRes.getAtom(" C  "));
                AtomState N_i        = state.get(pep[i].cRes.getAtom(" N  "));
                pep[i].psiN = new Triple().dihedral(N_iminus1, Ca_iminus1,
                    C_iminus1, N_i);
                
                // Get phiC
                //AtomState C_iminus1  = state.get(pep[i].nRes.getAtom(" C  "));
                //AtomState N_i        = state.get(pep[i].cRes.getAtom(" N  "));
                AtomState Ca_i       = state.get(pep[i].cRes.getAtom(" CA "));
                AtomState C_i        = state.get(pep[i].cRes.getAtom(" C  "));
                pep[i].phiC = new Triple().dihedral(C_iminus1, N_i, Ca_i, C_i);
                
                System.out.println("**"+pep[i]);
                System.out.println("psiN: "+pep[i].psiN+", phiC: "+pep[i].phiC);
                
            } 
            catch (AtomException ex) {} // left as null
        }
    }
//}}}

//{{{ assignSecStruct
//##############################################################################
    /**
    * We'll use Ian's infrastructure for SheetBuilder, but since we *are* 
    * looking for helices, we'll take only take peptides for which either
    * (1) pep's cRes N is H-bonded to resi-4's O and pep's cRes phi is in beta range
    *     or
    * (2) pep's nRes O is H-bonded to resi+4's N and pep's nRes psi is in beta range
    * 
    * Note that the phi & psi cutoffs used above are very approximate and were taken
    * from a simple visual inspection of the general case Rama plot in the 2003 Ca
    * geom paper.
    */
    void assignSecStruct(Collection peptides)
    {
        // First run-through
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep != null)
            {
                System.out.println(pep+" "+pep.phiC+" "+pep.psiN);
                
                if(pep.next != null && pep.prev != null && 
                   pep.nRes != null && pep.cRes != null)
                {
                    boolean hbForward = false, hbBackward = false;
                    
                    // Peptide's C=O makes an HB forward (beg or mid)
                    if (pep.hbondO != null && pep.hbondO.nRes != null)
                    {
                        int seqNumDiff = 
                            ((pep.hbondO).nRes).getSequenceInteger() -
                            (pep.nRes).getSequenceInteger();
                        if (pep.phiC > -180 && pep.phiC < 0 && seqNumDiff == 3)
                            hbForward = true;
                    }
                    
                    // Peptide's N-H makes an HB backwards (mid or end)
                    if (pep.hbondN != null && pep.hbondN.nRes != null)
                    {
                        int seqNumDiff = 
                            (pep.nRes).getSequenceInteger() - 
                            ((pep.hbondN).nRes).getSequenceInteger();
                        if (pep.psiN > -90 && pep.psiN < 45 && seqNumDiff == 3)
                            hbBackward = true;   //alter to allow for C caps?
                    }
                    
                    if (hbForward || hbBackward)
                        pep.isHelix = true;
                }
            }
        }
        
        // Second run-through to catch residues on ends that either make an HB
        // or are in the proper phi,psi range. Not as stringent here: make it 
        // either/or so we pick up those iffy ones on the ends.
        //????
        
    }
//}}}

//{{{ addHelices
//##############################################################################
    void addHelices(Collection peptides, Model model)
    {
        TreeSet thisHelixsResidues = new TreeSet<Residue>();
        
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if (pep.isHelix)
            {
                if (pep.nRes != null)
                    thisHelixsResidues.add(pep.nRes);
                if (pep.cRes != null)
                    thisHelixsResidues.add(pep.cRes);
                
                if (!pep.next.isHelix || pep.next == null || pep.cRes == null)
                {
                    // Make this helix, add it to the list, and
                    // reset this helix-making process
                    Helix thisHelix = new Helix(thisHelixsResidues);
                    helices.add(thisHelix);
                    thisHelixsResidues = new TreeSet<Residue>();
                }
            }
        }
        
        // Get rid of any helices with < 4 residues
        for (int i = 0; i < helices.size(); i++)
        {
            if (helices.get(i).residues.size() < 4)
                helices.remove(i);
        }
    }
//}}}

//{{{ findNcaps
//##############################################################################
public void findNcaps(Model model, ModelState state)
    /** 
    * We just assume the first residue in a helix is the Ncap.
    * This is a *very simple* Ncap-finding algorithm!
    *
    * Can alter later to incorporate Ca position relative to cylinder of
    * helix as in original RLab helix cap paper
    */
    {
        for (Helix helix : helices)
        {
            // Find the Ncaps
            if (helix.ncap != null) continue;
            else helix.ncap = helix.getRes("first");
            
            // Calculate a "backrub-like" angle for each Ncap
            // This is angle between local helix axis for the Ncap residue
            // and the normal to the plane formed by Ca(i,i-1,i+1).
            if (helix.ncap.getNext(model) != null && 
                helix.ncap.getPrev(model) != null )
            {
                try
                {
                        AtomState Ca     = state.get(helix.ncap.getAtom(" CA "));
                    AtomState prevCa = state.get(helix.ncap.getNext(model).getAtom(" CA "));
                    AtomState nextCa = state.get(helix.ncap.getNext(model).getAtom(" CA "));
                    
                    Triple normal = new Triple().likeNormal(prevCa, Ca, nextCa); 
                    
                    Triple tail = helix.axisTails.get(0);
                    Triple head = helix.axisHeads.get(0);
                    Triple axisAtOrigin = new Triple(head.getX()-tail.getX(),
                        head.getY()-tail.getY(), head.getZ()-tail.getZ() );
                    
                    // OK to mess with normal directly now
                    helix.ncapAngle = normal.angle(axisAtOrigin);
                }
                catch (driftwood.moldb2.AtomException ae)
                {
                    System.out.println("Problem calculating ncap angle...");
                }
            }
        } 
    }
//}}}

//{{{ findAxes
//##############################################################################
public void findAxes(Model model, ModelState state)
    {
        /** 
        * Calculates local helical axis for each set of 4 Ca's in each Helix. 
        * 
        * This is only one way of doing it... Can also Google some other
        * possibilities later.
        * This will be used to tabulate the angle between the local helix axis
        * and the normal vectors of certain peptides in the helix (e.g. Ncap, 
        * Asn's w/in helix) to look for evidence of backrubs in a local
        * coordinate system.
        * The point is to examine under what local circumstances the backrub
        * occurs.
        */
        for (Helix helix : helices)
        {
            ArrayList<Triple> thisHelixAxisHeads = new ArrayList<Triple>();
            ArrayList<Triple> thisHelixAxisTails = new ArrayList<Triple>();
            
            for (Residue res0 : helix.residues) // should be a sorted AL
            {
                // Take line from Ca_i   to Ca_i+2
                //  and line from Ca_i+1 to Ca_i+3
                // Line between the midpoints of those two lines points in 
                // the direction of a local axis vector.
                
                // Find the requisite 4 Ca's
                Residue[] res = new Residue[4];
                res[0] = res0;
                for (int i = 0; i < 3; i ++)
                    if (res[i].getNext(model) != null)
                        res[i+1] = res[i].getNext(model);
                AtomState[] cas = new AtomState[4];
                for (int i = 0; i < 4; i ++)
                    if (res[i] != null)
                    {
                        try
                        {
                            cas[i] = state.get(res[i].getAtom(" CA "));
                        }
                        catch ( driftwood.moldb2.AtomException ae)
                        {
                            System.out.println("Can't find CA in res "+res[i]);
                        }
                    }
                int numValidCas = 0;
                for (int i = 0; i < cas.length; i ++)
                    if (cas[i] != null)
                        numValidCas ++;
                
                if (numValidCas == 4)
                {
                    // Calculate the axis
                    Triple midpoint0to2 = new Triple().likeMidpoint(
                        cas[0], cas[2]);
                    Triple midpoint1to3 = new Triple().likeMidpoint(
                        cas[1], cas[3]);
                    Triple axisDir = new Triple().likeVector(
                        midpoint0to2, midpoint1to3);
                    Triple axisHead = axisDir.unit().mult(2).add(midpoint0to2);
                    Triple axisTail = midpoint0to2; // can change to midpoint
                                                    // of midpoints later...
                    // Add the axis to this helix
                    thisHelixAxisHeads.add(axisHead);
                    thisHelixAxisTails.add(axisTail);
                }
                else
                {
                    System.err.println("Wrong number Cas for this helical axis!");
                    System.err.println("Expected 4 but got "+numValidCas+"...");
                }
            }
            
            // Finish by giving a list of axes to this helix
            helix.axisHeads = thisHelixAxisHeads;
            helix.axisTails = thisHelixAxisTails;
            
        }//for(each Helix in helices)
    }
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
            if(pep.isHelix) //Beta)
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

//{{{ sketchNcaps
//##############################################################################
    void sketchNcaps(PrintStream out, ModelState state)
    {
        
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {ncaps}");
        out.println("@balllist {ncaps} radius= 0.3 color= hotpink");
        for (Helix helix : helices)
        {
            try
            {
                if (helix.ncap != null)
                {
                    AtomState ncapCa = state.get(helix.ncap.getAtom(" CA "));
                    out.println("{helix '"+helix.toString()+"' ncap} "+
                        df.format(ncapCa.getX())+" "+
                        df.format(ncapCa.getY())+" "+
                        df.format(ncapCa.getZ()) );
                }
            }
            catch (driftwood.moldb2.AtomException ae)
            {
                System.err.println("Can't find atom ' CA ' in helix "+helix);
            }
        }
    }
//}}}

//{{{ sketchAxes
//##############################################################################
    void sketchAxes(PrintStream out)
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        out.println("@group {helix axes}");
        out.println("@vectorlist {helix axes} color= orange");
        for (Helix helix : helices)
        {
            if (helix.axisHeads != null && helix.axisTails != null)
            {
                for (int i = 0; i < helix.axisHeads.size(); i++)
                {
                    // There should be the same number of  entries in axisHeads
                    // and axisTails, so it should be OK to do this
                    out.println("{helix ("+helix.toString()+")"+
                        " res"+((int)i+1)+"-"+((int)i+2)+" axis tail}P "+
                    df.format(helix.axisTails.get(i).getX())+" "+
                    df.format(helix.axisTails.get(i).getY())+" "+
                    df.format(helix.axisTails.get(i).getZ()) );
                    
                    out.println("{helix ("+helix.toString()+")"+
                        " res"+((int)i+1)+"-"+((int)i+2)+" axis head} "+
                    df.format(helix.axisHeads.get(i).getX())+" "+
                    df.format(helix.axisHeads.get(i).getY())+" "+
                    df.format(helix.axisHeads.get(i).getZ()) );
                }
            }
        }
    }
//}}}

//{{{ printHelicalPeptides
//##############################################################################
    void printHelicalPeptides(PrintStream out, Collection peptides)
    {
        for(Iterator iter = peptides.iterator(); iter.hasNext(); )
        {
            Peptide pep = (Peptide) iter.next();
            if(pep.isHelix) //Beta)
                out.println(filename.substring(0,4)+" "+pep); 
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
        // Make helices
        if (list != null) // more than one filename
        {
            // Set up filenames ArrayList
            filenames = new ArrayList<String>();
            File f = new File(list);
            Scanner s = new Scanner(f);
            while (s.hasNext());
                filenames.add(s.next());
            
            // Load model group from PDB file(s)
            for (int i = 0; i < filenames.size(); i++)
            {
                filename = filenames.get(i); // seen by doFile()
                doFile();
            }
        }
        else // just one filename
            doFile();
        
        if (doPrint)
            printHelices();
    }
    
    public static void main(String[] args)
    {
        HelixBuilder mainprog = new HelixBuilder();
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

//{{{ doFile
//##############################################################################
public void doFile() throws IOException
    {
        File file = new File(filename);
        LineNumberReader in = new LineNumberReader(new FileReader(file));
        PdbReader pdbReader = new PdbReader();
        CoordinateFile cf = pdbReader.read(in);
        
        Model m = cf.getFirstModel();
        ModelState state = m.getState();
        processModel(cf.getIdCode(), m, state);
    }
//}}}

//{{{ printHelices
//##############################################################################
public void printHelices()
    {
        System.out.println("Total number helices: "+helices.size());
        for (Helix helix : helices)
        {
            System.out.println("** "+helix.toString());
            for (Residue residue : helix.residues)
                System.out.println("  "+residue);
            if (doNcaps)
            {
                if (helix.ncap != null)
                    System.out.println("  ncap: "+helix.ncap);
                if (helix.ncapAngle != Double.NaN)
                    System.out.println("  ncap angle: "+helix.ncapAngle);
            }
            System.out.println();
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
            InputStream is = getClass().getResourceAsStream("HelixBuilder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'HelixBuilder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.HelixBuilder");
        System.err.println("Copyright (C) 2007 by Daniel Keedy. All rights reserved.");
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
        if (filename == null)
            filename = arg;
        else
            System.out.println("Didn't need "+arg+"; already have file "+filename);
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-list"))
        {
            list = param;
        }
        else if(flag.equals("-kin"))
        {
            doKin = true;
            doPrint = false;
        }
        else if(flag.equals("-print"))
        {
            doPrint = true;
        }
        else if(flag.equals("-ncaps"))
        {
            doNcaps = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}

//{{{ [myDihedral] unused now!
//##############################################################################
    /**
    * This is Ian's comment in driftwood.r3.Triple which is "too expensive in 
    * terms of creating objects, as it creates 3 new Triples, but it *is* easy
    * to understand the code.
    * 
    * I'm using it since I got weird results when I just used Triple.dihedral
    * (4 Triples).  Don't know why it would work, but we'll see...
    */
    double myDihedral(Triple a, Triple b, Triple c, Triple d)
    {
        Triple e, f, g, u, v;
        e = new Triple().likeDiff(b, a);
        f = new Triple().likeDiff(c, b);
        g = new Triple().likeDiff(d, c);
        u = e.cross(f); // overwrite 'e' b/c we don't need it anymore
        v = f.cross(g); // overwrite 'f' b/c we don't need it anymore
        
        double dihedral = u.angle(v);
        
        // Solve handedness problem:
        if(u.angle(g) > 90.0) dihedral = -dihedral;
        
        return dihedral;
    }
//}}}

}//class

