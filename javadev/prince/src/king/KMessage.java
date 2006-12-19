// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
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
* <code>KMessage</code> is a base class for "messages" or "events" to be
* passed around KiNG.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Dec 18 14:13:27 EST 2006
*/
public class KMessage //extends ... implements ...
{
    /** The interface to be implemented by all listeners / subscribers. */
    public interface Subscriber
    {
        /** Called when there is a KMessage to deliver to this Subscriber. */
        public void deliverMessage(KMessage msg);
    }
    
//{{{ Constants
    /** A new kinemage has been loaded from disk */
    public static final long KIN_LOADED         = (1L<<0);
    /** A different kinemage has become the currently active one */
    public static final long KIN_SWITCHED       = (1L<<1);
    /** A kinemage (presumably the current one) has been closed */
    public static final long KIN_CLOSED         = (1L<<2);
    /** All open kinemages have been closed */
    public static final long ALL_CLOSED         = (1L<<3);
    /** The KingPrefs object has been updated */
    public static final long PREFS_CHANGED      = (1L<<4);
    /** The current viewpoint has been altered: center moved, zoom changed, rotated, etc */
    public static final long VIEW_MOVED         = (1L<<5);
    /** A totally different viewpoint has been selected from the Views menu */
    public static final long VIEW_SELECTED      = (1L<<6);
//}}}

//{{{ Variable definitions
//##############################################################################
    protected Object    source      = null;
    protected Kinemage  kinemage    = null;
    protected int       kinChanges  = 0;
    protected long      progChanges = 0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KMessage(Object source)
    { this(source, 0); }
    
    /** "source" must not ever be a Kinemage, as a safety measure */
    public KMessage(Object source, long progChanges)
    {
        super();
        if(source instanceof Kinemage)
            throw new IllegalArgumentException("Source object for program-type events cannot be a Kinemage");
        
        this.source         = source;
        this.progChanges    = progChanges;
    }

    public KMessage(Kinemage kinemage, int kinChanges)
    {
        super();
        this.kinemage       = kinemage;
        this.kinChanges     = kinChanges;
    }
//}}}

//{{{ getSource/Kinemage/{Kinemage, Program}Changes
//##############################################################################
    /** Returns the "source" or originator of this message. (May be null.) */
    public Object getSource()
    { return this.source; }
    
    /** Returns the kinemage to which the event flags apply, if this is a kinemage message. (May be null.) */
    public Kinemage getKinemage()
    { return this.kinemage; }
    
    public int getKinemageChanges()
    { return this.kinChanges; }
    
    public long getProgramChanges()
    { return this.progChanges; }
//}}}

//{{{ testKin/Prog
//##############################################################################
    public boolean testKin(int mask)
    { return (this.kinChanges & mask) != 0; }
    
    public boolean testProg(long mask)
    { return (this.progChanges & mask) != 0; }
//}}}

//{{{ toString
//##############################################################################
    public String toString()
    {
        return "[kinChanges="+Integer.toHexString(kinChanges)
            +",progChanges="+Long.toHexString(progChanges)+"]";
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

