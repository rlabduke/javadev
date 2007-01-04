// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.util.*;
//}}}
/**
* <code>BondRot</code> is an object to represent a BondRot for doing bond rotations like in Mage.
* 
* <p>Copyright (C) 2004-2007 Vincent B. Chen. All rights reserved.
* <br>Begun in June 2004
*/
public class BondRot implements Iterable<KList>
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    int                 bondRotNum = -1;
    ArrayList<KList>    bondLists = null;
    boolean             isOpen = false;
    String              name = null;
    double              origAng = 0;
    double              currAng = 0;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Constructor
    */
    public BondRot(int rotNum)
    { this(rotNum, null, 0); }
    
    /**
    * Constructor
    */
    public BondRot(int rotNum, String nm, double angle)
    {
        bondLists   = new ArrayList<KList>();
        bondRotNum  = rotNum;
        isOpen      = true;
        name        = nm;
        origAng     = angle;
        currAng     = angle;
    }
//}}}

//{{{ add, is/setOpen, get/setCurrentAngle, getOrigAngle, getName
//##############################################################################
    /**
    * Adds a KList to this bondrot.
    */
    public void add(KList list)
    { bondLists.add(list); }
    
    public boolean isOpen()
    { return isOpen; }

    public void setOpen(boolean status)
    { isOpen = status; }

    public double getCurrentAngle()
    { return currAng; }
    
    public void setCurrentAngle(double ang)
    { currAng = ang; }
    
    public double getOrigAngle()
    { return origAng; }

    public String getName()
    { return name; }
//}}}

//{{{ getAxisList, iterator, toString, equals, hashCode
//##############################################################################
    /**
    * Returns the first list in this BondRot.
    * The first two points of said list determine the axis of rotation.
    */
    public KList getAxisList()
    { return bondLists.get(0); }

    public Iterator<KList> iterator()
    { return bondLists.iterator(); }

    public String toString()
    {
        //return ("BondRot " + bondRotNum + ", Contains: " + bondLists.size() + " lists");
        return name;
    }

    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof BondRot)) return false;
        BondRot rot = (BondRot) obj;
        return (name.equals(rot.getName()));
    }

    public int hashCode()
    { return name.hashCode(); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class
