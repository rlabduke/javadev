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
//}}}

//{{{ Variable definitions
//##############################################################################
    protected Object source;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KMessage(Object source)
    {
        super();
        this.source = source;
    }
//}}}

//{{{ getSource
//##############################################################################
    /** Returns the "source" or originator of this message. */
    public Object getSource()
    { return this.source; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

