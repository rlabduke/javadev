// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
//import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import driftwood.gui.*;
//}}}
/**
* <code>ContentPane</code> contains all of the GUI elements,
* except for the menu bar (which is held by the top-level window or applet).
*
* <p>Begun on Wed Apr 24 11:22:51 EDT 2002
* <br>Copyright (C) 2002-2004 by Ian W. Davis. All rights reserved.
*/
public class ContentPane extends JPanel // implements ...
{
//{{{ Variables
//##################################################################################################
    KingMain kMain = null;
    JScrollPane buttonScroll = null;
//}}}

//{{{ Constructor
//##################################################################################################
    /**
    * Does minimal initialization for a main window.
    * Call buildGUI() to construct all the GUI elements before calling pack() and setVisible().
    * @param kmain the KingMain that owns this window
    */
    public ContentPane(KingMain kmain)
    {
        super(new BorderLayout());
        kMain = kmain;
    }
//}}}

//{{{ notifyChange
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    static final int REDO_BUTTONS = KingMain.EM_SWITCH | KingMain.EM_CLOSE | KingMain.EM_CLOSEALL | KingMain.EM_EDIT_GROSS;
    
    void notifyChange(int event_mask)
    {
        // Take care of yourself
        if((event_mask & REDO_BUTTONS) != 0)
        {
            Kinemage kin = kMain.getKinemage();
            if(kin != null) setButtons(kin.buildButtons());
            else            setButtons(Box.createVerticalBox());
        }
        
        // Notify children
    }
//}}}

//{{{ buildGUI()
//##################################################################################################
    /**
    * Called after the constructor has finished, this starts a cascade that creates all subcomponents and initializes them.
    * After calling this, be sure to call pack() and setVisible(true) to make everything appear on screen.
    */
    public void buildGUI()
    {
        Container content = this;
        Component graphicsArea, buttonArea, bottomArea;
        JSplitPane majorSplit, minorSplit;
        
        // Build major sub-components
        graphicsArea = buildGraphicsArea();
        buttonArea = buildButtons();
        bottomArea = buildBottomArea();
        
        // Build spliters
        minorSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphicsArea, buttonArea);
        minorSplit.setOneTouchExpandable(true);
        minorSplit.setResizeWeight(1.0); // gives all extra space to the left side (graphics)
        majorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, minorSplit, bottomArea);
        majorSplit.setOneTouchExpandable(true);
        majorSplit.setResizeWeight(1.0); // gives all extra space to the top side (graphics)
        
        // Assemble sub-components
        content.add(majorSplit, BorderLayout.CENTER);
    }
//}}}

//{{{ buildGraphicsArea()
//##################################################################################################
    // Assembles the area in which graphics are drawn.
    Component buildGraphicsArea()
    {
        return kMain.getCanvas();
    }
//}}}
    
//{{{ buildButtons
//##################################################################################################
    JComponent buildButtons()
    {
        // Build the buttons later; create a scrolling panel for them now
        buttonScroll = new JScrollPane();
        buttonScroll.setPreferredSize(new Dimension(150,200));
        
        // Build the kinemage chooser
        JScrollPane chooserScroll = new JScrollPane(kMain.getStable().getChooser());
        
        // Put tabbed panel into another panel along with kin chooser
        JPanel overpanel = new JPanel(new BorderLayout());
        overpanel.add(chooserScroll, BorderLayout.NORTH);
        overpanel.add(buttonScroll, BorderLayout.CENTER);
        
        return overpanel;
    }
//}}}

//{{{ buildBottomArea()
//##################################################################################################
    // Assembles the area that holds depth clipping, show markers, pickcenter, etc.
    Component buildBottomArea()
    {
        JLabel zoomLabel = new JLabel("Zoom"); 
        JSlider zoomSlider = new JSlider(kMain.getCanvas().getZoomModel());
        JLabel clipLabel = new JLabel("Clipping"); 
        JSlider clipSlider = new JSlider(kMain.getCanvas().getClipModel());
        
        JButton backButton = new JButton(new ReflectiveAction(null, kMain.prefs.stepBackIcon, this, "onAnimBackward"));
        backButton.setToolTipText("Step backward one frame in the current animation");
        JButton fwdButton = new JButton(new ReflectiveAction(null, kMain.prefs.stepForwardIcon, this, "onAnimForward"));
        fwdButton.setToolTipText("Step forward one frame in the current animation");
        
        JButton hierarchyButton = new JButton(new ReflectiveAction("Show hierarchy", null, this, "onShowHierarchy"));
        hierarchyButton.setToolTipText("Show an editable tree view of the kinemage");
        
        GridBagPanel bottomPane = new GridBagPanel();
        bottomPane.setBorder( BorderFactory.createEmptyBorder(4,1,2,1) ); //TLBR
        // zoom & clip
        bottomPane.gbc.insets = new Insets(0, 3, 0, 1); //TLBR
        bottomPane.add(zoomLabel, 0, 0);
        bottomPane.add(clipLabel, 0, 1);
        bottomPane.gbc.insets = new Insets(0, 1, 0, 3); //TLBR
        bottomPane.add(zoomSlider, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1.0, 0.0);
        bottomPane.add(clipSlider, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1.0, 0.0);
        // animate buttons
        bottomPane.gbc.insets = new Insets(1, 3, 1, 3); //TLBR
        bottomPane.add(new JLabel("Animate"), 5, 1, 2, 1);
        bottomPane.gbc.insets = new Insets(0, 3, 0, 1); //TLBR
        bottomPane.add(backButton, 5, 0);
        bottomPane.gbc.insets = new Insets(0, 1, 0, 3); //TLBR
        bottomPane.add(fwdButton, 6, 0);
        // pickcenter and markers
        bottomPane.gbc.insets = new Insets(0, 3, 0, 3); //TLBR
        bottomPane.gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPane.gbc.weightx = 0.01;
        bottomPane.add(kMain.getCanvas().getPickcenterButton(), 2, 0, 1, 1);
        bottomPane.add(kMain.getCanvas().getMarkersButton(), 2, 1, 1, 1);
        // text & tools
        if(kMain.getTextWindow() != null)
        {
            JButton textButton = kMain.getTextWindow().getButton();
            textButton.setToolTipText("Display/edit the textual annotation of this kinemage");
            bottomPane.add(textButton, 3, 0, 1, 1);
        }
        bottomPane.add(hierarchyButton, 3, 1, 1, 1);
        
        return bottomPane;
    }
//}}}

//{{{ setButtons()
//##################################################################################################
    public void setButtons(Component c)
    {
        buttonScroll.setViewportView(c);
    }
//}}}

//{{{ onAnimForward, onAnimBackward, onShowHierarchy
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnimForward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null)
        {
            AnimationGroup a = k.getCurrentAnimation();
            if(a != null) a.forward();
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnimBackward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null)
        {
            AnimationGroup a = k.getCurrentAnimation();
            if(a != null) a.backward();
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowHierarchy(ActionEvent ev)
    {
        KinTree win = kMain.getKinTree();
        if(win != null) win.show();
    }
//}}}
}//class
