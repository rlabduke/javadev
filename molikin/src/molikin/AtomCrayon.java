// (jEdit options) :folding=explicit:collapseFolds=1:
package molikin;
import driftwood.moldb2.*;
/**
* <code>AtomCrayon</code> allows for customizing the rendering of kinemage
* point-like objects on a per-AtomState basis.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 11:27:47 EDT 2005
*/
public interface AtomCrayon extends Crayon
{
    /**
    * Customizes the rendering of a kinemage point.
    * @param as     the AtomState being represented at this point
    */
    public void forAtom(AtomState as);
    
}//class

