// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.rotarama;

import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;

import java.util.List;

//}}}
/**
* <code>RamaKinWriter</code> produces kinemages using the six Top8000-derived 
* Richardson Ramachandran categories.
* 
* <p>It can produce either 
*
* <p>This class is essentially a modified version of code in Ian's old 
* hless.Ramachandran (on his thesis DVD).
*
* <p>When run as an application, it just copies its template file to standard out.
*
* <p>Copyright (C) 2012 by Daniel A. Keedy. All rights reserved.
* <br>Begun on Tue Mar 13 2012
*/
public class RamaKinWriter //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.00");
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RamaKinWriter()
    {
        super();
    }
//}}}

//{{{ createRamaPlotKin
//##############################################################################
    /**
    * Takes a bunch of analyzed models and plots Rama plots for all of them.
    * If there is more than one model, superimposes all of their Rama plots
    * (doing each model individually (as with PDF output) could lead to too big 
    * of a kinemage).
    * @param analyses a Map&lt; Collection&lt;Ramalyze.RamaEval&gt;, String &gt;
    *   that maps each collection of analyzed residues to a label.
    *   All of them together are rendered as "All models"
    * @param structName a label identifying this structure, or null for none.
    * @param out a destination for the kinemage.
    */
    public void createRamaPlotKin(Map analyses, String structName, PrintWriter out) throws IOException
    {
        out.println("@text");
        out.println("Use the animate buttons or the 'a' key to cycle through the various Ramachandran plots.");
        out.println();
        textSummary(analyses, out);
        
        // Roughly the same as in RamaPdfWriter
        HashMap colormap = new HashMap();
        colormap.put(Ramalyze.RamaEval.GENERAL,  "purple");
        colormap.put(Ramalyze.RamaEval.ILEVAL,   "red");
        colormap.put(Ramalyze.RamaEval.PREPRO,   "blue");
        colormap.put(Ramalyze.RamaEval.GLYCINE,  "green");
        colormap.put(Ramalyze.RamaEval.TRANSPRO, "orange");
        colormap.put(Ramalyze.RamaEval.CISPRO,   "gold");
        
        try
        {
            BufferedReader template = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("rama8000/rama6-template.kin")));
            
            String line;
            
            // All data, no contours
            while((line = template.readLine()) != null)
            {
                if(line.startsWith("@dotlist")) break;
                else out.println(line);
            }
            writeAllData(analyses, colormap, out);
            
            // General
            while((line = template.readLine()) != null)
            {
                if(line.startsWith("@dotlist")) break;
                else out.println(line);
            }
            writeClass(analyses, Ramalyze.RamaEval.GENERAL, 
                colormap.get(Ramalyze.RamaEval.GENERAL).toString(), out);
            
            // Isoleucine/valine
            while((line = template.readLine()) != null)
            {
                if(line.startsWith("@dotlist")) break;
                else out.println(line);
            }
            writeClass(analyses, Ramalyze.RamaEval.ILEVAL, 
                colormap.get(Ramalyze.RamaEval.ILEVAL).toString(), out);
            
            // Pre-proline
            while((line = template.readLine()) != null)
            {
                if(line.startsWith("@dotlist")) break;
                else out.println(line);
            }
            writeClass(analyses, Ramalyze.RamaEval.PREPRO, 
                colormap.get(Ramalyze.RamaEval.PREPRO).toString(), out);
            
            // Glycine
            while((line = template.readLine()) != null)
            {
                if(line.startsWith("@dotlist")) break;
                else out.println(line);
            }
            writeClass(analyses, Ramalyze.RamaEval.GLYCINE, 
                colormap.get(Ramalyze.RamaEval.GLYCINE).toString(), out);
            
            // Trans proline
            while((line = template.readLine()) != null)
            {
                if(line.startsWith("@dotlist")) break;
                else out.println(line);
            }
            writeClass(analyses, Ramalyze.RamaEval.TRANSPRO, 
                colormap.get(Ramalyze.RamaEval.TRANSPRO).toString(), out);
            
            // Cis proline
            while((line = template.readLine()) != null)
            {
                if(line.startsWith("@dotlist")) break;
                else out.println(line);
            }
            writeClass(analyses, Ramalyze.RamaEval.CISPRO, 
                colormap.get(Ramalyze.RamaEval.CISPRO).toString(), out);
            
            template.close();
            out.flush(); // don't use close() in case it would close System.out?
        }
        catch(IOException ex)
        { System.err.println("Error reading rama6-template.kin resource file!"); }
    }
//}}}

