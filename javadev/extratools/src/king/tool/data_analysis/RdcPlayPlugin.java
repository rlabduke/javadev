// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;
import king.points.*;
import king.io.*;
import driftwood.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
//}}}

public class RdcPlayPlugin extends Plugin implements ChangeListener {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.00");
  //}}}
  
  //{{{ Variables
  double daxial, dn, drhombic, xisquared, etasquared, lambdasquared, zetasquared, deltacrit;
  double dzz, dyy, dxx;
  JSlider rhombSlider;
  JLabel rhombLabel;
  //}}}
  
  //{{{ Constructor
  public RdcPlayPlugin(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ onStart
  public void onStart(ActionEvent ev) {
    buildGUI();
    double rhombicity = ((double)rhombSlider.getValue()) / 1000.0;
    calcVariables(rhombicity);
    //daxial = -20.0;
    //drhombic = -20.0/3.0;
    //deltacrit = 10.0 / daxial;
    //dn = 1.05 * daxial;
    createKin();
  }
  //}}}
  
  //{{{ buildGUI
  public void buildGUI() {
    TablePane2 pane = new TablePane2();
    rhombSlider = new JSlider(1, 666, 1);
    rhombSlider.addChangeListener(this);
    pane.add(rhombSlider);
    pane.newRow();
    rhombLabel = new JLabel("0.001");
    pane.add(rhombLabel);
    JDialog dialog = new JDialog(kMain.getTopWindow(), "RDC Play Plugin", false);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setContentPane(pane);
    dialog.pack();
    dialog.show();
  }
  //}}}
  
  //{{{ stateChanged
  public void stateChanged(ChangeEvent ev) {
    double rhombicity = ((double)rhombSlider.getValue()) / 1000.0;
    //System.out.println(rhombicity);
    calcVariables(rhombicity);
    rhombLabel.setText(Double.toString(rhombicity));
    Kinemage kin = kMain.getKinemage();
    KIterator<KList> lists = KIterator.allLists(kin);
    KList list = lists.next();
    list.clear();
    createPoints(list);
  }
  //}}}
  
  //{{{ calcVariables
  public void calcVariables(double rhombicity) {
    dzz = 40;
    dyy = -20 - 30 * rhombicity;
    dxx = -20 + 30 * rhombicity;
    daxial = dzz / 2;
    drhombic = (dxx - dyy) / 3.0;
    deltacrit = dxx / daxial;
    //System.out.println(daxial + " " + drhombic + " " + deltacrit);
  }
  //}}}
  
  //{{{ createKin
  public void createKin() {
    Kinemage kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
    KGroup group = new KGroup("RDC Points");
    KGroup sub = new KGroup("Sub");
    sub.setHasButton(false);
    KList list = new KList(KList.VECTOR, "points");
    
    kin.add(group);
    group.add(sub);
    sub.add(list);

    createPoints(list);
    kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));

  }
  //}}}
  
  //{{{ createPoints
  public void createPoints(KList list) {
    VectorPoint origin = new VectorPoint("0 0 0", null);
    origin.setXYZ(0, 0, 0);
    VectorPoint plusZ = new VectorPoint("0 0 1", origin);
    plusZ.setXYZ(0, 0, 1);
    VectorPoint plusX = new VectorPoint("1 0 0", origin);
    plusX.setXYZ(1, 0, 0);
    plusX.setColor(KPalette.bluetint);
    list.add(origin);
    list.add(plusZ);
    list.add(plusX);
    for (int i = (int)((-1 - 3/2 * drhombic/daxial) * 100); i <= 200; i = i + 5) { // avoids round off errors, I think.
      //System.out.println((double) i / 100.0);
      //System.out.println(i);
      dn = i * daxial / 100.0;
      xisquared = (2 * daxial - dn) / (3 * daxial - (3/2) * drhombic);
      etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
      lambdasquared = (dn + daxial + (3 / 2) * drhombic) / (3 * drhombic);
      zetasquared = (dn + daxial + (3 / 2) * drhombic) / (3 * daxial + (3 / 2) * drhombic);
      //System.out.println(xisquared);
      //System.out.println(etasquared);
      VectorPoint old = null;
      VectorPoint negOld = null;
      ArrayList<KPoint> posPoints = new ArrayList<KPoint>();
      ArrayList<KPoint> negPoints = new ArrayList<KPoint>();
      for (double tau = - Math.PI; tau <= Math.PI; tau += Math.PI / 60) {
        //System.out.println(tau);
        VectorPoint point = new VectorPoint("Point " + df.format(dn), old);
        point.setXYZ(calcX(tau), calcY(tau), calcZ(tau));
        if (dn > 0) point.setColor(KPalette.green);
        //list.add(origin);
        //list.add(point);
        posPoints.add(point);
        old = point;
        VectorPoint negPoint = new VectorPoint("Neg Point " + df.format(dn), negOld);
        if (dn / daxial > deltacrit) {
          negPoint.setXYZ(calcX(tau), calcY(tau), -calcZ(tau));
        } else {
          negPoint.setXYZ(calcX(tau), -calcY(tau), calcZ(tau));
        }
        if (dn > 0) negPoint.setColor(KPalette.green);
        //list.add(negPoint);
        negPoints.add(negPoint);
        negOld = negPoint;
      }
      for (KPoint posPt : posPoints) {
        list.add(posPt);
      }
      for (KPoint negPt : negPoints) {
        list.add(negPt);
      }
    }
  }
  //}}}

  //{{{ calcX
  public double calcX(double tau) {
    //double xisquared = (2 * daxial - dn) / (3 * daxial - (3/2) * drhombic);
    //double etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    if (dn / daxial > deltacrit) {
      return Math.sqrt(xisquared) * Math.cos(tau);
    } else {
      return Math.sqrt(lambdasquared) * Math.cos(tau);
    }
  }
  //}}}
  
  //{{{ calcY
  public double calcY(double tau) {
    //double etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    if (dn / daxial > deltacrit) {
      return Math.sqrt(etasquared) * Math.sin(tau);
    } else {
      return Math.sqrt(1 - lambdasquared * Math.pow(Math.cos(tau),2) - zetasquared * Math.pow(Math.sin(tau),2));
    }
  }
  //}}}
  
  //{{{ calcZ
  /** only returns positive value **/
  public double calcZ(double tau) {
    //double xisquared = (2 * daxial - dn) / (3 * daxial - (3/2) * drhombic);
    //double etasquared = (2 * daxial - dn) / (3 * daxial + (3/2) * drhombic);
    if (dn / daxial > deltacrit) {
      return Math.sqrt(1 - xisquared * Math.pow(Math.cos(tau),2) - etasquared * Math.pow(Math.sin(tau),2));
    } else {
      return Math.sqrt(zetasquared) * Math.sin(tau);
    }
  }
  //}}}
  
  //{{{ getToolsMenuItem
  public JMenuItem getToolsMenuItem() {
    return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onStart"));
  }
  
  public String toString() {
    return "RDC Play";
  }
  //}}}
  
}
