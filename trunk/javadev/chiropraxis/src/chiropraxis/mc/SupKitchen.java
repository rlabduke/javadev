// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.moldb2.*;
import driftwood.util.*;
import molikin.logic.*;
import molikin.Quickin;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
//}}}
/**
* <code>SupKitchen</code> "cooks up" SUPerpositions (*groan*) of related protein 
* structures and analyzes the resulting ensembles via principal components.
*
* It tries to construct ensembles intelligently when the sequences are different, 
* there are insertion loops in the reference and/or some of the other models, etc.
*
* A *lot* of this code comes from Ian's chiropraxis.mc.SubImpose.
*
* REMAINING TO-DO:
*  - regularize bb after distortion?
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu June 25 2009
*/
public class SupKitchen //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df  = new DecimalFormat("###0.00");
    DecimalFormat df2 = new DecimalFormat("###0.0");
    public final String SELECT_CA          = "(!resHOH)&(atom_CA_)";
    public final String SELECT_BB_HEAVY    = "(!resHOH)&(atom_CA_|atom_N__|atom_C__|atom_O__)";
    public final String SELECT_BB_HEAVY_CB = "(!resHOH)&(atom_CA_|atom_N__|atom_C__|atom_O__|atom_CB_)";
//}}}

//{{{ Variable definitions
//##############################################################################
    
    // INPUT
    boolean  verbose = false;
    String   mdlFilename; // could be for mdls dir or mdls file
    File     mdlDir;
    File     mdlFile;
    String   refFilename;
    File     refFile;
    String   title;
    String   superimpose;
    boolean  splitChains = true;
    boolean  distort = true;
    double   leskSieve = 2.0;  // max acceptable RMSD
    
    // ENSEMBLE
    /** Holds models with however-many chains are present from the ref structure. */
    CoordinateFile     refCoordFile;
    /** Holds models with >=1 chains (depending on user's options) from other structures. 
    * Could contain just one CoordinateFile with multiple Models if NMR-like structure. */
    CoordinateFile[]   mdlCoordFiles;
    /** Holds models from the ref structure and the other structures. */
    CoordinateFile     ensemCoordFile;
    /** Map of Model -> "2XN" (colsXrows?) AtomState array, which comes from a 
    * sequence alignment of model to ref.  Coords in array[0]s should already be 
    * superposed onto ref; array[1]s *are* static ref. */
    Map                ensemAtoms;
    /** Intersection of ref atoms from all model-to-ref atom alignments. 
    * If any atoms were Lesk-sieved, they're ALREADY GONE from this set. */
    HashSet<AtomState> intersection;
    /** Number of entries in intersection.  Rows in U matrix. */
    int                nAtoms;
    /** Number of models in ensemble, including ref.  Columns in U matrix. */
    int                mEnsem;
    /** Maximum number of models in ensemble, including ref.  Capped to avoid ridiculous runtimes. */
    int                maxEnsemSize = 20;
    
    // PRINCIPAL COMPONENT ANALYSIS
    /** Selected AtomStates from ref, listed in the same order as the coordinates
    * of the first column in U matrix.  This guy is 1/3 the length of the first
    * column of U because it's by atom, not by x,y,z.  Purpose: a way to get back
    * to the atom world from the PCA world in order to write out (a) PC-transformed
    * structure(s). */
    ArrayList       pcaAtoms;
    /** Holds models based on the ref structure, but transformed according to 
    * principal components of selected atoms of the ensemble. */
    CoordinateFile  pcaCoordFile;
    /** Scaling factor for motion along PCs. */
    double          scale = 1.0;
    /** Choice of PCs to vector-sum.  1 to n. */
    int[]           pcChoice;
    
    // OUTPUT
    boolean  kinOut = true;
    /** Specifies how coordinates will be displayed in kinemage form.  
    * Default: -lots with Calphas. */
    Logic[]           logicList;
    
    
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SupKitchen()
    {
        super();
        
        BallAndStickLogic logic = new BallAndStickLogic();
        logic.doProtein       = true;
        logic.doNucleic       = true;
        logic.doHets          = true;
        logic.doIons          = true;
        logic.doWater         = true;
        logic.doPseudoBB      = true;
        logic.doBackbone      = true;
        logic.doSidechains    = true;
        logic.doHydrogens     = true;
        logic.doDisulfides    = true;
        logic.doBallsOnCarbon = false;
        logic.doBallsOnAtoms  = false;
        logic.colorBy         = BallAndStickLogic.COLOR_BY_MC_SC;
        logicList = new Logic[1];
        logicList[0] = logic;
    }
//}}}

//{{{ get/set...
//##############################################################################
    public String getTitle()
    { return title; }
    public double getLeskSieve()
    { return leskSieve; }
    public CoordinateFile getEnsemCoordFile()
    { return ensemCoordFile; }
    public CoordinateFile getPcaCoordFile()
    { return pcaCoordFile; }
    public double getScale()
    { return scale; }
    
    public void setRefFilename(String n)
    { refFilename = n; }
    public void setMdlFilename(String n)
    { mdlFilename = n; }
    public void setSuperimpose(String s)
    { superimpose = s; }
    public void setLeskSieve(double l)
    { leskSieve = l; }
    public void setScale(double s)
    { scale = s; }
    public void setDistort(boolean d)
    { distort = d; }
    public void setVerbose(boolean v)
    { verbose = v; }
//}}}


