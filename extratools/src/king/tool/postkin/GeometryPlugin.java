// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.points.*;
import king.tool.util.*;
import driftwood.r3.*;
import driftwood.gui.*;
import driftwood.moldb2.Residue;
import driftwood.data.*;
import driftwood.util.*;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.DecimalFormat;
//}}}

public class GeometryPlugin extends Plugin { 

//{{{ Constants   
    static final DecimalFormat df = new DecimalFormat("0.000");
    //static final DecimalFormat intf = new DecimalFormat("0");
//}}}

//{{{ Variable definitions
//###############################################################
    TreeMap<ResidueInfo, KPoint> caMap;
    TreeMap<ResidueInfo, KPoint> oxyMap;
    TreeMap<ResidueInfo, KPoint> carbMap;
    TreeMap<ResidueInfo, KPoint> nitMap;
    HashMap<ResidueInfo, ResidueInfo> sequenceMap;
    HashMap pepLength;
    HashMap pepSD;
    HashMap proLength;
    HashMap proSD;
    HashMap glyLength;
    HashMap glySD;
    HashMap pepAng;
    HashMap pepAngSD;
    HashMap proAng, proAngSD;
    HashMap glyAng, glyAngSD;
    //HashMap cisProAng, cisProAngSD;
    KList distList, angList, dihList;
    JFileChooser filechooser;

//}}}

//{{{ Constructor
    public GeometryPlugin(ToolBox tb) {
	super(tb);
    }
//}}}
    
//{{{ initialize
  /**
   * Contains all the hardcoded backbone geometry data from Engh and Huber 1999 values.
   **/
    public void initialize() {
	caMap = new TreeMap();
	oxyMap = new TreeMap();
	nitMap = new TreeMap();
	carbMap = new TreeMap();
	sequenceMap = new HashMap();

	pepLength = new HashMap();
	pepLength.put("nca", new Double(1.459));
	pepLength.put("cac", new Double(1.525));
	pepLength.put("co", new Double(1.229));
	pepLength.put("cn", new Double(1.336));
	pepSD = new HashMap();
	pepSD.put("nca", new Double(0.020));
	pepSD.put("cac", new Double(0.026));
	pepSD.put("co", new Double(0.019));
	pepSD.put("cn", new Double(0.023));

	proLength = new HashMap();
	proLength.put("nca", new Double(1.468));
	proLength.put("cac", new Double(1.524));
	proLength.put("co", new Double(1.228));
	proLength.put("cn", new Double(1.338));
	proSD = new HashMap();
	proSD.put("nca", new Double(0.017));
	proSD.put("cac", new Double(0.020));
	proSD.put("co", new Double(0.020));
	proSD.put("cn", new Double(0.019));

	glyLength = new HashMap();
	glyLength.put("nca", new Double(1.456));
	glyLength.put("cac", new Double(1.514));
	glyLength.put("co", new Double(1.232));
	glyLength.put("cn", new Double(1.326));
	glySD = new HashMap();
	glySD.put("nca", new Double(0.015));
	glySD.put("cac", new Double(0.016));
	glySD.put("co", new Double(0.016));
	glySD.put("cn", new Double(0.018));

	pepAng = new HashMap();
	pepAng.put("ncac", new Double(111));
	pepAng.put("cacn", new Double(117.2));
	pepAng.put("caco", new Double(120.1));
	pepAng.put("ocn", new Double(122.7));
	pepAng.put("cnca", new Double(121.7));
	pepAngSD = new HashMap();
	pepAngSD.put("ncac", new Double(2.7));
	pepAngSD.put("cacn", new Double(2.2));
	pepAngSD.put("caco", new Double(2.1));
	pepAngSD.put("ocn", new Double(1.6));
	pepAngSD.put("cnca", new Double(2.5));

	proAng = new HashMap();
	proAng.put("ncac", new Double(112.1));
	proAng.put("cacn", new Double(117.1));
	proAng.put("caco", new Double(120.2));
	proAng.put("ocn", new Double(121.1));
	proAng.put("cnca", new Double(119.3)); //trans pro
	proAng.put("ciscnca", new Double(127.0));
	proAngSD = new HashMap();
	proAngSD.put("ncac", new Double(2.6));
	proAngSD.put("cacn", new Double(2.8));
	proAngSD.put("caco", new Double(2.4));
	proAngSD.put("ocn", new Double(1.9));
	proAngSD.put("cnca", new Double(1.5));
	proAngSD.put("ciscnca", new Double(2.4));

	glyAng = new HashMap();
	glyAng.put("ncac", new Double(113.1));
	glyAng.put("cacn", new Double(116.2));
	glyAng.put("caco", new Double(120.6));
	glyAng.put("ocn", new Double(123.2));
	glyAng.put("cnca", new Double(122.3));
	glyAngSD = new HashMap();
	glyAngSD.put("ncac", new Double(2.5));
	glyAngSD.put("cacn", new Double(2.0));
	glyAngSD.put("caco", new Double(1.8));
	glyAngSD.put("ocn", new Double(1.7));
	glyAngSD.put("cnca", new Double(2.1));
	
    }
//}}}

//{{{ reset
  /**
  * Resets all the maps containing the KPoints from the kins.
  **/
  public void reset() {
    caMap = new TreeMap<ResidueInfo, KPoint>();
    oxyMap = new TreeMap<ResidueInfo, KPoint>();
    nitMap = new TreeMap<ResidueInfo, KPoint>();
    carbMap = new TreeMap<ResidueInfo, KPoint>();
    sequenceMap = new HashMap<ResidueInfo, ResidueInfo>();
  }
//}}}		
  
//{{{ onAnalyzeCurrent
  /**
   * Analyzes the geometry of the currently open kinemage.
   **/
    public void onAnalyzeCurrent(ActionEvent e) {
	initialize();
	
	System.out.println("Analyzing geometry of " + KinPointIdParser.getFirstGroupName(kMain.getKinemage()));
	//System.out.println("Analyzing geometry of " + pdbFile);
	KGroup geomGroup = new KGroup("Geometry");
	kMain.getKinemage().add(geomGroup);
	KGroup sub = new KGroup("geom");
	geomGroup.add(sub);
	distList = new KList(KList.BALL, "distances");
	angList = new KList(KList.VECTOR, "angles");
	dihList = new KList(KList.VECTOR, "dihedrals");
	sub.setHasButton(false);
	sub.add(distList);
	sub.add(angList);
	sub.add(dihList);
	splitKin(kMain.getKinemage());
	analyze();
	kMain.getKinemage().setModified(true);
	//kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    }
//}}}

//{{{ onAnalyzeAll
  /**
  * Determines all the files in a given directory, determined by the parent folder
  * of the file chosen, for geometry analysis.
  **/
  public void onAnalyzeAll(ActionEvent e) {
    initialize();
    if (filechooser == null) makeFileChooser();
    filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow())) {
      File f = filechooser.getSelectedFile();
      //System.out.println(f.getPath() + " : " + f.getName() + " : " + f.getParent());
      File[] allFiles = f.listFiles();   
      doAll(allFiles);
    }
  }
