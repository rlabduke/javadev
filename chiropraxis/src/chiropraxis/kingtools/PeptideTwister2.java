// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
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
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.moldb2.*;
import chiropraxis.mc.*;
//}}}
/**
* <code>PeptideTwister2</code> is an interface that allows one
* to adjust many different peptide orientations at once,
* so as to minimize tau deviation along the length of some chain.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul  2 11:42:51 EDT 2003
*/
public class PeptideTwister2 extends JPanel implements ActionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    HingeTool       parent;
    TablePane       mainPanel;
    JCheckBox       enableDials;
    Residue[]       residues;
    AngleDial[]     resDials;
    JLabel[][]      resLabels;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PeptideTwister2(HingeTool parent, Collection res)
    {
        super(new BorderLayout());
        this.parent = parent;
        
        this.residues = (Residue[])res.toArray(new Residue[res.size()]);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##################################################################################################
    private void buildGUI()
    {
        resDials    = new AngleDial[ residues.length-1 ];
        resLabels   = new JLabel[    residues.length   ][ 5 ];
        
        mainPanel = new TablePane();
        mainPanel.weights(0,0).add(makeHeaderLabels());
        mainPanel.weights(1,1).hfill(true).vfill(true).startSubtable().center();
        for(int i = 0; i < resLabels.length; i++)
        {
            Box box = Box.createVerticalBox();
            for(int j = 0; j < resLabels[i].length; j++)
            {
                resLabels[i][j] = new JLabel("?");
                resLabels[i][j].setAlignmentX(0.5f);
                box.add(resLabels[i][j]);
            }
            mainPanel.add(box);
        }
        mainPanel.endSubtable();
        
        mainPanel.newRow().skip();
        mainPanel.startSubtable().center();
        mainPanel.add(mainPanel.strut(46, 92)); // a hack!
        for(int i = 0; i < resDials.length; i++)
        {
            resDials[i] = new AngleDial();
            resDials[i].setAlignmentX(0);
            resDials[i].addChangeListener(parent);
            mainPanel.add(resDials[i]);
        }
        mainPanel.add(mainPanel.strut(46, 92)); // a hack!
        mainPanel.endSubtable();
        
        enableDials = new JCheckBox("Enable peptide rotation and sidechain idealization", false);
        enableDials.addActionListener(this);
        
        this.add(mainPanel, BorderLayout.CENTER);
        this.add(enableDials, BorderLayout.NORTH);
        
        updateLabels();
        updateEnabledState();
    }
    
    private Component makeHeaderLabels()
    {
        JLabel[] headerLabels = new JLabel[] { new JLabel("Residue"), 
            new JLabel("Tau dev"), new JLabel("Karplus"), new JLabel("Ramachdrn"), new JLabel("phi,psi") };
        Box box = Box.createVerticalBox();
        for(int i = 0; i < headerLabels.length; i++)
        {
            headerLabels[i].setAlignmentX(0);
            box.add(headerLabels[i]);
        }
        return box;
    }
//}}}

//{{{ updateLabels
//##################################################################################################
    void updateLabels()
    {
        for(int i = 0; i < residues.length; i++)
        {
            parent.updateLabels(residues[i], resLabels[i]);
        }
    }
//}}}

//{{{ updateConformation
//##################################################################################################
    ModelState updateConformation(ModelState startState)
    {
        if(enableDials.isSelected())
        {
            double[] angles = new double[resDials.length];
            for(int i = 0; i < resDials.length; i++) angles[i] = resDials[i].getDegrees();
            
            boolean[] idealizeSC = new boolean[residues.length];
            Arrays.fill(idealizeSC, true);
            if(!parent.cbIdealizeSC.isSelected())
                idealizeSC[0] = idealizeSC[idealizeSC.length-1] = false;
            
            return CaRotation.twistPeptides(residues, startState, angles, idealizeSC);
        }
        else return startState;
    }
//}}}

//{{{ actionPerformed, updateEnabledState
//##################################################################################################
    /** Catches the enable/disable event */
    public void actionPerformed(ActionEvent ev)
    {
        updateEnabledState();
        parent.stateChanged(new ChangeEvent(this));
    }
    
    void updateEnabledState()
    {
        final boolean enable = enableDials.isSelected();
        
        for(int i = 0; i < resLabels.length; i++)
            for(int j = 0; j < resLabels[i].length; j++)
                resLabels[i][j].setEnabled(enable);
        
        for(int i = 0; i < resDials.length; i++)
            resDials[i].setEnabled(enable);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