//{{{ textSummary
//##################################################################################################
    /**
    * Prints textual statistics about the results of a Ramachandran analysis
    * for any collection of Ramalyze.RamaEvals.
    * Intended to be used in the kinemage @text area.
    * @param analyses   a Map&lt; Collection&lt;Ramalyze.RamaEval&gt;, String &gt;
    *   that maps each collection of analyzed residues to a label;
    *   All of them together are rendered as "All models"
    * @param out        the destination for kinemage output
    */
    public void textSummary(Map analyses, PrintWriter out)
    {
        int total = 0, favored = 0, allowed = 0, outlier = 0, model = 1;
        
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); model++)
        {
            Collection analysis = (Collection) iter.next();
            
            // Tally stats
            for(Iterator iter2 = analysis.iterator(); iter2.hasNext(); )
            {
                Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter2.next();
                if(eval.score == Ramalyze.RamaEval.FAVORED) { total++; favored++; }
                else if(eval.score == Ramalyze.RamaEval.ALLOWED) { total++; allowed++; }
                else if(eval.score == Ramalyze.RamaEval.OUTLIER) { total++; outlier++; }
            }
            
            // Report stats
            out.println("Model "+model+":");
            out.println(df.format((100.0*favored)/total)+"% ("+favored+"/"+total+") of all residues were in favored (98%) regions.");
            out.println(df.format((100.0*(favored+allowed))/total)+"% ("+(favored+allowed)+"/"+total+") of all residues were in allowed (>99.8%) regions.");
            if(outlier == 0) out.println("There were no outliers.");
            else
            {
                out.println("There were "+outlier+" outliers (phi, psi):");
                for(Iterator iter2 = analysis.iterator(); iter2.hasNext(); )
                {
                    Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter2.next();
                    if(eval.score == Ramalyze.RamaEval.OUTLIER)
                        out.println("    "+eval.name+" ("+df.format(eval.phi)+", "+df.format(eval.psi)+")");
                }
            }
            out.println();
        }
        
        out.println("For more information, see Lovell, et al. (2003) Proteins: Struct Func Gen 50:437-450");
    }
//}}}

//{{{ writeAllData
//##################################################################################################
    /**
    * Writes data for all Ramachandran classes.
    * Outliers are colored according to the pairings in colormap.
    * @param analyses   a Map&lt; Collection&lt;Ramalyze.RamaEval&gt;, String &gt;
    *   that maps each collection of analyzed residues to a label;
    *   All of them together are rendered as "All models"
    * @param colormap   maps ramaclass Strings to Mage color Strings
    * @param out        the destination for kinemage output
    */
    void writeAllData(Map analyses, Map colormap, PrintWriter out)
    {
        int model = 1;
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); model++)
        {
            Collection analysis = (Collection) iter.next();
            out.println("@balllist {Good data} color= white radius= 1.5 master= {Data pts} nohilite"
                +(analyses.size() > 1 ? " master= {Model "+model+"}" : "")
                +(analyses.size() > 1 && model != 1 ? " off" : ""));
            for(Iterator iter2 = analysis.iterator(); iter2.hasNext(); )
            {
                Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter2.next();
                if(eval.score == Ramalyze.RamaEval.FAVORED || eval.score == Ramalyze.RamaEval.ALLOWED)
                { out.println("{"+eval.name+"} "+df.format(eval.phi)+" "+df.format(eval.psi)+" 0.0"); }
            }
        }
        
        model = 1;
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); model++)
        {
            Collection analysis = (Collection) iter.next();
            out.println("@balllist {Bad data} color= white radius= 3.0 master= {Data pts} nohilite"
                +(analyses.size() > 1 ? " master= {Model "+model+"}" : "")
                +(analyses.size() > 1 && model != 1 ? " off" : ""));
            StringBuffer outlierLabels = new StringBuffer("@labellist {Outlier labels} color= white master= {Outlier Lbls}"
                +(analyses.size() > 1 ? " master= {Model "+model+"}" : "")
                +(analyses.size() > 1 && model != 1 ? " off" : ""));
            for(Iterator iter2 = analysis.iterator(); iter2.hasNext(); )
            {
                Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter2.next();
                if(eval.score == Ramalyze.RamaEval.OUTLIER)
                {
                    String color = colormap.get(eval.type).toString();
                    out.println("{"+eval.name+"} "+color+" "+df.format(eval.phi)+" "+df.format(eval.psi)+" 0.0");
                    outlierLabels.append("{"+eval.name+"} "+df.format(eval.phi+3.0)+" "+df.format(eval.psi)+" 0.0\n");
                }
            }
            out.println(outlierLabels.toString());
        }
    }
//}}}

