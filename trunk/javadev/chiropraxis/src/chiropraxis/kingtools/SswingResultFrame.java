package chiropraxis.kingtools;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.io.FileReader;
import java.io.*;

public class SswingResultFrame extends JFrame
{
    String myFileName="";
    JTextArea textArea = new  JTextArea();
    String[] items=new String[500]; // XXX This should be an ArrayList or something
    double[] angles=new double[5];
    SidechainSswing sidechainSswing;
    
	public SswingResultFrame(String openFileName, SidechainSswing sidechainSswing)
    {
        this.sidechainSswing = sidechainSswing;
        
        for (int i=0; i<angles.length; i++)
            angles[i]=180;
        
        for (int i=0; i<items.length; i++)
            items[i]="";
        
		readFile(openFileName);
        myFileName=openFileName;
        
        Container content = this.getContentPane();
        content.setLayout(new BorderLayout());
        
        JList list = new JList(items);
        list.setFont(new Font("MonoSpaced", Font.PLAIN,14));
        
        content.add(new JScrollPane(list));
        
        // XXX This should be done with a ListSelectionListener
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JList theList = (JList)e.getSource();
                ListModel model = theList.getModel();
                
                int index = theList.locationToIndex(e.getPoint());
                String itemString =(String)model.getElementAt(index);
                String substring;
                
                boolean setCHI=false;
                int i=0;
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
                    SswingResultFrame.this.sidechainSswing.setAllAngles(angles); // Bizarre syntax for inner class
            }
        });
	}
    
	private void readFile(String filename) {
        
        try {
            Reader r =new FileReader(filename);
            BufferedReader br = new BufferedReader(r);
            String s;
            
            int i=0;
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
    
	public void showFrame()
    {
        setTitle(myFileName);
        setBounds(300,300,1000,300);
        setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        validate();
    }
}
