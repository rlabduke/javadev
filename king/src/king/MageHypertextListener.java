// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

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
* <code>MageHypertextListener</code> is able to get events when the user
* selects a Mage-style *{hyperlink}* from the text window.
*
* @see UIText#addHypertextListener(MageHypertextListener)
* @see UIText#removeHypertextListener(MageHypertextListener)
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jul 16 11:47:37 EDT 2004
*/
public interface MageHypertextListener //extends ... implements ...
{
    /**
    * Called by UIText whenever the user selects any Mage-style
    * hyperlink, which is bracked by *{ and *}.
    * @param link   the text of the link, minus the flanking brackets
    */
    public void mageHypertextHit(String link);
}//class

