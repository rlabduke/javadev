// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.event.*;
import java.io.*;
//import java.text.*;
import java.util.*;
//import javax.swing.*;
//}}}
/**
* <code>AnimationGroup</code> steps through a series of animation "frames", like a flip book animation.
* It acts on any AGE, but usually on a MasterGroup.
*
* <p>Begun on Wed Jun 12 19:38:47 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class AnimationGroup extends ArrayList // implements ...
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    String ID;
    Kinemage parent;
    int currPos = -1; // tracks the current active frame
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new animation sequence.
    * @param kin the Kinemage that owns this animation 
    * @param label the ID of this animation
    */
    public AnimationGroup(Kinemage kin, String label)
    {
        super();
        parent = kin;
        ID = label;
    }
//}}}

//{{{ reset(), forward(), backward()
//##################################################################################################
    /** Resets the animation to the first frame and displays that */
    public void reset()
    {
        currPos = 0;
        go();
    }
    
    /** Steps the animation forward */
    public void forward()
    {
        currPos++;
        go();
    }

    /** Steps the animation backward */
    public void backward()
    {
        currPos--;
        go();
    }

    /** Actually does the animation */
    void go()
    {
        int size = size();
        if(size == 0) return;
        
        if(currPos < 0)     currPos = size - 1;
        if(currPos >= size) currPos = 0;
        
        Iterator iter = iterator();
        while(iter.hasNext())
        {
            ((AGE)iter.next()).setOn(false);
        }
        
        ((AGE)get(currPos)).setOn(true);
        
        //kMain.notifyChange(KingMain.EM_ON_OFF);
        parent.signal.signalKinemage(parent, parent.signal.APPEARANCE);
    }
//}}}

//{{{ UI (Animations menu) interaction
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void selectedFromMenu(ActionEvent ev)
    {
        if(parent != null) parent.notifyAnimationSelected(this);
        reset();
    }
//}}}

//{{{ toString
//##################################################################################################
    /** Returns this animation's ID */
    public String toString() { return ID; }
//}}}
}//class
