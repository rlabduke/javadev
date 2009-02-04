// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.core.*;

//import java.io.*;
import javax.swing.*;
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
//import chiropraxis.kingtools.*;
//import king.tool.util.*;

//}}}

/**
* <code>CoCenterTool</code> is for cocentering an ensemble of structures on a
* a single point..
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
  ArrayList origCoords = null;
  //}}}
  
  //{{{ Constructors
  public CoCenterTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ start
  public void start() {
    buildGUI();
    show();
    // Helpful hint for users:
    this.services.setID("Ctrl-click, option-click, or middle-click a point to co-center");
  }
  //}}}
  
  //{{{ buildGUI
  public void buildGUI() {
    resetButton = new JButton(new ReflectiveAction("Reset Coordinates", null, this, "onReset"));
    
    pane = new TablePane2();
    pane.newRow();
    pane.add(resetButton);
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
    if(p != null)
    {
      String pName = p.getName();
      if (pName.length() > 14) pName = pName.substring(0, 14);
      System.out.println(pName);
      Kinemage kin = kMain.getKinemage();
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
    }
  }
  //}}}
  
  //{{{ onReset
  public void onReset(ActionEvent ev) {
    if (origCoords == null) return;
    Kinemage kin = kMain.getKinemage();
    Iterator iter = kin.iterator();
    int i = 0;
    System.out.println("Testing");
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
            System.out.println(new Double(test.getX()) +","+ new Double(test.getY()) +","+ new Double(test.getZ()));
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
