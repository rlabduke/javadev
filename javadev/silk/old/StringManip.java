// (jEdit options) :folding=explicit:collapseFolds=1:
package boundrotamers;
//import java.awt.*;
//import java.io.*;
import java.util.*;
//import javax.swing.*;

/**
 * <code>StringManip</code> contains string-processing routines that are commonly required but are missing from Java 1.3.
 * 'Inspiration' for many of these came from PHP's built-in functions.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Sat Mar 23 23:22:28 EST 2002
 */
public class StringManip //extends ... implements ...
{
//{{{ explode()
//##################################################################################################
    /**
    * Explodes a string, breaking it up into pieces defined by a delimiter character.
    *
    * @param src string to act on
    * @param separator the delimiter break the string on
    * @param trim call String.trim() on pieces
    * @param keepempties don't discard pieces that are empty strings
    * @return an array of Strings representing the pieces of <code>src</code>
    */
    public static String[] explode(String src, char separator, boolean trim, boolean keepempties)
    {
        Vector v = new Vector();

        int i, old_i, len = src.length();
        String piece;

        try {
            old_i = -1;
            for(i = src.indexOf(separator); i < len && i != -1; old_i = i, i = src.indexOf(separator, i+1))
            {
                piece = src.substring(old_i+1, i);
                if(trim) piece = piece.trim();
                if(piece.length() == 0 && !keepempties) {}
                else { v.add(piece); }
            }
            // Make sure we get the last piece!
            piece = src.substring(old_i+1);
            if(trim) piece = piece.trim();
            if(piece.length() == 0 && !keepempties) {}
            else { v.add(piece); }
        } catch(IndexOutOfBoundsException ex) {}

        // copy pieces to an array
        int sz = v.size();
        String[] result = new String[sz];
        for(i = 0; i < sz; i++) result[i] = (String)v.elementAt(i);
        return result;
    }

    /**
    * Explodes a string, breaking it up into pieces defined by a delimiter character.
    * Empty string pieces are not discarded and all pieces are trimmed of excess whitespace.
    *
    * @param src string to act on
    * @param separator the delimiter break the string on
    * @return an array of Strings representing the pieces of <code>src</code>
    */
    public static String[] explode(String src, char separator)
    {
        return explode(src, separator, true, true);
    }
//}}}

//{{{ explodeInts()
//##################################################################################################
    /**
    * Converts a textual list of numbers into an array of integers.
    *
    * @param src string to act on
    * @param delim delimiter character(s)
    * @throws NumberFormatException if one or more elements cannot be parsed
    */
    public static int[] explodeInts(String src, String delim) throws NumberFormatException
    {
        Vector v = new Vector();
        StringTokenizer st = new StringTokenizer(src, delim);
        Integer x;

        while(st.hasMoreTokens())
        {
            x = Integer.valueOf(st.nextToken());
            v.add(x);
        }

        // copy pieces to an array
        int sz = v.size();
        int[] result = new int[sz];
        for(int i = 0; i < sz; i++) result[i] = ((Integer)v.elementAt(i)).intValue();
        return result;
    }
//}}}

//{{{ explodeDoubles(), explodeFloats()
//##################################################################################################
    /**
    * Converts a textual list of numbers into an array of doubles.
    *
    * @param src string to act on
    * @param delim delimiter character(s)
    * @throws NumberFormatException if one or more elements cannot be parsed
    */
    public static double[] explodeDoubles(String src, String delim) throws NumberFormatException
    {
        Vector v = new Vector();
        StringTokenizer st = new StringTokenizer(src, delim);
        Double x;

        while(st.hasMoreTokens())
        {
            x = Double.valueOf(st.nextToken());
            v.add(x);
        }

        // copy pieces to an array
        int sz = v.size();
        double[] result = new double[sz];
        for(int i = 0; i < sz; i++) result[i] = ((Double)v.elementAt(i)).doubleValue();
        return result;
    }

    /**
    * Converts a textual list of numbers into an array of floats.
    *
    * @param src string to act on
    * @param delim delimiter character(s)
    * @throws NumberFormatException if one or more elements cannot be parsed
    */
    public static float[] explodeFloats(String src, String delim) throws NumberFormatException
    {
        double[] d = explodeDoubles(src, delim);
        float[]  f = new float[d.length];

        for(int i = 0; i < d.length; i++)
        {
            f[i] = (float)d[i];
        }

        return f;
    }
//}}}

//{{{ Pad left/right/center
//##################################################################################################
    /**
    * Pads a string with spaces and left-justifies it.
    *
    * @param s the string to pad
    * @param len the desired length
    */
    public static String padLeft(String s, int len)
    {
        StringBuffer sb = new StringBuffer(s);
        sb.ensureCapacity(len);
        for(int i = s.length(); i < len; i++) sb.append(' ');
        return sb.toString();
    }

