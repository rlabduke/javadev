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
import driftwood.util.*;
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
        loadIdealBackbones("rna32H.pdb");
        loadIdealBackbones("rna33H.pdb");
        loadIdealBackbones("rna22H.pdb");
        loadIdealBackbones("rna23H.pdb");
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
    
    idealResMap.put(filename.substring(3, 5), m);
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
    /** Returns a collection of all the amino acid codes supported by makeIdealResidue(). */
    public Collection getPuckerStates()
    {
        return Collections.unmodifiableCollection( idealResMap.keySet() );
    }
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
    * @param residues       Collection of 2 residues.
    * @param puckers        string of the pucker types wanted (33, 32, 23, or 22).
    * @param outputState    a ModelState that will have the new AtomStates added to it.
    * @param doPdbv3        boolean for making a residue with Pdbv3 names (vs pdbv2.3 names)
    * @return Collection of new residues of the specified pucker.
    * @throws IllegalArgumentException if aaType is not a recognized amino acid code.
    */
    public ModelState makeIdealResidue(ModelState origState, Collection residues, String puckers,  boolean doPdbv3) throws AtomException
    {
        // Get template
        Model idealModel = (Model)idealResMap.get(puckers);
        ModelState idealState = idealModel.getState();
        ArrayList origResidues = new ArrayList(residues);
        //Residue dRes = (Residue) origResidues.get(dockRes);
        // first dock ideal on residue
        ModelState dockedIdealState = dock3on3Residues(idealModel.getResidues(), idealState, origResidues, origState);
        //System.out.println("docked\n"+dockedIdealState.debugStates());
        //debugModelState(residues, origState, "orig.pdb");
        ArrayList idealResidues = new ArrayList(idealModel.getResidues());
        
        // tranform all base atoms based on glyc bond
        ModelState dockedBases = dockBases(origResidues, origState, idealResidues, dockedIdealState);
        //System.out.println("docked\n"+dockedBases.debugStates());
        //debugModelState(residues, dockedBases, "dockedbase.pdb");
        
        //System.out.println("orig\n"+origState.debugStates());
        // change all atomstates in original to ideal (since ideal don't have sidechains, this only does backbone)
        ModelState idealOrigState = new ModelState(dockedBases);
        for (int i = 0; i < idealResidues.size(); i++) {
          Residue ideal = (Residue)idealResidues.get(i);
          Residue orig = (Residue)origResidues.get(i);
          Iterator origAtoms = orig.getAtoms().iterator();
          while (origAtoms.hasNext()) {
            Atom origAt = (Atom) origAtoms.next();
            Atom idealAt = ideal.getAtom(origAt.getName());
            if (idealAt != null) { // since ideal residues don't have base atoms
              AtomState idealAtSt = dockedIdealState.get(idealAt);
              AtomState origAtSt = idealOrigState.get(origAt);
              origAtSt.setXYZ(idealAtSt.getX(), idealAtSt.getY(), idealAtSt.getZ());
              idealOrigState.add(origAtSt);
            }
          }
        }
        //debugModelState(residues, idealOrigState, "changed.pdb");
        //System.out.println("idealorig\n"+idealOrigState.debugStates());
        
        
        return idealOrigState;
    }
//}}}

