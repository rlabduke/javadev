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
import driftwood.data.*;
//}}}
/**
* <code>Kinemage</code> is the top-level container that holds one whole kinemage worth of data.
*
* <p>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 24 21:45:03 EDT 2002
*/
public class Kinemage extends AGE // implements ...
{
//{{{ Variable definitions
//##################################################################################################
    /** Called when stuff changes */
    public KinemageSignal signal = new KinemageSignal();
    
    /**
    * A Map for storing arbitrary information that should be associated with the kinemage.
    * Everyone is expected to play nice and use some sort of unique String as a key.
    */
    public Map metadata         = new HashMap();
    
    // Variables that control the appearance of this kinemage
    // These are updated by the user picking from the Display menu
    public boolean atWhitebackground   = false;
    public boolean atOnewidth          = false;
    public boolean atThinline          = false;
    public boolean atPerspective       = false;
    public boolean atFlat              = false;
    public boolean atListcolordominant = false;
    public double  atLens              = 0.0;

    // Other information fields often contained in a kinemage
    public String  atPdbfile           = null;
    public String  atCommand           = null;
    
    KingView        currView    = null;
    java.util.List  viewList    = new ArrayList();
    
    float[] boundingBox = null;
    float[] center = {0f, 0f, 0f};
    float span = 0f;
    
    // For looking up standard and custom colors
    Map allColorMap;
    Map newColorMap;
    Map unmodAllColorMap = null;
    Map unmodNewColorMap = null;
    
    // For managing aspects
    public Aspect   currAspect  = null;
    java.util.List  aspectList  = new ArrayList();
    
    // For looking up masters by name
    Map mastersMap = new HashMap();
    // For iterating thru the masters in order of creation
    java.util.List mastersList = new ArrayList();
    Component masterSpacer = Box.createRigidArea(new Dimension(0,15));
    
    // For assigning pointmasters to bitfield positions
    public String pmLookup = ""; // no pointmasters defined initially
    
    // Each kinemage has its own TreeModel
    DefaultTreeModel treeModel;
    
    // For tracking whether edits have been saved
    boolean modified = false;

    // For storing bondRots
    Collection bondRots = null;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a new container in the data hierarchy.
    *
    * @param nm the ID of this group
    */
    public Kinemage(String nm)
    {
        children = new ArrayList(10);
        setName(nm);
        treeModel = new DefaultTreeModel(this, true);
        allColorMap = new UberMap(KPalette.getFullMap());
        newColorMap = new UberMap();
    }
//}}}

//{{{ appendKinemage
//##################################################################################################
    /** Merges a kinemage into this one. That kinemage should be discarded afterwards. */
    public void appendKinemage(Kinemage that)
    {
        atWhitebackground   = this.atWhitebackground    || that.atWhitebackground;
        atOnewidth          = this.atOnewidth           || that.atOnewidth;
        atThinline          = this.atThinline           || that.atThinline;
        atPerspective       = this.atPerspective        || that.atPerspective;
        atFlat              = this.atFlat               || that.atFlat;
        atListcolordominant = this.atListcolordominant  || that.atListcolordominant;
        
        if(this.atPdbfile == null) atPdbfile = that.atPdbfile;
        if(this.atCommand == null) atCommand = that.atCommand;
        
        //viewList.addAll(    that.viewList);
        for(Iterator iter = that.viewList.iterator(); iter.hasNext(); )
        {
            KingView view = (KingView) iter.next();
            view.parent = this;
            this.viewList.add(view);
        }
        
        //aspectList.addAll(  that.aspectList);
        for(Iterator iter = that.aspectList.iterator(); iter.hasNext(); )
        {
            Aspect aspect = (Aspect) iter.next();
            aspect.parent = this;
            this.aspectList.add(aspect);
        }

        // Merge the colorsets
        for(Iterator iter = that.getNewPaintMap().values().iterator(); iter.hasNext(); )
            this.addPaint((KPaint)iter.next());
        
        // Merge the masters. This is a little tricky due to pointmasters.
        boolean convertPointmasters = false;
        for(Iterator iter = that.mastersList.iterator(); iter.hasNext(); )
        {
            MasterGroup m2 = (MasterGroup)iter.next();
            // Convert bitmasks from old kin to new kin correspondences
            if(m2.pm_mask != 0)
            {
                m2.pm_mask = this.toPmBitmask(that.fromPmBitmask(m2.pm_mask), true, false);
                convertPointmasters = true;
            }
            
            MasterGroup m1 = (MasterGroup)this.mastersMap.get(m2.getName());
            if(m1 == null)
            {
                m2.setOwner(this);
                this.mastersMap.put(m2.getName(), m2);
                this.mastersList.add(m2);
            }
            else
            {
                // This isn't ideal -- what if the bits in m2 are also set
                // in some other master belonging to this?
                // But it's about the best we can do.
                m1.pm_mask |= m2.pm_mask;
            }
        }
        
        // Finish merging the masters by converting point's pointmaster masks.
        // Each point is visited just once b/c instance= lists have no children of their own.
        if(convertPointmasters)
        {
            RecursivePointIterator iter = new RecursivePointIterator(that);
            while(iter.hasNext())
            {
                KPoint p = iter.next();
                p.setPmMask(this.toPmBitmask(that.fromPmBitmask(p.getPmMask())));
            }
        }
        
        for(Iterator iter = that.iterator(); iter.hasNext(); )
        {
            KGroup group = (KGroup)iter.next();
            group.setOwner(this);
            this.add(group);
        }
        
        this.initAll(); // gets us back to a consistent state
    }
//}}}

//{{{ get/setOwner, getKinemage
//##################################################################################################
    /** Determines the owner (parent) of this element */
    public AGE getOwner()
    { return null; }
    /** Establishes the owner (parent) of this element */
    public void setOwner(AGE owner)
    {}

