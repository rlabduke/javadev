// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.util;

//import java.awt.*;
//import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import javax.swing.*;
//import gnu.regexp.*;
//}}}
/**
* <code>Props</code> extends java.util.Properties with a few convenience functions for reading integers, booleans, etc.
* Notice that the single-argument versions of getXxx() throw NoSuchElementExceptions if the property can't be found.
* It has also been extended to allow single/double quoting of property values (via getString()),
* so as to preserve leading/trailing whitespace for properties where it's meaningful.
*
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
* <p>Begun on Mon Jul  1 15:31:04 EDT 2002
*/
public class Props extends Properties // implements ...
{
//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public Props()
    {
        super();
    }

    /**
    * Constructor
    */
    public Props(Properties defaults)
    {
        super(defaults);
    }
//}}}

//{{{ get/setDefaults
//##################################################################################################
    /**
    * Retrieves the Properties object that supplies values to this one
    * if this one can't find the requested property in its table.
    * Returns null if there is no such object.
    */
    public Properties getDefaults()
    {
        return this.defaults;
    }
    
    /**
    * Sets the Properties object that will supply default values to this one.
    * Passing null will cause this object to not refer to anything for defaults.
    */
    public void setDefaults(Properties def)
    {
        this.defaults = def;
    }
//}}}

//{{{ hasProperty()
//##################################################################################################
    /**
    * Returns true iff this Props object or one of its parents
    * contains a non-null value mapped to the specified key.
    */
    public boolean hasProperty(String key)
    {
        return (getProperty(key) != null);
    }
//}}}

//{{{ getString()
//##################################################################################################
    /** An alias for getProperty() that trims off leading and trailing quote marks*/
    public String getString(String key, String defVal)
    {
        String ret = getProperty(key);
        if(ret == null) ret = defVal;
        else if(ret.startsWith("\"") && ret.endsWith("\"")) ret = ret.substring(1, ret.length()-1);
        else if(ret.startsWith("'") && ret.endsWith("'"))   ret = ret.substring(1, ret.length()-1);
        return ret;
    }

    /** An alias for getProperty() that trims off leading and trailing quote marks*/
    public String getString(String key)
    {
        String ret = getProperty(key);
        if(ret == null) throw new NoSuchElementException("Property '"+key+"' could not be found");
        else if(ret.startsWith("\"") && ret.endsWith("\"")) ret = ret.substring(1, ret.length()-1);
        else if(ret.startsWith("'") && ret.endsWith("'"))   ret = ret.substring(1, ret.length()-1);
        return ret;
    }
//}}}

//{{{ getInt()
//##################################################################################################
    public int getInt(String key, int defVal)
    {
        int ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            try { ret = Integer.parseInt(prop); } catch(NumberFormatException ex) {}
        }
        return ret;
    }

    public int getInt(String key)
    {
        int ret = 0;
        String prop = getProperty(key);
        if(prop == null) throw new NoSuchElementException("Property '"+key+"' could not be found");
        try { ret = Integer.parseInt(prop); }
        catch(NumberFormatException ex)
        {
            NoSuchElementException nsee = new NoSuchElementException("Property '"+key+"' is not an integer");
            nsee.initCause(ex);
            throw nsee;
        }
        return ret;
    }
//}}}

//{{{ getLong()
//##################################################################################################
    public long getLong(String key, long defVal)
    {
        long ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            try { ret = Long.parseLong(prop); } catch(NumberFormatException ex) {}
        }
        return ret;
    }

    public long getLong(String key)
    {
        long ret = 0;
        String prop = getProperty(key);
        if(prop == null) throw new NoSuchElementException("Property '"+key+"' could not be found");
        try { ret = Long.parseLong(prop); }
        catch(NumberFormatException ex)
        {
            NoSuchElementException nsee = new NoSuchElementException("Property '"+key+"' is not an integer");
            nsee.initCause(ex);
            throw nsee;
        }
        return ret;
    }
//}}}

//{{{ getDouble()
//##################################################################################################
    public double getDouble(String key, double defVal)
    {
        double ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            try { ret = Double.parseDouble(prop); } catch(NumberFormatException ex) {}
        }
        return ret;
    }

    public double getDouble(String key)
    {
        double ret = 0.0;
        String prop = getProperty(key);
        if(prop == null) throw new NoSuchElementException("Property '"+key+"' could not be found");
        try { ret = Double.parseDouble(prop); }
        catch(NumberFormatException ex)
        {
            NoSuchElementException nsee = new NoSuchElementException("Property '"+key+"' is not a real number");
            nsee.initCause(ex);
            throw nsee;
        }
        return ret;
    }
//}}}

//{{{ getFloat()
//##################################################################################################
    public float getFloat(String key, float defVal)
    {
        float ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            try { ret = Float.parseFloat(prop); } catch(NumberFormatException ex) {}
        }
        return ret;
    }

    public float getFloat(String key)
    {
        float ret = 0f;
        String prop = getProperty(key);
        if(prop == null) throw new NoSuchElementException("Property '"+key+"' could not be found");
        try { ret = Float.parseFloat(prop); }
        catch(NumberFormatException ex)
        {
            NoSuchElementException nsee = new NoSuchElementException("Property '"+key+"' is not a real number");
            nsee.initCause(ex);
            throw nsee;
        }
        return ret;
    }
//}}}

//{{{ getBoolean()
//##################################################################################################
    public boolean getBoolean(String key, boolean defVal)
    {
        boolean ret = defVal;
        String prop = getProperty(key);
        if(prop != null)
        {
            prop = prop.toLowerCase();
            if(prop.equals("true")  || prop.equals("on") || prop.equals("yes") || prop.equals("1"))         ret = true;
            else if(prop.equals("false") || prop.equals("off") || prop.equals("no")  || prop.equals("0"))   ret = false;
        }
        return ret;
    }

    public boolean getBoolean(String key)
    {
        boolean ret = false;
        String prop = getProperty(key);
        if(prop == null) throw new NoSuchElementException("Property '"+key+"' could not be found");
        
        prop = prop.toLowerCase();
        if(prop.equals("true")  || prop.equals("on") || prop.equals("yes") || prop.equals("1"))         ret = true;
        else if(prop.equals("false") || prop.equals("off") || prop.equals("no")  || prop.equals("0"))   ret = false;
        else throw new NoSuchElementException("Property '"+key+"' is not a boolean value");
        
        return ret;
    }
//}}}
}//class
