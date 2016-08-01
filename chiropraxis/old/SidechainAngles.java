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
import driftwood.gnutil.*;
import driftwood.moldb.*;
import driftwood.r3.*;
import driftwood.util.*;
//}}}
/**
* <code>SidechainAngles</code> allows for measuring and altering
* the dihedral angles in an amino acid side-chain.
* All three-letter codes are handled internally in lower case,
* but the appropriate conversions should be made automatically.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu May  8 09:52:04 EDT 2003
*/
public class SidechainAngles //extends ... implements ...
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
    public SidechainAngles() throws IOException
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
        chisPerAA           = new GnuLinkedHashMap();
        anglesForAA         = new GnuLinkedHashMap();
        atomsForAngle       = new GnuLinkedHashMap();
        rotamersForAA       = new GnuLinkedHashMap();
        
        // Read in list of amino acids names and iterate thru them
        String[] aa = Strings.explode(props.getString("aminoacids"), ',');
        knownAA = new GnuLinkedHashSet(Arrays.asList(aa));
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
    public int countChiAngles(String rescode)
    {
        rescode = rescode.toLowerCase();
        Integer i = (Integer)chisPerAA.get(rescode);
        if(i == null) return -1;
        else return i.intValue();
    }
    
    /**
    * Returns the number of mobile angles for the given 3-letter residue code,
    * including mobile H (Ser OH, Met CH3, etc),
    * or -1 if that residue is unknown to the system
    */
    public int countAllAngles(String rescode)
    {
        rescode = rescode.toLowerCase();
        String[] angles = (String[])anglesForAA.get(rescode);
        if(angles == null) return -1;
        else return angles.length;
    }
    
    /**
    * Returns a String[] of all the named angles known for a given residue code,
    * or null if that residue is unknown.
    */
    public String[] nameAllAngles(String rescode)
    {
        rescode = rescode.toLowerCase();
        String[] angles = (String[])anglesForAA.get(rescode);
        if(angles == null) return null;
        else return (String[])angles.clone();
    }

//}}}

//{{{ measureAngle
//##################################################################################################
    public double measureAngle(String angleName, Residue res)
    {
        return measureAngle(res.getType()+"."+angleName, res.getAtomMap());
    }
    
    public double measureAngle(String angleName, AminoAcid aa)
    {
        return measureAngle(aa.getResidue().getType()+"."+angleName, aa.atomMap);
    }
    
    /**
    * Measures the value of some named angle as a value between
    * -180 and +180 degrees
    * @throws IllegalArgumentException if operation cannot succeed
    */
    public double measureAngle(String resDotAngle, Map atomMap)
    {
        resDotAngle = resDotAngle.toLowerCase();
        String[] atomNames = (String[])atomsForAngle.get(resDotAngle);
        if(atomNames == null || atomNames.length < 4)
            throw new IllegalArgumentException("Angle definition bad or not found");

        Atom a1, a2, a3, a4;
        a1 = (Atom)atomMap.get(atomNames[0]);
        a2 = (Atom)atomMap.get(atomNames[1]);
        a3 = (Atom)atomMap.get(atomNames[2]);
        a4 = (Atom)atomMap.get(atomNames[3]);
        if(a1 == null || a2 == null || a3 == null || a4 == null)
            throw new IllegalArgumentException("Missing atom in structure");

        return Triple.dihedral(a1, a2, a3, a4);
    }
//}}}

//{{{ areParentAndChild
//##################################################################################################
    protected boolean areParentAndChild(Atom parent, Atom child)
    {
        String p = parent.getID();
        String c = child.getID();
        if(p == null || c == null || p.length() != 4 || c.length() != 4)
            throw new IllegalArgumentException("Bad atom name(s)");
        
        // If statements are a workaround for seleno-Met,
        // which by convention is a HET and has no remoteness
        // indicator for the selenium atom.
        int pi = REMOTENESS.indexOf(p.charAt(2));
        if(parent.getResidue().getType().equals("MSE") && p.equals("SE  ")) pi = 'D';
        int ci = REMOTENESS.indexOf(c.charAt(2));
        if(child.getResidue().getType().equals("MSE") && c.equals("SE  ")) ci = 'D';
        
        return
        ((pi > ci && (p.charAt(3) == ' ' || p.charAt(3) == c.charAt(3)))    // parent closer AND on root or same branch
        || (pi == ci && p.charAt(3) == c.charAt(3)                          // OR child is an H of parent
            && p.charAt(1) != 'H' && c.charAt(1) == 'H'));
    }
//}}}