    /** Retrieves the Kinemage object holding this element */
    public Kinemage getKinemage()
    { return this; }
//}}}

//{{{ is/setOn, (set)hasButton, is/setDominant
//##################################################################################################
    /** Indicates whether this element will paint itself, given the chance */
    public boolean isOn()
    { return true; }
    /** Sets the painting status of this element */
    public void setOn(boolean paint)
    {}
    
    /** Indicates whether this element would display a button, given the chance */
    public boolean hasButton()
    { return false; }
    /** Sets whether this element would display a button, given the chance */
    public void setHasButton(boolean b)
    {}
    
    /** Indicates whether this element supresses buttons of elements below it */
    public boolean isDominant()
    { return false; }
    /** Sets whether this element supresses buttons of elements below it */
    public void setDominant(boolean b)
    {}
//}}}

//{{{ add, replace, initAll, getTreeModel, MutableTreeNode functions
//##################################################################################################
    /** Adds a child to this element */
    public void add(KGroup child)
    { children.add(child); }

    /**
    * Replaces oldChild with newChild, or performs an add()
    * of newChild if oldChild couldn't be found as part of the kinemage.
    * This is very useful for things like Mage's Remote Update.
    * @return the group actually replaced, or null for none
    */
    public KGroup replace(KGroup oldChild, KGroup newChild)
    {
        int idx = children.indexOf(oldChild);
        if(idx == -1)
        {
            add(newChild);
            return null;
        }
        else return (KGroup)children.set(idx, newChild);
    }
    
    /**
    * Convenience function to call after constructing a kinemage from a file
    * and before displaying it. This takes care of the following:
    *<ol>
    *<li>calcSize() -- re-computes size of the kinemage</li>
    *<li>ensureAllMastersExist() -- creates any missing masters</li>
    *<li>syncAllMasters() -- set masters' states appropriately</li>
    *<li>initAllViews() -- converts zooms to spans as necessary</li>
    *<li>animate(0) and animate2(0) -- initializes animations</li>
    *</ol>
    */
    public void initAll()
    {
        removeEmptyAGEs();
        calcSize();
        ensureAllMastersExist();
        removeUnusedMasters();
        syncAllMasters();
        initAllViews();
        animate(0);
        if(!hasAnimateGroups())
            animate2(0); // turns off animate group that was just turned on
    }

    public DefaultTreeModel getTreeModel() { return treeModel; }
    public void insert(MutableTreeNode child, int index)
    {
        if(! (child instanceof KGroup)) throw new IllegalArgumentException("Kinemages can only contain groups!");
        
        if(index < 0 || index > children.size())    children.add(child);
        else                                        children.add(index, child);
    }
    public void removeFromParent()                      {}
//}}}

//{{{ cboxHit, notifyCboxHit, buildButtons
//##################################################################################################
    /** Called when the associated checkbox is turned on/off */
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void cboxHit(ActionEvent ev)
    {}
    
