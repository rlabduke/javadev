// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.sc;

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
import driftwood.util.SoftLog;
//}}}
/**
* <code>SidechainIdealizer</code> is a class for working with
* ideal geometry side chains modeled in moldb2.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 14:08:45 EST 2003
*/
public class SidechainIdealizer //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    SidechainAngles2    scAngles;
    Map                 idealSidechainMap;  // Map<Residue.getName(), Map<Atom.getName(), Triple>>
    Map                 idealResMap;        // Map<Residue.getName(), Residue>
    ModelState          idealResState;
    Builder             builder;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public SidechainIdealizer() throws IOException
    {
        builder             = new Builder();
        scAngles            = new SidechainAngles2();
        idealSidechainMap   = loadIdealSidechains();
        loadIdealResides();
    }
//}}}

//{{{ loadIdealSidechains
//##################################################################################################
    /**
    * Opens a PDB of ideal geometry sc from the JAR, and enters coords in the table.
    * Coordinates are translated so that the C-alpha is at (0,0,0).
    * @throws AtomException if any atoms are missing coordinates,
    *   which should never happen with the input we're providing.
    * @return Map&lt;Residue.getName(), Map&lt;Atom.getName(), Triple&gt;&gt;
    */
    Map loadIdealSidechains() throws IOException
    {
        InputStream is = this.getClass().getResourceAsStream("singlesc.pdb");
        if(is == null) throw new IOException("File not found in JAR: singlesc.pdb");
        
        PdbReader   pdbr    = new PdbReader();
        ModelGroup  mg      = pdbr.read(is);
        Model       m       = mg.getFirstModel();
        ModelState  s       = m.getState();
        
        Map rmap = new HashMap();
        for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
        {
            Residue     res     = (Residue)ri.next();
            AtomState   ca      = s.get( res.getAtom(" CA ") );
            Map         amap    = new HashMap();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom        a   = (Atom)ai.next();
                AtomState   as  = s.get(a);
                amap.put(a.getName(), new Triple(as).sub(ca));
            }
            rmap.put(res.getName(), amap);
        }
        return rmap;
    }
//}}}

//{{{ loadIdealResides, getResidueTypes
//##################################################################################################
    void loadIdealResides() throws IOException
    {
        InputStream is = this.getClass().getResourceAsStream("singleres.pdb");
        if(is == null) throw new IOException("File not found in JAR: singleres.pdb");
        
        PdbReader   pdbr    = new PdbReader();
        ModelGroup  mg      = pdbr.read(is);
        Model       m       = mg.getFirstModel();
        idealResState       = m.getState();
        
        idealResMap = new TreeMap();
        for(Iterator iter = m.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            idealResMap.put(r.getName(), r);
        }
    }
    
    /** Returns a collection of all the amino acid codes supported by makeIdealResidue(). */
    public Collection getResidueTypes()
    {
        return Collections.unmodifiableCollection( idealResMap.keySet() );
    }
//}}}

