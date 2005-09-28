// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.dezymer;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.r3.*;
import chiropraxis.forcefield.*;
//}}}
/**
* <code>SequenceSpacer</code> uses pseudo-physical potentials and
* numerical minimization to lay out a graph of sequence relatedness in 3-D.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 28 09:39:34 EST 2003
*/
public class SequenceSpacer //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df = new DecimalFormat("0.0####");
    
    static final String BLOSUM_INDICES = "CSTPAGNDEQHRKMILVFYW";
    static final int[][] BLOSUM_62 = 
    {   { 9 }, // Cys
        { -1, 4 },
        { -1, 1, 5 },
        { -3, -1, -1, 7 },
        { 0, 1, 0, -1, 4 },
        { -3, 0, -2, -2, 0, 6 }, // Gly
        { -3, 1, 0, -2, -2, 0, 6 },
        { -3, 0, -1, -1, -2, -1, 1, 6 },
        { -4, 0, -1, -1, -1, -2, 0, 2, 5 },
        { -3, 0, -1, -1, -1, -2, 0, 0, 2, 5 },
        { -3, -1, -2, -2, -2, -2, 1, -1, 0, 0, 8 }, // His
        { -3, -1, -1, -2, -1, -2, 0, -2, 0, 1, 0, 5 },
        { -3, 0, -1, -1, -1, -2, 0, -1, 1, 1, -1, 2, 5},
        { -1, -1, -1, -2, -1, -3, -2, -3, -2, 0, -2, -1, -1, 5 },
        { -1, -1, -1, -3, -1, -4, -3, -3, -3, -3, -3, -3, -3, 1, 4 },
        { -1, -2, -1, -3, -1, -4, -3, -4, -3, -2, -3, -2, -2, 2, 2, 4 }, // Leu
        { -1, -2, 0, -2, 0, -3, -3, -3, -2, -2, -3, -3, -2, 1, 3, 1, 4 },
        { -2, -2, -2, -4, -2, -3, -3, -3, -3, -3, -1, -3, -3, 0, 0, 0, -1, 6 },
        { -2, -2, -2, -3, -2, -3, -2, -3, -2, -1, 2, -2, -2, -1, -1, -1, -1, 3, 7 },
        { -2, -3, -2, -4, -3, -2, -4, -4, -3, -2, -2, -3, -3, -1, -3, -2, -3, 1, 2, 11 }    };
//}}}

//{{{ CLASS: Sequence
//##############################################################################
    static class Sequence extends Triple implements Comparable
    {
        String      seq;
        BitSet      flags   = new BitSet();
        Collection  neighbors;  // other Sequences within 1 mutation of this one
        int         degree_1;   // number of neighbors
        int         degree_2;   // sum of n.degree_1 for n in neighbors
        
        public Sequence(String sequence)
        {
            super(10*Math.random(), 10*Math.random(), 10*Math.random());
            seq = sequence;
        }
        
        public String toString()
        { return seq; }
        
        public void neighborGraph(Set graph)
        {
            if(graph.contains(this)) return;
            graph.add(this);
            for(Iterator iter = neighbors.iterator(); iter.hasNext(); )
                ((Sequence) iter.next()).neighborGraph(graph);
        }
        
        public void calculateDegrees()
        {
            this.degree_1 = this.neighbors.size();
            this.degree_2 = 0;
            for(Iterator iter = neighbors.iterator(); iter.hasNext(); )
            {
                Sequence neighbor = (Sequence) iter.next();
                degree_2 += neighbor.neighbors.size();
            }
        }
        
        public int compareTo(Object o)
        {
            Sequence that = (Sequence) o;
            if(this.degree_1 != that.degree_1)
                return this.degree_1 - that.degree_1;
            else
                return this.degree_2 - that.degree_2;
        }
        
        public String printNeighborhood(Collection graph)
        {
            StringBuffer buf = new StringBuffer();
            for(int i = 0; i < seq.length(); i++)
            {
                buf.append("  ").append(seq.charAt(i));
                Set others = new HashSet();
                for(Iterator iter = graph.iterator(); iter.hasNext(); )
                    others.add(new Character(((Sequence)iter.next()).seq.charAt(i)));
                others.remove(new Character(seq.charAt(i)));
                for(Iterator iter = others.iterator(); iter.hasNext(); )
                    buf.append(iter.next());
                buf.append("\n");
            }
            return buf.toString();
        }
    }
//}}}

//{{{ CLASS: CollectionSizeComparator
//##############################################################################
    static public class CollectionSizeComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            return ((Collection)o1).size() - ((Collection)o2).size();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    List    inputFiles  = new ArrayList();
    int     numTries    = 5;
    double  pow         = 1.0; // controls spring fall-off
    boolean useBlosum   = false;
    
    Sequence[] seqs;
    int[] mutationDist;
    Collection neighborGraphs; // Collection of Sets of Sequences
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SequenceSpacer()
    {
        super();
    }
