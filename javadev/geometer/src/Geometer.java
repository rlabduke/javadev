// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package geometer;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;

import driftwood.r3.*;
import driftwood.moldb2.*;
//}}}
/**
 * <code>Geometer</code> is a standalone version of my geometry analysis
 * plugin in KiNG.  It works on PDB files.  Due to the fact that my plugin
 * works on kins, this program seems to be better at finding geometry 
 * errors.  
 * 
 * <p>Copyright (C) 2006 by Vincent B. Chen.  All rights reserved.
 * <br>Begun on Tues Oct 16 11:34:00 EDT 2006
**/

public class Geometer {

//{{{ Constants   
    static final DecimalFormat df = new DecimalFormat("0.000");
    //static final DecimalFormat intf = new DecimalFormat("0");
//}}}

//{{{ Variable definitions
//###############################################################
    HashMap pepInfo;
    HashMap proInfo;
    HashMap glyInfo;
    HashMap pepSD;
    HashMap proSD;
    HashMap glySD;
//}}}


//{{{ main
//###############################################################
    public static void main(String[] args) {
	if (args.length == 0) {
	    System.out.println("No pdb files were specified!");
	} else {
	    File[] inputs = new File[args.length];
	    for (int i = 0; i < args.length; i++) {
		inputs[i] = new File(System.getProperty("user.dir") + "/" + args[i]);
	    }
	    Geometer tester = new Geometer(inputs);
	}
    }
//}}}
    
//{{{ Constructor
//###############################################################
    public Geometer(File[] files) {
	readGeomFile();
	//System.out.println(files[0]);
	analyzePdbFile(files[0]);
    }
//}}}

//{{{ readGeomFile
//###############################################################
    /**
     * Reads in the geometry resource file.  This file contains all the
     * geometry data info from (as of 061024) the 1999 Engh and Huber
     * values.
     **/
    public void readGeomFile() {
	//geomProps = new Props();
	try {

	    BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("geomdata/enghhuber1999.txt")));
	    String line;
	    pepInfo = new HashMap();
	    proInfo = new HashMap();
	    glyInfo = new HashMap();
	    HashMap currentMap = null;
	    pepSD = new HashMap();
	    proSD = new HashMap();
	    glySD = new HashMap();
	    HashMap currentSD = null;
	    while ((line = reader.readLine())!=null) {
		if (line.startsWith("pep")) {
		    currentMap = pepInfo;
		    currentSD = pepSD;
		} else if (line.startsWith("pro")) {
		    currentMap = proInfo;
		    currentSD = proSD;
		} else if (line.startsWith("gly")) {
		    currentMap = glyInfo;
		    currentSD = glySD;
		} else {
		    String[] values = line.split(",");
		    //System.out.println(values[0]);
		    currentMap.put(values[0], new Double(values[1]));
		    currentSD.put(values[0], new Double(values[2]));
		}
	    }
	}  catch (IOException ie) {
	    System.err.println("Error reading in geometry data!");
	} 
    }
//}}}	    
	    
//{{{ analyzePdbFile
//###############################################################
    /**
     * Analyzes geometry of one PDB file.  It iterates through and 
     * analyzes each model in that PDB file.
     **/
    public void analyzePdbFile(File pdb) {
	PdbReader reader = new PdbReader();
	try {
	    CoordinateFile coordFile = reader.read(pdb);
	    Iterator models = (coordFile.getModels()).iterator();
	    while (models.hasNext()) {
		//System.out.print(".");
		Model mod = (Model) models.next();
		testGeometry(mod);
	    }
	} catch (IOException ie) {
	    System.err.println("Problem when reading pdb file");
	}
    }
//}}}

//{{{ testGeometry
//###############################################################
    /**
     * Analyzes geometry of one model.  Important function containing
     * all the calls to different geometry calculating functions.
     * If there's a mistake in calculation of a bond length or angle,
     * it's probably here.
     **/
    public void testGeometry(Model mod) {
	ModelState modState = mod.getState();
	Residue prevRes = null;
	AtomState prevCa = null;
	AtomState prevCarb = null;
	AtomState prevOxy = null;
	try {
	    Iterator residues = (mod.getResidues()).iterator();
	    while (residues.hasNext()) {
		Residue res = (Residue) residues.next();
		AtomState nit = modState.get(res.getAtom(" N  "));
		AtomState ca = modState.get(res.getAtom(" CA "));
		AtomState carb = modState.get(res.getAtom(" C  "));
		AtomState oxy = modState.get(res.getAtom(" O  "));
		calcDist(res, nit, ca);
		calcDist(res, ca, carb);
		calcDist(res, carb, oxy);
		calcAngle(res, nit, ca, carb);
		calcAngle(res, ca, carb, oxy);
		if (prevRes != null) {
		    calcDist(res, prevCarb, nit);
		    calcAngle(res, prevOxy, prevCarb, nit);
		    calcAngle(res, prevCarb, nit, ca, prevCa);
		    calcPepDihedral(res, prevCa, prevCarb, nit, ca);
		    calcAngle(prevRes, prevCa, prevCarb, nit); //this angle goes with the previous residue, which is why it gets passed prevRes.
		}
		prevRes = res;
		prevCarb = carb;
		prevOxy = oxy;
		prevCa = ca;
		
	    }
	} catch (AtomException ae) {
	    System.err.println("Problem with atom in model " + mod.toString());
	}
    }	
//}}}