    /** Propagates notice upward that a checkbox was turned on/off */
    public void notifyCboxHit()
    {
        //kMain.notifyChange(kMain.EM_ON_OFF);
        signal.signalKinemage(this, signal.APPEARANCE);
    }

    /** Builds a grouping of Mage-style on/off buttons in the specified container. */
    public Container buildButtons()
    {
        ensureAllMastersExist();
        
        Box buttonBox = Box.createVerticalBox();
        Iterator iter = children.iterator();
        while(iter.hasNext())
        {
            ((KGroup)iter.next()).buildButtons(buttonBox);
        }
        
        buttonBox.add(masterSpacer);
        iter = mastersList.iterator();
        while(iter.hasNext())
        {
            ((MasterGroup)iter.next()).buildButtons(buttonBox);
        }
        
        return buttonBox;
    }
//}}}
    
//{{{ calcSize, getSpan, getCenter
//##################################################################################################
    /**
    * Calculates the bounding box and sphere, and center and span of this kinemage.
    */
    public void calcSize()
    {
        // Produces NaN for the center when kinemage is empty
        //float[] bounds = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        //Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        float[] bounds = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
            -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
        
        calcBoundingBox(bounds);
        boundingBox = bounds;
        
        center[0] = (boundingBox[3] + boundingBox[0])/2f;
        center[1] = (boundingBox[4] + boundingBox[1])/2f;
        center[2] = (boundingBox[5] + boundingBox[2])/2f;
        
        span = 2f * (float)Math.sqrt(calcRadiusSq(center));
        if(span == 0) span = 1; // in case there's only one point in the kin
    }

    /**
    * Calculates the span of this kinemage according to the algorithm used by Mage.
    * @return the diameter of a sphere that encloses the bounding box that encloses this kinemage
    */
    public float getSpan()
    {
        if(boundingBox == null) calcSize();
        return span;
    }

    /**
    * Calculates the center of this kinemage according to the algorithm used by Mage.
    * @return the center of the bounding box that encloses this kinemage
    */
    public float[] getCenter()
    {
        if(boundingBox == null) calcSize();
        return (float[])center.clone();
    }
//}}}

//{{{ getCurrentView, addView, getViewIterator, getViewList, notifyViewSelected, initAllViews
//##################################################################################################
    /** Gets the current view for this kinemage */
    public KingView getCurrentView()
    {
        if(currView == null)
        {
            if(viewList.size() < 1)
            {
                KingView stdView = new KingView(this);
                addView(stdView);
                stdView.getSpan(); // converts zoom to span
                signal.signalKinemage(this, signal.STRUCTURE);
            }
            currView = (KingView)((KingView)viewList.get(0)).clone();
        }
        
        return currView;
    }
    
    /** For use creating new views inside the application. Note: doesn't add v to the viewMap. */
    public void addView(KingView v)
    {
        viewList.add(v);
    }
    
    /** Returns an iterator over all the saved views of this kinemage. */
    public Iterator getViewIterator() { return viewList.iterator(); }
    
    /**
    * Returns a List of all the saved views of this kinemage.
    * This list may be modified in order to remove or reorder
    * the views present in this kinemage.
    */
    public java.util.List getViewList() { return viewList; }
    
    /** Called by a view when the view is picked from a menu */
    public void notifyViewSelected(KingView newview)
    {
        currView = (KingView)newview.clone();
        //kMain.notifyChange(kMain.EM_NEWVIEW);
        signal.signalKinemage(this, signal.APPEARANCE);
    }
    
    /** Calls getSpan() on all views, which converts any values specified as zooms into spans. */
    public void initAllViews()
    {
        KingView v;
        for(Iterator iter = this.getViewIterator(); iter.hasNext(); )
        {
            v = (KingView)iter.next();
            v.getSpan();
        }
    }
//}}}

//{{{ syncAllMasters, ensureMasterExists, ensureAllMastersExist
//##################################################################################################
    /** Sets all masters to reflect the state of the group(s) they control */
    public void syncAllMasters()
    {
        for(Iterator iter = mastersList.iterator(); iter.hasNext(); )
        {
            ((MasterGroup)iter.next()).syncState();
        }
    }
    
    /** Finds a MasterGroup with the given name, creating one if it didn't already exist. */
    public void ensureMasterExists(String name)
    {
        getMasterByName(name);
    }

