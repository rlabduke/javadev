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
import driftwood.r3.*;
//}}}
/**
 * <code>GroupEditor</code> produces a dialog box for editing group/list properties.
 * It also manages the Transform (translate/rotate/scale) operation on groups/lists.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Tue Sep 24 10:04:44 UTC 2002
*/
public class GroupEditor implements ChangeListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain        kMain;
    Frame           ownerWindow;
    
    // Kinemage
    JDialog         kiDialog;
    GridBagPanel    kiPanel;
    JTextField      kiName;
    JCheckBox       kiWhiteback, kiOnewidth, kiThinline, kiPerspec, kiFlat, kiListcolor;
    JButton         kiOK, kiCancel;

    // Groups
    JDialog         grDialog;
    GridBagPanel    grPanel;
    JTextField      grName;
    JCheckBox       grIsOff, grNoButton, grDominant, grRecessiveOn, grAnimate, gr2Animate;
    JButton         grOK, grCancel;
    
    // Subgroups
    JDialog         suDialog;
    GridBagPanel    suPanel;
    JTextField      suName;
    JCheckBox       suIsOff, suNoButton, suDominant, suRecessiveOn;
    JButton         suOK, suCancel;
    
    // Lists
    JDialog         liDialog;
    boolean         liFirstShow     = true;
    TablePane       liPanel;
    JTextField      liName;
    JCheckBox       liIsOff, liNoButton, liNoHilite;
    JTextField      liWidth, liRadius, liAlpha;
    ColorPicker     liPicker;
    KPaint          originalColor   = null;
    KList           theKList        = null;
    JButton         liOK, liCancel;
    
    // Transform
    JDialog         trDialog;
    boolean         trFirstShow     = true;
    TablePane2      trPanel;
    JTextField      trTransX, trTransY, trTransZ; // translate, not transform
    JTextField      trRotX,   trRotY,   trRotZ;
    JTextField      trScale, trScaleX, trScaleY, trScaleZ;
    JCheckBox       trAboutOrigin;
    JButton         trBtnTrans, trBtnRot, trBtnAniso, trBtnScale, trClose;
    AGE             trTarget        = null;
    
    boolean acceptChanges = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public GroupEditor(KingMain kmain, Frame ownerWindow)
    {
        kMain = kmain;
        this.ownerWindow = ownerWindow;
        
        makeKinemageDialog();
        makeGroupDialog();
        makeSubgroupDialog();
        makeListDialog();
        makeTransformDialog();
    }
//}}}

