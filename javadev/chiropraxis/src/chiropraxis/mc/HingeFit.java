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
* <code>HingeFit</code> applies Backrub-like hinges to
* minize Ca-RMSD between two loops.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Sep  9 08:48:55 EDT 2003
*/
public class HingeFit //extends ... implements ...
{
//{{{ Constants
    static final Triple X_AXIS = new Triple(1, 0, 0);
    static final Triple Y_AXIS = new Triple(0, 1, 0);
    static final Triple Z_AXIS = new Triple(0, 0, 1);
    static final Triple ORIGIN = new Triple(0, 0, 0);
//}}}

//{{{ Variable definitions
//##############################################################################
    Builder         builder;
    
    AtomState[]     ca1     = null;
    AtomState[]     ca2     = null;
    Triple[]        tmp1    = null;
    Triple[]        tmp2    = null;
    String[]        labels  = null;

    File            file1 = null, file2 = null;
    InputStream     input1 = null, input2 = null;
    PrintStream     output = System.out;
    int             numTries = 25;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public HingeFit()
    {
        super();
        builder = new Builder();
    }
//}}}

//{{{ initData, loadAtomStates
//##############################################################################
    void initData() throws IOException
    {
        // Make arrays of C-alphas from input PDB files
        ca1 = loadAtomStates(input1);
        ca2 = loadAtomStates(input2);
        if(ca1.length != ca2.length)
            throw new IllegalArgumentException("Selections must have same number of C-alphas");
        
        // Allocate a scratch space
        tmp1 = new Triple[ ca1.length ];
        tmp2 = new Triple[ ca1.length ];
        for(int i = 0; i < ca1.length; i++) { tmp1[i] = new Triple(); tmp2[i] = new Triple(); }
        
        // Get residue names out
        labels = new String[ca1.length];
        for(int i = 0; i < ca1.length; i++) labels[i] = ca1[i].getResidue().toString();
    }
    
    AtomState[] loadAtomStates(InputStream in) throws IOException
    {
        if(in == null)
            throw new IllegalArgumentException("Must supply two input files");
        
        // Load model group from PDB files
        PdbReader pdbReader = new PdbReader();
        ModelGroup mg = pdbReader.read(in);
        Model m = mg.getFirstModel();
        
        // Extract the C-alphas
        Collection  res     = m.getResidues();
        ModelState  state   = m.getState();
        ArrayList   atoms   = new ArrayList();
        for(Iterator iter = res.iterator(); iter.hasNext(); )
        {
            Residue r = (Residue)iter.next();
            Atom a = r.getAtom(" CA ");
            try { if(a != null) atoms.add(state.get(a)); }
            catch(AtomException ex) { ex.printStackTrace(); } // should never happen
        }
        
        // Make them into an array
        AtomState[] ca = (AtomState[])atoms.toArray(new AtomState[atoms.size()]);
        return ca;
    }
//}}}

//{{{ calcRMSD, transform
//##############################################################################
    double calcRMSD(Triple[] t1, Triple[] t2)
    {
        double r = 0;
        for(int i = 0; i < t1.length; i++)
            r += t1[i].sqDistance(t2[i]);
        r = Math.sqrt(r / t1.length);
        return r;
    }
    
    void transform(Triple[] from, Triple[] to, Transform t)
    {
        for(int i = 0; i < from.length; i++)
            t.transform(from[i], to[i]);
    }
    
