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
import driftwood.star.*;
//}}}
/**
* <code>CifReader</code> loads mmCIF files into the MolDB2 data structures,
* albeit with some loss of information.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 16 14:37:06 EDT 2004
*/
public class CifReader //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    // In the order they appear in a PDB record. Non-required lists may be null.
    int     numRecords      = 0;
    List    groupPdb        = null; // NOT required, ATOM or HETATM
    List    atomSiteId      = null; // required, any string
    List    pdbAtomName     = null; // NOT required
    List    labelAtomId     = null; // required, must be converted to PDB style
    List    typeSymbol      = null; // required, usually the element name (not in PDB)
    List    labelAltId      = null; // required, . or ? should become ' '
    List    labelCompId     = null; // required, case insensitive (uppercased)
    List    labelAsymId     = null; // required, any string
    List    labelSeqId      = null; // required, an integer <= 1 (any string for us)
    List    labelEntityId   = null; // required, any string (not used by us?)
    List    pdbInsCode      = null; // NOT required, any string
    List    cartnX          = null; // we require, a float
    List    cartnY          = null; // we require, a float
    List    cartnZ          = null; // we require, a float
    List    occupancy       = null; // NOT required, a float
    List    bIsoOrEquiv     = null; // NOT required, a float
    List    uIsoOrEquiv     = null; // NOT required, B = 8*pi*pi*U
    List    pdbModelNum     = null; // NOT required, any string
    
    ModelGroup modelGroup = null;
    Map modelMap = null; // maps model names as Strings to Model objects
    Map resMap = null; // maps res pseudonames as Strings to Residue objects
    int fakeResNumber = 1; // used for waters, etc. that lack residue numbers
//}}}

//{{{ Constructor(s)
//##############################################################################
    public CifReader()
    {
        super();
    }
//}}}

//{{{ read (convenience)
//##################################################################################################
    public ModelGroup read(File f) throws IOException
    {
        Reader r = new FileReader(f);
        ModelGroup rv = read(r);
        rv.setFile(f);
        r.close();
        return rv;
    }

    public ModelGroup read(InputStream is) throws IOException
    {
        return read(new InputStreamReader(is));
    }

    public ModelGroup read(Reader r) throws IOException
    {
        try
        {
            StarFile starFile = new StarReader().parse(new LineNumberReader(r));
            return read(starFile);
        }
        catch(java.text.ParseException ex)
        {
            throw new IOException("Unable to parse mmCIF file as STAR: "+ex.getMessage());
        }
    }
//}}}

//{{{ read
//##############################################################################
    /**
    * Creates a ModelGroup from the specified CIF file object.
    * @throws IOException if the data is internally inconsistent
    */
    public ModelGroup read(StarFile dom) throws IOException
    {
        // Set up lists of data items
        Iterator dataBlocks = dom.getDataBlockNames().iterator();
        if(!dataBlocks.hasNext())
            throw new IOException("STAR file has no data blocks");
        DataBlock db = dom.getDataBlock((String)dataBlocks.next());
        loadItems(db);
        
        this.modelGroup = new ModelGroup();
        this.modelMap   = new HashMap();
        this.resMap     = new HashMap();
        this.fakeResNumber = 1;
        for(int i = 0; i < numRecords; i++)
        {
            Model       model   = getModel(i);
            Residue     res     = getResidue(model, i);
            Atom        atom    = getAtom(res, i);
            AtomState   as      = buildAtomState(atom, i);
            ModelState  ms      = model.makeState(as.getAltConf());
            
            // Waters, etc. don't have sequence IDs
            // Assume same residue until we get an atom name collision.
            if(ms.hasState(atom))
            {
                fakeResNumber++;    // bump up the residue number
                i--;                // reprocess this line
                continue;
            }
            
            try { ms.add(as); }
            catch(AtomException ex) { ex.printStackTrace(); } // logically unreachable
        }
        
        // This little dance makes sure that all alt confs define some state for every atom.
        for(Iterator iter = modelGroup.getModels().iterator(); iter.hasNext(); )
        {
            Model m = (Model) iter.next();
            try { m.fillInStates(); }
            catch(AtomException ex)
            {
                // This shouldn't ever be able to happen...
                SoftLog.err.println("Unable to find states for all atoms in model!");
                ex.printStackTrace(SoftLog.err);
            }
        }
        
        return this.modelGroup;
    }
//}}}