//{{{ make___Dialog
//##################################################################################################
    void makeKinemageDialog()
    {
        kiDialog = new JDialog(ownerWindow, "Edit kinemage properties", true);
        kiDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        kiPanel = new GridBagPanel();
        kiDialog.setContentPane(kiPanel);
        
        kiName      = new JTextField(20);
        kiWhiteback = new JCheckBox("@whiteback (Default to white background)");
        kiOnewidth  = new JCheckBox("@onewidth (No depth cueing by line width)");
        kiThinline  = new JCheckBox("@thinline (All lines are thin)");
        kiPerspec   = new JCheckBox("@perspective (Use realistic perspective)");
        kiFlat      = new JCheckBox("@flat (2-D display and navigation)");
        kiListcolor = new JCheckBox("@listcolordominant (Default to list color)");
        
        kiOK = new JButton(new ReflectiveAction("OK", null, this, "onKinemageOK"));
        kiCancel = new JButton(new ReflectiveAction("Cancel", null, this, "onKinemageCancel"));
        kiDialog.getRootPane().setDefaultButton(kiOK);
        
        kiPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        kiPanel.gbc.weightx = 1.0;
        kiPanel.gbc.weighty = 1.0;

        kiPanel.gbc.anchor = GridBagConstraints.WEST;
        kiPanel.add(new JLabel("Kinemage title:"), 0, 0, 2, 1);
        kiPanel.add(kiName,         0,  1, 2, 1);
        kiPanel.add(new JLabel("Preferred display options:"), 0, -1, 2, 1);
        kiPanel.add(kiWhiteback,    0, -1, 2, 1);
        kiPanel.add(kiOnewidth,     0, -1, 2, 1);
        kiPanel.add(kiThinline,     0, -1, 2, 1);
        kiPanel.add(kiPerspec,      0, -1, 2, 1);
        kiPanel.add(kiFlat,         0, -1, 2, 1);
        kiPanel.add(kiListcolor,    0, -1, 2, 1);
        kiPanel.gbc.anchor = GridBagConstraints.CENTER;
        kiPanel.add(kiOK,           0, -1);
        kiPanel.add(kiCancel,       1, -1);
    }

    void makeGroupDialog()
    {
        grDialog = new JDialog(ownerWindow, "Edit group properties", true);
        grDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        grPanel = new GridBagPanel();
        grDialog.setContentPane(grPanel);
        
        grName          = new JTextField(20);
        grIsOff         = new JCheckBox("off (Hide this and all children)");
        grNoButton      = new JCheckBox("nobutton (Don't provide on/off button)");
        grDominant      = new JCheckBox("dominant (Supress all children's buttons)");
        grRecessiveOn   = new JCheckBox("collapsable (Dominant only when off)");
        grAnimate       = new JCheckBox("animate (Include in ANIMATE animation)");
        gr2Animate      = new JCheckBox("2animate (Include in 2ANIMATE animation)");
        
        grOK = new JButton(new ReflectiveAction("OK", null, this, "onGroupOK"));
        grCancel = new JButton(new ReflectiveAction("Cancel", null, this, "onGroupCancel"));
        grDialog.getRootPane().setDefaultButton(grOK);
        
        grPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        grPanel.gbc.weightx = 1.0;
        grPanel.gbc.weighty = 1.0;

        grPanel.gbc.anchor = GridBagConstraints.WEST;
        grPanel.add(new JLabel("Group name/identifier:"), 0, 0, 2, 1);
        grPanel.add(grName,         0,  1, 2, 1);
        grPanel.add(grIsOff,        0, -1, 2, 1);
        grPanel.add(grNoButton,     0, -1, 2, 1);
        grPanel.add(grDominant,     0, -1, 2, 1);
        grPanel.add(grRecessiveOn,  0, -1, 2, 1);
        grPanel.add(grAnimate,      0, -1, 2, 1);
        grPanel.add(gr2Animate,     0, -1, 2, 1);
        grPanel.gbc.anchor = GridBagConstraints.CENTER;
        grPanel.add(grOK,           0, -1);
        grPanel.add(grCancel,       1, -1);
    }

    void makeSubgroupDialog()
    {
        suDialog = new JDialog(ownerWindow, "Edit subgroup properties", true);
        suDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        suPanel = new GridBagPanel();
        suDialog.setContentPane(suPanel);
        
        suName          = new JTextField(20);
        suIsOff         = new JCheckBox("off (Hide this and all children)");
        suNoButton      = new JCheckBox("nobutton (Don't provide on/off button)");
        suDominant      = new JCheckBox("dominant (Supress all children's buttons)");
        suRecessiveOn   = new JCheckBox("collapsable (Dominant only when off)");
        
        suOK = new JButton(new ReflectiveAction("OK", null, this, "onSubgroupOK"));
        suCancel = new JButton(new ReflectiveAction("Cancel", null, this, "onSubgroupCancel"));
        suDialog.getRootPane().setDefaultButton(suOK);
        
        suPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        suPanel.gbc.weightx = 1.0;
        suPanel.gbc.weighty = 1.0;

        suPanel.gbc.anchor = GridBagConstraints.WEST;
        suPanel.add(new JLabel("Subgroup name/identifier:"), 0, 0, 2, 1);
        suPanel.add(suName,         0,  1, 2, 1);
        suPanel.add(suIsOff,        0, -1, 2, 1);
        suPanel.add(suNoButton,     0, -1, 2, 1);
        suPanel.add(suDominant,     0, -1, 2, 1);
        suPanel.add(suRecessiveOn,  0, -1, 2, 1);
        suPanel.gbc.anchor = GridBagConstraints.CENTER;
        suPanel.add(suOK,           0, -1);
        suPanel.add(suCancel,       1, -1);
    }

    void makeListDialog()
    {
        liDialog = new JDialog(ownerWindow, "Edit list properties", true);
        liDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        liPanel = new TablePane();
        liDialog.setContentPane(liPanel);
        
        liName      = new JTextField(20);
        liIsOff     = new JCheckBox("off (Hide this list from view)");
        liNoButton  = new JCheckBox("nobutton (Don't provide on/off button)");
        liNoHilite  = new JCheckBox("nohighlight (No highlight on balls)");
        
        liWidth     = new JTextField(6);
        JLabel widthLabel = new JLabel("Line width:");
        widthLabel.setLabelFor(liWidth);
        liRadius    = new JTextField(6);
        JLabel radiusLabel = new JLabel("Ball radius:");
        radiusLabel.setLabelFor(liRadius);
        liAlpha     = new JTextField(6);
        JLabel alphaLabel = new JLabel("Alpha (0-255):");
        alphaLabel.setLabelFor(liAlpha);
        
        KingPrefs prefs = kMain.getPrefs();
        int patchSize = (prefs==null? 20 : prefs.getInt("colorSwatchSize"));
        liPicker = new ColorPicker(KPaint.BLACK_COLOR, patchSize);
        liPicker.addChangeListener(this);
        
        liOK = new JButton(new ReflectiveAction("OK", null, this, "onListOK"));
        liCancel = new JButton(new ReflectiveAction("Cancel", null, this, "onListCancel"));
        liDialog.getRootPane().setDefaultButton(liOK);
        
        liPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        liPanel.insets(2).hfill(true).vfill(false).top();
        liPanel.startSubtable();
            // most stuff
            liPanel.addCell(new JLabel("List name/identifier:"), 2, 1).newRow();
            liPanel.addCell(liName, 2, 1).newRow();
            liPanel.addCell(liIsOff, 2, 1).newRow();
            liPanel.addCell(liNoButton, 2, 1).newRow();
            liPanel.addCell(liNoHilite, 2, 1).newRow();
            liPanel.addCell(widthLabel).addCell(liWidth).newRow();
            liPanel.addCell(radiusLabel).addCell(liRadius).newRow();
            liPanel.addCell(alphaLabel).addCell(liAlpha).newRow();
        liPanel.endSubtable();
        liPanel.startSubtable(1,2);
            // colors
            liPanel.addCell(new JLabel("List color:"));
            liPanel.newRow();
            liPanel.addCell(liPicker);
        liPanel.endSubtable();
        liPanel.newRow();
        liPanel.bottom().startSubtable();
            // ok and cancel
            liPanel.center();
            liPanel.add(liOK);
            liPanel.add(liCancel);
        liPanel.endSubtable();
    }
