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
* <code>Helix</code> is a class evolved from Ian's Peptide.
* HelixBuilder makes an array of these then does alignments and whatnot to them..
* At the moment, all fields are public and written to directly.
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* <br>Begun on Tue Mar 30 10:45:56 EST 2004
*/
public class Helix //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Residues in this helix. Inherently contains chain and resnum info. */
    public ArrayList<Residue> residues;
    
    /** PDB code for the structure this helix is in */
    public String pdb;
    
    /** Ncap object with various measurements contained within. */
    public Ncap ncap;
    
    /** Tail position (i.e. beginning of vector) of the 4-Ca helix axis 
    * starting at each residue. */
    public ArrayList<Triple> axisTails;
    
    /** Head position (i.e. end of vector) of the 4-Ca helix axis starting 
    * at each residue. */
    public ArrayList<Triple> axisHeads;
    
    /** Tail position (i.e. beginning of vector) of the vector sum version of
    * the 4-Ca helix axis starting at each residue. */
    public Triple vectorSumAxisTail;
    
    /** Head position (i.e. end of vector) of the vector sum version of 
    * the 4-Ca helix axis starting at each residue. */
    public Triple vectorSumAxisHead;
    
    /** Type of helix, either alpha or 3-10. Depends on H-bonding pattern at 
    * N-cap, so only completed if N-cap is not null */
    public String typeAtNcap;
    
    // Don't need separate ArrayList<Triple> smoothAxisTails/Heads b/c
    // we'll just modify axisTails/Heads if we do that option
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Helix(TreeSet<Residue> residuesSet)
    {
        super();
        Iterator iter = residuesSet.iterator();
        residues = new ArrayList<Residue>();
        while (iter.hasNext())
            residues.add( (Residue)iter.next() );
        Collections.sort(residues);
        
        ncap              = null;
        axisTails         = null;
        axisHeads         = null;
        vectorSumAxisTail = null;
        vectorSumAxisHead = null;
        typeAtNcap        = "(ambiguous)";
    }
//}}}

//{{{ setNcapDistances
//##############################################################################
    public void setNcapDistances(Model model, ModelState state, boolean verbose)
    {
        try
        {
            String name = ncap.res.getName();
            if (name.equals("SER") || name.equals("THR") || name.equals("ASN") || name.equals("ASP"))
            {
                // First, get AtomStates to use for distance calculations
                Residue res = ncap.res;
                Residue res2 = res.getNext(model).getNext(model);
                Residue res3 = res2.getNext(model);
                Residue resminus1 = res.getPrev(model);
                Triple likeH2 = new Triple(state.get(res2.getAtom(" H  ")));
                Triple likeH3 = new Triple(state.get(res3.getAtom(" H  ")));
                Triple scAtom = null;
                Triple scAtom2 = null; // for ASP b/c two poss HB'ing atoms
                if (name.equals("SER"))  scAtom = new Triple(state.get(res.getAtom(" OG ")));
                if (name.equals("THR"))  scAtom = new Triple(state.get(res.getAtom(" OG1")));
                if (name.equals("ASN"))  scAtom = new Triple(state.get(res.getAtom(" OD1")));
                if (name.equals("ASP"))
                {
                    scAtom  = new Triple(state.get(res.getAtom(" OD1")));
                    scAtom2 = new Triple(state.get(res.getAtom(" OD2")));
                }
                Triple likeNcapCa   = new Triple(state.get(res.getAtom(" CA ")));
                Triple likeN3Ca     = new Triple(state.get(res3.getAtom(" CA ")));
                Triple likeNprimeCa = new Triple(state.get(resminus1.getAtom(" CA ")));
                if (verbose)
                {
                    System.err.println("likeNcapCa: '"+likeNcapCa+"'");
                    System.err.println("likeN3Ca: '"+likeN3Ca+"'");
                    System.err.println("likeNprimeCa: '"+likeNprimeCa+"'");
                }
                
                // Set distNcapScToN2H
                double dist = Triple.distance(scAtom, likeH2);
                if (scAtom2 != null)
                {
                    double altDist = Triple.distance(scAtom2, likeH2);
                    if (altDist < dist)  dist = altDist;
                }
                ncap.distNcapScToN2H = dist;
                
                // Set distNcapScToN3H
                dist = Triple.distance(scAtom, likeH3);
                if (scAtom2 != null)
                {
                    double altDist = Triple.distance(scAtom2, likeH3);
                    if (altDist < dist)  dist = altDist;
                }
                ncap.distNcapScToN3H = dist;
                
                // Set distNcapCaToN3Ca
                dist = Triple.distance(likeNcapCa, likeN3Ca);
                ncap.distNcapCaToN3Ca = dist;
                
                // Set distNprimeCaToN3Ca
                dist = Triple.distance(likeNcapCa, likeNprimeCa);
                ncap.distNprimeCaToN3Ca = dist;
            }
        }
        catch (driftwood.moldb2.AtomException ae)
        {
            System.err.println("Problem calculating distances at "+ncap+"...");
        }
    }