//{{{ makeSup
//##############################################################################
    /** Reads input files and superposes entire ensemble. */
    public void makeSup ()
    {
        // Models
        if(mdlFilename == null)
            throw new IllegalArgumentException("*** Must provide models dir or file!");
        File mf = new File(mdlFilename);
        if(mf.isDirectory())
            readMdlDir(mf, splitChains);
        else
            readMdlFile(mf, splitChains);
        
        // Ref
        boolean didAdHoc = false;
        if(refFilename == null)
        {
            pickAdHocRefFile();
            didAdHoc = true;
        }
        else refFile = new File(refFilename);
        readRefFile(refFile);
        Model refModel = refCoordFile.getFirstModel();
        if(refModel.getChainIDs().size() > 1)
        {
            System.err.println("*** Ref structure (model 1 from "+refFile+
                ") has multiple chains - superposition will be bad! .. exiting");
            System.exit(0);
        }
        String[] parts = Strings.explode(refCoordFile.getFile().getName(), '/');
        String basename = parts[parts.length-1];
        refModel.getState().setName(basename.substring(0,basename.indexOf(".pdb")));
        
        // Title
        String refChID = (String) refCoordFile.getFirstModel().getChainIDs().iterator().next();
        String r = refFile.getName().replaceAll(".pdb", "")+refChID;
        String m = (mdlFile != null ? mdlFile.getName() : mdlDir.getName()).replaceAll(".pdb", "");
        title = m+"."+r;
        System.err.println("Making \""+title+"\"");
        
        // Initial combination into superposed ensemble
        int ensemModelCount = 0;
        refModel.setName(""+ensemModelCount);
        ensemCoordFile = new CoordinateFile();
        ensemCoordFile.setIdCode(title);
        ensemCoordFile.add(refModel);
        ensemAtoms = new HashMap<Model,AtomState[][]>();
        for(int i = 0; i < mdlCoordFiles.length; i++)
        {
            CoordinateFile mdlCoordFile = (CoordinateFile) mdlCoordFiles[i];
            if(mdlCoordFile.getModels().size() != 0)
            {
                for(Iterator mIter = mdlCoordFile.getModels().iterator(); mIter.hasNext() && ensemModelCount < maxEnsemSize; )
                {
                    Model ensemModel = (Model) mIter.next();
                    if(didAdHoc && isRefDuplicate(ensemModel, refModel))
                    {
                        System.err.println(ensemModel+" is duplicate of ad hoc ref "+refModel+" - leaving out");
                        continue;
                    }
                    parts = Strings.explode(mdlCoordFile.getFile().getName(), '/');
                    basename = parts[parts.length-1];
                    ensemModel.getState().setName(basename.substring(0,basename.indexOf(".pdb")));
                    
                    AtomState[][] atoms = sup(ensemModel, refModel); // Lesk happens here
                    ensemModelCount++;
                    ensemModel.setName(""+ensemModelCount);
                    ensemCoordFile.add(ensemModel);
                    if(atoms != null) ensemAtoms.put(ensemModel, atoms);
                    else System.err.println("*** Error making atom-atom alignment for "+ensemModel+"!");
                }//model
            }
            else System.err.println("Skipping: "+mdlCoordFile+" because no models found");
        }//coord file
        mEnsem = ensemAtoms.keySet().size() + 1; // to include the ref
        
        intersect();
        
        trimEnsem();
        
        System.err.println("M = "+mEnsem+"\tsamples"); // cols
        System.err.println("N = "+nAtoms+"\tatoms");   // rows/3
    }
//}}}

//{{{ splitChains
//##############################################################################
    /** Divvies up the input CoordinateFile into single-chain CoordinateFiles. */
    public Collection splitChains(CoordinateFile orig)
    {
        Model model = orig.getFirstModel();               // ~input
        Collection ret = new ArrayList<CoordinateFile>(); // output
        
        for(Iterator cItr = model.getChainIDs().iterator(); cItr.hasNext(); )
        {
            String chainID = (String) cItr.next();
            Model clone = (Model) model.clone();
            for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
            {
                Residue res = (Residue) rItr.next();
                if(!res.getChain().equals(chainID) && clone.contains(res))
                {
                    try
                    {
                        clone.remove(res);
                        //System.err.println("Removed "+res+" from new chain"+chainID+" model");
                    }
                    catch(ResidueException ex)
                    { System.err.println("*** Error removing "+res+" from new chain"+chainID+" model!"); }
                }
                // else probably wanna keep this residue b/c in right chain
            }
            CoordinateFile cf = new CoordinateFile();
            cf.add(clone);
            ret.add(cf);
        }
        
        return ret;
    }
//}}}

//{{{ pickAdHocRefFile
//##############################################################################
    /**
    * Assuming ref file not provided, gets one from the provided models, 
    * be they in a directory or a single multi-model file.
    */
    public void pickAdHocRefFile()
    {
        // Find a resonable ref file
        if(mdlFile != null) refFile = mdlFile;
        else /*if(mdlDir != null)*/
        {
            File[] children = mdlDir.listFiles();
            if(children != null)
            {
                for(int i = 0; i < children.length; i++)
                {
                    File f = children[i];
                    if(f.getName().indexOf(".pdb") != -1)
                    {
                        refFile = f;
                        if(verbose) System.err.println("Picked "+f+" as ad hoc ref");
                        break;
                    }
                }//child
            }
        }
    }
//}}}

//{{{ isRefDuplicate
//##############################################################################
    /**
    * Tells whether provided model is equivalent to ref model and thus should be 
    * removed to avoid double-counting.  Important when ref was chosen ad hoc.
    * @param  m - model
    * @param  r - ref
    */
    public boolean isRefDuplicate(Model mdl, Model ref)
    {
        // mdl
        ArrayList<AtomState> mdlAtSt = new ArrayList<AtomState>();
        ModelState mdlSt = mdl.getState();
        for(Iterator rItr = mdl.getResidues().iterator(); rItr.hasNext(); )
        {
            Residue r = (Residue) rItr.next();
            for(Iterator aItr = r.getAtoms().iterator(); aItr.hasNext(); )
            {
                Atom a = (Atom) aItr.next();
                try
                {
                    AtomState s = mdlSt.get(a);
                    mdlAtSt.add(s);
                }
                catch(AtomException ex)
                { System.err.println("Can't find state for "+a+"!"); }
            }
        }
        
        // ref
        ArrayList<AtomState> refAtSt = new ArrayList<AtomState>();
        ModelState refSt = ref.getState();
        for(Iterator rItr = ref.getResidues().iterator(); rItr.hasNext(); )
        {
            Residue r = (Residue) rItr.next();
            for(Iterator aItr = r.getAtoms().iterator(); aItr.hasNext(); )
            {
                Atom a = (Atom) aItr.next();
                try
                {
                    AtomState s = refSt.get(a);
                    refAtSt.add(s);
                }
                catch(AtomException ex)
                { System.err.println("Can't find state for "+a+"!"); }
            }
        }
        
        // Let's assume that if all mdl atoms can find some ref w/ 
        // exactly the same coordinates, the two models are the same.
        for(AtomState m : mdlAtSt)
        {
            boolean matchExists = false;
            for(AtomState r : refAtSt)
            {
                if(m.equals(r))
                {
                    matchExists = true;
                    break;
                }
            }
            if(!matchExists) return false; // at least one atom doesn't match
        }
        return true;
    }
