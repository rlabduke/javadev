// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.util.*;
import java.text.DecimalFormat;
import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>DiveExport</code> writes simple text files that Jeremy's VirTools
* script can import into the Duke Immersive Virtual Environment (cave).
*
* Only visible balls/sphere and vectors are output.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb  3 16:28:53 EST 2005
*/
public class DiveExport extends Plugin
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.0###");
    static final Pattern atomNamePattern = Pattern.compile(":(..)");
//}}}

//{{{ Variable definitions
//##############################################################################
    JFileChooser        chooser = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DiveExport(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        if(currdir != null) chooser.setCurrentDirectory(new File(currdir));
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
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
                    save(out, kMain.getKinemage());
                    out.close();
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
//##############################################################################
    public void save(PrintStream out, Kinemage kin) throws IOException
    {
        if(kin == null) return;
        
        for(Iterator gi = kin.iterator(); gi.hasNext(); )
        {
            KGroup group = (KGroup) gi.next();
            if(!group.isOn()) continue;
            for(Iterator si = group.iterator(); si.hasNext(); )
            {
                KSubgroup subgroup = (KSubgroup) si.next();
                if(!subgroup.isOn()) continue;
                for(Iterator li = subgroup.iterator(); li.hasNext(); )
                {
                    KList list = (KList) li.next();
                    if(!list.isOn()) continue;
                    for(Iterator pi = list.iterator(); pi.hasNext(); )
                    {
                        KPoint pt = (KPoint) pi.next();
                        if(!pt.isOn()) continue;
                        savePoint(out, pt);
                    }
                }
            }
        }
        out.flush();
    }
//}}}

//{{{ savePoint
//##############################################################################
    /** Backslash delimited, must invert X-axis b/c DiVE has opposite handedness? */
    public void savePoint(PrintStream out, KPoint p) throws IOException
    {
        KList list = (KList) p.getOwner();
        if(list == null) return;
        
        if(p instanceof BallPoint || p instanceof SpherePoint)
        {
            out.println("ball_"+getPtName(p)+"\\"+df.format(-p.getX())+"\\"+df.format(p.getY())+"\\"+df.format(p.getZ())+"\\"+p.getName()+"\\"+df.format(getRadius(list, p)));
        }
        else if(p instanceof DotPoint)
        {
            out.println("dot_"+getPtName(p)+"\\"+df.format(-p.getX())+"\\"+df.format(p.getY())+"\\"+df.format(p.getZ())+"\\"+p.getName()+"\\"+getWidth(list, p));
        }
        else if(p instanceof LabelPoint)
        {
            out.println("label_"+getPtName(p)+"\\"+df.format(-p.getX())+"\\"+df.format(p.getY())+"\\"+df.format(p.getZ())+"\\"+p.getName());
        }
        else if(p instanceof VectorPoint || p instanceof ArrowPoint)
        {
            KPoint q = p.getPrev();
            if(q != null)
            {
                out.println("vector_"+getPtName(p)+"\\"+df.format(-q.getX())+"\\"+df.format(q.getY())+"\\"+df.format(q.getZ())+"\\"+
                    df.format(-p.getX())+"\\"+df.format(p.getY())+"\\"+df.format(p.getZ())+"\\"+p.getName()+"\\"+getWidth(list, p));
            }
        }
        else if(p instanceof TrianglePoint)
        {
            KPoint q = p.getPrev();
            if(q != null)
            {
                KPoint r = q.getPrev();
                if(r != null)
                {
                    out.println("triangle_"+getPtName(p)+"\\"+df.format(-r.getX())+"\\"+df.format(r.getY())+"\\"+df.format(r.getZ())+"\\"+
                        df.format(-q.getX())+"\\"+df.format(q.getY())+"\\"+df.format(q.getZ())+"\\"+
                        df.format(-p.getX())+"\\"+df.format(p.getY())+"\\"+df.format(p.getZ())+"\\"+p.getName());
                }
            }
        }
        //else
    }
//}}}

//{{{ getPtName
//##############################################################################
    /** Extracts putative atom name for kins made by Molikin */
    String getPtName(KPoint p)
    {
        /*
        Matcher m = atomNamePattern.matcher(p.getName());
        if(m.find() && m.groupCount() >= 1)
        {
            return m.group(1).trim();
        }
        else return "X";
        */
        KPaint color = p.getDrawingColor();
        while(color.isAlias()) color = color.getAlias();
        return color.toString();
    }
    
    int getWidth(KList list, KPoint p)
    {
        int w = p.getWidth();
        if(w != 0) return w;
        else return list.getWidth();
    }
    
    float getRadius(KList list, KPoint p)
    {
        float r = p.getRadius();
        if(r != 0) return r;
        else return list.getRadius();
    }
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
    { return "DIVE / VirTools"; }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExport(ActionEvent ev)
    { this.askExport(); }

    static public boolean isAppletSafe()
    { return false; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

