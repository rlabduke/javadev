// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
//}}}
/**
 * <code>PointEditor</code> allows editing of point properties,
 * for single points and for groups of points.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Fri Dec 13 08:34:44 EST 2002
*/
public class PointEditor implements ChangeListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    
    GroupEditor groupEditor;
    
    JDialog ptDialog;
    JTextField ptID, ptComment, ptAspects, ptMasters, ptWidthRadius, ptX, ptY, ptZ;
    ColorPicker ptPicker;
    JCheckBox ptUnpickable;
    JLabel ptIndex;
    JButton split;
    boolean ptFirstShow = true;
    
    KPoint thePoint = null;
    KPaint ptOrigColor = null;
    int index = 0; //index of this point in the KList
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PointEditor(KingMain kmain)
    {
        kMain = kmain;
        groupEditor = new GroupEditor(kMain, kMain.getTopWindow());
        makePointDialog();
    }
//}}}

//{{{ makePointDialog
//##################################################################################################
    private void makePointDialog()
    {
        ptDialog = new JDialog(kMain.getTopWindow(), "Edit point", true);
        ptDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        ptID = new JTextField(20);
        ptComment = new JTextField(20);
        ptAspects = new JTextField(8);
        ptMasters = new JTextField(8);
        ptWidthRadius = new JTextField(8);
        ptX = new JTextField(8);
        ptY = new JTextField(8);
        ptZ = new JTextField(8);
        ptUnpickable = new JCheckBox("Unpickable");
        ptIndex = new JLabel("Index is x/xxx");

        KingPrefs prefs = kMain.getPrefs();
        int patchSize = (prefs==null? 20 : prefs.getInt("colorSwatchSize"));
        ptPicker = new ColorPicker(KPaint.BLACK_COLOR, patchSize);
        ptPicker.addChangeListener(this);
        
        split = new JButton(new ReflectiveAction("Split list before this", null, this, "onPointSplit"));
        JButton ok = new JButton(new ReflectiveAction("OK", null, this, "onPointOK"));
        JButton cancel = new JButton(new ReflectiveAction("Cancel", null, this, "onPointCancel"));
        
        JButton editGroup       = new JButton(new ReflectiveAction("Edit group", null, this, "onEditGroup"));
        JButton editSubgroup    = new JButton(new ReflectiveAction("Edit subgroup", null, this, "onEditSubgroup"));
        JButton editList        = new JButton(new ReflectiveAction("Edit list", null, this, "onEditList"));
        
        TablePane tp = new TablePane();
        //tp.insets(4);
        tp.hfill(true);
        tp.startSubtable();
        tp.add(new JLabel("Point ID"));
        tp.add(ptID, 3, 1);
        tp.newRow();//----------
        tp.add(new JLabel("Comment"));
        tp.add(ptComment, 3, 1);
        tp.newRow();//----------
        tp.skip();
        tp.add(ptUnpickable);
        tp.add(ptPicker, 1, 10);
        tp.newRow();//----------
        tp.add(new JLabel("Aspects"));
        tp.add(ptAspects);
        tp.newRow();//----------
        tp.add(new JLabel("Pointmasters"));
        tp.add(ptMasters);
        tp.newRow();//----------
        tp.add(new JLabel("Width/Radius"));
        tp.add(ptWidthRadius);
        tp.newRow();//----------
        tp.skip();
        tp.add(new JLabel("0 => from list"));
        tp.newRow();//----------
        tp.add(new JLabel("X coord"));
        tp.add(ptX);
        tp.newRow();//----------
        tp.add(new JLabel("Y coord"));
        tp.add(ptY);
        tp.newRow();//----------
        tp.add(new JLabel("Z coord"));
        tp.add(ptZ);
        tp.newRow();//----------
        tp.add(ptIndex);
        tp.center().add(split);
        tp.newRow();//----------
        tp.add(Box.createVerticalGlue(), 2, 1); // allows for extra height of color picker
        tp.endSubtable();
        
        tp.newRow();//----------
        tp.skip();
        tp.newRow();//----------
        tp.startSubtable();
        tp.center();
        tp.add(editList);
        tp.add(editSubgroup);
        tp.add(editGroup);
        tp.endSubtable();
        
        tp.newRow();//----------
        tp.add(tp.strut(0,4));
        tp.newRow();//----------
        tp.startSubtable();
        tp.center();
        tp.add(ok);
        tp.add(cancel);
        tp.endSubtable();

        ptDialog.setContentPane(tp);
    }
