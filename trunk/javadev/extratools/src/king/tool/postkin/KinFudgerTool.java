// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.points.*;
import king.tool.util.*;

import driftwood.r3.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
import java.net.*;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.awt.event.*;
import java.lang.Double;
//}}}
/**
* <code>KinFudgerTool</code> allows a user to easily modify kinemages.  Users can shorten 
* or lengthen a bond, change bond angles, or dihedrals in the middle of a structure.  Users
* can also left click on a structure and drag to rotate a structure around.  Right click 
* and drag allows translation of structures.
* <p>Copyright (C) 2002-2007 by Vincent B. Chen. All rights reserved.
**/
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

    AbstractPoint draggedPoint = null;

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
	//}}}


    public void start() {
	if (kMain.getKinemage() == null) return;
	adjacencyMap = new HashMap();
	//buildAdjacencyList();

	show();
    }

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
//}}}

    private String askInput(String f, double orig) {
	String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(), "What is your desired " + f + " (orig value: " + df.format(orig) + ")");
	return choice;
    }
	
//{{{ mousePressed
    public void mousePressed(MouseEvent ev)
    {
        super.mousePressed(ev);
	//AbstractPoint draggedPoint = null;
        if(kMain.getKinemage() != null) {
	    buildAdjacencyList();
            draggedPoint = (AbstractPoint) kCanvas.getEngine().pickPoint(ev.getX(), ev.getY(), services.doSuperpick.isSelected());
	}
        else {draggedPoint = null;}
        // Otherwise, we just create a nonsensical warning message about stereo picking
        
        if(draggedPoint == null)
            mobilePoints = new HashSet();
        else
        {
	    mobilityFinder(null, draggedPoint);
        }
    }    
//}}}

//{{{ drag
//##################################################################################################
  /** Override this function for (left-button) drags */
  public void drag(int dx, int dy, MouseEvent ev) {
    //System.out.println(dx + ";" + dy);
    KView v = kMain.getView();
    if(v != null && draggedPoint != null) {
      
	    Dimension dim = kCanvas.getCanvasSize();
      
	    float[] xVector = v.translateRotated(1, 0, 0, Math.min(dim.width, dim.height));
	    float[] yVector = v.translateRotated(0, -1, 0, Math.min(dim.width, dim.height));
      
	    VectorPoint startAxis = new VectorPoint("start", null);
	    startAxis.setXYZ(draggedPoint.getX(), draggedPoint.getY(), draggedPoint.getZ());
	    VectorPoint xAxis = new VectorPoint("xAxis", startAxis);
	    xAxis.setXYZ(draggedPoint.getX() + xVector[0], draggedPoint.getY() + xVector[1], draggedPoint.getZ() + xVector[2]);
	    VectorPoint yAxis = new VectorPoint("yAxis", startAxis);
	    yAxis.setXYZ(draggedPoint.getX() + yVector[0], draggedPoint.getY() + yVector[1], draggedPoint.getZ() + yVector[2]);
      
	    float xRotAmount = ((float)(2.0*Math.PI) * dx / 3f);
	    Transform xRotate = new Transform();
	    xRotate = xRotate.likeRotation(draggedPoint, yAxis, xRotAmount);
      
	    float yRotAmount = ((float)(2.0*Math.PI) * dy / 3f);
	    Transform yRotate = new Transform();
	    yRotate = yRotate.likeRotation(draggedPoint, xAxis, yRotAmount);
	    //System.out.println("new axis calced");
	    
	    HashSet tempSet = new HashSet();  // for storing new, moved coords temporarily for mobilePoints
	    Kinemage kin = kMain.getKinemage();
	    Iterator iter = kin.iterator();
	    while (iter.hasNext()) {
        KGroup group = (KGroup) iter.next();
        if (group.isOn()) {
          KIterator<KPoint> groupIter = KIterator.allPoints(group);
          for (KPoint pt : groupIter) {
            /*
            Iterator groupIters = group.iterator();
            while (groupIters.hasNext()) {
              KSubgroup sub = (KSubgroup) groupIters.next();
              Iterator subIters = sub.iterator();
              while (subIters.hasNext()) {
                KList list = (KList) subIters.next();
                Iterator listIter = list.iterator();
                while (listIter.hasNext()) {
                */
                AbstractPoint point = (AbstractPoint) pt;
                if (mobilePoints.contains(point)) {
                  xRotate.transform(point);
                  yRotate.transform(point);
                  tempSet.add(clonePoint(point));
                }
                
              //}
            //}
          }
        }
	    }
	    mobilePoints.clear();
	    mobilePoints.addAll(tempSet);
      
    }
    else super.drag(dx, dy, ev);
    
  }
  //}}}
  
