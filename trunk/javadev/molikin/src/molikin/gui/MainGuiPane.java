// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;
import molikin.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>MainGuiPane</code> sets up the main GUI window. All a client needs to do
* is add() some buttons and listen for them to be pressed!
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Oct 17 11:57:01 EDT 2005
*/
public class MainGuiPane extends TablePane2 implements ListSelectionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /**
    * The list of current drawing panels (ball and stick, CPK, ribbon, etc.)
    * Add yourself as a listener if you need to know when the user switches.
    */
    public JList  drawingPaneList;
    
    CardLayout          drawingCards;
    JPanel              drawingPanel;
    
    CoordinateFile      coordFile;
    int                 paneNumber = 1;
    Collection          paneListData; // holds DrawingPane objects
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MainGuiPane(CoordinateFile cFile)
    {
        super();
        this.coordFile = cFile;
        this.paneListData = new ArrayList();
        
        buildGUI();
        onNewBallAndStick(null);
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        drawingPaneList = new FatJList(0, 10);
        drawingPaneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        drawingPaneList.setVisibleRowCount(8);
        drawingPaneList.addListSelectionListener(this);
        
        drawingCards = new CardLayout();
        drawingPanel = new JPanel(drawingCards);
        
        JButton ballAndStick = new JButton(new ReflectiveAction("New ball & stick", null, this, "onNewBallAndStick"));
        
        String bugReporting = "Doesn't work? Send the PDB/CIF file, the kinemage, and a description of your problem to iwd@duke.edu";
        
        final int insetSize = 10;
        this.insets(insetSize).memorize();
        // The HTML helps with word wrapping... (maybe)
        this.addCell(new JLabel("<html><i>"+bugReporting+"</i></html>"), 3, 1).newRow();
        this.weights(0,1).startSubtable();
            this.insets(insetSize).memorize();
            this.addCell(ballAndStick).newRow();
        this.endSubtable();
        this.weights(0,1).vfill(true).addCell(new JScrollPane(drawingPaneList));
        this.hfill(true).vfill(true).addCell(drawingPanel);
        
        // Space underneath for client buttons:
        this.newRow();
        this.skip();
        this.skip();
        this.startSubtable();
            this.insets(insetSize).memorize();
            //this.right().addCell(new JButton("Fake OK btn"));
    }
//}}}

//{{{ packParent, valueChanged (for ListSelectionListener)
//##############################################################################
    /**
    * Causes the window/dialog/etc that contains this component
    * to be resized and laid out again,
    * assuming some of its children have been invalidated.
    */
    void packParent()
    {
        // Find the top-level ancestor of this component,
        // and cause it to be laid out again.
        Container parent = this;
        while((parent = parent.getParent()) != null)
        {
            if(parent instanceof Window)
            {
                ((Window)parent).pack();
                break;
            }
        }
    }
    
    public void valueChanged(ListSelectionEvent ev)
    {
        if(!drawingPaneList.getValueIsAdjusting())
        {
            Object selected = drawingPaneList.getSelectedValue();
            if(selected != null)
                drawingCards.show(drawingPanel, selected.toString());
        }
    }
//}}}

//{{{ getSelectedPane, getAllPanes
//##############################################################################
    /** Might return null */
    public DrawingPane getSelectedPane()
    { return (DrawingPane) drawingPaneList.getSelectedValue(); }
    
    public Collection getAllPanes()
    { return paneListData; }
//}}}

//{{{ addDrawingPane, onNewBallAndStick
//##############################################################################
    void addDrawingPane(DrawingPane pane)
    {
        drawingPanel.add( (Component)pane, pane.toString() );

        paneListData.add(pane);
        drawingPaneList.setListData( paneListData.toArray() );
        drawingPaneList.setSelectedValue(pane, true);
        
        packParent();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onNewBallAndStick(ActionEvent ev)
    {
        addDrawingPane(new BallAndStickPane(coordFile, (paneNumber++)+" - Ball & stick"));
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