//{{{ dock3on3Residues
//##################################################################################################
    /**
    * Docks the RNA suite on the P and oxygens using 3 point docking.  The P is moved 
    * to match the reference P position exactly.
    * Neither of the original states is modified.
    * @throws   AtomException if the N, CA, or C atom is missing in from or to.
    */
    public ModelState dock3on3Residues(Collection mobResidues, ModelState mob, Collection refResidues, ModelState ref) throws AtomException
    {
      ArrayList mobResList = new ArrayList(mobResidues);
      Residue mobRes0 = (Residue) mobResList.get(0);
      Residue mobRes1 = (Residue) mobResList.get(1);
      ArrayList refResList = new ArrayList(refResidues);
      Residue refRes0 = (Residue) refResList.get(0);
      Residue refRes1 = (Residue) refResList.get(1);
      // Reposition all atoms
      Transform xform = builder.dock3on3(
        ref.get(refRes1.getAtom(" P  ")),
        ref.get(refRes1.getAtom(" O5'")),
        ref.get(refRes0.getAtom(" O3'")),
        mob.get(mobRes1.getAtom(" P  ")),
        mob.get(mobRes1.getAtom(" O5'")),
        mob.get(mobRes0.getAtom(" O3'"))
        );
      
      ModelState out = new ModelState(mob);
      for (Iterator allRes = mobResList.iterator(); allRes.hasNext(); ) {
        Residue mobRes = (Residue) allRes.next();
        for(Iterator iter = mobRes.getAtoms().iterator(); iter.hasNext(); )
        {
          Atom        a   = (Atom) iter.next();
          AtomState   s1  = mob.get(a);
          AtomState   s2  = (AtomState) s1.clone();
          out.add(s2);
          xform.transform(s2);
        }
      }
      
      // Reposition backbone atoms
      //out.get(mobRes.getAtom(" P  ")).like(ref.get(refRes.getAtom(" P  ")));
      //out.get(mobRes.getAtom(" C1'")).like(ref.get(refRes.getAtom(" C1'")));
      //out.get(mobRes.getAtom(" O3'")).like(ref.get(refRes.getAtom(" O3'")));
      //try { out.get(mobRes.getAtom(" O  ")).like(ref.get(refRes.getAtom(" O  "))); } catch(AtomException ex) {}
      //try { out.get(mobRes.getAtom(" H  ")).like(ref.get(refRes.getAtom(" H  "))); } catch(AtomException ex) {}
      
      return out;
    }
    //}}}
    
    //{{{ dockResidues
    public ModelState dockResidues(Collection mobResidues, ModelState mob, Collection refResidues, ModelState ref, Collection atoms) throws AtomException
    {
      //debugModelState(mobResidues, mob, "mobile.pdb");
      //debugModelState(refResidues, ref, "reference.pdb");
      ArrayList mobResList = new ArrayList(mobResidues);
      Residue mobRes0 = (Residue) mobResList.get(0);
      Residue mobRes1 = (Residue) mobResList.get(1);
      ArrayList refResList = new ArrayList(refResidues);
      Residue refRes0 = (Residue) refResList.get(0);
      Residue refRes1 = (Residue) refResList.get(1);
      ArrayList atomsList = new ArrayList(atoms);
      Collection atoms0 = (Collection) atomsList.get(0);
      //System.out.println(atoms0);
      Collection atoms1 = (Collection) atomsList.get(1);
      //System.out.println(atoms1);
      ArrayList<AtomState> mobAtSts = new ArrayList<AtomState>();
      ArrayList<AtomState> refAtSts = new ArrayList<AtomState>();
      for (Iterator iter0 = atoms0.iterator(); iter0.hasNext(); ) {
        String atom = (String) iter0.next();
        AtomState refAtSt = ref.get(refRes0.getAtom(atom));
        AtomState mobAtSt = mob.get(mobRes0.getAtom(atom));
        mobAtSts.add(mobAtSt);
        refAtSts.add(refAtSt);
      }
      for (Iterator iter1 = atoms1.iterator(); iter1.hasNext(); ) {
        String atom = (String) iter1.next();
        AtomState refAtSt = ref.get(refRes1.getAtom(atom));
        AtomState mobAtSt = mob.get(mobRes1.getAtom(atom));
        mobAtSts.add(mobAtSt);
        refAtSts.add(refAtSt);
      }
      //System.out.println("mobile: ");
      //for (AtomState as : mobAtSts) System.out.println(as.format(new DecimalFormat("0.000")));
      //System.out.println("ref: ");
      //for (AtomState as : refAtSts) System.out.println(as.format(new DecimalFormat("0.000")));
      // Reposition all atoms
      SuperPoser poser = new SuperPoser(refAtSts.toArray(new AtomState[refAtSts.size()]), mobAtSts.toArray(new AtomState[mobAtSts.size()]));
      Transform xform = poser.superpos();
      //System.out.println(xform);
      
      ModelState out = new ModelState(mob);
      for (Iterator allRes = mobResList.iterator(); allRes.hasNext(); ) {
        Residue mobRes = (Residue) allRes.next();
        for(Iterator iter = mobRes.getAtoms().iterator(); iter.hasNext(); )
        {
          Atom        a   = (Atom) iter.next();
          AtomState   s1  = mob.get(a);
          AtomState   s2  = (AtomState) s1.clone();
          out.add(s2);
          xform.transform(s2);
        }
      }
      
      // Reposition backbone atoms
      //out.get(mobRes.getAtom(" P  ")).like(ref.get(refRes.getAtom(" P  ")));
      //out.get(mobRes.getAtom(" C1'")).like(ref.get(refRes.getAtom(" C1'")));
      //out.get(mobRes.getAtom(" O3'")).like(ref.get(refRes.getAtom(" O3'")));
      //try { out.get(mobRes.getAtom(" O  ")).like(ref.get(refRes.getAtom(" O  "))); } catch(AtomException ex) {}
      //try { out.get(mobRes.getAtom(" H  ")).like(ref.get(refRes.getAtom(" H  "))); } catch(AtomException ex) {}
      
      return out;
    }
