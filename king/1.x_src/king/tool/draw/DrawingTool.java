// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.r3.*;
//}}}
/**
* <code>DrawingTool</code> provides many of the Mage "Draw New" functions.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 26 16:25:47 EST 2004
*/
public class DrawingTool extends BasicTool
{
//{{{ Constants
    static final int AUGER_RADIUS = 40;
//}}}

//{{{ Interface: UndoStep
//##############################################################################
    interface UndoStep
    {
        /** Triggers the undo action */
        public void undo();
    }
//}}}

//{{{ Class: ListChildrenUndo
//##############################################################################
    /*
    * Provides a simple undo mechanism for all the drawing tools.
    * Every time a list is changed, its <code>children</code> field
    * is cloned and saved here. The editing action can then be undone
    * simply by copying this over top of the current <code>children</code>.
    */
    static class ListChildrenUndo implements UndoStep
    {
        KList       list;
        ArrayList   children;
        KPoint      modPoint = null;
        KPoint      modPrev = null;
        
        /* Saves the state of list l */
        public ListChildrenUndo(KList l)
        {
            super();
            this.list = l;
            this.children = new ArrayList(l.children);
        }
        
        /** Saves the state of a point who's value of "prev" was set to null */
        public void savePoint(KPoint p)
        {
            this.modPoint = p;
            this.modPrev = p.getPrev();
        }
        
        /** Triggers the undo action */
        public void undo()
        {
            list.children = this.children;
            if(modPoint != null) modPoint.setPrev(modPrev);
        }
    }
//}}}

//{{{ Class: PointCoordsUndo
//##############################################################################
    /** Allows us to undo moving points, eg when we drag them around. */
    static class PointCoordsUndo implements UndoStep
    {
        KPoint[]    points;
        Triple[]    coords;
        
        public PointCoordsUndo(KPoint[] pts)
        {
            super();
            this.points = (KPoint[]) pts.clone();
            this.coords = new Triple[ points.length ];
            for(int i = 0; i < points.length; i++)
                coords[i] = new Triple(points[i]);
        }

