// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.forcefield;

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
* <code>NonbondedTest</code> has not yet been documented.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul 14 10:37:30 EDT 2004
*/
public class NonbondedTest //extends ... implements ...
{
    public static void main(String[] args)
    {
        runTest(100, 10000, 6.0);
        runTest(200, 10000, 6.0);
        runTest(300, 10000, 6.0);
        runTest(1000, 1000, 6.0);
        runTest(10000, 10, 6.0);
        runTest(100, 10000, 8.0);
        runTest(1000, 1000, 8.0);
        runTest(10000, 10, 8.0);
    }
    
    static void runTest(int nPoints, int nRuns, double cutoff)
    {
        double[] state = new double[nPoints * 3];
        // Av. diam. times spheres packed into a cube.
        double coeff = 3.5 * Math.pow(nPoints, 1.0/3.0);
        for(int i = 0; i < state.length; i++) state[i] = coeff * Math.random();
        int pairs = -1;
        
        SimpleNonbondedTerm simple = new SimpleNonbondedTerm(cutoff);
        long simpleTime = System.currentTimeMillis();
        for(int i = 0; i < nRuns; i++)
        {
            pairs = simple.countInteractions(state);
        }
        simpleTime = System.currentTimeMillis() - simpleTime;
        System.err.println("Simple: "+pairs+" pairs, "+nPoints+" points, within "
            +cutoff+" A: "+((float)simpleTime / (float)nRuns)+" ms (N="+nRuns+")");
        
        FancyNonbondedTerm fancy = new FancyNonbondedTerm(cutoff, 10);
        long fancyTime = System.currentTimeMillis();
        for(int i = 0; i < nRuns; i++)
        {
            pairs = fancy.countInteractions(state);
        }
        fancyTime = System.currentTimeMillis() - fancyTime;
        System.err.println("Fancy : "+pairs+" pairs, "+nPoints+" points, within "
            +cutoff+" A: "+((float)fancyTime / (float)nRuns)+" ms (N="+nRuns+")");
        
    }
}//class

