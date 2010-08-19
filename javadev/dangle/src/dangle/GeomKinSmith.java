// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package dangle;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.parser.*;
//}}}
/**
* <code>GeomKinSmith</code> takes input from geometry deviation information from 
* Dangle and makes nice visualizations in kinemage form of said deviations.
*/
public class GeomKinSmith //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    final DecimalFormat df1 = new DecimalFormat("0.##");
    final DecimalFormat df2 = new DecimalFormat("#.###");
    final DecimalFormat df3 = new DecimalFormat("#.##");
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Kinemage @balllist, @vectorlist, or point Strings for the current model */
    ArrayList<String> lengths, angles, cbdevs, peptides; //, pperps;
    Measurement[] meas;
    String label;
    CoordinateFile coords;
    boolean subgroup; // @subgroup as opposed to @group
    boolean doHets;
    double sigmaCutoff;
    TreeSet resnums;
    String altConf;
//}}}

//{{{ Constructor
//##############################################################################
    public GeomKinSmith(String l, CoordinateFile c, ArrayList<Measurement> m, double sc, boolean sg, boolean dh, TreeSet<Integer> rn, String ac)
    {
        label = l;
        coords = c;
        meas = (Measurement[]) m.toArray(new Measurement[m.size()]);
        sigmaCutoff = sc;
        subgroup = sg;
        doHets = dh;
        resnums = rn;
        altConf = ac;
    }
//}}}

//{{{ makeKin
//##############################################################################
    /**
    * Look through models in <code>coords</code>.  For each residue that is a
    * bond length length or angle outlier, add a visualization in the form of 
    * kinemage points to the proper global ArrayList.
    */
    public void makeKin() throws IllegalArgumentException
    {
        for(Iterator models = coords.getModels().iterator(); models.hasNext(); )
        {
            Model model = (Model) models.next();
            ModelState state = (altConf == null ? model.getState() : model.getState(altConf));
            if(state == null) throw new IllegalArgumentException(
                "Input structure "+coords.getIdCode()+" [model "+model+
                "] does not contain a state named '"+altConf+"'!");
            
            lengths = new ArrayList<String>();
            angles  = new ArrayList<String>();
            cbdevs  = new ArrayList<String>();
            peptides = new ArrayList<String>();
            
            for(Iterator residues = model.getResidues().iterator(); residues.hasNext(); )
            {
                Residue res = (Residue) residues.next();
                if(resnums == null || resnums.contains(res.getSequenceInteger()))
                {
                    boolean print = true;
                    if(print)
                    {
                        for(int i = 0; i < meas.length; i++)
                        {
                            if(meas[i] instanceof Measurement.Group)
                            {
                                Measurement.Group measurement = (Measurement.Group) meas[i];
                                groupOutput(model, state, res, measurement);
                            }
                            else // Measurement.Distance, Measurement.Angle, etc.
                            {
                                Measurement measurement = meas[i];
                                nonGroupOutput(model, state, res, measurement);
                            }
                        }
                    }
                }
            }
            
            printModel(model);
        }
    }
//}}}

