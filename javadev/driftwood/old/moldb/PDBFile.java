// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//XXX-JAVA13//import java.util.regex.*;
//import javax.swing.*;
import driftwood.gnutil.*;
import driftwood.util.*;
//}}}
/**
* <code>PDBFile</code> holds a collection of models,
* along with other header data from a PDB file.
* It also contains methods for loading data from and
* writing data to a PDB file.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Mar 17 10:01:58 EST 2003
*/
public class PDBFile //extends ... implements ...
{
//{{{ Constants
    /** The ID code assigned to this molecule by the Protein Data Bank (http://www.pdb.org/) */
    public static final String      PDB_ID_PROP = "pdb-id";
    /** The crystallographic resolution in Angstroms */
    public static final String      RESOLUTION_PROP = "xtal-resolution";
//}}}

//{{{ Variable definitions
//##################################################################################################
    Collection      headers;
    Map             models;                     // Map< Model.getID(), Model >
    Props           props;
    
    boolean         useSegIDs       = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PDBFile()
    {
        headers = new ArrayList();
        models  = new GnuLinkedHashMap();
        props   = new Props();
    }
//}}}

//{{{ addModel
//##################################################################################################
    /** Adds the non-null Model m to this model and messages m.setModel() */
    public void addModel(Model m)
    {
        if(m == null) throw new IllegalArgumentException("Cannot add a null Model");
        String id = m.getID();
        if(models.containsKey(id)) throw new IllegalArgumentException(
            "Model ID collision: Model already contains Model '"+id+"'");
        
        models.put(id, m);
        m.setPDBFile(this);
    }
//}}}

//{{{ getModel, getFirstModel, getModels
//##################################################################################################
    /**
    * Returns the named Model or throws NoSuchElementException if not found.
    * Names are the IDs returned by Model.getID().
    */
    public Model getModel(String id)
    {
        if(models.containsKey(id)) return (Model)models.get(id);
        else throw new NoSuchElementException("Cannot find Model '"+id+"'");
    }
    
    /**
    * Returns the first model in this file,
    * or throws NoSuchElementException if not found.
    */
    public Model getFirstModel()
    {
        Iterator iter = models.values().iterator();
        if(iter.hasNext()) return (Model)iter.next();
        else throw new NoSuchElementException("Cannot find any Models");
    }
    /** Returns an unmodifiable Collection of Models */
    public Collection getModels()
    {
        return Collections.unmodifiableCollection(models.values());
    }
//}}}

//{{{ getProps
//##################################################################################################
    /** Returns a Props object with more info about this file */
    public Props getProps()
    { return props; }
//}}}

//{{{ read
//##################################################################################################
    /** Convenience: reads directly from a PDB file */
    public void read(String filename) throws IOException
    {
        LineNumberReader r = new LineNumberReader(new FileReader(filename));
        read(r);
        r.close();
    }

    /** Convenience: reads directly from a PDB file */
    public void read(File file) throws IOException
    {
        LineNumberReader r = new LineNumberReader(new FileReader(file));
        read(r);
        r.close();
    }