//}}}
		
//{{{ doAll
  /**
  * Does analysis on all the files.
  **/
  public void doAll(File[] allFiles) {
    TreeSet<File> files = new TreeSet();
    for (File pdbFile : allFiles) {
      files.add(pdbFile);
    }
    for (File pdbFile : files) {
      if (pdbFile.getName().indexOf(".kin") > -1) {
        reset();
        kMain.getKinIO().loadFile(pdbFile, null);
        Kinemage kin = kMain.getKinemage();
        for (MasterGroup mast : kin.masterList()) {
          mast.setOn(false);
        }
        kin.getMasterByName("mainchain").setOn(true);
        kin.getMasterByName("chain A").setOn(true);
        kin.getMasterByName("alta").setOn(true);
        //System.out.println("Analyzing geometry of " + KinPointIdParser.getFirstGroupName(kMain.getKinemage()));
        System.out.println("Analyzing geometry of " + pdbFile);
        KGroup geomGroup = new KGroup("Geometry");
        kMain.getKinemage().add(geomGroup);
        KGroup sub = new KGroup("geom");
        geomGroup.add(sub);
        distList = new KList(KList.BALL, "distances");
        angList = new KList(KList.VECTOR, "angles");
        dihList = new KList(KList.VECTOR, "dihedrals");
        sub.add(distList);
        sub.add(angList);
        sub.add(dihList);
        //kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
        splitKin(kMain.getKinemage());
        analyze();
      }
      kMain.getTextWindow().setText("");
      kMain.getStable().closeCurrent();
    }
    //out.flush();
        //w.close();
        //} catch (IOException ex) {
        //    JOptionPane.showMessageDialog(kMain.getTopWindow(),
        //				  "An error occurred while saving the file.",
        //				  "Sorry!", JOptionPane.ERROR_MESSAGE);
        //}
	
  }
