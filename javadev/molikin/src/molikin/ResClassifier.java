// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

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
//}}}
/**
* <code>ResClassifier</code> is responsible for categorizing Residues as one of
* PROTEIN, NUCACID, WATER, ION (single atom residues named as a known element),
* OHET (organic/other het - not water or ion, but mostly HETATMs),
* or UNKNOWN(like ohet, but mostly ATOMs).
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Sep 29 11:04:27 EDT 2005
*/
public class ResClassifier //extends ... implements ...
{
//{{{ Constants
    /** Residue name matches a known amino acid designation */
    static public final Object PROTEIN = "protein";
    
    /** Residue name matches a known nucleic acid designation */
    static public final Object NUCACID = "nucleic_acid";
    
    /** Residue name matches a known water designation */
    static public final Object WATER = "water";
    
    /** Residue name matches a known element and has exactly one atom */
    static public final Object ION = "ion";
    
    /** Residue is not protein, nuc acid, water, or ion and is mostly HETATMs */
    static public final Object OHET = "other_het";
    
    /** Residue is not protein, nuc acid, water, or ion and is mostly ATOMs */
    static public final Object UNKNOWN = "unknown";
//}}}

//{{{ Variable definitions
//##############################################################################
    Map map = new HashMap(); // Residue to one of the defined classes
    
    // Sets of the above classifications
    public Set proteinRes   = new CheapSet();
    public Set nucAcidRes   = new CheapSet();
    public Set waterRes     = new CheapSet();
    public Set ionRes       = new CheapSet();
    public Set ohetRes      = new CheapSet();
    public Set unknownRes   = new CheapSet();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ResClassifier(Collection residues)
    {
        super();
        
        // For each residue, first try to guess what it is on its own.
        // Then compare backward and forward to catch things like seleno-Met
        // and the GFP fluorophore, which should be treated as "normal" biopolymers.
        
        Residue prevRes = null;
        Object prevClas = UNKNOWN;
        ArrayList unknowns = new ArrayList();
        
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue res = (Residue) iter.next();
            Object clas;
            if(Util.isProtein(res))                                 clas = PROTEIN;
            else if(Util.isNucleicAcid(res))                        clas = NUCACID;
            else if(Util.isWater(res))                              clas = WATER;
            else if(Util.isIon(res) && res.getAtoms().size() == 1)  clas = ION;
            // if mostly HETATMs, call it OHET; else call it UNKNOWN
            else
            {
                int hetCount = 0;
                for(Iterator i = res.getAtoms().iterator(); i.hasNext(); )
                {
                    Atom a = (Atom) i.next();
                    if(a.isHet()) hetCount++;
                }
                
                if(hetCount > res.getAtoms().size() - hetCount)     clas = OHET;
                else                                                clas = UNKNOWN;
            }
            
            // If current is unknown and was preceded by protein/nucleic acid,
            // treat it as a continuation of that.
            // Else it might be followed by protein/nucleic acid later on.
            //
            // On including OHETs vs. not in the relabeling scheme:
            // MODRES in protein backbond are usually done with HETATMs,
            // as are selenomethionine (but we allow for those in isProtein).
            // However, an SO4 following protein but still in chain _ would be
            // reclassified as protein if we allow OHETs to be relabeled like this.
            if(clas == UNKNOWN)// || clas == OHET)
            {
                if((prevClas == PROTEIN || prevClas == NUCACID) && isSameChain(prevRes, res))
                    clas = prevClas;
                else
                    unknowns.add(res);
            }
            // If current is protein/nucleic acid, it might have been preceded
            // by some unknowns, which should be treated as part of this.
            else if(clas == PROTEIN || clas == NUCACID)
            {
                if(unknowns.size() > 0)
                {
                    for(Iterator i = unknowns.iterator(); i.hasNext(); )
                    {
                        prevRes = (Residue) i.next();
                        if(isSameChain(prevRes, res)) map.put(prevRes, clas);
                        if(clas == PROTEIN)         proteinRes.add(prevRes);
                        else if(clas == NUCACID)    nucAcidRes.add(prevRes);
                    }
                    unknowns.clear();
                }
            }
            // If current is ion or water, any preceding unknowns
            // have to remain just that -- unknown.
            else unknowns.clear();
            //System.out.println(res +" "+ clas);
            map.put(res, clas);
            if(clas == PROTEIN)         proteinRes.add(res);
            else if(clas == NUCACID)    nucAcidRes.add(res);
            else if(clas == WATER)      waterRes.add(res);
            else if(clas == ION)        ionRes.add(res);
            else if(clas == OHET)       ohetRes.add(res);
            else if(clas == UNKNOWN)    unknownRes.add(res);
            
            prevRes = res;
            prevClas = clas;
        }
        //System.out.println("prot=" + proteinRes);
    }
//}}}

//{{{ isSameChain, classify
//##############################################################################
    private boolean isSameChain(Residue a, Residue b)
    {
        return (a.getChain().equals(b.getChain())   // same chain ID
            && a.sectionID == b.sectionID);         // no TER cards between them
    }
    
    /** Analyzes the Residue and determines which class it belongs to. */
    public Object classify(Residue res)
    { return map.get(res); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

