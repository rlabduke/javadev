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
* <code>AnimateScene</code> implements animating through all available groups.
*
* <p>Copyright (C) 2011 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Thu Dec 29 2011
*/
public class AnimateScene extends Scene
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.#");
//}}}

//{{{ Variable definitions
//##############################################################################
    TablePane2 cp;
    JTextField tfDuration;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AnimateScene(KingMain kMain, int duration)
    {
        // For this particular subclass of Scene,
        // "duration" is per frame in the animate sequence,
        // not total frames across the animate sequence
        super(kMain, duration);
        captureKinemageState();
    }
//}}}

//{{{ renderFrames, toString
//##############################################################################
    public void renderFrames(MovieMaker maker) throws IOException
    {
        restoreKinemageState();
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        for(int i = 0; i < kin.getNumAnimateGroups(); i++)
        {
            kin.animate(1);
            maker.writeFrame(this.duration);
        }
    }
    
    public String toString()
    { return "Animate ("+duration+" frame(s)/group)"; }
//}}}

//{{{ configure
//##############################################################################
    public boolean configure()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return false;
        if(kin.getNumAnimateGroups() < 1)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "No animatable groups are present!", "Oops...", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if(cp == null)
        {
            tfDuration = new JTextField();
            tfDuration.setText(Integer.toString(duration));
            
            cp = new TablePane2().hfill(true).memorize();
            cp.addCell(new JLabel("Duration (frames/group):")).addCell(tfDuration).newRow();
        }
        
        int result = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            cp, "Configure Animate",
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

