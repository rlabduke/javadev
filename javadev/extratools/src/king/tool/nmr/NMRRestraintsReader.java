// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.nmr;

import java.io.*;
import java.util.*;

import driftwood.util.*;
//}}}

/**
* <code>NMRRestraintsReader</code> is intended to read NMR restraints files (.mr) files.  It probably
* only works with .mr files from certain programs.
* 
* <br>Begun Tue Nov 06 13:26:49 EST 2007
**/

public class NMRRestraintsReader {
  
  //{{{ Constants
  public static final String  DIPOLAR   = "dipolar";
  //}}}
  
  //{{{ Variables
  TreeMap restraintsMap = null; // String of restraint type -> list of assigns.
  MagneticResonanceFile mrFile = null;
  //}}}
  
  
  /**
  * Basic constructor for NMRRestraintsReader
  */
  public NMRRestraintsReader() {
    restraintsMap = new TreeMap();
    mrFile = new MagneticResonanceFile();
  }
    
  //{{{ openFile
  //##################################################################################################
  //public void openFile()
  //{
  //  // Create file chooser on demand
  //  if(filechooser == null) makeFileChooser();
  //  
  //  if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
  //  {
	//    try {
  //      File f = filechooser.getSelectedFile();
  //      if(f != null && f.exists()) {
  //        //dialog.setTitle(f.getName());
  //        BufferedReader reader = new BufferedReader(new FileReader(f));
  //        scanFile(reader);
  //        reader.close();
  //        
  //        
  //      }
	//    } catch (IOException ie) {
  //      JOptionPane.showMessageDialog(kMain.getTopWindow(),
  //      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
  //      "Sorry!", JOptionPane.ERROR_MESSAGE);
	//    }
  //  }
  //}
  //}}}

  //{{{ scanFile
  //##################################################################################################
  /**
  * Does most of the work reading and analyzing the data files.
  **/
  public void scanFile(File f) throws IOException {
    if(f != null && f.exists()) {
      
      BufferedReader reader = new BufferedReader(new FileReader(f));
      String currentAssign = null;
      ArrayList currentList = null;
      String line;
      while((line = reader.readLine())!=null) {
        line = line.trim();
        if ((line.length() > 2)&&(!line.startsWith("!"))) {
          if (line.substring(0,2).matches("[A-Z]\\.")) {
            // new section of restraints
            if (currentAssign != null) currentList.add(currentAssign);
            System.out.print(line + " ");
            currentList = new ArrayList();
            restraintsMap.put(line, currentList);
          } else if (line.startsWith("assign")) {
            // new restraint
            if (currentAssign != null) {
              //System.out.println(currentAssign);
              currentList.add(currentAssign);
              currentAssign = line;
            } else {
              currentAssign = line;
            }
          } else if (currentAssign != null) {
            //System.out.println("currentAssign is not null " + line);
            currentAssign = currentAssign.concat(line);
          }
        } else {
          //System.out.println("line is false " + line);
          if (currentAssign != null) currentList.add(currentAssign);
          currentAssign = null;
        }
        //String[] strings = Strings.explode(line, delimiter.charAt(0), false, true);
        //allPoints.add(strings);
      }
      reader.close();        
    }
  }
  //}}}
  
  //{{{ analyzeFileContents
  public MagneticResonanceFile analyzeFileContents() {
    Iterator keys = restraintsMap.keySet().iterator();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      if (key.indexOf("DIPOLAR") > -1) readDipolarCouplings((ArrayList) restraintsMap.get(key));
    }
    return mrFile;
  }
  //}}}
  
  //{{{ readDipolarCouplings
  public void readDipolarCouplings(ArrayList assigns) {
    Iterator iter = assigns.iterator();
    while (iter.hasNext()) {
      String rdc = (String) iter.next();
      //System.out.println(rdc);
      String[] pieces = Strings.explode(rdc, "(".charAt(0), false, true);
      // 5 and 6 should have the important stuff (atom name, #, and values)
      String resFrom = pieces[5].substring(0, pieces[5].indexOf(")"));
      String resTo = pieces[6].substring(0, pieces[6].indexOf(")"));
      String values = pieces[6].substring(pieces[6].indexOf(")")+1, pieces[6].length());
      String fromNum = resFrom.substring(7, 11);
      String fromName = resFrom.substring(21, 25);
      String toNum = resTo.substring(7, 11);
      String toName = resTo.substring(21, 25);
      double[] vals = Strings.explodeDoubles(values, " ".charAt(0));
      System.out.println(fromNum + ":" + fromName + ":" + toNum + ":" + toName + ":" + vals[0]);
      DipolarRestraint dr = new DipolarRestraint(fromName, fromNum, toName, toNum, vals);
      mrFile.addDipolarCoupling(dr);
    }
  }
  //}}}
}