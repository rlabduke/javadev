package chiropraxis.kingtools;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.io.FileReader;

public class SswingResultFrame extends JFrame {

  String myFileName="";

	private JTextPane textPane = new JTextPane();
	private StyledDocument document = 
						  (StyledDocument)textPane.getDocument();

	public SswingResultFrame(String openFileName) {
		Container contentPane = getContentPane();
		readFile(openFileName);

    myFileName=openFileName;
		setAttributes();

		textPane.setFont(new Font("Dialog", Font.PLAIN, 18));
		contentPane.add(new JScrollPane(textPane), 
						BorderLayout.CENTER);
	}
	private void setAttributes() {
		SimpleAttributeSet attributes = new SimpleAttributeSet();

//		StyleConstants.setForeground(attributes, Color.blue);
//		StyleConstants.setUnderline(attributes, true);

//		document.setCharacterAttributes(5,9,attributes,false);

		StyleConstants.setForeground(attributes, Color.red);
		StyleConstants.setStrikeThrough(attributes, false);

		document.setCharacterAttributes(0,20,attributes,true);
	}
	private void readFile(String filename) {
		EditorKit kit = textPane.getEditorKit();
		try {
			kit.read(new FileReader(filename), document, 0);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
//	public static void main(String args[]){

	public void showFrame(){

         JFrame f=new SswingResultFrame(myFileName);
         f.setTitle(myFileName);
         f.setBounds(300,300,1200,300);
         f.setVisible(true);
         f.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

//         f.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//         f.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//         f.addWindowListener(new WindowAdapter() {
//             public void windowClosed(WindowEvent e) {
//                    System.exit(0);
//             }
//         });

//	    GJApp.launch(new SswingResultFrame(myFileName),
//			                 "Sswing results",300,300,1100,300);
 }


}
