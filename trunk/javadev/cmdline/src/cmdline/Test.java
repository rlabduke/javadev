// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import driftwood.r3.*;
import driftwood.moldb2.*;
import chiropraxis.rotarama.*;
import java.io.*;
import java.util.*;
//}}}

public class Test {

  //{{{ main
  public static void main(String[] args) {
    Test t = new Test();
  }
  //}}}
  
  //{{{ Constructor
  public Test() {
    String structureId = "1BIA";
    File infile = new File(structureId + ".pdb");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    InputStream is = null;
    java.io.BufferedOutputStream bs = null;
    try
    {
      is = new BufferedInputStream(new FileInputStream(infile));
      Ramalyze.runAnalysis(is, baos, Ramalyze.MODE_PDF );
      is.close();
      is = null;
      File of = new File(structureId + ".tst.pdf");
      bs = new BufferedOutputStream(new java.io.FileOutputStream(of));
      byte[] pic = baos.toByteArray ();
      bs.write(pic);
      bs.close();
      bs = null;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      if (is != null)
      {
        try
        {
          is.close();
        }
        catch (IOException e1)
        {
          e1.printStackTrace();
        }
      }
      if (bs != null)
      {
        try
        {
          bs.close();
        }
        catch (IOException e1)
        {
          e1.printStackTrace();
        }
      }
    }
  }
  //}}}
  
}
