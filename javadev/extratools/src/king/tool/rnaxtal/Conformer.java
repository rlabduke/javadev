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
import driftwood.data.*;
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
  //public static final String REMOTENESS = "HC5C4C3O2C1NCO4O3P";
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}

//{{{ Variable definitions
//##################################################################################################
    static private Conformer  instance    = null;
    /** Maps 3-letter res code to NDFT: Map<String, NDFloatTable> */
    HashMap                 tables;
    /** Maps lower case 3-letter codes to Lists of NamedConf objects */
    UberMap                 conformerMap;
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
      conformerMap = new UberMap();
      loadConformerData();
        //loadConformerNames();
        //this.scAngles2  = new SidechainAngles2();
    }
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
*/
//{{{ loadConformerData
//##################################################################################################
    private void loadConformerData() throws IOException
    {
//conformer file should have the following columns:
//        |--------------------averages----------------------------------------|-----------------sigmas---------------------------------------|
//bin,name,chi-1,delta-1,beta-1,gamma-1,epsilon,zeta,alpha,beta,gamma,delta,chi,chi-1,del-1,beta-1,gamma-1,epsil,zeta,alpha,beta,gam,delta,chi
//Note that some of the confs (especially wannabes) don't have chi data
      InputStream is = this.getClass().getResourceAsStream("rnaclusters070506.csv");
      if(is == null) throw new IOException("File not found in JAR: rnaclusters070506.csv");
      
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = reader.readLine())!= null) {
        if (!line.startsWith("#")) {
          String[] split = Strings.explode(line, ',', true, false);
          String bin = split[0];
          String confName = split[1];
          String[] avgs = new String[11];
          String[] sigma = new String[11];
          System.arraycopy(split, 2, avgs, 0, 11);
          System.arraycopy(split, 13, sigma, 0, 11);
          //System.out.println(Arrays.toString(avgs));
          //System.out.println(Arrays.toString(sigma));
          double[] avgDoubles = Strings.arrayToDouble(avgs);
          double[] sigDoubles = Strings.arrayToDouble(sigma);
          NamedConf conf = new NamedConf(bin, confName, avgDoubles, sigDoubles);
          conformerMap.put(confName, conf);
          //System.out.print("avgs:");
          //for (String s : avgs) {System.out.print(s+",");}
          //System.out.println();
          //System.out.print("sigmas");
          //for (String s : sigma) {System.out.print(s+",");}
          //System.out.println();
        }
      }
    }
//}}}

//{{{ CLASS: NamedConf
//##################################################################################################
    private static class NamedConf
    {
      String bin;
        String name;
        double[] bounds;
        double[] means;
        
        public NamedConf(String bin, String name, double[] avgs, double[] sigmas)
        {
          this.bin = bin;
          this.name = name;
          this.means = avgs;
          bounds = new double[avgs.length*2];
          for(int i = 0; i < avgs.length; i+=2) {
            if (sigmas[i/2] == Double.NaN) bounds[i] = Double.NaN;
            else {
              bounds[i] = avgs[i/2]-sigmas[i/2]; //lower bound
              bounds[i+1] = avgs[i/2]+sigmas[i/2]; //upper bound
            }
          }
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
        
        public String getBin()
        { return bin.substring(0, 2); }
        
        public String getName()
        { return name; }
        
        public double[] getMeans() 
        { return means; }
          
    }
//}}}

//{{{ getDefinedConformerNames
public String[] getDefinedConformerNames() {
  Collection conformerSet = conformerMap.values();
  String[] confNames = new String[conformerSet.size()];
  int i = 0;
  Iterator iter = conformerSet.iterator();
  while (iter.hasNext()) {
    confNames[i] = ((NamedConf)iter.next()).getName();
    i++;
  }
  return confNames;
}
//}}}

  //{{{ getConformerBin
  public String getConformerBin(String confName) {
    if (!conformerMap.containsKey(confName)) return null;
    NamedConf conf = (NamedConf)conformerMap.get(confName);
    return conf.getBin();
  }
  //}}}
  
  //{{{ getMeanValues
  public double[] getMeanValues(String confName) {
    if (!conformerMap.containsKey(confName)) return null;
    NamedConf conf = (NamedConf)conformerMap.get(confName);
    return conf.getMeans();
  }
  //}}}

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
        //    NamedConf nr = (NamedConf) iter.next();
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

