// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.*;
import chiropraxis.mc.*;
import molikin.logic.BallAndStickLogic;
//}}}
/**
* <code>LocalRotarama</code> expresses <b>local</b> rotamer and Ramachandran 
* scores, with either text or kinemage output.
* 
* <p>Begun on Tue Feb  2 2010
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
*/
public class LocalRotarama //extends ... implements ...
{
//{{{ Constants
    private final static String ROTA = "rotamer";
    private final static String RAMA = "Ramachandran";
    private final String AVERAGES = "Average local rotarama";
    private final String KINEMAGE = "Local rotarama kinemage";
    private final DecimalFormat df = new DecimalFormat("#.###");
    private final String aaNames = "ALA ARG ASN ASP CYS GLU GLN GLY HIS ILE"
                                  +"LEU LYS MET PHE PRO SER THR TRP TYR VAL";
//}}}

//{{{ Variable definitions
//##############################################################################
    private boolean  verbose = false;
    private String   mode = AVERAGES;
    private String   filename;
    private Model    model;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public LocalRotarama()
    {
        super();
    }
//}}}

//{{{ printKinColoredByScore
//##############################################################################
    private void printKinColoredByScore() throws IOException
    {
        HashMap<Residue,Double> rota = new Rotalyze().getEvals(model);
        HashMap<Residue,Double> rama = new Ramalyze().getEvals(model);
        
        BallAndStickLogic bsl = new BallAndStickLogic();
        bsl.doProtein    = true;
        bsl.doMainchain  = true;
        bsl.doSidechains = true;
        bsl.colorBy = BallAndStickLogic.COLOR_BY_ROTARAMA;
        bsl.rota = rota;
        bsl.rama = rama;
        
        String title = "";
        ModelState state = model.getState();
        if(state != null && state.getName().length() >= 4)
            title += state.getName().substring(0,4).toLowerCase()+" ";
        title += "rotarama";
        System.out.println("@kinemage {"+title+"}");
        System.out.println("@group {"+title+"} dominant");
        
        Collection chains = model.getChainIDs();
        for(Iterator ci = chains.iterator(); ci.hasNext(); )
        {
            String chainID = (String) ci.next();
            if(model.getChain(chainID) != null)
            {
                System.out.println("@subgroup {chain "+chainID+"} dominant");
                String mcColor = "blue"; // shouldn't matter...
                Set<Residue> residues = model.getChain(chainID);
                PrintWriter out = new PrintWriter(System.out);
                bsl.printKinemage(out, model, residues, mcColor);
            }
        }//chain
    }
//}}}

//{{{ printAverageLocalScores
//##############################################################################
    private void printAverageLocalScores() throws IOException
    {
        System.out.println("res\tnearby\tscored\trota\trama\tavg");
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            if(aaNames.indexOf(res.getName()) == -1) continue;
            try
            {
                // Re-rota/ramalyzes each time, but the cost in time is only 
                // ~20%, and it's nice and simple to have only one calcLocalScore() 
                // method which conveniently is accessible to the outside world.
                int    nn = new Neighborhood(res, model, 2, 2).getMembers().size();
                int    nr = calcLocalNumScored(res, model);
                double ro = calcLocalScore(res, model, this.ROTA);
                double ra = calcLocalScore(res, model, this.RAMA);
                double av = 0.5 * (ro + ra);
                
                //System.out.println(res.nickname()+"\t"+nn+"\t"+nr+"\t"
                //    +df.format(ro)+"\t"+df.format(ra)+"\t"+df.format(av));
                System.out.println(res+","+nn+","+nr+","
                    +df.format(ro)+","+df.format(ra)+","+df.format(av));
            }
            catch(IllegalArgumentException ex)
            { System.err.println("bad local score type for "+res); }
        }
    }
//}}}

//{{{ calcLocalNumScored
//##############################################################################
    public static int calcLocalNumScored(Residue res, Model model) throws IOException
    {
        HashMap<Residue,Double> rota = new Rotalyze().getEvals(model);
        HashMap<Residue,Double> rama = new Ramalyze().getEvals(model);
        
        int n = 0;
        for(Residue oth : new Neighborhood(res, model, 2, 2).getMembers())
            if(rota.keySet().contains(oth) || rama.keySet().contains(oth))
                n++;
        return n;
    }