//{{{ writeClass
//##################################################################################################
    /**
    * Writes data for one Ramachandran class.
    * Outliers are colored according to the pairings in colormap.
    * @param analyses   a Map&lt; Collection&lt;Ramalyze.RamaEval&gt;, String &gt;
    *   that maps each collection of analyzed residues to a label;
    *   All of them together are rendered as "All models"
    * @param evaltype   a constant from RamaEval
    * @param color      the color for the outlier markers (a Mage color)
    * @param out        the destination for kinemage output
    */
    void writeClass(Map analyses, String evaltype, String color, PrintWriter out)
    {
        int model = 1;
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); model++)
        {
            Collection analysis = (Collection) iter.next();
            out.println("@balllist {Good data} color= white radius= 1.5 master= {Data pts} nohilite"
                +(analyses.size() > 1 ? " master= {Model "+model+"}" : "")
                +(analyses.size() > 1 && model != 1 ? " off" : ""));
            for(Iterator iter2 = analysis.iterator(); iter2.hasNext(); )
            {
                Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter2.next();
                if(eval.type == evaltype
                && (eval.score == Ramalyze.RamaEval.FAVORED || eval.score == Ramalyze.RamaEval.ALLOWED))
                { out.println("{"+eval.name+"} "+df.format(eval.phi)+" "+df.format(eval.psi)+" 0.0"); }
            }
        }
        
        model = 1;
        for(Iterator iter = analyses.keySet().iterator(); iter.hasNext(); model++)
        {
            Collection analysis = (Collection) iter.next();
            out.println("@balllist {Bad data} color= white radius= 3.0 master= {Data pts} nohilite"
                +(analyses.size() > 1 ? " master= {Model "+model+"}" : "")
                +(analyses.size() > 1 && model != 1 ? " off" : ""));
            StringBuffer outlierLabels = new StringBuffer("@labellist {Outlier labels} color= white master= {Outlier Lbls}"
                +(analyses.size() > 1 ? " master= {Model "+model+"}" : "")
                +(analyses.size() > 1 && model != 1 ? " off" : ""));
            for(Iterator iter2 = analysis.iterator(); iter2.hasNext(); )
            {
                Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter2.next();
                if(eval.type == evaltype && eval.score == Ramalyze.RamaEval.OUTLIER)
                {
                    out.println("{"+eval.name+"} "+color+" "+df.format(eval.phi)+" "+df.format(eval.psi)+" 0.0");
                    outlierLabels.append("{"+eval.name+"} "+df.format(eval.phi+3.0)+" "+df.format(eval.psi)+" 0.0\n");
                }
            }
            out.println(outlierLabels.toString());
        }
    }
//}}}

//{{{ drawRamaOutlierCaTraces
//##################################################################################################
    /**
    * Takes ONE analyzed model and draws Rama outlier markups for it.
    * If there was more than one model, this method typically gets passed just the first one.
    * @param analyses a Collection&lt;Ramalyze.RamaEval&gt; of analyzed residues.
    * @param structName a label identifying this structure, or null for none.
    * @param out a destination for the kinemage.
    */
    public void drawRamaOutlierCaTraces(Collection analysis, Model model, ModelState state, PrintWriter out)
    {
        DecimalFormat df3 = new DecimalFormat("0.###");
        
        out.println("@vectorlist {bad Rama Ca} width= 4 color= green");
        
        for(Iterator iter2 = analysis.iterator(); iter2.hasNext(); )
        {
            Ramalyze.RamaEval eval = (Ramalyze.RamaEval) iter2.next();
            if(eval.score == Ramalyze.RamaEval.OUTLIER)
            {
                Residue res = eval.res;
                
                // Try really hard to avoid nulls
                Atom prevAtom = res.getPrev(model).getAtom(" CA ");
                Atom currAtom = res.getAtom(" CA ");
                Atom nextAtom = res.getNext(model).getAtom(" CA ");
                if(prevAtom == null) prevAtom = res.getAtom(" N  ");
                if(nextAtom == null) nextAtom = res.getAtom(" C  ");
                if(prevAtom == null || currAtom == null || nextAtom == null)
                { System.err.println("Unable to draw Ca-Ca for "+res); continue; }
                
                try
                {
                    AtomState prev = state.get(prevAtom);
                    AtomState curr = state.get(currAtom);
                    AtomState next = state.get(nextAtom);
                    
                    Triple t = new Triple();
                    t.likeMidpoint(prev, curr);
                    out.println("{"+res.getPrev(model)+" CA}P "
                        +df3.format(t.getX())+" "+df3.format(t.getY())+" "+df3.format(t.getZ()));
                    t = curr;
                    out.println("{"+res+" CA} "
                        +df3.format(t.getX())+" "+df3.format(t.getY())+" "+df3.format(t.getZ()));
                    t.likeMidpoint(next, curr);
                    out.println("{"+res.getNext(model)+" CA} "
                        +df3.format(t.getX())+" "+df3.format(t.getY())+" "+df3.format(t.getZ()));
                }
                catch(AtomException ex)
                { System.err.println("Unable to draw Ca-Ca for "+res); }
            }
        }
        
        out.flush(); // don't use close() in case it would close System.out?
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        try
        {
            // Otherwise, this will fail in a server environment!
            System.setProperty("java.awt.headless", "true");
            
            // Write out our template file to standard out
            BufferedReader template = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("rama8000/rama6-template.kin")));
            String line;
            while((line = template.readLine()) != null)
                System.out.println(line);
            template.close();
            System.out.flush();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }

    public static void main(String[] args)
    {
        RamaKinWriter mainprog = new RamaKinWriter();
        mainprog.Main();
    }
//}}}
}//class

