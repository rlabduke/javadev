// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool;
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
import driftwood.moldb.*;
import driftwood.util.*;
//}}}
/**
* <code>BackrubPlotter</code> is capable of taking molecular
* structures from driftwood.moldb and rendering them as
* kinemage entities.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 16:12:43 EST 2003
*/
public class BackrubPlotter
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
    Props       scProps;
    VectorPoint prev    = null;

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
    public BackrubPlotter()
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
    * @param residuesToAminoAcids
    *   a Map&lt;Residue, AminoAcid&gt; of things to plot.
    *   Residues are used for naming and connectivity;
    *   AminoAcids are used for atom coordinates.
    */
    public void plotAminoAcids(Map residuesToAminoAcids)
    {
        if(listMc == null || listMcH == null
        || listSc == null || listScH == null) createLists();
        
        clearLists();
        
        // For each residue, draw mainchain, then draw sidechain.
        Residue     r;
        AminoAcid   aa, nextAA;
        for(Iterator iter = residuesToAminoAcids.keySet().iterator(); iter.hasNext(); )
        {
            r   = (Residue)iter.next();
            aa  = (AminoAcid)residuesToAminoAcids.get(r);
            
            // Heavy atom backbone trace
            plotAtom(aa.N,  false,  listMc);
            plotAtom(aa.CA, true,   listMc);
            plotAtom(aa.C,  true,   listMc);
            plotAtom(aa.O,  true,   listMc);
            if(residuesToAminoAcids.containsKey(r.getNext()))
            {
                nextAA = (AminoAcid)residuesToAminoAcids.get(r.getNext());
                plotAtom(aa.C,      false,  listMc);
                plotAtom(nextAA.N,  true,   listMc);
            }
            
            // Hydrogen backbone trace
            if(aa.H != null)
            {
                plotAtom(aa.N,  false,  listMcH);
                plotAtom(aa.H,  true,   listMcH);
            }
            if(aa.HA != null)
            {
                plotAtom(aa.CA, false,  listMcH);
                plotAtom(aa.HA, true,   listMcH);
            }
            if(aa.HA1 != null)
            {
                plotAtom(aa.CA, false,  listMcH);
                plotAtom(aa.HA1,true,   listMcH);
            }
            if(aa.HA2 != null)
            {
                plotAtom(aa.CA, false,  listMcH);
                plotAtom(aa.HA2,true,   listMcH);
            }
            
            // Sidechain trace
            plotSidechain(r.getType(), aa, listSc, listScH);
        }//for(each residue)
        
        prev = null; //avoid memory leaks
    }
//}}}

//{{{ plotSidechain
//##################################################################################################
    /**
    * Syntax for connectivity strings is "aaaa,bbbb,cccc;dddd,eeee"
    * where commas separate 4-character atom IDs and semi-colons
    * denote breaks in the chain (like P in kinemage format).
    */
    void plotSidechain(String threeLetterCode, AminoAcid aa, KList listSc, KList listScH)
    {
        threeLetterCode = threeLetterCode.toLowerCase();
        String          scConn, token;
        StringTokenizer tokenizer;
        boolean         lineto;
        Atom            atom;
        
        // Heavy atoms
        scConn      = scProps.getString(threeLetterCode+".sc", " CA , CB ");
        //SoftLog.err.println(threeLetterCode+".sc = "+scConn);
        tokenizer   = new StringTokenizer(scConn, ",;", true);
        lineto      = false;
        while(tokenizer.hasMoreTokens())
        {
            token = tokenizer.nextToken();
            if(token.equals(","))           {}              //ignore
            else if(token.equals(";"))      lineto = false; //break chain
            else                                            //draw line
            {
                if(token.equals(" CA "))        atom = aa.CA;
                else if(token.equals(" N  "))   atom = aa.N;
                else                            atom = (Atom)aa.sc.get(token);
                
                if(atom == null)
                {
                    SoftLog.err.println("Couldn't find atom '"+token+"'");
                    lineto = false;
                }
                else
                {
                    plotAtom(atom, lineto, listSc);
                    lineto = true;
                }
            }
        }

        // Hydrogens
        scConn      = scProps.getString(threeLetterCode+".hy", "");
        tokenizer   = new StringTokenizer(scConn, ",;", true);
        lineto      = false;
        while(tokenizer.hasMoreTokens())
        {
            token = tokenizer.nextToken();
            if(token.equals(","))       {}              //ignore
            else if(token.equals(";"))  lineto = false; //break chain
            else                                        //draw line:
            {
                atom = (Atom)aa.sc.get(token);
                if(atom == null)        lineto = false; //  break chain
                else
                {
                    plotAtom(atom, lineto, listScH);    //  plot atom
                    lineto = true;
                }
            }
        }
    }
//}}}

//{{{ plotAtom
//##################################################################################################
    void plotAtom(Atom atom, boolean lineto, KList list)
    {
        if(!lineto) prev = null;
        VectorPoint p = new VectorPoint(list, atom.getID(), prev);
        
        p.setOrigX(atom.getX());
        p.setOrigY(atom.getY());
        p.setOrigZ(atom.getZ());
        //p.setUnpickable(true);
        
        list.add(p);
        prev = p;
        
        //if(lineto)  SoftLog.err.println("lineto "+atom.getID()+" at "+atom+" in list "+list.getName());
        //else        SoftLog.err.println("moveto "+atom.getID()+" at "+atom+" in list "+list.getName());
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
        if(listMc == null)
        {
            listMc = new KList();
            listMc.setName("mc");
            listMc.setType(KList.VECTOR);
        }
        if(listMcH == null)
        {
            listMcH = new KList();
            listMcH.setName("mcH");
            listMcH.setType(KList.VECTOR);
        }
        if(listSc == null)
        {
            listSc = new KList();
            listSc.setName("sc");
            listSc.setType(KList.VECTOR);
        }
        if(listScH == null)
        {
            listScH = new KList();
            listScH.setName("scH");
            listScH.setType(KList.VECTOR);
        }
        
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
        if(listMc == null || listMcH == null
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
        
        subgroup.add(listSc);
        listSc.setOwner(subgroup);
        subgroup.add(listScH);
        listScH.setOwner(subgroup);
        
        return subgroup;
    }
//}}}

//{{{ clearLists
//##################################################################################################
    /** Wipes the contents of the lists */
    public void clearLists()
    {
        if(listMc != null)  listMc.clear();
        if(listMcH != null) listMcH.clear();
        if(listSc != null)  listSc.clear();
        if(listScH != null) listScH.clear();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

