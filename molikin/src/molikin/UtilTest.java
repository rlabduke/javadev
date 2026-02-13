package molikin;

import org.junit.Test;
import static org.junit.Assert.*;
import driftwood.moldb2.*;

/**
 * Tests for molikin Util static utility methods: residue/atom classification,
 * element colors, VDW radii, and numeric checking.
 */
public class UtilTest
{
    static final double EPS = 1e-3;

    // --- Helper to create a residue with atoms ---

    private Residue makeResidue(String name, boolean het, String... atomNames) throws AtomException
    {
        Residue res = new Residue(" A", "", "   1", " ", name);
        for (String an : atomNames)
            res.add(new Atom(an, "XX", het));
        return res;
    }

    // --- isProtein ---

    @Test public void isProteinStandardAminoAcids() throws Exception
    {
        String[] aas = {"GLY", "ALA", "VAL", "PHE", "PRO", "MET", "ILE", "LEU",
                         "ASP", "GLU", "LYS", "ARG", "SER", "THR", "TYR", "HIS",
                         "CYS", "ASN", "GLN", "TRP"};
        for (String aa : aas)
            assertTrue(aa + " should be protein", Util.isProtein(makeResidue(aa, false, " CA ")));
    }

    @Test public void isProteinMSE() throws Exception
    {
        assertTrue(Util.isProtein(makeResidue("MSE", false, " CA ")));
    }

    @Test public void isProteinFalseForWater() throws Exception
    {
        assertFalse(Util.isProtein(makeResidue("HOH", false, " O  ")));
    }

    @Test public void isProteinByCAAtom() throws Exception
    {
        // Even unknown residue name should be protein if it has a CA
        assertTrue(Util.isProtein(makeResidue("XYZ", false, " CA ")));
    }

    @Test public void isProteinFalseForLigand() throws Exception
    {
        assertFalse(Util.isProtein(makeResidue("ATP", false, " N1 ")));
    }

    // --- isNucleicAcid ---

