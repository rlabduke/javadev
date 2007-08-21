// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.dangle;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.parser.*;
//}}}

/**
* <code>GeomKinMaker</code> takes input from geometry deviation information from 
* Dangle and makes nice visualizations in kinemage form of said deviations.
*/
public class GeomKinSmith //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    final DecimalFormat df1 = new DecimalFormat("0.##");
    final DecimalFormat df2 = new DecimalFormat("#.###");
//}}}

//{{{ Variable definitions
//##############################################################################
    ArrayList<String> distList, angList;  
        // each String entry in the AL is a kinemage-format balllist point or 
        // vectorlist point for the current model in coords
    Measurement[] meas;
    String label;
    CoordinateFile coords;
    boolean doDistDevsKin;
    boolean doAngleDevsKin;
    boolean doKinHeadings;
    double sigmaCutoff;
    File kinout;
    boolean ignoreDNA;
    boolean subgroupNotGroup;
//}}}

//{{{ Constructor
//##############################################################################
    public GeomKinSmith(ArrayList<Measurement> m, String l, CoordinateFile c, boolean dist, boolean ang, boolean head, double sc, boolean id, boolean sgng)
    {
        meas = (Measurement[]) m.toArray(new Measurement[m.size()]);
        label = l;
        coords = c;
        doDistDevsKin = dist;
        doAngleDevsKin = ang;
        doKinHeadings = head;
        sigmaCutoff = sc;
        ignoreDNA = id;
        subgroupNotGroup = sgng;
    }
//}}}

//{{{ makeKin
//##############################################################################
    public void makeKin()
    {
        // Look thru models -> residues for this CoordinateFile (from one pdb).
        // For each residue that is a(n) dist or angle outlier, add a 
        // geom outlier viz to the proper ArrayList of geom dev viz's for the
        // current model
        for(Iterator models = coords.getModels().iterator(); models.hasNext(); )
        {
            Model model = (Model) models.next();
            ModelState state = model.getState();
            
            distList  = new ArrayList<String>();
            angList   = new ArrayList<String>();
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                boolean print = true;
                
                // Get c2o2index ("// Print headings" in Dangle.java)
                int c2o2index = 999; // placeholder value
                for(int i = 0; i < meas.length; i++)
                {
                    if (ignoreDNA && (meas[i].getLabel()).equals("c2o2") && c2o2index == 999)
                    {
                        c2o2index = i;
                    }
                }
                
                // If c2o2 (for purpose of discrimination btw DNA & RNA) comes up 
                // NaN, we wanna omit this residue --> set 'print' to false
                for(int i = 0; i < meas.length; i++)
                {
                    if (ignoreDNA && c2o2index != 999)
                    {
                        if ( Double.isNaN(meas[c2o2index].measure(model, state, res)) )
                            print = false;
                    }
                }
                
                if (print)
                {
                    for(int i = 0; i < meas.length; i++)
                    {
                        // See if this Measurement is a Group of potentially interesting 
                        // Measurements, or just a regualar individual Measurement
                        
                        Measurement.Group potentiallyAGroup = null;
                        try { potentiallyAGroup = (Measurement.Group) meas[i]; }
                        catch (java.lang.ClassCastException cce) { ; }
                        
                        if (potentiallyAGroup != null) // then meas[i] is a Measurement.Group 
                                                       // (subclass of Measurement)
                        {
                            boolean doneWithGroup = false;
                            
                            Measurement.Group temp = (Measurement.Group) meas[i];
                            ArrayList<Measurement> measAL = ( ArrayList<Measurement> ) temp.group; // variable in Group
                            
                            for (Measurement m : measAL)
                            {
                                double val = m.measure(model, state, res);
                                double dev = m.getDeviation();
                                double ideal = m.mean;
                                if(!Double.isNaN(val) && !doneWithGroup)
                                {
                                    // We've found the valid Measurement in this Group
                                    doneWithGroup = true;
                                    
                                    if (!Double.isNaN(dev) && Math.abs(dev) >= sigmaCutoff)
                                    {
                                        if(m.getType().equals("distance"))
                                        {
                                            //System.out.println(res.toString()+" "+m.toString()+
                                            //    " "+val+" "+dev);
                                            
                                            distImpl(m, model, state, res, val, ideal);
                                        }
                                        if(m.getType().equals("angle"))
                                        {
                                            angleImpl(m, model, state, res, val, ideal);
                                        }
                                    }
                                }
                            }
                        }
                        
                        else // meas[i] is NOT a Measurement.Group; probably a 
                             // regular Measurement.Distance or Measurement.Angle
                        {
                            double val = meas[i].measure(model, state, res);
                            double dev = meas[i].getDeviation();
                            double ideal = meas[i].mean;
                            
                            if(!Double.isNaN(val) && !Double.isNaN(dev) && Math.abs(dev) >= sigmaCutoff)
                            {
                                if(meas[i].getType().equals("distance"))
                                {
                                    distImpl(meas[i], model, state, res, val, ideal);
                                }
                                if(meas[i].getType().equals("angle"))
                                {
                                    angleImpl(meas[i], model, state, res, val, ideal);
                                }
                            }
                        }
                        
                    } //for (each meas[i] in meas)
                    
                } //if (print)
            }
            
            printModel(model);
        }
    }
