// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import driftwood.gui.IndentBox;
//}}}
/**
* <code>MasterGroup</code> is used to implements masters.
*
* <p>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Jun  2 18:54:11 EDT 2002
*/
public class MasterGroup extends AGE // implements ...
{
//{{{ Static fields
    static final String pmLookup = "abcdefghijklmnopqrstuvwxyz123456";
//}}}

//{{{ Variable definitions
//##################################################################################################
    public int pm_mask = 0; // no point masters to start with
    boolean indent = false;
    Kinemage parent;
//}}}

//{{{ get/setOwner()
//##################################################################################################
    /** Determines the owner (parent) of this element */
    public AGE getOwner()
    { return parent; }
    /** Establishes the owner (parent) of this element */
    public void setOwner(AGE owner)
    {
        parent = (Kinemage)owner;
    }
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
        children = new ArrayList(10);
        setOwner(owner);
        setName(label);
        super.setOn(false);
    }
//}}}

//{{{ cboxHit, setOn, buildButtons
//##################################################################################################
    /** Called when the associated checkbox is turned on/off */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void cboxHit(ActionEvent ev)
    {
        this.setOn(cbox.getModel().isSelected());
        notifyCboxHit();
    }

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
    
    /** Builds a grouping of Mage-style on/off buttons in the specified container. */
    protected void buildButtons(Container cont)
    {
        if(hasButton())
        {
            if(indent)
            {
                IndentBox ibox = new IndentBox(cbox);
                ibox.setIndent(8);
                cont.add(ibox);
            }
            else cont.add(cbox);
        }
    }
//}}}

//{{{ touchMaster, touchAGE
//##################################################################################################
    /**
    * Traverses the kinemage, realizing the effects of turning on/off a single master.
    * @param state          true for on, false for off
    */
    private void touchMaster(boolean state)
    {
        if(parent == null) return;
        
        // Build a list of names of masters that are off
        ArrayList mastersOff = new ArrayList();
        for(Iterator iter = parent.masterList().iterator(); iter.hasNext(); )
        {
            MasterGroup m = (MasterGroup)iter.next();
            if(!m.isOn()) mastersOff.add(m.getName());
        }
        String[]    mOff    = (String[])mastersOff.toArray(new String[mastersOff.size()]);
        String      mName   = this.getName();
        Iterator    grIter, suIter, liIter;
        
        // Traverse the hierarchy applying the master
        for(grIter = parent.iterator(); grIter.hasNext(); )
        {
            KGroup group = (KGroup)grIter.next();
            touchAGE(group, state, mName, mOff);
            for(suIter = group.iterator(); suIter.hasNext(); )
            {
                KSubgroup subgroup = (KSubgroup)suIter.next();
                touchAGE(subgroup, state, mName, mOff);
                for(liIter = subgroup.iterator(); liIter.hasNext(); )
                {
                    KList list = (KList)liIter.next();
                    touchAGE(list, state, mName, mOff);
                }//lists
            }//subgroups
        }//groups
    }
    
    /**
    * Makes a master take affect on a single AGE.
    * If state is false (off), the AGE is turned off.
    * If state is true (on), *and* the AGE isn't controlled by a master that's off, it's turned on.
    * @param    mName the master adopting the state 'state'
    * @param    mOff all other masters that are already off
    */
    private void touchAGE(AGE age, boolean state, String mName, String[] mOff)
    {
        if(age.hasMaster(mName) && age.isOn() != state)
        {
            // master was just turned on and age is off
            if(state)
            {
                for(int i = 0; i < mOff.length; i++)
                    if(age.hasMaster(mOff[i])) return;
                age.setOn(true);
            }
            // master was just turned off and age is on
            else age.setOn(false);
        }
    }
//}}}

//{{{ syncState
//##################################################################################################
    /**
    * Traverses the kinemage, examining the states of its targets.
    * If any target is on, this master should be on.
    * Otherwise, it should be off.
    * Assume that pointmastered points will be on.
    */
    public void syncState()
    {
        if(parent == null) return;
        Iterator grIter, suIter, liIter, ptIter;
        KGroup      group;
        KSubgroup   subgroup;
        KList       list;
        KPoint      point;
        String      masterName = this.getName();
        boolean     state = false;
        
        for(grIter = parent.iterator(); !state && grIter.hasNext(); )
        {
            group = (KGroup)grIter.next();
            if(group.hasMaster(masterName)) state = state || group.isOn();
            for(suIter = group.iterator(); !state && suIter.hasNext(); )
            {
                subgroup = (KSubgroup)suIter.next();
                if(subgroup.hasMaster(masterName)) state = state || subgroup.isOn();
                for(liIter = subgroup.iterator(); !state && liIter.hasNext(); )
                {
                    list = (KList)liIter.next();
                    if(list.hasMaster(masterName)) state = state || list.isOn();
                    if(pm_mask != 0) // only do this if we're a pointmaster
                    {
                        for(ptIter = list.iterator(); !state && ptIter.hasNext(); )
                        {
                            point = (KPoint)ptIter.next();
                            state = state || point.pmWouldHit(pm_mask);
                        }//points
                    }//if pointmaster
                }//lists
            }//subgroups
        }//groups
        
        super.setOn(state);
    }
//}}}

//{{{ setPmMask, pmHit
//##################################################################################################
    /** Sets the point master flags of this button based on a string */
    public void setPmMask(String mask)
    {
        if(parent != null)
            pm_mask = parent.toPmBitmask(mask, true, false);
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
        for(Iterator iter = parent.masterList().iterator(); iter.hasNext(); )
        {
            MasterGroup m = (MasterGroup)iter.next();
            if(!m.isOn()) offmask |= m.pm_mask;
        }
        if(turnon && (mask & ~offmask) == 0) return;
        //System.err.println(" turnon = "+turnon);
        //System.err.println("   mask = "+Integer.toBinaryString(mask));
        //System.err.println("offmask = "+Integer.toBinaryString(offmask));

        Iterator grIter, suIter, liIter, ptIter;
        KGroup      group;
        KSubgroup   subgroup;
        KList       list;
        KPoint      point;
        
        for(grIter = parent.iterator(); grIter.hasNext(); )
        {
            group = (KGroup)grIter.next();
            for(suIter = group.iterator(); suIter.hasNext(); )
            {
                subgroup = (KSubgroup)suIter.next();
                for(liIter = subgroup.iterator(); liIter.hasNext(); )
                {
                    list = (KList)liIter.next();
                    for(ptIter = list.iterator(); ptIter.hasNext(); )
                    {
                        point = (KPoint)ptIter.next();
                        point.pmHit(mask, offmask, turnon);
                    }//points
                }//lists
            }//subgroups
        }//groups
    }//pmHit()
//}}}

//{{{ MutableTreeNode functions
//##################################################################################################
    public void insert(MutableTreeNode child, int index)
    {
        throw new UnsupportedOperationException("Masters can't be manipulated as part of the tree!");
    }
//}}}

//{{{ addMaster, removeMaster, hasMaster, masterIterator
//##################################################################################################
    /** Not supported */
    public void addMaster(String masterName)
    { throw new UnsupportedOperationException("Not supported by MasterGroup"); }
    
    /** Not supported */
    public void removeMaster(String masterName)
    { throw new UnsupportedOperationException("Not supported by MasterGroup"); }
    
    /** Not supported */
    public boolean hasMaster(String masterName)
    { throw new UnsupportedOperationException("Not supported by MasterGroup"); }
    
    /** Not supported */
    public Iterator masterIterator()
    { throw new UnsupportedOperationException("Not supported by MasterGroup"); }
//}}}
}//class
