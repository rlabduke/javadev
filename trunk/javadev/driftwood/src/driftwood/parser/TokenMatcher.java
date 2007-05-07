// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.parser;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>TokenMatcher</code> describes the minimal requirements for a lexer (tokenizer).
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri May  4 08:18:49 EDT 2007
*/
public interface TokenMatcher //extends ... implements ...
{
    /**
    * Returns true iff a valid token starts at position <code>start</code>
    * in the given character sequence.
    * After a successful result, end() and token() can be called for details.
    * <p>This function may return true but generate a null token iff afterwards
    * <code>this.end() == s.length()</code> and
    * <code>match(s, end()) == false</code>.
    * This basically allows for one null token representing the final ignorable
    * whitespace in a file.
    */
    public boolean match(CharSequence s, int start);
    
    /**
    * If the last match() was successful, returns the index of the end of
    * the token (exclusive) -- that is, the position to be used as the next start.
    */
    public int end();
    
    /**
    * If the last match() was successful, returns the (possibly normalized)
    * token that was recognized.  Because the token may be normalized and
    * whitespace/comments/etc may be skipped, the length of the token
    * is not guaranteed to equal <code>end() - start</code>.
    * This function *may* return null, see <code>match()</code>.
    */
    public String token();
}//class

