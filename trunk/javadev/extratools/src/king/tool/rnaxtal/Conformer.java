// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import chiropraxis.sc.SidechainAngles2;
import chiropraxis.kingtools.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.*;
import king.tool.postkin.*;
import king.core.*;
//}}}
/**
 * <code>Conformer</code> is a temp placeholder until I can figure out 
 * conformer scoring.
 *
* <p>Copyright (C) 2010 by Vincent B Chen. All rights reserved.
* <br>Begun on Mon Jan 04 15:59:57 EST 2010 
*/
public class Conformer //extends ... implements ...
{
  //{{{ Constants
  public static final String REMOTENESS = "HC5C4C3O2C1NCO4O3P";
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}

//{{{ Variable definitions
//##################################################################################################
    static private Conformer  instance    = null;
    /** Maps 3-letter res code to NDFT: Map<String, NDFloatTable> */
    HashMap                 tables;
    /** Maps lower case 3-letter codes to Lists of NamedRot objects */
    HashMap                 names;
    /** Defines geometry (how to measure chi angles, how many, etc */
    HashMap                 angleMap;
    HashMap                 adjacencyMap = null;
    HashMap                 mobileMap;
    //SidechainAngles2        scAngles2;
    // Used for working calculations
    Transform rot = new Transform();
//}}}

//{{{ getInstance, freeInstance
//##################################################################################################
    /**
    * Retrieves the current Conformer instance, or
    * creates it and loads the data tables from disk
    * if (1) this method has never been called before
    * or (2) this method has not been called since
    * freeInstance() was last called.
    * If creation fails due to missing resource data,
    * an IOException will be thrown.
    * @throws IOException if a Conformer instance could not be created
    */
    static public Conformer getInstance() throws IOException
    {
        if(instance != null)    return instance;
        else                    return (instance = new Conformer());
    }
    
    /**
    * Frees the internal reference to the allocated Conformer object.
    * It will be GC'ed when all references to it expire.
    * Future calls to getInstance() will allocate a new Conformer object.
    * This function allows sneaky users to have more than one Conformer
    * object in memory at once. This is generally a bad idea, as they're
    * really big, but we won't stop you if you're sure that's what you want.
    */
    static public void freeInstance()
    {
        instance = null;
    }
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    * @throws IOException if the needed resource(s) can't be loaded from the JAR file
    * @throws NoSuchElementException if the resource is missing a required entry
    */
    private Conformer() throws IOException
    {
        //loadTablesFromJar();
        //loadConformerNames();
        //this.scAngles2  = new SidechainAngles2();
        String[] delta_1   = {" C5'", " C4'", " C3'", " O3'"};
        String[] epsilon_1 = {" C4'", " C3'", " O3'", " P  "};
        String[] zeta_1    = {" C3'", " O3'", " P  ", " O5'"};
        String[] alpha     = {" O3'", " P  ", " O5'", " C5'"};
        String[] beta      = {" P  ", " O5'", " C5'", " C4'"};
        String[] gamma     = {" O5'", " C5'", " C4'", " C3'"};
        String[] delta     = {" C5'", " C4'", " C3'", " O3'"};
        angleMap = new HashMap();
        angleMap.put("delta-1", delta_1);
        angleMap.put("epsilon-1", epsilon_1);
        angleMap.put("zeta-1", zeta_1);
        angleMap.put("alpha", alpha);
        angleMap.put("beta", beta);
        angleMap.put("gamma", gamma);
        angleMap.put("delta", delta);
    }
