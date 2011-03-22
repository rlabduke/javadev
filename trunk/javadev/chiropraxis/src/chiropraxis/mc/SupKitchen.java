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
* <code>SupKitchen</code> "cooks up" superpositions (*groan*) of related protein 
* structures and analyzes the resulting ensembles via principal components.
*
* It tries to construct ensembles intelligently when the sequences are different, 
* there are insertion loops in the reference and/or some of the other models, etc.
*
* A lot of this code comes from Ian's chiropraxis.mc.SubImpose.
*
* TODO: regularize bb after distortion
*       keep rmsds w/in a certain radius of sup chain for -nosplit option (???)
*
* <p>Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu June 25 2009
*/
public class SupKitchen //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df  = new DecimalFormat("###0.00");
    DecimalFormat df2 = new DecimalFormat("###0.0");
    DecimalFormat df3 = new DecimalFormat("#0.00");
    public final String SELECT_CA            = "(atom_CA_)";
    public final String SELECT_BB_HEAVY      = "(atom_CA_|atom_N__|atom_C__|atom_O__)";
    public final String SELECT_BB_HEAVY_H    = "(atom_CA_|atom_N__|atom_C__|atom_O__|atom_H__)";
    public final String SELECT_BB_HEAVY_CB   = "(atom_CA_|atom_N__|atom_C__|atom_O__|atom_CB_)";
    public final String SELECT_BB_HEAVY_CB_H = "(atom_CA_|atom_N__|atom_C__|atom_O__|atom_CB_|atom_H__)";
//}}}

//{{{ Variable definitions
//##############################################################################
    
    /* Input */
    
    boolean  verbose = false;
    String   mdlFilename; // could be for mdls dir or mdls file
    File     mdlDir;
    File     mdlFile;
    String   refFilename;
    File     refFile;
    String   title;
    String   superimpose = SELECT_BB_HEAVY_H;
    boolean  distort = true;
    /** Max acceptable RMSD, effected by simple yes-or-no after sup. */
    double   rmsdMax  = -1;
    /** Max acceptable RMSD, achieved by Lesk sieve process.  Overrides rmsdMax. */
    double   rmsdLesk = 5.0;
    
    /* Ensemble Creation */
    
    /** If true, we split up all models by chain.
    * Required for PCA b/c allows formation of a square matrix of superpositions 
    * (some models may have lots of structure surrounding them whereas some may not).
    * Applies only to models; just one chain is taken from the ref regardless of this 
    * option so models will all superpose onto the same ref chain. */
    boolean            splitChains = true;
    
    /** Holds models with however-many chains are present from the ref structure. */
    CoordinateFile     refCoordFile;
    
    /** Holds models with >=1 chains (depending on user's options) from other structures. 
    * Could contain just one CoordinateFile with multiple Models if NMR-like structure. */
    CoordinateFile[]   mdlCoordFiles;
    
    /** Holds models from the ref structure and the other structures. */
    CoordinateFile     ensemCoordFile;
    
    /** Map of Model -> "2XN" (rows X cols) AtomState array, which comes from a 
    * sequence alignment of model to ref.  Coords in array[0]s should already be 
    * superposed onto ref; array[1]s *are* static ref. */
    Map                ensemAtoms;
    
    /** RMSD of each model to ref (after Lesk if applicable). */
    Map                rmsds;
    
    /** Intersection of ref atoms from all model-to-ref atom alignments. 
    * If any atoms were Lesk-sieved, they're ALREADY GONE from this set. */
    HashSet<AtomState> intersection;
    
    /** Number of entries in intersection.  Rows in U matrix. */
    int                nAtoms;
    
    /** Number of models in ensemble, including ref.  Columns in U matrix. */
    int                mEnsem;
    
    /** Maximum number of models in ensemble, including ref.  Capped to avoid ridiculous runtimes. */
    int                maxEnsemSize = 50;
    
    /* Principal Component Analysis */
    
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
    
    /** ... */
    int             roundsOfMinimization = 0;
    
    /* Output */
    
    boolean  kinOut = true;
    boolean  append = false;
    /** Backbone color for non-ref models. */
    String   color;
    /** Specifies how coords will be displayed in kin form.  Default: -lots with Calphas. */
    Logic[]           logicList;
    /** Specifies directory where ensemble PDBs will be written.  Alternative to
    * output kinemage or multi-MODEL, single PDB file. */
    String   pdbsDirname;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SupKitchen()
    {
        super();
        prepLogic();
    }
