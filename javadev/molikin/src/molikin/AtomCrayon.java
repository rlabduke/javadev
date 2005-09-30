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
public interface AtomCrayon //extends ... implements ...
{
    /**
    * Customizes the rendering of a kinemage point by returning a string
    * that includes color, width/radius, pointmasters, aspects, etc.
    * @param as     the AtomState being represented at this point
    * @return       a valid kinemage string, or "" for nothing.
    *   Null is NOT a valid return value and will cause problems.
    *   Leading/trailing spaces are not necessary.
    */
    public String colorAtom(AtomState as);
    
}//class

