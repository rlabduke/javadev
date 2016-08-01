// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.Strings;
//}}}
/**
* <code>AvgStrucGenerator3</code> is a rethink on generating an average model.
* It seems to work <b>much better</b> than my two previous versions: 
* the geometry is pretty realistic, as opposed to being distorted.
* The input PDBs are local motifs of the same length, but not necessarily 
* pre-superimposed.  Conveniently, they can be in fragments that may even be 
* N->C in one example and C->N in another.  The key is a separate structure-
* based alignment for each fragment.
* 
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Wed July 21, 2011.
*/
public class AvgStrucGenerator3 //extends ... implements ...
{
//{{{ CLASS: StructureBasedAligner
//##############################################################################
    static class StructureBasedAligner implements Alignment.Scorer
    {
        public boolean atomsAreEquivalant(AtomState s, AtomState t)
        {
            if(s.getName().equals(t.getName()))
                return true;
            if(s.getName().equals(" H  ") && t.getName().equals(" CD ")  // Xaa, Pro
            || s.getName().equals(" HA ") && t.getName().equals(" HA2")  // Xaa, Gly
            || s.getName().equals(" HA ") && t.getName().equals("1HA ")  // Xaa, Gly
            || s.getName().equals(" CB ") && t.getName().equals(" HA3")  // Xaa, Gly
            || s.getName().equals(" CB ") && t.getName().equals("2HA ")) // Xaa, Gly
                return true;
            if(s.getName().equals(" CD ") && t.getName().equals(" H  ")  // Pro, Xaa
            || s.getName().equals(" HA2") && t.getName().equals(" HA ")  // Gly, Xaa
            || s.getName().equals("1HA ") && t.getName().equals(" HA ")  // Gly, Xaa
            || s.getName().equals(" HA3") && t.getName().equals(" CB ")  // Gly, Xaa
            || s.getName().equals("2HA ") && t.getName().equals(" CB ")) // Gly, Xaa
                return true;
            return false;
        }
        
        // High is good, low is bad.
        public double score(Object a, Object b)
        {
            AtomState s = (AtomState) a;
            AtomState t = (AtomState) b;
            if(s == null || t == null)
                return -1;  // gap
            if(!atomsAreEquivalant(s, t))
                return 0;   // mismatch, even accounting for quasi-structurally-equivalent atom names
            else if(s.distance(t) > 2)
                return 0;   // far away
            else if(s.distance(t) <= 2 && s.distance(t) > 1)
                return 1;   // in the neighborhood
            else if(s.distance(t) <= 1 && s.distance(t) > 0.5)
                return 2;   // close
            else if(s.distance(t) <= 0.5)
                return 3;   // very close
            else
            {
                System.err.println("Not sure how to score "+s+" vs. "+t+"!");
                return 0;
            }
        }
        
        public double open_gap(Object a) { return extend_gap(a); }
        public double extend_gap(Object a) { return score(a, null); }
    }
//}}}

//{{{ CLASS: Range
//##############################################################################
    static class Range
    {
        // Pro doesn't have ' H  ', so use ' CD ' instead; see below.
        // Gly doesn't have ' CB ', so use ' HA3' or '2HA ' instead; see below.
        // Gly doesn't have ' HA ', so use ' HA2' or '1HA ' instead; see below.
        // The averaging will be over all atoms structurally close to the 
        // reference atom, so having one of these quasi-matches, even in the 
        // reference structure, ought to be just fiiine.
        final String bbAtoms = " N  , H  , CA , CB , HA3,2HA , HA , HA2,1HA , C  , O  ";
        //      sorting order:  1111 2222 3333 44444444444444 55555555555555 6666 7777
        
        public File file;
        public Model model;
        public Residue[] residues; // in order of lines in PDB file, so almost always in sequence order
        
        public Range(Model m, ArrayList<Residue> list, File f)
        {
            file = f;
            model = m;
            residues = new Residue[list.size()];
            for(int i = 0; i < list.size(); i++) residues[i] = list.get(i);
        }
        
        public AtomState[] getAtoms() throws AtomException
        {
            ModelState state = model.getState();
            ArrayList<AtomState> atoms = new ArrayList<AtomState>();
            
            for(int i = 0; i < residues.length; i++)
            {
                /*ORDER BY LINES IN PDB FILE:
                for(Iterator aIter = residues[i].getAtoms().iterator(); aIter.hasNext(); )
                {
                    Atom a = (Atom) aIter.next();
                    if((bbAtoms.indexOf(a.getName()) != -1)
                    || (a.getResidue().getName().equals("PRO") && a.getName().equals(" CD ")))
                        atoms.add(state.get(a));
                }ORDER BY STRUCTURAL SEQUENTIALITY:*/
                String[] bbAtomNames = Strings.explode(bbAtoms, ',');
                for(int j = 0; j < bbAtomNames.length; j++)
                {
                    for(Iterator aIter = residues[i].getAtoms().iterator(); aIter.hasNext(); )
                    {
                        Atom a = (Atom) aIter.next();
                        if((a.getName().equals(bbAtomNames[j]))
                        || (j == 1 && a.getName().equals(" CD ") && a.getResidue().getName().equals("PRO")))
                            atoms.add(state.get(a));
                    }
                }
            }
            AtomState[] ret = new AtomState[atoms.size()];
            for(int i = 0; i < atoms.size(); i++) ret[i] = atoms.get(i);
            return ret;
        }
        
