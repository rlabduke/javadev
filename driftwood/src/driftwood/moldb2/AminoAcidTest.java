package driftwood.moldb2;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

public class AminoAcidTest
{
    static final double EPS = 0.5; // half-degree tolerance for ideal structures
    Model helixModel;
    ModelState helixState;

    @Before public void setUp() throws IOException
    {
        InputStream is = getClass().getResourceAsStream("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        assertNotNull("Alpha helix PDB not found", is);
        CoordinateFile cf = new PdbReader().read(is);
        helixModel = cf.getFirstModel();
        helixState = helixModel.getState();
    }

    // --- translate ---

    @Test public void translateThreeToOne()
    {
        assertEquals("G", AminoAcid.translate("GLY"));
        assertEquals("A", AminoAcid.translate("ALA"));
        assertEquals("V", AminoAcid.translate("VAL"));
        assertEquals("L", AminoAcid.translate("LEU"));
        assertEquals("I", AminoAcid.translate("ILE"));
        assertEquals("P", AminoAcid.translate("PRO"));
        assertEquals("F", AminoAcid.translate("PHE"));
        assertEquals("Y", AminoAcid.translate("TYR"));
        assertEquals("W", AminoAcid.translate("TRP"));
        assertEquals("S", AminoAcid.translate("SER"));
        assertEquals("T", AminoAcid.translate("THR"));
        assertEquals("C", AminoAcid.translate("CYS"));
        assertEquals("M", AminoAcid.translate("MET"));
        assertEquals("K", AminoAcid.translate("LYS"));
        assertEquals("H", AminoAcid.translate("HIS"));
        assertEquals("R", AminoAcid.translate("ARG"));
        assertEquals("D", AminoAcid.translate("ASP"));
        assertEquals("N", AminoAcid.translate("ASN"));
        assertEquals("Q", AminoAcid.translate("GLN"));
        assertEquals("E", AminoAcid.translate("GLU"));
    }

    @Test public void translateOneToThree()
    {
        assertEquals("gly", AminoAcid.translate("G"));
        assertEquals("ala", AminoAcid.translate("A"));
        assertEquals("val", AminoAcid.translate("V"));
        assertEquals("leu", AminoAcid.translate("L"));
        assertEquals("ile", AminoAcid.translate("I"));
        assertEquals("pro", AminoAcid.translate("P"));
    }

    @Test public void translateLowercase()
    {
        assertEquals("G", AminoAcid.translate("gly"));
        assertEquals("A", AminoAcid.translate("ala"));
    }

    @Test public void translateUnknownThreeLetter()
    { assertEquals("X", AminoAcid.translate("XYZ")); }

    @Test public void translateUnknownOneLetter()
    { assertEquals("unk", AminoAcid.translate("Z")); }

    // --- getTau (N-CA-C angle) ---

    @Test public void getTauIdealHelix() throws Exception
    {
        // Ideal alanine tau should be close to 111.2 degrees
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        Residue res = residues.get(5); // middle residue
        double tau = AminoAcid.getTau(res, helixState);
        assertEquals(111.2, tau, 2.0); // within 2 degrees of ideal
    }

    @Test public void getTauDeviation() throws Exception
    {
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        Residue res = residues.get(5);
        double taudev = AminoAcid.getTauDeviation(res, helixState);
        // Ideal structure should have small deviation
        assertTrue(Math.abs(taudev) < 2.0);
    }

    // --- getPhi / getPsi ---

    @Test public void getPhiIdealHelix() throws Exception
    {
        // Ideal alpha helix phi is about -57 degrees
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        Residue res = residues.get(5); // middle residue (needs prev for phi)
        double phi = AminoAcid.getPhi(helixModel, res, helixState);
        assertEquals(-57.0, phi, 5.0); // within 5 degrees
    }

    @Test public void getPsiIdealHelix() throws Exception
    {
        // Ideal alpha helix psi is typically -40 to -50 degrees
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        Residue res = residues.get(5); // middle residue (needs next for psi)
        double psi = AminoAcid.getPsi(helixModel, res, helixState);
        assertTrue("psi should be in helix region: " + psi, psi > -55 && psi < -35);
    }

    @Test(expected = ResidueException.class)
    public void getPhiFirstResidueThrows() throws Exception
    {
        // First residue has no predecessor => can't compute phi
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        AminoAcid.getPhi(helixModel, residues.get(0), helixState);
    }

    @Test(expected = ResidueException.class)
    public void getPsiLastResidueThrows() throws Exception
    {
        // Last residue has no successor => can't compute psi
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        Residue last = residues.get(residues.size() - 1);
        AminoAcid.getPsi(helixModel, last, helixState);
    }

    @Test public void phiPsiConsistentAlongHelix() throws Exception
    {
        // All interior residues should have similar phi/psi
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        double prevPhi = Double.NaN, prevPsi = Double.NaN;
        for (int i = 2; i < residues.size() - 2; i++)
        {
            Residue res = residues.get(i);
            double phi = AminoAcid.getPhi(helixModel, res, helixState);
            double psi = AminoAcid.getPsi(helixModel, res, helixState);
            if (!Double.isNaN(prevPhi))
            {
                assertEquals("phi should be consistent at residue " + (i+1),
                    prevPhi, phi, 2.0);
                assertEquals("psi should be consistent at residue " + (i+1),
                    prevPsi, psi, 2.0);
            }
            prevPhi = phi;
            prevPsi = psi;
        }
    }

    // --- isPrepro ---

    @Test public void isPreproFalseForPolyAla() throws Exception
    {
        // No prolines in polyalanine
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        assertFalse(AminoAcid.isPrepro(helixModel, residues.get(5), helixState));
    }

    // --- isCisPeptide ---

    @Test public void isCisPeptideFalseForIdealHelix() throws Exception
    {
        // Ideal alpha helix should have all trans peptides
        List<Residue> residues = new ArrayList<Residue>(helixModel.getResidues());
        for (int i = 1; i < residues.size() - 1; i++)
            assertFalse("Residue " + (i+1) + " should be trans",
                AminoAcid.isCisPeptide(helixModel, residues.get(i), helixState));
    }
}
