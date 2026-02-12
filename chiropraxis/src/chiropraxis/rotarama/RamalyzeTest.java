package chiropraxis.rotarama;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import driftwood.moldb2.*;

public class RamalyzeTest
{
    Model helixModel, betaModel;

    @Before public void setUp() throws IOException
    {
        InputStream is = getClass().getResourceAsStream("/chiropraxis/mc/idealpolyala12-alpha.pdb");
        assertNotNull("Alpha helix PDB not found", is);
        helixModel = new PdbReader().read(is).getFirstModel();
        is.close();

        is = getClass().getResourceAsStream("/chiropraxis/mc/idealpolyala12-beta.pdb");
        assertNotNull("Beta strand PDB not found", is);
        betaModel = new PdbReader().read(is).getFirstModel();
        is.close();
    }

    // --- analyzeModel ---

    @Test public void analyzeHelixReturnsResults() throws IOException
    {
        Set results = Ramalyze.analyzeModel(helixModel, helixModel.getStates().values());
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    @Test public void analyzeHelixCountsInterior() throws IOException
    {
        // First and last residues can't get phi/psi, so fewer results than residues
        Set results = Ramalyze.analyzeModel(helixModel, helixModel.getStates().values());
        int numResidues = helixModel.getResidues().size();
        assertTrue(results.size() < numResidues);
        assertTrue(results.size() >= numResidues - 3); // at most 3 missing (first, last, partial)
    }

    @Test public void analyzeHelixAllFavored() throws IOException
    {
        // Ideal alpha helix should be all Favored in Ramachandran
        Set results = Ramalyze.analyzeModel(helixModel, helixModel.getStates().values());
        for (Object o : results)
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) o;
            assertEquals("Residue " + eval.res.getSequenceNumber() + " should be Favored",
                Ramalyze.RamaEval.FAVORED, eval.score);
        }
    }

    @Test public void analyzeHelixPhiPsiValues() throws IOException
    {
        // Verify phi/psi are in alpha helix region (~-57, ~-47)
        Set results = Ramalyze.analyzeModel(helixModel, helixModel.getStates().values());
        for (Object o : results)
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) o;
            if (eval.score.equals(Ramalyze.RamaEval.NOSCORE)) continue;
            assertTrue("phi should be negative for helix: " + eval.phi, eval.phi < 0);
            assertTrue("psi should be negative for helix: " + eval.psi, eval.psi < 0);
            assertEquals(-57, eval.phi, 10);
            assertEquals(-47, eval.psi, 10);
        }
    }

    @Test public void analyzeHelixTypeIsGeneral() throws IOException
    {
        // All alanines should be classified as General case
        Set results = Ramalyze.analyzeModel(helixModel, helixModel.getStates().values());
        for (Object o : results)
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) o;
            assertEquals(Ramalyze.RamaEval.GENERAL, eval.type);
        }
    }

    @Test public void analyzeHelixNumScorePositive() throws IOException
    {
        // All scores should be positive for a favored region
        Set results = Ramalyze.analyzeModel(helixModel, helixModel.getStates().values());
        for (Object o : results)
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) o;
            assertTrue("numscore should be positive: " + eval.numscore, eval.numscore > 0);
        }
    }

    // --- Beta strand ---

    @Test public void analyzeBetaReturnsResults() throws IOException
    {
        Set results = Ramalyze.analyzeModel(betaModel, betaModel.getStates().values());
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    @Test public void analyzeBetaAllFavoredOrAllowed() throws IOException
    {
        // Ideal beta strand should be all Favored or Allowed
        Set results = Ramalyze.analyzeModel(betaModel, betaModel.getStates().values());
        for (Object o : results)
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) o;
            assertTrue("Beta residue should be Favored or Allowed: " + eval.score,
                eval.score.equals(Ramalyze.RamaEval.FAVORED) ||
                eval.score.equals(Ramalyze.RamaEval.ALLOWED));
        }
    }

    @Test public void analyzeBetaPhiPsiValues() throws IOException
    {
        // Beta strand phi/psi: roughly (-120, 120) or (-140, 130)
        Set results = Ramalyze.analyzeModel(betaModel, betaModel.getStates().values());
        for (Object o : results)
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) o;
            if (eval.score.equals(Ramalyze.RamaEval.NOSCORE)) continue;
            assertTrue("phi should be negative for beta: " + eval.phi, eval.phi < 0);
            assertTrue("psi should be positive for beta: " + eval.psi, eval.psi > 0);
        }
    }

    // --- getEvals ---

    @Test public void getEvalsReturnsScores() throws IOException
    {
        Ramalyze ramalyze = new Ramalyze();
        HashMap<Residue,Double> evals = ramalyze.getEvals(helixModel);
        assertNotNull(evals);
        assertTrue(evals.size() > 0);
    }

    @Test public void getEvalsScoresArePositive() throws IOException
    {
        Ramalyze ramalyze = new Ramalyze();
        HashMap<Residue,Double> evals = ramalyze.getEvals(helixModel);
        for (Double score : evals.values())
            assertTrue("Score should be positive for ideal helix: " + score, score > 0);
    }

    // --- RamaEval constants ---

    @Test public void ramaEvalConstants()
    {
        assertEquals("Favored", Ramalyze.RamaEval.FAVORED);
        assertEquals("Allowed", Ramalyze.RamaEval.ALLOWED);
        assertEquals("OUTLIER", Ramalyze.RamaEval.OUTLIER);
        assertEquals("Not evaluated", Ramalyze.RamaEval.NOSCORE);
    }
}
