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
import driftwood.util.Strings;
import chiropraxis.forcefield.*;
//}}}
/**
* <code>PageSpacer</code> uses pseudo-physical potentials and
* numerical minimization to lay out a graph of sequence relatedness in 3-D.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Oct 28 09:39:34 EST 2003
*/
public class PageSpacer //extends ... implements ...
{
//{{{ Constants
    static DecimalFormat df = new DecimalFormat("0.0####");
//}}}

//{{{ CLASS: Page, Link
//##############################################################################
    static class Page extends Triple
    {
        final String name;
        final int index;
        
        public Page(String n, int i)
        { super(); name = n; index = i; }
        
        public String toString()
        { return name; }
    }
    
    static class Link
    {
        Page        from;
        Page        to;
        int         total_hits;
        double      frac_sess;
        double      avg_time;
        
        public Link()
        {}
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    List        inputFiles  = new ArrayList();
    int         numTries    = 5;
    double      repulsion   = 1.0;
    double      pow         = 1.0; // controls spring fall-off
    
    Page[]      pages;
    Collection  links;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public PageSpacer()
    {
        super();
    }
//}}}

//{{{ loadLinks
//##############################################################################
    void loadLinks(InputStream[] inp) throws IOException
    {
        this.links = new ArrayList();
        Map pgs = new UberMap(); // <String, Page>
        String  s;
        
        for(int k = 0; k < inp.length; k++)
        {
            LineNumberReader in = new LineNumberReader(new InputStreamReader(inp[k]));
            while((s = in.readLine()) != null)
            {
                String[] f = Strings.explode(s, ':');
                if(s.length() > 0 && !s.startsWith("#") && f.length >= 5)
                {
                    Page from = (Page) pgs.get(f[0]);
                    if(from == null)
                    {
                        from = new Page(f[0], pgs.size());
                        pgs.put(f[0], from);
                    }
                    
                    Page to = (Page) pgs.get(f[1]);
                    if(to == null)
                    {
                        to = new Page(f[1], pgs.size());
                        pgs.put(f[1], to);
                    }
                    
                    try
                    {
                        Link link = new Link();
                        link.from           = from;
                        link.to             = to;
                        link.total_hits     = Integer.parseInt(f[2]);
                        link.frac_sess      = Double.parseDouble(f[3]);
                        link.avg_time       = Double.parseDouble(f[4]);
                        links.add(link);
                    }
                    catch(NumberFormatException ex)
                    { System.err.println(ex.getMessage()); }
                }
            }
        }
        
        this.pages = (Page[]) pgs.values().toArray(new Page[pgs.size()]);
        for(int i = 0; i < pages.length; i++) if(pages[i].index != i) System.err.println("Page indexes are wrong!");
    }
//}}}

//{{{ createRestraints, scaleTime
//##############################################################################
    /** Returns a StateManager. */
    StateManager createRestraints()
    {
        StateManager stateman = new StateManager(pages, pages.length);
        Collection bondTerms = new ArrayList();
        for(Iterator iter = links.iterator(); iter.hasNext(); )
        {
            Link link = (Link) iter.next();
            bondTerms.add(new BondTerm(link.from.index, link.to.index, scaleTime(link.avg_time), 1.0));
            //bondTerms.add(new BondTerm(link.from.index, link.to.index, link.avg_time, 1.0/Math.pow(link.avg_time, pow)));
        }
        stateman.setBondTerms(bondTerms);
        
        int[] atomTypes = new int[pages.length]; // all pages are atom type 0
        NonbondedTerm nbTerm = new NonbondedTerm(atomTypes, 10*60, pages.length); // eval up to 10 minutes away
        nbTerm.setQ(0, 0, repulsion); // repulsion on all atom0 - atom0 interactions
        stateman.setNbTerms(Collections.singleton(nbTerm));
        
        return stateman;
    }
    
