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

//{{{ getNucleicAcidContigs
//##############################################################################
    /**
    * Given an ordered collection of Residues and a ModelState, return one or
    * more ordered collections of Residues that are contiguous (bonded).
    * @return Collection&lt;Collection&ltResidue&gt;&gt;
    */
    public Collection getNucleicAcidContigs(Collection residues, ModelState state, ResClassifier resC)
    {
        final double maxPP = 10.0; // from Prekin; ideal is ~7
        ArrayList allContigs = new ArrayList(), currContig = new ArrayList();
        Residue prevRes = null;
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue currRes = (Residue) iter.next();
            if(resC.classify(currRes) != ResClassifier.NUCACID) continue;
            else if(prevRes == null) { 
              if ((currRes.getAtom(" P  ")!=null)||(currRes.getAtom(" O5*")!=null)||(currRes.getAtom(" O5'")!=null)) {
                currContig.add(currRes);
                prevRes = currRes; 
              }
            }
            else
            {
                try
                {
                    Atom phos1 = prevRes.getAtom(" P  ");
                    if(phos1 == null) phos1 = prevRes.getAtom(" O5*");
                    if(phos1 == null) phos1 = prevRes.getAtom(" O5'");
                    AtomState prevP = state.get(phos1);
                    // Critical when two nucleic contigs appear back-to-back -- can't assume there's a P
                    Atom phos2 = currRes.getAtom(" P  ");
                    if(phos2 == null) phos2 = currRes.getAtom(" O5*");
                    if(phos2 == null) phos2 = currRes.getAtom(" O5'");
                    AtomState currP = state.get(phos2);
                    if(currP.sqDistance(prevP) < maxPP*maxPP && currRes.getChain().equals(prevRes.getChain()))
                    {
                        currContig.add(currRes);
                    }
                    else
                    {
                        if(currContig.size() > 0) { allContigs.add(currContig); currContig = new ArrayList(); }
                        currContig.add(currRes);
                    }
                    prevRes = currRes;
                }
                catch(AtomException ex) {
                }
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
            // Ensure prevRes and nextRes are not null if there's an AtomException.
            g.prevRes = res[i  ];
            g.nextRes = res[i+1];
            try
            {
                AtomState ca1 = state.get(res[i  ].getAtom(" CA "));
                AtomState ca2 = state.get(res[i+1].getAtom(" CA "));
                g.xyz.likeMidpoint(ca1, ca2);
                
                // Based on Ca(i-1) to Ca(i+2) distance, we may adjust ribbon width
                // and/or guidepoint position. Ribbon widens in areas of high OR low
                // curvature (alpha/beta, respectively). This is only a preliminary
                // estimate -- it's applied only for 3+ residues in a row. (checked below)
                //
                // For high curvature ONLY (alpha), we offset the guidepoint
                // outwards to make the spline track the chain (esp. helix);
                // this is done for each and does not require 3+ in a row.
                // Offset vector goes from the midpoint of Ca(i-1) to Ca(i+2)
                // thru the current guide point
                // (which is the midpoint of Ca(i) and Ca(i+1)).
                //
                //  CA-CA DIST  WIDTH FACTOR    OFFSET FACTOR   NOTE
                //  ==========  ============    =============   ====
                //  5.0         1               1               ~limit for curled-up protein
                //  5.5         1               1               } linear interpolation
                //  7.0         0               0               } from 1.0 to 0.0
                //  9.0         0               0               } linear interpolation
                //  10.5        1               0               } from 1.0 to 0.0
                //  11.0        1               0               ~limit for extended protein
                
                if(1 <= i && i <= res.length-3)
                {
                    AtomState ca0 = state.get(res[i-1].getAtom(" CA "));
                    AtomState ca3 = state.get(res[i+2].getAtom(" CA "));
                    double cacaDist = ca0.distance(ca3);
                    if(cacaDist < 7)
                    {
                        g.widthFactor = g.offsetFactor = Math.min(1.5, 7-cacaDist) / 1.5;
                        midpt.likeMidpoint(ca0, ca3);
                        offsetVec.likeVector(midpt, g.xyz).unit();
                        g.xyz.addMult(maxOffset*g.offsetFactor, offsetVec);
                    }
                    else if(cacaDist > 9)
                    {
                        g.widthFactor = Math.min(1.5, cacaDist-9) / 1.5;
                        g.offsetFactor = 0;
                    }
                    else
                        g.widthFactor = g.offsetFactor = 0;
                }
                
                // We do this last so that for CA-only structures, everything
                // possible is calculated before this throws an exception:
                AtomState ox1 = state.get(res[i  ].getAtom(" O  "));
                avec.likeVector(ca1, ca2);
                bvec.likeVector(ca1, ox1);
                g.cvec.likeCross(avec, bvec).unit();
                g.dvec.likeCross(g.cvec, avec).unit();
            }
            catch(AtomException ex) {}
        }
        //}}} Make normal guidepoints at middle of peptides
        
        //{{{ Check on widthFactors -- only apply for 3+ in a row > 0
        for(int i = 2; i < guides.length-2; )
        {
            // Scan to find first widened guidepoint:
            if(guides[i].widthFactor == 0) { i++; continue; }
            // Scan to find last widened guidepoint:
            int firstWide = i, nextThin = i+1;
            while(nextThin < guides.length-2 && guides[nextThin].widthFactor != 0) nextThin++;
            // If the span is less than 3, set them all back to zero:
            if(nextThin - firstWide < 3)
                for(int j = firstWide; j < nextThin; j++) guides[j].widthFactor = 0;
            //{System.err.println(guides[j].widthFactor+" -> 0 ["+j+"]"); guides[j].widthFactor = 0;}
            i = nextThin;
        }
        //}}}
        
        //{{{ Make dummy guidepoints at beginning and end
        try
        {
            g = new GuidePoint();
            AtomState ca = state.get(res[0].getAtom(" CA "));
            g.xyz.like(ca);
            g.cvec.like(guides[2].cvec);
            g.dvec.like(guides[2].dvec);
            g.offsetFactor = guides[2].offsetFactor;
            g.widthFactor = guides[2].widthFactor;
            g.prevRes = g.nextRes = res[0];
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
            g.offsetFactor = guides[guides.length-3].offsetFactor;
            g.widthFactor = guides[guides.length-3].widthFactor;
            g.prevRes = g.nextRes = res[res.length-1];
            guides[guides.length-1] = guides[guides.length-2] = g;
        }
        catch(AtomException ex) {}
        //}}} Make dummy guidepoints at beginning and end
        
        return guides;
    }