    /**
    * Builds MasterGroup objects for every master named by a group/subgroup/list.
    * This WILL NOT pick up on any pointmasters that aren't also used by one of the above.
    */
    public void ensureAllMastersExist()
    {
        Iterator grIter, suIter, liIter, maIter;
        KGroup      group;
        KSubgroup   subgroup;
        KList       list;
        
        for(grIter = this.iterator(); grIter.hasNext(); )
        {
            group = (KGroup)grIter.next();
            buildMasters(group);
            for(suIter = group.iterator(); suIter.hasNext(); )
            {
                subgroup = (KSubgroup)suIter.next();
                buildMasters(subgroup);
                for(liIter = subgroup.iterator(); liIter.hasNext(); )
                {
                    list = (KList)liIter.next();
                    buildMasters(list);
                }//lists
            }//subgroups
        }//groups
    }
//}}}

//{{{ buildMasters, getMasterByName, createMaster, masterIter
//##################################################################################################
    /** Iterate over all masters in age */
    private void buildMasters(AGE age)
    {
        MasterGroup     master;
        String          masterName;
        for(Iterator iter = age.masterIterator(); iter != null && iter.hasNext(); )
        {
            masterName = iter.next().toString();
            if(!mastersMap.containsKey(masterName))
            {
                master = createMaster(masterName);
                // We can call this now, assuming the whole file has been read in
                // and all groups to be pasted, have been.
                master.syncState();
                //System.err.println("Created master '"+masterName+"'");
            }
        }
    }
    
    /** Returns a MasterGroup with the given name, creating one if it didn't already exist. */
    public MasterGroup getMasterByName(String name)
    {
        if(mastersMap.containsKey(name)) return (MasterGroup)mastersMap.get(name);
        else return createMaster(name);
    }
    
    MasterGroup createMaster(String name)
    {
        MasterGroup m = new MasterGroup(this, name);
        mastersMap.put(name, m);
        mastersList.add(m);
        return m;
    }
    
    /** Returns an unmodifiable Collection of the masters */
    public Collection masterList()
    {
        return Collections.unmodifiableCollection(mastersList);
    }
//}}}

//{{{ toPmBitmask, fromPmBitmask
//##################################################################################################
    /**
    * Converts a string of characters to a bit mask for pointmasters.
    * The particular mapping of characters to bits is unique to each kinemage,
    * but only up to 32 different characters (case-sensitive) are supported).
    * @param addFlags   if true, assign unrecognized characters to bit positions,
    *   but don't necessarily create masters for them. Defaults to false.
    * @param addMasters if true, for newly-assigned characters also create
    *   a master to control that flag. Defaults to false.
    */
    public int toPmBitmask(String s, boolean addFlags, boolean addMasters)
    {
        int i, end_i, bit, mask = 0;
        end_i = s.length();
        
        for(i = 0; i < end_i; i++)
        {
            bit = pmLookup.indexOf(s.charAt(i));
            if(bit >= 0 && bit < 32) mask |= 1 << bit;
            else if(addFlags && bit == -1)
            {
                pmLookup = pmLookup+s.charAt(i);
                bit = pmLookup.indexOf(s.charAt(i));
                if(bit >= 0 && bit < 32) mask |= 1 << bit;
                if(addMasters)
                {
                    MasterGroup master = getMasterByName(s.substring(i,i+1));
                    master.setPmMask(s.substring(i,i+1));
                }
            }
        }
        
        return mask;
    }
    
    public int toPmBitmask(String s)
    { return toPmBitmask(s, false, false); }
    
    /** Does the inverse of toPmBitmask() */
    public String fromPmBitmask(int mask)
    {
        StringBuffer result = new StringBuffer();
        int probe = 1, end = Math.min(32, pmLookup.length());
        for(int i = 0; i < end; i++)
        {
            if((mask & probe) != 0) result.append(pmLookup.charAt(i));
            probe = probe << 1;
        }
        return result.toString();
    }
//}}}

//{{{ animate, animate2, doAnimation
//##################################################################################################
    /**
    * Drives the animation forward (+1), backward (-1), or to having just one
    * group on (0) for all groups marked 'animate'.
    * All '2animate' groups are turned off.
    */
    public void animate(int incr)
    { doAnimation(getAnimateGroups(), get2AnimateGroups(), incr); }

