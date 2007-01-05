// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.SoftLog;
//}}}
/**
 * <code>PovrayExport</code> writes files for raytracing with the POV-Ray program.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Thu Oct  3 09:51:11 EDT 2002
*/
public class PovrayExport extends Plugin
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##################################################################################################
    PrintWriter out = null;
    String lastPointID = null;
    Map triNormals = null;
    
    JFileChooser        chooser = null;
    SuffixFileFilter    povFilter;
    JCheckBox           doNormals;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PovrayExport(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        povFilter = new SuffixFileFilter("POV-Ray scene description");
        povFilter.addSuffix(".pov");
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(povFilter);
        chooser.setFileFilter(povFilter);
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
        //chooser.addPropertyChangeListener(this);
        
        doNormals = new JCheckBox("Smooth triangles and ribbons", true);
        chooser.setAccessory(doNormals);
    }
//}}}

//{{{ askExport
//##############################################################################
    public void askExport()
    {
        if(chooser == null) buildChooser();
        
        // Show the Save dialog
        if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(kMain.getTopWindow()))
        {
            File f = chooser.getSelectedFile();
            if(!povFilter.accept(f) &&
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file has the wrong extension. Append '.pov' to the name?",
            "Fix extension?", JOptionPane.YES_NO_OPTION))
            {
                f = new File(f+".pov");
            }

                
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    Writer w = new BufferedWriter(new FileWriter(f));
                    // Doesn't make sense to save more than the current kinemage.
                    // This is consistent with what PDF, JPG/PNG exporters do.
                    //save(w, kMain.getTextWindow().getText(), kMain.getStable().getKins());
                    save(w, kMain.getTextWindow().getText(), Collections.singleton(kMain.getKinemage()));
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file:\n"+ex.getMessage(),
                        "Sorry!", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
            }
        }
    }
//}}}

//{{{ save
//##################################################################################################
    /** Writes out all the currently open kinemages */
    public void save(Writer destination, String text, Collection kinemages)
    {
        Kinemage    kin;
        int         index   = 1;
        
        out = new PrintWriter(new BufferedWriter(destination));
        out.println("// (jEdit options) :folding=explicit:collapseFolds=1:");
        out.println("// Still: povray +FN8 +W600 +H600 +Q9 +A0.3 +UL +UV +I__FILE__.pov +O__FILE__.png && open __FILE__.png");
        out.println("// Movie: povray +FN8 +W600 +H600 +Q9 +A0.3 +UL +UV +KFF60 +KC +I__FILE__.pov +O__FILE__.png");
        out.println("// QuickTime: choose File | Export, then QuickTime Movie / LAN settings; set size and data rate.");
        out.println("//@text {{{");
        out.println("/*");
        out.println(text.trim());
        out.println("*/");
        out.println("//}}}");
        out.println();
        
        for(Iterator iter = kinemages.iterator(); iter.hasNext(); index++)
        {
            kin = (Kinemage)iter.next();
            if(doNormals.isSelected())  triNormals = buildTriangleNormals(kin);
            else                        triNormals = new HashMap();
            writeKinemage(kin, index);
        }
        
        out.flush();
    }
//}}}

