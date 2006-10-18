// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.star.*;
//}}}
/**
* <code>CifSecondaryStructure</code> returns secondary structure assignments
* based on the mmCIF file struct_conf category.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb  2 09:45:24 EST 2006
*/
class CifSecondaryStructure extends SecondaryStructure
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CifSecondaryStructure(DataCell data) throws IOException
    {
        super();
        doStructConf(data);     // helix, turns defined here (maybe strands)
        doStructSheet(data);    // strands/sheets mostly defined here
    }
//}}}

//{{{ doStructConf
//##############################################################################
    void doStructConf(DataCell data) throws IOException
    {
        List ssConfTypeId       = data.getItem("_struct_conf.conf_type_id");
        List ssBegLabelAsymId   = data.getItem("_struct_conf.beg_label_asym_id");
        List ssBegLabelSeqId    = data.getItem("_struct_conf.beg_label_seq_id");
        List ssEndLabelAsymId   = data.getItem("_struct_conf.end_label_asym_id");
        List ssEndLabelSeqId    = data.getItem("_struct_conf.end_label_seq_id");
        
        int ssElements = ssConfTypeId.size();
        if(ssElements == 0) return;
        if(ssBegLabelAsymId.size()  != ssElements
        || ssBegLabelSeqId.size()   != ssElements
        || ssEndLabelAsymId.size()  != ssElements
        || ssEndLabelSeqId.size()   != ssElements)
            throw new IOException("Elements in struct_conf disagree in length");
        
        for(int i = 0; i < ssElements; i++)
        {
            Range r = new Range();
            String type     = (String) ssConfTypeId.get(i);
                 if(type.startsWith("HELX"))    r.type = HELIX;
            else if(type.startsWith("TURN"))    r.type = TURN;
            else if(type.startsWith("STRN"))    r.type = STRAND;
            else                                r.type = COIL;
            
            String begAsym  = (String) ssBegLabelAsymId.get(i);
            String endAsym  = (String) ssEndLabelAsymId.get(i);
            if(!begAsym.equals(endAsym))
                System.err.println("Mismatched asym (chain) IDs (row "+(i+1)+")");
            r.chainId = begAsym;
            
            try {
                r.initSeqNum = Integer.parseInt(((String) ssBegLabelSeqId.get(i)).trim());
                r.endSeqNum  = Integer.parseInt(((String) ssEndLabelSeqId.get(i)).trim());
                addRange(r);
            } catch(NumberFormatException ex)
            { System.err.println("Non-numeric sequence numbers (row "+(i+1)+")"); }
        }
    }
//}}}

//{{{ doStructSheet
//##############################################################################
    void doStructSheet(DataCell data) throws IOException
    {
        List ssBegLabelAsymId   = data.getItem("_struct_sheet_range.beg_label_asym_id");
        List ssBegLabelSeqId    = data.getItem("_struct_sheet_range.beg_label_seq_id");
        List ssEndLabelAsymId   = data.getItem("_struct_sheet_range.end_label_asym_id");
        List ssEndLabelSeqId    = data.getItem("_struct_sheet_range.end_label_seq_id");
        
        int ssElements = ssBegLabelSeqId.size();
        if(ssElements == 0) return;
        if(ssBegLabelAsymId.size()  != ssElements
        || ssBegLabelSeqId.size()   != ssElements
        || ssEndLabelAsymId.size()  != ssElements
        || ssEndLabelSeqId.size()   != ssElements)
            throw new IOException("Elements in struct_sheet_range disagree in length");
        
        for(int i = 0; i < ssElements; i++)
        {
            Range r = new Range();
            r.type = STRAND; // this is a beta sheet, so it has to be
            
            String begAsym  = (String) ssBegLabelAsymId.get(i);
            String endAsym  = (String) ssEndLabelAsymId.get(i);
            if(!begAsym.equals(endAsym))
                System.err.println("Mismatched asym (chain) IDs (row "+(i+1)+")");
            r.chainId = begAsym;
            
            try {
                r.initSeqNum = Integer.parseInt(((String) ssBegLabelSeqId.get(i)).trim());
                r.endSeqNum  = Integer.parseInt(((String) ssEndLabelSeqId.get(i)).trim());
                addRange(r);
            } catch(NumberFormatException ex)
            { System.err.println("Non-numeric sequence numbers (row "+(i+1)+")"); }
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

