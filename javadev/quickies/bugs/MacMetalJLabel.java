import java.awt.*;
import javax.swing.*;
/**
* <code>MacMetalJLabel</code> shows that under certain circumstances that
* apply only to the Macintosh, JLabels can miscalculate their sizes so that
* many of them are not full drawn and instead end with an ellipsis (...).
* Other, similar components (e.g. JCheckBox) are also affected.
*
* <p>This bug only occurs with the Metal LnF *and* with antialiasing enabled.
* Thus, to see the bug, this program must be run as follows:
<pre>java -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel -Dapple.awt.antialiasing=on MacMetalJLabel</pre>
*
* <p>The bug occurs with a variety of layout managers.
* Those that are most susceptible (e.g. BoxLayout, FlowLayout) are the ones
* that allow all components to be their preferred size and do not force them
* to be larger.
* GridLayout, on the other hand, forces everything to be as large as the largest
* component. Thus, only the string "mainchain" is initially clipped in those
* layouts, and stretching the window also stretches the JLabels so that none
* of them end up clipped.
*
* <p>I can temporarily work around this bug by overriding JLabel (or JCheckBox, etc.)
* and increasing its preferred size by 1 pixel.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Apr  8 08:57:20 EDT 2004
*/
public class MacMetalJLabel //extends ... implements ...
{
    private static class CustomLabel extends JLabel
    {
        public CustomLabel(String name)
        { super(name); }
        
        public Dimension getPreferredSize()
        {
            Dimension size = super.getPreferredSize();
            size.width += 1;
            return size;
        }
    }
    
    public MacMetalJLabel()
    {
        makeLabelTest(Box.createVerticalBox());
        makeLabelTest(new JPanel(new FlowLayout()));
        makeLabelTest(new JPanel(new GridLayout(0,1)));
        makeLabelTest(new JPanel(new GridLayout(0,3)));

        makeCustomTest(Box.createVerticalBox());
        makeCustomTest(new JPanel(new FlowLayout()));
        makeCustomTest(new JPanel(new GridLayout(0,1)));
        makeCustomTest(new JPanel(new GridLayout(0,3)));
    }
    
    private void makeLabelTest(Container box)
    {
        String[] labels = { "1sbpH", "mainchain", "Hs", "Calphas", "sidechain", "water" };
        for(int i = 0; i < labels.length; i++)
            box.add(new JLabel(labels[i]));
        
        JFrame frame = new JFrame("JLabels");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(box);
        frame.pack();
        frame.setVisible(true);
    }

    private void makeCustomTest(Container box)
    {
        String[] labels = { "1sbpH", "mainchain", "Hs", "Calphas", "sidechain", "water" };
        for(int i = 0; i < labels.length; i++)
            box.add(new CustomLabel(labels[i]));
        
        JFrame frame = new JFrame("CustomLabels");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(box);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        new MacMetalJLabel();
    }
}//class

