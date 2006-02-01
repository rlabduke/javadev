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
* <p>TER cards are counted by incrementing Residue.sectionID: before the first
* TER, all Residues have sectionID = 0; after the first TER, sectionID = 1;
* after the second TER, sectionID = 2; etc.
* SectionID is NOT incremented just because chainID or segmentID changes,
* so further processing is necessary in most cases to create "real" sections.
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
    
    /** The set of all Models */
    CoordinateFile coordFile;
    
    /** The current Model */
    Model       model;
    
    /** A Map&lt;String, ModelState&gt; for this Model */
    Map         states;
    
    /** A Map&lt;String, Residue&gt; based on PDB naming */
    Map         residues;
    
    /** A surrogate atom serial number if necessary */
    int         autoSerial;
    
    /** Number of TER cards encountered so far */
    int         countTER;
    
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
        coordFile   = new CoordinateFile();
        model       = null;
        states      = new HashMap();
        residues    = new HashMap();
        autoSerial  = -9999;
        countTER    = 0;
    }
    
    void clearData()
    {
        coordFile   = null;
        model       = null;
        states      = null;
        residues    = null;
        autoSerial  = -9999;
        countTER    = 0;
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
    public CoordinateFile read(File f) throws IOException
    {
        Reader r = new FileReader(f);
        CoordinateFile rv = read(r);
        rv.setFile(f);
        r.close();
        return rv;
    }

    public CoordinateFile read(InputStream is) throws IOException
    {
        return read(new InputStreamReader(is));
    }

    public CoordinateFile read(Reader r) throws IOException
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
    public CoordinateFile read(LineNumberReader r) throws IOException
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
                    if(model != null) model.setStates(states);
                    
                    model = new Model(s.substring(10,14).trim());
                    coordFile.add(model);
                    states.clear();
                    residues.clear();
                }
                else if(s.startsWith("ENDMDL"))
                {
                    if(model != null) model.setStates(states);
                    
                    model = null;
                }
                else if(s.startsWith("TER"))
                {
                    // If we clear out our list of residues-by-name, then it's
                    // OK to do something like this:
                    //  chain B atoms...
                    //  TER
                    //  chain B atoms of same names...
                    // Although it seems awful, this is often what you get when
                    // doing a symmetry expansion in crystallography.
                    // So even though it's not very nice, we *should* allow it:
                    residues.clear();
                    
                    // Label our residues with how many TERs precede them.
                    // Thus, the identical "chain B Ile 47" 's above are distinct.
                    countTER++;
                }
                else if(s.startsWith("MASTER") || s.startsWith("END"))
                {
                    // These lines are useless. Ignore them.
                }
                else // headers
                {
                    // Headers are just saved for later and then output again
                    if(s.startsWith("HEADER") && s.length() >= 66)
                        coordFile.setIdCode(s.substring(62,66));
                    String six;
                    if(s.length() >= 6) six = s.substring(0,6);
                    else                six = s;
                    coordFile.addHeader(intern(six), s);
                }
            }
            catch(IndexOutOfBoundsException ex)
            { SoftLog.err.println("Error reading from PDB file, line "+r.getLineNumber()+": "+ex.getMessage()); }
            catch(NumberFormatException ex)
            { SoftLog.err.println("Error reading from PDB file, line "+r.getLineNumber()+": "+ex.getMessage()); }
        }//while more lines
        if(model != null) model.setStates(states);
                    
        
        CoordinateFile rv = coordFile;
        clearData();

        // This little dance makes sure that all alt confs define some state for every atom.
        for(Iterator iter = rv.getModels().iterator(); iter.hasNext(); )
        {
            Model m = (Model) iter.next();
            try { m.fillInStates(false); }
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
        ModelState mState = makeState(altConf);
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
            coordFile.add(model);
            states.clear();
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
            r.sectionID = countTER;
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

//{{{ makeState
//##################################################################################################
    /**
    * Returns a conformation identified by its one letter code,
    * in the form of a ModelState;
    * or <b>creates it if it didn't previously exist</b>.
    * <p>If the ID is something other than space (' '), the
    * new conformation will have the default conformation set
    * as its parent. If a default conformation does not exist
    * yet, it will also be created.
    */
    ModelState makeState(String stateID)
    {
        ModelState state = (ModelState) states.get(stateID);
        if(state == null)
        {
            state = new ModelState();
            states.put(stateID, state);
            if(! " ".equals(stateID))
                state.setParent(this.makeState(" "));
        }
        return state;
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
            String elem = null;
            if(s.length() >= 78) elem = getElement(s.substring(76,78).trim());
            if(elem == null) elem = getElement(id.substring(0,2));
            if(elem == null) elem = getElement(id.substring(1,2));
            if(elem == null) elem = "XX";
            
            a = new Atom(intern(id), elem, s.startsWith("HETATM"));
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

//{{{ getElement
//##############################################################################
    static final String[] allElementNames = {
"H", "HE",
"LI", "BE", "B", "C", "N", "O", "F", "NE",
"NA", "MG", "AL", "SI", "P", "S", "CL", "AR",
"K", "CA", "SC", "TI", "V", "CR", "MN", "FE", "CO", "NI", "CU", "ZN", "GA",
    "GE", "AS", "SE", "BR", "KR",
"RB", "SR", "Y", "ZR", "NB", "MO", "TC", "RU", "RH", "PD", "AG", "CD", "IN",
    "SN", "CB", "TE", "I", "XE",
"CS", "BA", "LA", "CE", "PR", "ND", "PM", "SM", "EU", "GD", "TB", "DY", "HO",
    "ER", "TM", "YB", "LU", "HF", "TA", "W", "RE", "OS", "IR", "PT", "AU",
    "HG", "TL", "PB", "BI", "PO", "AT", "RN",
"FR", "RA", "AC", "TH", "PA", "U", "NP", "PU", "AM", "CM", "BK", "CF", "ES",
    "FM", "MD", "NO", "LR", "RF", "DB", "SG", "BH", "HS", "MT", "DS"
    };
    static Map elementNames = null;
    /**
    * Pass in a valid element symbol, or D, T, or Q (1 or 2 chars, uppercase).
    * Get back a valid element symbol.
    * Returns null for things we don't recognize at all.
    */
    static String getElement(String name)
    {
        if(elementNames == null)
        {
            elementNames = new HashMap();
            for(int i = 0; i < allElementNames.length; i++)
                elementNames.put(allElementNames[i], allElementNames[i]);
            elementNames.put("D", "H"); // deuterium
            elementNames.put("T", "H"); // tritium
            elementNames.put("Q", "Q"); // NMR pseudo atoms
        }
        return (String) elementNames.get(name);
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