//}}}

//{{{ distImpl
//##############################################################################
    protected void distImpl(Measurement meas, Model model, ModelState state, Residue res, double val, double ideal)
    {
	    Measurement.Distance d = (Measurement.Distance) meas;
        AtomSpec atomSpec1 = (AtomSpec) d.getA();
        AtomSpec atomSpec2 = (AtomSpec) d.getB();
        
        String atomSpec1Name = getName(atomSpec1);
        String atomSpec2Name = getName(atomSpec2);
        
        // Get correct Residues using Measurement's resOffset values
        Residue res1 = atomSpec1.getRes(model, state, res);
        Residue res2 = atomSpec2.getRes(model, state, res);
        if (atomSpec1.getResOffset() == -1)      res1 = res.getPrev(model);
        if (atomSpec1.getResOffset() ==  1)      res1 = res.getNext(model);
        if (atomSpec2.getResOffset() == -1)      res2 = res.getPrev(model);
        if (atomSpec2.getResOffset() ==  1)      res2 = res.getNext(model);
        
        // Use correct Residues, look thru their Atoms, and get both Atoms
        // involved in this Distance Measurement
        Atom atom1 = null;
        Atom atom2 = null;
        Iterator i1 = (res1.getAtoms()).iterator();
        while (i1.hasNext())
        {
            Atom a = (Atom) i1.next();
            if ((a.getName().trim()).equals( atomSpec1Name.trim()) )
                atom1 = a;
        }        
        Iterator i2 = (res2.getAtoms()).iterator();    
        while (i2.hasNext())
        {
            Atom a = (Atom) i2.next();
            if ( (a.getName().trim()).equals(atomSpec2Name.trim()) )
                atom2 = a;
        }
        
        // Get AtomStates (and thereby coordinates) for the two Atoms found 
        // above, using the ModelState
        AtomState atomState1 = null;
        AtomState atomState2 = null;
        try
        {
            atomState1 = state.get(atom1);
            atomState2 = state.get(atom2);
        }
        catch (AtomException ae) 
        {
            System.out.println("Couldn't get AtomState");
        }
        
        String devName = label.substring(0,4)+" "+res1.getName()+res1.
            getSequenceInteger()+"-"+res2.getName()+res2.getSequenceInteger()
            +" "+atom1.getName().trim()+"-"+atom2.getName().trim();
        
        // Pass these new Triples instead of (Triple) atomState# so subsequent
        // manipulations of the coordinates don't alter the orig coordinates, 
        // which may be used in a subsequent measurement (e.g. i-1 C, i N here
        // and i N, i CA next)
        Triple atomState1Triple = new Triple(atomState1.getX(),
                                             atomState1.getY(),
                                             atomState1.getZ());
        Triple atomState2Triple = new Triple(atomState2.getX(),
                                             atomState2.getY(),
                                             atomState2.getZ());
        
	    //addBall(devName, (Triple) as1, (Triple) as2, val, ideal);
        //addBallInMiddle(devName, (Triple) as1, (Triple) as2, val, ideal);
        addSpring(devName, atomState1Triple, atomState2Triple, val, ideal);
    }
