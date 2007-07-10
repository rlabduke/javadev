// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.mysql;

import java.sql.*;
//import com.mysql.jdbc.*;
//}}}
/** 
* DatabaseManager is used for interfacing with the mysql database located on spiral (071007).
* It uses the mysql connector j 5.0.6 downloaded from the mysql website.  Any packages that use
* this code will have to include a copy of the mysql jar in the classpath when compiling and running
* this code.  
*/

public class DatabaseManager {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variables
  Connection conn;
  ResultSet rs;
  //}}}
  
  //{{{ Constructor
  public DatabaseManager() {
    try {
      Class.forName("com.mysql.jdbc.Driver").newInstance();
    } catch (ClassNotFoundException ce) {
      System.err.println("MySql jdbc driver was not found!");
    } catch (InstantiationException ie) {
      System.err.println("An instantiation exception occurred in DatabaseManager.java!");
    } catch (IllegalAccessException iae) {
      System.err.println("An error occurred in DatabaseManager, check your permissions!");
    }
  }
   //}}}
  
  //{{{ connectToDatabase
  public void connectToDatabase(String url) {
    try {
      conn = DriverManager.getConnection("jdbc:mysql:" + url, "vbc3", "mypig.v");
    } catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }
  }
  //}}}
  
  //{{{ select
  public void select(String selectQuery) {
    Statement stmt = null;
    rs = null;
    try {
      stmt = conn.createStatement();
      rs = stmt.executeQuery(selectQuery);
      //return rs;
      //while (rs.next()) {
      //  System.out.print(rs.getString("pdb_id") + " ");
      //  System.out.println(rs.getString(2));
        //rs.getInt("frag_length");
      //}
      
    } catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }/* finally {
      // it is a good idea to release resources in a finally{} block
      // in reverse-order of their creation if they are no-longer needed
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException sqlEx) {
        }
        rs = null;
      }
      
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException sqlEx) {
        }
        stmt = null;
      }
    }
    //return null;
    */
  }
  //}}}
    
  //{{{ getString
  public String getString(int i) {
    try {
      return rs.getString(i);
    } catch (SQLException ex) {
      System.out.println("SQLException in getString: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }
    return null;
  }
  //}}}
  
  //{{{ next
  public boolean next() {
    if (rs != null) {
      try {
        return rs.next();
      } catch (SQLException ex) {
        System.out.println("SQLException: " + ex.getMessage());
        System.out.println("SQLState: " + ex.getSQLState());
        System.out.println("VendorError: " + ex.getErrorCode());
      }
    }
    return false;
  }
  //}}}
  
  //{{{ reset
  public void reset() {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException sqlEx) {
      }
      rs = null;
    }
  }
  //}}}

  //{{{ close
  public void close() {
    try {
      reset();
      conn.close();
    } catch (SQLException ex) {
      System.out.println("SQLException in getString: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }
  }
  //}}}
  
}
