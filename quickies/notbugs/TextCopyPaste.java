import javax.swing.*;
/**
* <code>TextCopyPaste</code> demonstrates that the default keyboard shortcuts
* for a text area on the Macintosh use CTRL instead of APPLE, but only
* when run with -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Mar 18 16:29:52 EST 2004
*/
public class TextCopyPaste //extends ... implements ...
{
    public static void main(String[] args)
    {
        JTextArea text = new JTextArea(
            "Try cutting/copying/pasting this text.\n"+
            "APPLE won't work, but CTRL will.\n"+
            "Unless, of course you're using Aqua...",
            25, 80);
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(text);
        frame.pack();
        frame.setVisible(true);
    }
}//class

