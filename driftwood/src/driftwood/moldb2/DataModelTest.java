package driftwood.moldb2;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;

/**
 * Tests for the core moldb2 data model: Atom, AtomState, Residue,
 * Model, ModelState, and CoordinateFile.
 */
public class DataModelTest
{
    static final double EPS = 1e-6;

    // ========== Atom ==========

    @Test public void atomConstructorThreeArg()
    {
        Atom a = new Atom(" CA ", "C", false);
        assertEquals(" CA ", a.getName());
        assertEquals("C", a.getElement());
        assertFalse(a.isHet());
        assertNull(a.getResidue());
    }

    @Test public void atomConstructorHet()
    {
        Atom a = new Atom(" O  ", "O", true);
        assertTrue(a.isHet());
    }

    @Test public void atomConstructorOneArg()
    {
        Atom a = new Atom(" N  ");
        assertEquals(" N  ", a.getName());
        assertEquals("XX", a.getElement());
        assertFalse(a.isHet());
    }

    @Test public void atomCopyConstructor()
    {
        Atom orig = new Atom(" CA ", "C", true);
        Atom copy = new Atom(orig);
        assertEquals(orig.getName(), copy.getName());
        assertEquals(orig.getElement(), copy.getElement());
        assertEquals(orig.isHet(), copy.isHet());
        assertNotSame(orig, copy);
        assertFalse(orig.equals(copy)); // strict identity
    }

    @Test(expected = NullPointerException.class)
    public void atomNullNameThrows()
    { new Atom(null, "C", false); }

    @Test(expected = NullPointerException.class)
    public void atomNullElementThrows()
    { new Atom(" CA ", null, false); }

    @Test public void atomToStringWithoutResidue()
    {
        Atom a = new Atom(" CA ");
        assertEquals(" CA ", a.toString());
    }