//}}}

//{{{ empty code segment -- debugging distImpl
//##############################################################################
    
        // for debugging
        //String measLabel = meas.toString();
        //System.out.println("measLabel: "+measLabel);
        //System.out.println("atomSpec1 resOffset: "+atomSpec1.getResOffset());
        //System.out.println("atomSpec2 resOffset: "+atomSpec2.getResOffset());
        //System.out.println("res1.getCNIT(): "+res1.getCNIT());
        //System.out.println("res2.getCNIT(): "+res2.getCNIT());
        //String origspec1nametemp = atomSpec1.toString();
        //String origspec1name = origspec1nametemp.substring(origspec1nametemp.length() - 4);
        //String origspec2nametemp = atomSpec2.toString();
        //String origspec2name = origspec2nametemp.substring(origspec2nametemp.length() - 4);
        //System.out.println("origspec1name = "+origspec1name);
        //System.out.println("origspec2name = "+origspec2name);
        //System.out.println("atomSpec1Name.trim() = "+atomSpec1Name.trim());
        //System.out.println("atomSpec2Name.trim() = "+atomSpec2Name.trim());
        // for debugging
        //Iterator it1 = (res1.getAtoms()).iterator();
        //while (it1.hasNext())
        //{
        //    Atom a1 = (Atom) it1.next();
        //    //System.out.println("res1 has "+a1.toString());
        //}
        //Iterator it2 = (res2.getAtoms()).iterator();
        //while (it2.hasNext())
        //{
        //    Atom a2 = (Atom) it2.next();
        //    //System.out.println("res2 has "+a2.toString());
        //}
        //Atom res1C = null;
        //Atom res2N = null;
        //Atom res1N = null;
        //Iterator iter1 = (res1.getAtoms()).iterator();
        //while (iter1.hasNext())
        //{
        //    Atom a = (Atom) iter1.next();
        //    if ((a.getName().trim()).equals("C"))
        //        res1C = a;
        //}       
        //Iterator iter2 = (res2.getAtoms()).iterator();    
        //while (iter2.hasNext())
        //{
        //    Atom a = (Atom) iter2.next();
        //    if ((a.getName().trim()).equals("N"))
        //        res2N = a;
        //}
        //iter1 = (res1.getAtoms()).iterator();
        //while (iter1.hasNext())
        //{
        //    Atom a = (Atom) iter1.next();
        //    if ((a.getName().trim()).equals("N"))
        //        res1N = a;
        //}
        //System.out.println("res1C.getName() = "+res1C.getName());
        //System.out.println("res2N.getName() = "+res2N.getName());
        //System.out.println("res1N.getName() = "+res1N.getName());
        //AtomState res1Cstate = null;
        //AtomState res2Nstate = null;
        //AtomState res1Nstate = null;
        //try {
        //    res1Cstate = state.get(res1C); 
        //    res2Nstate = state.get(res2N);
        //    res1Nstate = state.get(res1N);
        //    System.out.println("res1 C: "+res1Cstate.getX()+", "+res1Cstate.getY()+", "+res1Cstate.getZ());
        //    System.out.println("res2 N: "+res2Nstate.getX()+", "+res2Nstate.getY()+", "+res2Nstate.getZ());
        //    System.out.println("res1 N: "+res1Nstate.getX()+", "+res1Nstate.getY()+", "+res1Nstate.getZ());
        //}
        //catch (AtomException ae) {System.out.println("Couldn't print coords");}
        //Iterator gooberiter = (res1.getAtoms()).iterator();
        //int count = 0;
        //while (gooberiter.hasNext())
        //{
        //    count ++;
        //    Atom a = (Atom) gooberiter.next();
        //    try
        //    {
        //        AtomState ast = state.get(a);
        //       System.out.println("res Atom"+count+": "+ast.getX()+" "+ast.getY()+" "+ast.getZ());
        //    }
        //    catch (AtomException ae) {System.out.println("Couldn't print coords");}
        //}
        //System.out.println("atomState1: "+atomState1.getX()+", "+atomState1.getY()+", "+atomState1.getZ());
        //System.out.println("atomState2: "+atomState2.getX()+", "+atomState2.getY()+", "+atomState2.getZ());
        //if (res1.equals(res2))
         //   System.out.println("res1 equals res2");
        //if (res1Cstate == null) System.out.println("res1Cstate == null");
        //else                    System.out.println("res1Cstate != null");
        //if (res2Nstate == null) System.out.println("res2Nstate == null");
        //else                    System.out.println("res2Nstate != null");
        //if (res1Nstate == null) System.out.println("res1Nstate == null");
        //else                    System.out.println("res1Nstate != null");
        //System.out.println("res1 = "+res1.toString());
        //System.out.println("res1.getName() = "+res1.getName());
        //System.out.println("res1.getSequenceInteger() = "+res1.getSequenceInteger());
        //System.out.println("label = "+label);
        //System.out.println("atomState1.getName().trim() = "+atomState1.getName().trim());
        //System.out.println("atomState2.getName().trim() = "+atomState2.getName().trim());
    
