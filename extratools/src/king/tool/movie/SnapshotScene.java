// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.movie;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
//}}}
/**
* <code>SnapshotScene</code> just shows the same still image for N frames.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan 16 10:11:03 EST 2007
*/
public class SnapshotScene extends Scene
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    TablePane2 cp;
    JTextField tfDuration;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SnapshotScene(KingMain kMain, int duration)
    {
        super(kMain, duration);
        captureKinemageState();
    }
//}}}

//{{{ renderFrames, toString
//##############################################################################
    public void renderFrames(MovieMaker maker) throws IOException
    {
        restoreKinemageState();
        maker.writeFrame(this.duration);
    }
    
    public String toString()
    { return "Snapshot ("+duration+" frames)"; }
//}}}

//{{{ configure
//##############################################################################
    public boolean configure()
    {
        if(cp == null)
        {
            tfDuration = new JTextField();
            tfDuration.setText(Integer.toString(duration));
            
            cp = new TablePane2().hfill(true).memorize();
            cp.addCell(new JLabel("Duration (frames):")).addCell(tfDuration).newRow();
        }
        
        int result = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            cp, "Configure Snapshot",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if(result == JOptionPane.OK_OPTION)
        {
            try { this.duration = Integer.parseInt(tfDuration.getText()); }
            catch(NumberFormatException ex) { tfDuration.setText(Integer.toString(duration)); }
            return true;
        }
        else return false;
    }
//}}}

//{{{ gotoEndState
//##############################################################################
    public void gotoEndState()
    {
        restoreKinemageState();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