//}}}

//{{{ prepLogic
//##############################################################################
    public void prepLogic()
    {
        BallAndStickLogic logic = new BallAndStickLogic();
        logic.doProtein       = true;
        logic.doNucleic       = true;
        logic.doHets          = true;
        logic.doMetals        = true;
        logic.doWater         = true;
        logic.doVirtualBB     = true;
        logic.doMainchain     = true;
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
    public double getRmsdLesk()
    { return rmsdLesk; }
    public CoordinateFile getEnsemCoordFile()
    { return ensemCoordFile; }
    public CoordinateFile getPcaCoordFile()
    { return pcaCoordFile; }
    public double getScale()
    { return scale; }
    public int getMaxEnsemSize()
    { return maxEnsemSize; }
    
    public void setRefFilename(String n)
    { refFilename = n; }
    public void setMdlFilename(String n)
    { mdlFilename = n; }
    public void setSuperimpose(String s)
    { superimpose = s; }
    public void setRmsdLesk(double l)
    { rmsdLesk = l; }
    public void setScale(double s)
    { scale = s; }
    public void setMaxEnsemSize(int i)
    { maxEnsemSize = i; }
    public void setDistort(boolean d)
    { distort = d; }
    public void setVerbose(boolean v)
    { verbose = v; }
//}}}

/* Ensemble Creation */

//{{{ makeSup
//##############################################################################
    /** Reads input files and superposes entire ensemble. */
    public void makeSup() throws IOException
    {
        // Models
        if(mdlFilename == null) throw new IllegalArgumentException(
            "*** Must provide models dir or file!");
        File mf = new File(mdlFilename);
        if(mf.isDirectory()) readMdlDir( mf, splitChains);
        else                 readMdlFile(mf, splitChains);
        
        // Ref
        if(refFilename == null) pickAdHocRefFile();
        else refFile = new File(refFilename);
        readRefFile(refFile);
        Model refModel = refCoordFile.getFirstModel();
        if(refModel.getChainIDs().size() > 1) throw new IllegalArgumentException(
            "*** Ref (model 1 from "+refFile+") has > 1 chain .. sup will be bad .. exiting!");
        String[] parts = Strings.explode(refCoordFile.getFile().getName(), '/');
        String basename = parts[parts.length-1];
        //String refChId = (String) refModel.getChainIDs().iterator().next();
        refModel.getState().setName(basename.substring(0,basename.indexOf(".pdb")));//+refChId);
        
        // Title
        String refChID = (String) refCoordFile.getFirstModel().getChainIDs().iterator().next();
        if(refChID.equals(" ")) refChID = "_";
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
        rmsds = new HashMap<Model,Double>();
        System.err.println("rmsd\tn_atoms\tselection\tmodel");
        for(int i = 0; i < mdlCoordFiles.length; i++)
        {
            CoordinateFile mdlCoordFile = (CoordinateFile) mdlCoordFiles[i];
            if(mdlCoordFile.getModels().size() != 0)
            {
                // Skip if from same PDB as ref, implying either ref chain duplicate
                // or totally unrelated protein that would screw up superposition.
                String mFilename = mdlCoordFile.getFile().getName();
                String rFilename = refCoordFile.getFile().getName();
                if(mFilename.equals(rFilename))
                {
                    System.err.println("Skipping additional model from ref file: "+rFilename);
                    continue;
                }
                Iterator mItr;
                for(mItr = mdlCoordFile.getModels().iterator(); mItr.hasNext() 
                && ensemModelCount < maxEnsemSize-1; ) // -1 b/c we've already added ref
                {
                    Model ensemModel = (Model) mItr.next();
                    
                    parts = Strings.explode(mdlCoordFile.getFile().getName(), '/');
                    basename = parts[parts.length-1];
                    String mName = basename.substring(0,basename.indexOf(".pdb"));
                    //if(splitChains) mName += (String) ensemModel.getChainIDs().iterator().next();
                    ensemModel.getState().setName(mName);
                    
                    if(refDupl(ensemModel, refModel))
                    {
                        System.err.println("Model "+ensemModel.getState().getName()+" "+ensemModel+
                            " is duplicate of ref "+refModel.getState().getName()+" "+refModel+
                            " .. leaving out");
                        continue;
                    }
                    
                    try
                    {
                        AtomState[][] atoms = sup(ensemModel, refModel, mdlCoordFile); // Lesk happens here (if at all)
                        if(atoms == null) continue; // error or rmsd too high
                        ensemModelCount++;
                        ensemModel.setName(""+ensemModelCount);
                        ensemCoordFile.add(ensemModel);
                        ensemAtoms.put(ensemModel, atoms);
                    }
                    catch(IllegalArgumentException ex)
                    {
                        System.err.println(ex.getMessage());
                    }
                }//model
                if(mItr.hasNext() && ensemModelCount == maxEnsemSize-1)
                {
                    System.err.println("Ensemble has reached max size ("+maxEnsemSize+" members) - skipping the rest!");
                    break;
                }
            }
            else System.err.println("Skipping: "+mdlCoordFile+" because no models found");
        }//coord file
        
        if(ensemAtoms.keySet().size() == 0)
        {
            System.err.println("No models left after superposition & trimming/pruning!");
            //System.exit(1);
            throw new IOException("No models left after superposition & trimming/pruning!");
        }
        
        mEnsem = ensemAtoms.keySet().size() + 1; // to include the ref
        
        intersect();
        
        if(splitChains) trimEnsem();
        
        System.err.println("M = "+mEnsem+"\tmodels"); // cols
        System.err.println("N = "+nAtoms+"\tatoms");  // rows/3
        
        if(nAtoms == 0)
        {
            System.err.println("Everything was trimmed!  Try a more similar set of structures.");
            System.exit(1);
        }
    }
//}}}

//{{{ splitChains, chainIsProt
//##############################################################################
    /** Divvies up the input CoordinateFile into single-chain CoordinateFiles. */
    public Collection splitChains(CoordinateFile orig)
    {
        Collection chains = new ArrayList<CoordinateFile>();
        for(Iterator mItr = orig.getModels().iterator(); mItr.hasNext(); )
        {
            Model model = (Model) mItr.next();
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
                        { clone.remove(res); }
                        catch(ResidueException ex)
                        { System.err.println("*** Error removing "+res+" from new chain"+chainID+" model!"); }
                    }
                    // else probably wanna keep this residue b/c in right chain
                }//residue
                CoordinateFile cf = new CoordinateFile();
                cf.add(clone);
                chains.add(cf);
            }//chain
        }//model
        return chains;
    }

    /**
    * Decides the given chain is protein if it has more amino acid residues than anything else.
    * @param chain  a single-model, single-chain CoordinateFile
    */
    public boolean chainIsProt(CoordinateFile chain)
    {
        final String aaNames = 
            "GLY:ALA:VAL:PHE:PRO:MET:ILE:LEU:ASP:GLU:LYS:ARG:SER:THR:TYR:HIS:CYS:ASN:GLN:TRP";
        int countProt = 0;
        int countOth  = 0;
        for(Iterator rItr = chain.getFirstModel().getResidues().iterator(); rItr.hasNext(); )
        {
            Residue r = (Residue) rItr.next();
            if(aaNames.indexOf(r.getName()) != -1)  countProt++;
            else                                    countOth++;
        }
        if(countProt > countOth && countProt >= 30) return true; // try to avoid short, bound peptides
        return false;
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

//{{{ refDupl
//##############################################################################
    /**
    * Tells whether provided model is equivalent to ref model and thus should be 
    * removed to avoid double-counting.  Important when ref was chosen ad hoc.
    * Assumes that if all atoms in <code>m</code> can find some atom in <code>r</code>
    * with exactly the same coordinates, <code>m</code> and <code>r</code> are the same.
    * @param mdl is the model
    * @param ref is the reference structure
    */
    public boolean refDupl(Model mdl, Model ref)
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
    public AtomState[][] sup(Model m1, Model m2, CoordinateFile cf1) throws IllegalArgumentException
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
                m1.getResidues(), s1, m2.getResidues(), s2, superimpose, align, cf1);
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
            
            ArrayList<AtomState> sieved = null; // remember ref atoms sieved below, if any, for later
            if(rmsdLesk >= 0)
            {
                // Eliminate selected atoms one-by-one until RMSD <= cutoff (default: 1A)
                sieved = new ArrayList<AtomState>();
                int sieveCount = 0;
                System.err.println(df.format(superpos.calcRMSD(R))+"\t"
                    +atoms[0].length+"\t[Lesk's sieve start]");
                while(superpos.calcRMSD(R) > rmsdLesk)
                {
                    sieveCount++;
                    SubImpose.sortByLeskSieve(atoms[0], atoms[1]); // messes up order!
                    int len = atoms[0].length - 1; // new length after we drop worst-fitting atom pair
                    sieved.add(atoms[1][len]); // ref
                    
                    AtomState[][] newAtoms = new AtomState[2][len];
                    for(int i = 0; i < 2; i++) for(int j = 0; j < len; j++) newAtoms[i][j] = atoms[i][j];
                    atoms = newAtoms;
                    
                    if(atoms[0].length < 3) throw new IllegalArgumentException(
                        "Can't achieve target rmsd of "+rmsdLesk+"A .. would have to trim to < 3 atoms!");
                    
                    superpos.reset(atoms[1], atoms[0]);
                    R = superpos.superpos();
                    rmsds.put(m1, superpos.calcRMSD(R));
                    System.err.println(df.format(superpos.calcRMSD(R))+"\t"
                        +atoms[0].length+"\t[Lesk's sieve #"+sieveCount+"]");
                }
            }
            else
            {
                if(rmsdMax >= 0 && superpos.calcRMSD(R) > rmsdMax) // RMSD too high
                {
                    System.err.println(df.format(superpos.calcRMSD(R))+"\t"
                        +atoms[0].length+"\t"+superimpose+"\t"+m1.getState().getName()+"\t"+" rmsd>"+rmsdMax+"A - skip");
                    return null;
                }
                else                                               // RMSD OK
                {
                    rmsds.put(m1, superpos.calcRMSD(R));
                    System.err.println(df.format(superpos.calcRMSD(R))+"\t"
                        +atoms[0].length+"\t"+superimpose+"\t"+m1.getState().getName());
                }
            }
            
            // Transform model 1 so transformed coords will be used in the future
            for(Iterator iter = Model.extractOrderedStatesByName(m1).iterator(); iter.hasNext(); )
            {
                AtomState as = (AtomState) iter.next();
                R.transform(as);
            }
            
            // Return remaining selected atoms in original order
            AtomState[][] origAtoms = this.getAtomsForSelection(
                m1.getResidues(), s1, m2.getResidues(), s2, superimpose, align, cf1); // for PCA later
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
    AtomState[][] getAtomsForSelection(Collection res1, ModelState s1, Collection res2, ModelState s2, String selection, Alignment align, CoordinateFile cf1) throws ParseException
    {
        // Get selected atom states from model 1
        Selection sel = Selection.fromString(selection);
        Collection allStates1 = Model.extractOrderedStatesByName(res1, Collections.singleton(s1));
        sel.init(allStates1, cf1);
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
        System.err.println("Trimming ensemble...");
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

/* Principal Component Analysis */

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
        System.err.println("Distorting ensemble via weighted-average PC...");
        for(double frac = -1*maxFrac; frac <= maxFrac; frac += (2*maxFrac)/steps)
        {
            if(verbose) System.err.print("  moving along weighted-average PC x "+df2.format(frac));
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
            if(verbose) System.err.println("Adding to X : ref   model"+ref);
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
                m1.getResidues(), s1, m2.getResidues(), s2, superimpose, align, cf1);
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

//{{{ minimizeBackbone
//##############################################################################
    void minimizeBackbone()
    {
        
        System.err.println("\n*** Backbone minimization not yet implemented! ***\n");
        System.exit(1);
        
        /*
        for(Iterator mIter = pcaCoordFile.getModels().iterator(); mIter.hasNext(); )
        {
            Model m = (Model) mIter.next();
            
            ArrayList points = 
            
            int len = points.size() - 3;
            
            // Create E terms and store in same order as points
            
            bondTerms = new ArrayList();
            angleTerms = new ArrayList();
            
            for(Iterator rIter = model.getResidues().iterator(); rIter.hasNext(); )
            {
                Residue r = (Residue) rIter.next();
                for(ATOM)
                {
                    
                    
                    
                }
            }
            
            
            StateManager stateMan = new StateManager(
                (MutableTuple3[])points.toArray(new MutableTuple3[points.size()]), len);
            stateMan.setBondTerms(bondTerms, 1);
            stateMan.setAngleTerms(angleTerms, 1);
            
            GradientMinimizer minimizer = new GradientMinimizer(stateMan);
            
            int steps = 0;
            boolean done = false;
            while(!done)
            {
                done = !minimizer.step();
                steps++;
                System.err.println("after step "+steps+":");
                System.err.println("  delta energy: "+minimizer.getEnergy+" ("+minimizer.getFracDeltaEnergy*100+"%)");
                System.err.println("  > new energy: "+minimizer.getDeltaEnergy);
            }
        }
        */
    }
//}}}

/* I/O, Odds 'n' Ends */

//{{{ readMdlDir/File, readPdb
//##############################################################################
    /** Reads through several PDB files containing models from a directory. */
    public void readMdlDir(File mf, boolean splitChains)
    {
        mdlDir  = mf;
        mdlFile = null;
        
        File[] children = mdlDir.listFiles();
        int numFilesToRead = children.length;
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
                                if(chainIsProt(cf))
                                {
                                    cf.setFile(f);
                                    cfs.add(cf);
                                }
                            }
                        }
                        else // all input chains kept in their original mdl coord file
                        {
                            inputCoordFile.setFile(f);
                            cfs.add(inputCoordFile);
                        }
                        System.err.println("Extracted model coords from " + inputCoordFile.getFile().getName());
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
                        if(chainIsProt(cf))
                        {
                            cf.setFile(mdlFile);
                            cfs.add(cf);
                        }
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
                System.err.println("Extracted model coords from " + inputCoordFile.getFile().getName());
            }
            catch(IOException ex)
            { System.err.println("*** Error reading models .pdb:" + mdlFile.getName() + "!"); }
        }
        else System.err.println("Selected models file not a .pdb: " + mdlFile.toString());
    }
    
    static public CoordinateFile readPdb(File f) throws IOException
    {
        PdbReader       pdbReader   = new PdbReader();
        CoordinateFile  coordFile   = pdbReader.read(f);
        return coordFile;
    }
