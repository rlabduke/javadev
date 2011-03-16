// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.io.*;
//import java.text.*;
import java.util.*;
import driftwood.data.*;
import driftwood.gui.*;
//}}}
/**
* <code>Kinemage</code> is the top-level container that holds one whole kinemage worth of data.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May 24 21:45:03 EDT 2002
*/
public class Kinemage extends AGE<Kinemage,KGroup> // implements ...
{
//{{{ Variable definitions
//##################################################################################################
    /**
    * A Map for storing arbitrary information that should be associated with the kinemage.
    * Everyone is expected to play nice and use some sort of unique String as a key.
    */
    public Map<String,Object> metadata = new HashMap<String,Object>();
    
    // Variables that control the appearance of this kinemage
    // These are updated by the user picking from the Display menu
    public boolean atWhitebackground    = false;
    public boolean atOnewidth           = false;
    public boolean atThinline           = false;
    public boolean atPerspective        = false;
    public boolean atFlat               = false;
    public boolean atListcolordominant  = false;
    public boolean atSidedcoloringAlpha = true;  // (ARK Spring2010)
    public boolean atSidedcoloringBeta  = false; // (ARK Spring2010)
    public double  atLens               = 0.0;

    // Other information fields often contained in a kinemage
    public String  atPdbfile           = null;
    public String  atCommand           = null;
    
    /** Labels for high-D kins */
    public Collection<String> dimensionNames = new ArrayList<String>();
    /** min,max,min,max,... for high-D kins */
    public List<Number> dimensionMinMax = new ArrayList<Number>();
    /** Scaling factors used to adjust output of "Show XYZ" */
    public List<Number> dimensionScale = new ArrayList<Number>();
    /** Translation factors used to adjust output of "Show XYZ" */
    public List<Number> dimensionOffset = new ArrayList<Number>();
    
    protected List<KView>   viewList    = new ArrayList<KView>();
    protected float[]       boundingBox = null;
    protected float[]       center      = {0f, 0f, 0f};
    protected float         span        = 0f;
    
    /** For looking up standard and custom colors */
    protected Map<String,KPaint> allColorMap;
    protected Map<String,KPaint> newColorMap;
    protected Map<String,KPaint> unmodAllColorMap = null;
    protected Map<String,KPaint> unmodNewColorMap = null;
    
    /** For managing aspects */
    protected List<Aspect> aspectList = new ArrayList<Aspect>();
    
    /** For looking up masters by name AND iterating thru the masters in order of creation */
    protected Map<String,MasterGroup> mastersMap = (Map<String,MasterGroup>) new UberMap();
    
    /** For assigning pointmasters to bitfield positions */
    public String pmLookup = ""; // no pointmasters defined initially
    
    /** For tracking whether edits have been saved */
    protected boolean modified = false;

    /** For storing BondRots */
    protected Collection<BondRot> bondRots = null;
    