//}}}
		
//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
	
        // Make accessory for file chooser
        TablePane acc = new TablePane();

        // Make actual file chooser -- will throw an exception if we're running as an Applet
        filechooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        
        filechooser.setAccessory(acc);
        //filechooser.addPropertyChangeListener(this);
        //filechooser.addChoosableFileFilter(fastaFilter);
        //filechooser.setFileFilter(fastaFilter);
    }
//}}}

//{{{ splitKin
/**
* Separates protein structure kin by backbone atom name.
**/
  public void splitKin(Kinemage kin) {
    KIterator<KPoint> pointIter = KIterator.visiblePoints(kin);
    for (KPoint pt : pointIter) {
      String atomName = KinPointIdParser.getAtomName(pt).toLowerCase();
      ResidueInfo res = makeResidueInfo(pt);
      //System.out.println(res);
      if (atomName.equals("ca")) {
        caMap.put(res, pt);
      }
      if (atomName.equals("o")) {
        oxyMap.put(res, pt);
      }
      if (atomName.equals("n")) {
        if (pt instanceof VectorPoint) { 
          // to figure out sequence order from connectivity; insertions codes are stupid.
          VectorPoint nit = (VectorPoint) pt;
          VectorPoint prev = (VectorPoint) nit.getPrev();
          if (prev != null) {
            String prevAtomName = KinPointIdParser.getAtomName(prev).toLowerCase();
            if (prevAtomName.equals("c")) {
              ResidueInfo prevRes = makeResidueInfo(prev);
              sequenceMap.put(prevRes, res);
            }
          } else {
            ResidueInfo fakePrev = new ResidueInfo("X", "", "-100", "Z", "ZZZ");
            sequenceMap.put(fakePrev, res);
          }
        }
        nitMap.put(res, pt);
      }
      if (atomName.equals("c")) {
        carbMap.put(res, pt);
      }
    }
  }
//}}}

//{{{ makeResideInfo
/**
* Takes a KPoint and makes a ResidueInfo object.
**/
        public ResidueInfo makeResidueInfo(KPoint pt) {
	String atomName = KinPointIdParser.getAtomName(pt).toLowerCase();
	String resName = KinPointIdParser.getResName(pt).toLowerCase();
	String resNum = KinPointIdParser.getResNumString(pt);
	String chainID = KinPointIdParser.getChainID(pt);
	String insCode = " ";
	if (!NumberUtils.isInteger(resNum)) {
	    int numLength = resNum.length();
	    if (numLength > 0) {
		insCode = resNum.substring(numLength - 1);
		resNum = resNum.substring(0, numLength - 1);
	    }
	}
	return new ResidueInfo(chainID, "", resNum, insCode, resName);
    }
//}}}    

