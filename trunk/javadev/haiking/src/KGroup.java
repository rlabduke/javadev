// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;

import java.util.*;
import javax.microedition.lcdui.*;
//}}}
/**
* <code>KGroup</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Feb 23 10:48:22 EST 2005
*/
public class KGroup //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The first point that should be affected by this group */
    public KPoint startPoint = null;
    /**
    * The first point that should NOT be affected by this group,
    * or null for end-of-chain.
    * Should be reachable by following startPoint.prev.
    */
    public KPoint stopPoint = null;
    
    String name;
    boolean isOn = true;
    // The "depth" of this grouping: 1 for groups, 2 for subgroups, 3 for lists.
    int depth;
    int flags;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KGroup(String name, int depth, int flags)
    {
        super();
        this.depth = depth;
        this.flags = flags;
        // Indent name using spaces
        for(int i = 1; i < depth; i++) name = "  "+name;
        this.name = name;
    }
//}}}

//{{{ processGroups
//##############################################################################
    /**
    * Given a list of various "groups" (kinemage groups, subgroups, and/or lists),
    * this will ensure that the on/off states of the list of points match.
    */
    static public void processGroups(Vector groupList, KPoint tailPt)
    {
        // First, we turn all the points on.
        while(tailPt != null)
        {
            tailPt.setOn(true);
            tailPt = tailPt.prev;
        }
        
        // Then we turn some off. This produces the "dominant off"
        // effect of hierarchical groups/subgroups/lists.
        for(int i = 0; i < groupList.size(); i++)
        {
            KGroup group = (KGroup) groupList.elementAt(i);
            if(group.isOn) continue;
            
            tailPt = group.startPoint;
            // We shouldn't ever need the null test, but better to be safe...
            while(tailPt != null && tailPt != group.stopPoint)
            {
                tailPt.setOn(false);
                tailPt = tailPt.prev;
            }
        }
    }
//}}}

//{{{ to/fromChoice
//##############################################################################
    /** Makes a given Choice reflect the state of the set of groups. */
    static public void toChoice(Vector groupList, Choice choice)
    {
        for(int i = 0; i < groupList.size(); i++)
        {
            KGroup group = (KGroup) groupList.elementAt(i);
            choice.setSelectedIndex(i, group.isOn);
        }
    }

    /** Makes the state of the set of groups reflect the given Choice. */
    static public void fromChoice(Vector groupList, Choice choice)
    {
        for(int i = 0; i < groupList.size(); i++)
        {
            KGroup group = (KGroup) groupList.elementAt(i);
            group.isOn = choice.isSelected(i);
        }
    }
//}}}

//{{{ makeList
//##############################################################################
    /** Creates a MULTIPLE selection List to match a set of groups. */
    static public List makeList(Vector groupList, String title)
    {
        String[] names = new String[groupList.size()];
        for(int i = 0; i < groupList.size(); i++)
        {
            KGroup group = (KGroup) groupList.elementAt(i);
            names[i] = group.name;
        }
        
        List list = new List(title, List.MULTIPLE, names, null);
        KGroup.toChoice(groupList, list);
        return list;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

