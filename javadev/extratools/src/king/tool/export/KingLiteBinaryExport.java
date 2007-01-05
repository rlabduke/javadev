// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
import king.core.*;
import king.points.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>KingLiteBinaryExport</code> writes binary "kinemage" files for the
* J2ME application KingLite.
* The format is simple: each point is four ints -- x, y, z, multi. Repeat as needed.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb  3 16:28:53 EST 2005
*/
public class KingLiteBinaryExport extends Plugin
{
//{{{ Constants
    static final String[] colors = { "red", "orange", "gold", "yellow", "lime", "green", "sea",
        "cyan", "sky", "blue", "purple", "magenta", "hotpink", "pink", "peach", "lilac", "pinktint",
        "peachtint", "yellowtint", "greentint", "bluetint", "lilactint", "white", "gray", "brown" };
    static final int TYPE_VECTOR_NODRAW = 0;
    static final int TYPE_VECTOR_DRAW1  = 1;
    static final int TYPE_VECTOR_DRAW2  = 2;
    static final int TYPE_DOT_SMALL     = 3;
    static final int TYPE_DOT_MEDIUM    = 4;
    static final int TYPE_DOT_LARGE     = 5;
    static final int TYPE_BALL          = 6;
    static final int TYPE_LABEL         = 7;

    // Bit that marks entries as kinemage "entities" (NEVER set for points)
    static final int ENTITY_BIT         = 1<<31;
    static final int ENTITY_TYPE_SHIFT  = 16;
    static final int ENTITY_TYPE_MASK   = 0x7fff;
    static final int ENTITY_LEN_SHIFT   = 0;
    static final int ENTITY_LEN_MASK    = 0xffff;
    
    // Types of entities
    static final int ENT_NULL           = 0;
    static final int ENT_GROUP          = 1;
    static final int ENT_SUBGROUP       = 2;
    static final int ENT_LIST           = 3;
    