//}}}

//{{{ editPoint
//##################################################################################################
    public void editPoint(KPoint p)
    {
        if(p == null) return;
        KList list = (KList)p.getOwner();   if(list == null) return;
        Kinemage kin = p.getKinemage();     if(kin == null) return;
        thePoint = p;
        
        // Write values to GUI
        ptID.setText(p.getName());
        String comment = p.getComment();
        if(comment == null) ptComment.setText("");
        else                ptComment.setText(comment);
        ptAspects.setText(p.getAspects());
        ptMasters.setText(kin.fromPmBitmask(p.getPmMask()));
        ptX.setText(Float.toString(p.getOrigX()));
        ptY.setText(Float.toString(p.getOrigY()));
        ptZ.setText(Float.toString(p.getOrigZ()));
        ptUnpickable.setSelected(p.isUnpickable());
        
        // Color
        ptOrigColor = p.getColor();
        ptPicker.setBackgroundMode(kMain.getCanvas().getEngine().backgroundMode);
        ptPicker.setExtras(kMain.getKinemage().getNewPaintMap().values());
        ptPicker.setSelection(ptOrigColor);
        
        // Width / Radius
        if(p instanceof VectorPoint)
        {
            ptWidthRadius.setEnabled(true);
            ptWidthRadius.setText(Integer.toString(p.getWidth()));
        }
        else if(p instanceof BallPoint)
        {
            ptWidthRadius.setEnabled(true);
            ptWidthRadius.setText(Float.toString(((BallPoint)p).r0));
        }
        else
        {
            ptWidthRadius.setEnabled(false);
            ptWidthRadius.setText("n/a");
        }
        
        // Index
        int size;
        index = 0;
        Iterator iter = list.iterator();
        for(size = 0; iter.hasNext(); size++)
        {
            // KPoint.equals() just compares coordinates.
            if(p == iter.next()) index = size;
        }
        ptIndex.setText("Index is "+(index+1)+"/"+size);
        if(index == 0)  split.setEnabled(false);
        else            split.setEnabled(true);
        
        ptDialog.pack();
        if(ptFirstShow) { ptDialog.setLocationRelativeTo(kMain.getTopWindow()); ptFirstShow = false; }
        ptDialog.setVisible(true);
        // thread stops here until we hit OK/Cancel
    }
//}}}