//}}}

//{{{ setNcapAngles
//##############################################################################    
    public void setNcapAngles(Model model, ModelState state)
    {
        try
        {
            // One option is angle between the local helix axis for the Ncap residue
            // and the normal to the plane formed by Ca(i,i-1,i+1).
            AtomState ca = state.get(ncap.res.getAtom(" CA "));
            Triple tail = axisTails.get(0);
            Triple head = axisHeads.get(0);
            Triple axisAtOrigin = new Triple(head.getX()-tail.getX(),
                head.getY()-tail.getY(), head.getZ()-tail.getZ() );
            if (ncap.res.getPrev(model) != null && ncap.res.getNext(model) != null)
            {
                // this angle defined
                AtomState prevCa = state.get(ncap.res.getPrev(model).getAtom(" CA "));
                AtomState nextCa = state.get(ncap.res.getNext(model).getAtom(" CA "));
                
                Triple normal = new Triple().likeNormal(prevCa, ca, nextCa); 
                
                ncap.normalTail = ca;
                ncap.normalHead = new Triple(ca.getX()+normal.getX(), 
                    ca.getY()+normal.getY(), ca.getZ()+normal.getZ());
                
                // OK to mess with normal directly now
                ncap.planeNormalAngle = normal.angle(axisAtOrigin);
            }
            // else (default in Ncap constructor: Double.NaN)
            
            // A second option is the angle between the Ncap Ca_Cb vector 
            // and the local helix axis
            Triple likeCa = new Triple(ca); // same coords as ca above but different object
            if (!ncap.res.getName().equals("GLY"))
            {
                Triple likeCb = new Triple(state.get(ncap.res.getAtom(" CB ")));
                Triple caCbAtOrigin = new Triple().likeVector(likeCa, likeCb);
                ncap.caCbAngle = caCbAtOrigin.angle(axisAtOrigin);
            }
            // else (default in Ncap constructor: Double.NaN)
        }
        catch (driftwood.moldb2.AtomException ae)
        {
            System.err.println("Problem calculating Ncap angles...");
        }
    }
//}}}

