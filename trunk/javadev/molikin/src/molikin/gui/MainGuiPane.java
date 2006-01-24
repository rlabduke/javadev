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
import driftwood.data.*;
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
    // pinktint is not used b/c that's used for connections to hets
    static final String[] BACKBONE_COLORS = { "white", "yellowtint", "peachtint", "greentint", "bluetint", "lilactint" };
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
    String              idCode;
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

        if(coordFile.getIdCode() != null)       this.idCode = coordFile.getIdCode();
        else if(coordFile.getFile() != null)    this.idCode = coordFile.getFile().getName();
        else                                    this.idCode = "macromol";
        
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
        JButton vanDerWaals = new JButton(new ReflectiveAction("New van der Waals", null, this, "onNewVanDerWaals"));
        JButton ribbons = new JButton(new ReflectiveAction("New ribbons", null, this, "onNewRibbons"));
        JButton removePane = new JButton(new ReflectiveAction("Clear selected", null, this, "onRemovePane"));
        JButton removeAll = new JButton(new ReflectiveAction("Clear all", null, this, "onRemoveAll"));
        
        String bugReporting = "Doesn't work? Send the PDB/CIF file, the kinemage, and a description of your problem to iwd@duke.edu";
        
        final int insetSize = 10;
        this.insets(insetSize).memorize();
        // The HTML helps with word wrapping... (maybe)
        this.addCell(new JLabel("<html><i>"+bugReporting+"</i></html>"), 3, 1).newRow();
        this.weights(0,1).startSubtable();
            this.insets(insetSize).hfill(true).weights(1,0).memorize();
            this.addCell(ballAndStick).newRow();
            this.addCell(vanDerWaals).newRow();
            this.addCell(ribbons).newRow();
            this.addCell(removePane).newRow();
            this.addCell(removeAll).newRow();
            // This acts as "glue" at the bottom to absorb all the extra space.
            // The result? All the buttons float to the top of the space!
            this.weights(1,1).addCell( this.strut(0,0) );
        this.endSubtable();
        this.weights(0,1).vfill(true).addCell(new JScrollPane(drawingPaneList));
        this.hfill(true).vfill(true).addCell(drawingPanel);
        
        // Space underneath for client buttons:
        this.newRow();
        //this.skip();
        //this.skip();
        this.startSubtable(3,1);
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

//{{{ add/removeDrawingPane, onRemovePane, onRemoveAll
//##############################################################################
    void addDrawingPane(DrawingPane pane)
    {
        drawingPanel.add( (Component)pane, pane.toString() );

        paneListData.add(pane);
        drawingPaneList.setListData( paneListData.toArray() );
        drawingPaneList.setSelectedValue(pane, true);
        
        packParent();
    }
    
    void removeDrawingPane(DrawingPane pane)
    {
        drawingPanel.remove( (Component)pane );
        
        paneListData.remove(pane);
        Object sel = drawingPaneList.getSelectedValue();
        drawingPaneList.setListData( paneListData.toArray() );
        drawingPaneList.setSelectedValue(sel, true);
        
        //packParent();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRemovePane(ActionEvent ev)
    {
        removeDrawingPane( (DrawingPane)drawingPaneList.getSelectedValue() );
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRemoveAll(ActionEvent ev)
    {
        // Make a copy or we get ConcurrentModEx
        Collection panes = new ArrayList(paneListData);
        for(Iterator iter = panes.iterator(); iter.hasNext(); )
        {
            DrawingPane pane = (DrawingPane) iter.next();
            removeDrawingPane( pane );
        }
    }
//}}}

//{{{ onNewBallAndStick, onNewVanDerWaals
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onNewBallAndStick(ActionEvent ev)
    {
        addDrawingPane(new BallAndStickPane(coordFile, (paneNumber++)+" - Ball & stick"));
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onNewVanDerWaals(ActionEvent ev)
    {
        addDrawingPane(new VanDerWaalsPane(coordFile, (paneNumber++)+" - van der Waals"));
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onNewRibbons(ActionEvent ev)
    {
        addDrawingPane(new RibbonPane(coordFile, (paneNumber++)+" - Ribbons"));
    }
//}}}

//{{{ printKinemage
//##############################################################################
    /** Emits the kinemage (text) representation as selected by the user */
    public void printKinemage(PrintWriter out)
    {
        Collection models = this.getSelectedModels();
        boolean groupByModel = (models.size() > 1);
        Collection chains = this.getSelectedChains();
        
        int modelCount = 0;
        for(Iterator mi = models.iterator(); mi.hasNext(); modelCount++)
        {
            Model m = (Model) mi.next();
            if(groupByModel) out.println("@group {"+idCode+" "+m+"} dominant animate master= {all models}");
            
            int chainCount = 0;
            for(Iterator ci = chains.iterator(); ci.hasNext(); chainCount++)
            {
                String chainID = (String) ci.next();
                if(groupByModel)    out.println("@subgroup {chain"+chainID+"} dominant master= {chain"+chainID+"}");
                else                out.println("@group {"+idCode+" "+chainID+"} dominant");
                
                for(Iterator iter = paneListData.iterator(); iter.hasNext(); )
                {
                    DrawingPane p = (DrawingPane) iter.next();
                    String bbColor = BACKBONE_COLORS[ (groupByModel ? modelCount : chainCount) % BACKBONE_COLORS.length];
                    p.printKinemage(out, m, chainID, bbColor);
                }
            }
        }
        
        out.flush();
    }
//}}}

//{{{ getSelectedModels, getSelectedChains
//##############################################################################
    /** As a Collection of Model objects. */
    public Collection getSelectedModels()
    {
        Set selectedModels = new HashSet();
        for(Iterator iter = paneListData.iterator(); iter.hasNext(); )
        {
            DrawingPane p = (DrawingPane) iter.next();
            selectedModels.addAll(p.getSelectedModels());
        }
        Collection models = new ArrayList(coordFile.getModels());
        models.retainAll(selectedModels);
        return models;
    }
    
    /** As a Collection of Strings representing chain IDs. */
    public Collection getSelectedChains()
    {
        Set selectedChains = new HashSet();
        for(Iterator iter = paneListData.iterator(); iter.hasNext(); )
        {
            DrawingPane p = (DrawingPane) iter.next();
            selectedChains.addAll(p.getSelectedChains());
        }
        Set chains = new UberSet();
        for(Iterator iter = coordFile.getModels().iterator(); iter.hasNext(); )
        {
            Model m = (Model) iter.next();
            chains.addAll(m.getChainIDs());
        }
        chains.retainAll(selectedChains);
        return chains;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

