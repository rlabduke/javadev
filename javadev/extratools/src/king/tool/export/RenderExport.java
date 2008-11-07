// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
import king.core.*;
import king.core.Engine;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import driftwood.gui.*;

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
  //}}}
  
  //{{{ Variables
  ArrayList<KPoint>[]         zbuffer;
  ArrayList<KList>[]          parents;                // KList that is acting parent for each pt in zbuffer; default is null
  public final int    TOP_LAYER;

  //}}}
  
  //{{{ Constructors
  public RenderExport(ToolBox tb) {
    super(tb);
    TOP_LAYER = 1000;
  }
  //}}}
  
  //{{{ askExport
  public void askExport() {
    Dimension       dim = kCanvas.getCanvasSize();
    ////kCanvas.paintCanvas(g2, dim, KinCanvas.QUALITY_BEST);
    Rectangle bounds = new Rectangle(dim);
    double width, height, size, xOff, yOff;
    width   = bounds.getWidth();
    height  = bounds.getHeight();
    System.out.println(width + "," + height);
    ////kCanvas.syncToKin(engine, kin);
    //KView view = kCanvas.getCurrentView();
    Engine eng = kCanvas.getEngine();
    System.out.println(eng.clipBack+", "+eng.clipFront);
    //render(kCanvas, view, bounds, eng);
    KIterator<KPoint> points = KIterator.visiblePoints(kMain.getKinemage());
    for (KPoint pt : points) {
      double x = pt.getDrawX();
      double y = pt.getDrawY();
      double z = pt.getDrawZ();
      if ((x >= 0)&&(x <= width)&&(y >= 0)&&(y <= height)&&(z > eng.clipBack)&&(z < eng.clipFront)) {
        System.out.println(pt.getName() + " X: "+pt.getDrawX()+" Y: "+pt.getDrawY()+" Z: "+pt.getDrawZ());
      }
    }
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
