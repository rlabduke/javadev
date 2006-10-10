// (jEdit options) :folding=explicit:collapseFolds=1:
package molikin;
import driftwood.moldb2.*;
/**
* <code>BondCrayon</code> allows for customizing the rendering of kinemage
* line-like objects on a per-AtomState basis.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 11:27:47 EDT 2005
*/
public interface BondCrayon extends Crayon
{
    /**
    * Customizes the rendering of a kinemage point.
    * Notice that "from" and "toward" are interchangable for drawing normal bonds,
    * but for half-bonds the decision is usually made based on "from" only.
    * @param from   the AtomState being represented at one end of the bond
    * @param toward the AtomState at the other end, or in the case of half-bonds,
    *   the AtomState on the other side of the midpoint
    */
    public void forBond(AtomState from, AtomState toward);
    
}//class