//{{{ analyze
/**
* Steps through the sequenceMap and calls all the geometry calculation functions.
**/
    public void analyze() {
    TreeSet<ResidueInfo> keys = new TreeSet(sequenceMap.keySet());
    for (ResidueInfo prevRes : keys) {
	    ResidueInfo res = sequenceMap.get(prevRes);
	    //System.out.println(prevRes + ":" + res);
	    //System.out.println(prevRes);
	    //System.out.println(res);
	    if (caMap.containsKey(res)) {
        Integer resNum = Integer.valueOf(res.getSequenceNumber());
        String resSeq = (res.getSequenceNumber() + res.getInsertionCode()).trim();
        String resNumFull = (res.getSequenceNumber() + res.getInsertionCode()).trim() + ":" + KinPointIdParser.getLastString(((KPoint) caMap.get(res)).getName()) + ":" + res.getChain().trim();
        String resName = KinPointIdParser.getResName(caMap.get(res));
        //System.out.println(resName);
        calcDist(resNumFull, resName, nitMap.get(res), caMap.get(res));
        calcDist(resNumFull, resName, caMap.get(res), carbMap.get(res));
        calcDist(resNumFull, resName, carbMap.get(res), oxyMap.get(res));
        calcAngle(resNumFull, resName, nitMap.get(res), caMap.get(res), carbMap.get(res));
        calcAngle(resNumFull, resName, caMap.get(res), carbMap.get(res), oxyMap.get(res));
        if (caMap.containsKey(prevRes)) {
          calcDist(resNumFull, resName, carbMap.get(prevRes), nitMap.get(res));
          calcAngle(resNumFull, resName, oxyMap.get(prevRes), carbMap.get(prevRes), nitMap.get(res));
          calcAngle(resNumFull, resName, carbMap.get(prevRes), nitMap.get(res), caMap.get(res), caMap.get(prevRes)); //this calcs the c-n-ca angle; the previous res' ca is to check to see if the peptide is a cis peptide
          //calcPepDihedral(prevRes.getSequenceNumber() + prevRes.getInsertionCode() + " " + resNumFull, resName, caMap.get(prevRes), carbMap.get(prevRes), nitMap.get(res), caMap.get(res));
          calcPepDihedral(resNumFull, resName, caMap.get(prevRes), carbMap.get(prevRes), nitMap.get(res), caMap.get(res));
        }
        if (sequenceMap.containsKey(res)) { //Ideally, if there is a CA, there will be a mapping in seqMap.
          ResidueInfo nextRes = (ResidueInfo) sequenceMap.get(res);
          calcAngle(resNumFull, resName, caMap.get(res), carbMap.get(res), nitMap.get(nextRes));
        }
	    }
    }
  }
//}}}
  
//{{{ calcDist
/**
* Calculates distance between two points and checks to see if it falls within 4 SD
* of Engh and Huber values.
**/
        public void calcDist(String key, String resName, KPoint pt1, KPoint pt2) {
	//System.out.println(pt1 + " " + pt2);
	if ((pt1 != null)&&(pt2 != null)) {
	    String atom1 = KinPointIdParser.getAtomName(pt1).toLowerCase();
	    String atom2 = KinPointIdParser.getAtomName(pt2).toLowerCase();
	    //String res1 = KinPointIdParser.getResName(pt1).toLowerCase();
	    double idealdist;
	    double sd;
	    if (resName.equals("pro")) {
		idealdist = ((Double)proLength.get(atom1 + atom2)).doubleValue();
		sd = ((Double)proSD.get(atom1 + atom2)).doubleValue();
	    } else if (resName.equals("gly")) {
		idealdist = ((Double)glyLength.get(atom1 + atom2)).doubleValue();
		sd = ((Double)glySD.get(atom1 + atom2)).doubleValue();
	    } else {
		idealdist = ((Double)pepLength.get(atom1 + atom2)).doubleValue();
		sd = ((Double)pepSD.get(atom1 + atom2)).doubleValue();
	    }
	    
	    Triple trip1 = new Triple(pt1);
	    Triple trip2 = new Triple(pt2);
	    double dist = trip1.distance(trip2);
	    if ((dist <= idealdist - 4 * sd)||(dist >= idealdist + 4 * sd)) {
		//System.out.print("res " + key + " " + atom1 + "-" + atom2 + " ");
		//System.out.print("distance " + df.format(dist) + " ");
		//System.out.println(df.format((dist - idealdist)/sd) + " sigma off");
		System.out.println(key + ":" + atom1 + "-" + atom2 + ":" + df.format(dist) + ":" + df.format((dist - idealdist)/sd));
		drawBall("res "+key+" "+atom1+"-"+atom2, trip1, trip2, dist, idealdist);
	    }
	}
    }
//}}}

