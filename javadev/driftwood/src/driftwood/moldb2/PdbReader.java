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
import driftwood.data.*;
import driftwood.util.*;
//}}}
/**
* <code>PdbReader</code> is a utility class for loading Models
* from PDB-format files.
*
* <p>PdbReader and PdbWriter fail to deal with the following kinds of records:
* <ul>  <li>SIGATM</li>
*       <li>ANISOU</li>
*       <li>SIGUIJ</li>
*       <li>CONECT</li> </ul>
* <p>Actually, PdbReader puts all of them in with the headers, and
* PdbWriter spits them all back out, but it may not put them in the
* right part of the file.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 11 11:15:15 EDT 2003
*/
public class PdbReader //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    // Shared data structures
    
    /** The group of Models */
    ModelGroup  group;
    
    /** The current Model */
    Model       model;
    
    /** A Map&lt;String, Residue&gt; based on PDB naming */
    Map         residues;
    
    /** A surrogate atom serial number if necessary */
    int         autoSerial;
    
    /** A map for intern'ing Strings */
    CheapSet    stringCache = new CheapSet();
    
    /** If true, drop leading and trailing whitespace from seg IDs */
    boolean     trimSegID       = false;
    /** If true, segment IDs will define chains and thus must be consistent within one residue. */
    boolean     useSegID        = false;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PdbReader()
    {
        clearData();
    }
//}}}

//{{{ init/clearData, intern
//##################################################################################################
    void initData()
    {
        group       = new ModelGroup();
        model       = null;
        residues    = new HashMap();
        autoSerial  = -9999;
    }
    
    void clearData()
    {
        group       = null;
        model       = null;
        residues    = null;
        autoSerial  = -9999;
        stringCache.clear();
    }
    
    /** Like String.intern(), but the cache is discarded after reading the file. */
    String intern(String s)
    {
        String t = (String) stringCache.get(s);
        if(t == null)
        {
            stringCache.add(s);
            return s;
        }
        else return t;
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
        return read(new LineNumberReader(r));
    }
//}}}

//{{{ read
//##################################################################################################
    /**
    * Reads a PDB file from a stream and extracts atom names and coordinates,
    * residue order, etc.
    * @param r a <code>LineNumberReader</code> hooked up to a PDB file
    */
    public ModelGroup read(LineNumberReader r) throws IOException
    {
        initData();
        
        String s;
        while((s = r.readLine()) != null)
        {
            try
            {
                if(s.startsWith("ATOM  ") || s.startsWith("HETATM"))
                {
                    readAtom(s);
                }
                else if(s.startsWith("MODEL ") && s.length() >= 14)
                {
                    model = new Model(s.substring(10,14).trim());
                    group.add(model);
                    residues.clear();
                }
                else if(s.startsWith("ENDMDL"))
                {
                    model = null;
                }
                else if(s.startsWith("TER") || s.startsWith("MASTER") || s.startsWith("END"))
                {
                    // These lines are useless. Ignore them.
                }
                else // headers
                {
                    // Headers are just saved for later and then output again
                    if(s.startsWith("HEADER") && s.length() >= 66)
                        group.setIdCode(s.substring(62,66));
                    String six;
                    if(s.length() >= 6) six = s.substring(0,6);
                    else                six = s;
                    group.addHeader(intern(six), s);
                }
            }
            catch(IndexOutOfBoundsException ex)
            { SoftLog.err.println("Error reading from PDB file, line "+r.getLineNumber()+": "+ex.getMessage()); }
            catch(NumberFormatException ex)
            { SoftLog.err.println("Error reading from PDB file, line "+r.getLineNumber()+": "+ex.getMessage()); }
        }//while more lines
        
        ModelGroup rv = group;
        clearData();

        // This little dance makes sure that all alt confs define some state for every atom.
        for(Iterator iter = rv.getModels().iterator(); iter.hasNext(); )
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
        
        return rv;
    }
//}}}

//{{{ readAtom
//##################################################################################################
    void readAtom(String s) throws NumberFormatException
    {
        checkModel();
        Residue r = makeResidue(s);
        Atom    a = makeAtom(r, s);
        
        String serial = s.substring(6, 11);
        AtomState state = new AtomState(a, serial);
        String altConf = intern(s.substring(16, 17));
        state.setAltConf(altConf);
        
        // We're now ready to add this to a ModelState
        // It's possible this state will have no coords, etc
        ModelState mState = model.makeState(altConf);
        // This protects us against duplicate lines
        // that re-define the same atom and state!
        try { mState.add(state); }
        catch(AtomException ex) { SoftLog.err.println(ex.getMessage()); }
        
        double x, y, z;
        x = Double.parseDouble(s.substring(30, 38).trim());
        y = Double.parseDouble(s.substring(38, 46).trim());
        z = Double.parseDouble(s.substring(46, 54).trim());
        state.setXYZ(x, y, z);
        
        if(s.length() >= 60)
        {
            String q = s.substring(54, 60).trim();
            if(q.length() > 0) state.setOccupancy(Double.parseDouble(q));
        }
        if(s.length() >= 66)
        {
            String b = s.substring(60, 66).trim();
            if(b.length() > 0) state.setTempFactor(Double.parseDouble(b));
        }
        if(s.length() >= 80 && s.charAt(78) != ' ')
        {
            // These are formatted as 2+, 1-, etc.
            if(s.charAt(79) == '-')         state.setCharge('0' - s.charAt(78));
            else if(s.charAt(79) == '+')    state.setCharge(s.charAt(78) - '0');
            // else do nothing -- sometimes this field is used for other purposes (?)
        }
    }
