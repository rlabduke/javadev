package chiropraxis.rotarama;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;

public class NDFloatTableTest
{
    static final float EPS = 1e-6f;

    // Helper: create a simple 1D table
    static NDFloatTable make1D(int bins, float min, float max, boolean wrap)
    {
        return new NDFloatTable("test1D", 1,
            new float[]{min}, new float[]{max},
            new int[]{bins}, new boolean[]{wrap});
    }

    // Helper: create a 2D table
    static NDFloatTable make2D(int binsX, int binsY, float minX, float maxX,
        float minY, float maxY, boolean wrapX, boolean wrapY)
    {
        return new NDFloatTable("test2D", 2,
            new float[]{minX, minY}, new float[]{maxX, maxY},
            new int[]{binsX, binsY}, new boolean[]{wrapX, wrapY});
    }

    // --- constructor and basic properties ---

    @Test public void constructorSetsProperties()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        assertEquals("test1D", t.getName());
        assertEquals(1, t.getDimensions());
        assertEquals(0, t.realCount());
    }

    @Test public void initialValuesAreZero()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        assertEquals(0, t.totalCount(), EPS);
        assertEquals(0, t.maxValue(), EPS);
    }

    // --- whereIs / centerOf ---

    @Test public void whereIs1D()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        // bin width = 10, so value 5 -> bin 0, value 15 -> bin 1
        int[] bin = t.whereIs(new float[]{5});
        assertEquals(0, bin[0]);
        bin = t.whereIs(new float[]{15});
        assertEquals(1, bin[0]);
        bin = t.whereIs(new float[]{95});
        assertEquals(9, bin[0]);
    }

    @Test public void centerOf1D()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        // Bin 0: center at 5, Bin 1: center at 15
        float[] center = t.centerOf(new int[]{0});
        assertEquals(5, center[0], EPS);
        center = t.centerOf(new int[]{1});
        assertEquals(15, center[0], EPS);
    }

    @Test public void whereIs2D()
    {
        NDFloatTable t = make2D(4, 4, 0, 360, 0, 360, true, true);
        // bin width = 90 in each dim
        int[] bin = t.whereIs(new float[]{45, 135});
        assertEquals(0, bin[0]);
        assertEquals(1, bin[1]);
    }

    // --- contains ---

    @Test public void containsInRange()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        assertTrue(t.contains(new float[]{50}));
    }

    @Test public void containsOutOfRange()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        assertFalse(t.contains(new float[]{-1}));
        assertFalse(t.contains(new float[]{101}));
    }

    @Test public void containsWrappedAlwaysTrue()
    {
        NDFloatTable t = make1D(10, 0, 360, true);
        assertTrue(t.contains(new float[]{-10}));
        assertTrue(t.contains(new float[]{400}));
    }

    @Test public void containsBin()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        assertTrue(t.contains(new int[]{5}));
        assertFalse(t.contains(new int[]{-1}));
        assertFalse(t.contains(new int[]{10}));
    }

    // --- tallySimple ---

    @Test public void tallySimpleIncrementsCorrectBin()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        t.tallySimple(new float[]{25}); // bin 2
        t.tallySimple(new float[]{25}); // bin 2 again
        t.tallySimple(new float[]{75}); // bin 7
        assertEquals(2, t.valueAt(new int[]{2}), EPS);
        assertEquals(1, t.valueAt(new int[]{7}), EPS);
        assertEquals(0, t.valueAt(new int[]{0}), EPS);
        assertEquals(3, t.realCount());
    }

    @Test public void totalCountMatchesTallies()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        for(int i = 0; i < 5; i++)
            t.tallySimple(new float[]{50});
        assertEquals(5, t.totalCount(), EPS);
    }

    // --- setValueAt / valueAt ---

    @Test public void setAndGetValue()
    {
        NDFloatTable t = make1D(10, 0, 100, false);
        t.setValueAt(new int[]{3}, 42.0f);
        assertEquals(42.0f, t.valueAt(new int[]{3}), EPS);
    }

    // --- maxValue ---

    @Test public void maxValueFindsMax()
    {
        NDFloatTable t = make1D(5, 0, 50, false);
        t.setValueAt(new int[]{0}, 1.0f);
        t.setValueAt(new int[]{2}, 5.0f);
        t.setValueAt(new int[]{4}, 3.0f);
        assertEquals(5.0f, t.maxValue(), EPS);
    }

    // --- zero ---

    @Test public void zeroClearsTable()
    {
        NDFloatTable t = make1D(5, 0, 50, false);
        t.tallySimple(new float[]{25});
        t.tallySimple(new float[]{25});
        t.zero();
        assertEquals(0, t.totalCount(), EPS);
        assertEquals(0, t.realCount());
    }

    // --- scale ---

    @Test public void scaleMultipliesAllBins()
    {
        NDFloatTable t = make1D(5, 0, 50, false);
        t.setValueAt(new int[]{0}, 2.0f);
        t.setValueAt(new int[]{1}, 4.0f);
        t.scale(3.0f);
        assertEquals(6.0f, t.valueAt(new int[]{0}), EPS);
        assertEquals(12.0f, t.valueAt(new int[]{1}), EPS);
    }

    // --- standardize ---

    @Test public void standardizeSetsMaxToValue()
    {
        NDFloatTable t = make1D(5, 0, 50, false);
        t.setValueAt(new int[]{0}, 2.0f);
        t.setValueAt(new int[]{2}, 10.0f);
        t.standardize(1.0f);
        assertEquals(1.0f, t.maxValue(), EPS);
        assertEquals(0.2f, t.valueAt(new int[]{0}), EPS);
    }

    // --- transformLog ---

    @Test public void transformLogAppliesLn()
    {
        NDFloatTable t = make1D(3, 0, 30, false);
        t.setValueAt(new int[]{0}, 0.0f);   // ln(0+1) = 0
        t.setValueAt(new int[]{1}, (float)(Math.E - 1)); // ln(e) = 1
        t.transformLog();
        assertEquals(0.0f, t.valueAt(new int[]{0}), EPS);
        assertEquals(1.0f, t.valueAt(new int[]{1}), 0.001f);
    }

    // --- interpolated valueAt ---

    @Test public void interpolation1DAtBinCenter()
    {
        NDFloatTable t = make1D(4, 0, 40, false);
        // Bin centers: 5, 15, 25, 35
        t.setValueAt(new int[]{1}, 10.0f);
        // At the center of bin 1, should get 10.0
        float val = t.valueAt(new float[]{15});
        assertEquals(10.0f, val, 0.5f); // approximate due to interpolation
    }

    @Test public void interpolation1DBetweenBins()
    {
        NDFloatTable t = make1D(4, 0, 40, false);
        t.setValueAt(new int[]{0}, 0.0f);
        t.setValueAt(new int[]{1}, 10.0f);
        // Midpoint between bin 0 center (5) and bin 1 center (15) is 10
        float val = t.valueAt(new float[]{10});
        assertEquals(5.0f, val, 0.5f); // midpoint interpolation
    }

    // --- 2D table ---

    @Test public void table2DSetAndGet()
    {
        NDFloatTable t = make2D(4, 4, 0, 360, 0, 360, true, true);
        t.setValueAt(new int[]{1, 2}, 99.0f);
        assertEquals(99.0f, t.valueAt(new int[]{1, 2}), EPS);
        assertEquals(0.0f, t.valueAt(new int[]{0, 0}), EPS);
    }

    @Test public void table2DTallySimple()
    {
        NDFloatTable t = make2D(4, 4, 0, 360, 0, 360, true, true);
        t.tallySimple(new float[]{45, 135}); // bin [0,1]
        assertEquals(1.0f, t.valueAt(new int[]{0, 1}), EPS);
    }

    // --- tallyGaussian ---

    @Test public void tallyGaussianSpreads()
    {
        NDFloatTable t = make1D(20, 0, 100, false);
        t.tallyGaussian(new float[]{50}, 10);
        // Center bin should have the most
        int[] centerBin = t.whereIs(new float[]{50});
        float centerVal = t.valueAt(centerBin);
        assertTrue("center should be positive", centerVal > 0);
        // Neighbor bins should also have some value
        float neighborVal = t.valueAt(new int[]{centerBin[0] + 1});
        assertTrue("neighbor should be positive", neighborVal > 0);
        assertTrue("center > neighbor", centerVal > neighborVal);
        assertEquals(1, t.realCount());
    }

    // --- tallyCosine ---

    @Test public void tallyCosineSpreads()
    {
        NDFloatTable t = make1D(20, 0, 100, false);
        t.tallyCosine(new float[]{50}, 15);
        int[] centerBin = t.whereIs(new float[]{50});
        float centerVal = t.valueAt(centerBin);
        assertTrue("center should be positive", centerVal > 0);
        assertEquals(1, t.realCount());
    }

    // --- distanceSquared ---

    @Test public void distanceSquaredBasic()
    {
        float d2 = NDFloatTable.distanceSquared(
            new float[]{0, 0, 0}, new float[]{3, 4, 0});
        assertEquals(25.0f, d2, EPS);
    }

    @Test public void distanceSquaredSamePoint()
    {
        float d2 = NDFloatTable.distanceSquared(
            new float[]{5, 5}, new float[]{5, 5});
        assertEquals(0.0f, d2, EPS);
    }

    // --- wrapping ---

    @Test public void wrappingBinsAreValid()
    {
        NDFloatTable t = make1D(36, 0, 360, true);
        // Negative bin should wrap
        int[] negBin = {-1};
        assertTrue("wrapped bin should be valid", t.contains(negBin));
    }

    // --- binary I/O roundtrip ---

    @Test public void binaryRoundtrip() throws Exception
    {
        NDFloatTable orig = make2D(5, 5, -180, 180, -180, 180, true, true);
        orig.setValueAt(new int[]{0, 0}, 1.0f);
        orig.setValueAt(new int[]{2, 3}, 42.5f);
        orig.setValueAt(new int[]{4, 4}, 99.9f);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        orig.writeBinary(new DataOutputStream(baos));

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        NDFloatTable loaded = new NDFloatTable(new DataInputStream(bais));

        assertEquals(orig.getName(), loaded.getName());
        assertEquals(orig.getDimensions(), loaded.getDimensions());
        assertArrayEquals(orig.getBins(), loaded.getBins());
        assertEquals(1.0f, loaded.valueAt(new int[]{0, 0}), EPS);
        assertEquals(42.5f, loaded.valueAt(new int[]{2, 3}), EPS);
        assertEquals(99.9f, loaded.valueAt(new int[]{4, 4}), EPS);
    }

    // --- name ---

    @Test public void setName()
    {
        NDFloatTable t = make1D(5, 0, 50, false);
        t.setName("newName");
        assertEquals("newName", t.getName());
    }
}