//{{{ calcAngle
/**
* Calculates angle between 3 points and checks to see if it falls within 4 SD
* of Engh and Huber values.
**/
        public void calcAngle(String key, String resName, KPoint pt1, KPoint pt2, KPoint pt3) {
	if ((pt1 != null)&&(pt2 != null)&&(pt3 != null)) {
	    String atom1 = KinPointIdParser.getAtomName(pt1).toLowerCase();
	    String atom2 = KinPointIdParser.getAtomName(pt2).toLowerCase();
	    String atom3 = KinPointIdParser.getAtomName(pt3).toLowerCase();
	    //String res1 = KinPointIdParser.getResName(pt1).toLowerCase();
	    double idealAng;
	    double sd;
	    if (resName.equals("pro")) {
		idealAng = ((Double)proAng.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)proAngSD.get(atom1 + atom2 + atom3)).doubleValue();
	    } else if (resName.equals("gly")) {
		idealAng = ((Double)glyAng.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)glyAngSD.get(atom1 + atom2 + atom3)).doubleValue();
	    } else if (resName.equals("cispro")) {
		idealAng = ((Double)proAng.get("cis" + atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)proAngSD.get("cis" + atom1 + atom2 + atom3)).doubleValue();
	    } else {
		idealAng = ((Double)pepAng.get(atom1 + atom2 + atom3)).doubleValue();
		sd = ((Double)pepAngSD.get(atom1 + atom2 + atom3)).doubleValue();
	    }
	    Triple trip1 = new Triple(pt1);
	    Triple trip2 = new Triple(pt2);
	    Triple trip3 = new Triple(pt3);
	    double ang = Triple.angle(trip1, trip2, trip3);
	    if ((ang <= idealAng - 4 * sd) || (ang >= idealAng + 4 * sd)) {
		//System.out.print("res " + key + " " + atom1 + "-" + atom2 + "-" + atom3 + " ");
		//System.out.print("angle " + df.format(ang) + " ");
		//System.out.println(df.format((ang - idealAng)/sd) + " sigma off");
		System.out.println(key + ":" + atom1 + "-" + atom2 + "-" + atom3 + ":" + df.format(ang) + ":" + df.format((ang - idealAng)/sd));
		drawLine("res "+key+" "+atom1+"-"+atom2+"-"+atom3, trip1, trip2, trip3, ang, idealAng);
	    }
	}
    }
//}}}
    
//{{{ calcAngle
/**
* Helper function to check if a peptide is cis or trans to call calcAngle with proper values.
**/
        public void calcAngle(String key, String resName, KPoint pt1, KPoint pt2, KPoint pt3, KPoint cisPt) {
	double dihed = 180;
	if ((pt1 != null)&&(pt2 != null)&&(pt3 != null)&&(cisPt != null)) {
	    //System.out.println("calcing for cispeptides");
	    Triple trip1 = new Triple(pt1);
	    Triple trip2 = new Triple(pt2);
	    Triple trip3 = new Triple(pt3);
	    Triple trip4 = new Triple(cisPt);
	    dihed = Triple.dihedral(cisPt, trip1, trip2, trip3);
	}
	if ((dihed < 30) && (dihed > -30) && (resName.equals("pro"))) {
	    calcAngle(key, "cispro", pt1, pt2, pt3);
	} else {
	    calcAngle(key, resName, pt1, pt2, pt3);
	}
    }
//}}}

//{{{ calcPepDihedral
/**
* Calculates peptide dihedral and checks to see if it falls outside 20 degrees of
* 180 or -180.  It also doesn't flag cis peptides at the moment.
**/
        public void calcPepDihedral(String key, String resName, KPoint pt1, KPoint pt2, KPoint pt3, KPoint pt4) {
	if ((pt1 != null)&&(pt2 != null)&&(pt3 != null)&&(pt4 != null)) {
	    String atom1 = KinPointIdParser.getAtomName(pt1).toLowerCase();
	    String atom2 = KinPointIdParser.getAtomName(pt2).toLowerCase();
	    String atom3 = KinPointIdParser.getAtomName(pt3).toLowerCase();
	    String atom4 = KinPointIdParser.getAtomName(pt4).toLowerCase();
	    Triple trip1 = new Triple(pt1);
	    Triple trip2 = new Triple(pt2);
	    Triple trip3 = new Triple(pt3);
	    Triple trip4 = new Triple(pt4);
	    double dihed = Triple.dihedral(trip1, trip2, trip3, trip4);
	    if (((dihed < 160)&&(dihed > 30))||((dihed > -160)&&(dihed < -30))) {
		//System.out.print("res " + key);// + " - res " + (key.intValue() + 1));
		//System.out.println(" peptide dihedral " + df.format(dihed));
		System.out.println(key + ":ca-c-n-ca:" + df.format(dihed));
		drawPep(trip1, trip2, trip3, trip4);
		//System.out.println(pt1.toString() + pt2.toString() + pt3.toString() + pt4.toString());
	    }
	} else {
	    //System.out.println(pt1 + ":" + pt2 + ":" + pt3 + ":" + pt4);
	}
    }
//}}}    
    