//}}}

//{{{ sup
//##############################################################################
    /**
    * Superposes 1 (mobile) onto 2 (ref).
    * Mostly stolen directly from Ian's chiropraxis.mc.SubImpose.
    */
    public AtomState[][] sup (Model m1, Model m2)
    {
        if(verbose) System.err.println("\nSuperposing model"+m1+" ("+SubImpose.getChains(m1).size()+
            " chains) onto model"+m2+" ("+SubImpose.getChains(m2).size()+" chains)");
        
        ModelState s1 = m1.getState();
        ModelState s2 = m2.getState();
        
        // Sequence-based residue alignment
        Alignment align = Alignment.alignChains(SubImpose.getChains(m1), SubImpose.getChains(m2), 
            new Alignment.NeedlemanWunsch(), new SubImpose.SimpleResAligner());
        // This ^ method rejects residue alignments with lots of gaps, e.g. when the  
        // ref is shorter, resulting in lots of nulls.  That leads to problems.
        // Here's an alternative alignment method that accepts such imperfect residue alignments:
        if(align.a.length == 0)
            align = Alignment.needlemanWunsch(m1.getResidues().toArray(), 
                m2.getResidues().toArray(), new SubImpose.SimpleResAligner());
        if(verbose)
        {
            System.err.println("Residue alignments:");
            for(int i = 0; i < align.a.length; i++)
            System.err.println("  "+align.a[i]+" <==> "+align.b[i]);
            System.err.println();
        }
        
        // One single structural superposition of all chains of 1 onto all chains of 2
        try
        {
            AtomState[][] atoms = this.getAtomsForSelection(
                m1.getResidues(), s1, m2.getResidues(), s2, superimpose, align);
            if(verbose)
            {
                System.err.println("Atom alignments:");
                for(int i = 0; i < atoms[0].length; i++)
                    System.err.println("  "+atoms[0][i]+" <==> "+atoms[1][i]);
                System.err.println();
            }
            if(atoms[0].length < 3) throw new IllegalArgumentException(
                "Can't superimpose on less than 3 atoms!");
            
            SuperPoser superpos = new SuperPoser(atoms[1], atoms[0]);
            Transform R = new Transform(); // identity, defaults to no superposition
            R = superpos.superpos();
            System.err.println("rmsd\tn_atoms\tselection");
            System.err.println(df.format(superpos.calcRMSD(R))+"\t"+atoms[0].length+"\t"+superimpose);
            
            ArrayList<AtomState> sieved = null; // remember ref atoms sieved below, if any, for later
            if(leskSieve >= 0)
            {
                // Eliminate selected atoms one-by-one until RMSD <= cutoff (default: 1A)
                sieved = new ArrayList<AtomState>();
                int sieveCount = 0;
                while(superpos.calcRMSD(R) > leskSieve && atoms[0].length > 3)
                {
                    sieveCount++;
                    SubImpose.sortByLeskSieve(atoms[0], atoms[1]); // messes up order!
                    int len = atoms[0].length - 1; // new length after we drop worst-fitting atom pair
                    sieved.add(atoms[1][len]); // ref
                    
                    AtomState[][] newAtoms = new AtomState[2][len];
                    for(int i = 0; i < 2; i++) for(int j = 0; j < len; j++) newAtoms[i][j] = atoms[i][j];
                    atoms = newAtoms;
                    
                    superpos.reset(atoms[1], atoms[0]);
                    R = superpos.superpos();
                    
                    System.err.println(
                        df.format(superpos.calcRMSD(R))+"\t"+atoms[0].length+"\t[Lesk's sieve x"+sieveCount+"]");
                }
            }
            if(verbose) System.err.println();
            
            // Transform model 1 so transformed coords will be used in the future
            for(Iterator iter = Model.extractOrderedStatesByName(m1).iterator(); iter.hasNext(); )
            {
                AtomState as = (AtomState) iter.next();
                R.transform(as);
            }
            
            // Return remaining selected atoms in original order
            AtomState[][] origAtoms = this.getAtomsForSelection(
                m1.getResidues(), s1, m2.getResidues(), s2, superimpose, align); // for PCA later
            if(sieved == null || sieved.size() == 0)
                return origAtoms;
            int newLen = origAtoms[0].length - sieved.size();
            AtomState[][] newAtoms = new AtomState[2][newLen];
            for(int i = 0; i < 2; i++)
                for(int j = 0; j < newAtoms[0].length; j++)
                    if(!sieved.contains(origAtoms[i][j])) // i.e. "if we didn't sieve this atom pair"
                        newAtoms[i][j] = origAtoms[i][j];
            return newAtoms;
        }
        catch(ParseException ex)
        {
            System.err.println("*** Error parsing atom selection: '"+superimpose+
                "' for superposing model: "+m1+" onto ref: "+m2);
        }
        return null;
    }
//}}}

//{{{ getAtomsForSelection
//##############################################################################
    /**
    * Apply the given selection to res1/s1, then find the corresponding atoms in res2/s2.
    * Return as a 2xN array of matched AtomStates, no nulls.
    * Stolen directly from Ian's chiropraxis.mc.SubImpose.
    */
    AtomState[][] getAtomsForSelection(Collection res1, ModelState s1, Collection res2, ModelState s2, String selection, Alignment align) throws ParseException
    {
        // Get selected atom states from model 1
        Selection sel = Selection.fromString(selection);
        Collection allStates1 = Model.extractOrderedStatesByName(res1, Collections.singleton(s1));
        sel.init(allStates1);
        Collection selStates1 = new ArrayList();
        for(Iterator iter = allStates1.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(sel.select(as))  selStates1.add(as);
        }
        
        // Set up residue correspondences
        Map map1to2 = new HashMap();
        for(int i = 0; i < align.a.length; i++)
        {
            if(align.a[i] != null)
                map1to2.put(align.a[i], align.b[i]); // b[i] may be null
        }
        
        // Get corresponding states from model 2
        Collection selStates2 = new ArrayList();
        int matched = 0;
        for(Iterator iter = selStates1.iterator(); iter.hasNext(); )
        {
            AtomState as1 = (AtomState) iter.next();
            AtomState as2 = null;
            Residue r = (Residue) map1to2.get( as1.getResidue() );
            if(r != null)
            {
                Atom a = r.getAtom( as1.getName() );
                if(a != null)
                {
                    try
                    {
                        as2 = s2.get(a);
                        matched++;
                    }
                    catch(AtomException ex) { ex.printStackTrace(); }
                }
            }
            selStates2.add(as2); // as2 could still be null
        }
        
        if(selStates1.size() != selStates2.size() || matched > selStates1.size())
            throw new RuntimeException("logical error; sel1="+selStates1.size()+", sel2="+selStates2.size()+", matched="+matched);
        
        // Arrange data into nice arrays for convenience
        AtomState[][] ret = new AtomState[2][matched];
        Iterator iter1 = selStates1.iterator();
        Iterator iter2 = selStates2.iterator();
        int idx = 0;
        while(iter1.hasNext())
        {
            AtomState as1 = (AtomState) iter1.next();
            AtomState as2 = (AtomState) iter2.next();
            if(as2 == null) continue;
            ret[0][idx] = as1;
            ret[1][idx] = as2;
            idx++;
        }
        
        if(idx != matched)
            throw new RuntimeException("logical error; idx="+idx+", matched="+matched);
        
        return ret;
    }