//{{{ group/nonGroupOutput
//##############################################################################
    public void groupOutput(Model model, ModelState state, Residue res, Measurement.Group measurement)
    {
        boolean doneWithGroup = false;
        for(Measurement m : (ArrayList<Measurement>) measurement.group) // variable in Measurement.Group
        {
            double val = m.measure(model, state, res, doHets);
            double dev = m.getDeviation();
            double ideal = m.mean;
            if(!Double.isNaN(val) && !doneWithGroup)
            {
                // We've found the valid Measurement in this Group
                doneWithGroup = true;
                if(Double.isNaN(ideal)) // std devs not defined
                {
                    if(m.getType().equals(Measurement.TYPE_DISTANCE) && m.getLabel().equals("cbdev"))
                        cbDevImpl(m, model, state, res, val);
                    else if(m.getType().equals(Measurement.TYPE_BASEPPERP))
                        pPerpImpl(m, model, state, res, val);
                }
                else if(!Double.isNaN(dev) && Math.abs(dev) >= sigmaCutoff) // std devs defined
                {
                    if(m.getType().equals(Measurement.TYPE_DISTANCE))
                        lengthImpl(m, model, state, res, val, ideal, dev);
                    else if(m.getType().equals(Measurement.TYPE_ANGLE))
                        angleImpl(m, model, state, res, val, ideal, dev);
                }
            }
        }
    }

    public void nonGroupOutput(Model model, ModelState state, Residue res, Measurement m)
    {
        double val = m.measure(model, state, res, doHets);
        double dev = m.getDeviation();
        double ideal = m.mean;
        if(Double.isNaN(ideal)) // std devs not defined
        {
            if(m.getType().equals(Measurement.TYPE_DISTANCE) && m.getLabel().equals("cbdev"))
                cbDevImpl(m, model, state, res, val);
            else if(m.getType().equals(Measurement.TYPE_BASEPPERP))
                pPerpImpl(m, model, state, res, val);
            else if(m.getType().equals(Measurement.TYPE_DIHEDRAL))
              peptideImpl(m, model, state, res, val);
        }
        else if(!Double.isNaN(dev) && Math.abs(dev) >= sigmaCutoff) // std devs defined
        {
            if(m.getType().equals(Measurement.TYPE_DISTANCE))
                lengthImpl(m, model, state, res, val, ideal, dev);
            else if(m.getType().equals(Measurement.TYPE_ANGLE))
                angleImpl(m, model, state, res, val, ideal, dev);
            else if(m.getType().equals(Measurement.TYPE_DIHEDRAL))
              peptideImpl(m, model, state, res, val);
        }
    }
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
        for(int i = 0; i < temp.length(); i ++)
        {
            String currChar = temp.substring(i, i+1);
            if(!currChar.equals("_") && i1 == 99 && i2 == 99)
                i1 = i;
            else
            if( currChar.equals("_") && i1 != 99 && i2 == 99)
                i2 = i;
        }
        
        if(i2 == 99)
            return temp.substring(i1);
        else
            return temp.substring(i1, i2);
    }
//}}}