//{{{ calcDist
//###############################################################
    /**
     * Calculates the distance between two AtomStates.  It uses the
     * Residue to figure out information about the residue for the output.
     **/
    public void calcDist(Residue res, AtomState pt1, AtomState pt2) {
	if ((pt1 != null)&&(pt2 != null)) {
	    String atom1 = (pt1.getAtom()).getName().trim().toLowerCase();
	    String atom2 = (pt2.getAtom()).getName().trim().toLowerCase();
	    double idealdist;
	    double sd;
	    String resName = res.getName().trim().toLowerCase();
	    if (resName.equals("pro")) {
		idealdist = ((Double)proInfo.get(atom1 + atom2)).doubleValue();
		sd = ((Double)proSD.get(atom1 + atom2)).doubleValue();
	    } else if (resName.equals("gly")) {
		idealdist = ((Double)glyInfo.get(atom1 + atom2)).doubleValue();
		sd = ((Double)glySD.get(atom1 + atom2)).doubleValue();
	    } else {
		idealdist = ((Double)pepInfo.get(atom1 + atom2)).doubleValue();
		sd = ((Double)pepSD.get(atom1 + atom2)).doubleValue();
	    }
	    double dist = pt1.distance(pt2);
	    if ((dist <= idealdist - 4 * sd)||(dist >= idealdist + 4 * sd)) {
		String resInfo = resName + res.getSequenceNumber().trim();
		System.out.println(resInfo + ":" + atom1 + "-" + atom2 + ":" + df.format(dist) + ":" + df.format((dist - idealdist)/sd));
	    }
	}
    }
//}}}

//{{{ calcAngle
//###############################################################
    /**
     * Calculates the angle between three AtomStates.  It uses the
     * Residue to figure out information about the residue for the output.
     **/
    public void calcAngle(Residue res, AtomState pt1, AtomState pt2, AtomState pt3) {
	if ((pt1 != null)&&(pt2 != null)&&(pt3 != null)) {
	    String atom1 = (pt1.getAtom()).getName().trim().toLowerCase();
	    String atom2 = (pt2.getAtom()).getName().trim().toLowerCase();
	    String atom3 = (pt3.getAtom()).getName().trim().toLowerCase();
	    double idealang;
	    double sd;
	    String resName = res.getName().trim().toLowerCase();
	    if (resName.equals("pro")) {
		idealang = ((Double)proInfo.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)proSD.get(atom1 + atom2 + atom3)).doubleValue();
	    } else if (resName.equals("gly")) {
		idealang = ((Double)glyInfo.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)glySD.get(atom1 + atom2 + atom3)).doubleValue();
	    } else {
		idealang = ((Double)pepInfo.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)pepSD.get(atom1 + atom2 + atom3)).doubleValue();
	    }
	    double ang = Triple.angle(pt1, pt2, pt3);
	    if ((ang <= idealang - 4 * sd)||(ang >= idealang + 4 * sd)) {
		String resInfo = resName + res.getSequenceNumber().trim();
		System.out.println(resInfo + ":" + atom1 + "-" + atom2 + "-" + atom3 + ":" + df.format(ang) + ":" + df.format((ang - idealang)/sd));
	    }
	}
    }
//}}}

//{{{ calcAngle
//###############################################################
    /**
     * Similar to the other calcAngle, but this one uses the extra AtomState
     * to determine whether the residue belongs to a cisProline.  If it does,
     * a new Residue is made with a new name to call the other calcAngle,
     * so it knows to use the cisPro angle versus the transPro angle. 
     * This only affects the C-N-CA angle.
     **/
    public void calcAngle(Residue res, AtomState pt1, AtomState pt2, AtomState pt3, AtomState cisPt) {
	double dihed = 180;
	String resName = res.getName().trim().toLowerCase();
	if ((pt1 != null)&&(pt2 != null)&&(pt3 != null)&&(cisPt != null)) {
	    //System.out.println("calcing for cispeptides");
	    dihed = Triple.dihedral(cisPt, pt1, pt2, pt3);
	}
	if ((dihed < 30) && (dihed > -30) && (resName.equals("pro"))) {
	    Residue cisRes = new Residue(res, res.getChain(), res.getSegment(), res.getSequenceNumber(), res.getInsertionCode(), "cispro");
	    calcAngle(cisRes, pt1, pt2, pt3);
	} else {
	    calcAngle(res, pt1, pt2, pt3);
	}
    }
//}}}

//{{{ calcPepDihedral
//###############################################################
    /**
     * Calculates a dihedral angle.  This is only used to calculate
     * the peptide dihedral angle, to see how far off of 180 degrees it is.
     **/
    public void calcPepDihedral(Residue res, AtomState pt1, AtomState pt2, AtomState pt3, AtomState pt4) {
	if ((pt1 != null)&&(pt2 != null)&&(pt3 != null)&&(pt4 != null)) {
	    String atom1 = (pt1.getAtom()).getName().trim().toLowerCase();
	    String atom2 = (pt2.getAtom()).getName().trim().toLowerCase();
	    String atom3 = (pt3.getAtom()).getName().trim().toLowerCase();
	    String atom4 = (pt4.getAtom()).getName().trim().toLowerCase();
	    double dihed = Triple.dihedral(pt1, pt2, pt3, pt4);
	    if (((dihed < 160)&&(dihed > 30))||((dihed > -160)&&(dihed < -30))) {	
		String resInfo = res.getName().trim().toLowerCase() + res.getSequenceNumber().trim();
		System.out.println(resInfo + ":ca-c-n-ca:" + df.format(dihed));
	    }
	}
    }
//}}}

}//class
