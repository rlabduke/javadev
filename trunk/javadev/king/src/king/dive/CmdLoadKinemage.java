// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;
import king.core.*;
import king.io.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>CmdLoadKinemage</code> transmits an entire kinemage, by saving it as
* a gzipped kinemage file and then parsing it on the other end.
* Obviously, properties that do not survive saving and loading will not be
* trasmitted.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan  8 09:46:22 EST 2007
*/
public class CmdLoadKinemage implements Command
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    byte[] buf;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CmdLoadKinemage(Kinemage kin)
    {
        super();
        // No exception should ever occur because it's all in memory.
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer w = new OutputStreamWriter( new GZIPOutputStream( baos ) );
            KinfileWriter kw = new KinfileWriter();
            kw.save(w, "", Collections.singleton(kin));
            w.close(); // required to flush GZIP
            this.buf = baos.toByteArray();
        }
        catch(IOException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ doCommand
//##############################################################################
    public void doCommand(Slave slave)
    {
        // No exception should ever occur because it's all in memory.
        try
        {
            KinfileParser kp = new KinfileParser();
            LineNumberReader lnr = new LineNumberReader(
                new InputStreamReader(
                new GZIPInputStream(
                new ByteArrayInputStream(this.buf))));
            kp.parse(lnr);
            slave.kin = kp.getKinemages().iterator().next();
            slave.canvas.repaint();
        }
        catch(IOException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