    /** Flags set since the last time someone queried changes. */
    protected int outerEventFlags = 0;
    /** Flags set that dirtied internal data which hasn't been cleaned yet. */
    protected boolean dirtyMasters = false, dirtySize = false;
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
        super();
        setName(nm);
        allColorMap = (Map<String,KPaint>) new UberMap(KPalette.getFullMap());
        newColorMap = (Map<String,KPaint>) new UberMap();
    }

    public Kinemage()
    { this(""); }
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
        
        for(KView view : that.viewList)
        {
            view.parent = this;
            this.viewList.add(view);
        }
        
        for(Aspect aspect : that.aspectList)
        {
            aspect.parent = this;
            this.aspectList.add(aspect);
        }

        // Merge the colorsets
        for(KPaint paint : that.getNewPaintMap().values())
            this.addPaint(paint);
        
        // Merge the masters. This is a little tricky due to pointmasters.
        boolean convertPointmasters = false;
        for(MasterGroup m2 :  that.masterList())
        {
            // Convert bitmasks from old kin to new kin correspondences
            if(m2.pm_mask != 0)
            {
                m2.pm_mask = this.toPmBitmask(that.fromPmBitmask(m2.pm_mask), true, false);
                convertPointmasters = true;
            }
            
            MasterGroup m1 = this.mastersMap.get(m2.getName());
            if(m1 == null)
            {
                m2.setParent(this);
                this.mastersMap.put(m2.getName(), m2);
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
        if(convertPointmasters) for(KPoint p : KIterator.allPoints(that))
            p.setPmMask(this.toPmBitmask(that.fromPmBitmask(p.getPmMask())));
        
        for(KGroup group : that)
        {
            this.add(group);
            group.setParent(this);
        }
        
        fireKinChanged(CHANGE_EVERYTHING);
        
        // Pass false to avoid removing unused groups, masters in kinemage "this".
        // Otherwise, (for instance) we may delete the mobile sidechain used by Chiropraxis tools.
        this.initAll(false); // gets us back to a consistent state
    }
//}}}

//{{{ get/setParent, getKinemage, is/setOn, (set)hasButton, is/setDominant
//##################################################################################################
    public Kinemage getParent()
    { return null; }
    
    public void setParent(Kinemage owner)
    { /* NO-OP */ }

    /** Retrieves the Kinemage object holding this element */
    public Kinemage getKinemage()
    { return this; }

    public boolean isOn()
    { return true; }
    
    public void setOn(boolean paint)
    { /* NO-OP */ }
    
    public boolean hasButton()
    { return false; }

    public void setHasButton(boolean b)
    { /* NO-OP */ }
    
    public boolean isDominant()
    { return false; }

    public void setDominant(boolean b)
    { /* NO-OP */ }
//}}}

//{{{ fire/queryKinChanged
//##################################################################################################
    protected static final int DIRTY_MASTERS =
        CHANGE_TREE_CONTENTS | CHANGE_TREE_MASTERS | CHANGE_POINT_CONTENTS | CHANGE_POINT_MASTERS | CHANGE_MASTERS_LIST;
    protected static final int DIRTY_SIZE =
        CHANGE_TREE_CONTENTS | CHANGE_POINT_CONTENTS | CHANGE_POINT_COORDINATES;

    public void fireKinChanged(int eventFlags)
    {
        this.outerEventFlags |= eventFlags;
        
        if((eventFlags & DIRTY_MASTERS) != 0)
            dirtyMasters = true;
        if((eventFlags & DIRTY_SIZE) != 0)
            dirtySize = true;
    }
    
    /** Returns all changes and clears all flags. */
    public int queryKinChanged()
    { return queryKinChanged(CHANGE_EVERYTHING, CHANGE_EVERYTHING); }
    
    /** Returns only the changes corresponding to set bits in the read mask, and clears those set in the clear mask. */
    public int queryKinChanged(int read, int clear)
    {
        int flags = outerEventFlags & read;
        outerEventFlags &= ~clear;
        return flags;
    }
//}}}

//{{{ initAll
//##################################################################################################
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
    * @param cleanEmpties   if true (the default), remove groups/subgroups/lists
    *   that contain no points and remove unused masters.
    */
    public void initAll(boolean cleanEmpties)
    {
        if(cleanEmpties) removeEmptyAGEs();
        calcSize();
        ensureAllMastersExist();
        if(cleanEmpties) removeUnusedMasters();
        syncAllMasters();
        initAllViews();
        animate(0);
        if(!hasAnimateGroups())
            animate2(0); // turns off animate group that was just turned on
    }
    
    public void initAll()
    { initAll(true); }
//}}}
    
//{{{ calcSize, getSpan, getCenter
//##################################################################################################
    /**
    * Calculates the bounding box and sphere, and center and span of this kinemage.
    */
    protected void calcSize()
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
        
        dirtySize = false;
    }

    /**
    * Calculates the span of this kinemage according to the algorithm used by Mage.
    * @return the diameter of a sphere that encloses the bounding box that encloses this kinemage
    */
    public float getSpan()
    {
        if(boundingBox == null || dirtySize) calcSize();
        return span;
    }

    /**
    * Calculates the center of this kinemage according to the algorithm used by Mage.
    * @return the center of the bounding box that encloses this kinemage
    */
    public float[] getCenter()
    {
        if(boundingBox == null || dirtySize) calcSize();
        return (float[])center.clone();
    }
//}}}

//{{{ addView, getViewList, initAllViews
//##################################################################################################
    /** For use creating new views inside the application. */
    public void addView(KView v)
    {
        viewList.add(v);
        fireKinChanged(CHANGE_VIEWS_LIST);
    }
    
    /**
    * Returns a List of all the saved views of this kinemage.
    * This list may be modified in order to remove or reorder
    * the views present in this kinemage.
    */
    public List<KView> getViewList() { return viewList; }
    
    /**
    * Calls getSpan() on all views, which converts any values specified as zooms into spans.
    * Also creates a default view if none exists.
    */
    public void initAllViews()
    {
        if(viewList.size() < 1)
        {
            KView stdView = new KView(this);
            this.addView(stdView);
        }
        for(KView v : viewList)
            v.getSpan();
    }
//}}}