//}}}

//{{{ makeTransformDialog, transform, trClearFields
//##################################################################################################
    private void makeTransformDialog()
    {
        trDialog = new JDialog(ownerWindow, "Transform coordinates", true);
        trDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        
        trTransX    = new JTextField(6);
        trTransY    = new JTextField(6);
        trTransZ    = new JTextField(6);
        trRotX      = new JTextField(6);
        trRotY      = new JTextField(6);
        trRotZ      = new JTextField(6);
        trScale     = new JTextField(6);
        trScaleX    = new JTextField(6);
        trScaleY    = new JTextField(6);
        trScaleZ    = new JTextField(6);
        
        trBtnTrans  = new JButton(new ReflectiveAction("Translate", null, this, "onTransformTranslate"));
        trBtnRot    = new JButton(new ReflectiveAction("Rotate", null, this, "onTransformRotate"));
        trBtnAniso  = new JButton(new ReflectiveAction("Stretch", null, this, "onTransformAnisoScale"));
        trBtnScale  = new JButton(new ReflectiveAction("Scale", null, this, "onTransformScale"));
        trClose     = new JButton(new ReflectiveAction("Close", null, this, "onTransformClose"));
        trAboutOrigin = new JCheckBox("Rotate/scale about origin", false);
        
        trPanel = new TablePane2();
        trPanel.insets(2).weights(1,1).memorize();
        trPanel.add(new JLabel("X"));
        trPanel.add(new JLabel("Y"));
        trPanel.add(new JLabel("Z"));
        trPanel.newRow();
        trPanel.add(trTransX);
        trPanel.add(trTransY);
        trPanel.add(trTransZ);
        trPanel.hfill(true).add(trBtnTrans);
        trPanel.newRow();
        trPanel.add(trRotX);
        trPanel.add(trRotY);
        trPanel.add(trRotZ);
        trPanel.hfill(true).add(trBtnRot);
        trPanel.newRow();
        trPanel.add(trScaleX);
        trPanel.add(trScaleY);
        trPanel.add(trScaleZ);
        trPanel.hfill(true).add(trBtnAniso);
        trPanel.newRow();
        trPanel.center().add(new JLabel("Scale factor:"), 2, 1);
        trPanel.add(trScale);
        trPanel.hfill(true).add(trBtnScale);
        trPanel.newRow();
        trPanel.center().add(trAboutOrigin, 4, 1);
        trPanel.newRow();
        trPanel.center().add(trClose, 4, 1);
        
        trDialog.setContentPane(trPanel);
    }
    
    /**
    * Displays a dialog for transforming the selected group, subgroup, or list.
    */
    public void transform(AGE target)
    {
        trTarget = target;
        
        trClearFields();
        
        // Display dialog box
        trDialog.pack();
        if(trFirstShow) { trDialog.setLocationRelativeTo(ownerWindow); trFirstShow = false; }
        trDialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
    }
    
    private void trClearFields()
    {
        trTransX.setText("0.0");
        trTransY.setText("0.0");
        trTransZ.setText("0.0");
        trRotX.setText("0.0");
        trRotY.setText("0.0");
        trRotZ.setText("0.0");
        trScale.setText("1.0");
        trScaleX.setText("1.0");
        trScaleY.setText("1.0");
        trScaleZ.setText("1.0");
    }