        public void undo()
        {
            for(int i = 0; i < points.length; i++)
                points[i].setXYZ(coords[i].getX(), coords[i].getY(), coords[i].getZ());
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    TablePane2      ui;
    JRadioButton    rbDoNothing, rbRevealList, rbEditGroup, rbEditSubgroup, rbEditList,
                    rbEditPoint, rbPaintPoints, rbMovePoint;
    JRadioButton    rbLineSegment, rbDottedLine, rbArcSegment,
                    rbBalls, rbLabels, rbDots, rbTriangle;
    JRadioButton    rbPunch, rbPrune, rbAuger, rbSphereCrop;
    
    Builder         builder = new Builder();
    KPoint          lineseg1 = null, lineseg2 = null;
    KPoint          triang1 = null, triang2 = null, triang3 = null;
    KPoint          arcseg1 = null, arcseg2 = null, arcseg3 = null;
    GroupEditor     grEditor;
    PointEditor     ptEditor;
    KPoint          draggedPoint = null;
    KPoint[]        allPoints = null;
    
    JComboBox       cmPointPaint;
    JTextField      tfShortenLine;
    JCheckBox       cbLabelIsID;
    JTextField      tfNumDots;
    JTextField      tfArcDegrees, tfArcShorten;
    JCheckBox       cbArcArrowhead;
    JTextField      tfTriangleSize;
    JTextField      tfCropRadius;
    
    /** Use add/removeLast() to enque UndoSteps wrapped in SoftReferences */
    LinkedList      undoStack;
    /** Used by getDrawingList() to create new lists when subgroup is updated */
    int             subgroupCounter = 0;
    /** Used by Auger for doing its marker drawing */
    int             lastAugerX, lastAugerY;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DrawingTool(ToolBox tb)
    {
        super(tb);
        grEditor = new GroupEditor(kMain, kMain.getTopWindow());
        ptEditor = new PointEditor(kMain);
        undoStack = new LinkedList();
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        ButtonGroup buttonGroup = new ButtonGroup();
        
        // Build all the radio buttons for different drawing modes
        rbDoNothing = new JRadioButton("Do nothing (navigate)");
        buttonGroup.add(rbDoNothing);
        rbRevealList = new JRadioButton("Reveal in hierarchy");
        buttonGroup.add(rbRevealList);
        rbEditGroup = new JRadioButton("Edit group props");
        buttonGroup.add(rbEditGroup);
        rbEditSubgroup = new JRadioButton("Edit subgroup props");
        buttonGroup.add(rbEditSubgroup);
        rbEditList = new JRadioButton("Edit list props");
        buttonGroup.add(rbEditList);
        rbEditPoint = new JRadioButton("Edit point props");
        buttonGroup.add(rbEditPoint);
        rbPaintPoints = new JRadioButton("Paint points");
        buttonGroup.add(rbPaintPoints);
        rbMovePoint = new JRadioButton("Move points");
        buttonGroup.add(rbMovePoint);
        
        rbLineSegment = new JRadioButton("Draw line segments");
        buttonGroup.add(rbLineSegment);
        rbDottedLine = new JRadioButton("Draw dotted lines");
        buttonGroup.add(rbDottedLine);
        rbArcSegment = new JRadioButton("Draw curved arc");
        buttonGroup.add(rbArcSegment);
        rbBalls = new JRadioButton("Draw balls");
        buttonGroup.add(rbBalls);
        rbLabels = new JRadioButton("Draw labels");
        buttonGroup.add(rbLabels);
        rbDots = new JRadioButton("Draw dots");
        buttonGroup.add(rbDots);
        rbTriangle = new JRadioButton("Draw triangles");
        buttonGroup.add(rbTriangle);

        rbPunch = new JRadioButton("Punch one point");
        buttonGroup.add(rbPunch);
        rbPrune = new JRadioButton("Prune a polyline");
        buttonGroup.add(rbPrune);
        rbAuger = new JRadioButton("Auger a region");
        buttonGroup.add(rbAuger);
        rbSphereCrop = new JRadioButton("Spherical crop");
        buttonGroup.add(rbSphereCrop);
        
        // Create the extra control panels
        cmPointPaint = new JComboBox(KPalette.getStandardMap().values().toArray());
        cmPointPaint.setSelectedItem(KPalette.green);
        TablePane tpPaintPts = new TablePane();
        tpPaintPts.addCell(new JLabel("Use color:"));
        tpPaintPts.addCell(cmPointPaint);
        FoldingBox fbPaintPts = new FoldingBox(rbPaintPoints, tpPaintPts);
        fbPaintPts.setAutoPack(true);
        fbPaintPts.setIndent(10);

        tfShortenLine = new JTextField("0.0", 6);
        TablePane tpLineSeg = new TablePane();
        tpLineSeg.addCell(new JLabel("Shorten lines by:"));
        tpLineSeg.addCell(tfShortenLine);
        FoldingBox fbLineSeg = new FoldingBox(rbLineSegment, tpLineSeg);
        fbLineSeg.setAutoPack(true);
        fbLineSeg.setIndent(10);
        
        tfNumDots = new JTextField("10", 6);
        TablePane tpDottedLine = new TablePane();
        tpDottedLine.addCell(new JLabel("Number of dots:"));
        tpDottedLine.addCell(tfNumDots);
        FoldingBox fbDottedLine = new FoldingBox(rbDottedLine, tpDottedLine);
        fbDottedLine.setAutoPack(true);
        fbDottedLine.setIndent(10);
        
        tfArcDegrees = new JTextField("120", 6);
        tfArcShorten = new JTextField("0", 6);
        cbArcArrowhead = new JCheckBox("Arrowhead", false);
        TablePane tpArcSegment = new TablePane();
        tpArcSegment.addCell(new JLabel("Curvature (degrees):"));
        tpArcSegment.addCell(tfArcDegrees);
        tpArcSegment.newRow();
        tpArcSegment.addCell(new JLabel("Shorten by (degrees):"));
        tpArcSegment.addCell(tfArcShorten);
        tpArcSegment.newRow();
        tpArcSegment.addCell(cbArcArrowhead);
        FoldingBox fbArcSegment = new FoldingBox(rbArcSegment, tpArcSegment);
        fbArcSegment.setAutoPack(true);
        fbArcSegment.setIndent(10);
        
        cbLabelIsID = new JCheckBox("Use ID of picked point for label", false);
        TablePane tpLabels = new TablePane();
        tpLabels.addCell(cbLabelIsID);
        FoldingBox fbLabels = new FoldingBox(rbLabels, tpLabels);
        fbLabels.setAutoPack(true);
        fbLabels.setIndent(10);
        
        tfTriangleSize = new JTextField("1.0", 6);
        TablePane tpTriangle = new TablePane();
        tpTriangle.addCell(new JLabel("Fractional size:"));
        tpTriangle.addCell(tfTriangleSize);
        FoldingBox fbTriangle = new FoldingBox(rbTriangle, tpTriangle);
        fbTriangle.setAutoPack(true);
        fbTriangle.setIndent(10);
        
        tfCropRadius = new JTextField("10", 6);
        TablePane tpSphereCrop = new TablePane();
        tpSphereCrop.addCell(new JLabel("Crop radius:"));
        tpSphereCrop.addCell(tfCropRadius);
        FoldingBox fbSphereCrop = new FoldingBox(rbSphereCrop, tpSphereCrop);
        fbSphereCrop.setAutoPack(true);
        fbSphereCrop.setIndent(10);
        
        // Choose default drawing tool
        rbEditList.setSelected(true);
        
        // Create the UNDO button, etc
        JButton btnUndo = new JButton(new ReflectiveAction("Undo drawing", null, this, "onUndo"));
        JButton btnNewSubgroup = new JButton(new ReflectiveAction("New subgroup", null, this, "onNewSubgroup"));
        
        // Put the UI together
        ui = new TablePane2();
        ui.hfill(true).vfill(true).insets(0,1,0,1).memorize();
        ui.addCell(rbDoNothing).newRow();
        ui.addCell(rbRevealList).newRow();
        ui.addCell(rbEditGroup).newRow();
        ui.addCell(rbEditSubgroup).newRow();
        ui.addCell(rbEditList).newRow();
        ui.addCell(rbEditPoint).newRow();
        ui.addCell(rbPaintPoints).newRow();
            ui.addCell(fbPaintPts).newRow();
        ui.addCell(rbMovePoint).newRow();
        ui.addCell(ui.strut(0,6)).newRow();
        
        ui.addCell(rbLineSegment).newRow();
            ui.addCell(fbLineSeg).newRow();
        ui.addCell(rbDottedLine).newRow();
            ui.addCell(fbDottedLine).newRow();
        ui.addCell(rbArcSegment).newRow();
            ui.addCell(fbArcSegment).newRow();
        ui.addCell(rbBalls).newRow();
        ui.addCell(rbLabels).newRow();
            ui.addCell(fbLabels).newRow();
        ui.addCell(rbDots).newRow();
        ui.addCell(rbTriangle).newRow();
            ui.addCell(fbTriangle).newRow();
        ui.addCell(ui.strut(0,6)).newRow();
        
        ui.addCell(rbPunch).newRow();
        ui.addCell(rbPrune).newRow();
        ui.addCell(rbAuger).newRow();
        ui.addCell(rbSphereCrop).newRow();
            ui.addCell(fbSphereCrop).newRow();
        ui.addCell(btnNewSubgroup).newRow();
        ui.addCell(btnUndo).newRow();
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
        if(rbDoNothing.isSelected())            return; // don't mark kin as modified
        else if(rbRevealList.isSelected())      doRevealList(x, y, p, ev);
        else if(rbEditGroup.isSelected())       doEditGroup(x, y, p, ev);
        else if(rbEditSubgroup.isSelected())    doEditSubgroup(x, y, p, ev);
        else if(rbEditList.isSelected())        doEditList(x, y, p, ev);
        else if(rbEditPoint.isSelected())       doEditPoint(x, y, p, ev);
        else if(rbPaintPoints.isSelected())     doPaintPoints(x, y, p, ev);
        else if(rbMovePoint.isSelected())       return; // don't mark kin as modified
        else if(rbLineSegment.isSelected())     doLineSegment(x, y, p, ev);
        else if(rbDottedLine.isSelected())      doDottedLine(x, y, p, ev);
        else if(rbArcSegment.isSelected())      doArcSegment(x, y, p, ev);
        else if(rbBalls.isSelected())           doBalls(x, y, p, ev);
        else if(rbLabels.isSelected())          doLabels(x, y, p, ev);
        else if(rbDots.isSelected())            doDots(x, y, p, ev);
        else if(rbTriangle.isSelected())        doTriangle(x, y, p, ev);
        else if(rbPunch.isSelected())           doPunch(x, y, p, ev);
        else if(rbPrune.isSelected())           doPrune(x, y, p, ev);
        else if(rbAuger.isSelected())           doAuger(x, y, p, ev);
        else if(rbSphereCrop.isSelected())      doSphereCrop(x, y, p, ev);
        
        Kinemage k = kMain.getKinemage();
        if(k != null) k.setModified(true);
    }
    
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    { super.click(x, y, p, ev); }
//}}}

//{{{ onUndo
//##############################################################################
    // target of reflection
    public void onUndo(ActionEvent ev)
    {
        if(undoStack.size() < 1) return;
        
        SoftReference ref = (SoftReference) undoStack.removeLast();
        UndoStep step = (UndoStep) ref.get();
        if(step == null) undoStack.clear(); // we should discard any other undos too
        else step.undo();
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }

    // target of reflection
    public void onNewSubgroup(ActionEvent ev)
    {
        // Forces creation of a new subgroup
        // and triggers message to kinemage
        this.getDrawingSubgroup(true);
    }
//}}}

//{{{ getDrawingGroup, getDrawingSubgroup
//##############################################################################
    /** Returns null if no kinemage is loaded */
    protected KGroup getDrawingGroup()
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return null;
        
        KGroup group = (KGroup)kin.metadata.get(this.getClass().getName()+".drawNewGroup");
        if(group == null || group.getKinemage() == null) // signals that it's not bound to a kinemage -- has been deleted
        {
            group = new KGroup(kin, "Drawn objs");
            //group.setDominant(true);
            kin.add(group);
            kin.metadata.put(this.getClass().getName()+".drawNewGroup", group);
            kin.signal.signalKinemage(kin, KinemageSignal.STRUCTURE); // the new alternative to notifyChange()
        }
        return group;
    }

