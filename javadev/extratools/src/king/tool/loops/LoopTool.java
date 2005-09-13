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

import driftwood.util.*;
import driftwood.gui.*;

public class LoopTool extends BasicTool {

//{{{ Constants

//}}}

//{{{ Variable definitions
//##############################################################################
    HashSet keptSet;
    HashMap pdbKeepMap;
    JButton delButton, keepButton, removeButton, openButton, delFromFileButton;
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

	openButton = new JButton(new ReflectiveAction("Open file", null, this, "onOpenFile"));
	delFromFileButton = new JButton(new ReflectiveAction("Delete from file", null, this, "onDeleteFromFile"));

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
	pane.add(openButton);
	pane.add(delFromFileButton);

        dialog.addWindowListener(this);

	dialog.setContentPane(pane);
    }

//}}}

    public void start() {
	if (kMain.getKinemage() == null) return;
	buildGUI();
	keptSet = new HashSet();
	pdbKeepMap = new HashMap();
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
			    System.out.print(exploded[0]+ " ");
			    System.out.println(exploded[1]+ " " + exploded[2]);
			    String pdbName = exploded[0];
			    pdbName = pdbName.toLowerCase();
			    //System.out.print(pdbName);
			    //pdbName = pdbName.toLowerCase();
			    if (pdbKeepMap.containsKey(pdbName)) {
				HashSet value = (HashSet) pdbKeepMap.get(pdbName);
				keepRange(value, Integer.parseInt(exploded[1])-5, Integer.parseInt(exploded[2])+10);
			    } else {
				HashSet value = new HashSet();
				keepRange(value, Integer.parseInt(exploded[1])-5, Integer.parseInt(exploded[2])+10);
				pdbKeepMap.put(pdbName, value);
			    }
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
	} else {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
						      "This PDB file name was not found in the reference file.",
						      "Sorry!", JOptionPane.ERROR_MESSAGE);
	}
	kCanvas.repaint();
    }



    public void onDelete(ActionEvent ev) {
	//currently assuming kins formatted as lots.
	delete(kMain.getKinemage(), keptSet);
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

    private void delete(AGE target, HashSet keepSet) {
	if (target instanceof KList) {
	    ListIterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		int resNum = KinUtil.getResNumber(pt);
		if (!keepSet.contains(new Integer(resNum))) {
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
