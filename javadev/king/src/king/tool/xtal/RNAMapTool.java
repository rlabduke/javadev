// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.xtal;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.isosurface.*;
import driftwood.util.*;
//}}}
/**
* <code>RNAMapPlugin</code> provides file/URL opening services
* to launch the RNAMapWindows that control individual maps.
* 
* Attempting to use EDMap programs as a starting point for RNA ED
* analysis. VBC 27 Oct 2003.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Apr  1 13:45:27 EST 2003
*/
public class RNAMapTool extends BasicTool implements PropertyChangeListener, ListSelectionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    JFileChooser        filechooser     = null;
    JDialog             urlchooser      = null;
    JList               urlList         = null;
    JTextField          urlField        = null;
    boolean             urlChooserOK    = false;
    JRadioButton        btnXplorType, btnOType, btnCcp4Type;
    SuffixFileFilter    omapFilter, xmapFilter, ccp4Filter, mapFilter;
    RNAMapWindow win;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public RNAMapTool(ToolBox tb)
    {
        super(tb);
        
        makeFileFilters();
	
    }
//}}}

//{{{ makeFileFilters
//##################################################################################################
    void makeFileFilters()
    {
        omapFilter = new SuffixFileFilter("O maps (DSN6/Brix)");
        omapFilter.addSuffix(".brix");
        omapFilter.addSuffix(".brix.gz");
        omapFilter.addSuffix(".dsn6");
        omapFilter.addSuffix(".dsn6.gz");
        omapFilter.addSuffix(".dn6");
        omapFilter.addSuffix(".dn6.gz");
        omapFilter.addSuffix(".omap");
        omapFilter.addSuffix(".omap.gz");
        
        xmapFilter = new SuffixFileFilter("XPLOR maps (ASCII format)");
        xmapFilter.addSuffix(".xmap");
        xmapFilter.addSuffix(".xmap.gz");
        xmapFilter.addSuffix(".xplor");
        xmapFilter.addSuffix(".xplor.gz");
        
        ccp4Filter = new SuffixFileFilter("CCP4 maps (type 2)");
        ccp4Filter.addSuffix(".ccp4");
        ccp4Filter.addSuffix(".ccp4.gz");
        ccp4Filter.addSuffix(".mbk");
        ccp4Filter.addSuffix(".mbk.gz");
        
        mapFilter = new SuffixFileFilter("All electron density maps");
        mapFilter.addSuffix(".ccp4");
        mapFilter.addSuffix(".ccp4.gz");
        mapFilter.addSuffix(".mbk");
        mapFilter.addSuffix(".mbk.gz");
        mapFilter.addSuffix(".xmap");
        mapFilter.addSuffix(".xmap.gz");
        mapFilter.addSuffix(".xplor");
        mapFilter.addSuffix(".xplor.gz");
        mapFilter.addSuffix(".brix");
        mapFilter.addSuffix(".brix.gz");
        mapFilter.addSuffix(".dsn6");
        mapFilter.addSuffix(".dsn6.gz");
        mapFilter.addSuffix(".dn6");
        mapFilter.addSuffix(".dn6.gz");
        mapFilter.addSuffix(".omap");
        mapFilter.addSuffix(".omap.gz");
        mapFilter.addSuffix(".map");
        mapFilter.addSuffix(".map.gz");
    }
//}}}

//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
        // Make accessory for file chooser
        TablePane acc = new TablePane();
        acc.weights(0,0);
        acc.add(new JLabel("Map type?"));
        acc.newRow();
        btnOType = new JRadioButton("O");
        acc.add(btnOType);
        acc.newRow();
        btnXplorType = new JRadioButton("XPLOR");
        acc.add(btnXplorType);
        acc.newRow();
        btnCcp4Type = new JRadioButton("CCP4");
        acc.add(btnCcp4Type);
        
        // Make buttons mutually exclusive
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(btnOType);
        btnGroup.add(btnXplorType);
        btnGroup.add(btnCcp4Type);
        
        // Make actual file chooser -- will throw an exception if we're running as an Applet
        filechooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        
        filechooser.setAccessory(acc);
        filechooser.addPropertyChangeListener(this);
        filechooser.addChoosableFileFilter(mapFilter);
        filechooser.setFileFilter(mapFilter);
    }
//}}}

//{{{ makeURLChooser
//##################################################################################################
    void makeURLChooser()
    {
        // Make accessory for URL chooser
        TablePane acc = new TablePane();
        acc.weights(0,0);
        acc.add(new JLabel("Map type?"));
        acc.newRow();
        btnOType = new JRadioButton("O");
        acc.add(btnOType);
        acc.newRow();
        btnXplorType = new JRadioButton("XPLOR");
        acc.add(btnXplorType);
        acc.newRow();
        btnCcp4Type = new JRadioButton("CCP4");
        acc.add(btnCcp4Type);
        
        // Make buttons mutually exclusive
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(btnOType);
        btnGroup.add(btnXplorType);
        btnGroup.add(btnCcp4Type);
        
        // Make actual URL chooser
        urlList = new FatJList(150, 12);
        JApplet applet = kMain.getApplet();
        if(applet != null)
        {
            String maps = applet.getParameter("edmapList");
            if(maps != null)
            {
                String[] maplist = Strings.explode(maps, ' ');
                urlList.setListData(maplist);
            }
        }
        urlList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        urlList.addListSelectionListener(this);
        JScrollPane listScroll = new JScrollPane(urlList);
        
        // Make an (editable) URL line
        urlField = new JTextField();
        
        // Make the command buttons
        JButton btnOK       = new JButton(new ReflectiveAction("OK", null, this, "onUrlOk"));
        JButton btnCancel   = new JButton(new ReflectiveAction("Cancel", null, this, "onUrlCancel"));
        TablePane btnPane = new TablePane();
        btnPane.center().insets(1,4,1,4);
        btnPane.add(btnOK);
        btnPane.add(btnCancel);
        
        // Put it all together in a content pane
        TablePane cp = new TablePane();
        cp.center().middle().insets(6);
        cp.add(listScroll);
        cp.add(acc);
        cp.newRow();
        cp.save().hfill(true).addCell(urlField, 2, 1).restore();
        cp.newRow();
        cp.add(btnPane, 2, 1);
        
        urlchooser = new JDialog(kMain.getTopWindow(), "ED Map URLs", true);
        urlchooser.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        urlchooser.setContentPane(cp);
        urlchooser.pack();
        urlchooser.setLocationRelativeTo(kMain.getTopWindow());
    }