//}}}

//{{{ getName
//##############################################################################
    protected String getName(AtomSpec atspec)
    {
        String origtemp = atspec.toString();
        String temp = origtemp.substring(origtemp.length() - 4);
        // e.g. "_C__"
        
        int i1 = 99; // first non-underscore character
        int i2 = 99; // first underscore character after end of non-underscore
                     // characters
        
        // Scan thru "_C__" looking for i1, i2
        for (int i = 0; i < temp.length(); i ++)
        {
            String currChar = temp.substring(i, i+1);
            if (!currChar.equals("_") && i1 == 99 && i2 == 99)
                i1 = i;
            else
            if ( currChar.equals("_") && i1 != 99 && i2 == 99)
                i2 = i;
        }
        
        if (i2 == 99)
            return temp.substring(i1);
        else
            return temp.substring(i1, i2);
    }
//}}}
    
//{{{ angleImpl
//##############################################################################
    protected void angleImpl(Measurement meas, Model model, ModelState state, Residue res, double val, double ideal)
    {
	    Measurement.Angle a = (Measurement.Angle) meas;
        AtomSpec atomSpec1 = a.getA();
        AtomSpec atomSpec2 = a.getB();
        AtomSpec atomSpec3 = a.getC();
        
        // Get AtomStates with resOffset and atomName
	    AtomState as1 = atomSpec1.get(model, state, res);
	    AtomState as2 = atomSpec2.get(model, state, res);
	    AtomState as3 = atomSpec3.get(model, state, res);
	    
	    String devName = label.substring(0,4)+" "+res.getName()+" "+
		    res.getSequenceInteger()+" "+as1.getName().trim()+"-"+
            as2.getName().trim()+"-"+as3.getName().trim();
        
	    //addLine(devName, (Triple) as1, (Triple) as2, (Triple) as3, val, ideal);
        //addBlurredLine(devName, (Triple) as1, (Triple) as2, (Triple) as3, val, ideal);
        addLineAndBlurredLine(devName, (Triple) as1, (Triple) as2, (Triple) as3, val, ideal);
    }
//}}}