//}}}

//{{{ intersect
//##############################################################################
    /** Finds intersection: ref atoms included in all model-ref alignments */
    public void intersect()
    {
        HashSet<AtomState> union = new HashSet<AtomState>();
        for(Iterator mItr = ensemAtoms.keySet().iterator(); mItr.hasNext(); )
        {
            Model model = (Model) mItr.next();
            AtomState[][] atoms = (AtomState[][]) ensemAtoms.get(model);
            for(int i = 0; i < atoms[1].length; i++)
            {
                //System.err.println("adding "+atoms[1][i]+" to union");
                union.add(atoms[1][i]);
            }
        }
        
        HashSet<AtomState> toRm = new HashSet<AtomState>();
        intersection = union;
        for(Iterator iItr = intersection.iterator(); iItr.hasNext(); )
        {
            AtomState iRefAtomSt = (AtomState) iItr.next();
            boolean inAllModels = true; // default: keep in intersection
            for(Iterator mItr = ensemAtoms.keySet().iterator(); mItr.hasNext(); )
            {
                Model model = (Model) mItr.next();
                AtomState[][] atoms = (AtomState[][]) ensemAtoms.get(model);
                boolean inThisModel = false;
                for(int i = 0; i < atoms[1].length; i++)
                {
                    AtomState mRefAtomSt = atoms[1][i];
                    if(mRefAtomSt != null && mRefAtomSt.equals(iRefAtomSt))
                    {
                        inThisModel = true;
                        break;
                    }
                }
                if(!inThisModel) // made it thru whole for loop w/o finding in model
                    inAllModels = false; // b/c it's not in *this* model
            }
            if(!inAllModels)
                toRm.add(iRefAtomSt);
        }
        for(Iterator rmItr = toRm.iterator(); rmItr.hasNext(); )
        {
            AtomState iRefAtomSt = (AtomState) rmItr.next();
            //System.err.println("rm'ing "+iRefAtomSt+" from intersection");
            intersection.remove(iRefAtomSt);
        }
        
        nAtoms = intersection.size();
        
        if(verbose)
        {
            System.err.println("\nintersection ("+nAtoms+" atoms):");
            ArrayList<AtomState> inter = new ArrayList<AtomState>();
            for(Iterator iItr = intersection.iterator(); iItr.hasNext(); )
                System.err.println("  "+(AtomState)iItr.next());
        }
    }
//}}}

//{{{ trimEnsem
//##############################################################################
    /**
    * For each ensemble member:
    *  - drops residues with *zero* selected atoms in intersection
    *  - drops selected atoms (for PCA later) not in intersection
    * Implicitly reflects Lesk sieve b/c sieved atoms aren't in intersection.
    */
    public void trimEnsem()
    {
        for(Iterator mItr = ensemAtoms.keySet().iterator(); mItr.hasNext(); )
        {
            Model ensemModel = (Model) mItr.next();
            
            AtomState[][] atoms = (AtomState[][]) ensemAtoms.get(ensemModel);
            atoms = trimAlignment(atoms, ensemModel);
            ensemAtoms.put(ensemModel, atoms);
            
            if(verbose)
            {
                System.err.println("\npost-trim alignment:");
                for(int i = 0; i < atoms[0].length; i++)
                    System.err.println("  "+atoms[0][i]+" <==> "+atoms[1][i]);
            }
            
            AtomState[] mdlIntrsctn = atoms[0]; // atoms from this model corresponding to ref-based intersection
            Model newEnsemModel = trimModel(ensemModel, mdlIntrsctn);
            ensemCoordFile.replace(ensemModel, newEnsemModel);
            
            if(verbose)
            {
                System.err.println("\npost-trim residues:");
                for(Iterator rItr = newEnsemModel.getResidues().iterator(); rItr.hasNext(); )
                    System.err.println("  "+(Residue)rItr.next());
            }
        }
    }
//}}}

//{{{ trimAlignment
//##############################################################################
    /**
    * If the given model contains any seleced atoms *not* in the intersection, 
    * this method drops them (so they won't be used for PCA later).
    */
    public AtomState[][] trimAlignment(AtomState[][] atoms, Model model)
    {
        // I had trouble thinking through what this method should do 
        // for some reason, so here's some pseudo-code that may help:
        //
        // for(sel_atom_in_mdl)
        //     for(int_atom)
        //         if(int_atom matches sel_atom_in_mdl)
        //             mark sel_atom_in_mdl as "in int"
        //     if(sel_atom_in_mdl "in int")
        //         keep this sel_atom_in_mdl
        
        ModelState state = model.getState();
        
        TreeSet<Integer> toKeep = new TreeSet<Integer>(); // indices
        
        for(int i = 0; i < atoms[1].length; i++)
        {
            AtomState enAtm = atoms[1][i];
            boolean inIn = false;
            for(Iterator iItr = intersection.iterator(); iItr.hasNext(); )
            {
                AtomState inAtm = (AtomState) iItr.next();
                if(inAtm.equals(enAtm)) inIn = true;
            }
            if(inIn) toKeep.add(i);
            //else System.err.println(enAtm+" NOT in intersection!");
        }
        
        AtomState[][] newAtoms = new AtomState[2][toKeep.size()];
        int count = 0;
        for(Iterator aiItr = toKeep.iterator(); aiItr.hasNext(); )
        {
            int ai = (Integer) aiItr.next();
            newAtoms[0][count] = atoms[0][ai];
            newAtoms[1][count] = atoms[1][ai];
            count++;
        }
        
        return newAtoms;
    }
