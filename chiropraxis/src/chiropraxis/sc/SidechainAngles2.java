// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.sc;

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
import driftwood.util.*;
//}}}
/**
* <code>SidechainAngles2</code> allows for measuring and altering
* the dihedral angles in an amino acid side-chain.
* All three-letter codes are handled internally in lower case,
* but the appropriate conversions should be made automatically.
*
* This class uses driftwood.moldb2.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May  8 09:52:04 EDT 2003
*/
public class SidechainAngles2 //extends ... implements ...
{
//{{{ Constants
    /** Higher indices into this string are LESS remote */
    public static final String REMOTENESS = "HZEDGBA ";
//}}}

//{{{ Variable definitions
//##################################################################################################
    Set     knownAA;            // 3 letter codes for all known amino acids
    Map     chisPerAA;          // <code,Integer>
    Map     anglesForAA;        // <code,String[] of angle names>
    Map     atomsForAngle;      // <code.angle_name,String[] of atom names>
    Map     rotamersForAA;      // < code, Collection<RotamerDef> >
    
    // Used for working calculations
    Transform rot = new Transform();
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @throws IOException if the needed resource(s) can't be loaded from the JAR file
    * @throws NoSuchElementException if the resource is missing a required entry
    */
    public SidechainAngles2() throws IOException
    {
        loadData();
    }
//}}}

//{{{ loadData
//##################################################################################################
    /** @throws NoSuchElementException if required data is missing from resource file */
    private void loadData() throws IOException
    {
        // Load all properties from file
        Props props = new Props();
        InputStream is = this.getClass().getResourceAsStream("angles.props");
        if(is == null) throw new IllegalArgumentException("Couldn't find resource in JAR file");
        props.load(is);
        is.close();
        
        // Create data structures
        chisPerAA           = new UberMap();
        anglesForAA         = new UberMap();
        atomsForAngle       = new UberMap();
        rotamersForAA       = new UberMap();
        
        // Read in list of amino acids names and iterate thru them
        String[] aa = Strings.explode(props.getString("aminoacids"), ',');
        knownAA = new UberSet(Arrays.asList(aa));
        for(Iterator iter = knownAA.iterator(); iter.hasNext(); )
        {
            String aaname = (String)iter.next();
            loadAminoAcid(props, aaname);
        }
    }
//}}}

//{{{ loadAminoAcid
//##################################################################################################
    private void loadAminoAcid(Props props, String aaname)
    {
        // Find number of canonical chi angles for this type
        Integer numChiAngles = new Integer(props.getInt(aaname+".chis"));
        chisPerAA.put(aaname, numChiAngles);

        // Find list of all mobile angles
        String[] anglelist = Strings.explode(props.getString(aaname+".angles"), ',', false, true);
        anglesForAA.put(aaname, anglelist);
        
        // Load 4-atom definition for each mobile dihedral
        for(int i = 0; i < anglelist.length; i++)
        {
            String key = aaname+"."+anglelist[i];
            String[] atomlist = Strings.explode(props.getString(key), ',');
            atomsForAngle.put(key, atomlist);
        }
        
        // Load list of rotamers
        ArrayList   rotamerDefs = new ArrayList();
        String[]    rotamerlist = Strings.explode(props.getString(aaname+".rotamers"),          ',', false, true);
        String[]    rotaFreq    = Strings.explode(props.getString(aaname+".frequencies", ""),   ',', false, true);
        
        for(int i = 0; i < rotamerlist.length; i++)
        {
            try
            {
                double[] rotamerangles = explodeDoubles(props.getString(aaname+"."+rotamerlist[i]));
                RotamerDef def = new RotamerDef();
                def.rotamerName = rotamerlist[i];
                def.frequency   = (rotaFreq.length > i ? rotaFreq[i] : "?");
                def.chiAngles   = rotamerangles;
                rotamerDefs.add(def);
            }
            catch(NumberFormatException ex)
            { ex.printStackTrace(SoftLog.err); }
        }
        rotamersForAA.put(aaname, rotamerDefs);
    }
//}}}

//{{{ explodeDoubles
//##################################################################################################
    double[] explodeDoubles(String s) throws NumberFormatException
    {
        String[]    strings = Strings.explode(s, ' ');
        double[]    doubles = new double[strings.length];
        for(int i = 0; i < strings.length; i++)
            doubles[i] = Double.parseDouble(strings[i]);
        return doubles;
    }
//}}}

//{{{ count{Chi, All}Angles, nameAllAngles
//##################################################################################################
    /**
    * Returns the number of chi angles for the given 3-letter residue code,
    * or -1 if that residue is unknown to the system
    */
    public int countChiAngles(Residue res)
    {
        String rescode = res.getName().toLowerCase();
        Integer i = (Integer)chisPerAA.get(rescode);
        if(i == null) return -1;
        else return i.intValue();
    }
    
