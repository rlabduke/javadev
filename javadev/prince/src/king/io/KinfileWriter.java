// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.io;
import king.core.*;
import king.points.*;

import java.awt.Color;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
 * <code>KinfileWriter</code> writes out traditional kinemage files.
 *
 * <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
 * <br>Begun on Thu Oct  3 09:51:11 EDT 2002
*/
public class KinfileWriter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df = driftwood.util.Strings.usDecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##################################################################################################
    PrintWriter out = null;
    String lastPointID = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public KinfileWriter()
    {
    }
//}}}

//{{{ save
//##################################################################################################
    /** Writes out all the currently open kinemages */
    public void save(Writer destination, String text, Collection<Kinemage> kinemages)
    {
        out = new PrintWriter(new BufferedWriter(destination));
        out.println("@text");
        out.println(text.trim());
        
        int index = 1;
        for(Kinemage kin : kinemages)
            writeKinemage(kin, index++);
        
        out.flush();
    }
//}}}

//{{{ writeKinemage
//##################################################################################################
    void writeKinemage(Kinemage kin, int index)
    {
        out.println("@kinemage "+index);
        if(!kin.getName().startsWith(KinfileParser.DEFAULT_KINEMAGE_NAME))
        { out.println("@title {"+kin.getName()+"}"); }
        
        if(kin.atWhitebackground)   out.println("@whitebackground");
        if(kin.atOnewidth)          out.println("@onewidth");
        else if(kin.atThinline)     out.println("@thinline");
        if(kin.atPerspective)       out.println("@perspective");
        if(kin.atFlat)              out.println("@flat");
        if(kin.atListcolordominant) out.println("@listcolordominant");
        if(kin.atLens > 0.0)        out.println("@lens "+df.format(kin.atLens));
        if(kin.atPdbfile != null)   out.println("@pdbfile {"+kin.atPdbfile+"}");
        if(kin.atCommand != null)   out.println("@command {"+kin.atCommand+"}");
        
        for(KPaint paint : kin.getNewPaintMap().values())
        {
            if(paint.isAlias())
                out.println("@colorset {"+paint+"} "+paint.getAlias());
            else
            {
                //out.println("< Couldn't save new color "+paint+" >");
                out.print("@hsvcolor {"+paint+"}");
                Color color = (Color) paint.getBlackExemplar();
                float[] hsv = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(),  null);
                out.print(" "+df.format(360*hsv[0])+" "+df.format(100*hsv[1])+" "+df.format(100*hsv[2]));
                color = (Color) paint.getWhiteExemplar();
                hsv = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(),  null);
                out.print(" "+df.format(360*hsv[0])+" "+df.format(100*hsv[1])+" "+df.format(100*hsv[2]));
                out.println();
            }
        }
        
        for(Aspect aspect : kin.getAspects())
            out.println("@"+aspect.getIndex()+"aspect {"+aspect.getName()+"}");
        
        int idx = 1;
        for(KView view : kin.getViewList())
            writeView(view, idx++);
        
        for(MasterGroup master : kin.masterList())
            writeMaster(master, kin);
        
        if(kin.dimensionNames.size() > 0)
        {
            out.print("@dimensions");
            for(String name : kin.dimensionNames)
                out.print(" {"+name+"}");
            out.println();
        }
        if(kin.dimensionMinMax.size() > 0)
        {
            out.print("@dimminmax");
            for(Number number : kin.dimensionMinMax)
                out.print(" "+df.format(number.doubleValue()));
            out.println();
        }
        
        // etc.
        
        for(KGroup group : kin)
            writeGroup(group, kin);
    }
//}}}

//{{{ writeGroup
//##################################################################################################
    void writeGroup(KGroup group, Kinemage kin)
    {
        int depth = group.getDepth();
        if(depth == 1)
        {
            out.print("@group {"+group.getName()+"}");
            if(  group.isAnimate())     out.print(" animate");
            if(  group.is2Animate())    out.print(" 2animate");
        }
        else if(depth == 2)
        {
            out.print("@subgroup {"+group.getName()+"}");
        }
        
        if(depth == 1 || depth == 2)
        {
            if(! group.isOn())          out.print(" off");
            if(! group.hasButton())     out.print(" nobutton");
            if(  group.isDominant())    out.print(" dominant");
            if(  group.isCollapsible()) out.print(" collapsible");
            if(  group.isLens())        out.print(" lens");
            
            for(String master : group.getMasters())
                out.print(" master= {"+master+"}");
            out.println();
        }
        
        // Even if we don't write a @(sub)group line, recurse thru children
        for(AGE age : group)
        {
            if(age instanceof KList)        writeList((KList) age, kin);
            else if(age instanceof KGroup)  writeGroup((KGroup) age, kin);
        }
    }
//}}}