//{{{ addBall
//##############################################################################
    /**
    * Adds distance outlier indicator to distList.
    **/
    public void addBall(String name, Triple trp1, Triple trp2, double val, double ideal) 
    {
	    double devValue = val-ideal;
        String color = "red";
	    if (devValue < 0) 
	    {
		    color = "blue";
            devValue = - devValue;
	    }
	    
        // set ball's position (x, y, z) next to atom #1
        Triple origVector = new Triple().likeVector(trp1, trp2);
	    origVector = origVector.mult(ideal/val).add(trp1);
	    double x =origVector.getX();
        double y =origVector.getY();
        double z =origVector.getZ();
        
        String line1 = "@balllist {"+name+"} color= "+color+" radius= "+
            df1.format(devValue)+" master= {dist devs}";
        String line2 = "{"+name+"}L "+
            df2.format(x)+" "+df2.format(y)+" "+df2.format(z);
        
        distList.add(line1);
        distList.add(line2);
    }
//}}}

//{{{ addBallInMiddle
//##############################################################################
/**
* Adds distance outlier indicator to kin.
**/
    public void addBallInMiddle(String name, Triple trp1, Triple trp2, double val, double ideal) 
	{
        double devValue = val-ideal;
        String color = "red";
	    if (devValue < 0) 
	    {
		    color = "blue";
		    devValue = - devValue;
	    }
		
		// set ball's position (x, y, z) at midpoint between two atoms
		Triple midpoint = new Triple().likeMidpoint(trp1, trp2);
		double x = midpoint.getX();
		double y = midpoint.getY();
        double z = midpoint.getZ();
		
		String line1 = "@balllist {"+name+"} color= "+color+" radius= "+
            df1.format(devValue)+" master= {dist devs}";
        String line2 = "{"+name+"}L "+
            df2.format(x)+" "+df2.format(y)+" "+df2.format(z);
        
        distList.add(line1);
        distList.add(line2);
    }
//}}}

//{{{ addSpring
//##############################################################################
/**
* Adds distance outlier indicator to kin.
**/
    public void addSpring(String name, Triple trp1, Triple trp2, double val, double ideal)
	{
        //distList.add("@balllist {trp1, trp2} color= cyan radius= "+
        //    df1.format(0.1)+" master= {dist devs}");
        //distList.add("{trp1}L "+
        //    df2.format(trp1.getX())+" "+df2.format(trp1.getY())+" "+df2.format(trp1.getZ()));
        //distList.add("{trp2}L "+
        //    df2.format(trp2.getX())+" "+df2.format(trp2.getY())+" "+df2.format(trp2.getZ()));
        
        Triple trp1Orig = new Triple(trp1.getX(), trp1.getY(), trp1.getZ());
        
        ArrayList<String> lines = new ArrayList<String>();
        
        // Set initial spring pos
        Triple v12 = new Triple(trp2.getX() - trp1.getX(), 
                                trp2.getY() - trp1.getY(), 
                                trp2.getZ() - trp1.getZ() );
        
        Triple trpHalf = v12.mult(0.5);
        Triple bondNormal = new Triple().likeNormal(trp1, trpHalf, trp2);
        
        // (any vector starting at trp1 should work here; we just want a normal to the bond)
        
        // Work at origin from here on out; add trp1 for kin output
        bondNormal.mult(0.2 + 0.25 * Math.abs(val - ideal));
        
        String  	        color = "red";
		if (val-ideal < 0)	color = "blue";
        
        lines.add("@vectorlist {"+name+"} color= "+color+" width= "
                +3+" master= {dist devs}");
        lines.add("{"+name+"}P "+df2.format(trp1.getX())+" "+
                                 df2.format(trp1.getY())+" "+
                                 df2.format(trp1.getZ()));
        Triple posOrig = new Triple(trp1.add(bondNormal).getX(), 
                                    trp1.add(bondNormal).getY(), 
                                    trp1.add(bondNormal).getZ() );
        
        // Move along bond axis and make the spring
        int n = 100;
        double ang = 0;
        double dist = new Triple(trp2.getX()-trp1.getX(), 
                                 trp2.getY()-trp1.getY(), 
                                 trp2.getZ()-trp1.getZ() ).mag();
        Triple pos = bondNormal;
        Triple nextSpringPoint;
        ang = 30;//(6*Math.PI) / n;
        for (int i = 0; i < n; i ++)
        {
            if (pos.mag() < dist - 0.2)
            {
                trp1 = new Triple(trp1Orig.getX(), trp1Orig.getY(), trp1Orig.getZ()); // reset trp1
                v12 = new Triple().likeVector(trp1, trp2);                            // reset v12
                
                // Rotate around atom1-atom2 axis
                Transform rotate = new Transform();
                rotate = rotate.likeRotation(v12, ang);
                rotate.transform(pos);
                
                // Move toward atom2 a bit
                pos.add( v12.mult(dist/n) );
                
                nextSpringPoint = trp1.add(pos).add(v12.mult(i*(val/n)));
                
                lines.add("{"+name+"}L "+df2.format(nextSpringPoint.getX())+" "+
                                         df2.format(nextSpringPoint.getY())+" "+
                                         df2.format(nextSpringPoint.getZ()) );
            }
        }
        lines.add("{"+name+"}L "+df2.format(trp2.getX())+" "+
                                 df2.format(trp2.getY())+" "+
                                 df2.format(trp2.getZ()) );
        
        // Output
        for (String line : lines)
            distList.add(line);
    }
