// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.movie;
import king.*;
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
* <code>Scene</code> is the abstract base class for "scenes" of a movie.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 15 10:01:22 EST 2007
*/
public abstract class Scene //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    protected KingMain  kMain;
    protected int       duration; // measured in frames

    // Current kinemage state
    protected KView     view = null;
    protected Aspect    aspect = null;
    protected BitSet    on_offState = null;
    protected BitSet    mastersState = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Scene(KingMain kMain, int duration)
    {
        super();
        this.kMain      = kMain;
        this.duration   = duration;
    }
//}}}

//{{{ capture/restoreKinemageState
//##############################################################################
    protected void captureKinemageState()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        this.view = kMain.getView().clone();
        this.aspect = kMain.getCanvas().getCurrentAspect();
        
        int i = 0;
        this.on_offState = new BitSet();
        for(AGE age : KIterator.allNonPoints(kin))
            on_offState.set(i++, age.isOn());
        
        i = 0;
        this.mastersState = new BitSet();
        for(MasterGroup master : kin.masterList())
            mastersState.set(i++, master.isOn());
    }
    
    protected void restoreKinemageState()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        if(this.view != null)
            kMain.setView(view);
          kMain.getCanvas().setCurrentAspect(this.aspect); // null is OK
        if(this.on_offState != null)
        {
            int i = 0;
            for(AGE age : KIterator.allNonPoints(kin))
            {
                boolean state = this.on_offState.get(i++);
                if(state != age.isOn())
                    age.setOn(state);
            }
        }
        if(this.mastersState != null)
        {
            int i = 0;
            for(MasterGroup master : kin.masterList())
            {
                boolean state = this.mastersState.get(i++);
                if(state != master.isOn())
                    master.setOn(state);
            }
        }
    }
//}}}

//{{{ renderFrames, configure, getDuration, toString
//##############################################################################
    /**
    * Actually write the frames of the movie.
    * The kinemage is guaranteed to be in a consistent "end state"
    * from the end of the last scene when this method is called.
    * No guarantee about the kinemage state is provided when this
    * method returns -- see gotoEndState().
    */
    abstract public void renderFrames(MovieMaker maker) throws IOException;
    
    /** True means "OK", false means "Cancel" */
    abstract public boolean configure();
    
    /**
    * Puts the kinemage into a consistent state, as it should look after
    * all frames of the current scene are rendered.
    */
    abstract public void gotoEndState();
    
    public int getDuration()
    { return duration; }
    
    /** This will be displayed in the MovieMaker GUI */
    public String toString()
    { return super.toString()+" **please override**"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