    @Test public void atomToStringWithResidue() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        Atom a = new Atom(" CA ", "C", false);
        res.add(a);
        assertTrue(a.toString().contains(" CA "));
        assertSame(res, a.getResidue());
    }

    // ========== AtomState ==========

    @Test public void atomStateConstructor()
    {
        Atom a = new Atom(" CA ", "C", false);
        AtomState as = new AtomState(a, "1");
        assertSame(a, as.getAtom());
        assertEquals("1", as.getSerial());
        assertEquals(" ", as.getAltConf());
        assertEquals(0.0, as.getCharge(), EPS);
        assertEquals(0.0, as.getTempFactor(), EPS);
        assertEquals(0.0, as.getOccupancy(), EPS);
        assertEquals("", as.getPast80());
        assertNull(as.getAnisoU());
    }

    @Test(expected = NullPointerException.class)
    public void atomStateNullAtomThrows()
    { new AtomState(null, "1"); }

    @Test public void atomStateCoordinates()
    {
        Atom a = new Atom(" CA ");
        AtomState as = new AtomState(a, "1");
        as.setX(1.5);
        as.setY(2.5);
        as.setZ(3.5);
        assertEquals(1.5, as.getX(), EPS);
        assertEquals(2.5, as.getY(), EPS);
        assertEquals(3.5, as.getZ(), EPS);
    }

    @Test public void atomStateProperties()
    {
        Atom a = new Atom(" CA ");
        AtomState as = new AtomState(a, "1");
        as.setTempFactor(30.0);
        as.setOccupancy(0.75);
        as.setCharge(-1.0);
        as.setAltConf("A");
        as.setPast80("extra data");
        as.setAnisoU("ANISOU    1  CA  ALA A   1  ...");
        assertEquals(30.0, as.getTempFactor(), EPS);
        assertEquals(0.75, as.getOccupancy(), EPS);
        assertEquals(-1.0, as.getCharge(), EPS);
        assertEquals("A", as.getAltConf());
        assertEquals("extra data", as.getPast80());
        assertEquals("ANISOU    1  CA  ALA A   1  ...", as.getAnisoU());
    }

    @Test public void atomStateConvenienceMethods() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        Atom a = new Atom(" CA ", "C", true);
        res.add(a);
        AtomState as = new AtomState(a, "1");
        assertEquals(" CA ", as.getName());
        assertEquals("C", as.getElement());
        assertSame(res, as.getResidue());
        assertTrue(as.isHet());
    }

    @Test public void atomStateClone()
    {
        Atom a = new Atom(" CA ");
        AtomState as = new AtomState(a, "1");
        as.setX(10.0);
        as.setY(20.0);
        as.setZ(30.0);
        as.setTempFactor(25.0);
        AtomState clone = (AtomState) as.clone();
        assertEquals(as.getX(), clone.getX(), EPS);
        assertEquals(as.getY(), clone.getY(), EPS);
        assertEquals(as.getZ(), clone.getZ(), EPS);
        assertEquals(as.getTempFactor(), clone.getTempFactor(), EPS);
        assertSame(as.getAtom(), clone.getAtom());
    }

    @Test public void atomStateCloneFor()
    {
        Atom a1 = new Atom(" CA ");
        Atom a2 = new Atom(" CA ");
        AtomState as = new AtomState(a1, "1");
        as.setX(5.0);
        as.setTempFactor(15.0);
        AtomState cloned = as.cloneFor(a2, "2");
        assertSame(a2, cloned.getAtom());
        assertEquals("2", cloned.getSerial());
        assertEquals(5.0, cloned.getX(), EPS);
        assertEquals(15.0, cloned.getTempFactor(), EPS);
    }

    @Test public void atomStateCloneForKeepsSerial()
    {
        Atom a1 = new Atom(" CA ");
        Atom a2 = new Atom(" CB ");
        AtomState as = new AtomState(a1, "42");
        AtomState cloned = as.cloneFor(a2);
        assertEquals("42", cloned.getSerial());
        assertSame(a2, cloned.getAtom());
    }

    // ========== Residue ==========

    @Test public void residueConstructor()
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        assertEquals(" A", res.getChain());
        assertEquals("", res.getSegment());
        assertEquals("   1", res.getSequenceNumber());
        assertEquals(1, res.getSequenceInteger());
        assertEquals(" ", res.getInsertionCode());
        assertEquals("ALA", res.getName());
    }

    @Test public void residueNullArgThrows()
    {
        try { new Residue(null, "", "1", " ", "ALA"); fail(); }
        catch (IllegalArgumentException e) {}
        try { new Residue(" A", null, "1", " ", "ALA"); fail(); }
        catch (IllegalArgumentException e) {}
        try { new Residue(" A", "", null, " ", "ALA"); fail(); }
        catch (IllegalArgumentException e) {}
        try { new Residue(" A", "", "1", null, "ALA"); fail(); }
        catch (IllegalArgumentException e) {}
        try { new Residue(" A", "", "1", " ", null); fail(); }
        catch (IllegalArgumentException e) {}
    }

    @Test public void residueSegidAsChain()
    {
        // When chain is blank but segment is not, segment is used as chain
        Residue res = new Residue("  ", "A1", "   1", " ", "ALA");
        assertEquals("A1", res.getChain());
        assertEquals("", res.getSegment());
    }

    @Test public void residueSegidNotUsedWhenChainPresent()
    {
        Residue res = new Residue(" B", "SEG1", "   1", " ", "ALA");
        assertEquals(" B", res.getChain());
        assertEquals("SEG1", res.getSegment());
    }

    @Test public void residueNanSeq()
    {
        Residue res = new Residue(" A", "", "ABC", " ", "ALA");
        assertEquals(Residue.NAN_SEQ, res.getSequenceInteger());
    }

    @Test public void residueAddAndGetAtom() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        Atom ca = new Atom(" CA ", "C", false);
        Atom n = new Atom(" N  ", "N", false);
        res.add(ca);
        res.add(n);
        assertSame(ca, res.getAtom(" CA "));
        assertSame(n, res.getAtom(" N  "));
        assertNull(res.getAtom(" CB "));
        assertEquals(2, res.getAtoms().size());
    }

    @Test(expected = AtomException.class)
    public void residueAddDuplicateNameThrows() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        res.add(new Atom(" CA ", "C", false));
        res.add(new Atom(" CA ", "C", false)); // same name
    }

    @Test public void residueAddMovesAtomFromOtherResidue() throws Exception
    {
        Residue res1 = new Residue(" A", "", "   1", " ", "ALA");
        Residue res2 = new Residue(" A", "", "   2", " ", "GLY");
        Atom a = new Atom(" CA ", "C", false);
        res1.add(a);
        assertSame(res1, a.getResidue());
        res2.add(a);
        assertSame(res2, a.getResidue());
        assertNull(res1.getAtom(" CA "));
        assertSame(a, res2.getAtom(" CA "));
    }

    @Test public void residueRemoveAtom() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        Atom a = new Atom(" CA ");
        res.add(a);
        res.remove(a);
        assertNull(res.getAtom(" CA "));
        assertNull(a.getResidue());
    }

    @Test(expected = AtomException.class)
    public void residueRemoveNonMemberThrows() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        Atom a = new Atom(" CA ");
        res.remove(a);
    }

    @Test public void residueAtomsUnmodifiable() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        res.add(new Atom(" CA "));
        try {
            res.getAtoms().clear();
            fail("Should be unmodifiable");
        } catch (UnsupportedOperationException e) {}
    }

    @Test public void residueModCount() throws Exception
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        int before = res.getModCount();
        res.add(new Atom(" CA "));
        assertTrue(res.getModCount() > before);
    }

    @Test public void residueCNIT()
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        String cnit = res.getCNIT();
        assertEquals(10, cnit.length());
        assertTrue(cnit.contains("A"));
        assertTrue(cnit.contains("ALA"));
    }

    @Test public void residueNickname()
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        String nick = res.nickname();
        assertEquals("Ala1", nick);
    }

    @Test public void residueCompareTo()
    {
        Residue r1 = new Residue(" A", "", "   1", " ", "ALA");
        Residue r2 = new Residue(" A", "", "   2", " ", "GLY");
        Residue r3 = new Residue(" B", "", "   1", " ", "ALA");
        assertTrue(r1.compareTo(r2) < 0); // same chain, seq 1 < 2
        assertTrue(r1.compareTo(r3) < 0); // chain A < B
        assertTrue(r2.compareTo(r1) > 0);
        assertEquals(0, r1.compareTo(r1)); // same object
    }

    @Test public void residueCompareToNull()
    {
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        assertTrue(r.compareTo(null) > 0);
    }

    @Test public void residueCompareByInsertionCode()
    {
        Residue r1 = new Residue(" A", "", "  10", "A", "ALA");
        Residue r2 = new Residue(" A", "", "  10", "B", "ALA");
        assertTrue(r1.compareTo(r2) < 0);
    }

    @Test public void residueCopyConstructor() throws Exception
    {
        Residue orig = new Residue(" A", "", "   1", " ", "ALA");
        orig.add(new Atom(" CA ", "C", false));
        orig.add(new Atom(" N  ", "N", false));
        Residue copy = new Residue(orig);
        assertEquals(orig.getChain(), copy.getChain());
        assertEquals(orig.getName(), copy.getName());
        assertNotSame(orig, copy);
        // Copy constructor without template's atoms
        assertEquals(0, copy.getAtoms().size());
    }

    @Test public void residueDeepCopyConstructor() throws Exception
    {
        Residue orig = new Residue(" A", "", "   1", " ", "ALA");
        orig.add(new Atom(" CA ", "C", false));
        orig.add(new Atom(" N  ", "N", false));
        Residue copy = new Residue(orig, " A", "", "   1", " ", "ALA");
        // Deep copy copies atoms
        assertEquals(2, copy.getAtoms().size());
        assertNotNull(copy.getAtom(" CA "));
        assertNotNull(copy.getAtom(" N  "));
        // Atoms should be different objects
        assertNotSame(orig.getAtom(" CA "), copy.getAtom(" CA "));
    }

    @Test public void residueToString()
    {
        Residue res = new Residue(" A", "", "   1", " ", "ALA");
        String s = res.toString();
        assertNotNull(s);
        assertTrue(s.contains("ALA"));
    }

    // ========== Model ==========

    @Test public void modelConstructor()
    {
        Model m = new Model("model1");
        assertEquals("model1", m.getName());
        assertEquals("model1", m.toString());
        assertTrue(m.getResidues().isEmpty());
        assertTrue(m.getChainIDs().isEmpty());
        assertTrue(m.getStates().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void modelNullNameThrows()
    { new Model(null); }

    @Test public void modelSetName()
    {
        Model m = new Model("old");
        m.setName("new");
        assertEquals("new", m.getName());
    }

    @Test(expected = NullPointerException.class)
    public void modelSetNullNameThrows()
    {
        Model m = new Model("test");
        m.setName(null);
    }

    @Test public void modelAddResidue() throws Exception
    {
        Model m = new Model("test");
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        m.add(r);
        assertEquals(1, m.getResidues().size());
        assertTrue(m.contains(r));
    }

    @Test(expected = ResidueException.class)
    public void modelAddDuplicateResidueThrows() throws Exception
    {
        Model m = new Model("test");
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        m.add(r);
        m.add(r);
    }

    @Test public void modelRemoveResidue() throws Exception
    {
        Model m = new Model("test");
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        m.add(r);
        m.remove(r);
        assertFalse(m.contains(r));
        assertTrue(m.getResidues().isEmpty());
    }

    @Test(expected = ResidueException.class)
    public void modelRemoveNonMemberThrows() throws Exception
    {
        Model m = new Model("test");
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        m.remove(r);
    }

    @Test public void modelChainManagement() throws Exception
    {
        Model m = new Model("test");
        Residue r1 = new Residue(" A", "", "   1", " ", "ALA");
        Residue r2 = new Residue(" A", "", "   2", " ", "GLY");
        Residue r3 = new Residue(" B", "", "   1", " ", "VAL");
        m.add(r1);
        m.add(r2);
        m.add(r3);
        assertEquals(2, m.getChainIDs().size());
        assertTrue(m.getChainIDs().contains(" A"));
        assertTrue(m.getChainIDs().contains(" B"));
        assertEquals(2, m.getChain(" A").size());
        assertEquals(1, m.getChain(" B").size());
    }

    @Test public void modelGetChainNull() throws Exception
    {
        Model m = new Model("test");
        assertNull(m.getChain(" Z"));
    }

    @Test public void modelRemoveCleanupChain() throws Exception
    {
        Model m = new Model("test");
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        m.add(r);
        assertEquals(1, m.getChainIDs().size());
        m.remove(r);
        assertTrue(m.getChainIDs().isEmpty());
    }

    @Test public void modelSegmentManagement() throws Exception
    {
        Model m = new Model("test");
        Residue r1 = new Residue(" A", "SEG1", "   1", " ", "ALA");
        Residue r2 = new Residue(" A", "SEG2", "   2", " ", "GLY");
        m.add(r1);
        m.add(r2);
        assertEquals(2, m.getSegmentIDs().size());
    }

    @Test public void modelReplaceResidue() throws Exception
    {
        Model m = new Model("test");
        Residue old = new Residue(" A", "", "   1", " ", "ALA");
        Residue replacement = new Residue(" A", "", "   1", " ", "GLY");
        m.add(old);
        m.replace(old, replacement);
        assertFalse(m.contains(old));
        assertTrue(m.contains(replacement));
        assertEquals(1, m.getResidues().size());
    }

    @Test public void modelGetResidueByCNIT() throws Exception
    {
        Model m = new Model("test");
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        m.add(r);
        String cnit = r.getCNIT();
        assertSame(r, m.getResidue(cnit));
        assertNull(m.getResidue("ZZZZZZZZZZ"));
    }

    @Test public void modelStateManagement() throws Exception
    {
        Model m = new Model("test");
        ModelState state = new ModelState();
        state.setName(" ");
        Map states = new TreeMap();
        states.put(" ", state);
        m.setStates(states);
        assertSame(state, m.getState(" "));
        assertSame(state, m.getState()); // default is " " when no alt confs
    }

    @Test public void modelStateDefaultAltConf() throws Exception
    {
        Model m = new Model("test");
        ModelState stateA = new ModelState();
        stateA.setName("A");
        ModelState stateB = new ModelState();
        stateB.setName("B");
        Map states = new TreeMap();
        states.put("A", stateA);
        states.put("B", stateB);
        m.setStates(states);
        // Default state is "A" when alt confs present
        assertSame(stateA, m.getState());
    }

    @Test public void modelClone() throws Exception
    {
        Model m = new Model("test");
        Residue r1 = new Residue(" A", "", "   1", " ", "ALA");
        Residue r2 = new Residue(" A", "", "   2", " ", "GLY");
        m.add(r1);
        m.add(r2);
        Model clone = (Model) m.clone();
        assertEquals(m.getName(), clone.getName());
        assertEquals(2, clone.getResidues().size());
        assertTrue(clone.contains(r1)); // residues are shared, not deep copied
        // But modifications to clone don't affect original
        clone.remove(r2);
        assertTrue(m.contains(r2));
        assertFalse(clone.contains(r2));
    }

    @Test public void modelResiduesUnmodifiable() throws Exception
    {
        Model m = new Model("test");
        try {
            m.getResidues().clear();
            fail("Should be unmodifiable");
        } catch (UnsupportedOperationException e) {}
    }

    @Test public void modelModCount() throws Exception
    {
        Model m = new Model("test");
        int before = m.getModCount();
        m.add(new Residue(" A", "", "   1", " ", "ALA"));
        assertTrue(m.getModCount() > before);
    }

    @Test public void modelDisulfides() throws Exception
    {
        Model m = new Model("test");
        assertNull(m.getDisulfides());
        Disulfides d = new Disulfides.NoDisulfides();
        m.setDisulfides(d);
        assertSame(d, m.getDisulfides());
    }

    // ========== ModelState ==========

    @Test public void modelStateNoParent() throws Exception
    {
        ModelState ms = new ModelState();
        assertNull(ms.getParent());
        Atom a = new Atom(" CA ");
        AtomState as = new AtomState(a, "1");
        ms.add(as);
        assertSame(as, ms.get(a));
        assertSame(as, ms.getLocal(a));
    }

    @Test(expected = AtomException.class)
    public void modelStateGetMissingThrows() throws Exception
    {
        ModelState ms = new ModelState();
        ms.get(new Atom(" CA "));
    }

    @Test(expected = AtomException.class)
    public void modelStateGetNullThrows() throws Exception
    {
        ModelState ms = new ModelState();
        ms.get(null);
    }

    @Test(expected = AtomException.class)
    public void modelStateAddDuplicateThrows() throws Exception
    {
        ModelState ms = new ModelState();
        Atom a = new Atom(" CA ");
        ms.add(new AtomState(a, "1"));
        ms.add(new AtomState(a, "2"));
    }

    @Test public void modelStateAddOverwrite() throws Exception
    {
        ModelState ms = new ModelState();
        Atom a = new Atom(" CA ");
        AtomState as1 = new AtomState(a, "1");
        as1.setX(1.0);
        AtomState as2 = new AtomState(a, "2");
        as2.setX(2.0);
        ms.add(as1);
        AtomState prev = ms.addOverwrite(as2);
        assertSame(as1, prev);
        assertSame(as2, ms.get(a));
    }

    @Test public void modelStateParentChaining() throws Exception
    {
        ModelState parent = new ModelState();
        Atom a1 = new Atom(" CA ");
        AtomState as1 = new AtomState(a1, "1");
        parent.add(as1);

        ModelState child = new ModelState(parent);
        // Child should find parent's state
        assertSame(as1, child.get(a1));
        assertNull(child.getLocal(a1)); // but not locally

        // Child's own state takes precedence
        Atom a2 = new Atom(" CB ");
        AtomState as2 = new AtomState(a2, "2");
        child.add(as2);
        assertSame(as2, child.get(a2));
    }

    @Test public void modelStateHasState() throws Exception
    {
        ModelState ms = new ModelState();
        Atom a = new Atom(" CA ");
        assertFalse(ms.hasState(a));
        ms.add(new AtomState(a, "1"));
        assertTrue(ms.hasState(a));
    }

    @Test public void modelStateHasStateViaParent() throws Exception
    {
        ModelState parent = new ModelState();
        Atom a = new Atom(" CA ");
        parent.add(new AtomState(a, "1"));
        ModelState child = new ModelState(parent);
        assertTrue(child.hasState(a));
    }

    @Test(expected = IllegalArgumentException.class)
    public void modelStateCircularParentThrows()
    {
        ModelState ms = new ModelState();
        ms.setParent(ms);
    }

    @Test(expected = IllegalArgumentException.class)
    public void modelStateIndirectCircularThrows()
    {
        ModelState ms1 = new ModelState();
        ModelState ms2 = new ModelState(ms1);
        ms1.setParent(ms2);
    }

    @Test public void modelStateGetLocalStateMap() throws Exception
    {
        ModelState ms = new ModelState();
        Atom a = new Atom(" CA ");
        ms.add(new AtomState(a, "1"));
        Map map = ms.getLocalStateMap();
        assertEquals(1, map.size());
        // Should be unmodifiable
        try {
            map.clear();
            fail("Should be unmodifiable");
        } catch (UnsupportedOperationException e) {}
    }

    @Test public void modelStateCreateCollapsed() throws Exception
    {
        ModelState parent = new ModelState();
        Atom a1 = new Atom(" CA ");
        AtomState as1 = new AtomState(a1, "1");
        as1.setX(1.0);
        parent.add(as1);

        ModelState child = new ModelState(parent);
        Atom a2 = new Atom(" CB ");
        AtomState as2 = new AtomState(a2, "2");
        as2.setX(2.0);
        child.add(as2);

        ModelState collapsed = child.createCollapsed();
        assertNull(collapsed.getParent());
        // Should have both atoms locally
        assertNotNull(collapsed.getLocal(a1));
        assertNotNull(collapsed.getLocal(a2));
        assertEquals(1.0, collapsed.get(a1).getX(), EPS);
        assertEquals(2.0, collapsed.get(a2).getX(), EPS);
    }

    @Test public void modelStateCreateCollapsedWithExclude() throws Exception
    {
        ModelState grandparent = new ModelState();
        Atom a1 = new Atom(" N  ");
        grandparent.add(new AtomState(a1, "1"));

        ModelState parent = new ModelState(grandparent);
        Atom a2 = new Atom(" CA ");
        parent.add(new AtomState(a2, "2"));

        ModelState child = new ModelState(parent);
        Atom a3 = new Atom(" C  ");
        child.add(new AtomState(a3, "3"));

        // Collapse child onto grandparent: should include a2 and a3 locally, but not a1
        ModelState collapsed = child.createCollapsed(grandparent);
        assertSame(grandparent, collapsed.getParent());
        assertNotNull(collapsed.getLocal(a2));
        assertNotNull(collapsed.getLocal(a3));
        assertNull(collapsed.getLocal(a1));
        // But a1 should still be accessible through parent chain
        assertNotNull(collapsed.get(a1));
    }

    @Test public void modelStateName()
    {
        ModelState ms = new ModelState();
        assertNull(ms.getName());
        ms.setName("A");
        assertEquals("A", ms.getName());
    }

    @Test public void modelStateNameFromParent()
    {
        ModelState parent = new ModelState();
        parent.setName("A");
        ModelState child = new ModelState(parent);
        assertEquals("A", child.getName());
    }

    @Test public void modelStateSizeHintConstructor() throws Exception
    {
        ModelState parent = new ModelState();
        ModelState child = new ModelState(parent, 100);
        assertSame(parent, child.getParent());
    }

    // ========== CoordinateFile ==========

    @Test public void coordinateFileConstructor()
    {
        CoordinateFile cf = new CoordinateFile();
        assertTrue(cf.getModels().isEmpty());
        assertTrue(cf.getHeaders().isEmpty());
        assertNull(cf.getFile());
        assertNull(cf.getIdCode());
    }

    @Test public void coordinateFileAddModel()
    {
        CoordinateFile cf = new CoordinateFile();
        Model m = new Model("1");
        cf.add(m);
        assertEquals(1, cf.getModels().size());
        assertSame(m, cf.getFirstModel());
    }

    @Test(expected = NullPointerException.class)
    public void coordinateFileAddNullThrows()
    {
        CoordinateFile cf = new CoordinateFile();
        cf.add(null);
    }

    @Test(expected = NoSuchElementException.class)
    public void coordinateFileGetFirstModelEmptyThrows()
    {
        CoordinateFile cf = new CoordinateFile();
        cf.getFirstModel();
    }

    @Test public void coordinateFileReplaceModel()
    {
        CoordinateFile cf = new CoordinateFile();
        Model m1 = new Model("1");
        Model m2 = new Model("2");
        cf.add(m1);
        cf.replace(m1, m2);
        assertEquals(1, cf.getModels().size());
        assertSame(m2, cf.getFirstModel());
    }

    @Test public void coordinateFileReplaceNonexistentAdds()
    {
        CoordinateFile cf = new CoordinateFile();
        Model m1 = new Model("1");
        Model m2 = new Model("2");
        cf.add(m1);
        cf.replace(new Model("nonexistent"), m2);
        assertEquals(2, cf.getModels().size());
    }

    @Test public void coordinateFileRemoveModel()
    {
        CoordinateFile cf = new CoordinateFile();
        Model m = new Model("1");
        cf.add(m);
        cf.remove(m);
        assertTrue(cf.getModels().isEmpty());
    }

    @Test public void coordinateFileHeaders()
    {
        CoordinateFile cf = new CoordinateFile();
        cf.addHeader("REMARK", "This is a remark");
        cf.addHeader("TITLE", "Test structure");
        assertEquals(2, cf.getHeaders().size());
    }

    @Test public void coordinateFileUserModHeadersFirst()
    {
        CoordinateFile cf = new CoordinateFile();
        cf.addHeader("REMARK", "First added");
        cf.addHeader(CoordinateFile.SECTION_USER_MOD, "User mod");
        // USER MOD should be at position 0
        Iterator iter = cf.getHeaders().iterator();
        assertEquals("User mod", iter.next());
        assertEquals("First added", iter.next());
    }

    @Test public void coordinateFileIdCode()
    {
        CoordinateFile cf = new CoordinateFile();
        cf.setIdCode("1ABC");
        assertEquals("1ABC", cf.getIdCode());
    }

    @Test public void coordinateFileSecondaryStructure()
    {
        CoordinateFile cf = new CoordinateFile();
        // Default is AllCoil
        assertNotNull(cf.getSecondaryStructure());
    }

    @Test public void coordinateFileDisulfides()
    {
        CoordinateFile cf = new CoordinateFile();
        // Default is NoDisulfides
        assertNotNull(cf.getDisulfides());
    }

    @Test public void coordinateFileDeployDisulfidesToModels() throws Exception
    {
        CoordinateFile cf = new CoordinateFile();
        Model m1 = new Model("1");
        Model m2 = new Model("2");
        cf.add(m1);
        cf.add(m2);
        Disulfides d = new Disulfides.NoDisulfides();
        cf.setDisulfides(d);
        cf.deployDisulfidesToModels();
        assertSame(d, m1.getDisulfides());
        assertSame(d, m2.getDisulfides());
    }

    @Test public void coordinateFilePdbv2Count()
    {
        CoordinateFile cf = new CoordinateFile();
        assertEquals(0, cf.getPdbv2Count());
        cf.setPdbv2Count(42);
        assertEquals(42, cf.getPdbv2Count());
    }

    // ========== Residue.getNext/getPrev (requires Model) ==========

    @Test public void residueGetNextPrev() throws Exception
    {
        Model m = new Model("test");
        Residue r1 = new Residue(" A", "", "   1", " ", "ALA");
        Residue r2 = new Residue(" A", "", "   2", " ", "GLY");
        Residue r3 = new Residue(" A", "", "   3", " ", "VAL");
        m.add(r1);
        m.add(r2);
        m.add(r3);

        assertSame(r2, r1.getNext(m));
        assertSame(r3, r2.getNext(m));
        assertNull(r3.getNext(m));

        assertNull(r1.getPrev(m));
        assertSame(r1, r2.getPrev(m));
        assertSame(r2, r3.getPrev(m));
    }

    @Test public void residueGetNextStopsAtChainBoundary() throws Exception
    {
        Model m = new Model("test");
        Residue r1 = new Residue(" A", "", "   1", " ", "ALA");
        Residue r2 = new Residue(" B", "", "   1", " ", "GLY");
        m.add(r1);
        m.add(r2);
        assertNull(r1.getNext(m));
        assertNull(r2.getPrev(m));
    }

    @Test public void residueGetNextNullModel()
    {
        Residue r = new Residue(" A", "", "   1", " ", "ALA");
        assertNull(r.getNext(null));
        assertNull(r.getPrev(null));
    }

    // ========== Residue.cloneStates ==========

    @Test public void residueCloneStates() throws Exception
    {
        Residue orig = new Residue(" A", "", "   1", " ", "ALA");
        Atom origCA = new Atom(" CA ", "C", false);
        Atom origN = new Atom(" N  ", "N", false);
        orig.add(origCA);
        orig.add(origN);

        ModelState origState = new ModelState();
        AtomState caState = new AtomState(origCA, "1");
        caState.setX(10.0); caState.setY(20.0); caState.setZ(30.0);
        AtomState nState = new AtomState(origN, "2");
        nState.setX(11.0); nState.setY(21.0); nState.setZ(31.0);
        origState.add(caState);
        origState.add(nState);

        Residue copy = new Residue(orig, " A", "", "   1", " ", "ALA");
        ModelState copyState = new ModelState();
        copy.cloneStates(orig, origState, copyState);

        AtomState copyCA = copyState.get(copy.getAtom(" CA "));
        assertEquals(10.0, copyCA.getX(), EPS);
        assertEquals(20.0, copyCA.getY(), EPS);
        assertEquals(30.0, copyCA.getZ(), EPS);
        // Cloned state should reference the copy's atom, not the original
        assertSame(copy.getAtom(" CA "), copyCA.getAtom());
    }

    // ========== ModelState.createForModel ==========

    @Test public void modelStateCreateForModel() throws Exception
    {
        Model model = new Model("test");
        Residue r1 = new Residue(" A", "", "   1", " ", "ALA");
        Atom a1 = new Atom(" CA ", "C", false);
        r1.add(a1);
        model.add(r1);

        Atom extra = new Atom(" CB ");
        ModelState full = new ModelState();
        full.add(new AtomState(a1, "1"));
        full.add(new AtomState(extra, "2")); // not in model

        ModelState filtered = full.createForModel(model);
        assertTrue(filtered.hasState(a1));
        assertFalse(filtered.hasState(extra));
    }
}