//{{{ onPointOK, onPointCancel, onPointColor
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPointOK(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();

        thePoint.setName(ptID.getText());
        String comment = ptComment.getText().trim();
        if(comment.length() == 0)   thePoint.setComment(null);
        else                        thePoint.setComment(comment);
        String aspects = ptAspects.getText().trim().toUpperCase();
        if(aspects.length() > 0)    thePoint.setAspects(aspects);
        else                        thePoint.setAspects(null);
        if(kin != null) thePoint.setPmMask(kin.toPmBitmask(ptMasters.getText().trim(), true, true));
        thePoint.setUnpickable(ptUnpickable.isSelected());
        
        try { thePoint.setOrigX(Float.parseFloat(ptX.getText().trim())); }
        catch(NumberFormatException ex) {}
        try { thePoint.setOrigY(Float.parseFloat(ptY.getText().trim())); }
        catch(NumberFormatException ex) {}
        try { thePoint.setOrigZ(Float.parseFloat(ptZ.getText().trim())); }
        catch(NumberFormatException ex) {}
        
        // Let "" be the same as zero here
        if(ptWidthRadius.getText().trim().equals("")) ptWidthRadius.setText("0");
        try {
            if(thePoint instanceof VectorPoint)
            {
                int w = Integer.parseInt(ptWidthRadius.getText().trim());
                if(w > 7) w = 7;
                if(w < 0) w = 0;
                thePoint.setWidth(w);
            }
            else if(thePoint instanceof BallPoint)
            {
                float r = Float.parseFloat(ptWidthRadius.getText().trim());
                ((BallPoint)thePoint).r0 = r;
            }
        } catch(NumberFormatException ex) {}
        
        ptDialog.setVisible(false);
        thePoint = null; // avoid memory leaks

        if(kin != null) kin.setModified(true);
        kMain.notifyChange(KingMain.EM_EDIT_FINE);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPointCancel(ActionEvent ev)
    {
        thePoint.setColor(ptOrigColor);
        ptDialog.setVisible(false);
        thePoint = null; // avoid memory leaks
    }

    public void stateChanged(ChangeEvent ev)
    {
        Kinemage kin = thePoint.getKinemage();
        if(kin == null) return;
        
        thePoint.setColor(ptPicker.getSelection());
        kMain.notifyChange(KingMain.EM_EDIT_FINE);
    }
//}}}

//{{{ onPointSplit
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPointSplit(ActionEvent ev)
    {
        KList origlist = (KList)thePoint.getOwner();            if(origlist == null) return;
        KSubgroup subgroup = (KSubgroup)origlist.getOwner();    if(subgroup == null) return;
        
        java.util.List allkids = origlist.children;
        if(index < 1 || index >= allkids.size()) return;
        
        KList newlist  = new KList(subgroup, origlist.getName());
        subgroup.add(newlist);
        
        newlist.type    = origlist.type;
        newlist.color   = origlist.color;
        newlist.radius  = origlist.radius;
        newlist.width   = origlist.width;
        newlist.flags   = origlist.flags;
        newlist.masters = new ArrayList(origlist.masters);
        newlist.setOn(origlist.isOn());
        newlist.setHasButton(origlist.hasButton());
        //newlist.setDominant(origlist.isDominant());
        
        origlist.children = new ArrayList( allkids.subList(0,index) );
        newlist.children  = new ArrayList( allkids.subList(index,allkids.size()) );
        for(Iterator iter = newlist.iterator(); iter.hasNext(); )
        { ((KPoint)iter.next()).setOwner(newlist); }
        
        // We may need to duplicate this point to avoid breaks in the list
        if(thePoint.getPrev() != null)
        {
            if(thePoint instanceof VectorPoint)
            {
                VectorPoint prev , origvp, newvp;
                origvp = (VectorPoint)thePoint;
                prev = (VectorPoint)origvp.getPrev();
                
                newvp = new VectorPoint(origlist, origvp.getName(), prev);
                newvp.setOrigX(origvp.getOrigX());
                newvp.setOrigY(origvp.getOrigY());
                newvp.setOrigZ(origvp.getOrigZ());
                newvp.setAspects(origvp.getAspects());
                newvp.setPmMask(origvp.getPmMask());
                newvp.multi = origvp.multi;
                newvp.setWidth(origvp.getWidth());
                origlist.add(newvp);
                
                origvp.setPrev(null);
            }
            else if(thePoint instanceof TrianglePoint)
            {
                TrianglePoint prev , origtp, newtp;
                origtp = (TrianglePoint)thePoint;
                prev = (TrianglePoint)origtp.getPrev();
                
                newtp = new TrianglePoint(origlist, origtp.getName(), prev);
                newtp.setOrigX(origtp.getOrigX());
                newtp.setOrigY(origtp.getOrigY());
                newtp.setOrigZ(origtp.getOrigZ());
                newtp.setAspects(origtp.getAspects());
                newtp.setPmMask(origtp.getPmMask());
                newtp.multi = origtp.multi;
                origlist.add(newtp);
                
                origtp.setPrev(null);
            }
        }
        
        Kinemage k = kMain.getKinemage();
        if(k != null) k.setModified(true);
        kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_EDIT_FINE);
    }
//}}}

//{{{ onEditGroup, onEditSubgroup, onEditList
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditGroup(ActionEvent ev)
    {
        KList list = (KList)thePoint.getOwner();            if(list == null) return;
        KSubgroup subgroup = (KSubgroup)list.getOwner();    if(subgroup == null) return;
        KGroup group = (KGroup)subgroup.getOwner();         if(group == null) return;
        if(groupEditor.editGroup(group)) kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditSubgroup(ActionEvent ev)
    {
        KList list = (KList)thePoint.getOwner();            if(list == null) return;
        KSubgroup subgroup = (KSubgroup)list.getOwner();    if(subgroup == null) return;
        if(groupEditor.editSubgroup(subgroup)) kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditList(ActionEvent ev)
    {
        KList list = (KList)thePoint.getOwner();            if(list == null) return;
        if(groupEditor.editList(list)) kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

