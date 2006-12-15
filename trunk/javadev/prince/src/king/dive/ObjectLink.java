// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.dive;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>ObjectLink</code> encapsulates the requirements for streaming objects
* back and forth over a TCP connection.
* It does allocate a background thread to do its work, so it's not cheap...
*
* <p>In the declaration, S is the type to be Sent, and R is the type to be Received.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec 15 08:14:25 EST 2006
*/
public class ObjectLink <S extends Serializable, R extends Serializable> //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    Socket              socket = null;
    ObjectOutputStream  outStream;
    ObjectInputStream   inStream;
    
    BlockingQueue       recvQueue = new LinkedBlockingQueue();
    Exception           recvEx = null;
    Receiver            receiver;
    Thread              recvThread;
//}}}

//{{{ Constructor(s)
//##############################################################################
    /** Tries to connect to the server at host:port */
    public ObjectLink(String host, int port) throws IOException
    {
        super();
        InetAddress addr = InetAddress.getByName(host); //IOEx
        this.socket = new Socket(addr, port); //IOEx
        setupStreams(); //IOEx
    }

    /**
    * Listens for clients on local port "port".
    * This is a convenience for "one off" servers,
    * but for "real" servers that might listen for multiple
    * connecting clients, you need to handle the ServerSocket yourself.
    */
    public ObjectLink(int port) throws IOException
    {
        super();
        ServerSocket server = new ServerSocket(port); //IOEx
        this.socket = server.accept(); //IOEx (various)
        server.close();
        setupStreams(); //IOEx
    }
    
    /** Connects with a pre-established socket */
    public ObjectLink(Socket socket) throws IOException
    {
        super();
        this.socket = socket;
        setupStreams(); //IOEx
    }
//}}}

//{{{ setupStreams, disconnect
//##############################################################################
    private void setupStreams() throws IOException
    {
        socket.setKeepAlive(true); // may not be needed, really
        
        outStream = new ObjectOutputStream(
            new BufferedOutputStream(socket.getOutputStream()));
        // Essential to prevent deadlock: OIS must read header on create
        outStream.flush();
        inStream = new ObjectInputStream(
            new BufferedInputStream(socket.getInputStream()));
        
        // Can't have this guys trying anything until the socket exists!
        receiver = new Receiver();
        recvThread = new Thread(receiver);
        recvThread.setDaemon(true);
        recvThread.start();
    }
    
    public void disconnect()
    {
        receiver.quit = true;
        receiver = null;

        try { outStream.close(); } catch(IOException ex) {}
        try { inStream.close(); } catch(IOException ex) {}
        try { socket.close(); } catch(IOException ex) {}
        socket = null;
        outStream = null;
        inStream = null;
    }
//}}}

//{{{ send, receive
//##############################################################################
    private void send(Object o) throws IOException
    {
        try { outStream.reset(); } catch(IOException ex) {}
        try { outStream.writeObject(o); }
        finally
        {
            try { outStream.flush(); } catch(IOException ex) {}
        }
        // OOS maintains an internal cache of all the objects sent so far,
        // so that multiple references can resolve to the same object on both ends.
        // OIS must do the same, in order to decode the stream.
        // But for us, this is a single-send method, and we want stuff GC'd after.
        // Calling reset() clears that cache, and notifies OIS to do the same.
        // This is much better than discarding the OOS/OIS (which leads to
        // worries of inadvertant buffer loss with OIS).
        // Note we flush, not close, because closing the stream closes the socket.
    }
    
    private Object receive() throws Exception
    {
        return inStream.readObject();
        // See notes in send() about object caching.
    }
//}}}

//{{{ CLASS: Receiver
//##############################################################################
    /** Basically, this exists so we can have non-blocking reads */
    class Receiver implements Runnable
    {
        public volatile boolean quit = false;
        
        public void run()
        {
            while(!quit)
            {
                Object o = null;
                try { o = receive(); } catch(Exception ex) { recvEx = ex; }
                if(o != null) recvQueue.add(o);
            }
        }
    }
//}}}

//{{{ get, getBlocking, put
//##############################################################################
    /**
    * Returns the next object that was sent to us, or null if none is available.
    *
    * @throws Exception if an exception was generated while trying to read an
    *   object from the stream.  It is not guaranteed that the next available
    *   object in the queue was the cause of the exception, nor is it
    *   guaranteed that exceptions will not be "lost" by this mechanism.
    *   See ObjectInput.readObject() for a list of possible exceptions.
    */
    public R get() throws Exception
    {
        if(recvEx != null)
        {
            Exception ex = recvEx;
            recvEx = null;
            throw ex;
        }
        return (R) recvQueue.poll();
    }
    
    /** Like get(), but doesn't return until an object is available. */
    public R getBlocking() throws Exception
    {
        if(recvEx != null)
        {
            Exception ex = recvEx;
            recvEx = null;
            throw ex;
        }
        return (R)recvQueue.take();
    }
    
    /**
    * Queues an object to be sent to a remote listener.
    *
    * @throws IOException if an exception was generated while trying to send an
    *   object to the stream.  Unlike get(), this method blocks until the object
    *   is sent, so any exception comes directly from sending this object.
    */
    public void put(S o) throws IOException
    { send(o); }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

