// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;

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
* <code>RnaIdealizer</code> is a class for working with
* ideal geometry side chains modeled in moldb2.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 14:08:45 EST 2003
*/
public class RnaIdealizer //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    //SidechainAngles2    scAngles;
    Map                 idealSidechainMap;  // Map<Residue.getName(), Map<Atom.getName(), Triple>>
    Map                 idealResMap;        // Map<filename??, Model>
    //Map                 idealResMapv23;
    //ModelState          idealResState;
    //ModelState          idealResStatev23;
    Builder             builder;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public RnaIdealizer() throws IOException
    {
        builder             = new Builder();
        //scAngles            = new SidechainAngles2();
        //idealSidechainMap   = loadIdealSidechains(); //doesn't currently seem to be used.
        //loadIdealResidues();
        //loadIdealResiduesv23();
        idealResMap = new TreeMap();
        loadIdealBackbones("rna32.pdb");
        loadIdealBackbones("rna33.pdb");
        loadIdealBackbones("rna22.pdb");
        loadIdealBackbones("rna23.pdb");
    }
//}}}

  //{{{ loadIdealBackbones
  public void loadIdealBackbones(String filename) throws IOException {
    InputStream is = this.getClass().getResourceAsStream(filename);
    if(is == null) throw new IOException("File not found in JAR: "+filename);
    
    PdbReader       pdbr    = new PdbReader();
    CoordinateFile  cf      = pdbr.read(is);
    Model           m       = cf.getFirstModel();
    //idealResState           = m.getState();
    
    idealResMap.put(filename, m);
  }
  //}}}

//{{{ loadIdealSidechains
//##################################################################################################
    /**
    * Opens a PDB of ideal geometry sc from the JAR, and enters coords in the table.
    * Coordinates are translated so that the C-alpha is at (0,0,0).
    * @return Map&lt;Residue.getName(), Map&lt;Atom.getName(), Triple&gt;&gt;
    */
    //Map loadIdealSidechains() throws IOException
    //{
    //    InputStream is = this.getClass().getResourceAsStream("singlesc.pdb");
    //    if(is == null) throw new IOException("File not found in JAR: singlesc.pdb");
    //    
    //    PdbReader       pdbr    = new PdbReader();
    //    CoordinateFile  cf      = pdbr.read(is);
    //    Model           m       = cf.getFirstModel();
    //    ModelState      s       = m.getState();
    //    
    //    Map rmap = new HashMap();
    //    for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
    //    {
    //        try
    //        {
    //            Residue     res     = (Residue)ri.next();
    //            AtomState   ca      = s.get( res.getAtom(" CA ") );
    //            Map         amap    = new HashMap();
    //            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
    //            {
    //                Atom        a   = (Atom)ai.next();
    //                AtomState   as  = s.get(a);
    //                amap.put(a.getName(), new Triple(as).sub(ca));
    //            }
    //            rmap.put(res.getName(), amap);
    //        }
    //        catch(AtomException ex) { ex.printStackTrace(); }
    //    }
    //    return rmap;
    //}
//}}}

//{{{ loadIdealResides, getResidueTypes
//##################################################################################################
    //void loadIdealResidues() throws IOException
    //{
    //    InputStream is = this.getClass().getResourceAsStream("singleres.pdb");
    //    if(is == null) throw new IOException("File not found in JAR: singleres.pdb");
    //    
    //    PdbReader       pdbr    = new PdbReader();
    //    CoordinateFile  cf      = pdbr.read(is);
    //    Model           m       = cf.getFirstModel();
    //    idealResState           = m.getState();
    //    
    //    idealResMap = new TreeMap();
    //    for(Iterator iter = m.getResidues().iterator(); iter.hasNext(); )
    //    {
    //        Residue r = (Residue) iter.next();
    //        idealResMap.put(r.getName(), r);
    //    }
    //}
    //
    //void loadIdealResiduesv23() throws IOException
    //{
    //    InputStream is = this.getClass().getResourceAsStream("singleres-v23.pdb");
    //    if(is == null) throw new IOException("File not found in JAR: singleres.pdb");
    //    
    //    PdbReader       pdbr    = new PdbReader();
    //    CoordinateFile  cf      = pdbr.read(is);
    //    Model           m       = cf.getFirstModel();
    //    idealResStatev23           = m.getState();
    //    
    //    idealResMapv23 = new TreeMap();
    //    for(Iterator iter = m.getResidues().iterator(); iter.hasNext(); )
    //    {
    //        Residue r = (Residue) iter.next();
    //        idealResMapv23.put(r.getName(), r);
    //    }
    //}
    //
    ///** Returns a collection of all the amino acid codes supported by makeIdealResidue(). */
    //public Collection getResidueTypes()
    //{
    //    return Collections.unmodifiableCollection( idealResMap.keySet() );
    //}
