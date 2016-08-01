// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

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
* <code>KinLoadListener</code> is an interface for monitoring
* the progress of a kinemage that's being loaded in the
* background. All these methods are called from the
* Swing event-handling thread.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Apr 11 10:07:14 EDT 2003
*/
public interface KinLoadListener //extends ... implements ...
{
    /**
    * Messaged periodically as the parser reads the file.
    */
    public void updateProgress(long charsRead);
    
    /**
    * Messaged if anything is thrown during the loading process.
    * This generally means loadingComplete() won't be called.
    */
    public void loadingException(Throwable t);
    
    /**
    * Messaged if and when loading finished successfully.
    */
    public void loadingComplete(KinfileParser parser);
}//class

