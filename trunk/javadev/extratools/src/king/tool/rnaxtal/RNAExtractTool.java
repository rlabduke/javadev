// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;

import king.*;
import king.core.*;
import king.points.*;
import king.tool.util.*;

import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}

public class RNAExtractTool extends BasicTool {

    HashMap kinKeepMap; // fullName (fileName + startResidue#) -> hashset of integers to keep.
    //HashMap pdbMultiLoopMap; // fullName -> fileName
    HashMap fileMultiMap; // fileName -> list of fullNames
    HashMap chainMap; // fullName -> chainID
    JFileChooser filechooser;

    public RNAExtractTool(ToolBox tb) {
	super(tb);
	//buildGUI();
    }

    public void start() {
	kinKeepMap = new HashMap();
	fileMultiMap = new HashMap();
	chainMap = new HashMap();
	onOpenFile();
	onDoAll();
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
    }
//}}}


    public void onOpenFile() {
	if (filechooser == null) makeFileChooser();
	filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	int choice = filechooser.showOpenDialog(kMain.getTopWindow());
	if(JFileChooser.APPROVE_OPTION == choice)
	{
	    try {
		File f = filechooser.getSelectedFile();
		if(f != null && f.exists()) {
		    //dialog.setTitle(f.getName());
		    BufferedReader reader = new BufferedReader(new FileReader(f));
		    String line;
		    try {
			while ((line = reader.readLine())!=null) {
			    String[] exploded = Strings.explode(line, ',', false, true);
			    //System.out.print(exploded[0]+ " ");
			    //System.out.println(exploded[1]+ " " + exploded[2]);
			    String fileName = exploded[0];
			    fileName = fileName.toLowerCase();
			    HashSet value = new HashSet();
			    keepRange(value, Integer.parseInt(exploded[1]), Integer.parseInt(exploded[2]));
			    int startRes = Integer.parseInt(exploded[1]);
			    String fullName = fileName + "-" + Integer.toString(startRes);
			    //pdbMultiLoopMap.put(fullName, fileName);
			    ArrayList list;
			    if (fileMultiMap.containsKey(fileName)) {
				list = (ArrayList) fileMultiMap.get(fileName);
			    } else {
				list = new ArrayList();
				fileMultiMap.put(fileName, list);
			    }
			    list.add(fullName);
			    kinKeepMap.put(fullName, value);
			    String chainID = exploded[3].toLowerCase();
			    chainMap.put(fullName, chainID);
			}
		    } catch (IOException ex) {
			JOptionPane.showMessageDialog(kMain.getTopWindow(),
						      "A read error occurred while reading the file:\n"+ex.getMessage(),
						      "Sorry!", JOptionPane.ERROR_MESSAGE);
		    }
		    kCanvas.repaint();
		}
	    } catch (IOException ex) {
		JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
	    }
	}
	if (JFileChooser.CANCEL_OPTION == choice) {
	    parent.activateDefaultTool();
	}
    }

    private void keepRange(HashSet keepSet, int firstNum, int secondNum) {
	for (int i = firstNum; i <= secondNum; i++) {
	    keepSet.add(new Integer(i));
	}
	//listModel.addElement(Integer.toString(firstNum) + " to " + Integer.toString(secondNum));
    }

    public void onDoAll() {
	if (filechooser == null) makeFileChooser();
	filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	int choice = filechooser.showOpenDialog(kMain.getTopWindow());
        if(JFileChooser.APPROVE_OPTION == choice)
	{
	    File f = filechooser.getSelectedFile();
	    File[] allFiles = f.listFiles();
	    File saveLoc = new File(f, "fragments");
	    //File[] allFiles = f.getParentFile().listFiles();
	    //File saveLoc = new File(f.getParentFile(), "fragments");
	    Set keys = fileMultiMap.keySet();
	    Iterator iter = keys.iterator();
	    while (iter.hasNext()) {
		String structName = (String) iter.next();
		ArrayList fullNames = (ArrayList) fileMultiMap.get(structName);
		String fileName = "";
		File fileToOpen = null;
		int i = 0;
		while ((fileName.indexOf(structName)==-1)&&(i < allFiles.length)) {
		    File kinFile = allFiles[i];
		    String tempName = kinFile.getName().toLowerCase();
		    if ((tempName.indexOf(structName)>-1)&&(tempName.indexOf(".kin")>-1)) {
			fileName = tempName;
			fileToOpen = kinFile;
		    }
		    i++;
		}
		Iterator fullNameIter = fullNames.iterator();
		while (fullNameIter.hasNext()) {
		    String fullName = (String) fullNameIter.next();
		    if (fileToOpen != null) {
			System.out.print(fullName + " -> ");
			System.out.println(fileName);
			kMain.getKinIO().loadFile(fileToOpen, null);
			//System.out.println(fileToOpen.getPath());
			deleteFromFile((HashSet)kinKeepMap.get(fullName), fullName);
			kMain.getKinIO().saveFile(new File(saveLoc, fullName + ".kin"));
			kMain.getTextWindow().setText("");
			kMain.getStable().closeCurrent();
			
			//System.out.print(fullName + " -> ");
			//System.out.println(fileName);
		    }
		}
	    }
		    
	    //HashMap fileMap = new HashMap();
	    
	    //for (int i = 0; i < allFiles.length; i++) {
	    //	File pdbFile = allFiles[i];
	    //	String pdbName = pdbFile.getName().toLowerCase();
	    //	fileMap.put(pdbName, pdbFile);
		// gotta figure out some way to search filemap with actual file names...
		//System.out.println(pdbFile.getPath() + " : " + pdbName + " : " + pdbFile.getParent());
		//System.out.println(pdbKeepMap.containsKey(pdbName.toLowerCase()));
	}
	if (JFileChooser.CANCEL_OPTION == choice) {
	    parent.activateDefaultTool();
	}
    }

    public void deleteFromFile(HashSet keepSet, String fullName) {

	delete(kMain.getKinemage(), keepSet, (String) chainMap.get(fullName));
	rename(kMain.getKinemage(), fullName);
	
	kCanvas.repaint();
    }

    private void delete(AGE target, HashSet keepSet, String chainID) {
	if (target instanceof Kinemage) {
	    if (target != null) ((Kinemage)target).setModified(true);
	}
	if (target instanceof KList) {
	    Iterator iter = target.iterator();
	    while (iter.hasNext()) {
		KPoint pt = (KPoint) iter.next();
		int resNum = KinUtil.getResNumber(pt);
		String ptChain = KinUtil.getChainID(pt).toLowerCase();
		if ((!keepSet.contains(new Integer(resNum)))||(!chainID.equals(ptChain))) {
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
		delete((AGE)iter.next(), keepSet, chainID);
	    }
	}
    }

    
    private void rename(AGE target, String addOn) {
	if (target instanceof KList) {
	    Iterator iter = target.iterator();
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
    
//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return null; }

    public String toString() { return "RNA Extract Tool"; }    


}    
	    
