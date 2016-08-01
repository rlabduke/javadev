// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.Strings;
//}}}
/**
* <code>TabDataLoader</code> takes a file of "tabular" data
* and parses it into a set of DataSample's.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Apr 16 15:42:40 EDT 2003
*/
public class TabDataLoader //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    SilkOptions opt;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public TabDataLoader(SilkOptions options)
    {
        this.opt    = options;
    }
//}}}

//{{{ parseReader
//##################################################################################################
    /**
    * Takes a file of tabular data, breaks it up according to the rules
    * specified in options, and returns a Collection of DataSample's.
    * Skips lines starting with hash marks (#).
    */
    public Collection parseReader(LineNumberReader in) throws IOException
    {
        ArrayList   ret = new ArrayList();
        String      line;
        String[]    fields;
        DataSample  sample;
        
        while((line = in.readLine()) != null)
        {
            if(line.startsWith("#")) continue;
            
            fields = Strings.explode(line, opt.inSep);
            sample = new DataSample();
            try
            {
                // Remember that the indices from SilkOptions start at 1, not 0
                if(opt.label != 0)
                    sample.label = fields[opt.label-1];
                
                sample.coords = new double[opt.nDim];
                for(int i = 0; i < opt.nDim; i++)
                    sample.coords[i] = Double.parseDouble(fields[ opt.coords[i]-1 ]);
                
                if(opt.weight != 0)
                    sample.weight = Double.parseDouble(fields[ opt.weight-1 ]);
                
                if(opt.color != 0)
                    sample.color = fields[ opt.color-1 ];
                
                ret.add(sample);
            }
            catch(NumberFormatException ex)
            {
                if(opt.verbosity >= SilkOptions.V_VERBOSE)
                    System.err.println("Misformatted number on line "+in.getLineNumber());
            }
            catch(IndexOutOfBoundsException ex)
            {
                if(opt.verbosity >= SilkOptions.V_VERBOSE)
                    System.err.println("Too few fields ("+fields.length+") on line "+in.getLineNumber());
            }
        }
        
        return ret;
    }
//}}}

//{{{ parseFile
//##################################################################################################
    /** Convenience function for calling parseReader() */
    public Collection parseFile(File f) throws IOException
    {
        LineNumberReader lnr = new LineNumberReader(new FileReader(f));
        Collection samples = parseReader(lnr);
        lnr.close();
        return samples;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