//{{{ writeList
//##################################################################################################
    void writeList(KList list, Kinemage kin)
    {
        out.print("@"+list.getType()+"list {"+list.getName()+"}");

        if(list.getInstance() != null)
            out.print(" instance= {"+list.getInstance().getName()+"}");
        
        if(! list.isOn())           out.print(" off");
        if(! list.hasButton())      out.print(" nobutton");
        if(  list.isLens())         out.print(" lens");
        
        KPaint paint = list.getColor();
        if(paint == null) paint = KPalette.defaultColor;
        if(paint.isAlias()) out.print(" color= {"+paint+"}");
        else                out.print(" color= "+paint);
        
        if(list.getType() == KList.VECTOR || list.getType() == KList.DOT)
        {
            if(list.getWidth() != 2) out.print(" width= "+list.getWidth());
        }
        else if(list.getType() == KList.BALL || list.getType() == KList.SPHERE || list.getType() == KList.RING)
        {
            out.print(" radius= "+df.format(list.getRadius()));
            if(list.getNoHighlight()) out.print(" nohighlight"); 
        }
        else if(list.getType() == KList.ARROW)
        {
            out.print(" radius= "+df.format(list.getRadius()));
            out.print(" angle= "+df.format(list.getAngle()));
        }
        
        if(list.getAlpha() != 255) out.print(" alpha= "+df.format(list.getAlpha()/255.0));
        for(String master : list.getMasters())
            out.print(" master= {"+master+"}");
        
        if(list.getDimension() != 3) out.print(" dimension= "+list.getDimension());
        out.println();

        lastPointID = null;
        for(KPoint point : list)
        {
            writePoint(point, list, kin);
            lastPointID = point.getName();
        }
    }
//}}}

//{{{ writePoint
//##################################################################################################
    void writePoint(KPoint point, KList list, Kinemage kin)
    {
        if(point.getComment() != null)  out.print("<"+point.getComment()+">");
        String pointID = point.getName();
        if(pointID.equals(lastPointID)) out.print("{\"}");
        else                            out.print("{"+pointID+"}");
        
        if(point.getPmMask() != 0)      out.print("'"+kin.fromPmBitmask(point.getPmMask())+"' ");
        if(point.getAspects() != null)  out.print("("+point.getAspects()+") ");
        if(point.isBreak())             out.print("P ");
        if(point.isUnpickable())        out.print("U ");
        if(point.isGhost())             out.print("ghost ");
        
        if(point instanceof TrianglePoint && point.isBreak())
            out.print("X "); // because triangle- and ribbonlists don't break for P
        else if(point instanceof VectorPoint)
        {
            VectorPoint v = (VectorPoint)point;
            if(v.getWidth() > 0 && v.getWidth() != list.getWidth()) out.print("width"+v.getWidth()+" ");
        }
        else if(point instanceof BallPoint)
        {
            BallPoint b = (BallPoint)point;
            if(b.getRadius() > 0f) out.print("r="+df.format(b.getRadius())+" ");
        }
        else if(point instanceof MarkerPoint)
        {
            MarkerPoint m = (MarkerPoint)point;
            if(m.getStyle() != 0) out.print("s="+m.getStyle()+" ");
        }
        /* XXX-DEBUG * /
        else if(point instanceof TrianglePoint)
        {
            if((point.multi & point.SEQ_EVEN_BIT) != 0) out.print("<even> ");
            else out.print("<odd> ");
        }
        /* XXX-DEBUG */
        
        KPaint paint = point.getColor();
        if(paint != null && paint != list.getColor())
            out.print(paint+" ");
        
        float[] coords = point.getAllCoords();
        if(coords == null)
            out.println(df.format(point.getX())+" "+df.format(point.getY())+" "+df.format(point.getZ()));
        else
        {
            for(int i = 0; i < coords.length; i++) out.print(df.format(coords[i])+" ");
            out.println();
        }
    }
//}}}

//{{{ writeView, writeMaster
//##################################################################################################
    void writeView(KView view, int index)
    {
        out.println("@"+index+"viewid {"+view.getName()+"}");
        out.println("@"+index+"span "+view.getSpan());
        out.println("@"+index+"zslab "+(view.getClip()*200f));
        float[] center = view.getCenter();
        out.println("@"+index+"center "+df.format(center[0])+" "+df.format(center[1])+" "+df.format(center[2]));
        
        int[] axes = view.getViewingAxes();
        if(axes != null)
            out.println("@"+index+"axischoice "+(axes[0]+1)+" "+(axes[1]+1)+" "+(axes[2]+1));
        
        // Writen out Mage-style, for a post-multiplied matrix
        out.print("@"+index+"matrix");
        for(int i = 0; i < 3; i++)
        {
            for(int j = 0; j < 3; j++) out.print(" "+df.format(view.xform[j][i])); 
        }
        out.println();
    }
    
    void writeMaster(MasterGroup master, Kinemage kin)
    {
        out.print("@master {"+master.getName()+"}");
        //if(! master.isOn())         out.print(" off");
        if(! master.hasButton())    out.print(" nobutton");
        if(master.getIndent())      out.print(" indent");
        out.println();
        
        if(master.pm_mask != 0)
        {
            out.println("@pointmaster '"+kin.fromPmBitmask(master.pm_mask)+"' {"+master.getName()+"}");
        }
    }
///}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

