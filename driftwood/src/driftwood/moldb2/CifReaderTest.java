package driftwood.moldb2;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

/**
 * Tests for CifReader using 1a7y-extended.cif, which contains
 * extended 5-character CCD codes (X2AVD) as a "future-forward" feature.
 *
 * Structure: Actinomycin D crystal (1A7Y) with 3 copies in the ASU.
 * Chains A, B, C each have 11 residues: THR, X2AVD, PRO, SAR, MVA, PXZ,
 * THR, X2AVD, PRO, SAR, MVA. Plus EEE and MOH ligands.
 */
public class CifReaderTest
{
    static final double EPS = 1e-3;
    CifReader reader;
    CoordinateFile cf;

    @Before public void setUp() throws IOException
    {
        reader = new CifReader();
        InputStream is = getClass().getResourceAsStream("/driftwood/moldb2/1a7y-extended.cif");
        assertNotNull("CIF resource not found", is);
        cf = reader.read(is);
        is.close();
    }

    // --- Basic parsing ---

    @Test public void idCode()
    { assertEquals("1A7Y", cf.getIdCode()); }

    @Test public void singleModel()
    { assertEquals(1, cf.getModels().size()); }

    @Test public void totalAtomCount()
    {
        Model m = cf.getFirstModel();
        int count = 0;
        for (Iterator it = m.getResidues().iterator(); it.hasNext(); )
        {
            Residue r = (Residue) it.next();
            count += r.getAtoms().size();
        }
        assertEquals(314, count);
    }

    // --- Chains ---

    @Test public void hasThreeChains()
    {
        Model m = cf.getFirstModel();
        Set chains = new HashSet();
        for (Iterator it = m.getResidues().iterator(); it.hasNext(); )
        {
            Residue r = (Residue) it.next();
            chains.add(r.getChain());
        }
        assertTrue("Expected chain A", chains.contains("A"));
        assertTrue("Expected chain B", chains.contains("B"));
        assertTrue("Expected chain C", chains.contains("C"));
        assertEquals(3, chains.size());
    }

    // --- First residue: THR A 1 ---

    @Test public void firstResidueName()
    {
        Residue first = findResidue("A", "1", "THR");
        assertNotNull("THR A 1 not found", first);
        assertEquals("THR", first.getName());
    }

    @Test public void firstResidueAtomCount()
    {
        Residue thr = findResidue("A", "1", "THR");
        assertNotNull(thr);
        assertEquals(7, thr.getAtoms().size()); // N, CA, C, O, CB, OG1, CG2
    }

    @Test public void firstAtomCoordinates() throws AtomException
    {
        Residue thr = findResidue("A", "1", "THR");
        assertNotNull(thr);
        Atom n = thr.getAtom(" N  ");
        assertNotNull("N atom not found in THR", n);
        ModelState ms = getDefaultState();
        AtomState as = ms.get(n);
        assertNotNull(as);
        assertEquals(11.239, as.getX(), EPS);
        assertEquals( 9.853, as.getY(), EPS);
        assertEquals(11.574, as.getZ(), EPS);
    }

    @Test public void firstAtomBfactor() throws AtomException
    {
        Residue thr = findResidue("A", "1", "THR");
        Atom n = thr.getAtom(" N  ");
        AtomState as = getDefaultState().get(n);
        assertEquals(2.58, as.getTempFactor(), EPS);
    }

    @Test public void firstAtomOccupancy() throws AtomException
    {
        Residue thr = findResidue("A", "1", "THR");
        Atom n = thr.getAtom(" N  ");
        AtomState as = getDefaultState().get(n);
        assertEquals(1.00, as.getOccupancy(), EPS);
    }

    // --- Extended CCD code: X2AVD (5-character residue name) ---

    @Test public void extendedCcdCodeStored()
    {
        Residue x2avd = findResidue("A", "2", "X2AVD");
        assertNotNull("X2AVD A 2 not found", x2avd);
        assertEquals("X2AVD", x2avd.getName());
    }

    @Test public void extendedCcdCodeAtomCount()
    {
        Residue x2avd = findResidue("A", "2", "X2AVD");
        assertNotNull(x2avd);
        // X2AVD has: N, CA, CB, CG1, CG2, C, O = 7 atoms
        assertEquals(7, x2avd.getAtoms().size());
    }

    @Test public void extendedCcdCodeIsHet()
    {
        Residue x2avd = findResidue("A", "2", "X2AVD");
        assertNotNull(x2avd);
        Atom n = x2avd.getAtom(" N  ");
        assertNotNull("N atom not found in X2AVD", n);
        assertTrue("X2AVD atoms should be HETATM", n.isHet());
    }

