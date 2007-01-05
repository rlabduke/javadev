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
import driftwood.util.SoftLog;
//}}}
/**
* <code>Vrml97Writer</code> writes the currently visible kinemage
* as a VRML97 (a.k.a. VRML 2.0) files.
*
* <p>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Oct  3 09:51:11 EDT 2002
*/
public class Vrml97Writer extends Plugin
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.####");
//}}}

//{{{ Variable definitions
//##################################################################################################
    PrintWriter out = null;
    String lastPointID = null;
    JFileChooser fileChooser;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Vrml97Writer(ToolBox tb)
    {
        super(tb);
        fileChooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) fileChooser.setCurrentDirectory(new File(currdir));
    }
//}}}

//{{{ save
//##################################################################################################
    /** Writes out all the currently open kinemages */
    public void save(Writer destination)
    {
        Kinemage    kin     = kMain.getKinemage();
        
        out = new PrintWriter(new BufferedWriter(destination));
        out.println("#VRML V2.0 utf8");
        
        defineConstants();
        writeKinemage(kin, 1);
        
        out.flush();
    }
//}}}

//{{{ writeKinemage
//##################################################################################################
    void writeKinemage(Kinemage kin, int index)
    {
        if(!kin.getName().startsWith(KinfileParser.DEFAULT_KINEMAGE_NAME))
        { out.println("# @title {"+kin.getName()+"}"); }
        if(kin.atWhitebackground)   out.println("# @whitebackground");
        if(kin.atPdbfile != null)   out.println("# @pdbfile {"+kin.atPdbfile+"}");
        
        out.println("# BEGIN @kinemage "+index);
        out.println("Transform { children [");
        for(Iterator iter = kin.iterator(); iter.hasNext(); )
        {
            KGroup group = (KGroup)iter.next();
            writeGroup(group, kin);
        }
        out.println("] }");
        out.println("# END @kinemage "+index);
    }
//}}}

//{{{ writeGroup
//##################################################################################################
    void writeGroup(KGroup group, Kinemage kin)
    {
        // Only write out visible groups!
        if(! group.isOn()) return;
        
        out.println(" # BEGIN @group {"+group.getName()+"}");
        out.println(" Transform { children [");
        for(Iterator iter = group.iterator(); iter.hasNext(); )
        {
            KSubgroup subgroup = (KSubgroup)iter.next();
            writeSubgroup(subgroup, kin);
        }
        out.println(" ] }");
        out.println(" # END @group {"+group.getName()+"}");
    }
//}}}

//{{{ writeSubgroup
//##################################################################################################
    void writeSubgroup(KGroup subgroup, Kinemage kin)
    {
        // Only write out visible subgroups!
        if(! subgroup.isOn()) return;
        
        out.println("  # BEGIN @subgroup {"+subgroup.getName()+"}");
        out.println("  Transform { children [");
        for(Iterator iter = subgroup.iterator(); iter.hasNext(); )
        {
            KList list = (KList)iter.next();
            writeList(list, kin);
        }
        out.println("  ] }");
        out.println("  # END @subgroup {"+subgroup.getName()+"}");
    }
//}}}

//{{{ writeList
//##################################################################################################
    void writeList(KList list, Kinemage kin)
    {
        // Only write out visible lists!
        if(! list.isOn()) return;
        
        out.println("   # BEGIN @"+list.getType()+"list {"+list.getName()+"}");
        out.println("   Transform { children [");
        lastPointID = null;
        for(Iterator iter = list.iterator(); iter.hasNext(); )
        {
            KPoint point = (KPoint)iter.next();
            if(point instanceof DotPoint)           writeDotPoint(point, list, kin);
            else if(point instanceof BallPoint)     writeBallPoint(point, list, kin);
            // else ignore points we don't know how to render
        }
        out.println("   ] }");
        out.println("   # END @"+list.getType()+"list {"+list.getName()+"}");
    }
//}}}

//{{{ writeDotPoint
//##################################################################################################
    void writeDotPoint(KPoint point, KList list, Kinemage kin)
    {
        String xyz, color;
        xyz = df.format(point.getOrigX())+" "+df.format(point.getOrigY())+" "+df.format(point.getOrigZ());
        if(point.getColor() != null)    color = point.getColor().toString();
        else                            color = list.getColor().toString();
        
        out.println("# "+point.getName());
        out.println(" Transform { translation "+xyz+" children [");
        out.println(" Shape { geometry USE kpDot appearance USE kc_"+color+" } ] }");
    }
//}}}

//{{{ writeBallPoint
//##################################################################################################
    void writeBallPoint(KPoint point, KList list, Kinemage kin)
    {
        String xyz, color;
        xyz = df.format(point.getOrigX())+" "+df.format(point.getOrigY())+" "+df.format(point.getOrigZ());
        if(point.getColor() != null)    color = point.getColor().toString();
        else                            color = list.getColor().toString();

        BallPoint ball = (BallPoint)point;
        float radius = ball.r0;
        if(radius <= 0) radius = list.getRadius();
        
        out.println("# "+point.getName());
        out.println(" Transform { translation "+xyz+" children [");
        out.println(" Shape { geometry Sphere { radius "+df.format(radius)+" } appearance USE kc_"+color+" } ] }");
    }
//}}}

//{{{ writeView(), writeMaster()
//##################################################################################################
    void writeView(KView view, int index)
    {
        /* NOT IMPLEMENTED * /
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
        /* NOT IMPLEMENTED */
    }
    
    void writeMaster(MasterGroup master)
    {
        /* NOT IMPLEMENTED * /
        out.print("@master {"+master.getName()+"}");
        //if(! master.isOn())         out.print(" off");
        if(! master.hasButton())    out.print(" nobutton");
        out.println();
        
        if(master.pm_mask != 0)
        {
            out.println("@pointmaster '"+MasterGroup.fromPmBitmask(master.pm_mask)+"' {"+master.getName()+"}");
        }
        /* NOT IMPLEMENTED */
    }
///}}}

//{{{ defineConstants
//##################################################################################################
    void defineConstants()
    {
        out.println("############################################################");
        out.println("# Definitions of geometric primitives");
        out.println("DEF kpDot Sphere { radius 0.1 }");
        out.println("############################################################");
        out.println("# Definitions of standard colors");
        out.println("DEF kc_red Appearance { material Material { diffuseColor 1 0 0 } }");
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
    { return "#export-vrml97"; }

    public String toString()
    { return "VRML 2.0 (97)"; }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExport(ActionEvent ev)
    {
        if(fileChooser.showSaveDialog(kMain.getTopWindow()) == fileChooser.APPROVE_OPTION)
        {
            File f = fileChooser.getSelectedFile();
            if( !f.exists() ||
                JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                    "This file exists -- do you want to overwrite it?",
                    "Overwrite file?", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION )
            {
                try
                {
                    Writer w = new BufferedWriter(new FileWriter(f));
                    this.save(w);
                    w.close();
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file.",
                        "Sorry!",
                        JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
            }
        }
    }

    static public boolean isAppletSafe()
    { return false; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

