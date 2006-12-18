// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

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
    /** A different kinemage has become the currently active one */
    public static final long KIN_SWITCHED       = (1L<<0);
    /** A kinemage (presumably the current one) has been closed */
    public static final long KIN_CLOSED         = (1L<<1);
    /** All open kinemages have been closed */
    public static final long ALL_CLOSED         = (1L<<2);
    /** The KingPrefs object has been updated */
    public static final long PREFS_CHANGED      = (1L<<3);
    /** The current viewpoint has been altered: center moved, zoom changed, rotated, etc */
    public static final long VIEW_MOVED         = (1L<<4);
    /** A totally different viewpoint has been selected from the Views menu */
    public static final long VIEW_SELECTED      = (1L<<5);
//}}}

//{{{ Variable definitions
//##############################################################################
    protected Object    source;
    protected int       kinChanges;
    protected long      progChanges;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KMessage(Object source)
    { this(source, 0, 0); }
    
    public KMessage(Object source, int kinChanges, long progChanges)
    {
        super();
        this.source         = source;
        this.kinChanges     = kinChanges;
        this.progChanges    = progChanges;
    }
//}}}

//{{{ getSource, get/set{Kinemage, Program}Changes
//##############################################################################
    /** Returns the "source" or originator of this message. */
    public Object getSource()
    { return this.source; }
    
    public int getKinemageChanges()
    { return this.kinChanges; }
    
    public void setKinemageChanges(int c)
    { this.kinChanges = c; }
    
    public long getProgramChanges()
    { return this.progChanges; }
    
    public void setProgramChanges(long c)
    { this.progChanges = c; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

