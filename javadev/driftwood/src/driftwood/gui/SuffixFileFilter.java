// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.gui;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>SuffixFileFilter</code> filters files based on their
* suffixes/extensions/endings. You should include a dot when
* specifying suffixes, eg. ".png", ".jpg", and ".gif".
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar  4 13:00:39 EST 2003
*/
public class SuffixFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter, java.io.FilenameFilter
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    String      description;
    ArrayList   suffixes;
    ArrayList   patterns;
    boolean     caseSensitive;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Creates a new SuffixFileFilter that ignores case
    * @param description the visible name of the filter, eg "Image files"
    */
    public SuffixFileFilter(String description)
    {
        this(description, false);
    }
    /**
    * Creates a new SuffixFileFilter
    * @param description the visible name of the filter, eg "Image files"
    * @param caseSensitive if false, names are compared without regard to case
    */
    public SuffixFileFilter(String description, boolean caseSensitive)
    {
        this.description    = description;
        this.caseSensitive  = caseSensitive;
        suffixes = new ArrayList();
        patterns = new ArrayList();
    }
//}}}

//{{{ addSuffix
//##################################################################################################
    /**
    * Adds a new suffix.
    * Files that match any one or more of the added
    * suffixes will be accepted for display.
    */
    public void addSuffix(String suffix)
    {
        if(!caseSensitive) suffix = suffix.toLowerCase();
        suffixes.add(suffix);
    }
//}}}

//{{{ addPattern
/**
* Adds a new regular expression String to match.
*/
public void addPattern(String pattern) {
  patterns.add(pattern);
}
//}}}

//{{{ getDescription
//##################################################################################################
    /** Returns the description string from the constructor */
    public String getDescription()
    { return description; }
//}}}

//{{{ accept
//##################################################################################################
    public boolean accept(File f)
    {
        if(f == null) return false;
        // Don't hide existing directories, but don't allow
        // any old filename that happens to not have an extension.
        if((f.isDirectory() || !f.isFile()) && f.exists()) return true;
        return accept(f.getName());
    }
    
    public boolean accept(String name)
    {
        if(name == null) return false;
        if(!caseSensitive) name = name.toLowerCase();
        
        String suffix;
        boolean ok = false;
        for(Iterator iter = suffixes.iterator(); iter.hasNext() && !ok; )
        {
            suffix = (String)iter.next();
            if(name.endsWith(suffix)) ok = true;
        }
        
        return (ok||acceptPattern(name));
    }
    
    public boolean accept(File path, String filename)
    {
        return accept(filename);
    }
    
    public boolean acceptPattern(String name) {
      if(name == null) return false;
      
      String pattern;
      boolean ok = false;
      for(Iterator iter = patterns.iterator(); iter.hasNext() && !ok; )
      {
        pattern = (String)iter.next();
        if(name.matches(pattern)) ok = true;
      }
      
      return ok;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

