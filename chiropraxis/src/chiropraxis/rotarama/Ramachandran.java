// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

//import java.awt.*;
import java.io.*;
//import java.net.*;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Ramachandran</code> is a simplified version of
* my hless.Ramachandran evaluator and plot generator.
* This version is simply for evaluating the Ramachandran
* fitness of residues programatically.
*
* <p>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Sep 10 09:08:41 EDT 2002
*/
public class Ramachandran //extends ... implements ...
{
//{{{ Constants
    public static final double ALL_FAVORED          = 0.0200;   // 98.00%
    public static final double GENERAL_ALLOWED      = 0.0005;   // 99.95%
    public static final double CISPRO_ALLOWED       = 0.0020;   // 99.80%
    public static final double OTHER_ALLOWED        = 0.0010;   // 99.90%
    /*
    public static final double GENERAL_ALLOWED      = 0.0005;   // 99.95%
    public static final double OTHER_ALLOWED        = 0.0020;   // 99.80%
    */
//}}}

//{{{ Variable definitions
//##################################################################################################
    static private Ramachandran instance    = null;
    float[]                     phipsi      = new float[2];
    NDFloatTable                genTable      = null, ilevalTable = null,
                                preproTable   = null, glyTable    = null, 
                                transproTable = null, cisproTable = null;
                                /*proTable = null;*/
//}}}

//{{{ getInstance, freeInstance
//##################################################################################################
    /**
    * Retrieves the current Ramachandran instance, or
    * creates it and loads the data tables from disk
    * if (1) this method has never been called before
    * or (2) this method has not been called since
    * freeInstance() was last called.
    * If creation fails due to missing resource data,
    * an IOException will be thrown.
    * @throws IOException if a Ramachandran instance could not be created
    */
    static public Ramachandran getInstance() throws IOException
    {
        if(instance != null)    return instance;
        else                    return (instance = new Ramachandran());
    }
    
    /**
    * Frees the internal reference to the allocated Ramachandran object.
    * It will be GC'ed when all references to it expire.
    * Future calls to getInstance() will allocate a new Ramachandran object.
    * This function allows sneaky users to have more than one Ramachandran
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
    * Creates a new Ramachandran scorer.
    * @throws IOException if the required resources are not available
    */
    private Ramachandran() throws IOException
    {
        InputStream sGen, sIleval, sPrepro, sGly, sCispro, sTranspro; /*, sPro;*/
        sGen      = this.getClass().getResourceAsStream("rama8000/general.ndft");
        sIleval   = this.getClass().getResourceAsStream("rama8000/ileval.ndft");
        sPrepro   = this.getClass().getResourceAsStream("rama8000/prepro.ndft");
        sGly      = this.getClass().getResourceAsStream("rama8000/glycine.ndft");
        sTranspro = this.getClass().getResourceAsStream("rama8000/transpro.ndft");
        sCispro   = this.getClass().getResourceAsStream("rama8000/cispro.ndft");
        /*sPro      = this.getClass().getResourceAsStream("rama5200/proline.ndft");*/
        
        if(sGen == null || sIleval == null || sPrepro == null || sGly == null || sTranspro == null || sCispro == null) 
            throw new IOException("Could not find required .ndft files");
        
        DataInputStream dis;
        dis = new DataInputStream(new BufferedInputStream(sGen));
          genTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sIleval));
          ilevalTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sPrepro));
          preproTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sGly));
          glyTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sTranspro));
          transproTable = new NDFloatTable(dis);
        dis = new DataInputStream(new BufferedInputStream(sCispro));
          cisproTable = new NDFloatTable(dis);
        /*dis = new DataInputStream(new BufferedInputStream(sPro));
          proTable = new NDFloatTable(dis);*/
        
        sGen.close();
        sIleval.close();
        sPrepro.close();
        sGly.close();
        sTranspro.close();
        sCispro.close();
        /*sPro.close();*/
    }
//}}}

//{{{ rawScore
//##################################################################################################
    /**
    * Returns true iff the given residue can reasonably be
    * scored on the Ramachandran plot and it is neither
    * favored nor allowed.
    * @throws ResidueException if no score can be calculated
    *   for this residue.
    */
    public double rawScore(Model model, Residue res, ModelState state) throws ResidueException
    {
        double phi, psi, score;
        try {
            phi = AminoAcid.getPhi(model, res, state);
            psi = AminoAcid.getPsi(model, res, state);
        } catch(AtomException ex)
        { throw new ResidueException("Can't get Ramachandran score for "+res+": "+ex.getMessage()); }
        
        phipsi[0] = (float)phi;
        phipsi[1] = (float)psi;
        String name = res.getName();
        
        if(name.equals("GLY"))
            score = glyTable.valueAt(phipsi);
        else if(name.equals("PRO"))
        {
            if(AminoAcid.isCloserToCis(model, res, state))
                score = cisproTable.valueAt(phipsi);
            else
                score = transproTable.valueAt(phipsi);
        }
            /*score = proTable.valueAt(phipsi);*/
        else if(AminoAcid.isPrepro(model, res, state))
            score = preproTable.valueAt(phipsi);
        else if(name.equals("ILE") || name.equals("VAL"))
            score = ilevalTable.valueAt(phipsi);
        else
            score = genTable.valueAt(phipsi);
        
        return score;
    }
//}}}

//{{{ isOutlier
//##################################################################################################
    /**
    * Returns true iff the given residue can reasonably be
    * scored on the Ramachandran plot and it is neither
    * favored nor allowed.
    */
    public boolean isOutlier(Model model, Residue res, ModelState state)
    {
        String protein = "GLY,ALA,VAL,LEU,ILE,PRO,PHE,TYR,TRP,SER,THR,CYS,MET,MSE,LYS,HIS,ARG,ASP,ASN,GLN,GLU";
        String resType = res.getName();
        if(protein.indexOf(resType) == -1) return false;
        
        try
        {
            double score = rawScore(model, res, state);
            if(resType.equals("PRO") && AminoAcid.isCloserToCis(model, res, state))
            {
                return (score < CISPRO_ALLOWED);
            }
            else if(resType.equals("GLY")
                || AminoAcid.isPrepro(model, res, state)
                || resType.equals("ILE") || resType.equals("VAL"))
            {
                return (score < OTHER_ALLOWED);
            }
            else
            {
                return (score < GENERAL_ALLOWED);
            }
            /*if(resType.equals("GLY")
                || resType.equals("PRO")
                || AminoAcid.isPrepro(model, res, state))
            { return (score < OTHER_ALLOWED); }*/        
        }
        catch(ResidueException ex)
        { return false; }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