    /** Returns null if no kinemage is loaded */
    protected KSubgroup getDrawingSubgroup(boolean forceCreate)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return null;
        KGroup group = this.getDrawingGroup();
        
        KSubgroup subgroup = (KSubgroup)kin.metadata.get(this.getClass().getName()+".drawNewSubgroup");
        if(subgroup == null || forceCreate || subgroup.getKinemage() == null) // signals that it's not bound to a kinemage -- has been deleted
        {
            subgroupCounter++;
            subgroup = new KSubgroup(group, "Drawn objs "+subgroupCounter);
            subgroup.setDominant(true);
            group.add(subgroup);
            kin.metadata.put(this.getClass().getName()+".drawNewSubgroup", subgroup);
            kin.signal.signalKinemage(kin, KinemageSignal.STRUCTURE); // the new alternative to notifyChange()
        }
        return subgroup;
    }
//}}}

//{{{ getDrawingList
//##############################################################################
    /**
    * @param listType   is one of the KList constants
    * @param id         is an identifier so one can have e.g. multiple vector lists
    * @return null if no kinemage is loaded
    */
    protected KList getDrawingList(String listType, String id)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return null;
        KSubgroup subgroup = this.getDrawingSubgroup(false);
        
        String listName = this.getClass().getName()+".drawNewList."+listType+"."+id+"."+subgroupCounter;
        KList list = (KList)kin.metadata.get(listName);
        if(list == null || list.getKinemage() == null) // signals that it's not bound to a kinemage -- has been deleted
        {
            list = new KList(subgroup, "Drawn "+listType+"s");
            list.setType(listType);
            KPaint[] colors = {KPalette.magenta, KPalette.green, KPalette.gold};
            list.setColor(colors[subgroupCounter % colors.length]);
            subgroup.add(list);
            kin.metadata.put(listName, list);
            kin.signal.signalKinemage(kin, KinemageSignal.STRUCTURE); // the new alternative to notifyChange()
        }
        return list;
    }
