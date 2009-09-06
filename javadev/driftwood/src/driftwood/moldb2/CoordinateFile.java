// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.gui.*;
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
    int                 pdbv2atoms      = 0;
    
    SecondaryStructure  secondaryStructure = new SecondaryStructure.AllCoil();
    Disulfides          disulfides         = new Disulfides.NoDisulfides();
    
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

//{{{ add, replace, remove
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
    
    public void remove(Model oldModel) {
      models.remove(oldModel);
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

//{{{ get/setSecondaryStructure
//##################################################################################################
    // These are associated with a CoordinateFile rather than with a Model
    // because they're defined at the file level for PDB and mmCIF.
    
    /** Default assignment marks everything as COIL. */
    public SecondaryStructure getSecondaryStructure()
    { return this.secondaryStructure; }
    
    public void setSecondaryStructure(SecondaryStructure s)
    { this.secondaryStructure = s; }
//}}}

//{{{ get/setDisulfides
//##################################################################################################
    // These are associated with a CoordinateFile rather than with a Model
    // because they're defined at the file level for PDB and mmCIF.
    
    /** Default assignment marks everything as COIL. */
    public Disulfides getDisulfides()
    { return this.disulfides; }
    
    public void setDisulfides(Disulfides d)
    { this.disulfides = d; }
//}}}

//{{{ get/setPdbv23Count
public void setPdbv2Count(int count) {
  pdbv2atoms = count;
  //System.out.println("coord file pdbv2atoms set to: "+pdbv2atoms);
}

public int getPdbv2Count() {
  return pdbv2atoms;
}
//}}}

//{{{ getFileFilters
public static SuffixFileFilter getCoordFileFilter() {
  SuffixFileFilter allFilter = new SuffixFileFilter("Structural (PDB or mmCIF) files");
  allFilter.addSuffix(".pdb");
  allFilter.addSuffix(".xyz");
  allFilter.addSuffix(".ent");
  allFilter.addSuffix(".cif");
  allFilter.addSuffix(".mmcif");
  allFilter.addPattern(".*\\.pdb.*\\.gz");
  allFilter.addSuffix(".xyz.gz");
  allFilter.addSuffix(".ent.gz");
  allFilter.addSuffix(".cif.gz");
  allFilter.addSuffix(".mmcif.gz");
  return allFilter;
}

public static SuffixFileFilter getPdbFileFilter() {
  SuffixFileFilter pdbFilter = new SuffixFileFilter("Protein Data Bank (PDB) files");
  pdbFilter.addSuffix(".pdb");
  pdbFilter.addSuffix(".xyz");
  pdbFilter.addSuffix(".ent");
  pdbFilter.addPattern(".*\\.pdb.*\\.gz");
  pdbFilter.addSuffix(".xyz.gz");
  pdbFilter.addSuffix(".ent.gz");
  return pdbFilter;
}

static public SuffixFileFilter getCifFileFilter() {
  SuffixFileFilter cifFilter = new SuffixFileFilter("mmCIF files");
  cifFilter.addSuffix(".cif");
  cifFilter.addSuffix(".mmcif");
  cifFilter.addSuffix(".cif.gz");
  cifFilter.addSuffix(".mmcif.gz");
  return cifFilter;
}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