//}}}

//{{{ checkModel, makeResidue
//##################################################################################################
    /** Makes sure that a model exists for things to go into */
    void checkModel()
    {
        if(model == null)
        {
            model = new Model("1");
            group.add(model);
            residues.clear();
        }
    }
    
    /** Retrieves a residue, creating it if necessary */
    Residue makeResidue(String s) throws NumberFormatException
    {
        checkModel();
        
        // Always pretend there is a fully space-padded field
        // present, because lines may be different lengths.
        String segID = "    ";
        if(s.length() > 72)
        {
            if(s.length() >= 76)    segID = s.substring(72,76);
            else                    segID = Strings.justifyLeft(s.substring(72), 4);
        }
        
        String key = s.substring(17,27);
        if(useSegID) key += segID;
        Residue r = (Residue)residues.get(key);
        
        if(r == null)
        {
            if(trimSegID) segID = segID.trim();
                    segID   = intern(segID);
            String  chainID = intern(s.substring(21,22));
            String  seqNum  = intern(s.substring(22,26));
            String  insCode = intern(s.substring(26,27));
            String  resName = intern(s.substring(17,20));
            r = new Residue(chainID, segID, seqNum, insCode, resName);
            residues.put(key, r);
            try
            {
                model.add(r);
            }
            catch(ResidueException ex)
            {
                SoftLog.err.println("Logical error: residue "+r+" already exists in model.");
                ex.printStackTrace(SoftLog.err);
            }
        }
        
        return r;
    }
//}}}

//{{{ makeAtom
//##################################################################################################
    /**
    * Returns the named atom from the given (non-null)
    * Residue, or creates it if it doesn't exist yet.
    */
    Atom makeAtom(Residue r, String s)
    {
        String  id  = s.substring(12, 16);
        Atom    a   = r.getAtom(id);
        if(a == null)
        {
            a = new Atom(intern(id), s.startsWith("HETATM"));
            try { r.add(a); }
            catch(AtomException ex)
            {
                System.err.println("Logical error!");
                ex.printStackTrace();
            }
        }
        return a;
    }
//}}}

//{{{ setUseSegID
//##################################################################################################
    /**
    * Determines whether or not segment IDs determine resiude identity;
    * i.e., whether otherwise identical residue specifications with
    * different segIDs will be treated as being the same or different.
    * In most cases, this should be left at its default value of <code>false</code>.
    * However, for structures without chain IDs but with multiple chains
    * (e.g. structures coming out of CNS refinement) it should be <code>true</code>.
    */
    public void setUseSegID(boolean b)
    { useSegID = b; }
//}}}

//{{{ extractSheetDefinitions - out of commission
//##################################################################################################
    /**
    * Given PDB headers and a Model, returns Sets of Residues that belong to various beta sheets.
    * The sets are organized into a Map with the sheet names as its keys.
    *
    * TODO: this should really return more detail, like individual strands.
    *
    * @param headers    the PDB headers, each line as it's own String
    * @param model      the model in which to find the residues
    * @return Map&lt;String, Set&lt;Residue&gt;&gt;
    */
    /*
    static public Map extractSheetDefinitions(Collection headers, Model model)
    {
        Map sheets = new UberMap();
        for(Iterator hi = headers.iterator(); hi.hasNext(); )
        {
            String h = (String) hi.next();
            if(h.startsWith("SHEET "))
            {
                String first = h.substring(21, 27) + h.substring(17, 20);
                String last  = h.substring(32, 38) + h.substring(28, 31);
                String sheetID = h.substring(11,14);
                
                Set sheet = (Set) sheets.get(sheetID);
                if(sheet == null)
                {
                    sheet = new UberSet();
                    sheets.put(sheetID, sheet);
                }
                
                Residue res1 = model.getResidue(first);
                Residue res2 = model.getResidue(last);
                if(res1 == null || res2 == null) continue;
                do {
                    sheet.add(res1);
                    res1 = res1.getNext(model);
                } while(res1 != null && !res1.equals(res2));
                sheet.add(res2);
            }
        }
        return sheets;
    }
    */
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