//}}}

//{{{ readRefFile, pickRefChain
//##############################################################################
    /** Reads in ref file and chooses a chain. */
    public void readRefFile(File refFile)
    {
        if(refFile.getName().indexOf(".pdb") != -1)
        {
            try
            {
                CoordinateFile inputCoordFile = readPdb(refFile);
                System.err.println("Extracted ref coords from " + inputCoordFile.getFile().getName());
                // Take just one chain, so models will all sup to same chain.
                refCoordFile = pickRefChain(inputCoordFile);
                refCoordFile.setFile(refFile);
            }
            catch(IOException ex)
            { System.err.println("*** Error reading ref .pdb: " + refFile.getName() + "!"); }
        }
        else System.err.println("Selected ref file not a .pdb: " + refFile.toString());
    }
    
    /** Picks which chain in ref file we will superimpose onto. */
    public CoordinateFile pickRefChain(CoordinateFile inputCoordFile)
    {
        ArrayList<CoordinateFile> chains = (ArrayList<CoordinateFile>) splitChains(inputCoordFile);
        if(chains.size() == 0)
            throw new IllegalArgumentException("No chains found in ref file "+refFile+"!");
        
        CoordinateFile retCh   = null;
        String         retChId = null;
        
        // Try and pick a "common" (protein) chain, not just one at random.
        for(int i = 0; i < chains.size(); i++)
        {
            CoordinateFile chain = chains.get(i);
            if(chainIsProt(chain))
            {
                String chainID = (String) chain.getFirstModel().getChainIDs().iterator().next();
                if(chainID.equals(" "))
                { retCh = chain; retChId = chainID; }
                if(chainID.equals("A"))
                { retCh = chain; retChId = chainID; }
                if(chainID.equals("_"))
                { retCh = chain; retChId = chainID; }
            }
        }
        if(retCh == null)
        {
            // Resort to random (protein) chain
            for(int i = 0; i < chains.size(); i++)
            {
                CoordinateFile chain = chains.get(i);
                String chainID = (String) chain.getFirstModel().getChainIDs().iterator().next();
                if(chainIsProt(chain))
                { retCh = chain; retChId = chainID; }
            }
        }
        
        // Return
        if(retCh != null)
        {
            System.err.print("Using chain '"+retChId+"' from ref "+inputCoordFile.getFile().getName());
            if(chains.size() > 1) System.err.print(" - there was/were "+(chains.size()-1)+" other(s)");
            System.err.println();
            return retCh;
        }
        throw new IllegalArgumentException("*** Error: Ref file contains no protein chains - aborting!");
    }
