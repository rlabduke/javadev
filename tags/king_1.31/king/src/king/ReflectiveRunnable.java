// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

//import java.awt.*;
//import java.io.*;
import java.lang.reflect.*;
//import java.text.*;
//import java.util.*;
//import javax.swing.*;
import driftwood.util.SoftLog;
//}}}
/**
* <code>ReflectiveRunnable</code> uses the Reflection API to redirect calls to run() to any method in any class.
* It operates much like <code>ReflectiveAction</code>.
* This is also known (apparently) as a trampoline, because it bounces you from one method to another.
* I got the idea and most of the implementation from http://java.sun.com/docs/books/performance/ 
*
* <p>Begun on Wed Jun 12 09:33:00 EDT 2002
* <br>Copyright (C) 2002-2003 by Ian W. Davis. All rights reserved.
*/
public class ReflectiveRunnable implements Runnable
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    Object targetObject;
    String methodName;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Creates a run()-redirector.
    * @param target the object to receive the function call
    * @param method the name of the method to call; method must be declared as <i>public void some_method()</i>.
    */
    public ReflectiveRunnable(Object target, String method)
    {
        targetObject = target;
        methodName = method;
    }
//}}}

//{{{ run()
//##################################################################################################
    public void run()
    {
        try
        {
            Class[] formalParams = { };
            Method m = targetObject.getClass().getMethod(methodName, formalParams);
            
            Object[] params = { };
            m.invoke(targetObject, params);
        }
        catch(Exception ex) { ex.printStackTrace(SoftLog.err); }
    }
//}}}
}//class
