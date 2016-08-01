// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>AltConfShearFinder</code> searches through the alternate conformations
* of a crystal structure, looking for residues that appear to undergo
* a shear-like conformational change.
*
* <p>Copyright (C) 2010 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Sep 21 2010
*/
public class AltConfShearFinder //extends ... implements ...
{
//{{{ Constants
    DecimalFormat df1 = new DecimalFormat("0.0##");
    DecimalFormat df2 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean     verbose = false;
    Collection  inputFiles;
    double      maxTheta;
    double      maxRmsdChange;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AltConfShearFinder()
    {
        super();
        inputFiles = new ArrayList();
    }
//}}}

//{{{ searchModel
//##############################################################################
    void searchModel(PrintStream out, String label, Model model)
    {
        //{{{ DEBUG:
        //System.err.println("BY VALUE");
        //Collection stateC = model.getStates().values();
        //ModelState[] states = (ModelState[]) stateC.toArray(new ModelState[stateC.size()]);
        //for(int i = 0; i < states.length; i++)
        //{
        //    for(int j = i+1; j < states.length; j++)
        //    {
        //        ModelState state1 = states[i];
        //        ModelState state2 = states[j];
        //        System.err.println(+i+" -> "+getStateName(state1, model)
        //                      +", "+j+" -> "+getStateName(state2, model));
        //    }
        //}
        //
        //System.err.println("BY KEY");
        //for(Iterator iter1 = model.getStates().keySet().iterator(); iter1.hasNext(); )
        //{
        //    String key1 = (String) iter1.next();
        //    for(Iterator iter2 = model.getStates().keySet().iterator(); iter2.hasNext(); )
        //    {
        //        String key2 = (String) iter2.next();
        //        if(key2.equals(key1)) continue;
        //        ModelState state1 = (ModelState) model.getStates().get(key1);
        //        ModelState state2 = (ModelState) model.getStates().get(key2);
        //        System.err.println(key1+" -> "+getStateName(state1, model)
        //                     +", "+key2+" -> "+getStateName(state2, model));
        //    }
        //}
        //System.exit(0);
        //}}}
        
        final double maxCaShift = 0.01;
        // Value ^ used for AltConfBackrubFinder: 0.01
        // "less than 2% more examples at 0.1 A allowance"
        Collection stateC = model.getStates().values();
        ModelState[] states = (ModelState[]) stateC.toArray(new ModelState[stateC.size()]);
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res1 = (Residue) iter.next();
            if(!AminoAcid.isAminoAcid(res1)) continue;
            Residue res2 = res1.getNext(model);  if(res2 == null) continue;
            Residue res3 = res2.getNext(model);  if(res3 == null) continue;
            Residue res4 = res3.getNext(model);  if(res4 == null) continue;
            Atom ca1 = res1.getAtom(" CA ");
            Atom ca2 = res2.getAtom(" CA ");
            Atom ca3 = res3.getAtom(" CA ");
            Atom ca4 = res4.getAtom(" CA ");
            Atom cb2 = res2.getAtom(" CB ");
            Atom cb3 = res3.getAtom(" CB ");
            Atom o2  = res2.getAtom(" O  ");
            
            // Vectors between atom pairs for max Ca & Cb moves
            Triple maxCaCa  = new Triple(1,0,0);
            Triple maxCbCb2 = new Triple(1,0,0);
            Triple maxCbCb3 = new Triple(1,0,0);
            Triple maxOO2   = new Triple(1,0,0);
            double maxCa2Dist = 0, maxCa3Dist = 0;
            double maxCb2Dist = 0, maxCb3Dist = 0;
            double maxO2Dist  = 0;
            int maxI = -1, maxJ = -1;
            
            // Test between all pairs of states
            for(int i = 0; i < states.length; i++)
            {
                for(int j = i+1; j < states.length; j++)
                {
                    try
                    {
                        // If anchor Ca's move too far, skip this one
                        AtomState ca1alt1 = states[i].get(ca1);
                        AtomState ca1alt2 = states[j].get(ca1);
                        AtomState ca4alt1 = states[i].get(ca4);
                        AtomState ca4alt2 = states[j].get(ca4);
                        if(ca1alt1.distance(ca1alt2) > maxCaShift
                        || ca4alt1.distance(ca4alt2) > maxCaShift) continue;
                        
                        // Look for a big movement by the central Ca's
                        AtomState ca2alt1 = states[i].get(ca2);
                        AtomState ca2alt2 = states[j].get(ca2);
                        AtomState ca3alt1 = states[i].get(ca3);
                        AtomState ca3alt2 = states[j].get(ca3);
                        double ca2dist = ca2alt1.distance(ca2alt2);
                        double ca3dist = ca3alt1.distance(ca3alt2);
                        if(ca2dist + ca3dist > maxCa2Dist + maxCa3Dist)
                        {
                            maxCa2Dist = ca2dist;
                            maxCa3Dist = ca3dist;
                            Triple ca1mid = new Triple().likeMidpoint(ca1alt1, ca1alt2);
                            Triple ca4mid = new Triple().likeMidpoint(ca4alt1, ca4alt2);
                            maxCaCa.likeVector(ca1mid, ca4mid);
                            
                            AtomState cb2alt1 = states[i].get(cb2);
                            AtomState cb2alt2 = states[j].get(cb2);
                            AtomState cb3alt1 = states[i].get(cb3);
                            AtomState cb3alt2 = states[j].get(cb3);
                            maxCb2Dist = cb2alt1.distance(cb2alt2);
                            maxCb3Dist = cb3alt1.distance(cb3alt2);
                            maxCbCb2.likeVector(cb2alt1, cb2alt2);
                            maxCbCb3.likeVector(cb3alt1, cb3alt2);
                            
                            AtomState o2alt1 = states[i].get(o2);
                            AtomState o2alt2 = states[j].get(o2);
                            maxO2Dist = o2alt1.distance(o2alt2);
                            maxOO2.likeVector(o2alt1, o2alt2);
                            
                            // Wait 'til now to claim this state combo is best so far
                            maxI = i;
                            maxJ = j;
                        }
                    }
                    catch(AtomException ex) {}
                }//for j states
            }//for i states
            
            if(maxI == -1 || maxJ == -1)
                continue; // these residues may contain a Gly - no Cb
            
            if(maxCa2Dist > 0 && maxCa3Dist > 0)
            {
                // Spit out our results
                String alt1 = getStateName(states[maxI], model);
                String alt2 = getStateName(states[maxJ], model);
                String delim = ",";
                out.print(label.toLowerCase()+delim+
                    alt1+delim+alt2+delim+
                    //res1.getCNIT()+delim+res4.getCNIT()+delim+
                    res1.getChain()+delim+res1.getSequenceInteger()+delim+
                    res1.getInsertionCode()+delim+res1.getName()+delim+
                    res4.getChain()+delim+res4.getSequenceInteger()+delim+
                    res4.getInsertionCode()+delim+res4.getName()+delim+
                    df1.format(maxCa2Dist)+delim+               // Ca
                    df1.format(maxCa3Dist)+delim+
                    df1.format(maxCb2Dist)+delim+               // Cb
                    df1.format(maxCb3Dist)+delim+
                    df1.format(maxO2Dist)+delim+                // C=O
                    df2.format(maxCaCa.angle(maxCbCb2))+delim+  // perp or ||
                    df2.format(maxCaCa.angle(maxCbCb3))+delim+  // perp or ||
                    df2.format(maxCaCa.angle(maxOO2)));         // perp or ||
                if(verbose) out.println();
                
                // Try to interrelate C-alphas (& C=Os) using shears and backrubs
                ShearFit shearFit = new ShearFit();
                /*shearFit.initData(model, res1, res2, res3, res4, alt1, alt2, "ca+o", verbose, delim);
                ModelState fitState = shearFit.interrelateAltConfs(10, 1.0);
                // Other things I've tried for ^ that are faster but maybe 
                // more prone to local minima due to large initial changes:
                // 5, 10.0
                // 5, 15.0*/
                shearFit.initData(model, res1, res2, res3, res4, alt1, alt2, "ca+o", verbose, delim, maxTheta, maxRmsdChange);
                ModelState fitState = shearFit.interrelateAltConfs();
                // Now goes to convergence, i.e. until RMSD changes level out,
                // instead of for a set number of trials as above.
                // That doesn't necessarily mean the *best* solution will have been found,
                // since my little "algorithm" here is not provably accurate
                // (although it is deterministic), but it does mean we're done.
                if(!verbose) out.println();
                
                System.err.println();
            }
        }//for each residue
    }
