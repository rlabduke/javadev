// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;
import molikin.crayons.*;
import driftwood.moldb2.SecondaryStructure; // (ARK Spring2010)

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.lang.Math; // (ARK Spring2010)
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.r3.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>RibbonPrinter</code> prints various ribbon representations.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jan 24 13:18:01 EST 2006
*/
public class RibbonPrinter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df = driftwood.util.Strings.usDecimalFormat("0.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    PrintWriter out;
    PrintWriter outGuides; ///(ARK Spring2010)
    RibbonCrayon crayon = molikin.crayons.ConstCrayon.NONE;
    
    Triple tmp = new Triple();

    // From PKININIT.c (ribwidalpha, ribwidbeta, ribwidcoil)
    double widthAlpha   = 2.0;
    double widthBeta    = 2.2;
    double widthCoil    = 1.0;
    double widthDefault = 2.0;
    
    boolean rnaPointIDs = false;
    final int   nIntervals = 4;  // (ARK Spring2010) moved this from top of printFancyRibbon()
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RibbonPrinter(PrintWriter out)
    {
        super();
        this.out = out;
        //try { // print kinemage header, (ARK Spring2010), get PDB id ???
        //	outGuides = new PrintWriter("0ssinterrupt_wht_helixonly/0guides.kin"); 
	//        outGuides.println("@kinemage 1\n@title {Guides}\n@group{axes} dominant off");  
	//        outGuides.println("@vectorlist {axes}\n{x}P -10 0 0 {\"}10 0 0\n{y}P 0 -10 0 {\"}0 10 0\n{z}P 0 0 -10 {\"}0 0 10");
	//        outGuides.println("@labellist {labels}\n{-x} -10 0.5 0\n{-y} 0.5 -10 0\n{-z} 0 0.5 -10");
        //} 
        //catch (Exception e) { System.err.println("Error creating outGuides.kin"); }        	
    }
//}}}

//{{{ printGuidepoints
//##############################################################################
    /**
    * For debugging purposes, displays several of the geometric constructions
    * used in building ribbons. Several kinemage lists are generated.
    */
    public void printGuidepoints(GuidePoint[] guides)
    {
        Triple end = new Triple();

        out.println("@balllist {guide points} color= red radius= 0.20 master= {guidepts}");
        for(int i = 0; i < guides.length; i++)
            out.println("{guide point off="+df.format(guides[i].offsetFactor)+" wid="+df.format(guides[i].widthFactor)+"} "+guides[i].xyz.format(df));

        out.println("@vectorlist {c vectors} color= green master= {guidepts}");
        for(int i = 0; i < guides.length; i++)
        {
            end.likeSum(guides[i].xyz, guides[i].cvec);
            out.println("{c}P "+guides[i].xyz.format(df)+" {\"}L "+end.format(df));
        }

        out.println("@vectorlist {d vectors} color= blue master= {guidepts}");
        for(int i = 0; i < guides.length; i++)
        {
            end.likeSum(guides[i].xyz, guides[i].dvec);
            out.println("{d}P "+guides[i].xyz.format(df)+" {\"}L "+end.format(df));
        }

        out.flush();
    }
//}}}

//{{{ printGuideptGp
//##############################################################################
//##############################################################################
    /**
    * Creates a separate kinemage with the guidepoints for the current ribbon element 
    * grouped together, (ARK Spring2010)
    */
    public void printGuideptGp(GuidePoint[] guides, String groupID, String type, int stGuide, int endGuide, Tuple3[][] splinepts)
    {
	Triple origin = new Triple();
	outGuides.println("@group {"+groupID+"} off animate dominant");
	outGuides.println("@balllist {guide points} color= red radius= 0.20 master= {guidepts} master= {"+type+"}");
	outGuides.println("{guide point} "+origin.format(df));
	outGuides.println("@vectorlist {c vectors} color= green master= {guidepts} master= {"+type+"} master= {c vecs} master= {lines}");
	for(int i = stGuide; i<=endGuide; i++)
		outGuides.println("{c}P "+origin.format(df)+" {\"}L "+guides[i].cvec.mult(5).format(df));
	outGuides.println("@balllist {c ends} color= green master= {guidepts} master= {"+type+"} master= {c vecs} master = {ends}");
	for(int i = stGuide; i<=endGuide; i++)
		outGuides.println("{"+getPointID(splinepts[1][i],guides[i],guides[i+1],i,nIntervals)+"} "+guides[i].cvec.format(df));
	outGuides.println("@vectorlist {d vectors} color= blue master= {guidepts} master= {"+type+"} master= {d vecs} master= {lines}");
	for(int i = stGuide; i<=endGuide; i++)
		outGuides.println("{d}P "+origin.format(df)+" {\"}L "+guides[i].dvec.mult(5).format(df));
	outGuides.println("@balllist {d ends} color= blue master= {guidepts} master= {"+type+"} master= {d vecs} master = {ends}");
	for(int i = stGuide; i<=endGuide; i++)
		outGuides.println("{"+getPointID(splinepts[1][i],guides[i],guides[i+1],i,nIntervals)+"} "+guides[i].dvec.format(df));
	outGuides.println("@vectorlist {conSeq} color= white alpha= 0.5 master= {guidepts} master= {"+type+"} master= {conSeq}");
	outGuides.println("{Seq connect} P "+guides[stGuide].dvec.format(df));
	for(int i = stGuide+1; i<=endGuide; i++)
		outGuides.println("{Seq connect} "+guides[i].dvec.format(df));
	outGuides.println("@vectorlist {arrowheads} color= white alpha= 0.5 master= {guidepts} master= {"+type+"} master= {conSeq} master= {arrowheads}");
	for(int i = stGuide+1; i<=endGuide; i++){
		if(i==guides.length-2) break;
		Triple arrow = new Triple().likeDiff(guides[i-1].dvec,guides[i].dvec).unit().mult(0.3);
		Triple orth = new Triple().likeOrthogonal(arrow).mult(0.4);
		Triple tailSt = new Triple().likeDiff(guides[i-1].dvec,guides[i].dvec).unit().mult(0.6).add(guides[i].dvec);
		arrow.add(guides[i].dvec);
		outGuides.println("{arrow} P "+arrow.format(df)+" {\"}L "+new Triple().likeSum(tailSt, orth).format(df));
		outGuides.println("{arrow} P "+arrow.format(df)+" {\"}L "+new Triple().likeDiff(tailSt, orth).format(df));
	}
	outGuides.println("@vectorlist {conPt} color= yellowtint alpha= 0.5 master= {guidepts} master= {"+type+"} master= {conPt}");
	for(int i = stGuide; i<=endGuide; i++)
		outGuides.println("{Pt connect}P "+guides[i].dvec.format(df)+" {\"}L "+guides[i].cvec.format(df));    
    }
//}}}

//{{{ printOne/Two/Three/FiveLine
//##############################################################################
    public void printOneLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, 0, 99); }
    
    public void printTwoLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, -1, 2); }
    
    public void printThreeLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, -1, 1); }
    
    public void printFiveLine(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    { printNLineImpl(guides, nIntervals, variableWidth, -1, 0.5); }
//}}}

//{{{ printNLineImpl
//##############################################################################
    /**
    * Displays an N-skein ribbon of uniform or variable width.
    * Only points are generated; the client is responsible for writing "@vectorlist ...".
    */
    public void printNLineImpl(GuidePoint[] guides, int nIntervals, boolean variableWidth, double strandStart, double strandStride)
    {
    	int         len     = guides.length;
        NRUBS       nrubs   = new NRUBS();
        Triple[]    pts     = new Triple[len];
        for(int i = 0; i < len; i++) pts[i] = new Triple();
        
        for(double strand = strandStart; strand <= 1; strand+=strandStride)
        {
            for(int i = 0; i < len; i++)
            {
                double ribwid = (guides[i].offsetFactor > 0 ? widthAlpha : widthBeta);
                double halfWidth = 0.5 * (widthCoil + guides[i].widthFactor*(ribwid - widthCoil));
                if(!variableWidth) halfWidth = widthDefault / 2.0;
                pts[i].like(guides[i].xyz).addMult(strand*halfWidth, guides[i].dvec);
            }
            Tuple3[] spline = nrubs.spline(pts, nIntervals);
            boolean isBreak = true;
            for(int i = 0; i < spline.length; i++)
            {
                int startGuide = (i/nIntervals) + 1;
                crayon.forRibbon(spline[i], guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
                // For this to make sense, we have to be able to restart line if there's a break
                if(!crayon.shouldPrint()) { isBreak = true; continue; }
                String ptID = getPointID(spline[i], guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
                tmp.like(spline[i]); // because Tuple3 doesn't have a format() method
                out.println("{"+ptID+"}"+(isBreak ? "P " : "")+crayon.getKinString()+" "+tmp.format(df));
                isBreak = false;
            }
        }

        out.flush();
    }
//}}}

//{{{ printFlatRibbon
//##############################################################################
    /**
    * Displays a triangulated ribbon of uniform or variable width.
    * Only points are generated; the client is responsible for writing "@ribbonlist ...".
    */
    public void printFlatRibbon(GuidePoint[] guides, int nIntervals, boolean variableWidth)
    {
        int         len     = guides.length;
        NRUBS       nrubs   = new NRUBS();
        Triple[]    pts1    = new Triple[len];
        Triple[]    pts2    = new Triple[len];
        for(int i = 0; i < len; i++)
        {
            pts1[i] = new Triple();
            pts2[i] = new Triple();
        }
        
        for(int i = 0; i < len; i++)
        {
            double ribwid = (guides[i].offsetFactor > 0 ? widthAlpha : widthBeta);
            double halfWidth = 0.5 * (widthCoil + guides[i].widthFactor*(ribwid - widthCoil));
            if(!variableWidth) halfWidth = widthDefault / 2.0;
            pts1[i].like(guides[i].xyz).addMult( halfWidth, guides[i].dvec);
            pts2[i].like(guides[i].xyz).addMult(-halfWidth, guides[i].dvec);
        }
        Tuple3[] spline1 = nrubs.spline(pts1, nIntervals);
        Tuple3[] spline2 = nrubs.spline(pts2, nIntervals);
        boolean isBreak = true;
        for(int i = 0; i < spline1.length; i++)
        {
            int startGuide = (i/nIntervals) + 1;
            tmp.likeMidpoint(spline1[i], spline2[i]);
            crayon.forRibbon(tmp, guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
            // For this to make sense, we have to be able to restart line if there's a break
            if(!crayon.shouldPrint()) { isBreak = true; continue; }
            String ptID = getPointID(tmp, guides[startGuide], guides[startGuide+1], i%nIntervals, nIntervals);
            tmp.like(spline1[i]); // because Tuple3 doesn't have a format() method
            out.println("{"+ptID+"}"+(isBreak ? "X " : "")+crayon.getKinString()+" "+tmp.format(df));
            tmp.like(spline2[i]);
            out.println("{\"}"+crayon.getKinString()+" "+tmp.format(df));
            isBreak = false;
        }

        out.flush();
    }
//}}}

//{{{ CLASS: RibbonElement
//##############################################################################
    static class RibbonElement implements java.lang.Comparable <RibbonElement>  
    {
        int start = 0, end = 0;
        Object type = SecondaryStructure.COIL;
        SecondaryStructure.Range range = null;
        
        public RibbonElement() {}			
        
        public RibbonElement(RibbonElement that)
        { this.like(that); }
        
        public RibbonElement(SecondaryStructure.Range range)
        {
            this.range = range;
            if(range == null) type = SecondaryStructure.COIL;
            else type = range.getType();
            if(type == SecondaryStructure.TURN) type = SecondaryStructure.COIL;
        }
        
        public boolean sameSSE(RibbonElement that)
        { return (this.type == that.type && this.range == that.range); }
        
        public void like(RibbonElement that)
        {
            this.start  = that.start;
            this.end    = that.end;
            this.type   = that.type;
            this.range  = that.range;
        }
        
        public int compareTo(RibbonElement that) { // (ARK Spring2010)
		int a, b;
		if(this.range==null) a=0;
		else a=this.range.strand;
		if(that.range==null) b=0;
		else b=that.range.strand;
		return a-b;
	}
    }
//}}}

//{{{ printFancyRibbon, printFancy
//##############################################################################
    /**
    * Displays a triangulated ribbon of with arrowheads, etc.
    * Several lists are generated, but with additional parameters specified below.
    */
    public void printFancyRibbon(GuidePoint[] guides, SecondaryStructure secStruct,
      double widthAlpha, double widthBeta, String listAlpha, String listBeta, String listCoil, ModelState state) // (ARK Spring2010) added ModelState 
    {
      printFancyRibbon(guides, secStruct, widthAlpha, widthBeta, listAlpha, listBeta, listCoil, null, state); // (ARK Spring2010) added state
    }
    
    public void printFancyRibbon(GuidePoint[] guides, SecondaryStructure secStruct,
        double widthAlpha, double widthBeta, String listAlpha, String listBeta, String listCoil, String listCoilOutline, ModelState state)
    {
    	// Data allocation, splining {{{
        int         len     = guides.length;
        NRUBS       nrubs   = new NRUBS();
 
        // Seven stands of guidepoints: coil, alpha, beta, (beta arrowheads, see below)
        double[] halfWidths = {0, -widthAlpha/2, widthAlpha/2, -widthBeta/2, widthBeta/2, -widthBeta, widthBeta};
        Triple[][] guidepts = new Triple[halfWidths.length][guides.length];
        for(int i = 0; i < guidepts.length; i++)
            for(int j = 0; j < guidepts[i].length; j++)
                guidepts[i][j] = new Triple(guides[j].xyz).addMult(halfWidths[i], guides[j].dvec); // original
                //guidepts[i][j] = new Triple(guides[j].xyz).addMult(halfWidths[i]*guides[j].widthFactor, guides[j].dvec); // added guides[j].widthFactor, (ARK Spring2010)
        // Seven strands of interpolated points
        Tuple3[][] splinepts = new Tuple3[halfWidths.length][];
        for(int i = 0; i < halfWidths.length; i++)
            splinepts[i] = nrubs.spline(guidepts[i], nIntervals);
        // I'm pretty sure this isn't needed now:
        // Arrow heads -- can't just spline some offset guidepoints
        // or there's a small break between the arrow body and arrow head.
        //splinepts[halfWidths.length  ] = new Triple[ splinepts[0].length ];
        //splinepts[halfWidths.length+1] = new Triple[ splinepts[0].length ];
        //for(int i = 0; i < splinepts[0].length; i++)
        //{
        //    splinepts[halfWidths.length  ][i] = new Triple().likeVector(splinepts[4][i], splinepts[3][i]).div(2).add(splinepts[3][i]);
        //    splinepts[halfWidths.length+1][i] = new Triple().likeVector(splinepts[3][i], splinepts[4][i]).div(2).add(splinepts[4][i]);
        //}
        // Data allocation, splining }}}

        // Discovery of ribbon elements: ribbons, ropes, and arrows {{{
        ArrayList ribbonElements = new ArrayList();
        RibbonElement ribElement = new RibbonElement();
        RibbonElement prevRibElt = new RibbonElement(); // (ARK Spring2010)
        ribbonElements.add(ribElement);
        ribElement.type = null;
        for(int i = 0; i < guides.length-3; i++)
        {
            GuidePoint g1 = guides[i+1], g2 = guides[i+2];
            
            // These two won't really be ribbon elements; we're just reusing the class for convenience.
            RibbonElement currSS = new RibbonElement(secStruct.getRange(g1.nextRes));
            RibbonElement nextSS = new RibbonElement(secStruct.getRange(g2.nextRes));
            // Otherwise, we get one unit of coil before alpha or beta at start
            if(ribElement.type == null) ribElement.like(currSS);
            
            if(!ribElement.sameSSE(currSS)) // helix / sheet starting
            {
                if(currSS.type == SecondaryStructure.HELIX
                || currSS.type == SecondaryStructure.STRAND)
                {
                    ribElement.end = nIntervals*i + 1;
                    ribbonElements.add(ribElement = new RibbonElement(currSS));
                    // Every helix / sheet starts from coil; see below
                    ribElement.start = nIntervals*i + 1;
                }
            }
            if(!ribElement.sameSSE(nextSS)) // helix / sheet ending
            {
                if(currSS.type == SecondaryStructure.HELIX
                || currSS.type == SecondaryStructure.STRAND)
                {
                    int end = nIntervals*i + 0;
                    if(currSS.type == SecondaryStructure.STRAND) end += nIntervals - 1;
                    ribElement.end = end;
                    ribbonElements.add(ribElement = new RibbonElement());
                    // Every helix / sheet flows into coil
                    ribElement.type = SecondaryStructure.COIL;
                    ribElement.start = end;
                }
            }
        }
        ribElement.end = splinepts[0].length-1;
        // Discovery of ribbon elements: ribbons, ropes, and arrows }}}
       
        // We juggle crayons around to make sure that black edges stay black.
        RibbonCrayon normalCrayon = this.getCrayon();
        // The ConstCrayon will return "" as the point color, thereby overriding normalCrayon.
        RibbonCrayon edgeCrayon = new CompositeCrayon().add(new ConstCrayon("")).add(normalCrayon);
        
        Collections.sort(ribbonElements); // sort by strand number --(ARK Spring2010)
        // Create list of just strands for searching through 
        Vector strands = new Vector(); 
        for(Iterator iter0 = ribbonElements.iterator(); iter0.hasNext(); ){
        	RibbonElement ribElement0 = (RibbonElement) iter0.next();
        	if(ribElement0.type == SecondaryStructure.STRAND)
        		strands.add(ribElement0);
        } // end (ARK Spring2010)
        
        for(Iterator iter = ribbonElements.iterator(); iter.hasNext(); ) 
        {
            ribElement = (RibbonElement) iter.next();
	    int stGuide = (ribElement.start / nIntervals) + 2; 	// (ARK Spring2010) 
            int endGuide = (ribElement.end / nIntervals) - 1;   // (ARK Spring2010)
            
            //System.err.println(ribElement.type+"    ["+ribElement.start+", "+ribElement.end+"]");
            if(ribElement.type == SecondaryStructure.HELIX) //{{{
            {
                // Determine sidedness using middle of helix, (ARK Spring2010)  
                int k = (int) Math.floor( (ribElement.start + ribElement.end) / 2 ); 
		Triple pt = new Triple(splinepts[2][k]); 
                Triple v1 = new Triple().likeVector(pt,splinepts[1][k]);
                Triple v2 = new Triple().likeVector(pt,splinepts[1][k+1]);
                Triple cross = v1.cross(v2);
                double dot = cross.unit().dot(guides[(int)Math.floor(k/nIntervals)+1].cvec.unit()); 
                // end (ARK Spring2010)
                
            	this.setCrayon(normalCrayon);
                out.println("@ribbonlist {fancy helix} "+listAlpha); 

                for(int i = ribElement.start; i < ribElement.end; i++)
                {
                    if(dot>0){ // current normal direction points into helix
                    	// flip the normals (for sidedness) by switching the order of these two lines, (ARK Spring2010)
                    	printFancy(guides, splinepts[2], i);
                    	printFancy(guides, splinepts[1], i);
                    }
                    else{
                    	printFancy(guides, splinepts[1], i);    
                    	printFancy(guides, splinepts[2], i);
                    }
                }
                printFancy(guides, splinepts[0], ribElement.end); // angled tip at end of helix
                this.setCrayon(edgeCrayon);
                out.println("@vectorlist {fancy helix edges} width= 1 "+listAlpha+" color= deadblack");
                // black edge, left side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end; i++)
                    printFancy(guides, splinepts[1], i);
                printFancy(guides, splinepts[0], ribElement.end);
                // black edge, right side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end; i++)
                    printFancy(guides, splinepts[2], i);
                printFancy(guides, splinepts[0], ribElement.end);
                
		// Print clustered guideposts, (ARK Spring2010) 
		//String groupID = "helix "+ribElement.range.initSeqNum+" to "+ribElement.range.endSeqNum;
		//printGuideptGp(guides, groupID, "helix", stGuide, endGuide+2, splinepts);
                
            } //}}}  ////end if(helix)
            else if(ribElement.type == SecondaryStructure.STRAND) //{{{
            {
            	// Determine sidedness, (ARK Spring2010) {{{
                double dot = 0;
		
                if(ribElement.range.strand != 1){ // o.w. it's first in the sheet
			// look for previous strand (strand-1)
			int i;
			for(i=0; i<strands.size(); i++){ 
				RibbonElement curElt = (RibbonElement)strands.get(i);
				if(curElt.range.equals(ribElement.range.previous)){ 
					prevRibElt = curElt;
					break;
				}
			}
			SecondaryStructure.Range prevRange = prevRibElt.range; 
			if(prevRange==null) prevRibElt = ribElement;  ////handle this differently?
			
			// determine sidedness using splinepts and normals
			int prevStGuide = (prevRibElt.start / nIntervals) + 2;
			int prevEndGuide = (prevRibElt.end / nIntervals) + 1;
			int curClosest = stGuide;	// Find the pair of guidepoints closest to each other
			int prevClosest = prevStGuide;	// (one on each strand) to perform the test
			double closestDist = 99999;  
			for(i = stGuide; i<=endGuide+2; i++){ 
				for(int j = prevStGuide; j<=prevEndGuide; j++){
					try{	// look for closest pair of H-bonding partners
						AtomState O = state.get(guides[i].prevRes.getAtom(" O  "));
						AtomState N = state.get(guides[j].nextRes.getAtom(" N  "));
						double dist = O.distance(N);
						if(dist<closestDist){
							curClosest = i;
							prevClosest = j;
							closestDist = dist;
						}
					}
					catch(AtomException ex) {}
				}
			}
			int kCur = Math.min(nIntervals*(curClosest-1), splinepts[4].length); ////min no longer needed????              
			int kPrev = Math.min(nIntervals*(prevClosest-1), splinepts[4].length);               
			Triple ptCur = new Triple(splinepts[4][kCur]); 
			Triple v1Cur = new Triple().likeVector(ptCur,splinepts[3][kCur]);
			Triple v2Cur = new Triple().likeVector(ptCur,splinepts[3][kCur+1]);
			Triple crossCur = v1Cur.cross(v2Cur);
			Triple ptPrev = new Triple(splinepts[4][kPrev]); 
			Triple v1Prev = new Triple().likeVector(ptPrev,splinepts[3][kPrev]);
			Triple v2Prev = new Triple().likeVector(ptPrev,splinepts[3][kPrev+1]);
			Triple crossPrev = v1Prev.cross(v2Prev);
			
			dot = crossCur.dot(crossPrev);
		}   // end sidedeness, (ARK Spring2010) }}}

            	this.setCrayon(normalCrayon);
                out.println("@ribbonlist {fancy sheet} "+listBeta);
                for(int i = ribElement.start; i < ribElement.end-1; i++)
                {
		    // If strands are not "facing" the same way,
                    // flip the normals (for sidedness) by switching the order of these two lines, (ARK Spring2010)
                    if( (dot<0 && !prevRibElt.range.flipped) || (dot>0 && prevRibElt.range.flipped) ){  
                   	    printFancy(guides, splinepts[4], i);
                    	    printFancy(guides, splinepts[3], i);
                    	    ribElement.range.flipped = true;
                    }
                    else {
                    	    printFancy(guides, splinepts[3], i);
                    	    printFancy(guides, splinepts[4], i);
                    }
                }
                // Ending *exactly* like this is critical to avoiding a break
                // between the arrow body and arrow head!
	        if( (dot<0 && !prevRibElt.range.flipped) || (dot>0 && prevRibElt.range.flipped) ){
                	// flip the normals for arrow head, (ARK Spring2010)
                	printFancy(guides, splinepts[6], ribElement.end-2);
                	printFancy(guides, splinepts[5], ribElement.end-2);
	        	printFancy(guides, splinepts[0], ribElement.end);
                }
	        else {
                	printFancy(guides, splinepts[5], ribElement.end-2);
                	printFancy(guides, splinepts[6], ribElement.end-2);
                	printFancy(guides, splinepts[0], ribElement.end);
                }
                this.setCrayon(edgeCrayon);
                out.println("@vectorlist {fancy sheet edges} width= 1 "+listBeta+" color= deadblack");
                // black edge, left side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end-1; i++)
                    printFancy(guides, splinepts[3], i);
                printFancy(guides, splinepts[5], ribElement.end-2);
                printFancy(guides, splinepts[0], ribElement.end);
                // black edge, right side
                printFancy(guides, splinepts[0], ribElement.start, true);
                for(int i = ribElement.start; i < ribElement.end-1; i++)
                    printFancy(guides, splinepts[4], i);
                printFancy(guides, splinepts[6], ribElement.end-2);
                printFancy(guides, splinepts[0], ribElement.end);
                
                ///Print clustered strand guideposts, (ARK Spring2010) 
		//String groupID = "strand "+ribElement.range.initSeqNum+" to "+ribElement.range.endSeqNum;
		//printGuideptGp(guides, groupID, "strand", stGuide, endGuide+2, splinepts);
		
            } // end if strand }}}
            else // COIL {{{
            {
                // for black outlines on coils
                if (listCoilOutline != null) {
                  this.setCrayon(normalCrayon);
                  out.println("@vectorlist {fancy coil edges} "+listCoilOutline);
                  for(int i = ribElement.start; i <= ribElement.end; i++)
                    printFancy(guides, splinepts[0], i);
                }
                this.setCrayon(normalCrayon);
                out.println("@vectorlist {fancy coil} "+listCoil);
                for(int i = ribElement.start; i <= ribElement.end; i++)
                    printFancy(guides, splinepts[0], i);
            	
            	///Print clustered guideposts, (ARK Spring2010) 
		//String groupID = "coil "+stGuide;
		//printGuideptGp(guides, groupID, "coil", stGuide, endGuide+2, splinepts);
            } //}}}
        }

        this.setCrayon(normalCrayon);
        out.flush();
        //outGuides.flush(); //(ARK Spring2010)
    }
    
    private void printFancy(GuidePoint[] guides, Tuple3[] splines, int i)
    { printFancy(guides, splines, i, false); } 
    
    private void printFancy(GuidePoint[] guides, Tuple3[] splines, int i, boolean lineBreak)
    {
        int nIntervals  = (splines.length - 1) / (guides.length - 3);
        int interval    = i % nIntervals;
        int startGuide  = (i / nIntervals) + 1;
        tmp.like(splines[i]); // because Tuple3 doesn't have a format() command
        out.print("{");
        out.print(getPointID(splines[i], guides[startGuide], guides[startGuide+1], interval, nIntervals));
        out.print("}");
        if(lineBreak) out.print("P ");
        crayon.forRibbon(splines[i], guides[startGuide], guides[startGuide+1], interval, nIntervals);
        out.print(crayon.getKinString());
        out.print(" ");
        out.println(tmp.format(df));
    }
//}}}

//{{{ get/setCrayon, setWidth
//##############################################################################
    /** The RibbonCrayon used for coloring these sections. */
    public RibbonCrayon getCrayon()
    { return this.crayon; }
    /** The RibbonCrayon used for coloring these sections. */
    public void setCrayon(RibbonCrayon c)
    { this.crayon = c; }
    
    /** Sets the width for constant-width ribbons.  Default is 2.0. */
    public void setWidth(double w)
    { this.widthDefault = w; }
    /**
    * Sets the width for variable-width ribbons.
    * Default is (2.0, 2.2, 1.0) for protein and (3.0, 3.0, 3.0) for nucleic acids.
    */
    public void setWidth(double alpha, double beta, double coil)
    {
        this.widthAlpha = alpha;
        this.widthBeta  = beta;
        this.widthCoil  = coil;
    }
//}}}

//{{{ getPointID, setRnaPointIDs
//##############################################################################
    //
    // One day, all this should be replaced with a pluggable system
    // like AtomIDer and PrekinIDer.
    //
    
    // Don't need all these params (right now) but kept them b/c they match RibbonCrayon
    private String getPointID(Tuple3 point, GuidePoint start, GuidePoint end, int interval, int nIntervals)
    {
        Residue res = start.nextRes; // == end.prevRes, in all cases
        if(rnaPointIDs && interval <= nIntervals/2)
            res = start.prevRes; // == first res, for RNA/DNA only
        
        StringBuffer buf = new StringBuffer(11); // should be long enough for almost all
        buf.append(res.getName());                      // 3
        buf.append(" ");                                // 1
        buf.append(res.getChain());                     // 1
        buf.append(" ");                                // 1
        buf.append(res.getSequenceNumber().trim());     // 1 - 4 (or more)
        buf.append(res.getInsertionCode());             // 1
        return buf.toString().toLowerCase();
    }
    
    /**
    * If true, residue names in point IDs will be decided based on the RNA/DNA
    * alignment of guidepoints to residues.
    * If false (the default), the protein style will be used instead.
    */
    public void setRnaPointIDs(boolean r)
    { this.rnaPointIDs = r; }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

