// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
//import gnu.regexp.*;
//}}}
/**
 * <code>ContactMapTool</code> is a special tool designed to support the contact maps produced by the <code>validate</code> package.
 * It's only useful from a web page, because it uses the browser to display info on the residues.
 * <p>To use: put <code>@kingtool {king.ContactMapTool}</code> in your kinemage,
 * and <code>&lt;PARAM name="ContactMapToolSource" value="some/relative/path/info.html"&gt;</code> in your HTML.
 *
 * <p>Begun on Fri Jul 19 10:57:30 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class ContactMapTool extends FlatlandTool // implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    GridBagPanel toolPane;
    JButton rowBtn, colBtn;
    String rowLbl = null, colLbl = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ContactMapTool(ToolBox tb)
    {
        super(tb);
        setToolName("Contact Map");
        
        toolPane = new GridBagPanel();
        toolPane.gbc.insets = new Insets(4,4,4,4);
        JLabel msg = new JLabel("<html>Select a point, then use these<br>buttons to get more information<br>in the browser window.");
        rowBtn = new JButton(new ReflectiveAction("---", null, this, "onRowBtn"));
        colBtn = new JButton(new ReflectiveAction("---", null, this, "onColBtn"));
        toolPane.add(msg, 0, 0);
        toolPane.gbc.fill = GridBagConstraints.HORIZONTAL;
        toolPane.gbc.weightx = 1.0;
        toolPane.add(rowBtn, 0, 1);
        toolPane.add(colBtn, 0, 2);
        
        parent.show();
        /*
        Point pt = parent.dialog.getLocation();
        Dimension pdim = parent.dialog.getSize();
        Dimension wdim = parent.kMain.getMainWindow().getSize();
        pt.x += wdim.width  - pdim.width;
        pt.y += wdim.height - pdim.height;
        parent.dialog.setLocation(pt);
        */
    }
//}}}

//{{{ click() functions    
//##################################################################################################
    /** Override this function for (left-button) clicks */
    public void click(int x, int y, KPoint p, MouseEvent ev)
    {
        super.click(x, y, p, ev);
        if(p != null)
        {
            String s;
            try
            {
                s = p.getName();
                rowLbl = s.substring(0, s.indexOf(','));
                rowBtn.setText(rowLbl);
                colLbl = s.substring(s.indexOf(',')+1);
                colBtn.setText(colLbl);
            }
            catch(IndexOutOfBoundsException ex) {}
        }
    }
//}}}

//{{{ event handlers
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onRowBtn(ActionEvent ev)
    { if(rowLbl != null) goToURL(rowLbl); }
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onColBtn(ActionEvent ev)
    { if(colLbl != null) goToURL(colLbl); }
    
    void goToURL(String anchor)
    {
        JApplet app = parent.kMain.getApplet();
        if(app == null) { echo("Not in an applet!"); return; }
        String reslistfile = app.getParameter("ContactMapToolSource");
        if(reslistfile == null) { echo("<PARAM name=\"ContactMapToolSource\" value=\"...\"> not defined!"); return; }
        
        anchor = anchor.replace(' ', '_');
        try
        {
            URL base = app.getDocumentBase();
            URL target = new URL(base, reslistfile+"#"+anchor);
            echo(target.toString());
            app.getAppletContext().showDocument(target, "cmaptoolsrcwin");
        }
        catch(MalformedURLException ex)
        { ex.printStackTrace(); }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ get/set functions    
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    public Component getToolPanel()
    { return toolPane; }
//}}}

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}
}//class