    /**
    * Drives the animation forward (+1), backward (-1), or to having just one
    * group on (0) for all groups marked '2animate'.
    * All 'animate' groups are turned off.
    */
    public void animate2(int incr)
    { doAnimation(get2AnimateGroups(), getAnimateGroups(), incr); }
    
    void doAnimation(AGE[] ages, AGE[] offages, int incr)
    {
        int firstOn = -1;
        for(int i = 0; i < ages.length; i++)
        {
            if(ages[i].isOn())
            {
                firstOn = i;
                break;
            }
        }
        
        int turnOn = (firstOn == -1 ? 0 : firstOn+incr);
        if(turnOn < 0) turnOn = ages.length-1;
        else if(turnOn >= ages.length) turnOn = 0;
        
        for(int i = 0; i < ages.length; i++)
            ages[i].setOn(i == turnOn);
        
        for(int i = 0; i < offages.length; i++)
            offages[i].setOn(false);
    }
//}}}

//{{{ accumulate, accumulate2, doAccumulate
//##################################################################################################
    /** Turns on the next 'animate' group. '2animate' groups are not affected. */
    public void accumulate()
    { doAccumulate(getAnimateGroups()); }
    
    /** Turns on the next '2animate' group. 'animate' groups are not affected. */
    public void accumulate2()
    { doAccumulate(get2AnimateGroups()); }
    
    /**
    * In order for this to work as expected, we want to turn on the first off group
    * that we encounter AFTER the first on group, in a circular way.
    */
    void doAccumulate(AGE[] ages)
    {
        int firstOn = -1;
        for(int i = 0; i < ages.length; i++)
        {
            if(ages[i].isOn())
            {
                firstOn = i;
                break;
            }
        }
        
        for(int i = firstOn+1; i < firstOn+ages.length; i++)
        {
            if( !ages[i % ages.length].isOn() )
            {
                ages[i % ages.length].setOn(true);
                break;
            }
        }
    }
//}}}

//{{{ has(2)AnimateGroups, get(2)AnimateGroups
//##################################################################################################
    public boolean hasAnimateGroups()
    {
        for(Iterator iter = children.iterator(); iter.hasNext(); )
        {
            KGroup group = (KGroup) iter.next();
            if(group.isAnimate()) return true;
        }
        return false;
    }
    
    public boolean has2AnimateGroups()
    {
        for(Iterator iter = children.iterator(); iter.hasNext(); )
        {
            KGroup group = (KGroup) iter.next();
            if(group.is2Animate()) return true;
        }
        return false;
    }
    
    KGroup[] getAnimateGroups()
    {
        ArrayList animateGroups = new ArrayList();
        for(Iterator iter = children.iterator(); iter.hasNext(); )
        {
            KGroup group = (KGroup) iter.next();
            if(group.isAnimate()) animateGroups.add(group);
        }
        return (KGroup[]) animateGroups.toArray(new KGroup[animateGroups.size()]);
    }
    
    KGroup[] get2AnimateGroups()
    {
        ArrayList animateGroups = new ArrayList();
        for(Iterator iter = children.iterator(); iter.hasNext(); )
        {
            KGroup group = (KGroup) iter.next();
            if(group.is2Animate()) animateGroups.add(group);
        }
        return (KGroup[]) animateGroups.toArray(new KGroup[animateGroups.size()]);
    }
//}}}

//{{{ addPaint, getPaintForName, getAll/NewPaintMap
//##################################################################################################
    /** Registers the given paint as a legal color for objects in this kinemage. */
    public void addPaint(KPaint paint)
    {
        if(paint == null)
            throw new NullPointerException("Can't add a null paint");
        
        allColorMap.put(paint.toString(), paint);
        newColorMap.put(paint.toString(), paint);
    }
    
    /** Returns the KPaint with the given name, or null if unknown. */
    public KPaint getPaintForName(String name)
    {
        KPaint paint = (KPaint)allColorMap.get(name);
        
        // this will catch things like rust, skyblue, paleyellow, grey, etc.
        if(paint == null)
            paint = KPalette.forName(name);
        
        return paint;
    }
    
    /** Returns an unmodifiable Map&lt;String, KPaint&gt; of known colors */
    public Map getAllPaintMap()
    {
        if(unmodAllColorMap == null)
            unmodAllColorMap = Collections.unmodifiableMap(allColorMap);
        return unmodAllColorMap;
    }
    