//{{{ setNcapPhiPsis
//##############################################################################
    public void setNcapPhiPsis(Model model, ModelState state)
    {
        try
        {
            // Phi, psi for Ncap residue
            Triple likeCa = new Triple(state.get(ncap.res.getAtom(" CA ")));
            Triple likeN = new Triple(state.get(ncap.res.getAtom(" N  ")));
            Triple likeC = new Triple(state.get(ncap.res.getAtom(" C  ")));
            if (ncap.res.getPrev(model) != null) // phi defined
            {
                Triple likePrevC = new Triple(state.get(ncap.res.getPrev(model).getAtom(" C  ")));
                ncap.phi = Triple.dihedral(likePrevC, likeN, likeCa, likeC);
            }
            if (ncap.res.getNext(model) != null) // psi defined
            {
                Triple likeNextN = new Triple(state.get(ncap.res.getNext(model).getAtom(" N  ")));
                ncap.psi = Triple.dihedral(likeN, likeCa, likeC, likeNextN);
            }
            
            // Phi, psi for Ncap i+1 residue ("N1")
            if (ncap.res.getNext(model) != null)
            {
                Residue n1 = ncap.res.getNext(model);
                Triple likeN1Ca = new Triple(state.get(n1.getAtom(" CA ")));
                Triple likeN1N = new Triple(state.get(n1.getAtom(" N  ")));
                Triple likeN1C = new Triple(state.get(n1.getAtom(" C  ")));
                ncap.n1Phi = Triple.dihedral(likeC, likeN1N, likeN1Ca, likeN1C);
                if (n1.getNext(model) != null) // psi defined
                {
                    Triple likeN2N = new Triple(state.get(n1.getNext(model).getAtom(" N  ")));
                    ncap.n1Psi = Triple.dihedral(likeN1N, likeN1Ca, likeN1C, likeN2N);
                }
            }
            
            // Phi, psi for Ncap i-1 residue ("N'")
            if (ncap.res.getPrev(model) != null)
            {
                Residue nprime = ncap.res.getPrev(model);
                Triple likeNprimeCa = new Triple(state.get(nprime.getAtom(" CA ")));
                Triple likeNprimeN = new Triple(state.get(nprime.getAtom(" N  ")));
                Triple likeNprimeC = new Triple(state.get(nprime.getAtom(" C  ")));
                if (nprime.getPrev(model) != null) // phi defined
                {
                    Triple likendoubleprimeC = new Triple(state.get(
                        nprime.getPrev(model).getAtom(" C  ")));
                    ncap.nprimePhi = Triple.dihedral(likendoubleprimeC, likeNprimeN, 
                        likeNprimeCa, likeNprimeC);
                }
                ncap.nprimePsi = Triple.dihedral(likeNprimeN, likeNprimeCa, likeNprimeC, likeN);
            }
        }
        catch (driftwood.moldb2.AtomException ae)
        {
            System.err.println("Problem calculating ncap i, i-1, and i+1 phi & psi...");
        }
    }
//}}}

//{{{ setNcapScLengths
//##############################################################################
    public void setNcapScLengths(Model model)
    {
        TreeSet<String> zeroChis = new TreeSet<String>();
        zeroChis.add("GLY");   zeroChis.add("ALA");
        
        TreeSet<String> oneChi = new TreeSet<String>();
        oneChi.add("CYS");     oneChi.add("SER");     oneChi.add("VAL");
        oneChi.add("THR");     oneChi.add("PRO");     oneChi.add("PHE");
        oneChi.add("TYR");
        
        TreeSet<String> twoChis = new TreeSet<String>();
        twoChis.add("TRP");    twoChis.add("HIS");    twoChis.add("LEU");
        twoChis.add("ILE");    twoChis.add("ASP");    twoChis.add("ASN");
        
        TreeSet<String> threeChis = new TreeSet<String>();
        threeChis.add("GLU");  threeChis.add("GLN");  threeChis.add("MET");
        
        TreeSet<String> fourChis = new TreeSet<String>();
        fourChis.add("LYS");   fourChis.add("ARG");
        
        Residue n3 = ncap.res.getNext(model).getNext(model).getNext(model);
        String n3ResType   = n3.getName();            
        String ncapResType = ncap.res.getName();
        //if (verbose) System.err.println("ncapResType: '"+ncapResType+
        //    "'...\tn3ResType: '"+n3ResType+"'...");
        
        // Ncap
        if (ncap != null)
        {
            if (zeroChis.contains(ncapResType))   ncap.ncapNumChis = 0; // shouldn't happen
            if (oneChi.contains(ncapResType))     ncap.ncapNumChis = 1;
            if (twoChis.contains(ncapResType))    ncap.ncapNumChis = 2;
            if (threeChis.contains(ncapResType))  ncap.ncapNumChis = 3; // shouldn't happen
            if (fourChis.contains(ncapResType))   ncap.ncapNumChis = 4; // shouldn't happen
        }
        
        // N3
        if (n3 != null)
        {
            if (zeroChis.contains(n3ResType))     ncap.n3NumChis = 0;
            if (oneChi.contains(n3ResType))       ncap.n3NumChis = 1;
            if (twoChis.contains(n3ResType))      ncap.n3NumChis = 2;
            if (threeChis.contains(n3ResType))    ncap.n3NumChis = 3;
            if (fourChis.contains(n3ResType))     ncap.n3NumChis = 4;
        }
    }