//}}}

//{{{ addLine
//##############################################################################
/**
* Adds angle outlier indicator to angList.
**/
    public void addLine(String name, Triple trp1, Triple trp2, Triple trp3, double val, double ideal) 
	{
		// Calculate normal to atoms 1-3 and place at origin
		// Purpose: set up rotation matrix
		Triple normal = new Triple().likeNormal(trp1, trp2, trp3);
		
		// Set up said rotation matrix, which will rotate around the normal 
		// by the angle given by (diff btw idea and actual angle)
		Transform rotate = new Transform();
        rotate = rotate.likeRotation(normal, val-ideal);
		
		// Make new vector from atom 2 (which will not move during rotation)
		// to atom 3 (which will move), but shifted to origin
		Triple point = new Triple(trp3.getX() - trp2.getX(),
                                  trp3.getY() - trp2.getY(),
                                  trp3.getZ() - trp2.getZ());
        
		// Actually do the rotation, then move vector's tail back to atom 2
		rotate.transform(point);
        Triple v = new Triple(trp2.getX() + point.getX(),
                              trp2.getY() + point.getY(),
                              trp2.getZ() + point.getZ());
        
        // Output
        String              color = "red";
		if (val-ideal < 0)  color = "blue";
        
        String line1 = "@vectorlist {"+name+"} color= "+color+" width= 4"
            +" master= {angle devs}";
        String line2 = "{"+name+"}P "+df2.format(trp2.getX())+" "+
                                      df2.format(trp2.getY())+" "+
                                      df2.format(trp2.getZ());
        String line3 = "{"+name+"}L "+df2.format(v.getX())+" "+
                                      df2.format(v.getY())+" "+
                                      df2.format(v.getZ());
        angList.add(line1);
        angList.add(line2);
        angList.add(line3);
	}
//}}}