    // Group properties
    static final int ANIMATE_BIT        = 1;
//}}}

//{{{ Variable definitions
//##############################################################################
    JFileChooser        chooser = null;
    SuffixFileFilter    kltFilter, pdbFilter;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KingLiteBinaryExport(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ buildChooser
//##############################################################################
    private void buildChooser()
    {
        kltFilter = new SuffixFileFilter("KingLite binary kinemage");
        kltFilter.addSuffix(".klt");
        pdbFilter = new SuffixFileFilter("Palm database for IBM WebSphere/J9 VM");
        pdbFilter.addSuffix(".pdb");
        
        String currdir = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(pdbFilter);
        chooser.addChoosableFileFilter(kltFilter);
        chooser.setFileFilter(pdbFilter);
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
            boolean doPalmHeader = (chooser.getFileFilter() == pdbFilter);
            if(doPalmHeader)
            {
                if(!pdbFilter.accept(f) &&
                JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                "This file has the wrong extension. Append '.pdb' to the name?",
                "Fix extension?", JOptionPane.YES_NO_OPTION))
                    f = new File(f+".pdb");
            }
            else
            {
                if(!kltFilter.accept(f) &&
                JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                "This file has the wrong extension. Append '.klt' to the name?",
                "Fix extension?", JOptionPane.YES_NO_OPTION))
                    f = new File(f+".klt");
            }

                
            if(!f.exists() ||
            JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            "This file exists -- do you want to overwrite it?",
            "Overwrite file?", JOptionPane.YES_NO_OPTION))
            {
                try
                {
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                    String palmName = f.getName();
                    if(palmName.toLowerCase().endsWith(".pdb")) palmName = palmName.substring(0, palmName.length()-4);
                    if(doPalmHeader) writePalmHeader(out, palmName, "Created by KiNG "+kMain.getPrefs().getString("version"));
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

//{{{ writePalmHeader
//##############################################################################
    public void writePalmHeader(DataOutputStream out, String kinName, String aboutText) throws IOException
    {
        // Palm DataBase (PDB) file based on http://www.palmos.com/dev/support/docs/fileformats/Intro.html
        // 32 bytes: Database name
        String dbname = "J9RMS "+System.currentTimeMillis();
        out.writeBytes(dbname);
        for(int i = dbname.length(); i < 32; i++) out.writeByte(0);
        // Data I don't care about: flags and date stamps
        int[] hdr1 = {
            // attributes/version   creation date           modification date       last backup date
            0x00, 0x08, 0x00, 0x00, 0x42, 0x05, 0xdc, 0x5c, 0x42, 0x05, 0xdc, 0x9a, 0x42, 0x05, 0xdc, 0x9a,
            // modification #       appInfoID               sortInfoID              type ("j9rs")
            0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x68, 0x00, 0x00, 0x00, 0x00, 0x6a, 0x39, 0x72, 0x73,
            // creator ("KngL")     unique ID seed          nextRecordList ID       # records 
            0x4b, 0x6e, 0x67, 0x4c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03
        };
        for(int i = 0; i < hdr1.length; i++) out.writeByte(hdr1[i]);
        // Now three record entries. We need to know size of data items 1 and 2 first...
        int baseOffset = 0x00000100; 
        out.writeInt(baseOffset);                                       // offset to start of first record
        out.writeInt(0x00000001);                                       // UID for this record
        out.writeInt(baseOffset+kinName.length());                      // offset to start of second record
        out.writeInt(0x00000002);                                       // UID for this record
        out.writeInt(baseOffset+kinName.length()+aboutText.length());   // offset to start of third record
        out.writeInt(0x00000003);                                       // UID for this record
        // More data I don't care about: the app info block
        int[] hdr2 = {
            //                                  empty?       "KingLite" followed by nulls (32b total)   
                                                0x00, 0x00,  0x4b, 0x69, 0x6e, 0x67, 0x4c, 0x69, 0x74, 0x65,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            //                                               unknown...
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  0x01, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00,
            // unknown...     (these bytes vary...)   (this one doesn't!)
            0x00, 0x01, 0x01, 0xe7, 0xf7, 0x71, 0x0c, 0x16
        };
        for(int i = 0; i < hdr2.length; i++) out.writeByte(hdr2[i]);
        // Now the (Java) database name, followed by some nulls, for 32 (16 bit) chars
        String storename = Long.toString(System.currentTimeMillis(), 16)+kinName;
        if(storename.length() > 32) storename = storename.substring(0, 32);
        out.writeChars(storename);
        for(int i = storename.length(); i < 32; i++) out.writeChar( (char)0 );
        int[] hdr3 = { //application name, etc.
                                                             0x00, 0x00, 0x00, 0x24, 0x00, 0x4b, 0x00, 0x69,
            0x00, 0x6e, 0x00, 0x67, 0x00, 0x4c, 0x00, 0x69,  0x00, 0x74, 0x00, 0x65, 0x00, 0x2d, 0x00, 0x49,
            0x00, 0x61, 0x00, 0x6e, 0x00, 0x20, 0x00, 0x44,  0x00, 0x61, 0x00, 0x76, 0x00, 0x69, 0x00, 0x73
        };
        for(int i = 0; i < hdr3.length; i++) out.writeByte(hdr3[i]);
        // Raw data:
        out.writeBytes(kinName);
        out.writeBytes(aboutText);
    }
//}}}

//{{{ save
//##############################################################################
    public void save(DataOutputStream out, Kinemage kin) throws IOException
    {
        if(kin == null) return;
        
        for(Iterator gi = kin.iterator(); gi.hasNext(); )
        {
            KGroup group = (KGroup) gi.next();
            if(!group.isOn()) continue;
            if(group.hasButton()) saveGroup(out, group);
            for(Iterator si = group.iterator(); si.hasNext(); )
            {
                KSubgroup subgroup = (KSubgroup) si.next();
                if(!subgroup.isOn()) continue;
                if(subgroup.hasButton() && !group.isDominant()) saveGroup(out, subgroup);
                for(Iterator li = subgroup.iterator(); li.hasNext(); )
                {
                    KList list = (KList) li.next();
                    if(!list.isOn()) continue;
                    if(list.hasButton() && !subgroup.isDominant() && !group.isDominant()) saveGroup(out, list);
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

//{{{ saveGroup
//##############################################################################
    void saveGroup(DataOutputStream out, AGE age) throws IOException
    {
        int entityType = 0;
        int flags = 0;
        
        if(age instanceof KGroup)
        {
            entityType = ENT_GROUP;
            if(((KGroup)age).isAnimate()) flags |= ANIMATE_BIT;
        }
        else if(age instanceof KSubgroup)
        {
            entityType = ENT_SUBGROUP;
        }
        else if(age instanceof KList)
        {
            entityType = ENT_LIST;
        }
        
        String name = age.getName();
        int len = 4 + 4+ 2*name.length(); // length in bytes
        int ent = ENTITY_BIT | (entityType<<ENTITY_TYPE_SHIFT) | (len<<ENTITY_LEN_SHIFT);
        
        out.writeInt(ent);
        out.writeInt(flags);
        out.writeInt(name.length());
        out.writeChars(name);
    }
//}}}

//{{{ savePoint
//##############################################################################
    public void savePoint(DataOutputStream out, KPoint p) throws IOException
    {
        KList list = (KList) p.getParent();
        if(list == null) return;
        
        int multi = 0;
        if(p instanceof DotPoint)
        {
            int width = list.getWidth();
            if(width <= 2)                  multi |= TYPE_DOT_SMALL;
            else if(width <= 4)             multi |= TYPE_DOT_MEDIUM;
            else                            multi |= TYPE_DOT_LARGE;
        }
        else if(p instanceof BallPoint)
        {
            multi |= TYPE_BALL;
            float radius = p.getRadius();
            if(radius == 0) radius = list.getRadius();
            int r = (int) Math.round((radius * 1000.0) / 8.0);
            multi |= ((r & 0xffff) << 8);
        }
        else if(p instanceof VectorPoint)
        {
            int w = p.getWidth();
            if(w == 0) w = list.getWidth();
            
            if(p.isBreak())                 multi |= TYPE_VECTOR_NODRAW;
            else if(w <= 3)                 multi |= TYPE_VECTOR_DRAW1;
            else                            multi |= TYPE_VECTOR_DRAW2;
        }
        else if(p instanceof LabelPoint)   multi |= TYPE_LABEL;
        else return;
        
        // Without this step, @colorset renders as 31 (white)
        KPaint paint = p.getDrawingColor(kCanvas.getEngine());
        while(paint.isAlias()) paint = paint.getAlias();
        String color = paint.toString();
        int colorIndex = 31;
        for(int i = 0; i < colors.length; i++)
        {
            if(colors[i].equals(color)) colorIndex = i;
        }
        multi |= (colorIndex << 3);
        
        // Values b/t +/- 0.001 and +/- 1,000,000
        int x = (int)Math.round(p.getX() * 1000);
        int y = (int)Math.round(p.getY() * 1000);
        int z = (int)Math.round(p.getZ() * 1000);
        
        out.writeInt(multi);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        
        if(p instanceof LabelPoint)
        {
            String s = p.getName();
            out.writeInt(s.length());
            out.writeChars(s);
        }
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
    { return "KingLite (binary)"; }

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