//}}}

//{{{ doRevealList, doEditGroup/Subgroup/List/Point
//##############################################################################
    public void doRevealList(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        KList list = (KList)p.getOwner();
        if(list == null) return;
        kMain.getKinTree().reveal(list);
        kMain.getKinTree().show();
    }

    public void doEditGroup(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        KList list = (KList)p.getOwner();
        if(list == null) return;
        KSubgroup subgroup = (KSubgroup)list.getOwner();
        if(subgroup == null) return;
        KGroup group = (KGroup)subgroup.getOwner();
        if(group == null) return;
        if(grEditor.editGroup(group))
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }
    
    public void doEditSubgroup(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        KList list = (KList)p.getOwner();
        if(list == null) return;
        KSubgroup subgroup = (KSubgroup)list.getOwner();
        if(subgroup == null) return;
        if(grEditor.editSubgroup(subgroup))
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }
    
    public void doEditList(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        KList list = (KList)p.getOwner();
        if(list == null) return;
        if(grEditor.editList(list))
            kMain.notifyChange(KingMain.EM_EDIT_GROSS);
    }

    public void doEditPoint(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p != null) ptEditor.editPoint(p);
    }
//}}}

//{{{ doPaintPoints
//##############################################################################
    protected void doPaintPoints(int x, int y, KPoint p, MouseEvent ev)
    {
        Engine engine = kCanvas.getEngine();
        Collection points = engine.pickAll2D(x, y, services.doSuperpick.isSelected(), AUGER_RADIUS);
        
        // Painting can't be undone because so many
        // points following those removed might be modified.
        
        // Remove all the points
        KPaint paintColor = (KPaint) cmPointPaint.getSelectedItem();
        for(Iterator iter = points.iterator(); iter.hasNext(); )
        {
            p = (KPoint) iter.next();
            p.setColor(paintColor);
        }
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }
//}}}

//{{{ doLineSegment
//##############################################################################
    protected void doLineSegment(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        if(lineseg1 == null)
        {
            lineseg1 = p;
        }
        else// if(lineseg2 == null)
        {
            lineseg2 = p;
            KList list = this.getDrawingList(KList.VECTOR, "lineSegment");
            if(list == null) return;
            
            undoStack.addLast(new SoftReference(new ListChildrenUndo(list)));
            
            // Calculate line shortening
            double shorten = 0.0;
            try { shorten = Double.parseDouble(tfShortenLine.getText()); }
            catch(NumberFormatException ex) {}
            double xlen = lineseg1.getOrigX() - lineseg2.getOrigX();
            double ylen = lineseg1.getOrigY() - lineseg2.getOrigY();
            double zlen = lineseg1.getOrigZ() - lineseg2.getOrigZ();
            double len = Math.sqrt(xlen*xlen + ylen*ylen + zlen*zlen);
            double a = 1.0 - shorten/len; // the multiplier used below
            
            VectorPoint v1 = new VectorPoint(list, "drawn", null);
            v1.setOrigX(a*lineseg1.getOrigX() + (1-a)*lineseg2.getOrigX());
            v1.setOrigY(a*lineseg1.getOrigY() + (1-a)*lineseg2.getOrigY());
            v1.setOrigZ(a*lineseg1.getOrigZ() + (1-a)*lineseg2.getOrigZ());
            list.add(v1);
            VectorPoint v2 = new VectorPoint(list, "drawn", v1);
            v2.setOrigX(a*lineseg2.getOrigX() + (1-a)*lineseg1.getOrigX());
            v2.setOrigY(a*lineseg2.getOrigY() + (1-a)*lineseg1.getOrigY());
            v2.setOrigZ(a*lineseg2.getOrigZ() + (1-a)*lineseg1.getOrigZ());
            list.add(v2);
            lineseg1 = lineseg2 = null;
            
            Kinemage kin = kMain.getKinemage();
            if(kin == null) return;
            kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
        }
    }
