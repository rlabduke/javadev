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
//import driftwood.r3.*;
//}}}
/**
* <code>SequenceTree</code> uses a hierarchical clustering approach plus the
* BLOSUM 62 scoring matrix to make a tree-like graph of sequence relatedness in 2-D.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 28 09:39:34 EST 2003
*/
public class SequenceTree //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df = new DecimalFormat("0.0####");
    static final String COLOR_CODES = "ABCDEFGHIJKLM";
    
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
    class Sequence
    {
        Sequence child1 = null, child2 = null;
        String  seq     = null;
        // either seq is null or children are null, but not both

        BitSet  flags   = new BitSet();     // for marking which files this came from
        int     index   = -1;               // array index for tracking blosum scores
        int     weight  = 1;                // number of sequences clustered into this node
        int     height  = 0;                // max number of branches at or below this
        double  minx = -1, maxx = -1;       // position on the 2-D chart
        
        public Sequence(String sequence)
        {
            seq = sequence;
        }
        
        public Sequence(Sequence s1, Sequence s2)
        {
            child1 = s1;
            child2 = s2;
            weight = child1.weight + child2.weight;
            height = currentHeight++;
            
            flags.clear();
            flags.or(child1.flags);
            flags.or(child2.flags);
        }
        
        public String toString()
        { return (seq == null ? weight+" sequences" : seq); }
        
        public void calculatePositions()
        {
            if(child1 == null || child2 == null)
                minx = maxx = currentX++;
            else
            {
                child1.calculatePositions();
                child2.calculatePositions();
                minx = child1.minx;
                maxx = child2.maxx;
            }
        }
        
        public double getX()
        {
            if(child1 == null || child2 == null)
                return (minx + maxx) / 2.0;
            else
                return (child1.maxx + child2.minx) / 2.0;
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    List    inputFiles  = new ArrayList();
    int     currentX = 0;
    int     currentHeight = 1;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SequenceTree()
    {
        super();
    }
//}}}

//{{{ loadSequences
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
        
        // Assign unique index for every unique sequence
        Sequence[] retVal = (Sequence[]) seqs.values().toArray(  new Sequence[seqs.size()]  );
        for(int i = 0; i < retVal.length; i++)
            retVal[i].index = i;
        
        return retVal;
    }
//}}}

//{{{ blosumScore
//##############################################################################
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

//{{{ renderToKinemage
//##############################################################################
    void renderToKinemage(Sequence top, PrintStream out)
    {
        double scaleY = (double)(top.maxx - top.minx) / (double)top.height;
        
        out.println("@kinemage");
        out.println("@onewidth");
        out.println("@flat");
        int i = 0, k = 0;
        for(Iterator iter = inputFiles.iterator(); iter.hasNext(); )
            out.println("@"+(++i)+"aspect {"+((File)iter.next()).getName()+"}");
        
        out.println("@group {connections}");
        out.println("@subgroup {connections} nobutton");
        out.println("@vectorlist {links} color= gray");
        renderConnections(top, scaleY, out);

        out.println("@group {sequences}");
        out.println("@subgroup {sequences} nobutton");
        out.println("@balllist {balls} color= white radius= 0.8");
        renderNodeBall(top, scaleY, out);
        
        /*out.println("@labellist {labels} color= white off");
        for(i = 0; i < seqs.length; i++)
        {
            out.print("{"+seqs[i].toString()+"} (");
            for(k = 0; k < inputFiles.size(); k++)
            {
                if(seqs[i].flags.get(k))    out.print(COLOR_CODES.charAt( k % COLOR_CODES.length() ));
                else                        out.print("X");
            }
            out.println(") "+df.format(seqs[i].getX())+" "+df.format(seqs[i].getY())+" "+df.format(seqs[i].getZ()));
        }*/
        // Translucent balls, one group per file
        /*String[] clearColors = {"hotpink", "red", "orange", "gold", "yellow", "lime", "green"};
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
        }*/
    }
    
    void renderNodeBall(Sequence node, double scaleY, PrintStream out)
    {
        double x = node.getX(), y = node.height * scaleY;

        out.print("{"+node+"} (");
        for(int k = 0; k < inputFiles.size(); k++)
        {
            if(node.flags.get(k))       out.print(COLOR_CODES.charAt( k % COLOR_CODES.length() ));
            else                        out.print("Z");
        }
        out.println(") "+df.format(x)+" "+df.format(y)+" 0");
        
        if(node.child1 != null) renderNodeBall(node.child1, scaleY, out);
        if(node.child2 != null) renderNodeBall(node.child2, scaleY, out);
    }
    
    void renderConnections(Sequence node, double scaleY, PrintStream out)
    {
        double x1 = node.getX(), y1 = node.height * scaleY;
        
        if(node.child1 != null)
        {
            double x2 = node.child1.getX(), y2 = node.child1.height * scaleY;
            out.println("{"+node+"}P "+df.format(x1)+" "+df.format(y1)+" 0");
            //out.println("{"+node.child1+"} "+df.format(x2)+" "+df.format(y2)+" 0");
            out.print("{"+node.child1+"} (");
            for(int k = 0; k < inputFiles.size(); k++)
            {
                if(node.child1.flags.get(k))    out.print(COLOR_CODES.charAt( k % COLOR_CODES.length() ));
                else                            out.print("X");
            }
            out.println(") "+df.format(x2)+" "+df.format(y2)+" 0");
        }

        if(node.child2 != null)
        {
            double x2 = node.child2.getX(), y2 = node.child2.height * scaleY;
            out.println("{"+node+"}P "+df.format(x1)+" "+df.format(y1)+" 0");
            //out.println("{"+node.child2+"} "+df.format(x2)+" "+df.format(y2)+" 0");
            out.print("{"+node.child2+"} (");
            for(int k = 0; k < inputFiles.size(); k++)
            {
                if(node.child2.flags.get(k))    out.print(COLOR_CODES.charAt( k % COLOR_CODES.length() ));
                else                            out.print("X");
            }
            out.println(") "+df.format(x2)+" "+df.format(y2)+" 0");
        }

        if(node.child1 != null) renderConnections(node.child1, scaleY, out);
        if(node.child2 != null) renderConnections(node.child2, scaleY, out);
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
        int i, j;
        
        // Load all the sequence objects from files or stdin
        Sequence[] seqs;
        if(inputFiles.isEmpty())
            seqs = loadSequences(new InputStream[] {System.in});
        else
        {
            InputStream[] in = new InputStream[ inputFiles.size() ];
            for(i = 0; i < in.length; i++)
                in[i] = new FileInputStream( (File)inputFiles.get(i) );
            seqs = loadSequences(in);
        }
        
        // TODO: check to make sure lengths all match!!
        
        // Calculate all blosum distances - highest is closest
        double[][] blosumDist = new double[seqs.length][seqs.length];
        for(i = 0; i < seqs.length; i++)
        {
            blosumDist[i][i] = -Double.MAX_VALUE; // so we never try merging i with itself!
            for(j = i+1; j < seqs.length; j++)
                blosumDist[i][j] = blosumDist[j][i] = blosumScore(seqs[i], seqs[j]);
        }
        
        // Iterate through, merging the nearest sequences into a cluster
        Sequence topNode = null;
        this.currentHeight = 1;
        while(true)
        {
            // Find closest pair
            int bestI = -1, bestJ = -1;
            double bestDist = -Double.MAX_VALUE;
            for(i = 0; i < seqs.length; i++)
            {
                for(j = i+1; j < seqs.length; j++)
                {
                    if(blosumDist[i][j] > bestDist)
                    {
                        bestDist = blosumDist[i][j];
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            if(bestDist == -Double.MAX_VALUE) break;
            
            // Merge the pair
            Sequence child1 = seqs[bestI];
            Sequence child2 = seqs[bestJ];
            topNode = new Sequence(child1, child2);
            seqs[bestI] = topNode;
            topNode.index = bestI;
            seqs[bestJ] = null;
            
            // Recalculate for bestI (weighted average)
            for(i = 0; i < seqs.length; i++)
            {
                if(i != bestI)
                {
                    blosumDist[i][bestI] = blosumDist[bestI][i] =
                        ((child1.weight * blosumDist[i][bestI]) + (child2.weight * blosumDist[i][bestJ])) / (child1.weight + child2.weight);
                }
                blosumDist[i][bestJ] = blosumDist[bestJ][i] = -Double.MAX_VALUE; // Wipe out info for bestJ
            }
        }
        // seqs[] is now null everywhere except for topNode
        // blosumDist[][] is now == -Double.MAX_VALUE everywhere
        
        this.currentX = 0;
        topNode.calculatePositions();
        
        // Write a kinemage visualization
        renderToKinemage(topNode, System.out);
    }

    public static void main(String[] args)
    {
        SequenceTree mainprog = new SequenceTree();
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
            InputStream is = getClass().getResourceAsStream("SequenceTree.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'SequenceTree.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.minimize.SequenceTree");
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
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