//{{{ drawPep
/**
* Adds peptide outlier indicator to kin.
**/
        public void drawPep(Triple trp1, Triple trp2, Triple trp3, Triple trp4) {
	VectorPoint p1 = new VectorPoint("ca 1", null);
	p1.setXYZ(trp1.getX(), trp1.getY(), trp1.getZ());
	VectorPoint p3 = new VectorPoint("n 2", p1);
	p3.setXYZ(trp3.getX(), trp3.getY(), trp3.getZ());
	VectorPoint p2 = new VectorPoint("c 1", null);
	p2.setXYZ(trp2.getX(), trp2.getY(), trp2.getZ());
	VectorPoint p4 = new VectorPoint("ca 2", p2);
	p4.setXYZ(trp4.getX(), trp4.getY(), trp4.getZ());
	dihList.add(p1);
	dihList.add(p3);
	dihList.add(p2);
	dihList.add(p4);
	dihList.setColor(KPalette.green);
	dihList.setWidth(4);
	
    }
//}}}		

//{{{ drawBall
/**
* Adds distance outlier indicator to kin.
**/
        public void drawBall(String name, Triple trp1, Triple trp2, double dist, double idealdist) {
	double distdiff = dist - idealdist;
	KPaint color = KPalette.blue;
	if (distdiff < 0) {
	    color = KPalette.red;
	    distdiff = - distdiff;
	}
	BallPoint point = new BallPoint(name);
	distList.add(point);
	point.setColor(color);
	point.setRadius((float)distdiff);
	Triple origVector = new Triple().likeVector(trp1, trp2);
	origVector = origVector.mult(idealdist/dist).add(trp1);
	point.setX(origVector.getX());
	point.setY(origVector.getY());
	point.setZ(origVector.getZ());
    }
//}}}

//{{{ drawLine
/**
* Adds angle outlier indicator to kin.
**/
        public void drawLine(String name, Triple trp1, Triple trp2, Triple trp3, double ang, double idealang) {
	//Triple vect1 = new Triple().likeVector(trp2, trp1);
	//Triple vect2 = new Triple().likeVector(trp2, trp3);
	//Triple normal = vect1.cross(vect2);
	Triple normal = new Triple().likeNormal(trp1, trp2, trp3);
	VectorPoint testNorm = new VectorPoint("testNorm", null);
	//geomList.add(testNorm);
	testNorm.setXYZ(trp2.getX(), trp2.getY(), trp2.getZ());
	VectorPoint norm = new VectorPoint("norm", testNorm);
	//geomList.add(norm);
	norm.setXYZ(normal.getX() + trp2.getX(), normal.getY() + trp2.getY(), normal.getZ() + trp2.getZ());
	Transform rotate = new Transform();
	//rotate = rotate.likeRotation(normal, idealang - ang);
	rotate = rotate.likeRotation(testNorm, norm, ang - idealang);
	VectorPoint prev = new VectorPoint(name, null);
	angList.add(prev);
	prev.setX(trp2.getX());
	prev.setY(trp2.getY());
	prev.setZ(trp2.getZ());
	VectorPoint point = new VectorPoint(name, prev);
	point.setColor(KPalette.pink);
	angList.add(point);
	point.setX(trp3.getX());
	point.setY(trp3.getY());
	point.setZ(trp3.getZ());
	rotate.transform(point);
	
    }
//}}}

//{{{ getToolsMenuItem
        public JMenuItem getToolsMenuItem() {
	//return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onStart"));
	JMenu menu = new JMenu("Analyze Geometry");
	menu.add(new JMenuItem(new ReflectiveAction("Analyze Current", null, this, "onAnalyzeCurrent")));
	menu.add(new JMenuItem(new ReflectiveAction("Analyze All", null, this, "onAnalyzeAll")));
	return menu;
    }
//}}}
    
//{{{ getHelp

  /** Returns the URL of a web page explaining use of this tool */
  public URL getHelpURL()
  {
    URL     url     = getClass().getResource("/extratools/tools-manual.html");
    String  anchor  = getHelpAnchor();
    if(url != null && anchor != null)
    {
      try { url = new URL(url, anchor); }
      catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
      return url;
    }
    else return null;
  }
  
  public String getHelpAnchor()
  { return "#geometry-plugin"; }
//}}}
    
//{{{ toString
    public String toString() {
	return "Analyze Geometry";
    }
//}}}
}