//}}}

  //{{{ dockBases
  public ModelState dockBases(Collection mobResidues, ModelState mob, Collection refResidues, ModelState ref) throws AtomException
  {
    if (mobResidues.size() != refResidues.size()) SoftLog.err.println("Must have the same number of residues in both inputs to dock bases!");
    else {
      ArrayList mobList = new ArrayList(mobResidues);
      ArrayList refList = new ArrayList(refResidues);
      ModelState out = new ModelState(mob);
      for (int i = 0; i < mobList.size(); i++) {
        Residue mobRes = (Residue) mobList.get(i);
        Residue refRes = (Residue) refList.get(i);
        //System.out.println(ref.get(refRes.getAtom(" C1'")));
        //System.out.println(ref.get(getBaseBondAtom(refRes)));
        //System.out.println(ref.get(refRes.getAtom(" O4'")));
        //System.out.println(mob.get(mobRes.getAtom(" C1'")));
        //System.out.println(mob.get(getBaseBondAtom(mobRes)));
        //System.out.println(mob.get(mobRes.getAtom(" O4'")));
        Transform xform = builder.dock3on3(
          ref.get(refRes.getAtom(" C1'")),
          ref.get(getBaseBondAtom(refRes)),
          ref.get(refRes.getAtom(" O4'")),
          mob.get(mobRes.getAtom(" C1'")),
          mob.get(getBaseBondAtom(mobRes)),
          mob.get(mobRes.getAtom(" O4'"))
          );

        for(Iterator iter = mobRes.getAtoms().iterator(); iter.hasNext(); )
        {
          Atom        a   = (Atom) iter.next();
          AtomState   s1  = out.get(a);
          //System.out.println(s1);
          //AtomState   s2  = (AtomState) s1.clone();
          //out.add(s2);
          if (!isBackboneAtom(a)) {
            //System.out.println("transforming");
            Tuple3 transed = xform.transform(s1);
            s1.setXYZ(transed.getX(), transed.getY(), transed.getZ());
            //System.out.println(s1);
            out.add(s1);
          }
          
        }
      }
      return out;
    }
    return null;
  }
  //}}}
  
  //{{{ isBackboneAtom
  public boolean isBackboneAtom(Atom a) {
    return (
      (a.getName().indexOf("'") > -1)||
      (a.getName().indexOf("*") > -1)||
      (a.getName().equals(" P  "))||
      (a.getName().equals(" OP1"))||
      (a.getName().equals(" OP2")));
  }
  //}}}
  
  //{{{ getBaseBondAtom
  public Atom getBaseBondAtom(Residue res) throws AtomException {
    Atom at = null;
    at = res.getAtom(" N9 ");
    if (at == null)
      at = res.getAtom(" N1 ");
    if (at == null) {
      Iterator atoms = res.getAtoms().iterator();
      while (atoms.hasNext()||(at != null)) {
        Atom test = (Atom) atoms.next();
        if (!isBackboneAtom(test)) at = test;
      }
    }
    if (at == null) throw new AtomException(res.getName()+"does not seem to have base atoms");
    return at;
  }
  //}}}

  //{{{ debugModelState
  /** Writes a debug PDB into the current working directory.  If the PDB exists,
  *   then this will add a number to the filename to make a new file.
  * @param residues    a collection of Residues
  * @param state       a ModelState with corresponding state info.
  * @param filename    a filename, with extension (i.e. .pdb).
  */
  public void debugModelState(Collection residues, ModelState state, String fileName) {
    try {
      File outFile = new File(System.getProperty("user.dir")+"-"+fileName);
      int counter = 0;
      while (outFile.exists()) {
        String[] split = Strings.explode(fileName, '.');
        String newName = "";
        for (int i = 0; i < split.length-1; i++) {
          newName = newName + split[i];
        }
        newName = newName + "." + counter +"." + split[split.length-1];
        outFile = new File(System.getProperty("user.dir")+"-"+newName);
        counter++;
      }
      System.out.println("writing file to "+outFile.toString());
      PdbWriter writer = new PdbWriter(outFile);
      writer.writeResidues(residues, state);
      writer.close();
    } catch (IOException ie) {
      ie.printStackTrace();
    }
  }
  //}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

