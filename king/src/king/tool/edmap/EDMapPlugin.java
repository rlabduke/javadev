// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.edmap;
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
* <code>EDMapPlugin</code> provides file/URL opening services
* to launch the EDMapWindows that control individual maps.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Apr  1 13:45:27 EST 2003
*/
public class EDMapPlugin extends Plugin implements ListSelectionListener, KMessage.Subscriber
{
//{{{ Constants
    static final String MAPTYPE_O = "O map (DSN6/Brix)";
    static final String MAPTYPE_XPLOR = "XPLOR map (ASCII format)";
    static final String MAPTYPE_CCP4 = "CCP4 map (type 2)";
    static final String MAPTYPE_MTZ = "MTZ map coefficients";
//}}}

//{{{ CLASS: MapFileOpen
//##############################################################################
    private class MapFileOpen implements FileDropHandler.Listener
    {
        public String toString()
        { return "Open as electron density map in KiNG"; }
        
        public boolean canHandleDroppedFile(File file)
        {
            return mapFilter.accept(file);
        }
        
        public void handleDroppedFile(File f)
        {
            try
            {
                openMapFile(f);
            }
            catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    JFileChooser        filechooser     = null;
    JDialog             urlchooser      = null;
    JList               urlList         = null;
    JTextField          urlField        = null;
    boolean             urlChooserOK    = false;
    SuffixFileFilter    omapFilter, xmapFilter, ccp4Filter, mapFilter, mtzFilter;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public EDMapPlugin(ToolBox tb)
    {
        super(tb);
        makeFileFilters();
        kMain.subscribe(this);
        kMain.getFileDropHandler().addFileDropListener(new MapFileOpen());
    }
//}}}

//{{{ deliverMessage
//##################################################################################################
    public void deliverMessage(KMessage msg)
    {
        if(msg.testProg(KMessage.KING_STARTUP))
        {
            //System.err.println("Might try to load maps now");
            kMain.unsubscribe(this);
            
            JApplet applet = kMain.getApplet();
            if(applet == null) return;
            KinCanvas canvas = kMain.getCanvas();
            if(canvas == null) return;
            ToolBox toolbox = canvas.getToolBox();
            if(toolbox == null) return;
            
            // Try multiple names for this parameter
            boolean isOmap = false;
            String mapsrc = applet.getParameter("xmap");
            if(mapsrc == null) { mapsrc = applet.getParameter("omap"); isOmap = true; }
            if(mapsrc == null)  return;
        
            try
            {
                URL mapURL = new URL(applet.getDocumentBase(), mapsrc);
                
                CrystalVertexSource map;
                if(isOmap)
                { map = new OMapVertexSource(mapURL.openStream()); }
                else
                { map = new XplorVertexSource(mapURL.openStream()); }
                
                new EDMapWindow(toolbox, map, mapURL.getFile(), false);
            }
            catch(MalformedURLException ex)
            { SoftLog.err.println("<PARAM> xmap/omap specified an unresolvable URL."); }
            catch(IOException ex)
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
        ccp4Filter.addSuffix(".map");
        ccp4Filter.addSuffix(".map.gz");
        
        mtzFilter = new SuffixFileFilter("MTZ map coefficients");
        mtzFilter.addSuffix(".mtz");
        mtzFilter.addSuffix(".mtz.gz");
        
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
        mapFilter.addSuffix(".mtz");
        mapFilter.addSuffix(".mtz.gz");
    }
//}}}

//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
        // Make actual file chooser -- will throw an exception if we're running as an Applet
        filechooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        
        filechooser.addChoosableFileFilter(mapFilter);
        filechooser.addChoosableFileFilter(omapFilter);
        filechooser.addChoosableFileFilter(xmapFilter);
        filechooser.addChoosableFileFilter(ccp4Filter);
        filechooser.addChoosableFileFilter(mtzFilter);
        filechooser.setFileFilter(mapFilter);
    }
//}}}

//{{{ makeURLChooser
//##################################################################################################
    void makeURLChooser()
    {
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
        urlField = new JTextField(20);
        
        // Make the command buttons
        JButton btnOK       = new JButton(new ReflectiveAction("OK", null, this, "onUrlOk"));
        JButton btnCancel   = new JButton(new ReflectiveAction("Cancel", null, this, "onUrlCancel"));
        
        // Put it all together in a content pane
        TablePane2 cp = new TablePane2();
        cp.center().middle().insets(6).memorize();
        cp.addCell(listScroll,2,1);
        cp.newRow();
        cp.weights(0,1).addCell(new JLabel("URL:")).hfill(true).addCell(urlField);
        cp.newRow().startSubtable(2,1).center().insets(1,4,1,4).memorize();
        cp.addCell(btnOK).addCell(btnCancel).endSubtable();
        
        urlchooser = new JDialog(kMain.getTopWindow(), "ED Map URLs", true);
        urlchooser.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        urlchooser.setContentPane(cp);
        urlchooser.pack();
        urlchooser.setLocationRelativeTo(kMain.getTopWindow());
    }
//}}}

  //{{{ loadFileFromCmdline
  /** Plugins that can work on files from the king cmdline should overwrite this function */
  public void loadFileFromCmdline(ArrayList<File> files, ArrayList<String> args) {
      for (File f : files) {
        try {
          if (mapFilter.accept(f)) { // needed to filter files that could get passed to this which aren't maps
            if (kMain.getKinemage() != null) {
              if(ccp4Filter.accept(f)) {
                openMapFile(f, MAPTYPE_CCP4, args.contains("-phenix")); //auto opens ccp4 maps, for more seemless integration with phenix
              } else {
                openMapFile(f);
              }
            } else {
              JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "In order to run KiNG with a map from cmdline,\n you must also give a kin or PDB file!",
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
            }
          }
        } catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
      }
      
  }
  //}}}

//{{{ getToolsMenuItem, toString
//##################################################################################################
    /**
    * Creates a new JMenuItem to be displayed in the Tools menu,
    * which will allow the user to access function(s) associated
    * with this Plugin.
    *
    * Only one JMenuItem may be returned, but it could be a JMenu
    * that contained several functionalities under it.
    *
    * The Plugin may return null to indicate that it has no
    * associated menu item.
    */
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onOpenMap"));
    }
    
