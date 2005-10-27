// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;
import king.tool.util.KinUtil;
import king.tool.postkin.ConnectivityFinder;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;

import driftwood.util.*;
import driftwood.r3.*;
import driftwood.gui.*;
//import driftwood.moldb2.AminoAcid;

public class LoopDockingTool extends BasicTool {
    

//{{{ Constants
    
//}}}

//{{{ Variable definitions
//##############################################################################
    HashSet mobilePoints;
    ArrayList refPath, mobilePath;
    HashMap startColorMap, endColorMap;
    HashMap pdbKeepMap;
    TreeMap bFactorMap;
    TablePane pane;
    JFileChooser filechooser;
    ConnectivityFinder connect;
    JButton openButton;
    JTextField numResField;
    JRadioButton dockOnStartButton;
    JRadioButton dockOnEndButton;

//}}}

    public LoopDockingTool(ToolBox tb) {
	super(tb);
    }

//{{{ buildGUI
//##############################################################################
    protected void buildGUI()
    {

	dialog = new JDialog(kMain.getTopWindow(), "Loops", false);
	//lowNumField = new JTextField("", 5);
	//highNumField = new JTextField("", 5);
	//listModel = new DefaultListModel();
	//keptList = new JList(listModel);

	///keepButton = new JButton(new ReflectiveAction("Keep!", null, this, "onKeep"));
	//delButton = new JButton(new ReflectiveAction("Delete rest!", null, this, "onDelete"));
	//removeButton = new JButton(new ReflectiveAction("Remove last", null, this, "onRemove"));

	openButton = new JButton(new ReflectiveAction("Open file", null, this, "onOpenFile"));
	JLabel label = new JLabel("Number of Residues to dock: ");
	numResField = new JTextField("8", 5);
	
	dockOnStartButton = new JRadioButton("Dock on start of loop", true);
	dockOnEndButton = new JRadioButton("Dock on end of loop", false);
	ButtonGroup dockType = new ButtonGroup();
	dockType.add(dockOnStartButton);
	dockType.add(dockOnEndButton);
	//delFromFileButton = new JButton(new ReflectiveAction("Delete from file", null, this, "onDeleteFromFile"));
	//doAllButton = new JButton(new ReflectiveAction("Do ALL from file", null, this, "onDoAll"));

	pane = new TablePane();
	pane.newRow();
	//pane.add(lowNumField);
	//pane.add(highNumField);
	//pane.add(keepButton);
	//pane.add(delButton);
	//pane.newRow().save().hfill(true).vfill(true);
	//pane.add(new JScrollPane(keptList), 4, 1);
	//pane.newRow().restore();
	//pane.add(removeButton, 2, 1);
	pane.add(openButton, 1, 1);
	pane.newRow();
	pane.add(dockOnStartButton);
	pane.add(dockOnEndButton);
	pane.newRow();
	pane.add(label);
	pane.add(numResField);
	//pane.add(delFromFileButton, 1, 1);
	//pane.newRow();
	//pane.add(doAllButton, 3, 1);

        dialog.addWindowListener(this);

	dialog.setContentPane(pane);
    }

//}}}

