// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

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
//}}}
/**
* <code>Ribbons</code> contains functions used to make ribbon drawings.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 23 12:43:49 EST 2006
*/
public class Ribbons //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public Ribbons()
    {
        super();
    }
//}}}

//{{{ getProteinContigs
//##############################################################################
    /**
    * Given an ordered collection of Residues and a ModelState, return one or
    * more ordered collections of Residues that are contiguous (bonded).
    * @return Collection&lt;Collection&ltResidue&gt;&gt;
    */
    public Collection getProteinContigs(Collection residues, ModelState state, ResClassifier resC)
    {
        final double maxCaCa = 5.0; // from Prekin; ideal is 3.80
        ArrayList allContigs = new ArrayList(), currContig = new ArrayList();
        Residue prevRes = null;
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue currRes = (Residue) iter.next();
            if(resC.classify(currRes) != ResClassifier.PROTEIN) continue;
            else if(prevRes == null) { currContig.add(currRes); prevRes = currRes; }
            else
            {
                try
                {
                    AtomState prevCa = state.get(prevRes.getAtom(" CA "));
                    AtomState currCa = state.get(currRes.getAtom(" CA "));
                    if(currCa.sqDistance(prevCa) < maxCaCa*maxCaCa && currRes.getChain().equals(prevRes.getChain()))
                    {
                        currContig.add(currRes);
                    }
                    else
                    {
                        if(currContig.size() > 0) allContigs.add(currContig);
                        currContig = new ArrayList();
                        currContig.add(currRes);
                    }
                    prevRes = currRes;
                }
                catch(AtomException ex) {}
            }
        }
        if(currContig.size() > 0) allContigs.add(currContig);
        return allContigs;
    }
//}}}

//{{{ makeProteinGuidepoints
//##############################################################################
    /**
    * Converts a collection of contiguous protein residues into a series of spline guidepoints.
    * Curvature-based offsets have already been computed, but "d" vectors have not yet been
    * flipped as neccessary to remove excess twist from the ribbon.
    */
    public GuidePoint[] makeProteinGuidepoints(Collection contigRes, ModelState state)
    {
        Residue[] res = (Residue[]) contigRes.toArray(new Residue[contigRes.size()]);
         // 2 identical dummies on each end (Ca of first/last residue); guidepoints fall between residues
        GuidePoint[] guides = new GuidePoint[res.length+4-1];
        GuidePoint g; // for easy reference
        
        Triple avec = new Triple(), bvec = new Triple(), midpt = new Triple(), offsetVec = new Triple();
        final double maxOffset = 1.5; // maximum displacement of guidepoint based on curvature
        
        //{{{ Make normal guidepoints at middle of peptides
        for(int i = 0; i < res.length-1; i++)
        {
            g = guides[i+2] = new GuidePoint();
            try
            {
                AtomState ca1 = state.get(res[i  ].getAtom(" CA "));
                AtomState ca2 = state.get(res[i+1].getAtom(" CA "));
                AtomState ox1 = state.get(res[i  ].getAtom(" O  "));
                g.xyz.likeMidpoint(ca1, ca2);
                avec.likeVector(ca1, ca2);
                bvec.likeVector(ca1, ox1);
                g.cvec.likeCross(avec, bvec).unit();
                g.dvec.likeCross(g.cvec, avec).unit();
                
                // Based on Ca(i-1) to Ca(i+2) distance, we may adjust ribbon width
                // and/or guidepoint position. Ribbon widens in areas of high OR low
                // curvature (alpha/beta, respectively). This is only a preliminary
                // estimate -- it's applied only for 3+ residues in a row. (checked below)
                // For high curvature ONLY (alpha), we offset the guidepoint
                // outwards to make the spline track the helix. Offset vector goes from
                // the midpoint of Ca(i-1) to Ca(i+2) thru the current guide point
                // (which is the midpoint of Ca(i) and Ca(i+1)).
                //  CA-CA DIST  WIDTH FACTOR    OFFSET FACTOR   NOTE
                //  ==========  ============    =============   ====
                //  5.0         1               1               ~limit for curled-up protein
                //  5.5         1               1               } linear interpolation
                //  7.0         0               0               } from 1.0 to 0.0
                //  9.0         0               0               } linear interpolation
                //  10.5        1               0               } from 1.0 to 0.0
                //  11.0        1               0               ~limit for extended protein
                
                // TODO: store width factor in GuidePoint; make second pass to check for 3+ mers
                
                if(1 <= i && i <= res.length-3)
                {
                    AtomState ca0 = state.get(res[i-1].getAtom(" CA "));
                    AtomState ca3 = state.get(res[i+2].getAtom(" CA "));
                    double widthFactor, offsetFactor;
                    double cacaDist = ca0.distance(ca3);
                    if(cacaDist < 7)
                    {
                        widthFactor = offsetFactor = Math.min(1.5, 7-cacaDist) / 1.5;
                        midpt.likeMidpoint(ca0, ca3);
                        offsetVec.likeVector(midpt, g.xyz).unit();
                        g.xyz.addMult(maxOffset*offsetFactor, offsetVec);
                    }
                    else if(cacaDist > 9)
                    {
                        widthFactor = offsetFactor = Math.min(1.5, cacaDist-9) / 1.5;
                        offsetFactor = 0;
                    }
                    else
                        widthFactor = offsetFactor = 0;
                }
            }
            catch(AtomException ex) {}
        }
        //}}} Make normal guidepoints at middle of peptides
        
        //{{{ Make dummy guidepoints at beginning and end
        try
        {
            g = new GuidePoint();
            AtomState ca = state.get(res[0].getAtom(" CA "));
            g.xyz.like(ca);
            g.cvec.like(guides[2].cvec);
            g.dvec.like(guides[2].dvec);
            guides[0] = guides[1] = g;
        }
        catch(AtomException ex) {}
        try
        {
            g = new GuidePoint();
            AtomState ca = state.get(res[res.length-1].getAtom(" CA "));
            g.xyz.like(ca);
            g.cvec.like(guides[guides.length-3].cvec);
            g.dvec.like(guides[guides.length-3].dvec);
            guides[guides.length-1] = guides[guides.length-2] = g;
        }
        catch(AtomException ex) {}
        //}}} Make dummy guidepoints at beginning and end
        
        return guides;
    }
//}}}

//{{{ untwistRibbon
//##############################################################################
    /**
    * Removes excess twist from a ribbon by reversing the sign of the GuidePoint
    * "d" vectors as necessary. A vector is flipped if it has a negative dot product
    * with the previous one (i.e. the angle between them is greater than 90 degrees).
    */
    public void untwistRibbon(GuidePoint[] guides)
    {
        for(int i = 1; i < guides.length; i++)
        {
            if(guides[i].dvec.dot(guides[i-1].dvec) < 0)
                guides[i].dvec.neg();
        }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

