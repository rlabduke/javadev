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
import driftwood.isosurface.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>AnisoOxygenSearch</code> has not yet been documented.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun  7 16:32:10 EDT 2004
*/
public class AnisoOxygenSearch //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    File pdbFile = null;
    File mapFile = null;
    boolean verbose    = false;
    boolean textOutput = true;
    boolean shearTest  = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AnisoOxygenSearch()
    {
        super();
    }
//}}}

//{{{ searchModel
//##############################################################################
    void searchModel(PrintStream out, String fileLabel, Model model, ModelState state, CrystalVertexSource map)
    {
        final double minDensity = 2.0 * map.sigma; // at current O position
        final double envDensity = 1.2 * map.sigma; // ED for "envelope" we're examining
        final int numSamples = 64; // number of samples taken around the envelope
        
        Builder builder = new Builder();
        DecimalFormat df = new DecimalFormat("0.0000");
        DecimalFormat df2 = new DecimalFormat("0.00");
        if(textOutput)
        {
            if(shearTest) out.println("#file:model:chain1:resnum1:inscode1:restype1:chain4:resnum4:inscode4:restype4:ox2_bfactor:angle:ellipse_ratio");
            else          out.println("#file:model:residue:ox_bfactor:angle:ellipse_ratio");
        }
        else
        {
            out.println("@kinemage 1");
            out.println("@group {O-rings} dominant");
        }
        
        for(Iterator ri = model.getResidues().iterator(); ri.hasNext(); )
        {
            Residue r = (Residue) ri.next();
            try
            {
                // find oxygen atom
                AtomState ox = state.get(r.getAtom(" O  "));
                // create search plane
                AtomState c = state.get(r.getAtom(" C  "));
                AtomState ca1 = state.get(r.getAtom(" CA "));
                AtomState ca2 = state.get(r.getNext(model).getAtom(" CA "));
                // project Ca-Ca vector onto search plane (project onto normal and subtract)
                Triple caca = new Triple().likeVector(ca1, ca2);        // Ca---Ca
                Triple vert = new Triple().likeVector(c, ox).unit();    // C----O
                vert.mult(caca.dot(vert));                              // length of CaCa in C-O direction
                caca.sub(vert);                                         // remove C-O component
                // check center density
                double oxDensity = map.evaluateAtPoint(ox.getX(), ox.getY(), ox.getZ());
                if(Double.isNaN(oxDensity) || oxDensity < minDensity)
                {
                    if(textOutput) 
                    {
                        if(shearTest)
                        {
                            Residue r1 = r.getPrev(model);
                            Residue r4 = r.getNext(model).getNext(model);
                            if(r1 == null || r4 == null) continue;
                            out.println(fileLabel.toLowerCase()+":"+model+":"
                                +r1.getChain()+":"+r1.getSequenceInteger()+":"+r1.getInsertionCode()+":"+r1.getName()+":"
                                +r4.getChain()+":"+r4.getSequenceInteger()+":"+r4.getInsertionCode()+":"+r4.getName()+":"
                                +df2.format(ox.getTempFactor())+":bad_density:bad_density");
                        }
                        else out.println(fileLabel+":"+model+":"+r.getCNIT()+":bad_density:bad_density");
                    }
                    continue; // skip this oxygen
                }
                // find envelope points
                ArrayList envelope = new ArrayList();
                for(double i = 0; i < numSamples; i++)
                {
                    // unit vector perpendicular to C--O
                    Triple inplane = builder.construct4(ca1, c, ox, 1, 90, 360*i/numSamples);
                    inplane.sub(ox); // to get to vector form
                    envelope.add( findBelow(envDensity, map, ox, inplane) );
                }
                // calculate principle axes
                PrincipleAxes pca = new PrincipleAxes(envelope);
                Tuple3[] axes = pca.getAxes();
                double[] lengths = pca.getLengths();
                // print axis ratio and orientation
                double orientation = caca.angle(axes[0]);
                if(orientation > 90) orientation = 180 - orientation; // correct for arbitrary sign
                double ellipseRatio = lengths[0] / lengths[1];
                if(verbose)
                {
                    System.err.println(r+": "+lengths[0]+" / "+lengths[1]);
                }
                if(textOutput)
                {
                    if(shearTest)
                    {
                        Residue r1 = r.getPrev(model);
                        Residue r4 = r.getNext(model).getNext(model);
                        if(r1 == null || r4 == null) continue;
                        out.println(fileLabel.toLowerCase()+":"+model+":"
                            +r1.getChain()+":"+r1.getSequenceInteger()+":"+r1.getInsertionCode()+":"+r1.getName()+":"
                            +r4.getChain()+":"+r4.getSequenceInteger()+":"+r4.getInsertionCode()+":"+r4.getName()+":"
                            +df2.format(ox.getTempFactor())+":"+df.format(orientation)+":"+df.format(ellipseRatio));
                    }
                    else out.println(fileLabel.toLowerCase()+":"+model+":"+r.getCNIT()+":"+df2.format(ox.getTempFactor())+":"+df.format(orientation)+":"+df.format(ellipseRatio));
                }
                else
                {
                    // print kinemage info
                    out.println("@vectorlist {"+fileLabel+":"+model+":"+r.getCNIT()+"} color= orange");
                    for(Iterator iter = envelope.iterator(); iter.hasNext(); )
                    {
                        Triple t = (Triple) iter.next();
                        out.println("{} "+t.format(df));
                    }
                    
                    // start at O for now -- I don't know where on the envelope these two axes go to & from!
                    Triple axis0 = new Triple(ox).add( ((Triple) axes[0]).mult(lengths[0]) );
                    Triple axis1 = new Triple(ox).add( ((Triple) axes[1]).mult(lengths[1]) );
                    out.println("@vectorlist {"+fileLabel+":"+model+":"+r.getCNIT()+"} color= lilactint");
                    out.println("{}P "+ox.format(df));
                    out.println("{} "+axis0.format(df));
                    out.println("@vectorlist {"+fileLabel+":"+model+":"+r.getCNIT()+"} color= greentint");
                    out.println("{}P "+ox.format(df));
                    out.println("{} "+axis1.format(df));
                }
            }
            catch(Exception ex) // Atom, NullPtr
            {
                //out.println(fileLabel+":"+model+":"+r.getCNIT()+":missing_atom:missing_atom");
                //ex.printStackTrace();
            }
        }
    }
