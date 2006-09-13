// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.text.DecimalFormat;

import driftwood.util.*;
import driftwood.gui.*;
import king.tool.util.KinUtil;

public class LoopTool extends BasicTool {

//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("00.0");
//}}}

//{{{ Variable definitions
//##############################################################################
    HashSet keptSet; 
    HashMap startColorMap, endColorMap;
    HashMap pdbKeepMap; // full name -> hashset of integers to keep
    HashMap pdbMultiLoopMap; // full name (pdbName + startResidue#) -> pdbName
    HashMap chainMap; // fullName -> chainID
    TreeMap bFactorMap; // bfactor + pdbName -> fullName (to do files in order of bfactor)
    JButton delButton, keepButton, removeButton, openButton, delFromFileButton, doAllButton;
    JTextField lowNumField, highNumField;
    TablePane pane;
    JList keptList;
    DefaultListModel listModel;
    JFileChooser filechooser;


    public LoopTool(ToolBox tb) {
	super(tb);
	//buildGUI();
    }
    
//{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {

	dialog = new JDialog(kMain.getTopWindow(), "Loops", false);
	lowNumField = new JTextField("", 5);
	highNumField = new JTextField("", 5);
	listModel = new DefaultListModel();
	keptList = new JList(listModel);

	keepButton = new JButton(new ReflectiveAction("Keep!", null, this, "onKeep"));
	delButton = new JButton(new ReflectiveAction("Delete rest!", null, this, "onDelete"));
	removeButton = new JButton(new ReflectiveAction("Remove last", null, this, "onRemove"));

	openButton = new JButton(new ReflectiveAction("Open CSV file", null, this, "onOpenFile"));
	delFromFileButton = new JButton(new ReflectiveAction("Delete from file", null, this, "onDeleteFromFile"));
	doAllButton = new JButton(new ReflectiveAction("Do ALL from file", null, this, "onDoAll"));

	pane = new TablePane();
	pane.newRow();
	pane.add(lowNumField);
	pane.add(highNumField);
	pane.add(keepButton);
	pane.add(delButton);
	pane.newRow().save().hfill(true).vfill(true);
	pane.add(new JScrollPane(keptList), 4, 1);
	pane.newRow().restore();
	pane.add(removeButton, 2, 1);
	pane.add(openButton, 1, 1);
	pane.add(delFromFileButton, 1, 1);
	pane.newRow();
	pane.add(doAllButton, 3, 1);

        dialog.addWindowListener(this);

	dialog.setContentPane(pane);
    }

//}}}

    public void start() {
	//if (kMain.getKinemage() == null) return;
	buildGUI();
	keptSet = new HashSet();
	pdbKeepMap = new HashMap();
	startColorMap = new HashMap();
	endColorMap = new HashMap();
	bFactorMap = new TreeMap();
	pdbMultiLoopMap = new HashMap();
	chainMap = new HashMap();
	//colorator = new RecolorNonRibbon();
	show();
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
        //filechooser.addChoosableFileFilter(fastaFilter);
        //filechooser.setFileFilter(fastaFilter);
    }
