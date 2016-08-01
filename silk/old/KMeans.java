// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package silk;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>KMeans</code> has not yet been documented.
*

java -cp lib/silk.jar silk.SilkCmdLine -ndim=2 -label=1 -coords=2,3 -wrap -insep=: -gridsize=2 -cosine=5 -twopass=6.5   -bounds=-180,180,-180,180 -fraction -title="Top500 General case (not Gly, Pro, or pre-Pro) B<30" -kin < srcdata/rama500-general-noGPpreP.tab > kin/rama/rama500-general.kin
java -cp lib/silk.jar silk.SilkCmdLine -ndim=2 -label=1 -coords=2,3 -wrap -insep=: -gridsize=2 -cosine=5 -twopass=8 -bounds=-180,180,-180,180 -fraction -title="Top500 Alanine (no repet sec struct) B<30" -kin < srcdata/rama500-ala-nosec.tab >  kin/rama/rama500-ala-nosec.kin

* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Dec  4 10:33:47 EST 2006
*/
public class KMeans //extends ... implements ...
{
//{{{ Constants
static final String[] COLORS = { "red", "orange", "gold", "yellow", "lime",
    "green", "sea", "cyan", "sky", "blue", "purple", "magenta", "hotpink",
    "pink", "lilac", "pinktint", "peachtint", "yellowtint", "greentint",
    "bluetint", "lilactint", "white", "gray", "brown" };
//}}}

//{{{ CLASS: Cluster
//##############################################################################
    class Cluster
    {
        double[]    coords;
        double      strength = 1;
        ArrayList   members = new ArrayList();
        
        public Cluster(DataSample data)
        {
            this.coords = (double[]) data.coords.clone();
        }
        
        public double dist(DataSample data)
        { return this.dist(data.coords); }
        
        public double dist(double[] data_coords)
        {
            double dist = 0;
            for(int i = 0; i < coords.length; i++)
            {
                double diff = this.coords[i] - data_coords[i];
                // XXX this seems buggy
                //if(options.wrap[i])
                //{
                //    double min = options.bounds[2*i];
                //    double max = options.bounds[2*i + 1];
                //    double span = max - min;
                //    diff = Math.abs(diff) % span;
                //    if(diff > span/2) diff = span - diff;
                //}
                dist += diff*diff;
            }
            return dist * strength; // Math.sqrt(dist * strength);
        }
        
        public void add(DataSample data)
        { members.add(data); }
        
        public ArrayList getMembers()
        { return members; }
        
        public int size()
        { return members.size(); }
        
        public double startNewCycle()
        {
            double[] oldCoords = (double[]) coords.clone();
            if(members.size() > 0) for(int i = 0; i < coords.length; i++)
            {
                coords[i] = 0;
                for(Iterator iter = members.iterator(); iter.hasNext(); )
                    coords[i] += ((DataSample) iter.next()).coords[i];
                coords[i] /= members.size();
            }
            // must be averaged with previous estimate of strength
            // or it's easy to fall into giant cluster - tiny cluster cycles
            if(balanceClusters) strength = (Math.pow(members.size()+1.0, 2.0/options.nDim) + strength) / 2.0;
            else                strength = 1;
            members.clear();
            
            return dist(oldCoords);
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    SilkOptions options;
    final boolean balanceClusters = false;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KMeans(SilkOptions options)
    {
        super();
        this.options = options;
    }
//}}}

//{{{ findClusters
//##############################################################################
    public void findClusters(Collection dataSamples, int numClusters, KinfilePlotter plotter)
    {
        ArrayList samples = new ArrayList(dataSamples); // so we don't change it
        Collections.shuffle(samples);

        Cluster[] clusters = new Cluster[numClusters];
        for(int k = 0; k < numClusters; k++)
            clusters[k] = new Cluster( (DataSample) samples.get(k) );
        
        // This causes clusters to chase each other in circles, plus weird overlaps
        int currClusterSizeLimit = samples.size();
        int targetClusterSizeLimit = (int) Math.ceil(((double)samples.size()) / ((double)numClusters));
        //currClusterSizeLimit = targetClusterSizeLimit;
        
        for(int i = 0; i < 300; i++) // need a better stopping criterion
        {
            // This was supposed to escape local minima, but it just makes things unstable {{{
            if(i != 0 && i % 10 == 0 && i < 200)
            {
                int minSize = samples.size()+1, maxSize = -1;
                int minIndex = 0, maxIndex = 0;
                for(int k = 0; k < numClusters; k++)
                {
                    int size =  clusters[k].size();
                    if(size < minSize)
                    {
                        minSize = size;
                        minIndex = k;
                    }
                    if(size > maxSize)
                    {
                        maxSize = size;
                        maxIndex = k;
                    }
                }
                ArrayList members = clusters[maxIndex].getMembers();
                DataSample seed = (DataSample) members.get( (int)Math.floor( members.size() * Math.random() ) );
                clusters[minIndex] = new Cluster(seed);
            }
            //}}}
            
            double clustersMoved = 0;
            for(int k = 0; k < numClusters; k++)
            {
                clustersMoved += clusters[k].startNewCycle();
                System.err.print("\rk-means clustering; pass "+i+"; size cap "+currClusterSizeLimit+"; clusters moved = "+clustersMoved);
            }
            
            for(Iterator iter = samples.iterator(); iter.hasNext(); )
            {
                DataSample data = (DataSample) iter.next();
                double  closestDist     = Double.POSITIVE_INFINITY;
                Cluster closestCluster  = null;
                for(int k = 0; k < numClusters; k++)
                {
                    double dist = clusters[k].dist(data);
                    if(dist < closestDist && clusters[k].size() < currClusterSizeLimit)
                    {
                        closestCluster = clusters[k];
                        closestDist = dist;
                    }
                }
                closestCluster.add(data);
            }

            if(i < 10 || i % 10 == 0)
            {
                for(int k = 0; k < numClusters; k++)
                {
                    for(Iterator iter = clusters[k].getMembers().iterator(); iter.hasNext(); )
                    {
                        DataSample data = (DataSample) iter.next();
                        data.color = COLORS[ k % COLORS.length ];
                        data.label = "Cluster "+k+", "+clusters[k].size()+" members";
                    }
                }
                plotter.plot2D(options.outputSink, samples);
            }
            
            //if(currClusterSizeLimit > targetClusterSizeLimit)
            //    currClusterSizeLimit = (int) Math.max(0.9 * currClusterSizeLimit, targetClusterSizeLimit);
        }
System.err.println();
        
        for(int k = 0; k < numClusters; k++)
System.err.println("Cluster "+k+" has "+clusters[k].size()+" members");
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

