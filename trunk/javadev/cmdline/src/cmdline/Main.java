// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import cmdline.fragment.*;
//}}}

public class Main {
  
  //{{{ main
  //###############################################################
  public static void main(String[] args) {
    if (args.length > 0) {
      String[] cmdargs = new String[args.length - 1];
      for (int i = 0; i < cmdargs.length; i++) {
        cmdargs[i] = args[i + 1];
      }
      if (args[0].equals("libraryfilterer")) {
        LibraryFilterer.main(cmdargs);
      } else if (args[0].equals("pdbsuperimposer")) {
        PdbSuperimposer.main(cmdargs);
      } else if (args[0].equals("calcrmsd")) {
        ParameterCalcRmsd.main(cmdargs);
      } else if (args[0].equals("pdbsuperimposer_RNA")) {
        PdbSuperimposer_RNA.main(cmdargs);
      /*} else if (args[0].equals("multipdbsuperimposer")) {
        MultiPdbSuperimposer.main(cmdargs);
      } else if (args[0].equals("multimadsuperimposer")) {
        MultiMADSuperimposer.main(cmdargs);*/
      } else if (args[0].equals("sql")) {
        MySqlLiaison.main(cmdargs);
      } else if (args[0].equals("fragmentrotator")||(args[0].equals("fragrot"))) {
        FragmentRotator.main(cmdargs);
      } else if (args[0].equals("sequenceanalyzer")||(args[0].equals("seqan"))) {
        SequenceAnalyzer.main(cmdargs);
      } else if (args[0].equals("fraglibcreator")) {
        FragmentLibraryCreator.main(cmdargs);
      } else if (args[0].equals("fragparamwriter")) {
        FragmentParameterWriter.main(cmdargs);
      } else if (args[0].equals("test")) {
        Test.main(cmdargs);
      } else {
        System.err.println("Unknown cmdline function!");
        System.err.println("Try one of the following:");
        System.err.println("libraryfilterer | pdbsuperimposer | calcrmsd | pdbsuperimposer_RNA |");
        System.err.println("multipdbsuperimposer | multimadsuperimposer | sql | fragmentrotator |");
        System.err.println("sequenceanalyzer | fraglibcreator | fragparamwriter| test");
      }
    } else {
      System.err.println("No cmdline function specified!");
      System.err.println("Try one of the following:");
      System.err.println("libraryfilterer | pdbsuperimposer | calcrmsd | pdbsuperimposer_RNA |");
      System.err.println("multipdbsuperimposer | multimadsuperimposer | sql | fragmentrotator |");
      System.err.println("sequenceanalyzer | fraglibcreator | fragparamwriter | test");
    }
  }
  //}}}
  
  //{{{ Constructor
  public Main() {
  }
  //}}}
  
}
