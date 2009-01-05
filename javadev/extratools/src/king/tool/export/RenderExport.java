// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
import king.core.*;
import king.points.*;
import king.core.Engine;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>RenderExport</code> allows the current graphics to be exported for use
* in the render program (raster3d). 
*
* <p>Copyright (C) 2003-2008 by Vincent B. Chen. All rights reserved.
* <br>Begun on Wed Nov 05 16:11:39 EST 2008
*/
public class RenderExport extends Plugin {
  
  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  ArrayList<KPoint>[]         zbuffer;
  ArrayList<KList>[]          parents;                // KList that is acting parent for each pt in zbuffer; default is null
  double width, height;
  Engine eng;
  PrintWriter out;
  //}}}
  
  //{{{ Constructors
  public RenderExport(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ askExport
  public void askExport() {
    // Show the Save dialog
    String currdir = System.getProperty("user.dir");
    JFileChooser chooser = new JFileChooser();
    if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
    if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(kMain.getTopWindow()))
    {
      File f = chooser.getSelectedFile();
      if(!f.exists() ||
        JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
      "This file exists -- do you want to overwrite it?",
      "Overwrite file?", JOptionPane.YES_NO_OPTION))
      {
        try { exportImage(f); }
        catch(IOException ex)
        {
          JOptionPane.showMessageDialog(kMain.getTopWindow(),
          "An I/O error occurred while saving the file:\n"+ex.getMessage(),
          "Sorry!", JOptionPane.ERROR_MESSAGE);
          ex.printStackTrace(SoftLog.err);
        }
      }
    }
  }
  //}}}

  //{{{ exportImage
  public void exportImage(File f) throws IOException {
    out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
    out.println("kingoutput.kin");
    out.println("64 64      NTX,NTY  tiles in x,y ");
    out.println("16 16      NPX,NPY   pixels (x,y) per tile ");
    out.println("4          SCHEME anti-aliasing level ");
    out.println("0 0 0      BKGND background, 0 0 0 for black (1 1 1 for white) ");
    out.println("F       SHADOW  T with, F omit shadows ");
    out.println("25         IPHONG Phong power ");
    out.println("0.25       STRAIT  secondary light percent contribution ");
    out.println("0.05       AMBIEN  ambient light percent contribution ");
    out.println("0.25       SPECLR  specular reflection  percent contribution ");
    out.println("0          EYPOS for perspective, 0 for orthographic ");
    out.println("-1 1 1     SOURCE primary light position, 1 1 1 right shoulder ");
    out.println("1 0 0 0    TMAT  post-multipy a horizontal vector x y z 1 ");
    out.println("0 -1 0 0   TMAT ");
    out.println("0 0 1 0    TMAT ");
    out.println("-450.000 450.000 -450.000 900.000    TMAT ");
    out.println("3          INMODE input mode must be 3 for flagged type ");
    out.println("*          INFMTS free format for triangles and planes, type 1 (normals: 13) ");
    out.println("*          INFMTS free format for sphere descriptors, type 2 ");
    out.println("*          INFMTS free format for cylinder descriptors, type 3 ");
    
    out.println(renderView());
    
    out.flush();
  }
  //}}}
  
  //{{{ renderView
  public String renderView() {
    String render = "";
    Dimension dim = kCanvas.getCanvasSize();
    ////kCanvas.paintCanvas(g2, dim, KinCanvas.QUALITY_BEST);
    Rectangle bounds = new Rectangle(dim);
    width   = bounds.getWidth();
    height  = bounds.getHeight();
    System.out.println(width + "," + height);
    ////kCanvas.syncToKin(engine, kin);
    //KView view = kCanvas.getCurrentView();
    eng = kCanvas.getEngine();
    System.out.println(eng.clipBack+", "+eng.clipFront);
    //render(kCanvas, view, bounds, eng);
    KIterator<KList> lists = KIterator.allLists(kMain.getKinemage());
    for (KList list : lists) {
      String type = list.getType();
      if (type.equals(KList.VECTOR))        render = render + renderVector(list);
      else if (type.equals(KList.BALL))     render = render + renderBall(list);
      else if (type.equals(KList.SPHERE))   render = render + renderBall(list);
      else if (type.equals(KList.TRIANGLE)) render = render + renderTriangle(list);
      else if (type.equals(KList.RIBBON))   render = render + renderTriangle(list);
      //else System.out.println(type + " is not supported yet!");
    }
    return render;
  }
  //}}}
  
  //{{{ renderBall
  public String renderBall(KList list) {
    String render = "";
    KIterator<KPoint> points = KIterator.visiblePoints(list);
    for (KPoint pt : points) {
      if (pt instanceof BallPoint) {
        System.out.println(pt.getWidth());
        render = render + "2 \n";
        render = render + df.format(pt.getDrawX())+" "+df.format(pt.getDrawY())+" "+df.format(pt.getDrawZ())+" ";
        render = render + df.format(((BallPoint)pt).getDrawRadius()) + " ";
        render = render + convertColor(pt.getDrawingColor(eng))+"\n";
      }
    }
    return render;
  }
  //}}}
  
  //{{{ renderVector
  public String renderVector(KList list) {
    String render = "";
    HashMap<KPoint, KPoint> drawnPoints = new HashMap<KPoint, KPoint>();
    KIterator<KPoint> points = KIterator.visiblePoints(list);
    for (KPoint pt : points) {
      if (pt instanceof VectorPoint) {
        KPoint prev = pt.getPrev();
        if ((prev != null)&&((isDrawn(prev))||(isDrawn(pt)))) {
          drawnPoints.put(prev, pt);
        }
      }
    }
    for (KPoint start : drawnPoints.keySet()) {
      KPoint end = drawnPoints.get(start);
      render = render + "3 \n";
      render = render + df.format(start.getDrawX())+" "+df.format(start.getDrawY())+" "+df.format(start.getDrawZ())+" ";
      render = render /*+ df.format(list.getWidth())*/+"1.500 ";
      render = render + df.format(end.getDrawX())+" "+df.format(end.getDrawY())+" "+df.format(end.getDrawZ())+" ";
      render = render + "1.000 ";
      render = render + convertColor(end.getDrawingColor(eng))+"\n";
      System.out.print("X: "+df.format(start.getDrawX())+" Y: "+df.format(start.getDrawY())+" Z: "+df.format(start.getDrawZ()) +"->");
      System.out.println("X: "+df.format(end.getDrawX())+" Y: "+df.format(end.getDrawY())+" Z: "+df.format(end.getDrawZ()));
    }
    return render;
  }
  //}}}
  
  //{{{ renderTriangle
  public String renderTriangle(KList list) {
    String render = "";
    KIterator<KPoint> points = KIterator.visiblePoints(list);
    for (KPoint pt : points) {
      if (pt instanceof TrianglePoint) {
        
        TrianglePoint tpt = (TrianglePoint)pt;
        if(tpt.getPrev() == null || tpt.getPrev().getPrev() == null/* || maincolor.isInvisible()*/) {
          System.out.println("no prev points");
        } else {
          TrianglePoint A = tpt, B = tpt.getPrev(), C = tpt.getPrev().getPrev();
          render = render + "1 \n";
          render = render + df.format(A.getDrawX())+" "+df.format(A.getDrawY())+" "+df.format(A.getDrawZ())+" ";
          render = render + df.format(B.getDrawX())+" "+df.format(B.getDrawY())+" "+df.format(B.getDrawZ())+" ";
          render = render + df.format(C.getDrawX())+" "+df.format(C.getDrawY())+" "+df.format(C.getDrawZ())+" ";
          render = render + convertColor(pt.getDrawingColor(eng))+"\n";
        }
      }
    }
    System.out.println(render);
    return render;
  }
  //}}}
  
  //{{{ convertColor
  public String convertColor(KPaint color) {
    Color hsv = color.getBlackExemplar();
    String rgbString = df.format((float)hsv.getRed()/255)+" "+df.format((float)hsv.getGreen()/255)+" "+df.format((float)hsv.getBlue()/255);
    return rgbString;
  }
  //}}}
  
  //{{{ isDrawn
  public boolean isDrawn(KPoint pt) {
    double x = pt.getDrawX();
    double y = pt.getDrawY();
    double z = pt.getDrawZ();
    return ((x >= 0)&&(x <= width)&&(y >= 0)&&(y <= height)&&(z > eng.clipBack)&&(z < eng.clipFront));
  }
  //}}}
 
  //{{{ render
  //##################################################################################################
  ///**
  //* Transforms the given Transformable and renders it to a graphics context.
  //* @param xformable      the Transformable that will be transformed and rendered
  //* @param view           a KView representing the current rotation/zoom/clip
  //* @param bounds         the bounds of the area to render to.
  //*   Note that this function does not clip g to ensure that it only paints within these bounds!
  //*/
  //void render(Transformable xformable, KView view, Rectangle bounds, Engine eng)
  //{
  //  eng.transform(xformable, view, bounds);
  //  
  //  // Now paint them to the graphics
  //  for(int i = 0; i <= TOP_LAYER; i++)
  //  {
  //    // Calculate depth-cueing constants for this level
  //    //if(cueIntensity)    colorCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
  //    //else                colorCue = KPaint.COLOR_LEVELS - 1;
  //    //if(cueThickness)    widthCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
  //    //else                widthCue = (KPaint.COLOR_LEVELS-1) / 2;
  //    
  //    //if(colorCue >= KPaint.COLOR_LEVELS)
  //    //    SoftLog.err.println("colorCue = "+colorCue+"; i = "+i+"; TOP_LAYER = "+TOP_LAYER);
  //    
  //    // Render all points at this level (faster to not use iterators)
  //    ArrayList<KPoint>   zb      = zbuffer[i];
  //    ArrayList<KList>    pnt     = parents[i];
  //    for(int j = 0, end_j = zb.size(); j < end_j; j++)
  //    {
  //      KPoint  pt  = (KPoint) zb.get(j);
  //      KList   l   = (KList) pnt.get(j);
  //      if(l == null) {
  //        //pt.paint2D(this);
  //        System.out.println("X: "+pt.getDrawX()+"Y: "+pt.getDrawY()+"Z: "+pt.getDrawZ());
  //      }
  //      else // see setActingParent() for an explanation
  //      {
  //        KList oldPnt = pt.getParent();
  //        pt.setParent(l);
  //        //pt.paint2D(this);
  //        pt.setParent(oldPnt);
  //      }
  //    }
  //  }
  //}    
  //}}}
  
  //{{{ getToolsMenuItem, getHelpMenuItem, toString, onExport, isAppletSafe
  //##################################################################################################
  public JMenuItem getToolsMenuItem()
  {
    return new JMenuItem(new ReflectiveAction(this.toString()+"...", null, this, "onExport"));
  }
  
  public JMenuItem getHelpMenuItem()
  { return null; }
  
  public String toString()
  { return "Render Image"; }
  
  // This method is the target of reflection -- DO NOT CHANGE ITS NAME
  public void onExport(ActionEvent ev)
  { this.askExport(); }
  
  static public boolean isAppletSafe()
  { return false; }
  //}}}
  
}
