// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.sc;

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
import driftwood.util.*;
//}}}
/**
* <code>RotamersForTerwilliger</code> writes out our rotamer library in the
* format specified by Tom Terwilliger for use in the automated structure
* solution package Phenix (sp?).
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue May 18 16:04:53 EDT 2004
*/
public class RotamersForTerwilliger //extends ... implements ...
{
//{{{ Constants
    String DUMMY_GLY_ALA =
"     1    GLY  ! RESIDUE ID -----------------------\n"+
"   4  ! N_ATOMS\n"+
"   1  ! N_GROUP\n"+
"   1  ! I_GROUP ---------------\n"+
"    1.46    0.00    0.00  N     ! XYZ\n"+
"    0.00    0.00    0.00  CA    ! XYZ\n"+
"   -0.53    1.43    0.00  C     ! XYZ\n"+
"    0.21    2.39   -0.13  O     ! XYZ\n"+
"     2    ALA  ! RESIDUE ID -----------------------\n"+
"   5  ! N_ATOMS\n"+
"   1  ! N_GROUP\n"+
"   1  ! I_GROUP ---------------\n"+
"    1.45    0.00    0.00  N     ! XYZ\n"+
"    0.00    0.00    0.00  CA    ! XYZ\n"+
"   -0.49    1.44    0.00  C     ! XYZ\n"+
"   -0.61    2.07   -1.04  O     ! XYZ\n"+
"   -0.53   -0.77    1.20  CB    ! XYZ";
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RotamersForTerwilliger()
    {
        super();
    }
//}}}

//{{{ writeRotamers
//##############################################################################
    void writeRotamers(PrintStream out) throws IOException
    {
        final String[] resList = {"GLY", "ALA", "SER", "VAL", "ILE", "LEU", "MET",
            "CYS", "PHE", "TYR", "LYS", "ARG", "TRP", "HIS", "GLU", "ASP", "GLN",
            "ASN", "PRO", "THR"
        };
        DecimalFormat df = new DecimalFormat("0.00");
        
        SidechainIdealizer  idealizer   = new SidechainIdealizer();
        SidechainAngles2    scangles    = new SidechainAngles2();
        Builder             builder     = new Builder();
        // Skip GLY, ALA
        out.println(DUMMY_GLY_ALA);
        for(int i = 2; i < resList.length; i++)
        {
            out.println(Strings.justifyRight(""+(i+1), 6)+"    "+resList[i]+"  ! RESIDUE ID -----------------------");
            // Create an idealized residue of the appropriate type
            ModelState stateRaw = new ModelState(); // will be written into by next call
            Residue res = idealizer.makeIdealResidue(' ', "", i+1, ' ', resList[i], stateRaw);
            stateRaw = stateRaw.createCollapsed();
            // Orient the residue correctly (transform in place)
            AtomState n = stateRaw.get(res.getAtom(" N  "));
            AtomState ca = stateRaw.get(res.getAtom(" CA "));
            AtomState c = stateRaw.get(res.getAtom(" C  "));
            Transform xform = builder.dock3on3(new Triple(0,0,0), new Triple(1,0,0), new Triple(0,1,0), ca, n, c);
            for(Iterator iter = stateRaw.getLocalStateMap().values().iterator(); iter.hasNext(); )
                xform.transform((AtomState) iter.next());
            ModelState stateDock = stateRaw;
            // Find a list of all the heavy atoms
            Collection heavyAtoms = new ArrayList();
            for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
            {
                Atom a = (Atom) iter.next();
                if(a.getName().charAt(1) != 'H')
                    heavyAtoms.add(a);
            }
            out.println(Strings.justifyRight(""+heavyAtoms.size(), 4)+"  ! N_ATOMS");
            // Find a list of all the rotamers possible for this sidechain
            RotamerDef[] rotamers = scangles.getAllRotamers(res);
            out.println(Strings.justifyRight(""+rotamers.length, 4)+"  ! N_GROUP");
            // Create a model of each rotamer and write it out
            for(int j = 0; j < rotamers.length; j++)
            {
                out.println(Strings.justifyRight(""+(j+1), 4)+"  ! I_GROUP --------------- "+rotamers[j].toString());
                ModelState stateRot = scangles.setChiAngles(res, stateDock, rotamers[j].chiAngles);
                for(Iterator iter = heavyAtoms.iterator(); iter.hasNext(); )
                {
                    Atom a = (Atom) iter.next();
                    AtomState as = stateRot.get(a);
                    out.println(
                        Strings.justifyRight(df.format(as.getX()), 8)+
                        Strings.justifyRight(df.format(as.getY()), 8)+
                        Strings.justifyRight(df.format(as.getZ()), 8)+
                        " "+a.getName()+"   ! XYZ"
                    );
                }
            }
        }
    }
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
    public void Main()
    {
        try
        {
            writeRotamers(System.out);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        RotamersForTerwilliger mainprog = new RotamersForTerwilliger();
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
            InputStream is = getClass().getResourceAsStream("RotamersForTerwilliger.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'RotamersForTerwilliger.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("chiropraxis.sc.RotamersForTerwilliger");
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
        // Handle files, etc. here
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

