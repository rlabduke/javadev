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
* <code>SpinAroundScene</code> implements spinning about the Y axis.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 15 13:13:33 EST 2007
*/
public class SpinAroundScene extends Scene
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.#");
//}}}

//{{{ Variable definitions
//##############################################################################
    double degrees;
    
    TablePane2 cp;
    JTextField tfDegrees, tfDuration;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SpinAroundScene(KingMain kMain, int duration, double degrees)
    {
        super(kMain, duration);
        captureKinemageState();
        this.degrees = degrees;
    }
//}}}

//{{{ renderFrames, toString
//##############################################################################
    public void renderFrames(MovieMaker maker) throws IOException
    {
        restoreKinemageState();
        for(int i = 0; i < duration; i++)
        {
            KView nextView = (KView) kMain.getView().clone();
            nextView.rotateY((float)(Math.toRadians(degrees) / duration));
            kMain.setView(nextView);
            maker.writeFrame();
        }
    }
    
    public String toString()
    { return "Spin ("+duration+" frames, "+df.format(degrees)+" degrees)"; }
//}}}

//{{{ configure
//##############################################################################
    public boolean configure()
    {
        if(cp == null)
        {
            tfDegrees = new JTextField();
            tfDegrees.setText(df.format(degrees));
            tfDuration = new JTextField();
            tfDuration.setText(Integer.toString(duration));
            
            cp = new TablePane2().hfill(true).memorize();
            cp.addCell(new JLabel("Duration (frames):")).addCell(tfDuration).newRow();
            cp.addCell(new JLabel("Extent (degrees):")).addCell(tfDegrees).newRow();
        }
        
        int result = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            cp, "Configure Spin",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if(result == JOptionPane.OK_OPTION)
        {
            try { this.duration = Integer.parseInt(tfDuration.getText()); }
            catch(NumberFormatException ex) { tfDuration.setText(Integer.toString(duration)); }
            try { this.degrees = Double.parseDouble(tfDegrees.getText()); }
            catch(NumberFormatException ex) { tfDegrees.setText(df.format(degrees)); }
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
        kMain.getView().rotateY((float)Math.toRadians(degrees));
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