    /** Returns an unmodifiable Map&lt;String, KPaint&gt; of colors that have been defined just for this kinemage */
    public Map getNewPaintMap()
    {
        if(unmodNewColorMap == null)
            unmodNewColorMap = Collections.unmodifiableMap(newColorMap);
        return unmodNewColorMap;
    }
//}}}

//{{{ Aspect functions
//##################################################################################################
    /** Gets the current aspect for this kinemage (may be null) */
    public Aspect getCurrentAspect()
    { return currAspect; }
    
    public void createAspect(String name, Integer index)
    {
        Aspect a = new Aspect(this, name, index);
        aspectList.add(a);
    }
    
    /** Returns an iterator over all the saved aspects of this kinemage. */
    public ListIterator getAspectIterator() { return aspectList.listIterator(); }
    
    /** Called by an aspect when the aspect is picked from a menu */
    public void notifyAspectSelected(Aspect newAspect)
    {
        currAspect = newAspect;
        //kMain.notifyChange(kMain.EM_DISPLAY);
        signal.signalKinemage(this, signal.APPEARANCE);
    }
//}}}

//{{{ is/setModified
//##################################################################################################
    /**
    * If true, then the kinemage has been modified since it was loaded
    * and the changes have not yet been saved.
    * This property is used for save-on-exit traps.
    */
    public boolean isModified()
    { return modified; }
    
    /** Sets the value of isModified(). */
    public void setModified(boolean b)
    { modified = b; }
//}}}

//{{{ setBondRots, getBondRots
//##################################################################################################
    public void setBondRots(Collection br) {
        bondRots = br;
    }

    public Collection getBondRots() {
        return bondRots;
    }
//}}}

//{{{ removeUnusedMasters
//##################################################################################################
    /** Deletes all masters that don't affect any group/subgroup/list/point */
    public void removeUnusedMasters()
    {
        Iterator grIter, suIter, liIter, ptIter;
        KGroup      group;
        KSubgroup   subgroup;
        KList       list;
        KPoint      point;
        Set         usedNames = new HashSet();
        int         pm_mask = 0;
        
        // First, tally all masters and pointmasters
        for(grIter = this.iterator(); grIter.hasNext(); )
        {
            group = (KGroup)grIter.next();
            if(group.masters != null) usedNames.addAll(group.masters);
            for(suIter = group.iterator(); suIter.hasNext(); )
            {
                subgroup = (KSubgroup)suIter.next();
                if(subgroup.masters != null) usedNames.addAll(subgroup.masters);
                for(liIter = subgroup.iterator(); liIter.hasNext(); )
                {
                    list = (KList)liIter.next();
                    if(list.masters != null) usedNames.addAll(list.masters);
                    if(pm_mask != 0) // only do this if we're a pointmaster
                    {
                        for(ptIter = list.iterator(); ptIter.hasNext(); )
                        {
                            point = (KPoint)ptIter.next();
                            pm_mask |= point.getPmMask();
                        }//points
                    }//if pointmaster
                }//lists
            }//subgroups
        }//groups
        
        // Now, remove masters that aren't used
        for(Iterator iter = mastersMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry e = (Map.Entry) iter.next();
            String name = (String) e.getKey();
            MasterGroup master = (MasterGroup) e.getValue();
            if( !usedNames.contains(name) && (master.pm_mask & pm_mask) == 0)
            {
                iter.remove();
                mastersList.remove(master);
            }
        }
    }
//}}}

//{{{ removeEmptyAGEs
//##################################################################################################
    /** Deletes all groups/subgroups/lists that have no points under them */
    public void removeEmptyAGEs()
    {
        Iterator grIter, suIter, liIter;
        KGroup      group;
        KSubgroup   subgroup;
        KList       list;
        
        for(grIter = this.iterator(); grIter.hasNext(); )
        {
            group = (KGroup)grIter.next();
            for(suIter = group.iterator(); suIter.hasNext(); )
            {
                subgroup = (KSubgroup)suIter.next();
                for(liIter = subgroup.iterator(); liIter.hasNext(); )
                {
                    list = (KList)liIter.next();
                    if(list.children.size() == 0) liIter.remove();
                }//lists
                if(subgroup.children.size() == 0) suIter.remove();
            }//subgroups
            if(group.children.size() == 0) grIter.remove();
        }//groups
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class
