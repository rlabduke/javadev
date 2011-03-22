// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;
import king.points.*;

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
import java.util.List;
//}}}
/**
* <code>PointEditor</code> allows editing of point properties,
* for single points and for groups of points.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
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
    JButton ok;
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
        ptDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
        /*JButton*/ ok = new JButton(new ReflectiveAction("OK", null, this, "onPointOK"));
        JButton cancel = new JButton(new ReflectiveAction("Cancel", null, this, "onPointCancel"));
        
        // Key bindings: just type the key to execute -- DAK 110311
        InputMap im = ptDialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,     0                       ), "ptcancel");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,          KingMain.MENU_ACCEL_MASK), "ptcancel");
        ActionMap am = ptDialog.getRootPane().getActionMap();
        am.put("ptcancel", new ReflectiveAction(null, null, this, "onPointCancel"));
        
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
        KList list = (KList)p.getParent();  if(list == null) return;
        Kinemage kin = p.getKinemage();     if(kin == null) return;
        thePoint = p;
        
        // Write values to GUI
        ptID.setText(p.getName());
        String comment = p.getComment();
        if(comment == null) ptComment.setText("");
        else                ptComment.setText(comment);
        ptAspects.setText(p.getAspects());
        ptMasters.setText(kin.fromPmBitmask(p.getPmMask()));
        ptX.setText(Float.toString((float) p.getX()));
        ptY.setText(Float.toString((float) p.getY()));
        ptZ.setText(Float.toString((float) p.getZ()));
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
        ptDialog.getRootPane().setDefaultButton(ok); // DAK July 26 2009
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
        
        try { thePoint.setX(Float.parseFloat(ptX.getText().trim())); }
        catch(NumberFormatException ex) {}
        try { thePoint.setY(Float.parseFloat(ptY.getText().trim())); }
        catch(NumberFormatException ex) {}
        try { thePoint.setZ(Float.parseFloat(ptZ.getText().trim())); }
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
        
        ptDialog.dispose();
        thePoint = null; // avoid memory leaks

        if(kin != null) kin.setModified(true);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPointCancel(ActionEvent ev)
    {
        thePoint.setColor(ptOrigColor);
        ptDialog.dispose();
        thePoint = null; // avoid memory leaks
    }

    public void stateChanged(ChangeEvent ev)
    {
        Kinemage kin = thePoint.getKinemage();
        if(kin == null) return;
        
        thePoint.setColor(ptPicker.getSelection());
    }
//}}}

//{{{ onPointSplit
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPointSplit(ActionEvent ev)
    {
        KList origlist = (KList)thePoint.getParent();   if(origlist == null) return;
        KGroup subgroup = (KGroup)origlist.getParent(); if(subgroup == null) return;
        if(index < 1 || index >= origlist.getChildren().size()) return;
        
        KList newlist = origlist.clone(true); // do clone points
        
        ArrayList<KPoint> origpts = origlist.getChildren();
        ArrayList<KPoint> origsubset = new ArrayList<KPoint>( origpts.subList(0, index+1) );
        origpts.clear();
        origpts.addAll(origsubset);
        origlist.fireKinChanged(AHE.CHANGE_POINT_CONTENTS);
        
        ArrayList<KPoint> newpts = newlist.getChildren();
        ArrayList<KPoint> newsubset = new ArrayList<KPoint>( newpts.subList(index, newpts.size()) );
        newsubset.get(0).setPrev(null); // break the chain for vector / triangle points
        newpts.clear();
        newpts.addAll(newsubset);
        subgroup.add(newlist);

        Kinemage k = kMain.getKinemage();
        if(k != null) k.setModified(true);
    }
//}}}

//{{{ onEditGroup, onEditSubgroup, onEditList
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditGroup(ActionEvent ev)
    {
        KList list = (KList)thePoint.getParent();       if(list == null) return;
        KGroup subgroup = (KGroup)list.getParent();     if(subgroup == null) return;
        KGroup group = (KGroup)subgroup.getParent();    if(group == null) return;
        groupEditor.editGroup(group);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditSubgroup(ActionEvent ev)
    {
        KList list = (KList)thePoint.getParent();       if(list == null) return;
        KGroup subgroup = (KGroup)list.getParent();     if(subgroup == null) return;
        groupEditor.editSubgroup(subgroup);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onEditList(ActionEvent ev)
    {
        KList list = (KList)thePoint.getParent();       if(list == null) return;
        groupEditor.editList(list);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