//}}}

//{{{ edit___
//##################################################################################################
    /**
    * Edits properties of the specified kinemage.
    * @return true if the specified kinemage was modified in any way
    */
    public boolean editKinemage(Kinemage kinemage)
    {
        // Copy info from kinemage to dialog components
        kiName.setText(kinemage.getName());
        kiName.selectAll();
        kiName.requestFocus();
        kiWhiteback.setSelected(    kinemage.atWhitebackground);
        kiOnewidth.setSelected(     kinemage.atOnewidth);
        kiThinline.setSelected(     kinemage.atThinline);
        kiPerspec.setSelected(      kinemage.atPerspective);
        kiFlat.setSelected(         kinemage.atFlat);
        kiListcolor.setSelected(    kinemage.atListcolordominant);
        
        // Display dialog box
        kiDialog.pack();
        kiDialog.setLocationRelativeTo(ownerWindow);
        kiDialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
        
        // Read info from dialog into kinemage
        if(acceptChanges)
        {
            kinemage.setName(      kiName.getText());
            kinemage.atWhitebackground      = kiWhiteback.isSelected();
            kinemage.atOnewidth             = kiOnewidth.isSelected();
            kinemage.atThinline             = kiThinline.isSelected();
            kinemage.atPerspective          = kiPerspec.isSelected();
            kinemage.atFlat                 = kiFlat.isSelected();
            kinemage.atListcolordominant    = kiListcolor.isSelected();
            markKinModified(kinemage);
            kMain.getMenus().displayMenu.syncCheckboxes();
        }
        
        return acceptChanges;
    }

    /**
    * Edits properties of the specified group.
    * @return true if the specified group was modified in any way
    */
    public boolean editGroup(KGroup group)
    {
        // Copy info from group to dialog components
        grName.setText(group.getName());
        grName.selectAll();
        grName.requestFocus();
        grIsOff.setSelected(        !group.isOn());
        grNoButton.setSelected(     !group.hasButton());
        grDominant.setSelected(     group.isDominant());
        grRecessiveOn.setSelected(  group.isRecessiveOn());
        grAnimate.setSelected(      group.isAnimate());
        gr2Animate.setSelected(     group.is2Animate());
        
        // Display dialog box
        grDialog.pack();
        grDialog.setLocationRelativeTo(ownerWindow);
        grDialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
        
        // Read info from dialog into group
        if(acceptChanges)
        {
            group.setName(          grName.getText());
            group.setOn(            !grIsOff.isSelected());
            group.setHasButton(     !grNoButton.isSelected());
            group.setDominant(      grDominant.isSelected());
            group.setRecessiveOn(   grRecessiveOn.isSelected());
            group.setAnimate(       grAnimate.isSelected());
            group.set2Animate(      gr2Animate.isSelected());
            markKinModified(group);
        }
        
        return acceptChanges;
    }

    /**
    * Edits properties of the specified subgroup.
    * @return true if the specified subgroup was modified in any way
    */
    public boolean editSubgroup(KSubgroup subgroup)
    {
        // Copy info from subgroup to dialog components
        suName.setText(subgroup.getName());
        suName.selectAll();
        suName.requestFocus();
        suIsOff.setSelected(        !subgroup.isOn());
        suNoButton.setSelected(     !subgroup.hasButton());
        suDominant.setSelected(     subgroup.isDominant());
        suRecessiveOn.setSelected(  subgroup.isRecessiveOn());
        
        // Display dialog box
        suDialog.pack();
        suDialog.setLocationRelativeTo(ownerWindow);
        suDialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
        
        // Read info from dialog into subgroup
        if(acceptChanges)
        {
            subgroup.setName(       suName.getText());
            subgroup.setOn(         !suIsOff.isSelected());
            subgroup.setHasButton(  !suNoButton.isSelected());
            subgroup.setDominant(   suDominant.isSelected());
            subgroup.setRecessiveOn(suRecessiveOn.isSelected());
            markKinModified(subgroup);
        }
        
        return acceptChanges;
    }

    /**
    * Edits properties of the specified list.
    * @return true if the specified list was modified in any way
    */
    public boolean editList(KList list)
    {
        // Copy info from list to dialog components
        theKList = list;
        liName.setText(list.getName());
        liName.selectAll();
        liName.requestFocus();
        liIsOff.setSelected(    !list.isOn());
        liNoButton.setSelected( !list.hasButton());
        liNoHilite.setSelected((list.flags & KList.NOHILITE) != 0);
        liWidth.setText(Integer.toString(list.width));
        liRadius.setText(Float.toString(list.radius));
        liAlpha.setText(Integer.toString(list.alpha));
        originalColor = list.getColor();
        liPicker.setBackgroundMode(kMain.getCanvas().getEngine().backgroundMode);
        liPicker.setExtras(kMain.getKinemage().getNewPaintMap().values());
        liPicker.setSelection(originalColor);
        
        // Display dialog box
        liDialog.pack();
        if(liFirstShow) { liDialog.setLocationRelativeTo(ownerWindow); liFirstShow = false; }
        liDialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
        
        // Read info from dialog into list
        if(acceptChanges)
        {
            list.setName(      liName.getText());
            list.setOn(        !liIsOff.isSelected());
            list.setHasButton( !liNoButton.isSelected());
            if(liNoHilite.isSelected()) list.flags |= KList.NOHILITE;
            else                        list.flags &= ~KList.NOHILITE;
            try { list.width = Integer.parseInt(liWidth.getText()); }
            catch(NumberFormatException ex) {}
            if(list.width < 1) list.width = 1;
            if(list.width > 7) list.width = 7;
            try { list.radius = Float.parseFloat(liRadius.getText()); }
            catch(NumberFormatException ex) {}
            try { list.alpha = Integer.parseInt(liAlpha.getText()); }
            catch(NumberFormatException ex) {}
            if(list.alpha < 0)      list.alpha = 0;
            if(list.alpha > 255)    list.alpha = 255;
            // Color is handled as soon as the choice is registered,
            // so we don't need to deal with it here.
            markKinModified(list);
        }
        else
        {
            list.setColor(originalColor);
            kMain.notifyChange(KingMain.EM_DISPLAY);
        }
        theKList = null; // to avoid memory leaks
        
        return acceptChanges;
    }
