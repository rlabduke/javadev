// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;
import king.*;
import king.core.*;
import king.tool.postkin.ConnectivityFinder;
//import king.tool.docking.DockLsqTool;

import java.util.*;
import java.text.DecimalFormat;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.gui.*;

public class RMSDTool extends BasicTool {

//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.000");
//}}}

//{{{ CLASS: PointKeeper
//##############################################################################
    class PointKeeper implements ActionListener
    {
        public JList        pointList;
        public ArrayList    tupleList;
        public JButton      btnClear, btnRemove;
        public KList        markList;
        
        DefaultListModel    listModel;
        
        public PointKeeper(KPaint paint)
        {
            tupleList = new ArrayList();
            listModel = new DefaultListModel();
            pointList = new JList(listModel);
            pointList.setVisibleRowCount(3);
            btnClear = new JButton("Clear");
            btnClear.addActionListener(this);
            btnRemove = new JButton("Remove last");
            btnRemove.addActionListener(this);
            markList = new KList(KList.LABEL);
            markList.setColor(paint);
        }
        
        public int count()
        { return tupleList.size(); }
        
        public void add(String tag, Tuple3 t)
        {
            tupleList.add(t);
            listModel.addElement(tag);
            LabelPoint label = new LabelPoint(markList, Integer.toString(count()));
            label.setX(t.getX());
            label.setY(t.getY());
            label.setZ(t.getZ());
            label.setUnpickable(true);
            markList.add(label);
            syncCalcButton();
        }
        
        public void removeLast()
        {
            if(tupleList.size() > 0) tupleList.remove(tupleList.size()-1);
            if(listModel.size() > 0) listModel.remove(listModel.size()-1);
            if(markList.children.size() > 0) markList.children.remove(markList.children.size()-1);
            syncCalcButton();
        }
        
        public void actionPerformed(ActionEvent ev)
        {
            if(ev.getSource() == btnClear) clear();
            else if(ev.getSource() == btnRemove) removeLast();
            
            kCanvas.repaint();
        }
        
