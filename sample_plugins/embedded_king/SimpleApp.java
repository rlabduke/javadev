// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;

import driftwood.gui.*;
import king.*;
import king.core.*;
import king.points.*;
//}}}
/**
* <code>SimpleApp</code> is a bare-bones example of how to embed KiNG graphics
* into another Java application.
* This may be a good solution for cases where just creating a KiNG tool or plugin
* couldn't provide a rich enough user experience.
*
* <p>KiNG wasn't originally designed to support this, so the code may seem a bit kludgy.
* Reading the developer documentation for KiNG to understand its overall architecture
* may help make this clearer.
*
* <p>This example was originally developed for KiNG 1.x.  While is has been
* modified slightly now to work with KiNG 2.x, it is also reasonably easy to
* embed just the KiNG graphics engine, without the rest of the KiNG baggage
* (Plugin / Tool / ToolBox, KMessage, KinCanvas, etc).
* See the KiNG developer's tutorial materials for examples of how to do this.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 21 12:11:18 EDT 2006
*/
public class SimpleApp //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain kMain;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SimpleApp()
    {
        super();
        
        this.kMain = new KingMain();
        kMain.createComponents(false, false); // no on/off buttons or zoom/clip sliders
        
        // This is how we set the size of the graphics area.
        // Left unset, it defaults to about 75% of the monitor size.
        KinCanvas kCanvas = kMain.getCanvas();
        kCanvas.setPreferredSize(new Dimension(300,300));
        kCanvas.setMinimumSize(new Dimension(300,300));

        // Install our custom mouse-handling code
        ToolBox tb = kCanvas.getToolBox();
        tb.toolActivated(new SimpleTool(this, tb));

        // Load a simple kinemage for the user to start with
        URL kin = getClass().getResource("/cone.kin");
        if(kin != null) kMain.getKinIO().loadURL(kin, null);
        else System.err.println("Couldn't find the specified kinemage!");
        
        // Just for looks :)
        kMain.getContentPane().setBorder(BorderFactory.createLineBorder(new Color(0.5f, 0.5f, 0.5f), 1));

        // Create a layout
        TablePane2 cp = new TablePane2();
        cp.hfill(false).center().vfill(false).center().insets(20).memorize();
        cp.addCell(new JLabel("Hello world of embedded KiNG!"), 2, 1);
        cp.newRow();
        cp.addCell(kMain.getContentPane());
        cp.hfill(true).vfill(true).startSubtableRaw(1,1);
            cp.hfill(true).insets(4).memorize();
            cp.addCell(new JButton(new ReflectiveAction("Open kin file...", null, kMain.getMenus(), "onFileOpen"))).newRow();
            cp.addCell(new JButton(new ReflectiveAction("Create graphics in code", null, this, "onMakeKin"))).newRow();
            cp.addCell(new JButton("Button 3")).newRow();
        cp.endSubtable();
        
        // Put it in a frame and bring up the GUI
        JFrame frame = new JFrame("Simple example of embedding KiNG graphics");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(cp);
        frame.pack();
        frame.show();
    }
//}}}

//{{{ onMakeKin
//##############################################################################
    // Called by reflection -- do not rename
    /**
    * Creates a simple kinemage in code and loads it.
    * For a more extensive example, look at the demoPlugin code.
    * Also, look at king.core.KinParser, king.GroupEditor, king.PointEditor,
    * and especially king.tool.draw.DrawingTool for more examples.
    */
    public void onMakeKin(ActionEvent ev)
    {
        Kinemage kin = new Kinemage("A kinemage");
        KGroup group = new KGroup("A group");
        kin.add(group);
        KGroup subgroup = new KGroup("A subgroup");
        group.add(subgroup);
        
        KList list = new KList(KList.BALL, "List of ___");
        subgroup.add(list);
        list.setRadius(0.1f);
        list.setColor(KPalette.sea);
        
        BallPoint bp;
        bp = new BallPoint("");
        list.add(bp);
        bp.setXYZ(0, 0, 0);
        bp = new BallPoint("");
        list.add(bp);
        bp.setXYZ(3, 0, 0);
        bp = new BallPoint("");
        list.add(bp);
        bp.setXYZ(0, 4, 0);
        bp = new BallPoint("");
        list.add(bp);
        bp.setXYZ(0, 0, 5);
        
        // These viewing preferences can also be set in the Engine
        // after the kin is loaded already.
        kin.atPerspective = true; // rather than orthographic projection
        kin.atWhitebackground = true; // some folks like this
        
        kin.initAll(); // sets up default view, etc -- MUST be called at the end
        kMain.getStable().closeAll(); // clean up, avoid memory leaks
        kMain.getStable().append(Collections.singleton(kin));
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ main
//##############################################################################
    public static void main(String[] args)
    {
        new SimpleApp();
    }
//}}}
}//class

