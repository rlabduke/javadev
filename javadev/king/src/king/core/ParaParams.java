// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>ParaParams</code> manages switching a kinemage in and out of
* parallel coordinates display of its high dimensional data.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Nov 17 11:35:12 EST 2006
*/
public class ParaParams //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Kinemage    kin;
    int         numDim;
    double[]    min;
    double[]    max;
    Map         normalChildren;
    Map         parallelChildren;
    KingView    normalView;
    KingView    parallelView;
    boolean     inParallelMode = false;
    KGroup      axisGroup = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ParaParams(Kinemage kin)
    {
        super();
        this.kin = kin;
        this.numDim = kin.dimensionNames.size();
        this.min = new double[numDim];
        this.max = new double[numDim];
        for(int i = 0; i < numDim; i++)
        {
            min[i] = 0;
            max[i] = 360;
        }
        
        this.normalChildren = new HashMap();
        this.parallelChildren = new HashMap();
        this.normalView = new KingView(kin); // will never be used
        this.parallelView = new KingView(kin);
        parallelView.setCenter(0.5f, 0.5f, 0f);
        parallelView.setSpan(1.2f);
        parallelView.setName("PC Overview");
        kin.addView((KingView) parallelView.clone());
        kin.signal.signalKinemage(kin, KinemageSignal.STRUCTURE);
    }
//}}}

//{{{ getMin/Max/NumDim
//##############################################################################
    /** Returns the minimum value to be shown on axis i */
    public double getMin(int dimension_i)
    { return this.min[dimension_i]; }
    
    /** Returns the maximum value to be shown on axis i */
    public double getMax(int dimension_i)
    { return this.max[dimension_i]; }
    
    /** Returns the total number of dimension axes to be displayed */
    public int getNumDim()
    { return this.numDim; }
//}}}

//{{{ swap
//##############################################################################
    /** Toggles between "normal" and parallel coordinates modes. */
    public void swap()
    {
        if(inParallelMode)  fromParallelCoords();
        else                toParallelCoords();
        inParallelMode = !inParallelMode;
    }
//}}}

//{{{ toParallelCoords
//##############################################################################
    void toParallelCoords()
    {
        normalChildren.clear(); // to remove any stale entries
        for(Iterator ki = kin.iterator(); ki.hasNext(); )
        {
            KGroup g = (KGroup) ki.next();
            for(Iterator gi = g.iterator(); gi.hasNext(); )
            {
                KSubgroup s = (KSubgroup) gi.next();
                for(Iterator si = s.iterator(); si.hasNext(); )
                {
                    KList l = (KList) si.next();
                    List newChildren = (List) parallelChildren.get(l);
                    if(newChildren == null)
                    {
                        newChildren = makeParallelPlot(l);
                        parallelChildren.put(l, newChildren);
                    }
                    normalChildren.put(l, l.children);
                    l.children = newChildren;
                }
            }
        }
        makeParallelAxes();
        kin.calcSize(); // bounding box, etc. has changed!
        normalView = kin.getCurrentView();
        parallelView.setViewingAxes(normalView.getViewingAxes());
        parallelView.selectedFromMenu(null);
    }
//}}}

//{{{ fromParallelCoords
//##############################################################################
    void fromParallelCoords()
    {
        parallelChildren.clear(); // to remove any stale entries
        for(Iterator ki = kin.iterator(); ki.hasNext(); )
        {
            KGroup g = (KGroup) ki.next();
            for(Iterator gi = g.iterator(); gi.hasNext(); )
            {
                KSubgroup s = (KSubgroup) gi.next();
                for(Iterator si = s.iterator(); si.hasNext(); )
                {
                    KList l = (KList) si.next();
                    List newChildren = (List) normalChildren.get(l);
                    if(newChildren == null)
                    {
                        newChildren = new ArrayList();
                        normalChildren.put(l, newChildren);
                    }
                    parallelChildren.put(l, l.children);
                    l.children = newChildren;
                }
            }
        }
        kin.calcSize(); // bounding box, etc. has changed!
        parallelView = kin.getCurrentView();
        normalView.setViewingAxes(parallelView.getViewingAxes());
        normalView.selectedFromMenu(null);
    }
//}}}

//{{{ makeParallelPlot
//##############################################################################
    List makeParallelPlot(KList list)
    {
        ArrayList out = new ArrayList();
        for(Iterator iter = list.iterator(); iter.hasNext(); )
        {
            KPoint normalPt = (KPoint) iter.next();
            float[] allCoords = normalPt.getAllCoords();
            if(allCoords == null) continue;
            ParaPoint ppLast = null;
            for(int i = 0; i < allCoords.length; i++)
            {
                ParaPoint pp = new ParaPoint(normalPt, i, ppLast, this);
                out.add(pp);
                ppLast = pp;
            }
        }
        return out;
    }
//}}}

//{{{ makeParallelAxes
//##############################################################################
    void makeParallelAxes()
    {
        boolean newAxes = (axisGroup == null);
        if(newAxes)
        {
            axisGroup = new KGroup(kin, "PC axes");
            kin.add(axisGroup);
        }
        else axisGroup.children.clear();
        
        KSubgroup subgroup = new KSubgroup(axisGroup, "");
        subgroup.setHasButton(false);
        axisGroup.add(subgroup);
        
        KList axisList = new KList(subgroup, "axes");
        axisList.setType(KList.VECTOR);
        axisList.setColor(KPalette.white);
        subgroup.add(axisList);

        KList labelList = new KList(subgroup, "labels");
        labelList.setType(KList.LABEL);
        labelList.setColor(KPalette.white);
        subgroup.add(labelList);
        
        String[] dimNames = (String[]) kin.dimensionNames.toArray(new String[numDim]);
        DecimalFormat df = new DecimalFormat("0.###");
        for(int i = 0; i < numDim; i++)
        {
            VectorPoint v1 = new VectorPoint(axisList, "", null);
            v1.setXYZ((double)i / (double)(numDim-1), 0, 0);
            v1.setUnpickable(true);
            axisList.add(v1);
            VectorPoint v2 = new VectorPoint(axisList, "", v1);
            v2.setXYZ((double)i / (double)(numDim-1), 1, 0);
            v2.setUnpickable(true);
            axisList.add(v2);
            
            LabelPoint l1 = new LabelPoint(labelList, dimNames[i]);
            l1.setXYZ((double)i / (double)(numDim-1), 1.05, 0);
            l1.setUnpickable(true);
            labelList.add(l1);
            l1 = new LabelPoint(labelList, df.format(max[i]));
            l1.setXYZ((double)i / (double)(numDim-1), 1.02, 0);
            l1.setUnpickable(true);
            labelList.add(l1);
            l1 = new LabelPoint(labelList, df.format(min[i]));
            l1.setXYZ((double)i / (double)(numDim-1), -0.03, 0);
            l1.setUnpickable(true);
            labelList.add(l1);
        }
        
        kin.signal.signalKinemage(kin, KinemageSignal.STRUCTURE | KinemageSignal.APPEARANCE);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

