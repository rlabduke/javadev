// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.xtal;
import king.*;
import king.core.*;

//import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.event.*;

import driftwood.gui.*;
import driftwood.r3.*;
//}}}




public class SuiteRotationTool extends BasicTool implements ChangeListener {

    //{{{ Constants

    //}}}

//{{{ Variable definitions
//##################################################################################################
    AngleDial suiteDial;
    TablePane pane;
    KPoint firstPoint = null;
    KPoint secondPoint = null;
    KPoint thirdPoint = null;
    RNATriple firstTrip = null;
    RNATriple secondTrip = null;
    RNATriple thirdTrip = null;
    Transform rotate;
    KList list;

//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */

    public SuiteRotationTool(ToolBox tb) {
	super(tb);
	list = new KList();
	buildGUI();
    }

//}}}

    private void buildGUI() {
	suiteDial = new AngleDial();
	suiteDial.addChangeListener(this);

	pane = new TablePane();
	pane.addCell(suiteDial);
    }

    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
	if(p instanceof VectorPoint) {
	    if (firstPoint == null) {
		firstPoint = p;
		firstTrip = new RNATriple(p);
	    } else if (secondPoint == null) {
		secondPoint = p;
		secondTrip = new RNATriple(p);
		calcRotation();
	    } else {
		thirdPoint = p;
		thirdTrip = new RNATriple(p);
	    }
	
	}
    }

    private void calcRotation() {
	rotate = new Transform();
	rotate = rotate.likeRotation(firstTrip, secondTrip, suiteDial.getDegrees());
    }

    private void doRotation() {
	list = (KList) thirdPoint.getOwner();
	rotate = rotate.likeRotation(firstTrip, secondTrip, suiteDial.getDegrees());
	RNATriple origTrip = (RNATriple) thirdTrip.clone();
	rotate.transform(thirdTrip);
	thirdPoint.setOrigX(thirdTrip.getX());
	thirdPoint.setOrigY(thirdTrip.getY());
	thirdPoint.setOrigZ(thirdTrip.getZ());
	thirdTrip = origTrip;
	//System.out.println(thirdPoint + ", org x: " + thirdPoint.getOrigX());
	//System.out.println(thirdPoint + ", x: " + thirdPoint.getX());
	list.add(thirdPoint);
	
    }

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return pane; }
    
    public String getHelpAnchor()
    { return "#suite-tool"; }
    
    public String toString() { return "Suite Rotation"; }
//}}}

    public void stateChanged(ChangeEvent ev) {

	//System.out.println(suiteDial.getDegrees());
	doRotation();
        kCanvas.repaint();
    }


}//class