//}}}

    public void onOpenFile(ActionEvent ev) {
	if (filechooser == null) makeFileChooser();

        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
	{
	    try {
		File f = filechooser.getSelectedFile();
		if(f != null && f.exists()) {
		    dialog.setTitle(f.getName());
		    BufferedReader reader = new BufferedReader(new FileReader(f));
		    String line;
		    try {
			while ((line = reader.readLine())!=null) {
			    String[] exploded = Strings.explode(line, ',', false, true);
			    String pdbName = exploded[0];
			    pdbName = pdbName.toLowerCase();
			    HashSet value = new HashSet();
			    //keepRange(value, Integer.parseInt(exploded[1])-8, Integer.parseInt(exploded[2])+8);
			    //int startRes = Integer.parseInt(exploded[1])-8;
			    keepRange(value, Integer.parseInt(exploded[1]), Integer.parseInt(exploded[2]));
			    int startRes = Integer.parseInt(exploded[1]);
			    String fullName = pdbName + "-" + Integer.toString(startRes);
			    //HashSet fullSet = new HashSet();
			    //fullSet.add(fullName);
			    pdbMultiLoopMap.put(fullName, pdbName);
			    pdbKeepMap.put(fullName, value);
			    HashSet start = new HashSet();
			    HashSet end = new HashSet();
			    if (startColorMap.containsKey(pdbName)) {
				start = (HashSet) startColorMap.get(pdbName);
				end = (HashSet) endColorMap.get(pdbName);
			    }
			    int loopStart = Integer.parseInt(exploded[1]);
			    int loopEnd = Integer.parseInt(exploded[2]);
			    for (int i = loopStart-8; i <= loopStart; i++) {
				int resToColor = i;
				start.add(new Integer(resToColor));
				//start.add(new Integer(exploded[1]));
			    }
			    //start.add(new Integer(Integer.parseInt(exploded[1])+1));
			    for (int i = loopEnd; i <= loopEnd + 8; i++) {
				int resToColor = i;
				end.add(new Integer(resToColor));
				//end.add(new Integer(exploded[2]));
			    }
			    //end.add(new Integer(Integer.parseInt(exploded[2])+1));
			    startColorMap.put(pdbName, start);
			    endColorMap.put(pdbName, end);
			    bFactorMap.put(df.format(Double.parseDouble(exploded[5]))+pdbName, fullName);
			    String chainID = exploded[3].toLowerCase();
			    chainMap.put(fullName, chainID);
			    //}
			}
		    } catch (IOException ex) {
			JOptionPane.showMessageDialog(kMain.getTopWindow(),
						      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
						      "Sorry!", JOptionPane.ERROR_MESSAGE);
			//ex.printStackTrace(SoftLog.err);
		    }
		    
		    kCanvas.repaint(); // otherwise we get partial-redraw artifacts
		}
	    } 

	    catch(IOException ex) { // includes MalformedURLException 
		JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
	    } catch(IllegalArgumentException ex) {
		JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
	    }
	}
    }

    public void onDoAll(ActionEvent ev) {
	if (filechooser == null) makeFileChooser();

        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
	{
	    //try {
		File f = filechooser.getSelectedFile();
		System.out.println(f.getPath() + " : " + f.getName() + " : " + f.getParent());
		File[] allFiles = f.getParentFile().listFiles();
		File saveLoc = new File(f.getParentFile(), "loops");
		HashMap fileMap = new HashMap();
		for (int i = 0; i < allFiles.length; i++) {
		    File pdbFile = allFiles[i];
		    String pdbName = pdbFile.getName().substring(0,4).toLowerCase();
		    fileMap.put(pdbName, pdbFile);
		    //System.out.println(pdbFile.getPath() + " : " + pdbName + " : " + pdbFile.getParent());
		    //System.out.println(pdbKeepMap.containsKey(pdbName.toLowerCase()));
		}
		Collection values = bFactorMap.values();
		Iterator iter = values.iterator();
		while (iter.hasNext()) {
		    String fullName = (String) iter.next();
		    String pdbName = (String) pdbMultiLoopMap.get(fullName);
		    HashSet keepSet = (HashSet) pdbKeepMap.get(fullName);
		    //System.out.println(pdbName);
		    //System.out.println(fullName);
		    File pdbFile = (File) fileMap.get(pdbName);
		    //if (pdbKeepMap.containsKey(pdbName)) {
		    //System.out.println(pdbFile);
			kMain.getKinIO().loadFile(pdbFile, null);
			System.out.println(pdbFile.getPath());
			//onDeleteFromFile(ev);
			deleteFromFile(keepSet, pdbName);
			kMain.getKinIO().saveFile(new File(saveLoc, fullName + ".kin"));
			kMain.getTextWindow().setText("");
			kMain.getStable().closeCurrent();
		    
		}
		//}
		//catch(IOException ex) { // includes MalformedURLException 
		//	JOptionPane.showMessageDialog(kMain.getTopWindow(),
		//			      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
		//			      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
		//} catch(IllegalArgumentException ex) {
		//JOptionPane.showMessageDialog(kMain.getTopWindow(),
		//			      "Wrong map format was chosen, or map is corrupt:\n"+ex.getMessage(),
		//			      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
		//}
	}
    }


    public void onKeep(ActionEvent ev) {
	if (KinUtil.isNumeric(lowNumField.getText())&&(KinUtil.isNumeric(highNumField.getText()))) {
	    int firstNum = Integer.parseInt(lowNumField.getText());
	    int secondNum = Integer.parseInt(highNumField.getText());
	    if (firstNum > secondNum) {
		int temp = secondNum;
		secondNum = firstNum;
		firstNum = temp;
	    }
	    //for (int i = firstNum; i <= secondNum; i++) {
	    //	keptSet.add(new Integer(i));
	    //}
	    keepRange(keptSet, firstNum, secondNum);
	    listModel.addElement(Integer.toString(firstNum) + " to " + Integer.toString(secondNum));
	} else {
	    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
						  JOptionPane.ERROR_MESSAGE);
	}
    }

    private void keepRange(HashSet keepSet, int firstNum, int secondNum) {
	for (int i = firstNum; i <= secondNum; i++) {
	    keepSet.add(new Integer(i));
	}
	//listModel.addElement(Integer.toString(firstNum) + " to " + Integer.toString(secondNum));
    }

    public void onDeleteFromFile(ActionEvent ev) {
	String pdbName = kMain.getKinemage().atPdbfile.substring(0, 4).toLowerCase();
	if (pdbKeepMap.containsKey(pdbName)) {
	    HashSet keepSet = (HashSet) pdbKeepMap.get(pdbName);
	    delete(kMain.getKinemage(), keepSet);
	    recolor(kMain.getKinemage(), (HashSet) startColorMap.get(pdbName), KPalette.lime);
	    recolor(kMain.getKinemage(), (HashSet) endColorMap.get(pdbName), KPalette.red);
	    rename(kMain.getKinemage(), pdbName);
	} else {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
						      "This PDB file name was not found in the reference file.",
						      "Sorry!", JOptionPane.ERROR_MESSAGE);
	}
	kCanvas.repaint();
    }

    public void deleteFromFile(HashSet keepSet, String pdbName) {
	//String pdbName = kMain.getKinemage().atPdbfile.substring(0, 4).toLowerCase();
	//if (pdbMultiLoopMap.containsKey(pdbName)) {
	    delete(kMain.getKinemage(), keepSet);
	    //recolor(kMain.getKinemage(), (HashSet) startColorMap.get(pdbName), KPalette.green);
	    //recolor(kMain.getKinemage(), (HashSet) endColorMap.get(pdbName), KPalette.red);
	    rename(kMain.getKinemage(), pdbName);
	    //} else {
	    //    JOptionPane.showMessageDialog(kMain.getTopWindow(),
	    //					      "This PDB file name was not found in the reference file.",
							  //					      "Sorry!", JOptionPane.ERROR_MESSAGE);
	    //}
	kCanvas.repaint();
    }

    public void onDelete(ActionEvent ev) {
	//currently assuming kins formatted as lots.
	delete(kMain.getKinemage(), keptSet);
	//recolor(kMain.getKinemage(), startColor
	kCanvas.repaint();
    }

    public void onRemove(ActionEvent ev) {
	//if(kept.size() > 0) tupleList.remove(tupleList.size()-1);
	if(listModel.size() > 0) {
	    String last = (String) listModel.get(listModel.size()-1);
	    listModel.remove(listModel.size()-1);
	    
	    String[] limits = last.split(" to ");
	    int firstNum = Integer.parseInt(limits[0]);
	    int secondNum = Integer.parseInt(limits[1]);
	    for (int i = firstNum; i <= secondNum; i++) {
		keptSet.remove(new Integer(i));
	    }
	}
	//if(markList.children.size() > 0) markList.children.remove(markList.children.size()-1);
	
    }

    private void rename(AGE target, String addOn) {
	if (target instanceof KList) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		pt.setName(pt.getName() + " " + addOn);
		//int resNum = KinUtil.getResNumber(pt);
		//if (colorSet.contains(new Integer(resNum))) {
		//    pt.setColor(color);
		//} 
	    }
	} else {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		rename((AGE)iter.next(), addOn);
	    }
	}
    }

    private void recolor(AGE target, HashSet colorSet, KPaint color) {
	if (target instanceof KList) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		int resNum = KinUtil.getResNumber(pt);
		if (colorSet.contains(new Integer(resNum))) {
		    pt.setColor(color);
		} 
		
		
		
	    }
	} else {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		recolor((AGE)iter.next(), colorSet, color);
	    }
	}
    }

    private void delete(AGE target, HashSet keepSet) {
	if (target instanceof Kinemage) {
	    if (target != null) ((Kinemage)target).setModified(true);
	}
	if (target instanceof KList) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		int resNum = KinUtil.getResNumber(pt);
		String ptChain = KinUtil.getChainID(pt).toLowerCase();
		if ((!keepSet.contains(new Integer(resNum)))){//||(!chainID.equals(ptChain))) {
		    iter.remove();
		} else if ((keepSet.contains(new Integer(resNum)))&&(!keepSet.contains(new Integer(resNum-1)))) {
		    if (pt instanceof VectorPoint) {
			VectorPoint vpoint = (VectorPoint) pt;
			KPoint prev = vpoint.getPrev();
			if (prev instanceof KPoint) {
			    if (!keepSet.contains(new Integer(KinUtil.getResNumber(prev)))) {
				vpoint.setPrev(null);
			    }
			}
		    }
		}
	    
		
		    
	    }
	} else {
	    //System.out.println(target.toString());
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		delete((AGE)iter.next(), keepSet);
	    }
	}
    }

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return dialog; }

    public String toString() { return "Loop Tool"; }    


}