    double scaleTime(double t)
    {
        //return t;
        //return Math.sqrt(t);
        return Math.log(Math.max(1, t));
    }
//}}}

//{{{ findLowestEnergy, randomizePositions
//##############################################################################
    double findLowestEnergy(StateManager stateman)
    {
        DecimalFormat df = new DecimalFormat("0.0000E0");
        double      bestEnergy  = Double.POSITIVE_INFINITY;
        Triple[]    bestState   = new Triple[pages.length];
        for(int i = 0; i < bestState.length; i++) bestState[i] = new Triple();
        
        for(int k = 0; k < numTries; k++)
        {
            randomizePositions(pages, 10.0);
            stateman.setState(); // sucks in coords of Page objects again
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

    void randomizePositions(Page[] pgs, double boxSize)
    {
        for(int i = 0; i < pgs.length; i++)
        {
            pgs[i].setX(Math.random()*boxSize);
            pgs[i].setY(Math.random()*boxSize);
            pgs[i].setZ(Math.random()*boxSize);
        }
    }
//}}}

//{{{ plotPages
//##############################################################################
    void plotPages(Page[] pages, PrintStream out)
    {
        out.println("@subgroup {pages}");
        out.println("@balllist {balls} radius= 1.0 color= sea off");
        for(int i = 0; i < pages.length; i++)
        {
            out.println("{"+pages[i]+"} "+pages[i].format(df));
        }
        out.println("@labellist {labels} color= sea");
        for(int i = 0; i < pages.length; i++)
        {
            out.println("{"+pages[i]+"} "+pages[i].format(df));
        }
    }
//}}}

//{{{ plotLinks
//##############################################################################
    void plotLinks(Collection links, PrintStream out)
    {
        double maxHits = 0;
        for(Iterator iter = links.iterator(); iter.hasNext(); )
        {
            Link link = (Link) iter.next();
            maxHits = Math.max(maxHits, link.total_hits);
        }
        
        //String[] linkColors = {"blue", "purple", "magenta", "hotpink", "red", "orange", "gold", "yellow", "yellowtint", "white"};
        String[] linkColors = {"blue", "sky", "bluetint", "white", "peachtint", "peach", "orange"};
        Triple u = new Triple(), v = new Triple(), mid = new Triple();
        
        out.println("@subgroup {links}");
        //out.println("@vectorlist {vectors}");
        out.println("@arrowlist {arrows}");
        for(Iterator iter = links.iterator(); iter.hasNext(); )
        {
            Link link = (Link) iter.next();
            // This should never happen, BUT...
            if(link.from == link.to) continue;
            
            int weight = (int) Math.ceil(7.0 * link.total_hits / maxHits);
            if(weight > 7) weight = 7;
            int color = (int) Math.floor(linkColors.length * link.frac_sess);
            if(color == linkColors.length) color--;
            
            u.likeVector(link.from, link.to);
            v.likeOrthogonal(u);
            v.mult(0.03 * u.mag());
            if(link.from.toString().compareTo(link.to.toString()) < 0)
                v.neg();
            mid.likeMidpoint(link.from, link.to);
            mid.add(v);
            
            out.print("{"+link.from+"}P "+link.from.format(df));
            out.println(" {x}L width"+weight+" "+linkColors[color]+" "+mid.format(df));
            out.println(" {"+link.to+"}L width"+weight+" "+linkColors[color]+" "+link.to.format(df));
        }

        out.println("@labellist {hits} color= greentint off");
        for(Iterator iter = links.iterator(); iter.hasNext(); )
        {
            Link link = (Link) iter.next();
            // This should never happen, BUT...
            if(link.from == link.to) continue;
            
            u.likeVector(link.from, link.to);
            v.likeOrthogonal(u);
            v.mult(0.06 * u.mag());
            if(link.from.toString().compareTo(link.to.toString()) < 0)
                v.neg();
            mid.likeMidpoint(link.from, link.to);
            mid.add(v);
            
            out.println("{"+link.total_hits+"} "+mid.format(df));
        }

        out.println("@labellist {% users} color= greentint off");
        for(Iterator iter = links.iterator(); iter.hasNext(); )
        {
            Link link = (Link) iter.next();
            // This should never happen, BUT...
            if(link.from == link.to) continue;
            
            u.likeVector(link.from, link.to);
            v.likeOrthogonal(u);
            v.mult(0.06 * u.mag());
            if(link.from.toString().compareTo(link.to.toString()) < 0)
                v.neg();
            mid.likeMidpoint(link.from, link.to);
            mid.add(v);
            
            out.println("{"+df.format(100 * link.frac_sess)+"%} "+mid.format(df));
        }

        out.println("@labellist {avg. time} color= greentint off");
        for(Iterator iter = links.iterator(); iter.hasNext(); )
        {
            Link link = (Link) iter.next();
            // This should never happen, BUT...
            if(link.from == link.to) continue;
            
            u.likeVector(link.from, link.to);
            v.likeOrthogonal(u);
            v.mult(0.06 * u.mag());
            if(link.from.toString().compareTo(link.to.toString()) < 0)
                v.neg();
            mid.likeMidpoint(link.from, link.to);
            mid.add(v);
            
            out.println("{"+df.format(link.avg_time)+" sec} "+mid.format(df));
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
        // Load all the links from files or stdin
        if(inputFiles.isEmpty())
            loadLinks(new InputStream[] {System.in});
        else
        {
            InputStream[] in = new InputStream[ inputFiles.size() ];
            for(int i = 0; i < in.length; i++)
                in[i] = new FileInputStream( (File)inputFiles.get(i) );
            loadLinks(in);
        }
        
        // Set up all of our harmonic restraints
        StateManager stateman = createRestraints();
        
        // Run the minimization to position the sequences in R3
        double bestEnergy = findLowestEnergy(stateman);
        
        // Write a kinemage visualization
        System.out.println("@kinemage");
        System.out.println("@perspective");
        System.out.println("@onewidth");
        plotPages(pages, System.out);
        plotLinks(links, System.out);
    }

    public static void main(String[] args)
    {
        PageSpacer mainprog = new PageSpacer();
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
            InputStream is = getClass().getResourceAsStream("PageSpacer.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'PageSpacer.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.minimize.PageSpacer");
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
        else if(flag.equals("-repulsion"))
        {
            try { repulsion = Double.parseDouble(param); }
            catch(NumberFormatException ex) {}
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

