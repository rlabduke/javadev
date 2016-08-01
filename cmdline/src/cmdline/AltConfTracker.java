// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.util.Strings;
import driftwood.moldb2.*;
import driftwood.r3.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//}}}
/**
* <code>AltConfTracker</code> simply follows alternate conformations
* and reports where they begin and end.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu Sep 23 2010
*/
public class AltConfTracker //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df = new DecimalFormat("#.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean  verbose  = false;
    File     inFile   = null;
    boolean  scOnly   = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AltConfTracker()
    {
        super();
    }
//}}}

//{{{ trackBbAlts
//##############################################################################
    public void trackBbAlts() throws IOException
    {
        ArrayList bbNames = new ArrayList();
        bbNames.add(" N  ");
        bbNames.add(" H  ");
        bbNames.add(" CA ");
        bbNames.add(" HA ");
        bbNames.add(" C  ");
        bbNames.add(" O  ");
        
        PdbReader pdbReader = new PdbReader();
        CoordinateFile coordFile = pdbReader.read(inFile);
        Model model = coordFile.getFirstModel();
        
        Collection stateC = model.getStates().values();
        ModelState[] states = (ModelState[]) stateC.toArray(new ModelState[stateC.size()]);
        
        ArrayList altRes = new ArrayList();
        
        for(Iterator rIter = model.getResidues().iterator(); rIter.hasNext(); )
        {
            Residue res = (Residue) rIter.next();
            if(!AminoAcid.isAminoAcid(res)) continue;
            if(hasBbAlts(res, states, bbNames))
            {
                // Extend current alt
                altRes.add(res);
            }
            else
            {
                // End current alt (if we're in one)
                if(!altRes.isEmpty())
                {
                    printAlt(System.out, coordFile, altRes);
                    altRes = new ArrayList();
                }
            }
        }
    }
//}}}

//{{{ trackScAlts
//##############################################################################
    public void trackScAlts() throws IOException
    {
        ArrayList bbNames = new ArrayList();
        bbNames.add(" N  ");
        bbNames.add(" H  ");
        bbNames.add(" CA ");
        bbNames.add(" HA ");
        bbNames.add(" C  ");
        bbNames.add(" O  ");
        
        PdbReader pdbReader = new PdbReader();
        CoordinateFile coordFile = pdbReader.read(inFile);
        Model model = coordFile.getFirstModel();
        
        Collection stateC = model.getStates().values();
        ModelState[] states = (ModelState[]) stateC.toArray(new ModelState[stateC.size()]);
        
        ArrayList altRes = new ArrayList();
        
        for(Iterator rIter = model.getResidues().iterator(); rIter.hasNext(); )
        {
            Residue res = (Residue) rIter.next();
            if(!AminoAcid.isAminoAcid(res)) continue;
            if(hasScAlts(res, states, bbNames) && !hasBbAlts(res, states, bbNames))
            {
                // Print single-res sc alt
                altRes = new ArrayList();
                altRes.add(res);
                printAlt(System.out, coordFile, altRes);
            }
        }
    }
//}}}

//{{{ trackAltConfs [OLD]
//##############################################################################
//    public void trackAltConfs() throws IOException
//    {
//        ArrayList<String> bbNames = new ArrayList<String>();
//        bbNames.add(" N  ");
//        bbNames.add(" H  ");
//        bbNames.add(" CA ");
//        bbNames.add(" HA ");
//        bbNames.add(" C  ");
//        bbNames.add(" O  ");
//        
//        PdbReader pdbReader = new PdbReader();
//        CoordinateFile coordFile = pdbReader.read(inFile);
//        Model model = coordFile.getFirstModel();
//        
//        Collection stateC = model.getStates().values();
//        ModelState[] states = (ModelState[]) stateC.toArray(new ModelState[stateC.size()]);
//        
//        ArrayList altRes = new ArrayList();
//        boolean bbAltSomewhere = false, scAltSomewhere = false; // somewhere in the current alt
//        
//        for(Iterator rIter = model.getResidues().iterator(); rIter.hasNext(); )
//        {
//            Residue res = (Residue) rIter.next();
//            if(!AminoAcid.isAminoAcid(res)) continue;
//            
//            boolean bbAlt = hasBbAlts(res, states, bbNames);
//            boolean scAlt = hasScAlts(res, states, bbNames);
//            if(!bbAltSomewhere) bbAltSomewhere = true;
//            if(!scAltSomewhere) scAltSomewhere = true;
//            
//            if(bbAlt)
//            {
//                // Extend current alt
//                altRes.add(res);
//            }
//            //if(scAlt) don't extend current alt - only bb alts count for that
//            else
//            {
//                // End current alt (if we're in one)
//                if(!altRes.isEmpty())
//                {
//                    printAlt(System.out, coordFile, altRes, bbAltSomewhere, scAltSomewhere);
//                    altRes = new ArrayList();
//                    bbAltSomewhere = false;
//                    scAltSomewhere = false;
//                }
//            }
//        }
//    }
//}}}

