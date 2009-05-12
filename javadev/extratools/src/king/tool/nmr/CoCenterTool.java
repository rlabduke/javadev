// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.core.*;

//import java.io.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.util.*;
//import java.text.*;
//
import driftwood.gui.*;
//import driftwood.r3.*;
//import Jama.*;
//import driftwood.moldb2.*;
import driftwood.util.*;
import king.tool.util.*;
//import chiropraxis.kingtools.*;
//import king.tool.util.*;

//}}}

/**
* <code>CoCenterTool</code> is for cocentering an ensemble of structures on a
* a single point.
* 
* <p>Copyright (C) 2007 by Vincent B. Chen. All rights reserved.
* <br>Begun Wed Nov 07 14:17:42 EST 2007
**/
public class CoCenterTool extends BasicTool {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  TablePane2 pane;
  JButton resetButton;
  JComboBox atomsBox;
  JCheckBox doParensBox;
  JCheckBox doSlideBox;
  ArrayList origCoords = null;
  int current = Integer.MIN_VALUE;
  //int currentParen = Integer.MIN_VALUE;
  ReflectiveAction backAct;
  ReflectiveAction fwdAct;
  
  Timer smoothTimer;
  float xincr;
  float yincr;
  float zincr;
  float[] center;
  int slideCounter;
  //}}}
  
