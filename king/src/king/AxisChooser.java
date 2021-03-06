// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
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
//}}}
/**
* <code>AxisChooser</code> is the GUI for mapping high-dimensional kinemages
* onto the X, Y, and Z axes.
*
* <p>Copyright (C) 2006-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jun 13 16:10:45 EDT 2006
*/
public class AxisChooser //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain kMain;
    Kinemage kin;
    int kinDimension;
    Collection dimNames;
    
    JList xAxisList, yAxisList, zAxisList;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public AxisChooser(KingMain kMain, Kinemage kin)
    {
        super();
        this.kMain = kMain;
        this.kin = kin;
        this.kinDimension = getKinDimension(kin);
        this.dimNames = new ArrayList(kin.dimensionNames);
        for(int i = dimNames.size(); i < kinDimension; i++)
            dimNames.add("Axis "+(i+1));
        
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        int[] selected = {0, 1, 2};
        KView view = kMain.getCanvas().getCurrentView(kin);
        if(view.getViewingAxes() != null)
            selected = view.getViewingAxes();
        //System.err.println(selected[0]+","+selected[1]+","+selected[2]);
        
        final int visibleRows = 10;
        xAxisList = new FatJList(0, 10);
        xAxisList.setVisibleRowCount(visibleRows);
        xAxisList.setListData(dimNames.toArray());
        xAxisList.setSelectedIndex(Math.min(selected[0], dimNames.size()-1));
        yAxisList = new FatJList(0, 10);
        yAxisList.setVisibleRowCount(visibleRows);
        yAxisList.setListData(dimNames.toArray());
        yAxisList.setSelectedIndex(Math.min(selected[1], dimNames.size()-1));
        zAxisList = new FatJList(0, 10);
        zAxisList.setVisibleRowCount(visibleRows);
        zAxisList.setListData(dimNames.toArray());
        zAxisList.setSelectedIndex(Math.min(selected[2], dimNames.size()-1));
        
        JButton btnOK = new JButton(new ReflectiveAction("Set axes", null, this, "onSetAxes"));
        
        TablePane2 cp = new TablePane2();
        cp.insets(10).memorize();
        cp.add(new JLabel("X axis"));
        cp.add(new JLabel("Y axis"));
        cp.add(new JLabel("Z axis"));
        cp.newRow();
        cp.add(new JScrollPane(xAxisList));
        cp.add(new JScrollPane(yAxisList));
        cp.add(new JScrollPane(zAxisList));
        cp.newRow();
        cp.startSubtable(3,1).center().addCell(btnOK).endSubtable();
        
        JDialog dialog = new JDialog(kMain.getTopWindow(), "Choose axes", false /* not modal */);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(cp);
        dialog.pack();
        dialog.show(); // will return immediately, b/c dialog is non-modal
    }
//}}}

//{{{ onSetAxes
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSetAxes(ActionEvent ev)
    {
        int xIndex = Math.max(0, xAxisList.getSelectedIndex());
        int yIndex = Math.max(1, yAxisList.getSelectedIndex());
        int zIndex = Math.max(2, zAxisList.getSelectedIndex());
        
        KView view = kMain.getView();
        if(view == null) return;
        view.setViewingAxes(new int[] {xIndex, yIndex, zIndex});
        view.activateViewingAxes();
    }
//}}}

//{{{ getKinDimension
//##############################################################################
    /**
    * Figures out how many axes are present in this kinemage (i.e. its dimension).
    */
    static public int getKinDimension(Kinemage kin)
    {
        int numAxes = 0;
        for(KList list : KIterator.allLists(kin))
            numAxes = Math.max(numAxes, list.getDimension());
        return numAxes;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