//{{{ loadItems
//##############################################################################
    /** Loads needed items as Lists of Strings from the given data cell. */
    void loadItems(DataCell data) throws IOException
    {
        groupPdb        = data.getItem("_atom_site.group_PDB");
        atomSiteId      = data.getItem("_atom_site.id");
        pdbAtomName     = data.getItem("_atom_site.pdbx_PDB_atom_name");
        labelAtomId     = data.getItem("_atom_site.label_atom_id");
        typeSymbol      = data.getItem("_atom_site.type_symbol");
        labelAltId      = data.getItem("_atom_site.label_alt_id");
        labelCompId     = data.getItem("_atom_site.label_comp_id");
        labelAsymId     = data.getItem("_atom_site.label_asym_id");
        labelSeqId      = data.getItem("_atom_site.label_seq_id");
        labelEntityId   = data.getItem("_atom_site.label_entity_id");
        pdbInsCode      = data.getItem("_atom_site.pdbx_PDB_ins_code");
        cartnX          = data.getItem("_atom_site.Cartn_x");
        cartnY          = data.getItem("_atom_site.Cartn_y");
        cartnZ          = data.getItem("_atom_site.Cartn_z");
        occupancy       = data.getItem("_atom_site.occupancy");
        bIsoOrEquiv     = data.getItem("_atom_site.B_iso_or_equiv");
        uIsoOrEquiv     = data.getItem("_atom_site.U_iso_or_equiv");
        pdbModelNum     = data.getItem("_atom_site.pdbx_PDB_model_num");
        
        numRecords = labelAtomId.size();
        if(numRecords == 0
        || atomSiteId.size()    != numRecords
        || typeSymbol.size()    != numRecords
        || labelAltId.size()    != numRecords
        || labelCompId.size()   != numRecords
        || labelAsymId.size()   != numRecords
        || labelSeqId.size()    != numRecords
        || labelEntityId.size() != numRecords
        || cartnX.size()        != numRecords
        || cartnY.size()        != numRecords
        || cartnZ.size()        != numRecords)
            throw new IOException("Required atom_site data items are missing or disagree in length.");
        
        if(groupPdb.size()      == 0) groupPdb      = null;
        if(pdbAtomName.size()   == 0) pdbAtomName   = null;
        if(pdbInsCode.size()    == 0) pdbInsCode    = null;
        if(occupancy.size()     == 0) occupancy     = null;
        if(bIsoOrEquiv.size()   == 0) bIsoOrEquiv   = null;
        if(uIsoOrEquiv.size()   == 0) uIsoOrEquiv   = null;
        if(pdbModelNum.size()   == 0) pdbModelNum   = null;
    }
//}}}

//{{{ getModel
//#############################################################################
    /** Returns the model for the given record number. */
    Model getModel(int i)
    {
        String modelName = (pdbModelNum == null ? "1" : (String) pdbModelNum.get(i));
        Model m = (Model) modelMap.get(modelName);
        if(m == null)
        {
            m = new Model(modelName);
            modelMap.put(modelName, m);
            this.modelGroup.add(m);
        }
        return m;
    }
//}}}

//{{{ getResidue
//##############################################################################
    /** Returns the residue for the given record number. */
    Residue getResidue(Model m, int i)
    {
        String asymId = (String) labelAsymId.get(i);
        // Waters, etc. don't have sequence IDs
        // Assume same residue until we get an atom name collision.
        // Collision is handled at the top level, before adding AtomState to ModelState
        String  seqId = (String) labelSeqId.get(i);
        if(".".equals(seqId) || "?".equals(seqId)) seqId = Integer.toString(fakeResNumber);
        // Residue names are case-insensitive, so we convert to uppercase.
        String compId = ((String) labelCompId.get(i)).toUpperCase();
        
        String resLookup = m.getName()+"$"+asymId+"$"+seqId+"$"+compId;
        Residue r = (Residue) resMap.get(resLookup);
        if(r == null)
        {
            //String insCode = (pdbInsCode == null ? " " : (String) pdbInsCode.get(i));
            String insCode = " ";
            if(pdbInsCode != null)
            {
                insCode = (String) pdbInsCode.get(i);
                //if(insCode.length() > 1) insCode = insCode.substring(0,1);
                if(".".equals(insCode) || "?".equals(insCode) || insCode.length() == 0) insCode = " ";
            }
            r = new Residue(asymId, "", seqId, insCode, compId);
            resMap.put(resLookup, r);
            try { m.add(r); }
            catch(ResidueException ex) { ex.printStackTrace(); } // logical error
        }
        return r;
    }