  //{{{ Constructors
  public CoCenterTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ start
  public void start() {
    if (pane == null) {
      buildGUI();
    }
    show();
    pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('j'), "doFwd");
    pane.getActionMap().put("doFwd", fwdAct);
    kMain.getContentPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('j'), "doFwd");
    kMain.getContentPane().getActionMap().put("doFwd", fwdAct);
    pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('J'), "doBack");
    pane.getActionMap().put("doBack", backAct);
    kMain.getContentPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('J'), "doBack");
    kMain.getContentPane().getActionMap().put("doBack", backAct);
    
    smoothTimer = new Timer(20, new ReflectiveAction(null, null, this, "onSmooth"));
    slideCounter = 1;
    // Helpful hint for users:
    this.services.setID("Ctrl-click, option-click, or middle-click a point to co-center");
  }
  
  public void stop()
  {
    onReset(null);
    origCoords = null;
    hide();
  }
  //}}}
  
  //{{{ buildGUI
  public void buildGUI() {
    String[] atoms = {"n", "ca", "c", "o", "p"};
    atomsBox = new JComboBox(atoms);
    resetButton = new JButton(new ReflectiveAction("Reset Coordinates", null, this, "onReset"));
    backAct = new ReflectiveAction(null, kMain.getPrefs().stepBackIcon, this, "onBackward");
    //backAct.setAccelerator(KeyStroke.getKeyStroke('N'));
    JMenuItem backMenu = new JMenuItem(backAct);
    JButton backButton = new JButton(backAct);
    backButton.setToolTipText("Cocenter and center on previous residue");
    fwdAct = new ReflectiveAction(null, kMain.getPrefs().stepForwardIcon, this, "onForward");
    //fwdAct.setAccelerator(KeyStroke.getKeyStroke('n'));
    JMenuItem fwdMenu = new JMenuItem(fwdAct);
    //System.out.println("accel:"+fwdMenu.getAccelerator()+" "+backMenu.getAccelerator());
    JButton fwdButton = new JButton(fwdAct);
    fwdButton.setToolTipText("Cocenter and center on next residue");
    //fwdButton.setMnemonic(KeyEvent.VK_N);
    doParensBox = new JCheckBox("Cocenter on parens");
    doSlideBox = new JCheckBox("Slide to next point");
    
    pane = new TablePane2();
    pane.newRow();
    pane.add(atomsBox);
    pane.add(backButton);
    pane.add(fwdButton);
    pane.newRow();
    pane.add(resetButton, 3, 1);
    pane.newRow();
    pane.add(doParensBox, 3, 1);
    pane.newRow();
    pane.add(doSlideBox, 3, 1);
  }
  //}}}
  
  //{{{ c_click
  //##############################################################################
  /** Override this function for middle-button/control clicks */
  public void c_click(int x, int y, KPoint p, MouseEvent ev)
  {
    if (origCoords == null) {
      origCoords = new ArrayList();
      Iterator groups = kMain.getKinemage().iterator();
      while (groups.hasNext()) {
        KGroup group = (KGroup) groups.next();
        KGroup clone = group.clone(true);
        origCoords.add(clone);
      }
    }
    if(p != null) {
      cocenter(p, kMain.getKinemage());
    }
  }
  //}}}
  
  //{{{ cocenter
  public void cocenter(KPoint p, Kinemage kin) {
    long startTime = System.currentTimeMillis();
    setOrigCoords();
    current = KinUtil.getResNumber(p);
    String pName = p.getName();
    if (pName.matches("\\([0-9]*\\).*")) {
      pName = pName.substring(pName.indexOf(")")+1, pName.length()).trim();
    }
    if (pName.length() > 14) pName = pName.substring(0, 14);
    //System.out.println(pName);
    Iterator iter = kin.iterator();
    while (iter.hasNext()) {
      KGroup group = (KGroup) iter.next();
      KIterator<KPoint> pts = KIterator.allPoints(group);
      boolean foundPt = false;
      double xtrans = Double.NaN;
      double ytrans = Double.NaN;
      double ztrans = Double.NaN;
      while (pts.hasNext() && !foundPt) {
        KPoint test = pts.next();
        String testName = test.getName();
        if (testName.matches("\\([0-9]*\\).*")) {
          testName = testName.substring(testName.indexOf(")")+1, testName.length()).trim();
        }
        if (testName.length() > 14) testName = testName.substring(0, 14);
        if (testName.equals(pName)) {
          foundPt = true;
          xtrans = test.getX()-p.getX();
          ytrans = test.getY()-p.getY();
          ztrans = test.getZ()-p.getZ();
        }
      }
      if (foundPt) {
        pts = KIterator.allPoints(group);
        for (KPoint pt : pts) {
          pt.setX(pt.getX() - xtrans);
          pt.setY(pt.getY() - ytrans);
          pt.setZ(pt.getZ() - ztrans);
        }
      }
    }
    System.out.println("Cocenter took "+(System.currentTimeMillis()-startTime)/1000.0+" seconds");
  }
  //}}}
  
  //{{{ cocenterParens
  /** Cocenters on a number in parens in point ID **/
  public void cocenterParens(KPoint p, Kinemage kin) {
    long startTime = System.currentTimeMillis();
    setOrigCoords();
    String matchParen = getParen(p);
    //System.out.println(matchParen);
    current = Integer.parseInt(matchParen);
    Iterator iter = kin.iterator();
    while (iter.hasNext()) {
      KGroup group = (KGroup) iter.next();
      KIterator<KPoint> pts = KIterator.allPoints(group);
      boolean foundPt = false;
      double xtrans = Double.NaN;
      double ytrans = Double.NaN;
      double ztrans = Double.NaN;
      while (pts.hasNext() && !foundPt) {
        KPoint test = pts.next();
        String testParen = getParen(test);
        if (testParen.equals(matchParen)) {
          foundPt = true;
          xtrans = test.getX()-p.getX();
          ytrans = test.getY()-p.getY();
          ztrans = test.getZ()-p.getZ();
        }
      }
      if (foundPt) {
        pts = KIterator.allPoints(group);
        for (KPoint pt : pts) {
          pt.setX(pt.getX() - xtrans);
          pt.setY(pt.getY() - ytrans);
          pt.setZ(pt.getZ() - ztrans);
        }
      }
    }
    System.out.println("Cocenter took "+(System.currentTimeMillis()-startTime)/1000.0+" seconds");
  }
  //}}}
  
  //{{{ setOrigCoords
  public void setOrigCoords() {
    if (origCoords == null) {
      origCoords = new ArrayList();
      Iterator groups = kMain.getKinemage().iterator();
      while (groups.hasNext()) {
        KGroup group = (KGroup) groups.next();
        KGroup clone = group.clone(true);
        origCoords.add(clone);
      }
    }
  }
  //}}}
  
  //{{{ onReset
  public void onReset(ActionEvent ev) {
    if (origCoords == null) return;
    Kinemage kin = kMain.getKinemage();
    Iterator iter = kin.iterator();
    int i = 0;
    //System.out.println("Testing");
    while (iter.hasNext()) {
      KGroup group = (KGroup) iter.next();
      KIterator<KPoint> points = KIterator.allPoints(group);
      if (points.hasNext()) {
        KPoint p = points.next();
        String pName = p.getName();
        KGroup origGroup = (KGroup) origCoords.get(i);
        KIterator<KPoint> pts = KIterator.allPoints(origGroup);
        boolean foundPt = false;
        double xtrans = Double.NaN;
        double ytrans = Double.NaN;
        double ztrans = Double.NaN;
        while (pts.hasNext() && !foundPt) {
          KPoint test = pts.next();
          String testName = test.getName();
          if (testName.equals(pName)) {
            foundPt = true;
            xtrans = p.getX()-test.getX();
            ytrans = p.getY()-test.getY();
            ztrans = p.getZ()-test.getZ();
            //System.out.println(new Double(test.getX()) +","+ new Double(test.getY()) +","+ new Double(test.getZ()));
          }
        }
        if (foundPt) {
          pts = KIterator.allPoints(group);
          for (KPoint pt : pts) {
            pt.setX(pt.getX() - xtrans);
            pt.setY(pt.getY() - ytrans);
            pt.setZ(pt.getZ() - ztrans);
          }
        }
        
      }
      i++;
    }
  }
  //}}}
  
  //{{{ onForward/Backward
  public void onForward(ActionEvent ev) {
    long startTime = System.currentTimeMillis();
    KPoint next = findNextPoint(doParensBox.isSelected());
    System.out.println("Finding match point took "+(System.currentTimeMillis()-startTime)/1000.0+" seconds");
    coReCenter(next, doParensBox.isSelected());
  }
  
  public void onBackward(ActionEvent ev) {
    long startTime = System.currentTimeMillis();
    KPoint prev = findPrevPoint(doParensBox.isSelected());
    System.out.println("Finding match point took "+(System.currentTimeMillis()-startTime)/1000.0+" seconds");
    coReCenter(prev, doParensBox.isSelected());
    //Kinemage kin = kMain.getKinemage();
    //KIterator<KPoint> points = KIterator.visiblePoints(kin);
    //KPoint point = null;
    //KPoint highPoint = null;
    //int highResNum = Integer.MIN_VALUE;
    //while (point == null && points.hasNext()) {
    //  KPoint testPt = points.next();
    //  String atomName = KinUtil.getAtomName(testPt).toLowerCase();
    //  if (atomName.equals((String)atomsBox.getSelectedItem())) {
    //    int resNum = KinUtil.getResNumber(testPt);
    //    if (resNum == current - 1) {
    //      point = testPt;
    //    } else if (resNum > highResNum) {
    //      highResNum = resNum;
    //      highPoint = testPt;
    //    }
    //  }
    //}
    //if (point != null) {
    //  cocenter(point, kin);
    //  services.pick(point);
    //  services.centerOnPoint(point);
    //} else {
    //  if (highPoint != null) {
    //    cocenter(highPoint, kin);
    //    services.pick(highPoint);
    //    services.centerOnPoint(highPoint);
    //  }
    //}
  }
  //}}}
  
  //{{{ onForwardParen
  //public void forwardParens() {
  //  long startTime = System.currentTimeMillis();
  //  //Kinemage kin = kMain.getKinemage();
  //  //KIterator<KPoint> points = KIterator.visiblePoints(kin);
  //  //KPoint point = null;
  //  //KPoint lowPoint = null;
  //  //int lowResNum = Integer.MAX_VALUE;
  //  //int lowParen = Integer.MAX_VALUE;
  //  //while (points.hasNext()) {
  //  //  KPoint testPt = points.next();
  //  //  String paren = getParen(testPt);
  //  //  if (!paren.equals("")) {
  //  //    int parenNum = Integer.parseInt(paren);
  //  //    //System.out.println(paren);
  //  //    if ((parenNum > currentParen)&&(parenNum < lowParen)) {
  //  //      point = testPt;
  //  //      lowParen = parenNum;
  //  //    } else if (parenNum < lowResNum) {
  //  //      lowResNum = parenNum;
  //  //      lowPoint = testPt;
  //  //    }
  //  //  }
  //  //}
  //  System.out.println("Finding match point took "+(System.currentTimeMillis()-startTime)/1000.0+" seconds");
  //  if (point != null) {
  //    cocenterParens(point, kin);
  //    services.pick(point);
  //    services.centerOnPoint(point);
  //  } else {
  //    if (lowPoint != null) {
  //      cocenterParens(lowPoint, kin);
  //      services.pick(lowPoint);
  //      services.centerOnPoint(lowPoint);
  //    }
  //  }
  //}
  //}}}
  
  //{{{ findNextPoint
  public KPoint findNextPoint(boolean useParens) {
    Kinemage kin = kMain.getKinemage();
    KIterator<KPoint> points = KIterator.visiblePoints(kin);
    KPoint point = null;
    KPoint lowestPoint = null;
    int lowestNum = Integer.MAX_VALUE;
    int nearestNum = Integer.MAX_VALUE;
    while (points.hasNext()) {
      KPoint testPt = points.next();
      int testNum = Integer.MIN_VALUE;
      if (useParens) {
        String paren = getParen(testPt);
        if (!paren.equals("")) testNum = Integer.parseInt(paren);
      } else {
        String atomName = KinUtil.getAtomName(testPt).toLowerCase();
        if (atomName.equals((String)atomsBox.getSelectedItem())) {
          testNum = KinUtil.getResNumber(testPt);
        }
      }
      if (testNum != Integer.MIN_VALUE) {
        if (testNum == current + 1) return testPt;
        if ((testNum > current)&&(testNum < nearestNum)) {
          point = testPt;
          nearestNum = testNum;
        } else if (testNum < lowestNum) {
          lowestNum = testNum;
          lowestPoint = testPt;
        }
      }
    }
    if (point != null) return point;
    return lowestPoint;
  }
  //}}}

  //{{{ findPrevPoint
  public KPoint findPrevPoint(boolean useParens) {
    Kinemage kin = kMain.getKinemage();
    KIterator<KPoint> points = KIterator.visiblePoints(kin);
    KPoint point = null;
    KPoint highestPoint = null;
    int highestNum = Integer.MIN_VALUE;
    int nearestNum = Integer.MIN_VALUE;
    while (points.hasNext()) {
      KPoint testPt = points.next();
      int testNum = Integer.MAX_VALUE;
      if (useParens) {
        String paren = getParen(testPt);
        if (!paren.equals("")) testNum = Integer.parseInt(paren);
      } else {
        String atomName = KinUtil.getAtomName(testPt).toLowerCase();
        if (atomName.equals((String)atomsBox.getSelectedItem())) {
          testNum = KinUtil.getResNumber(testPt);
        }
      }
      if (testNum != Integer.MAX_VALUE) {
        if (testNum == current - 1) return testPt;
        if ((testNum < current)&&(testNum > nearestNum)) {
          point = testPt;
          nearestNum = testNum;
        } else if (testNum > highestNum) {
          highestNum = testNum;
          highestPoint = testPt;
        }
      }
    }
    if (point != null) return point;
    return highestPoint;
  }
  //}}}
  
  //{{{ coReCenter
  public void coReCenter(KPoint point, boolean doParens) {
    Kinemage kin = kMain.getKinemage();
    if (point != null) {
      if (doSlideBox.isSelected()) smoothCenter(point);
      else                         services.centerOnPoint(point);
      if (doParens)  cocenterParens(point, kin);
      else           cocenter(point, kin);
      services.pick(point);
      //services.centerOnPoint(point);

    }
  }
  //}}}
  
  //{{{ smoothCenter
  public void smoothCenter(KPoint point) {
    KView view = kMain.getView();
    center = view.getCenter();
    //for (float f : center) System.out.println(f);
    float xdiff = (float) point.getX() - center[0];
    float ydiff = (float) point.getY() - center[1];
    float zdiff = (float) point.getZ() - center[2];
    xincr = xdiff / 30;
    yincr = ydiff / 30;
    zincr = zdiff / 30;
    smoothTimer.start();
    //for (int i = 1; i <= 20; i++) {
    //  view.setCenter(center[0]+xincr*i, center[1]+yincr*i, center[2]+zincr*i);
    //}
  }
  
  public void onSmooth(ActionEvent ev) {
    KView view = kMain.getView();
    //for (float f : center) System.out.print(f+" ");
    //System.out.println();
    view.setCenter(center[0]+(float)(xincr*15*(Math.sin(Math.PI*slideCounter/30-Math.PI/2)+1)), center[1]+(float)(yincr*15*(Math.sin(Math.PI*slideCounter/30-Math.PI/2)+1)), center[2]+(float)(zincr*15*(Math.sin(Math.PI*slideCounter/30-Math.PI/2)+1)));
    slideCounter++;
    if (slideCounter > 30) {
      //for (float f : view.getCenter()) System.out.println(f);
      slideCounter = 1;
      smoothTimer.stop();
    }
  }
  //}}}
  
  //{{{ getParen
  public String getParen(KPoint p) {
    String name = p.getName();
    if (name.matches("\\([0-9]*\\).*")) {
      String[] parsed = Strings.explode(p.getName(), " ".charAt(0), false, true);
      //String matchParen = "";
      for (String s : parsed) {
        if (s.matches("\\([0-9]*\\)")) {
          return s.substring(1, s.length()-1);
        }
      }
    }
    return "";
  }
  //}}}

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
  { return "#co-center-tool"; }
  
  public String toString() { return "Co-center Tool"; }    
  //}}}
  
}
