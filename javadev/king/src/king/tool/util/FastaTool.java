// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;
import king.*;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
//import java.beans.*;
import java.io.*;
//import java.net.*;
//import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
//import javax.swing.event.*;
import driftwood.gui.*;
//import driftwood.isosurface.*;
import driftwood.util.*;
//}}}
/**

*/
public class FastaTool extends BasicTool //implements PropertyChangeListener
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
    //JRadioButton        btnXplorType, btnOType, btnCcp4Type;
    //SuffixFileFilter    omapFilter, xmapFilter, ccp4Filter, mapFilter;
    SuffixFileFilter fastaFilter;
    HashMap nameMap = null;
    KPaint[] colors = null;
    //RNAMapWindow win;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public FastaTool(ToolBox tb)
    {
        super(tb);
        
        makeFileFilters();
	
    }
//}}}

//{{{ makeFileFilters
//##################################################################################################
    void makeFileFilters()
    {
	fastaFilter = new SuffixFileFilter("Fasta Files");
	fastaFilter.addSuffix(".faa");
	

    }
//}}}

//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
	
        // Make accessory for file chooser
        TablePane acc = new TablePane();
        /*
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
        */

        // Make actual file chooser -- will throw an exception if we're running as an Applet
        filechooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        
        filechooser.setAccessory(acc);
        //filechooser.addPropertyChangeListener(this);
        filechooser.addChoosableFileFilter(fastaFilter);
        filechooser.setFileFilter(fastaFilter);
    }
//}}}
/*
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
*/
//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#fasta-tool"; }
    
    public String toString()
    { return "Read Fasta"; }
//}}}

//{{{ start
//##################################################################################################
    public void start()
    {
        if(kMain.getKinemage() == null) return;

        try
        {
            //if(kMain.getApplet() != null)   openMapURL();
            //else                            openMapFile();
	    openMapFile();
        }
        catch(IOException ex) // includes MalformedURLException
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
        }
        catch(IllegalArgumentException ex)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
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
		FileReader reader = new FileReader(f);
		scanFile(reader);
		/*
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
		*/
                kCanvas.repaint(); // otherwise we get partial-redraw artifacts
            }
        }
    }
//}}}

    private void scanFile(FileReader reader) {
	//StringBuffer name = null;
	StringBuffer buffer = null;
	boolean forName = false;
	nameMap = new HashMap();
	try {
	    int readInt = 0;
	    while ((readInt = reader.read()) != -1) {
		//int readInt = reader.read();
		
		if (readInt == '>') {
		    forName = true;
		    buffer = new StringBuffer();
		} else if (readInt == '\n') {
		    if (forName) {
			String name = buffer.toString();
			buffer = new StringBuffer();
			nameMap.put(name, buffer);
			forName = false;
		    }
		} else {
		    buffer.append((char)readInt);
		}

		//if (readInt == '-') {
		//    System.out.print("F");
		//} else {
		//    System.out.print("T");
		//}
		//System.out.println('>');
		//System.out.println(new Character(readInt));
		//System.out.println(readInt);
		//System.out.println(readInt=='>');
		
	    }
	} catch(IOException ex) {
	    System.out.println("IOException thrown");
	}
	prepColorArray();
	recolor();
	//System.out.println(kMain.getKinemage().toString());
	//System.out.println(buffer);
    }

    private void prepColorArray() {
	Collection aligns = nameMap.values();
	Iterator iter = aligns.iterator();
	StringBuffer firstSeq = (StringBuffer) iter.next();
	StringBuffer secSeq = (StringBuffer) iter.next();
	//Array is a little longer than necessary.
	colors = new KPaint[firstSeq.length()];
	for (int i = 0; i < firstSeq.length(); i++) {
	    char firstChar = firstSeq.charAt(i);
	    char secChar = secSeq.charAt(i);
	    if (firstChar == secChar) {
		colors[i] = KPalette.green;
	    } else if (secChar == '-') {
		colors[i] = KPalette.red;
	    } else {
		colors[i] = KPalette.blue;
	    }
	}
    }

    private void recolor() {
	Kinemage kin = kMain.getKinemage();
	Iterator kinIter = kin.iterator();
	while (kinIter.hasNext()) {
	    KGroup group = (KGroup) kinIter.next();
	    Iterator groupIter = group.iterator();
	    while (groupIter.hasNext()) {
		AGE sub = (AGE) groupIter.next();
		if (sub instanceof KSubgroup) {
		    Iterator subIter = sub.iterator();
		    while (subIter.hasNext()) {
			KList list = (KList) subIter.next();
			recolor(list);
		    }
		} else if (sub instanceof KList) {
		    KList list = (KList) sub;
		    recolor(list);
		}
	    }
	}
    }

    private void recolor(KList list) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    String pName = point.getName();
	    if (getResidueNumber(pName) > 0) {
		point.setColor(colors[getResidueNumber(pName)-1]);
	    }

	}
    }

    public boolean isNumeric(String s) {
	try {
	    Integer.parseInt(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    private int getResidueNumber(String name) {
	String[] uncleanParsed = name.split(" ");
	String[] parsed = new String[uncleanParsed.length];
        int i2 = 0;
	// To clean out the empty strings from the split name.
	for (int i = 0; i < uncleanParsed.length; i++) {
	    String unclean = uncleanParsed[i];
	    if ((!unclean.equals(""))&&(!unclean.equals(" "))) {
		parsed[i2] = unclean;
		i2++;
	    }
	}
	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    if (isNumeric(parseValue)) {
		return Integer.parseInt(parseValue);
	    }
	}

	// another pass to see if there are any AAName + int in name.
	if (parsed[1].length() > 3) {
	    String parseValue = parsed[1].substring(3);
	    if (isNumeric(parseValue)) {
		//System.out.print(parseValue + " ");
		return Integer.parseInt(parseValue);
	    }
	}
	System.out.print(parsed[1] + ":");
	//System.out.print(":");
	return -1;
    }

	     

/*
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
    
    // Gets called when a new URL is picked from the list 
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
*/
//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

