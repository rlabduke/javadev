// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;

import driftwood.r3.*;
import driftwood.gui.*;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.awt.event.*;
import java.lang.Double;

public class KinFudgerTool extends BasicTool {

//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.000");
//}}}

//{{{ Variable definitions
//##############################################################################
    HashMap adjacencyMap;
    HashSet mobilePoints;
    TablePane pane;

    JRadioButton fudgeDistance, fudgeAngle, fudgeDihedral;
    JCheckBox onePointBox;

    AbstractPoint firstClick, secondClick, thirdClick;

//}}}


//{{{ Constructor(s)
//##############################################################################
    public KinFudgerTool(ToolBox tb) {
	super(tb);
	buildGUI();
    }
//}}}


//{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {
        
        //dialog = new JDialog(kMain.getTopWindow(),"Fudge Kins", false);

	fudgeDistance = new JRadioButton("Adjust Distance", true);
	fudgeAngle = new JRadioButton("Adjust Angle", false);
	fudgeDihedral = new JRadioButton("Adjust Dihedral", false);

	ButtonGroup fudgeGroup = new ButtonGroup();
	fudgeGroup.add(fudgeDistance);
	fudgeGroup.add(fudgeAngle);
	fudgeGroup.add(fudgeDihedral);

	JButton exportButton = new JButton(new ReflectiveAction("Export", null, this, "onExport"));
	//exportButton.addActionListener(this);
	
	onePointBox = new JCheckBox("Move One Point");
	
	
	pane = new TablePane();
	pane.newRow();
	pane.add(fudgeDistance);
	pane.add(fudgeAngle);
	pane.add(fudgeDihedral);
	pane.newRow();
	pane.add(onePointBox);
	pane.add(exportButton);