//{{{ writeKinemage
//##################################################################################################
    void writeKinemage(Kinemage kin, int index)
    {
        Iterator iter;
        out.println("//@kinemage "+index+" - hand edit at end of kinemage def.");
        out.println("// Should be span (from view) / (2 * rendering width in pixels)");
        out.println("#declare LW = "+df.format(kin.getCurrentView().getSpan())+" / (2 * 600); // unit line width"); 
        out.println("#declare DW = "+df.format(kin.getCurrentView().getSpan())+" / (2 * 600); // unit dot width"); 
        out.println("#declare LabelFont = \"timrom.ttf\"; // may need +L/path/to/font/files");
        out.println("#declare LabelDepth = 0.001; // essentially flat text");
        out.println("#declare LabelOffset = 0;");
        out.println("#declare LabelXform = transform { scale 1.0 }; // use this to get text facing forward");
        out.println();
        out.println("#declare DefaultTexture = texture { finish { ambient 0.4 diffuse 0.5 specular 0.3 roughness 0.1 } };");
        out.println("//{{{ Color definitions");
        Collection colorsets = kin.getAllPaintMap().values();
        for(iter = colorsets.iterator(); iter.hasNext(); )
        {
            KPaint kp = (KPaint)iter.next();
            Paint p;
            if(kin.atWhitebackground)   p = kp.getWhiteExemplar();
            else                        p = kp.getBlackExemplar();
            if(p instanceof Color)
            {
                Color c = (Color) p;
                out.print("#declare C_"+kp+" = texture { DefaultTexture pigment { rgb <");
                out.print(df.format(c.getRed()/255.0)+", ");
                out.print(df.format(c.getGreen()/255.0)+", ");
                out.print(df.format(c.getBlue()/255.0));
                out.println("> } };");
            }
        }
        out.println("//}}} Color definitions");
        
        out.println();
        out.println("//{{{ Kinemage definition");
        out.println("#declare Kinemage"+index+" = union {");
        for(iter = kin.iterator(); iter.hasNext(); )
        {
            KGroup group = (KGroup)iter.next();
            writeGroup(group, kin);
        }
        out.println("}; // End of Kinemage"+index);
        out.println("//}}} Kinemage definition");
        
        out.println();
        out.println("//{{{ View definitions");
        out.println("// The kinemage is declared using the original coordinates from the file.");
        out.println("// By default, these 'views' are used to transform it into a 2x2x2 unit box centered");
        out.println("// on the origin, while the viewpoint/camera is 6 units away on the +Z axis.");
        out.println("// Alternatively, you can work in the kinemage space by transforming the view instead:");
        out.println("//     camera { ... transform { View0 inverse } } // INVERSE is crucial!");
        out.println("// Use CLIPPED_BY statements with kinemage object declaration to get clipping.");
        int idx = 0;
        writeView(kin.getCurrentView(), idx++);
        for(iter = kin.getViewIterator(); iter.hasNext(); idx++)
        {
            writeView((KView)iter.next(), idx);
        }
        out.println("//}}} View definitions");
        
        out.println();
        out.println("object {");
        out.println("    Kinemage"+index);
        out.println("    transform { View0 }");
        out.println("    // clipped_by ... from view definitions");
        out.println("}");

        out.println();
        out.println("camera {");
        if(kin.atPerspective)   out.println("    perspective // orthographic");
        else                    out.println("    orthographic // perspective");
        out.println("    location <0, 0, 0>");
        out.println("    up <0, 1, 0> // * 3/4 // for 800x600, 1024x768, etc.");
        out.println("    right <-1, 0, 0> // right-handed");
        if(kin.atPerspective)   out.println("    direction <0, 0, 6> // <0, 0, 1>");
        else                    out.println("    direction <0, 0, 1> // <0, 0, 6>");
        out.println("    rotate <0, 180, 0>");
        out.println("    translate <0, 0, 6>");
        out.println("}");
        
        out.println();
        out.println("light_source { <-1, 1, 6> rgb 1 }");
        if(kin.atWhitebackground)   out.println("background { rgb 1 }");
        else                        out.println("background { rgb 0 }");
        out.println("fog { // for Mage-style depth cueing");
        out.println("    fog_type 2      // 'ground fog'");
        out.println("    up z            // density gradient along z");
        out.println("    fog_offset 1.0  // fog starts at z = ...");
        out.println("    fog_alt 0.001   // no transition region");
        if(kin.atWhitebackground)   out.println("    rgb 1");
        else                        out.println("    rgb 0");
        out.println("    distance 1.25   // smaller = stronger");
        out.println("}");
    }
//}}}

//{{{ writeGroup
//##################################################################################################
    void writeGroup(KGroup group, Kinemage kin)
    {
        if(!group.isOn()) out.print("/*");
        out.println("//@group {"+group.getName()+"}");
        
        for(Iterator iter = group.iterator(); iter.hasNext(); )
        {
            KSubgroup subgroup = (KSubgroup)iter.next();
            writeSubgroup(subgroup, kin);
        }
        
        out.println("// End of "+group.getName());
        if(!group.isOn()) out.println("*/");
    }
//}}}

//{{{ writeSubgroup
//##################################################################################################
    void writeSubgroup(KGroup subgroup, Kinemage kin)
    {
        if(!subgroup.isOn()) out.print("/*");
        out.println("//@subgroup {"+subgroup.getName()+"}");

        for(Iterator iter = subgroup.iterator(); iter.hasNext(); )
        {
            KList list = (KList)iter.next();
            writeList(list, kin);
        }
        
        out.println("// End of "+subgroup.getName());
        if(!subgroup.isOn()) out.println("*/");
    }
//}}}

//{{{ writeList
//##################################################################################################
    void writeList(KList list, Kinemage kin)
    {
        if(!list.isOn()) out.print("/*");
        out.println("union { //@"+list.getType()+"list {"+list.getName()+"}");

        lastPointID = null;
        for(Iterator iter = list.iterator(); iter.hasNext(); )
        {
            KPoint point = (KPoint)iter.next();
            writePoint(point, list, kin);
            lastPointID = point.getName();
        }
        
        KPaint paint = list.getColor();
        if(paint == null) paint = KPalette.defaultColor;
        out.println("texture { C_"+paint+" }");
        //TODO: support alpha
        //if(list.alpha != 255) out.print(" alpha= "+df.format(list.alpha/255.0));
        
        out.println("} // End of "+list.getName());
        if(!list.isOn()) out.println("*/");
    }
