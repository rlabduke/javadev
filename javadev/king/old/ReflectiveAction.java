package king;
//import java.awt.*;
import java.awt.event.*;
//import java.io.*;
import java.lang.reflect.*;
//import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
//import duke.kinemage.util.*;

/**
* <code>ReflectiveAction</code> uses the Reflection API to let one Action class service many action-generators.
* This is also known (apparently) as a trampoline, because it bounces you from one method to another.
* I got the idea and most of the implementation from http://java.sun.com/docs/books/performance/ 
*
* <p>Begun on Mon Apr 29 19:23:51 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 */
public class ReflectiveAction extends AbstractAction
{
//##################################################################################################
    Object targetObject;
    String methodName;

//##################################################################################################
    /**
    * Constructor
    */
    public ReflectiveAction(String name, Icon icon, Object target, String method)
    {
        super(name, icon);
        targetObject = target;
        methodName = method;
    }

//##################################################################################################
    /**
    * An ReflectiveAction that reflects ActionEvents. Target method should have the form
    * <code>public void some_function_name(ActionEvent ev)</code>.
    */
    public void actionPerformed(ActionEvent ev)
    {
        try
        {
            Class[] formalParams = { Class.forName("java.awt.event.ActionEvent") };
            Method m = targetObject.getClass().getMethod(methodName, formalParams);
            
            Object[] params = { ev };
            m.invoke(targetObject, params);
        }
        catch(InvocationTargetException ex)
        {
            System.err.println("InvocationTargetException: "+ex.getMessage());
            if(ex.getTargetException() != null) ex.getTargetException().printStackTrace();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'

}//class
