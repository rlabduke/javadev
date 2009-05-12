// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;
import king.*;
import king.core.*;
import king.points.*;

import java.util.*;
import driftwood.r3.*;
//}}}

/**
* <code>ParenGroup</code> 
* 
* <p>Copyright (C) 2009 by Vincent B. Chen. All rights reserved.
* <br>Begun Fri May 08 15:08:17 EDT 2009
**/
public class ParenGroup {

  //{{{ Constants
  //}}}

  //{{{ Variables
  ArrayList<KPoint> points;
  KList labelList;
  int num;
  //}}}

  //{{{ Constructors
  public ParenGroup(int n) {
    labelList = new KList(KList.LABEL);
    points = new ArrayList<KPoint>();
    num = n;
  }
  //}}}
  
  //{{{ addSubPoint
  public void addSub(KPoint point) {
    //System.out.println(points.contains(new Triple(point)));
    if (points.contains(point)) {
      points.remove(point);
      LabelPoint label = new LabelPoint(Integer.toString(num));
      label.setX(point.getX());
      label.setY(point.getY());
      label.setZ(point.getZ());
      label.setUnpickable(true);
      labelList.getChildren().remove(label);
      String pName = point.getName();
      if (pName.matches("\\([0-9]*\\).*")) {
        point.setName(pName.substring(pName.indexOf(")")+1, pName.length()));
      }
    } else {
      points.add(point);
      //labelList.add(point);
      LabelPoint label = new LabelPoint(Integer.toString(num));
      label.setX(point.getX());
      label.setY(point.getY());
      label.setZ(point.getZ());
      label.setUnpickable(true);
      labelList.add(label);
      String pName = point.getName();
      if (!pName.matches("\\([0-9]*\\).*")) {
        point.setName("("+Integer.toString(num)+") "+pName);
      }
    }
  }
  //}}}
  
  //{{{ add
  public void add(KPoint point) {
    if (!points.contains(point)) {
      points.add(point);
      //labelList.add(point);
      LabelPoint label = new LabelPoint(Integer.toString(num));
      label.setX(point.getX());
      label.setY(point.getY());
      label.setZ(point.getZ());
      label.setUnpickable(true);
      labelList.add(label);
    }
  }
  //}}}
  
  //{{{ clear
  public void clear() {
    for (KPoint p : points) {
      String pName = p.getName();
      if (pName.matches("\\([0-9]*\\).*")) {
        p.setName(pName.substring(pName.indexOf(")")+1, pName.length()));
      }
    }
    points.clear();
    labelList.getChildren().clear();
  }
  //}}}

  //{{{ getList
  public KList getList() {
    return labelList;
  }
  //}}}
  
  //{{{ getParen
  public String getParen() {
    return Integer.toString(num);
  }
  //}}}
  
  //{{{ toString
  public String toString() {
    return "Paren group "+Integer.toString(num)+" "+points.size()+" points";
  }
  //}}}

}
