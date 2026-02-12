package chiropraxis.rotarama;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class RotCorTest
{
    RotCor rotcor;

    @Before public void setUp()
    { rotcor = new RotCor(); }

    // --- calcModalRotName ---

    @Test public void modalRotNameUnanimous()
    { assertEquals("tp", rotcor.calcModalRotName(new String[]{"tp", "tp", "tp"})); }

    @Test public void modalRotNameMajority()
    { assertEquals("tp", rotcor.calcModalRotName(new String[]{"tp", "tp", "mm", "tp", "mm"})); }

    @Test public void modalRotNameSingle()
    { assertEquals("tt", rotcor.calcModalRotName(new String[]{"tt"})); }

    @Test public void modalRotNameTied()
    {
        // With a tie, should return one of the tied names
        String result = rotcor.calcModalRotName(new String[]{"tp", "mm"});
        assertTrue(result.equals("tp") || result.equals("mm"));
    }

    @Test public void modalRotNameMultipleCandidates()
    {
        // 3 "tp", 2 "mm", 1 "tt" => "tp" wins
        assertEquals("tp", rotcor.calcModalRotName(
            new String[]{"tp", "mm", "tp", "tt", "mm", "tp"}));
    }

    // --- calcModalRotFrac ---

    @Test public void modalRotFracUnanimous()
    { assertEquals(1.0, rotcor.calcModalRotFrac(new String[]{"tp", "tp", "tp"}), 1e-10); }

    @Test public void modalRotFracHalf()
    { assertEquals(0.5, rotcor.calcModalRotFrac(new String[]{"tp", "mm"}), 1e-10); }

    @Test public void modalRotFracThreeOfFive()
    { assertEquals(0.6, rotcor.calcModalRotFrac(new String[]{"tp", "tp", "mm", "tp", "mm"}), 1e-10); }

    @Test public void modalRotFracExcludesOutlier()
    {
        // OUTLIER should not be chosen as the modal rotamer
        double frac = rotcor.calcModalRotFrac(
            new String[]{"OUTLIER", "OUTLIER", "OUTLIER", "tp"});
        // "tp" has 1/4 = 0.25, OUTLIER is excluded from being modal
        assertEquals(0.25, frac, 1e-10);
    }

    @Test public void modalRotFracSingleOutlier()
    {
        // If everything is OUTLIER, calcModalRotFrac should still return
        // based on the non-OUTLIER modal; with only OUTLIERs, maxFreq stays -1
        // so result would be -1/count which is negative. This tests the edge case.
        double frac = rotcor.calcModalRotFrac(new String[]{"OUTLIER", "OUTLIER"});
        // maxFreq is -1, tally is 2 => frac = -0.5
        assertTrue(frac < 0);
    }

    // --- isConsensus ---

    // 1-chi residues (CYS,SER,THR,VAL,PRO): threshold 0.85
    @Test public void isConsensus1ChiAbove()
    { assertTrue(rotcor.isConsensus("CYS", 0.90)); }

    @Test public void isConsensus1ChiAtThreshold()
    { assertTrue(rotcor.isConsensus("VAL", 0.85)); }

    @Test public void isConsensus1ChiBelow()
    { assertFalse(rotcor.isConsensus("SER", 0.80)); }

    // 2-chi residues (ASN,ASP,HIS,ILE,LEU,PHE,TRP,TYR): threshold 0.7
    @Test public void isConsensus2ChiAbove()
    { assertTrue(rotcor.isConsensus("LEU", 0.75)); }

    @Test public void isConsensus2ChiBelow()
    { assertFalse(rotcor.isConsensus("PHE", 0.65)); }

    // 3-chi residues (GLN,GLU,MET): threshold 0.55
    @Test public void isConsensus3ChiAbove()
    { assertTrue(rotcor.isConsensus("GLU", 0.60)); }

    @Test public void isConsensus3ChiBelow()
    { assertFalse(rotcor.isConsensus("MET", 0.50)); }

    // 4-chi residues (ARG,LYS): threshold 0.4
    @Test public void isConsensus4ChiAbove()
    { assertTrue(rotcor.isConsensus("ARG", 0.45)); }

    @Test public void isConsensus4ChiAtThreshold()
    { assertTrue(rotcor.isConsensus("LYS", 0.40)); }

    @Test public void isConsensus4ChiBelow()
    { assertFalse(rotcor.isConsensus("LYS", 0.35)); }

    // Non-rotameric residues
    @Test public void isConsensusGlyAlwaysFalse()
    { assertFalse(rotcor.isConsensus("GLY", 1.0)); }

    @Test public void isConsensusAlaAlwaysFalse()
    { assertFalse(rotcor.isConsensus("ALA", 1.0)); }

    // --- Constants ---

    @Test public void noConsensusConstant()
    { assertEquals("NO_CONSENSUS", RotCor.NO_CONSENSUS_ROTNAME); }
}
