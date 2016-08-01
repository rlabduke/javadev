// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
//}}}
/**
* <code>KinLoader</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb  3 15:37:23 EST 2005
*/
public class KinLoader extends List implements CommandListener
{
//{{{ Constants
    // Bit that marks entries as kinemage "entities" (NEVER set for points)
    static final int ENTITY_BIT         = 1<<31;
    static final int ENTITY_TYPE_SHIFT  = 16;
    static final int ENTITY_TYPE_MASK   = 0x7fff;
    static final int ENTITY_LEN_SHIFT   = 0;
    static final int ENTITY_LEN_MASK    = 0xffff;
    
    // Types of entities
    static final int ENT_NULL           = 0;
    static final int ENT_GROUP          = 1;
    static final int ENT_SUBGROUP       = 2;
    static final int ENT_LIST           = 3;
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain    kMain;
    Command     cmdChoose, cmdEnterURL, cmdDelete;
    String[]    storeNames = {};
    
    Form        urlForm;
    TextField   urlField;
    Command     urlOK, urlCancel;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KinLoader(KingMain kMain)
    {
        super("Choose kin", List.IMPLICIT);
        this.kMain = kMain;
        this.setCommandListener(this);
        
        cmdChoose = new Command("Open kin", Command.SCREEN, 1);
        this.addCommand(cmdChoose);
        cmdEnterURL = new Command("Get from Web", Command.SCREEN, 2);
        this.addCommand(cmdEnterURL);
        cmdDelete = new Command("Delete", Command.SCREEN, 3);
        this.addCommand(cmdDelete);
        
        urlField = new TextField("URL", "http://localhost/~ian/haikins/", 256, TextField.URL);
        urlForm = new Form("Kin URL", new Item[] { urlField });
        urlForm.setCommandListener(this);
        urlOK = new Command("OK", Command.OK, 1);
        urlForm.addCommand(urlOK);
        urlCancel = new Command("Cancel", Command.CANCEL, 2);
        urlForm.addCommand(urlCancel);
        
        loadKinList();
    }
//}}}

//{{{ loadKinList, findKinemages
//##############################################################################
    void loadKinList()
    {
        while(this.size() > 0) this.delete(0);
        
        String[] kinNames = findKinemages();
        for(int i = 0; i < kinNames.length; i++)
        {
            if(kinNames[i] != null) this.append(kinNames[i], null);
            else                    this.append(storeNames[i], null);
        }
    }