//{{{ s_drag
  /** Override this function for right-button/shift drags */
  public void s_drag(int dx, int dy, MouseEvent ev)
  {
    KView v = kMain.getView();
    if(v != null && draggedPoint != null)
    {
      
	    Dimension dim = kCanvas.getCanvasSize();
	    //float[] center = v.getCenter();
	    //float[] offset = v.translateRotated(ev.getX() - dim.width/2, dim.height/2 - ev.getY(), 0, Math.min(dim.width, dim.height));
	    Triple origCoord = new Triple().like(draggedPoint);
	    //System.out.println(origCoord);
	    //origCoord = (new Triple(center[0]+offset[0], center[1]+offset[1], center[2]+offset[2])).sub(origCoord);
	    //System.out.println(origCoord);
	    float[] offset = v.translateRotated(dx, -dy, 0, Math.min(dim.width, dim.height));
	    
	    //draggedPoint.setX(draggedPoint.getX() + origCoord.getX());
	    //draggedPoint.setY(draggedPoint.getY() + origCoord.getY());
	    //draggedPoint.setZ(draggedPoint.getZ() + origCoord.getZ());  
	    draggedPoint.setX(draggedPoint.getX() + offset[0]);
	    draggedPoint.setY(draggedPoint.getY() + offset[1]);
	    draggedPoint.setZ(draggedPoint.getZ() + offset[2]);  
      
	    HashSet tempSet = new HashSet();  // for storing new, moved coords temporarily for mobilePoints
	    Kinemage kin = kMain.getKinemage();
	    Iterator iter = kin.iterator();
	    while (iter.hasNext()) {
        KGroup group = (KGroup) iter.next();
        if (group.isOn()) {
          KIterator<KPoint> groupIter = KIterator.allPoints(group);
          for (KPoint pt : groupIter) {
            /*
          Iterator groupIters = group.iterator();
          while (groupIters.hasNext()) {
            KSubgroup sub = (KSubgroup) groupIters.next();
            Iterator subIters = sub.iterator();
            while (subIters.hasNext()) {
              KList list = (KList) subIters.next();
              Iterator listIter = list.iterator();
              while (listIter.hasNext()) {
                */
                AbstractPoint point = (AbstractPoint) pt;
                if (mobilePoints.contains(point)) {
                  //System.out.println("Moving: " + point);
                  //mobilePoints.remove(point);
                  //point.setX(point.getX() + origCoord.getX());
                  //point.setY(point.getY() + origCoord.getY());
                  //point.setZ(point.getZ() + origCoord.getZ());  
                  point.setX(point.getX() + offset[0]);
                  point.setY(point.getY() + offset[1]);
                  point.setZ(point.getZ() + offset[2]);
                  tempSet.add(clonePoint(point));
                }
                
              //}
            //}
          }
        }
	    }
	    mobilePoints.clear();
	    mobilePoints.addAll(tempSet);
      
    }
    else super.s_drag(dx, dy, ev);
  }
//}}}


  public void buildAdjacencyList() {
    adjacencyMap = new HashMap();
    Kinemage kin = kMain.getKinemage();
    if (kin != null) kin.setModified(true);
    KIterator<KPoint> iter = KIterator.allPoints(kin);
    for (KPoint point : iter) {
      if (point instanceof VectorPoint) {
        VectorPoint currPoint = (VectorPoint) point;
        if ((!currPoint.isBreak())&&(currPoint.isOn())) {
          VectorPoint prevPoint = (VectorPoint) currPoint.getPrev();
          addPoints(prevPoint, currPoint);
          addPoints(currPoint, prevPoint);
        }
      }
    }
  }
   /*     
    Iterator iter = kin.iterator();
    while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
      Iterator groupIters = group.iterator();
      while (groupIters.hasNext()) {
        KSubgroup sub = (KSubgroup) groupIters.next();
        Iterator subIters = sub.iterator();
        while (subIters.hasNext()) {
          KList list = (KList) subIters.next();
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
              //}
          }
			    //}
        }
      }
      //}
    }
  }*/
    
//{{{ addPoints
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
//}}}

//{{{ mobilityFinder
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
//}}}
    
    private Object clonePoint(AbstractPoint point) {
	VectorPoint pointClone = new VectorPoint(point.getName(), null);
	pointClone.setX((float) point.getX());
	pointClone.setY((float) point.getY());
	pointClone.setZ((float) point.getZ());
	return pointClone;
    }