//}}}

//{{{ doBalls, doLabels
//##############################################################################
    protected void doBalls(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        KList list = this.getDrawingList(KList.BALL, "balls");
        if(list == null) return;
        
        undoStack.addLast(new SoftReference(new ListChildrenUndo(list)));
        
        BallPoint b = new BallPoint(list, "drawn");
        b.setOrigX(p.getOrigX());
        b.setOrigY(p.getOrigY());
        b.setOrigZ(p.getOrigZ());
        list.add(b);
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }

    protected void doLabels(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        KList list = this.getDrawingList(KList.LABEL, "labels");
        if(list == null) return;
        
        Object labelText = p.getName();
        if(!cbLabelIsID.isSelected())
        {
            labelText = JOptionPane.showInputDialog(kMain.getTopWindow(),
                "Enter label text", "Enter label text",
                JOptionPane.QUESTION_MESSAGE,
                null, null, labelText);
            if(labelText == null) return;
        }
        
        undoStack.addLast(new SoftReference(new ListChildrenUndo(list)));
        
        LabelPoint lbl = new LabelPoint(list, labelText.toString());
        lbl.setOrigX(p.getOrigX());
        lbl.setOrigY(p.getOrigY());
        lbl.setOrigZ(p.getOrigZ());
        list.add(lbl);
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }
//}}}

//{{{ doDots
//##############################################################################
    protected void doDots(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        KList list = this.getDrawingList(KList.DOT, "dots");
        if(list == null) return;
        
        undoStack.addLast(new SoftReference(new ListChildrenUndo(list)));
        
        DotPoint dt = new DotPoint(list, "drawn");
        dt.setOrigX(p.getOrigX());
        dt.setOrigY(p.getOrigY());
        dt.setOrigZ(p.getOrigZ());
        list.add(dt);
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }
//}}}

//{{{ doDottedLine
//##############################################################################
    protected void doDottedLine(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        if(lineseg1 == null)
        {
            lineseg1 = p;
        }
        else// if(lineseg2 == null)
        {
            lineseg2 = p;
            KList list = this.getDrawingList(KList.DOT, "dottedLine");
            if(list == null) return;
            
            undoStack.addLast(new SoftReference(new ListChildrenUndo(list)));
            
            // Endpoints and working registers
            Triple p1 = new Triple(lineseg1.getOrigX(), lineseg1.getOrigY(), lineseg1.getOrigZ());
            Triple p2 = new Triple(lineseg2.getOrigX(), lineseg2.getOrigY(), lineseg2.getOrigZ());
            Triple x1 = new Triple(), x2 = new Triple();
            
            // Number of dots
            int nDots = 10;
            try { nDots = Math.max(1, Integer.parseInt(tfNumDots.getText())); }
            catch(NumberFormatException ex) {}
            
            // Draw dots
            for(double i = 1; i <= nDots; i++)
            {
                x1.likeProd(1.0-(i/(nDots+1.0)), p1);
                x2.likeProd(i/(nDots+1.0), p2);
                x1.add(x2);
                DotPoint dt = new DotPoint(list, "drawn");
                dt.setOrigX(x1.getX());
                dt.setOrigY(x1.getY());
                dt.setOrigZ(x1.getZ());
                list.add(dt);
            }
            lineseg1 = lineseg2 = null;
            
            Kinemage kin = kMain.getKinemage();
            if(kin == null) return;
            kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
        }
    }
//}}}

