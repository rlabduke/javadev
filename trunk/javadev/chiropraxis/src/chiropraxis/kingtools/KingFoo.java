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
    {
        final double waterRadius = 1.4;
        double totalRadius = fooRadius+waterRadius;
        // Dots projected onto foo surface will be at least 16/A^2
        // b/c surface grows as square of radius
        double density = 3;//Math.max(4, 16 * (fooRadius/totalRadius) * (fooRadius/totalRadius));
        Collection dotSphere = new Builder().makeDotSphere(totalRadius, density);
        
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
        Collection atoms = m.getState().createCollapsed().getLocalStateMap().values();
        
        long time = System.currentTimeMillis();
        KingFoo kf = new KingFoo(atoms, fooRadius, fooRadius/1.0);
        kf.placeFoosWithProbe(inputFile);
        time = System.currentTimeMillis() - time;
        System.err.println(kf.getFoos().size()+" foos were placed successfully in "+time+" ms");
        
        time = System.currentTimeMillis();
        kf.removeWetFoos();
        time = System.currentTimeMillis() - time;
        System.err.println(kf.getFoos().size()+" dry foos remaining after "+time+" ms");
        
        System.out.println("@kinemage 1");
        System.out.println("@group {foo cavities}");
        System.out.println("@subgroup {foo cavities}");
        System.out.println("@balllist {foo balls} radius= "+df.format(fooRadius)+" color= sea nohighlight alpha= 1.0");
        for(Iterator iter = kf.getFoos().iterator(); iter.hasNext(); )
            System.out.println("{x} "+((Triple)iter.next()).format(df));
        
        /*time = System.currentTimeMillis();
        Collection dotSurface = kf.surfaceFoos(16);
        time = System.currentTimeMillis() - time;
        System.err.println(dotSurface.size()+" dots placed in "+time+" ms");
        
        System.out.println("@dotlist {foo dots} color= purple off");
        for(Iterator iter = dotSurface.iterator(); iter.hasNext(); )
            System.out.println("{x} "+((Triple)iter.next()).format(df));
        */
    }
//}}}
}//class

