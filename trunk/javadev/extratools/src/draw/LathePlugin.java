// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;
import king.*;
import king.core.*;

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
//}}}
/**
* <code>LathePlugin</code> allows creation of solid
* 3D primitives from trianglelists.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Sep 25 09:09:28 EDT 2003
*/
public class LathePlugin extends Plugin
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public LathePlugin(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ lathe
//##############################################################################
    /**
    * Constructs a point cloud based on a polyline by rotation about an axis.
    * Typically this is used to construct objects with an axis of symmetry --
    * bottles, drinking glasses, tennis balls, etc.
    * The resulting point cloud is a two dimensional array with the first
    * index corresponding to the index of an input point and the second
    * corresponding to one step during the rotation.
    */
    static public Triple[][] lathe(Tuple3[] in, Tuple3 axisBase, Tuple3 axisTip, int steps)
    {
        Triple[][] out = new Triple[in.length][steps];
        Transform rot = new Transform();
        for(int i = 0; i < steps; i++)
        {
            rot.likeRotation(axisBase, axisTip, 360.0*i / (double)steps);
            for(int j = 0; j < in.length; j++)
            {
                out[j][i] = new Triple(in[j]);
                rot.transform(out[j][i]);
            }
        }
        return out;
    }
//}}}

//{{{ solidLathe
//##############################################################################
    /**
    * Converts a vectorlist into a set of trianglelists using lathe().
    */
    public KGroup solidLathe(KList vecList, Tuple3 axisBase, Tuple3 axisTip, int steps)
    {
        KGroup group = new KGroup();
        group.setName("New group");
        KSubgroup subg = new KSubgroup(group, "lathe obj");
        subg.setDominant(true);
        group.add(subg);
        
        // Put polyline into a form we can work with
        Triple[] polyline = new Triple[vecList.children.size()];
        Iterator iter = vecList.children.iterator();
        for(int i = 0; iter.hasNext(); i++)
        {
            KPoint p = (KPoint)iter.next();
            polyline[i] = new Triple( p.getOrigX(), p.getOrigY(), p.getOrigZ() );
        }
        
        // Build mesh into a TrianglePoint object
        Triple[][] mesh = lathe(polyline, axisBase, axisTip, steps);
        for(int i = 1; i < polyline.length; i++)
        {
            TrianglePoint last = null;
            KList tlist = new KList(subg, "band "+i);
            tlist.setType(KList.RIBBON);
            tlist.setColor(  ((KPoint)vecList.children.get(i)).getDrawingColor()  );
            subg.add(tlist);
            
            Triple[] strip1 = mesh[i-1];
            Triple[] strip2 = mesh[i];
            // Having strip2 be on the axis of rotation makes those triangles
            // a weird color when rendered in KiNG
            if(strip2[0].sqDistance(strip2[1]) < strip1[0].sqDistance(strip1[1]))
            {
                Triple[] swap = strip1;
                strip1 = strip2;
                strip2 = swap;
            }
            
            for(int j = 0; j < steps; j++)
            {
                TrianglePoint curr = new TrianglePoint(tlist, i+":"+j, last);
                curr.setOrigX( strip2[j].getX() );
                curr.setOrigY( strip2[j].getY() );
                curr.setOrigZ( strip2[j].getZ() );
                tlist.add(curr);
                last = curr;
                curr = new TrianglePoint(tlist, i+":"+j, last);
                curr.setOrigX( strip1[j].getX() );
                curr.setOrigY( strip1[j].getY() );
                curr.setOrigZ( strip1[j].getZ() );
                tlist.add(curr);
                last = curr;
            }
            // Close up the end
            TrianglePoint curr = new TrianglePoint(tlist, i+":0", last);
            curr.setOrigX( strip2[0].getX() );
            curr.setOrigY( strip2[0].getY() );
            curr.setOrigZ( strip2[0].getZ() );
            tlist.add(curr);
            last = curr;
            curr = new TrianglePoint(tlist, i+":0", last);
            curr.setOrigX( strip1[0].getX() );
            curr.setOrigY( strip1[0].getY() );
            curr.setOrigZ( strip1[0].getZ() );
            tlist.add(curr);
            last = curr; // unnecessary
        }
        
        return group;
    }
//}}}

//{{{ findVectorLists
//##############################################################################
    /**
    * Finds all the vectorlists in a given kinemage.
    */
    static public Collection findVectorLists(Kinemage kin)
    {
        ArrayList out = new ArrayList();
        for(Iterator gi = kin.children.iterator(); gi.hasNext(); )
        {
            KGroup group = (KGroup)gi.next();
            for(Iterator si = group.children.iterator(); si.hasNext(); )
            {
                KSubgroup subg = (KSubgroup)si.next();
                for(Iterator li = subg.children.iterator(); li.hasNext(); )
                {
                    KList list = (KList)li.next();
                    if(list.getType().equals(KList.VECTOR) && list.isTotallyOn())
                        out.add(list);
                }
            }
        }
        return out;
    }
//}}}

//{{{ getToolsMenuItem, getHelpAnchor, toString, onLathe
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
        /*JMenu menu;
        JMenuItem item;
        menu = new JMenu("Solid object drawing");
        item = new JMenuItem(new ReflectiveAction("Lathe: line -> object", null, this, "onLathe"));
        menu.add(item);
        return menu;*/
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onLathe"));
    }
    
    public String getHelpAnchor()
    { return "#solidobj-plugin"; }

    public String toString()
    { return "Lathe: line -> object"; }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onLathe(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k == null)
            return;
        Object[] choices = findVectorLists(k).toArray();
        if(choices.length == 0)
            return;
        KList choice = (KList)JOptionPane.showInputDialog(kMain.getTopWindow(),
            "Choose a vectorlist to lathe:",
            "Choose list", JOptionPane.PLAIN_MESSAGE,
            null, choices, choices[0]);
        if(choice == null)
            return;
        
        KGroup group = solidLathe(choice, new Triple(0,0,0), new Triple(0,1,0), 32);
        group.setOwner(k);
        k.add(group);
        k.setModified(true);
        kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }
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

