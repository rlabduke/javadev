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
//}}}

//{{{ Constructor
//##############################################################################
    public GeomKinSmith(ArrayList<Measurement> m, String l, CoordinateFile c, boolean dist, boolean ang, boolean head, double sc)
    {
        meas = (Measurement[]) m.toArray(new Measurement[m.size()]);
        label = l;
        coords = c;
        doDistDevsKin = dist;
        doAngleDevsKin = ang;
        doKinHeadings = head;
        sigmaCutoff = sc;
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
                
                for(int i = 0; i < meas.length; i++)
                {
                    // See if this Measurement is a Group of potentially interesting 
                    // Measurements, or just a regualar individual Measurement
                    
                    Measurement.Group potentiallyAGroup = null;
                    try { potentiallyAGroup = (Measurement.Group) meas[i]; }
                    catch (java.lang.ClassCastException cce) { ; }
                    
                    if (potentiallyAGroup != null)
                    {
                        // meas[i] is a Measurement.Group (subclass of Measurement)
                        
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
                    
                    else
                    {
                        // meas[i] is NOT a Measurement.Group; probably a 
                        // regular Measurement.Distance or Measurement.Angle
                        
                        double val = meas[i].measure(model, state, res);
                        double dev = meas[i].getDeviation();
                        double ideal = meas[i].mean;
                        
                        if(!Double.isNaN(dev) && Math.abs(dev) >= sigmaCutoff)
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
                    
                }
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
        AtomSpec atomSpec1 = d.getA();
        AtomSpec atomSpec2 = d.getB();
        
        // Get AtomStates with resOffset and atomName
	    AtomState as1 = atomSpec1.get(model, state, res);
	    AtomState as2 = atomSpec2.get(model, state, res);
	    
	    String devName = label.substring(0,4)+" "+res.getName()+" "+
		    res.getSequenceInteger()+" "+as1.getName().trim()+"-"+
            as2.getName().trim();
        
	    //addBall(devName, (Triple) as1, (Triple) as2, val, ideal);
        addBallInMiddle(devName, (Triple) as1, (Triple) as2, val, ideal);
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
        addBlurredLine(devName, (Triple) as1, (Triple) as2, (Triple) as3, val, ideal);
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
		
		// Make new vector from atom 2 (which will not move during rotation)
		// to atom 3 (which will move), but shifted to origin
		Triple point1 = new Triple(trp3.getX() - trp2.getX(),
                                   trp3.getY() - trp2.getY(),
                                   trp3.getZ() - trp2.getZ());
        Triple point2 = new Triple(trp3.getX() - trp2.getX(),
                                   trp3.getY() - trp2.getY(),
                                   trp3.getZ() - trp2.getZ());
        Triple point3 = new Triple(trp3.getX() - trp2.getX(),
                                   trp3.getY() - trp2.getY(),
                                   trp3.getZ() - trp2.getZ());
        Triple point4 = new Triple(trp3.getX() - trp2.getX(),
                                   trp3.getY() - trp2.getY(),
                                   trp3.getZ() - trp2.getZ());
        
		// Actually do the rotation, then move vector's tail back to atom 2
		rotate1.transform(point1);
        Triple v1 = new Triple(trp2.getX() + point1.getX(),
                               trp2.getY() + point1.getY(),
                               trp2.getZ() + point1.getZ());
        rotate2.transform(point2);
        Triple v2 = new Triple(trp2.getX() + point2.getX(),
                               trp2.getY() + point2.getY(),
                               trp2.getZ() + point2.getZ());
        rotate3.transform(point3);
        Triple v3 = new Triple(trp2.getX() + point3.getX(),
                               trp2.getY() + point3.getY(),
                               trp2.getZ() + point3.getZ());
        rotate4.transform(point4);
        Triple v4 = new Triple(trp2.getX() + point4.getX(),
                               trp2.getY() + point4.getY(),
                               trp2.getZ() + point4.getZ());
        
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
        
        System.out.println("@group {"+label.substring(0,4)+" "+
            mod.getName()+"} dominant master= {all models}");
        
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