//{{{ doArcSegment
//##############################################################################
    protected void doArcSegment(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        if(arcseg1 == null)         arcseg1 = p; // reference position
        else if(arcseg2 == null)    arcseg2 = p; // arc tail
        else// if(arcseg3 == null)
        {
            arcseg3 = p; // arc head
            KList list = this.getDrawingList(KList.VECTOR, "arcSegment");
            if(list == null) return;
            undoStack.addLast(new SoftReference(new ListChildrenUndo(list)));
            
            //       R
            //
            // P---M-->Q
            //  .  .  .
            //   . . .
            //     C 
            Triple R = new Triple(arcseg1.getOrigX(), arcseg1.getOrigY(), arcseg1.getOrigZ());
            Triple P = new Triple(arcseg2.getOrigX(), arcseg2.getOrigY(), arcseg2.getOrigZ());
            Triple Q = new Triple(arcseg3.getOrigX(), arcseg3.getOrigY(), arcseg3.getOrigZ());
            Triple M = new Triple().likeMidpoint(P, Q);
            double arcDegrees = 90;
            try { arcDegrees = Double.parseDouble(tfArcDegrees.getText()); }
            catch(NumberFormatException ex) {}
            if(arcDegrees < 1) arcDegrees = 1;
            if(arcDegrees > 360) arcDegrees = 360;
            double phi = (arcDegrees < 180 ? arcDegrees : 360 - arcDegrees);
            double distPQ = P.distance(Q);
            double distMQ = distPQ / 2;
            double distMC = distMQ / Math.tan(Math.toRadians(phi/2));
            Triple C = builder.construct4(R, Q, M, distMC, 90, (arcDegrees < 180 ? 180 : 0));
            Triple Cx = new Triple().likeNormal(P, R, Q).add(C);
            // Not good if angle=180 and Q, C, P are colinear!
            //if(arcDegrees < 180)    Cx.likeNormal(Q, C, P).add(C);
            //else                    Cx.likeNormal(P, C, Q).add(C);

            double shortenDegrees = 0;
            try { shortenDegrees = Double.parseDouble(tfArcShorten.getText()); }
            catch(NumberFormatException ex) {}

            Triple rotPoint = new Triple();
            Transform xform = new Transform();
            VectorPoint v1 = null, prev = null;
            for(double rotDegrees = shortenDegrees/2; rotDegrees <= (arcDegrees-shortenDegrees/2); rotDegrees+=1.0)
            {
                xform.likeRotation(C, Cx, rotDegrees);
                xform.transform(P, rotPoint);
                prev = v1;
                v1 = new VectorPoint(list, "drawn", prev);
                v1.setOrigX(rotPoint.getX());
                v1.setOrigY(rotPoint.getY());
                v1.setOrigZ(rotPoint.getZ());
                list.add(v1);
            }
            
            // Add arrowheads here
            if(cbArcArrowhead.isSelected())
            {
                Triple arrowBase    = new Triple(prev.getOrigX(), prev.getOrigY(), prev.getOrigZ());
                Triple arrowTip     = new Triple(v1.getOrigX(), v1.getOrigY(), v1.getOrigZ());
                makeArrowhead(R, arrowBase, arrowTip, 0.12*distPQ, 30, 4, list);
            }
            
            arcseg1 = arcseg2 = arcseg3 = null;
            Kinemage kin = kMain.getKinemage();
            if(kin == null) return;
            kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
        }
    }
//}}}

//{{{ makeArrowhead
//##############################################################################
    /**
    * Creates an arrowhead.
    * @param tine       the first tine around the base-tip axis will point toward this point
    * @param base       the base of the arrow body
    * @param tip        the tip of the arrow
    * @param tineLength the length of each tine
    * @param tineAngle  the angle of the tines to the arrow body
    * @param tineCount  how many tines will be created
    * @param list       the list in which to create the tines
    */
    void makeArrowhead(Tuple3 tine, Tuple3 base, Tuple3 tip,
        double tineLength, double tineAngle, int tineCount, KList list)
    {
        for(int i = 0; i < tineCount; i++)
        {
            Triple tineTip = builder.construct4(tine, base, tip, tineLength, tineAngle, (360.0*i)/tineCount);
            VectorPoint v1 = new VectorPoint(list, "drawn", null);
            v1.setOrigXYZ(tip);
            list.add(v1);
            VectorPoint v2 = new VectorPoint(list, "drawn", v1);
            v2.setOrigXYZ(tineTip);
            list.add(v2);
        }
    }
//}}}

//{{{ doTriangle
//##############################################################################
    protected void doTriangle(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        if(triang1 == null)
            triang1 = p;
        else if(triang2 == null)
            triang2 = p;
        else// if(triang3 == null)
        {
            triang3 = p;
            KList list = this.getDrawingList(KList.TRIANGLE, "triangle");
            if(list == null) return;
            
            undoStack.addLast(new SoftReference(new ListChildrenUndo(list)));
            
            // Corner points
            Triple p1 = new Triple(triang1.getOrigX(), triang1.getOrigY(), triang1.getOrigZ());
            Triple p2 = new Triple(triang2.getOrigX(), triang2.getOrigY(), triang2.getOrigZ());
            Triple p3 = new Triple(triang3.getOrigX(), triang3.getOrigY(), triang3.getOrigZ());
            // Center point
            Triple ctr = new Triple().add(p1).add(p2).add(p3).mult(1.0/3.0);
            
            // Amount of shrinkage
            double a = 1.0;
            try { a = Math.abs(Double.parseDouble(tfTriangleSize.getText())); }
            catch(NumberFormatException ex) {}
            
            // Draw triangle
            TrianglePoint t1 = new TrianglePoint(list, "drawn", null);
            t1.setOrigX(a*p1.getX() + (1-a)*ctr.getX());
            t1.setOrigY(a*p1.getY() + (1-a)*ctr.getY());
            t1.setOrigZ(a*p1.getZ() + (1-a)*ctr.getZ());
            list.add(t1);
            TrianglePoint t2 = new TrianglePoint(list, "drawn", t1);
            t2.setOrigX(a*p2.getX() + (1-a)*ctr.getX());
            t2.setOrigY(a*p2.getY() + (1-a)*ctr.getY());
            t2.setOrigZ(a*p2.getZ() + (1-a)*ctr.getZ());
            list.add(t2);
            TrianglePoint t3 = new TrianglePoint(list, "drawn", t2);
            t3.setOrigX(a*p3.getX() + (1-a)*ctr.getX());
            t3.setOrigY(a*p3.getY() + (1-a)*ctr.getY());
            t3.setOrigZ(a*p3.getZ() + (1-a)*ctr.getZ());
            list.add(t3);
            triang1 = triang2 = triang3 = null;
            
            Kinemage kin = kMain.getKinemage();
            if(kin == null) return;
            kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
        }
    }