//}}}

//{{{ trimModel
//##############################################################################
    /**
    * If a residue has any atoms in the intersection, it's kept.
    * Otherwise, it's dropped, and its coordinates won't be in any output PDB.
    * Note: This probably leaves a lot of residues with only some atoms to be used for PCA.
    */
    public Model trimModel(Model model, AtomState[] intrsctn)
    {
        TreeSet<Residue> toKeep = new TreeSet<Residue>();
        for(int i = 0; i < intrsctn.length; i++)
        {
            Atom a = intrsctn[i].getAtom();
            Residue r = a.getResidue();
            toKeep.add(r); // this residue has at least one atom in intersection - keep
        }
        
        TreeSet<Residue> toRm = new TreeSet<Residue>();
        for(Iterator rItr = model.getResidues().iterator(); rItr.hasNext(); )
        {
            Residue r = (Residue) rItr.next();
            boolean wannaKeep = false;
            for(Iterator kItr = toKeep.iterator(); kItr.hasNext(); )
            {
                Residue k = (Residue) kItr.next();
                if(k.getCNIT().equals(r.getCNIT()))
                {
                    wannaKeep = true;
                    break;
                }
            }
            if(!wannaKeep)
                toRm.add(r);
        }
        
        for(Iterator rItr = toRm.iterator(); rItr.hasNext(); )
        {
            Residue r = (Residue) rItr.next();
            try
            { model.remove(r); }
            catch(ResidueException ex)
            { System.err.println("*** Error removing "+r+" from model "+model); }
        }
        
        return model;
    }
//}}}


//{{{ doPca
//##############################################################################
    /**
    * Prepares square X matrix comprising ensemble for PCA, performs PCA on it, and 
    * creates a set of "models" representing movements along an average of some subset of PCs.
    */
    public void doPca()
    {
        System.err.println("Starting PCA on PCs "+Strings.arrayInParens(pcChoice));
        
        // Prep for SVD
        double[][] xArray = buildX();
        Matrix x = new Matrix(xArray);
        if(verbose)
        {
            System.err.print("Input X matrix:");
            x.print(new PrintWriter(System.err, true), new DecimalFormat("#.####"), 1);
        }
        
        // Do SVD
        // X = USV^T  (S = sigma, V^T = V transpose)
        if(verbose) System.err.println("Doing SVD now");
        SingularValueDecomposition svd = new SingularValueDecomposition(x);
        Matrix u = svd.getU(); // columns = principal componenents
        Matrix s = svd.getS(); // diagonal elements = singular values (weights)
        if(verbose)
        {
            System.err.print("SVD output U Matrix:");
            u.print(new PrintWriter(System.err, true), new DecimalFormat("#.####"), 1);
            System.err.print("SVD output S matrix:");
            s.print(new PrintWriter(System.err, true), new DecimalFormat("#.####"), 1);
        }
        
        // Take weighted vector sum (i.e. weighted average x,y,z) of desired PCs
        if(Double.isNaN(scale)) scale = 1.0; // default: x1.0 scaling
        double[] avgPc = avgPc(u, s);
        
        // Build a set of true copies of the ref model, 
        // with the atoms originally used for superposition 
        // moved according to the averaged PC in increments.
        pcaCoordFile = new CoordinateFile();
        pcaCoordFile.setIdCode(title+"_PCA");
        double maxFrac = 1.0;
        int    step    = 1  ;
        int    steps   = 10 ;
        System.err.println("Distorting ensemble via wt'd-avg PC ...");
        for(double frac = -1*maxFrac; frac <= maxFrac; frac += (2*maxFrac)/steps)
        {
            if(verbose) System.err.print("  moving along wt'd-avg PC x "+df2.format(frac));
            try
            {
                Model model = applyPcToRefModel(avgPc, frac, step);
                if(model != null) pcaCoordFile.add(model);
            }
            catch(IOException ex)
            { System.err.println("*** Error opening ref file: "+refFile.getName()+"!"); }
            if(verbose) System.err.println(" .. done");
            step++;
        }
    }
//}}}

