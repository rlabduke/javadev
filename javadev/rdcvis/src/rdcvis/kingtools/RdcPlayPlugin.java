// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package rdcvis.kingtools;
import king.*;
import king.core.*;
import king.points.*;
import king.io.*;
import driftwood.gui.*;
import driftwood.r3.*;
import rdcvis.*;
import driftwood.util.SoftLog;

import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import Jama.*;
//}}}

public class RdcPlayPlugin extends Plugin implements ChangeListener {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.00");
  static final String[] RAINBOW = { "grey", "purple", "blue", "sky", "cyan", "sea", "green", "lime", "yellow" ,"gold" ,"orange" ,"red"  };
  //}}}
  
  //{{{ Variables
  double daxial, dn, drhombic, xisquared, etasquared, lambdasquared, zetasquared, deltacrit;
  double dzz, dyy, dxx;
  RdcDrawer2 drawer;
  JSlider rhombSlider, rdcSlider, szzSlider;
  JLabel rhombLabel, rdcLabel, eigLabel;
  JCheckBox surfaceBox, singleRdcBox;
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
    int szz = szzSlider.getValue();
    calcVariables(rhombicity, szz);
    Matrix saupeDiag = new Matrix(3, 3);
    saupeDiag.set(0, 0, dxx);
    saupeDiag.set(1, 1, dyy);
    saupeDiag.set(2, 2, dzz);
    Matrix eigV = saupeDiag.eig().getV();
    drawer = new RdcDrawer2(saupeDiag, eigV);
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
    surfaceBox = new JCheckBox("Do surfaces");
    rhombSlider = new JSlider(1, 666, 1);
    rhombSlider.addChangeListener(this);
    rhombLabel = new JLabel("0.001");
    
    szzSlider = new JSlider(4, 40, 40);
    szzSlider.addChangeListener(this);
    eigLabel = new JLabel("");
    
    singleRdcBox = new JCheckBox(new ReflectiveAction("Only do one RDC", null, this, "onSingle"));
    rdcSlider = new JSlider(-20029, 39999, -20029);
    rdcSlider.addChangeListener(this);
    rdcSlider.setEnabled(false);
    rdcLabel = new JLabel("-20.029");
    rdcLabel.setEnabled(false);
    
    pane.add(surfaceBox);
    pane.newRow();
    pane.add(rhombSlider);
    pane.newRow();
    pane.add(rhombLabel);
    pane.newRow();
    pane.add(szzSlider);
    pane.newRow();
    pane.add(eigLabel);
    pane.newRow();
    pane.add(singleRdcBox);
    pane.newRow();
    pane.add(rdcSlider);
    pane.newRow();
    pane.add(rdcLabel);
    
