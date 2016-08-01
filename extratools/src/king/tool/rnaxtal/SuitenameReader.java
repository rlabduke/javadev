// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.rnaxtal;

import java.io.*;
import java.util.*;
import driftwood.data.*;
//}}}
public class SuitenameReader {
  
  //{{{ Constants
  //}}}

  //{{{ Variables
  UberMap suites = null;
  //}}}

  //{{{ Constructor
  public SuitenameReader() {
    suites = new UberMap();
  }
  //}}}

  //{{{ readFile
  public void readFile(File f) throws IOException {
    if(f != null && f.exists()) {
      BufferedReader reader = new BufferedReader(new FileReader(f));
      String line;
      String[] split = new String[6];
      while((line = reader.readLine())!=null&&(!line.startsWith(" all general case"))) {
        String[] testSplit = line.split(":");
        if (testSplit.length == 5) { // bug in suitename when file names > 32 char
          split[0] = testSplit[0].substring(0, 32);
          split[1] = testSplit[0].substring(32, testSplit[0].length());
          for (int i = 2; i < 6; i++) {
            split[i] = testSplit[i-1];
          }
        } else {
          split = testSplit;
        }
        //CNNNNITTT, suitename
        suites.put(split[2]+split[3]+split[4]+split[5].substring(0, 3), split[5].substring(9, 11));
      }
      Iterator iter = suites.keySet().iterator();
      while(iter.hasNext()) {
        String key = (String)iter.next();
        System.out.print(key+"=");
        System.out.println(suites.get(key));
        
      }
    }
  }
  //}}}

  //{{{ getConformerName
  /** 
  *   Takes a residue CNIT string as input.
  *   Returns the two character suite conformer name, or null if CNIT not found.
  */
  public String getConformerName(String cnit) {
    if (suites.containsKey(cnit)) {
      return (String) suites.get(cnit);
    }
    return null;
  }
  //}}}

}