//{{{ buildX
//##############################################################################
    /** Portrays ensemble in the guise of an N x M (rows x columns) 2-D array. */
    public double[][] buildX()
    {
        if(verbose) System.err.println("Building X matrix for PCA");
        
        double[][] xArray = new double[3*nAtoms][mEnsem];
        int mCount = 0;
        
        // Ref (just once)
        Iterator mItr1 = ensemAtoms.keySet().iterator();
        if(mItr1.hasNext())
        {
            Model ref = (Model) mItr1.next(); // doesn't matter which we pick here - will use ref half of alnmt
            if(verbose) System.err.println("Adding to X matrix: ref   model"+ref);
            AtomState[][] atoms = (AtomState[][]) ensemAtoms.get(ref);
            pcaAtoms = new ArrayList<AtomState>(); // so later, we'll know which coord in X belongs to which atom
            ArrayList<Double> coords = new ArrayList<Double>(); // one 1x3N "sample" (ensemble member)
            for(int n = 0; n < atoms[0].length; n++)
            {
                if(coords.size() < (3*nAtoms)-2 && atoms[0][n] != null && atoms[1][n] != null)
                {
                    AtomState refState  = atoms[1][n];
                    Atom      refAtom   = refState.getAtom();
                    pcaAtoms.add(refState);
                    coords.add( atoms[1][n].getX() );
                    coords.add( atoms[1][n].getY() );
                    coords.add( atoms[1][n].getZ() );
                }
            }
            // go up to coords.size() instead of nAtoms*3 here b/c 
            // coords.size() could be < nAtoms*3 given Gly w/ bb+Cb option
            for(int n = 0; n < coords.size(); n++)
                xArray[n][mCount] = coords.get(n);  //  m = cols = samples;  n = rows = atoms
            mCount++;
            /*if(verbose)
            {
                System.err.print("ref ensem X matrix entry: ");
                for(int n = 0; n < coords.size(); n++) System.err.print(coords.get(n)+" ");
                System.err.println(" .. "+coords.size()/3+" atoms");
            }*/
        }
        
        // Models (loop)
        for(Iterator mItr2 = ensemAtoms.keySet().iterator(); mItr2.hasNext(); )
        {
            Model model = (Model) mItr2.next();
            if(verbose) System.err.println("Adding to X : ensem model"+model);
            AtomState[][] atoms = (AtomState[][]) ensemAtoms.get(model);
            ArrayList<Double> coords = new ArrayList<Double>(); // one 1x3N "sample" (ensemble member)
            for(int n = 0; n < atoms[0].length; n++)
            {
                if(coords.size() < (3*nAtoms)-2 && atoms[0][n] != null && atoms[1][n] != null)
                {
                    AtomState refState  = atoms[1][n];
                    Atom      refAtom   = refState.getAtom();
                    coords.add( atoms[0][n].getX() );
                    coords.add( atoms[0][n].getY() );
                    coords.add( atoms[0][n].getZ() );
                }
            }
            // go up to coords.size() instead of nAtoms*3 here b/c 
            // coords.size() could be < nAtoms*3 given Gly w/ bb+Cb option
            for(int n = 0; n < coords.size(); n++)
                xArray[n][mCount] = coords.get(n); // n = rows = atoms;  m = cols = samples
            mCount++;
            /*if(verbose)
            {
                System.err.print("mdl ensem X matrix entry: ");
                for(int n = 0; n < coords.size(); n++) System.err.print(coords.get(n)+" ");
                System.err.println(" .. "+coords.size()/3+" atoms");
            }*/
        }
        
        // Substract ensemble average coordinates from all members.
        // B Qian, A Ortiz, D Baker (2004) PNAS - "Improvement..."
        // calls this a "coordinate displacement vector" or CDV.
        if(verbose) System.err.println("Making displacement vectors for X matrix");
        for(int n = 0; n < 3*nAtoms; n++)
        {
            // Columns first this time, because we want an average 
            // for a specific x, y, or z value across the ensemble.
            double avgVal = 0;
            for(int m = 0; m < mEnsem; m++) avgVal += xArray[n][m];
            avgVal /= mEnsem;
            for(int m = 0; m < mEnsem; m++) xArray[n][m] = xArray[n][m] - avgVal;
        }
        
        return xArray;
    }
//}}}

//{{{ avgPc
//##############################################################################
    /** Calculates vector avg of the user's choice of PC(s) and scales it (opt'l). */
    public double[] avgPc(Matrix u, Matrix s)
    {
        double[] avgPc = new double[pcaAtoms.size()+2];
        for(int n = 0; n < pcaAtoms.size(); n++)
        {
            // Add all desired PCs to this PC vector
            double avgX = 0, avgY = 0, avgZ = 0;
            for(int p = 0; p < pcChoice.length; p++)
            {
                int pcIdx = pcChoice[p]-1;
                // B Qian, A Ortiz, D Baker (2004) PNAS - "Improvement..."
                // defines a PC as the U matrix times the S matrix, like so:
                avgX += u.get(n  , pcIdx) * s.get(pcIdx, pcIdx) * scale;
                avgY += u.get(n+1, pcIdx) * s.get(pcIdx, pcIdx) * scale;
                avgZ += u.get(n+2, pcIdx) * s.get(pcIdx, pcIdx) * scale;
            }
            avgPc[n  ] = avgX / pcChoice.length;
            avgPc[n+1] = avgY / pcChoice.length;
            avgPc[n+2] = avgZ / pcChoice.length;
        }
        return avgPc;
    }
//}}}

//{{{ applyPcToRefModel
//##############################################################################
    /**
    * Applies the user's choice of principal component(s) to *all* atoms -- i.e. not
    * just the user's selected atoms -- to a fresh copy of the original ref model.
    */
    public Model applyPcToRefModel(double[] avgPc, double frac, int step) throws IOException
    {
        // Trim to intersection - implicitly pulls out the single ref chain we orig'ly chose
        AtomState[] refIntrsctn = new AtomState[intersection.size()];
        int count = 0;
        for(Iterator inItr = intersection.iterator(); inItr.hasNext(); )
        {
            refIntrsctn[count] = (AtomState) inItr.next();
            count++;
        }
        CoordinateFile cf1 = readPdb(refFile);
        CoordinateFile cf2 = readPdb(refFile);
        Model m1 = trimModel(cf1.getFirstModel(), refIntrsctn); // PC-distorted - becomes ref    for sup
        Model m2 = trimModel(cf2.getFirstModel(), refIntrsctn); // rigid ref    - becomes mobile for sup
        m1.setName(""+step);
        m2.setName(""+step);
        
        // Apply PC to 1
        for(int i = 0; i < pcaAtoms.size(); i++)
        {
            AtomState as = (AtomState) pcaAtoms.get(i);
            Triple dxyz = new Triple(frac * avgPc[i  ],
                                     frac * avgPc[i+1],
                                     frac * avgPc[i+2]);
            m1 = applyPcToAtom(m1, as, dxyz); // directly modifies m1
        }
        
        if(distort) return m1; // if we wanted PC-distorted ref model, we're done
        
        // Transform *all* of now-mobile model (2) according to PC motions of 
        // the subset of selected atoms.  Code modified from sup() method.
        ModelState s1 = m1.getState();
        ModelState s2 = m2.getState();
        Alignment align = Alignment.alignChains(SubImpose.getChains(m1), SubImpose.getChains(m2), 
            new Alignment.NeedlemanWunsch(), new SubImpose.SimpleResAligner());
        try
        {
            AtomState[][] atoms = this.getAtomsForSelection(
                m1.getResidues(), s1, m2.getResidues(), s2, superimpose, align);
            SuperPoser superpos = new SuperPoser(atoms[0], atoms[1]); // swapped [1],[0] -> [0],[1]
            Transform R = new Transform(); // identity, defaults to no superposition
            R = superpos.superpos();
            
            // Transform 2 (was 1 in sup()!) so transformed coords will be used in the future
            for(Iterator iter = Model.extractOrderedStatesByName(m2).iterator(); iter.hasNext(); )
            {
                AtomState as = (AtomState) iter.next();
                R.transform(as);
            }
            
            return m2;
        }
        catch(ParseException ex)
        { System.err.println("*** Error parsing atom selection: '"+superimpose+
            "' for superposing model: "+m2+" onto ref: "+m1); }
        return null;
    }
//}}}