    /**
    * Returns the number of mobile angles for the given 3-letter residue code,
    * including mobile H (Ser OH, Met CH3, etc),
    * or -1 if that residue is unknown to the system
    */
    public int countAllAngles(Residue res)
    {
        String rescode = res.getName().toLowerCase();
        String[] angles = (String[])anglesForAA.get(rescode);
        if(angles == null) return -1;
        else return angles.length;
    }
    
    /**
    * Returns a String[] of all the named angles known for a given residue code,
    * or null if that residue is unknown.
    */
    public String[] nameAllAngles(Residue res)
    {
        String rescode = res.getName().toLowerCase();
        String[] angles = (String[])anglesForAA.get(rescode);
        if(angles == null) return null;
        else return (String[])angles.clone();
    }

//}}}

//{{{ measureAngle
//##################################################################################################
    /**
    * Measures the value of some named angle as a value between
    * -180 and +180 degrees
    * @throws IllegalArgumentException if operation cannot succeed
    * @throws AtomException if required atoms are missing
    */
    public double measureAngle(String angleName, Residue res, ModelState state)
    {
        String resDotAngle = (res.getName()+"."+angleName).toLowerCase();
        String[] atomNames = (String[])atomsForAngle.get(resDotAngle);
        if(atomNames == null || atomNames.length < 4)
            throw new IllegalArgumentException("Angle definition bad or not found for '"+resDotAngle+"'");

        AtomState a1, a2, a3, a4;
        a1 = state.get(res.getAtom(atomNames[0]));
        a2 = state.get(res.getAtom(atomNames[1]));
        a3 = state.get(res.getAtom(atomNames[2]));
        a4 = state.get(res.getAtom(atomNames[3]));

        return Triple.dihedral(a1, a2, a3, a4);
    }
//}}}

//{{{ areParentAndChild
//##################################################################################################
    protected boolean areParentAndChild(Atom parent, Atom child)
    {
        String p = parent.getName();
        String c = child.getName();
        if(p == null || c == null || p.length() != 4 || c.length() != 4)
            throw new IllegalArgumentException("Bad atom name(s)");
        
        // If statements are a workaround for seleno-Met,
        // which by convention is a HET and has no remoteness
        // indicator for the selenium atom.
        int pi = REMOTENESS.indexOf(p.charAt(2));
        if(parent.getResidue().getName().equals("MSE") && p.equals("SE  ")) pi = REMOTENESS.indexOf('D');
        int ci = REMOTENESS.indexOf(c.charAt(2));
        if( child.getResidue().getName().equals("MSE") && c.equals("SE  ")) ci = REMOTENESS.indexOf('D');
        
        return
        ((pi > ci && (p.charAt(3) == ' ' || p.charAt(3) == c.charAt(3)))    // parent closer AND on root or same branch
        || (pi == ci && p.charAt(3) == c.charAt(3)                          // OR child is an H of parent
            && p.charAt(1) != 'H' && c.charAt(1) == 'H'));
    }
//}}}

//{{{ setAngle
//##################################################################################################
    /**
    * Adjusts the model such that the appropriate atoms
    * are rotated about the named angle axis.
    * Sets the angle to some absolute value in degrees.
    * Use measureAngle() to learn the initial value, if needed.
    * @throws IllegalArgumentException if operation cannot succeed
    * @throws AtomException if required atoms are missing
    */
    public ModelState setAngle(String angleName, Residue res, ModelState state, double endAngle)
    {
        // Copied from measureAngle -- we need this info anyway...
        String resDotAngle = (res.getName()+"."+angleName).toLowerCase();
        String[] atomNames = (String[])atomsForAngle.get(resDotAngle);
        if(atomNames == null || atomNames.length < 4)
            throw new IllegalArgumentException("Angle definition bad or not found for '"+resDotAngle+"'");

        AtomState a1, a2, a3, a4;
        a1 = state.get(res.getAtom(atomNames[0]));
        a2 = state.get(res.getAtom(atomNames[1]));
        a3 = state.get(res.getAtom(atomNames[2]));
        a4 = state.get(res.getAtom(atomNames[3]));
        
        double startAngle = Triple.dihedral(a1, a2, a3, a4);
        double dTheta = endAngle - startAngle;
        rot.likeRotation(a2, a3, dTheta);
        
        ModelState ms = new ModelState(state);
        for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
        {
            Atom atom = (Atom)iter.next();
            a1 = state.get(atom);
            a2 = (AtomState)a1.clone();
            if( areParentAndChild(a3.getAtom(), atom) )
            {
                rot.transform(a2);
                ms.add(a2);
            }
        }
        
        return ms;
    }
//}}}

//{{{ measureAllAngles, setAllAngles
//##################################################################################################
    /** Angles that throw an AtomException (missing Atom/AtomState) are evaluated as Double.NaN */
    public double[] measureAllAngles(Residue res, ModelState state)
    {
        String rescode = res.getName().toLowerCase();
        String[] angles = nameAllAngles(res);
        if(angles == null) throw new IllegalArgumentException("Unknown residue type");
        
        double[] values = new double[ angles.length ];
        for(int i = 0; i < angles.length; i++)
        {
            try { values[i] = measureAngle(angles[i], res, state); }
            catch(AtomException ex) { values[i] = Double.NaN; }
        }
        
        return values;
    }
    