//}}}

//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#rnamap-tool"; }
    
    public String toString()
    { return "Analyze RNA Maps"; }
//}}}

//{{{ start
//##################################################################################################
    public void start()
    {
        if(kMain.getKinemage() == null) return;

        try
        {
            if(kMain.getApplet() != null)   openMapURL();
            else                            openMapFile();
        }
        catch(IOException ex) // includes MalformedURLException
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(SoftLog.err);
        }
        catch(IllegalArgumentException ex)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(SoftLog.err);
        }
    }
//}}}

//{{{ openMapFile
//##################################################################################################
    void openMapFile() throws IOException
    {
        // Create file chooser on demand
        if(filechooser == null) makeFileChooser();
        
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
        {
            File f = filechooser.getSelectedFile();
            if(f != null && f.exists())
            {
                CrystalVertexSource map;
                
                if(btnOType.isSelected())
                {
                    map = new OMapVertexSource(new FileInputStream(f));
                }
                else if(btnXplorType.isSelected())
                {
                    map = new XplorVertexSource(new FileInputStream(f));
                }
                else if(btnCcp4Type.isSelected())
                {
                    map = new Ccp4VertexSource(new FileInputStream(f));
                }
                else throw new IllegalArgumentException("Map type not specified");
                
                win = new RNAMapWindow(parent, map, f.getName());
                kCanvas.repaint(); // otherwise we get partial-redraw artifacts
            }
        }
    }
//}}}

//{{{ openMapURL, onUrlCancel, onUrlOk
//##################################################################################################
    void openMapURL() throws MalformedURLException, IOException
    {
        // Create chooser on demand
        if(urlchooser == null) makeURLChooser();
        
        urlchooser.pack();
        urlchooser.setVisible(true);
        // execution halts until dialog is closed...
        
        if(urlChooserOK)
        {
            CrystalVertexSource map;
            URL mapURL = new URL(urlField.getText());
            InputStream is = new BufferedInputStream(mapURL.openStream());
            
            if(btnOType.isSelected())           map = new OMapVertexSource(is);
            else if(btnXplorType.isSelected())  map = new XplorVertexSource(is);
            else if(btnCcp4Type.isSelected())   map = new Ccp4VertexSource(is);
            else throw new IllegalArgumentException("Map type not specified");
            
            win = new RNAMapWindow(parent, map, mapURL.getFile());
            kCanvas.repaint(); // otherwise we get partial-redraw artifacts
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onUrlCancel(ActionEvent ev)
    {
        urlChooserOK = false;
        urlchooser.setVisible(false);
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onUrlOk(ActionEvent ev)
    {
        urlChooserOK = true;
        urlchooser.setVisible(false);
    }
//}}}

//{{{ propertyChange, valueChanged, click
//##################################################################################################
    public void propertyChange(PropertyChangeEvent ev)
    {
        if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(ev.getPropertyName()))
        {
            File f = (File)ev.getNewValue();
            if(f == null) {}
            else if(omapFilter.accept(f)) btnOType.setSelected(true);
            else if(xmapFilter.accept(f)) btnXplorType.setSelected(true);
            else if(ccp4Filter.accept(f)) btnCcp4Type.setSelected(true);
        }
    }
    
    /* Gets called when a new URL is picked from the list */
    public void valueChanged(ListSelectionEvent ev)
    {
        Object o = urlList.getSelectedValue();
        if(o == null) {}
        else
        {
            String name = o.toString();
                 if(omapFilter.accept(name)) btnOType.setSelected(true);
            else if(xmapFilter.accept(name)) btnXplorType.setSelected(true);
            else if(ccp4Filter.accept(name)) btnCcp4Type.setSelected(true);
            urlField.setText("http://"+name);
            
            JApplet applet = kMain.getApplet();
            if(applet != null)
            {
                try
                {
                    URL mapURL = new URL(applet.getDocumentBase(), applet.getParameter("edmapBase")+"/"+name);
                    urlField.setText(mapURL.toString());
                }
                catch(MalformedURLException ex)
                {
                    SoftLog.err.println(applet.getDocumentBase());
                    SoftLog.err.println(applet.getParameter("edmapBase"));
                    SoftLog.err.println(name);
                    ex.printStackTrace(SoftLog.err);
                }
            }
        }
    }

    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if (win != null) {
	    if((p instanceof VectorPoint)&&(win.polyIsSelected())) {
		win.polyTrack((VectorPoint) p);
	    } else if ((p != null)&&(win.planeIsSelected())) {
		win.planeTrack(p);
	    }
	}
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

