// (jEdit options) :folding=explicit:collapseFolds=1:
import java.applet.*;
import java.awt.*;
import java.net.*;

/**
* <code>JavaCheck</code> is Java 1.0 applet that displays the version of Java currently installed.
* Depending on that version, it can display one or more short messages and/or jump to a different URL.
*
* <p>Parameters (&lt;PARAM&gt;):
* <ul>
* <li><code>version</code><i>n</i>
*   <br>Minimum version required to pass inspection, e.g. 1.3.1</li>
* <li><code>message</code><i>n</i>
*   <br>Message displayed upon passing inspection. (optional)</li>
* <li><code>url</code><i>n</i>
*   <br>Web page loaded upon passing inspection. (optional)</li>
* <li><code>default</code>
*   <br>Message displayed if none of the version conditions are met, evaluated in order (1 first, then 2, etc).</li>
* </ul>
*
* <p>Begun on Mon Jun 10 10:17:39 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class JavaCheck extends Applet //... implements ...
{
//{{{ Variable definitions
//##################################################################################################
    int level = 0;
//}}}

//{{{ init
//##################################################################################################
    public void init()
    {
        setBackground(Color.white);
        setLayout(new GridLayout(0,1));
        
        String versionStr = System.getProperty("java.version", "1.0");
        Label  versionLbl = new Label("Your Java version is "+versionStr, Label.CENTER);
        add(versionLbl);
        
        String msg = getParameter("default");
        if(msg == null) msg = "";

        int i = 1;        
        String versionReq = getParameter("version"+i);
        while(versionReq != null)
        {
            if(versionReq.compareTo(versionStr) <= 0)
            {
                msg = getParameter("message"+i);
                if(msg == null) msg = "";
                level = i;
                break; // quit this loop
            }
            versionReq = getParameter("version"+(++i));
        }

        Label passfail = new Label(msg, Label.CENTER);
        add(passfail);
        
        // Look for a web page to jump to...
        String urlStr = getParameter("url"+i);
        if(urlStr != null)
        {
            try {
                URL url = new URL(this.getDocumentBase(), urlStr);
                this.getAppletContext().showDocument(url);
            } catch(MalformedURLException ex) {}
        }
    }
//}}}

//{{{ getLevel()
//##################################################################################################
    public int getLevel() { return level; }
//}}}

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}
}//class
