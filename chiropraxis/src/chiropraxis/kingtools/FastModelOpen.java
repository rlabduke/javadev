// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>FastModelOpen</code> is a helper component for the ModelManager.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul  7 12:01:44 EDT 2004
*/
public class FastModelOpen implements MouseListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    ModelManager2   client;
    public FatJList list;
    File            basedir = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FastModelOpen(ModelManager2 client)
    {
        super();
        this.client = client;
        
        list = new FatJList(0, 10);
        list.addMouseListener(this);
    }
//}}}

//{{{ updateList
//##############################################################################
    public void updateList(File basedir, javax.swing.filechooser.FileFilter filter)
    {
        File[] files = basedir.listFiles();
        if(files == null) return;
        
        this.basedir = basedir;
        ArrayList okFiles = new ArrayList();
        for(int i = 0; i < files.length; i++)
        {
            File f = files[i];
            if(f.isFile() && !f.isDirectory() && filter.accept(f))
                okFiles.add(f.getName());
        }
        
        //System.err.println("Files approved by FastModelOpen:");
        //System.err.println(okFiles);
        this.list.setListData(okFiles.toArray());
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Mouse listeners
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        if(ev.getClickCount() == 2 && basedir != null)
        {
            int index = list.locationToIndex(ev.getPoint());
            if(index != -1)
            {
                String filename = list.getModel().getElementAt(index).toString();
                File file = new File(basedir, filename);
                if(file.exists())
                {
                    try { client.openPDB(file); }
                    catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
                }
            }
        }
    }

    public void mouseEntered(MouseEvent ev)     {}
    public void mouseExited(MouseEvent ev)      {}
    public void mousePressed(MouseEvent ev)     {}
    public void mouseReleased(MouseEvent ev)    {}
//}}}
}//class