    @Test public void extendedCcdCodeInChainC()
    {
        Residue x2avd = findResidue("C", "2", "X2AVD");
        assertNotNull("X2AVD C 2 not found", x2avd);
        assertEquals("X2AVD", x2avd.getName());
    }

    @Test public void extendedCcdCodeInChainB()
    {
        Residue x2avd = findResidue("B", "2", "X2AVD");
        assertNotNull("X2AVD B 2 not found", x2avd);
    }

    // --- Non-standard residues ---

    @Test public void sarResidue()
    {
        Residue sar = findResidue("A", "4", "SAR");
        assertNotNull("SAR A 4 not found", sar);
        assertTrue(sar.getAtom(" CN ").isHet());
    }

    @Test public void mvaResidue()
    {
        Residue mva = findResidue("A", "5", "MVA");
        assertNotNull("MVA A 5 not found", mva);
    }

    @Test public void pxzChromophore()
    {
        Residue pxz = findResidue("A", "6", "PXZ");
        assertNotNull("PXZ A 6 not found", pxz);
        // PXZ has 22 atoms in this file
        assertEquals(22, pxz.getAtoms().size());
    }

    @Test public void pxzQuotedAtomNames()
    {
        // CIF file has "C'" and "O'" for PXZ - test they parse correctly
        Residue pxz = findResidue("A", "6", "PXZ");
        assertNotNull(pxz);
        // C' should become " C' " (4-char PDB-style name)
        boolean foundCprime = false;
        for (Iterator it = pxz.getAtoms().iterator(); it.hasNext(); )
        {
            Atom a = (Atom) it.next();
            if (a.getName().trim().equals("C'"))
            {
                foundCprime = true;
                break;
            }
        }
        assertTrue("C' atom not found in PXZ", foundCprime);
    }

    // --- Ligands ---

    @Test public void eeeResidueExists()
    {
        // EEE ligands assigned to chain A via auth_asym_id
        Residue eee = findResidue("A", "104", "EEE");
        assertNotNull("EEE A 104 not found", eee);
        assertEquals(6, eee.getAtoms().size()); // C1, C2, O1, O2, C3, C4
    }

    @Test public void mohResidueExists()
    {
        // MOH assigned to chain C via auth_asym_id
        Residue moh = findResidue("C", "108", "MOH");
        assertNotNull("MOH C 108 not found", moh);
        assertEquals(2, moh.getAtoms().size()); // C, O
    }

    // --- Chain B is complete ---

    @Test public void chainBResidueCount()
    {
        Model m = cf.getFirstModel();
        int count = 0;
        for (Iterator it = m.getResidues().iterator(); it.hasNext(); )
        {
            Residue r = (Residue) it.next();
            if ("B".equals(r.getChain())) count++;
        }
        // Chain B: 11 peptide residues only (no EEE/MOH)
        assertEquals(11, count);
    }

    // --- Coordinates of last atom ---

    @Test public void lastAtomCoordinates() throws AtomException
    {
        // MOH C 108, atom O: 18.932, 15.402, 4.944
        Residue moh = findResidue("C", "108", "MOH");
        assertNotNull(moh);
        Atom o = moh.getAtom(" O  ");
        assertNotNull("O atom not found in MOH", o);
        AtomState as = getDefaultState().get(o);
        assertNotNull(as);
        assertEquals(18.932, as.getX(), EPS);
        assertEquals(15.402, as.getY(), EPS);
        assertEquals( 4.944, as.getZ(), EPS);
    }

    // --- ATOM vs HETATM ---

    @Test public void thrIsNotHet()
    {
        Residue thr = findResidue("A", "1", "THR");
        Atom ca = thr.getAtom(" CA ");
        assertFalse("THR CA should not be HETATM", ca.isHet());
    }

    @Test public void proIsNotHet()
    {
        Residue pro = findResidue("A", "3", "PRO");
        assertNotNull(pro);
        Atom ca = pro.getAtom(" CA ");
        assertFalse("PRO CA should not be HETATM", ca.isHet());
    }

    // --- Helper methods ---

    private Residue findResidue(String chain, String seqNum, String name)
    {
        Model m = cf.getFirstModel();
        for (Iterator it = m.getResidues().iterator(); it.hasNext(); )
        {
            Residue r = (Residue) it.next();
            if (r.getChain().equals(chain)
                && r.getSequenceNumber().equals(seqNum)
                && r.getName().equals(name))
                return r;
        }
        return null;
    }

    private ModelState getDefaultState()
    {
        Model m = cf.getFirstModel();
        return (ModelState) m.getStates().values().iterator().next();
    }
}