//{{{ applyPcToAtom
//##############################################################################
    /** Applies the given translation to the given selected atom, then returns the model. */
    public Model applyPcToAtom(Model m, AtomState as, Triple dxyz)
    {
        Atom       a  = (Atom) as.getAtom();
        String     an = a.getName();
        Residue    r  = (Residue) a.getResidue();
        int        rt = r.getSequenceInteger();
        String     rn = r.getName();
        String     c  = r.getChain();
        
        // Find the atom in the provided ref clone ('m') 
        // corresponding to the PCA-modified atom ('a') 
        ModelState ms = (ModelState) m.getState();
        for(Iterator rIter = m.getResidues().iterator(); rIter.hasNext(); )
        {
            Residue res = (Residue) rIter.next();
            if(res.getSequenceInteger() == rt
            && res.getName().equals(rn)
            && res.getChain().equals(c))
            {
                // right residue
                for(Iterator aIter = res.getAtoms().iterator(); aIter.hasNext(); )
                {
                    Atom atom = (Atom) aIter.next();
                    if(atom.getName().equals(an))
                    {
                        // right atom
                        try
                        {
                            AtomState atomState = ms.get(atom);
                            atomState.setX( atomState.getX() + dxyz.getX() );
                            atomState.setY( atomState.getY() + dxyz.getY() );
                            atomState.setZ( atomState.getZ() + dxyz.getZ() );
                        }
                        catch(AtomException ex)
                        { System.err.println("*** Error extracting coords for "+atom+"!"); }
                    }
                }//atom
            }//residue
        }
        
        return m;
    }
//}}}


//{{{ read...
//##############################################################################
    /** Reads through several PDB files containing models from a directory. */
    public void readMdlDir(File mf, boolean splitChains)
    {
        mdlDir  = mf;
        mdlFile = null;
        
        File[] children = mdlDir.listFiles();
        int numFilesToRead = children.length;
        if(children.length > maxEnsemSize)
        {
            System.err.println("Found "+children.length+" models - too many! Using first "+maxEnsemSize);
            numFilesToRead = maxEnsemSize;
        }
        if(children != null)
        {
            ArrayList<CoordinateFile> cfs = new ArrayList<CoordinateFile>();
            for(int i = 0; i < numFilesToRead; i++)
            {
                File f = children[i];
                if(f.getName().indexOf(".pdb") != -1)
                {
                    try
                    {
                        CoordinateFile inputCoordFile = readPdb(f);
                        if(splitChains) // each input chain => separate mdl coord file
                        {
                            for(Iterator cfi = splitChains(inputCoordFile).iterator(); cfi.hasNext(); )
                            {
                                CoordinateFile cf = (CoordinateFile) cfi.next();
                                cf.setFile(f);
                                cfs.add(cf);
                            }
                        }
                        else // all input chains kept in their original mdl coord file
                        {
                            inputCoordFile.setFile(f);
                            cfs.add(inputCoordFile);
                        }
                        System.err.println("Extracted model coords: " + inputCoordFile.toString());
                    }
                    catch(IOException ex)
                    { System.err.println("*** Error reading model .pdb:" + f.getName() + "!"); }
                }
                else System.err.println("Ignoring non-.pdb file: " + f.getName());
            }
            
            mdlCoordFiles = new CoordinateFile[cfs.size()];
            for(int i = 0; i < cfs.size(); i++)
                mdlCoordFiles[i] = (CoordinateFile) cfs.get(i);
        }
        else System.err.println("Nothing in selected models dir: " + mdlDir);
    }

    /** Reads one NMR-like PDB file containing models. */
    public void readMdlFile(File mf, boolean splitChains)
    {
        mdlDir  = null;
        mdlFile = mf;
        
        if(mdlFile.getName().indexOf(".pdb") != -1)
        {
            try
            {
                ArrayList<CoordinateFile> cfs = new ArrayList<CoordinateFile>();
                CoordinateFile inputCoordFile = readPdb(mdlFile);
                System.err.println("Found "+inputCoordFile.getModels().size()+" models in "+inputCoordFile.toString());
                if(splitChains) // each input chain => separate mdl coord file
                {
                    for(Iterator cfi = splitChains(inputCoordFile).iterator(); cfi.hasNext(); )
                    {
                        CoordinateFile cf = (CoordinateFile) cfi.next();
                        cf.setFile(mdlFile);
                        cfs.add(cf);
                    }
                }
                else // all input chains kept in their original mdl coord file
                {
                    inputCoordFile.setFile(mdlFile);
                    cfs.add(inputCoordFile);
                }
                
                mdlCoordFiles = new CoordinateFile[cfs.size()];
                for(int i = 0; i < cfs.size(); i++)
                    mdlCoordFiles[i] = (CoordinateFile) cfs.get(i); // could have just 1
            }
            catch(IOException ex)
            { System.err.println("*** Error reading models .pdb:" + mdlFile.getName() + "!"); }
        }
        else System.err.println("Selected models file not a .pdb: " + mdlFile.toString());
    }

    public void readRefFile(File refFile)
    {
        if(refFile.getName().indexOf(".pdb") != -1)
        {
            try
            {
                // Take just one chain, so models will all sup to same chain.
                CoordinateFile inputCoordFile = readPdb(refFile);
                ArrayList<CoordinateFile> chains = (ArrayList<CoordinateFile>) splitChains(inputCoordFile);
                if(chains.size() == 0)
                    throw new IllegalArgumentException("No chains found in ref file "+refFile+"!");
                String chainID = null;
                // Try and pick a reasonable chain, not just one at random.
                for(int i = 0; i < chains.size(); i++)
                {
                    CoordinateFile chain = chains.get(i);
                    chainID = (String) chain.getFirstModel().getChainIDs().iterator().next();
                    if(chainID.equals("A") || chainID.equals("_") || chainID.equals(" "))
                    {
                        refCoordFile = chain;
                        refCoordFile.setFile(refFile);
                        break;
                    }
                }
                if(refCoordFile == null) // no chain "A" or "_" or " "
                {
                    refCoordFile = chains.get(0);
                    refCoordFile.setFile(refFile);
                    chainID = (String) refCoordFile.getFirstModel().getChainIDs().iterator().next();
                }
                if(verbose)
                {
                    System.err.print("Using chain "+chainID+" from ref file "+refFile);
                    if(chains.size() > 1) System.err.print(" - there was/were "+(chains.size()-1)+" other(s)");
                    System.err.println();
                }
                System.err.println("Extracted ref coords: " + refCoordFile.toString());
            }
            catch(IOException ex)
            { System.err.println("*** Error reading ref .pdb:" + refFile.getName() + "!"); }
        }
        else System.err.println("Selected ref file not a .pdb: " + refFile.toString());
    }
    
    static public CoordinateFile readPdb(File f) throws IOException
    {
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(f);
        return coordFile;
    }
