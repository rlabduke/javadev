// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fragmentfiller;

import java.util.*;
import driftwood.r3.*;
import driftwood.moldb2.*;
//}}}

public interface Filler {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  //}}}
  
  //public void searchDB(ArrayList list);
  
  public CoordinateFile[] getFragments(PdbLibraryReader libReader);
  
  public Tuple3[] getTupleArray(ArrayList<Triple> states);
  
}
