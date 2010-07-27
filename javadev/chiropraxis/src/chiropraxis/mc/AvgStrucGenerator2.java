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
* <code>AvgStrucGenerator2</code> builds on <code>AvgStrucGenerator</code>.
* Here the input PDBs are <b>local</b> and <b>pre-superposed</b> rather than 
* full structures in their original coordinate frame.
* Conveniently, they can be in fragments that may even by N->C in one example
* and C->N in another.  The key is a separate structure-based alignment for 
* each fragment.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Wed July 14, 2010.
*/
public class AvgStrucGenerator2 //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("###.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    
    boolean verbose = false;
    
    ArrayList<File>  files;
    
    Triple[][]  original;
    Triple[]    averaged;
    double[]    pseudoBs;
    
    File                    refFile;
    Model                   refModel;
    Range[]                 refRanges;
    HashMap<Range,Integer>  refIndices; // reference range => index of row in original
    
    /** Maximum displacement for any one atom in the alignment for which we will
    * still consider the superposition good enough to use that structure in the
    * coordinate averaging */
    double maxDist = 2;
    
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AvgStrucGenerator2()
    {
        super();
        files = new ArrayList<File>();
    }
//}}}

//{{{ getCoords
//##############################################################################
    public void getCoords()
    {
        PdbReader reader = new PdbReader();
        for(int e = 0; e < files.size(); e++)
        {
            File file = files.get(e);
            if(file.isDirectory()) throw new IllegalArgumentException(
                file+" is a directory, not a PDB!");
            try
            {
                CoordinateFile structure = reader.read(file);
                Model model = structure.getFirstModel();
                Range[] ranges = getRanges(model);
                
                if(refFile == null)
                {
                    refFile = file;
                    getRefCoords(model, ranges, e);
                }
                else
                {
                    getOtherCoords(model, ranges, e);
                }
            }
            catch(IOException ex)
            { System.err.println("Error reading "+file+"!"); }
            catch(AtomException ex)
            { System.err.println("Error getting atoms from "+file+"!"); }
        }
        
        if(verbose)
        {
            System.err.println("Original coordinates:");
            for(int a = 0; a < original.length; a++)
            {
                for(int e = 0; e < original[a].length; e++)
                {
                    System.err.print(" "+original[a][e]);
                }
                System.err.println("");
            }
        }
    }
//}}}

//{{{ getRefCoords
//##############################################################################
    public void getRefCoords(Model model, Range[] ranges, int e) throws AtomException
    {
        // Define reference
        refModel = model;
        refRanges = ranges;
        
        int rows = refModel.getResidues().size() * 5; // atoms
        int cols = files.size();                      // examples
        original = new Triple[rows][cols];
        refIndices = new HashMap<Range,Integer>();
        
        // Store each atom's coordinates, 
        // keeping track of which Range points to which atoms
        int x = 0;
        for(Range range : refRanges)
        {
            refIndices.put(range, x);
            
            AtomState[] atoms = range.getAtoms();
            for(int a = 0; a < atoms.length; a++)
            {
                original[x+a][e] = new Triple(atoms[a]);
            }
            x += range.getAtoms().length;
        }
    }
//}}}

//{{{ getOtherCoords
//##############################################################################
    public void getOtherCoords(Model model, Range[] ranges, int e) throws AtomException
    {
        for(Range range : ranges)
        {
            // Find the reference range this range corresponds to
            double bestScore    = Double.NEGATIVE_INFINITY;
            Range  bestRefRange = null;
            for(Range refRange : refRanges)
            {
                // Align residues by atomic structure
                Alignment align = Alignment.needlemanWunsch(
                    refRange.getAtoms(), range.getAtoms(), new StructureBasedAligner());
                double score = align.score(new StructureBasedAligner());
                if(verbose)
                {
                    System.err.println("Score = "+score+" for atom alignment:");
                    for(int j = 0; j < align.a.length; j++)
                        System.err.println("  "+align.a[j]+" <==> "+align.b[j]);
                }
                if(score > bestScore)
                {
                    bestScore    = score;
                    bestRefRange = refRange;
                }
            }
            if(bestScore == 0) throw new IllegalArgumentException(
                "*** Best structure-based alignment score is 0!  Structures not pre-superposed? ***");
            if(verbose) System.err.println(range+" goes with... "+"\n"+bestRefRange+"\n");
            
            // Store coordinates
            int x = refIndices.get(bestRefRange); // row/atom it starts on ("offset")
            AtomState[] atoms = range.getAtoms();
            for(int a = 0; a < atoms.length; a++)
            {
                original[x+a][e] = new Triple(atoms[a]);
            }
        }//per range
    }
//}}}

//{{{ getRanges
//##############################################################################
    public Range[] getRanges(Model m)
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
                if(range.size() > 0) ranges.add(new Range(m, range));
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
        if(range.size() > 0) ranges.add(new Range(m, range));
        
        Range[] ret = new Range[ranges.size()];
        for(int i = 0; i < ranges.size(); i++) ret[i] = ranges.get(i);
        return ret;
    }
