// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.Strings;
//}}}
/**
* <code>PdbWriter</code> allows output of models in PDB format.
*
* <p>PdbReader and PdbWriter fail to deal with the following kinds of records:
* <ul>  <li>SIGATM</li>
*       <li>ANISOU</li>
*       <li>SIGUIJ</li>
*       <li>CONECT</li> </ul>
* <p>Actually, PdbReader puts all of them in with the headers, and
* PdbWriter spits them all back out, but it may not put them in the
* right part of the file.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jun 23 09:30:27 EDT 2003
*/
public class PdbWriter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df2 = new DecimalFormat("0.00");
    static final DecimalFormat df3 = new DecimalFormat("0.000");
//}}}

//{{{ Variable definitions
//##################################################################################################
    /** The output sink for PDB-format data. Remember to flush() when you're done! */
    PrintWriter         out;
    
    /** Whether we should use existing AtomState serial numbers or calculate our own */
    boolean             renumberAtoms   = false;
    
    /** The current number for renumbering atoms on output */
    int atomSerial = 1;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /** Constructor for writing to an output stream. */
    public PdbWriter(OutputStream os)
    {
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    }

    /** Constructor for writing to a writer. */
    public PdbWriter(Writer w)
    {
        out = new PrintWriter(new BufferedWriter(w));
    }

    /** Constructor for writing to a file. */
    public PdbWriter(File f) throws IOException
    {
        out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
    }
//}}}

//{{{ close, get/setRenumberAtoms
//##################################################################################################
    /** Closes the underlying stream. Useful with e.g. {@link #PdbWriter(File)}. */
    public void close()
    {
        out.close();
    }
    
    /** If true, atoms will be renumbered from 1 when writen out. */
    public boolean getRenumberAtoms()
    { return renumberAtoms; }
    
    /** Turns on or off atom renumbering, and resets the counter to 1. */
    public void setRenumberAtoms(boolean b)
    {
        renumberAtoms   = b;
        atomSerial      = 1; 
    }
//}}}

//{{{ writeAtom
//##################################################################################################
    /**
    * Writes out a single of ATOM or HETATM line.
    */
    public void writeAtom(AtomState as)
    {
        try
        {
            Residue         res = as.getResidue();
            StringBuffer    sb  = new StringBuffer(80);
            
            if(as.isHet())  sb.append("HETATM");
            else            sb.append("ATOM  ");
            
            String serial = as.getSerial();
            if(renumberAtoms) serial = Integer.toString(atomSerial++);
            if(serial.length() > 5) serial.substring(0, 5);
            else if(serial.length() < 5) serial = Strings.justifyRight(serial, 5);
            sb.append(serial);
            sb.append(" "); // unused
            
            sb.append(as.getName());
            String altconf = as.getAltConf();
            if(altconf.length() > 1) altconf = altconf.substring(0, 1);
            else if(altconf.length() == 0) altconf = " ";
            sb.append(altconf);
            
            sb.append(Strings.justifyLeft(res.getName(), 3));
            sb.append(" "); // unused
            sb.append(res.getChain());
            
            String seqNum = res.getSequenceNumber();
            sb.append(Strings.justifyRight(seqNum, 4));
            sb.append(res.getInsertionCode());
            sb.append("   "); // unused
            
            sb.append(Strings.justifyRight(df3.format(as.getX()), 8));
            sb.append(Strings.justifyRight(df3.format(as.getY()), 8));
            sb.append(Strings.justifyRight(df3.format(as.getZ()), 8));
            sb.append(Strings.justifyRight(df2.format(as.getOccupancy()), 6));
            sb.append(Strings.justifyRight(df2.format(as.getTempFactor()), 6));
            sb.append("      "); // unused
            
            String seg = res.getSegment();
            if(seg == null) seg = "    "; // should never happen
            else if(seg.length() > 4) seg = seg.substring(0,4);
            sb.append(Strings.justifyLeft(seg, 4));
            sb.append("  "); // element
            if(as.getCharge() == 0.0)   sb.append("  ");
            else if(as.getCharge() > 0) sb.append(((int)as.getCharge())+"+");
            else                        sb.append(((int)as.getCharge())+"-");
            
            out.println(sb);
            out.flush();
        }
        catch(NullPointerException ex) {}
    }
//}}}