//}}}

//{{{ doPunch, excisePoint
//##############################################################################
    protected void doPunch(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        KList list = (KList) p.getOwner();
        if(list == null) return;
        
        ListChildrenUndo step = new ListChildrenUndo(list);
        undoStack.addLast(new SoftReference(step));
        excisePoint(p, step);
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }
    
    // Used by Punch and Auger -- not undoable in & of itself
    // Step can be null, or it will be used to save the modified point.
    private void excisePoint(KPoint p, ListChildrenUndo step)
    {
        if(p == null) return;
        KList list = (KList) p.getOwner();
        if(list == null) return;
        
        for(ListIterator iter = list.children.listIterator(); iter.hasNext(); )
        {
            KPoint q = (KPoint) iter.next();
            if(q == p)
            {
                iter.remove();
                if(iter.hasNext())
                {
                    q = (KPoint) iter.next();
                    if(step != null) step.savePoint(q);
                    q.setPrev(null);
                }
                break;
            }//if we found the point
        }//for all points in the list
    }
//}}}

//{{{ doPrune
//##############################################################################
    protected void doPrune(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        
        KList list = (KList) p.getOwner();
        if(list == null) return;
        
        ListChildrenUndo step = new ListChildrenUndo(list);
        undoStack.addLast(new SoftReference(step));
        
        for(ListIterator iter = list.children.listIterator(); iter.hasNext(); )
        {
            KPoint q = (KPoint) iter.next();
            if(q == p)
            {
                iter.remove();
                if(q.getPrev() != null)
                {
                    while(iter.hasPrevious()) // remove preceding points
                    {
                        q = (KPoint) iter.previous();
                        iter.remove();
                        if(q.getPrev() == null) break;
                    }
                }
                while(iter.hasNext()) // remove following points
                {
                    q = (KPoint) iter.next();
                    if(q.getPrev() == null) break;
                    iter.remove();
                }
                break;
            }
        }
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }
//}}}

//{{{ doAuger
//##############################################################################
    protected void doAuger(int x, int y, KPoint p, MouseEvent ev)
    {
        Engine engine = kCanvas.getEngine();
        Collection points = engine.pickAll2D(x, y, services.doSuperpick.isSelected(), AUGER_RADIUS);
        
        // Augering can't be undone because so many
        // points following those removed might be modified.
        
        // Remove all the points
        for(Iterator iter = points.iterator(); iter.hasNext(); )
        {
            p = (KPoint) iter.next();
            excisePoint(p, null);
        }
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }
//}}}

//{{{ doSphereCrop
//##############################################################################
    protected void doSphereCrop(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p == null) return;
        Kinemage kin = p.getKinemage();
        if(kin == null) return;
        
        double r = 10;
        try { r = Double.parseDouble(tfCropRadius.getText()); }
        catch(NumberFormatException ex) {}
        double r2 = r*r;

        // Cropping can't be undone because so many
        // points following those removed might be modified.

        // Find all the points THAT ARE CURRENTLY VISIBLE
        // and outside the cropping sphere.
        RecursivePointIterator rpi = new RecursivePointIterator(kin, false, true); // inc. unpickables
        ArrayList toRemove = new ArrayList();
        while(rpi.hasNext())
        {
            KPoint q = rpi.next();
            double dx = p.getOrigX() - q.getOrigX();
            double dy = p.getOrigY() - q.getOrigY();
            double dz = p.getOrigZ() - q.getOrigZ();
            if(dx*dx + dy*dy + dz*dz > r2 && q.isTotallyOn())
                toRemove.add(q);
        }
        
        // Now remove them
        for(Iterator iter = toRemove.iterator(); iter.hasNext(); )
            excisePoint( (KPoint)iter.next(), null );
        
        kin.signal.signalKinemage(kin, KinemageSignal.APPEARANCE);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ xx_drag() functions