//}}}

//{{{ writePoint
//##################################################################################################
    void writePoint(KPoint point, KList list, Kinemage kin)
    {
        if(point == null) return;

        out.println("//"+point.getName());
        KPaint paint = point.getColor();
        String paintName = (paint != null && paint != list.getColor() && !kin.atListcolordominant) ? " texture {C_"+paint+"}" : "";
        if(point instanceof BallPoint)
        {
            double rad = ((BallPoint)point).r0;
            if(rad <= 0) rad = list.getRadius();
            out.println("sphere { <"+df.format(point.getX())+", "+df.format(point.getY())+", "+df.format(point.getZ())+">, "
                +df.format(rad)+paintName+" }");
        }
        else if(point instanceof DotPoint)
        {
            int width = list.getWidth();
            out.println("sphere { <"+df.format(point.getX())+", "+df.format(point.getY())+", "+df.format(point.getZ())+">, "
                +"DW*"+df.format(width)+paintName+" }");
        }
        else if(point instanceof LabelPoint)
        {
            out.print("text { ttf LabelFont, \""+point+"\", LabelDepth, LabelOffset");
            out.print(" transform { LabelXform }");
            out.print(" transform { translate <"+df.format(point.getX())+", "+df.format(point.getY())+", "+df.format(point.getZ())+"> }");
            out.println(paintName+" }");
        }
        // SpherePoint is a child of BallPoint
        else if(point instanceof TrianglePoint)
        {
            KPoint prev = point.getPrev();
            if(prev != null)
            {
                KPoint prevprev = prev.getPrev();
                if(prevprev != null && !(point.equals(prev) || prev.equals(prevprev) || prevprev.equals(point)))
                {
                    Triple n1 = (Triple) triNormals.get(point), n2 = (Triple) triNormals.get(prev), n3 = (Triple) triNormals.get(prevprev);
                    if(n1 == null || n2 == null || n3 == null)
                    {
                        System.err.println("Null normal for "+point);
                        out.println("triangle { <"+df.format(prevprev.getX())+", "+df.format(prevprev.getY())+", "+df.format(prevprev.getZ())+">, "
                            +"<"+df.format(prev.getX())+", "+df.format(prev.getY())+", "+df.format(prev.getZ())+">, "
                            +"<"+df.format(point.getX())+", "+df.format(point.getY())+", "+df.format(point.getZ())+">"
                            +paintName+" }");
                    }
                    else
                    {
                        // Get all normals facing same way, using n3 as reference.
                        // This *might* (?) lead to a consistent inside/outside definition.
                        if(n3.angle(n1) > 90) n1.neg();
                        if(n3.angle(n2) > 90) n2.neg();
                        out.println("smooth_triangle { <"+df.format(prevprev.getX())+", "+df.format(prevprev.getY())+", "+df.format(prevprev.getZ())+">, "
                            +"<"+df.format(n3.getX())+", "+df.format(n3.getY())+", "+df.format(n3.getZ())+">, "
                            +"<"+df.format(prev.getX())+", "+df.format(prev.getY())+", "+df.format(prev.getZ())+">, "
                            +"<"+df.format(n2.getX())+", "+df.format(n2.getY())+", "+df.format(n2.getZ())+">, "
                            +"<"+df.format(point.getX())+", "+df.format(point.getY())+", "+df.format(point.getZ())+">, "
                            +"<"+df.format(n1.getX())+", "+df.format(n1.getY())+", "+df.format(n1.getZ())+">"
                            +paintName+" }");
                    }
                }
            }
        }
        else if(point instanceof VectorPoint)
        {
            int width = point.getWidth();
            if(width <= 0) width = list.getWidth();
            out.println("sphere { <"+df.format(point.getX())+", "+df.format(point.getY())+", "+df.format(point.getZ())+">, "
                +"LW*"+df.format(width)+paintName+" }");
            KPoint prev = point.getPrev();
            if(prev != null && !point.equals(prev))
                out.println("cylinder { <"+df.format(point.getX())+", "+df.format(point.getY())+", "+df.format(point.getZ())+">, "
                    +"<"+df.format(prev.getX())+", "+df.format(prev.getY())+", "+df.format(prev.getZ())+">, "
                    +"LW*"+df.format(width)+paintName+" }");
        }
        else out.println("// "+point.getClass()+" is not supported in POV-Ray output");
    }
