// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package chiropraxis.kingtools;
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
        if(idealizer == null)
        {
            JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "Library of ideal amino acids could not be loaded.",
                "Missing data resource",
                JOptionPane.ERROR_MESSAGE);
            parent.activateDefaultTool();
            return;
        }
            
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
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
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
        try
        {
            String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(),
                "Mutate this sidechain to what?",
                "Choose mutation",
                JOptionPane.QUESTION_MESSAGE, null,
                resChoices, orig.getName());
            
            if(choice == null) return; // user canceled operation
            
            // Create the mutated sidechain
            ModelState newState = new ModelState(origState);
            Residue newRes = idealizer.makeIdealResidue(orig.getChain(),
                orig.getSegment(),
                orig.getSequenceNumber(),
                orig.getInsertionCode(),
                choice, newState);
            
            // Align it on the old backbone
            newState = idealizer.dockResidue(newRes, newState, orig, origState);
            
            // Deal with His protonation states
            if(choice.equals("HIS"))
            {
                choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(),
                    "Choose protonation state:",
                    "Choose protonation state",
                    JOptionPane.QUESTION_MESSAGE, null,
                    new String[] {"ND1 and NE2", "ND1 only", "NE2 only"}, "ND1 and NE2");
                if("ND1 only".equals(choice))
                    newRes.remove(newRes.getAtom(" HE2"));
                else if("NE2 only".equals(choice))
                    newRes.remove(newRes.getAtom(" HD1"));
            }
            
            // Create the mutated model
            Model newModel = (Model) model.clone();
            newModel.replace(orig, newRes);
            
            // Remove any unnecessary AtomStates from the model
            newState = newState.createForModel(newModel);
            
            // Insert the mutated model into the model manager
            modelman.replaceModelAndState(newModel, newState);
            
            // Make a note in the headers
            modelman.srcmodgrp.addHeader(ModelGroup.SECTION_USER_MOD, "USER  MOD Mutated "+orig+" to "+newRes);
        }
        catch(ResidueException ex)
        {
            ex.printStackTrace(SoftLog.err);
        }
        catch(AtomException ex)
        {
            ex.printStackTrace(SoftLog.err);
        }
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
    { return "#scmut-tool"; }
    
    public String toString() { return "Sidechain mutator"; }
//}}}
}//class