//}}}

//{{{ loadSequences, seqDistance
//##############################################################################
    Sequence[] loadSequences(InputStream[] inp) throws IOException
    {
        Map     seqs = new HashMap(); // <String, Sequence>
        String  s;
        for(int k = 0; k < inp.length; k++)
        {
            LineNumberReader    in = new LineNumberReader(new InputStreamReader(inp[k]));
            while((s = in.readLine()) != null)
            {
                if(s.length() > 0)
                {
                    Sequence seq = (Sequence)seqs.get(s);
                    if(seq == null)
                    {
                        seq = new Sequence(s);
                        seqs.put(s, seq);
                    }
                    seq.flags.set(k); // mark it as coming from this stream
                }
            }
        }
        
        return (Sequence[]) seqs.values().toArray(  new Sequence[seqs.size()]  );
    }
    
    /** Returns the number of point mutations to convert seq1 to seq2 */
    int seqDistance(Sequence seq1, Sequence seq2)
    {
        String s1 = seq1.toString();
        String s2 = seq2.toString();
        int result = 0, len = s1.length();
        
        for(int i = 0; i < len; i++)
        {
            if(s1.charAt(i) != s2.charAt(i))
                result++;
        }
        
        return result;
    }
//}}}

//{{{ blosumDistance, blosumScore
//##############################################################################
    /**
    * Returns the difference between the average maximum BLOSUM score
    * for 1 and 2 (i.e., the average of testing each against itself)
    * and the actual BLOSUM score for 1 and 2, in log-odds units.
    *
    * <p>This meets the following criteria:<ol>
    * <li>Symmetry: d(a,b) == d(b,a)</li>
    * <li>Identity: d(a,a) == d(b,b) == 0</li>
    * </ol>
    */
    double blosumDistance(Sequence seq1, Sequence seq2)
    {
        double mismatch     = blosumScore(seq1, seq2);
        double identical    = (blosumScore(seq1, seq1) + blosumScore(seq2, seq2)) / 2.0;
        
        return (identical - mismatch) / 7; // scales so size of balls is still OK
    }
    
    /** Range is [-4, +11] per amino acid in the input. See Henikoff &amp; Henikoff (1992) PNAS 92:10915 */
    int blosumScore(Sequence seq1, Sequence seq2)
    {
        String s1 = seq1.toString();
        String s2 = seq2.toString();
        int result = 0, len = s1.length();
        int i = 0, j = 0, k = 0;
        
        for(k = 0; k < len; k++)
        {
            try {
            i = BLOSUM_INDICES.indexOf(s1.charAt(k));
            j = BLOSUM_INDICES.indexOf(s2.charAt(k));
            if(i < 0 || j < 0) {} // do nothing; unknown AA code
            else if(i < j)  result += BLOSUM_62[j][i];
            else            result += BLOSUM_62[i][j];
            } catch(IndexOutOfBoundsException ex) {
                ex.printStackTrace();
                System.err.println("i = "+i+"; j = "+j+"; k = "+k);
                System.err.println("Comparing "+s1+" ("+s1.length()+") and "+s2+" ("+s2.length()+")");
                System.exit(1);
            }
        }
        
        return result;
    }
//}}}

//{{{ createRestraints
//##############################################################################
    /** Fills in mutationDist and returns a StateManager. */
    StateManager createRestraints()
    {
        this.mutationDist = new int[ seqs.length*seqs.length ];

        StateManager stateman = new StateManager(seqs, seqs.length);
        Collection bondTerms = new ArrayList();
        for(int i = 0; i < seqs.length; i++)
        {
            mutationDist[seqs.length*i + i] = 0;
            for(int j = i+1; j < seqs.length; j++)
            {
                int d = seqDistance(seqs[i], seqs[j]);
                mutationDist[seqs.length*i + j] = d;
                mutationDist[seqs.length*j + i] = d;
                
                if(useBlosum)
                {
                    double b = blosumDistance(seqs[i], seqs[j]);
                    bondTerms.add(new BondTerm(i, j, b, 1.0/Math.pow(b, pow)));
                }
                else
                    bondTerms.add(new BondTerm(i, j, d, 1.0/Math.pow(d, pow)));
            }
        }
        stateman.setBondTerms(bondTerms);
        return stateman;
    }
//}}}

