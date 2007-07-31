// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import molikin.logic.*;
import driftwood.gui.*;
import driftwood.r3.*;
import driftwood.util.*;
import driftwood.data.*;
import driftwood.util.SoftLog;
import driftwood.moldb2.*;
import driftwood.mysql.*;
import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.*;
import java.util.zip.*;
import java.sql.*;

//}}}

/**
* <code>MySqlLiaison</code> is from Vince...FILL IN TEXT LATER
*
* <p>Copyright (C) 2007 by Daniel Keedy. All rights reserved.
* 
*/
public class MySqlLiaison
{
//{{{ Variable definitions
  
  static String sqlFilename;
  static String outPrefix;
  static String[] args;
  
  //}}}

//{{{ Constructor
//###############################################################
  public MySqlLiaison()
  {
	  ;
  }
//}}}

//{{{ main
//###############################################################
  public static void main(String[] cmdargs)
  {
	  args = cmdargs;
      System.out.println("");
      
	  MySqlLiaison liaison = new MySqlLiaison();
	  liaison.parseArgs();
	  liaison.run();
  }
//}}}

//{{{ parseArgs, interpretArg
//###############################################################
  public void parseArgs()
  {
	  if (args.length < 2)
	  {
		  System.out.println("Not enough arguments: you must have (1) a mysql"
			  +" query file and (2) a prefix for the output kin(s)!");
	  }
      
      String arg;
      for (int i = 0; i < args.length; i ++)
      {
          arg = args[i];
          if (!arg.startsWith("-"))
          {
              // Probably either the sql query filename or the output prefix
              interpretArg(arg);
          }
          // else if (arg.startsWith("-"))
              // It's a flag, which we will pass on to MultiPdbSuperimposer
      }
  }
  
  public void interpretArg(String arg)
  {
      if (arg.indexOf(".sql") > 0)
      {
          System.out.println("sqlFilename:          "+arg);
          sqlFilename = arg;
      }
      else
      {
          System.out.println("outPrefix:            "+arg);
          outPrefix = arg;
      }
  }
//}}}

//{{{ run
//###############################################################
  public void run()
  {
	performMySqlQuery();
	
	// Add one extra arg, outPrefix+".csv", so MultiPdbSuperimposer
    // knows where to look for query results
    String[] argsToPass = new String[args.length+1];
    for (int i = 0; i < args.length; i ++)
        argsToPass[i] = args[i];
    argsToPass[args.length] = outPrefix+".csv";
	
    // Superimposed kin written here
	MultiPdbSuperimposer.main(argsToPass);
  }
//}}}

//{{{ performMySqlQuery
    public void performMySqlQuery() 
    {
      // Prep the query
      String sqlSelect = "";
      File sqlFile = new File(sqlFilename);
      try 
      {
                  Scanner s = new Scanner(sqlFile);
          String line = "";
          String lineTrimmed = "";
          while (s.hasNextLine())
          {
              line = s.nextLine();
              lineTrimmed = line.trim();
              if (lineTrimmed.length() != 0 && 
                  lineTrimmed.indexOf("--") < 0) 
              {
                  // it's not a blank line or a comment
                  sqlSelect = sqlSelect+" "+line;
              }
          }
          //System.out.println(sqlSelect);
      }
      catch (FileNotFoundException e)
      {
          System.out.println("Cannot find sql query file");
      }
      
      // Contact the database and perform query
      DatabaseManager dm = new DatabaseManager();
      dm.connectToDatabase("//spiral.research.duhs.duke.edu/neo500");
      dm.select(sqlSelect);
      
      // Write query results to a file
      File temp = new File(outPrefix+".csv");
      String tempAbsPath = temp.getAbsolutePath();
      int tempAbsPathLength = tempAbsPath.length();
      String outPrefixAbsPath = tempAbsPath.substring(0, tempAbsPathLength-4);   // to cut off the .csv I just added
      File sqlResults = new File(outPrefixAbsPath+".csv");
      try
      {
          PrintWriter out = new PrintWriter(sqlResults);
          while (dm.next())
          {
              //listOfMatches.add(dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3));
              String lineToPrint = dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" ";	// column 1 = pdb_id
                                                        // column 2 = chain_id
                                                        // column 2 = res_num
              out.println(lineToPrint);
              //System.out.println(lineToPrint);
          }
          out.close();
      }
      catch (IOException ie)
      {
          System.out.println("Error writing sql results file");
      }
      System.out.println("sql results file:     "+sqlResults.getPath()+" has been written");
      
      // Next, MultiPdbSuperimposer will access this file
}
//}}}

}//class