    JDialog dialog = new JDialog(kMain.getTopWindow(), "RDC Play Plugin", false);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setContentPane(pane);
    dialog.pack();
    dialog.show();
  }
  //}}}
  
  //{{{ onSingle
  public void onSingle(ActionEvent ev) {
    this.stateChanged(new ChangeEvent(singleRdcBox));
    if (!singleRdcBox.isSelected()) {
      rdcSlider.setEnabled(false);
      rdcLabel.setEnabled(false);
    } else {
      rdcSlider.setEnabled(true);
      rdcLabel.setEnabled(true);
    }
  }
  //}}}
  
  //{{{ stateChanged
  public void stateChanged(ChangeEvent ev) {
    if (singleRdcBox.isSelected()) {
      rdcSlider.setMinimum((int)((dyy+0.001)*1000));
      rdcLabel.setText(Double.toString(((double)rdcSlider.getValue()) / 1000.0));
    }
    int szz = szzSlider.getValue();

    double rhombicity = ((double)rhombSlider.getValue()) / 1000.0;
    //System.out.println(rhombicity);
    calcVariables(rhombicity, szz);
    
    Matrix saupeDiag = new Matrix(3, 3);
    saupeDiag.set(0, 0, dxx);
    saupeDiag.set(1, 1, dyy);
    saupeDiag.set(2, 2, dzz);
    Matrix eigV = saupeDiag.eig().getV();
    drawer = new RdcDrawer2(saupeDiag, eigV);
    
    rhombLabel.setText(Double.toString(rhombicity));
    Kinemage kin = kMain.getKinemage();
    KIterator<KList> lists = KIterator.allLists(kin);
    KList pointList= null;
    KList surfaceList = null;
    for (KList list : lists) {
      if (list.getName().equals("rdcpoints")) {
        pointList = list;
        pointList.clear();
      }
      if (list.getName().equals("rdcsurface")) {
        surfaceList = list;
        surfaceList.clear();
      }
    }
    
    drawCurves(pointList, surfaceList);
    //for (double d = dyy; d < dzz; d=d+2) {
    //  drawer.drawCurve(d, new Triple(0, 0, 0), new Triple(1, 0, 0), 1, 40, d, list, "test", 1);
    //}
    //createPoints(list);
  }
  //}}}
  
  //{{{ calcVariables
  public void calcVariables(double rhombicity, int szz) {
    //dzz = 40;
    //dyy = -20 - 30 * rhombicity;
    //dxx = -20 + 30 * rhombicity;
    dzz = szz;
    dyy = -dzz/2 - dzz*3/4*rhombicity;
    dxx = -dzz/2 + dzz*3/4*rhombicity;
    setEigLabels(dzz, dxx, dyy);
    daxial = dzz / 2;
    drhombic = (dxx - dyy) / 3.0;
    deltacrit = dxx / daxial;
    //System.out.println(daxial + " " + drhombic + " " + deltacrit);
  }
  //}}}
  
  //{{{ setEigLabels
  public void setEigLabels(double szz, double sxx, double syy) {
    eigLabel.setText("Szz: "+df.format(szz)+" Sxx: "+df.format(sxx)+" Syy: "+df.format(syy));
  }
  //}}}
  
  //{{{ createKin
  public void createKin() {
    Kinemage kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
    KGroup group = new KGroup("RDC Curves");
    KGroup sub = new KGroup("Sub");
    sub.setHasButton(false);
    KList list = new KList(KList.VECTOR, "rdcpoints");
    KList surf = new KList(KList.BALL, "rdcsurface");
    surf.setNoHighlight(true);
    surf.setAlpha(100);
    surf.setRadius((float)0.1);
    surf.setColor(KPalette.forName("white"));
    
    kin.add(group);
    group.add(sub);
    sub.add(list);
    sub.add(surf);
    
    drawCurves(list, surf);
    //int currCurve = 0;
    //for (double d = dyy; d < dzz; d=d+2) {
    //  int colorIdx = (RAINBOW.length * currCurve) / (int)(dzz-dyy);
    //  currCurve++;
    //  drawer.drawCurve(d, new Triple(0, 0, 0), new Triple(1, 0, 0), 1, 40, d, list, "test", 1, KPalette.forName(RAINBOW[colorIdx]));
    //}
    //createPoints(list);
    kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));

  }
  //}}}
  
  //{{{ drawCurves
  public void drawCurves(KList list, KList surfList) {
    int currCurve = 0;
    int totalCurves = (int)((dzz-dyy)/2)+1;
    int totalCurvesUnder = (int)((dxx-dyy)/2)+1;
    int currCurveUnder = totalCurvesUnder-1;
    if (singleRdcBox.isSelected()) {
      double d = ((double)rdcSlider.getValue())/1000.0;
      drawer.drawCurve(d, new Triple(0, 0, 0), new Triple(1, 0, 0), 1, 1000, d, list, "test "+d, 1, KPalette.forName("green"));
      if (surfaceBox.isSelected()) {
        drawer.drawSurface(d, new Triple(0, 0, 0), surfList);
      }
    } else {
      for (double d = dyy+0.001; d < dzz; d=d+2) {
        int colorIdx;
        if (d < dxx) {
          colorIdx = (RAINBOW.length * currCurveUnder) / totalCurvesUnder;
          currCurveUnder--;
        } else {
          colorIdx = (RAINBOW.length * currCurve) / (totalCurves-totalCurvesUnder);
          currCurve++;
        }
        drawer.drawCurve(d, new Triple(0, 0, 0), new Triple(1, 0, 0), 1, 1000, d, list, "test "+d, 1, KPalette.forName(RAINBOW[colorIdx]));
        if (surfaceBox.isSelected()) {
          drawer.drawSurface(d, new Triple(0, 0, 0), surfList);
        }
      }
    }
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
  { return "#rdc-play"; }
  
  public String toString() {
    return "RDC play";
  }
  //}}}
  
}
