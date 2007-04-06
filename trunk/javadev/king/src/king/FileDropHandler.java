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
* on the KinCanvas or some other JComponent.
*
* <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 26 15:31:04 EST 2004
*/
public class FileDropHandler extends TransferHandler
{
//{{{ INTERFACE: Listener
//##############################################################################
    /**
    * The toString() method should return a single phrase describing
    * the action performed by this listener with the dropped files.
    */
    public interface Listener
    {
        /**
        * Returns true if the listener can do something useful with the given
        * file (like open it), or false otherwise.
        * Does NOT actually do anything with the file.
        */
        public boolean canHandleDroppedFile(File file);

        /**
        * Actually opens/reads/whatever the dropped file.
        */
        public void handleDroppedFile(File file);
    }
//}}}

//{{{ CLASS: KinFileOpen
//##############################################################################
    private class KinFileOpen implements Listener
    {
        public String toString()
        { return "Open the kinemage file"; }
        
        public boolean canHandleDroppedFile(File file)
        {
            String name = file.getName().toLowerCase();
            return name.endsWith(".kin") || name.endsWith(".kip");
        }
        
        public void handleDroppedFile(File file)
        {
            KinfileIO io = kMain.getKinIO();
            io.loadFile(file, null);
        }
    }
//}}}

//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain kMain;
    ArrayList<Listener> listeners = new ArrayList<Listener>();
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FileDropHandler(KingMain kMain)
    {
        super();
        this.kMain = kMain;
        this.addFileDropListener(new KinFileOpen());
    }
//}}}

//{{{ handleDropsFor, addFileDropListener
//##############################################################################
    /** Sets this as a listener for file drops on the component. */
    public void handleDropsFor(JComponent kCanvas)
    {
        kCanvas.setTransferHandler(this);
    }

    /** Gives the listener a chance to do something with dropped files. */
    public void addFileDropListener(Listener listener)
    {
        listeners.add(listener);
    }
//}}}

//{{{ doFileDrop
//##############################################################################
    public void doFileDrop(File file)
    {
        if(!file.exists())
        {
            SoftLog.err.println("Drag-n-drop of non-existant file!? '"+file+"'");
            return;
        }
        
        ArrayList<Listener> ok = new ArrayList<Listener>();
        for(Listener l : listeners)
            if(l.canHandleDroppedFile(file))
                ok.add(l);
        if(ok.size() == 0)
            return; // should show error msg?
        else if(ok.size() == 1)
            ok.get(0).handleDroppedFile(file);
        else
        {
            Listener theOne = (Listener) JOptionPane.showInputDialog(
                kMain.getTopWindow(), "What would you like to do with "+file.getName()+"?",
                "What would you like to do?", JOptionPane.QUESTION_MESSAGE, null,
                ok.toArray(), ok.get(0)
            );
            if(theOne != null)
                theOne.handleDroppedFile(file);
        }
    }
//}}}

//{{{ canImport
//##############################################################################
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)
    {
        // we can ignore comp because this handler is the same for all components
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
        // we can ignore comp because this handler is the same for all components
        if(canImport(comp, t.getTransferDataFlavors()))
        {
            try
            {
                //KinfileIO io = kMain.getKinIO();
                java.util.List filelist = (java.util.List) t.getTransferData(DataFlavor.javaFileListFlavor);
                for(Iterator iter = filelist.iterator(); iter.hasNext(); )
                    //io.loadFile((File)iter.next(), null);
                    doFileDrop((File) iter.next());
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

