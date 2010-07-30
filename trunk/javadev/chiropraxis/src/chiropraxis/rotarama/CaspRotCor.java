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
//}}}
/**
* <code>CaspRotCor</code> specializes <code>RotCor</code> for CASP assessment:
* it requires standard CASP file nomenclature and automatically combines 
* model "segments" into complete models.
* 
* <p>Begun on Fri Jan 15 2010
* <p>Copyright (C) 2010 by Daniel Keedy. All rights reserved.
*/
public class CaspRotCor extends RotCor
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    int numSegs = 0; // convenience variable - continually overwritten
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CaspRotCor()
    {
        super();
    }
//}}}

//{{{ getName
//##############################################################################
    /**
    * Strips prefixes and extensions from name to reach CASP format:
    * T0###[TS/AL]###_#
    * T0###[TS/AL]###_#_#
    */
    public String getName(String filename)
    {
        // T0388TS002_1_cleanH.pdb
        // T0496TS283_1_1.pdb
        // T0999TS123_1.pdb
        String[] namePieces = Strings.explode(filename, '/');
        String name = namePieces[namePieces.length-1];
        if(name.length() < 15 || name.indexOf(".pdb") == -1)
        {
            throw new IllegalArgumentException(
                "Filename doesn't match CASP format! "+filename);
        }
        name = name.substring(0,14);
        if(name.substring(12,13).equals(".")) // T0999TS123_1.p
        {
            return name.substring(0,12);
        }
        else if(name.substring(12,13).equals("_"))
        {
            try // T0496TS283_1_1
            {
                int segID = Integer.parseInt(name.substring(13,14));
                return name;
            }
            catch(NumberFormatException ex) // T0388TS002_1_c
            {
                return name.substring(0,12);
            }
        }
        else throw new IllegalArgumentException(
            "Filename doesn't match CASP format! "+filename);
    }
//}}}

//{{{ get_ methods
//##############################################################################
    /** Returns T0### part (target ID) of model name. */
    public String getTargetID(String filename)
    { return getName(filename).substring(0, 5); }

    /** Returns TS or AL part of model name. */
    public String getTSorAL(String filename)
    { return getName(filename).substring(5, 7); }

    /** Returns ### part (group ID) of model name. */
    public String getGroupID(String filename)
    { return getName(filename).substring(7, 10); }

    /** Returns _# part (model ID) of model name.  Does not 
    * include possible additional _# segment ID characters. */
    public String getModelID(String filename)
    { return getName(filename).substring(10,12); }

    /** Returns _# part (segment ID, after model ID) of 
    * model name, or null if there is no segment ID. */
    public String getSegmentID(String filename)
    {
        String name = getName(filename);
        if(name == null)              return null;
        else if(name.length() == 14)  return name.substring(12,14);
        else if(name.length() == 12)  return null;
        else                          return null; // weird name length
            
    }
//}}}

//{{{ assessModels
//##############################################################################
    /**
    * Performs <i>rotcor</i> assessment for all models individually against the target.
    * Overrides parent RotCor method!
    */
    public void assessModels()
    {
        File mdlsDir = new File(mdlsDirname);
        String[] listing = mdlsDir.list();
        if(listing == null)
        {
            System.err.println(mdlsDirname+" is an empty directory or does not exist!");
            System.exit(1);
        }
        ArrayList<String> mdlFilenames = new ArrayList<String>();
        for(int i = 0; i < listing.length; i++) mdlFilenames.add(listing[i]);
        Collections.sort(mdlFilenames);
        
        if(mode == RESIDUE) System.err.println("Ignoring -residue mode: supported by RotCor, not CaspRotCor");
        
        System.out.println("Target:TargetCount:TSorAL:Group:Model:SegmentsUsed:TargetRotamers:ModelRotamers:Matches:RotCor");
        
        // Keep track of segments we've combined into full models so we don't duplicate any.
        TreeSet<String> pastSegs = new TreeSet<String>();
        
        for(int i = 0; i < mdlFilenames.size(); i++) // for each individual predicted model
        {
            String mdlFilename = mdlFilenames.get(i);
            if(mdlFilename.indexOf(".pdb") == -1) continue; // only consider PDBs
            if(pastSegs.contains(mdlFilename))    continue; // don't re-use segments
            
            if(getSegmentID(mdlFilename) != null)
            {
                // Start new segment
                TreeSet<String> currSegs = new TreeSet<String>();
                currSegs.add(mdlFilename);
                pastSegs.add(mdlFilename);
                for(int j = 0; j < mdlFilenames.size(); j++)
                {
                    if(i != j)
                    {
                        String othMdlFilename = mdlFilenames.get(j);
                        if(othMdlFilename.indexOf(".pdb") != -1
                        && getTargetID(othMdlFilename).equals(getTargetID(mdlFilename))
                        && getGroupID(othMdlFilename).equals(getGroupID(mdlFilename))
                        && getModelID(othMdlFilename).equals(getModelID(mdlFilename)))
                        {
                            currSegs.add(othMdlFilename); // to combine now
                            pastSegs.add(othMdlFilename); // so we don't re-use later
                        }
                    }
                }
                assessSegsModel(currSegs);
            }
            else
            {
                assessModel(mdlFilename);
            }
        }
    }
