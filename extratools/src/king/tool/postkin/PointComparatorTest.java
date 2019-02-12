// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.tool.util.*;
import king.*;
import king.io.*;
import king.core.*;
import king.points.*;
import java.io.*;
import java.util.*;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

//}}}

public class PointComparatorTest {
  
  String testKinString = String.join("\n", 
"@kinemage",
"@group {6BUVW.amber_001_ed_nomodbase_2a} dominant animate",
"@subgroup {amber_001_ 1} dominant master= {chain  B} off",
"@vectorlist {mc} color= white master= {mainchain}",
"{ n   ala B  43  B98.05 6BUVW.amber_00}P , 38.799, 80.140, 55.562",
"{ ca  ala B  43  B86.29 6BUVW.amber_00}, 40.073, 80.775, 55.157",
"{ n   leu B  44  B79.65 6BUVW.amber_00}, 40.839, 81.925, 53.065",
"{ c   leu B  44  B77.76 6BUVW.amber_00}, 41.334, 80.720, 50.873",
"{ c   leu C  44  B77.76 6BUVW.amber_00}P , 41.334, 80.720, 50.873",
"{ c   aleu B  44  B77.76 6BUVW.amber_00}, 41.334, 80.720, 50.873",
"{ c   bleu B  44  B77.76 6BUVW.amber_00}, 41.334, 80.720, 50.873");

  @Test
  public void testCompareProtein() {
    Kinemage testKinemage = getTestKinemage();
    KIterator<KPoint> testPoints = KIterator.allPoints(testKinemage);
    KPoint n43 = testPoints.next();
    KPoint ca43 = testPoints.next();
    KPoint n44 = testPoints.next();
    KPoint cB44 = testPoints.next();
    KPoint cC44 = testPoints.next();
    KPoint ac44 = testPoints.next();
    KPoint bc44 = testPoints.next();
    PointComparator pc = new PointComparator();
    assertEquals(pc.compare(n43, n43), 0);
    assertEquals(pc.compare(n43, ca43), -1);
    assertEquals(pc.compare(ca43, n43), 1);
    assertEquals(pc.compare(n43, n44), -1);
    assertEquals(pc.compare(cB44, cC44), -1);
    assertEquals(pc.compare(ac44, bc44), -1);
  }
  
  public Kinemage getTestKinemage() {
    KinfileParser parser = new KinfileParser();
    try {
      parser.parse(new LineNumberReader(new StringReader(testKinString)));
    } catch (IOException ie) {
    }
    Kinemage testKinemage = null;
    Iterator iter = parser.getKinemages().iterator();
    while (iter.hasNext()) {
      testKinemage = (Kinemage)iter.next();
    }
    return testKinemage;
  }
  
}