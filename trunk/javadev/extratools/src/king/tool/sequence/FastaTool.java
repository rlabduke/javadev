// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.sequence;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
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
import driftwood.moldb2.AminoAcid;
import king.tool.util.*;


//}}}
/**

*/
public class FastaTool extends BasicTool //implements ActionListener
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

    JDialog dialog;
    JButton openFileButton, colorStructure, colorProbe, exportButton;
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

    private void buildGUI() {

	dialog = new JDialog(kMain.getTopWindow(), "Fasta Tool", false);

	openFileButton = new JButton(new ReflectiveAction("Open Fasta File", null, this, "onFileOpen"));
	
	colorStructure = new JButton(new ReflectiveAction("Color Structure", null, this, "onColorStructure"));
	colorProbe = new JButton(new ReflectiveAction("Color Probe Dots", null, this, "onColorProbe"));
	exportButton = new JButton(new ReflectiveAction("Export to Fasta", null, this, "onDoAll"));
	
	
	TablePane pane = new TablePane();
	pane.add(openFileButton);
	pane.newRow();
	pane.add(colorStructure);
	pane.newRow();
	pane.add(colorProbe);
	pane.newRow();
	pane.add(exportButton);

	dialog.setContentPane(pane);
    }

//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
	
        // Make accessory for file chooser
        TablePane acc = new TablePane();

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

//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#fasta-tool"; }

    public Container getToolPanel()
    { return dialog; }
    
    public String toString()
    { return "Read Fasta"; }
//}}}

//{{{ start
//##################################################################################################
    public void start()
    {
        //if(kMain.getKinemage() == null) return;

        //try
        //{
            //if(kMain.getApplet() != null)   openMapURL();
            //else                            openMapFile();
	    buildGUI();
	    //show();
	    dialog.pack();
	    dialog.setLocationRelativeTo(kMain.getTopWindow());
	    dialog.setVisible(true);
	    kCanvas.repaint(); // otherwise we get partial-redraw artifacts
	    //openFastaFile();

	    //}
	    //catch(IOException ex) // includes MalformedURLException
	    //{
            //JOptionPane.showMessageDialog(kMain.getTopWindow(),
            //    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
            //    "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
	    //}
	    //catch(IllegalArgumentException ex)
	    //{
	    //    JOptionPane.showMessageDialog(kMain.getTopWindow(),
	    //        "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
	    //       "Sorry!", JOptionPane.ERROR_MESSAGE);
            //ex.printStackTrace(SoftLog.err);
	    //}
	//buildGUI();
	//dialog.pack();
	//dialog.setLocationRelativeTo(kMain.getTopWindow());
        //dialog.setVisible(true);
    }
//}}}

//{{{ openMapFile
//##################################################################################################
    void openFastaFile() throws IOException
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
		//buildGUI();
		//show();
		//dialog.pack();
		//dialog.setLocationRelativeTo(kMain.getTopWindow());
		//dialog.setVisible(true);
                //kCanvas.repaint(); // otherwise we get partial-redraw artifacts
            }
        }
    }