//}}}

  //{{{ getAngleNames
  public String[] getAngleNames() {
    String[] names = {"delta-1", "epsilon-1", "zeta-1", "alpha", "beta", "gamma", "delta"};
    return names;
  }
  //}}}
  
  //{{{ measureAllAngles
  public double[] measureAllAngles(Residue first, Residue sec, ModelState state) throws AtomException {
    setAdjacency(first, sec, state);
    HashMap atomStates = new HashMap();
    mapAtomStates(first, state, atomStates);
    mapAtomStates(sec, state, atomStates);
    //HashSet mobile = ConnectivityFinder.mobilityFinder(a2, a3, adjacencyMap, atomStates);
    mobileMap = new HashMap();

    String[] angles = getAngleNames();
    double[] values = new double[angles.length];
    for (int i = 0; i < angles.length; i++) {
      try {
        values[i] = measureAngle(angles[i], first, sec, state);
        AtomState[] as = getAngleAtomStates(angles[i], first, sec, state);
        AtomState a2, a3;
        a2 = as[1];
        a3 = as[2];
        HashSet mobile = ConnectivityFinder.mobilityFinder(a2, a3, adjacencyMap, atomStates);
        //System.out.println(mobile);
        mobileMap.put(angles[i], mobile);
      }
      catch (AtomException ae) {
        values[i] = Double.NaN; 
      }
    }
    
    return values;
  }
  //}}}
  
  //{{{ measureAngle
  public double measureAngle(String angleName, Residue first, Residue sec, ModelState state) throws AtomException {
    AtomState[] as = getAngleAtomStates(angleName, first, sec, state);
    return Triple.dihedral(as[0], as[1], as[2], as[3]);
  }
  //}}}
  
  //{{{ getAngleAtomStates
  public AtomState[] getAngleAtomStates(String angleName, Residue res1, Residue res2, ModelState state) throws AtomException {
    //System.out.println(angleName);
    String[] atomNames = (String[]) angleMap.get(angleName);
    AtomState[] states = new AtomState[4];
    for (int i = 0; i < 4; i++) {
      String s = atomNames[i];
      if (s.equals(" P  ")||(s.equals(" O5'"))) {
        states[i] = state.get(res2.getAtom(s));
      } else if (angleName.endsWith("-1")) {
        states[i] = state.get(res1.getAtom(s));
      } else if (angleName.equals("alpha")) { //since alpha has atoms from both res
        if (s.equals(" O3'")) {
          states[i] = state.get(res1.getAtom(s)); //O3' from first residue
        } else {
          states[i] = state.get(res2.getAtom(s)); //C5' from second res
        }
      } else {
        states[i] = state.get(res2.getAtom(s));
      }
    }
    return states;
  }
  //}}}

  //{{{ setAngle
  //##################################################################################################
  /**
  * Adjusts the model such that the appropriate atoms
  * are rotated about the named angle axis.
  * Sets the angle to some absolute value in degrees.
  * Use measureAngle() to learn the initial value, if needed.
  * @return a modified ModelState (input state is not changed)
  * @throws IllegalArgumentException if operation cannot succeed
  * @throws AtomException if required atoms are missing
  */
  public ModelState setAngle(String angleName, Residue res1, Residue res2, ModelState state, double endAngle) throws AtomException
  {
    AtomState[] as = getAngleAtomStates(angleName, res1, res2, state);
    AtomState a1, a2, a3, a4;
    a1 = as[0];
    a2 = as[1];
    a3 = as[2];
    a4 = as[3];
    
    double startAngle = Triple.dihedral(a1, a2, a3, a4);
    double dTheta = endAngle - startAngle;
    rot.likeRotation(a2, a3, dTheta);
    
    ModelState ms = new ModelState(state);
    HashSet mobile = (HashSet) mobileMap.get(angleName);
    //System.out.println(mobile.size());
    //HashMap atomStates = new HashMap();
    //mapAtomStates(res1, state, atomStates);
    //mapAtomStates(res2, state, atomStates);
    //HashSet mobile = ConnectivityFinder.mobilityFinder(a2, a3, adjacencyMap, atomStates);
    
    ArrayList allAtoms = new ArrayList(res1.getAtoms());
    allAtoms.addAll(res2.getAtoms());
   // boolean atomFound = false;
    for(Iterator iter = allAtoms.iterator(); iter.hasNext(); )
    {
      Atom atom = (Atom)iter.next();
      //System.out.println(atom.toString()+" being tested");
      a1 = state.get(atom);
      a2 = (AtomState)a1.clone();
      //if( areParentAndChild(a3.getAtom(), atom) )
   //   if (a4.equals(a1)) atomFound = !atomFound; // this assumes that atoms come in the proper order along the backbone...
      //System.out.println(a1);
      if (mobile.contains(atom)) {
        rot.transform(a2);
        ms.add(a2);
      }
    }
    
    return ms;
  }
  //}}}
  
  //{{{ setAllAngles
  public ModelState setAllAngles(Residue res1, Residue res2, ModelState state, double[] values)
  {
    String rescode = res1.getName().toLowerCase();
    String[] angles = getAngleNames();
    if(angles == null)
      throw new IllegalArgumentException("Unknown residue type");
    if(values.length < angles.length)
      throw new IllegalArgumentException("Not enough angles specified");
    
    for(int i = 0; i < angles.length; i++)
    {
      if(!Double.isNaN(values[i]))
      {
        try { state = setAngle(angles[i], res1, res2, state, values[i]); }
        catch(IllegalArgumentException ex)  { ex.printStackTrace(SoftLog.err); }
        catch(AtomException ex)             { ex.printStackTrace(SoftLog.err); }
      }
    }
    
    return state;
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
    
    // for converting the shifted hydrogens in pdbv3 back to pdbv2.3 (e.g. HG11 to 1HG1)
    if (p.charAt(0) == 'H') p = p.substring(3) + p.substring(0,3);
    if (c.charAt(0) == 'H') c = c.substring(3) + c.substring(0,3);
    
    if ((p.charAt(3) == '\'') || (p.charAt(3) == '*')) p = p.substring(1, 2);
    int pi = REMOTENESS.indexOf(p.charAt(2));
    int ci = REMOTENESS.indexOf(c.charAt(2));
    
    return
    ((pi > ci && (p.charAt(3) == ' ' || p.charAt(3) == c.charAt(3)))    // parent closer AND on root or same branch
      || (pi == ci && (p.charAt(3) == ' ' || p.charAt(3) == c.charAt(3))  // OR child is an H of parent
        && p.charAt(1) != 'H' && c.charAt(1) == 'H'));
  }
  //}}}

  //{{{ setAdjacency
  public void setAdjacency(Residue res1, Residue res2, ModelState state) {
    if (adjacencyMap != null) return;
    Model tempModel = new Model("temp");
    try {
      tempModel.add(res1);
      tempModel.add(res2);
    } catch (ResidueException re) {/*shouldn't happen*/}
    ArrayList list = new ArrayList();
    list.add(res1);
    list.add(res2);
    Kinemage kin = ModelPlotter.buildKinObject(tempModel, list, state);
    adjacencyMap = new HashMap();
    ConnectivityFinder.buildAdjacencyMap(kin, true, adjacencyMap);
  }
  //}}}
  
  //{{{ mapAtomStates
  public void mapAtomStates(Residue res, ModelState state, HashMap map) throws AtomException { 
    ArrayList allAtoms = new ArrayList(res.getAtoms());
    for(Iterator iter = allAtoms.iterator(); iter.hasNext(); )
    {
      Atom atom = (Atom)iter.next();
      //System.out.println(atom.toString()+" being tested");
      AtomState a1 = state.get(atom);
      Triple a1Trip = new Triple(a1);
      map.put(a1Trip.format(df), a1);
    }
  }
  //}}}

//{{{ dumpConformerNames
  //public String dumpConformerNames() {
  //  String allConformerNames = "";
  //  loadConformerNames();
  //  Iterator keys = names.keySet().iterator();
  //  while (keys.hasNext()) {
  //    String resName = (String) keys.next();
  //    ArrayList rotaList = (ArrayList) names.get(resName);
  //    Iterator namedRots = rotaList.iterator();
  //    while (namedRots.hasNext()) {
  //      NamedRot rot = (NamedRot) namedRots.next();
  //      String bounds = Arrays.toString(rot.bounds);
  //      allConformerNames = allConformerNames+resName+" "+rot.getName()+"="+bounds.substring(1, bounds.length()-1)+"\n";
  //    }
  //  }
  //  return allConformerNames;
  //}