//}}}

//{{{ on__OK, on__Cancel
//##################################################################################################
    /** Event handler for OK button */
    public void onKinemageOK(ActionEvent ev)
    {
        acceptChanges = true;
        kiDialog.dispose();
    }
    /** Event handler for Cancel button */
    public void onKinemageCancel(ActionEvent ev)
    {
        acceptChanges = false;
        kiDialog.dispose();
    }

    /** Event handler for OK button */
    public void onGroupOK(ActionEvent ev)
    {
        acceptChanges = true;
        grDialog.dispose();
    }
    /** Event handler for Cancel button */
    public void onGroupCancel(ActionEvent ev)
    {
        acceptChanges = false;
        grDialog.dispose();
    }

    /** Event handler for OK button */
    public void onSubgroupOK(ActionEvent ev)
    {
        acceptChanges = true;
        suDialog.dispose();
    }
    /** Event handler for Cancel button */
    public void onSubgroupCancel(ActionEvent ev)
    {
        acceptChanges = false;
        suDialog.dispose();
    }

    /** Event handler for OK button */
    public void onListOK(ActionEvent ev)
    {
        acceptChanges = true;
        liDialog.dispose();
    }
    /** Event handler for Cancel button */
    public void onListCancel(ActionEvent ev)
    {
        acceptChanges = false;
        liDialog.dispose();
    }