//}}}

    // scans the fasta file.
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
	    }
	} catch(IOException ex) {
	    System.out.println("IOException thrown");
	}
	prepColorArray();
	//recolor();
	//System.out.println(kMain.getKinemage().toString());
	//System.out.println(buffer);
    }

    // preps KPaint array with colors depending on alignments from fasta file
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
	    } else if (firstChar == '-') {
		firstSeq.deleteCharAt(i);
		secSeq.deleteCharAt(i);
		i--;
	    } else {
		colors[i] = KPalette.blue;
	    }
	}
    }

    // recolors all points in kinemage.
    private void recolorNoDots() {
	Kinemage kin = kMain.getKinemage();
	Iterator kinIter = kin.iterator();
	while (kinIter.hasNext()) {
	    KGroup group = (KGroup) kinIter.next();
	    if ((group.getName()).indexOf("dots") == -1) {
		Iterator groupIter = group.iterator();
		while (groupIter.hasNext()) {
		    AGE sub = (AGE) groupIter.next();
		    if (sub instanceof KSubgroup) {
			Iterator subIter = sub.iterator();
			while (subIter.hasNext()) {
			    KList list = (KList) subIter.next();
			    recolorStructure(list);
			}
		    } else if (sub instanceof KList) {
			KList list = (KList) sub;
			recolorStructure(list);
		    }
		}
	    }
	}
    }

    private void recolorDots() {
	Kinemage kin = kMain.getKinemage();
	Iterator kinIter = kin.iterator();
	while (kinIter.hasNext()) {
	    KGroup group = (KGroup) kinIter.next();
	    if ((group.getName()).indexOf("dots") > -1) {
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
    }

    private void recolor(KList list) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    String pName = point.getName();
	    int resNum = getResidueNumber(pName);
	    if (resNum > 0) {
		point.setColor(colors[resNum-1]);
	    }

	}
    }

    private void recolorStructure(KList list) {
	Iterator iter = list.iterator();
	while (iter.hasNext()) {
	    KPoint point = (KPoint) iter.next();
	    String pName = point.getName();
	    int resNum = getResidueNumber(pName);
	    if (resNum > 0) {
		KPaint color = colors[resNum-1];
		if (color.equals(KPalette.blue)) {
		    point.setColor(KPalette.bluetint);
		} else if (color.equals(KPalette.red)) {
		    point.setColor(KPalette.red);
		} else if (color.equals(KPalette.green)) {
		    point.setColor(KPalette.greentint);
		}
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
    /*
    private int getResNumberDots(String name) {
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
	
	// another pass to see if there are any AAName + int in name.
	if (parsed[1].length() > 3) {
	    String parseValue = parsed[1].substring(3);
	    if (isNumeric(parseValue)) {
		//System.out.print(parseValue + " ");
		return Integer.parseInt(parseValue);
	    }
	}

	return -1;
    }

    private int getResNumberStructure(String name) {
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
	    //System.out.println(parseValue + ", " + i);
	    if (isNumeric(parseValue)) {
		return Integer.parseInt(parseValue);
	    }
	}

	return -1;
    }
    */
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
	
	// another pass to see if there are any AAName + int in name.
	if (parsed[1].length() > 3) {
	    String parseValue = parsed[1].substring(3);
	    if (isNumeric(parseValue)) {
		//System.out.print(parseValue + " ");
		return Integer.parseInt(parseValue);
	    }
	}

	// one pass to see if there are any straight up ints in the name
	for (int i = 0; i < parsed.length; i++) {
	    String parseValue = parsed[i];
	    //System.out.println(parseValue + ", " + i);
	    if (isNumeric(parseValue)) {
		return Integer.parseInt(parseValue);
	    }
	}

	return -1;
    }

    public void onFileOpen(ActionEvent ev) {
        try
        {
            //if(kMain.getApplet() != null)   openMapURL();
            //else                            openMapFile();
	    //buildGUI();
	    //show();
	    //dialog.pack();
	    //dialog.setLocationRelativeTo(kMain.getTopWindow());
	    //dialog.setVisible(true);
	    //kCanvas.repaint(); // otherwise we get partial-redraw artifacts
	    openFastaFile();

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

	//openFastaFile();
    }

    public void onColorStructure(ActionEvent ev) {
	recolorNoDots();
	kCanvas.repaint();
    }

    public void onColorProbe(ActionEvent ev) {
	recolorDots();
	kCanvas.repaint();
    }

    public void onDoAll(ActionEvent ev) {
	if (filechooser == null) makeFileChooser();
	filechooser.setFileFilter(null);
	String output = "";
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
	{
	    File f = filechooser.getSelectedFile();
	    //System.out.println(f.getPath() + " : " + f.getName() + " : " + f.getParent());
	    File[] allFiles = f.getParentFile().listFiles();
	    for (int i = 0; i < allFiles.length; i++) {
		File pdbFile = allFiles[i];
		kMain.getKinIO().loadFile(pdbFile, null);
		output = output + ">" + pdbFile.getName() + "\n" + export(kMain.getKinemage()) + "\n";
		kMain.getStable().closeCurrent();
	    }
	}
	System.out.println(output);
    }

    public void onExport(ActionEvent ev) {
	//try {
	//    Writer w = new FileWriter(f);
	//    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    



	Kinemage kin = kMain.getKinemage();
	//Iterator iter = kin.iterator();
	//KGroup firstGroup = (KGroup) iter.next();
	//iter = firstGroup.iterator();
	
	export(kin);
	//System.out.println("exporting");
    }



    /**
     * Only does the first group, subgroup's list.
     **/
    private String export(AGE target) {
	String output = "";
	if (target instanceof KList) {
	    ListIterator iter = target.iterator();
	    int resNum = 1000000;
	    //String output = "";
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		int newResNum = KinUtil.getResNumber(pt.getName());
		if (resNum != newResNum) {
		    if (newResNum > resNum + 1) output = output.concat("\n");
		    output = output.concat(AminoAcid.translate(KinUtil.getResName(pt)));
		    //if (newResNum > resNum + 1) output = output.concat("\n");
		    resNum = newResNum;
		}
	    }
	    //System.out.println(output);
	    return output;
	    
		    
	} else {
	    Iterator iter = target.iterator();
	    //while (iter.hasNext()) {
	    return export((AGE) iter.next());
		//}
	}
	//return "null";
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