    /** Looks up all available kins stored in RMS system. */
    private String[] findKinemages()
    {
        String[] kinNames = {};
        this.storeNames = RecordStore.listRecordStores();
        if(storeNames == null) storeNames = new String[0];
        
        kinNames = new String[ storeNames.length ];
        
        for(int i = 0; i < storeNames.length; i++)
        {
            try
            {
                RecordStore store = RecordStore.openRecordStore(storeNames[i], false);
                byte[] name = store.getRecord(1); // the human-readable name
                kinNames[i] = new String(name);
                store.closeRecordStore();
            }
            catch(RecordStoreNotFoundException ex) { error("Not found: "+ex.getMessage()); }
            catch(RecordStoreNotOpenException ex) { error("Not open: "+ex.getMessage()); }
            catch(InvalidRecordIDException ex) { error("Bad ID: "+ex.getMessage()); }
            catch(RecordStoreFullException ex) { error("Full: "+ex.getMessage()); }
            catch(RecordStoreException ex) { error("General: "+ex.getMessage()); }
        }
        
        return kinNames;
    }
//}}}

//{{{ storeKinemage
//##############################################################################
    public void storeKinemage(String name, String about, InputStream in)
    {
        try
        {
            byte[] data;
            String storeName = Long.toString(System.currentTimeMillis(), 16)+name;
            if(storeName.length() > 32) storeName = storeName.substring(0, 32);
        
            RecordStore store = RecordStore.openRecordStore(storeName, true);
            data = name.getBytes(); store.addRecord(data, 0, data.length);
            data = about.getBytes(); store.addRecord(data, 0, data.length);
            data = new byte[4096];
            int i = 0;
            try
            {
                while(true)
                {
                    int d = in.read();
                    if(d == -1) break;
                    
                    data[i++] = (byte) d;
                    if(i == data.length)
                    {
                        store.addRecord(data, 0, data.length);
                        i = 0;
                    }
                }
            }
            catch(IOException ex) { error("I/O: "+ex.getMessage()); }
            if(i > 0) store.addRecord(data, 0, i);
            
            store.closeRecordStore();
        }
        catch(RecordStoreNotFoundException ex) { error("Not found: "+ex.getMessage()); }
        catch(RecordStoreNotOpenException ex) { error("Not open: "+ex.getMessage()); }
        catch(InvalidRecordIDException ex) { error("Bad ID: "+ex.getMessage()); }
        catch(RecordStoreFullException ex) { error("Full: "+ex.getMessage()); }
        catch(RecordStoreException ex) { error("General: "+ex.getMessage()); }
    }
//}}}

//{{{ loadKinemage
//##############################################################################
    public void loadKinemage(InputStream is)
    {
        DataInputStream in = new DataInputStream(is);
        kMain.kCanvas.clearKinemage(); // discarding the old kin
        KPoint tailPt = null;
        View view = null;
        Vector groupList = new Vector();
        int nPointsRead = 0;
        
        try 
        {
            while(true)
            {
                int multi = in.readInt();
                if((multi & ENTITY_BIT) == 0) // POINTS
                {
                    KPoint newPt = new KPoint(in.readInt(), in.readInt(), in.readInt(), multi);
                    if(newPt.getType() == KPoint.TYPE_LABEL)
                        newPt.setPointID( readUnicodeString(in) );
                    newPt.prev = tailPt;
                    tailPt = newPt;
                    nPointsRead++;
                }
                else // ENTITIES (anything that's not a point)
                {
                    int entityType = (multi>>ENTITY_TYPE_SHIFT) & ENTITY_TYPE_MASK;
                    int entityLen = (multi>>ENTITY_LEN_SHIFT) & ENTITY_LEN_MASK;
                    switch(entityType)
                    {
                        case ENT_GROUP:
                        case ENT_SUBGROUP:
                        case ENT_LIST:
                            loadGroup(in, groupList, entityType, tailPt);
                            break;
                        default: // just discard the bytes for this entity
                            for(int i = 0; i < entityLen; i++) in.readUnsignedByte();
                            break;
                    }
                }
            }
        }
        catch(IOException ex) {} // this is also how we find EOF
        try { in.close(); } catch(IOException ex) {}
        
        if(tailPt == null)
        {
            error("Kinemage had no points!");
            return;
        }
        
        if(view == null) view = makeStartingView(tailPt);
        
        // Terminate open groups if needed.
        for(int i = 0; i < groupList.size(); i++)
        {
            KGroup g = (KGroup) groupList.elementAt(i);
            if(g.startPoint == null) g.startPoint = tailPt;
        }
        
        kMain.kCanvas.loadKinemage(tailPt, view, groupList);
        Display.getDisplay(kMain).setCurrent(kMain.kCanvas);
    }
//}}}

//{{{ loadGroup
//##############################################################################
    void loadGroup(DataInput in, Vector groupList, int groupDepth, KPoint lastPointRead) throws IOException
    {
        //int groupDepth = in.readInt();
        int groupFlags = in.readInt();
        String groupName = readUnicodeString(in);
        
        // Terminate open groups if needed.
        for(int i = 0; i < groupList.size(); i++)
        {
            KGroup g = (KGroup) groupList.elementAt(i);
            if(g.depth >= groupDepth && g.startPoint == null)
                g.startPoint = lastPointRead;
        }
        
        KGroup group = new KGroup(groupName, groupDepth, groupFlags);
        group.stopPoint = lastPointRead;
        groupList.addElement(group);
    }
//}}}

//{{{ readUnicodeString
//##############################################################################
    /**
    * Reads a string stored as a number of characters (4-byte int)
    * followed by those characters as two-byte Unicode chars.
    */
    String readUnicodeString(DataInput in) throws IOException
    {
        int strlen = in.readInt();
        StringBuffer buf = new StringBuffer(strlen);
        for(int i = 0; i < strlen; i++) buf.append( in.readChar() );
        return buf.toString();
    }
//}}}

//{{{ makeStartingView
//##############################################################################
    View makeStartingView(KPoint p)
    {
        View view = new View();
        if(p == null) return view;
        
        int minx = p.x0, miny = p.y0, minz = p.z0;
        int maxx = p.x0, maxy = p.y0, maxz = p.z0;
        p = p.prev;
        while(p != null)
        {
            minx = Math.min(minx, p.x0);
            miny = Math.min(miny, p.y0);
            minz = Math.min(minz, p.z0);
            maxx = Math.max(maxx, p.x0);
            maxy = Math.max(maxy, p.y0);
            maxz = Math.max(maxz, p.z0);
            p = p.prev;
        }
        
        // Calculate becomes center
        view.cx = (minx + maxx) / 2;
        view.cy = (miny + maxy) / 2;
        view.cz = (minz + maxz) / 2;
        
        // Max becomes halfwidths
        maxx -= view.cx;
        maxy -= view.cy;
        maxz -= view.cz;
        int radius = Math.max(Math.max(maxx, maxy), maxz);
        int viewport = Math.min(kMain.kCanvas.getWidth(), kMain.kCanvas.getHeight())/2;
        
        view.setScale(0);
        while((radius / view.getScaleDivisor()) > viewport) view.setScale(view.getScale()+1);
        
        return view;
    }
//}}}

//{{{ commandAction
//##############################################################################
    public void commandAction(Command c, Displayable s)
    {
        if(c == cmdChoose)// || c == List.SELECT_COMMAND)
        {
            int index = this.getSelectedIndex();
            if(index == -1) return;
            try
            {
                RecordStore store = RecordStore.openRecordStore(storeNames[index], false);
                loadKinemage(new RecordStoreInputStream(store, 3));
                store.closeRecordStore();
            }
            catch(RecordStoreNotFoundException ex) { error("Not found: "+ex.getMessage()); }
            catch(RecordStoreNotOpenException ex) { error("Not open: "+ex.getMessage()); }
            catch(InvalidRecordIDException ex) { error("Bad ID: "+ex.getMessage()); }
            catch(RecordStoreFullException ex) { error("Full: "+ex.getMessage()); }
            catch(RecordStoreException ex) { error("General: "+ex.getMessage()); }
        }
        else if(c == cmdEnterURL)
        { Display.getDisplay(kMain).setCurrent(urlForm); }
        else if(c == cmdDelete)
        {
            int index = this.getSelectedIndex();
            if(index == -1) return;
            try
            {
                RecordStore.deleteRecordStore(storeNames[index]);
            }
            catch(RecordStoreNotFoundException ex) { error("Not found: "+ex.getMessage()); }
            catch(RecordStoreException ex) { error("General: "+ex.getMessage()); }
            this.loadKinList();
        }
        else if(c == urlOK)
        {
            try
            {
                String url = urlField.getString();
                InputStream in = Connector.openInputStream(url);
                String name = url;
                try {
                    if(name.lastIndexOf('/') != -1) name = name.substring(name.lastIndexOf('/')+1, name.length());
                    if(name.lastIndexOf('.') != -1) name = name.substring(0, name.lastIndexOf('.'));
                } catch(IndexOutOfBoundsException ex) {}
                storeKinemage(name, url, in);
                in.close();
                this.loadKinList();
                Display.getDisplay(kMain).setCurrent(this);
            }
            catch(Exception ex) { error(ex.getClass()+": "+ex.getMessage()); }
        }
        else if(c == urlCancel)
        { Display.getDisplay(kMain).setCurrent(this); }
        else kMain.commandAction(c, s);
    }
//}}}

//{{{ error
//##############################################################################
    public void error(String msg)
    {
        System.err.println(msg);
        Alert alert = new Alert("Error", msg, null, AlertType.ERROR);
        alert.setTimeout(Alert.FOREVER);
        Display.getDisplay(kMain).setCurrent(alert);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