    @Test public void isNucleicAcidStandardBases() throws Exception
    {
        assertTrue(Util.isNucleicAcid(makeResidue("  A", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("  C", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("  G", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("  T", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("  U", false)));
    }

    @Test public void isNucleicAcidDNA() throws Exception
    {
        assertTrue(Util.isNucleicAcid(makeResidue(" DA", false)));
        assertTrue(Util.isNucleicAcid(makeResidue(" DC", false)));
        assertTrue(Util.isNucleicAcid(makeResidue(" DG", false)));
        assertTrue(Util.isNucleicAcid(makeResidue(" DT", false)));
    }

    @Test public void isNucleicAcidThreeLetter() throws Exception
    {
        assertTrue(Util.isNucleicAcid(makeResidue("ADE", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("CYT", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("GUA", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("THY", false)));
        assertTrue(Util.isNucleicAcid(makeResidue("URA", false)));
    }

    @Test public void isNucleicAcidFalseForProtein() throws Exception
    {
        assertFalse(Util.isNucleicAcid(makeResidue("ALA", false)));
    }

    // --- isWater ---

    @Test public void isWaterVariants() throws Exception
    {
        assertTrue(Util.isWater(makeResidue("HOH", false, " O  ")));
        assertTrue(Util.isWater(makeResidue("DOD", false, " O  ")));
        assertTrue(Util.isWater(makeResidue("WAT", false, " O  ")));
        assertTrue(Util.isWater(makeResidue("SOL", false, " O  ")));
        assertTrue(Util.isWater(makeResidue("TIP", false, " O  ")));
        assertTrue(Util.isWater(makeResidue("H20", false, " O  ")));
    }

    @Test public void isWaterFalseForProtein() throws Exception
    {
        assertFalse(Util.isWater(makeResidue("ALA", false)));
    }

    // --- isMetal ---

    @Test public void isMetalCommonMetals() throws Exception
    {
        assertTrue(Util.isMetal(makeResidue(" ZN", false)));
        assertTrue(Util.isMetal(makeResidue(" FE", false)));
        assertTrue(Util.isMetal(makeResidue(" MG", false)));
        assertTrue(Util.isMetal(makeResidue(" CA", false)));
        assertTrue(Util.isMetal(makeResidue("NA ", false)));
    }

    @Test public void isMetalFalseForProtein() throws Exception
    {
        assertFalse(Util.isMetal(makeResidue("ALA", false)));
    }

    // --- isMainchain ---

    @Test public void isMainchainBackboneAtoms() throws Exception
    {
        Residue res = makeResidue("ALA", false, " N  ", " CA ", " C  ", " O  ");
        for (Object o : res.getAtoms())
        {
            Atom a = (Atom) o;
            AtomState as = new AtomState(a, "1");
            assertTrue(a.getName() + " should be mainchain", Util.isMainchain(as));
        }
    }

    @Test public void isMainchainFalseForSidechain() throws Exception
    {
        Residue res = makeResidue("ALA", false, " CB ");
        Atom cb = res.getAtom(" CB ");
        AtomState as = new AtomState(cb, "1");
        assertFalse(Util.isMainchain(as));
    }

    // --- isNumeric ---

    @Test public void isNumericIntegers()
    {
        assertTrue(Util.isNumeric("42"));
        assertTrue(Util.isNumeric("-7"));
        assertTrue(Util.isNumeric("0"));
    }

    @Test public void isNumericDecimals()
    {
        assertTrue(Util.isNumeric("3.14"));
        assertTrue(Util.isNumeric("-0.5"));
        assertTrue(Util.isNumeric("1e10"));
        assertTrue(Util.isNumeric("2.5E-3"));
    }

    @Test public void isNumericFalse()
    {
        assertFalse(Util.isNumeric("abc"));
        assertFalse(Util.isNumeric(""));
        assertFalse(Util.isNumeric(null));
    }

    // --- getElementColor ---

    @Test public void getElementColorKnown()
    {
        assertEquals("white", Util.getElementColor("C"));
        assertEquals("sky", Util.getElementColor("N"));
        assertEquals("red", Util.getElementColor("O"));
        assertEquals("yellow", Util.getElementColor("S"));
        assertEquals("gray", Util.getElementColor("H"));
        assertEquals("gold", Util.getElementColor("P"));
    }

    @Test public void getElementColorUnknown()
    {
        assertEquals("hotpink", Util.getElementColor("XY"));
    }

    // --- getVdwRadius ---

    @Test public void getVdwRadiusCommon()
    {
        assertEquals(1.75, Util.getVdwRadius("C"), EPS);
        assertEquals(1.55, Util.getVdwRadius("N"), EPS);
        assertEquals(1.40, Util.getVdwRadius("O"), EPS);
        assertEquals(1.17, Util.getVdwRadius("H"), EPS);
        assertEquals(1.80, Util.getVdwRadius("S"), EPS);
        assertEquals(1.80, Util.getVdwRadius("P"), EPS);
    }

    @Test public void getVdwRadiusUnknown()
    {
        assertEquals(2.0, Util.getVdwRadius("XY"), EPS);
    }

    // --- altsAreCompatible ---

    @Test public void altsAreCompatibleSameAlt() throws Exception
    {
        Atom a1 = new Atom(" CA ");
        Atom a2 = new Atom(" CB ");
        AtomState as1 = new AtomState(a1, "1");
        AtomState as2 = new AtomState(a2, "2");
        as1.setAltConf("A");
        as2.setAltConf("A");
        assertTrue(Util.altsAreCompatible(as1, as2));
    }

    @Test public void altsAreCompatibleOneBlank() throws Exception
    {
        Atom a1 = new Atom(" CA ");
        Atom a2 = new Atom(" CB ");
        AtomState as1 = new AtomState(a1, "1");
        AtomState as2 = new AtomState(a2, "2");
        as1.setAltConf("A");
        // as2 default is " "
        assertTrue(Util.altsAreCompatible(as1, as2));
        assertTrue(Util.altsAreCompatible(as2, as1));
    }

    @Test public void altsNotCompatible() throws Exception
    {
        Atom a1 = new Atom(" CA ");
        Atom a2 = new Atom(" CB ");
        AtomState as1 = new AtomState(a1, "1");
        AtomState as2 = new AtomState(a2, "2");
        as1.setAltConf("A");
        as2.setAltConf("B");
        assertFalse(Util.altsAreCompatible(as1, as2));
    }
}
