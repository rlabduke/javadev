package driftwood.moldb2;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

public class PdbReaderTest
{
    static final double EPS = 1e-3;
    PdbReader reader;

    @Before public void setUp()
    { reader = new PdbReader(); }

    // --- Helper to load a resource PDB ---

    private CoordinateFile readResource(String path) throws IOException
    {
        InputStream is = getClass().getResourceAsStream(path);
        assertNotNull("Resource not found: " + path, is);
        return reader.read(is);
    }

    // --- Read ideal alpha helix ---

    @Test public void readAlphaHelixHasOneModel() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        assertEquals(1, cf.getModels().size());
    }

    @Test public void readAlphaHelixResidueCount() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        Model model = cf.getFirstModel();
        // 12 ALA residues + partial 13th (just N, CA, H)
        Collection residues = model.getResidues();
        assertEquals(13, residues.size());
    }

    @Test public void readAlphaHelixAllAlanine() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        Model model = cf.getFirstModel();
        for (Object o : model.getResidues())
        {
            Residue res = (Residue) o;
            assertEquals("ALA", res.getName());
        }
    }

    @Test public void readAlphaHelixAtomCount() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        Model model = cf.getFirstModel();
        ModelState state = model.getState();
        // Each full ALA has 10 atoms, residue 13 has 3 (N, CA, H)
        // Total: 12*10 + 3 = 123
        int count = 0;
        for (Object o : model.getResidues())
            count += ((Residue) o).getAtoms().size();
        assertEquals(123, count);
    }

    @Test public void readAlphaHelixCoordinates() throws Exception
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        Model model = cf.getFirstModel();
        ModelState state = model.getState();
        // Residue 1 CA should be at (0, 0, 0)
        Residue res1 = (Residue) model.getResidues().iterator().next();
        AtomState ca = state.get(res1.getAtom(" CA "));
        assertEquals(0.0, ca.getX(), EPS);
        assertEquals(0.0, ca.getY(), EPS);
        assertEquals(0.0, ca.getZ(), EPS);
    }

    @Test public void readAlphaHelixHasState() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        Model model = cf.getFirstModel();
        assertNotNull(model.getState());
        assertFalse(model.getStates().isEmpty());
    }

    // --- Read singleres.pdb (all amino acid types) ---

    @Test public void readSingleResMultipleChains() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/sc/singleres.pdb");
        Model model = cf.getFirstModel();
        Set chains = model.getChainIDs();
        // Each amino acid uses a different chain ID (single letter code)
        assertTrue(chains.size() >= 18); // at least 18 standard amino acids
    }

    @Test public void readSingleResAlaChain() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/sc/singleres.pdb");
        Model model = cf.getFirstModel();
        ModelState state = model.getState();
        // ALA is chain A
        Set alaChain = model.getChain(" A");
        assertNotNull(alaChain);
        assertEquals(1, alaChain.size());
        Residue ala = (Residue) alaChain.iterator().next();
        assertEquals("ALA", ala.getName());
    }

    @Test public void readSingleResArgHasAllAtoms() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/sc/singleres.pdb");
        Model model = cf.getFirstModel();
        // ARG is chain R
        Set argChain = model.getChain(" R");
        assertNotNull(argChain);
        Residue arg = (Residue) argChain.iterator().next();
        assertEquals("ARG", arg.getName());
        // ARG has: N, CA, C, O, CB, CG, CD, NE, CZ, NH1, NH2, H, HA,
        // HB2, HB3, HG2, HG3, HD2, HD3, HE, HH11, HH12, HH21, HH22 = 24 atoms
        assertEquals(24, arg.getAtoms().size());
    }

    @Test public void readSingleResBFactor() throws Exception
    {
        CoordinateFile cf = readResource("/chiropraxis/sc/singleres.pdb");
        Model model = cf.getFirstModel();
        ModelState state = model.getState();
        Set alaChain = model.getChain(" A");
        Residue ala = (Residue) alaChain.iterator().next();
        AtomState ca = state.get(ala.getAtom(" CA "));
        assertEquals(30.0, ca.getTempFactor(), EPS);
    }

    @Test public void readSingleResOccupancy() throws Exception
    {
        CoordinateFile cf = readResource("/chiropraxis/sc/singleres.pdb");
        Model model = cf.getFirstModel();
        ModelState state = model.getState();
        Set alaChain = model.getChain(" A");
        Residue ala = (Residue) alaChain.iterator().next();
        AtomState ca = state.get(ala.getAtom(" CA "));
        assertEquals(1.0, ca.getOccupancy(), EPS);
    }

    // --- Read from String (inline PDB) ---

    @Test public void readMinimalPdb() throws Exception
    {
        String pdb =
            "ATOM      1  N   ALA A   1       1.000   2.000   3.000  1.00 10.00\n" +
            "ATOM      2  CA  ALA A   1       2.000   3.000   4.000  1.00 10.00\n" +
            "ATOM      3  C   ALA A   1       3.000   4.000   5.000  1.00 10.00\n" +
            "ATOM      4  O   ALA A   1       4.000   5.000   6.000  1.00 10.00\n";
        CoordinateFile cf = reader.read(new StringReader(pdb));
        Model model = cf.getFirstModel();
        assertEquals(1, model.getResidues().size());
        Residue res = (Residue) model.getResidues().iterator().next();
        assertEquals("ALA", res.getName());
        assertEquals(" A", res.getChain());

        ModelState state = model.getState();
        AtomState n = state.get(res.getAtom(" N  "));
        assertEquals(1.0, n.getX(), EPS);
        assertEquals(2.0, n.getY(), EPS);
        assertEquals(3.0, n.getZ(), EPS);
    }

    @Test public void readMultipleResidues() throws IOException
    {
        String pdb =
            "ATOM      1  N   ALA A   1       1.000   0.000   0.000  1.00  0.00\n" +
            "ATOM      2  CA  ALA A   1       2.000   0.000   0.000  1.00  0.00\n" +
            "ATOM      3  C   ALA A   1       3.000   0.000   0.000  1.00  0.00\n" +
            "ATOM      4  O   ALA A   1       3.500   1.000   0.000  1.00  0.00\n" +
            "ATOM      5  N   GLY A   2       4.000   0.000   0.000  1.00  0.00\n" +
            "ATOM      6  CA  GLY A   2       5.000   0.000   0.000  1.00  0.00\n" +
            "ATOM      7  C   GLY A   2       6.000   0.000   0.000  1.00  0.00\n" +
            "ATOM      8  O   GLY A   2       6.500   1.000   0.000  1.00  0.00\n";
        CoordinateFile cf = reader.read(new StringReader(pdb));
        Model model = cf.getFirstModel();
        assertEquals(2, model.getResidues().size());
    }

    @Test public void readHetatm() throws IOException
    {
        String pdb =
            "HETATM    1  O   HOH A 101       5.000   5.000   5.000  1.00 20.00\n";
        CoordinateFile cf = reader.read(new StringReader(pdb));
        Model model = cf.getFirstModel();
        Residue res = (Residue) model.getResidues().iterator().next();
        assertEquals("HOH", res.getName());
        Atom o = res.getAtom(" O  ");
        assertTrue(o.isHet());
    }

    @Test public void readMultipleModels() throws IOException
    {
        String pdb =
            "MODEL        1\n" +
            "ATOM      1  CA  ALA A   1       1.000   0.000   0.000  1.00  0.00\n" +
            "ENDMDL\n" +
            "MODEL        2\n" +
            "ATOM      1  CA  ALA A   1       2.000   0.000   0.000  1.00  0.00\n" +
            "ENDMDL\n";
        CoordinateFile cf = reader.read(new StringReader(pdb));
        assertEquals(2, cf.getModels().size());
    }

    // --- Residue navigation ---

    @Test public void residueGetNextAndPrev() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        Model model = cf.getFirstModel();
        List<Residue> residues = new ArrayList<Residue>(model.getResidues());
        // Residues should be navigable
        Residue first = residues.get(0);
        Residue second = first.getNext(model);
        assertNotNull(second);
        Residue backToFirst = second.getPrev(model);
        assertNotNull(backToFirst);
        assertSame(first, backToFirst);
    }

    @Test public void residueSequenceNumbers() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        Model model = cf.getFirstModel();
        List<Residue> residues = new ArrayList<Residue>(model.getResidues());
        // First residue should be number 1
        assertEquals(1, residues.get(0).getSequenceInteger());
    }

    // --- Read beta strand ---

    @Test public void readBetaStrand() throws IOException
    {
        CoordinateFile cf = readResource("/chiropraxis/mc/idealpolyala12-beta.pdb");
        Model model = cf.getFirstModel();
        assertNotNull(model);
        assertTrue(model.getResidues().size() > 0);
    }
}