//}}}

//{{{ setTypeAtNcap
//##############################################################################
    /** 
    * Sets type to "alpha" or "3-10" depending on the mc H-bonding at the N-cap.
    * If the i_i+3 (3-10) H-bond exists, we'll call it 3-10 regardless of whether
    * the i_i+4 (alpha) H-bond also exists. Otherwise, it's alpha.
    * 
    * As in HelixBuilder.findHBonds, H-bonds are based on the criteria defined in
    * W. Kabsch and C. Sander (1983) Biopolymers, 22:2577.
    * The basic idea is that the H-bond is accepted if
    * E = 0.42*0.20*332*(1/rON + 1/rCH - 1/rOH - 1/rCN) is less than -0.5 kcal/mol.
    * Atom-atom distances are in Angstroms and E is in kcal/mol.
    * Ideal alignment allows distances up to 5.2 A (O to N);
    * ideal distance allows angles up to 63 degrees.
    * Be careful -- it will try to pick up i to {i, i+1, i+2} "H-bonds".
    */
    public void setTypeAtNcap(Model model, ModelState state)
    {
        try
        {
            if (ncap == null) System.err.println("N-cap null for "+toString()+
                " => not setting helix type (alpha vs. 3-10) at N-cap");
            else
            {
                Residue res3 = ncap.res.getNext(model).getNext(model).getNext(model);
                
                AtomState carbon0 = state.get(ncap.res.getAtom(" C  "));
                AtomState oxygen0 = state.get(ncap.res.getAtom(" O  "));
                AtomState nitrogen3 = state.get(res3.getAtom(" N  "));
                AtomState hydrogen3 = state.get(res3.getAtom(" H  "));
                AtomState nitrogen4 = state.get(res3.getNext(model).getAtom(" N  "));
                AtomState hydrogen4 = state.get(res3.getNext(model).getAtom(" H  "));
                
                // 3-10 (i to i+3)
                double rON = oxygen0.distance(nitrogen3);
                double rCH = carbon0.distance(hydrogen3);
                double rOH = oxygen0.distance(hydrogen3);
                double rCN = carbon0.distance(nitrogen3);
                double energy310   = 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
                
                //// alpha (i to i+4)
                rON        = oxygen0.distance(nitrogen4);
                rCH        = carbon0.distance(hydrogen4);
                rOH        = oxygen0.distance(hydrogen4);
                rCN        = carbon0.distance(nitrogen4);
                double energyAlpha = 27.9*(1/rON + 1/rCH - 1/rOH - 1/rCN);
                
                double bestBondE = -0.6;//-0.5;
                if      (energy310   < bestBondE)  typeAtNcap = "3_10";
                else if (energyAlpha < bestBondE)  typeAtNcap = "alpha";
                // else typeAtNcap remains "(ambiguous)"
                System.err.println("N-cap "+ncap+" is "+typeAtNcap);
            }
        }
        catch (AtomException ae)
        {
            System.err.println("Trouble w/ H-bonding for alpha vs. 3-10 at "+ncap);
        }
    }
//}}}

//{{{ getRes
//##############################################################################
    public Residue getRes(String firstOrLast)
    {
        Residue resToReturn = residues.get(0); 
        if (firstOrLast.equals("first"))
        {
            for (Residue res : residues)
            {
                if (res.getSequenceInteger() < resToReturn.getSequenceInteger())
                    resToReturn = res;
            }
        }
        else if (firstOrLast.equals("last"))
        {
            for (Residue res : residues)
            {
                if (res.getSequenceInteger() > resToReturn.getSequenceInteger())
                    resToReturn = res;
            }
        }
        return resToReturn;
    }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "helix from "+getRes("first")+" to "+getRes("last");
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

