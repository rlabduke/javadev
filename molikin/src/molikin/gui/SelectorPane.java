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
* <code>SelectorPane</code> is a GUI element for selecting sets of residues
* from a collection of models, based on various criteria.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 30 14:37:36 EDT 2005
*/
public class SelectorPane extends TablePane2 implements ListSelectionListener, ActionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JList           modelList;      // holds Models
    JList           chainList;      // holds Strings
    JList           resNumList;     // holds Strings
    JList           resTypeList;    // holds Strings
    JTextField      resRangeField;
    
    ResRanger       resRanger;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SelectorPane(CoordinateFile cfile)
    {
        super();
        buildGUI1();
        populateLists(cfile);
        buildGUI2();
    }
//}}}

//{{{ buildGUI[12]
//##############################################################################
    private void buildGUI1()
    {
        modelList = new FatJList(0, 4);
            modelList.setVisibleRowCount(6);
        chainList = new FatJList(0, 4);
            chainList.setVisibleRowCount(6);
        resNumList = new FatJList(0, 4);
            resNumList.addListSelectionListener(this);
            resNumList.setVisibleRowCount(6);
        resTypeList = new FatJList(0, 4);
            resTypeList.setVisibleRowCount(6);
        resRangeField = new AttentiveTextField();
            resRangeField.addActionListener(this);
    }
    
    private void buildGUI2()
    {
        boolean showModels = (modelList.getModel().getSize() > 1);
        boolean showChains = (chainList.getModel().getSize() > 1);
        int cols = 2;
        if(showModels)  cols++;
        if(showChains)  cols++;
        
        this.insets(2,8,2,8).memorize();
        this.weights(1,0).memorize();
        if(showModels)  this.addCell(new JLabel("Models"));
        if(showChains)  this.addCell(new JLabel("Chains"));
        this.addCell(new JLabel("Numbers"));
        this.addCell(new JLabel("Types"));
        this.newRow();
        this.weights(1,1).memorize(); // all vertical space to lists
        if(showModels)  this.hfill(true).vfill(true).addCell(new JScrollPane(modelList));
        if(showChains)  this.hfill(true).vfill(true).addCell(new JScrollPane(chainList));
        this.hfill(true).vfill(true).addCell(new JScrollPane(resNumList));
        this.hfill(true).vfill(true).addCell(new JScrollPane(resTypeList));
        this.newRow();
        this.weights(1,0).memorize();
        this.startSubtable(cols,1);
            this.insets(2,8,2,8).memorize();
            this.weights(0,1).addCell(new JLabel("Numbers"));
            this.hfill(true).addCell(resRangeField); // all horiz space to field
            this.newRow();
            this.addCell(new JLabel("e.g. 5, 34, 77-100, 120a-120f, 200"), 2, 1);
        this.endSubtable();
    }
//}}}

//{{{ populateLists
//##############################################################################
    private void populateLists(CoordinateFile cfile)
    {
        Collection models = cfile.getModels();
        modelList.setListData(models.toArray());
        modelList.setSelectionInterval(0, models.size()-1); // select all
        
        ArrayList allRes = new ArrayList();
        UberSet chainIDs = new UberSet();
        HashSet resNames = new HashSet();
        for(Iterator iter = models.iterator(); iter.hasNext(); )
        {
            Model m = (Model) iter.next();
            allRes.addAll(m.getResidues());
            chainIDs.addAll(m.getChainIDs());
            for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
            {
                Residue r = (Residue) ri.next();
                resNames.add(r.getName());
            }
        }
        chainList.setListData(chainIDs.toArray());
        chainList.setSelectionInterval(0, chainIDs.size()-1); // select all
        
        resRanger = new ResRanger(allRes);
        Collection allResNum = resRanger.getAllNumbers();
        resNumList.setListData(allResNum.toArray());
        resNumList.setSelectionInterval(0, allResNum.size()-1); // select all
        // TODO: transfer selection to Ranger and to text field
        
        Object[] rNames = resNames.toArray();
        Arrays.sort(rNames);
        resTypeList.setListData(rNames);
        resTypeList.setSelectionInterval(0, resNames.size()-1); // select all
    }
//}}}

//{{{ getSelectedModels/Chains/Residues
//##############################################################################
    /** As a Collection of Model objects. */
    public Collection getSelectedModels()
    { return Arrays.asList(modelList.getSelectedValues()); }
    
    /** As a Collection of Strings representing chain IDs. */
    public Collection getSelectedChains()
    { return Arrays.asList(chainList.getSelectedValues()); }
    
    /** As a Set of Residue objects. */
    public Set getSelectedResidues(Model m)
    { return getSelectedResidues(m.getResidues()); }
    
    /** As a Set of Residue objects, which are a subset of the collection passed in. */
    public Set getSelectedResidues(Collection residues)
    {
        Set chains = new HashSet(Arrays.asList(chainList.getSelectedValues()));
        Set resTypes = new HashSet(Arrays.asList(resTypeList.getSelectedValues()));
        // Not needed -- list or text has already set the selection
        //resRanger.select(resRangeField.getText().toUpperCase());
        Set resNumbers = resRanger.getSelectedNumbers();
        
        UberSet selected = new UberSet();
        
        for(Iterator ri = residues.iterator(); ri.hasNext(); )
        {
            Residue r = (Residue) ri.next();
            if(resTypes.contains(r.getName()) && chains.contains(r.getChain()))
            {
                String resNum = r.getSequenceNumber().trim() + r.getInsertionCode().trim();
                if(resNumbers.contains(resNum))
                    selected.add(r);
            }
        }
        
        return selected;
    }
//}}}

//{{{ ListSelectionListener for res numbers list: valueChanged
//##############################################################################
    public void valueChanged(ListSelectionEvent ev)
    {
        if(resNumList.getValueIsAdjusting()) return;
        if(resRanger != null)
        {
            resRanger.select(resNumList.getSelectedIndices());
            resRangeField.setText( resRanger.getSelectionString() );
        }
    }
//}}}

//{{{ ActionListener for res numbers text: actionPerformed
//##############################################################################
    public void actionPerformed(ActionEvent ev)
    {
        resRanger.select(resRangeField.getText().toUpperCase());
        //resRangeField.setText( resRanger.getSelectionString() ); -- see below
        BitSet sel = resRanger.getSelectionMask();
        int[] indices = new int[ sel.cardinality() ];
        int i = 0, j = 0;
        for(i = 0; i < sel.length(); i++)
        {
            if(sel.get(i)) indices[j++] = i;
        }
        resNumList.setSelectedIndices(indices);
        // change in list selection will trigger regularization of text field contents
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

