// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.points.*;

import java.awt.Dimension;
//import java.awt.event.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.Transform;
//}}}
/**
* <code>ParaParams</code> manages switching a kinemage in and out of
* parallel coordinates display of its high dimensional data.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Nov 17 11:35:12 EST 2006
*/
public class ParaParams implements KMessage.Subscriber, Transformable
{
//{{{ Constants
//}}}

//{{{ CLASS: WeakTransformable
//##############################################################################
    static class WeakTransformable implements Transformable
    {
        WeakReference<Transformable> ref;
        ToolBox toolbox;
        
        public WeakTransformable(ToolBox toolbox, Transformable tr)
        {
            this.toolbox = toolbox;
            this.ref = new WeakReference<Transformable>(tr);
        }
        
        public void doTransform(Engine engine, Transform xform)
        {
            Transformable tr = this.ref.get();
            if(tr == null)
                toolbox.transformables.remove(this);
            else
                tr.doTransform(engine, xform);
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain    kMain;
    Kinemage    kin;
    int         numDim;
    double[]    min;
    double[]    max;
    double[]    range;
    KView       normalView;
    KView       parallelView;
    boolean     inParallelMode = false, isUpdating = false;
    KGroup      axisGroup = null;
    
    Map<KList, ArrayList<KPoint>> normalChildren;
    Map<KList, ArrayList<KPoint>> parallelChildren;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ParaParams(KingMain kMain, Kinemage kin)
    {
        super();
        this.kMain  = kMain;
        this.kin    = kin;
        this.numDim = Math.max(2, kin.dimensionNames.size()); // if less, denom -> 0
        this.min    = new double[numDim];
        this.max    = new double[numDim];
        this.range  = new double[numDim];
        List minmax = kin.dimensionMinMax;
        for(int i = 0; i < numDim; i++)
        {
            if(minmax.size() > 2*i)     min[i] = ((Number) minmax.get(2*i)).doubleValue();
            else                        min[i] = 0;
            if(minmax.size() > 2*i + 1) max[i] = ((Number) minmax.get(2*i + 1)).doubleValue();
            else                        max[i] = 360;
            range[i] = max[i] - min[i];
        }
        
        this.normalChildren = new HashMap<KList, ArrayList<KPoint>>();
        this.parallelChildren = new HashMap<KList, ArrayList<KPoint>>();
        this.normalView = new KView(kin); // will never be used
        this.parallelView = new KView(kin);
        parallelView.setCenter(0.5f, 0.5f, 0f);
        parallelView.setSpan(1.2f);
        parallelView.setName("PC Overview");
        kin.addView((KView) parallelView.clone());
        
        kMain.subscribe( new KMessage.WeakSubscriber(kMain, this) );
        ToolBox toolbox = kMain.getCanvas().getToolBox();
        toolbox.transformables.add( new WeakTransformable(toolbox, this) );
    }
//}}}

//{{{ getMin/Max/Range/NumDim
//##############################################################################
    /** Returns the minimum value to be shown on axis i */
    public double getMin(int dimension_i)
    { return this.min[dimension_i]; }
    
    /** Returns the maximum value to be shown on axis i */
    public double getMax(int dimension_i)
    { return this.max[dimension_i]; }
    
    /** Returns max[i] - min[i] */
    public double getRange(int dimension_i)
    { return this.range[dimension_i]; }

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
    }
//}}}

//{{{ isInParallelMode
public boolean isInParallelMode() {
  return inParallelMode;
}
//}}}

//{{{ toParallelCoords
//##############################################################################
    public void toParallelCoords()
    {
        if(inParallelMode) return;
        
      Dimension dim = kMain.getCanvas().getCanvasSize();
      ////kCanvas.paintCanvas(g2, dim, KinCanvas.QUALITY_BEST);
      double width   = dim.getWidth();      
      
        isUpdating = true;
        normalChildren.clear(); // to remove any stale entries
        for(KList l : KIterator.allLists(kin))
        {
            ArrayList<KPoint> newChildren = parallelChildren.get(l);
            if(newChildren == null)
            {
                newChildren = makeParallelPlot(l, width);
                parallelChildren.put(l, newChildren);
            }
            normalChildren.put(l, l.getChildren());
            l.setChildren(newChildren);
        }
        makeParallelAxes();
        //kin.calcSize(); // bounding box, etc. has changed!
        normalView = kMain.getCanvas().getCurrentView(kin);
        parallelView.setViewingAxes(normalView.getViewingAxes());
        kMain.getCanvas().setCurrentView(parallelView);
        inParallelMode = true;
        isUpdating = false;
    }
//}}}

//{{{ fromParallelCoords
//##############################################################################
    public void fromParallelCoords()
    {
        if(!inParallelMode) return;
        isUpdating = true;
        parallelChildren.clear(); // to remove any stale entries
        for(KList l : KIterator.allLists(kin))
        {
            ArrayList<KPoint> newChildren = normalChildren.get(l);
            if(newChildren == null)
            {
                newChildren = new ArrayList<KPoint>();
                normalChildren.put(l, newChildren);
            }
            parallelChildren.put(l, l.getChildren()); // Commenting this out forces a "redraw" every time we enter PC
            l.setChildren(newChildren);
        }
        //kin.calcSize(); // bounding box, etc. has changed!
        parallelView = kMain.getCanvas().getCurrentView(kin);
        normalView.setViewingAxes(parallelView.getViewingAxes());
        kMain.getCanvas().setCurrentView(kin, normalView);
        inParallelMode = false;

        // Carry over picking information
        ToolServices services = kMain.getCanvas().getToolBox().services;
        KPoint picked = services.getLastPicked(0);
        if(picked != null && picked instanceof ParaPoint)
            services.pick( ((ParaPoint) picked).proxyFor );
        
        isUpdating = false;
    }
//}}}

//{{{ makeParallelPlot
//##############################################################################
    ArrayList<KPoint> makeParallelPlot(KList list, double width)
    {
        ArrayList<KPoint> out = new ArrayList<KPoint>();
        for(Iterator iter = list.iterator(); iter.hasNext(); )
        {
            KPoint normalPt = (KPoint) iter.next();
            float[] allCoords = normalPt.getAllCoords();
            if(allCoords == null) continue;
            if(normalPt instanceof MarkerPoint
            || normalPt instanceof VectorPoint
            || normalPt instanceof TrianglePoint) continue;
            ParaPoint ppLast = null;
            for(int i = 0; i < allCoords.length; i++)
            {
                ParaPoint pp = new ParaPoint(normalPt, i, ppLast, this, width);
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
      Dimension dim = kMain.getCanvas().getCanvasSize();
      ////kCanvas.paintCanvas(g2, dim, KinCanvas.QUALITY_BEST);
      double width   = dim.getWidth();
      //System.out.println(width);
      
        boolean newAxes = (axisGroup == null || axisGroup.getKinemage() == null);
        if(newAxes)
        {
            axisGroup = new KGroup("PC axes");
            kin.add(axisGroup);
        }
        else axisGroup.clear();
        
        KGroup subgroup = new KGroup("");
        subgroup.setHasButton(false);
        axisGroup.add(subgroup);
        
        KList axisList = new KList(KList.VECTOR, "axes");
        axisList.setColor(KPalette.white);
        subgroup.add(axisList);

        KList labelList = new KList(KList.LABEL, "labels");
        labelList.setColor(KPalette.white);
        subgroup.add(labelList);
        
        String[] dimNames = kin.dimensionNames.toArray(new String[numDim]);
        DecimalFormat df = new DecimalFormat("0.###");
        for(int i = 0; i < numDim; i++)
        {
          if (width < 900) width = 900;
          double xcoord = (double)i / (double)(numDim-1)+((double)i-(double)numDim/2)/ (double)(numDim-1)*(width-900)/900;
            VectorPoint v1 = new VectorPoint("", null);
            v1.setXYZ(xcoord, 0, 0);
            v1.setUnpickable(true);
            axisList.add(v1);
            VectorPoint v2 = new VectorPoint("", v1);
            v2.setXYZ(xcoord, 1, 0);
            v2.setUnpickable(true);
            axisList.add(v2);
            
            LabelPoint l1 = new LabelPoint(dimNames[i]);
            l1.setXYZ(xcoord, 1.05, 0);
            l1.setUnpickable(true);
            l1.setHorizontalAlignment(LabelPoint.CENTER);
            labelList.add(l1);
            l1 = new LabelPoint(df.format(max[i]));
            l1.setXYZ(xcoord, 1.02, 0);
            l1.setUnpickable(true);
            l1.setHorizontalAlignment(LabelPoint.CENTER);
            labelList.add(l1);
            l1 = new LabelPoint(df.format(min[i]));
            l1.setXYZ(xcoord, -0.03, 0);
            l1.setUnpickable(true);
            l1.setHorizontalAlignment(LabelPoint.CENTER);
            labelList.add(l1);
        }
    }
//}}}

//{{{ deliverMessage, doTransform
//##############################################################################
    public void deliverMessage(KMessage msg)
    {
        if(msg.testProg(KMessage.PRE_KIN_SAVE))
        {
            // Otherwise, we could save the temporary PC representation instead!
            this.fromParallelCoords();
        }
        // Force "redraw" of PC if list contents change,
        // or else we get e.g. duplications of data points.
        if(msg.testKin(AHE.CHANGE_POINT_CONTENTS) && !isUpdating && !inParallelMode)
        {
            parallelChildren.clear();
        }
    }
    
    public void doTransform(Engine engine, Transform xform)
    {
        if(kMain.getKinemage() != this.kin) return; // only active when this kin is
        if(!inParallelMode) return; // only when in PC mode
        
        ToolServices services = kMain.getCanvas().getToolBox().services;
        if(!services.doMarkers.isSelected()) return; // only do this if markers are active
        
        KPoint picked = services.getLastPicked(0);
        if(picked == null) return; // only if something is picked
        if(picked instanceof ParaPoint)
            picked = ((ParaPoint) picked).proxyFor;
        
        KList marker1 = new KList(KList.VECTOR);
        marker1.setWidth(4);
        if(engine.whiteBackground)  marker1.setColor(KPalette.deadblack);
        else                        marker1.setColor(KPalette.deadwhite);
        
        VectorPoint prev = null;
        for(KPoint p : parallelChildren.get(picked.getParent()))
        {
            if(((ParaPoint)p).proxyFor != picked) continue;
            VectorPoint v = new VectorPoint(p.getName(), prev);
            v.setXYZ(p.getX(), p.getY(), p.getZ());
            marker1.add(v);
            prev = v;
        }
        
        marker1.doTransform(engine, xform);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

