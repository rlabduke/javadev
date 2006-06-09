// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
 * <code>KinWriter</code> writes out traditional kinemage files.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Thu Oct  3 09:51:11 EDT 2002
*/
public class KinWriter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.####");
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
    public KinWriter()
    {
    }
//}}}

//{{{ save()
//##################################################################################################
    /** Writes out all the currently open kinemages */
    public void save(Writer destination, String text, Collection kinemages)
    {
        Kinemage    kin;
        int         index   = 1;
        
        out = new PrintWriter(new BufferedWriter(destination));
        out.println("@text");
        out.println(text.trim());
        
        for(Iterator iter = kinemages.iterator(); iter.hasNext(); index++)
        {
            kin = (Kinemage)iter.next();
            writeKinemage(kin, index);
        }
        
        out.flush();
    }
//}}}

//{{{ writeKinemage()
//##################################################################################################
    void writeKinemage(Kinemage kin, int index)
    {
        Iterator iter;
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
        
        Collection colorsets = kin.getNewPaintMap().values();
        for(iter = colorsets.iterator(); iter.hasNext(); )
        {
            KPaint paint = (KPaint)iter.next();
            if(paint.isAlias())
                out.println("@colorset {"+paint+"} "+paint.getAlias());
            else
                out.println("< Couldn't save new color "+paint+" >");
        }
        
        Aspect aspect;
        for(iter = kin.getAspectIterator(); iter.hasNext(); )
        {
            aspect = (Aspect)iter.next();
            out.println("@"+aspect.getIndex()+"aspect {"+aspect.getName()+"}");
        }
        
        int idx = 1;
        for(iter = kin.getViewIterator(); iter.hasNext(); idx++)
        {
            writeView((KingView)iter.next(), idx);
        }
        
        for(iter = kin.masterList().iterator(); iter.hasNext(); )
        {
            writeMaster((MasterGroup)iter.next(), kin);
        }
        
        // etc.
        
        KGroup group;
        for(iter = kin.iterator(); iter.hasNext(); )
        {
            group = (KGroup)iter.next();
            writeGroup(group, kin);
        }
    }
//}}}

//{{{ writeGroup()
//##################################################################################################
    void writeGroup(KGroup group, Kinemage kin)
    {
        Iterator iter;
        out.print("@group {"+group.getName()+"}");
        if(! group.isOn())          out.print(" off");
        if(! group.hasButton())     out.print(" nobutton");
        if(  group.isDominant())    out.print(" dominant");
        if(  group.isRecessiveOn()) out.print(" recessiveon");
        if(  group.isAnimate())     out.print(" animate");
        if(  group.is2Animate())    out.print(" 2animate");
        if(  group.isLens())        out.print(" lens");
        
        /*MasterGroup master;
        for(iter = kin.masterIter(); iter.hasNext(); )
        {
            master = (MasterGroup)iter.next();
            if(master.isTarget(group)) out.print(" master= {"+master.getName()+"}");
        }*/
        for(iter = group.masterIterator(); iter != null && iter.hasNext(); )
        {
            out.print(" master= {"+iter.next().toString()+"}");
        }        
        out.println();
        
        KSubgroup subgroup;
        for(iter = group.iterator(); iter.hasNext(); )
        {
            subgroup = (KSubgroup)iter.next();
            writeSubgroup(subgroup, kin);
        }
    }
//}}}

//{{{ writeSubgroup()
//##################################################################################################
    void writeSubgroup(KSubgroup subgroup, Kinemage kin)
    {
        Iterator iter;
        out.print("@subgroup {"+subgroup.getName()+"}");
        if(! subgroup.isOn())           out.print(" off");
        if(! subgroup.hasButton())      out.print(" nobutton");
        if(  subgroup.isDominant())     out.print(" dominant");
        if(  subgroup.isRecessiveOn())  out.print(" recessiveon");
        if(  subgroup.isLens())         out.print(" lens");
        
        /*MasterGroup master;
        for(iter = kin.masterIter(); iter.hasNext(); )
        {
            master = (MasterGroup)iter.next();
            if(master.isTarget(subgroup)) out.print(" master= {"+master.getName()+"}");
        }*/
        for(iter = subgroup.masterIterator(); iter != null && iter.hasNext(); )
        {
            out.print(" master= {"+iter.next().toString()+"}");
        }        
        out.println();
        
        KList list;
        for(iter = subgroup.iterator(); iter.hasNext(); )
        {
            list = (KList)iter.next();
            writeList(list, kin);
        }
    }
//}}}

//{{{ writeList()
//##################################################################################################
    void writeList(KList list, Kinemage kin)
    {
        Iterator iter;
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
            if(list.width != 2) out.print(" width= "+list.width);
        }
        else if(list.getType() == KList.BALL || list.getType() == KList.SPHERE)
        {
            out.print(" radius= "+df.format(list.radius));
            if((list.flags & KList.NOHILITE) != 0) out.print(" nohilite"); 
        }
        else if(list.getType() == KList.ARROW)
        {
            out.print(" radius= "+df.format(list.getRadius()));
            out.print(" angle= "+df.format(list.getAngle()));
        }
        
        if(list.alpha != 255) out.print(" alpha= "+df.format(list.alpha/255.0));
        for(iter = list.masterIterator(); iter != null && iter.hasNext(); )
        {
            out.print(" master= {"+iter.next().toString()+"}");
        }
        out.println();

        KPoint point;
        lastPointID = null;
        for(iter = list.iterator(); iter.hasNext(); )
        {
            point = (KPoint)iter.next();
            writePoint(point, list, kin);
            lastPointID = point.getName();
        }
    }
//}}}

//{{{ writePoint()
//##################################################################################################
    void writePoint(KPoint point, KList list, Kinemage kin)
    {
        Iterator iter;
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
            if(v.width > 0 && v.width != list.width) out.print("width"+v.width+" ");
        }
        else if(point instanceof BallPoint)
        {
            BallPoint b = (BallPoint)point;
            if(b.r0 > 0f) out.print("r="+df.format(b.r0)+" ");
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
            out.println(df.format(point.getOrigX())+" "+df.format(point.getOrigY())+" "+df.format(point.getOrigZ()));
        else
        {
            for(int i = 0; i < coords.length; i++) out.print(df.format(coords[i])+" ");
            out.println();
        }
    }
//}}}

//{{{ writeView(), writeMaster()
//##################################################################################################
    void writeView(KingView view, int index)
    {
        out.println("@"+index+"viewid {"+view.getName()+"}");
        out.println("@"+index+"span "+view.getSpan());
        out.println("@"+index+"zslab "+(view.getClip()*200f));
        float[] center = view.getCenter();
        out.println("@"+index+"center "+df.format(center[0])+" "+df.format(center[1])+" "+df.format(center[2]));
        
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
        if(master.indent)           out.print(" indent");
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

