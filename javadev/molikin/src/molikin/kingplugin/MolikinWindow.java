// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.kingplugin;
import molikin.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import driftwood.util.*;
import king.*;
import king.core.*;
//}}}
/**
* <code>MolikinWindow</code> is the GUI for generating kinemages of
* a particular CoordinateFile.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Nov  9 13:54:38 EST 2005
*/
public class MolikinWindow //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    ToolBox                 parent;
    KingMain                kMain;
    KinCanvas               kCanvas;
    ToolServices            services;

    CoordinateFile          coordFile;
    JFrame                  frame;
    MainGuiPane             guiPane;
    int                     kinNumber = 1;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MolikinWindow(ToolBox tb, CoordinateFile cfile)
    {
        super();
        
        parent      = tb;
        kMain       = tb.kMain;
        kCanvas     = tb.kCanvas;
        services    = tb.services;
        
        coordFile   = cfile;
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    void buildGUI()
    {
        guiPane = new MainGuiPane(coordFile);
        guiPane.weights(1,0).addCell(new JButton(new ReflectiveAction("Done", null, this, "onDone")));
        guiPane.weights(0,0).addCell(new JButton(new ReflectiveAction("As new kinemage", null, this, "onCreateKinemage")));
        guiPane.weights(0,0).addCell(new JButton(new ReflectiveAction("Append to current", null, this, "onAppendKinemage")));
        
        String title = "Molikin";
        if(coordFile.getFile() != null)         title = coordFile.getFile().getName()+" - "+title;
        else if(coordFile.getIdCode() != null)  title = coordFile.getIdCode()+" - "+title;
        
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(guiPane);
        frame.pack();
        
        // Position this window just right of the main window,
        // so we can find it again after creating a kinemage.
        Container w = kMain.getContentContainer();
        if(w != null)
        {
            Point p = w.getLocation();
            Dimension dimDlg = frame.getSize();
            Dimension dimWin = w.getSize();
            //p.x += dimWin.width - (dimDlg.width / 2) ;
            p.x += dimWin.width - dimDlg.width + 64;
            p.y += (dimWin.height - dimDlg.height) / 2;
            frame.setLocation(p);
        }
        
        frame.setVisible(true);
    }
    
    public void onCreateKinemage(ActionEvent ev)
    { buildKinemage(null); }
    
    public void onAppendKinemage(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage(); // could be null
        buildKinemage(kin);
        if(kin != null) kin.setModified(true);
    }
    
    public void onDone(ActionEvent ev)
    {
        frame.dispose();
    }
//}}}

//{{{ buildKinemage
//##############################################################################
    void buildKinemage(Kinemage appendTo)
    {
        StreamTank kinData = new StreamTank();
        PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
        
        out.println("@kinemage "+(kinNumber++));
        out.println("@onewidth");
        guiPane.printKinemage(out);

        out.flush();
        kinData.close();
        kMain.getKinIO().loadStream(kinData.getInputStream(), kinData.size(), appendTo);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