//}}}

//{{{ idealizeSidechain
//##################################################################################################
    /**
    * Idealizes all aspects of sidechain geometry (bond lengths and angles).
    * Dihedrals are preserved from the original model.
    * All heavy atoms must be present, but H's are optional.
    * This method will not create missing atoms, only move existing ones.
    * It also doesn't move atoms that aren't present in the idealized residues;
    * this caused issues with the PDB format change.
    * It returns <code>start</code> if the residue is of unknown type.
    *//*
    public ModelState idealizeSidechain(Residue res, ModelState start)
    {
        Residue idealRes = (Residue) idealResMap.get(res.getName());
        Residue idealResv23 = (Residue) idealResMapv23.get(res.getName());
        if((idealRes == null)||(idealResv23 == null)) // a residue we don't recognize
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
                        AtomState   s2;
                        if (a2 != null) {
                          s2 = idealResState.get(a2);
                        } else {
                          s2 = idealResStatev23.get(idealResv23.getAtom(nm)); //should only be called on atoms that got changed.
                        }
                        AtomState   s3  = (AtomState)s1.clone();
                        //System.out.println("moving "+s2.toString()+" onto "+s3.toString());
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
    */
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
    * @param doPdbv3        boolean for making a residue with Pdbv3 names (vs pdbv2.3 names)
    * @return the new residue of the specified type.
    * @throws IllegalArgumentException if aaType is not a recognized amino acid code.
    */
    /*
    public Residue makeIdealResidue(String chain, String segment, String seqNum, String insCode, String resName, ModelState outputState, boolean doPdbv3)
    {
        // Get template
        Map resMap;
        ModelState resState;
        if (doPdbv3) {
          resMap = idealResMap;
          resState = idealResState;
        } else {
          resMap = idealResMapv23;
          resState = idealResStatev23;
        }
          
        if(!resMap.containsKey(resName))
            throw new IllegalArgumentException("'"+resName+"' is not a known amino acid");
        Residue templateRes = (Residue) resMap.get(resName);
        
        // Copy it, with a new name
        try
        {
            Residue newRes = new Residue(templateRes, chain, segment, seqNum, insCode, resName);
            newRes.cloneStates(templateRes, resState, outputState);
            return newRes;
        }
        catch(AtomException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
    */
    /** legacy version that defaults to using old (pdb v2.3) format **/
    /*
    public Residue makeIdealResidue(String chain, String segment, String seqNum, String insCode, String resName, ModelState outputState)
    {
      return makeIdealResidue(chain, segment, seqNum, insCode, resName, outputState, false);
    }
    */
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
    public ModelState dockResidue(Residue mobRes, ModelState mob, Residue refRes, ModelState ref) throws AtomException
    {
        // Reposition all atoms
        Transform xform = builder.dock3on3(
            ref.get(refRes.getAtom(" P  ")),
            ref.get(refRes.getAtom(" C1'")),
            ref.get(refRes.getAtom(" O3'")),
            mob.get(mobRes.getAtom(" P  ")),
            mob.get(mobRes.getAtom(" C1'")),
            mob.get(mobRes.getAtom(" O3'"))
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
        out.get(mobRes.getAtom(" P  ")).like(ref.get(refRes.getAtom(" P  ")));
        out.get(mobRes.getAtom(" C1'")).like(ref.get(refRes.getAtom(" C1'")));
        out.get(mobRes.getAtom(" O3'")).like(ref.get(refRes.getAtom(" O3'")));
        //try { out.get(mobRes.getAtom(" O  ")).like(ref.get(refRes.getAtom(" O  "))); } catch(AtomException ex) {}
        //try { out.get(mobRes.getAtom(" H  ")).like(ref.get(refRes.getAtom(" H  "))); } catch(AtomException ex) {}
        
        return out;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

