// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.moldb2.*;
import driftwood.r3.*;
import driftwood.util.*;
import chiropraxis.sc.*;
//}}}
/**
* <code>ScMutTool</code> allows users to mutate
* sidechains in a protein model.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 12 09:58:34 EST 2004
*/
public class ScMutTool  extends ModelingTool
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    SidechainIdealizer  idealizer   = null;
    String[]            resChoices  = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ScMutTool(ToolBox tb)
    {
        super(tb);
        
        try
        {
            idealizer = new SidechainIdealizer();
            Collection c = idealizer.getResidueTypes();
            resChoices = (String[]) c.toArray(new String[c.size()]);
        }
        catch(IOException ex)
        { ex.printStackTrace(SoftLog.err); }
    }
//}}}

//{{{ start/stop/reset
//##################################################################################################
    public void start()
    {
        super.start();

        // Bring up model manager
        modelman.onShowDialog(null);
    }
//}}}

//{{{ c_click
//##############################################################################
    /** Override this function for middle-button/control clicks */
    public void c_click(int x, int y, KPoint p, MouseEvent ev)
    {
        if(p != null)
        {
            if(modelman.isMolten())
            {
                JOptionPane.showMessageDialog(kMain.getMainWindow(),
                    "Residues cannot be mutated while the model is being resculpted.\n"
                    +"Please release all mobile groups and then try again.",
                    "Cannot mutate molten model",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Model       model   = modelman.getModel();
            ModelState  state   = modelman.getFrozenState();
            Residue     orig    = this.getResidueNearest(model, state,
                p.getOrigX(), p.getOrigY(), p.getOrigZ());
            
            if(orig != null)
                askMutateResidue(model, orig, state);
        }
    }
//}}}

//{{{ askMutateResidue
//##############################################################################
    void askMutateResidue(Model model, Residue orig, ModelState origState)
    {
        if(libraryModel == null)
        {
            JOptionPane.showMessageDialog(kMain.getMainWindow(),
                "Library of amino acids could not be loaded.",
                "Missing data resource",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String choice = (String) JOptionPane.showInputDialog(kMain.getMainWindow(),
            "Mutate this sidechain to what?",
            "Choose mutation",
            JOptionPane.QUESTION_MESSAGE, null,
            resChoices, orig.getName());
        
        if(choice == null) return; // user canceled operation
        
        // Create the mutated sidechain
        ModelState newState = new ModelState(origState);
        Residue newRes = mutateResidue(orig, origState, choice, newState);
        if(newRes == null) return;
        
        if(choice.equals("HIS"))
        {
            // deal with protonation states here
        }
        
        // Create the mutated model
        Model newModel = (Model) model.clone();
        newModel.remove(orig);
        newModel.add(newRes);
        newModel.restoreOrder();
        
        // Insert the mutated model into the model manager
        modelman.replaceModelAndState(newModel, newState);
    }
//}}}

//{{{ mutateResidue
//##############################################################################
    /**
    * Creates a new residue like the old one, but with a different sidechain.
    * Backbone atoms will retain their positions.
    * @param orig       the original residue
    * @param origState  conformation of orig
    * @param mutateTo   three-letter code of the desired amino acid type
    * @param newState   a place to deposit the new AtomStates
    * @return null on failure
    */
    Residue mutateResidue(Residue orig, ModelState origState, String mutateTo, ModelState newState)
    {
        // Find a template
        Residue templateRes = null;
        for(Iterator iter = libraryModel.getResidues().iterator(); iter.hasNext(); )
        {
            Residue r = (Residue)iter.next();
            if(mutateTo.equals(r.getName()))
                templateRes = r;
        }
        if(templateRes == null) return templateRes;
        
        // Copy it
        Residue newRes = new Residue(templateRes,
            orig.getChain(),
            orig.getSegment(),
            orig.getSequenceNumber(),
            orig.getInsertionCode(),
            templateRes.getName());
        newRes.cloneStates(templateRes, libraryState, newState);
        
        try
        {
            // Reposition all atoms
            Transform xform = builder.dock3on3(
                origState.get(orig.getAtom(" CA ")),
                origState.get(orig.getAtom(" N  ")),
                origState.get(orig.getAtom(" C  ")),
                newState.get(newRes.getAtom(" CA ")),
                newState.get(newRes.getAtom(" N  ")),
                newState.get(newRes.getAtom(" C  "))
            );
            for(Iterator iter = newRes.getAtoms().iterator(); iter.hasNext(); )
                xform.transform(newState.get((Atom)iter.next()));
            
            // Reposition backbone atoms
            newState.get(newRes.getAtom(" N  ")).like(origState.get(orig.getAtom(" N  ")));
            newState.get(newRes.getAtom(" CA ")).like(origState.get(orig.getAtom(" CA ")));
            newState.get(newRes.getAtom(" C  ")).like(origState.get(orig.getAtom(" C  ")));
            newState.get(newRes.getAtom(" O  ")).like(origState.get(orig.getAtom(" O  ")));
            try { newState.get(newRes.getAtom(" H  ")).like(origState.get(orig.getAtom(" H  "))); }
            catch(AtomException ex) {}
        }
        catch(AtomException ex) { return null; }
        
        // Idealize sidechain
        newState = idealizer.idealizeCB(newRes, newState);
        
        return newRes;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ getToolPanel, getHelpAnchor, toString
//##################################################################################################
    /** Returns a component with controls and options for this tool */
    protected Container getToolPanel()
    { return null; }
    
    /**
    * Returns an anchor marking a place within <code>king-manual.html</code>
    * that is the help for this tool. This is called by the default
    * implementation of <code>getHelpURL()</code>. 
    * If you override that function, you can safely ignore this one.
    * @return for example, "#navigate-tool" (or null)
    */
    public String getHelpAnchor()
    { return null; /*"#scmut-tool";*/ }
    
    public String toString() { return "Sidechain mutator"; }
//}}}
}//class

