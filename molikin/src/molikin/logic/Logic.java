// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin.logic;

import java.util.*;
import java.io.*;
import driftwood.moldb2.*;
//}}}
/**
* <code>Logic</code> is a simple interface for the other logic objects.
*
* <p>Copyright (C) 2009 by Vincent B. Chen. All rights reserved.
* <br>Begun on Mon Feb 09 15:45:44 EST 2009
*/
public interface Logic {
  
  public void printKinemage(PrintWriter out, Model m, Set residues, String pdbId, String bbColor);
  
}
