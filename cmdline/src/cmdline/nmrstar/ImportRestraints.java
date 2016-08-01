// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline.nmrstar;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.star.*;
//}}}
/**
* <code>ImportRestraints</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Apr 18 11:05:07 EDT 2007
*/
public class ImportRestraints //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Collection inputFiles = new ArrayList();

    // first id to assign in database
    int     firstRestraintId    = 1;
    int     firstAtomId         = 1;
    
    // Maps identifier from the file (String) to id in database (String)
    Map     restraints          = new HashMap();
    Map     atoms               = new HashMap();
    
    String  pdbId               = null;
    String  distConstId         = null;
    String  subtype             = null;
    String  format              = null;
    
    PrintWriter outRestr        = null;
    PrintWriter outAtoms        = null;
    PrintWriter outEnds         = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ImportRestraints()
    {
        super();
    }
//}}}

//{{{ processFileOrDir, processFile
//##############################################################################
    void processFileOrDir(Collection files)
    {
        for(Iterator iter = files.iterator(); iter.hasNext(); )
        {
            File fileOrDir = (File) iter.next();
            if(fileOrDir.isFile())
                processFile(fileOrDir);
            else if(fileOrDir.isDirectory())
                processFileOrDir(Arrays.asList(fileOrDir.listFiles()));
            else
                System.err.println("Don't know how to process "+fileOrDir);
        }
    }
    
    void processFile(File file)
    {
        try
        {
            System.err.println("Processing "+file+" ...");
            LineNumberReader lnr = new LineNumberReader(new FileReader(file));
            StarFile starFile = new StarReader().parse(lnr);
            lnr.close();
            
            restraints.clear();
            atoms.clear();
            pdbId       = null;
            distConstId = null;
            subtype     = null;
            format      = null;
            
            for(Iterator iBlock = starFile.getDataBlockNames().iterator(); iBlock.hasNext(); )
            {
                String blockName = (String) iBlock.next();
                DataBlock block = starFile.getDataBlock(blockName);
                processDataCell(block);
                for(Iterator iSave = block.getSaveFrames().iterator(); iSave.hasNext(); )
                {
                    DataCell cell = (DataCell) iSave.next();
                    processDataCell(cell);
                }
            }
        }
        catch(Exception ex)
        { ex.printStackTrace(); }
    }
//}}}

//{{{ processDataCell
//##############################################################################
    /** Process a save_ frame from the file */
    void processDataCell(DataCell save)
    {
        try
        {
            List l;
            l = save.getItem("_File_characteristics.PDB_ID");
            if(l.size() > 0) this.pdbId = (String) l.get(0);
            l = save.getItem("_Distance_constraints.ID");
            if(l.size() > 0) this.distConstId = (String) l.get(0);
            l = save.getItem("_Distance_constraints.Subtype");
            if(l.size() > 0) this.subtype = (String) l.get(0);
            l = save.getItem("_Distance_constraints.Format");
            if(l.size() > 0) this.format = (String) l.get(0);
            
            // Get data that describes actual restraints
            List constId    = save.getItem("_Dist_constraint_value.Constraints_ID");
            List treeId     = save.getItem("_Dist_constraint_value.Constraint_ID");
            List nodeId     = save.getItem("_Dist_constraint_value.Tree_node_ID");
            List dist       = save.getItem("_Dist_constraint_value.Distance_val");
            List lowerB     = save.getItem("_Dist_constraint_value.Distance_lower_bound_val");
            List upperB     = save.getItem("_Dist_constraint_value.Distance_upper_bound_val");
            if(checkTableLength(new List[] {constId, treeId, nodeId, dist}) > 0)
            {
                // do something with it...
                writeRestraints(constId, treeId, nodeId, dist, lowerB, upperB);
            }
            
            // Get data that links atoms to restraints
                 constId    = save.getItem("_Dist_constraint.Constraints_ID");
                 treeId     = save.getItem("_Dist_constraint.Dist_constraint_tree_ID");
                 nodeId     = save.getItem("_Dist_constraint.Tree_node_member_node_ID");
            List endId      = save.getItem("_Dist_constraint.Constraint_tree_node_member_ID");
            List segId      = save.getItem("_Dist_constraint.Auth_segment_code");
            List resNum     = save.getItem("_Dist_constraint.Auth_seq_ID");
            List atomName   = save.getItem("_Dist_constraint.Auth_atom_ID");
            if(checkTableLength(new List[] {constId, treeId, nodeId, endId, segId, resNum, atomName}) > 0)
            {
                // do something with it...
                writeAtoms(segId, resNum, atomName);
                writeEnds(constId, treeId, nodeId, endId, segId, resNum, atomName);
            }
        }
        catch(IllegalStateException ex)
        { ex.printStackTrace(); }
    }
