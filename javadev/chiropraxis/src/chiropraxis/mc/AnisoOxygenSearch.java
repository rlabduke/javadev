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
        boolean txtOutput = true;
        
        final double minDensity = 2.0 * map.sigma; // at current O position
        final double envDensity = 1.2 * map.sigma; // ED for "envelope" we're examining
        final int numSamples = 64; // number of samples taken around the envelope
        
        Builder builder = new Builder();
        DecimalFormat df = new DecimalFormat("0.0000");
        if(txtOutput) out.println("#file:model:residue:angle:ellipse_ratio");
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
                vert.mult(caca.dot(vert));                             // length of CaCa in C-O direction
                caca.sub(vert);                                         // remove C-O component
                // check center density
                double oxDensity = map.evaluateAtPoint(ox.getX(), ox.getY(), ox.getZ());
                if(Double.isNaN(oxDensity) || oxDensity < minDensity)
                {
                    if(txtOutput) out.println(fileLabel+":"+model+":"+r.getCNIT()+":bad_density:bad_density");
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
                if(txtOutput) out.println(fileLabel+":"+model+":"+r.getCNIT()+":"+df.format(orientation)+":"+df.format(ellipseRatio));
                else
                {
                    // print kinemage info
                    out.println("@vectorlist {"+fileLabel+":"+model+":"+r.getCNIT()+"} color= orange");
                    for(Iterator iter = envelope.iterator(); iter.hasNext(); )
                    {
                        Triple t = (Triple) iter.next();
                        out.println("{} "+t.format(df));
                    }
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
        double density, searchDist = 0;
        do {
            searchDist += stepSize;
            searchPath.like(dirVect).unit().mult(searchDist);
            searchPath.add(startPt);
            density = map.evaluateAtPoint(searchPath.getX(), searchPath.getY(), searchPath.getZ());
        } while(density > target);
        return searchPath;
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
        
        searchModel(System.out, label, m, state, map);
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
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

