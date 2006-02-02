// (jEdit options) :folding=explicit:collapseFolds=1:
package molikin;
import driftwood.moldb2.*;
/**
* <code>RibbonCrayon</code> allows for customizing the rendering of kinemage
* ribbon-like objects.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 11:27:47 EDT 2005
*/
public interface RibbonCrayon //extends ... implements ...
{
    /**
    * Customizes the rendering of a kinemage point by returning a string
    * that includes color, width/radius, pointmasters, aspects, etc.
    * @param start      the GuidePoint at the start of this span
    * @param end        the GuidePoint at the end of this span
    * @param interval   a number from 0 (start) to nIntervals (end), inclusive
    * @param nIntervals the number of pieces this section of ribbon is broken into
    * @return           a valid kinemage string, or "" for nothing.
    *   Null is NOT a valid return value and will cause problems.
    *   Leading/trailing spaces are not necessary.
    */
    public String colorRibbon(GuidePoint start, GuidePoint end, int interval, int nIntervals);
    
}//class

