// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.backrub;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb.*;
import driftwood.r3.*;
import chiropraxis.rama.*;
//}}}
/**
* <code>TryTrio</code> has not yet been documented.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Mar 18 16:58:48 EST 2003
*/
public class TryTrio //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public TryTrio()
    {
    }
//}}}

//{{{ findResidueByKinemageID
//##################################################################################################
    /**
    * Searches a model to find the residue named in a kinemage point ID.
    * The ID follows the format used by Prekin:
    * {AAAAaTTT C N+  B##.##}
    *  012345678901234567890
    * where A is the PDB atom ID, a is the alternate conformation code,
    * T is the residue type, C is the chain ID, N is the residue number
    * and insertion code (one or more digits) and the last field is the B factor.
    * @throws NoSuchElementException if the residue can't be found
    */
    public Residue findResidueByKinemageID(Model model, String id)
    {
        try
        {
            String resType = id.substring(5,8).trim().toUpperCase();
            int endOfNum = id.indexOf(' ',11);
            String resNum = id.substring(11, endOfNum).trim();
            String segID = id.substring(9,10); // chain ID

            Segment seg = model.getSegment(segID);
            Residue res = seg.getResidue(resType+resNum);
            return res;
        }
        catch(IndexOutOfBoundsException ex)
        {
            NoSuchElementException ex2 = new NoSuchElementException("{"+id+"} not found");
            ex2.initCause(ex);
            throw ex2;
        }
    }
//}}}

//{{{ transformAtoms
//##################################################################################################
    public void transformAtoms(Transform t, Atom[] atoms)
    {
        for(int i = 0; i < atoms.length; i++)
        {
            t.transform(atoms[i]);
        }
    }
//}}}