    public void start() {
	//if (kMain.getKinemage() == null) return;
	buildGUI();
	//keptSet = new HashSet();
	pdbKeepMap = new HashMap();
	startColorMap = new HashMap();
	endColorMap = new HashMap();
	connect = new ConnectivityFinder(kMain);
	//bFactorMap = new TreeMap();
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
			    //System.out.print(exploded[0]+ " ");
			    //System.out.println(exploded[1]+ " " + exploded[2]);
			    String pdbName = exploded[0];
			    pdbName = pdbName.toLowerCase();
			    //System.out.print(pdbName);
			    //pdbName = pdbName.toLowerCase();
			    //if (pdbKeepMap.containsKey(pdbName)) {
				
			    //HashSet value = (HashSet) pdbKeepMap.get(pdbName);
			    //HashSet startSet = (HashSet) startColorMap.get(pdbName);
			    //HashSet endSet = (HashSet) endColorMap.get(pdbName);
				//keepRange(value, Integer.parseInt(exploded[1])-5, Integer.parseInt(exploded[2])+10);
			    //	startSet.add(Integer.parseInt(exploded[1]));
			    //startSet.add(Integer.parseInt(exploded[1])+1);
			    //	endSet.add(Integer.parseInt(exploded[2]));
			    //	endSet.add(Integer.parseInt(exploded[2])+1);
				//bFactorMap.put(df.format(Double.parseDouble(exploded[5]))+pdbName, pdbName);
				//} else {
				//HashSet value = new HashSet();
				//keepRange(value, Integer.parseInt(exploded[1])-5, Integer.parseInt(exploded[2])+10);
				int start = Integer.parseInt(exploded[1])-5;
				String fullName = pdbName + Integer.toString(start);
				    //pdbKeepMap.put(fullName, value);
				//HashSet start = new HashSet();
				//HashSet end = new HashSet();
				//start.add(Integer.parseInt(exploded[1]));
				//start.add(Integer.parseInt(exploded[1])+1);
				//end.add(Integer.parseInt(exploded[2]));
				//end.add(Integer.parseInt(exploded[2])+1);
				startColorMap.put(fullName, new Integer(exploded[1]));
				endColorMap.put(fullName, new Integer(exploded[2]));
			        //bFactorMap.put(df.format(Double.parseDouble(exploded[5]))+pdbName, pdbName);
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

    public void click(int x, int y, KPoint p, MouseEvent ev) {
	super.click(x, y, p, ev);

	if (KinUtil.isInteger(numResField.getText())) {
	    int numRestoSuper = Integer.parseInt(numResField.getText()) - 1;
	
	
	if (p != null) {
	    if (refPath != null) {
		connect.buildAdjacencyList(false);
		KList list = (KList) p.getOwner();
		KSubgroup sub = (KSubgroup) list.getOwner();
		KGroup group = (KGroup) sub.getOwner();
		String pdbID = group.getName().substring(0, 4).toLowerCase();
		mobilePoints = connect.mobilityFinder((AbstractPoint)p);
		Integer minRes = findMinResidue(mobilePoints);
		Integer ssStart = (Integer) endColorMap.get(pdbID + minRes.toString());
		Integer ssEnd = new Integer(ssStart.intValue() + numRestoSuper);
		if (dockOnStartButton.isSelected()) {
		    ssEnd = (Integer) startColorMap.get(pdbID + minRes.toString());
		    ssStart = new Integer(ssEnd.intValue() - numRestoSuper);
		}
		//HashSet mobile = connect.mobilityFinder((AbstractPoint)p);
		mobilePath = connect.pathFinder(findPoint(mobilePoints, ssStart), findPoint(mobilePoints, ssEnd));
		connect.buildAdjacencyList(true);
		mobilePoints = connect.mobilityFinder((AbstractPoint)p);
		superimpose(refPath, mobilePath);
	    } else {
		connect.buildAdjacencyList(false);
		KList list = (KList) p.getOwner();
		KSubgroup sub = (KSubgroup) list.getOwner();
		KGroup group = (KGroup) sub.getOwner();
		String pdbID = group.getName().substring(0, 4).toLowerCase();
	        mobilePoints = connect.mobilityFinder((AbstractPoint)p);
		Integer minRes = findMinResidue(mobilePoints);
		Integer ssStart = (Integer) endColorMap.get(pdbID + minRes.toString());
		Integer ssEnd = new Integer(ssStart.intValue() + numRestoSuper);
		if (dockOnStartButton.isSelected()) {
		    ssEnd = (Integer) startColorMap.get(pdbID + minRes.toString());
		    ssStart = new Integer(ssEnd.intValue() - numRestoSuper);
		}
		//HashSet mobile = connect.mobilityFinder((AbstractPoint)p);
		refPath = connect.pathFinder(findPoint(mobilePoints, ssStart), findPoint(mobilePoints, ssEnd));
	    }
		
	}
	}
    }

    public void superimpose(ArrayList refList, ArrayList mobileList) {
	//	connect.buildAdjacencyList(true);
	
	Tuple3[] ref = (Tuple3[])refList.toArray(new Tuple3[refList.size()]);
	//System.out.println(ref.length);
        Tuple3[] mob = (Tuple3[])mobileList.toArray(new Tuple3[mobileList.size()]);
	//System.out.println(mob.length);
        double[] w = new double[ref.length];
        Arrays.fill(w, 1.0);
        
        SuperPoser poser = new SuperPoser(ref, mob);
        Transform t = poser.superpos(w);
        
        Kinemage kin = kMain.getKinemage();
        if(!t.isNaN() && kin != null)
        {
            transform(kin, t);
            kin.setModified(true);
        }
        
        // Swap which button is selected
        //if(btnReference.isSelected())   btnMobile.setSelected(true);
        //else                            btnReference.setSelected(true);

        //if(!keepRefBox.isSelected()) {
	//    pkReference.clear();
	//    btnMobile.setSelected(true);
	//}
        //pkMobile.clear();
        kCanvas.repaint();

    }

//{{{ transformAllMobile
//##############################################################################
    private void transform(AGE target, Transform t)
    {
        //if(!target.isOn()) return;
        
        if(target instanceof KList)
        {
            Triple proxy = new Triple();
            for(Iterator iter = target.iterator(); iter.hasNext(); )
            {
                KPoint pt = (KPoint)iter.next();
                if(mobilePoints.contains(pt))
                {
                    proxy.setXYZ(pt.getOrigX(), pt.getOrigY(), pt.getOrigZ());
                    t.transform(proxy);
                    pt.setOrigX(proxy.getX());
                    pt.setOrigY(proxy.getY());
                    pt.setOrigZ(proxy.getZ());
                }
            }
        }
        else
        {
            for(Iterator iter = target.iterator(); iter.hasNext(); )
                transform((AGE)iter.next(), t);
        }
    }
//}}}

    public AbstractPoint findPoint(HashSet mobile, Integer searchNum) {
	Iterator iter = mobile.iterator();
	// HashSet alphaPoints = new HashSet();
	//AbstractPoint startPoint;
	while (iter.hasNext()) {
	    AbstractPoint point = (AbstractPoint) iter.next();
	    int resNum = KinUtil.getResNumber(point.getName().trim());
	    if (searchNum.intValue() == resNum) {
		//alphaPoints.add(point);
		String atom = KinUtil.getAtomName(point.getName().trim()).toUpperCase();
		if (atom.equals("CA")) {
		    return point;
		}
	    }
	}
	return null;
    }

    public Integer findMinResidue(Collection points) {
	Iterator iter = points.iterator();
	int lowNum = 100000;
	while (iter.hasNext()) {
	    AbstractPoint point = (AbstractPoint) iter.next();
	    int resNum = KinUtil.getResNumber(point.getName().trim());
	    if (resNum < lowNum) {
		lowNum = resNum;
	    }
	}
	return new Integer(lowNum);
    }
	    

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    //protected Container getToolPanel()
    //{ return dialog; }

    public String toString() { return "Loop Docking"; }    


	
}