        public String toString()
        {
            if(residues == null || residues.length == 0) return "'empty range'";
            else
            {
                String name = "'";
                for(int i = 0; i < residues.length; i++)
                    name += residues[i].nickname()+(i < residues.length-1 ? "-" : "");
                name += (file != null ? " "+file.getName()+"'" : " ??file??'");
                return name;
            }
        }
    }
//}}}

//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    
    boolean  verbose  = false;
    
    ArrayList<File>  files;
    int  maxNumFiles  = 555;
    
    HashMap<AtomState, AtomState[]>  atomMap;  // each ref atom -> list of corresponding atoms 
    
    /** If not NaN, iteratively re-average until we converge on a "stable" average structure */
    double  minRmsf = Double.NaN; 
    
    /** Naively start by using first model or user-recommended model as reference,
    * then adjust by using closest to average model as reference. */
    int  refIndex  = 0;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AvgStrucGenerator3()
    {
        super();
    }
//}}}

//{{{ readCoordinates
//##############################################################################
    public void readCoordinates()
    {
        PdbReader reader = new PdbReader();
        
        // Reference (just for internal bookkeeping -- user need never know)
        Range[] refRanges = null;
        File file = files.get(0);
        try
        {
            CoordinateFile structure = reader.read(file);
            Model model = structure.getFirstModel();
            refRanges = getRanges(model, file);
        }
        catch(IOException ex)
        { System.err.println("Error reading "+file+"!"); }
        
        // Everything that'll get averaged, including the "reference" structure
        atomMap = new HashMap<AtomState, AtomState[]>();
        for(int i = 0; i < files.size(); i++)
        {
            file = files.get(i);
            try
            {
                CoordinateFile structure = reader.read(file);
                Model model = structure.getFirstModel();
                Range[] ranges = getRanges(model, file);
                alignRanges(ranges, refRanges);
            }
            catch(IOException ex)
            { System.err.println("Error reading "+file+"!"); }
            catch(AtomException ex)
            { System.err.println("Error getting atoms from "+file+"!"); }
        }
    }
//}}}

//{{{ getRanges
//##############################################################################
    public Range[] getRanges(Model m, File f)
    {
        ArrayList<Range>   ranges = new ArrayList<Range>();
        ArrayList<Residue> range  = new ArrayList<Residue>();
        
        for(Iterator rIter = m.getResidues().iterator(); rIter.hasNext(); )
        {
            Residue r = (Residue) rIter.next();
            if(r.getPrev(m) == null
            || !r.getChain().equals(r.getPrev(m).getChain())
            || (r.getSequenceInteger() - r.getPrev(m).getSequenceInteger()) > 1)
            {
                // Store completed range & start new one
                if(range.size() > 0) ranges.add(new Range(m, range, f));
                range = new ArrayList<Residue>();
                range.add(r);
            }
            else
            {
                // Continue current range
                range.add(r);
            }
        }
        // Store completed final range
        if(range.size() > 0) ranges.add(new Range(m, range, f));
        
        Range[] ret = new Range[ranges.size()];
        for(int i = 0; i < ranges.size(); i++) ret[i] = ranges.get(i);
        return ret;
    }
//}}}

//{{{ alignRanges
//##############################################################################
    public void alignRanges(Range[] ranges, Range[] refRanges) throws AtomException
    {
        for(Range range : ranges)
        {
            // Find the reference range this range corresponds to
            double    bestScore    = 0;
            Range     bestRefRange = null;
            Alignment bestAlign    = null;
            for(Range refRange : refRanges)
            {
                try
                {
                    // Align residues by atomic structure
                    Alignment align = Alignment.needlemanWunsch(
                        range.getAtoms(), refRange.getAtoms(), new StructureBasedAligner());
                    double score = align.score(new StructureBasedAligner());
                    if(score > bestScore)
                    {
                        bestScore    = score;
                        bestRefRange = refRange;
                        bestAlign    = align;
                    }
                }
                catch(AtomException ex)
                { System.err.println("Error aligning "+range+" to "+refRange+"!"); }
            }
            if(bestScore == 0) throw new IllegalArgumentException(
                "*** Can't use "+range+" because best structure-based"+
                " alignment score is 0!  Structure not pre-superposed? ***");
            if(verbose)
            {
                System.err.println(range+" goes with... "+"\n"+bestRefRange);
                System.err.println("Best atom alignment (score = "+bestScore+"):");
                for(int i = 0; i < bestAlign.a.length; i++)
                {
                    System.err.println("  "+bestAlign.a[i]+" <==> "+bestAlign.b[i]);
                    //AtomState asA = (AtomState) bestAlign.a[i]; // oth, may be null
                    //AtomState asB = (AtomState) bestAlign.b[i]; // ref, never null
                    //Atom aA = (asA != null ? asA.getAtom() : null);
                    //Atom aB = (asB != null ? asB.getAtom() : null);
                    //System.err.println("  "+aA+" <==> "+aB);
                }
            }
            
            // Store coordinates
            for(int i = 0; i < bestAlign.a.length; i++)
            {
                AtomState othAtom = (AtomState) bestAlign.a[i];
                AtomState refAtom = (AtomState) bestAlign.b[i];
                AtomState[] othAtoms = (
                    atomMap.keySet().contains(refAtom) ? 
                    atomMap.get(refAtom) : // retrieve
                    new AtomState[0]);     // initialize
                int len = othAtoms.length;
                AtomState[] othAtomsPlus1 = new AtomState[len+1];
                System.arraycopy(othAtoms, 0, othAtomsPlus1, 0, len);
                othAtomsPlus1[len] = othAtom;
                atomMap.put(refAtom, othAtomsPlus1);
            }
            
            // For the user's records:
            System.err.println("Using "+range+" for averaging");
            if(verbose) System.err.println();
        }
    }
//}}}

//{{{ averageCoordinates
//##############################################################################
    public void averageCoordinates()
    {
        // Make new average model & state
        Model      avgModel = new Model("avg model");
        ModelState avgState = new ModelState();
        int        serial   = 1;
        
        // Create the average atoms
        for(Iterator iter = atomMap.keySet().iterator(); iter.hasNext(); )
        {
            AtomState   refAtom  = (AtomState) iter.next();
            AtomState[] othAtoms = atomMap.get(refAtom);
            
            // Sanity check
            if(verbose)
            {
                System.err.println("Contributors for "+refAtom+":");
                for(int i = 0; i < othAtoms.length; i++)
                    if(othAtoms[i] != null)
                        System.err.println("  "+othAtoms[i]);
                System.err.println();
            }
            
            // Average
            int countNotNull = 0;
            Triple avgAtom = new Triple();
            for(int i = 0; i < othAtoms.length; i++)
            {
                if(othAtoms[i] != null)
                {
                    avgAtom.add(othAtoms[i]);
                    countNotNull++;
                }
            }
            avgAtom.div((double) countNotNull);
            
            // Standard deviation (for pseudo-B)
            double pseudoB = 0;
            for(int i = 0; i < othAtoms.length; i++)
            {
                if(othAtoms[i] != null)
                {
                    pseudoB += Math.pow(Triple.distance(avgAtom, othAtoms[i]), 2);
                }
            }
            pseudoB = Math.sqrt(pseudoB / ((double) countNotNull)) * 100;
            
            // Store for average structure
            refAtom.like(avgAtom);
            refAtom.setTempFactor(pseudoB);
            refAtom.setOccupancy(1.0);
            Residue res = refAtom.getResidue();
            try
            {
                if(!avgModel.contains(res)) avgModel.add(res);
                avgState.add(refAtom);
            }
            catch(ResidueException ex)
            { System.err.println("Error adding "+res+" to average model!"); }
            catch(AtomException ex)
            { System.err.println("Error adding "+refAtom+" to average model state!"); }
            
            /*// Store as alanine for average structure -- DOESN'T WORK!
            String newName = refAtom.getName();
            if(newName.equals(" CD "))                           newName = " H  ";
            if(newName.equals(" HA2") || newName.equals("1HA ")) newName = " HA ";
            if(newName.equals(" HA3") || newName.equals("2HA ")) newName = " CB ";
            Atom newAtom = new Atom(newName);
            AtomState newAS = new AtomState(newAtom, ""+serial);
            serial++;
            Residue res = refAtom.getResidue();
            Residue newRes = new Residue(res, res.getChain(), res.getSegment(), 
                res.getSequenceNumber(), res.getInsertionCode(), "ALA");
            try
            {
                if(!avgModel.contains(newRes)) avgModel.add(newRes);
                // else, hopefully I'm pointing to the right object with newRes...
                newRes.add(newAtom);
                avgState.add(newAS);
            }
            catch(ResidueException ex)
            { System.err.println("Error adding "+res+" to average model!"); }
            catch(AtomException ex)
            { System.err.println("Error adding "+refAtom+" to average model state!"); }*/
        }
        
        // Print the average structure
        if(verbose) System.err.println("Printing average structure...");
        System.out.println("USER  MOD "+files.size()+" contributing structures");
        PdbWriter pdbWriter = new PdbWriter(System.out);
        pdbWriter.writeResidues(avgModel.getResidues(), avgState);
        pdbWriter.close();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if(files.size() < 2) throw new IllegalArgumentException(
            "Only "+files.size()+" files -- need at least 2 to average!");
        
        readCoordinates();
        averageCoordinates();
    }

    public static void main(String[] args)
    {
        AvgStrucGenerator3 mainprog = new AvgStrucGenerator3();
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
            InputStream is = getClass().getResourceAsStream("AvgStrucGenerator3.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AvgStrucGenerator3.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AvgStrucGenerator3");
        System.err.println("Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.");
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
        File file = new File(arg);
        if(file != null)
        {
            if(files == null) files = new ArrayList<File>(); // start
            if(files.size() < maxNumFiles) files.add(file);  // continue
        }
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

