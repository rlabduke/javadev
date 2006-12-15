// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.Timer;
import driftwood.r3.*;
//}}}
/**
* <code>MasterGUI</code> has not yet been documented.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec 15 11:42:40 EST 2006
*/
public class MasterGUI implements ActionListener, MouseListener, MouseMotionListener
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Master master;
    KView view;
    Triple leftEye, rightEye;
    Point whereLast = new Point(0,0);
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MasterGUI(Master m)
    {
        super();
        this.master = m;
        
        this.view = new KView(null);
        view.setCenter(0, 0, 0);
        view.setSpan(3);
        view.setClip(3);
        this.leftEye = Slave.getTriple(master.props, "master.observer_left_eye_px");
        this.rightEye = Slave.getTriple(master.props, "master.observer_right_eye_px");
        
        Timer timer = new Timer(1000 / 30, this);
        //timer.start();
        
        createMouseUI();
    }
//}}}

//{{{ actionPerformed
//##############################################################################
    public void actionPerformed(ActionEvent ev)
    {
        view.rotateY( (float) Math.toRadians(1.0) );
        master.sendCommand( new CmdUpdateViewpoint(view, leftEye, rightEye) );
    }
//}}}

//{{{ createMouseUI
//##############################################################################
    private void createMouseUI()
    {
        JFrame frame = new JFrame("king.dive input");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(600, 600));
        frame.addMouseListener(this);
        frame.addMouseMotionListener(this);
        frame.pack();
        frame.show();
    }
//}}}

//{{{ Mouse motion listeners
//##################################################################################################
    public void mouseDragged(MouseEvent ev)
    {
        Point where = ev.getPoint();
        int dx, dy;
        dx = where.x - whereLast.x;
        dy = where.y - whereLast.y;
        
        view.rotateX((float)(2.0*Math.PI) * dy / 600f);
        view.rotateY((float)(2.0*Math.PI) * dx / 600f);
        master.sendCommand( new CmdUpdateViewpoint(view, leftEye, rightEye) );
        
        whereLast = where;
    }
    
    public void mouseMoved(MouseEvent ev)
    {}
//}}}

//{{{ Mouse click listners
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {}
    public void mouseEntered(MouseEvent ev)
    {}
    public void mouseExited(MouseEvent ev)
    {}
    
    public void mousePressed(MouseEvent ev)
    {
        whereLast = ev.getPoint();
    }
    
    public void mouseReleased(MouseEvent ev)
    {}
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