//{{{ lengthImpl
//##############################################################################
    protected void lengthImpl(Measurement meas, Model model, ModelState state, Residue res, double val, double ideal, double dev)
    {
	    Measurement.Distance d = (Measurement.Distance) meas;
        AtomSpec atomSpec1 = (AtomSpec) d.getA();
        AtomSpec atomSpec2 = (AtomSpec) d.getB();
        
        String atomSpec1Name = getName(atomSpec1);
        String atomSpec2Name = getName(atomSpec2);
        
        // Get correct Residues using Measurement's resOffset values
        Residue res1 = atomSpec1.getRes(model, state, res);
        Residue res2 = atomSpec2.getRes(model, state, res);
        
        if(atomSpec1.getResOffset() == -1)      res1 = res.getPrev(model);
        if(atomSpec1.getResOffset() ==  1)      res1 = res.getNext(model);
        if(atomSpec2.getResOffset() == -1)      res2 = res.getPrev(model);
        if(atomSpec2.getResOffset() ==  1)      res2 = res.getNext(model);
        
        // Use correct Residues, look thru their Atoms, and get both Atoms
        // involved in this Distance Measurement
        Atom atom1 = null;
        Atom atom2 = null;
        Iterator i1 = (res1.getAtoms()).iterator();
        while (i1.hasNext())
        {
            Atom a = (Atom) i1.next();
            
            if( (a.getName().trim()).equals( atomSpec1Name.trim()) ||
                (a.getName().trim()).equals((atomSpec1Name.trim()).replace('*', '\'')) ||
                (a.getName().trim()).equals((atomSpec1Name.trim()).replace('\'', '*')) )
            {
                atom1 = a;
            }
        }        
        Iterator i2 = (res2.getAtoms()).iterator();    
        while (i2.hasNext())
        {
            Atom a = (Atom) i2.next();
            
            if( (a.getName().trim()).equals( atomSpec2Name.trim()) ||
                (a.getName().trim()).equals((atomSpec2Name.trim()).replace('*', '\'')) ||
                (a.getName().trim()).equals((atomSpec2Name.trim()).replace('\'', '*')) )
            {
                atom2 = a;
            }
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
        catch(AtomException ae) 
        { System.err.println("Couldn't get AtomState"); }
        
        String devName = label.substring(0,4)+" "+res1.getName()+res1.
            getSequenceInteger()+"-"+res2.getName()+res2.getSequenceInteger()
            +" "+atom1.getName().trim()+"-"+atom2.getName().trim()+" "+df2.format(dev)+" sigma";
        
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
        addSpring(devName, atomState1Triple, atomState2Triple, val, ideal, dev);
        
        //{{{ [for debugging]
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
        //    if((a.getName().trim()).equals("C"))
        //        res1C = a;
        //}       
        //Iterator iter2 = (res2.getAtoms()).iterator();    
        //while (iter2.hasNext())
        //{
        //    Atom a = (Atom) iter2.next();
        //    if((a.getName().trim()).equals("N"))
        //        res2N = a;
        //}
        //iter1 = (res1.getAtoms()).iterator();
        //while (iter1.hasNext())
        //{
        //    Atom a = (Atom) iter1.next();
        //    if((a.getName().trim()).equals("N"))
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
        //if(res1.equals(res2))
         //   System.out.println("res1 equals res2");
        //if(res1Cstate == null) System.out.println("res1Cstate == null");
        //else                    System.out.println("res1Cstate != null");
        //if(res2Nstate == null) System.out.println("res2Nstate == null");
        //else                    System.out.println("res2Nstate != null");
        //if(res1Nstate == null) System.out.println("res1Nstate == null");
        //else                    System.out.println("res1Nstate != null");
        //System.out.println("res1 = "+res1.toString());
        //System.out.println("res1.getName() = "+res1.getName());
        //System.out.println("res1.getSequenceInteger() = "+res1.getSequenceInteger());
        //System.out.println("label = "+label);
        //System.out.println("atomState1.getName().trim() = "+atomState1.getName().trim());
        //System.out.println("atomState2.getName().trim() = "+atomState2.getName().trim());
        //}}}
    }
//}}}

//{{{ addSpring
//##############################################################################
    /**
    * Adds a "spring" bond length outlier indicator to <code>lengths</code>.
    **/
    public void addSpring(String name, Triple trp1, Triple trp2, double val, double ideal, double dev)
	{
        Triple trp1Orig = new Triple(trp1.getX(), trp1.getY(), trp1.getZ());
        
        ArrayList<String> lines = new ArrayList<String>();
        
        // Set initial spring pos
        Triple v12 = new Triple(trp2.getX() - trp1.getX(), 
                                trp2.getY() - trp1.getY(), 
                                trp2.getZ() - trp1.getZ() );
        Triple xAxisUnitVector = new Triple(1, 0, 0);
        Triple bondNormal = new Triple().likeNormal(trp1, trp2, xAxisUnitVector);
        // (any vector starting at trp1 should work here; we just want a normal to the bond)
        
        // Work at origin from here on out; will add trp1 for kin output
        bondNormal.mult(0.2);
        
        String  	        color = "red";
		if(val-ideal < 0)	color = "blue";
        
        lines.add("@vectorlist {"+name+"} color= "+color+" width= "+3+" master= {length dev}");
        lines.add("{"+name+"}P U "+df2.format(trp1.getX())+" "+
                                   df2.format(trp1.getY())+" "+
                                   df2.format(trp1.getZ()));
        lines.add("{"+name+"}  U "+df2.format(trp1.getX()+bondNormal.getX())+" "+
                                   df2.format(trp1.getY()+bondNormal.getY())+" "+
                                   df2.format(trp1.getZ()+bondNormal.getZ()));
        
        double posOrigX = trp1.getX() + bondNormal.getX();
        double posOrigY = trp1.getY() + bondNormal.getY();
        double posOrigZ = trp1.getZ() + bondNormal.getZ();
        Triple posOrig = new Triple(posOrigX, posOrigY, posOrigZ);
                                    //trp1.add(bondNormal).getX(), 
                                    //trp1.add(bondNormal).getY(), 
                                    //trp1.add(bondNormal).getZ() );
        
        // Set up angle of rotation for each point in spring
        
        // Note that #turns = ang/6, so...
        //    for dev = 0,   #turns = 36/6 = 6
        //       (theoretically... wouldn't be shown unless -sigma=0.0 flag was used)
        //    for dev = 4,   #turns = (36-1.5*4)/6 = (36-6)/6 = 30/6 = 5
        //    for dev = -4,  #turns = (36+1.5*4)/6 = (36+6)/6 = 42/6 = 7
        //    for dev = 6,   #turns = (36-1.5*6)/6 = (36-9)/6 = 27/6 = 4.333
        //    for dev = 8,   #turns = (36-1.5*8)/6 = (36-12)/6 = 24/6 = 4
        //    for dev = 10,  #turns = (36-1.5*10)/6 = (36-15)/6 = 21/6 = 3.5
        //       (cap for #turns)
        //    for dev = -10, #turns = (36+1.5*10)/6 = (36+15)/6 = 51/6 = 8.5
        //       (cap for #turns)
        // So, theoretical range for #turns is 6 +/- 2.5 (i.e. 3.5 to 8.5)
        // and actual range for #turns using sigmaCutoff of 4 is (3.5 to 5)U(6 to 8.5)
        
        // In summary: +/- 90deg (i.e. quarter turn of spring) per sigma
        
        double ang = 36;
        if(dev >  10) dev =  10;
        if(dev < -10) dev = -10;
        if(dev <= 0)
        {
            // actual dist < ideal --> scrunched up --> bigger angle
            // further exaggerate w/ 2nd term: for scrunched up dists, angles get even bigger
            // (dev in sigma; usually in range of -10 < dev < 10)
            ang += 1.5*Math.abs(dev);
        }
        else if(dev > 0)
        {
            // actual dist > ideal --> stretched out --> smaller angle
            // further exaggerate w/ 2nd term: for stretched up dists, angles get even smaller
            // (dev in sigma; usually in range of -10 < dev < 10)
            ang -= 1.5*dev;
        }
        
        // Move along bond axis and make the spring
        int n = 60; // 60 pts = 6 turns * 10 points/turn
                    // (and 10 points/turn --> ang = 360/10 = 36deg/point --> that's why 
                    // I used a default of ang=36 above)
        double dist = new Triple(trp2.getX()-trp1.getX(), 
                                 trp2.getY()-trp1.getY(), 
                                 trp2.getZ()-trp1.getZ() ).mag();
        Triple pos = bondNormal;
        Triple nextSpringPoint;
        for(int i = 1; i <= n; i ++)
        {
            trp1 = new Triple(trp1Orig.getX(), trp1Orig.getY(), trp1Orig.getZ()); // reset trp1
            v12 = new Triple().likeVector(trp1, trp2);                            // reset v12
            
            // Rotate around atom1-atom2 axis
            Transform rotate = new Transform();
            rotate = rotate.likeRotation(v12, ang);
            rotate.transform(pos);
            
            // Move toward atom2 a bit
            Triple stepTowardsTrp2 = new Triple(v12.getX(), v12.getY(), v12.getZ());
            stepTowardsTrp2.div(n);
            pos.add(stepTowardsTrp2); 
            
            nextSpringPoint = trp1.add(pos);
            
            lines.add("{"+name+"} U "+df2.format(nextSpringPoint.getX())+" "+
                                      df2.format(nextSpringPoint.getY())+" "+
                                      df2.format(nextSpringPoint.getZ()) );
        }
        lines.add("{"+name+"} U "+df2.format(trp2.getX())+" "+
                                  df2.format(trp2.getY())+" "+
                                  df2.format(trp2.getZ()) );
        
        // Output
        for(String line : lines) lengths.add(line);
    }
//}}}

//{{{ angleImpl
//##############################################################################
    protected void angleImpl(Measurement meas, Model model, ModelState state, Residue res, double val, double ideal, double dev)
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
            as2.getName().trim()+"-"+as3.getName().trim()+" "+df2.format(dev)+" sigma";
        
	    //addLine(devName, (Triple) as1, (Triple) as2, (Triple) as3, val, ideal);
        //addBlurredLine(devName, (Triple) as1, (Triple) as2, (Triple) as3, val, ideal);
        addFan(devName, (Triple) as1, (Triple) as2, (Triple) as3, val, ideal);
    }
