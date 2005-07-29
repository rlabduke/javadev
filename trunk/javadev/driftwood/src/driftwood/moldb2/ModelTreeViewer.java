// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.moldb2;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>ModelTreeViewer</code> is a fun GUI viewer for exploring
* the contents of a PDB file.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jun 13 15:31:13 EDT 2003
*/
public class ModelTreeViewer //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ModelTreeViewer()
    {
        super();
    }
//}}}

//{{{ buildRootNode
//##################################################################################################
    DefaultMutableTreeNode buildRootNode(CoordinateFile coordFile)
    {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("All models", true);
        for(Iterator iter = coordFile.getModels().iterator(); iter.hasNext(); )
        {
            Model model = (Model)iter.next();
            DefaultMutableTreeNode modelNode = buildModelNode(model);
            rootNode.add(modelNode);
        }
        
        return rootNode;
    }
//}}}

//{{{ buildModelNode
//##################################################################################################
    DefaultMutableTreeNode buildModelNode(Model model)
    {
        DefaultMutableTreeNode modelNode = new DefaultMutableTreeNode("Model "+model, true);
        
        // All chains
        DefaultMutableTreeNode chainsNode = new DefaultMutableTreeNode("Chains", true);
        modelNode.add(chainsNode);
        for(Iterator chainIter = model.getChainIDs().iterator(); chainIter.hasNext(); )
        {
            String ch = (String)chainIter.next();
            DefaultMutableTreeNode chainNode = new DefaultMutableTreeNode("Chain '"+ch+"'", true);
            chainsNode.add(chainNode);
            for(Iterator iter = model.getChain(ch).iterator(); iter.hasNext(); )
            {
                Residue res = (Residue)iter.next();
                DefaultMutableTreeNode resNode = buildResidueNode(res);
                chainNode.add(resNode);
            }
        }
        
        // All segments
        DefaultMutableTreeNode segmentsNode = new DefaultMutableTreeNode("Segments", true);
        modelNode.add(segmentsNode);
        for(Iterator segmentIter = model.getSegmentIDs().iterator(); segmentIter.hasNext(); )
        {
            String seg = (String)segmentIter.next();
            DefaultMutableTreeNode segmentNode = new DefaultMutableTreeNode("Segment '"+seg+"'", true);
            segmentsNode.add(segmentNode);
            for(Iterator iter = model.getSegment(seg).iterator(); iter.hasNext(); )
            {
                Residue res = (Residue)iter.next();
                DefaultMutableTreeNode resNode = buildResidueNode(res);
                segmentNode.add(resNode);
            }
        }
        
        // All residues
        DefaultMutableTreeNode allResNode = new DefaultMutableTreeNode("All residues", true);
        modelNode.add(allResNode);
        for(Iterator iter = model.getResidues().iterator(); iter.hasNext(); )
        {
            Residue res = (Residue)iter.next();
            DefaultMutableTreeNode resNode = buildResidueNode(res);
            allResNode.add(resNode);
        }
        
        return modelNode;
    }
//}}}

//{{{ buildResidueNode
//##################################################################################################
    DefaultMutableTreeNode buildResidueNode(Residue res)
    {
        DefaultMutableTreeNode resNode = new DefaultMutableTreeNode(res, true);
        
        for(Iterator iter = res.getAtoms().iterator(); iter.hasNext(); )
        {
            Atom atom = (Atom)iter.next();
            DefaultMutableTreeNode atomNode = new DefaultMutableTreeNode(atom.getName(), false);
            resNode.add(atomNode);
        }
        
        return resNode;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ loadCoordinateFile, launchTreeWindow
//##################################################################################################
    CoordinateFile loadCoordinateFile() throws IOException
    {
        PdbReader reader = new PdbReader();
        CoordinateFile cf = reader.read(System.in);
        return cf;
    }
    
    void launchTreeWindow(TreeNode rootNode)
    {
        JTree tree = new JTree(rootNode, true);
        //tree.setRootVisible(false);
        JScrollPane scroll = new JScrollPane(tree);
        JPanel cp = new JPanel(new BorderLayout());
        cp.add(scroll);
        
        JFrame frame = new JFrame("Model Tree Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(cp);
        
        frame.setSize(500, 800);
        frame.setVisible(true);
    }
//}}}

//{{{ Main, main
//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        try
        {
            CoordinateFile cf = loadCoordinateFile();
            TreeNode rootNode = buildRootNode(cf);
            launchTreeWindow(rootNode);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            SoftLog.err.println("*** Reading PDB file failed on I/O ***");
        }
    }

    public static void main(String[] args)
    {
        ModelTreeViewer mainprog = new ModelTreeViewer();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            SoftLog.err.println();
            mainprog.showHelp(true);
            SoftLog.err.println();
            SoftLog.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}

//{{{ parseArguments, showHelp
//##################################################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        String  arg, flag, param;
        boolean interpFlags = true;
        
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") || !interpFlags || arg.equals("-"))
            {
                // This is probably a filename or something
                interpretArg(arg);
            }
            else if(arg.equals("--"))
            {
                // Stop treating things as flags once we find --
                interpFlags = false;
            }
            else
            {
                // This is a flag. It may have a param after the = sign
                int eq = arg.indexOf('=');
                if(eq != -1)
                {
                    flag    = arg.substring(0, eq);
                    param   = arg.substring(eq+1);
                }
                else
                {
                    flag    = arg;
                    param   = null;
                }
                
                try { interpretFlag(flag, param); }
                catch(NullPointerException ex)
                { throw new IllegalArgumentException("'"+arg+"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("ModelTreeViewer.help");
            if(is == null) SoftLog.err.println("\n*** Unable to locate help information in 'ModelTreeViewer.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        SoftLog.err.println("driftwood.moldb2.ModelTreeViewer");
        SoftLog.err.println("Copyright (C) 2003 by Ian W. Davis. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ interpretArg, interpretFlag
//##################################################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

