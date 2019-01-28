// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import king.*;
import king.core.*;
import king.tool.util.*;

import java.util.*;
import java.text.*;
//}}}

public class Kinimol {

  //{{{ Constants
  static final DecimalFormat df = new DecimalFormat("0.000");
  static final DecimalFormat dfB = new DecimalFormat("0.00");
  //}}}
  
  //{{{ Variables
  //}}}
  
  //{{{ Constructors
  public Kinimol() {
  }
  //}}}

  //{{{ convertGrouptoPdb
  public static String convertGrouptoPdb(KGroup group, String pdbName) {
    String pdb = "";
    PointComparator pc = new PointComparator();
    int i = 1;
    TreeSet<KPoint> keyTree = new TreeSet<KPoint>(pc);
    KIterator<KPoint> points = KIterator.allPoints(group);
    for (KPoint point : points) { //sorts out the points (hopefully) into proper PDB order
      keyTree.add(point);
    }
    for (KPoint point : keyTree) {
      String atomName = PointComparator.getAtomName(point.getName().toUpperCase());
      String aaName = KinPointIdParser.getResName(point.getName().toUpperCase());
      pdb = pdb.concat("ATOM  ");
      pdb = pdb.concat(formatStrings(String.valueOf(i), 5) + " ");
      pdb = pdb.concat(atomName);
      pdb = pdb.concat(String.format("%4s",aaName) + "  ");
      pdb = pdb.concat(formatStrings(String.valueOf(KinPointIdParser.getResNumber(point.getName().toUpperCase())), 4) + "    ");
      pdb = pdb.concat(formatStrings(df.format(point.getX()), 8));
      pdb = pdb.concat(formatStrings(df.format(point.getY()), 8));
      pdb = pdb.concat(formatStrings(df.format(point.getZ()), 8));
      pdb = pdb.concat("  1.00");
      pdb = pdb.concat(formatStrings(dfB.format(KinPointIdParser.getBvalue(point)), 6));
      pdb = pdb.concat("                " + pdbName + "\n");
      i++;
    }
    return pdb;
  }
//}}}

/*  public void savePDB(File f) {
    try {
	    Writer w = new FileWriter(f);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	    //Set keys = adjacencyMap.keySet();
	    int i = 1;
	    PointComparator pc = new PointComparator();
	    TreeSet keyTree = new TreeSet(pc);
	    keyTree.addAll(adjacencyMap.keySet());
	    Iterator iter = keyTree.iterator();
	    while (iter.hasNext()) {
        AbstractPoint point = (AbstractPoint) iter.next();
        out.print("ATOM  ");
        //String atomNum = String.valueOf(i);
        //while (atomNum.length()<5) {
          //    atomNum = " " + atomNum;
        //}
        out.print(formatStrings(String.valueOf(i), 5) + " ");
        //out.print(point.getName().toUpperCase().substring(0, 8) + "  " + point.getName().toUpperCase().substring(8) + "     ");
        String atomName = PointComparator.getAtomName(point.getName().toUpperCase());
        if (atomName.equals("UNK ")) {
          
        }
        out.print(KinPointIdParser.getAtomName(point.getName().toUpperCase()) + " ");
        out.print(KinPointIdParser.getResAA(point.getName().toUpperCase()) + "  ");
        out.print(formatStrings(String.valueOf(KinPointIdParser.getResNumber(point.getName().toUpperCase())), 4) + "    ");
        out.print(formatStrings(df.format(point.getX()), 8));
        out.print(formatStrings(df.format(point.getY()), 8));
        out.print(formatStrings(df.format(point.getZ()), 8));
        out.println("  1.00  0.00");
        i++;
	    }
	    out.flush();
	    w.close();
    } catch (IOException ex) {
	    JOptionPane.showMessageDialog(kMain.getTopWindow(),
      "An error occurred while saving the file.",
      "Sorry!", JOptionPane.ERROR_MESSAGE);
    }
  }*/
  
  public static String formatStrings(String value, int numSpaces) {
    while (value.length() < numSpaces) {
	    value = " " + value;
    }
    return value;
  }
  
}
