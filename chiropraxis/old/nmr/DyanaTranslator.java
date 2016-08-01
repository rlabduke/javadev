// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.nmr;

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
* <code>DyanaTranslator</code> is a utility class to assist
* in the translation of atom names from DYANA format to PDB
* format.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jul 10 15:45:31 EDT 2003
*/
public class DyanaTranslator //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    static DyanaTranslator      instance        = null;
    Map                         resLookup;
//}}}

//{{{ Constructor(s)
//##############################################################################
    private DyanaTranslator()
    {
        super();
        String[] lookupALA = {
            "HN", " H  ",
            "HA", " HA ",
            "HB1", "1HB ",
            "HB2", "2HB ",
            "HB3", "3HB ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "N", " N  ",
            "O", " O  "
        };
        String[] lookupARG = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG2", "1HG ",
            "HG3", "2HG ",
            "HD2", "1HD ",
            "HD3", "2HD ",
            "HE", " HE ",
            "HH11", "1HH1",
            "HH12", "2HH1",
            "HH21", "1HH2",
            "HH22", "2HH2",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD", " CD ",
            "CZ", " CZ ",
            "N", " N  ",
            "NE", " NE ",
            "NH1", " NH1",
            "NH2", " NH2",
            "O", " O  "
        };
        String[] lookupASP = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HD2", "    ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "N", " N  ",
            "O", " O  ",
            "OD1", " OD1",
            "OD2", " OD2"
        };
        String[] lookupASN = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HD21", "2HD2",
            "HD22", "1HD2",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "N", " N  ",
            "ND2", " ND2",
            "O", " O  ",
            "OD1", " OD1"
        };
        String[] lookupCYS = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG", " HG ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "N", " N  ",
            "O", " O  ",
            "SG", " SG "
        };
        String[] lookupGLU = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG2", "1HG ",
            "HG3", "2HG ",
            "HE2", "    ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD", " CD ",
            "N", " N  ",
            "O", " O  ",
            "OE1", " OE1",
            "OE2", " OE2"
        };
        String[] lookupGLN = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG2", "1HG ",
            "HG3", "2HG ",
            "HE21", "2HE2",
            "HE22", "1HE2",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD", " CD ",
            "N", " N  ",
            "NE2", " NE2",
            "O", " O  ",
            "OE1", " OE1"
        };
        String[] lookupGLY = {
            "HN", " H  ",
            "HA1", "1HA ",
            "HA2", "2HA ",
            "C", " C  ",
            "CA", " CA ",
            "N", " N  ",
            "O", " O  "
        };
        String[] lookupHIS = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HD1", " HD1",
            "HD2", " HD2",
            "HE1", " HE1",
            "HE2", "    ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD2", " CD2",
            "CE1", " CE1",
            "N", " N  ",
            "ND1", " ND1",
            "NE2", " NE2",
            "O", " O  "
        };
        String[] lookupILE = {
            "HN", " H  ",
            "HA", " HA ",
            "HB", " HB ",
            "HG12", "1HG1",
            "HG13", "2HG1",
            "HG21", "1HG2",
            "HG22", "2HG2",
            "HG23", "3HG2",
            "HD11", "1HD1",
            "HD12", "2HD1",
            "HD13", "3HD1",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG1", " CG1",
            "CG2", " CG2",
            "CD1", " CD1",
            "N", " N  ",
            "O", " O  "
        };
        String[] lookupLEU = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG", " HG ",
            "HD11", "1HD1",
            "HD12", "2HD1",
            "HD13", "3HD1",
            "HD21", "1HD2",
            "HD22", "2HD2",
            "HD23", "3HD2",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD1", " CD1",
            "CD2", " CD2",
            "N", " N  ",
            "O", " O  "
        };
        String[] lookupLYS = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG2", "1HG ",
            "HG3", "2HG ",
            "HD2", "1HD ",
            "HD3", "2HD ",
            "HE2", "1HE ",
            "HE3", "2HE ",
            "HZ1", "1HZ ",
            "HZ2", "2HZ ",
            "HZ3", "3HZ ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD", " CD ",
            "CE", " CE ",
            "N", " N  ",
            "NZ", " NZ ",
            "O", " O  "
        };
        String[] lookupMET = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG2", "1HG ",
            "HG3", "2HG ",
            "HE1", "1HE ",
            "HE2", "2HE ",
            "HE3", "3HE ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CE", " CE ",
            "N", " N  ",
            "O", " O  ",
            "SD", " SD "
        };
        String[] lookupPHE = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HD1", " HD1",
            "HD2", " HD2",
            "HE1", " HE1",
            "HE2", " HE2",
            "HZ", " HZ ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD1", " CD1",
            "CD2", " CD2",
            "CE1", " CE1",
            "CE2", " CE2",
            "CZ", " CZ ",
            "N", " N  ",
            "O", " O  "
        };
        String[] lookupPRO = {
            "", " H2 ",
            "", " H1 ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG2", "1HG ",
            "HG3", "2HG ",
            "HD2", "1HD ",
            "HD3", "2HD ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD", " CD ",
            "N", " N  ",
            "O", " O  "
        };
        String[] lookupSER = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HG", " HG ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "N", " N  ",
            "O", " O  ",
            "OG", " OG "
        };
        String[] lookupTHR = {
            "HN", " H  ",
            "HA", " HA ",
            "HB", " HB ",
            "HG1", " HG1",
            "HG21", "1HG2",
            "HG22", "2HG2",
            "HG23", "3HG2",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG2", " CG2",
            "N", " N  ",
            "O", " O  ",
            "OG1", " OG1"
        };
        String[] lookupTRP = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HD1", " HD1",
            "HE1", " HE1",
            "HE3", " HE3",
            "HZ2", " HZ2",
            "HZ3", " HZ3",
            "HH2", " HH2",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD1", " CD1",
            "CD2", " CD2",
            "CE2", " CE2",
            "CE3", " CE3",
            "CZ2", " CZ2",
            "CZ3", " CZ3",
            "CH2", " CH2",
            "N", " N  ",
            "NE1", " NE1",
            "O", " O  "
        };
        String[] lookupTYR = {
            "HN", " H  ",
            "HA", " HA ",
            "HB2", "1HB ",
            "HB3", "2HB ",
            "HD1", " HD1",
            "HD2", " HD2",
            "HE1", " HE1",
            "HE2", " HE2",
            "HH", " HH ",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG", " CG ",
            "CD1", " CD1",
            "CD2", " CD2",
            "CE1", " CE1",
            "CE2", " CE2",
            "CZ", " CZ ",
            "N", " N  ",
            "O", " O  ",
            "OH", " OH "
        };
        String[] lookupVAL = {
            "HN", " H  ",
            "HA", " HA ",
            "HB", " HB ",
            "HG11", "1HG1",
            "HG12", "2HG1",
            "HG13", "3HG1",
            "HG21", "1HG2",
            "HG22", "2HG2",
            "HG23", "3HG2",
            "C", " C  ",
            "CA", " CA ",
            "CB", " CB ",
            "CG1", " CG1",
            "CG2", " CG2",
            "N", " N  ",
            "O", " O  "
        };
        
        resLookup = new HashMap();
        addResidue("GLY", lookupGLY);
        addResidue("ALA", lookupALA);
        addResidue("VAL", lookupVAL);
        addResidue("LEU", lookupLEU);
        addResidue("ILE", lookupILE);
        addResidue("PRO", lookupPRO);
        addResidue("PHE", lookupPHE);
        addResidue("TYR", lookupTYR);
        addResidue("TRP", lookupTRP);
        addResidue("SER", lookupSER);
        addResidue("THR", lookupTHR);
        addResidue("CYS", lookupCYS);
        addResidue("MET", lookupMET);
        addResidue("LYS", lookupLYS);
        addResidue("HIS", lookupHIS);
        addResidue("ARG", lookupARG);
        addResidue("ASP", lookupASP);
        addResidue("ASN", lookupASN);
        addResidue("GLN", lookupGLN);
        addResidue("GLU", lookupGLU);
    }
//}}}

//{{{ addResidue, getInstance
//##############################################################################
    private void addResidue(String resName, String[] pairs)
    {
        Map atomLookup = new HashMap();
        for(int i = 0; i < pairs.length; i += 2)
        {
            atomLookup.put(pairs[i], pairs[i+1]);
        }
        resLookup.put(resName, atomLookup);
    }
    
    /** Returns a shared instance of this singleton class. */
    static public DyanaTranslator getInstance()
    {
        if(instance == null) instance = new DyanaTranslator();
        return instance;
    }
//}}}

//{{{ translate
//##############################################################################
    /**
    * Translates atom names from the format used by the NMR program
    * DYANA to the name format used by PDB files.
    * @param resName    the 3 character residue type (uppercase)
    * @param atomName   the <i>N</i> character atom name, stripped of whitespace (uppercase)
    * @return           the corresponding PDB name, always 4 characters long;
    *   or null if the name could not be translated.
    */
    public String translate(String resName, String atomName)
    {
        Map atomLookup = (Map)resLookup.get(resName);
        if(atomLookup == null) return null;
        else return (String)atomLookup.get(atomName);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

