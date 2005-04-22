// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.*;
//}}}
/**
* <code>KingFoo</code> is a cavity-detection utility based on DCR's "foo" in Mage.
* It fills empty spaces with overlapping balls and then covers them in a dot surface.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Dec  8 15:35:19 EST 2004
*/
public class KingFoo //extends ... implements ...
{
//{{{ Constants
    static final double MAX_ATOM_RADIUS = 2.0;
    /**
    * Offsets from (0,0,0) to neighbor spheres in an FCC lattice.
    * See placeFoosFCC.
    */
    static final Triple[] FCC_OFFSETS = {
        // In plane, clockwise from top right:
        new Triple( 0, 1, 0),
        new Triple( 1, 0, 0),
        new Triple( 1,-1, 0),
        new Triple( 0,-1, 0),
        new Triple(-1, 0, 0),
        new Triple(-1, 1, 0),
        // Above the plane, clockwise from top right:
        new Triple( 0, 0, 1),
        new Triple( 0,-1, 1),
        new Triple(-1, 0, 1),
        // Below the plane, clockwise from top:
        new Triple( 0, 1,-1),
        new Triple( 1, 0,-1),
        new Triple( 0, 0,-1)
    };
    
    static Transform makeFCCtoCartesian()
    {
        // How i affects x, y, z:
        double ix = 1.0;
        double iy = 0.0;
        double iz = 0.0;
        // How j affects x, y, z:
        double jx = Math.cos(Math.toRadians(60));       // 60 deg from equilateral triangle
        double jy = Math.sin(Math.toRadians(60));       // "
        double jz = 0.0;
        // How k affects x, y, z:
        double kx = Math.cos(Math.toRadians(60));       // 0.5 over to reach midpoint of underlying triangle
        double ky = Math.tan(Math.toRadians(30)) / 2;   // Enough up to reach midpoint of underlying triangle
        double kz = Math.sqrt(1 - kx*kx - ky*ky);       // Makes the sphere-sphere distance == 1, given the above
        
        return new Transform().likeMatrix(ix, jx, kx, iy, jy, ky, iz, jz, kz);
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    Collection atomStates;
    double fooRadius, touchDist;
    
    // Only foos that don't hit protein but are "close" go in fooBin, liveFoos
    // Foos in contact with solvent may later be removed from liveFoos but not fooBin
    SpatialBin atomBin, fooBin;
    Collection liveFoos = new LinkedList();
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a new object for calculating foos. This is somewhat resouce-hungry.
    * @param atomStates     a Collection of AtomStates defining the environment
    * @param fooRadius      the size of the foo balls to be placed in cavities
    * @param touchDist      the gap size below which a foo and an atom are considered to be touching
    */
    public KingFoo(Collection atomStates, double fooRadius, double touchDist)
    {
        super();
        this.atomStates = new ArrayList(atomStates);
        this.fooRadius = fooRadius;
        this.touchDist = touchDist;
        
        // Optimum bin width is ___ times the typical search distance
        // Empirically, 1x and 2x are about the same (more bins vs. more points)
        // 1.5x seems to be a bit faster than either one (only ~10%, though)
        this.atomBin = new SpatialBin(1.5 * (MAX_ATOM_RADIUS+fooRadius+touchDist));
        this.fooBin = new SpatialBin(1.5 * (fooRadius));
        
        this.atomBin.addAll(this.atomStates);
    }
//}}}

//{{{ placeFoos
//##############################################################################
    /**
    * Places foo balls randomly in the described bounding box, discarding those
    * that intersect the protein atoms. This should be called before trying to
    * generate a dot surface.
    * <p>This function can be called repeatedly to cumulatively place additional foos.
    * @param numTrials      the number of random trials to conduct
    * @param center         the center of the bounding box
    * @param halfwidths     the halfwidth of the bounding box in X, Y, and Z
    * @return the number of foos successfully placed (CUMULATIVELY)
    */
    public int placeFoos(int numTrials, Triple center, Triple halfwidths)
    {
        double widthX = 2 * (halfwidths.getX() - fooRadius);
        double widthY = 2 * (halfwidths.getX() - fooRadius);
        double widthZ = 2 * (halfwidths.getX() - fooRadius);
        ArrayList hits = new ArrayList();
        
        for(int i = 0; i < numTrials; i++)
        {
            // Create randomly positioned foo
            Triple foo = new Triple(center.getX() + widthX*(Math.random()-0.5),
                center.getY() + widthY*(Math.random()-0.5),
                center.getZ() + widthZ*(Math.random()-0.5));
            
            // Coarse test to find nearby atoms
            hits.clear();
            atomBin.findSphere(foo, MAX_ATOM_RADIUS+fooRadius+touchDist, hits);
            
            // Fine test to distinguish overlaps from contacts
            boolean isDead = false, isTouching = false;
            for(int j = 0; j < hits.size(); j++)
            {
                AtomState as = (AtomState) hits.get(j);
                double realDist = foo.sqDistance(as);
                double deadDist = getVdwRadius(as) + fooRadius;
                double liveDist = deadDist + touchDist;
                deadDist = deadDist * deadDist; // less than this is collision
                liveDist = liveDist * liveDist; // greater than this is not touching
                if(realDist < deadDist)         { isDead        = true; break; }
                else if(realDist <= liveDist)   { isTouching    = true;        }
            }
            
            // Storage of successfully placed foos
            if(!isDead && isTouching)
            {
                fooBin.add(foo);
                liveFoos.add(foo);
            }
        }
        return liveFoos.size();
    }
//}}}

//{{{ placeFoosWithProbe
//##############################################################################
    /**
    * Uses Probe to place foo balls on the accessible surface of non-water atoms.
    * This should be called before trying to generate a dot surface.
    * @return the number of foos successfully placed
    */
    public int placeFoosWithProbe(File pdbFile) throws IOException
    {
        DecimalFormat df = new DecimalFormat("0.0###");
        String cmd = "probe -drop -add"+df.format(fooRadius)+" -r0.0 -density2.0 -u -out 'not water' '"+pdbFile+"'";
        Process proc = Runtime.getRuntime().exec(Strings.tokenizeCommandLine(cmd));
        ProcessTank tank = new ProcessTank();
        tank.fillTank(proc);
        
        String s;
        LineNumberReader in = new LineNumberReader(new InputStreamReader(tank.getStdout()));
        while((s = in.readLine()) != null)
        {
            String[] fields = Strings.explode(s, ':');
            try
            {
                Triple foo = new Triple(Double.parseDouble(fields[14]),
                    Double.parseDouble(fields[15]),
                    Double.parseDouble(fields[16]));
                fooBin.add(foo);
                liveFoos.add(foo);
            }
            catch(NumberFormatException ex) { ex.printStackTrace(); }
        }
        in.close();
        return liveFoos.size();
    }
//}}}

//{{{ placeFoosFCC
//##############################################################################
    /**
    * Place foos on a face-centered cubic (FCC) lattice.
    * FCC lattices (aka cubic closest packed) are one of two closest packed
    * arrangements of spheres, the other one being hexagonal closest packed.
    * Both consist of stacked layers of spheres in a (flat) triangular lattice,
    * with the layers offset to settle in the divots of the layer below.
    * HCP uses an ABABAB pattern, while FCC uses the ABCABC pattern.
    * (A layers lie directly above other A layers, B above B, etc.)
    * FCC is higher symmetry than HCP, or so I hear.
    *
    * <p>Just as grid points on a cubic lattice can be indexed with three
    * integers (i,j,k), so can points on a face-centered cubic lattice.
    * The transformation between FCC "coordinates" and Cartesian ones is a
    * simple affine transform (aka matrix multiplication).
    * The one uses here keeps (0,0,0) and (1,0,0) as themselves, and maintains
    * the handedness of the coordinate system.
    *
    * <p>For more information, see http://www.polymorf.net/matter6.htm
    * and http://mathworld.wolfram.com/CubicClosePacking.html
    *
    * <p><b>The Algorithm</b>
    * <br>The idea is to start with a "seed" foo, and then grow out to its
    * neighbors, and their neighbors, etc etc.
    * At any step, a foo that contacts protein is marked dead and that trail stops.
    * This continues for a specified number of steps.
    * We have to make sure not to re-visit nodes we've already been to, to keep
    * this from expanding exponentially.
    * Depth first search (DFS) is easy to implement recursively, but because we can
    * (and do!) follow very inefficient paths to some nodes, the foo doesn't
    * expand equally in all directions.
    * Instead, we must do Breadth First Search (BFS), which is more irritating,
    * but assures we reach each node for the first time in the minimal number of steps.
    *
    * <p>This should be called before trying to generate a dot surface.
    *
    * @param centerCartn    the desired Cartesian coordinates for the origen.
    * @param numSteps       the numbers of steps out toward neighbors to take,
    *   total. Stop when this hits zero.
    * @return the number of foos successfully placed
    */
    public int placeFoosFCC(Triple centerCartn, double gridSpacing, int numSteps)
    {
        // The transform to get from (i,j,k) to Cartesian coordinates.
        Transform fcc2cartn = makeFCCtoCartesian();
        fcc2cartn.append(new Transform().likeScale(gridSpacing));
        fcc2cartn.append(new Transform().likeTranslation(centerCartn));
        
        // All the ijk's to try as seeds in this round:
        Set centerFCC = new HashSet();
        centerFCC.add(new Triple(0,0,0)); // origen
        // All the ijk indices that have been tried, for good or ill.
        // Don't bother trying them again. Doesn't yet include indices in centerFCC.
        Set triedFCC = new HashSet();
        // Temporary storage for doing spatial searches
        List hits = new ArrayList();

        // At the START of this function:
        // - the foos indexed by ijk's (in centerFCC) have not been tried before
        //   and are not in triedFCC
        // - they may or may not overlap with atoms in the macromolecule
        // - they may or may not need to check their neighbors, depending on numSteps
        while(true) // do numSteps cycles - break is about 2/3 of the way down...
        {
            // Iterate over all "seed" points at the surface of the growing blob of foos. {{{
            Triple foo = new Triple(); // reallocated only when we retain it
            for(Iterator iter = centerFCC.iterator(); iter.hasNext(); )
            {
                Triple ijk = (Triple) iter.next();
                triedFCC.add(ijk); // we know it wasn't already in there; see below
                fcc2cartn.transform(ijk, foo);
                
                // Coarse test to find nearby atoms
                hits.clear();
                atomBin.findSphere(foo, MAX_ATOM_RADIUS+fooRadius+touchDist, hits);
                
                // Fine test to distinguish overlaps from contacts
                boolean isDead = false, isTouching = false;
                for(int j = 0; j < hits.size(); j++)
                {
                    AtomState as = (AtomState) hits.get(j);
                    double realDist = foo.sqDistance(as);
                    double deadDist = getVdwRadius(as) + fooRadius;
                    double liveDist = deadDist + touchDist;
                    deadDist = deadDist * deadDist; // less than this is collision
                    liveDist = liveDist * liveDist; // greater than this is not touching
                    if(realDist < deadDist)         { isDead        = true; break; }
                    else if(realDist <= liveDist)   { isTouching    = true;        }
                }
                
                // Foo clashes with protein - abort this line of exploration.
                if(isDead) { iter.remove(); continue; }
                
                // We only want to try surfacing foos near the protein, but other
                // non-dead foos still need to recruit their neighbors (see below).
                if(isTouching)
                {
                    fooBin.add(foo);
                    liveFoos.add(foo);
                    foo = new Triple(); // allocate a new one for future rounds
                }
            }//}}} end iteration over foos to try in this round
            
            // Do we have more steps? If not, we're done!
            if(--numSteps < 0) break;
            
            // All non-dead foos now contact their neighbors, if we have more steps. {{{
            Set newFCC = new HashSet(); // points to be tried in the NEXT recursion
            Triple neighbor = new Triple(); // reallocated only when we use it
            for(Iterator iter = centerFCC.iterator(); iter.hasNext(); )
            {
                Triple ijk = (Triple) iter.next();
                for(int i = 0; i < FCC_OFFSETS.length; i++)
                {
                    neighbor.like(ijk).add(FCC_OFFSETS[i]);
                    if(triedFCC.contains(neighbor)) continue; // we've been down this road before...
                    
                    newFCC.add(neighbor);
                    neighbor = new Triple(); // allocate a new one for future rounds
                }
            }//}}}
            
            // Rename variables and start from the top!
            centerFCC = newFCC;
        } // end of outer while loop over numSteps rounds...

        return liveFoos.size();
    }
//}}}

//{{{ removeWetFoos
//##############################################################################
    /**
    * Removes any foos that are solvent accessible to water (hence "wet").
    * If a 1.4A radius ball can be placed at any point on the foo surface and
    * not contact a protein atom, that foo is considered "wet" and will be removed.
    * Here's how to test that:
<p><pre>
                        . &lt;== potential water site 
     R_foo  R_water=1.4  .
    +-----+--------------&gt;
                         .
                        .
</pre>
    * <p>That is, create a dot ball centered on the foo with a radius of
    * (R_foo + R_water). Check each of those for protein atom centers within
    * (R_water + R_atom_VDW).
    * @return the number of placed foos REMAINING after removing wet ones.
    */
    public int removeWetFoos()
    { return removeWetFoos(1.4); }
    
    public int removeWetFoos(double waterRadius)
    {
        double totalRadius = fooRadius+waterRadius;
        // Dots projected onto foo surface will be ~ 16/A^2
        // b/c surface grows as square of radius.
        // Should be fine for most applications.
        double density = 16 * (fooRadius/totalRadius) * (fooRadius/totalRadius);
        Collection dotSphere = new Builder().makeDotSphere(totalRadius, density);
        //DEBUG: System.err.println(dotSphere.size()+" wet dots tested per foo");
        
        Triple dot = new Triple(); // avoid creating and discarding Triples
        ArrayList hits = new ArrayList();
        for(Iterator fi = liveFoos.iterator(); fi.hasNext(); )
        {
            Triple foo = (Triple) fi.next();
            for(Iterator di = dotSphere.iterator(); di.hasNext(); )
            {
                // Position dot on sphere surface
                Triple stdDot = (Triple) di.next();
                dot.likeSum(foo, stdDot);
                
                // We don't care if we intersect other foos or not.
                
                // Coarse test to find nearby atoms
                hits.clear();
                atomBin.findSphere(dot, MAX_ATOM_RADIUS+waterRadius, hits);
                
                // Fine test to distinguish overlaps from contacts
                boolean hitAtom = false;
                for(int j = 0; j < hits.size(); j++)
                {
                    AtomState as = (AtomState) hits.get(j);
                    String rName = as.getResidue().getName();
                    if(rName.equals("HOH") || rName.equals("H2O") || rName.equals("WAT"))
                        continue; // don't count hits to water!
                    double realDist = dot.sqDistance(as);
                    double deadDist = getVdwRadius(as) + waterRadius;
                    deadDist = deadDist * deadDist; // less than this is collision
                    if(realDist < deadDist) { hitAtom = true; break; }
                }
                
                // If we didn't hit any atoms, this is a solvent-accessible dot.
                if(!hitAtom)
                {
                    fi.remove();
                    break; // go on to next foo, skip remaining dots
                }
            }
        }
        return liveFoos.size();
    }
//}}}

//{{{ surfaceFoos
//##############################################################################
    /**
    * Creates a dot surface over the successfully placed foos.
    * placeFoos() must be called before calling this function.
    * @param dotDensity     density of dot placement, in dots per square unit area.
    * @return a Collection of Triples representing the dot surface
    */
    public Collection surfaceFoos(double dotDensity)
    {
        Collection dotSphere = new Builder().makeDotSphere(fooRadius, dotDensity);
        Collection dotSurface = new ArrayList();
        Triple dot = new Triple(); // avoid creating and discarding Triples
        ArrayList hits = new ArrayList();
        for(Iterator fi = liveFoos.iterator(); fi.hasNext(); )
        {
            Triple foo = (Triple) fi.next();
            for(Iterator di = dotSphere.iterator(); di.hasNext(); )
            {
                // Position dot on sphere surface
                Triple stdDot = (Triple) di.next();
                dot.likeSum(foo, stdDot);
                
                // Make sure this foo dot isn't inside some other foo
                // (Except that this dot could be inside this foo, due to roundoff)
                // 0.99 is b/c closely packed foos may expose so little surface
                // that no dots happen to fall on it -- this helps make surface smooth.
                hits.clear();
                fooBin.findSphere(dot, 0.99*fooRadius, hits);
                if(hits.size() > 1 || (hits.size() == 1 && hits.get(0) == foo))
                    continue;
                
                // Coarse test to find nearby atoms
                hits.clear();
                atomBin.findSphere(dot, MAX_ATOM_RADIUS+touchDist, hits);
                
                // Fine test to distinguish overlaps from contacts
                boolean isDead = false, isTouching = false;
                for(int j = 0; j < hits.size(); j++)
                {
                    AtomState as = (AtomState) hits.get(j);
                    double realDist = dot.sqDistance(as);
                    double deadDist = getVdwRadius(as);
                    double liveDist = deadDist + touchDist;
                    deadDist = deadDist * deadDist; // less than this is collision (shouldn't happen)
                    liveDist = liveDist * liveDist; // greater than this is not touching
                    if(realDist < deadDist)         { isDead        = true; break; }
                    else if(realDist <= liveDist)   { isTouching    = true;        }
                }
                
                // No dot should collide with an atom, except rarely due to roundoff
                if(isTouching && !isDead)
                {
                    dotSurface.add(dot);
                    dot = new Triple(); // only replace "used" dots
                }
            }
        }
        return dotSurface;
    }
//}}}

//{{{ getVdwRadius
//##############################################################################
    /**
    * Returns an approximate van der Waals radius for common atoms in proteins.
    * Based on Mike Word's Probe code (atomprops.h), but far less complete.
    */
    double getVdwRadius(AtomState as)
    {
        final double dummyVal = 0.0;
        String name = as.getName();
        
        if(Character.isLetter(name.charAt(0)))  return dummyVal;
        else if(name.charAt(1) == 'H')          return 1.17;
        else if(name.charAt(1) == 'C')          return 1.75;
        else if(name.charAt(1) == 'O')          return 1.40;
        else if(name.charAt(1) == 'N')          return 1.55;
        else if(name.charAt(1) == 'P')          return 1.80;
        else if(name.charAt(1) == 'S')          return 1.80;
        else                                    return dummyVal;
    }
//}}}

//{{{ getFoos
//##############################################################################
    /**
    * Returns an unmodifiable Collection of Triples marking the centers of all
    * successfully placed foos (excluding any removed by removeWetFoos()).
    */
    public Collection getFoos()
    {
        return Collections.unmodifiableCollection(liveFoos);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ main (for testing)
//##############################################################################
    static public void main(String[] args) throws IOException, NumberFormatException
    {
        Builder b = new Builder();
        DecimalFormat df = new DecimalFormat("0.0###");
        File inputFile = null;
        
        for(int i = 0; i < args.length; i++)
        {
            if(inputFile == null) inputFile = new File(args[i]);
            else System.err.println("Unrecognized argument "+args[i]);
        }
        
        final double fooRadius = 1.0;
        Model m = new PdbReader().read(inputFile).getFirstModel();
        Collection atoms = new LinkedList(m.getState().createCollapsed().getLocalStateMap().values());
        // Remove HET atoms:
        for(Iterator iter = atoms.iterator(); iter.hasNext(); )
        {
            AtomState as = (AtomState) iter.next();
            if(as.isHet()) iter.remove();
        }
        
        long time = System.currentTimeMillis();
        KingFoo kf = new KingFoo(atoms, fooRadius, fooRadius/1.0);
        //DEBUG: kf.placeFoosFCC(new Triple(), 2.0, 2);
        kf.placeFoosFCC(new Triple(81.574, 28.956, 85.759), 0.4, 40);
        time = System.currentTimeMillis() - time;
        System.err.println(kf.getFoos().size()+" foos were placed successfully in "+time+" ms");
        
        time = System.currentTimeMillis();
        kf.removeWetFoos(3.0);
        time = System.currentTimeMillis() - time;
        System.err.println(kf.getFoos().size()+" dry foos remaining after "+time+" ms");
        
        System.out.println("@kinemage 1");
        System.out.println("@group {foo cavities}");
        System.out.println("@subgroup {foo cavities}");
        System.out.println("@balllist {foo balls} radius= "+df.format(fooRadius)+" color= pink off nohighlight alpha= 1.0");
        for(Iterator iter = kf.getFoos().iterator(); iter.hasNext(); )
            System.out.println("{x} "+((Triple)iter.next()).format(df));
        
        /**/
        time = System.currentTimeMillis();
        Collection dotSurface = kf.surfaceFoos(16);
        time = System.currentTimeMillis() - time;
        System.err.println(dotSurface.size()+" dots placed in "+time+" ms");
        
        System.out.println("@dotlist {foo dots} color= gray");
        for(Iterator iter = dotSurface.iterator(); iter.hasNext(); )
            System.out.println("{x} "+((Triple)iter.next()).format(df));
        /**/
    }
//}}}
}//class