//}}}

//{{{ getAtom
//##############################################################################
    /** Returns the appropriate Atom for the line number */
    Atom getAtom(Residue r, int i)
    {
        String name = getAtomName(i);
        Atom a = r.getAtom(name);
        if(a == null)
        {
            a = new Atom(name, (groupPdb == null ? false : ((String)groupPdb.get(i)).equals("HETATM")));
            try { r.add(a); }
            catch(AtomException ex) { ex.printStackTrace(); }
        }
        return a;
    }
//}}}

//{{{ getAtomName
//##############################################################################
    String getAtomName(int i)
    {
        if(pdbAtomName != null)
        {
            String atomName = (String) pdbAtomName.get(i);
            if(atomName.length() == 4) return atomName;
        }
        
        // Our rules for guessing the PDB atom name for 2- and 3-char names:
        // 1. The second character (of four) must be a letter
        // 2. A one-char element name implies a leading space (or digit)
        // 3. Known het atoms often have two-char element names
        // 4. By default, assume a leading space

        String atomName = (String) labelAtomId.get(i);
        int len = atomName.length();
        if(len < 1) return "    ";
        else if(len == 1) return " "+atomName+"  ";
        else if(len == 2)
        {
            if(!Character.isLetter(atomName.charAt(0)))
                return atomName+"  ";
            else if(!Character.isLetter(atomName.charAt(1)))
                return " "+atomName+" ";
            else if(typeSymbol != null && ((String)typeSymbol.get(i)).length() == 1)
                return " "+atomName+" ";
            else if(groupPdb != null && ((String)groupPdb.get(i)).equals("HETATM"))
                return atomName+"  ";
            else
                return " "+atomName+" ";
        }
        else if(len == 3)
        {
            if(!Character.isLetter(atomName.charAt(0)))
                return atomName+" ";
            else if(!Character.isLetter(atomName.charAt(1)))
                return " "+atomName;
            else if(typeSymbol != null && ((String)typeSymbol.get(i)).length() == 1)
                return " "+atomName;
            else if(groupPdb != null && ((String)groupPdb.get(i)).equals("HETATM"))
                return atomName+" ";
            else
                return " "+atomName;
        }
        else if(len == 4) return atomName;
        else return atomName.substring(0, 4); // length() > 4
    }
//}}}

//{{{ buildAtomState
//##############################################################################
    /** Creates an AtomState object for the specified line */
    AtomState buildAtomState(Atom a, int i)
    {
        AtomState as = new AtomState(a, (String) atomSiteId.get(i));
        
        if(labelAltId != null)
        {
            String altId = (String) labelAltId.get(i);
            //if(altId.length() > 1) altId = altId.substring(0,1);
            if(".".equals(altId) || "?".equals(altId) || altId.length() == 0) altId = " ";
            as.setAltConf(altId);
        }
        if(cartnX != null)
        {
            try { as.setX(Double.parseDouble((String) cartnX.get(i))); }
            catch(NumberFormatException ex) {}
        }
        if(cartnY != null)
        {
            try { as.setY(Double.parseDouble((String) cartnY.get(i))); }
            catch(NumberFormatException ex) {}
        }
        if(cartnZ != null)
        {
            try { as.setZ(Double.parseDouble((String) cartnZ.get(i))); }
            catch(NumberFormatException ex) {}
        }
        if(occupancy != null)
        {
            try { as.setOccupancy(Double.parseDouble((String) occupancy.get(i))); }
            catch(NumberFormatException ex) {}
        }
        
        
        if(bIsoOrEquiv != null)
        {
            try { as.setTempFactor(Double.parseDouble((String) bIsoOrEquiv.get(i))); }
            catch(NumberFormatException ex) {}
        }
        else if(uIsoOrEquiv != null)
        {
            try { as.setTempFactor(8 * Math.PI * Math.PI * Double.parseDouble((String) uIsoOrEquiv.get(i))); }
            catch(NumberFormatException ex) {}
        }
        
        return as;
    }
//}}}

//{{{ main (for testing)
//##############################################################################
    public static void main(String[] args) throws IOException
    {
        ModelGroup mg = new CifReader().read(System.in);
        PdbWriter w = new PdbWriter(System.out);
        w.writeModelGroup(mg, Collections.EMPTY_MAP);
        w.close();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