//}}}

//{{{ makeNucleicAcidGuidepoints
//##############################################################################
    /**
    * Converts a collection of contiguous nucleic acid residues into a series of spline guidepoints.
    * Curvature-based offsets have already been computed, but "d" vectors have not yet been
    * flipped as neccessary to remove excess twist from the ribbon.
    */
    public GuidePoint[] makeNucleicAcidGuidepoints(Collection contigRes, ModelState state)
    {
        Residue[] res = (Residue[]) contigRes.toArray(new Residue[contigRes.size()]);
         // 2 identical dummies on each end (see below); other guidepoints fall between phosphates
        GuidePoint[] guides = new GuidePoint[res.length+4-1];
        GuidePoint g; // for easy reference
        Triple avec = new Triple(), bvec = new Triple(), midpt = new Triple(), offsetVec = new Triple();
        
        //{{{ Make normal guidepoints at middle of residues
        for(int i = 0; i < res.length-1; i++)
        {
            g = guides[i+2] = new GuidePoint();
            // Ensure prevRes and nextRes are not null if there's an AtomException.
            //g.prevRes = g.nextRes = res[i]; // both really "this" res
            // This makes the fancy (arrowheaded) ribbon code work right
            g.prevRes = res[i  ];
            g.nextRes = res[i+1];
            try
            {
                Atom phos1 = res[i].getAtom(" P  ");
                if(phos1 == null) phos1 = res[i].getAtom(" O5*");
                if(phos1 == null) phos1 = res[i].getAtom(" O5'");
                AtomState p1 = state.get(phos1);
                AtomState p2 = state.get(res[i+1].getAtom(" P  "));
                g.xyz.likeMidpoint(p1, p2);
                
                // Based on P(i-1) to P(i+1) or P(i) to P(i+2) distance, we may offset guide points.
                // For areas of high curvature, the guide point is offset towards the C4'
              
                if(1 <= i && i <= res.length-3)
                {
                    Atom phos0 = res[i-1].getAtom(" P  ");
                    if(phos0 == null) phos0 = res[i-1].getAtom(" O5*");
                    if(phos0 == null) phos0 = res[i-1].getAtom(" O5'");
                    AtomState p0 = state.get(phos0);
                    AtomState p3 = state.get(res[i+2].getAtom(" P  "));
                    double ppDist1 = g.xyz.distance(p0);
                    double ppDist2 = g.xyz.distance(p3);
                    // Default values for regions of low curvature:
                    boolean isTightlyCurved = false;
                    double ppDistance = ppDist1;
                    double maxOffset = 0;
                    g.offsetFactor = 0;
                    g.widthFactor = 0; // no width hinting for nucleic acids, period
                    if(ppDist1 <= 9.0 || ppDist2 <= 9.0)
                    {
                        isTightlyCurved = true;
                        ppDistance = Math.min(ppDist1, ppDist2);
                        Atom carbon4 = res[i].getAtom(" C4*");
                        if(carbon4 == null) carbon4 = res[i].getAtom(" C4'");
                        AtomState c4 = state.get(carbon4);
                        maxOffset = g.xyz.distance(c4) + 1.0; // allows guide point to go past the C4'
                        g.offsetFactor = (9.0 - ppDistance) / (9.0 - 7.0);
                        if(g.offsetFactor > 1) g.offsetFactor = 1; // reaches full offset at 7A curvature
                        // Has no effect in low curvature b/c offsetFactor = maxOffset = 0
                        offsetVec.likeVector(g.xyz, c4).unit();
                        g.xyz.addMult(maxOffset*g.offsetFactor, offsetVec);
                    }
                }
                
                // We do this last so that for P-only structures (do those exist?),
                // everything possible is calculated before this throws an exception:
                Atom carbon3 = res[i].getAtom(" C3*");
                if(carbon3 == null) carbon3 = res[i].getAtom(" C3'");
                AtomState c3 = state.get(carbon3);
                Atom carbon1 = res[i].getAtom(" C1*");
                if(carbon1 == null) carbon1 = res[i].getAtom(" C1'");
                AtomState c1 = state.get(carbon1);
                
                avec.likeVector(p1, p2);
                bvec.likeVector(c3, c1);
                g.cvec.likeCross(avec, bvec).unit();
                g.dvec.likeCross(g.cvec, avec).unit();
            }
            catch(AtomException ex) {}
        }
        //}}} Make normal guidepoints at middle of peptides
        
        // widthFactors are not used with nucleic acid ribbons
        
        //{{{ Make dummy guidepoints at beginning and end
        try
        {
            // 5' guide point is the phosphate or O5'
            g = new GuidePoint();
            Atom phos = res[0].getAtom(" P  ");
            if(phos == null) phos = res[0].getAtom(" O5*");
            if(phos == null) phos = res[0].getAtom(" O5'");
            AtomState p = state.get(phos);
            g.xyz.like(p);
            g.cvec.like(guides[2].cvec);
            g.dvec.like(guides[2].dvec);
            g.offsetFactor = guides[2].offsetFactor;
            g.widthFactor = guides[2].widthFactor;
            g.prevRes = g.nextRes = res[0];
            guides[0] = guides[1] = g;
        }
        catch(AtomException ex) {}
        try
        {
            // Prekin: 3' guide point is 2/3 of the way to the O3' from the last guide point
            //g = new GuidePoint();
            //Atom oxygen3 = res[res.length-1].getAtom(" O3*");
            //if(oxygen3 == null) oxygen3 = res[res.length-1].getAtom(" O3'");
            //AtomState o3 = state.get(oxygen3);
            //g.xyz.like(guides[guides.length-3].xyz).addMult(2, o3).div(3);
            // IWD: 3' guide point is C3'
            g = new GuidePoint();
            Atom carbon3 = res[res.length-1].getAtom(" C3*");
            if(carbon3 == null) carbon3 = res[res.length-1].getAtom(" C3'");
            AtomState c3 = state.get(carbon3);
            g.xyz.like(c3);
            g.cvec.like(guides[guides.length-3].cvec);
            g.dvec.like(guides[guides.length-3].dvec);
            g.offsetFactor = guides[guides.length-3].offsetFactor;
            g.widthFactor = guides[guides.length-3].widthFactor;
            g.prevRes = g.nextRes = res[res.length-1];
            guides[guides.length-1] = guides[guides.length-2] = g;
        }
        catch(AtomException ex) {}
        //}}} Make dummy guidepoints at beginning and end
        
        return guides;
    }
//}}}

//{{{ swapEdgeAndFace, untwistRibbon
//##############################################################################
    /**
    * Converts a RNA-style ribbon (the default) to a DNA-style one,
    * and vice versa, by swapping the "c" and "d" vectors.
    * This results in a 90 degree rotation of the ribbon around the ribbon axis.
    */
    public void swapEdgeAndFace(GuidePoint[] guides)
    {
        for(int i = 0; i < guides.length; i++)
        {
            Triple tmp = guides[i].cvec;
            guides[i].cvec = guides[i].dvec;
            guides[i].dvec = tmp;
        }
    }
    
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