//{{{ idealizeCB
//##################################################################################################
    /**
    * Given a heavy-atom backbone (N, CA, C)
    * this will reconstruct the C-beta and H-alpha(s)
    * if they already exist.
    * The existing sidechain will be rotated about the C-alpha
    * to bring it into the correct position.
    *
    * <p>The reconstruction is fully generic and is not
    * adjusted for the type of residue under consideration.
    * Only bond angles (not lengths) are altered.
    *
    * <p>You typically want {@link #idealizeSidechain(Residue, ModelState)}
    * instead of this function, because it idealizes all sidechain geometry.
    *
    * @return a new state, descended from orig, which contains
    *   new states for all non-mainchain atoms.
    * @throws ResidueException if any of the required backbone atoms
    *   (N, CA, C) are missing.
    */
    public static ModelState idealizeCB(Residue res, ModelState orig)
    {
        Triple t1, t2, ideal = new Triple();
        Builder build = new Builder();
        ModelState modState = new ModelState(orig);
        
        try // looking for AtomExceptions when states are missing
        {
            // These will trigger AtomExceptions if res is missing an Atom
            // because it will try to retrieve the state of null.
            AtomState aaN   = orig.get( res.getAtom(" N  ") );
            AtomState aaCA  = orig.get( res.getAtom(" CA ") );
            AtomState aaC   = orig.get( res.getAtom(" C  ") );
            
            // Build an ideal C-beta and swing the side chain into place
            Atom cBeta = res.getAtom(" CB ");
            if(cBeta != null)
            {
                // Construct ideal C-beta
                t1 = build.construct4(aaN, aaC, aaCA, 1.536, 110.4, 123.1);
                t2 = build.construct4(aaC, aaN, aaCA, 1.536, 110.6, -123.0);
                ideal.likeMidpoint(t1, t2);
                
                // Construct rotation to align actual and ideal
                AtomState aaCB = orig.get(cBeta);
                double theta = Triple.angle(ideal, aaCA, aaCB);
                //SoftLog.err.println("Angle of correction: "+theta);
                t1.likeNormal(ideal, aaCA, aaCB).add(aaCA);
                Transform xform = new Transform().likeRotation(aaCA, t1, theta);
                
                // Apply the transformation
                for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
                {
                    Atom        atom    = (Atom)iter.next();
                    String      name    = atom.getName();
                    
                    // Transform everything that's not mainchain
                    if( !( name.equals(" N  ") || name.equals(" H  ")
                        || name.equals(" CA ") || name.equals(" HA ")
                        || name.equals("1HA ") || name.equals("2HA ")
                        || name.equals(" C  ") || name.equals(" O  ")) )
                    {
                        // Clone the original state, move it, and insert it into our model
                        AtomState   s1      = orig.get(atom);
                        AtomState   s2      = (AtomState)s1.clone();
                        xform.transform(s1, s2);
                        modState.add(s2);
                    }//if atom is not mainchain
                }//for each atom in the residue
            }//rebuilt C-beta
            
            
            // Reconstruct alpha hydrogens
            // These are easier -- just compute the position and make it so!
            Atom hAlpha = res.getAtom(" HA ");
            if(hAlpha != null)
            {
                AtomState s1 = orig.get(hAlpha);
                AtomState s2 = (AtomState)s1.clone();
                t1 = build.construct4(aaN, aaC, aaCA, 1.100, 107.9, -118.3);
                t2 = build.construct4(aaC, aaN, aaCA, 1.100, 108.1, 118.2);
                s2.likeMidpoint(t1, t2).sub(aaCA).unit().mult(1.100).add(aaCA);
                modState.add(s2);
            }
            
            // Now for glycine, and then we're done
            hAlpha = res.getAtom("1HA ");
            if(hAlpha != null)
            {
                AtomState s1 = orig.get(hAlpha);
                AtomState s2 = (AtomState)s1.clone();
                t1 = build.construct4(aaN, aaC, aaCA, 1.100, 109.3, -121.6);
                t2 = build.construct4(aaC, aaN, aaCA, 1.100, 109.3, 121.6);
                s2.likeMidpoint(t1, t2).sub(aaCA).unit().mult(1.100).add(aaCA);
                modState.add(s2);
            }
            hAlpha = res.getAtom("2HA ");
            if(hAlpha != null)
            {
                AtomState s1 = orig.get(hAlpha);
                AtomState s2 = (AtomState)s1.clone();
                t1 = build.construct4(aaN, aaC, aaCA, 1.100, 109.3, 121.6);
                t2 = build.construct4(aaC, aaN, aaCA, 1.100, 109.3, -121.6);
                s2.likeMidpoint(t1, t2).sub(aaCA).unit().mult(1.100).add(aaCA);
                modState.add(s2);
            }
        }
        catch(AtomException ex)
        { throw new ResidueException("C-beta idealize failed: "+ex.getMessage()); }
        
        return modState;
    }
//}}}

