// (jEdit options) :folding=explicit:collapseFolds=1:
package cmdline;

import java.io.*;
import java.util.*;
import java.text.*;

public class ParameterCalcRmsd {

  //{{{ Constants
  DecimalFormat df = new DecimalFormat("0.000");
  //}}}
  
  //{{{ Variables
  ArrayList<Double[]> allParams;
  //}}}
  
  //{{{ main
  public static void main(String[] args) {
    long startTime = System.currentTimeMillis();
    if (args.length == 0) {
	    System.out.println("No files were specified!");
    } else {
	    //File[] inputs = new File[args.length];
	    //for (int i = 0; i < args.length; i++) {
      //  inputs[i] = new File(System.getProperty("user.dir") + "/" + args[i]);
      //  System.out.println(inputs[i]);
	    //}
	    //File outFile = new File(System.getProperty("user.dir") + "/" + args[0] + ".rmsd.txt");
      File input = new File(System.getProperty("user.dir") + "/" + args[0]);
      File output = new File(System.getProperty("user.dir") + "/" + args[1]);
	    ParameterCalcRmsd filterer = new ParameterCalcRmsd(input, output);
    }
    long endTime = System.currentTimeMillis();
    System.out.println((endTime - startTime)/1000 + " seconds to generate rmsds");
  }
  //}}}
  
  //{{{ Constructors
  public ParameterCalcRmsd(File f, File outFile) {
    allParams = new ArrayList<Double[]>();
    scanFile(f);
    doAllonAll(outFile);
  }
  //}}}
  
  
  //{{{ scanFile
  public void scanFile(File f) {
    if(f != null && f.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line;
        while((line = reader.readLine())!=null){
          line = line.trim();
          String[] frameInfo = line.split(" ");
          Double[] params = new Double[frameInfo.length];
          for (int i = 0; i < frameInfo.length; i++) {
            params[i] = Double.valueOf(frameInfo[i]);
          }
          allParams.add(params);
        }
      } catch (IOException ie) {
        System.err.println(ie.toString());
      }
    }
  }
  //}}}
  
  //{{{ doAllonAll
  public void doAllonAll(File outFile) {
    try {
	    Writer w = new FileWriter(outFile);
	    PrintWriter out = new PrintWriter(new BufferedWriter(w));
      for (int j = 0; j < allParams.size(); j++) {
        Double[] params = allParams.get(j);
        for (int i = 0; i < allParams.size(); i++) {
          if (i != j) {
            Double[] compareTo = allParams.get(i);
            double rmsd = -calcRmsd(params, compareTo);
            out.println((j+1) + " " + (i+1) + " " + df.format(rmsd));
          }
        }
      }
      out.flush();
	    w.close();
    } catch (IOException ex) {
	    System.err.println("An error occurred while saving the file.");
    }
  }
  //}}}
  
  //{{{ calcRmsd
  public double calcRmsd(Double[] first, Double[] compareTo) {
    double sum = 0;
    for (int i = 0; i < first.length; i++) {
      sum = sum + Math.pow(first[i] - compareTo[i], 2);
      //sum = sum + Math.sqrt((Math.pow((refpoint.getX() - modpoint.getX()), 2) + Math.pow((refpoint.getY() - modpoint.getY()), 2) + Math.pow((refpoint.getZ() - modpoint.getZ()), 2)));
    }
    double rmsd = Math.sqrt(sum/first.length);
    return rmsd;
  }
  //}}}
}
