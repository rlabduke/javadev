// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.nmr;

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
* <code>DyanaReader</code> loads data in the format(s)
* native to the NMR program DYANA.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul  9 16:33:08 EDT 2003
*/
public class DyanaReader //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    /** The model we're consulting for building up data */
    Model           theModel;
    
    /** All the residues we're searching through for matching names */
    Residue[]       noeResidues     = null;
    
    /** A Map&lt;String, Residue&gt; for caching name lookup results */
    Map             noeNameCache    = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public DyanaReader(Model model)
    {
        super();
        this.theModel   = model;
    }
//}}}

//{{{ readNOEs (convenience)
//##############################################################################
    public Collection readNOEs(File f) throws IOException
    {
        Collection residues = theModel.getResidues();
        LineNumberReader in = new LineNumberReader(new FileReader(f));
        Collection noes = readNOEs(in, residues);
        in.close();
        return noes;
    }
//}}}

//{{{ readNOEs
//##############################################################################
    /**
    * Reads a set of NOE records from the given stream.
    * @param in         the stream to read from
    * @param residues   the residues to search (Model.getResidues() by default)
    * @return           a Collection&lt;NoeConstraint&gt;
    */
    public Collection readNOEs(LineNumberReader in, Collection residues) throws IOException
    {
        noeResidues     = (Residue[])residues.toArray(new Residue[residues.size()]);
        noeNameCache    = new HashMap();
        
        Collection atoms1   = new ArrayList();
        Collection atoms2   = new ArrayList();
        Collection noes     = new ArrayList();
        
        String s;
        while((s = in.readLine()) != null)
        {
            if(s.length() < 36) continue;
            try
            {
                findNoeAtoms(s.substring(0,13), atoms1);
                findNoeAtoms(s.substring(15,28), atoms2);
                double dist         = Double.parseDouble(s.substring(28,36).trim());
                NoeConstraint noe   = new NoeConstraint(atoms1, atoms2, dist);
                noes.add(noe);
            }
            catch(NumberFormatException ex)
            {
                ex.printStackTrace();
                System.err.println("[line "+in.getLineNumber()+"] Bad number in DYANA NOE record");
            }
            catch(ResidueException ex)
            {
                //ex.printStackTrace();
                //System.err.println("*** [line "+in.getLineNumber()+"] Named residue/atom does not exist");
                System.err.println("[line "+in.getLineNumber()+"] "+ex.getMessage());
            }
            catch(IllegalArgumentException ex)
            {
                ex.printStackTrace();
                System.err.println("[line "+in.getLineNumber()+"] No atoms matching descriptor were found");
            }
        }
        
        return noes;
    }
//}}}

//{{{ findNoeAtoms
//##############################################################################
    /**
    * The given string will be parsed to find the first matching
    * Residue and all matching Atoms within it.
    * The Atoms will be placed into <code>atoms</code> after it
    * has been <code>clear</code>ed.
    * @throws ResidueException if the named residue couldn't be found
    */
    private void findNoeAtoms(String spec, Collection atoms) throws NumberFormatException
    {
        atoms.clear();
        
        // First, find the right residue
        Residue r = (Residue)noeNameCache.get(spec);
        if(r == null)
        {
            int seqNum      = Integer.parseInt(spec.substring(0,3).trim());
            char iCode      = spec.charAt(3);
            String resName  = spec.substring(4,7);
            for(int i = 0; i < noeResidues.length; i++)
            {
                if(noeResidues[i].getSequenceNumber()   == seqNum
                && noeResidues[i].getInsertionCode()    == iCode
                && noeResidues[i].getName().equals(resName))
                {
                    r = noeResidues[i];
                    noeNameCache.put(spec, r); // save it for later!
                    break;
                }
            }
        }
        
        if(r == null)
            throw new ResidueException("No residue was found that matches '"+spec+"'");
        
        // Then find the right atom(s)
        String noeAtom = spec.substring(9,13).trim();
        
        if(noeAtom.charAt(0) == 'Q')
        {
            noeAtom = noeAtom.substring(1);
            for(Iterator iter = r.getAtoms().iterator(); iter.hasNext(); )
            {
                Atom a = (Atom)iter.next();
                String aName = a.getName();
                aName = aName.substring(1,4).trim()+aName.substring(0,1).trim();
                if(aName.endsWith(noeAtom))
                    atoms.add(a);
            }
        }
        else
        {
            DyanaTranslator trans = DyanaTranslator.getInstance();
            String pdbAtom = trans.translate(r.getName(), noeAtom);
            Atom a = r.getAtom(pdbAtom);
            if(a != null) atoms.add(a);
        }
        
        if(atoms.size() < 1)
            throw new ResidueException(r+" has no atom matching '"+noeAtom+"'");
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