//{{{ hasBbAlts
//##############################################################################
    boolean hasBbAlts(Residue res, ModelState[] states, ArrayList<String> bbNames)
    {
        boolean bbAlt = false;
        for(String bbName : bbNames)
        {
            Atom bbAtom = res.getAtom(bbName);
            for(int i = 0; i < states.length; i++)
            {
                for(int j = i+1; j < states.length; j++)
                {
                    try
                    {
                        AtomState bbState_i = states[i].get(bbAtom);
                        AtomState bbState_j = states[j].get(bbAtom);
                        if(bbState_i.distance(bbState_j) > 0)
                        {
                            bbAlt = true;
                        }
                    }
                    catch(AtomException ex) {}
                }//for j states
            }//for i states
        }
        if(bbAlt)
        {
            return true;
        }
        return false;
    }
//}}}

//{{{ hasScAlts
//##############################################################################
    boolean hasScAlts(Residue res, ModelState[] states, ArrayList<String> bbNames)
    {
        boolean scAlt = false;
        for(Iterator aIter = res.getAtoms().iterator(); aIter.hasNext(); )
        {
            Atom scAtom = (Atom) aIter.next();
            if(bbNames.contains(scAtom.getName())) continue;
            for(int i = 0; i < states.length; i++)
            {
                for(int j = i+1; j < states.length; j++)
                {
                    try
                    {
                        AtomState scState_i = states[i].get(scAtom);
                        AtomState scState_j = states[j].get(scAtom);
                        if(scState_i.distance(scState_j) > 0)
                        {
                            scAlt = true;
                        }
                    }
                    catch(AtomException ex) {}
                }//for j states
            }//for i states
        }
        if(scAlt)
        {
            return true;
        }
        return false;
    }
//}}}

//{{{ printAlt
//##############################################################################
    void printAlt(PrintStream out, CoordinateFile coordFile, ArrayList altRes)//, boolean bbAlt, boolean scAlt)
    {
        int numRes = altRes.size();
        
        Residue begin = (Residue) altRes.get(0);
        Residue end   = (Residue) altRes.get(altRes.size()-1);
        
        out.println(coordFile.getIdCode().toLowerCase()+":"+
            begin.getChain()+":"+begin.getSequenceInteger()+":"+
            begin.getInsertionCode()+":"+begin.getName()+":"+
            end.getChain()+":"+end.getSequenceInteger()+":"+
            end.getInsertionCode()+":"+end.getName()+":"+numRes);
            //+":"+bbAlt+":"+scAlt);
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        if(inFile == null) throw new IllegalArgumentException(
            "Must provide input PDB file!");
        
        if(scOnly)
        {
            System.err.println("Sidechain Alt Mode: only single-res sc");
            trackScAlts();
        }
        else
        {
            System.err.println("Backbone Alt Mode: bb req'd, sc opt'l, multi-residue allowed");
            trackBbAlts();
        }
    }

    public static void main(String[] args)
    {
        AltConfTracker mainprog = new AltConfTracker();
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
            System.err.println();
            System.err.println("*** Error in execution: "+ex.getMessage());
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
            InputStream is = getClass().getResourceAsStream("AltConfTracker.help");
            if(is == null)
            {
                System.err.println("\n*** Usage: java AltConfTracker in.pdb ***\n");
            }
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.AltConfTracker");
        System.err.println("Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.");
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
        if(inFile == null) inFile = new File(arg);
        else System.err.println("Can't use additional argument: "+arg);
    }
    
    void interpretFlag(String flag, String param)
    {
        try
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
            if(flag.equals("-sc"))
            {
                scOnly = true;
            }
            else if(flag.equals("-dummy_option"))
            {
                // handle option here
            }
            else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
        }
        catch(NumberFormatException ex)
        { throw new IllegalArgumentException("Non-number argument to "+flag+": '"+param+"'"); }
    }
//}}}
}//class