    void transform(Triple[] from, Triple[] to, int first, int last, Transform t)
    {
        for(int i = first; i <= last; i++)
            t.transform(from[i], to[i]);
    }
//}}}

//{{{ bestFitZ
//##############################################################################
    /**
    * Finds the angle of rotation around Z
    * that minimizes the RMSD between ref and mob.
    */
    double bestFitZ(Triple[] ref, Triple[] mob, int i, int j)
    {
        Triple[] tmp = new Triple[ mob.length ];
        for(int k = 0; k < tmp.length; k++) tmp[k] = new Triple(mob[k]);
        
        double startRMSD = calcRMSD(ref, mob);
        double rmsd1, rmsd2, r;
        double angle, bestAngle = 0;
        Transform t = new Transform();
        
        r = startRMSD;
        angle = 0;
        while(true)
        {
            rmsd1 = r;
            angle += 0.05;
            t.likeRotation(Z_AXIS, angle);
            transform(mob, tmp, i, j, t);
            r = calcRMSD(ref, tmp);
            if(r > rmsd1) break;
            else bestAngle = angle;
        }

        r = startRMSD;
        angle = 0;
        while(true)
        {
            rmsd2 = r;
            angle -= 0.05;
            t.likeRotation(Z_AXIS, angle);
            transform(mob, tmp, i, j, t);
            r = calcRMSD(ref, tmp);
            if(r > rmsd2) break;
            else if(r < rmsd1) bestAngle = angle;
        }
        
        return bestAngle;
    }
//}}}

//{{{ findBestHinge
//##############################################################################
    double findBestHinge()
    {
        DecimalFormat df = new DecimalFormat("0.0000");
        int i, j, best_i = 0, best_j = 0;
        double startRMSD = calcRMSD(ca1, ca2);
        double bestTheta = Double.NaN, bestRMSD = startRMSD;
        
        for(i = 0; i < ca2.length; i++)
        {
            for(j = i+2; j < ca2.length; j++)
            {
                // Translate so that our axis of rotation matches the Z axis
                // Third point is arbitrary -- we just needed something to use.
                // This is no longer really necessary -- bestFitZ could rotate
                // around the i-j axis directly.
                Transform t = builder.dock3on3(ORIGIN, Z_AXIS, X_AXIS, ca2[i], ca2[j], ca2[i+1]);
                transform(ca1, tmp1, t);
                transform(ca2, tmp2, t);
                
                // Rotate object2 to best fit object1
                double theta = bestFitZ(tmp1, tmp2, i, j);
                t.likeRotation(Z_AXIS, theta);
                transform(tmp2, tmp2, i, j, t);
                
                // Compare RMSDs
                double rmsd = calcRMSD(tmp1, tmp2);
                if(rmsd < bestRMSD)
                {
                    bestRMSD = rmsd;
                    best_i = i;
                    best_j = j;
                    bestTheta = theta;
                }
            }
        }
        
        // Apply the best hinge to our original data
        Transform t = new Transform().likeRotation(ca2[best_i], ca2[best_j], bestTheta);
        transform(ca2, ca2, best_i, best_j, t);
        
        System.err.println("Most advantageous rotation: "+df.format(bestTheta)+" degrees between "
            +labels[best_i]+" and "+labels[best_j]);
        System.err.println("RMSD (before) = "+df.format(startRMSD)+"; RMSD (after) = "+df.format(bestRMSD)
            +"; diff = "+df.format(bestRMSD-startRMSD));
        //System.err.println("Actual final RMSD = "+df.format( calcRMSD(ca1, ca2) ));
        
        return bestRMSD;
    }
//}}}

//{{{ drawCa1, drawCa2, drawCaTrace
//##############################################################################
    void drawCa1()
    {
        System.out.println("@group {ref CAs} dominant");
        System.out.println("@vectorlist {C-alpha trace} color= white");
        drawCaTrace(ca1);
    }
    
    void drawCa2()
    {
        System.out.println("@group {mobile CAs} dominant animate");
        System.out.println("@vectorlist {C-alpha trace} color= yellowtint");
        drawCaTrace(ca2);
    }
    
    void drawCaTrace(AtomState[] as)
    {
        DecimalFormat df = new DecimalFormat("0.0####");
        for(int i = 0; i < as.length; i++)
        {
            System.out.println("{"+as[i]+"} "+df.format(as[i].getX())+" "
                +df.format(as[i].getY())+" "+df.format(as[i].getZ()));
        }
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        // Sets up ca1, ca2, tmp1, tmp2, and labels
        initData();
        
        System.out.println("@kinemage 1");
        drawCa1();
        drawCa2();

        // Find 10 best hinges
        for(int k = 0; k < numTries; k++)
        {
            findBestHinge();
            System.err.println();
            drawCa2();
        }
        
        try { input1.close(); } catch(IOException ex) {}
        try { input2.close(); } catch(IOException ex) {}
        output.close();
    }

    public static void main(String[] args)
    {
        HingeFit mainprog = new HingeFit();
        try
        {
            mainprog.showHelp(false);
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
            InputStream is = getClass().getResourceAsStream("HingeFit.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'HingeFit.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.mc.HingeFit");
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
        File f = new File(arg);
        try
        {
            if(f.exists())
            {
                if(input1 == null)
                {
                    file1 = f;
                    input1 = new BufferedInputStream(new FileInputStream(f));
                }
                else if(input2 == null)
                {
                    file2 = f;
                    input2 = new BufferedInputStream(new FileInputStream(f));
                }
                else
                    output = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
            }
            else if(input1 != null && input2 != null)
                output = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
            else
                System.err.println("*** WARNING: file '"+f+"' was not found");
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.err.println("*** WARNING: file '"+f+"' was not found");
        }
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
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}

//{{{ empty
//##############################################################################
//}}}
}//class

