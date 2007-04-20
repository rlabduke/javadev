// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.loops;

import king.*;
import king.core.*;
import driftwood.gui.*;
import driftwood.util.*;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
//}}}

public class LoopRmsdTool extends BasicTool implements ChangeListener {
  
  //{{{ Constants
  //}}}
  
  //{{{ Variable
  JFileChooser filechooser;
  ArrayList<TreeMap<Double, ArrayList<Integer>>> rmsdMapsList = null;
  ArrayList offLoops;
  TablePane2 pane;
  HighLowSliders[] sliders;
  //JSlider[] lowSliders;
  //JSlider[] highSliders;
  //}}}
  
  //{{{ Constructors
  public LoopRmsdTool(ToolBox tb) {
    super(tb);
  }
  //}}}
  
  //{{{ start
  public void start() {
    offLoops = new ArrayList();
    openFile();
    buildGUI();
    show();
  }
  //}}}
  
  //{{{ buildGUI
  //##############################################################################
  private void buildGUI() {
    
    dialog = new JDialog(kMain.getTopWindow(), "Loop Rmsd Plugin", false);
    pane = new TablePane2();
    
    //lowSliders = new JSlider[rmsdMapsList.size()];
    //highSliders = new JSlider[rmsdMapsList.size()];
    sliders = new HighLowSliders[rmsdMapsList.size()];
    for (int i = 0; i < rmsdMapsList.size(); i++) {
      TreeMap<Double, ArrayList<Integer>> map = rmsdMapsList.get(i);
      int lowValue = (int) Math.floor((map.firstKey()).doubleValue());
      int highValue = (int) Math.ceil((map.lastKey()).doubleValue());
      sliders[i] = new HighLowSliders(lowValue, highValue, 100);
      sliders[i].setMajorTickSpacing(5);
      sliders[i].setPaintTicks(true);
      //lowSliders[i] = new JSlider(lowValue * 100, highValue * 100, lowValue * 100);
      //lowSliders[i].setMajorTickSpacing(500);
      //lowSliders[i].setPaintTicks(true);
      //lowSliders[i].setPaintLabels(true);
      //highSliders[i] = new JSlider(lowValue * 100, highValue * 100, highValue * 100);
      //highSliders[i].setMajorTickSpacing(500);
      //highSliders[i].setPaintTicks(true);
      //highSliders[i].setPaintLabels(true);
      sliders[i].addChangeListener(this);
      pane.add(sliders[i].getLowSlider());
      pane.add(sliders[i].getHighSlider());
      pane.newRow();
      pane.add(sliders[i].getLowLabel());
      pane.add(sliders[i].getHighLabel());
      pane.newRow();
    }
    dialog.addWindowListener(this);
    dialog.setContentPane(pane);
    
  }
  //}}}
  
  //{{{ stateChanged
  public void stateChanged(ChangeEvent ev) {
    //JSlider source = (JSlider) ev.getSource();
    //System.out.println(source.toString() + ": " + source.getValue() / 100);
    offLoops.clear();
    for (int i = 0; i < sliders.length; i++) {
      HighLowSliders hls = sliders[i];
      hls.stateChanged(ev);
      calcOffLoopNums(hls, rmsdMapsList.get(i));
    }
    turnOffLoops();
    //System.out.println(offLoops.size());
  }
  //}}}  
  
  //{{{ calcOffLoopNums
  public void calcOffLoopNums(HighLowSliders hls, TreeMap<Double, ArrayList<Integer>> map) {
    Double lowVal = new Double(hls.getLowValue());
    Double highVal = new Double(hls.getHighValue());
    SortedMap headMap = map.headMap(lowVal);
    Collection<ArrayList> headVals = headMap.values();
    for (ArrayList loopNums : headVals) {
      offLoops.addAll(loopNums);
    }
    SortedMap tailMap = map.tailMap(highVal);
    Collection<ArrayList> tailVals = tailMap.values();
    for (ArrayList loopNums : tailVals) {
      offLoops.addAll(loopNums);
    }
  }
  //}}}
    
  //{{{ turnOffLoops
  public void turnOffLoops() {
    Kinemage kin = kMain.getKinemage();
    if (kin != null) {
      ArrayList<KGroup> groups = kin.getChildren();
      for (int i = 0; i < groups.size(); i++) {
        KGroup group = groups.get(i);
        //int loopInt = loopNum.intValue();
        Integer currNum = new Integer(i + 1); //cause of index starting at 0
        if (offLoops.contains(currNum)) {
          group.setOn(false);
        } else {
          group.setOn(true);
        }
      }
    }
  }
  //}}}
  
  //{{{ makeFileChooser
  //##################################################################################################
  void makeFileChooser()
  {
    
    // Make accessory for file chooser
    TablePane acc = new TablePane();
    
    // Make actual file chooser -- will throw an exception if we're running as an Applet
    filechooser = new JFileChooser();
    String currdir = System.getProperty("user.dir");
    if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
    
    filechooser.setAccessory(acc);
    
  }
  //}}}
  
  //{{{ openFile
  //##################################################################################################
  public void openFile()
  {
    // Create file chooser on demand
    if(filechooser == null) makeFileChooser();
    
    if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
    {
	    try {
        File f = filechooser.getSelectedFile();
        if(f != null && f.exists()) {
          //dialog.setTitle(f.getName());
          BufferedReader reader = new BufferedReader(new FileReader(f));
          String line;
          while ((line = reader.readLine()) != null) {
            String[] splitLine = Strings.explode(line, " ".charAt(0), false, true);
            if (rmsdMapsList == null) {
              rmsdMapsList = new ArrayList<TreeMap<Double, ArrayList<Integer>>>();
              for (int i = 0; i < splitLine.length - 1; i++) {
                TreeMap<Double, ArrayList<Integer>> map = new TreeMap<Double, ArrayList<Integer>>();
                rmsdMapsList.add(map);
              }
            }
            Integer loopNum = Integer.parseInt(splitLine[0]);
            for (int i = 1; i < splitLine.length; i++) {
              Double value = Double.parseDouble(splitLine[i]);
              TreeMap<Double, ArrayList<Integer>> map = rmsdMapsList.get(i-1);
              ArrayList<Integer> listofLoopNums = null;
              if (map.containsKey(value)) {
                listofLoopNums = map.get(value);
                listofLoopNums.add(loopNum);
              } else {
                listofLoopNums = new ArrayList<Integer>();
                listofLoopNums.add(loopNum);
                map.put(value, listofLoopNums);

              }
            
              //System.out.print(value);
            }
          }          
          reader.close();
        }
      } catch (IOException ie) {
        JOptionPane.showMessageDialog(kMain.getTopWindow(),
        "An I/O error occurred while loading the file:\n"+ie.getMessage(),
        "Sorry!", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
  //}}}
  
  //{{{ getHelpAnchor, toString
  //##################################################################################################
  public String getHelpAnchor()
  { return null; }
  
  public Container getToolPanel()
  { return dialog; }
  
  public String toString()
  { return "Read-in RMSD file"; }
  //}}}
  
}
