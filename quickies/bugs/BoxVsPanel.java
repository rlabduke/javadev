import java.awt.*;
import javax.swing.*;
/**
* <code>BoxVsPanel</code> demonstrates a Box-rendering
* bug on the Mac, Java 1.4.1.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Jun 10 08:51:17 EDT 2003
*/
public class BoxVsPanel //extends ... implements ...
{
    public void Main()
    {
        JPanel  panel   = new JPanel(new GridLayout(0,1));
        Box     box     = Box.createVerticalBox();
        
        fillContainer(panel);
        fillContainer(box);
        
        Box cp = Box.createHorizontalBox();
        cp.add(panel);
        cp.add(box);
        
        JFrame frame = new JFrame("BoxVsPanel2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(cp);
        frame.pack();
        frame.setSize(frame.getWidth()+100, frame.getHeight()+100);
        frame.setVisible(true);
    }
    
    void fillContainer(Container c)
    {
        c.add(new JCheckBox("[Dum dum dum] A loooong time ago in a"));
        c.add(new JCheckBox("galaxy"));
        c.add(new JCheckBox("far, far away..."));
    }

    public static void main(String[] args)
    {
        BoxVsPanel mainprog = new BoxVsPanel();
        mainprog.Main();
    }
}//class

