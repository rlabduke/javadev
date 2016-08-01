// (jEdit options) :folding=explicit:collapseFolds=1:
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DlgTest //extends ... implements ...
{
    public static void main(String[] args) { new DlgTest(args).Main(); }

    public DlgTest(String[] args)
    {
    }

    public void Main()
    {
        JFrame frame = new JFrame("The Frame");
        frame.getContentPane().add(new JLabel("the frame"));
        frame.pack();
        frame.setVisible(true);

        // The following bug occurs in Java 1.4.0 and 1.4.0_01
        // but not in Java 1.3.1_01
        // Tested under RedHat Linux 7.3 and KDE

        JDialog dialog = new JDialog(frame, "The Dialog", true);
        //dialog.getContentPane().add(new JLabel("the dialog")); // this works great, no delay
        dialog.getContentPane().add(new JLabel("Hello World!")); // this waits several seconds before painting
        //dialog.getContentPane().add(new JProgressBar());       // this also waits several seconds before painting
        dialog.pack();
        dialog.setVisible(true);
    }
}//class
