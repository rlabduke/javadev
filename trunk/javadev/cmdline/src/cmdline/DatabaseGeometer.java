// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.r3.*;
import driftwood.util.Strings;

import java.util.*;
import java.io.*;
import java.text.ParseException;
import java.text.DecimalFormat;
//}}}
/**
* <code>DatabaseGeometer</code> is a simple utility class to take a text file
* containing SQL output on an atom-by-atom level and calculate things like bond
* angles. This opens up the possibility of answering such questions as the 
* following: does a given bond angle for a given amino acid type in a given 
* rotamer vary with phi,psi?
*/
public class DatabaseGeometer //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    DecimalFormat df  = new DecimalFormat("###.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    boolean verbose = false;
    String filename = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DatabaseGeometer()
    {
        super();
    }
//}}}

//{{{ processFile
//##############################################################################
    public void processFile(String filename)
    {
        if (verbose) System.err.println("Starting processFile for '"+filename+"'");
        ArrayList<Double> bondAngles = new ArrayList<Double>();
        try
        {
            String[] atomnames = getAtomNames(new Scanner(new File(filename)));
            Triple[] atoms = new Triple[3];
            for (int i = 0; i < 3; i++)   atoms[i] = null;
            
            // Collect data
            Scanner s = new Scanner(new File(filename));
            s.nextLine(); // skip header
            while (s.hasNextLine())
            {
                // "1a12 A 268  ASN ( 1.7  MolP= 2.111 ),N,35.173,-28.552,-0.557,56,-14.2,-65.5,-19.5,10.79,ASN"
                String line = s.nextLine();
                String[] parts = Strings.explode(line, ',');
                
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                double z = Double.parseDouble(parts[4]);
                Triple xyz = new Triple(x,y,z);
                
                if (atoms[0] == null)
                {
                    if (parts[1].equals(atomnames[0]))  atoms[0] = xyz;
                    else
                    {
                        if (verbose) System.err.println("Got off register with atom names => resetting");
                        for (int i = 0; i < 3; i++)  atoms[i] = null;
                    }
                }
                else if (atoms[1] == null)
                {
                    if (parts[1].equals(atomnames[1]))  atoms[1] = xyz;
                    else
                    {
                        if (verbose) System.err.println("Got off register with atom names => resetting");
                        for (int i = 0; i < 3; i++)  atoms[i] = null;
                    }
                }
                else if (atoms[2] == null)
                {
                    if (parts[1].equals(atomnames[2]))
                    {
                        atoms[2] = xyz;
                        double bondAngle = Triple.angle(atoms[0], atoms[1], atoms[2]);
                        bondAngles.add(bondAngle);
                        atoms = new Triple[3];
                        for (int i = 0; i < 3; i++)  atoms[i] = null;
                        if (verbose) System.err.println(
                            "Added angle: "+df.format(bondAngle)+" for "+parts[0]);
                    }
                    else
                    {
                        if (verbose) System.err.println("Got off register with atom names => resetting");
                        for (int i = 0; i < 3; i++)  atoms[i] = null;
                    }
                }
            }
            
            doOutput(bondAngles, atomnames);
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("Problem w/ # in processFile...");
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Problem w/ file in processFile...");
        }
    }
//}}}

//{{{ getAtomNames
//##############################################################################
    public String[] getAtomNames(Scanner s)
    {
        String[] atomnames = new String[3];
        
        s.nextLine(); // skip header
        if (s.hasNextLine())
        {
            String[] parts = Strings.explode(s.nextLine(), ',');
            atomnames[0] = parts[1];
        }
        if (s.hasNextLine())
        {
            String[] parts = Strings.explode(s.nextLine(), ',');
            atomnames[1] = parts[1];
        }
        if (s.hasNextLine())
        {
            String[] parts = Strings.explode(s.nextLine(), ',');
            atomnames[2] = parts[1];
        }
        
        System.err.println("Found atom names: "+atomnames[0]+", "+atomnames[1]+", "+atomnames[2]);
        return atomnames;
    }
//}}}

//{{{ doOutput
//##############################################################################
    public void doOutput(ArrayList<Double> bondAngles, String[] atomnames)
    {
        // Average data
        double sum = 0;
        int count = 0;
        for (double bondAngle : bondAngles)
        {
            sum += bondAngle;
            count ++;
        }
        double avgBondAngle = sum / (1.0*count);
        
        // Calc stdev
        sum = 0;
        for (double bondAngle : bondAngles)
        {
            sum += Math.pow(bondAngle-avgBondAngle, 2);
        }
        double stdev = Math.sqrt( sum / (1.0 * count) );
        
        // Print out results
        System.out.println("Average "+atomnames[0].trim()+"-"+
            atomnames[1].trim()+"-"+atomnames[2].trim()+" angle \t"+
            df.format(avgBondAngle));
        System.out.println("Standard deviation \t"+df.format(stdev));
        System.out.println("Contributors (n) \t"+count);
    }
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException, ParseException
    {
        if (verbose) System.err.println("Starting Main...");
        if (filename == null)
        {
            System.err.println("Need input file!");
            System.exit(0);
        }
        else
        {
            processFile(filename);
        }
    }

    public static void main(String[] args)
    {
        DatabaseGeometer mainprog = new DatabaseGeometer();
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
            InputStream is = getClass().getResourceAsStream("DatabaseGeometer.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'DatabaseGeometer.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.DatabaseGeometer");
        System.err.println("Copyright (C) 2008 by Daniel Keedy. All rights reserved.");
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
        if (filename == null)      filename = arg;
        else throw new IllegalArgumentException("too many arguments!");
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-v"))
        {
            System.out.println("Doing verbose...");
            verbose = true;
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class