//}}}

//{{{ searchModel2
//##############################################################################
    void searchModel2(PrintStream out, String fileLabel, Model model, ModelState state, CrystalVertexSource map)
    {
        final double minDensity = 3.0 * map.sigma; // at current O position
        final double envSigmaMin = 1.2; //* map.sigma; // min "envelope" ED we're examining
        final double envSigmaMax = 2.4; //* map.sigma; // max "envelope" ED we're examining
        final int numSamples = 64; // number of samples taken around the envelope
        
        Builder builder = new Builder();
        DecimalFormat df = new DecimalFormat("0.0000");
        DecimalFormat df2 = new DecimalFormat("0.00");
        DecimalFormat df3 = new DecimalFormat("0.0");
        if(textOutput)
        {
            if(shearTest) out.println("#file:model:chain1:resnum1:inscode1:restype1:chain4:resnum4:inscode4:restype4:ox2_bfactor:angle:max_ellipse_ratio");
            else          out.println("#file:model:residue:ox_bfactor:angle:max_ellipse_ratio");
        }
        else
        {
            out.println("@kinemage 1");
            out.println("@group {O-rings} dominant");
        }
        
        for(Iterator ri = model.getResidues().iterator(); ri.hasNext(); )
        {
            Residue r = (Residue) ri.next();
            try
            {
                // find oxygen atom
                AtomState ox = state.get(r.getAtom(" O  "));
                // create search plane
                AtomState c = state.get(r.getAtom(" C  "));
                AtomState ca1 = state.get(r.getAtom(" CA "));
                AtomState ca2 = state.get(r.getNext(model).getAtom(" CA "));
                // project Ca-Ca vector onto search plane (project onto normal and subtract)
                Triple caca = new Triple().likeVector(ca1, ca2);        // Ca---Ca
                Triple vert = new Triple().likeVector(c, ox).unit();    // C----O
                vert.mult(caca.dot(vert));                              // length of CaCa in C-O direction
                caca.sub(vert);                                         // remove C-O component
                // check center density
                double oxDensity = map.evaluateAtPoint(ox.getX(), ox.getY(), ox.getZ());
                if(Double.isNaN(oxDensity) || oxDensity < minDensity)
                {
                    if(textOutput) 
                    {
                        if(shearTest)
                        {
                            Residue r1 = r.getPrev(model);
                            Residue r4 = r.getNext(model).getNext(model);
                            if(r1 == null || r4 == null) continue;
                            out.println(fileLabel.toLowerCase()+":"+model+":"
                                +r1.getChain()+":"+r1.getSequenceInteger()+":"+r1.getInsertionCode()+":"+r1.getName()+":"
                                +r4.getChain()+":"+r4.getSequenceInteger()+":"+r4.getInsertionCode()+":"+r4.getName()+":"
                                +df2.format(ox.getTempFactor())+":bad_density:bad_density");
                        }
                        else out.println(fileLabel.toLowerCase()+":"+model+":"+r.getCNIT()+":bad_density:bad_density");
                    }
                    continue; // skip this oxygen
                }
                
                // iterate through several density contour levels
                double maxEllipseRatio = Double.NEGATIVE_INFINITY;
                double orientationAtMaxEllipseRatio = Double.NaN;
                ArrayList envelopeAtMaxEllipseRatio = null;
                double[] lengthsAtMaxEllipseRatio = null;
                Tuple3[] axesAtMaxEllipseRatio = null;
                double envSigma = envSigmaMin;
                while(envSigma <= envSigmaMax)
                {
                    double envDensity = envSigma * map.sigma;
                    // find envelope points
                    ArrayList envelope = new ArrayList();
                    for(double i = 0; i < numSamples; i++)
                    {
                        // unit vector perpendicular to C--O
                        Triple inplane = builder.construct4(ca1, c, ox, 1, 90, 360*i/numSamples);
                        inplane.sub(ox); // to get to vector form
                        envelope.add( findBelow(envDensity, map, ox, inplane) );
                    }
                    // calculate principle axes
                    PrincipleAxes pca = new PrincipleAxes(envelope);
                    Tuple3[] axes = pca.getAxes();
                    double[] lengths = pca.getLengths();
                    // print axis ratio and orientation
                    double orientation = caca.angle(axes[0]);
                    if(orientation > 90) orientation = 180 - orientation; // correct for arbitrary sign
                    double ellipseRatio = lengths[0] / lengths[1];
                    if(ellipseRatio > maxEllipseRatio)
                    {
                        maxEllipseRatio = ellipseRatio;
                        orientationAtMaxEllipseRatio = orientation;
                        envelopeAtMaxEllipseRatio = envelope;
                        lengthsAtMaxEllipseRatio = lengths;
                        axesAtMaxEllipseRatio = axes;
                    }
                    if(verbose) System.err.println(r+" "+df3.format(envSigma)+"sigma: "+orientation+", "+ellipseRatio);
                    envSigma += 0.1;
                }
                
                if(textOutput)
                {
                    if(shearTest)
                    {
                        Residue r1 = r.getPrev(model);
                        Residue r4 = r.getNext(model).getNext(model);
                        if(r1 == null || r4 == null) continue;
                        out.println(fileLabel.toLowerCase()+":"+model+":"
                            +r1.getChain()+":"+r1.getSequenceInteger()+":"+r1.getInsertionCode()+":"+r1.getName()+":"
                            +r4.getChain()+":"+r4.getSequenceInteger()+":"+r4.getInsertionCode()+":"+r4.getName()+":"
                            +df2.format(ox.getTempFactor())+":"+df.format(orientationAtMaxEllipseRatio)+":"+df.format(maxEllipseRatio));
                    }
                    else out.println(fileLabel.toLowerCase()+":"+model+":"+r.getCNIT()+":"+df2.format(ox.getTempFactor())+":"
                        +df.format(orientationAtMaxEllipseRatio)+":"+df.format(maxEllipseRatio));
                }
                else
                {
                    // print kinemage info
                    out.println("@vectorlist {"+fileLabel+":"+model+":"+r.getCNIT()+"} color= orange");
                    for(Iterator iter = envelopeAtMaxEllipseRatio.iterator(); iter.hasNext(); )
                    {
                        Triple t = (Triple) iter.next();
                        out.println("{} "+t.format(df));
                    }
                    
                    // start at O for now -- I don't know where on the envelope these two axes go to & from!
                    Triple axis0 = new Triple(ox).add( ((Triple) axesAtMaxEllipseRatio[0]).mult(lengthsAtMaxEllipseRatio[0]) );
                    Triple axis1 = new Triple(ox).add( ((Triple) axesAtMaxEllipseRatio[1]).mult(lengthsAtMaxEllipseRatio[1]) );
                    out.println("@arrowlist {"+fileLabel+":"+model+":"+r.getCNIT()+" 1st PC} radius= 0.05 color= purple");
                    out.println("{}P "+ox.format(df));
                    out.println("{} "+axis0.format(df));
                    out.println("@arrowlist {"+fileLabel+":"+model+":"+r.getCNIT()+" 2nd PC} radius= 0.05 color= lilactint");
                    out.println("{}P "+ox.format(df));
                    out.println("{} "+axis1.format(df));
                }
            }
            catch(Exception ex) // Atom, NullPtr
            {
                //out.println(fileLabel+":"+model+":"+r.getCNIT()+":missing_atom:missing_atom");
                //ex.printStackTrace();
            }
        }
    }