//{{{ findLowestEnergy, randomizePositions
//##############################################################################
    double findLowestEnergy(StateManager stateman)
    {
        DecimalFormat df = new DecimalFormat("0.0000E0");
        double      bestEnergy  = Double.POSITIVE_INFINITY;
        Triple[]    bestState   = new Triple[seqs.length];
        for(int i = 0; i < bestState.length; i++) bestState[i] = new Triple();
        
        for(int k = 0; k < numTries; k++)
        {
            randomizePositions(seqs, 10.0);
            stateman.setState(); // sucks in coords of Sequence objects again
            GradientMinimizer min = new GradientMinimizer(stateman);
            long time = System.currentTimeMillis();
            for(int i = 1; i <= 100; i++)
            {
                if(!min.step()) break;
                if(min.getFracDeltaEnergy() > -1e-4) break;
            }
            time = System.currentTimeMillis() - time;
            System.err.println(time+" ms; E = "+df.format(min.getEnergy()));
            if(min.getEnergy() < bestEnergy)
            {
                bestEnergy  = min.getEnergy();
                stateman.getState(bestState);
            }
        }
        stateman.setState(bestState);
        stateman.getState(); // read out the new coordinates into members of seqs!!
        
        System.err.println();
        System.err.println("Best energy:  "+df.format(bestEnergy));
        
        return bestEnergy;
    }

    void randomizePositions(Sequence[] seqs, double boxSize)
    {
        for(int i = 0; i < seqs.length; i++)
        {
            seqs[i].setX(Math.random()*boxSize);
            seqs[i].setY(Math.random()*boxSize);
            seqs[i].setZ(Math.random()*boxSize);
        }
    }
//}}}

//{{{ findNeighbors, makeNeighborGraphs
//##############################################################################
    void findNeighbors()
    {
        for(int i = 0; i < seqs.length; i++)
        {
            Collection neighbors = seqs[i].neighbors = new ArrayList();
            for(int j = 0; j < seqs.length; j++)
            {
                if(mutationDist[seqs.length*i + j] == 1)
                    neighbors.add(seqs[j]);
            }
        }
        // Once all neighbors are known, calculate degrees of connectedness.
        for(int i = 0; i < seqs.length; i++) seqs[i].calculateDegrees();
    }
    
    Collection makeNeighborGraphs()
    {
        Set allSeq = new HashSet(Arrays.asList(seqs));
        ArrayList graphs = new ArrayList(); // Collection of Sets of Sequences
        while(allSeq.size() > 0)
        {
            Sequence s = (Sequence) allSeq.iterator().next();
            Set graph = new HashSet();
            s.neighborGraph(graph);
            graphs.add(graph);
            allSeq.removeAll(graph);
        }
        Collections.sort(graphs, new ReverseComparator(new CollectionSizeComparator()));
        
        return graphs;
    }
//}}}

//{{{ renderLinkedViews
//##############################################################################
    void renderLinkedViews(Sequence[] seqs, Collection neighborGraphs, PrintStream out)
    {
        final int maxViews = 10;
        out.println("@text");
        int i = 0;
        for(Iterator iter = neighborGraphs.iterator(); iter.hasNext() && i < maxViews; i++)
        {
            ArrayList ng = new ArrayList((Collection) iter.next());
            Collections.sort(ng, Collections.reverseOrder());
            Sequence first = (Sequence) ng.get(0);
            out.println("*{v="+(i+2)+"}*   "+ng.size()+" member cluster with "+first.degree_1+" neighbors to "+first.seq);
            out.println(first.printNeighborhood(ng));
        }
        out.println("@1viewid {Overview}"); // other params auto determined on kinemage load
        i = 0;
        for(Iterator iter = neighborGraphs.iterator(); iter.hasNext() && i < maxViews; i++)
        {
            ArrayList ng = new ArrayList((Collection) iter.next());
            Collections.sort(ng, Collections.reverseOrder());
            Sequence first = (Sequence) ng.get(0);
            out.println("@"+(i+2)+"viewid {"+first.seq+"}");
            out.println("@"+(i+2)+"center "+first.format(df));
            out.println("@"+(i+2)+"span 4");
        }
    }
//}}}