	//dialog.setContentPane(pane);

    }


    public void start() {
	if (kMain.getKinemage() == null) return;
	adjacencyMap = new HashMap();
	//buildAdjacencyList();

	show();
    }

    //public void stop()
    //{
	
    //    hide();
	//System.out.println("parent active?");
	//parent.activateDefaultTool();
    //}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if (p != null) {
	    if (fudgeDistance.isSelected()) {
		if (firstClick != null) {
		    buildAdjacencyList();
		    double orig = (new Triple(firstClick)).distance(p);
		    String ans = askInput("distance?", orig);
		    if (ans != null) {
		    double dist = Double.parseDouble(ans);
			System.out.println("starting to find mobiles");
			mobilityFinder(firstClick,(AbstractPoint) p);
			System.out.println(mobilePoints.size());
			System.out.println("finished finding mobiles");
			translatePoints(firstClick, (AbstractPoint) p, dist);
			System.out.println("finished");
		    }
		    firstClick = null;
		} else {
		    firstClick = (AbstractPoint) p;
		}
	    }
	    if (fudgeAngle.isSelected()) {
		if (firstClick !=null) {
		    if (secondClick != null) {
			buildAdjacencyList();
			double currAngle = Triple.angle(firstClick, secondClick, p);
			String ans = askInput("angle?", currAngle);
			//System.out.println(ans);
			if (ans != null) {
			    double idealAngle = Double.parseDouble(ans);
			    System.out.println("starting to find mobiles");
			    mobilityFinder(secondClick,(AbstractPoint) p);
			    System.out.println("finished finding mobiles");
			    //double currAngle = Triple.angle(firstClick, secondClick, (AbstractPoint) p);
			    rotatePoints(firstClick, secondClick, (AbstractPoint) p, idealAngle);
			    System.out.println("finished");
			}
			firstClick = null;
			secondClick = null;
		    } else {
			secondClick = (AbstractPoint) p;
		    }
		} else {
		    firstClick = (AbstractPoint) p;
		}
	    }
	    if (fudgeDihedral.isSelected()) {
		if (firstClick !=null) {
		    if (secondClick != null) {
			if (thirdClick != null) {
			    buildAdjacencyList();
			    double currAngle = Triple.dihedral(firstClick, secondClick, thirdClick, (AbstractPoint) p);
			    String ans = askInput("dihedral angle?", currAngle);
			    //System.out.println(ans);
			    if (ans != null) {
				double idealAngle = Double.parseDouble(ans);
				//System.out.println("starting to find mobiles");
				if (onePointBox.isSelected()) {
				    mobilityFinder(thirdClick, (AbstractPoint) p);
				} else {
				    mobilityFinder(secondClick, thirdClick);
				}
				//System.out.println("finished finding mobiles");
				//double currAngle = Triple.dihedral(firstClick, secondClick, thirdClick, (AbstractPoint) p);
				rotateDihedral(firstClick, secondClick, thirdClick, (AbstractPoint) p, idealAngle);
				//System.out.println("finished");
			    }
			    firstClick = null;
			    secondClick = null;
			    thirdClick = null;
			} else {
			    thirdClick = (AbstractPoint) p;
			}
		    } else {
			secondClick = (AbstractPoint) p;
		    }
		} else {
		    firstClick = (AbstractPoint) p;
		}
	    }	    
	}
	    
    }

    private String askInput(String f, double orig) {
	String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(), "What is your desired " + f + " (orig value: " + df.format(orig) + ")");
	return choice;
    }

    


    public void buildAdjacencyList() {
	adjacencyMap = new HashMap();
	Kinemage kin = kMain.getKinemage();
	if (kin != null) kin.setModified(true);
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		Iterator groupIters = group.iterator();
		while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    if (sub.isOn()) {
		    while (subIters.hasNext()) {
			KList list = (KList) subIters.next();
			if (list.isOn()) {
			    Iterator listIter = list.iterator();
			    while (listIter.hasNext()) {
				Object next = listIter.next();
				if (next instanceof VectorPoint) {
				    VectorPoint currPoint = (VectorPoint) next;
				    
				    if ((!currPoint.isBreak())&&(currPoint.isOn())) {
					VectorPoint prevPoint = (VectorPoint) currPoint.getPrev();
					addPoints(prevPoint, currPoint);
					addPoints(currPoint, prevPoint);
				    }
				}
			    }
			}
		    }
		    }
		}
	    }
	}
    }
    
    private void addPoints(VectorPoint prev, VectorPoint curr) {
	if (adjacencyMap.containsKey(prev)) {
	    HashSet prevSet = (HashSet) adjacencyMap.get(prev);
	    prevSet.add(curr);
	} else {
	    HashSet prevSet = new HashSet();
	    prevSet.add(curr);
	    adjacencyMap.put(prev, prevSet);
	}
    }

    public void mobilityFinder(AbstractPoint first, AbstractPoint second) {

	    
	Set keys = adjacencyMap.keySet();
	Iterator iter = keys.iterator();
	HashMap colors = new HashMap();
	mobilePoints = new HashSet();
	if (onePointBox.isSelected()) {
	    mobilePoints.add(clonePoint(second));
	    HashSet set = (HashSet) adjacencyMap.get(second);
	    iter = set.iterator();
	    while (iter.hasNext()) {
		AbstractPoint adjPoint = (AbstractPoint) iter.next();
		HashSet adjSet = (HashSet) adjacencyMap.get(adjPoint);
		if (adjSet.size() == 1) {
		    mobilePoints.add(clonePoint(adjPoint));
		}
	    }
	
	} else {
	    while (iter.hasNext()) {
		Object key = iter.next();
		colors.put(key, KPalette.white);
	    }
	    colors.put(second, KPalette.gray);
	    colors.put(first, KPalette.deadblack);
	    LinkedList queue = new LinkedList();
	    queue.addFirst(second);
	    mobilePoints.add(clonePoint(second));
	    while (!queue.isEmpty()) {
		AbstractPoint point = (AbstractPoint) queue.getFirst();
		queue.removeFirst();
		HashSet adjSet = (HashSet) adjacencyMap.get(point);
		Iterator adjIter = adjSet.iterator();
		while (adjIter.hasNext()) {
		    AbstractPoint adjPoint = (AbstractPoint) adjIter.next();
		    if (colors.get(adjPoint).equals(KPalette.white)) {
			colors.put(adjPoint, KPalette.gray);
			mobilePoints.add(clonePoint(adjPoint));
			queue.addLast(adjPoint);
		    }
		}
		colors.put(point, KPalette.deadblack);
	    }
	}
    }

    private Object clonePoint(AbstractPoint point) {
	VectorPoint pointClone = new VectorPoint(null, point.getName(), null);
	pointClone.setX((float) point.getX());
	pointClone.setY((float) point.getY());
	pointClone.setZ((float) point.getZ());
	return pointClone;
    }

    public void translatePoints(AbstractPoint first, AbstractPoint second, double idealDist) {
	System.out.println("translate");
	double realDist = (new Triple(first)).distance(second);
	Triple origVector = new Triple(second.getX() - first.getX(), second.getY() - first.getY(), second.getZ() - first.getZ());
	origVector = origVector.mult(idealDist/realDist).add(first).sub(second);
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		Iterator groupIters = group.iterator();
		while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    while (subIters.hasNext()) {
			KList list = (KList) subIters.next();
			Iterator listIter = list.iterator();
			while (listIter.hasNext()) {
			    AbstractPoint point = (AbstractPoint) listIter.next();
			    if (mobilePoints.contains(point)) {
				//System.out.println("Moving: " + point);
				point.setX(point.getX() + origVector.getX());
				point.setY(point.getY() + origVector.getY());
				point.setZ(point.getZ() + origVector.getZ());  
			    }
			    
			}
		    }
		}
	    }
	}
	kCanvas.repaint();

    }

    public void rotatePoints(AbstractPoint first, AbstractPoint second, AbstractPoint third, double idealAngle) {
        double currAngle = Triple.angle(first, second, third);
	System.out.println(currAngle + ", " + idealAngle);
	Triple vectA = new Triple(first.getX()-second.getX(), first.getY()-second.getY(), first.getZ()-second.getZ());
	Triple vectB = new Triple(third.getX()-second.getX(), third.getY()-second.getY(), third.getZ()-second.getZ());
	Triple normal = vectA.cross(vectB);
	VectorPoint ppoint = new VectorPoint(null, "axis", null);
	ppoint.setXYZ(second.getX(), second.getY(), second.getZ());
	VectorPoint vpoint = new VectorPoint(null, "test", ppoint);
	vpoint.setX(normal.getX()+second.getX());
	vpoint.setY(normal.getY()+second.getY());
	vpoint.setZ(normal.getZ()+second.getZ());
	//drawDebug(ppoint, vpoint);
	Transform rotate = new Transform();
	rotate = rotate.likeRotation(ppoint, vpoint, idealAngle - currAngle);
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		Iterator groupIters = group.iterator();
		while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    while (subIters.hasNext()) {
			KList list = (KList) subIters.next();
			Iterator listIter = list.iterator();
			while (listIter.hasNext()) {
			    AbstractPoint point = (AbstractPoint) listIter.next();
			    if (mobilePoints.contains(point)) {
				rotate.transform(point); 
			    }
			    
			}
		    }
		}
	    }
	}
	kCanvas.repaint();
    }

    public void rotateDihedral(AbstractPoint first, AbstractPoint second, AbstractPoint third, AbstractPoint fourth, double idealAngle) {
	double currAngle = Triple.dihedral(first, second, third, fourth);
	Transform rotate = new Transform();
	rotate = rotate.likeRotation(second, third, idealAngle - currAngle);
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
		Iterator groupIters = group.iterator();
		while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    while (subIters.hasNext()) {
			KList list = (KList) subIters.next();
			Iterator listIter = list.iterator();
			while (listIter.hasNext()) {
			    AbstractPoint point = (AbstractPoint) listIter.next();
			    if (mobilePoints.contains(point)) {
				rotate.transform(point); 
			    }
			    
			}
		    }
		}
	    }
	}
	kCanvas.repaint();
	
    }

    private void drawDebug(AbstractPoint prev, VectorPoint point) {
	Kinemage kin = kMain.getKinemage();
	KGroup group = new KGroup(kin, "test");
	kin.add(group);
	KSubgroup sub = new KSubgroup(group, "test");
	group.add(sub);
	KList list = new KList(sub, "list");
	sub.add(list);
	list.add(prev);
	list.add(point);
    }

    public void onExport(ActionEvent ev) {
	buildAdjacencyList();
	JFileChooser saveChooser = new JFileChooser();
	String currdir = System.getProperty("user.dir");
	if(currdir != null) {
	    saveChooser.setCurrentDirectory(new File(currdir));
	}
	if (saveChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow())) {
	    File f = saveChooser.getSelectedFile();
	    if( !f.exists() ||
                JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                    "This file exists -- do you want to overwrite it?",
                    "Overwrite file?", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION )
            {
                savePDB(f);
            }
	}

    }

    public void savePDB(File f) {
	try {
	    Writer w = new FileWriter(f);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    //Set keys = adjacencyMap.keySet();
	    int i = 1;
	    PointComparator pc = new PointComparator();
	    TreeSet keyTree = new TreeSet(pc);
	    keyTree.addAll(adjacencyMap.keySet());
	    Iterator iter = keyTree.iterator();
	    while (iter.hasNext()) {
		AbstractPoint point = (AbstractPoint) iter.next();
		out.print("ATOM  ");
		//String atomNum = String.valueOf(i);
		//while (atomNum.length()<5) {
		//    atomNum = " " + atomNum;
		//}
		out.print(formatStrings(String.valueOf(i), 5) + " ");
		//out.print(point.getName().toUpperCase().substring(0, 8) + "  " + point.getName().toUpperCase().substring(8) + "     ");
		String atomName = PointComparator.getAtomName(point.getName().toUpperCase());
		if (atomName.equals("UNK ")) {
		    
		}
		out.print(PointComparator.getAtomName(point.getName().toUpperCase()) + " ");
		out.print(PointComparator.getResAA(point.getName().toUpperCase()) + "  ");
		out.print(formatStrings(String.valueOf(PointComparator.getResNumber(point.getName().toUpperCase())), 4) + "    ");
		out.print(formatStrings(df.format(point.getX()), 8));
		out.print(formatStrings(df.format(point.getY()), 8));
		out.print(formatStrings(df.format(point.getZ()), 8));
		out.println("  1.00  0.00");
		i++;
	    }
	    out.flush();
	    w.close();
	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An error occurred while saving the file.",
                "Sorry!", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String formatStrings(String value, int numSpaces) {
	while (value.length() < numSpaces) {
	    value = " " + value;
	}
	return value;
	//if (coord < 0) {
	//    return (df.format(coord));
	//}
	//return " " + (df.format(coord));
    }

	

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return pane; }

    public String getHelpAnchor()
    { return null; }

    public String toString() { return "Fudge Kins"; }
//}}}
}//class	    
