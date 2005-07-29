// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>CoordinateFile</code> is a lightweight container for a group
* of Models that have some relationship to one another.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 11 11:15:15 EDT 2003
*/
public class CoordinateFile //extends ... implements ...
{
//{{{ Constants
    /** The first thing in the file: user modifications */
    public static final String  SECTION_USER_MOD        = "USER  MOD";
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** The collection of Models that belong to this group */
    ArrayList           models;
    Collection          unmodModels     = null;
    ArrayList           headers;
    Collection          unmodHeaders    = null;
    
    File        file = null;
    String      idCode = null;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public CoordinateFile()
    {
        models  = new ArrayList();
        headers = new ArrayList();
    }
//}}}

//{{{ getModels, getFirstModel
//##################################################################################################
    /** Returns an unmodifiable view of the models in this group */
    public Collection getModels()
    {
        if(unmodModels == null)
            unmodModels = Collections.unmodifiableCollection(models);
        return unmodModels;
    }
    
    /**
    * Returns the first model.
    * @throws NoSuchElementException if no models are present.
    */
    public Model getFirstModel()
    {
        Iterator iter = models.iterator();
        return (Model)iter.next();
    }
//}}}

//{{{ add
//##################################################################################################
    /** Adds a model to this group */
    public void add(Model m)
    {
        if(m == null) throw new NullPointerException("Cannot add a null model");
        models.add(m);
    }
    
    /** Replaces one model with another, or just adds the new model if the old one wasn't present. */
    public void replace(Model oldModel, Model newModel)
    {
        if(newModel == null) throw new NullPointerException("Cannot add a null model");
        
        int idx = models.indexOf(oldModel);
        if(idx == -1)   models.add(newModel);
        else            models.set(idx, newModel);
    }
//}}}

//{{{ addHeader, getHeaders
//##################################################################################################
    /**
    * Adds a line of header information to the list of data
    * associated with this group of models.
    * @param section    which block of info this line belongs in.
    * @param header     the actual header data.
    */
    public void addHeader(String section, String header)
    {
        if(SECTION_USER_MOD.equals(section))
            headers.add(0, header);
        else
            headers.add(header);
    }
    
    /** Returns an unmodifiable view of all the headers in this group */
    public Collection getHeaders()
    {
        if(unmodHeaders == null)
            unmodHeaders = Collections.unmodifiableCollection(headers);
        return unmodHeaders;
    }
//}}}

//{{{ get/set File, IdCode
//##################################################################################################
    /** Returns the file from which these models were loaded. May be null. */
    public File getFile()
    { return file; }
    /** Sets the file from which these models were loaded. May be null. */
    public void setFile(File file)
    { this.file = file; }
    
    /** Returns the ID code associated with these models (e.g. PDB ID). May be null. */
    public String getIdCode()
    { return idCode; }
    /** Sets the ID code associated with these models (e.g. PDB ID). May be null. */
    public void setIdCode(String idCode)
    { this.idCode = idCode; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