//}}}

//{{{ onTransform___
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTransformClose(ActionEvent ev)
    {
        trDialog.dispose();
        trTarget = null; // to avoid memory leaks
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTransformTranslate(ActionEvent ev)
    {
        try
        {
            float x, y, z;
            x = Float.parseFloat(trTransX.getText().trim());
            y = Float.parseFloat(trTransY.getText().trim());
            z = Float.parseFloat(trTransZ.getText().trim());
            
            translate(trTarget, x, y, z);
            markKinModified(trTarget);
        }
        catch(NumberFormatException ex) {}
        
        trClearFields();
        kMain.notifyChange(KingMain.EM_EDIT_FINE);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTransformRotate(ActionEvent ev)
    {
        float[] bounds = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        float x, y, z;
        
        if(trAboutOrigin.isSelected())
        {
            x = y = z = 0;
        }
        else
        {
            trTarget.calcBoundingBox(bounds);
            x = (bounds[3] + bounds[0])/2f;
            y = (bounds[4] + bounds[1])/2f;
            z = (bounds[5] + bounds[2])/2f;
        }
        
        Transform rot = new Transform().likeTranslation(-x, -y, -z);
        Transform tmp = new Transform();
        
        try
        {
            double xd, yd, zd;
            xd = Double.parseDouble(trRotX.getText().trim());
            yd = Double.parseDouble(trRotY.getText().trim());
            zd = Double.parseDouble(trRotZ.getText().trim());
            
            tmp.likeRotation(new Triple(1,0,0), xd);
            rot.append(tmp);
            tmp.likeRotation(new Triple(0,1,0), yd);
            rot.append(tmp);
            tmp.likeRotation(new Triple(0,0,1), zd);
            rot.append(tmp);
            
            tmp.likeTranslation(x, y, z);
            rot.append(tmp);
            
            rotate(trTarget, rot);
            markKinModified(trTarget);
        }
        catch(NumberFormatException ex) {}
        
        trClearFields();
        kMain.notifyChange(KingMain.EM_EDIT_FINE);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTransformScale(ActionEvent ev)
    {
        float[] bounds = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        float x, y, z;
        
        if(trAboutOrigin.isSelected())
        {
            x = y = z = 0;
        }
        else
        {
            trTarget.calcBoundingBox(bounds);
            x = (bounds[3] + bounds[0])/2f;
            y = (bounds[4] + bounds[1])/2f;
            z = (bounds[5] + bounds[2])/2f;
        }
        
        try
        {
            float s;
            s = Float.parseFloat(trScale.getText().trim());
            scale(trTarget, x, y, z, s, s, s);
            markKinModified(trTarget);
        }
        catch(NumberFormatException ex) {}
        
        trClearFields();
        kMain.notifyChange(KingMain.EM_EDIT_FINE);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onTransformAnisoScale(ActionEvent ev)
    {
        float[] bounds = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        float x, y, z;
        
        if(trAboutOrigin.isSelected())
        {
            x = y = z = 0;
        }
        else
        {
            trTarget.calcBoundingBox(bounds);
            x = (bounds[3] + bounds[0])/2f;
            y = (bounds[4] + bounds[1])/2f;
            z = (bounds[5] + bounds[2])/2f;
        }
        
        try
        {
            float sx = Float.parseFloat(trScaleX.getText().trim());
            float sy = Float.parseFloat(trScaleY.getText().trim());
            float sz = Float.parseFloat(trScaleZ.getText().trim());
            scale(trTarget, x, y, z, sx, sy, sz);
            markKinModified(trTarget);
        }
        catch(NumberFormatException ex) {}
        
        trClearFields();
        kMain.notifyChange(KingMain.EM_EDIT_FINE);
    }
//}}}

//{{{ translate, rotate, scale
//##################################################################################################
    /**
    * Permanently alters the base coordinates of all the points
    * beneath the selected AGE.
    */
    public static void translate(AGE target, float x, float y, float z)
    {
        KPoint pt;
        Iterator iter;
        
        if(target instanceof KList)
        {
            for(iter = target.iterator(); iter.hasNext(); )
            {
                pt = (KPoint)iter.next();
                pt.setOrigX(pt.getOrigX() + x);
                pt.setOrigY(pt.getOrigY() + y);
                pt.setOrigZ(pt.getOrigZ() + z);
            }
        }
        else
        {
            for(iter = target.iterator(); iter.hasNext(); )
            {
                translate((AGE)iter.next(), x, y, z);
            }
        }
    }

    /**
    * Permanently alters the base coordinates of all the points
    * beneath the selected AGE.
    */
    public static void rotate(AGE target, Transform r)
    {
        KPoint pt;
        Iterator iter;
        
        if(target instanceof KList)
        {
            Triple proxy = new Triple();
            for(iter = target.iterator(); iter.hasNext(); )
            {
                pt = (KPoint)iter.next();
                proxy.setXYZ(pt.getOrigX(), pt.getOrigY(), pt.getOrigZ());
                r.transform(proxy);
                pt.setOrigX(proxy.getX());
                pt.setOrigY(proxy.getY());
                pt.setOrigZ(proxy.getZ());
            }
        }
        else
        {
            for(iter = target.iterator(); iter.hasNext(); )
            {
                rotate((AGE)iter.next(), r);
            }
        }
    }

    /**
    * Permanently alters the base coordinates of all the points
    * beneath the selected AGE.
    * Points are scaled such that (x,y,z) remains fixed in space
    * and other points expand/contract around it.
    */
    public static void scale(AGE target, float x, float y, float z, float sx, float sy, float sz)
    {
        KPoint pt;
        Iterator iter;
        
        if(target instanceof KList)
        {
            // There's no perfect answer here, but this seems reasonable at least...
            float s = (float) Math.sqrt((sx*sx + sy*sy + sz*sz)/3.0);
            ((KList)target).radius *= s;
            for(iter = target.iterator(); iter.hasNext(); )
            {
                pt = (KPoint)iter.next();
                pt.setOrigX( (pt.getOrigX()-x)*sx + x);
                pt.setOrigY( (pt.getOrigY()-y)*sy + y);
                pt.setOrigZ( (pt.getOrigZ()-z)*sz + z);
                if(pt instanceof BallPoint) ((BallPoint)pt).r0 *= s;
            }
        }
        else
        {
            for(iter = target.iterator(); iter.hasNext(); )
            {
                scale((AGE)iter.next(), x, y, z, sx, sy, sz);
            }
        }
    }
//}}}

//{{{ stateChanged, markKinModified
//##################################################################################################
    public void stateChanged(ChangeEvent ev)
    {
        theKList.setColor( liPicker.getSelection() );
        kMain.notifyChange(KingMain.EM_DISPLAY);
    }
    
    void markKinModified(AGE age)
    {
        Kinemage k = age.getKinemage();
        if(k != null) k.setModified(true);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

