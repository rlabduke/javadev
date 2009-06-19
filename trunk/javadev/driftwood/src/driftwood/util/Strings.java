// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>Strings</code> is a utility class for manipulating and formatting
* String objects in a variety of ways.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 20 10:35:10 EST 2003
*/
public class Strings //extends ... implements ...
{
//{{{ Constants
    static final DecimalFormat df0 = new DecimalFormat("0");
    static final DecimalFormat df1 = new DecimalFormat("0.0");
//}}}

//{{{ Variable definitions
//##################################################################################################
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public Strings()
    {
    }
//}}}

//{{{ justify{Left, Right, Center}
//##################################################################################################
    /**
    * Pads a string with spaces and left-justifies it.
    *
    * @param s the string to justify
    * @param len the desired length
    */
    public static String justifyLeft(String s, int len)
    {
        StringBuffer sb = new StringBuffer(s);
        sb.ensureCapacity(len);
        for(int i = s.length(); i < len; i++) sb.append(' ');
        return sb.toString();
    }

    /**
    * Pads a string with spaces and right-justifies it.
    *
    * @param s the string to justify
    * @param len the desired length
    */
    public static String justifyRight(String s, int len)
    {
        StringBuffer sb = new StringBuffer(s);
        sb.ensureCapacity(len);
        for(int i = s.length(); i < len; i++) sb.insert(0, ' ');
        return sb.toString();
    }

    /**
    * Pads a string with spaces and centers it.
    *
    * @param s the string to justify
    * @param len the desired length
    */
    public static String justifyCenter(String s, int len)
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

//{{{ force{Left, Right}
//##################################################################################################
    /**
    * Pads a string with spaces and left-justifies it.
    * If the string exceeds the specified length to start with, the
    * left-most <i>len</i> characters are returned.
    * @param s the string to justify
    * @param len the desired length
    */
    public static String forceLeft(String s, int len)
    {
        int l = s.length();
        if(l > len)         return s.substring(0, len);
        else if(l == len)   return s;
        else                return justifyLeft(s, len);
    }

