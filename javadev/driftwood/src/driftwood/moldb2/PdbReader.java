// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.*;
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
    HashMap     v2tov3Map       = null;
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
    void initData() throws IOException
    {
        coordFile   = new CoordinateFile();
        model       = null;
        states      = new HashMap();
        residues    = new HashMap();
        autoSerial  = -9999;
        countTER    = 0;
        v2tov3Map   = new HashMap();

        InputStream is = this.getClass().getResourceAsStream("PDBv2toPDBv3.hashmap.txt");
        if(is == null) throw new IOException("File not found in JAR: singleres.pdb");
        readV2toV3map(is);
    }

    private void readV2toV3map(InputStream is) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = reader.readLine())!=null) {
        if (!line.startsWith("#")) {
          String[] strings = Strings.explode(line, ':', false, false);
          //System.out.println(strings[1] + " and " + strings[0]);
          v2tov3Map.put(strings[1], strings[0]);
        }
      }
      reader.close();
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
        v2tov3Map   = null;
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
        InputStream r = new FileInputStream(f);
        CoordinateFile rv = read(r);
        rv.setFile(f);
        r.close();
        return rv;
    }

    public CoordinateFile read(InputStream is) throws IOException
    {
      // Test for GZIPped files
      is = new BufferedInputStream(is);
      is.mark(10);
      if(is.read() == 31 && is.read() == 139)
      {
        // We've found the gzip magic numbers...
        is.reset();
        is = new GZIPInputStream(is);
      }
      else is.reset();
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

        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long totalFree = (freeMemory + (maxMemory - allocatedMemory));

        //SoftLog.err.println("max memory: " + maxMemory /1024);
        //SoftLog.err.println("total free memory: " + (freeMemory + (maxMemory - allocatedMemory)) / 1024);

        int pdbv2atoms = 0;
        String s;
        while(((s = r.readLine()) != null)&&((double)totalFree/(double)maxMemory > 0.05))
        {
          allocatedMemory = runtime.totalMemory();
          freeMemory = runtime.freeMemory();
          totalFree = (freeMemory + (maxMemory - allocatedMemory));
            try
            {
                if(s.startsWith("ATOM  ") || s.startsWith("HETATM"))
                {
                  if (isVersion23(s)) pdbv2atoms++;
                  readAtom(s);
                }
                else if(s.startsWith("ANISOU"))
                {
                    readAnisoU(s);
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
            { SoftLog.err.println("Error reading from PDB file, line "+r.getLineNumber()+": "+s); }
            catch(NumberFormatException ex)
            { SoftLog.err.println("Error reading from PDB file, line "+r.getLineNumber()+": "+s); }
        }//while more lines
        if (!((double)totalFree/(double)maxMemory > 0.05)) {
          SoftLog.err.println("PDB file too large, aborting read, removing partial model "+model);
          coordFile.remove(model);
          model = null;
        }
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
        // This sets up secondary structure assignments
        rv.setSecondaryStructure(new PdbSecondaryStructure(rv.getHeaders()));

        // This sets up disulfide bond residue-residue pairings
        rv.setDisulfides(new PdbDisulfides(rv.getHeaders()));

        rv.setPdbv2Count(pdbv2atoms);

        return rv;
    }
//}}}

//{{{ isVersion23
/** for testing an atom to see if it is pdb v2.3 **/
public boolean isVersion23(String atomLine) {
  String atomRes = atomLine.substring(12, 16) + " " + atomLine.substring(17, 20);
  return v2tov3Map.containsKey(atomRes);
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
        catch(AtomException ex)
        {
            // Sloppy PDB-like files (like the ligand ones from Rosetta)
            // sometimes duplicate atom names, like every carbon is " C  ".
            // But moldb2 requires that every atom have a unique name.
            // So we append a number after the 4-char name (" C  1", " C  2"),
            // which will be used internally but will be cut off again when
            // the file is saved back to PDB.  Should be pretty seamless.

            // We retain this warning message, because duplicate atom defs
            // still IS an error in many cases and shouldn't be silenced.
            SoftLog.err.println(ex.getMessage());

            a = makeUniqueAtom(r, s);
            state = new AtomState(a, serial);
            state.setAltConf(altConf);
            try { mState.add(state); }
            catch(AtomException ex2)
            {
                // This is a true error condition -- shouldn't ever be able to get here.
                ex2.printStackTrace();
                System.err.println("Logical error in PDB construction!");
            }
        }

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
        if(s.length() > 80)
        {
            state.setPast80(intern(s.substring(80)));
        }
    }
//}}}

//{{{ readAnisoU
//##################################################################################################
    void readAnisoU(String s) throws NumberFormatException
    {
        checkModel();
        Residue r = makeResidue(s);
        Atom    a = makeAtom(r, s);
        //Atom    a = r.getAtom(s.substring(12, 16));
        //if(a == null) throw new AtomException("Logical error: ANISOU should always follow ATOM or HEATATM!");

        String altConf = intern(s.substring(16, 17));
        ModelState mState = makeState(altConf);
        try
        {
            AtomState state = mState.get(a);
            state.setAnisoU(s);
        }
        catch(AtomException ex)
        {
            SoftLog.err.println("Logical error: ANISOU should always follow ATOM or HEATATM!");
            ex.printStackTrace(SoftLog.err);
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
        //if(useSegID) key += segID;
        if (segID != "    ")
        {
            key += segID;
        }
        Residue r = (Residue)residues.get(key);

        if(r == null)
        {
            if(trimSegID) segID = segID.trim();
                    segID   = intern(segID);
            String  chainID = intern(s.substring(20,22));
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
            if (model != null)
              state.setName(coordFile.getIdCode()+" "+model.toString());
            states.put(stateID, state);
            if(! " ".equals(stateID))
                state.setParent(this.makeState(" "));
        }
        return state;
    }
//}}}

//{{{ makeAtom, makeUniqueAtom
//##################################################################################################
    /**
    * Returns the named atom from the given (non-null)
    * Residue, or creates it if it doesn't exist yet.
    */
    Atom makeAtom(Residue r, String s)
    {
        // The usual case -- create a new atom only if needed
        return makeAtomImpl(r, s, s.substring(12, 16));
    }

    /** Forces creation of a brand new atom with a unique name */
    Atom makeUniqueAtom(Residue r, String s)
    {
        String id = s.substring(12, 16);
        for(int i = 1; true; i++)
        {
            String extID = id+i; // more than 4 chars, intentionally
            Atom a = r.getAtom(extID);
            if(a == null) return makeAtomImpl(r, s, extID);
        }
    }

    Atom makeAtomImpl(Residue r, String s, String id)
    {
        Atom a = r.getAtom(id);
        if(a == null)
        {
            String elem = null;
            String resName = r.getName();
            if(s.length() >= 78) elem = getElement(s.substring(76,78).trim(), resName);
            if(elem == null) elem = getElement(id.substring(0,2), resName);
            if(elem == null) elem = getElement(id.substring(1,2), resName);
            if(elem == null) elem = getElement(id.substring(0,1), resName);
            // VMD produces some (but not all) H with names like _1HB (instead of 1HB_ or _HB1)
            if(elem == null) elem = getElement(id.substring(2,3), resName);
            if(elem == null) elem = "XX";
            //System.out.print("atom:"+id+"="+elem+" ");

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
    // ambiguous_resnames are for identifying atoms that could be hydrogens or heavy atoms
    // e.g. Hg.
    static final String[] ambiguous_resnames = {
      // residues with He (currently none 070711)
    "PHF", "HF3", "HF5", // residues with Hf
    " HG", "HG2","HGB","HGC","HGI","MAC","MBO","MMC","PHG","PMB","AAS","AMS","BE7","CMH","EMC","EMT", // residues with Hg
    " HO","HO3" //residues with Ho
    // residues with Hs. (currently none 070711)
    };
    static Map elementNames = null;
    static ArrayList ambigAtomResidues = null;
    /**
    * Pass in a valid element symbol, or D, T, or Q (1 or 2 chars, uppercase).
    * Get back a valid element symbol.
    *
    * It now takes in the residue name in order to check whether the atom is a hydrogen
    * or one of several heavy atoms (He, Hf, Hg, Ho, Hs).
    * These residues are hard-coded in, so they have to be updated when new residues
    * with these heavy atoms are created.
    * If the residue isn't found, it defaults to saying it is a hydrogen.
    *
    * Returns null for things we don't recognize at all.
    */
    static String getElement(String name, String resName)
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
        // tests to see if atom is one of several atoms that can be ambiguous.
        if (name.equals("HE")||name.equals("HF")||name.equals("HG")||name.equals("HO")||name.equals("HS")) {
          if (ambigAtomResidues == null) {
            ambigAtomResidues = new ArrayList();
            for (int i = 0; i < ambiguous_resnames.length; i++)
              ambigAtomResidues.add(ambiguous_resnames[i]);
          }
          if (ambigAtomResidues.contains(resName)) {
            return (String) elementNames.get(name);
          } else {
            return "H";
          }
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