//{{{ writeResidues
//##################################################################################################
    /**
    * Write the given residues with no additional header information.
    * The residues will be sorted into their natural order before being written.
    */
    public void writeResidues(Collection residues, ModelState state)
    {
        Residue[] res = (Residue[])residues.toArray(new Residue[residues.size()]);
        Arrays.sort(res);
        
        for(int i = 0; i < res.length; i++)
        {
            for(Iterator iter = res[i].getAtoms().iterator(); iter.hasNext(); )
            {
                try
                {
                    Atom        a   = (Atom)iter.next();
                    AtomState   as  = state.get(a);
                    writeAtom(as);
                }
                catch(AtomException ex) {} // missing state
            }//for each atom
        }//for each residue
    }
//}}}

//{{{ writeModelGroup
//##################################################################################################
    /**
    * Writes out a whole group of models, complete with
    * all header information. This function should generate
    * a PDB file with (almost?) all of the information present
    * in the original that was read by PdbReader.
    * @param modelGroup     the group of models to be written out.
    * @param modelStates    a Map&lt;Model, Collection&lt;ModelState&gt;&gt; of states to write.
    *   That is, for each Model there should be an entry in this map that
    *   contains a Collection of the states one would like to write out.
    *   If this Collection is missing or null, it will be obtained from Model.getStates().
    */
    public void writeModelGroup(ModelGroup modelGroup, Map modelStates)
    {
        for(Iterator iter = modelGroup.getHeaders().iterator(); iter.hasNext(); )
        {
            String header = iter.next().toString(); // they should already be Strings
            if(!header.startsWith("CONECT")) out.println(header);
        }
        
        for(Iterator iter = modelGroup.getModels().iterator(); iter.hasNext(); )
        {
            Model model = (Model)iter.next();
            if(modelGroup.getModels().size() > 1) // only use MODEL when >1
                out.println("MODEL     "+Strings.justifyRight(model.getName(), 4));
            
            
            Collection stateSet = (Collection)modelStates.get(model);
            if(stateSet == null) stateSet = model.getStates();
            ModelState[] states = (ModelState[])stateSet.toArray(new ModelState[stateSet.size()]);
            
            writeModel(model, states);
            
            
            if(modelGroup.getModels().size() > 1)
                out.println("ENDMDL");
        }//for each model
        
        // This only makes sense if we haven't renumbered the atoms!
        if(!renumberAtoms)
        {
            for(Iterator iter = modelGroup.getHeaders().iterator(); iter.hasNext(); )
            {
                String header = iter.next().toString(); // they should already be Strings
                if(header.startsWith("CONECT")) out.println(header);
            }
        }
        
        // we don't output a MASTER checksum record,
        // though it wouldn't be hard if someone wants to implement it.
        out.println("END   ");
        out.flush();
    }
//}}}

//{{{ writeModel
//##################################################################################################
    private void writeModel(Model model, ModelState[] states)
    {
        Set outputStates = new HashSet(); // to avoid duplicates
        
        Residue oldRes = null;
        for(Iterator ri = model.getResidues().iterator(); ri.hasNext(); )
        {
            Residue res = (Residue)ri.next();
            for(Iterator ai = res.getAtoms().iterator(); ai.hasNext(); )
            {
                Atom atom = (Atom)ai.next();
                for(int i = 0; i < states.length; i++)
                {
                    try
                    {
                        AtomState as = states[i].get(atom);
                        // We want to make sure every atom output has a unique PDB name.
                        // We're not worried so much about duplicating coordinates (old code).
                        // Name requirement is important for dealing with alt confs,
                        // where a single atom (' ') may move in A but not B --
                        // this led to two ATOM entries with different coords but the same name.
                        String aName = as.getAtom().toString()+as.getAltConf();
                        //if(!outputStates.contains(as))
                        if(!outputStates.contains(aName))
                        {
                            //outputStates.add(as);
                            outputStates.add(aName);
                            writeAtom(as);
                        }
                    }
                    catch(AtomException ex) {} // no state
                }
            }//for each atom

            // insert TER record
            if(oldRes != null && !oldRes.getChain().equals(res.getChain()))
            {
                StringBuffer    sb  = new StringBuffer(27).append("TER   ");
                // This gets the serial number wrong unless we're renumbering,
                // but nobody uses them anyway...
                sb.append(Strings.justifyRight(Integer.toString(atomSerial++), 5));
                sb.append("      "); // unused
                sb.append(Strings.justifyLeft(oldRes.getName(), 3));
                sb.append(" "); // unused
                sb.append(oldRes.getChain());
                sb.append(Strings.justifyRight(oldRes.getSequenceNumber(), 4));
                sb.append(oldRes.getInsertionCode());
            }
            
            oldRes = res;
        }// for each residue
            
        out.flush();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