//}}}

//{{{ CLASS: Range
//##############################################################################
    static class Range
    {
        final String bbAtoms = " N  , H  ,  CA ,  C  ,  O  "; // Pro doesn't have ' H  '
        
        public Model model;
        public Residue[] residues; // probably in sequence order
        
        public Range(Model m, ArrayList<Residue> list)
        {
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
                for(Iterator aIter = residues[i].getAtoms().iterator(); aIter.hasNext(); )
                {
                    Atom a = (Atom) aIter.next();
                    if(bbAtoms.indexOf(a.getName()) != -1) atoms.add(state.get(a));
                }
            }
            AtomState[] ret = new AtomState[atoms.size()];
            for(int i = 0; i < atoms.size(); i++) ret[i] = atoms.get(i);
            return ret;
        }
        
        public String toString()
        {
            if(residues == null || residues.length == 0) return "empty range";
            else
            {
                String name = "'";
                for(int i = 0; i < residues.length; i++)
                    name += residues[i].nickname()+(i < residues.length-1 ? "-" : "'");
                return name;
            }
            //return "'"+residues[0]+"' to '"+residues[residues.length-1]+"'";
        }
    }
//}}}

//{{{ CLASS: StructureBasedAligner
//##############################################################################
    static class StructureBasedAligner implements Alignment.Scorer
    {
        // High is good, low is bad.
        public double score(Object a, Object b)
        {
            AtomState s = (AtomState) a;
            AtomState t = (AtomState) b;
            if(s == null || t == null)
                return -1;  // gap
            else if(!s.getAtom().getName().equals(t.getAtom().getName()))
                return 0;   // mismatch
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

//{{{ averageCoords
//##############################################################################
    public void averageCoords()
    {
        averaged = new Triple[original.length];
        pseudoBs = new double[original.length];
        
        for(int a = 0; a < original.length; a++)
        {
            // average
            averaged[a] = new Triple(0,0,0);
            int count = 0;
            for(int e = 0; e < original[a].length; e++)
            {
                if(original[a][e] != null)
                {
                    averaged[a].add(original[a][e]);
                    count++;
                }
            }
            averaged[a].div(1.0*count);
            
            // standard deviation (for pseudo-B)
            pseudoBs[a] = 0;
            for(int e = 0; e < original[a].length; e++)
            {
                if(original[a][e] != null)
                    pseudoBs[a] += Math.pow(Triple.distance(averaged[a], original[a][e]), 2);
            }
            pseudoBs[a] = Math.sqrt(pseudoBs[a]/(1.0*count)) * 100;
        }
    }
//}}}

//{{{ printAvgStruc
//##############################################################################
    public void printAvgStruc()
    {
        try
        {
            // Make new average model+state
            Model      avgModel = new Model("avg model");
            ModelState avgState = new ModelState();
            int count = 0;
            
            // Enumerate atoms in order, change their coords to averaged coords,
            // and add them and their residues to the new average model+state
            for(Range range : refRanges)
            {
                int x = refIndices.get(range); // offset
                AtomState[] atoms = range.getAtoms();
                for(int a = 0; a < atoms.length; a++)
                {
                    AtomState atom = atoms[a];
                    atom.setX(averaged[x+a].getX());
                    atom.setY(averaged[x+a].getY());
                    atom.setZ(averaged[x+a].getZ());
                    atom.setTempFactor(pseudoBs[a]);
                    atom.setOccupancy(1.0);
                    
                    Residue res = atom.getAtom().getResidue();
                    if(!avgModel.contains(res)) avgModel.add(res);
                    avgState.add(atom);
                }
            }
            
            // Print out new average model+state as a PDB
            if(verbose) System.err.println("Printing average structure...");
            System.out.println("USER  MOD "+files.size()+" contributing structures");
            PdbWriter pdbWriter = new PdbWriter(System.out);
            pdbWriter.writeResidues(avgModel.getResidues(), avgState);
            pdbWriter.close();
        }
        catch(ResidueException ex)
        { System.err.println("Error adding a residue to average structure!"); }
        catch(AtomException ex)
        { System.err.println("Error adding an atom to average structure!"); }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if(files.isEmpty()) throw new IllegalArgumentException("Must supply at least two input PDBs!");
        
        getCoords();
        
        averageCoords(); // XX-TODO: make recursive w/ maxDist (?)
        
        printAvgStruc();
    }

    public static void main(String[] args)
    {
        AvgStrucGenerator2 mainprog = new AvgStrucGenerator2();
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
            InputStream is = getClass().getResourceAsStream("AvgStrucGenerator2.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AvgStrucGenerator2.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AvgStrucGenerator2");
        System.err.println("Copyright (C) 2008 by Daniel A. Keedy. All rights reserved.");
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
        if(file != null) files.add(file);
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
        else if(flag.equals("-distcutoff") || flag.equals("-maxdist"))
        {
            try
            { maxDist = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { System.err.println("Couldn't parse "+param+" as a double for maxDist"); }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