//{{{ *** Dave's notes on CB idealization ***
//##################################################################################################
/*
Idealizing the Cb:dist,angle,dihedral, ideal-tau
              dist, angleCAB,dihedralNCAB,angleideal
              dist, angleNAB,dihedralCNAB,angleideal
Ala, from  C: 1.536 , 110.1,  122.9,    111.2 (Ala)(i.e., N,C,Ca,Cb)
Ala, from  N: 1.536 , 110.6, -122.6,    111.2      (i.e., C,N,Ca,Cb)
Pro, from  C: 1.530 , 112.2,  115.1,    111.8 (Pro)
Pro, from  N: 1.530 , 103.0, -120.7,    111.8
Val, from  C: 1.540 , 109.1,  123.4,    111.2 (Val,Thr,Ile)
Val, from  N: 1.540 , 111.5, -122.0,    111.2
Leu, from  C: 1.530 , 110.1,  122.8,    111.2 (Leu,Met,Phe,Ser,...others)
Leu, from  N: 1.530 , 110.5, -122.6,    111.2
Gly, form  C: 1.100 , 109.3,  121.6,    112.5 (Gly HA1)
Gly, form  C: 1.100 , 109.3, -121.6,    112.5

atom abreviation convention: N==N, A==Ca, C==C, B==Cb
construct fourth point from N C Ca -- betaNCAB
construct fourth point form C N Ca -- betaCNAB

average: (betaNCAB + betaCNAB)/2 ==> betaxyz
recompute distance of averaged beta position:
   adjust betaxyz to be at ideal distance, i.e. scale Ca--betaxyz distance.
compute the deviation from ideal beta position:
   distance between original Cb position and ideal beta position.
dihedral: N---CA---CBideal---CBactual (from N---Ca direction)
   dihedralNABB = dihedral4pt(N,Ca,Cbideal,Cborig);

So dihedral is from N---Ca direction and the plane splitting Tau is at ~60 deg.
The pair of computed Cb's will lie on either side of this plane.

Convex side of Tau is the (-120 dihedral) side of the Cbetadeviation.
As Tau gets larger, the computed Cb's  get pushed across the plane from
either side since the canonical N Ca Cb and C Ca Cb angles are larger than the
angle to a line in the splitting plane at a given dihedral to the N C Ca plane.
So prekin's ideal Cb is pushed toward the convex side of Tau, and
prekin's ideal Cb has smaller N Ca Cb and C Ca Cb angles than canonical.
In order to restore canonical angles, the Cb would be pushed to make the
dihedral angle from the N C Ca plane larger, i.e. pushed even further toward
the convex side of Tau.

Concave side of Tau is the (+60 dihedral) side of the Cbetadeviation.
As Tau gts smaller, the computed Cb's do not meet the plane from
either side since the canonical N Ca Cb and C Ca Cb angles are smaller than the
angle to a line in the splitting plane at a given dihedral to the N C Ca plane.
So prekin's ideal Cb is pushed toward the cancave side of Tau, and
prekin's ideal Cb has larger N Ca Cb and C Ca Cb angles than canonical.
In order to restore canonical angles, the Cb would be pushed to make the
dihedral angle from the N C Ca plane smaller, i.e. pushed even further toward
the concave side of Tau.

If refinement programs forces the angles C,Ca,Cb and N,Ca,Cb to be canonical,
then the deposited coord for Cb would be offset in these ways from
prekin's ideal Cbeta.

Note:
If N Ca C always stay in the same plane, then as Tau changes from 180 to 0
the angles N Ca Cb and C Ca Cb go from 90 to a maximum.
In as much as the only difference in geometry for different Tau angles is just
the simple opening angle of the simple angle Tau, then those atoms do stay
in the same plane, i.e. the effective axis of the Tau angle change is
perpendicular to the N Ca C plane and NOT along the Ca---Cb vector.
*/
//}}}

//{{{ [BROKEN] idealizeSidechain
//##################################################################################################
    /**
    * Idealizes all aspects of sidechain geometry (bond lengths and angles).
    * Dihedrals are preserved from the original model.
    * All heavy atoms must be present, but H's are optional.
    * This method will not create missing atoms, only move existing ones.
    * It returns <code>start</code> if the residue is of unknown type.
    *
    * <p>Doesn't work right for Ala Hs -- randomly oriented.
    * /
    public ModelState idealizeSidechain(Residue res, ModelState start)
    {
        Map atomMap = (Map)idealSidechainMap.get(res.getName());
        if(atomMap == null) // a residue we don't recognize
            return start;
        
        // Save initial conformation. Chis only b/c we might lack H's.
        double[] chis = scAngles.measureChiAngles(res, start);
        //DEBUG: for(int i = 0; i < chis.length; i++) SoftLog.err.println("chi"+(i+1)+"="+chis[i]);
        
        // This step corrects bond lengths and angles,
        // but leaves Ca-Cb oriented randomly.
        ModelState  end = new ModelState(start);
        AtomState   ca  = start.get( res.getAtom(" CA ") );
        for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
        {
            Atom    a   = (Atom)iter.next();
            Triple  t   = (Triple)atomMap.get(a.getName());
            if(t != null) // we have coords for this atom
            {
                AtomState   s1  = start.get(a);
                AtomState   s2  = (AtomState)s1.clone();
                s2.like(t).add(ca); // t has Ca at 0,0,0
                end.add(s2);
            }
        }
        
        // This step corrects Ca-Cb orientation.
        end = idealizeCB(res, end);
        
        // Restore original orientation (chi angles)
        end = scAngles.setChiAngles(res, end, chis);
        
        return end;
    }*/
//}}}