//}}}
/*
//{{{ loadTablesFromJar
//##################################################################################################
    private void loadTablesFromJar() throws IOException
    {
        tables = new HashMap(30);
        NDFloatTable ndft;
        tables.put("ser", loadTable("rota/ser.ndft"));
        tables.put("thr", loadTable("rota/thr.ndft"));
        tables.put("cys", loadTable("rota/cys.ndft"));
        tables.put("val", loadTable("rota/val.ndft"));
        tables.put("pro", loadTable("rota/pro.ndft"));
        
        tables.put("leu", loadTable("rota/leu.ndft"));
        tables.put("ile", loadTable("rota/ile.ndft"));
        tables.put("trp", loadTable("rota/trp.ndft"));
        tables.put("asp", loadTable("rota/asp.ndft"));
        tables.put("asn", loadTable("rota/asn.ndft"));
        tables.put("his", loadTable("rota/his.ndft"));
        ndft = loadTable("rota/phetyr.ndft");
        tables.put("phe", ndft);
        tables.put("tyr", ndft);
        
        ndft = loadTable("rota/met.ndft");
        tables.put("met", ndft);
        tables.put("mse", ndft); // seleno-Met
        tables.put("glu", loadTable("rota/glu.ndft"));
        tables.put("gln", loadTable("rota/gln.ndft"));
        
        tables.put("lys", loadTable("rota/lys.ndft"));
        tables.put("arg", loadTable("rota/arg.ndft"));
    }
    
    private NDFloatTable loadTable(String path) throws IOException
    {
        InputStream is = this.getClass().getResourceAsStream(path);
        if(is == null) throw new IOException("Missing resource");
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        NDFloatTable tab = new NDFloatTable(dis);
        dis.close();
        return tab;
    }
//}}}

//{{{ loadConformerNames
//##################################################################################################
    private void loadConformerNames()
    {
        // These bins are often WAY too big -- the point is to partition space first,
        // and use the empirical distributions to decide if it's valid or not.
        // Box boundaries are inclusive, so a point on the border could go either way.
        // In those cases, though, it really doesn't matter, so it can be arbitrary.
        // All boundaries are on a non-cyclic 0-360 or 0-180 grid.
        // Some rotamers need more than one box to account for this.
        // Boundaries were determined by hand by IWD while looking at kins labeled
        // by an automatic hill-climbing algorithm.
        //  name    min1    max1    min2    max2    ...
        this.names = new HashMap();
        ArrayList tbl;
        
        // thr, val, ser, cys
        // val has weird "extra" peaks above 1%: 125-135 and 345-360
        tbl = new ArrayList();
        tbl.add(new NamedRot("p",       new int[] {0,      120}));
        tbl.add(new NamedRot("t",       new int[] {120,    240}));
        tbl.add(new NamedRot("m",       new int[] {240,    360}));
        names.put("thr", tbl);
        names.put("val", tbl);
        names.put("ser", tbl);
        names.put("cys", tbl);
        
        // pro
        tbl = new ArrayList();
        tbl.add(new NamedRot("Cg_endo", new int[] {0,      180}));
        tbl.add(new NamedRot("Cg_exo",  new int[] {180,    360}));
        names.put("pro", tbl);
        
        // phe, tyr
        tbl = new ArrayList();
        tbl.add(new NamedRot("p90",     new int[] {0,      120,    0,      180}));
        tbl.add(new NamedRot("t80",     new int[] {120,    240,    0,      180}));
        tbl.add(new NamedRot("m-85",    new int[] {240,    360,    35,     150}));
        tbl.add(new NamedRot("m-30",    new int[] {240,    360,    0,      35}));
        tbl.add(new NamedRot("m-30",    new int[] {240,    360,    150,    180}));
        names.put("phe", tbl);
        names.put("tyr", tbl);
        
        // trp
        tbl = new ArrayList();
        tbl.add(new NamedRot("p-90",    new int[] {0,      120,    180,    360}));
        tbl.add(new NamedRot("p90",     new int[] {0,      120,    0,      180}));
        tbl.add(new NamedRot("t-105",   new int[] {120,    240,    180,    305}));
        tbl.add(new NamedRot("t90",     new int[] {120,    240,    0,      180}));
        tbl.add(new NamedRot("t90",     new int[] {120,    240,    305,    360}));
        tbl.add(new NamedRot("m-90",    new int[] {240,    360,    180,    305}));
        tbl.add(new NamedRot("m0",      new int[] {240,    360,    305,    360}));
        tbl.add(new NamedRot("m0",      new int[] {240,    360,    0,      45}));
        tbl.add(new NamedRot("m95",     new int[] {240,    360,    45,     180}));
        names.put("trp", tbl);
        
        // his
        tbl = new ArrayList();
        tbl.add(new NamedRot("p-80",    new int[] {0,      120,    180,    360}));
        tbl.add(new NamedRot("p80",     new int[] {0,      120,    0,      180}));
        tbl.add(new NamedRot("t-160",   new int[] {120,    240,    130,    225}));
        tbl.add(new NamedRot("t-80",    new int[] {120,    240,    225,    360}));
        tbl.add(new NamedRot("t60",     new int[] {120,    240,    0,      130}));
        tbl.add(new NamedRot("m-70",    new int[] {240,    360,    225,    360}));
        tbl.add(new NamedRot("m-70",    new int[] {240,    360,    0,      20}));
        tbl.add(new NamedRot("m170",    new int[] {240,    360,    130,    225}));
        tbl.add(new NamedRot("m80",     new int[] {240,    360,    20,     130}));
        names.put("his", tbl);
        
        // leu
        tbl = new ArrayList();
        tbl.add(new NamedRot("pp",      new int[] {0,      120,    0,      120}));
        tbl.add(new NamedRot("pt?",     new int[] {0,      120,    120,    240}));
        tbl.add(new NamedRot("tp",      new int[] {120,    240,    0,      120}));
        tbl.add(new NamedRot("tt",      new int[] {120,    240,    120,    240}));
        tbl.add(new NamedRot("tm?",     new int[] {120,    240,    240,    360}));
        tbl.add(new NamedRot("mp",      new int[] {240,    360,    0,      120}));
        tbl.add(new NamedRot("mt",      new int[] {240,    360,    120,    240}));
        tbl.add(new NamedRot("mm?",     new int[] {240,    360,    240,    360}));
        names.put("leu", tbl);
        
        // ile
        tbl = new ArrayList();
        tbl.add(new NamedRot("pp",      new int[] {0,      120,    0,      120}));
        tbl.add(new NamedRot("pt",      new int[] {0,      120,    120,    240}));
        tbl.add(new NamedRot("tp",      new int[] {120,    240,    0,      120}));
        tbl.add(new NamedRot("tt",      new int[] {120,    240,    120,    240}));
        tbl.add(new NamedRot("tm?",     new int[] {120,    240,    240,    360}));
        tbl.add(new NamedRot("mp",      new int[] {240,    360,    0,      120}));
        tbl.add(new NamedRot("mt",      new int[] {240,    360,    120,    240}));
        tbl.add(new NamedRot("mm",      new int[] {240,    360,    240,    360}));
        names.put("ile", tbl);
        
        // asn
        tbl = new ArrayList();
        tbl.add(new NamedRot("p-10",    new int[] {0,      120,    180,    360}));
        tbl.add(new NamedRot("p30",     new int[] {0,      120,    0,      180}));
        tbl.add(new NamedRot("t-20",    new int[] {120,    240,    180,    360}));
        tbl.add(new NamedRot("t-20",    new int[] {120,    240,    0,      10}));
        tbl.add(new NamedRot("t30",     new int[] {120,    240,    10,     180}));
        tbl.add(new NamedRot("m-20",    new int[] {240,    360,    300,    360}));
        tbl.add(new NamedRot("m-20",    new int[] {240,    360,    0,      40}));
        tbl.add(new NamedRot("m-80",    new int[] {240,    360,    200,    300}));
        tbl.add(new NamedRot("m120",    new int[] {240,    360,    40,     200}));
        names.put("asn", tbl);
        
        // asp
        tbl = new ArrayList();
        tbl.add(new NamedRot("p-10",    new int[] {0,      120,    90,     180}));
        tbl.add(new NamedRot("p30",     new int[] {0,      120,    0,      90}));
        tbl.add(new NamedRot("t0",      new int[] {120,    240,    0,      45}));
        tbl.add(new NamedRot("t0",      new int[] {120,    240,    120,    180}));
        tbl.add(new NamedRot("t70",     new int[] {120,    240,    45,     120}));
        tbl.add(new NamedRot("m-20",    new int[] {240,    360,    0,      180}));
        names.put("asp", tbl);
        
        // gln
        tbl = new ArrayList();
        tbl.add(new NamedRot("pt20",    new int[] {0,      120,    120,    240,    0,      360}));
        tbl.add(new NamedRot("pm0",     new int[] {0,      120,    240,    360,    0,      360}));
        tbl.add(new NamedRot("pp0?",    new int[] {0,      120,    0,      120,    0,      360}));
        tbl.add(new NamedRot("tp-100",  new int[] {120,    240,    0,      120,    150,    300}));
        tbl.add(new NamedRot("tp60",    new int[] {120,    240,    0,      120,    0,      150}));
        tbl.add(new NamedRot("tp60",    new int[] {120,    240,    0,      120,    300,    360}));
        tbl.add(new NamedRot("tt0",     new int[] {120,    240,    120,    240,    0,      360}));
        tbl.add(new NamedRot("tm0?",    new int[] {120,    240,    240,    360,    0,      360}));
        tbl.add(new NamedRot("mp0",     new int[] {240,    360,    0,      120,    0,      360}));
        tbl.add(new NamedRot("mt-30",   new int[] {240,    360,    120,    240,    0,      360}));
        tbl.add(new NamedRot("mm-40",   new int[] {240,    360,    240,    360,    0,      60}));
        tbl.add(new NamedRot("mm-40",   new int[] {240,    360,    240,    360,    210,    360}));
        tbl.add(new NamedRot("mm100",   new int[] {240,    360,    240,    360,    60,     210}));
        names.put("gln", tbl);
        
        // glu
        tbl = new ArrayList();
        tbl.add(new NamedRot("pp20?",   new int[] {0,      120,    0,      120,    0,      180}));
        tbl.add(new NamedRot("pt-20",   new int[] {0,      120,    120,    240,    0,      180}));
        tbl.add(new NamedRot("pm0",     new int[] {0,      120,    240,    360,    0,      180}));
        tbl.add(new NamedRot("tp10",    new int[] {120,    240,    0,      120,    0,      180}));
        tbl.add(new NamedRot("tt0",     new int[] {120,    240,    120,    240,    0,      180}));
        tbl.add(new NamedRot("tm-20",   new int[] {120,    240,    240,    360,    0,      180}));
        tbl.add(new NamedRot("mp0",     new int[] {240,    360,    0,      120,    0,      180}));
        tbl.add(new NamedRot("mt-10",   new int[] {240,    360,    120,    240,    0,      180}));
        tbl.add(new NamedRot("mm-40",   new int[] {240,    360,    240,    360,    0,      180}));
        names.put("glu", tbl);
        
        // met (mmt and tpt maybe two peaks each)
        tbl = new ArrayList();
        tbl.add(new NamedRot("ppp?",    new int[] {0,      120,    0,      120,    0,      120}));
        tbl.add(new NamedRot("ptp",     new int[] {0,      120,    120,    240,    0,      120}));
        tbl.add(new NamedRot("ptt?",    new int[] {0,      120,    120,    240,    120,    240}));
        tbl.add(new NamedRot("ptm",     new int[] {0,      120,    120,    240,    240,    360}));
        tbl.add(new NamedRot("pmm?",    new int[] {0,      120,    240,    360,    240,    360}));
        tbl.add(new NamedRot("tpp",     new int[] {120,    240,    0,      120,    0,      120}));
        tbl.add(new NamedRot("tpp",     new int[] {120,    240,    0,      120,    330,    360}));
        tbl.add(new NamedRot("tpt",     new int[] {120,    240,    0,      120,    120,    330}));
        tbl.add(new NamedRot("ttp",     new int[] {120,    240,    120,    240,    0,      120}));
        tbl.add(new NamedRot("ttt",     new int[] {120,    240,    120,    240,    120,    240}));
        tbl.add(new NamedRot("ttm",     new int[] {120,    240,    120,    240,    240,    360}));
        tbl.add(new NamedRot("tmt?",    new int[] {120,    240,    240,    360,    120,    240}));
        tbl.add(new NamedRot("tmm?",    new int[] {120,    240,    240,    360,    240,    360}));
        tbl.add(new NamedRot("mpp?",    new int[] {240,    360,    0,      120,    0,      120}));
        tbl.add(new NamedRot("mpt?",    new int[] {240,    360,    0,      120,    120,    240}));
        tbl.add(new NamedRot("mtp",     new int[] {240,    360,    120,    240,    0,      120}));
        tbl.add(new NamedRot("mtt",     new int[] {240,    360,    120,    240,    120,    240}));
        tbl.add(new NamedRot("mtm",     new int[] {240,    360,    120,    240,    240,    360}));
        tbl.add(new NamedRot("mmt",     new int[] {240,    360,    240,    360,    30,     240}));
        tbl.add(new NamedRot("mmm",     new int[] {240,    360,    240,    360,    0,      30}));
        tbl.add(new NamedRot("mmm",     new int[] {240,    360,    240,    360,    240,    360}));
        names.put("met", tbl);
        names.put("mse", tbl); // seleno-Met
        
        // lys (kept all b/c can't see 4D peaks; some never really occur)
        tbl = new ArrayList();
        tbl.add(new NamedRot("pppp?",   new int[] {0,      120,    0,      120,    0,      120,    0,      120}));
        tbl.add(new NamedRot("pppt?",   new int[] {0,      120,    0,      120,    0,      120,    120,    240}));
        tbl.add(new NamedRot("pppm?",   new int[] {0,      120,    0,      120,    0,      120,    240,    360}));
        tbl.add(new NamedRot("pptp?",   new int[] {0,      120,    0,      120,    120,    240,    0,      120}));
        tbl.add(new NamedRot("pptt?",   new int[] {0,      120,    0,      120,    120,    240,    120,    240}));
        tbl.add(new NamedRot("pptm?",   new int[] {0,      120,    0,      120,    120,    240,    240,    360}));
        tbl.add(new NamedRot("ppmp?",   new int[] {0,      120,    0,      120,    240,    360,    0,      120}));
        tbl.add(new NamedRot("ppmt?",   new int[] {0,      120,    0,      120,    240,    360,    120,    240}));
        tbl.add(new NamedRot("ppmm?",   new int[] {0,      120,    0,      120,    240,    360,    240,    360}));
        tbl.add(new NamedRot("ptpp?",   new int[] {0,      120,    120,    240,    0,      120,    0,      120}));
        tbl.add(new NamedRot("ptpt",    new int[] {0,      120,    120,    240,    0,      120,    120,    240}));
        tbl.add(new NamedRot("ptpm?",   new int[] {0,      120,    120,    240,    0,      120,    240,    360}));
        tbl.add(new NamedRot("pttp",    new int[] {0,      120,    120,    240,    120,    240,    0,      120}));
        tbl.add(new NamedRot("pttt",    new int[] {0,      120,    120,    240,    120,    240,    120,    240}));
        tbl.add(new NamedRot("pttm",    new int[] {0,      120,    120,    240,    120,    240,    240,    360}));
        tbl.add(new NamedRot("ptmp?",   new int[] {0,      120,    120,    240,    240,    360,    0,      120}));
        tbl.add(new NamedRot("ptmt",    new int[] {0,      120,    120,    240,    240,    360,    120,    240}));
        tbl.add(new NamedRot("ptmm?",   new int[] {0,      120,    120,    240,    240,    360,    240,    360}));
        tbl.add(new NamedRot("pmpp?",   new int[] {0,      120,    240,    360,    0,      120,    0,      120}));
        tbl.add(new NamedRot("pmpt?",   new int[] {0,      120,    240,    360,    0,      120,    120,    240}));
        tbl.add(new NamedRot("pmpm?",   new int[] {0,      120,    240,    360,    0,      120,    240,    360}));
        tbl.add(new NamedRot("pmtp?",   new int[] {0,      120,    240,    360,    120,    240,    0,      120}));
        tbl.add(new NamedRot("pmtt?",   new int[] {0,      120,    240,    360,    120,    240,    120,    240}));
        tbl.add(new NamedRot("pmtm?",   new int[] {0,      120,    240,    360,    120,    240,    240,    360}));
        tbl.add(new NamedRot("pmmp?",   new int[] {0,      120,    240,    360,    240,    360,    0,      120}));
        tbl.add(new NamedRot("pmmt?",   new int[] {0,      120,    240,    360,    240,    360,    120,    240}));
        tbl.add(new NamedRot("pmmm?",   new int[] {0,      120,    240,    360,    240,    360,    240,    360}));
        tbl.add(new NamedRot("tppp?",   new int[] {120,    240,    0,      120,    0,      120,    0,      120}));
        tbl.add(new NamedRot("tppt?",   new int[] {120,    240,    0,      120,    0,      120,    120,    240}));
        tbl.add(new NamedRot("tppm?",   new int[] {120,    240,    0,      120,    0,      120,    240,    360}));
        tbl.add(new NamedRot("tptp",    new int[] {120,    240,    0,      120,    120,    240,    0,      120}));
        tbl.add(new NamedRot("tptt",    new int[] {120,    240,    0,      120,    120,    240,    120,    240}));
        tbl.add(new NamedRot("tptm",    new int[] {120,    240,    0,      120,    120,    240,    240,    360}));
        tbl.add(new NamedRot("tpmp?",   new int[] {120,    240,    0,      120,    240,    360,    0,      120}));
        tbl.add(new NamedRot("tpmt?",   new int[] {120,    240,    0,      120,    240,    360,    120,    240}));
        tbl.add(new NamedRot("tpmm?",   new int[] {120,    240,    0,      120,    240,    360,    240,    360}));
        tbl.add(new NamedRot("ttpp",    new int[] {120,    240,    120,    240,    0,      120,    0,      120}));
        tbl.add(new NamedRot("ttpt",    new int[] {120,    240,    120,    240,    0,      120,    120,    240}));
        tbl.add(new NamedRot("ttpm?",   new int[] {120,    240,    120,    240,    0,      120,    240,    360}));
        tbl.add(new NamedRot("tttp",    new int[] {120,    240,    120,    240,    120,    240,    0,      120}));
        tbl.add(new NamedRot("tttt",    new int[] {120,    240,    120,    240,    120,    240,    120,    240}));
        tbl.add(new NamedRot("tttm",    new int[] {120,    240,    120,    240,    120,    240,    240,    360}));
        tbl.add(new NamedRot("ttmp?",   new int[] {120,    240,    120,    240,    240,    360,    0,      120}));
        tbl.add(new NamedRot("ttmt",    new int[] {120,    240,    120,    240,    240,    360,    120,    240}));
        tbl.add(new NamedRot("ttmm",    new int[] {120,    240,    120,    240,    240,    360,    240,    360}));
        tbl.add(new NamedRot("tmpp?",   new int[] {120,    240,    240,    360,    0,      120,    0,      120}));
        tbl.add(new NamedRot("tmpt?",   new int[] {120,    240,    240,    360,    0,      120,    120,    240}));
        tbl.add(new NamedRot("tmpm?",   new int[] {120,    240,    240,    360,    0,      120,    240,    360}));
        tbl.add(new NamedRot("tmtp?",   new int[] {120,    240,    240,    360,    120,    240,    0,      120}));
        tbl.add(new NamedRot("tmtt?",   new int[] {120,    240,    240,    360,    120,    240,    120,    240}));
        tbl.add(new NamedRot("tmtm?",   new int[] {120,    240,    240,    360,    120,    240,    240,    360}));
        tbl.add(new NamedRot("tmmp?",   new int[] {120,    240,    240,    360,    240,    360,    0,      120}));
        tbl.add(new NamedRot("tmmt?",   new int[] {120,    240,    240,    360,    240,    360,    120,    240}));
        tbl.add(new NamedRot("tmmm?",   new int[] {120,    240,    240,    360,    240,    360,    240,    360}));
        tbl.add(new NamedRot("mppp?",   new int[] {240,    360,    0,      120,    0,      120,    0,      120}));
        tbl.add(new NamedRot("mppt?",   new int[] {240,    360,    0,      120,    0,      120,    120,    240}));
        tbl.add(new NamedRot("mppm?",   new int[] {240,    360,    0,      120,    0,      120,    240,    360}));
        tbl.add(new NamedRot("mptp?",   new int[] {240,    360,    0,      120,    120,    240,    0,      120}));
        tbl.add(new NamedRot("mptt",    new int[] {240,    360,    0,      120,    120,    240,    120,    240}));
        tbl.add(new NamedRot("mptm?",   new int[] {240,    360,    0,      120,    120,    240,    240,    360}));
        tbl.add(new NamedRot("mpmp?",   new int[] {240,    360,    0,      120,    240,    360,    0,      120}));
        tbl.add(new NamedRot("mpmt?",   new int[] {240,    360,    0,      120,    240,    360,    120,    240}));
        tbl.add(new NamedRot("mpmm?",   new int[] {240,    360,    0,      120,    240,    360,    240,    360}));
        tbl.add(new NamedRot("mtpp",    new int[] {240,    360,    120,    240,    0,      120,    0,      120}));
        tbl.add(new NamedRot("mtpt",    new int[] {240,    360,    120,    240,    0,      120,    120,    240}));
        tbl.add(new NamedRot("mtpm?",   new int[] {240,    360,    120,    240,    0,      120,    240,    360}));
        tbl.add(new NamedRot("mttp",    new int[] {240,    360,    120,    240,    120,    240,    0,      120}));
        tbl.add(new NamedRot("mttt",    new int[] {240,    360,    120,    240,    120,    240,    120,    240}));
        tbl.add(new NamedRot("mttm",    new int[] {240,    360,    120,    240,    120,    240,    240,    360}));
        tbl.add(new NamedRot("mtmp?",   new int[] {240,    360,    120,    240,    240,    360,    0,      120}));
        tbl.add(new NamedRot("mtmt",    new int[] {240,    360,    120,    240,    240,    360,    120,    240}));
        tbl.add(new NamedRot("mtmm",    new int[] {240,    360,    120,    240,    240,    360,    240,    360}));
        tbl.add(new NamedRot("mmpp?",   new int[] {240,    360,    240,    360,    0,      120,    0,      120}));
        tbl.add(new NamedRot("mmpt?",   new int[] {240,    360,    240,    360,    0,      120,    120,    240}));
        tbl.add(new NamedRot("mmpm?",   new int[] {240,    360,    240,    360,    0,      120,    240,    360}));
        tbl.add(new NamedRot("mmtp",    new int[] {240,    360,    240,    360,    120,    240,    0,      120}));
        tbl.add(new NamedRot("mmtt",    new int[] {240,    360,    240,    360,    120,    240,    120,    240}));
        tbl.add(new NamedRot("mmtm",    new int[] {240,    360,    240,    360,    120,    240,    240,    360}));
        tbl.add(new NamedRot("mmmp?",   new int[] {240,    360,    240,    360,    240,    360,    0,      120}));
        tbl.add(new NamedRot("mmmt",    new int[] {240,    360,    240,    360,    240,    360,    120,    240}));
        tbl.add(new NamedRot("mmmm",    new int[] {240,    360,    240,    360,    240,    360,    240,    360}));
        names.put("lys", tbl);
        
        // arg (again, some entries are dummies for peaks that never occur)
        tbl = new ArrayList();
        tbl.add(new NamedRot("ppp_?",   new int[] {0,      120,    0,      120,    0,      120,    0,      360}));
        tbl.add(new NamedRot("ppt_?",   new int[] {0,      120,    0,      120,    120,    240,    0,      360}));
        tbl.add(new NamedRot("ppm_?",   new int[] {0,      120,    0,      120,    240,    360,    0,      360}));
        tbl.add(new NamedRot("ptp85",   new int[] {0,      120,    120,    240,    0,      120,    0,      120}));
        tbl.add(new NamedRot("ptp180",  new int[] {0,      120,    120,    240,    0,      120,    120,    360}));
        tbl.add(new NamedRot("ptt85",   new int[] {0,      120,    120,    240,    120,    240,    0,      120}));
        tbl.add(new NamedRot("ptt180",  new int[] {0,      120,    120,    240,    120,    240,    120,    240}));
        tbl.add(new NamedRot("ptt-85",  new int[] {0,      120,    120,    240,    120,    240,    240,    360}));
        tbl.add(new NamedRot("ptm85",   new int[] {0,      120,    120,    240,    240,    360,    0,      120}));
        tbl.add(new NamedRot("ptm180",  new int[] {0,      120,    120,    240,    240,    360,    120,    360}));
        tbl.add(new NamedRot("pmp_?",   new int[] {0,      120,    240,    360,    0,      120,    0,      360}));
        tbl.add(new NamedRot("pmt_?",   new int[] {0,      120,    240,    360,    120,    240,    0,      360}));
        tbl.add(new NamedRot("pmm_?",   new int[] {0,      120,    240,    360,    240,    360,    0,      360}));
        tbl.add(new NamedRot("tpp85",   new int[] {120,    240,    0,      120,    0,      120,    0,      120}));
        tbl.add(new NamedRot("tpp180",  new int[] {120,    240,    0,      120,    0,      120,    120,    360}));
        tbl.add(new NamedRot("tpt85",   new int[] {120,    240,    0,      120,    120,    240,    0,      120}));
        tbl.add(new NamedRot("tpt180",  new int[] {120,    240,    0,      120,    120,    240,    120,    360}));
        tbl.add(new NamedRot("tpm_?",   new int[] {120,    240,    0,      120,    240,    360,    0,      360}));
        tbl.add(new NamedRot("ttp85",   new int[] {120,    240,    120,    240,    0,      120,    0,      120}));
        tbl.add(new NamedRot("ttp180",  new int[] {120,    240,    120,    240,    0,      120,    120,    240}));
        tbl.add(new NamedRot("ttp-105", new int[] {120,    240,    120,    240,    0,      120,    240,    360}));
        tbl.add(new NamedRot("ttt85",   new int[] {120,    240,    120,    240,    120,    240,    0,      120}));
        tbl.add(new NamedRot("ttt180",  new int[] {120,    240,    120,    240,    120,    240,    120,    240}));
        tbl.add(new NamedRot("ttt-85",  new int[] {120,    240,    120,    240,    120,    240,    240,    360}));
        tbl.add(new NamedRot("ttm105",  new int[] {120,    240,    120,    240,    240,    360,    0,      120}));
        tbl.add(new NamedRot("ttm180",  new int[] {120,    240,    120,    240,    240,    360,    120,    240}));
        tbl.add(new NamedRot("ttm-85",  new int[] {120,    240,    120,    240,    240,    360,    240,    360}));
        tbl.add(new NamedRot("tmp_?",   new int[] {120,    240,    240,    360,    0,      120,    0,      360}));
        tbl.add(new NamedRot("tmt_?",   new int[] {120,    240,    240,    360,    120,    240,    0,      360}));
        tbl.add(new NamedRot("tmm_?",   new int[] {120,    240,    240,    360,    240,    360,    0,      360}));
        tbl.add(new NamedRot("mpp_?",   new int[] {240,    360,    0,      120,    0,      120,    0,      360}));
        tbl.add(new NamedRot("mpt_?",   new int[] {240,    360,    0,      120,    120,    240,    0,      360}));
        tbl.add(new NamedRot("mpm_?",   new int[] {240,    360,    0,      120,    240,    360,    0,      360}));
        tbl.add(new NamedRot("mtp85",   new int[] {240,    360,    120,    240,    0,      120,    0,      120}));
        tbl.add(new NamedRot("mtp180",  new int[] {240,    360,    120,    240,    0,      120,    120,    240}));
        tbl.add(new NamedRot("mtp-105", new int[] {240,    360,    120,    240,    0,      120,    240,    360}));
        tbl.add(new NamedRot("mtt85",   new int[] {240,    360,    120,    240,    120,    240,    0,      120}));
        tbl.add(new NamedRot("mtt180",  new int[] {240,    360,    120,    240,    120,    240,    120,    240}));
        tbl.add(new NamedRot("mtt-85",  new int[] {240,    360,    120,    240,    120,    240,    240,    360}));
        tbl.add(new NamedRot("mtm105",  new int[] {240,    360,    120,    240,    240,    360,    0,      120}));
        tbl.add(new NamedRot("mtm180",  new int[] {240,    360,    120,    240,    240,    360,    120,    240}));
        tbl.add(new NamedRot("mtm-85",  new int[] {240,    360,    120,    240,    240,    360,    240,    360}));
        tbl.add(new NamedRot("mmp_?",   new int[] {240,    360,    240,    360,    0,      120,    0,      360}));
        tbl.add(new NamedRot("mmt85",   new int[] {240,    360,    240,    360,    120,    240,    0,      120}));
        tbl.add(new NamedRot("mmt180",  new int[] {240,    360,    240,    360,    120,    240,    120,    240}));
        tbl.add(new NamedRot("mmt-85",  new int[] {240,    360,    240,    360,    120,    240,    240,    360}));
        tbl.add(new NamedRot("mmm180",  new int[] {240,    360,    240,    360,    240,    360,    0,      240}));
        tbl.add(new NamedRot("mmm-85",  new int[] {240,    360,    240,    360,    240,    360,    240,    360}));
        names.put("arg", tbl);
    }
//}}}

//{{{ CLASS: NamedRot
//##################################################################################################
    private static class NamedRot
    {
        String name;
        int[] bounds;
        
        public NamedRot(String name, int[] bounds)
        {
            this.name = name;
            this.bounds = bounds;
        }
        
        // assumes input angles have already been wrapped
        public boolean contains(double[] ang)
        {
            for(int i = 0; i < bounds.length; i+=2)
            {
                int ii = i / 2;
                if(ang[ii] < bounds[i] || ang[ii] > bounds[i+1]) return false;
            }
            return true;
        }
        
        public String getName()
        { return name; }
    }
//}}}
*/
//{{{ identify
//##################################################################################################
    /**
    * Names the specified sidechain rotamer according to the conventions in the
    * Penultimate Conformer Library.  Returns null if the conformation can't be named.
    * This is ONLY meaningful if evaluate() returns &gt;= 0.01 for the given conformation.
    *
    * @throws IllegalArgumentException if the residue type is unknown
    * @throws AtomException atoms or states are missing
    */
    public String identify(Residue res, ModelState state) { 
      //return identify(res.getName(), scAngles2.measureChiAngles(res, state)); 
      return "conf name";
    }
        
    /**
    * Names the specified sidechain rotamer according to the conventions in the
    * Penultimate Conformer Library.  Returns null if the conformation can't be named.
    * This is ONLY meaningful if evaluate() returns &gt;= 0.01 for the given conformation.
    *
    * @throws IllegalArgumentException if the residue type is unknown
    */
    public String identify(String rescode, double[] chiAngles)
    {
        //rescode = rescode.toLowerCase();
        //for(int i = 0; i < chiAngles.length; i++)
        //{
        //    chiAngles[i] = chiAngles[i] % 360;
        //    if(chiAngles[i] < 0) chiAngles[i] += 360;
        //}
        //// for these residues, the last chi angle is only 0 - 180
        //if("asp".equals(rescode) || "glu".equals(rescode) || "phe".equals(rescode) || "tyr".equals(rescode))
        //{
        //    int i = chiAngles.length - 1;
        //    chiAngles[i] = chiAngles[i] % 180;
        //    if(chiAngles[i] < 0) chiAngles[i] += 180;
        //}
        //
        //Collection tbl = (Collection) names.get(rescode);
        //if(tbl == null)
        //    throw new IllegalArgumentException("Unknown residue type");
        //if(chiAngles == null)
        //    throw new IllegalArgumentException("No chi angles supplied");
        ////if(chiAngles.length < ndft.getDimensions())
        ////    throw new IllegalArgumentException("Too few chi angles supplied");
        //for(int i = 0; i < chiAngles.length; i++) if(Double.isNaN(chiAngles[i]))
        //    throw new IllegalArgumentException("Some chi angles could not be measured");
        //
        //for(Iterator iter = tbl.iterator(); iter.hasNext(); )
        //{
        //    NamedRot nr = (NamedRot) iter.next();
        //    if(nr.contains(chiAngles)) return nr.getName();
        //}
        //return null;
        return "conf name";
    }
