// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>TauByPhiPsi</code> computes an expected tau value,
* or tau deviation, for a particular residue based on its
* position on the Ramachandran plot (phi, psi)
* and class (general, gly, pro, or pre-pro).
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jan  9 16:02:07 EST 2004
*/
public class TauByPhiPsi //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    static private TauByPhiPsi  instance    = null;
    float[]                     phipsi      = new float[2];
    NDFloatTable                genTable = null, glyTable = null,
                                proTable = null, preproTable = null;
//}}}

//{{{ getInstance, freeInstance
//##################################################################################################
    /**
    * Retrieves the current TauByPhiPsi instance, or
    * creates it and loads the data tables from disk
    * if (1) this method has never been called before
    * or (2) this method has not been called since
    * freeInstance() was last called.
    * If creation fails due to missing resource data,
    * an IOException will be thrown.
    * @throws IOException if a TauByPhiPsi instance could not be created
    */
    static public TauByPhiPsi getInstance() throws IOException
    {
        if(instance != null)    return instance;
        else                    return (instance = new TauByPhiPsi());
    }
    
    /**
    * Frees the internal reference to the allocated TauByPhiPsi object.
    * It will be GC'ed when all references to it expire.
    * Future calls to getInstance() will allocate a new TauByPhiPsi object.
    * This function allows sneaky users to have more than one TauByPhiPsi
    * object in memory at once. This is generally a bad idea, as they're
    * really big, but we won't stop you if you're sure that's what you want.
    */
    static public void freeInstance()
    {
        instance = null;
    }
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a new TauByPhiPsi scorer.
    * @throws IOException if the required resources are not available
    */
    private TauByPhiPsi() throws IOException
    {
        super();
        
        InputStream sGen, sGly, sPro, sPrepro;
        sGen    = this.getClass().getResourceAsStream("p_a_karplus_1996/tau-general.ndft");
        sGly    = this.getClass().getResourceAsStream("p_a_karplus_1996/tau-glycine.ndft");
        sPro    = this.getClass().getResourceAsStream("p_a_karplus_1996/tau-proline.ndft");
        sPrepro = this.getClass().getResourceAsStream("p_a_karplus_1996/tau-prepro.ndft");
        
        if(sGen == null || sGly == null || sPro == null || sPrepro == null)
            throw new IOException("Could not find required .ndft files");
        
        DataInputStream dis;
        dis = new DataInputStream(new BufferedInputStream(sGen));
        genTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sGly));
        glyTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sPro));
        proTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sPrepro));
        preproTable = new NDFloatTable(dis);

        sGen.close();
        sGly.close();
        sPro.close();
        sPrepro.close();
    }
//}}}

//{{{ getExpectedTau
//##################################################################################################
    /**
    * Returns the expected tau angle (N-CA-C) for the given residue,
    * given its class (general, gly, pro, pre-pro) and phi,psi.
    * @throws ResidueException if no angle can be calculated
    *   for this residue.
    */
    public double getExpectedTau(Model model, Residue res, ModelState state) throws ResidueException
    {
        double phi, psi, angle;
        try {
            phi = AminoAcid.getPhi(model, res, state);
            psi = AminoAcid.getPsi(model, res, state);
        } catch(AtomException ex)
        { throw new ResidueException("Can't get tau angle for "+res+": "+ex.getMessage()); }
        
        phipsi[0] = (float)phi;
        phipsi[1] = (float)psi;
        String name = res.getName();
        
        if(name.equals("GLY"))
            angle = glyTable.valueAt(phipsi);
        else if(name.equals("PRO"))
            angle = proTable.valueAt(phipsi);
        else if(AminoAcid.isPrepro(model, res, state))
            angle = preproTable.valueAt(phipsi);
        else
            angle = genTable.valueAt(phipsi);
        
        return angle;
    }
//}}}

//{{{ getTauDeviation
//##################################################################################################
    /**
    * Returns the deviation of this residue's tau angle (N-CA-C) from expected,
    * given its class (general, gly, pro, pre-pro) and phi,psi.
    * @throws ResidueException if no angle can be calculated
    *   for this residue.
    */
    public double getTauDeviation(Model model, Residue res, ModelState state) throws ResidueException
    {
        try
        {
            double expected = getExpectedTau(model, res, state);
            double actual   = AminoAcid.getTau(res, state);
            return actual - expected;
        }
        catch(AtomException ex) { throw new ResidueException("Couldn't calculate tau: "+ex.getMessage()); }
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