    /**
    * Reads a PDB file from a stream and extracts atom names and coordinates,
    * residue order, etc.
    * @param r a <code>LineNumberReader</code> hooked up to a PDB file
    */
    public void read(LineNumberReader r) throws IOException
    {
        String s, id;
        double x, y, z;
        Model   model   = null;
        Segment seg     = null;
        Residue res     = null;
        Atom    atom    = null;
        int atomcnt = 0, rescnt = 0;
        
        while((s = r.readLine()) != null)
        {
            try
            {
                if(s.startsWith("ATOM  ") || s.startsWith("HETATM")) //{{{
                {
                    if(model == null)
                    {
                        model = new Model("");
                        this.addModel(model);
                        seg = null; res = null;
                    }
                    
                    if(useSegIDs)
                    {
                        id = s.substring(72, 76);
                        if(id.trim().equals("")) id = s.substring(21, 22);
                    }
                    else id = s.substring(21, 22);
                    if(seg == null || !seg.getID().equals(id))
                    {
                        seg = new Segment(id);
                        model.addSegment(seg);
                        res = null;
                    }
                    
                    id = s.substring(17, 20).trim() + s.substring(22, 27).trim();
                    if(res == null || !res.getID().equals(id))
                    {
                        res = new Residue(s.substring(17, 20), s.substring(22, 27));
                        seg.addResidue(res);
                        rescnt++;
                    }
                    
                    id      = s.substring(12, 16);
                    x       = Double.parseDouble(s.substring(30, 38).trim());
                    y       = Double.parseDouble(s.substring(38, 46).trim());
                    z       = Double.parseDouble(s.substring(46, 54).trim());
                    atom    = new Atom(id, x, y, z);
                    atomcnt++;
                    
                    atom.setHet(s.startsWith("HETATM"));
                    atom.setAltConf(s.charAt(16));
                    //atom.serial = Integer.parseInt(s.substring(6, 11).trim());
                    res.addAtom(atom);
                    
                    atom.setOccupancy(Double.parseDouble(s.substring(54, 60).trim()));
                    atom.setTempFactor(Double.parseDouble(s.substring(60, 66).trim()));
                }//}}}
                else if(s.startsWith("MODEL ") && model == null)
                {
                    if(s.length() >= 14)    id = s.substring(10,14).trim();
                    else                    id = "";
                    model = new Model(id);
                    this.addModel(model);
                    seg = null; res = null;
                }
                else if(s.startsWith("ENDMDL"))
                {
                    model = null;
                    seg = null; res = null;
                }
                else
                {
                    headers.add(s);
                    
                    if(s.startsWith("HEADER")) this.getProps().setProperty(PDB_ID_PROP, s.substring(62,66));
                    /* XXX-JAVA13 * /
                    else if(s.startsWith("REMARK   2")) // extract information about the resolution
                    {
                        Pattern pat = Pattern.compile("\\d\\.\\d+");
                        Matcher mat = pat.matcher(s);
                        if(mat.find())
                        {
                            try {
                                Double.parseDouble(mat.group());
                                this.getProps().setProperty(RESOLUTION_PROP, mat.group());
                            } catch(NumberFormatException ex) {}
                        }
                    }
                    /* XXX-JAVA13 */
                }// headers
            }
            catch(Exception ex)
            {
                SoftLog.err.println("Error reading from PDB file, line "+r.getLineNumber()+": "+ex.getMessage());
            }
        }//while more lines
        //SoftLog.err.println(atomcnt+" atoms read and created");
        //SoftLog.err.println(rescnt+" residues read and created");
    }
//}}}

//{{{ writeAtoms
//##################################################################################################
    /**
    * Writes out a series of ATOM or HETATM lines.
    */
    public void writeAtoms(Collection atoms, OutputStream o)
    {
        Atom a;
        Residue res;
        Segment seg;
        Model model;
        int atomSerial = 1;
        PrintStream out = new PrintStream(o);
        StringBuffer sb;
        String s;
        DecimalFormat df2 = new DecimalFormat("0.00");
        DecimalFormat df3 = new DecimalFormat("0.000");
        
        for(Iterator iter = atoms.iterator(); iter.hasNext(); atomSerial++)
        {
            try
            {
                a = (Atom)iter.next();
                res = a.getResidue();
                seg = res.getSegment();
                model = seg.getModel();
                sb = new StringBuffer(80);
                
                if(a.isHet())   sb.append("HETATM");
                else            sb.append("ATOM  ");
                sb.append(Strings.justifyRight(Integer.toString(atomSerial), 5));
                sb.append(" ");
                sb.append(a.getID());
                sb.append(a.getAltConf());
                sb.append(Strings.justifyLeft(res.getType(), 3));
                sb.append(" ");
                if(useSegIDs)   sb.append(" ");
                else            sb.append(seg.getID().charAt(0));
                
                s = res.getNumber();
                if(s.charAt(s.length()-1) < '0' || s.charAt(s.length()-1) > '9')
                    sb.append(Strings.justifyRight(s, 5));
                else
                    sb.append(Strings.justifyRight(s, 4)).append(" ");
                sb.append("   ");
                
                sb.append(Strings.justifyRight(df3.format(a.getX()), 8));
                sb.append(Strings.justifyRight(df3.format(a.getY()), 8));
                sb.append(Strings.justifyRight(df3.format(a.getZ()), 8));
                sb.append(Strings.justifyRight(df2.format(a.getOccupancy()), 6));
                sb.append(Strings.justifyRight(df2.format(a.getTempFactor()), 6));
                sb.append("      ");
                if(useSegIDs)   sb.append(Strings.justifyLeft(seg.getID(), 4));
                else            sb.append("    ");
                sb.append("    "); // element and charge
                
                out.println(sb);
            }
            catch(NullPointerException ex) {}
        }
        
        out.flush();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