//}}}

//{{{ addFan
//##############################################################################
    /**
    * Adds a "fan" bond angle outlier indicator to <code>angles</code>: 
    * a thick line for the ideal value and increasingly thinner lines 
    * approaching the actual value from the ideal value.
    **/
    public void addFan(String name, Triple trp1, Triple trp2, Triple trp3, double val, double ideal) 
	{
		// Make first, non-blurred line
        String  	        color = "red";
		if(val-ideal < 0)	color = "blue";
        
        Triple v21 = new Triple().likeVector(trp2, trp1);
        Triple v21short = v21.mult(0.75).add(trp2);
        
        String line1 = "@vectorlist {"+name+"} color= "+color+" width= "+4+" master= {angle dev}";
        String line2 = "{"+name+"}P U "+df2.format(v21short.getX())+" "+
                                        df2.format(v21short.getY())+" "+
                                        df2.format(v21short.getZ());
        String line3 = "{"+name+"}  U "+df2.format(trp2.getX())+" "+
                                        df2.format(trp2.getY())+" "+
                                        df2.format(trp2.getZ());
        angles.add(line1);
        angles.add(line2);
        angles.add(line3);
        
        // Make second, blurred line
        addBlur(name, trp1, trp2, trp3, val, ideal);
	}
//}}}

