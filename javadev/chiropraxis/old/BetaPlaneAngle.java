// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.mc;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
//}}}
/**
* <code>BetaPlaneAngle</code> uses LsqPlane to find the angle
* between the Ca-Cb vector and the plane of the beta sheet
* for every beta-sheet residue in a structure.
* Feed a PDB on stdin. Get a kinemage on stdout.
*
* <p>The algorithm is as follows. Beta sheets are defined by the PDB SHEET records.
* For every residue in a sheet, the 8 Ca closest to it's own Ca are found.
* Those 9 points are used to define the normal of a least-squares plane.
* The angle of the residue to the plane is then taken to be 90 degrees minus
* the angle between the Ca-Cb vector and the plane normal vector.
*
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 18 11:07:12 EST 2004
*/
public class BetaPlaneAngle //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ class: Sortable
//##############################################################################
    /** For sorting CA's by distance from the target residue */
    static class Sortable implements Comparable
    {
        AtomState as;
        double dist;
        
        public Sortable(AtomState as, double dist)
        {
            this.as = as;
            this.dist = dist;
        }
        
        public int compareTo(Object o)
        {
            Sortable that = (Sortable)o;
            if(this.dist < that.dist) return -1;
            else if(this.dist > that.dist) return 1;
            else return (this.hashCode() - that.hashCode());
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public BetaPlaneAngle()
    {
        super();
    }
//}}}

//{{{ processModel
//##############################################################################
    void processModel(Model model, ModelState state, Collection headers)
    {
        DecimalFormat df = new DecimalFormat("0.00");
        Collection calphas = extractCalphas(model, state);
        Map sheets = PdbReader.extractSheetDefinitions(headers, model);
        for(Iterator iter1 = sheets.keySet().iterator(); iter1.hasNext(); )
        {
            String sheetID = (String) iter1.next();
            Set sheet = (Set) sheets.get(sheetID);
            for(Iterator iter2 = sheet.iterator(); iter2.hasNext(); )
            {
                try
                {
                    Residue target = (Residue) iter2.next();
                    AtomState ca = state.get(target.getAtom(" CA "));
                    AtomState cb = state.get(target.getAtom(" CB "));
                    Collection nearest = find9Nearest(ca, calphas);
                    
                    boolean allInSheet = true;
                    /* This requirement is too restrictive?
                    for(Iterator iter3 = nearest.iterator(); iter3.hasNext(); )
                    {
                        AtomState as = (AtomState) iter3.next();
                        if(! sheet.contains(as.getResidue()))
                        {
                            allInSheet = false;
                            //System.err.println(target+" failed because "+as.getResidue()+" wasn't in the same sheet.");
                        }
                    }
                    */
                    
                    if(allInSheet)
                    {
                        LsqPlane plane = new LsqPlane();
                        plane.fitPlane(nearest);
                        Triple cacb = new Triple(cb).sub(ca);
                        double angle = Math.abs(90.0 - cacb.angle(plane.getNormal()));
                        System.err.println("'"+target.getCNIT()+"','"+df.format(angle)+"'");
                        System.out.println("@group {"+target+"} dominant animate");
                        System.out.println("@balllist {plane points} radius= 0.2 color= orange");
                        AtomState as = null;
                        for(Iterator iter3 = nearest.iterator(); iter3.hasNext(); )
                        {
                            as = (AtomState) iter3.next();
                            System.out.println("{"+as.getAtom()+"} "+df.format(as.getX())+" "+df.format(as.getY())+" "+df.format(as.getZ()));
                        }
                        System.out.println("{"+ca.getAtom()+"} r=0.3 "+df.format(ca.getX())+" "+df.format(ca.getY())+" "+df.format(ca.getZ()));
                        
                        System.out.println("@labellist {angle} color= lime");
                        System.out.println("{"+df.format(angle)+"} "+df.format(ca.getX())+" "+df.format(ca.getY())+" "+df.format(ca.getZ()));
                        
                        System.out.println("@vectorlist {plane} color= peachtint width= 1");
                        Builder builder = new Builder();
                        Triple cntr = new Triple(plane.getAnchor());
                        Triple norm = new Triple(cntr).add(plane.getNormal());
                        for(int i = 0; i < 360; i+= 5)
                        {
                            System.out.println("{"+df.format(angle)+"}P "+df.format(cntr.getX())+" "+df.format(cntr.getY())+" "+df.format(cntr.getZ()));
                            Triple end = builder.construct4(as, norm, cntr, as.distance(ca), 90, i);
                            System.out.println("{"+df.format(angle)+"} "+df.format(end.getX())+" "+df.format(end.getY())+" "+df.format(end.getZ()));
                        }
                    }
                }
                catch(AtomException ex) {} // probably a Gly
            }
        }
    }
//}}}

//{{{ extractCalphas, find9Nearest
//##############################################################################
    Collection extractCalphas(Model model, ModelState state)
    {
        ArrayList cas = new ArrayList();
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue) iter.next();
            try { cas.add(state.get(r.getAtom(" CA "))); }
            catch(AtomException ex) {} // non-protein residue
        }
        return cas;
    }

    /**
    * Find the 9 atoms closest to target (including itself) that belong the same chain.
    * I originally wanted 9, but 5 works pretty reliably using just chain ID and distance.
    * Getting the correct set of 9 would require analyzing strand by strand...
    * Still 9 might be better than 5 because outliers have less effect.
    */
    Collection find9Nearest(AtomState target, Collection scatter)
    {
        ArrayList sorted = new ArrayList();
        for(Iterator iter = scatter.iterator(); iter.hasNext(); )
        {
            AtomState s = (AtomState) iter.next();
            if(s.getResidue().getChain() == target.getResidue().getChain())
                sorted.add( new Sortable(s, s.distance(target)) );
        }
        Collections.sort(sorted);
        
        ArrayList nearest = new ArrayList();
        for(Iterator iter = sorted.subList(0, 9).iterator(); iter.hasNext(); )
            nearest.add(((Sortable)iter.next()).as);
        
        return nearest;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws Exception
    {
        // Load model group from PDB files
        PdbReader pdbReader = new PdbReader();
        ModelGroup mg = pdbReader.read(System.in);
        
        System.out.println("@kinemage 1");
        
        Model m = mg.getFirstModel();
        ModelState state = m.getState();
        processModel(m, state, mg.getHeaders());
    }

    public static void main(String[] args)
    {
        BetaPlaneAngle mainprog = new BetaPlaneAngle();
        try
        {
            mainprog.Main();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
//}}}
}//class

