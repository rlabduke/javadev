// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;

import java.io.*;
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
        
        cmdChoose = new Command("Open local", Command.SCREEN, 1);
        this.addCommand(cmdChoose);
        cmdEnterURL = new Command("Open URL", Command.SCREEN, 2);
        this.addCommand(cmdEnterURL);
        cmdDelete = new Command("Delete", Command.SCREEN, 3);
        this.addCommand(cmdDelete);
        
        urlField = new TextField("URL", "http://kinemage.biochem.duke.edu/", 256, TextField.URL);
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

//{{{ loadKinemage
//##############################################################################
    public void loadKinemage(InputStream is)
    {
        DataInputStream in = new DataInputStream(is);
        kMain.kCanvas.clearKinemage(); // discarding the old kin
        KPoint tailPt = null;
        int nPointsRead = 0;
        try 
        {
            while(true)
            {
                KPoint newPt = new KPoint(in.readInt(), in.readInt(), in.readInt(), in.readInt());
                if(newPt.getType() == KPoint.TYPE_LABEL)
                {
                    int strlen = in.readInt();
                    StringBuffer buf = new StringBuffer(strlen);
                    for(int i = 0; i < strlen; i++) buf.append( in.readChar() );
                    newPt.setPointID( buf.toString() );
                }
                newPt.prev = tailPt;
                tailPt = newPt;
                nPointsRead++;
            }
        }
        catch(IOException ex) {} // this is also how we find EOF
        try { in.close(); } catch(IOException ex) {}
        
        if(tailPt == null)
        {
            error("Kinemage had no points!");
            return;
        }
        
        View view = makeStartingView(tailPt);
        
        /*System.err.println("view: "+view.cx+"  "+view.cy+"  "+view.cz+"  scale "+view.getScale());
        int cnt = 0;
        KPoint p = tailPt;
        for( ; p != null; cnt++) p = p.prev;
        System.err.println(cnt+" points in kinemage; "+nPointsRead+" were read in");*/
        
        kMain.kCanvas.loadKinemage(tailPt, view);
        Display.getDisplay(kMain).setCurrent(kMain.kCanvas);
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
        while((radius >> view.getScale()) > viewport) view.setScale(view.getScale()+1);
        
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