//{{{ addBlur
//##############################################################################
    /**
    * Adds the thin part of a "fan" bond angle outlier indicator to <code>angles</code>: 
    * increasingly thinner lines approaching the actual value from the ideal value.
    **/
    public void addBlur(String name, Triple trp1, Triple trp2, Triple trp3, double val, double ideal) 
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
		if(val-ideal < 0)	color = "blue";
        
        ArrayList<Triple> v1234 = new ArrayList<Triple>();
        v1234.add(v1);
        v1234.add(v2);
        v1234.add(v3);
        v1234.add(v4);
        for(int i = 0; i < v1234.size(); i ++)
        {
            String line1 = "@vectorlist {"+name+"} color= "+color+" width= "+(4-i)+" master= {angle dev}";
            Triple v = v1234.get(i);
            String line2 = "{"+name+"}P U "+df2.format(trp2.getX())+" "+
                                            df2.format(trp2.getY())+" "+
                                            df2.format(trp2.getZ());
            String line3 = "{"+name+"}  U "+df2.format(v.getX())+" "+
                                            df2.format(v.getY())+" "+
                                            df2.format(v.getZ());
            angles.add(line1);
            angles.add(line2);
            angles.add(line3);
        }
	}
//}}}

//{{{ cbDevImpl
//##############################################################################
    protected void cbDevImpl(Measurement meas, Model model, ModelState state, Residue res, double val)
    {
	    double cbdevCutoff = 0.25; // from Rama/Calpha paper (Lovell et al., Proteins, 2003)
        if(val > cbdevCutoff)
        {
            Measurement.Distance cbdev = (Measurement.Distance) meas;
            
            // Actual
            AtomSpec  actualSpec = (AtomSpec)  cbdev.getA();
            AtomState actual     = (AtomState) actualSpec.get(model, state, res);
            
            // Ideal
            XyzSpec.IdealTetrahedral idealSpec = (XyzSpec.IdealTetrahedral) cbdev.getB();
            Triple                   ideal     = (Triple)                   idealSpec.get(model, state, res);
            
            String devName = label.substring(0,4)+" "+res+" Cbeta dev "+df3.format(val)+"A";
            
            addBall(devName, ideal, val);
        }
    }
//}}}

//{{{ addBall
//##############################################################################
    /**
    * Adds a "ball" Cbeta deviation outlier indicator to <code>cbdevs</code>: 
    * a kinemage ball centered at the ideal Cbeta position with radius equal 
    * to the ideal-actual distance.
    **/
    protected void addBall(String name, Triple ideal, double val)
    {
        String cbdev1 = "@balllist {"+name+"} color= magenta radius= "+df2.format(val)+" master= {Cbeta dev}";
        String cbdev2 = "{"+name+"} "+df2.format(ideal.getX())+" "+
                                        df2.format(ideal.getY())+" "+
                                        df2.format(ideal.getZ());
        cbdevs.add(cbdev1);
        cbdevs.add(cbdev2);
    }
//}}}

//{{{ pPerpImpl
//##############################################################################
    protected void pPerpImpl(Measurement meas, Model model, ModelState state, Residue res, double val)
    {
	    throw new IllegalArgumentException("Base-phosphate perpendicular visualizations not yet implemented!");
    }