    public String toString()
    { return "Electron density maps"; }
//}}}

//{{{ getHelpMenuItem, getHelpAnchor
//##################################################################################################
    /**
    * Creates a new JMenuItem to be displayed in the Help menu,
    * which will allow the user to access help information associated
    * with this Plugin.
    *
    * Only one JMenuItem may be returned, but it could be a JMenu
    * that contained several items under it. However,
    * Plugins are encouraged to consolidate all documentation
    * into one location. The king.HTMLHelp class may be of use here.
    *
    * The Plugin may return null to indicate that it has no
    * associated menu item.
    */
    public JMenuItem getHelpMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onHelp"));
    }
    
    public String getHelpAnchor()
    { return "#edmap-plugin"; }
//}}}

//{{{ onOpenMap, askMapFormat
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onOpenMap(ActionEvent ev)
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
    
    String askMapFormat(String f) // filename or URL
    {
        Object[] choices = {MAPTYPE_O, MAPTYPE_XPLOR, MAPTYPE_CCP4, MAPTYPE_MTZ};
        String defaultChoice = MAPTYPE_O;
        if(omapFilter.accept(f))        defaultChoice = MAPTYPE_O;
        else if(xmapFilter.accept(f))   defaultChoice = MAPTYPE_XPLOR;
        else if(ccp4Filter.accept(f))   defaultChoice = MAPTYPE_CCP4;
        else if(mtzFilter.accept(f))    defaultChoice = MAPTYPE_MTZ;
        
        String choice = (String)JOptionPane.showInputDialog(kMain.getTopWindow(),
            "What format is this map in?",
            "Choose format", JOptionPane.PLAIN_MESSAGE,
            null, choices, defaultChoice);
        
        return choice;
    }
//}}}

//{{{ openMapFile
//##################################################################################################
    void openMapFile() throws IOException
    {
        // Create file chooser on demand
        if(filechooser == null) makeFileChooser();
        
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
        {
            File f = filechooser.getSelectedFile();
            openMapFile(f);
            System.setProperty("user.dir", f.getAbsolutePath());
        }
    }

    void openMapFile(File f) throws IOException {
      if(f != null && f.exists()) {
        openMapFile(f, askMapFormat(f.getName()), false);
      }
    }
    
