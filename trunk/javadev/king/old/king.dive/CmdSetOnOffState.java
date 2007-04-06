// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>CmdSetOnOffState</code> synchronizes the on/off state of buttons and
* masters between two identical kinemages.
* Bad things may happen if the kinemages are not identical, of course.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan  8 13:30:56 EST 2007
*/
public class CmdSetOnOffState implements Command
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    BitSet on_off;
    BitSet masters;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CmdSetOnOffState(Kinemage kin)
    {
        super();
        
        int i = 0;
        this.on_off = new BitSet();
        for(AGE age : KIterator.allNonPoints(kin))
            on_off.set(i++, age.isOn());
        
        i = 0;
        this.masters = new BitSet();
        for(MasterGroup master : kin.masterList())
            masters.set(i++, master.isOn());
    }
//}}}

//{{{ doCommand
//##############################################################################
    public void doCommand(Slave slave)
    {
        Kinemage kin = slave.kin;
        
        int i = 0;
        for(AGE age : KIterator.allNonPoints(kin))
        {
            boolean state = this.on_off.get(i++);
            if(state != age.isOn())
                age.setOn(state);
        }

        i = 0;
        for(MasterGroup master : kin.masterList())
        {
            boolean state = this.masters.get(i++);
            if(state != master.isOn())
                master.setOn(state);
        }

        slave.canvas.repaint();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

