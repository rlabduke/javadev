import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
/**
* <code>CtrlClickBug</code> demonstrates that ctrl-click on the Mac w/
* Java 1.4.2 update 1 does not generate a mouseClicked() event.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Sep 24 08:00:09 EDT 2004
*/
public class CtrlClickBug implements MouseListener
{
    public static void main(String[] args)
    {
        CtrlClickBug mainprog = new CtrlClickBug();
    }

    public CtrlClickBug()
    {
        JFrame frame = new JFrame("Test window");
        frame.setSize(new Dimension(600,600));
        frame.addMouseListener(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void mousePressed(MouseEvent ev)
    {
        System.out.println("-------------------------------------------------");
        System.out.println("--> Mouse btn pressed: "+ev);
    }
    
    public void mouseReleased(MouseEvent ev)
    {
        System.out.println("--> Mouse btn released: "+ev);
    }

    public void mouseClicked(MouseEvent ev)
    {
        System.out.println("--> MOUSE BTN CLICKED: "+ev);
    }
    
    public void mouseEntered(MouseEvent ev)
    {
    }

    public void mouseExited(MouseEvent ev)
    {
    }
}//class

