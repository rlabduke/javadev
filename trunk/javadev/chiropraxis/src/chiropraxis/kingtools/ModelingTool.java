// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
import king.*;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>ModelingTool</code> provides a few small conveniences
* for tools that work with the model manager.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 19 15:27:08 EDT 2003
*/
abstract public class ModelingTool extends BasicTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The model manager */
    protected ModelManager2     modelman;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * @throws RuntimeException if the ModelManager2 plugin cannot be found
    */
    public ModelingTool(ToolBox tb)
    {
        super(tb);
        
        modelman = null;
        Collection plugins = parent.getPluginList();
        for(Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            Plugin plugin = (Plugin)iter.next();
            //if(ModelManager2.class.equals(plugin.getClass()))
            if(plugin instanceof ModelManager2)
                modelman = (ModelManager2)plugin;
        }
        if(modelman == null)
            throw new RuntimeException("Model manager plugin not loaded");
    }
//}}}

//{{{ getResidueNearest
//##############################################################################
    /**
    * Returns the residue with an atom nearest to the given point.
    * The model must be specified so that we can make sure the
    * AtomState we find isn't a "stray"; a state that doesn't match
    * a real residue in the model. This situation can arise when we
    * make mutations, for example.
    * @return null if the residue is not found
    */
    public Residue getResidueNearest(Model m, ModelState s, double x, double y, double z)
    {
        Triple t = new Triple(x, y, z);
        double min2 = Double.MAX_VALUE;
        AtomState closest = null;
        
        s = s.createCollapsed(); // make sure we'll iterate through all points
        for(Iterator iter = s.getLocalStateMap().values().iterator(); iter.hasNext(); )
        {
            AtomState   a   = (AtomState)iter.next();
            double      d   = t.sqDistance(a);
            if(m.contains(a.getResidue()) && d < min2)
            {
                min2    = d;
                closest = a;
            }
        }
        
        if(closest == null) return null;
        else return closest.getResidue();
    }
//}}}

//{{{ getDependencies
//##################################################################################################
    /**
    * All modelling tools are dependent on the ModelManager2 plugin.
    */
    static public Collection getDependencies()
    {
        Collection superDep = BasicTool.getDependencies();
        ArrayList dep = new ArrayList(superDep);
        dep.add(ModelManager2.class.getName());
        return dep;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