//}}}

//{{{ calcLocalScore
//##############################################################################
    /**
    * Calculates the average rotamer or Ramachandran percentile score for a
    * local region around the provided residue in the provided model.
    * May be called statically to retrieve rotamer or Ramachandran information
    * on a single residue-centered region of interest.
    * @param type one of the ROTA or RAMA constants defined in this class
    * @throws IOException if Rotalyze or Ramalyze fails to evaulate the model
    * @throws IllegalArgumentException if type is not one of the ROTA or RAMA
    * constants defined in this class
    */
    public static double calcLocalScore(Residue res, Model model, String type) throws IOException, IllegalArgumentException
    {
        HashMap<Residue,Double> scores = null;
        if     (type.equals(ROTA)) scores = new Rotalyze().getEvals(model);
        else if(type.equals(RAMA)) scores = new Ramalyze().getEvals(model);
        else throw new IllegalArgumentException("argument 'type' must be one "
            +"of the ROTA or RAMA String constants defined in LocalRotarama");
        
        if(scores == null) // can this ever happen?
        {
            System.err.println("failed to produce "+type+" scores for "+model);
            return Double.NaN;
        }
        
        double score = 0;
        int n = 0;
        for(Residue member : new Neighborhood(res, model, 2, 2).getMembers())
        {
            if(scores.keySet().contains(member))
            {
                score += scores.get(member);
                n++;
            }
        }
        if(n == 0) return Double.NaN; // can't divide by 0
        return score / (double) n;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(filename == null)
        {
            showHelp(true);
            System.exit(1);
        }
        try
        {
            File file = new File(filename);
            if(file.isDirectory()) return; // skip nested directories
            CoordinateFile coords = new PdbReader().read(file);
            this.model = coords.getFirstModel();
            
            String[] parts = Strings.explode(file.getName(), '/');
            String filename = parts[parts.length-1];
            System.err.println(mode+" for "+filename);
            try
            {
                if     (mode.equals(KINEMAGE)) printKinColoredByScore();
                else if(mode.equals(AVERAGES)) printAverageLocalScores();
            }
            catch(IOException ex)
            { System.err.println("failed to rota/Ramalyze "+model); }
        }
        catch(IOException ex)
        { System.err.println("Error rotalyzing/Ramalyzing model: "+filename); }
    }
    
    public static void main(String[] args)
    {
        LocalRotarama mainprog = new LocalRotarama();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(Exception ex)
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
            InputStream is = getClass().getResourceAsStream("LocalRotarama.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'LocalRotarama.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.rotarama.LocalRotarama version "+getVersion()+" build "+getBuild());
        System.err.println("Copyright (C) 2010 by Daniel Keedy. All rights reserved.");
    }

    // Get version number
    String getVersion()
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream("chiropraxis/version.props");
        if(is == null)
            System.err.println("\n*** Unable to locate version number in 'version.props' ***\n");
        else
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                line = reader.readLine();
                if(line != null && line.indexOf("version=") != -1)
                    return line.substring( line.indexOf("=")+1 );
            }
            catch(IOException ex) { ex.printStackTrace(); }
        }
        return "?.??";
    }

    // Get build number
    String getBuild()
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream("chiropraxis/buildnum.props");
        if(is == null)
            System.err.println("\n*** Unable to locate build number in 'buildnum.props' ***\n");
        else
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                line = reader.readLine();
                if(line != null && line.indexOf("buildnum=") != -1)
                    return line.substring( line.indexOf("=")+1 );
            }
            catch(IOException ex) { ex.printStackTrace(); }
        }
        return "yyyymmdd.????";
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
        if(filename == null) filename = arg;
        else
        {
            showHelp(true);
            System.exit(1);
        }
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-kin") || flag.equals("-k"))
        {
            mode = KINEMAGE;
        }
        else if(flag.equals("-avg") || flag.equals("-a"))
        {
            mode = AVERAGES;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class