//{{{ addBlurredLine
//##############################################################################
/**
* Adds angle outlier indicator to angList.
**/
    public void addBlurredLine(String name, Triple trp1, Triple trp2, Triple trp3, double val, double ideal) 
	{
		// Calculate normal to atoms 1-3; automatically placed at origin
		// Purpose: set up rotation matrices
		Triple normal = new Triple().likeNormal(trp1, trp2, trp3);
		
		// Set up said rotation matrices, which will rotate around the normal 
		// by the angles given by dividing the (diff btw idea and actual angle) 
        // into quarters
		Transform rotate1 = new Transform();
        rotate1 = rotate1.likeRotation(normal, (val-ideal) * 1.00);
        Transform rotate2 = new Transform();
        rotate2 = rotate2.likeRotation(normal, (val-ideal) * 0.75);
        Transform rotate3 = new Transform();
        rotate3 = rotate3.likeRotation(normal, (val-ideal) * 0.50);
        Transform rotate4 = new Transform();
        rotate4 = rotate4.likeRotation(normal, (val-ideal) * 0.25);
		
		// Make new vectors from atom 2 (which will not move during rotation)
		// to atom 3 (which will move), but shifted to origin
		Triple point1 = new Triple().likeVector(trp2, trp3);
        Triple point2 = new Triple().likeVector(trp2, trp3);
        Triple point3 = new Triple().likeVector(trp2, trp3);
        Triple point4 = new Triple().likeVector(trp2, trp3);
        
		// Actually do the rotations, then move vectors' tails back to atom 2
		rotate1.transform(point1);
        rotate2.transform(point2);
        rotate3.transform(point3);
        rotate4.transform(point4);
        
        Triple v1 = point1.mult(0.75).add(trp2);
        Triple v2 = point2.mult(0.75).add(trp2);
        Triple v3 = point3.mult(0.75).add(trp2);
        Triple v4 = point4.mult(0.75).add(trp2);
        
        // Output
        String  	        color = "red";
		if (val-ideal < 0)	color = "blue";
        
        ArrayList<Triple> v1234 = new ArrayList<Triple>();
        v1234.add(v1);
        v1234.add(v2);
        v1234.add(v3);
        v1234.add(v4);
        for (int i = 0; i < v1234.size(); i ++)
        {
            String line1 = "@vectorlist {"+name+"} color= "+color+" width= "
                +(4-i)+" master= {angle devs}";
            Triple v = v1234.get(i);
            String line2 = "{"+name+"}P "+df2.format(trp2.getX())+" "+
                                          df2.format(trp2.getY())+" "+
                                          df2.format(trp2.getZ());
            String line3 = "{"+name+"}L "+df2.format(v.getX())+" "+
                                          df2.format(v.getY())+" "+
                                          df2.format(v.getZ());
            angList.add(line1);
            angList.add(line2);
            angList.add(line3);
        }
	}
//}}}

//{{{ addLineAndBlurredLine
//##############################################################################
/**
* Adds angle outlier indicator to angList.
**/
    public void addLineAndBlurredLine(String name, Triple trp1, Triple trp2, Triple trp3, double val, double ideal) 
	{
		// Make first, non-blurred line
        String  	        color = "red";
		if (val-ideal < 0)	color = "blue";
        
        Triple v21 = new Triple().likeVector(trp2, trp1);
        Triple v21short = v21.mult(0.75).add(trp2);
        
        String line1 = "@vectorlist {"+name+"} color= "+color+" width= "
                +4+" master= {angle devs}";
        String line2 = "{"+name+"}P "+df2.format(v21short.getX())+" "+
                                      df2.format(v21short.getY())+" "+
                                      df2.format(v21short.getZ());
        String line3 = "{"+name+"}L "+df2.format(trp2.getX())+" "+
                                      df2.format(trp2.getY())+" "+
                                      df2.format(trp2.getZ());
        angList.add(line1);
        angList.add(line2);
        angList.add(line3);
        
        // Make second, blurred line
        addBlurredLine(name, trp1, trp2, trp3, val, ideal);
	}
//}}}

//{{{ printModel
//##############################################################################
    public void printModel(Model mod)
    {
        if (doKinHeadings)
        {
            System.out.println("@kinemage {"+label.substring(0,4)+" geom devs}");
            System.out.println("@master {dist devs}");
            System.out.println("@master {angle devs}");
        }
        
        if (subgroupNotGroup)
            System.out.println("@subgroup {"+label.substring(0,4)+" "+
                mod.getName()+" geom devs} dominant master= {all models}");
        else
            System.out.println("@group {"+label.substring(0,4)+" "+
                mod.getName()+" geom devs} dominant master= {all models}");
        
        // Print geom outlier viz's
        if (doDistDevsKin)
            for (String distLine : distList)    System.out.println(distLine);
        if (doAngleDevsKin)
            for (String angLine : angList)      System.out.println(angLine);
        
        if (distList.size() == 0)   System.out.println("<distList is empty>");
        if (angList.size() == 0)    System.out.println("<angList is empty>");
    }
//}}}

}//class

