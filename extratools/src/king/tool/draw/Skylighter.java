// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.draw;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
import driftwood.data.CheapSet;
//}}}
/**
* <code>Skylighter</code> does "fake skylight" calculations for a collection of spheres.
* This can be used e.g. to enhance rendering of space-filling molecular models, ala QuteMol.
* I think this is called "ambient occlusion" in QuteMol; ie the occlusion of ambient (sky) light.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Nov  2 15:26:11 EST 2006
*/
public class Skylighter //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    // TODO: this should be determined on the fly or specified by user
    final double gridSpacing = 0.25; // 0.125 is slightly nicer but takes 4x as long

    Transform[] transforms;    
    Tuple3[]    origCenters;
    double[]    radii;
    
    Triple[]    centers; // transformed by one of the tranforms[]
    double[]    hits; // number of rays hit
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Skylighter(Tuple3[] sphereCenters, double[] sphereRadii, Transform[] rotations)
    {
        super();
        if(sphereCenters.length != sphereRadii.length)
            throw new IllegalArgumentException("Number of sphere centers and radii must match");
        
        this.origCenters    = sphereCenters;
        this.radii          = sphereRadii;
        this.transforms     = rotations;
        
        this.centers = new Triple[ sphereCenters.length ];
        for(int i = 0; i < centers.length; i++) centers[i] = new Triple();
        
        this.hits = new double[ centers.length ]; // all start at zero
        
        this.calculate();
    }
//}}}

//{{{ calculate
//##############################################################################
    private void calculate()
    {
        // When trying to find the set of spheres hit by light,
        // we put their Integer indexes into a Set.
        // To avoid re-creating these objects many times:
        Integer[] sphereInts = new Integer[origCenters.length];
        for(int i = 0; i < origCenters.length; i++) sphereInts[i] = new Integer(i);
        
        // We always cast light rays from +Z toward -Z.
        // For each provided transform, created a rotated version of the sphere scene.
        for(int j = 0; j < transforms.length; j++)
        {
            // Rotate sphere locations:
            Transform T = transforms[j];
            for(int i = 0; i < centers.length; i++)
                T.transform(origCenters[i], centers[i]);
            // Determine X-Y extent of transformed scene.
            // Project light rays on a regular grid in this region.
            Triple[] bounds = Builder.makeBoundingBox(Arrays.asList(centers), radii);
            Triple min = bounds[0], max = bounds[1];
            double xWidth = (max.getX() - min.getX()), yWidth = (max.getY() - min.getY());
            // For each light ray, track which sphere is first to intercept it (frontmost sphere).
            int[][] frontSphereIndex = new int[ (int)Math.ceil(xWidth/gridSpacing) ][ (int)Math.ceil(yWidth/gridSpacing) ];
            for(int ix = 0; ix < frontSphereIndex.length; ix++)
                for(int iy = 0; iy < frontSphereIndex[ix].length; iy++)
                    frontSphereIndex[ix][iy] = -1; // flag for no entry
            // Cycle through spheres and see which light ray(s) hit them.
            for(int iSphere = 0; iSphere < centers.length; iSphere++)
            {
                // Test against all rays in the box bounding this sphere.
                Triple ctr = centers[iSphere];
                double rad = radii[iSphere];
                int min_ix = (int)Math.ceil((ctr.getX() - rad - min.getX()) / gridSpacing);
                int min_iy = (int)Math.ceil((ctr.getY() - rad - min.getY()) / gridSpacing);
                int max_ix = (int)Math.floor((ctr.getX() + rad - min.getX()) / gridSpacing);
                int max_iy = (int)Math.floor((ctr.getY() + rad - min.getY()) / gridSpacing);
                // unusual formating for two-level loop:
                for(int ix = min_ix; ix <= max_ix; ix++)
                for(int iy = min_iy; iy <= max_iy; iy++)
                {
                    double dX = (min.getX() + ix*gridSpacing) - ctr.getX();
                    double dY = (min.getY() + iy*gridSpacing) - ctr.getY();
                    int prevFrontIndex = frontSphereIndex[ix][iy];
                    if(prevFrontIndex == -1
                    || (ctr.getZ() > centers[prevFrontIndex].getZ() && dX*dX + dY*dY <= rad*rad))
                        frontSphereIndex[ix][iy] = iSphere;
                }
            }//end: for-all-spheres
            // Credit each sphere that was hit by 1+ light beams.
            // Each sphere is either hit or not -- no bonus for multiple hits,
            // b/c they have varying sizes/areas, which may be occluded by overlaps too.
            CheapSet hitSphereIndexes = new CheapSet();
            // unusual formating for two-level loop:
            for(int ix = 0; ix < frontSphereIndex.length; ix++)
            for(int iy = 0; iy < frontSphereIndex[ix].length; iy++)
            {
                int iSphere = frontSphereIndex[ix][iy];
                if(iSphere != -1) hitSphereIndexes.add( sphereInts[iSphere] );
            }
            for(Iterator iter = hitSphereIndexes.iterator(); iter.hasNext(); )
            {
                int iSphere = ((Integer) iter.next()).intValue();
                hits[iSphere] += 1;
            }
        }//end: for-each-transform
        // Normalize hits to 0-to-1 scale.
        //double maxHits = 0;
        //for(int i = 0; i < hits.length; i++) maxHits = Math.max(maxHits, hits[i]);
        double maxHits = transforms.length;
        if(maxHits > 0)
            for(int i = 0; i < hits.length; i++) hits[i] /= maxHits;
    }
//}}}

//{{{ getLighting
//##############################################################################
    /**
    * For each input sphere, returns a number between 0 and 1 indicating
    * how much "ambient" skylight is hitting the object.
    */
    public double[] getLighting()
    { return hits; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