    /**
    * Pads a string with spaces and right-justifies it.
    * If the string exceeds the specified length to start with, the
    * right-most <i>len</i> characters are returned.
    * @param s the string to justify
    * @param len the desired length
    */
    public static String forceRight(String s, int len)
    {
        int l = s.length();
        if(l > len)         return s.substring(l-len, l);
        else if(l == len)   return s;
        else                return justifyRight(s, len);
    }
//}}}

//{{{ formatMemory
//##################################################################################################
    /** Formats a long in terms of memory (kb, Mb, etc) */
    public static String formatMemory(long mem)
    {
             if(mem < 1000L)            return mem+" b";
        else if(mem < 10000L)           return df1.format( (double)mem / 1000L )+" kb";
        else if(mem < 1000000L)         return df0.format( (double)mem / 1000L )+" kb";
        else if(mem < 10000000L)        return df1.format( (double)mem / 1000000L )+" Mb";
        else if(mem < 1000000000L)      return df0.format( (double)mem / 1000000L )+" Mb";
        else if(mem < 10000000000L)     return df1.format( (double)mem / 1000000000L )+" Gb";
        else if(mem < 1000000000000L)   return df0.format( (double)mem / 1000000000L )+" Gb";
        else if(mem < 10000000000000L)  return df1.format( (double)mem / 1000000000000L )+" Tb";
        else                            return df0.format( (double)mem / 1000000000000L )+" Tb";
    }
//}}}

//{{{ tokenizeCommandLine
//##################################################################################################
    /**
    * Breaks a command line into tokens suitable for passing to Runtime.exec().
    * Tokens are delimitated by whitespace (as defined by Character.isWhitespace()),
    * and all whitespace is discarded.
    * Quoting with either double or single quotes is allowed,
    * and whitespace inside quotes will be preserved.
    * No escape sequences are recognized (inside or outside quotes).
    */
    public static String[] tokenizeCommandLine(String cmdline)
    {
        int i = 0, len = cmdline.length();
        ArrayList tokenList = new ArrayList();
        StringBuffer token = null;
        char ch, quoteMode = '\0';
        
        for(i = 0; i < len; i++)
        {
            ch = cmdline.charAt(i);
            if(quoteMode == '\'' || quoteMode == '"')
            {
                if(ch == quoteMode)
                {
                    tokenList.add(token.toString());
                    token       = null;
                    quoteMode   = '\0';
                }
                else token.append(ch);
            }
            else if(ch == '\'' || ch == '"')
            {
                if(token != null) tokenList.add(token.toString());
                token       = new StringBuffer();
                quoteMode   = ch;
            }
            else if(Character.isWhitespace(ch))
            {
                if(token != null)
                {
                    tokenList.add(token.toString());
                    token = null;
                }
            }
            else
            {
                if(token == null) token = new StringBuffer();
                token.append(ch);
            }
        }//for(each character in cmdline)
        
        // Make sure we get the last one!
        if(token != null) tokenList.add(token.toString());
        
        return (String[])tokenList.toArray( new String[tokenList.size()] );
    }
//}}}

//{{{ jarUrlToFile
//##################################################################################################
    /**
    * Given a URL that references something in a local JAR file
    * (e.g. jar:file:/some/crazy/path/to/my.jar!/foo/bar.class)
    * this function will return a File object that points to
    * the JAR file in question.
    * @throws NullPointerException if the URL is null
    * @throws IllegalArgumentException if the URL is inappropriate,
    *   for example a URL that references a remote location via HTTP.
    * @throws IOException if there's a filesystem error when locating the JAR file.
    */
    static public File jarUrlToFile(URL url) throws IOException
    {
        if(!url.getProtocol().equals("jar"))
            throw new IllegalArgumentException("URL must be a jar: type URL");

        JarURLConnection jarConn = (JarURLConnection)url.openConnection();
        url = jarConn.getJarFileURL();
        
        if(!url.getProtocol().equals("file"))
            throw new IllegalArgumentException("URL must be a jar:file: type URL");
        
        // decode() translates e.g. %20 into a space;
        // the File constructor is OK with e.g.
        // Windows paths containing backslashes
        File f = new File(URLDecoder.decode(url.getFile()));
        
        if(!f.exists())
            throw new IOException("Specified JAR file does not exist");
        
        return f;
    }
//}}}

//{{{ count
  public static int count(String s, String sub) {
    int count = 0;
    int i = s.indexOf(sub);
    while (i != -1) {
      count++;
      i = s.indexOf(sub, i+1);
      //System.out.println(i);
    }
    return count;
  }
//}}}

//{{{ explode
//##################################################################################################
    /**
    * Explodes a string, breaking it up into pieces defined by a delimiter character.
    *
    * @param src        string to act on
    * @param separator  the delimiter break the string on
    * @param keepEmpty  don't discard pieces that are empty strings
    *   (defaults to false when separator is whitespace, true otherwise)
    * @param trim       call String.trim() on pieces
    *   (defaults to true when separator is whitespace, false otherwise)
    * @return an array of Strings representing the pieces of <code>src</code>
    */
    public static String[] explode(String src, char separator, boolean keepEmpty, boolean trim)
    {
        ArrayList list = new ArrayList();
        int i, old_i, len = src.length();
        String piece;

        try
        {
            old_i = -1;
            for(i = src.indexOf(separator); i < len && i != -1; i = src.indexOf(separator, i+1))
            {
                piece = src.substring(old_i+1, i);
                if(trim) piece = piece.trim();
                if(piece.length() == 0 && !keepEmpty) {}
                else { list.add(piece); }
                old_i = i;
            }
            // Make sure we get the last piece!
            piece = src.substring(old_i+1);
            if(trim) piece = piece.trim();
            if(piece.length() == 0 && !keepEmpty) {}
            else { list.add(piece); }
        }
        catch(IndexOutOfBoundsException ex) {}

        return (String[])list.toArray(new String[list.size()]);
    }
    
    public static String[] explode(String src, char separator)
    {
        if(Character.isWhitespace(separator))   return explode(src, separator, false, true);
        else                                    return explode(src, separator, true, false);
    }
//}}}

//{{{ explodeInts, explodeDoubles
//##################################################################################################
    /** Explodes a string, then calls Integer.parseInt() on each fragment. */
    public static int[] explodeInts(String s, char separator) throws NumberFormatException
    {
        String[]    strings = explode(s, separator, false, true);
        int[]       ints    = new int[strings.length];
        for(int i = 0; i < strings.length; i++)
            ints[i] = Integer.parseInt(strings[i]);
        return ints;
    }
    
