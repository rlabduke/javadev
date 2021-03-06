// (jEdit options) :folding=explicit:collapseFolds=1:
package molikin;
import driftwood.moldb2.*;
import driftwood.r3.*;
/**
* <code>RibbonCrayon</code> allows for customizing the rendering of kinemage
* ribbon-like objects.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 11:27:47 EDT 2005
*/
public interface RibbonCrayon extends Crayon
{
    /**
    * Customizes the rendering of a kinemage point.
    * @param point      the location of the current point on the spline
    * @param start      the GuidePoint at the start of this span
    * @param end        the GuidePoint at the end of this span
    * @param interval   a number from 0 (start) to nIntervals (end), inclusive
    * @param nIntervals the number of pieces this section of ribbon is broken into
    */
    public void forRibbon(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals);
    
}//class

