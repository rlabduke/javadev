// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.io.*;
//import java.text.*;
import java.util.*;
//}}}
/**
* <code>MasterGroup</code> is used to implements masters.
* Masters turn on and off disparate elements of the hierarchy with a single button.
* Although they're declared as descendants of AGE, they should contain no children.
* Instead, use <code>AGE.addMaster(masterGroup.toString())</code>.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Jun  2 18:54:11 EDT 2002
*/
public class MasterGroup extends AGE<Kinemage,AHE> // implements ...
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    public      int     pm_mask     = 0;        // no point masters to start with
    protected   boolean indent      = false;
    protected   boolean forceOnOff  = false;    // was state specified in kin? See setOnForced()
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new master control.
    *
    * @param owner the Kinemage that owns this master 
    * @param label the ID of this master
    */
    public MasterGroup(Kinemage owner, String label)
    {
        super();
        setParent(owner);
        setName(label);
    }
//}}}

//{{{ setOn{Limited, Forced}
//##################################################################################################
    /**
    * Turns the checkbox on/off from within the program.
    * Automatically makes the same change to all groups immediately under this one.
    * Doesn't automatically trigger a redraw.
    */
    public void setOn(boolean alive)
    {
        super.setOn(alive);
        touchMaster(alive);
        if(pm_mask != 0) pmHit(pm_mask, alive);
    }
    
    /** 
    * Has the effect of setOn(), but without triggering the master.
    * That is, all groups/subgroups/lists/points that are under the control
    * of this master WILL NOT be affected by this function call.
    */
    public void setOnLimited(boolean alive)
    {
        super.setOn(alive);
    }
    
    /**
    * Has the effect of setOn(), but delays triggering the master until
    * syncState() is called. This is useful for explicit 'on' and 'off'
    * designations of masters in the kinemage file.
    */
    public void setOnForced(boolean alive)
    {
        setOnLimited(alive);
        this.forceOnOff = true;
    }
//}}}

//{{{ touchMaster
//##################################################################################################
    /**
    * Traverses the kinemage, realizing the effects of turning on/off a single master.
    * @param state          true for on, false for off
    */
    private void touchMaster(boolean state)
    {
        if(parent == null) return;
        
        // Build a list of names of masters that are off
        ArrayList<String> mastersOff = new ArrayList<String>();
        for(MasterGroup m : getKinemage().masterList())
            if(!m.isOn()) mastersOff.add(m.getName());
        String[]    mOff    = mastersOff.toArray(new String[mastersOff.size()]);
        String      mName   = this.getName();
        
        // Traverse the hierarchy applying the master
        OUTER: for(AGE age : KIterator.allNonPoints(getKinemage()))
        {
            Collection<String> ageMasters = age.getMasters();
            if(ageMasters.contains(mName) && age.isOn() != state)
            {
                // master was just turned on and age is off
                if(state)
                {
                    for(int i = 0; i < mOff.length; i++)
                        if(ageMasters.contains(mOff[i])) continue OUTER;
                    age.setOn(true);
                }
                // master was just turned off and age is on
                else age.setOn(false);
            }
        }
    }
//}}}

//{{{ syncState
//##################################################################################################
    /**
    * Traverses the kinemage, examining the states of its targets.
    * If the master is on, it will be turned off iff none of its targets are on.
    * If the master is off, all targets will be turned off.
    * Assume that pointmastered points will be on.
    */
    public void syncState()
    {
        if(parent == null) return;
        
        if(this.forceOnOff)
        {
            // This *WILL* trigger the master to turn things it controls ON or OFF
            this.setOn( this.isOn() );
            //this.forceOnOff = false;
        }
        else if(this.isOn())
        {
            // If any AGE controlled by this master is on, this master should be on; else it should be off.
            boolean state = false;
            String masterName = this.getName();
            for(AGE age : KIterator.allNonPoints(getKinemage()))
            {
                if(age.getMasters().contains(masterName)) state |= age.isOn();
                if(state) break;
            }
            if(this.pm_mask != 0) for(KPoint point : KIterator.allPoints(getKinemage()))
            {
                state |= point.pmWouldHit(this.pm_mask);
                if(state) break;
            }
            // This won't trigger the master to turn on groups that are off
            this.setOnLimited(state);
        }
        else
        {
            // Master wasn't on, but wasn't forced (ie wasn't explicitly specified in the kin).
            // This should never happen, but if it did no action would be needed.
        }
    }
//}}}

//{{{ get/setPmMask, pmHit
//##################################################################################################
    public int getPmMask()
    { return pm_mask; }
    
    /** Sets the point master flags of this button based on a string */
    public void setPmMask(String mask)
    {
        pm_mask = getKinemage().toPmBitmask(mask, true, false);
        // assume that if we have points, at least some of them are on
        // therefore, this master should start out on, too
        ///super.setOn(true);
    }
    
    /**
    * Processes a pointmaster on/off request.
    * @param mask the bitmask indicating which masters are being turned on/off
    * @param turnon <code>true</code> if affected groups are to be turned on,
    *               <code>false</code> if affected groups are to be turned off.
    */
    public void pmHit(int mask, boolean turnon)
    {
        if(parent == null) return;

        // Build a mask of master bits that are off; we're already on
        int offmask = 0;
        for(MasterGroup m : getKinemage().masterList())
            if(!m.isOn()) offmask |= m.pm_mask;
        if(turnon && (mask & ~offmask) == 0) return;
        
        //System.err.println(" turnon = "+turnon);
        //System.err.println("   mask = "+Integer.toBinaryString(mask));
        //System.err.println("offmask = "+Integer.toBinaryString(offmask));

        for(KPoint point : KIterator.allPoints(getKinemage()))
            point.pmHit(mask, offmask, turnon);
    }
//}}}

//{{{ addMaster, removeMaster, getMasters
//##################################################################################################
    /** Not supported */
    public void addMaster(String masterName)
    { throw new UnsupportedOperationException("Not supported by MasterGroup"); }
    
    /** Not supported */
    public void removeMaster(String masterName)
    { throw new UnsupportedOperationException("Not supported by MasterGroup"); }
    
    /** Not supported */
    public Collection<String> getMasters()
    { throw new UnsupportedOperationException("Not supported by MasterGroup"); }
//}}}
}//class