//}}}

//{{{ writeKin
//##############################################################################
    /** 
    * Writes kinemage of ensemble to desired output (e.g. System.out).
    */
    public PrintWriter writeKin(PrintWriter out, CoordinateFile cf, String name)
    {
        if(!append) out.println("@kinemage {"+name+"}");
        out.println("@onewidth");
        
        //double[] rmsdBins  = getRmsdBins(cf);
        //String[] rmsdMstrs = getRmsdMasters(rmsdBins);
        //for(String rmsdMstr : rmsdMstrs) out.println("@master {"+rmsdMstr+"}");
        
        String mMstr = "models";
        out.println("@master {"+mMstr+"}");
        
        //String rMstr = "sup ref: "+refCoordFile.getFirstModel().getState().getName();
        String rMstr = "ref model";
        out.println("@master {"+rMstr+"}");
        
        Logic logic = logicList[0];
        for(Iterator mItr = cf.getModels().iterator(); mItr.hasNext(); )
        {
            Model m = (Model) mItr.next();
            
            if(append && m.getName().equals("0")) continue; // skip ref if appending
            
            String mName = m.getName();
            if(m.getState().getName().indexOf("null") == -1) // true for ensem; false for PCA
                mName += " "+m.getState().getName()+((String)m.getChainIDs().iterator().next());
            
            if(m.getName().equals("0"))
            {
                out.println("@group {"+mName+"} dominant master= {"+rMstr+"}");
            }
            else
            {
                //int rmsdMstrIdx = getRmsdMasterIdx(m, rmsdBins);
                //String rmsdMstr = (rmsdMstrIdx != -1 ? rmsdMstrs[rmsdMstrIdx] : "rmsd ???");
                out.println("@group {"+mName+"} dominant animate master= {"+mMstr+"}");//master= {"+rmsdMstr+"}");
            }
            
            for(Iterator cItr = m.getChainIDs().iterator(); cItr.hasNext(); )
            {
                String c = (String) cItr.next();
                String c2 = (c.equals(" ") ? "_" : c);
                out.println("@subgroup {chain"+c+"} dominant master= {chain"+c2+"}");
                String col = null;
                if(m.getName().equals("0")) col = "yellowtint";
                else col = (color != null ? color : "white"); // model: user choice or white
                logic.printKinemage(out, m, m.getChain(c), mName, col);
            }
        }
        out.flush();
        return out;
    }
