// (jEdit options) :folding=explicit:collapseFolds=1:
package cmdline.fragment;

import java.io.*;

public class LibraryWriter {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  Writer w;
  PrintWriter out;
  //}}}
  
  //{{{ Constructor
  public LibraryWriter(File lib) {
    try {
      //System.out.println(saveFile.getCanonicalPath());
      if (!lib.exists()) {
        lib.createNewFile();
      }
      w = new FileWriter(lib);
      out = new PrintWriter(new BufferedWriter(w));
      //out.print(text);
      //out.flush();
      //w.close();
    } catch (IOException ie) {
      System.out.println("Error when writing lib file!");
      ie.printStackTrace();
    }
  }
  //}}}
  
  //{{{ write
  public void write(String text) {
    out.print(text);
    //out.flush();
  }
  //}}}
  
  //{{{ close
  public void close() {
    try {
      out.flush();
      w.close();
    } catch (IOException ie) {
      System.out.println("Error when writing lib file!");
      ie.printStackTrace();
    }
  }
  //}}}
  
}