        public void clear()
        {
            tupleList.clear();
            listModel.clear();
            markList.clear();
            syncCalcButton();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    TablePane       toolpane;
    PointKeeper     pkReference;
    PointKeeper     pkMobile;
    JRadioButton    btnReference, btnMobile;
    JButton         btnCalc;
    JCheckBox keepRefBox;
    HashSet mobilePoints;
    HashMap adjacencyMap;
    AbstractPoint firstClick, secondClick;
    LinkedList refList = new LinkedList();
    ConnectivityFinder connect;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RMSDTool(ToolBox tb)
    {
        super(tb);
	pkReference = new PointKeeper(KPalette.sky);
        pkMobile    = new PointKeeper(KPalette.hotpink);
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
	//super.buildGUI();
        btnCalc = new JButton(new ReflectiveAction("Calculate RMSD", null, this, "onCalc"));
	btnCalc.setEnabled(false);
        
        btnReference    = new JRadioButton("Reference", true);
        btnMobile       = new JRadioButton("Mobile", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(btnReference);
        bg.add(btnMobile);
        
        toolpane = new TablePane();
        toolpane.center();
        toolpane.add(btnReference);
        toolpane.add(pkReference.btnClear);
        toolpane.add(pkReference.btnRemove);
        toolpane.newRow().save().hfill(true).vfill(true);
        toolpane.add(new JScrollPane(pkReference.pointList),3,1);
        toolpane.newRow().restore();
        toolpane.add(btnMobile);
        toolpane.add(pkMobile.btnClear);
        toolpane.add(pkMobile.btnRemove);
        toolpane.newRow().save().hfill(true).vfill(true);
        toolpane.add(new JScrollPane(pkMobile.pointList),3,1);
        toolpane.newRow().restore();
        toolpane.add(btnCalc,3,1);
	//btnDock.setLabel("Dock mobile on reference");
	//keepRefBox = new JCheckBox("Keep reference points", true);
	//toolpane.newRow();
	//toolpane.add(keepRefBox, 3, 1);
    }

    public void start() {
	if (kMain.getKinemage() == null) return;
	connect = new ConnectivityFinder(kMain);
	//adjacencyMap = new HashMap();
	//buildAdjacencyList();

	show();
    }


//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        //super.click(x, y, p, ev);
        services.pick(p);
        
        if(p != null && p.getComment() != null)
            clickActionHandler(p.getComment());

        if(p != null) {
	    if (firstClick != null) {
		connect.buildAdjacencyList(false);
		ArrayList list = connect.pathFinder(firstClick, (AbstractPoint) p);
		//System.out.println(list.size());
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
		    AbstractPoint point = (AbstractPoint) iter.next();
		    //Triple t = new Triple(point);
		    if (btnReference.isSelected()) {
			pkReference.add(point.getName(), point);
		    } else if (btnMobile.isSelected()) {
			pkMobile.add(point.getName(), point);
		    }
		    else {
			JOptionPane.showMessageDialog(kMain.getTopWindow(),
						      "Either 'Reference' or 'Mobile' should be selected.",
						      "Error", JOptionPane.ERROR_MESSAGE);
		    }
		    firstClick = null;
		    kCanvas.repaint();
		}
	    } else {
		firstClick = (AbstractPoint) p;
	    }
	    

        }
    }

//{{{ onCalc
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onCalc(ActionEvent ev)
    {
	//connect.buildAdjacencyList(true);
	//mobilePoints = connect.mobilityFinder((AbstractPoint)pkMobile.tupleList.get(0));
        Tuple3[] ref = (Tuple3[])pkReference.tupleList.toArray(new Tuple3[pkReference.tupleList.size()]);
        Tuple3[] mob = (Tuple3[])pkMobile.tupleList.toArray(new Tuple3[pkMobile.tupleList.size()]);
        
	double sum = 0;
	for (int i = 0; i < ref.length; i++) {
	    Tuple3 refpoint = ref[i];
	    Tuple3 mobpoint = mob[i];
	    sum = sum + Math.sqrt((Math.pow((refpoint.getX() - mobpoint.getX()), 2) + Math.pow((refpoint.getY() - mobpoint.getY()), 2) + Math.pow((refpoint.getZ() - mobpoint.getZ()), 2)));
	}
	double rmsd = Math.sqrt(sum/ref.length);
	System.out.println("The calculated RMSD is: " + df.format(rmsd));
	/*
        SuperPoser poser = new SuperPoser(ref, mob);
        Transform t = poser.superpos();
        
        Kinemage kin = kMain.getKinemage();
        if(!t.isNaN() && kin != null)
        {
            transform(kin, t);
            kin.setModified(true);
        }
        
        // Swap which button is selected
        //if(btnReference.isSelected())   btnMobile.setSelected(true);
        //else                            btnReference.setSelected(true);

        if(!keepRefBox.isSelected()) {
	    pkReference.clear();
	    btnMobile.setSelected(true);
	}
        pkMobile.clear();
        kCanvas.repaint();
	*/
    }

    void syncCalcButton()
    {
        btnCalc.setEnabled(pkReference.count() >= 2 && pkReference.count() == pkMobile.count());
    }

//{{{ transformAllVisible
//##############################################################################
    private void transform(AGE target, Transform t)
    {
        //if(!target.isOn()) return;
        
        if(target instanceof KList)
        {
            Triple proxy = new Triple();
            for(Iterator iter = target.iterator(); iter.hasNext(); )
            {
                KPoint pt = (KPoint)iter.next();
                if(mobilePoints.contains(pt))
                {
                    proxy.setXYZ(pt.getX(), pt.getY(), pt.getZ());
                    t.transform(proxy);
                    pt.setX(proxy.getX());
                    pt.setY(proxy.getY());
                    pt.setZ(proxy.getZ());
                }
            }
        }
        else
        {
            for(Iterator iter = target.iterator(); iter.hasNext(); )
                transform((AGE)iter.next(), t);
        }
    }
//}}}

//{{{ signalTransform
//##################################################################################################
    /**
    * A call to this method indicates the subscriber
    * should transform its coordinates from model-space
    * to display-space and optionally add one or more
    * KPoints to the supplied Engine using addPaintable().
    *
    * <p>This method will be called in response to TransformSignal.signalTransform().
    *
    * @param engine     the Engine object describing the
    *   dimensions and properties of the space to be painted.
    * @param xform      the Transform to apply.
    *   The subscriber must not modify the original Transform it
    *   receives! Subscibers may, however, copy and modify the
    *   Transform(s) they pass to internal substructures.
    */
    public void signalTransform(Engine engine, Transform xform)
    {
        pkReference.markList.signalTransform(engine, xform);
        pkMobile.markList.signalTransform(engine, xform);
    }
//}}}

//{{{ getToolPanel, getHelpURL/Anchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return toolpane; }
    

    public String toString() { return "RMSD Tool"; }

    public String getHelpAnchor() { return null; }

}
