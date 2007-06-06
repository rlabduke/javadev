// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

public class Main {
  
  //{{{ main
  //###############################################################
  public static void main(String[] args) {
    String[] cmdargs = new String[args.length - 1];
    for (int i = 0; i < cmdargs.length; i++) {
      cmdargs[i] = args[i + 1];
    }
    if (args[0].equals("libraryfilterer")) {
      LibraryFilterer.main(cmdargs);
    } else if (args[0].equals("pdbsuperimposer")) {
      PdbSuperimposer.main(cmdargs);
    } else {
      System.err.println("Unknown cmdline function!");
    }
  }
  //}}}
  
  //{{{ Constructor
  public Main() {
  }
  //}}}
  
}