//}}}

//{{{ checkTableLength
//##############################################################################
    int checkTableLength(List[] cols)
    {
        int maxLen = 0;
        for(int i = 0; i < cols.length; i++)
            maxLen = Math.max(maxLen, cols[i].size());
        
        int retVal = maxLen;
        for(int i = 0; i < cols.length; i++)
        {
            if(cols[i].size() != maxLen)
            {
                System.err.println("cols["+i+"] is too short");
                retVal = 0;
            }
        }
        
        return retVal;
    }
//}}}

//{{{ writeRestraints
//##############################################################################
    void writeRestraints(List constId, List treeId, List nodeId, List dist, List lowerB, List upperB)
    {
        int len = constId.size();
        for(int i = 0; i < len; i++)
        {
            String restrKey = this.pdbId+"_"+this.distConstId
                +"_"+constId.get(i)+"_"+treeId.get(i)+"_"+nodeId.get(i);
            if(restraints.get(restrKey) != null)
                throw new IllegalStateException("Duplicated restraint name "+restrKey);
            String restrId = Integer.toString(firstRestraintId++);
            restraints.put(restrKey, restrId);
            
            outRestr.print(restrId);
            outRestr.print("\t");
            outRestr.print(this.pdbId);
            outRestr.print("\t");
            outRestr.print(dist.get(i));
            outRestr.print("\t");
            outRestr.print(i < lowerB.size() ? lowerB.get(i) : "\\N");
            outRestr.print("\t");
            outRestr.print(i < upperB.size() ? upperB.get(i) : "\\N");
            outRestr.print("\t");
            outRestr.print(this.subtype);
            outRestr.print("\t");
            outRestr.print(this.format);
            outRestr.println();
        }
        outRestr.flush();
    }
//}}}

//{{{ writeAtoms
//##############################################################################
    void writeAtoms(List segId, List resNum, List atomName)
    {
        int len = segId.size();
        for(int i = 0; i < len; i++)
        {
            String atomKey = this.pdbId+"_"+this.distConstId
                +"_"+segId.get(i)+"_"+resNum.get(i)+"_"+atomName.get(i);
            String atomId = (String) atoms.get(atomKey);
            if(atomId == null)
            {
                atomId = Integer.toString(firstAtomId++);
                atoms.put(atomKey, atomId);
                outAtoms.print(atomId);
                outAtoms.print("\t");
                outAtoms.print(this.pdbId);
                outAtoms.print("\t");
                outAtoms.print(segId.get(i));
                outAtoms.print("\t");
                outAtoms.print(resNum.get(i));
                outAtoms.print("\t");
                outAtoms.print(atomName.get(i));
                outAtoms.println();
            }
        }
        outAtoms.flush();
    }
//}}}

//{{{ writeEnds
//##############################################################################
    void writeEnds(List constId, List treeId, List nodeId, List endId,
        List segId, List resNum, List atomName)
    {
        int len = constId.size();
        for(int i = 0; i < len; i++)
        {
            String restrKey = this.pdbId+"_"+this.distConstId
                +"_"+constId.get(i)+"_"+treeId.get(i)+"_"+nodeId.get(i);
            String restrId = (String) restraints.get(restrKey);
            if(restrId == null)
                throw new IllegalStateException("Missing restraint name "+restrKey);
            
            String atomKey = this.pdbId+"_"+this.distConstId
                +"_"+segId.get(i)+"_"+resNum.get(i)+"_"+atomName.get(i);
            String atomId = (String) atoms.get(atomKey);
            if(atomId == null)
                throw new IllegalStateException("Missing atom name "+atomKey);
            
            outEnds.print(atomId);
            outEnds.print("\t");
            outEnds.print(restrId);
            outEnds.print("\t");
            outEnds.print(endId.get(i));
            outEnds.println();
        }
        outEnds.flush();
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
    public void Main() throws IOException
    {
        if(outRestr == null)
            outRestr = new PrintWriter(new FileWriter("restraints.tab"));
        if(outAtoms == null)
            outAtoms = new PrintWriter(new FileWriter("atoms.tab"));
        if(outEnds == null)
            outEnds = new PrintWriter(new FileWriter("atoms_restraints.tab"));
        
        processFileOrDir(inputFiles);
    }

    public static void main(String[] args)
    {
        ImportRestraints mainprog = new ImportRestraints();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
    }
//}}}

//{{{ parseArguments, showHelp
//##############################################################################
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
                { throw new IllegalArgumentException("'"+arg
                    +"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("ImportRestraints.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'ImportRestraints.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("cmdline.nmrstar.ImportRestraints");
        System.err.println("Copyright (C) 2007 by Ian W. Davis. All rights reserved.");
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
//##############################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
        inputFiles.add(new File(arg));
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

