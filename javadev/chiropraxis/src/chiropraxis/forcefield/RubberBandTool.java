// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.forcefield;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.*;

import king.*;
import king.core.*;
//}}}
/**
* <code>RubberBandTool</code> is a toy version of Sculpt for KiNG.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jul 13 11:26:26 EDT 2004
*/
public class RubberBandTool extends BasicTool implements Runnable
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Triple endpoint0    = new Triple(0,0,0);
    Triple endpoint1    = new Triple(10,10,10);
    Triple mouseTug     = new Triple();
    
    KList               rubberBand;
    StateManager        stateMan;
    GradientMinimizer   minimizer;
    Collection          baseTerms;
    
    KPoint          draggedPoint = null;
    KPoint[]        allPoints = null;

    volatile boolean runMin = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RubberBandTool(ToolBox tb)
    {
        super(tb);
        this.rubberBand = makeRandomConf(100);
        this.stateMan   = makeState(rubberBand);
        
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setPriority( Thread.currentThread().getPriority() - 1 );
        thread.start();
    }
//}}}

//{{{ makeRandomConf, makeState
//##############################################################################
    KList makeRandomConf(int howmany)
    {
        KList list = new KList();
        list.setName("rubber band");
        list.setColor(KPalette.pinktint);
        
        VectorPoint prev = null;
        for(int i = 0; i < howmany; i++)
        {
            VectorPoint p = new VectorPoint(list, "pt "+i, prev);
            p.setXYZ(10*Math.random(), 10*Math.random(), 10*Math.random());
            list.add(p);
            prev = p;
        }
        
        return list;
    }
    
    /** Returns a set of EnergyTerm objects for the list */
    StateManager makeState(KList klist)
    {
        ArrayList points = new ArrayList(klist.children);
        points.add(this.endpoint0);
        points.add(this.endpoint1);
        points.add(this.mouseTug);
        
        int len = points.size() - 3;
        ArrayList terms = new ArrayList();
        for(int i = 0; i < len-1; i++)
            terms.add(new BondTerm(i, i+1, 1, 10));
        //terms.add(new BondTerm(0,       len,    0, 1));
        //terms.add(new BondTerm(len-1,   len+1,  0, 1));
        for(int i = 0; i < len-2; i++)
            terms.add(new AngleTerm(i, i+1, i+2, 120, 10));
        
        StateManager stateman = new StateManager((MutableTuple3[])points.toArray(new MutableTuple3[points.size()]), len);
        stateman.setEnergyTerms((EnergyTerm[])terms.toArray(new EnergyTerm[terms.size()]));
        baseTerms = terms;
        
        return stateman;
    }
//}}}

//{{{ start
//##################################################################################################
    public void start()
    {
        super.start();
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        KGroup group = new KGroup(kin, "RubberBand");
        group.setDominant(true);
        KSubgroup subgroup = new KSubgroup(group, "");
        group.add(subgroup);
        this.rubberBand.setOwner(subgroup);
        subgroup.add(rubberBand);
        
        kin.add(group);
        kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }
//}}}

//{{{ run
//##############################################################################
    public void run()
    {
        while(true)//(!backgroundTerminate)
        {
            // We re-create the minimizer in case it hit bottom last time
            this.minimizer = new GradientMinimizer(stateMan);
            long time = System.currentTimeMillis();
            int steps = 0;
            boolean done = false;
            while(runMin && !done)
            {
                synchronized(stateMan)
                {
                    done = !minimizer.step();
                    steps++;
                }
                
                long elapsed = System.currentTimeMillis() - time;
                if(elapsed > 33) // no more than 30 updates / sec
                {
                    // update the kinemage from the GUI thread
                    //System.err.println(steps+" steps between updates"); steps = 0;
                    SwingUtilities.invokeLater(new ReflectiveRunnable(this, "updateKinemage"));
                }
            }//while runMin
            //System.err.println(steps+" steps between updates"); steps = 0;
            SwingUtilities.invokeLater(new ReflectiveRunnable(this, "updateKinemage"));
            
            // we have to own the lock in order to wait()
            synchronized(this)
            {
                // we will be notify()'d when state changes
                try { this.wait(); }
                catch(InterruptedException ex) {}
            }
        }
    }