//{{{ idealizeSidechain
//##################################################################################################
    /**
    * Idealizes all aspects of sidechain geometry (bond lengths and angles).
    * Dihedrals are preserved from the original model.
    * All heavy atoms must be present, but H's are optional.
    * This method will not create missing atoms, only move existing ones.
    * It returns <code>start</code> if the residue is of unknown type.
    */
    public ModelState idealizeSidechain(Residue res, ModelState start)
    {
        Residue idealRes = (Residue) idealResMap.get(res.getName());
        if(idealRes == null) // a residue we don't recognize
            return start;
        
        // Save initial conformation. Chis only b/c we might lack H's.
        // Actually, we can do all angles and ignore any NaN's we get.
        double[] chis = scAngles.measureAllAngles(res, start);
        //DEBUG: for(int i = 0; i < chis.length; i++) SoftLog.err.println("chi"+(i+1)+"="+chis[i]);
        
        try
        {
            ModelState  end = new ModelState(start);
            AtomState   ca1 = start.get( res.getAtom(" CA ") );
            AtomState   n1  = start.get( res.getAtom(" N  ") );
            AtomState   c1  = start.get( res.getAtom(" C  ") );
            AtomState   ca2 = idealResState.get( idealRes.getAtom(" CA ") );
            AtomState   n2  = idealResState.get( idealRes.getAtom(" N  ") );
            AtomState   c2  = idealResState.get( idealRes.getAtom(" C  ") );
            Transform xform = builder.dock3on3(ca1, n1, c1, ca2, n2, c2);
            for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
            {
                try 
                {
                    Atom    a1  = (Atom)iter.next();
                    String  nm  = a1.getName();
                    Atom    a2  = idealRes.getAtom(nm);
                    if(!(nm.equals(" N  ") || nm.equals(" H  ") || nm.equals(" C  ") || nm.equals(" O  ")))
                    {
                        AtomState   s1  = start.get(a1);
                        AtomState   s2  = idealResState.get(a2);
                        AtomState   s3  = (AtomState)s1.clone();
                        xform.transform(s2, s3); // transforms it into position
                        end.add(s3);
                    }
                }
                catch(AtomException ex) {} // no action
            }
            
            // Correct for non-ideal tau angle
            end = idealizeCB(res, end);
            
            // Restore original orientation (chi angles)
            end = scAngles.setAllAngles(res, end, chis);
            
            return end;
        }
        catch(AtomException ex) { return start; }
    }
//}}}

//{{{ makeIdealResidue
//##################################################################################################
    /**
    * Creates an ideal geometry residue at an arbitrary position/orientation.
    * @param chain          the chain ID. Not zero. Space (' ') is a good default.
    * @param segment        the seg ID. Not null. Empty string ("") is a good default.
    * @param seqNum         the number in sequence. May have any value.
    * @param insCode        the insertion code. Not zero. Space (' ') is a good default.
    * @param resName        one of the three letter codes returned by getResidueTypes().
    * @param outputState    a ModelState that will have the new AtomStates added to it.
    * @return the new residue of the specified type.
    * @throws IllegalArgumentException if aaType is not a recognized amino acid code.
    */
    public Residue makeIdealResidue(char chain, String segment, int seqNum, char insCode, String resName, ModelState outputState)
    {
        // Get template
        if(!idealResMap.containsKey(resName))
            throw new IllegalArgumentException("'"+resName+"' is not a known amino acid");
        Residue templateRes = (Residue) idealResMap.get(resName);
        
        // Copy it, with a new name
        Residue newRes = new Residue(templateRes, chain, segment, seqNum, insCode, resName);
        newRes.cloneStates(templateRes, idealResState, outputState);
        
        return newRes;
    }
//}}}

//{{{ dockResidue
//##################################################################################################
    /**
    * Docks the backbone of one residue onto that of another.
    * All backbone atoms are adjusted to match the original exactly,
    * then the CB position is idealized using idealizeCB().
    * Neither of the original states is modified.
    * @throws   AtomException if the N, CA, or C atom is missing in from or to.
    */
    public ModelState dockResidue(Residue mobRes, ModelState mob, Residue refRes, ModelState ref)
    {
        // Reposition all atoms
        Transform xform = builder.dock3on3(
            ref.get(refRes.getAtom(" CA ")),
            ref.get(refRes.getAtom(" N  ")),
            ref.get(refRes.getAtom(" C  ")),
            mob.get(mobRes.getAtom(" CA ")),
            mob.get(mobRes.getAtom(" N  ")),
            mob.get(mobRes.getAtom(" C  "))
        );

        ModelState out = new ModelState(mob);
        for(Iterator iter = mobRes.getAtoms().iterator(); iter.hasNext(); )
        {
            Atom        a   = (Atom) iter.next();
            AtomState   s1  = mob.get(a);
            AtomState   s2  = (AtomState) s1.clone();
            out.add(s2);
            xform.transform(s2);
        }
        
        // Reposition backbone atoms
        out.get(mobRes.getAtom(" N  ")).like(ref.get(refRes.getAtom(" N  ")));
        out.get(mobRes.getAtom(" CA ")).like(ref.get(refRes.getAtom(" CA ")));
        out.get(mobRes.getAtom(" C  ")).like(ref.get(refRes.getAtom(" C  ")));
        try { out.get(mobRes.getAtom(" O  ")).like(ref.get(refRes.getAtom(" O  "))); } catch(AtomException ex) {}
        try { out.get(mobRes.getAtom(" H  ")).like(ref.get(refRes.getAtom(" H  "))); } catch(AtomException ex) {}
        
        return idealizeCB(mobRes, out);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

