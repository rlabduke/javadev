package chiropraxis.kingtools;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.io.FileReader;
import java.io.*;

public class SswingResultFrame extends JFrame{

  String myFileName="";
  JTextArea textArea = new  JTextArea();
  String items[]=new String[500];
  double angles[]=new double[5];
  int i;
   
	public SswingResultFrame(String openFileName) {

    for (i=0; i<5; i++)
        angles[i]=180;

    for (i=0; i<500; i++)
        items[i]="";

		readFile(openFileName);
    myFileName=openFileName;
        
    Container content = this.getContentPane();
    content.setLayout(new BorderLayout());

    JList list = new JList(items);
    list.setFont(new Font("MonoSpaced", Font.PLAIN,14));

    content.add(new JScrollPane(list));

    list.addMouseListener(new MouseAdapter() {
             public void mouseClicked(MouseEvent e) {
                   JList theList = (JList)e.getSource();
                   ListModel model = theList.getModel();

                   int index = theList.locationToIndex(e.getPoint());
                   String itemString =(String)model.getElementAt(index);
                   String substring;

                   boolean setCHI=false;
                   i=0;
                   StringTokenizer st = new StringTokenizer(itemString, ":");
                   while (st.hasMoreTokens()) {
                      if(i<4)
                      {
                         substring = st.nextToken();                                                 
                         if(setCHI=substring.startsWith("CHI"))
                         {
                            substring = substring.substring(5,11);
                            angles[i] = Double.valueOf(substring).doubleValue();
                            i++;
                         }else ;
                      }else st.nextToken();
                   }
                   if (setCHI)
                      SswingTool.MODEL_SSWING.setAllAngles(angles);                        
             }
    });
	}

	private void readFile(String filename) {

    try {
      Reader r =new FileReader(filename);
      BufferedReader br = new BufferedReader(r);
      String s;

      i=0;
      while((s = br.readLine()) != null)
      {
         items[i]=s + "\n";
         i++;
      }
      br.close();
    }
    catch(IOException e) {
         System.err.println(e.getMessage());
    }	
	}

	public void showFrame(){
         JFrame f=new SswingResultFrame(myFileName);
 
         f.setTitle(myFileName);
         f.setBounds(300,300,1000,300);
         f.setVisible(true);
         f.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

         f.validate();

  }
}