    void openMapFile(File f, String choice, boolean phenixColors) throws IOException
    {
        if(f != null && f.exists())
        {
            //String choice = askMapFormat(f.getName());  moved this to separate function to allow for auto opening of maps
            //                                            from cmdline; for more seemless phenix integration
            CrystalVertexSource map;

            // An MTZ can hold several maps (2Fo-Fc, difference, ...) at once,
            // so it gets its own path that may open more than one window.
            if(MAPTYPE_MTZ.equals(choice))
            {
                openMtzFile(new FileInputStream(f), f.getName(), phenixColors);
                return;
            }

            if(MAPTYPE_O.equals(choice))
                map = new OMapVertexSource(new FileInputStream(f));
            else if(MAPTYPE_XPLOR.equals(choice))
                map = new XplorVertexSource(new FileInputStream(f));
            else if(MAPTYPE_CCP4.equals(choice))
                map = new Ccp4VertexSource(new FileInputStream(f));
            else throw new IllegalArgumentException("Map type not specified");

            EDMapWindow win = new EDMapWindow(parent, map, f.getName(), phenixColors);
            kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
        }
    }
//}}}

//{{{ openMtzFile, askMtzCoefficients
//##################################################################################################
    /**
    * Opens an MTZ file of map coefficients. Unlike the pre-computed map formats,
    * one MTZ typically contains coefficients for several maps -- most commonly a
    * 2Fo-Fc map and an Fo-Fc difference map. The header is read once, the user
    * picks which of the recognized coefficient sets to compute, and each one gets
    * its own map window with the appropriate contour presets.
    */
    void openMtzFile(InputStream in, String displayName, boolean phenixColors) throws IOException
    {
        // Reads the header only; no FFT is computed yet.
        MtzVertexSource probe = new MtzVertexSource(in, false);

        java.util.List<String[]> chosen = askMtzCoefficients(displayName, probe.getAvailableCoefficients());
        if(chosen.isEmpty()) return;

        Window topWin = kMain.getTopWindow();
        Cursor oldCursor = (topWin == null ? null : topWin.getCursor());
        if(topWin != null) topWin.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try
        {
            for(String[] pair : chosen)
            {
                // Each map means another FFT, but the file is only read from disk once.
                MtzVertexSource m = probe.forCoefficients(pair[0], pair[1]);
                new EDMapWindow(parent, m, displayName+" "+pair[2], phenixColors, pair[2]);
            }
        }
        finally
        {
            if(topWin != null) topWin.setCursor(oldCursor);
        }

        kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
    }

    /**
    * Asks which of the map coefficient sets found in an MTZ file should be opened.
    * The best set of each kind is checked by default, so the usual Refmac or Phenix
    * file offers a 2Fo-Fc map and a difference map without any extra clicking.
    * @return the chosen {F, PHI, kind} triples; empty if the user cancelled
    */
    java.util.List<String[]> askMtzCoefficients(String displayName, java.util.List<String[]> available)
    {
        ArrayList<String[]> chosen = new ArrayList<String[]>();

        // Nothing to choose between
        if(available.size() == 1)
        {
            chosen.add(available.get(0));
            return chosen;
        }

        // Pre-check the highest-priority set of each kind; the list is in priority order,
        // so variants like 2FOFCWT_fill are offered but not selected.
        Set<String> kindsSeen = new HashSet<String>();
        ArrayList<JCheckBox> boxes = new ArrayList<JCheckBox>();
        TablePane2 cp = new TablePane2();
        cp.left().insets(1, 4, 1, 4).memorize();
        cp.addCell(new JLabel("Maps found in "+displayName+":")).newRow();
        for(String[] pair : available)
        {
            JCheckBox box = new JCheckBox(pair[2]+"   ("+pair[0]+" / "+pair[1]+")",
                kindsSeen.add(pair[2]));
            boxes.add(box);
            cp.addCell(box).newRow();
        }

        int result = JOptionPane.showConfirmDialog(kMain.getTopWindow(), cp,
            "Choose maps to open", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(result != JOptionPane.OK_OPTION) return chosen;

        for(int i = 0; i < boxes.size(); i++)
        {
            if(boxes.get(i).isSelected()) chosen.add(available.get(i));
        }
        return chosen;
    }
//}}}

//{{{ openMapURL, onUrlCancel, onUrlOk
//##################################################################################################
    void openMapURL() throws MalformedURLException, IOException
    {
        // Create chooser on demand
        if(urlchooser == null) makeURLChooser();
        
        //urlchooser.pack(); -- gets too wide when urlField has a long URL in it
        urlchooser.setVisible(true);
        // execution halts until dialog is closed...
        
        if(urlChooserOK)
        {
            CrystalVertexSource map;
            URL mapURL = new URL(urlField.getText());
            InputStream is = new BufferedInputStream(mapURL.openStream());
            
            String choice = askMapFormat(urlField.getText());
            if(MAPTYPE_MTZ.equals(choice))
            {
                openMtzFile(is, mapURL.getFile(), false);
                return;
            }

            if(MAPTYPE_O.equals(choice))            map = new OMapVertexSource(is);
            else if(MAPTYPE_XPLOR.equals(choice))   map = new XplorVertexSource(is);
            else if(MAPTYPE_CCP4.equals(choice))    map = new Ccp4VertexSource(is);
            else throw new IllegalArgumentException("Map type not specified");

            EDMapWindow win = new EDMapWindow(parent, map, mapURL.getFile(), false);
            kMain.publish(new KMessage(kMain.getKinemage(), AHE.CHANGE_TREE_CONTENTS));
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

//{{{ valueChanged
//##################################################################################################
    /* Gets called when a new URL is picked from the list */
    public void valueChanged(ListSelectionEvent ev)
    {
        Object o = urlList.getSelectedValue();
        if(o == null) {}
        else
        {
            String name = o.toString();
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
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

