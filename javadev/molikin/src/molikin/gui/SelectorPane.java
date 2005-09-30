// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
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
public class SelectorPane extends TablePane2
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JList           modelList;      // holds Models
    JList           chainList;      // holds Strings
    JList           resTypeList;    // holds Strings
    JTextField      resRangeField;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public SelectorPane(CoordinateFile cfile)
    {
        super();
        buildGUI();
        populateLists(cfile);
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        modelList = new FatJList(0, 4);
            modelList.setVisibleRowCount(6);
        chainList = new FatJList(0, 4);
            chainList.setVisibleRowCount(6);
        resTypeList = new FatJList(0, 4);
            resTypeList.setVisibleRowCount(6);
        resRangeField = new JTextField();
        
        this.insets(2,8,2,8).memorize();
        this.addCell(new JLabel("Models"));
        this.addCell(new JLabel("Chains"));
        this.addCell(new JLabel("Types"));
        this.newRow();
        this.hfill(true).addCell(new JScrollPane(modelList));
        this.hfill(true).addCell(new JScrollPane(chainList));
        this.hfill(true).addCell(new JScrollPane(resTypeList));
        /*this.startSubtable();
            this.addCell(new JCheckBox("Protein", true)).newRow();
            this.addCell(new JCheckBox("DNA / RNA", true)).newRow();
            this.addCell(new JCheckBox("Hets", true)).newRow();
            this.addCell(new JCheckBox("Ions", true)).newRow();
            this.addCell(new JCheckBox("Water", true)).newRow();
        this.endSubtable();*/
        this.newRow();
        this.startSubtable(3,1);//(4,1);
            this.weights(0.0, 1.0).addCell(new JLabel("Ranges"));
            this.hfill(true).weights(1.0, 1.0).addCell(resRangeField);
            this.newRow();
            this.addCell(new JLabel("e.g. 5, 34, 77-100, 120b-120f, 200"), 2, 1);
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
        
        UberSet chainIDs = new UberSet();
        HashSet resNames = new HashSet();
        for(Iterator iter = models.iterator(); iter.hasNext(); )
        {
            Model m = (Model) iter.next();
            chainIDs.addAll(m.getChainIDs());
            for(Iterator ri = m.getResidues().iterator(); ri.hasNext(); )
            {
                Residue r = (Residue) ri.next();
                resNames.add(r.getName());
            }
        }
        
        chainList.setListData(chainIDs.toArray());
        chainList.setSelectionInterval(0, chainIDs.size()-1); // select all
        
        Object[] rNames = resNames.toArray();
        Arrays.sort(rNames);
        resTypeList.setListData(rNames);
        resTypeList.setSelectionInterval(0, resNames.size()-1); // select all
    }
//}}}

//{{{ getSelectedResidues
//##############################################################################
    public Set getSelectedResidues()
    {
        Collection models = Arrays.asList(modelList.getSelectedValues());
        Collection chains = Arrays.asList(chainList.getSelectedValues());
        Set resTypes = new HashSet(Arrays.asList(resTypeList.getSelectedValues()));
        
        UberSet selected = new UberSet();
        
        for(Iterator mi = models.iterator(); mi.hasNext(); )
        {
            Model m = (Model) mi.next();
            for(Iterator ci = chains.iterator(); ci.hasNext(); )
            {
                String chainID = (String) ci.next();
                Collection chain = m.getChain(chainID);
                if(chain == null) continue;
                for(Iterator ri = chain.iterator(); ri.hasNext(); )
                {
                    Residue r = (Residue) ri.next();
                    if(resTypes.contains(r.getName()))
                        selected.add(r);
                }
            }
        }
        
        return selected;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