//}}}

//{{{ evaluate
//##################################################################################################
    /**
    * Evaluates the specified rotamer from 0.0 (worst)
    * to 1.0 (best).
    * @throws IllegalArgumentException if the residue type is unknown
    * @throws AtomException atoms or states are missing
    */
    public double evaluate(Residue res1, Residue res2, ModelState state) {
      return 100;
    }
    
    /**
    * Evaluates the specified rotamer from 0.0 (worst)
    * to 1.0 (best).
    * @throws IllegalArgumentException if the residue type is unknown
    */
    public double evaluate(String rescode, double[] chiAngles)
    {
        //rescode = rescode.toLowerCase();
        //NDFloatTable ndft = (NDFloatTable)tables.get(rescode);
        //if(ndft == null)
        //    throw new IllegalArgumentException("Unknown residue type");
        //if(chiAngles == null)
        //    throw new IllegalArgumentException("No chi angles supplied");
        //if(chiAngles.length < ndft.getDimensions())
        //    throw new IllegalArgumentException("Too few chi angles supplied");
        //for(int i = 0; i < chiAngles.length; i++) if(Double.isNaN(chiAngles[i]))
        //    throw new IllegalArgumentException("Some chi angles could not be measured");
        //
        //float[] chis = new float[ chiAngles.length ];
        //for(int i = 0; i < chis.length; i++) chis[i] = (float)chiAngles[i];
        //return ndft.valueAt(chis);
        return 100;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

