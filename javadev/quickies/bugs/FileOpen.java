import java.awt.event.*;
import java.io.*;
import javax.swing.*;
/**
* <code>FileOpen</code> shows that once a JFileChooser has been
* created, it doesn't notice if a directory's contents change.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jul  2 09:21:46 EDT 2003
*/
public class FileOpen extends JFrame implements ActionListener
{
    JFileChooser        fileChooser;

    public FileOpen()
    {
        super("File | Open test");
        
        fileChooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) fileChooser.setCurrentDirectory(new File(currdir));
        
        JButton button = new JButton("Open a file...");
        button.addActionListener(this);
        
        this.getContentPane().add(button);
    }

    public void actionPerformed(ActionEvent ev)
    {
        fileChooser.showOpenDialog(this);
    }

    public static void main(String[] args)
    {
        FileOpen mainprog = new FileOpen();
        mainprog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainprog.pack();
        mainprog.setVisible(true);
    }
}//class