    /** Explodes a string, then calls Integer.parseInt() on each fragment. */
    public static double[] explodeDoubles(String s, char separator) throws NumberFormatException
    {
        String[]    strings = explode(s, separator, false, true);
        double[]    doubles = new double[strings.length];
        for(int i = 0; i < strings.length; i++)
            doubles[i] = Double.parseDouble(strings[i]);
        return doubles;
    }
//}}}

//{{{ expandVariables
//##################################################################################################
    /**
    * Expands placeholders into their values.
    * Very similar to java.text.MessageFormat.format(),
    * except without the weirdnesses.
    * Valid placeholders are {0}, {1}, etc. -- indexes into the array.
    * This isn't particularly efficient, so for big jobs, use regexes or something.
    * Expansion should be recursive.
    */
    static public String expandVariables(String template, String[] fillins)
    {
        for(int i = 0; i < fillins.length; i++)
        {
            String v = "{"+i+"}";
            int j = 0;
            while(true)
            {
                j = template.indexOf(v, j);
                if(j == -1) break;
                template = template.substring(0,j)
                    +fillins[i]
                    +template.substring(j+v.length());
            }
        }
        return template;
    }

    /**
    * Expands placeholders into their values.
    * This is like {@link #expandVariables(String, String[])}, except that instead
    * of looking for "{1}", "{2}", etc., we look for "{"+keys[0]+"}", "{"+keys[1]+"}", etc.
    * This isn't particularly efficient, so for big jobs, use regexes or something.
    * Expansion should be recursive.
    * @throws IllegalArgumentException if keys.length != fillins.length
    */
    static public String expandVariables(String template, String[] keys, String[] fillins)
    {
        if(keys.length != fillins.length)
            throw new IllegalArgumentException("Length of keys[] and fillins[] must match");
        
        for(int i = 0; i < fillins.length; i++)
        {
            String v = "{"+keys[i]+"}";
            int j = 0;
            while(true)
            {
                j = template.indexOf(v, j);
                if(j == -1) break;
                template = template.substring(0,j)
                    +fillins[i]
                    +template.substring(j+v.length());
            }
        }
        return template;
    }
//}}}

//{{{ compareVersions
//##################################################################################################
    /**
    * Compares two version strings with pieces separated by dots.
    * On a piece by piece basis, the comparison is numeric if both
    * pieces are integers, and by dictionary order otherwise.
    * @return less than, equal to, or greater than zero as
    *   ver1 is less than, equal to, or greater than ver2.
    */
    static public int compareVersions(String ver1, String ver2)
    {
        String[] v1 = explode(ver1, '.');
        String[] v2 = explode(ver2, '.');
        int len = Math.max(v1.length, v2.length);
        for(int i = 0; i < len; i++)
        {
            if(i > v1.length)       return -1;
            else if(i > v2.length)  return 1;
            try
            {
                int n1 = Integer.parseInt(v1[i]);
                int n2 = Integer.parseInt(v2[i]);
                if(n1 != n2) return n1 - n2;
            }
            catch(NumberFormatException ex)
            {
                int comp = v1[i].compareTo(v2[i]);
                if(comp != 0) return comp;
            }
        }
        return 0;
    }
//}}}

//{{{ usDecimalFormat
//##################################################################################################
    /**
    * Produces a DecimalFormat object in the en_US Locale, so it uses dots
    * for decimal separators, etc.
    * Otherwise, new DecimalFormat(...) may use commas as decimal points, etc.
    * See the Java Tutorial's Internationalization trail for more details.
    * @param pattern    a formatting string that follows the rules described
    *   in the javadoc for DecimalFormat (i.e. not a "localized" pattern).
    */
    static public DecimalFormat usDecimalFormat(String pattern)
    {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        if(nf instanceof DecimalFormat)
        {
            DecimalFormat df = (DecimalFormat) nf;
            df.applyPattern(pattern);
            return df;
        }
        else return new DecimalFormat(pattern); // oops!  do the best we can...
    }
//}}}

//{{{ arrayInParens
//##################################################################################################
    /**
    * Writes array of integers like this: "(1, 2, 3)"
    */
    static public String arrayInParens(int[] values)
    {
        String s = "(";
        for(int i = 0; i < values.length-1; i++)
            s += values[i] + ", ";
        s += values[values.length-1] + ")";
        return s;
    }

    /**
    * Writes array of doubles like this: "(931.238, 2.001, 13.000)"
    */
    static public String arrayInParens(double[] values)
    {
        DecimalFormat df = new DecimalFormat("###.###");
        String s = "(";
        for(int i = 0; i < values.length-1; i++)
            s += df.format(values[i]) + ", ";
        s += df.format(values[values.length-1]) + ")";
        return s;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