//##################################################################################################
    /** Override this function for (left-button) drags */
    public void drag(int dx, int dy, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(rbMovePoint.isSelected() && v != null && allPoints != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            for(int k = 0; k < allPoints.length; k++)
            {
                float[] offset = v.translateRotated(dx, -dy, 0, Math.min(dim.width, dim.height));
                
                // Check to make sure this isn't just a SpherePoint disk:
                if(allPoints[k] instanceof ProxyPoint) continue;
                
                allPoints[k].setOrigX(allPoints[k].getOrigX() + offset[0]);
                allPoints[k].setOrigY(allPoints[k].getOrigY() + offset[1]);
                allPoints[k].setOrigZ(allPoints[k].getOrigZ() + offset[2]);
            }

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
        else super.drag(dx, dy, ev);
    }

    /** Override this function for middle-button/control drags */
    public void c_drag(int dx, int dy, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(rbMovePoint.isSelected() && v != null && draggedPoint != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            float[] offset = v.translateRotated(dx, -dy, 0, Math.min(dim.width, dim.height));
            draggedPoint.setOrigX(draggedPoint.getOrigX() + offset[0]);
            draggedPoint.setOrigY(draggedPoint.getOrigY() + offset[1]);
            draggedPoint.setOrigZ(draggedPoint.getOrigZ() + offset[2]);

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
        else super.c_drag(dx, dy, ev);
    }
//}}}

//{{{ xx_wheel() functions
//##################################################################################################
    /** Override this function for mouse wheel motion */
    public void wheel(int rotation, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(rbMovePoint.isSelected() && v != null && draggedPoint != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            for(int k = 0; k < allPoints.length; k++)
            {
                float[] offset = v.translateRotated(0, 0, 6*rotation, Math.min(dim.width, dim.height));
                allPoints[k].setOrigX(allPoints[k].getOrigX() + offset[0]);
                allPoints[k].setOrigY(allPoints[k].getOrigY() + offset[1]);
                allPoints[k].setOrigZ(allPoints[k].getOrigZ() + offset[2]);
            }

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
        else super.wheel(rotation, ev);
    }

    /** Override this function for mouse wheel motion with control down */
    public void c_wheel(int rotation, MouseEvent ev)
    {
        KingView v = kMain.getView();
        if(rbMovePoint.isSelected() && v != null && draggedPoint != null)
        {
            Dimension dim = kCanvas.getCanvasSize();
            float[] offset = v.translateRotated(0, 0, 6*rotation, Math.min(dim.width, dim.height));
            draggedPoint.setOrigX(draggedPoint.getOrigX() + offset[0]);
            draggedPoint.setOrigY(draggedPoint.getOrigY() + offset[1]);
            draggedPoint.setOrigZ(draggedPoint.getOrigZ() + offset[2]);

            Kinemage k = kMain.getKinemage();
            if(k != null) k.setModified(true);
            kCanvas.repaint();
        }
        else super.c_wheel(rotation, ev);
    }
//}}}

//{{{ mousePressed, mouseReleased
//##################################################################################################
    public void mousePressed(MouseEvent ev)
    {
        super.mousePressed(ev);
        if(kMain.getKinemage() != null)
            draggedPoint = kCanvas.getEngine().pickPoint(ev.getX(), ev.getY(), services.doSuperpick.isSelected());
        else draggedPoint = null;
        // Otherwise, we just create a nonsensical warning message about stereo picking
        
        if(draggedPoint == null)
            allPoints = null;
        else if(draggedPoint instanceof LabelPoint)
        {
            // Labels should never drag other points with them!
            allPoints = new KPoint[] {draggedPoint};
        }
        else
        {
            // The 0.5 allows for a little roundoff error,
            // both in the kinemage itself and our floating point numbers.
            Collection all = kCanvas.getEngine().pickAll3D(
                draggedPoint.getDrawX(), draggedPoint.getDrawY(), draggedPoint.getDrawZ(),
                services.doSuperpick.isSelected(), 0.5);
            allPoints = (KPoint[])all.toArray( new KPoint[all.size()] );
        }
        
        if(allPoints != null && rbMovePoint.isSelected())
            undoStack.addLast(new SoftReference(new PointCoordsUndo(allPoints)));
    }
    
    public void mouseReleased(MouseEvent ev)
    {
        // Let's keep the point around so we can Z-translate too
        //draggedPoint = null;
    }
//}}}

//{{{ mouseMoved/Dragged/Exited, needAugerCircle, overpaintCanvas
//##################################################################################################
    public void mouseMoved(MouseEvent ev)
    {
        super.mouseMoved(ev);
        lastAugerX = ev.getX();
        lastAugerY = ev.getY();
        if(needAugerCircle()) // trigger a redraw
            kCanvas.repaint();
    }

    public void mouseDragged(MouseEvent ev)
    {
        super.mouseDragged(ev);
        lastAugerX = ev.getX();
        lastAugerY = ev.getY();
        // repaint will occur anyway
    }

    public void mouseExited(MouseEvent ev)
    {
        super.mouseExited(ev);
        // Stop painting the marker at all when mouse leaves drawing area
        lastAugerX = lastAugerY = -1;
        if(needAugerCircle()) // trigger a redraw
            kCanvas.repaint();
    }
    
    /** Do we need to see the circle that marks area of effect for Auger and similar tools? */
    boolean needAugerCircle()
    { return rbAuger.isSelected() || rbPaintPoints.isSelected(); }
    
    /**
    * Called by KinCanvas after all kinemage painting is complete,
    * this gives the tools a chance to write additional info
    * (e.g., point IDs) to the graphics area.
    * <p>We use it as an indication that the canvas has just been
    * redrawn, so we may need to paint the area of effect for Auger.
    * @param painter    the Painter that can paint on the current canvas
    */
    public void overpaintCanvas(Painter painter)
    {
        if(lastAugerX < 0 || lastAugerY < 0) return;
        
        if(needAugerCircle())
        {
            double diam = AUGER_RADIUS * 2;
            painter.drawOval(new Color(0xcc0000), lastAugerX, lastAugerY, 0, diam, diam);
        }
    }
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return ui; }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this tool. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#navigate-tool" (or null)
    */
    public String getHelpAnchor()
    { return "#drawnew-tool"; }
    
    public String toString() { return "Edit / draw / delete"; }
//}}}
}//class

