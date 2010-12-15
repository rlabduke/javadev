// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.core.*;

import javax.swing.*;
import java.util.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import driftwood.util.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;

//}}}
/**
* <code>ParenTool</code> 
* 
* <p>Copyright (C) 2009 by Vincent B. Chen. All rights reserved.
* <br>Begun Fri May 08 15:08:17 EDT 2009
**/
public class ParenTool extends BasicTool implements ListSelectionListener {

  //{{{ Constants
  //}}}

  //{{{ Variables
  TablePane2 pane;
  JList  drawingPaneList;
  HashMap<String, ParenGroup> groupData;
  //}}}

  //{{{ Constructors
  public ParenTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ buildGUI
  public void buildGUI() {
    Icon greenPlus   = new ImageIcon(getClass().getResource("greenplus.png"));
    Icon redMinus    = new ImageIcon(getClass().getResource("redminus.png"));
    
    JButton addButton = new JButton(new ReflectiveAction("Add group", greenPlus, this, "onAdd"));
    JButton removeButton = new JButton(new ReflectiveAction("Remove group", redMinus, this, "onMinus"));
    
    drawingPaneList = new FatJList(0, 4);
    drawingPaneList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    drawingPaneList.setVisibleRowCount(8);
    drawingPaneList.addListSelectionListener(this);
    
    pane = new TablePane2();
    pane.newRow();
    pane.add(addButton);
    pane.addCell(new JScrollPane(drawingPaneList), 1, 2);
    pane.newRow();
    pane.add(removeButton);
    
  }
  //}}}
  
  //{{{ start
  public void start() {
    if (pane == null) {
      buildGUI();
    }
    groupData = new HashMap<String, ParenGroup>();
    analyzeKin(kMain.getKinemage());
    show();

    // Helpful hint for users:
    //this.services.setID("Ctrl-click, option-click, or middle-click a point to co-center");
  }
  //}}}
  
  //{{{ analyzeKin
  public void analyzeKin(Kinemage kin) {
    //Iterator groups = kin.iterator();
    //while (groups.hasNext()) {
    //  KGroup group = (KGroup) groups.next();
    //  
    //}
    KIterator<KPoint> points = KIterator.allPoints(kin);
    for (KPoint p : points) {
      String pName = p.getName();
      if (pName.matches("\\([0-9]*\\).*")) {
        //System.out.println(pName);
        String paren = getParen(p);
        if (groupData.containsKey(paren)) {
          ParenGroup pGroup = groupData.get(paren);
          pGroup.add(p);
        } else {
          ParenGroup pGroup = new ParenGroup(Integer.parseInt(paren));
          pGroup.add(p);
          groupData.put(paren, pGroup);
        }
      }
    }
    Object[] groups = groupData.values().toArray();
    Arrays.sort(groups);
    drawingPaneList.setListData(groups);
  }
  //}}}
  
  //{{{ onAdd/Minus
  public void onAdd(ActionEvent ev) {
    String inputValue = "A";
    while (!inputValue.matches("[0-9][0-9]*")) {
      inputValue = JOptionPane.showInputDialog("Please input an integer value");
    }
    ParenGroup pGroup = new ParenGroup(Integer.parseInt(inputValue));
    groupData.put(inputValue, pGroup);
    Object[] groups = groupData.values().toArray();
    Arrays.sort(groups);
    drawingPaneList.setListData(groups);
  }
  
  public void onMinus(ActionEvent ev) {
    ParenGroup selected = (ParenGroup) drawingPaneList.getSelectedValue();
    if(selected != null) {
      selected.clear();
    }
    groupData.remove(selected.getParen());
    Object[] groups = groupData.values().toArray();
    Arrays.sort(groups);
    drawingPaneList.setListData(groups);
    kMain.getKinemage().fireKinChanged(AGE.CHANGE_POINT_CONTENTS);
  }
  //}}}
  
  //{{{ xx_click() functions
  //##################################################################################################
  /** Override this function for (left-button) clicks */
  public void click(int x, int y, KPoint p, MouseEvent ev)
  {
    super.click(x, y, p, ev);
    ParenGroup selected = (ParenGroup) drawingPaneList.getSelectedValue();
    if((selected != null)&&(p != null)) {
      selected.addSub(p);
      kMain.getKinemage().fireKinChanged(AGE.CHANGE_POINT_CONTENTS);
    }
  }
  //}}}
  
  //{{{ valueChanged
  public void valueChanged(ListSelectionEvent ev) {
    if(!drawingPaneList.getValueIsAdjusting())
    {
      Object selected = drawingPaneList.getSelectedValue();
      if(selected != null) {
        kMain.getKinemage().fireKinChanged(AGE.CHANGE_POINT_ON_OFF);
        //drawingCards.show(drawingPanel, selected.toString());
      }
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
  
  //{{{ doTransform
  //##################################################################################################
  public void doTransform(Engine engine, Transform xform)
  {
    ParenGroup g = (ParenGroup) drawingPaneList.getSelectedValue();
    if (g != null) g.getList().doTransform(engine, xform);
    
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
  { return "#paren-tool"; }
  
  public String toString() { return "Paren tool"; }    
  //}}}
  
}
