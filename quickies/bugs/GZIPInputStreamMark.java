// (jEdit options) :folding=explicit:collapseFolds=1:
import java.io.*;
import java.util.*;
import java.util.zip.*;
/**
* <code>GZIPInputStreamMark</code> show that a bug exists in
* GZIPInputStream.mark() and GZIPInputStream.reset().
*
* Supply a gzipped file as input on stdin.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb 18 15:05:39 EST 2003
*/
public class GZIPInputStreamMark //extends ... implements ...
{
    public static void main(String[] args) throws IOException
    {
        final int sz = 10, ntests = 3;
        int i, j;
        
        BufferedInputStream buf1 = new BufferedInputStream(System.in);
        System.out.println("buf1.markSupported() == "+buf1.markSupported());
        for(j = 0; j < ntests; j++)
        {
            System.out.print("Test "+(j+1)+": first "+sz+" bytes of stream:");
            buf1.mark(sz);
            for(i = 0; i < sz; i++) System.out.print(" "+buf1.read());
            System.out.println();
            buf1.reset();
        }
        System.out.println();
        
        GZIPInputStream buf2 = new GZIPInputStream(buf1);
        System.out.println("buf2.markSupported() == "+buf2.markSupported());
        for(j = 0; j < ntests; j++)
        {
            System.out.print("Test "+(j+1)+": first "+sz+" bytes of stream:");
            buf2.mark(sz);
            for(i = 0; i < sz; i++) System.out.print(" "+buf2.read());
            System.out.println();
            buf2.reset();
        }
        System.out.println();
        
        BufferedInputStream buf3 = new BufferedInputStream(buf2);
        System.out.println("buf3.markSupported() == "+buf3.markSupported());
        for(j = 0; j < ntests; j++)
        {
            System.out.print("Test "+(j+1)+": first "+sz+" bytes of stream:");
            buf3.mark(sz);
            for(i = 0; i < sz; i++) System.out.print(" "+buf3.read());
            System.out.println();
            buf3.reset();
        }
        System.out.println();
    }
}//class