//}}}

  //{{{ peptideImpl
  //##############################################################################
  protected void peptideImpl(Measurement meas, Model model, ModelState state, Residue res, double val) {
    Measurement.Dihedral d = (Measurement.Dihedral) meas;
    double dihed = d.measureImpl(model, state, res);
    if ((dihed < 170)&&(dihed > -170)) {
      AtomSpec atA = d.getA();
      AtomSpec atB = d.getB();
      AtomSpec atC = d.getC();
      AtomSpec atD = d.getD();
      AtomState asA = atA.get(model, state, res);
	    AtomState asB = atB.get(model, state, res);
	    AtomState asC = atC.get(model, state, res);
	    AtomState asD = atD.get(model, state, res);
      
	    String pointId = label.substring(0,4)+" "+res.getName()+" "+
      res.getSequenceInteger()+" peptide dihedral:"+df2.format(dihed);
      
      String line1 = "{"+pointId+"}P U "+df2.format(asA.getX())+" "+
                                        df2.format(asA.getY())+" "+
                                        df2.format(asA.getZ());
      String line2 = "{"+pointId+"}U "+df2.format(asC.getX())+" "+
                                        df2.format(asC.getY())+" "+
                                        df2.format(asC.getZ());
      String line3 = "{"+pointId+"}P U "+df2.format(asB.getX())+" "+
                                        df2.format(asB.getY())+" "+
                                        df2.format(asB.getZ());
      String line4 = "{"+pointId+"}U "+df2.format(asD.getX())+" "+
                                        df2.format(asD.getY())+" "+
                                        df2.format(asD.getZ());
      peptides.add(line1);
      peptides.add(line2);
      peptides.add(line3);
      peptides.add(line4);
    }
  }
  //}}}

//{{{ printModel
//##############################################################################
    public void printModel(Model mod)
    {
        System.out.println("@master {length dev} on");
        System.out.println("@master {angle dev} on");
        System.out.println("@master {peptide dev} on");
        System.out.println("@master {Cbeta dev} on");
        
        boolean multimodel = coords.getModels().size() > 1;
        System.out.println((subgroup ? "@subgroup" : "@group")+" {"
            +label.substring(0,4)+(multimodel ? " "+mod.getName() : "")
            +" geom devs} dominant"+(multimodel ? " master= {all models}" : ""));
        
        // Make sure the measurements we're visualzing were even considered 
        // before we mistakenly say there are no errors when there really are!
        boolean triedLngth = false;
        boolean triedAngle = false;
        boolean triedCbDev = false;
        //boolean triedPperp = false;
        boolean triedPepDev = false;
        for(int i = 0; i < meas.length; i++)
        {
            if(meas[i].getType().equals(Measurement.TYPE_DISTANCE)
            && meas[i].getLabel().equals("cbdev"))                       triedCbDev = true;
            else if(meas[i].getType().equals(Measurement.TYPE_DISTANCE)) triedLngth = true;
            else if(meas[i].getType().equals(Measurement.TYPE_ANGLE))    triedAngle = true;
            //else if(m.getType().equals(Measurement.TYPE_BASEPPERP))    triedPperp = true;
            else if(meas[i].getType().equals(Measurement.TYPE_DIHEDRAL)) triedPepDev = true;
        }
        
        if(!triedLngth)            System.err.println("(Didn't look for bond length outliers)");
        else if(lengths.isEmpty()) System.err.println("No bond length outliers (in selected residues)");
        else for(String lngthLine : lengths) System.out.println(lngthLine);
        
        if(!triedAngle)           System.err.println("(Didn't look for bond angle outliers)");
        else if(angles.isEmpty()) System.err.println("No bond angle outliers (in selected residues)");
        else for(String angleLine : angles) System.out.println(angleLine);
        
        if(!triedCbDev)           System.err.println("(Didn't look for Cbeta dev outliers)");
        else if(cbdevs.isEmpty()) System.err.println("No Cbeta dev outliers (in selected residues)");
        else for(String cbdevLine : cbdevs) System.out.println(cbdevLine);
        
        //if(!triedPperp)           System.err.println("(Didn't look for base-P perp outliers)");
        //else if(pperps.isEmpty()) System.err.println("No base-P perp outliers");
        //else for(String pperpLine : pperps) System.out.println(pperpLine);
        
        if(!triedPepDev)           System.err.println("(Didn't look for peptide outliers)");
        else if(peptides.isEmpty()) System.err.println("No peptide outliers (in selected residues)");
        else {
          System.out.println("@vectorlist {peptide outliers} color= green width= "+4+" master= {peptide dev}");
          for(String pepLine : peptides) System.out.println(pepLine);
        }
    }
//}}}
}//class
