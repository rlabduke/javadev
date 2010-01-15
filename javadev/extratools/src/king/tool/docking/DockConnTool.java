// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.docking;
import king.*;
import king.core.*;
import king.points.*;
import king.tool.postkin.ConnectivityFinder;
import driftwood.util.SoftLog;

import java.net.*;
import java.util.*;

import java.awt.event.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.gui.*;
//}}}
/**
* <code>DockConnTool</code> has yet to be documented.
*/
public class DockConnTool extends DockLsqTool {

//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    //TablePane       toolpane;
    //JRadioButton    btnReference, btnMobile;
    //JButton         btnDock;
    /*JCheckBox keepRefBox;*/
    HashSet mobilePoints;
    HashMap adjacencyMap;
    AbstractPoint firstClick, secondClick;
    LinkedList refList = new LinkedList();
    ConnectivityFinder connect;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DockConnTool(ToolBox tb)
    {
        super(tb);

        addGUI();
    }
//}}}

//{{{ addGUI, start
//##############################################################################
    private void addGUI()
    {
        //super.buildGUI();
        //btnDock = new JButton(new ReflectiveAction("Dock mobile on reference", null, this, "onDock"));
        btnDock.setLabel("Dock mobile on reference");
        /*keepRefBox = new JCheckBox("Keep reference points", true);
        toolpane.newRow();
        toolpane.add(keepRefBox, 3, 1);*/
    }

    public void start()
    {
        if (kMain.getKinemage() == null) return;
        connect = new ConnectivityFinder(kMain);
        //adjacencyMap = new HashMap();
        //buildAdjacencyList();
    
        show();
    }
//}}}

//{{{ xx_click() functions
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        //super.click(x, y, p, ev);
        services.pick(p);
        
        if(p != null && p.getComment() != null)
            clickActionHandler(p.getComment());

        if(p != null) {
	    if (firstClick != null) {
		connect.buildAdjacencyList(false);
		ArrayList list = connect.pathFinder(firstClick, (AbstractPoint) p);
		//System.out.println(list.size());
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
		    AbstractPoint point = (AbstractPoint) iter.next();
		    //Triple t = new Triple(point);
		    if (btnReference.isSelected()) {
			pkReference.add(point.getName(), point);
		    } else if (btnMobile.isSelected()) {
			pkMobile.add(point.getName(), point);
		    }
		    else {
			JOptionPane.showMessageDialog(kMain.getTopWindow(),
						      "Either 'Reference' or 'Mobile' should be selected.",
						      "Error", JOptionPane.ERROR_MESSAGE);
		    }
		    firstClick = null;
		}
	    } else {
		firstClick = (AbstractPoint) p;
	    }
	    
	    /*	
            Triple t = new Triple(p.getX(), p.getY(), p.getZ());
            if(btnReference.isSelected())
            {
                pkReference.add(p.getName(), t);
            }
            else if(btnMobile.isSelected()) 
            {
                pkMobile.add(p.getName(), t);
            }
            else
            {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Either 'Reference' or 'Mobile' should be selected.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
	    */
        }
    }
//}}}
    
//{{{ onDock
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDock(ActionEvent ev)
    {
        connect.buildAdjacencyList(true);
        mobilePoints = connect.mobilityFinder((AbstractPoint)pkMobile.tupleList.get(0));
        Tuple3[] ref = (Tuple3[])pkReference.tupleList.toArray(new Tuple3[pkReference.tupleList.size()]);
        Tuple3[] mob = (Tuple3[])pkMobile.tupleList.toArray(new Tuple3[pkMobile.tupleList.size()]);
        
        SuperPoser poser = new SuperPoser(ref, mob);
        Transform t = poser.superpos();
        
        Kinemage kin = kMain.getKinemage();
        if(!t.isNaN() && kin != null)
        {
            transform(kin, t);
            kin.setModified(true);
        }
        
        if(cbKeepReference.isSelected())  btnMobile.setSelected(true);
        else                              btnReference.setSelected(true);
        
        /*if(!keepRefBox.isSelected()) {*/
        if(!cbKeepReference.isSelected())
        {
            pkReference.clear();
	        btnMobile.setSelected(true);
	    }
        pkMobile.clear();
        kCanvas.repaint();
    }
//}}}

//{{{ transformAllVisible
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
                    proxy.setXYZ(pt.getX(), pt.getY(), pt.getZ());
                    t.transform(proxy);
                    pt.setX(proxy.getX());
                    pt.setY(proxy.getY());
                    pt.setZ(proxy.getZ());
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

//{{{ toString
//##############################################################################
public String toString() { return "Dock by pick range"; }
    
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL     url     = getClass().getResource("/extratools/tools-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else return null;
    }

    public String getHelpAnchor() { return "#dockbypicking-tool"; }
//}}}
}