//{{{ renderToKinemage
//##############################################################################
    void renderToKinemage(Sequence[] seqs, int[] mutationDist, PrintStream out)
    {
        final String COLOR_CODES = "MABCDEF";
        
        out.println("@onewidth");
        int i = 0, k = 0;
        for(Iterator iter = inputFiles.iterator(); iter.hasNext(); )
            out.println("@"+(++i)+"aspect {"+((File)iter.next()).getName()+"}");
        out.println("@group {sequences}");
        out.println("@subgroup {sequences} nobutton");
        
        out.println("@labellist {labels} color= white off");
        for(i = 0; i < seqs.length; i++)
        {
            out.print("{"+seqs[i].toString()+"} (");
            for(k = 0; k < inputFiles.size(); k++)
            {
                if(seqs[i].flags.get(k))    out.print(COLOR_CODES.charAt( k % COLOR_CODES.length() ));
                else                        out.print("X");
            }
            out.println(") "+df.format(seqs[i].getX())+" "+df.format(seqs[i].getY())+" "+df.format(seqs[i].getZ()));
        }
        
        out.println("@balllist {balls} color= white radius= 0.1");
        for(i = 0; i < seqs.length; i++)
        {
            out.print("{"+seqs[i].toString()+"} (");
            for(k = 0; k < inputFiles.size(); k++)
            {
                if(seqs[i].flags.get(k))    out.print(COLOR_CODES.charAt( k % COLOR_CODES.length() ));
                else                        out.print("X");
            }
            out.println(") "+df.format(seqs[i].getX())+" "+df.format(seqs[i].getY())+" "+df.format(seqs[i].getZ()));
        }
        
        out.println("@group {connections}");
        out.println("@subgroup {connections} nobutton");
        renderConnections(1, "white",       seqs, mutationDist, out);
        renderConnections(2, "bluetint",    seqs, mutationDist, out);
        renderConnections(3, "sky off",     seqs, mutationDist, out);
        renderConnections(4, "blue off",    seqs, mutationDist, out);
        renderConnections(5, "purple off",  seqs, mutationDist, out);
        
        // Translucent balls, one group per file
        String[] clearColors = {"hotpink", "red", "orange", "gold", "yellow", "lime", "green"};
        k = 0;
        for(Iterator iter = inputFiles.iterator(); iter.hasNext(); k++)
        {
            out.println("@group {"+((File)iter.next()).getName()+"} dominant animate");
            double radius = 0.1 + 0.02*(k % 5);
            out.println("@balllist {balls} color= "+clearColors[k%clearColors.length]+" radius= "+df.format(radius)+" nohighlight alpha= 0.4");
            for(i = 0; i < seqs.length; i++)
            {
                if(seqs[i].flags.get(k))
                    out.println("{"+seqs[i].toString()+"} "+df.format(seqs[i].getX())+" "+df.format(seqs[i].getY())+" "+df.format(seqs[i].getZ()));
            }
        }
    }
//}}}

//{{{ renderConnections
//##############################################################################
    void renderConnections(int dist, String color, Sequence[] seqs, int[] mutationDist, PrintStream out)
    {
        out.println("@vectorlist {+"+dist+"} color= "+color);
        for(int i = 0; i < seqs.length; i++)
        {
            for(int j = i; j < seqs.length; j++)
            {
                int d = mutationDist[seqs.length*i + j];
                if(d == dist)
                {
                    StringBuffer aspects = new StringBuffer(inputFiles.size()+2);
                    aspects.append("(");
                    for(int k = 0; k < inputFiles.size(); k++)
                    {
                        if(seqs[i].flags.get(k) && seqs[j].flags.get(k))    aspects.append(" "); // default color
                        else                                                aspects.append("X"); // gray
                    }
                    aspects.append(")");
                    
                    out.println("{"+seqs[i].toString()+"} P "+aspects+" "+seqs[i].format(df));
                    out.println("{"+seqs[j].toString()+"} "+aspects+" "+seqs[j].format(df));
                }
            }
        }
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
    public void Main() throws IOException
    {
        // Load all the sequence objects from files or stdin
        if(inputFiles.isEmpty())
            this.seqs = loadSequences(new InputStream[] {System.in});
        else
        {
            InputStream[] in = new InputStream[ inputFiles.size() ];
            for(int i = 0; i < in.length; i++)
                in[i] = new FileInputStream( (File)inputFiles.get(i) );
            this.seqs = loadSequences(in);
        }
        
        // TODO: check to make sure lengths all match!!
        
        // Set up all of our harmonic restraints
        StateManager stateman = createRestraints();
        
        // Run the minimization to position the sequences in R3
        double bestEnergy = findLowestEnergy(stateman);
        
        // Find neighbors for all sequences
        findNeighbors();
        
        // Group sequences into connected graphs (edges are point mutations)
        this.neighborGraphs = makeNeighborGraphs();
        System.err.println(seqs.length+" sequences form "+neighborGraphs.size()+" connected graphs");
        
        // Write a kinemage visualization
        System.out.println("@kinemage");
        renderLinkedViews(seqs, neighborGraphs, System.out);
        renderToKinemage(seqs, mutationDist, System.out);
    }

    public static void main(String[] args)
    {
        SequenceSpacer mainprog = new SequenceSpacer();
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
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println("*** I/O error: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("SequenceSpacer.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SequenceSpacer.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.minimize.SequenceSpacer");
        System.err.println("Copyright (C) 2003 by Ian W. Davis. All rights reserved.");
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
        inputFiles.add(new File(arg));
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-tries"))
        {
            try { numTries = Integer.parseInt(param); }
            catch(NumberFormatException ex) {}
        }
        else if(flag.equals("-power"))
        {
            try { pow = Double.parseDouble(param); }
            catch(NumberFormatException ex) {}
        }
        else if(flag.equals("-blosum"))
        {
            useBlosum = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