//}}}

//{{{ writePdb(s)
//##############################################################################
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
    * Makes a new "ensemble directory" and writes into it a separate PDB 
    * for each superposed ensemble member.
    * Intended for cmdline mode, not for KiNG tool.
    */
    public void writePdbs(CoordinateFile cf)
    {
        // Make sure output directory exists.  If not, make it.
        File pdbsDir = new File(pdbsDirname);
        if(!pdbsDir.exists())
        {
            boolean success = (new File(pdbsDirname).mkdir());
            if(success) System.err.println("Created "+pdbsDirname+"/");
            else
            {
                System.err.println("ERROR: Couldn't create output PDBs directory: "+pdbsDirname+"/");
                System.exit(1);
            }
        }
        
        // Write separate PDB file for each ensemble member
        for(Iterator mItr = cf.getModels().iterator(); mItr.hasNext(); )
        {
            Model m = (Model) mItr.next();
            
            if(refCoordFile.getFile().getName().indexOf(m.getState().getName()) != -1)
            {
                System.err.println("Skipping model from ref file: "+m.getState().getName());
                continue;
            }
            
            CoordinateFile c = new CoordinateFile();
            c.add(m);
            String pdbFilename = pdbsDirname + "/" + m.getState().getName() + "_" + m.getName() + ".pdb";
            File pdbFile = new File(pdbFilename);
            try
            {
                System.err.println("Writing to "+pdbFilename);
                PrintWriter out = new PrintWriter(pdbFile);
                PdbWriter pdbWriter = new PdbWriter(out);
                pdbWriter.writeCoordinateFile(c, new HashMap());
                out.flush();
                pdbWriter.close();
            }
            catch(FileNotFoundException ex)
            { System.err.println("Error writing ensemble PDBs to "+pdbFilename); }
            catch(IOException ex)
            { System.err.println("Error writing ensemble PDBs to "+pdbFilename); }
        }
    }
