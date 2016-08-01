// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
//}}}
/**
* <code>KinemageSignal</code> provides a reusable framework
* for publish-subscribe message passing.
* This signal is used to notify listeners of changes to a kinemage.
*
* This signal is not thread-safe.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 27 10:11:30 EST 2003
*/
public class KinemageSignal implements KinemageSignalSubscriber
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    ArrayList       subscribers;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public KinemageSignal()
    {
        subscribers = new ArrayList();
    }
//}}}

//{{{ signalKinemage
//##################################################################################################
    /**
    * A call to this method will publish this signal (i.e., call this method)
    * for each subscriber, one at a time, in the current thread.
    * Because this class also implements the subscriber interface,
    * these signals can be chained together to create deep networks.
    *
    * @param kinemage   the Kinemage object that has changed
    * @param bitmask    a set of flags describing which properties have changed.
    */
    public void signalKinemage(Kinemage kinemage, int bitmask)
    {
        KinemageSignalSubscriber subscriber;
        Iterator iter = subscribers.iterator();
        while(iter.hasNext())
        {
            subscriber = (KinemageSignalSubscriber)iter.next();
            subscriber.signalKinemage(kinemage, bitmask);
        }
    }
//}}}

//{{{ subscribe, getSubscribers
//##################################################################################################
    /**
    * Adds a subscriber to this signal. The subscriber will be notified
    * whenever this signal is activated. Every subscriber must be unique,
    * so if there is some current subscriber oldSubscriber such that
    * <code>newSubscriber.equals(oldSubscriber)</code>, then oldSubscriber
    * will be removed before newSubscriber is added.
    *
    * <p>Subscribers are notified in the same order they were added; however,
    * they should NOT rely on this as it may change in future implementations.
    */
    public void subscribe(KinemageSignalSubscriber newSubscriber)
    {
        if(newSubscriber == null) return;
        
        int i = subscribers.indexOf(newSubscriber);
        if(i != -1) subscribers.remove(i);
        subscribers.add(newSubscriber);
    }
    
    /** Returns an unmodifiable Collection of the current subscriber list */
    public Collection getSubscribers()
    { return Collections.unmodifiableCollection(subscribers); }
//}}}

//{{{ unsubscribe, unsubscribeAll
//##################################################################################################
    /**
    * Removes a subscriber from this signal; the subscriber will no
    * longer be notified when this signal is activated.
    * @return the subscriber that was removed, or null if oldSubscriber was not subscribed
    */
    public KinemageSignalSubscriber unsubscribe(KinemageSignalSubscriber oldSubscriber)
    {
        if(oldSubscriber == null) return null;
        
        int i = subscribers.indexOf(oldSubscriber);
        if(i != -1) return (KinemageSignalSubscriber)subscribers.remove(i);
        else        return null;
    }
    
    /**
    * Removes all subscribers from this signal's distribution list.
    */
    public void unsubscribeAll()
    { subscribers.clear(); }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