//{{{ translocatePoints
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
        KIterator<KPoint> groupIter = KIterator.allPoints(group);
        for (KPoint pt : groupIter) {
          /*
        Iterator groupIters = group.iterator();
        while (groupIters.hasNext()) {
          KSubgroup sub = (KSubgroup) groupIters.next();
          Iterator subIters = sub.iterator();
          while (subIters.hasNext()) {
            KList list = (KList) subIters.next();
            Iterator listIter = list.iterator();
            while (listIter.hasNext()) {*/
              AbstractPoint point = (AbstractPoint) pt;
              if (mobilePoints.contains(point)) {
                //System.out.println("Moving: " + point);
                point.setX(point.getX() + origVector.getX());
                point.setY(point.getY() + origVector.getY());
                point.setZ(point.getZ() + origVector.getZ());  
              }
              
            //}
          //}
        }
	    }
    }
  }
//}}}

//{{{ rotatePoints
        public void rotatePoints(AbstractPoint first, AbstractPoint second, AbstractPoint third, double idealAngle) {
        double currAngle = Triple.angle(first, second, third);
	System.out.println(currAngle + ", " + idealAngle);
	Triple vectA = new Triple(first.getX()-second.getX(), first.getY()-second.getY(), first.getZ()-second.getZ());
	Triple vectB = new Triple(third.getX()-second.getX(), third.getY()-second.getY(), third.getZ()-second.getZ());
	Triple normal = vectA.cross(vectB);
	VectorPoint ppoint = new VectorPoint("axis", null);
	ppoint.setXYZ(second.getX(), second.getY(), second.getZ());
	VectorPoint vpoint = new VectorPoint("test", ppoint);
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
      KIterator<KPoint> groupIter = KIterator.allPoints(group);
      for (KPoint pt : groupIter) {
        /*
      Iterator groupIters = group.iterator();
      while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    while (subIters.hasNext()) {
          KList list = (KList) subIters.next();
          Iterator listIter = list.iterator();
          while (listIter.hasNext()) {*/
            AbstractPoint point = (AbstractPoint) pt;
            if (mobilePoints.contains(point)) {
              rotate.transform(point); 
            }
            
          //}
		    //}
      }
    }
	}
    }
//}}}

//{{{ rotateDihedral
public void rotateDihedral(AbstractPoint first, AbstractPoint second, AbstractPoint third, AbstractPoint fourth, double idealAngle) {
	double currAngle = Triple.dihedral(first, second, third, fourth);
	Transform rotate = new Transform();
	rotate = rotate.likeRotation(second, third, idealAngle - currAngle);
	Kinemage kin = kMain.getKinemage();
	Iterator iter = kin.iterator();
	while (iter.hasNext()) {
	    KGroup group = (KGroup) iter.next();
	    if (group.isOn()) {
        KIterator<KPoint> groupIter = KIterator.allPoints(group);
        for (KPoint pt : groupIter) {
          /*
		Iterator groupIters = group.iterator();
		while (groupIters.hasNext()) {
		    KSubgroup sub = (KSubgroup) groupIters.next();
		    Iterator subIters = sub.iterator();
		    while (subIters.hasNext()) {
			KList list = (KList) subIters.next();
			Iterator listIter = list.iterator();
			while (listIter.hasNext()) {*/
			    AbstractPoint point = (AbstractPoint) pt;
			    if (mobilePoints.contains(point)) {
				rotate.transform(point); 
			    }
			    
			//}
		    //}
		}
	    }
	}
	
    }
//}}}

    private void drawDebug(AbstractPoint prev, VectorPoint point) {
	Kinemage kin = kMain.getKinemage();
	KGroup group = new KGroup("test");
	kin.add(group);
	KGroup sub = new KGroup("test");
	group.add(sub);
	KList list = new KList(KList.VECTOR, "list");
	sub.add(list);
	list.add(prev);
	list.add(point);
    }

//{{{ onExport
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
//}}}


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
		out.print(KinUtil.getResAA(point.getName().toUpperCase()) + "  ");
		out.print(formatStrings(String.valueOf(KinUtil.getResNumber(point.getName().toUpperCase())), 4) + "    ");
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

    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL     url     = getClass().getResource("/extratools/tools-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else return null;
    }

    public String getHelpAnchor()
    { return "#kinfudger-tool"; }

    public String toString() { return "Fudge Kins"; }
//}}}
}//class	    