//}}}

//{{{ getStateName
//##############################################################################
    /**
    * Finds the key in the Model's Map<String,ModelState>
    * for the given ModelState, or null if it isn't found.
    */
    String getStateName(ModelState state, Model model)
    {
        Map states = model.getStates();
        for(Iterator iter = states.keySet().iterator(); iter.hasNext(); )
        {
            String key = (String) iter.next();
            ModelState curr = (ModelState) states.get(key);
            if(curr.equals(state)) // does ModelState implement equals?
                return key;
        }
        return null;
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        if(Double.isNaN(maxTheta))
        {
            maxTheta = 1.0; // bigger changes may lead to early false minima
            System.err.println("-maxtheta=#.# not provided -- using default of "+maxTheta);
        }
        if(Double.isNaN(maxRmsdChange))
        {
            maxRmsdChange = 0.001; // pretty small change -- should mean we've ~converged
            System.err.println("-maxrmsdchange=#.# not provided -- using default of "+maxRmsdChange);
        }
        
        PdbReader reader = new PdbReader();
        for(Iterator files = inputFiles.iterator(); files.hasNext(); )
        {
            File f = (File) files.next();
            try
            {
                CoordinateFile cf = reader.read(f);
                for(Iterator models = cf.getModels().iterator(); models.hasNext(); )
                {
                    Model m = (Model) models.next();
                    String label = f.toString();
                    if(cf.getIdCode() != null) label = cf.getIdCode();
                    searchModel(System.out, label, m);
                }
            }
            catch(IOException ex)
            { System.err.println("IOException when processing "+f); }
        }
    }

    public static void main(String[] args)
    {
        AltConfShearFinder mainprog = new AltConfShearFinder();
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
            InputStream is = getClass().getResourceAsStream("AltConfShearFinder.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AltConfShearFinder.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AltConfShearFinder");
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
        File f = new File(arg);
        if(f.isFile()) inputFiles.add(f);
        else throw new IllegalArgumentException("'"+arg+"' is not a valid file name.");
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
        else if(flag.equals("-maxtheta"))
        {
            try
            { maxTheta = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { System.err.println("Error parsing "+param+" as a double!"); }
        }
        else if(flag.equals("-maxrmsdchange"))
        {
            try
            { maxRmsdChange = Double.parseDouble(param); }
            catch(NumberFormatException ex)
            { System.err.println("Error parsing "+param+" as a double!"); }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