//}}}

//{{{ write...
//##############################################################################
    /** 
    * Writes kinemage of ensemble to desired output (usually System.out).
    */
    public PrintWriter writeKin(PrintWriter out, CoordinateFile cf, String name)
    {
        out.println("@kinemage {"+name+"}");
        out.println("@onewidth");
        out.println("@master {models}");
        out.println("@master {ref}");
        out.println("@master {"+title+"}");
        Logic logic = logicList[0];
        for(Iterator mItr = cf.getModels().iterator(); mItr.hasNext(); )
        {
            Model m = (Model) mItr.next();
            String mName = m.getName();
            if(m.getState().getName().indexOf("null") == -1) mName += " "+m.getState().getName();
            if(m.getName().equals("0")) out.println("@group {"+mName+"} dominant master= {"+title+"} master= {ref}");
            else                        out.println("@group {"+mName+"} dominant master= {"+title+"} master= {models} animate");
            for(Iterator cItr = m.getChainIDs().iterator(); cItr.hasNext(); )
            {
                String c = (String) cItr.next();
                out.println("@subgroup {chain"+c+"} dominant master= {chain"+c+"}");
                String color = (m.getName().equals("0") ? "yellowtint" : "white");
                logic.printKinemage(out, m, m.getChain(c), mName, color);
            }
        }
        out.flush();
        return out;
    }

    /**
    * Writes a single, multi-MODEL PDB holding entire ensemble.
    */
    public void writePdb(PrintStream ps, CoordinateFile cf)
    {
        PrintWriter out = new PrintWriter(ps);
        PdbWriter pdbWriter = new PdbWriter(out);
        pdbWriter.writeCoordinateFile(cf, new HashMap());
        out.flush();
        pdbWriter.close();
    }

    /**
    * Makes a new "ensemble directory" and writes into it a separate PDB for  
    * each superposed ensemble member.
    * Intended for cmdline mode, not for KiNG tool.
    */
    public void writePdbs(CoordinateFile cf)
    {
        System.err.println("*** Haven't implemented writePdbs(...) method! ***");
        System.exit(0);
    }
//}}}

//{{{ parsePcChoice
//##############################################################################
    /** Figures out indices of desired PCs from a string. */
    public void parsePcChoice(String pcChoiceString)
    {
        ArrayList<Integer> pcChoiceAL = new ArrayList<Integer>();
        // pcChoiceString: "1,2-5,33" or "1-10" or "1"
        for(String range : Strings.explode(pcChoiceString, ',')) // "1-3" or "1"
        {
            String[] ends = Strings.explode(range, '-');
            if(ends.length == 1) // "1"
            {
                try
                {
                    int num = Integer.parseInt(ends[0]);
                    pcChoiceAL.add(num);
                }
                catch(NumberFormatException ex)
                { System.err.println("*** Can't parse PC range: '"+range+"'"); }
            }
            else if(ends.length == 2) // "1-3"
            {
                try
                {
                    int beg = Integer.parseInt(ends[0]);
                    int end = Integer.parseInt(ends[1]);
                    for(int num = beg; num <= end; num++)
                        pcChoiceAL.add(num);
                }
                catch(NumberFormatException ex)
                { System.err.println("*** Can't parse PC range: '"+range+"'"); }
            }
            else
                throw new IllegalArgumentException("*** Can't parse PC choice: '"+pcChoice+"'");
        }
        
        pcChoice = new int[pcChoiceAL.size()];
        for(int i = 0; i < pcChoiceAL.size(); i++)
            pcChoice[i] = pcChoiceAL.get(i); // modifies global variable - nothing to return
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, ParseException
    {
        System.err.println("The SupKitchen is busy preparing your order ...");
        System.err.println("Desired output: "+(kinOut ? "kin" : "PDB"));
        if(leskSieve >= 0)
            System.err.println("Trimming to "+leskSieve+"A rmsd");
        else
            System.err.println("Not trimming to an rmsd cutoff");
        if(superimpose == null)
            superimpose = SELECT_BB_HEAVY;
        makeSup();
        if(pcChoice == null) // not doing PCA
        {
            if(kinOut)
                writeKin(new PrintWriter(System.out), ensemCoordFile, title);
            else
                writePdb(System.out, ensemCoordFile);
            System.err.println("... Your steaming-hot sup is served!");
        }
        else
        {
            doPca();
            if(kinOut)
                writeKin(new PrintWriter(System.out), pcaCoordFile, title);
            else
                writePdb(System.out, pcaCoordFile);
            System.err.println("... Your steaming-hot sup (w/ a side of PCA) is served!");
        }
    }

    public static void main(String[] args)
    {
        SupKitchen mainprog = new SupKitchen();
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
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(2);
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
            InputStream is = getClass().getResourceAsStream("SupKitchen.help");
            if(is == null)
            {
                System.err.println("*** Unable to locate help information in 'SupKitchen.help' ***");
                System.err.println("\n*** Usage: SupKitchen modelsDirectoryOrNMRlikeFile [refFile] ***\n");
            }
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.SupKitchen");
        System.err.println("Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.");
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
        if(mdlFilename == null)      mdlFilename = arg;
        else if(refFilename == null) refFilename = arg;
        else throw new IllegalArgumentException("too many arguments!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-kin"))
        {
            kinOut = true;
        }
        else if(flag.equals("-pdb"))
        {
            kinOut = false;
        }
        else if(flag.equals("-nosplit"))
        {
            splitChains = false;
        }
        else if(flag.equals("-pc"))
        {
            parsePcChoice(param); // sets global var pcChoice
        }
        else if(flag.equals("-scale"))
        {
            try
            { scale = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { System.err.println("*** Error formatting "+param+" as double for scale!"); }
        }
        else if(flag.equals("-lesk"))
        {
            try
            { leskSieve = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { throw new IllegalArgumentException(param+" isn't a number!"); }
        }
        else if(flag.equals("-nolesk"))
        {
            leskSieve = -1;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

