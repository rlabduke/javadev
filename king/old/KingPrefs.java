// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.io.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
//}}}
/**
 * <code>KingPrefs</code> holds information about the preferrences of the user running this instance of KiNG.
 * For documentation of the available options, see rc/config/king_prefs
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Fri Jun 21 09:10:40 EDT 2002
*/
public class KingPrefs extends Props // implements ...
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    
    // Resources
    Image windowIcon = null; // the icon to display on the corner of the window, in the task bar, etc.
    
    // Settings
    // For descriptions of settings, see rc/config/king_prefs
    float fontMagnification = 1.0f;
    boolean showPickCenter = true;
    boolean showAnimateButtons = true;
    boolean showTextAtBottom = false;
    
    boolean mmtoolDoClipping = false;
    
    boolean treeConfirmDelete = true;
    boolean saveConfirmOverwrite = true;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public KingPrefs()
    {
        super();
        
        // Resources
        // Get the icon that is displayed in the corner of the main window
        //try { windowIcon = new ImageIcon(getClass().getResource("/rc/images/kingicon.gif")).getImage(); }
        try { windowIcon = new ImageIcon(getClass().getResource("/rc/images/king-glass.png")).getImage(); }
        catch(NullPointerException ex) {}
        
        // Settings
        try {
            File propfile;
            // current dir first, then home dir
            propfile = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + ".king_prefs");
            if(!propfile.exists()) propfile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".king_prefs");
            InputStream is = new BufferedInputStream(new FileInputStream(propfile));
            this.load(is);
            is.close();
        }
        catch(NullPointerException ex) {
            echo("Couldn't load user preferences from .king_prefs in current or home directory.");
        } catch(IOException ex) {
            echo("Couldn't load user preferences from .king_prefs in current or home directory.");
        } catch(SecurityException ex) {
            echo("Couldn't load user preferences from .king_prefs in current or home directory.");
        }
        
        fontMagnification = getFloat("fontMagnification", fontMagnification);
        showPickCenter = getBoolean("showPickCenter", showPickCenter);
        showAnimateButtons = getBoolean("showAnimateButtons", showAnimateButtons);
        showTextAtBottom = getBoolean("showTextAtBottom", showTextAtBottom);
        
        mmtoolDoClipping = getBoolean("mmtoolDoClipping", mmtoolDoClipping);
        
        treeConfirmDelete = getBoolean("treeConfirmDelete", treeConfirmDelete);
        saveConfirmOverwrite = getBoolean("saveConfirmOverwrite", saveConfirmOverwrite);
    }
//}}}

//{{{ Variable retrieval
//##################################################################################################
    /** Returns the icon to be used for decorating windows created by the program */
    public Image getWindowIcon() { return windowIcon; }
//}}}

//{{{ getString()
//##################################################################################################
    /** Just an alias for getProperty() */
    public String getString(String key, String defVal)
    {
        String ret = getProperty(key);
        if(ret == null) ret = defVal;
        return ret;
    }
//}}}

//{{{ getInteger()
//##################################################################################################
    public int getInteger(String key, int defVal)
    {
        int ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            try { ret = Integer.parseInt(prop); } catch(NumberFormatException ex) {}
        }
        return ret;
    }
//}}}

//{{{ getDouble()
//##################################################################################################
    public double getDouble(String key, double defVal)
    {
        double ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            try { ret = Double.parseDouble(prop); } catch(NumberFormatException ex) {}
        }
        return ret;
    }
//}}}

//{{{ getFloat()
//##################################################################################################
    public float getFloat(String key, float defVal)
    {
        float ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            try { ret = Float.parseFloat(prop); } catch(NumberFormatException ex) {}
        }
        return ret;
    }
//}}}

//{{{ getBoolean()
//##################################################################################################
    public boolean getBoolean(String key, boolean defVal)
    {
        boolean ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            if(prop.equalsIgnoreCase("true")  || prop.equalsIgnoreCase("on")
                || prop.equalsIgnoreCase("yes") || prop.equals("1")) ret = true;

            else if(prop.equalsIgnoreCase("false") || prop.equalsIgnoreCase("off")
                || prop.equalsIgnoreCase("no")  || prop.equals("0")) ret = false;
        }
        return ret;
    }
//}}}

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}
}//class
