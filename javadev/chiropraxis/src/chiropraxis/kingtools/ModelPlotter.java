// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
import king.*;
import king.core.*;
import king.points.*;
import king.io.*;

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
import driftwood.data.*;
import molikin.logic.*;
import molikin.*;
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
        
        StreamTank kinData = new StreamTank();
        PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
        
        BallAndStickLogic bsl = Quickin.getLotsLogic(false);
        
        out.println("@kinemage 1");
        bsl.printKinemage(out, model, Collections.singletonList(state), new UberSet(residues), "", mainColor.toString());
        
        out.flush();
        kinData.close();
        KinfileParser parser = new KinfileParser();
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(kinData.getInputStream()));
        try {
          parser.parse(lnr);
          ArrayList<Kinemage> kins = new ArrayList<Kinemage>(parser.getKinemages());
          for (Kinemage kin : kins) {
            KIterator<KList> lists = KIterator.allLists(kin);
            for (KList l : lists) {
              KIterator<KPoint> points = KIterator.allPoints(l);
              if (l.getName().endsWith("mc")) {
                moveAllPoints(listMc, points);
              } else if (l.getName().endsWith("mcH")) {
                moveAllPoints(listMcH, points);
              } else if (l.getName().endsWith("sc")) {
                moveAllPoints(listSc, points);
              } else if (l.getName().endsWith("scH")) {
                moveAllPoints(listScH, points);
              }
            }
          }
          lnr.close();
        } catch (IOException ie) {
          //I don't think this should ever happen...
          System.out.println("IOException in streamtank in ModelPlotter?");
        }
        parser = null;
        kinData = null;
        // For each residue, draw mainchain, then draw sidechain.
      //  for(Iterator iter = residues.iterator(); iter.hasNext(); )
      //  {
      //      Residue r       = (Residue)iter.next();
      //      Residue next    = r.getNext(model); // next along the chain
      //      
      //      // Heavy atom backbone trace
      //      plotString(r, state, scProps.getString("aminoacid.mc", ""), listMc);
      //      // Hydrogen backbone trace
      //      plotString(r, state, scProps.getString("aminoacid.hy", ""), listMcH);
      //
      //      // Heavy atom sidechain trace
      //      String tlc = r.getName().toLowerCase();
      //      plotString(r, state, scProps.getString(tlc+".sc", ""), listSc);
      //      // Hydrogen sidechain trace
      //      plotString(r, state, scProps.getString(tlc+".hy", ""), listScH);
      //
      //      // Connect the backbone between residues as necessary
      //      if(next != null && residues.contains(next))
      //      {
      //          Atom C, N;
      //          C = r.getAtom(" C  ");
      //          N = next.getAtom(" N  ");
      //          if(C != null && N != null)
      //          {
      //              try {
      //                  plotAtom(state.get(C),  false,  listMc);
      //                  plotAtom(state.get(N),  true,   listMc);
      //              } catch(AtomException ex) { SoftLog.err.println(ex.getMessage()); }
      //          }
      //          
      //          Atom Ca1, Ca2;
      //          Ca1 = r.getAtom(" CA ");
      //          Ca2 = next.getAtom(" CA ");
      //          if(Ca1 != null && Ca2 != null)
      //          {
      //              try {
      //                  plotAtom(state.get(Ca1),  false,  listCa);
      //                  plotAtom(state.get(Ca2),  true,   listCa);
      //              } catch(AtomException ex) { SoftLog.err.println(ex.getMessage()); }
      //          }
      //      }
      //  }//for(each residue)
      //  
      //  prev = null; //avoid memory leaks
    }
//}}}

  //{{{ buildKinObject
  /** for building a Kinemage object from a model and collection of residues **/
  public static Kinemage buildKinObject(Model model, Collection residues, ModelState state) {
    StreamTank kinData = new StreamTank();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(kinData));
    
    BallAndStickLogic bsl = Quickin.getLotsLogic(false);
    
    out.println("@kinemage 1");
    bsl.printKinemage(out, model, Collections.singletonList(state), new UberSet(residues), "", "grey");
    
    out.flush();
    kinData.close();
    KinfileParser parser = new KinfileParser();
    LineNumberReader lnr = new LineNumberReader(new InputStreamReader(kinData.getInputStream()));
    Kinemage outKin = null;
    try {
      parser.parse(lnr);
      ArrayList<Kinemage> kins = new ArrayList<Kinemage>(parser.getKinemages());
      outKin = kins.get(0);
      lnr.close();
    } catch (IOException ie) {
      //I don't think this should ever happen...
      System.out.println("IOException in streamtank in Quickin?");
      ie.printStackTrace();
    }

    parser = null;
    kinData = null;
    return outKin;
  }
  //}}}

  //{{{ moveAllPoints
  public void moveAllPoints(KList listTo, KIterator<KPoint> points) {
    for (KPoint p : points) {
      listTo.add(p);
    }
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
        VectorPoint p = new VectorPoint(name, prev);
        
        p.setX(atomState.getX());
        p.setY(atomState.getY());
        p.setZ(atomState.getZ());
        
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
        // Added "refit" prefix to master names b/c otherwise these lists
        // may be switched off unexpectedly when inserted into a kinemage
        // where the existing masters are already off.
        if(listCa == null)
        {
            listCa = new KList(KList.VECTOR);
            listCa.setName("Calphas");
            listCa.addMaster("refit Calphas");    // matches Prekin 6.25
            listCa.setOn(false);
        }
        if(listMc == null)
        {
            listMc = new KList(KList.VECTOR);
            listMc.setName("mc");
            listMc.addMaster("refit mainchain");  // matches Prekin 6.25
        }
        if(listMcH == null)
        {
            listMcH = new KList(KList.VECTOR);
            listMcH.setName("mcH");
            listMcH.addMaster("refit mainchain"); // matches Prekin 6.25
            listMcH.addMaster("refit H");       // matches Prekin 6.25
        }
        if(listSc == null)
        {
            listSc = new KList(KList.VECTOR);
            listSc.setName("sc");
            listSc.addMaster("refit sidechains");  // matches Prekin 6.25
        }
        if(listScH == null)
        {
            listScH = new KList(KList.VECTOR);
            listScH.setName("scH");
            listScH.addMaster("refit sidechains"); // matches Prekin 6.25
            listScH.addMaster("refit H");       // matches Prekin 6.25
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
        
        KGroup subMc = new KGroup("mainchain");
        group.add(subMc);
        KGroup subSc = new KGroup("sidechain");
        group.add(subSc);
        
        subMc.add(listMc);
        subMc.add(listMcH);
        subMc.add(listCa);
        
        subSc.add(listSc);
        subSc.add(listScH);
        
        return group;
    }

    /**
    * Creates a (dominant) subgroup that owns this plotter's lists.
    * Unlike createLists, a new object is created every time.
    */
    public KGroup createSubgroup(String name)
    {
        if(listMc == null || listMcH == null
        || listSc == null || listScH == null) createLists();
        
        KGroup subgroup = new KGroup();
        subgroup.setName(name);
        subgroup.setDominant(true);
        
        subgroup.add(listMc);
        subgroup.add(listMcH);
        subgroup.add(listCa);
        
        subgroup.add(listSc);
        subgroup.add(listScH);
        
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

