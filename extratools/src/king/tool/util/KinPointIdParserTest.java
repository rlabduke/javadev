// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.util;

import king.tool.export.*;
import king.tool.postkin.*;
import king.core.*;

import java.io.*;

import static org.junit.Assert.*;
import org.junit.Test;
//}}}

public class KinPointIdParserTest {
  
  
  @Test
  public void testGetMethods() throws IOException {
    PointComparator comparator = new PointComparator();
    Kinemage testKin = PdbExportTest.loadKinResource("3sgbH.kin");
    KIterator<KPoint> allPoints = KIterator.allPoints(testKin);
    for (KPoint testPoint : allPoints) {
      assertTrue(comparator.getAtomName(testPoint.getName()).length() == 4);
      assertTrue(KinPointIdParser.getAltConf(testPoint.getName()).length() == 1);
      assertTrue(KinPointIdParser.getResName(testPoint.getName()).length() == 3);
    }
    
  }
}