//}}}

//{{{ findBelow
//##############################################################################
    Triple findBelow(double target, CrystalVertexSource map, Tuple3 startPt, Tuple3 dirVect)
    {
        final double stepSize = 0.025; // should be good enough for gov't work
        Triple searchPath = new Triple();
        /*double density = map.evaluateAtPoint(startPt.getX(), startPt.getY(), startPt.getZ());
        double densityPrev, searchDist = 0;*/
        double density, searchDist = 0;
        do {
            searchDist += stepSize;
            searchPath.like(dirVect).unit().mult(searchDist);
            searchPath.add(startPt);
            /*densityPrev = density;*/
            density = map.evaluateAtPoint(searchPath.getX(), searchPath.getY(), searchPath.getZ());
        } while(density > target);
        /*} while(density > target && (searchDist < 0.4 || density < densityPrev));*/
        return searchPath;
        /* Would be nice to check that we haven't bridged connected density 
        /* to an adjacent atom, as was possible with the simple approach above,
        /* but requiring a downhill trajectory doesn't work b/c of bumps, 
        /* and the only other thing I can think of is a distance cutoff for the search path,
        /* which would be really arbitrary... */
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
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
        PdbReader reader = new PdbReader();
        CoordinateFile cf = reader.read(pdbFile);
        Model m = (Model) cf.getFirstModel();
        ModelState state = m.getState();
        String label = pdbFile.toString();
        if(cf.getIdCode() != null) label = cf.getIdCode();
        
        InputStream is = new BufferedInputStream(new FileInputStream(mapFile));
        CrystalVertexSource map = new OMapVertexSource(is);
        is.close();
        
        //searchModel(System.out, label, m, state, map);
        searchModel2(System.out, label, m, state, map);
    }

    public static void main(String[] args)
    {
        AnisoOxygenSearch mainprog = new AnisoOxygenSearch();
        try
        {
            mainprog.parseArguments(args);
            if(mainprog.pdbFile == null || mainprog.mapFile == null)
                throw new IllegalArgumentException("Must specify both a PDB file and an O map");
            
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
            InputStream is = getClass().getResourceAsStream("AnisoOxygenSearch.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'AnisoOxygenSearch.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.AnisoOxygenSearch");
        System.err.println("Copyright (C) 2004 by Ian W. Davis. All rights reserved.");
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
        if(pdbFile == null)         pdbFile = new File(arg);
        else if(mapFile == null)    mapFile = new File(arg);
        else throw new IllegalArgumentException("Unneccessary parameter '"+arg+"'");
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
        else if(flag.equals("-shear") || flag.equals("-s"))
        {
            shearTest = true;
        }
        else if(flag.equals("-text"))
        {
            textOutput = true;
        }
        else if(flag.equals("-kin"))
        {
            textOutput = false;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

