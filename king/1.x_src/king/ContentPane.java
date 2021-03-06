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
    JSplitPane minorSplit = null; // hold buttons and graphics area
    JSplitPane majorSplit = null; // hold minor split and zoom/clip sliders
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
        
        // Set up keystrokes for animations
        ActionMap am = this.getActionMap();
        InputMap im = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A , 0), "anim1fwd" );
        am.put("anim1fwd", new ReflectiveAction(null, null, this, "onAnimForward" ) );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A , KeyEvent.SHIFT_MASK), "anim1back" );
        am.put("anim1back", new ReflectiveAction(null, null, this, "onAnimBackward" ) );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B , 0), "anim2fwd" );
        am.put("anim2fwd", new ReflectiveAction(null, null, this, "onAnim2Forward" ) );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B , KeyEvent.SHIFT_MASK), "anim2back" );
        am.put("anim2back", new ReflectiveAction(null, null, this, "onAnim2Backward" ) );
        
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS , 0), "accum1" );
        am.put("accum1", new ReflectiveAction(null, null, this, "onAccumulate" ) );
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS , KeyEvent.SHIFT_MASK), "accum2" );
        am.put("accum2", new ReflectiveAction(null, null, this, "onAccumulate2" ) );
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
    /** Defaults to showing both button panel and slider panel */
    public void buildGUI() { buildGUI(true, true); }

    /**
    * Called after the constructor has finished, this starts a cascade that creates all subcomponents and initializes them.
    * After calling this, be sure to call pack() and setVisible(true) to make everything appear on screen.
    */
    public void buildGUI(boolean useButtons, boolean useSliders)
    {
        Container content = this;
        Component graphicsArea, buttonArea, topArea, bottomArea, totalArea;
        
        // Build major sub-components
        graphicsArea = buildGraphicsArea();
        buttonArea = buildButtons();
        bottomArea = buildBottomArea();
        
        // Build top component -- horizontal splitter
        if(useButtons)
        {
            minorSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphicsArea, buttonArea);
            minorSplit.setOneTouchExpandable(true);
            minorSplit.setResizeWeight(1.0); // gives all extra space to the left side (graphics)
            topArea = minorSplit;
        }
        else
        {
            minorSplit = null;
            topArea = graphicsArea;
        }
        
        // Build total GUI -- vertical splitter
        if(useSliders)
        {
            majorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topArea, bottomArea);
            majorSplit.setOneTouchExpandable(true);
            majorSplit.setResizeWeight(1.0); // gives all extra space to the top side (graphics)
            content.add(majorSplit, BorderLayout.CENTER);
        }
        else
        {
            majorSplit = null;
            content.add(topArea, BorderLayout.CENTER);
        }
    }
//}}}

//{{{ buildGraphicsArea, get/setGraphicsComponent
//##################################################################################################
    // Assembles the area in which graphics are drawn.
    Component buildGraphicsArea()
    {
        return kMain.getCanvas();
    }
    
    /** Sets the component that will occupy KinCanvas's usual space */
    public void setGraphicsComponent(Component c)
    {
        if(minorSplit == null)
        {
            if(majorSplit == null) this.add(c, BorderLayout.CENTER);
            else majorSplit.setTopComponent(c);
        }
        else minorSplit.setLeftComponent(c);
    }
    
    /** Gets the component currently acting as the drawing surface */
    public Component getGraphicsComponent()
    {
        if(minorSplit == null)
        {
            if(majorSplit == null) return this.getComponents()[0];
            else return majorSplit.getTopComponent();
        }
        else return minorSplit.getLeftComponent();
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
        KingPrefs prefs = kMain.getPrefs();
        Kinemage kin = kMain.getKinemage();
        if(kin == null || prefs == null)
        {
            buttonScroll.setViewportView(c);
            return;
        }
        
        TablePane2 cp = new TablePane2();
        cp.hfill(true).vfill(true).insets(0).addCell(c,2,1).newRow();
        cp.weights(1,0).insets(1).memorize();
        if(kin.hasAnimateGroups())
        {
            JButton backButton = new JButton(new ReflectiveAction(null, prefs.stepBackIcon, this, "onAnimBackward"));
            backButton.setToolTipText("Step backward one frame in the main animation");
            JButton fwdButton = new JButton(new ReflectiveAction(null, prefs.stepForwardIcon, this, "onAnimForward"));
            fwdButton.setToolTipText("Step forward one frame in the main animation");
            cp.addCell(cp.strut(0,8),2,1).newRow();
            cp.center().addCell(new JLabel("Animate"),2,1).newRow();
            cp.right().addCell(backButton).left().addCell(fwdButton).newRow();
        }
        if(kin.has2AnimateGroups())
        {
            JButton backButton = new JButton(new ReflectiveAction(null, prefs.stepBackIcon, this, "onAnim2Backward"));
            backButton.setToolTipText("Step backward one frame in the secondary animation");
            JButton fwdButton = new JButton(new ReflectiveAction(null, prefs.stepForwardIcon, this, "onAnim2Forward"));
            fwdButton.setToolTipText("Step forward one frame in the secondary animation");
            cp.addCell(cp.strut(0,8),2,1).newRow();
            cp.center().addCell(new JLabel("2-Animate"),2,1).newRow();
            cp.right().addCell(backButton).left().addCell(fwdButton).newRow();
        }
        
        buttonScroll.setViewportView(cp);
        
        // Makes sure that the brushed metal look appears on OS X.
        // java.swing.Boxes apparently don't draw their background correctly.
        
        // Aiieee! This just makes it worse. Half stripped, half metal!
        //JPanel wrapper = new JPanel(new BorderLayout());
        //wrapper.add(c, BorderLayout.CENTER);
        //buttonScroll.setViewportView(wrapper);
    }
//}}}

//{{{ onAnim(2)Forward, onAnim(2)Backward, onShowHierarchy
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnimForward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null) k.animate(1);
        kMain.getCanvas().repaint();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnimBackward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null) k.animate(-1);
        kMain.getCanvas().repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnim2Forward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null) k.animate2(1);
        kMain.getCanvas().repaint();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAnim2Backward(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null) k.animate2(-1);
        kMain.getCanvas().repaint();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onShowHierarchy(ActionEvent ev)
    {
        KinTree win = kMain.getKinTree();
        if(win != null) win.show();
    }
//}}}

//{{{ onAccumulate, onAccumulate2
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAccumulate(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null) k.accumulate();
        kMain.getCanvas().repaint();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAccumulate2(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k != null) k.accumulate2();
        kMain.getCanvas().repaint();
    }
//}}}
}//class
