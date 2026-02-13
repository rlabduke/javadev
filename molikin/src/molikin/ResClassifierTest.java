package molikin;

import org.junit.Test;
import static org.junit.Assert.*;
import driftwood.moldb2.*;
import java.util.*;

/**
 * Tests for ResClassifier: categorizing residues as protein, nucleic acid,
 * water, metal, other het, or unknown.
 */
public class ResClassifierTest
{
    // --- Helpers ---

    private Residue makeResidue(String chain, String seq, String name, boolean het, String... atomNames) throws AtomException
    {
        Residue res = new Residue(chain, "", seq, " ", name);
        for (String an : atomNames)
            res.add(new Atom(an, "XX", het));
        return res;
    }

    // --- Single classification ---

    @Test public void classifyProtein() throws Exception
    {
        Residue ala = makeResidue(" A", "   1", "ALA", false, " N  ", " CA ", " C  ", " O  ");
        ResClassifier rc = new ResClassifier(Collections.singletonList(ala));
        assertSame(ResClassifier.PROTEIN, rc.classify(ala));
        assertTrue(rc.proteinRes.contains(ala));
    }

    @Test public void classifyWater() throws Exception
    {
        Residue hoh = makeResidue(" A", " 100", "HOH", true, " O  ");
        ResClassifier rc = new ResClassifier(Collections.singletonList(hoh));
        assertSame(ResClassifier.WATER, rc.classify(hoh));
        assertTrue(rc.waterRes.contains(hoh));
    }

    @Test public void classifyMetal() throws Exception
    {
        Residue zn = makeResidue(" A", " 200", " ZN", true, "ZN  ");
        ResClassifier rc = new ResClassifier(Collections.singletonList(zn));
        assertSame(ResClassifier.METAL, rc.classify(zn));
        assertTrue(rc.metalRes.contains(zn));
    }

    @Test public void classifyMetalNeedsSingleAtom() throws Exception
    {
        // Metal name but multiple atoms => not classified as metal
        Residue zn = makeResidue(" A", " 200", " ZN", true, "ZN  ", " O  ");
        ResClassifier rc = new ResClassifier(Collections.singletonList(zn));
        assertNotSame(ResClassifier.METAL, rc.classify(zn));
    }

    @Test public void classifyNucleicAcid() throws Exception
    {
        Residue a = makeResidue(" A", "   1", "  A", false, " P  ", " C1'");
        ResClassifier rc = new ResClassifier(Collections.singletonList(a));
        assertSame(ResClassifier.NUCACID, rc.classify(a));
        assertTrue(rc.nucAcidRes.contains(a));
    }

    @Test public void classifyOtherHet() throws Exception
    {
        // Mostly HETATMs -> OHET
        Residue lig = makeResidue(" A", " 300", "LIG", true, " C1 ", " C2 ", " O1 ");
        ResClassifier rc = new ResClassifier(Collections.singletonList(lig));
        assertSame(ResClassifier.OHET, rc.classify(lig));
        assertTrue(rc.ohetRes.contains(lig));
    }

    @Test public void classifyUnknown() throws Exception
    {
        // Mostly ATOMs, not protein/nuc/water/metal -> UNKNOWN
        Residue unk = makeResidue(" A", " 400", "UNK", false, " X1 ", " X2 ");
        ResClassifier rc = new ResClassifier(Collections.singletonList(unk));
        assertSame(ResClassifier.UNKNOWN, rc.classify(unk));
        assertTrue(rc.unknownRes.contains(unk));
    }

    // --- Mixed chains ---

    @Test public void classifyMixedProteinAndWater() throws Exception
    {
        List residues = new ArrayList();
        residues.add(makeResidue(" A", "   1", "ALA", false, " N  ", " CA ", " C  ", " O  "));
        residues.add(makeResidue(" A", "   2", "GLY", false, " N  ", " CA ", " C  ", " O  "));
        residues.add(makeResidue(" A", " 100", "HOH", true, " O  "));
        ResClassifier rc = new ResClassifier(residues);
        assertSame(ResClassifier.PROTEIN, rc.classify((Residue) residues.get(0)));
        assertSame(ResClassifier.PROTEIN, rc.classify((Residue) residues.get(1)));
        assertSame(ResClassifier.WATER, rc.classify((Residue) residues.get(2)));
        assertEquals(2, rc.proteinRes.size());
        assertEquals(1, rc.waterRes.size());
    }

    @Test public void classifyUnknownBetweenProteinsGetsReclassified() throws Exception
    {
        // Unknown residue between two proteins in the same chain
        // should be reclassified as protein
        List residues = new ArrayList();
        Residue ala1 = makeResidue(" A", "   1", "ALA", false, " N  ", " CA ", " C  ", " O  ");
        Residue unk = makeResidue(" A", "   2", "XYZ", false, " X1 ", " X2 ");
        Residue ala2 = makeResidue(" A", "   3", "ALA", false, " N  ", " CA ", " C  ", " O  ");
        residues.add(ala1);
        residues.add(unk);
        residues.add(ala2);
        ResClassifier rc = new ResClassifier(residues);
        // unk preceded by protein, same chain => reclassified as protein
        assertSame(ResClassifier.PROTEIN, rc.classify(unk));
    }

    @Test public void classifyAllSetsPopulated() throws Exception
    {
        List residues = new ArrayList();
        residues.add(makeResidue(" A", "   1", "ALA", false, " N  ", " CA ", " C  ", " O  "));
        residues.add(makeResidue(" B", "   1", "  A", false, " P  "));
        residues.add(makeResidue(" C", " 100", "HOH", true, " O  "));
        residues.add(makeResidue(" D", " 200", " ZN", true, "ZN  "));
        residues.add(makeResidue(" E", " 300", "LIG", true, " C1 ", " C2 "));
        ResClassifier rc = new ResClassifier(residues);
        assertEquals(1, rc.proteinRes.size());
        assertEquals(1, rc.nucAcidRes.size());
        assertEquals(1, rc.waterRes.size());
        assertEquals(1, rc.metalRes.size());
        assertEquals(1, rc.ohetRes.size());
    }

    @Test public void classifyEmptyList()
    {
        ResClassifier rc = new ResClassifier(Collections.EMPTY_LIST);
        assertTrue(rc.proteinRes.isEmpty());
        assertTrue(rc.nucAcidRes.isEmpty());
        assertTrue(rc.waterRes.isEmpty());
        assertTrue(rc.metalRes.isEmpty());
        assertTrue(rc.ohetRes.isEmpty());
        assertTrue(rc.unknownRes.isEmpty());
    }
}