//}}}

//{{{ assessSegsModel
//##############################################################################
    /**
    * Performs entire <i>rotcor</i> assessment for one set of CASP segments - 
    * together comprising a full model - against the target.
    */
    public void assessSegsModel(TreeSet<String> segs)
    {
        Rotalyze rotalyze = new Rotalyze();
        String repSeg = (String) segs.iterator().next(); // just to get target/model/groupID
        int mdlNumRotsSum = 0;
        int trgNumRotsSum = 0;
        int matchesSum    = 0;
        double rotcorSum  = 0.0;
        for(Iterator sItr = segs.iterator(); sItr.hasNext(); )
        {
            String mdlFilename = (String) sItr.next();
            try
            {
                Model mdl = extractModel(mdlsDirname+"/"+mdlFilename);
                if(mdl == null) continue;
                
                // Get rotamer names, then index by target residue
                HashMap<Residue,String> tmpRotNames = rotalyze.getNames(mdl);
                HashMap<Residue,String> mdlRotNames = alignModel(mdl, tmpRotNames);
                
                int[] tallies = tallyRotamers(mdlRotNames);
                mdlNumRotsSum += tallies[1];
                trgNumRotsSum += tallies[0];
                matchesSum    += tallies[2];
                rotcorSum     += (double) tallies[2] / (double) tallies[0];
                if(verbose) System.err.println("new rotcor: "+df.format(rotcorSum));
            }
            catch(IOException ex)
            { System.err.println("Error reading file: "+mdlFilename); }
        }
        
        numSegs = segs.size();
        int[] tallySums = new int[] {trgNumRotsSum, mdlNumRotsSum, matchesSum};
        outputModel(repSeg, tallySums, rotcorSum);
        numSegs = 0;
    }
//}}}

//{{{ outputModel
//##############################################################################
    /**
    * Outputs <i>rotcor</i> for a full model: already full or combined from segments.
    * Overrides parent method in <code>RotCor</code>!
    */
    public void outputModel(String mdlFilename, int[] tallies, double rotcor)
    {
        // Target TargetCount TSorAL Group Model SegmentsUsed TargetRotamers ModelRotamers Matches RotCor
        
        System.out.println(
            trgName+":"+
            trgCount+":"+
            getTSorAL(mdlFilename)+":"+
            getGroupID(mdlFilename)+":"+
            getModelID(mdlFilename)+":"+
            numSegs+":"+
            tallies[0]+":"+
            tallies[1]+":"+
            tallies[2]+":"+
            df.format(rotcor)
        );
    }
//}}}

//{{{ main
//##############################################################################
    public static void main(String[] args)
    {
        CaspRotCor mainprog = new CaspRotCor();
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
            InputStream is = getClass().getResourceAsStream("CaspRotCor.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'CaspRotCor.help' ***\n");
            else
            {
                try { streamcopy(is, System.err); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.rotarama.CaspRotCor version "+getVersion()+" build "+getBuild());
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
}//class