//{{{ syncAllMasters, ensureAllMastersExist
//##################################################################################################
    /** Sets all masters to reflect the state of the group(s) they control */
    public void syncAllMasters()
    {
        for(MasterGroup master : mastersMap.values()) master.syncState();
    }
    
    /**
    * Builds MasterGroup objects for every master named by a group/subgroup/list.
    * This WILL NOT pick up on any pointmasters that aren't also used by one of the above.
    */
    public void ensureAllMastersExist()
    {
        for(AGE age : KIterator.allNonPoints(this))
            // I'm not sure why the cast is required ... compiler bug?
            for(String masterName : (Collection<String>) age.getMasters())
                if(!mastersMap.containsKey(masterName))
                {
                    MasterGroup master = getMasterByName(masterName);
                    // We can call this now, assuming the whole file has been read in
                    // and all groups to be pasted, have been.
                    master.syncState();
                }
        dirtyMasters = false;
    }
//}}}

//{{{ getMasterByName, ensureMasterExists, masterList
//##################################################################################################
    /** Returns a MasterGroup with the given name, creating one if it didn't already exist. */
    public MasterGroup getMasterByName(String name)
    {
        MasterGroup master = mastersMap.get(name);
        if(master == null)
        {
            master = new MasterGroup(this, name);
            mastersMap.put(name, master);
            fireKinChanged(CHANGE_MASTERS_LIST);
        }
        return master;
    }
    
    /** Finds a MasterGroup with the given name, creating one if it didn't already exist. */
    public void ensureMasterExists(String name)
    { getMasterByName(name); }
    
    /** Returns an unmodifiable Collection of the masters */
    public Collection<MasterGroup> masterList()
    {
        if(dirtyMasters) ensureAllMastersExist();
        return Collections.unmodifiableCollection(mastersMap.values());
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
    
    protected void doAnimation(AGE[] ages, AGE[] offages, int incr)
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
        
        for(int i = 0; i < ages.length; i++) {
            ages[i].setOn(i == turnOn);
            // for doing moviews
            if ((i == turnOn)&&(((KGroup)ages[i]).isMoview())) {
              KView view = getViewList().get(((KGroup)ages[i]).getMoview() - 1);
              String currViewKey = null;
              for (Map.Entry e : metadata.entrySet()) {
                if (e.getValue() instanceof KView) {
                  currViewKey = (String)e.getKey();
                }
              }
              if ((currViewKey != null)&&(view != null)) {
                metadata.put(currViewKey, view.clone());
                view.activateViewingAxes();
                fireKinChanged(CHANGE_KIN_METADATA);
                fireKinChanged(CHANGE_VIEW_TRANSFORM);
              }
            }
        }
        
        for(int i = 0; i < offages.length; i++)
            offages[i].setOn(false);
    }
//}}}

//{{{ [accumulate, accumulate2, doAccumulate BEFORE 090930 FOR POSTERITY]
//##################################################################################################
//    /** Turns on the next 'animate' group. '2animate' groups are not affected. */
//    public void accumulate()
//    { doAccumulate(getAnimateGroups()); }
//    
//    /** Turns on the next '2animate' group. 'animate' groups are not affected. */
//    public void accumulate2()
//    { doAccumulate(get2AnimateGroups()); }
//    
//    /**
//    * In order for this to work as expected, we want to turn on the first off group
//    * that we encounter AFTER the first on group, in a circular way.
//    */
//    protected void doAccumulate(AGE[] ages)
//    {
//        int firstOn = -1;
//        for(int i = 0; i < ages.length; i++)
//        {
//            if(ages[i].isOn())
//            {
//                firstOn = i;
//                break;
//            }
//        }
//        
//        for(int i = firstOn+1; i < firstOn+ages.length; i++)
//        {
//            if( !ages[i % ages.length].isOn() )
//            {
//                ages[i % ages.length].setOn(true);
//                break;
//            }
//        }
//    }
//}}}

