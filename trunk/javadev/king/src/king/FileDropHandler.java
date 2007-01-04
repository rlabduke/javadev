// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.util.*;
//}}}
/**
* <code>FileDropHandler</code> allows Drag-n-Drop opening of kinemages
* on the KinCanvas.
*
* <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 26 15:31:04 EST 2004
*/
public class FileDropHandler extends TransferHandler
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain kMain;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FileDropHandler(KingMain kMain, JComponent kCanvas)
    {
        super();
        this.kMain = kMain;
        
        kCanvas.setTransferHandler(this);
    }
//}}}

//{{{ canImport
//##############################################################################
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)
    {
        // we can ignore comp because this handler isn't shared by multiple components
        for(int i = 0; i < transferFlavors.length; i++)
        {
            if(DataFlavor.javaFileListFlavor.equals(transferFlavors[i]))
                return true;
        }
        return false;
    }
//}}}

//{{{ importData
//##############################################################################
    public boolean importData(JComponent comp, Transferable t)
    {
        // we can ignore comp because this handler isn't shared by multiple components
        if(canImport(comp, t.getTransferDataFlavors()))
        {
            try
            {
                KinfileIO io = kMain.getKinIO();
                java.util.List filelist = (java.util.List) t.getTransferData(DataFlavor.javaFileListFlavor);
                for(Iterator iter = filelist.iterator(); iter.hasNext(); )
                    io.loadFile((File)iter.next(), null);
            }
            catch(IOException ex) { ex.printStackTrace(SoftLog.err); }
            catch(UnsupportedFlavorException ex) { ex.printStackTrace(SoftLog.err); }
        }
        return false;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