//}}}

//{{{ updateKinemage
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void updateKinemage()
    {
        synchronized(stateMan)
        {
            stateMan.writeState();
        }
        kCanvas.repaint();
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
        runMin = true;
        
        if(ev.getClickCount() == 2)
        {
            int i;
            int[] atomTypes = new int[ rubberBand.children.size() + 3 ];
            for(i = 0; i < rubberBand.children.size(); i++) atomTypes[i] = 1;
            for( ; i < atomTypes.length; i++) atomTypes[i] = 0;
            NonbondedTerm term = new NonbondedTerm(atomTypes, 6, 20);
            baseTerms.add(term);
        }

        synchronized(this) { this.notifyAll(); }
    }
//}}}

//{{{ xx_drag() functions
//##################################################################################################
    /** Override this function for (left-button) drags */
    public void drag(int dx, int dy, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(v != null && draggedPoint != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            /*float[] offset = v.translateRotated(dx, -dy, 0, Math.min(dim.width, dim.height));
            mouseTug.setX(mouseTug.getX() + offset[0]);
            mouseTug.setY(mouseTug.getY() + offset[1]);
            mouseTug.setZ(mouseTug.getZ() + offset[2]);*/
            float[] center = v.getCenter();
            float[] offset = v.translateRotated(ev.getX() - dim.width/2, dim.height/2 - ev.getY(), 0, Math.min(dim.width, dim.height));
            mouseTug.setX(center[0]+offset[0]);
            mouseTug.setY(center[1]+offset[1]);
            mouseTug.setZ(center[2]+offset[2]);

            synchronized(stateMan) { stateMan.readState(); }
            synchronized(this) { this.notifyAll(); }
        }
        else super.drag(dx, dy, ev);
    }
//}}}

//{{{ Mouse click listners
//##################################################################################################
    public void mousePressed(MouseEvent ev)
    {
        super.mousePressed(ev);
        if(kMain.getKinemage() != null)
            draggedPoint = kCanvas.getEngine().pickPoint(ev.getX(), ev.getY(), services.doSuperpick.isSelected());
        else draggedPoint = null;
        // Otherwise, we just create a nonsensical warning message about stereo picking
        
        if(draggedPoint == null)
            allPoints = null;
        else
        {
            mouseTug.like(draggedPoint);
            int i = rubberBand.children.indexOf(draggedPoint);
            int j = rubberBand.children.size() + 2;
            if(i != -1 ) synchronized(stateMan)
            {
                ArrayList terms = new ArrayList(baseTerms);
                terms.add(new BondTerm(i, j, 0, 30));
                stateMan.setEnergyTerms((EnergyTerm[])terms.toArray(new EnergyTerm[terms.size()]));
            }
            
            // The 0.5 allows for a little roundoff error,
            // both in the kinemage itself and our floating point numbers.
            Collection all = kCanvas.getEngine().pickAll3D(
                draggedPoint.getDrawX(), draggedPoint.getDrawY(), draggedPoint.getDrawZ(),
                services.doSuperpick.isSelected(), 0.5);
            allPoints = (KPoint[])all.toArray( new KPoint[all.size()] );
        }
    }
    
    public void mouseReleased(MouseEvent ev)
    {
        // Let's keep the point around so we can Z-translate too
        //draggedPoint = null;
        synchronized(stateMan)
        {
            ArrayList terms = new ArrayList(baseTerms);
            stateMan.setEnergyTerms((EnergyTerm[])terms.toArray(new EnergyTerm[terms.size()]));
        }
    }
//}}}

//{{{ getHelpAnchor, toString
//##################################################################################################
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this tool. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#navigate-tool" (or null)
    */
    public String getHelpAnchor()
    { return null; }
    
    public String toString() { return "Rubber band toy"; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

