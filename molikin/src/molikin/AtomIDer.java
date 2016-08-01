// (jEdit options) :folding=explicit:collapseFolds=1:
package molikin;
import driftwood.moldb2.AtomState;
/**
* <code>AtomIDer</code> generates kinemage point IDs for atoms.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Nov  8 15:18:42 EST 2005
*/
public interface AtomIDer //extends ... implements ...
{
    /**
    * Returns a string to be used as the kinemage point ID
    * for the supplied AtomState.
    * @param as     the state to be described
    * @return       a string suitable for a point ID
    */
    public String identifyAtom(AtomState as);
    
}//class

