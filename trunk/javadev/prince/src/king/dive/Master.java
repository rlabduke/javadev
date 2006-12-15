// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.*;
//}}}
/**
* <code>Master</code> distributes Commands to a bunch of Slaves for rendering.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec 15 08:03:06 EST 2006
*/
public class Master //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Props props;
    Collection<ObjectLink<Command,String>> slaveLinks = new ArrayList<ObjectLink<Command,String>>();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Master()
    {
        super();
        Props defaultProps = new Props();
        try { defaultProps.load( getClass().getResourceAsStream("default.props") ); }
        catch(IOException ex) { ex.printStackTrace(); }
        this.props = new Props(defaultProps);
    }
//}}}

//{{{ sendCommand
//##############################################################################
    public void sendCommand(Command c)
    {
        for(ObjectLink<Command,String> link : slaveLinks)
        {
            try { link.put(c); }
            catch(IOException ex)
            { System.out.println("Error commanding slave: "+ex.getMessage()); }
        }
    }
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
        int port = props.getInt("master.port");
        System.out.println("Listening for slaves on port "+port);
        
        ServerSocket server = null;
        try
        {
            server = new ServerSocket(port);
            while(true)
            {
                Socket socket = server.accept();
                try
                {
                    ObjectLink<Command,String> link = new ObjectLink<Command,String>(socket);
                    slaveLinks.add(link);
                }
                catch(IOException ex)
                { System.err.println("Failed to connect with slave: "+ex.getMessage()); }
            }
        }
        catch(IOException ex)
        { ex.printStackTrace(); }
        finally
        {
            for(ObjectLink<Command,String> link : slaveLinks)
                link.disconnect();
            try { if(server != null) server.close(); } catch(IOException ex) {}
        }
    }

    public static void main(String[] args)
    {
        Master mainprog = new Master();
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
            InputStream is = getClass().getResourceAsStream("Master.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'Master.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("king.dive.Master");
        System.err.println("Copyright (C) 2006 by Ian W. Davis. All rights reserved.");
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