//{{{ accumulate, accumulate2, doAccumulate
//##################################################################################################
    /**
    * Turns on the next (+1) or turns off the most recent (-1) 'animate' group. 
    * '2animate' groups are not affected.
    */
    public void accumulate(int incr)
    { doAccumulate(getAnimateGroups(), incr); }
    
    /**
    * Turns on the next (+1) or turns off the most recent (-1) '2animate' group. 
    * 'animate' groups are not affected.
    */
    public void accumulate2(int incr)
    { doAccumulate(get2AnimateGroups(), incr); }
    
    /**
    * In order for this to work as expected, we want to turn on the first off 
    * group that we encounter AFTER the first on group (+1), in a circular way, 
    * or turn off the LAST on group (-1).
    */
    protected void doAccumulate(AGE[] ages, int incr)
    {
        if(incr == -1)
        {
            // "Decumulate"
            int lastOn = -1;
            for(int i = 0; i < ages.length; i++)
            {
                if(ages[i].isOn())
                {
                    lastOn = i;
                }
            }
            if(lastOn == -1) return;
            ages[lastOn].setOn(false);
        }
        else
        {
            // Accumulate
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
    }
//}}}

//{{{ has(2)AnimateGroups, get(2)AnimateGroups
//##################################################################################################
    public boolean hasAnimateGroups()
    {
        for(KGroup group : children)
            if(group.isAnimate()) return true;
        return false;
    }
    
    public boolean has2AnimateGroups()
    {
        for(KGroup group : children)
            if(group.is2Animate()) return true;
        return false;
    }
    
    protected KGroup[] getAnimateGroups()
    {
        ArrayList<KGroup> animateGroups = new ArrayList<KGroup>();
        for(KGroup group : children)
            if(group.isAnimate()) animateGroups.add(group);
        return animateGroups.toArray(new KGroup[animateGroups.size()]);
    }
    
    protected KGroup[] get2AnimateGroups()
    {
        ArrayList<KGroup> animateGroups = new ArrayList<KGroup>();
        for(KGroup group : children)
            if(group.is2Animate()) animateGroups.add(group);
        return animateGroups.toArray(new KGroup[animateGroups.size()]);
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
        
        fireKinChanged(CHANGE_KIN_METADATA);
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
    public Map<String,KPaint> getAllPaintMap()
    {
        if(unmodAllColorMap == null)
            unmodAllColorMap = Collections.unmodifiableMap(allColorMap);
        return unmodAllColorMap;
    }
    
    /** Returns an unmodifiable Map&lt;String, KPaint&gt; of colors that have been defined just for this kinemage */
    public Map<String,KPaint> getNewPaintMap()
    {
        if(unmodNewColorMap == null)
            unmodNewColorMap = Collections.unmodifiableMap(newColorMap);
        return unmodNewColorMap;
    }
//}}}

//{{{ createAspect, getAspects
//##################################################################################################
    public void createAspect(String name, Integer index)
    {
        Aspect a = new Aspect(this, name, index);
        aspectList.add(a);
        fireKinChanged(CHANGE_ASPECTS_LIST);
    }
    
    /** Returns an unmodifiable view of all the saved aspects of this kinemage. */
    public List<Aspect> getAspects() { return Collections.unmodifiableList(aspectList); }
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

//{{{ get/setBondRots
//##################################################################################################
    public void setBondRots(Collection<BondRot> br)
    { bondRots = br; }

    public Collection<BondRot> getBondRots()
    { return bondRots; }
//}}}

//{{{ getFileFilters
public static SuffixFileFilter getKinFileFilter() {
  SuffixFileFilter kinFilter = new SuffixFileFilter("Kinemage files");
  kinFilter.addSuffix(".kin");
  kinFilter.addSuffix(".kip");
  kinFilter.addSuffix(".kin.gz");
  kinFilter.addSuffix(".kip.gz");
  return kinFilter;
}
//}}}

//{{{ removeUnusedMasters
//##################################################################################################
    /** Deletes all masters that don't affect any group/subgroup/list/point */
    public void removeUnusedMasters()
    {
        Set<String> usedNames   = new HashSet<String>();
        int         pm_mask     = 0;
        
        // First, tally all masters and pointmasters
        for(AGE age : KIterator.allNonPoints(this))
            usedNames.addAll(age.getMasters());
        for(KList list : KIterator.allLists(this))
            for(KPoint point : list.getChildren())
                pm_mask |= point.getPmMask();
        
        // Now, remove masters that aren't used
        for(Iterator<Map.Entry<String,MasterGroup>> iter = mastersMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry<String,MasterGroup> e = iter.next();
            String name = e.getKey();
            MasterGroup master = e.getValue();
            if(!usedNames.contains(name) && (master.pm_mask & pm_mask) == 0)
            {
                iter.remove();
                fireKinChanged(CHANGE_MASTERS_LIST);
            }
        }
    }
//}}}

//{{{ removeEmptyAGEs
//##################################################################################################
    /** Deletes all groups/subgroups/lists that have no points under them */
    public void removeEmptyAGEs()
    {
        removeEmptyAGEs(this);
    }
    
    protected void removeEmptyAGEs(AGE parent)
    {
        for(Iterator<AHE> iter = parent.getChildren().iterator(); iter.hasNext(); )
        {
            AHE ahe = iter.next();
            if(ahe instanceof KList)
            {
                KList list = (KList) ahe;
                if(list.getChildren().isEmpty() && list.getInstance() == null)
                {
                    iter.remove();
                    if(list.getParent() == parent)
                        list.setParent(null);
                    fireKinChanged(CHANGE_TREE_CONTENTS);
                }
            }
            else if(ahe instanceof AGE) // but not a list
            {
                AGE age = (AGE) ahe;
                removeEmptyAGEs(age);
                if(age.getChildren().isEmpty())
                {
                    iter.remove();
                    if(age.getParent() == parent)
                        age.setParent(null);
                    fireKinChanged(CHANGE_TREE_CONTENTS);
                }
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class