    /**
    * Pads a string with spaces and right-justifies it.
    *
    * @param s the string to pad
    * @param len the desired length
    */
    public static String padRight(String s, int len)
    {
        StringBuffer sb = new StringBuffer(s);
        sb.ensureCapacity(len);
        for(int i = s.length(); i < len; i++) sb.insert(0, ' ');
        return sb.toString();
    }

    /**
    * Pads a string with spaces and centers it.
    *
    * @param s the string to pad
    * @param len the desired length
    */
    public static String padCenter(String s, int len)
    {
        StringBuffer sb = new StringBuffer(s);
        sb.ensureCapacity(len);
        for(int i = s.length(); i < len; i++)
        {
            sb.append(' ');
            if(++i < len) sb.insert(0, ' ');
        }
        return sb.toString();
    }
//}}}

//{{{ parseInt()
//##################################################################################################
    /**
    * A more robust and flexible integer-parser than is provided by Java.
    * Digit characters are the set [+-0123456789].
    * Leading non-digit characters are ignored and parsing stops with the
    * first non-digit character following the first digit character.
    * If no number is found in the string, a NumberFormatException is thrown.
    *
    * @param source the string to parse
    * @param begin the offset at which to begin (inclusive)
    * @param end the offset at which to end (exclusive)
    * @return the integer representation of source
    * @throws NumberFormatException if no number is found in source
    */
    public static int parseInt(String source, int begin, int end) throws NumberFormatException
    {
        end = Math.min(end, source.length());
        int i = begin;
        char c = source.charAt(i);

        while( (c<'0' || c>'9') && c!='+' && c!='-' )
        {
            if(++i >= end) throw new NumberFormatException(source);
            c = source.charAt(i);
        }

        //try { for( ; (c<'0' || c>'9') && c!='+' && c!='-'; c = source.charAt(++i)); }
        //catch(IndexOutOfBoundsException ex) { throw new NumberFormatException(source); }

        boolean negative = false;
        if(c == '-') { negative = true; c = source.charAt(++i); }
        else if(c == '+') { c = source.charAt(++i); }

        int result = 0;
        while( c>='0' && c<='9' )
        {
            result *= 10;
            result += c - '0';
            if(++i >= end) break;
            c = source.charAt(i);
        }

        return (negative ? -result : result);
    }
//}}}

//{{{ parseDouble
//##################################################################################################
    /**
    * A more robust and flexible real-number-parser than is provided by Java.
    * Digit characters are the set [+-.eE0123456789].
    * Leading non-digit characters are ignored and parsing stops with the
    * first non-digit character following the first digit character.
    * If no number is found in the string, a NumberFormatException is thrown.
    *
    * @param source the string to parse
    * @param begin the offset at which to begin (inclusive)
    * @param end the offset at which to end (exclusive)
    * @return the double representation of source
    * @throws NumberFormatException if no number is found in source
    */
    public static double parseDouble(String source, int begin, int end) throws NumberFormatException
    {
        end = Math.min(end, source.length());
        int i = begin;
        char c = source.charAt(i);

        while( (c<'0' || c>'9') && c!='+' && c!='-' && c!='.' )
        {
            if(++i >= end) throw new NumberFormatException(source);
            c = source.charAt(i);
        }

        boolean negative = false;
        if(c == '-') { negative = true; c = source.charAt(++i); }
        else if(c == '+') { c = source.charAt(++i); }

        double result = 0.0;
        boolean decpoint = false;
        int power = 0;

        while( (c>='0' && c<='9') || c=='.')
        {
            if(c == '.') { decpoint = true; }
            else
            {
                if(decpoint) power--;
                result *= 10;
                result += c - '0';
            }

            if(++i >= end) break;
            c = source.charAt(i);
        }

        if( c=='e' || c=='E' ) power += parseInt(source, i, end);
        result *= Math.pow(10.0, power);

        return (negative ? -result : result);
    }
//}}}

//##################################################################################################
    // Convenience function for debugging
    void echo(String s) { System.err.println(s); }

}//class
