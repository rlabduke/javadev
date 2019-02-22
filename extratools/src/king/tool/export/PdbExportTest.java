// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;

import java.io.*;
import java.util.*;

import king.core.*;
import king.io.*;
import driftwood.util.*;

import static org.junit.Assert.*;
import org.junit.Test;

//}}}

public class PdbExportTest {
  
  public static Kinemage loadKinResource(String resource) throws IOException {
    KinfileParser parser = new KinfileParser();
    InputStream is = PdbExportTest.class.getResourceAsStream(resource);
    parser.parse(new LineNumberReader(new InputStreamReader(is)));
    is.close();
    Kinemage testKinemage = null;
    Iterator iter = parser.getKinemages().iterator();
    while (iter.hasNext()) {
      testKinemage = (Kinemage)iter.next();
    }
    return testKinemage;
  }
  
  public ArrayList<String> loadPdbResource(String resource) throws IOException {
    InputStream is = getClass().getResourceAsStream(resource);
    LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));
    ArrayList<String> pdbLines = new ArrayList<String>();
    String line="";
    while((line = lnr.readLine())!= null) {
      pdbLines.add(line);
    }
    is.close();
    return pdbLines;
  }
  
  public HashSet prepPdbLinesforCompare(ArrayList<String> pdbLines) {
    HashSet lineSet = new HashSet();
    for (String line : pdbLines) {
      if (line.startsWith("ATOM")) {
        String trimmedLine = line.substring(11, 66);
        //System.out.println(trimmedLine);
        lineSet.add(trimmedLine);
      }
    }
    //System.out.println(lineSet.size());
    return lineSet;
  }
  
  public HashSet checkElemsFromFirstInSecond(HashSet first, HashSet second) {
    HashSet secondCopy = new HashSet(second);
    secondCopy.removeAll(first);
    return secondCopy;
  }
  
  public String makeErrorOutputString(HashSet comparisonSet, String sourceSetName) {
    String outputString = "Unexpected extra lines in "+sourceSetName+" PDB:\n";
    Iterator iter = comparisonSet.iterator();
    int count = 0;
    while (iter.hasNext()&&count < 100) {
      outputString = outputString+(String)iter.next()+"\n";
      count++;
    }
    if (comparisonSet.size() > 100) {
      outputString = "Too many unexpected extra lines ("+comparisonSet.size()+") in "+sourceSetName+" PDB to print!  Examples:\n"+outputString;
    }
    return outputString;
  }
  
  public void testFileExport(String kinName, String pdbName) throws IOException {
    Kinemage testKin = loadKinResource(kinName);
    ArrayList<String> pdbLines = loadPdbResource(pdbName);
    System.out.println(pdbLines.size());
    ArrayList<String> exportPdbLines = new ArrayList<String>(Arrays.asList(PdbExport.getPdbText(testKin).split("\n")));
    System.out.println(exportPdbLines.size());
    HashSet editedRefPdbLines = prepPdbLinesforCompare(pdbLines);
    HashSet generatedPdbLines = prepPdbLinesforCompare(exportPdbLines);
    HashSet generatedRemainder = checkElemsFromFirstInSecond(editedRefPdbLines, generatedPdbLines);
    HashSet referenceRemainder = checkElemsFromFirstInSecond(generatedPdbLines, editedRefPdbLines);
    assertTrue(makeErrorOutputString(generatedRemainder, "generated"), generatedRemainder.isEmpty());
    assertTrue(makeErrorOutputString(referenceRemainder, "reference"), referenceRemainder.isEmpty());
    assertTrue(editedRefPdbLines.equals(generatedPdbLines));
  }
  
  @Test
  public void testExports() throws IOException {
    testFileExport("404dH.kin", "404dH.pdb");
    testFileExport("3sgbH.kin", "3sgbH.pdb");


  }
}