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
* <code>LocalRotarama</code> makes a kinemage colored by localized 
* rotamer/Ramachandran score.
* 
* <p>Begun on Tue Feb  2 2010
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
*/
public class LocalRotarama //extends ... implements ...
{
//{{{ Constants
    String KINEMAGE = "kinemage output";
    String AVERAGES = "average local rotarama score output";
    DecimalFormat df = new DecimalFormat("#.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    String   mode = KINEMAGE;
    boolean  verbose = false;
    String   filename;
    Model    model;
    HashMap<Residue,Double>  rota, rama;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public LocalRotarama()
    {
        super();
    }
//}}}

//{{{ rotaramalyze
//##############################################################################
    /**
    * Stores rotamer names and rotamer and Ramachandran evaluations.
    */
    public void rotaramalyze() throws IOException
    {
        File file = new File(filename);
        if(file.isDirectory()) return; // skip nested directories
        if(verbose) System.err.println("Rotalyzing "+file);
        
        CoordinateFile coords = new PdbReader().read(file);
        model = coords.getFirstModel();
        Rotalyze rotalyze = new Rotalyze();
        Ramalyze ramalyze = new Ramalyze();
        rota = rotalyze.getEvals(model);
        rama = ramalyze.getEvals(model);
        
        if(verbose)
        {
            ArrayList<Residue> rotaRes = new ArrayList<Residue>();
            ArrayList<Residue> ramaRes = new ArrayList<Residue>();
            for(Iterator iter = rota.keySet().iterator(); iter.hasNext(); )
                rotaRes.add( (Residue) iter.next() );
            for(Iterator iter = rama.keySet().iterator(); iter.hasNext(); )
                ramaRes.add( (Residue) iter.next() );
            Collections.sort(ramaRes);
            Collections.sort(rotaRes);
            System.err.println("Rota:");
            for(Residue r : rotaRes) System.err.println("  "+r+": "+rota.get(r));
            System.err.println("Rama:");
            for(Residue r : ramaRes) System.err.println("  "+r+": "+rama.get(r));
        }
    }
//}}}

//{{{ colorKinByScore
//##############################################################################
    public void colorKinByScore()
    {
        BallAndStickLogic bsl = new BallAndStickLogic();
        bsl.doProtein    = true;
        bsl.doMainchain  = true;
        bsl.doSidechains = true;
        bsl.colorBy = BallAndStickLogic.COLOR_BY_ROTARAMA;
        bsl.rota = this.rota;
        bsl.rama = this.rama;
        
        String title = "";
        ModelState state = model.getState();
        if(state != null && state.getName().length() >= 4)
            title += state.getName().substring(0,4).toLowerCase()+" ";
        title += "rota/Rama";
        System.out.println("@kinemage {"+title+"}");
        System.out.println("@group {"+title+"}");
        
        Collection chains = model.getChainIDs();
        for(Iterator ci = chains.iterator(); ci.hasNext(); )
        {
            String chainID = (String) ci.next();
            if(model.getChain(chainID) != null)
            {
                System.out.println("@subgroup {chain "+chainID+"} dominant"); // master= {col rota/Rama}");
                String mcColor = "blue"; // shouldn't matter anyway (?)
                Set<Residue> residues = model.getChain(chainID);
                PrintWriter out = new PrintWriter(System.out);
                bsl.printKinemage(out, model, residues, mcColor);
            }
        }
    }
//}}}

//{{{ averageLocalScores
//##############################################################################
    public void averageLocalScores()
    {
        TreeSet<Residue> goober = new TreeSet<Residue>();
        for(Iterator iter = rama.keySet().iterator(); iter.hasNext(); )
            goober.add( (Residue) iter.next() );
        for(Iterator iter = rota.keySet().iterator(); iter.hasNext(); )
            goober.add( (Residue) iter.next() );
        ArrayList<Residue> scored = new ArrayList<Residue>();
        for(Iterator iter = goober.iterator(); iter.hasNext(); )
            scored.add( (Residue) iter.next() );
        Collections.sort(scored);
        System.out.println("res\t\tnrota\tavrota\tnrama\tavrama\tavboth");
        for(Residue res : scored)
        {
            double rotaAvg = 0, ramaAvg = 0;
            int    rotaCnt = 0, ramaCnt = 0;
            for(Iterator iter = rota.keySet().iterator(); iter.hasNext(); )
            {
                Residue oth = (Residue) iter.next();
                try
                {
                    if(residuesAreClose(res, oth))
                    {
                        rotaAvg += rota.get(oth);
                        rotaCnt++;
                    }
                }
                catch(AtomException ex)
                { System.err.println("error adding '"+oth+"' to rota avg for '"+res+"'"); }
            }
            for(Iterator iter = rama.keySet().iterator(); iter.hasNext(); )
            {
                Residue oth = (Residue) iter.next();
                try
                {
                    if(residuesAreClose(res, oth))
                    {
                        ramaAvg += rama.get(oth);
                        ramaCnt++;
                    }
                }
                catch(AtomException ex)
                { System.err.println("error adding '"+oth+"' to rama avg for '"+res+"'"); }
            }
            if(rotaCnt >= 5 && ramaCnt >= 6)
            {
                System.out.println(res+"\t"
                    +rotaCnt+"\t"+df.format(rotaAvg)+"\t"
                    +ramaCnt+"\t"+df.format(ramaAvg)+"\t"
                    +df.format((0.5*(rotaAvg+ramaAvg))));
            }
        }//res
    }
//}}}

//{{{ residuesAreClose
//##############################################################################
    /**
    * Returns true if any atom in the first residue is sufficiently close 
    * to any atom in the second residue, including sidechain atoms, or if
    * their sequence separation is 
    */
    public boolean residuesAreClose(Residue r1, Residue r2) throws AtomException
    {
        // sequence
        int seqDiff = Math.abs(r1.getSequenceInteger() - r2.getSequenceInteger());
        if(seqDiff <= 2) return true;
        
        // distance
        double distance = 3;
        ModelState s = model.getState();
        for(Iterator iter1 = r1.getAtoms().iterator(); iter1.hasNext(); )
        {
            AtomState a1 = s.get( (Atom) iter1.next() );
            for(Iterator iter2 = r2.getAtoms().iterator(); iter2.hasNext(); )
            {
                AtomState a2 = s.get( (Atom) iter2.next() );
                if(a1.distance(a2) < distance) return true;
            }
        }
        
        return false;
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
            rotaramalyze();
            System.err.println("mode: "+mode);
            if(mode.equals(KINEMAGE))       colorKinByScore();
            else if(mode.equals(AVERAGES))  averageLocalScores();
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
