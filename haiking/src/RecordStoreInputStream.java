// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;

import java.io.*;
import javax.microedition.rms.*;
//}}}
/**
* <code>RecordStoreInputStream</code> allows one to read records from a RecordStore
* as if they were continuous blocks of a file, thus avoiding trying to load
* a large block of data into memory all at once.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Feb 10 10:10:45 EST 2005
*/
public class RecordStoreInputStream extends InputStream
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    RecordStore     store;
    byte[]          data = null;
    int             index;
    int             nextRecord;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public RecordStoreInputStream(RecordStore store, int firstRecord)
    {
        super();
        this.store = store;
        this.nextRecord = firstRecord;
    }
//}}}

//{{{ read
//##############################################################################
    public int read() throws IOException
    {
        // Use while to skip over 0-byte records
        while(data == null || index >= data.length)
        {
            try
            {
                if(nextRecord >= store.getNextRecordID()) return -1;
                data = null; // might help with gc
                data = store.getRecord(nextRecord++);
                index = 0;
                //System.err.println("new data block: "+data.length+" bytes");
            }
            catch(RecordStoreException ex)
            { throw new IOException(ex.getClass()+": "+ex.getMessage()); }
        }
        //System.err.println("Byte: "+data[index]);
        return data[index++] & 0xff; // have to do this or values come out negative!
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