//{{{ makeFullArray
//##################################################################################################
    public Atom[] makeFullArray(AABackbone m1, AABackbone ctr, AABackbone p1)
    {
        ArrayList atoms = new ArrayList();
        //atoms.add(m1.N);
        atoms.add(m1.CA);
        atoms.add(m1.C);
        atoms.add(m1.O);
        //if(m1.H != null) atoms.add(m1.H);
        if(m1.HA != null) atoms.add(m1.HA);
        if(m1.HA1 != null) atoms.add(m1.HA1);
        if(m1.HA2 != null) atoms.add(m1.HA2);
        if(m1.CB != null) atoms.add(m1.CB);
        atoms.add(ctr.N);
        atoms.add(ctr.CA);
        atoms.add(ctr.C);
        atoms.add(ctr.O);
        if(ctr.H != null) atoms.add(ctr.H);
        if(ctr.HA != null) atoms.add(ctr.HA);
        if(ctr.HA1 != null) atoms.add(ctr.HA1);
        if(ctr.HA2 != null) atoms.add(ctr.HA2);
        if(ctr.CB != null) atoms.add(ctr.CB);
        atoms.add(p1.N);
        atoms.add(p1.CA);
        //atoms.add(p1.C);
        //atoms.add(p1.O);
        if(p1.H != null) atoms.add(p1.H);
        if(p1.HA != null) atoms.add(p1.HA);
        if(p1.HA1 != null) atoms.add(p1.HA1);
        if(p1.HA2 != null) atoms.add(p1.HA2);
        if(p1.CB != null) atoms.add(p1.CB);
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
//}}}

//{{{ makeArray1, makeArray2
//##################################################################################################
    public Atom[] makeArray1(AABackbone m1, AABackbone ctr)
    {
        ArrayList atoms = new ArrayList();
        atoms.add(m1.CA);
        atoms.add(m1.C);
        atoms.add(m1.O);
        atoms.add(ctr.N);
        atoms.add(ctr.CA);
        if(ctr.H != null) atoms.add(ctr.H);
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
    
    public Atom[] makeArray2(AABackbone ctr, AABackbone p1)
    {
        ArrayList atoms = new ArrayList();
        atoms.add(ctr.CA);
        atoms.add(ctr.C);
        atoms.add(ctr.O);
        atoms.add(p1.N);
        atoms.add(p1.CA);
        if(p1.H != null) atoms.add(p1.H);
        
        return (Atom[])atoms.toArray(new Atom[atoms.size()]);
    }
//}}}

//{{{ calcTauRMSD
//##################################################################################################
    /**
    * Returns the RMS deviation of tau from ideal (in degrees) for the given two-peptide region.
    */
    public double calcTauRMSD(AABackbone m1, AABackbone ctr, AABackbone p1)
    {
        //TODO: report in sigma?
        // stddev(18aa) = 2.8
        // stddev(Gly ) = 2.9
        // stddev(Pro ) = 2.5
        double ideal, dev1, dev2, dev3;
        
        String type = m1.getResidue().getType();
        if(type.equals("GLY"))      ideal = 112.5;
        else if(type.equals("PRO")) ideal = 111.8;
        else                        ideal = 111.2;
        dev1 = ideal - m1.getTau();
        
        type = ctr.getResidue().getType();
        if(type.equals("GLY"))      ideal = 112.5;
        else if(type.equals("PRO")) ideal = 111.8;
        else                        ideal = 111.2;
        dev2 = ideal - ctr.getTau();
        
        type = p1.getResidue().getType();
        if(type.equals("GLY"))      ideal = 112.5;
        else if(type.equals("PRO")) ideal = 111.8;
        else                        ideal = 111.2;
        dev3 = ideal - p1.getTau();
        
        return Math.sqrt( (dev1*dev1 + dev2*dev2 + dev3*dev3) / 3.0 );
    }
//}}}

//{{{ Main() and main()
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main(String[] args) throws NumberFormatException, IOException
    {
        // Parse arguments: pdbfile residue angle
        String pdbFilename = args[0];
        PDBFile pdb = new PDBFile();
        pdb.read(pdbFilename);
        Model model = pdb.getFirstModel();
        
        String resName = args[1];
        Residue res = findResidueByKinemageID(model, resName);
        
        double theta    = Double.parseDouble(args[2]);
        
        // Create data structures
        AABackbone m1, ctr, p1;
        m1  = new AABackbone(res.getPrev());
        ctr = new AABackbone(res);
        p1  = new AABackbone(res.getNext());
        Collection backbone = Arrays.asList( new AABackbone[] {m1, ctr, p1});
        
        Ramachandran rama = new Ramachandran();
        Backrub rubadub = new Backrub(res, rama);
        Backrub.Constraints bc = new Backrub.Constraints();
        bc.majorAngle = theta;
        bc.minor1Start  = -180;
        bc.minor1End    = 180;
        bc.minor1Step   = 1;
        bc.minor2Start  = -180;
        bc.minor2End    = 180;
        bc.minor2Step   = 1;
        
        // Plot, transform, plot again
        KinfilePlotter plot = new KinfilePlotter(System.out, false);
        plot.plotBackbone(backbone);
        System.err.println("Worst original tau deviation = "+rubadub.getWorstTauDev(m1, ctr, p1)+" degrees");
        
        long time = System.currentTimeMillis();
        rubadub.optimizeConformation(bc);
        time = System.currentTimeMillis() - time;
        
        backbone = rubadub.makeConformation(bc.majorAngle, bc.minor1Best, bc.minor2Best);
        Iterator iter = backbone.iterator();
        m1  = (AABackbone)iter.next();
        ctr = (AABackbone)iter.next();
        p1  = (AABackbone)iter.next();
        plot.plotBackbone(backbone);
        System.err.println("Worst modified tau deviation = "+rubadub.getWorstTauDev(m1, ctr, p1)+" degrees");
        System.err.println("Elapsed time "+time+" ms");
    }

    public static void main(String[] args)
    {
        TryTrio mainprog = new TryTrio();
        try
        {
            mainprog.Main(args);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.err.println();
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}
}//class