//}}}

//{{{ getRmsdBins, getRmsdMasters, getRmsdMasterIdx
//##############################################################################
    int bins = 5;

    /** Puts model RMSDs into equally spaced bins. */
    public double[] getRmsdBins(CoordinateFile cf)
    {
        ArrayList<Double> rmsdList = new ArrayList<Double>();
        for(Iterator mItr = cf.getModels().iterator(); mItr.hasNext(); )
        {
            Model m = (Model) mItr.next();
            if(!rmsds.keySet().contains(m)) continue;
            rmsdList.add((Double) rmsds.get(m));
        }
        Collections.sort(rmsdList);
        
        double range = rmsdList.get(rmsdList.size()-1) - rmsdList.get(0);
        double step = range / bins;
        double[] rmsdBins = new double[bins*2];
        double rmsd = rmsdList.get(0);
        for(int i = 0; i < bins; i++)
        {
            rmsdBins[2*i] = rmsd;
            rmsd += step;
            rmsdBins[2*i+1] = rmsd;
        }
        return rmsdBins;
    }

    /**
    * Makes masters corresponding to equally spaced RMSD bins
    * so user can easily turn subsets of models on/off.
    */
    public String[] getRmsdMasters(double[] rmsdBins)
    {
        String[] rmsdMstrs = new String[bins];
        for(int i = 0; i < bins; i++)
        {
            double rmsdLo = rmsdBins[2*i];
            double rmsdHi = rmsdBins[2*i+1];
            rmsdMstrs[i] = "rmsd "+df3.format(rmsdLo)+"-"+df3.format(rmsdHi);
        }
        return rmsdMstrs;
    }

    /**
    * Finds master for this model's RMSD bin.
    * @return  index in rmsdMstrs
    */
    public int getRmsdMasterIdx(Model m, double[] rmsdBins)
    {
        if(!rmsds.keySet().contains(m))
        {
            System.err.println("*** Error: Can't find rmsd for "+m.getState().getName());
            return -1;
        }
        double rmsd = (Double) rmsds.get(m);
        for(int i = 0; i < bins-1; i++)
        {
            double rmsdLo = rmsdBins[2*i];
            double rmsdHi = rmsdBins[2*i+1];
            if(rmsd >= rmsdLo && rmsd <= rmsdHi) return i;
        }
        if(rmsd >= rmsdBins[2*(bins-1)]) return bins-1;
        System.err.println("*** Error: Can't find rmsd MASTER for "+m.getState().getName());
        return -1;
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
        System.err.println("The SupKitchen is busy preparing your order...");
        if(append && !kinOut && pdbsDirname == null)
        {
            System.err.println("Assuming that -append implies -kin");
            kinOut = true;
        }
        if(!splitChains)
        {
            System.err.println("Not splitting models into chains precludes PCA!");
            pcChoice = null;
        }
        else
        {
            System.err.println("Splitting models into chains, so also trimming ensemble to consensus alignment");
        }
        // RMSD trimming
        if(rmsdLesk >= 0)
        {
            System.err.println("Trimming to "+rmsdLesk+" rmsd w/ Lesk sieve");
            if(rmsdMax >= 0)
            {
                rmsdMax = -1;
                System.err.println("(Ignoring straight "+rmsdMax+" rmsd cutoff)");
            }
        }
        else if(rmsdMax >= 0)
            System.err.println("Using straight "+rmsdMax+" rmsd cutoff");
        else
            System.err.println("No trimming/pruning regardless of rmsd");
        System.err.println("Output: "+(kinOut ? "kin" : (pdbsDirname == null ? "PDB" : "PDBs")));
        System.err.println("PCA: "+(pcChoice == null ? "off (just superposition)" : "on!"));
        
        try
        {
            makeSup();
        }
        catch(IOException ex)
        {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        if(pcChoice == null) // not doing PCA
        {
            if(kinOut)                   writeKin(new PrintWriter(System.out), ensemCoordFile, title);
            else if(pdbsDirname != null) writePdbs(ensemCoordFile);
            else                         writePdb(System.out, ensemCoordFile);
            System.err.println("... Your steaming-hot sup is served!");
        }
        else
        {
            doPca();
            if(roundsOfMinimization > 0) minimizeBackbone();
            if(kinOut)                   writeKin(new PrintWriter(System.out), pcaCoordFile, title);
            else if(pdbsDirname != null) writePdbs(ensemCoordFile);
            else                         writePdb(System.out, pcaCoordFile);
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
                System.err.println("\n*** Usage:  SupKitchen models_dir|multimodel_pdb [ref_pdb] ***\n");
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
        else if(flag.equals("-append"))
        {
            append = true;
        }
        else if(flag.equals("-color"))
        {
            color = param;
        }
        else if(flag.equals("-kin"))
        {
            kinOut = true;
        }
        else if(flag.equals("-pdb"))
        {
            kinOut = false;
        }
        else if(flag.equals("-pdbs"))
        {
            kinOut = false;
            pdbsDirname = param;
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
        else if(flag.equals("-min"))
        {
            try
            { roundsOfMinimization = Integer.parseInt(param); }
            catch(NumberFormatException ex)
            { throw new IllegalArgumentException(param+" isn't a number!"); }
        }
        else if(flag.equals("-maxensemsize") || flag.equals("-maxm"))
        {
            try
            { maxEnsemSize = Integer.parseInt(param); }
            catch(NumberFormatException ex)
            { throw new IllegalArgumentException(param+" isn't a number!"); }
        }
        else if(flag.equals("-rmsdmax"))
        {
            try
            { rmsdMax = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { throw new IllegalArgumentException(param+" isn't a number!"); }
        }
        else if(flag.equals("-normsdmax"))
        {
            rmsdMax = -1;
        }
        else if(flag.equals("-lesk"))
        {
            try
            { rmsdLesk = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { throw new IllegalArgumentException(param+" isn't a number!"); }
        }
        else if(flag.equals("-nolesk"))
        {
            rmsdLesk = -1;
        }
        else if(flag.equals("-ca"))
        {
            superimpose = SELECT_CA;
        }
        else if(flag.equals("-bbheavy"))
        {
            superimpose = SELECT_BB_HEAVY;
        }
        else if(flag.equals("-bbheavyh"))
        {
            superimpose = SELECT_BB_HEAVY_H;
        }
        else if(flag.equals("-bbheavycb"))
        {
            superimpose = SELECT_BB_HEAVY_CB;
        }
        else if(flag.equals("-bbheavycbh"))
        {
            superimpose = SELECT_BB_HEAVY_CB_H;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

