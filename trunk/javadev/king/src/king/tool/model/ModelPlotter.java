// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.model;
import king.*;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.util.*;
//}}}
/**
* <code>ModelPlotter</code> is capable of taking molecular
* structures from driftwood.moldb2 and rendering them as
* kinemage entities.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 16:12:43 EST 2003
*/
public class ModelPlotter
{
//{{{ Constants
    static final DecimalFormat df2 = new DecimalFormat("0.00");
//}}}

//{{{ Variable definitions
//##################################################################################################
    Props       scProps;
    VectorPoint prev    = null;

    KList       listCa  = null;
    KList       listMc  = null;
    KList       listMcH = null;
    KList       listSc  = null;
    KList       listScH = null;
    
    public KPaint   mainColor   = KPalette.defaultColor;
    public KPaint   sideColor   = KPalette.defaultColor;
    public KPaint   hyColor     = KPalette.defaultColor;
    public int      modelWidth  = 2;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ModelPlotter()
    {
        // Load side chain connectivity database
        scProps = new Props();
        try
        {
            InputStream is = getClass().getResourceAsStream("sc-connect.props");
            if(is != null)
            {
                scProps.load(is);
                is.close();
            }
            else SoftLog.err.println("Couldn't find sc-connect.props");
        }
        catch(IOException ex)
        { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ plotAminoAcids
//##################################################################################################
    /**
    * Creates a plot of the given protein residues (backbone and sidechain)
    * @param residues a Collection of Residues to plot. If this is large,
    *   it should probably be a Set, for performance reasons.
    * @param state the conformation that should be plotted
    */
    public void plotAminoAcids(Model model, Collection residues, ModelState state)
    {
        if(listCa == null || listMc == null || listMcH == null
        || listSc == null || listScH == null) createLists();
        
        clearLists();
        
        // For each residue, draw mainchain, then draw sidechain.
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
        {
            Residue r       = (Residue)iter.next();
            Residue next    = r.getNext(model); // next along the chain
            
            // Heavy atom backbone trace
            plotString(r, state, scProps.getString("aminoacid.mc", ""), listMc);
            // Hydrogen backbone trace
            plotString(r, state, scProps.getString("aminoacid.hy", ""), listMcH);

            // Heavy atom sidechain trace
            String tlc = r.getName().toLowerCase();
            plotString(r, state, scProps.getString(tlc+".sc", ""), listSc);
            // Hydrogen sidechain trace
            plotString(r, state, scProps.getString(tlc+".hy", ""), listScH);

            // Connect the backbone between residues as necessary
            if(next != null && residues.contains(next))
            {
                Atom C, N;
                C = r.getAtom(" C  ");
                N = next.getAtom(" N  ");
                if(C != null && N != null)
                {
                    try {
                        plotAtom(state.get(C),  false,  listMc);
                        plotAtom(state.get(N),  true,   listMc);
                    } catch(AtomException ex) { SoftLog.err.println(ex.getMessage()); }
                }
                
                Atom Ca1, Ca2;
                Ca1 = r.getAtom(" CA ");
                Ca2 = next.getAtom(" CA ");
                if(Ca1 != null && Ca2 != null)
                {
                    try {
                        plotAtom(state.get(Ca1),  false,  listCa);
                        plotAtom(state.get(Ca2),  true,   listCa);
                    } catch(AtomException ex) { SoftLog.err.println(ex.getMessage()); }
                }
            }
        }//for(each residue)
        
        prev = null; //avoid memory leaks
    }
//}}}

//{{{ plotString
//##################################################################################################
    /**
    * Syntax for connectivity strings is "aaaa,bbbb,cccc;dddd,eeee"
    * where commas separate 4-character atom IDs and semi-colons
    * denote breaks in the chain (like P in kinemage format).
    */
    void plotString(Residue res, ModelState state, String connect, KList drawList)
    {
        String          token;
        StringTokenizer tokenizer   = new StringTokenizer(connect, ",;", true);
        boolean         lineto      = false;
        Atom            atom;
        
        while(tokenizer.hasMoreTokens())
        {
            token = tokenizer.nextToken();
            if(token.equals(","))       {}              //ignore
            else if(token.equals(";"))  lineto = false; //break chain
            else                                        //draw line:
            {
                atom = res.getAtom(token);
                if(atom == null)        lineto = false; //  break chain
                else
                {
                    try {
                        plotAtom(state.get(atom), lineto, drawList);    //  plot atom
                        lineto = true;
                    } catch(AtomException ex) {
                        SoftLog.err.println(ex.getMessage());
                        lineto = false;
                    }
                }
            }
        }// while(more tokens)
    }
//}}}

//{{{ plotAtom
//##################################################################################################
    void plotAtom(AtomState atomState, boolean lineto, KList list)
    {
        Residue r = atomState.getResidue();
        String name = (
            atomState.getName()
            +atomState.getAltConf()
            +r.getName()
            +" "+r.getChain()+" "
            +r.getSequenceNumber()
            +r.getInsertionCode()
        ).toLowerCase()
        +(atomState.getOccupancy() < 1 ? " "+df2.format(atomState.getOccupancy()) : "")
        +(atomState.getTempFactor() > 1 ? " B"+df2.format(atomState.getTempFactor()) : "");
        
        if(!lineto) prev = null;
        VectorPoint p = new VectorPoint(list, name, prev);
        
        p.setOrigX(atomState.getX());
        p.setOrigY(atomState.getY());
        p.setOrigZ(atomState.getZ());
        
        list.add(p);
        prev = p;
    }
//}}}

//{{{ createLists
//##################################################################################################
    /**
    * Creates the lists used by this plotter.
    * Lists are created only once -- if called again,
    * it will reconfigure them, but it will not create
    * new instances
    */
    public void createLists()
    {
        if(listCa == null)
        {
            listCa = new KList();
            listCa.setName("Calphas");
            listCa.setType(KList.VECTOR);
            listCa.addMaster("Calphas");    // matches Prekin 6.25
            listCa.setOn(false);
        }
        if(listMc == null)
        {
            listMc = new KList();
            listMc.setName("mc");
            listMc.setType(KList.VECTOR);
            listMc.addMaster("mainchain");  // matches Prekin 6.25
        }
        if(listMcH == null)
        {
            listMcH = new KList();
            listMcH.setName("mcH");
            listMcH.setType(KList.VECTOR);
            listMcH.addMaster("mainchain"); // matches Prekin 6.25
            listMcH.addMaster("H's");       // matches Prekin 6.25
        }
        if(listSc == null)
        {
            listSc = new KList();
            listSc.setName("sc");
            listSc.setType(KList.VECTOR);
            listSc.addMaster("sidechain");  // matches Prekin 6.25
        }
        if(listScH == null)
        {
            listScH = new KList();
            listScH.setName("scH");
            listScH.setType(KList.VECTOR);
            listScH.addMaster("sidechain"); // matches Prekin 6.25
            listScH.addMaster("H's");       // matches Prekin 6.25
        }
        
        listCa.setWidth(modelWidth);
        listCa.setColor(mainColor);
        listMc.setWidth(modelWidth);
        listMc.setColor(mainColor);
        listMcH.setWidth(modelWidth);
        listMcH.setColor(hyColor);
        listSc.setWidth(modelWidth);
        listSc.setColor(sideColor);
        listScH.setWidth(modelWidth);
        listScH.setColor(hyColor);
    }
//}}}

//{{{ createGroup, createSubgroup
//##################################################################################################
    /**
    * Creates a (dominant) group that owns this plotter's lists.
    * Unlike createLists, a new object is created every time.
    */
    public KGroup createGroup(String name)
    {
        if(listCa == null || listMc == null || listMcH == null
        || listSc == null || listScH == null) createLists();
        
        KGroup group = new KGroup();
        group.setName(name);
        group.setDominant(true);
        
        KSubgroup subMc = new KSubgroup(group, "mainchain");
        group.add(subMc);
        KSubgroup subSc = new KSubgroup(group, "sidechain");
        group.add(subSc);
        
        subMc.add(listMc);
        listMc.setOwner(subMc);
        subMc.add(listMcH);
        listMcH.setOwner(subMc);
        subMc.add(listCa);
        listCa.setOwner(subMc);
        
        subSc.add(listSc);
        listSc.setOwner(subSc);
        subSc.add(listScH);
        listScH.setOwner(subSc);
        
        return group;
    }

    /**
    * Creates a (dominant) subgroup that owns this plotter's lists.
    * Unlike createLists, a new object is created every time.
    */
    public KSubgroup createSubgroup(String name)
    {
        if(listMc == null || listMcH == null
        || listSc == null || listScH == null) createLists();
        
        KSubgroup subgroup = new KSubgroup();
        subgroup.setName(name);
        subgroup.setDominant(true);
        
        subgroup.add(listMc);
        listMc.setOwner(subgroup);
        subgroup.add(listMcH);
        listMcH.setOwner(subgroup);
        subgroup.add(listCa);
        listCa.setOwner(subgroup);
        
        subgroup.add(listSc);
        listSc.setOwner(subgroup);
        subgroup.add(listScH);
        listScH.setOwner(subgroup);
        
        return subgroup;
    }
//}}}

//{{{ clearLists, setHOn
//##################################################################################################
    /** Wipes the contents of the lists */
    public void clearLists()
    {
        if(listCa != null)  listCa.clear();
        if(listMc != null)  listMc.clear();
        if(listMcH != null) listMcH.clear();
        if(listSc != null)  listSc.clear();
        if(listScH != null) listScH.clear();
    }
    
    /** Turns the hydrogens on or off */
    public void setHOn(boolean on)
    {
        if(listMcH != null) listMcH.setOn(on);
        if(listScH != null) listScH.setOn(on);
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

