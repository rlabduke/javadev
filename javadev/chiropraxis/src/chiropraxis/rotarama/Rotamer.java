// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import chiropraxis.sc.SidechainAngles2;
import driftwood.moldb2.*;
//}}}
/**
 * <code>Rotamer</code> is a utility class for
 * evaluating rotamer quality in a protein model.
 *
 * <p>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
 * <br>Begun on Tue Nov 19 12:12:30 EST 2002
*/
public class Rotamer //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    static private Rotamer  instance    = null;
    /** Maps 3-letter res code to NDFT: Map<String, NDFloatTable> */
    HashMap                 tables;
    /** Defines geometry (how to measure chi angles, how many, etc */
    SidechainAngles2        scAngles2;
//}}}

//{{{ getInstance, freeInstance
//##################################################################################################
    /**
    * Retrieves the current Rotamer instance, or
    * creates it and loads the data tables from disk
    * if (1) this method has never been called before
    * or (2) this method has not been called since
    * freeInstance() was last called.
    * If creation fails due to missing resource data,
    * an IOException will be thrown.
    * @throws IOException if a Rotamer instance could not be created
    */
    static public Rotamer getInstance() throws IOException
    {
        if(instance != null)    return instance;
        else                    return (instance = new Rotamer());
    }
    
    /**
    * Frees the internal reference to the allocated Rotamer object.
    * It will be GC'ed when all references to it expire.
    * Future calls to getInstance() will allocate a new Rotamer object.
    * This function allows sneaky users to have more than one Rotamer
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
    private Rotamer() throws IOException
    {
        loadTablesFromJar();
        this.scAngles2  = new SidechainAngles2();
    }
//}}}

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

//{{{ evaluate
//##################################################################################################
    /**
    * Evaluates the specified rotamer from 0.0 (worst)
    * to 1.0 (best).
    * @throws IllegalArgumentException if the residue type is unknown
    * @throws AtomException atoms or states are missing
    */
    public double evaluate(Residue res, ModelState state)
    {
        return evalImpl2(res.getName(), scAngles2.measureChiAngles(res, state));
    }
    
    double evalImpl2(String rescode, double[] chiAngles)
    {
        rescode = rescode.toLowerCase();
        NDFloatTable ndft = (NDFloatTable)tables.get(rescode);
        if(ndft == null)
            throw new IllegalArgumentException("Unknown residue type");
        if(chiAngles == null)
            throw new IllegalArgumentException("No chi angles supplied");
        if(chiAngles.length < ndft.getDimensions())
            throw new IllegalArgumentException("Too few chi angles supplied");
        
        float[] chis = new float[ chiAngles.length ];
        for(int i = 0; i < chis.length; i++) chis[i] = (float)chiAngles[i];
        return ndft.valueAt(chis);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

