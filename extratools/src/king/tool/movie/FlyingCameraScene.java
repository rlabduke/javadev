// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.movie;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.r3.*;
//}}}
/**
* <code>FlyingCameraScene</code> blends from the previous view to the current view.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan 16 10:11:03 EST 2007
*/
public class FlyingCameraScene extends Scene
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    TablePane2 cp;
    JTextField tfDuration;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FlyingCameraScene(KingMain kMain, int duration)
    {
        super(kMain, duration);
        captureKinemageState();
    }
//}}}

//{{{ renderFrames, toString
//##############################################################################
    public void renderFrames(MovieMaker maker) throws IOException
    {
        KView startView = kMain.getView();
        restoreKinemageState();
        KView endView = kMain.getView();
        for(int i = 0; i < duration; i++)
        {
            KView currView = blendViews(startView, endView, (double)i / (double)duration);
            kMain.setView(currView);
            maker.writeFrame();
        }
    }
    
    public String toString()
    { return "Fly ("+duration+" frames)"; }
//}}}

//{{{ blendViews
//##############################################################################
    KView blendViews(KView startV, KView endV, double t)
    {
        final double T = 1 - t;
        KView out = startV.clone();
        
        float[] startC = startV.getCenter();
        float[] endC = endV.getCenter();
        out.setCenter(
            (float)(T*startC[0] + t*endC[0]),
            (float)(T*startC[1] + t*endC[1]),
            (float)(T*startC[2] + t*endC[2])
        );
        
        out.setClip((float)(T*startV.getClip() + t*endV.getClip()));
        
        // Exponential blending -- constant speed zoom
        double startS = startV.getSpan();
        double endS = endV.getSpan();
        // outS = e^tx * startS
        // endS = e^x * startS
        double x = Math.log(endS / startS);
        out.setSpan((float)(Math.exp(t*x) * startS));
        
        Quaternion startQ = getQuat(startV);
        Quaternion endQ = getQuat(endV);
        Quaternion slerpQ = new Quaternion().likeSlerp(startQ, endQ, t);
        setQuat(out, slerpQ);
        
        return out;
    }
    
    private Quaternion getQuat(KView v)
    {
        v.compile();
        Transform t = new Transform().likeMatrix(
            v.R11, v.R12, v.R13,
            v.R21, v.R22, v.R23,
            v.R31, v.R32, v.R33
        );
        Quaternion q = new Quaternion().likeRotation(t);
        return q;
    }
    
    private void setQuat(KView v, Quaternion q)
    {
        Transform t = new Transform().likeQuaternion(q);
        float[][] matrix = new float[][] {
            {(float)t.get(1,1), (float)t.get(1,2), (float)t.get(1,3)},
            {(float)t.get(2,1), (float)t.get(2,2), (float)t.get(2,3)},
            {(float)t.get(3,1), (float)t.get(3,2), (float)t.get(3,3)}
        };
        v.setMatrix(matrix);
    }
//}}}

//{{{ configure
//##############################################################################
    public boolean configure()
    {
        if(cp == null)
        {
            tfDuration = new JTextField();
            tfDuration.setText(Integer.toString(duration));
            
            cp = new TablePane2().hfill(true).memorize();
            cp.addCell(new JLabel("Duration (frames):")).addCell(tfDuration).newRow();
        }
        
        int result = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            cp, "Configure Snapshot",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if(result == JOptionPane.OK_OPTION)
        {
            try { this.duration = Integer.parseInt(tfDuration.getText()); }
            catch(NumberFormatException ex) { tfDuration.setText(Integer.toString(duration)); }
            return true;
        }
        else return false;
    }
//}}}

//{{{ gotoEndState
//##############################################################################
    public void gotoEndState()
    {
        restoreKinemageState();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

