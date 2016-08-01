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
  boolean fromMAD;
  boolean justQueryNoSuperimpose;
  
  //}}}

//{{{ Constructor
//###############################################################
  public MySqlLiaison()
  {
	  fromMAD = false;
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
          else if (arg.equals("-mad") || arg.equals("-MAD"))
          {
              fromMAD = true;
          }
          else if (arg.equals("-justquery"))
          {
              System.out.println("Just an SQL query this time (no superimposition) ...");
              justQueryNoSuperimpose = true;
          }
          // Else, if it's any other arg that starts with "-", it's a flag 
          // which we will pass on to MultiPdbSuperimposer
      }
  }
  
  public void interpretArg(String arg)
  {
      if (arg.indexOf(".sql") > 0)
      {
          System.out.println("Your SQL query filename: "+arg);
          sqlFilename = arg;
      }
      else
      {
          System.out.println("Your outprefix:          "+arg);
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
      
      /*
      // Superimposed kin written here
      if (!justQueryNoSuperimpose)
      {
          if (fromMAD)
              MultiMADSuperimposer.main(argsToPass);
          else
              MultiPdbSuperimposer.main(argsToPass);
      }
      */
  }
//}}}

//{{{ performMySqlQuery
  public void performMySqlQuery() 
  {
      // Prep the query
      File sqlFile = new File(sqlFilename);
      ArrayList<String> sqlSelectLines = new ArrayList<String>();
      try 
      {
          Scanner s = new Scanner(sqlFile);
          String line = "";
          String lineTrimmed = "";
          while (s.hasNextLine())
          {
              line = s.nextLine();
              lineTrimmed = line.trim();
              if (lineTrimmed.length() != 0 && lineTrimmed.indexOf("--") != 0) 
              {
                  // it's not a blank line or a comment
                  sqlSelectLines.add(line);
              }
          }
      }
      catch (FileNotFoundException e)
      {
          System.out.println("Cannot find sql query file");
      }
      
      // Get # columns SELECTed in query
      int indexSELECT = 0;
      int indexFROM = 0;
      for (int i = 0; i < sqlSelectLines.size(); i ++)
      {
          String line = sqlSelectLines.get(i);
          if ( (line.trim()).indexOf("SELECT") >= 0)    indexSELECT = i;
          if ( (line.trim()).indexOf("FROM"  ) >= 0)    indexFROM   = i;
      }
      int numColumns = 0;
      for (int i = indexSELECT; i < indexFROM; i ++)
      numColumns ++;
      System.out.println("numColums: "+numColumns);
      
      // Make single-String query
      String sqlSelect = "";
      for (String line : sqlSelectLines)
      {
          sqlSelect = sqlSelect+" "+line;
          System.out.println(line);
      }
      
      // Contact the database and perform query
      DatabaseManager dm = new DatabaseManager();
      dm.connectToDatabase("//spiral.research.duhs.duke.edu/neo500");
      dm.select(sqlSelect);
      
      System.out.println("** Done **");
      
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
              //String lineToPrint = dm.getString(1)+" "+dm.getString(2)+" "+dm.getString(3)+" ";	// column 1 = pdb_id
                                                        // column 2 = chain_id
                                                        // column 2 = res_num
              String lineToPrint = "";
              for (int i = 1; i <= numColumns; i ++)
                  lineToPrint += dm.getString(i)+";";
              
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