    /** Angles that are set to NaN are ignored. */
    public ModelState setAllAngles(Residue res, ModelState state, double[] values)
    {
        String rescode = res.getName().toLowerCase();
        String[] angles = nameAllAngles(res);
        if(angles == null)
            throw new IllegalArgumentException("Unknown residue type");
        if(values.length < angles.length)
            throw new IllegalArgumentException("Not enough angles specified");
        
        for(int i = 0; i < angles.length; i++)
        {
            if(!Double.isNaN(values[i]))
            {
                try { state = setAngle(angles[i], res, state, values[i]); }
                catch(IllegalArgumentException ex)  { ex.printStackTrace(SoftLog.err); }
                catch(AtomException ex)             { ex.printStackTrace(SoftLog.err); }
            }
        }
        
        return state;
    }
//}}}

//{{{ measureChiAngles, setChiAngles
//##################################################################################################
    /** Angles that throw an AtomException (missing Atom/AtomState) are evaluated as Double.NaN */
    public double[] measureChiAngles(Residue res, ModelState state)
    {
        String rescode = res.getName().toLowerCase();
        int angles = countChiAngles(res);
        if(angles < 0) throw new IllegalArgumentException("Unknown residue type");
        
        double[] values = new double[ angles ];
        for(int i = 0; i < angles; i++)
        {
            try { values[i] = measureAngle("chi"+(i+1), res, state); }
            catch(AtomException ex) { values[i] = Double.NaN; }
        }
        
        return values;
    }
    
    public ModelState setChiAngles(Residue res, ModelState state, double[] values)
    {
        String rescode = res.getName().toLowerCase();
        int angles = countChiAngles(res);
        if(angles < 0)
            throw new IllegalArgumentException("Unknown residue type");
        if(values.length < angles)
            throw new IllegalArgumentException("Not enough angles specified");
        
        for(int i = 0; i < angles; i++)
        {
            try { state = setAngle("chi"+(i+1), res, state, values[i]); }
            catch(IllegalArgumentException ex)  { ex.printStackTrace(SoftLog.err); }
            catch(AtomException ex)             { ex.printStackTrace(SoftLog.err); }
        }
        
        return state;
    }
//}}}

//{{{ getAllRotamers
//##################################################################################################
    /**
    * Returns an array of all the named rotamers known for a given residue code,
    * or null if that residue is unknown.
    */
    public RotamerDef[] getAllRotamers(Residue res)
    { return getAllRotamers(res.getName()); }
    
    /**
    * Returns an array of all the named rotamers known for a given residue code,
    * or null if that residue is unknown.
    */
    public RotamerDef[] getAllRotamers(String rescode)
    {
        rescode = rescode.toLowerCase();
        Collection c = (Collection)rotamersForAA.get(rescode);
        if(c == null) return null;
        else return (RotamerDef[])c.toArray(new RotamerDef[c.size()]);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