//{{{ setAngle
//##################################################################################################
    public void setAngle(String angleName, Residue res, double endAngle)
    { setAngle(res.getType()+"."+angleName, res.getAtomMap(), endAngle); }
    public void setAngle(String angleName, AminoAcid aa, double endAngle)
    { setAngle(aa.getResidue().getType()+"."+angleName, aa.atomMap, endAngle); }
    
    /**
    * Adjusts the model such that the appropriate atoms
    * are rotated about the named angle axis.
    * Sets the angle to some absolute value in degrees.
    * Use measureAngle() to learn the initial value, if needed.
    */
    public void setAngle(String resDotAngle, Map atomMap, double endAngle)
    {
        // Copied from measureAngle -- we need this info anyway...
        resDotAngle = resDotAngle.toLowerCase();
        String[] atomNames = (String[])atomsForAngle.get(resDotAngle);
        if(atomNames == null || atomNames.length < 4)
            throw new IllegalArgumentException("Angle definition bad or not found");

        Atom a1, a2, a3, a4;
        a1 = (Atom)atomMap.get(atomNames[0]);
        a2 = (Atom)atomMap.get(atomNames[1]);
        a3 = (Atom)atomMap.get(atomNames[2]);
        a4 = (Atom)atomMap.get(atomNames[3]);
        if(a1 == null || a2 == null || a3 == null || a4 == null)
            throw new IllegalArgumentException("Missing atom in structure for "+resDotAngle);
        
        double startAngle = Triple.dihedral(a1, a2, a3, a4);
        double dTheta = endAngle - startAngle;
        rot.likeRotation(a2, a3, dTheta);
        
        for(Iterator iter = atomMap.values().iterator(); iter.hasNext(); )
        {
            Atom atom = (Atom)iter.next();
            if( areParentAndChild(a3, atom) ) rot.transform(atom);
        }
    }
//}}}

//{{{ measureAllAngles, setAllAngles
//##################################################################################################
    public double[] measureAllAngles(Residue res)
    { return measureAllAngles(res.getType(), res.getAtomMap()); }
    public double[] measureAllAngles(AminoAcid aa)
    { return measureAllAngles(aa.getResidue().getType(), aa.atomMap); }
    
    public double[] measureAllAngles(String rescode, Map atomMap)
    {
        rescode = rescode.toLowerCase();
        String[] angles = nameAllAngles(rescode);
        if(angles == null) throw new IllegalArgumentException("Unknown residue type");
        
        double[] values = new double[ angles.length ];
        for(int i = 0; i < angles.length; i++)
        {
            values[i] = measureAngle(rescode+"."+angles[i], atomMap);
        }
        
        return values;
    }
    
    public void setAllAngles(Residue res, double[] values)
    { setAllAngles(res.getType(), res.getAtomMap(), values); }
    public void setAllAngles(AminoAcid aa, double[] values)
    { setAllAngles(aa.getResidue().getType(), aa.atomMap, values); }
    
    public void setAllAngles(String rescode, Map atomMap, double[] values)
    {
        rescode = rescode.toLowerCase();
        String[] angles = nameAllAngles(rescode);
        if(angles == null)
            throw new IllegalArgumentException("Unknown residue type");
        if(values.length < angles.length)
            throw new IllegalArgumentException("Not enough angles specified");
        
        for(int i = 0; i < angles.length; i++)
        {
            try { setAngle(rescode+"."+angles[i], atomMap, values[i]); }
            catch(IllegalArgumentException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
//}}}

//{{{ measureChiAngles, setChiAngles
//##################################################################################################
    public double[] measureChiAngles(Residue res)
    { return measureChiAngles(res.getType(), res.getAtomMap()); }
    public double[] measureChiAngles(AminoAcid aa)
    { return measureChiAngles(aa.getResidue().getType(), aa.atomMap); }
    
    public double[] measureChiAngles(String rescode, Map atomMap)
    {
        rescode = rescode.toLowerCase();
        int angles = countChiAngles(rescode);
        if(angles < 0) throw new IllegalArgumentException("Unknown residue type");
        
        double[] values = new double[ angles ];
        for(int i = 0; i < angles; i++)
        {
            values[i] = measureAngle(rescode+".chi"+(i+1), atomMap);
        }
        
        return values;
    }
    
    public void setChiAngles(Residue res, double[] values)
    { setChiAngles(res.getType(), res.getAtomMap(), values); }
    public void setChiAngles(AminoAcid aa, double[] values)
    { setChiAngles(aa.getResidue().getType(), aa.atomMap, values); }
    
    public void setChiAngles(String rescode, Map atomMap, double[] values)
    {
        rescode = rescode.toLowerCase();
        int angles = countChiAngles(rescode);
        if(angles < 0)
            throw new IllegalArgumentException("Unknown residue type");
        if(values.length < angles)
            throw new IllegalArgumentException("Not enough angles specified");
        
        for(int i = 0; i < angles; i++)
        {
            try { setAngle(rescode+".chi"+(i+1), atomMap, values[i]); }
            catch(IllegalArgumentException ex) { ex.printStackTrace(SoftLog.err); }
        }
    }
//}}}

//{{{ getAllRotamers
//##################################################################################################
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

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