//}}}

//{{{ writeView
//##################################################################################################
    void writeView(KView view, int index)
    {
        out.println("//@"+index+"viewid {"+view.getName()+"}");
        
        float[] center = view.getCenter();
        out.println("#declare View"+index+" = transform {");
        out.println("    translate <"+df.format(-center[0])+", "+df.format(-center[1])+", "+df.format(-center[2])+">");
        out.println("    matrix <");
        for(int i = 0; i < 3; i++)
        {
            out.print("    ");
            for(int j = 0; j < 3; j++) out.print(df.format(view.xform[j][i])+", ");
            out.println();
        }
        out.println("    0, 0, 0>");
        out.println("    //rotate <0,20*sin(2*pi*clock),0> // rocking motion around Y");
        out.println("    //rotate <0,360*clock,0> // smooth rotation around Y");
        out.println("    //rotate <180*clock, 360*clock, 0> // tumbling motion");
        out.println("    scale 1.0/"+df.format(view.getSpan()));
        out.println("//clipped_by { plane { z, "+df.format(view.getClip())+"} }");
        out.println("//clipped_by { plane { -z, "+df.format(view.getClip())+"} }");
        out.println("};");
    }
///}}}

//{{{ buildTriangleNormals
//##################################################################################################
    /**
    * Builds a mapping of KPoints (which are equated by x,y,z) to normal vectors.
    */
    Map buildTriangleNormals(Kinemage kin)
    {
        // 1. Accumulate triangle normals involving a particular point
        RecursivePointIterator rpi = new RecursivePointIterator(kin);
        Map normals = new HashMap(); // maps KPoints to Collections of Triples
        while(rpi.hasNext())
        {
            KPoint pt = rpi.next();
            if(!(pt instanceof TrianglePoint)) continue;
            TrianglePoint t = (TrianglePoint) pt;
            if(t.getPrev() == null || t.getPrev().getPrev() == null) continue;
            TrianglePoint u = (TrianglePoint) t.getPrev(), v = (TrianglePoint) t.getPrev().getPrev();
            if(t.equals(u) || u.equals(v) || v.equals(t)) continue;
            Triple a = new Triple(u).sub(t);
            Triple b = new Triple(v).sub(t);
            a.cross(b).unit();
            // Swap alternate triangles to keep normals pointing the right way!
            if((t.multi & t.SEQ_EVEN_BIT) != 0) a.neg();
            Collection n = (Collection) normals.get(t);
            if(n == null)
            {
                n = new ArrayList();
                normals.put(t, n);
            }
            n.add(a);
            n = (Collection) normals.get(u);
            if(n == null)
            {
                n = new ArrayList();
                normals.put(u, n);
            }
            n.add(a);
            n = (Collection) normals.get(v);
            if(n == null)
            {
                n = new ArrayList();
                normals.put(v, n);
            }
            n.add(a);
        }
        // 2. At each point, average the normal vectors
        Map avNormal = new HashMap(); // maps KPoints to single Triples
        for(Iterator iter = normals.keySet().iterator(); iter.hasNext(); )
        {
            TrianglePoint t = (TrianglePoint) iter.next();
            Collection n = (Collection) normals.get(t);
            Triple x = new Triple();
            for(Iterator i2 = n.iterator(); i2.hasNext(); )
                x.add( (Triple) i2.next() );
            x.mult(1.0 / n.size()).unit();
            // Check for disagreement. Flip any normal off by > 90 deg.
            for(Iterator i2 = n.iterator(); i2.hasNext(); )
            {
                Triple y = (Triple) i2.next();
                if(x.angle(y) > 90) y.neg();
            }
            x.setXYZ(0, 0, 0);
            for(Iterator i2 = n.iterator(); i2.hasNext(); )
                x.add( (Triple) i2.next() );
            x.mult(1.0 / n.size()).unit();
            // Check for disagreement. Report any problems
            for(Iterator i2 = n.iterator(); i2.hasNext(); )
            {
                Triple y = (Triple) i2.next();
                if(x.angle(y) > 90)
                    System.err.println("Conflicting normals for {"+t+"}");
            }
            avNormal.put(t, x);
        }
        return avNormal;
    }
//}}}

//{{{ getToolsMenuItem, getHelpMenuItem, toString, onExport, isAppletSafe
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString()+"...", null, this, "onExport"));
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
    { return "#export-povray"; }

    public String toString()
    { return "POV-Ray scene"; }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExport(ActionEvent ev)
    { this.askExport(); }

    static public boolean isAppletSafe()
    { return false; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

