// (jEdit options) :folding=explicit:collapseFolds=1:
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

/**
* <code>JComboBoxLeak</code> shows a memory leak caused by
* removeAllItems() in JComboBox.
*
* <br>Begun on Tue Feb 18 10:33:35 EST 2003
*/
public class JComboBoxLeak //extends ... implements ...
{
    static DecimalFormat df = new DecimalFormat("#,##0");

//##################################################################################################
    /** I use this from JDB for tracking down memory leaks */
    public static String showMem()
    {
        Runtime runtime = Runtime.getRuntime();
        int i, total0 = 0, free0 = 0, total1 = 0, free1 = 0, used; // in kilobytes
        // Take up to 10 tries at garbage collection
        for(i = 0; i < 10; i++)
        {
            total1 = (int)(runtime.totalMemory() >> 10);
            free1  = (int)(runtime.freeMemory() >> 10);
            if(total1 == total0 && free1 == free0) break;
            else
            {
                System.gc();
                //try { Thread.sleep(500); } catch(InterruptedException ex) {}
                total0 = total1;
                free0  = free1;
            }
        }
        used = total1 - free1;
        return df.format(used)+"kb / "+df.format(total1)+"kb";
    }

//##################################################################################################
    public static void main(String[] args)
    {
        JComboBox box = new JComboBox();
        System.out.println("Before creating bigObj: "+showMem());
        
        int[] bigObj = new int[10000000]; // ~ 40MB
        System.out.println("After creating bigObj: "+showMem());
        box.addItem(bigObj);
        bigObj = null;
        System.out.println("After adding bigObj: "+showMem());
        
        box.removeAllItems();
        System.out.println("After calling removeAllElements(): "+showMem());
        
        int[] smallObj = new int[1000000]; // ~ 4MB
        System.out.println("After creating smallObj: "+showMem());
        box.addItem(smallObj);
        smallObj = null;
        System.out.println("After adding smallObj: "+showMem());
        
        box.removeAllItems();
        System.out.println("After calling removeAllElements(): "+showMem());
        
        box.setModel(new DefaultComboBoxModel());
        System.out.println("After replacing data model: "+showMem());
    }
}//class

