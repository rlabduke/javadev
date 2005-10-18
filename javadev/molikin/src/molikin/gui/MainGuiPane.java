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

